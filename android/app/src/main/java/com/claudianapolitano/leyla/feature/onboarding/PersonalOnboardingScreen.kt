package com.claudianapolitano.leyla.feature.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudianapolitano.leyla.core.LocationRepository
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.cycle.CycleState
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.plainClickable
import kotlinx.coroutines.launch

/**
 * "About you", shown once right after sign-in and before pairing: the intro
 * carousel, your name, the cycle question, then why Leyla asks for location —
 * explained before the system prompt, not after. Port of
 * `PersonalOnboardingView.swift`.
 */
private enum class Step { WELCOME, NAME, CYCLE, LOCATION }

private data class IntroPage(val icon: ImageVector, val title: String, val subtitle: String)

private val introPages = listOf(
    IntroPage(
        Icons.Filled.Favorite,
        "Welcome to Leyla",
        "Everything a couple needs — in one little app.",
    ),
    IntroPage(
        Icons.AutoMirrored.Filled.Send,
        "Feel close, always",
        "Send a little \"thinking of you\" that lands right on their home screen.",
    ),
    IntroPage(
        Icons.Filled.Map,
        "Share your world",
        "Watch the distance between you shrink, share moments, and count down to reunions.",
    ),
    IntroPage(
        Icons.Filled.MonitorHeart,
        "Care for each other",
        "Cycle-aware tips so you always know how to show up for each other.",
    ),
)

@Composable
fun PersonalOnboardingScreen(modifier: Modifier = Modifier) {
    var step by remember { mutableStateOf(Step.WELCOME) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val context = LocalContext.current

    var name by remember { mutableStateOf(Session.current.user?.displayName.orEmpty()) }
    var saving by remember { mutableStateOf(false) }

    // The location step is the last one: whichever way the system prompt is
    // answered, onboarding is done and pairing is next. Refusing is a valid
    // answer — distance simply stays off until it's turned on in the map.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants.values.any { it }
        if (granted) LocationRepository.setSharing(context, true)
        Session.completePersonalOnboarding()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Theme.warmGradient),
    ) {
        AnimatedContent(
            modifier = Modifier.systemBarsPadding(),
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it } + fadeIn()) togetherWith
                    (slideOutHorizontally(tween(300)) { -it } + fadeOut())
            },
            label = "onboarding-step",
        ) { current ->
            when (current) {
                Step.WELCOME -> WelcomeStep(onContinue = { step = Step.NAME })

                Step.NAME -> NameStep(
                    name = name,
                    onName = { name = it },
                    saving = saving,
                    onContinue = {
                        scope.launch {
                            saving = true
                            Session.updateName(name)
                            saving = false
                            step = Step.CYCLE
                        }
                    },
                )

                Step.CYCLE -> CycleStep { hasCycle ->
                    haptics.tap()
                    scope.launch { CycleState.setUserHasCycle(hasCycle) }
                    step = Step.LOCATION
                }

                Step.LOCATION -> LocationStep {
                    haptics.tap()
                    if (LocationRepository.hasPermission(context)) {
                        LocationRepository.setSharing(context, true)
                        Session.completePersonalOnboarding()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                }
            }
        }

        if (step != Step.WELCOME) {
            StepDots(
                index = when (step) {
                    Step.WELCOME, Step.NAME -> 0
                    Step.CYCLE -> 1
                    Step.LOCATION -> 2
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun StepDots(index: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .width(if (i == index) 20.dp else 7.dp)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i == index) 1f else 0.4f)),
            )
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    val pagerState = rememberPagerState { introPages.size }
    Column(Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val intro = introPages[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    intro.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(84.dp),
                )
                Spacer(Modifier.size(26.dp))
                Text(
                    intro.title,
                    style = IOSText.display,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    intro.subtitle,
                    style = IOSText.title3,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            repeat(introPages.size) { i ->
                Box(
                    Modifier
                        .width(if (i == pagerState.currentPage) 22.dp else 8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (i == pagerState.currentPage) 1f else 0.45f)),
                )
            }
        }

        Box(Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
            PrimaryButton(text = "Get started", onClick = onContinue)
        }
        Spacer(Modifier.size(20.dp))
    }
}

@Composable
private fun NameStep(
    name: String,
    onName: (String) -> Unit,
    saving: Boolean,
    onContinue: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    StepScaffold(
        icon = Icons.Filled.Person,
        title = "What's your name?",
        body = "This is how your partner will see you in Leyla.",
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (name.isEmpty()) {
                Text("Your name", style = IOSText.title3, color = Color.White.copy(alpha = 0.7f))
            }
            BasicTextField(
                value = name,
                onValueChange = onName,
                singleLine = true,
                enabled = !saving,
                textStyle = IOSText.title3.copy(color = Color.White, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.size(28.dp))
        PrimaryButton(
            text = "Continue",
            onClick = { keyboard?.hide(); onContinue() },
            enabled = name.isNotBlank(),
            loading = saving,
        )
    }
}

@Composable
private fun CycleStep(onChoose: (Boolean) -> Unit) {
    StepScaffold(
        icon = Icons.Filled.Favorite,
        title = "Do you have a\nmenstrual cycle?",
        body = "This tailors Leyla for you — track your own cycle, or get gentle tips to " +
            "support your partner's. You can change it anytime.",
        // Android's counterpart to Apple Health, named up front the way iOS does.
        footnote = "If you do, Leyla reads your cycle from Health Connect — only after you " +
            "allow it. Nothing is written back.",
    ) {
        PrimaryButton(text = "Yes, I do", onClick = { onChoose(true) })
        Spacer(Modifier.size(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .plainClickable { onChoose(false) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("No, I don't", style = IOSText.headline, color = Color.White)
        }
    }
}

@Composable
private fun LocationStep(onContinue: () -> Unit) {
    StepScaffold(
        icon = Icons.Filled.Place,
        title = "Location for distance",
        body = "When you and your partner both turn it on, Leyla shows how far apart you are. " +
            "You're in control — you can turn it off anytime.",
    ) {
        PrimaryButton(text = "Continue", onClick = onContinue)
    }
}

/** The shared shape of the three question steps: icon, title, body, actions. */
@Composable
private fun StepScaffold(
    icon: ImageVector,
    title: String,
    body: String,
    footnote: String? = null,
    actions: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        Spacer(Modifier.size(22.dp))
        Text(
            title,
            style = IOSText.display,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            body,
            style = IOSText.subheadline,
            color = Color.White.copy(alpha = 0.95f),
            textAlign = TextAlign.Center,
        )
        if (footnote != null) {
            Spacer(Modifier.size(10.dp))
            Text(
                footnote,
                style = IOSText.footnote,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}
