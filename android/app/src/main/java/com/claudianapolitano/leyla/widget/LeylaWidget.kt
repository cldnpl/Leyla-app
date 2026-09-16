package com.claudianapolitano.leyla.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.claudianapolitano.leyla.MainActivity
import com.claudianapolitano.leyla.R
import com.claudianapolitano.leyla.core.LeylaApi
import com.claudianapolitano.leyla.core.WidgetStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        // The tap is the whole point of the widget, so it must not wait for the
        // app to open. A widget's onReceive gets ~10 seconds; one POST fits.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { LeylaApi.sendMissYou() }
        }
        refresh(context)
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
        fun isPlaced(context: Context): Boolean =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, LeylaWidget::class.java))
                .isNotEmpty()

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
