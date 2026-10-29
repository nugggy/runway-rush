package com.nugggy.runwayrush.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.nugggy.runwayrush.model.Avatar
import com.nugggy.runwayrush.render.AvatarRenderer.darker
import com.nugggy.runwayrush.render.AvatarRenderer.lighter
import com.nugggy.runwayrush.render.AvatarRenderer.withAlpha

object Face {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val shadowCols = intArrayOf(0, 0xFFB98A9A.toInt(), 0xFF3A2E3E.toInt(), 0xFFD8B4FF.toInt(), 0xFF1A1A1A.toInt(), 0xFFFF7AB8.toInt())

    fun draw(c: Canvas, b: Body, pose: Pose, t: Float) {
        val a = b.a
        val skin = b.skin
        p.shader = null
        // makeup: eyeshadow
        if (a.makeup in 1..5 && !pose.blink) {
            if (a.makeup == 5) {
                val cols = intArrayOf(0xFFFF5C9A.toInt(), 0xFFFFD86B.toInt(), 0xFF5CE0B8.toInt())
                for (s in intArrayOf(-1, 1)) for ((i, cc) in cols.withIndex()) { p.color = withAlpha(cc, 150); c.drawOval(s * 6f - 4.6f + i * 0.5f * s, 18.5f - i * 0.8f, s * 6f + 4.6f, 22f - i * 0.6f, p) }
            } else {
                p.color = withAlpha(shadowCols[a.makeup], if (a.makeup == 2) 150 else 110)
                for (s in intArrayOf(-1, 1)) c.drawOval(s * 6f - 4.8f, 17.5f, s * 6f + 4.8f, 22.5f, p)
            }
        }
        // eyes
        val eyeCol = Avatar.EYES[a.eyeColour.coerceIn(0, Avatar.EYES.size - 1)]
        for (s in intArrayOf(-1, 1)) {
            val ex = s * 6.2f; val ey = 22.5f
            if (pose.blink || a.eyeShape == 2 && (t % 6f) < 0.05f) {
                p.style = Paint.Style.STROKE; p.strokeWidth = 1.1f; p.color = 0xFF2A1A20.toInt()
                c.drawArc(ex - 3.4f, ey - 2f, ex + 3.4f, ey + 2f, 20f, 140f, false, p)
                p.style = Paint.Style.FILL
                continue
            }
            val hgt = when (a.eyeShape) { 1 -> 2.6f; 2 -> 2.2f; else -> 3.2f }
            p.style = Paint.Style.FILL; p.color = Color.WHITE
            c.drawOval(ex - 3.6f, ey - hgt, ex + 3.6f, ey + hgt, p)
            p.color = eyeCol; c.drawCircle(ex + s * 0.2f, ey + 0.2f, 2.3f, p)
            p.color = 0xFF120A10.toInt(); c.drawCircle(ex + s * 0.2f, ey + 0.2f, 1.15f, p)
            p.color = Color.WHITE; c.drawCircle(ex + s * 0.2f - 0.8f, ey - 0.8f, 0.7f, p)
            if (a.eyeShape == 2) { p.color = darker(skin, 0.05f); c.drawRect(ex - 3.8f, ey - hgt - 1f, ex + 3.8f, ey - 0.6f, p) }
            // upper lid line
            p.style = Paint.Style.STROKE; p.strokeWidth = if (a.eyeShape == 3 || a.makeup == 4) 1.3f else 0.9f; p.color = 0xFF2A1A20.toInt()
            c.drawArc(ex - 3.7f, ey - hgt, ex + 3.7f, ey + hgt, 190f, 160f, false, p)
            if (a.makeup == 4) { c.drawLine(ex + s * 3.5f, ey - 0.6f, ex + s * 5.8f, ey - 2.4f, p) }
            if (a.eyeShape == 3) for (k in 0..2) { val lx = ex + s * (1.5f + k * 1.1f); c.drawLine(lx, ey - hgt + 0.3f, lx + s * 0.9f, ey - hgt - 1.4f, p) }
            p.style = Paint.Style.FILL
        }
        // glitter makeup
        if (a.makeup == 3) {
            for (i in 0 until 6) {
                val tw = ((t * 2f + i * 0.37f) % 1f)
                p.color = withAlpha(Color.WHITE, (255 * (1 - tw)).toInt())
                val gx = (if (i % 2 == 0) -1 else 1) * (8f + (i % 3) * 1.6f); val gy = 25f + (i % 3) * 1.5f
                c.drawCircle(gx, gy, 0.5f + tw * 0.4f, p)
            }
        }
        // brows
        val hair = b.hair
        p.style = Paint.Style.STROKE; p.color = darker(hair, 0.15f)
        p.strokeWidth = when (a.brows) { 3 -> 2.0f; 2 -> 1.4f; else -> 1.2f }
        for (s in intArrayOf(-1, 1)) {
            path.reset()
            when (a.brows) {
                1 -> { path.moveTo(s * 2.8f, 17.5f); path.quadTo(s * 6f, 14.2f, s * 9.5f, 17f) }
                2 -> { path.moveTo(s * 2.8f, 16.8f); path.lineTo(s * 9.2f, 16.2f) }
                else -> { path.moveTo(s * 2.8f, 17f); path.quadTo(s * 6f, 15.4f, s * 9.3f, 16.8f) }
            }
            c.drawPath(path, p)
        }
        // nose
        p.strokeWidth = 0.9f; p.color = darker(skin, 0.28f)
        path.reset(); path.moveTo(0.4f, 25f); path.quadTo(1.8f, 29f, -0.5f, 29.6f); c.drawPath(path, p)
        p.style = Paint.Style.FILL
        // blush
        if (a.blush) { p.color = 0x40FF5C7A; c.drawOval(-12.5f, 27f, -7f, 30.5f, p); c.drawOval(7f, 27f, 12.5f, 30.5f, p) }
        if (a.freckles) { p.color = withAlpha(darker(skin, 0.35f), 170); for (i in 0 until 7) { val fx = (if (i < 4) -1 else 1) * (7f + (i % 4) * 1.3f); c.drawCircle(fx, 26.5f + (i % 2) * 1.4f, 0.45f, p) } }
        // mouth
        val lip = Avatar.LIPS[a.lips.coerceIn(0, Avatar.LIPS.size - 1)]
        val sm = pose.smile
        path.reset()
        path.moveTo(-4.2f, 33f)
        path.quadTo(0f, 33f + 2.5f + sm * 2.5f, 4.2f, 33f)
        path.quadTo(0f, 32f, -4.2f, 33f)
        p.color = lip; c.drawPath(path, p)
        if (sm > 0.7f) { p.color = Color.WHITE; c.drawRect(-2.6f, 33f, 2.6f, 34.2f, p) }
        p.color = lighter(lip, 0.35f); c.drawOval(-1.4f, 34.6f, 1.4f, 35.4f, p)
        // facial hair
        if (a.facial > 0) {
            p.color = withAlpha(darker(hair, 0.05f), if (a.facial == 1) 90 else 235)
            when (a.facial) {
                1 -> { Shapes.sym(floatArrayOf(0f, 30.5f, 9f, 29f, 14f, 30f, 13f, 34f, 7f, 40f, 0f, 41.5f), path); c.drawPath(path, p) }
                2 -> { Shapes.sym(floatArrayOf(0f, 30.2f, 3f, 30f, 5.6f, 31f, 6.2f, 33f, 3f, 32.2f, 0f, 32.4f), path); c.drawPath(path, p) }
                3 -> { Shapes.sym(floatArrayOf(0f, 30.2f, 4f, 30f, 5.6f, 32f, 4.2f, 33f, 3.6f, 37f, 2f, 41.5f, 0f, 42f), path); c.drawPath(path, p)
                    p.color = lip; path.reset(); path.moveTo(-3.5f, 33.2f); path.quadTo(0f, 35.5f, 3.5f, 33.2f); path.quadTo(0f, 32.6f, -3.5f, 33.2f); c.drawPath(path, p) }
                4 -> { Shapes.sym(floatArrayOf(0f, 30f, 6f, 29.5f, 15f, 24f, 15.8f, 29f, 13f, 38f, 6f, 45f, 0f, 46f), path); c.drawPath(path, p)
                    p.color = lip; path.reset(); path.moveTo(-3.6f, 33.2f); path.quadTo(0f, 35.6f, 3.6f, 33.2f); path.quadTo(0f, 32.6f, -3.6f, 33.2f); c.drawPath(path, p) }
            }
        }
    }
}

object Hair {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()

    private fun fill(c: Canvas, col: Int, path: Path) {
        p.style = Paint.Style.FILL; p.shader = null
        p.color = col; c.drawPath(path, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.9f; p.color = darker(col, 0.3f); c.drawPath(path, p)
        p.style = Paint.Style.FILL
    }

    private fun strands(c: Canvas, col: Int, x0: Float, x1: Float, y0: Float, y1: Float, n: Int) {
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.7f; p.color = withAlpha(darker(col, 0.25f), 140)
        for (i in 0 until n) { val x = x0 + (x1 - x0) * (i + 0.5f) / n; c.drawLine(x, y0, x * 1.02f, y1, p) }
        p.style = Paint.Style.FILL
    }

    /** Hair that sits behind the head and body. */
    fun back(c: Canvas, b: Body, t: Float) {
        val col = b.hair
        val sway = kotlin.math.sin(t * 2f) * 0.8f
        when (b.a.hairStyle) {
            4 -> fill(c, col, Shapes.sym(floatArrayOf(0f, -1f, 14f, 1f, 19.5f, 14f, 20f, 38f, 18f, 46f, 18f, 46f, 0f, 46f), path))
            5 -> { fill(c, col, Shapes.sym(floatArrayOf(0f, -1f, 14f, 1f, 19.5f, 14f, 21f, 50f, 21f + sway, 96f, 21f + sway, 96f, 0f, 96f), path)); strands(c, col, -18f, 18f, 50f, 94f, 8) }
            6 -> fill(c, col, Shapes.sym(floatArrayOf(0f, -1f, 15f, 1f, 21f, 14f, 22f, 36f, 25f, 50f, 21f, 64f, 25f + sway, 80f, 22f, 96f, 26f + sway, 104f, 0f, 102f), path))
            7 -> { fill(c, col, Shapes.poly(floatArrayOf(8f, 6f, 16f, 2f, 22f + sway, 20f, 24f + sway, 50f, 19f + sway, 72f, 15f, 55f, 15f, 25f), path)) }
            10 -> for (s in intArrayOf(-1, 1)) fill(c, col, Shapes.poly(floatArrayOf(s * 15f, 16f, s * 21f, 14f, s * (26f + sway), 40f, s * (24f + sway), 62f, s * 20f, 64f, s * 19f, 40f), path))
            11 -> { p.shader = null; fill(c, col, Shapes.poly(afro(0f, 16f, 27f, 16), path)) }
            12 -> fill(c, col, Shapes.poly(afroEllipse(0f, 24f, 24f, 24f, 14), path))
            13 -> { fill(c, col, Shapes.sym(floatArrayOf(0f, -1f, 15f, 1f, 20f, 14f, 22f, 50f, 22f + sway, 110f, 22f + sway, 110f, 0f, 110f), path)); strands(c, col, -20f, 20f, 40f, 108f, 12) }
            16 -> fill(c, col, Shapes.sym(floatArrayOf(0f, 0f, 14f, 1f, 18f, 14f, 18f, 40f, 20f + sway, 62f, 20f + sway, 62f, 0f, 62f), path))
            18 -> { fill(c, col, Shapes.sym(floatArrayOf(0f, -1f, 15f, 1f, 21f, 14f, 22f, 36f, 24f, 46f, 20f, 52f, 23f + sway, 62f, 0f, 60f), path)); strands(c, col, -20f, 20f, 36f, 60f, 10) }
            21 -> { fill(c, col, Shapes.sym(floatArrayOf(0f, -2f, 15f, 0f, 22f, 14f, 24f, 50f, 24f + sway, 98f, 24f + sway, 98f, 0f, 98f), path)); strands(c, col, -22f, 22f, 30f, 96f, 9) }
            19 -> {}
            else -> {}
        }
    }

    /** Hair on top of the head. [hatOn] trims tall styles. */
    fun front(c: Canvas, b: Body, t: Float, hatOn: Boolean) {
        val col = b.hair
        val hl = withAlpha(lighter(col, 0.35f), 120)
        val cap = floatArrayOf(0f, -0.5f, 9f, 0.5f, 15f, 6f, 17f, 15f, 16.5f, 22f, 15f, 22f, 14f, 13f, 9f, 9f, 0f, 9f)
        when (b.a.hairStyle) {
            0 -> { p.color = withAlpha(col, 200); p.shader = null; c.drawPath(Shapes.sym(floatArrayOf(0f, 2.5f, 9f, 3.5f, 15f, 10f, 15.6f, 17f, 14.6f, 17f, 13f, 11f, 0f, 8f), path), p) }
            1 -> fill(c, col, Shapes.sym(floatArrayOf(0f, 0f, 9f, 1f, 15.5f, 7f, 16.8f, 16f, 15.4f, 16f, 13f, 10f, 5f, 11f, 0f, 9.5f), path))
            2 -> fill(c, col, Shapes.poly(floatArrayOf(-16.8f, 18f, -16f, 6f, -8f, 0f, 6f, -0.5f, 15f, 5f, 17f, 16f, 15.5f, 18f, 13f, 9f, 3f, 11f, -8f, 8f, -14f, 12f), path))
            3 -> fill(c, col, Shapes.poly(floatArrayOf(-16.5f, 18f, -16f, 6f, -8f, -1f, 2f, -6f, 12f, -4f, 16.5f, 6f, 16.8f, 17f, 15f, 17f, 12f, 8f, 0f, 7f, -12f, 9f), path))
            4 -> fill(c, col, Shapes.sym(floatArrayOf(0f, -0.5f, 9f, 0.5f, 15.5f, 6f, 17f, 15f, 17.5f, 28f, 15f, 30f, 14.5f, 14f, 10f, 11f, 0f, 11.5f), path))
            5, 13, 21 -> fill(c, col, Shapes.poly(floatArrayOf(0f, -0.5f, 10f, 0.5f, 16f, 7f, 18f, 18f, 18.5f, 40f, 15.5f, 40f, 14.5f, 18f, 9f, 8f, 1f, 4f, -1f, 4f, -9f, 8f, -14.5f, 18f, -15.5f, 40f, -18.5f, 40f, -18f, 18f, -16f, 7f, -10f, 0.5f), path))
            6, 18 -> fill(c, col, Shapes.poly(floatArrayOf(0f, -1f, 11f, 0f, 18f, 8f, 20f, 22f, 19f, 38f, 16f, 30f, 14.5f, 16f, 8f, 9f, 2f, 6f, -4f, 10f, -10f, 11f, -14.5f, 18f, -16f, 32f, -19f, 38f, -20f, 22f, -17f, 7f, -10f, 0f), path))
            7, 8, 9, 10, 19, 20 -> {
                fill(c, col, Shapes.sym(cap, path))
                when (b.a.hairStyle) {
                    8 -> if (!hatOn) fill(c, col, Shapes.poly(afroEllipse(0f, -4f, 10f, 8f, 10), path))
                    9 -> if (!hatOn) for (s in intArrayOf(-1, 1)) fill(c, col, Shapes.poly(afroEllipse(s * 12f, 1f, 7f, 7f, 9), path))
                    20 -> if (!hatOn) fill(c, col, Shapes.poly(afroEllipse(0f, -2f, 6f, 5f, 8), path))
                    19 -> for (s in intArrayOf(-1, 1)) {
                        for (k in 0 until 7) fill(c, col, Shapes.poly(afroEllipse(s * (15f - k * 0.3f), 30f + k * 7f, 3.8f, 4.2f, 6), path))
                        p.color = Pal2.accent; c.drawCircle(s * 13f, 80f, 2f, p)
                    }
                }
            }
            11 -> fill(c, col, Shapes.sym(floatArrayOf(0f, -7f, 12f, -5f, 19f, 3f, 20f, 15f, 16.5f, 17f, 14f, 10f, 0f, 9f), path))
            12 -> fill(c, col, Shapes.poly(afroEllipse(0f, 5f, 18.5f, 8.5f, 12), path))
            14 -> { fill(c, col, Shapes.sym(floatArrayOf(0f, 3f, 9f, 4f, 15f, 10f, 15.6f, 17f, 14.6f, 17f, 13f, 11f, 0f, 8f), path))
                if (!hatOn) fill(c, col, Shapes.sym(floatArrayOf(0f, -14f, 3f, -12f, 4.5f, 2f, 4.5f, 6f, 0f, 6f), path)) }
            15 -> fill(c, col, Shapes.poly(floatArrayOf(-16.5f, 16f, -15.5f, 5f, -6f, -1f, 7f, -0.5f, 15.5f, 6f, 16.5f, 15f, 14.5f, 14f, 10f, 13f, 2f, 12f, -6f, 9f, -13f, 11f), path))
            16 -> fill(c, col, Shapes.sym(floatArrayOf(0f, -1f, 9f, 0f, 15.5f, 6f, 17f, 16f, 15.4f, 18f, 13f, 9f, 0f, 8f), path))
            17 -> {}
        }
        if (b.a.hairStyle !in intArrayOf(0, 14, 17)) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = hl
            c.drawArc(-10f, 1f, 6f, 14f, 200f, 70f, false, p)
            p.style = Paint.Style.FILL
        }
    }

    private fun afro(cx: Float, cy: Float, r: Float, n: Int): FloatArray {
        val out = FloatArray(n * 2)
        for (i in 0 until n) {
            val a = Math.PI * 2 * i / n
            val rr = r * (if (i % 2 == 0) 1f else 0.93f)
            out[i * 2] = cx + (rr * kotlin.math.cos(a)).toFloat(); out[i * 2 + 1] = cy + (rr * kotlin.math.sin(a)).toFloat()
        }
        return out
    }

    private fun afroEllipse(cx: Float, cy: Float, rx: Float, ry: Float, n: Int): FloatArray {
        val out = FloatArray(n * 2)
        for (i in 0 until n) {
            val a = Math.PI * 2 * i / n
            val k = if (i % 2 == 0) 1f else 0.9f
            out[i * 2] = cx + (rx * k * kotlin.math.cos(a)).toFloat(); out[i * 2 + 1] = cy + (ry * k * kotlin.math.sin(a)).toFloat()
        }
        return out
    }
}

object Pal2 { const val accent = 0xFFFF5C9A.toInt() }
