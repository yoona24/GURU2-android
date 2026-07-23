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

    @Query("SELECT * FROM expenses ORDER BY paymentDate DESC, updatedAt DESC")
    suspend fun getAll(): List<ExpenseEntity>

    @Query(
        "SELECT * FROM expenses WHERE type = 'FIXED' " +
            "ORDER BY nextPaymentDate ASC, updatedAt DESC",
    )
    suspend fun getFixedExpenses(): List<ExpenseEntity>
}
