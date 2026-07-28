package com.guru2.payday.ui.expense

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.guru2.payday.data.local.ExpenseEntity
import com.guru2.payday.data.local.PaydayDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ExpenseAddActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXPENSE_TYPE = "EXTRA_EXPENSE_TYPE"
        const val EXTRA_EXPENSE_ID = "EXTRA_EXPENSE_ID"
    }

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var chipGroupCategory: ChipGroup
    private lateinit var btnSave: Button
    private lateinit var btnClose: TextView
    private lateinit var tvTopTitle: TextView

    private var selectedCategory = "월급"
    private var calendar = Calendar.getInstance()
    private var expenseType = ExpenseEntity.TYPE_VARIABLE // 기본값 지출

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 전달받은 타입 확인 (INCOME이면 수입, 아니면 지출)
        expenseType = intent.getStringExtra(EXTRA_EXPENSE_TYPE) ?: ExpenseEntity.TYPE_VARIABLE

        setupDynamicLayout()
        setupUI()
        setupListeners()
    }

    private fun setupDynamicLayout() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // 상단 타이틀 레이아웃
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

        // 수입인지 지출인지에 따라 타이틀 동적 변경
        val titleText = if (expenseType == "INCOME") "수입 추가" else "지출 추가"
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

        // 이름 입력
        val nameLabel = if (expenseType == "INCOME") "수입 이름" else "지출 항목"
        rootLayout.addView(createLabel(nameLabel))
        etTitle = EditText(this).apply {
            hint = if (expenseType == "INCOME") "수입 이름 입력" else "지출 항목 입력"
            layoutParams = createParam()
        }
        rootLayout.addView(etTitle)

        // 금액 입력
        rootLayout.addView(createLabel("금액"))
        etAmount = EditText(this).apply {
            hint = "금액 입력"
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = createParam()
        }
        rootLayout.addView(etAmount)

        // 카테고리
        rootLayout.addView(createLabel("카테고리"))
        chipGroupCategory = ChipGroup(this).apply {
            isSingleSelection = true
            layoutParams = createParam()
        }

        // 수입과 지출에 맞는 카테고리 구성
        val categories = if (expenseType == "INCOME") {
            listOf("월급", "용돈", "환급", "기타")
        } else {
            listOf("식비", "교통", "쇼핑", "기타")
        }

        for ((index, cat) in categories.withIndex()) {
            val chip = Chip(this).apply {
                text = cat
                isCheckable = true
                if (index == 0) isChecked = true
            }
            chipGroupCategory.addView(chip)
        }
        selectedCategory = categories[0]
        rootLayout.addView(chipGroupCategory)

        // 날짜
        val dateLabel = if (expenseType == "INCOME") "입금일" else "결제 날짜"
        rootLayout.addView(createLabel(dateLabel))
        etDate = EditText(this).apply {
            hint = "mm/dd/yyyy"
            isFocusable = false
            isClickable = true
            layoutParams = createParam()
        }
        rootLayout.addView(etDate)

        // 저장 버튼
        btnSave = Button(this).apply {
            text = "저장하기"
            isEnabled = false
            alpha = 0.5f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            ).apply { topMargin = 60 }
        }
        rootLayout.addView(btnSave)

        setContentView(rootLayout)
    }

    private fun createLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 30; bottomMargin = 10 }
        }
    }

    private fun createParam(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

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
                validateInputs()
            }
        })

        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validateInputs() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }

        etDate.setOnClickListener {
            showDatePickerDialog()
        }

        chipGroupCategory.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            selectedCategory = chip?.text?.toString() ?: "월급"
            validateInputs()
        }

        btnSave.setOnClickListener {
            saveData()
        }
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

    private fun validateInputs() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()

        val isValid = title.isNotEmpty() && amount > 0 && date.isNotEmpty()

        btnSave.isEnabled = isValid
        btnSave.alpha = if (isValid) 1f else 0.5f
    }

    private fun saveData() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().replace(",", "")
        val amount = amountStr.toLongOrNull() ?: 0L
        val date = etDate.text.toString()

        lifecycleScope.launch(Dispatchers.IO) {
            val entity = ExpenseEntity(
                name = "[$selectedCategory] $title",
                category = selectedCategory,
                paymentMethod = "현금",
                paymentDate = date,
                amount = amount,
                type = expenseType // 수입 또는 지출 타입으로 저장
            )

            PaydayDatabase.getInstance(this@ExpenseAddActivity).expenseDao().insert(entity)

            withContext(Dispatchers.Main) {
                val msg = if (expenseType == "INCOME") "수입이 등록되었습니다." else "지출이 등록되었습니다."
                Toast.makeText(this@ExpenseAddActivity, msg, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
