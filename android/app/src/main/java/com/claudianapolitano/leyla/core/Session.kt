package com.claudianapolitano.leyla.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Global app state: authentication, current user, and couple. Port of
 * `Us/Core/Session.swift`, trimmed to what Home reads — auth, pairing and
 * onboarding routing land with their own screens.
 */
object Session {

    enum class State { LOADING, SIGNED_OUT, NEEDS_PERSONAL_ONBOARDING, NEEDS_PAIRING, READY }

    data class Snapshot(
        val state: State = State.LOADING,
        val user: User? = null,
        val partner: User? = null,
        val couple: Couple? = null,
        /**
         * Bumps whenever a connected partner changes shared data. Feature
         * screens key their refresh off it, the way the iOS views use
         * `.task(id: session.remoteChangeID)`.
         */
        val remoteChangeId: Long = 0,
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    val current: Snapshot get() = _snapshot.value

    /**
     * Called on launch to restore an existing session.
     *
     * The login must survive app restarts: once signed in, the ONLY things that
     * send the user back to the sign-in screen are an explicit sign-out or the
     * backend definitively rejecting the refresh token. A network blip, a
     * server error, or a slow launch must NOT throw the session away.
     */
    suspend fun bootstrap() {
        if (TokenStore.accessToken == null && TokenStore.refreshToken == null) {
            _snapshot.update { it.copy(state = State.SIGNED_OUT) }
            return
        }
        try {
            val user = LeylaApi.me()
            PartnerPrefs.pronoun = PartnerPronoun.from(user.partnerPronoun)
            val couple = LeylaApi.getCouple()
            _snapshot.update {
                it.copy(
                    state = if (couple.paired) State.READY else State.NEEDS_PAIRING,
                    user = user,
                    partner = couple.partner,
                    couple = couple.couple,
                )
            }
        } catch (_: UnauthorizedException) {
            // The refresh token was rejected: the only unrecoverable case, and
            // the only place bootstrap is allowed to drop the session.
            TokenStore.clear()
            _snapshot.update { Snapshot(state = State.SIGNED_OUT) }
        } catch (_: Exception) {
            // Transient failure (offline, server error, timeout). The tokens are
            // still valid, so keep the user logged in; live data refreshes on
            // next use.
            _snapshot.update { it.copy(state = State.READY) }
        }
    }

    fun noteRemoteChange() {
        _snapshot.update { it.copy(remoteChangeId = it.remoteChangeId + 1) }
    }

    /**
     * Keeps the distance widget in sync with what the Home map shows. The
     * widget itself is not ported yet, so this is where that wiring will hang.
     */
    fun publishDistance(km: Double?) {
        lastPublishedKm = km
    }

    @Volatile
    var lastPublishedKm: Double? = null
        private set
}
