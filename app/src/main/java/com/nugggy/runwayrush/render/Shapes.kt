package com.nugggy.runwayrush.render

import android.graphics.Path

/** Shape helpers. Garments are described as the right half of a symmetric outline. */
object Shapes {
    /**
     * Builds a smooth closed path from the right-hand points (top to bottom), mirrored to the left.
     * Repeat a point to make a sharp corner there.
     */
    fun sym(pts: FloatArray, path: Path = Path(), cx: Float = 0f): Path {
        val n = pts.size / 2
        val xs = FloatArray(n * 2); val ys = FloatArray(n * 2)
        for (i in 0 until n) { xs[i] = cx + pts[i * 2]; ys[i] = pts[i * 2 + 1] }
        for (i in 0 until n) { xs[n + i] = cx - pts[(n - 1 - i) * 2]; ys[n + i] = pts[(n - 1 - i) * 2 + 1] }
        return smooth(xs, ys, path)
    }

    /** Smooth closed path through arbitrary points (x0,y0,x1,y1,...). */
    fun poly(pts: FloatArray, path: Path = Path()): Path {
        val n = pts.size / 2
        val xs = FloatArray(n) { pts[it * 2] }; val ys = FloatArray(n) { pts[it * 2 + 1] }
        return smooth(xs, ys, path)
    }

    private fun smooth(xs: FloatArray, ys: FloatArray, path: Path): Path {
        path.reset()
        val m = xs.size
        if (m < 3) return path
        fun mx(i: Int) = (xs[i % m] + xs[(i + 1) % m]) / 2
        fun my(i: Int) = (ys[i % m] + ys[(i + 1) % m]) / 2
        path.moveTo(mx(m - 1), my(m - 1))
        for (i in 0 until m) path.quadTo(xs[i], ys[i], mx(i), my(i))
        path.close()
        return path
    }

    /** Straight-edged polygon. */
    fun lines(pts: FloatArray, path: Path = Path()): Path {
        path.reset()
        path.moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) { path.lineTo(pts[i], pts[i + 1]); i += 2 }
        path.close()
        return path
    }

    fun f(vararg v: Float) = v
}
