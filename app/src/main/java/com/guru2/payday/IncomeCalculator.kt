package com.guru2.payday

// 1. 수입 관리 데이터 모델
data class IncomeItem(
    val id: String = "",
    val title: String, // 수입명 (최대 20자)
    val amount: Long,  // 금액
    val category: String  // 카테고리 (월급/용돈/환급/기타)
)

object IncomeCalculator {

    // 2. 수입명 유효성 검사 (최대 20자 이내, 공백 불가)
    fun validateTitle(title: String): Pair<Boolean, String>{
        if(title.isBlank()){
            return Pair(false, "수입명을 입력해주세요.")
        }
        if (title.length > 20){
            return Pair(false, "수입명은 최대 20자까지 입력 가능합니다.")
        }
        return Pair(true, "")
    }
    /**
     *  수입 금액 유효성 검사 및 천 단위 콤마 포맷팅 도메인 로직
     *  - UI와 분리된 순수 도메인 계층에서 금액 계산을 담당
     */
    fun validateAndFormat(amountStr : String): Pair<Boolean, String>{
        // 콤마 제거 후 숫자(Long)로 변환 (변환 실패 시 0 처리)
        val cleanAmount = amountStr.replace(",", "").toLongOrNull() ?: 0L

        // 금액이 0원 이하인지 검사
        if (cleanAmount <= 0){
            return Pair(false, "금액은 0원 초과여야 합니다. ")
        }

        //천 단위 콤마 포맷 적용
        val formatted = String.format("%,d", cleanAmount)
        return Pair(true, formatted)
    }
    // 3. 전체 입력값(수입명, 금액, 카테고리)이 모두 유효한지 검사
    fun isFormValid(title: String, amountStr: String, category: String): Boolean{
        val(isTitleValid, _) = validateTitle(title)
        val cleanAmount = amountStr.replace(",", "").toLongOrNull() ?: 0L
        val isAmountValid = cleanAmount > 0
        val isCategoryValid = category.isNotBlank()

        return isTitleValid && isAmountValid && isCategoryValid
    }
}
