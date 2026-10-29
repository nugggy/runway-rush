package com.nugggy.runwayrush.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.nugggy.runwayrush.core.Button
import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.core.Pal
import com.nugggy.runwayrush.core.Scene
import com.nugggy.runwayrush.core.Sfx
import com.nugggy.runwayrush.core.Touch
import com.nugggy.runwayrush.core.TouchType
import com.nugggy.runwayrush.core.Track
import com.nugggy.runwayrush.core.Ui
import com.nugggy.runwayrush.model.Themes
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin

/** Spins a roulette of themes and reveals the one for this round. */
class ThemeScene(game: Game, private val match: Match) : Scene(game) {
    private val spinTime = 2.8f
    private var lastTick = -1
    private var revealed = false
    private var auto = 0f
    private val card = RectF()
    private val order = Themes.all.shuffled(kotlin.random.Random(match.seed)).take(30) + match.theme

    override fun enter() {
        match.phase = "theme"
        match.attach(game)
        game.sound.music(Track.SHOP)
        game.profile.seenThemes.add(match.theme.id)
    }

    override fun layout() {
        ui.clear()
        if (!match.multiplayer) Look.backButton(this) { game.go(MenuScene(game)) }
        val b = Button("Start shopping", "play", Pal.BERRY, 32f) { go() }
        b.visible = false
        ui.add(b.set(w / 2 - 190f, h - 116f, 380f, 88f))
    }

    private fun go() {
        match.started = System.currentTimeMillis()
        game.go(StoreScene(game, match))
    }

    override fun update(dt: Float) {
        match.hostTick(game, dt)
        val idx = idx()
        if (!revealed && idx != lastTick) { lastTick = idx; game.sound.play(Sfx.CLICK, 0.6f, 1.2f) }
        if (!revealed && time >= spinTime) { revealed = true; game.sound.play(Sfx.FANFARE); ui.items.last().visible = true }
        if (revealed) { auto += dt; if (auto > (if (match.multiplayer) 7f else 30f)) go() }
    }

    private fun idx(): Int {
        val t = (time / spinTime).coerceAtMost(1f)
        val eased = 1 - (1 - t) * (1 - t) * (1 - t)
        return (eased * (order.size - 1)).toInt()
    }

    override fun draw(c: Canvas) {
        val th = if (revealed) match.theme else order[idx()]
        Look.photoBg(c, game, "themes/${th.image}.jpg", 170)
        Ui.text(c, "Today's theme", w / 2, 70f, 30f, Pal.CREAM, Paint.Align.CENTER, Ui.condensed)
        card.set(w / 2 - 420f, 100f, w / 2 + 420f, if (revealed) 560f else 300f)
        Ui.panel(c, card, 0xEE201230.toInt(), 30f, if (revealed) Pal.GOLD else 0x55FFFFFF)
        if (!revealed) {
            val jitter = sin(time * 40f) * 3f
            Ui.text(c, th.name, w / 2, 220f + jitter, 64f, Color.WHITE, Paint.Align.CENTER, Ui.serif)
        } else {
            val pop = (1f + 0.25f * (1f - ((time - spinTime) / 0.3f).coerceIn(0f, 1f)))
            val img = RectF(card.left + 20f, card.top + 20f, card.left + 420f, card.top + 245f)
            game.assets.cover(c, "themes/${th.image}.jpg", img)
            Ui.text(c, th.name, card.left + 450f, card.top + 86f, 50f * pop.coerceAtMost(1.1f), Pal.GOLD, tf = Ui.serif)
            Ui.wrap(c, th.blurb, card.left + 452f, card.top + 140f, card.width() - 480f, 26f, Pal.CREAM)
            Ui.text(c, "Stylist's hints", card.left + 30f, card.top + 300f, 26f, Pal.PINK, tf = Ui.condensed)
            var x = card.left + 30f
            for (tag in th.tags) {
                val tw = Ui.measure(tag, 26f) + 40f
                Ui.pill(c, RectF(x, card.top + 320f, x + tw, card.top + 370f), 0xFF5A3FC0.toInt())
                Ui.text(c, tag, x + tw / 2, card.top + 354f, 26f, Color.WHITE, Paint.Align.CENTER, shadow = false)
                x += tw + 12f
            }
            Ui.wrap(c, "You have ${(match.duration / 60).toInt()} minutes to shop. Walk the store, try things on, then strut the runway. ${match.contestants.size - 1} rivals are competing for the audience vote.",
                card.left + 30f, card.top + 410f, card.width() - 60f, 24f, Color.WHITE)
            if (match.multiplayer) Ui.text(c, "Starting in ${ceil(7f - auto).toInt().coerceAtLeast(0)}", w / 2, h - 140f, 26f, Pal.GOLD, Paint.Align.CENTER)
        }
        ui.draw(c)
    }
}

/** Browse every theme and pick one to play. */
class ThemeListScene(game: Game) : Scene(game) {
    private var scroll = 0f
    private var maxScroll = 0f
    private var dragId = -1; private var dragY = 0f; private var moved = 0f
    private val cells = ArrayList<Pair<RectF, Int>>()

    override fun layout() { ui.clear(); Look.backButton(this) { game.go(MenuScene(game)) } }

    override fun touch(t: Touch): Boolean {
        if (ui.touch(t)) return true
        when (t.type) {
            TouchType.DOWN -> { dragId = t.id; dragY = t.y; moved = 0f }
            TouchType.MOVE -> if (t.id == dragId) { val d = t.y - dragY; dragY = t.y; moved += abs(d); scroll = (scroll - d).coerceIn(0f, maxScroll) }
            TouchType.UP -> if (t.id == dragId) {
                dragId = -1
                if (moved < 14f) cells.firstOrNull { it.first.contains(t.x, t.y) }?.let { game.sound.play(Sfx.PICK); game.go(ThemeScene(game, Match.single(game, it.second))) }
            }
        }
        return true
    }

    override fun draw(c: Canvas) {
        Ui.gradientBg(c, w, h, Pal.PLUM, Pal.INK)
        val cols = ((w - 80f) / 300f).toInt().coerceAtLeast(3)
        val cw = (w - 80f) / cols; val ch = cw * 0.68f
        val top = 120f
        maxScroll = (ceil(Themes.count / cols.toFloat()) * ch - (h - top) + 20f).coerceAtLeast(0f)
        cells.clear()
        c.save(); c.clipRect(0f, top - 10f, w, h)
        for (th in Themes.all) {
            val x = 40f + (th.id % cols) * cw; val y = top + (th.id / cols) * ch - scroll
            if (y > h || y + ch < top - 10f) continue
            val r = RectF(x + 8f, y + 8f, x + cw - 8f, y + ch - 8f)
            cells.add(r to th.id)
            c.save(); val pth = android.graphics.Path(); pth.addRoundRect(r, 18f, 18f, android.graphics.Path.Direction.CW); c.clipPath(pth)
            game.assets.cover(c, "themes/${th.image}.jpg", r)
            Ui.gradientBg(c, 0f, 0f, 0, 0)
            val g = Ui.p; g.shader = android.graphics.LinearGradient(0f, r.top, 0f, r.bottom, 0x00000000, 0xDD12081C.toInt(), android.graphics.Shader.TileMode.CLAMP); c.drawRect(r, g); g.shader = null
            c.restore()
            Ui.text(c, th.name, r.left + 16f, r.bottom - 20f, 24f, Color.WHITE)
            if (th.id in game.profile.seenThemes) Ui.star(c, r.right - 22f, r.top + 22f, 12f, Pal.GOLD)
        }
        c.restore()
        Ui.text(c, "All ${Themes.count} themes", 110f, 72f, 44f, Pal.GOLD, tf = Ui.serif)
        Ui.text(c, "Tap one to play it solo", w - 40f, 72f, 24f, Pal.CREAM, Paint.Align.RIGHT, Ui.regular)
        ui.draw(c)
    }
}
