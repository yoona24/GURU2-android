package com.guru2.payday.ui.expense

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.guru2.payday.R
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.notification.ExpenseNotificationScheduler
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseEditFlowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dao by lazy { PaydayDatabase.getInstance(context).expenseDao() }

    @Before
    fun grantNotificationPermission() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun editRecalculatesExpenseAndDeleteRemovesExpenseAndWork() = runBlocking {
        val id = dao.insert(
            ExpenseEntity(
                type = ExpenseEntity.TYPE_FIXED,
                name = "NetflixTest",
                amount = 17_000,
                category = "여가",
                paymentMethod = "현대카드",
                paymentDate = LocalDate.now().toString(),
                recurringDay = 25,
                recurrence = ExpenseEntity.RECURRENCE_MONTHLY,
                nextPaymentDate = LocalDate.now().plusMonths(1).withDayOfMonth(25).toString(),
            ),
        )
        val original = requireNotNull(dao.getById(id))
        ExpenseNotificationScheduler.schedule(context, original)

        val editIntent = Intent(context, ExpenseAddActivity::class.java).apply {
            action = Intent.ACTION_EDIT
            putExtra(ExpenseAddActivity.EXTRA_EXPENSE_ID, id)
        }
        ActivityScenario.launch<ExpenseAddActivity>(editIntent).use { scenario ->
            SystemClock.sleep(500)
            onView(withText(R.string.expense_edit_title)).check(matches(isDisplayed()))
            onView(withId(R.id.deleteButton)).check(matches(isDisplayed()))
            onView(withId(R.id.expenseNameInput)).check(matches(withText(original.name)))
            onView(withId(R.id.expenseAmountInput)).perform(replaceText("24000"))
            scenario.onActivity {
                it.findViewById<View>(R.id.shareSwitch).performClick()
                it.findViewById<View>(R.id.increaseShareButton).performClick()
                it.findViewById<TextView>(R.id.recurringDayInput).text = "28일"
            }
            onView(withId(R.id.saveButton)).perform(click())
            SystemClock.sleep(600)
        }

        val updated = requireNotNull(dao.getById(id))
        assertEquals(24_000L, updated.amount)
        assertEquals(3, updated.shareCount)
        assertEquals(8_000L, updated.personalAmount)
        assertEquals(28, updated.recurringDay)
        assertEquals(28, LocalDate.parse(updated.nextPaymentDate).dayOfMonth)
        assertTrue(
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork("expense-payment-$id")
                .get()
                .any { it.state == WorkInfo.State.ENQUEUED },
        )

        ActivityScenario.launch<ExpenseAddActivity>(editIntent).use {
            SystemClock.sleep(500)
            onView(withId(R.id.deleteButton)).perform(click())
            onView(withId(android.R.id.button1)).perform(click())
            SystemClock.sleep(600)
        }

        assertNull(dao.getById(id))
        assertTrue(
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork("expense-payment-$id")
                .get()
                .all { it.state == WorkInfo.State.CANCELLED },
        )
    }
}
