package com.medimate.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate

object Notifier {
    private const val CHANNEL = "reminder"
    const val NOTIF_ID = 1

    fun handLabel(ctx: Context, left: Boolean): String =
        ctx.getString(if (left) R.string.hand_left_loc else R.string.hand_right_loc)

    private fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val ch = NotificationChannel(
            CHANNEL, ctx.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(ch)
    }

    fun showReminder(ctx: Context) {
        ensureChannel(ctx)
        val left = Prefs(ctx).isLeftOn(LocalDate.now())

        val open = PendingIntent.getActivity(
            ctx, 1,
            Intent(ctx, ReminderActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle(ctx.getString(R.string.notif_title))
            .setContentText(ctx.getString(R.string.notif_text, handLabel(ctx, left)))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        ctx.getSystemService(NotificationManager::class.java).notify(NOTIF_ID, n)
    }

    fun dismiss(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
    }
}
