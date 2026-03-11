package com.doman.wordsteps.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doman.wordsteps.data.database.DailyStats
import com.doman.wordsteps.data.database.ReconstructionScore
import com.doman.wordsteps.data.database.TimedScore
import com.doman.wordsteps.data.models.PatternMastery
import com.doman.wordsteps.data.models.UserStats
import com.doman.wordsteps.ui.home.patternLabels
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val NavyMid       = Color(0xFF142338)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val Purple        = Color(0xFF9D7FFF)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateBack: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Stats", "Leaderboards")

    Scaffold(
        containerColor = Navy,
        topBar = {
            TopAppBar(
                title = {
                    Text("Progress", color = TextPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = Amber)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyLight)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        val isSelected = index == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) Amber else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Navy else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTab == 0) {
                        OverviewCard(state.stats)
                        BestStreakCard(state.stats)
                        if (state.dailyStats.isNotEmpty()) {
                            AccuracyChartCard(state.dailyStats)
                        }
                        PatternBreakdownCard(state.patterns)
                        RecentMistakesCard(state.recentMistakes)
                    } else {
                        ReconstructionLeaderboardCard(state.reconstructionTopScores)
                        TimedLeaderboardCard(
                            scores30 = state.timedScores30,
                            scores60 = state.timedScores60,
                            scores90 = state.timedScores90
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(stats: UserStats) {
    val accuracy = if (stats.totalAttempts > 0)
        stats.correctAttempts.toFloat() / stats.totalAttempts else 0f
    val pct = (accuracy * 100).roundToInt()

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(color = NavyLight, startAngle = -90f, sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(color = Teal, startAngle = -90f, sweepAngle = 360f * accuracy,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$pct%", color = Teal, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("acc", color = TextSecondary, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OverviewRow("Total answered", "${stats.totalAttempts}", TextPrimary)
                OverviewRow("Correct",        "${stats.correctAttempts}", Teal)
                OverviewRow("Incorrect",
                    "${stats.totalAttempts - stats.correctAttempts}", Rose)
            }
        }
    }
}

@Composable
private fun OverviewRow(label: String, value: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.width(120.dp))
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BestStreakCard(stats: UserStats) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceAround) {
            StreakStat("🔥", "${stats.currentStreak}", "Current Streak", Amber)
            Divider(Modifier.height(48.dp).width(1.dp), color = NavyLight)
            StreakStat("🏆", "${stats.bestStreak}", "Best Streak", Teal)
        }
    }
}

@Composable
private fun StreakStat(emoji: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun AccuracyChartCard(dailyStats: List<DailyStats>) {
    // dailyStats comes newest-first from DB, reverse for chart left→right
    val sorted = dailyStats.reversed()
    val dateLabel = SimpleDateFormat("MMM d", Locale.getDefault())

    SectionCard {
        Text("Accuracy Over Time", color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Last ${sorted.size} days", color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))

        if (sorted.size < 2) {
            Text("Keep practicing — chart appears after 2 days of data.",
                color = TextSecondary, fontSize = 13.sp)
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w = size.width
                val h = size.height
                val padBottom = 24.dp.toPx()
                val padTop    = 12.dp.toPx()
                val chartH    = h - padBottom - padTop
                val stepX     = w / (sorted.size - 1).toFloat()

                // Grid lines at 0%, 50%, 100%
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = padTop + chartH * (1f - frac)
                    drawLine(
                        color       = NavyLight,
                        start       = Offset(0f, y),
                        end         = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Filled area under the line
                val fillPath = Path().apply {
                    sorted.forEachIndexed { i, day ->
                        val x = i * stepX
                        val y = padTop + chartH * (1f - day.accuracy)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    lineTo((sorted.size - 1) * stepX, padTop + chartH)
                    lineTo(0f, padTop + chartH)
                    close()
                }
                drawPath(
                    path  = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Teal.copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                // Line
                val linePath = Path().apply {
                    sorted.forEachIndexed { i, day ->
                        val x = i * stepX
                        val y = padTop + chartH * (1f - day.accuracy)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path        = linePath,
                    color       = Teal,
                    style       = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Dots
                sorted.forEachIndexed { i, day ->
                    val x = i * stepX
                    val y = padTop + chartH * (1f - day.accuracy)
                    drawCircle(color = Teal,        radius = 4.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = NavyMid,     radius = 2.dp.toPx(), center = Offset(x, y))
                }
            }

            // X-axis labels — show first, middle, last
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                val indices = listOf(0, sorted.size / 2, sorted.size - 1).distinct()
                val shown   = sorted.filterIndexed { i, _ -> i in indices }
                shown.forEach { day ->
                    Text(
                        try {
                            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(day.date)
                            dateLabel.format(parsed ?: Date())
                        } catch (e: Exception) { day.date },
                        color    = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternBreakdownCard(patterns: List<PatternMastery>) {
    SectionCard {
        Text("Pattern Breakdown", color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        if (patterns.isEmpty()) {
            Text("No data yet — complete a practice session first.",
                color = TextSecondary, fontSize = 13.sp)
        } else {
            patterns.forEach { p ->
                val accuracy = p.accuracy
                val label    = patternLabels[p.pattern] ?: p.pattern
                val color    = when {
                    accuracy < 0.4f -> Rose
                    accuracy < 0.7f -> Amber
                    else            -> Teal
                }
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically) {
                        Column {
                            Text(label, color = TextPrimary, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium)
                            Text("${p.totalAttempts} attempts",
                                color = TextSecondary, fontSize = 11.sp)
                        }
                        Text("${(accuracy * 100).roundToInt()}%",
                            color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp)
                        .clip(RoundedCornerShape(50)).background(NavyLight)) {
                        Box(Modifier.fillMaxWidth(accuracy.coerceIn(0f, 1f))
                            .fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMistakesCard(mistakes: List<com.doman.wordsteps.data.models.Attempt>) {
    SectionCard {
        Text("Recent Mistakes", color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        if (mistakes.isEmpty()) {
            Text("No mistakes yet — great job! 🎉", color = TextSecondary, fontSize = 13.sp)
        } else {
            mistakes.take(10).forEach { attempt ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(attempt.correctWord, color = Teal,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("→", color = TextSecondary, fontSize = 14.sp)
                            Text(attempt.userAnswer, color = Rose, fontSize = 14.sp)
                        }
                        attempt.mistakePattern?.let { pattern ->
                            Text(patternLabels[pattern] ?: pattern,
                                color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
                if (attempt != mistakes.take(10).last())
                    Divider(color = NavyLight, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun ReconstructionLeaderboardCard(scores: List<ReconstructionScore>) {
    val medals     = listOf("🥇", "🥈", "🥉")
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A0A3D), NavyMid)))
            .border(1.dp, Purple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("🧩 Word Reconstruction", color = TextPrimary,
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Top 3", color = Purple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))
        if (scores.isEmpty()) {
            Text("Play Word Reconstruction to set a score!",
                color = TextSecondary, fontSize = 13.sp)
        } else {
            scores.forEachIndexed { index, score ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(medals.getOrElse(index) { "·" }, fontSize = 22.sp)
                    Column(Modifier.weight(1f)) {
                        Text("${score.totalPoints} pts",
                            color = when (index) {
                                0    -> Amber
                                1    -> Color(0xFFCDD1D6)
                                else -> Color(0xFFCD7F32)
                            },
                            fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${score.wordsCompleted}/${score.totalWords} words  •  ×${"%.1f".format(score.bestCombo)} combo  •  ${dateFormat.format(Date(score.timestamp))}",
                            color = TextSecondary, fontSize = 11.sp)
                    }
                }
                if (index < scores.size - 1)
                    Divider(color = NavyLight.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun TimedLeaderboardCard(
    scores30: List<TimedScore>,
    scores60: List<TimedScore>,
    scores90: List<TimedScore>
) {
    val tabs      = listOf("30s", "60s", "90s")
    val allScores = listOf(scores30, scores60, scores90)
    var selected  by remember { mutableStateOf(0) }
    val medals     = listOf("🥇", "🥈", "🥉")
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3D2A00), NavyMid)))
            .border(1.dp, Amber.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("⏱ Timed Blitz", color = TextPrimary,
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Top 3", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))

        // Duration tabs
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NavyLight)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selected
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Amber else Color.Transparent)
                        .clickable { selected = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label,
                        color = if (isSelected) Navy else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        val scores = allScores[selected]
        if (scores.isEmpty()) {
            Text("No scores for ${tabs[selected]} yet!",
                color = TextSecondary, fontSize = 13.sp)
        } else {
            scores.forEachIndexed { index, score ->
                val accuracy = if (score.wordsAnswered > 0)
                    (score.score * 100 / score.wordsAnswered) else 0
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(medals.getOrElse(index) { "·" }, fontSize = 22.sp)
                    Column(Modifier.weight(1f)) {
                        Text("${score.score} correct",
                            color = when (index) {
                                0    -> Amber
                                1    -> Color(0xFFCDD1D6)
                                else -> Color(0xFFCD7F32)
                            },
                            fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${score.wordsAnswered} answered  •  $accuracy% acc  •  ${dateFormat.format(Date(score.timestamp))}",
                            color = TextSecondary, fontSize = 11.sp)
                    }
                }
                if (index < scores.size - 1)
                    Divider(color = NavyLight.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

// ── Reusable card ─────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NavyMid)
            .border(1.dp, NavyLight, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) { content() }
}