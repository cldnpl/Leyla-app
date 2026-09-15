package com.claudianapolitano.leyla.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the Leyla backend, matching `Us/Core/Models.swift` field for
 * field. Dates arrive as ISO-8601 strings and are kept as strings until a
 * screen actually needs to do date maths with them — Home doesn't.
 */

@Serializable
data class User(
    val id: String,
    val email: String? = null,
    val displayName: String,
    val avatarPath: String? = null,
    val birthday: String? = null,
    val partnerPronoun: String? = null,
    val createdAt: String? = null,
    /** Whether the address was confirmed with a code we emailed to it. */
    val emailVerified: Boolean? = null,
    /**
     * Account-level cycle settings, kept server-side so they survive a
     * reinstall. `hasCycle` is null when the question was never answered.
     */
    val hasCycle: Boolean? = null,
    val cycleShareLevel: String? = null,
)

@Serializable
data class Couple(
    val id: String,
    val startDate: String? = null,
    val status: String,
    val createdAt: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)

@Serializable
data class CoupleResponse(
    val paired: Boolean,
    val couple: Couple? = null,
    val partner: User? = null,
)

/** A short-lived code one partner shows so the other can join the couple. */
@Serializable
data class PairingCode(
    val code: String,
    val expiresAt: String? = null,
)

@Serializable
data class MissYouEvent(
    val id: String,
    val senderId: String,
    val kind: String,
    val createdAt: String? = null,
)

@Serializable
data class PartnerLocation(
    val sharing: Boolean,
    val lat: Double? = null,
    val lng: Double? = null,
    val mode: String? = null,
    val partnerName: String? = null,
    val updatedAt: String? = null,
)

/**
 * The partner's opt-in shared cycle summary, or `{sharing:false}`. Coarse by
 * design: a phase, and optionally a day count — never raw symptoms.
 */
@Serializable
data class PartnerCycle(
    val sharing: Boolean,
    val phase: String? = null,
    val cycleDay: Int? = null,
    val periodInDays: Int? = null,
    val note: String? = null,
    val partnerName: String? = null,
    val updatedAt: String? = null,
)

/**
 * The partner's opt-in shared due date, or `{sharing:false}`. Week, trimester
 * and countdown are derived on the client.
 */
@Serializable
data class PartnerPregnancy(
    val sharing: Boolean,
    val dueDate: String? = null,
    val partnerName: String? = null,
    val updatedAt: String? = null,
)

/** Error payload returned by the API (`{"error": "...", "code": "..."}`). */
@Serializable
data class ApiErrorResponse(
    @SerialName("error") val message: String,
    val code: String? = null,
)

/** Thrown for any non-2xx response; carries the server's own message when it sent one. */
class ApiException(
    val status: Int,
    val payload: ApiErrorResponse?,
    override val message: String = payload?.message ?: "Request failed ($status)",
) : Exception(message)

/** The one unrecoverable case: the refresh token itself was rejected. */
class UnauthorizedException : Exception("Unauthorized")
