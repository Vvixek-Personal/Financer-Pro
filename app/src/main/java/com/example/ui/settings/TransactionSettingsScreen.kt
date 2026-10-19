package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Dedicated Transaction Settings Screen (Rule 13)
 * Configures input defaults, payment methods, high-amount warnings, and deletion confirmations.
 */
@Composable
fun TransactionSettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val defaultTxType by viewModel.defaultTxType.collectAsStateWithLifecycle()
    val rememberLastCategory by viewModel.rememberLastCategory.collectAsStateWithLifecycle()
    val confirmDelete by viewModel.confirmTxDelete.collectAsStateWithLifecycle()
    val groupByDate by viewModel.groupByDate.collectAsStateWithLifecycle()
    val defaultPaymentMode by viewModel.defaultPaymentMode.collectAsStateWithLifecycle()
    val highAmountEnabled by viewModel.highAmountWarningEnabled.collectAsStateWithLifecycle()
    val highAmountThreshold by viewModel.highAmountThreshold.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()

    var thresholdInput by remember(highAmountThreshold) { mutableStateOf(highAmountThreshold.toInt().toString()) }

    Scaffold(
        topBar = {
            SettingsHeaderTitle(title = "Transaction Preferences", onBack = onBack)
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
            // Input Defaults Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Input Defaults",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Default Transaction Type", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (typeKey, label) ->
                            val isSelected = defaultTxType.equals(typeKey, ignoreCase = true)
                            FilledTonalButton(
                                onClick = { viewModel.updateDefaultTxType(typeKey) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSelected) {
                                        if (typeKey == "EXPENSE") ExpenseRed else IncomeGreen
                                    } else SleekBg,
                                    contentColor = if (isSelected) Color.White else SleekTextPrimary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else SleekBorder)
                            ) {
                                Text(label, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SleekBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Default Payment Method", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("UPI", "Cash", "Card", "NetBank").forEach { mode ->
                            val isSelected = defaultPaymentMode.equals(mode, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateDefaultPaymentMode(mode) },
                                label = { Text(mode, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SleekBorder, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Remember Last Category", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Automatically select last-used category for faster entry", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(
                            checked = rememberLastCategory,
                            onCheckedChange = { viewModel.toggleRememberLastCategory(it) }
                        )
                    }
                }
            }

            // Organization & Safety Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Safety & Organization",
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
                            Text("Group Transactions by Date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Organize records into Today, Yesterday and monthly section headers", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(
                            checked = groupByDate,
                            onCheckedChange = { viewModel.toggleGroupByDate(it) }
                        )
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
                            Text("Confirm Before Deleting", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Show confirmation dialog to prevent accidental deletion", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(
                            checked = confirmDelete,
                            onCheckedChange = { viewModel.toggleConfirmTxDelete(it) }
                        )
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
                            Text("High Amount Alert", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Requires confirmation for amounts over $currencySymbol$thresholdInput", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(
                            checked = highAmountEnabled,
                            onCheckedChange = { viewModel.updateHighAmountWarning(it, thresholdInput.toDoubleOrNull() ?: 5000.0) }
                        )
                    }

                    if (highAmountEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = thresholdInput,
                            onValueChange = {
                                thresholdInput = it.filter { char -> char.isDigit() }
                                val d = thresholdInput.toDoubleOrNull()
                                if (d != null && d > 0) {
                                    viewModel.updateHighAmountWarning(true, d)
                                }
                            },
                            label = { Text("Warning Threshold ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }
}
