package com.nutritrack.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutritrack.app.ui.theme.FlameCoralCalories
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarStripView(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val polishLocale = remember { Locale("pl", "PL") }
    val dayNameFormat = remember { SimpleDateFormat("EE", polishLocale) }
    val dayNumFormat = remember { SimpleDateFormat("d", polishLocale) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, d MMMM", polishLocale) }

    // Generate +/- 15 days window around current selection
    val daysWindow = remember(selectedDate) {
        val list = mutableListOf<Date>()
        val cal = Calendar.currentClean(selectedDate)
        cal.add(Calendar.DAY_OF_YEAR, -14)
        for (i in 0..28) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedDate) {
        val selectedIdx = daysWindow.indexOfFirst { it.isSameDayAs(selectedDate) }
        if (selectedIdx != -1) {
            lazyListState.animateScrollToItem(maxOf(0, selectedIdx - 2))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Top Header: Date title + Today snap button + DatePicker icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = getRelativeDateLabel(selectedDate),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = fullDateFormat.format(selectedDate).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!selectedDate.isSameDayAs(Date())) {
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDateSelected(Date()) },
                        color = FlameCoralCalories.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Dzisiaj",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = FlameCoralCalories
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { time = selectedDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val picked = Calendar.getInstance().apply {
                                    set(y, m, d, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onDateSelected(picked.time)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = "Kalendarz",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Horizontal Days Strip
        LazyRow(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(daysWindow) { date ->
                val isSelected = date.isSameDayAs(selectedDate)
                val isToday = date.isSameDayAs(Date())

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isSelected -> FlameCoralCalories
                                isToday -> FlameCoralCalories.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayNameFormat.format(date).uppercase().replace(".", ""),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = dayNumFormat.format(date),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun getRelativeDateLabel(date: Date): String {
    val calNow = Calendar.getInstance().clean()
    val calTarget = Calendar.getInstance().apply { time = date }.clean()

    val diffDays = ((calTarget.timeInMillis - calNow.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    return when (diffDays) {
        0 -> "Dzisiaj"
        -1 -> "Wczoraj"
        1 -> "Jutro"
        else -> SimpleDateFormat("EEEE", Locale("pl", "PL")).format(date).replaceFirstChar { it.uppercase() }
    }
}

private fun Calendar.clean(): Calendar {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return this
}

private fun Calendar.Companion.currentClean(date: Date): Calendar {
    return getInstance().apply {
        time = date
        clean()
    }
}

private fun Date.isSameDayAs(other: Date): Boolean {
    val c1 = Calendar.getInstance().apply { time = this@isSameDayAs }
    val c2 = Calendar.getInstance().apply { time = other }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
