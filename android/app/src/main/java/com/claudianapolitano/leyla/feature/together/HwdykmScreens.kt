package com.claudianapolitano.leyla.feature.together

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * How Well Do You Know Me?: answer some questions honestly, guess your
 * partner's answers to the rest, then compare. Port of `HwdykmViews.swift`.
 */

// MARK: - Pack list

data class HwdykmPacksUiState(
    val packs: List<HwdykmPackSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "Your partner",
)

class HwdykmPacksViewModel : ViewModel() {

    private val _state = MutableStateFlow(HwdykmPacksUiState())
    val state: StateFlow<HwdykmPacksUiState> = _state.asStateFlow()

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
                _state.update { it.copy(packs = GamesApi.hwdykmPacks(), errorMessage = null, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }
}

@Composable
fun HwdykmPackListScreen(
    onOpenPack: (HwdykmPackSummary) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HwdykmPacksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors

    // Reloaded on every entry: a pack finished a moment ago rotates server-side,
    // and coming back should show that rather than the stale list.
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
                items(state.packs, key = { it.id }) { pack ->
                    HwdykmPackCard(pack, state.partnerName) { onOpenPack(pack) }
                }
            }
        }
    }
}

@Composable
private fun HwdykmPackCard(pack: HwdykmPackSummary, partnerName: String, onClick: () -> Unit) {
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
                    Text(
                        "${pack.questionCount} questions",
                        style = IOSText.caption,
                        color = colors.secondary,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            PackStatusBadge(
                bothDone = pack.bothDone,
                myDone = pack.myDone,
                accent = accent,
                doneLabel = "Results",
                doneIcon = Icons.Filled.CheckCircle,
            )
        }
    }
}

/** "Results" / "Waiting" / chevron, shared by the know-me and debate pack rows. */
@Composable
internal fun PackStatusBadge(
    bothDone: Boolean,
    myDone: Boolean,
    accent: Color,
    doneLabel: String,
    doneIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val colors = LeylaTheme.colors
    when {
        bothDone -> Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(doneIcon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Text(doneLabel, style = IOSText.caption.weight(FontWeight.Bold), color = accent)
        }

        myDone -> Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.HourglassEmpty,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp),
            )
            Text("Waiting", style = IOSText.caption.weight(FontWeight.Bold), color = accent)
        }

        else -> Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.secondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

// MARK: - Play

data class HwdykmPlayUiState(
    val pack: HwdykmPackDetail? = null,
    val index: Int = 0,
    val selection: String? = null,
    val showResults: Boolean = false,
    val submitting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "your partner",
) {
    val isLast: Boolean get() = index == ((pack?.questions?.size ?: 1) - 1)
}

class HwdykmPlayViewModel(private val packId: String) : ViewModel() {

    private val _state = MutableStateFlow(HwdykmPlayUiState())
    val state: StateFlow<HwdykmPlayUiState> = _state.asStateFlow()

    /**
     * Set the first time the results are actually shown with both partners done,
     * so the "mark this round as seen" call fires exactly once per visit — the
     * server rotates the pack when both users have done that.
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
                val pack = GamesApi.hwdykmPack(packId)
                val firstOpen = pack.questions.indexOfFirst { it.myAnswer == null }
                _state.update {
                    it.copy(
                        pack = pack,
                        showResults = pack.myDone,
                        index = if (!pack.myDone && firstOpen >= 0) firstOpen else it.index,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                syncSelection()
                maybeMarkSeen()
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { GamesApi.hwdykmPack(packId) }.getOrNull()?.let { fresh ->
                _state.update { it.copy(pack = fresh) }
                maybeMarkSeen()
            }
        }
    }

    fun select(optionId: String) {
        _state.update { it.copy(selection = if (it.selection == optionId) null else optionId) }
    }

    fun back() {
        if (_state.value.index == 0) return
        _state.update { it.copy(index = it.index - 1) }
        syncSelection()
    }

    fun next(onSaved: () -> Unit = {}) {
        val current = _state.value
        val answer = current.selection.orEmpty()
        val pack = current.pack ?: return
        if (answer.isEmpty() || current.submitting) return

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, errorMessage = null) }
            try {
                GamesApi.answerHwdykm(packId, pack.questions[current.index].id, answer)
                val fresh = GamesApi.hwdykmPack(packId)
                if (current.isLast) {
                    _state.update { it.copy(pack = fresh, showResults = true, submitting = false) }
                    maybeMarkSeen()
                } else {
                    _state.update { it.copy(pack = fresh, index = it.index + 1, submitting = false) }
                    syncSelection()
                }
                onSaved()
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, errorMessage = describe(e)) }
            }
        }
    }

    private fun syncSelection() {
        _state.update { current ->
            current.copy(selection = current.pack?.questions?.getOrNull(current.index)?.myAnswer)
        }
    }

    /**
     * Ticks the server-side "both users have seen the results" gate the moment
     * the reveal is on screen with both partners done. Once both tick it, the
     * pack rotates and the next fetch returns brand-new questions.
     */
    private fun maybeMarkSeen() {
        val current = _state.value
        if (markedSeen || !current.showResults || current.pack?.bothDone != true) return
        markedSeen = true
        viewModelScope.launch { runCatching { GamesApi.markHwdykmPackSeen(packId) } }
    }
}

@Composable
fun HwdykmPlayScreen(
    packId: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    viewModel: HwdykmPlayViewModel = viewModel(key = "hwdykm-$packId") { HwdykmPlayViewModel(packId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(colorKey)
    val haptics = rememberHaptics()

    Box(modifier.fillMaxSize().background(colors.background)) {
        when {
            state.pack == null -> LoadingOrError(state.isLoading, state.errorMessage)
            state.showResults -> HwdykmResults(state, colorKey, accent, viewModel::reload)
            else -> HwdykmQuestionScreen(
                state = state,
                accent = accent,
                onSelect = { haptics.lightTap(); viewModel.select(it) },
                onBack = viewModel::back,
                onNext = { viewModel.next { haptics.success() } },
            )
        }
    }
}

@Composable
private fun HwdykmQuestionScreen(
    state: HwdykmPlayUiState,
    accent: Color,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val pack = state.pack ?: return
    val question = pack.questions.getOrNull(state.index.coerceAtMost(pack.questions.size - 1)) ?: return

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
                total = pack.questions.size,
                index = state.index,
                accent = accent,
                isDone = { pack.questions.getOrNull(it)?.myAnswer != null },
                modifier = Modifier.padding(top = 12.dp),
            )

            HwdykmRolePill(question.subjectIsMe, state.partnerName)

            Text(
                question.prompt,
                style = IOSText.title2.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )

            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                question.options.forEach { option ->
                    HwdykmOptionRow(
                        text = option.label,
                        selected = state.selection == option.id,
                        accent = accent,
                        onClick = { if (!state.submitting) onSelect(option.id) },
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(state.errorMessage, style = IOSText.footnote, color = errorRed())
            }
        }

        PlayNavBar(
            showBack = state.index > 0,
            nextLabel = if (state.isLast) "Finish" else "Next",
            nextIcon = if (state.isLast) Icons.Filled.Check else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            accent = accent,
            enabled = state.selection != null,
            submitting = state.submitting,
            onBack = onBack,
            onNext = onNext,
        )
    }
}

@Composable
private fun HwdykmResults(
    state: HwdykmPlayUiState,
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
            val matches = pack.questions.count { it.matched }
            ScoreRing(pack.score, accent, Modifier.padding(top = 12.dp))
            Text(
                when {
                    pack.score >= 70 -> "You really know each other! 💖"
                    pack.score >= 40 -> "Not bad — room to learn 😊"
                    else -> "Opposites attract 😅"
                },
                style = IOSText.headline,
                color = colors.ink,
            )
            Text(
                "Matched on $matches of ${pack.questions.size}",
                style = IOSText.subheadline,
                color = colors.secondary,
            )
            pack.questions.forEach { HwdykmRevealRow(it, colorKey, state.partnerName) }
        } else {
            QuizIconTile(
                Icons.Filled.HourglassTop,
                colorKey,
                size = 72.dp,
                modifier = Modifier.padding(top = 40.dp),
            )
            Text("Waiting for results", style = IOSText.title2.weight(FontWeight.Bold), color = colors.ink)
            Text(
                "You're all locked in! We'll reveal your compatibility once " +
                    "${state.partnerName} finishes this pack too.",
                style = IOSText.subheadline,
                color = colors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
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
private fun HwdykmRevealRow(question: HwdykmQuestion, colorKey: String, partnerName: String) {
    val colors = LeylaTheme.colors
    GradientCard(
        colorKey = colorKey,
        cornerRadius = 18.dp,
        contentPadding = 14.dp,
        alpha = if (question.matched) 0.6f else 0.3f,
    ) {
        Text(question.prompt, style = IOSText.subheadline.weight(FontWeight.Bold), color = colors.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            if (question.subjectIsMe) "About you" else "About $partnerName",
            style = IOSText.caption2.weight(FontWeight.Bold),
            color = colors.secondary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (question.matched) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (question.matched) Theme.coral else colors.secondary,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Real answer: ${question.option(question.honestAnswer)?.label ?: "—"}",
                    style = IOSText.footnote,
                    color = colors.ink,
                )
                Text(
                    "Guess: ${question.option(question.guess)?.label ?: "—"}",
                    style = IOSText.footnote,
                    color = colors.secondary,
                )
            }
        }
    }
}

// MARK: - Small pieces

/** "ANSWER HONESTLY" / "GUESS <partner>'S ANSWER" banner above the prompt. */
@Composable
fun HwdykmRolePill(subjectIsMe: Boolean, partnerName: String, modifier: Modifier = Modifier) {
    Text(
        if (subjectIsMe) "ANSWER HONESTLY" else "GUESS ${partnerName.uppercase()}'S ANSWER",
        style = IOSText.caption.weight(FontWeight.Bold).copy(letterSpacing = 1.sp),
        color = Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(if (subjectIsMe) Theme.coral else Theme.rose)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
fun HwdykmOptionRow(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent else colors.card)
            .plainClickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = IOSText.headline,
            color = if (selected) Color.White else colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** The big "%d% match" dial on the reveal screen. */
@Composable
fun ScoreRing(score: Int, color: Color, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    Box(modifier.size(130.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = color,
                // -90° so the dial starts at twelve o'clock, like the iOS ring.
                startAngle = -90f,
                sweepAngle = 360f * (score.coerceIn(0, 100) / 100f).coerceAtLeast(0.001f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$score%",
                style = IOSText.title.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
                color = colors.ink,
            )
            Text("match", style = IOSText.caption, color = colors.secondary)
        }
    }
}
