package com.guru2.payday

import android.Manifest
import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class DashboardHelper {

    /**
     * 이번 달 지출 비율 도넛(Pie) 차트 설정 함수
     * - 정지 지출, 소비, 저축 비율을 시각화
     */
    fun setupDonutChart(pieChart: PieChart, totalIncome: Float, fixed: Float, consumption: Float, saving: Float){

        //수입이 하나도 등록되지 않은 경우 (0원 이하) 체크 분기
        if(totalIncome <= 0f){
            pieChart.clear()
            pieChart.centerText = "이번 달 수입을\n먼저 등록해주세요. "
            pieChart.invalidate()
            return
        }
        // 등록된 수입/지출 데이터가 하나도 없는 경우 예외 처리
        if(fixed == 0f && consumption == 0f && saving == 0f){
            pieChart.clear()
            pieChart.centerText = "지출 내역이 없습니다. "
            pieChart.invalidate()
            return
        }

        //차트에 들어갈 엔트리 데이터 생성
        val entries = listOf(
            PieEntry(fixed, "정지 지출"),
            PieEntry(consumption, "소비"),
            PieEntry(saving, "저축")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
        }

        // 이번 달 남은 금액 계산 로직 (총 수입 - (정기지출 + 소비 + 지출))
        val remainingAmount = totalIncome - (fixed + consumption + saving)

        //도넛 차트 디자인 및 속성 적용
        pieChart.apply {
            data = PieData(dataSet)
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)

            centerText = "이번 달 남은 금액\n%, d원".format(remainingAmount.toLong())
            description.isEnabled = false
            invalidate()
        }
    }

    /**
     * 카테고리별 지출 상위 5개 막대(Bar) 그래프 설정 함수
     * - 금액이 큰 순으로 정렬 후 상위 5개만 표시하고 나머지는 '기타'로 합산
     */

    fun setupBarChart(barChart: BarChart, categoryMap : Map<String, Float>){
        val sorted = categoryMap.entries.sortedByDescending { it.value }
        val entries = ArrayList<BarEntry>()
        var otherSum = 0f

        // 명세 조건
        if(sorted.size <= 5){
            // 5개 이하인 경우 전부 그대로 표시
            sorted.forEachIndexed { index, entry ->
                entries.add(BarEntry(index.toFloat(), entry.value))
            }
        }else {
            // 6개 이상인 경우 : 상위 4개 + 나머지를 더한 '기타' 1개 = 총 5개 항목으로 제한
            for(i in 0 until 4){
                entries.add(BarEntry(i.toFloat(), sorted[i].value))
            }
            // 5번째 이후 항목들의 금액을 모두 '기타'로 합산
            for(i in 4 until sorted.size){
                otherSum += sorted[i].value
            }
            entries.add(BarEntry(4f, otherSum)) // 5번째 자리에 '기타' 배치
        }
        val dataSet = BarDataSet(entries, "카데고리별 지출").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
        }

        // 바 차트 속성 적용 및 새로고침
        barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            invalidate()
        }
    }
}
