package com.nugggy.runwayrush.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.nugggy.runwayrush.core.Assets
import com.nugggy.runwayrush.core.Button
import com.nugggy.runwayrush.model.Avatar
import com.nugggy.runwayrush.model.Item
import com.nugggy.runwayrush.model.Outfit
import com.nugggy.runwayrush.model.Slot
import kotlin.math.sin

class Pose {
    var liftL = 0f
    var liftR = 0f
    var armL = 7f
    var armR = 7f
    var bob = 0f
    var tilt = 0f
    var blink = false
    var smile = 0.5f

    fun idle(t: Float, seed: Float = 0f) {
        liftL = 0f; liftR = 0f
        armL = 7f + sin(t * 1.3f + seed) * 1.5f; armR = 7f + sin(t * 1.3f + seed + 1f) * 1.5f
        bob = sin(t * 2f + seed) * 0.8f; tilt = 0f
        blink = ((t + seed * 3f) % 4.2f) < 0.12f
    }

    fun walk(phase: Float, t: Float, seed: Float = 0f) {
        val s = sin(phase)
        liftL = (s).coerceAtLeast(0f); liftR = (-s).coerceAtLeast(0f)
        armL = 9f + s * 9f; armR = 9f - s * 9f
        bob = -kotlin.math.abs(s) * 3f
        tilt = s * 2f
        blink = ((t + seed * 3f) % 4.2f) < 0.12f
    }

    fun strike(t: Float) {
        liftL = 0f; liftR = 0.15f
        armL = 32f; armR = 6f
        bob = 0f; tilt = -3f; smile = 1f
        blink = (t % 3.7f) < 0.1f
    }
}

/** Body measurements in figure units. The figure is 200 units tall, feet at y = 200. */
class Body(val a: Avatar) {
    val sh: Float; val wa: Float; val hp: Float; val legW: Float; val armW: Float; val bust: Float
    val scale: Float
    val vScale: Float
    init {
        when (a.body) {
            0 -> { sh = 20f; wa = 12.5f; hp = 17f; legW = 8.5f; armW = 5.4f; bust = 0f; scale = 1f }
            2 -> { sh = 21.5f; wa = 14.5f; hp = 23f; legW = 10.5f; armW = 6.2f; bust = 3f; scale = 1f }
            3 -> { sh = 26f; wa = 18.5f; hp = 21f; legW = 10.5f; armW = 7.3f; bust = 0f; scale = 1.03f }
            4 -> { sh = 19f; wa = 12.5f; hp = 17.5f; legW = 8.5f; armW = 5.2f; bust = 1f; scale = 0.9f }
            else -> { sh = 22f; wa = 14f; hp = 19f; legW = 9.5f; armW = 6f; bust = 1f; scale = 1f }
        }
        vScale = 0.92f + a.height * 0.04f
    }
    val neckY = 40f
    val shoulderY = 53f
    val waistY = 95f
    val hipY = 112f
    val crotchY = 124f
    val legTop = 114f
    val legLen = 78f
    val hipX get() = hp * 0.5f
    val armX get() = sh - 3.5f
    val armY = 56f
    val armLen = 64f
    val skin get() = Avatar.SKIN[a.skin.coerceIn(0, Avatar.SKIN.size - 1)]
    val hair get() = Avatar.HAIR[a.hairColour.coerceIn(0, Avatar.HAIR.size - 1)]
}

object AvatarRenderer {
    lateinit var assets: Assets

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    /**
     * Draws a full figure. [x], [feetY] are the screen position of the feet; [height] is the drawn height in pixels.
     */
    fun draw(c: Canvas, x: Float, feetY: Float, height: Float, a: Avatar, outfit: Outfit?, pose: Pose, time: Float,
             mannequin: Boolean = false, shadow: Boolean = true, flip: Boolean = false) {
        val b = Body(a)
        val s = height / 200f * b.scale
        if (shadow) {
            p.shader = null; p.style = Paint.Style.FILL; p.color = 0x40000000
            c.drawOval(x - 28 * s, feetY - 5 * s, x + 28 * s, feetY + 5 * s, p)
        }
        c.save()
        c.translate(x, feetY)
        c.scale(if (flip) -s else s, s * b.vScale)
        c.translate(0f, -200f + pose.bob)
        c.rotate(pose.tilt, 0f, 200f)
        drawFigure(c, b, outfit, pose, time, mannequin)
        c.restore()
    }

    private var ghost = false

    private fun drawFigure(c: Canvas, b: Body, o: Outfit?, pose: Pose, t: Float, mq: Boolean) {
        val top = o?.get(Slot.TOP); val bottom = o?.get(Slot.BOTTOM); val dress = o?.get(Slot.DRESS)
        val outer = o?.get(Slot.OUTER); val shoes = o?.get(Slot.SHOES); val hat = o?.get(Slot.HAT)
        val back = o?.get(Slot.BACK)
        val skin = if (mq) 0xFFE9E3EE.toInt() else b.skin
        val bigHat = hat != null && hat.style.key in setOf("helmet")

        // 1. behind the body
        back?.let { Garments.back(c, b, it, t, behind = true) }
        if (!mq && !bigHat) Hair.back(c, b, t)

        // 2. legs
        val g = Garments
        val tail = bottom?.let { Garments.key(it) } == "mermaidtail"
        for (side in intArrayOf(-1, 1)) {
            val lift = if (side < 0) pose.liftL else pose.liftR
            c.save()
            c.translate(side * b.hipX, b.legTop - lift * 7f)
            if (!tail) {
                if (!ghost) limb(c, b.legW, b.legW * 0.55f, b.legLen, skin, mq)
                // foot
                if (shoes == null && !ghost) { p.shader = null; p.color = darker(skin, 0.06f); c.drawOval(-5f, b.legLen - 2f, 5f + side * 2f, b.legLen + 7f, p) }
                val wide = bottom?.let { Garments.key(it) } in setOf("wide", "flares", "culottes") || dress?.let { Garments.key(it) } in setOf("tux", "suit")
                if (!wide) shoes?.let { g.shoe(c, b, it, side, t) }
                g.legs(c, b, bottom, dress, side, t, mq)
                if (wide) shoes?.let { g.shoe(c, b, it, side, t) }
                if (bottom == null && dress == null && !mq) g.baseShorts(c, b, side)
            }
            c.restore()
        }

        // 3. torso
        if (!ghost) torso(c, b, skin, mq)
        if (!mq) {
            if (top == null && dress == null) g.baseTop(c, b)
            if (bottom == null && dress == null) g.baseShortsTorso(c, b)
        }
        bottom?.let { g.bottomTorso(c, b, it, t) }
        top?.let { g.topTorso(c, b, it, t, tucked = bottom != null && Garments.key(bottom) in setOf("pencil", "wide", "culottes", "pleated")) }
        dress?.let { g.dressTorso(c, b, it, t) }
        o?.get(Slot.NECK)?.let { if (it.style.key !in Garments.OVER_NECK) g.neck(c, b, it, t) }
        outer?.let { g.outerTorso(c, b, it, t) }
        o?.get(Slot.NECK)?.let { if (it.style.key in Garments.OVER_NECK) g.neck(c, b, it, t) }
        back?.let { Garments.back(c, b, it, t, behind = false) }

        // 4. arms
        for (side in intArrayOf(-1, 1)) {
            val ang = if (side < 0) pose.armL else -pose.armR
            c.save()
            c.translate(side * b.armX, b.armY)
            c.rotate(ang)
            if (!ghost) {
            limb(c, b.armW, b.armW * 0.72f, b.armLen, skin, mq)
            p.shader = null; p.color = skin
            c.drawCircle(0f, b.armLen + 3.5f, b.armW * 0.82f, p)
            if (!mq) { p.color = darker(skin, 0.15f); p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; c.drawCircle(0f, b.armLen + 3.5f, b.armW * 0.82f, p); p.style = Paint.Style.FILL }
            }
            if (dress == null && top == null && !mq) g.baseSleeve(c, b)
            top?.let { g.sleeve(c, b, it, t) }
            dress?.let { g.sleeve(c, b, it, t) }
            outer?.let { g.sleeve(c, b, it, t) }
            if (side > 0) o?.get(Slot.BAG)?.let { g.handItem(c, b, it, t) }
            c.restore()
        }

        // 5. head
        if (!ghost) head(c, b, skin, mq, pose, t, o) else headItems(c, b, o, t)
        if (dress != null && Garments.key(dress) == "ghost") g.dressTorso(c, b, dress, t)
    }

    private fun headItems(c: Canvas, b: Body, o: Outfit?, t: Float) {
        o?.get(Slot.EARS)?.let { Garments.ears(c, b, it, t) }
        o?.get(Slot.GLASSES)?.let { Garments.glasses(c, b, it, t) }
        o?.get(Slot.HAT)?.let { Garments.hat(c, b, it, t) }
    }

    private fun limb(c: Canvas, w0: Float, w1: Float, len: Float, col: Int, mq: Boolean) {
        Shapes.sym(floatArrayOf(0f, -4f, w0 * 0.85f, -3f, w0, 4f, w0 * 0.92f, len * 0.45f, w1, len, w1, len, 0f, len + 1f), path)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.color = -16777216; p.shader = LinearGradient(-w0, 0f, w0, 0f, intArrayOf(darker(col, 0.12f), col, col, darker(col, 0.16f)), floatArrayOf(0f, 0.35f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        if (mq) return
        p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; p.color = darker(col, 0.2f)
        c.drawPath(path, p); p.style = Paint.Style.FILL
    }

    private fun torso(c: Canvas, b: Body, col: Int, mq: Boolean) {
        // neck
        p.shader = null; p.style = Paint.Style.FILL; p.color = darker(col, 0.1f)
        c.drawRect(-5.5f, 34f, 5.5f, 52f, p)
        Shapes.sym(floatArrayOf(0f, 47f, 6f, 46f, b.sh * 0.65f, 49f, b.sh, 52f, b.sh, 60f, b.sh - 2f, 72f,
            b.wa + b.bust * 0.3f, 80f, b.wa, b.waistY, b.hp, 106f, b.hp, b.hipY, b.hp - 1f, b.crotchY, 0f, b.crotchY), path)
        p.color = Color.BLACK
        p.color = -16777216; p.shader = LinearGradient(-b.hp, 0f, b.hp, 0f, intArrayOf(darker(col, 0.12f), col, col, darker(col, 0.18f)), floatArrayOf(0f, 0.3f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        if (!mq) { p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; p.color = darker(col, 0.22f); c.drawPath(path, p); p.style = Paint.Style.FILL }
    }

    private fun head(c: Canvas, b: Body, col: Int, mq: Boolean, pose: Pose, t: Float, o: Outfit?) {
        val a = b.a
        // ears
        p.shader = null; p.style = Paint.Style.FILL; p.color = darker(col, 0.05f)
        c.drawOval(-18f, 18f, -12f, 30f, p); c.drawOval(12f, 18f, 18f, 30f, p)
        // head shape
        Shapes.sym(floatArrayOf(0f, 2f, 9f, 3f, 15.5f, 11f, 16f, 22f, 14f, 32f, 8f, 40f, 0f, 42f), path)
        p.color = Color.BLACK
        p.color = -16777216; p.shader = RadialGradient(-3f, 18f, 24f, intArrayOf(lighter(col, 0.06f), col, darker(col, 0.14f)), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(path, p); p.shader = null
        if (!mq) { p.style = Paint.Style.STROKE; p.strokeWidth = 0.8f; p.color = darker(col, 0.22f); c.drawPath(path, p); p.style = Paint.Style.FILL }
        o?.get(Slot.EARS)?.let { Garments.ears(c, b, it, t) }
        if (mq) { o?.get(Slot.GLASSES)?.let { Garments.glasses(c, b, it, t) }; o?.get(Slot.HAT)?.let { Garments.hat(c, b, it, t) }; return }

        Face.draw(c, b, pose, t)
        val hat = o?.get(Slot.HAT)
        if (hat?.style?.key != "helmet") Hair.front(c, b, t, hat != null)
        o?.get(Slot.GLASSES)?.let { Garments.glasses(c, b, it, t) }
        hat?.let { Garments.hat(c, b, it, t) }
    }

    // ------------------------------------------------------------------ icons

    private val iconPose = Pose().apply { armL = 14f; armR = 14f }
    private val iconOutfit = Outfit()
    private val mannequin = Avatar(body = 1, height = 2)

    /** Draws an item on a faint mannequin, framed to the part of the body it belongs to. */
    fun icon(c: Canvas, item: Item, r: RectF, t: Float, body: Boolean = true) {
        ghost = !body
        try { iconInner(c, item, r, t) } finally { ghost = false }
    }

    /** Renders an item to a small bitmap (used for rack displays). */
    private val thumbCache = HashMap<Long, android.graphics.Bitmap>()
    fun thumb(item: Item, w: Int, h: Int, body: Boolean = false): android.graphics.Bitmap {
        val key = item.id.toLong() * 1_000_000L + w * 1000L + h + if (body) 500_000_000_000L else 0L
        return thumbCache.getOrPut(key) {
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            icon(Canvas(bmp), item, RectF(0f, 0f, w.toFloat(), h.toFloat()), 0.3f, body)
            bmp
        }
    }

    private fun iconInner(c: Canvas, item: Item, r: RectF, t: Float) {
        iconOutfit.clear(); iconOutfit.wear(item)
        // region of the 200-unit figure to show: top, bottom (y) and half width
        val (y0, y1) = when (item.slot) {
            Slot.HAT -> -22f to 46f
            Slot.GLASSES -> 6f to 38f
            Slot.EARS -> 10f to 44f
            Slot.NECK -> 30f to 110f
            Slot.TOP -> 34f to 128f
            Slot.OUTER -> 34f to 175f
            Slot.BOTTOM -> 88f to 202f
            Slot.DRESS -> 30f to 205f
            Slot.SHOES -> 150f to 203f
            Slot.BAG -> 70f to 175f
            Slot.BACK -> -10f to 200f
        }
        val span = y1 - y0
        val sc = (r.height() / span).coerceAtMost(r.width() / 70f)
        c.save()
        c.clipRect(r)
        c.translate(r.centerX(), r.centerY())
        c.scale(sc, sc)
        c.translate(if (item.slot == Slot.BAG) -24f else 0f, -(y0 + y1) / 2)
        drawFigure(c, Body(mannequin), iconOutfit, iconPose, t, mq = true)
        c.restore()
    }

    fun darker(c: Int, f: Float) = Button.darken(c, f)
    fun lighter(c: Int, f: Float) = Button.lighten(c, f)
    fun withAlpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
}
