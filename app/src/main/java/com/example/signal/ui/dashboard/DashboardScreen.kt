package com.example.signal.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Week", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.loadStats() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF6C63FF))
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6C63FF))
            }
        } else {
            // ── Today at a Glance ────────────────────────────────────────────
            Text("Today at a Glance", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, letterSpacing = 1.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(Modifier.weight(1f), "📥", "Captured", state.todayTotal.toString(), Color(0xFF6C63FF))
                StatCard(Modifier.weight(1f), "✅", "Actioned", state.todayActioned.toString(), Color(0xFF4CAF50))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(Modifier.weight(1f), "📅", "Scheduled", state.todayScheduled.toString(), Color(0xFF2196F3))
                StatCard(Modifier.weight(1f), "❌", "Ignored", state.todayIgnored.toString(), Color(0xFFFF4444))
            }

            // ── Overdue ───────────────────────────────────────────────────────
            if (state.overdueCount > 0) {
                Surface(
                    color = Color(0xFFFF4444).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF4444).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⚠️", fontSize = 20.sp)
                        Column {
                            Text("${state.overdueCount} Overdue Task(s)", color = Color(0xFFFF4444), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("These tasks need immediate attention", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Streak ────────────────────────────────────────────────────────
            Surface(
                color = Color(0xFF1A1A2E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFF8C00).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("🔥", fontSize = 36.sp)
                    Column {
                        Text(
                            "${state.streakDays} Day Streak",
                            color = Color(0xFFFF8C00),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Days with zero ignored critical tasks",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── Weekly Chart ──────────────────────────────────────────────────
            Text("Weekly Activity", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, letterSpacing = 1.sp)

            Surface(
                color = Color(0xFF1A1A2E),
                shape = RoundedCornerShape(12.dp)
            ) {
                WeeklyBarChart(
                    data = state.weeklyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(16.dp)
                )
            }

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(Color(0xFF4CAF50), "Actioned")
                LegendDot(Color(0xFF2196F3), "Scheduled")
                LegendDot(Color(0xFFFF4444), "Ignored")
            }

            // ── Top Avoided ───────────────────────────────────────────────────
            if (state.topIgnoredCategories.isNotEmpty()) {
                Text("Most Avoided", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, letterSpacing = 1.sp)
                Surface(
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("You keep ignoring these:", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        state.topIgnoredCategories.forEachIndexed { i, cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${i + 1}. ${cat.category.lowercase().replaceFirstChar { it.uppercase() }}", color = Color.White, fontSize = 14.sp)
                                Text("${cat.cnt} times", color = Color(0xFFFF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── AI Insight ────────────────────────────────────────────────────
            Surface(
                color = Color(0xFF6C63FF).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF6C63FF).copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🤖", fontSize = 20.sp)
                    Column {
                        Text("AI Behavioral Insight", color = Color(0xFF9F97FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        val insightText = when {
                            state.todayIgnored > state.todayActioned ->
                                "You're deferring more than acting today. Try the 2-minute rule: if it takes less than 2 minutes, do it now."
                            state.streakDays >= 3 ->
                                "Great momentum! You've maintained a ${state.streakDays}-day streak. Keep enforcing your decisions."
                            state.overdueCount > 2 ->
                                "You have ${state.overdueCount} overdue tasks. Block 30 minutes today to clear the backlog."
                            else ->
                                "Stay consistent. Every enforced decision builds your accountability habit."
                        }
                        Text(insightText, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, emoji: String, label: String, value: String, color: Color) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

@Composable
private fun WeeklyBarChart(data: List<DayStats>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier) {
        val barWidth   = size.width / (data.size * 3f)
        val maxVal     = data.maxOf { it.actioned + it.scheduled + it.ignored }.coerceAtLeast(1)
        val chartHeight = size.height - 30.dp.toPx()

        data.forEachIndexed { i, day ->
            val groupX = i * (size.width / data.size)
            val total  = (day.actioned + day.scheduled + day.ignored).toFloat()
            var yOffset = chartHeight

            // stacked segments
            listOf(
                Pair(day.actioned,  android.graphics.Color.parseColor("#4CAF50")),
                Pair(day.scheduled, android.graphics.Color.parseColor("#2196F3")),
                Pair(day.ignored,   android.graphics.Color.parseColor("#FF4444"))
            ).forEach { (count, colorInt) ->
                if (count > 0) {
                    val barH = (count / maxVal.toFloat()) * chartHeight
                    drawRect(
                        color  = Color(colorInt),
                        topLeft = Offset(groupX + barWidth / 2, yOffset - barH),
                        size   = androidx.compose.ui.geometry.Size(barWidth, barH)
                    )
                    yOffset -= barH
                }
            }

            // Day label
            drawContext.canvas.nativeCanvas.drawText(
                day.label,
                groupX + barWidth,
                size.height,
                android.graphics.Paint().apply {
                    color     = android.graphics.Color.argb(150, 255, 255, 255)
                    textSize  = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
