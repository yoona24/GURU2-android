package com.guru2.payday

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guru2.payday.auth.PasswordHasher
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.data.local.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthLoginFlowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun registeredEmailCanLoginRegardlessOfLetterCase() = runBlocking {
        UserSession(context).signOut()
        val email = "login-${System.currentTimeMillis()}@example.com"
        val password = "test1234"
        val salt = PasswordHasher.createSalt()
        val userId = PaydayDatabase.getInstance(context).userDao().insert(
            UserEntity(
                email = email,
                passwordHash = PasswordHasher.hash(password, salt),
                passwordSalt = salt,
                nickname = "로그인테스트",
            ),
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.emailInput)).perform(replaceText(email.uppercase()))
            onView(withId(R.id.passwordInput)).perform(replaceText(password))
            onView(withId(R.id.loginButton)).perform(click())
            SystemClock.sleep(700)
        }

        assertEquals(userId, UserSession(context).userId)
        UserSession(context).signOut()
    }
}
