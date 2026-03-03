package com.example.wordsteps.ui.typing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy          = Color(0xFF0F1B2D)
private val NavyLight     = Color(0xFF1A2E4A)
private val Amber         = Color(0xFFFFC044)
private val Teal          = Color(0xFF00C9A7)
private val Rose          = Color(0xFFFF6B8A)
private val Purple        = Color(0xFF9D7FFF)
private val TextPrimary   = Color(0xFFF0F4FF)
private val TextSecondary = Color(0xFF8A9BB5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingScreen(
    viewModel: TypingViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Navy,
        topBar = {
            TopAppBar(
                title = { Text("Spelling Mode", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is TypingUiState.Setup    -> SetupScreen(onStart = { viewModel.startSession(it) })
                is TypingUiState.Loading  -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Amber)
                }
                is TypingUiState.Question -> QuestionScreen(
                    state    = s,
                    onSpeak  = { viewModel.speakCurrentWord() },
                    onInput  = { viewModel.onInputChanged(it) },
                    onSubmit = { viewModel.submitAnswer() }
                )
                is TypingUiState.Feedback -> FeedbackScreen(
                    state  = s,
                    onNext = { viewModel.nextQuestion() }
                )
                is TypingUiState.Finished -> FinishedScreen(
                    state     = s,
                    onRestart = { viewModel.restartSession() },
                    onBack    = onNavigateBack
                )
            }
        }
    }
}

// ── Setup: pick word count ────────────────────────────────────────────────────
@Composable
private fun SetupScreen(onStart: (Int) -> Unit) {
    var selected by remember { mutableStateOf(10) }
    val options = listOf(5, 10, 15, 20)

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Spelling Test", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "You will hear a word.\nType it correctly.",
            color = TextSecondary, fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(48.dp))
        Text("How many words?", color = TextSecondary, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { count ->
                val isSelected = count == selected
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Purple else NavyLight)
                        .border(1.dp, if (isSelected) Purple else NavyLight, RoundedCornerShape(16.dp))
                        .clickable { selected = count },
                    contentAlignment = Alignment.Center
                ) {
                    Text(count.toString(),
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { onStart(selected) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("Start", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── Question ──────────────────────────────────────────────────────────────────
@Composable
private fun QuestionScreen(
    state: TypingUiState.Question,
    onSpeak: () -> Unit,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress   = state.questionNumber.toFloat() / state.totalQuestions,
            modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
            color      = Purple,
            trackColor = NavyLight
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${state.questionNumber} / ${state.totalQuestions}", color = TextSecondary, fontSize = 12.sp)
            Text("Score: ${state.score}", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(56.dp))

        // Speaker button — greyed out until TTS is ready
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    if (state.isSpeaking)
                        Brush.radialGradient(listOf(Purple.copy(alpha = 0.3f), NavyLight))
                    else
                        Brush.radialGradient(listOf(NavyLight, NavyLight))
                )
                .border(
                    width = 2.dp,
                    color = when {
                        !state.ttsReady  -> TextSecondary.copy(alpha = 0.3f)
                        state.isSpeaking -> Purple
                        else             -> NavyLight
                    },
                    shape = CircleShape
                )
                .clickable(enabled = state.ttsReady && !state.isSpeaking) { onSpeak() },
            contentAlignment = Alignment.Center
        ) {
            if (!state.ttsReady) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = TextSecondary.copy(alpha = 0.5f),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Hear word",
                    tint = if (state.isSpeaking) Purple else TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            when {
                !state.ttsReady  -> "Preparing audio..."
                !state.hasSpoken -> "Tap to hear the word"
                else             -> "Tap to hear again"
            },
            color = when {
                !state.ttsReady  -> Amber
                !state.hasSpoken -> TextSecondary
                else             -> Purple
            },
            fontSize = 13.sp,
            fontWeight = if (state.hasSpoken && state.ttsReady) FontWeight.SemiBold else FontWeight.Normal
        )

        Spacer(Modifier.height(44.dp))

        AnimatedVisibility(visible = state.hasSpoken) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value           = state.userInput,
                    onValueChange   = onInput,
                    placeholder     = { Text("Type the word...", color = TextSecondary) },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction      = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboard?.hide()
                        if (state.userInput.isNotBlank()) onSubmit()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Purple,
                        unfocusedBorderColor = NavyLight,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = Purple
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick  = { keyboard?.hide(); onSubmit() },
                    enabled  = state.userInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("Submit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ── Feedback ──────────────────────────────────────────────────────────────────
@Composable
private fun FeedbackScreen(state: TypingUiState.Feedback, onNext: () -> Unit) {
    val color = if (state.isCorrect) Teal else Rose

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(2.dp, color.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (state.isCorrect) "✓" else "✗",
                fontSize = 36.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(if (state.isCorrect) "Correct!" else "Incorrect",
            color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavyLight)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Correct spelling", color = TextSecondary, fontSize = 13.sp)
                Text(state.correctWord, color = Teal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            if (!state.isCorrect) {
                Divider(color = Navy, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Your answer", color = TextSecondary, fontSize = 13.sp)
                    Text(state.userAnswer.ifBlank { "(empty)" },
                        color = Rose, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(36.dp))
        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(if (state.questionNumber == state.totalQuestions) "See Results" else "Next Word",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── Finished ──────────────────────────────────────────────────────────────────
@Composable
private fun FinishedScreen(
    state: TypingUiState.Finished,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Session Complete!", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Purple.copy(alpha = 0.18f), Navy)))
                .border(2.dp, Purple.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${state.accuracy}%", color = Purple, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("accuracy", color = TextSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("${state.score} / ${state.total} correct", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, NavyLight)) {
            Text("Back to Home", color = TextPrimary, fontSize = 16.sp)
        }
    }
}