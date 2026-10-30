package com.nutritrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutritrack.app.data.local.DailyLogWithMeals
import com.nutritrack.app.data.local.UserGoalsEntity
import com.nutritrack.app.data.repository.NutritionRepository
import com.nutritrack.app.model.MealType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    val dailyLog: StateFlow<DailyLogWithMeals?> = _selectedDate
        .flatMapLatest { date -> repository.getDailyLogFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userGoals: StateFlow<UserGoalsEntity?> = repository.getUserGoalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserGoalsEntity())

    init {
        ensureLogCreatedForDate(_selectedDate.value)
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
        ensureLogCreatedForDate(date)
    }

    private fun ensureLogCreatedForDate(date: Date) {
        viewModelScope.launch {
            repository.getOrCreateDailyLog(date)
        }
    }

    fun deleteFoodEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteFoodEntry(entryId)
        }
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository) as T
        }
    }
}
