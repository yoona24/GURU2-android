package com.guru2.payday.ui.expense

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.guru2.payday.R
import com.guru2.payday.databinding.ActivityExpenseAddBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseAddBinding

    private val expenseTypeTabs: List<TextView>
        get() = listOf(
            binding.fixedExpenseTab,
            binding.variableExpenseTab,
            binding.savingExpenseTab,
        )

    private val categoryButtons: List<TextView>
        get() = listOf(
            binding.leisureCategoryButton,
            binding.housingCategoryButton,
            binding.foodCategoryButton,
            binding.transportCategoryButton,
            binding.medicalCategoryButton,
            binding.shoppingCategoryButton,
            binding.otherCategoryButton,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemBars()
        setupExpenseTypeTabs()
        setupCategoryButtons()
        setupDatePicker()
        setupRecurringOptions()
        setupActions()
    }

    private fun setupSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun setupExpenseTypeTabs() {
        expenseTypeTabs.forEach { tab ->
            tab.setOnClickListener {
                selectExpenseType(tab)
            }
        }
        selectExpenseType(binding.fixedExpenseTab)
    }

    private fun selectExpenseType(selectedTab: TextView) {
        expenseTypeTabs.forEach { tab ->
            val selected = tab == selectedTab
            tab.setBackgroundResource(
                if (selected) R.drawable.bg_expense_tab_selected else android.R.color.transparent,
            )
            tab.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (selected) R.color.payday_text_primary else R.color.payday_text_secondary,
                ),
            )
        }
        binding.recurringSection.visibility =
            if (selectedTab == binding.variableExpenseTab) View.GONE else View.VISIBLE
    }

    private fun setupCategoryButtons() {
        categoryButtons.forEach { button ->
            button.setOnClickListener {
                selectCategory(button)
            }
        }
        selectCategory(binding.leisureCategoryButton)
    }

    private fun selectCategory(selectedButton: TextView) {
        categoryButtons.forEach { button ->
            val selected = button == selectedButton
            button.setBackgroundResource(
                if (selected) R.drawable.bg_expense_chip_selected
                else R.drawable.bg_expense_chip_unselected,
            )
            button.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (selected) R.color.white else R.color.payday_text_secondary,
                ),
            )
        }
    }

    private fun setupDatePicker() {
        binding.paymentDateInput.setOnClickListener {
            val today = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    binding.paymentDateInput.text = SimpleDateFormat(
                        "MM/dd/yyyy",
                        Locale.US,
                    ).format(selectedDate.time)
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH),
            ).show()
        }
    }

    private fun setupRecurringOptions() {
        binding.recurringDayInput.text = resources.getStringArray(R.array.recurring_days).first()
        binding.recurringCycleInput.text =
            resources.getStringArray(R.array.recurring_cycles).first()

        binding.recurringDayInput.setOnClickListener {
            showOptions(
                anchor = binding.recurringDayInput,
                options = resources.getStringArray(R.array.recurring_days),
            )
        }
        binding.recurringCycleInput.setOnClickListener {
            showOptions(
                anchor = binding.recurringCycleInput,
                options = resources.getStringArray(R.array.recurring_cycles),
            )
        }
    }

    private fun showOptions(anchor: TextView, options: Array<String>) {
        PopupMenu(this, anchor).apply {
            options.forEachIndexed { index, option ->
                menu.add(0, index, index, option)
            }
            setOnMenuItemClickListener { item ->
                anchor.text = item.title
                true
            }
            show()
        }
    }

    private fun setupActions() {
        binding.closeButton.setOnClickListener {
            finish()
        }
        binding.saveButton.setOnClickListener {
            val name = binding.expenseNameInput.text.toString().trim()
            val amount = binding.expenseAmountInput.text.toString().trim()
            if (name.isEmpty() || amount.isEmpty()) {
                Toast.makeText(this, R.string.expense_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, R.string.expense_saved, Toast.LENGTH_SHORT).show()
        }
    }
}
