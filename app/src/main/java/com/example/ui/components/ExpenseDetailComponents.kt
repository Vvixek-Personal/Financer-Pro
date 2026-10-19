package com.example.ui

import com.example.ui.theme.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Expense
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom Modifier extension to draw a dashed border around rounded rectangles.
 */
fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 18.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp
) = this.drawBehind {
    val strokePx = strokeWidth.toPx()
    val radiusPx = cornerRadius.toPx()
    val dashPx = dashLength.toPx()
    val gapPx = gapLength.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), 0f)

    val inset = strokePx / 2
    val rect = RoundRect(
        left = inset,
        top = inset,
        right = size.width - inset,
        bottom = size.height - inset,
        cornerRadius = CornerRadius(radiusPx, radiusPx)
    )
    val path = Path().apply { addRoundRect(rect) }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokePx, pathEffect = pathEffect)
    )
}

/**
 * High-fidelity Custom Vector Illustration for the "No Receipt Image Attached" placeholder,
 * matching the aesthetic in the reference design.
 */
@Composable
fun ReceiptIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp, 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft glowing aura circle behind receipt
            drawCircle(
                color = Color(0xFFE0F2FE).copy(alpha = 0.85f),
                radius = w * 0.36f,
                center = Offset(w * 0.5f, h * 0.52f)
            )

            // Sparkle / cross plus marks
            val plusColor = Color(0xFF93C5FD)
            // Left sparkle
            drawLine(
                color = plusColor,
                start = Offset(w * 0.28f, h * 0.35f),
                end = Offset(w * 0.34f, h * 0.35f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = plusColor,
                start = Offset(w * 0.31f, h * 0.30f),
                end = Offset(w * 0.31f, h * 0.40f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Right sparkle
            drawLine(
                color = plusColor,
                start = Offset(w * 0.68f, h * 0.38f),
                end = Offset(w * 0.74f, h * 0.38f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = plusColor,
                start = Offset(w * 0.71f, h * 0.33f),
                end = Offset(w * 0.71f, h * 0.43f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Small bottom-right sparkle dot
            drawCircle(
                color = plusColor.copy(alpha = 0.7f),
                radius = 2.dp.toPx(),
                center = Offset(w * 0.66f, h * 0.68f)
            )

            // Receipt paper boundaries
            val left = w * 0.37f
            val right = w * 0.63f
            val top = h * 0.18f
            val bottom = h * 0.82f
            val foldSize = 8.dp.toPx()

            // Receipt paper path with folded corner and tear teeth
            val paperPath = Path().apply {
                moveTo(left, top + 6.dp.toPx())
                quadraticTo(left, top, left + 6.dp.toPx(), top)
                lineTo(right - foldSize, top)
                lineTo(right, top + foldSize)
                lineTo(right, bottom - 4.dp.toPx())

                val teeth = 4
                val toothWidth = (right - left) / teeth
                for (i in 0 until teeth) {
                    val toothX = right - (i + 0.5f) * toothWidth
                    val toothY = if (i % 2 == 0) bottom - 3.dp.toPx() else bottom + 1.dp.toPx()
                    val endX = right - (i + 1f) * toothWidth
                    lineTo(toothX, toothY)
                    lineTo(endX, bottom - 4.dp.toPx())
                }
                lineTo(left, top + 6.dp.toPx())
                close()
            }

            // Draw paper body fill
            drawPath(
                path = paperPath,
                color = Color.White
            )

            // Draw paper outline
            drawPath(
                path = paperPath,
                color = Color(0xFF93C5FD),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw corner fold triangle outline
            val foldPath = Path().apply {
                moveTo(right - foldSize, top)
                lineTo(right - foldSize, top + foldSize)
                lineTo(right, top + foldSize)
            }
            drawPath(
                path = foldPath,
                color = Color(0xFF93C5FD),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw receipt content lines
            val lineLeft = left + 6.dp.toPx()
            val lineRight = right - 6.dp.toPx()

            // Line 1
            drawLine(
                color = Color(0xFF93C5FD),
                start = Offset(lineLeft, top + 13.dp.toPx()),
                end = Offset(lineRight - 4.dp.toPx(), top + 13.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Line 2
            drawLine(
                color = Color(0xFF93C5FD),
                start = Offset(lineLeft, top + 20.dp.toPx()),
                end = Offset(lineRight - 2.dp.toPx(), top + 20.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Line 3
            drawLine(
                color = Color(0xFF93C5FD),
                start = Offset(lineLeft, top + 27.dp.toPx()),
                end = Offset(lineLeft + (lineRight - lineLeft) * 0.6f, top + 27.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Pixel-perfect Receipt Detail screen matching user specification and reference layout.
 */
@Composable
fun ExpenseDetailDialog(
    expense: Expense,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ExpenseDetailContent(
            expense = expense,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick
        )
    }
}

@Composable
fun ExpenseDetailContent(
    expense: Expense,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currencySymbol by viewModel.selectedCurrencySymbol.collectAsStateWithLifecycle()
    val categoryIcons by viewModel.categoryIcons.collectAsStateWithLifecycle()

    var currentImagePath by remember(expense.imagePath) { mutableStateOf(expense.imagePath) }
    var activeBitmapForEdit by remember { mutableStateOf<Bitmap?>(null) }
    var showCropper by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFullImageViewer by remember { mutableStateOf(false) }

    // Media Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadFullResolutionBitmap(context, uri)
            if (bitmap != null) {
                activeBitmapForEdit = bitmap
                showCropper = true
            } else {
                Toast.makeText(context, "Error loading image from gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            activeBitmapForEdit = bitmap
            showCropper = true
        }
    }

    val isIncome = expense.type == "INCOME"
    val formattedDate = remember(expense.date) {
        SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(expense.date))
    }

    // Color definitions matching the screenshot
    val pageBg = SleekBg
    val cardBg = SleekSurface
    val cardBorder = SleekBorder
    val primaryBlue = SleekPrimary
    val lightBlueButtonBg = SleekPrimaryContainer
    val lightBlueButtonText = SleekPrimary
    val darkBlueButtonBg = SleekPrimary
    val incomeGreen = IncomeGreen
    val expenseRed = ExpenseRed

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg),
        color = pageBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = 90.dp) // Room for sticky bottom button
                ) {
                    // ==========================================
                    // 🔝 TOP NAVIGATION BAR
                    // ==========================================
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button (<)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.ArrowBack,
                                    contentDescription = "Back",
                                    tint = SleekTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Title
                        Text(
                            text = "Receipt Detail",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 18.sp
                        )

                        // Action Buttons: Edit & Delete
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit Icon Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onEditClick()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.Edit,
                                        contentDescription = "Edit Transaction",
                                        tint = SleekTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Delete Icon Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showDeleteConfirmDialog = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.DeleteOutline,
                                        contentDescription = "Delete Transaction",
                                        tint = expenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // 💳 1. MAIN TRANSACTION SUMMARY CARD
                    // ==========================================
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Category Avatar + Name + Type Pill + Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Avatar circle
                                val avatarBg = if (isIncome) incomeGreen.copy(alpha = 0.15f) else expenseRed.copy(alpha = 0.15f)
                                val avatarTint = if (isIncome) incomeGreen else expenseRed
                                val emoji = getCategoryEmoji(expense.category, categoryIcons)

                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (emoji.isNotBlank()) {
                                        Text(text = emoji, fontSize = 22.sp)
                                    } else {
                                        Icon(
                                            imageVector = getCategoryIcon(expense.category, categoryIcons),
                                            contentDescription = expense.category,
                                            tint = avatarTint,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = expense.category,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Badge Pill: INCOME / EXPENSE
                                        Surface(
                                            color = if (isIncome) incomeGreen.copy(alpha = 0.15f) else expenseRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isIncome) "INCOME" else "EXPENSE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIncome) incomeGreen else expenseRed,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SleekTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = cardBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Large Amount Text (+₹6.00 / -₹XX.XX)
                            val amountSign = if (isIncome) "+" else "-"
                            val amountColor = if (isIncome) incomeGreen else expenseRed
                            val formattedAmount = String.format(Locale.getDefault(), "%,.2f", expense.amount)

                            Text(
                                text = "$amountSign$currencySymbol$formattedAmount",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = amountColor,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Note / Description
                            val displayNote = if (!expense.note.isNullOrBlank()) expense.note else expense.category
                            Text(
                                text = displayNote,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SleekTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // "Edit Transaction Details" Button inside Card
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEditClick()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Edit Transaction Details",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ==========================================
                    // 📎 2. RECEIPT ATTACHMENT SECTION HEADER
                    // ==========================================
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = lightBlueButtonBg,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = AppIcons.AttachFile,
                                    contentDescription = null,
                                    tint = primaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Receipt Attachment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary,
                            fontSize = 16.sp
                        )
                    }

                    // ==========================================
                    // 🖼️ 3. RECEIPT ATTACHMENT CARD
                    // ==========================================
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val imgPath = currentImagePath
                            if (!imgPath.isNullOrBlank() && File(imgPath).exists()) {
                                // 🌟 Image Attached View
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                                            .background(cardBg)
                                            .clickable { showFullImageViewer = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = File(imgPath),
                                            contentDescription = "Attached Receipt",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Tap to expand indicator
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.Black.copy(alpha = 0.55f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = AppIcons.ZoomIn,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text("Zoom", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Receipt management button row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val file = File(imgPath)
                                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                                if (bitmap != null) {
                                                    activeBitmapForEdit = bitmap
                                                    showCropper = true
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, cardBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextPrimary)
                                        ) {
                                            Icon(AppIcons.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Crop / Rotate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        Button(
                                            onClick = {
                                                val file = File(imgPath)
                                                if (file.exists()) file.delete()
                                                val updated = expense.copy(imagePath = null)
                                                viewModel.updateExpense(updated)
                                                currentImagePath = null
                                                viewModel.refreshUsageData()
                                                Toast.makeText(context, "Receipt removed", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = expenseRed.copy(alpha = 0.15f), contentColor = expenseRed)
                                        ) {
                                            Icon(AppIcons.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // 🌟 No Receipt Attached (Matching user screenshot exactly)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(cardBg)
                                        .dashedBorder(
                                            color = cardBorder,
                                            strokeWidth = 1.5.dp,
                                            cornerRadius = 18.dp
                                        )
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Custom Receipt Graphic
                                        ReceiptIllustration()

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = "No Receipt Image Attached",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekTextPrimary
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Attach a receipt image for your records",
                                            fontSize = 13.sp,
                                            color = SleekTextSecondary,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Camera & Gallery Buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val suppressAutoLock = LocalAutoLockSuppressor.current
                                            // Camera Button (Dark Blue)
                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    suppressAutoLock()
                                                    cameraLauncher.launch()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = darkBlueButtonBg),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Icon(
                                                    imageVector = AppIcons.PhotoCamera,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Camera",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }

                                            // Gallery Button (Light Blue)
                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    suppressAutoLock()
                                                    galleryLauncher.launch("image/*")
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(46.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = lightBlueButtonBg),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Icon(
                                                    imageVector = AppIcons.PhotoLibrary,
                                                    contentDescription = null,
                                                    tint = lightBlueButtonText,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Gallery",
                                                    color = lightBlueButtonText,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ==========================================
                // 📌 4. STICKY BOTTOM "EDIT TRANSACTION DETAILS" BUTTON
                // ==========================================
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    color = Color.Transparent
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEditClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = primaryBlue.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Transaction Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

        // ==========================================
        // 🗑️ DELETE CONFIRMATION DIALOG
        // ==========================================
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Text(
                        text = "Delete Transaction?",
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this $currencySymbol${String.format(Locale.getDefault(), "%,.2f", expense.amount)} ${expense.category} record? This action cannot be undone.",
                        color = SleekTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteConfirmDialog = false
                            onDeleteClick()
                            Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = expenseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmDialog = false }
                    ) {
                        Text("Cancel", color = SleekTextSecondary)
                    }
                },
                containerColor = cardBg,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // ==========================================
        // 🔍 FULL IMAGE VIEWER DIALOG
        // ==========================================
        if (showFullImageViewer && !currentImagePath.isNullOrBlank()) {
            Dialog(
                onDismissRequest = { showFullImageViewer = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { showFullImageViewer = false },
                    contentAlignment = Alignment.Center
                ) {
                    val fullImg = currentImagePath
                    if (!fullImg.isNullOrBlank()) {
                        AsyncImage(
                            model = File(fullImg),
                            contentDescription = "Full Receipt",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    IconButton(
                        onClick = { showFullImageViewer = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(AppIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        // ==========================================
        // ✂️ IMAGE EDITOR / CROPPER
        // ==========================================
        val activeBmp = activeBitmapForEdit
        if (showCropper && activeBmp != null) {
            ImageEditDialog(
                initialBitmap = activeBmp,
                onDismiss = { showCropper = false },
                onSave = { savedPath ->
                    showCropper = false
                    currentImagePath = savedPath
                    val updated = expense.copy(imagePath = savedPath)
                    viewModel.updateExpense(updated)
                    viewModel.refreshUsageData()
                    Toast.makeText(context, "Receipt attached successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }
      }
    }
}

/**
 * Built-in Image Edit Dialog with rotation and crop controls.
 */
@Composable
fun ImageEditDialog(
    initialBitmap: Bitmap,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var currentBitmap by remember { mutableStateOf(initialBitmap) }
    var cropFactor by remember { mutableStateOf(0.0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111215)),
            color = Color(0xFF111215)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(AppIcons.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Text(
                        text = "Receipt Photo Editor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = {
                            var finalBitmap = currentBitmap
                            if (cropFactor > 0.05f) {
                                val xSize = (finalBitmap.width * (1f - cropFactor * 2)).toInt().coerceIn(10, finalBitmap.width)
                                val ySize = (finalBitmap.height * (1f - cropFactor * 2)).toInt().coerceIn(10, finalBitmap.height)
                                val startX = ((finalBitmap.width - xSize) / 2).coerceIn(0, finalBitmap.width - 1)
                                val startY = ((finalBitmap.height - ySize) / 2).coerceIn(0, finalBitmap.height - 1)
                                try {
                                    finalBitmap = Bitmap.createBitmap(finalBitmap, startX, startY, xSize, ySize)
                                } catch (e: Exception) {
                                    android.util.Log.e("ReceiptCrop", "Failed to crop receipt bitmap", e)
                                }
                            }

                            try {
                                val receiptDir = File(context.filesDir, "receipts").apply {
                                    if (!exists()) mkdirs()
                                }
                                val file = File(receiptDir, "receipt_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(file).use { out ->
                                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                }
                                onSave(file.absolutePath)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error saving receipt: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(AppIcons.Check, contentDescription = "Apply", tint = Color(0xFF10B981))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Canvas/Image Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF2C2F36), RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Editing Canvas",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (cropFactor > 0.05f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(1f - cropFactor * 2)
                                .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controls
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1D21)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2F36)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Adjustment Tools",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Rotate control
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(AppIcons.RotateRight, contentDescription = null, tint = Color.White)
                                Text("Rotation angle", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val matrix = Matrix().apply { postRotate(90f) }
                                    currentBitmap = Bitmap.createBitmap(
                                        currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2F36)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Rotate 90°", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFF2C2F36))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Crop control slider
                        Text(
                            text = "Crop Bounds Center Inset",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = cropFactor,
                            onValueChange = { cropFactor = it },
                            valueRange = 0f..0.45f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0xFF2C2F36)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Full Frame", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("Tight Center Crop", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
