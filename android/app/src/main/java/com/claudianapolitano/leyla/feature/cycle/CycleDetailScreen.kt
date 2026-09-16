package com.claudianapolitano.leyla.feature.cycle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.HealthConnectManager
import com.claudianapolitano.leyla.core.PartnerCycle
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.together.PrimaryButton
import com.claudianapolitano.leyla.feature.together.TextAction
import com.claudianapolitano.leyla.feature.together.plainClickable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The screen behind Home's cycle card. Full port of `CycleDetailView` in
 * `Us/Features/Cycle/CycleViews.swift`.
 *
 * Three paths, decided by the "do you have a cycle?" answer: not answered yet
 * asks, yes tracks her own cycle from Health Connect (or switches to pregnancy
 * mode), no shows her shared phase with the explainer and support tips.
 */
@Composable
fun CycleDetailScreen(modifier: Modifier = Modifier) {
    val state by CycleState.snapshot.collectAsStateWithLifecycle()
    val session by Session.snapshot.collectAsStateWithLifecycle()
    val colors = LeylaTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val partnerName = session.partner?.displayName?.takeIf { it.isNotBlank() }
        ?: leylaString(R.string.your_partner)

    var showHealthDetail by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var showDuePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        HealthConnectManager.requestPermissionsContract(),
    ) {
        // The result set is whatever was granted; ask the manager rather than
        // trusting it, so a partial grant is treated as not connected.
        scope.launch {
            CycleState.onPermissionResult(context)
            isBusy = false
        }
    }

    LaunchedEffect(Unit) { CycleState.refreshOnAppear(context) }

    if (showHealthDetail) {
        HealthDetailPage(
            isConnected = state.healthConnected,
            isBusy = isBusy,
            onRefresh = {
                scope.launch {
                    isBusy = true
                    CycleState.refreshInsights(context)
                    isBusy = false
                }
            },
            onBack = { showHealthDetail = false },
        )
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Every path through this screen starts by naming where the data comes
        // from, exactly as iOS does.
        Row(Modifier.fillMaxWidth()) { HealthConnectBadge() }

        when (state.userHasCycle) {
            null -> AskCard(
                partnerName = partnerName,
                onAnswer = { scope.launch { CycleState.setUserHasCycle(it) } },
                onOpenHealth = { showHealthDetail = true },
                isConnected = state.healthConnected,
            )

            true -> SelfContent(
                state = state,
                partnerName = partnerName,
                isBusy = isBusy,
                onConnect = {
                    isBusy = true
                    permissionLauncher.launch(HealthConnectManager.permissions)
                },
                onRefresh = {
                    scope.launch {
                        isBusy = true
                        CycleState.refreshInsights(context)
                        isBusy = false
                    }
                },
                onOpenHealth = { showHealthDetail = true },
                onNoteChange = { CycleState.saveNote(it) },
                onNoteCommit = { scope.launch { CycleState.syncNoteIfSharing() } },
                onShareLevel = { scope.launch { CycleState.setShareLevel(it) } },
                onStartPregnancy = { showDuePicker = true },
                onEndPregnancy = { scope.launch { CycleState.endPregnancy() } },
            )

            false -> PartnerContent(state = state, partnerName = partnerName)
        }

        PrivacyNote()
    }

    if (showDuePicker) {
        DueDatePicker(
            onDismiss = { showDuePicker = false },
            onPick = { due ->
                showDuePicker = false
                scope.launch { CycleState.startPregnancy(due) }
            },
        )
    }
}

// MARK: - Not answered yet → ask

@Composable
private fun AskCard(
    partnerName: String,
    onAnswer: (Boolean) -> Unit,
    onOpenHealth: () -> Unit,
    isConnected: Boolean,
) {
    val colors = LeylaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LeylaCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(leylaString(R.string.cycle_ask_title), style = IOSText.headline, color = colors.ink)
                Text(
                    leylaString(R.string.cycle_ask_body),
                    style = IOSText.subheadline,
                    color = colors.secondary,
                )
                Text(
                    leylaString(R.string.health_reads_line),
                    style = IOSText.footnote,
                    color = colors.secondary,
                )
                PrimaryButton(
                    text = leylaString(R.string.cycle_ask_yes),
                    onClick = { onAnswer(true) },
                )
                TextAction(
                    text = leylaString(R.string.cycle_ask_no, partnerName),
                    onClick = { onAnswer(false) },
                    color = Theme.rose,
                    style = IOSText.subheadline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        HealthConnectRow(isConnected = isConnected, onOpen = onOpenHealth)
    }
}

// MARK: - She has a cycle → tracking, or pregnancy mode

@Composable
private fun SelfContent(
    state: CycleState.Snapshot,
    partnerName: String,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHealth: () -> Unit,
    onNoteChange: (String) -> Unit,
    onNoteCommit: () -> Unit,
    onShareLevel: (CycleShareLevel) -> Unit,
    onStartPregnancy: () -> Unit,
    onEndPregnancy: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.isPregnant && state.pregnancyInsights != null) {
            PregnancyContent(
                insights = state.pregnancyInsights,
                partnerName = partnerName,
                onEnd = onEndPregnancy,
            )
        } else {
            CycleContent(
                state = state,
                partnerName = partnerName,
                isBusy = isBusy,
                onConnect = onConnect,
                onRefresh = onRefresh,
                onOpenHealth = onOpenHealth,
                onNoteChange = onNoteChange,
                onNoteCommit = onNoteCommit,
                onShareLevel = onShareLevel,
            )
            PregnancyEntry(onClick = onStartPregnancy)
        }
    }
}

@Composable
private fun CycleContent(
    state: CycleState.Snapshot,
    partnerName: String,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onOpenHealth: () -> Unit,
    onNoteChange: (String) -> Unit,
    onNoteCommit: () -> Unit,
    onShareLevel: (CycleShareLevel) -> Unit,
) {
    val colors = LeylaTheme.colors
    val insights = state.insights

    when {
        state.healthAvailability != HealthConnectManager.Availability.AVAILABLE ->
            InfoCard(
                leylaString(
                    if (state.healthAvailability == HealthConnectManager.Availability.NEEDS_PROVIDER_UPDATE) {
                        R.string.health_needs_update
                    } else {
                        R.string.health_unavailable
                    },
                ),
            )

        insights != null -> {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PhaseRing(
                    phase = insights.phase,
                    cycleDay = insights.cycleDay,
                    cycleLength = insights.cycleLength,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    leylaString(
                        R.string.cycle_next_period_summary,
                        insights.predictedNextPeriod.formatMedium(),
                        insights.cycleLength,
                    ),
                    style = IOSText.footnote,
                    color = colors.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
                HealthConnectSourceNote()
            }
            HealthConnectRow(isConnected = true, onOpen = onOpenHealth)
            ThoughtsCard(note = state.todayNote, onChange = onNoteChange, onCommit = onNoteCommit)
            SharingCard(level = state.shareLevel, partnerName = partnerName, onChange = onShareLevel)
        }

        state.healthConnected ->
            // Connected, but nothing logged yet. Offering "Connect" again here
            // would be a dead end for someone whose tracking app simply hasn't
            // synced, so this asks them to check again instead.
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LeylaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            leylaString(R.string.cycle_no_data_title),
                            style = IOSText.headline,
                            color = colors.ink,
                        )
                        Text(
                            leylaString(R.string.cycle_no_data_body),
                            style = IOSText.subheadline,
                            color = colors.secondary,
                        )
                        if (isBusy) {
                            InlineSpinner()
                        } else {
                            TextAction(
                                text = leylaString(R.string.health_check_again),
                                onClick = onRefresh,
                                color = Theme.rose,
                                style = IOSText.subheadline.weight(FontWeight.SemiBold),
                            )
                        }
                    }
                }
                HealthConnectRow(
                    isConnected = true,
                    onOpen = onOpenHealth,
                    statusDetail = leylaString(R.string.health_waiting_for_data),
                )
            }

        else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LeylaCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(leylaString(R.string.cycle_your_cycle), style = IOSText.headline, color = colors.ink)
                    Text(
                        leylaString(R.string.cycle_connect_body, partnerName),
                        style = IOSText.subheadline,
                        color = colors.secondary,
                    )
                }
            }
            HealthConnectCard(isConnected = false, isBusy = isBusy, onConnect = onConnect)
        }
    }
}

// MARK: - Pregnancy (her side)

@Composable
private fun PregnancyContent(
    insights: PregnancyInsights,
    partnerName: String,
    onEnd: () -> Unit,
) {
    val colors = LeylaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PregnancyRing(
                week = insights.week,
                daysToDue = insights.daysToDue,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                leylaString(
                    R.string.pregnancy_due_summary,
                    insights.dueDate.formatMedium(),
                    leylaString(PregnancyEngine.trimesterTitle(insights.trimester)),
                ),
                style = IOSText.footnote,
                color = colors.secondary,
                textAlign = TextAlign.Center,
            )
        }

        LeylaCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.ChildCare,
                    contentDescription = null,
                    tint = Theme.rose,
                    modifier = Modifier.size(24.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(leylaString(R.string.pregnancy_this_week), style = IOSText.headline, color = colors.ink)
                    Text(
                        leylaString(R.string.pregnancy_baby_size, leylaString(insights.babySizeRes)),
                        style = IOSText.subheadline,
                        color = colors.secondary,
                    )
                }
            }
        }

        LeylaCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    leylaString(PregnancyEngine.trimesterTitle(insights.trimester)),
                    style = IOSText.headline,
                    color = colors.ink,
                )
                Text(
                    leylaString(PregnancyEngine.trimesterAbout(insights.trimester)),
                    style = IOSText.subheadline,
                    color = colors.secondary,
                )
            }
        }

        Text(
            leylaString(R.string.pregnancy_partner_can_see, partnerName),
            style = IOSText.caption,
            color = colors.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        TextAction(
            text = leylaString(R.string.pregnancy_end_tracking),
            onClick = onEnd,
            color = colors.secondary,
            style = IOSText.footnote,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PregnancyEntry(onClick: () -> Unit) {
    val colors = LeylaTheme.colors
    LeylaCard(Modifier.plainClickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.ChildCare,
                contentDescription = null,
                tint = Theme.rose,
                modifier = Modifier.size(26.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(leylaString(R.string.pregnancy_expecting_title), style = IOSText.headline, color = colors.ink)
                Text(
                    leylaString(R.string.pregnancy_expecting_body),
                    style = IOSText.subheadline,
                    color = colors.secondary,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.secondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// MARK: - Thoughts and sharing

@Composable
private fun ThoughtsCard(note: String, onChange: (String) -> Unit, onCommit: () -> Unit) {
    val colors = LeylaTheme.colors
    LeylaCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    leylaString(R.string.cycle_todays_thoughts),
                    style = IOSText.headline,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(LocalDate.now().formatMedium(), style = IOSText.caption, color = colors.secondary)
            }
            OutlinedTextField(
                value = note,
                onValueChange = onChange,
                placeholder = {
                    Text(
                        leylaString(R.string.cycle_thoughts_placeholder),
                        style = IOSText.subheadline,
                        color = colors.secondary,
                    )
                },
                textStyle = IOSText.subheadline,
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Theme.rose.copy(alpha = 0.07f),
                    unfocusedContainerColor = Theme.rose.copy(alpha = 0.07f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    cursorColor = Theme.rose,
                ),
            )
        }
    }
    // Leaving the field is the moment a shared note is worth publishing.
    DisposableEffect(Unit) { onDispose { onCommit() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharingCard(
    level: CycleShareLevel,
    partnerName: String,
    onChange: (CycleShareLevel) -> Unit,
) {
    val colors = LeylaTheme.colors
    var expanded by remember { mutableStateOf(false) }

    LeylaCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                leylaString(R.string.cycle_share_with, partnerName),
                style = IOSText.headline,
                color = colors.ink,
            )
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Theme.rose.copy(alpha = 0.08f))
                        .plainClickable { expanded = true }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        leylaString(level.titleRes),
                        style = IOSText.subheadline.weight(FontWeight.Medium),
                        color = Theme.rose,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.UnfoldMore,
                        contentDescription = null,
                        tint = Theme.rose,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    CycleShareLevel.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(leylaString(option.titleRes)) },
                            onClick = {
                                expanded = false
                                if (option != level) onChange(option)
                            },
                        )
                    }
                }
            }
            Text(
                leylaString(level.explanationRes, partnerName),
                style = IOSText.footnote,
                color = colors.secondary,
            )
        }
    }
}

// MARK: - He supports her

@Composable
private fun PartnerContent(state: CycleState.Snapshot, partnerName: String) {
    val colors = LeylaTheme.colors
    val pregnancy = state.partnerPregnancy
    val pregnancyDue = pregnancy?.takeIf { it.sharing }?.let { PregnancyEngine.parseDueDate(it.dueDate) }
    // There is a cycle to show only when she is sharing *and* the phase she
    // shared is one we know — pair them so neither can be present without the
    // other.
    val sharedCycle = state.partner
        ?.takeIf { it.sharing }
        ?.let { partner -> CyclePhase.from(partner.phase)?.let { partner to it } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            pregnancyDue != null -> PartnerPregnancyView(pregnancyDue, partnerName)
            sharedCycle != null ->
                PartnerCycleView(sharedCycle.first, sharedCycle.second, partnerName)
            else -> InfoCard(leylaString(R.string.cycle_partner_not_sharing, partnerName))
        }

        // Even on this path, say plainly where the data comes from and that
        // nothing is read from this phone.
        LeylaCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    leylaString(R.string.health_source),
                    style = IOSText.headline,
                    color = Theme.rose,
                )
                Text(
                    leylaString(R.string.cycle_supporter_health_note, partnerName),
                    style = IOSText.subheadline,
                    color = colors.secondary,
                )
            }
        }
        // No cycle-tracking controls here — a supporter never tracks or shares a
        // cycle of their own. Correcting the answer lives in Settings ▸ You.
    }
}

@Composable
private fun PartnerCycleView(partner: PartnerCycle, phase: CyclePhase, partnerName: String) {
    val colors = LeylaTheme.colors

    partner.cycleDay?.let { day ->
        Column(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PhaseRing(
                phase = phase,
                cycleDay = day,
                cycleLength = estimatedLength(day, partner.periodInDays),
            )
            partner.periodInDays?.let { pid ->
                Text(
                    if (pid <= 0) leylaString(R.string.cycle_partner_period_any_day)
                    else leylaString(R.string.cycle_partner_period_in_days, pid),
                    style = IOSText.footnote,
                    color = colors.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        }
    }

    LeylaCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(phase.icon, contentDescription = null, tint = phase.color, modifier = Modifier.size(20.dp))
                Text(leylaString(phase.titleRes), style = IOSText.headline, color = phase.color)
            }
            ExplainerRow(leylaString(R.string.cycle_whats_happening), leylaString(phase.aboutRes))
            ExplainerRow(leylaString(R.string.cycle_what_she_may_feel), leylaString(phase.symptomsRes))
        }
    }

    LeylaCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(leylaString(R.string.cycle_how_to_support), style = IOSText.headline, color = colors.ink)
            phase.partnerTipsRes.forEach { tip ->
                TipRow(text = leylaString(tip), tint = phase.color)
            }
        }
    }

    partner.note?.takeIf { it.isNotBlank() }?.let { note ->
        LeylaCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    leylaString(R.string.cycle_partner_thoughts, partnerName),
                    style = IOSText.headline,
                    color = colors.ink,
                )
                Text(note, style = IOSText.subheadline, color = colors.secondary)
            }
        }
    }
}

@Composable
private fun PartnerPregnancyView(due: LocalDate, partnerName: String) {
    val colors = LeylaTheme.colors
    val pg = PregnancyEngine.insights(due)

    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PregnancyRing(week = pg.week, daysToDue = pg.daysToDue)
    }

    LeylaCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.ChildCare, contentDescription = null, tint = Theme.rose, modifier = Modifier.size(20.dp))
                Text(
                    leylaString(R.string.pregnancy_partner_expecting, partnerName),
                    style = IOSText.headline,
                    color = Theme.rose,
                )
            }
            Text(
                leylaString(R.string.pregnancy_partner_week_size, pg.week, leylaString(pg.babySizeRes)),
                style = IOSText.subheadline,
                color = colors.secondary,
            )
            HorizontalDivider(color = colors.hairline)
            Text(
                leylaString(PregnancyEngine.trimesterAbout(pg.trimester)),
                style = IOSText.subheadline,
                color = colors.secondary,
            )
        }
    }

    LeylaCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(leylaString(R.string.cycle_how_to_support), style = IOSText.headline, color = colors.ink)
            PregnancyEngine.trimesterSupport(pg.trimester).forEach { tip ->
                TipRow(text = leylaString(tip), tint = Theme.rose)
            }
        }
    }
}

// MARK: - Building blocks

@Composable
private fun ExplainerRow(label: String, text: String) {
    val colors = LeylaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = IOSText.subheadline.weight(FontWeight.SemiBold), color = colors.ink)
        Text(text, style = IOSText.subheadline, color = colors.secondary)
    }
}

@Composable
private fun TipRow(text: String, tint: Color) {
    val colors = LeylaTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
        Text(text, style = IOSText.subheadline, color = colors.ink)
    }
}

@Composable
private fun InfoCard(text: String) {
    val colors = LeylaTheme.colors
    LeylaCard {
        Text(text, style = IOSText.subheadline, color = colors.secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PrivacyNote() {
    val colors = LeylaTheme.colors
    Text(
        leylaString(R.string.cycle_privacy_note),
        style = IOSText.caption,
        color = colors.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun HealthDetailPage(
    isConnected: Boolean,
    isBusy: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = LeylaTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        TextAction(
            text = leylaString(R.string.back),
            onClick = onBack,
            color = Theme.rose,
            style = IOSText.subheadline.weight(FontWeight.SemiBold),
            modifier = Modifier.padding(start = 20.dp, top = 16.dp),
        )
        HealthConnectDetailScreen(isConnected = isConnected, onRefresh = onRefresh, isBusy = isBusy)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePicker(onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val defaultDue = LocalDate.now().plusMonths(7)
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = defaultDue.toEpochMillis(),
        // A due date in the past is a pregnancy that already ended.
        selectableDates = FutureDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = pickerState.selectedDateMillis?.toLocalDate() ?: defaultDue
                onPick(picked)
            }) { Text(leylaString(R.string.pregnancy_start_tracking)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(leylaString(R.string.cancel)) } },
    ) {
        DatePicker(state = pickerState, title = {
            Text(
                leylaString(R.string.pregnancy_due_date_question),
                style = IOSText.title2.weight(FontWeight.Bold),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp),
            )
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object FutureDates : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis >= LocalDate.now().toEpochMillis()
}

// MARK: - Small helpers

/** iOS's `.formatted(date: .abbreviated)` — "15 Sep 2026" in the user's locale. */
@Composable
private fun LocalDate.formatMedium(): String =
    format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()

/**
 * The supporter's ring has only what she shares — estimate her cycle length
 * from her cycle day plus the days-to-next-period she shared (fallback 28).
 */
private fun estimatedLength(cycleDay: Int, periodInDays: Int?): Int =
    periodInDays?.let { (cycleDay + it).coerceIn(21, 40) } ?: CycleEngine.DEFAULT_CYCLE_LENGTH
