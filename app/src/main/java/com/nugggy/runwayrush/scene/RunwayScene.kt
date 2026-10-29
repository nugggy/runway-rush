package com.nugggy.runwayrush.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.nugggy.runwayrush.core.Button
import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.core.Icons
import com.nugggy.runwayrush.core.Pal
import com.nugggy.runwayrush.core.Scene
import com.nugggy.runwayrush.core.Sfx
import com.nugggy.runwayrush.core.Track
import com.nugggy.runwayrush.core.Ui
import com.nugggy.runwayrush.render.AvatarRenderer
import com.nugggy.runwayrush.render.Pose
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Shared runway stage drawing. */
object Stage {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun yAt(z: Float) = 650f - z * 330f
    fun scaleAt(z: Float) = 1f - z * 0.62f

    fun draw(c: Canvas, game: Game, w: Float, t: Float, flashes: List<FloatArray>, crowdSeed: Int) {
        Look.photoBg(c, game, "bg/runway.jpg", 190)
        val cx = w / 2
        // runway
        path.reset(); path.moveTo(cx - 90f, 300f); path.lineTo(cx + 90f, 300f); path.lineTo(cx + 380f, 720f); path.lineTo(cx - 380f, 720f); path.close()
        p.color = -16777216; p.shader = LinearGradient(0f, 300f, 0f, 720f, 0xFF3A2450.toInt(), 0xFF120A1C.toInt(), Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.color = -16777216; p.shader = LinearGradient(0f, 300f, 0f, 720f, 0x00FFFFFF, 0x44FFFFFF, Shader.TileMode.CLAMP)
        c.drawRect(cx - 4f, 300f, cx + 4f, 720f, p); p.shader = null
        // edge lights
        for (i in 0 until 14) {
            val z = i / 13f
            val y = yAt(z) + 40f * (1 - z); val half = 90f + (380f - 90f) * (1 - (z))
            val on = ((t * 4f - i * 0.4f) % 3f) < 1f
            p.color = if (on) 0xFFFFE9A8.toInt() else 0x99B08040.toInt()
            val r = 3f + 5f * (1 - z)
            c.drawCircle(cx - half, y, r, p); c.drawCircle(cx + half, y, r, p)
        }
        // backdrop arch
        p.color = -16777216; p.shader = RadialGradient(cx, 300f, 220f, intArrayOf(0x99FF5C9A.toInt(), 0x00FF5C9A), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, 300f, 220f, p); p.shader = null
        Ui.text(c, "RUNWAY", cx, 280f, 34f, 0xCCFFD86B.toInt(), Paint.Align.CENTER, Ui.black)
        // spotlights sweeping
        for (k in 0 until 3) {
            val ang = sin(t * 0.7f + k * 2.1f) * 160f
            path.reset(); path.moveTo(cx + (k - 1) * 260f, 0f); path.lineTo(cx + ang - 70f, 700f); path.lineTo(cx + ang + 70f, 700f); path.close()
            p.color = -16777216; p.shader = LinearGradient(0f, 0f, 0f, 700f, 0x33FFF4E8, 0x00FFF4E8, Shader.TileMode.CLAMP)
            c.drawPath(path, p); p.shader = null
        }
        crowd(c, w, t, crowdSeed)
        for (f in flashes) {
            val a = (f[2] / 0.25f).coerceIn(0f, 1f)
            p.color = -16777216; p.shader = RadialGradient(f[0], f[1], 60f, intArrayOf(Color.argb((a * 255).toInt(), 255, 255, 255), 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            c.drawCircle(f[0], f[1], 60f, p); p.shader = null
        }
    }

    private fun crowd(c: Canvas, w: Float, t: Float, seed: Int) {
        val r = Random(seed)
        val cx = w / 2
        for (row in 0 until 4) {
            val y = 390f + row * 95f
            val inner = 120f + (y - 300f) / 420f * 290f + 40f
            var x = 20f
            while (x < cx - inner) {
                val col = Color.rgb(20 + r.nextInt(40), 10 + r.nextInt(25), 30 + r.nextInt(40))
                val bob = abs(sin(t * 3f + x * 0.05f)) * 4f
                val s = 0.8f + row * 0.18f
                for (side in intArrayOf(-1, 1)) {
                    val px = if (side < 0) x else w - x
                    p.color = col
                    c.drawCircle(px, y - 38f * s - bob, 15f * s, p)
                    c.drawRoundRect(px - 24f * s, y - 22f * s - bob, px + 24f * s, y + 30f * s, 18f * s, 18f * s, p)
                }
                x += 44f + r.nextInt(16)
            }
        }
    }
}

/** Each contestant walks the runway in turn. */
class RunwayScene(game: Game, private val match: Match) : Scene(game) {
    private val order = match.contestants.filter { !it.local }.shuffled(Random(match.seed)) + match.me
    private var idx = 0
    private var t = 0f
    private val walk = 3.4f; private val hold = 1.8f; private val back = 1.3f
    private val pose = Pose()
    private val flashes = ArrayList<FloatArray>()
    private var phaseWalk = 0f
    private var cheered = false
    private val rnd = Random(match.seed)

    override fun enter() {
        match.phase = "runway"
        match.attach(game)
        game.sound.music(Track.RUNWAY)
    }

    override fun layout() {
        ui.clear()
        ui.add(Button("Skip", "play", 0x88201230.toInt(), 24f) { next() }.set(w - 190f, 24f, 166f, 66f))
    }

    private fun next() {
        idx++; t = 0f; cheered = false
        if (idx >= order.size) { idx = order.size - 1; game.go(VoteScene(game, match)) }
        else game.sound.play(Sfx.WHOOSH, 0.6f)
    }

    override fun update(dt: Float) {
        match.hostTick(game, dt)
        t += dt
        if (t > walk + hold + back) { next(); return }
        if (t > walk && !cheered) { cheered = true; game.sound.play(Sfx.CHEER, 0.7f) }
        val flashRate = if (t > walk && t < walk + hold) 10f else 2.5f
        if (rnd.nextFloat() < flashRate * dt) {
            flashes.add(floatArrayOf(40f + rnd.nextFloat() * (w - 80f), 380f + rnd.nextFloat() * 280f, 0.25f))
            if (rnd.nextFloat() < 0.5f) game.sound.play(Sfx.SHUTTER, 0.4f, 0.9f + rnd.nextFloat() * 0.3f)
        }
        val it = flashes.iterator(); while (it.hasNext()) { val f = it.next(); f[2] -= dt; if (f[2] <= 0f) it.remove() }
    }

    override fun draw(c: Canvas) {
        Stage.draw(c, game, w, time, flashes, match.seed.toInt())
        val cur = order[idx]
        val z: Float
        when {
            t < walk -> { z = 1f - t / walk; phaseWalk += 0.18f; pose.walk(phaseWalk, time, cur.id.toFloat()) }
            t < walk + hold -> { z = 0f; pose.strike(time) }
            else -> { z = (t - walk - hold) / back; phaseWalk += 0.18f; pose.walk(phaseWalk, time, cur.id.toFloat()) }
        }
        val alpha = if (t > walk + hold) 1f - z else (t / 0.4f).coerceAtMost(1f)
        val y = Stage.yAt(z); val s = Stage.scaleAt(z)
        c.saveLayerAlpha(0f, 0f, w, h, (alpha * 255).toInt())
        AvatarRenderer.draw(c, w / 2, y, 520f * s, cur.avatar, cur.outfit, pose, time)
        c.restore()
        // caption
        val cap = RectF(30f, h - 180f, 470f, h - 30f)
        Ui.panel(c, cap, 0xDD201230.toInt(), 22f, if (cur.local) Pal.GOLD else 0x55FFFFFF)
        Ui.text(c, if (cur.local) "${cur.name} (you)" else cur.name, cap.left + 24f, cap.top + 50f, 38f, Pal.GOLD, tf = Ui.serif)
        val pieces = cur.outfit.all().sortedBy { it.slot.ordinal }.take(3).joinToString(", ") { it.style.name }
        Ui.wrap(c, if (pieces.isEmpty()) "Brave choice: the basics" else "Wearing $pieces" + if (cur.outfit.items.size > 3) " and more" else "", cap.left + 24f, cap.top + 86f, cap.width() - 40f, 21f, Pal.CREAM)
        Ui.text(c, "Model ${idx + 1} of ${order.size}", w - 40f, h - 40f, 24f, Pal.CREAM, Paint.Align.RIGHT, Ui.regular)
        Ui.text(c, match.theme.name, w / 2, 60f, 36f, Color.WHITE, Paint.Align.CENTER, Ui.serif)
        ui.draw(c)
    }

    override fun back(): Boolean = true
}

/** Everyone votes for their favourite look (not their own). */
class VoteScene(game: Game, private val match: Match) : Scene(game) {
    private val others = match.contestants.filter { !it.local }
    private val poses = others.associate { it.id to Pose() }
    private var voted = -1
    private var timer = 30f

    override fun enter() {
        match.phase = "vote"
        match.attach(game)
        game.sound.music(Track.RUNWAY)
        if (match.net?.isHost == true) match.hostCheckVotes(game)
    }

    override fun layout() {
        ui.clear()
        val n = others.size
        val cw = ((w - 80f) / n).coerceAtMost(260f)
        val x0 = w / 2 - cw * n / 2
        for ((i, c) in others.withIndex()) {
            val b = Button("", null) { cast(c.id) }
            b.drawFn = { cv, btn ->
                val sel = voted == c.id
                Ui.panel(cv, btn.rect, if (sel) 0xEE5A2A6E.toInt() else 0xCC201230.toInt(), 22f, if (sel) Pal.GOLD else 0x44FFFFFF)
                if (btn.pressed) { Ui.p.color = 0x22FFFFFF; cv.drawRoundRect(btn.rect, 22f, 22f, Ui.p) }
                val pose = poses[c.id]!!
                if (sel) pose.strike(time) else pose.idle(time, c.id.toFloat())
                AvatarRenderer.draw(cv, btn.rect.centerX(), btn.rect.bottom - 70f, (btn.rect.height() - 110f).coerceAtMost(cw * 1.8f), c.avatar, c.outfit, pose, time)
                Ui.text(cv, c.name, btn.rect.centerX(), btn.rect.bottom - 26f, 26f, Color.WHITE, Paint.Align.CENTER)
                if (sel) Icons.draw(cv, "heart", btn.rect.right - 30f, btn.rect.top + 30f, 34f, Pal.PINK)
            }
            ui.add(b.set(x0 + i * cw + 6f, 130f, cw - 12f, h - 170f))
        }
    }

    private fun cast(id: Int) {
        if (voted >= 0) return
        voted = id
        game.sound.play(Sfx.VOTE)
        match.vote(game, id)
    }

    override fun update(dt: Float) {
        match.hostTick(game, dt)
        if (voted < 0) {
            timer -= dt
            if (timer <= 0f) cast(others.maxByOrNull { it.score?.total ?: 0 }?.id ?: others.first().id)
        }
    }

    override fun draw(c: Canvas) {
        Stage.draw(c, game, w, time, emptyList(), match.seed.toInt())
        val p = Ui.p; p.shader = null; p.color = 0x88000000.toInt(); c.drawRect(0f, 0f, w, h, p)
        Ui.text(c, if (voted < 0) "Vote for your favourite look" else "Vote cast. Counting...", w / 2, 70f, 46f, Pal.GOLD, Paint.Align.CENTER, Ui.serif)
        Ui.text(c, "Theme: ${match.theme.name}" + if (voted < 0) "   |   ${timer.toInt()}s" else "", w / 2, 112f, 24f, Pal.CREAM, Paint.Align.CENTER, Ui.regular)
        ui.draw(c)
    }

    override fun back(): Boolean = true
}
