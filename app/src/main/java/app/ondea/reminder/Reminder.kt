package app.ondea.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.ondea.MainActivity
import app.ondea.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Optional daily "take your temperature" reminder. Only the on/off switch and the time are
 * stored here (plain preferences, nothing health-related), so it can be rescheduled after boot.
 */
object Reminder {
    private const val PREFS = "reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_MINUTES = "minutes"
    private const val CHANNEL = "daily_reminder"
    private const val WINDOW_MS = 10 * 60 * 1000L

    fun isEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun minuteOfDay(ctx: Context) = prefs(ctx).getInt(KEY_MINUTES, 7 * 60)

    fun configure(ctx: Context, enabled: Boolean, minuteOfDay: Int) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).putInt(KEY_MINUTES, minuteOfDay).apply()
        schedule(ctx)
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(ctx)
        am.cancel(pi)
        if (!isEnabled(ctx)) return
        val minutes = minuteOfDay(ctx)
        val zone = ZoneId.systemDefault()
        var at = LocalDate.now(zone).atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(zone)
        if (!at.toInstant().isAfter(java.time.Instant.now())) at = at.plusDays(1)
        // An inexact window needs no special alarm permission and is gentle on battery.
        am.setWindow(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), WINDOW_MS, pi)
    }

    internal fun notify(ctx: Context) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, ctx.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Deliberately neutral wording: it may show on the lock screen.
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ctx.getString(R.string.reminder_title))
            .setContentText(ctx.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1, n)
        } catch (_: SecurityException) {
        }
    }

    private fun pendingIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminder.notify(context)
        Reminder.schedule(context)
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESCHEDULE_ACTIONS) Reminder.schedule(context)
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
