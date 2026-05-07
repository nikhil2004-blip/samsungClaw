package com.example.signal.fake

import com.example.signal.data.local.CategoryCount
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.model.ClassifiedTask
import com.example.signal.data.model.NotificationData
import com.example.signal.data.model.UserDecision
import com.example.signal.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTaskRepository : TaskRepository {

    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    fun reset() {
        tasks.value = emptyList()
    }

    override suspend fun insertFromClassified(notificationData: NotificationData, classified: ClassifiedTask) {
        val newTask = TaskEntity(
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
            suggestedActions = "[]",
            status = "PENDING",
            userDecision = null,
            ignoreReason = null,
            scheduledFor = null,
            decidedAt = null,
            completedAt = null,
            requiresEnforcement = classified.requiresEnforcement,
            isOverdue = false,
            rescheduleCount = 0
        )
        tasks.value = tasks.value + newTask
    }

    override suspend fun insertManualTask(task: TaskEntity) {
        tasks.value = tasks.value + task
    }

    override fun getAllTasks(): Flow<List<TaskEntity>> = tasks
    
    override fun getAllTasksSortedByPriority(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.sortedByDescending { it.importance == "CRITICAL" }
    }

    override fun getPendingTasks(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.status == "PENDING" }
    }

    override fun getInProgressTasks(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.status == "IN_PROGRESS" }
    }

    override fun getDoneTasks(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.status == "DONE" }
    }

    override fun getIgnoredTasks(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.status == "IGNORED" }
    }

    override fun getOverdueTasks(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.isOverdue }
    }

    override fun getTasksByCategory(category: String): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.category == category }
    }

    override fun getPromotionalTasks(): Flow<List<TaskEntity>> = tasks.map { list ->
        list.filter { it.category == "PROMOTIONAL" }
    }

    override suspend fun getTaskById(id: String): TaskEntity? = tasks.value.find { it.id == id }

    override fun observeTaskById(id: String): Flow<TaskEntity?> = tasks.map { list ->
        list.find { it.id == id }
    }

    override suspend fun applyDecision(taskId: String, decision: UserDecision) {
        val list = tasks.value.toMutableList()
        val index = list.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = list[index]
            list[index] = task.copy(
                userDecision = decision.name,
                status = if (decision == UserDecision.DO_NOW) "IN_PROGRESS" else "PENDING",
                decidedAt = System.currentTimeMillis()
            )
            tasks.value = list
        }
    }

    override suspend fun setIgnoreReason(taskId: String, reason: String) {
        val list = tasks.value.toMutableList()
        val index = list.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = list[index]
            list[index] = task.copy(ignoreReason = reason, status = "IGNORED")
            tasks.value = list
        }
    }

    override suspend fun scheduleTask(taskId: String, scheduledForMs: Long) {
        val list = tasks.value.toMutableList()
        val index = list.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = list[index]
            list[index] = task.copy(scheduledFor = scheduledForMs)
            tasks.value = list
        }
    }

    override suspend fun markDone(taskId: String) {
        val list = tasks.value.toMutableList()
        val index = list.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = list[index]
            list[index] = task.copy(status = "DONE", completedAt = System.currentTimeMillis())
            tasks.value = list
        }
    }

    override suspend fun markOverdueTasks() {
        val now = System.currentTimeMillis()
        val list = tasks.value.map { task ->
            if (task.deadlineTimestamp != null && task.deadlineTimestamp < now && task.status != "DONE") {
                task.copy(isOverdue = true)
            } else {
                task
            }
        }
        tasks.value = list
    }

    override suspend fun updateDynamicPriorities() {
        // No-op for fake
    }

    override suspend fun getTodayCount(startOfDay: Long): Int = tasks.value.count { it.capturedAt >= startOfDay }

    override suspend fun getTodayActionedCount(startOfDay: Long): Int = tasks.value.count {
        it.capturedAt >= startOfDay && it.userDecision == "DO_NOW"
    }

    override suspend fun getTodayScheduledCount(startOfDay: Long): Int = tasks.value.count {
        it.capturedAt >= startOfDay && it.userDecision == "SCHEDULE"
    }

    override suspend fun getTodayIgnoredCount(startOfDay: Long): Int = tasks.value.count {
        it.capturedAt >= startOfDay && it.userDecision == "IGNORE"
    }

    override suspend fun getOverdueCount(): Int = tasks.value.count { it.isOverdue }

    override suspend fun getTasksThisWeek(startOfWeek: Long): List<TaskEntity> = tasks.value.filter {
        it.capturedAt >= startOfWeek
    }

    override suspend fun getTopIgnoredCategories(): List<CategoryCount> {
        return tasks.value.filter { it.status == "IGNORED" }
            .groupBy { it.category }
            .map { (category, list) -> CategoryCount(category, list.size) }
            .sortedByDescending { it.cnt }
    }

    override suspend fun clearAll() {
        tasks.value = emptyList()
    }

    override fun parseSuggestedActions(json: String): List<String> = emptyList()
}
