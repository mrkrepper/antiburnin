package com.example.antiburnin

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class DimService : AccessibilityService() {

    private var view: ZonesView? = null
    private var tiktokOpen = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled" || key == "visibility" || key == "editMode") refresh()
    }

    override fun onServiceConnected() {
        Cfg.prefs(this).registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg in Cfg.TIKTOK) {
            tiktokOpen = true
        } else if (!isIgnored(pkg)) {
            tiktokOpen = false
        } else {
            return
        }
        refresh()
    }

    // Шторка сповіщень і клавіатура не мають ховати затемнення
    private fun isIgnored(pkg: String): Boolean =
        pkg == "com.android.systemui" ||
            pkg.contains("inputmethod") ||
            pkg.contains("keyboard") ||
            pkg.contains("swiftkey")

    private fun refresh() {
        removeView()
        if (!tiktokOpen || !Cfg.enabled(this)) return

        val editing = Cfg.editMode(this)
        val v = ZonesView(this, editing) { Cfg.setEditMode(this, false) }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (!editing) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

        getSystemService(WindowManager::class.java).addView(v, lp)
        view = v
    }

    private fun removeView() {
        view?.let {
            try {
                getSystemService(WindowManager::class.java).removeView(it)
            } catch (_: Exception) {
            }
        }
        view = null
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        Cfg.prefs(this).unregisterOnSharedPreferenceChangeListener(listener)
        removeView()
        super.onDestroy()
    }
}
