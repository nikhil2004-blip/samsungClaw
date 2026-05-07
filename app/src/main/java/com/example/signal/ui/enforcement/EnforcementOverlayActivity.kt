package com.example.signal.ui.enforcement

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.signal.data.local.TaskEntity
import com.example.signal.ui.theme.SignalTheme
import com.example.signal.worker.WorkManagerHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class EnforcementOverlayActivity : ComponentActivity() {

    private val viewModel: EnforcementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock-screen / always on top
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: run { finish(); return }
        viewModel.loadTask(taskId)

        // Block back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@EnforcementOverlayActivity,
                    "Please make a decision to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        setContent {
            SignalTheme {
                val task by viewModel.task.collectAsStateWithLifecycle()
                val decisionComplete by viewModel.decisionComplete.collectAsStateWithLifecycle()

                LaunchedEffect(decisionComplete) {
                    if (decisionComplete) finish()
                }

                task?.let { t ->
                    EnforcementOverlayScreen(
                        task = t,
                        suggestedActions = viewModel.getSuggestedActions(t),
                        onDoNow = {
                            viewModel.doNow(t.id)
                            // Open source app
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(t.packageName)
                                if (launchIntent != null) startActivity(launchIntent)
                            } catch (e: Exception) { /* ignore */ }
                        },
                        onSchedule = { scheduledMs ->
                            viewModel.schedule(t.id, scheduledMs)
                            WorkManagerHelper.scheduleTaskReminder(this, t.id, scheduledMs)
                        },
                        onDelegate = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Task: ${t.extractedTask}\nDeadline: ${t.deadline ?: "N/A"}")
                            }
                            startActivity(Intent.createChooser(shareIntent, "Delegate task via"))
                            viewModel.delegate(t.id)
                        },
                        onIgnore = { reason -> viewModel.ignore(t.id, reason) }
                    )
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6C63FF))
                }
            }
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}

// ─── Composable UI ────────────────────────────────────────────────────────────

enum class OverlaySubView { NONE, SCHEDULE, IGNORE }

@Composable
fun EnforcementOverlayScreen(
    task: TaskEntity,
    suggestedActions: List<String>,
    onDoNow: () -> Unit,
    onSchedule: (Long) -> Unit,
    onDelegate: () -> Unit,
    onIgnore: (String) -> Unit
) {
    var subView by remember { mutableStateOf(OverlaySubView.NONE) }
    var ignoreReason by remember { mutableStateOf("") }

    val importanceColor = when (task.importance) {
        "CRITICAL" -> Color(0xFFFF4444)
        "HIGH"     -> Color(0xFFFF8C00)
        "MEDIUM"   -> Color(0xFF2196F3)
        else       -> Color(0xFF4CAF50)
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.sourceApp,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = importanceColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, importanceColor)
                ) {
                    Text(
                        text = task.importance,
                        color = importanceColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // ── Task ──────────────────────────────────────────────────────────
            Text(
                text = "Action Required",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
            Text(
                text = task.extractedTask,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )

            // ── Deadline countdown ────────────────────────────────────────────
            task.deadline?.let { dl ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF8C00), modifier = Modifier.size(18.dp))
                    Text(text = dl, color = Color(0xFFFF8C00), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    task.deadlineTimestamp?.let { ts ->
                        val remaining = ts - System.currentTimeMillis()
                        if (remaining > 0) {
                            val hours = remaining / 3_600_000
                            val mins  = (remaining % 3_600_000) / 60_000
                            Text(text = "(${hours}h ${mins}m left)", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── AI Suggested Actions ──────────────────────────────────────────
            if (suggestedActions.isNotEmpty()) {
                Text(text = "AI Suggested Actions", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestedActions.take(3).forEach { action ->
                        Surface(
                            color = Color(0xFF6C63FF).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF6C63FF).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = action,
                                color = Color(0xFF9F97FF),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // ── Sub-views or main buttons ──────────────────────────────────────
            AnimatedContent(targetState = subView, label = "subview") { sv ->
                when (sv) {
                    OverlaySubView.SCHEDULE -> ScheduleSubView(
                        onConfirm = { onSchedule(it); subView = OverlaySubView.NONE },
                        onCancel  = { subView = OverlaySubView.NONE }
                    )
                    OverlaySubView.IGNORE -> IgnoreSubView(
                        reason = ignoreReason,
                        onReasonChange = { ignoreReason = it },
                        onConfirm = { onIgnore(ignoreReason) },
                        onCancel  = { subView = OverlaySubView.NONE }
                    )
                    OverlaySubView.NONE -> DecisionButtons(
                        isEscalated = task.rescheduleCount >= 2,
                        onDoNow    = onDoNow,
                        onSchedule = { subView = OverlaySubView.SCHEDULE },
                        onDelegate = onDelegate,
                        onIgnore   = { subView = OverlaySubView.IGNORE }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DecisionButtons(
    isEscalated: Boolean,
    onDoNow: () -> Unit,
    onSchedule: () -> Unit,
    onDelegate: () -> Unit,
    onIgnore: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isEscalated) "⚠️ You've deferred this twice. Time to act." else "What do you want to do?",
            color = if (isEscalated) Color(0xFFFF4444) else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DecisionButton(
                modifier = Modifier.weight(1f),
                emoji = "✅",
                label = "DO NOW",
                containerColor = Color(0xFF1B5E20),
                borderColor = Color(0xFF4CAF50),
                onClick = onDoNow
            )
            DecisionButton(
                modifier = Modifier.weight(1f),
                emoji = "📅",
                label = "SCHEDULE",
                containerColor = Color(0xFF0D3B6E),
                borderColor = Color(0xFF2196F3),
                onClick = onSchedule,
                enabled = !isEscalated
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DecisionButton(
                modifier = Modifier.weight(1f),
                emoji = "👤",
                label = "DELEGATE",
                containerColor = Color(0xFF4A148C),
                borderColor = Color(0xFFAB47BC),
                onClick = onDelegate
            )
            DecisionButton(
                modifier = Modifier.weight(1f),
                emoji = "❌",
                label = "IGNORE",
                containerColor = Color(0xFF3E0D0D),
                borderColor = Color(0xFFFF4444),
                onClick = onIgnore
            )
        }
    }
}

@Composable
private fun DecisionButton(
    modifier: Modifier,
    emoji: String,
    label: String,
    containerColor: Color,
    borderColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) borderColor else Color.Gray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Text(
                text = label,
                color = if (enabled) Color.White else Color.Gray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScheduleSubView(onConfirm: (Long) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var selectedMs by remember { mutableStateOf(System.currentTimeMillis() + 3_600_000L) }
    val label = remember(selectedMs) {
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(selectedMs))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("📅 Schedule Reminder", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Surface(
            color = Color(0xFF1A1A2E),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF2196F3))
        ) {
            Text(
                text = label,
                color = Color(0xFF64B5F6),
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        Button(
            onClick = {
                val cal = Calendar.getInstance()
                DatePickerDialog(context, { _, y, m, d ->
                    TimePickerDialog(context, { _, h, min ->
                        cal.set(y, m, d, h, min, 0)
                        selectedMs = cal.timeInMillis
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Pick Date & Time") }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel", color = Color.White) }
            Button(
                onClick = { onConfirm(selectedMs) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.weight(1f)
            ) { Text("Confirm") }
        }
    }
}

@Composable
private fun IgnoreSubView(
    reason: String,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("❌ Ignore Task", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Why are you ignoring this? (required — min 10 characters)", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)

        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            placeholder = { Text("Enter your reason...", color = Color.White.copy(alpha = 0.3f)) },
            modifier = Modifier.fillMaxWidth().testTag("IgnoreReasonField"),
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF4444),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Text("${reason.length}/10 minimum", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel", color = Color.White) }
            Button(
                onClick = onConfirm,
                enabled = reason.length >= 10,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                modifier = Modifier.weight(1f)
            ) { Text("Confirm Ignore") }
        }
    }
}
