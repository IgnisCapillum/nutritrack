package com.nutritrack.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nutritrack.app.data.repository.NutritionRepository
import com.nutritrack.app.model.FoodProduct
import com.nutritrack.app.model.MealType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BarcodeTestItem(val name: String, val barcode: String)

class ScannerViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _resolvedProduct = MutableStateFlow<FoodProduct?>(null)
    val resolvedProduct: StateFlow<FoodProduct?> = _resolvedProduct.asStateFlow()

    private val _notFoundBarcode = MutableStateFlow<String?>(null)
    val notFoundBarcode: StateFlow<String?> = _notFoundBarcode.asStateFlow()

    private val _targetMealType = MutableStateFlow(MealType.LUNCH)
    val targetMealType: StateFlow<MealType> = _targetMealType.asStateFlow()

    val testBarcodes = listOf(
        BarcodeTestItem("Twaróg wiejski Piątnica", "5900531004018"),
        BarcodeTestItem("Skyr Fruvita Biedronka", "5900820014797"),
        BarcodeTestItem("Chleb żytni Piekarnia", "5901234567890"),
        BarcodeTestItem("Kabanosy Exclusive Tarczyński", "5902583002345"),
        BarcodeTestItem("Ryż biały Lubella", "5900084010203")
    )

    fun setTargetMealType(type: MealType) {
        _targetMealType.value = type
    }

    fun onBarcodeDetected(barcode: String) {
        if (_isResolving.value || _resolvedProduct.value != null || _notFoundBarcode.value != null) {
            return
        }

        val clean = barcode.trim()
        if (clean.isEmpty()) return

        _isResolving.value = true
        viewModelScope.launch {
            try {
                val product = repository.fetchProductByBarcode(clean)
                if (product != null) {
                    _resolvedProduct.value = product
                } else {
                    _notFoundBarcode.value = clean
                }
            } catch (_: Exception) {
                _notFoundBarcode.value = clean
            } finally {
                _isResolving.value = false
            }
        }
    }

    fun reset() {
        _resolvedProduct.value = null
        _notFoundBarcode.value = null
        _isResolving.value = false
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(repository) as T
        }
    }
}
