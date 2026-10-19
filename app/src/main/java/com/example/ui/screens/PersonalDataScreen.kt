package com.example.ui

import com.example.ui.theme.*
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R

/**
 * 0️⃣ PERSONAL DATA SCREEN
 * Allows managing user profile details, occupation, and avatar photo.
 */
@Composable
fun PersonalDataScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val currentName by viewModel.userName.collectAsStateWithLifecycle()
    val currentDob by viewModel.userDob.collectAsStateWithLifecycle()
    val currentJob by viewModel.userJob.collectAsStateWithLifecycle()
    val currentIncome by viewModel.userMonthlyIncome.collectAsStateWithLifecycle()
    val currentGender by viewModel.userGender.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.userProfileImageUri.collectAsStateWithLifecycle()

    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsStateWithLifecycle()
    val googleAccountEmail by viewModel.googleAccountEmail.collectAsStateWithLifecycle()
    val googleProfileName by viewModel.googleProfileName.collectAsStateWithLifecycle()
    val googleProfilePhotoUrl by viewModel.googleProfilePhotoUrl.collectAsStateWithLifecycle()

    var nameText by remember { mutableStateOf(currentName ?: "William John Malik") }
    LaunchedEffect(currentName) {
        currentName?.let {
            if (it.isNotBlank()) {
                nameText = it
            }
        }
    }
    var dobText by remember { mutableStateOf(currentDob) }
    var jobText by remember { mutableStateOf(currentJob) }
    var incomeText by remember { mutableStateOf(currentIncome) }
    var genderOption by remember { mutableStateOf(currentGender) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showPhotoOptionSheet by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateUserProfileImageFromUri(context, uri)
            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Personal Data", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Profile Picture Circle
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .clickable { showPhotoOptionSheet = true }
            ) {
                if (!profileImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_profile_avatar),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SleekPrimary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.CameraAlt,
                        contentDescription = "Edit photo",
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = jobText,
                onValueChange = { jobText = it },
                label = { Text("Occupation / Job") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dobText,
                onValueChange = { dobText = it },
                label = { Text("Date of Birth") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = incomeText,
                onValueChange = { incomeText = it },
                label = { Text("Income Range") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveUserProfile(nameText, dobText, jobText, incomeText, genderOption)
                    Toast.makeText(context, "Personal data saved successfully!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Badges & Milestones Section in Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = AppIcons.Star,
                    contentDescription = null,
                    tint = Color(0xFFEA580C),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Badges and Milestone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            ComingSoonConstructionBanner()

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showPhotoOptionSheet) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionSheet = false },
            title = { Text("Change Profile Photo") },
            text = { Text("Select photo from device gallery or remove current photo.") },
            confirmButton = {
                val suppressAutoLock = LocalAutoLockSuppressor.current
                TextButton(onClick = {
                    showPhotoOptionSheet = false
                    suppressAutoLock()
                    imagePickerLauncher.launch("image/*")
                }) { Text("Choose from Gallery") }
            },
            dismissButton = {
                Row {
                    if (!profileImageUri.isNullOrBlank()) {
                        TextButton(onClick = {
                            showPhotoOptionSheet = false
                            viewModel.removeUserProfileImage(context)
                            Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Remove", color = Color(0xFFDC2626))
                        }
                    }
                    TextButton(onClick = { showPhotoOptionSheet = false }) { Text("Cancel") }
                }
            }
        )
    }
}

/**
 * 0️⃣-B BADGES AND MILESTONES SCREEN
 */
@Composable
fun BadgesAndMilestonesScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Badges and Milestone", onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ComingSoonConstructionBanner()
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

/**
 * 🏗️ COMING SOON CONSTRUCTION BANNER
 */
@Composable
fun ComingSoonConstructionBanner(
    modifier: Modifier = Modifier
) {
    val swingAngle = rememberLoopFloat(-12f, 12f, 2200, "swing", reverse = true, rest = 0f)
    val craneHookOffsetY = rememberLoopFloat(0f, 14f, 1800, "craneY", reverse = true, rest = 0f)

    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.5.dp, SleekBorder),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Construction Graphic Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Background City Outline
                    val buildingColor = Color(0xFF334155)
                    val windowColor = Color(0xFF94A3B8).copy(alpha = 0.4f)

                    // Building 1
                    drawRect(
                        color = buildingColor,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.45f),
                        size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.55f)
                    )
                    for (r in 0..3) {
                        for (c in 0..1) {
                            drawRect(
                                color = windowColor,
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f + c * 20f, h * 0.5f + r * 22f),
                                size = androidx.compose.ui.geometry.Size(12f, 14f)
                            )
                        }
                    }

                    // Building 2
                    drawRect(
                        color = buildingColor,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.35f),
                        size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.65f)
                    )
                    for (r in 0..4) {
                        for (c in 0..1) {
                            drawRect(
                                color = windowColor,
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.28f + c * 24f, h * 0.4f + r * 22f),
                                size = androidx.compose.ui.geometry.Size(14f, 14f)
                            )
                        }
                    }

                    // Building 3 (Right building under construction)
                    drawRect(
                        color = buildingColor,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.5f),
                        size = androidx.compose.ui.geometry.Size(w * 0.23f, h * 0.5f)
                    )
                    val scaffoldColor = Color(0xFFF59E0B).copy(alpha = 0.6f)
                    drawLine(
                        color = scaffoldColor,
                        start = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.4f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.95f),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = scaffoldColor,
                        start = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.4f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.95f),
                        strokeWidth = 3f
                    )
                    for (yStep in 0..3) {
                        val yPos = h * 0.4f + yStep * 20f
                        drawLine(
                            color = scaffoldColor,
                            start = androidx.compose.ui.geometry.Offset(w * 0.72f, yPos),
                            end = androidx.compose.ui.geometry.Offset(w * 0.95f, yPos),
                            strokeWidth = 2f
                        )
                    }

                    // 2. Construction Crane
                    val craneColor = Color(0xFFF59E0B)
                    val craneX = w * 0.55f
                    val craneBaseY = h * 0.95f
                    val craneTopY = h * 0.15f

                    drawLine(
                        color = craneColor,
                        start = androidx.compose.ui.geometry.Offset(craneX - 6f, craneBaseY),
                        end = androidx.compose.ui.geometry.Offset(craneX - 6f, craneTopY),
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = craneColor,
                        start = androidx.compose.ui.geometry.Offset(craneX + 6f, craneBaseY),
                        end = androidx.compose.ui.geometry.Offset(craneX + 6f, craneTopY),
                        strokeWidth = 4f
                    )
                    for (step in 0..6) {
                        val y1 = craneTopY + step * 20f
                        val y2 = y1 + 20f
                        drawLine(craneColor, androidx.compose.ui.geometry.Offset(craneX - 6f, y1), androidx.compose.ui.geometry.Offset(craneX + 6f, y2), 2f)
                    }

                    // Horizontal Jib
                    val jibLeft = craneX - w * 0.2f
                    val jibRight = craneX + w * 0.35f
                    drawLine(
                        color = craneColor,
                        start = androidx.compose.ui.geometry.Offset(jibLeft, craneTopY),
                        end = androidx.compose.ui.geometry.Offset(jibRight, craneTopY),
                        strokeWidth = 5f
                    )
                    drawRect(
                        color = Color(0xFF64748B),
                        topLeft = androidx.compose.ui.geometry.Offset(jibLeft, craneTopY - 6f),
                        size = androidx.compose.ui.geometry.Size(24f, 16f)
                    )

                    // Cable & Swinging Hook
                    val cableHookX = craneX + w * 0.2f
                    val cableStartY = craneTopY
                    val cableLength = h * 0.35f + craneHookOffsetY

                    val swingRad = Math.toRadians(swingAngle.toDouble())
                    val endHookX = (cableHookX + Math.sin(swingRad) * cableLength).toFloat()
                    val endHookY = (cableStartY + Math.cos(swingRad) * cableLength).toFloat()

                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = androidx.compose.ui.geometry.Offset(cableHookX, cableStartY),
                        end = androidx.compose.ui.geometry.Offset(endHookX, endHookY),
                        strokeWidth = 2f
                    )

                    // Hook Badge
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = 12f,
                        center = androidx.compose.ui.geometry.Offset(endHookX, endHookY + 12f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = androidx.compose.ui.geometry.Offset(endHookX, endHookY + 12f)
                    )
                }

                // Overlay Text
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Construction,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Coming soon",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Star,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Badges and Milestone",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We are currently constructing your Badges and Milestones hub! Soon you will earn unlockable badges for daily savings streaks, financial milestones, and smart budgeting achievements.",
                fontSize = 13.sp,
                color = SleekTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
