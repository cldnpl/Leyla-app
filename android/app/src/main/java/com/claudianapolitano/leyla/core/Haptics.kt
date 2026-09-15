package com.claudianapolitano.leyla.core

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Port of `Us/Core/Haptics.swift`.
 *
 * iOS reaches for the feedback generators directly; on Android the same taps go
 * through the hosting [View], which is what routes them to the device's own
 * haptic settings — a raw `Vibrator` would buzz even for someone who has turned
 * touch feedback off.
 */
@Immutable
class Haptics(private val view: View) {

    /** `Haptics.tap(.medium)` — confirming a pick. */
    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** `Haptics.tap(.light)` — selecting an option, switching a tool. */
    fun lightTap() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** `Haptics.success()` — an answer saved, a drawing submitted. */
    fun success() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
