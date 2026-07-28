package com.guru2.payday.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guru2.payday.auth.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserIncomeDaoTest {
    private lateinit var database: PaydayDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PaydayDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun accountPasswordAndTransactionsAreStoredPerUser() = runBlocking {
        val salt = PasswordHasher.createSalt()
        val passwordHash = PasswordHasher.hash("password123", salt)
        val firstUserId = database.userDao().insert(
            UserEntity(
                email = "first@example.com",
                passwordHash = passwordHash,
                passwordSalt = salt,
                nickname = "첫번째",
            ),
        )
        val secondUserId = database.userDao().insert(
            UserEntity(
                email = "second@example.com",
                passwordHash = passwordHash,
                passwordSalt = salt,
                nickname = "두번째",
            ),
        )

        assertNotEquals("password123", passwordHash)
        assertTrue(PasswordHasher.verify("password123", salt, passwordHash))

        database.incomeDao().insert(
            IncomeEntity(
                userId = firstUserId,
                name = "월급",
                amount = 2_000_000,
                category = "월급",
                receivedDate = "2026-07-28",
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                userId = firstUserId,
                type = ExpenseEntity.TYPE_VARIABLE,
                name = "점심",
                amount = 12_000,
                category = "식비",
                paymentMethod = "카드",
                paymentDate = "2026-07-28",
            ),
        )

        assertEquals(1, database.incomeDao().getAllForUser(firstUserId).size)
        assertEquals(0, database.incomeDao().getAllForUser(secondUserId).size)
        assertEquals(1, database.expenseDao().getAllForUser(firstUserId).size)
        assertEquals(0, database.expenseDao().getAllForUser(secondUserId).size)
    }
}
