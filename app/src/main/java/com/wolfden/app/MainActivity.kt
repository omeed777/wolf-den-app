package com.wolfden.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val black = Color.rgb(8, 8, 8)
    private val gold = Color.rgb(255, 215, 0)
    private val white = Color.rgb(245, 245, 245)
    private val muted = Color.rgb(184, 184, 184)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 40, 48, 40)
            setBackgroundColor(black)
        }

        val spacerTop = View(this)
        root.addView(spacerTop, LinearLayout.LayoutParams(1, 0, 1f))

        val brand = ImageView(this).apply {
            setImageResource(com.wolfden.app.R.drawable.ic_wolf_den)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Wolf Den logo"
        }
        val brandParams = LinearLayout.LayoutParams(-1, 150)
        brandParams.bottomMargin = 8
        root.addView(brand, brandParams)

        val title = TextView(this).apply {
            text = "WOLF DEN"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(gold)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val subtitle = TextView(this).apply {
            text = "CROSSFIT"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(gold)
        }
        root.addView(subtitle, LinearLayout.LayoutParams(-1, -2))

        val heading = TextView(this).apply {
            text = "ورود اعضا"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(white)
        }
        val headingParams = LinearLayout.LayoutParams(-1, -2)
        headingParams.topMargin = 42
        root.addView(heading, headingParams)

        val hint = TextView(this).apply {
            text = "برای ورود شماره موبایل خود را وارد کنید."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }
        root.addView(hint, LinearLayout.LayoutParams(-1, -2))

        val phone = EditText(this).apply {
            this.hint = "09xxxxxxxxx"
            textSize = 18f
            setTextColor(white)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_PHONE
            gravity = Gravity.CENTER
            setSingleLine(true)
        }
        val phoneParams = LinearLayout.LayoutParams(-1, 58)
        phoneParams.topMargin = 24
        root.addView(phone, phoneParams)

        val continueButton = Button(this).apply {
            text = "دریافت کد تایید"
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(gold)
            setOnClickListener {
                showOtp(phone.text.toString())
            }
        }
        val buttonParams = LinearLayout.LayoutParams(-1, 58)
        buttonParams.topMargin = 16
        root.addView(continueButton, buttonParams)

        val mode = TextView(this).apply {
            text = "نسخه آزمایشی Wolf Den"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }
        val modeParams = LinearLayout.LayoutParams(-1, -2)
        modeParams.topMargin = 14
        root.addView(mode, modeParams)

        val spacerBottom = View(this)
        root.addView(spacerBottom, LinearLayout.LayoutParams(1, 0, 1f))

        setContentView(root)
    }

    private fun showOtp(phone: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 40, 48, 40)
            setBackgroundColor(black)
        }

        val title = TextView(this).apply {
            text = "تایید شماره"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(white)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val info = TextView(this).apply {
            text = "کد تایید برای $phone"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(muted)
        }
        val infoParams = LinearLayout.LayoutParams(-1, -2)
        infoParams.topMargin = 10
        root.addView(info, infoParams)

        val otp = EditText(this).apply {
            hint = "کد ۶ رقمی"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(white)
            setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }
        val otpParams = LinearLayout.LayoutParams(-1, 58)
        otpParams.topMargin = 24
        root.addView(otp, otpParams)

        val login = Button(this).apply {
            text = "ورود به Wolf Den"
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(gold)
            setOnClickListener { showHome() }
        }
        val loginParams = LinearLayout.LayoutParams(-1, 58)
        loginParams.topMargin = 16
        root.addView(login, loginParams)

        val back = Button(this).apply {
            text = "ویرایش شماره موبایل"
            setOnClickListener { showLogin() }
        }
        root.addView(back, LinearLayout.LayoutParams(-1, 52))

        setContentView(root)
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(black)
        }
        val text = TextView(this).apply {
            text = "WOLF DEN\n\nورود موفق بود 🐺\n\nنسخه آزمایشی آماده است."
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(gold)
        }
        root.addView(text, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }
}
