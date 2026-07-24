package com.guru2.payday.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.PaydayDatabase
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseNotificationSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun grantNotificationPermission() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun fixedExpenseNotificationCanBeScheduledAndCancelled() {
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

    @Test
    fun workerShowsExpectedMessageReschedulesAndOpensExpenseList() = runBlocking {
        val dao = PaydayDatabase.getInstance(context).expenseDao()
        val today = LocalDate.now()
        val expenseId = dao.insert(
            ExpenseEntity(
                type = ExpenseEntity.TYPE_FIXED,
                name = "Netflix",
                amount = 12_000,
                category = "여가",
                paymentMethod = "현대카드",
                paymentDate = today.toString(),
                recurringDay = today.dayOfMonth,
                recurrence = ExpenseEntity.RECURRENCE_MONTHLY,
                nextPaymentDate = today.toString(),
            ),
        )
        val expense = requireNotNull(dao.getById(expenseId))
        ExpenseNotificationScheduler.schedule(context, expense)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        var notification = notificationManager.activeNotifications
            .firstOrNull { it.id == expenseId.toInt() }
            ?.notification
        repeat(20) {
            if (notification != null) return@repeat
            SystemClock.sleep(250)
            notification = notificationManager.activeNotifications
                .firstOrNull { it.id == expenseId.toInt() }
                ?.notification
        }

        assertNotNull(notification)
        assertEquals(
            "Netflix 결제 하루 전이에요 · 12,000원 예정",
            notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
        notification?.contentIntent?.send()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertTrue(
            readShellOutput("dumpsys activity activities")
                .contains("ExpenseListActivity"),
        )

        var updated = dao.getById(expenseId)
        repeat(20) {
            if (updated?.nextPaymentDate != today.toString()) return@repeat
            SystemClock.sleep(250)
            updated = dao.getById(expenseId)
        }
        assertEquals(today.plusMonths(1), LocalDate.parse(updated?.nextPaymentDate))

        dao.delete(requireNotNull(updated))
        ExpenseNotificationScheduler.cancel(context, expenseId)
        notificationManager.cancel(expenseId.toInt())
    }

    private fun readShellOutput(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
        return descriptor.use {
            java.io.FileInputStream(it.fileDescriptor).bufferedReader().readText()
        }
    }
}
