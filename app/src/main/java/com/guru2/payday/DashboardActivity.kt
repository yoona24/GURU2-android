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

    private var allExpenses = listOf<ExpenseEntity>()
    private var currentFilteredCategory = "전체"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = UserSession(this)
        if (!session.isLoggedIn) {
            returnToLogin()
            return
        }
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.menuButton.setOnClickListener {
            binding.logoutButton.visibility =
                if (binding.logoutButton.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.logoutButton.setOnClickListener {
            session.signOut()
            returnToLogin()
        }

        binding.listTab.setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }

        binding.addExpenseButton.setOnClickListener {
            val options = arrayOf("수입 등록", "지출 등록")
            AlertDialog.Builder(this)
                .setTitle("항목 선택")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            startActivity(Intent(this, ExpenseAddActivity::class.java).apply {
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, "INCOME")
                            })
                        }
                        1 -> {
                            startActivity(Intent(this, ExpenseAddActivity::class.java).apply {
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, ExpenseEntity.TYPE_VARIABLE)
                            })
                        }
                    }
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized || !session.isLoggedIn) return

        lifecycleScope.launch {
            val (expenses, incomes) = withContext(Dispatchers.IO) {
                val database = PaydayDatabase.getInstance(this@DashboardActivity)
                database.expenseDao().getAllForUser(session.userId) to
                    database.incomeDao().getAllForUser(session.userId)
            }

            allExpenses = expenses

            val totalExpense = expenses.sumOf { it.amount }
            val totalIncome = incomes.sumOf { it.amount }

            val fixedExpense = expenses.filter { it.type == ExpenseEntity.TYPE_FIXED }.sumOf { it.amount }
            val variableExpense = expenses.filter { it.type == ExpenseEntity.TYPE_VARIABLE }.sumOf { it.amount }
            val savingsExpense = expenses.filter { it.category == "저축/투자" || it.category == "저축" || it.category == "투자" }.sumOf { it.amount }

            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

            binding.totalExpense.text = getString(R.string.won_amount, formatter.format(totalExpense))
            binding.recurringExpense.text = getString(R.string.won_amount, formatter.format(fixedExpense))

            if (totalIncome > 0) {
                findTextViewByKeywords(binding.root, listOf("percent", "%", "수입"))?.let {
                    it.text = "${(totalExpense * 100) / totalIncome}%"
                }
            }

            findContainerView(binding.root)?.let { container ->
                // 화면을 다시 열 때 이전에 추가한 거래 뷰가 중복되지 않도록 정리한다.
                if (container.tag != "initialized") {
                    container.tag = "initialized"
                }

                // XML에 정의된 기본 뷰 이후의 동적 뷰만 제거한다.
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

                // 지출 유형별 합계를 요약해서 표시한다.
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

                // 가장 지출이 큰 카테고리를 과다 지출 항목으로 표시한다.
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

                // 카테고리별 거래 내역을 선택하는 필터를 구성한다.
                val sectionTitle = TextView(this@DashboardActivity).apply {
                    text = "카테고리별 거래 내역"
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.parseColor("#111111"))
                    setPadding(0, 8, 0, 12)
                }
                container.addView(sectionTitle)

                val categories = listOf("전체", "여가", "주거", "식비", "교통", "의료", "쇼핑", "기타")
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
                        setBackgroundColor(if (cat == currentFilteredCategory) Color.parseColor("#1890FF") else Color.parseColor("#F1F3F5"))
                        setTextColor(if (cat == currentFilteredCategory) Color.WHITE else Color.parseColor("#555555"))
                        setOnClickListener {
                            currentFilteredCategory = cat
                            onResume() // 선택한 카테고리로 거래 목록을 다시 표시한다.
                        }
                    }
                    chipContainer.addView(chip)
                }
                chipScroll.addView(chipContainer)
                container.addView(chipScroll)

                // 선택한 카테고리에 해당하는 상세 거래 내역을 표시한다.
                val filteredList = if (currentFilteredCategory == "전체") expenses else expenses.filter { it.category == currentFilteredCategory }

                filteredList.forEach { expense ->
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
                        text = expense.name
                        textSize = 14f
                        setTextColor(Color.parseColor("#222222"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    val dateTv = TextView(this@DashboardActivity).apply {
                        text = expense.category
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
                        text = "-${formatter.format(expense.amount)}원"
                        textSize = 14f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(Color.parseColor("#FF4D4F"))
                    }
                    val methodTv = TextView(this@DashboardActivity).apply {
                        text = "지출"
                        textSize = 11f
                        setTextColor(Color.parseColor("#888888"))
                    }
                    rightLayout.addView(amountTv)
                    rightLayout.addView(methodTv)

                    rowLayout.addView(leftLayout)
                    rowLayout.addView(rightLayout)
                    container.addView(rowLayout)
                }
            }

            binding.emptyDashboard.visibility =
                if (expenses.isEmpty() && incomes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

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

    private fun returnToLogin() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

}
