package com.example.ui

import com.example.ui.theme.*
import android.content.Context
import android.util.Log
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.material.icons.filled.Fingerprint
import com.example.data.AppSettingsManager
import com.example.data.PinSecurityUtils

private const val TAG = "PinSecurityComponents"

/**
 * Full Screen 4-Digit PIN Lock Screen.
 * Prevents app access when locked until the correct 4-digit PIN is entered.
 */
@Composable
fun PinLockScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val routeToLocalRecovery by viewModel.routeToLocalRecovery.collectAsStateWithLifecycle()
    if (routeToLocalRecovery) {
        LocalRecoveryScreen(
            viewModel = viewModel,
            onBack = { viewModel.resetRouteToLocalRecovery() }
        )
        return
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appSettingsState by AppSettingsManager.getInstance(context).state.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val lockoutRemaining = (appSettingsState.pinLockoutUntil - now).coerceAtLeast(0L)
    val isLockedOut = lockoutRemaining > 0L

    var remainingSecs by remember { mutableStateOf(lockoutRemaining / 1000) }
    LaunchedEffect(appSettingsState.pinLockoutUntil) {
        while (true) {
            val curNow = System.currentTimeMillis()
            val rem = (appSettingsState.pinLockoutUntil - curNow).coerceAtLeast(0L)
            remainingSecs = rem / 1000
            if (rem <= 0) break
            kotlinx.coroutines.delay(1000L)
        }
    }

    val activity = context as? FragmentActivity
    val executor = ContextCompat.getMainExecutor(context)
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticateBiometric = remember(biometricManager) {
        try {
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    var enteredPin by remember { mutableStateOf("") }
    var isErrorState by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val biometricPrompt = remember {
        activity?.let { act ->
            BiometricPrompt(act, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockWithBiometrics()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    android.util.Log.w("BiometricAuth", "Biometric error ($errorCode): $errString")
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED) {
                        errorMessage = errString.toString()
                        isErrorState = true
                    }
                }
            })
        }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Finance App")
            .setSubtitle("Authenticate to access your financial data")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Use PIN")
            .build()
    }

    LaunchedEffect(Unit) {
        if (appSettingsState.biometricEnabled && canAuthenticateBiometric && !isLockedOut) {
            try {
                biometricPrompt?.authenticate(promptInfo)
            } catch (e: Exception) {
                android.util.Log.e("BiometricAuth", "Failed to launch BiometricPrompt", e)
            }
        }
    }

    val vibrateOnError = {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate device on PIN error", e)
        }
    }

    LaunchedEffect(enteredPin) {
        if (!isLockedOut && enteredPin.length == 4) {
            when (val result = viewModel.unlockAppWithPin(enteredPin)) {
                PinSecurityUtils.PinVerifyResult.Success -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    enteredPin = ""
                    isErrorState = false
                    errorMessage = ""
                }
                PinSecurityUtils.PinVerifyResult.KeystoreKeyCorrupted -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    enteredPin = ""
                    isErrorState = true
                    errorMessage = "Security key missing/invalid. Routing to Local Recovery..."
                }
                PinSecurityUtils.PinVerifyResult.Failed -> {
                    vibrateOnError()
                    isErrorState = true
                    errorMessage = "Incorrect Passcode. Please try again."
                    enteredPin = ""
                }
            }
        } else if (enteredPin.isNotEmpty()) {
            isErrorState = false
            errorMessage = ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SleekBg,
                        SleekSurface,
                        SleekBg
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 400.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Brand Logo & Lock Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SleekPrimary.copy(alpha = 0.12f))
                        .border(BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.45f)), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo_modern),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Small lock badge at bottom right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(SleekPrimary)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Lock,
                            contentDescription = "App Locked",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Text(
                    text = "Welcome Back",
                    fontSize = SleekSizes.textHeadline,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = when {
                        isLockedOut -> "Too many incorrect attempts. Try again in ${remainingSecs}s."
                        isErrorState -> errorMessage
                        else -> "Enter 4-digit PIN code to unlock"
                    },
                    fontSize = SleekSizes.textBody,
                    fontWeight = FontWeight.Medium,
                    color = if (isErrorState || isLockedOut) ExpenseRed else SleekTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 4 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.25f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dotScale"
                        )

                        Box(
                            modifier = Modifier
                                .scale(dotScale)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isErrorState -> Color(0xFFEF4444)
                                        isFilled -> SleekPrimary
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = when {
                                        isErrorState -> Color(0xFFEF4444)
                                        isFilled -> SleekPrimary
                                        else -> Color.Gray.copy(alpha = 0.5f)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Numeric Keypad
            PinKeypad(
                onDigitClick = { digit ->
                    if (!isLockedOut && enteredPin.length < 4) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        enteredPin += digit
                    }
                },
                onBackspaceClick = {
                    if (!isLockedOut && enteredPin.isNotEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onClearClick = {
                    if (!isLockedOut) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        enteredPin = ""
                    }
                }
            )

            if (appSettingsState.biometricEnabled && canAuthenticateBiometric && !isLockedOut) {
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = {
                        try {
                            biometricPrompt?.authenticate(promptInfo)
                        } catch (e: Exception) {
                            android.util.Log.e("BiometricAuth", "Failed to launch BiometricPrompt", e)
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Fingerprint,
                        contentDescription = "Unlock with Biometrics",
                        tint = SleekPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            var showForgotPinDialog by remember { mutableStateOf(false) }

            TextButton(
                onClick = { showForgotPinDialog = true },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Forgot PIN? Reset Lock",
                    color = SleekTextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showForgotPinDialog) {
                AlertDialog(
                    onDismissRequest = { showForgotPinDialog = false },
                    title = {
                        Text("Reset App Lock & Erase Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    },
                    text = {
                        Text(
                            "To protect your financial privacy, the app lock cannot be bypassed without the correct PIN.\n\n" +
                            "Resetting the app lock will permanently erase all local accounts, transactions, expenses, budgets, and savings goals.\n\n" +
                            "Are you sure you want to proceed?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekTextPrimary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showForgotPinDialog = false
                                viewModel.resetAppLockAndWipeData()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Erase Everything & Unlock", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showForgotPinDialog = false }) {
                            Text("Cancel", color = SleekTextSecondary)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Standard 0-9 Tactile Numeric Keypad.
 */
@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keypadGrid = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "DEL")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keypadGrid.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { item ->
                    KeypadButton(
                        item = item,
                        onClick = {
                            when (item) {
                                "C" -> onClearClick()
                                "DEL" -> onBackspaceClick()
                                else -> onDigitClick(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    item: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (item == "C" || item == "DEL") Color.Transparent
                else SleekSurface
            )
            .border(
                width = if (item == "C" || item == "DEL") 0.dp else 1.dp,
                color = SleekBorder,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            "DEL" -> Icon(
                imageVector = AppIcons.Backspace,
                contentDescription = "Delete",
                tint = SleekTextSecondary,
                modifier = Modifier.size(SleekSizes.iconLarge)
            )
            "C" -> Text(
                text = "CLEAR",
                fontSize = SleekSizes.textBodySmall,
                fontWeight = FontWeight.Bold,
                color = SleekTextSecondary
            )
            else -> Text(
                text = item,
                fontSize = SleekSizes.textHeadline,
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            )
        }
    }
}

/**
 * First-Run Optional Passcode Setup Dialog.
 * Appears once on app first launch if passcode hasn't been set or explicitly skipped.
 */
@Composable
fun FirstRunPinSetupDialog(
    onSetPin: (String) -> Unit,
    onMaybeLater: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Enter, 2: Confirm
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val currentInput = if (step == 1) firstPin else confirmPin

    Dialog(
        onDismissRequest = onMaybeLater,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = SleekShapes.xxl,
            color = SleekSurface,
            border = BorderStroke(1.dp, SleekBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF7ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = if (step == 1) "Protect Your App" else "Confirm Passcode",
                    fontSize = SleekSizes.textTitleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (errorText.isNotEmpty()) errorText
                    else if (step == 1) "Set a 4-digit PIN to secure your financial records."
                    else "Re-enter your 4-digit PIN to confirm.",
                    fontSize = SleekSizes.textBodyMedium,
                    color = if (errorText.isNotEmpty()) ExpenseRed else SleekTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 4 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < currentInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) Color(0xFF10B981) else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Mini Keypad
                PinKeypad(
                    onDigitClick = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (step == 1) {
                            if (firstPin.length < 4) {
                                firstPin += digit
                                errorText = ""
                                if (firstPin.length == 4) {
                                    step = 2
                                }
                            }
                        } else {
                            if (confirmPin.length < 4) {
                                confirmPin += digit
                                errorText = ""
                                if (confirmPin.length == 4) {
                                    if (confirmPin == firstPin) {
                                        onSetPin(firstPin)
                                    } else {
                                        errorText = "Passcodes do not match. Try again."
                                        confirmPin = ""
                                        step = 1
                                        firstPin = ""
                                    }
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (step == 1 && firstPin.isNotEmpty()) {
                            firstPin = firstPin.dropLast(1)
                        } else if (step == 2 && confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                        }
                    },
                    onClearClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (step == 1) firstPin = "" else confirmPin = ""
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onMaybeLater) {
                        Text("Maybe later", color = SleekTextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    if (step == 2) {
                        TextButton(onClick = {
                            step = 1
                            firstPin = ""
                            confirmPin = ""
                            errorText = ""
                        }) {
                            Text("Reset", color = Color(0xFFF97316), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Change or Setup/Remove Passcode Security Dialog.
 */
@Composable
fun ChangePinDialog(
    currentAppPin: String?,
    onDismiss: () -> Unit,
    onSavePin: (String?) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Steps:
    // If currentAppPin exists: 1: Current PIN, 2: New PIN, 3: Confirm New PIN
    // If currentAppPin == null: 2: New PIN, 3: Confirm New PIN
    var step by remember { mutableIntStateOf(if (currentAppPin != null) 1 else 2) }

    var enteredCurrent by remember { mutableStateOf("") }
    var enteredNew by remember { mutableStateOf("") }
    var enteredConfirm by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    val activeInput = when (step) {
        1 -> enteredCurrent
        2 -> enteredNew
        else -> enteredConfirm
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = SleekShapes.xxl,
            color = SleekSurface,
            border = BorderStroke(1.dp, SleekBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentAppPin != null) "Change Passcode" else "Setup Passcode",
                        fontSize = SleekSizes.textTitle,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(AppIcons.Close, contentDescription = "Close", tint = SleekTextSecondary)
                    }
                }

                Text(
                    text = if (errorText.isNotEmpty()) errorText
                    else when (step) {
                        1 -> "Enter your current 4-digit passcode."
                        2 -> "Enter your new 4-digit passcode."
                        else -> "Re-enter your new 4-digit passcode to confirm."
                    },
                    fontSize = SleekSizes.textBodyMedium,
                    color = if (errorText.isNotEmpty()) ExpenseRed else SleekTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 4 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < activeInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) Color(0xFF10B981) else Color.Transparent)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Keypad
                PinKeypad(
                    onDigitClick = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (step) {
                            1 -> {
                                if (enteredCurrent.length < 4) {
                                    enteredCurrent += digit
                                    errorText = ""
                                    if (enteredCurrent.length == 4) {
                                        when (PinSecurityUtils.verifyPin(enteredCurrent, currentAppPin ?: "")) {
                                            PinSecurityUtils.PinVerifyResult.Success -> {
                                                step = 2
                                            }
                                            PinSecurityUtils.PinVerifyResult.KeystoreKeyCorrupted -> {
                                                errorText = "Security key missing/invalid. Use Local Recovery to reset."
                                                enteredCurrent = ""
                                            }
                                            PinSecurityUtils.PinVerifyResult.Failed -> {
                                                errorText = "Incorrect current passcode."
                                                enteredCurrent = ""
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                if (enteredNew.length < 4) {
                                    enteredNew += digit
                                    errorText = ""
                                    if (enteredNew.length == 4) {
                                        step = 3
                                    }
                                }
                            }
                            3 -> {
                                if (enteredConfirm.length < 4) {
                                    enteredConfirm += digit
                                    errorText = ""
                                    if (enteredConfirm.length == 4) {
                                        if (enteredConfirm == enteredNew) {
                                            onSavePin(enteredNew)
                                            Toast.makeText(context, "Passcode updated successfully!", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            errorText = "Passcodes do not match. Try again."
                                            enteredConfirm = ""
                                            step = 2
                                            enteredNew = ""
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (step) {
                            1 -> if (enteredCurrent.isNotEmpty()) enteredCurrent = enteredCurrent.dropLast(1)
                            2 -> if (enteredNew.isNotEmpty()) enteredNew = enteredNew.dropLast(1)
                            3 -> if (enteredConfirm.isNotEmpty()) enteredConfirm = enteredConfirm.dropLast(1)
                        }
                    },
                    onClearClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (step) {
                            1 -> enteredCurrent = ""
                            2 -> enteredNew = ""
                            3 -> enteredConfirm = ""
                        }
                    }
                )

                // Remove Passcode option if currentAppPin exists
                if (currentAppPin != null && step == 1) {
                    TextButton(
                        onClick = {
                            when (PinSecurityUtils.verifyPin(enteredCurrent, currentAppPin ?: "")) {
                                PinSecurityUtils.PinVerifyResult.Success -> {
                                    onSavePin(null)
                                    Toast.makeText(context, "Passcode removed.", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                PinSecurityUtils.PinVerifyResult.KeystoreKeyCorrupted -> {
                                    errorText = "Security key missing/invalid. Reset via Local Recovery."
                                }
                                PinSecurityUtils.PinVerifyResult.Failed -> {
                                    errorText = "Type current passcode first to remove."
                                }
                            }
                        }
                    ) {
                        Text(
                            text = "Turn Off Passcode Lock",
                            color = Color(0xFFEF4444),
                            fontSize = SleekSizes.textBodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
