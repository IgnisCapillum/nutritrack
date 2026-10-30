package com.nutritrack.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nutritrack.app.data.repository.NutritionRepository
import com.nutritrack.app.model.MealType
import com.nutritrack.app.ui.theme.FlameCoralCalories
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodEntryScreen(
    repository: NutritionRepository,
    selectedDate: Date,
    initialBarcode: String? = null,
    initialMealType: MealType = MealType.LUNCH,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(initialBarcode.orEmpty()) }
    var servingSizeText by remember { mutableStateOf("100") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var fiberText by remember { mutableStateOf("") }
    var selectedMeal by remember { mutableStateOf(initialMealType) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Własny produkt", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("Nazwa produktu *") },
                placeholder = { Text("np. Domowy chleb gryczany") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Marka / Producent (opcjonalnie)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Kod kreskowy EAN (opcjonalnie)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Wartości odżywcze w porcji",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = servingSizeText,
                    onValueChange = { servingSizeText = it },
                    label = { Text("Waga porcji (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it; errorText = null },
                    label = { Text("Kalorie (kcal) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it },
                    label = { Text("Białko (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = carbsText,
                    onValueChange = { carbsText = it },
                    label = { Text("Węglowodany (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = fatText,
                    onValueChange = { fatText = it },
                    label = { Text("Tłuszcz (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Dodaj do posiłku:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MealType.entries) { m ->
                    val isSelected = m == selectedMeal
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedMeal = m },
                        color = if (isSelected) FlameCoralCalories else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = m.titlePL,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (name.isBlank()) {
                            errorText = "Wprowadź nazwę produktu"
                            return@clickable
                        }
                        val cal = caloriesText.replace(",", ".").toDoubleOrNull()
                        if (cal == null) {
                            errorText = "Wprowadź prawidłową liczbę kalorii"
                            return@clickable
                        }
                        val portion = servingSizeText.replace(",", ".").toDoubleOrNull() ?: 100.0
                        val prot = proteinText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val carbs = carbsText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val fat = fatText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val fiber = fiberText.replace(",", ".").toDoubleOrNull()

                        coroutineScope.launch {
                            repository.addFoodItemToMeal(
                                date = selectedDate,
                                mealType = selectedMeal,
                                name = name.trim(),
                                brand = brand.trim().ifEmpty { null },
                                servingSizeGrams = portion,
                                calories = cal,
                                protein = prot,
                                carbs = carbs,
                                fat = fat,
                                fiber = fiber,
                                barcode = barcode.trim().ifEmpty { null }
                            )
                            onNavigateBack()
                        }
                    },
                color = FlameCoralCalories
            ) {
                Text(
                    text = "Zapisz i dodaj do posiłku",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
