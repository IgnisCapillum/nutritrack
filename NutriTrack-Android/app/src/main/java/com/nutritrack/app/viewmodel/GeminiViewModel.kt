package com.nutritrack.app.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutritrack.app.data.repository.NutritionRepository
import com.nutritrack.app.model.ChatMessage
import com.nutritrack.app.model.GeminiAnalysisResult
import com.nutritrack.app.model.GeminiFoodItem
import com.nutritrack.app.model.MealType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class GeminiViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<GeminiAnalysisResult?>(null)
    val analysisResult: StateFlow<GeminiAnalysisResult?> = _analysisResult.asStateFlow()

    private val _analyzedImage = MutableStateFlow<Bitmap?>(null)
    val analyzedImage: StateFlow<Bitmap?> = _analyzedImage.asStateFlow()

    private val _targetMealType = MutableStateFlow(MealType.LUNCH)
    val targetMealType: StateFlow<MealType> = _targetMealType.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Chat state ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Cześć! Jestem Twoim asystentem żywieniowym Gemini AI. 🥗\n\nMożesz **napisać** co zjadłeś, **zrobić zdjęcie** swojego talerza lub wybrać jedną z gotowych propozycji poniżej. Przeanalizuję składniki, oszacuję wagę i dokładnie policzę makroskładniki!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    val quickSuggestions = listOf(
        "🍳 Jajecznica z 3 jaj na maśle ze szczypiorkiem i chlebem",
        "🍗 Pierś z kurczaka 180g, ryż basmati i warzywa",
        "🍓 Owsianka 60g z bananem i skyrem waniliowym",
        "🥪 2 kromki chleba żytniego z twarogiem i łososiem"
    )

    fun setTargetMealType(type: MealType) {
        _targetMealType.value = type
    }

    fun analyzeMealPhoto(bitmap: Bitmap, prompt: String? = null) {
        _isAnalyzing.value = true
        _analyzedImage.value = bitmap
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.analyzeMealWithGemini(prompt, bitmap)
            result.onSuccess { analysis ->
                _analysisResult.value = analysis
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Błąd analizy zdjęcia"
            }
            _isAnalyzing.value = false
        }
    }

    fun updateIngredientWeight(index: Int, newWeightGrams: Double) {
        val current = _analysisResult.value ?: return
        val items = current.items.toMutableList()
        if (index in items.indices) {
            val old = items[index]
            val ratio = if (old.estimatedWeightGrams > 0) newWeightGrams / old.estimatedWeightGrams else 1.0
            val updated = old.copy(
                estimatedWeightGrams = newWeightGrams,
                calories = (old.calories * ratio).coerceAtLeast(0.0),
                protein = (old.protein * ratio).coerceAtLeast(0.0),
                carbs = (old.carbs * ratio).coerceAtLeast(0.0),
                fat = (old.fat * ratio).coerceAtLeast(0.0)
            )
            items[index] = updated
            val newResult = current.copy(items = items)
            newResult.recalculateTotals()
            _analysisResult.value = newResult
        }
    }

    fun removeIngredient(index: Int) {
        val current = _analysisResult.value ?: return
        val items = current.items.toMutableList()
        if (index in items.indices) {
            items.removeAt(index)
            val newResult = current.copy(items = items)
            newResult.recalculateTotals()
            _analysisResult.value = newResult
        }
    }

    fun saveAnalysisToLog(date: Date, onSaved: () -> Unit) {
        val result = _analysisResult.value ?: return
        viewModelScope.launch {
            for (item in result.items) {
                repository.addFoodItemToMeal(
                    date = date,
                    mealType = _targetMealType.value,
                    name = item.name,
                    brand = "AI Gemini",
                    servingSizeGrams = item.estimatedWeightGrams,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    fiber = null,
                    isAIGenerated = true
                )
            }
            _analysisResult.value = null
            _analyzedImage.value = null
            onSaved()
        }
    }

    fun sendChatMessage(text: String, image: Bitmap?, targetMeal: MealType) {
        if (text.isBlank() && image == null) return

        val userMessage = ChatMessage(
            isUser = true,
            text = text.ifBlank { null },
            image = image
        )
        _chatMessages.value = _chatMessages.value + userMessage
        _isAnalyzing.value = true

        viewModelScope.launch {
            val result = repository.analyzeMealWithGemini(text.ifBlank { null }, image)
            result.onSuccess { analysis ->
                val assistantMessage = ChatMessage(
                    isUser = false,
                    text = "Oto podsumowanie makroskładników dla: **${analysis.mealName}**.\n${analysis.description}",
                    analysisResult = analysis
                )
                _chatMessages.value = _chatMessages.value + assistantMessage
            }.onFailure { err ->
                val assistantMessage = ChatMessage(
                    isUser = false,
                    text = "Przepraszam, wystąpił problem podczas analizy: ${err.localizedMessage}. Możesz spróbować ponownie lub sprawdzić swój klucz API w Ustawieniach."
                )
                _chatMessages.value = _chatMessages.value + assistantMessage
            }
            _isAnalyzing.value = false
        }
    }

    fun saveChatMessageMealToLog(messageId: String, date: Date, mealType: MealType) {
        val list = _chatMessages.value.toMutableList()
        val idx = list.indexOfFirst { it.id == messageId }
        if (idx != -1) {
            val msg = list[idx]
            val analysis = msg.analysisResult ?: return
            viewModelScope.launch {
                for (item in analysis.items) {
                    repository.addFoodItemToMeal(
                        date = date,
                        mealType = mealType,
                        name = item.name,
                        brand = "AI Gemini",
                        servingSizeGrams = item.estimatedWeightGrams,
                        calories = item.calories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        isAIGenerated = true
                    )
                }
                list[idx] = msg.copy(isSavedToLog = true)
                _chatMessages.value = list
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GeminiViewModel(repository) as T
        }
    }
}
