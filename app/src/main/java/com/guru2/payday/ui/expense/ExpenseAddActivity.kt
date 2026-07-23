package com.guru2.payday.ui.expense

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.guru2.payday.R
import com.guru2.payday.databinding.ActivityExpenseAddBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseAddBinding
    private var sharePeopleCount = MIN_SHARE_PEOPLE
    private var selectedExpenseType = ExpenseType.FIXED
    private val isEditMode: Boolean
        get() = intent.action == Intent.ACTION_EDIT ||
            intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

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
        setupSharePeople()
        setupRecurringOptions()
        setupActions()
        setupScreenMode()
    }

    private fun setupSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun setupExpenseTypeTabs() {
        binding.fixedExpenseTab.setOnClickListener {
            selectExpenseType(ExpenseType.FIXED)
        }
        binding.variableExpenseTab.setOnClickListener {
            selectExpenseType(ExpenseType.VARIABLE)
        }
        binding.savingExpenseTab.setOnClickListener {
            selectExpenseType(ExpenseType.SAVING)
        }
        selectExpenseType(ExpenseType.FIXED)
    }

    private fun selectExpenseType(expenseType: ExpenseType) {
        selectedExpenseType = expenseType
        val selectedTab = when (expenseType) {
            ExpenseType.FIXED -> binding.fixedExpenseTab
            ExpenseType.VARIABLE -> binding.variableExpenseTab
            ExpenseType.SAVING -> binding.savingExpenseTab
        }
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
        updateFormForExpenseType(expenseType)
    }

    private fun updateFormForExpenseType(expenseType: ExpenseType) {
        val isFixedExpense = expenseType == ExpenseType.FIXED
        val isSavingExpense = expenseType == ExpenseType.SAVING
        val fixedSectionVisibility = if (isFixedExpense) View.VISIBLE else View.GONE

        binding.shareSection.visibility = fixedSectionVisibility
        binding.sharePeopleSection.visibility =
            if (isFixedExpense && binding.shareSwitch.isChecked) View.VISIBLE else View.GONE
        binding.recurringDivider.visibility = fixedSectionVisibility
        binding.recurringSection.visibility = fixedSectionVisibility

        binding.paymentMethodLabel.setText(
            if (isSavingExpense) R.string.expense_destination else R.string.payment_method,
        )
        binding.paymentMethodInput.setHint(
            if (isSavingExpense) {
                R.string.expense_destination_hint
            } else {
                R.string.payment_method_hint
            },
        )
        updateCategories(isSavingExpense)
    }

    private fun updateCategories(isSavingExpense: Boolean) {
        val categoryLabels = if (isSavingExpense) {
            listOf(R.string.category_saving, R.string.category_investment)
        } else {
            listOf(
                R.string.category_leisure,
                R.string.category_housing,
                R.string.category_food,
                R.string.category_transport,
                R.string.category_medical,
                R.string.category_shopping,
                R.string.category_other,
            )
        }

        categoryButtons.forEachIndexed { index, button ->
            val label = categoryLabels.getOrNull(index)
            button.visibility = if (label == null) View.GONE else View.VISIBLE
            if (label != null) {
                button.setText(label)
            }
        }
        selectCategory(binding.leisureCategoryButton)
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
                        if (isEditMode) "yyyy년 M월 d일" else "MM/dd/yyyy",
                        if (isEditMode) Locale.KOREA else Locale.US,
                    ).format(selectedDate.time)
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH),
            ).show()
        }
    }

    private fun setupSharePeople() {
        binding.shareSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.sharePeopleSection.visibility =
                if (isChecked && selectedExpenseType == ExpenseType.FIXED) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            if (isChecked) {
                updateShareCost()
            }
        }
        binding.decreaseShareButton.setOnClickListener {
            if (sharePeopleCount > MIN_SHARE_PEOPLE) {
                sharePeopleCount--
                updateShareCost()
            }
        }
        binding.increaseShareButton.setOnClickListener {
            if (sharePeopleCount < MAX_SHARE_PEOPLE) {
                sharePeopleCount++
                updateShareCost()
            }
        }
        binding.expenseAmountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) = Unit

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int,
            ) {
                if (binding.shareSwitch.isChecked) {
                    updateShareCost()
                }
            }

            override fun afterTextChanged(text: Editable?) = Unit
        })
        updateShareCost()
    }

    private fun updateShareCost() {
        val totalAmount = binding.expenseAmountInput.text
            .toString()
            .replace(",", "")
            .toLongOrNull()
            ?: 0L
        val personalCost = totalAmount / sharePeopleCount
        val formattedCost = NumberFormat.getNumberInstance(Locale.KOREA).format(personalCost)

        binding.sharePeopleCountText.text = sharePeopleCount.toString()
        binding.shareCostText.text = getString(
            R.string.share_cost,
            formattedCost,
            sharePeopleCount,
        )
        binding.decreaseShareButton.isEnabled = sharePeopleCount > MIN_SHARE_PEOPLE
        binding.decreaseShareButton.alpha =
            if (binding.decreaseShareButton.isEnabled) 1f else DISABLED_ALPHA
        binding.increaseShareButton.isEnabled = sharePeopleCount < MAX_SHARE_PEOPLE
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
            Toast.makeText(
                this,
                if (isEditMode) R.string.expense_updated else R.string.expense_saved,
                Toast.LENGTH_SHORT,
            ).show()
        }
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun setupScreenMode() {
        if (!isEditMode) {
            return
        }

        binding.screenTitle.setText(R.string.expense_edit_title)
        binding.saveButton.setText(R.string.edit)
        binding.deleteButton.visibility = View.VISIBLE

        val expenseType = ExpenseType.fromValue(
            intent.getStringExtra(EXTRA_EXPENSE_TYPE),
        )
        selectExpenseType(expenseType)
        binding.expenseNameInput.setText(
            intent.getStringExtra(EXTRA_EXPENSE_NAME) ?: DEFAULT_EDIT_NAME,
        )
        binding.expenseAmountInput.setText(
            intent.getLongExtra(EXTRA_EXPENSE_AMOUNT, DEFAULT_EDIT_AMOUNT).toString(),
        )
        binding.paymentMethodInput.setText(
            intent.getStringExtra(EXTRA_PAYMENT_METHOD) ?: DEFAULT_EDIT_PAYMENT_METHOD,
        )
        binding.paymentDateInput.text =
            intent.getStringExtra(EXTRA_PAYMENT_DATE) ?: DEFAULT_EDIT_PAYMENT_DATE
        selectCategoryByName(
            intent.getStringExtra(EXTRA_CATEGORY) ?: getString(R.string.category_leisure),
        )
    }

    private fun selectCategoryByName(categoryName: String) {
        categoryButtons
            .firstOrNull { it.visibility == View.VISIBLE && it.text.toString() == categoryName }
            ?.let(::selectCategory)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_expense_title)
            .setMessage(R.string.delete_expense_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                Toast.makeText(this, R.string.expense_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_EDIT_MODE = "expense_edit_mode"
        const val EXTRA_EXPENSE_TYPE = "expense_type"
        const val EXTRA_EXPENSE_NAME = "expense_name"
        const val EXTRA_EXPENSE_AMOUNT = "expense_amount"
        const val EXTRA_CATEGORY = "expense_category"
        const val EXTRA_PAYMENT_METHOD = "expense_payment_method"
        const val EXTRA_PAYMENT_DATE = "expense_payment_date"

        private const val DEFAULT_EDIT_NAME = "넷플릭스"
        private const val DEFAULT_EDIT_AMOUNT = 15_000L
        private const val DEFAULT_EDIT_PAYMENT_METHOD = "현대카드"
        private const val DEFAULT_EDIT_PAYMENT_DATE = "2026년 12월 22일"
        const val MIN_SHARE_PEOPLE = 2
        const val MAX_SHARE_PEOPLE = 20
        const val DISABLED_ALPHA = 0.4f
    }

    private enum class ExpenseType {
        FIXED,
        VARIABLE,
        SAVING,
        ;

        companion object {
            fun fromValue(value: String?): ExpenseType =
                entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FIXED
        }
    }
}
