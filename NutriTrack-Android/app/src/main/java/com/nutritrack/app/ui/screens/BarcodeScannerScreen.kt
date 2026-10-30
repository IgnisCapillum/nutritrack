package com.nutritrack.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nutritrack.app.data.repository.NutritionRepository
import com.nutritrack.app.model.FoodProduct
import com.nutritrack.app.model.MealType
import com.nutritrack.app.ui.theme.ElectricIndigoProtein
import com.nutritrack.app.ui.theme.FlameCoralCalories
import com.nutritrack.app.ui.theme.GoldenAmberFat
import com.nutritrack.app.ui.theme.MintEmeraldCarbs
import com.nutritrack.app.viewmodel.ScannerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    viewModel: ScannerViewModel,
    repository: NutritionRepository,
    selectedDate: Date,
    onNavigateBack: () -> Unit,
    onNavigateToCustomFoodWithBarcode: (String, MealType) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val isResolving by viewModel.isResolving.collectAsState()
    val resolvedProduct by viewModel.resolvedProduct.collectAsState()
    val notFoundBarcode by viewModel.notFoundBarcode.collectAsState()
    val targetMealType by viewModel.targetMealType.collectAsState()

    var portionGrams by remember { mutableDoubleStateOf(100.0) }
    var torchEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(resolvedProduct) {
        if (resolvedProduct != null) {
            portionGrams = resolvedProduct!!.servingSizeGrams
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skaner kodów EAN", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Wstecz", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Camera Preview View
            if (hasCameraPermission) {
                CameraPreview(
                    torchEnabled = torchEnabled,
                    onBarcodeScanned = { barcode ->
                        viewModel.onBarcodeDetected(barcode)
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak zezwolenia na aparat.\nUżyj przycisków testowych poniżej lub włącz uprawnienia w systemie.",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            // Viewfinder reticle overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 140.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 180.dp)
                        .border(width = 2.dp, color = FlameCoralCalories, shape = RoundedCornerShape(16.dp))
                )
            }

            // Quick Test Barcode Buttons (Simulator / Real Polish Grocery Staples)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Szybki test kodów polskich produktów:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.testBarcodes) { item ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.onBarcodeDetected(item.barcode) },
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Loading Indicator while resolving
            if (isResolving) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FlameCoralCalories, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sprawdzam bazę produktów...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Bottom Resolved Product Sheet
            if (resolvedProduct != null) {
                val prod = resolvedProduct!!
                val nutrition = prod.calculatedNutrition(portionGrams)

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${prod.brand ?: "Produkt spożywczy"} • Kod: ${prod.barcode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = "${nutrition.calories.roundToInt()} kcal",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FlameCoralCalories
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Macros
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroPill("Białko", "${nutrition.protein}g", ElectricIndigoProtein)
                            MacroPill("Węgle", "${nutrition.carbs}g", MintEmeraldCarbs)
                            MacroPill("Tłuszcz", "${nutrition.fat}g", GoldenAmberFat)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Portion slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Waga porcji:", style = MaterialTheme.typography.bodyMedium)
                            Text("${portionGrams.roundToInt()} g", fontWeight = FontWeight.Bold, color = FlameCoralCalories)
                        }

                        Slider(
                            value = portionGrams.toFloat(),
                            onValueChange = { portionGrams = it.toDouble().roundToInt().toDouble() },
                            valueRange = 10f..400f,
                            colors = SliderDefaults.colors(
                                thumbColor = FlameCoralCalories,
                                activeTrackColor = FlameCoralCalories
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.reset() },
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Skanuj inny",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            repository.addFoodItemToMeal(
                                                date = selectedDate,
                                                mealType = targetMealType,
                                                name = prod.name,
                                                brand = prod.brand,
                                                servingSizeGrams = portionGrams,
                                                calories = nutrition.calories,
                                                protein = nutrition.protein,
                                                carbs = nutrition.carbs,
                                                fat = nutrition.fat,
                                                barcode = prod.barcode
                                            )
                                            onNavigateBack()
                                        }
                                    },
                                color = FlameCoralCalories
                            ) {
                                Text(
                                    text = "Dodaj do ${targetMealType.titlePL}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 12.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }

            // Not Found Barcode Banner
            if (notFoundBarcode != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nie znaleziono produktu",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kod kreskowy $notFoundBarcode nie istnieje jeszcze w bazie. Możesz dodać ten produkt raz, a zostanie zapisany dla Ciebie na stałe!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.reset() },
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Anuluj",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .padding(vertical = 10.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val code = notFoundBarcode!!
                                        viewModel.reset()
                                        onNavigateToCustomFoodWithBarcode(code, targetMealType)
                                    },
                                color = FlameCoralCalories
                            ) {
                                Text(
                                    text = "Dodaj jako nowy",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 10.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    torchEnabled: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val barcodeScanner = BarcodeScanning.getClient()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank()) {
                                        onBarcodeScanned(rawValue)
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    camera.cameraControl.enableTorch(torchEnabled)
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
