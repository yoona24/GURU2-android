package com.guru2.payday.ui.expense

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.PaydayDatabase
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 수입과 지출의 등록 폼을 구성하고, 지출 수정 시 기존 데이터를 불러와 갱신한다.
 */
class ExpenseAddActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXPENSE_TYPE = "EXTRA_EXPENSE_TYPE"
        const val EXTRA_EXPENSE_ID = "EXTRA_EXPENSE_ID"
    }

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var etPaymentMethod: EditText
    private lateinit var chipGroupCategory: ChipGroup
    private lateinit var chipGroupType: ChipGroup
    private lateinit var chipGroupCycle: ChipGroup

    private lateinit var layoutRecurringOptions: LinearLayout
    private lateinit var layoutSharedHeader: LinearLayout
    private lateinit var layoutSharedPeople: LinearLayout
    private lateinit var switchShared: Switch
    private lateinit var tvSharedCount: TextView
    private lateinit var tvMyShareAmount: TextView
    private lateinit var etRecurringDay: EditText

    private lateinit var btnSave: Button
    private lateinit var btnClose: TextView
    private lateinit var tvTopTitle: TextView

    private var selectedCategory = "여가"
    private var selectedExpenseType = ExpenseEntity.TYPE_VARIABLE
    private var selectedCycle = "월간"
    private var sharedPersonCount = 2
    private var calendar = Calendar.getInstance()
    private var expenseType = ExpenseEntity.TYPE_VARIABLE
    private lateinit var session: UserSession
    private var editingExpense: ExpenseEntity? = null
    private val expenseId: Long
        get() = intent.getLongExtra(EXTRA_EXPENSE_ID, 0L)
    private val isEditMode: Boolean
        get() = intent.action == android.content.Intent.ACTION_EDIT && expenseId > 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = UserSession(this)
        if (!session.isLoggedIn) {
            finish()
            return
        }

        expenseType = intent.getStringExtra(EXTRA_EXPENSE_TYPE) ?: ExpenseEntity.TYPE_VARIABLE

        setupDynamicLayout()
        setupUI()
        setupListeners()
        if (isEditMode) loadExpenseForEdit()
    }

    // 선택한 거래 유형에 맞춰 입력 항목과 선택 옵션을 동적으로 구성한다.
    private fun setupDynamicLayout() {
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val topLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        btnClose = TextView(this).apply {
            text = "✕"
            textSize = 22f
            setPadding(10, 10, 20, 10)
        }

        // 등록 유형과 수정 여부에 맞춰 화면 제목을 표시한다.
        val titleText = when {
            isEditMode -> "지출 수정"
            expenseType == "INCOME" -> "수입 등록"
            else -> "지출 추가"
        }
        tvTopTitle = TextView(this).apply {
            text = titleText
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        topLayout.addView(btnClose)
        topLayout.addView(tvTopTitle)
        rootLayout.addView(topLayout)

        // 지출 등록에서만 고정 지출, 변동 지출, 저축·투자 유형을 선택한다.
        if (expenseType != "INCOME") {
            chipGroupType = ChipGroup(this).apply {
                isSingleSelection = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 20; bottomMargin = 10 }
            }
            val types = listOf("고정 지출", "변동 지출", "저축/투자")
            for ((idx, t) in types.withIndex()) {
                val chip = Chip(this).apply {
                    text = t
                    isCheckable = true
                    if (idx == 1) isChecked = true
                }
                chipGroupType.addView(chip)
            }
            rootLayout.addView(chipGroupType)
        }

        val nameLabel = if (expenseType == "INCOME") "수입 이름" else "지출 이름"
        rootLayout.addView(createLabel(nameLabel))
        etTitle = EditText(this).apply {
            hint = if (expenseType == "INCOME") "수입 이름 입력" else "지출 이름 입력"
            layoutParams = createParam()
        }
        rootLayout.addView(etTitle)

        rootLayout.addView(createLabel("금액"))
        etAmount = EditText(this).apply {
            hint = "금액 입력"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = createParam()
        }
        rootLayout.addView(etAmount)

        rootLayout.addView(createLabel("카테고리"))
        chipGroupCategory = ChipGroup(this).apply {
            isSingleSelection = true
            layoutParams = createParam()
        }
        updateCategoryChips(if (expenseType == "INCOME") "INCOME" else ExpenseEntity.TYPE_VARIABLE)
        rootLayout.addView(chipGroupCategory)

        if (expenseType != "INCOME") {
            rootLayout.addView(createLabel("결제 수단"))
            etPaymentMethod = EditText(this).apply {
                hint = "예: 현대카드, 현금"
                layoutParams = createParam()
            }
            rootLayout.addView(etPaymentMethod)
        }

        val dateLabel = if (expenseType == "INCOME") "입금일" else "결제 날짜"
        rootLayout.addView(createLabel(dateLabel))
        etDate = EditText(this).apply {
            hint = "mm/dd/yyyy"
            isFocusable = false
            isClickable = true
            layoutParams = createParam()
        }
        rootLayout.addView(etDate)

        // 공유 인원과 정기 결제 설정은 선택한 지출 유형에 따라 노출한다.
        if (expenseType != "INCOME") {
            layoutSharedHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 30 }
            }
            val tvSharedLabel = TextView(this).apply {
                text = "공유 여부"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            switchShared = Switch(this)
            layoutSharedHeader.addView(tvSharedLabel)
            layoutSharedHeader.addView(switchShared)
            rootLayout.addView(layoutSharedHeader)

            layoutSharedPeople = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 10 }
            }

            val rowSharedSub = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = createParam()
            }
            val tvSharedInfo = TextView(this).apply {
                text = "공유 인원"
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btnMinus = Button(this).apply { text = "-" }
            tvSharedCount = TextView(this).apply {
                text = " 2 "
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(20, 0, 20, 0)
            }
            val btnPlus = Button(this).apply { text = "+" }

            btnMinus.setOnClickListener {
                if (sharedPersonCount > 1) {
                    sharedPersonCount--
                    tvSharedCount.text = " $sharedPersonCount "
                    updateMyShareAmount()
                }
            }
            btnPlus.setOnClickListener {
                sharedPersonCount++
                tvSharedCount.text = " $sharedPersonCount "
                updateMyShareAmount()
            }

            rowSharedSub.addView(tvSharedInfo)
            rowSharedSub.addView(btnMinus)
            rowSharedSub.addView(tvSharedCount)
            rowSharedSub.addView(btnPlus)
            layoutSharedPeople.addView(rowSharedSub)

            tvMyShareAmount = TextView(this).apply {
                text = "내 부담 (0원/2명 공유)"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#3F51B5"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            layoutSharedPeople.addView(tvMyShareAmount)
            rootLayout.addView(layoutSharedPeople)

            layoutRecurringOptions = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                layoutParams = createParam()
            }

            val tvRecurringTitle = TextView(this).apply {
                text = "정기 결제 설정"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 40; bottomMargin = 10 }
            }
            layoutRecurringOptions.addView(tvRecurringTitle)

            layoutRecurringOptions.addView(createLabel("결제일"))
            etRecurringDay = EditText(this).apply {
                hint = "1일"
                layoutParams = createParam()
            }
            layoutRecurringOptions.addView(etRecurringDay)

            layoutRecurringOptions.addView(createLabel("반복 주기"))
            chipGroupCycle = ChipGroup(this).apply {
                isSingleSelection = true
                layoutParams = createParam()
            }
            val cycles = listOf("월간", "주간", "연간")
            for ((idx, c) in cycles.withIndex()) {
                val chip = Chip(this).apply {
                    text = c
                    isCheckable = true
                    if (idx == 0) isChecked = true
                }
                chipGroupCycle.addView(chip)
            }
            layoutRecurringOptions.addView(chipGroupCycle)
            rootLayout.addView(layoutRecurringOptions)
        }

        btnSave = Button(this).apply {
            text = if (isEditMode) "수정하기" else "저장하기"
            isEnabled = false
            alpha = 0.5f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            ).apply { topMargin = 50; bottomMargin = 50 }
        }
        rootLayout.addView(btnSave)

        scrollView.addView(rootLayout)
        setContentView(scrollView)
    }

    // 거래 유형별로 사용할 수 있는 카테고리 목록을 다시 그린다.
    private fun updateCategoryChips(type: String) {
        chipGroupCategory.removeAllViews()
        val categories = when (type) {
            "INCOME" -> listOf("월급", "용돈", "환급", "기타")
            "SAVINGS" -> listOf("저축", "투자")
            else -> listOf("여가", "주거", "식비", "교통", "의료")
        }

        for ((index, cat) in categories.withIndex()) {
            val chip = Chip(this).apply {
                text = cat
                isCheckable = true
                if (index == 0) isChecked = true
            }
            chipGroupCategory.addView(chip)
        }
        selectedCategory = categories.firstOrNull() ?: "여가"
    }

    private fun createLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20; bottomMargin = 5 }
        }
    }

    private fun createParam(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // 금액을 천 단위 콤마 형식으로 표시하고 입력 상태를 다시 검증한다.
    private fun setupUI() {
        etAmount.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != current) {
                    etAmount.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(Regex("[^\\d]"), "")
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toLong()
                        val formatted = NumberFormat.getNumberInstance(Locale.KOREA).format(parsed)
                        current = formatted
                        etAmount.setText(formatted)
                        etAmount.setSelection(formatted.length)
                    } else {
                        current = ""
                        etAmount.setText("")
                    }
                    etAmount.addTextChangedListener(this)
                }
                if (expenseType != "INCOME") {
                    updateMyShareAmount()
                }
                validateInputs()
            }
        })

        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validateInputs() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 총 금액을 공유 인원으로 나눠 현재 사용자의 부담 금액을 계산한다.
    private fun updateMyShareAmount() {
        val amountStr = etAmount.text.toString().replace(",", "")
        val totalAmount = amountStr.toLongOrNull() ?: 0L
        val myShare = if (sharedPersonCount > 0) totalAmount / sharedPersonCount else totalAmount
        val formattedShare = NumberFormat.getNumberInstance(Locale.KOREA).format(myShare)
        tvMyShareAmount.text = "내 부담 (${formattedShare}원 / ${sharedPersonCount}명 공유)"
    }

    // 유형, 카테고리, 공유 여부, 반복 주기 선택에 따른 화면 동작을 연결한다.
    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }
        etDate.setOnClickListener { showDatePickerDialog() }

        if (expenseType != "INCOME") {
            switchShared.setOnCheckedChangeListener { _, isChecked ->
                layoutSharedPeople.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked) updateMyShareAmount()
            }

            chipGroupType.setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                val typeName = chip?.text?.toString()
                selectedExpenseType = when (typeName) {
                    "고정 지출" -> {
                        layoutRecurringOptions.visibility = View.VISIBLE
                        layoutSharedHeader.visibility = View.VISIBLE
                        ExpenseEntity.TYPE_FIXED
                    }
                    "저축/투자" -> {
                        layoutRecurringOptions.visibility = View.GONE
                        layoutSharedHeader.visibility = View.VISIBLE
                        "SAVINGS"
                    }
                    else -> {
                        layoutRecurringOptions.visibility = View.GONE
                        layoutSharedHeader.visibility = View.GONE
                        switchShared.isChecked = false
                        layoutSharedPeople.visibility = View.GONE
                        ExpenseEntity.TYPE_VARIABLE
                    }
                }
                updateCategoryChips(selectedExpenseType)
            }

            chipGroupCycle.setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                selectedCycle = chip?.text?.toString() ?: "월간"
            }
        }

        chipGroupCategory.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            selectedCategory = chip?.text?.toString() ?: "여가"
            validateInputs()
        }

        btnSave.setOnClickListener { saveData() }
    }

    private fun showDatePickerDialog() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            calendar.set(y, m, d)
            val dateString = String.format("%04d-%02d-%02d", y, m + 1, d)
            etDate.setText(dateString)
            validateInputs()
        }, year, month, day).show()
    }

    // 필수 입력값이 모두 유효할 때만 저장 버튼을 활성화한다.
    private fun validateInputs() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()

        val isValid = title.isNotEmpty() && amount > 0 && date.isNotEmpty()
        btnSave.isEnabled = isValid
        btnSave.alpha = if (isValid) 1f else 0.5f
    }

    // 현재 사용자에게 속한 지출을 조회해 수정 화면에 기존 값을 채운다.
    private fun loadExpenseForEdit() {
        lifecycleScope.launch {
            val expense = withContext(Dispatchers.IO) {
                PaydayDatabase.getInstance(this@ExpenseAddActivity)
                    .expenseDao()
                    .getByIdForUser(expenseId, session.userId)
            }
            if (expense == null) {
                finish()
                return@launch
            }
            editingExpense = expense
            etTitle.setText(expense.name)
            etAmount.setText(NumberFormat.getNumberInstance(Locale.KOREA).format(expense.amount))
            etDate.setText(expense.paymentDate)
            if (::etPaymentMethod.isInitialized) {
                etPaymentMethod.setText(expense.paymentMethod)
            }
            validateInputs()
        }
    }

    // 입력값을 현재 사용자의 거래 데이터로 저장하거나 기존 지출을 갱신한다.
    private fun saveData() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()
        val method = if (::etPaymentMethod.isInitialized) etPaymentMethod.text.toString().ifEmpty { "현금" } else "현금"

        lifecycleScope.launch(Dispatchers.IO) {
            val database = PaydayDatabase.getInstance(this@ExpenseAddActivity)

            // 수입과 지출은 서로 다른 저장 흐름으로 분기한다.
            if (expenseType == "INCOME") {
                // 수입 저장은 수입 관리 기능에서 처리한다.
            } else {
                val current = editingExpense
                if (current != null) {
                    database.expenseDao().update(
                        current.copy(
                            name = title,
                            amount = amount,
                            category = selectedCategory,
                            paymentMethod = method,
                            paymentDate = date,
                            type = selectedExpenseType,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    database.expenseDao().insert(
                        ExpenseEntity(
                            userId = session.userId,
                            name = title,
                            category = selectedCategory,
                            paymentMethod = method,
                            paymentDate = date,
                            amount = amount,
                            type = selectedExpenseType
                        )
                    )
                }
            }


            withContext(Dispatchers.Main) {
                Toast.makeText(this@ExpenseAddActivity, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
