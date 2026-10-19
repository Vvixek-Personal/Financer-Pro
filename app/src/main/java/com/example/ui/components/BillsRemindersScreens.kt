package com.example.ui

import com.example.ui.theme.*
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReminderEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fullscreen view for reviewing, adding, editing, and paying upcoming or recurring bills.
 */
@Composable
fun BillsFullScreen(
    viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit,
    onPayBill: (String, Double) -> Unit
) {
    val context = LocalContext.current
    val billsList = viewModel.billsList
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val totalBalance = remember(accounts) { accounts.sumOf { it.balance } }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<BillEntry?>(null) }
    var billTitle by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var billDueDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var billErrorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler { onBack() }

    if (showAddEditDialog) {
        Dialog(onDismissRequest = { showAddEditDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (editingBill == null) "Add New Bill" else "Edit Bill",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )

                    OutlinedTextField(
                        value = billTitle,
                        onValueChange = { billTitle = it },
                        label = { Text("Bill Title", color = SleekTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = billAmount,
                        onValueChange = { billAmount = it },
                        label = { Text("Amount", color = SleekTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    billDueDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.DateRange, contentDescription = null, tint = SleekPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Due Date: $billDueDate", color = SleekTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddEditDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = SleekTextSecondary)
                        }

                        Button(
                            onClick = {
                                val amt = billAmount.toDoubleOrNull() ?: 0.0
                                if (billTitle.isNotBlank() && amt > 0) {
                                    val currentEditing = editingBill
                                    if (currentEditing == null) {
                                        billsList.add(
                                            BillEntry(
                                                id = UUID.randomUUID().toString(),
                                                title = billTitle.trim(),
                                                amount = amt,
                                                dueDate = billDueDate
                                            )
                                        )
                                    } else {
                                        val idx = billsList.indexOf(currentEditing)
                                        if (idx != -1) {
                                            billsList[idx] = currentEditing.copy(
                                                title = billTitle.trim(),
                                                amount = amt,
                                                dueDate = billDueDate
                                            )
                                        }
                                    }
                                    showAddEditDialog = false
                                }
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

    if (billErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { billErrorMessage = null },
            icon = { Icon(AppIcons.Warning, contentDescription = null, tint = Color(0xFFEF4444)) },
            title = { Text("Payment Blocked", fontWeight = FontWeight.Bold, color = SleekTextPrimary) },
            text = { Text(billErrorMessage ?: "", color = SleekTextSecondary) },
            confirmButton = {
                TextButton(onClick = { billErrorMessage = null }) {
                    Text("Understood", color = SleekPrimary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = SleekSurface
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SleekSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back", tint = SleekTextPrimary)
                    }
                    Text(
                        text = "Upcoming Bills",
                        style = MaterialTheme.typography.titleLarge,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = {
                        editingBill = null
                        billTitle = ""
                        billAmount = ""
                        billDueDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        showAddEditDialog = true
                    }
                ) {
                    Icon(AppIcons.Add, contentDescription = "Add Bill", tint = SleekPrimary)
                }
            }

            if (billsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(AppIcons.ReceiptLong, contentDescription = null, tint = SleekTextSecondary, modifier = Modifier.size(48.dp))
                        Text("No bills to pay", fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = 16.sp)
                        Text("Tap + to add a recurring bill", color = SleekTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(billsList, key = { it.id }) { bill ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SleekBorder.copy(alpha = 0.25f))
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = AppIcons.Receipt,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bill.title, fontSize = 14.sp, color = SleekTextPrimary, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Due: ${bill.dueDate}", fontSize = 11.sp, color = SleekTextSecondary)
                                }

                                Text(
                                    text = String.format("₹%.0f", bill.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SleekTextPrimary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            if (bill.amount > totalBalance) {
                                                billErrorMessage = "Cannot pay ₹%.0f for %s: Amount exceeds available balance (₹%.0f available).".format(bill.amount, bill.title, totalBalance)
                                                return@FilledTonalButton
                                            }
                                            billsList.remove(bill)
                                            onPayBill(bill.title, bill.amount)
                                            Toast.makeText(context, "Paid ${bill.title}", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f), contentColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = {
                                            editingBill = bill
                                            billTitle = bill.title
                                            billAmount = bill.amount.toInt().toString()
                                            billDueDate = bill.dueDate
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(AppIcons.Edit, contentDescription = "Edit", tint = SleekPrimary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            billsList.remove(bill)
                                            Toast.makeText(context, "Deleted ${bill.title}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(AppIcons.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen view for managing reminders backed by Room database.
 */
@Composable
fun RemindersFullScreen(
    viewModel: FinanceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var reminderText by remember { mutableStateOf("") }
    var reminderDueDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }

    BackHandler { onBack() }

    if (showAddEditDialog) {
        Dialog(onDismissRequest = { showAddEditDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SleekSurface),
                border = BorderStroke(1.dp, SleekBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (editingReminder == null) "Add New Reminder" else "Edit Reminder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )

                    OutlinedTextField(
                        value = reminderText,
                        onValueChange = { reminderText = it },
                        label = { Text("Reminder Note", color = SleekTextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    reminderDueDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.DateRange, contentDescription = null, tint = SleekPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Due Date: $reminderDueDate", color = SleekTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddEditDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = SleekTextSecondary)
                        }

                        Button(
                            onClick = {
                                if (reminderText.isNotBlank()) {
                                    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val dueDateLong = try { df.parse(reminderDueDate)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }

                                    val currentEditing = editingReminder
                                    if (currentEditing == null) {
                                        viewModel.insertReminder(reminderText.trim(), dueDateLong, true)
                                    } else {
                                        viewModel.updateReminder(currentEditing.copy(text = reminderText.trim(), dueDate = dueDateLong))
                                    }
                                    showAddEditDialog = false
                                    Toast.makeText(context, "Saved reminder to Room DB!", Toast.LENGTH_SHORT).show()
                                }
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SleekSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back", tint = SleekTextPrimary)
                    }
                    Text(
                        text = "Payment Reminders",
                        style = MaterialTheme.typography.titleLarge,
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = {
                        editingReminder = null
                        reminderText = ""
                        reminderDueDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                        showAddEditDialog = true
                    }
                ) {
                    Icon(AppIcons.Add, contentDescription = "Add Reminder", tint = SleekPrimary)
                }
            }

            Text(
                text = "Room-backed reminders with push notifications & active state.",
                style = MaterialTheme.typography.bodySmall,
                color = SleekTextSecondary
            )

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(AppIcons.NotificationsNone, contentDescription = null, tint = SleekTextSecondary, modifier = Modifier.size(48.dp))
                        Text("No active reminders", fontWeight = FontWeight.Bold, color = SleekTextPrimary, fontSize = 16.sp)
                        Text("Tap + to add a reminder", color = SleekTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(reminders, key = { it.id }) { rem ->
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(rem.dueDate))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SleekBorder.copy(alpha = 0.25f))
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(AppIcons.NotificationsActive, contentDescription = null, tint = if (rem.isEnabled) Color(0xFFEC4899) else SleekTextSecondary, modifier = Modifier.size(22.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rem.text, fontSize = 14.sp, color = SleekTextPrimary, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Due: $dateStr • ${if (rem.isEnabled) "Active" else "Stopped"}", fontSize = 11.sp, color = if (rem.isEnabled) Color(0xFFEC4899) else SleekTextSecondary, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Switch(
                                        checked = rem.isEnabled,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateReminder(rem.copy(isEnabled = isChecked))
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFEC4899),
                                            checkedTrackColor = Color(0xFFEC4899).copy(alpha = 0.5f)
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.updateReminder(rem.copy(isCompleted = true, isEnabled = false))
                                            Toast.makeText(context, "Marked reminder as complete!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(AppIcons.CheckCircle, contentDescription = "Complete", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            editingReminder = rem
                                            reminderText = rem.text
                                            reminderDueDate = dateStr
                                            showAddEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(AppIcons.Edit, contentDescription = "Edit", tint = SleekPrimary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteReminder(rem)
                                            Toast.makeText(context, "Deleted reminder", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(AppIcons.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
