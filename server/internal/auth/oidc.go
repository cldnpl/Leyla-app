package auth

import (
	"context"
	"crypto/rsa"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"math/big"
	"net/http"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// Identity is what a verified ID token says about the person signing in.
type Identity struct {
	// Subject is the provider's stable id for this account ("sub"). It is the
	// only field safe to key a user on: email addresses change hands.
	Subject string
	// Email is set only when the provider says it verified the address, since
	// an unverified one must never be used to find an existing account.
	Email *string
	Name  string
}

// OIDCVerifier validates OpenID Connect ID tokens against a provider's
// published JWKS, caching the keys for an hour.
//
// Sign in with Apple has its own copy of this logic in apple.go. That one came
// first and is deliberately left alone: it is live on iOS, and folding it in
// here would mean re-testing a flow this change does not otherwise touch.
type OIDCVerifier struct {
	jwksURL   string
	issuers   map[string]bool
	audiences map[string]bool
	client    *http.Client

	mu        sync.Mutex
	keys      map[string]*rsa.PublicKey
	fetchedAt time.Time
}

const googleJWKSURL = "https://www.googleapis.com/oauth2/v3/certs"

// NewGoogleVerifier accepts tokens minted for any of the given client IDs.
//
// Google publishes its issuer both with and without the scheme and both are
// current, so each is accepted.
func NewGoogleVerifier(audiences []string) *OIDCVerifier {
	return newOIDCVerifier(
		googleJWKSURL,
		[]string{"https://accounts.google.com", "accounts.google.com"},
		audiences,
	)
}

func newOIDCVerifier(jwksURL string, issuers, audiences []string) *OIDCVerifier {
	iss := make(map[string]bool, len(issuers))
	for _, i := range issuers {
		iss[i] = true
	}
	aud := make(map[string]bool, len(audiences))
	for _, a := range audiences {
		aud[a] = true
	}
	return &OIDCVerifier{
		jwksURL:   jwksURL,
		issuers:   iss,
		audiences: aud,
		client:    &http.Client{Timeout: 10 * time.Second},
		keys:      map[string]*rsa.PublicKey{},
	}
}

// Configured reports whether any client ID was supplied. Without one every
// token would fail the audience check, so the endpoint says so up front
// instead of rejecting sign-ins that are actually fine.
func (v *OIDCVerifier) Configured() bool { return len(v.audiences) > 0 }

type oidcJWK struct {
	Kid string `json:"kid"`
	N   string `json:"n"`
	E   string `json:"e"`
	Kty string `json:"kty"`
}

func (v *OIDCVerifier) refreshKeys(ctx context.Context) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, v.jwksURL, nil)
	if err != nil {
		return err
	}
	resp, err := v.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("oidc: jwks returned %s", resp.Status)
	}

	var jwks struct {
		Keys []oidcJWK `json:"keys"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&jwks); err != nil {
		return err
	}
	keys := make(map[string]*rsa.PublicKey, len(jwks.Keys))
	for _, k := range jwks.Keys {
		if k.Kty != "RSA" {
			continue
		}
		nBytes, err := base64.RawURLEncoding.DecodeString(k.N)
		if err != nil {
			continue
		}
		eBytes, err := base64.RawURLEncoding.DecodeString(k.E)
		if err != nil {
			continue
		}
		keys[k.Kid] = &rsa.PublicKey{
			N: new(big.Int).SetBytes(nBytes),
			E: int(new(big.Int).SetBytes(eBytes).Int64()),
		}
	}
	v.mu.Lock()
	v.keys = keys
	v.fetchedAt = time.Now()
	v.mu.Unlock()
	return nil
}

func (v *OIDCVerifier) keyFor(ctx context.Context, kid string) (*rsa.PublicKey, error) {
	v.mu.Lock()
	key, ok := v.keys[kid]
	stale := time.Since(v.fetchedAt) > time.Hour
	v.mu.Unlock()
	if ok && !stale {
		return key, nil
	}
	if err := v.refreshKeys(ctx); err != nil {
		if ok {
			return key, nil // fall back to a cached key on transient fetch failure
		}
		return nil, err
	}
	v.mu.Lock()
	key, ok = v.keys[kid]
	v.mu.Unlock()
	if !ok {
		return nil, errors.New("oidc: unknown key id")
	}
	return key, nil
}

// Verify validates an ID token's signature, issuer, audience and expiry, and
// returns what it claims about the signer.
func (v *OIDCVerifier) Verify(ctx context.Context, idToken string) (Identity, error) {
	claims := jwt.MapClaims{}
	_, err := jwt.ParseWithClaims(idToken, claims, func(t *jwt.Token) (any, error) {
		if _, ok := t.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, errors.New("oidc: unexpected signing method")
		}
		kid, _ := t.Header["kid"].(string)
		return v.keyFor(ctx, kid)
	}, jwt.WithValidMethods([]string{"RS256"}))
	if err != nil {
		return Identity{}, fmt.Errorf("oidc: verify token: %w", err)
	}

	iss, _ := claims["iss"].(string)
	if !v.issuers[iss] {
		return Identity{}, errors.New("oidc: issuer mismatch")
	}
	if !v.audienceOK(claims["aud"]) {
		return Identity{}, errors.New("oidc: audience mismatch")
	}

	id := Identity{}
	id.Subject, _ = claims["sub"].(string)
	if id.Subject == "" {
		return Identity{}, errors.New("oidc: missing sub")
	}
	id.Name, _ = claims["name"].(string)
	// Anyone can put an address in a token they control; only the provider
	// asserting it checked out makes it usable for finding an existing account.
	if e, ok := claims["email"].(string); ok && e != "" && verifiedClaim(claims["email_verified"]) {
		id.Email = &e
	}
	return id, nil
}

func (v *OIDCVerifier) audienceOK(aud any) bool {
	switch a := aud.(type) {
	case string:
		return v.audiences[a]
	case []any:
		for _, x := range a {
			if s, ok := x.(string); ok && v.audiences[s] {
				return true
			}
		}
	}
	return false
}

// verifiedClaim reads email_verified, which providers send as either a bool or
// the string "true".
func verifiedClaim(v any) bool {
	switch b := v.(type) {
	case bool:
		return b
	case string:
		return b == "true"
	}
	return false
}
