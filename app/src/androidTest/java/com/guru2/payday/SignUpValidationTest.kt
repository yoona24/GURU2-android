package com.guru2.payday

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpValidationTest {
    @Test
    fun signUpButtonEnablesForValidKoreanNickname() {
        ActivityScenario.launch(SignUpActivity::class.java).use {
            onView(withId(R.id.emailInput)).perform(replaceText("user@example.com"))
            onView(withId(R.id.passwordInput)).perform(replaceText("password1!"))
            onView(withId(R.id.passwordConfirmInput)).perform(replaceText("password1!"))

            onView(withId(R.id.nicknameInput)).perform(replaceText("오윤아"))
            onView(withId(R.id.signUpButton)).check(matches(isEnabled()))

            onView(withId(R.id.nicknameInput)).perform(replaceText("오"))
            onView(withId(R.id.signUpButton)).check(matches(not(isEnabled())))
        }
    }
}
