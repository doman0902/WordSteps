package com.example.wordsteps.ui.reconstruction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.wordsteps.data.database.ReconstructionScore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val NavyMid       = Color(0xFF142338)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val Purple        = Color(0xFF9D7FFF)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

private val TILE_SIZE     = 44.dp
private val TILE_GAP      = 6.dp
private val TILE_FONT     = 19.sp
private const val TILES_PER_ROW = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconstructionScreen(
    viewModel: ReconstructionViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Navy,
        topBar = {
            TopAppBar(
                title = { Text("Word Reconstruction", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                is ReconstructionUiState.Loading    -> LoadingContent()
                is ReconstructionUiState.Question   -> QuestionContent(s, viewModel)
                is ReconstructionUiState.WordFailed -> WordFailedContent(s) { viewModel.nextQuestion() }
                is ReconstructionUiState.Feedback   -> FeedbackContent(s) { viewModel.nextQuestion() }
                is ReconstructionUiState.Finished   -> FinishedContent(s,
                    onPlayAgain = { viewModel.restartSession() },
                    onHome      = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Purple) }
}

// ── Question ──────────────────────────────────────────────────────────────────
@Composable
private fun QuestionContent(state: ReconstructionUiState.Question, viewModel: ReconstructionViewModel) {
    val wordLen = state.wordToReconstruct.length

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress
        LinearProgressIndicator(
            progress   = state.questionNumber.toFloat() / state.totalQuestions,
            modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
            color      = Purple, trackColor = NavyLight
        )
        Spacer(Modifier.height(10.dp))

        // HUD
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("${state.questionNumber} / ${state.totalQuestions}", color = TextSecondary, fontSize = 12.sp)
            val comboColor = when {
                state.combo >= 3.0 -> Amber
                state.combo >= 2.0 -> Teal
                else               -> Purple
            }
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(comboColor.copy(alpha = 0.15f))
                    .border(1.dp, comboColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("×${"%.1f".format(state.combo)}", color = comboColor,
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(contentAlignment = Alignment.TopEnd) {
                Text("${state.totalPoints} pts", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                state.pointsJustEarned?.let {
                    Text("+$it", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = (-14).dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Spell the word correctly", color = TextSecondary, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(3.dp))
        Text("Highlighted letters are hints  •  wrong tap resets combo",
            color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        // ── Answer slots ──────────────────────────────────────────────────────
        SlotGrid(
            wordLength = wordLen,
            slots      = state.slots,
            onRemove   = { tile -> viewModel.removePlacedTile(tile) }
        )

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Divider(Modifier.weight(1f), color = NavyLight)
            Text("choose", color = TextSecondary, fontSize = 11.sp)
            Divider(Modifier.weight(1f), color = NavyLight)
        }
        Spacer(Modifier.height(16.dp))

        // ── Letter bank ───────────────────────────────────────────────────────
        BankGrid(tiles = state.scrambledTiles, onTap = { viewModel.tapTile(it) })

        Spacer(Modifier.height(24.dp))
    }
}

// ── Slot grid — renders word-length slots at their correct positions ──────────
@Composable
private fun SlotGrid(
    wordLength: Int,
    slots: Map<Int, LetterTile>,
    onRemove: (LetterTile) -> Unit
) {
    // The last filled non-locked slot index (tappable for backspace)
    val lastFilledIndex = (0 until wordLength).reversed()
        .firstOrNull { slots[it]?.isLocked == false }

    val rows = (0 until wordLength).chunked(TILES_PER_ROW)
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        rows.forEach { indices ->
            Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                indices.forEach { index ->
                    val tile     = slots[index]
                    val isLocked = tile?.isLocked == true
                    val isLast   = index == lastFilledIndex

                    val bgColor = when {
                        isLocked       -> Purple.copy(alpha = 0.08f)
                        tile != null   -> Purple.copy(alpha = 0.18f)
                        else           -> NavyMid
                    }
                    val borderColor = when {
                        isLocked       -> Purple.copy(alpha = 0.3f)
                        isLast         -> Purple
                        tile != null   -> Purple.copy(alpha = 0.5f)
                        else           -> NavyLight
                    }

                    Box(
                        modifier = Modifier
                            .size(TILE_SIZE)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                            .then(
                                if (tile != null && isLast && !isLocked)
                                    Modifier.clickable { onRemove(tile) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tile != null) {
                            Text(
                                tile.char.uppercase(),
                                color      = if (isLocked) TextSecondary else TextPrimary,
                                fontSize   = TILE_FONT,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Bank grid — same tile size, wraps at TILES_PER_ROW ───────────────────────
@Composable
private fun BankGrid(tiles: List<LetterTile>, onTap: (LetterTile) -> Unit) {
    val rows = tiles.chunked(TILES_PER_ROW)
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                row.forEach { tile -> BankTile(tile) { onTap(tile) } }
            }
        }
    }
}

@Composable
private fun BankTile(tile: LetterTile, onTap: () -> Unit) {
    val bgColor     by animateColorAsState(if (tile.isFlashingWrong) Rose.copy(alpha = 0.3f) else NavyLight, tween(200), label = "bg")
    val borderColor by animateColorAsState(if (tile.isFlashingWrong) Rose else Amber.copy(alpha = 0.5f), tween(200), label = "bd")
    val textColor   by animateColorAsState(if (tile.isFlashingWrong) Rose else Amber, tween(200), label = "tx")
    Box(
        modifier = Modifier.size(TILE_SIZE).clip(RoundedCornerShape(10.dp))
            .background(bgColor).border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = !tile.isFlashingWrong) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(tile.char.uppercase(), color = textColor, fontSize = TILE_FONT, fontWeight = FontWeight.Bold)
    }
}

// ── Word Failed ───────────────────────────────────────────────────────────────
@Composable
private fun WordFailedContent(state: ReconstructionUiState.WordFailed, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("💔", fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text("Combo lost!", color = Rose, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text("The correct spelling was:", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Rose.copy(alpha = 0.1f))
            .border(1.dp, Rose.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 28.dp, vertical = 14.dp)) {
            Text(state.correctWord, color = TextPrimary, fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text("${state.questionNumber} / ${state.totalQuestions}  •  ${state.totalPoints} pts  •  ×${"%.1f".format(state.combo)} combo",
            color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(44.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Text(if (state.questionNumber == state.totalQuestions) "See Results" else "Next Word",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── Correct Feedback ──────────────────────────────────────────────────────────
@Composable
private fun FeedbackContent(state: ReconstructionUiState.Feedback, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(80.dp).clip(CircleShape).background(Teal.copy(alpha = 0.12f))
            .border(2.dp, Teal.copy(alpha = 0.4f), CircleShape), Alignment.Center) {
            Text("✓", fontSize = 36.sp, color = Teal, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Text("Correct!", color = Teal, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NavyMid)
            .border(1.dp, NavyLight, RoundedCornerShape(16.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Word", color = TextSecondary, fontSize = 13.sp)
                Text(state.correctWord, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Points earned", color = TextSecondary, fontSize = 13.sp)
                Text("+${state.pointsEarned}", color = Amber, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            if (state.gotPerfectBonus) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("✨ Perfect bonus", color = Amber, fontSize = 12.sp)
                    Text("+${Scoring.PERFECT_WORD_BONUS}", color = Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Divider(color = NavyLight, thickness = 0.5.dp)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Combo", color = TextSecondary, fontSize = 13.sp)
                Text("×${"%.1f".format(state.combo)}", color = Purple, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Total", color = TextSecondary, fontSize = 13.sp)
                Text("${state.totalPoints} pts", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("${state.questionNumber} / ${state.totalQuestions}", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(40.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Text(if (state.questionNumber == state.totalQuestions) "See Results" else "Next Word",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── End Screen ────────────────────────────────────────────────────────────────
@Composable
private fun FinishedContent(state: ReconstructionUiState.Finished, onPlayAgain: () -> Unit, onHome: () -> Unit) {
    val medals     = listOf("🥇", "🥈", "🥉")
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Navy)
        .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Session Complete", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("${state.summary.score} / ${state.summary.total} words", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A0A3D), NavyMid)))
            .border(1.dp, Purple.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Final Score", color = TextSecondary, fontSize = 13.sp)
                Text("${state.totalPoints} pts", color = Purple, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Divider(color = NavyLight.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Best Combo", color = TextSecondary, fontSize = 13.sp)
                Text("×${"%.1f".format(state.bestCombo)}", color = Amber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Accuracy", color = TextSecondary, fontSize = 13.sp)
                Text("${state.summary.accuracy}%", color = Teal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(NavyMid)
            .border(1.dp, NavyLight, RoundedCornerShape(20.dp)).padding(18.dp)) {
            Text("TOP ATTEMPTS", color = TextSecondary, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Spacer(Modifier.height(14.dp))
            if (state.topScores.isEmpty()) {
                Text("No previous scores.", color = TextSecondary, fontSize = 13.sp)
            } else {
                state.topScores.forEachIndexed { index, score ->
                    val isNew = score.totalPoints == state.totalPoints &&
                            (System.currentTimeMillis() - score.timestamp) < 15_000L
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                        Text(medals.getOrElse(index) { "·" }, fontSize = 20.sp)
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${score.totalPoints} pts",
                                    color = if (isNew) Amber else TextPrimary,
                                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                if (isNew) Box(Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(Amber.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("NEW", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Text("${score.wordsCompleted}/${score.totalWords} words  •  ×${"%.1f".format(score.bestCombo)} combo  •  ${dateFormat.format(Date(score.timestamp))}",
                                color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    if (index < state.topScores.size - 1) Divider(color = NavyLight.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Text("Play Again", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)) {
            Text("Back to Home", color = TextPrimary, fontSize = 16.sp)
        }
    }
}