package com.guru2.payday.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자별 지출 정보와 정기 결제 및 공유 설정을 저장하는 Room 엔티티다.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,
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
    // 공유 지출이면 전체 금액을 인원수로 나눠 사용자의 실제 부담액을 반환한다.
    val personalAmount: Long
        get() = if (isShared) amount / shareCount.coerceAtLeast(1) else amount

    // 연간 정기 지출은 월 단위 금액으로 환산해 대시보드 집계에 사용한다.
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
