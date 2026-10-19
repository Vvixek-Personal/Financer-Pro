package com.example.ui

import com.example.ui.theme.*
import android.content.Context
import androidx.work.*
import com.example.data.FinanceDatabase
import com.example.data.RecurringProcessor
import java.util.concurrent.TimeUnit

class RecurringTransactionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = FinanceDatabase.getDatabase(applicationContext)
            RecurringProcessor.processRecurringRules(applicationContext, db)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "recurring_transaction_daily_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
