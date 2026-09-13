package com.claudianapolitano.leyla.core

import io.ktor.http.HttpMethod

/**
 * High-level API methods mapping to the Leyla backend endpoints. Only the ones
 * Home needs are here; the rest follow as their screens are ported.
 */
object LeylaApi {

    suspend fun me(): User = ApiClient.get("/v1/me")

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
data class UpdateLocationBody(
    val lat: Double,
    val lng: Double,
    val accuracy: Double? = null,
    val mode: String,
)
