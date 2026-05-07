# SIGNAL App — Manual Testing & Permission Steps

Some features in SIGNAL involve system-level permissions and overlays that cannot be fully automated in standard instrumentation tests without root or specialized setup.

## 1. System Permissions (One-Time Setup)

To run UI tests or use the app, manually grant these permissions:

- **Notification Access**: 
  - Go to `Settings > Apps > Special app access > Notification access`.
  - Toggle **ON** for `SIGNAL`.
  - This allows the `NotificationInterceptorService` to capture tasks.

- **Display Over Other Apps (Overlay)**:
  - Go to `Settings > Apps > Special app access > Appear on top`.
  - Toggle **ON** for `SIGNAL`.
  - This allows the `EnforcementOverlayActivity` to block interactions until a decision is made.

## 2. API Key Configuration

Ensure your `local.properties` file contains the Groq API Key:
```properties
GROQ_API_KEY=your_actual_key_here
```
The tests use `MockWebServer`, but the app requires this for real-world classification.

## 3. Running Instrumented Tests

- Open Android Studio.
- Ensure an Emulator (API 30+) or Physical Device is connected.
- Right-click on `app/src/androidTest/java` and select **Run 'All Tests'**.
- **Important**: If the screen is locked, the `EnforcementOverlayTest` might fail. Keep the device awake.

## 4. Manual UI Verification Flow

1. **Onboarding**: Ensure the splash screen transitions to the main board after the first launch.
2. **Notification Capture**: Send a message to the device (e.g., via WhatsApp). Verify a new task appears in the `Inbox`.
3. **Enforcement**: If a task is "Critical", verify the full-screen overlay appears and prevents navigation until "Do Now", "Schedule", or "Ignore" is selected.
4. **Rescheduling**: Schedule a task for 1 minute later. Verify a reminder notification appears.
