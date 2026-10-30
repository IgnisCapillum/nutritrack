package com.nutritrack.app.model

import java.util.UUID
import kotlin.math.pow
import kotlin.math.roundToInt

data class FoodProduct(
    val barcode: String = "",
    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double? = null,
    val servingSize: String? = null,
    val servingSizeGrams: Double = 100.0,
    val category: String? = null,
    var source: String? = null,
    val id: String = if (barcode.isNotEmpty()) barcode else UUID.randomUUID().toString()
) {
    fun calculatedNutrition(grams: Double): CalculatedNutrition {
        val factor = maxOf(0.0, grams) / 100.0
        return CalculatedNutrition(
            calories = (caloriesPer100g * factor).roundToOneDecimal(),
            protein = (proteinPer100g * factor).roundToOneDecimal(),
            carbs = (carbsPer100g * factor).roundToOneDecimal(),
            fat = (fatPer100g * factor).roundToOneDecimal(),
            fiber = fiberPer100g?.let { (it * factor).roundToOneDecimal() }
        )
    }
}

data class CalculatedNutrition(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double?
)

private fun Double.roundToOneDecimal(): Double {
    return (this * 10.0).roundToInt() / 10.0
}
