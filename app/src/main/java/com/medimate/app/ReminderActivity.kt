package com.medimate.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalTime

class ReminderActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_reminder)

        val now = LocalTime.now()
        findViewById<TextView>(R.id.clockText).text = Bn.time(now.hour, now.minute)

        val left = Prefs(this).isLeftOn(LocalDate.now())
        findViewById<TextView>(R.id.handText).text = Notifier.handLabel(this, left)

        findViewById<TextView>(R.id.btnDone).setOnClickListener {
            Notifier.dismiss(this)
            finish()
        }
        findViewById<TextView>(R.id.btnSnooze).setOnClickListener {
            Scheduler.snooze(this)
            Notifier.dismiss(this)
            finish()
        }
    }
}
