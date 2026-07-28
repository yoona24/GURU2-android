package com.guru2.payday

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
     * 이번 달 지출 분포 도넛(Pie) 차트 설정 함수
     * - 총 수입 대비 총 지출의 비율(퍼센트)을 차트 중앙에 표시합니다.
     */
    fun setupDonutChart(pieChart: PieChart, totalIncome: Float, totalExpense: Float, fixed: Float, consumption: Float, saving: Float) {

        // 수입이 등록되지 않은 경우 예외 처리
        if (totalIncome <= 0f) {
            pieChart.clear()
            pieChart.centerText = "이번 달 수입을\n먼저 등록해주세요."
            pieChart.invalidate()
            return
        }

        // 지출 내역이 없는 경우
        if (totalExpense <= 0f) {
            pieChart.clear()
            pieChart.centerText = "0%\n지출 내역이 없습니다."
            pieChart.invalidate()
            return
        }

        // 전체 수입 대비 총 지출 퍼센트 계산
        val expensePercent = ((totalExpense / totalIncome) * 100).toInt()

        // 차트에 들어갈 엔트리 데이터 생성 (고정, 변동, 저축)
        val entries = listOf(
            PieEntry(fixed, "고정 지출"),
            PieEntry(consumption, "변동 지출"),
            PieEntry(saving, "저축/투자")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 3f
            valueTextSize = 12f
        }

        // 도넛 차트 디자인 및 속성 적용
        pieChart.apply {
            data = PieData(dataSet)
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 65f
            transparentCircleRadius = 70f

            // 피그마 디자인처럼 중앙에 퍼센테이지 표시
            centerText = "$expensePercent%"
            setCenterTextSize(24f)
            setCenterTextColor(Color.parseColor("#333333"))

            description.isEnabled = false
            legend.isEnabled = false
            invalidate()
        }
    }

    /**
     * 카테고리별 지출 상위 5개 막대(Bar) 그래프 설정 함수
     */
    fun setupBarChart(barChart: BarChart, categoryMap: Map<String, Float>) {
        val sorted = categoryMap.entries.sortedByDescending { it.value }
        val entries = ArrayList<BarEntry>()
        var otherSum = 0f

        if (sorted.size <= 5) {
            sorted.forEachIndexed { index, entry ->
                entries.add(BarEntry(index.toFloat(), entry.value))
            }
        } else {
            for (i in 0 until 4) {
                entries.add(BarEntry(i.toFloat(), sorted[i].value))
            }
            for (i in 4 until sorted.size) {
                otherSum += sorted[i].value
            }
            entries.add(BarEntry(4f, otherSum))
        }

        val dataSet = BarDataSet(entries, "카테고리별 지출").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
        }

        barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            invalidate()
        }
    }
}
