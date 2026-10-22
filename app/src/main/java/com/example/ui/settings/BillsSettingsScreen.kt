package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BillsSettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val defaultRecurrence by viewModel.billDefaultRecurrence.collectAsStateWithLifecycle()
    val recurringEnd by viewModel.billRecurringEnd.collectAsStateWithLifecycle()
    val defaultCategory by viewModel.billDefaultCategory.collectAsStateWithLifecycle()
    val autoMarkPaid by viewModel.billAutoMarkPaid.collectAsStateWithLifecycle()
    val archiveDays by viewModel.billArchiveDays.collectAsStateWithLifecycle()

    val showUpcomingDashboard by viewModel.billShowUpcomingDashboard.collectAsStateWithLifecycle()
    val upcomingDays by viewModel.billUpcomingDays.collectAsStateWithLifecycle()
    val sortOrder by viewModel.billSortOrder.collectAsStateWithLifecycle()

    val defaultFilter by viewModel.billDefaultFilter.collectAsStateWithLifecycle()
    val overdueAlert by viewModel.billOverdueAlert.collectAsStateWithLifecycle()
    val showNotes by viewModel.billShowNotes.collectAsStateWithLifecycle()

    var activePickerTitle by remember { mutableStateOf<String?>(null) }
    var pickerOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSelectedValue by remember { mutableStateOf("") }
    var onOptionSelected by remember { mutableStateOf<((String) -> Unit)?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Surface(
            color = SleekSurface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, SleekBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = SleekTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Bills & Reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "Manage recurring bills, due dates & cash flow forecast",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        var selectedTab by remember { mutableIntStateOf(0) }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SleekSurface,
            contentColor = SleekPrimary,
            divider = { HorizontalDivider(color = SleekBorder) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "⚙️ Preferences",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) SleekPrimary else SleekTextSecondary
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "📅 Cash Flow Calendar",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) SleekPrimary else SleekTextSecondary
                    )
                }
            )
        }

        if (selectedTab == 1) {
            CashFlowCalendarView(viewModel = viewModel)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // ================= SECTION 1: Bills Preferences =================
            BillsSettingSectionCard(
                sectionIcon = AppIcons.AccountBalanceWallet,
                sectionIconBg = Color(0xFFEFF0FE),
                sectionIconTint = Color(0xFF4F46E5),
                title = "Bills Preferences",
                subtitle = "Configure how bills are handled in the app"
            ) {
                BillsSettingItemRow(
                    icon = AppIcons.CalendarToday,
                    iconBg = Color(0xFFEBF5FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Default Recurrence",
                    subtitle = "Set default recurrence for new bills",
                    valueText = defaultRecurrence,
                    onClick = {
                        activePickerTitle = "Default Recurrence"
                        pickerOptions = listOf("Daily", "Weekly", "Monthly", "Yearly")
                        currentSelectedValue = defaultRecurrence
                        onOptionSelected = { viewModel.updateBillDefaultRecurrence(it) }
                    }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingItemRow(
                    icon = AppIcons.CalendarMonth,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF059669),
                    title = "Recurring End",
                    subtitle = "When should recurring bills stop",
                    valueText = recurringEnd,
                    onClick = {
                        activePickerTitle = "Recurring End"
                        pickerOptions = listOf("Never", "After 6 Months", "After 12 Months", "After 24 Months")
                        currentSelectedValue = recurringEnd
                        onOptionSelected = { viewModel.updateBillRecurringEnd(it) }
                    }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingItemRow(
                    icon = AppIcons.Sell,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    title = "Default Bill Category",
                    subtitle = "Category selected for new bills",
                    valueText = defaultCategory,
                    onClick = {
                        activePickerTitle = "Default Bill Category"
                        pickerOptions = listOf("Utilities", "Rent", "Subscriptions", "Credit Card", "Insurance", "Education", "Other")
                        currentSelectedValue = defaultCategory
                        onOptionSelected = { viewModel.updateBillDefaultCategory(it) }
                    }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingSwitchRow(
                    icon = AppIcons.TaskAlt,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Mark as Paid Automatically",
                    subtitle = "Auto mark recurring bills as paid on due date",
                    checked = autoMarkPaid,
                    onCheckedChange = { viewModel.toggleBillAutoMarkPaid(it) }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingItemRow(
                    icon = AppIcons.Archive,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    title = "Archive Paid Bills",
                    subtitle = "Move paid bills to archive after",
                    valueText = archiveDays,
                    onClick = {
                        activePickerTitle = "Archive Paid Bills"
                        pickerOptions = listOf("Immediately", "7 Days", "30 Days", "60 Days", "90 Days", "Never")
                        currentSelectedValue = archiveDays
                        onOptionSelected = { viewModel.updateBillArchiveDays(it) }
                    }
                )
            }

            // ================= SECTION 2: Upcoming Bills =================
            BillsSettingSectionCard(
                sectionIcon = AppIcons.FormatListBulleted,
                sectionIconBg = Color(0xFFE0F2FE),
                sectionIconTint = Color(0xFF0284C7),
                title = "Upcoming Bills",
                subtitle = "Control how upcoming bills are shown"
            ) {
                BillsSettingSwitchRow(
                    icon = AppIcons.GridView,
                    iconBg = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFEA580C),
                    title = "Show Upcoming Bills on Dashboard",
                    subtitle = "Display upcoming bills on home dashboard",
                    checked = showUpcomingDashboard,
                    onCheckedChange = { viewModel.toggleBillShowUpcomingDashboard(it) }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingItemRow(
                    icon = AppIcons.DateRange,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Upcoming Days to Show",
                    subtitle = "Number of days to show upcoming bills",
                    valueText = upcomingDays,
                    onClick = {
                        activePickerTitle = "Upcoming Days to Show"
                        pickerOptions = listOf("3 Days", "7 Days", "14 Days", "30 Days")
                        currentSelectedValue = upcomingDays
                        onOptionSelected = { viewModel.updateBillUpcomingDays(it) }
                    }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingItemRow(
                    icon = AppIcons.SwapVert,
                    iconBg = Color(0xFFEBF5FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Sort Upcoming Bills By",
                    subtitle = "Choose the default sorting order",
                    valueText = sortOrder,
                    onClick = {
                        activePickerTitle = "Sort Upcoming Bills By"
                        pickerOptions = listOf("Due Date (Nearest)", "Amount (High to Low)", "Amount (Low to High)", "Bill Name")
                        currentSelectedValue = sortOrder
                        onOptionSelected = { viewModel.updateBillSortOrder(it) }
                    }
                )
            }

            // ================= SECTION 3: Bill Management =================
            BillsSettingSectionCard(
                sectionIcon = AppIcons.SnippetFolder,
                sectionIconBg = Color(0xFFDCFCE7),
                sectionIconTint = Color(0xFF16A34A),
                title = "Bill Management",
                subtitle = "Manage how bills are organized and displayed"
            ) {
                BillsSettingItemRow(
                    icon = AppIcons.FilterList,
                    iconBg = Color(0xFFCFFAFE),
                    iconTint = Color(0xFF0891B2),
                    title = "Default Filter",
                    subtitle = "Default filter when opening bills",
                    valueText = defaultFilter,
                    onClick = {
                        activePickerTitle = "Default Filter"
                        pickerOptions = listOf("All Bills", "Unpaid", "Paid", "Overdue")
                        currentSelectedValue = defaultFilter
                        onOptionSelected = { viewModel.updateBillDefaultFilter(it) }
                    }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingSwitchRow(
                    icon = AppIcons.AccessTime,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    title = "Overdue Bills",
                    subtitle = "Move overdue bills to top",
                    checked = overdueAlert,
                    onCheckedChange = { viewModel.toggleBillOverdueAlert(it) }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                BillsSettingSwitchRow(
                    icon = AppIcons.StickyNote2,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    title = "Show Notes on Bill List",
                    subtitle = "Display bill notes in the list",
                    checked = showNotes,
                    onCheckedChange = { viewModel.toggleBillShowNotes(it) }
                )
            }

            // ================= SECTION 4: About Bills & Reminders =================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6FE)),
                border = BorderStroke(1.dp, Color(0xFFE2E4FA)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, Color(0xFF4F46E5)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "i",
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "About Bills & Reminders",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3544C4),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "These settings only affect how bills are managed and displayed in the app.\nNo alerts, notifications or external reminders are sent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B5563),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

    if (activePickerTitle != null) {
        AlertDialog(
            onDismissRequest = { activePickerTitle = null },
            title = {
                Text(
                    activePickerTitle ?: "",
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            },
            text = {
                Column {
                    pickerOptions.forEach { opt ->
                        val selected = opt == currentSelectedValue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected?.invoke(opt)
                                    activePickerTitle = null
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                opt,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) SleekPrimary else SleekTextPrimary
                            )
                            if (selected) {
                                Icon(
                                    AppIcons.Check,
                                    contentDescription = null,
                                    tint = SleekPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activePickerTitle = null }) {
                    Text("Cancel", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun BillsSettingSectionCard(
    sectionIcon: ImageVector,
    sectionIconBg: Color,
    sectionIconTint: Color,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = sectionIconBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = sectionIcon,
                            contentDescription = null,
                            tint = sectionIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

            content()
        }
    }
}

@Composable
private fun BillsSettingItemRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    valueText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconBg,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SleekTextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SleekPrimary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = SleekPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BillsSettingSwitchRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconBg,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SleekTextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SleekPrimary
            )
        )
    }
}

// ==========================================
// 📅 30-DAY CASH FLOW & SUBSCRIPTION CALENDAR
// ==========================================

@Composable
fun CashFlowCalendarView(viewModel: FinanceViewModel) {
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val recurringRules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val billsCount = viewModel.billsList.size

    val projections = remember(billsCount, recurringRules) {
        viewModel.getCashFlowProjections(30)
    }

    val totalUpcomingBills = remember(projections) {
        projections.sumOf { it.plannedBills }
    }

    val lowestBalance = remember(projections) {
        projections.minOfOrNull { it.projectedBalance } ?: 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        Surface(
            shape = SleekShapes.xl,
            color = SleekSurface,
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("30-Day Upcoming Bills", fontSize = 11.sp, color = SleekTextSecondary)
                        Text(
                            text = "%s%,.0f".format(currencySymbol, totalUpcomingBills),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Projected Lowest Point", fontSize = 11.sp, color = SleekTextSecondary)
                        Text(
                            text = "%s%,.0f".format(currencySymbol, lowestBalance),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lowestBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                if (lowestBalance < 0) {
                    Surface(
                        shape = SleekShapes.md,
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "⚠️", fontSize = 16.sp)
                            Text(
                                text = "Overdraft Warning: Projected balance dips into negative territory within 30 days.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Daily Cash Flow Timeline",
            fontSize = SleekSizes.textBody,
            fontWeight = FontWeight.Bold,
            color = SleekTextPrimary
        )

        // Day by Day List
        projections.forEach { day ->
            val hasBills = day.plannedBills > 0
            val isNegative = day.projectedBalance < 0

            Surface(
                shape = SleekShapes.lg,
                color = if (hasBills) SleekPrimaryContainer.copy(alpha = 0.12f) else SleekSurface,
                border = BorderStroke(
                    1.dp,
                    if (hasBills) SleekPrimary.copy(alpha = 0.3f) else SleekBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = SleekShapes.md,
                            color = if (hasBills) SleekPrimary else SleekBorder.copy(alpha = 0.4f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.dayOfWeek,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasBills) Color.White else SleekTextSecondary
                                )
                                Text(
                                    text = day.dayLabel.take(2),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (hasBills) Color.White else SleekTextPrimary
                                )
                            }
                        }

                        Column {
                            Text(
                                text = day.dayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                            if (hasBills) {
                                Text(
                                    text = "⚡ Due: ${day.billNames.joinToString(", ")} (-%s%,.0f)".format(currencySymbol, day.plannedBills),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEF4444)
                                )
                            } else {
                                Text(
                                    text = "Estimated standard burn",
                                    fontSize = 10.sp,
                                    color = SleekTextSecondary
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%s%,.0f".format(currencySymbol, day.projectedBalance),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNegative) Color(0xFFEF4444) else SleekTextPrimary
                        )
                        Text(
                            text = if (isNegative) "Deficit" else "Est. Balance",
                            fontSize = 10.sp,
                            color = SleekTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

