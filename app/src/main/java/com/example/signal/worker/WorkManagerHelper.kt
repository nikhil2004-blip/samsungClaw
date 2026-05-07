package com.example.signal.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object WorkManagerHelper {

    fun scheduleTaskReminder(context: Context, taskId: String, scheduledTimeMs: Long) {
        val delay = scheduledTimeMs - System.currentTimeMillis()
        if (delay <= 0) return

        val data = workDataOf(TaskRescheduleWorker.KEY_TASK_ID to taskId)

        val request = OneTimeWorkRequestBuilder<TaskRescheduleWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .addTag(taskId)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(taskId, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelTaskReminder(context: Context, taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(taskId)
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
