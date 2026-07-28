package com.guru2.payday.ui.expense

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guru2.payday.R
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.PaydayDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomeSaveFlowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val userId = 9001L

    @Test
    fun incomeFormSavesIncomeForCurrentUser() = runBlocking {
        UserSession(context).signIn(userId)
        val dao = PaydayDatabase.getInstance(context).incomeDao()
        val beforeCount = dao.getAllForUser(userId).size

        ActivityScenario.launch<ExpenseAddActivity>(
            Intent(context, ExpenseAddActivity::class.java).apply {
                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, "INCOME")
            },
        ).use { scenario ->
            onView(withId(R.id.expenseNameInput)).perform(replaceText("테스트 수입"))
            onView(withId(R.id.expenseAmountInput)).perform(replaceText("50000"))
            scenario.onActivity {
                it.findViewById<TextView>(R.id.paymentDateInput).text = "2026-07-29"
            }
            onView(withId(R.id.saveButton)).perform(scrollTo(), click())
            SystemClock.sleep(500)
        }

        val saved = dao.getAllForUser(userId)
        assertEquals(beforeCount + 1, saved.size)
        assertTrue(saved.any { it.name == "테스트 수입" && it.amount == 50_000L })
    }
}
