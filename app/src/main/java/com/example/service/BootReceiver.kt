package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("diary_settings", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("reminder_enabled", false)
            if (isEnabled) {
                val hour = prefs.getInt("reminder_hour", 21)
                val minute = prefs.getInt("reminder_minute", 0)
                ReminderScheduler.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
