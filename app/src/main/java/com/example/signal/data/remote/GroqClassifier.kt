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
class GroqClassifier @Inject constructor(
    private val api: GroqApiService   // ← injected via NetworkModule, not a static object
) {

    private val gson = Gson()

    companion object {
        private const val TAG = "GroqClassifier"
        private const val MODEL = "llama-3.3-70b-versatile"

        private const val SYSTEM_PROMPT = """You are a notification classification AI. Analyze the notification below and return ONLY a valid JSON object with no markdown, no explanation, no backticks.

CRITICAL INSTRUCTIONS FOR IMPORTANCE:
1. You MUST factor in temporal urgency. The "Received" timestamp is the current time.
2. If a deadline or event is more than 3 days in the future, assign "low" or "medium" importance.
3. If a deadline or event is today or tomorrow, assign "high" or "critical" importance.
4. Promotional messages, ads, and general marketing should ALWAYS be "category": "promotional" and "importance": "low", and "requiresEnforcement": false.

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

            Log.d(TAG, "Classifying notification from ${notificationData.sourceApp}")
            val response = api.classify(request)
            val content = response.choices.firstOrNull()?.message?.content
                ?: return fallback(notificationData).also {
                    Log.w(TAG, "Empty response from Groq — using fallback")
                }

            Log.d(TAG, "Groq raw response: $content")
            parseResponse(notificationData.id, content)

        } catch (e: Exception) {
            Log.e(TAG, "Groq classification failed: ${e.message}", e)
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

            val deadlineTs = obj.get("deadlineTimestamp")?.takeIf { !it.isJsonNull }
                ?.asString?.let { parseIso(it) }

            val result = ClassifiedTask(
                notificationId = notificationId,
                importance = importance,
                category = category,
                task = obj.get("task")?.asString ?: "",
                deadline = obj.get("deadline")?.takeIf { !it.isJsonNull }?.asString,
                deadlineTimestamp = deadlineTs,
                suggestedActions = actions,
                requiresEnforcement = obj.get("requiresEnforcement")?.asBoolean ?: false
            )

            Log.d(TAG, "Classified: category=${result.category} importance=${result.importance}")
            result

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
