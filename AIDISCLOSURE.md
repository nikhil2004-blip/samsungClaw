# SIGNAL - AI Disclosure & Usage

This document outlines how artificial intelligence was utilized both in the **development** of the SIGNAL application and as a core **runtime feature** of the product itself.

---

## 🛠️ AI in the Development Process

We put together this project using an awesome mix of tools to move incredibly fast during the hackathon. 

- **GitHub Copilot:** Heavily relied upon for code auto-completion, brainstorming logic, and writing boilerplate Jetpack Compose UI components.
- **Claude Models:** Used for architecting complex data flows, handling edge cases in the WorkManager logic, and refining the user experience.
- **Antigravity:** Integrated to help orchestrate multi-file codebase changes rapidly, allowing us to refactor architecture on the fly.

Our core development environments included **Android Studio**, **VS Code**, and **Zed** (for blazing fast side-edits).

---

## 🧠 AI as a Runtime Feature (Notification Classification)

SIGNAL uses an external Large Language Model (LLM) to power its core decision enforcement loop.

- **Model & Provider:** Groq - `llama-3.3-70b-versatile` (accessed via the Groq API).
- **Primary Purpose:** Real-time classification of notifications to determine importance, extract actionable tasks, identify deadlines, and suggest actions for enforcement.
- **Data Sent to API:** Only notification metadata (source app, title, body, and timestamp) is sent to the Groq API for classification.
- **Storage & Handling:** The repository does not include model weights. Structured classification outputs (importance, task text, timestamps) are stored locally in Room; raw model responses are not persisted beyond what is necessary for parsing.
- **Privacy Note:** Users must grant the Notification Listener permission to enable the feature. Before using the app on personal data, review the privacy implications and avoid sending highly sensitive content to third-party APIs.

### How The Classification Pipeline Works

Every notification goes through this real-time pipeline:

```kotlin
// GroqClassifier.kt (simplified)
suspend fun classify(notification: NotificationData): ClassifiedTask {
  val response = groqApi.classify(
    GroqRequest(
      model  = "llama-3.3-70b-versatile",
      messages = listOf(
        GroqMessage("system", SYSTEM_PROMPT),
        GroqMessage("user",  buildUserPrompt(notification))
      ),
      temperature = 0.1,  // low temp = deterministic JSON
      maxTokens  = 300
    )
  )
  return parseJson(response.choices[0].message.content)
}
```

The model returns structured JSON:

```json
{
 "importance": "critical",
 "category": "deadline",
 "task": "Submit IEEE paper by tonight 11:59 PM",
 "deadline": "tonight 11:59 PM",
 "deadlineTimestamp": "2024-11-15T23:59:00",
 "actions": ["Open WhatsApp", "Set Reminder", "Start Now"],
 "requiresEnforcement": true
}
```

Groq's **llama-3.3-70b** processes at ~500 tokens/second, ensuring the classification happens instantly before the user can passively swipe away the notification.
