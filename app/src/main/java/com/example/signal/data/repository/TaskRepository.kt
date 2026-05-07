package com.example.signal.data.repository

import android.content.Context
import com.example.signal.data.local.AppDatabase
import com.example.signal.data.local.CategoryCount
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.model.*
import com.example.signal.utils.CalendarHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val dao = db.taskDao()
    private val gson = Gson()

    // ── Insert ─────────────────────────────────────────────────────────────────

    suspend fun insertFromClassified(
        notificationData: com.example.signal.data.model.NotificationData,
        classified: ClassifiedTask
    ) {
        // Promotional notifications are auto-ignored — they never surface in the main board
        val autoIgnorePromo = classified.category == TaskCategory.PROMOTIONAL &&
                !classified.requiresEnforcement
        val initialStatus = if (autoIgnorePromo) TaskStatus.IGNORED.name else TaskStatus.PENDING.name

        val entity = TaskEntity(
            id = notificationData.id,
            sourceApp = notificationData.sourceApp,
            packageName = notificationData.packageName,
            originalTitle = notificationData.title,
            originalBody = notificationData.body,
            capturedAt = notificationData.capturedAt,
            extractedTask = classified.task,
            importance = classified.importance.name,
            category = classified.category.name,
            deadline = classified.deadline,
            deadlineTimestamp = classified.deadlineTimestamp,
            suggestedActions = gson.toJson(classified.suggestedActions),
            status = initialStatus,
            userDecision = if (autoIgnorePromo) UserDecision.IGNORE.name else null,
            ignoreReason = if (autoIgnorePromo) "Auto-ignored promotional" else null,
            scheduledFor = null,
            decidedAt = if (autoIgnorePromo) System.currentTimeMillis() else null,
            completedAt = null,
            requiresEnforcement = classified.requiresEnforcement,
            isOverdue = false,
            rescheduleCount = 0
        )
        dao.insertTask(entity)

        // ── Auto-add MEETING tasks to device calendar ──────────────────────────
        if (classified.category == TaskCategory.MEETING &&
            classified.deadlineTimestamp != null &&
            CalendarHelper.hasCalendarPermission(context)
        ) {
            CalendarHelper.insertMeetingEvent(
                context = context,
                title = classified.task.ifBlank { notificationData.title },
                description = "From ${notificationData.sourceApp}: ${notificationData.body}",
                startMs = classified.deadlineTimestamp
            )
        }
    }

    suspend fun insertManualTask(task: TaskEntity) = dao.insertTask(task)

    // ── Observe ────────────────────────────────────────────────────────────────

    fun getAllTasks(): Flow<List<TaskEntity>> = dao.getAllTasks()
    fun getAllTasksSortedByPriority(): Flow<List<TaskEntity>> = dao.getAllTasksSortedByPriority()
    fun getPendingTasks(): Flow<List<TaskEntity>> = dao.getPendingTasks()
    fun getInProgressTasks(): Flow<List<TaskEntity>> = dao.getInProgressTasks()
    fun getDoneTasks(): Flow<List<TaskEntity>> = dao.getDoneTasks()
    fun getIgnoredTasks(): Flow<List<TaskEntity>> = dao.getIgnoredTasks()
    fun getOverdueTasks(): Flow<List<TaskEntity>> = dao.getOverdueTasks()
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>> =
        dao.getTasksByCategory(category)
    fun getPromotionalTasks(): Flow<List<TaskEntity>> = dao.getPromotionalTasks()
    fun getMissedTasks(): Flow<List<TaskEntity>> = dao.getMissedTasks()

    suspend fun getTaskById(id: String): TaskEntity? = dao.getTaskById(id)
    fun observeTaskById(id: String) = dao.observeTaskById(id)

    // ── Decisions ──────────────────────────────────────────────────────────────

    suspend fun applyDecision(taskId: String, decision: UserDecision) {
        val status = when (decision) {
            UserDecision.DO_NOW   -> TaskStatus.IN_PROGRESS.name
            UserDecision.SCHEDULE -> TaskStatus.PENDING.name
            UserDecision.DELEGATE -> TaskStatus.PENDING.name
            UserDecision.IGNORE   -> TaskStatus.IGNORED.name
        }
        dao.updateDecision(taskId, status, decision.name, System.currentTimeMillis())
    }

    suspend fun setIgnoreReason(taskId: String, reason: String) =
        dao.updateIgnoreReason(taskId, reason)

    suspend fun scheduleTask(taskId: String, scheduledForMs: Long) =
        dao.updateSchedule(taskId, scheduledForMs)

    suspend fun markDone(taskId: String) =
        dao.markDone(taskId, System.currentTimeMillis())

    suspend fun markOverdueTasks() {
        val now = System.currentTimeMillis()
        dao.markOverdueTasks(now)
        dao.updateDynamicPriorities(now)
    }

    /**
     * Sweep past-deadline tasks into MISSED.
     * Call this periodically (e.g. every minute) from the ViewModel.
     */
    suspend fun sweepMissedTasks() {
        dao.sweepMissedTasks(System.currentTimeMillis())
    }

    /** User taps "I did this" on a missed task — moves it to DONE (translucent). */
    suspend fun markMissedAsDone(taskId: String) =
        dao.markMissedAsDone(taskId, System.currentTimeMillis())

    suspend fun updateDynamicPriorities() {
        dao.updateDynamicPriorities(System.currentTimeMillis())
    }

    suspend fun getTodayCount(startOfDay: Long) = dao.getTodayCount(startOfDay)
    suspend fun getTodayActionedCount(startOfDay: Long) = dao.getTodayActionedCount(startOfDay)
    suspend fun getTodayScheduledCount(startOfDay: Long) = dao.getTodayScheduledCount(startOfDay)
    suspend fun getTodayIgnoredCount(startOfDay: Long) = dao.getTodayIgnoredCount(startOfDay)
    suspend fun getOverdueCount() = dao.getOverdueCount(System.currentTimeMillis())
    suspend fun getTasksThisWeek(startOfWeek: Long) = dao.getTasksThisWeek(startOfWeek)
    suspend fun getTopIgnoredCategories(): List<CategoryCount> = dao.getTopIgnoredCategories()

    // ── Helpers ────────────────────────────────────────────────────────────────

    suspend fun clearAll() = dao.clearAll()

    fun parseSuggestedActions(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
