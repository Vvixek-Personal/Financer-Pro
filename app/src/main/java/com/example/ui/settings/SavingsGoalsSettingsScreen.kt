package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Dedicated Savings Goals Settings Screen (Rule 11)
 * Configures layout mode, progress visualization styles, and automatic milestone calculations.
 */
@Composable
fun SavingsGoalsSettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val viewMode by viewModel.goalViewMode.collectAsStateWithLifecycle()
    val progressStyle by viewModel.goalProgressStyle.collectAsStateWithLifecycle()

    var autoGapCalculation by remember { mutableStateOf(true) }
    var celebrateMilestones by remember { mutableStateOf(true) }
    var autoArchiveCompleted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SettingsHeaderTitle(title = "Savings Goals Settings", onBack = onBack)
        },
        containerColor = SleekBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display Preferences Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Display & Visualization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // View Mode
                    Text("Card Layout", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Grid", "List").forEach { mode ->
                            val isSelected = viewMode == mode
                            FilledTonalButton(
                                onClick = { viewModel.updateGoalViewMode(mode) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) SleekPrimary else SleekBg,
                                    contentColor = if (isSelected) Color.White else SleekTextPrimary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) SleekPrimary else SleekBorder)
                            ) {
                                Icon(
                                    imageVector = if (mode == "Grid") AppIcons.GridView else AppIcons.ViewAgenda,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(mode)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Style
                    Text("Progress Indicator Style", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Circle", "Bar").forEach { style ->
                            val isSelected = progressStyle == style
                            FilledTonalButton(
                                onClick = { viewModel.updateGoalProgressStyle(style) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) SleekPrimary else SleekBg,
                                    contentColor = if (isSelected) Color.White else SleekTextPrimary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) SleekPrimary else SleekBorder)
                            ) {
                                Icon(
                                    imageVector = if (style == "Circle") AppIcons.PieChart else AppIcons.LinearScale,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (style == "Circle") "Circular Arc" else "Horizontal Bar")
                            }
                        }
                    }
                }
            }

            // Calculation & Goal Rules Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Behavior & Calculations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Gap Calculation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Automatically calculates suggested weekly contribution based on target date", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = autoGapCalculation, onCheckedChange = { autoGapCalculation = it })
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SleekBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Milestone Celebrations", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Celebrate when reaching 25%, 50%, 75% and 100% of target amount", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = celebrateMilestones, onCheckedChange = { celebrateMilestones = it })
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SleekBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Archive Completed Goals", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Hide 100% reached goals from active list into completed archive", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = autoArchiveCompleted, onCheckedChange = { autoArchiveCompleted = it })
                    }
                }
            }
        }
    }
}
