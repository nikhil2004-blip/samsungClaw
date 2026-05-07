package com.example.signal.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // ── Permissions ───────────────────────────────────────────────────────
        SettingsSectionHeader("Permissions")

        SettingsItem(
            icon = "🔔",
            title = "Notification Access",
            subtitle = "Required for SIGNAL to intercept notifications",
            action = {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }) { Text("Grant", color = Color(0xFF6C63FF)) }
            }
        )

        SettingsItem(
            icon = "🪟",
            title = "Display Over Apps",
            subtitle = "Required to show enforcement overlay",
            action = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                    )
                }) { Text("Grant", color = Color(0xFF6C63FF)) }
            }
        )

        // ── Enforcement ───────────────────────────────────────────────────────
        SettingsSectionHeader("Enforcement Mode")

        val enforcementOptions = listOf("Critical Only", "High & Critical", "All")
        var selectedMode by remember { mutableStateOf("High & Critical") }

        Surface(
            color = Color(0xFF1A1A2E),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(8.dp)) {
                enforcementOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = option }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedMode,
                            onClick  = { selectedMode = option },
                            colors   = RadioButtonDefaults.colors(selectedColor = Color(0xFF6C63FF))
                        )
                        Text(option, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        // ── Quiet Hours ───────────────────────────────────────────────────────
        SettingsSectionHeader("Quiet Hours")

        var quietHoursEnabled by remember { mutableStateOf(false) }
        SettingsItem(
            icon = "🌙",
            title = "Quiet Hours",
            subtitle = "Batch enforcements during sleep hours (11 PM – 7 AM)",
            action = {
                Switch(
                    checked = quietHoursEnabled,
                    onCheckedChange = { quietHoursEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6C63FF))
                )
            }
        )

        // ── Calendar Integration ──────────────────────────────────────────────
        SettingsSectionHeader("Calendar Integration")

        var calendarSyncEnabled by remember { mutableStateOf(true) }
        SettingsItem(
            icon = "📅",
            title = "Sync Meetings to Calendar",
            subtitle = "Automatically add meeting notifications to your device calendar",
            action = {
                Switch(
                    checked = calendarSyncEnabled,
                    onCheckedChange = { calendarSyncEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6C63FF))
                )
            }
        )

        SettingsItem(
            icon = "🔔",
            title = "Meeting Reminder",
            subtitle = "15-minute reminder added automatically to calendar events",
            action = {
                Text("15 min", color = Color(0xFF6C63FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        )

        // ── Data ──────────────────────────────────────────────────────────────
        SettingsSectionHeader("Data")

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showClearDialog = true },
            color = Color(0xFF3E0D0D),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFFF4444).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF4444))
                Column {
                    Text("Clear All Data", color = Color(0xFFFF4444), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Delete all tasks and history", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
        }

        // ── About ─────────────────────────────────────────────────────────────
        SettingsSectionHeader("About")
        Surface(color = Color(0xFF1A1A2E), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SIGNAL v1.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("AI-Powered Notification Intelligence", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Text("Team BBBY — MS Ramaiah Institute of Technology", color = Color(0xFF6C63FF), fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = { Text("Clear All Data?", color = Color.White) },
            text = { Text("This will permanently delete all tasks and history. This cannot be undone.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAll(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel", color = Color.White) }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(title, color = Color(0xFF6C63FF), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

@Composable
private fun SettingsItem(icon: String, title: String, subtitle: String, action: @Composable () -> Unit) {
    Surface(color = Color(0xFF1A1A2E), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            }
            action()
        }
    }
}
