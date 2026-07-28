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

class ExpenseAddActivity : AppCompatActivity() {

    companion object {
        // 인텐트로 전달받을 데이터의 키값 상수 정의 (지출/수입 타입, 지출 ID)
        const val EXTRA_EXPENSE_TYPE = "EXTRA_EXPENSE_TYPE"
        const val EXTRA_EXPENSE_ID = "EXTRA_EXPENSE_ID"
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

    // 인텐트로부터 수정할 지출 ID를 가져오는 프로퍼티
    private val expenseId: Long
        get() = intent.getLongExtra(EXTRA_EXPENSE_ID, 0L)

    // 인텐트 액션이 EDIT이고 ID가 존재하면 수정 모드로 판단
    private val isEditMode: Boolean
        get() = intent.action == android.content.Intent.ACTION_EDIT && expenseId > 0

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

        // 코드로 동적 레이아웃 생성 및 UI 초기화 실행
        setupDynamicLayout()
        setupUI()
        setupListeners()

        // 수정 모드인 경우 기존 데이터를 불러와 폼에 채움
        if (isEditMode) loadExpenseForEdit()
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

        // 수정 모드인지, 수입 등록인지 지출 추가인지에 따라 상단 타이틀 텍스트 동적 결정
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
            for ((idx, t) in types.withIndex()) {
                val chip = Chip(this).apply {
                    text = t
                    isCheckable = true
                    if (idx == 1) isChecked = true // 기본값은 변동 지출 선택
                }
                chipGroupType.addView(chip)
            }
            rootLayout.addView(chipGroupType)
        }

        // 이름 입력란 생성 (수입/지출에 따라 라벨 및 힌트 변경)
        val nameLabel = if (expenseType == "INCOME") "수입 이름" else "지출 이름"
        rootLayout.addView(createLabel(nameLabel))
        etTitle = EditText(this).apply {
            hint = if (expenseType == "INCOME") "수입 이름 입력" else "지출 이름 입력"
            layoutParams = createParam()
        }
        rootLayout.addView(etTitle)

        // 금액 입력란 생성 (숫자 키보드 지정)
        rootLayout.addView(createLabel("금액"))
        etAmount = EditText(this).apply {
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
        updateCategoryChips(if (expenseType == "INCOME") "INCOME" else ExpenseEntity.TYPE_VARIABLE)
        rootLayout.addView(chipGroupCategory)

        // 지출인 경우에만 결제 수단 입력란 추가
        if (expenseType != "INCOME") {
            rootLayout.addView(createLabel("결제 수단"))
            etPaymentMethod = EditText(this).apply {
                hint = "예: 현대카드, 현금"
                layoutParams = createParam()
            }
            rootLayout.addView(etPaymentMethod)
        }

        // 날짜(입금일/결제 날짜) 입력란 생성 (클릭 시 데이트 피커 호출)
        val dateLabel = if (expenseType == "INCOME") "입금일" else "결제 날짜"
        rootLayout.addView(createLabel(dateLabel))
        etDate = EditText(this).apply {
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

            // 공유 인원 상세 설정 레이아웃
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

            // 공유 인원 감소 버튼 리스너
            btnMinus.setOnClickListener {
                if (sharedPersonCount > 1) {
                    sharedPersonCount--
                    tvSharedCount.text = " $sharedPersonCount "
                    updateMyShareAmount()
                }
            }
            // 공유 인원 증가 버튼 리스너
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

            // 내 부담금 표시 텍스트뷰
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

            // 정기 결제 옵션 레이아웃
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

        // 저장/수정 버튼 생성 (초기에는 입력 검증 전이므로 비활성화)
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

    // 선택된 지출/수입 타입에 맞춰 카테고리 칩 목록을 동적으로 갱신하는 함수
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
    }

    // 공유 인원 수에 따른 개인 부담금을 계산하여 텍스트뷰에 반영하는 함수
    private fun updateMyShareAmount() {
        val amountStr = etAmount.text.toString().replace(",", "")
        val totalAmount = amountStr.toLongOrNull() ?: 0L
        val myShare = if (sharedPersonCount > 0) totalAmount / sharedPersonCount else totalAmount
        val formattedShare = NumberFormat.getNumberInstance(Locale.KOREA).format(myShare)
        tvMyShareAmount.text = "내 부담 (${formattedShare}원 / ${sharedPersonCount}명 공유)"
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

            // 반복 주기 칩 그룹 선택 변경 리스너
            chipGroupCycle.setOnCheckedChangeListener { group, checkedId ->
                val chip = group.findViewById<Chip>(checkedId)
                selectedCycle = chip?.text?.toString() ?: "월간"
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

    // 필수 입력값(제목, 금액, 날짜) 유효성을 검사하여 저장 버튼 활성화 여부를 결정하는 함수
    private fun validateInputs() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()

        val isValid = title.isNotEmpty() && amount > 0 && date.isNotEmpty()
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
            etTitle.setText(expense.name)
            etAmount.setText(NumberFormat.getNumberInstance(Locale.KOREA).format(expense.amount))
            etDate.setText(expense.paymentDate)
            if (::etPaymentMethod.isInitialized) {
                etPaymentMethod.setText(expense.paymentMethod)
            }
            validateInputs()
        }
    }

    // 입력된 데이터를 수집하여 데이터베이스에 저장(신규 또는 수정)하는 함수
    private fun saveData() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()
        val method = if (::etPaymentMethod.isInitialized) etPaymentMethod.text.toString().ifEmpty { "현금" } else "현금"

        lifecycleScope.launch(Dispatchers.IO) {
            val database = PaydayDatabase.getInstance(this@ExpenseAddActivity)

            // 수입 등록일 때와 지출 등록일 때 저장 테이블 분기 처리
            if (expenseType == "INCOME") {
                // 수입 데이터 저장 로직 처리부
            } else {
                val current = editingExpense
                if (current != null) {
                    // 기존 지출 내역 수정 업데이트
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
                    // 신규 지출 내역 삽입
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

            // 저장 완료 후 메인 스레드에서 토스트 메시지 출력 및 액티비티 종료
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ExpenseAddActivity, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
