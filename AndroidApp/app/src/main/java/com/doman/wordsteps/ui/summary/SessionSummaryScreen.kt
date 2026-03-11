package com.doman.wordsteps.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doman.wordsteps.data.models.SessionSummary
import com.doman.wordsteps.ui.home.patternLabels

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val NavyMid       = Color(0xFF142338)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

@Composable
fun SessionSummaryScreen(
    summary: SessionSummary,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val mistakePatterns = summary.patternBreakdown
        .filter { result -> summary.wrongWords.any { it.pattern == result.pattern } }
        .map { result -> result to summary.wrongWords.filter { it.pattern == result.pattern } }

    val unclassifiedMistakes = summary.wrongWords.filter { it.pattern == null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Session Complete", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("${summary.score} / ${summary.total} correct", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))

        AccuracyRing(summary.accuracy)
        Spacer(Modifier.height(32.dp))

        if (summary.correctWords.isNotEmpty()) {
            SectionCard(title = "Correct") {
                summary.correctWords.forEach { word ->
                    Text(
                        word,
                        color = Teal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (mistakePatterns.isNotEmpty() || unclassifiedMistakes.isNotEmpty()) {
            SectionCard(title = "Mistakes") {
                mistakePatterns.forEachIndexed { index, (result, wrongWords) ->
                    val label = patternLabels[result.pattern] ?: result.pattern
                    Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    wrongWords.forEach { wrong ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(wrong.userAnswer.ifBlank { "(empty)" }, color = Rose, fontSize = 15.sp)
                            Text("→", color = TextSecondary, fontSize = 13.sp)
                            Text(wrong.correct, color = Teal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    if (index < mistakePatterns.size - 1 || unclassifiedMistakes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Divider(color = NavyLight, thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                if (unclassifiedMistakes.isNotEmpty()) {
                    Text("Other", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    unclassifiedMistakes.forEach { wrong ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(wrong.userAnswer.ifBlank { "(empty)" }, color = Rose, fontSize = 15.sp)
                            Text("→", color = TextSecondary, fontSize = 13.sp)
                            Text(wrong.correct, color = Teal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (summary.wrongWords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Teal.copy(alpha = 0.08f))
                    .border(1.dp, Teal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Perfect score! No mistakes.", color = Teal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber)) {
            Text("Play Again", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)) {
            Text("Back to Home", color = TextPrimary, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AccuracyRing(accuracy: Int) {
    val fraction  = accuracy / 100f
    val ringColor = when {
        accuracy >= 80 -> Teal
        accuracy >= 50 -> Amber
        else           -> Rose
    }
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(color = NavyLight, startAngle = -90f, sweepAngle = 360f,
                useCenter = false, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * fraction,
                useCenter = false, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$accuracy%", color = ringColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("accuracy", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(NavyMid).border(1.dp, NavyLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(title, color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        content()
    }
}