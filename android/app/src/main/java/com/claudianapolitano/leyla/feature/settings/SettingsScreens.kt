package com.claudianapolitano.leyla.feature.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.claudianapolitano.leyla.BuildConfig
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.leylaString
import com.claudianapolitano.leyla.core.HealthConnectManager
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.PartnerPrefs
import com.claudianapolitano.leyla.core.PartnerPronoun
import com.claudianapolitano.leyla.core.Session
import com.claudianapolitano.leyla.designsystem.IOSText
import com.claudianapolitano.leyla.designsystem.LeylaCard
import com.claudianapolitano.leyla.designsystem.LeylaTheme
import com.claudianapolitano.leyla.designsystem.Theme
import com.claudianapolitano.leyla.designsystem.weight
import com.claudianapolitano.leyla.feature.cycle.CycleState
import com.claudianapolitano.leyla.feature.cycle.HealthConnect
import com.claudianapolitano.leyla.feature.premium.PremiumStore
import com.claudianapolitano.leyla.feature.together.TextAction
import com.claudianapolitano.leyla.feature.together.plainClickable
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * Root Settings screen. Each domain (profile, relationship, health, …) lives in
 * its own page so the top-level list stays scannable. Port of
 * `Us/Features/Settings/SettingsView.swift`.
 *
 */
@Composable
fun SettingsScreen(
    onOpenProfile: () -> Unit,
    onOpenRelationship: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val isPremium by PremiumStore.isPremium.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var notificationsOn by remember { mutableStateOf<Boolean?>(null) }

    // Re-read on every resume: the only way to turn these back on after a
    // refusal is the system settings, so the answer changes while we are away.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsOn = granted }

    fun enableNotifications() {
        // Below 13 there is no permission to ask for, and after a refusal the
        // dialog never shows again — either way the system settings are the
        // only place the person can actually turn these on.
        if (Build.VERSION.SDK_INT >= 33 && notificationsOn == null) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
    }

    SettingsPage(modifier) {
        SettingsGroup {
            SettingsRow(
                icon = Icons.Filled.Person,
                tint = Theme.coral,
                title = leylaString(R.string.settings_profile),
                onClick = onOpenProfile,
            )
            HorizontalDivider(color = colors.hairline)
            SettingsRow(
                icon = Icons.Filled.Favorite,
                tint = Theme.rose,
                title = leylaString(R.string.settings_relationship),
                onClick = onOpenRelationship,
            )
            HorizontalDivider(color = colors.hairline)
            SettingsRow(
                icon = Icons.Filled.MonitorHeart,
                tint = Color(0xFFE5484D),
                title = leylaString(R.string.health_source),
                onClick = onOpenHealth,
            )
        }

        SectionHeader(leylaString(R.string.settings_app_section))
        SettingsGroup {
            SettingsRow(
                icon = Icons.Filled.Language,
                tint = Color(0xFF2F7DD1),
                title = leylaString(R.string.settings_language),
                detail = currentLanguageName(),
                onClick = onOpenLanguage,
            )
            HorizontalDivider(color = colors.hairline)
            SettingsRow(
                icon = Icons.Filled.Notifications,
                tint = Theme.coral,
                title = leylaString(R.string.settings_notifications),
                detail = when (notificationsOn) {
                    true -> leylaString(R.string.settings_on)
                    false -> leylaString(R.string.settings_off)
                    null -> ""
                },
                onClick = ::enableNotifications,
            )
        }

        SettingsGroup {
            SettingsRow(
                icon = Icons.Filled.AutoAwesome,
                tint = Theme.coral,
                title = leylaString(R.string.settings_premium),
                detail = if (isPremium) leylaString(R.string.settings_premium_active)
                else PremiumStore.priceLine,
                onClick = onOpenPremium,
            )
        }

        SettingsGroup {
            SettingsRow(
                icon = Icons.Filled.Key,
                tint = colors.secondary,
                title = leylaString(R.string.settings_account),
                onClick = onOpenAccount,
            )
        }
    }
}

// MARK: - Relationship

/**
 * Everything about "us": who the partner is, how to refer to them, and when the
 * relationship began. Port of `RelationshipSettingsView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipSettingsScreen(modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val session by Session.snapshot.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var pronoun by remember { mutableStateOf(PartnerPrefs.pronoun) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    val partnerName = session.partner?.displayName?.takeIf { it.isNotBlank() }
    val partnerFirstName = partnerName?.substringBefore(' ') ?: leylaString(R.string.settings_them)

    // Seed from the account once it has loaded, without stamping over a date
    // the person has just picked.
    LaunchedEffect(session.couple?.startDate) {
        if (startDate == null) {
            startDate = session.couple?.startDate?.let {
                runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
            }
        }
    }

    SettingsPage(modifier) {
        SectionHeader(leylaString(R.string.settings_your_partner))
        SettingsGroup {
            LabeledValue(
                leylaString(R.string.settings_partner),
                partnerName ?: "—",
            )
            HorizontalDivider(color = colors.hairline)
            PronounPicker(
                label = leylaString(R.string.settings_refer_to_as, partnerFirstName),
                selected = pronoun,
                onSelect = {
                    pronoun = it
                    scope.launch { Session.setPartnerPronoun(it) }
                },
            )
        }

        SettingsGroup {
            Row(
                Modifier
                    .fillMaxWidth()
                    .plainClickable { showPicker = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(leylaString(R.string.settings_together_since), style = IOSText.body, color = colors.ink)
                Text(
                    startDate?.let(::mediumDate) ?: "—",
                    style = IOSText.body.weight(FontWeight.SemiBold),
                    color = Theme.coral,
                )
            }
            HorizontalDivider(color = colors.hairline)
            LabeledValue(
                leylaString(R.string.settings_days_together),
                startDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).coerceAtLeast(0).toString() } ?: "—",
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (startDate ?: LocalDate.now()).toUtcMillis(),
            selectableDates = NotInTheFuture,
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = millis.toLocalDate()
                        startDate = picked
                        scope.launch { Session.saveStartDate(picked.toString()) }
                    }
                    showPicker = false
                }) { Text(leylaString(R.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(leylaString(R.string.cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun PronounPicker(label: String, selected: PartnerPronoun, onSelect: (PartnerPronoun) -> Unit) {
    val colors = LeylaTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .plainClickable { expanded = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = IOSText.body, color = colors.ink, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    leylaString(selected.labelRes),
                    style = IOSText.body.weight(FontWeight.SemiBold),
                    color = Theme.coral,
                )
                Icon(
                    Icons.Filled.UnfoldMore,
                    contentDescription = null,
                    tint = Theme.coral,
                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PartnerPronoun.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(leylaString(option.labelRes)) },
                    onClick = { expanded = false; if (option != selected) onSelect(option) },
                )
            }
        }
    }
}

private val PartnerPronoun.labelRes: Int
    get() = when (this) {
        PartnerPronoun.SHE -> R.string.pronoun_she
        PartnerPronoun.HE -> R.string.pronoun_he
        PartnerPronoun.THEY -> R.string.pronoun_they
    }

// MARK: - Health Connect

/** Connection status plus the way into Health Connect. Port of `AppleHealthSettingsView`. */
@Composable
fun HealthSettingsScreen(modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cycle by CycleState.snapshot.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        HealthConnectManager.requestPermissionsContract(),
    ) { scope.launch { CycleState.onPermissionResult(context) } }

    LaunchedEffect(Unit) { CycleState.refreshOnAppear(context) }

    // Same gate as iOS: only offer the connect button to someone who tracks a
    // cycle, on a device that can do it, who hasn't granted it yet.
    val canConnect = cycle.userHasCycle == true &&
        !cycle.healthConnected &&
        cycle.healthAvailability == HealthConnectManager.Availability.AVAILABLE

    SettingsPage(modifier) {
        SectionHeader(leylaString(R.string.settings_health_section))
        SettingsGroup {
            LabeledValue(
                leylaString(R.string.health_source),
                leylaString(
                    if (cycle.healthConnected) R.string.health_connected else R.string.health_not_connected,
                ),
            )
            if (canConnect) {
                HorizontalDivider(color = colors.hairline)
                TextAction(
                    text = leylaString(R.string.health_connect_action),
                    onClick = { permissionLauncher.launch(HealthConnectManager.permissions) },
                    icon = Icons.Filled.MonitorHeart,
                    color = Theme.rose,
                    style = IOSText.body.weight(FontWeight.SemiBold),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            HorizontalDivider(color = colors.hairline)
            TextAction(
                text = leylaString(R.string.health_open_settings),
                onClick = { runCatching { context.startActivity(HealthConnect.settingsIntent()) } },
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                color = Theme.rose,
                style = IOSText.body.weight(FontWeight.SemiBold),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        Text(
            leylaString(R.string.settings_health_footer),
            style = IOSText.footnote,
            color = colors.secondary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

// MARK: - Premium

/** Subscription state, price and restore. Port of `PremiumSettingsView`. */
@Composable
fun PremiumSettingsScreen(onOpenPaywall: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val context = LocalContext.current
    val isPremium by PremiumStore.isPremium.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var notificationsOn by remember { mutableStateOf<Boolean?>(null) }

    // Re-read on every resume: the only way to turn these back on after a
    // refusal is the system settings, so the answer changes while we are away.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsOn = granted }

    fun enableNotifications() {
        // Below 13 there is no permission to ask for, and after a refusal the
        // dialog never shows again — either way the system settings are the
        // only place the person can actually turn these on.
        if (Build.VERSION.SDK_INT >= 33 && notificationsOn == null) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }
    }

    SettingsPage(modifier) {
        SettingsGroup {
            if (isPremium) {
                LabeledValue(
                    leylaString(R.string.settings_premium),
                    leylaString(R.string.settings_premium_active),
                )
            } else {
                SettingsRow(
                    icon = Icons.Filled.AutoAwesome,
                    tint = Theme.coral,
                    title = leylaString(R.string.settings_unlock_premium),
                    detail = PremiumStore.priceLine,
                    onClick = onOpenPaywall,
                )
            }
            if (BuildConfig.DEBUG) {
                HorizontalDivider(color = colors.hairline)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(leylaString(R.string.settings_dev_unlock), style = IOSText.body, color = colors.ink)
                    Switch(
                        checked = isPremium,
                        onCheckedChange = { PremiumStore.setEntitled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Theme.rose),
                    )
                }
            }
        }
        Text(
            leylaString(
                if (isPremium) R.string.settings_premium_footer_active
                else R.string.settings_premium_footer_free,
            ),
            style = IOSText.footnote,
            color = colors.secondary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

// MARK: - Account

/**
 * Sign out, unpair, delete — kept away from the day-to-day settings so a
 * destructive tap can't be an accident of scrolling. Port of
 * `AccountSettingsView`.
 */
@Composable
fun AccountSettingsScreen(modifier: Modifier = Modifier) {
    val colors = LeylaTheme.colors
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf<Destructive?>(null) }

    SettingsPage(modifier) {
        SettingsGroup {
            TextAction(
                text = leylaString(R.string.settings_sign_out),
                onClick = { scope.launch { Session.signOut() } },
                color = colors.ink,
                style = IOSText.body,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }

        SettingsGroup {
            TextAction(
                text = leylaString(R.string.settings_unpair),
                onClick = { confirming = Destructive.UNPAIR },
                color = Theme.coral,
                style = IOSText.body,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            HorizontalDivider(color = colors.hairline)
            TextAction(
                text = leylaString(R.string.settings_delete_account),
                onClick = { confirming = Destructive.DELETE },
                color = Theme.coral,
                style = IOSText.body,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        Text(
            leylaString(R.string.settings_account_footer),
            style = IOSText.footnote,
            color = colors.secondary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    confirming?.let { action ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(leylaString(action.titleRes)) },
            text = { Text(leylaString(action.bodyRes)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    scope.launch {
                        when (action) {
                            Destructive.UNPAIR -> {
                                runCatching { LeylaApi.unpair() }
                                Session.loadCouple()
                            }
                            Destructive.DELETE -> {
                                runCatching { LeylaApi.deleteAccount() }
                                Session.signOut()
                            }
                        }
                    }
                }) { Text(leylaString(action.confirmRes), color = Theme.coral) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(leylaString(R.string.cancel)) }
            },
        )
    }
}

private enum class Destructive(val titleRes: Int, val bodyRes: Int, val confirmRes: Int) {
    UNPAIR(R.string.settings_unpair_title, R.string.settings_unpair_body, R.string.settings_unpair),
    DELETE(R.string.settings_delete_title, R.string.settings_delete_body, R.string.settings_delete_forever),
}

// MARK: - Building blocks

/** The scrolling, grouped-list body every settings page shares. */
@Composable
private fun SettingsPage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LeylaTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    LeylaCard(cornerRadius = 18.dp, contentPadding = 16.dp) { content() }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = IOSText.caption.weight(FontWeight.SemiBold),
        color = LeylaTheme.colors.secondary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    onClick: () -> Unit,
    detail: String? = null,
) {
    val colors = LeylaTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .plainClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(title, style = IOSText.body, color = colors.ink, modifier = Modifier.weight(1f))
        if (detail != null) {
            Text(detail, style = IOSText.subheadline, color = colors.secondary)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.secondary.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    val colors = LeylaTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = IOSText.body, color = colors.ink)
        Text(value, style = IOSText.body, color = colors.secondary)
    }
}

// MARK: - Dates

private fun mediumDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** A relationship cannot have started tomorrow. */
@OptIn(ExperimentalMaterial3Api::class)
private object NotInTheFuture : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= LocalDate.now().toUtcMillis()
}
