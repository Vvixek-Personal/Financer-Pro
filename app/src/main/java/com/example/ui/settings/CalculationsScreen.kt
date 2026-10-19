package com.example.ui

import com.example.ui.theme.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Dedicated Financial Calculations Screen (Rule 12)
 * Includes:
 * 1. Currency Converter (Money-to-Money across 100+ currencies)
 * 2. Split Bill & Tip Calculator (with direct option to record share as expense)
 * 3. Percentage & Discount Calculator (X% of Y, % Change, Discount savings)
 * 4. Average & Run-rate Calculator (daily allowance, projected monthly spend)
 * 5. Loan EMI & Mortgage Calculator
 * 6. Wealth & Compound Interest / SIP Simulator
 * 7. Emergency Fund & Runway Analyzer
 * 8. Inflation & Purchasing Power Erosion Calculator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Currency Converter",
        "Split Bill & Tip",
        "Percentage",
        "Run-Rate",
        "Loan & EMI",
        "Wealth & Compound",
        "Emergency Runway",
        "Inflation Erosion"
    )

    Scaffold(
        topBar = {
            SettingsHeaderTitle(title = "Financial Calculators", onBack = onBack)
        },
        containerColor = SleekBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Calculator Tab Selector
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SleekSurface,
                contentColor = SleekPrimary,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = SleekBorder) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) SleekPrimary else SleekTextSecondary
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> CurrencyConverterTool(viewModel)
                    1 -> SplitBillCalculatorTool(viewModel)
                    2 -> PercentageCalculatorTool()
                    3 -> RunRateCalculatorTool(viewModel)
                    4 -> LoanEmiCalculatorTool(viewModel)
                    5 -> WealthCompoundInterestCalculatorTool(viewModel)
                    6 -> EmergencyFundCalculatorTool(viewModel)
                    7 -> InflationCalculatorTool(viewModel)
                }
            }
        }
    }
}

@Composable
private fun CurrencyConverterTool(viewModel: FinanceViewModel) {
    val defaultCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val allCurrencies = remember { CurrencyManager.currencies }

    var fromCurrency by remember { mutableStateOf(CurrencyManager.getByCode(defaultCode)) }
    var toCurrency by remember { mutableStateOf(CurrencyManager.getByCode("USD")) }
    var inputAmountText by remember { mutableStateOf("100") }
    val context = LocalContext.current

    val inputAmount = inputAmountText.toDoubleOrNull() ?: 0.0
    val convertedResult = remember(inputAmount, fromCurrency.code, toCurrency.code) {
        CurrencyManager.convert(inputAmount, fromCurrency.code, toCurrency.code)
    }

    val exchangeRateSingle = remember(fromCurrency.code, toCurrency.code) {
        CurrencyManager.convert(1.0, fromCurrency.code, toCurrency.code)
    }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Result Display Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Converted Amount",
                    style = MaterialTheme.typography.labelMedium,
                    color = SleekTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${toCurrency.symbol}${String.format(Locale.getDefault(), "%,.2f", convertedResult)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary
                )
                Text(
                    text = "${fromCurrency.symbol}${String.format(Locale.getDefault(), "%,.2f", inputAmount)} ${fromCurrency.code} = ${toCurrency.symbol}${String.format(Locale.getDefault(), "%,.2f", convertedResult)} ${toCurrency.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = SleekPrimaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "1 ${fromCurrency.code} = ${String.format(Locale.getDefault(), "%,.4f", exchangeRateSingle)} ${toCurrency.code}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Amount Input Field
        OutlinedTextField(
            value = inputAmountText,
            onValueChange = { inputAmountText = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Amount to Convert") },
            leadingIcon = {
                Text(
                    text = fromCurrency.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface
            )
        )

        // Quick Amount Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("10", "50", "100", "500", "1000").forEach { preset ->
                FilledTonalButton(
                    onClick = { inputAmountText = preset },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(preset, fontSize = 12.sp)
                }
            }
        }

        // Currency Selectors with Swap Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // From Currency Button
            OutlinedCard(
                onClick = { showFromPicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
                colors = CardDefaults.outlinedCardColors(containerColor = SleekSurface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fromCurrency.flag, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("From", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(fromCurrency.code, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    }
                }
            }

            // Swap Button
            IconButton(
                onClick = {
                    val temp = fromCurrency
                    fromCurrency = toCurrency
                    toCurrency = temp
                },
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .background(SleekPrimary, CircleShape)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = AppIcons.SwapHoriz,
                    contentDescription = "Swap Currencies",
                    tint = Color.White
                )
            }

            // To Currency Button
            OutlinedCard(
                onClick = { showToPicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
                colors = CardDefaults.outlinedCardColors(containerColor = SleekSurface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(toCurrency.flag, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("To", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(toCurrency.code, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    }
                }
            }
        }

        // Copy Result Button
        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Currency Result", "${toCurrency.symbol}${String.format(Locale.getDefault(), "%,.2f", convertedResult)} ${toCurrency.code}")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied result to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
        ) {
            Icon(AppIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Conversion Result")
        }
    }

    // Currency Pickers
    if (showFromPicker) {
        CurrencyPickerDialog(
            title = "Select Source Currency",
            currencies = allCurrencies,
            selectedCode = fromCurrency.code,
            onSelect = {
                fromCurrency = it
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false }
        )
    }

    if (showToPicker) {
        CurrencyPickerDialog(
            title = "Select Target Currency",
            currencies = allCurrencies,
            selectedCode = toCurrency.code,
            onSelect = {
                toCurrency = it
                showToPicker = false
            },
            onDismiss = { showToPicker = false }
        )
    }
}

@Composable
private fun SplitBillCalculatorTool(viewModel: FinanceViewModel) {
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    var billAmountText by remember { mutableStateOf("1500") }
    var tipPercent by remember { mutableIntStateOf(10) }
    var peopleCount by remember { mutableIntStateOf(3) }
    val context = LocalContext.current

    val billAmount = billAmountText.toDoubleOrNull() ?: 0.0
    val tipAmount = (billAmount * tipPercent) / 100.0
    val totalWithTip = billAmount + tipAmount
    val perPersonAmount = if (peopleCount > 0) totalWithTip / peopleCount else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Result Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Each Person Pays", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", perPersonAmount)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Bill", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", billAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tip ($tipPercent%)", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", tipAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total With Tip", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", totalWithTip)}",
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }
                }
            }
        }

        // Bill Amount Input
        OutlinedTextField(
            value = billAmountText,
            onValueChange = { billAmountText = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Total Bill Amount") },
            leadingIcon = {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface
            )
        )

        // Tip Percentage Selector
        Column {
            Text("Tip Percentage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 5, 10, 15, 20).forEach { pct ->
                    val isSelected = tipPercent == pct
                    FilledTonalButton(
                        onClick = { tipPercent = pct },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected) SleekPrimary else SleekSurface,
                            contentColor = if (isSelected) Color.White else SleekTextPrimary
                        ),
                        border = BorderStroke(1.dp, if (isSelected) SleekPrimary else SleekBorder),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("$pct%", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // People Stepper
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Number of People", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("$peopleCount person${if (peopleCount > 1) "s" else ""} splitting", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (peopleCount > 1) peopleCount-- },
                        modifier = Modifier
                            .background(SleekPrimaryContainer, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(AppIcons.Remove, contentDescription = "Decrease", tint = SleekPrimary)
                    }

                    Text(
                        text = "$peopleCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(
                        onClick = { if (peopleCount < 50) peopleCount++ },
                        modifier = Modifier
                            .background(SleekPrimaryContainer, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(AppIcons.Add, contentDescription = "Increase", tint = SleekPrimary)
                    }
                }
            }
        }

        // Action: Record my share directly into transactions
        Button(
            onClick = {
                if (perPersonAmount > 0) {
                    viewModel.addExpense(
                        amount = perPersonAmount,
                        category = "Food & Dining",
                        date = System.currentTimeMillis(),
                        note = "Split bill ($peopleCount people) total: $currencySymbol$totalWithTip",
                        type = "EXPENSE",
                        currencyCode = viewModel.selectedCurrencyCode.value
                    )
                    Toast.makeText(context, "Recorded my share ($currencySymbol${String.format(Locale.getDefault(), "%,.2f", perPersonAmount)}) as Expense", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
        ) {
            Icon(AppIcons.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Record My Share as Expense")
        }
    }
}

@Composable
private fun PercentageCalculatorTool() {
    var subMode by remember { mutableIntStateOf(0) } // 0: X% of Y, 1: % Change, 2: Discount
    val subModes = listOf("X% of Y", "% Change", "Discount")

    var val1Text by remember { mutableStateOf("18") }
    var val2Text by remember { mutableStateOf("2500") }

    val val1 = val1Text.toDoubleOrNull() ?: 0.0
    val val2 = val2Text.toDoubleOrNull() ?: 0.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sub Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subModes.forEachIndexed { index, mode ->
                val isSelected = subMode == index
                FilledTonalButton(
                    onClick = { subMode = index },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSelected) SleekPrimary else SleekSurface,
                        contentColor = if (isSelected) Color.White else SleekTextPrimary
                    ),
                    border = BorderStroke(1.dp, if (isSelected) SleekPrimary else SleekBorder)
                ) {
                    Text(mode, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Calculation result
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (subMode) {
                    0 -> { // X% of Y
                        val result = (val1 * val2) / 100.0
                        Text("$val1% of $val2 is", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f", result),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary
                        )
                    }
                    1 -> { // % Change from val1 to val2
                        val diff = val2 - val1
                        val pctChange = if (val1 != 0.0) (diff / val1) * 100.0 else 0.0
                        val isPositive = pctChange >= 0
                        Text("Percentage Change", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format(Locale.getDefault(), "%.2f", pctChange)}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) IncomeGreen else ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Difference: ${if (diff >= 0) "+" else ""}${String.format(Locale.getDefault(), "%,.2f", diff)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary
                        )
                    }
                    else -> { // Discount
                        val discountPct = val1
                        val originalPrice = val2
                        val saved = (originalPrice * discountPct) / 100.0
                        val finalPrice = originalPrice - saved
                        Text("Final Price After $discountPct% Off", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f", finalPrice),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You save: ${String.format(Locale.getDefault(), "%,.2f", saved)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }

        // Inputs
        OutlinedTextField(
            value = val1Text,
            onValueChange = { val1Text = it.filter { char -> char.isDigit() || char == '.' } },
            label = {
                Text(
                    when (subMode) {
                        0 -> "Percentage (%)"
                        1 -> "Initial Amount"
                        else -> "Discount (%)"
                    }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface
            )
        )

        OutlinedTextField(
            value = val2Text,
            onValueChange = { val2Text = it.filter { char -> char.isDigit() || char == '.' } },
            label = {
                Text(
                    when (subMode) {
                        0 -> "Total Value (Y)"
                        1 -> "New Amount"
                        else -> "Original Price"
                    }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface
            )
        )
    }
}

@Composable
private fun RunRateCalculatorTool(viewModel: FinanceViewModel) {
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()

    var spentSoFarText by remember { mutableStateOf("8500") }
    var daysElapsedText by remember { mutableStateOf("10") }
    var totalDaysInMonthText by remember { mutableStateOf("30") }

    val spent = spentSoFarText.toDoubleOrNull() ?: 0.0
    val daysElapsed = (daysElapsedText.toIntOrNull() ?: 1).coerceAtLeast(1)
    val totalDays = (totalDaysInMonthText.toIntOrNull() ?: 30).coerceAtLeast(daysElapsed)
    val daysRemaining = totalDays - daysElapsed

    val dailyAverage = spent / daysElapsed
    val projectedTotal = dailyAverage * totalDays
    val remainingBudget = (monthlyBudget - spent).coerceAtLeast(0.0)
    val safeDailyAllowance = if (daysRemaining > 0) remainingBudget / daysRemaining else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Overview Summary
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Safe Daily Allowance for Rest of Month", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", safeDailyAllowance)} / day",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (safeDailyAllowance >= dailyAverage) IncomeGreen else ExpenseRed
                )
                Text(
                    text = "$daysRemaining days remaining in period",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Pace", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", dailyAverage)} / day",
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Projected Total", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", projectedTotal)}",
                            fontWeight = FontWeight.Bold,
                            color = if (monthlyBudget > 0 && projectedTotal > monthlyBudget) ExpenseRed else IncomeGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Budget Margin", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        val variance = monthlyBudget - projectedTotal
                        Text(
                            text = "${if (variance >= 0) "+" else ""}$currencySymbol${String.format(Locale.getDefault(), "%,.0f", variance)}",
                            fontWeight = FontWeight.Bold,
                            color = if (variance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }

        // Inputs
        OutlinedTextField(
            value = spentSoFarText,
            onValueChange = { spentSoFarText = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Amount Spent So Far ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SleekSurface,
                unfocusedContainerColor = SleekSurface
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = daysElapsedText,
                onValueChange = { daysElapsedText = it.filter { char -> char.isDigit() } },
                label = { Text("Days Elapsed") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SleekSurface,
                    unfocusedContainerColor = SleekSurface
                )
            )

            OutlinedTextField(
                value = totalDaysInMonthText,
                onValueChange = { totalDaysInMonthText = it.filter { char -> char.isDigit() } },
                label = { Text("Days in Month") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SleekSurface,
                    unfocusedContainerColor = SleekSurface
                )
            )
        }
    }
}

@Composable
private fun LoanEmiCalculatorTool(viewModel: FinanceViewModel) {
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()

    var principalText by remember { mutableStateOf("100000") }
    var interestRateText by remember { mutableStateOf("8.5") }
    var tenureYearsText by remember { mutableStateOf("5") }

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val annualRate = interestRateText.toDoubleOrNull() ?: 0.0
    val tenureYears = tenureYearsText.toDoubleOrNull() ?: 1.0

    val (monthlyEmi, totalInterest, totalPayment) = remember(principal, annualRate, tenureYears) {
        if (principal <= 0 || annualRate <= 0 || tenureYears <= 0) {
            Triple(0.0, 0.0, 0.0)
        } else {
            val monthlyRate = annualRate / (12.0 * 100.0)
            val totalMonths = (tenureYears * 12.0).toInt().coerceAtLeast(1)
            val mathPow = Math.pow(1.0 + monthlyRate, totalMonths.toDouble())
            val emi = (principal * monthlyRate * mathPow) / (mathPow - 1.0)
            val totalPay = emi * totalMonths
            val interest = totalPay - principal
            Triple(emi, interest, totalPay)
        }
    }

    val principalRatio = if (totalPayment > 0) (principal / totalPayment).toFloat().coerceIn(0f, 1f) else 0.7f
    val interestRatio = 1f - principalRatio

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Output Summary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Monthly EMI Payment",
                    style = MaterialTheme.typography.labelMedium,
                    color = SleekTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", monthlyEmi)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Stacked Amortization Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(principalRatio.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFF10B981))
                        )
                        Box(
                            modifier = Modifier
                                .weight(interestRatio.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(0xFFF59E0B))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Principal: ${(principalRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Interest: ${(interestRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Interest Payable", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", totalInterest)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Amount Payable", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", totalPayment)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }
                }
            }
        }

        // Input Fields Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Loan Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )

                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = it },
                    label = { Text("Loan Principal ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it },
                        label = { Text("Interest Rate (% p.a.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        )
                    )

                    OutlinedTextField(
                        value = tenureYearsText,
                        onValueChange = { tenureYearsText = it },
                        label = { Text("Tenure (Years)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        )
                    )
                }

                // Quick Tenure Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1 Year" to "1", "3 Years" to "3", "5 Years" to "5", "10 Years" to "10", "20 Years" to "20").forEach { (label, years) ->
                        val isSelected = tenureYearsText == years
                        FilterChip(
                            selected = isSelected,
                            onClick = { tenureYearsText = years },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WealthCompoundInterestCalculatorTool(viewModel: FinanceViewModel) {
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()

    var initialDepositText by remember { mutableStateOf("10000") }
    var monthlyContributionText by remember { mutableStateOf("2500") }
    var annualInterestRateText by remember { mutableStateOf("12.0") }
    var investmentYearsText by remember { mutableStateOf("10") }

    val initialDeposit = initialDepositText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val monthlyContribution = monthlyContributionText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val annualRate = annualInterestRateText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 12.0
    val years = investmentYearsText.toDoubleOrNull()?.coerceIn(1.0, 50.0) ?: 10.0

    val monthlyRate = (annualRate / 100.0) / 12.0
    val totalMonths = (years * 12).toInt()

    val (futureValue, totalInvested, totalGains) = remember(initialDeposit, monthlyContribution, annualRate, years) {
        var balance = initialDeposit
        var invested = initialDeposit
        for (m in 1..totalMonths) {
            balance = (balance + monthlyContribution) * (1.0 + monthlyRate)
            invested += monthlyContribution
        }
        val gains = (balance - invested).coerceAtLeast(0.0)
        Triple(balance, invested, gains)
    }

    val investedRatio = if (futureValue > 0) (totalInvested / futureValue).toFloat().coerceIn(0.05f, 0.95f) else 0.5f
    val gainsRatio = 1f - investedRatio

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = SleekPrimaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Estimated Wealth at Year ${years.toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", futureValue)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = SleekPrimary
                )

                Text(
                    text = "Total Value Accumulated",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(investedRatio)
                                .fillMaxHeight()
                                .background(SleekPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .weight(gainsRatio)
                                .fillMaxHeight()
                                .background(Color(0xFF10B981))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SleekPrimary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Invested: ${(investedRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gains: ${(gainsRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SleekBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Out-of-Pocket", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", totalInvested)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estimated Compounding Gain", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            "+$currencySymbol${String.format(Locale.getDefault(), "%,.0f", totalGains)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // Inputs Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Growth Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )

                OutlinedTextField(
                    value = initialDepositText,
                    onValueChange = { initialDepositText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Initial Lumpsum ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    )
                )

                OutlinedTextField(
                    value = monthlyContributionText,
                    onValueChange = { monthlyContributionText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monthly Contribution / SIP ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = annualInterestRateText,
                        onValueChange = { annualInterestRateText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Expected Return (% p.a.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        )
                    )

                    OutlinedTextField(
                        value = investmentYearsText,
                        onValueChange = { investmentYearsText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Duration (Years)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        )
                    )
                }

                // Quick Duration Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("3 Yrs" to "3", "5 Yrs" to "5", "10 Yrs" to "10", "15 Yrs" to "15", "25 Yrs" to "25").forEach { (label, yr) ->
                        val isSelected = investmentYearsText == yr
                        FilterChip(
                            selected = isSelected,
                            onClick = { investmentYearsText = yr },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyFundCalculatorTool(viewModel: FinanceViewModel) {
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val totalBalance = remember(accounts) { accounts.sumOf { it.balance } }
    val defaultLiquid = if (totalBalance > 0) String.format(Locale.US, "%.0f", totalBalance) else "50000"
    val defaultBurn = if (monthlyBudget > 0) String.format(Locale.US, "%.0f", monthlyBudget) else "15000"

    var currentSavingsText by remember { mutableStateOf(defaultLiquid) }
    var monthlyBurnText by remember { mutableStateOf(defaultBurn) }
    var targetMonthsSelection by remember { mutableIntStateOf(6) }

    val currentSavings = currentSavingsText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val monthlyBurn = monthlyBurnText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 1.0

    val currentRunwayMonths = if (monthlyBurn > 0) currentSavings / monthlyBurn else 0.0
    val targetFundNeeded = monthlyBurn * targetMonthsSelection
    val shortfallOrSurplus = currentSavings - targetFundNeeded
    val isFundFunded = shortfallOrSurplus >= 0

    val progressFraction = if (targetFundNeeded > 0) {
        (currentSavings / targetFundNeeded).toFloat().coerceIn(0f, 1f)
    } else 0f

    val (statusLabel, statusColor, statusDesc) = when {
        currentRunwayMonths < 1.0 -> Triple(
            "Critical Vulnerability",
            ExpenseRed,
            "Less than 1 month of living expenses saved. An unexpected expense could cause immediate distress."
        )
        currentRunwayMonths < 3.0 -> Triple(
            "Starter Safety Net",
            WarningOrange,
            "Under 3 months of runway. Suitable for minor emergencies, but vulnerable to job transitions."
        )
        currentRunwayMonths < 6.0 -> Triple(
            "Standard Stability",
            IncomeGreen,
            "Solid 3-6 month reserve covering essential obligations. Good protection against shocks."
        )
        else -> Triple(
            "Bulletproof Independence",
            Color(0xFF3B82F6),
            "6+ months of living costs secured. Provides high peace of mind and career flexibility."
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Runway Hero Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = String.format(Locale.getDefault(), "%.1f Months", currentRunwayMonths),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )

                Text(
                    text = "Current Living Runway",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar towards selected target months
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: $targetMonthsSelection Months ($currencySymbol${String.format(Locale.getDefault(), "%,.0f", targetFundNeeded)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekTextSecondary
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}% funded",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isFundFunded) IncomeGreen else statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(SleekBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isFundFunded) IncomeGreen else statusColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = statusDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Target Duration Chips & Input Parameters
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Target Runway Horizon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "3 Months" to 3,
                        "6 Months" to 6,
                        "9 Months" to 9,
                        "12 Months" to 12
                    ).forEach { (label, months) ->
                        val isSelected = targetMonthsSelection == months
                        FilterChip(
                            selected = isSelected,
                            onClick = { targetMonthsSelection = months },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                OutlinedTextField(
                    value = currentSavingsText,
                    onValueChange = { currentSavingsText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Current Liquid Savings ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    )
                )

                OutlinedTextField(
                    value = monthlyBurnText,
                    onValueChange = { monthlyBurnText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Monthly Essential Living Cost ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    )
                )
            }
        }

        // Savings Trajectory Plan Card
        if (!isFundFunded && targetFundNeeded > currentSavings) {
            val deficit = targetFundNeeded - currentSavings
            val save6m = deficit / 6.0
            val save12m = deficit / 12.0
            val save24m = deficit / 24.0

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Monthly Savings Required to Reach Target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TrajectoryBox("In 6 Months", "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", save6m)}/mo")
                        TrajectoryBox("In 12 Months", "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", save12m)}/mo")
                        TrajectoryBox("In 24 Months", "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", save24m)}/mo")
                    }
                }
            }
        }

        // Copy Plan Button
        OutlinedButton(
            onClick = {
                val summary = "Emergency Fund Analysis:\n" +
                        "• Current Runway: ${String.format(Locale.getDefault(), "%.1f", currentRunwayMonths)} months ($currencySymbol${String.format(Locale.getDefault(), "%,.0f", currentSavings)})\n" +
                        "• Target: $targetMonthsSelection months ($currencySymbol${String.format(Locale.getDefault(), "%,.0f", targetFundNeeded)})\n" +
                        "• Status: $statusLabel"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Emergency Fund Plan", summary))
                Toast.makeText(context, "Emergency fund summary copied!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = AppIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Emergency Plan to Clipboard")
        }
    }
}

@Composable
private fun TrajectoryBox(period: String, amountStr: String) {
    Surface(
        color = SleekBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(period, style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(amountStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SleekPrimary)
        }
    }
}

@Composable
private fun InflationCalculatorTool(viewModel: FinanceViewModel) {
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var initialAmountText by remember { mutableStateOf("100000") }
    var inflationRateText by remember { mutableStateOf("6.0") }
    var horizonYearsText by remember { mutableStateOf("10") }

    val amount = initialAmountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rate = inflationRateText.toDoubleOrNull()?.coerceIn(0.1, 50.0) ?: 6.0
    val years = horizonYearsText.toDoubleOrNull()?.coerceIn(1.0, 50.0) ?: 10.0

    val factor = (1.0 + (rate / 100.0)).pow(years)
    val futureRealValue = if (factor > 0) amount / factor else 0.0
    val purchasingPowerLost = (amount - futureRealValue).coerceAtLeast(0.0)
    val futureCostOfBasket = amount * factor
    val retainedPercentage = if (amount > 0) ((futureRealValue / amount) * 100.0).coerceIn(0.0, 100.0) else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Result Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, WarningOrange.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = WarningOrange.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Real Purchasing Power in ${years.toInt()} Years",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", futureRealValue)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = WarningOrange
                )

                Text(
                    text = "What $currencySymbol${String.format(Locale.getDefault(), "%,.0f", amount)} will actually buy (at ${String.format(Locale.getDefault(), "%.1f", rate)}% inflation)",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dual Decay Bar (Retained vs Eroded)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Retained Value (${retainedPercentage.toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Eroded (${(100.0 - retainedPercentage).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        val retainedWeight = (retainedPercentage / 100.0).toFloat().coerceIn(0.01f, 0.99f)
                        Box(
                            modifier = Modifier
                                .weight(retainedWeight)
                                .fillMaxHeight()
                                .background(IncomeGreen)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f - retainedWeight)
                                .fillMaxHeight()
                                .background(ExpenseRed)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Future Replacement Cost", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            "$currencySymbol${String.format(Locale.getDefault(), "%,.0f", futureCostOfBasket)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Purchasing Power Lost", style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                        Text(
                            "-$currencySymbol${String.format(Locale.getDefault(), "%,.0f", purchasingPowerLost)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }

        // Inputs Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Inflation Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )

                OutlinedTextField(
                    value = initialAmountText,
                    onValueChange = { initialAmountText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Today's Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    )
                )

                OutlinedTextField(
                    value = inflationRateText,
                    onValueChange = { inflationRateText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Annual Inflation Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("3%", "5%", "6%", "7%", "10%").forEach { rateStr ->
                        val clean = rateStr.removeSuffix("%")
                        FilterChip(
                            selected = inflationRateText == clean,
                            onClick = { inflationRateText = clean },
                            label = { Text(rateStr, fontSize = 11.sp) }
                        )
                    }
                }

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))

                Text(
                    text = "Time Horizon (Years)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekTextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("5 Yrs" to "5", "10 Yrs" to "10", "15 Yrs" to "15", "20 Yrs" to "20", "30 Yrs" to "30").forEach { (label, yr) ->
                        FilterChip(
                            selected = horizonYearsText == yr,
                            onClick = { horizonYearsText = yr },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Copy Inflation Breakdown Button
        OutlinedButton(
            onClick = {
                val summary = "Inflation Erosion Projection:\n" +
                        "• Initial: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", amount)}\n" +
                        "• After ${years.toInt()} Years at $rate% Inflation: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", futureRealValue)} real value\n" +
                        "• Cost to match today's basket: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", futureCostOfBasket)}"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Inflation Analysis", summary))
                Toast.makeText(context, "Inflation analysis copied!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = AppIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Inflation Analysis to Clipboard")
        }
    }
}

@Composable
fun CurrencyPickerDialog(
    title: String,
    currencies: List<CurrencyItem>,
    selectedCode: String,
    onSelect: (CurrencyItem) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        currencies.filter {
            query.isBlank() ||
                    it.country.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search currency...") },
                    leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = SleekTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filtered, key = { it.code + it.country }) { item ->
                        val isSelected = item.code.equals(selectedCode, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) SleekPrimary.copy(alpha = 0.12f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.code} (${item.symbol}) - ${item.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SleekPrimary else SleekTextPrimary
                                    )
                                    Text(item.country, style = MaterialTheme.typography.labelSmall, color = SleekTextSecondary)
                                }
                                if (isSelected) {
                                    Icon(AppIcons.Check, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SleekPrimary)
            }
        },
        containerColor = SleekSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
