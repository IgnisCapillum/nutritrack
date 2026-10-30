package com.nutritrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutritrack.app.ui.theme.FlameCoralCalories
import kotlin.math.roundToInt

@Composable
fun MacroRingView(
    consumedCalories: Double,
    goalCalories: Double,
    size: Dp = 130.dp,
    strokeWidth: Dp = 12.dp,
    ringColor: Color = FlameCoralCalories,
    modifier: Modifier = Modifier
) {
    val progress = if (goalCalories > 0) {
        (consumedCalories / goalCalories).toFloat().coerceIn(0f, 1f)
    } else 0f

    val remaining = (goalCalories - consumedCalories).roundToInt()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "MacroRingAnimation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            // Background track
            drawCircle(
                color = ringColor.copy(alpha = 0.15f),
                style = Stroke(width = strokePx)
            )

            // Progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${consumedCalories.roundToInt()}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (remaining >= 0) "pozostało: $remaining" else "ponad cel: ${-remaining}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
