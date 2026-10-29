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
import com.nugggy.runwayrush.core.Track
import com.nugggy.runwayrush.core.Ui
import com.nugggy.runwayrush.model.Avatar
import com.nugggy.runwayrush.render.AvatarRenderer
import com.nugggy.runwayrush.render.Pose
import kotlin.random.Random

/** Character creator: every part of the look can be changed. */
class CreatorScene(game: Game, private val firstTime: Boolean) : Scene(game) {
    private val a = game.profile.avatar.copy()
    private var tab = 0
    private val tabs = listOf("Body", "Face", "Hair", "Glam")
    private val pose = Pose()
    private val panel = RectF()
    private var spin = 0f

    private sealed class Row(val label: String)
    private class Cycle(label: String, val text: () -> String, val step: (Int) -> Unit) : Row(label)
    private class Swatch(label: String, val cols: IntArray, val get: () -> Int, val set: (Int) -> Unit) : Row(label)
    private class Toggle(label: String, val get: () -> Boolean, val set: (Boolean) -> Unit) : Row(label)

    private fun rows(): List<Row> = when (tab) {
        0 -> listOf(
            Swatch("Skin tone", Avatar.SKIN, { a.skin }, { a.skin = it }),
            Cycle("Body shape", { Avatar.BODY[a.body] }, { a.body = wrap(a.body + it, Avatar.BODY.size) }),
            Cycle("Height", { listOf("Shortest", "Short", "Medium", "Tall", "Tallest")[a.height] }, { a.height = wrap(a.height + it, 5) }),
        )
        1 -> listOf(
            Swatch("Eye colour", Avatar.EYES, { a.eyeColour }, { a.eyeColour = it }),
            Cycle("Eye shape", { Avatar.EYE_SHAPES[a.eyeShape] }, { a.eyeShape = wrap(a.eyeShape + it, Avatar.EYE_SHAPES.size) }),
            Cycle("Eyebrows", { Avatar.BROWS[a.brows] }, { a.brows = wrap(a.brows + it, Avatar.BROWS.size) }),
            Toggle("Freckles", { a.freckles }, { a.freckles = it }),
        )
        2 -> listOf(
            Cycle("Hairstyle", { Avatar.HAIR_NAMES[a.hairStyle] }, { a.hairStyle = wrap(a.hairStyle + it, Avatar.HAIR_STYLES) }),
            Swatch("Hair colour", Avatar.HAIR, { a.hairColour }, { a.hairColour = it }),
            Cycle("Facial hair", { Avatar.FACIAL[a.facial] }, { a.facial = wrap(a.facial + it, Avatar.FACIAL.size) }),
        )
        else -> listOf(
            Swatch("Lip colour", Avatar.LIPS, { a.lips }, { a.lips = it }),
            Cycle("Makeup", { Avatar.MAKEUP[a.makeup] }, { a.makeup = wrap(a.makeup + it, Avatar.MAKEUP.size) }),
            Toggle("Rosy cheeks", { a.blush }, { a.blush = it }),
        )
    }

    private fun wrap(v: Int, n: Int) = ((v % n) + n) % n

    override fun enter() { game.sound.music(Track.MENU) }

    override fun layout() {
        ui.clear()
        panel.set(w * 0.46f, 110f, w - 30f, h - 120f)
        if (!firstTime) Look.backButton(this) { game.go(MenuScene(game)) }
        // tabs
        val tw = (panel.width() - 24f) / tabs.size
        for ((i, name) in tabs.withIndex()) {
            val b = Button(name, null, if (i == tab) Pal.PINK else 0xFF3E2A58.toInt(), 26f) { tab = i; layout() }
            b.selected = i == tab
            ui.add(b.set(panel.left + 12f + i * tw, panel.top + 14f, tw - 10f, 60f))
        }
        var y = panel.top + 110f
        for (r in rows()) {
            val rowY = y
            when (r) {
                is Cycle -> {
                    ui.add(Button("", "back", 0xFF5A3FC0.toInt()) { r.step(-1) }.set(panel.right - 330f, rowY - 8f, 64f, 64f))
                    val nb = Button("", "play", 0xFF5A3FC0.toInt()) { r.step(1) }
                    ui.add(nb.set(panel.right - 80f, rowY - 8f, 64f, 64f))
                }
                is Swatch -> {
                    val n = r.cols.size
                    val avail = panel.width() - 40f
                    val sz = (avail / n).coerceAtMost(54f)
                    for (i in 0 until n) {
                        val b = Button("", null) { r.set(i) }
                        b.drawFn = { c, btn ->
                            val cx = btn.rect.centerX(); val cy = btn.rect.centerY(); val rad = btn.rect.width() / 2 - 4f
                            val pp = Ui.p; pp.shader = null; pp.style = Paint.Style.FILL
                            pp.color = 0x66000000; c.drawCircle(cx, cy + 3f, rad, pp)
                            pp.color = r.cols[i]; c.drawCircle(cx, cy, rad, pp)
                            if (r.get() == i) { pp.style = Paint.Style.STROKE; pp.strokeWidth = 4f; pp.color = Pal.GOLD; c.drawCircle(cx, cy, rad + 3f, pp); pp.style = Paint.Style.FILL }
                        }
                        ui.add(b.set(panel.left + 20f + i * sz, rowY + 36f, sz, sz))
                    }
                    y += 40f
                }
                is Toggle -> {
                    val b = Button(if (r.get()) "On" else "Off", null, if (r.get()) Pal.MINT else 0xFF4A4054.toInt(), 26f)
                    b.onClick = { r.set(!r.get()); b.label = if (r.get()) "On" else "Off"; b.color = if (r.get()) Pal.MINT else 0xFF4A4054.toInt() }
                    ui.add(b.set(panel.right - 180f, rowY - 8f, 160f, 64f))
                }
            }
            y += 100f
        }
        // bottom actions
        ui.add(Button("Name: ${game.profile.name}", "person", 0xFF3E2A58.toInt(), 26f) {
            game.activity.promptText("Your name", game.profile.name, 14, false) { s -> if (!s.isNullOrBlank()) { game.profile.name = s; layout() } }
        }.set(30f, h - 96f, 360f, 72f))
        ui.add(Button("Random", "dice", 0xFFD6702B.toInt(), 28f) { randomise() }.set(w * 0.46f, h - 96f, 230f, 72f))
        ui.add(Button(if (firstTime) "Let's go" else "Save", "check", Pal.BERRY, 30f) { save() }.set(w - 290f, h - 96f, 260f, 72f))
    }

    private fun randomise() {
        val r = Random(System.nanoTime())
        val n = Avatar.random(r)
        a.skin = n.skin; a.hairStyle = n.hairStyle; a.hairColour = n.hairColour; a.eyeColour = n.eyeColour; a.eyeShape = n.eyeShape
        a.brows = n.brows; a.lips = n.lips; a.blush = n.blush; a.freckles = n.freckles; a.facial = n.facial; a.body = n.body; a.height = n.height; a.makeup = n.makeup
        spin = 1f
        game.sound.play(Sfx.WHOOSH)
        layout()
    }

    private fun save() {
        game.profile.avatar = a.copy()
        game.profile.created = true
        game.saveProfile()
        game.sound.play(Sfx.PICK)
        game.go(MenuScene(game))
    }

    override fun update(dt: Float) { spin = (spin - dt * 2.5f).coerceAtLeast(0f) }

    override fun draw(c: Canvas) {
        Look.photoBg(c, game, "bg/studio.jpg", 110)
        Look.title(c, if (firstTime) "Create your model" else "Change your look", if (firstTime) 40f else 110f, 74f, 52f)
        // preview
        val cx = w * 0.23f
        Look.spotlight(c, cx, 380f, 300f)
        pose.idle(time)
        pose.smile = 0.8f
        val sc = 1f + spin * 0.08f
        AvatarRenderer.draw(c, cx, 600f, 470f * sc, a, game.profile.lastOutfit.takeIf { firstTime.not() }, pose, time)
        // option panel
        Ui.panel(c, panel)
        var y = panel.top + 110f
        for (r in rows()) {
            Ui.text(c, r.label, panel.left + 24f, y + 34f, 28f, Pal.CREAM)
            when (r) {
                is Cycle -> Ui.text(c, r.text(), panel.right - 173f, y + 34f, 26f, Color.WHITE, Paint.Align.CENTER, Ui.regular)
                is Swatch -> y += 40f
                is Toggle -> {}
            }
            y += 100f
        }
        ui.draw(c)
    }

    override fun back(): Boolean { if (firstTime) return false; game.go(MenuScene(game)); return true }
}
