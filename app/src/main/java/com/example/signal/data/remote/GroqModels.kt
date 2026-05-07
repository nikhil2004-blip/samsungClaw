package com.example.signal.data.remote

import com.google.gson.annotations.SerializedName

// ── Request ───────────────────────────────────────────────────────────────────

data class GroqRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Double = 0.1,
    @SerializedName("max_tokens") val maxTokens: Int = 300
)

data class GroqMessage(
    @SerializedName("role") val role: String,     // "system" | "user"
    @SerializedName("content") val content: String
)

// ── Response ──────────────────────────────────────────────────────────────────

data class GroqResponse(
    @SerializedName("choices") val choices: List<GroqChoice>
)

data class GroqChoice(
    @SerializedName("message") val message: GroqResponseMessage
)

data class GroqResponseMessage(
    @SerializedName("content") val content: String
)
