package com.example.signal.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ── Insert / Update ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    // ── Single task ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun observeTaskById(taskId: String): Flow<TaskEntity?>

    // ── Board queries ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM tasks ORDER BY capturedAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * All active (non-ignored, non-done) tasks sorted by:
     *   1. Priority: CRITICAL(0) > HIGH(1) > MEDIUM(2) > LOW(3)
     *   2. Then deadline ASC (tasks with no deadline go last)
     */
    @Query("""
        SELECT * FROM tasks
        WHERE status NOT IN ('DONE','IGNORED')
        ORDER BY
          CASE importance
            WHEN 'CRITICAL' THEN 0
            WHEN 'HIGH'     THEN 1
            WHEN 'MEDIUM'   THEN 2
            ELSE                 3
          END ASC,
          CASE WHEN deadlineTimestamp IS NULL THEN 1 ELSE 0 END ASC,
          deadlineTimestamp ASC
    """)
    fun getAllTasksSortedByPriority(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' ORDER BY CASE WHEN deadlineTimestamp IS NULL THEN 1 ELSE 0 END, deadlineTimestamp ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'IN_PROGRESS' ORDER BY CASE WHEN deadlineTimestamp IS NULL THEN 1 ELSE 0 END, deadlineTimestamp ASC")
    fun getInProgressTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'DONE' ORDER BY completedAt DESC")
    fun getDoneTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'IGNORED' AND category != 'PROMOTIONAL' ORDER BY capturedAt DESC")
    fun getIgnoredTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isOverdue = 1 AND status != 'DONE' ORDER BY deadlineTimestamp ASC")
    fun getOverdueTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY capturedAt DESC")
    fun getTasksByCategory(category: String): Flow<List<TaskEntity>>

    /** Promotional / Advert tasks (auto-ignored) — shown in the Adverts tab */
    @Query("SELECT * FROM tasks WHERE category = 'PROMOTIONAL' ORDER BY capturedAt DESC")
    fun getPromotionalTasks(): Flow<List<TaskEntity>>

    // ── Dashboard / analytics queries ─────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM tasks WHERE capturedAt >= :startOfDay")
    suspend fun getTodayCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE userDecision = 'DO_NOW' AND capturedAt >= :startOfDay")
    suspend fun getTodayActionedCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE userDecision = 'SCHEDULE' AND capturedAt >= :startOfDay")
    suspend fun getTodayScheduledCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE userDecision = 'IGNORE' AND capturedAt >= :startOfDay")
    suspend fun getTodayIgnoredCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE deadlineTimestamp < :now AND status != 'DONE'")
    suspend fun getOverdueCount(now: Long): Int

    @Query("SELECT * FROM tasks WHERE capturedAt >= :startOfWeek ORDER BY capturedAt ASC")
    suspend fun getTasksThisWeek(startOfWeek: Long): List<TaskEntity>

    @Query("""
        SELECT category, COUNT(*) as cnt FROM tasks 
        WHERE userDecision = 'IGNORE' 
        GROUP BY category 
        ORDER BY cnt DESC 
        LIMIT 3
    """)
    suspend fun getTopIgnoredCategories(): List<CategoryCount>

    // ── Update helpers ────────────────────────────────────────────────────────

    @Query("UPDATE tasks SET status = :status, userDecision = :decision, decidedAt = :now WHERE id = :taskId")
    suspend fun updateDecision(taskId: String, status: String, decision: String, now: Long)

    @Query("UPDATE tasks SET ignoreReason = :reason WHERE id = :taskId")
    suspend fun updateIgnoreReason(taskId: String, reason: String)

    @Query("UPDATE tasks SET scheduledFor = :scheduledFor, rescheduleCount = rescheduleCount + 1 WHERE id = :taskId")
    suspend fun updateSchedule(taskId: String, scheduledFor: Long)

    @Query("UPDATE tasks SET isOverdue = 1 WHERE deadlineTimestamp < :now AND status != 'DONE'")
    suspend fun markOverdueTasks(now: Long)

    @Query("UPDATE tasks SET status = 'DONE', completedAt = :now WHERE id = :taskId")
    suspend fun markDone(taskId: String, now: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    @Query("""
        UPDATE tasks 
        SET importance = CASE 
            WHEN deadlineTimestamp < :now + 86400000 THEN 'CRITICAL'
            WHEN deadlineTimestamp < :now + 259200000 THEN 'HIGH'
            WHEN deadlineTimestamp < :now + 604800000 THEN 'MEDIUM'
            ELSE 'LOW'
        END
        WHERE deadlineTimestamp IS NOT NULL AND status NOT IN ('DONE', 'IGNORED')
    """)
    suspend fun updateDynamicPriorities(now: Long)
}

// Helper for top-ignored category query
data class CategoryCount(
    val category: String,
    val cnt: Int
)
