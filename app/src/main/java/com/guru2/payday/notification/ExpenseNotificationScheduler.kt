package com.guru2.payday.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.guru2.payday.data.local.ExpenseEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ExpenseNotificationScheduler {
    private const val WORK_PREFIX = "expense-payment-"

    fun schedule(context: Context, expense: ExpenseEntity) {
        val paymentDate = expense.nextPaymentDate?.let(LocalDate::parse) ?: return
        val notificationTime = LocalDateTime.of(
            paymentDate.minusDays(1),
            LocalTime.of(9, 0),
        )
        val now = LocalDateTime.now()
        val effectiveTime = if (notificationTime.isAfter(now)) notificationTime else now
        val delay = Duration.between(now, effectiveTime)
        val input = Data.Builder()
            .putLong(ExpenseNotificationWorker.KEY_EXPENSE_ID, expense.id)
            .putString(ExpenseNotificationWorker.KEY_NAME, expense.name)
            .putLong(ExpenseNotificationWorker.KEY_AMOUNT, expense.personalAmount)
            .putString(ExpenseNotificationWorker.KEY_NEXT_DATE, paymentDate.toString())
            .putString(ExpenseNotificationWorker.KEY_RECURRENCE, expense.recurrence)
            .build()
        val request = OneTimeWorkRequestBuilder<ExpenseNotificationWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(expense.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, expenseId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(expenseId))
    }

    private fun workName(expenseId: Long) = "$WORK_PREFIX$expenseId"
}
