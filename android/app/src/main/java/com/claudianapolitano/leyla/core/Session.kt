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
            _snapshot.update { it.copy(user = user) }
            loadCouple()
        } catch (_: UnauthorizedException) {
            // The refresh token was rejected: the only unrecoverable case, and
            // the only place bootstrap is allowed to drop the session.
            TokenStore.clear()
            _snapshot.update { Snapshot(state = State.SIGNED_OUT) }
        } catch (_: Exception) {
            // Transient failure (offline, server error, timeout). The tokens are
            // still valid, so keep the user logged in; live data refreshes on
            // next use.
            if (!restoreTestPairingIfNeeded()) {
                _snapshot.update { it.copy(state = State.READY) }
            }
        }
    }

    // MARK: - Sign in / out

    /** Stores the new session and routes to whatever step comes next. */
    suspend fun handleAuth(auth: AuthResponse) {
        TokenStore.save(auth.accessToken, auth.refreshToken)
        PartnerPrefs.pronoun = PartnerPronoun.from(auth.user.partnerPronoun)
        _snapshot.update { it.copy(user = auth.user) }
        loadCouple()
    }

    /**
     * Fetches the couple and routes: paired goes to Home, genuinely unpaired
     * goes to the step still owing. A network failure falls back to the demo
     * pairing rather than throwing someone back to the sign-in screen.
     */
    suspend fun loadCouple() {
        try {
            val couple = LeylaApi.getCouple()
            if (couple.paired && couple.couple != null) {
                // A real couple always wins over the demo bypass.
                AppPrefs.testPaired = false
                _snapshot.update {
                    it.copy(state = State.READY, partner = couple.partner, couple = couple.couple)
                }
                return
            }
            if (restoreTestPairingIfNeeded()) return
            _snapshot.update { it.copy(state = nextOnboardingState(), partner = null, couple = null) }
        } catch (_: Exception) {
            if (restoreTestPairingIfNeeded()) return
            _snapshot.update { it.copy(state = nextOnboardingState()) }
        }
    }

    suspend fun signOut() {
        // Retire the refresh token server-side before dropping it locally;
        // afterwards there is nothing left to present.
        TokenStore.refreshToken?.let { rt -> runCatching { LeylaApi.logout(rt) } }
        TokenStore.clear()
        AppPrefs.clearAccountState()
        _snapshot.value = Snapshot(state = State.SIGNED_OUT)
    }

    // MARK: - Onboarding

    private fun nextOnboardingState(): State =
        if (AppPrefs.personalOnboardingDone) State.NEEDS_PAIRING else State.NEEDS_PERSONAL_ONBOARDING

    /** Marks the "about you" step complete and advances to pairing. */
    fun completePersonalOnboarding() {
        AppPrefs.personalOnboardingDone = true
        _snapshot.update { it.copy(state = State.NEEDS_PAIRING) }
    }

    /** Saves the chosen display name to the backend. */
    suspend fun updateName(displayName: String) {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty()) return
        runCatching { LeylaApi.updateDisplayName(trimmed) }.getOrNull()?.let(::updateUser)
    }

    // MARK: - Demo pairing

    /**
     * TEST ONLY ([SharedConfig.DEMO_MODE]): the "0000" code opens the app
     * without a real partner so the UI can be exercised end to end.
     */
    fun enterTestPairing() {
        AppPrefs.testPaired = true
        _snapshot.update {
            it.copy(
                state = State.READY,
                partner = User(id = "test-partner", displayName = "Partner"),
                couple = Couple(id = "test-couple", status = "active"),
            )
        }
    }

    /** Restores a persisted demo pairing so relaunch goes straight to Home. */
    private fun restoreTestPairingIfNeeded(): Boolean {
        if (!SharedConfig.DEMO_MODE || !AppPrefs.testPaired) return false
        enterTestPairing()
        return true
    }

    /** Publishes a freshly saved account (name, avatar, cycle settings). */
    fun updateUser(user: User) {
        PartnerPrefs.pronoun = PartnerPronoun.from(user.partnerPronoun)
        _snapshot.update { it.copy(user = user) }
    }

    /**
     * Changes how the app refers to the partner. Stored on the account so a
     * reinstall keeps it, and cached locally so the UI switches at once.
     */
    suspend fun setPartnerPronoun(pronoun: PartnerPronoun) {
        PartnerPrefs.pronoun = pronoun
        runCatching { LeylaApi.updatePartnerPronoun(pronoun.wire) }.getOrNull()?.let(::updateUser)
    }

    /** Saves the day the relationship started, which Home counts from. */
    suspend fun saveStartDate(isoDay: String) {
        val couple = runCatching { LeylaApi.updateCoupleStartDate(isoDay) }.getOrNull() ?: return
        _snapshot.update { it.copy(couple = couple.couple ?: it.couple) }
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
