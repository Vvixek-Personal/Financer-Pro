package com.example

import com.example.data.Budget
import com.example.data.Expense
import com.example.data.Money
import com.example.ui.AnalyticsCalculator
import com.example.ui.CurrencyManager
import com.example.ui.suggestCategoryFromNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TransactionUtilsAndFilterTest {

    private val defaultExpenseCategories = listOf(
        "Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Education"
    )
    private val defaultIncomeCategories = listOf(
        "Salary", "Freelance", "Investments", "Gifts", "Refunds"
    )

    @Test
    fun suggestCategoryFromNote_matchesKnownKeywords() {
        assertEquals("Food", suggestCategoryFromNote("Morning Starbucks Coffee", defaultExpenseCategories))
        assertEquals("Transport", suggestCategoryFromNote("Uber ride to airport", defaultExpenseCategories))
        assertEquals("Shopping", suggestCategoryFromNote("Amazon order electronics", defaultExpenseCategories))
        assertEquals("Bills", suggestCategoryFromNote("Monthly electricity bill", defaultExpenseCategories))
        assertEquals("Entertainment", suggestCategoryFromNote("Netflix subscription renewal", defaultExpenseCategories))
        assertEquals("Health", suggestCategoryFromNote("Pharmacy medicine pickup", defaultExpenseCategories))
        assertEquals("Salary", suggestCategoryFromNote("Monthly salary deposit", defaultIncomeCategories))
        assertEquals("Investments", suggestCategoryFromNote("Quarterly stock dividend", defaultIncomeCategories))
    }

    @Test
    fun suggestCategoryFromNote_returnsNullForBlankOrUnmatchedNotes() {
        assertNull(suggestCategoryFromNote("", defaultExpenseCategories))
        assertNull(suggestCategoryFromNote("   ", defaultExpenseCategories))
        assertNull(suggestCategoryFromNote("Random note xyz", defaultExpenseCategories))
        assertNull(suggestCategoryFromNote("Coffee", emptyList()))
    }

    @Test
    fun budgetPerformance_computesWarningAndOverBudgetVarianceCorrectly() {
        val budgets = listOf(
            Budget(
                id = 1L,
                category = "Food",
                amountLimit = 1000.0,
                amountLimitMinor = 100000L,
                monthYear = "11-2026",
                currencyCode = "USD"
            ),
            Budget(
                id = 2L,
                category = "Transport",
                amountLimit = 500.0,
                amountLimitMinor = 50000L,
                monthYear = "11-2026",
                currencyCode = "USD"
            )
        )
        val expenses = listOf(
            Expense(
                id = 1L,
                amountMinor = 75000L,
                amount = 750.0,
                category = "Food",
                date = 1700000000000L,
                type = "EXPENSE",
                currencyCode = "USD"
            ),
            Expense(
                id = 2L,
                amountMinor = 60000L,
                amount = 600.0,
                category = "Transport",
                date = 1700000000000L,
                type = "EXPENSE",
                currencyCode = "USD"
            )
        )

        val perf = AnalyticsCalculator.computeBudgetPerformance(expenses, budgets, "USD")
        assertEquals(2, perf.size)

        val transport = perf.first { it.category == "Transport" }
        assertTrue(transport.isOverBudget)
        assertEquals(120.0, transport.percentUsed, 0.01)
        assertEquals(-100.0, transport.variance, 0.01)

        val food = perf.first { it.category == "Food" }
        assertFalse(food.isOverBudget)
        assertEquals(75.0, food.percentUsed, 0.01)
        assertEquals(250.0, food.variance, 0.01)
    }

    @Test
    fun currencyManager_resolvesNonInrSymbolsAndMinorConversion() {
        assertEquals("$", CurrencyManager.getByCode("USD").symbol)
        assertEquals("€", CurrencyManager.getByCode("EUR").symbol)
        assertEquals("¥", CurrencyManager.getByCode("JPY").symbol)
        assertEquals("£", CurrencyManager.getByCode("GBP").symbol)

        val minorUsd = Money.parse("42.50", "USD", Locale.US)!!
        assertEquals(42.50, Money.toDouble(minorUsd, "USD"), 0.001)
    }
}
