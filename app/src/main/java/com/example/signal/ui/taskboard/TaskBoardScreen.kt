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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.signal.data.local.TaskEntity
import com.example.signal.ui.theme.*
import java.util.*
import java.util.concurrent.TimeUnit

// ── Priority helpers ───────────────────────────────────────────────────────────

private fun importanceColor(importance: String) = when (importance) {
    "CRITICAL" -> Rose500
    "HIGH"     -> Amber500
    "MEDIUM"   -> Blue500
    else       -> Emerald500
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "DEADLINE"    -> Icons.Outlined.Timer
    "MEETING"     -> Icons.Outlined.Groups
    "PAYMENT"     -> Icons.Outlined.AccountBalance
    "MESSAGE"     -> Icons.Outlined.Chat
    "REMINDER"    -> Icons.Outlined.NotificationsNone
    "PROMOTIONAL" -> Icons.Outlined.Sell
    else          -> Icons.Outlined.PushPin
}

private fun TaskFilter.filterIcon(): ImageVector = when (this) {
    TaskFilter.ALL           -> Icons.Outlined.AllInbox
    TaskFilter.HIGH_PRIORITY -> Icons.Outlined.PriorityHigh
    TaskFilter.DEADLINES     -> Icons.Outlined.Timer
    TaskFilter.MEETINGS      -> Icons.Outlined.Groups
    TaskFilter.MESSAGES      -> Icons.Outlined.Chat
    TaskFilter.PAYMENTS      -> Icons.Outlined.AccountBalance
    TaskFilter.REMINDERS     -> Icons.Outlined.NotificationsNone
    TaskFilter.MISSED        -> Icons.Outlined.ErrorOutline
    TaskFilter.ADVERTS       -> Icons.Outlined.Sell
    TaskFilter.OTHER         -> Icons.Outlined.MoreHoriz
}

private fun importanceLabel(importance: String) = when (importance) {
    "CRITICAL" -> "Crit"
    "HIGH"     -> "High"
    "MEDIUM"   -> "Med"
    else       -> "Low"
}

// ── Screen ─────────────────────────────────────────────────────────────────────

@Composable
fun TaskBoardScreen(viewModel: TaskViewModel = hiltViewModel()) {
    val state  by viewModel.uiState.collectAsStateWithLifecycle()
    val cs     = MaterialTheme.colorScheme
    var showAddDialog  by remember { mutableStateOf(false) }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.background)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Inbox",
                            style = MaterialTheme.typography.headlineMedium,
                            color = cs.onBackground
                        )
                        Text(
                            "${state.allMessages.size} active",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val critical = state.allMessages.count { it.importance == "CRITICAL" }
                        val high     = state.allMessages.count { it.importance == "HIGH" }
                        if (critical > 0) PriorityBubble(critical, Rose500)
                        if (high > 0)     PriorityBubble(high,     Amber500)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search bar
                OutlinedTextField(
                    value       = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = {
                        Text(
                            "Search tasks…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier    = Modifier.fillMaxWidth().height(50.dp),
                    singleLine  = true,
                    shape       = RoundedCornerShape(12.dp),
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = cs.onSurface,
                        unfocusedTextColor   = cs.onSurface,
                        focusedBorderColor   = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        focusedContainerColor    = cs.surfaceVariant,
                        unfocusedContainerColor  = cs.surfaceVariant
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp)
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
                            leadingIcon = {
                                Icon(
                                    filter.filterIcon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    "${filter.label}${if (count > 0) " ($count)" else ""}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (filter) {
                                    TaskFilter.HIGH_PRIORITY -> Rose500
                                    TaskFilter.MISSED        -> Amber500
                                    else                     -> cs.primary
                                },
                                selectedLabelColor      = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor          = cs.surfaceVariant,
                                labelColor              = cs.onSurfaceVariant,
                                iconColor               = cs.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled          = true,
                                selected         = selected,
                                borderColor      = cs.outline,
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { showAddDialog = true },
                containerColor   = cs.primary,
                contentColor     = Color.White,
                shape            = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->

        val displayList = state.currentList()

        when {
            state.selectedFilter == TaskFilter.MISSED -> MissedView(
                tasks     = state.missed,
                padding   = padding,
                onDidThis = viewModel::markMissedAsDone
            )
            displayList.isEmpty() && state.done.isEmpty() && state.selectedFilter != TaskFilter.ALL ->
                EmptyPartition(state.selectedFilter, Modifier.padding(padding))
            state.selectedFilter == TaskFilter.ALL -> AllMessagesView(
                state      = state,
                padding    = padding,
                expandedId = expandedTaskId,
                onExpand   = { id -> expandedTaskId = if (expandedTaskId == id) null else id },
                onMarkDone = viewModel::markDone
            )
            else -> PartitionView(
                tasks      = displayList,
                filter     = state.selectedFilter,
                padding    = padding,
                expandedId = expandedTaskId,
                onExpand   = { id -> expandedTaskId = if (expandedTaskId == id) null else id },
                onMarkDone = viewModel::markDone
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

    if (allEmpty) { EmptyState(modifier = Modifier.padding(padding)); return }

    val shownIds        = mutableSetOf<String>()
    val overdueTasks    = state.overdue.also { shownIds.addAll(it.map { t -> t.id }) }
    val criticalHigh    = state.highPriority.filter { !shownIds.contains(it.id) }.also { shownIds.addAll(it.map { t -> t.id }) }
    val inProgressTasks = state.inProgress.filter { !shownIds.contains(it.id) }.also { shownIds.addAll(it.map { t -> t.id }) }
    val pendingTasks    = state.pending.filter { !shownIds.contains(it.id) }.also { shownIds.addAll(it.map { t -> t.id }) }
    val doneTasks       = state.done.filter { !shownIds.contains(it.id) }

    LazyColumn(
        contentPadding = PaddingValues(
            start  = 16.dp, end    = 16.dp,
            top    = padding.calculateTopPadding() + 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (overdueTasks.isNotEmpty()) {
            val list = overdueTasks.distinctBy { it.id }
            item { SectionHeader("Overdue", Rose500, Icons.Outlined.ErrorOutline, list.size) }
            items(list, key = { "overdue_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) }, isOverdue = true)
            }
        }
        if (criticalHigh.isNotEmpty()) {
            val list = criticalHigh.distinctBy { it.id }
            item { SectionHeader("High Priority", Amber500, Icons.Outlined.PriorityHigh, list.size) }
            items(list, key = { "high_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (inProgressTasks.isNotEmpty()) {
            val list = inProgressTasks.distinctBy { it.id }
            item { SectionHeader("In Progress", Blue500, Icons.Outlined.HourglassTop, list.size) }
            items(list, key = { "inprog_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (pendingTasks.isNotEmpty()) {
            val list = pendingTasks.distinctBy { it.id }
            item { SectionHeader("Pending", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Outlined.Inbox, list.size) }
            items(list, key = { "pending_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (doneTasks.isNotEmpty()) {
            val list = doneTasks.distinctBy { it.id }
            item { SectionHeader("Completed", Emerald500, Icons.Outlined.DoneAll, list.size) }
            items(list, key = { "done_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) }, isDone = true)
            }
        }
    }
}

// ── Partition tab ─────────────────────────────────────────────────────────────

@Composable
private fun PartitionView(
    tasks: List<TaskEntity>,
    filter: TaskFilter,
    padding: PaddingValues,
    expandedId: String?,
    onExpand: (String) -> Unit,
    onMarkDone: (String) -> Unit
) {
    if (tasks.isEmpty()) { EmptyPartition(filter, Modifier.padding(padding)); return }

    val critical = tasks.filter { it.importance == "CRITICAL" }
    val high     = tasks.filter { it.importance == "HIGH" }
    val medium   = tasks.filter { it.importance == "MEDIUM" }
    val low      = tasks.filter { it.importance == "LOW" }

    LazyColumn(
        contentPadding = PaddingValues(
            start  = 16.dp, end    = 16.dp,
            top    = padding.calculateTopPadding() + 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (critical.isNotEmpty()) {
            val list = critical.distinctBy { it.id }
            item { SectionHeader("Critical", Rose500, Icons.Outlined.PriorityHigh, list.size) }
            items(list, key = { "crit_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (high.isNotEmpty()) {
            val list = high.distinctBy { it.id }
            item { SectionHeader("High", Amber500, Icons.Outlined.Bolt, list.size) }
            items(list, key = { "hi_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (medium.isNotEmpty()) {
            val list = medium.distinctBy { it.id }
            item { SectionHeader("Medium", Blue500, Icons.Outlined.Remove, list.size) }
            items(list, key = { "med_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
        if (low.isNotEmpty()) {
            val list = low.distinctBy { it.id }
            item { SectionHeader("Low", Emerald500, Icons.Outlined.KeyboardArrowDown, list.size) }
            items(list, key = { "low_${it.id}" }) { task ->
                TaskCard(task, expandedId == task.id, { onExpand(task.id) }, { onMarkDone(task.id) })
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun PriorityBubble(count: Int, color: Color) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color  = color.copy(alpha = 0.12f),
        shape  = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            "$count",
            color      = color,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    color: Color,
    icon: ImageVector,
    count: Int
) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text       = title.uppercase(),
            style      = MaterialTheme.typography.labelMedium,
            color      = color,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                count.toString(),
                style      = MaterialTheme.typography.labelSmall,
                color      = color,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
    val cs = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.9f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    val impColor   = importanceColor(task.importance)
    val catIcon    = categoryIcon(task.category)
    val borderColor = if (isOverdue) impColor.copy(alpha = pulseAlpha) else cs.outlineVariant

    val deadlineColor = task.deadlineTimestamp?.let { ts ->
        val remaining = ts - System.currentTimeMillis()
        when {
            remaining < 0              -> Rose500
            remaining < 2 * 3_600_000L -> Rose500
            remaining < 24 * 3_600_000L -> Amber500
            else                       -> Emerald500
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .graphicsLayer { alpha = if (isDone) 0.55f else 1f },
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(impColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = impColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text  = task.extractedTask,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isDone) cs.onSurface.copy(alpha = 0.45f) else cs.onSurface,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text  = task.sourceApp,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                // Priority badge
                Surface(
                    color  = impColor.copy(alpha = 0.10f),
                    shape  = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, impColor.copy(alpha = 0.30f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(impColor)
                        )
                        Text(
                            text  = importanceLabel(task.importance),
                            style = MaterialTheme.typography.labelSmall,
                            color = impColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                        Icons.Outlined.Schedule, null,
                        tint = deadlineColor ?: cs.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text  = dl,
                        style = MaterialTheme.typography.bodySmall,
                        color = deadlineColor ?: cs.onSurfaceVariant
                    )
                }
            }

            // Meeting calendar tag
            if (task.category == "MEETING") {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = Blue500.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth, null,
                            tint = Blue500,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "Added to calendar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Blue500
                        )
                    }
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
                            Icons.Outlined.Notifications, null,
                            tint = Blue500,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "Reminder: $schedLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = Blue500
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
                        Icons.Outlined.Block, null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "Ignored${task.ignoreReason?.let { " — $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Delegated state badge
            if (task.userDecision == "DELEGATE") {
                Spacer(Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Send, null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "Forwarded to someone else",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            // Expanded details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = cs.outlineVariant)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Original notification",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        task.originalBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Captured: ${formatTime(task.capturedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                    task.userDecision?.let { d ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Decision: $d",
                            style      = MaterialTheme.typography.labelMedium,
                            color      = cs.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!isDone) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onMarkDone,
                            border  = BorderStroke(1.dp, Emerald500),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle, null,
                                tint = Emerald500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Mark as Done", color = Emerald500)
                        }
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
    val cs = MaterialTheme.colorScheme

    if (tasks.isEmpty()) {
        Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.DoneAll, null,
                        tint = Emerald500,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text("Nothing missed!", style = MaterialTheme.typography.headlineSmall, color = cs.onBackground)
                Text(
                    "Tasks whose deadline passed will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start  = 16.dp, end    = 16.dp,
            top    = padding.calculateTopPadding() + 8.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color    = Amber500.copy(alpha = 0.08f),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, Amber500.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline, null,
                        tint = Amber500,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            "These passed without action",
                            style = MaterialTheme.typography.titleSmall,
                            color = Amber500,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tap \"I did this\" if you handled it offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
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
    val cs = MaterialTheme.colorScheme
    val catIcon = categoryIcon(task.category)
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
        colors   = CardDefaults.cardColors(containerColor = cs.surface),
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, Amber500.copy(alpha = 0.30f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Amber500.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = catIcon,
                        contentDescription = null,
                        tint = Amber500,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text  = task.extractedTask,
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface.copy(alpha = 0.75f),
                        maxLines = 2
                    )
                    Text(
                        text  = task.sourceApp,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }

                Surface(
                    color  = Amber500.copy(alpha = 0.10f),
                    shape  = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Amber500.copy(alpha = 0.30f))
                ) {
                    Text(
                        text     = missedAgo,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Amber500,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            task.deadline?.let { dl ->
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Schedule, null,
                        tint = Rose500,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(dl, style = MaterialTheme.typography.bodySmall, color = Rose500)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick  = onDidThis,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Emerald500.copy(alpha = 0.12f),
                    contentColor   = Emerald500
                ),
                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.50f))
            ) {
                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("I did this", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Empty states ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Inbox, null,
                    tint = cs.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text("All clear!", style = MaterialTheme.typography.headlineSmall, color = cs.onBackground)
            Text(
                "No pending tasks. Keep it up!",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPartition(filter: TaskFilter, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    filter.filterIcon(), null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                "No ${filter.label}",
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onBackground
            )
            Text(
                "AI-classified ${filter.label.lowercase()} will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }
    }
}

// ── Add task dialog ────────────────────────────────────────────────────────────

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (TaskEntity) -> Unit) {
    val cs       = MaterialTheme.colorScheme
    var title    by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = cs.surface,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text(
                "New Task",
                style = MaterialTheme.typography.titleLarge,
                color = cs.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it },
                    label         = { Text("Task description") },
                    leadingIcon   = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = cs.onSurface,
                        unfocusedTextColor   = cs.onSurface,
                        focusedBorderColor   = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        focusedLabelColor    = cs.primary,
                        unfocusedLabelColor  = cs.onSurfaceVariant,
                        focusedContainerColor    = cs.surfaceVariant,
                        unfocusedContainerColor  = cs.surfaceVariant
                    )
                )
                OutlinedTextField(
                    value         = deadline,
                    onValueChange = { deadline = it },
                    label         = { Text("Deadline (optional)") },
                    leadingIcon   = { Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(18.dp)) },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = cs.onSurface,
                        unfocusedTextColor   = cs.onSurface,
                        focusedBorderColor   = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        focusedLabelColor    = cs.primary,
                        unfocusedLabelColor  = cs.onSurfaceVariant,
                        focusedContainerColor    = cs.surfaceVariant,
                        unfocusedContainerColor  = cs.surfaceVariant
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
                                id                  = UUID.randomUUID().toString(),
                                sourceApp           = "Manual",
                                packageName         = "",
                                originalTitle       = title,
                                originalBody        = "",
                                capturedAt          = System.currentTimeMillis(),
                                extractedTask       = title,
                                importance          = "MEDIUM",
                                category            = "OTHER",
                                deadline            = deadline.ifBlank { null },
                                deadlineTimestamp   = null,
                                suggestedActions    = "[]",
                                status              = "PENDING",
                                userDecision        = null,
                                ignoreReason        = null,
                                scheduledFor        = null,
                                decidedAt           = null,
                                completedAt         = null,
                                requiresEnforcement = false,
                                isOverdue           = false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                shape  = RoundedCornerShape(12.dp)
            ) { Text("Add Task") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.onSurfaceVariant)
            }
        }
    )
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun formatTime(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
