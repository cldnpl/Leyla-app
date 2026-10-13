package com.claudianapolitano.leyla.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.claudianapolitano.leyla.MainActivity
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.AppPrefs
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.MissYouStatus
import com.claudianapolitano.leyla.core.PartnerPrefs
import com.claudianapolitano.leyla.core.PartnerPronoun
import com.claudianapolitano.leyla.core.SharedConfig
import com.claudianapolitano.leyla.core.WidgetStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * The Leyla home-screen widget: days together, the distance between you, and a
 * tap that sends "I miss you" without opening the app. Port of the three
 * widgets in `ios/UsWidget/UsWidget.swift`, folded into one.
 *
 * iOS ships them separately because WidgetKit asks the person to choose a kind
 * and a size up front. Android's widget picker shows one entry per provider and
 * resizes freely afterwards, so three near-identical entries would be three
 * ways to ask the same question — one widget that shows both numbers reads
 * better in that gallery.
 *
 * Everything it draws comes from [WidgetStore]. A widget wakes on the system's
 * schedule, with no session in memory and often no network, so it must never
 * need a request to render.
 */
class LeylaWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_MISS_YOU) return

        val snapshot = WidgetStore.load(context)
        // A second tap while the first is still sending would just send twice.
        if (snapshot.missYouStatus == MissYouStatus.SENDING) return

        // The tap is the whole point of the widget, so it must not wait for the
        // app to open. goAsync keeps the process alive for the POST and the
        // few seconds of "Sent ✓" — all inside the ~10 seconds a receiver gets,
        // which is why the request is capped.
        val pending = goAsync()
        WidgetStore.saveMissYouStatus(MissYouStatus.SENDING, SEND_TIMEOUT_MS + STATUS_SHOWN_MS)
        redraw(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sent = withTimeoutOrNull(SEND_TIMEOUT_MS) {
                    runCatching {
                        // The demo couple has no server row to send to.
                        if (!(SharedConfig.DEMO_MODE && AppPrefs.testPaired)) LeylaApi.sendMissYou()
                    }.isSuccess
                } ?: false

                // Said on the widget itself, which works whatever the
                // notification setting — Android hides a background app's
                // toast when its notifications are off.
                WidgetStore.saveMissYouStatus(
                    if (sent) MissYouStatus.SENT else MissYouStatus.FAILED,
                    STATUS_SHOWN_MS,
                )
                redraw(context)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, toastText(context, sent, snapshot.partnerName), Toast.LENGTH_SHORT)
                        .show()
                }

                delay(STATUS_SHOWN_MS)
                WidgetStore.saveMissYouStatus(null)
                redraw(context)
            } finally {
                pending.finish()
            }
        }
    }

    /** Redraws in place — quicker than a refresh broadcast round trip. */
    private fun redraw(context: Context) {
        AppWidgetManager.getInstance(context)
            .updateAppWidget(ComponentName(context, LeylaWidget::class.java), buildViews(context))
    }

    private fun toastText(context: Context, sent: Boolean, partnerName: String?): String {
        if (!sent) return context.getString(R.string.widget_miss_you_failed)
        val name = partnerName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.your_partner)
        return context.getString(
            when (PartnerPrefs.pronoun) {
                PartnerPronoun.SHE -> R.string.miss_you_toast_her
                PartnerPronoun.HE -> R.string.miss_you_toast_him
                PartnerPronoun.THEY -> R.string.miss_you_toast_them
            },
            name,
        )
    }

    private fun buildViews(context: Context): RemoteViews {
        val snapshot = WidgetStore.load(context)
        val views = RemoteViews(context.packageName, R.layout.widget_leyla)

        val days = snapshot.daysTogether
        views.setTextViewText(
            R.id.widget_days,
            days?.toString() ?: context.getString(R.string.widget_dash),
        )
        views.setTextViewText(R.id.widget_days_label, context.getString(R.string.widget_days_together))

        // While a heart tap is in flight (and briefly after), its status takes
        // the day count's place so the tap visibly did something.
        val status = snapshot.missYouStatus
        val showStatus = status != null
        views.setViewVisibility(R.id.widget_status, if (showStatus) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_days, if (showStatus) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_days_label, if (showStatus) View.GONE else View.VISIBLE)
        if (status != null) {
            views.setTextViewText(
                R.id.widget_status,
                context.getString(
                    when (status) {
                        MissYouStatus.SENDING -> R.string.widget_sending
                        MissYouStatus.SENT -> R.string.widget_sent
                        MissYouStatus.FAILED -> R.string.widget_send_failed
                    },
                ),
            )
        }

        val km = snapshot.distanceKm
        views.setTextViewText(
            R.id.widget_distance,
            when {
                km == null -> context.getString(R.string.widget_no_distance)
                else -> context.getString(R.string.widget_km_apart, km.roundToInt())
            },
        )

        views.setTextViewText(
            R.id.widget_partner,
            snapshot.partnerName ?: context.getString(R.string.app_name),
        )

        // Tapping the heart sends the nudge; tapping anywhere else opens the app,
        // which is what someone expects from a widget they can't read fully.
        views.setOnClickPendingIntent(R.id.widget_heart, missYouIntent(context))
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        return views
    }

    private fun missYouIntent(context: Context): PendingIntent {
        val intent = Intent(context, LeylaWidget::class.java).setAction(ACTION_MISS_YOU)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val ACTION_MISS_YOU = "com.claudianapolitano.leyla.widget.MISS_YOU"

        /** Long enough for a slow network, short enough to fit a receiver's budget. */
        private const val SEND_TIMEOUT_MS = 6_000L

        /** How long "Sent ✓" stays up before the day count comes back. */
        private const val STATUS_SHOWN_MS = 2_500L

        /** Redraws every placed widget. Safe to call when none exist. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LeylaWidget::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, LeylaWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }

        /** Whether the person has actually placed one, which the guide asks. */
        fun isPlaced(context: Context): Boolean = placedCount(context) > 0

        /** How many Leyla widgets are on the home screen right now. */
        fun placedCount(context: Context): Int =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, LeylaWidget::class.java))
                .size

        /** Xiaomi, Redmi and POCO all run Xiaomi's launcher and permission model. */
        fun isXiaomi(): Boolean =
            Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.BRAND.lowercase() in setOf("xiaomi", "redmi", "poco")

        /**
         * Opens the screen where "Home screen shortcuts" can be turned on —
         * Xiaomi's own permission editor when it exists, the standard app
         * settings page otherwise.
         */
        fun openShortcutPermission(context: Context) {
            val miui = Intent("miui.intent.action.APP_PERM_EDITOR")
                .setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity",
                )
                .putExtra("extra_pkgname", context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val fallback = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(miui) }
                .onFailure { runCatching { context.startActivity(fallback) } }
        }

        /**
         * Whether this launcher lets an app ask for the widget to be pinned.
         *
         * This is the one place Android can do something iOS cannot: WidgetKit
         * gives no way to place a widget from inside the app, so iOS has to
         * teach the gesture. Most Android launchers accept the request and just
         * show a confirmation — but not all do, which is why the written steps
         * stay on the screen either way.
         */
        fun canRequestPin(context: Context): Boolean =
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

        /** Asks the launcher to place the widget, with its own confirmation. */
        fun requestPin(context: Context): Boolean {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            return manager.requestPinAppWidget(
                ComponentName(context, LeylaWidget::class.java),
                null,
                null,
            )
        }
    }
}
