package com.guru2.payday.ui.expense

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.InputFilter
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.guru2.payday.auth.UserSession
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.IncomeEntity
import com.guru2.payday.data.local.PaydayDatabase
import com.guru2.payday.notification.ExpenseNotificationScheduler
import com.guru2.payday.R
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseAddActivity : AppCompatActivity() {

    companion object {
        // 인텐트로 전달받을 데이터의 키값 상수 정의 (지출/수입 타입, 지출 ID)
        const val EXTRA_EXPENSE_TYPE = "EXTRA_EXPENSE_TYPE"
        const val EXTRA_EXPENSE_ID = "EXTRA_EXPENSE_ID"
        const val EXTRA_INCOME_ID = "EXTRA_INCOME_ID"
    }

    // UI 컴포넌트 선언
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
    private lateinit var tvMonthlyConversion: TextView
    private lateinit var btnDelete: TextView
    private lateinit var btnMinus: Button
    private lateinit var btnPlus: Button

    private lateinit var btnSave: Button
    private lateinit var btnClose: TextView
    private lateinit var tvTopTitle: TextView

    // 사용자가 선택한 입력값 상태 변수들 초기화
    private var selectedCategory = "여가"
    private var selectedExpenseType = ExpenseEntity.TYPE_VARIABLE
    private var selectedCycle = "월간"
    private var sharedPersonCount = 2
    private var calendar = Calendar.getInstance()
    private var expenseType = ExpenseEntity.TYPE_VARIABLE
    private lateinit var session: UserSession
    private var editingExpense: ExpenseEntity? = null
    private var editingIncome: IncomeEntity? = null

    // 인텐트로부터 수정할 지출 ID를 가져오는 프로퍼티
    private val expenseId: Long
        get() = intent.getLongExtra(EXTRA_EXPENSE_ID, 0L)

    private val incomeId: Long
        get() = intent.getLongExtra(EXTRA_INCOME_ID, 0L)

    // 인텐트 액션이 EDIT이고 지출 또는 수입 ID가 존재하면 수정 모드로 판단
    private val isEditMode: Boolean
        get() = intent.action == Intent.ACTION_EDIT && (expenseId > 0 || incomeId > 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = UserSession(this)

        // 로그인 상태가 아니면 화면 종료
        if (!session.isLoggedIn) {
            finish()
            return
        }

        // 전달받은 지출 유형(타입) 확인, 없으면 기본 변동 지출로 설정
        expenseType = intent.getStringExtra(EXTRA_EXPENSE_TYPE) ?: ExpenseEntity.TYPE_VARIABLE
        selectedExpenseType = expenseType

        // 코드로 동적 레이아웃 생성 및 UI 초기화 실행
        setupDynamicLayout()
        setupUI()
        setupListeners()

        // 수정 모드인 경우 기존 데이터를 불러와 폼에 채움
        if (isEditMode) {
            if (expenseType == "INCOME") loadIncomeForEdit() else loadExpenseForEdit()
        }
    }

    // XML 레이아웃 없이 코드로만 전체 화면 레이아웃을 동적으로 구성하는 함수
    private fun setupDynamicLayout() {
        // 스크롤뷰 생성
        val scrollView = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        // 전체 컨테이너 역할을 하는 수직 리니어 레이아웃 생성
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 상단 타이틀 바 레이아웃 (닫기 버튼 + 화면 제목)
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

        btnDelete = TextView(this).apply {
            id = R.id.deleteButton
            text = "삭제"
            textSize = 14f
            visibility = if (isEditMode) View.VISIBLE else View.GONE
            setPadding(20, 10, 10, 10)
        }

        // 수정 모드인지, 수입 등록인지 지출 추가인지에 따라 상단 타이틀 텍스트 동적 결정
        val titleText = when {
            isEditMode && expenseType == "INCOME" -> "수입 수정"
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
        topLayout.addView(btnDelete)
        rootLayout.addView(topLayout)

        // 지출 추가일 때만 고정/변동/저축 칩 그룹 탭 노출 (수입일 때는 숨김)
        if (expenseType != "INCOME") {
            chipGroupType = ChipGroup(this).apply {
                isSingleSelection = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 20; bottomMargin = 10 }
            }
            val types = listOf("고정 지출", "변동 지출", "저축/투자")
            val typeIds = listOf(R.id.fixedExpenseTab, R.id.variableExpenseTab, R.id.savingExpenseTab)
            for ((idx, t) in types.withIndex()) {
                val chip = Chip(this).apply {
                    id = typeIds[idx]
                    text = t
                    isCheckable = true
                    isChecked = when (expenseType) {
                        ExpenseEntity.TYPE_FIXED -> idx == 0
                        ExpenseEntity.TYPE_SAVING -> idx == 2
                        else -> idx == 1
                    }
                }
                chipGroupType.addView(chip)
            }
            rootLayout.addView(chipGroupType)
        }

        // 이름 입력란 생성 (수입/지출에 따라 라벨 및 힌트 변경)
        val nameLabel = if (expenseType == "INCOME") "수입 이름" else "지출 이름"
        rootLayout.addView(createLabel(nameLabel))
        etTitle = EditText(this).apply {
            id = R.id.expenseNameInput
            hint = if (expenseType == "INCOME") "수입 이름 입력" else "지출 이름 입력"
            filters = arrayOf(InputFilter.LengthFilter(20))
            layoutParams = createParam()
        }
        rootLayout.addView(etTitle)

        // 금액 입력란 생성 (숫자 키보드 지정)
        rootLayout.addView(createLabel("금액"))
        etAmount = EditText(this).apply {
            id = R.id.expenseAmountInput
            hint = "금액 입력"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = createParam()
        }
        rootLayout.addView(etAmount)

        // 카테고리 칩 그룹 생성
        rootLayout.addView(createLabel("카테고리"))
        chipGroupCategory = ChipGroup(this).apply {
            isSingleSelection = true
            layoutParams = createParam()
        }
        updateCategoryChips(if (expenseType == "INCOME") "INCOME" else selectedExpenseType)
        rootLayout.addView(chipGroupCategory)

        // 지출인 경우에만 결제 수단 입력란 추가
        if (expenseType != "INCOME") {
            rootLayout.addView(createLabel("결제 수단"))
            etPaymentMethod = EditText(this).apply {
                id = R.id.paymentMethodInput
                hint = "예: 현대카드, 현금"
                layoutParams = createParam()
            }
            rootLayout.addView(etPaymentMethod)
        }

        // 날짜(입금일/결제 날짜) 입력란 생성 (클릭 시 데이트 피커 호출)
        val dateLabel = if (expenseType == "INCOME") "입금일" else "결제 날짜"
        rootLayout.addView(createLabel(dateLabel))
        etDate = EditText(this).apply {
            id = R.id.paymentDateInput
            hint = "mm/dd/yyyy"
            isFocusable = false
            isClickable = true
            layoutParams = createParam()
        }
        rootLayout.addView(etDate)

        // 공유 여부 및 정기 결제 옵션 영역 (수입이 아닐 때만 구성)
        if (expenseType != "INCOME") {
            // 공유 여부 헤더 레이아웃
            layoutSharedHeader = LinearLayout(this).apply {
                id = R.id.shareSection
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
            switchShared = Switch(this).apply { id = R.id.shareSwitch }
            layoutSharedHeader.addView(tvSharedLabel)
            layoutSharedHeader.addView(switchShared)
            rootLayout.addView(layoutSharedHeader)

            // 공유 인원 상세 설정 레이아웃
            layoutSharedPeople = LinearLayout(this).apply {
                id = R.id.sharePeopleSection
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
            btnMinus = Button(this).apply {
                id = R.id.decreaseShareButton
                text = "-"
            }
            tvSharedCount = TextView(this).apply {
                id = R.id.sharePeopleCountText
                text = "2"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(20, 0, 20, 0)
            }
            btnPlus = Button(this).apply {
                id = R.id.increaseShareButton
                text = "+"
            }

            // 공유 인원 감소 버튼 리스너
            btnMinus.setOnClickListener {
                if (sharedPersonCount > 2) {
                    sharedPersonCount--
                    tvSharedCount.text = sharedPersonCount.toString()
                    updateMyShareAmount()
                }
                updateShareButtons()
            }
            // 공유 인원 증가 버튼 리스너
            btnPlus.setOnClickListener {
                if (sharedPersonCount < 10) {
                    sharedPersonCount++
                    tvSharedCount.text = sharedPersonCount.toString()
                    updateMyShareAmount()
                }
                updateShareButtons()
            }

            rowSharedSub.addView(tvSharedInfo)
            rowSharedSub.addView(btnMinus)
            rowSharedSub.addView(tvSharedCount)
            rowSharedSub.addView(btnPlus)
            layoutSharedPeople.addView(rowSharedSub)

            // 내 부담금 표시 텍스트뷰
            tvMyShareAmount = TextView(this).apply {
                id = R.id.shareCostText
                text = "총 0원 · 내 부담 0원(2명 공유)"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#3F51B5"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            layoutSharedPeople.addView(tvMyShareAmount)
            rootLayout.addView(layoutSharedPeople)

            // 정기 결제 옵션 레이아웃
            layoutRecurringOptions = LinearLayout(this).apply {
                id = R.id.recurringSection
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
                id = R.id.recurringDayInput
                hint = "1일"
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(2))
                layoutParams = createParam()
            }
            layoutRecurringOptions.addView(etRecurringDay)

            layoutRecurringOptions.addView(createLabel("반복 주기"))
            chipGroupCycle = ChipGroup(this).apply {
                id = R.id.recurringCycleInput
                isSingleSelection = true
                layoutParams = createParam()
            }
            val cycles = listOf("월간", "연간")
            for ((idx, c) in cycles.withIndex()) {
                val chip = Chip(this).apply {
                    text = c
                    isCheckable = true
                    if (idx == 0) isChecked = true
                }
                chipGroupCycle.addView(chip)
            }
            layoutRecurringOptions.addView(chipGroupCycle)
            tvMonthlyConversion = TextView(this).apply {
                id = R.id.monthlyConversionText
                visibility = View.GONE
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            }
            layoutRecurringOptions.addView(tvMonthlyConversion)
            rootLayout.addView(layoutRecurringOptions)
            applyExpenseTypeVisibility()
        }

        // 저장/수정 버튼 생성 (초기에는 입력 검증 전이므로 비활성화)
        btnSave = Button(this).apply {
            id = R.id.saveButton
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

    // 선택된 지출/수입 타입에 맞춰 카테고리 칩 목록을 동적으로 갱신하는 함수
    private fun updateCategoryChips(type: String) {
        chipGroupCategory.removeAllViews()
        val categories = when (type) {
            "INCOME" -> listOf("월급", "용돈", "환급", "기타")
            ExpenseEntity.TYPE_SAVING -> listOf("저축", "투자")
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

    // 입력 필드 상단의 라벨(TextView)을 생성하는 유틸 함수
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

    // 공통 레이아웃 파라미터(MATCH_PARENT, WRAP_CONTENT) 생성 함수
    private fun createParam(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // UI 입력 감지 및 텍스트 변화 리스너 설정 함수
    private fun setupUI() {
        // 금액 입력 시 실시간으로 천단위 콤마 포맷팅 및 입력 검증 수행
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

        // 제목(이름) 입력 시 입력 검증 수행
        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validateInputs() }
            override fun afterTextChanged(s: Editable?) {}
        })
        etDate.addTextChangedListener(validationWatcher())
        if (::etPaymentMethod.isInitialized) {
            etPaymentMethod.addTextChangedListener(validationWatcher())
        }
        if (::etRecurringDay.isInitialized) {
            etRecurringDay.addTextChangedListener(validationWatcher())
        }
    }

    // 공유 인원 수에 따른 개인 부담금을 계산하여 텍스트뷰에 반영하는 함수
    private fun updateMyShareAmount() {
        val amountStr = etAmount.text.toString().replace(",", "")
        val totalAmount = amountStr.toLongOrNull() ?: 0L
        val myShare = if (sharedPersonCount > 0) totalAmount / sharedPersonCount else totalAmount
        val formattedShare = NumberFormat.getNumberInstance(Locale.KOREA).format(myShare)
        val formattedTotal = NumberFormat.getNumberInstance(Locale.KOREA).format(totalAmount)
        tvMyShareAmount.text = "총 ${formattedTotal}원 · 내 부담 ${formattedShare}원(${sharedPersonCount}명 공유)"
        updateMonthlyConversion()
    }

    private fun updateShareButtons() {
        btnMinus.isEnabled = sharedPersonCount > 2
        btnPlus.isEnabled = sharedPersonCount < 10
    }

    private fun updateMonthlyConversion() {
        if (!::tvMonthlyConversion.isInitialized) return
        val amount = etAmount.text.toString().replace(",", "").toLongOrNull() ?: 0L
        if (selectedExpenseType == ExpenseEntity.TYPE_FIXED && selectedCycle == "연간") {
            val converted = NumberFormat.getNumberInstance(Locale.KOREA).format(amount / 12)
            tvMonthlyConversion.text = "월 환산 약 ${converted}원"
            tvMonthlyConversion.visibility = View.VISIBLE
        } else {
            tvMonthlyConversion.visibility = View.GONE
        }
    }

    private fun validationWatcher() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = validateInputs()
        override fun afterTextChanged(s: Editable?) = Unit
    }

    private fun applyExpenseTypeVisibility() {
        val isFixed = selectedExpenseType == ExpenseEntity.TYPE_FIXED
        layoutRecurringOptions.visibility = if (isFixed) View.VISIBLE else View.GONE
        layoutSharedHeader.visibility = if (isFixed) View.VISIBLE else View.GONE
        if (!isFixed) {
            switchShared.isChecked = false
            layoutSharedPeople.visibility = View.GONE
        }
        updateMonthlyConversion()
        validateInputs()
    }

    // 각종 버튼 및 칩 선택 이벤트 리스너 설정 함수
    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }
        etDate.setOnClickListener { showDatePickerDialog() }

        if (expenseType != "INCOME") {
            // 공유 여부 스위치 토글 리스너
            switchShared.setOnCheckedChangeListener { _, isChecked ->
                layoutSharedPeople.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked) updateMyShareAmount()
            }

            // 지출 타입(고정, 변동, 저축/투자) 칩 그룹 선택 변경 리스너
            chipGroupType.setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                val typeName = chip?.text?.toString()
                selectedExpenseType = when (typeName) {
                    "고정 지출" -> {
                        ExpenseEntity.TYPE_FIXED
                    }
                    "저축/투자" -> {
                        ExpenseEntity.TYPE_SAVING
                    }
                    else -> ExpenseEntity.TYPE_VARIABLE
                }
                updateCategoryChips(selectedExpenseType)
                applyExpenseTypeVisibility()
            }

            // 반복 주기 칩 그룹 선택 변경 리스너
            chipGroupCycle.setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                selectedCycle = chip?.text?.toString() ?: "월간"
                updateMonthlyConversion()
                validateInputs()
            }
        }

        // 카테고리 칩 그룹 선택 변경 리스너
        chipGroupCategory.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            selectedCategory = chip?.text?.toString() ?: "여가"
            validateInputs()
        }

        // 저장 버튼 클릭 리스너
        btnSave.setOnClickListener { saveData() }
        btnDelete.setOnClickListener { confirmDelete() }
    }

    // 날짜 선택을 위한 DatePickerDialog를 띄우는 함수
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

    // 모든 필수 입력값이 유효할 때만 저장 버튼을 활성화한다.
    private fun validateInputs() {
        if (!::btnSave.isInitialized) return
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()
        val categoryValid = selectedCategory.isNotBlank()
        val paymentMethodValid = expenseType == "INCOME" ||
            (::etPaymentMethod.isInitialized && etPaymentMethod.text.toString().trim().isNotEmpty())
        val recurringValid = selectedExpenseType != ExpenseEntity.TYPE_FIXED ||
            (::etRecurringDay.isInitialized &&
                (etRecurringDay.text.toString().filter(Char::isDigit).toIntOrNull() in 1..31))

        val isValid = title.isNotEmpty() && amount > 0 && date.isNotEmpty() &&
            categoryValid && paymentMethodValid && recurringValid
        btnSave.isEnabled = isValid
        btnSave.alpha = if (isValid) 1f else 0.5f
    }

    // 수정 모드 진입 시 기존 데이터베이스에서 지출 정보를 비동기로 불러와 폼에 세팅하는 함수
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
            selectedExpenseType = expense.type
            selectChipForText(chipGroupType, when (expense.type) {
                ExpenseEntity.TYPE_FIXED -> "고정 지출"
                ExpenseEntity.TYPE_SAVING -> "저축/투자"
                else -> "변동 지출"
            })
            updateCategoryChips(expense.type)
            selectChipForText(chipGroupCategory, expense.category)
            selectedCategory = expense.category
            etTitle.setText(expense.name)
            etAmount.setText(NumberFormat.getNumberInstance(Locale.KOREA).format(expense.amount))
            etDate.setText(expense.paymentDate)
            if (::etPaymentMethod.isInitialized) {
                etPaymentMethod.setText(expense.paymentMethod)
            }
            if (expense.type == ExpenseEntity.TYPE_FIXED) {
                etRecurringDay.setText((expense.recurringDay ?: 1).toString())
                selectedCycle = if (expense.recurrence == ExpenseEntity.RECURRENCE_YEARLY) "연간" else "월간"
                selectChipForText(chipGroupCycle, selectedCycle)
                switchShared.isChecked = expense.isShared
                sharedPersonCount = expense.shareCount.coerceIn(2, 10)
                tvSharedCount.text = sharedPersonCount.toString()
                updateShareButtons()
                updateMyShareAmount()
            }
            applyExpenseTypeVisibility()
            validateInputs()
        }
    }

    // 현재 사용자의 수입을 조회해 수정 화면에 기존 값을 채운다.
    private fun loadIncomeForEdit() {
        lifecycleScope.launch {
            val income = withContext(Dispatchers.IO) {
                PaydayDatabase.getInstance(this@ExpenseAddActivity)
                    .incomeDao()
                    .getById(incomeId, session.userId)
            }
            if (income == null) {
                finish()
                return@launch
            }
            editingIncome = income
            updateCategoryChips("INCOME")
            selectChipForText(chipGroupCategory, income.category)
            selectedCategory = income.category
            etTitle.setText(income.name)
            etAmount.setText(NumberFormat.getNumberInstance(Locale.KOREA).format(income.amount))
            etDate.setText(income.receivedDate)
            validateInputs()
        }
    }

    private fun selectChipForText(group: ChipGroup, text: String) {
        (0 until group.childCount)
            .map { group.getChildAt(it) }
            .filterIsInstance<Chip>()
            .firstOrNull { it.text.toString() == text }
            ?.let { group.check(it.id) }
    }

    // 입력된 데이터를 수집하여 데이터베이스에 저장(신규 또는 수정)하는 함수
    private fun saveData() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()
        val method = if (::etPaymentMethod.isInitialized) etPaymentMethod.text.toString().trim() else ""
        val isFixed = selectedExpenseType == ExpenseEntity.TYPE_FIXED
        val recurringDay = if (isFixed) {
            etRecurringDay.text.toString().filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 31)
        } else null
        val recurrence = when {
            !isFixed -> null
            selectedCycle == "연간" -> ExpenseEntity.RECURRENCE_YEARLY
            else -> ExpenseEntity.RECURRENCE_MONTHLY
        }
        val nextPaymentDate = recurringDay?.let { calculateNextPaymentDate(date, it, recurrence) }
        val isShared = isFixed && switchShared.isChecked
        val shareCount = if (isShared) sharedPersonCount.coerceIn(2, 10) else 1

        lifecycleScope.launch(Dispatchers.IO) {
            val database = PaydayDatabase.getInstance(this@ExpenseAddActivity)

            // 수입 등록일 때와 지출 등록일 때 저장 테이블 분기 처리
            if (expenseType == "INCOME") {
                val currentIncome = editingIncome
                if (currentIncome != null) {
                    database.incomeDao().update(
                        currentIncome.copy(
                            name = title,
                            amount = amount,
                            category = selectedCategory,
                            receivedDate = date,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                } else {
                    database.incomeDao().insert(
                        IncomeEntity(
                            userId = session.userId,
                            name = title,
                            amount = amount,
                            category = selectedCategory,
                            receivedDate = date,
                        ),
                    )
                }
            } else {
                val current = editingExpense
                if (current != null) {
                    val updated = current.copy(
                        name = title,
                        amount = amount,
                        category = selectedCategory,
                        paymentMethod = method,
                        paymentDate = date,
                        type = selectedExpenseType,
                        isShared = isShared,
                        shareCount = shareCount,
                        recurringDay = recurringDay,
                        recurrence = recurrence,
                        nextPaymentDate = nextPaymentDate,
                        updatedAt = System.currentTimeMillis(),
                    )
                    database.expenseDao().update(updated)
                    updateNotification(updated)
                } else {
                    val expense = ExpenseEntity(
                        userId = session.userId,
                        name = title,
                        category = selectedCategory,
                        paymentMethod = method,
                        paymentDate = date,
                        amount = amount,
                        type = selectedExpenseType,
                        isShared = isShared,
                        shareCount = shareCount,
                        recurringDay = recurringDay,
                        recurrence = recurrence,
                        nextPaymentDate = nextPaymentDate,
                    )
                    val id = database.expenseDao().insert(
                        ExpenseEntity(
                            userId = expense.userId,
                            name = expense.name,
                            category = expense.category,
                            paymentMethod = expense.paymentMethod,
                            paymentDate = expense.paymentDate,
                            amount = expense.amount,
                            type = expense.type,
                            isShared = expense.isShared,
                            shareCount = expense.shareCount,
                            recurringDay = expense.recurringDay,
                            recurrence = expense.recurrence,
                            nextPaymentDate = expense.nextPaymentDate,
                        ),
                    )
                    updateNotification(expense.copy(id = id))
                }
            }

            // 저장 완료 후 메인 스레드에서 토스트 메시지 출력 및 액티비티 종료
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ExpenseAddActivity, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun calculateNextPaymentDate(
        paymentDate: String,
        recurringDay: Int,
        recurrence: String?,
    ): String {
        val baseDate = runCatching { LocalDate.parse(paymentDate) }.getOrElse { LocalDate.now() }
        return if (recurrence == ExpenseEntity.RECURRENCE_YEARLY) {
            val thisYear = baseDate.withMonth(baseDate.monthValue)
                .withDayOfMonth(recurringDay.coerceAtMost(baseDate.lengthOfMonth()))
            if (thisYear.isAfter(baseDate)) thisYear.toString() else thisYear.plusYears(1).toString()
        } else {
            val candidate = baseDate.withDayOfMonth(recurringDay.coerceAtMost(baseDate.lengthOfMonth()))
            if (candidate.isAfter(baseDate)) candidate.toString() else {
                val nextMonth = baseDate.plusMonths(1)
                nextMonth.withDayOfMonth(recurringDay.coerceAtMost(nextMonth.lengthOfMonth())).toString()
            }
        }
    }

    private fun updateNotification(expense: ExpenseEntity) {
        if (expense.type == ExpenseEntity.TYPE_FIXED && expense.nextPaymentDate != null) {
            ExpenseNotificationScheduler.schedule(this, expense)
        } else {
            ExpenseNotificationScheduler.cancel(this, expense.id)
        }
    }

    private fun confirmDelete() {
        if (editingIncome != null) {
            AlertDialog.Builder(this)
                .setTitle("수입 삭제")
                .setMessage("이 수입 내역을 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제") { _, _ -> deleteIncome(requireNotNull(editingIncome)) }
                .show()
            return
        }
        val expense = editingExpense ?: return
        AlertDialog.Builder(this)
            .setTitle("지출 삭제")
            .setMessage("이 지출 내역을 삭제할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ -> deleteExpense(expense) }
            .show()
    }

    private fun deleteIncome(income: IncomeEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            PaydayDatabase.getInstance(this@ExpenseAddActivity).incomeDao().delete(income)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ExpenseAddActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK, Intent())
                finish()
            }
        }
    }

    private fun deleteExpense(expense: ExpenseEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            PaydayDatabase.getInstance(this@ExpenseAddActivity).expenseDao().delete(expense)
            ExpenseNotificationScheduler.cancel(this@ExpenseAddActivity, expense.id)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ExpenseAddActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK, Intent())
                finish()
            }
        }
    }
}
