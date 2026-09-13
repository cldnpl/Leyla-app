package com.claudianapolitano.leyla.feature.home

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.ApiException
import com.claudianapolitano.leyla.core.Coordinate
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.LocationRepository
import com.claudianapolitano.leyla.core.PartnerPrefs
import com.claudianapolitano.leyla.core.PartnerPronoun
import com.claudianapolitano.leyla.core.PartnerLocation
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.SharedConfig
import com.claudianapolitano.leyla.feature.cycle.CycleState
import com.claudianapolitano.leyla.feature.cycle.PregnancyEngine
import com.claudianapolitano.leyla.feature.cycle.PregnancyInsights
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Demo positions (SharedConfig.DEMO_MODE) so the map is visible before real
// location sharing is set up: Claudia in Naples, Alex in Tashkent.
private val SAMPLE_MINE = Coordinate(40.8518, 14.2681)
private val SAMPLE_PARTNER = Coordinate(41.2995, 69.2401)

/** Everything Home draws, already resolved — the composables do no lookups. */
data class HomeUiState(
    val myName: String = "",
    val partnerName: String = "",
    val myAvatarPath: String? = null,
    val partnerAvatarPath: String? = null,
    @param:StringRes val heroTitleRes: Int = R.string.hero_thinking_of_them,
    val missYouSent: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val mapMine: Coordinate? = null,
    val mapPartner: Coordinate? = null,
    val mapKm: Double? = null,
    val cycle: CycleState.Snapshot = CycleState.Snapshot(),
    val partnerPregnancyInsights: PregnancyInsights? = null,
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var partnerLocation: PartnerLocation? = null
    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                Session.snapshot,
                CycleState.snapshot,
                LocationRepository.currentLocation,
                LocationRepository.isSharing,
            ) { session, cycle, myLocation, isSharing ->
                Quad(session, cycle, myLocation.takeIf { isSharing }, Unit)
            }.collect { (session, cycle, realMine, _) ->
                _state.update { current ->
                    val mapMine = realMine ?: SAMPLE_MINE.takeIf { SharedConfig.DEMO_MODE }
                    val mapPartner = realPartner() ?: SAMPLE_PARTNER.takeIf { SharedConfig.DEMO_MODE }
                    current.copy(
                        myName = myName(session.user?.displayName),
                        partnerName = partnerName(session.partner?.displayName),
                        myAvatarPath = session.user?.avatarPath,
                        partnerAvatarPath = session.partner?.avatarPath,
                        heroTitleRes = heroTitleRes(),
                        mapMine = mapMine,
                        mapPartner = mapPartner,
                        mapKm = LocationRepository.kmBetween(mapMine, mapPartner),
                        cycle = cycle,
                        partnerPregnancyInsights = partnerPregnancyInsights(cycle),
                    )
                }
                // Keep the distance widget in sync with what the Home map shows
                // (real when available; the demo sample otherwise, so a test
                // widget matches the screen).
                Session.publishDistance(_state.value.mapKm)
            }
        }
    }

    /** Mirrors the iOS `.task` / `.onAppear` block on HomeView. */
    fun onAppear(context: Context) {
        LocationRepository.init(context)
        LocationRepository.refresh(context)
        viewModelScope.launch { loadPartnerLocation() }
        viewModelScope.launch { CycleState.refreshOnAppear() }
        startPolling()
    }

    /**
     * While Home is on screen, re-read the partner's position every minute so
     * the distance moves on its own — no pull-to-refresh, no reopening the app.
     */
    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                loadPartnerLocation()
            }
        }
    }

    private suspend fun loadPartnerLocation() {
        partnerLocation = runCatching { LeylaApi.partnerLocation() }.getOrNull()
        val mapMine = _state.value.mapMine
        val mapPartner = realPartner() ?: SAMPLE_PARTNER.takeIf { SharedConfig.DEMO_MODE }
        _state.update {
            it.copy(mapPartner = mapPartner, mapKm = LocationRepository.kmBetween(mapMine, mapPartner))
        }
        Session.publishDistance(_state.value.mapKm)
    }

    fun sendMissYou() {
        if (_state.value.isSending || _state.value.missYouSent) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, errorMessage = null) }
            try {
                LeylaApi.sendMissYou()
                _state.update { it.copy(isSending = false, missYouSent = true) }
                delay(2_000)
                _state.update { it.copy(missYouSent = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSending = false,
                        errorMessage = (e as? ApiException)?.message ?: e.localizedMessage,
                    )
                }
            }
        }
    }

    // Navigation targets that land with their own screens.
    fun onAddWidget() = Unit
    fun onEditProfile() = Unit
    fun onOpenMap() = Unit
    fun onOpenCycle() = Unit

    // MARK: - Copy

    private fun myName(displayName: String?): String =
        displayName ?: if (SharedConfig.DEMO_MODE) "Claudia" else ""

    private fun partnerName(displayName: String?): String {
        // Demo fallback so the sample map/copy reads "Alex".
        if (SharedConfig.DEMO_MODE && (displayName.isNullOrEmpty() || displayName == "Partner")) {
            return "Alex"
        }
        return displayName ?: ""
    }

    /**
     * Pronouns aren't isolated tokens across languages, so each pronoun gets its
     * own string. The whole sentence is translated as one unit instead of
     * stitching a translated template around an English "him"/"her"/"them".
     */
    @StringRes
    private fun heroTitleRes(): Int = when (PartnerPrefs.pronoun) {
        PartnerPronoun.SHE -> R.string.hero_thinking_of_her
        PartnerPronoun.HE -> R.string.hero_thinking_of_him
        PartnerPronoun.THEY -> R.string.hero_thinking_of_them
    }

    /** Real partner position — only while they're actually sharing. */
    private fun realPartner(): Coordinate? {
        val p = partnerLocation ?: return null
        if (!p.sharing) return null
        val lat = p.lat ?: return null
        val lng = p.lng ?: return null
        return Coordinate(lat, lng)
    }

    private fun partnerPregnancyInsights(cycle: CycleState.Snapshot): PregnancyInsights? {
        val pregnancy = cycle.partnerPregnancy ?: return null
        if (!pregnancy.sharing) return null
        val due = PregnancyEngine.parseDueDate(pregnancy.dueDate) ?: return null
        return PregnancyEngine.insights(due)
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
