package com.example.signal.data.model

data class ClassifiedTask(
    val notificationId: String,
    val importance: ImportanceLevel,
    val category: TaskCategory,
    val task: String,
    val deadline: String?,
    val deadlineTimestamp: Long?,
    val suggestedActions: List<String>,
    val requiresEnforcement: Boolean
)
