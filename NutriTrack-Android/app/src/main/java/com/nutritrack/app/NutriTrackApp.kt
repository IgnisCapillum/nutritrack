package com.nutritrack.app

import android.app.Application
import com.nutritrack.app.data.local.NutriDatabase
import com.nutritrack.app.data.repository.NutritionRepository

class NutriTrackApp : Application() {

    lateinit var database: NutriDatabase
        private set

    lateinit var repository: NutritionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = NutriDatabase.getInstance(this)
        repository = NutritionRepository(database.nutriDao())
    }

    companion object {
        lateinit var instance: NutriTrackApp
            private set
    }
}
