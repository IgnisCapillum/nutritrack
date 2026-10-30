package com.nutritrack.app.model

enum class MealType(
    val rawValue: String,
    val titlePL: String,
    val defaultOrder: Int
) {
    BREAKFAST("breakfast", "Śniadanie", 0),
    SECOND_BREAKFAST("second_breakfast", "II Śniadanie", 1),
    LUNCH("lunch", "Obiad", 2),
    DINNER("dinner", "Kolacja", 3),
    SNACKS("snacks", "Przekąski", 4);

    companion object {
        fun fromRaw(raw: String): MealType {
            return entries.firstOrNull { it.rawValue == raw } ?: BREAKFAST
        }
    }
}
