package com.example.androidapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.Api
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _apiResponse = MutableStateFlow<String>("Teszt!")

    val apiResponse = _apiResponse.asStateFlow()

    
    fun testApiCall() {
        viewModelScope.launch {
            try {
                val response = Api.retrofitService.getTestMessage()

                _apiResponse.value = response.message

            } catch (e: Exception) {
                _apiResponse.value = "Hiba: ${e.message}"
            }
        }
    }
}