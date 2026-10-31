package com.example.ui.health

import com.example.data.Money
import com.example.data.Expense
import com.example.data.SavingsGoal
import com.example.data.Account
import com.example.data.realExpense
import com.example.data.realIncome

/**
 * Calculates financial health scores based on current financial state
 */
class FinancialHealthCalculator {

    data class HealthScoreBreakdown(
        val overallScore: Int,
        val ratingTitle: String,
        val ratingColorHex: Long,
        val budgetScore: Int,
        val savingsScore: Int,
        val runwayScore: Int,
        val stabilityScore: Int,
        val runwayMonths: Double,
        val savingsRatePct: Int,
        val monthlyBurn: Double,
        val liquidReserves: Double,
        val insights: List<String>
    )

    fun calculateHealthScore(
        allExpenses: List<Expense>,
        allGoals: List<SavingsGoal>,
        allAccounts: List<Account>,
        budget: Double,
        currencyCode: String,
        simulatedMonthlySavingsBoost: Double = 0.0,
        simulatedExpenseCut: Double = 0.0
    ): HealthScoreBreakdown {
        val cur = currencyCode
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)

        val recentExpenses = allExpenses.filter { it.date >= thirtyDaysAgo }
        val rawRecentSpend = recentExpenses.realExpense(cur)
        val actualSpend = (rawRecentSpend - simulatedExpenseCut).coerceAtLeast(0.0)

        val recentIncome = recentExpenses.realIncome(cur)
        val effectiveIncome = if (recentIncome > 0) recentIncome else (actualSpend * 1.25).coerceAtLeast(1000.0)

        val totalGoalSavings = allGoals.sumOf { goal ->
            val converted = Money.convert(goal.currentAmountMinor, goal.currencyCode, cur)
            Money.toDouble(converted, cur)
        } + simulatedMonthlySavingsBoost

        val totalLiquidAccounts = allAccounts.sumOf { acc ->
            val converted = Money.convert(acc.balanceMinor, acc.currencyCode, cur)
            Money.toDouble(converted, cur)
        }
        val totalLiquidReserves = (totalLiquidAccounts + (effectiveIncome - actualSpend).coerceAtLeast(0.0)).coerceAtLeast(0.0)

        // 1. Runway Calculation (Goal: 6 months emergency cushion)
        val monthlyBurn = if (actualSpend > 0) actualSpend else 500.0
        val runwayMonths = totalLiquidReserves / monthlyBurn
        val runwayScore = ((runwayMonths / 6.0) * 100.0).coerceIn(0.0, 100.0).toInt()

        // 2. Budget Discipline Score
        val budgetScore = if (budget > 0) {
            val ratio = actualSpend / budget
            when {
                ratio <= 0.80 -> 100
                ratio <= 1.00 -> (100 - ((ratio - 0.80) / 0.20 * 20)).toInt().coerceIn(70, 100)
                ratio <= 1.20 -> (70 - ((ratio - 1.00) / 0.20 * 40)).toInt().coerceIn(20, 70)
                else -> 10
            }
        } else {
            75 // Baseline when no budget cap set
        }

        // 3. Savings Velocity Score (Goal: 20%+ savings rate)
        val savingsRate = ((totalGoalSavings.coerceAtLeast(10.0) / effectiveIncome.coerceAtLeast(100.0))).coerceIn(0.0, 1.0)
        val savingsScore = ((savingsRate / 0.20) * 100.0).coerceIn(0.0, 100.0).toInt()
        val savingsRatePct = (savingsRate * 100).toInt()

        // 4. Expense Stability Score
        val stabilityScore = if (recentExpenses.size >= 5) 85 else 70

        // Composite Weighted Score
        val overall = (
            (budgetScore * 0.30) +
            (savingsScore * 0.25) +
            (runwayScore * 0.25) +
            (stabilityScore * 0.20)
        ).toInt().coerceIn(5, 100)

        val (ratingTitle, ratingColorHex) = when {
            overall >= 80 -> "Excellent" to 0xFF10B981
            overall >= 60 -> "Good Standing" to 0xFF6366F1
            overall >= 40 -> "Fair" to 0xFFF59E0B
            else -> "Needs Attention" to 0xFFEF4444
        }

        val insights = mutableListOf<String>()
        if (runwayMonths < 3.0) {
            insights.add("⚠️ Emergency cushion is under 3 months (${"%.1f".format(runwayMonths)} mos). Prioritize building emergency savings.")
        } else if (runwayMonths >= 6.0) {
            insights.add("🛡️ Outstanding emergency runway! You have ${"%.1f".format(runwayMonths)} months of living expenses secured.")
        }

        if (budget > 0 && actualSpend > budget) {
            insights.add("⚡ Monthly spend exceeds your target budget by ${"%.0f".format(actualSpend - budget)}. Review non-essential categories.")
        } else if (budget > 0) {
            insights.add("🎯 Spending is within budget limits (${"%.0f".format((actualSpend / budget) * 100)}% utilized).")
        }

        if (savingsRatePct >= 20) {
            insights.add("🚀 Great savings momentum! Saving ${savingsRatePct}% of monthly income into financial goals.")
        } else {
            insights.add("💡 Increasing savings by just 5% can boost your financial resilience score by up to 12 points.")
        }

        return HealthScoreBreakdown(
            overallScore = overall,
            ratingTitle = ratingTitle,
            ratingColorHex = ratingColorHex,
            budgetScore = budgetScore,
            savingsScore = savingsScore,
            runwayScore = runwayScore,
            stabilityScore = stabilityScore,
            runwayMonths = runwayMonths,
            savingsRatePct = savingsRatePct,
            monthlyBurn = monthlyBurn,
            liquidReserves = totalLiquidReserves,
            insights = insights
        )
    }
}


