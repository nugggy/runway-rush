package com.nugggy.runwayrush.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.nugggy.runwayrush.BuildConfig
import com.nugggy.runwayrush.core.Button
import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.core.Pal
import com.nugggy.runwayrush.core.Scene
import com.nugggy.runwayrush.core.Track
import com.nugggy.runwayrush.core.Ui
import com.nugggy.runwayrush.model.Catalog
import com.nugggy.runwayrush.model.Themes
import com.nugggy.runwayrush.render.AvatarRenderer
import com.nugggy.runwayrush.render.Pose
import kotlin.math.sin

/** Shared look for screen backgrounds and headers. */
object Look {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    fun photoBg(c: Canvas, game: Game, img: String, dim: Int = 150) {
        r.set(0f, 0f, game.w, Game.H)
        game.assets.cover(c, img, r)
        p.color = -16777216; p.shader = LinearGradient(0f, 0f, 0f, Game.H, intArrayOf(Color.argb(dim / 2, 20, 8, 30), Color.argb(dim, 20, 8, 30), Color.argb(minOf(255, dim + 60), 12, 4, 20)), null, Shader.TileMode.CLAMP)
        c.drawRect(r, p); p.shader = null
    }

    fun spotlight(c: Canvas, x: Float, y: Float, rad: Float, col: Int = 0x55FFF4E8) {
        p.color = -16777216; p.shader = RadialGradient(x, y, rad, intArrayOf(col, 0x00FFF4E8), null, Shader.TileMode.CLAMP)
        c.drawCircle(x, y, rad, p); p.shader = null
    }

    fun title(c: Canvas, text: String, x: Float, y: Float, size: Float, align: Paint.Align = Paint.Align.LEFT) {
        Ui.text(c, text, x, y, size, Pal.GOLD, align, Ui.serif)
    }

    fun backButton(scene: Scene, onBack: () -> Unit): Button =
        scene.ui.add(Button("", "back", 0x88201230.toInt()) { onBack() }.set(20f, 18f, 72f, 72f))
}

class SplashScene(game: Game) : Scene(game) {
    private var loaded = false
    override fun enter() {
        Thread {
            Catalog.size; Themes.count
            game.assets.bitmap("bg/menu.jpg"); game.assets.bitmap("bg/studio.jpg")
            loaded = true
        }.start()
        game.updater.check()
        AvatarRenderer.assets = game.assets
        game.sound.enabled = game.profile.sound
    }

    override fun update(dt: Float) {
        if (loaded && time > 1.6f) game.go(if (game.profile.created) MenuScene(game) else CreatorScene(game, firstTime = true))
    }

    override fun draw(c: Canvas) {
        Ui.gradientBg(c, w, h, Pal.PLUM, Pal.INK)
        val a = (time / 0.6f).coerceAtMost(1f)
        Ui.text(c, "Makeover with Edie", w / 2, h / 2 - 10f, 84f, Pal.GOLD, Paint.Align.CENTER, Ui.serif, alpha = (a * 255).toInt())
        Ui.text(c, "R U N W A Y   R U S H", w / 2, h / 2 + 50f, 26f, Pal.CREAM, Paint.Align.CENTER, Ui.condensed, alpha = (a * 220).toInt())
        val dots = ((time * 3).toInt() % 4)
        Ui.text(c, "Stocking the shelves" + ".".repeat(dots), w / 2, h - 60f, 22f, Pal.GREY, Paint.Align.CENTER, Ui.regular, false)
    }
}

class MenuScene(game: Game) : Scene(game) {
    private val pose = Pose()

    override fun enter() {
        game.sound.music(Track.MENU)
        game.net?.let { it.close(); game.net = null }
    }

    override fun layout() {
        ui.clear()
        val bx = 70f; val bw = 420f
        ui.add(Button("Play Solo", "play", Pal.BERRY, 34f) { game.go(ThemeScene(game, Match.single(game))) }.set(bx, 300f, bw, 92f))
        ui.add(Button("WiFi Party", "wifi", 0xFF5A3FC0.toInt(), 32f) { game.go(LobbyScene(game, null)) }.set(bx, 410f, bw, 84f))
        ui.add(Button("My Look", "person", 0xFF1E9E9A.toInt(), 30f) { game.go(CreatorScene(game, false)) }.set(bx, 512f, 202f, 78f))
        ui.add(Button("Themes", "flag", 0xFFD6702B.toInt(), 30f) { game.go(ThemeListScene(game)) }.set(bx + 218f, 512f, 202f, 78f))
        val snd = Button("", if (game.profile.sound) "music" else "mute", 0x88201230.toInt())
        snd.onClick = {
            game.profile.sound = !game.profile.sound
            game.sound.enabled = game.profile.sound
            snd.icon = if (game.profile.sound) "music" else "mute"
            game.saveProfile()
        }
        ui.add(snd.set(w - 92f, 18f, 72f, 72f))
        ui.add(Button("Share", "share", 0xFF5CB85C.toInt(), 26f) { game.activity.shareApp() }.set(w - 386f, 18f, 196f, 72f))
        ui.add(Button("", "download", 0x88201230.toInt()) { if (game.updater.state == com.nugggy.runwayrush.core.Updater.State.FAILED || game.updater.state == com.nugggy.runwayrush.core.Updater.State.CURRENT) { game.updater.check(); game.toast("Checking for updates") } else game.updater.openReleases() }.set(w - 176f, 18f, 72f, 72f))
    }

    override fun draw(c: Canvas) {
        Look.photoBg(c, game, "bg/menu.jpg", 120)
        // title
        Ui.text(c, "Makeover", 66f, 140f, 96f, Pal.GOLD, tf = Ui.serif)
        Ui.text(c, "with Edie", 120f, 222f, 78f, Pal.PINK, tf = Ui.serif)
        Ui.text(c, "Dress to the theme. Own the runway.", 72f, 268f, 24f, Pal.CREAM, tf = Ui.regular)
        // avatar on a podium
        val cx = w * 0.72f
        Look.spotlight(c, cx, 420f, 330f)
        val p = Ui.p
        p.shader = null; p.color = 0xFF2E1A47.toInt(); c.drawOval(cx - 150f, 618f, cx + 150f, 668f, p)
        p.color = -16777216; p.shader = LinearGradient(0f, 600f, 0f, 650f, Pal.GOLD, 0xFFB08020.toInt(), Shader.TileMode.CLAMP)
        c.drawOval(cx - 150f, 600f, cx + 150f, 650f, p); p.shader = null
        pose.idle(time)
        AvatarRenderer.draw(c, cx, 625f, 520f, game.profile.avatar, game.profile.lastOutfit, pose, time)
        Ui.text(c, game.profile.name, cx, 700f, 30f, Color.WHITE, Paint.Align.CENTER)
        // stats
        val pr = game.profile
        Ui.text(c, "Shows ${pr.games}   Wins ${pr.wins}   Best ${pr.bestScore}", 72f, 640f, 22f, Pal.CREAM, tf = Ui.regular)
        Ui.text(c, "${Catalog.size} items  |  ${Themes.count} themes", 72f, 670f, 20f, Pal.GREY, tf = Ui.regular, shadow = false)
        Ui.text(c, game.updater.statusText() + "   v" + BuildConfig.VERSION_NAME, 72f, 700f, 18f, Pal.GREY, tf = Ui.regular, shadow = false)
        ui.draw(c)
        // twinkles
        for (i in 0 until 12) {
            val ph = (time * 0.6f + i * 0.21f) % 1f
            Ui.star(c, (i * 173 % w.toInt()).toFloat(), 40f + (i * 97 % 300), 4f + 6f * sin(ph * Math.PI).toFloat(), Color.argb((200 * sin(ph * Math.PI)).toInt(), 255, 240, 200))
        }
    }
}
