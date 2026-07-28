package com.guru2.payday.ui.expense

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guru2.payday.R
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.IncomeEntity
import com.guru2.payday.data.local.PaydayDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomeEditFlowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database by lazy { PaydayDatabase.getInstance(context) }
    private val userId = 9_991L

    @Test
    fun selectingIncomeOpensEditScreenAndSupportsUpdateAndDelete() = runBlocking {
        UserSession(context).signIn(userId)
        val incomeId = database.incomeDao().insert(
            IncomeEntity(
                userId = userId,
                name = "수입수정테스트",
                amount = 100_000,
                category = "월급",
                receivedDate = "2026-07-29",
            ),
        )

        ActivityScenario.launch(ExpenseListActivity::class.java).use {
            onView(withText("수입수정테스트")).perform(click())
            onView(withText("수입 수정")).check(matches(isDisplayed()))
            onView(withId(R.id.expenseAmountInput)).perform(replaceText("120000"))
            onView(withId(R.id.saveButton)).perform(scrollTo(), click())
            SystemClock.sleep(500)
        }

        assertEquals(120_000L, database.incomeDao().getById(incomeId, userId)?.amount)

        ActivityScenario.launch(ExpenseListActivity::class.java).use {
            onView(withText("수입수정테스트")).perform(click())
            onView(withId(R.id.deleteButton)).perform(click())
            onView(withId(android.R.id.button1)).perform(click())
            SystemClock.sleep(500)
        }

        assertNull(database.incomeDao().getById(incomeId, userId))
        UserSession(context).signOut()
    }
}
