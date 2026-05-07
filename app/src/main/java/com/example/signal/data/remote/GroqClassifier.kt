package com.example.signal.data.remote

import android.util.Log
import com.example.signal.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqClassifier @Inject constructor() {

    private val gson = Gson()
    private val api = GroqRetrofitClient.service

    companion object {
        private const val TAG = "GroqClassifier"
        private const val MODEL = "llama-3.3-70b-versatile"

        private const val SYSTEM_PROMPT = """You are a notification classification AI. Analyze the notification below and return ONLY a valid JSON object with no markdown, no explanation, no backticks.

JSON schema:
{
  "importance": "critical" | "high" | "medium" | "low",
  "category": "deadline" | "meeting" | "payment" | "message" | "reminder" | "promotional" | "other",
  "task": "string (extracted task in plain English)",
  "deadline": "string | null (natural language, e.g. tonight 11:59 PM)",
  "deadlineTimestamp": "ISO8601 string | null",
  "actions": ["string array of 2-4 suggested action labels"],
  "requiresEnforcement": true | false
}"""
    }

    suspend fun classify(notificationData: NotificationData): ClassifiedTask {
        return try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(notificationData.capturedAt))

            val userContent = """App: ${notificationData.sourceApp}
Title: ${notificationData.title}
Message: ${notificationData.body}
Received: $timestamp"""

            val request = GroqRequest(
                model = MODEL,
                messages = listOf(
                    GroqMessage("system", SYSTEM_PROMPT),
                    GroqMessage("user", userContent)
                )
            )

            val response = api.classify(request)
            val content = response.choices.firstOrNull()?.message?.content
                ?: return fallback(notificationData)

            parseResponse(notificationData.id, content)

        } catch (e: Exception) {
            Log.e(TAG, "Groq classification failed: ${e.message}")
            fallback(notificationData)
        }
    }

    private fun parseResponse(notificationId: String, json: String): ClassifiedTask {
        return try {
            // Strip any accidental markdown fences
            val clean = json.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val obj = gson.fromJson(clean, JsonObject::class.java)

            val importance = when (obj.get("importance")?.asString?.lowercase()) {
                "critical" -> ImportanceLevel.CRITICAL
                "high"     -> ImportanceLevel.HIGH
                "medium"   -> ImportanceLevel.MEDIUM
                else       -> ImportanceLevel.LOW
            }

            val category = when (obj.get("category")?.asString?.lowercase()) {
                "deadline"    -> TaskCategory.DEADLINE
                "meeting"     -> TaskCategory.MEETING
                "payment"     -> TaskCategory.PAYMENT
                "message"     -> TaskCategory.MESSAGE
                "reminder"    -> TaskCategory.REMINDER
                "promotional" -> TaskCategory.PROMOTIONAL
                else          -> TaskCategory.OTHER
            }

            val actions = obj.getAsJsonArray("actions")
                ?.map { it.asString }
                ?: emptyList()

            // Parse ISO8601 deadline timestamp
            val deadlineTs = obj.get("deadlineTimestamp")?.takeIf { !it.isJsonNull }
                ?.asString?.let { parseIso(it) }

            ClassifiedTask(
                notificationId = notificationId,
                importance = importance,
                category = category,
                task = obj.get("task")?.asString ?: "",
                deadline = obj.get("deadline")?.takeIf { !it.isJsonNull }?.asString,
                deadlineTimestamp = deadlineTs,
                suggestedActions = actions,
                requiresEnforcement = obj.get("requiresEnforcement")?.asBoolean ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message}")
            ClassifiedTask(
                notificationId = notificationId,
                importance = ImportanceLevel.MEDIUM,
                category = TaskCategory.OTHER,
                task = "Review this notification",
                deadline = null,
                deadlineTimestamp = null,
                suggestedActions = listOf("Open App", "Review Later"),
                requiresEnforcement = false
            )
        }
    }

    private fun parseIso(iso: String): Long? {
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            )
            formats.firstNotNullOfOrNull { fmt ->
                runCatching { fmt.parse(iso)?.time }.getOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fallback(data: NotificationData) = ClassifiedTask(
        notificationId = data.id,
        importance = ImportanceLevel.MEDIUM,
        category = TaskCategory.OTHER,
        task = data.title.ifBlank { "Review notification from ${data.sourceApp}" },
        deadline = null,
        deadlineTimestamp = null,
        suggestedActions = listOf("Open App", "Mark Done", "Ignore"),
        requiresEnforcement = false
    )
}
