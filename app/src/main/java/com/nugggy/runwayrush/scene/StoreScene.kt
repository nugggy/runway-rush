package com.nugggy.runwayrush.scene

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.nugggy.runwayrush.core.Button
import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.core.Icons
import com.nugggy.runwayrush.core.Pal
import com.nugggy.runwayrush.core.Scene
import com.nugggy.runwayrush.core.Sfx
import com.nugggy.runwayrush.core.Touch
import com.nugggy.runwayrush.core.TouchType
import com.nugggy.runwayrush.core.Track
import com.nugggy.runwayrush.core.Ui
import com.nugggy.runwayrush.model.Catalog
import com.nugggy.runwayrush.model.Item
import com.nugggy.runwayrush.model.Scoring
import com.nugggy.runwayrush.model.Slot
import com.nugggy.runwayrush.model.Style
import com.nugggy.runwayrush.render.AvatarRenderer
import com.nugggy.runwayrush.render.Pose
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** A rail, shelf or stand holding every variant of one style. */
class Fixture(val x: Float, val y: Float, val style: Style, val items: List<Item>, val zone: Zone) {
    val w = when (style.slot) { Slot.SHOES, Slot.HAT -> 150f; Slot.GLASSES, Slot.EARS, Slot.NECK -> 130f; Slot.BACK -> 120f; else -> 170f }
    val foot = RectF(x - w / 2, y - 30f, x + w / 2, y + 6f)
    var cache: Bitmap? = null
}

class Zone(val name: String, val slots: List<Slot>, val rect: RectF, val colour: Int)

class Shopper(val c: Contestant) {
    var x = 0f; var y = 0f; var tx = 0f; var ty = 0f; var wait = 0f; var phase = 0f; var moving = false; var flip = false
    val pose = Pose()
}

class StoreScene(game: Game, private val match: Match) : Scene(game) {
    companion object {
        const val WW = 4600f
        const val WH = 3200f
        const val REACH = 170f
    }

    private val zones = ArrayList<Zone>()
    private val fixtures = ArrayList<Fixture>()
    private val shoppers = ArrayList<Shopper>()
    private val me = match.me
    private var px = 0f; private var py = 0f
    private var camX = 0f; private var camY = 0f
    private val zoom = 0.82f
    private var walkPhase = 0f
    private var moving = false
    private var flip = false
    private val pose = Pose()
    private var target: Fixture? = null
    private var moveTo: Pair<Float, Float>? = null
    private var remaining = match.duration
    private var finished = false
    private var near: Fixture? = null

    // joystick
    private var joyId = -1
    private var joyX = 0f; private var joyY = 0f; private var joyDX = 0f; private var joyDY = 0f
    private var tapId = -1; private var tapX = 0f; private var tapY = 0f; private var tapT = 0f

    // panels
    private var browsing: Fixture? = null
    private var showOutfit = false
    private var scroll = 0f
    private var scrollMax = 0f
    private var dragId = -1
    private var dragY = 0f
    private var dragMoved = 0f
    private val panel = RectF()
    private val cards = ArrayList<Pair<RectF, Item>>()
    private val minimap = RectF()
    private var netT = 0f
    private var outfitT = 0f
    private var tip = ""
    private var tipT = 0f
    private val particles = ArrayList<FloatArray>()

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()
    private var floorShader: BitmapShader? = null
    private var marbleShader: BitmapShader? = null
    private val m = Matrix()

    private lateinit var browseBtn: Button
    private lateinit var finishBtn: Button
    private lateinit var outfitBtn: Button
    private lateinit var closeBtn: Button

    override fun enter() {
        match.phase = "shop"
        match.attach(game)
        game.sound.music(Track.SHOP)
        build()
        px = WW - 560f; py = WH - 260f
        camX = px; camY = py
        val rnd = Random(match.seed)
        for (c in match.contestants) if (!c.local) {
            val s = Shopper(c)
            s.x = WW - 700f + rnd.nextFloat() * 400f; s.y = WH - 300f + rnd.nextFloat() * 120f
            s.tx = s.x; s.ty = s.y; s.wait = rnd.nextFloat() * 2f
            if (!c.ai) { c.x = s.x; c.y = s.y }
            shoppers.add(s)
        }
        game.assets.bitmap("bg/floor_wood.jpg")?.let { floorShader = BitmapShader(it, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
        game.assets.bitmap("bg/floor_marble.jpg")?.let { marbleShader = BitmapShader(it, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR) }
        val elapsed = (System.currentTimeMillis() - match.started) / 1000f
        if (match.multiplayer) remaining = (match.duration - elapsed).coerceIn(30f, match.duration)
        showTip("Find pieces that fit: ${match.theme.name}")
    }

    private fun build() {
        val cw = WW / 4f; val ch = (WH - 200f) / 3f
        fun zr(col: Int, row: Int, span: Int = 1) = RectF(col * cw + 30f, 100f + row * ch + 30f, (col + span) * cw - 30f, 100f + (row + 1) * ch - 30f)
        zones += Zone("Tops", listOf(Slot.TOP), zr(0, 0), 0xFFFF9EC7.toInt())
        zones += Zone("Dresses & Suits", listOf(Slot.DRESS), zr(1, 0, 2), 0xFFC9AEF0.toInt())
        zones += Zone("Outerwear", listOf(Slot.OUTER), zr(3, 0), 0xFF8FCBF2.toInt())
        zones += Zone("Bottoms", listOf(Slot.BOTTOM), zr(0, 1), 0xFFA8EBCF.toInt())
        zones += Zone("Shoes", listOf(Slot.SHOES), zr(1, 1), 0xFFFFD86B.toInt())
        zones += Zone("Hats", listOf(Slot.HAT), zr(2, 1), 0xFFFF8A5C.toInt())
        zones += Zone("Wings & Capes", listOf(Slot.BACK), zr(3, 1), 0xFFB6E3C6.toInt())
        zones += Zone("Eyewear & Earrings", listOf(Slot.GLASSES, Slot.EARS), zr(0, 2), 0xFF6BC5FF.toInt())
        zones += Zone("Necklaces & Bags", listOf(Slot.NECK, Slot.BAG), zr(1, 2), 0xFFF4C6C4.toInt())
        zones += Zone("Fitting Rooms", emptyList(), zr(2, 2, 2), 0xFFFFF4E8.toInt())
        for (z in zones) {
            val styles = Catalog.STYLES.filter { it.slot in z.slots }
            if (styles.isEmpty()) continue
            val n = styles.size
            val aspect = z.rect.width() / z.rect.height()
            val cols = ceil(sqrt(n * aspect)).toInt().coerceAtLeast(1)
            val rows = ceil(n / cols.toFloat()).toInt()
            val gx = (z.rect.width() - 120f) / cols; val gy = (z.rect.height() - 160f) / rows
            for ((i, st) in styles.withIndex()) {
                val cx = z.rect.left + 60f + gx * (i % cols + 0.5f)
                val cy = z.rect.top + 150f + gy * (i / cols + 0.5f) + 30f
                fixtures += Fixture(cx, cy, st, Catalog.byStyle[st.key] ?: emptyList(), z)
            }
        }
    }

    override fun layout() {
        ui.clear()
        browseBtn = ui.add(Button("Browse", "hanger", Pal.BERRY, 30f) { near?.let { openRack(it) } }.set(w / 2 - 140f, h - 120f, 280f, 84f))
        finishBtn = ui.add(Button("Hit the runway", "flag", 0xFF1E9E9A.toInt(), 26f) { finish() }.set(w - 300f, h - 104f, 280f, 80f))
        outfitBtn = ui.add(Button("My outfit", "shirt", 0xFF5A3FC0.toInt(), 26f) { showOutfit = true; browsing = null; scroll = 0f }.set(w - 300f, h - 196f, 280f, 76f))
        closeBtn = ui.add(Button("", "close", 0xFF4A4054.toInt()) { closePanels() }.set(w - 96f, 26f, 70f, 70f))
        minimap.set(w - 250f, 20f, w - 20f, 20f + 230f * (WH / WW))
    }

    private fun closePanels() { browsing = null; showOutfit = false }

    private fun openRack(f: Fixture) {
        browsing = f; showOutfit = false; scroll = 0f
        game.sound.play(Sfx.WHOOSH, 0.5f)
        moving = false; joyId = -1
    }

    private fun finish() {
        if (finished) return
        finished = true
        closePanels()
        game.sound.play(Sfx.GO)
        match.finishShopping(game)
    }

    // ---------------------------------------------------------------- update

    override fun update(dt: Float) {
        match.hostTick(game, dt)
        if (!finished) {
            remaining -= dt
            if (remaining <= 0f) { remaining = 0f; game.toast("Time's up"); finish() }
            val sec = remaining.toInt()
            if (remaining < 10f && (remaining + dt).toInt() != sec) game.sound.play(Sfx.BEEP)
        }
        tipT -= dt
        val panelOpen = browsing != null || showOutfit
        // movement
        var vx = 0f; var vy = 0f
        val speed = 520f
        if (!panelOpen && !finished) {
            if (joyId >= 0) {
                val d = hypot(joyDX, joyDY)
                if (d > 8f) { vx = joyDX / d * speed * min(1f, d / 70f); vy = joyDY / d * speed * min(1f, d / 70f) }
                moveTo = null
            } else moveTo?.let { (tx, ty) ->
                val dx = tx - px; val dy = ty - py; val d = hypot(dx, dy)
                if (d < 14f) { moveTo = null; target?.let { if (dist(it) < REACH) openRack(it) }; target = null }
                else { vx = dx / d * speed; vy = dy / d * speed }
            }
        }
        moving = abs(vx) + abs(vy) > 1f
        if (moving) {
            val nx = (px + vx * dt).coerceIn(60f, WW - 60f)
            val ny = (py + vy * dt).coerceIn(160f, WH - 40f)
            px = nx; py = ny
            if (joyId >= 0) resolve()
            if (abs(vx) > 5f) flip = vx < 0
            walkPhase += dt * 11f
            pose.walk(walkPhase, time)
        } else pose.idle(time)

        camX += (px - camX) * min(1f, dt * 6f)
        camY += (py - 60f - camY) * min(1f, dt * 6f)
        val halfW = w / 2 / zoom; val halfH = h / 2 / zoom
        camX = camX.coerceIn(halfW, WW - halfW); camY = camY.coerceIn(halfH, WH - halfH)

        near = fixtures.filter { dist(it) < REACH }.minByOrNull { dist(it) }
        browseBtn.visible = near != null && !panelOpen && !finished
        browseBtn.label = near?.let { "Browse ${it.style.name}s".let { s -> if (s.length > 22) "Browse" else s } } ?: "Browse"
        closeBtn.visible = panelOpen
        finishBtn.visible = !panelOpen && !finished
        outfitBtn.visible = !panelOpen && !finished

        updateShoppers(dt)
        // network
        if (match.multiplayer && !finished) {
            netT += dt; outfitT += dt
            if (netT > 0.12f) {
                netT = 0f
                val msg = JSONObject().put("t", "pos").put("x", px.toDouble()).put("y", py.toDouble()).put("mv", moving).put("f", flip)
                if (outfitT > 1f) { outfitT = 0f; msg.put("o", me.outfit.toJson()) }
                if (match.net?.isHost == true) msg.put("id", me.id)
                match.net?.send(msg)
            }
        }
        val it = particles.iterator()
        while (it.hasNext()) { val q = it.next(); q[0] += q[2] * dt; q[1] += q[3] * dt; q[3] += 300f * dt; q[4] -= dt; if (q[4] <= 0) it.remove() }
    }

    private fun dist(f: Fixture) = hypot(f.x - px, f.y - 20f - py)

    private fun resolve() {
        for (f in fixtures) {
            val fr = f.foot
            if (px > fr.left - 14f && px < fr.right + 14f && py > fr.top - 6f && py < fr.bottom + 6f) {
                val dl = px - (fr.left - 14f); val dr = fr.right + 14f - px; val dtp = py - (fr.top - 6f); val db = fr.bottom + 6f - py
                when (minOf(minOf(dl, dr), minOf(dtp, db))) { dl -> px = fr.left - 14f; dr -> px = fr.right + 14f; dtp -> py = fr.top - 6f; else -> py = fr.bottom + 6f }
            }
        }
    }

    private fun updateShoppers(dt: Float) {
        val rnd = Random((time * 1000).toInt())
        for (s in shoppers) {
            val c = s.c
            if (!c.ai) {
                // remote human: ease towards last known position
                s.x += (c.x - s.x) * min(1f, dt * 8f); s.y += (c.y - s.y) * min(1f, dt * 8f)
                s.moving = c.moving; s.flip = c.flip
            } else {
                if (s.wait > 0f) { s.wait -= dt; s.moving = false }
                else {
                    val dx = s.tx - s.x; val dy = s.ty - s.y; val d = hypot(dx, dy)
                    if (d < 10f) {
                        s.wait = 1.5f + rnd.nextFloat() * 3f
                        val f = fixtures[rnd.nextInt(fixtures.size)]
                        s.tx = f.x + (rnd.nextFloat() - 0.5f) * 80f; s.ty = f.y + 50f + rnd.nextFloat() * 30f
                        // AI puts on the next piece of its planned look while browsing
                        val next = match.planFor(c).all().firstOrNull { !c.outfit.isWearing(it) }
                        if (next != null && rnd.nextFloat() < 0.6f) c.outfit.wear(next)
                    } else { s.x += dx / d * 380f * dt; s.y += dy / d * 380f * dt; s.moving = true; s.flip = dx < 0 }
                }
            }
            if (s.moving) { s.phase += dt * 10f; s.pose.walk(s.phase, time, c.id.toFloat()) } else s.pose.idle(time, c.id.toFloat())
        }
    }

    private fun showTip(s: String) { tip = s; tipT = 4f }

    // ---------------------------------------------------------------- touch

    override fun touch(t: Touch): Boolean {
        val panelOpen = browsing != null || showOutfit
        if (panelOpen) {
            if (ui.touch(t)) return true
            return panelTouch(t)
        }
        if (ui.touch(t)) return true
        when (t.type) {
            TouchType.DOWN -> {
                if (minimap.contains(t.x, t.y)) {
                    val wx = (t.x - minimap.left) / minimap.width() * WW; val wy = (t.y - minimap.top) / minimap.height() * WH
                    moveTo = wx to wy; target = null; return true
                }
                if (t.x < w * 0.42f && joyId < 0) { joyId = t.id; joyX = t.x; joyY = t.y; joyDX = 0f; joyDY = 0f; return true }
                tapId = t.id; tapX = t.x; tapY = t.y; tapT = time
            }
            TouchType.MOVE -> if (t.id == joyId) {
                joyDX = t.x - joyX; joyDY = t.y - joyY
                val d = hypot(joyDX, joyDY)
                if (d > 90f) { joyX += joyDX / d * (d - 90f); joyY += joyDY / d * (d - 90f); joyDX = t.x - joyX; joyDY = t.y - joyY }
            }
            TouchType.UP -> {
                if (t.id == joyId) { joyId = -1; joyDX = 0f; joyDY = 0f }
                else if (t.id == tapId) {
                    tapId = -1
                    if (hypot(t.x - tapX, t.y - tapY) < 30f) {
                        val wx = camX + (t.x - w / 2) / zoom; val wy = camY + (t.y - h / 2) / zoom
                        val hit = fixtures.firstOrNull { wx > it.x - it.w / 2 - 10f && wx < it.x + it.w / 2 + 10f && wy > it.y - 150f && wy < it.y + 20f }
                        if (hit != null) {
                            if (dist(hit) < REACH) openRack(hit) else { target = hit; moveTo = hit.x to hit.y + 50f }
                        } else { target = null; moveTo = wx to wy }
                    }
                }
            }
        }
        return true
    }

    private fun panelTouch(t: Touch): Boolean {
        when (t.type) {
            TouchType.DOWN -> if (panel.contains(t.x, t.y)) { dragId = t.id; dragY = t.y; dragMoved = 0f }
            TouchType.MOVE -> if (t.id == dragId) { val d = t.y - dragY; dragY = t.y; dragMoved += abs(d); scroll = (scroll - d).coerceIn(0f, scrollMax) }
            TouchType.UP -> if (t.id == dragId) {
                dragId = -1
                if (dragMoved < 16f) {
                    val hit = cards.firstOrNull { it.first.contains(t.x, t.y) }
                    if (hit != null) toggle(hit.second, hit.first)
                }
            }
        }
        return true
    }

    private fun toggle(item: Item, at: RectF) {
        val o = me.outfit
        if (o.isWearing(item)) { o.remove(item.slot); game.sound.play(Sfx.DROP) }
        else {
            o.wear(item); game.sound.play(Sfx.PICK)
            val rnd = Random(item.id)
            repeat(14) { particles.add(floatArrayOf(at.centerX(), at.centerY(), (rnd.nextFloat() - 0.5f) * 500f, -200f - rnd.nextFloat() * 300f, 0.8f, rnd.nextInt(4).toFloat())) }
            val fit = Scoring.itemFit(item, match.theme)
            if (fit >= 6f) showTip("The judges will love that for ${match.theme.name}") else if (fit < 1f && item.slot in setOf(Slot.TOP, Slot.BOTTOM, Slot.DRESS)) showTip("Hmm, is that really on theme?")
        }
    }

    // ---------------------------------------------------------------- draw

    override fun draw(c: Canvas) {
        c.save()
        c.translate(w / 2, h / 2); c.scale(zoom, zoom); c.translate(-camX, -camY)
        drawWorld(c)
        c.restore()
        if (browsing == null && !showOutfit) drawHud(c)
        if (browsing != null || showOutfit) drawPanel(c)
        ui.draw(c)
        for (q in particles) Ui.star(c, q[0], q[1], 6f, intArrayOf(Pal.GOLD, Pal.PINK, Pal.MINT, Pal.SKY)[q[5].toInt()])
        if (finished && match.multiplayer) drawWaiting(c)
    }

    private fun drawWorld(c: Canvas) {
        val vl = camX - w / 2 / zoom - 50f; val vr = camX + w / 2 / zoom + 50f
        val vt = camY - h / 2 / zoom - 200f; val vb = camY + h / 2 / zoom + 200f
        // floor
        p.color = Color.BLACK; p.shader = floorShader; if (floorShader == null) p.color = 0xFFC9A27A.toInt()
        floorShader?.let { m.setScale(0.9f, 0.9f); it.setLocalMatrix(m) }
        c.drawRect(0f, 0f, WW, WH, p); p.shader = null
        // zones
        for (z in zones) {
            if (z.slots.isEmpty() && marbleShader != null) { p.color = Color.BLACK; p.shader = marbleShader; c.drawRoundRect(z.rect, 40f, 40f, p); p.shader = null }
            else { p.color = AvatarRenderer.withAlpha(z.colour, 120); c.drawRoundRect(z.rect, 40f, 40f, p) }
            p.style = Paint.Style.STROKE; p.strokeWidth = 8f; p.color = AvatarRenderer.withAlpha(z.colour, 230); c.drawRoundRect(z.rect, 40f, 40f, p); p.style = Paint.Style.FILL
            // department sign
            if (z.rect.right > vl && z.rect.left < vr && z.rect.bottom > vt && z.rect.top < vb) {
                val tw = Ui.measure(z.name, 46f, Ui.black) + 70f
                r.set(z.rect.centerX() - tw / 2, z.rect.top + 24f, z.rect.centerX() + tw / 2, z.rect.top + 100f)
                Ui.panel(c, r, 0xEE2E1A47.toInt(), 38f, z.colour)
                Ui.text(c, z.name, r.centerX(), r.centerY() + 16f, 46f, Color.WHITE, Paint.Align.CENTER, Ui.black)
            }
        }
        // back wall
        p.color = -16777216; p.shader = LinearGradient(0f, 0f, 0f, 110f, 0xFF4B2A5E.toInt(), 0xFF2E1A47.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, WW, 110f, p); p.shader = null
        for (i in 0 until 23) { val x = 100f + i * 200f; Look.spotlight(c, x, 110f, 90f, 0x66FFF4C0); p.color = 0xFFFFF4C0.toInt(); c.drawRect(x - 30f, 96f, x + 30f, 104f, p) }
        drawFittingRoom(c)

        // y-sorted fixtures and people
        data class D(val y: Float, val fn: () -> Unit)
        val list = ArrayList<D>()
        for (f in fixtures) if (f.x > vl - 100f && f.x < vr + 100f && f.y > vt && f.y < vb + 100f) list.add(D(f.y) { drawFixture(c, f) })
        for (s in shoppers) if (s.x > vl && s.x < vr && s.y > vt && s.y < vb + 100f) list.add(D(s.y) {
            AvatarRenderer.draw(c, s.x, s.y, 175f, s.c.avatar, s.c.outfit, s.pose, time, flip = s.flip)
            nameTag(c, s.x, s.y - 205f, s.c.name, if (s.c.ai) 0xCC2E1A47.toInt() else 0xCC5A3FC0.toInt())
        })
        list.add(D(py) {
            if (moveTo != null) { p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = 0x99FFD86B.toInt(); c.drawCircle(moveTo!!.first, moveTo!!.second, 26f + sin(time * 8f) * 4f, p); p.style = Paint.Style.FILL }
            Look.spotlight(c, px, py - 40f, 120f, 0x40FFF4E8)
            AvatarRenderer.draw(c, px, py, 180f, me.avatar, me.outfit, pose, time, flip = flip)
            nameTag(c, px, py - 210f, "You", Pal.BERRY)
        })
        list.sortBy { it.y }
        for (d in list) d.fn()
        // highlight nearest
        near?.let { f -> p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = AvatarRenderer.withAlpha(Pal.GOLD, (180 + 60 * sin(time * 6f)).toInt()); c.drawRoundRect(f.x - f.w / 2 - 12f, f.y - 160f, f.x + f.w / 2 + 12f, f.y + 18f, 20f, 20f, p); p.style = Paint.Style.FILL }
    }

    private fun drawFittingRoom(c: Canvas) {
        val z = zones.last()
        // curtains (fitting booths)
        for (i in 0 until 4) {
            val x = z.rect.left + 120f + i * 220f; val y = z.rect.top + 140f
            r.set(x, y, x + 180f, y + 230f)
            p.color = 0xFF2E1A47.toInt(); c.drawRect(r, p)
            p.color = -16777216; p.shader = LinearGradient(x, 0f, x + 30f, 0f, intArrayOf(0xFFB0306B.toInt(), 0xFF7A1E4A.toInt()), null, Shader.TileMode.MIRROR)
            c.drawRect(x + 6f, y + 10f, x + 174f, y + 230f, p); p.shader = null
            p.color = 0xFFE3B341.toInt(); c.drawRect(x - 6f, y, x + 186f, y + 10f, p)
            Ui.star(c, x + 90f, y + 120f, 16f, 0x66FFD86B)
        }
        // big mirror with live reflection of the player
        val mx = z.rect.right - 420f; val my = z.rect.top + 130f
        r.set(mx, my, mx + 300f, my + 420f)
        p.color = 0xFFE3B341.toInt(); c.drawRoundRect(r.left - 14f, r.top - 14f, r.right + 14f, r.bottom + 14f, 160f, 160f, p)
        p.color = -16777216; p.shader = LinearGradient(r.left, r.top, r.right, r.bottom, intArrayOf(0xFFBFD8E8.toInt(), 0xFFE8F2F8.toInt(), 0xFF9FB8CC.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, 150f, 150f, p); p.shader = null
        c.save(); c.clipRect(r)
        AvatarRenderer.draw(c, r.centerX(), r.bottom - 30f, 330f, me.avatar, me.outfit, pose, time, shadow = false)
        p.color = 0x30FFFFFF; c.drawRect(r.left + 30f, r.top, r.left + 70f, r.bottom, p)
        c.restore()
        Ui.text(c, "Mirror", r.centerX(), r.bottom + 50f, 34f, Pal.INK, Paint.Align.CENTER, Ui.black, false)
        // runway exit
        val ex = z.rect.left + 180f; val ey = z.rect.bottom - 200f
        r.set(ex, ey, ex + 520f, ey + 130f)
        Ui.panel(c, r, 0xFF1E9E9A.toInt(), 30f, Pal.GOLD)
        Ui.text(c, "Runway this way", r.centerX(), r.centerY() + 14f, 40f, Color.WHITE, Paint.Align.CENTER, Ui.black)
    }

    private fun drawFixture(c: Canvas, f: Fixture) {
        val bmpW = (f.w + 30f).toInt(); val bmpH = 170
        var bmp = f.cache
        if (bmp == null) { bmp = renderFixture(f, bmpW, bmpH); f.cache = bmp }
        p.color = 0x40000000; c.drawOval(f.x - f.w / 2, f.y - 8f, f.x + f.w / 2, f.y + 12f, p)
        c.drawBitmap(bmp, f.x - bmpW / 2f, f.y - bmpH + 10f, null)
        // label
        val label = f.style.name
        val tw = Ui.measure(label, 22f) + 24f
        r.set(f.x - tw / 2, f.y + 14f, f.x + tw / 2, f.y + 46f)
        Ui.pill(c, r, 0xDD2E1A47.toInt())
        Ui.text(c, label, f.x, f.y + 38f, 22f, Color.WHITE, Paint.Align.CENTER, shadow = false)
    }

    /** Pre-renders a rack with a few of its pieces so drawing the shop each frame stays cheap. */
    private fun renderFixture(f: Fixture, bw: Int, bh: Int): Bitmap {
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = bw / 2f
        val pp = Paint(Paint.ANTI_ALIAS_FLAG)
        val chrome = LinearGradient(0f, 0f, 0f, 8f, intArrayOf(0xFFF2F2F7.toInt(), 0xFF8A8A96.toInt()), null, Shader.TileMode.CLAMP)
        val show = f.items.take(when (f.style.slot) { Slot.SHOES, Slot.HAT -> 3; Slot.GLASSES, Slot.EARS, Slot.NECK -> 3; Slot.BACK -> 1; else -> 5 })
        when (f.style.slot) {
            Slot.TOP, Slot.BOTTOM, Slot.DRESS, Slot.OUTER -> {
                // clothes rail
                pp.color = 0xFF6A6A76.toInt(); c.drawRect(cx - f.w / 2 + 6f, 24f, cx - f.w / 2 + 12f, bh - 6f.toFloat(), pp); c.drawRect(cx + f.w / 2 - 12f, 24f, cx + f.w / 2 - 6f, bh - 6f.toFloat(), pp)
                pp.shader = chrome; c.drawRoundRect(cx - f.w / 2, 18f, cx + f.w / 2, 26f, 4f, 4f, pp); pp.shader = null
                val sw = (f.w - 30f) / show.size
                for ((i, it) in show.withIndex()) {
                    val x = cx - f.w / 2 + 15f + i * sw
                    pp.color = 0xFF3A3A44.toInt(); pp.style = Paint.Style.STROKE; pp.strokeWidth = 2f
                    c.drawLine(x + sw / 2, 22f, x + sw / 2, 32f, pp); pp.style = Paint.Style.FILL
                    val th = if (it.slot == Slot.DRESS) 132 else 104
                    c.drawBitmap(AvatarRenderer.thumb(it, (sw + 26).toInt(), th), x - 13f, 30f, null)
                }
            }
            Slot.SHOES -> {
                for (k in 0..1) { pp.color = 0xFFB98A5E.toInt(); c.drawRect(cx - f.w / 2, 70f + k * 55f, cx + f.w / 2, 80f + k * 55f, pp) }
                pp.color = 0xFF8A6A48.toInt(); c.drawRect(cx - f.w / 2, 30f, cx - f.w / 2 + 8f, bh - 4f.toFloat(), pp); c.drawRect(cx + f.w / 2 - 8f, 30f, cx + f.w / 2, bh - 4f.toFloat(), pp)
                for ((i, it) in f.items.take(6).withIndex()) c.drawBitmap(AvatarRenderer.thumb(it, 50, 50), cx - f.w / 2 + 6f + (i % 3) * 46f, 24f + (i / 3) * 55f, null)
            }
            Slot.HAT -> {
                pp.color = -16777216; pp.shader = RadialGradient(cx, 120f, 70f, 0xFFF2E8D2.toInt(), 0xFFB98A5E.toInt(), Shader.TileMode.CLAMP)
                c.drawOval(cx - f.w / 2, 100f, cx + f.w / 2, 150f, pp); pp.shader = null
                pp.color = 0xFF8A6A48.toInt(); c.drawRect(cx - 6f, 140f, cx + 6f, bh.toFloat(), pp)
                for ((i, it) in show.withIndex()) c.drawBitmap(AvatarRenderer.thumb(it, 66, 66), cx - f.w / 2 + 6f + i * 44f, 50f - (i % 2) * 22f, null)
            }
            Slot.GLASSES, Slot.EARS, Slot.NECK, Slot.BAG -> {
                // glass display counter
                pp.color = 0xFF2E1A47.toInt(); c.drawRoundRect(cx - f.w / 2, 90f, cx + f.w / 2, bh.toFloat(), 10f, 10f, pp)
                pp.color = -16777216; pp.shader = LinearGradient(0f, 40f, 0f, 100f, 0x88D8F0FF.toInt(), 0x55A0C8E0, Shader.TileMode.CLAMP)
                c.drawRoundRect(cx - f.w / 2, 40f, cx + f.w / 2, 100f, 10f, 10f, pp); pp.shader = null
                val n = if (f.style.slot == Slot.BAG) 2 else 3
                for ((i, it) in f.items.take(n).withIndex()) {
                    val sz = if (f.style.slot == Slot.BAG) 70 else 52
                    c.drawBitmap(AvatarRenderer.thumb(it, sz, sz, body = f.style.slot != Slot.BAG), cx - f.w / 2 + 8f + i * (f.w - 16f) / n, 40f, null)
                }
            }
            Slot.BACK -> {
                val it = show.first()
                c.drawBitmap(AvatarRenderer.thumb(it, bw - 10, bh - 10, body = true), 5f, 0f, null)
            }
        }
        return bmp
    }

    private fun nameTag(c: Canvas, x: Float, y: Float, s: String, col: Int) {
        val tw = Ui.measure(s, 24f) + 26f
        r.set(x - tw / 2, y - 26f, x + tw / 2, y + 8f)
        Ui.pill(c, r, col)
        Ui.text(c, s, x, y, 24f, Color.WHITE, Paint.Align.CENTER, shadow = false)
    }

    // ---------------------------------------------------------------- HUD

    private fun drawHud(c: Canvas) {
        // theme chip
        val tw = Ui.measure(match.theme.name, 30f) + 120f
        r.set(20f, 20f, 20f + tw.coerceAtLeast(300f), 112f)
        Ui.panel(c, r, 0xE62E1A47.toInt(), 24f, Pal.GOLD)
        Ui.text(c, "THEME", r.left + 22f, r.top + 30f, 18f, Pal.GOLD, tf = Ui.condensed)
        Ui.text(c, match.theme.name, r.left + 22f, r.top + 70f, 30f, Color.WHITE)
        // timer
        val urgent = remaining < 30f
        r.set(w / 2 - 110f, 18f, w / 2 + 110f, 100f)
        Ui.panel(c, r, if (urgent) 0xEEB0203A.toInt() else 0xE62E1A47.toInt(), 40f, if (urgent) Color.WHITE else 0x55FFFFFF)
        Icons.draw(c, "clock", r.left + 44f, r.centerY(), 40f, Pal.GOLD)
        val sc = if (urgent) 1f + 0.08f * abs(sin(time * 6f)) else 1f
        Ui.text(c, Ui.clockText(remaining), r.centerX() + 26f, r.centerY() + 20f * sc, 54f * sc, Color.WHITE, Paint.Align.CENTER, Ui.black)
        // minimap
        Ui.panel(c, minimap, 0xDD1B1026.toInt(), 12f, 0x66FFFFFF)
        val sx = minimap.width() / WW; val sy = minimap.height() / WH
        for (z in zones) { p.color = AvatarRenderer.withAlpha(z.colour, 170); c.drawRect(minimap.left + z.rect.left * sx, minimap.top + z.rect.top * sy, minimap.left + z.rect.right * sx, minimap.top + z.rect.bottom * sy, p) }
        p.color = 0xAA2E1A47.toInt()
        for (f in fixtures) c.drawCircle(minimap.left + f.x * sx, minimap.top + f.y * sy, 2f, p)
        for (s in shoppers) { p.color = if (s.c.ai) 0xFFFFFFFF.toInt() else Pal.SKY; c.drawCircle(minimap.left + s.x * sx, minimap.top + s.y * sy, 4f, p) }
        p.color = Pal.PINK; c.drawCircle(minimap.left + px * sx, minimap.top + py * sy, 6f + sin(time * 6f), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = Color.WHITE
        c.drawRect(minimap.left + (camX - w / 2 / zoom) * sx, minimap.top + (camY - h / 2 / zoom) * sy, minimap.left + (camX + w / 2 / zoom) * sx, minimap.top + (camY + h / 2 / zoom) * sy, p)
        p.style = Paint.Style.FILL
        Ui.text(c, "Tap the map to walk", minimap.centerX(), minimap.bottom + 24f, 17f, Pal.CREAM, Paint.Align.CENTER, Ui.regular)
        // joystick
        if (joyId >= 0) {
            p.color = 0x33FFFFFF; c.drawCircle(joyX, joyY, 90f, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0x66FFFFFF; c.drawCircle(joyX, joyY, 90f, p); p.style = Paint.Style.FILL
            p.color = 0xCCFF5C9A.toInt(); c.drawCircle(joyX + joyDX.coerceIn(-90f, 90f), joyY + joyDY.coerceIn(-90f, 90f), 42f, p)
        } else if (time < 8f && browsing == null) {
            Ui.text(c, "Drag on the left to walk.  Tap a rack to go to it.", 30f, h - 30f, 22f, Pal.CREAM, tf = Ui.regular)
        }
        // worn count
        val n = me.outfit.items.size
        Ui.text(c, "Wearing $n item${if (n == 1) "" else "s"}", w - 160f, h - 210f, 22f, Pal.CREAM, Paint.Align.CENTER, Ui.regular)
        // tip
        if (tipT > 0f) {
            val a = (tipT.coerceAtMost(0.4f) / 0.4f)
            val tw2 = Ui.measure(tip, 26f) + 60f
            r.set(w / 2 - tw2 / 2, 116f, w / 2 + tw2 / 2, 166f)
            Ui.panel(c, r, Color.argb((a * 230).toInt(), 255, 216, 107), 25f, 0)
            Ui.text(c, tip, w / 2, 150f, 26f, Pal.INK, Paint.Align.CENTER, shadow = false, alpha = (a * 255).toInt())
        }
    }

    // ---------------------------------------------------------------- browse / outfit panel

    private fun drawPanel(c: Canvas) {
        p.color = 0xAA0A0410.toInt(); c.drawRect(0f, 0f, w, h, p)
        // preview of the player
        val pvx = w * 0.17f
        Look.spotlight(c, pvx, 380f, 260f)
        pose.idle(time)
        AvatarRenderer.draw(c, pvx, 640f, 520f, me.avatar, me.outfit, pose, time)
        Ui.text(c, Ui.clockText(remaining), pvx, 60f, 44f, if (remaining < 30f) 0xFFFF5C7A.toInt() else Color.WHITE, Paint.Align.CENTER, Ui.black)
        panel.set(w * 0.34f, 20f, w - 110f, h - 20f)
        Ui.panel(c, panel, 0xF2201230.toInt())
        val f = browsing
        val items: List<Item>
        val title: String
        if (f != null) { items = f.items; title = "${f.style.name}  (${items.size})" }
        else { items = me.outfit.all(); title = "My outfit" }
        Ui.text(c, title, panel.left + 30f, panel.top + 58f, 40f, Pal.GOLD, tf = Ui.serif)
        if (f == null) {
            Ui.text(c, "Tap an item to take it off", panel.left + 32f, panel.top + 94f, 22f, Pal.CREAM, tf = Ui.regular)
            stylistTip(c)
        } else Ui.text(c, "Tap to try on.  Tap again to take off.", panel.left + 32f, panel.top + 94f, 22f, Pal.CREAM, tf = Ui.regular)

        val gridTop = panel.top + 116f
        val cols = ((panel.width() - 40f) / 190f).toInt().coerceAtLeast(2)
        val cw = (panel.width() - 40f) / cols
        val chh = cw * 1.22f
        val rows = ceil(items.size / cols.toFloat()).toInt()
        scrollMax = (rows * chh - (panel.bottom - gridTop - 14f) + (if (f == null) 120f else 0f)).coerceAtLeast(0f)
        cards.clear()
        c.save()
        c.clipRect(panel.left, gridTop, panel.right, panel.bottom - 8f)
        for ((i, it) in items.withIndex()) {
            val x = panel.left + 20f + (i % cols) * cw
            val y = gridTop + (i / cols) * chh - scroll + if (f == null) 120f else 0f
            if (y > panel.bottom || y + chh < gridTop) continue
            val cr = RectF(x + 6f, y + 6f, x + cw - 6f, y + chh - 6f)
            cards.add(cr to it)
            val worn = me.outfit.isWearing(it)
            Ui.panel(c, cr, if (worn) 0xFF5A2A6E.toInt() else 0xFF2E1A47.toInt(), 18f, if (worn) Pal.GOLD else 0x33FFFFFF)
            AvatarRenderer.icon(c, it, RectF(cr.left + 8f, cr.top + 8f, cr.right - 8f, cr.bottom - 52f), time)
            val name = if (Ui.measure(it.name, 17f) > cr.width() - 14f) it.name.take(((cr.width() - 14f) / 9f).toInt()) + "." else it.name
            Ui.text(c, name, cr.centerX(), cr.bottom - 26f, 17f, Color.WHITE, Paint.Align.CENTER, Ui.regular, false)
            if (worn) { Ui.pill(c, RectF(cr.right - 44f, cr.top + 8f, cr.right - 8f, cr.top + 44f), Pal.GOLD); Icons.draw(c, "check", cr.right - 26f, cr.top + 26f, 24f, Pal.INK) }
            val fit = Scoring.itemFit(it, match.theme)
            if (fit >= 6f) Ui.star(c, cr.left + 24f, cr.top + 24f, 12f, 0xCCFFD86B.toInt())
        }
        c.restore()
        if (items.isEmpty() && f == null) Ui.text(c, "You're not wearing anything from the shop yet.", panel.centerX(), panel.centerY() + 60f, 26f, Pal.GREY, Paint.Align.CENTER, Ui.regular)
        if (scrollMax > 0f) {
            val th = (panel.height() - 140f) * ((panel.height() - 140f) / (panel.height() - 140f + scrollMax))
            val ty = gridTop + (panel.height() - 140f - th) * (scroll / scrollMax)
            Ui.pill(c, RectF(panel.right - 12f, ty, panel.right - 5f, ty + th), 0x88FFFFFF.toInt())
        }
    }

    private fun stylistTip(c: Canvas) {
        val o = me.outfit
        val missing = match.theme.tags.filter { tag -> o.all().none { tag in it.tags } }
        val msg = when {
            o.get(Slot.DRESS) == null && (o.get(Slot.TOP) == null || o.get(Slot.BOTTOM) == null) -> "Stylist: you still need a full base look. Try a dress, or a top with bottoms."
            o.get(Slot.SHOES) == null -> "Stylist: don't forget shoes. The runway is long."
            missing.isNotEmpty() -> "Stylist: the judges are hoping for something more ${missing.first()}."
            o.items.size < 5 -> "Stylist: a few accessories would finish this off."
            else -> "Stylist: this look is runway ready."
        }
        val rr = RectF(panel.left + 24f, panel.top + 112f, panel.right - 24f, panel.top + 220f)
        Ui.panel(c, rr, 0x33FFD86B, 16f, 0)
        Icons.draw(c, "eye", rr.left + 40f, rr.centerY(), 38f, Pal.GOLD)
        Ui.wrap(c, msg, rr.left + 80f, rr.top + 42f, rr.width() - 100f, 24f, Pal.CREAM)
    }

    private fun drawWaiting(c: Canvas) {
        p.color = 0xCC0A0410.toInt(); c.drawRect(0f, 0f, w, h, p)
        Ui.text(c, "Looking fabulous", w / 2, h / 2 - 30f, 56f, Pal.GOLD, Paint.Align.CENTER, Ui.serif)
        val waiting = match.contestants.filter { !it.ai && !it.done }.joinToString(", ") { it.name }
        Ui.text(c, if (waiting.isEmpty()) "Heading to the runway..." else "Waiting for $waiting", w / 2, h / 2 + 30f, 28f, Color.WHITE, Paint.Align.CENTER, Ui.regular)
        AvatarRenderer.draw(c, w / 2, h - 40f, 260f, me.avatar, me.outfit, pose.also { it.idle(time) }, time)
    }

    override fun back(): Boolean {
        if (browsing != null || showOutfit) { closePanels(); return true }
        return true
    }
}
