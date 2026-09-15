package com.claudianapolitano.leyla.core

import io.ktor.http.HttpMethod

/**
 * High-level API methods mapping to the Leyla backend endpoints. Only the ones
 * Home needs are here; the rest follow as their screens are ported.
 */
object LeylaApi {

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

    // MARK: - Cycle / pregnancy sharing

    suspend fun partnerCycle(): PartnerCycle = ApiClient.get("/v1/cycle")

    suspend fun partnerPregnancy(): PartnerPregnancy = ApiClient.get("/v1/pregnancy")
}

@kotlinx.serialization.Serializable
data class UpdateNameBody(val displayName: String)

@kotlinx.serialization.Serializable
data class UpdatePronounBody(val partnerPronoun: String)

@kotlinx.serialization.Serializable
data class UpdateCycleSettingsBody(val hasCycle: Boolean?, val cycleShareLevel: String?)

@kotlinx.serialization.Serializable
data class UpdateLocationBody(
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val mode: String,
)
