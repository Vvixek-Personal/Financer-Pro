package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

private const val TAG = "LocalRecoveryManager"
private const val SNAPSHOT_FORMAT_VERSION = 2
private const val MAX_RETAINED_SNAPSHOTS = 10

data class SnapshotMeta(
    val fileName: String,
    val createdAt: Long,
    val fileSize: Long = 0L,
    val accountsCount: Int = 0,
    val expensesCount: Int = 0,
    val budgetsCount: Int = 0,
    val savingsGoalsCount: Int = 0,
    val remindersCount: Int = 0,
    val isValid: Boolean = true
)

private data class RecoveryDatabaseSnapshot(
    val accounts: List<Account>,
    val expenses: List<Expense>,
    val budgets: List<Budget>,
    val goals: List<SavingsGoal>,
    val reminders: List<ReminderEntity>,
    val rules: List<RecurringRule>
)

object LocalRecoveryManager {
    fun getSnapshotDir(context: Context): File {
        val dir = File(context.filesDir, "snapshots")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun listSnapshots(context: Context): List<SnapshotMeta> {
        val dir = getSnapshotDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()

        return files.map { file ->
            try {
                val jsonStr = file.readText(Charsets.UTF_8)
                val json = JSONObject(jsonStr)
                val payloadStr = json.optString("payload", "")
                val expectedChecksum = json.optString("checksum", "")
                val isValidChecksum = expectedChecksum.isNotBlank() &&
                    sha256(payloadStr.toByteArray(Charsets.UTF_8)) == expectedChecksum

                val payload = if (payloadStr.isNotBlank()) JSONObject(payloadStr) else json
                val formatVersion = payload.optInt("formatVersion", 1)
                val requiredCollections = listOf("accounts", "expenses", "budgets", "savingsGoals", "reminders")
                val collectionsPresent = requiredCollections.all { payload.optJSONArray(it) != null } &&
                    (formatVersion < 2 || payload.optJSONArray("recurringRules") != null)
                val schemaVersion = payload.optInt("schemaVersion", 0)
                val supportedVersion = formatVersion in 1..SNAPSHOT_FORMAT_VERSION &&
                    schemaVersion in 0..FINANCE_DB_VERSION
                val meta = payload.optJSONObject("meta")
                val createdAt = meta?.optLong("createdAt", file.lastModified()) ?: file.lastModified()
                val counts = meta?.optJSONObject("counts")
                val countEntries = listOf(
                    "accounts" to "accounts",
                    "expenses" to "expenses",
                    "budgets" to "budgets",
                    "savingsGoals" to "savingsGoals",
                    "reminders" to "reminders",
                    "recurringRules" to "recurringRules"
                )
                val countsMatch = if (formatVersion >= 2) {
                    counts != null && countEntries.all { (countKey, arrayKey) ->
                        counts.has(countKey) && counts.optInt(countKey, -1) == (payload.optJSONArray(arrayKey)?.length() ?: 0)
                    }
                } else {
                    counts == null || countEntries.all { (countKey, arrayKey) ->
                        !counts.has(countKey) || counts.optInt(countKey, -1) == (payload.optJSONArray(arrayKey)?.length() ?: 0)
                    }
                }
                // Unchecksummed legacy files are intentionally marked unverified: the UI's
                // normal restore action does not permit them without an explicit override.
                val integrityValid = collectionsPresent && supportedVersion && countsMatch && isValidChecksum

                SnapshotMeta(
                    fileName = file.name,
                    createdAt = createdAt,
                    fileSize = file.length(),
                    accountsCount = payload.optJSONArray("accounts")?.length() ?: 0,
                    expensesCount = payload.optJSONArray("expenses")?.length() ?: 0,
                    budgetsCount = payload.optJSONArray("budgets")?.length() ?: 0,
                    savingsGoalsCount = payload.optJSONArray("savingsGoals")?.length() ?: 0,
                    remindersCount = payload.optJSONArray("reminders")?.length() ?: 0,
                    isValid = integrityValid
                )
            } catch (e: Exception) {
                Log.w(TAG, "Corrupt or unreadable snapshot: ${file.name}", e)
                SnapshotMeta(
                    fileName = file.name,
                    createdAt = file.lastModified(),
                    fileSize = file.length(),
                    isValid = false
                )
            }
        }.sortedByDescending { it.createdAt }
    }

    suspend fun createSnapshot(context: Context, db: FinanceDatabase): Boolean {
        return try {
            // Read every financial table inside one Room transaction so the snapshot
            // cannot combine records from different database moments.
            val dao = db.financeDao()
            val databaseSnapshot = db.withTransaction {
                RecoveryDatabaseSnapshot(
                    accounts = dao.getAccountsSnapshot(),
                    expenses = dao.getExpensesSnapshot(),
                    budgets = dao.getBudgetsSnapshot(),
                    goals = dao.getSavingsGoalsSnapshot(),
                    reminders = dao.getRemindersSnapshot(),
                    rules = dao.getRecurringRulesSnapshot()
                )
            }
            val accounts = databaseSnapshot.accounts
            val expenses = databaseSnapshot.expenses
            val budgets = databaseSnapshot.budgets
            val goals = databaseSnapshot.goals
            val reminders = databaseSnapshot.reminders
            val rules = databaseSnapshot.rules

            val prefs = context.getSharedPreferences(AppSettingsManager.PREFS_NAME, Context.MODE_PRIVATE)
            val currencyCode = prefs.getString("currency_code", "INR") ?: "INR"
            val currencySymbol = prefs.getString("currency_symbol", "₹") ?: "₹"
            val currencyName = prefs.getString("currency_name", "Indian Rupee") ?: "Indian Rupee"
            val monthlyBudget = prefs.getFloat("monthly_budget", 0f).toDouble()

            val now = System.currentTimeMillis()

            val payload = JSONObject()
            payload.put("formatVersion", SNAPSHOT_FORMAT_VERSION)
            payload.put("schemaVersion", FINANCE_DB_VERSION)
            payload.put("createdAt", now)

            val meta = JSONObject().apply {
                put("createdAt", now)
                val counts = JSONObject().apply {
                    put("accounts", accounts.size)
                    put("expenses", expenses.size)
                    put("budgets", budgets.size)
                    put("savingsGoals", goals.size)
                    put("reminders", reminders.size)
                    put("recurringRules", rules.size)
                }
                put("counts", counts)
            }
            payload.put("meta", meta)

            val settingsObj = JSONObject().apply {
                put("currencyCode", currencyCode)
                put("currencySymbol", currencySymbol)
                put("currencyName", currencyName)
                put("monthlyBudget", monthlyBudget)
            }
            payload.put("settings", settingsObj)

            // Accounts
            val accountsArr = JSONArray()
            accounts.forEach { acc ->
                val obj = JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("type", acc.type)
                    put("currencyCode", acc.currencyCode)
                    put("openingBalance", acc.openingBalance)
                    put("balance", acc.balance)
                    put("openingBalanceMinor", acc.openingBalanceMinor)
                    put("balanceMinor", acc.balanceMinor)
                }
                accountsArr.put(obj)
            }
            payload.put("accounts", accountsArr)

            // Recurring Rules
            val rulesArr = JSONArray()
            rules.forEach { r ->
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("amountMinor", r.amountMinor)
                    put("category", r.category)
                    put("frequency", r.frequency)
                    put("startDate", r.startDate)
                    put("nextDueDate", r.nextDueDate)
                    put("isActive", r.isActive)
                    put("currencyCode", r.currencyCode)
                    put("accountId", r.accountId)
                    put("note", r.note ?: "")
                }
                rulesArr.put(obj)
            }
            payload.put("recurringRules", rulesArr)

            // Expenses
            val expensesArr = JSONArray()
            expenses.forEach { exp ->
                val obj = JSONObject().apply {
                    put("id", exp.id)
                    put("amount", exp.amount)
                    put("amountMinor", exp.amountMinor)
                    put("category", exp.category)
                    put("date", exp.date)
                    put("note", exp.note ?: "")
                    put("imagePath", exp.imagePath ?: "")
                    put("type", exp.type)
                    put("currencyCode", exp.currencyCode)
                    if ((exp.originalCurrencyCode == null) != (exp.originalAmountMinor == null)) {
                        throw IllegalStateException("Expense ${exp.id} has incomplete original-currency metadata")
                    }
                    if (exp.originalCurrencyCode != null && exp.originalAmountMinor != null) {
                        put("originalCurrencyCode", exp.originalCurrencyCode)
                        put("originalAmountMinor", exp.originalAmountMinor)
                    }
                    put("accountId", exp.accountId)
                    put("kind", exp.kind)
                    put("goalId", exp.goalId ?: -1L)
                    put("recurringRuleId", exp.recurringRuleId ?: -1L)
                }
                expensesArr.put(obj)
            }
            payload.put("expenses", expensesArr)

            // Budgets
            val budgetsArr = JSONArray()
            budgets.forEach { b ->
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("category", b.category)
                    put("amountLimit", b.amountLimit)
                    put("amountLimitMinor", b.amountLimitMinor)
                    put("monthYear", b.monthYear)
                    put("currencyCode", b.currencyCode)
                }
                budgetsArr.put(obj)
            }
            payload.put("budgets", budgetsArr)

            // Savings Goals
            val goalsArr = JSONArray()
            goals.forEach { g ->
                val obj = JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmount", g.targetAmount)
                    put("currentAmount", g.currentAmount)
                    put("targetAmountMinor", g.targetAmountMinor)
                    put("currentAmountMinor", g.currentAmountMinor)
                    put("targetDate", g.targetDate)
                    put("frequency", g.frequency)
                    put("contributionAmount", g.contributionAmount)
                    put("contributionAmountMinor", g.contributionAmountMinor)
                    put("isAutoGap", g.isAutoGap)
                    put("iconTag", g.iconTag)
                    put("category", g.category)
                    put("imageUri", g.imageUri ?: "")
                    put("currencyCode", g.currencyCode)
                }
                goalsArr.put(obj)
            }
            payload.put("savingsGoals", goalsArr)

            // Reminders
            val remindersArr = JSONArray()
            reminders.forEach { rem ->
                val obj = JSONObject().apply {
                    put("id", rem.id)
                    put("text", rem.text)
                    put("dueDate", rem.dueDate)
                    put("isCompleted", rem.isCompleted)
                    put("isEnabled", rem.isEnabled)
                }
                remindersArr.put(obj)
            }
            payload.put("reminders", remindersArr)

            val payloadString = payload.toString()
            val checksum = sha256(payloadString.toByteArray(Charsets.UTF_8))

            val wrapper = JSONObject().apply {
                put("formatVersion", SNAPSHOT_FORMAT_VERSION)
                put("checksum", checksum)
                put("payload", payloadString)
            }

            val dir = getSnapshotDir(context)
            val fileName = "snapshot_${now}_${System.nanoTime()}.json"
            val tempFile = File(dir, "${fileName}.tmp")
            val targetFile = File(dir, fileName)

            FileOutputStream(tempFile).use { out ->
                out.write(wrapper.toString(2).toByteArray(Charsets.UTF_8))
                out.flush()
                out.fd.sync()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.delete()
                throw IllegalStateException("Could not atomically publish recovery snapshot")
            }

            pruneOldSnapshots(context, MAX_RETAINED_SNAPSHOTS)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create local recovery snapshot", e)
            false
        }
    }

    suspend fun restoreSnapshot(
        context: Context,
        db: FinanceDatabase,
        fileName: String,
        allowUnverifiedLegacy: Boolean = false,
        onCurrencyRestored: (String, String, String, Double) -> Unit
    ): Boolean {
        val snapshotDir = getSnapshotDir(context).canonicalFile
        val file = File(snapshotDir, fileName).canonicalFile
        if (file.parentFile != snapshotDir || !file.name.endsWith(".json")) {
            Log.w(TAG, "Rejected invalid snapshot path: $fileName")
            return false
        }
        if (!file.exists() || !file.isFile || !file.canRead()) {
            Log.w(TAG, "Snapshot file does not exist or unreadable: $fileName")
            return false
        }

        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val root = JSONObject(jsonStr)

            val payloadObj = if (root.has("payload")) {
                val payloadString = root.getString("payload")
                val expectedChecksum = root.optString("checksum", "")
                val actualChecksum = sha256(payloadString.toByteArray(Charsets.UTF_8))
                if (expectedChecksum.isBlank() || expectedChecksum != actualChecksum) {
                    Log.w(TAG, "Snapshot checksum mismatch for $fileName")
                    return false
                }
                JSONObject(payloadString)
            } else {
                if (!allowUnverifiedLegacy) {
                    Log.w(TAG, "Snapshot lacks checksum and unverified legacy restore not permitted: $fileName")
                    return false
                }
                root
            }

            val formatVersion = payloadObj.optInt("formatVersion", 1)
            val schemaVersion = payloadObj.optInt("schemaVersion", 0)
            if (formatVersion !in 1..SNAPSHOT_FORMAT_VERSION) {
                throw IllegalArgumentException("Unsupported snapshot format version: $formatVersion")
            }
            if (schemaVersion !in 0..FINANCE_DB_VERSION) {
                throw IllegalArgumentException("Snapshot database schema $schemaVersion is newer than supported schema $FINANCE_DB_VERSION")
            }
            if (root.has("payload") && root.optInt("formatVersion", formatVersion) != formatVersion) {
                throw IllegalArgumentException("Snapshot wrapper and payload format versions do not match")
            }
            val requiredArrays = listOf("accounts", "expenses", "budgets", "savingsGoals", "reminders")
            requiredArrays.forEach { key ->
                if (!payloadObj.has(key) || payloadObj.isNull(key) || payloadObj.optJSONArray(key) == null) {
                    throw IllegalArgumentException("Snapshot is missing required collection: $key")
                }
            }
            if (formatVersion >= 2 && payloadObj.optJSONArray("recurringRules") == null) {
                throw IllegalArgumentException("Snapshot is missing required collection: recurringRules")
            }

            // Parse accounts
            val rawAccounts = payloadObj.optJSONArray("accounts") ?: JSONArray()
            val accounts = (0 until rawAccounts.length()).map { idx ->
                val obj = rawAccounts.getJSONObject(idx)
                val cur = obj.optString("currencyCode", "INR")
                val minor = if (obj.has("balanceMinor")) obj.getLong("balanceMinor") else Money.fromDouble(obj.optDouble("balance", 0.0), cur)
                val opMinor = if (obj.has("openingBalanceMinor")) obj.getLong("openingBalanceMinor") else minor
                val name = obj.optString("name", "Account").trim()
                if (name.isBlank()) throw IllegalArgumentException("Account at index $idx has empty name")
                Account(
                    id = obj.optLong("id", 0L),
                    name = name,
                    type = obj.optString("type", "BANK"),
                    currencyCode = cur,
                    openingBalance = Money.toDouble(opMinor, cur),
                    balance = Money.toDouble(minor, cur),
                    openingBalanceMinor = opMinor,
                    balanceMinor = minor
                )
            }

            val validAccountIds = accounts.map { it.id }.toSet()
            if (validAccountIds.size != accounts.size || accounts.any { it.id <= 0L }) {
                throw IllegalArgumentException("Snapshot contains duplicate or invalid account IDs")
            }

            // Parse recurring rules
            val rawRules = payloadObj.optJSONArray("recurringRules") ?: JSONArray()
            val rules = (0 until rawRules.length()).map { idx ->
                val obj = rawRules.getJSONObject(idx)
                val cur = obj.optString("currencyCode", "INR")
                val amtMinor = obj.optLong("amountMinor", 0L)
                val accId = obj.optLong("accountId", 0L)
                RecurringRule(
                    id = obj.optLong("id", 0L),
                    title = obj.optString("title", "Recurring").ifBlank { "Recurring Bill" },
                    amountMinor = amtMinor,
                    category = obj.optString("category", "General"),
                    frequency = obj.optString("frequency", "MONTHLY"),
                    startDate = obj.optLong("startDate", System.currentTimeMillis()),
                    nextDueDate = obj.optLong("nextDueDate", System.currentTimeMillis()),
                    isActive = obj.optBoolean("isActive", true),
                    currencyCode = cur,
                    accountId = accId.also {
                        if (!validAccountIds.contains(it)) throw IllegalArgumentException("Recurring rule at index $idx references missing account $it")
                    },
                    note = obj.optString("note", "")
                )
            }

            // Parse expenses
            val rawExpenses = payloadObj.optJSONArray("expenses") ?: JSONArray()
            val expenses = (0 until rawExpenses.length()).map { idx ->
                val obj = rawExpenses.getJSONObject(idx)
                val cur = obj.optString("currencyCode", "INR")
                val minor = if (obj.has("amountMinor")) obj.getLong("amountMinor") else Money.fromDouble(obj.optDouble("amount", 0.0), cur)
                val gId = obj.optLong("goalId", -1L)
                val rId = obj.optLong("recurringRuleId", -1L)
                val accId = obj.optLong("accountId", 0L)
                if (!validAccountIds.contains(accId)) {
                    throw IllegalArgumentException("Expense at index $idx references missing account $accId")
                }
                val originalCurrencyCode = if (obj.has("originalCurrencyCode") && !obj.isNull("originalCurrencyCode")) {
                    obj.getString("originalCurrencyCode").takeIf { it.isNotBlank() }
                } else null
                val originalAmountMinor = if (obj.has("originalAmountMinor") && !obj.isNull("originalAmountMinor")) {
                    obj.getLong("originalAmountMinor")
                } else null
                if ((originalCurrencyCode == null) != (originalAmountMinor == null)) {
                    throw IllegalArgumentException("Expense at index $idx has incomplete original-currency metadata")
                }
                Expense(
                    id = obj.optLong("id", 0L),
                    amount = Money.toDouble(minor, cur),
                    amountMinor = minor,
                    category = obj.optString("category", "General"),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    note = if (obj.has("note")) obj.getString("note").ifBlank { null } else null,
                    imagePath = if (obj.has("imagePath")) obj.getString("imagePath").ifBlank { null } else null,
                    type = obj.optString("type", "EXPENSE"),
                    currencyCode = cur,
                    accountId = accId,
                    kind = obj.optString("kind", "REGULAR"),
                    goalId = if (gId > 0L) gId else null,
                    recurringRuleId = if (rId > 0L) rId else null,
                    originalCurrencyCode = originalCurrencyCode,
                    originalAmountMinor = originalAmountMinor
                )
            }

            // Parse budgets
            val rawBudgets = payloadObj.optJSONArray("budgets") ?: JSONArray()
            val budgets = (0 until rawBudgets.length()).map { idx ->
                val obj = rawBudgets.getJSONObject(idx)
                val cur = obj.optString("currencyCode", "INR")
                val limitMinor = if (obj.has("amountLimitMinor")) obj.getLong("amountLimitMinor") else Money.fromDouble(obj.optDouble("amountLimit", 0.0), cur)
                Budget(
                    id = obj.optLong("id", 0L),
                    category = obj.optString("category", "General"),
                    amountLimit = Money.toDouble(limitMinor, cur),
                    amountLimitMinor = limitMinor,
                    monthYear = obj.optString("monthYear", "ALL"),
                    currencyCode = cur
                )
            }

            // Parse goals
            val rawGoals = payloadObj.optJSONArray("savingsGoals") ?: JSONArray()
            val goals = (0 until rawGoals.length()).map { idx ->
                val obj = rawGoals.getJSONObject(idx)
                val cur = obj.optString("currencyCode", "INR")
                val targetMinor = if (obj.has("targetAmountMinor")) obj.getLong("targetAmountMinor") else Money.fromDouble(obj.optDouble("targetAmount", 0.0), cur)
                val curMinor = if (obj.has("currentAmountMinor")) obj.getLong("currentAmountMinor") else Money.fromDouble(obj.optDouble("currentAmount", 0.0), cur)
                val contribMinor = if (obj.has("contributionAmountMinor")) obj.getLong("contributionAmountMinor") else Money.fromDouble(obj.optDouble("contributionAmount", 0.0), cur)
                SavingsGoal(
                    id = obj.optLong("id", 0L),
                    name = obj.optString("name", "Goal").ifBlank { "Savings Goal" },
                    targetAmount = Money.toDouble(targetMinor, cur),
                    currentAmount = Money.toDouble(curMinor, cur),
                    targetAmountMinor = targetMinor,
                    currentAmountMinor = curMinor,
                    targetDate = obj.optLong("targetDate", 0L),
                    frequency = obj.optString("frequency", "MONTHLY"),
                    contributionAmount = Money.toDouble(contribMinor, cur),
                    contributionAmountMinor = contribMinor,
                    isAutoGap = obj.optBoolean("isAutoGap", true),
                    iconTag = obj.optString("iconTag", "🎯"),
                    category = obj.optString("category", "Savings"),
                    imageUri = if (obj.has("imageUri")) obj.getString("imageUri").ifBlank { null } else null,
                    currencyCode = cur
                )
            }

            // Parse reminders
            val rawReminders = payloadObj.optJSONArray("reminders") ?: JSONArray()
            val reminders = (0 until rawReminders.length()).map { idx ->
                val obj = rawReminders.getJSONObject(idx)
                ReminderEntity(
                    id = obj.optLong("id", 0L),
                    text = obj.optString("text", "Reminder"),
                    dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            }

            val goalIds = goals.map { it.id }.toSet()
            val recurringRuleIds = rules.map { it.id }.toSet()
            if (goalIds.size != goals.size || goals.any { it.id <= 0L }) {
                throw IllegalArgumentException("Snapshot contains duplicate or invalid savings-goal IDs")
            }
            if (rules.map { it.id }.toSet().size != rules.size || rules.any { it.id <= 0L }) {
                throw IllegalArgumentException("Snapshot contains duplicate or invalid recurring-rule IDs")
            }
            expenses.forEachIndexed { idx, expense ->
                if (expense.goalId != null && expense.goalId !in goalIds) {
                    throw IllegalArgumentException("Expense at index $idx references missing savings goal ${expense.goalId}")
                }
                if (expense.recurringRuleId != null && expense.recurringRuleId !in recurringRuleIds) {
                    throw IllegalArgumentException("Expense at index $idx references missing recurring rule ${expense.recurringRuleId}")
                }
            }

            // Validate metadata counts before changing the live database.
            val counts = payloadObj.optJSONObject("meta")?.optJSONObject("counts")
            val expectedCounts = mapOf(
                "accounts" to accounts.size,
                "expenses" to expenses.size,
                "budgets" to budgets.size,
                "savingsGoals" to goals.size,
                "reminders" to reminders.size,
                "recurringRules" to rules.size
            )
            if (formatVersion >= 2 && counts == null) {
                throw IllegalArgumentException("Snapshot is missing integrity record counts")
            }
            counts?.let {
                expectedCounts.forEach { (key, actual) ->
                    if (formatVersion >= 2 && !it.has(key)) {
                        throw IllegalArgumentException("Snapshot integrity metadata is missing $key count")
                    }
                    if (it.has(key) && it.optInt(key, -1) != actual) {
                        throw IllegalArgumentException("Snapshot count mismatch for $key")
                    }
                }
            }

            // Mandatory Pre-restore Safety Snapshot
            val safetySnapshotCreated = createSnapshot(context, db)
            if (!safetySnapshotCreated) {
                Log.e(TAG, "Pre-restore safety snapshot failed! Aborting restoration.")
                return false
            }

            // Atomic transactional restore
            val dao = db.financeDao()
            db.withTransaction {
                dao.clearAllData()
                accounts.forEach { dao.insertAccount(it) }
                rules.forEach { dao.insertRecurringRule(it) }
                expenses.forEach { dao.insertExpense(it) }
                budgets.forEach { dao.insertBudget(it) }
                goals.forEach { dao.insertSavingsGoal(it) }
                reminders.forEach { dao.insertReminder(it) }

                // Verification of restored record counts
                val restored = RecoveryDatabaseSnapshot(
                    accounts = dao.getAccountsSnapshot(),
                    expenses = dao.getExpensesSnapshot(),
                    budgets = dao.getBudgetsSnapshot(),
                    goals = dao.getSavingsGoalsSnapshot(),
                    reminders = dao.getRemindersSnapshot(),
                    rules = dao.getRecurringRulesSnapshot()
                )
                val expectedCounts = listOf(accounts.size, expenses.size, budgets.size, goals.size, reminders.size, rules.size)
                val actualCounts = listOf(restored.accounts.size, restored.expenses.size, restored.budgets.size, restored.goals.size, restored.reminders.size, restored.rules.size)
                if (actualCounts != expectedCounts) {
                    throw IllegalStateException("Record count mismatch after restore. Expected $expectedCounts but found $actualCounts")
                }
            }

            // Restore settings
            val settings = payloadObj.optJSONObject("settings")
            val currencyCode = settings?.optString("currencyCode", "INR") ?: "INR"
            val currencySymbol = settings?.optString("currencySymbol", "₹") ?: "₹"
            val currencyName = settings?.optString("currencyName", "Indian Rupee") ?: "Indian Rupee"
            val monthlyBudget = settings?.optDouble("monthlyBudget", 0.0) ?: 0.0

            val prefs = context.getSharedPreferences(AppSettingsManager.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("currency_code", currencyCode)
                .putString("currency_symbol", currencySymbol)
                .putString("currency_name", currencyName)
                .putFloat("monthly_budget", monthlyBudget.toFloat())
                .apply()

            onCurrencyRestored(currencyCode, currencySymbol, currencyName, monthlyBudget)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Restoration failed for snapshot $fileName", e)
            false
        }
    }

    fun deleteSnapshot(context: Context, fileName: String): Boolean {
        val file = File(getSnapshotDir(context), fileName)
        return if (file.exists()) file.delete() else false
    }

    fun pruneOldSnapshots(context: Context, maxToKeep: Int) {
        val dir = getSnapshotDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return
        if (files.size <= maxToKeep) return
        files.sortBy { it.lastModified() }
        val toDelete = files.size - maxToKeep
        for (i in 0 until toDelete) {
            files[i].delete()
        }
    }
}
