package com.nugggy.runwayrush.core

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.nugggy.runwayrush.MainActivity
import com.nugggy.runwayrush.model.Profile
import com.nugggy.runwayrush.net.NetSession
import com.nugggy.runwayrush.scene.MenuScene
import com.nugggy.runwayrush.scene.SplashScene
import java.util.concurrent.ConcurrentLinkedQueue

/** Base class for every screen in the game. Virtual canvas is game.w x 720. */
abstract class Scene(val game: Game) {
    val ui = UiList().also { it.sound = game.sound }
    var time = 0f
    open fun enter() {}
    open fun exit() {}
    open fun layout() {}
    open fun update(dt: Float) {}
    abstract fun draw(c: Canvas)
    open fun touch(t: Touch): Boolean = ui.touch(t)
    /** Return true when handled. */
    open fun back(): Boolean = false
    val w get() = game.w
    val h get() = Game.H
}

class Game(val activity: MainActivity) {

    companion object { const val H = 720f }

    var w = 1280f; private set
    private var scale = 1f
    private var offX = 0f
    private var screenW = 0
    private var screenH = 0

    val assets = Assets(activity).also { com.nugggy.runwayrush.render.AvatarRenderer.assets = it }
    val sound = Sound().also { it.bind(activity.cacheDir) }
    val profile = Profile.load(activity)
    val updater = Updater(this)
    var net: NetSession? = null

    private val queue = ConcurrentLinkedQueue<() -> Unit>()
    private val touches = ConcurrentLinkedQueue<Touch>()

    var scene: Scene? = null; private set
    private var next: Scene? = null
    private var fade = 0f
    private var fadeDir = 0
    private var started = false

    private val toasts = ArrayList<Pair<String, Float>>()
    private val toastPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlay = Paint()

    fun start() {
        sound.init()
        post {
            go(SplashScene(this), instant = true)
        }
    }

    fun post(fn: () -> Unit) { queue.add(fn) }

    fun postTouch(type: TouchType, id: Int, x: Float, y: Float) {
        touches.add(Touch(type, id, (x - offX) / scale, y / scale))
    }

    fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (width == screenW && height == screenH && started) return
        screenW = width; screenH = height
        scale = height / H
        w = width / scale
        offX = 0f
        started = true
        scene?.layout()
    }

    fun go(s: Scene, instant: Boolean = false) {
        if (instant || scene == null) {
            scene?.exit()
            scene = s
            s.enter(); s.layout()
            fade = 1f; fadeDir = -1
        } else {
            next = s
            fadeDir = 1
        }
    }

    fun toast(msg: String) { toasts.add(msg to 3.2f) }

    fun onBack(): Boolean {
        val s = scene ?: return false
        if (s.back()) return true
        if (s !is MenuScene) { go(MenuScene(this)); return true }
        return false
    }

    fun frame(c: Canvas, dt: Float) {
        while (true) { val fn = queue.poll() ?: break; fn() }
        net?.pump()
        val s = scene
        if (s != null && fadeDir <= 0) {
            while (true) { val t = touches.poll() ?: break; if (!updater.touch(t)) s.touch(t) }
        } else touches.clear()

        if (fadeDir > 0) {
            fade += dt * 4f
            if (fade >= 1f) {
                fade = 1f; fadeDir = -1
                scene?.exit()
                val n = next!!; next = null
                scene = n; n.enter(); n.layout()
            }
        } else if (fadeDir < 0) {
            fade -= dt * 3f
            if (fade <= 0f) { fade = 0f; fadeDir = 0 }
        }

        s?.time = (s?.time ?: 0f) + dt
        scene?.update(dt)
        sound.update(dt)

        c.save()
        c.scale(scale, scale)
        c.drawColor(Color.BLACK)
        scene?.draw(c)
        updater.draw(c, dt)
        drawToasts(c, dt)
        if (fade > 0f) {
            overlay.color = Color.argb((fade * 255).toInt().coerceIn(0, 255), 8, 4, 16)
            c.drawRect(0f, 0f, w, H, overlay)
        }
        c.restore()
    }

    private fun drawToasts(c: Canvas, dt: Float) {
        var y = H - 110f
        val it = toasts.listIterator()
        while (it.hasNext()) {
            val (m, t) = it.next()
            val nt = t - dt
            if (nt <= 0) { it.remove(); continue }
            it.set(m to nt)
            val a = (nt.coerceAtMost(0.5f) / 0.5f)
            toastPaint.textSize = 26f
            toastPaint.typeface = Ui.bold
            val tw = toastPaint.measureText(m)
            val r = RectF(w / 2 - tw / 2 - 26, y - 34, w / 2 + tw / 2 + 26, y + 14)
            toastPaint.color = Color.argb((a * 220).toInt(), 20, 10, 30)
            c.drawRoundRect(r, 24f, 24f, toastPaint)
            toastPaint.color = Color.argb((a * 255).toInt(), 255, 255, 255)
            c.drawText(m, w / 2 - tw / 2, y - 1, toastPaint)
            y -= 58f
        }
    }

    fun saveProfile() { profile.save(activity) }

    fun shutdown() {
        net?.close(); net = null
        sound.release()
    }
}
