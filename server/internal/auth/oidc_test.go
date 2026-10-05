package auth

import (
	"context"
	"crypto/rand"
	"crypto/rsa"
	"encoding/base64"
	"encoding/json"
	"math/big"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// oidcFixture is a stand-in provider: an RSA key, a JWKS endpoint serving its
// public half, and a way to mint tokens with whatever claims a test needs.
type oidcFixture struct {
	key    *rsa.PrivateKey
	server *httptest.Server
}

const testKID = "test-key"

func newOIDCFixture(t *testing.T) *oidcFixture {
	t.Helper()
	key, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		t.Fatalf("generate key: %v", err)
	}
	f := &oidcFixture{key: key}
	f.server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		e := big.NewInt(int64(key.PublicKey.E)).Bytes()
		_ = json.NewEncoder(w).Encode(map[string]any{
			"keys": []map[string]string{{
				"kty": "RSA",
				"kid": testKID,
				"n":   base64.RawURLEncoding.EncodeToString(key.PublicKey.N.Bytes()),
				"e":   base64.RawURLEncoding.EncodeToString(e),
			}},
		})
	}))
	t.Cleanup(f.server.Close)
	return f
}

func (f *oidcFixture) verifier(audiences ...string) *OIDCVerifier {
	return newOIDCVerifier(f.server.URL, []string{"https://accounts.google.com"}, audiences)
}

func (f *oidcFixture) token(t *testing.T, claims jwt.MapClaims) string {
	t.Helper()
	tok := jwt.NewWithClaims(jwt.SigningMethodRS256, claims)
	tok.Header["kid"] = testKID
	signed, err := tok.SignedString(f.key)
	if err != nil {
		t.Fatalf("sign token: %v", err)
	}
	return signed
}

func validClaims() jwt.MapClaims {
	return jwt.MapClaims{
		"iss":            "https://accounts.google.com",
		"aud":            "client-a",
		"sub":            "google-user-1",
		"email":          "someone@example.com",
		"email_verified": true,
		"name":           "Someone",
		"exp":            time.Now().Add(time.Hour).Unix(),
		"iat":            time.Now().Unix(),
	}
}

func TestVerifyAcceptsAWellFormedToken(t *testing.T) {
	f := newOIDCFixture(t)
	id, err := f.verifier("client-a").Verify(context.Background(), f.token(t, validClaims()))
	if err != nil {
		t.Fatalf("expected the token to verify, got %v", err)
	}
	if id.Subject != "google-user-1" {
		t.Errorf("subject = %q, want google-user-1", id.Subject)
	}
	if id.Name != "Someone" {
		t.Errorf("name = %q, want Someone", id.Name)
	}
	if id.Email == nil || *id.Email != "someone@example.com" {
		t.Errorf("email = %v, want someone@example.com", id.Email)
	}
}

// The email is what links a Google sign-in to an existing account, so an
// address the provider has not verified must not come through: otherwise
// anyone able to mint a token claiming an address could walk into that account.
func TestVerifyDropsAnUnverifiedEmail(t *testing.T) {
	f := newOIDCFixture(t)
	claims := validClaims()
	claims["email_verified"] = false

	id, err := f.verifier("client-a").Verify(context.Background(), f.token(t, claims))
	if err != nil {
		t.Fatalf("expected the token to verify, got %v", err)
	}
	if id.Email != nil {
		t.Errorf("email = %v, want nil for an unverified address", *id.Email)
	}
	if id.Subject != "google-user-1" {
		t.Errorf("subject = %q, want the token still to identify the user", id.Subject)
	}
}

func TestVerifyAcceptsEmailVerifiedAsAString(t *testing.T) {
	f := newOIDCFixture(t)
	claims := validClaims()
	claims["email_verified"] = "true"

	id, err := f.verifier("client-a").Verify(context.Background(), f.token(t, claims))
	if err != nil {
		t.Fatalf("expected the token to verify, got %v", err)
	}
	if id.Email == nil {
		t.Error("email = nil, want the address through when email_verified is \"true\"")
	}
}

func TestVerifyRejectsBadTokens(t *testing.T) {
	f := newOIDCFixture(t)

	cases := map[string]func(jwt.MapClaims){
		"another app's audience": func(c jwt.MapClaims) { c["aud"] = "someone-elses-client" },
		"wrong issuer":           func(c jwt.MapClaims) { c["iss"] = "https://evil.example.com" },
		"expired":                func(c jwt.MapClaims) { c["exp"] = time.Now().Add(-time.Hour).Unix() },
		"no subject":             func(c jwt.MapClaims) { delete(c, "sub") },
	}
	for name, mangle := range cases {
		t.Run(name, func(t *testing.T) {
			claims := validClaims()
			mangle(claims)
			if _, err := f.verifier("client-a").Verify(context.Background(), f.token(t, claims)); err == nil {
				t.Fatalf("expected %s to be rejected", name)
			}
		})
	}
}

// A token signed by a key that is not the provider's is the whole attack this
// verifier exists to stop.
func TestVerifyRejectsAForeignSigningKey(t *testing.T) {
	f := newOIDCFixture(t)
	attacker := newOIDCFixture(t)

	// Minted by the attacker's key, but presented to a verifier that trusts
	// only the real provider's JWKS.
	token := attacker.token(t, validClaims())
	if _, err := f.verifier("client-a").Verify(context.Background(), token); err == nil {
		t.Fatal("expected a token signed by an unknown key to be rejected")
	}
}

func TestVerifyAcceptsAnAudienceList(t *testing.T) {
	f := newOIDCFixture(t)
	claims := validClaims()
	claims["aud"] = []any{"client-b", "client-a"}

	if _, err := f.verifier("client-a").Verify(context.Background(), f.token(t, claims)); err != nil {
		t.Fatalf("expected a matching entry in an aud list to pass, got %v", err)
	}
}

// With no client ID configured every token would fail the audience check, so
// the endpoint refuses up front rather than reporting valid sign-ins as bad.
func TestConfiguredReportsWhetherAnyClientIDIsSet(t *testing.T) {
	f := newOIDCFixture(t)
	if f.verifier().Configured() {
		t.Error("Configured() = true with no client IDs, want false")
	}
	if !f.verifier("client-a").Configured() {
		t.Error("Configured() = false with a client ID, want true")
	}
}
