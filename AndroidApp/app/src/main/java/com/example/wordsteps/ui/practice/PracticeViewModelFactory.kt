package com.example.wordsteps.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wordsteps.data.repository.SpellRepository

class PracticeViewModelFactory(
    private val repository: SpellRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PracticeViewModel(repository) as T
    }
}