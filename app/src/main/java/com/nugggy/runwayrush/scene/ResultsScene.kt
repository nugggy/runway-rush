package com.nugggy.runwayrush.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
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
import org.json.JSONObject
import kotlin.random.Random

class ResultsScene(game: Game, private val match: Match) : Scene(game) {
    private val ranked = match.contestants.sortedWith(compareByDescending<Contestant> { it.votes }.thenByDescending { it.score?.total ?: 0 })
    private val poses = ranked.associate { it.id to Pose() }
    private val confetti = ArrayList<FloatArray>()
    private val rnd = Random(match.seed)
    private val myRank = ranked.indexOfFirst { it.local } + 1

    override fun enter() {
        match.phase = "results"
        match.attach(game)
        game.sound.music(Track.MENU)
        game.sound.play(Sfx.FANFARE)
        val pr = game.profile
        pr.games++
        if (myRank == 1) pr.wins++
        pr.bestScore = maxOf(pr.bestScore, match.me.score?.total ?: 0)
        game.saveProfile()
        for (i in 0 until 160) confetti.add(floatArrayOf(rnd.nextFloat() * 1600f, -rnd.nextFloat() * 800f, (rnd.nextFloat() - 0.5f) * 80f, 120f + rnd.nextFloat() * 160f, rnd.nextFloat() * 360f, rnd.nextInt(5).toFloat()))
    }

    override fun layout() {
        ui.clear()
        val net = match.net
        when {
            net == null -> {
                ui.add(Button("Play again", "play", Pal.BERRY, 30f) { game.go(ThemeScene(game, Match.single(game))) }.set(w - 360f, h - 100f, 330f, 80f))
                ui.add(Button("Menu", "back", 0xFF4A4054.toInt(), 28f) { game.go(MenuScene(game)) }.set(w - 560f, h - 100f, 180f, 80f))
            }
            net.isHost -> {
                ui.add(Button("Back to party", "people", Pal.BERRY, 28f) {
                    net.broadcast(JSONObject().put("t", "lobby_again"))
                    game.go(LobbyScene(game, net, joined = true))
                }.set(w - 360f, h - 100f, 330f, 80f))
                ui.add(Button("Leave", "back", 0xFF4A4054.toInt(), 28f) { net.close(); game.net = null; game.go(MenuScene(game)) }.set(w - 560f, h - 100f, 180f, 80f))
            }
            else -> ui.add(Button("Leave", "back", 0xFF4A4054.toInt(), 28f) { net.close(); game.net = null; game.go(MenuScene(game)) }.set(w - 230f, h - 100f, 200f, 80f))
        }
    }

    override fun update(dt: Float) {
        for (q in confetti) {
            q[0] += q[2] * dt; q[1] += q[3] * dt; q[4] += dt * 200f
            if (q[1] > h + 20f) { q[1] = -20f; q[0] = rnd.nextFloat() * w }
        }
        if (time > 2f && time - 0.02f < 2f) game.sound.play(Sfx.CHEER)
    }

    override fun draw(c: Canvas) {
        Look.photoBg(c, game, "bg/results.jpg", 170)
        val p = Ui.p
        Ui.text(c, if (myRank == 1) "You won the show" else "The audience has spoken", w * 0.33f, 70f, 48f, Pal.GOLD, Paint.Align.CENTER, Ui.serif)
        Ui.text(c, match.theme.name, w * 0.33f, 110f, 26f, Pal.CREAM, Paint.Align.CENTER, Ui.regular)

        // podium: 2nd, 1st, 3rd
        val cx = w * 0.33f
        val podium = listOf(Triple(1, cx - 210f, 150f), Triple(0, cx, 210f), Triple(2, cx + 210f, 110f))
        for ((rank, x, ht) in podium) {
            val cont = ranked.getOrNull(rank) ?: continue
            val top = h - 60f - ht
            val reveal = ((time - (2 - rank) * 0.6f) / 0.5f).coerceIn(0f, 1f)
            p.color = -16777216; p.shader = LinearGradient(0f, top, 0f, h, intArrayOf(if (rank == 0) Pal.GOLD else if (rank == 1) 0xFFC7CBD3.toInt() else 0xFFD08A4E.toInt(), 0xFF2E1A47.toInt()), null, Shader.TileMode.CLAMP)
            c.drawRect(x - 95f, top, x + 95f, h, p); p.shader = null
            Ui.text(c, "${rank + 1}", x, top + 70f, 64f, Color.WHITE, Paint.Align.CENTER, Ui.black)
            if (reveal > 0f) {
                val pose = poses[cont.id]!!
                if (rank == 0) pose.strike(time) else pose.idle(time, cont.id.toFloat())
                c.saveLayerAlpha(0f, 0f, w, h, (reveal * 255).toInt())
                if (rank == 0) Look.spotlight(c, x, top - 160f, 200f, 0x66FFD86B)
                AvatarRenderer.draw(c, x, top, if (rank == 0) 330f else 270f, cont.avatar, cont.outfit, pose, time)
                Ui.text(c, cont.name, x, top + 110f, 26f, Color.WHITE, Paint.Align.CENTER)
                Ui.text(c, "${cont.votes} vote${if (cont.votes == 1) "" else "s"}", x, top + 140f, 20f, Pal.CREAM, Paint.Align.CENTER, Ui.regular)
                if (rank == 0) Icons.draw(c, "crown", x, top - (330f * 1.0f) - 4f, 44f, Pal.GOLD)
                c.restore()
            }
        }

        // leaderboard
        val board = RectF(w * 0.66f, 30f, w - 30f, h - 120f)
        Ui.panel(c, board, 0xE6201230.toInt())
        Ui.text(c, "Leaderboard", board.left + 24f, board.top + 50f, 32f, Pal.GOLD, tf = Ui.serif)
        Ui.text(c, "votes", board.right - 130f, board.top + 50f, 18f, Pal.GREY, Paint.Align.RIGHT, Ui.regular)
        Ui.text(c, "style", board.right - 24f, board.top + 50f, 18f, Pal.GREY, Paint.Align.RIGHT, Ui.regular)
        val rowH = ((board.height() - 170f) / ranked.size).coerceAtMost(62f)
        for ((i, cont) in ranked.withIndex()) {
            val y = board.top + 80f + i * rowH
            if (cont.local) { p.color = 0x33FFD86B; c.drawRoundRect(board.left + 10f, y, board.right - 10f, y + rowH - 6f, 12f, 12f, p) }
            Ui.text(c, "${i + 1}", board.left + 34f, y + rowH * 0.62f, 26f, if (i == 0) Pal.GOLD else Color.WHITE, Paint.Align.CENTER, Ui.black)
            Ui.text(c, cont.name + if (cont.local) " (you)" else "", board.left + 64f, y + rowH * 0.62f, 24f, Color.WHITE)
            Ui.text(c, "${cont.votes}", board.right - 130f, y + rowH * 0.62f, 24f, Pal.PINK, Paint.Align.RIGHT)
            Ui.text(c, "${cont.score?.total ?: 0}", board.right - 24f, y + rowH * 0.62f, 24f, Pal.MINT, Paint.Align.RIGHT)
        }
        // my breakdown
        match.me.score?.let { s ->
            val y = board.bottom - 84f
            Ui.text(c, "Your style score ${s.total}/100", board.left + 24f, y + 20f, 24f, Pal.GOLD)
            Ui.text(c, "Theme ${s.theme}/60  Look ${s.complete}/18  Extras ${s.accessories}/10  Colour ${s.colour}/12", board.left + 24f, y + 52f, 17f, Pal.CREAM, tf = Ui.regular)
        }
        // confetti
        val cols = intArrayOf(Pal.GOLD, Pal.PINK, Pal.MINT, Pal.SKY, Pal.CORAL)
        for (q in confetti) {
            c.save(); c.translate(q[0], q[1]); c.rotate(q[4])
            p.color = cols[q[5].toInt()]; c.drawRect(-5f, -3f, 5f, 3f, p)
            c.restore()
        }
        if (match.net != null && match.net.isHost.not()) Ui.text(c, "Waiting for the host...", w - 260f, h - 120f, 20f, Pal.CREAM, Paint.Align.CENTER, Ui.regular)
        ui.draw(c)
    }

    override fun back(): Boolean { if (match.net == null) { game.go(MenuScene(game)); return true }; return true }
}
