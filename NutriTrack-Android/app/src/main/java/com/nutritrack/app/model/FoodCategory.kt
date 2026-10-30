package com.nutritrack.app.model

enum class FoodCategory(val displayName: String) {
    ALL("Wszystkie"),
    DAIRY("Nabiał"),
    MEAT_FISH("Mięso i Ryby"),
    BAKERY_GRAINS("Pieczywo i Zboża"),
    FRUIT_VEG("Owoce i Warzywa"),
    FITNESS("Fit i Białko"),
    DRINKS("Napoje"),
    SNACKS_SWEETS("Przekąski i Słodycze");

    companion object {
        fun fromDisplayName(name: String?): FoodCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: ALL
        }
    }
}
