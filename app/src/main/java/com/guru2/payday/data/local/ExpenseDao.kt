package com.guru2.payday.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getByIdForUser(id: Long, userId: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY paymentDate DESC, updatedAt DESC")
    suspend fun getAllForUser(userId: Long): List<ExpenseEntity>

    @Query(
        "SELECT * FROM expenses WHERE userId = :userId AND type = 'FIXED' " +
            "ORDER BY nextPaymentDate ASC, updatedAt DESC",
    )
    suspend fun getFixedExpensesForUser(userId: Long): List<ExpenseEntity>

    @Query(
        "SELECT * FROM expenses WHERE type = 'FIXED' " +
            "ORDER BY nextPaymentDate ASC, updatedAt DESC",
    )
    suspend fun getFixedExpenses(): List<ExpenseEntity>
}
