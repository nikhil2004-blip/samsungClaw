package com.example.signal.ui.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.signal.ui.theme.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context    = LocalContext.current
    val cs         = MaterialTheme.colorScheme
    val isDark     by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = cs.onBackground
        )
        Text(
            "Manage permissions and preferences",
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // ── Appearance ────────────────────────────────────────────────────────
        SectionHeader("Appearance")

        SettingsCard {
            SettingsRow(
                icon     = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                iconTint = cs.primary,
                title    = if (isDark) "Dark Mode" else "Light Mode",
                subtitle = "Switch between light and dark appearance"
            ) {
                Switch(
                    checked  = isDark,
                    onCheckedChange = { viewModel.toggleDarkTheme(it) },
                    colors   = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = cs.primary,
                        uncheckedThumbColor = cs.onSurfaceVariant,
                        uncheckedTrackColor = cs.surfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Permissions ───────────────────────────────────────────────────────
        SectionHeader("Permissions")

        SettingsCard {
            SettingsRow(
                icon     = Icons.Outlined.Notifications,
                iconTint = Blue500,
                title    = "Notification Access",
                subtitle = "Required for SIGNAL to intercept notifications"
            ) {
                TextButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    colors  = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                ) {
                    Text("Grant", style = MaterialTheme.typography.labelLarge)
                }
            }

            HorizontalDivider(color = cs.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                icon     = Icons.Outlined.Layers,
                iconTint = cs.primary,
                title    = "Display Over Apps",
                subtitle = "Required to show the enforcement overlay"
            ) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                ) {
                    Text("Grant", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Enforcement ───────────────────────────────────────────────────────
        SectionHeader("Enforcement mode")

        val enforcementOptions = listOf("Critical Only", "High & Critical", "All")
        var selectedMode by remember { mutableStateOf("High & Critical") }

        SettingsCard {
            enforcementOptions.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = option }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = option == selectedMode,
                        onClick  = { selectedMode = option },
                        colors   = RadioButtonDefaults.colors(
                            selectedColor   = cs.primary,
                            unselectedColor = cs.onSurfaceVariant
                        )
                    )
                    Text(
                        option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (option == selectedMode) cs.onSurface else cs.onSurfaceVariant
                    )
                }
                if (index < enforcementOptions.lastIndex) {
                    HorizontalDivider(color = cs.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Quiet Hours ───────────────────────────────────────────────────────
        SectionHeader("Quiet hours")

        SettingsCard {
            var quietHoursEnabled by remember { mutableStateOf(false) }
            SettingsRow(
                icon     = Icons.Outlined.Bedtime,
                iconTint = Indigo400,
                title    = "Quiet Hours",
                subtitle = "Pause enforcement from 11 PM to 7 AM"
            ) {
                Switch(
                    checked  = quietHoursEnabled,
                    onCheckedChange = { quietHoursEnabled = it },
                    colors   = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = cs.primary,
                        uncheckedThumbColor = cs.onSurfaceVariant,
                        uncheckedTrackColor = cs.surfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Calendar ──────────────────────────────────────────────────────────
        SectionHeader("Calendar integration")

        SettingsCard {
            var calendarSync by remember { mutableStateOf(true) }
            SettingsRow(
                icon     = Icons.Outlined.CalendarMonth,
                iconTint = Emerald500,
                title    = "Sync to Calendar",
                subtitle = "Auto-create events for detected meetings"
            ) {
                Switch(
                    checked  = calendarSync,
                    onCheckedChange = { calendarSync = it },
                    colors   = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = cs.primary,
                        uncheckedThumbColor = cs.onSurfaceVariant,
                        uncheckedTrackColor = cs.surfaceVariant
                    )
                )
            }

            HorizontalDivider(color = cs.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                icon     = Icons.Outlined.Notifications,
                iconTint = Amber500,
                title    = "Meeting Reminders",
                subtitle = "15-minute reminder for calendar events"
            ) {
                Surface(
                    color  = cs.primaryContainer,
                    shape  = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "15 min",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = cs.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Data ──────────────────────────────────────────────────────────────
        SectionHeader("Data")

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showClearDialog = true },
            color  = Rose500.copy(alpha = 0.06f),
            shape  = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Rose500.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Rose500.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null,
                        tint = Rose500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Clear All Data",
                        style = MaterialTheme.typography.titleSmall,
                        color = Rose500,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Permanently delete all tasks and history",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Rose500.copy(alpha = 0.60f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── About ─────────────────────────────────────────────────────────────
        SectionHeader("About")

        SettingsCard {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "SIGNAL  v1.0",
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "AI-Powered Notification Intelligence",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                    Text(
                        "Team BBBY · MS Ramaiah Institute of Technology",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // ── Clear data confirmation dialog ─────────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = Rose500,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Clear all data?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "This will permanently delete all tasks and history. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAll(); showClearDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title.uppercase(),
        style      = MaterialTheme.typography.labelMedium,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = cs.surface,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    action: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        action()
    }
}
