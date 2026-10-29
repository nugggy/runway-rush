package com.nugggy.runwayrush.scene

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.nugggy.runwayrush.core.Button
import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.core.Icons
import com.nugggy.runwayrush.core.Pal
import com.nugggy.runwayrush.core.Scene
import com.nugggy.runwayrush.core.Sfx
import com.nugggy.runwayrush.core.Track
import com.nugggy.runwayrush.core.Ui
import com.nugggy.runwayrush.net.HostFinder
import com.nugggy.runwayrush.net.NetSession
import com.nugggy.runwayrush.render.AvatarRenderer
import com.nugggy.runwayrush.render.Pose

/** WiFi party: host a room or join one on the same network. */
class LobbyScene(game: Game, private var net: NetSession?, private val joined: Boolean = false) : Scene(game) {
    private var finder: HostFinder? = null
    private var aiCount = 3
    private val poses = HashMap<Int, Pose>()
    private var lastHosts = 0
    private var lastPlayers = -1

    override fun enter() {
        game.sound.music(Track.MENU)
        val n = net
        if (n == null) { finder = HostFinder(game.activity).also { it.start() } }
        else attach(n)
    }

    override fun exit() { finder?.stop() }

    private fun attach(n: NetSession) {
        game.net = n
        n.onLobbyChanged = { game.post { layout() } }
        n.handler = { t, m, _ ->
            when (t) {
                "start" -> if (!n.isHost) {
                    val match = Match.fromStart(n, m)
                    match.attach(game)
                    game.sound.play(Sfx.GO)
                    game.go(ThemeScene(game, match))
                }
                "_hostgone" -> { game.toast("The host closed the party"); leave() }
                "_join" -> game.sound.play(Sfx.VOTE)
            }
        }
    }

    private fun host() {
        val n = NetSession.host(game)
        net = n
        finder?.stop(); finder = null
        attach(n)
        game.sound.play(Sfx.PICK)
        layout()
    }

    private fun join(ip: String, port: Int = NetSession.PORT) {
        val n = NetSession.join(game, ip, port)
        net = n
        finder?.stop(); finder = null
        attach(n)
        layout()
    }

    private fun leave() {
        net?.close(); net = null; game.net = null
        game.go(MenuScene(game))
    }

    override fun layout() {
        ui.clear()
        Look.backButton(this) { leave() }
        val n = net
        if (n == null) {
            ui.add(Button("Host a party", "crown", Pal.BERRY, 32f) { host() }.set(80f, 200f, 440f, 96f))
            ui.add(Button("Join by IP address", "wifi", 0xFF5A3FC0.toInt(), 26f) {
                game.activity.promptText("Host IP address (shown on the host's screen)", NetSession.localIp()?.substringBeforeLast(".")?.plus(".") ?: "192.168.", 40, true) { s ->
                    if (!s.isNullOrBlank()) join(s.trim())
                }
            }.set(80f, 316f, 440f, 80f))
            val hosts = finder?.found?.values?.toList() ?: emptyList()
            lastHosts = hosts.size
            for ((i, hst) in hosts.withIndex()) {
                ui.add(Button("Join ${hst.name}'s party", "play", 0xFF1E9E9A.toInt(), 26f) { join(hst.host, hst.port) }.also { it.sublabel = null }.set(w / 2 + 40f, 200f + i * 96f, w / 2 - 120f, 82f))
            }
        } else {
            if (n.isHost) {
                ui.add(Button("", "back", 0xFF4A4054.toInt()) { aiCount = (aiCount - 1).coerceAtLeast(0) }.set(80f, h - 140f, 70f, 70f))
                ui.add(Button("", "play", 0xFF4A4054.toInt()) { aiCount = (aiCount + 1).coerceAtMost(6) }.set(380f, h - 140f, 70f, 70f))
                val start = Button("Start the show", "flag", Pal.BERRY, 32f) {
                    if (n.players.size + aiCount < 2) { game.toast("Add an AI rival or wait for a friend"); return@Button }
                    val match = Match.hostStart(game, n, aiCount)
                    match.attach(game)
                    game.sound.play(Sfx.GO)
                    game.go(ThemeScene(game, match))
                }
                ui.add(start.set(w - 440f, h - 124f, 400f, 96f))
            }
        }
        lastPlayers = n?.players?.size ?: -1
    }

    override fun update(dt: Float) {
        if (net == null && (finder?.found?.size ?: 0) != lastHosts) layout()
        net?.error?.let { e -> game.toast(e); leave() }
    }

    override fun draw(c: Canvas) {
        Look.photoBg(c, game, "bg/lobby.jpg", 150)
        Look.title(c, "WiFi Party", 110f, 76f, 52f)
        val n = net
        if (n == null) {
            Ui.wrap(c, "Everyone needs to be on the same WiFi network. One phone hosts, the others join. Up to 8 players, plus AI rivals.", 80f, 140f, w / 2 - 120f, 24f, Pal.CREAM)
            Ui.text(c, "Parties nearby", w / 2 + 40f, 170f, 30f, Pal.GOLD, tf = Ui.condensed)
            if ((finder?.found?.size ?: 0) == 0) {
                val dots = ".".repeat(((time * 2).toInt() % 4))
                Ui.text(c, "Searching$dots", w / 2 + 40f, 240f, 26f, Pal.GREY, tf = Ui.regular)
                Icons.draw(c, "wifi", w / 2 + 60f, 320f, 60f + 6f * kotlin.math.sin(time * 4f), 0x66FFFFFF)
            }
            Ui.text(c, "Your IP: ${NetSession.localIp() ?: "not on WiFi"}", 80f, h - 40f, 22f, Pal.GREY, tf = Ui.regular)
        } else {
            val status = when {
                n.isHost -> "You are hosting. Friends can join using IP ${n.ip ?: "?"}"
                !n.connected -> "Connecting..."
                else -> "Connected. Waiting for the host to start."
            }
            Ui.text(c, status, 80f, 140f, 26f, Pal.CREAM, tf = Ui.regular)
            // players
            val list = n.players.values.toList()
            val slotW = ((w - 160f) / 8f).coerceAtMost(150f)
            for ((i, pl) in list.withIndex()) {
                val x = 80f + slotW * (i + 0.5f)
                val pose = poses.getOrPut(pl.id) { Pose() }
                pose.idle(time, pl.id.toFloat())
                Look.spotlight(c, x, 380f, 110f)
                AvatarRenderer.draw(c, x, 470f, 240f, pl.avatar, null, pose, time)
                val tag = if (pl.id == n.myId) "${pl.name} (you)" else pl.name
                Ui.text(c, tag, x, 510f, 22f, if (pl.id == 0) Pal.GOLD else Color.WHITE, Paint.Align.CENTER)
                if (pl.id == 0) Icons.draw(c, "crown", x, 196f, 30f, Pal.GOLD)
            }
            if (n.isHost) {
                Ui.text(c, "AI rivals", 80f, h - 156f, 26f, Pal.CREAM)
                Ui.text(c, "$aiCount", 265f, h - 88f, 44f, Color.WHITE, Paint.Align.CENTER, Ui.black)
                Ui.text(c, "${list.size} player${if (list.size == 1) "" else "s"} in the room", w - 440f, h - 150f, 24f, Pal.CREAM, tf = Ui.regular)
            }
        }
        ui.draw(c)
    }

    override fun back(): Boolean { leave(); return true }
}
