package com.example.ui.models

import com.example.data.Money

/**
 * Data models for entries used in the FinanceViewModel
 */

/**
 * Represents a bill entry with amount and due date
 */
data class BillEntry(
    val id: String,
    val title: String,
    val amountMinor: Long = 0L,
    val dueDate: String,
    val amount: Double = Money.toDouble(amountMinor, "INR")
) {
    constructor(id: String, title: String, amount: Double, dueDate: String) : this(
        id = id,
        title = title,
        amountMinor = Money.fromDouble(amount, "INR"),
        dueDate = dueDate,
        amount = amount
    )
}

/**
 * Represents a reminder entry
 */
data class ReminderEntry(
    val id: String,
    val text: String,
    val dueDate: String,
    val isCompleted: Boolean = false,
    val isEnabled: Boolean = true
)
