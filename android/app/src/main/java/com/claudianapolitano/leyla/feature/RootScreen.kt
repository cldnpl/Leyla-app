package com.claudianapolitano.leyla.feature

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.feature.auth.AuthOptionsScreen
import com.claudianapolitano.leyla.feature.auth.SignInScreen
import com.claudianapolitano.leyla.feature.auth.WelcomeScreen
import com.claudianapolitano.leyla.feature.onboarding.PersonalOnboardingScreen
import com.claudianapolitano.leyla.feature.pairing.PairingScreen

/**
 * Routes between the major app states based on auth + pairing. Port of
 * `Us/Features/RootView.swift`.
 *
 * Every games and couple endpoint needs both a token and a couple, so a build
 * that opened straight on the tab shell could only ever show "unauthorized" —
 * this is the gate that was missing.
 */
@Composable
fun RootScreen(modifier: Modifier = Modifier) {
    val session by Session.snapshot.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors

    /** Which auth form the signed-out screen is showing, if any. */
    var authMode by remember { mutableStateOf<AuthMode?>(null) }

    /**
     * Whether the email form is up. iOS presents it as a sheet over the
     * provider options; here it is a second step, so Back returns to the
     * providers rather than all the way out to the welcome screen.
     */
    var emailForm by remember { mutableStateOf(false) }

    // Without this, system Back closes the app from anywhere in the signed-out
    // flow: these steps are local state, not a nav back stack, so there is
    // nothing for the framework to pop. Back on the welcome screen itself is
    // left alone — leaving the app is the right thing to do there.
    BackHandler(enabled = session.state == Session.State.SIGNED_OUT && authMode != null) {
        if (emailForm) emailForm = false else authMode = null
    }

    AnimatedContent(
        targetState = session.state,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
        label = "root-state",
        modifier = modifier.fillMaxSize(),
    ) { state ->
        when (state) {
            Session.State.LOADING -> Box(
                Modifier.fillMaxSize().background(colors.background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Theme.rose)
            }

            Session.State.SIGNED_OUT -> when (val mode = authMode) {
                null -> WelcomeScreen(
                    onRegister = { emailForm = false; authMode = AuthMode.REGISTER },
                    onLogIn = { emailForm = false; authMode = AuthMode.LOGIN },
                )

                else -> if (emailForm) {
                    SignInScreen(
                        isSignUp = mode == AuthMode.REGISTER,
                        onBack = { emailForm = false },
                    )
                } else {
                    AuthOptionsScreen(
                        isRegister = mode == AuthMode.REGISTER,
                        onEmail = { emailForm = true },
                        onBack = { authMode = null },
                    )
                }
            }

            Session.State.NEEDS_PERSONAL_ONBOARDING -> PersonalOnboardingScreen()

            Session.State.NEEDS_PAIRING -> PairingScreen()

            Session.State.READY -> MainTabView()
        }
    }
}

/** Fixed by the choice made on the welcome screen, so the form has no toggle. */
internal enum class AuthMode { REGISTER, LOGIN }
