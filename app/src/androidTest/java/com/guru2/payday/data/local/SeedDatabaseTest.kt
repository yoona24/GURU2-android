package com.guru2.payday.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guru2.payday.auth.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "payday-seed-verification.db"
    private var database: PaydayDatabase? = null

    @After
    fun cleanUp() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun seedDatabaseContainsWorkingDemoAccountAndSampleTransactions() = runBlocking {
        context.deleteDatabase(databaseName)
        database = Room.databaseBuilder(context, PaydayDatabase::class.java, databaseName)
            .createFromAsset("database/payday-demo.db")
            .build()

        val user = requireNotNull(database?.userDao()?.getByEmail("demo@payday.com"))
        assertTrue(PasswordHasher.verify("payday1234", user.passwordSalt, user.passwordHash))
        assertEquals(1, database?.incomeDao()?.getAllForUser(user.id)?.size)
        assertEquals(3, database?.expenseDao()?.getAllForUser(user.id)?.size)
    }
}
