package com.medimate.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

object Scheduler {
    private const val REQ_DAILY = 100
    private const val REQ_SNOOZE = 101
    private const val REQ_SHOW_APP = 102
    private const val SNOOZE_MINUTES = 10L

    /** Next moment (epoch millis) at which the daily reminder should fire. */
    fun nextTrigger(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var t = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        if (!t.isAfter(now.plusSeconds(1))) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }

    /** (Re)schedules the next daily alarm, or cancels it if disabled. */
    fun schedule(ctx: Context) {
        val prefs = Prefs(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (!prefs.enabled) {
            am.cancel(firePending(ctx, REQ_DAILY))
            return
        }
        val trigger = nextTrigger(prefs.hour, prefs.minute)
        // setAlarmClock is exempt from Doze / battery limits and needs no extra permission.
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(trigger, showAppPending(ctx)),
            firePending(ctx, REQ_DAILY)
        )
    }

    /** One-off alarm N milliseconds from now (used for snooze and the test button). */
    fun fireIn(ctx: Context, millis: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(System.currentTimeMillis() + millis, showAppPending(ctx)),
            firePending(ctx, REQ_SNOOZE)
        )
    }

    fun snooze(ctx: Context) = fireIn(ctx, SNOOZE_MINUTES * 60_000L)

    private fun firePending(ctx: Context, req: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, req, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun showAppPending(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, REQ_SHOW_APP, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
