package com.guru2.payday.ui.expense

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.PaydayDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseListEditNavigationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database by lazy { PaydayDatabase.getInstance(context) }
    private var expenseId: Long = 0

    @Before
    fun createExpense() = runBlocking {
        UserSession(context).signIn(TEST_USER_ID)
        expenseId = database.expenseDao().insert(
            ExpenseEntity(
                userId = TEST_USER_ID,
                type = ExpenseEntity.TYPE_FIXED,
                name = "월세수정테스트",
                amount = 300_000,
                category = "기타",
                paymentMethod = "현금",
                paymentDate = "2026-07-28",
            ),
        )
    }

    @After
    fun deleteExpense() = runBlocking {
        database.expenseDao().getById(expenseId)?.let { database.expenseDao().delete(it) }
        UserSession(context).signOut()
    }

    @Test
    fun selectingExpenseOpensEditScreenWithExistingValues() {
        ActivityScenario.launch(ExpenseListActivity::class.java).use {
            onView(withText("월세수정테스트")).perform(click())
            onView(withText("지출 수정")).check(matches(isDisplayed()))
            onView(withText("월세수정테스트")).check(matches(isDisplayed()))
            onView(withText("300,000")).check(matches(isDisplayed()))
            onView(withText("수정하기")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    companion object {
        private const val TEST_USER_ID = 9_999_991L
    }
}
