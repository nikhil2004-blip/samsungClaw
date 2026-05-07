package com.example.signal.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat

data class OnboardingStep(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val buttonLabel: String,
    val isSkippable: Boolean = false
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStep("🔔", "Grant Notification Access",
            "SIGNAL needs to read your notifications to classify them with AI.",
            "Grant Access"),
        OnboardingStep("🪟", "Allow Display Over Apps",
            "The enforcement overlay needs permission to appear above other apps.",
            "Allow Overlay"),
        OnboardingStep("🗓️", "Connect Google Calendar",
            "Optionally auto-create calendar events for detected meetings.",
            "Connect Calendar", isSkippable = true),
        OnboardingStep("🤖", "You're ready!",
            "SIGNAL will now intercept important notifications and help you act on them.",
            "Start Using SIGNAL", isSkippable = false)
    )

    val gradient = Brush.verticalGradient(colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                steps.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == currentStep) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i == currentStep) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.2f))
                    )
                }
            }

            val step = steps[currentStep]

            Text(step.emoji, fontSize = 72.sp)
            Spacer(Modifier.height(24.dp))
            Text(step.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(step.subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    when (currentStep) {
                        0 -> {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            currentStep++
                        }
                        1 -> {
                            context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                        )
                            currentStep++
                        }
                        2 -> currentStep++
                        3 -> if (hasRequiredPermissions(context)) onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(step.buttonLabel, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            if (step.isSkippable) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    if (currentStep < steps.lastIndex) currentStep++ else onComplete()
                }) { Text("Skip for now", color = Color.White.copy(alpha = 0.5f)) }
            }
        }
    }
}

private fun hasRequiredPermissions(context: android.content.Context): Boolean {
    val isNotificationListenerEnabled = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)

    val canDrawOverlays = Settings.canDrawOverlays(context)

    return isNotificationListenerEnabled && canDrawOverlays
}
