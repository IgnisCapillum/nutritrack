package com.nutritrack.app.data.repository

import android.graphics.Bitmap
import com.nutritrack.app.data.api.GeminiNutritionService
import com.nutritrack.app.data.api.OpenFoodFactsService
import com.nutritrack.app.data.local.DailyLogEntity
import com.nutritrack.app.data.local.DailyLogWithMeals
import com.nutritrack.app.data.local.FoodEntryEntity
import com.nutritrack.app.data.local.MealEntity
import com.nutritrack.app.data.local.NutriDao
import com.nutritrack.app.data.local.UserGoalsEntity
import com.nutritrack.app.model.FoodCategory
import com.nutritrack.app.model.FoodProduct
import com.nutritrack.app.model.GeminiAnalysisResult
import com.nutritrack.app.model.MealType
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NutritionRepository(
    private val dao: NutriDao,
    val openFoodFactsService: OpenFoodFactsService = OpenFoodFactsService(),
    val geminiNutritionService: GeminiNutritionService = GeminiNutritionService()
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatDateId(date: Date): String {
        return dateFormat.format(date)
    }

    fun getDailyLogFlow(date: Date): Flow<DailyLogWithMeals?> {
        val dateId = formatDateId(date)
        return dao.getDailyLogWithMealsFlow(dateId)
    }

    suspend fun getOrCreateDailyLog(date: Date): DailyLogWithMeals {
        val dateId = formatDateId(date)
        val existing = dao.getDailyLogWithMeals(dateId)
        if (existing != null && existing.meals.size >= MealType.entries.size) {
            return existing
        }

        // Insert log if not exists
        dao.insertDailyLog(
            DailyLogEntity(
                id = dateId,
                dateEpochMillis = date.time
            )
        )

        // Ensure all meal types are created
        for (type in MealType.entries) {
            val existingMeal = dao.findMeal(dateId, type.rawValue)
            if (existingMeal == null) {
                dao.insertMeal(
                    MealEntity(
                        id = UUID.randomUUID().toString(),
                        dailyLogId = dateId,
                        mealTypeRaw = type.rawValue,
                        sortOrder = type.defaultOrder
                    )
                )
            }
        }

        return dao.getDailyLogWithMeals(dateId)!!
    }

    suspend fun addFoodItemToMeal(
        date: Date,
        mealType: MealType,
        name: String,
        brand: String? = null,
        servingSizeGrams: Double,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double? = null,
        barcode: String? = null,
        imageUrl: String? = null,
        isAIGenerated: Boolean = false
    ) {
        val dailyLog = getOrCreateDailyLog(date)
        val targetMeal = dailyLog.meals.firstOrNull { it.mealType == mealType }
            ?: throw IllegalStateException("Meal $mealType not found")

        val entry = FoodEntryEntity(
            mealId = targetMeal.meal.id,
            name = name,
            brand = brand,
            servingSizeGrams = servingSizeGrams,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            barcode = barcode,
            imageUrl = imageUrl,
            isAIGenerated = isAIGenerated
        )
        dao.insertFoodEntry(entry)
    }

    suspend fun deleteFoodEntry(entryId: String) {
        dao.deleteFoodEntryById(entryId)
    }

    fun getUserGoalsFlow(): Flow<UserGoalsEntity?> {
        return dao.getUserGoalsFlow()
    }

    suspend fun getUserGoals(): UserGoalsEntity {
        val existing = dao.getUserGoals()
        if (existing != null) return existing
        val defaultGoals = UserGoalsEntity()
        dao.saveUserGoals(defaultGoals)
        return defaultGoals
    }

    suspend fun saveUserGoals(goals: UserGoalsEntity) {
        dao.saveUserGoals(goals)
    }

    suspend fun searchFood(query: String, category: FoodCategory? = null): List<FoodProduct> {
        return openFoodFactsService.search(query, category)
    }

    suspend fun fetchProductByBarcode(barcode: String): FoodProduct? {
        // Also check if user previously entered this barcode locally
        val localCustom = dao.findRecentEntryByBarcode(barcode)
        if (localCustom != null) {
            val factor = 100.0 / localCustom.servingSizeGrams
            return FoodProduct(
                barcode = barcode,
                name = localCustom.name,
                brand = localCustom.brand,
                imageUrl = localCustom.imageUrl,
                caloriesPer100g = localCustom.calories * factor,
                proteinPer100g = localCustom.protein * factor,
                carbsPer100g = localCustom.carbs * factor,
                fatPer100g = localCustom.fat * factor,
                fiberPer100g = localCustom.fiber?.let { it * factor },
                servingSize = "${localCustom.servingSizeGrams.toInt()}g",
                servingSizeGrams = localCustom.servingSizeGrams,
                source = "Twoja baza"
            )
        }

        return openFoodFactsService.fetchByBarcode(barcode)
    }

    suspend fun analyzeMealWithGemini(
        textDescription: String?,
        image: Bitmap?
    ): Result<GeminiAnalysisResult> {
        val goals = getUserGoals()
        val apiKey = goals.geminiApiKey.trim()

        if (apiKey.isEmpty()) {
            // Return instant high-quality mock result for demonstration
            return Result.success(geminiNutritionService.mockAnalyze(textDescription))
        }

        return geminiNutritionService.analyzeMeal(textDescription, image, apiKey)
    }
}
