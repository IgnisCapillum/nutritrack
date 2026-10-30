package com.nutritrack.app.model

import android.graphics.Bitmap
import java.util.Date
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String? = null,
    val image: Bitmap? = null,
    val analysisResult: GeminiAnalysisResult? = null,
    val timestamp: Date = Date(),
    var isSavedToLog: Boolean = false
)
