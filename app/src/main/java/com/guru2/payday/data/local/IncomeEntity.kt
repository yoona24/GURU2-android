package com.guru2.payday.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val amount: Long,
    val category: String,
    val receivedDate: String,
    val memo: String = "",
    val isRecurring: Boolean = false,
    val recurrence: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
