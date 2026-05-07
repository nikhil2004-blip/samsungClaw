package com.example.signal.service

import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.signal.data.model.NotificationData
import com.example.signal.data.remote.GroqClassifier
import com.example.signal.data.repository.TaskRepository
import com.example.signal.ui.enforcement.EnforcementOverlayActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class NotificationInterceptorService : NotificationListenerService() {

    @Inject lateinit var groqClassifier: GroqClassifier
    @Inject lateinit var taskRepository: TaskRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var blockedPackages: Set<String>

    override fun onCreate() {
        super.onCreate()
        blockedPackages = setOf(
            packageName,
            "android",
            "com.android.systemui",
            "com.android.phone",
            "com.android.launcher3",
            "com.google.android.gms",
            "com.samsung.android.lool",
            "com.sec.android.app.launcher"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val pkg = sbn.packageName ?: return
        if (pkg in blockedPackages) return

        // Skip ongoing / non-clearable notifications (charging, music player, etc.)
        if (sbn.isOngoing) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val body  = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Skip empty notifications
        if (title.isBlank() && body.isBlank()) return

        val sourceApp = getAppName(pkg)
        val notificationData = NotificationData(
            id          = UUID.randomUUID().toString(),
            sourceApp   = sourceApp,
            packageName = pkg,
            title       = title,
            body        = body,
            capturedAt  = System.currentTimeMillis()
        )

        serviceScope.launch {
            try {
                val classified = groqClassifier.classify(notificationData)
                taskRepository.insertFromClassified(notificationData, classified)

                if (classified.requiresEnforcement) {
                    val intent = Intent(applicationContext, EnforcementOverlayActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(EnforcementOverlayActivity.EXTRA_TASK_ID, notificationData.id)
                    }
                    applicationContext.startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process notification: ${e.message}")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // no-op for now
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun getAppName(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    companion object {
        private const val TAG = "NotificationInterceptor"
    }
}
