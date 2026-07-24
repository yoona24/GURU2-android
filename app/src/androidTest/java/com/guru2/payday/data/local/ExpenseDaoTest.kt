package com.guru2.payday.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {
    private lateinit var database: PaydayDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PaydayDatabase::class.java,
        ).build()
        dao = database.expenseDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun fixedExpenseCanBeInsertedUpdatedAndDeleted() = runBlocking {
        val id = dao.insert(
            ExpenseEntity(
                type = ExpenseEntity.TYPE_FIXED,
                name = "Netflix",
                amount = 17_000,
                category = "여가",
                paymentMethod = "Card",
                paymentDate = "2026-07-24",
                isShared = true,
                shareCount = 2,
                recurringDay = 25,
                recurrence = ExpenseEntity.RECURRENCE_MONTHLY,
                nextPaymentDate = "2026-07-25",
            ),
        )

        val inserted = requireNotNull(dao.getById(id))
        assertEquals(8_500L, inserted.personalAmount)
        assertEquals(1, dao.getFixedExpenses().size)

        dao.update(inserted.copy(amount = 24_000))
        assertEquals(12_000L, dao.getById(id)?.personalAmount)

        dao.delete(requireNotNull(dao.getById(id)))
        assertNull(dao.getById(id))
    }
}
