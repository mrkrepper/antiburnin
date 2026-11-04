package com.example.antiburnin

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

object Cfg {
    val TIKTOK = listOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.ss.android.ugc.aweme"
    )

    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences("antiburnin", Context.MODE_PRIVATE)

    fun enabled(c: Context) = prefs(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean("enabled", v).apply()

    fun visibility(c: Context) = prefs(c).getInt("visibility", 50)
    fun setVisibility(c: Context, v: Int) = prefs(c).edit().putInt("visibility", v).apply()

    fun editMode(c: Context) = prefs(c).getBoolean("editMode", false)
    fun setEditMode(c: Context, v: Boolean) = prefs(c).edit().putBoolean("editMode", v).apply()

    // Зони в частках екрана: лівий, верхній, правий, нижній край
    private val defaults = arrayOf(
        floatArrayOf(0.84f, 0.48f, 1.00f, 0.84f),  // колонка кнопок справа
        floatArrayOf(0.40f, 0.91f, 0.60f, 0.975f)  // плюсик знизу
    )

    fun zone(c: Context, i: Int): RectF {
        val d = defaults[i]
        val p = prefs(c)
        return RectF(
            p.getFloat("z${i}l", d[0]),
            p.getFloat("z${i}t", d[1]),
            p.getFloat("z${i}r", d[2]),
            p.getFloat("z${i}b", d[3])
        )
    }

    fun setZone(c: Context, i: Int, r: RectF) {
        prefs(c).edit()
            .putFloat("z${i}l", r.left)
            .putFloat("z${i}t", r.top)
            .putFloat("z${i}r", r.right)
            .putFloat("z${i}b", r.bottom)
            .apply()
    }
}

class ZonesView(
    context: Context,
    private val editing: Boolean,
    private val onDone: () -> Unit
) : View(context) {

    private val rects = Array(2) { Cfg.zone(context, it) }

    private val fill = Paint()
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.CYAN
    }
    private val handle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN }
    private val button = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E7D32") }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    private val doneRect = RectF()
    private var active = -1
    private var resizing = false
    private var pressedDone = false
    private var lastX = 0f
    private var lastY = 0f

    private fun px(i: Int) = RectF(
        rects[i].left * width,
        rects[i].top * height,
        rects[i].right * width,
        rects[i].bottom * height
    )

    override fun onDraw(canvas: Canvas) {
        val alpha = (100 - Cfg.visibility(context)) * 255 / 100
        fill.color = Color.argb(alpha, 0, 0, 0)

        for (i in 0..1) {
            val r = px(i)
            canvas.drawRect(r, fill)
            if (editing) {
                canvas.drawRect(r, stroke)
                canvas.drawCircle(r.right - 45f, r.bottom - 45f, 36f, handle)
                canvas.drawText(if (i == 0) "Справа" else "+", r.centerX(), r.centerY(), label)
            }
        }

        if (editing) {
            doneRect.set(width / 2f - 160f, 120f, width / 2f + 160f, 240f)
            canvas.drawRoundRect(doneRect, 30f, 30f, button)
            canvas.drawText("Готово", doneRect.centerX(), doneRect.centerY() + 12f, label)
            canvas.drawText(
                "Тягни прямокутник, кружечок змінює розмір",
                width / 2f, 300f, label
            )
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!editing) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedDone = doneRect.contains(e.x, e.y)
                active = -1
                if (!pressedDone) {
                    for (i in 0..1) {
                        val r = px(i)
                        if (hypot(e.x - (r.right - 45f), e.y - (r.bottom - 45f)) < 80f) {
                            active = i
                            resizing = true
                            break
                        }
                    }
                    if (active == -1) {
                        for (i in 0..1) {
                            if (px(i).contains(e.x, e.y)) {
                                active = i
                                resizing = false
                                break
                            }
                        }
                    }
                }
                lastX = e.x
                lastY = e.y
            }

            MotionEvent.ACTION_MOVE -> {
                if (active >= 0) {
                    val dx = (e.x - lastX) / width
                    val dy = (e.y - lastY) / height
                    val r = rects[active]
                    if (resizing) {
                        r.right = maxOf(r.left + 0.05f, minOf(r.right + dx, 1f))
                        r.bottom = maxOf(r.top + 0.03f, minOf(r.bottom + dy, 1f))
                    } else {
                        val w = r.width()
                        val h = r.height()
                        val l = (r.left + dx).coerceIn(0f, 1f - w)
                        val t = (r.top + dy).coerceIn(0f, 1f - h)
                        r.set(l, t, l + w, t + h)
                    }
                    lastX = e.x
                    lastY = e.y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (active >= 0) Cfg.setZone(context, active, rects[active])
                if (pressedDone && e.actionMasked == MotionEvent.ACTION_UP &&
                    doneRect.contains(e.x, e.y)
                ) {
                    post { onDone() }
                }
                active = -1
                pressedDone = false
            }
        }
        return true
    }
}
