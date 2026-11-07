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
import androidx.compose.ui.platform.LocalContext
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
import java.io.File
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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

fun getCategoryColor(category: String): Color {
    val clean = category.trim().lowercase(java.util.Locale.getDefault())
    return when {
        clean.contains("food") || clean.contains("dining") || clean.contains("restaurant") || clean.contains("grocer") || clean.contains("cafe") || clean.contains("coffee") || clean.contains("fastfood") || clean.contains("snack") || clean.contains("baking") -> Color(0xFFF97316)
        clean.contains("travel") || clean.contains("transport") || clean.contains("car") || clean.contains("fuel") || clean.contains("gas") || clean.contains("flight") || clean.contains("taxi") || clean.contains("bus") -> Color(0xFF0D9488)
        clean.contains("rent") || clean.contains("home") || clean.contains("house") || clean.contains("housing") || clean.contains("mortgage") || clean.contains("flat") || clean.contains("apartment") -> Color(0xFF2563EB)
        clean.contains("util") || clean.contains("bill") || clean.contains("electric") || clean.contains("power") || clean.contains("water") || clean.contains("bolt") || clean.contains("wifi") || clean.contains("internet") || clean.contains("phone") || clean.contains("mobile") -> Color(0xFF16A34A)
        clean.contains("entertain") || clean.contains("movie") || clean.contains("cinema") || clean.contains("game") || clean.contains("gaming") || clean.contains("music") || clean.contains("netflix") || clean.contains("streaming") -> Color(0xFFE11D48)
        clean.contains("shop") || clean.contains("cloth") || clean.contains("fashion") || clean.contains("store") || clean.contains("shoe") || clean.contains("retail") || clean.contains("mall") -> Color(0xFF9333EA)
        clean.contains("health") || clean.contains("medic") || clean.contains("doctor") || clean.contains("hospital") || clean.contains("pharmacy") || clean.contains("clinic") || clean.contains("dental") || clean.contains("dentist") -> Color(0xFFDC2626)
        clean.contains("fit") || clean.contains("gym") || clean.contains("sport") || clean.contains("workout") || clean.contains("exercise") || clean.contains("yoga") -> Color(0xFF059669)
        clean.contains("educat") || clean.contains("school") || clean.contains("college") || clean.contains("university") || clean.contains("tuition") || clean.contains("course") || clean.contains("book") || clean.contains("study") -> Color(0xFF4F46E5)
        clean.contains("salary") || clean.contains("wage") || clean.contains("income") || clean.contains("paycheck") || clean.contains("earnings") -> Color(0xFF10B981)
        clean.contains("freelance") || clean.contains("work") || clean.contains("business") || clean.contains("consult") || clean.contains("client") || clean.contains("gig") -> Color(0xFF0284C7)
        clean.contains("invest") || clean.contains("stock") || clean.contains("crypto") || clean.contains("bitcoin") || clean.contains("trading") || clean.contains("dividend") || clean.contains("share") -> Color(0xFF8B5CF6)
        clean.contains("saving") || clean.contains("save") || clean.contains("deposit") || clean.contains("vault") || clean.contains("piggy") || clean.contains("reserve") || clean.contains("emergency") -> Color(0xFFF59E0B)
        clean.contains("gift") || clean.contains("donat") || clean.contains("charit") || clean.contains("present") -> Color(0xFFEC4899)
        clean.contains("spa") || clean.contains("beauty") || clean.contains("salon") || clean.contains("barber") || clean.contains("haircut") || clean.contains("skincare") || clean.contains("personal") -> Color(0xFFD946EF)
        clean.contains("person") || clean.contains("family") || clean.contains("friend") || clean.contains("kid") || clean.contains("child") || clean.contains("baby") -> Color(0xFF0EA5E9)
        clean.contains("pet") || clean.contains("dog") || clean.contains("cat") || clean.contains("animal") || clean.contains("vet") -> Color(0xFFD97706)
        clean.contains("subscri") || clean.contains("recur") || clean.contains("membership") -> Color(0xFF6366F1)
        clean.contains("loan") || clean.contains("debt") || clean.contains("emi") || clean.contains("credit") || clean.contains("card") || clean.contains("repay") -> Color(0xFFEF4444)
        clean.contains("tax") || clean.contains("fee") || clean.contains("fine") || clean.contains("duty") || clean.contains("charge") || clean.contains("receipt") -> Color(0xFFB91C1C)
        clean.contains("tech") || clean.contains("comput") || clean.contains("laptop") || clean.contains("code") -> Color(0xFF0284C7)
        clean.contains("construct") || clean.contains("repair") || clean.contains("build") || clean.contains("tool") -> Color(0xFFCA8A04)
        clean.contains("event") || clean.contains("holiday") || clean.contains("celebrat") -> Color(0xFFEA580C)
        clean.contains("star") || clean.contains("badge") || clean.contains("milestone") -> Color(0xFFEAB308)
        else -> {
            val hash = kotlin.math.abs(category.hashCode())
            val palette = listOf(
                Color(0xFFF97316), Color(0xFF0D9488), Color(0xFF2563EB), Color(0xFF16A34A),
                Color(0xFFE11D48), Color(0xFF9333EA), Color(0xFF0EA5E9), Color(0xFFF59E0B),
                Color(0xFF10B981), Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF8B5CF6)
            )
            palette[hash % palette.size]
        }
    }
}

// Category palette helper backed by getCategoryColor
val categoryColors: Map<String, Color> = mapOf(
    "Food" to Color(0xFFF97316),
    "Travel" to Color(0xFF0D9488),
    "Rent" to Color(0xFF2563EB),
    "Utilities" to Color(0xFF16A34A),
    "Entertainment" to Color(0xFFE11D48),
    "Shopping" to Color(0xFF9333EA),
    "Persons" to Color(0xFF0EA5E9),
    "Others" to Color(0xFF64748B),
    "Salary" to Color(0xFF10B981),
    "Freelance" to Color(0xFF0284C7),
    "Investments" to Color(0xFF8B5CF6),
    "Gifts" to Color(0xFFEC4899),
    "Health" to Color(0xFFDC2626),
    "Healthcare" to Color(0xFFDC2626),
    "Medical" to Color(0xFFDC2626),
    "Education" to Color(0xFF4F46E5),
    "Groceries" to Color(0xFFF97316),
    "Transport" to Color(0xFF0D9488),
    "Bills" to Color(0xFF16A34A),
    "Savings" to Color(0xFFF59E0B),
    "Fitness" to Color(0xFF059669),
    "Personal Care" to Color(0xFFD946EF),
    "Subscriptions" to Color(0xFF6366F1),
    "Taxes" to Color(0xFFB91C1C),
    "Loan" to Color(0xFFEF4444)
)

fun getCategoryEmoji(category: String, customMap: Map<String, String> = emptyMap()): String {
    val clean = (customMap[category] ?: category).trim().lowercase(java.util.Locale.getDefault())
    return when {
        clean.contains("coffee") || clean.contains("cafe") -> "☕"
        clean.contains("fastfood") || clean.contains("burger") || clean.contains("pizza") -> "🍔"
        clean.contains("food") || clean.contains("dining") || clean.contains("restaurant") || clean.contains("grocer") || clean.contains("meal") -> "🍽️"
        clean.contains("flight") || clean.contains("plane") || clean.contains("aviation") -> "✈️"
        clean.contains("bus") -> "🚌"
        clean.contains("gas") || clean.contains("fuel") || clean.contains("petrol") -> "⛽"
        clean.contains("travel") || clean.contains("transport") || clean.contains("car") || clean.contains("taxi") -> "🚗"
        clean.contains("rent") || clean.contains("home") || clean.contains("house") || clean.contains("housing") || clean.contains("apartment") -> "🏠"
        clean.contains("water") -> "💧"
        clean.contains("util") || clean.contains("bill") || clean.contains("electric") || clean.contains("power") || clean.contains("bolt") -> "💡"
        clean.contains("game") || clean.contains("gaming") || clean.contains("esports") -> "🎮"
        clean.contains("music") || clean.contains("spotify") || clean.contains("song") -> "🎵"
        clean.contains("entertain") || clean.contains("movie") || clean.contains("cinema") || clean.contains("netflix") -> "🎬"
        clean.contains("cloth") || clean.contains("fashion") || clean.contains("dress") -> "👗"
        clean.contains("shop") || clean.contains("store") || clean.contains("mall") -> "🛍️"
        clean.contains("salary") || clean.contains("wage") || clean.contains("income") || clean.contains("paycheck") -> "💵"
        clean.contains("freelance") || clean.contains("work") || clean.contains("business") || clean.contains("job") -> "💼"
        clean.contains("invest") || clean.contains("stock") || clean.contains("shares") || clean.contains("crypto") -> "📈"
        clean.contains("saving") || clean.contains("save") || clean.contains("deposit") || clean.contains("vault") -> "🐷"
        clean.contains("gift") || clean.contains("present") || clean.contains("donat") || clean.contains("charit") -> "🎁"
        clean.contains("health") || clean.contains("medic") || clean.contains("doctor") || clean.contains("hospital") || clean.contains("pharmacy") -> "🏥"
        clean.contains("fit") || clean.contains("gym") || clean.contains("sport") || clean.contains("workout") -> "🏋️"
        clean.contains("educat") || clean.contains("school") || clean.contains("college") || clean.contains("university") || clean.contains("book") -> "🏫"
        clean.contains("spa") || clean.contains("beauty") || clean.contains("salon") || clean.contains("haircut") || clean.contains("personal") -> "🧖"
        clean.contains("person") || clean.contains("family") || clean.contains("friend") || clean.contains("people") -> "👥"
        clean.contains("pet") || clean.contains("dog") || clean.contains("cat") || clean.contains("animal") -> "🐾"
        clean.contains("subscri") || clean.contains("recur") -> "🔄"
        clean.contains("loan") || clean.contains("debt") || clean.contains("emi") || clean.contains("credit") || clean.contains("card") -> "💳"
        clean.contains("tax") || clean.contains("fine") || clean.contains("fee") || clean.contains("receipt") -> "🧾"
        clean.contains("tech") || clean.contains("comput") || clean.contains("laptop") || clean.contains("code") -> "💻"
        clean.contains("construct") || clean.contains("repair") || clean.contains("tool") -> "🔨"
        clean.contains("event") || clean.contains("holiday") || clean.contains("celebrat") -> "📅"
        clean.contains("star") || clean.contains("badge") || clean.contains("reward") -> "⭐️"
        clean.contains("pending") || clean.contains("wait") -> "⏳"
        else -> "📦"
    }
}

fun getCategoryIcon(category: String, customMap: Map<String, String> = emptyMap()): ImageVector {
    val raw = customMap[category] ?: category
    val clean = raw.trim().lowercase(java.util.Locale.getDefault())

    // 1. Direct icon identifier matching
    when (clean) {
        "restaurant", "food", "dining" -> return AppIcons.Restaurant
        "directionscar", "car", "travel" -> return AppIcons.DirectionsCar
        "home", "house", "rent" -> return AppIcons.Home
        "bolt", "electricbolt", "electricity", "utilities" -> return AppIcons.Bolt
        "movie", "entertainment" -> return AppIcons.Movie
        "shoppingcart", "shopping", "shoppingbag" -> return AppIcons.ShoppingCart
        "person", "persons" -> return AppIcons.Person
        "localhospital", "healing", "hospital", "health" -> return AppIcons.LocalHospital
        "school", "education" -> return AppIcons.School
        "work", "business" -> return AppIcons.Work
        "flight", "flighttakeoff", "airplane" -> return AppIcons.Flight
        "sportsesports", "gaming" -> return AppIcons.SportsEsports
        "cardgiftcard", "gift", "gifts" -> return AppIcons.CardGiftcard
        "monetizationon", "salary", "income" -> return AppIcons.MonetizationOn
        "settings" -> return AppIcons.Settings
        "pets", "pet" -> return AppIcons.Pets
        "star" -> return AppIcons.Star
        "construction" -> return AppIcons.Construction
        "fastfood" -> return AppIcons.Fastfood
        "coffee" -> return AppIcons.Coffee
        "waterdrop", "water" -> return AppIcons.WaterDrop
        "checkroom", "clothes" -> return AppIcons.Checkroom
        "directionsbus", "bus" -> return AppIcons.DirectionsBus
        "localgasstation", "fuel", "gas" -> return AppIcons.LocalGasStation
        "fitnesscenter", "gym", "fitness" -> return AppIcons.FitnessCenter
        "event", "calendar" -> return AppIcons.Event
        "spa", "beauty" -> return AppIcons.Spa
        "pending" -> return AppIcons.Pending
        "accountbalance", "bank" -> return AppIcons.AccountBalance
        "creditcard", "card", "loan" -> return AppIcons.CreditCard
        "savings" -> return AppIcons.Savings
        "piechart", "budget" -> return AppIcons.PieChart
        "receipt", "tax" -> return AppIcons.Receipt
        "laptop", "computer" -> return AppIcons.Laptop
        "beachaccess", "vacation", "holiday" -> return AppIcons.BeachAccess
        "shoppingbag" -> return AppIcons.ShoppingBag
        "payments", "cash" -> return AppIcons.Payments
        "security", "insurance", "shield" -> return AppIcons.Security
        "autograph", "growth", "stocks" -> return AppIcons.AutoGraph
        "category" -> return AppIcons.Category
    }

    // 2. Keyword heuristic matching
    return when {
        clean.contains("coffee") || clean.contains("cafe") -> AppIcons.Coffee
        clean.contains("fastfood") || clean.contains("burger") || clean.contains("pizza") -> AppIcons.Fastfood
        clean.contains("food") || clean.contains("dining") || clean.contains("restaurant") || clean.contains("grocer") || clean.contains("meal") || clean.contains("snack") || clean.contains("bak") -> AppIcons.Restaurant
        clean.contains("flight") || clean.contains("plane") || clean.contains("aviation") -> AppIcons.Flight
        clean.contains("bus") -> AppIcons.DirectionsBus
        clean.contains("gas") || clean.contains("fuel") || clean.contains("petrol") -> AppIcons.LocalGasStation
        clean.contains("travel") || clean.contains("transport") || clean.contains("car") || clean.contains("taxi") || clean.contains("commute") -> AppIcons.DirectionsCar
        clean.contains("rent") || clean.contains("home") || clean.contains("house") || clean.contains("housing") || clean.contains("apartment") || clean.contains("mortgage") || clean.contains("flat") -> AppIcons.Home
        clean.contains("water") -> AppIcons.WaterDrop
        clean.contains("util") || clean.contains("bill") || clean.contains("electric") || clean.contains("power") || clean.contains("bolt") || clean.contains("wifi") || clean.contains("internet") || clean.contains("phone") || clean.contains("mobile") -> AppIcons.Bolt
        clean.contains("game") || clean.contains("gaming") || clean.contains("esports") -> AppIcons.SportsEsports
        clean.contains("entertain") || clean.contains("movie") || clean.contains("cinema") || clean.contains("netflix") || clean.contains("theatre") || clean.contains("music") || clean.contains("streaming") -> AppIcons.Movie
        clean.contains("cloth") || clean.contains("fashion") || clean.contains("shoe") || clean.contains("wear") -> AppIcons.Checkroom
        clean.contains("shop") || clean.contains("store") || clean.contains("mall") || clean.contains("retail") -> AppIcons.ShoppingCart
        clean.contains("health") || clean.contains("medic") || clean.contains("doctor") || clean.contains("hospital") || clean.contains("pharmacy") || clean.contains("clinic") || clean.contains("dental") || clean.contains("care") -> AppIcons.LocalHospital
        clean.contains("fit") || clean.contains("gym") || clean.contains("sport") || clean.contains("workout") || clean.contains("exercise") || clean.contains("yoga") -> AppIcons.FitnessCenter
        clean.contains("educat") || clean.contains("school") || clean.contains("college") || clean.contains("university") || clean.contains("tuition") || clean.contains("course") || clean.contains("book") || clean.contains("study") -> AppIcons.School
        clean.contains("salary") || clean.contains("wage") || clean.contains("income") || clean.contains("paycheck") || clean.contains("earnings") || clean.contains("stipend") -> AppIcons.MonetizationOn
        clean.contains("freelance") || clean.contains("work") || clean.contains("business") || clean.contains("consult") || clean.contains("client") || clean.contains("gig") || clean.contains("job") -> AppIcons.Work
        clean.contains("invest") || clean.contains("stock") || clean.contains("crypto") || clean.contains("bitcoin") || clean.contains("trading") || clean.contains("share") || clean.contains("dividend") || clean.contains("fund") -> AppIcons.AutoGraph
        clean.contains("saving") || clean.contains("save") || clean.contains("deposit") || clean.contains("vault") || clean.contains("piggy") || clean.contains("reserve") || clean.contains("emergency") -> AppIcons.Savings
        clean.contains("gift") || clean.contains("present") || clean.contains("donat") || clean.contains("charit") -> AppIcons.CardGiftcard
        clean.contains("spa") || clean.contains("beauty") || clean.contains("salon") || clean.contains("barber") || clean.contains("haircut") || clean.contains("skincare") || clean.contains("personal") -> AppIcons.Spa
        clean.contains("person") || clean.contains("family") || clean.contains("friend") || clean.contains("kid") || clean.contains("child") || clean.contains("baby") || clean.contains("people") -> AppIcons.Person
        clean.contains("pet") || clean.contains("dog") || clean.contains("cat") || clean.contains("animal") || clean.contains("vet") -> AppIcons.Pets
        clean.contains("subscri") || clean.contains("recur") || clean.contains("membership") -> AppIcons.Autorenew
        clean.contains("loan") || clean.contains("debt") || clean.contains("emi") || clean.contains("credit") || clean.contains("card") || clean.contains("repay") -> AppIcons.CreditCard
        clean.contains("tax") || clean.contains("fine") || clean.contains("fee") || clean.contains("duty") || clean.contains("receipt") -> AppIcons.Receipt
        clean.contains("tech") || clean.contains("comput") || clean.contains("laptop") || clean.contains("code") -> AppIcons.Laptop
        clean.contains("construct") || clean.contains("repair") || clean.contains("build") || clean.contains("tool") -> AppIcons.Construction
        clean.contains("event") || clean.contains("holiday") || clean.contains("celebrat") -> AppIcons.Event
        clean.contains("star") || clean.contains("badge") || clean.contains("milestone") -> AppIcons.Star
        clean.contains("pending") || clean.contains("wait") -> AppIcons.Pending
        clean.contains("bank") || clean.contains("account") -> AppIcons.AccountBalance
        else -> AppIcons.Category
    }
}

val LocalAutoLockSuppressor = compositionLocalOf<() -> Unit> { {} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceAppScreen(viewModel: FinanceViewModel) {
    val animPref by viewModel.isAnimationEnabled.collectAsStateWithLifecycle()
    val animEnabled = animPref && systemAnimationsAllowed()
    val compactLayout by viewModel.isCompactLayout.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalAutoLockSuppressor provides { viewModel.suppressNextAutoLock() },
        LocalAnimationsEnabled provides animEnabled,
        // Haptics + compact layout are consumed through CompositionLocals so every tile in the
        // app honours them, not just the few screens that read the ViewModel directly.
        LocalHapticsEnabled provides viewModel.hapticsEnabled.collectAsStateWithLifecycle().value,
        LocalCompactLayout provides compactLayout
    ) {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
    val appContext = LocalContext.current
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
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val globalLoadingMessage by viewModel.appLoadingMessage.collectAsStateWithLifecycle()
    var isInitialAppLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(950)
        isInitialAppLoading = false
    }

    val appPin by viewModel.appPin.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val hasPromptedFirstRunPin by viewModel.hasPromptedFirstRunPin.collectAsStateWithLifecycle()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var customTxTypeForAddDialog by remember { mutableStateOf<String?>(null) }
    var prefilledDateForAddDialog by remember { mutableStateOf<Long?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var viewingDetailExpense by remember { mutableStateOf<Expense?>(null) }
    var recordedTransactionInfo by remember { mutableStateOf<RecordedTransactionInfo?>(null) }

    var activeSettingsSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }
    val confirmTxDelete by viewModel.confirmTxDelete.collectAsStateWithLifecycle()
    var expensePendingDelete by remember { mutableStateOf<Expense?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerFullscreen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()

    val pendingShortcut by viewModel.pendingShortcutAction.collectAsStateWithLifecycle()
    LaunchedEffect(pendingShortcut) {
        val action = pendingShortcut ?: return@LaunchedEffect
        when (action) {
            "add_expense" -> {
                customTxTypeForAddDialog = "EXPENSE"
                prefilledDateForAddDialog = null
                showAddExpenseDialog = true
            }
            "add_income" -> {
                customTxTypeForAddDialog = "INCOME"
                prefilledDateForAddDialog = null
                showAddExpenseDialog = true
            }
            "open_analytics" -> {
                currentScreen = Screen.Analytics
            }
            "open_calendar" -> {
                currentScreen = Screen.Calendar
            }
        }
        viewModel.clearPendingShortcutAction()
    }

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

    val isSecurityLockEnabled = !appPin.isNullOrBlank() || biometricEnabled
    val locked = isAppLocked && isSecurityLockEnabled

    // Close drawer automatically if app gets locked
    LaunchedEffect(isAppLocked, appPin, biometricEnabled) {
        if (locked) {
            drawerState.close()
        }
    }

    // Re-lock app according to Inactivity Auto-Lock duration when foregrounded
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appPin, biometricEnabled) {
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
    BackHandler(enabled = locked) {
        // Do nothing on back button during lock screen
    }

    // SYSTEM BACK BUTTON HANDLER (Pops navigation stack, closes drawer/settings/dialogs)
    val canHandleBack = (!locked) && (
            drawerFullscreen ||
            drawerState.isOpen ||
            activeSettingsSubScreen != null ||
            viewingDetailExpense != null ||
            editingExpense != null ||
            showAddExpenseDialog ||
            backStack.size > 1 ||
            currentScreen != Screen.Dashboard
    )

    // "Confirm before exiting" (Appearance > Financial display) — when on, and the user is already
    // at the dashboard root with nothing open, ask once before letting the system close the app.
    val confirmBeforeExit by viewModel.confirmBeforeExit.collectAsStateWithLifecycle()
    val hostActivity = LocalContext.current as? android.app.Activity
    var showExitConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = !locked && !canHandleBack && confirmBeforeExit) {
        showExitConfirm = true
    }
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Leave Financer?", fontWeight = FontWeight.Bold) },
            text = { Text("Your data is saved on this device, so nothing is lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    hostActivity?.finish()
                }) { Text("Exit", color = ExpenseRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Stay") }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

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
            if (!locked) {
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
                val autoSuggestCategory by viewModel.autoSuggestCategory.collectAsStateWithLifecycle()
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
                    autoSuggestCategory = autoSuggestCategory,
                    gstReserveAmount = if (isGstEnabledForDialog) viewModel.getGstReserveAmount() else 0.0,
                    monthlySafeAmount = if (isMonthlySafeEnabledForDialog) monthlySafeAmountForDialog else 0.0,
                    currencySymbol = currencySymbol,
                    currencyCode = selectedCurrencyCode,
                    accounts = accounts,
                    onAddCategory = { name, catType -> viewModel.addCustomCategory(name, catType) },
                    onDeleteCategory = { viewModel.deleteCustomCategory(it) },
                    onEditCategory = { old, new -> viewModel.renameCustomCategory(old, new) },
                    onDismiss = {
                        showAddExpenseDialog = false
                        customTxTypeForAddDialog = null
                    },
                    onConfirmTransfer = { fromAccId, toAccId, amount, note, date ->
                        viewModel.transferFunds(fromAccId, toAccId, amount, note, date)
                        viewModel.refreshUsageData()
                        showAddExpenseDialog = false
                        customTxTypeForAddDialog = null
                        val fromName = accounts.find { it.id == fromAccId }?.name ?: "Source"
                        val toName = accounts.find { it.id == toAccId }?.name ?: "Destination"
                        recordedTransactionInfo = RecordedTransactionInfo(
                            amount = amount,
                            category = "Transfer",
                            type = "TRANSFER",
                            note = "$fromName ➔ $toName${if (note.isNotBlank()) " • $note" else ""}"
                        )
                    },
                    onConfirm = { amount, category, date, note, imagePath, type ->
                        if (viewModel.soundFeedbackEnabled.value) {
                            try {
                                val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                                audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.7f)
                            } catch (_: Exception) {}
                        }
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
                            if (confirmTxDelete) {
                                expensePendingDelete = target
                            } else {
                                viewModel.deleteExpense(target)
                            }
                        }
                    }
                )
            }

            val pendingDel = expensePendingDelete
            if (pendingDel != null) {
                AlertDialog(
                    onDismissRequest = { expensePendingDelete = null },
                    title = {
                        Text("Delete Transaction?", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to delete this ${pendingDel.category} entry of $currencySymbol%,.2f? You can also undo this action within 5 seconds.".format(pendingDel.amount),
                            color = SleekTextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val toDelete = expensePendingDelete
                                expensePendingDelete = null
                                if (toDelete != null) {
                                    viewModel.deleteExpense(toDelete)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { expensePendingDelete = null },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Text("Cancel", color = SleekTextPrimary)
                        }
                    },
                    containerColor = SleekSurface,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            val undoVisible by viewModel.undoDeleteVisible.collectAsStateWithLifecycle()
            val undoMessage by viewModel.undoDeleteMessage.collectAsStateWithLifecycle()

            LaunchedEffect(undoVisible) {
                if (undoVisible) {
                    kotlinx.coroutines.delay(5000)
                    viewModel.dismissUndoDelete()
                }
            }

            ExpenseUndoSnackbar(
                visible = undoVisible,
                message = undoMessage,
                onUndo = { viewModel.undoDeleteExpense() },
                onDismiss = { viewModel.dismissUndoDelete() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            )
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
                        onBack = { activeSettingsSubScreen = null },
                        onNavigateToBadges = { activeSettingsSubScreen = SettingsSubScreen.BadgesAndMilestones },
                        onOpenSettings = {
                            activeSettingsSubScreen = SettingsSubScreen.Appearance
                        },
                        onNavigateToAnalytics = {
                            activeSettingsSubScreen = null
                            navigateToScreen(Screen.Analytics)
                        },
                        onNavigateToGoals = {
                            activeSettingsSubScreen = SettingsSubScreen.SavingsGoals
                        },
                        onNavigateToExport = {
                            activeSettingsSubScreen = SettingsSubScreen.Export
                        },
                        onNavigateToDataManagement = {
                            activeSettingsSubScreen = SettingsSubScreen.DataManagement
                        }
                    )
                    SettingsSubScreen.BadgesAndMilestones -> BadgesMilestonesScreen(
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
                    SettingsSubScreen.Export -> ExportDataScreen(
                        viewModel = viewModel,
                        onBack = { activeSettingsSubScreen = null }
                    )
                    SettingsSubScreen.FinancialHealthStudio -> {
                        com.example.ui.components.FinancialHealthStudioDialog(
                            viewModel = viewModel,
                            onDismiss = { activeSettingsSubScreen = null },
                            onNavigateToBudget = { activeSettingsSubScreen = SettingsSubScreen.Budgets },
                            onNavigateToGoals = { activeSettingsSubScreen = SettingsSubScreen.SavingsGoals }
                        )
                    }
                    SettingsSubScreen.NetWorthAssets -> {
                        com.example.ui.screens.NetWorthAssetsScreen(
                            viewModel = viewModel,
                            onBack = { activeSettingsSubScreen = null }
                        )
                    }
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
            message = "Getting things ready…",
            subtitle = "Loading your transactions, budgets and goals"
        )
    }

    // 🚚 Global Truck Loading Dialog for asynchronous tasks
    TruckLoadingDialog(
        isOpen = globalLoadingMessage != null && !locked,
        message = globalLoadingMessage ?: "Processing...",
        subtitle = "This only takes a moment",
        onDismissRequest = { viewModel.hideAppLoading() }
    )
    }
}
}
}

