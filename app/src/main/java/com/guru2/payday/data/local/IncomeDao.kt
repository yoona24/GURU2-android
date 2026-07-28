package com.guru2.payday.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: IncomeEntity): Long

    @Update
    suspend fun update(income: IncomeEntity)

    @Delete
    suspend fun delete(income: IncomeEntity)

    @Query("SELECT * FROM incomes WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: Long, userId: Long): IncomeEntity?

    @Query("SELECT * FROM incomes WHERE userId = :userId ORDER BY receivedDate DESC, updatedAt DESC")
    suspend fun getAllForUser(userId: Long): List<IncomeEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM incomes WHERE userId = :userId")
    suspend fun getTotalForUser(userId: Long): Long
}
