package com.nugggy.runwayrush.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import java.util.concurrent.ConcurrentHashMap

/** Loads the photos and fabric textures bundled in assets/img and builds procedural patterns. */
class Assets(private val ctx: Context) {
    private val cache = ConcurrentHashMap<String, Bitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val shaders = HashMap<String, BitmapShader>()
    private val src = Rect()
    private val dst = RectF()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    fun bitmap(path: String): Bitmap? {
        cache[path]?.let { return it }
        if (path in missing) return null
        return try {
            ctx.assets.open("img/$path").use { s ->
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                BitmapFactory.decodeStream(s, null, opts)
            }?.also { cache[path] = it }
        } catch (e: Exception) { missing.add(path); null }
    }

    /** Draws an image covering the rect (centre crop). Falls back to a gradient when missing. */
    fun cover(c: Canvas, path: String, r: RectF, alpha: Int = 255, fallbackTop: Int = Pal.PLUM, fallbackBottom: Int = Pal.INK) {
        val b = bitmap(path)
        if (b == null) {
            c.save(); c.clipRect(r); c.translate(r.left, r.top)
            Ui.gradientBg(c, r.width(), r.height(), fallbackTop, fallbackBottom)
            c.restore(); return
        }
        val ir = b.width.toFloat() / b.height
        val rr = r.width() / r.height()
        if (ir > rr) { val sw = (b.height * rr).toInt(); val x = (b.width - sw) / 2; src.set(x, 0, x + sw, b.height) }
        else { val sh = (b.width / rr).toInt(); val y = (b.height - sh) / 2; src.set(0, y, b.width, y + sh) }
        paint.alpha = alpha
        c.drawBitmap(b, src, r, paint)
        paint.alpha = 255
    }

    /** Repeating shader for a fabric texture, or a generated pattern. */
    fun fabricShader(name: String): BitmapShader? {
        shaders[name]?.let { return it }
        val bmp = when (name) {
            "stripes" -> pattern { c, s -> val p = Paint(); p.color = Color.WHITE; c.drawColor(Color.argb(255, 120, 120, 120)); for (i in 0 until 4) c.drawRect(0f, i * s / 4f, s.toFloat(), i * s / 4f + s / 9f, p) }
            "dots" -> pattern { c, s -> val p = Paint(Paint.ANTI_ALIAS_FLAG); c.drawColor(Color.rgb(130, 130, 130)); p.color = Color.WHITE
                for (y in 0 until 4) for (x in 0 until 4) c.drawCircle(x * s / 4f + (if (y % 2 == 0) s / 8f else 0f) + s / 16f, y * s / 4f + s / 8f, s / 22f, p) }
            "check" -> pattern { c, s -> val p = Paint(); c.drawColor(Color.rgb(140, 140, 140)); p.color = Color.argb(140, 255, 255, 255)
                for (i in 0 until 4) { c.drawRect(i * s / 4f, 0f, i * s / 4f + s / 8f, s.toFloat(), p); c.drawRect(0f, i * s / 4f, s.toFloat(), i * s / 4f + s / 8f, p) } }
            "hearts" -> pattern { c, s -> c.drawColor(Color.rgb(140, 140, 140)); for (y in 0 until 3) for (x in 0 until 3) Icons.draw(c, "heart", x * s / 3f + s / 6f + (y % 2) * s / 6f, y * s / 3f + s / 6f, s / 8f, Color.WHITE) }
            "stars" -> pattern { c, s -> c.drawColor(Color.rgb(130, 130, 130)); for (y in 0 until 3) for (x in 0 until 3) Ui.star(c, x * s / 3f + s / 6f + (y % 2) * s / 6f, y * s / 3f + s / 6f, s / 14f, Color.WHITE) }
            "chevron" -> pattern { c, s -> val p = Paint(Paint.ANTI_ALIAS_FLAG); c.drawColor(Color.rgb(130, 130, 130)); p.color = Color.WHITE; p.strokeWidth = s / 14f; p.style = Paint.Style.STROKE
                for (i in -1 until 5) { val y = i * s / 4f; c.drawLine(0f, y + s / 8f, s / 2f, y, p); c.drawLine(s / 2f, y, s.toFloat(), y + s / 8f, p) } }
            else -> bitmap("textures/$name.jpg")?.let { greyed(it) }
        } ?: return null
        val sh = BitmapShader(bmp, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
        shaders[name] = sh
        return sh
    }

    /** Converts a texture to a mid-grey luminance map so it can be tinted with any colour. */
    private fun greyed(b: Bitmap): Bitmap {
        val w = b.width; val h = b.height
        val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
        var sum = 0L
        for (v in px) sum += (Color.red(v) * 30 + Color.green(v) * 59 + Color.blue(v) * 11) / 100
        val mean = (sum / px.size).toInt().coerceAtLeast(1)
        for (i in px.indices) {
            val v = px[i]
            val l = (Color.red(v) * 30 + Color.green(v) * 59 + Color.blue(v) * 11) / 100
            // normalise around a mid-high grey, keep contrast
            val n = (175 + (l - mean) * 1.15f).toInt().coerceIn(30, 255)
            px[i] = Color.rgb(n, n, n)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Some fabrics keep their natural colours rather than being tinted. */
    fun keepsColour(name: String) = name in setOf("leopard", "camo", "snakeskin", "zebra", "tiedye", "floral", "paisley", "tartan", "gold")

    private val natural = HashMap<String, BitmapShader>()
    fun naturalShader(name: String): BitmapShader? {
        natural[name]?.let { return it }
        val b = bitmap("textures/$name.jpg") ?: return null
        return BitmapShader(b, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR).also { natural[name] = it }
    }

    private fun pattern(fn: (Canvas, Int) -> Unit): Bitmap {
        val s = 64
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        fn(Canvas(b), s)
        return b
    }
}
