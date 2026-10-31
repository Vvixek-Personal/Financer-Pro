package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*

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

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedAccountIdFilter by remember { mutableStateOf<Int?>(null) }
    
    val rawCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val categories = remember(rawCategories) { listOf("All") + rawCategories }

    var selectedExpenseIds by remember { mutableStateOf(setOf<Long>()) }
    var showDateRangePickerDialog by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    // Filtered expenses list
    val filteredExpenses = expenses.filter {
        val matchesSearch = it.note?.contains(searchQuery, ignoreCase = true) == true ||
                it.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "All" || it.category == selectedCategoryFilter
        matchesSearch && matchesCategory
    }

    // Date grouping logic (Today, Yesterday, 19 November, etc.)
    val groupedExpenses = remember(filteredExpenses) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calYesterday.time)

        filteredExpenses.groupBy { expense ->
            val expenseDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(expense.date))
            when (expenseDateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(expense.date))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Navigation Header: [<] Transactions [Search]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedDateRange != null || selectedAccountIdFilter != null || searchQuery.isNotEmpty()) {
                            viewModel.setDateRange(null, null)
                            selectedAccountIdFilter = null
                            searchQuery = ""
                            isSearchActive = false
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekSurface.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = SleekTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { isSearchActive = !isSearchActive },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekSurface.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = "Search Transactions",
                        tint = SleekTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar Input (Animated Toggle)
            AnimatedVisibility(visible = isSearchActive) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search transactions...", color = SleekTextSecondary, fontSize = 14.sp) },
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
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it }) { cat ->
                    val selected = selectedCategoryFilter == cat
                    val chipBg = if (selected) SleekPrimary else SleekSurface
                    val chipText = if (selected) Color.White else SleekTextSecondary
                    val chipBorder = if (!selected) BorderStroke(1.dp, SleekBorder) else null

                    Box(
                        modifier = Modifier
                            .clip(SleekShapes.chip)
                            .background(chipBg)
                            .then(if (chipBorder != null) Modifier.border(chipBorder, SleekShapes.chip) else Modifier)
                            .clickable { selectedCategoryFilter = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SleekSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.ReceiptLong,
                                    contentDescription = null,
                                    tint = SleekTextSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = "No transactions found",
                            color = SleekTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedExpenses.forEach { (dateHeader, txList) ->
                        item {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.labelLarge,
                                color = SleekTextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        items(txList, key = { it.id }) { expense ->
                            val isSelected = selectedExpenseIds.contains(expense.id)
                            Image1TransactionRow(
                                expense = expense,
                                isSelected = isSelected,
                                categoryIcons = categoryIcons,
                                currencySymbol = currencySymbol,
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
                            color = Color.White,
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
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(AppIcons.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteExpenses(selectedExpenses)
                                    selectedExpenseIds = emptySet()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(ExpenseRed.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(AppIcons.Delete, contentDescription = "Delete Selected", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { selectedExpenseIds = emptySet() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(AppIcons.Close, contentDescription = "Deselect All", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showDateRangePickerDialog) {
            DateRangePickerDialog(
                onDismiss = { showDateRangePickerDialog = false },
                onSelectRange = { start: Long, end: Long ->
                    viewModel.setDateRange(start, end)
                    showDateRangePickerDialog = false
                }
            )
        }
    }
}

/**
 * Image 1 Inspired Transaction Row Card:
 * Soft surface squircle, circular avatar/logo on left, title & Received/Paid status in middle,
 * clean green (+$5,710.20) or red (-$124.55) amount on right.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Image1TransactionRow(
    expense: Expense,
    isSelected: Boolean,
    categoryIcons: Map<String, String> = emptyMap(),
    currencySymbol: String = "₹",
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val isTransfer = expense.category.equals("Transfer", ignoreCase = true) || expense.kind.startsWith("TRANSFER") || expense.type == "TRANSFER"
    val isIncome = !isTransfer && expense.type == "INCOME"
    val displayName = expense.note?.takeIf { it.isNotBlank() } ?: expense.category

    val accentColor = when {
        isTransfer -> Color(0xFF0284C7)
        isIncome -> IncomeGreen
        else -> ExpenseRed
    }
    val accentBg = when {
        isTransfer -> Color(0xFF0284C7).copy(alpha = 0.15f)
        isIncome -> IncomeGreenBg
        else -> SleekBg
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = SleekSurface
        ),
        shape = SleekShapes.card,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) BorderStroke(1.dp, SleekPrimary) else BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SleekSpacing.cardPadding),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isTransfer -> "Transfer"
                            isIncome -> "Received"
                            else -> "Paid"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = AppIcons.Schedule,
                        contentDescription = null,
                        tint = SleekTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            val sign = when {
                isTransfer -> if (expense.kind == "TRANSFER_IN") "+" else "-"
                isIncome -> "+"
                else -> "-"
            }
            Text(
                text = String.format("%s%s%,.2f", sign, currencySymbol, expense.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onSelectRange: (Long, Long) -> Unit
) {
    var startCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }
    var endCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        })
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().testTag("date_range_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Date Range",
                    style = MaterialTheme.typography.titleMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )

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
                            Text(dateFormatter.format(startCalendar.time), color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = SleekBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("To:", color = SleekTextSecondary, fontSize = 13.sp)
                            Text(dateFormatter.format(endCalendar.time), color = SleekTextPrimary, fontWeight = FontWeight.Bold)
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
                        Text("Apply", color = Color.White)
                    }
                }
            }
        }
    }
}
