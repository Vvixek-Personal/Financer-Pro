package com.example.ui

import com.example.ui.theme.*
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🎨 Reference Wireframe Matched Add Transaction Dialog
 * Step 1: Main Add Screen with circular ring dial, Expense/Income toggle, category bar & Confirm button
 * Step 2: Select Category Sheet modal
 * Step 3: Add Description screen with category badge, receipt pickers & Save button
 */
@Composable
fun AddExpenseDialog(
    prefilledDate: Long?,
    categories: List<String>,
    expenseCategories: List<String> = emptyList(),
    incomeCategories: List<String> = emptyList(),
    categoryIcons: Map<String, String> = emptyMap(),
    expenses: List<Expense> = emptyList(),
    savingsGoals: List<com.example.data.SavingsGoal> = emptyList(),
    defaultTxType: String = "EXPENSE",
    rememberLastCategory: Boolean = true,
    lastUsedExpenseCategory: String? = null,
    lastUsedIncomeCategory: String? = null,
    gstReserveAmount: Double = 0.0,
    monthlySafeAmount: Double = 0.0,
    currencySymbol: String = "₹",
    currencyCode: String = "INR",
    accounts: List<com.example.data.Account> = emptyList(),
    onAddCategory: (name: String, categoryType: String) -> Unit = { name, _ -> },
    onDeleteCategory: (String) -> Unit = {},
    onEditCategory: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onConfirmTransfer: ((fromAccountId: Long, toAccountId: Long, amount: Double, note: String, date: Long) -> Unit)? = null,
    onConfirm: (amount: Double, category: String, date: Long, note: String, imagePath: String?, type: String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var step by rememberSaveable { mutableIntStateOf(1) } // 1: Main Add Screen, 2: Add Description Screen
    var showCategorySheet by remember { mutableStateOf(false) }

    var type by rememberSaveable { mutableStateOf(defaultTxType) } // "EXPENSE", "INCOME", "TRANSFER"
    var amountStr by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable {
        val initialCategory = when (type) {
            "INCOME" -> (if (rememberLastCategory) lastUsedIncomeCategory else null)
                ?: incomeCategories.firstOrNull() ?: "Salary"
            "TRANSFER" -> "Transfer"
            else -> (if (rememberLastCategory) lastUsedExpenseCategory else null)
                ?: expenseCategories.firstOrNull() ?: categories.firstOrNull() ?: "Food"
        }
        mutableStateOf(initialCategory)
    }
    var fromAccountId by rememberSaveable(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var toAccountId by rememberSaveable(accounts) {
        val second = accounts.getOrNull(1)?.id
        mutableStateOf(if (second != null && second != fromAccountId) second else fromAccountId)
    }
    var note by rememberSaveable { mutableStateOf("") }

    var isAmountEditing by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCatTypeForDialog by remember { mutableStateOf(type) }

    // Image/receipt selection states
    val context = LocalContext.current
    var attachedImagePath by remember { mutableStateOf<String?>(null) }
    var editingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropperDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadFullResolutionBitmap(context, uri)
            if (bitmap != null) {
                editingBitmap = bitmap
                showCropperDialog = true
            } else {
                Toast.makeText(context, "Error loading full-resolution image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            editingBitmap = bitmap
            showCropperDialog = true
        }
    }

    val totalIncome = remember(expenses, currencyCode) {
        expenses.realIncome(currencyCode)
    }
    val totalExpenses = remember(expenses, currencyCode) {
        expenses.realExpense(currencyCode)
    }
    val totalLockedInGoals = remember(savingsGoals) {
        savingsGoals.sumOf { it.currentAmount }
    }
    val recordedLockedInExpenses = remember(expenses) {
        expenses.filter { it.category == "Locked Savings" && it.type == "EXPENSE" }.sumOf { it.amount } - expenses.filter { it.category == "Goal Withdrawal" && it.type == "INCOME" }.sumOf { it.amount }
    }
    val unrecordedLockedSavings = (totalLockedInGoals - recordedLockedInExpenses).coerceAtLeast(0.0)

    val availableBalance = (totalIncome - totalExpenses - unrecordedLockedSavings - gstReserveAmount - monthlySafeAmount).coerceAtLeast(0.0)

    val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    val isExceedingIncome = type == "EXPENSE" && (enteredAmount > availableBalance || totalExpenses + enteredAmount > totalIncome)

    // Automatically set default category when switching type
    LaunchedEffect(type) {
        if (type == "INCOME") {
            val validIncome = if (incomeCategories.isNotEmpty()) incomeCategories else listOf("Salary", "Freelance", "Investments", "Gifts", "Others")
            if (!validIncome.contains(category)) {
                category = validIncome.firstOrNull() ?: "Salary"
            }
        } else {
            val validExpense = if (expenseCategories.isNotEmpty()) expenseCategories else categories
            if (!validExpense.contains(category)) {
                category = validExpense.firstOrNull() ?: "Food"
            }
        }
    }

    val activeColor = when (type) {
        "INCOME" -> IncomeGreen
        "TRANSFER" -> Color(0xFF0284C7)
        else -> ExpenseRed
    }
    val activeSoftBg = when (type) {
        "INCOME" -> IncomeGreenBg
        "TRANSFER" -> Color(0xFF0284C7).copy(alpha = 0.15f)
        else -> ExpenseRedBg
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = SleekShapes.dialog,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("add_expense_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SleekSpacing.cardPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step == 1) {
                    // Top Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "New transaction",
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Record incoming, outgoing or account transfer",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = "Close",
                                tint = SleekTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Type Switcher: Expense / Income / Transfer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SleekShapes.chip)
                            .background(SleekBg)
                            .border(BorderStroke(1.dp, SleekBorder), SleekShapes.chip)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isExpenseActive = type == "EXPENSE"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SleekShapes.chip)
                                .background(if (isExpenseActive) ExpenseRedBg else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    type = "EXPENSE"
                                    category = (if (rememberLastCategory) lastUsedExpenseCategory else null)
                                        ?: expenseCategories.firstOrNull() ?: categories.firstOrNull() ?: "Food"
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isExpenseActive) ExpenseRed else SleekTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Expense",
                                    fontSize = 13.sp,
                                    fontWeight = if (isExpenseActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isExpenseActive) ExpenseRed else SleekTextSecondary
                                )
                            }
                        }

                        val isIncomeActive = type == "INCOME"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SleekShapes.chip)
                                .background(if (isIncomeActive) IncomeGreenBg else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    type = "INCOME"
                                    category = (if (rememberLastCategory) lastUsedIncomeCategory else null)
                                        ?: incomeCategories.firstOrNull() ?: "Salary"
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncomeActive) IncomeGreen else SleekTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Income",
                                    fontSize = 13.sp,
                                    fontWeight = if (isIncomeActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isIncomeActive) IncomeGreen else SleekTextSecondary
                                )
                            }
                        }

                        val isTransferActive = type == "TRANSFER"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SleekShapes.chip)
                                .background(if (isTransferActive) Color(0xFF0284C7).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    type = "TRANSFER"
                                    category = "Transfer"
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Sync,
                                    contentDescription = null,
                                    tint = if (isTransferActive) Color(0xFF0284C7) else SleekTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Transfer",
                                    fontSize = 13.sp,
                                    fontWeight = if (isTransferActive) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isTransferActive) Color(0xFF0284C7) else SleekTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Main Amount Centerpiece Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SleekShapes.card)
                            .background(SleekBg)
                            .border(BorderStroke(1.dp, SleekBorder), SleekShapes.card)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isAmountEditing = true
                            }
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isAmountEditing) {
                                val focusManager = LocalFocusManager.current
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    placeholder = { Text("0", fontSize = 28.sp, color = SleekTextSecondary) },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = activeColor,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            isAmountEditing = false
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    modifier = Modifier.width(160.dp).testTag("expense_amount_input")
                                )
                            } else {
                                val formattedDisplay = if (enteredAmount > 0) "$currencySymbol%,.0f".format(enteredAmount) else "${currencySymbol}0"
                                Text(
                                    text = formattedDisplay,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = activeColor,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (enteredAmount > 0) "Tap to edit amount" else "Tap to enter amount",
                                    fontSize = 12.sp,
                                    color = SleekTextSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Numpad / Preset Amount Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(100.0, 500.0, 1000.0, 2000.0).forEach { amt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(activeSoftBg)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val current = amountStr.toDoubleOrNull() ?: 0.0
                                        amountStr = "%.0f".format(current + amt)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "+$currencySymbol%,.0f".format(amt),
                                    fontSize = 11.sp,
                                    color = activeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (type == "EXPENSE" && isExceedingIncome) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (totalLockedInGoals > 0) {
                                "Warning: Amount exceeds available liquid balance ($currencySymbol%,.2f). Note: $currencySymbol%,.0f is locked in Savings Goals 🔒".format(availableBalance, totalLockedInGoals)
                            } else {
                                "Warning: Amount exceeds available balance ($currencySymbol%,.2f)".format(availableBalance)
                            },
                            color = Color(0xFFEF5350),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Category Selector or Account Transfer Selector Card
                    if (type == "TRANSFER") {
                        TransferAccountSelectorCard(
                            accounts = accounts,
                            fromAccountId = fromAccountId,
                            toAccountId = toAccountId,
                            currencySymbol = currencySymbol,
                            onFromAccountSelected = { fromAccountId = it },
                            onToAccountSelected = { toAccountId = it },
                            onSwapAccounts = {
                                val temp = fromAccountId
                                fromAccountId = toAccountId
                                toAccountId = temp
                            }
                        )
                    } else {
                        val categoryEmoji = getCategoryEmoji(category, categoryIcons)
                        Card(
                            shape = SleekShapes.card,
                            colors = CardDefaults.cardColors(containerColor = SleekBg),
                            border = BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showCategorySheet = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val catIcon = getCategoryIcon(category, categoryIcons)
                                    val catColor = getCategoryColor(category)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(SleekShapes.chip)
                                            .background(catColor.copy(alpha = 0.16f))
                                            .border(BorderStroke(1.dp, catColor.copy(alpha = 0.28f)), SleekShapes.chip),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = category,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Category",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SleekTextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = category.ifBlank { "Select a category" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SleekTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = AppIcons.ChevronRight,
                                    contentDescription = "Select category",
                                    tint = SleekTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. Confirm Button
                    val isConfirmEnabled = if (type == "TRANSFER") {
                        enteredAmount > 0 && fromAccountId != toAccountId
                    } else {
                        enteredAmount > 0 && !isExceedingIncome
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (enteredAmount > 0) {
                                step = 2 // Proceed to Add Description step
                            } else {
                                isAmountEditing = true
                            }
                        },
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeColor,
                            disabledContainerColor = activeColor.copy(alpha = 0.3f)
                        ),
                        shape = SleekShapes.chip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("confirm_amount_btn")
                    ) {
                        Text(
                            text = if (enteredAmount > 0) "Continue" else "Enter amount",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                } else {
                    // STEP 2: ADD DESCRIPTION SCREEN
                    // Top Bar with Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { step = 1 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.ArrowBack,
                                contentDescription = "Back",
                                tint = SleekTextPrimary
                            )
                        }

                        Text(
                            text = "Add note",
                            style = MaterialTheme.typography.titleMedium,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.width(36.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val fromAccName = accounts.find { it.id == fromAccountId }?.name ?: "Source"
                    val toAccName = accounts.find { it.id == toAccountId }?.name ?: "Destination"

                    // Category / Transfer Icon Badge & Amount Display
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(SleekShapes.chip)
                            .background(activeSoftBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (type == "TRANSFER") AppIcons.Sync else AppIcons.Category,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$currencySymbol%,.0f".format(enteredAmount),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor
                    )

                    Text(
                        text = if (type == "TRANSFER") "Transfer: $fromAccName ➔ $toAccName" else "${if (type == "INCOME") "Income" else "Expense"} • $category",
                        fontSize = 12.sp,
                        color = SleekTextSecondary,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description Input Box
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Note (optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = { if (it.length <= 100) note = it },
                            placeholder = { Text("Add note or description...", color = SleekTextSecondary, fontSize = 14.sp) },
                            minLines = 3,
                            maxLines = 4,
                            shape = SleekShapes.input,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeColor,
                                unfocusedBorderColor = SleekBorder,
                                focusedContainerColor = SleekBg,
                                unfocusedContainerColor = SleekBg
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_note_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${note.length}/100",
                                fontSize = 12.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Add Receipt (Optional)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Receipt (optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val receiptPath = attachedImagePath
                        if (!receiptPath.isNullOrBlank() && File(receiptPath).exists()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(SleekShapes.card)
                                    .border(1.dp, SleekBorder, SleekShapes.card)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = File(receiptPath),
                                    contentDescription = "Receipt",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { attachedImagePath = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(AppIcons.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val suppressAutoLock = LocalAutoLockSuppressor.current
                                // Camera Button
                                Surface(
                                    onClick = {
                                        suppressAutoLock()
                                        cameraLauncher.launch(null)
                                    },
                                    shape = SleekShapes.chip,
                                    color = SleekBg,
                                    border = BorderStroke(1.dp, SleekBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.CameraAlt,
                                            contentDescription = "Camera",
                                            tint = SleekPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Camera",
                                            fontSize = 12.sp,
                                            color = SleekTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Gallery Button
                                Surface(
                                    onClick = {
                                        suppressAutoLock()
                                        galleryLauncher.launch("image/*")
                                    },
                                    shape = SleekShapes.chip,
                                    color = SleekBg,
                                    border = BorderStroke(1.dp, SleekBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Image,
                                            contentDescription = "Gallery",
                                            tint = SleekPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gallery",
                                            fontSize = 12.sp,
                                            color = SleekTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Final Save Button
                    val finalNote = if (note.trim().isBlank()) {
                        if (type == "TRANSFER") "Transfer from $fromAccName to $toAccName" else category
                    } else note.trim()

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (type == "TRANSFER") {
                                onConfirmTransfer?.invoke(
                                    fromAccountId,
                                    toAccountId,
                                    enteredAmount,
                                    finalNote,
                                    prefilledDate ?: System.currentTimeMillis()
                                )
                            } else {
                                onConfirm(
                                    enteredAmount,
                                    category,
                                    prefilledDate ?: System.currentTimeMillis(),
                                    finalNote,
                                    attachedImagePath,
                                    type
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        shape = SleekShapes.chip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("save_expense_btn")
                    ) {
                        Text(
                            text = when (type) {
                                "INCOME" -> "Save income"
                                "TRANSFER" -> "Transfer Funds"
                                else -> "Save expense"
                            },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for Select Category (Frame 4)
    if (showCategorySheet) {
        SelectCategorySheet(
            selectedCategory = category,
            type = type,
            categories = categories,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            categoryIcons = categoryIcons,
            onSelectCategoryWithType = { selectedCat, catType ->
                category = selectedCat
                type = catType
                showCategorySheet = false
            },
            onAddCustomCategoryClickWithType = { catType ->
                newCatTypeForDialog = catType
                showCreateCategoryDialog = true
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    val editBmp = editingBitmap
    if (showCropperDialog && editBmp != null) {
        ImageEditDialog(
            initialBitmap = editBmp,
            onDismiss = { showCropperDialog = false },
            onSave = { savedPath ->
                showCropperDialog = false
                attachedImagePath = savedPath
            }
        )
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            initialType = newCatTypeForDialog,
            onDismiss = { showCreateCategoryDialog = false },
            onConfirm = { newCat, catType ->
                onAddCategory(newCat, catType)
                category = newCat
                type = catType
                showCreateCategoryDialog = false
            }
        )
    }
}

/**
 * 📱 Select Category Modal Dialog (Wireframe 4)
 */
@Composable
fun SelectCategorySheet(
    selectedCategory: String,
    type: String,
    categories: List<String> = emptyList(),
    expenseCategories: List<String> = emptyList(),
    incomeCategories: List<String> = emptyList(),
    categoryIcons: Map<String, String> = emptyMap(),
    onSelectCategory: (String) -> Unit = {},
    onSelectCategoryWithType: ((category: String, selectedType: String) -> Unit)? = null,
    onAddCustomCategoryClick: () -> Unit = {},
    onAddCustomCategoryClickWithType: ((categoryType: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val defaultExpensePreset = listOf("Food", "Travel", "Rent", "Utilities", "Entertainment", "Shopping", "Home", "Others")
    val defaultIncomePreset = listOf("Salary", "Freelance", "Investments", "Gifts", "Others")

    val finalExpenseList = (defaultExpensePreset + (if (expenseCategories.isNotEmpty()) expenseCategories else categories.filter { !defaultIncomePreset.contains(it) })).distinct()
    val finalIncomeList = (defaultIncomePreset + (if (incomeCategories.isNotEmpty()) incomeCategories else categories.filter { defaultIncomePreset.contains(it) })).distinct()

    val haptic = LocalHapticFeedback.current
    var activeTab by remember { mutableStateOf(if (type == "INCOME") "INCOME" else "EXPENSE") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Tab Switcher (Expense / Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SleekBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isExpTab = activeTab == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpTab) Color(0xFFEF5350) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeTab = "EXPENSE"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense Categories",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpTab) Color.White else SleekTextSecondary
                        )
                    }

                    val isIncTab = activeTab == "INCOME"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIncTab) Color(0xFF10B981) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeTab = "INCOME"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income Categories",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncTab) Color.White else SleekTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (activeTab == "EXPENSE") {
                    CategoryGridSection(
                        items = finalExpenseList,
                        selectedCategory = selectedCategory,
                        categoryIcons = categoryIcons,
                        activeBg = Color(0xFFFEE2E2),
                        activeColor = Color(0xFFEF5350),
                        onSelect = { cat ->
                            if (onSelectCategoryWithType != null) {
                                onSelectCategoryWithType(cat, "EXPENSE")
                            } else {
                                onSelectCategory(cat)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            if (onAddCustomCategoryClickWithType != null) {
                                onAddCustomCategoryClickWithType("EXPENSE")
                            } else {
                                onAddCustomCategoryClick()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = AppIcons.Add,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Custom Expense Category",
                            color = Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    CategoryGridSection(
                        items = finalIncomeList,
                        selectedCategory = selectedCategory,
                        categoryIcons = categoryIcons,
                        activeBg = Color(0xFFDCFCE7),
                        activeColor = Color(0xFF10B981),
                        onSelect = { cat ->
                            if (onSelectCategoryWithType != null) {
                                onSelectCategoryWithType(cat, "INCOME")
                            } else {
                                onSelectCategory(cat)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            if (onAddCustomCategoryClickWithType != null) {
                                onAddCustomCategoryClickWithType("INCOME")
                            } else {
                                onAddCustomCategoryClick()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = AppIcons.Add,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Custom Income Category",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridSection(
    items: List<String>,
    selectedCategory: String,
    categoryIcons: Map<String, String>,
    activeBg: Color,
    activeColor: Color,
    onSelect: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val columns = 4
    val rows = items.chunked(columns)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    val emoji = getCategoryEmoji(cat, categoryIcons)

                    val catIcon = getCategoryIcon(cat, categoryIcons)
                    val catColor = getCategoryColor(cat)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(cat)
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) catColor.copy(alpha = 0.22f) else SleekBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) catColor else SleekBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = cat,
                                tint = if (isSelected) catColor else SleekTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeColor else SleekTextPrimary,
                            maxLines = 1
                        )
                    }
                }
                // Fill empty slots if last row has less than 4 items
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TransferAccountSelectorCard(
    accounts: List<com.example.data.Account>,
    fromAccountId: Long,
    toAccountId: Long,
    currencySymbol: String,
    onFromAccountSelected: (Long) -> Unit,
    onToAccountSelected: (Long) -> Unit,
    onSwapAccounts: () -> Unit
) {
    Card(
        shape = SleekShapes.card,
        colors = CardDefaults.cardColors(containerColor = SleekBg),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FROM ACCOUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextSecondary
                )
                if (accounts.size >= 2) {
                    IconButton(
                        onClick = onSwapAccounts,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Sync,
                            contentDescription = "Swap accounts",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // From Account list
            if (accounts.isEmpty()) {
                Text(
                    text = "No accounts available",
                    fontSize = 12.sp,
                    color = SleekTextSecondary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = acc.id == fromAccountId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF0284C7) else SleekSurface,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else SleekBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onFromAccountSelected(acc.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = acc.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SleekTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$currencySymbol%,.0f".format(acc.balance),
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else SleekTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TO ACCOUNT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SleekTextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // To Account list
            if (accounts.isEmpty()) {
                Text(
                    text = "No accounts available",
                    fontSize = 12.sp,
                    color = SleekTextSecondary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = acc.id == toAccountId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF10B981) else SleekSurface,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF10B981) else SleekBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToAccountSelected(acc.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = acc.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SleekTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$currencySymbol%,.0f".format(acc.balance),
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else SleekTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (fromAccountId == toAccountId) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Source and destination accounts must be different.",
                    fontSize = 11.sp,
                    color = Color(0xFFEF5350),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

