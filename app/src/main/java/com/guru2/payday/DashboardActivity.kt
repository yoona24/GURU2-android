package com.guru2.payday

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.IncomeEntity
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.databinding.ActivityDashboardBinding
import com.guru2.payday.ui.expense.ExpenseAddActivity
import com.guru2.payday.ui.expense.ExpenseListActivity
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: UserSession

    // 전체 지출 및 수입 데이터를 저장하는 리스트 변수 선언
    private var allExpenses = listOf<ExpenseEntity>()
    private var allIncomes = listOf<IncomeEntity>()
    // 현재 선택된 카테고리 필터 초기값 ("전체")
    private var currentFilteredCategory = "전체"

    // 지출과 수입을 통합하여 리스트에 띄우기 위한 Sealed Class 정의
    sealed class DashboardItem {
        abstract val date: String
        abstract val amount: Long
        abstract val name: String
        abstract val category: String

        // 지출 항목 데이터를 담는 클래스
        data class ExpenseItem(val expense: ExpenseEntity) : DashboardItem() {
            override val date: String get() = expense.paymentDate
            override val amount: Long get() = expense.amount
            override val name: String get() = expense.name
            override val category: String get() = expense.category
        }

        // 수입 항목 데이터를 담는 클래스
        data class IncomeItem(val income: IncomeEntity) : DashboardItem() {
            override val date: String get() = income.receivedDate
            override val amount: Long get() = income.amount
            override val name: String get() = income.name
            override val category: String get() = income.category
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = UserSession(this)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    // 각종 버튼 및 UI 요소들의 클릭 리스너를 설정하는 함수
    private fun setupListeners() {
        // 메뉴 버튼 클릭 시 로그아웃 버튼의 가시성 토글 (보이기/숨기기)
        binding.menuButton.setOnClickListener {
            binding.logoutButton.visibility =
                if (binding.logoutButton.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // 로그아웃 버튼 클릭 시 세션 종료 후 로그인 화면으로 이동
        binding.logoutButton.setOnClickListener {
            session.signOut()
            returnToLogin()
        }

        // 리스트 탭 클릭 시 지출 내역 목록 화면으로 이동
        binding.listTab.setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }

        // [+] 플로팅 버튼 클릭 시 수입/지출 등록 선택 다이얼로그 표시
        binding.addExpenseButton.setOnClickListener {
            val options = arrayOf("수입 등록", "지출 등록")
            AlertDialog.Builder(this)
                .setTitle("항목 선택")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // 수입 등록 화면으로 이동
                            startActivity(Intent(this, ExpenseAddActivity::class.java).apply {
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, "INCOME")
                            })
                        }
                        1 -> {
                            // 지출 등록 화면으로 이동
                            startActivity(Intent(this, ExpenseAddActivity::class.java).apply {
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, ExpenseEntity.TYPE_VARIABLE)
                            })
                        }
                    }
                }
                .show()
        }
    }

    // 화면이 활성화될 때마다 데이터베이스에서 최신 내역을 비동기로 불러와 대시보드를 갱신하는 함수
    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized || !session.isLoggedIn) return

        lifecycleScope.launch {
            // 백그라운드 스레드(IO)에서 지출 및 수입 데이터를 동시에 조회
            val (expenses, incomes) = withContext(Dispatchers.IO) {
                val database = PaydayDatabase.getInstance(this@DashboardActivity)
                database.expenseDao().getAllForUser(session.userId) to
                    database.incomeDao().getAllForUser(session.userId)
            }

            allExpenses = expenses
            allIncomes = incomes

            // 전체 지출 및 수입 합계 계산
            val totalExpense = expenses.sumOf { it.amount }
            val totalIncome = incomes.sumOf { it.amount }

            // 고정, 변동, 저축/투자 지출 금액 각각 계산
            val fixedExpense = expenses.filter { it.type == ExpenseEntity.TYPE_FIXED }.sumOf { it.amount }
            val variableExpense = expenses.filter { it.type == ExpenseEntity.TYPE_VARIABLE }.sumOf { it.amount }
            val savingsExpense = expenses.filter { it.category == "저축/투자" || it.category == "저축" || it.category == "투자" }.sumOf { it.amount }

            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
            binding.totalExpense.text = getString(R.string.won_amount, formatter.format(totalExpense))
            binding.recurringExpense.text = getString(R.string.won_amount, formatter.format(fixedExpense))

            // 수입이 존재할 경우 수입 대비 지출 비율 계산 및 표시
            if (totalIncome > 0) {
                findTextViewByKeywords(binding.root, listOf("percent", "%", "수입"))?.let {
                    it.text = "${(totalExpense * 100) / totalIncome}%"
                }
            }

            // 대시보드 동적 컨테이너 뷰 찾기 및 스크롤뷰 구성 처리
            findContainerView(binding.root)?.let { container ->
                val baseChildrenCount = 3
                if (container.childCount > baseChildrenCount) {
                    container.removeViews(baseChildrenCount, container.childCount - baseChildrenCount)
                }

                if (container.parent !is ScrollView) {
                    val parentGroup = container.parent as? ViewGroup
                    if (parentGroup != null) {
                        val index = parentGroup.indexOfChild(container)
                        parentGroup.removeView(container)

                        val scrollView = ScrollView(this@DashboardActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                0,
                                1f
                            )
                            isFillViewport = true
                            addView(container)
                        }
                        parentGroup.addView(scrollView, index)
                    }
                }

                // 1. 고정 지출 / 변동 지출 / 저축/투자 요약 바 생성 및 추가
                val breakdownLayout = LinearLayout(this@DashboardActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16, 16, 16, 16)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 16, 0, 16)
                    }
                    setBackgroundColor(Color.parseColor("#F8F9FA"))
                }
                val breakdownText = TextView(this@DashboardActivity).apply {
                    text = "고정 지출: ${formatter.format(fixedExpense)}원  |  변동 지출: ${formatter.format(variableExpense)}원  |  저축/투자: ${formatter.format(savingsExpense)}원"
                    textSize = 12f
                    setTextColor(Color.parseColor("#555555"))
                }
                breakdownLayout.addView(breakdownText)
                container.addView(breakdownLayout)

                // 2. 가장 많이 지출된 항목을 찾아 과다 지출 카드 영역 생성 및 추가
                if (totalExpense > 0) {
                    val categoryMap = expenses.groupBy { it.category }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }
                    val maxCategory = categoryMap.maxByOrNull { it.value }

                    if (maxCategory != null) {
                        val percent = (maxCategory.value * 100) / totalExpense
                        val cardView = CardView(this@DashboardActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 24)
                            }
                            radius = 12f
                            cardElevation = 2f
                            setCardBackgroundColor(Color.parseColor("#FFFBE6"))
                        }
                        val cardContent = LinearLayout(this@DashboardActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(24, 20, 24, 20)
                        }
                        val titleView = TextView(this@DashboardActivity).apply {
                            text = "⚠️ 과다 지출 항목"
                            textSize = 12f
                            setTextColor(Color.parseColor("#FA8C16"))
                        }
                        val descView = TextView(this@DashboardActivity).apply {
                            text = "${maxCategory.key} (${percent}%, ${formatter.format(maxCategory.value)}원)"
                            textSize = 15f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setTextColor(Color.parseColor("#333333"))
                            setPadding(0, 4, 0, 0)
                        }
                        cardContent.addView(titleView)
                        cardContent.addView(descView)
                        cardView.addView(cardContent)
                        container.addView(cardView)
                    }
                }

                // 3. 카테고리별 거래 내역 타이틀 및 가로 스크롤 필터 칩 생성
                val sectionTitle = TextView(this@DashboardActivity).apply {
                    text = "카테고리별 거래 내역"
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.parseColor("#111111"))
                    setPadding(0, 8, 0, 12)
                }
                container.addView(sectionTitle)

                val categories = listOf("전체", "여가", "주거", "식비", "교통", "의료", "쇼핑", "월급", "용돈", "기타")
                val chipScroll = HorizontalScrollView(this@DashboardActivity).apply {
                    isHorizontalScrollBarEnabled = false
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 16)
                    }
                }
                val chipContainer = LinearLayout(this@DashboardActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                // 카테고리별 칩 버튼 생성 및 클릭 리스너 설정
                categories.forEach { cat ->
                    val chip = TextView(this@DashboardActivity).apply {
                        text = cat
                        textSize = 13f
                        setPadding(32, 16, 32, 16)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 12, 0)
                        }
                        gravity = Gravity.CENTER
                        // 선택된 카테고리에 따라 배경색 및 텍스트 색상 변경
                        setBackgroundColor(if (cat == currentFilteredCategory) Color.parseColor("#1890FF") else Color.parseColor("#F1F3F5"))
                        setTextColor(if (cat == currentFilteredCategory) Color.WHITE else Color.parseColor("#555555"))
                        setOnClickListener {
                            currentFilteredCategory = cat
                            onResume() // 칩 선택 시 화면 다시 로드하여 필터링 적용
                        }
                    }
                    chipContainer.addView(chip)
                }
                chipScroll.addView(chipContainer)
                container.addView(chipScroll)

                // 4. 상세 거래 내역 리스트 렌더링 (지출 + 수입 통합 및 필터링 적용)
                val combinedList = mutableListOf<DashboardItem>()
                expenses.forEach { combinedList.add(DashboardItem.ExpenseItem(it)) }
                incomes.forEach { combinedList.add(DashboardItem.IncomeItem(it)) }

                val filteredList = if (currentFilteredCategory == "전체") {
                    combinedList
                } else {
                    combinedList.filter { it.category == currentFilteredCategory }
                }

                // 날짜 기준 내림차순 정렬 후 각각의 행 뷰 생성
                filteredList.sortedByDescending { it.date }.forEach { item ->
                    val rowLayout = LinearLayout(this@DashboardActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 16, 0, 16)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val leftLayout = LinearLayout(this@DashboardActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val nameTv = TextView(this@DashboardActivity).apply {
                        text = item.name
                        textSize = 14f
                        setTextColor(Color.parseColor("#222222"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    val dateTv = TextView(this@DashboardActivity).apply {
                        text = item.category
                        textSize = 12f
                        setTextColor(Color.parseColor("#888888"))
                    }
                    leftLayout.addView(nameTv)
                    leftLayout.addView(dateTv)

                    val rightLayout = LinearLayout(this@DashboardActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.END
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }

                    val amountTv = TextView(this@DashboardActivity).apply {
                        textSize = 14f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    val methodTv = TextView(this@DashboardActivity).apply {
                        textSize = 11f
                        setTextColor(Color.parseColor("#888888"))
                    }

                    // 지출과 수입 항목에 따라 금액 부호, 색상, 타입 텍스트 분기 처리
                    when (item) {
                        is DashboardItem.ExpenseItem -> {
                            amountTv.text = "-${formatter.format(item.amount)}원"
                            amountTv.setTextColor(Color.parseColor("#FF4D4F")) // 지출 빨간색
                            methodTv.text = "지출"
                        }
                        is DashboardItem.IncomeItem -> {
                            amountTv.text = "+${formatter.format(item.amount)}원"
                            amountTv.setTextColor(Color.parseColor("#52C41A")) // 수입 초록색
                            methodTv.text = "수입"
                        }
                    }

                    rightLayout.addView(amountTv)
                    rightLayout.addView(methodTv)

                    rowLayout.addView(leftLayout)
                    rowLayout.addView(rightLayout)
                    container.addView(rowLayout)
                }
            }

            // 데이터가 아예 없을 경우 빈 상태 뷰 표시 여부 결정
            binding.emptyDashboard.visibility =
                if (expenses.isEmpty() && incomes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // 특정 키워드가 포함된 TextView를 뷰 계층 구조에서 재귀적으로 찾는 함수
    private fun findTextViewByKeywords(rootView: View, keywords: List<String>): TextView? {
        if (rootView is TextView) {
            val text = rootView.text.toString().lowercase()
            val tag = rootView.tag?.toString()?.lowercase() ?: ""
            if (keywords.any { text.contains(it) || tag.contains(it) }) {
                return rootView
            }
        }
        if (rootView is ViewGroup) {
            for (i in 0 until rootView.childCount) {
                val found = findTextViewByKeywords(rootView.getChildAt(i), keywords)
                if (found != null) return found
            }
        }
        return null
    }

    // 동적 컨테이너로 사용할 ViewGroup을 뷰 계층 구조에서 찾는 함수
    private fun findContainerView(rootView: View): ViewGroup? {
        if (rootView is ViewGroup && rootView.childCount > 3 && rootView !is CardView && rootView !is ScrollView) {
            return rootView
        }
        if (rootView is ViewGroup) {
            for (i in 0 until rootView.childCount) {
                val found = findContainerView(rootView.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    // 로그아웃 시 로그인 액티비티로 이동하고 기존 백스택을 모두 비우는 함수
    private fun returnToLogin() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
