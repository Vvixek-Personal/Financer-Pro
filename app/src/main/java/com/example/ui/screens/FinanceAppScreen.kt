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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import android.content.Context
import kotlin.math.roundToInt
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import com.example.BuildConfig
import coil.compose.AsyncImage

private const val TAG = "FinanceAppScreen"

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", AppIcons.Dashboard)
    object Expenses : Screen("expenses", "Finance", AppIcons.ReceiptLong)
    object Analytics : Screen("analytics", "Analytics", AppIcons.PieChart)
    object Calendar : Screen("calendar", "Calendar", AppIcons.CalendarMonth)
}

fun getCategoryEmoji(category: String, customMap: Map<String, String> = emptyMap()): String {
    val resolved = (customMap[category] ?: category).lowercase(java.util.Locale.getDefault())
    return when (resolved) {
        "food", "dining", "restaurant", "baking", "fastfood" -> "🍔"
        "travel", "flight", "aviation", "taxi", "transportation", "flighttakeoff" -> "✈️"
        "rent", "home", "housing", "real estate" -> "🏠"
        "utilities", "electricity", "water", "gas", "electricbolt", "bolt" -> "💡"
        "entertainment", "gaming", "movies", "cinema", "sportsesports", "movie" -> "🎮"
        "shopping", "clothing", "gear", "shoppingcart" -> "🛍️"
        "persons", "friends", "family", "dogs", "cats", "pets", "person" -> "👥"
        "salary", "income", "finance", "monetizationon" -> "💵"
        "freelance", "work" -> "💼"
        "investments", "crypto" -> "🪙"
        "gifts", "gift", "cardgiftcard" -> "🎁"
        "art" -> "🎨"
        "botany" -> "🌱"
        "cars", "car", "directionscar" -> "🚗"
        "technology", "programming" -> "💻"
        "fashion" -> "👗"
        "birds" -> "🐦"
        "health care", "healthcare", "medical", "localhospital", "healing" -> "🏥"
        "geography" -> "🗺️"
        "lgbtq" -> "🏳️‍🌈"
        "mental health" -> "🧠"
        "sports", "fitness", "fitnesscenter" -> "⚽"
        "photography" -> "📷"
        "design" -> "🖋️"
        "ufo" -> "🛸"
        "music" -> "🎶"
        "school" -> "🏫"
        "settings" -> "⚙️"
        "star" -> "⭐️"
        "construction" -> "🔨"
        "coffee" -> "☕"
        "waterdrop" -> "💧"
        "checkroom" -> "🧥"
        "directionsbus" -> "🚌"
        "localgasstation" -> "⛽"
        "event" -> "📅"
        "spa" -> "🧖"
        "pending" -> "⏳"
        else -> "📦"
    }
}

// Category palette helper
val categoryColors = mapOf(
    "Food" to Color(0xFFF97316),        // Orange
    "Travel" to Color(0xFF0D9488),      // Teal
    "Rent" to Color(0xFF2563EB),        // Blue
    "Utilities" to Color(0xFF16A34A),   // Green
    "Entertainment" to Color(0xFFE11D48),// Rose
    "Shopping" to Color(0xFF9333EA),     // Purple
    "Persons" to Color(0xFF0EA5E9),      // Sky Blue (Persons Category)
    "Others" to Color(0xFF64748B)        // Slate
)

fun getCategoryIcon(category: String, customMap: Map<String, String> = emptyMap()): ImageVector {
    val resolved = customMap[category] ?: category
    return when (resolved) {
        "Food", "Restaurant" -> AppIcons.Restaurant
        "Travel", "DirectionsCar" -> AppIcons.DirectionsCar
        "Rent", "Home" -> AppIcons.Home
        "Utilities", "Bolt", "ElectricBolt" -> AppIcons.Bolt
        "Entertainment", "Movie" -> AppIcons.Movie
        "Shopping", "ShoppingCart" -> AppIcons.ShoppingCart
        "Persons", "Person" -> AppIcons.Person
        "LocalHospital", "Healing" -> AppIcons.LocalHospital
        "School" -> AppIcons.School
        "Work" -> AppIcons.Work
        "Flight", "FlightTakeoff" -> AppIcons.Flight
        "SportsEsports" -> AppIcons.SportsEsports
        "CardGiftcard" -> AppIcons.CardGiftcard
        "MonetizationOn" -> AppIcons.MonetizationOn
        "Settings" -> AppIcons.Settings
        "Pets" -> AppIcons.Pets
        "Star" -> AppIcons.Star
        "Construction" -> AppIcons.Construction
        "Fastfood" -> AppIcons.Fastfood
        "Coffee" -> AppIcons.Coffee
        "WaterDrop" -> AppIcons.WaterDrop
        "Checkroom" -> AppIcons.Checkroom
        "DirectionsBus" -> AppIcons.DirectionsBus
        "LocalGasStation" -> AppIcons.LocalGasStation
        "FitnessCenter" -> AppIcons.FitnessCenter
        "Event" -> AppIcons.Event
        "Spa" -> AppIcons.Spa
        "Pending" -> AppIcons.Pending
        else -> AppIcons.Category
    }
}

val LocalAutoLockSuppressor = compositionLocalOf<() -> Unit> { {} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppScreen(viewModel: FinanceViewModel) {
    val animPref by viewModel.isAnimationEnabled.collectAsStateWithLifecycle()
    val animEnabled = animPref && systemAnimationsAllowed()
    CompositionLocalProvider(
        LocalAutoLockSuppressor provides { viewModel.suppressNextAutoLock() },
        LocalAnimationsEnabled provides animEnabled
    ) {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val selectedDateRange by viewModel.selectedDateRange.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val categoryIcons by viewModel.categoryIcons.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val selectedCurrencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()

    val globalLoadingMessage by viewModel.appLoadingMessage.collectAsStateWithLifecycle()
    var isInitialAppLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(950)
        isInitialAppLoading = false
    }

    val appPin by viewModel.appPin.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val hasPromptedFirstRunPin by viewModel.hasPromptedFirstRunPin.collectAsStateWithLifecycle()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var customTxTypeForAddDialog by remember { mutableStateOf<String?>(null) }
    var prefilledDateForAddDialog by remember { mutableStateOf<Long?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var viewingDetailExpense by remember { mutableStateOf<Expense?>(null) }
    var recordedTransactionInfo by remember { mutableStateOf<RecordedTransactionInfo?>(null) }

    var activeSettingsSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerFullscreen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()

    // Always reopen at normal width
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) drawerFullscreen = false
    }

    val navigateToScreen: (Screen) -> Unit = { selected ->
        activeSettingsSubScreen = null
        if (currentScreen != selected) {
            backStack.add(selected)
            currentScreen = selected
        }
    }

    // Close drawer automatically if app gets locked
    LaunchedEffect(isAppLocked, appPin) {
        if (isAppLocked && !appPin.isNullOrBlank()) {
            drawerState.close()
        }
    }

    // Re-lock app according to Inactivity Auto-Lock duration when foregrounded
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appPin) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    viewModel.onAppBackgrounded()
                }
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    viewModel.onAppForegrounded()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept back button when locked
    BackHandler(enabled = isAppLocked && !appPin.isNullOrBlank()) {
        // Do nothing on back button during lock screen
    }

    // SYSTEM BACK BUTTON HANDLER (Pops navigation stack, closes drawer/settings/dialogs)
    val canHandleBack = (!isAppLocked || appPin.isNullOrBlank()) && (
            drawerFullscreen ||
            drawerState.isOpen ||
            activeSettingsSubScreen != null ||
            viewingDetailExpense != null ||
            editingExpense != null ||
            showAddExpenseDialog ||
            backStack.size > 1 ||
            currentScreen != Screen.Dashboard
    )

    val locked = isAppLocked && !appPin.isNullOrBlank()

    BackHandler(enabled = canHandleBack) {
        when {
            activeSettingsSubScreen == SettingsSubScreen.LocalRecovery -> {
                activeSettingsSubScreen = SettingsSubScreen.DataManagement
            }
            activeSettingsSubScreen != null -> activeSettingsSubScreen = null
            drawerFullscreen -> drawerFullscreen = false
            drawerState.isOpen -> scope.launch { drawerState.close() }
            viewingDetailExpense != null -> viewingDetailExpense = null
            editingExpense != null -> editingExpense = null
            showAddExpenseDialog -> showAddExpenseDialog = false
            backStack.size > 1 -> {
                backStack.removeAt(backStack.lastIndex)
                currentScreen = backStack.last()
            }
            currentScreen != Screen.Dashboard -> {
                backStack.clear()
                backStack.add(Screen.Dashboard)
                currentScreen = Screen.Dashboard
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            modifier = if (locked) Modifier.clearAndSetSemantics { } else Modifier,
            drawerState = drawerState,
            gesturesEnabled = !locked && activeSettingsSubScreen == null,
            drawerContent = {
                val fullscreenAnim = remember { Animatable(if (drawerFullscreen) 1f else 0f) }
                LaunchedEffect(drawerFullscreen) {
                    fullscreenAnim.animateTo(
                        targetValue = if (drawerFullscreen) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = 420,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        )
                    )
                }

                ModalDrawerSheet(
                    drawerContainerColor = Color.Transparent,
                    drawerTonalElevation = 0.dp,
                    drawerShape = RectangleShape,
                    modifier = Modifier
                        .fillMaxHeight()
                        .layout { measurable, constraints ->
                            val progress = fullscreenAnim.value
                            val screenW = constraints.maxWidth
                            val baseW = 330.dp.roundToPx().coerceAtMost(screenW)
                            val w = (baseW + (screenW - baseW) * progress).roundToInt()
                            val topPad = (44.dp.roundToPx() * (1f - progress)).roundToInt()
                            val botPad = (44.dp.roundToPx() * (1f - progress)).roundToInt()
                            val endPad = (12.dp.roundToPx() * (1f - progress)).roundToInt()

                            val targetW = (w - endPad).coerceAtLeast(0)
                            val targetH = (constraints.maxHeight - topPad - botPad).coerceAtLeast(0)
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = targetW,
                                    maxWidth = targetW,
                                    minHeight = targetH,
                                    maxHeight = targetH
                                )
                            )
                            layout(w, constraints.maxHeight) {
                                placeable.place(0, topPad)
                            }
                        }
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topEnd = 24.dp * (1f - fullscreenAnim.value),
                            bottomEnd = 24.dp * (1f - fullscreenAnim.value),
                            topStart = 0.dp,
                            bottomStart = 0.dp
                        ),
                        color = SleekBg,
                        border = BorderStroke(1.dp, SleekBorder),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(drawerState.currentValue, drawerFullscreen) {
                                if (drawerState.currentValue != DrawerValue.Open) return@pointerInput
                                val trigger = 36.dp.toPx()
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val first = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                        if ((!drawerFullscreen && overSlop > 0f) ||
                                            (drawerFullscreen && overSlop < 0f)) change.consume()
                                    }
                                    if (first != null) {
                                        var total = 0f
                                        horizontalDrag(first.id) { c ->
                                            total += c.positionChange().x
                                            c.consume()
                                        }
                                        if (!drawerFullscreen && total > trigger) {
                                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            drawerFullscreen = true
                                        } else if (drawerFullscreen && total < -trigger) {
                                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            drawerFullscreen = false
                                        }
                                    }
                                }
                            }
                    ) {
                        SidebarDrawerContent(
                            viewModel = viewModel,
                            onCloseDrawer = { scope.launch { drawerState.close() } },
                            onOpenSettingsScreen = { subScreen ->
                                activeSettingsSubScreen = subScreen
                                scope.launch { drawerState.close() }
                                drawerFullscreen = false
                            },
                            onChangePasswordClick = {
                                showChangePinDialog = true
                            },
                            isFullscreen = drawerFullscreen,
                            onToggleFullscreen = {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                drawerFullscreen = !drawerFullscreen
                            }
                        )
                    }
                }
            }
        ) {
        val drawerParallaxProgress by animateFloatAsState(
            targetValue = if (drawerState.targetValue == DrawerValue.Open) 1f else 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "drawer_parallax"
        )
        Scaffold(
            modifier = Modifier.graphicsLayer {
                translationX = drawerParallaxProgress * 24.dp.toPx()
            },
        bottomBar = {
            if (!isAppLocked || appPin.isNullOrBlank()) {
                FloatingDockBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { selected -> navigateToScreen(selected) },
                    selectedLanguage = viewModel.selectedLanguage.collectAsStateWithLifecycle().value
                )
            }
        },
        containerColor = SleekBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isAnimEnabled = LocalAnimationsEnabled.current
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (isAnimEnabled) {
                        val screenOrder = listOf(Screen.Dashboard, Screen.Expenses, Screen.Analytics, Screen.Calendar)
                        val fromIndex = screenOrder.indexOf(initialState).coerceAtLeast(0)
                        val toIndex = screenOrder.indexOf(targetState).coerceAtLeast(0)
                        val isForward = toIndex >= fromIndex
                        val slideOffsetFraction = if (isForward) 0.12f else -0.12f

                        (slideInHorizontally(
                            initialOffsetX = { (it * slideOffsetFraction).roundToInt() },
                            animationSpec = tween(durationMillis = 280, easing = AppleDecelerateEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 240, easing = AppleDecelerateEasing)
                        ) + scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(durationMillis = 280, easing = AppleDecelerateEasing)
                        )) togetherWith (
                            slideOutHorizontally(
                                targetOffsetX = { (-it * slideOffsetFraction).roundToInt() },
                                animationSpec = tween(durationMillis = 220, easing = AppleDecelerateEasing)
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                            )
                        )
                    } else {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                },
                label = "FluidScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> DashboardTab(
                        expenses = filteredExpenses,
                        userName = userName,
                        monthlyBudget = monthlyBudget,
                        categoryIcons = categoryIcons,
                        onUpdateBudget = { viewModel.updateMonthlyBudget(it) },
                        onUpdateName = { viewModel.saveUserName(it) },
                        onAddExpenseClick = {
                            customTxTypeForAddDialog = "EXPENSE"
                            prefilledDateForAddDialog = null
                            showAddExpenseDialog = true
                        },
                        onAddIncomeClick = {
                            customTxTypeForAddDialog = "INCOME"
                            prefilledDateForAddDialog = null
                            showAddExpenseDialog = true
                        },
                        onNavigateToExpenses = { navigateToScreen(Screen.Expenses) },
                        onNavigateToAnalytics = { navigateToScreen(Screen.Analytics) },
                        onProfileClick = { scope.launch { drawerState.open() } },
                        onEditExpenseClick = { viewingDetailExpense = it },
                        onOpenSettingsSubScreen = { activeSettingsSubScreen = it },
                        viewModel = viewModel
                    )
                    Screen.Expenses -> ExpensesTab(
                        viewModel = viewModel,
                        onAddExpenseClick = {
                            prefilledDateForAddDialog = null
                            showAddExpenseDialog = true
                        },
                        onEditExpenseClick = { exp: Expense -> viewingDetailExpense = exp }
                    )
                    Screen.Analytics -> AnalyticsTab(
                        viewModel = viewModel,
                        onAddClick = {
                            activeSettingsSubScreen = null
                            prefilledDateForAddDialog = null
                            showAddExpenseDialog = true
                        },
                        onProfileClick = { scope.launch { drawerState.open() } },
                        onExpenseClick = { exp: Expense -> viewingDetailExpense = exp }
                    )
                    Screen.Calendar -> EnhancedCalendarTab(
                        expenses = expenses,
                        categoryIcons = categoryIcons,
                        currencySymbol = currencySymbol,
                        currencyCode = selectedCurrencyCode,
                        billsList = viewModel.billsList,
                        onAddExpenseForDate = { date, type ->
                            prefilledDateForAddDialog = date
                            customTxTypeForAddDialog = type
                            showAddExpenseDialog = true
                        },
                        onEditExpense = { viewingDetailExpense = it },
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                }
            }

            if (showAddExpenseDialog) {
                val defaultTxType by viewModel.defaultTxType.collectAsStateWithLifecycle()
                val rememberLastCategory by viewModel.rememberLastCategory.collectAsStateWithLifecycle()
                val lastUsedExpenseCategory by viewModel.lastUsedExpenseCategory.collectAsStateWithLifecycle()
                val lastUsedIncomeCategory by viewModel.lastUsedIncomeCategory.collectAsStateWithLifecycle()
                val isGstEnabledForDialog by viewModel.isGstEnabled.collectAsStateWithLifecycle()
                val gstRateForDialog by viewModel.gstRatePercent.collectAsStateWithLifecycle()
                val isMonthlySafeEnabledForDialog by viewModel.isMonthlySafeEnabled.collectAsStateWithLifecycle()
                val monthlySafeAmountForDialog by viewModel.monthlySafeAmount.collectAsStateWithLifecycle()
                AddExpenseDialog(
                    prefilledDate = prefilledDateForAddDialog,
                    categories = allCategories,
                    expenseCategories = expenseCategories,
                    incomeCategories = incomeCategories,
                    categoryIcons = categoryIcons,
                    expenses = expenses,
                    defaultTxType = customTxTypeForAddDialog ?: defaultTxType,
                    rememberLastCategory = rememberLastCategory,
                    lastUsedExpenseCategory = lastUsedExpenseCategory,
                    lastUsedIncomeCategory = lastUsedIncomeCategory,
                    gstReserveAmount = if (isGstEnabledForDialog) viewModel.getGstReserveAmount() else 0.0,
                    monthlySafeAmount = if (isMonthlySafeEnabledForDialog) monthlySafeAmountForDialog else 0.0,
                    currencySymbol = currencySymbol,
                    currencyCode = selectedCurrencyCode,
                    onAddCategory = { name, catType -> viewModel.addCustomCategory(name, catType) },
                    onDeleteCategory = { viewModel.deleteCustomCategory(it) },
                    onEditCategory = { old, new -> viewModel.renameCustomCategory(old, new) },
                    onDismiss = {
                        showAddExpenseDialog = false
                        customTxTypeForAddDialog = null
                    },
                    onConfirm = { amount, category, date, note, imagePath, type ->
                        viewModel.addExpense(amount, category, date, note, imagePath, type, selectedCurrencyCode)
                        viewModel.refreshUsageData()
                        showAddExpenseDialog = false
                        customTxTypeForAddDialog = null
                        recordedTransactionInfo = RecordedTransactionInfo(
                            amount = amount,
                            category = category,
                            type = type,
                            note = note
                        )
                    }
                )
            }

            val recInfo = recordedTransactionInfo
            if (recInfo != null) {
                TransactionSuccessDialog(
                    info = recInfo,
                    onDismiss = { recordedTransactionInfo = null }
                )
            }

            val editExp = editingExpense
            if (editExp != null) {
                EditExpenseDialog(
                    expense = editExp,
                    categories = allCategories,
                    expenseCategories = expenseCategories,
                    incomeCategories = incomeCategories,
                    categoryIcons = categoryIcons,
                    currencySymbol = currencySymbol,
                    currencyCode = selectedCurrencyCode,
                    expenses = expenses,
                    onAddCategory = { name, catType -> viewModel.addCustomCategory(name, catType) },
                    onDeleteCategory = { viewModel.deleteCustomCategory(it) },
                    onEditCategory = { old, new -> viewModel.renameCustomCategory(old, new) },
                    onDismiss = { editingExpense = null },
                    onConfirm = { updatedExpense ->
                        viewModel.updateExpense(updatedExpense)
                        editingExpense = null
                    }
                )
            }

            val viewExp = viewingDetailExpense
            if (viewExp != null) {
                ExpenseDetailDialog(
                    expense = viewExp,
                    viewModel = viewModel,
                    onDismiss = { viewingDetailExpense = null },
                    onEditClick = {
                        val target = viewingDetailExpense
                        viewingDetailExpense = null
                        if (target != null) {
                            editingExpense = target
                        }
                    },
                    onDeleteClick = {
                        val target = viewingDetailExpense
                        viewingDetailExpense = null
                        if (target != null) {
                            viewModel.deleteExpense(target)
                        }
                    }
                )
            }
        }
    } // End ModalNavigationDrawer

    // 🍎 Apple Settings Native Push & Pop Navigation Transition Suite
    val settingsAnimEnabled = LocalAnimationsEnabled.current
    AnimatedContent(
        targetState = activeSettingsSubScreen,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            if (!settingsAnimEnabled) {
                (EnterTransition.None togetherWith ExitTransition.None)
                    .using(SizeTransform(clip = false) { _, _ -> snap() })
            } else if (targetState != null) {
                // Fluid slide-in from right edge with subtle fade and 20% parallax exit
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(
                        durationMillis = 320,
                        easing = AppleDecelerateEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 220, easing = AppleDecelerateEasing)
                )).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth * 20 / 100 },
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = AppleDecelerateEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                    )
                ).using(
                    SizeTransform(clip = false) { _, _ -> snap() }
                )
            } else {
                // Fluid back pop to right edge with 20% parallax return
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth * 20 / 100 },
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = AppleDecelerateEasing
                    )
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 200, easing = AppleDecelerateEasing)
                )).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(
                            durationMillis = 280,
                            easing = AppleDecelerateEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                    )
                ).using(
                    SizeTransform(clip = false) { _, _ -> snap() }
                )
            }
        },
        label = "apple_settings_navigation_transition"
    ) { targetScreen ->
        if (targetScreen != null) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 16.dp, clip = false),
                color = SleekBg
            ) {
                when (targetScreen) {
                    SettingsSubScreen.PersonalData -> PersonalDataScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.BadgesAndMilestones -> BadgesAndMilestonesScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Appearance -> AppearanceScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Language -> LanguageScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Currency -> CurrencySettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.DateTime -> DateTimeScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Bills -> BillsSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.CategoriesTags -> CategoriesTagsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Budgets -> BudgetSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.SavingsGoals -> SavingsGoalsSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Calculations -> CalculationsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Transactions -> TransactionSettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.BackupRestore -> BackupRestoreScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.DataManagement -> DataManagementScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null },
                        onNavigateToLocalRecovery = { activeSettingsSubScreen = SettingsSubScreen.LocalRecovery }
                    )
                    SettingsSubScreen.LocalRecovery -> LocalRecoveryScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = SettingsSubScreen.DataManagement }
                    )
                    SettingsSubScreen.Security -> SecuritySettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Privacy -> PrivacySettingsScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.AboutApp -> AboutAppScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.HelpSupport -> HelpSupportScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.DataAndStorage -> DataAndStorageScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.ThemeAndLanguage -> ThemeAndLanguageScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.Export -> ExportDataScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.FaqAndHelp -> FaqAndHelpScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize())
        }
    }

    // Full Screen Lock Overlay if app is locked with a PIN
    if (locked) {
        PinLockScreen(viewModel = viewModel)
    }

    // First Run Optional PIN Setup Prompt
    if (!locked && appPin.isNullOrBlank() && !hasPromptedFirstRunPin) {
        FirstRunPinSetupDialog(
            onSetPin = { newPin ->
                viewModel.setAppPin(newPin)
                viewModel.markFirstRunPinPrompted()
            },
            onMaybeLater = {
                viewModel.markFirstRunPinPrompted()
            }
        )
    }

    // Change Password / PIN Dialog
    if (showChangePinDialog && !locked) {
        ChangePinDialog(
            currentAppPin = appPin,
            onDismiss = { showChangePinDialog = false },
            onSavePin = { newPin ->
                viewModel.setAppPin(newPin)
            }
        )
    }

    if (!locked && userName.isNullOrBlank()) {
        OnboardingNameDialog(onSave = { viewModel.saveUserName(it) })
    }

    // 🚚 Fullscreen Delivery Truck Loader for app startup
    if (isInitialAppLoading && !locked) {
        TruckLoadingScreen(
            message = "Loading Your Financial Workspace...",
            subtitle = "Preparing secure offline ledgers, accounts & charts"
        )
    }

    // 🚚 Global Truck Loading Dialog for asynchronous tasks
    TruckLoadingDialog(
        isOpen = globalLoadingMessage != null && !locked,
        message = globalLoadingMessage ?: "Processing...",
        subtitle = "Please wait while your task completes safely",
        onDismissRequest = { viewModel.hideAppLoading() }
    )
    }
}
}
}

// ==========================================
// 1️⃣ DASHBOARD TAB
// ==========================================
@Composable
fun DashboardTab(
    expenses: List<Expense>,
    userName: String?,
    monthlyBudget: Double,
    categoryIcons: Map<String, String> = emptyMap(),
    onUpdateBudget: (Double) -> Unit,
    onUpdateName: (String) -> Unit,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit = {},
    onNavigateToExpenses: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onProfileClick: () -> Unit,
    onEditExpenseClick: (Expense) -> Unit,
    onOpenSettingsSubScreen: (SettingsSubScreen) -> Unit = {},
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val selectedCurrencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val billShowUpcomingDashboard by viewModel.billShowUpcomingDashboard.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.userProfileImageUri.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val showStreakDialog by viewModel.showStreakDialog.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val appPin by viewModel.appPin.collectAsStateWithLifecycle()
    val isLocked = isAppLocked && !appPin.isNullOrBlank()
    val isPrivacyMode by viewModel.privacyModeEnabled.collectAsStateWithLifecycle()
    val isPrivacyRevealed by viewModel.privacyRevealOverride.collectAsStateWithLifecycle()
    val shouldHideBalance = isPrivacyMode && !isPrivacyRevealed

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    val todayExpense = remember(expenses) {
        expenses.filter { it.date in todayStart..todayEnd && it.type != "INCOME" && it.category != "Locked Savings" }
            .sumOf { it.amount }
    }

    var showQuickDepositDialog by remember { mutableStateOf<SavingsGoal?>(null) }
    var showQuickSplitDialog by remember { mutableStateOf(false) }
    var showQuickConvertDialog by remember { mutableStateOf(false) }
    var showAdjustBudgetDialog by remember { mutableStateOf(false) }

    val showAiConsentDialog by viewModel.showAiConsentDialog.collectAsStateWithLifecycle()
    if (showAiConsentDialog && !isLocked) {
        AiConsentDialog(
            onConsentAccepted = { viewModel.setAiConsent(true) },
            onConsentDeclined = { viewModel.setAiConsent(false) },
            onDismiss = { viewModel.dismissAiConsentDialog() }
        )
    }

    if (showStreakDialog && !isLocked) {
        DailyStreakCelebrationDialog(
            streakCount = currentStreak,
            onDismiss = { viewModel.dismissStreakDialog() }
        )
    }

    val (thisMonthRange, lastMonthRange) = remember {
        val cal = Calendar.getInstance()
        val cMonth = cal.get(Calendar.MONTH)
        val cYear = cal.get(Calendar.YEAR)

        val calStart = Calendar.getInstance().apply {
            set(cYear, cMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(cYear, cMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val lmCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val lmMonth = lmCal.get(Calendar.MONTH)
        val lmYear = lmCal.get(Calendar.YEAR)

        val lmStart = Calendar.getInstance().apply {
            set(lmYear, lmMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lmEnd = Calendar.getInstance().apply {
            set(lmYear, lmMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        (calStart.timeInMillis..calEnd.timeInMillis) to (lmStart.timeInMillis..lmEnd.timeInMillis)
    }

    // All-time income, expense, and savings goals totals for Total Net Balance
    val totalAllTimeIncome = remember(expenses, selectedCurrencyCode) { expenses.realIncome(selectedCurrencyCode) }
    val totalAllTimeExpense = remember(expenses, selectedCurrencyCode) { expenses.realExpense(selectedCurrencyCode) }
    val totalSavingsGoalsMoney = remember(savingsGoals) { savingsGoals.totalSavings() }
    val overallTotalNetBalance = remember(expenses, savingsGoals, selectedCurrencyCode) { expenses.netWorth(savingsGoals, selectedCurrencyCode) }

    // Filter current month expenses
    val thisMonthExpenses = remember(expenses, thisMonthRange) {
        expenses.filter {
            it.date in thisMonthRange && it.type != "INCOME" && it.category != "Locked Savings"
        }
    }
    val thisMonthTotal = remember(thisMonthExpenses, selectedCurrencyCode) {
        thisMonthExpenses.realExpense(selectedCurrencyCode)
    }

    // Filter current month incomes
    val thisMonthIncomes = remember(expenses, thisMonthRange) {
        expenses.filter {
            it.date in thisMonthRange && it.type == "INCOME" && it.category != "Goal Withdrawal"
        }
    }
    val thisMonthIncomeTotal = remember(thisMonthIncomes, selectedCurrencyCode) {
        thisMonthIncomes.realIncome(selectedCurrencyCode)
    }

    // Last month expenses
    val lastMonthExpenses = remember(expenses, lastMonthRange) {
        expenses.filter {
            it.date in lastMonthRange && it.type != "INCOME" && it.category != "Locked Savings"
        }
    }
    val lastMonthTotal = remember(lastMonthExpenses, selectedCurrencyCode) {
        lastMonthExpenses.realExpense(selectedCurrencyCode)
    }

    // Difference Calculation
    val diffPct = remember(thisMonthTotal, lastMonthTotal) {
        if (lastMonthTotal > 0) {
            ((thisMonthTotal - lastMonthTotal) / lastMonthTotal) * 100
        } else {
            0.0
        }
    }

    var showChangeNameDialog by remember { mutableStateOf(false) }
    var showBillsScreen by remember { mutableStateOf(false) }
    var showRemindersScreen by remember { mutableStateOf(false) }
    var showSavingGoalsScreen by remember { mutableStateOf(false) }
    var showAllShortcutsScreen by remember { mutableStateOf(false) }
    var showStartupReminder by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        showStartupReminder = false
    }

    if (showAllShortcutsScreen) {
        val todayStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
        val hasBillDueToday = viewModel.billsList.any { it.dueDate == todayStr }

        AllShortcutsTabScreen(
            onBack = { showAllShortcutsScreen = false },
            onAddExpense = {
                showAllShortcutsScreen = false
                onAddExpenseClick()
            },
            onAddIncome = {
                showAllShortcutsScreen = false
                onAddIncomeClick()
            },
            onQuickSplit = {
                showAllShortcutsScreen = false
                showQuickSplitDialog = true
            },
            onQuickConvert = {
                showAllShortcutsScreen = false
                showQuickConvertDialog = true
            },
            onViewGoals = {
                showAllShortcutsScreen = false
                showSavingGoalsScreen = true
            },
            onNavigateToExpenses = {
                showAllShortcutsScreen = false
                onNavigateToExpenses()
            },
            onNavigateToAnalytics = {
                showAllShortcutsScreen = false
                onNavigateToAnalytics()
            },
            onBillsClick = {
                showAllShortcutsScreen = false
                showBillsScreen = true
            },
            onReminderClick = {
                showAllShortcutsScreen = false
                showRemindersScreen = true
            },
            onAdjustBudget = {
                showAllShortcutsScreen = false
                showAdjustBudgetDialog = true
            },
            onOpenStreak = {
                showAllShortcutsScreen = false
                viewModel.triggerShowStreakDialog()
            },
            onOpenSettingsSubScreen = { subScreen ->
                showAllShortcutsScreen = false
                onOpenSettingsSubScreen(subScreen)
            },
            hasBillDueToday = hasBillDueToday,
            currentStreak = currentStreak,
            selectedLanguage = selectedLanguage
        )
        return
    }

    // Startup reminder alert dialog removed - shown on-screen in dashboard feed

    if (showSavingGoalsScreen) {
        SavingGoalsFullScreen(
            viewModel = viewModel,
            onBack = { showSavingGoalsScreen = false }
        )
        return
    }

    if (showBillsScreen) {
        BillsFullScreen(
            viewModel = viewModel,
            onBack = { showBillsScreen = false },
            onPayBill = { title: String, amt: Double ->
                viewModel.addExpense(
                    amount = amt,
                    category = "Bills",
                    date = System.currentTimeMillis(),
                    note = "Paid $title",
                    type = "EXPENSE",
                    currencyCode = selectedCurrencyCode
                )
            }
        )
        return
    }

    if (showRemindersScreen) {
        RemindersFullScreen(
            viewModel = viewModel,
            onBack = { showRemindersScreen = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Header & User Profile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                val initials = if (!userName.isNullOrBlank()) {
                    userName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(2)
                } else "U"

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SleekPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profileImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_default_profile_avatar),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column {
                    val timeOfDay = remember {
                        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                            in 0..11 -> "Good Morning,"
                            in 12..16 -> "Good Afternoon,"
                            else -> "Good Evening,"
                        }
                    }
                    Text(
                        text = timeOfDay,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    Text(
                        text = userName ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Streak Badge beside profile & Quick Add Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 🔥 Streak Flame Logo (replaces text pill as requested)
                StreakFlameLogo(
                    streakCount = currentStreak,
                    onClick = { viewModel.triggerShowStreakDialog() },
                    modifier = Modifier.testTag("dashboard_streak_flame_badge")
                )

                IconButton(
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .size(48.dp)
                        .tactileDepth(elevation = 5.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    SleekPrimary,
                                    mixPrimaryWithColor(SleekPrimary, Color.Black, 0.18f)
                                )
                            )
                        )
                        .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .testTag("dashboard_add_expense_fab")
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = "Add Expense",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Balance Card (Minimal, Calm, Flat Fintech Style)
        val animatedBalance = overallTotalNetBalance.toFloat()
        val animatedIncome = totalAllTimeIncome.toFloat()
        val animatedExpense = totalAllTimeExpense.toFloat()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dashboard_hero_balance_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.AccountBalance,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Net balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary
                        )
                        if (isPrivacyMode) {
                            IconButton(
                                onClick = { viewModel.togglePrivacyReveal() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (shouldHideBalance) AppIcons.VisibilityOff else AppIcons.Visibility,
                                    contentDescription = "Toggle privacy reveal",
                                    tint = SleekTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (overallTotalNetBalance >= 0) IncomeGreenBg else ExpenseRedBg
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (overallTotalNetBalance >= 0) "Surplus" else "Deficit",
                            color = if (overallTotalNetBalance >= 0) IncomeGreen else ExpenseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (shouldHideBalance) "$currencySymbol ••••••" else String.format(Locale.getDefault(), "%s%s%,.2f", if (animatedBalance >= 0) "" else "-", currencySymbol, Math.abs(animatedBalance)),
                    style = MaterialTheme.typography.displayMedium,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp
                )

                if (totalSavingsGoalsMoney > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (shouldHideBalance) "Includes $currencySymbol•••• in savings goals" else String.format(Locale.getDefault(), "Includes %s%,.0f in savings goals", currencySymbol, totalSavingsGoalsMoney),
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown row: Income vs Expense capsules
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income Capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IncomeGreenBg)
                            .border(BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.TrendingUp,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (shouldHideBalance) "$currencySymbol••••" else String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, animatedIncome),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Expense Capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ExpenseRedBg)
                            .border(BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.TrendingDown,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Expense",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (shouldHideBalance) "$currencySymbol••••" else String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, animatedExpense),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDecrease = thisMonthTotal <= lastMonthTotal
                    val pillBg = if (isDecrease) IncomeGreenBg else ExpenseRedBg
                    val pillTextColor = if (isDecrease) IncomeGreen else ExpenseRed
                    val prefixSign = if (isDecrease) "-" else "+"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pillBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%s%.1f%%", prefixSign, Math.abs(diffPct)),
                            color = pillTextColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "vs last month (%s%,.0f)", currencySymbol, lastMonthTotal),
                        color = SleekTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 💡 1. Daily Smart Financial Insight
        DailyFinancialInsightWidget(
            thisMonthIncome = thisMonthIncomeTotal,
            thisMonthExpense = thisMonthTotal,
            monthlyBudget = monthlyBudget,
            streakCount = currentStreak,
            currencySymbol = currencySymbol
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ⚡ 2. Quick Shortcuts Feed (Non-repeating 1-tap actions + "More" to open All Shortcuts Launcher)
        QuickServicesCategorySection(
            viewModel = viewModel,
            selectedLanguage = selectedLanguage,
            onAddExpense = onAddExpenseClick,
            onAddIncome = onAddIncomeClick,
            onQuickSplit = { showQuickSplitDialog = true },
            onQuickConvert = { showQuickConvertDialog = true },
            onMoreClick = { showAllShortcutsScreen = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🧾 4. Upcoming Bills Widget with inline "Pay Now"
        if (billShowUpcomingDashboard) {
            UpcomingBillsDashboardWidget(
                bills = viewModel.billsList,
                currencySymbol = currencySymbol,
                onPayBill = { bill ->
                    val totalBal = accounts.sumOf { it.balance }
                    if (bill.amount > totalBal && totalBal > 0) {
                        Toast.makeText(context, "Cannot pay: amount exceeds total balance!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addExpense(
                            amount = bill.amount,
                            category = "Bills",
                            date = System.currentTimeMillis(),
                            note = "Paid ${bill.title}",
                            type = "EXPENSE",
                            currencyCode = selectedCurrencyCode
                        )
                        viewModel.billsList.remove(bill)
                        Toast.makeText(context, "Paid ${bill.title} successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                onViewAllBills = { showBillsScreen = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6. Smart Budget Health Gauge Ring
        if (monthlyBudget > 0) {
            SmartBudgetHealthGaugeWidget(
                monthlyBudget = monthlyBudget,
                currentMonthExpense = thisMonthTotal,
                currencySymbol = currencySymbol,
                onUpdateBudget = { showAdjustBudgetDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            MonthlyBudgetSnapshotCard(
                monthlyBudget = monthlyBudget,
                currentMonthExpense = thisMonthTotal,
                currencySymbol = currencySymbol,
                onUpdateBudget = onUpdateBudget
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 🌟 Dynamic Financial Health Score & Diagnostics Widget
        val overdueBillsCount = remember(viewModel.billsList.toList()) {
            val nowTime = System.currentTimeMillis()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            viewModel.billsList.count { bill ->
                try {
                    val parsed = sdf.parse(bill.dueDate)
                    parsed != null && parsed.time < nowTime
                } catch (e: Exception) {
                    false
                }
            }
        }
        FinancialHealthScoreWidget(
            incomeThisMonth = thisMonthIncomeTotal,
            expenseThisMonth = thisMonthTotal,
            monthlyBudget = monthlyBudget,
            overdueBillsCount = overdueBillsCount,
            currencySymbol = currencySymbol,
            onExploreBudget = { showAdjustBudgetDialog = true }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🎯 7. Interactive Savings Goal Mini-Carousel
        if (savingsGoals.isNotEmpty()) {
            SavingsGoalsMiniCarouselWidget(
                goals = savingsGoals,
                currencySymbol = currencySymbol,
                onQuickDeposit = { goal, amount ->
                    viewModel.quickDepositToGoal(goal, amount)
                    Toast.makeText(context, "Deposited \$%.0f to \${goal.name}!".format(amount), Toast.LENGTH_SHORT).show()
                },
                onViewAllGoals = { showSavingGoalsScreen = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 📊 8. Monthly Top Spending Categories Widget
        if (thisMonthExpenses.isNotEmpty()) {
            TopSpendingCategoriesWidget(
                expensesThisMonth = thisMonthExpenses,
                categoryIcons = categoryIcons,
                currencySymbol = currencySymbol,
                onNavigateToAnalytics = onNavigateToAnalytics
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent Activity List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent activity",
                style = MaterialTheme.typography.titleMedium,
                color = SleekTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "See all",
                color = SleekPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onNavigateToExpenses() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activities recorded yet. Tap + to add.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekTextSecondary
                )
            }
        } else {
            // Show top 3 recent items
            expenses.take(3).forEach { expense ->
                RecentExpenseRow(
                    expense = expense,
                    categoryIcons = categoryIcons,
                    currencySymbol = currencySymbol,
                    onClick = { onEditExpenseClick(expense) }
                )
            }
        }
        Spacer(modifier = Modifier.height(90.dp))
    }

    val targetGoal = showQuickDepositDialog
    if (targetGoal != null) {
        QuickGoalDepositDialog(
            goal = targetGoal,
            currencySymbol = currencySymbol,
            onDismiss = { showQuickDepositDialog = null },
            onConfirmDeposit = { amt ->
                viewModel.quickDepositToGoal(targetGoal, amt)
                showQuickDepositDialog = null
                Toast.makeText(context, "Deposited to ${targetGoal.name} successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showQuickSplitDialog) {
        QuickSplitBillDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showQuickSplitDialog = false },
            onRecordExpense = { amt, note ->
                viewModel.addExpense(
                    amount = amt,
                    category = "Food & Dining",
                    date = System.currentTimeMillis(),
                    note = note,
                    type = "EXPENSE",
                    currencyCode = selectedCurrencyCode
                )
                Toast.makeText(context, "Recorded split expense of $currencySymbol${String.format(Locale.getDefault(), "%,.2f", amt)}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showQuickConvertDialog) {
        QuickCurrencyConvertDialog(
            currencyCode = selectedCurrencyCode,
            onDismiss = { showQuickConvertDialog = false },
            onOpenFullSettings = {
                showQuickConvertDialog = false
                onOpenSettingsSubScreen(SettingsSubScreen.Currency)
            }
        )
    }

    if (showAdjustBudgetDialog) {
        var budgetInput by remember { mutableStateOf(if (monthlyBudget > 0) monthlyBudget.toInt().toString() else "") }
        Dialog(onDismissRequest = { showAdjustBudgetDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Set Monthly Budget Target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "Your daily spending allowance will automatically adapt based on the days remaining in the month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Budget Target ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAdjustBudgetDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = SleekTextSecondary)
                        }
                        Button(
                            onClick = {
                                val amt = budgetInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    onUpdateBudget(amt)
                                    showAdjustBudgetDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showChangeNameDialog) {
        Dialog(onDismissRequest = { showChangeNameDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                var newName by remember { mutableStateOf(userName ?: "") }
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Update Your Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Your Name", color = SleekTextSecondary) },
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangeNameDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (newName.trim().isNotEmpty()) {
                                    onUpdateName(newName.trim())
                                    showChangeNameDialog = false
                                }
                            },
                            enabled = newName.trim().isNotEmpty(),
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
        Spacer(modifier = Modifier.height(110.dp))
    }
}

// ==========================================
// 🎨 QUICK SHORTCUTS & CATEGORY FEED
// ==========================================
@Composable
fun QuickServicesCategorySection(
    viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    selectedLanguage: String = "English",
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onQuickSplit: () -> Unit,
    onQuickConvert: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_services_category_feed")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Bolt,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = LanguageManager.tr("Shortcuts", selectedLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onMoreClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = LanguageManager.tr("All Tools", selectedLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryFeedTile(
                    icon = AppIcons.TrendingDown,
                    label = LanguageManager.tr("Expense", selectedLanguage),
                    tileColor = ExpenseRed,
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.TrendingUp,
                    label = LanguageManager.tr("Income", selectedLanguage),
                    tileColor = IncomeGreen,
                    onClick = onAddIncome,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.CallSplit,
                    label = LanguageManager.tr("Split", selectedLanguage),
                    tileColor = Color(0xFF6366F1),
                    onClick = onQuickSplit,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.CurrencyExchange,
                    label = LanguageManager.tr("Convert", selectedLanguage),
                    tileColor = Color(0xFF0EA5E9),
                    onClick = onQuickConvert,
                    modifier = Modifier.weight(1f)
                )

                CategoryFeedTile(
                    icon = AppIcons.GridView,
                    label = LanguageManager.tr("More", selectedLanguage),
                    tileColor = SleekPrimary,
                    onClick = onMoreClick,
                    modifier = Modifier.weight(1f),
                    hasBadge = false
                )
            }
        }
    }
}

@Composable
fun CategoryFeedTile(
    icon: ImageVector,
    label: String,
    tileColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tile_scale"
    )

    val isDark = isDarkModeActive
    val bgBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                tileColor.copy(alpha = 0.38f),
                tileColor.copy(alpha = 0.16f),
                Color(0xFF0F172A)
            ),
            start = Offset(0f, 0f),
            end = Offset(120f, 120f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White,
                tileColor.copy(alpha = 0.18f),
                tileColor.copy(alpha = 0.30f)
            ),
            start = Offset(0f, 0f),
            end = Offset(120f, 120f)
        )
    }
    val rimBorder = if (isDark) tileColor.copy(alpha = 0.65f) else tileColor.copy(alpha = 0.40f)
    val iconTint = if (isDark) tileColor.copy(alpha = 0.99f) else tileColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(elevation = if (isDark) 4.dp else 2.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(bgBrush)
                .border(BorderStroke(1.5.dp, rimBorder), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = SleekTextPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MonthlyBudgetSnapshotCard(
    monthlyBudget: Double,
    currentMonthExpense: Double,
    currencySymbol: String = "₹",
    onUpdateBudget: (Double) -> Unit
) {
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SleekBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_monthly_budget_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SleekPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.PieChart,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Monthly budget",
                            style = MaterialTheme.typography.titleMedium,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (monthlyBudget > 0) "Current month burn rate" else "No target configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                TextButton(
                    onClick = { showEditBudgetDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (monthlyBudget > 0) "Adjust" else "Set target",
                        color = SleekPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            if (monthlyBudget > 0) {
                val spentPct = ((currentMonthExpense / monthlyBudget) * 100).coerceAtLeast(0.0)
                val remaining = monthlyBudget - currentMonthExpense
                val isExceeded = remaining < 0
                val progressFraction = (currentMonthExpense / monthlyBudget).toFloat().coerceIn(0f, 1f)

                val statusColor = when {
                    isExceeded -> ExpenseRed
                    spentPct >= 80 -> WarningOrange
                    else -> IncomeGreen
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Spent this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, currentMonthExpense),
                            style = MaterialTheme.typography.titleLarge,
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isExceeded) "Exceeded by ${String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, Math.abs(remaining))}"
                                else String.format(Locale.getDefault(), "%.0f%% used", spentPct),
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!isExceeded) String.format(Locale.getDefault(), "%s%,.0f remaining", currencySymbol, remaining) else "Limit: ${String.format(Locale.getDefault(), "%s%,.0f", currencySymbol, monthlyBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Clean flat progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SleekBorder.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SleekBorder.copy(alpha = 0.25f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = SleekTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set a monthly target to visualize burn rate and track spending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showEditBudgetDialog) {
        var inputBudget by remember { mutableStateOf(if (monthlyBudget > 0) String.format(Locale.getDefault(), "%.0f", monthlyBudget) else "") }
        Dialog(onDismissRequest = { showEditBudgetDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Monthly Budget",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inputBudget,
                        onValueChange = { inputBudget = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Budget Cap ($currencySymbol)", color = SleekTextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditBudgetDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = SleekTextSecondary)
                        }
                        Button(
                            onClick = {
                                val value = inputBudget.toDoubleOrNull() ?: 0.0
                                onUpdateBudget(value)
                                showEditBudgetDialog = false
                            },
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
}

@Composable
fun RecentExpenseRow(
    expense: Expense,
    categoryIcons: Map<String, String> = emptyMap(),
    currencySymbol: String = "₹",
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date))
    val isIncome = expense.type == "INCOME"
    val catColor = if (isIncome) {
        when (expense.category) {
            "Salary" -> Color(0xFF10B981)
            "Freelance" -> Color(0xFF0D9488)
            "Investments" -> Color(0xFF3B82F6)
            "Gifts" -> Color(0xFFEC4899)
            else -> Color(0xFF10B981)
        }
    } else {
        categoryColors[expense.category] ?: SleekPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catColor.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, catColor.copy(alpha = 0.25f)), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(expense.category, categoryIcons),
                    contentDescription = expense.category,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.note ?: (if (isIncome) "Income" else "Expense"),
                    style = MaterialTheme.typography.titleSmall,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${expense.category} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }

            Text(
                text = String.format("%s%s%,.2f", if (isIncome) "+" else "-", currencySymbol, expense.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isIncome) Color(0xFF10B981) else ExpenseRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// ExpensesTab moved to ExpensesTabScreen.kt
// Image1TransactionRow and DateRangePickerDialog moved to ExpensesTabScreen.kt
// AnalyticsStatCard moved to AnalyticsTabScreen.kt
// AnalyticsTab moved to AnalyticsTabScreen.kt

// AnalyticsPeriodTransactionsCard moved to AnalyticsTabScreen.kt

// NetWorthOverTimeChartCard and GraphPoint moved to AnalyticsTabScreen.kt

// ==========================================
// 4️⃣.5️⃣ CATEGORY SELECTOR COMPONENTS
// ==========================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CategorySelectorGrid(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categories: List<String>,
    categoryIcons: Map<String, String> = emptyMap(),
    onAddCustomCategoryClick: () -> Unit,
    onDeleteCustomCategory: ((String) -> Unit)? = null,
    onEditCustomCategory: ((String, String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            val emoji = getCategoryEmoji(cat, categoryIcons)
            
            val isDefault = listOf("Food", "Travel", "Rent", "Utilities", "Entertainment", "Shopping", "Persons", "Others", "Salary", "Freelance", "Investments", "Gifts").contains(cat)
            
            var showEditDeleteDialog by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCategorySelected(cat)
                        },
                        onLongClick = {
                            if (!isDefault && (onDeleteCustomCategory != null || onEditCustomCategory != null)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEditDeleteDialog = true
                            }
                        }
                    ),
                shape = CircleShape,
                color = if (isSelected) SleekPrimary.copy(alpha = 0.2f) else SleekNeutralLight,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) SleekPrimary else SleekBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) SleekPrimary else SleekTextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    
                    if (!isDefault && isSelected) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "Edit Category",
                            tint = SleekTextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { showEditDeleteDialog = true }
                        )
                    }
                }
            }
            
            if (showEditDeleteDialog) {
                ManageCategoryDialog(
                    categoryName = cat,
                    onDismiss = { showEditDeleteDialog = false },
                    onDelete = {
                        if (onDeleteCustomCategory != null) {
                            onDeleteCustomCategory(cat)
                        }
                        showEditDeleteDialog = false
                    },
                    onRename = { newName ->
                        if (onEditCustomCategory != null) {
                            onEditCustomCategory(cat, newName)
                        }
                        showEditDeleteDialog = false
                    }
                )
            }
        }
        
        if (onAddCustomCategoryClick != null) {
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAddCustomCategoryClick() },
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = "Add Custom Category",
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Add Custom",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SleekPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ManageCategoryDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(categoryName) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showConfirmDelete) {
                    Text(
                        text = "Delete Category?",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Are you sure you want to delete \"$categoryName\"? Associated expenses will revert to \"Others\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SleekTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showConfirmDelete = false },
                            border = BorderStroke(1.dp, SleekBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextSecondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                } else {
                    Text(
                        text = "Manage Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Category Name", color = SleekTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedContainerColor = SleekSurface,
                            unfocusedContainerColor = SleekSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showConfirmDelete = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (newName.trim().isNotEmpty() && newName.trim() != categoryName) {
                                    onRename(newName.trim())
                                }
                                onDismiss()
                            },
                            enabled = newName.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rename", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SleekTextSecondary)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5️⃣ ADD EXPENSE DIALOG
// Defined in AddTransactionFlowComponents.kt
// ==========================================
@Composable
private fun AddExpenseDialogOld(
    prefilledDate: Long?,
    categories: List<String>,
    categoryIcons: Map<String, String> = emptyMap(),
    expenses: List<Expense> = emptyList(),
    currencyCode: String = "INR",
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onEditCategory: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, date: Long, note: String, imagePath: String?, type: String) -> Unit
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var amountStr by remember { mutableStateOf("") }

    val totalIncome = remember(expenses, currencyCode) {
        expenses.realIncome(currencyCode)
    }
    val totalExpenses = remember(expenses, currencyCode) {
        expenses.realExpense(currencyCode)
    }
    val availableBalance = (totalIncome - totalExpenses).coerceAtLeast(0.0)

    val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    val isExceedingIncome = type == "EXPENSE" && (enteredAmount > availableBalance || totalExpenses + enteredAmount > totalIncome)
    
    val incomeCategories = listOf("Salary", "Freelance", "Investments", "Gifts", "Others")
    val currentCategoriesList = if (type == "INCOME") {
        incomeCategories
    } else {
        categories
    }
    
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "Food") }
    var note by remember { mutableStateOf("") }

    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    // Automatically set default category when switching type
    LaunchedEffect(type) {
        category = if (type == "INCOME") "Salary" else (categories.firstOrNull() ?: "Food")
    }

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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().testTag("add_expense_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (type == "INCOME") "Add New Income" else "Add New Expense",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Segmented Switcher (Expense / Income)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekBorder.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (type == "EXPENSE") SleekPrimary else Color.Transparent)
                            .clickable { type = "EXPENSE" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            color = if (type == "EXPENSE") Color.White else SleekTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (type == "INCOME") Color(0xFF10B981) else Color.Transparent)
                            .clickable { type = "INCOME" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            color = if (type == "INCOME") Color.White else SleekTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prefilled Date Status Info
                if (prefilledDate != null) {
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(prefilledDate))
                    Text(
                        text = "Adding for: $dateFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SleekPrimary.copy(alpha = 0.1f))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Amount Input Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)", color = SleekTextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == "INCOME") Color(0xFF10B981) else SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface,
                        focusedLabelColor = if (type == "INCOME") Color(0xFF10B981) else SleekPrimary,
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                if (type == "EXPENSE") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Available Balance: ₹${String.format(Locale.getDefault(), "%,.2f", availableBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isExceedingIncome) ExpenseRed else SleekTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (totalIncome > 0) {
                            Text(
                                text = "Total Income: ₹${String.format(Locale.getDefault(), "%,.2f", totalIncome)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary
                            )
                        }
                    }
                    if (isExceedingIncome) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = AppIcons.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (totalIncome <= 0) 
                                        "Expense cannot exceed total income. Income is ₹0.00." 
                                    else 
                                        "Expense (₹${String.format(Locale.getDefault(), "%,.2f", enteredAmount)}) exceeds available bank balance (₹${String.format(Locale.getDefault(), "%,.2f", availableBalance)}).",
                                    color = ExpenseRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Total Income so far: ₹${String.format(Locale.getDefault(), "%,.2f", totalIncome)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekTextSecondary,
                    fontWeight = FontWeight.Bold
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

                Spacer(modifier = Modifier.height(14.dp))

                // Note description input (Mandatory)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description *", color = SleekTextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == "INCOME") Color(0xFF10B981) else SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface,
                        focusedLabelColor = if (type == "INCOME") Color(0xFF10B981) else SleekPrimary,
                        unfocusedLabelColor = SleekTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input")
                )
                if (note.trim().isEmpty()) {
                    Text(
                        "Description is required",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Attached image section in Add Dialog
                Text(
                    text = "Receipt Photo (Optional)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                val photoPath = attachedImagePath
                if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = File(photoPath),
                            contentDescription = "Attached Receipt",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { attachedImagePath = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(AppIcons.Close, contentDescription = "Clear Image", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val suppressAutoLock = LocalAutoLockSuppressor.current
                        Button(
                            onClick = {
                                suppressAutoLock()
                                cameraLauncher.launch()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(AppIcons.PhotoCamera, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera", color = SleekPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                suppressAutoLock()
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(AppIcons.Photo, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery", color = SleekPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && note.trim().isNotEmpty()) {
                                onConfirm(
                                    amount,
                                    category,
                                    prefilledDate ?: System.currentTimeMillis(),
                                    note.trim(),
                                    attachedImagePath,
                                    type
                                )
                            }
                        },
                        enabled = note.trim().isNotEmpty() && enteredAmount > 0.0 && !isExceedingIncome,
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "INCOME") Color(0xFF10B981) else SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val bmpToEdit = editingBitmap
    if (showCropperDialog && bmpToEdit != null) {
        ImageEditDialog(
            initialBitmap = bmpToEdit,
            onDismiss = { showCropperDialog = false },
            onSave = { savedPath ->
                showCropperDialog = false
                attachedImagePath = savedPath
            }
        )
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onConfirm = { newCat ->
                onAddCategory(newCat)
                category = newCat
                showCreateCategoryDialog = false
            }
        )
    }
}

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

// ==========================================
// 7️⃣ LIVE CALENDAR TAB
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarTab(
    expenses: List<Expense>,
    categoryIcons: Map<String, String> = emptyMap(),
    currencyCode: String = "INR",
    onAddExpenseForDate: (Long) -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val today = Calendar.getInstance()
    
    val fontScale = androidx.compose.ui.platform.LocalConfiguration.current.fontScale
    val calendarAspectRatio = if (fontScale > 1.3f) 0.7f else if (fontScale > 1.1f) 0.85f else 1f

    // Baseline: March 2027. +1 month on each 7th of the month.
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

    val initialYear = 2000
    val currentMonthIndex = (today.get(Calendar.YEAR) - initialYear) * 12 + today.get(Calendar.MONTH)
    val maxMonthIndex = (maxCalendarLimit.get(Calendar.YEAR) - initialYear) * 12 + maxCalendarLimit.get(Calendar.MONTH)

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = currentMonthIndex,
        pageCount = { maxMonthIndex + 1 }
    )

    val activeYear = initialYear + pagerState.currentPage / 12
    val activeMonth = pagerState.currentPage % 12
    val canGoForward = pagerState.currentPage < maxMonthIndex
    
    val coroutineScope = rememberCoroutineScope()

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    var selectedDayOfMonth by remember { mutableStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    val selectedDateMillis = remember(activeYear, activeMonth, selectedDayOfMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, activeYear)
            set(Calendar.MONTH, activeMonth)
            set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val selectedDayExpenses = expenses.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.get(Calendar.YEAR) == activeYear &&
                cal.get(Calendar.MONTH) == activeMonth &&
                cal.get(Calendar.DAY_OF_MONTH) == selectedDayOfMonth
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transaction Calendar",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Double-tap day to add. Valid until ${SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(maxCalendarLimit.time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    val currentDisplayCal = remember(activeYear, activeMonth) {
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, activeYear)
                            set(Calendar.MONTH, activeMonth)
                        }
                    }
                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentDisplayCal.time),
                        style = MaterialTheme.typography.titleMedium,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
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
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val pageYear = initialYear + page / 12
                    val pageMonth = page % 12
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SleekTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val firstDayCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, pageYear)
                            set(Calendar.MONTH, pageMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
                        val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val dayOffset = firstDayOfWeek - 1

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0 until 6) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (col in 0 until 7) {
                                        val slotIndex = row * 7 + col
                                        val dayNum = slotIndex - dayOffset + 1
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
                                            val dayIncome = dayExpenses.realIncome(currencyCode)
                                            val dayExpense = dayExpenses.realExpense(currencyCode)
                                            val hasTransactions = dayExpenses.isNotEmpty()
                                            val isProfit = hasTransactions && dayIncome >= dayExpense
                                            val isLoss = hasTransactions && dayExpense > dayIncome

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(calendarAspectRatio)
                                                    .padding(2.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> SleekPrimary
                                                            isToday -> SleekPrimaryContainer.copy(alpha = 0.5f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = when {
                                                            isSelected -> Color.Transparent
                                                            isToday -> SleekPrimary
                                                            else -> Color.Transparent
                                                        },
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .combinedClickable(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            selectedDayOfMonth = dayNum
                                                            if (page != pagerState.currentPage) {
                                                                coroutineScope.launch {
                                                                    pagerState.animateScrollToPage(page)
                                                                }
                                                            }
                                                        },
                                                        onDoubleClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            selectedDayOfMonth = dayNum
                                                            val clickedDate = Calendar.getInstance().apply {
                                                                set(Calendar.YEAR, pageYear)
                                                                set(Calendar.MONTH, pageMonth)
                                                                set(Calendar.DAY_OF_MONTH, dayNum)
                                                            }.timeInMillis
                                                            onAddExpenseForDate(clickedDate)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isToday -> SleekPrimary
                                                            else -> SleekTextPrimary
                                                        }
                                                    )
                                                    if (hasTransactions) {
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(top = 2.dp)
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    when {
                                                                        isSelected -> Color.White
                                                                        isProfit -> Color(0xFF10B981)
                                                                        else -> Color(0xFFEF4444)
                                                                    }
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.weight(1f).aspectRatio(calendarAspectRatio))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selectedDateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
            Text(
                text = "Transactions on $selectedDateStr",
                style = MaterialTheme.typography.titleMedium,
                color = SleekTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { onAddExpenseForDate(selectedDateMillis) },
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(AppIcons.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedDayExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions logged for this day.",
                    color = SleekTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedDayExpenses.forEach { expense ->
                    val isIncome = expense.type == "INCOME"
                    val catColor = if (isIncome) {
                        when (expense.category) {
                            "Salary" -> Color(0xFF10B981)
                            "Freelance" -> Color(0xFF0D9488)
                            "Investments" -> Color(0xFF3B82F6)
                            "Gifts" -> Color(0xFFEC4899)
                            else -> Color(0xFF10B981)
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
                            .clickable { onEditExpense(expense) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getCategoryEmoji(expense.category, categoryIcons),
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.note ?: (if (isIncome) "Income" else "Expense"),
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

                        Text(
                            text = String.format("%s₹%,.2f", if (isIncome) "+" else "-", expense.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isIncome) Color(0xFF10B981) else ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = { onDeleteExpense(expense) },
                            modifier = Modifier.size(24.dp)
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
        Spacer(modifier = Modifier.height(110.dp))
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

                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
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
// SidebarSettingsTile, SidebarMenuItemData, and SidebarGroupCard moved to SidebarDrawerContent.kt

// SidebarDrawerContent moved to SidebarDrawerContent.kt


@Composable
fun ThemeCirclePreview(index: Int, customHue: Float, modifier: Modifier = Modifier) {
    val (primary, primaryContainer, onPrimaryContainer) = com.example.ui.theme.getPresetThemeColors(index, customHue)
    Canvas(modifier = modifier) {
        // Top half
        drawArc(
            color = primary,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true
        )
        // Bottom-left quadrant
        drawArc(
            color = primaryContainer,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true
        )
        // Bottom-right quadrant
        drawArc(
            color = onPrimaryContainer,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true
        )
    }
}

@Composable
fun ColorThemeGrid(
    selectedThemeIndex: Int,
    customHue: Float,
    onThemeSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 4) {
                    val index = row * 4 + col
                    val isSelected = selectedThemeIndex == index
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SleekPrimary.copy(alpha = 0.12f) else SleekBorder.copy(alpha = 0.3f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) SleekPrimary else SleekBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onThemeSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 15) {
                            // Eyedropper custom picker
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SleekPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Colorize,
                                    contentDescription = "Custom Theme",
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Standard preset theme circle
                            ThemeCirclePreview(
                                index = index,
                                customHue = customHue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        // Checkmark Overlay
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(SleekPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaqAccordion(viewModel: FinanceViewModel) {
    val faqs = listOf(
        "Is my financial data secure?" to "Yes, all data is stored offline locally on your device and never uploaded to any servers.",
        "How do I set a monthly budget?" to "Click the pencil icon on the monthly card on the Dashboard to set your budget limit.",
        "What are custom categories?" to "Select '+ Add Custom' in the Category dropdown when adding/editing an expense to add new categories.",
        "How do I delete or edit transactions?" to "On the Transactions tab, long press or tap on any expense row to select it, then use the floating actions bar to edit or delete."
    )
    
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        faqs.forEachIndexed { index, (question, answer) ->
            val isExpanded = expandedIndex == index
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedIndex = if (isExpanded) null else index }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // 🧠 AI BASED ASK
        var aiQuestion by remember { mutableStateOf("") }
        val aiAnswer by viewModel.aiCardAnswer.collectAsStateWithLifecycle()
        val isQuerying by viewModel.isAiCardLoading.collectAsStateWithLifecycle()
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekPrimaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = AppIcons.AutoAwesome,
                        contentDescription = "AI",
                        tint = SleekPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Ask AI Financial Guide",
                        style = MaterialTheme.typography.titleSmall,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Have a question about your personal budget, tax, or saving tips? Ask our AI Financial Guide for practical insights grounded in your aggregated financial totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = aiQuestion,
                    onValueChange = { 
                        if (it.length <= 1000) {
                            aiQuestion = it 
                        }
                    },
                    placeholder = { Text("How can I start investing ₹2000/month?", fontSize = 13.sp, color = SleekTextSecondary) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedContainerColor = SleekSurface,
                        unfocusedContainerColor = SleekSurface,
                        focusedTextColor = SleekTextPrimary,
                        unfocusedTextColor = SleekTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🔒 AI queries are processed online via Google Gemini. For your security, never enter account numbers, passwords, or personal credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (aiQuestion.isNotBlank()) {
                            viewModel.askAiCardQuestion(aiQuestion)
                        }
                    },
                    enabled = !isQuerying && aiQuestion.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isQuerying) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Ask AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                val answer = aiAnswer
                if (answer != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextPrimary,
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Version Footer
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expense Tracker",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "100% Offline • Secure Room DB",
                        fontSize = 10.sp,
                        color = SleekTextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SleekPrimary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "v1.29",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SleekPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
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
                            val accentColor = if (isIncome) Color(0xFF10B981) else Color(0xFFEF4444)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isIncome) "INCOME RECORDED" else "EXPENSE RECORDED",
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
