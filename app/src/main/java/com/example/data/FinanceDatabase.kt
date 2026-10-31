package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

private const val TAG = "FinanceDatabase"

const val FINANCE_DB_VERSION = 14
const val DATABASE_NAME = "finance_database"

@Database(
    entities = [
        Expense::class,
        Account::class,
        Budget::class,
        SavingsGoal::class,
        RecurringRule::class,
        ReminderEntity::class
    ],
    version = FINANCE_DB_VERSION,
    exportSchema = true
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(context: Context, defaultCurrency: String? = null): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                // The second read is essential: another thread may have initialized Room
                // while this caller was waiting to enter the monitor.
                INSTANCE ?: run {
                    val appContext = context.applicationContext
                    val effectiveCurrency = defaultCurrency ?: try {
                        appContext.getSharedPreferences(AppSettingsManager.PREFS_NAME, Context.MODE_PRIVATE)
                            .getString("currency_code", "INR") ?: "INR"
                    } catch (_: Exception) {
                        "INR"
                    }

                    // Pre-migration safety backup: version-guarded before Room.databaseBuilder.
                    try {
                        val dbFile = appContext.getDatabasePath(DATABASE_NAME)
                        if (dbFile.exists()) {
                            var existingVersion = 0
                            try {
                                val helperDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                                    dbFile.path,
                                    null,
                                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                                )
                                existingVersion = helperDb.version
                                helperDb.close()
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to read database version before migration", e)
                            }

                            if (existingVersion in 1 until FINANCE_DB_VERSION) {
                                PreMigrationBackup.backupDatabaseBeforeMigration(appContext, DATABASE_NAME)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Pre-migration backup check failed: ${e.message}")
                    }

                    Room.databaseBuilder(
                        appContext,
                        FinanceDatabase::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(*getFinanceDbMigrations(effectiveCurrency))
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }
    }
}
