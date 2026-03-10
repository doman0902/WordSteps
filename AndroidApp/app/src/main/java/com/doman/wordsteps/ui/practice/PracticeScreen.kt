package com.doman.wordsteps.ui.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doman.wordsteps.ui.summary.SessionSummaryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PracticeUiState.Loading  -> LoadingScreen()
                is PracticeUiState.Question -> QuestionScreen(state, viewModel)
                is PracticeUiState.Feedback -> FeedbackScreen(state, viewModel)
                is PracticeUiState.Finished -> SessionSummaryScreen(
                    summary     = state.summary,
                    onPlayAgain = { viewModel.restartQuiz() },
                    onHome      = { onNavigateBack() }
                )
            }
        }
    }
}

@Composable
fun QuestionScreen(state: PracticeUiState.Question, viewModel: PracticeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Question ${state.questionNumber}/${state.totalQuestions}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Score: ${state.currentScore}/${state.totalQuestions}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.currentStreak > 2) {
            Text(
                text = "🔥 Streak: ${state.currentStreak}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Which spelling is correct?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        state.question.options.forEachIndexed { index, option ->
            OptionButton(
                text = option,
                onClick = { viewModel.submitAnswer(index) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun FeedbackScreen(state: PracticeUiState.Feedback, viewModel: PracticeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (state.isCorrect) "✓" else "✗",
            fontSize = 120.sp,
            color = if (state.isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (state.isCorrect) "Correct!" else "Incorrect",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (!state.isCorrect) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Correct answer: ${state.correctAnswer}",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "You selected: ${state.userAnswer}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Score: ${state.currentScore}/${state.totalQuestions}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.nextQuestion() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Next Question →", fontSize = 18.sp)
        }
    }
}

@Composable
fun OptionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}