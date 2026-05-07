package com.example.signal.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.signal.R
import com.example.signal.ui.enforcement.EnforcementOverlayActivity

/**
 * Receives the exact alarm and immediately launches the Enforcement Overlay
 * so the user is forced to act at the scheduled snooze time.
 *
 * Also posts a heads-up notification as a fallback in case the system
 * blocks the direct Activity start (strict background restrictions).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        // ── 1. Directly launch enforcement overlay ─────────────────────────────
        val overlayIntent = Intent(context, EnforcementOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EnforcementOverlayActivity.EXTRA_TASK_ID, taskId)
        }
        context.startActivity(overlayIntent)

        // ── 2. High-priority heads-up notification as fallback ─────────────────
        ensureChannel(context)

        val pi = PendingIntent.getActivity(
            context, taskId.hashCode(), overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Reminder — Action Required")
            .setContentText("Your scheduled task needs attention. Tap to decide.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pi, true)   // Shows even on lock screen
            .setContentIntent(pi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.hashCode(), notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Task Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Fires when a snoozed task's reminder time arrives"
                        enableVibration(true)
                        enableLights(true)
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val CHANNEL_ID    = "signal_reminders"
    }
}
