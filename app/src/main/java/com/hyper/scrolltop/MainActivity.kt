package com.hyper.scrolltop

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }

        val title = TextView(this).apply {
            text = "澎湃OS 状态栏回顶设置"
            textSize = 22f
            setPadding(0, 0, 0, 40)
        }

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val switch = SwitchCompat(this).apply {
            text = "状态栏点击回顶开关"
            textSize = 18f
            isChecked = prefs.getBoolean("enable_scroll_top", true)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("enable_scroll_top", isChecked).apply()
            }
        }

        layout.addView(title)
        layout.addView(switch)
        setContentView(layout)
    }
}
