package com.example.antiburnin

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(pad, b.top + pad, pad, b.bottom + pad)
            insets
        }

        val title = TextView(this).apply {
            text = "Антивигорання для TikTok"
            textSize = 22f
        }

        status = TextView(this).apply { textSize = 16f }

        val openSettings = Button(this).apply {
            text = "1. Увімкнути службу"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val sw = Switch(this).apply {
            text = "Затемнення увімкнено"
            isChecked = Cfg.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Cfg.setEnabled(this@MainActivity, on) }
        }

        val label = TextView(this).apply { textSize = 16f }
        val seek = SeekBar(this).apply {
            max = 70
            progress = Cfg.visibility(this@MainActivity) - 20
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    label.text = "Видимість кнопок: ${p + 20}%"
                    if (fromUser) Cfg.setVisibility(this@MainActivity, p + 20)
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        label.text = "Видимість кнопок: ${seek.progress + 20}%"

        val edit = Button(this).apply {
            text = "2. Налаштувати зони поверх TikTok"
            setOnClickListener {
                if (!serviceOn()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Спочатку увімкни службу (кнопка 1)",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                Cfg.setEditMode(this@MainActivity, true)
                Toast.makeText(
                    this@MainActivity,
                    "Перетягни прямокутники на кнопки й натисни «Готово»",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Cfg.TIKTOK.firstNotNullOfOrNull {
                    packageManager.getLaunchIntentForPackage(it)
                }
                if (intent != null) startActivity(intent)
            }
        }

        root.addView(title)
        root.addView(status)
        root.addView(openSettings)
        root.addView(sw)
        root.addView(label)
        root.addView(seek)
        root.addView(edit)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (serviceOn()) "Служба: увімкнена ✅" else "Служба: вимкнена ❌"
    }

    private fun serviceOn(): Boolean {
        val s = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(this, DimService::class.java)
        return s.contains(cn.flattenToString()) || s.contains(cn.flattenToShortString())
    }
}
