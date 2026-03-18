package com.doman.wordsteps.ui.patternhunt

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
import com.doman.wordsteps.ui.home.patternLabels

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
fun PatternHuntScreen(
    viewModel: PatternHuntViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Navy,
        topBar = {
            TopAppBar(
                title = { Text("Pattern Hunt", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                is PatternHuntUiState.Loading ->
                    LoadingContent()
                is PatternHuntUiState.FindWrong ->
                    FindWrongContent(s) { viewModel.selectWrongOption(it) }
                is PatternHuntUiState.FindWrongFeedback ->
                    FindWrongFeedbackContent(s) { viewModel.proceedToNamePattern() }
                is PatternHuntUiState.NamePattern ->
                    NamePatternContent(s) { viewModel.selectPattern(it) }
                is PatternHuntUiState.NamePatternFeedback ->
                    NamePatternFeedbackContent(s) { viewModel.nextQuestion() }
                is PatternHuntUiState.Finished ->
                    FinishedContent(
                        state       = s,
                        onPlayAgain = { viewModel.restartSession() },
                        onHome      = onNavigateBack
                    )
            }
        }
    }
}


@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(color = Purple)
    }
}


@Composable
private fun FindWrongContent(
    state: PatternHuntUiState.FindWrong,
    onSelect: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress   = state.questionNumber.toFloat() / state.totalQuestions,
            modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
            color      = Purple,
            trackColor = NavyLight
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("${state.questionNumber} / ${state.totalQuestions}",
                color = TextSecondary, fontSize = 12.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Purple.copy(alpha = 0.15f))
                    .border(1.dp, Purple.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text("${state.score} pts", color = Purple,
                    fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("🔍", fontSize = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "Find the MISSPELLED word",
            color      = TextPrimary,
            fontSize   = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center
        )
        Text(
            "One of these four words is spelled incorrectly",
            color     = TextSecondary,
            fontSize  = 13.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(32.dp))

        state.question.options.forEach { option ->
            OptionTile(text = option, color = Purple, onClick = { onSelect(option) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun FindWrongFeedbackContent(
    state: PatternHuntUiState.FindWrongFeedback,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.End) {
            Text(
                if (state.isCorrect) "10 pts" else "0 pts",
                color    = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            if (state.isCorrect) "✓" else "✗",
            fontSize = 56.sp,
            color    = if (state.isCorrect) Teal else Rose
        )

        Spacer(Modifier.height(12.dp))

        Text(
            if (state.isCorrect) "Correct!" else "Not quite",
            color      = if (state.isCorrect) Teal else Rose,
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavyMid)
                .border(1.dp, NavyLight, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.isCorrect) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("You've chosen", color = TextSecondary, fontSize = 13.sp)
                    Text(
                        state.selectedOption,
                        color      = Rose,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Misspelled word", color = TextSecondary, fontSize = 13.sp)
                    Text(state.question.wrongOption, color = Rose,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Correct spelling", color = TextSecondary, fontSize = 13.sp)
                    Text(state.question.correctWord, color = Teal,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("You selected", color = TextSecondary, fontSize = 13.sp)
                    Text(state.selectedOption, color = TextPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(
                "Now name the pattern →",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }
    }
}


@Composable
private fun NamePatternContent(
    state: PatternHuntUiState.NamePattern,
    onSelect: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress   = state.questionNumber.toFloat() / state.totalQuestions,
            modifier   = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
            color      = Amber,
            trackColor = NavyLight
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("${state.questionNumber} / ${state.totalQuestions}",
                color = TextSecondary, fontSize = 12.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Amber.copy(alpha = 0.15f))
                    .border(1.dp, Amber.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text("${state.score} pts", color = Amber,
                    fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("🧠", fontSize = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "Name the mistake pattern",
            color      = TextPrimary,
            fontSize   = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(NavyMid)
                .border(1.dp, Rose.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                state.chosenWrongWord,
                color      = Rose,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "20 pts for correct answer",
            color    = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(24.dp))

        state.patternOptions.forEach { pattern ->
            val label = patternLabels[pattern] ?: pattern
            OptionTile(text = label, color = Amber, onClick = { onSelect(pattern) })
            Spacer(Modifier.height(10.dp))
        }
    }
}


@Composable
private fun NamePatternFeedbackContent(
    state: PatternHuntUiState.NamePatternFeedback,
    onNext: () -> Unit
) {
    val correctLabel  = patternLabels[state.question.pattern] ?: state.question.pattern
    val selectedLabel = patternLabels[state.selectedPattern] ?: state.selectedPattern

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (state.isCorrect) "20 pts" else "0 pts",
                    color    = TextSecondary,
                    fontSize = 12.sp
                )
                if (state.isCorrect && state.gotSpeedBonus) {
                    Text(
                        "bonus: 10 pts",
                        color      = Amber,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            if (state.isCorrect) "✓" else "✗",
            fontSize = 56.sp,
            color    = if (state.isCorrect) Teal else Rose
        )

        Spacer(Modifier.height(12.dp))

        Text(
            if (state.isCorrect) "Correct!" else "Not quite",
            color      = if (state.isCorrect) Teal else Rose,
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavyMid)
                .border(1.dp, NavyLight, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.spacedBy(8.dp),
                Alignment.CenterVertically
            ) {
                Text(
                    state.question.wrongOption,
                    color      = Rose,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
                Text("→", color = TextSecondary, fontSize = 13.sp)
                Text(
                    state.question.correctWord,
                    color      = Teal,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp
                )
            }

            Divider(color = NavyLight, thickness = 0.5.dp)

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Correct pattern", color = TextSecondary, fontSize = 13.sp)
                Text(correctLabel, color = Teal,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (!state.isCorrect) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Your answer", color = TextSecondary, fontSize = 13.sp)
                    Text(selectedLabel, color = Rose,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Divider(color = NavyLight, thickness = 0.5.dp)

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Round score", color = TextSecondary, fontSize = 13.sp)
                Text("${state.roundPoints} pts", color = Purple,
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick  = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(
                if (state.questionNumber == state.totalQuestions) "See Results" else "Next Word →",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }
    }
}


@Composable
private fun FinishedContent(
    state: PatternHuntUiState.Finished,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val summary = state.summary

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Session Complete", color = TextPrimary,
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("${summary.total} rounds", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1A0A3D), NavyMid)))
                .border(1.dp, Purple.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Final Score", color = TextSecondary, fontSize = 13.sp)
                Text("${summary.score} pts", color = Purple,
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            Divider(color = NavyLight.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Misspelled words found", color = TextSecondary, fontSize = 13.sp)
                Text("${summary.foundWrongCount} / ${summary.total}",
                    color = Teal, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Patterns correctly named", color = TextSecondary, fontSize = 13.sp)
                Text("${summary.namedPatternCount} / ${summary.total}",
                    color = Amber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        val mistakes = summary.wrongWords.filter { !it.foundWrong || !it.namedPattern }
        if (mistakes.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NavyMid)
                    .border(1.dp, NavyLight, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Text("REVIEW", color = TextSecondary, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                mistakes.forEach { mistake ->
                    val patternLabel = patternLabels[mistake.pattern] ?: mistake.pattern
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mistake.wrongOption, color = Rose,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("→", color = TextSecondary, fontSize = 12.sp)
                            Text(mistake.correctWord, color = Teal,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(patternLabel, color = TextSecondary, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    if (mistake != mistakes.last())
                        Divider(color = NavyLight, thickness = 0.5.dp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick  = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("Play Again", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

@Composable
private fun OptionTile(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavyMid)
            .border(1.dp, NavyLight, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary, fontSize = 17.sp,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}