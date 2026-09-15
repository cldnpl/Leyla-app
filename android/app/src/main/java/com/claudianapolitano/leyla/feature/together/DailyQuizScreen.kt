package com.claudianapolitano.leyla.feature.together

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.claudianapolitano.leyla.designsystem.Avatar
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
 * The "Question of the Day": one question that changes daily, drawn from a
 * rotating category (with its colour). Answer independently, then compare. Port
 * of `DailyQuizView.swift`.
 */

data class DailyQuizUiState(
    val daily: QuizDaily? = null,
    val selection: String? = null,
    val draft: String = "",
    val submitting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "your partner",
    val myAvatarPath: String? = null,
    val partnerAvatarPath: String? = null,
)

class DailyQuizViewModel : ViewModel() {

    private val _state = MutableStateFlow(DailyQuizUiState())
    val state: StateFlow<DailyQuizUiState> = _state.asStateFlow()

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
    }

    fun load() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(daily = GamesApi.dailyQuiz(), isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }

    fun select(optionId: String) {
        _state.update { it.copy(selection = if (it.selection == optionId) null else optionId) }
    }

    fun type(text: String) {
        _state.update { it.copy(draft = text, selection = text.trim().ifEmpty { null }) }
    }

    fun submit(onAnswered: () -> Unit = {}) {
        val answer = _state.value.selection?.trim().orEmpty()
        if (answer.isEmpty() || _state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, errorMessage = null) }
            try {
                GamesApi.answerDailyQuiz(answer)
                _state.update { it.copy(draft = "", daily = GamesApi.dailyQuiz(), submitting = false) }
                onAnswered()
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, errorMessage = describe(e)) }
            }
        }
    }
}

@Composable
fun DailyQuizScreen(
    modifier: Modifier = Modifier,
    viewModel: DailyQuizViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current
    val daily = state.daily

    Box(modifier.fillMaxSize().background(colors.background)) {
        if (daily == null) {
            LoadingOrError(state.isLoading, state.errorMessage, onRetry = viewModel::load)
        } else {
            val accent = QuizPalette.accent(daily.colorKey)
            val question = daily.question
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    QuizIconTile(daily.icon, daily.colorKey, size = 64.dp)
                    Text(
                        daily.categoryTitle.uppercase(),
                        style = IOSText.caption.weight(FontWeight.Bold).copy(letterSpacing = 1.sp),
                        color = accent,
                    )
                    Text(
                        question.prompt,
                        style = IOSText.title2.weight(FontWeight.Bold),
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                    )
                }

                if (question.myAnswer == null) {
                    QuizInputSection(
                        question = question,
                        colorKey = daily.colorKey,
                        selection = state.selection,
                        draft = state.draft,
                        enabled = !state.submitting,
                        onSelect = { haptics.lightTap(); viewModel.select(it) },
                        onType = viewModel::type,
                    )
                    PrimaryButton(
                        text = "Submit answer",
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submit { haptics.success() }
                        },
                        enabled = !state.selection.isNullOrBlank(),
                        loading = state.submitting,
                    )
                } else {
                    CompareSection(state, daily, accent)
                }

                if (state.errorMessage != null) {
                    Text(state.errorMessage!!, style = IOSText.footnote, color = errorRed())
                }
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun CompareSection(state: DailyQuizUiState, daily: QuizDaily, accent: Color) {
    val colors = LeylaTheme.colors
    val question = daily.question
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AnswerCard(
            name = "You",
            avatarPath = state.myAvatarPath,
            option = question.option(question.myAnswer),
            fallback = question.myAnswer.orEmpty(),
            accent = Theme.coral,
        )

        val partnerAnswer = question.partnerAnswer
        if (partnerAnswer != null) {
            AnswerCard(
                name = state.partnerName,
                avatarPath = state.partnerAvatarPath,
                option = question.option(partnerAnswer),
                fallback = partnerAnswer,
                accent = Theme.rose,
            )
            if (question.isChoice) {
                val match = question.myAnswer == partnerAnswer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (match) Icons.Filled.Favorite else Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (match) Theme.coral else colors.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (match) "You match!" else "Different picks",
                        style = IOSText.subheadline.weight(FontWeight.Bold),
                        color = if (match) Theme.coral else colors.secondary,
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.AccessTimeFilled,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "Waiting for ${state.partnerName} to answer…",
                    style = IOSText.footnote,
                    color = colors.secondary,
                )
            }
        }

        Text(
            "Come back tomorrow for a new question 💫",
            style = IOSText.footnote,
            color = colors.secondary,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun AnswerCard(
    name: String,
    avatarPath: String?,
    option: QuizOption?,
    fallback: String,
    accent: Color,
) {
    val colors = LeylaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(path = avatarPath, name = name, size = 32.dp)

        val imageUrl = option?.imageUrl
        when {
            imageUrl != null -> CatalogImage(
                imageUrl,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )

            option?.icon != null -> Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    sfSymbolIcon(option.icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = IOSText.caption.weight(FontWeight.Bold), color = accent)
            Text(option?.label ?: fallback, style = IOSText.body, color = colors.ink)
        }
    }
}
