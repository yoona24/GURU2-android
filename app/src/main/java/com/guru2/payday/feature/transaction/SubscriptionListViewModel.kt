package com.guru2.payday.feature.transaction

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SubscriptionListViewModel : ViewModel() {

    // 빈 상태 여부를 화면에 알려주는 LiveData(데이터가 없으면 true)
    private val _isEmptyState = MutableLiveData<Boolean>(true)
    val isEmptyState: LiveData<Boolean> get() = _isEmptyState

    fun sortList(items: List<SubscriptionItem>, sortType: String): List<SubscriptionItem>{
        //리스트가 비어있는지 여부 체크 후 빈 상태 LiveData 업데이트
        _isEmptyState.value = items.isEmpty()

        return when(sortType){
            "결제일순" -> items.sortedBy { it.paymentDate }
            "금액순" -> items.sortedByDescending { it.monthlyAmount }
            else -> items
        }
    }
}
