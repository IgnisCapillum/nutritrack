package com.nutritrack.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutritrack.app.data.local.FoodEntryEntity
import com.nutritrack.app.data.local.MealWithEntries
import com.nutritrack.app.model.MealType
import com.nutritrack.app.ui.components.CalendarStripView
import com.nutritrack.app.ui.components.MacroProgressBar
import com.nutritrack.app.ui.components.MacroRingView
import com.nutritrack.app.ui.theme.ElectricIndigoProtein
import com.nutritrack.app.ui.theme.FlameCoralCalories
import com.nutritrack.app.ui.theme.GoldenAmberFat
import com.nutritrack.app.ui.theme.MintEmeraldCarbs
import com.nutritrack.app.viewmodel.DashboardViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSearch: (MealType) -> Unit,
    onNavigateToScanner: (MealType) -> Unit,
    onNavigateToGeminiPhoto: (MealType) -> Unit,
    onNavigateToGeminiChat: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyLog by viewModel.dailyLog.collectAsState()
    val goals by viewModel.userGoals.collectAsState()

    val calorieGoal = goals?.calorieGoal ?: 2200.0
    val proteinGoal = goals?.proteinGoalGrams ?: 160.0
    val carbsGoal = goals?.carbsGoalGrams ?: 220.0
    val fatGoal = goals?.fatGoalGrams ?: 70.0

    val consumedCalories = dailyLog?.totalCalories ?: 0.0
    val consumedProtein = dailyLog?.totalProtein ?: 0.0
    val consumedCarbs = dailyLog?.totalCarbs ?: 0.0
    val consumedFat = dailyLog?.totalFat ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NutriTrack",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                        Surface(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = FlameCoralCalories.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "PL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FlameCoralCalories
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Ustawienia",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToSearch(MealType.LUNCH) },
                containerColor = FlameCoralCalories,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Dodaj produkt")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Calendar strip
            item {
                CalendarStripView(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }

            // 2. Macro Summary Card
            item {
                MacroSummaryCard(
                    consumedCalories = consumedCalories,
                    calorieGoal = calorieGoal,
                    consumedProtein = consumedProtein,
                    proteinGoal = proteinGoal,
                    consumedCarbs = consumedCarbs,
                    carbsGoal = carbsGoal,
                    consumedFat = consumedFat,
                    fatGoal = fatGoal
                )
            }

            // 3. Quick Action Hub (AI Photo, Barcode Scanner, AI Chat)
            item {
                QuickActionHub(
                    onPhotoClick = { onNavigateToGeminiPhoto(MealType.LUNCH) },
                    onScannerClick = { onNavigateToScanner(MealType.LUNCH) },
                    onChatClick = onNavigateToGeminiChat
                )
            }

            // 4. Meal Breakdown Sections
            items(MealType.entries) { mealType ->
                val mealWithEntries = dailyLog?.mealFor(mealType)
                MealSectionCard(
                    mealType = mealType,
                    mealWithEntries = mealWithEntries,
                    onAddClick = { onNavigateToSearch(mealType) },
                    onDeleteEntry = { viewModel.deleteFoodEntry(it.id) }
                )
            }
        }
    }
}

@Composable
fun MacroSummaryCard(
    consumedCalories: Double,
    calorieGoal: Double,
    consumedProtein: Double,
    proteinGoal: Double,
    consumedCarbs: Double,
    carbsGoal: Double,
    consumedFat: Double,
    fatGoal: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacroRingView(
                consumedCalories = consumedCalories,
                goalCalories = calorieGoal,
                size = 120.dp
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroProgressBar(
                    label = "Białko",
                    consumed = consumedProtein,
                    goal = proteinGoal,
                    color = ElectricIndigoProtein
                )
                MacroProgressBar(
                    label = "Węglowodany",
                    consumed = consumedCarbs,
                    goal = carbsGoal,
                    color = MintEmeraldCarbs
                )
                MacroProgressBar(
                    label = "Tłuszcze",
                    consumed = consumedFat,
                    goal = fatGoal,
                    color = GoldenAmberFat
                )
            }
        }
    }
}

@Composable
fun QuickActionHub(
    onPhotoClick: () -> Unit,
    onScannerClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            title = "Skanuj AI",
            subtitle = "Zdjęcie posiłku",
            icon = Icons.Rounded.AutoAwesome,
            accentColor = FlameCoralCalories,
            modifier = Modifier.weight(1f),
            onClick = onPhotoClick
        )
        QuickActionButton(
            title = "Kod kreskowy",
            subtitle = "Skaner EAN-13",
            icon = Icons.Rounded.QrCodeScanner,
            accentColor = ElectricIndigoProtein,
            modifier = Modifier.weight(1f),
            onClick = onScannerClick
        )
        QuickActionButton(
            title = "Czat AI",
            subtitle = "Dietetyk Gemini",
            icon = Icons.Rounded.Chat,
            accentColor = MintEmeraldCarbs,
            modifier = Modifier.weight(1f),
            onClick = onChatClick
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun MealSectionCard(
    mealType: MealType,
    mealWithEntries: MealWithEntries?,
    onAddClick: () -> Unit,
    onDeleteEntry: (FoodEntryEntity) -> Unit
) {
    val entries = mealWithEntries?.entries.orEmpty()
    val totalCalories = mealWithEntries?.totalCalories ?: 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mealType.titlePL,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (totalCalories > 0) {
                        Surface(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = FlameCoralCalories.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${totalCalories.roundToInt()} kcal",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FlameCoralCalories
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onAddClick),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Dodaj",
                            tint = FlameCoralCalories,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Dodaj",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FlameCoralCalories
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Food entries
            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.forEach { entry ->
                        FoodEntryRow(entry = entry, onDelete = { onDeleteEntry(entry) })
                    }
                }
            }
        }
    }
}

@Composable
fun FoodEntryRow(
    entry: FoodEntryEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (entry.isAIGenerated) {
                    Surface(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = FlameCoralCalories.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlameCoralCalories
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                text = "${entry.servingSizeGrams.roundToInt()}g • B: ${entry.protein.roundToInt()}g • W: ${entry.carbs.roundToInt()}g • T: ${entry.fat.roundToInt()}g",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${entry.calories.roundToInt()} kcal",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Usuń",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
