package com.medimate.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped on reboot / app update / time change, so re-arm them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.schedule(context)
    }
}
