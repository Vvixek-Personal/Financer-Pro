package com.example.data

import com.example.data.Money

/**
 * Extension functions for Expense calculations
 */

fun List<Expense>.realExpense(currencyCode: String): Double {
    return this.filter { it.type != "INCOME" && it.kind != "TRANSFER_IN" }
        .sumOf { exp ->
            val converted = Money.convertWithFallback(exp.amountMinor, exp.currencyCode, currencyCode)
            Money.toDouble(converted, currencyCode)
        }
}

fun List<Expense>.realIncome(currencyCode: String): Double {
    return this.filter { it.type == "INCOME" || it.kind == "TRANSFER_IN" }
        .sumOf { exp ->
            val converted = Money.convertWithFallback(exp.amountMinor, exp.currencyCode, currencyCode)
            Money.toDouble(converted, currencyCode)
        }
}
