package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val dao: FinanceDao, private val database: FinanceDatabase) {
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()
    val allAccounts: Flow<List<Account>> = dao.getAllAccounts()
    val allBudgets: Flow<List<Budget>> = dao.getAllBudgets()
    val allSavingsGoals: Flow<List<SavingsGoal>> = dao.getAllSavingsGoals()
    val allRecurringRules: Flow<List<RecurringRule>> = dao.getAllRecurringRules()
    val allReminders: Flow<List<ReminderEntity>> = dao.getAllReminders()

    suspend fun ensureDefaultAccountCreated(defaultCurrency: String = "INR"): Long {
        val existing = dao.getAccountsSnapshot()
        if (existing.isNotEmpty()) {
            if (existing.size == 1 && existing.first().name.equals("Cash", ignoreCase = true)) {
                dao.insertAccount(
                    Account(
                        name = "Bank Account",
                        type = "BANK",
                        currencyCode = defaultCurrency,
                        openingBalance = 0.0,
                        balance = 0.0,
                        openingBalanceMinor = 0L,
                        balanceMinor = 0L
                    )
                )
            }
            return existing.first().id
        }
        val cashId = dao.insertAccount(
            Account(
                name = "Cash",
                type = "CASH",
                currencyCode = defaultCurrency,
                openingBalance = 0.0,
                balance = 0.0,
                openingBalanceMinor = 0L,
                balanceMinor = 0L
            )
        )
        dao.insertAccount(
            Account(
                name = "Bank Account",
                type = "BANK",
                currencyCode = defaultCurrency,
                openingBalance = 0.0,
                balance = 0.0,
                openingBalanceMinor = 0L,
                balanceMinor = 0L
            )
        )
        return cashId
    }

    suspend fun insertExpense(expense: Expense): Long {
        val targetAccountId = if (expense.accountId > 0L) {
            val existing = dao.getAccountById(expense.accountId)
                ?: throw IllegalArgumentException("Account with id ${expense.accountId} does not exist")
            existing.id
        } else {
            dao.getAccountsSnapshot().firstOrNull()?.id
                ?: throw IllegalStateException("No account exists. Please create an account before adding transactions.")
        }

        val toInsert = syncExpenseAmounts(expense.copy(accountId = targetAccountId))
        val id = dao.insertExpense(toInsert)
        syncAccountBalance(targetAccountId)
        return id
    }

    suspend fun updateExpense(expense: Expense) {
        val old = dao.getExpenseById(expense.id)
        val synced = syncExpenseAmounts(expense)
        dao.updateExpense(synced)
        if (old != null && old.accountId != synced.accountId) {
            syncAccountBalance(old.accountId)
        }
        syncAccountBalance(synced.accountId)
    }

    suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense)
        syncAccountBalance(expense.accountId)
    }

    suspend fun deleteExpenseById(id: Long) {
        val expense = dao.getExpenseById(id)
        dao.deleteExpenseById(id)
        if (expense != null) {
            syncAccountBalance(expense.accountId)
        }
    }

    suspend fun deleteAllExpenses() {
        dao.deleteAllExpenses()
        syncAllAccountBalances()
    }

    suspend fun insertAccount(account: Account): Long {
        val id = dao.insertAccount(syncAccountAmounts(account))
        syncAccountBalance(id)
        return id
    }

    suspend fun updateAccount(account: Account) = dao.updateAccount(syncAccountAmounts(account))
    suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)
    suspend fun getExpensesCountForAccount(accountId: Long): Int = dao.getExpensesCountForAccount(accountId)
    suspend fun reassignExpensesAccount(sourceAccountId: Long, targetAccountId: Long) {
        dao.reassignExpensesAccount(sourceAccountId, targetAccountId)
        syncAccountBalance(sourceAccountId)
        syncAccountBalance(targetAccountId)
    }
    suspend fun getAccountsSnapshot(): List<Account> = dao.getAccountsSnapshot()
    suspend fun getAccountById(id: Long) = dao.getAccountById(id)

    suspend fun getExpenseById(id: Long) = dao.getExpenseById(id)

    suspend fun computeAccountBalanceMinor(accountId: Long): Long {
        val account = dao.getAccountById(accountId) ?: return 0L
        val expenses = dao.getExpensesSnapshot().filter { it.accountId == accountId }
        val netChange = expenses.sumOf { exp ->
            val isIncome = exp.type == "INCOME" || exp.kind == "TRANSFER_IN"
            val sign = if (isIncome) 1L else -1L
            val amountInAccountCurrencyMinor = if (exp.currencyCode == account.currencyCode) {
                exp.amountMinor
            } else {
                Money.convert(exp.amountMinor, exp.currencyCode, account.currencyCode)
            }
            sign * amountInAccountCurrencyMinor
        }
        return account.openingBalanceMinor + netChange
    }

    suspend fun syncAccountBalance(accountId: Long) {
        val account = dao.getAccountById(accountId) ?: return
        val newBalanceMinor = computeAccountBalanceMinor(accountId)
        val updated = account.copy(
            balanceMinor = newBalanceMinor,
            balance = Money.toDouble(newBalanceMinor, account.currencyCode)
        )
        dao.updateAccount(updated)
    }

    suspend fun syncAllAccountBalances() {
        val accounts = dao.getAccountsSnapshot()
        accounts.forEach { acc ->
            syncAccountBalance(acc.id)
        }
    }

    suspend fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        date: Long,
        note: String?,
        currencyCode: String
    ): Pair<Long, Long> {
        require(fromAccountId != toAccountId) { "Source and destination accounts must be different." }
        require(amount > 0.0) { "Transfer amount must be greater than zero." }

        val fromAccount = dao.getAccountById(fromAccountId)
            ?: throw IllegalArgumentException("Source account not found.")
        val toAccount = dao.getAccountById(toAccountId)
            ?: throw IllegalArgumentException("Destination account not found.")

        val amountMinor = Money.fromDouble(amount, currencyCode)
        val cleanNote = note?.trim()

        val outExpense = Expense(
            amount = amount,
            amountMinor = amountMinor,
            category = "Transfer",
            date = date,
            note = if (cleanNote.isNullOrEmpty()) "Transfer to ${toAccount.name}" else "$cleanNote (➔ ${toAccount.name})",
            type = "EXPENSE",
            currencyCode = currencyCode,
            accountId = fromAccountId,
            kind = "TRANSFER_OUT"
        )

        val inExpense = Expense(
            amount = amount,
            amountMinor = amountMinor,
            category = "Transfer",
            date = date,
            note = if (cleanNote.isNullOrEmpty()) "Transfer from ${fromAccount.name}" else "$cleanNote (⬅ ${fromAccount.name})",
            type = "INCOME",
            currencyCode = currencyCode,
            accountId = toAccountId,
            kind = "TRANSFER_IN"
        )

        return database.withTransaction {
            val outId = dao.insertExpense(outExpense)
            val inId = dao.insertExpense(inExpense)
            syncAccountBalance(fromAccountId)
            syncAccountBalance(toAccountId)
            Pair(outId, inId)
        }
    }

    suspend fun insertBudget(budget: Budget): Long {
        val synced = syncBudgetAmounts(budget)
        val existing = dao.getBudgetByCategoryAndMonth(synced.category, synced.monthYear)
        return if (existing != null) {
            dao.updateBudget(synced.copy(id = existing.id))
            existing.id
        } else {
            dao.insertBudget(synced)
        }
    }
    suspend fun updateBudget(budget: Budget) = dao.updateBudget(syncBudgetAmounts(budget))
    suspend fun deleteBudget(budget: Budget) = dao.deleteBudget(budget)

    suspend fun insertSavingsGoal(goal: SavingsGoal) = dao.insertSavingsGoal(syncSavingsGoalAmounts(goal))
    suspend fun updateSavingsGoal(goal: SavingsGoal) = dao.updateSavingsGoal(syncSavingsGoalAmounts(goal))
    suspend fun deleteSavingsGoal(goal: SavingsGoal) = dao.deleteSavingsGoal(goal)

    suspend fun insertRecurringRule(rule: RecurringRule) = dao.insertRecurringRule(rule)
    suspend fun updateRecurringRule(rule: RecurringRule) = dao.updateRecurringRule(rule)
    suspend fun deleteRecurringRule(rule: RecurringRule) = dao.deleteRecurringRule(rule)
    suspend fun getRecurringRuleById(id: Long) = dao.getRecurringRuleById(id)

    suspend fun convertAllEntitiesToCurrency(
        targetCurrency: String,
        rates: Map<String, Double>
    ) {
        database.withTransaction {
            // 1. Convert Expenses (Preserve original values if not set)
            val currentExpenses = dao.getExpensesSnapshot()
            currentExpenses.forEach { exp ->
                val originalCurrency = exp.originalCurrencyCode ?: exp.currencyCode
                val originalMinor = exp.originalAmountMinor ?: exp.amountMinor
                val convertedMinor = Money.convert(exp.amountMinor, exp.currencyCode, targetCurrency, rates)
                val convertedDouble = Money.toDouble(convertedMinor, targetCurrency)
                val updatedExp = exp.copy(
                    amount = convertedDouble,
                    amountMinor = convertedMinor,
                    currencyCode = targetCurrency,
                    originalCurrencyCode = originalCurrency,
                    originalAmountMinor = originalMinor
                )
                dao.updateExpense(updatedExp)
            }

            // 2. Convert Accounts
            val currentAccounts = dao.getAccountsSnapshot()
            currentAccounts.forEach { acc ->
                val newOpeningMinor = Money.convert(acc.openingBalanceMinor, acc.currencyCode, targetCurrency, rates)
                val newBalanceMinor = Money.convert(acc.balanceMinor, acc.currencyCode, targetCurrency, rates)
                val updatedAcc = acc.copy(
                    currencyCode = targetCurrency,
                    openingBalanceMinor = newOpeningMinor,
                    openingBalance = Money.toDouble(newOpeningMinor, targetCurrency),
                    balanceMinor = newBalanceMinor,
                    balance = Money.toDouble(newBalanceMinor, targetCurrency)
                )
                dao.updateAccount(updatedAcc)
            }

            // 3. Convert Budgets
            val currentBudgets = dao.getBudgetsSnapshot()
            currentBudgets.forEach { b ->
                val newLimitMinor = Money.convert(b.amountLimitMinor, b.currencyCode, targetCurrency, rates)
                val updatedBudget = b.copy(
                    currencyCode = targetCurrency,
                    amountLimitMinor = newLimitMinor,
                    amountLimit = Money.toDouble(newLimitMinor, targetCurrency)
                )
                dao.updateBudget(updatedBudget)
            }

            // 4. Convert Savings Goals
            val currentGoals = dao.getSavingsGoalsSnapshot()
            currentGoals.forEach { g ->
                val newTargetMinor = Money.convert(g.targetAmountMinor, g.currencyCode, targetCurrency, rates)
                val newCurrentMinor = Money.convert(g.currentAmountMinor, g.currencyCode, targetCurrency, rates)
                val newContributionMinor = Money.convert(g.contributionAmountMinor, g.currencyCode, targetCurrency, rates)
                val updatedGoal = g.copy(
                    currencyCode = targetCurrency,
                    targetAmountMinor = newTargetMinor,
                    targetAmount = Money.toDouble(newTargetMinor, targetCurrency),
                    currentAmountMinor = newCurrentMinor,
                    currentAmount = Money.toDouble(newCurrentMinor, targetCurrency),
                    contributionAmountMinor = newContributionMinor,
                    contributionAmount = Money.toDouble(newContributionMinor, targetCurrency)
                )
                dao.updateSavingsGoal(updatedGoal)
            }

            // 5. Convert Recurring Rules
            val currentRules = dao.getRecurringRulesSnapshot()
            currentRules.forEach { r ->
                val newRuleMinor = Money.convert(r.amountMinor, r.currencyCode, targetCurrency, rates)
                val updatedRule = r.copy(
                    currencyCode = targetCurrency,
                    amountMinor = newRuleMinor
                )
                dao.updateRecurringRule(updatedRule)
            }
        }
    }

    private fun syncExpenseAmounts(expense: Expense): Expense {
        val currency = expense.currencyCode
        return when {
            expense.amountMinor != 0L -> {
                // amountMinor is authoritative single source of truth
                expense.copy(amount = Money.toDouble(expense.amountMinor, currency))
            }
            expense.amount != 0.0 -> {
                expense.copy(amountMinor = Money.fromDouble(expense.amount, currency))
            }
            else -> expense
        }
    }

    private fun syncAccountAmounts(account: Account): Account {
        val currency = account.currencyCode
        var acc = account
        if (acc.openingBalanceMinor != 0L) {
            acc = acc.copy(openingBalance = Money.toDouble(acc.openingBalanceMinor, currency))
        } else if (acc.openingBalance != 0.0) {
            acc = acc.copy(openingBalanceMinor = Money.fromDouble(acc.openingBalance, currency))
        }

        if (acc.balanceMinor != 0L) {
            acc = acc.copy(balance = Money.toDouble(acc.balanceMinor, currency))
        } else if (acc.balance != 0.0) {
            acc = acc.copy(balanceMinor = Money.fromDouble(acc.balance, currency))
        }
        return acc
    }

    private fun syncBudgetAmounts(budget: Budget): Budget {
        val currency = budget.currencyCode
        return when {
            budget.amountLimitMinor != 0L -> {
                budget.copy(amountLimit = Money.toDouble(budget.amountLimitMinor, currency))
            }
            budget.amountLimit != 0.0 -> {
                budget.copy(amountLimitMinor = Money.fromDouble(budget.amountLimit, currency))
            }
            else -> budget
        }
    }

    private fun syncSavingsGoalAmounts(goal: SavingsGoal): SavingsGoal {
        val currency = goal.currencyCode
        var g = goal
        if (g.targetAmountMinor != 0L) {
            g = g.copy(targetAmount = Money.toDouble(g.targetAmountMinor, currency))
        } else if (g.targetAmount != 0.0) {
            g = g.copy(targetAmountMinor = Money.fromDouble(g.targetAmount, currency))
        }

        if (g.currentAmountMinor != 0L) {
            g = g.copy(currentAmount = Money.toDouble(g.currentAmountMinor, currency))
        } else if (g.currentAmount != 0.0) {
            g = g.copy(currentAmountMinor = Money.fromDouble(g.currentAmount, currency))
        }

        if (g.contributionAmountMinor != 0L) {
            g = g.copy(contributionAmount = Money.toDouble(g.contributionAmountMinor, currency))
        } else if (g.contributionAmount != 0.0) {
            g = g.copy(contributionAmountMinor = Money.fromDouble(g.contributionAmount, currency))
        }
        return g
    }

    suspend fun insertReminder(reminder: ReminderEntity) = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = dao.deleteReminder(reminder)
    suspend fun getReminderById(id: Long) = dao.getReminderById(id)

    suspend fun clearAllData() = dao.clearAllData()

    suspend fun restoreAllData(
        expenses: List<Expense>,
        accounts: List<Account>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        reminders: List<ReminderEntity>,
        rules: List<RecurringRule> = emptyList()
    ) {
        database.withTransaction {
            dao.clearAllData()
            accounts.forEach { insertAccount(it) }
            rules.forEach { insertRecurringRule(it) }
            expenses.forEach { insertExpense(it) }
            budgets.forEach { insertBudget(it) }
            goals.forEach { insertSavingsGoal(it) }
            reminders.forEach { insertReminder(it) }
        }
    }
}
