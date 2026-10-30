package com.nutritrack.app.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutritrack.app.model.ChatMessage
import com.nutritrack.app.model.MealType
import com.nutritrack.app.ui.theme.ElectricIndigoProtein
import com.nutritrack.app.ui.theme.FlameCoralCalories
import com.nutritrack.app.ui.theme.GoldenAmberFat
import com.nutritrack.app.ui.theme.MintEmeraldCarbs
import com.nutritrack.app.viewmodel.GeminiViewModel
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    viewModel: GeminiViewModel,
    selectedDate: Date,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isAnalyzing.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var inputText by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedImage = bitmap
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dietetyk Gemini AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Wirtualny asystent żywieniowy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
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
        ) {
            // Chat messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChatMessageBubble(
                        message = message,
                        onSaveMeal = {
                            viewModel.saveChatMessageMealToLog(message.id, selectedDate, selectedMealType)
                        }
                    )
                }

                if (isGenerating) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = FlameCoralCalories,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini analizuje...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Quick suggestions row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.quickSuggestions) { item ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val clean = item.substringAfter(" ")
                                viewModel.sendChatMessage(clean, null, selectedMealType)
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Selected image thumbnail preview before sending
            if (selectedImage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = selectedImage!!.asImageBitmap(),
                        contentDescription = "Załączone zdjęcie",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(onClick = { selectedImage = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Usuń zdjęcie", tint = Color.Red)
                    }
                }
            }

            // Bottom Input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Załącz zdjęcie",
                            tint = FlameCoralCalories
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Napisz co zjadłeś...") },
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank() || selectedImage != null) {
                                viewModel.sendChatMessage(inputText, selectedImage, selectedMealType)
                                inputText = ""
                                selectedImage = null
                                focusManager.clearFocus()
                            }
                        }),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FlameCoralCalories,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || selectedImage != null) {
                                viewModel.sendChatMessage(inputText, selectedImage, selectedMealType)
                                inputText = ""
                                selectedImage = null
                                focusManager.clearFocus()
                            }
                        },
                        enabled = !isGenerating && (inputText.isNotBlank() || selectedImage != null)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Wyślij",
                            tint = if (inputText.isNotBlank() || selectedImage != null) FlameCoralCalories else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onSaveMeal: () -> Unit
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                ),
            color = if (isUser) FlameCoralCalories else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isUser) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.image != null) {
                    Image(
                        bitmap = message.image.asImageBitmap(),
                        contentDescription = "Zdjęcie użytkownika",
                        modifier = Modifier
                            .size(width = 200.dp, height = 150.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (!message.text.isNullOrBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                // If message has structured analysis result, render interactive card
                if (message.analysisResult != null) {
                    val analysis = message.analysisResult
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${analysis.totalCalories.roundToInt()} kcal",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = FlameCoralCalories
                                )
                            )
                            Text(
                                text = "B: ${analysis.totalProtein.roundToInt()}g • W: ${analysis.totalCarbs.roundToInt()}g • T: ${analysis.totalFat.roundToInt()}g",
                                style = MaterialTheme.typography.labelSmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (message.isSavedToLog) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Zapisano",
                                        tint = MintEmeraldCarbs,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = " Zapisano w dzienniku",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MintEmeraldCarbs,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onSaveMeal),
                                    color = FlameCoralCalories
                                ) {
                                    Text(
                                        text = "Dodaj posiłek do dziennika",
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
                }
            }
        }
    }
}
