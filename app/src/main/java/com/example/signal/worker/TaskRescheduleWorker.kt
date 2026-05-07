package com.example.signal.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.signal.R
import com.example.signal.data.repository.TaskRepository
import com.example.signal.ui.enforcement.EnforcementOverlayActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TaskRescheduleWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val task   = taskRepository.getTaskById(taskId) ?: return Result.failure()

        // ── 1. Directly launch the enforcement overlay ─────────────────────────
        // This fires even when the screen is off / locked because the Activity
        // declares FLAG_SHOW_WHEN_LOCKED + FLAG_TURN_SCREEN_ON in its onCreate.
        val overlayIntent = Intent(context, EnforcementOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EnforcementOverlayActivity.EXTRA_TASK_ID, taskId)
        }
        context.startActivity(overlayIntent)

        // ── 2. Also post a high-priority notification as a fallback ────────────
        // If the system blocks the direct Activity start (battery saver etc.)
        // the user can still tap the notification to reach the overlay.
        createNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val urgencyLabel = if (task.rescheduleCount >= 2) "⚠️ FINAL REMINDER" else "⏰ Scheduled Reminder"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$urgencyLabel — ${task.sourceApp}")
            .setContentText(task.extractedTask)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, /* highPriority = */ true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.hashCode(), notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled task reminders that re-open the enforcement overlay"
                enableVibration(true)
                enableLights(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val CHANNEL_ID  = "signal_reminders"
    }
}
