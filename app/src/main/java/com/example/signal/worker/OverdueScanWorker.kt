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
import com.example.signal.MainActivity
import com.example.signal.R
import com.example.signal.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OverdueScanWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        taskRepository.markOverdueTasks()
        val overdueCount = taskRepository.getOverdueCount()

        if (overdueCount > 0) {
            sendDigestNotification(overdueCount)
        }
        return Result.success()
    }

    private fun sendDigestNotification(overdueCount: Int) {
        createChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Good morning! SIGNAL Digest")
            .setContentText("You have $overdueCount overdue task(s). Tap to review.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(DIGEST_NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Daily Digest", NotificationManager.IMPORTANCE_DEFAULT)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    companion object {
        const val CHANNEL_ID            = "signal_digest"
        const val DIGEST_NOTIFICATION_ID = 9999
    }
}
