<!-- APK install and build instructions for evaluators -->
# Demo APK — Install & Build Instructions

This document explains two safe ways to install the demo build of SIGNAL and why Play Protect may block the direct APK tap.

## Option A — Open the APK directly (Play Protect may block)

- Location in repo: apk/app-release.apk (if present in the repository release assets).
- On many devices the system Package Installer calls Google Play Protect and will either show a warning or hard-block the install. This is due to the app requesting sensitive permissions (notification listener, overlay, exact alarm) and because the APK is not distributed via the Play Store, so it lacks Play-protect reputation.
- If you choose this route, you may need to temporarily disable Play Protect on the device (not recommended on devices with personal data):

  1. Open the Play Store app on the device.
  2. Tap your profile avatar → Play Protect → Settings (gear icon).
  3. Toggle off Scan apps with Play Protect or similar.

  NOTE: Settings location and wording vary by Android / Play Store version. Disabling Play Protect reduces protection — prefer Option B below.

## Option B — Recommended: Build from source and install via ADB (bypasses Play Protect)

Prerequisites:

- Git and repository cloned locally
- Java 11 installed
- Android SDK + command-line tools (adb)
- USB Debugging enabled on device

Steps (Windows):

```powershell
git clone <your-repo-url>
cd your-repo-folder
\gradlew.bat assembleRelease
# or use ./gradlew assembleRelease on macOS/Linux

# Uninstall any previous app (replace package name if changed)
adb uninstall com.example.signal

# Install the generated APK
adb install app\build\outputs\apk\release\app-release.apk
```

Why this works: `adb install` sideloads the APK via the Android Debug Bridge. Play Protect checks that run during user-driven installs are not applied to ADB installs, so the package is installed directly. This is the safest evaluator workflow for local testing.

## Setting the GROQ API key (local build)

The project reads Groq keys from local.properties. Add your key locally before building:

```
GROQ_API_KEY=sk_live_your_key_here
GROQ_API_KEYS=
```

Place local.properties in the project root (do not commit it). The build will inject the value into `BuildConfig.GROQ_API_KEY`.

## Troubleshooting

- If adb install fails with INSTALL_FAILED_VERSION_DOWNGRADE or INSTALL_FAILED_ALREADY_EXISTS, run:

```
adb uninstall com.example.signal
adb install -r app\build\outputs\apk\release\app-release.apk
```

- If the APK is missing, build with assembleRelease or generate a signed APK from Android Studio: Build → Generate Signed Bundle / APK → APK → follow the wizard.

## Rationale — why Play Protect blocks the APK

- Sensitive permissions: Apps using BIND_NOTIFICATION_LISTENER_SERVICE, SYSTEM_ALERT_WINDOW (overlay), and exact alarms are flagged as high-risk because they can intercept or obstruct user interactions.
- Reputation: Apps distributed outside the Play Store lack Google’s reputation metadata. Play Protect relies on telemetry and Play Store signals to decide whether to allow a non-Play install.

Because Play Protect behavior is controlled by Google’s servers and the device Play Store app, there is no manifest flag or signing trick that will reliably turn a hard-block into a simple warning. For hackathon evaluators, building the APK locally and installing via adb (Option B) is the recommended path.

---

If you want, I can also add a short note to the top-level README linking to this page (so the existing "Download Demo APK" link opens this instruction file). Would you like me to update the README now?
