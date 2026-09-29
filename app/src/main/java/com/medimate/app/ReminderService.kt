package com.medimate.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import java.time.LocalDate

/**
 * Rings (alarm sound + vibration) until the user answers on the reminder screen.
 * While it runs, every phone unlock brings the reminder screen back.
 */
class ReminderService : Service() {

    companion object {
        private const val ACTION_STOP = "com.medimate.app.STOP"
        private const val CHANNEL = "reminder_alarm"
        private const val OLD_CHANNEL = "reminder"
        private const val NOTIF_ID = 1

        /** Sound/vibration stop by themselves after this long (notification + unlock pop-up stay). */
        private const val MAX_RING_MS = 10 * 60_000L

        /** Lets the reminder screen close itself when the service ends (e.g. Done tapped in the shade). */
        @Volatile
        var onStopped: (() -> Unit)? = null

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, ReminderService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ReminderService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringing = false
    private var receiverRegistered = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            showScreen()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(OLD_CHANNEL) // old channel had its own (short) sound
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(
                CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)          // the service plays the sound itself
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )

        if (!ringing) {
            ringing = true
            startRinging()
            handler.postDelayed({ stopRinging() }, MAX_RING_MS)
            registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
            receiverRegistered = true
        }
        showScreen() // works from the background when "display over other apps" is allowed
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        if (receiverRegistered) {
            try { unregisterReceiver(unlockReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        onStopped?.invoke()
        super.onDestroy()
    }

    private fun showScreen() {
        try {
            startActivity(
                Intent(this, ReminderActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            // Blocked without overlay permission; the full-screen notification still covers a locked phone.
        }
    }

    private fun startRinging() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val r = RingtoneManager.getRingtone(this, uri)
            r.setAudioAttributes(attrs)
            r.setLooping(true)
            r.play()
            ringtone = r
        } catch (_: Exception) {}

        try {
            val v = getSystemService(Vibrator::class.java)
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attrs)
            vibrator = v
        } catch (_: Exception) {}

        try {
            val wl = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "medimate:ring")
            wl.acquire(MAX_RING_MS + 10_000L)
            wakeLock = wl
        } catch (_: Exception) {}
    }

    private fun stopRinging() {
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        wakeLock = null
    }

    private fun buildNotification(): Notification {
        val left = Prefs(this).isLeftOn(LocalDate.now())

        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, ReminderActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val done = PendingIntent.getService(
            this, 2,
            Intent(this, ReminderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val doneAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_stat_drop),
            getString(R.string.done),
            done
        ).build()

        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, Bn.handLoc(this, left)))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .addAction(doneAction)
            .build()
    }
}
