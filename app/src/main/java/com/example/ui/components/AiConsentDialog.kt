package com.example.ui

import com.example.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AiConsentDialog(
    onConsentAccepted: () -> Unit,
    onConsentDeclined: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = AppIcons.AutoAwesome,
                    contentDescription = "AI",
                    tint = SleekPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "AI Financial Advisor Consent",
                    fontWeight = FontWeight.Bold,
                    fontSize = SleekSizes.textTitle,
                    color = SleekTextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "To provide personalized financial advice, this feature transmits your question and high-level monthly summaries to Google servers (Gemini via Firebase):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekTextPrimary,
                    lineHeight = 20.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant.copy(alpha = 0.6f)),
                    shape = SleekShapes.md
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "✓ What is sent: Aggregated monthly income, total expenses, net balance, top spending category, and category names.",
                            fontSize = SleekSizes.textBodySmall,
                            color = SleekTextPrimary,
                            lineHeight = 16.sp
                        )
                        HorizontalDivider(color = SleekBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "✗ What is NEVER sent: Transaction notes, merchant names, individual account numbers, or personal credentials.",
                            fontSize = SleekSizes.textBodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SleekPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "You can enable or disable AI features at any time in Settings > Privacy & Security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SleekTextSecondary,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConsentAccepted,
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                shape = SleekShapes.md,
                modifier = Modifier.heightIn(min = SleekSizes.buttonSmall)
            ) {
                Text(
                    text = "Allow & Continue",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onConsentDeclined,
                shape = SleekShapes.md,
                modifier = Modifier.heightIn(min = SleekSizes.buttonSmall)
            ) {
                Text(
                    text = "Decline",
                    color = SleekTextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        },
        shape = SleekShapes.xl,
        containerColor = SleekSurface
    )
}
