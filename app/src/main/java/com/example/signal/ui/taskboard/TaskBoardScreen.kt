package com.example.signal.ui.taskboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.signal.data.local.TaskEntity
import com.example.signal.data.model.TaskStatus
import com.example.signal.data.model.UserDecision
import com.example.signal.ui.theme.SignalTheme
import java.text.SimpleDateFormat
import java.util.*

private val FILTERS = listOf("All", "Deadlines", "Meetings", "Payments", "Messages")

@Composable
fun TaskBoardScreen(viewModel: TaskViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D1A))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search tasks...", color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedContainerColor = Color(0xFF1A1A2E),
                        unfocusedContainerColor = Color(0xFF1A1A2E)
                    )
                )
                Spacer(Modifier.height(8.dp))
                // Filter chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FILTERS.forEach { filter ->
                        val selected = filter == state.selectedFilter
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6C63FF),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1A1A2E),
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF6C63FF)
            ) { Icon(Icons.Default.Add, contentDescription = "Add task", tint = Color.White) }
        }
    ) { padding ->
        val allEmpty = state.overdue.isEmpty() && state.pending.isEmpty() &&
                       state.inProgress.isEmpty() && state.done.isEmpty()

        if (allEmpty) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Overdue section
                if (state.overdue.isNotEmpty()) {
                    item { SectionHeader("🔥 CRITICAL & OVERDUE", Color(0xFFFF4444)) }
                    items(state.overdue, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isExpanded = expandedTaskId == task.id,
                            onTap = { expandedTaskId = if (expandedTaskId == task.id) null else task.id },
                            onMarkDone = { viewModel.markDone(task.id) },
                            isOverdue = true
                        )
                    }
                }
                // In-progress
                if (state.inProgress.isNotEmpty()) {
                    item { SectionHeader("⚡ IN PROGRESS", Color(0xFF2196F3)) }
                    items(state.inProgress, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isExpanded = expandedTaskId == task.id,
                            onTap = { expandedTaskId = if (expandedTaskId == task.id) null else task.id },
                            onMarkDone = { viewModel.markDone(task.id) }
                        )
                    }
                }
                // Pending
                if (state.pending.isNotEmpty()) {
                    item { SectionHeader("📋 PENDING", Color(0xFFFFB300)) }
                    items(state.pending, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isExpanded = expandedTaskId == task.id,
                            onTap = { expandedTaskId = if (expandedTaskId == task.id) null else task.id },
                            onMarkDone = { viewModel.markDone(task.id) }
                        )
                    }
                }
                // Done
                if (state.done.isNotEmpty()) {
                    item { SectionHeader("✅ COMPLETED TODAY", Color(0xFF4CAF50)) }
                    items(state.done, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            isExpanded = expandedTaskId == task.id,
                            onTap = { expandedTaskId = if (expandedTaskId == task.id) null else task.id },
                            onMarkDone = { viewModel.markDone(task.id) },
                            isDone = true
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { task ->
                viewModel.addManualTask(task)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onMarkDone: () -> Unit,
    isOverdue: Boolean = false,
    isDone: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )

    val importanceColor = when (task.importance) {
        "CRITICAL" -> Color(0xFFFF4444)
        "HIGH"     -> Color(0xFFFF8C00)
        "MEDIUM"   -> Color(0xFF2196F3)
        else       -> Color(0xFF4CAF50)
    }

    val categoryEmoji = when (task.category) {
        "DEADLINE"    -> "⏰"
        "MEETING"     -> "🤝"
        "PAYMENT"     -> "💳"
        "MESSAGE"     -> "💬"
        "REMINDER"    -> "🔔"
        "PROMOTIONAL" -> "🏷️"
        else          -> "📌"
    }

    val timeRemainingColor = task.deadlineTimestamp?.let { ts ->
        val remaining = ts - System.currentTimeMillis()
        when {
            remaining < 0                -> Color(0xFFFF4444)
            remaining < 2 * 3_600_000L   -> Color(0xFFFF4444)
            remaining < 24 * 3_600_000L  -> Color(0xFFFF8C00)
            else                         -> Color(0xFF4CAF50)
        }
    }

    val borderColor = if (isOverdue) importanceColor.copy(alpha = pulseAlpha) else importanceColor.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .graphicsLayer { alpha = if (isDone) 0.6f else 1f },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category dot
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(importanceColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text(categoryEmoji, fontSize = 16.sp) }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.extractedTask,
                        color = if (isDone) Color.White.copy(alpha = 0.5f) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2
                    )
                    Text(
                        text = task.sourceApp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }

                // Importance badge
                Surface(
                    color = importanceColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = task.importance.take(3),
                        color = importanceColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Deadline + time remaining
            task.deadline?.let { dl ->
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null,
                        tint = timeRemainingColor ?: Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp))
                    Text(text = dl, color = timeRemainingColor ?: Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // Expanded details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(8.dp))
                    Text("Original notification:", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    Text(task.originalBody, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Captured: ${formatTime(task.capturedAt)}", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                    task.userDecision?.let { d ->
                        Text("Decision: $d", color = Color(0xFF6C63FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (!isDone) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onMarkDone,
                            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Mark as Done ✅", color = Color(0xFF4CAF50)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎯", fontSize = 64.sp)
            Text("You're all caught up!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("No pending tasks. Keep it up!", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (TaskEntity) -> Unit) {
    var title by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = { Text("Add Task Manually", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task description", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (optional)", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            TaskEntity(
                                id = UUID.randomUUID().toString(),
                                sourceApp = "Manual",
                                packageName = "",
                                originalTitle = title,
                                originalBody = "",
                                capturedAt = System.currentTimeMillis(),
                                extractedTask = title,
                                importance = "MEDIUM",
                                category = "OTHER",
                                deadline = deadline.ifBlank { null },
                                deadlineTimestamp = null,
                                suggestedActions = "[]",
                                status = "PENDING",
                                userDecision = null,
                                ignoreReason = null,
                                scheduledFor = null,
                                decidedAt = null,
                                completedAt = null,
                                requiresEnforcement = false,
                                isOverdue = false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.7f)) }
        }
    )
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(ms))
