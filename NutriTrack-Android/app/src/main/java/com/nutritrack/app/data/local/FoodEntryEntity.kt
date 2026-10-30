package com.nutritrack.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "food_entries",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mealId"])]
)
data class FoodEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val mealId: String,
    val name: String,
    val brand: String? = null,
    val servingSizeGrams: Double = 100.0,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double? = null,
    val barcode: String? = null,
    val imageUrl: String? = null,
    val isAIGenerated: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
