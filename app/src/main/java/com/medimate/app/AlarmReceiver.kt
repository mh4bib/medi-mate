package com.medimate.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderService.start(context)   // rings until the user responds
        Scheduler.schedule(context)      // line up the next day's alarm
    }
}
