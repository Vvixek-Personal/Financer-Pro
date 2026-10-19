package com.example.ui

import com.example.ui.theme.*
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.DataImporter
import com.example.ui.components.ImportParseResult

enum class SettingsSubScreen {
    PersonalData,
    BadgesAndMilestones,
    Appearance,
    Language,
    Currency,
    DateTime,
    Bills,
    CategoriesTags,
    Budgets,
    SavingsGoals,
    Calculations,
    Transactions,
    BackupRestore,
    DataManagement,
    LocalRecovery,
    Security,
    Privacy,
    AboutApp,
    HelpSupport,
    DataAndStorage,
    ThemeAndLanguage,
    Export,
    FaqAndHelp
}

@Composable
fun SettingsHeaderTitle(title: String, onBack: () -> Unit) {
    Surface(
        color = SleekSurface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, SleekBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )
        }
    }
}

// PersonalDataScreen and BadgesAndMilestonesScreen moved to PersonalDataScreen.kt


// ComingSoonConstructionBanner moved to PersonalDataScreen.kt


// ==========================================
// AppearanceScreen moved to AppearanceSettingsScreen.kt

// LanguageScreen moved to LanguageSettingsScreen.kt
// DateTimeScreen moved to DateTimeSettingsScreen.kt


// BillsSettingsScreen moved to BillsSettingsScreen.kt

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
// 6️⃣ CATEGORIES & TAGS SCREEN
// ==========================================
@Composable
fun CategoriesTagsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategories.collectAsStateWithLifecycle()
    val goalCategories by viewModel.goalCategories.collectAsStateWithLifecycle()
    val budgetCategories by viewModel.budgetCategories.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()

    val preventDeleteUsed by viewModel.preventDeleteUsedCategories.collectAsStateWithLifecycle()
    val showCategoryInList by viewModel.showCategoryInTransactionList.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Dialog state variables
    var activeCategoryTypeDialog by remember { mutableStateOf<String?>(null) } // "Expense", "Income", "Savings", "Budget"
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoriesDialog by remember { mutableStateOf(false) }
    var showDeleteCategoriesDialog by remember { mutableStateOf(false) }
    var showReorderCategoriesDialog by remember { mutableStateOf(false) }
    var showCategoryColorsDialog by remember { mutableStateOf(false) }

    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showEditTagsDialog by remember { mutableStateOf(false) }
    var showDeleteTagsDialog by remember { mutableStateOf(false) }
    var showReorderTagsDialog by remember { mutableStateOf(false) }
    var showTagColorsDialog by remember { mutableStateOf(false) }

    var showInfoNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Custom Header matching reference image
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Categories & Tags",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "Manage your categories and tags for better organization",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { showInfoNoteDialog = true }) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(2.dp, Color(0xFF4F46E5)),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "i",
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= SECTION 1: Categories =================
            BillsSettingSectionCard(
                sectionIcon = AppIcons.AccountBalanceWallet,
                sectionIconBg = Color(0xFFEFF0FE),
                sectionIconTint = Color(0xFF4F46E5),
                title = "Categories",
                subtitle = "Create, edit and manage your income & expense categories"
            ) {
                // Top Category Pills/Cards (Grid style: Expense, Income, Savings, Budget)
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Expense Categories Card
                        CategoryPillCard(
                            modifier = Modifier.weight(1f),
                            icon = AppIcons.ArrowDownward,
                            iconBg = Color(0xFFDCFCE7),
                            iconTint = Color(0xFF16A34A),
                            title = "Expense Categories",
                            subtitle = "Manage expense categories",
                            count = expenseCategories.size,
                            onClick = { activeCategoryTypeDialog = "Expense" }
                        )

                        // Income Categories Card
                        CategoryPillCard(
                            modifier = Modifier.weight(1f),
                            icon = AppIcons.ArrowUpward,
                            iconBg = Color(0xFFDBEAFE),
                            iconTint = Color(0xFF2563EB),
                            title = "Income Categories",
                            subtitle = "Manage income categories",
                            count = incomeCategories.size,
                            onClick = { activeCategoryTypeDialog = "Income" }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Savings Categories Card
                        CategoryPillCard(
                            modifier = Modifier.weight(1f),
                            icon = AppIcons.Savings,
                            iconBg = Color(0xFFFFEDD5),
                            iconTint = Color(0xFFEA580C),
                            title = "Savings Categories",
                            subtitle = "Manage savings goals",
                            count = goalCategories.size,
                            onClick = { activeCategoryTypeDialog = "Savings" }
                        )

                        // Budget Categories Card
                        CategoryPillCard(
                            modifier = Modifier.weight(1f),
                            icon = AppIcons.PieChart,
                            iconBg = Color(0xFFF3E8FF),
                            iconTint = Color(0xFF9333EA),
                            title = "Budget Categories",
                            subtitle = "Manage budget limits",
                            count = budgetCategories.size,
                            onClick = { activeCategoryTypeDialog = "Budget" }
                        )
                    }
                }

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Create Category Row
                BillsSettingItemRow(
                    icon = AppIcons.Add,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Create Category",
                    subtitle = "Add a new category",
                    valueText = "",
                    onClick = { showCreateCategoryDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Edit Categories Row
                BillsSettingItemRow(
                    icon = AppIcons.Edit,
                    iconBg = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFEA580C),
                    title = "Edit Categories",
                    subtitle = "Modify or rename existing categories",
                    valueText = "",
                    onClick = { showEditCategoriesDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Delete Categories Row
                BillsSettingItemRow(
                    icon = AppIcons.Delete,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    title = "Delete Categories",
                    subtitle = "Delete categories you no longer use",
                    valueText = "",
                    onClick = { showDeleteCategoriesDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Reorder Categories Row
                BillsSettingItemRow(
                    icon = AppIcons.SwapVert,
                    iconBg = Color(0xFFEBF5FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Reorder Categories",
                    subtitle = "Change the order of categories",
                    valueText = "",
                    onClick = { showReorderCategoriesDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Category Colors Row
                BillsSettingItemRow(
                    icon = AppIcons.Palette,
                    iconBg = Color(0xFFCFFAFE),
                    iconTint = Color(0xFF0891B2),
                    title = "Category Colors",
                    subtitle = "Customize colors for your categories",
                    valueText = "",
                    onClick = { showCategoryColorsDialog = true }
                )
            }

            // ================= SECTION 2: Tags =================
            BillsSettingSectionCard(
                sectionIcon = AppIcons.Sell,
                sectionIconBg = Color(0xFFEFF0FE),
                sectionIconTint = Color(0xFF4F46E5),
                title = "Tags",
                subtitle = "Create, edit and manage tags to label your transactions"
            ) {
                // Create Tag Row
                BillsSettingItemRow(
                    icon = AppIcons.Add,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Create Tag",
                    subtitle = "Add a new tag",
                    valueText = "",
                    onClick = { showCreateTagDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Edit Tags Row
                BillsSettingItemRow(
                    icon = AppIcons.Edit,
                    iconBg = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFEA580C),
                    title = "Edit Tags",
                    subtitle = "Modify or rename existing tags",
                    valueText = "",
                    onClick = { showEditTagsDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Delete Tags Row
                BillsSettingItemRow(
                    icon = AppIcons.Delete,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    title = "Delete Tags",
                    subtitle = "Delete tags you no longer use",
                    valueText = "",
                    onClick = { showDeleteTagsDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Reorder Tags Row
                BillsSettingItemRow(
                    icon = AppIcons.SwapVert,
                    iconBg = Color(0xFFEBF5FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Reorder Tags",
                    subtitle = "Change the order of tags",
                    valueText = "",
                    onClick = { showReorderTagsDialog = true }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Tag Colors Row
                BillsSettingItemRow(
                    icon = AppIcons.Palette,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = "Tag Colors",
                    subtitle = "Customize colors for your tags",
                    valueText = "",
                    onClick = { showTagColorsDialog = true }
                )
            }

            // ================= SECTION 3: General Preferences =================
            BillsSettingSectionCard(
                sectionIcon = AppIcons.Settings,
                sectionIconBg = Color(0xFFEFF0FE),
                sectionIconTint = Color(0xFF4F46E5),
                title = "General Preferences",
                subtitle = "Configure how categories & tags behave in the app"
            ) {
                // Prevent Deleting Used Categories
                BillsSettingSwitchRow(
                    icon = AppIcons.Link,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF059669),
                    title = "Prevent Deleting Used Categories",
                    subtitle = "Protect categories that are used in transactions",
                    checked = preventDeleteUsed,
                    onCheckedChange = { viewModel.togglePreventDeleteUsedCategories(it) }
                )

                HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f))

                // Show Category in Transaction List
                BillsSettingSwitchRow(
                    icon = AppIcons.Sell,
                    iconBg = Color(0xFFEBF5FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Show Category in Transaction List",
                    subtitle = "Display category name in transaction list",
                    checked = showCategoryInList,
                    onCheckedChange = { viewModel.toggleShowCategoryInTransactionList(it) }
                )
            }

            // ================= SECTION 4: Note / Info Banner =================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6FE)),
                border = BorderStroke(1.dp, Color(0xFFE2E4FA)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showInfoNoteDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Note",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3544C4),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Deleting a category will not delete your transactions. Transactions will be moved to \"Uncategorized\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4B5563),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // ==========================================
    // INTERACTIVE DIALOGS FOR CATEGORIES & TAGS
    // ==========================================

    // 1. Manage specific Category Type Dialog
    val activeType = activeCategoryTypeDialog
    if (activeType != null) {
        val type = activeType
        val categoryList = when (type) {
            "Expense" -> expenseCategories
            "Income" -> incomeCategories
            "Savings" -> goalCategories
            "Budget" -> budgetCategories
            else -> expenseCategories
        }
        var newCatInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { activeCategoryTypeDialog = null },
            title = {
                Text(
                    "$type Categories",
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCatInput,
                            onValueChange = { newCatInput = it },
                            placeholder = { Text("New $type category name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newCatInput.isNotBlank()) {
                                    viewModel.addCategoryWithType(newCatInput, type)
                                    newCatInput = ""
                                    Toast.makeText(context, "Added to $type categories!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryList.forEach { cat ->
                            key(cat) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SleekBg, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        cat,
                                        fontWeight = FontWeight.Medium,
                                        color = SleekTextPrimary
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteAnyCategory(cat)
                                            Toast.makeText(context, "Deleted '$cat'", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            AppIcons.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
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
                TextButton(onClick = { activeCategoryTypeDialog = null }) {
                    Text("Done", color = SleekPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 2. Create Category Dialog
    if (showCreateCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("Expense") }

        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            title = {
                Text("Create Category", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Category Name", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        placeholder = { Text("e.g. Groceries, Gym, Freelance") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Category Type", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Expense", "Income", "Savings", "Budget").forEach { t ->
                            val selected = t == selectedType
                            FilterChip(
                                selected = selected,
                                onClick = { selectedType = t },
                                label = { Text(t, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.addCategoryWithType(catName, selectedType)
                            showCreateCategoryDialog = false
                            Toast.makeText(context, "Created '$catName' category!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategoryDialog = false }) {
                    Text("Cancel", color = SleekTextSecondary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3. Edit Categories Dialog
    if (showEditCategoriesDialog) {
        var editingCat by remember { mutableStateOf<String?>(null) }
        var newCatName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditCategoriesDialog = false },
            title = {
                Text("Edit Categories", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                if (editingCat == null) {
                    val allCats = (expenseCategories + incomeCategories + goalCategories + budgetCategories).distinct()
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCats.size, key = { allCats[it] }) { idx ->
                            val cat = allCats[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingCat = cat
                                        newCatName = cat
                                    }
                                    .background(SleekBg, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, fontWeight = FontWeight.Medium, color = SleekTextPrimary)
                                Icon(AppIcons.Edit, contentDescription = "Edit", tint = SleekPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Rename '${editingCat}'", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { editingCat = null }) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val cat = editingCat
                                    if (newCatName.isNotBlank() && cat != null) {
                                        viewModel.renameAnyCategory(cat, newCatName)
                                        editingCat = null
                                        Toast.makeText(context, "Renamed category!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditCategoriesDialog = false }) {
                    Text("Close", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 4. Delete Categories Dialog
    if (showDeleteCategoriesDialog) {
        val allCats = (expenseCategories + incomeCategories + goalCategories + budgetCategories).distinct()

        AlertDialog(
            onDismissRequest = { showDeleteCategoriesDialog = false },
            title = {
                Text("Delete Categories", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Column {
                    Text(
                        "Every category is deletable. Transactions in deleted categories will be reassigned to Uncategorized.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCats.size, key = { allCats[it] }) { idx ->
                            val cat = allCats[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SleekBg, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, fontWeight = FontWeight.Medium, color = SleekTextPrimary)
                                IconButton(
                                    onClick = {
                                        viewModel.deleteAnyCategory(cat)
                                        Toast.makeText(context, "Deleted '$cat'", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(AppIcons.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeleteCategoriesDialog = false }) {
                    Text("Done", color = SleekPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 5. Reorder Categories Dialog
    if (showReorderCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showReorderCategoriesDialog = false },
            title = {
                Text("Reorder Categories", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Text(
                    "Categories are automatically sorted alphabetically and organized by type for quick access throughout transaction entry and budgeting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showReorderCategoriesDialog = false }) {
                    Text("OK", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 6. Category Colors Dialog
    if (showCategoryColorsDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryColorsDialog = false },
            title = {
                Text("Category Colors", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Text(
                    "Category colors are automatically assigned vibrant, distinct palette colors across all charts, pie graphs, and transaction cards for optimal visual scanning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showCategoryColorsDialog = false }) {
                    Text("OK", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 7. Create Tag Dialog
    if (showCreateTagDialog) {
        var tagNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = {
                Text("Create Tag", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tag Name", style = MaterialTheme.typography.labelMedium, color = SleekTextSecondary)
                    OutlinedTextField(
                        value = tagNameInput,
                        onValueChange = { tagNameInput = it },
                        placeholder = { Text("e.g. TaxDeductible, Urgent, Vacation") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tagNameInput.isNotBlank()) {
                            viewModel.addTag(tagNameInput)
                            showCreateTagDialog = false
                            Toast.makeText(context, "Created tag '$tagNameInput'", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTagDialog = false }) {
                    Text("Cancel", color = SleekTextSecondary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 8. Edit Tags Dialog
    if (showEditTagsDialog) {
        var editingTag by remember { mutableStateOf<String?>(null) }
        var newTagName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditTagsDialog = false },
            title = {
                Text("Edit Tags", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                if (editingTag == null) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allTags.size, key = { allTags[it] }) { idx ->
                            val tag = allTags[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingTag = tag
                                        newTagName = tag
                                    }
                                    .background(SleekBg, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tag, fontWeight = FontWeight.Medium, color = SleekTextPrimary)
                                Icon(AppIcons.Edit, contentDescription = "Edit", tint = SleekPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Rename '${editingTag}'", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { editingTag = null }) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val tag = editingTag
                                    if (newTagName.isNotBlank() && tag != null) {
                                        viewModel.renameTag(tag, newTagName)
                                        editingTag = null
                                        Toast.makeText(context, "Renamed tag!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditTagsDialog = false }) {
                    Text("Close", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 9. Delete Tags Dialog
    if (showDeleteTagsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTagsDialog = false },
            title = {
                Text("Delete Tags", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags.size, key = { allTags[it] }) { idx ->
                        val tag = allTags[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SleekBg, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tag, fontWeight = FontWeight.Medium, color = SleekTextPrimary)
                            IconButton(
                                onClick = {
                                    viewModel.deleteTag(tag)
                                    Toast.makeText(context, "Deleted tag '$tag'", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(AppIcons.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeleteTagsDialog = false }) {
                    Text("Done", color = SleekPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 10. Reorder Tags Dialog
    if (showReorderTagsDialog) {
        AlertDialog(
            onDismissRequest = { showReorderTagsDialog = false },
            title = {
                Text("Reorder Tags", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Text(
                    "Tags are sorted alphabetically to help you search and organize transaction labels quickly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showReorderTagsDialog = false }) {
                    Text("OK", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 11. Tag Colors Dialog
    if (showTagColorsDialog) {
        AlertDialog(
            onDismissRequest = { showTagColorsDialog = false },
            title = {
                Text("Tag Colors", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Text(
                    "Tags are automatically styled with subtle primary theme accents and badges across transaction notes and filter lists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showTagColorsDialog = false }) {
                    Text("OK", color = SleekPrimary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 12. Info / Note Dialog
    if (showInfoNoteDialog) {
        AlertDialog(
            onDismissRequest = { showInfoNoteDialog = false },
            title = {
                Text("Categories & Tags Info", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Text(
                    "Deleting a category will not delete your existing transactions or budgets. All associated transactions will safely remain intact and be moved to \"Uncategorized\".\n\nAll categories—including system defaults, expense, income, savings, and budget categories—are completely editable and deletable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekTextPrimary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoNoteDialog = false }) {
                    Text("Got it", color = SleekPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun CategoryPillCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekBg),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SleekPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = SleekTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary,
                fontSize = 12.sp,
                softWrap = true
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SleekTextSecondary,
                fontSize = 10.sp,
                softWrap = true,
                lineHeight = 13.sp
            )
        }
    }
}

// ==========================================
// 7️⃣ BUDGET SETTINGS SCREEN
// ==========================================
@Composable
fun BudgetSettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
    val currencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val includeRecurringBills by viewModel.budgetIncludeRecurringBills.collectAsStateWithLifecycle()

    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val allExpenses by viewModel.expenses.collectAsStateWithLifecycle()

    val warn80 by viewModel.budgetWarning80.collectAsStateWithLifecycle()
    val warn90 by viewModel.budgetWarning90.collectAsStateWithLifecycle()
    val warn100 by viewModel.budgetWarning100.collectAsStateWithLifecycle()

    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var editBudgetInput by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showColorPaletteDialog by remember { mutableStateOf(false) }

    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var targetCategoryForBudget by remember { mutableStateOf("Food") }
    var categoryLimitInput by remember { mutableStateOf("") }

    val masterWarnings = warn80 || warn90 || warn100

    val currentMonthCategorySpent = remember(allExpenses, currencyCode) {
        val cal = Calendar.getInstance()
        val m = cal.get(Calendar.MONTH)
        val y = cal.get(Calendar.YEAR)
        allExpenses.filter {
            it.type != "INCOME" &&
            Calendar.getInstance().apply { timeInMillis = it.date }.run {
                get(Calendar.MONTH) == m && get(Calendar.YEAR) == y
            }
        }.groupBy { it.category }
            .mapValues { entry -> entry.value.realExpense(currencyCode) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header matching reference layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(SleekSurface, CircleShape)
                    .border(1.dp, SleekBorder, CircleShape)
            ) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = SleekTextPrimary
                )
            }

            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(SleekSurface, CircleShape)
                    .border(1.dp, SleekBorder, CircleShape)
            ) {
                Icon(
                    imageVector = AppIcons.Info,
                    contentDescription = "Info",
                    tint = SleekTextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Screen Header Title & Subtitle
            Text(
                text = "Budgets",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary,
                fontSize = 28.sp
            )
            Text(
                text = "Manage your monthly spending limits & warnings.",
                style = MaterialTheme.typography.bodyMedium,
                color = SleekTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. GENERAL SETTINGS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // General Settings Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SleekPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "General Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 16.sp
                        )
                    }

                    // Monthly Budget Limit Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editBudgetInput = if (monthlyBudget % 1.0 == 0.0) {
                                    monthlyBudget.toLong().toString()
                                } else {
                                    monthlyBudget.toString()
                                }
                                showEditBudgetDialog = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.PieChart,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Monthly Budget Limit",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Configure your total monthly spending ceiling",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$currencySymbol${String.format(java.util.Locale.US, "%,.2f", monthlyBudget)}",
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

                    // Currency Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.CurrencyExchange,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Currency",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Current budget currency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "$currencyCode ($currencySymbol)",
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

                    // Recurring Bills Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.Autorenew,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Recurring Bills",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Include recurring bills in budget tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = includeRecurringBills,
                            onCheckedChange = { viewModel.toggleBudgetIncludeRecurringBills(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SleekPrimary,
                                uncheckedThumbColor = SleekTextSecondary,
                                uncheckedTrackColor = SleekBorder
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. BUDGET WARNINGS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Budget Warnings Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "Budget Warnings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 16.sp
                        )
                    }

                    // Warning Thresholds Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.ReportProblem,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Warning Thresholds",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Manage cautionary spend notifications",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = masterWarnings,
                                onCheckedChange = { enable ->
                                    viewModel.toggleBudgetWarning(80, enable)
                                    viewModel.toggleBudgetWarning(90, enable)
                                    viewModel.toggleBudgetWarning(100, enable)
                                }
                            )
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

                    // 80% Caution Indicator Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "80% Caution Indicator",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Warn when spending reaches 80%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = warn80,
                                onCheckedChange = { viewModel.toggleBudgetWarning(80, it) }
                            )
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

                    // 90% High Alert Indicator Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF97316).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFF97316),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "90% High Alert Indicator",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Warn when spending reaches 90%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = warn90,
                                onCheckedChange = { viewModel.toggleBudgetWarning(90, it) }
                            )
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = SleekBorder.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

                    // Exceeded Indicator Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.RemoveCircleOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Exceeded Indicator",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Alert when over budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = warn100,
                                onCheckedChange = { viewModel.toggleBudgetWarning(100, it) }
                            )
                            Icon(
                                imageVector = AppIcons.ChevronRight,
                                contentDescription = null,
                                tint = SleekTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2.5 CATEGORY SPENDING LIMITS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF8B5CF6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Category Spending Limits",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Set individual monthly limits per category",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                targetCategoryForBudget = "Food"
                                categoryLimitInput = ""
                                showCategoryBudgetDialog = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary.copy(alpha = 0.12f))
                        ) {
                            Icon(AppIcons.Add, contentDescription = "Add Category Budget", tint = SleekPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (budgets.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SleekPrimaryContainer.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, SleekBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(AppIcons.Tune, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("No category budgets set", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SleekTextPrimary)
                                    Text("Tap '+' above to set a spending limit for Food, Transport, Shopping, etc.", fontSize = 11.sp, color = SleekTextSecondary)
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (b in budgets) {
                                val spent = currentMonthCategorySpent[b.category] ?: 0.0
                                val limit = b.amountLimit.coerceAtLeast(1.0)
                                val progress = (spent / limit).coerceIn(0.0, 1.2).toFloat()
                                val isOver = spent > limit
                                val isNear = progress >= 0.8f && !isOver

                                val barColor = when {
                                    isOver -> Color(0xFFEF4444)
                                    isNear -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SleekPrimaryContainer.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, SleekBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(b.category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SleekTextPrimary)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = barColor.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (isOver) "Exceeded" else if (isNear) "Near Limit" else "On Track",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = barColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        targetCategoryForBudget = b.category
                                                        categoryLimitInput = if (b.amountLimit % 1.0 == 0.0) b.amountLimit.toLong().toString() else b.amountLimit.toString()
                                                        showCategoryBudgetDialog = true
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(AppIcons.Edit, contentDescription = "Edit", tint = SleekPrimary, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteCategoryBudget(b.category) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(AppIcons.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Progress Bar
                                        LinearProgressIndicator(
                                            progress = { (progress / 1.0f).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = barColor,
                                            trackColor = SleekBorder.copy(alpha = 0.6f)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Spent: $currencySymbol${String.format(Locale.US, "%,.2f", spent)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SleekTextSecondary
                                            )
                                            Text(
                                                text = "Limit: $currencySymbol${String.format(Locale.US, "%,.2f", b.amountLimit)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. APPEARANCE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Appearance Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Palette,
                                    contentDescription = null,
                                    tint = Color(0xFF06B6D4),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 16.sp
                        )
                    }

                    // Indicator Colors Item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showColorPaletteDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF14B8A6).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.ColorLens,
                                        contentDescription = null,
                                        tint = Color(0xFF14B8A6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Indicator Colors",
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Customize warning colors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = SleekTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Color Spectrum Bar Matching Reference
                    val rainbowBrush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFEF4444),
                            Color(0xFFF97316),
                            Color(0xFFF59E0B),
                            Color(0xFFEAB308),
                            Color(0xFF84CC16),
                            Color(0xFF10B981),
                            Color(0xFF06B6D4),
                            Color(0xFF3B82F6),
                            Color(0xFF6366F1),
                            Color(0xFF8B5CF6),
                            Color(0xFFEC4899),
                            Color(0xFFEF4444)
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(rainbowBrush)
                    ) {
                        // Pointer diamond accent matching image
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(10.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. NOTE / INFO BANNER CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekPrimary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SleekPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.Info,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Note:",
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Use existing budget logic. Warning thresholds are based on your transactions and budget limits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    // EDIT BUDGET DIALOG
    if (showEditBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            title = {
                Text(
                    text = "Monthly Budget Limit",
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set your total monthly spending ceiling ($currencyCode):",
                        fontSize = 12.sp,
                        color = SleekTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editBudgetInput,
                        onValueChange = { editBudgetInput = it },
                        prefix = { Text("$currencySymbol ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = editBudgetInput.toDoubleOrNull()
                        if (num != null && num > 0) {
                            viewModel.updateMonthlyBudget(num)
                        }
                        showEditBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBudgetDialog = false }) {
                    Text("Cancel", color = SleekTextSecondary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // INFO DIALOG
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text("About Budgets & Warnings", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Text(
                    "Budgets help you keep track of your overall monthly spending. Your spending progress is updated in real-time as transactions are recorded. Warning thresholds automatically alert you when you reach 80%, 90%, or exceed 100% of your ceiling.",
                    fontSize = 13.sp,
                    color = SleekTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Got It")
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // INDICATOR COLOR CUSTOMIZER DIALOG
    if (showColorPaletteDialog) {
        AlertDialog(
            onDismissRequest = { showColorPaletteDialog = false },
            title = {
                Text("Indicator Colors", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Warning indicator color palette is synced with your active theme system accent palette.", fontSize = 12.sp, color = SleekTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(Color(0xFFF59E0B), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFF10B981), Color(0xFF6366F1)).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, CircleShape)
                                    .border(1.5.dp, SleekBorder, CircleShape)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showColorPaletteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Done")
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // CATEGORY BUDGET DIALOG
    if (showCategoryBudgetDialog) {
        val expenseCatList = listOf("Food", "Shopping", "Entertainment", "Transport", "Bills", "Utilities", "Housing", "Health", "Education", "Other")

        AlertDialog(
            onDismissRequest = { showCategoryBudgetDialog = false },
            title = {
                Text(
                    text = "Set Category Spending Limit",
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose a category and enter your monthly spending limit:",
                        fontSize = 12.sp,
                        color = SleekTextSecondary
                    )

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        expenseCatList.forEach { cat ->
                            val selected = targetCategoryForBudget.equals(cat, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) SleekPrimary else SleekSurface,
                                border = BorderStroke(1.dp, if (selected) SleekPrimary else SleekBorder),
                                modifier = Modifier.clickable {
                                    targetCategoryForBudget = cat
                                }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else SleekTextPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = categoryLimitInput,
                        onValueChange = { categoryLimitInput = it },
                        label = { Text("Limit Amount ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitVal = categoryLimitInput.toDoubleOrNull()
                        if (limitVal != null && limitVal > 0 && targetCategoryForBudget.isNotBlank()) {
                            viewModel.setCategoryBudget(targetCategoryForBudget, limitVal)
                            showCategoryBudgetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Limit", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryBudgetDialog = false }) {
                    Text("Cancel", color = SleekTextSecondary)
                }
            },
            containerColor = SleekSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ==========================================
// 1️⃣1️⃣ BACKUP & RESTORE SCREEN (WITH PORTABLE CODE SYNC)
// ==========================================
@Composable
fun BackupRestoreScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val autoBackup by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val lastBackup by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
    val syncMode by viewModel.syncMode.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val lastCloudSync by viewModel.lastCloudSyncTimestamp.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var generatedCode by remember { mutableStateOf<String?>(null) }
    var inputCode by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showCloudRestoreConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Backup & Restore", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 🛡️ SECTION 0: STORAGE & SYNC ENGINE MODE SELECTOR
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(
                    colors = if (syncMode == "ONLINE_SYNC") {
                        listOf(Color(0xFF0284C7).copy(alpha = 0.8f), Color(0xFF38BDF8).copy(alpha = 0.8f))
                    } else {
                        listOf(Color(0xFF10B981).copy(alpha = 0.8f), Color(0xFF34D399).copy(alpha = 0.8f))
                    }
                )),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7).copy(alpha = 0.15f)
                                    else Color(0xFF10B981).copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (syncMode == "ONLINE_SYNC") AppIcons.CloudSync else AppIcons.Security,
                                contentDescription = "Storage Mode",
                                tint = if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7) else Color(0xFF10B981),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Storage & Sync Destination",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = if (syncMode == "ONLINE_SYNC") "Cloud Sync Enabled (Firebase Vault)" else "Local Only Storage (100% Offline & Private)",
                                fontSize = 12.sp,
                                color = if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7) else Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Choose whether financial records stay exclusively on this device or synchronize to your secure cloud database:",
                        fontSize = 12.sp,
                        color = SleekTextSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 🔲 OPTION A: LOCAL ONLY
                    Surface(
                        onClick = {
                            viewModel.setSyncMode("LOCAL_ONLY")
                            Toast.makeText(context, "Switched to Local Only Mode (Offline)", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (syncMode == "LOCAL_ONLY") Color(0xFF10B981).copy(alpha = 0.12f) else SleekSurfaceElevated,
                        border = BorderStroke(
                            width = if (syncMode == "LOCAL_ONLY") 1.8.dp else 1.dp,
                            color = if (syncMode == "LOCAL_ONLY") Color(0xFF10B981) else SleekBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = syncMode == "LOCAL_ONLY",
                                onClick = {
                                    viewModel.setSyncMode("LOCAL_ONLY")
                                    Toast.makeText(context, "Switched to Local Only Mode", Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF10B981))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Local Storage Only",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SleekTextPrimary
                                    )
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "100% PRIVATE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "All accounts, transactions and budgets stay in SQLite Room on device. Zero external data transmission.",
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ☁️ OPTION B: ONLINE CLOUD SYNC
                    Surface(
                        onClick = {
                            viewModel.setSyncMode("ONLINE_SYNC")
                            Toast.makeText(context, "Switched to Online Cloud Sync Mode", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7).copy(alpha = 0.12f) else SleekSurfaceElevated,
                        border = BorderStroke(
                            width = if (syncMode == "ONLINE_SYNC") 1.8.dp else 1.dp,
                            color = if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7) else SleekBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = syncMode == "ONLINE_SYNC",
                                onClick = {
                                    viewModel.setSyncMode("ONLINE_SYNC")
                                    Toast.makeText(context, "Switched to Online Cloud Sync Mode", Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Online Cloud Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SleekTextPrimary
                                    )
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "FIREBASE VAULT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Securely backup and synchronize accounts, transactions, budgets & goals to Cloud Firestore.",
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🚀 ACTIVE MODE CONTROLS & PROPER BUTTONS
                    if (syncMode == "ONLINE_SYNC") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SleekSurfaceElevated)
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Cloud Sync Status",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextPrimary
                                    )
                                    Text(
                                        text = "State: $cloudSyncStatus • Last synced: $lastCloudSync",
                                        fontSize = 11.sp,
                                        color = SleekTextSecondary
                                    )
                                }
                                if (isCloudSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 🔘 PROPER BUTTON: SYNC NOW
                            Button(
                                onClick = {
                                    viewModel.performCloudSync { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isCloudSyncing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (isCloudSyncing) AppIcons.Sync else AppIcons.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCloudSyncing) "Syncing in Progress..." else "Sync Now to Cloud",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 🔘 PROPER BUTTON: RESTORE FROM CLOUD
                            OutlinedButton(
                                onClick = { showCloudRestoreConfirmDialog = true },
                                enabled = !isCloudSyncing,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Restore Latest From Cloud Vault",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // LOCAL ONLY HELPER BANNER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.1f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Local Mode Active: Zero network traffic. Your finances are strictly offline and encrypted on this device.",
                                fontSize = 11.sp,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            // 📦 SECTION 1: EXPORT BACKUP CODE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(
                    colors = listOf(
                        SleekPrimary.copy(alpha = 0.6f),
                        Color(0xFF8B5CF6).copy(alpha = 0.6f)
                    )
                )),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.QrCode,
                                contentDescription = "Backup Code",
                                tint = Color(0xFF8B5CF6),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Full Portable Data Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = "Export 100% of your transactions, profile & settings into text code",
                                fontSize = 12.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (generatedCode == null) {
                        Button(
                            onClick = {
                                isExporting = true
                                scope.launch {
                                    val code = viewModel.generateFullBackupCode()
                                    generatedCode = code
                                    isExporting = false
                                }
                            },
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Encrypted Code...")
                            } else {
                                Icon(AppIcons.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate My Backup Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val code = generatedCode ?: ""
                        // Display Generated Code Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SleekBg)
                                .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = code,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SleekTextPrimary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "Backup code copied to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Icon(AppIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Code", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    isExporting = true
                                    scope.launch {
                                        val code = viewModel.generateFullBackupCode()
                                        generatedCode = code
                                        isExporting = false
                                        Toast.makeText(context, "Fresh backup code regenerated!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(AppIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF16A34A).copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Ready to paste! Includes Profile, 2-Year Transactions, Calendar, Savings & Settings.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 📥 SECTION 2: IMPORT / RESTORE FROM CODE
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.FileDownload,
                                contentDescription = "Restore Code",
                                tint = SleekPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Restore Data From Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SleekTextPrimary
                            )
                            Text(
                                text = "Paste your backup code string below to restore all details",
                                fontSize = 12.sp,
                                color = SleekTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it },
                        placeholder = { Text("Paste AISTUDIO_BACKUP_V1:... code string here", fontSize = 12.sp, color = SleekTextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekPrimary,
                            unfocusedBorderColor = SleekBorder,
                            focusedContainerColor = SleekBg,
                            unfocusedContainerColor = SleekBg
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    inputCode = clip
                                    Toast.makeText(context, "Pasted from clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(AppIcons.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste")
                        }

                        Button(
                            onClick = {
                                if (inputCode.isBlank()) {
                                    Toast.makeText(context, "Please paste a backup code first!", Toast.LENGTH_SHORT).show()
                                } else {
                                    showRestoreConfirmDialog = true
                                }
                            },
                            enabled = inputCode.isNotBlank() && !isRestoring,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restoring...")
                            } else {
                                Icon(AppIcons.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Data Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 💾 SECTION 3: LOCAL OFFLINE MANUAL BACKUP
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LOCAL STORAGE BACKUP", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("Your database is also snapshot-backed on device storage.", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Last Local Snapshot: $lastBackup", style = MaterialTheme.typography.bodySmall, color = SleekPrimary)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.markBackupPerformed()
                            Toast.makeText(context, "Local backup snapshot updated!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(AppIcons.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Manual Local Snapshot")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Automatic Daily Sync Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automatic Daily Snapshot", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        Text("Saves local database snapshots daily without cloud dependency", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    }
                    Switch(checked = autoBackup, onCheckedChange = { viewModel.toggleAutoBackupEnabled(it) })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(150.dp))
        }
    }

    // 🚨 RESTORE CONFIRMATION DIALOG
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore From Backup Code?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This action will import all 2-year transactions, calendar logs, savings goals, budgets, profile details, and settings from the backup code. Existing database records will be synced and updated. Proceed?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        isRestoring = true
                        scope.launch {
                            val res = viewModel.restoreFromBackupCode(inputCode)
                            isRestoring = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "🎉 ${res.getOrNull()}", Toast.LENGTH_LONG).show()
                                inputCode = ""
                            } else {
                                Toast.makeText(context, "❌ Error: ${res.exceptionOrNull()?.localizedMessage ?: "Invalid code format"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                ) {
                    Text("Yes, Restore Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ☁️ CLOUD RESTORE CONFIRMATION DIALOG
    if (showCloudRestoreConfirmDialog) {
        val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val isUserLoggedIn = authUser != null
        AlertDialog(
            onDismissRequest = { showCloudRestoreConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(AppIcons.CloudDownload, contentDescription = null, tint = Color(0xFF0284C7))
                    Text("Restore from Cloud Vault?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (!isUserLoggedIn) {
                    Text("You must be signed in to access and restore your cloud vault. Please sign in via the Account tab first.")
                } else {
                    Text("This will download your latest cloud backup and restore your accounts, transactions, budgets, and savings goals into the local database. An emergency safety backup of your current local database will be created automatically before restoring. Proceed?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloudRestoreConfirmDialog = false
                        viewModel.restoreFromCloud { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = isUserLoggedIn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Restore from Cloud")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 🚚 Delivery Truck Loading Dialog for Export & Restore Operations
    TruckLoadingDialog(
        isOpen = isExporting,
        message = "Generating Encrypted Backup Code...",
        subtitle = "Packing transactions, budgets & settings securely"
    )

    TruckLoadingDialog(
        isOpen = isRestoring,
        message = "Restoring Financial Records...",
        subtitle = "Decrypting vault and syncing local database"
    )
}

// ==========================================
// 1️⃣2️⃣ DATA MANAGEMENT SCREEN
// ==========================================
@Composable
fun DataManagementScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onNavigateToLocalRecovery: () -> Unit = {}
) {
    val storageSize by viewModel.storageSize.collectAsStateWithLifecycle()
    val syncMode by viewModel.syncMode.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val lastCloudSync by viewModel.lastCloudSyncTimestamp.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val currencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportParseResult?>(null) }
    var selectedImportAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var skipDuplicates by remember { mutableStateOf(true) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val res = DataImporter.parseFile(context, uri, currencyCode, selectedImportAccountId)
            if (res.transactions.isNotEmpty()) {
                importResult = res
                showImportDialog = true
            } else {
                val err = res.errors.firstOrNull() ?: "No valid transactions found in selected file."
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Data Management", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LOCAL RECOVERY SYSTEM", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("Versioned rolling snapshots with SHA-256 integrity checks and automated rollback protection.", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToLocalRecovery,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Icon(AppIcons.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Local Recovery", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 📥 IMPORT TRANSACTIONS (CSV / JSON) CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text("IMPORT TRANSACTIONS (CSV / JSON)", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import bank statements, spreadsheets, or backup ledgers directly into SQLite database. Supports standard CSV, Excel exports, and JSON formats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(AppIcons.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose CSV or JSON File", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 💾 STORAGE & SYNC ENGINE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (syncMode == "ONLINE_SYNC") AppIcons.CloudSync else AppIcons.Security,
                                contentDescription = null,
                                tint = if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7) else Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Text("STORAGE & SYNC DESTINATION", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        }
                        Surface(
                            color = if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (syncMode == "ONLINE_SYNC") "CLOUD SYNC" else "LOCAL ONLY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncMode == "ONLINE_SYNC") Color(0xFF0284C7) else Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (syncMode == "ONLINE_SYNC") {
                            "Encrypted cloud synchronization active with Firebase Vault. Status: $cloudSyncStatus • Last synced: $lastCloudSync"
                        } else {
                            "100% Offline & Private: Records stay solely in on-device SQLite database. No data is sent to external servers."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val target = if (syncMode == "ONLINE_SYNC") "LOCAL_ONLY" else "ONLINE_SYNC"
                                viewModel.setSyncMode(target)
                                Toast.makeText(
                                    context,
                                    if (target == "ONLINE_SYNC") "Switched to Online Cloud Sync" else "Switched to Local Storage Only",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (syncMode == "ONLINE_SYNC") AppIcons.Security else AppIcons.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (syncMode == "ONLINE_SYNC") "Use Local Only" else "Enable Cloud Sync", fontSize = 12.sp)
                        }

                        if (syncMode == "ONLINE_SYNC") {
                            Button(
                                onClick = {
                                    viewModel.performCloudSync { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isCloudSyncing,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                if (isCloudSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(AppIcons.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCloudSyncing) "Syncing..." else "Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LOCAL STORAGE USAGE", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text("Total database size: $storageSize", style = MaterialTheme.typography.bodyMedium, color = SleekPrimary)

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.refreshUsageData()
                            Toast.makeText(context, "Cache cleared successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear Temporary Cache")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DESTRUCTIVE ZONE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("Resetting data will clear custom categories and settings.", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Application Preferences", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Preferences?") },
            text = { Text("Are you sure you want to reset all custom app settings to default?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showResetDialog = false
                        Toast.makeText(context, "Preferences reset.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    val currentImport = importResult
    if (showImportDialog && currentImport != null) {
        val totalImportAmount = currentImport.transactions.sumOf { it.amount }
        val incomeCount = currentImport.transactions.count { it.type == "INCOME" }
        val expenseCount = currentImport.transactions.count { it.type == "EXPENSE" }

        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                importResult = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(AppIcons.FileDownload, contentDescription = null, tint = Color(0xFF0284C7))
                    Text("Import ${currentImport.format} Data")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Found ${currentImport.transactions.size} transactions ($expenseCount expenses, $incomeCount income).",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekTextPrimary
                    )
                    Text(
                        text = "Total volume: $currencySymbol%,.2f".format(totalImportAmount),
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "TARGET ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextSecondary
                    )

                    // Account selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            val isSelected = acc.id == selectedImportAccountId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0284C7) else SleekSurface,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else SleekBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedImportAccountId = acc.id }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = acc.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SleekTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$currencySymbol%,.0f".format(acc.balance),
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else SleekTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Skip existing duplicates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
                            Text("Prevents adding identical entries on same date", fontSize = 10.sp, color = SleekTextSecondary)
                        }
                        Switch(
                            checked = skipDuplicates,
                            onCheckedChange = { skipDuplicates = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }

                    if (currentImport.errors.isNotEmpty()) {
                        Text(
                            text = "${currentImport.errors.size} row(s) skipped due to format issues.",
                            fontSize = 10.sp,
                            color = Color(0xFFEF5350)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importTransactions(
                            transactions = currentImport.transactions,
                            targetAccountId = selectedImportAccountId,
                            skipDuplicates = skipDuplicates
                        ) { imported, skipped ->
                            Toast.makeText(
                                context,
                                "Successfully imported $imported transactions" + if (skipped > 0) " ($skipped duplicates skipped)" else "",
                                Toast.LENGTH_LONG
                            ).show()
                            showImportDialog = false
                            importResult = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Import Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showImportDialog = false
                        importResult = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// 1️⃣3️⃣ SECURITY SETTINGS SCREEN
// ==========================================
@Composable
fun SecuritySettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val appPin by viewModel.appPin.collectAsStateWithLifecycle()
    val lockOnRestart by viewModel.lockOnRestart.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val hideAmounts by viewModel.hideSensitiveAmounts.collectAsStateWithLifecycle()
    val isScreenshotProtected by viewModel.screenshotProtection.collectAsStateWithLifecycle()
    val autoLockDuration by viewModel.autoLockDuration.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Security Settings", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PASSCODE PIN LOCK", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Text(if (!appPin.isNullOrBlank()) "Passcode PIN Status: ACTIVE 🔒" else "Passcode PIN Status: DISABLED 🔓", style = MaterialTheme.typography.bodySmall, color = SleekPrimary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showPinDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (appPin.isNullOrBlank()) "Set PIN" else "Change PIN")
                        }

                        if (!appPin.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setAppPin(null)
                                    Toast.makeText(context, "PIN Passcode removed.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Remove PIN")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Lock Immediately on App Restart", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Requires PIN whenever application reopens", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = lockOnRestart, onCheckedChange = { viewModel.toggleLockOnRestart(it) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    val canUseBiometric = remember(context) {
                        try {
                            androidx.biometric.BiometricManager.from(context)
                                .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
                        } catch (_: Exception) {
                            false
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Biometric / Fingerprint Unlock", fontWeight = FontWeight.Bold, color = if (canUseBiometric) SleekTextPrimary else SleekTextSecondary)
                            Text(
                                if (!canUseBiometric) "No strong biometric enrolled or supported on device" else "Use system biometric authentication",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary
                            )
                        }
                        Switch(
                            checked = biometricEnabled && canUseBiometric,
                            enabled = canUseBiometric && !appPin.isNullOrBlank(),
                            onCheckedChange = { viewModel.toggleBiometricEnabled(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hide Sensitive Balances on Launch", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Blurs total balance card by default", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(checked = hideAmounts, onCheckedChange = { viewModel.toggleHideSensitiveAmounts(it) })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Screenshot & Capture Protection", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Blocks Android system screenshots & screen recordings (FLAG_SECURE) for banking privacy", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        }
                        Switch(
                            checked = isScreenshotProtected,
                            onCheckedChange = { viewModel.toggleScreenshotProtection(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SleekBorder)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Inactivity Auto-Lock Duration", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        Text("Re-locks the app after being placed in background", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        val currentAutoLock = com.example.data.AutoLockDuration.fromString(autoLockDuration)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            com.example.data.AutoLockDuration.values().forEach { option ->
                                val isSelected = currentAutoLock == option
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateAutoLockDuration(option.name) },
                                    label = { Text(option.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SleekPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4) pinInput = it },
                    label = { Text("Enter 4 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length == 4) {
                        viewModel.setAppPin(pinInput)
                        showPinDialog = false
                        pinInput = ""
                        Toast.makeText(context, "PIN Passcode set successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Please enter exactly 4 digits", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Save PIN") }
            },
            dismissButton = { OutlinedButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
        )
    }
}

// ==========================================
// 1️⃣4️⃣ PRIVACY SETTINGS SCREEN
// ==========================================
@Composable
fun PrivacySettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val privacyMode by viewModel.privacyModeEnabled.collectAsStateWithLifecycle()
    val aiConsent by viewModel.aiConsentEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Privacy Settings", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DATA STORAGE & PRIVACY ARCHITECTURE", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Core Storage: All transaction entries, receipts, accounts, and local backup files remain strictly on your local device inside Room SQLite database. Backups to cloud are disabled.\n" +
                        "• Online Features: Exchange rate updates (open.er-api.com) and optional AI Advisor (Google Gemini via Firebase AI Logic) require internet. AI features are disabled by default and require explicit consent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Financial Advisor", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text(
                                "Enable Gemini AI insights and daily budget recommendations",
                                style = MaterialTheme.typography.bodySmall,
                                color = SleekTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = aiConsent,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    viewModel.promptAiConsentDialog()
                                } else {
                                    viewModel.setAiConsent(false)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "When enabled, high-level monthly aggregates (total income, expenses, net balance) and questions are transmitted securely to Google Gemini servers via Firebase. Transaction notes, account numbers, and merchant names are NEVER shared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekTextSecondary.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Privacy Blur Mode", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        Text("Mask financial numbers when sharing screens", style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary)
                    }
                    Switch(checked = privacyMode, onCheckedChange = { viewModel.togglePrivacyModeEnabled(it) })
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

// ==========================================
// 1️⃣5️⃣ ABOUT APP & UPDATES TIMELINE SCREEN
// ==========================================
@Composable
fun AboutAppScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("What's New & About App", onBack)

        // Segmented Tabs: Updates Timeline vs App Specs
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(AppIcons.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Updates Timeline", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(AppIcons.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("About & System", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                item {
                    UpdatesTimelineView()
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val logoPulse = rememberLoopFloat(0.96f, 1.04f, 2200, "app_logo_pulse", FastOutSlowInEasing, reverse = true, rest = 1f)
                Box(
                    modifier = Modifier
                        .scale(logoPulse)
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(BorderStroke(2.dp, SleekPrimary.copy(alpha = 0.5f)), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo_modern),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Finance Tracker Pro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                Text("Version 1.28 • Build 2026.09 (v28)", style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary)

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("System Architecture & Specifications", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        HorizontalDivider(color = SleekBorder)
                        DetailSpecRow("Platform", "Android (Jetpack Compose & Kotlin)")
                        DetailSpecRow("Database", "Dual-Tier: 100% Offline Room + Optional Firebase Cloud Sync")
                        DetailSpecRow("Screen Protection", "Android FLAG_SECURE Banking Privacy Shield")
                        DetailSpecRow("Diagnostics", "AI Financial Health Score & Loan EMI Engine")
                        DetailSpecRow("Localization", "9 Built-in Languages with native RTL")
                        DetailSpecRow("Currencies", "100+ Global fiat currencies catalog with live forex")
                        DetailSpecRow("Data Security", "Local Encrypted File Storage, Biometric & PIN Lock")
                        DetailSpecRow("Target SDK", "Android 15 (API 36)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Privacy Guarantee & Data Safety", fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                        Text(
                            "Financer defaults to 100% offline local storage on your device. When you explicitly enable Online Cloud Sync (Google Firebase Firestore) or use the AI Financial Advisor (Google Gemini), your financial records and queries are transmitted securely via encrypted HTTPS to Google Cloud Services under your authenticated account. Data is encrypted in transit and at rest, never sold, and never used for advertising.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🚚 UIVERSE.IO DELIVERY TRUCK LOADER PREVIEW CARD
                var showTruckPreviewDialog by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Delivery Truck Loading Engine",
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                        Text(
                            "Interactive high-fidelity physics loader (Uiverse.io by vinodjangid07) used across app startup, cloud sync, backups and data operations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        DeliveryTruckLoader(
                            scale = 1.0f,
                            caption = "Loading Assets & Data...",
                            subCaption = "1s suspension bobbing • 1.4s road animation"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = { showTruckPreviewDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(AppIcons.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Fullscreen Loading Screen")
                        }
                    }
                }
                if (showTruckPreviewDialog) {
                    TruckLoadingDialog(
                        isOpen = true,
                        message = "Synchronizing Financial Vault...",
                        subtitle = "Testing Uiverse.io delivery truck loading screen",
                        onDismissRequest = { showTruckPreviewDialog = false }
                    )
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun DetailSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = SleekTextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary)
    }
}

// ==========================================
// 1️⃣6️⃣ HELP & SUPPORT SCREEN
// ==========================================
@Composable
fun HelpSupportScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val faqs = remember {
        listOf(
            "How do I use Quick Shortcuts and the All Shortcuts Launcher?" to "You can access primary shortcuts directly from your Home dashboard. Tap the 'More' button to open the full-screen App Launcher with all 16+ tools, search filter, and category pills.",
            "How do I view App Update details and release specifications?" to "Navigate to Sidebar -> What's New & About App -> Updates Timeline to inspect each release, its realtime date, and categorized specifications.",
            "How does the Daily Streak work?" to "Every day you open the app and review or record financial activity, your streak advances! Celebrate milestones with dynamic flame bursts, unlock higher tier badges (Starter -> Bronze -> Silver -> Gold -> Platinum -> Diamond), and tap anywhere on screen to smoothly dismiss the celebration.",
            "How do I use the interactive Calendar tab?" to "The Calendar tab provides a visual breakdown of your income and expenses day by day. Tap any day to inspect transactions, toggle month/week navigation, and use filters to analyze your cash flow across time.",
            "How do I change the default currency?" to "Go to Sidebar -> Currency, search for your currency, and choose Option A (Keep existing) or Option B (Convert existing transactions using real rates).",
            "Are my financial transactions safe?" to "Yes! Your entire financial database is stored 100% locally on your device with local backup & restore capabilities.",
            "How do I manage my monthly budgets?" to "Go to Sidebar -> Budgets or the Home screen budget card to configure monthly limits and warning thresholds.",
            "How do I lock the app with a PIN?" to "Navigate to Sidebar -> Security and tap 'Set PIN' to create a 4-digit security code.",
            "How do I switch the application language?" to "Navigate to Sidebar -> Language and select your language. It instantly updates text across all screens."
        )
    }

    val filtered = remember(searchQuery) {
        faqs.filter { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Help & Support Center", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search help topics") },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null, tint = SleekTextSecondary) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            filtered.forEach { (question, answer) ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(question, fontWeight = FontWeight.Bold, color = SleekTextPrimary, modifier = Modifier.weight(1f))
                            Icon(if (expanded) AppIcons.KeyboardArrowUp else AppIcons.KeyboardArrowDown, contentDescription = null, tint = SleekPrimary)
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(answer, style = MaterialTheme.typography.bodySmall, color = SleekTextSecondary, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

// Dedicated Export Statements & Reports Screen
@Composable
fun ExportDataScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val currencyCode by viewModel.selectedCurrencyCode.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

    var dateRangeFilter by remember { mutableStateOf("All Time") }
    var typeFilter by remember { mutableStateOf("ALL") }
    var categoryFilter by remember { mutableStateOf("ALL") }
    var includeDetailedTxns by remember { mutableStateOf(true) }

    var showCategoryDropdown by remember { mutableStateOf(false) }

    val allCategories = remember(expenses) {
        listOf("ALL") + expenses.map { it.category }.distinct().sorted()
    }

    val filteredExpenses = remember(expenses, dateRangeFilter, typeFilter, categoryFilter) {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        expenses.filter { exp ->
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
            val matchesDate = when (dateRangeFilter) {
                "This Month" -> expCal.get(Calendar.MONTH) == currentMonth && expCal.get(Calendar.YEAR) == currentYear
                "Last Month" -> {
                    val lastMonthCal = Calendar.getInstance().apply {
                        add(Calendar.MONTH, -1)
                    }
                    expCal.get(Calendar.MONTH) == lastMonthCal.get(Calendar.MONTH) && expCal.get(Calendar.YEAR) == lastMonthCal.get(Calendar.YEAR)
                }
                "This Year" -> expCal.get(Calendar.YEAR) == currentYear
                else -> true
            }

            val matchesType = when (typeFilter) {
                "EXPENSE" -> exp.type != "INCOME"
                "INCOME" -> exp.type == "INCOME"
                else -> true
            }

            val matchesCategory = if (categoryFilter == "ALL") true else exp.category == categoryFilter

            matchesDate && matchesType && matchesCategory
        }
    }

    val totalIncome = remember(filteredExpenses, currencyCode) { filteredExpenses.realIncome(currencyCode) }
    val totalExpense = remember(filteredExpenses, currencyCode) { filteredExpenses.realExpense(currencyCode) }
    val netBalance = totalIncome - totalExpense

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Export Statements & Reports", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STATEMENT FILTERS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(AppIcons.FilterList, contentDescription = null, tint = SleekPrimary)
                        Text(
                            text = "Statement Configuration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SleekTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Date Range Filter:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All Time", "This Month", "Last Month", "This Year").forEach { option ->
                            val isSelected = dateRangeFilter == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SleekPrimary else SleekNeutralLight)
                                    .border(1.dp, if (isSelected) SleekPrimary else SleekBorder, RoundedCornerShape(12.dp))
                                    .clickable { dateRangeFilter = option }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SleekTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Transaction Type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL" to "All Types", "EXPENSE" to "Expenses", "INCOME" to "Income").forEach { (typeKey, label) ->
                            val isSelected = typeFilter == typeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SleekPrimaryContainer else SleekNeutralLight)
                                    .border(1.dp, if (isSelected) SleekPrimary else SleekBorder, RoundedCornerShape(12.dp))
                                    .clickable { typeFilter = typeKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SleekPrimary else SleekTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SleekTextSecondary)

                        Box {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SleekNeutralLight,
                                border = BorderStroke(1.dp, SleekBorder),
                                modifier = Modifier.clickable { showCategoryDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (categoryFilter == "ALL") "All Categories" else categoryFilter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekTextPrimary
                                    )
                                    Icon(AppIcons.ArrowDropDown, contentDescription = null, tint = SleekTextSecondary)
                                }
                            }

                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                allCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(if (cat == "ALL") "All Categories" else cat) },
                                        onClick = {
                                            categoryFilter = cat
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SleekBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include Itemized Transaction Ledger", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekTextPrimary)
                            Text("Include full line-item transaction table in PDF/CSV exports", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                        Switch(
                            checked = includeDetailedTxns,
                            onCheckedChange = { includeDetailedTxns = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SleekPrimary)
                        )
                    }
                }
            }

            // SUMMARY PREVIEW BADGE
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekPrimaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredExpenses.size} Records Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SleekPrimary
                        )
                        Text(
                            text = "Filter: $dateRangeFilter",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SleekTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF10B981).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Income", fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                Text("₹%,.0f".format(totalIncome), fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(ExpenseRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Expense", fontSize = 9.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                                Text("₹%,.0f".format(totalExpense), fontSize = 12.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SleekNeutralLight, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Net Flow", fontSize = 9.sp, color = SleekTextSecondary, fontWeight = FontWeight.Bold)
                                Text("₹%,.0f".format(netBalance), fontSize = 12.sp, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // EXPORT FORMAT 1: PDF REPORT
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("PDF Financial Ledger Statement", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SleekTextPrimary)
                            Text("Formatted document with executive KPI summary and transaction ledger table.", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (filteredExpenses.isEmpty()) {
                                Toast.makeText(context, "No transactions found matching the selected filters.", Toast.LENGTH_SHORT).show()
                            } else {
                                DataExporter.sharePdfReport(
                                    context = context,
                                    expenses = filteredExpenses,
                                    dateRangeStr = dateRangeFilter,
                                    typeFilterStr = typeFilter,
                                    categoryFilterStr = categoryFilter,
                                    includeDetailedTxns = includeDetailedTxns
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF Document", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // EXPORT FORMAT 2: CSV SPREADSHEET
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.Description, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("CSV Spreadsheet File", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SleekTextPrimary)
                            Text("Raw structured table data ready for Microsoft Excel, Google Sheets, or accounting tools.", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            if (filteredExpenses.isEmpty()) {
                                Toast.makeText(context, "No transactions found matching the selected filters.", Toast.LENGTH_SHORT).show()
                            } else {
                                DataExporter.exportToCSV(
                                    context = context,
                                    expenses = filteredExpenses,
                                    dateRangeStr = dateRangeFilter,
                                    typeFilterStr = typeFilter,
                                    categoryFilterStr = categoryFilter
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF16A34A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.FileDownload, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export CSV Spreadsheet", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // EXPORT FORMAT 3: JSON STRUCTURED DATA
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.Code, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("JSON Data Export", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SleekTextPrimary)
                            Text("Standard developer & backup JSON schema with itemized ledger array and metrics summary.", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            if (filteredExpenses.isEmpty()) {
                                Toast.makeText(context, "No transactions found matching the selected filters.", Toast.LENGTH_SHORT).show()
                            } else {
                                DataExporter.exportToJson(
                                    context = context,
                                    expenses = filteredExpenses,
                                    dateRangeStr = dateRangeFilter,
                                    typeFilterStr = typeFilter,
                                    categoryFilterStr = categoryFilter
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.Code, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export JSON Document", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // EXPORT FORMAT 4: GRAPHIC SUMMARY REPORT
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AppIcons.BarChart, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(24.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Graphic Summary Report", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SleekTextPrimary)
                            Text("High-resolution graphic image visual summarizing key financial metrics.", fontSize = 11.sp, color = SleekTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            if (filteredExpenses.isEmpty()) {
                                Toast.makeText(context, "No transactions found matching the selected filters.", Toast.LENGTH_SHORT).show()
                            } else {
                                DataExporter.shareImageReport(
                                    context = context,
                                    expenses = filteredExpenses,
                                    dateRangeStr = dateRangeFilter,
                                    typeFilterStr = typeFilter,
                                    categoryFilterStr = categoryFilter
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, SleekPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.Share, contentDescription = null, tint = SleekPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Graphic Summary", color = SleekPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// Keep legacy screen aliases for backwards compatibility
@Composable fun DataAndStorageScreen(viewModel: FinanceViewModel, onBack: () -> Unit) { DataManagementScreen(viewModel, onBack) }
@Composable fun ThemeAndLanguageScreen(viewModel: FinanceViewModel, onBack: () -> Unit) { AppearanceScreen(viewModel, onBack) }
@Composable fun FaqAndHelpScreen(viewModel: FinanceViewModel, onBack: () -> Unit) { HelpSupportScreen(viewModel, onBack) }
