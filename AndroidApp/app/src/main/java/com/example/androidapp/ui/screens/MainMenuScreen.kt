package com.example.androidapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.savedstate.savedState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    navController: NavController,
    viewModel: MainViewModel
)
{
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("WordStep")
                }
            )
        }
    ) {padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center)
        {
            when (val state = uiState) {
                //LOADING
                is QuizUIState.Loading -> {
                    Text(text = "Szó betöltése...")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                //SIKER
                is QuizUIState.Success -> {
                    val quizData = state.quizData
                    val selectedOption = state.selectedOption
                    val isCorrect = state.isCorrect
                    Text(
                        text = "Válaszd ki a helyes írásmódot!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "CEFR Szint: ${quizData.cefr_level}")
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    quizData.options.forEach { option ->
                        val isButtonEnabled = (isCorrect == null)
                        val buttonColors = when {
                            isCorrect == null -> {
                                ButtonDefaults.buttonColors()
                            }
                            option == quizData.correct -> {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Green,
                                    disabledContainerColor = Color.Green
                                )
                            }
                            option == selectedOption && isCorrect == false -> {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    disabledContainerColor = Color.Red
                                )
                            }
                            else -> {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray,
                                    disabledContainerColor = Color.Gray
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.checkAnswer(option)
                            },
                            enabled = isButtonEnabled,
                            colors = buttonColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Text(text = option)
                        }
                    }
                    if (isCorrect != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.loadQuizWord()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Következő szó")
                        }
                    }
                }
                //HIBA
                is QuizUIState.Error -> {
                    val errorMessage = state.message
                    Text(text = "Hiba", color = Color.Red)
                    Text(text = errorMessage)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.loadQuizWord()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Próbáld újra betölteni")
                    }
                }
                //IDLE
                is QuizUIState.Idle -> {
                    Text(text = "Nyomj a gmobra az új szóért")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.loadQuizWord()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Start Kvíz")
                    }
                }
            }

        }
        }
}