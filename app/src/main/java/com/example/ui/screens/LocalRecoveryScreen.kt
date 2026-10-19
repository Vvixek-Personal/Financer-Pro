package com.example.ui

import com.example.ui.theme.*
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceDatabase
import com.example.data.LocalRecoveryManager
import com.example.data.SnapshotMeta
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocalRecoveryScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshots by remember { mutableStateOf(emptyList<SnapshotMeta>()) }
    var selectedSnapshotForRestore by remember { mutableStateOf<SnapshotMeta?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    fun loadSnapshots() {
        scope.launch {
            snapshots = LocalRecoveryManager.listSnapshots(context)
        }
    }

    LaunchedEffect(Unit) {
        loadSnapshots()
    }

    val lastSnapshot = snapshots.firstOrNull { it.isValid }
    val storageSize = snapshots.sumOf { it.fileSize }
    val storageSizeStr = if (storageSize > 1024 * 1024) {
        String.format(Locale.getDefault(), "%.2f MB", storageSize.toDouble() / (1024 * 1024))
    } else {
        String.format(Locale.getDefault(), "%.1f KB", storageSize.toDouble() / 1024)
    }

    val statusProtected = snapshots.isNotEmpty() && snapshots.all { it.isValid }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SettingsHeaderTitle("Local Recovery", onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Status Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = SleekShapes.lg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RECOVERY SYSTEM STATUS", fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = SleekSizes.textBodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Surface(
                                shape = SleekShapes.sm,
                                color = if (statusProtected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (statusProtected) "Protected" else "Needs Attention",
                                    color = if (statusProtected) Color(0xFF15803D) else Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = SleekSizes.textCaption,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        HorizontalDivider(color = SleekBorder)

                        RecoveryStatRow("Last Successful Snapshot", lastSnapshot?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it.createdAt)) } ?: "None")
                        RecoveryStatRow("Active Snapshots", "${snapshots.size} / 5 (Rolling History)")
                        RecoveryStatRow("Database Version", "8 (Schema V8)")
                        RecoveryStatRow("Recovery Storage", storageSizeStr)
                    }
                }
            }

            item {
                // Actions Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekBorder),
                    shape = SleekShapes.lg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("RECOVERY ACTIONS", fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = SleekSizes.textBodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)

                        Button(
                            onClick = {
                                scope.launch {
                                    isWorking = true
                                    val db = FinanceDatabase.getDatabase(context)
                                    val success = LocalRecoveryManager.createSnapshot(context, db)
                                    isWorking = false
                                    if (success) {
                                        Toast.makeText(context, "Recovery snapshot created successfully!", Toast.LENGTH_SHORT).show()
                                        loadSnapshots()
                                    } else {
                                        Toast.makeText(context, "Failed to create recovery snapshot.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isWorking,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = SleekSizes.buttonMedium),
                            shape = SleekShapes.md,
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Icon(AppIcons.Backup, contentDescription = null, modifier = Modifier.size(SleekSizes.iconSmall))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Recovery Snapshot", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = {
                                if (snapshots.isNotEmpty()) {
                                    selectedSnapshotForRestore = snapshots.firstOrNull { it.isValid } ?: snapshots.first()
                                    showRestoreConfirmDialog = true
                                } else {
                                    Toast.makeText(context, "No recovery snapshots available.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isWorking && snapshots.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = SleekSizes.buttonMedium),
                            shape = SleekShapes.md
                        ) {
                            Icon(AppIcons.Restore, contentDescription = null, modifier = Modifier.size(SleekSizes.iconSmall))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Latest Snapshot", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            item {
                Text("RECOVERY SNAPSHOTS HISTORY", fontWeight = FontWeight.Bold, color = SleekTextSecondary, fontSize = SleekSizes.textBodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (snapshots.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No local recovery snapshots found.", color = SleekTextSecondary)
                    }
                }
            } else {
                items(snapshots, key = { it.fileName }) { snapshot ->
                    SnapshotItemCard(
                        snapshot = snapshot,
                        onRestore = {
                            selectedSnapshotForRestore = snapshot
                            showRestoreConfirmDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                LocalRecoveryManager.deleteSnapshot(context, snapshot.fileName)
                                loadSnapshots()
                                Toast.makeText(context, "Snapshot deleted.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    val snap = selectedSnapshotForRestore
    if (showRestoreConfirmDialog && snap != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore Recovery Snapshot?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(snap.createdAt))}")
                    Text("Accounts: ${snap.accountsCount}")
                    Text("Expenses: ${snap.expensesCount}")
                    Text("Budgets: ${snap.budgetsCount}")
                    Text("Savings Goals: ${snap.savingsGoalsCount}")
                    Text("Reminders: ${snap.remindersCount}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "WARNING: Restoring will replace current financial data with this snapshot. A safety emergency copy of your current data will be created automatically before restoration.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        scope.launch {
                            isWorking = true
                            val db = FinanceDatabase.getDatabase(context)
                            val success = LocalRecoveryManager.restoreSnapshot(context, db, snap.fileName) { code, symbol, name, budget ->
                                viewModel.updateDefaultCurrency(code, symbol, name, false)
                                viewModel.updateMonthlyBudget(budget)
                            }
                            isWorking = false
                            if (success) {
                                viewModel.refreshUsageData()
                                Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_LONG).show()
                                loadSnapshots()
                            } else {
                                Toast.makeText(context, "Restoration failed or checksum rejected.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Create Safety Copy & Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecoveryStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SleekTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = SleekTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SnapshotItemCard(
    snapshot: SnapshotMeta,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = SleekShapes.md,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(snapshot.createdAt)),
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary,
                    fontSize = SleekSizes.textBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = SleekShapes.xs,
                    color = if (snapshot.isValid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = if (snapshot.isValid) "Valid Integrity" else "Corrupted",
                        color = if (snapshot.isValid) Color(0xFF15803D) else Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        fontSize = SleekSizes.textMicro,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "${snapshot.accountsCount} accounts • ${snapshot.expensesCount} expenses • ${snapshot.budgetsCount} budgets • ${snapshot.savingsGoalsCount} goals • ${snapshot.remindersCount} reminders",
                style = MaterialTheme.typography.bodySmall,
                color = SleekTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(AppIcons.Delete, contentDescription = null, modifier = Modifier.size(SleekSizes.iconMicro))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = SleekSizes.textBodySmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRestore,
                    enabled = snapshot.isValid,
                    shape = SleekShapes.sm,
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(AppIcons.Restore, contentDescription = null, modifier = Modifier.size(SleekSizes.iconMicro))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = SleekSizes.textBodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
