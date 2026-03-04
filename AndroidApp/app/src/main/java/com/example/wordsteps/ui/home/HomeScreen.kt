package com.example.wordsteps.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.roundToInt

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val RoseDim       = Color(0xFF3D0F1D)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

val patternLabels = mapOf(
    "vowel_swap"       to "Vowel Swap",
    "single_to_double" to "Single → Double",
    "vowel_drop"       to "Vowel Drop",
    "double_to_single" to "Double → Single",
    "insertion"        to "Extra Letter",
    "transposition"    to "Transposition",
    "consonant_drop"   to "Consonant Drop",
    "consonant_change" to "Consonant Change",
    "ie_ei_swap"       to "IE / EI Swap",
    "y_to_ie_ending"   to "Y → IE Ending",
    "i_y_swap"         to "I / Y Swap"
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartPractice: () -> Unit,
    onStartAdaptive: () -> Unit,
    onOpenStats: () -> Unit,
    onStartTyping: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state = viewModel.uiState.collectAsState().value

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadData()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Navy)) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(Color(0x22FFC044), Color.Transparent),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width / 2f, 0f)
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Amber)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Header ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("WordSteps", color = TextPrimary, fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                        Text("Master English spelling patterns",
                            color = TextSecondary, fontSize = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick  = onOpenStats,
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(NavyLight)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = "Stats", tint = Amber)
                        }
                        IconButton(
                            onClick  = onOpenSettings,
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(NavyLight)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                if (state.weakestPattern != null) {
                    WeakPatternCard(state.weakestPattern!!)
                    Spacer(Modifier.height(20.dp))
                }

                PracticeModeCard(
                    title = "Practice", subtitle = "Random words from all categories",
                    emoji = "📝",
                    gradient = Brush.linearGradient(listOf(Color(0xFF1A3A5C), Navy)),
                    accentColor = Teal, onClick = onStartPractice
                )
                Spacer(Modifier.height(14.dp))
                PracticeModeCard(
                    title = "Smart Practice", subtitle = "Targets your weakest spelling pattern",
                    emoji = "🎯",
                    gradient = Brush.linearGradient(listOf(Color(0xFF3D1A0A), Navy)),
                    accentColor = Amber, onClick = onStartAdaptive
                )
                Spacer(Modifier.height(14.dp))
                PracticeModeCard(
                    title = "Spelling Mode", subtitle = "Hear it, type it — no clues",
                    emoji = "🔊",
                    gradient = Brush.linearGradient(listOf(Color(0xFF1A0A3D), Navy)),
                    accentColor = Color(0xFF9D7FFF), onClick = onStartTyping
                )

                Spacer(Modifier.height(28.dp))
                if (state.patternMasteryList.isNotEmpty()) {
                    PatternMasterySection(state.patternMasteryList)
                }
            }
        }
    }
}

@Composable
private fun WeakPatternCard(pattern: com.example.wordsteps.data.models.PatternMastery) {
    val accuracy = (pattern.accuracy * 100).roundToInt()
    val label    = patternLabels[pattern.pattern] ?: pattern.pattern
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(RoseDim)
            .border(1.dp, Rose.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = Rose.copy(alpha = 0.2f), startAngle = -90f,
                        sweepAngle = 360f, useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(color = Rose, startAngle = -90f,
                        sweepAngle = 360f * (accuracy / 100f), useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("$accuracy%", color = Rose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Needs Work", color = Rose, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${pattern.totalAttempts} attempts · use Smart Practice to improve",
                    color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PracticeModeCard(
    title: String, subtitle: String, emoji: String,
    gradient: Brush, accentColor: Color, onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 36.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("→", color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PatternMasterySection(patterns: List<com.example.wordsteps.data.models.PatternMastery>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Pattern Mastery", color = TextSecondary, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        patterns.take(6).forEach { p ->
            val accuracy = p.accuracy
            val label    = patternLabels[p.pattern] ?: p.pattern
            val barColor = when {
                accuracy < 0.4f -> Rose
                accuracy < 0.7f -> Amber
                else            -> Teal
            }
            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = TextPrimary, fontSize = 13.sp)
                    Text("${(accuracy * 100).roundToInt()}%",
                        color = barColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(50)).background(NavyLight)) {
                    Box(modifier = Modifier.fillMaxWidth(accuracy.coerceIn(0f, 1f))
                        .fillMaxHeight().clip(RoundedCornerShape(50)).background(barColor))
                }
            }
        }
    }
}