package com.guru2.payday.feature.transaction

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

//구독 아이템 데이터 모델
data class SubscriptionItem(
    val title: String,
    val monthlyAmount: Int,
    val paymentDate: String, // 결제일("yyyy-MM-dd" 형식)
    val isShared: Boolean,
    val sharedCount: Int = 0  // 공유 중인 인원 수 (공유 n명 표시용)
){
    //오늘 날짜와 결제일을 비교하여 D-DAY 계산 함수
    fun getDDay(): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetDate = sdf.parse(paymentDate) ?: return "D-?"
            val today: Date = Date()

            // 날짜 간의 시간 차이를 일(Day) 수로 변환
            val diffInMillis = targetDate.time - today.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            // 각 조건문에서 명시적 문자열 반환
            return when{
                diffDays == 0L -> "D-Day"
                diffDays > 0 -> "D-$diffDays"
                else -> "D+${-diffDays}"
            }
        }catch (e: Exception){
            return "D-?"
        }
    }
    //공유 여부 뱃지 텍스트 반환 함수 (예: "공유 2명")
    fun getShareBadgeText(): String{
        return if (isShared && sharedCount > 0) "공유 ${sharedCount}명" else ""
    }
}