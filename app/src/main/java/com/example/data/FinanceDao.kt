package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getExpensesSnapshot(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT EXISTS(SELECT 1 FROM expenses WHERE recurringRuleId = :ruleId AND date = :date)")
    suspend fun recurringExpenseExists(ruleId: Long, date: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    // Accounts
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    suspend fun getAccountsSnapshot(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM expenses WHERE accountId = :accountId")
    suspend fun getExpensesCountForAccount(accountId: Long): Int

    @Query("UPDATE expenses SET accountId = :targetAccountId WHERE accountId = :sourceAccountId")
    suspend fun reassignExpensesAccount(sourceAccountId: Long, targetAccountId: Long)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    // Budgets
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun getBudgetsSnapshot(): List<Budget>

    @Query("SELECT * FROM budgets WHERE category = :category AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetByCategoryAndMonth(category: String, monthYear: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    // Savings Goals
    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals")
    suspend fun getSavingsGoalsSnapshot(): List<SavingsGoal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllSavingsGoals()

    // Recurring Rules
    @Query("SELECT * FROM recurring_rules")
    fun getAllRecurringRules(): Flow<List<RecurringRule>>

    @Query("SELECT * FROM recurring_rules")
    suspend fun getRecurringRulesSnapshot(): List<RecurringRule>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getRecurringRuleById(id: Long): RecurringRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringRule(rule: RecurringRule): Long

    @Update
    suspend fun updateRecurringRule(rule: RecurringRule)

    @Delete
    suspend fun deleteRecurringRule(rule: RecurringRule)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAllRecurringRules()

    // Reminders
    @Query("SELECT * FROM reminders")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getRemindersSnapshot(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()

    @Transaction
    suspend fun clearAllData() {
        deleteAllExpenses()
        deleteAllAccounts()
        deleteAllBudgets()
        deleteAllSavingsGoals()
        deleteAllRecurringRules()
        deleteAllReminders()
    }

    @Transaction
    suspend fun getDatabaseCounts(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        counts["expenses"] = getExpensesSnapshot().size
        counts["accounts"] = getAccountsSnapshot().size
        counts["budgets"] = getBudgetsSnapshot().size
        counts["goals"] = getSavingsGoalsSnapshot().size
        counts["recurringRules"] = getRecurringRulesSnapshot().size
        counts["reminders"] = getRemindersSnapshot().size
        return counts
    }
}
