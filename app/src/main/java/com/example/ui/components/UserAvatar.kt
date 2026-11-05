package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekPrimaryContainer
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.isDarkModeActive

/**
 * One avatar implementation for the whole app.
 *
 * Every screen previously drew the same stock photo (`ic_default_profile_avatar`) when the user had
 * not picked a picture, which read as somebody else's face. This renders the user's initials on a
 * theme-aware accent tint instead, with a soft accent ring, and falls back gracefully when a picture
 * is set.
 */
@Composable
fun UserAvatar(
    userName: String?,
    imageUri: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val initials = userInitials(userName)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(listOf(SleekPrimary, SleekPrimaryContainer, SleekPrimary))
            )
            .padding(2.dp)
            .clip(CircleShape)
            .background(SleekSurface),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = userName ?: "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(SleekPrimary.copy(alpha = if (isDarkModeActive) 0.24f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary
                )
            }
        }
    }
}

/** First letters of the first two words, uppercased, with a safe fallback. */
fun userInitials(userName: String?): String {
    val raw = userName?.trim().orEmpty()
    if (raw.isEmpty()) return "U"
    val parts = raw.split(" ", "  ").filter { it.isNotBlank() }
    val letters = parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    return letters.ifBlank { "U" }
}