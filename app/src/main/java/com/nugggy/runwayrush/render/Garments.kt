package com.nugggy.runwayrush.render

import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.nugggy.runwayrush.model.Item
import com.nugggy.runwayrush.render.AvatarRenderer.darker
import com.nugggy.runwayrush.render.AvatarRenderer.lighter
import com.nugggy.runwayrush.render.AvatarRenderer.withAlpha
import kotlin.math.cos
import kotlin.math.sin

/** Draws every garment style in figure coordinates. */
object Garments {
    val OVER_NECK = setOf("scarf", "boa", "lei", "silkscarf")

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val path2 = Path()
    private val bounds = RectF()
    private val m = Matrix()
    private val filters = HashMap<Int, PorterDuffColorFilter>()
    private val SHINY = setOf("satin", "silk", "gold", "sequin", "leather", "snakeskin", "velvet")
    private val SPARKLE = setOf("sequin", "glitter", "gold")

    // ---------------------------------------------------------------- painting

    private fun filter(col: Int) = filters.getOrPut(col) { PorterDuffColorFilter(col, PorterDuff.Mode.MULTIPLY) }

    /** Fills [shape] with the item's colour and fabric, then shades and outlines it. */
    fun fill(c: Canvas, it: Item, shape: Path, t: Float, col: Int = it.colour.rgb, fabric: String = it.fabric, outline: Boolean = true) {
        p.style = Paint.Style.FILL; p.alpha = 255
        val a = AvatarRenderer.assets
        var sh: BitmapShader? = null
        var natural = false
        if (fabric != "solid") {
            if (a.keepsColour(fabric)) { sh = a.naturalShader(fabric); natural = sh != null }
            if (sh == null) sh = a.fabricShader(fabric)
        }
        if (sh != null) {
            val tex = fabric in setOf("stripes", "dots", "check", "hearts", "stars", "chevron")
            m.setScale(if (tex) 0.32f else 0.17f, if (tex) 0.32f else 0.17f)
            sh.setLocalMatrix(m)
            p.shader = sh
            p.colorFilter = if (natural) null else filter(col)
            if (fabric == "sheer") p.alpha = 215
        } else { p.shader = null; p.colorFilter = null; p.color = col }
        c.drawPath(shape, p)
        p.colorFilter = null; p.shader = null; p.alpha = 255

        shape.computeBounds(bounds, true)
        // soft cylindrical shading
        sp.style = Paint.Style.FILL
        sp.color = -16777216; sp.shader = LinearGradient(bounds.left, 0f, bounds.right, 0f, intArrayOf(0x38000000, 0x00000000, 0x08FFFFFF, 0x40000000), floatArrayOf(0f, 0.35f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(shape, sp)
        if (fabric in SHINY) {
            sp.color = -16777216; sp.shader = LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom, intArrayOf(0x00FFFFFF, 0x50FFFFFF, 0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.3f, 0.45f, 0.7f, 0.85f), Shader.TileMode.CLAMP)
            c.drawPath(shape, sp)
        }
        sp.shader = null
        if (fabric in SPARKLE || "sparkly" in it.style.tags) sparkle(c, shape, t, it.id)
        if (outline) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 0.9f
            p.color = if (natural) 0xFF3A2A20.toInt() else darker(col, 0.38f)
            c.drawPath(shape, p)
            p.style = Paint.Style.FILL
        }
    }

    private fun sparkle(c: Canvas, shape: Path, t: Float, seed: Int) {
        c.save(); c.clipPath(shape)
        val w = bounds.width(); val h = bounds.height()
        val n = ((w * h) / 90f).toInt().coerceIn(3, 26)
        for (i in 0 until n) {
            val hx = ((seed * 31 + i * 97) % 101) / 101f; val hy = ((seed * 17 + i * 53) % 103) / 103f
            val ph = ((t * 1.4f + i * 0.173f + seed * 0.01f) % 1f)
            val al = (sin(ph * Math.PI).toFloat() * 255).toInt()
            val x = bounds.left + hx * w; val y = bounds.top + hy * h
            val r = 0.6f + ph * 1.2f
            sp.color = withAlpha(Color.WHITE, al)
            c.drawCircle(x, y, 0.45f, sp)
            sp.strokeWidth = 0.35f; sp.style = Paint.Style.STROKE
            c.drawLine(x - r, y, x + r, y, sp); c.drawLine(x, y - r, x, y + r, sp)
            sp.style = Paint.Style.FILL
        }
        c.restore()
    }

    private fun solid(c: Canvas, shape: Path, col: Int, outline: Boolean = true) {
        p.style = Paint.Style.FILL; p.shader = null; p.colorFilter = null; p.color = col
        c.drawPath(shape, p)
        if (outline) { p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; p.color = darker(col, 0.35f); c.drawPath(shape, p); p.style = Paint.Style.FILL }
    }

    private fun line(c: Canvas, col: Int, w: Float, vararg pts: Float) {
        p.style = Paint.Style.STROKE; p.shader = null; p.color = col; p.strokeWidth = w
        var i = 0
        while (i + 3 < pts.size) { c.drawLine(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], p); i += 2 }
        p.style = Paint.Style.FILL
    }

    private fun dot(c: Canvas, col: Int, x: Float, y: Float, r: Float) { p.style = Paint.Style.FILL; p.shader = null; p.color = col; c.drawCircle(x, y, r, p) }

    private fun shine(c: Canvas, x: Float, y: Float, r: Float, col: Int) {
        sp.color = -16777216; sp.shader = RadialGradient(x - r * 0.3f, y - r * 0.3f, r, intArrayOf(lighter(col, 0.6f), col, darker(col, 0.3f)), null, Shader.TileMode.CLAMP)
        c.drawCircle(x, y, r, sp); sp.shader = null
    }

    // ---------------------------------------------------------------- body outline helpers

    /** Half width of the body at height [y]. */
    private fun wAt(b: Body, y: Float): Float = when {
        y < 60f -> b.sh
        y < 72f -> b.sh - (y - 60f) / 12f * 2f
        y < b.waistY -> lerp(b.sh - 2f, b.wa, (y - 72f) / (b.waistY - 72f)) + b.bust * (1f - kotlin.math.abs(y - 78f) / 12f).coerceAtLeast(0f) * 0.5f
        y < b.hipY -> lerp(b.wa, b.hp, (y - b.waistY) / (b.hipY - b.waistY))
        else -> b.hp
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

    /**
     * Right-half outline for upper-body garments.
     * neck: crew, v, scoop, turtle, boat, none. arm: set, tank, cami, halter, tube, off.
     */
    private fun upper(b: Body, neck: String, arm: String, hem: Float, flare: Float = 0f, ease: Float = 1.6f, waistIn: Boolean = true): FloatArray {
        val pts = ArrayList<Float>()
        fun add(x: Float, y: Float) { pts.add(x); pts.add(y) }
        when (arm) {
            "set" -> {
                when (neck) {
                    "v" -> { add(0f, 62f); add(6.5f, 45f) }
                    "scoop" -> { add(0f, 57f); add(9f, 47f) }
                    "turtle" -> { add(0f, 36f); add(6.8f, 36f); add(6.8f, 36f); add(7.4f, 44f) }
                    "boat" -> { add(0f, 49f); add(12f, 47.5f) }
                    else -> { add(0f, 49.5f); add(6.8f, 45.5f) }
                }
                add(b.sh * 0.7f, 49.5f); add(b.sh + ease * 0.8f, 52.5f); add(b.sh + ease, 60f)
            }
            "tank" -> { add(0f, if (neck == "v") 62f else 56f); add(7f, 48f); add(b.sh * 0.62f, 47.5f); add(b.sh * 0.62f, 47.5f); add(b.sh - 4f, 62f) }
            "cami" -> { add(0f, 62f); add(5f, 59f); add(b.sh * 0.55f, 47f); add(b.sh * 0.55f, 47f); add(b.sh * 0.6f, 47.5f); add(b.sh - 3f, 64f) }
            "halter" -> { add(0f, 60f); add(5f, 43f); add(7f, 43f); add(7f, 43f); add(b.sh - 6f, 64f) }
            "tube" -> { add(0f, 64f); add(wAt(b, 64f) + ease, 63f); add(wAt(b, 64f) + ease, 63f) }
            "off" -> { add(0f, 57f); add(b.sh + 3f, 55.5f); add(b.sh + 3f, 55.5f); add(b.sh + 3f, 61f) }
        }
        add(wAt(b, 70f) + ease, 70f)
        val ys = floatArrayOf(82f, if (waistIn) b.waistY else 97f, 105f, b.hipY)
        for (y in ys) if (y < hem - 2f) add(wAt(b, y) + ease + if (y > b.waistY) flare * ((y - b.waistY) / (hem - b.waistY)).coerceIn(0f, 1f) else 0f, y)
        val hw = (if (hem > b.hipY) b.hp else wAt(b, hem)) + ease + flare
        add(hw, hem); add(hw, hem); add(0f, hem)
        return pts.toFloatArray()
    }

    /** Skirt from the waist to [hem]. */
    private fun skirt(b: Body, top: Float, hem: Float, flare: Float, ease: Float = 1.8f): FloatArray =
        floatArrayOf(0f, top, wAt(b, top) + ease, top, wAt(b, top) + ease, top, b.hp + ease + flare * 0.25f, b.hipY,
            b.hp + ease + flare, hem, b.hp + ease + flare, hem, 0f, hem + 1f)

    /** A leg tube in leg-local coordinates. */
    private fun legTube(b: Body, len: Float, bottomW: Float, ease: Float = 1.4f, kneeW: Float = -1f): FloatArray {
        val kw = if (kneeW < 0) (b.legW * 0.78f + ease) else kneeW
        return floatArrayOf(0f, -6f, b.legW + ease, -6f, b.legW + ease, -2f, (b.legW + ease + kw) / 2f, minOf(len, 22f),
            if (len > 40f) kw else (b.legW + ease) * 0.95f, minOf(len, 44f), bottomW, len, bottomW, len, 0f, len)
    }

    private fun sleeveShape(b: Body, len: Float, endW: Float, puff: Float = 0f, start: Float = -5f): FloatArray {
        val w = b.armW + 1.8f
        return if (puff > 0f) floatArrayOf(0f, start, w + puff * 0.6f, start + 1f, w + puff, len * 0.45f, endW, len, endW, len, 0f, len)
        else floatArrayOf(0f, start, w * 0.9f, start, w, start + 4f, (w + endW) / 2f, len * 0.5f, endW, len, endW, len, 0f, len)
    }

    // ---------------------------------------------------------------- base layer

    private val baseCol = 0xFFE8E4EC.toInt()
    fun baseTop(c: Canvas, b: Body) { solid(c, Shapes.sym(upper(b, "scoop", "tank", 100f, ease = 0.6f), path), baseCol) }
    fun baseSleeve(c: Canvas, b: Body) {}
    fun baseShortsTorso(c: Canvas, b: Body) {
        solid(c, Shapes.sym(floatArrayOf(0f, 99f, wAt(b, 99f) + 0.8f, 99f, b.hp + 0.8f, b.hipY, b.hp + 0.8f, 126f, 0f, 128f), path), 0xFF6E6A80.toInt())
    }
    fun baseShorts(c: Canvas, b: Body, side: Int) {
        solid(c, Shapes.sym(legTube(b, 14f, b.legW + 1.2f, 0.8f), path), 0xFF6E6A80.toInt())
    }

    // ---------------------------------------------------------------- tops

    private fun topHem(k: String): Float = when (k) {
        "crop", "sportstop" -> 82f; "tube", "halter" -> 92f; "blouse", "offshoulder" -> 102f; "corset", "peasant" -> 108f
        "tank", "hawaii", "polo" -> 117f; "turtle", "tuxshirt" -> 113f; "bikini" -> 80f; "armourtop" -> 110f; else -> 121f
    }

    fun topTorso(c: Canvas, b: Body, it: Item, t: Float, tucked: Boolean) {
        val k = it.style.key
        var hem = topHem(k)
        if (tucked && hem > 100f) hem = 98f
        val acc = it.accent.rgb
        when (k) {
            "bikini" -> {
                for (s in intArrayOf(-1, 1)) fill(c, it, Shapes.poly(floatArrayOf(s * 1.5f, 77f, s * 4f, 64f, s * (b.wa + 1f), 76f + b.bust, s * (b.wa - 2f), 80f + b.bust), path), t)
                line(c, darker(it.colour.rgb, 0.3f), 0.8f, -4f, 64f, -6f, 46f); line(c, darker(it.colour.rgb, 0.3f), 0.8f, 4f, 64f, 6f, 46f)
                return
            }
            "corset" -> {
                fill(c, it, Shapes.sym(upper(b, "none", "tube", hem, ease = 1f), path), t)
                for (i in 0 until 6) { val y = 68f + i * 6.5f; line(c, acc, 0.7f, -2.5f, y, 2.5f, y + 3f, -2.5f, y + 3f, 2.5f, y) }
                line(c, darker(it.colour.rgb, 0.3f), 0.6f, -6f, 64f, -5f, hem, 6f, 64f, 5f, hem)
                return
            }
            "armourtop" -> {
                fill(c, it, Shapes.sym(upper(b, "crew", "set", hem, ease = 2.5f, flare = 2f), path), t)
                line(c, lighter(it.colour.rgb, 0.4f), 1.2f, 0f, 52f, 0f, hem - 2f)
                for (y in floatArrayOf(92f, 100f)) line(c, darker(it.colour.rgb, 0.4f), 0.8f, -b.wa, y, b.wa, y)
                return
            }
        }
        val (neck, arm) = when (k) {
            "tank", "sportstop" -> "scoop" to "tank"; "crop" -> "crew" to "set"; "blouse" -> "boat" to "set"; "shirt", "hawaii", "polo", "tuxshirt" -> "v" to "set"
            "turtle" -> "turtle" to "set"; "offshoulder" -> "none" to "off"; "tube" -> "none" to "tube"; "halter" -> "none" to "halter"
            "peasant" -> "scoop" to "set"; "sweater" -> "crew" to "set"; else -> "crew" to "set"
        }
        val ease = if (k in setOf("sweater", "hoodie", "jersey", "hawaii")) 3f else 1.6f
        val flare = if (k == "peasant" || k == "hawaii") 3f else if (k == "sweater" || k == "hoodie") 2f else 0f
        if (k == "hoodie") {
            // hood behind the neck
            fill(c, it, Shapes.sym(floatArrayOf(0f, 40f, 12f, 40f, 15f, 47f, 12f, 53f, 0f, 55f), path2), t)
        }
        fill(c, it, Shapes.sym(upper(b, neck, arm, hem, flare, ease), path), t)
        when (k) {
            "shirt", "tuxshirt" -> { collar(c, b, if (k == "tuxshirt") Color.WHITE else it.colour.rgb, it, t); for (i in 0 until 5) dot(c, darker(it.colour.rgb, 0.3f), 0f, 66f + i * 10f, 0.9f); line(c, darker(it.colour.rgb, 0.3f), 0.5f, 0f, 62f, 0f, hem) }
            "polo" -> { collar(c, b, it.colour.rgb, it, t); dot(c, acc, 0f, 54f, 0.9f); dot(c, acc, 0f, 59f, 0.9f) }
            "hawaii" -> collar(c, b, it.colour.rgb, it, t)
            "hoodie" -> {
                line(c, acc, 0.8f, -3f, 50f, -3.5f, 66f, 3f, 50f, 3.5f, 66f)
                fill(c, it, Shapes.sym(floatArrayOf(0f, 98f, 11f, 98f, 13f, 112f, 13f, 112f, 0f, 112f), path2), t)
            }
            "sweater" -> { for (y in floatArrayOf(hem - 4f)) line(c, darker(it.colour.rgb, 0.25f), 0.6f, -b.hp - 4f, y, b.hp + 4f, y); line(c, darker(it.colour.rgb, 0.25f), 1.2f, -7f, 47f, 0f, 50f, 7f, 47f) }
            "jersey" -> { Garments.number(c, it, t); line(c, acc, 2f, -6.8f, 46.5f, 0f, 50.5f, 6.8f, 46.5f) }
            "sportstop" -> line(c, acc, 1.6f, -b.wa - 1f, 80f, b.wa + 1f, 80f)
            "tuxshirt" -> {}
            "turtle" -> for (y in floatArrayOf(38.5f, 41f)) line(c, darker(it.colour.rgb, 0.2f), 0.5f, -6.5f, y, 6.5f, y)
            "crop", "tee" -> if (it.fabric == "solid" && it.id % 3 == 0) Ui2.motif(c, 0f, 75f, 7f, acc, it.id)
        }
        if (k == "tuxshirt") bow(c, 0f, 47.5f, 6f, 0xFF1E1C22.toInt())
    }

    private fun collar(c: Canvas, b: Body, col: Int, it: Item, t: Float) {
        for (s in intArrayOf(-1, 1)) {
            Shapes.lines(floatArrayOf(0f, 51f, s * 7.5f, 44f, s * 10f, 47f, s * 5f, 56f), path2)
            if (col == Color.WHITE) solid(c, path2, col) else fill(c, it, path2, t)
        }
    }

    private fun bow(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        Shapes.lines(floatArrayOf(x, y, x - s, y - s * 0.55f, x - s, y + s * 0.55f), path2); solid(c, path2, col)
        Shapes.lines(floatArrayOf(x, y, x + s, y - s * 0.55f, x + s, y + s * 0.55f), path2); solid(c, path2, col)
        dot(c, darker(col, 0.2f), x, y, s * 0.3f)
    }

    private fun number(c: Canvas, it: Item, t: Float) {
        val n = (it.id % 99).toString()
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; color = it.accent.rgb }
        c.drawText(n, 0f, 90f, tp)
    }

    fun sleeve(c: Canvas, b: Body, it: Item, t: Float) {
        val k = it.style.key
        val w = b.armW
        val shape: FloatArray? = when (k) {
            "tee", "polo", "crop" -> sleeveShape(b, 17f, w + 1.6f)
            "hawaii", "jersey" -> sleeveShape(b, 21f, w + 3f)
            "blouse" -> sleeveShape(b, 22f, w + 1.2f, puff = 4f)
            "shirt", "tuxshirt", "turtle" -> sleeveShape(b, 62f, w * 0.75f + 1.4f)
            "hoodie", "sweater" -> sleeveShape(b, 63f, w * 0.75f + 2.4f).also { }
            "offshoulder" -> sleeveShape(b, 22f, w + 2.5f, puff = 2f, start = 3f)
            "peasant" -> sleeveShape(b, 44f, w + 5f, puff = 2f)
            "armourtop" -> floatArrayOf(0f, -8f, w + 5f, -6f, w + 5f, 8f, w + 3f, 14f, 0f, 15f)
            // dresses
            "aline", "minidress", "sailor", "shirtdress", "safari" -> sleeveShape(b, 14f, w + 1.8f)
            "wrap" -> sleeveShape(b, 40f, w * 0.85f + 2f)
            "kaftan" -> sleeveShape(b, 50f, w + 9f)
            "princess" -> sleeveShape(b, 20f, w + 1f, puff = 5f)
            "gown", "bridal" -> if (it.id % 2 == 0) null else sleeveShape(b, 60f, w * 0.75f + 1f)
            "suit", "tux", "tracksuit", "onesie", "superhero", "wetsuit", "skeleton" -> sleeveShape(b, 63f, w * 0.75f + 1.6f)
            "spacesuit" -> sleeveShape(b, 64f, w + 3f, puff = 3f)
            "wizard" -> sleeveShape(b, 58f, w + 10f)
            "jumpsuit" -> if (it.id % 2 == 0) sleeveShape(b, 16f, w + 1.6f) else null
            // outerwear
            "denimjkt", "biker", "blazer", "trench", "cardigan", "bomber", "varsity", "raincoat", "labcoat", "military" -> sleeveShape(b, 64f, w * 0.75f + 2.6f)
            "puffer" -> sleeveShape(b, 64f, w + 2.5f, puff = 3.5f)
            "furcoat" -> sleeveShape(b, 62f, w + 3.5f, puff = 2f)
            "kimono" -> sleeveShape(b, 48f, w + 10f)
            "cropjkt" -> sleeveShape(b, 60f, w * 0.75f + 2.6f)
            else -> null
        }
        if (shape == null) return
        val col = if (k == "varsity") it.accent.rgb else it.colour.rgb
        if (k == "varsity") fill(c, it, Shapes.sym(shape, path), t, col = col, fabric = "solid") else fill(c, it, Shapes.sym(shape, path), t)
        when (k) {
            "tracksuit" -> line(c, it.accent.rgb, 1.3f, b.armW + 1.2f, 0f, b.armW * 0.75f + 1.2f, 62f)
            "skeleton" -> line(c, Color.WHITE, 1.6f, 0f, 2f, 0f, 30f, 0f, 34f, 0f, 60f)
            "superhero" -> line(c, it.accent.rgb, 2.2f, -w * 0.75f - 1.5f, 55f, w * 0.75f + 1.5f, 55f)
            "furcoat" -> fluff(c, 0f, 62f, w + 4f, lighter(it.colour.rgb, 0.2f))
            "shirt", "tuxshirt", "blazer", "suit", "tux" -> line(c, darker(it.colour.rgb, 0.3f), 0.6f, -w * 0.75f - 1.2f, 57f, w * 0.75f + 1.2f, 57f)
        }
    }

    private fun fluff(c: Canvas, x: Float, y: Float, w: Float, col: Int) {
        for (i in -3..3) dot(c, col, x + i * w / 3.4f, y + (i % 2) * 1.2f, 2.4f)
    }

    // ---------------------------------------------------------------- bottoms

    fun bottomTorso(c: Canvas, b: Body, it: Item, t: Float) {
        val k = it.style.key
        val top = b.waistY - 3f
        when (k) {
            "mini" -> fill(c, it, Shapes.sym(skirt(b, top, 134f, 5f), path), t)
            "pleated", "kilt" -> {
                val hem = if (k == "kilt") 152f else 142f
                fill(c, it, Shapes.sym(skirt(b, top, hem, 9f), path), t)
                for (i in -3..3) line(c, darker(it.colour.rgb, 0.3f), 0.5f, i * 4f, b.hipY, i * 5.4f, hem)
                if (k == "kilt") { dot(c, 0xFFE3B341.toInt(), b.hp - 2f, 125f, 1.4f); line(c, 0xFF2A1A10.toInt(), 1.5f, -b.wa - 2f, top + 2f, b.wa + 2f, top + 2f) }
            }
            "pencil" -> { fill(c, it, Shapes.sym(skirt(b, top, 162f, -3f), path), t); line(c, darker(it.colour.rgb, 0.3f), 0.6f, 4f, 150f, 4f, 162f) }
            "maxi" -> fill(c, it, Shapes.sym(skirt(b, top, 196f, 15f), path), t)
            "tutu" -> {
                for (layer in 0..2) {
                    val pts = ArrayList<Float>()
                    val hem = 128f + layer * 3f; val wd = b.hp + 18f - layer * 3f
                    pts.addAll(listOf(0f, top + 2f, wAt(b, top) + 2f, top + 2f))
                    for (i in 0..6) { val ang = i / 6f; pts.add(wd * (1f - ang * 0.0f) * (if (i % 2 == 0) 1f else 0.92f)); pts.add(b.hipY + (hem - b.hipY) * (0.3f + 0.7f * (i / 6f))) }
                    pts.addAll(listOf(0f, hem + 2f))
                    p.alpha = 255
                    fill(c, it, Shapes.sym(pts.toFloatArray(), path), t, col = if (layer == 1) lighter(it.colour.rgb, 0.2f) else it.colour.rgb, fabric = if (layer == 0) it.fabric else "solid")
                }
            }
            "sarong" -> { fill(c, it, Shapes.poly(floatArrayOf(-b.wa - 2f, top, b.wa + 2f, top, b.hp + 4f, b.hipY, b.hp + 6f, 180f, -2f, 168f, -b.hp - 4f, 150f, -b.hp - 3f, b.hipY), path), t); dot(c, it.accent.rgb, b.wa - 2f, top + 3f, 2f) }
            "mermaidtail" -> {
                fill(c, it, Shapes.sym(floatArrayOf(0f, top, wAt(b, top) + 2f, top, b.hp + 3f, b.hipY, b.hp + 1f, 150f, 7f, 186f, 7f, 186f, 18f, 206f, 16f, 212f, 0f, 202f), path), t)
                for (r in 0 until 6) for (i in -2..2) { p.style = Paint.Style.STROKE; p.strokeWidth = 0.5f; p.color = lighter(it.colour.rgb, 0.3f); c.drawArc(i * 6f - 3f, 120f + r * 10f, i * 6f + 3f, 126f + r * 10f, 0f, 180f, false, p) }
                p.style = Paint.Style.FILL
            }
            "bikinibottom" -> fill(c, it, Shapes.sym(floatArrayOf(0f, 106f, wAt(b, 106f) + 0.8f, 103f, wAt(b, 106f) + 0.8f, 103f, b.hp * 0.45f, 123f, 0f, 125f), path), t)
            else -> {
                // trousers and shorts: seat section
                val waist = if (k in setOf("leggings", "bikebottom", "joggers")) top + 1f else top
                fill(c, it, Shapes.sym(floatArrayOf(0f, waist, wAt(b, waist) + 1.6f, waist, wAt(b, waist) + 1.6f, waist, b.hp + 1.8f, b.hipY, b.hp + 1.8f, b.crotchY + 2f, 0f, b.crotchY + 4f), path), t)
                if (k in setOf("jeans", "skinny", "flares", "shorts", "cargo")) {
                    line(c, 0xFFE3B341.toInt(), 0.5f, -b.wa, waist + 2.5f, b.wa, waist + 2.5f)
                    line(c, darker(it.colour.rgb, 0.35f), 0.6f, 0f, waist, 0f, waist + 18f)
                    dot(c, 0xFFE3B341.toInt(), 0f, waist + 2.5f, 1f)
                }
                if (k in setOf("wide", "culottes")) line(c, darker(it.colour.rgb, 0.35f), 1.5f, -b.wa - 1f, waist + 1.5f, b.wa + 1f, waist + 1.5f)
            }
        }
    }

    /** Leg sections of bottoms and one-pieces, drawn in leg-local coordinates. */
    fun legs(c: Canvas, b: Body, bottom: Item?, dress: Item?, side: Int, t: Float, mq: Boolean) {
        bottom?.let { legPart(c, b, it, it.style.key, side, t) }
        dress?.let {
            val k = it.style.key
            val legKey = when (k) {
                "jumpsuit" -> "wide"; "overalls" -> "jeans"; "suit", "tux" -> "wide"; "tracksuit" -> "joggers"; "onesie" -> "onesie"
                "spacesuit" -> "space"; "superhero", "wetsuit" -> "leggings"; "skeleton" -> "skeleton"; "safari" -> "shorts"
                else -> null
            }
            if (legKey != null) legPart(c, b, it, legKey, side, t)
        }
    }

    private fun legPart(c: Canvas, b: Body, it: Item, key: String, side: Int, t: Float) {
        val L = b.legLen
        val lw = b.legW
        val shape = when (key) {
            "jeans", "cargo" -> legTube(b, L - 2f, lw * 0.72f + 1.8f)
            "skinny" -> legTube(b, L - 1f, lw * 0.56f + 0.9f, 0.9f)
            "wide" -> legTube(b, L + 4f, lw + 3.5f, 1.8f, lw + 2.5f)
            "flares" -> legTube(b, L + 4f, lw + 5f, 1.4f, lw * 0.7f + 1f)
            "shorts" -> legTube(b, 16f, lw + 2f)
            "culottes" -> legTube(b, 48f, lw + 5f, 2f, lw + 3f)
            "leggings", "skeleton" -> legTube(b, L - 2f, lw * 0.56f + 0.5f, 0.5f)
            "joggers" -> floatArrayOf(0f, -6f, lw + 2f, -6f, lw + 2f, 30f, lw * 0.85f + 2f, 60f, lw * 0.6f + 1.4f, L - 4f, lw * 0.6f + 1.4f, L - 4f, 0f, L - 4f)
            "bikebottom" -> legTube(b, 34f, lw * 0.82f + 0.6f, 0.6f)
            "onesie" -> legTube(b, L + 2f, lw * 0.7f + 2.5f, 2.2f)
            "space" -> floatArrayOf(0f, -6f, lw + 4f, -6f, lw + 4.5f, 20f, lw + 3f, 40f, lw + 4f, 60f, lw * 0.8f + 4f, L + 2f, lw * 0.8f + 4f, L + 2f, 0f, L + 2f)
            else -> null
        } ?: return
        fill(c, it, Shapes.sym(shape, path), t)
        when (key) {
            "cargo" -> { fill(c, it, Shapes.sym(floatArrayOf(0f, 26f, 5f, 26f, 5f, 26f, 5f, 38f, 5f, 38f, 0f, 38f), path2, side * -1.5f), t) }
            "joggers" -> line(c, it.accent.rgb, 1.2f, side * (lw + 1.5f), -4f, side * (lw * 0.6f + 1.2f), L - 6f)
            "skeleton" -> line(c, Color.WHITE, 2f, 0f, 2f, 0f, 36f, 0f, 42f, 0f, L - 4f)
            "space" -> for (y in floatArrayOf(18f, 42f)) line(c, darker(it.colour.rgb, 0.3f), 1f, -lw - 4f, y, lw + 4f, y)
            "jeans", "skinny", "flares" -> if (it.fabric == "denim" && it.id % 4 == 0) { line(c, 0xCCFFFFFF.toInt(), 0.7f, -3f, 36f, 3f, 36f, -2f, 38f, 3.5f, 38f) }
        }
        if (key == "flares" || key == "wide") line(c, darker(it.colour.rgb, 0.3f), 0.5f, 0f, 10f, 0f, L)
    }

    // ---------------------------------------------------------------- dresses

    fun dressTorso(c: Canvas, b: Body, it: Item, t: Float) {
        val k = it.style.key
        val col = it.colour.rgb; val acc = it.accent.rgb
        fun body(pts: FloatArray) = fill(c, it, Shapes.sym(pts, path), t)
        fun dress(neck: String, arm: String, hem: Float, flare: Float, ease: Float = 1.6f) {
            val up = upper(b, neck, arm, b.waistY + 0.5f, 0f, ease)
            // drop the hem corner and the centre point, continue as a skirt
            val trimmed = up.copyOf(up.size - 6)
            val sk = floatArrayOf(b.hp + ease + flare * 0.3f, b.hipY, b.hp + ease + flare, hem, b.hp + ease + flare, hem, 0f, hem + 1f)
            body(trimmed + sk)
        }
        when (k) {
            "slip" -> dress("v", "cami", 152f, 7f, 1.2f)
            "aline" -> dress("crew", "set", 140f, 13f)
            "gown" -> { dress(if (it.id % 2 == 0) "none" else "scoop", if (it.id % 2 == 0) "tube" else "set", 202f, 34f); folds(c, b, 112f, 200f, 5, col) }
            "princess" -> { dress("scoop", "set", 202f, 32f); folds(c, b, 112f, 200f, 5, col); bow(c, 0f, b.waistY, 6f, acc) }
            "mermaid" -> body(upper(b, "none", "tube", 158f, -2f, 1f).let { u -> u.copyOf(u.size - 6) + floatArrayOf(b.hp - 2f, 150f, b.hp + 16f, 202f, b.hp + 16f, 202f, 0f, 203f) })
            "minidress" -> dress("crew", "set", 128f, 6f)
            "wrap" -> { dress("v", "set", 150f, 8f); line(c, darker(col, 0.35f), 0.8f, -b.wa, 62f, b.wa, 125f); fill(c, it, Shapes.poly(floatArrayOf(-b.wa - 1f, 93f, b.wa + 1f, 93f, b.wa + 1f, 97f, -b.wa - 1f, 97f), path2), t) }
            "sundress" -> dress("scoop", "tank", 142f, 15f)
            "shirtdress" -> { dress("v", "set", 145f, 7f); collar(c, b, col, it, t); for (i in 0 until 6) dot(c, darker(col, 0.35f), 0f, 66f + i * 9f, 0.9f); line(c, acc, 2f, -b.wa - 1.5f, b.waistY, b.wa + 1.5f, b.waistY) }
            "jumpsuit" -> { body(upper(b, "v", if (it.id % 2 == 0) "set" else "tank", b.crotchY + 3f, 0f, 1.8f)); line(c, acc, 1.8f, -b.wa - 1.5f, b.waistY, b.wa + 1.5f, b.waistY) }
            "overalls" -> {
                solid(c, Shapes.sym(upper(b, "crew", "set", 100f, 0f, 1.4f), path), acc)
                body(floatArrayOf(0f, 66f, 8f, 66f, 8f, 66f, 9f, 85f, wAt(b, 92f) + 1.8f, 92f, b.hp + 1.8f, b.hipY, b.hp + 1.8f, b.crotchY + 2f, 0f, b.crotchY + 4f))
                for (s in intArrayOf(-1, 1)) { line(c, col, 2.5f, s * 7f, 66f, s * 10f, 49f); dot(c, 0xFFE3B341.toInt(), s * 7f, 67.5f, 1.3f) }
                fill(c, it, Shapes.sym(floatArrayOf(0f, 72f, 5f, 72f, 5f, 72f, 5f, 82f, 5f, 82f, 0f, 82f), path2), t)
            }
            "bridal" -> {
                dress("none", "tube", 204f, 40f, 1.2f)
                folds(c, b, 112f, 200f, 7, col)
                p.color = 0x55FFFFFF; c.drawRect(-b.wa - 1f, b.waistY - 1.5f, b.wa + 1f, b.waistY + 1.5f, p)
            }
            "flapper" -> {
                body(upper(b, "v", "cami", 136f, 0f, 2f))
                for (r in 0 until 4) { val y = 104f + r * 9f; for (i in -9..9) line(c, if (r % 2 == 0) lighter(col, 0.3f) else darker(col, 0.2f), 0.6f, i * 2.2f, y, i * 2.2f + 0.3f, y + 9f) }
                line(c, acc, 1.6f, -b.hp, 103f, b.hp, 103f)
            }
            "kaftan" -> { dress("v", "set", 196f, 12f, 4f); line(c, acc, 1.2f, -6.5f, 46f, 0f, 62f, 6.5f, 46f) }
            "bodycon" -> dress(if (it.id % 2 == 0) "scoop" else "none", if (it.id % 2 == 0) "set" else "tube", 132f, -2f, 0.8f)
            "prom" -> {
                body(upper(b, "none", if (it.id % 2 == 0) "halter" else "tube", b.waistY + 1f, 0f, 1.2f))
                for (layer in 0..2) {
                    val hem = 148f + layer * 3f
                    fill(c, it, Shapes.sym(skirt(b, b.waistY - 1f, hem, 22f - layer * 4f, 1.2f), path), t, col = if (layer == 1) lighter(col, 0.18f) else col, fabric = if (layer == 2) it.fabric else "solid")
                }
            }
            "sailor" -> {
                dress("v", "set", 138f, 10f)
                Shapes.lines(floatArrayOf(-12f, 47f, 12f, 47f, 13f, 60f, 0f, 66f, -13f, 60f), path2); solid(c, path2, acc)
                line(c, Color.WHITE, 0.7f, -11f, 58f, 0f, 63f, 11f, 58f)
                bow(c, 0f, 63f, 4f, 0xFFE0263A.toInt())
            }
            "suit", "tux" -> {
                solid(c, Shapes.sym(upper(b, "v", "set", 100f, 0f, 1f), path), if (k == "tux") Color.WHITE else lighter(acc, 0.5f))
                c.save()
                Shapes.poly(floatArrayOf(-0.5f, 48f, 0.5f, 48f, 6f, 92f, -6f, 92f), path2)
                c.clipOutPath(path2)
                body(upper(b, "v", "set", 126f, 2f, 2.4f))
                c.restore()
                lapels(c, b, it, t, if (k == "tux") darker(col, 0.25f) else col, 92f)
                for (y in floatArrayOf(96f, 106f)) dot(c, darker(col, 0.4f), 3.5f, y, 1f)
                if (k == "tux") bow(c, 0f, 50f, 5f, 0xFF1E1C22.toInt()) else { Shapes.poly(floatArrayOf(-1.5f, 49f, 1.5f, 49f, 2.5f, 80f, 0f, 84f, -2.5f, 80f), path2); solid(c, path2, acc) }
            }
            "tracksuit" -> {
                body(upper(b, "turtle", "set", 120f, 1f, 2.6f))
                line(c, darker(col, 0.4f), 0.8f, 0f, 37f, 0f, 120f)
                line(c, acc, 1.4f, -b.sh - 1f, 56f, -b.hp - 2f, 118f); line(c, acc, 1.4f, b.sh + 1f, 56f, b.hp + 2f, 118f)
            }
            "onesie" -> {
                body(upper(b, "crew", "set", b.crotchY + 3f, 0f, 2.4f))
                for (i in 0 until 4) dot(c, acc, 0f, 58f + i * 12f, 1.3f)
                fill(c, it, Shapes.sym(floatArrayOf(0f, 38f, 14f, 39f, 17f, 47f, 12f, 53f, 0f, 53f), path2), t)
            }
            "spacesuit" -> {
                body(upper(b, "turtle", "set", b.crotchY + 4f, 0f, 4f))
                Shapes.poly(floatArrayOf(-9f, 66f, 9f, 66f, 9f, 84f, -9f, 84f), path2); solid(c, path2, 0xFF2A2F3A.toInt())
                dot(c, 0xFFE0263A.toInt(), -5f, 71f, 1.6f); dot(c, 0xFF39FF6E.toInt(), 0f, 71f, 1.6f); dot(c, 0xFF2EE6FF.toInt(), 5f, 71f, 1.6f)
                line(c, 0xFF39FF6E.toInt(), 1f, -6f, 78f, 6f, 78f)
                line(c, darker(col, 0.35f), 1f, -b.wa - 3f, b.waistY, b.wa + 3f, b.waistY)
            }
            "wizard" -> {
                dress("v", "set", 198f, 20f, 3.5f)
                line(c, acc, 1.6f, -6.5f, 46f, 0f, 62f, 6.5f, 46f)
                for (i in 0 until 5) com.nugggy.runwayrush.core.Ui.star(c, -b.hp + (i * 23 % 40), 120f + (i * 17 % 70), 2.5f, 0xFFFFD86B.toInt())
                line(c, acc, 1.6f, -b.wa - 2f, b.waistY, b.wa + 2f, b.waistY)
            }
            "superhero" -> {
                body(upper(b, "crew", "set", b.crotchY + 3f, 0f, 0.8f))
                Shapes.poly(floatArrayOf(-10f, 62f, 10f, 62f, 7f, 78f, 0f, 83f, -7f, 78f), path2); solid(c, path2, acc)
                com.nugggy.runwayrush.core.Ui.star(c, 0f, 71f, 5f, darker(col, 0.05f))
                line(c, 0xFFE3B341.toInt(), 3f, -b.wa - 1f, b.waistY, b.wa + 1f, b.waistY)
                fill(c, it, Shapes.sym(floatArrayOf(0f, 104f, b.hp + 1f, 104f, b.hp + 1f, b.crotchY + 2f, 0f, b.crotchY + 4f), path2), t, col = acc, fabric = "solid")
            }
            "wetsuit" -> {
                body(upper(b, "turtle", "set", b.crotchY + 3f, 0f, 0.8f))
                Shapes.poly(floatArrayOf(-b.sh, 54f, b.sh, 54f, b.sh - 2f, 68f, -b.sh + 2f, 68f), path2); solid(c, path2, acc)
                line(c, 0xFF888888.toInt(), 0.8f, 0f, 37f, 0f, 100f)
            }
            "swimsuit" -> fill(c, it, Shapes.sym(upper(b, "scoop", "tank", 100f, 0f, 0.8f).let { u -> u.copyOf(u.size - 6) + floatArrayOf(wAt(b, 104f) + 0.8f, 104f, b.hp * 0.45f, 123f, 0f, 125f) }, path), t)
            "skeleton" -> {
                body(upper(b, "crew", "set", b.crotchY + 3f, 0f, 0.8f))
                val wc = Color.WHITE
                line(c, wc, 2.2f, 0f, 52f, 0f, 112f)
                for (i in 0 until 4) { val y = 62f + i * 7f; p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f; p.color = wc; c.drawArc(-12f + i, y - 3f, 12f - i, y + 5f, 200f, 140f, false, p) }
                p.style = Paint.Style.FILL
                Shapes.poly(floatArrayOf(-10f, 108f, 10f, 108f, 6f, 120f, -6f, 120f), path2); solid(c, path2, wc, outline = false)
            }
            "safari" -> {
                body(upper(b, "v", "set", 112f, 1f, 2.4f))
                collar(c, b, col, it, t)
                for (s in intArrayOf(-1, 1)) fill(c, it, Shapes.sym(floatArrayOf(0f, 64f, 5f, 64f, 5f, 64f, 5f, 74f, 5f, 74f, 0f, 74f), path2, s * 8f), t)
                line(c, 0xFF3A2A1A.toInt(), 2.4f, -b.wa - 2f, b.waistY + 1f, b.wa + 2f, b.waistY + 1f)
                fill(c, it, Shapes.sym(floatArrayOf(0f, 112f, b.hp + 2f, 112f, b.hp + 2f, b.crotchY + 2f, 0f, b.crotchY + 4f), path2), t)
            }
            else -> dress("crew", "set", 140f, 10f)
        }
    }

    private fun folds(c: Canvas, b: Body, y0: Float, y1: Float, n: Int, col: Int) {
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.6f; p.color = withAlpha(darker(col, 0.35f), 160)
        for (i in 0 until n) {
            val x0 = -b.hp + (2 * b.hp) * (i + 0.5f) / n
            path2.reset(); path2.moveTo(x0 * 0.8f, y0 + 6f); path2.quadTo(x0 * 1.3f, (y0 + y1) / 2, x0 * 1.9f, y1 - 2f)
            c.drawPath(path2, p)
        }
        p.style = Paint.Style.FILL
    }

    private fun lapels(c: Canvas, b: Body, it: Item, t: Float, col: Int, bottom: Float) {
        for (s in intArrayOf(-1, 1)) {
            Shapes.lines(floatArrayOf(s * 1f, bottom, s * 7f, 46f, s * 11f, 48f, s * 13f, 60f, s * 9f, 64f), path2)
            fill(c, it, path2, t, col = col, fabric = if (it.style.key == "tux") "satin" else it.fabric)
        }
    }

    // ---------------------------------------------------------------- outerwear

    fun outerTorso(c: Canvas, b: Body, it: Item, t: Float) {
        val k = it.style.key
        val col = it.colour.rgb; val acc = it.accent.rgb
        val hem = when (k) {
            "cropjkt" -> 88f; "biker", "bomber", "denimjkt", "varsity" -> 110f; "puffer" -> 116f; "waistcoat" -> 104f
            "cardigan" -> 122f; "blazer" -> 128f; "military" -> 136f; "raincoat" -> 150f; "kimono" -> 150f
            "labcoat" -> 162f; "furcoat", "trench" -> 168f; "poncho" -> 114f; else -> 120f
        }
        val closed = k in setOf("puffer", "raincoat", "varsity", "bomber", "military")
        val ease = when (k) { "puffer" -> 5f; "furcoat" -> 5f; "poncho" -> 0f; "waistcoat" -> 1f; else -> 3f }
        val flare = when (k) { "trench", "labcoat", "raincoat", "furcoat", "kimono" -> 6f; else -> 1f }
        if (k == "poncho") {
            fill(c, it, Shapes.sym(floatArrayOf(0f, 42f, 7f, 42f, 8f, 47f, b.sh + 10f, 75f, b.sh + 16f, 116f, b.sh + 16f, 116f, 0f, 124f), path), t)
            for (i in -6..6) line(c, acc, 0.8f, i * 4f, 116f + kotlin.math.abs(i) * -0.6f, i * 4.2f, 122f - kotlin.math.abs(i) * 0.6f)
            line(c, acc, 1.6f, -b.sh - 8f, 95f, b.sh + 8f, 95f)
            return
        }
        val neck = if (k == "waistcoat") "v" else if (closed) "turtle" else "v"
        val arm = if (k == "waistcoat") "tank" else "set"
        c.save()
        if (!closed) {
            val gap = when (k) { "cardigan", "labcoat", "kimono", "furcoat" -> 6f; "blazer" -> 1f; else -> 3f }
            Shapes.lines(floatArrayOf(-0.5f, 40f, 0.5f, 40f, gap, hem + 2f, -gap, hem + 2f), path2)
            if (k != "waistcoat") c.clipOutPath(path2)
        }
        fill(c, it, Shapes.sym(upper(b, neck, arm, hem, flare, ease), path), t)
        c.restore()
        when (k) {
            "puffer" -> { for (i in 1..5) line(c, darker(col, 0.25f), 0.8f, -b.hp - 5f, 52f + i * 12f, b.hp + 5f, 52f + i * 12f); line(c, darker(col, 0.4f), 0.8f, 0f, 36f, 0f, hem) }
            "biker" -> { line(c, 0xFFC7CBD3.toInt(), 0.9f, 4f, 50f, 2f, hem); lapels(c, b, it, t, col, 66f); line(c, 0xFF1E1C22.toInt(), 2.4f, -b.wa - 3f, hem - 3f, b.wa + 3f, hem - 3f) }
            "blazer", "military" -> {
                if (k == "blazer") lapels(c, b, it, t, col, 84f)
                val rows = if (k == "military") floatArrayOf(64f, 76f, 88f, 100f, 112f) else floatArrayOf(92f, 104f)
                for (y in rows) { if (k == "military") { dot(c, 0xFFE3B341.toInt(), -5f, y, 1.4f); dot(c, 0xFFE3B341.toInt(), 5f, y, 1.4f) } else dot(c, darker(col, 0.4f), 2f, y, 1f) }
                if (k == "military") { for (s in intArrayOf(-1, 1)) { Shapes.lines(floatArrayOf(s * (b.sh - 7f), 49f, s * (b.sh + 4f), 50f, s * (b.sh + 4f), 54f, s * (b.sh - 7f), 53f), path2); solid(c, path2, 0xFFE3B341.toInt()) }; line(c, acc, 1.4f, -b.sh, 62f, b.wa, 100f) }
                if (k == "blazer") fill(c, it, Shapes.sym(floatArrayOf(0f, 106f, 5f, 106f, 5f, 106f, 5f, 108f, 5f, 108f, 0f, 108f), path2, b.wa - 4f), t)
            }
            "trench" -> { lapels(c, b, it, t, col, 70f); line(c, darker(col, 0.3f), 3f, -b.wa - 4f, b.waistY, b.wa + 4f, b.waistY); for (y in floatArrayOf(70f, 82f, 104f)) { dot(c, darker(col, 0.45f), -6f, y, 1.2f); dot(c, darker(col, 0.45f), 6f, y, 1.2f) } }
            "denimjkt" -> { collar(c, b, col, it, t); for (s in intArrayOf(-1, 1)) { line(c, 0xFFE3B341.toInt(), 0.5f, s * 4f, 66f, s * 13f, 66f); dot(c, 0xFFC7CBD3.toInt(), s * 8f, 70f, 1f) }; line(c, darker(col, 0.3f), 1.8f, -b.wa - 3f, hem - 3f, b.wa + 3f, hem - 3f) }
            "cardigan" -> { for (i in 0 until 5) dot(c, acc, 6f, 60f + i * 11f, 1.1f); line(c, darker(col, 0.25f), 1.6f, -b.hp - 3f, hem - 2.5f, b.hp + 3f, hem - 2.5f) }
            "furcoat" -> { fluff(c, -5f, 50f, 8f, lighter(col, 0.15f)); fluff(c, 5f, 50f, 8f, lighter(col, 0.15f)) }
            "bomber", "varsity" -> {
                for (y in floatArrayOf(hem - 4f, hem - 2f)) line(c, acc, 0.9f, -b.wa - 3f, y, b.wa + 3f, y)
                line(c, acc, 1.6f, -6.8f, 38f, 6.8f, 38f)
                line(c, 0xFFC7CBD3.toInt(), 0.8f, 0f, 38f, 0f, hem)
                if (k == "varsity") { val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; typeface = android.graphics.Typeface.DEFAULT_BOLD; color = acc; textAlign = Paint.Align.CENTER }; c.drawText(('A' + it.id % 26).toString(), -9f, 76f, tp) }
            }
            "raincoat" -> { for (y in floatArrayOf(66f, 82f, 98f, 114f)) dot(c, darker(col, 0.4f), 0f, y, 1.3f); fill(c, it, Shapes.sym(floatArrayOf(0f, 37f, 14f, 38f, 18f, 46f, 12f, 52f, 0f, 52f), path2), t) }
            "waistcoat" -> { for (i in 0 until 4) dot(c, darker(col, 0.45f), 0f, 70f + i * 9f, 1f); line(c, 0xFFE3B341.toInt(), 0.5f, 0f, 84f, 9f, 88f) }
            "labcoat" -> { lapels(c, b, it, t, col, 70f); for (s in intArrayOf(-1, 1)) line(c, darker(col, 0.2f), 0.6f, s * 8f, 118f, s * 18f, 118f); line(c, 0xFF2F55D4.toInt(), 1f, -10f, 68f, -10f, 74f) }
            "kimono" -> { line(c, acc, 2f, -6.5f, 45f, -6f, hem); line(c, acc, 2f, 6.5f, 45f, 6f, hem) }
            "cropjkt" -> lapels(c, b, it, t, col, 70f)
        }
    }

    // ---------------------------------------------------------------- neck, ears, glasses

    fun neck(c: Canvas, b: Body, it: Item, t: Float) {
        val col = it.colour.rgb; val acc = it.accent.rgb
        when (it.style.key) {
            "pearls" -> for (i in 0..14) { val a = Math.PI * (0.1 + 0.8 * i / 14.0); shine(c, (cos(a) * 11).toFloat(), 47f + (sin(a) * 11).toFloat(), 1.5f, col) }
            "chain" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f; p.color = col; for (i in 0..10) { val a = Math.PI * (0.1 + 0.8 * i / 10.0); val x = (cos(a) * 12).toFloat(); val y = 47f + (sin(a) * 14).toFloat(); c.drawOval(x - 1.8f, y - 1.2f, x + 1.8f, y + 1.2f, p) }; p.style = Paint.Style.FILL }
            "choker" -> { fill(c, it, Shapes.lines(floatArrayOf(-6f, 41f, 6f, 41f, 6f, 44f, -6f, 44f), path), t); shine(c, 0f, 45.5f, 1.6f, acc) }
            "scarf" -> {
                fill(c, it, Shapes.poly(floatArrayOf(-9f, 41f, 9f, 41f, 11f, 50f, 0f, 53f, -11f, 50f), path), t)
                fill(c, it, Shapes.lines(floatArrayOf(3f, 49f, 10f, 49f, 11f, 90f, 4f, 92f), path2), t)
                for (i in 0..3) line(c, darker(col, 0.2f), 0.5f, 4.5f + i * 1.8f, 91f, 4.5f + i * 1.8f, 95f)
            }
            "bowtie" -> bow(c, 0f, 47f, 5.5f, col)
            "tie" -> { fill(c, it, Shapes.poly(floatArrayOf(-2.2f, 46f, 2.2f, 46f, 1.6f, 51f, 3.5f, 88f, 0f, 93f, -3.5f, 88f, -1.6f, 51f), path), t) }
            "pendant" -> { line(c, 0xFFE3B341.toInt(), 0.4f, -7f, 46f, 0f, 63f, 7f, 46f); Shapes.poly(floatArrayOf(0f, 61f, 2.6f, 64.5f, 0f, 69f, -2.6f, 64.5f), path); solid(c, path, col); dot(c, 0x99FFFFFF.toInt(), -0.6f, 63.5f, 0.6f) }
            "lei" -> for (i in 0..16) { val a = Math.PI * (0.0 + 1.0 * i / 16.0); val x = (cos(a) * 13).toFloat(); val y = 48f + (sin(a) * 13).toFloat(); flower(c, x, y, 2.3f, if (i % 3 == 0) acc else col) }
            "boa" -> {
                val fc = col
                for (s in intArrayOf(-1, 1)) for (i in 0..18) { val y = 46f + i * 3.6f; val x = s * (7f + if (i < 3) i * 3.5f else 10.5f - (i - 3) * 0.12f); fuzz(c, x, y, 3.2f, fc, i) }
            }
            "medal" -> { line(c, acc, 2f, -7f, 46f, 0f, 70f, 7f, 46f); shine(c, 0f, 73f, 4.5f, col); com.nugggy.runwayrush.core.Ui.star(c, 0f, 73f, 2.2f, darker(col, 0.25f)) }
            "silkscarf" -> { fill(c, it, Shapes.poly(floatArrayOf(-8f, 42f, 8f, 42f, 6f, 48f, -6f, 48f), path), t); fill(c, it, Shapes.poly(floatArrayOf(-1f, 46f, 5f, 47f, 7f, 58f, 1f, 55f), path2), t) }
        }
    }

    private fun flower(c: Canvas, x: Float, y: Float, r: Float, col: Int) {
        for (k in 0 until 5) { val a = k * Math.PI * 2 / 5; dot(c, col, x + (cos(a) * r * 0.6f).toFloat(), y + (sin(a) * r * 0.6f).toFloat(), r * 0.55f) }
        dot(c, 0xFFFFD86B.toInt(), x, y, r * 0.35f)
    }

    private fun fuzz(c: Canvas, x: Float, y: Float, r: Float, col: Int, i: Int) {
        dot(c, darker(col, 0.15f), x, y, r); dot(c, col, x - 0.6f, y - 0.6f, r * 0.75f); dot(c, lighter(col, 0.25f), x + (i % 3 - 1) * 0.8f, y - 1f, r * 0.4f)
    }

    fun ears(c: Canvas, b: Body, it: Item, t: Float) {
        val col = it.colour.rgb
        for (s in intArrayOf(-1, 1)) {
            val x = s * 15.5f; val y = 28.5f
            when (it.style.key) {
                "studs" -> shine(c, x, y, 1.3f, col)
                "hoops" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 1f; p.color = col; c.drawCircle(x, y + 4f, 4f, p); p.style = Paint.Style.FILL }
                "drops" -> { line(c, 0xFFE3B341.toInt(), 0.4f, x, y, x, y + 4f); Shapes.poly(floatArrayOf(x, y + 3.5f, x + 1.8f, y + 7f, x, y + 9f, x - 1.8f, y + 7f), path); solid(c, path, col) }
                "tassels" -> { Shapes.lines(floatArrayOf(x, y, x + 2.5f, y + 10f, x - 2.5f, y + 10f), path); solid(c, path, col) }
                "starsear" -> com.nugggy.runwayrush.core.Ui.star(c, x, y + 2f, 2.4f, col)
            }
        }
    }

    fun glasses(c: Canvas, b: Body, it: Item, t: Float) {
        val col = it.colour.rgb
        val frame = darker(col, 0.3f)
        val lens = withAlpha(darker(col, 0.55f), 220)
        val ey = 22.5f
        fun lensPair(shapeFor: (Int) -> Path, fillCol: Int) {
            for (s in intArrayOf(-1, 1)) { val sh = shapeFor(s); p.style = Paint.Style.FILL; p.color = fillCol; c.drawPath(sh, p); p.style = Paint.Style.STROKE; p.strokeWidth = 1f; p.color = frame; c.drawPath(sh, p); p.style = Paint.Style.FILL
                p.color = 0x55FFFFFF; c.drawCircle(s * 6.2f - 1.4f, ey - 1.4f, 0.9f, p) }
            line(c, frame, 0.8f, -2.2f, ey - 0.5f, 2.2f, ey - 0.5f)
            line(c, frame, 0.7f, -11f, ey - 1f, -15f, ey - 2f); line(c, frame, 0.7f, 11f, ey - 1f, 15f, ey - 2f)
        }
        when (it.style.key) {
            "roundsun" -> lensPair({ s -> Path().apply { addCircle(s * 6.4f, ey, 4.6f, Path.Direction.CW) } }, lens)
            "aviator" -> lensPair({ s -> Shapes.poly(floatArrayOf(s * 2.4f, ey - 3.5f, s * 10.5f, ey - 3.5f, s * 10f, ey + 2f, s * 6.5f, ey + 5f, s * 2.8f, ey + 1f), Path()) }, withAlpha(0xFF6E5A30.toInt(), 200))
            "cateye" -> lensPair({ s -> Shapes.poly(floatArrayOf(s * 2.2f, ey - 2.2f, s * 8f, ey - 3.5f, s * 12f, ey - 6f, s * 10.5f, ey + 2f, s * 6f, ey + 3.5f, s * 2.5f, ey + 1.5f), Path()) }, withAlpha(col, 90))
            "heartglasses" -> lensPair({ s -> heart(s * 6.4f, ey, 5.2f) }, withAlpha(col, 200))
            "specs" -> lensPair({ s -> Path().apply { addRoundRect(s * 6.4f - 4.4f, ey - 3.2f, s * 6.4f + 4.4f, ey + 3.2f, 1.2f, 1.2f, Path.Direction.CW) } }, 0x22FFFFFF)
            "starglasses" -> lensPair({ s -> starPath(s * 6.4f, ey, 5.4f) }, withAlpha(col, 210))
            "goggles" -> { line(c, darker(col, 0.2f), 2.2f, -16f, ey - 2f, 16f, ey - 2f); lensPair({ s -> Path().apply { addOval(s * 6.4f - 4.8f, ey - 3.8f, s * 6.4f + 4.8f, ey + 3.8f, Path.Direction.CW) } }, withAlpha(0xFF8FCBF2.toInt(), 150)) }
            "visor" -> {
                Shapes.poly(floatArrayOf(-16f, ey - 4f, 16f, ey - 4f, 15f, ey + 3.5f, 0f, ey + 5f, -15f, ey + 3.5f), path)
                p.color = -16777216; p.shader = LinearGradient(-16f, 0f, 16f, 0f, intArrayOf(withAlpha(col, 210), withAlpha(lighter(col, 0.5f), 200), withAlpha(col, 210)), null, Shader.TileMode.CLAMP)
                c.drawPath(path, p); p.shader = null
                line(c, withAlpha(Color.WHITE, 140), 0.6f, -12f, ey - 2f, 12f, ey - 2f)
            }
            "eyepatch" -> { line(c, 0xFF1E1C22.toInt(), 0.7f, -16f, 14f, 16f, 30f); p.color = col; c.drawOval(-10.5f, ey - 4f, -2f, ey + 4f, p) }
            "monocle" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 1f; p.color = col; c.drawCircle(6.2f, ey, 4.4f, p); p.style = Paint.Style.FILL; line(c, col, 0.4f, 10f, ey + 2f, 13f, 50f) }
        }
    }

    private fun heart(x: Float, y: Float, s: Float) = Path().apply {
        moveTo(x, y + s * 0.6f)
        cubicTo(x - s * 1.1f, y - s * 0.1f, x - s * 0.5f, y - s * 0.9f, x, y - s * 0.3f)
        cubicTo(x + s * 0.5f, y - s * 0.9f, x + s * 1.1f, y - s * 0.1f, x, y + s * 0.6f); close()
    }

    private fun starPath(x: Float, y: Float, r: Float) = Path().apply {
        for (i in 0 until 10) { val rr = if (i % 2 == 0) r else r * 0.45f; val a = -Math.PI / 2 + i * Math.PI / 5
            val px = x + (rr * cos(a)).toFloat(); val py = y + (rr * sin(a)).toFloat(); if (i == 0) moveTo(px, py) else lineTo(px, py) }
        close()
    }

    // ---------------------------------------------------------------- hats

    fun hat(c: Canvas, b: Body, it: Item, t: Float) {
        val col = it.colour.rgb; val acc = it.accent.rgb
        fun f(pts: FloatArray) = fill(c, it, Shapes.poly(pts, path), t)
        fun fs(pts: FloatArray) = fill(c, it, Shapes.sym(pts, path), t)
        when (it.style.key) {
            "cap" -> { fs(floatArrayOf(0f, -3f, 10f, -1.5f, 16.5f, 8f, 17f, 13f, 0f, 13f)); fill(c, it, Shapes.poly(floatArrayOf(-17f, 11f, 17f, 11f, 21f, 15f, 0f, 17f, -21f, 15f), path2), t); dot(c, acc, 0f, -2.5f, 1.4f) }
            "beanie" -> { fs(floatArrayOf(0f, -6f, 11f, -4f, 17f, 6f, 17.5f, 13f, 0f, 13f)); fill(c, it, Shapes.sym(floatArrayOf(0f, 8f, 18f, 8f, 18f, 8f, 18f, 15f, 18f, 15f, 0f, 15f), path2), t); for (i in -7..7) line(c, darker(col, 0.25f), 0.5f, i * 2.4f, 8.5f, i * 2.4f, 14.5f); fuzz(c, 0f, -7f, 4f, acc, 1) }
            "sunhat" -> { fill(c, it, Shapes.poly(floatArrayOf(-36f, 11f, -20f, 4f, 20f, 4f, 36f, 11f, 20f, 15f, -20f, 15f), path2), t); fs(floatArrayOf(0f, -9f, 10f, -8f, 15f, 0f, 15f, 9f, 0f, 9f)); line(c, acc, 2.6f, -15f, 6f, 15f, 6f); flower(c, 12f, 5f, 3.5f, acc) }
            "fedora" -> { fill(c, it, Shapes.poly(floatArrayOf(-25f, 9f, -15f, 4f, 15f, 4f, 25f, 9f, 15f, 12f, -15f, 12f), path2), t); f(floatArrayOf(-14f, 8f, -12f, -7f, -4f, -10f, 0f, -7f, 4f, -10f, 12f, -7f, 14f, 8f)); line(c, acc, 2.4f, -14f, 4.5f, 14f, 4.5f) }
            "tophat" -> { fill(c, it, Shapes.poly(floatArrayOf(-21f, 6f, 21f, 6f, 21f, 9.5f, -21f, 9.5f), path2), t); f(floatArrayOf(-12f, 7f, -12f, 7f, -13f, -27f, -13f, -27f, 13f, -27f, 13f, -27f, 12f, 7f, 12f, 7f)); line(c, acc, 3f, -12.5f, 2f, 12.5f, 2f) }
            "beret" -> { f(floatArrayOf(-19f, 8f, -16f, -2f, -2f, -6f, 14f, -3f, 18f, 4f, 10f, 9f, -10f, 10f)); dot(c, darker(col, 0.2f), 2f, -6f, 1.6f) }
            "cowboyhat" -> { fill(c, it, Shapes.poly(floatArrayOf(-30f, 0f, -24f, 8f, -12f, 11f, 12f, 11f, 24f, 8f, 30f, 0f, 22f, 4f, 0f, 6f, -22f, 4f), path2), t); f(floatArrayOf(-13f, 6f, -12f, -9f, -5f, -12f, 0f, -9f, 5f, -12f, 12f, -9f, 13f, 6f)); line(c, acc, 2f, -13f, 3f, 13f, 3f) }
            "crown" -> { fill(c, it, Shapes.lines(floatArrayOf(-14f, 7f, -15f, -10f, -8f, -2f, 0f, -14f, 8f, -2f, 15f, -10f, 14f, 7f), path), t, fabric = "gold".takeIf { col == 0xFFE3B341.toInt() } ?: "solid"); for ((i, x) in floatArrayOf(-8f, 0f, 8f).withIndex()) shine(c, x, 3f, 1.8f, if (i == 1) acc else 0xFFE0263A.toInt()); sparkleAt(c, t, 0f, -4f, 14f) }
            "tiara" -> { fill(c, it, Shapes.lines(floatArrayOf(-12f, 6f, -9f, 0f, -4f, 2f, 0f, -6f, 4f, 2f, 9f, 0f, 12f, 6f, 0f, 3f), path), t); shine(c, 0f, -2f, 1.8f, acc); sparkleAt(c, t, 0f, 0f, 12f) }
            "witch" -> { fill(c, it, Shapes.poly(floatArrayOf(-30f, 9f, -18f, 4f, 18f, 4f, 30f, 9f, 18f, 13f, -18f, 13f), path2), t); fill(c, it, Shapes.lines(floatArrayOf(-14f, 7f, -6f, -24f, 10f, -42f, 4f, -26f, 14f, 7f), path), t); line(c, acc, 2.5f, -13.5f, 3f, 13.5f, 3f); p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; p.color = 0xFFE3B341.toInt(); c.drawRect(-3f, 1.2f, 3f, 4.8f, p); p.style = Paint.Style.FILL }
            "bucket" -> { fill(c, it, Shapes.poly(floatArrayOf(-21f, 14f, -14f, 6f, 14f, 6f, 21f, 14f, 0f, 16f), path2), t); fs(floatArrayOf(0f, -5f, 11f, -4f, 14.5f, 8f, 14.5f, 8f, 0f, 9f)) }
            "headband" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = col; c.drawArc(-17f, 0f, 17f, 30f, 195f, 150f, false, p); p.style = Paint.Style.FILL; if (it.id % 2 == 0) bow(c, 10f, 4f, 4.5f, acc) }
            "flowercrown" -> { for (i in 0..8) { val a = Math.PI * (1.08 + 0.84 * i / 8.0); flower(c, (cos(a) * 17).toFloat(), 14f + (sin(a) * 12).toFloat(), 3f, if (i % 2 == 0) col else acc) } }
            "bunny" -> for (s in intArrayOf(-1, 1)) { fill(c, it, Shapes.poly(floatArrayOf(s * 5f, 4f, s * 4f, -16f, s * 8f, -32f, s * 13f, -16f, s * 12f, 4f), path), t); Shapes.poly(floatArrayOf(s * 7f, 0f, s * 7f, -16f, s * 8.4f, -27f, s * 10.5f, -16f, s * 10f, 0f), path2); solid(c, path2, 0xFFFF9EC7.toInt(), false) }
            "cat" -> for (s in intArrayOf(-1, 1)) { fill(c, it, Shapes.lines(floatArrayOf(s * 6f, 5f, s * 12f, -10f, s * 16f, 7f), path), t); Shapes.lines(floatArrayOf(s * 8.5f, 4f, s * 12f, -5f, s * 14f, 5f), path2); solid(c, path2, 0xFFFF9EC7.toInt(), false) }
            "partyhat" -> { fill(c, it, Shapes.lines(floatArrayOf(-9f, 5f, 2f, -30f, 9f, 5f), path), t); fuzz(c, 2f, -31f, 3.2f, acc, 2) }
            "halo" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = withAlpha(col, 80); c.drawOval(-14f, -16f, 14f, -6f, p); p.strokeWidth = 1.8f; p.color = col; c.drawOval(-13f, -15f, 13f, -7f, p); p.style = Paint.Style.FILL; sparkleAt(c, t, 0f, -11f, 14f) }
            "horns" -> for (s in intArrayOf(-1, 1)) fill(c, it, Shapes.poly(floatArrayOf(s * 6f, 6f, s * 8f, -2f, s * 14f, -12f, s * 13f, -2f, s * 12f, 6f), path), t)
            "chef" -> { fs(floatArrayOf(0f, -26f, 9f, -28f, 16f, -20f, 15f, -10f, 12f, -4f, 12f, -4f, 0f, -4f)); fill(c, it, Shapes.sym(floatArrayOf(0f, -5f, 13f, -5f, 13f, -5f, 14f, 8f, 14f, 8f, 0f, 8f), path2), t) }
            "helmet" -> {
                p.style = Paint.Style.FILL; p.color = -16777216; p.shader = RadialGradient(-6f, 12f, 30f, intArrayOf(0x30FFFFFF, 0x1A8FCBF2, 0x608FCBF2), floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP)
                c.drawCircle(0f, 21f, 26f, p); p.shader = null
                p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f; p.color = withAlpha(Color.WHITE, 160); c.drawCircle(0f, 21f, 26f, p); c.drawArc(-20f, 0f, 4f, 30f, 200f, 50f, false, p); p.style = Paint.Style.FILL
                fill(c, it, Shapes.sym(floatArrayOf(0f, 42f, 18f, 41f, 20f, 47f, 20f, 47f, 0f, 49f), path2), t)
            }
            "bandana" -> { fs(floatArrayOf(0f, -2f, 10f, -1f, 16.5f, 8f, 17f, 13f, 12f, 11f, 0f, 10f)); fill(c, it, Shapes.poly(floatArrayOf(15f, 10f, 23f, 14f, 21f, 20f, 16f, 15f), path2), t) }
            "pirate" -> { fill(c, it, Shapes.poly(floatArrayOf(-27f, 8f, -18f, -10f, 0f, -6f, 18f, -10f, 27f, 8f, 0f, 12f), path), t); line(c, 0xFFE3B341.toInt(), 1f, -25f, 7.5f, 0f, 11f, 25f, 7.5f); dot(c, Color.WHITE, 0f, 0f, 3f); dot(c, 0xFF1E1C22.toInt(), -1f, 0f, 0.7f); dot(c, 0xFF1E1C22.toInt(), 1f, 0f, 0.7f) }
            "antlers" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f; p.color = col; for (s in intArrayOf(-1, 1)) { c.drawLine(s * 8f, 4f, s * 14f, -18f, p); c.drawLine(s * 11.5f, -8f, s * 19f, -12f, p); c.drawLine(s * 13f, -14f, s * 9f, -22f, p) }; p.style = Paint.Style.FILL; line(c, darker(col, 0.2f), 2.5f, -16f, 8f, 16f, 8f) }
            "santa" -> { fill(c, it, Shapes.poly(floatArrayOf(-16f, 8f, -10f, -8f, 6f, -14f, 22f, -6f, 26f, 6f, 16f, -2f, 16f, 8f), path), t); fuzz(c, 26f, 7f, 3.6f, Color.WHITE, 1); for (i in -4..4) fuzz(c, i * 4f, 9f, 3f, Color.WHITE, i + 4) }
        }
    }

    private fun sparkleAt(c: Canvas, t: Float, x: Float, y: Float, w: Float) {
        for (i in 0 until 3) {
            val ph = (t * 1.2f + i * 0.33f) % 1f
            val al = (sin(ph * Math.PI).toFloat() * 255).toInt()
            val sx = x + ((i * 37) % 7 - 3) * w / 7f; val sy = y + ((i * 13) % 5 - 2)
            sp.style = Paint.Style.STROKE; sp.strokeWidth = 0.5f; sp.color = withAlpha(Color.WHITE, al)
            val r = 1f + ph * 1.5f
            c.drawLine(sx - r, sy, sx + r, sy, sp); c.drawLine(sx, sy - r, sx, sy + r, sp)
            sp.style = Paint.Style.FILL
        }
    }

    // ---------------------------------------------------------------- shoes

    fun shoe(c: Canvas, b: Body, it: Item, side: Int, t: Float) {
        val L = b.legLen
        val col = it.colour.rgb; val acc = it.accent.rgb
        val lw = b.legW * 0.55f
        fun boot(top: Float, topW: Float) = fill(c, it, Shapes.sym(floatArrayOf(0f, top, topW, top, topW, top, lw + 1.6f, L - 4f, lw + 2.5f, L + 3f, lw + 3f, L + 7f, lw + 3f, L + 7f, 0f, L + 8f), path), t)
        when (it.style.key) {
            "sneakers" -> { fill(c, it, Shapes.sym(floatArrayOf(0f, L - 5f, lw + 1.5f, L - 5f, lw + 3.5f, L + 2f, lw + 3.5f, L + 6f, 0f, L + 6.5f), path), t); solid(c, Shapes.sym(floatArrayOf(0f, L + 5f, lw + 3.6f, L + 5f, lw + 3.6f, L + 5f, lw + 3.6f, L + 8f, lw + 3.6f, L + 8f, 0f, L + 8f), path2), Color.WHITE); line(c, acc, 0.9f, -2f, L - 2f, 2f, L - 2f, -2f, L, 2f, L) }
            "heels" -> { fill(c, it, Shapes.sym(floatArrayOf(0f, L - 1f, lw + 1.5f, L - 1f, lw + 2.4f, L + 4f, 1.5f, L + 9f, 0f, L + 9f), path), t); line(c, darker(col, 0.3f), 1.2f, side * (lw + 0.5f), L + 3f, side * (lw + 1f), L + 9f) }
            "ankleboots" -> boot(L - 16f, lw + 1.8f)
            "kneeboots" -> boot(28f, b.legW * 0.78f + 1.6f)
            "cowboyboots" -> { boot(44f, b.legW * 0.7f + 2f); line(c, acc, 0.6f, -3f, 50f, 0f, 58f, 3f, 50f); line(c, acc, 0.6f, -3f, 58f, 0f, 66f, 3f, 58f) }
            "sandals" -> { line(c, col, 1.3f, -lw - 1.5f, L + 1f, lw + 1.5f, L + 1f); line(c, col, 1.3f, -lw - 2f, L + 5f, lw + 2f, L + 5f); line(c, col, 1f, 0f, L - 4f, 0f, L + 5f); solid(c, Shapes.sym(floatArrayOf(0f, L + 7f, lw + 3f, L + 7f, lw + 3f, L + 7f, lw + 3f, L + 8.5f, lw + 3f, L + 8.5f, 0f, L + 8.5f), path2), darker(col, 0.2f)) }
            "flats" -> { fill(c, it, Shapes.sym(floatArrayOf(0f, L + 1f, lw + 2f, L + 1f, lw + 3f, L + 5f, lw + 3f, L + 7f, 0f, L + 7.5f), path), t); bow(c, 0f, L + 2f, 2.4f, acc) }
            "platforms" -> { fill(c, it, Shapes.sym(floatArrayOf(0f, L - 3f, lw + 2f, L - 3f, lw + 3f, L + 3f, lw + 3.5f, L + 13f, lw + 3.5f, L + 13f, 0f, L + 13f), path), t); line(c, darker(col, 0.35f), 0.7f, -lw - 3f, L + 5f, lw + 3f, L + 5f) }
            "loafers" -> { fill(c, it, Shapes.sym(floatArrayOf(0f, L - 1f, lw + 2f, L - 1f, lw + 3f, L + 4f, lw + 3f, L + 7.5f, 0f, L + 8f), path), t); line(c, acc, 1.2f, -lw, L + 1.5f, lw, L + 1.5f) }
            "gumboots" -> { boot(38f, b.legW * 0.78f + 2.2f); line(c, darker(col, 0.25f), 2f, -b.legW * 0.78f - 2f, 40f, b.legW * 0.78f + 2f, 40f) }
            "combat" -> { boot(L - 26f, lw + 2.4f); for (i in 0 until 5) line(c, acc, 0.6f, -2f, L - 24f + i * 5f, 2f, L - 22f + i * 5f); solid(c, Shapes.sym(floatArrayOf(0f, L + 6f, lw + 3.2f, L + 6f, lw + 3.2f, L + 6f, lw + 3.2f, L + 9f, lw + 3.2f, L + 9f, 0f, L + 9f), path2), 0xFF1E1C22.toInt()) }
            "slippers" -> { for (i in 0 until 6) fuzz(c, (i - 2.5f) * 2.4f, L + 3f + (i % 2), 3f, col, i) }
            "rollers" -> { boot(L - 18f, lw + 2f); solid(c, Shapes.sym(floatArrayOf(0f, L + 7f, lw + 3.5f, L + 7f, lw + 3.5f, L + 7f, lw + 3.5f, L + 9f, lw + 3.5f, L + 9f, 0f, L + 9f), path2), 0xFFC7CBD3.toInt()); dot(c, acc, -3f, L + 11.5f, 2.6f); dot(c, acc, 3f, L + 11.5f, 2.6f) }
            "flippers" -> fill(c, it, Shapes.sym(floatArrayOf(0f, L - 2f, lw + 2f, L - 2f, lw + 6f, L + 10f, lw + 10f, L + 20f, 0f, L + 18f), path), t)
            "glass" -> {
                Shapes.sym(floatArrayOf(0f, L - 1f, lw + 1.5f, L - 1f, lw + 2.4f, L + 4f, 1.5f, L + 9f, 0f, L + 9f), path)
                p.style = Paint.Style.FILL; p.color = withAlpha(lighter(col, 0.5f), 150); c.drawPath(path, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 0.7f; p.color = Color.WHITE; c.drawPath(path, p); p.style = Paint.Style.FILL
                sparkleAt(c, t, 0f, L + 3f, 6f)
            }
        }
    }

    // ---------------------------------------------------------------- hand-held items

    fun handItem(c: Canvas, b: Body, it: Item, t: Float) {
        val col = it.colour.rgb; val acc = it.accent.rgb
        val hy = b.armLen + 3.5f
        when (it.style.key) {
            "handbag" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = darker(col, 0.3f); c.drawArc(-6f, hy - 4f, 6f, hy + 10f, 180f, 180f, false, p); p.style = Paint.Style.FILL
                fill(c, it, Shapes.poly(floatArrayOf(-9f, hy + 3f, 9f, hy + 3f, 9f, hy + 3f, 11f, hy + 18f, 11f, hy + 18f, -11f, hy + 18f, -11f, hy + 18f, -9f, hy + 3f), path), t); dot(c, 0xFFE3B341.toInt(), 0f, hy + 7f, 1.2f) }
            "clutch" -> { fill(c, it, Shapes.poly(floatArrayOf(-11f, hy - 3f, 7f, hy - 3f, 7f, hy - 3f, 7f, hy + 6f, 7f, hy + 6f, -11f, hy + 6f, -11f, hy + 6f, -11f, hy - 3f), path), t); line(c, darker(col, 0.3f), 0.6f, -11f, hy, 7f, hy + 2f) }
            "tote" -> { line(c, darker(col, 0.25f), 1.4f, -6f, hy + 8f, 0f, hy, 6f, hy + 8f); fill(c, it, Shapes.lines(floatArrayOf(-11f, hy + 8f, 11f, hy + 8f, 12f, hy + 32f, -12f, hy + 32f), path), t) }
            "basket" -> { p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f; p.color = darker(col, 0.2f); c.drawArc(-9f, hy - 2f, 9f, hy + 18f, 180f, 180f, false, p); p.style = Paint.Style.FILL
                fill(c, it, Shapes.lines(floatArrayOf(-12f, hy + 8f, 12f, hy + 8f, 9f, hy + 24f, -9f, hy + 24f), path), t); for (i in 0..3) line(c, darker(col, 0.35f), 0.6f, -11f, hy + 11f + i * 4f, 11f, hy + 11f + i * 4f); fill(c, it, Shapes.lines(floatArrayOf(-11f, hy + 8f, 11f, hy + 8f, 11f, hy + 10f, -11f, hy + 10f), path2), t, col = 0xFFE0263A.toInt(), fabric = "gingham") }
            "briefcase" -> { line(c, darker(col, 0.3f), 1.6f, -4f, hy + 4f, -4f, hy, 4f, hy, 4f, hy + 4f); fill(c, it, Shapes.lines(floatArrayOf(-13f, hy + 4f, 13f, hy + 4f, 13f, hy + 22f, -13f, hy + 22f), path), t); dot(c, 0xFFE3B341.toInt(), -6f, hy + 6f, 1f); dot(c, 0xFFE3B341.toInt(), 6f, hy + 6f, 1f) }
            "wand" -> { line(c, 0xFF3A2A20.toInt(), 1.3f, 0f, hy, 10f, hy - 22f); com.nugggy.runwayrush.core.Ui.star(c, 11f, hy - 25f, 5f, col); sparkleAt(c, t, 11f, hy - 25f, 14f) }
            "umbrella" -> { line(c, 0xFF3A2A20.toInt(), 1.2f, 0f, hy - 4f, 0f, hy + 64f); p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f; p.color = 0xFF3A2A20.toInt(); c.drawArc(-4f, hy - 8f, 0f, hy - 2f, 180f, 180f, false, p); p.style = Paint.Style.FILL
                fill(c, it, Shapes.poly(floatArrayOf(0f, hy + 6f, 4f, hy + 20f, 3f, hy + 50f, 0f, hy + 58f, -3f, hy + 50f, -4f, hy + 20f), path), t) }
            "guitar" -> { line(c, 0xFF5A3420.toInt(), 2.4f, 0f, hy - 22f, 0f, hy + 10f); fill(c, it, Shapes.poly(floatArrayOf(-8f, hy + 8f, 0f, hy + 4f, 8f, hy + 8f, 11f, hy + 18f, 7f, hy + 22f, 11f, hy + 30f, 0f, hy + 36f, -11f, hy + 30f, -7f, hy + 22f, -11f, hy + 18f), path), t); Shapes.poly(floatArrayOf(-4f, hy + 18f, 4f, hy + 18f, 5f, hy + 28f, -5f, hy + 28f), path2); solid(c, path2, acc); for (i in -1..1) line(c, 0xFFC7CBD3.toInt(), 0.3f, i * 0.8f, hy - 22f, i * 0.8f, hy + 30f) }
            "racket" -> { line(c, 0xFF3A2A20.toInt(), 2f, 0f, hy - 4f, 0f, hy + 12f); p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = col; c.drawOval(-8f, hy + 12f, 8f, hy + 34f, p); p.strokeWidth = 0.3f; p.color = 0xCCFFFFFF.toInt(); for (i in -3..3) { c.drawLine(i * 2f, hy + 13f, i * 2f, hy + 33f, p); c.drawLine(-7f, hy + 23f + i * 2.8f, 7f, hy + 23f + i * 2.8f, p) }; p.style = Paint.Style.FILL }
            "bouquet" -> { Shapes.lines(floatArrayOf(-7f, hy - 6f, 7f, hy - 6f, 1f, hy + 14f, -1f, hy + 14f), path); solid(c, path, 0xFF4E9A5A.toInt()); for (i in 0 until 6) flower(c, -6f + (i % 3) * 6f, hy - 9f - (i / 3) * 5f, 3f, if (i % 2 == 0) col else acc) }
        }
    }

    // ---------------------------------------------------------------- back items

    fun back(c: Canvas, b: Body, it: Item, t: Float, behind: Boolean) {
        val col = it.colour.rgb; val acc = it.accent.rgb
        val flap = sin(t * 3f) * 3f
        val k = it.style.key
        if (!behind) {
            when (k) {
                "backpack" -> for (s in intArrayOf(-1, 1)) { line(c, darker(col, 0.2f), 3f, s * (b.sh * 0.55f), 50f, s * (b.wa + 1f), 92f) }
                "cape", "herocape" -> { line(c, darker(col, 0.25f), 2f, -9f, 47f, 9f, 47f); shine(c, 0f, 48f, 2.4f, if (k == "cape") 0xFFE3B341.toInt() else acc) }
                "jetpack" -> for (s in intArrayOf(-1, 1)) line(c, 0xFF45434C.toInt(), 2.4f, s * (b.sh * 0.55f), 50f, s * (b.wa + 1f), 90f)
                "surfboard" -> {}
            }
            return
        }
        when (k) {
            "angel", "fairy", "bat", "butterfly", "dragonwings" -> for (s in intArrayOf(-1, 1)) {
                c.save(); c.scale(s.toFloat(), 1f); c.rotate(-flap * 0.6f, 4f, 66f)
                when (k) {
                    "angel" -> { for (layer in 0..2) { val r = 1f - layer * 0.22f
                        fill(c, it, Shapes.poly(floatArrayOf(4f, 58f, 22f * r, 34f, 46f * r, 24f, 62f * r, 30f, 58f * r, 50f, 50f * r, 72f, 38f * r, 96f, 24f * r, 110f, 12f, 92f), path), t, col = if (layer == 1) lighter(col, 0.15f) else col)
                        for (i in 0 until 5) { p.style = Paint.Style.STROKE; p.strokeWidth = 0.6f; p.color = darker(col, 0.25f); c.drawLine(12f, 70f, (54f - i * 8f) * r, 40f + i * 16f * r, p); p.style = Paint.Style.FILL } } }
                    "fairy" -> { p.alpha = 255; fill(c, it, Shapes.poly(floatArrayOf(4f, 64f, 26f, 30f, 50f, 22f, 54f, 40f, 36f, 64f), path), t); fill(c, it, Shapes.poly(floatArrayOf(4f, 70f, 34f, 74f, 42f, 96f, 26f, 104f, 10f, 86f), path2), t)
                        p.style = Paint.Style.STROKE; p.strokeWidth = 0.6f; p.color = withAlpha(Color.WHITE, 180); c.drawLine(6f, 64f, 46f, 28f, p); c.drawLine(6f, 70f, 34f, 96f, p); p.style = Paint.Style.FILL }
                    "bat" -> fill(c, it, Shapes.lines(floatArrayOf(4f, 60f, 20f, 36f, 56f, 26f, 60f, 44f, 50f, 50f, 48f, 62f, 38f, 64f, 34f, 76f, 24f, 74f, 14f, 86f), path), t)
                    "dragonwings" -> { fill(c, it, Shapes.lines(floatArrayOf(4f, 60f, 18f, 30f, 62f, 6f, 58f, 30f, 64f, 52f, 50f, 54f, 52f, 74f, 38f, 72f, 30f, 92f, 14f, 84f), path), t); line(c, darker(col, 0.4f), 1.4f, 4f, 60f, 62f, 6f, 18f, 30f, 18f, 30f) }
                    "butterfly" -> { fill(c, it, Shapes.poly(floatArrayOf(4f, 64f, 20f, 30f, 46f, 24f, 52f, 44f, 32f, 66f), path), t); fill(c, it, Shapes.poly(floatArrayOf(4f, 70f, 32f, 70f, 40f, 92f, 24f, 104f, 8f, 88f), path2), t)
                        dot(c, acc, 34f, 42f, 5f); dot(c, Color.WHITE, 34f, 42f, 2f); dot(c, acc, 24f, 88f, 4f) }
                }
                c.restore()
            }
            "cape" -> fill(c, it, Shapes.sym(floatArrayOf(0f, 44f, 12f, 44f, b.sh + 3f, 54f, b.sh + 10f + flap * 0.4f, 120f, b.sh + 20f + flap, 194f, b.sh + 20f + flap, 194f, 0f, 196f), path), t)
            "herocape" -> fill(c, it, Shapes.sym(floatArrayOf(0f, 44f, 12f, 44f, b.sh + 3f, 54f, b.sh + 8f, 110f, b.sh + 18f + flap * 1.5f, 172f, b.sh + 18f + flap * 1.5f, 172f, 0f, 168f), path), t)
            "backpack" -> fill(c, it, Shapes.sym(floatArrayOf(0f, 54f, b.sh + 4f, 56f, b.sh + 5f, 100f, b.sh + 5f, 100f, 0f, 102f), path), t)
            "jetpack" -> {
                for (s in intArrayOf(-1, 1)) {
                    fill(c, it, Shapes.sym(floatArrayOf(0f, 44f, 6f, 46f, 6f, 100f, 6f, 100f, 0f, 102f), path, s * (b.sh + 3f)), t)
                    val fl = 10f + sin(t * 30f + s) * 4f
                    p.color = -16777216; p.shader = LinearGradient(0f, 102f, 0f, 102f + fl * 2f, intArrayOf(0xFFFFF4A0.toInt(), 0xFFFF8C2E.toInt(), 0x00FF2E2E), null, Shader.TileMode.CLAMP)
                    c.drawPath(Shapes.lines(floatArrayOf(s * (b.sh + 3f) - 4f, 102f, s * (b.sh + 3f) + 4f, 102f, s * (b.sh + 3f), 102f + fl * 2f), path2), p); p.shader = null
                }
            }
            "surfboard" -> { c.save(); c.rotate(12f, 0f, 100f); fill(c, it, Shapes.poly(floatArrayOf(20f, -14f, 34f, 20f, 36f, 100f, 30f, 180f, 20f, 196f, 10f, 180f, 4f, 100f, 6f, 20f), path), t); line(c, acc, 1.6f, 20f, -10f, 20f, 192f); c.restore() }
        }
    }
}

/** Small decorative motifs printed on plain tees. */
object Ui2 {
    fun motif(c: Canvas, x: Float, y: Float, s: Float, col: Int, seed: Int) {
        when (seed % 4) {
            0 -> com.nugggy.runwayrush.core.Ui.star(c, x, y, s, col)
            1 -> com.nugggy.runwayrush.core.Icons.draw(c, "heart", x, y, s * 1.6f, col)
            2 -> com.nugggy.runwayrush.core.Icons.draw(c, "crown", x, y, s * 1.6f, col)
            else -> com.nugggy.runwayrush.core.Icons.draw(c, "music", x, y, s * 1.6f, col)
        }
    }
}
