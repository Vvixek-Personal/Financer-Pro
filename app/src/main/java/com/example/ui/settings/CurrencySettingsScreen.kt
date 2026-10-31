package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Money
import java.util.Locale

/**
 * Dedicated Currency & Exchange Rates Configuration Screen (Rule 6 & 7)
 * - 100+ countries/currencies searchable by Country, Name, Code, and Symbol.
 * - Option A (Keep Existing) vs Option B (Convert Existing with live rate & confirmation).
 * - Default Currency for Statistics selector.
 * - Exchange rate status & local cache management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val currentCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val currentSymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val currentName by viewModel.selectedCurrencyName.collectAsStateWithLifecycle()
    val statsCode by viewModel.statsCurrencyCode.collectAsStateWithLifecycle()
    val statsSymbol by viewModel.statsCurrencySymbol.collectAsStateWithLifecycle()
    val statsName by viewModel.statsCurrencyName.collectAsStateWithLifecycle()
    val isAutoUpdate by viewModel.isAutoExchangeRateUpdateEnabled.collectAsStateWithLifecycle()
    val lastUpdate by viewModel.lastExchangeRateUpdate.collectAsStateWithLifecycle()
    val isUpdatingRates by viewModel.isUpdatingExchangeRates.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }
    var pendingCurrencyChange by remember { mutableStateOf<CurrencyItem?>(null) }
    var showStatsCurrencyPicker by remember { mutableStateOf(false) }

    val allCurrencies = remember { CurrencyManager.currencies }

    val filteredCurrencies = remember(searchQuery, selectedRegion) {
        allCurrencies.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.country.contains(searchQuery, ignoreCase = true) ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.code.contains(searchQuery, ignoreCase = true) ||
                    item.symbol.contains(searchQuery, ignoreCase = true)

            val matchesRegion = when (selectedRegion) {
                "All" -> true
                "Popular" -> item.code in listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED")
                "Asia" -> item.country in listOf("India", "Japan", "China", "Hong Kong", "Singapore", "South Korea", "Bangladesh", "Pakistan", "Sri Lanka", "Nepal", "Indonesia", "Malaysia", "Thailand", "Philippines", "Vietnam")
                "Europe" -> item.country in listOf("European Union", "United Kingdom", "Switzerland", "Sweden", "Norway", "Denmark", "Poland", "Czech Republic", "Hungary", "Romania", "Croatia", "Iceland")
                "Americas" -> item.country in listOf("United States", "Canada", "Brazil", "Mexico", "Argentina", "Chile", "Colombia", "Peru", "Costa Rica", "Dominican Republic", "Jamaica")
                "Middle East & Africa" -> item.country in listOf("United Arab Emirates", "Saudi Arabia", "South Africa", "Egypt", "Nigeria", "Kenya", "Qatar", "Kuwait", "Morocco", "Ethiopia")
                else -> true
            }

            matchesQuery && matchesRegion
        }
    }

    Scaffold(
        topBar = {
            SettingsHeaderTitle(title = "Currency & Rates", onBack = onBack)
        },
        containerColor = SleekBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header summary cards
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Active Currency Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Default Transaction Currency Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Surface(
                                    color = IncomeGreenBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "TRANSACTIONS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val currentItem = CurrencyManager.getByCode(currentCode)
                                    Text(text = currentItem.flag, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "$currentCode ($currentSymbol)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextPrimary
                                        )
                                        Text(
                                            text = currentName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SleekTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Statistics Default Currency Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface),
                            border = BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStatsCurrencyPicker = true }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = SleekPrimaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "STATISTICS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = AppIcons.Edit,
                                        contentDescription = "Change Stats Currency",
                                        tint = SleekTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statsItem = CurrencyManager.getByCode(statsCode)
                                    Text(text = statsItem.flag, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "$statsCode ($statsSymbol)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextPrimary
                                        )
                                        Text(
                                            text = statsName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SleekTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live Rates & Offline Sync section (Rule 7)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = AppIcons.Sync,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Exchange Rates Engine",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Cached offline. Rates auto-refresh every ~10 days.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary
                                )
                                Text(
                                    text = "Last synced: $lastUpdate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.refreshExchangeRates() },
                                enabled = !isUpdatingRates,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isUpdatingRates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = SleekPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = AppIcons.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = SleekBorder, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-update exchange rates",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekTextPrimary
                            )
                            Switch(
                                checked = isAutoUpdate,
                                onCheckedChange = { viewModel.toggleAutoExchangeRateUpdate(it) }
                            )
                        }
                    }
                }
            }

            // Search Bar & Filter chips
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Choose Default Currency (100+ Available)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_search_input"),
                        placeholder = { Text("Search by country, name, code, symbol...") },
                        leadingIcon = {
                            Icon(AppIcons.Search, contentDescription = "Search", tint = SleekTextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(AppIcons.Clear, contentDescription = "Clear", tint = SleekTextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Region Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Popular", "Asia", "Europe", "Americas", "Middle East & Africa").forEach { region ->
                            val isSelected = selectedRegion == region
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRegion = region },
                                label = { Text(region, fontSize = 12.sp) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = SleekSurface,
                                    labelColor = SleekTextPrimary
                                ),
                                border = BorderStroke(1.dp, if (isSelected) SleekPrimary else SleekBorder)
                            )
                        }
                    }
                }
            }

            // Currencies List
            items(filteredCurrencies, key = { it.code + it.country }) { currency ->
                val isCurrentDefault = currency.code.equals(currentCode, ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentDefault) SleekPrimary.copy(alpha = 0.08f) else SleekSurface
                    ),
                    border = BorderStroke(
                        width = if (isCurrentDefault) 1.5.dp else 1.dp,
                        color = if (isCurrentDefault) SleekPrimary else SleekBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            if (!isCurrentDefault) {
                                pendingCurrencyChange = currency
                            }
                        }
                        .testTag("currency_item_${currency.code}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currency.flag, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currency.country,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SleekTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = SleekPrimaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = currency.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = currency.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "1 USD = ${String.format(Locale.getDefault(), "%,.2f", currency.rateToUsd)} ${currency.code}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekTextSecondary.copy(alpha = 0.75f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCurrentDefault) SleekPrimary else SleekBorder.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currency.symbol,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isCurrentDefault) Color.White else SleekTextPrimary
                                    )
                                }
                            }
                            if (isCurrentDefault) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Current",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Option A vs Option B Currency Conversion Dialog (Rule 6)
    val pendingChange = pendingCurrencyChange
    if (pendingChange != null) {
        val target = pendingChange
        var selectedOption by remember { mutableStateOf("KEEP") } // "KEEP" or "CONVERT"

        val currentRate = remember(currentCode, target.code) {
            val oneMajor = Money.factor(currentCode).toLong()
            when (val res = Money.tryConvert(oneMajor, currentCode, target.code)) {
                is Money.ConversionResult.Success -> String.format(Locale.US, "%,.4f", res.rateUsed)
                else -> "N/A"
            }
        }

        AlertDialog(
            onDismissRequest = { pendingCurrencyChange = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = target.flag, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Currency to ${target.code}?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "${if (CurrencyManager.areRatesFresh()) "Current Rate" else "Reference estimate (refresh for current rate)"}: 1 $currentCode = $currentRate ${target.code}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "How should existing transactions be handled?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Option A: Keep Existing Transactions
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedOption == "KEEP") SleekPrimary.copy(alpha = 0.08f) else SleekSurface
                        ),
                        border = BorderStroke(
                            width = if (selectedOption == "KEEP") 1.5.dp else 1.dp,
                            color = if (selectedOption == "KEEP") SleekPrimary else SleekBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = "KEEP" }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = selectedOption == "KEEP",
                                onClick = { selectedOption = "KEEP" },
                                colors = RadioButtonDefaults.colors(selectedColor = SleekPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Option A — Keep Existing Values",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "Existing transactions keep their original numbers and history. Only new transactions will use ${target.code} (${target.symbol}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option B: Convert Existing Transactions
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedOption == "CONVERT") SleekPrimary.copy(alpha = 0.08f) else SleekSurface
                        ),
                        border = BorderStroke(
                            width = if (selectedOption == "CONVERT") 1.5.dp else 1.dp,
                            color = if (selectedOption == "CONVERT") SleekPrimary else SleekBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = "CONVERT" }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(
                                selected = selectedOption == "CONVERT",
                                onClick = { selectedOption = "CONVERT" },
                                colors = RadioButtonDefaults.colors(selectedColor = SleekPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Option B — Convert Transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "Recalculates past amounts to ${target.code} using exchange rate (1 $currentCode = $currentRate ${target.code}). Recommended for accurate net balances.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val convert = (selectedOption == "CONVERT")
                        viewModel.updateDefaultCurrency(
                            code = target.code,
                            symbol = target.symbol,
                            name = target.name,
                            convertExisting = convert
                        )
                        pendingCurrencyChange = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Apply & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCurrencyChange = null }) {
                    Text("Cancel", color = SleekTextSecondary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Default Currency for Statistics Picker Dialog
    if (showStatsCurrencyPicker) {
        AlertDialog(
            onDismissRequest = { showStatsCurrencyPicker = false },
            title = {
                Text(
                    text = "Default Currency for Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Select the currency used when aggregating total expenses, category charts, and trend metrics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val quickCurrencies = remember {
                        allCurrencies.filter { it.code in listOf("INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "SGD", "AED", "SAR") }
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(quickCurrencies, key = { it.code }) { item ->
                            val isSelected = item.code.equals(statsCode, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SleekPrimary.copy(alpha = 0.12f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateStatsCurrency(item.code, item.symbol, item.name)
                                        showStatsCurrencyPicker = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${item.code} (${item.symbol}) - ${item.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SleekPrimary else SleekTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = AppIcons.Check,
                                            contentDescription = null,
                                            tint = SleekPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsCurrencyPicker = false }) {
                    Text("Close", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
