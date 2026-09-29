package com.medimate.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var timePicker: TimePicker
    private lateinit var enableSwitch: Switch
    private lateinit var pillLeft: TextView
    private lateinit var pillRight: TextView
    private lateinit var nextText: TextView

    private var left = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        timePicker = findViewById(R.id.timePicker)
        enableSwitch = findViewById(R.id.enableSwitch)
        pillLeft = findViewById(R.id.pillLeft)
        pillRight = findViewById(R.id.pillRight)
        nextText = findViewById(R.id.nextText)

        timePicker.setIs24HourView(false)
        timePicker.hour = prefs.hour
        timePicker.minute = prefs.minute
        enableSwitch.isChecked = prefs.enabled

        left = prefs.isLeftOn(LocalDate.now())
        pillLeft.setOnClickListener { left = true; renderPills() }
        pillRight.setOnClickListener { left = false; renderPills() }
        findViewById<TextView>(R.id.btnSave).setOnClickListener { save() }

        renderPills()
        renderNext()
        askNotificationPermission()
    }

    private fun save() {
        timePicker.clearFocus()
        prefs.hour = timePicker.hour
        prefs.minute = timePicker.minute
        prefs.enabled = enableSwitch.isChecked
        prefs.setTodayHand(left)
        Scheduler.schedule(this)
        renderNext()
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    private fun renderPills() {
        pillLeft.setBackgroundResource(if (left) R.drawable.bg_pill_on else R.drawable.bg_pill_off)
        pillRight.setBackgroundResource(if (left) R.drawable.bg_pill_off else R.drawable.bg_pill_on)
        pillLeft.setTextColor(if (left) Color.WHITE else getColor(R.color.text_secondary))
        pillRight.setTextColor(if (left) getColor(R.color.text_secondary) else Color.WHITE)
    }

    private fun renderNext() {
        if (!prefs.enabled) {
            nextText.setText(R.string.reminder_off)
            return
        }
        val zone = ZoneId.systemDefault()
        val trigger = Instant.ofEpochMilli(Scheduler.nextTrigger(prefs.hour, prefs.minute))
            .atZone(zone)
        val day = when (trigger.toLocalDate()) {
            LocalDate.now() -> getString(R.string.today)
            LocalDate.now().plusDays(1) -> getString(R.string.tomorrow)
            else -> Bn.num(trigger.toLocalDate().toString())
        }
        val hand = getString(
            if (prefs.isLeftOn(trigger.toLocalDate())) R.string.left_hand else R.string.right_hand
        )
        nextText.text = getString(
            R.string.next_reminder, day, Bn.time(trigger.hour, trigger.minute), hand
        )
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
