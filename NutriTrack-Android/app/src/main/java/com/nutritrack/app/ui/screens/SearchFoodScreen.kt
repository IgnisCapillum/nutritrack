package com.nutritrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutritrack.app.model.FoodCategory
import com.nutritrack.app.model.FoodProduct
import com.nutritrack.app.model.MealType
import com.nutritrack.app.ui.theme.ElectricIndigoProtein
import com.nutritrack.app.ui.theme.FlameCoralCalories
import com.nutritrack.app.ui.theme.GoldenAmberFat
import com.nutritrack.app.ui.theme.MintEmeraldCarbs
import com.nutritrack.app.viewmodel.SearchViewModel
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFoodScreen(
    viewModel: SearchViewModel,
    selectedDate: Date,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToCustomFood: () -> Unit
) {
    val query by viewModel.query.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeProduct by viewModel.activeProduct.collectAsState()
    val portionGrams by viewModel.portionGrams.collectAsState()
    val targetMealType by viewModel.targetMealType.collectAsState()

    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dodaj do posiłku", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Skanuj kod",
                            tint = FlameCoralCalories
                        )
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
        ) {
            // Search Input Box
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = { Text("Szukaj produktu lub marki (np. Skyr, Twaróg)...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Szukaj", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Wyczyść")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = FlameCoralCalories,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Category Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                items(FoodCategory.entries) { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.onCategorySelected(cat) },
                        color = if (isSelected) FlameCoralCalories else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 0.dp else 1.dp
                    ) {
                        Text(
                            text = cat.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Results count or loading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (query.isEmpty()) "Polecane produkty (${results.size})" else "Znalezione produkty (${results.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = FlameCoralCalories
                    )
                }
            }

            // Products list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results) { product ->
                    ProductSearchResultRow(
                        product = product,
                        onClick = { viewModel.selectProduct(product) }
                    )
                }

                // Bottom custom entry banner if nothing fits
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onNavigateToCustomFood),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Dodaj własny",
                                tint = FlameCoralCalories
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = "Nie ma Twojego produktu?",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kliknij tutaj, aby dodać własny produkt ręcznie.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Product Detail & Serving Size
    if (activeProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissProduct() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val prod = activeProduct!!
            val nutrition = prod.calculatedNutrition(portionGrams)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    text = prod.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!prod.brand.isNullOrBlank()) {
                    Text(
                        text = prod.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calculated Calories & Macros Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wartość odżywcza",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${nutrition.calories.roundToInt()} kcal",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FlameCoralCalories
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroPill("Białko", "${nutrition.protein}g", ElectricIndigoProtein)
                            MacroPill("Węgle", "${nutrition.carbs}g", MintEmeraldCarbs)
                            MacroPill("Tłuszcz", "${nutrition.fat}g", GoldenAmberFat)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Portion grams slider and manual input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Waga porcji",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${portionGrams.roundToInt()} g",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FlameCoralCalories
                        )
                    )
                }

                Slider(
                    value = portionGrams.toFloat(),
                    onValueChange = { viewModel.setPortionGrams(it.toDouble().roundToInt().toDouble()) },
                    valueRange = 10f..500f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = FlameCoralCalories,
                        activeTrackColor = FlameCoralCalories,
                        inactiveTrackColor = FlameCoralCalories.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick portion pills (50g, 100g, 150g, 200g, 250g)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50.0, 100.0, 150.0, 200.0, 250.0).forEach { g ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setPortionGrams(g) },
                            color = if (portionGrams.roundToInt() == g.toInt()) FlameCoralCalories else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${g.toInt()}g",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (portionGrams.roundToInt() == g.toInt()) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Target Meal Type Selection
                Text(
                    text = "Posiłek:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MealType.entries) { m ->
                        val isSelected = m == targetMealType
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setTargetMealType(m) },
                            color = if (isSelected) FlameCoralCalories else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = m.titlePL,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Add to log button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.addActiveProductToLog(selectedDate) {
                                onNavigateBack()
                            }
                        },
                    color = FlameCoralCalories
                ) {
                    Text(
                        text = "Dodaj do posiłku (${nutrition.calories.roundToInt()} kcal)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .padding(vertical = 14.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun MacroPill(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
fun ProductSearchResultRow(
    product: FoodProduct,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!product.brand.isNullOrBlank()) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "w 100g: B: ${product.proteinPer100g}g • W: ${product.carbsPer100g}g • T: ${product.fatPer100g}g",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.caloriesPer100g.roundToInt()} kcal",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FlameCoralCalories
                    )
                )
                if (product.source != null) {
                    Text(
                        text = product.source!!,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
