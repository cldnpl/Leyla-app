package com.claudianapolitano.leyla.feature.premium

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.CategoryCard
import com.claudianapolitano.leyla.feature.together.DebateArgumentBlock
import com.claudianapolitano.leyla.feature.together.DrawPromptHeader
import com.claudianapolitano.leyla.feature.together.DrawToolbar
import com.claudianapolitano.leyla.feature.together.IconChoiceRow
import com.claudianapolitano.leyla.feature.together.QuizCategorySummary
import com.claudianapolitano.leyla.feature.together.QuizIconTile
import com.claudianapolitano.leyla.feature.together.QuizOption
import com.claudianapolitano.leyla.feature.together.QuizPalette
import com.claudianapolitano.leyla.feature.together.ScoreRing
import com.claudianapolitano.leyla.feature.together.SnapCameraTarget
import com.claudianapolitano.leyla.feature.together.SnapClueHeader
import com.claudianapolitano.leyla.feature.together.StepDots
import com.claudianapolitano.leyla.feature.together.MaterialCard
import kotlinx.coroutines.delay

/**
 * The animated phone on the paywall. Port of
 * `Us/Features/Premium/PaywallPhoneMockup.swift`.
 *
 * The screens are not drawings of the app — they are the app: every card, row,
 * ring and toolbar here is the same composable the real game renders, fed with
 * sample data and laid out at true phone width, then scaled down into the
 * bezel. So the proportions, paddings and type sizes match what you actually
 * get.
 */
private const val SCREEN_DWELL_MS = 3_600L

/** The logical width the demo screens lay themselves out at — a real phone. */
private val LOGICAL_WIDTH = 393.dp

/**
 * Keeps a real phone's 0.461 aspect ratio, so the scaled-down screens are never
 * stretched — 178 / 393 is exactly the scale factor applied inside.
 */
private val SCREEN_WIDTH = 178.dp
private val SCREEN_HEIGHT = 386.dp
private val BEZEL = 8.dp

@Composable
fun PaywallPhone(modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val screens = DemoScreen.entries
    var index by remember { mutableIntStateOf(0) }
    var appeared by remember { mutableStateOf(false) }

    val entry by animateFloatAsState(if (appeared) 1f else 0f, tween(700), label = "phone-entry")

    LaunchedEffect(Unit) { appeared = true }
    LaunchedEffect(Unit) {
        while (true) {
            delay(SCREEN_DWELL_MS)
            index = (index + 1) % screens.size
        }
    }

    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PhoneFrame(
            modifier = Modifier
                .scale(0.9f + 0.1f * entry)
                .alpha(entry),
        ) {
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    // Cross-dissolve with a hair of zoom — a slide would show
                    // two screens side by side mid-flight.
                    (fadeIn(tween(420)) + scaleIn(tween(420), initialScale = 0.96f)) togetherWith
                        (fadeOut(tween(420)) + scaleOut(tween(420), targetScale = 1.04f))
                },
                label = "demo-screen",
                modifier = Modifier.fillMaxSize(),
            ) { position ->
                DemoScreenView(screens[position])
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedContent(
                targetState = index,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "demo-caption",
            ) { position ->
                Text(
                    screens[position].caption,
                    style = IOSText.subheadline.weight(FontWeight.SemiBold),
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                screens.indices.forEach { position ->
                    val selected = position == index
                    val width by animateFloatAsState(
                        if (selected) 18f else 6f, tween(400), label = "dot",
                    )
                    Box(
                        Modifier
                            .width(width.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Theme.rose else colors.ink.copy(alpha = 0.12f)),
                    )
                }
            }
        }
    }
}

/** Device bezel, with the screen scaled down from real phone dimensions. */
@Composable
private fun PhoneFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val bodyWidth = SCREEN_WIDTH + BEZEL * 2
    val bodyHeight = SCREEN_HEIGHT + BEZEL * 2
    val scale = SCREEN_WIDTH / LOGICAL_WIDTH

    Box(
        modifier
            .size(bodyWidth, bodyHeight)
            .clip(RoundedCornerShape(44.dp))
            .background(Color(0.09f, 0.09f, 0.09f))
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))),
                RoundedCornerShape(44.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(SCREEN_WIDTH, SCREEN_HEIGHT)
                .clip(RoundedCornerShape(37.dp))
                // The theme gradient is translucent, so it needs something behind it.
                .background(Color.White),
        ) {
            // Rendering a phone at a fraction of its size is a density change,
            // not a transform: shrink the density and every dp and sp inside
            // shrinks with it, so the demo really is laid out at phone width
            // and its proportions, paddings and type sizes stay true.
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density * scale,
                    fontScale = density.fontScale,
                ),
            ) {
                Box(Modifier.fillMaxSize()) { content() }
            }

            // The pill cut-out, drawn over whatever screen is playing.
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 7.dp)
                    .size(56.dp, 16.dp)
                    .clip(CircleShape)
                    .background(Color(0.09f, 0.09f, 0.09f)),
            )
        }
    }
}

// MARK: - Which screens play

private enum class DemoScreen(val caption: String, val navTitle: String) {
    QUIZ_PACKS("12 quiz packs, from cute to spicy", "Quiz"),
    QUIZ_PLAY("Answer apart, compare after", "Spicy Questions"),
    KNOW_ME_SCORE("See how well you really know each other", "Deep Cuts"),
    DRAW("Same prompt, two canvases", "Draw Together"),
    SNAP("Race around the house and snap it", "Snap Hunt"),
    DEBATE("An AI judge scores every round", "Food Fights"),
}

/** Renders one real app screen at phone scale, complete with status and nav bar. */
@Composable
private fun DemoScreenView(screen: DemoScreen) {
    val colors = LeylaTheme.colors
    var play by remember(screen) { mutableStateOf(false) }
    LaunchedEffect(screen) {
        play = false
        delay(200)
        play = true
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize()) {
            StatusBar()
            NavBar(screen.navTitle)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (screen) {
                    DemoScreen.QUIZ_PACKS -> QuizPacksDemo(play)
                    DemoScreen.QUIZ_PLAY -> QuizPlayDemo(play)
                    DemoScreen.KNOW_ME_SCORE -> KnowMeDemo(play)
                    DemoScreen.DRAW -> DrawDemo(play)
                    DemoScreen.SNAP -> SnapDemo(play)
                    DemoScreen.DEBATE -> DebateDemo(play)
                }
            }
        }
    }
}

@Composable
private fun StatusBar() {
    val colors = LeylaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("9:41", style = IOSText.subheadline.weight(FontWeight.SemiBold), color = colors.ink)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.NetworkCell, contentDescription = null, tint = colors.ink, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun NavBar(title: String) {
    val colors = LeylaTheme.colors
    Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 18.dp)) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = Theme.rose,
            modifier = Modifier.align(Alignment.CenterStart).size(22.dp),
        )
        Text(
            title,
            style = IOSText.headline,
            color = colors.ink,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

// MARK: 1 — the real quiz category list

@Composable
private fun QuizPacksDemo(play: Boolean) {
    val colors = LeylaTheme.colors
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Answer on your own, then compare your answers.",
            style = IOSText.subheadline,
            color = colors.secondary,
        )
        SampleData.categories.forEachIndexed { i, category ->
            val enter by animateFloatAsState(
                if (play) 1f else 0f,
                tween(550, delayMillis = i * 70),
                label = "pack",
            )
            CategoryCard(
                category = category,
                onClick = {},
                modifier = Modifier.alpha(enter).padding(top = (24 * (1 - enter)).dp),
            )
        }
    }
}

// MARK: 2 — the real quiz play screen

@Composable
private fun QuizPlayDemo(play: Boolean) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent("red")
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            QuizIconTile("flame.fill", "red", size = 60.dp)
            StepDots(total = 6, index = 2, accent = accent, isDone = { it < 2 })
            Text(
                "Question 3 of 6",
                style = IOSText.caption.weight(FontWeight.Bold),
                color = colors.secondary,
            )
            Text(
                SampleData.quizPrompt,
                style = IOSText.title2.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SampleData.quizOptions.forEachIndexed { i, option ->
                    IconChoiceRow(
                        option = option,
                        colorKey = "red",
                        selected = play && i == 1,
                        onClick = {},
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Back", style = IOSText.subheadline.weight(FontWeight.Bold), color = colors.secondary)
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .alpha(if (play) 1f else 0.45f)
                    .clip(CircleShape)
                    .background(accent)
                    .padding(horizontal = 26.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Next", style = IOSText.headline, color = Color.White)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// MARK: 3 — the real Know Me results

@Composable
private fun KnowMeDemo(play: Boolean) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent("pink")
    val score by animateFloatAsState(if (play) 87f else 0f, tween(1200), label = "score")

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScoreRing(score = score.toInt(), color = accent)
        Text("You really know each other! 💖", style = IOSText.headline, color = colors.ink)
        Text("Matched on 7 of 8", style = IOSText.subheadline, color = colors.secondary)

        SampleData.knowMeReveals.forEachIndexed { i, reveal ->
            val enter by animateFloatAsState(
                if (play) 1f else 0f,
                tween(500, delayMillis = 700 + i * 120),
                label = "reveal",
            )
            Column(
                Modifier
                    .alpha(enter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(QuizPalette.gradient("pink", alpha = if (reveal.matched) 0.6f else 0.3f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(reveal.prompt, style = IOSText.subheadline.weight(FontWeight.Bold), color = colors.ink)
                Text("Real answer: ${reveal.honest}", style = IOSText.footnote, color = colors.ink)
                Text("Guess: ${reveal.guess}", style = IOSText.footnote, color = colors.secondary)
            }
        }
    }
}

// MARK: 4 — the real drawing pad

@Composable
private fun DrawDemo(play: Boolean) {
    val accent = QuizPalette.accent("purple")
    val progress by animateFloatAsState(if (play) 1f else 0f, tween(2000), label = "doodle")

    Column(Modifier.fillMaxSize()) {
        DrawPromptHeader(prompt = "our first date", remaining = if (play) 154 else 158, accent = accent)
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.White), contentAlignment = Alignment.Center) {
            HeartDoodle(progress = progress, modifier = Modifier.size(190.dp, 170.dp))
        }
        DrawToolbar(
            selectedColorId = "red",
            brushSize = 6f,
            isEraser = false,
            isBucket = false,
            accent = accent,
            submitting = false,
            onColor = {}, onBrushSize = {}, onEraser = {}, onBucket = {},
            onUndo = {}, onClear = {}, onDone = {},
        )
    }
}

/** A heart sketched in one stroke, the way you'd actually draw it with a finger. */
@Composable
private fun HeartDoodle(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.92f)
            cubicTo(w * 0.12f, h * 0.72f, 0f, h * 0.52f, w * 0.02f, h * 0.3f)
            arcTo(
                androidx.compose.ui.geometry.Rect(
                    Offset(w * 0.02f, h * 0.3f - w * 0.24f),
                    Size(w * 0.48f, w * 0.48f),
                ),
                180f, 180f, false,
            )
            arcTo(
                androidx.compose.ui.geometry.Rect(
                    Offset(w * 0.5f, h * 0.3f - w * 0.24f),
                    Size(w * 0.48f, w * 0.48f),
                ),
                180f, 180f, false,
            )
            cubicTo(w * 0.98f, h * 0.52f, w * 0.88f, h * 0.72f, w * 0.5f, h * 0.92f)
        }
        // Trimming the path is what makes it look drawn rather than stamped.
        val measure = PathMeasure().apply { setPath(path, false) }
        val drawn = Path()
        measure.getSegment(0f, measure.length * progress, drawn, true)
        drawPath(
            drawn,
            color = Color(0.90f, 0.22f, 0.21f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

// MARK: 5 — the real snap hunt

@Composable
private fun SnapDemo(play: Boolean) {
    val accent = QuizPalette.accent("green")
    val pulse by animateFloatAsState(if (play) 1f else 0.97f, tween(1300), label = "snap-pulse")

    Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        MaterialCard(cornerRadius = 28.dp, contentPadding = 24.dp) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                SnapClueHeader(
                    clue = "something that smells like home",
                    subtitle = "Race around the house and snap your cleverest find.",
                    accent = accent,
                )
                SnapCameraTarget(accent = accent, onClick = {}, modifier = Modifier.scale(pulse))
            }
        }
    }
}

// MARK: 6 — the real debate verdict

@Composable
private fun DebateDemo(play: Boolean) {
    val colors = LeylaTheme.colors
    val accent = QuizPalette.accent("blue")
    val crown by animateFloatAsState(if (play) 1f else 0.7f, tween(550), label = "crown")

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon(
            Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(54.dp).scale(crown),
        )
        Text("You won the debate! 🏆", style = IOSText.title2.weight(FontWeight.Bold), color = colors.ink)
        Text("2–1 · you vs Alex", style = IOSText.subheadline, color = colors.secondary)

        SampleData.debateRounds.forEachIndexed { i, round ->
            val enter by animateFloatAsState(
                if (play) 1f else 0f,
                tween(500, delayMillis = 250 + i * 200),
                label = "round",
            )
            Column(
                Modifier
                    .alpha(enter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(QuizPalette.gradient("blue", alpha = if (round.iWon) 0.6f else 0.3f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "“${round.motion}”",
                    style = IOSText.subheadline.weight(FontWeight.Bold),
                    color = colors.ink,
                )
                DebateArgumentBlock("You", round.mine, round.myScore, round.iWon, accent)
                DebateArgumentBlock("Alex", round.theirs, round.theirScore, !round.iWon, accent)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (round.iWon) Icons.Filled.EmojiEvents else Icons.Filled.Flag,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(round.verdict, style = IOSText.footnote, color = colors.secondary)
                }
            }
        }
    }
}

// MARK: - Sample content

/**
 * Stand-in data for the demo — shaped like the real catalog so the real
 * composables render exactly as they do in the app.
 */
private object SampleData {
    val categories = listOf(
        QuizCategorySummary("sex_love", "Sex & Love", "flame.fill", "red", 10, 8, 0.8),
        QuizCategorySummary("money_finances", "Money & Finances", "banknote.fill", "green", 10, 5, 0.5),
        QuizCategorySummary("travel", "Travel", "airplane", "blue", 10, 3, 0.3),
        QuizCategorySummary("family", "Family", "house.fill", "amber", 10, 7, 0.7),
    )

    const val quizPrompt = "What's your ideal way to end a long day together?"

    val quizOptions = listOf(
        QuizOption(id = "opt0", label = "A long shower, together", icon = "drop.fill"),
        QuizOption(id = "opt1", label = "Sofa, series, no talking", icon = "tv.fill"),
        QuizOption(id = "opt2", label = "Straight to bed", icon = "moon.stars.fill"),
    )

    data class Reveal(val prompt: String, val honest: String, val guess: String, val matched: Boolean)

    val knowMeReveals = listOf(
        Reveal("What's my comfort food after a bad day?", "Pasta al pomodoro", "Pasta al pomodoro", true),
        Reveal("Which of my friends do I complain about most?", "Giulia", "Marco", false),
    )

    data class Round(
        val motion: String,
        val mine: String,
        val theirs: String,
        val myScore: Int,
        val theirScore: Int,
        val iWon: Boolean,
        val verdict: String,
    )

    val debateRounds = listOf(
        Round(
            motion = "Pineapple belongs on pizza",
            mine = "Sweet and savoury is the oldest trick in the book — and tomato is a fruit too.",
            theirs = "Texture matters. Warm fruit on molten cheese is a texture crime.",
            myScore = 8, theirScore = 6, iWon = true,
            verdict = "Cleaner argument with a concrete example — your round.",
        ),
    )
}
