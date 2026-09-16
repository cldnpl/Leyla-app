package com.claudianapolitano.leyla.feature.onboarding

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.PartnerPrefs
import com.claudianapolitano.leyla.core.PartnerPronoun
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.rememberHaptics
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.plainClickable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Shown once after pairing, on BOTH partners' devices. Port of
 * `Us/Features/Onboarding/SetupFlowView.swift`.
 *
 * 1. pick the partner's pronoun (personalises copy + widget),
 * 2. set how long you've been together (drives the "Been together" counter).
 */
object SetupFlow {
    /**
     * Runs if the pronoun hasn't been chosen or the couple has no start date.
     * Both are things only the pair can answer, and the app reads worse without
     * them, so it asks once rather than guessing forever.
     */
    fun isNeeded(): Boolean =
        !PartnerPrefs.hasChosenPronoun || Session.current.couple?.startDate == null
}

/** Named apart from the personal onboarding's own `Step` in this package. */
private enum class SetupStep { PRONOUN, START_DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupFlowScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val session by Session.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    val existingStart = remember(session.couple?.startDate) {
        session.couple?.startDate?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
    }
    var step by remember {
        mutableStateOf(if (PartnerPrefs.hasChosenPronoun) SetupStep.START_DATE else SetupStep.PRONOUN)
    }
    var selected by remember { mutableStateOf<PartnerPronoun?>(null) }
    var startDate by remember { mutableStateOf(existingStart ?: LocalDate.now()) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(existingStart) { existingStart?.let { startDate = it } }

    val partnerName = session.partner?.displayName?.takeIf { it.isNotBlank() }
        ?: leylaString(R.string.your_partner)

    Box(
        modifier
            .fillMaxSize()
            .background(Theme.warmGradient)
            .systemBarsPadding(),
    ) {
        when (step) {
            SetupStep.PRONOUN -> PronounStep(
                partnerName = partnerName,
                selected = selected,
                onSelect = { selected = it; haptics.tap() },
                onContinue = {
                    selected?.let { pronoun -> scope.launch { Session.setPartnerPronoun(pronoun) } }
                    // Don't re-ask the start date if the couple already has one.
                    if (existingStart != null) onDone() else step = SetupStep.START_DATE
                },
            )

            SetupStep.START_DATE -> StartDateStep(
                startDate = startDate,
                onDateChange = { startDate = it },
                saving = saving,
                onFinish = {
                    saving = true
                    scope.launch {
                        Session.saveStartDate(startDate.toString())
                        saving = false
                        onDone()
                    }
                },
            )
        }
    }
}

// MARK: - Step 1 · Pronoun

@Composable
private fun PronounStep(
    partnerName: String,
    selected: PartnerPronoun?,
    onSelect: (PartnerPronoun) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Filled.WavingHand,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(52.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                leylaString(R.string.setup_about_partner, partnerName),
                style = IOSText.largeTitle.weight(FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                leylaString(R.string.setup_pronoun_body, partnerName),
                style = IOSText.subheadline,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
            )
        }

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PartnerPronoun.entries.forEach { option ->
                PronounOption(option, option == selected) { onSelect(option) }
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = leylaString(R.string.setup_continue),
            onClick = onContinue,
            enabled = selected != null,
        )
    }
}

@Composable
private fun PronounOption(pronoun: PartnerPronoun, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.18f))
            .plainClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            leylaString(pronoun.labelRes),
            style = IOSText.headline,
            color = if (isSelected) Theme.rose else Color.White,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) Theme.rose else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}

private val PartnerPronoun.labelRes: Int
    get() = when (this) {
        PartnerPronoun.SHE -> R.string.pronoun_she
        PartnerPronoun.HE -> R.string.pronoun_he
        PartnerPronoun.THEY -> R.string.pronoun_they
    }

// MARK: - Step 2 · Start date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDateStep(
    startDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    saving: Boolean,
    onFinish: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = NotInTheFuture,
    )

    // The picker owns the selection once it is on screen; mirror it out.
    LaunchedEffect(pickerState.selectedDateMillis) {
        pickerState.selectedDateMillis?.let {
            onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
        }
    }

    val days = ChronoUnit.DAYS.between(startDate, LocalDate.now()).coerceAtLeast(0)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.size(16.dp))
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(50.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                leylaString(R.string.setup_how_long),
                style = IOSText.title.weight(FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                leylaString(R.string.setup_pick_the_day),
                style = IOSText.subheadline,
                color = Color.White.copy(alpha = 0.95f),
            )
        }

        // The plate is deliberately white (this screen is a fixed pink
        // gradient), so the picker is pinned to light colours or its day
        // numbers would turn white-on-white in dark mode.
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(8.dp),
        ) {
            DatePicker(
                state = pickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = LeylaTheme.colors.ink,
                    headlineContentColor = LeylaTheme.colors.ink,
                    weekdayContentColor = Color(0.45f, 0.42f, 0.46f),
                    dayContentColor = Color(0.11f, 0.10f, 0.13f),
                    selectedDayContainerColor = Theme.rose,
                    selectedDayContentColor = Color.White,
                    todayContentColor = Theme.rose,
                    todayDateBorderColor = Theme.rose,
                    navigationContentColor = Color(0.11f, 0.10f, 0.13f),
                    yearContentColor = Color(0.11f, 0.10f, 0.13f),
                    currentYearContentColor = Theme.rose,
                    selectedYearContainerColor = Theme.rose,
                    selectedYearContentColor = Color.White,
                    subheadContentColor = Color(0.45f, 0.42f, 0.46f),
                ),
            )
        }

        Text(
            leylaString(R.string.setup_days_together, days),
            style = IOSText.headline,
            color = Color.White,
        )

        Spacer(Modifier.size(12.dp))
        PrimaryButton(
            text = leylaString(R.string.setup_finish),
            onClick = onFinish,
            enabled = !saving,
            loading = saving,
        )
    }
}

/** A relationship cannot have started tomorrow. */
@OptIn(ExperimentalMaterial3Api::class)
private object NotInTheFuture : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
