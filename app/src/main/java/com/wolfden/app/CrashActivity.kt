package com.wolfden.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message = intent.getStringExtra("message")
            ?: getSharedPreferences("wolf_den_crash", MODE_PRIVATE).getString("message", "Unknown crash")
            ?: "Unknown crash"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.rgb(8, 8, 8))
        }
        val title = TextView(this).apply {
            text = "Wolf Den — گزارش خطا"
            textSize = 24f
            setTextColor(Color.WHITE)
        }
        val detail = TextView(this).apply {
            text = message
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 24, 0, 24)
        }
        val scroll = ScrollView(this).apply { addView(detail) }
        val close = Button(this).apply {
            text = "بستن و تلاش دوباره"
            setOnClickListener {
                getSharedPreferences("wolf_den_crash", MODE_PRIVATE).edit().clear().apply()
                finishAffinity()
            }
        }
        root.addView(title)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(close)
        setContentView(root)
    }
}
