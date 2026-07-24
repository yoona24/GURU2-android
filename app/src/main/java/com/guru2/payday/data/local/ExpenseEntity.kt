package com.guru2.payday.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val name: String,
    val amount: Long,
    val category: String,
    val paymentMethod: String,
    val paymentDate: String,
    val isShared: Boolean = false,
    val shareCount: Int = 1,
    val recurringDay: Int? = null,
    val recurrence: String? = null,
    val nextPaymentDate: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val personalAmount: Long
        get() = if (isShared) amount / shareCount.coerceAtLeast(1) else amount

    val monthlyAmount: Long
        get() = if (recurrence == RECURRENCE_YEARLY) personalAmount / 12 else personalAmount

    companion object {
        const val TYPE_FIXED = "FIXED"
        const val TYPE_VARIABLE = "VARIABLE"
        const val TYPE_SAVING = "SAVING"
        const val RECURRENCE_MONTHLY = "MONTHLY"
        const val RECURRENCE_YEARLY = "YEARLY"
    }
}
