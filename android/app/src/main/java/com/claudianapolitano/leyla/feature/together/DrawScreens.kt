package com.claudianapolitano.leyla.feature.together

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.Avatar
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Draw Together: one shared prompt, two drawings, revealed side by side once
 * both are in. Port of `DrawViews.swift`.
 */

data class DrawUiState(
    val round: DrawRound? = null,
    val isLoading: Boolean = true,
    val submitting: Boolean = false,
    val errorMessage: String? = null,
    val partnerName: String = "your partner",
    val myAvatarPath: String? = null,
    val partnerAvatarPath: String? = null,
)

class DrawViewModel : ViewModel() {

    private val _state = MutableStateFlow(DrawUiState())
    val state: StateFlow<DrawUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            Session.snapshot.collect { s ->
                _state.update {
                    it.copy(
                        partnerName = s.partner?.displayName ?: "your partner",
                        myAvatarPath = s.user?.avatarPath,
                        partnerAvatarPath = s.partner?.avatarPath,
                    )
                }
            }
        }
        load()
        startPolling()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(round = GamesApi.draw(), isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { GamesApi.draw() }.getOrNull()?.let { fresh ->
                _state.update { it.copy(round = fresh) }
            }
        }
    }

    /**
     * Every few seconds, re-fetch the current round so the screen follows the
     * partner on its own: a fresh round they started replaces the local state
     * even while this phone is on the drawing pad, and their submission flips
     * this phone from "waiting" to the reveal.
     *
     * Any change is taken, not just a new round id. iOS gets the reveal from a
     * push waking its `remoteChangeID` reload; there is no push on Android yet,
     * so watching only the id would leave someone stuck on the waiting screen
     * with nothing but "Check again" to get them out of it.
     */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(5_000)
                val latest = runCatching { GamesApi.draw() }.getOrNull() ?: continue
                if (latest != _state.value.round) {
                    _state.update { it.copy(round = latest) }
                }
            }
        }
    }

    fun submit(jpeg: ByteArray, onSaved: () -> Unit = {}) {
        val roundId = _state.value.round?.roundId ?: return
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, errorMessage = null) }
            try {
                _state.update { it.copy(round = GamesApi.submitDraw(jpeg, roundId), submitting = false) }
                onSaved()
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, errorMessage = describe(e)) }
            }
        }
    }

    fun newRound(force: Boolean = false, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(round = GamesApi.newDrawRound(force), errorMessage = null) }
                onStarted()
            } catch (e: Exception) {
                // The partner may still be working in the existing round.
                // Re-read it so this phone enters the waiting state instead of
                // offering a second, overlapping drawing.
                val message = describe(e)
                runCatching { GamesApi.draw() }.getOrNull()?.let { fresh ->
                    _state.update { it.copy(round = fresh) }
                }
                _state.update { it.copy(errorMessage = message) }
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun DrawTogetherScreen(
    modifier: Modifier = Modifier,
    viewModel: DrawViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val haptics = rememberHaptics()
    var confirmForceNewRound by remember { mutableStateOf(false) }
    val round = state.round

    Box(modifier.fillMaxSize().background(colors.background)) {
        when {
            round == null -> LoadingOrError(state.isLoading, state.errorMessage, onRetry = viewModel::reload)

            round.mySubmitted -> DrawRevealScreen(
                state = state,
                round = round,
                onNewRound = { viewModel.newRound { haptics.lightTap() } },
                onReload = viewModel::reload,
                onStartOver = { confirmForceNewRound = true },
            )

            // Keyed to the round: starting a fresh prompt must hand back a
            // blank sheet, not the strokes from the round that just ended.
            else -> key(round.roundId) {
                DrawingPad(
                    prompt = round.prompt,
                    submitting = state.submitting,
                    onSubmit = { jpeg -> viewModel.submit(jpeg) { haptics.success() } },
                )
            }
        }
    }

    if (confirmForceNewRound) {
        AlertDialog(
            onDismissRequest = { confirmForceNewRound = false },
            title = { Text("Start a new drawing?") },
            text = { Text("This will end the current round for both of you and pick a new prompt.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForceNewRound = false
                    viewModel.newRound(force = true) { haptics.lightTap() }
                }) {
                    Text("Start over", color = Theme.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmForceNewRound = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DrawRevealScreen(
    state: DrawUiState,
    round: DrawRound,
    onNewRound: () -> Unit,
    onReload: () -> Unit,
    onStartOver: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent("purple")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "“${round.prompt}”",
            style = IOSText.title3.weight(FontWeight.Bold),
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (round.revealed) {
            Text("The big reveal! 🎨", style = IOSText.headline, color = accent)
            ArtworkCard("You", round.myImagePath, state.myAvatarPath, "purple")
            ArtworkCard(state.partnerName, round.partnerImagePath, state.partnerAvatarPath, "purple")
        } else {
            QuizIconTile(
                Icons.Filled.HourglassTop,
                "purple",
                size = 64.dp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Waiting for ${state.partnerName}'s drawing",
                style = IOSText.title3.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                "Your drawing is locked in. We'll show both drawings once ${state.partnerName} finishes.",
                style = IOSText.subheadline,
                color = colors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            ArtworkCard("Your drawing", round.myImagePath, state.myAvatarPath, "purple")
        }

        if (state.errorMessage != null) {
            Text(state.errorMessage, style = IOSText.footnote, color = errorRed(), textAlign = TextAlign.Center)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            if (round.revealed) {
                PillButton(
                    text = "New drawing",
                    color = accent,
                    onClick = onNewRound,
                    icon = Icons.Filled.Refresh,
                )
            } else {
                TextAction("Check again", onReload, icon = Icons.Filled.Refresh, color = accent)
                // Escape hatch: without this the only way out of the waiting
                // state is the partner submitting their drawing.
                TextAction("Start a new drawing", onStartOver, icon = Icons.Filled.Refresh)
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

/** One square drawing or photo with its author's avatar above it. */
@Composable
internal fun ArtworkCard(
    title: String,
    path: String?,
    avatarPath: String?,
    colorKey: String,
    modifier: Modifier = Modifier,
    alpha: Float = 0.4f,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LeylaTheme.colors
    GradientCard(
        colorKey = colorKey,
        modifier = modifier,
        cornerRadius = 20.dp,
        contentPadding = 12.dp,
        alpha = alpha,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(path = avatarPath, name = title, size = 22.dp)
            Text(title, style = IOSText.caption.weight(FontWeight.Bold), color = colors.secondary)
            if (trailing != null) {
                Spacer(Modifier.weight(1f))
                trailing()
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                // Drawings are exported on a white sheet, so the plate behind
                // them stays white in both schemes rather than showing a dark
                // rim around the artwork.
                .background(Color.White),
        ) {
            if (path != null) {
                RemoteImage(path, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
