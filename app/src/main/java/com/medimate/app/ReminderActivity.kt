package com.medimate.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import java.time.LocalDate
import java.time.LocalTime

class ReminderActivity : Activity() {

    private val stopListener: () -> Unit = { runOnUiThread { finish() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_reminder)

        ReminderService.onStopped = stopListener

        findViewById<TextView>(R.id.btnDone).setOnClickListener {
            ReminderService.stop(this)
            finish()
        }
        findViewById<TextView>(R.id.btnSnooze).setOnClickListener {
            Scheduler.snooze(this)
            ReminderService.stop(this)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val now = LocalTime.now()
        findViewById<TextView>(R.id.clockText).text = Bn.time(now.hour, now.minute)
        val left = Prefs(this).isLeftOn(LocalDate.now())
        findViewById<TextView>(R.id.handText).text = Bn.handLoc(this, left)
    }

    override fun onDestroy() {
        if (ReminderService.onStopped === stopListener) ReminderService.onStopped = null
        super.onDestroy()
    }
}
