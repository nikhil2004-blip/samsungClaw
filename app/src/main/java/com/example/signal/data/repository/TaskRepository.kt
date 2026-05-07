package com.example.signal.data.repository

import com.example.signal.data.local.CategoryCount
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.model.ClassifiedTask
import com.example.signal.data.model.NotificationData
import com.example.signal.data.model.UserDecision
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    suspend fun insertFromClassified(notificationData: NotificationData, classified: ClassifiedTask)
    suspend fun insertManualTask(task: TaskEntity)
    
    fun getAllTasks(): Flow<List<TaskEntity>>
    fun getAllTasksSortedByPriority(): Flow<List<TaskEntity>>
    fun getPendingTasks(): Flow<List<TaskEntity>>
    fun getInProgressTasks(): Flow<List<TaskEntity>>
    fun getDoneTasks(): Flow<List<TaskEntity>>
    fun getIgnoredTasks(): Flow<List<TaskEntity>>
    fun getOverdueTasks(): Flow<List<TaskEntity>>
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>>
    fun getPromotionalTasks(): Flow<List<TaskEntity>>
    
    suspend fun getTaskById(id: String): TaskEntity?
    fun observeTaskById(id: String): Flow<TaskEntity?>
    
    suspend fun applyDecision(taskId: String, decision: UserDecision)
    suspend fun setIgnoreReason(taskId: String, reason: String)
    suspend fun scheduleTask(taskId: String, scheduledForMs: Long)
    suspend fun markDone(taskId: String)
    suspend fun markOverdueTasks()
    suspend fun updateDynamicPriorities()
    
    suspend fun getTodayCount(startOfDay: Long): Int
    suspend fun getTodayActionedCount(startOfDay: Long): Int
    suspend fun getTodayScheduledCount(startOfDay: Long): Int
    suspend fun getTodayIgnoredCount(startOfDay: Long): Int
    suspend fun getOverdueCount(): Int
    suspend fun getTasksThisWeek(startOfWeek: Long): List<TaskEntity>
    suspend fun getTopIgnoredCategories(): List<CategoryCount>
    
    suspend fun clearAll()
    fun parseSuggestedActions(json: String): List<String>
}
