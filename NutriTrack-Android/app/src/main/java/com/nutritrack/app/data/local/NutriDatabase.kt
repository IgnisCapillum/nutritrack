package com.nutritrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyLogEntity::class,
        MealEntity::class,
        FoodEntryEntity::class,
        UserGoalsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NutriDatabase : RoomDatabase() {
    abstract fun nutriDao(): NutriDao

    companion object {
        @Volatile
        private var INSTANCE: NutriDatabase? = null

        fun getInstance(context: Context): NutriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutriDatabase::class.java,
                    "nutritrack_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
