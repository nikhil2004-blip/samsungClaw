<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="100" alt="SIGNAL Icon" />

# SIGNAL
### AI-Powered Notification Intelligence & Decision Enforcement

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Groq](https://img.shields.io/badge/AI-Groq%20LLaMA%203.3-F55036?logo=meta&logoColor=white)](https://groq.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-API%2026-4F46E5)](https://developer.android.com/tools/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-10B981)](LICENSE)

*Built in 24 hours at MS Ramaiah Institute of Technology · Team BBBY*

</div>

---

## The Problem

Your phone gets hundreds of notifications a day. An IEEE paper deadline sits next to a pizza discount. A client meeting reminder is buried under 23 group messages. You swipe them all away — and forget the deadline.

**Existing tools don't fix this.** Notification managers just group the noise. Task apps need manual entry. AI assistants only respond when asked.

The missing layer is **Decision Enforcement** — a system that recognises what's important and ensures you actually decide what to do with it, right now, before it's forgotten.

---

## What SIGNAL Does

SIGNAL runs silently in the background, intercepts every notification, and sends it through a Groq AI pipeline that classifies its importance, extracts the actual task, and identifies the deadline. For anything critical or high-priority, it surfaces a **mandatory full-screen overlay** — you cannot swipe it away. You must choose: *Do Now, Schedule, Delegate, or Ignore (with a reason)*.

Every decision is logged. Over time, your dashboard shows you exactly where your attention goes and where it disappears.

```
Raw Notification
      ↓
AI Classification  (Groq · LLaMA 3.3-70b · ~500 tok/s)
      ↓
Structured Task  (importance · category · deadline · actions)
      ↓
Enforcement Overlay  (mandatory decision — no swipe-away)
      ↓
Logged Decision  (do now / schedule / delegate / ignore + reason)
      ↓
Automated Follow-up  (WorkManager reminder at exact scheduled time)
      ↓
Accountability Dashboard  (streaks · weekly chart · avoidance patterns)
```

---

## Features

### Notification Interceptor
- Captures every incoming notification system-wide via `NotificationListenerService`
- Filters noise — duplicates, empty bodies, system chrome notifications
- Extracts source app, title, body, and timestamp asynchronously

### Groq AI Classification Engine
- Sends each notification to **Groq API** (`llama-3.3-70b-versatile`)
- Returns structured JSON: importance level, category, extracted task, natural-language deadline, ISO timestamp, suggested action labels, and `requiresEnforcement` flag
- Responds in under 2 seconds — fast enough to feel instant
- Graceful fallback to manual review queue on API failure

### Decision Enforcement Overlay
- Full-screen overlay (`TYPE_APPLICATION_OVERLAY`) that cannot be dismissed passively
- Back button shows a warning — you must pick an action
- Four decisions:
  - **Do Now** — marks in-progress, opens source app directly
  - **Schedule** — date/time picker → WorkManager exact-time reminder → re-surfaces overlay at that moment
  - **Delegate** — opens share sheet to forward the task
  - **Ignore** — requires a typed reason (min 10 chars) — no silent dismissal
- Live countdown timer on time-sensitive tasks
- Spring-physics entrance animation

### Smart Task Board (Inbox)
- Unified inbox with filter chips: All · Priority · Deadlines · Meetings · Payments · Messages · Reminders · Missed · Adverts · Other
- Section grouping: Overdue (pulsing red border) · High Priority · In Progress · Pending · Completed
- Tap to expand — full original notification body, captured timestamp, decision history
- FAB to add manual tasks (tasks received outside the app)
- Live search across task text and source app name

### Accountability Dashboard
- Today at-a-glance: Captured · Actioned · Scheduled · Ignored
- 7-day stacked bar chart (built with Compose Canvas API — no external chart library)
- Streak counter — consecutive days with zero ignored critical tasks
- Most-Avoided list — categories you keep deferring
- AI behavioral insight — dynamic tip based on your current pattern

### Automation Engine
- **Deferred task resurfacing** — WorkManager re-shows enforcement overlay at exactly the scheduled time; snooze twice and the option is removed
- **Overdue escalation** — missed deadlines re-appear with an OVERDUE badge
- **Google Calendar auto-integration** — meeting notifications auto-create calendar events with 15-minute reminders
- **Daily digest** — 8 AM push: pending count + overdue count
- **Quiet Hours** — batch overnight enforcements; show consolidated overlay on wake

### Settings
- Light / Dark mode toggle (stored in DataStore, applies instantly app-wide)
- Enforcement level: Critical Only · High & Critical · All
- Quiet Hours switch (11 PM – 7 AM)
- Calendar sync toggle
- Danger zone: Clear all data with confirmation

---

## Architecture

```
┌─────────────────────────────────────────────┐
│  UI LAYER  (Jetpack Compose + Material 3)   │
│  TaskBoardScreen · DashboardScreen           │
│  SettingsScreen · OnboardingScreen           │
│  EnforcementOverlayActivity                  │
└──────────────────┬──────────────────────────┘
                   │  StateFlow / collectAsStateWithLifecycle
┌──────────────────▼──────────────────────────┐
│  VIEWMODEL LAYER                             │
│  TaskViewModel · DashboardViewModel          │
│  SettingsViewModel                           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  REPOSITORY  (single source of truth)        │
│  TaskRepository · OnboardingRepository       │
└────────┬─────────────────────────┬──────────┘
         │                         │
┌────────▼────────┐    ┌──────────▼──────────┐
│  ROOM DATABASE  │    │  GROQ API (Retrofit) │
│  TaskDao        │    │  llama-3.3-70b       │
│  TaskEntity     │    │  Google Calendar API │
└─────────────────┘    └─────────────────────┘
         ▲
┌────────┴─────────────────────────────────────┐
│  BACKGROUND LAYER                             │
│  NotificationListenerService                  │
│  WorkManager Workers                          │
│  AlarmScheduler · ReminderReceiver            │
└───────────────────────────────────────────────┘
```

**Pattern:** MVVM + Clean Architecture  
**DI:** Hilt (Dagger)  
**Async:** Kotlin Coroutines + Flow throughout

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose · Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel · StateFlow · `collectAsStateWithLifecycle` |
| DI | Hilt (Dagger) |
| Local DB | Room (SQLite) |
| Preferences | DataStore Preferences |
| Networking | Retrofit 2 · OkHttp 3 · Gson |
| AI | Groq API — `llama-3.3-70b-versatile` |
| Background | WorkManager · NotificationListenerService |
| Overlay | `TYPE_APPLICATION_OVERLAY` system window |
| Calendar | Google Calendar API |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 35 |

---

## Project Structure

```
app/src/main/java/com/example/signal/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt          # Room DB setup
│   │   ├── TaskDao.kt              # All DB queries
│   │   └── TaskEntity.kt           # DB schema
│   ├── model/
│   │   ├── ClassifiedTask.kt
│   │   ├── ImportanceLevel.kt
│   │   ├── TaskCategory.kt
│   │   └── TaskStatus.kt
│   ├── remote/
│   │   ├── GroqApiService.kt       # Retrofit interface
│   │   ├── GroqClassifier.kt       # AI call + JSON parsing
│   │   └── GroqModels.kt           # Request/response models
│   └── repository/
│       ├── OnboardingRepository.kt # DataStore: onboarding + dark mode
│       └── TaskRepository.kt       # All business logic
├── di/
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── WorkerModule.kt
├── service/
│   └── NotificationInterceptorService.kt
├── ui/
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   ├── enforcement/
│   │   ├── EnforcementOverlayActivity.kt
│   │   └── EnforcementViewModel.kt
│   ├── navigation/
│   │   └── MainNavigation.kt
│   ├── onboarding/
│   │   └── OnboardingScreen.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── taskboard/
│   │   ├── TaskBoardScreen.kt
│   │   └── TaskViewModel.kt
│   └── theme/
│       ├── Color.kt                # Full design-token palette
│       ├── Theme.kt                # Light + Dark ColorScheme
│       └── Type.kt                 # Typography scale
├── utils/
│   └── CalendarHelper.kt
├── worker/
│   ├── AlarmScheduler.kt
│   ├── OverdueScanWorker.kt
│   ├── ReminderReceiver.kt
│   ├── SweepWorker.kt
│   ├── TaskRescheduleWorker.kt
│   └── WorkManagerHelper.kt
├── MainActivity.kt
└── SignalApplication.kt
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android device or emulator running API 26+
- A free [Groq API key](https://console.groq.com) — the free tier is more than enough

### 1. Clone the repo
```bash
git clone https://github.com/your-username/signal.git
cd signal
```

### 2. Configure secrets
Copy the example properties file and fill in your values:
```bash
cp local.properties.example local.properties
```

Open `local.properties` and set:
```properties
sdk.dir=/path/to/your/Android/sdk
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

> **Never commit `local.properties`.** It is already listed in `.gitignore`.

### 3. Build & run
```bash
# Debug APK
./gradlew assembleDebug

# Install directly to a connected device
./gradlew installDebug
```

Or open the project in Android Studio and press **Run**.

### 4. Grant permissions (on first launch)
SIGNAL's onboarding walks you through three required permissions:

| Permission | Why |
|-----------|-----|
| **Notification Listener** | To intercept and classify incoming notifications |
| **Display Over Other Apps** | To show the enforcement overlay above any app |
| **Calendar** (optional) | To auto-create meeting events |

---

## AI Classification — How It Works

Every notification goes through this pipeline:

```kotlin
// GroqClassifier.kt (simplified)
suspend fun classify(notification: NotificationData): ClassifiedTask {
    val response = groqApi.classify(
        GroqRequest(
            model    = "llama-3.3-70b-versatile",
            messages = listOf(
                GroqMessage("system", SYSTEM_PROMPT),
                GroqMessage("user",   buildUserPrompt(notification))
            ),
            temperature = 0.1,   // low temp = deterministic JSON
            maxTokens   = 300
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

Groq's **llama-3.3-70b** processes at ~500 tokens/second — the classification happens faster than the user can look at their phone.

---

## Data Model

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val sourceApp: String,           // "WhatsApp", "Gmail", …
    val packageName: String,
    val originalTitle: String,
    val originalBody: String,
    val capturedAt: Long,            // Unix epoch ms
    val extractedTask: String,       // AI-extracted plain-English task
    val importance: String,          // CRITICAL | HIGH | MEDIUM | LOW
    val category: String,            // DEADLINE | MEETING | PAYMENT | …
    val deadline: String?,           // Human-readable deadline
    val deadlineTimestamp: Long?,    // Epoch ms for comparison
    val suggestedActions: String,    // JSON array (stored as string)
    val status: String,              // PENDING | IN_PROGRESS | DONE | IGNORED
    val userDecision: String?,       // DO_NOW | SCHEDULE | DELEGATE | IGNORE
    val ignoreReason: String?,
    val scheduledFor: Long?,
    val decidedAt: Long?,
    val completedAt: Long?,
    val requiresEnforcement: Boolean,
    val isOverdue: Boolean,
    val rescheduleCount: Int = 0
)
```

---

## Design System

The UI follows a custom **Indigo-based design system** with full light and dark theme support. Colours are defined as semantic tokens in `Color.kt` and mapped to Material 3's full colour scheme in `Theme.kt`.

| Token | Dark | Light | Use |
|-------|------|-------|-----|
| `primary` | `#818CF8` | `#4F46E5` | Accent, buttons, links |
| `background` | `#111520` | `#F8F9FC` | Screen backgrounds |
| `surface` | `#181C28` | `#FFFFFF` | Cards, sheets |
| `outlineVariant` | `#1E2230` | `#E8ECF5` | Card borders |
| `Rose500` | `#EF4444` | `#EF4444` | Critical / error |
| `Amber500` | `#F59E0B` | `#F59E0B` | High / warning |
| `Blue500` | `#3B82F6` | `#3B82F6` | Medium / info |
| `Emerald500` | `#10B981` | `#10B981` | Low / success |

The theme preference is persisted in **DataStore** and toggled from the Settings screen — no app restart required.

---

## Permissions

| Permission | Required | Purpose |
|-----------|----------|---------|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | ✅ Yes | Intercept notifications |
| `SYSTEM_ALERT_WINDOW` | ✅ Yes | Show enforcement overlay |
| `POST_NOTIFICATIONS` | ✅ Yes | Reminder push notifications |
| `INTERNET` | ✅ Yes | Groq API calls |
| `SCHEDULE_EXACT_ALARM` | ✅ Yes | Precise WorkManager reminders |
| `USE_EXACT_ALARM` | ✅ Yes | Exact-time alarm trigger |
| `READ_CALENDAR` / `WRITE_CALENDAR` | ⚡ Optional | Meeting event creation |
| `RECEIVE_BOOT_COMPLETED` | ⚡ Optional | Restart workers after reboot |

---

## Known Limitations

- **Groq API key is required.** The app has no offline AI fallback beyond a basic keyword classifier.
- **Android battery optimisation** on some OEM devices (Xiaomi, OPPO, etc.) may kill the `NotificationListenerService`. Users may need to whitelist SIGNAL from battery optimisation.
- **Overlay permission** must be granted manually via system settings — Android does not allow runtime permission grants for this.
- **Calendar integration** requires the device to have a Google account and Google Calendar installed.
- The enforcement overlay was designed for MIUI, One UI, and stock Android — behaviour on heavily modified launchers may vary.

---

## Roadmap

- [ ] iCloud / Outlook Calendar integration
- [ ] Wear OS companion — quick decisions from the wrist
- [ ] Widget — live pending-task count on the home screen
- [ ] Per-app enforcement rules (e.g., always enforce Gmail, never enforce games)
- [ ] Export weekly report as PDF
- [ ] On-device LLM option (Gemini Nano) for offline classification
- [ ] Team mode — delegate tasks directly to contacts within SIGNAL

---

## Team

**Team BBBY** · MS Ramaiah Institute of Technology

Built in 24 hours as a hackathon submission. The entire stack — AI pipeline, enforcement overlay, Room DB, background workers, and Compose UI — was designed and shipped within a single day.

---

## License

```
MIT License — Copyright (c) 2024 Team BBBY

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.
```

---

<div align="center">

Made with focus, caffeine, and a hard deadline — exactly the kind of thing SIGNAL was built to enforce.

</div>
