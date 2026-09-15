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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.core.ApiException
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.premium.PremiumStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Games tab: today's question, the quiz entry, then the couple games. Port
 * of `Us/Features/Together/TogetherView.swift`.
 */

data class TogetherUiState(
    val daily: QuizDaily? = null,
    val dailyError: String? = null,
    val partnerName: String = "Your partner",
    val isPremium: Boolean = false,
)

class TogetherViewModel : ViewModel() {

    private val _state = MutableStateFlow(TogetherUiState())
    val state: StateFlow<TogetherUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Session.snapshot.collect { session ->
                _state.update { it.copy(partnerName = session.partner?.displayName ?: "Your partner") }
            }
        }
        viewModelScope.launch {
            PremiumStore.isPremium.collect { premium ->
                _state.update { it.copy(isPremium = premium) }
            }
        }
    }

    fun load() {
        viewModelScope.launch { loadDaily() }
    }

    private suspend fun loadDaily() {
        try {
            _state.update { it.copy(daily = GamesApi.dailyQuiz(), dailyError = null) }
        } catch (e: Exception) {
            _state.update { it.copy(daily = null, dailyError = describe(e)) }
        }
    }
}

/** The server's own message when it sent one, the exception's otherwise. */
internal fun describe(error: Throwable): String =
    (error as? ApiException)?.payload?.message
        ?: error.message
        ?: "Something went wrong"

@Composable
fun TogetherScreen(
    onOpenDaily: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenGame: (GameDef) -> Unit,
    onLocked: (GameDef) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TogetherViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors

    // iOS fires this from onAppear rather than .task so a parent-view rebuild
    // can't cancel the request; the Compose equivalent is a LaunchedEffect keyed
    // to the screen itself, which survives recomposition the same way.
    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                "Games",
                style = IOSText.largeTitle.weight(FontWeight.Bold),
                color = colors.ink,
                modifier = Modifier.padding(top = 8.dp),
            )

            when {
                state.daily != null -> DailyQuestionCard(
                    daily = state.daily!!,
                    onClick = onOpenDaily,
                )

                state.dailyError != null -> MaterialCard(cornerRadius = 16.dp, contentPadding = 16.dp) {
                    Text(
                        "Daily question unavailable",
                        style = IOSText.subheadline.weight(FontWeight.Bold),
                        color = colors.ink,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        state.dailyError!!,
                        style = IOSText.caption,
                        color = colors.secondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextAction("Retry", { viewModel.load() }, color = Theme.rose)
                }
            }

            QuizEntryCard(onClick = onOpenQuiz)

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Games", style = IOSText.title2.weight(FontWeight.Bold), color = colors.ink)
                    Text(
                        "Play together, at your own pace",
                        style = IOSText.subheadline,
                        color = colors.secondary,
                    )
                }

                GameDef.all.forEach { game ->
                    val locked = PremiumStore.isGameLocked(game.id, state.isPremium)
                    GameCard(
                        game = game,
                        locked = locked,
                        onClick = { if (locked) onLocked(game) else onOpenGame(game) },
                    )
                }
            }

            // The tab bar floats over the scroll content, so the last card needs
            // its own room — otherwise the scroll ends with it under the bar.
            Spacer(Modifier.size(80.dp))
        }
    }
}

/** The hero card at the top of Games: today's rotating question, category-coloured. */
@Composable
fun DailyQuestionCard(daily: QuizDaily, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(daily.colorKey)
    val answered = daily.question.myAnswer != null
    // Turn label: waiting on partner, my move, or ready to compare.
    val turn = when {
        daily.question.bothAnswered -> "COMPARE"
        answered -> "WAITING"
        else -> "YOUR TURN"
    }

    GradientCard(
        colorKey = daily.colorKey,
        modifier = modifier,
        cornerRadius = 26.dp,
        contentPadding = 20.dp,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "QUESTION OF THE DAY",
                style = IOSText.caption.weight(FontWeight.Bold),
                color = accent,
            )
            Spacer(Modifier.weight(1f))
            TurnBadge(turn, accent)
        }
        Spacer(Modifier.size(14.dp))
        Text(
            daily.question.prompt,
            style = IOSText.title3.weight(FontWeight.Bold),
            color = colors.ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                sfSymbolIcon(daily.icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                daily.categoryTitle,
                style = IOSText.subheadline.weight(FontWeight.Bold),
                color = accent,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Single card that stands in for the whole Quiz section, matching the game
 * cards. Tapping it opens the full list of quiz topic packs.
 */
@Composable
fun QuizEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent("purple")
    MaterialCard(modifier = modifier, onClick = onClick) {
        QuizIconTile(Icons.Filled.Widgets, "purple", size = 52.dp)
        Spacer(Modifier.size(14.dp))
        Text("Quiz", style = IOSText.title3.weight(FontWeight.Bold), color = colors.ink)
        Spacer(Modifier.size(14.dp))
        Text(
            "Answer privately, then compare — topic packs from cute to deep, plus a daily question.",
            style = IOSText.subheadline,
            color = colors.secondary,
        )
        Spacer(Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Browse quizzes",
                style = IOSText.subheadline.weight(FontWeight.Bold),
                color = accent,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Game card: icon tile, title + badge, description, CTA. Port of `GameCard`. */
@Composable
fun GameCard(
    game: GameDef,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(game.colorKey)
    MaterialCard(modifier = modifier, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            QuizIconTile(if (locked) "lock.fill" else game.icon, game.colorKey, size = 52.dp)
            Spacer(Modifier.weight(1f))
            if (locked) PremiumLockBadge()
        }

        Spacer(Modifier.size(14.dp))

        if (game.badge != null && !locked) {
            Text(
                game.badge,
                style = IOSText.caption2.weight(FontWeight.Bold),
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Theme.warmGradient)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Spacer(Modifier.size(6.dp))
        }

        Text(game.title, style = IOSText.title3.weight(FontWeight.Bold), color = colors.ink)
        Spacer(Modifier.size(14.dp))
        Text(game.subtitle, style = IOSText.subheadline, color = colors.secondary)
        Spacer(Modifier.size(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (locked) "Unlock with Premium" else game.cta,
                style = IOSText.subheadline.weight(FontWeight.Bold),
                color = accent,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                when {
                    locked -> Icons.Filled.AutoAwesome
                    game.kind == GameDef.Kind.COMING_SOON -> Icons.Filled.Schedule
                    else -> Icons.Filled.PlayArrow
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Placeholder for games that need infra we haven't shipped yet. */
@Composable
fun ComingSoonGameScreen(game: GameDef, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        QuizIconTile(game.icon, game.colorKey, size = 84.dp)
        Text(game.title, style = IOSText.title.weight(FontWeight.Bold), color = colors.ink)
        Text(
            "COMING SOON",
            style = IOSText.caption.weight(FontWeight.Bold),
            color = QuizPalette.accent(game.colorKey),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(QuizPalette.accent(game.colorKey).copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Text(
            game.subtitle,
            style = IOSText.body,
            color = colors.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "We're building this one to work at your own pace — play now, your partner catches up whenever.",
            style = IOSText.footnote,
            color = colors.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
