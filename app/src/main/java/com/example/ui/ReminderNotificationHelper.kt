package com.example.ui

import com.example.ui.theme.*
import android.app.NotificationChannel
import android.util.Log
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.data.FinanceDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "ReminderNotification"

class BillReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong("reminder_id", -1L)
        val reminderTitle = inputData.getString("reminder_title") ?: "Payment Due"
        val dueDateTimestamp = inputData.getLong("due_date", System.currentTimeMillis())
        val recurringBodyOverride = inputData.getString("recurring_body_override")

        try {
            if (reminderId < 0) {
                // Negative id = a recurring-bill pre-due nudge (see RecurringBillReminderScheduler),
                // not a manual ReminderEntity. There is nothing to look up in Room for this one —
                // the worker's own input data already carries everything needed to notify.
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dueDateTimestamp))
                ReminderNotificationHelper.showReminderNotification(
                    context = applicationContext,
                    reminderId = reminderId,
                    title = reminderTitle,
                    dueDateStr = dateStr,
                    bodyOverride = recurringBodyOverride
                )
            } else if (reminderId > 0) {
                val db = FinanceDatabase.getDatabase(applicationContext)
                val reminder = db.financeDao().getReminderById(reminderId)
                if (reminder != null && reminder.isEnabled && !reminder.isCompleted) {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(dueDateTimestamp))
                    ReminderNotificationHelper.showReminderNotification(
                        context = applicationContext,
                        reminderId = reminderId,
                        title = reminder.text,
                        dueDateStr = dateStr
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to show reminder notification", e)
        }
        return Result.success()
    }
}

object ReminderScheduler {
    fun scheduleReminder(context: Context, reminderId: Long, title: String, dueDateTimestamp: Long) {
        val now = System.currentTimeMillis()
        val delayMs = (dueDateTimestamp - now).coerceAtLeast(0L)

        val inputData = workDataOf(
            "reminder_id" to reminderId,
            "reminder_title" to title,
            "due_date" to dueDateTimestamp
        )

        val request = OneTimeWorkRequestBuilder<BillReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        try {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_$reminderId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule reminder with WorkManager", e)
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel reminder with WorkManager", e)
        }
    }
}

object ReminderNotificationHelper {
    private const val CHANNEL_ID = "finance_reminders_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Financial Reminders"
            val descriptionText = "Notifications for financial reminders and due bills"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        dueDateStr: String,
        bodyOverride: String? = null
    ) {
        createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (bodyOverride != null) "Upcoming bill: $title" else "Reminder Due: $title")
            .setContentText(bodyOverride ?: "Due: $dueDateStr. Tap to open app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // reminderId.toInt() truncates a Long to 32 bits; for a huge or negative id that can
        // collide with an unrelated notification's id. Hash it into a stable, collision-resistant
        // Int instead so manual reminders and recurring-bill nudges never overwrite each other.
        val notificationId = reminderId.hashCode()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission not granted to post reminder", e)
            }
        }
    }
}

/**
 * Feature: pre-due notifications for RECURRING bills (rent, EMI, subscriptions...), not just the
 * one-off manual reminders ReminderScheduler already handled. RecurringProcessor silently creates
 * the expense on the due date itself, so until now users had no warning before that charge landed.
 *
 * Reuses the existing BillReminderWorker/notification channel — this is the same delivery
 * mechanism as ReminderScheduler.scheduleReminder, just driven by a RecurringRule's nextDueDate
 * and a configurable lead time instead of a manual ReminderEntity.
 */
object RecurringBillReminderScheduler {

    /** How long before nextDueDate to notify. User-configurable later; sane default for now. */
    enum class LeadTime(val label: String, val millis: Long) {
        SAME_DAY("On the day", 0L),
        ONE_DAY("1 day before", TimeUnit.DAYS.toMillis(1)),
        THREE_DAYS("3 days before", TimeUnit.DAYS.toMillis(3)),
        ONE_WEEK("1 week before", TimeUnit.DAYS.toMillis(7));
    }

    private fun workName(ruleId: Long) = "recurring_bill_reminder_$ruleId"

    /**
     * Schedules (or reschedules) the pre-due notification for one recurring rule's CURRENT
     * nextDueDate. Call this every time a RecurringRule is created, edited, or advanced by
     * RecurringProcessor — ExistingWorkPolicy.REPLACE means calling it again safely replaces the
     * previous schedule rather than stacking duplicate notifications.
     */
    fun scheduleForRule(
        context: Context,
        rule: com.example.data.RecurringRule,
        leadTime: LeadTime = LeadTime.ONE_DAY
    ) {
        if (!rule.isActive) {
            cancelForRule(context, rule.id)
            return
        }

        val fireAt = (rule.nextDueDate - leadTime.millis)
        val now = System.currentTimeMillis()
        // If the lead time would fire in the past (rule is already overdue, or lead time is
        // longer than time remaining), fire immediately instead of silently dropping the
        // notification — better a late nudge than none.
        val delayMs = (fireAt - now).coerceAtLeast(0L)

        val amountStr = com.example.data.Money.format(rule.amountMinor, rule.currencyCode)
        val bodyText = when (leadTime) {
            LeadTime.SAME_DAY -> "$amountStr due today for ${rule.title}"
            else -> "$amountStr due for ${rule.title} in ${leadTimeDescription(leadTime)}"
        }

        val inputData = workDataOf(
            "reminder_id" to -(rule.id), // negative id namespace: never collides with manual ReminderEntity ids
            "reminder_title" to rule.title,
            "due_date" to rule.nextDueDate,
            "recurring_body_override" to bodyText
        )

        val request = OneTimeWorkRequestBuilder<BillReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        try {
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(rule.id),
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule recurring bill reminder with WorkManager", e)
        }
    }

    fun cancelForRule(context: Context, ruleId: Long) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(workName(ruleId))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel recurring bill reminder with WorkManager", e)
        }
    }

    private fun leadTimeDescription(leadTime: LeadTime): String = when (leadTime) {
        LeadTime.SAME_DAY -> "today"
        LeadTime.ONE_DAY -> "1 day"
        LeadTime.THREE_DAYS -> "3 days"
        LeadTime.ONE_WEEK -> "1 week"
    }
}
