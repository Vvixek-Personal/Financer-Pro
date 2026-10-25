package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

sealed class CloudSyncResult {
    data class Success(val message: String) : CloudSyncResult()
    data class Failure(val error: String, val isOffline: Boolean = false) : CloudSyncResult()
    data class Conflict(val message: String, val localCount: Int, val remoteCount: Int) : CloudSyncResult()
}

sealed class SyncDirection {
    object ToCloud : SyncDirection()
    object FromCloud : SyncDirection()
    object Merge : SyncDirection()
}

class CloudSyncManager private constructor(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _lastSyncTimestamp = MutableStateFlow("Never")
    val lastSyncTimestamp: StateFlow<String> = _lastSyncTimestamp.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: CloudSyncManager? = null
        fun getInstance(context: Context): CloudSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudSyncManager(context).also { INSTANCE = it }
            }
        }
    }

    private fun formatTimestamp(ts: Long): String {
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ts))
    }

    private fun friendlyErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            e is java.net.UnknownHostException || e is java.net.SocketTimeoutException || msg.contains("UNAVAILABLE", ignoreCase = true) ->
                "Network connection unavailable. Please check your internet connection."
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Access denied to cloud vault. Please sign in again."
            msg.contains("UNAUTHENTICATED", ignoreCase = true) ->
                "Authentication expired. Please sign in again."
            else -> "Cloud operation could not be completed. Please try again."
        }
    }

    suspend fun performSync(
        database: FinanceDatabase,
        currencyCode: String,
        currencySymbol: String,
        currencyName: String,
        monthlyBudget: Double,
        direction: SyncDirection = SyncDirection.Merge
    ): CloudSyncResult {
        val user = auth.currentUser ?: return CloudSyncResult.Failure("User not signed in. Please sign in to sync cloud data.")
        val userId = user.uid
        val dao = database.financeDao()

        return try {
            val userDocRef = firestore.collection("users").document(userId)

            // Fetch local snapshots
            val expenses = dao.getExpensesSnapshot()
            val accounts = dao.getAccountsSnapshot()
            val budgets = dao.getBudgetsSnapshot()
            val goals = dao.getSavingsGoalsSnapshot()
            val recurringRules = dao.getRecurringRulesSnapshot()
            val reminders = dao.getRemindersSnapshot()

            val localTotalCount = expenses.size + accounts.size + budgets.size + 
                goals.size + recurringRules.size + reminders.size

            // Check if local database is empty - prevent accidental cloud deletion
            if (localTotalCount == 0) {
                return CloudSyncResult.Failure(
                    "Local database is empty. Refusing to sync to prevent accidental cloud data deletion. " +
                    "Please restore from cloud first or verify your local data."
                )
            }

            // Fetch remote counts for conflict detection
            val remoteCounts = mutableMapOf<String, Int>()
            val subcollectionNames = listOf("expenses", "accounts", "budgets", "goals", "recurringRules", "reminders")
            
            for (subcollectionName in subcollectionNames) {
                try {
                    val remoteSnapshot = userDocRef.collection(subcollectionName).get().await()
                    remoteCounts[subcollectionName] = remoteSnapshot.documents.size
                } catch (_: Exception) {
                    remoteCounts[subcollectionName] = 0
                }
            }

            val remoteTotalCount = remoteCounts.values.sum()

            // Detect potential data loss scenarios
            if (direction == SyncDirection.Merge && remoteTotalCount > 0 && localTotalCount == 0) {
                return CloudSyncResult.Conflict(
                    "Cloud has data but local database is empty. Use restore from cloud instead.",
                    localCount = localTotalCount,
                    remoteCount = remoteTotalCount
                )
            }

            // Settings only stored in users/{uid}
            val settingsData = mutableMapOf<String, Any>()
            settingsData["currencyCode"] = currencyCode
            settingsData["currencySymbol"] = currencySymbol
            settingsData["currencyName"] = currencyName
            settingsData["monthlyBudget"] = monthlyBudget
            val now = System.currentTimeMillis()
            settingsData["lastSync"] = now

            userDocRef.set(settingsData, SetOptions.merge()).await()

            val subcollections = listOf(
                "expenses" to expenses.map { it.id.toString() to it.toMap() },
                "accounts" to accounts.map { it.id.toString() to it.toMap() },
                "budgets" to budgets.map { it.id.toString() to it.toMap() },
                "goals" to goals.map { it.id.toString() to it.toMap() },
                "recurringRules" to recurringRules.map { it.id.toString() to it.toMap() },
                "reminders" to reminders.map { it.id.toString() to it.toMap() }
            )

            val batchSets = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any?>>>()
            val batchDeletes = mutableListOf<com.google.firebase.firestore.DocumentReference>()

            for ((subcollectionName, items) in subcollections) {
                val localIds = items.map { it.first }.toSet()
                
                // Only delete remote records if we're explicitly syncing to cloud
                // and local data exists for this collection
                if (direction == SyncDirection.ToCloud || direction == SyncDirection.Merge) {
                    if (items.isNotEmpty()) {
                        try {
                            val remoteSnapshot = userDocRef.collection(subcollectionName).get().await()
                            for (remoteDoc in remoteSnapshot.documents) {
                                if (remoteDoc.id !in localIds) {
                                    batchDeletes.add(remoteDoc.reference)
                                }
                            }
                        } catch (_: Exception) {
                            // Non-fatal if offline/empty collection
                        }
                    }
                }

                for ((docId, itemData) in items) {
                    val docRef = userDocRef.collection(subcollectionName).document(docId)
                    batchSets.add(docRef to itemData)
                }
            }

            // Write in WriteBatch chunks of max 400
            val allOperations: MutableList<suspend (com.google.firebase.firestore.WriteBatch) -> Unit> = mutableListOf()
            
            // Only perform deletes if we have local data to prevent accidental cloud wipe
            if (localTotalCount > 0) {
                for (delRef in batchDeletes) {
                    allOperations.add { batch -> batch.delete(delRef) }
                }
            }
            for ((docRef, data) in batchSets) {
                allOperations.add { batch -> batch.set(docRef, data, SetOptions.merge()) }
            }

            for (chunk in allOperations.chunked(400)) {
                val batch = firestore.batch()
                for (op in chunk) {
                    op(batch)
                }
                batch.commit().await()
            }

            _lastSyncTimestamp.value = formatTimestamp(now)
            CloudSyncResult.Success("Cloud Sync Complete")
        } catch (e: Exception) {
            CloudSyncResult.Failure(friendlyErrorMessage(e))
        }
    }

    suspend fun restoreFromCloud(
        database: FinanceDatabase,
        currencyCallback: (String, String, String, Double) -> Unit,
        forceRestore: Boolean = false
    ): CloudSyncResult {
        val user = auth.currentUser ?: return CloudSyncResult.Failure("User not signed in. Please sign in to restore cloud data.")
        val userId = user.uid
        val dao = database.financeDao()

        return try {
            val userDocRef = firestore.collection("users").document(userId)
            val doc = userDocRef.get().await()
            if (!doc.exists()) return CloudSyncResult.Failure("No cloud data found in your cloud vault.")

            // Check if local database has data - warn user before overwriting
            val localCounts = dao.getDatabaseCounts()
            val localTotal = localCounts.values.sum()
            
            if (localTotal > 0 && !forceRestore) {
                return CloudSyncResult.Conflict(
                    "Local database contains $localTotal records. Use forceRestore=true to overwrite, or backup first.",
                    localCount = localTotal,
                    remoteCount = 0
                )
            }

            val currencyCode = doc.getString("currencyCode") ?: "INR"
            val currencySymbol = doc.getString("currencySymbol") ?: "₹"
            val currencyName = doc.getString("currencyName") ?: "Indian Rupee"
            val monthlyBudget = doc.getDouble("monthlyBudget")
                ?: doc.getDouble("monthly_budget")
                ?: 0.0

            // Try reading from subcollections
            val accountsSub = userDocRef.collection("accounts").get().await()
            val expensesSub = userDocRef.collection("expenses").get().await()
            val budgetsSub = userDocRef.collection("budgets").get().await()
            val goalsSub = userDocRef.collection("goals").get().await()
            val rulesSub = userDocRef.collection("recurringRules").get().await()
            val remindersSub = userDocRef.collection("reminders").get().await()

            val hasSubcollections = !accountsSub.isEmpty || !expensesSub.isEmpty || !budgetsSub.isEmpty ||
                !goalsSub.isEmpty || !rulesSub.isEmpty || !remindersSub.isEmpty

            val parsedAccounts: List<Account>
            val parsedExpenses: List<Expense>
            val parsedBudgets: List<Budget>
            val parsedGoals: List<SavingsGoal>
            val parsedRules: List<RecurringRule>
            val parsedReminders: List<ReminderEntity>

            if (hasSubcollections) {
                parsedAccounts = accountsSub.documents.mapNotNull { it.data?.toAccount() }
                parsedExpenses = expensesSub.documents.mapNotNull { it.data?.toExpense() }
                parsedBudgets = budgetsSub.documents.mapNotNull { it.data?.toBudget() }
                parsedGoals = goalsSub.documents.mapNotNull { it.data?.toSavingsGoal() }
                parsedRules = rulesSub.documents.mapNotNull { it.data?.toRecurringRule() }
                parsedReminders = remindersSub.documents.mapNotNull { it.data?.toReminderEntity() }
            } else {
                // Fallback to legacy array format in users/{uid} doc
                @Suppress("UNCHECKED_CAST")
                val rawAccounts = doc.get("accounts") as? List<Map<String, Any?>> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val rawExpenses = doc.get("expenses") as? List<Map<String, Any?>> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val rawBudgets = doc.get("budgets") as? List<Map<String, Any?>> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val rawGoals = doc.get("goals") as? List<Map<String, Any?>> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val rawRules = doc.get("recurringRules") as? List<Map<String, Any?>> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val rawReminders = doc.get("reminders") as? List<Map<String, Any?>> ?: emptyList()

                parsedAccounts = rawAccounts.map { it.toAccount() }
                parsedExpenses = rawExpenses.map { it.toExpense() }
                parsedBudgets = rawBudgets.map { it.toBudget() }
                parsedGoals = rawGoals.map { it.toSavingsGoal() }
                parsedRules = rawRules.map { it.toRecurringRule() }
                parsedReminders = rawReminders.map { it.toReminderEntity() }
            }

            // Validate we have data to restore
            if (parsedAccounts.isEmpty() && parsedExpenses.isEmpty() && 
                parsedBudgets.isEmpty() && parsedGoals.isEmpty()) {
                return CloudSyncResult.Failure("No valid data found in cloud backup.")
            }

            // Take a pre-restore safety copy of SQLite database file
            PreMigrationBackup.backupDatabaseBeforeMigration(context, DATABASE_NAME)

            database.withTransaction {
                dao.clearAllData()
                // Insert accounts first to satisfy foreign key constraints
                parsedAccounts.forEach { dao.insertAccount(it) }
                parsedExpenses.forEach { dao.insertExpense(it) }
                parsedBudgets.forEach { dao.insertBudget(it) }
                parsedGoals.forEach { dao.insertSavingsGoal(it) }
                parsedRules.forEach { dao.insertRecurringRule(it) }
                parsedReminders.forEach { dao.insertReminder(it) }
            }

            currencyCallback(currencyCode, currencySymbol, currencyName, monthlyBudget)
            val lastSync = doc.getLong("lastSync") ?: System.currentTimeMillis()
            _lastSyncTimestamp.value = formatTimestamp(lastSync)
            CloudSyncResult.Success("Cloud Restore Complete: Restored ${parsedExpenses.size} transactions, ${parsedAccounts.size} accounts, ${parsedBudgets.size} budgets, and ${parsedGoals.size} goals.")
        } catch (e: Exception) {
            CloudSyncResult.Failure(friendlyErrorMessage(e))
        }
    }

    suspend fun getRemoteDataCount(): CloudSyncResult {
        val user = auth.currentUser ?: return CloudSyncResult.Failure("User not signed in.")
        val userId = user.uid
        
        return try {
            val userDocRef = firestore.collection("users").document(userId)
            val doc = userDocRef.get().await()
            
            if (!doc.exists()) {
                return CloudSyncResult.Failure("No cloud data found.")
            }

            val subcollectionNames = listOf("expenses", "accounts", "budgets", "goals", "recurringRules", "reminders")
            var totalCount = 0
            
            for (subcollectionName in subcollectionNames) {
                try {
                    val snapshot = userDocRef.collection(subcollectionName).get().await()
                    totalCount += snapshot.documents.size
                } catch (_: Exception) {
                    // Continue with other collections
                }
            }
            
            CloudSyncResult.Success("Cloud contains $totalCount records across all collections")
        } catch (e: Exception) {
            CloudSyncResult.Failure(friendlyErrorMessage(e))
        }
    }
}
