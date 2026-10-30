package com.nutritrack.app.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class GeminiFoodItem(
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("estimated_weight_g") var estimatedWeightGrams: Double = 100.0,
    @SerializedName("calories") var calories: Double = 0.0,
    @SerializedName("protein_g") var protein: Double = 0.0,
    @SerializedName("carbs_g") var carbs: Double = 0.0,
    @SerializedName("fat_g") var fat: Double = 0.0,
    @SerializedName("confidence") val confidence: String? = null
)

data class GeminiAnalysisResult(
    @SerializedName("meal_name") val mealName: String,
    @SerializedName("description") val description: String,
    @SerializedName("total_calories") var totalCalories: Double,
    @SerializedName("total_protein_g") var totalProtein: Double,
    @SerializedName("total_carbs_g") var totalCarbs: Double,
    @SerializedName("total_fat_g") var totalFat: Double,
    @SerializedName("items") var items: List<GeminiFoodItem> = emptyList()
) {
    fun recalculateTotals() {
        totalCalories = items.sumOf { it.calories }
        totalProtein = items.sumOf { it.protein }
        totalCarbs = items.sumOf { it.carbs }
        totalFat = items.sumOf { it.fat }
    }
}
