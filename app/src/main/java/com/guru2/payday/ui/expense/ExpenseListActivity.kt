package com.guru2.payday.ui.expense

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guru2.payday.DashboardActivity
import com.guru2.payday.MainActivity
import com.guru2.payday.R
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.IncomeEntity
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.databinding.ActivityExpenseListBinding
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseListBinding
    private lateinit var session: UserSession

    // 지출과 수입 아이템을 통합하여 관리하기 위한 Sealed Class 정의
    sealed class ListItem {
        abstract val date: String

        // 지출 항목 데이터를 담는 클래스
        data class ExpenseItem(val expense: ExpenseEntity) : ListItem() {
            override val date: String get() = expense.paymentDate
        }

        // 수입 항목 데이터를 담는 클래스
        data class IncomeItem(val income: IncomeEntity) : ListItem() {
            override val date: String get() = income.receivedDate
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = UserSession(this)

        // 로그인 상태가 아니라면 메인 액티비티로 이동하고 현재 화면 종료
        if (!session.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 뷰 바인딩 설정 및 레이아웃 뷰 연결
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 대시보드 탭 클릭 시 대시보드 화면으로 이동
        binding.dashboardTab.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }

        // 지출/수입 추가 버튼 클릭 시 종류 선택 다이얼로그 띄우기
        binding.addExpenseButton.setOnClickListener {
            val options = arrayOf("수입 등록", "지출 등록")
            AlertDialog.Builder(this)
                .setTitle("항목 선택")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // 수입 등록 화면으로 이동
                            startActivity(
                                Intent(this, ExpenseAddActivity::class.java).apply {
                                    putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, "INCOME")
                                }
                            )
                        }
                        1 -> {
                            // 지출 등록 화면으로 이동
                            startActivity(
                                Intent(this, ExpenseAddActivity::class.java).apply {
                                    putExtra(
                                        ExpenseAddActivity.EXTRA_EXPENSE_TYPE,
                                        ExpenseEntity.TYPE_VARIABLE
                                    )
                                }
                            )
                        }
                    }
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 포커스를 얻을 때마다 최신 데이터를 불러옴
        loadData()
    }

    // 데이터베이스에서 사용자별 지출 및 수입 내역을 비동기로 조회하는 함수
    private fun loadData() {
        lifecycleScope.launch {
            val (expenses, incomes) = withContext(Dispatchers.IO) {
                val db = PaydayDatabase.getInstance(this@ExpenseListActivity)
                val expenseList = db.expenseDao().getAllForUser(session.userId)
                val incomeList = db.incomeDao().getAllForUser(session.userId)
                Pair(expenseList, incomeList)
            }
            // 조회한 데이터를 화면에 렌더링
            renderItems(expenses, incomes)
        }
    }

    // 가져온 지출 및 수입 데이터를 결합하여 날짜순으로 정렬한 뒤 화면에 동적 생성하는 함수
    private fun renderItems(expenses: List<ExpenseEntity>, incomes: List<IncomeEntity>) {
        binding.expenseList.removeAllViews()

        val items = mutableListOf<ListItem>()
        expenses.forEach { items.add(ListItem.ExpenseItem(it)) }
        incomes.forEach { items.add(ListItem.IncomeItem(it)) }

        // 날짜를 기준으로 내림차순 정렬 (최신순)
        items.sortByDescending { it.date }

        // 리스트가 비어있는 경우 빈 상태 뷰 표시 여부 제어
        binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

        items.forEach { itemObj ->
            // 개별 아이템 뷰 레이아웃 인플레이트
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_expense, binding.expenseList, false)

            val nameTv = itemView.findViewById<TextView>(R.id.expenseItemName)
            val detailTv = itemView.findViewById<TextView>(R.id.expenseItemDetail)
            val amountTv = itemView.findViewById<TextView>(R.id.expenseItemAmount)

            // 아이템 타입에 따른 분기 처리 (지출 vs 수입)
            when (itemObj) {
                is ListItem.ExpenseItem -> {
                    val expense = itemObj.expense
                    nameTv.text = expense.name
                    detailTv.text = getString(
                        R.string.expense_list_detail,
                        expense.category,
                        formatDate(expense.paymentDate),
                        expense.paymentMethod,
                    )
                    // 지출 금액 표시 (마이너스 기호 및 빨간색 테마 적용)
                    amountTv.text = "-${numberFormat.format(expense.amount)}원"
                    amountTv.setTextColor(android.graphics.Color.parseColor("#FF4D4F"))

                    // 지출 항목 클릭 시 수정 화면으로 이동
                    itemView.setOnClickListener {
                        startActivity(
                            Intent(this, ExpenseAddActivity::class.java).apply {
                                action = Intent.ACTION_EDIT
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_ID, expense.id)
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, expense.type)
                            }
                        )
                    }
                }
                is ListItem.IncomeItem -> {
                    val income = itemObj.income
                    nameTv.text = income.name
                    detailTv.text = "${income.category}  ·  ${formatDate(income.receivedDate)}"
                    // 수입 금액 표시 (플러스 기호 및 초록색 테마 적용)
                    amountTv.text = "+${numberFormat.format(income.amount)}원"
                    amountTv.setTextColor(android.graphics.Color.parseColor("#52C41A"))

                    itemView.setOnClickListener {
                        startActivity(
                            Intent(this, ExpenseAddActivity::class.java).apply {
                                action = Intent.ACTION_EDIT
                                putExtra(ExpenseAddActivity.EXTRA_INCOME_ID, income.id)
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, "INCOME")
                            },
                        )
                    }
                }
            }

            // 동적으로 생성한 아이템 뷰를 리스트 컨테이너에 추가
            binding.expenseList.addView(itemView)
        }
    }

    // 날짜 문자열을 지정된 포맷(년. 월. 일.)으로 변환해주는 유틸 함수
    private fun formatDate(value: String): String = runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
    }.getOrDefault(value)
}
