package com.example.signal.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.example.signal.ui.theme.*

data class OnboardingStep(
    val icon: ImageVector,
    val iconTint: androidx.compose.ui.graphics.Color,
    val title: String,
    val subtitle: String,
    val buttonLabel: String,
    val isSkippable: Boolean = false
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val cs      = MaterialTheme.colorScheme
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            icon        = Icons.Outlined.Notifications,
            iconTint    = Blue500,
            title       = "Grant Notification Access",
            subtitle    = "SIGNAL reads your notifications and uses AI to classify what actually matters.",
            buttonLabel = "Grant Access"
        ),
        OnboardingStep(
            icon        = Icons.Outlined.Layers,
            iconTint    = Indigo400,
            title       = "Allow Display Over Apps",
            subtitle    = "The enforcement overlay needs permission to appear above other apps.",
            buttonLabel = "Allow Overlay"
        ),
        OnboardingStep(
            icon        = Icons.Outlined.CalendarMonth,
            iconTint    = Emerald500,
            title       = "Connect Google Calendar",
            subtitle    = "Optionally auto-create calendar events for detected meetings.",
            buttonLabel = "Connect Calendar",
            isSkippable = true
        ),
        OnboardingStep(
            icon        = Icons.Outlined.AutoAwesome,
            iconTint    = cs.primary,
            title       = "You're all set!",
            subtitle    = "SIGNAL will now intercept important notifications and help you stay on top of what matters.",
            buttonLabel = "Start using SIGNAL"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── App name / brand ───────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SIGNAL",
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = cs.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "AI Notification Intelligence",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }

            // ── Step content ───────────────────────────────────────────────────
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInVertically { it / 3 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 3 } + fadeOut())
                },
                label = "step"
            ) { step ->
                val s = steps[step]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(s.iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = s.icon,
                            contentDescription = null,
                            tint = s.iconTint,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            s.title,
                            style     = MaterialTheme.typography.headlineSmall,
                            color     = cs.onBackground,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            s.subtitle,
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }
            }

            // ── Progress + actions ─────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width  = if (i == currentStep) 24.dp else 8.dp,
                                    height = 8.dp
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (i == currentStep) cs.primary
                                    else if (i < currentStep) cs.primary.copy(alpha = 0.40f)
                                    else cs.outlineVariant
                                )
                        )
                    }
                }

                val step = steps[currentStep]

                // Primary button
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    shape  = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        step.buttonLabel,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (step.isSkippable) {
                    TextButton(
                        onClick = {
                            if (currentStep < steps.lastIndex) currentStep++ else onComplete()
                        }
                    ) {
                        Text(
                            "Skip for now",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun hasRequiredPermissions(context: android.content.Context): Boolean {
    val notificationOk = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)
    val overlayOk = Settings.canDrawOverlays(context)
    return notificationOk && overlayOk
}
