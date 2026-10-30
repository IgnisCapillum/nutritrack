package com.nutritrack.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_goals")
data class UserGoalsEntity(
    @PrimaryKey val id: String = DEFAULT_ID,
    val calorieGoal: Double = 2200.0,
    val proteinGoalGrams: Double = 160.0,
    val carbsGoalGrams: Double = 220.0,
    val fatGoalGrams: Double = 70.0,
    val waterGoalMl: Double = 2500.0,
    val isPolishLanguage: Boolean = true,
    val geminiApiKey: String = ""
) {
    companion object {
        const val DEFAULT_ID = "primary_user_goals"
    }
}
