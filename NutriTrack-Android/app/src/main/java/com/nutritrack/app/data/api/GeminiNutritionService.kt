package com.nutritrack.app.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nutritrack.app.model.GeminiAnalysisResult
import com.nutritrack.app.model.GeminiFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.min

class GeminiNutritionService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {
    private var cachedDiscoveredModels: List<String> = emptyList()
    private var activeModelName: String = "gemini-3.8-flash"

    private val fallbackCandidates = listOf(
        "gemini-3.8-flash",
        "gemini-3-flash-preview",
        "gemini-3-flash",
        "gemini-2.5-flash",
        "gemini-2.0-flash-exp",
        "gemini-2.0-flash",
        "gemini-1.5-flash"
    )

    suspend fun fetchAvailableModels(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) return@withContext emptyList()

        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", cleanKey)
            .header("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JsonParser.parseString(body).asJsonObject
                val modelsArr = root.getAsJsonArray("models") ?: return@withContext emptyList()

                val discovered = mutableListOf<String>()
                for (elem in modelsArr) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val name = obj.get("name")?.asString?.replace("models/", "") ?: continue
                        val methods = obj.getAsJsonArray("supportedGenerationMethods")
                        val supportsGenerate = methods?.any { it.asString == "generateContent" } ?: false
                        if (supportsGenerate) {
                            discovered.add(name)
                        }
                    }
                }
                cachedDiscoveredModels = discovered
                discovered
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun analyzeMeal(
        textDescription: String?,
        image: Bitmap?,
        apiKey: String
    ): Result<GeminiAnalysisResult> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Klucz API Gemini nie został skonfigurowany. Przejdź do Ustawień, aby dodać swój klucz Google AI Studio.")
            )
        }

        var base64Image: String? = null
        if (image != null) {
            base64Image = prepareImageData(image)
        }

        var modelsToTry = if (cachedDiscoveredModels.isEmpty()) {
            val fetched = fetchAvailableModels(cleanKey)
            if (fetched.isNotEmpty()) sortModelsByPriority(fetched) else emptyList()
        } else {
            sortModelsByPriority(cachedDiscoveredModels)
        }

        if (modelsToTry.isEmpty()) {
            modelsToTry = listOf(activeModelName) + fallbackCandidates.filter { it != activeModelName }
        }

        var lastError: Exception? = null

        for (model in modelsToTry) {
            val cleanModel = model.replace("models/", "")
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$cleanKey"

            val prompt = buildString {
                appendLine("Jesteś profesjonalnym dietetykiem i analitykiem żywienia w aplikacji NutriTrack.")
                appendLine("Przeanalizuj posiłek na podstawie dostarczonego zdjęcia i/lub opisu użytkownika.")
                appendLine("1. Zidentyfikuj poszczególne składniki dania.")
                appendLine("2. Oszacuj realistyczną wagę każdej porcji w gramach.")
                appendLine("3. Oblicz dokładne kalorie (kcal), białko (g), węglowodany (g) i tłuszcze (g) dla każdego składnika.")
                appendLine("4. Policz łączne sumaryczne wartości dla całego dania.")
                appendLine("5. Zwróć nazwy i opis w języku polskim.")
                appendLine("MUSISZ odpowiedzieć WYŁĄCZNIE poprawnym obiektem JSON (bez żadnego dodatkowego tekstu, bez znaczników ```json).")
                appendLine("Ścisły schemat JSON:")
                appendLine("{\"meal_name\": \"Nazwa dania\", \"description\": \"Krótki opis\", \"total_calories\": 500, \"total_protein_g\": 30.0, \"total_carbs_g\": 40.0, \"total_fat_g\": 20.0, \"items\": [{\"name\": \"Składnik\", \"estimated_weight_g\": 100, \"calories\": 150, \"protein_g\": 10.0, \"carbs_g\": 15.0, \"fat_g\": 5.0, \"confidence\": \"wysoka\"}]}")
                if (!textDescription.isNullOrBlank()) {
                    appendLine("\nInformacja od użytkownika: \"$textDescription\"")
                }
            }

            val payloadJson = buildRequestPayload(prompt, base64Image)
            val requestBody = payloadJson.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", cleanKey)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.code == 404) {
                        lastError = RuntimeException("Model $cleanModel nie został odnaleziony (404)")
                        return@use
                    }
                    if (!response.isSuccessful) {
                        lastError = RuntimeException("Błąd API Gemini (${response.code}): $body")
                        return@use
                    }

                    val parsed = parseGeminiResponse(body)
                    activeModelName = cleanModel
                    return@withContext Result.success(parsed)
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: RuntimeException("Nie udało się uzyskać odpowiedzi od AI"))
    }

    private fun buildRequestPayload(promptText: String, base64Image: String?): String {
        val root = JsonObject()
        val contents = com.google.gson.JsonArray()
        val contentObj = JsonObject()
        val parts = com.google.gson.JsonArray()

        val textPart = JsonObject()
        textPart.addProperty("text", promptText)
        parts.add(textPart)

        if (base64Image != null) {
            val imagePart = JsonObject()
            val inlineData = JsonObject()
            inlineData.addProperty("mime_type", "image/jpeg")
            inlineData.addProperty("data", base64Image)
            imagePart.add("inline_data", inlineData)
            parts.add(imagePart)
        }

        contentObj.add("parts", parts)
        contents.add(contentObj)
        root.add("contents", contents)

        val genConfig = JsonObject()
        genConfig.addProperty("temperature", 0.2)
        genConfig.addProperty("response_mime_type", "application/json")
        root.add("generationConfig", genConfig)

        return root.toString()
    }

    private fun parseGeminiResponse(jsonBody: String): GeminiAnalysisResult {
        val root = JsonParser.parseString(jsonBody).asJsonObject
        val candidates = root.getAsJsonArray("candidates") ?: throw IllegalArgumentException("Brak pola candidates w odpowiedzi")
        val firstCandidate = candidates.get(0).asJsonObject
        val content = firstCandidate.getAsJsonObject("content")
        val parts = content.getAsJsonArray("parts")
        val textPart = parts.get(0).asJsonObject.get("text").asString

        var cleaned = textPart.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        cleaned = cleaned.trim()

        return gson.fromJson(cleaned, GeminiAnalysisResult::class.java)
    }

    private fun prepareImageData(bitmap: Bitmap): String {
        val maxDimension = 1024
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun sortModelsByPriority(models: List<String>): List<String> {
        val flash3 = mutableListOf<String>()
        val flashGeneral = mutableListOf<String>()
        val pro3 = mutableListOf<String>()
        val others = mutableListOf<String>()

        for (m in models) {
            val lower = m.lowercase()
            if ((lower.contains("3") || lower.contains("3.8")) && lower.contains("flash")) {
                flash3.add(m)
            } else if (lower.contains("flash")) {
                flashGeneral.add(m)
            } else if (lower.contains("3") && lower.contains("pro")) {
                pro3.add(m)
            } else {
                others.add(m)
            }
        }
        return flash3 + flashGeneral + pro3 + others
    }

    fun mockAnalyze(query: String? = null, preset: Int = 0): GeminiAnalysisResult {
        val text = query?.lowercase().orEmpty()
        if (text.contains("jajk") || text.contains("jajecz") || text.contains("omlet")) {
            return GeminiAnalysisResult(
                mealName = "Jajecznica na maśle ze szczypiorkiem i chlebem",
                description = "Wysokobiałkowe śniadanie z pełnowartościowym białkiem i węglowodanami złożonymi.",
                totalCalories = 460.0,
                totalProtein = 24.5,
                totalCarbs = 34.0,
                totalFat = 28.0,
                items = listOf(
                    GeminiFoodItem(name = "Jajka kurze klasa L (3 sztuki)", estimatedWeightGrams = 180.0, calories = 257.0, protein = 22.6, carbs = 1.2, fat = 17.1, confidence = "98%"),
                    GeminiFoodItem(name = "Masło ekstra 82%", estimatedWeightGrams = 10.0, calories = 74.0, protein = 0.1, carbs = 0.1, fat = 8.2, confidence = "95%"),
                    GeminiFoodItem(name = "Chleb żytni na zakwasie", estimatedWeightGrams = 50.0, calories = 114.0, protein = 3.1, carbs = 22.5, fat = 0.7, confidence = "92%"),
                    GeminiFoodItem(name = "Pomidor ze szczypiorkiem", estimatedWeightGrams = 80.0, calories = 15.0, protein = 0.7, carbs = 3.1, fat = 0.2, confidence = "90%")
                )
            )
        } else if (text.contains("owoc") || text.contains("owsian") || text.contains("skyr")) {
            return GeminiAnalysisResult(
                mealName = "Owsianka ze skyrem waniliowym i owocami",
                description = "Zbilansowany posiłek bogaty w błonnik, antyoksydanty i białko.",
                totalCalories = 435.0,
                totalProtein = 29.0,
                totalCarbs = 64.0,
                totalFat = 6.5,
                items = listOf(
                    GeminiFoodItem(name = "Płatki owsiane górskie", estimatedWeightGrams = 50.0, calories = 188.0, protein = 6.0, carbs = 31.0, fat = 3.5, confidence = "97%"),
                    GeminiFoodItem(name = "Skyr waniliowy Fruvita", estimatedWeightGrams = 150.0, calories = 126.0, protein = 15.0, carbs = 16.5, fat = 0.0, confidence = "95%"),
                    GeminiFoodItem(name = "Borówki i banan", estimatedWeightGrams = 100.0, calories = 75.0, protein = 1.0, carbs = 17.0, fat = 0.2, confidence = "94%"),
                    GeminiFoodItem(name = "Nasiona chia", estimatedWeightGrams = 10.0, calories = 46.0, protein = 1.7, carbs = 4.2, fat = 3.1, confidence = "88%")
                )
            )
        }

        return when (preset % 3) {
            0 -> GeminiAnalysisResult(
                mealName = "Obiad fitness: Kurczak, ryż basmati i warzywa na parze",
                description = "Zbilansowany posiłek wysokobiałkowy ze złożonymi węglowodanami i mikroskładnikami.",
                totalCalories = 585.0,
                totalProtein = 48.0,
                totalCarbs = 64.0,
                totalFat = 9.5,
                items = listOf(
                    GeminiFoodItem(name = "Grillowana pierś z kurczaka", estimatedWeightGrams = 180.0, calories = 297.0, protein = 39.6, carbs = 0.0, fat = 5.4, confidence = "98%"),
                    GeminiFoodItem(name = "Ugotowany ryż basmati", estimatedWeightGrams = 180.0, calories = 234.0, protein = 4.8, carbs = 50.4, fat = 0.6, confidence = "95%"),
                    GeminiFoodItem(name = "Brokuły i marchewka na parze", estimatedWeightGrams = 150.0, calories = 54.0, protein = 3.6, carbs = 9.0, fat = 0.5, confidence = "92%"),
                    GeminiFoodItem(name = "Oliwa z oliwek (dresing)", estimatedWeightGrams = 5.0, calories = 44.0, protein = 0.0, carbs = 0.0, fat = 5.0, confidence = "85%")
                )
            )
            1 -> GeminiAnalysisResult(
                mealName = "Owsianka proteinowa z bananem i masłem orzechowym",
                description = "Pożywne śniadanie bogate w błonnik, białko i zdrowe tłuszcze.",
                totalCalories = 495.0,
                totalProtein = 28.5,
                totalCarbs = 62.0,
                totalFat = 14.0,
                items = listOf(
                    GeminiFoodItem(name = "Płatki owsiane górskie", estimatedWeightGrams = 60.0, calories = 226.0, protein = 7.1, carbs = 37.4, fat = 4.3, confidence = "97%"),
                    GeminiFoodItem(name = "Skyr naturalny", estimatedWeightGrams = 150.0, calories = 98.0, protein = 18.0, carbs = 6.0, fat = 0.0, confidence = "94%"),
                    GeminiFoodItem(name = "Banan krojony", estimatedWeightGrams = 80.0, calories = 71.0, protein = 0.9, carbs = 18.2, fat = 0.2, confidence = "96%"),
                    GeminiFoodItem(name = "Masło orzechowe 100%", estimatedWeightGrams = 15.0, calories = 90.0, protein = 4.2, carbs = 2.0, fat = 7.5, confidence = "89%")
                )
            )
            else -> GeminiAnalysisResult(
                mealName = "Kanapki z chlebem żytnim, twarogiem i łososiem",
                description = "Klasyczna kolacja z pełnoziarnistego chleba na zakwasie z białkiem i kwasami omega-3.",
                totalCalories = 430.0,
                totalProtein = 34.0,
                totalCarbs = 42.0,
                totalFat = 12.0,
                items = listOf(
                    GeminiFoodItem(name = "Chleb żytni na zakwasie (2 kromki)", estimatedWeightGrams = 80.0, calories = 182.0, protein = 5.0, carbs = 36.0, fat = 1.1, confidence = "95%"),
                    GeminiFoodItem(name = "Twaróg chudy Piątnica", estimatedWeightGrams = 100.0, calories = 86.0, protein = 18.0, carbs = 3.5, fat = 0.0, confidence = "96%"),
                    GeminiFoodItem(name = "Łosoś wędzony na zimno", estimatedWeightGrams = 50.0, calories = 88.0, protein = 10.0, carbs = 0.0, fat = 5.4, confidence = "93%"),
                    GeminiFoodItem(name = "Ogórek zielony i rzodkiewka", estimatedWeightGrams = 80.0, calories = 12.0, protein = 0.6, carbs = 2.5, fat = 0.1, confidence = "90%")
                )
            )
        }
    }
}
