package com.nutritrack.app.data.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nutritrack.app.data.catalog.PolishFoodCatalog
import com.nutritrack.app.model.FoodCategory
import com.nutritrack.app.model.FoodProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class OpenFoodFactsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val barcodeCache = ConcurrentHashMap<String, FoodProduct>()
    private val searchCache = ConcurrentHashMap<String, List<FoodProduct>>()

    private val searchBaseUrl = "https://world.openfoodfacts.org/cgi/search.pl"
    private val productV2Url = "https://world.openfoodfacts.org/api/v2/product"
    private val productV0Url = "https://world.openfoodfacts.org/api/v0/product"

    suspend fun search(query: String, category: FoodCategory? = null): List<FoodProduct> =
        withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            val localMatches = PolishFoodCatalog.search(cleanQuery, category)
            if (cleanQuery.isEmpty()) {
                return@withContext localMatches
            }

            val results = mutableListOf<FoodProduct>()
            val seenBarcodes = mutableSetOf<String>()

            for (item in localMatches) {
                results.add(item.copy(source = "Baza lokalna"))
                if (item.barcode.isNotEmpty()) {
                    seenBarcodes.add(item.barcode)
                }
            }

            val cacheKey = cleanQuery.lowercase()
            searchCache[cacheKey]?.let { cached ->
                for (item in cached) {
                    if (item.barcode.isEmpty() || !seenBarcodes.contains(item.barcode)) {
                        results.add(item)
                        if (item.barcode.isNotEmpty()) seenBarcodes.add(item.barcode)
                    }
                }
                return@withContext results
            }

            val urlBuilder = searchBaseUrl.toHttpUrlOrNull()?.newBuilder() ?: return@withContext results
            urlBuilder.addQueryParameter("search_terms", cleanQuery)
            urlBuilder.addQueryParameter("search_simple", "1")
            urlBuilder.addQueryParameter("action", "process")
            urlBuilder.addQueryParameter("json", "1")
            urlBuilder.addQueryParameter("page_size", "60")
            urlBuilder.addQueryParameter("lc", "pl")
            urlBuilder.addQueryParameter(
                "fields",
                "code,product_name,product_name_pl,product_name_en,generic_name_pl,generic_name,brands,nutriments,image_front_small_url,serving_size,quantity"
            )

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "NutriTrack-Android - Version 1.0 - contact@nutritrack.app")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val remoteItems = parseSearchResults(body)
                            val newItems = mutableListOf<FoodProduct>()
                            for (item in remoteItems) {
                                if (item.barcode.isEmpty() || !seenBarcodes.contains(item.barcode)) {
                                    newItems.add(item)
                                    if (item.barcode.isNotEmpty()) {
                                        seenBarcodes.add(item.barcode)
                                    }
                                }
                            }
                            searchCache[cacheKey] = newItems
                            results.addAll(newItems)
                        }
                    }
                }
            } catch (e: Exception) {
                // Return local results if network fails
            }

            results
        }

    suspend fun fetchByBarcode(barcode: String): FoodProduct? = withContext(Dispatchers.IO) {
        val clean = barcode.trim()
        if (clean.isEmpty()) return@withContext null

        barcodeCache[clean]?.let { return@withContext it }

        PolishFoodCatalog.findByBarcode(clean)?.let {
            val local = it.copy(source = "Baza lokalna")
            barcodeCache[clean] = local
            return@withContext local
        }

        // Try OFF v2
        fetchRemoteBarcode("$productV2Url/$clean.json?lc=pl", clean)?.let {
            barcodeCache[clean] = it
            return@withContext it
        }

        // Fallback to OFF v0
        fetchRemoteBarcode("$productV0Url/$clean.json", clean)?.let {
            barcodeCache[clean] = it
            return@withContext it
        }

        null
    }

    private fun fetchRemoteBarcode(url: String, fallbackBarcode: String): FoodProduct? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NutriTrack-Android - Version 1.0 - contact@nutritrack.app")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                parseSingleProduct(body, fallbackBarcode)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSearchResults(jsonStr: String): List<FoodProduct> {
        val list = mutableListOf<FoodProduct>()
        try {
            val root = JsonParser.parseString(jsonStr).asJsonObject
            val products = root.getAsJsonArray("products") ?: return emptyList()

            for (elem in products) {
                if (!elem.isJsonObject) continue
                val obj = elem.asJsonObject
                val product = parseProductJsonObject(obj, obj.optString("code", ""))
                if (product != null) {
                    list.add(product)
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseSingleProduct(jsonStr: String, fallbackBarcode: String): FoodProduct? {
        try {
            val root = JsonParser.parseString(jsonStr).asJsonObject
            val status = root.optInt("status", 0)
            if (status != 1) return null
            val productObj = root.getAsJsonObject("product") ?: return null
            return parseProductJsonObject(productObj, fallbackBarcode)
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseProductJsonObject(obj: JsonObject, fallbackBarcode: String): FoodProduct? {
        val name = obj.optString("product_name_pl", "")
            .ifBlank { obj.optString("product_name", "") }
            .ifBlank { obj.optString("product_name_en", "") }
            .ifBlank { obj.optString("generic_name_pl", "") }
            .ifBlank { obj.optString("generic_name", "") }
            .ifBlank { obj.optString("brands", "") }
            .ifBlank { "Produkt spożywczy" }

        val code = obj.optString("code", fallbackBarcode)
        val brand = obj.optString("brands", "").ifBlank { null }
        val imageUrl = obj.optString("image_front_small_url", "")
            .ifBlank { obj.optString("image_front_url", "") }
            .ifBlank { null }
        val servingSize = obj.optString("serving_size", "").ifBlank { null }

        val nutriments = obj.getAsJsonObject("nutriments") ?: JsonObject()

        var kcal = extractNutrient(nutriments, "energy-kcal_100g", "energy-kcal_value", "energy-kcal")
        val protein = extractNutrient(nutriments, "proteins_100g", "proteins_value", "proteins")
        val carbs = extractNutrient(nutriments, "carbohydrates_100g", "carbohydrates_value", "carbohydrates")
        val fat = extractNutrient(nutriments, "fat_100g", "fat_value", "fat")
        val fiber = extractNutrientOrNull(nutriments, "fiber_100g", "fiber_value", "fiber")

        if (kcal == 0.0) {
            val kj = extractNutrient(nutriments, "energy-kj_100g", "energy-kj_value", "energy-kj", "energy_100g")
            if (kj > 0.0) {
                kcal = ((kj / 4.184) * 10.0).roundToInt() / 10.0
            }
        }

        if (kcal == 0.0 && protein == 0.0 && carbs == 0.0 && fat == 0.0) {
            return null
        }

        return FoodProduct(
            barcode = code,
            name = name,
            brand = brand,
            imageUrl = imageUrl,
            caloriesPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            fiberPer100g = fiber,
            servingSize = servingSize,
            servingSizeGrams = 100.0,
            source = "Open Food Facts"
        )
    }

    private fun extractNutrient(nutriments: JsonObject, vararg keys: String): Double {
        for (k in keys) {
            if (nutriments.has(k) && !nutriments.get(k).isJsonNull) {
                val element = nutriments.get(k)
                val num = parseDouble(element)
                if (num != null && num >= 0) return num
            }
        }
        return 0.0
    }

    private fun extractNutrientOrNull(nutriments: JsonObject, vararg keys: String): Double? {
        for (k in keys) {
            if (nutriments.has(k) && !nutriments.get(k).isJsonNull) {
                val element = nutriments.get(k)
                val num = parseDouble(element)
                if (num != null && num >= 0) return num
            }
        }
        return null
    }

    private fun parseDouble(elem: JsonElement): Double? {
        return try {
            if (elem.isJsonPrimitive && elem.asJsonPrimitive.isNumber) {
                elem.asDouble
            } else {
                val str = elem.asString.replace(",", ".").trim()
                str.toDoubleOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.optString(key: String, default: String): String {
        return if (has(key) && !get(key).isJsonNull) get(key).asString else default
    }

    private fun JsonObject.optInt(key: String, default: Int): Int {
        return try {
            if (has(key) && !get(key).isJsonNull) get(key).asInt else default
        } catch (_: Exception) {
            default
        }
    }
}
