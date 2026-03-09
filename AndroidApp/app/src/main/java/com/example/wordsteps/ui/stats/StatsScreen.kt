package com.example.wordsteps.ui.stats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wordsteps.data.database.ReconstructionScore
import com.example.wordsteps.data.models.PatternMastery
import com.example.wordsteps.data.models.UserStats
import com.example.wordsteps.ui.home.patternLabels
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OverviewCard(state.stats)
                BestStreakCard(state.stats)
                AllPatternsCard(state.patterns)
                RecentMistakesCard(state.recentMistakes)
                ReconstructionLeaderboardCard(state.reconstructionTopScores)  // ← NEW
            }
        }
    }
}

// ── Overall accuracy ring ────────────────────────────────────────────────────
@Composable
private fun OverviewCard(stats: UserStats) {
    val accuracy = if (stats.totalAttempts > 0)
        stats.correctAttempts.toFloat() / stats.totalAttempts else 0f
    val pct = (accuracy * 100).roundToInt()

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
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

// ── Streak card ──────────────────────────────────────────────────────────────
@Composable
private fun BestStreakCard(stats: UserStats) {
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            StreakStat("🔥", "${stats.currentStreak}", "Current Streak", Amber)
            Divider(modifier = Modifier.height(48.dp).width(1.dp), color = NavyLight)
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

// ── Pattern mastery bars ─────────────────────────────────────────────────────
@Composable
private fun AllPatternsCard(patterns: List<PatternMastery>) {
    SectionCard {
        Column {
            Text("Pattern Breakdown", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            if (patterns.isEmpty()) {
                Text("No data yet — complete a practice session first.",
                    color = TextSecondary, fontSize = 13.sp)
            } else {
                patterns.forEach { p ->
                    val accuracy = p.accuracy
                    val label    = patternLabels[p.pattern] ?: p.pattern
                    val color    = when { accuracy < 0.4f -> Rose; accuracy < 0.7f -> Amber; else -> Teal }
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("${p.totalAttempts} attempts", color = TextSecondary, fontSize = 11.sp)
                            }
                            Text("${(accuracy * 100).roundToInt()}%",
                                color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(50)).background(NavyLight)) {
                            Box(modifier = Modifier.fillMaxWidth(accuracy.coerceIn(0f, 1f))
                                .fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
                        }
                    }
                }
            }
        }
    }
}

// ── Recent mistakes list ─────────────────────────────────────────────────────
@Composable
private fun RecentMistakesCard(mistakes: List<com.example.wordsteps.data.models.Attempt>) {
    SectionCard {
        Column {
            Text("Recent Mistakes", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            if (mistakes.isEmpty()) {
                Text("No mistakes yet — great job! 🎉", color = TextSecondary, fontSize = 13.sp)
            } else {
                mistakes.take(10).forEach { attempt ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
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
                    if (attempt != mistakes.take(10).last()) {
                        Divider(color = NavyLight, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ── Word Reconstruction leaderboard ─────────────────────────────────────────
@Composable
private fun ReconstructionLeaderboardCard(scores: List<ReconstructionScore>) {
    val medals     = listOf("🥇", "🥈", "🥉")
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF1A0A3D), NavyMid))
            )
            .border(1.dp, Purple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🧩 Word Reconstruction", color = TextPrimary,
                fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Top 3", color = Purple, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))

        if (scores.isEmpty()) {
            Text("Play Word Reconstruction to set a score!",
                color = TextSecondary, fontSize = 13.sp)
        } else {
            scores.forEachIndexed { index, score ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(medals.getOrElse(index) { "·" }, fontSize = 22.sp)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${score.totalPoints} pts",
                            color = when (index) { 0 -> Amber; 1 -> Color(0xFFCDD1D6); else -> Color(0xFFCD7F32) },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "${score.wordsCompleted}/${score.totalWords} words  •  ×${"%.1f".format(score.bestCombo)} combo  •  ${dateFormat.format(Date(score.timestamp))}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                if (index < scores.size - 1) {
                    Divider(color = NavyLight.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── Reusable card wrapper ────────────────────────────────────────────────────
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NavyMid)
            .border(1.dp, NavyLight, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        content()
    }
}