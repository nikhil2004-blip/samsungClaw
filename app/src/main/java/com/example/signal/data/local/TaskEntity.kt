package com.example.signal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val sourceApp: String,
    val packageName: String,
    val originalTitle: String,
    val originalBody: String,
    val capturedAt: Long,
    val extractedTask: String,
    val importance: String,         // CRITICAL | HIGH | MEDIUM | LOW
    val category: String,           // DEADLINE | MEETING | PAYMENT | ...
    val deadline: String?,
    val deadlineTimestamp: Long?,
    val suggestedActions: String,   // JSON array stored as string
    val status: String,             // PENDING | IN_PROGRESS | DONE | IGNORED
    val userDecision: String?,      // DO_NOW | SCHEDULE | DELEGATE | IGNORE
    val ignoreReason: String?,
    val scheduledFor: Long?,
    val decidedAt: Long?,
    val completedAt: Long?,
    val requiresEnforcement: Boolean,
    val isOverdue: Boolean,
    val rescheduleCount: Int = 0
)
