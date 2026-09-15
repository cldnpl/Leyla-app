package com.claudianapolitano.leyla.feature.pairing

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.claudianapolitano.leyla.core.ApiException
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.SharedConfig
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.TextAction
import com.claudianapolitano.leyla.feature.together.errorRed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Connect with your partner: show a code, or redeem theirs. Port of
 * `PairingView.swift`.
 */

data class PairingUiState(
    val generatedCode: String? = null,
    val enteredCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /** Real codes are 6 characters; "0000" is the demo bypass. */
    val canConnect: Boolean
        get() {
            val code = enteredCode.trim()
            if (SharedConfig.DEMO_MODE && code == "0000") return true
            return code.length >= 6
        }
}

class PairingViewModel : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        generate()
        startPolling()
    }

    private fun generate() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(generatedCode = LeylaApi.createPairingCode().code, errorMessage = null) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = describe(e)) }
            }
        }
    }

    /**
     * While this partner is showing the invite code, watch the backend and
     * advance the moment the other one redeems it — so the screen disappears on
     * its own, with no manual "continue".
     */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(3_000)
                val couple = runCatching { LeylaApi.getCouple() }.getOrNull() ?: continue
                if (couple.paired) {
                    // Flips the session to READY, which takes this screen away.
                    Session.loadCouple()
                    return@launch
                }
            }
        }
    }

    fun typeCode(value: String) {
        _state.update { it.copy(enteredCode = value.uppercase(), errorMessage = null) }
    }

    fun redeem(onPaired: () -> Unit = {}) {
        val code = _state.value.enteredCode.trim().uppercase()
        if (!_state.value.canConnect || _state.value.isLoading) return

        // TEST ONLY (SharedConfig.DEMO_MODE): "0000" opens the app without a
        // real partner so the UI can be exercised.
        if (SharedConfig.DEMO_MODE && code == "0000") {
            onPaired()
            Session.enterTestPairing()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                LeylaApi.redeemPairing(code)
                onPaired()
                Session.loadCouple()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = describe(e)) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { Session.signOut() }
    }

    private fun describe(error: Throwable): String =
        (error as? ApiException)?.payload?.message ?: error.message ?: "Something went wrong"

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}

@Composable
fun PairingScreen(
    modifier: Modifier = Modifier,
    viewModel: PairingViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            Modifier.padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.Link,
                contentDescription = null,
                tint = Theme.coral,
                modifier = Modifier.size(44.dp),
            )
            Text(
                "Connect with your partner",
                style = IOSText.title2.weight(FontWeight.Bold),
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                "Share a code, or enter the one your partner gives you.",
                style = IOSText.subheadline,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )
        }

        // Invite half
        LeylaCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Invite your partner", style = IOSText.headline, color = colors.ink)
                val code = state.generatedCode
                if (code != null) {
                    Text(
                        code,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        color = Theme.coral,
                    )
                    Text(
                        "Share this code with your partner",
                        style = IOSText.footnote,
                        color = colors.secondary,
                        textAlign = TextAlign.Center,
                    )
                    TextAction(
                        text = "Share code",
                        icon = Icons.Filled.Share,
                        color = Theme.coral,
                        style = IOSText.subheadline.weight(FontWeight.Bold),
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join me on Leyla 💜 Use my pairing code: $code",
                                )
                            }
                            context.startActivity(Intent.createChooser(share, null))
                        },
                    )
                } else {
                    CircularProgressIndicator(color = Theme.rose, modifier = Modifier.padding(vertical = 10.dp))
                    Text(
                        "Preparing your code…",
                        style = IOSText.footnote,
                        color = colors.secondary,
                    )
                }
            }
        }

        // Redeem half
        LeylaCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("I have a code", style = IOSText.headline, color = colors.ink)
                OutlinedTextField(
                    value = state.enteredCode,
                    onValueChange = viewModel::typeCode,
                    placeholder = { Text("Enter code", textAlign = TextAlign.Center) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    textStyle = IOSText.title2.copy(
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedIndicatorColor = Theme.rose,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton(
                    text = "Connect",
                    onClick = {
                        keyboard?.hide()
                        viewModel.redeem { haptics.success() }
                    },
                    enabled = state.canConnect,
                    loading = state.isLoading,
                )
            }
        }

        if (state.errorMessage != null) {
            Text(
                state.errorMessage!!,
                style = IOSText.footnote,
                color = errorRed(),
                textAlign = TextAlign.Center,
            )
        }

        TextAction(
            text = "Sign out",
            onClick = viewModel::signOut,
            color = colors.secondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
