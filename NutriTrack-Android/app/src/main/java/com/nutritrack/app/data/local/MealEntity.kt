package com.nutritrack.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["dailyLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dailyLogId"])]
)
data class MealEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dailyLogId: String,
    val mealTypeRaw: String,
    val sortOrder: Int
)
