package com.doman.wordsteps.ui.timed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doman.wordsteps.data.database.TimedScore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val NavyMid       = Color(0xFF142338)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimedScreen(
    viewModel: TimedViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Navy,
        topBar = {
            TopAppBar(
                title = { Text("Timed Blitz", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is TimedUiState.Loading        -> LoadingContent()
                is TimedUiState.DurationPicker -> DurationPickerContent(s) { viewModel.startGame(it) }
                is TimedUiState.Playing        -> PlayingContent(s) { viewModel.onAnswerSelected(it) }
                is TimedUiState.Finished       -> FinishedContent(
                    state       = s,
                    onPlayAgain = { viewModel.restartGame() },
                    onHome      = onNavigateBack
                )
            }
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────
@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(color = Amber)
    }
}

// ── Duration Picker ───────────────────────────────────────────────────────────
@Composable
private fun DurationPickerContent(
    state: TimedUiState.DurationPicker,
    onSelect: (Int) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⏱", fontSize = 56.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("Timed Blitz", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose the correct spelling as fast as you can.\n1 point per correct answer.",
            color = TextSecondary, fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(40.dp))
        Text(
            "CHOOSE GAME LENGTH",
            color = TextSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(16.dp))
        state.options.forEach { seconds ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavyMid)
                    .border(1.dp, NavyLight, RoundedCornerShape(16.dp))
                    .clickable { onSelect(seconds) }
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${seconds}s",
                        color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                    )
                    Text(
                        when (seconds) {
                            30   -> "Quick sprint"
                            60   -> "Standard"
                            else -> "Marathon"
                        },
                        color = TextSecondary, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ── Playing ───────────────────────────────────────────────────────────────────
@Composable
private fun PlayingContent(
    state: TimedUiState.Playing,
    onAnswerSelected: (Int) -> Unit
) {
    val timerProgress = state.timeRemaining.toFloat() / state.totalDuration.toFloat()
    val timerColor = when {
        timerProgress > 0.5f  -> Teal
        timerProgress > 0.25f -> Amber
        else                  -> Rose
    }
    val accuracy = if (state.wordsAnswered > 0)
        (state.score * 100 / state.wordsAnswered) else 0

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Timer bar ─────────────────────────────────────────────────────────
        LinearProgressIndicator(
            progress   = timerProgress,
            modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color      = timerColor,
            trackColor = NavyLight
        )
        Spacer(Modifier.height(10.dp))

        // ── HUD ───────────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.timeRemaining}s",
                color = timerColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Amber.copy(alpha = 0.15f))
                    .border(1.dp, Amber.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    "${state.score} pts",
                    color = Amber, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                )
            }
            Text(
                "$accuracy% acc",
                color = TextSecondary, fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(36.dp))

        // ── Question prompt ───────────────────────────────────────────────────
        Text(
            "Which spelling is correct?",
            color = TextSecondary, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(32.dp))

        // ── Options ───────────────────────────────────────────────────────────
        state.question.options.forEachIndexed { index, option ->
            val flash = state.flash

            val targetBg = when {
                flash == null                  -> NavyMid
                index == flash.correctIndex    -> Teal.copy(alpha = 0.2f)
                index == flash.selectedIndex   -> Rose.copy(alpha = 0.2f)
                else                           -> NavyMid
            }
            val targetBorder = when {
                flash == null                  -> NavyLight
                index == flash.correctIndex    -> Teal
                index == flash.selectedIndex   -> Rose
                else                           -> NavyLight
            }
            val targetText = when {
                flash == null                  -> TextPrimary
                index == flash.correctIndex    -> Teal
                index == flash.selectedIndex   -> Rose
                else                           -> TextSecondary
            }

            val bgColor     by animateColorAsState(targetBg,     tween(200), label = "bg$index")
            val borderColor by animateColorAsState(targetBorder, tween(200), label = "bd$index")
            val textColor   by animateColorAsState(targetText,   tween(200), label = "tx$index")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                    .then(
                        if (flash == null)
                            Modifier.clickable { onAnswerSelected(index) }
                        else Modifier
                    )
                    .padding(vertical = 18.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Finished ──────────────────────────────────────────────────────────────────
@Composable
private fun FinishedContent(
    state: TimedUiState.Finished,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val accuracy   = if (state.wordsAnswered > 0) (state.score * 100 / state.wordsAnswered) else 0
    val medals     = listOf("🥇", "🥈", "🥉")
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏱", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text("Time's Up!", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("${state.totalDuration}s game", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))

        // ── Score card ────────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NavyMid)
                .border(1.dp, NavyLight, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Correct answers", color = TextSecondary, fontSize = 14.sp)
                Text(
                    "${state.score}",
                    color = Teal, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                )
            }
            Divider(color = NavyLight)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Total answered", color = TextSecondary, fontSize = 13.sp)
                Text("${state.wordsAnswered}", color = TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Accuracy", color = TextSecondary, fontSize = 13.sp)
                Text("$accuracy%", color = Amber,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Top 3 leaderboard ─────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3D2A00), NavyMid)))
                .border(1.dp, Amber.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text("TOP SCORES", color = TextSecondary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text("Best 3", color = Amber, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            if (state.topScores.isEmpty()) {
                Text("No scores yet.", color = TextSecondary, fontSize = 13.sp)
            } else {
                state.topScores.forEachIndexed { index, entry ->
                    val isNew = entry.score == state.score &&
                            (System.currentTimeMillis() - entry.timestamp) < 15_000L
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        Arrangement.spacedBy(12.dp),
                        Alignment.CenterVertically
                    ) {
                        Text(medals.getOrElse(index) { "·" }, fontSize = 20.sp)
                        Column(Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${entry.score} pts",
                                    color = if (isNew) Amber else TextPrimary,
                                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                                )
                                if (isNew) {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Amber.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("NEW", color = Amber, fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            Text(
                                "${entry.wordsAnswered} answered  •  ${entry.duration}s  •  ${dateFormat.format(Date(entry.timestamp))}",
                                color = TextSecondary, fontSize = 11.sp
                            )
                        }
                    }
                    if (index < state.topScores.size - 1)
                        Divider(color = NavyLight.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick  = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Amber)
        ) {
            Text("Play Again", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick  = onHome,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Text("Back to Home", color = TextPrimary, fontSize = 16.sp)
        }
    }
}