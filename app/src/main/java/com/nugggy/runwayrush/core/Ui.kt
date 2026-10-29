package com.nugggy.runwayrush.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object Pal {
    const val INK = 0xFF1B1026.toInt()
    const val PLUM = 0xFF2E1A47.toInt()
    const val BERRY = 0xFFB0306B.toInt()
    const val PINK = 0xFFFF5C9A.toInt()
    const val CORAL = 0xFFFF8A5C.toInt()
    const val GOLD = 0xFFFFD86B.toInt()
    const val MINT = 0xFF5CE0B8.toInt()
    const val SKY = 0xFF6BC5FF.toInt()
    const val CREAM = 0xFFFFF4E8.toInt()
    const val GREY = 0xFF9A8FA8.toInt()
}

object Ui {
    val bold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    val black: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    val regular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val condensed: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
    val serif: Typeface = Typeface.create("serif", Typeface.BOLD_ITALIC)

    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int = Color.WHITE,
             align: Paint.Align = Paint.Align.LEFT, tf: Typeface = bold, shadow: Boolean = true, alpha: Int = 255) {
        tp.textSize = size; tp.typeface = tf; tp.textAlign = align; tp.shader = null
        if (shadow) {
            tp.color = Color.argb((alpha * 0.55f).toInt(), 0, 0, 0)
            c.drawText(s, x + size * 0.05f, y + size * 0.07f, tp)
        }
        tp.color = color; tp.alpha = alpha
        c.drawText(s, x, y, tp)
    }

    fun measure(s: String, size: Float, tf: Typeface = bold): Float {
        tp.textSize = size; tp.typeface = tf; return tp.measureText(s)
    }

    /** Draws wrapped text, returns the height used. */
    fun wrap(c: Canvas, s: String, x: Float, y: Float, maxW: Float, size: Float, color: Int = Color.WHITE,
             align: Paint.Align = Paint.Align.LEFT, tf: Typeface = regular, lineH: Float = size * 1.3f): Float {
        val words = s.split(" ")
        var line = ""
        var yy = y
        for (wd in words) {
            val t = if (line.isEmpty()) wd else "$line $wd"
            if (measure(t, size, tf) > maxW && line.isNotEmpty()) {
                text(c, line, x, yy, size, color, align, tf, shadow = false); yy += lineH; line = wd
            } else line = t
        }
        if (line.isNotEmpty()) { text(c, line, x, yy, size, color, align, tf, shadow = false); yy += lineH }
        return yy - y
    }

    fun panel(c: Canvas, rect: RectF, color: Int = 0xE6201230.toInt(), radius: Float = 26f, border: Int = 0x55FFFFFF) {
        sp.style = Paint.Style.FILL; sp.shader = null
        sp.color = 0x55000000
        r.set(rect.left + 4, rect.top + 8, rect.right + 4, rect.bottom + 8)
        c.drawRoundRect(r, radius, radius, sp)
        sp.color = color
        c.drawRoundRect(rect, radius, radius, sp)
        if (border != 0) {
            sp.style = Paint.Style.STROKE; sp.strokeWidth = 2.5f; sp.color = border
            c.drawRoundRect(rect, radius, radius, sp)
            sp.style = Paint.Style.FILL
        }
    }

    fun gradientBg(c: Canvas, w: Float, h: Float, top: Int, bottom: Int) {
        sp.color = -16777216; sp.shader = LinearGradient(0f, 0f, 0f, h, top, bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, sp)
        sp.shader = null
    }

    fun pill(c: Canvas, rect: RectF, color: Int) {
        sp.style = Paint.Style.FILL; sp.shader = null; sp.color = color
        val rr = min(rect.height(), rect.width()) / 2
        c.drawRoundRect(rect, rr, rr, sp)
    }

    fun star(c: Canvas, cx: Float, cy: Float, rOut: Float, color: Int, filled: Boolean = true) {
        val path = Path()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) rOut else rOut * 0.45f
            val a = -Math.PI / 2 + i * Math.PI / 5
            val x = cx + (rr * cos(a)).toFloat(); val y = cy + (rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        sp.shader = null; sp.color = color
        sp.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        sp.strokeWidth = rOut * 0.15f
        c.drawPath(path, sp)
        sp.style = Paint.Style.FILL
    }

    fun clockText(sec: Float): String {
        val s = sec.toInt().coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }
}

/** Vector icons, so the game needs no emoji glyphs. */
object Icons {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()

    fun draw(c: Canvas, name: String, cx: Float, cy: Float, s: Float, color: Int = Color.WHITE) {
        p.color = color; p.strokeWidth = s * 0.12f; p.style = Paint.Style.STROKE; p.shader = null
        val h = s / 2
        path.reset()
        when (name) {
            "back" -> { path.moveTo(cx + h * 0.4f, cy - h * 0.7f); path.lineTo(cx - h * 0.4f, cy); path.lineTo(cx + h * 0.4f, cy + h * 0.7f); c.drawPath(path, p) }
            "play" -> { p.style = Paint.Style.FILL; path.moveTo(cx - h * 0.5f, cy - h * 0.7f); path.lineTo(cx + h * 0.7f, cy); path.lineTo(cx - h * 0.5f, cy + h * 0.7f); path.close(); c.drawPath(path, p) }
            "close" -> { c.drawLine(cx - h * 0.6f, cy - h * 0.6f, cx + h * 0.6f, cy + h * 0.6f, p); c.drawLine(cx + h * 0.6f, cy - h * 0.6f, cx - h * 0.6f, cy + h * 0.6f, p) }
            "check" -> { path.moveTo(cx - h * 0.7f, cy); path.lineTo(cx - h * 0.2f, cy + h * 0.5f); path.lineTo(cx + h * 0.7f, cy - h * 0.6f); c.drawPath(path, p) }
            "people" -> {
                p.style = Paint.Style.FILL
                c.drawCircle(cx - h * 0.35f, cy - h * 0.35f, h * 0.28f, p); c.drawCircle(cx + h * 0.4f, cy - h * 0.3f, h * 0.24f, p)
                c.drawRoundRect(cx - h * 0.85f, cy, cx + h * 0.15f, cy + h * 0.8f, h * 0.3f, h * 0.3f, p)
                c.drawRoundRect(cx + h * 0.0f, cy + h * 0.05f, cx + h * 0.85f, cy + h * 0.8f, h * 0.3f, h * 0.3f, p)
            }
            "person" -> { p.style = Paint.Style.FILL; c.drawCircle(cx, cy - h * 0.35f, h * 0.35f, p); c.drawRoundRect(cx - h * 0.65f, cy + h * 0.1f, cx + h * 0.65f, cy + h * 0.85f, h * 0.35f, h * 0.35f, p) }
            "shirt" -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx - h * 0.3f, cy - h * 0.75f); path.lineTo(cx - h * 0.9f, cy - h * 0.35f); path.lineTo(cx - h * 0.65f, cy + h * 0.05f)
                path.lineTo(cx - h * 0.5f, cy - h * 0.1f); path.lineTo(cx - h * 0.5f, cy + h * 0.8f); path.lineTo(cx + h * 0.5f, cy + h * 0.8f)
                path.lineTo(cx + h * 0.5f, cy - h * 0.1f); path.lineTo(cx + h * 0.65f, cy + h * 0.05f); path.lineTo(cx + h * 0.9f, cy - h * 0.35f)
                path.lineTo(cx + h * 0.3f, cy - h * 0.75f); path.quadTo(cx, cy - h * 0.4f, cx - h * 0.3f, cy - h * 0.75f); path.close(); c.drawPath(path, p)
            }
            "hanger" -> {
                path.moveTo(cx, cy - h * 0.2f); path.lineTo(cx - h * 0.9f, cy + h * 0.5f); path.lineTo(cx + h * 0.9f, cy + h * 0.5f); path.close(); c.drawPath(path, p)
                path.reset(); path.moveTo(cx, cy - h * 0.2f); path.cubicTo(cx, cy - h * 0.5f, cx + h * 0.35f, cy - h * 0.55f, cx + h * 0.25f, cy - h * 0.8f); c.drawPath(path, p)
            }
            "dice" -> {
                c.drawRoundRect(cx - h * 0.75f, cy - h * 0.75f, cx + h * 0.75f, cy + h * 0.75f, h * 0.25f, h * 0.25f, p)
                p.style = Paint.Style.FILL
                for ((dx, dy) in listOf(-0.35f to -0.35f, 0f to 0f, 0.35f to 0.35f, 0.35f to -0.35f, -0.35f to 0.35f)) c.drawCircle(cx + dx * h, cy + dy * h, h * 0.1f, p)
            }
            "gear" -> {
                c.drawCircle(cx, cy, h * 0.42f, p)
                for (i in 0 until 8) { val a = i * Math.PI / 4; c.drawLine(cx + (h * 0.55f * cos(a)).toFloat(), cy + (h * 0.55f * sin(a)).toFloat(), cx + (h * 0.85f * cos(a)).toFloat(), cy + (h * 0.85f * sin(a)).toFloat(), p) }
            }
            "wifi" -> {
                for (i in 1..3) { val rr = h * 0.3f * i; c.drawArc(cx - rr, cy + h * 0.5f - rr, cx + rr, cy + h * 0.5f + rr, 225f, 90f, false, p) }
                p.style = Paint.Style.FILL; c.drawCircle(cx, cy + h * 0.5f, h * 0.1f, p)
            }
            "clock" -> { c.drawCircle(cx, cy, h * 0.8f, p); c.drawLine(cx, cy, cx, cy - h * 0.5f, p); c.drawLine(cx, cy, cx + h * 0.35f, cy + h * 0.1f, p) }
            "map" -> {
                path.moveTo(cx - h * 0.8f, cy - h * 0.6f); path.lineTo(cx - h * 0.27f, cy - h * 0.8f); path.lineTo(cx + h * 0.27f, cy - h * 0.6f); path.lineTo(cx + h * 0.8f, cy - h * 0.8f)
                path.lineTo(cx + h * 0.8f, cy + h * 0.6f); path.lineTo(cx + h * 0.27f, cy + h * 0.8f); path.lineTo(cx - h * 0.27f, cy + h * 0.6f); path.lineTo(cx - h * 0.8f, cy + h * 0.8f); path.close()
                c.drawPath(path, p); c.drawLine(cx - h * 0.27f, cy - h * 0.8f, cx - h * 0.27f, cy + h * 0.6f, p); c.drawLine(cx + h * 0.27f, cy - h * 0.6f, cx + h * 0.27f, cy + h * 0.8f, p)
            }
            "download" -> { c.drawLine(cx, cy - h * 0.8f, cx, cy + h * 0.3f, p); path.moveTo(cx - h * 0.45f, cy - h * 0.1f); path.lineTo(cx, cy + h * 0.35f); path.lineTo(cx + h * 0.45f, cy - h * 0.1f); c.drawPath(path, p); c.drawLine(cx - h * 0.8f, cy + h * 0.8f, cx + h * 0.8f, cy + h * 0.8f, p) }
            "heart" -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx, cy + h * 0.8f); path.cubicTo(cx - h * 1.2f, cy - h * 0.1f, cx - h * 0.6f, cy - h * 1.0f, cx, cy - h * 0.35f)
                path.cubicTo(cx + h * 0.6f, cy - h * 1.0f, cx + h * 1.2f, cy - h * 0.1f, cx, cy + h * 0.8f); c.drawPath(path, p)
            }
            "crown" -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx - h * 0.85f, cy + h * 0.6f); path.lineTo(cx - h * 0.85f, cy - h * 0.5f); path.lineTo(cx - h * 0.4f, cy); path.lineTo(cx, cy - h * 0.75f)
                path.lineTo(cx + h * 0.4f, cy); path.lineTo(cx + h * 0.85f, cy - h * 0.5f); path.lineTo(cx + h * 0.85f, cy + h * 0.6f); path.close(); c.drawPath(path, p)
            }
            "music" -> {
                p.style = Paint.Style.FILL
                c.drawCircle(cx - h * 0.45f, cy + h * 0.5f, h * 0.25f, p); c.drawCircle(cx + h * 0.45f, cy + h * 0.35f, h * 0.25f, p)
                p.style = Paint.Style.STROKE
                path.moveTo(cx - h * 0.22f, cy + h * 0.5f); path.lineTo(cx - h * 0.22f, cy - h * 0.6f); path.lineTo(cx + h * 0.68f, cy - h * 0.75f); path.lineTo(cx + h * 0.68f, cy + h * 0.35f); c.drawPath(path, p)
            }
            "mute" -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx - h * 0.8f, cy - h * 0.25f); path.lineTo(cx - h * 0.4f, cy - h * 0.25f); path.lineTo(cx, cy - h * 0.7f); path.lineTo(cx, cy + h * 0.7f); path.lineTo(cx - h * 0.4f, cy + h * 0.25f); path.lineTo(cx - h * 0.8f, cy + h * 0.25f); path.close(); c.drawPath(path, p)
                p.style = Paint.Style.STROKE; c.drawLine(cx + h * 0.25f, cy - h * 0.3f, cx + h * 0.8f, cy + h * 0.3f, p); c.drawLine(cx + h * 0.8f, cy - h * 0.3f, cx + h * 0.25f, cy + h * 0.3f, p)
            }
            "trophy" -> {
                p.style = Paint.Style.FILL
                path.moveTo(cx - h * 0.55f, cy - h * 0.75f); path.lineTo(cx + h * 0.55f, cy - h * 0.75f); path.quadTo(cx + h * 0.55f, cy + h * 0.2f, cx, cy + h * 0.25f); path.quadTo(cx - h * 0.55f, cy + h * 0.2f, cx - h * 0.55f, cy - h * 0.75f); c.drawPath(path, p)
                c.drawRect(cx - h * 0.1f, cy + h * 0.2f, cx + h * 0.1f, cy + h * 0.55f, p); c.drawRect(cx - h * 0.45f, cy + h * 0.55f, cx + h * 0.45f, cy + h * 0.8f, p)
                p.style = Paint.Style.STROKE; c.drawArc(cx - h * 0.95f, cy - h * 0.65f, cx - h * 0.35f, cy - h * 0.05f, 90f, 180f, false, p); c.drawArc(cx + h * 0.35f, cy - h * 0.65f, cx + h * 0.95f, cy - h * 0.05f, 270f, 180f, false, p)
            }
            "eye" -> { c.drawOval(cx - h * 0.85f, cy - h * 0.45f, cx + h * 0.85f, cy + h * 0.45f, p); p.style = Paint.Style.FILL; c.drawCircle(cx, cy, h * 0.25f, p) }
            "flag" -> { c.drawLine(cx - h * 0.6f, cy - h * 0.8f, cx - h * 0.6f, cy + h * 0.85f, p); p.style = Paint.Style.FILL; path.moveTo(cx - h * 0.6f, cy - h * 0.8f); path.lineTo(cx + h * 0.75f, cy - h * 0.45f); path.lineTo(cx - h * 0.6f, cy - h * 0.05f); path.close(); c.drawPath(path, p) }
            "share" -> {
                c.drawLine(cx - h * 0.5f, cy, cx + h * 0.45f, cy - h * 0.5f, p); c.drawLine(cx - h * 0.5f, cy, cx + h * 0.45f, cy + h * 0.5f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx - h * 0.5f, cy, h * 0.24f, p); c.drawCircle(cx + h * 0.45f, cy - h * 0.5f, h * 0.24f, p); c.drawCircle(cx + h * 0.45f, cy + h * 0.5f, h * 0.24f, p)
            }
            "undo" -> { c.drawArc(cx - h * 0.6f, cy - h * 0.6f, cx + h * 0.6f, cy + h * 0.6f, 200f, 260f, false, p); path.moveTo(cx - h * 0.85f, cy - h * 0.45f); path.lineTo(cx - h * 0.55f, cy - h * 0.05f); path.lineTo(cx - h * 0.15f, cy - h * 0.35f); c.drawPath(path, p) }
            else -> c.drawCircle(cx, cy, h * 0.5f, p)
        }
    }
}

/** A tappable widget. */
open class Button(
    var label: String,
    var icon: String? = null,
    var color: Int = Pal.BERRY,
    var textSize: Float = 30f,
    var onClick: () -> Unit = {},
) {
    val rect = RectF()
    var pressed = false
    var enabled = true
    var visible = true
    var selected = false
    var pointer = -1
    var radius = -1f
    var sublabel: String? = null
    var drawFn: ((Canvas, Button) -> Unit)? = null

    fun set(l: Float, t: Float, wd: Float, ht: Float): Button { rect.set(l, t, l + wd, t + ht); return this }

    open fun draw(c: Canvas) {
        if (!visible) return
        drawFn?.let { it(c, this); return }
        val rr = if (radius >= 0) radius else min(rect.height() / 2, 34f)
        val p = Ui.p
        p.style = Paint.Style.FILL
        val base = if (!enabled) 0xFF4A4054.toInt() else color
        // shadow
        p.shader = null; p.color = 0x66000000
        c.drawRoundRect(rect.left, rect.top + 6, rect.right, rect.bottom + 6, rr, rr, p)
        val dy = if (pressed) 4f else 0f
        p.color = -16777216; p.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, lighten(base, 0.22f), darken(base, 0.12f), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect.left, rect.top + dy, rect.right, rect.bottom + dy, rr, rr, p)
        p.shader = null
        if (selected) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = Pal.GOLD
            c.drawRoundRect(rect.left - 3, rect.top - 3 + dy, rect.right + 3, rect.bottom + 3 + dy, rr + 3, rr + 3, p)
            p.style = Paint.Style.FILL
        }
        // gloss
        p.color = 0x22FFFFFF
        c.drawRoundRect(rect.left + 6, rect.top + dy + 4, rect.right - 6, rect.centerY() + dy, rr, rr, p)
        val cy = rect.centerY() + dy
        val ic = icon
        if (label.isEmpty() && ic != null) {
            Icons.draw(c, ic, rect.centerX(), cy, min(rect.height(), rect.width()) * 0.5f)
        } else if (ic != null) {
            val isz = rect.height() * 0.45f
            val tw = Ui.measure(label, textSize)
            val total = isz + 14 + tw
            val sx = rect.centerX() - total / 2
            Icons.draw(c, ic, sx + isz / 2, cy, isz)
            Ui.text(c, label, sx + isz + 14, cy + textSize * 0.36f, textSize)
        } else {
            val sub = sublabel
            if (sub != null) {
                Ui.text(c, label, rect.centerX(), cy - 2, textSize, align = Paint.Align.CENTER)
                Ui.text(c, sub, rect.centerX(), cy + textSize * 0.9f, textSize * 0.6f, 0xDDFFFFFF.toInt(), Paint.Align.CENTER, Ui.regular, false)
            } else Ui.text(c, label, rect.centerX(), cy + textSize * 0.36f, textSize, align = Paint.Align.CENTER)
        }
    }

    companion object {
        fun lighten(c: Int, f: Float): Int = Color.argb(Color.alpha(c),
            (Color.red(c) + (255 - Color.red(c)) * f).toInt(), (Color.green(c) + (255 - Color.green(c)) * f).toInt(), (Color.blue(c) + (255 - Color.blue(c)) * f).toInt())
        fun darken(c: Int, f: Float): Int = Color.argb(Color.alpha(c),
            (Color.red(c) * (1 - f)).toInt(), (Color.green(c) * (1 - f)).toInt(), (Color.blue(c) * (1 - f)).toInt())
    }
}

class UiList {
    val items = ArrayList<Button>()
    var sound: Sound? = null

    fun add(b: Button): Button { items.add(b); return b }
    fun clear() = items.clear()
    fun draw(c: Canvas) { for (b in items) b.draw(c) }

    fun touch(t: Touch): Boolean {
        when (t.type) {
            TouchType.DOWN -> for (b in items.asReversed()) {
                if (b.visible && b.enabled && b.rect.contains(t.x, t.y)) { b.pressed = true; b.pointer = t.id; return true }
            }
            TouchType.MOVE -> for (b in items) if (b.pointer == t.id) { b.pressed = b.rect.contains(t.x, t.y); return true }
            TouchType.UP -> for (b in items) if (b.pointer == t.id) {
                val hit = b.pressed && b.rect.contains(t.x, t.y)
                b.pressed = false; b.pointer = -1
                if (hit) { sound?.play(Sfx.CLICK); b.onClick() }
                return true
            }
        }
        return false
    }
}
