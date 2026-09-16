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

// MARK: - Journal, milestones and media

/**
 * A photo (or other file) held by the backend. [fileUrl] and [thumbUrl] are
 * relative, couple-scoped and authenticated, so they are fetched the way
 * `ApiConfig.imageRequest` sets up rather than as plain URLs.
 */
@Serializable
data class MediaItem(
    val id: String,
    val kind: String,
    val caption: String? = null,
    val uploaderId: String,
    val fileUrl: String,
    val thumbUrl: String,
    val createdAt: String? = null,
)

@Serializable
data class MediaList(
    val media: List<MediaItem> = emptyList(),
    val count: Int = 0,
    val storageUsed: Long = 0,
)

/** A "first" the couple wants to remember — first date, first trip, and so on. */
@Serializable
data class Milestone(
    val id: String,
    val title: String,
    /** A calendar day, sent by the server as midnight UTC. */
    val date: String,
    val kind: String? = null,
)

@Serializable
data class MilestoneList(val milestones: List<Milestone> = emptyList())

/**
 * One partner's diary entry for a day: free text and/or photos. Both partners'
 * entries for the same date are grouped under a shared day card on the client.
 */
@Serializable
data class JournalEntry(
    val id: String,
    val authorId: String,
    /** The day being written about, as midnight UTC. */
    val date: String,
    val body: String = "",
    val photos: List<MediaItem> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class JournalList(val entries: List<JournalEntry> = emptyList())

/**
 * Where the email-change code went. [devCode] is only ever populated by a dev
 * server with no mail provider configured, so the flow is not a dead end there.
 */
@Serializable
data class EmailChangeRequested(
    val sentTo: String,
    val expiresAt: String? = null,
    val devCode: String? = null,
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
