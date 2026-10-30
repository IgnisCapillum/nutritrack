package com.nutritrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nutritrack.app.model.MealType
import com.nutritrack.app.ui.screens.BarcodeScannerScreen
import com.nutritrack.app.ui.screens.CustomFoodEntryScreen
import com.nutritrack.app.ui.screens.DashboardScreen
import com.nutritrack.app.ui.screens.GeminiChatScreen
import com.nutritrack.app.ui.screens.GeminiPhotoScreen
import com.nutritrack.app.ui.screens.SearchFoodScreen
import com.nutritrack.app.ui.screens.SettingsScreen
import com.nutritrack.app.ui.theme.NutriTrackTheme
import com.nutritrack.app.viewmodel.DashboardViewModel
import com.nutritrack.app.viewmodel.GeminiViewModel
import com.nutritrack.app.viewmodel.ScannerViewModel
import com.nutritrack.app.viewmodel.SearchViewModel
import com.nutritrack.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = NutriTrackApp.instance.repository

        setContent {
            NutriTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NutriTrackNavGraph(repository = repository)
                }
            }
        }
    }
}

@Composable
fun NutriTrackNavGraph(
    repository: com.nutritrack.app.data.repository.NutritionRepository
) {
    val navController = rememberNavController()

    val dashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel<DashboardViewModel>(
        factory = DashboardViewModel.Factory(repository)
    )
    val searchViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SearchViewModel>(
        factory = SearchViewModel.Factory(repository)
    )
    val scannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel<ScannerViewModel>(
        factory = ScannerViewModel.Factory(repository)
    )
    val geminiViewModel = androidx.lifecycle.viewmodel.compose.viewModel<GeminiViewModel>(
        factory = GeminiViewModel.Factory(repository)
    )
    val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel>(
        factory = SettingsViewModel.Factory(repository)
    )

    val selectedDate by dashboardViewModel.selectedDate.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        // 1. Dashboard
        composable("dashboard") {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToSearch = { mealType ->
                    searchViewModel.setInitialMealType(mealType)
                    navController.navigate("search/${mealType.rawValue}")
                },
                onNavigateToScanner = { mealType ->
                    scannerViewModel.setTargetMealType(mealType)
                    scannerViewModel.reset()
                    navController.navigate("scanner/${mealType.rawValue}")
                },
                onNavigateToGeminiPhoto = { mealType ->
                    geminiViewModel.setTargetMealType(mealType)
                    navController.navigate("gemini_photo/${mealType.rawValue}")
                },
                onNavigateToGeminiChat = {
                    navController.navigate("gemini_chat")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // 2. Food Search
        composable(
            route = "search/{mealType}",
            arguments = listOf(navArgument("mealType") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealRaw = backStackEntry.arguments?.getString("mealType") ?: "lunch"
            val mealType = MealType.fromRaw(mealRaw)

            SearchFoodScreen(
                viewModel = searchViewModel,
                selectedDate = selectedDate,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = {
                    scannerViewModel.setTargetMealType(mealType)
                    scannerViewModel.reset()
                    navController.navigate("scanner/$mealRaw")
                },
                onNavigateToCustomFood = {
                    navController.navigate("custom_food?mealType=$mealRaw")
                }
            )
        }

        // 3. Barcode Scanner
        composable(
            route = "scanner/{mealType}",
            arguments = listOf(navArgument("mealType") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealRaw = backStackEntry.arguments?.getString("mealType") ?: "lunch"
            val mealType = MealType.fromRaw(mealRaw)

            BarcodeScannerScreen(
                viewModel = scannerViewModel,
                repository = repository,
                selectedDate = selectedDate,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCustomFoodWithBarcode = { barcode, mType ->
                    navController.navigate("custom_food?barcode=$barcode&mealType=${mType.rawValue}")
                }
            )
        }

        // 4. Gemini AI Photo Estimator
        composable(
            route = "gemini_photo/{mealType}",
            arguments = listOf(navArgument("mealType") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealRaw = backStackEntry.arguments?.getString("mealType") ?: "lunch"
            val mealType = MealType.fromRaw(mealRaw)

            GeminiPhotoScreen(
                viewModel = geminiViewModel,
                selectedDate = selectedDate,
                initialMealType = mealType,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Gemini AI Chat Assistant
        composable("gemini_chat") {
            GeminiChatScreen(
                viewModel = geminiViewModel,
                selectedDate = selectedDate,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. Custom Food Entry
        composable(
            route = "custom_food?barcode={barcode}&mealType={mealType}",
            arguments = listOf(
                navArgument("barcode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("mealType") {
                    type = NavType.StringType
                    defaultValue = "lunch"
                }
            )
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode")
            val mealRaw = backStackEntry.arguments?.getString("mealType") ?: "lunch"
            val mealType = MealType.fromRaw(mealRaw)

            CustomFoodEntryScreen(
                repository = repository,
                selectedDate = selectedDate,
                initialBarcode = barcode,
                initialMealType = mealType,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7. Settings
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
