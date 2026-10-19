package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class CalendarDisplayMode {
    MONTH, WEEK, AGENDA
}

enum class CalendarTransactionFilter {
    ALL, EXPENSE_ONLY, INCOME_ONLY
}

/**
 * Supercharged & Animated Financial Calendar Experience:
 * - Smooth AnimatedContent transitions across Month, Week, and Agenda modes
 * - Week view with fluid week swiping, automatic month transition, and blank edge cells
 * - All Transactions stream in Agenda view with Month/Date grouping & filtering
 * - Tapping Month Name opens the Full Month Transactions Ledger Modal Sheet
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EnhancedCalendarTab(
    expenses: List<Expense>,
    categoryIcons: Map<String, String> = emptyMap(),
    currencySymbol: String = "₹",
    currencyCode: String = "INR",
    billsList: List<BillEntry> = emptyList(),
    onAddExpenseForDate: (Long, String) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val today = remember { Calendar.getInstance() }

    val fontScale = androidx.compose.ui.platform.LocalConfiguration.current.fontScale
    val calendarAspectRatio = if (fontScale > 1.3f) 0.72f else if (fontScale > 1.1f) 0.85f else 1f

    // 2000 to March 2027+
    val initialYear = 2000
    val maxCalendarLimit = remember(today.get(Calendar.DAY_OF_MONTH)) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2027)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 31)
            if (today.get(Calendar.DAY_OF_MONTH) >= 7) {
                add(Calendar.MONTH, 1)
            }
        }
    }

    val currentMonthIndex = (today.get(Calendar.YEAR) - initialYear) * 12 + today.get(Calendar.MONTH)
    val maxMonthIndex = (maxCalendarLimit.get(Calendar.YEAR) - initialYear) * 12 + maxCalendarLimit.get(Calendar.MONTH)

    val pagerState = rememberPagerState(
        initialPage = currentMonthIndex,
        pageCount = { maxMonthIndex + 1 }
    )

    val activeYear = initialYear + pagerState.currentPage / 12
    val activeMonth = pagerState.currentPage % 12
    val canGoForward = pagerState.currentPage < maxMonthIndex

    var displayMode by remember { mutableStateOf(CalendarDisplayMode.MONTH) }
    var txFilter by remember { mutableStateOf(CalendarTransactionFilter.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchKeyword by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedDayOfMonth by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }
    var showMonthTransactionsSheet by remember { mutableStateOf(false) }
    var weekSlideDirection by remember { mutableIntStateOf(1) }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val selectedDateMillis = remember(activeYear, activeMonth, selectedDayOfMonth) {
        val maxDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, activeYear)
            set(Calendar.MONTH, activeMonth)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        Calendar.getInstance().apply {
            set(Calendar.YEAR, activeYear)
            set(Calendar.MONTH, activeMonth)
            set(Calendar.DAY_OF_MONTH, selectedDayOfMonth.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Monthly transactions for currently active month
    val monthExpenses = remember(expenses, activeYear, activeMonth) {
        expenses.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.YEAR) == activeYear && c.get(Calendar.MONTH) == activeMonth
        }
    }

    val monthIncome = remember(monthExpenses, currencyCode) { monthExpenses.realIncome(currencyCode) }
    val monthExpense = remember(monthExpenses, currencyCode) { monthExpenses.realExpense(currencyCode) }
    val monthNet = monthIncome - monthExpense

    // Animated monthly numbers
    val animatedMonthIncome by animateFloatAsState(
        targetValue = monthIncome.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "monthIncomeAnim"
    )
    val animatedMonthExpense by animateFloatAsState(
        targetValue = monthExpense.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "monthExpenseAnim"
    )
    val animatedMonthNet by animateFloatAsState(
        targetValue = monthNet.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "monthNetAnim"
    )

    val daysWithExpenses = remember(monthExpenses) {
        monthExpenses.map {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.DAY_OF_MONTH)
        }.distinct().size
    }
    val avgDailySpend = if (daysWithExpenses > 0) monthExpense / daysWithExpenses else 0.0

    // Selected day transactions
    val selectedDayExpenses = remember(expenses, activeYear, activeMonth, selectedDayOfMonth, txFilter, selectedCategoryFilter, searchKeyword) {
        expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            val isSameDay = cal.get(Calendar.YEAR) == activeYear &&
                    cal.get(Calendar.MONTH) == activeMonth &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedDayOfMonth
            if (!isSameDay) return@filter false

            val matchesType = when (txFilter) {
                CalendarTransactionFilter.ALL -> true
                CalendarTransactionFilter.EXPENSE_ONLY -> it.type != "INCOME" && it.category != "Locked Savings"
                CalendarTransactionFilter.INCOME_ONLY -> it.type == "INCOME"
            }
            val matchesCategory = selectedCategoryFilter == null || it.category == selectedCategoryFilter
            val matchesSearch = searchKeyword.isBlank() ||
                    it.category.contains(searchKeyword, ignoreCase = true) ||
                    (it.note?.contains(searchKeyword, ignoreCase = true) == true)

            matchesType && matchesCategory && matchesSearch
        }
    }

    val selectedDayTotalIncome = remember(selectedDayExpenses, currencyCode) { selectedDayExpenses.realIncome(currencyCode) }
    val selectedDayTotalExpense = remember(selectedDayExpenses, currencyCode) { selectedDayExpenses.realExpense(currencyCode) }

    // Selected day upcoming bills
    val selectedDayBills = remember(billsList, activeYear, activeMonth, selectedDayOfMonth) {
        val targetDayStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDayOfMonth, activeMonth + 1, activeYear)
        billsList.filter { bill -> bill.dueDate == targetDayStr }
    }

    // All available categories in current month for filter chips
    val monthCategories = remember(monthExpenses) {
        monthExpenses.map { it.category }.distinct().sorted()
    }

    val currentDisplayCal = remember(activeYear, activeMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, activeYear)
            set(Calendar.MONTH, activeMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val currentMonthTitle = remember(activeYear, activeMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentDisplayCal.time)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // TOP HEADER: Title + View Mode Switch + Today + Search
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Calendar",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Swipe weeks • Double-tap day • Tap month name",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                var todayPressed by remember { mutableStateOf(false) }
                val todayScale by animateFloatAsState(
                    targetValue = if (todayPressed) 0.92f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "todayScale"
                )

                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        todayPressed = true
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(currentMonthIndex)
                            todayPressed = false
                        }
                        selectedDayOfMonth = today.get(Calendar.DAY_OF_MONTH)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SleekPrimary.copy(alpha = 0.15f),
                        contentColor = SleekPrimary
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .scale(todayScale)
                ) {
                    Icon(AppIcons.Today, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isSearchActive = !isSearchActive
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSearchActive) SleekPrimary else SleekSurface)
                        .border(1.dp, SleekBorder, RoundedCornerShape(10.dp))
                ) {
                    AnimatedContent(
                        targetState = isSearchActive,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "searchIconAnim"
                    ) { active ->
                        Icon(
                            imageVector = if (active) AppIcons.Close else AppIcons.Search,
                            contentDescription = "Search",
                            tint = if (active) Color.White else SleekTextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                OutlinedTextField(
                    value = searchKeyword,
                    onValueChange = { searchKeyword = it },
                    placeholder = { Text("Search transactions in calendar...", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(AppIcons.Search, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchKeyword.isNotEmpty()) {
                            IconButton(onClick = { searchKeyword = "" }) {
                                Icon(AppIcons.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface,
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // VIEW MODE SWITCHER (Month | Week | Agenda)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SleekSurface,
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    CalendarDisplayMode.MONTH to "Month",
                    CalendarDisplayMode.WEEK to "Week",
                    CalendarDisplayMode.AGENDA to "Agenda"
                ).forEach { (mode, label) ->
                    val isSelected = displayMode == mode
                    val animatedBg by animateColorAsState(
                        targetValue = if (isSelected) SleekPrimary else Color.Transparent,
                        animationSpec = tween(250),
                        label = "pillColor"
                    )
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else SleekTextSecondary,
                        animationSpec = tween(250),
                        label = "pillTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(animatedBg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                displayMode = mode
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedTextColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MONTHLY OVERVIEW CASHFLOW RIBBON
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SleekSurface,
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(IncomeGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Income", fontSize = 11.sp, color = SleekTextSecondary)
                    }
                    Text(
                        text = "+$currencySymbol${String.format(Locale.getDefault(), "%,.0f", animatedMonthIncome)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ExpenseRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expense", fontSize = 11.sp, color = SleekTextSecondary)
                    }
                    Text(
                        text = "-$currencySymbol${String.format(Locale.getDefault(), "%,.0f", animatedMonthExpense)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (monthNet >= 0) SleekPrimary else Color(0xFFF59E0B)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Net", fontSize = 11.sp, color = SleekTextSecondary)
                    }
                    Text(
                        text = "${if (animatedMonthNet >= 0) "+" else "-"}$currencySymbol${String.format(Locale.getDefault(), "%,.0f", abs(animatedMonthNet))}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (animatedMonthNet >= 0) SleekPrimary else Color(0xFFF59E0B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ANIMATED CONTENT SWITCHER BETWEEN MODES (Month / Week / Agenda)
        AnimatedContent(
            targetState = displayMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) + slideInVertically { it / 6 })
                    .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically { -it / 6 })
            },
            label = "calendarDisplayModeAnim"
        ) { targetMode ->
            when (targetMode) {
                CalendarDisplayMode.AGENDA -> {
                    AgendaAllTransactionsView(
                        allExpenses = expenses,
                        currentMonthExpenses = monthExpenses,
                        currentMonthTitle = currentMonthTitle,
                        currencySymbol = currencySymbol,
                        currencyCode = currencyCode,
                        categoryIcons = categoryIcons,
                        onEditExpense = onEditExpense,
                        onDeleteExpense = onDeleteExpense
                    )
                }
                CalendarDisplayMode.MONTH, CalendarDisplayMode.WEEK -> {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Month Navigation Header with Interactive Month Name Tap Sheet
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (pagerState.currentPage > 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                            selectedDayOfMonth = 1
                                        }
                                    }
                                ) {
                                    Icon(AppIcons.ChevronLeft, contentDescription = "Prev Month", tint = SleekPrimary)
                                }

                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showMonthTransactionsSheet = true
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = SleekSurfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, SleekBorder)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AnimatedContent(
                                                targetState = currentMonthTitle,
                                                transitionSpec = {
                                                    (fadeIn(animationSpec = tween(220)) + slideInVertically { it / 4 })
                                                        .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutVertically { -it / 4 })
                                                },
                                                label = "monthYearAnim"
                                            ) { monthStr ->
                                                Text(
                                                    text = monthStr,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = SleekTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Icon(
                                                imageVector = AppIcons.ReceiptLong,
                                                contentDescription = "View Month Transactions",
                                                tint = SleekPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = "Tap to view ${monthExpenses.size} transactions • Daily Avg: $currencySymbol${String.format(Locale.getDefault(), "%,.0f", avgDailySpend)}",
                                            fontSize = 10.sp,
                                            color = SleekPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (canGoForward) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                            selectedDayOfMonth = 1
                                        }
                                    },
                                    enabled = canGoForward
                                ) {
                                    Icon(
                                        imageVector = AppIcons.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = if (canGoForward) SleekPrimary else SleekTextSecondary.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Days of week header
                            Row(modifier = Modifier.fillMaxWidth()) {
                                daysOfWeek.forEach { day ->
                                    Text(
                                        text = day,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (day == "Sun" || day == "Sat") SleekPrimary else SleekTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // PAGER (Supports Month / Week view internally)
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val pageYear = initialYear + page / 12
                                val pageMonth = page % 12

                                val firstDayCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, pageYear)
                                    set(Calendar.MONTH, pageMonth)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                }
                                val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
                                val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                val dayOffset = firstDayOfWeek - 1

                                if (targetMode == CalendarDisplayMode.WEEK) {
                                    // RULE: "in week one use the same logic of old one swiping just change the day and would automatically when changed into another month and do not show week 1 week 2 buttons. also add animations"
                                    val selectedSlot = (selectedDayOfMonth - 1) + dayOffset
                                    val targetRow = selectedSlot / 7

                                    val goToNextWeek: () -> Unit = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        weekSlideDirection = 1
                                        val nextDay = selectedDayOfMonth + 7
                                        if (nextDay <= daysInMonth) {
                                            selectedDayOfMonth = nextDay
                                        } else {
                                            if (canGoForward) {
                                                val overflowDay = nextDay - daysInMonth
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                                val nextMonthCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, pageYear)
                                                    set(Calendar.MONTH, pageMonth + 1)
                                                    set(Calendar.DAY_OF_MONTH, 1)
                                                }
                                                val maxInNext = nextMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                                selectedDayOfMonth = overflowDay.coerceIn(1, maxInNext)
                                            }
                                        }
                                    }

                                    val goToPrevWeek: () -> Unit = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        weekSlideDirection = -1
                                        val prevDay = selectedDayOfMonth - 7
                                        if (prevDay >= 1) {
                                            selectedDayOfMonth = prevDay
                                        } else {
                                            if (pagerState.currentPage > 0) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                }
                                                val prevMonthCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, pageYear)
                                                    set(Calendar.MONTH, pageMonth - 1)
                                                    set(Calendar.DAY_OF_MONTH, 1)
                                                }
                                                val maxInPrev = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                                val underflowDay = maxInPrev + prevDay
                                                selectedDayOfMonth = underflowDay.coerceIn(1, maxInPrev)
                                            }
                                        }
                                    }

                                    var dragAccumulator by remember { mutableFloatStateOf(0f) }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(page, targetRow, selectedDayOfMonth) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = { dragAccumulator = 0f },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragAccumulator += dragAmount
                                                    },
                                                    onDragEnd = {
                                                        if (dragAccumulator < -40f) {
                                                            goToNextWeek()
                                                        } else if (dragAccumulator > 40f) {
                                                            goToPrevWeek()
                                                        }
                                                        dragAccumulator = 0f
                                                    },
                                                    onDragCancel = { dragAccumulator = 0f }
                                                )
                                            }
                                    ) {
                                        // Swipe navigation header row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = goToPrevWeek,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(AppIcons.ChevronLeft, contentDescription = "Prev Week", tint = SleekPrimary)
                                            }

                                            Text(
                                                text = "Swipe left/right to navigate weeks",
                                                fontSize = 11.sp,
                                                color = SleekTextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )

                                            IconButton(
                                                onClick = goToNextWeek,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(AppIcons.ChevronRight, contentDescription = "Next Week", tint = SleekPrimary)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Animated sliding week row
                                        AnimatedContent(
                                            targetState = Triple(page, targetRow, selectedDayOfMonth),
                                            transitionSpec = {
                                                if (weekSlideDirection > 0) {
                                                    (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 2 } + fadeIn())
                                                        .togetherWith(slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width -> -width / 2 } + fadeOut())
                                                } else {
                                                    (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 2 } + fadeIn())
                                                        .togetherWith(slideOutHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { width -> width / 2 } + fadeOut())
                                                }
                                            },
                                            label = "weekSwipeRowAnim"
                                        ) { (_, currentRow, _) ->
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                for (col in 0 until 7) {
                                                    val slotIndex = currentRow * 7 + col
                                                    val dayNum = slotIndex - dayOffset + 1

                                                    // RULE: make it blank on days when ended and starting of month rather than showing dates from past or next month
                                                    if (dayNum in 1..daysInMonth) {
                                                        RenderAnimatedDayCell(
                                                            dayNum = dayNum,
                                                            daysInMonth = daysInMonth,
                                                            pageYear = pageYear,
                                                            pageMonth = pageMonth,
                                                            activeYear = activeYear,
                                                            activeMonth = activeMonth,
                                                            selectedDayOfMonth = selectedDayOfMonth,
                                                            calendarAspectRatio = calendarAspectRatio,
                                                            expenses = expenses,
                                                            billsList = billsList,
                                                            today = today,
                                                            currencyCode = currencyCode,
                                                            onSelectDay = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                selectedDayOfMonth = it
                                                            },
                                                            onDoubleClickDay = { day ->
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                selectedDayOfMonth = day
                                                                val clickedDate = Calendar.getInstance().apply {
                                                                    set(Calendar.YEAR, pageYear)
                                                                    set(Calendar.MONTH, pageMonth)
                                                                    set(Calendar.DAY_OF_MONTH, day)
                                                                }.timeInMillis
                                                                onAddExpenseForDate(clickedDate, "EXPENSE")
                                                            }
                                                        )
                                                    } else {
                                                        // Blank edge slot
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .aspectRatio(calendarAspectRatio)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Full Month view
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (row in 0 until 6) {
                                            val firstDayInRow = row * 7 - dayOffset + 1
                                            if (firstDayInRow > daysInMonth) break

                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                for (col in 0 until 7) {
                                                    val slotIndex = row * 7 + col
                                                    val dayNum = slotIndex - dayOffset + 1

                                                    if (dayNum in 1..daysInMonth) {
                                                        RenderAnimatedDayCell(
                                                            dayNum = dayNum,
                                                            daysInMonth = daysInMonth,
                                                            pageYear = pageYear,
                                                            pageMonth = pageMonth,
                                                            activeYear = activeYear,
                                                            activeMonth = activeMonth,
                                                            selectedDayOfMonth = selectedDayOfMonth,
                                                            calendarAspectRatio = calendarAspectRatio,
                                                            expenses = expenses,
                                                            billsList = billsList,
                                                            today = today,
                                                            currencyCode = currencyCode,
                                                            onSelectDay = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                selectedDayOfMonth = it
                                                                if (page != pagerState.currentPage) {
                                                                    coroutineScope.launch {
                                                                        pagerState.animateScrollToPage(page)
                                                                    }
                                                                }
                                                            },
                                                            onDoubleClickDay = { day ->
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                selectedDayOfMonth = day
                                                                val clickedDate = Calendar.getInstance().apply {
                                                                    set(Calendar.YEAR, pageYear)
                                                                    set(Calendar.MONTH, pageMonth)
                                                                    set(Calendar.DAY_OF_MONTH, day)
                                                                }.timeInMillis
                                                                onAddExpenseForDate(clickedDate, "EXPENSE")
                                                            }
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .aspectRatio(calendarAspectRatio)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // FILTER CHIPS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = txFilter == CalendarTransactionFilter.ALL && selectedCategoryFilter == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    txFilter = CalendarTransactionFilter.ALL
                    selectedCategoryFilter = null
                },
                label = { Text("All (${selectedDayExpenses.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SleekPrimary,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = txFilter == CalendarTransactionFilter.EXPENSE_ONLY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    txFilter = if (txFilter == CalendarTransactionFilter.EXPENSE_ONLY) CalendarTransactionFilter.ALL else CalendarTransactionFilter.EXPENSE_ONLY
                },
                label = { Text("Expenses", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ExpenseRed,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = txFilter == CalendarTransactionFilter.INCOME_ONLY,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    txFilter = if (txFilter == CalendarTransactionFilter.INCOME_ONLY) CalendarTransactionFilter.ALL else CalendarTransactionFilter.INCOME_ONLY
                },
                label = { Text("Income", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IncomeGreen,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            )

            monthCategories.take(6).forEach { cat ->
                val isSelected = selectedCategoryFilter == cat
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedCategoryFilter = if (isSelected) null else cat
                    },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SleekPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = SleekPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SELECTED DAY SUMMARY CARD
        val selectedDateStr = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedDateStr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Spent: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", selectedDayTotalExpense)}",
                                fontSize = 11.sp,
                                color = if (selectedDayTotalExpense > 0) ExpenseRed else SleekTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            if (selectedDayTotalIncome > 0) {
                                Text(
                                    text = "Earned: +$currencySymbol${String.format(Locale.getDefault(), "%,.2f", selectedDayTotalIncome)}",
                                    fontSize = 11.sp,
                                    color = IncomeGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddExpenseForDate(selectedDateMillis, "EXPENSE")
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ExpenseRed.copy(alpha = 0.15f),
                                contentColor = ExpenseRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Expense", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAddExpenseForDate(selectedDateMillis, "INCOME")
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = IncomeGreen.copy(alpha = 0.15f),
                                contentColor = IncomeGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Income", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // UPCOMING BILLS ON THIS DAY
        AnimatedVisibility(
            visible = selectedDayBills.isNotEmpty(),
            enter = expandVertically(animationSpec = spring()) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedDayBills.forEach { bill ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bill Due Today: ${bill.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SleekTextPrimary
                                )
                                Text(
                                    text = "Amount: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", bill.amount)}",
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SELECTED DAY TRANSACTIONS LIST
        AnimatedContent(
            targetState = selectedDayExpenses.isEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "txListAnim"
        ) { isEmpty ->
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = AppIcons.EventBusy,
                            contentDescription = null,
                            tint = SleekTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions on this date",
                            color = SleekTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+ Expense' or '+ Income' above to log",
                            color = SleekTextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedDayExpenses.forEach { expense ->
                        val isIncome = expense.type == "INCOME"
                        val catColor = if (isIncome) {
                            when (expense.category) {
                                "Salary" -> IncomeGreen
                                "Freelance" -> Color(0xFF0D9488)
                                "Investments" -> Color(0xFF3B82F6)
                                "Gifts" -> Color(0xFFEC4899)
                                else -> IncomeGreen
                            }
                        } else {
                            categoryColors[expense.category] ?: SleekPrimary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SleekSurface)
                                .border(1.dp, SleekBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onEditExpense(expense)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(catColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getCategoryEmoji(expense.category, categoryIcons),
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = expense.note?.takeIf { it.isNotBlank() } ?: (if (isIncome) "Income" else "Expense"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = expense.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%s%s%,.2f", if (isIncome) "+" else "-", currencySymbol, expense.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isIncome) IncomeGreen else ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteExpense(expense)
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Delete,
                                    contentDescription = "Delete",
                                    tint = ExpenseRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }

    if (showMonthTransactionsSheet) {
        MonthTransactionsModalSheet(
            monthTitle = currentMonthTitle,
            monthExpenses = monthExpenses,
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            categoryIcons = categoryIcons,
            onDismiss = { showMonthTransactionsSheet = false },
            onEditExpense = onEditExpense,
            onDeleteExpense = onDeleteExpense,
            onAddExpense = {
                showMonthTransactionsSheet = false
                onAddExpenseForDate(selectedDateMillis, "EXPENSE")
            }
        )
    }
}

/**
 * 📋 Agenda View displaying ALL transactions across the entire history
 */
@Composable
fun AgendaAllTransactionsView(
    allExpenses: List<Expense>,
    currentMonthExpenses: List<Expense>,
    currentMonthTitle: String,
    currencySymbol: String,
    currencyCode: String = "INR",
    categoryIcons: Map<String, String>,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var agendaFilterMode by remember { mutableStateOf("ALL") }
    var agendaSearchQuery by remember { mutableStateOf("") }

    val filteredList = remember(allExpenses, currentMonthExpenses, agendaFilterMode, agendaSearchQuery) {
        val baseList = when (agendaFilterMode) {
            "THIS_MONTH" -> currentMonthExpenses
            "EXPENSE" -> allExpenses.filter { it.type != "INCOME" && it.category != "Locked Savings" }
            "INCOME" -> allExpenses.filter { it.type == "INCOME" }
            else -> allExpenses
        }

        if (agendaSearchQuery.isBlank()) {
            baseList.sortedByDescending { it.date }
        } else {
            baseList.filter {
                it.category.contains(agendaSearchQuery, ignoreCase = true) ||
                        (it.note?.contains(agendaSearchQuery, ignoreCase = true) == true)
            }.sortedByDescending { it.date }
        }
    }

    val totalIncome = remember(filteredList, currencyCode) { filteredList.realIncome(currencyCode) }
    val totalExpense = remember(filteredList, currencyCode) { filteredList.realExpense(currencyCode) }
    val netCashflow = totalIncome - totalExpense

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Full Agenda Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "Showing all transactions across complete history",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SleekPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${filteredList.size} entries",
                        fontSize = 11.sp,
                        color = SleekPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SleekSurfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Earned: +$currencySymbol${String.format(Locale.getDefault(), "%,.0f", totalIncome)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                    Text(
                        text = "Spent: -$currencySymbol${String.format(Locale.getDefault(), "%,.0f", totalExpense)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    Text(
                        text = "Net: ${if (netCashflow >= 0) "+" else "-"}$currencySymbol${String.format(Locale.getDefault(), "%,.0f", abs(netCashflow))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netCashflow >= 0) SleekPrimary else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = agendaSearchQuery,
                onValueChange = { agendaSearchQuery = it },
                placeholder = { Text("Filter all transactions in agenda...", fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(AppIcons.Search, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    if (agendaSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { agendaSearchQuery = "" }) {
                            Icon(AppIcons.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SleekSurfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = SleekSurfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = SleekPrimary,
                    unfocusedBorderColor = SleekBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "All (${allExpenses.size})",
                    "THIS_MONTH" to "$currentMonthTitle (${currentMonthExpenses.size})",
                    "EXPENSE" to "Expenses",
                    "INCOME" to "Income"
                ).forEach { (key, label) ->
                    val isSelected = agendaFilterMode == key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            agendaFilterMode = key
                        },
                        label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SleekPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = AppIcons.EventBusy,
                            contentDescription = null,
                            tint = SleekTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No transactions found in this view",
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                val groupedByMonth = remember(filteredList) {
                    val map = linkedMapOf<String, MutableList<Expense>>()
                    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    filteredList.forEach { tx ->
                        val monthKey = monthFmt.format(Date(tx.date))
                        map.getOrPut(monthKey) { mutableListOf() }.add(tx)
                    }
                    map
                }

                groupedByMonth.forEach { (monthName, monthTxns) ->
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SleekPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = monthName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary
                                )
                                Text(
                                    text = "${monthTxns.size} transactions",
                                    fontSize = 10.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val dayFmt = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                        val groupedByDay = monthTxns.groupBy { dayFmt.format(Date(it.date)) }

                        groupedByDay.forEach { (dayLabel, dayTxns) ->
                            Text(
                                text = dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SleekTextSecondary,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)
                            )

                            dayTxns.forEach { tx ->
                                val isInc = tx.type == "INCOME"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SleekBg)
                                        .border(1.dp, SleekBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onEditExpense(tx)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(getCategoryEmoji(tx.category, categoryIcons), fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.note?.takeIf { it.isNotBlank() } ?: tx.category,
                                            fontSize = 12.sp,
                                            color = SleekTextPrimary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tx.category} • ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.date))}",
                                            fontSize = 10.sp,
                                            color = SleekTextSecondary
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.getDefault(), "%s%s%,.2f", if (isInc) "+" else "-", currencySymbol, tx.amount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isInc) IncomeGreen else ExpenseRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDeleteExpense(tx)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Delete,
                                            contentDescription = "Delete",
                                            tint = ExpenseRed.copy(alpha = 0.6f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📊 Modal Bottom Sheet displaying all transactions of the active month
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthTransactionsModalSheet(
    monthTitle: String,
    monthExpenses: List<Expense>,
    currencySymbol: String,
    currencyCode: String = "INR",
    categoryIcons: Map<String, String>,
    onDismiss: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onAddExpense: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterType by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(monthExpenses, filterType, searchQuery) {
        monthExpenses.filter { tx ->
            val matchesType = when (filterType) {
                "EXPENSE" -> tx.type != "INCOME" && tx.category != "Locked Savings"
                "INCOME" -> tx.type == "INCOME"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true)
            matchesType && matchesSearch
        }.sortedByDescending { it.date }
    }

    val monthIncome = remember(monthExpenses, currencyCode) { monthExpenses.realIncome(currencyCode) }
    val monthExpense = remember(monthExpenses, currencyCode) { monthExpenses.realExpense(currencyCode) }
    val monthNet = monthIncome - monthExpense

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SleekSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = SleekBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxHeight(0.85f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SleekPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${monthExpenses.size} Total",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Complete monthly transaction ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekSurfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income", fontSize = 10.sp, color = SleekTextSecondary)
                        Text(
                            text = "+$currencySymbol${String.format(Locale.getDefault(), "%,.0f", monthIncome)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(SleekBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Expense", fontSize = 10.sp, color = SleekTextSecondary)
                        Text(
                            text = "-$currencySymbol${String.format(Locale.getDefault(), "%,.0f", monthExpense)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(SleekBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Net Balance", fontSize = 10.sp, color = SleekTextSecondary)
                        Text(
                            text = "${if (monthNet >= 0) "+" else "-"}$currencySymbol${String.format(Locale.getDefault(), "%,.0f", abs(monthNet))}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (monthNet >= 0) SleekPrimary else Color(0xFFF59E0B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search this month...", fontSize = 12.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(AppIcons.Search, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(AppIcons.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SleekSurfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = SleekSurfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = SleekPrimary,
                    unfocusedBorderColor = SleekBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All (${monthExpenses.size})",
                    "EXPENSE" to "Expenses",
                    "INCOME" to "Income"
                ).forEach { (type, label) ->
                    val isSelected = filterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            filterType = type
                        },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SleekPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = AppIcons.EventBusy,
                            contentDescription = null,
                            tint = SleekTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No transactions found in this month", color = SleekTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddExpense,
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Transaction", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { tx ->
                        val isInc = tx.type == "INCOME"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SleekBg)
                                .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onEditExpense(tx)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(getCategoryEmoji(tx.category, categoryIcons), fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.note?.takeIf { it.isNotBlank() } ?: tx.category,
                                    fontSize = 12.sp,
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tx.category} • ${SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault()).format(Date(tx.date))}",
                                    fontSize = 10.sp,
                                    color = SleekTextSecondary
                                )
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%s%s%,.2f", if (isInc) "+" else "-", currencySymbol, tx.amount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInc) IncomeGreen else ExpenseRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteExpense(tx)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Delete,
                                    contentDescription = "Delete",
                                    tint = ExpenseRed.copy(alpha = 0.65f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Day Cell
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.RenderAnimatedDayCell(
    dayNum: Int,
    daysInMonth: Int,
    pageYear: Int,
    pageMonth: Int,
    activeYear: Int,
    activeMonth: Int,
    selectedDayOfMonth: Int,
    calendarAspectRatio: Float,
    expenses: List<Expense>,
    billsList: List<BillEntry>,
    today: Calendar,
    currencyCode: String = "INR",
    onSelectDay: (Int) -> Unit,
    onDoubleClickDay: (Int) -> Unit
) {
    if (dayNum in 1..daysInMonth) {
        val isSelected = selectedDayOfMonth == dayNum && pageYear == activeYear && pageMonth == activeMonth
        val isToday = today.get(Calendar.YEAR) == pageYear &&
                today.get(Calendar.MONTH) == pageMonth &&
                today.get(Calendar.DAY_OF_MONTH) == dayNum

        val dayExpenses = expenses.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.YEAR) == pageYear &&
                    c.get(Calendar.MONTH) == pageMonth &&
                    c.get(Calendar.DAY_OF_MONTH) == dayNum
        }

        val targetDayStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayNum, pageMonth + 1, pageYear)
        val hasBillDue = billsList.any { bill -> bill.dueDate == targetDayStr }

        val dayIncome = dayExpenses.realIncome(currencyCode)
        val dayExpense = dayExpenses.realExpense(currencyCode)
        val hasTransactions = dayExpenses.isNotEmpty()
        val isProfit = hasTransactions && dayIncome >= dayExpense

        val targetScale = if (isSelected) 1.035f else 1.0f
        val animatedScale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "dayScale"
        )

        val animatedBgColor by animateColorAsState(
            targetValue = when {
                isSelected -> SleekPrimary
                isToday -> SleekPrimaryContainer.copy(alpha = 0.5f)
                else -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 200),
            label = "dayBgColor"
        )

        val animatedBorderColor by animateColorAsState(
            targetValue = when {
                isSelected -> Color.Transparent
                isToday -> SleekPrimary
                else -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 200),
            label = "dayBorderColor"
        )

        val animatedTextColor by animateColorAsState(
            targetValue = when {
                isSelected -> Color.White
                isToday -> SleekPrimary
                else -> SleekTextPrimary
            },
            animationSpec = tween(durationMillis = 200),
            label = "dayTextColor"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(calendarAspectRatio)
                .padding(2.dp)
                .scale(animatedScale)
                .clip(RoundedCornerShape(12.dp))
                .background(animatedBgColor)
                .border(
                    width = 1.dp,
                    color = animatedBorderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .combinedClickable(
                    onClick = { onSelectDay(dayNum) },
                    onDoubleClick = { onDoubleClickDay(dayNum) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = dayNum.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = animatedTextColor
                )

                if (hasTransactions || hasBillDue) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasTransactions) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> Color.White
                                            isProfit -> IncomeGreen
                                            else -> ExpenseRed
                                        }
                                    )
                            )
                        }
                        if (hasBillDue) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFFDE68A) else Color(0xFFF59E0B))
                            )
                        }
                    }
                }
            }
        }
    } else {
        // RULE: Blank cell on days when ended and starting of month rather than showing dates from past or next month
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(calendarAspectRatio)
        )
    }
}
