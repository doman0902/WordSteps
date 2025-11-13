package com.example.androidapp.ui.screens

import android.os.Message
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.Api
import com.example.androidapp.data.QuizWordResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


sealed interface QuizUIState{
    data class Success(
        val quizData: QuizWordResponse,
        val selectedOption: String?=null,
        val isCorrect: Boolean?=null
    ): QuizUIState
    data object Loading: QuizUIState
    data class Error(val message: String): QuizUIState
    data object Idle: QuizUIState
}
class MainViewModel : ViewModel() {

    private val _uiState=MutableStateFlow<QuizUIState>(QuizUIState.Idle)

    val uiState=_uiState.asStateFlow()


    fun loadQuizWord(){
        viewModelScope.launch {
            try {
                _uiState.value= QuizUIState.Loading
                val quizResponse= Api.retrofitService.getQuizWord()
                _uiState.value= QuizUIState.Success(quizResponse)
            }catch (e: Exception){
                _uiState.value= QuizUIState.Error("Hiba a kvíz betöltésekor")
            }

        }
    }

    fun checkAnswer(selectedOption: String?){
        val currentState=_uiState.value
        if (currentState is QuizUIState.Success){
            //nem fut le újra ha már érkezett válasz
            if(currentState.isCorrect!=null) return

            val correctAnswer=currentState.quizData.correct
            val isAnswerCorrect=(selectedOption==correctAnswer)

            _uiState.value=currentState.copy(
                selectedOption=selectedOption,
                isCorrect = isAnswerCorrect
            )
        }
    }


}