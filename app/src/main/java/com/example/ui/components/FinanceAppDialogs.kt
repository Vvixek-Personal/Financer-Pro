package com.example.ui

import com.example.ui.theme.*
import android.util.Log
import com.example.data.*
import com.example.R
import java.util.*
import java.text.*
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.ui.components.*
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import android.widget.Toast

private const val TAG = "FinanceAppDialogs"

// ==========================================
// 6️⃣ EDIT EXPENSE DIALOG
// ==========================================
@Composable
fun EditExpenseDialog(
    expense: Expense,
    categories: List<String>,
    expenseCategories: List<String> = emptyList(),
    incomeCategories: List<String> = emptyList(),
    categoryIcons: Map<String, String> = emptyMap(),
    currencySymbol: String = "₹",
    currencyCode: String = "INR",
    expenses: List<Expense> = emptyList(),
    onAddCategory: (name: String, categoryType: String) -> Unit = { _, _ -> },
    onDeleteCategory: (String) -> Unit,
    onEditCategory: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var type by rememberSaveable { mutableStateOf(expense.type) }
    var amountStr by rememberSaveable { mutableStateOf(expense.amount.toString()) }

    val otherExpenses = remember(expenses, expense, currencyCode) {
        expenses.filter { it.id != expense.id }.realExpense(currencyCode)
    }
    val totalIncome = remember(expenses, expense, amountStr, type, currencyCode) {
        if (expense.type == "INCOME") {
            expenses.filter { it.id != expense.id }.realIncome(currencyCode) + (if (type == "INCOME") (amountStr.toDoubleOrNull() ?: 0.0) else 0.0)
        } else {
            expenses.realIncome(currencyCode)
        }
    }

    val availableBalanceForEdit = (totalIncome - otherExpenses).coerceAtLeast(0.0)
    val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    val isExceedingIncome = type == "EXPENSE" && (enteredAmount > availableBalanceForEdit || otherExpenses + enteredAmount > totalIncome)

    val defaultExpensePreset = listOf("Food", "Travel", "Rent", "Utilities", "Entertainment", "Shopping", "Home", "Others")
    val defaultIncomePreset = listOf("Salary", "Freelance", "Investments", "Gifts", "Others")
    val currentCategoriesList = if (type == "INCOME") {
        (defaultIncomePreset + (if (incomeCategories.isNotEmpty()) incomeCategories else categories.filter { defaultIncomePreset.contains(it) })).distinct()
    } else {
        (defaultExpensePreset + (if (expenseCategories.isNotEmpty()) expenseCategories else categories.filter { !defaultIncomePreset.contains(it) })).distinct()
    }
    
    var category by rememberSaveable { mutableStateOf(expense.category) }
    var note by rememberSaveable { mutableStateOf(expense.note ?: "") }

    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    var firstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(type) {
        if (firstLoad) {
            firstLoad = false
        } else {
            category = if (type == "INCOME") (currentCategoriesList.firstOrNull() ?: "Salary") else (currentCategoriesList.firstOrNull() ?: "Food")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = SleekShapes.dialog,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_expense_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(SleekSpacing.cardPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (type == "INCOME") "Edit income" else "Edit expense",
                    style = MaterialTheme.typography.titleMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Segmented Switcher (Expense / Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SleekShapes.chip)
                        .background(SleekBg)
                        .border(BorderStroke(1.dp, SleekBorder), SleekShapes.chip)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SleekShapes.chip)
                            .background(if (type == "EXPENSE") ExpenseRedBg else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                type = "EXPENSE"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            color = if (type == "EXPENSE") ExpenseRed else SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SleekShapes.chip)
                            .background(if (type == "INCOME") IncomeGreenBg else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                type = "INCOME"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            color = if (type == "INCOME") IncomeGreen else SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($currencySymbol)", color = SleekTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    shape = SleekShapes.input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == "INCOME") IncomeGreen else SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedContainerColor = SleekBg,
                        unfocusedContainerColor = SleekBg,
                        focusedLabelColor = if (type == "INCOME") IncomeGreen else SleekPrimary,
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == "EXPENSE") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Available: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", availableBalanceForEdit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExceedingIncome) ExpenseRed else SleekTextSecondary,
                            fontSize = 12.sp
                        )
                        if (totalIncome > 0) {
                            Text(
                                text = "Total income: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", totalIncome)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (isExceedingIncome) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ExpenseRedBg),
                            shape = SleekShapes.chip,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AppIcons.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (totalIncome <= 0) 
                                        "Expense cannot exceed total income." 
                                    else 
                                        "Expense exceeds available balance.",
                                    color = ExpenseRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Total income: $currencySymbol${String.format(Locale.getDefault(), "%,.2f", totalIncome)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                CategorySelectorGrid(
                    selectedCategory = category,
                    onCategorySelected = { category = it },
                    categories = currentCategoriesList,
                    categoryIcons = categoryIcons,
                    onAddCustomCategoryClick = { showCreateCategoryDialog = true },
                    onDeleteCustomCategory = if (type == "EXPENSE") onDeleteCategory else null,
                    onEditCustomCategory = if (type == "EXPENSE") onEditCategory else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Note description input (Mandatory)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description *", color = SleekTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    shape = SleekShapes.input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == "INCOME") IncomeGreen else SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedContainerColor = SleekBg,
                        unfocusedContainerColor = SleekBg,
                        focusedLabelColor = if (type == "INCOME") IncomeGreen else SleekPrimary,
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (note.trim().isEmpty()) {
                    Text(
                        "Description is required",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Confirm and Cancel buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, SleekBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextSecondary),
                        shape = SleekShapes.chip,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && note.trim().isNotEmpty()) {
                                onConfirm(
                                    expense.copy(
                                        amountMinor = Money.fromDouble(amount, expense.currencyCode),
                                        category = category,
                                        note = note.trim(),
                                        type = type
                                    )
                                )
                            }
                        },
                        enabled = note.trim().isNotEmpty() && enteredAmount > 0.0 && !isExceedingIncome,
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "INCOME") IncomeGreen else SleekPrimary),
                        shape = SleekShapes.chip,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Save", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            initialType = type,
            onDismiss = { showCreateCategoryDialog = false },
            onConfirm = { newCat, catType ->
                onAddCategory(newCat, catType)
                category = newCat
                showCreateCategoryDialog = false
            }
        )
    }
}


@Composable
fun OnboardingNameDialog(
    onSave: (String) -> Unit
) {
    var nameStr by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = { /* Prevent dismiss to force name entry */ }
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekBg),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(16.dp, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(SleekPrimaryContainer)
                        .border(2.dp, SleekBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_profile_avatar),
                        contentDescription = "Welcome User",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Welcome to Finance",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please enter your name to personalize your offline ledgers and insights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nameStr,
                    onValueChange = { nameStr = it },
                    label = { Text("Your Name", color = SleekTextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedLabelColor = SleekPrimary,
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (nameStr.trim().isNotEmpty()) {
                            onSave(nameStr.trim())
                        }
                    },
                    enabled = nameStr.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_save_button")
                ) {
                    Text(
                        "Get Started",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CreateCategoryDialog(
    initialType: String = "EXPENSE",
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String) -> Unit = { _, _ -> },
    onConfirmSingle: ((String) -> Unit)? = null
) {
    var newCatName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(if (initialType == "INCOME") "INCOME" else "EXPENSE") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().testTag("create_category_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Add New Category",
                    style = MaterialTheme.typography.titleMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Category Type Selector (Expense vs Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isExp = selectedType == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExp) Color(0xFFEF5350) else Color.Transparent)
                            .clickable { selectedType = "EXPENSE" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Expense Category",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExp) Color.White else SleekTextSecondary
                        )
                    }

                    val isInc = selectedType == "INCOME"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isInc) Color(0xFF10B981) else Color.Transparent)
                            .clickable { selectedType = "INCOME" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Income Category",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInc) Color.White else SleekTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val previewIcon = getCategoryIcon(newCatName.ifBlank { if (selectedType == "INCOME") "Salary" else "Food" })
                val previewColor = getCategoryColor(newCatName.ifBlank { if (selectedType == "INCOME") "Salary" else "Food" })

                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(previewColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = previewIcon,
                                contentDescription = null,
                                tint = previewColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    label = { Text("Category Name", color = SleekTextSecondary) },
                    placeholder = { Text(if (selectedType == "INCOME") "e.g., Freelance, Bonus..." else "e.g., Gym, Subscriptions...", color = SleekTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedType == "INCOME") Color(0xFF10B981) else Color(0xFFEF5350),
                        unfocusedBorderColor = SleekBorder,
                        focusedLabelColor = if (selectedType == "INCOME") Color(0xFF10B981) else Color(0xFFEF5350),
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
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
                            if (newCatName.trim().isNotEmpty()) {
                                onConfirm(newCatName.trim(), selectedType)
                                onConfirmSingle?.invoke(newCatName.trim())
                            }
                        },
                        enabled = newCatName.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "INCOME") Color(0xFF10B981) else Color(0xFFEF5350)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    CreateCategoryDialog(
        initialType = "EXPENSE",
        onDismiss = onDismiss,
        onConfirmSingle = onConfirm
    )
}


@Composable
fun AdjustBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var budgetStr by remember { mutableStateOf(currentBudget.toInt().toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().testTag("adjust_budget_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "Adjust Monthly Budget",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Budget Cap (₹)", color = SleekTextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedLabelColor = SleekPrimary,
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("budget_input")
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, SleekBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val budgetVal = budgetStr.toDoubleOrNull() ?: 0.0
                            if (budgetVal > 0) {
                                onConfirm(budgetVal)
                            }
                        },
                        enabled = (budgetStr.toDoubleOrNull() ?: 0.0) > 0.0,
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DateRangeReportModalDialog(
    initialStartDate: Long,
    initialEndDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (startDate: Long, endDate: Long) -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    val context = LocalContext.current
    val dateSdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.5.dp, SleekPrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("date_range_picker_modal")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                AppIcons.DateRange,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Select Report Period",
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Pick Start & End Date for report",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Start Date Picker Field
                Text(
                    "Start Date",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = if (startDate == 0L) System.currentTimeMillis() else startDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selCal = Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    startDate = selCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.CalendarMonth, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (startDate == 0L) "All Time / Beginning" else dateSdf.format(Date(startDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                        }
                        Text("Pick Date", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // End Date Picker Field
                Text(
                    "End Date",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = endDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selCal = Calendar.getInstance().apply {
                                        set(year, month, day, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }
                                    endDate = selCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.CalendarMonth, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = dateSdf.format(Date(endDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                        }
                        Text("Pick Date", fontSize = 12.sp, color = SleekPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text("Quick Selection", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf("7 Days", "30 Days", "This Month", "All Time")
                    presets.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val now = Calendar.getInstance()
                                when (preset) {
                                    "7 Days" -> {
                                        endDate = now.timeInMillis
                                        startDate = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_MONTH, -7)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                        }.timeInMillis
                                    }
                                    "30 Days" -> {
                                        endDate = now.timeInMillis
                                        startDate = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_MONTH, -30)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                        }.timeInMillis
                                    }
                                    "This Month" -> {
                                        endDate = now.timeInMillis
                                        startDate = Calendar.getInstance().apply {
                                            set(Calendar.DAY_OF_MONTH, 1)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                        }.timeInMillis
                                    }
                                    "All Time" -> {
                                        endDate = now.timeInMillis
                                        startDate = 0L
                                    }
                                }
                            },
                            label = { Text(preset, fontSize = 9.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SleekBorder)
                    ) {
                        Text("Cancel", color = SleekTextPrimary)
                    }

                    Button(
                        onClick = { onConfirm(startDate, endDate) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Text("Apply Range", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameStr by remember { mutableStateOf(currentName) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Edit Your Name",
                    style = MaterialTheme.typography.titleMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = nameStr,
                    onValueChange = { nameStr = it },
                    label = { Text("Name", color = SleekTextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedLabelColor = SleekPrimary,
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
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
                            if (nameStr.trim().isNotEmpty()) {
                                onConfirm(nameStr.trim())
                            }
                        },
                        enabled = nameStr.trim().isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// TRANSACTION RECORDED SUCCESS ANIMATED DIALOG
// ==========================================
data class RecordedTransactionInfo(
    val amount: Double,
    val category: String,
    val type: String, // "EXPENSE" or "INCOME"
    val note: String? = null
)

@Composable
fun TransactionSuccessDialog(
    info: RecordedTransactionInfo,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var animPhase by remember { mutableIntStateOf(0) } // 0: rotating red swoosh in navy circle, 1: white checkmark draw, 2: card reveal
    val scaleAnim = remember { Animatable(0.35f) }
    val checkmarkProgress = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val cardTranslationY = remember { Animatable(35f) }

    val swooshRotation = rememberLoopFloat(0f, 360f, 650, "swoosh_rotation", LinearEasing, reverse = false, rest = 0f)

    LaunchedEffect(Unit) {
        // Step 1: Scale up dark blue circle with crisp spring physics
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        // Hold rotating red swoosh briefly
        kotlinx.coroutines.delay(550)
        animPhase = 1

        // Step 2: Animate checkmark draw with Apple Decelerate Easing
        checkmarkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(380, easing = AppleDecelerateEasing)
        )
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to perform celebration haptic feedback", e)
        }
        animPhase = 2

        // Step 3: Reveal text card with coordinated subtle glide
        launch {
            cardAlpha.animateTo(1f, tween(260, easing = AppleDecelerateEasing))
        }
        launch {
            cardTranslationY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(enabled = false) {}
            ) {
                // Navy Blue Badge Container from Video (Color(0xFF0B2E4E))
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF0B2E4E))
                        .border(4.dp, Color(0xFF1E40AF).copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerPx = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        if (animPhase == 0) {
                            // Red Swoosh Shape from Frame 0 & Frame 1 of video
                            rotate(swooshRotation, pivot = centerPx) {
                                val redPath = Path().apply {
                                    moveTo(centerPx.x - radius * 0.4f, centerPx.y + radius * 0.1f)
                                    cubicTo(
                                        centerPx.x - radius * 0.2f, centerPx.y - radius * 0.45f,
                                        centerPx.x + radius * 0.4f, centerPx.y - radius * 0.35f,
                                        centerPx.x + radius * 0.35f, centerPx.y + radius * 0.2f
                                    )
                                    cubicTo(
                                        centerPx.x + radius * 0.15f, centerPx.y + radius * 0.5f,
                                        centerPx.x - radius * 0.35f, centerPx.y + radius * 0.4f,
                                        centerPx.x - radius * 0.4f, centerPx.y + radius * 0.1f
                                    )
                                    close()
                                }
                                drawPath(
                                    path = redPath,
                                    color = Color(0xFFE53935)
                                )
                            }
                        } else {
                            // White Checkmark (✓) from Frame 2 & Frame 3 of video
                            val p = checkmarkProgress.value
                            val checkPath = Path().apply {
                                val start = Offset(size.width * 0.30f, size.height * 0.52f)
                                val mid = Offset(size.width * 0.44f, size.height * 0.66f)
                                val end = Offset(size.width * 0.72f, size.height * 0.38f)

                                moveTo(start.x, start.y)
                                if (p <= 0.5f) {
                                    val localP = p / 0.5f
                                    lineTo(
                                        start.x + (mid.x - start.x) * localP,
                                        start.y + (mid.y - start.y) * localP
                                    )
                                } else {
                                    lineTo(mid.x, mid.y)
                                    val localP = (p - 0.5f) / 0.5f
                                    lineTo(
                                        mid.x + (end.x - mid.x) * localP,
                                        mid.y + (end.y - mid.y) * localP
                                    )
                                }
                            }

                            drawPath(
                                path = checkPath,
                                color = Color.White,
                                style = Stroke(
                                    width = size.width * 0.12f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Detail Card below badge
                if (cardAlpha.value > 0.01f) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SleekSurface),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, SleekBorder),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .graphicsLayer {
                                alpha = cardAlpha.value
                                translationY = cardTranslationY.value
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            val isIncome = info.type.equals("INCOME", ignoreCase = true)
                            val isTransfer = info.type.equals("TRANSFER", ignoreCase = true)
                            val accentColor = when {
                                isTransfer -> Color(0xFF0284C7)
                                isIncome -> Color(0xFF10B981)
                                else -> Color(0xFFEF4444)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when {
                                        isTransfer -> "TRANSFER COMPLETED"
                                        isIncome -> "INCOME RECORDED"
                                        else -> "EXPENSE RECORDED"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%.2f", info.amount)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = SleekTextPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = info.category + if (!info.note.isNullOrBlank()) " • ${info.note}" else "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SleekTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2E4E)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Done",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
