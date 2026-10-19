package com.example.ui

import com.example.ui.theme.*
import com.example.data.Expense
import com.example.data.Money
import com.example.data.SavingsGoal

/**
 * Core Financial Logic Extensions for Unified Ledger (Issue #9)
 * - Excludes SAVINGS_LOCK and SAVINGS_RELEASE from real income and real expense metrics to prevent double counting.
 * - Supports multi-currency normalization via Money.convert.
 */

// Revenue: True external income in minor units (INCOME kind or EXPENSE type == INCOME)
fun Iterable<Expense>.realIncomeMinor(targetCurrencyCode: String): Long {
    var sum = 0L
    for (expense in this) {
        if (expense.kind != "SAVINGS_LOCK" && expense.kind != "SAVINGS_RELEASE") {
            val isIncome = expense.type == "INCOME" || expense.kind == "INCOME"
            if (isIncome) {
                val converted = Money.convert(expense.amountMinor, expense.currencyCode, targetCurrencyCode)
                sum = Money.add(sum, converted)
            }
        }
    }
    return sum
}

fun Iterable<Expense>.realIncome(targetCurrencyCode: String): Double {
    return Money.toDouble(realIncomeMinor(targetCurrencyCode), targetCurrencyCode)
}

// Spend: True external expense in minor units
fun Iterable<Expense>.realExpenseMinor(targetCurrencyCode: String): Long {
    var sum = 0L
    for (expense in this) {
        if (expense.kind != "SAVINGS_LOCK" && expense.kind != "SAVINGS_RELEASE") {
            val isExpense = expense.type != "INCOME" && expense.kind != "INCOME"
            if (isExpense) {
                val converted = Money.convert(expense.amountMinor, expense.currencyCode, targetCurrencyCode)
                sum = Money.add(sum, converted)
            }
        }
    }
    return sum
}

fun Iterable<Expense>.realExpense(targetCurrencyCode: String): Double {
    return Money.toDouble(realExpenseMinor(targetCurrencyCode), targetCurrencyCode)
}

// Cash Flow (Available Cash) across all accounts in minor units
fun Iterable<Expense>.availableCashMinor(targetCurrencyCode: String): Long {
    var totalIn = 0L
    var totalOut = 0L
    for (expense in this) {
        val converted = Money.convert(expense.amountMinor, expense.currencyCode, targetCurrencyCode)
        val isIncome = expense.type == "INCOME" || expense.kind == "INCOME" || expense.kind == "SAVINGS_RELEASE"
        if (isIncome) {
            totalIn = Money.add(totalIn, converted)
        } else {
            totalOut = Money.add(totalOut, converted)
        }
    }
    return Money.subtract(totalIn, totalOut)
}

fun Iterable<Expense>.availableCash(targetCurrencyCode: String): Double {
    return Money.toDouble(availableCashMinor(targetCurrencyCode), targetCurrencyCode)
}

// Total Savings Balance in minor units
fun Iterable<SavingsGoal>.totalSavingsMinor(targetCurrencyCode: String = "INR"): Long {
    var sum = 0L
    for (goal in this) {
        val converted = Money.convert(goal.currentAmountMinor, goal.currencyCode, targetCurrencyCode)
        sum = Money.add(sum, converted)
    }
    return sum
}

fun Iterable<SavingsGoal>.totalSavings(targetCurrencyCode: String = "INR"): Double {
    return Money.toDouble(totalSavingsMinor(targetCurrencyCode), targetCurrencyCode)
}

// Net Worth
fun Iterable<Expense>.netWorth(savingsGoals: Iterable<SavingsGoal>, targetCurrencyCode: String): Double {
    val cashMinor = this.availableCashMinor(targetCurrencyCode)
    val savingsMinor = savingsGoals.totalSavingsMinor(targetCurrencyCode)
    val netMinor = Money.add(cashMinor, savingsMinor)
    return Money.toDouble(netMinor, targetCurrencyCode)
}
