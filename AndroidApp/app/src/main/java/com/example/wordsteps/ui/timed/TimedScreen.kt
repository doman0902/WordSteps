package com.example.wordsteps.ui.timed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val NavyMid       = Color(0xFF142338)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val Purple        = Color(0xFF9D7FFF)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

private val BANK_TILE_SIZE = 44.dp
private val TILE_GAP       = 6.dp
private val BANK_TILE_FONT = 19.sp

private fun slotTileSize(screenWidthDp: Float, wordLength: Int, horizontalPadding: Float = 48f): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.TextUnit> {
    if (wordLength == 0) return Pair(44.dp, 19.sp)
    val available = screenWidthDp - horizontalPadding
    val rawSize = (available - (wordLength - 1) * TILE_GAP.value) / wordLength
    val tileSize = rawSize.coerceIn(20f, 44f)
    val fontSize = (tileSize * 0.42f).coerceIn(10f, 19f)
    return Pair(tileSize.dp, fontSize.sp)
}

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
                title = { Text("Timed Mode", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                is TimedUiState.Playing        -> PlayingContent(s, viewModel)
                is TimedUiState.Finished       -> FinishedContent(s,
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
        CircularProgressIndicator(color = Purple)
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
        Text("Timed Mode", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Spell as many words as you can before time runs out. 1 point per correct word.",
            color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        Text("Choose game length", color = TextSecondary, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(16.dp))

        state.options.forEach { seconds ->
            Button(
                onClick  = { onSelect(seconds) },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = NavyMid)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "${seconds}s",
                        color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
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
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Playing ───────────────────────────────────────────────────────────────────
@Composable
private fun PlayingContent(state: TimedUiState.Playing, viewModel: TimedViewModel) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
    val (slotSize, slotFont) = slotTileSize(screenWidth, state.word.length)

    // Determine flash overlay colours
    val isFlashing  = state.flashResult != null
    val flashColor  = when (state.flashResult) {
        WordResult.CORRECT -> Teal
        WordResult.WRONG   -> Rose
        null               -> Color.Transparent
    }
    val slotBorderColor = when (state.flashResult) {
        WordResult.CORRECT -> Teal
        WordResult.WRONG   -> Rose
        null               -> null   // use default per-slot colour
    }

    // Timer progress (0f..1f)
    val timerProgress = state.timeRemaining.toFloat() / state.totalDuration.toFloat()
    val timerColor = when {
        timerProgress > 0.5f -> Teal
        timerProgress > 0.25f -> Amber
        else                  -> Rose
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Timer bar ────────────────────────────────────────────────────────
        LinearProgressIndicator(
            progress    = timerProgress,
            modifier    = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
            color       = timerColor,
            trackColor  = NavyLight
        )
        Spacer(Modifier.height(10.dp))

        // ── HUD row ───────────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            // Countdown number
            Text(
                "${state.timeRemaining}s",
                color      = timerColor,
                fontSize   = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            // Score badge
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(Purple.copy(alpha = 0.15f))
                    .border(1.dp, Purple.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    "${state.score} pts",
                    color = Purple, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                )
            }
            // Words attempted
            Text(
                "${state.wordsAttempted} tried",
                color = TextSecondary, fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(28.dp))
        Text("Spell the word correctly", color = TextSecondary, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(3.dp))
        Text("Tap a placed tile to remove it",
            color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(24.dp))

        // ── Answer slots ──────────────────────────────────────────────────────
        val lastFilledIndex = (0 until state.word.length).reversed()
            .firstOrNull { state.slots[it]?.isLocked == false }

        Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
            (0 until state.word.length).forEach { index ->
                val tile     = state.slots[index]
                val isLocked = tile?.isLocked == true
                val isLast   = index == lastFilledIndex

                // During flash: override colours for all slots
                val bgColor = when {
                    isFlashing && state.flashResult == WordResult.CORRECT -> Teal.copy(alpha = 0.15f)
                    isFlashing && state.flashResult == WordResult.WRONG   -> Rose.copy(alpha = 0.15f)
                    isLocked     -> Purple.copy(alpha = 0.08f)
                    tile != null -> Purple.copy(alpha = 0.18f)
                    else         -> NavyMid
                }
                val borderColor = slotBorderColor ?: when {
                    isLocked     -> Purple.copy(alpha = 0.3f)
                    isLast       -> Purple
                    tile != null -> Purple.copy(alpha = 0.5f)
                    else         -> NavyLight
                }
                val textColor = when {
                    isFlashing && state.flashResult == WordResult.CORRECT -> Teal
                    isFlashing && state.flashResult == WordResult.WRONG   -> Rose
                    isLocked     -> TextSecondary
                    else         -> TextPrimary
                }

                Box(
                    modifier = Modifier
                        .size(slotSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                        .then(
                            if (tile != null && isLast && !isLocked && !isFlashing)
                                Modifier.clickable { viewModel.removePlacedTile(tile) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (tile != null) {
                        Text(
                            tile.char.uppercase(),
                            color      = textColor,
                            fontSize   = slotFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── "choose" divider ─────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Divider(Modifier.weight(1f), color = NavyLight)
            Text("choose", color = TextSecondary, fontSize = 11.sp)
            Divider(Modifier.weight(1f), color = NavyLight)
        }
        Spacer(Modifier.height(16.dp))

        // ── Bank tiles ────────────────────────────────────────────────────────
        TimedBankGrid(
            tiles     = state.bankTiles,
            enabled   = !isFlashing,
            onTap     = { viewModel.tapTile(it) }
        )
    }
}

// ── Bank grid — same as Reconstruction, 44dp tiles, wraps by available width ─
@Composable
private fun TimedBankGrid(
    tiles: List<TimedLetterTile>,
    enabled: Boolean,
    onTap: (TimedLetterTile) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
    val available   = screenWidth - 48f
    val tilesPerRow = maxOf(1, ((available + TILE_GAP.value) / (BANK_TILE_SIZE.value + TILE_GAP.value)).toInt())
    val rows = tiles.chunked(tilesPerRow)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TILE_GAP)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                row.forEach { tile ->
                    Box(
                        modifier = Modifier
                            .size(BANK_TILE_SIZE)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyLight)
                            .border(1.dp, Amber.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(10.dp))
                            .then(if (enabled) Modifier.clickable { onTap(tile) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tile.char.uppercase(),
                            color      = if (enabled) Amber else Amber.copy(alpha = 0.4f),
                            fontSize   = BANK_TILE_FONT,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
    val accuracy = if (state.wordsAttempted > 0)
        (state.score * 100 / state.wordsAttempted) else 0

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⏱", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text("Time's up!", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "${state.totalDuration}s game",
            color = TextSecondary, fontSize = 14.sp
        )
        Spacer(Modifier.height(32.dp))

        // Score card
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NavyMid)
                .border(1.dp, NavyLight, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Words correct", color = TextSecondary, fontSize = 14.sp)
                Text(
                    "${state.score}",
                    color = Teal, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
                )
            }
            Divider(color = NavyLight)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Words attempted", color = TextSecondary, fontSize = 13.sp)
                Text("${state.wordsAttempted}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Accuracy", color = TextSecondary, fontSize = 13.sp)
                Text("$accuracy%", color = Amber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("Play Again", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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