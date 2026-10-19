package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Dedicated Date & Time Settings Screen
 * Configures date formatting (dd/MM/yyyy, MM/dd/yyyy, yyyy-MM-dd) and first day of week.
 */
@Composable
fun DateTimeScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val firstDay by viewModel.firstDayOfWeek.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Date & Time Settings", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("DATE FORMAT", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd").forEach { fmt ->
                val selected = dateFormat == fmt
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) SleekPrimary.copy(alpha = 0.12f) else SleekSurface),
                    border = BorderStroke(1.dp, if (selected) SleekPrimary else SleekBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { viewModel.updateDateFormat(fmt) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(fmt, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        RadioButton(selected = selected, onClick = { viewModel.updateDateFormat(fmt) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("FIRST DAY OF THE WEEK", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            listOf("Monday", "Sunday", "Saturday").forEach { day ->
                val selected = firstDay == day
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) SleekPrimary.copy(alpha = 0.12f) else SleekSurface),
                    border = BorderStroke(1.dp, if (selected) SleekPrimary else SleekBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { viewModel.updateFirstDayOfWeek(day) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        RadioButton(selected = selected, onClick = { viewModel.updateFirstDayOfWeek(day) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}
