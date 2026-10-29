package com.nugggy.runwayrush.core

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/** Hosts the render thread. All game logic runs on that thread. */
@SuppressLint("ViewConstructor")
class GameView(context: Context, private val game: Game) : SurfaceView(context), SurfaceHolder.Callback {

    @Volatile private var running = false
    private var thread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread({ loop() }, "game-loop").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        game.post { game.resize(width, height) }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try { thread?.join(1500) } catch (_: InterruptedException) {}
        thread = null
    }

    private fun loop() {
        var last = System.nanoTime()
        game.post { game.resize(width, height) }
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
            last = now
            val canvas = try { holder.lockHardwareCanvas() } catch (e: Exception) { null } ?: continue
            try {
                game.frame(canvas, dt)
            } catch (e: Exception) {
                android.util.Log.e("RunwayRush", "frame error", e)
            } finally {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val idx = event.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val id = event.getPointerId(idx); val x = event.getX(idx); val y = event.getY(idx)
                game.postTouch(TouchType.DOWN, id, x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    game.postTouch(TouchType.MOVE, event.getPointerId(i), event.getX(i), event.getY(i))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                game.postTouch(TouchType.UP, event.getPointerId(idx), event.getX(idx), event.getY(idx))
            }
            MotionEvent.ACTION_CANCEL -> {
                for (i in 0 until event.pointerCount) {
                    game.postTouch(TouchType.UP, event.getPointerId(i), -1f, -1f)
                }
            }
        }
        return true
    }
}

enum class TouchType { DOWN, MOVE, UP }

data class Touch(val type: TouchType, val id: Int, val x: Float, val y: Float)
