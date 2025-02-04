package com.github.ccxxxi.ecnutimetable.ecnudb

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.github.ccxxxi.ecnutimetable.R
import com.github.ccxxxi.ecnutimetable.databinding.ActivityImportTimetableFromEcnudbBinding
import com.googlecode.tesseract.android.Ocr

class ImportTimetableFromEcnudbActivity : AppCompatActivity() {
    private lateinit var ecnudbViewModel: EcnudbViewModel
    private lateinit var binding: ActivityImportTimetableFromEcnudbBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImportTimetableFromEcnudbBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val captchaVal = binding.captchaVal
        val captchaImg = binding.captchaImg
        val getTimetable = binding.getTimetable
        val loading = binding.loading
        val year = binding.year
        val sem = binding.sem

        ecnudbViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(EcnudbViewModel::class.java)

        ecnudbViewModel.captchaImg.observe(this@ImportTimetableFromEcnudbActivity, {
            val img = it!!

            // show captcha image
            captchaImg.setImageBitmap(img)

            // recognize the captcha automatically
            val v = Ocr(this).rec(it)
            captchaVal.setText(v)
        })

        ArrayAdapter.createFromResource(
            this,
            R.array.year_arr,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            year.adapter = it
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.sem_arr,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sem.adapter = it
        }

        // todo: 获取当前学年学期并设置下拉框初始值
        // todo: 把学号密码保存到本地以便自动填写

        ecnudbViewModel.formState.observe(this@ImportTimetableFromEcnudbActivity, {
            val state = it!!

            // button is enabled iff data is valid
            getTimetable.isEnabled = state.isDataValid

            // show error message if any
            username.error = state.usernameError?.let(::getString)
            password.error = state.passwordError?.let(::getString)
            captchaVal.error = state.captchaError?.let(::getString)
        })

        fun loginDataChanged() = ecnudbViewModel.loginDataChanged(
            username.text.toString(),
            password.text.toString(),
            captchaVal.text.toString(),
        )
        username.afterTextChanged(::loginDataChanged)
        password.afterTextChanged(::loginDataChanged)
        captchaVal.afterTextChanged(::loginDataChanged)

        fun getTimetable() {
            if (!getTimetable.isEnabled) return

            loading.visibility = View.VISIBLE

            ecnudbViewModel.getTimetable(
                username.text.toString(),
                password.text.toString(),
                captchaVal.text.toString(),
                year.selectedItem.toString().slice(0..3).toInt(),
                sem.selectedItemPosition,
            )
        }
        // todo: more listener; 验证码已自动识别，不需要把焦点挪过去
        captchaVal.setOnEditorActionListener { _, _, _ ->
            getTimetable()
            true
        }
        getTimetable.setOnClickListener { getTimetable() }

        ecnudbViewModel.loginResult.observe(this@ImportTimetableFromEcnudbActivity, {
            when (val r = it!!) {
                is Error -> {
                    Toast.makeText(this, r.errorMessage, Toast.LENGTH_LONG).show()
                    TODO("获取失败后续处理")
                }
                is Success -> {
                    Toast.makeText(this, "${r.realName}登陆成功，正在获取课表", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: () -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) = afterTextChanged.invoke()
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
    })
}
