package com.nutritrack.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NutriDao {

    // --- Daily Log & Meals ---

    @Transaction
    @Query("SELECT * FROM daily_logs WHERE id = :dateId LIMIT 1")
    fun getDailyLogWithMealsFlow(dateId: String): Flow<DailyLogWithMeals?>

    @Transaction
    @Query("SELECT * FROM daily_logs WHERE id = :dateId LIMIT 1")
    suspend fun getDailyLogWithMeals(dateId: String): DailyLogWithMeals?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyLog(log: DailyLogEntity): Long

    @Update
    suspend fun updateDailyLog(log: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Query("SELECT * FROM meals WHERE dailyLogId = :dailyLogId AND mealTypeRaw = :mealTypeRaw LIMIT 1")
    suspend fun findMeal(dailyLogId: String, mealTypeRaw: String): MealEntity?

    @Query("SELECT * FROM meals WHERE dailyLogId = :dailyLogId ORDER BY sortOrder ASC")
    suspend fun getMealsForDay(dailyLogId: String): List<MealEntity>

    // --- Food Entries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntry(entry: FoodEntryEntity): Long

    @Delete
    suspend fun deleteFoodEntry(entry: FoodEntryEntity)

    @Query("DELETE FROM food_entries WHERE id = :entryId")
    suspend fun deleteFoodEntryById(entryId: String)

    @Query("SELECT * FROM food_entries WHERE barcode = :barcode LIMIT 1")
    suspend fun findRecentEntryByBarcode(barcode: String): FoodEntryEntity?

    // --- User Goals ---

    @Query("SELECT * FROM user_goals WHERE id = :id LIMIT 1")
    fun getUserGoalsFlow(id: String = UserGoalsEntity.DEFAULT_ID): Flow<UserGoalsEntity?>

    @Query("SELECT * FROM user_goals WHERE id = :id LIMIT 1")
    suspend fun getUserGoals(id: String = UserGoalsEntity.DEFAULT_ID): UserGoalsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserGoals(goals: UserGoalsEntity)
}
