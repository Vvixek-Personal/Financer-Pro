package com.example.ui

import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// NOTE: The shared-element scaffolding that used to live here (LocalSharedTransitionScope,
// LocalNavAnimatedVisibilityScope, Modifier.sharedExpenseBounds / sharedExpenseElement and the
// expenseItemEnter/ExitTransition helpers) was never wired into any screen, so it was removed
// rather than left as a trap for future readers. ExpenseUndoSnackbar below is still in use.

@Composable
fun ExpenseUndoSnackbar(
    visible: Boolean,
    message: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(tween(250)),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = SleekShapes.xl,
            color = Color(0xFF1E222D),
            border = BorderStroke(1.dp, Color(0xFF333846)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .shadow(12.dp, SleekShapes.xl)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onUndo,
                        colors = ButtonDefaults.textButtonColors(contentColor = SleekPrimary)
                    ) {
                        Icon(
                            imageVector = AppIcons.Undo,
                            contentDescription = "Undo",
                            modifier = Modifier.size(SleekSizes.iconSmall),
                            tint = SleekPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "UNDO",
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary,
                            fontSize = SleekSizes.textBodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
