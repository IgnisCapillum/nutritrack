package com.nutritrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutritrack.app.data.repository.NutritionRepository
import com.nutritrack.app.model.FoodCategory
import com.nutritrack.app.model.FoodProduct
import com.nutritrack.app.model.MealType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class SearchViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FoodCategory.ALL)
    val selectedCategory: StateFlow<FoodCategory> = _selectedCategory.asStateFlow()

    private val _results = MutableStateFlow<List<FoodProduct>>(emptyList())
    val results: StateFlow<List<FoodProduct>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Selected product for portion sheet
    private val _activeProduct = MutableStateFlow<FoodProduct?>(null)
    val activeProduct: StateFlow<FoodProduct?> = _activeProduct.asStateFlow()

    private val _portionGrams = MutableStateFlow(100.0)
    val portionGrams: StateFlow<Double> = _portionGrams.asStateFlow()

    private val _targetMealType = MutableStateFlow(MealType.LUNCH)
    val targetMealType: StateFlow<MealType> = _targetMealType.asStateFlow()

    private var searchJob: Job? = null

    init {
        performSearch("", FoodCategory.ALL)
    }

    fun setInitialMealType(mealType: MealType) {
        _targetMealType.value = mealType
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250) // debounce typing
            performSearch(newQuery, _selectedCategory.value)
        }
    }

    fun onCategorySelected(category: FoodCategory) {
        _selectedCategory.value = category
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            performSearch(_query.value, category)
        }
    }

    private suspend fun performSearch(q: String, cat: FoodCategory) {
        _isLoading.value = true
        try {
            val list = repository.searchFood(q, cat)
            _results.value = list
        } catch (_: Exception) {
            _results.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    fun selectProduct(product: FoodProduct) {
        _activeProduct.value = product
        _portionGrams.value = product.servingSizeGrams
    }

    fun setPortionGrams(grams: Double) {
        _portionGrams.value = grams.coerceAtLeast(1.0)
    }

    fun setTargetMealType(mealType: MealType) {
        _targetMealType.value = mealType
    }

    fun dismissProduct() {
        _activeProduct.value = null
    }

    fun addActiveProductToLog(date: Date, onComplete: () -> Unit) {
        val prod = _activeProduct.value ?: return
        val grams = _portionGrams.value
        val nutrition = prod.calculatedNutrition(grams)

        viewModelScope.launch {
            repository.addFoodItemToMeal(
                date = date,
                mealType = _targetMealType.value,
                name = prod.name,
                brand = prod.brand,
                servingSizeGrams = grams,
                calories = nutrition.calories,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fat = nutrition.fat,
                fiber = nutrition.fiber,
                barcode = prod.barcode.ifEmpty { null },
                imageUrl = prod.imageUrl
            )
            _activeProduct.value = null
            onComplete()
        }
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(repository) as T
        }
    }
}
