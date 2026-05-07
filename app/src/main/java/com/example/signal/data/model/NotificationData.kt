package com.example.signal.data.model

data class NotificationData(
    val id: String,           // UUID generated on capture
    val sourceApp: String,    // "WhatsApp"
    val packageName: String,  // "com.whatsapp"
    val title: String,
    val body: String,
    val capturedAt: Long      // System.currentTimeMillis()
)
