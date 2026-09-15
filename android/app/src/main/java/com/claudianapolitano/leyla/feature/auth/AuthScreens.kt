package com.claudianapolitano.leyla.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.ApiException
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.plainClickable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Signed-out entry. Port of `WelcomeView` and `SignInView`.
 *
 * iOS offers Sign in with Apple alongside email, and shows a "coming soon"
 * note for Google. Neither has an Android counterpart wired up — Apple's is an
 * iOS-only flow and Google needs the Sign-In SDK plus a backend endpoint — so
 * this build offers email only rather than parking buttons that cannot work.
 */

/** The brand wordmark, in the same script face the iOS welcome screen uses. */
private val chopinScript = FontFamily(Font(R.font.chopin_script))

@Composable
fun WelcomeScreen(
    onRegister: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Theme.warmGradient)
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "Leyla",
            fontFamily = chopinScript,
            fontSize = 110.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            "Two people, one little world.",
            style = IOSText.title3,
            color = Color.White.copy(alpha = 0.95f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))

        PrimaryButton(text = "Create an account", onClick = onRegister)
        Spacer(Modifier.size(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .plainClickable(onClick = onLogIn)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Log in", style = IOSText.headline, color = Color.White)
        }
        Spacer(Modifier.size(40.dp))
    }
}

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Matches the backend's own rule: a real address and 8+ characters. */
    val isValid: Boolean get() = email.contains("@") && password.length >= 8
}

class SignInViewModel : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun typeEmail(value: String) = _state.update { it.copy(email = value, errorMessage = null) }

    fun typePassword(value: String) = _state.update { it.copy(password = value, errorMessage = null) }

    fun submit(isSignUp: Boolean) {
        val current = _state.value
        if (!current.isValid || current.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val email = current.email.trim()
                val auth = if (isSignUp) {
                    // The name isn't asked here — it's collected right after, in
                    // the personal onboarding. Send a placeholder from the email.
                    val placeholder = email.substringBefore('@').take(30).ifEmpty { "there" }
                    LeylaApi.register(email, current.password, placeholder)
                } else {
                    LeylaApi.login(email, current.password)
                }
                // Session flips the app to the next step, which unmounts this
                // screen — so nothing needs to be cleared here.
                Session.handleAuth(auth)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = (e as? ApiException)?.payload?.message
                            ?: e.message
                            ?: "Something went wrong",
                    )
                }
            }
        }
    }
}

@Composable
fun SignInScreen(
    isSignUp: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        modifier
            .fillMaxSize()
            .background(Theme.warmGradient),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                if (isSignUp) "Create your account" else "Welcome back",
                style = IOSText.display,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(24.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::typeEmail,
                label = { Text("Email") },
                singleLine = true,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                colors = whiteFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::typePassword,
                label = { Text("Password") },
                singleLine = true,
                enabled = !state.isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                colors = whiteFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (isSignUp) {
                Spacer(Modifier.size(8.dp))
                Text(
                    "At least 8 characters. You'll choose your name in the next step.",
                    style = IOSText.footnote,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }

            if (state.errorMessage != null) {
                Spacer(Modifier.size(12.dp))
                Text(
                    state.errorMessage!!,
                    style = IOSText.footnote,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.size(24.dp))
            PrimaryButton(
                text = if (isSignUp) "Create account" else "Log in",
                onClick = { keyboard?.hide(); viewModel.submit(isSignUp) },
                enabled = state.isValid,
                loading = state.isLoading,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(40.dp))
        }
    }
}

/** Field colours that read on the warm gradient rather than on a card. */
@Composable
private fun whiteFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.18f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.18f),
    disabledContainerColor = Color.White.copy(alpha = 0.10f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
    cursorColor = Color.White,
    focusedIndicatorColor = Color.White,
    unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f),
)
