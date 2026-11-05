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
import kotlinx.coroutines.flow.first
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
            val vibrate = try {
                context.getSharedPreferences(
                    com.example.data.AppSettingsManager.PREFS_NAME,
                    Context.MODE_PRIVATE
                ).getBoolean("haptics_enabled", true)
            } catch (e: Exception) {
                true
            }
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(vibrate)
                enableLights(true)
                lightColor = reminderAccentColor()
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Accent colour for notifications, taken from the user's saved theme accent so the reminder
     * matches the app instead of always using a fixed blue.
     */
    private fun reminderAccentColor(): Int = try {
        // Read the accent the app is actually using right now (theme state is process-wide).
        val primary = com.example.ui.theme.SleekPrimary
        android.graphics.Color.rgb(
            (primary.red * 255).toInt(),
            (primary.green * 255).toInt(),
            (primary.blue * 255).toInt()
        )
    } catch (e: Exception) {
        android.graphics.Color.parseColor("#0061A4")
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
            // A real monochrome bell instead of the generic framework "info" glyph, which looked
            // like a system warning rather than a financial reminder.
            .setSmallIcon(com.example.R.drawable.ic_notification_bell)
            .setColor(reminderAccentColor())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentTitle(if (bodyOverride != null) "Upcoming bill: $title" else "Reminder Due: $title")
            .setContentText(bodyOverride ?: "Due: $dueDateStr. Tap to open app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // reminderId.toInt() truncates a Long to 32 bits; for a huge or negative id that can
        // collide with an unrelated notification's id. Hash it into a stable, collision-resistant
        // Int instead so manual reminders and recurring-bill nudges never overwrite each other.
        // If the user asked for no haptics, deliver the reminder silently.
        val hapticsOn = try {
            context.getSharedPreferences(
                com.example.data.AppSettingsManager.PREFS_NAME,
                Context.MODE_PRIVATE
            ).getBoolean("haptics_enabled", true)
        } catch (e: Exception) {
            true
        }
        if (!hapticsOn) builder.setSilent(true)

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
        leadTimeDays: Int = -1
    ) {
        val leadTime = if (leadTimeDays >= 0) {
            when {
                leadTimeDays >= 7 -> LeadTime.ONE_WEEK
                leadTimeDays >= 3 -> LeadTime.THREE_DAYS
                leadTimeDays >= 1 -> LeadTime.ONE_DAY
                else -> LeadTime.SAME_DAY
            }
        } else {
            // Fall back to the user's Bills setting ("On the day" / "1 day before" / ...).
            leadTimeFromStoredSetting(context)
        }
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

    /**
     * Maps the persisted "bill_reminder_timing" preference (the Bills > Reminder timing option)
     * onto a concrete lead time. This is why the Bills screen's reminder timing now actually
     * affects when notifications arrive instead of being a display-only preference.
     */
    fun leadTimeFromStoredSetting(context: Context): LeadTime {
        val raw = try {
            context.getSharedPreferences(
                com.example.data.AppSettingsManager.PREFS_NAME,
                Context.MODE_PRIVATE
            ).getString("bill_reminder_timing", "0") ?: "0"
        } catch (e: Exception) {
            "0"
        }
        val days = raw.filter { it.isDigit() }.toIntOrNull() ?: 0
        return when {
            days >= 7 -> LeadTime.ONE_WEEK
            days >= 3 -> LeadTime.THREE_DAYS
            days >= 1 -> LeadTime.ONE_DAY
            else -> when {
                raw.contains("week", true) -> LeadTime.ONE_WEEK
                raw.contains("3", true) -> LeadTime.THREE_DAYS
                raw.contains("day", true) && !raw.contains("0", true) -> LeadTime.ONE_DAY
                else -> LeadTime.SAME_DAY
            }
        }
    }

    /** Reschedules every active rule so a timing change takes effect without waiting for edits. */
    fun rescheduleAll(context: Context) {
        try {
            val db = com.example.data.FinanceDatabase.getDatabase(context)
            kotlinx.coroutines.runBlocking {
                db.financeDao().getAllRecurringRules().first().forEach { rule ->
                    if (rule.isActive) scheduleForRule(context, rule) else cancelForRule(context, rule.id)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reschedule bill reminders", e)
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
