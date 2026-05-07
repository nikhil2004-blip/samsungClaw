package com.example.signal.ui.taskboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.signal.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Priority helpers ───────────────────────────────────────────────────────────

private val PRIORITY_ORDER = listOf("CRITICAL", "HIGH", "MEDIUM", "LOW")

private fun importanceColor(importance: String) = when (importance) {
    "CRITICAL" -> Color(0xFFFF4444)
    "HIGH"     -> Color(0xFFFF8C00)
    "MEDIUM"   -> Color(0xFF2196F3)
    else       -> Color(0xFF4CAF50)
}

private fun importanceEmoji(importance: String) = when (importance) {
    "CRITICAL" -> "🔴"
    "HIGH"     -> "🟠"
    "MEDIUM"   -> "🔵"
    else       -> "🟢"
}

private fun categoryEmoji(category: String) = when (category) {
    "DEADLINE"    -> "⏰"
    "MEETING"     -> "🤝"
    "PAYMENT"     -> "💳"
    "MESSAGE"     -> "💬"
    "REMINDER"    -> "🔔"
    "PROMOTIONAL" -> "🏷️"
    else          -> "📌"
}

// ── Screen ─────────────────────────────────────────────────────────────────────

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Inbox", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.allMessages.size} active",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                    // Priority summary badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val critical = state.allMessages.count { it.importance == "CRITICAL" }
                        val high     = state.allMessages.count { it.importance == "HIGH" }
                        if (critical > 0) PriorityBubble(critical, Color(0xFFFF4444), "🔴")
                        if (high > 0)     PriorityBubble(high,     Color(0xFFFF8C00), "🟠")
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Search bar
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search...", color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.5f))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor    = Color.White,
                        unfocusedTextColor  = Color.White,
                        focusedBorderColor  = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor   = Color(0xFF1A1A2E),
                        unfocusedContainerColor = Color(0xFF1A1A2E)
                    )
                )

                Spacer(Modifier.height(8.dp))

                // Filter chips — horizontal scroll
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(TaskFilter.values()) { filter ->
                        val selected = filter == state.selectedFilter
                        val count = when (filter) {
                            TaskFilter.ALL           -> state.allMessages.size
                            TaskFilter.HIGH_PRIORITY -> state.highPriority.size
                            TaskFilter.DEADLINES     -> state.deadlines.size
                            TaskFilter.MEETINGS      -> state.meetings.size
                            TaskFilter.MESSAGES      -> state.messages.size
                            TaskFilter.PAYMENTS      -> state.payments.size
                            TaskFilter.REMINDERS     -> state.reminders.size
                            TaskFilter.MISSED        -> state.missed.size
                            TaskFilter.ADVERTS       -> state.adverts.size
                            TaskFilter.OTHER         -> state.other.size
                        }
                        FilterChip(
                            selected = selected,
                            onClick  = { viewModel.setFilter(filter) },
                            label    = {
                                Text(
                                    "${filter.emoji} ${filter.label}${if (count > 0) " ($count)" else ""}",
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (filter) {
                                    TaskFilter.HIGH_PRIORITY -> Color(0xFFFF4444)
                                    else                     -> Color(0xFF6C63FF)
                                },
                                selectedLabelColor   = Color.White,
                                containerColor       = Color(0xFF1A1A2E),
                                labelColor           = Color.White.copy(alpha = 0.7f)
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

        val displayList = state.currentList()

        if (state.selectedFilter == TaskFilter.MISSED) {
            // Missed tab — dedicated view with "I did this" restore button
            MissedView(
                tasks      = state.missed,
                padding    = padding,
                onDidThis  = viewModel::markMissedAsDone
            )
        } else if (displayList.isEmpty() && state.done.isEmpty() && state.selectedFilter != TaskFilter.ALL) {
            EmptyPartition(
                filter = state.selectedFilter,
                modifier = Modifier.padding(padding)
            )
        } else if (state.selectedFilter == TaskFilter.ALL) {
            AllMessagesView(
                state       = state,
                padding     = padding,
                expandedId  = expandedTaskId,
                onExpand    = { id -> expandedTaskId = if (expandedTaskId == id) null else id },
                onMarkDone  = viewModel::markDone
            )
        } else {
            PartitionView(
                tasks       = displayList,
                filter      = state.selectedFilter,
                padding     = padding,
                expandedId  = expandedTaskId,
                onExpand    = { id -> expandedTaskId = if (expandedTaskId == id) null else id },
                onMarkDone  = viewModel::markDone
            )
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd     = { task ->
                viewModel.addManualTask(task)
                showAddDialog = false
            }
        )
    }
}

// ── All Messages tab ──────────────────────────────────────────────────────────

@Composable
private fun AllMessagesView(
    state: TaskBoardUiState,
    padding: PaddingValues,
    expandedId: String?,
    onExpand: (String) -> Unit,
    onMarkDone: (String) -> Unit
) {
    val allEmpty = state.overdue.isEmpty() && state.pending.isEmpty() &&
            state.inProgress.isEmpty() && state.done.isEmpty()

    if (allEmpty) {
        EmptyState(modifier = Modifier.padding(padding))
        return
    }

    val shownIds = mutableSetOf<String>()
    val overdueTasks = state.overdue
    shownIds.addAll(overdueTasks.map { it.id })

    val criticalHigh = state.highPriority.filter { !shownIds.contains(it.id) }
    shownIds.addAll(criticalHigh.map { it.id })

    val inProgressTasks = state.inProgress.filter { !shownIds.contains(it.id) }
    shownIds.addAll(inProgressTasks.map { it.id })

    val pendingTasks = state.pending.filter { !shownIds.contains(it.id) }
    shownIds.addAll(pendingTasks.map { it.id })

    val doneTasks = state.done.filter { !shownIds.contains(it.id) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top   = padding.calculateTopPadding() + 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (overdueTasks.isNotEmpty()) {
            val distinctList = overdueTasks.distinctBy { it.id }
            item { SectionHeader("🔥 OVERDUE", Color(0xFFFF4444), distinctList.size) }
            items(distinctList, key = { "overdue_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) }, isOverdue = true)
            }
        }
        if (criticalHigh.isNotEmpty()) {
            val distinctList = criticalHigh.distinctBy { it.id }
            item { SectionHeader("🔴 HIGH PRIORITY", Color(0xFFFF8C00), distinctList.size) }
            items(distinctList, key = { "high_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (inProgressTasks.isNotEmpty()) {
            val distinctList = inProgressTasks.distinctBy { it.id }
            item { SectionHeader("⚡ IN PROGRESS", Color(0xFF2196F3), distinctList.size) }
            items(distinctList, key = { "inprog_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (pendingTasks.isNotEmpty()) {
            val distinctList = pendingTasks.distinctBy { it.id }
            item { SectionHeader("📋 PENDING", Color(0xFFFFB300), distinctList.size) }
            items(distinctList, key = { "pending_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (doneTasks.isNotEmpty()) {
            val distinctList = doneTasks.distinctBy { it.id }
            item { SectionHeader("✅ COMPLETED TODAY", Color(0xFF4CAF50), distinctList.size) }
            items(distinctList, key = { "done_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) }, isDone = true)
            }
        }
    }
}

// ── Partition tab (Deadlines / Meetings / Messages / etc.) ────────────────────

@Composable
private fun PartitionView(
    tasks: List<TaskEntity>,
    filter: TaskFilter,
    padding: PaddingValues,
    expandedId: String?,
    onExpand: (String) -> Unit,
    onMarkDone: (String) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyPartition(filter, Modifier.padding(padding))
        return
    }

    // Group by priority within the partition
    val critical = tasks.filter { it.importance == "CRITICAL" }
    val high     = tasks.filter { it.importance == "HIGH" }
    val medium   = tasks.filter { it.importance == "MEDIUM" }
    val low      = tasks.filter { it.importance == "LOW" }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top   = padding.calculateTopPadding() + 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (critical.isNotEmpty()) {
            val distinctList = critical.distinctBy { it.id }
            item { SectionHeader("🔴 CRITICAL", Color(0xFFFF4444), distinctList.size) }
            items(distinctList, key = { "crit_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (high.isNotEmpty()) {
            val distinctList = high.distinctBy { it.id }
            item { SectionHeader("🟠 HIGH", Color(0xFFFF8C00), distinctList.size) }
            items(distinctList, key = { "hi_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (medium.isNotEmpty()) {
            val distinctList = medium.distinctBy { it.id }
            item { SectionHeader("🔵 MEDIUM", Color(0xFF2196F3), distinctList.size) }
            items(distinctList, key = { "med_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (low.isNotEmpty()) {
            val distinctList = low.distinctBy { it.id }
            item { SectionHeader("🟢 LOW", Color(0xFF4CAF50), distinctList.size) }
            items(distinctList, key = { "low_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun PriorityBubble(count: Int, color: Color, emoji: String) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            "$emoji $count",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: Color, count: Int) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
            Text(
                count.toString(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
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
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )

    val impColor  = importanceColor(task.importance)
    val catEmoji  = categoryEmoji(task.category)
    val impEmoji  = importanceEmoji(task.importance)

    val deadlineColor = task.deadlineTimestamp?.let { ts ->
        val remaining = ts - System.currentTimeMillis()
        when {
            remaining < 0               -> Color(0xFFFF4444)
            remaining < 2 * 3_600_000L  -> Color(0xFFFF4444)
            remaining < 24 * 3_600_000L -> Color(0xFFFF8C00)
            else                        -> Color(0xFF4CAF50)
        }
    }

    val borderColor = if (isOverdue) impColor.copy(alpha = pulseAlpha)
                      else           impColor.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .graphicsLayer { alpha = if (isDone) 0.55f else 1f },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category icon circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(impColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Text(catEmoji, fontSize = 17.sp) }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.extractedTask,
                        color = if (isDone) Color.White.copy(alpha = 0.45f) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = task.sourceApp,
                        color = Color.White.copy(alpha = 0.38f),
                        fontSize = 11.sp
                    )
                }

                // Priority badge
                Surface(
                    color = impColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, impColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$impEmoji ${task.importance.take(4)}",
                        color = impColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Deadline row
            task.deadline?.let { dl ->
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule, null,
                        tint = deadlineColor ?: Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = dl,
                        color = deadlineColor ?: Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            // Meeting calendar tag
            if (task.category == "MEETING") {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF2196F3).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "📅 Added to calendar",
                        color = Color(0xFF2196F3),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Scheduled reminder badge
            task.scheduledFor?.let { sf ->
                if (sf > System.currentTimeMillis()) {
                    val schedLabel = java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(sf))
                    Spacer(Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications, null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "⏰ Reminder: $schedLabel",
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Ignored state badge
            if (task.userDecision == "IGNORE") {
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Block, null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "Ignored${task.ignoreReason?.let { " — $it" } ?: ""}",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Forwarded/Delegated state badge
            if (task.userDecision == "DELEGATE") {
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Send, null,
                        tint = Color(0xFFAB47BC).copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "↗️ Forwarded to someone else",
                        color = Color(0xFFAB47BC).copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            // Expanded details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
                    Spacer(Modifier.height(8.dp))
                    Text("Original notification:", color = Color.White.copy(alpha = 0.38f), fontSize = 11.sp)
                    Text(task.originalBody, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Captured: ${formatTime(task.capturedAt)}",
                        color = Color.White.copy(alpha = 0.28f),
                        fontSize = 10.sp
                    )
                    task.userDecision?.let { d ->
                        Text(
                            "Decision: $d",
                            color = Color(0xFF6C63FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!isDone) {
                        Spacer(Modifier.height(10.dp))
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

// ── Missed tab ────────────────────────────────────────────────────────────────

@Composable
private fun MissedView(
    tasks: List<TaskEntity>,
    padding: PaddingValues,
    onDidThis: (String) -> Unit
) {
    if (tasks.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🌟", fontSize = 64.sp)
                Text("Nothing missed!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Tasks whose deadline passed will appear here",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top   = padding.calculateTopPadding() + 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Warning banner
        item {
            Surface(
                color  = Color(0xFFFF8C00).copy(alpha = 0.12f),
                shape  = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFF8C00).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("😶‍🌫️", fontSize = 22.sp)
                    Column {
                        Text(
                            "These passed without action",
                            color = Color(0xFFFF8C00),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap \"I did this\" if you handled it offline",
                            color = Color(0xFFFF8C00).copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        items(tasks.distinctBy { it.id }, key = { "missed_${it.id}" }) { task ->
            MissedCard(task = task, onDidThis = { onDidThis(task.id) })
        }
    }
}

@Composable
private fun MissedCard(task: TaskEntity, onDidThis: () -> Unit) {
    val catEmoji = categoryEmoji(task.category)
    val missedAgo = task.deadlineTimestamp?.let { ts ->
        val diffMs = System.currentTimeMillis() - ts
        val hours  = TimeUnit.MILLISECONDS.toHours(diffMs)
        val mins   = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
        when {
            hours >= 24 -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}d ago"
            hours > 0   -> "${hours}h ${mins}m ago"
            else        -> "${mins}m ago"
        }
    } ?: "deadline passed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFF8C00).copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF8C00).copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) { Text(catEmoji, fontSize = 17.sp) }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = task.extractedTask,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    Text(
                        text = task.sourceApp,
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 11.sp
                    )
                }

                // "Missed X ago" badge
                Surface(
                    color  = Color(0xFFFF8C00).copy(alpha = 0.12f),
                    shape  = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF8C00).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = missedAgo,
                        color = Color(0xFFFF8C00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Deadline label
            task.deadline?.let { dl ->
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule, null,
                        tint = Color(0xFFFF4444),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(dl, color = Color(0xFFFF4444), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // "I did this" button
            Button(
                onClick = onDidThis,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)),
                border  = BorderStroke(1.dp, Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I did this  ✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Empty / Completed states ───────────────────────────────────────────────────

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
private fun EmptyPartition(filter: TaskFilter, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(filter.emoji, fontSize = 56.sp)
            Text(
                "No ${filter.label}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "AI-classified ${filter.label.lowercase()} will appear here",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (TaskEntity) -> Unit) {
    var title    by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1A1A2E),
        title  = { Text("Add Task Manually", color = Color.White) },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task description", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor   = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline (optional)", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor   = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
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
