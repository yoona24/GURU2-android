package com.guru2.payday.ui.expense

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.guru2.payday.R
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.databinding.ActivityExpenseAddBinding
import com.guru2.payday.notification.ExpenseNotificationScheduler
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseAddActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseAddBinding
    private var sharePeopleCount = MIN_SHARE_PEOPLE
    private var selectedExpenseType = ExpenseType.VARIABLE
    private var selectedCategoryName = ""
    private var selectedPaymentDate: LocalDate? = null
    private var loadedExpense: ExpenseEntity? = null
    private var isFormattingAmount = false

    private val isEditMode: Boolean
        get() = intent.action == Intent.ACTION_EDIT ||
            intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

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
        setupCategoryButtons()
        setupExpenseTypeTabs()
        setupDatePicker()
        setupSharePeople()
        setupRecurringOptions()
        setupValidation()
        setupActions()
        setupScreenMode()
    }

    private fun setupSystemBars() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun setupExpenseTypeTabs() {
        binding.fixedExpenseTab.setOnClickListener { selectExpenseType(ExpenseType.FIXED) }
        binding.variableExpenseTab.setOnClickListener { selectExpenseType(ExpenseType.VARIABLE) }
        binding.savingExpenseTab.setOnClickListener { selectExpenseType(ExpenseType.SAVING) }

        val initialType = ExpenseType.fromValue(intent.getStringExtra(EXTRA_EXPENSE_TYPE))
        selectExpenseType(initialType)
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
        if (expenseType == ExpenseType.FIXED) {
            requestNotificationPermissionIfNeeded()
        }
        validateForm()
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
        updateMonthlyConversion()
    }

    private fun setupCategoryButtons() {
        categoryButtons.forEach { button ->
            button.setOnClickListener { selectCategory(button) }
        }
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
            label?.let(button::setText)
        }
        selectCategory(binding.leisureCategoryButton)
    }

    private fun selectCategory(selectedButton: TextView) {
        selectedCategoryName = selectedButton.text.toString()
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
        validateForm()
    }

    private fun setupDatePicker() {
        binding.paymentDateInput.setOnClickListener {
            val initialDate = selectedPaymentDate ?: LocalDate.now()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedPaymentDate = LocalDate.of(year, month + 1, day)
                    renderPaymentDate()
                    validateForm()
                },
                initialDate.year,
                initialDate.monthValue - 1,
                initialDate.dayOfMonth,
            ).show()
        }
    }

    private fun renderPaymentDate() {
        val date = selectedPaymentDate ?: return
        binding.paymentDateInput.text = date.format(
            if (isEditMode) {
                DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
            } else {
                DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US)
            },
        )
    }

    private fun setupSharePeople() {
        binding.shareSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.sharePeopleSection.visibility =
                if (isChecked && selectedExpenseType == ExpenseType.FIXED) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            updateShareCost()
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
        updateShareCost()
    }

    private fun updateShareCost() {
        val totalAmount = amountValue()
        val personalCost = totalAmount / sharePeopleCount
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

        binding.sharePeopleCountText.text = sharePeopleCount.toString()
        binding.shareCostText.text = getString(
            R.string.share_cost,
            formatter.format(totalAmount),
            formatter.format(personalCost),
            sharePeopleCount,
        )
        binding.decreaseShareButton.isEnabled = sharePeopleCount > MIN_SHARE_PEOPLE
        binding.decreaseShareButton.alpha =
            if (binding.decreaseShareButton.isEnabled) 1f else DISABLED_ALPHA
        binding.increaseShareButton.isEnabled = sharePeopleCount < MAX_SHARE_PEOPLE
        binding.increaseShareButton.alpha =
            if (binding.increaseShareButton.isEnabled) 1f else DISABLED_ALPHA
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
                onSelected = ::updateMonthlyConversion,
            )
        }
    }

    private fun showOptions(
        anchor: TextView,
        options: Array<String>,
        onSelected: () -> Unit = {},
    ) {
        PopupMenu(this, anchor).apply {
            options.forEachIndexed { index, option ->
                menu.add(0, index, index, option)
            }
            setOnMenuItemClickListener { item ->
                anchor.text = item.title
                onSelected()
                true
            }
            show()
        }
    }

    private fun setupValidation() {
        binding.expenseNameInput.addTextChangedListener(SimpleTextWatcher(::validateForm))
        binding.paymentMethodInput.addTextChangedListener(SimpleTextWatcher(::validateForm))
        binding.expenseAmountInput.addTextChangedListener(
            SimpleTextWatcher {
                formatAmountInput()
                updateShareCost()
                updateMonthlyConversion()
                validateForm()
            },
        )
        validateForm()
    }

    private fun formatAmountInput() {
        if (isFormattingAmount) return
        val raw = binding.expenseAmountInput.text.toString().replace(",", "")
        if (raw.isBlank()) return
        val amount = raw.toLongOrNull() ?: return
        val formatted = NumberFormat.getNumberInstance(Locale.KOREA).format(amount)
        if (formatted != binding.expenseAmountInput.text.toString()) {
            isFormattingAmount = true
            binding.expenseAmountInput.setText(formatted)
            binding.expenseAmountInput.setSelection(formatted.length)
            isFormattingAmount = false
        }
    }

    private fun updateMonthlyConversion() {
        val isYearly = selectedExpenseType == ExpenseType.FIXED &&
            binding.recurringCycleInput.text.toString() == YEARLY_LABEL
        binding.monthlyConversionText.visibility = if (isYearly) View.VISIBLE else View.GONE
        if (isYearly) {
            val monthlyAmount = amountValue() / 12
            binding.monthlyConversionText.text = getString(
                R.string.monthly_conversion,
                NumberFormat.getNumberInstance(Locale.KOREA).format(monthlyAmount),
            )
        }
    }

    private fun validateForm() {
        if (!::binding.isInitialized) return
        val isValid = binding.expenseNameInput.text.toString().trim().isNotEmpty() &&
            amountValue() > 0 &&
            selectedCategoryName.isNotEmpty() &&
            binding.paymentMethodInput.text.toString().trim().isNotEmpty() &&
            selectedPaymentDate != null
        binding.saveButton.isEnabled = isValid
        binding.saveButton.alpha = if (isValid) 1f else DISABLED_ALPHA
    }

    private fun setupActions() {
        binding.closeButton.setOnClickListener { finish() }
        binding.saveButton.setOnClickListener { saveExpense() }
        binding.deleteButton.setOnClickListener { showDeleteConfirmation() }
    }

    private fun setupScreenMode() {
        if (!isEditMode) return

        binding.screenTitle.setText(R.string.expense_edit_title)
        binding.saveButton.setText(R.string.edit)
        binding.deleteButton.visibility = View.VISIBLE

        val expenseId = intent.getLongExtra(EXTRA_EXPENSE_ID, 0L)
        if (expenseId > 0) {
            loadExpense(expenseId)
        } else {
            populateDesignSample()
        }
    }

    private fun loadExpense(expenseId: Long) {
        lifecycleScope.launch {
            val expense = withContext(Dispatchers.IO) {
                PaydayDatabase.getInstance(this@ExpenseAddActivity)
                    .expenseDao()
                    .getById(expenseId)
            } ?: run {
                finish()
                return@launch
            }
            loadedExpense = expense
            populateExpense(expense)
        }
    }

    private fun populateDesignSample() {
        populateExpense(
            ExpenseEntity(
                type = ExpenseEntity.TYPE_FIXED,
                name = DEFAULT_EDIT_NAME,
                amount = DEFAULT_EDIT_AMOUNT,
                category = getString(R.string.category_leisure),
                paymentMethod = DEFAULT_EDIT_PAYMENT_METHOD,
                paymentDate = DEFAULT_EDIT_PAYMENT_DATE,
                recurringDay = 1,
                recurrence = ExpenseEntity.RECURRENCE_MONTHLY,
            ),
        )
    }

    private fun populateExpense(expense: ExpenseEntity) {
        selectExpenseType(ExpenseType.fromValue(expense.type))
        binding.expenseNameInput.setText(expense.name)
        binding.expenseAmountInput.setText(expense.amount.toString())
        binding.paymentMethodInput.setText(expense.paymentMethod)
        selectedPaymentDate = LocalDate.parse(expense.paymentDate)
        renderPaymentDate()
        selectCategoryByName(expense.category)
        sharePeopleCount = expense.shareCount.coerceIn(MIN_SHARE_PEOPLE, MAX_SHARE_PEOPLE)
        binding.shareSwitch.isChecked = expense.isShared
        expense.recurringDay?.let { binding.recurringDayInput.text = getString(R.string.day_value, it) }
        binding.recurringCycleInput.text =
            if (expense.recurrence == ExpenseEntity.RECURRENCE_YEARLY) YEARLY_LABEL else MONTHLY_LABEL
        updateShareCost()
        updateMonthlyConversion()
        validateForm()
    }

    private fun selectCategoryByName(categoryName: String) {
        categoryButtons
            .firstOrNull { it.visibility == View.VISIBLE && it.text.toString() == categoryName }
            ?.let(::selectCategory)
    }

    private fun saveExpense() {
        if (!binding.saveButton.isEnabled) {
            Toast.makeText(this, R.string.expense_all_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (amountValue() <= 0L) {
            Toast.makeText(this, R.string.expense_amount_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        val expense = buildExpense()
        lifecycleScope.launch {
            val savedExpense = withContext(Dispatchers.IO) {
                val dao = PaydayDatabase.getInstance(this@ExpenseAddActivity).expenseDao()
                if (expense.id == 0L) {
                    expense.copy(id = dao.insert(expense))
                } else {
                    dao.update(expense)
                    expense
                }
            }
            loadedExpense?.let {
                if (it.type == ExpenseEntity.TYPE_FIXED &&
                    savedExpense.type != ExpenseEntity.TYPE_FIXED
                ) {
                    ExpenseNotificationScheduler.cancel(this@ExpenseAddActivity, it.id)
                }
            }
            if (savedExpense.type == ExpenseEntity.TYPE_FIXED) {
                ExpenseNotificationScheduler.schedule(this@ExpenseAddActivity, savedExpense)
            }
            Toast.makeText(
                this@ExpenseAddActivity,
                if (isEditMode) R.string.expense_updated else R.string.expense_saved,
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    private fun buildExpense(): ExpenseEntity {
        val isFixed = selectedExpenseType == ExpenseType.FIXED
        val recurringDay = if (isFixed) {
            binding.recurringDayInput.text.toString().filter(Char::isDigit).toInt()
        } else {
            null
        }
        val recurrence = if (!isFixed) {
            null
        } else if (binding.recurringCycleInput.text.toString() == YEARLY_LABEL) {
            ExpenseEntity.RECURRENCE_YEARLY
        } else {
            ExpenseEntity.RECURRENCE_MONTHLY
        }
        return ExpenseEntity(
            id = loadedExpense?.id ?: 0L,
            type = selectedExpenseType.name,
            name = binding.expenseNameInput.text.toString().trim(),
            amount = amountValue(),
            category = selectedCategoryName,
            paymentMethod = binding.paymentMethodInput.text.toString().trim(),
            paymentDate = requireNotNull(selectedPaymentDate).toString(),
            isShared = isFixed && binding.shareSwitch.isChecked,
            shareCount = if (isFixed && binding.shareSwitch.isChecked) sharePeopleCount else 1,
            recurringDay = recurringDay,
            recurrence = recurrence,
            nextPaymentDate = recurringDay?.let { calculateNextPaymentDate(it, recurrence).toString() },
        )
    }

    private fun calculateNextPaymentDate(day: Int, recurrence: String?): LocalDate {
        val today = LocalDate.now()
        val validDay = day.coerceAtMost(today.lengthOfMonth())
        var candidate = today.withDayOfMonth(validDay)
        if (!candidate.isAfter(today)) {
            candidate = if (recurrence == ExpenseEntity.RECURRENCE_YEARLY) {
                candidate.plusYears(1)
            } else {
                candidate.plusMonths(1).let {
                    it.withDayOfMonth(day.coerceAtMost(it.lengthOfMonth()))
                }
            }
        }
        return candidate
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_expense_title)
            .setMessage(R.string.delete_expense_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> deleteExpense() }
            .show()
    }

    private fun deleteExpense() {
        val expense = loadedExpense
        if (expense == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                PaydayDatabase.getInstance(this@ExpenseAddActivity).expenseDao().delete(expense)
            }
            ExpenseNotificationScheduler.cancel(this@ExpenseAddActivity, expense.id)
            Toast.makeText(
                this@ExpenseAddActivity,
                R.string.expense_deleted,
                Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun amountValue(): Long = binding.expenseAmountInput.text
        .toString()
        .replace(",", "")
        .toLongOrNull()
        ?: 0L

    companion object {
        const val EXTRA_EDIT_MODE = "expense_edit_mode"
        const val EXTRA_EXPENSE_ID = "expense_id"
        const val EXTRA_EXPENSE_TYPE = "expense_type"
        const val EXTRA_EXPENSE_NAME = "expense_name"
        const val EXTRA_EXPENSE_AMOUNT = "expense_amount"
        const val EXTRA_CATEGORY = "expense_category"
        const val EXTRA_PAYMENT_METHOD = "expense_payment_method"
        const val EXTRA_PAYMENT_DATE = "expense_payment_date"

        private const val DEFAULT_EDIT_NAME = "넷플릭스"
        private const val DEFAULT_EDIT_AMOUNT = 15_000L
        private const val DEFAULT_EDIT_PAYMENT_METHOD = "현대카드"
        private const val DEFAULT_EDIT_PAYMENT_DATE = "2026-12-22"
        private const val MONTHLY_LABEL = "월간"
        private const val YEARLY_LABEL = "연간"
        private const val MIN_SHARE_PEOPLE = 2
        private const val MAX_SHARE_PEOPLE = 10
        private const val DISABLED_ALPHA = 0.4f
    }

    private enum class ExpenseType {
        FIXED,
        VARIABLE,
        SAVING,
        ;

        companion object {
            fun fromValue(value: String?): ExpenseType =
                entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: VARIABLE
        }
    }

    private class SimpleTextWatcher(
        private val afterChanged: () -> Unit,
    ) : TextWatcher {
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
        ) = Unit

        override fun afterTextChanged(text: Editable?) = afterChanged()
    }
}
