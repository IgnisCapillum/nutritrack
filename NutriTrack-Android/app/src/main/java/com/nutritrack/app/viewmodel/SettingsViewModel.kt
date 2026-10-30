package com.nutritrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutritrack.app.data.local.UserGoalsEntity
import com.nutritrack.app.data.repository.NutritionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    val userGoals: StateFlow<UserGoalsEntity?> = repository.getUserGoalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserGoalsEntity())

    private val _isSavedMessageVisible = MutableStateFlow(false)
    val isSavedMessageVisible: StateFlow<Boolean> = _isSavedMessageVisible.asStateFlow()

    fun updateGoals(
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        waterMl: Double,
        apiKey: String
    ) {
        viewModelScope.launch {
            val updated = UserGoalsEntity(
                id = UserGoalsEntity.DEFAULT_ID,
                calorieGoal = calories.coerceAtLeast(500.0),
                proteinGoalGrams = protein.coerceAtLeast(0.0),
                carbsGoalGrams = carbs.coerceAtLeast(0.0),
                fatGoalGrams = fat.coerceAtLeast(0.0),
                waterGoalMl = waterMl.coerceAtLeast(0.0),
                geminiApiKey = apiKey.trim()
            )
            repository.saveUserGoals(updated)
            _isSavedMessageVisible.value = true
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.saveUserGoals(UserGoalsEntity())
        }
    }

    fun dismissSavedMessage() {
        _isSavedMessageVisible.value = false
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
