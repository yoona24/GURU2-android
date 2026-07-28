package com.guru2.payday.ui.expense

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.guru2.payday.DashboardActivity
import com.guru2.payday.MainActivity
import com.guru2.payday.R
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.ExpenseEntity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = UserSession(this)
        if (!session.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.dashboardTab.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
        binding.addExpenseButton.setOnClickListener {
            startActivity(
                Intent(this, ExpenseAddActivity::class.java).apply {
                    putExtra(
                        ExpenseAddActivity.EXTRA_EXPENSE_TYPE,
                        ExpenseEntity.TYPE_VARIABLE,
                    )
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val expenses = withContext(Dispatchers.IO) {
                PaydayDatabase.getInstance(this@ExpenseListActivity)
                    .expenseDao()
                    .getAllForUser(session.userId)
            }
            renderExpenses(expenses)
        }
    }

    private fun renderExpenses(expenses: List<ExpenseEntity>) {
        binding.expenseList.removeAllViews()
        binding.emptyState.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
        expenses.forEach { expense ->
            val item = LayoutInflater.from(this)
                .inflate(R.layout.item_expense, binding.expenseList, false)

            item.findViewById<TextView>(R.id.expenseItemName).text = expense.name

            item.findViewById<TextView>(R.id.expenseItemDetail).text = getString(
                R.string.expense_list_detail,
                expense.category,
                formatDate(expense.paymentDate), // 수정된 날짜 필드
                expense.paymentMethod,
            )

            item.findViewById<TextView>(R.id.expenseItemAmount).text = getString(
                R.string.expense_list_amount,
                NumberFormat.getNumberInstance(Locale.KOREA).format(expense.amount), // 수정된 금액 필드 반영
            )

            item.setOnClickListener {
                startActivity(
                    Intent(this, ExpenseAddActivity::class.java).apply {
                        action = Intent.ACTION_EDIT
                        putExtra(ExpenseAddActivity.EXTRA_EXPENSE_ID, expense.id)
                        putExtra(ExpenseAddActivity.EXTRA_EXPENSE_TYPE, expense.type)
                    },
                )
            }
            binding.expenseList.addView(item)
        }
    }

    private fun formatDate(value: String): String = runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
    }.getOrDefault(value)
}
