package com.claudianapolitano.leyla.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.ApiException
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import kotlinx.coroutines.launch

/**
 * Two-step email change: enter the new address, then the code we mail to it.
 * Port of `Us/Features/Settings/ChangeEmailView.swift`.
 *
 * The code goes to the *new* address on purpose — receiving it is what proves
 * the address is real and reachable. Nothing on the account changes until the
 * code is confirmed, so abandoning the sheet halfway is harmless.
 */
private enum class Step { ADDRESS, CODE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailSheet(onDismiss: () -> Unit) {
    val colors = LeylaTheme.colors
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var step by remember { mutableStateOf(Step.ADDRESS) }
    var newEmail by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sentTo by remember { mutableStateOf("") }
    /** Only ever set by a dev server with no mail provider configured. */
    var devCode by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emailLooksValid = remember(newEmail) {
        val trimmed = newEmail.trim()
        val at = trimmed.indexOf('@')
        if (at <= 0) {
            false
        } else {
            val domain = trimmed.substring(at + 1)
            domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
        }
    }

    fun sendCode() {
        scope.launch {
            busy = true
            errorMessage = null
            try {
                val response = LeylaApi.requestEmailChange(newEmail.trim())
                sentTo = response.sentTo
                devCode = response.devCode
                code = ""
                step = Step.CODE
            } catch (e: Exception) {
                errorMessage = (e as? ApiException)?.payload?.message ?: e.message
            } finally {
                busy = false
            }
        }
    }

    fun confirmCode() {
        scope.launch {
            busy = true
            errorMessage = null
            try {
                Session.updateUser(LeylaApi.confirmEmailChange(code.trim()))
                haptics.success()
                onDismiss()
            } catch (e: Exception) {
                errorMessage = (e as? ApiException)?.payload?.message ?: e.message
            } finally {
                busy = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.backgroundTop,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    leylaString(R.string.email_change_title),
                    style = IOSText.title3.weight(FontWeight.Bold),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss, enabled = !busy) {
                    Text(leylaString(R.string.cancel), color = colors.secondary)
                }
                if (busy) {
                    CircularProgressIndicator(
                        color = Theme.rose,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(horizontal = 12.dp).size(20.dp),
                    )
                } else {
                    TextButton(
                        onClick = { if (step == Step.ADDRESS) sendCode() else confirmCode() },
                        enabled = if (step == Step.ADDRESS) emailLooksValid else code.trim().length >= 6,
                    ) {
                        Text(
                            leylaString(
                                if (step == Step.ADDRESS) R.string.email_send_code
                                else R.string.email_confirm,
                            ),
                            style = IOSText.subheadline.weight(FontWeight.SemiBold),
                            color = Theme.coral,
                        )
                    }
                }
            }

            when (step) {
                Step.ADDRESS -> {
                    Text(
                        leylaString(R.string.email_new_email),
                        style = IOSText.footnote.weight(FontWeight.SemiBold),
                        color = colors.secondary,
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        placeholder = { Text(leylaString(R.string.email_placeholder)) },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        leylaString(R.string.email_we_will_send),
                        style = IOSText.footnote,
                        color = colors.secondary,
                    )
                }

                Step.CODE -> {
                    Text(
                        leylaString(R.string.email_code_sent_to, sentTo),
                        style = IOSText.footnote.weight(FontWeight.SemiBold),
                        color = colors.secondary,
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = ::sendCode, enabled = !busy) {
                        Text(
                            leylaString(R.string.email_send_new_code),
                            style = IOSText.footnote,
                            color = Theme.coral,
                        )
                    }
                    Text(
                        devCode?.let { leylaString(R.string.email_dev_code, it) }
                            ?: leylaString(R.string.email_check_inbox),
                        style = IOSText.footnote,
                        color = colors.secondary,
                    )
                }
            }

            errorMessage?.let {
                Text(it, style = IOSText.footnote, color = Theme.coral)
            }
        }
    }
}
