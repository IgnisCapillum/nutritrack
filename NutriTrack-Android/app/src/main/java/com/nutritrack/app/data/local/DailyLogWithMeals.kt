package com.nutritrack.app.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.nutritrack.app.model.MealType

data class MealWithEntries(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val entries: List<FoodEntryEntity>
) {
    val mealType: MealType
        get() = MealType.fromRaw(meal.mealTypeRaw)

    val totalCalories: Double
        get() = entries.sumOf { it.calories }

    val totalProtein: Double
        get() = entries.sumOf { it.protein }

    val totalCarbs: Double
        get() = entries.sumOf { it.carbs }

    val totalFat: Double
        get() = entries.sumOf { it.fat }
}

data class DailyLogWithMeals(
    @Embedded val log: DailyLogEntity,
    @Relation(
        entity = MealEntity::class,
        parentColumn = "id",
        entityColumn = "dailyLogId"
    )
    val meals: List<MealWithEntries>
) {
    val totalCalories: Double
        get() = meals.sumOf { it.totalCalories }

    val totalProtein: Double
        get() = meals.sumOf { it.totalProtein }

    val totalCarbs: Double
        get() = meals.sumOf { it.totalCarbs }

    val totalFat: Double
        get() = meals.sumOf { it.totalFat }

    fun mealFor(type: MealType): MealWithEntries? {
        return meals.firstOrNull { it.meal.mealTypeRaw == type.rawValue }
    }
}
