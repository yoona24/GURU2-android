package com.guru2.payday.ui.expense

import android.Manifest
import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.guru2.payday.R
import com.guru2.payday.data.local.ExpenseEntity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseFormUiTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun grantNotificationPermission() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun variableIsDefaultAndOnlyFixedShowsRecurringFields() {
        val scenario = ActivityScenario.launch<ExpenseAddActivity>(
            Intent(context, ExpenseAddActivity::class.java),
        )
        scenario.use {
            onView(withId(R.id.variableExpenseTab)).check(matches(isDisplayed()))
            onView(withId(R.id.shareSection)).check(matches(withEffectiveVisibility(GONE)))
            onView(withId(R.id.recurringSection)).check(matches(withEffectiveVisibility(GONE)))

            onView(withId(R.id.fixedExpenseTab)).perform(click())
            onView(withId(R.id.shareSection)).check(matches(isDisplayed()))
            onView(withId(R.id.recurringSection)).perform(scrollTo()).check(matches(isDisplayed()))

            scenario.onActivity {
                it.findViewById<TextView>(R.id.savingExpenseTab).performClick()
            }
            onView(withId(R.id.shareSection)).check(matches(withEffectiveVisibility(GONE)))
            onView(withId(R.id.recurringSection)).check(matches(withEffectiveVisibility(GONE)))
            onView(withText(R.string.category_saving)).check(matches(isDisplayed()))
            onView(withText(R.string.category_investment)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun inputFormattingAndRequiredFieldValidationWork() {
        val scenario = ActivityScenario.launch<ExpenseAddActivity>(
            Intent(context, ExpenseAddActivity::class.java),
        )
        scenario.use {
            onView(withId(R.id.saveButton)).check(matches(org.hamcrest.Matchers.not(isEnabled())))
            onView(withId(R.id.expenseNameInput))
                .perform(replaceText("123456789012345678901"))
                .check(matches(withText("12345678901234567890")))
            onView(withId(R.id.expenseAmountInput))
                .perform(replaceText("17000"))
                .check(matches(withText("17,000")))
            onView(withId(R.id.paymentMethodInput)).perform(replaceText("현대카드"))
            scenario.onActivity {
                it.findViewById<TextView>(R.id.paymentDateInput).performClick()
            }
            onView(withId(android.R.id.button1)).perform(click())
            onView(withId(R.id.saveButton)).check(matches(isEnabled()))
        }
    }

    @Test
    fun yearlyConversionAndShareStepperMatchSpecification() {
        val scenario = ActivityScenario.launch<ExpenseAddActivity>(
            Intent(context, ExpenseAddActivity::class.java).apply {
                putExtra(
                    ExpenseAddActivity.EXTRA_EXPENSE_TYPE,
                    ExpenseEntity.TYPE_FIXED,
                )
            },
        )
        scenario.use {
            onView(withId(R.id.expenseAmountInput)).perform(replaceText("17000"))
            onView(withId(R.id.recurringCycleInput)).perform(scrollTo())
            scenario.onActivity {
                it.findViewById<TextView>(R.id.recurringCycleInput).performClick()
            }
            onData(org.hamcrest.Matchers.anything())
                .atPosition(1)
                .inRoot(isPlatformPopup())
                .perform(click())
            onView(withId(R.id.monthlyConversionText))
                .check(matches(withText("월 환산 약 1,416원")))

            scenario.onActivity {
                it.findViewById<android.view.View>(R.id.shareSwitch).performClick()
            }
            onView(withId(R.id.sharePeopleSection)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.shareCostText))
                .check(matches(withText("총 17,000원 · 내 부담 8,500원(2명 공유)")))
            onView(withId(R.id.increaseShareButton)).perform(click())
            onView(withId(R.id.sharePeopleCountText)).check(matches(withText("3")))
            onView(withId(R.id.shareCostText))
                .check(matches(withText("총 17,000원 · 내 부담 5,666원(3명 공유)")))

            repeat(10) {
                onView(withId(R.id.increaseShareButton)).perform(click())
            }
            onView(withId(R.id.sharePeopleCountText)).check(matches(withText("10")))
            onView(withId(R.id.increaseShareButton))
                .check(matches(org.hamcrest.Matchers.not(isEnabled())))
        }
    }
}
