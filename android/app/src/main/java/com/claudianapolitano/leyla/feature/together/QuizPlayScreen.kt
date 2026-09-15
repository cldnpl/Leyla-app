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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
 * Play flow for a quiz: pick (or write) an answer per question — the choice is
 * changeable until you tap "Next". Answers are saved as you advance. After the
 * last question you land on a results screen: a full comparison once your
 * partner has finished too, otherwise a "waiting for results" state. Port of
 * `QuizPlayView.swift`.
 */

data class QuizPlayUiState(
    val quiz: QuizDetail? = null,
    val index: Int = 0,
    /** The current question's pending pick (option id, or the typed text). */
    val selection: String? = null,
    val draft: String = "",
    val showResults: Boolean = false,
    val submitting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "your partner",
) {
    val question: QuizQuestion?
        get() = quiz?.questions?.getOrNull(index.coerceAtMost((quiz.questions.size - 1).coerceAtLeast(0)))

    val isLast: Boolean get() = index == ((quiz?.questions?.size ?: 1) - 1)

    val ready: Boolean get() = !selection.isNullOrBlank()
}

class QuizPlayViewModel(private val quizId: String) : ViewModel() {

    private val _state = MutableStateFlow(QuizPlayUiState())
    val state: StateFlow<QuizPlayUiState> = _state.asStateFlow()

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
                val quiz = GamesApi.quiz(quizId)
                val allAnswered = quiz.questions.all { it.myAnswer != null }
                val firstOpen = quiz.questions.indexOfFirst { it.myAnswer == null }
                _state.update {
                    it.copy(
                        quiz = quiz,
                        // Already completed → straight to results.
                        showResults = allAnswered,
                        index = if (!allAnswered && firstOpen >= 0) firstOpen else it.index,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                syncSelection()
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            runCatching { GamesApi.quiz(quizId) }.getOrNull()?.let { fresh ->
                _state.update { it.copy(quiz = fresh) }
            }
        }
    }

    fun select(optionId: String) {
        _state.update { it.copy(selection = if (it.selection == optionId) null else optionId) }
    }

    fun type(text: String) {
        _state.update { it.copy(draft = text, selection = text.trim().ifEmpty { null }) }
    }

    fun back() {
        val current = _state.value
        if (current.index == 0) return
        _state.update { it.copy(index = it.index - 1) }
        syncSelection()
    }

    fun next(onFinished: () -> Unit = {}) {
        val current = _state.value
        val answer = current.selection?.trim().orEmpty()
        val quiz = current.quiz ?: return
        if (answer.isEmpty() || current.submitting) return

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, errorMessage = null) }
            try {
                val question = quiz.questions[current.index]
                GamesApi.answerQuiz(quizId, question.id, answer)
                // Keep the stored answers fresh, so Back shows what was saved.
                val fresh = GamesApi.quiz(quizId)
                if (current.isLast) {
                    _state.update { it.copy(quiz = fresh, showResults = true, submitting = false) }
                    onFinished()
                } else {
                    _state.update { it.copy(quiz = fresh, index = it.index + 1, submitting = false) }
                    syncSelection()
                    onFinished()
                }
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, errorMessage = describe(e)) }
            }
        }
    }

    fun reviewAnswers() {
        _state.update { it.copy(index = 0, showResults = false) }
        syncSelection()
    }

    /**
     * Loads the answer already stored for the question now on screen — empty for
     * one not answered yet. Always called when the index changes.
     */
    private fun syncSelection() {
        _state.update { current ->
            val stored = current.quiz?.questions?.getOrNull(current.index)?.myAnswer
            current.copy(selection = stored, draft = stored.orEmpty())
        }
    }
}

@Composable
fun QuizPlayScreen(
    quizId: String,
    colorKey: String,
    modifier: Modifier = Modifier,
    viewModel: QuizPlayViewModel = viewModel(key = "quiz-play-$quizId") { QuizPlayViewModel(quizId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(colorKey)
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current

    Box(modifier.fillMaxSize().background(colors.background)) {
        when {
            state.quiz == null -> LoadingOrError(state.isLoading, state.errorMessage)
            state.showResults -> QuizResults(state, colorKey, accent, viewModel)
            else -> QuizQuestionScreen(
                state = state,
                colorKey = colorKey,
                accent = accent,
                onSelect = { haptics.lightTap(); viewModel.select(it) },
                onType = viewModel::type,
                onBack = { focusManager.clearFocus(); viewModel.back() },
                onNext = {
                    // Hand the keyboard back before the question changes, so the
                    // field that held this answer is gone by the time the next
                    // one is built.
                    focusManager.clearFocus()
                    viewModel.next { haptics.success() }
                },
            )
        }
    }
}

@Composable
private fun QuizQuestionScreen(
    state: QuizPlayUiState,
    colorKey: String,
    accent: Color,
    onSelect: (String) -> Unit,
    onType: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = LeylaTheme.colors
    val quiz = state.quiz ?: return
    val question = state.question ?: return

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            QuizIconTile(quiz.icon, colorKey, size = 60.dp, modifier = Modifier.padding(top = 8.dp))

            StepDots(
                total = quiz.questions.size,
                index = state.index,
                accent = accent,
                isDone = { quiz.questions.getOrNull(it)?.myAnswer != null },
            )

            Text(
                "Question ${state.index + 1} of ${quiz.questions.size}",
                style = IOSText.caption.weight(FontWeight.Bold),
                color = colors.secondary,
            )

            Text(
                question.prompt,
                style = IOSText.title2.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )

            QuizInputSection(
                question = question,
                colorKey = colorKey,
                selection = state.selection,
                draft = state.draft,
                enabled = !state.submitting,
                onSelect = onSelect,
                onType = onType,
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
            enabled = state.ready,
            submitting = state.submitting,
            onBack = onBack,
            onNext = onNext,
        )
    }
}

/** The per-question input: photo cards, icon rows, or a free-text field. */
@Composable
fun QuizInputSection(
    question: QuizQuestion,
    colorKey: String,
    selection: String?,
    draft: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = question.options
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (question.hasPhotos) 14.dp else 12.dp),
    ) {
        if (question.isChoice && options != null) {
            options.forEach { option ->
                if (question.hasPhotos) {
                    PhotoChoiceCard(
                        option = option,
                        colorKey = colorKey,
                        selected = selection == option.stableId,
                        onClick = { if (enabled) onSelect(option.stableId) },
                    )
                } else {
                    IconChoiceRow(
                        option = option,
                        colorKey = colorKey,
                        selected = selection == option.stableId,
                        onClick = { if (enabled) onSelect(option.stableId) },
                    )
                }
            }
        } else {
            AnswerField(value = draft, onValueChange = onType, enabled = enabled)
        }
    }
}

/** The multi-line free-text answer box. */
@Composable
fun AnswerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Your answer",
    minHeight: androidx.compose.ui.unit.Dp = 84.dp,
) {
    val colors = LeylaTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .padding(14.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = IOSText.body, color = colors.secondary)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = IOSText.body.copy(color = colors.ink),
            cursorBrush = SolidColor(Theme.rose),
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight),
        )
    }
}

/** Back / Next bar pinned under the scroll in every play flow. */
@Composable
fun PlayNavBar(
    showBack: Boolean,
    nextLabel: String,
    nextIcon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    enabled: Boolean,
    submitting: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            TextAction(
                "Back",
                onBack,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                style = IOSText.subheadline.weight(FontWeight.Bold),
            )
        }
        Spacer(Modifier.weight(1f))
        PillButton(
            text = nextLabel,
            color = accent,
            onClick = onNext,
            icon = nextIcon,
            enabled = enabled && !submitting,
            loading = submitting,
        )
    }
}

// MARK: - Results

@Composable
private fun QuizResults(
    state: QuizPlayUiState,
    colorKey: String,
    accent: Color,
    viewModel: QuizPlayViewModel,
) {
    val colors = LeylaTheme.colors
    val quiz = state.quiz ?: return
    val partnerDone = quiz.questions.all { it.partnerAnswer != null }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (partnerDone) {
            val matches = quiz.questions.count { it.myAnswer != null && it.myAnswer == it.partnerAnswer }
            QuizIconTile(Icons.Filled.CheckCircle, colorKey, size = 64.dp, modifier = Modifier.padding(top = 12.dp))
            Text("Results", style = IOSText.title.weight(FontWeight.Bold), color = colors.ink)
            if (quiz.questions.any { it.isChoice }) {
                Text(
                    "You matched on $matches of ${quiz.questions.size}",
                    style = IOSText.headline,
                    color = accent,
                )
            }
            quiz.questions.forEach { question ->
                QuizResultRow(question, colorKey, accent, state.partnerName)
            }
        } else {
            QuizIconTile(
                Icons.Filled.HourglassTop,
                colorKey,
                size = 72.dp,
                modifier = Modifier.padding(top = 40.dp),
            )
            Text("Waiting for results", style = IOSText.title2.weight(FontWeight.Bold), color = colors.ink)
            Text(
                "You've answered them all! We'll show how you compare once " +
                    "${state.partnerName} finishes this quiz too.",
                style = IOSText.subheadline,
                color = colors.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        TextAction(
            "Refresh",
            viewModel::reload,
            icon = Icons.Filled.Refresh,
            color = accent,
            style = IOSText.subheadline.weight(FontWeight.Bold),
        )
        TextAction("Review my answers", viewModel::reviewAnswers, style = IOSText.footnote)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun QuizResultRow(
    question: QuizQuestion,
    colorKey: String,
    accent: Color,
    partnerName: String,
) {
    val colors = LeylaTheme.colors
    val match = question.myAnswer != null && question.myAnswer == question.partnerAnswer
    GradientCard(colorKey = colorKey, cornerRadius = 20.dp, contentPadding = 16.dp, alpha = 0.5f) {
        Text(question.prompt, style = IOSText.subheadline.weight(FontWeight.Bold), color = colors.ink)
        Spacer(Modifier.height(12.dp))
        // Picked options are short labels and read well next to each other.
        // Written answers get a full-width row each — squeezed into half the
        // screen, a long answer becomes a narrow column of single words.
        if (question.isChoice) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniAnswer("You", question.option(question.myAnswer), question.myAnswer, Theme.coral, Modifier.weight(1f))
                MiniAnswer(
                    partnerName,
                    question.option(question.partnerAnswer),
                    question.partnerAnswer,
                    Theme.rose,
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (match) Icons.Filled.Favorite else Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = if (match) Theme.coral else colors.secondary,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    if (match) "You match!" else "Different picks",
                    style = IOSText.caption.weight(FontWeight.Bold),
                    color = if (match) Theme.coral else colors.secondary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniAnswer("You", question.option(question.myAnswer), question.myAnswer, Theme.coral)
                MiniAnswer(
                    partnerName,
                    question.option(question.partnerAnswer),
                    question.partnerAnswer,
                    Theme.rose,
                )
            }
        }
    }
}

@Composable
private fun MiniAnswer(
    name: String,
    option: QuizOption?,
    fallback: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(name, style = IOSText.caption2.weight(FontWeight.Bold), color = tint)
        option?.imageUrl?.let { url ->
            CatalogImage(
                url,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Top) {
            option?.icon?.let {
                Icon(sfSymbolIcon(it), contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            }
            // No line limit: the answer is the whole point of this screen, so the
            // card grows to fit it instead of trailing off in "…".
            Text(option?.label ?: fallback ?: "—", style = IOSText.footnote, color = colors.ink)
        }
    }
}

// MARK: - Option cards

/** Big full-width photo option with a gradient scrim, label, and selected state. */
@Composable
fun PhotoChoiceCard(
    option: QuizOption,
    colorKey: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = QuizPalette.accent(colorKey)
    Box(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(QuizPalette.gradient(colorKey))
            .selectionBorder(selected, accent, 20.dp, width = 4.dp)
            .plainClickable(onClick = onClick),
    ) {
        option.imageUrl?.let { url ->
            CatalogImage(url, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // Scrim, so the white label survives a bright photo.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0.5f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.6f),
                    ),
                ),
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(option.label, style = IOSText.title3.weight(FontWeight.Bold), color = Color.White)
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** A tappable option row with an icon, label, and selected state. */
@Composable
fun IconChoiceRow(
    option: QuizOption,
    colorKey: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(colorKey)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .selectionBorder(selected, accent, 18.dp)
            .plainClickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) accent else accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                sfSymbolIcon(option.icon),
                contentDescription = null,
                tint = if (selected) Color.White else accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(option.label, style = IOSText.headline, color = colors.ink, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
