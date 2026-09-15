package com.claudianapolitano.leyla.feature.together

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Couples Debate: you both argue the same motion, an AI judge compares the two
 * cases. Port of `DebateViews.swift`.
 */

// MARK: - Pack list

data class DebatePacksUiState(
    val packs: List<DebatePackSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "Your partner",
)

class DebatePacksViewModel : ViewModel() {

    private val _state = MutableStateFlow(DebatePacksUiState())
    val state: StateFlow<DebatePacksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Session.snapshot.collect { s ->
                _state.update { it.copy(partnerName = s.partner?.displayName ?: "Your partner") }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(packs = GamesApi.debatePacks(), errorMessage = null, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }
}

@Composable
fun DebatePackListScreen(
    onOpenPack: (DebatePackSummary) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebatePacksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize().background(colors.background)) {
        if (state.packs.isEmpty()) {
            LoadingOrError(state.isLoading, state.errorMessage, onRetry = viewModel::load)
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 20.dp, bottom = 60.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Choose a topic to debate!",
                            style = IOSText.title2.weight(FontWeight.Bold),
                            color = colors.ink,
                        )
                        Text(
                            "Pick a pack — you'll each argue a side and an AI judge crowns a winner.",
                            style = IOSText.subheadline,
                            color = colors.secondary,
                        )
                    }
                }
                items(state.packs, key = { it.id }) { pack ->
                    DebatePackCard(pack, state.partnerName) { onOpenPack(pack) }
                }
            }
        }
    }
}

@Composable
private fun DebatePackCard(pack: DebatePackSummary, partnerName: String, onClick: () -> Unit) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(pack.colorKey)
    GradientCard(colorKey = pack.colorKey, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuizIconTile(pack.icon, pack.colorKey)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(pack.title, style = IOSText.headline, color = colors.ink)
                    if (pack.tag.isNotEmpty()) {
                        Text(pack.tag, style = IOSText.caption2.weight(FontWeight.Bold), color = accent)
                    }
                }
                if (pack.isMyTurn) {
                    YourTurnHint(partnerName, accent)
                } else {
                    Text("${pack.roundCount} rounds", style = IOSText.caption, color = colors.secondary)
                }
            }
            Spacer(Modifier.width(10.dp))
            PackStatusBadge(
                bothDone = pack.bothDone,
                myDone = pack.myDone,
                accent = accent,
                doneLabel = "Results",
                doneIcon = Icons.Filled.EmojiEvents,
            )
        }
    }
}

// MARK: - Play

data class DebatePlayUiState(
    val pack: DebatePackDetail? = null,
    val index: Int = 0,
    val draft: String = "",
    val showResults: Boolean = false,
    val submitting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "your partner",
) {
    val isLast: Boolean get() = index == ((pack?.rounds?.size ?: 1) - 1)
}

class DebatePlayViewModel(private val packId: String) : ViewModel() {

    private val _state = MutableStateFlow(DebatePlayUiState())
    val state: StateFlow<DebatePlayUiState> = _state.asStateFlow()

    /**
     * Set the first time the results are shown with both partners done, so
     * markDebatePackSeen fires exactly once per visit. Once both users have hit
     * that endpoint the server retires the round and hands out fresh motions.
     */
    private var markedSeen = false

    init {
        viewModelScope.launch {
            Session.snapshot.collect { s ->
                _state.update { it.copy(partnerName = s.partner?.displayName ?: "your partner") }
            }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val pack = GamesApi.debatePack(packId)
                val firstOpen = pack.rounds.indexOfFirst { it.myArgument == null }
                _state.update {
                    it.copy(
                        pack = pack,
                        showResults = pack.myDone,
                        index = if (!pack.myDone && firstOpen >= 0) firstOpen else it.index,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                syncDraft()
                maybeMarkSeen()
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { GamesApi.debatePack(packId) }.getOrNull()?.let { fresh ->
                _state.update { it.copy(pack = fresh) }
                maybeMarkSeen()
            }
        }
    }

    fun type(text: String) {
        _state.update { it.copy(draft = text) }
    }

    fun back() {
        if (_state.value.index == 0) return
        _state.update { it.copy(index = it.index - 1) }
        syncDraft()
    }

    fun next(onSaved: () -> Unit = {}) {
        val current = _state.value
        val argument = current.draft.trim()
        val pack = current.pack ?: return
        if (argument.isEmpty() || current.submitting) return

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, errorMessage = null) }
            try {
                GamesApi.argueDebate(packId, pack.rounds[current.index].id, argument)
                val fresh = GamesApi.debatePack(packId)
                if (current.isLast) {
                    _state.update { it.copy(pack = fresh, showResults = true, submitting = false) }
                    maybeMarkSeen()
                } else {
                    _state.update { it.copy(pack = fresh, index = it.index + 1, submitting = false) }
                    syncDraft()
                }
                onSaved()
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, errorMessage = describe(e)) }
            }
        }
    }

    private fun syncDraft() {
        _state.update { current ->
            current.copy(draft = current.pack?.rounds?.getOrNull(current.index)?.myArgument.orEmpty())
        }
    }

    private fun maybeMarkSeen() {
        val current = _state.value
        if (markedSeen || !current.showResults || current.pack?.bothDone != true) return
        markedSeen = true
        viewModelScope.launch { runCatching { GamesApi.markDebatePackSeen(packId) } }
    }
}

@Composable
fun DebatePlayScreen(
    packId: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    viewModel: DebatePlayViewModel = viewModel(key = "debate-$packId") { DebatePlayViewModel(packId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(colorKey)
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current

    Box(modifier.fillMaxSize().background(colors.background)) {
        when {
            state.pack == null -> LoadingOrError(state.isLoading, state.errorMessage)
            state.showResults -> DebateResults(state, colorKey, accent, viewModel::reload)
            else -> DebateRoundScreen(
                state = state,
                accent = accent,
                onType = viewModel::type,
                onBack = { focusManager.clearFocus(); viewModel.back() },
                onNext = {
                    focusManager.clearFocus()
                    viewModel.next { haptics.success() }
                },
            )
        }
    }
}

@Composable
private fun DebateRoundScreen(
    state: DebatePlayUiState,
    accent: Color,
    onType: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val pack = state.pack ?: return
    val round = pack.rounds.getOrNull(state.index.coerceAtMost(pack.rounds.size - 1)) ?: return

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StepDots(
                total = pack.rounds.size,
                index = state.index,
                accent = accent,
                isDone = { pack.rounds.getOrNull(it)?.myArgument != null },
                modifier = Modifier.padding(top = 12.dp),
            )

            // Both of you get this exact prompt — nobody is handed a side, so
            // the judge is comparing two answers to the same question rather
            // than to opposite ones.
            Text(
                "YOU BOTH ARGUE THIS",
                style = IOSText.caption.weight(FontWeight.Bold).copy(letterSpacing = 1.sp),
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Theme.coral)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )

            Text(
                "“${round.motion}”",
                style = IOSText.title2.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )

            Text(
                "Agree or disagree — make your case. ${state.partnerName} answers the same one, " +
                    "and the judge picks the better argument.",
                style = IOSText.subheadline,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )

            AnswerField(
                value = state.draft,
                onValueChange = onType,
                enabled = !state.submitting,
                placeholder = "Make your case…",
                minHeight = 160.dp,
            )

            if (state.errorMessage != null) {
                Text(state.errorMessage, style = IOSText.footnote, color = errorRed())
            }
        }

        PlayNavBar(
            showBack = state.index > 0,
            nextLabel = if (state.isLast) "Finish" else "Next",
            nextIcon = if (state.isLast) Icons.Filled.Check else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            accent = accent,
            enabled = state.draft.isNotBlank(),
            submitting = state.submitting,
            onBack = onBack,
            onNext = onNext,
        )
    }
}

@Composable
private fun DebateResults(
    state: DebatePlayUiState,
    colorKey: String,
    accent: Color,
    onRefresh: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val pack = state.pack ?: return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (pack.bothDone) {
            Icon(
                crownIcon(pack.overallWinner),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.padding(top = 12.dp).size(54.dp),
            )
            Text(
                crownTitle(pack.overallWinner, state.partnerName),
                style = IOSText.title2.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                "${pack.myWins}–${pack.partnerWins} · you vs ${state.partnerName}",
                style = IOSText.subheadline,
                color = colors.secondary,
            )
            pack.rounds.forEach { DebateRevealRow(it, colorKey, accent, state.partnerName) }
        } else {
            QuizIconTile(
                Icons.Filled.HourglassTop,
                colorKey,
                size = 72.dp,
                modifier = Modifier.padding(top = 40.dp),
            )
            Text("Waiting for the rebuttal", style = IOSText.title2.weight(FontWeight.Bold), color = colors.ink)
            Text(
                "Your case is locked in! The judge compares it with ${state.partnerName}'s answer " +
                    "to the same prompt once they've argued too.",
                style = IOSText.subheadline,
                color = colors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            pack.rounds.forEach { DebatePendingRow(it, colorKey) }
        }

        TextAction(
            "Refresh",
            onRefresh,
            icon = Icons.Filled.Refresh,
            color = accent,
            style = IOSText.subheadline.weight(FontWeight.Bold),
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun DebateRevealRow(
    round: DebateRound,
    colorKey: String,
    accent: Color,
    partnerName: String,
) {
    val colors = LeylaTheme.colors
    val iWon = round.roundWinner == "me"
    GradientCard(
        colorKey = colorKey,
        cornerRadius = 18.dp,
        contentPadding = 14.dp,
        alpha = if (iWon) 0.6f else 0.3f,
    ) {
        Text("“${round.motion}”", style = IOSText.subheadline.weight(FontWeight.Bold), color = colors.ink)
        Spacer(Modifier.height(10.dp))
        DebateArgumentBlock("You", round.myArgument, round.myScore, round.roundWinner == "me", accent)
        Spacer(Modifier.height(10.dp))
        DebateArgumentBlock(
            partnerName,
            round.partnerArgument,
            round.partnerScore,
            round.roundWinner == "partner",
            accent,
        )
        round.verdict?.let { verdict ->
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    when {
                        round.roundWinner == "tie" -> Icons.Filled.AllInclusive
                        iWon -> Icons.Filled.EmojiEvents
                        else -> Icons.Filled.Flag
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                Text(verdict, style = IOSText.footnote, color = colors.secondary)
            }
        }
    }
}

@Composable
private fun DebatePendingRow(round: DebateRound, colorKey: String) {
    val colors = LeylaTheme.colors
    GradientCard(colorKey = colorKey, cornerRadius = 18.dp, contentPadding = 14.dp, alpha = 0.3f) {
        Text("“${round.motion}”", style = IOSText.subheadline.weight(FontWeight.Bold), color = colors.ink)
        Spacer(Modifier.height(6.dp))
        Text("Your case", style = IOSText.caption2.weight(FontWeight.Bold), color = colors.secondary)
        Text(round.myArgument ?: "—", style = IOSText.footnote, color = colors.ink)
    }
}

/**
 * One side's case in the results reveal: who argued, the judge's score, the
 * text. Port of `DebateArgumentBlock`.
 */
@Composable
fun DebateArgumentBlock(
    title: String,
    text: String?,
    score: Int?,
    highlight: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = colors.surface.alpha * if (highlight) 1f else 0.5f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = IOSText.caption2.weight(FontWeight.Bold), color = colors.secondary)
            Spacer(Modifier.weight(1f))
            if (score != null) {
                Text(
                    "$score/10",
                    style = IOSText.caption2.weight(FontWeight.Bold),
                    color = if (highlight) accent else colors.secondary,
                )
            }
        }
        Text(text ?: "—", style = IOSText.footnote, color = colors.ink)
    }
}

private fun crownIcon(winner: String?): ImageVector = when (winner) {
    "me" -> Icons.Filled.EmojiEvents
    "partner" -> Icons.Filled.Flag
    else -> Icons.Filled.AllInclusive
}

private fun crownTitle(winner: String?, partnerName: String): String = when (winner) {
    "me" -> "You won the debate! 🏆"
    "partner" -> "$partnerName took this one 😅"
    else -> "It's a draw — great match! 🤝"
}
