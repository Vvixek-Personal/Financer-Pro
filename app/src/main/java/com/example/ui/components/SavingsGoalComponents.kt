package com.example.ui

import com.example.ui.theme.*
import com.example.data.Money
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Account
import com.example.data.SavingsGoal
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 🎯 FULL SAVING GOALS SCREEN (MATCHING REFERENCE IMAGE)
// ==========================================
@Composable
fun SavingGoalsFullScreen(
    viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val goalCategoriesList by viewModel.goalCategories.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var selectedGoalForDeposit by remember { mutableStateOf<SavingsGoal?>(null) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    // Active Coin Deposit Animation state
    var activeDepositAnimationInfo by remember { mutableStateOf<Pair<Double, String>?>(null) }

    BackHandler { onBack() }

    // Calculate Total Savings
    val totalSaving = remember(savingsGoals) { savingsGoals.sumOf { g: SavingsGoal -> g.currentAmount } }

    // Category Tabs list
    val categories: List<String> = remember(savingsGoals, goalCategoriesList) {
        val base = listOf("All")
        val customFromGoals = savingsGoals.map { g: SavingsGoal -> g.category }.distinct()
        (base + goalCategoriesList + customFromGoals).distinct()
    }

    // Filtered goals grid
    val filteredGoals: List<SavingsGoal> = remember(savingsGoals, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            savingsGoals
        } else {
            savingsGoals.filter { g: SavingsGoal -> g.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Header Bar: Back | Title ("Saving Goals") | More Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBack()
                    }
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = SleekTextPrimary
                    )
                }

                Text(
                    text = "Saving Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                ) {
                    Icon(
                        imageVector = AppIcons.MoreVert,
                        contentDescription = "Options",
                        tint = SleekTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Rounded Rectangular Credit Card Theme Box (Tuned with Customizable App Theme)
            val totalTargetAll = remember(savingsGoals) { savingsGoals.sumOf { g: SavingsGoal -> g.targetAmount } }
            val overallProgressRatio = if (totalTargetAll > 0) (totalSaving / totalTargetAll).toFloat().coerceIn(0f, 1f) else 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SleekShapes.xxl)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SleekPrimary,
                                SleekPrimary.copy(alpha = 0.85f),
                                Color(0xFF0F172A)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = SleekShapes.xxl
                    )
                    .padding(22.dp)
            ) {
                // Background decorative circles/watermark
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = 120.dp.toPx(),
                        center = Offset(size.width + 10.dp.toPx(), -20.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f),
                        radius = 180.dp.toPx(),
                        center = Offset(size.width - 20.dp.toPx(), size.height + 20.dp.toPx())
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row: Icon + Label & Goals Counter Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = AppIcons.Savings,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(SleekSizes.iconSmall)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TOTAL SAVINGS VAULT",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Smart Badge with Active Goals count
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${savingsGoals.size} Goals",
                                    fontSize = SleekSizes.textCaption,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Big Amount Display in INR
                    Text(
                        text = "₹%,.2f".format(totalSaving),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Goal Target Progress bar inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target: ₹%,.0f".format(totalTargetAll),
                            fontSize = SleekSizes.textBodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${(overallProgressRatio * 100).toInt()}% Saved",
                            fontSize = SleekSizes.textBodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress track bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(SleekShapes.xs)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(overallProgressRatio)
                                .clip(SleekShapes.xs)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF34D399), Color(0xFF10B981))
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Section Title "My Saving" & "+ Add New" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Saving",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAddDialog = true
                    },
                    shape = SleekShapes.pill,
                    border = BorderStroke(1.dp, SleekPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(SleekSizes.iconMicro)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add New",
                        color = SleekPrimary,
                        fontSize = SleekSizes.textBodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Category Filter Pills with Add & Delete Category support
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories, key = { cat: String -> cat }) { cat: String ->
                    val isSelected = selectedCategoryFilter.equals(cat, ignoreCase = true)
                    val bgColor = if (isSelected) SleekPrimary else SleekSurface
                    val textColor = if (isSelected) Color.White else SleekTextSecondary
                    val borderColor = if (isSelected) SleekPrimary else SleekBorder
                    val isDeletable = cat !in listOf("All", "Saving", "Investment", "Expenditure")

                    Box(
                        modifier = Modifier
                            .clip(SleekShapes.lg)
                            .background(bgColor)
                            .border(1.dp, borderColor, SleekShapes.lg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCategoryFilter = cat
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = SleekSizes.textBodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isDeletable) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else SleekBorder.copy(alpha = 0.5f))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (selectedCategoryFilter.equals(cat, ignoreCase = true)) {
                                                selectedCategoryFilter = "All"
                                            }
                                            viewModel.deleteGoalCategory(cat)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Close,
                                        contentDescription = "Delete Category",
                                        tint = textColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Category Button Pill
                item {
                    Box(
                        modifier = Modifier
                            .clip(SleekShapes.lg)
                            .background(SleekPrimaryContainer.copy(alpha = 0.3f))
                            .border(1.dp, SleekPrimary.copy(alpha = 0.4f), SleekShapes.lg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCreateCategoryDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = "Add Category",
                                tint = SleekPrimary,
                                modifier = Modifier.size(SleekSizes.iconSmall)
                            )
                            Text(
                                text = "Category",
                                fontSize = SleekSizes.textBodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. 2-Column Grid of Goal Cards
            if (filteredGoals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SleekPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Savings,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (savingsGoals.isEmpty()) "No Saving Goals Created" else "No Goals in $selectedCategoryFilter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+ Add New' to set up your target saving goal!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredGoals, key = { goal: SavingsGoal -> goal.id }) { goal: SavingsGoal ->
                        SavingGoalCardItem(
                            goal = goal,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedGoalForDeposit = goal
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Add / Edit Goal Dialog
        if (showAddDialog || editingGoal != null) {
            AddEditSavingsGoalDialog(
                goalToEdit = editingGoal,
                viewModel = viewModel,
                onDismiss = {
                    showAddDialog = false
                    editingGoal = null
                },
                onSave = { title, target, category, imageUri, targetDate ->
                    val goalToEdit = editingGoal
                    if (goalToEdit != null) {
                        viewModel.updateSavingsGoal(
                            goalToEdit.copy(
                                name = title,
                                targetAmountMinor = Money.fromDouble(target, goalToEdit.currencyCode),
                                category = category,
                                imageUri = imageUri,
                                targetDate = targetDate
                            )
                        )
                    } else {
                        viewModel.addSavingsGoal(
                            name = title,
                            targetAmount = target,
                            initialAmount = 0.0,
                            targetDate = targetDate,
                            category = category,
                            imageUri = imageUri
                        )
                    }
                    showAddDialog = false
                    editingGoal = null
                }
            )
        }

        // Create Goal Category Dialog
        if (showCreateCategoryDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateCategoryDialog = false
                    newCategoryInput = ""
                },
                containerColor = SleekSurface,
                title = {
                    Text(
                        "Create Goal Category",
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Enter a new category name for your goals:",
                            fontSize = SleekSizes.textBodyMedium,
                            color = SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedTextField(
                            value = newCategoryInput,
                            onValueChange = { newCategoryInput = it },
                            label = { Text("Category Name (e.g. Gadgets, Vacation)") },
                            singleLine = true,
                            shape = SleekShapes.md,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = SleekBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCategoryInput.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val trimmed = newCategoryInput.trim()
                                viewModel.addGoalCategory(trimmed)
                                selectedCategoryFilter = trimmed
                                newCategoryInput = ""
                                showCreateCategoryDialog = false
                            }
                        },
                        shape = SleekShapes.md,
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Text("Create", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateCategoryDialog = false
                        newCategoryInput = ""
                    }) {
                        Text("Cancel", color = SleekTextSecondary)
                    }
                }
            )
        }

        // Goal Detail & Quick Deposit Sheet
        val expenses by viewModel.expenses.collectAsStateWithLifecycle()
        val selectedCurrencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
        val availableNetBalance = remember(expenses, selectedCurrencyCode) {
            val inc = expenses.realIncome(selectedCurrencyCode)
            val exp = expenses.realExpense(selectedCurrencyCode)
            (inc - exp).coerceAtLeast(0.0)
        }

        selectedGoalForDeposit?.let { goal ->
            GoalDepositDetailBottomSheet(
                goal = goal,
                availableNetBalance = availableNetBalance,
                onDismiss = { selectedGoalForDeposit = null },
                onDeposit = { amount ->
                    viewModel.quickDepositToGoal(goal, amount)
                    selectedGoalForDeposit = null
                    // Trigger coin rolling animation!
                    activeDepositAnimationInfo = Pair(amount, goal.name)
                },
                onDeduct = { amount ->
                    viewModel.quickDeductFromGoal(goal, amount)
                    selectedGoalForDeposit = null
                },
                onEdit = {
                    editingGoal = goal
                    selectedGoalForDeposit = null
                },
                onDelete = {
                    viewModel.deleteSavingsGoal(goal)
                    selectedGoalForDeposit = null
                }
            )
        }

        // Coin Deposit Animation Overlay (Video matched)
        activeDepositAnimationInfo?.let { (amt, name) ->
            CoinDepositAnimationDialog(
                amount = amt,
                goalName = name,
                onDismiss = { activeDepositAnimationInfo = null }
            )
        }
    }
}

// ==========================================
// 🎨 GOAL CARD ITEM (MATCHING REFERENCE IMAGE GRID)
// ==========================================
@Composable
fun SavingGoalCardItem(
    goal: SavingsGoal,
    onClick: () -> Unit
) {
    val progressRatio = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio.coerceIn(0f, 1f),
        animationSpec = appTween(750, AppleDecelerateEasing),
        label = "goalProgress"
    )

    val formattedDate = remember(goal.targetDate) {
        if (goal.targetDate > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.targetDate))
        } else {
            "Undefined / Open Goal"
        }
    }

    val primaryAccent = getCategoryAccentColor(goal.category)

    Card(
        shape = SleekShapes.xl,
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("savings_goal_item_${goal.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Circular Avatar Badge with surrounding Progress Ring Arc and Celebration Pulse
            SavingsGoalCelebrationPulse(
                isCompleted = progressRatio >= 1f,
                accentColor = primaryAccent
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background Track Ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = primaryAccent.copy(alpha = 0.15f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = primaryAccent,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Inner Avatar Image or Category Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(primaryAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!goal.imageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = goal.imageUri,
                                contentDescription = goal.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = getCategoryVectorIcon(goal.category, goal.name),
                                contentDescription = goal.category,
                                tint = primaryAccent,
                                modifier = Modifier.size(SleekSizes.iconMedium)
                            )
                        }
                    }
                }
            }

            // Goal Title
            Text(
                text = goal.name,
                fontSize = SleekSizes.textBodyLarge,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Target Date Subtitle
            Text(
                text = formattedDate,
                fontSize = SleekSizes.textCaption,
                color = SleekTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Goal Saved vs Target Amount Row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "₹%,.0f".format(goal.currentAmount),
                    fontSize = SleekSizes.textBody,
                    fontWeight = FontWeight.Bold,
                    color = primaryAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " of ₹%,.0f".format(goal.targetAmount),
                    fontSize = SleekSizes.textCaption,
                    color = SleekTextSecondary,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// 🪙 COIN DEPOSIT ANIMATION DIALOG (VIDEO MATCHED)
// ==========================================
@Composable
fun CoinDepositAnimationDialog(
    amount: Double,
    goalName: String,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var animPhase by remember { mutableIntStateOf(0) } // 0: Rolling, 1: Arcing, 2: Dropped into Wallet Slot

    val rollX by animateFloatAsState(
        targetValue = if (animPhase >= 1) 0f else -130f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "rollX"
    )

    val rollRotation by animateFloatAsState(
        targetValue = if (animPhase >= 1) 720f else 0f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "rollRotation"
    )

    val coinY by animateFloatAsState(
        targetValue = when (animPhase) {
            0 -> 0f
            1 -> -80f
            else -> 18f
        },
        animationSpec = tween(if (animPhase == 1) 380 else 420, easing = AppleDecelerateEasing),
        label = "coinY"
    )

    val coinScale by animateFloatAsState(
        targetValue = when (animPhase) {
            0 -> 1f
            1 -> 1.15f
            else -> 0.40f
        },
        animationSpec = tween(420, easing = AppleDecelerateEasing),
        label = "coinScale"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (animPhase == 2) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    val cardGlowAlpha by animateFloatAsState(
        targetValue = if (animPhase == 2) 0.6f else 0f,
        animationSpec = tween(380, easing = AppleDecelerateEasing),
        label = "cardGlowAlpha"
    )

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(120)
        animPhase = 1
        delay(420)
        animPhase = 2
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(1800)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = SleekShapes.xxl,
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Depositing to $goalName",
                        fontSize = SleekSizes.textBody,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Animation Canvas Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Horizontal rolling track line
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(2.dp)
                                .align(Alignment.Center)
                        ) {
                            drawLine(
                                color = SleekBorder,
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Wallet / Card Slot Container
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 20.dp)
                                .scale(cardScale)
                                .width(180.dp)
                                .height(105.dp)
                                .clip(SleekShapes.xl)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF7C3AED), Color(0xFF4C1D95))
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFFA78BFA).copy(alpha = cardGlowAlpha + 0.4f),
                                    shape = SleekShapes.xl
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Slot Opening Line
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .width(80.dp)
                                    .height(5.dp)
                                    .clip(SleekShapes.xs)
                                    .background(Color(0xFF2E1065))
                            )

                            // Subtle Card Icon / Brand Tag
                            Text(
                                text = "FS",
                                fontSize = SleekSizes.textTitle,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Animated Gold Coin with INR Symbol (₹)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = rollX.dp, y = coinY.dp)
                                .rotate(rollRotation)
                                .scale(coinScale)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFCA8A04))
                                    )
                                )
                                .border(2.dp, Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF713F12)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = animPhase == 2,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "+₹%,.2f Saved!".format(amount),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Deducted from usable balance & locked in goal",
                                fontSize = 11.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ➕ ADD / EDIT GOAL DIALOG
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditSavingsGoalDialog(
    goalToEdit: SavingsGoal? = null,
    viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, category: String, imageUri: String?, targetDate: Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val goalCategoriesList by viewModel.goalCategories.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(goalToEdit?.name ?: "") }
    var targetStr by remember { mutableStateOf(goalToEdit?.targetAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var category by remember { mutableStateOf(goalToEdit?.category ?: "Saving") }
    var imageUriStr by remember { mutableStateOf<String?>(goalToEdit?.imageUri) }

    var selectedPresetMonths by remember {
        mutableIntStateOf(if (goalToEdit != null && goalToEdit.targetDate <= 0L) 0 else 6)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showInlineAddCategory by remember { mutableStateOf(false) }
    var inlineCategoryName by remember { mutableStateOf("") }

    // Custom Image Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadFullResolutionBitmap(context, uri)
            if (bitmap != null) {
                val savedPath = saveBitmapToInternalStorage(context, bitmap, "goal")
                if (savedPath != null) {
                    imageUriStr = savedPath
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = SleekShapes.xxl,
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (goalToEdit == null) "Add New Saving Goal" else "Edit Saving Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Goal Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Goal Name (e.g. Holiday, Laptop)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    shape = SleekShapes.md,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_name_input")
                )

                // Target Amount Input in INR (₹)
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = {
                        targetStr = it
                        errorMessage = null
                    },
                    label = { Text("Total Target Money (INR ₹)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = SleekPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = SleekShapes.md,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_target_input")
                )

                // Category Selection
                Text(
                    text = "Select Category",
                    fontSize = SleekSizes.textBodySmall,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    goalCategoriesList.forEach { cat ->
                        val isSel = category.equals(cat, ignoreCase = true)
                        val isDeletable = cat !in listOf("Saving", "Investment", "Expenditure")

                        FilterChip(
                            selected = isSel,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                category = cat
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(cat, fontSize = SleekSizes.textBodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (isDeletable) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (category.equals(cat, ignoreCase = true)) {
                                                        category = "Saving"
                                                    }
                                                    viewModel.deleteGoalCategory(cat)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.Close,
                                                contentDescription = "Delete Category",
                                                tint = SleekTextSecondary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            shape = SleekShapes.md
                        )
                    }

                    FilterChip(
                        selected = showInlineAddCategory,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showInlineAddCategory = !showInlineAddCategory
                        },
                        label = { Text("+ New", fontSize = SleekSizes.textBodySmall, fontWeight = FontWeight.Bold, color = SleekPrimary) },
                        shape = SleekShapes.md
                    )
                }

                AnimatedVisibility(visible = showInlineAddCategory) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inlineCategoryName,
                            onValueChange = { inlineCategoryName = it },
                            placeholder = { Text("Category Name") },
                            singleLine = true,
                            shape = SleekShapes.md,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (inlineCategoryName.isNotBlank()) {
                                    val trimmed = inlineCategoryName.trim()
                                    viewModel.addGoalCategory(trimmed)
                                    category = trimmed
                                    inlineCategoryName = ""
                                    showInlineAddCategory = false
                                }
                            },
                            shape = SleekShapes.md,
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Text("Add", fontSize = SleekSizes.textBodySmall)
                        }
                    }
                }

                // Target Timeframe Presets
                Text(
                    text = "Target Timeframe",
                    fontSize = SleekSizes.textBodySmall,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        0 to "Undefined",
                        3 to "3 Mon",
                        6 to "6 Mon",
                        12 to "1 Year",
                        24 to "2 Years"
                    ).forEach { (months, label) ->
                        val isSel = selectedPresetMonths == months
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SleekShapes.md)
                                .background(if (isSel) SleekPrimary else SleekPrimaryContainer.copy(alpha = 0.2f))
                                .clickable { selectedPresetMonths = months }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = SleekSizes.textMicro,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Goal Image Option
                Text(
                    text = "Goal Image (Optional)",
                    fontSize = SleekSizes.textBodySmall,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SleekPrimary.copy(alpha = 0.12f))
                            .border(1.dp, SleekBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!imageUriStr.isNullOrBlank()) {
                            AsyncImage(
                                model = imageUriStr,
                                contentDescription = "Custom Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = getCategoryVectorIcon(category, name),
                                contentDescription = "Default Category Icon",
                                tint = SleekPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val suppressAutoLock = LocalAutoLockSuppressor.current
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    suppressAutoLock()
                                    imagePickerLauncher.launch("image/*")
                                },
                                shape = SleekShapes.md,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Choose Image", fontSize = SleekSizes.textCaption)
                            }

                            if (!imageUriStr.isNullOrBlank()) {
                                TextButton(
                                    onClick = { imageUriStr = null }
                                ) {
                                    Text("Remove", fontSize = SleekSizes.textCaption, color = Color(0xFFEF4444))
                                }
                            }
                        }
                        Text(
                            text = if (imageUriStr == null) "A category icon avatar will be created automatically" else "Custom image attached",
                            fontSize = SleekSizes.textMicro,
                            color = SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                errorMessage?.let { err ->
                    Text(err, color = Color(0xFFEF4444), fontSize = SleekSizes.textBodySmall)
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SleekTextSecondary, fontSize = SleekSizes.textBodyMedium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val targetVal = targetStr.toDoubleOrNull()
                            if (name.isBlank()) {
                                errorMessage = "Please enter a goal name"
                                return@Button
                            }
                            if (targetVal == null || targetVal <= 0) {
                                errorMessage = "Please enter a valid target amount"
                                return@Button
                            }

                            val calcDate = if (selectedPresetMonths == 0) {
                                0L
                            } else {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.MONTH, selectedPresetMonths)
                                cal.timeInMillis
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(name.trim(), targetVal, category, imageUriStr, calcDate)
                        },
                        shape = SleekShapes.md,
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Text(if (goalToEdit == null) "Save Goal" else "Update", color = Color.White, fontSize = SleekSizes.textBodyMedium)
                    }
                }
            }
        }
    }
}

// ==========================================
// 🔍 GOAL DEPOSIT & DETAIL BOTTOM SHEET
// ==========================================
@Composable
fun GoalDepositDetailBottomSheet(
    goal: SavingsGoal,
    availableNetBalance: Double = 0.0,
    onDismiss: () -> Unit,
    onDeposit: (Double) -> Unit,
    onDeduct: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isDepositMode by remember { mutableStateOf(true) } // true: Deposit, false: Withdraw
    var depositInput by remember { mutableStateOf("") }
    var isAmountEditing by remember { mutableStateOf(false) }

    val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    val primaryAccent = getCategoryAccentColor(goal.category)
    val activeColor = if (isDepositMode) Color(0xFF10B981) else Color(0xFFEF4444)
    val activeSoftBg = if (isDepositMode) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)

    val enteredAmount = depositInput.toDoubleOrNull() ?: 0.0
    val isExceedingNetBalance = isDepositMode && (enteredAmount > availableNetBalance || availableNetBalance <= 0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = SleekShapes.xxl,
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with Goal Avatar & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(primaryAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!goal.imageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = goal.imageUri,
                                    contentDescription = goal.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = getCategoryVectorIcon(goal.category, goal.name),
                                    contentDescription = goal.category,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Category: ${goal.category}",
                                fontSize = SleekSizes.textBodySmall,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(AppIcons.Edit, contentDescription = "Edit", tint = SleekPrimary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(AppIcons.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                    }
                }

                // Goal Status Banner
                Surface(
                    shape = SleekShapes.lg,
                    color = primaryAccent.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text("Saved so far", fontSize = SleekSizes.textCaption, color = SleekTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "₹%,.2f".format(goal.currentAmount),
                                fontSize = SleekSizes.textTitle,
                                fontWeight = FontWeight.Black,
                                color = primaryAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f, fill = false)) {
                            Text("Target Amount", fontSize = SleekSizes.textCaption, color = SleekTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "₹%,.2f".format(goal.targetAmount),
                                fontSize = SleekSizes.textBody,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Circular Transaction Type Switcher (Matching Main Balance Popup Style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deposit Circle Button
                    val isDep = isDepositMode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(SleekShapes.xl)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isDepositMode = true
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isDep) Color(0xFFDCFCE7) else SleekBg)
                                .border(
                                    width = if (isDep) 2.dp else 1.dp,
                                    color = if (isDep) Color(0xFF10B981) else SleekBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.ArrowDownward,
                                contentDescription = "Deposit to Goal",
                                tint = if (isDep) Color(0xFF10B981) else SleekTextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Deposit",
                            fontSize = SleekSizes.textBodySmall,
                            fontWeight = if (isDep) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDep) Color(0xFF10B981) else SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Withdraw Circle Button
                    val isWith = !isDepositMode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(SleekShapes.xl)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isDepositMode = false
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isWith) Color(0xFFFEE2E2) else SleekBg)
                                .border(
                                    width = if (isWith) 2.dp else 1.dp,
                                    color = if (isWith) Color(0xFFEF4444) else SleekBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.ArrowUpward,
                                contentDescription = "Withdraw from Goal",
                                tint = if (isWith) Color(0xFFEF4444) else SleekTextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Withdraw",
                            fontSize = SleekSizes.textBodySmall,
                            fontWeight = if (isWith) FontWeight.Bold else FontWeight.Medium,
                            color = if (isWith) Color(0xFFEF4444) else SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Big Amount Display Box (Circular Dial Style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SleekShapes.xl)
                        .background(activeSoftBg)
                        .border(1.dp, activeColor.copy(alpha = 0.4f), SleekShapes.xl)
                        .clickable { isAmountEditing = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isAmountEditing) {
                            OutlinedTextField(
                                value = depositInput,
                                onValueChange = { depositInput = it },
                                placeholder = { Text("0", fontSize = SleekSizes.textHeadlineLarge, color = SleekTextSecondary) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = SleekSizes.textHeadlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = activeColor,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.width(140.dp)
                            )
                        } else {
                            val formatted = if (enteredAmount > 0) "₹%,.0f".format(enteredAmount) else "₹0"
                            Text(
                                text = formatted,
                                fontSize = SleekSizes.textHeadlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeColor,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enteredAmount > 0) "Tap to edit amount" else "Tap to enter transaction amount",
                                fontSize = SleekSizes.textCaption,
                                color = SleekTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Preset Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(100.0, 500.0, 1000.0, 2000.0).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SleekShapes.md)
                                .background(activeSoftBg)
                                .border(1.dp, activeColor.copy(alpha = 0.2f), SleekShapes.md)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val current = depositInput.toDoubleOrNull() ?: 0.0
                                    depositInput = "%.0f".format(current + preset)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+₹%,.0f".format(preset),
                                fontSize = SleekSizes.textCaption,
                                fontWeight = FontWeight.Bold,
                                color = activeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Available Net Balance & Lock Warning Card
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Available Net Balance:",
                            fontSize = SleekSizes.textCaption,
                            color = SleekTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "₹%,.2f".format(availableNetBalance),
                            fontSize = SleekSizes.textBodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (availableNetBalance > 0) Color(0xFF10B981) else Color(0xFFEF4444),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isDepositMode && isExceedingNetBalance) {
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
                                Icon(
                                    imageVector = AppIcons.Lock,
                                    contentDescription = "Balance Locked",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "🔒 Total Balance Locked: Deposit of ₹%,.2f exceeds available net balance (₹%,.2f). Add Income first!".format(enteredAmount, availableNetBalance),
                                    fontSize = SleekSizes.textCaption,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                // Main Action Confirm Button
                Button(
                    onClick = {
                        if (enteredAmount <= 0) return@Button
                        if (isDepositMode) {
                            if (isExceedingNetBalance) return@Button
                            onDeposit(enteredAmount)
                        } else {
                            if (enteredAmount > goal.currentAmount) {
                                onDeduct(goal.currentAmount)
                            } else {
                                onDeduct(enteredAmount)
                            }
                        }
                    },
                    enabled = enteredAmount > 0 && !(isDepositMode && isExceedingNetBalance),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        disabledContainerColor = activeColor.copy(alpha = 0.3f)
                    ),
                    shape = SleekShapes.lg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SleekSizes.buttonHeightLarge)
                ) {
                    Text(
                        text = if (isDepositMode && isExceedingNetBalance) "🔒 Net Balance Locked"
                               else if (isDepositMode && enteredAmount > 0) "Confirm Deposit (+₹%,.0f)".format(enteredAmount)
                               else if (!isDepositMode && enteredAmount > 0) "Confirm Withdrawal (-₹%,.0f)".format(enteredAmount)
                               else "Enter Amount",
                        color = Color.White,
                        fontSize = SleekSizes.textBodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==========================================
// 🎨 CATEGORY VECTOR & ACCENT HELPERS
// ==========================================
fun getCategoryVectorIcon(category: String, name: String): ImageVector {
    val combined = "$category $name".lowercase()
    return when {
        combined.contains("holiday") || combined.contains("travel") || combined.contains("trip") || combined.contains("beach") -> AppIcons.BeachAccess
        combined.contains("laptop") || combined.contains("tech") || combined.contains("phone") || combined.contains("gadget") -> AppIcons.Laptop
        combined.contains("education") || combined.contains("study") || combined.contains("school") || combined.contains("college") -> AppIcons.School
        combined.contains("shopping") || combined.contains("cloth") || combined.contains("fashion") -> AppIcons.ShoppingBag
        combined.contains("car") || combined.contains("vehicle") || combined.contains("bike") -> AppIcons.DirectionsCar
        combined.contains("investment") || combined.contains("stock") || combined.contains("mutual") -> AppIcons.TrendingUp
        combined.contains("emergency") || combined.contains("health") || combined.contains("medical") -> AppIcons.Shield
        else -> AppIcons.Savings
    }
}

fun getCategoryAccentColor(category: String): Color {
    return when (category.lowercase()) {
        "investment" -> Color(0xFF10B981)
        "expenditure", "shopping" -> Color(0xFFF59E0B)
        "travel", "holiday" -> Color(0xFF0EA5E9)
        "tech" -> Color(0xFF6366F1)
        "education" -> Color(0xFF8B5CF6)
        "vehicle" -> Color(0xFFEC4899)
        "emergency" -> Color(0xFFEF4444)
        else -> Color(0xFF0D9488) // Sleek teal
    }
}

// ==========================================
// 📌 LEGACY EMBEDDED SECTION (MAINTAINED)
// ==========================================
@Composable
fun SavingsGoalsSection(
    savingsGoals: List<SavingsGoal>,
    accounts: List<Account> = emptyList(),
    selectedLanguage: String = "English",
    onAddGoalClick: () -> Unit,
    onEditGoalClick: (SavingsGoal) -> Unit,
    onDeleteGoalClick: (SavingsGoal) -> Unit,
    onDepositGoalClick: (SavingsGoal) -> Unit,
    onDeductGoalClick: ((SavingsGoal) -> Unit)? = null,
    onQuickDeposit: (SavingsGoal, Double) -> Unit,
    onQuickDeduct: ((SavingsGoal, Double) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saving Goals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            OutlinedButton(
                onClick = onAddGoalClick,
                shape = SleekShapes.xl,
                border = BorderStroke(1.dp, SleekPrimary)
            ) {
                Text("+ Add New", fontSize = SleekSizes.textBodySmall, color = SleekPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (savingsGoals.isEmpty()) {
            Text("No saving goals yet. Tap '+ Add New' to get started!", fontSize = SleekSizes.textBodySmall, color = SleekTextSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                savingsGoals.forEach { goal ->
                    SavingGoalCardItem(
                        goal = goal,
                        onClick = { onDepositGoalClick(goal) }
                    )
                }
            }
        }
    }
}
