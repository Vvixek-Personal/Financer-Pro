package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*

private enum class ExpenseSortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST("Highest"),
    LOWEST("Lowest")
}

@Composable
fun ExpensesTab(
    viewModel: FinanceViewModel,
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Expense) -> Unit
) {
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val selectedDateRange by viewModel.selectedDateRange.collectAsStateWithLifecycle()
    val categoryIcons by viewModel.categoryIcons.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val currencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val groupByDate by viewModel.groupByDate.collectAsStateWithLifecycle()
    val confirmTxDelete by viewModel.confirmTxDelete.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val isCompact = LocalCompactLayout.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME, TRANSFER
    var selectedAccountIdFilter by remember { mutableStateOf<Long?>(null) }
    var sortOrder by remember { mutableStateOf(ExpenseSortOrder.NEWEST) }

    val rawCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val categories = remember(rawCategories) { listOf("All") + rawCategories }

    var selectedExpenseIds by remember { mutableStateOf(setOf<Long>()) }
    var showDateRangePickerDialog by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    // Filtered & sorted expenses list
    val filteredExpenses = remember(
        expenses,
        searchQuery,
        selectedCategoryFilter,
        selectedTypeFilter,
        selectedAccountIdFilter,
        sortOrder
    ) {
        val filtered = expenses.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.note?.contains(searchQuery, ignoreCase = true) == true ||
                item.category.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "All" || item.category == selectedCategoryFilter
            val isTransferItem = item.category.equals("Transfer", ignoreCase = true) ||
                item.kind.startsWith("TRANSFER") || item.type == "TRANSFER"
            val matchesType = when (selectedTypeFilter) {
                "EXPENSE" -> item.type == "EXPENSE" && !isTransferItem
                "INCOME" -> item.type == "INCOME" && !isTransferItem
                "TRANSFER" -> isTransferItem
                else -> true
            }
            val matchesAccount = selectedAccountIdFilter == null || item.accountId == selectedAccountIdFilter
            matchesSearch && matchesCategory && matchesType && matchesAccount
        }

        when (sortOrder) {
            ExpenseSortOrder.NEWEST -> filtered.sortedByDescending { it.date }
            ExpenseSortOrder.OLDEST -> filtered.sortedBy { it.date }
            ExpenseSortOrder.HIGHEST -> filtered.sortedByDescending { it.amount }
            ExpenseSortOrder.LOWEST -> filtered.sortedBy { it.amount }
        }
    }

    val filteredIncomeTotal = remember(filteredExpenses, currencyCode) {
        filteredExpenses.realIncome(currencyCode)
    }
    val filteredExpenseTotal = remember(filteredExpenses, currencyCode) {
        filteredExpenses.realExpense(currencyCode)
    }
    val filteredNetTotal = filteredIncomeTotal - filteredExpenseTotal

    val dateFormatSetting = com.example.data.UiPrefs.dateFormat

    // Date grouping logic (Today, Yesterday, or user's formatted date)
    val groupedExpenses = remember(filteredExpenses, dateFormatSetting) {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = keyFmt.format(Date())
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val yesterdayStr = keyFmt.format(calYesterday.time)

        filteredExpenses.groupBy { expense ->
            val expenseDateStr = keyFmt.format(Date(expense.date))
            when (expenseDateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> com.example.data.UiPrefs.formatDate(expense.date)
            }
        }
    }

    val accountNamesMap = remember(accounts) { accounts.associate { it.id to it.name } }
    val hasActiveFilters = selectedCategoryFilter != "All" ||
        selectedTypeFilter != "ALL" ||
        selectedAccountIdFilter != null ||
        searchQuery.isNotEmpty() ||
        selectedDateRange != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = if (isCompact) 8.dp else 12.dp)
        ) {
            // Top Navigation Header: [Date Range] Transactions [Reset?] [Search]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showDateRangePickerDialog = true
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedDateRange != null) SleekPrimaryContainer
                            else SleekSurface.copy(alpha = 0.9f)
                        )
                        .testTag("expenses_date_range_button")
                ) {
                    Icon(
                        imageVector = AppIcons.DateRange,
                        contentDescription = "Filter by Date Range",
                        tint = if (selectedDateRange != null) SleekPrimary else SleekTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasActiveFilters) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategoryFilter = "All"
                                selectedTypeFilter = "ALL"
                                selectedAccountIdFilter = null
                                searchQuery = ""
                                isSearchActive = false
                                viewModel.setDateRange(null, null)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ExpenseRedBg)
                                .testTag("expenses_clear_filters_btn")
                        ) {
                            Icon(
                                imageVector = AppIcons.Clear,
                                contentDescription = "Reset Filters",
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSearchActive) SleekPrimaryContainer else SleekSurface.copy(alpha = 0.9f))
                            .testTag("expenses_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = AppIcons.Search,
                            contentDescription = "Search Transactions",
                            tint = if (isSearchActive) SleekPrimary else SleekTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar Input (Animated Toggle)
            AnimatedVisibility(visible = isSearchActive) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search transactions or tags...", color = SleekTextSecondary, fontSize = 14.sp) },
                        leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = SleekTextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(AppIcons.Close, contentDescription = "Clear", tint = SleekTextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = SleekShapes.input,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_search_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Type, Wallet & Sort Filter Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val typeOptions = listOf(
                    "ALL" to "All",
                    "EXPENSE" to "Expenses",
                    "INCOME" to "Income",
                    "TRANSFER" to "Transfers"
                )
                typeOptions.forEach { (key, label) ->
                    val isTypeSelected = selectedTypeFilter == key
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTypeFilter = key
                        },
                        shape = SleekShapes.chip,
                        color = if (isTypeSelected) SleekPrimary else SleekSurface,
                        border = if (!isTypeSelected) BorderStroke(1.dp, SleekBorder) else null,
                        modifier = Modifier.testTag("expenses_type_filter_${key.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            color = if (isTypeSelected) onAccentColor else SleekTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                if (accounts.isNotEmpty()) {
                    Box {
                        val activeAccountLabel = accounts.find { it.id == selectedAccountIdFilter }?.name ?: "All Wallets"
                        val isAccActive = selectedAccountIdFilter != null
                        Surface(
                            onClick = { showAccountMenu = true },
                            shape = SleekShapes.chip,
                            color = if (isAccActive) SleekPrimaryContainer else SleekSurface,
                            border = BorderStroke(1.dp, if (isAccActive) SleekPrimary else SleekBorder),
                            modifier = Modifier.testTag("expenses_account_filter_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isAccActive) SleekPrimary else SleekTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activeAccountLabel,
                                    color = if (isAccActive) SleekPrimary else SleekTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = AppIcons.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (isAccActive) SleekPrimary else SleekTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showAccountMenu,
                            onDismissRequest = { showAccountMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Wallets", fontWeight = if (selectedAccountIdFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedAccountIdFilter = null
                                    showAccountMenu = false
                                }
                            )
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name, fontWeight = if (selectedAccountIdFilter == acc.id) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedAccountIdFilter = acc.id
                                        showAccountMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Box {
                    Surface(
                        onClick = { showSortMenu = true },
                        shape = SleekShapes.chip,
                        color = SleekSurface,
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.testTag("expenses_sort_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.SwapVert,
                                contentDescription = "Sort",
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = sortOrder.label,
                                color = SleekTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        ExpenseSortOrder.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, fontWeight = if (sortOrder == option) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    sortOrder = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it }) { cat ->
                    val selected = selectedCategoryFilter == cat
                    val chipBg = if (selected) SleekPrimary else SleekSurface
                    val chipText = if (selected) onAccentColor else SleekTextSecondary
                    val chipBorder = if (!selected) BorderStroke(1.dp, SleekBorder) else null

                    Box(
                        modifier = Modifier
                            .clip(SleekShapes.chip)
                            .background(chipBg)
                            .then(if (chipBorder != null) Modifier.border(chipBorder, SleekShapes.chip) else Modifier)
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            color = chipText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Live Cashflow Summary Strip for current filters
            if (filteredExpenses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val masked = com.example.data.UiPrefs.maskRowAmounts
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SleekSurface,
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expenses_summary_strip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${filteredExpenses.size} items",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                            if (selectedDateRange != null) {
                                Text(
                                    text = "${com.example.data.UiPrefs.formatDate(selectedDateRange!!.first)} – ${com.example.data.UiPrefs.formatDate(selectedDateRange!!.second)}",
                                    fontSize = 10.sp,
                                    color = SleekPrimary
                                )
                            } else {
                                Text(
                                    text = "Filtered total",
                                    fontSize = 10.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Inflow", fontSize = 10.sp, color = SleekTextSecondary)
                                Text(
                                    text = if (masked) "$currencySymbol •••" else com.example.data.UiPrefs.formatAmount(filteredIncomeTotal, currencySymbol, showPlusSign = true),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Outflow", fontSize = 10.sp, color = SleekTextSecondary)
                                Text(
                                    text = if (masked) "$currencySymbol •••" else com.example.data.UiPrefs.formatAmount(filteredExpenseTotal, currencySymbol, negativeSign = true),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net", fontSize = 10.sp, color = SleekTextSecondary)
                                Text(
                                    text = if (masked) "$currencySymbol •••" else com.example.data.UiPrefs.formatSignedAmount(filteredNetTotal, currencySymbol, if (filteredNetTotal >= 0) "+" else "-"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filteredNetTotal >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDarkModeActive) SleekPrimary.copy(alpha = 0.18f)
                                    else SleekPrimary.copy(alpha = 0.10f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasActiveFilters) AppIcons.FilterList else AppIcons.ReceiptLong,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = if (hasActiveFilters) "Nothing matches these filters" else "No transactions yet",
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasActiveFilters)
                                "Try clearing the search or picking a different filter."
                            else
                                "Add your first income or expense and your history will appear here.",
                            color = SleekTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        if (hasActiveFilters) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedCategoryFilter = "All"
                                    selectedTypeFilter = "ALL"
                                    selectedAccountIdFilter = null
                                    searchQuery = ""
                                    viewModel.setDateRange(null, null)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.6f))
                            ) {
                                Text("Clear filters", color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onAddExpenseClick,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Icon(AppIcons.Add, contentDescription = null, tint = onAccentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Transaction", color = onAccentColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 10.dp)
                ) {
                    if (groupByDate && (sortOrder == ExpenseSortOrder.NEWEST || sortOrder == ExpenseSortOrder.OLDEST)) {
                        groupedExpenses.forEach { (dateHeader, txList) ->
                            item(key = "header_$dateHeader") {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SleekTextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            items(txList, key = { it.id }) { expense ->
                                val isSelected = selectedExpenseIds.contains(expense.id)
                                Image1TransactionRow(
                                    expense = expense,
                                    isSelected = isSelected,
                                    categoryIcons = categoryIcons,
                                    currencySymbol = currencySymbol,
                                    accountName = accountNamesMap[expense.accountId],
                                    onLongClick = {
                                        selectedExpenseIds = if (isSelected) {
                                            selectedExpenseIds - expense.id
                                        } else {
                                            selectedExpenseIds + expense.id
                                        }
                                    },
                                    onClick = {
                                        if (selectedExpenseIds.isNotEmpty()) {
                                            selectedExpenseIds = if (isSelected) {
                                                selectedExpenseIds - expense.id
                                            } else {
                                                selectedExpenseIds + expense.id
                                            }
                                        } else {
                                            onEditExpenseClick(expense)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        items(filteredExpenses, key = { it.id }) { expense ->
                            val isSelected = selectedExpenseIds.contains(expense.id)
                            Image1TransactionRow(
                                expense = expense,
                                isSelected = isSelected,
                                categoryIcons = categoryIcons,
                                currencySymbol = currencySymbol,
                                accountName = accountNamesMap[expense.accountId],
                                onLongClick = {
                                    selectedExpenseIds = if (isSelected) {
                                        selectedExpenseIds - expense.id
                                    } else {
                                        selectedExpenseIds + expense.id
                                    }
                                },
                                onClick = {
                                    if (selectedExpenseIds.isNotEmpty()) {
                                        selectedExpenseIds = if (isSelected) {
                                            selectedExpenseIds - expense.id
                                        } else {
                                            selectedExpenseIds + expense.id
                                        }
                                    } else {
                                        onEditExpenseClick(expense)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Selected Expense Action Bar (Multi-Select & Delete/Edit)
        if (selectedExpenseIds.isNotEmpty()) {
            val selectedExpenses = filteredExpenses.filter { selectedExpenseIds.contains(it.id) }
            if (selectedExpenses.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekPrimary),
                    shape = SleekShapes.dialog,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedExpenses.size} items selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onAccentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedExpenses.size == 1) {
                                IconButton(
                                    onClick = {
                                        onEditExpenseClick(selectedExpenses.first())
                                        selectedExpenseIds = emptySet()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(onAccentColor.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(AppIcons.Edit, contentDescription = "Edit", tint = onAccentColor, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (confirmTxDelete) {
                                        showBatchDeleteConfirm = true
                                    } else {
                                        viewModel.deleteExpenses(selectedExpenses)
                                        selectedExpenseIds = emptySet()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ExpenseRedBg, CircleShape)
                            ) {
                                Icon(AppIcons.Delete, contentDescription = "Delete Selected", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { selectedExpenseIds = emptySet() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(onAccentColor.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(AppIcons.Close, contentDescription = "Deselect All", tint = onAccentColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (showBatchDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showBatchDeleteConfirm = false },
                        title = {
                            Text("Delete ${selectedExpenses.size} Transactions?", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to delete ${selectedExpenses.size} selected transactions? You can undo within 5 seconds.",
                                color = SleekTextSecondary
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showBatchDeleteConfirm = false
                                    viewModel.deleteExpenses(selectedExpenses)
                                    selectedExpenseIds = emptySet()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatchDeleteConfirm = false }) {
                                Text("Cancel", color = SleekTextSecondary)
                            }
                        },
                        containerColor = SleekSurface,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        if (showDateRangePickerDialog) {
            DateRangePickerDialog(
                initialStartMs = selectedDateRange?.first,
                initialEndMs = selectedDateRange?.second,
                onDismiss = { showDateRangePickerDialog = false },
                onClearRange = {
                    viewModel.setDateRange(null, null)
                    showDateRangePickerDialog = false
                },
                onSelectRange = { start: Long, end: Long ->
                    viewModel.setDateRange(start, end)
                    showDateRangePickerDialog = false
                }
            )
        }
    }
}

/**
 * Transaction Row Card honoring showTransactionTags, maskRowAmounts, showCents,
 * longPressToEdit, compactLayout, and user dateFormat preferences.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Image1TransactionRow(
    expense: Expense,
    isSelected: Boolean,
    categoryIcons: Map<String, String> = emptyMap(),
    currencySymbol: String = "₹",
    accountName: String? = null,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val isTransfer = expense.category.equals("Transfer", ignoreCase = true) || expense.kind.startsWith("TRANSFER") || expense.type == "TRANSFER"
    val isIncome = !isTransfer && expense.type == "INCOME"
    val displayName = expense.note?.takeIf { it.isNotBlank() } ?: expense.category
    val isCompact = LocalCompactLayout.current

    val accentColor = when {
        isTransfer -> Color(0xFF0284C7)
        isIncome -> IncomeGreen
        else -> ExpenseRed
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = SleekSurface
        ),
        shape = SleekShapes.card,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) BorderStroke(1.5.dp, SleekPrimary) else BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (com.example.data.UiPrefs.longPressToEdit) onLongClick else null
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 11.dp else SleekSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val catIcon = getCategoryIcon(expense.category, categoryIcons)
            val catColor = if (isIncome) IncomeGreen else getCategoryColor(expense.category)
            val iconBg = if (isTransfer) Color(0xFF0284C7).copy(alpha = 0.15f) else catColor.copy(alpha = 0.15f)
            val iconBorder = if (isTransfer) Color(0xFF0284C7).copy(alpha = 0.30f) else catColor.copy(alpha = 0.28f)
            val iconTint = if (isTransfer) Color(0xFF0284C7) else catColor

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
                    .border(BorderStroke(1.dp, iconBorder), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTransfer) AppIcons.Sync else catIcon,
                    contentDescription = expense.category,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val statusLabel = when {
                        isTransfer -> "Transfer"
                        isIncome -> "Received"
                        else -> "Paid"
                    }
                    val dateLabel = com.example.data.UiPrefs.formatDate(expense.date)
                    val metaText = buildString {
                        append(statusLabel)
                        if (com.example.data.UiPrefs.showTransactionTags && !accountName.isNullOrBlank()) {
                            append(" · ")
                            append(accountName)
                        }
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = AppIcons.Schedule,
                        contentDescription = null,
                        tint = SleekTextSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val sign = when {
                isTransfer -> if (expense.kind == "TRANSFER_IN") "+" else "-"
                isIncome -> "+"
                else -> "-"
            }
            Text(
                text = if (com.example.data.UiPrefs.maskRowAmounts) {
                    "$currencySymbol ••••"
                } else {
                    com.example.data.UiPrefs.formatSignedAmount(expense.amount, currencySymbol, sign)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
fun DateRangePickerDialog(
    initialStartMs: Long? = null,
    initialEndMs: Long? = null,
    onDismiss: () -> Unit,
    onClearRange: (() -> Unit)? = null,
    onSelectRange: (Long, Long) -> Unit
) {
    fun normalizeStart(ms: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun normalizeEnd(ms: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    var startCalendar by remember {
        mutableStateOf(
            if (initialStartMs != null) normalizeStart(initialStartMs)
            else normalizeStart(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis)
        )
    }
    var endCalendar by remember {
        mutableStateOf(
            normalizeEnd(initialEndMs ?: System.currentTimeMillis())
        )
    }

    val dateFormatter = remember(com.example.data.UiPrefs.dateFormat) { com.example.data.UiPrefs.formatter() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().testTag("date_range_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (onClearRange != null && (initialStartMs != null || initialEndMs != null)) {
                        TextButton(onClick = onClearRange) {
                            Text("All Time", color = ExpenseRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Quick Range Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "Today" to 0,
                        "Last 7D" to 6,
                        "Last 30D" to 29,
                        "This Month" to -1
                    )
                    presets.forEach { (label, daysBack) ->
                        Surface(
                            onClick = {
                                val now = System.currentTimeMillis()
                                endCalendar = normalizeEnd(now)
                                startCalendar = if (daysBack == -1) {
                                    Calendar.getInstance().apply {
                                        set(Calendar.DAY_OF_MONTH, 1)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                } else {
                                    Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, -daysBack)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                }
                            },
                            shape = SleekShapes.chip,
                            color = SleekPrimaryContainer,
                            border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SleekPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Start & End Stepper Controls
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SleekBg,
                    border = BorderStroke(1.dp, SleekBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("From:", color = SleekTextSecondary, fontSize = 13.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        startCalendar = (startCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(AppIcons.ChevronLeft, contentDescription = "Previous day", tint = SleekTextPrimary, modifier = Modifier.size(18.dp))
                                }
                                Text(dateFormatter.format(startCalendar.time), color = SleekTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = {
                                        val next = (startCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                                        if (next.timeInMillis <= endCalendar.timeInMillis) {
                                            startCalendar = next
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(AppIcons.ChevronRight, contentDescription = "Next day", tint = SleekTextPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = SleekBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("To:", color = SleekTextSecondary, fontSize = 13.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val prev = (endCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                                        if (prev.timeInMillis >= startCalendar.timeInMillis) {
                                            endCalendar = prev
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(AppIcons.ChevronLeft, contentDescription = "Previous day", tint = SleekTextPrimary, modifier = Modifier.size(18.dp))
                                }
                                Text(dateFormatter.format(endCalendar.time), color = SleekTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = {
                                        endCalendar = (endCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(AppIcons.ChevronRight, contentDescription = "Next day", tint = SleekTextPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSelectRange(startCalendar.timeInMillis, endCalendar.timeInMillis)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply", color = onAccentColor)
                    }
                }
            }
        }
    }
}

