package com.nutritrack.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val id: String, // Format: "yyyy-MM-dd"
    val dateEpochMillis: Long,
    val waterMl: Double = 0.0,
    val note: String? = null
)
