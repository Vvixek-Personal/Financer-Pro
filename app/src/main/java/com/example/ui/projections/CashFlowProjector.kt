package com.example.ui.projections

import com.example.data.Money
import com.example.data.Expense
import com.example.data.Account
import com.example.data.realExpense
import com.example.ui.models.BillEntry

/**
 * Projects cash flow based on current accounts, expenses, and bills
 */
class CashFlowProjector {

    data class CashFlowDayProjection(
        val dayTimestamp: Long,
        val dayLabel: String,
        val dayOfWeek: String,
        val projectedBalance: Double,
        val plannedIncome: Double,
        val plannedBills: Double,
        val isDangerZone: Boolean,
        val billNames: List<String> = emptyList()
    )

    fun projectCashFlow(
        allAccounts: List<Account>,
        allExpenses: List<Expense>,
        allBills: List<BillEntry>,
        currencyCode: String,
        daysToForecast: Int = 30
    ): List<CashFlowDayProjection> {
        var runningBalance = allAccounts.sumOf { acc ->
            val converted = Money.convert(acc.balanceMinor, acc.currencyCode, currencyCode)
            Money.toDouble(converted, currencyCode)
        }

        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val recentSpend = allExpenses.filter { it.date >= thirtyDaysAgo }.realExpense(currencyCode)
        val dailyBurn = (recentSpend / 30.0).coerceAtLeast(15.0)

        val cal = java.util.Calendar.getInstance()
        val projections = mutableListOf<CashFlowDayProjection>()

        val sdfDay = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
        val sdfDow = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())

        for (i in 0 until daysToForecast) {
            val date = cal.time
            val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)

            // Find bills due on this day of month
            val billsDue = allBills.filter { bill ->
                val dueDay = bill.dueDate.trim().toIntOrNull()
                if (dueDay != null) {
                    dueDay == dayOfMonth
                } else {
                    bill.dueDate.contains(dayOfMonth.toString())
                }
            }

            val billTotal = billsDue.sumOf { it.amount }
            runningBalance = (runningBalance - billTotal - dailyBurn)

            projections.add(
                CashFlowDayProjection(
                    dayTimestamp = cal.timeInMillis,
                    dayLabel = sdfDay.format(date),
                    dayOfWeek = sdfDow.format(date),
                    projectedBalance = runningBalance,
                    plannedIncome = 0.0,
                    plannedBills = billTotal,
                    isDangerZone = runningBalance < (dailyBurn * 5),
                    billNames = billsDue.map { it.title }
                )
            )
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        return projections
    }
}
