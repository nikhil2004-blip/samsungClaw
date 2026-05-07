package com.example.signal.ui.dashboard

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.signal.ui.theme.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    color = cs.onBackground
                )
                Text(
                    text  = "Your productivity at a glance",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.loadStats() },
                colors  = IconButtonDefaults.iconButtonColors(
                    containerColor = cs.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Refresh",
                    tint = cs.onSurfaceVariant
                )
            }
        }

        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp)
            }
        } else {

            // ── Today at a Glance ──────────────────────────────────────────────
            SectionLabel("Today at a glance")

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Outlined.MoveToInbox,
                        label    = "Captured",
                        value    = state.todayTotal.toString(),
                        accentColor = cs.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Outlined.CheckCircle,
                        label    = "Actioned",
                        value    = state.todayActioned.toString(),
                        accentColor = Emerald500
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Outlined.CalendarToday,
                        label    = "Scheduled",
                        value    = state.todayScheduled.toString(),
                        accentColor = Blue500
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon     = Icons.Outlined.RemoveCircleOutline,
                        label    = "Ignored",
                        value    = state.todayIgnored.toString(),
                        accentColor = Rose500
                    )
                }
            }

            // ── Overdue alert ──────────────────────────────────────────────────
            if (state.overdueCount > 0) {
                Surface(
                    shape  = RoundedCornerShape(14.dp),
                    color  = Rose500.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Rose500.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Rose500.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Rose500,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${state.overdueCount} Overdue Task${if (state.overdueCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Rose500,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Needs immediate attention",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Streak ─────────────────────────────────────────────────────────
            Surface(
                shape  = RoundedCornerShape(14.dp),
                color  = cs.surface,
                border = BorderStroke(1.dp, cs.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Amber500.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Whatshot,
                            contentDescription = null,
                            tint = Amber500,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${state.streakDays} Day Streak",
                            style = MaterialTheme.typography.titleMedium,
                            color = Amber500,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Zero ignored critical tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Text(
                        "🔥",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            // ── Weekly Activity ────────────────────────────────────────────────
            SectionLabel("Weekly activity")

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cs.surface,
                border = BorderStroke(1.dp, cs.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeeklyBarChart(
                        data        = state.weeklyData,
                        labelColor  = cs.onSurfaceVariant,
                        modifier    = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )

                    HorizontalDivider(color = cs.outlineVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(Emerald500, "Actioned")
                        LegendDot(Blue500,    "Scheduled")
                        LegendDot(Rose500,    "Ignored")
                    }
                }
            }

            // ── Most Avoided ───────────────────────────────────────────────────
            if (state.topIgnoredCategories.isNotEmpty()) {
                SectionLabel("Most avoided")

                Surface(
                    shape  = RoundedCornerShape(14.dp),
                    color  = cs.surface,
                    border = BorderStroke(1.dp, cs.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        state.topIgnoredCategories.forEachIndexed { i, cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text  = "${i + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurfaceVariant,
                                        modifier = Modifier.width(16.dp)
                                    )
                                    Text(
                                        text  = cat.category.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cs.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Rose500.copy(alpha = 0.10f)
                                ) {
                                    Text(
                                        "${cat.cnt}×",
                                        style  = MaterialTheme.typography.labelMedium,
                                        color  = Rose500,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (i < state.topIgnoredCategories.lastIndex) {
                                HorizontalDivider(color = cs.outlineVariant)
                            }
                        }
                    }
                }
            }

            // ── AI Insight ─────────────────────────────────────────────────────
            Surface(
                shape  = RoundedCornerShape(14.dp),
                color  = cs.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.20f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(cs.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "AI Insight",
                            style      = MaterialTheme.typography.titleSmall,
                            color      = cs.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        val insightText = when {
                            state.todayIgnored > state.todayActioned ->
                                "You're deferring more than acting today. Try the 2-minute rule — if it takes less than 2 minutes, do it now."
                            state.streakDays >= 3 ->
                                "Great momentum! You've maintained a ${state.streakDays}-day streak. Keep enforcing your decisions."
                            state.overdueCount > 2 ->
                                "You have ${state.overdueCount} overdue tasks. Block 30 minutes today to clear the backlog."
                            else ->
                                "Stay consistent. Every enforced decision builds your accountability habit."
                        }
                        Text(
                            insightText,
                            style  = MaterialTheme.typography.bodyMedium,
                            color  = cs.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text.uppercase(),
        style      = MaterialTheme.typography.labelMedium,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
    )
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        color    = cs.surface,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text  = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<DayStats>,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val emeraldArgb = Emerald500.toArgb()
    val blueArgb    = Blue500.toArgb()
    val roseArgb    = Rose500.toArgb()
    val labelArgb   = labelColor.toArgb()

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val barWidth    = size.width / (data.size * 3.2f)
        val maxVal      = data.maxOf { it.actioned + it.scheduled + it.ignored }.coerceAtLeast(1)
        val chartHeight = size.height - 28.dp.toPx()
        val cornerRadius = 4.dp.toPx()

        data.forEachIndexed { i, day ->
            val groupX = i * (size.width / data.size) + barWidth * 0.6f
            var yOffset = chartHeight

            listOf(
                Pair(day.actioned,  emeraldArgb),
                Pair(day.scheduled, blueArgb),
                Pair(day.ignored,   roseArgb)
            ).forEachIndexed { segIndex, (count, colorInt) ->
                if (count > 0) {
                    val barH = (count / maxVal.toFloat()) * chartHeight
                    val isTop = segIndex == 0
                    drawRoundRect(
                        color       = Color(colorInt),
                        topLeft     = Offset(groupX, yOffset - barH),
                        size        = androidx.compose.ui.geometry.Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            if (isTop) cornerRadius else 0f,
                            if (isTop) cornerRadius else 0f
                        )
                    )
                    yOffset -= barH
                }
            }

            drawContext.canvas.nativeCanvas.drawText(
                day.label,
                groupX + barWidth / 2,
                size.height,
                android.graphics.Paint().apply {
                    color     = labelArgb
                    textSize  = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}
