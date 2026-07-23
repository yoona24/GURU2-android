package com.guru2.payday.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.guru2.payday.data.local.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseNotificationSchedulerTest {
    @Test
    fun fixedExpenseNotificationCanBeScheduledAndCancelled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expenseId = 99L
        ExpenseNotificationScheduler.schedule(
            context,
            ExpenseEntity(
                id = expenseId,
                type = ExpenseEntity.TYPE_FIXED,
                name = "Netflix",
                amount = 12_000,
                category = "여가",
                paymentMethod = "Card",
                paymentDate = "2026-07-24",
                recurringDay = 25,
                recurrence = ExpenseEntity.RECURRENCE_MONTHLY,
                nextPaymentDate = "2026-12-25",
            ),
        )

        val workManager = WorkManager.getInstance(context)
        val scheduled = workManager
            .getWorkInfosForUniqueWork("expense-payment-$expenseId")
            .get()
        assertEquals(1, scheduled.size)
        assertEquals(WorkInfo.State.ENQUEUED, scheduled.single().state)

        ExpenseNotificationScheduler.cancel(context, expenseId)
        workManager.getWorkInfosForUniqueWork("expense-payment-$expenseId").get()
    }
}
