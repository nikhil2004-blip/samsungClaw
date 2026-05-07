package com.example.signal.data.repository

import com.example.signal.data.local.AppDatabase
import com.example.signal.data.local.CategoryCount
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: AppDatabase
) {
    private val dao = db.taskDao()
    private val gson = Gson()

    // ── Insert ─────────────────────────────────────────────────────────────────

    suspend fun insertFromClassified(
        notificationData: com.example.signal.data.model.NotificationData,
        classified: ClassifiedTask
    ) {
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
            status = TaskStatus.PENDING.name,
            userDecision = null,
            ignoreReason = null,
            scheduledFor = null,
            decidedAt = null,
            completedAt = null,
            requiresEnforcement = classified.requiresEnforcement,
            isOverdue = false,
            rescheduleCount = 0
        )
        dao.insertTask(entity)
    }

    suspend fun insertManualTask(task: TaskEntity) = dao.insertTask(task)

    // ── Observe ────────────────────────────────────────────────────────────────

    fun getAllTasks(): Flow<List<TaskEntity>> = dao.getAllTasks()
    fun getPendingTasks(): Flow<List<TaskEntity>> = dao.getPendingTasks()
    fun getInProgressTasks(): Flow<List<TaskEntity>> = dao.getInProgressTasks()
    fun getDoneTasks(): Flow<List<TaskEntity>> = dao.getDoneTasks()
    fun getIgnoredTasks(): Flow<List<TaskEntity>> = dao.getIgnoredTasks()
    fun getOverdueTasks(): Flow<List<TaskEntity>> = dao.getOverdueTasks()
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>> =
        dao.getTasksByCategory(category)

    suspend fun getTaskById(id: String): TaskEntity? = dao.getTaskById(id)
    fun observeTaskById(id: String) = dao.observeTaskById(id)

    // ── Decisions ──────────────────────────────────────────────────────────────

    suspend fun applyDecision(taskId: String, decision: UserDecision) {
        val status = when (decision) {
            UserDecision.DO_NOW -> TaskStatus.IN_PROGRESS.name
            UserDecision.SCHEDULE -> TaskStatus.PENDING.name
            UserDecision.DELEGATE -> TaskStatus.PENDING.name
            UserDecision.IGNORE -> TaskStatus.IGNORED.name
        }
        dao.updateDecision(taskId, status, decision.name, System.currentTimeMillis())
    }

    suspend fun setIgnoreReason(taskId: String, reason: String) =
        dao.updateIgnoreReason(taskId, reason)

    suspend fun scheduleTask(taskId: String, scheduledForMs: Long) =
        dao.updateSchedule(taskId, scheduledForMs)

    suspend fun markDone(taskId: String) =
        dao.markDone(taskId, System.currentTimeMillis())

    suspend fun markOverdueTasks() = dao.markOverdueTasks(System.currentTimeMillis())

    // ── Dashboard ──────────────────────────────────────────────────────────────

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
