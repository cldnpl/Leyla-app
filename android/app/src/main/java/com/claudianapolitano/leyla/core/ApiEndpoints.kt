package com.claudianapolitano.leyla.core

import io.ktor.http.HttpMethod

/**
 * High-level API methods mapping to the Leyla backend endpoints. Only the ones
 * Home needs are here; the rest follow as their screens are ported.
 */
object LeylaApi {

    // MARK: - Auth

    suspend fun register(email: String, password: String, displayName: String): AuthResponse =
        ApiClient.send(
            "/v1/auth/register",
            HttpMethod.Post,
            RegisterBody(email, password, displayName),
            authorized = false,
        )

    suspend fun login(email: String, password: String): AuthResponse =
        ApiClient.send("/v1/auth/login", HttpMethod.Post, LoginBody(email, password), authorized = false)

    /**
     * Exchanges a Google ID token for a Leyla session. The backend verifies the
     * token against Google's keys, so registering and logging in are the same
     * call: it creates the account the first time and finds it afterwards.
     */
    suspend fun googleSignIn(idToken: String): AuthResponse =
        ApiClient.send("/v1/auth/google", HttpMethod.Post, GoogleBody(idToken), authorized = false)

    /**
     * Retires the refresh token server-side. Sent unauthorized on purpose: the
     * token in the body is the credential, and the access token may already
     * have expired by the time someone signs out.
     */
    suspend fun logout(refreshToken: String) {
        ApiClient.sendVoid(
            "/v1/auth/logout",
            HttpMethod.Post,
            LogoutBody(refreshToken),
            authorized = false,
        )
    }

    // MARK: - Pairing

    suspend fun createPairingCode(): PairingCode = ApiClient.post("/v1/pairing/code")

    suspend fun redeemPairing(code: String): CoupleResponse =
        ApiClient.post("/v1/pairing/redeem", RedeemBody(code))

    suspend fun me(): User = ApiClient.get("/v1/me")

    // MARK: - Profile

    suspend fun updateDisplayName(displayName: String): User =
        ApiClient.send("/v1/me", HttpMethod.Patch, UpdateNameBody(displayName))

    suspend fun updatePartnerPronoun(pronoun: String): User =
        ApiClient.send("/v1/me", HttpMethod.Patch, UpdatePronounBody(pronoun))

    /** Persists the account-level cycle settings so they survive a reinstall. */
    suspend fun updateCycleSettings(hasCycle: Boolean? = null, shareLevel: String? = null): User =
        ApiClient.send("/v1/me", HttpMethod.Patch, UpdateCycleSettingsBody(hasCycle, shareLevel))

    /**
     * Uploads a new profile photo. The server saves it, sets `avatarPath` on the
     * account and hands back the updated user.
     */
    suspend fun uploadAvatar(jpeg: ByteArray): User = ApiClient.json.decodeFromString(
        ApiClient.uploadImage("/v1/me/avatar", jpeg, filename = "avatar.jpg"),
    )

    suspend fun deleteAvatar(): User = ApiClient.send("/v1/me/avatar", HttpMethod.Delete, null)

    suspend fun getCouple(): CoupleResponse = ApiClient.get("/v1/couple")

    /** The day the relationship started, which Home counts from. */
    suspend fun updateCoupleStartDate(isoDay: String): CoupleResponse =
        ApiClient.send("/v1/couple", HttpMethod.Patch, UpdateCoupleBody(isoDay))

    /**
     * Breaks the pairing. Both accounts survive — only the couple between them
     * is removed, so either can pair again.
     */
    suspend fun unpair() = ApiClient.sendVoid("/v1/couple", HttpMethod.Delete)

    /** Permanently deletes the signed-in account and everything on it. */
    suspend fun deleteAccount() = ApiClient.sendVoid("/v1/me", HttpMethod.Delete)

    /**
     * Starts an email change by mailing a code to the *new* address — receiving
     * it is what proves the address is reachable. Nothing on the account moves
     * until the code is confirmed.
     */
    suspend fun requestEmailChange(newEmail: String): EmailChangeRequested =
        ApiClient.send("/v1/me/email/request", HttpMethod.Post, RequestEmailChangeBody(newEmail))

    suspend fun confirmEmailChange(code: String): User =
        ApiClient.send("/v1/me/email/confirm", HttpMethod.Post, ConfirmEmailChangeBody(code))

    // MARK: - "I miss you"

    suspend fun sendMissYou(): MissYouEvent = ApiClient.post("/v1/miss-you")

    // MARK: - Partner location

    suspend fun partnerLocation(): PartnerLocation = ApiClient.get("/v1/location")

    suspend fun updateLocation(lat: Double, lng: Double, accuracy: Double?, mode: String) {
        ApiClient.sendVoid(
            "/v1/location",
            HttpMethod.Put,
            UpdateLocationBody(lat, lng, accuracy, mode),
        )
    }

    suspend fun stopSharingLocation() {
        ApiClient.sendVoid("/v1/location", HttpMethod.Put, mapOf("mode" to "off"))
    }

    // MARK: - Journal, milestones and media

    /** The shared diary, newest first — the server already orders it date DESC. */
    suspend fun listJournal(): List<JournalEntry> =
        ApiClient.get<JournalList>("/v1/journal").entries

    suspend fun createJournalEntry(date: String, body: String): JournalEntry =
        ApiClient.send("/v1/journal", HttpMethod.Post, CreateJournalBody(date, body))

    /** Only the text is editable; the day an entry belongs to is fixed. */
    suspend fun updateJournalEntry(id: String, body: String): JournalEntry =
        ApiClient.send("/v1/journal/$id", HttpMethod.Put, UpdateJournalBody(body))

    suspend fun uploadJournalPhoto(entryId: String, jpeg: ByteArray): MediaItem =
        ApiClient.json.decodeFromString(
            ApiClient.uploadImage("/v1/journal/$entryId/photos", jpeg, filename = "photo.jpg"),
        )

    suspend fun deleteJournalEntry(id: String) =
        ApiClient.sendVoid("/v1/journal/$id", HttpMethod.Delete)

    suspend fun listMilestones(): List<Milestone> =
        ApiClient.get<MilestoneList>("/v1/milestones").milestones

    suspend fun createMilestone(title: String, date: String, kind: String = "milestone"): Milestone =
        ApiClient.send("/v1/milestones", HttpMethod.Post, CreateMilestoneBody(title, date, kind))

    suspend fun updateMilestone(id: String, title: String, date: String): Milestone =
        ApiClient.send("/v1/milestones/$id", HttpMethod.Patch, UpdateMilestoneBody(title, date))

    suspend fun deleteMilestone(id: String) =
        ApiClient.sendVoid("/v1/milestones/$id", HttpMethod.Delete)

    /** Removes one photo. The server only lets its uploader do this. */
    suspend fun deletePhoto(id: String) = ApiClient.sendVoid("/v1/media/$id", HttpMethod.Delete)

    // MARK: - Cycle / pregnancy sharing

    suspend fun partnerCycle(): PartnerCycle = ApiClient.get("/v1/cycle")

    suspend fun partnerPregnancy(): PartnerPregnancy = ApiClient.get("/v1/pregnancy")

    /**
     * Publishes the coarse cycle summary the chosen sharing level allows. Only
     * ever the phase, the day and a countdown — never raw symptoms, and never
     * the dates read from Health Connect.
     */
    suspend fun putCycle(phase: String, cycleDay: Int, periodInDays: Int, note: String?) =
        ApiClient.sendVoid(
            "/v1/cycle",
            HttpMethod.Put,
            UpdateCycleBody(phase, cycleDay, periodInDays, note),
        )

    /** Withdraws the shared summary, leaving the partner with nothing to see. */
    suspend fun stopSharingCycle() = ApiClient.sendVoid("/v1/cycle", HttpMethod.Delete)

    suspend fun putPregnancy(dueDate: String) =
        ApiClient.sendVoid("/v1/pregnancy", HttpMethod.Put, UpdatePregnancyBody(dueDate))

    suspend fun stopSharingPregnancy() = ApiClient.sendVoid("/v1/pregnancy", HttpMethod.Delete)
}

@kotlinx.serialization.Serializable
data class RegisterBody(val email: String, val password: String, val displayName: String)

@kotlinx.serialization.Serializable
data class LoginBody(val email: String, val password: String)

@kotlinx.serialization.Serializable
data class LogoutBody(val refreshToken: String)

@kotlinx.serialization.Serializable
data class GoogleBody(val idToken: String)

@kotlinx.serialization.Serializable
data class RedeemBody(val code: String)

@kotlinx.serialization.Serializable
data class UpdateNameBody(val displayName: String)

@kotlinx.serialization.Serializable
data class UpdateCoupleBody(val startDate: String)

@kotlinx.serialization.Serializable
data class RequestEmailChangeBody(val newEmail: String)

@kotlinx.serialization.Serializable
data class ConfirmEmailChangeBody(val code: String)

@kotlinx.serialization.Serializable
data class UpdatePronounBody(val partnerPronoun: String)

@kotlinx.serialization.Serializable
data class UpdateCycleSettingsBody(val hasCycle: Boolean?, val cycleShareLevel: String?)

@kotlinx.serialization.Serializable
data class UpdateCycleBody(
    val phase: String,
    val cycleDay: Int,
    val periodInDays: Int,
    val note: String?,
)

@kotlinx.serialization.Serializable
data class UpdatePregnancyBody(val dueDate: String)

@kotlinx.serialization.Serializable
data class CreateJournalBody(val date: String, val body: String)

@kotlinx.serialization.Serializable
data class UpdateJournalBody(val body: String)

@kotlinx.serialization.Serializable
data class CreateMilestoneBody(val title: String, val date: String, val kind: String)

@kotlinx.serialization.Serializable
data class UpdateMilestoneBody(val title: String, val date: String)

@kotlinx.serialization.Serializable
data class UpdateLocationBody(
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val mode: String,
)
