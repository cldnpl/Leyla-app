package com.claudianapolitano.leyla.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.claudianapolitano.leyla.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Sign in with Google, via Credential Manager.
 *
 * The old Google Sign-In SDK is deprecated; this is the replacement, and it
 * shows the system account sheet rather than a screen of our own. What comes
 * back is an ID token for the *web* client, which the backend verifies — the
 * app never sees a password and never trusts the token itself.
 */
object GoogleSignIn {

    /** Blank until `GOOGLE_WEB_CLIENT_ID` is set in `local.properties`. */
    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()

    /** Someone backing out of the account sheet is not an error worth showing. */
    class Cancelled : Exception()

    /**
     * Returns a Google ID token to hand to the backend.
     *
     * [filterByAuthorizedAccounts] asks for accounts that have used this app
     * before. Logging in wants that (one tap for someone coming back); signing
     * up cannot use it, since nobody has an authorized account yet — so a
     * first-time [isSignUp] run asks for every account on the device.
     */
    suspend fun idToken(context: Context, isSignUp: Boolean): String {
        check(isConfigured) { "GOOGLE_WEB_CLIENT_ID is not set" }

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(!isSignUp)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val response = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            throw Cancelled()
        } catch (e: NoCredentialException) {
            // Logging in filtered to previously authorized accounts and found
            // none. Ask again unfiltered before giving up: this is someone who
            // signed up on another device, or who cleared the app's data.
            if (isSignUp) throw NoGoogleAccount()
            return idToken(context, isSignUp = true)
        } catch (e: GetCredentialException) {
            throw Exception(e.message ?: "Google sign-in failed", e)
        }

        val credential = response.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw Exception("Unexpected credential from Google")
        }
        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw Exception("Could not read the Google credential", e)
        }
    }

    /** No Google account on the device at all — nothing the app can fix. */
    class NoGoogleAccount : Exception("No Google account on this device. Add one in Settings, or use email.")
}
