package com.guru2.payday

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.databinding.ActivityDashboardBinding
import com.guru2.payday.ui.expense.ExpenseAddActivity // 👈 패키지 임포트 추가됨
import com.guru2.payday.ui.expense.ExpenseListActivity
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: UserSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = UserSession(this)
        if (!session.isLoggedIn) {
            returnToLogin()
            return
        }
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // [+] 플로팅 버튼 클릭 리스너
        binding.addExpenseButton.setOnClickListener {
            val options = arrayOf("수입 등록", "지출 등록")
            AlertDialog.Builder(this)
                .setTitle("항목 선택")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // 수입 등록 선택 시 ExpenseAddActivity로 이동
                            startActivity(Intent(this, ExpenseAddActivity::class.java).apply {
                                putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, "INCOME")
                            })
                        }
                        1 -> {
                            // 지출 등록 선택 시 ExpenseAddActivity로 이동 (지출 타입 전달)
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
            val total = expenses.sumOf { it.amount }
            val recurring = expenses.filter { it.type == ExpenseEntity.TYPE_FIXED }
                .sumOf { it.amount }
            val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
            binding.totalExpense.text = getString(R.string.won_amount, formatter.format(total))
            binding.recurringExpense.text = getString(R.string.won_amount, formatter.format(recurring))
            binding.emptyDashboard.visibility =
                if (expenses.isEmpty() && incomes.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun returnToLogin() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
