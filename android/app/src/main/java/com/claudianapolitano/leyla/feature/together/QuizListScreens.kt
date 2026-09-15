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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
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
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.premium.PremiumStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// MARK: - Categories

data class QuizCategoriesUiState(
    val categories: List<QuizCategorySummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "Your partner",
    val isPremium: Boolean = false,
)

class QuizCategoriesViewModel : ViewModel() {

    private val _state = MutableStateFlow(QuizCategoriesUiState())
    val state: StateFlow<QuizCategoriesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Session.snapshot.collect { s ->
                _state.update { it.copy(partnerName = s.partner?.displayName ?: "Your partner") }
            }
        }
        viewModelScope.launch {
            PremiumStore.isPremium.collect { p -> _state.update { it.copy(isPremium = p) } }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.categories.isEmpty()) }
            try {
                _state.update {
                    it.copy(categories = GamesApi.quizCategories(), errorMessage = null, isLoading = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }
}

/** Full list of quiz topic packs, opened from the "Quiz" card on the Games tab. */
@Composable
fun QuizCategoriesScreen(
    onOpenCategory: (QuizCategorySummary) -> Unit,
    onLocked: (QuizCategorySummary) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizCategoriesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors

    Box(modifier.fillMaxSize().background(colors.background)) {
        if (state.categories.isEmpty()) {
            LoadingOrError(state.isLoading, state.errorMessage, onRetry = viewModel::load)
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 20.dp, bottom = 60.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text(
                        if (state.isPremium) {
                            "Answer on your own, then compare your answers."
                        } else {
                            "Answer on your own, then compare your answers. " +
                                "Two packs are free — Premium opens the rest."
                        },
                        style = IOSText.subheadline,
                        color = colors.secondary,
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    val locked = PremiumStore.isQuizCategoryLocked(category.id, state.isPremium)
                    CategoryCard(
                        category = category,
                        locked = locked,
                        partnerName = state.partnerName,
                        onClick = { if (locked) onLocked(category) else onOpenCategory(category) },
                    )
                }
            }
        }
    }
}

/** Topic card with icon, title, and a colored progress bar. Port of `CategoryCard`. */
@Composable
fun CategoryCard(
    category: QuizCategorySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    partnerName: String = "Your partner",
) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(category.colorKey)
    GradientCard(
        colorKey = category.colorKey,
        modifier = modifier,
        contentPadding = 18.dp,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuizIconTile(if (locked) "lock.fill" else category.icon, category.colorKey)
            Spacer(Modifier.width(16.dp))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(category.title, style = IOSText.headline, color = colors.ink)
                if (locked) {
                    Text(
                        "${category.quizCount} quizzes · Premium",
                        style = IOSText.caption.weight(FontWeight.Bold),
                        color = accent,
                    )
                } else {
                    // The bar is the couple's: a quiz answered by one of us is
                    // worth half, and only fills up once both have answered.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressBar(category.progress, accent, Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${Math.round(category.progress * 100)}%",
                            style = IOSText.caption.weight(FontWeight.Bold),
                            color = accent,
                        )
                    }
                    if (category.waitingForMe > 0) {
                        YourTurnHint(
                            partnerName = partnerName,
                            accent = accent,
                            compact = true,
                        )
                    } else if (category.partnerCompletedCount != null) {
                        Text(
                            "You ${category.completedCount}/${category.quizCount} · " +
                                "$partnerName ${category.partnerCompletedCount}/${category.quizCount}",
                            style = IOSText.caption2,
                            color = colors.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            when {
                locked -> PremiumLockBadge(compact = true)
                category.progress >= 1.0 -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
                else -> Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// MARK: - Category detail

data class QuizCategoryUiState(
    val detail: QuizCategoryDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "Your partner",
)

class QuizCategoryViewModel(private val categoryId: String) : ViewModel() {

    private val _state = MutableStateFlow(QuizCategoryUiState())
    val state: StateFlow<QuizCategoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Session.snapshot.collect { s ->
                _state.update { it.copy(partnerName = s.partner?.displayName ?: "Your partner") }
            }
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(detail = GamesApi.quizCategory(categoryId), errorMessage = null, isLoading = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e), isLoading = false) }
            }
        }
    }
}

/** Lists the quizzes inside a category. Port of `QuizCategoryDetailView`. */
@Composable
fun QuizCategoryScreen(
    categoryId: String,
    onOpenQuiz: (quizId: String, colorKey: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizCategoryViewModel = viewModel(key = "quiz-category-$categoryId") {
        QuizCategoryViewModel(categoryId)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val detail = state.detail

    // Coming back from a quiz should show the pack's new done state, not the
    // list as it was when we left it.
    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize().background(colors.background)) {
        if (detail == null) {
            LoadingOrError(state.isLoading, state.errorMessage, onRetry = viewModel::load)
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 20.dp, bottom = 60.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(detail.quizzes, key = { it.id }) { quiz ->
                    QuizCard(
                        quiz = quiz,
                        colorKey = detail.colorKey,
                        partnerName = state.partnerName,
                        onClick = { onOpenQuiz(quiz.id, detail.colorKey, quiz.title) },
                    )
                }
            }
        }
    }
}

/** One quiz row: tag + format badge, title, icon, and a play/compare state. */
@Composable
fun QuizCard(
    quiz: QuizSummary,
    colorKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    partnerName: String = "Your partner",
) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent(colorKey)
    GradientCard(
        colorKey = colorKey,
        modifier = modifier,
        cornerRadius = 24.dp,
        contentPadding = 18.dp,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!quiz.tag.isNullOrEmpty()) {
                        Text(
                            quiz.tag,
                            style = IOSText.caption2.weight(FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accent)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        QuizFormat.badgeFor(quiz.format),
                        style = IOSText.caption2.weight(FontWeight.Bold),
                        color = accent,
                    )
                }
                Text(quiz.title, style = IOSText.title3.weight(FontWeight.Bold), color = colors.ink)
            }
            Spacer(Modifier.width(12.dp))
            QuizIconTile(quiz.icon, colorKey)
        }

        // They answered first: say so on the card itself, so you don't have to
        // open every quiz to find the one that's waiting on you.
        if (!quiz.myDone && quiz.partnerDone) {
            Spacer(Modifier.height(14.dp))
            YourTurnHint(partnerName = partnerName, accent = accent)
        }

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${quiz.questionCount} question${if (quiz.questionCount == 1) "" else "s"}",
                style = IOSText.caption,
                color = colors.secondary,
            )
            Spacer(Modifier.weight(1f))
            val (label, icon) = when {
                quiz.myDone && quiz.partnerDone -> "See results" to Icons.Filled.CheckCircle
                quiz.myDone -> "Waiting for results" to Icons.Filled.HourglassEmpty
                quiz.partnerDone -> "ANSWER NOW" to Icons.Filled.PlayArrow
                else -> "PLAY" to Icons.Filled.PlayArrow
            }
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, style = IOSText.footnote.weight(FontWeight.Bold), color = accent)
        }
    }
}
