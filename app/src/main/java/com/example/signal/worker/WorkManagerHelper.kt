package com.example.signal.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkManagerHelper {

    fun scheduleTaskReminder(context: Context, taskId: String, scheduledTimeMs: Long) {
        val delay = scheduledTimeMs - System.currentTimeMillis()
        if (delay <= 0) return
        // Use exact AlarmManager for precise snooze timing
        AlarmScheduler.schedule(context, taskId, scheduledTimeMs)
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        AlarmScheduler.cancel(context, taskId)
    }

    fun scheduleOverdueScan(context: Context) {
        val request = PeriodicWorkRequestBuilder<OverdueScanWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "overdue_scan",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Runs SweepWorker every 15 minutes (WorkManager minimum) to move
     * past-deadline tasks → MISSED even when the main UI is not open.
     * This fixes the case where a user schedules a snooze, the alarm fires,
     * they dismiss the overlay, and the original deadline passes in the background.
     */
    fun scheduleMissedSweep(context: Context) {
        val request = PeriodicWorkRequestBuilder<SweepWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "missed_sweep",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Delay until next 8:00 AM */
    private fun computeInitialDelay(): Long {
        val now = java.util.Calendar.getInstance()
        val next8am = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return next8am.timeInMillis - now.timeInMillis
    }
}
