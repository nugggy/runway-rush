package com.nugggy.runwayrush.scene

import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.model.Avatar
import com.nugggy.runwayrush.model.Outfit
import com.nugggy.runwayrush.model.ScoreBreakdown
import com.nugggy.runwayrush.model.Scoring
import com.nugggy.runwayrush.model.Theme
import com.nugggy.runwayrush.model.Themes
import com.nugggy.runwayrush.net.NetSession
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class Contestant(val id: Int, var name: String, var avatar: Avatar, var outfit: Outfit, val ai: Boolean, val local: Boolean) {
    var score: ScoreBreakdown? = null
    var votes = 0
    var done = false
    var skill = 0.6f
    // live store position for other players
    var x = 0f; var y = 0f; var moving = false; var flip = false
    var votedFor = -1
    var plan: Outfit? = null

    fun toJson(withOutfit: Boolean) = JSONObject().put("id", id).put("name", name).put("av", avatar.toJson()).put("ai", ai).also {
        if (withOutfit) it.put("o", outfit.toJson())
        if (score != null) it.put("sc", score!!.total)
    }
}

/** One round: a theme, five minutes of shopping, a runway show and a vote. */
class Match(val theme: Theme, val seed: Long, val net: NetSession?) {
    val contestants = ArrayList<Contestant>()
    val rnd = Random(seed)
    var duration = 300f
    var started = System.currentTimeMillis()
    var phase = "theme"
    var myVote = -1
    val multiplayer get() = net != null
    val isHost get() = net == null || net.isHost
    val me get() = contestants.first { it.local }
    var judges = 24
    var votesIn = HashSet<Int>()
    var runwaySent = false
    var resultsSent = false
    private var runwayDeadline = 0f
    private var voteDeadline = 0f

    fun byId(id: Int) = contestants.firstOrNull { it.id == id }

    companion object {
        fun single(game: Game, themeId: Int = -1): Match {
            val seed = System.nanoTime()
            val r = Random(seed)
            val theme = if (themeId >= 0) Themes.all[themeId] else pickTheme(game, r)
            val m = Match(theme, seed, null)
            m.contestants.add(Contestant(0, game.profile.name, game.profile.avatar.copy(), Outfit(), ai = false, local = true))
            m.addAi(5)
            return m
        }

        fun pickTheme(game: Game, r: Random): Theme {
            val unseen = Themes.all.filter { it.id !in game.profile.seenThemes }
            val pool = unseen.ifEmpty { game.profile.seenThemes.clear(); Themes.all }
            return pool[r.nextInt(pool.size)]
        }

        /** Host builds the round from everyone in the lobby. */
        fun hostStart(game: Game, net: NetSession, aiCount: Int): Match {
            val seed = System.nanoTime()
            val r = Random(seed)
            val theme = pickTheme(game, r)
            val m = Match(theme, seed, net)
            for (p in net.players.values) m.contestants.add(Contestant(p.id, p.name, p.avatar.copy(), Outfit(), ai = false, local = p.id == net.myId))
            m.addAi(aiCount)
            val msg = JSONObject().put("t", "start").put("theme", theme.id).put("seed", seed).put("dur", m.duration.toDouble())
            msg.put("c", JSONArray().apply { for (c in m.contestants) put(c.toJson(false)) })
            net.broadcast(msg)
            return m
        }

        fun fromStart(net: NetSession, msg: JSONObject): Match {
            val m = Match(Themes.all[msg.getInt("theme")], msg.getLong("seed"), net)
            m.duration = msg.optDouble("dur", 300.0).toFloat()
            val a = msg.getJSONArray("c")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val id = o.getInt("id")
                m.contestants.add(Contestant(id, o.optString("name"), Avatar.fromJson(o.optJSONObject("av") ?: JSONObject()), Outfit(), o.optBoolean("ai"), id == net.myId))
            }
            return m
        }
    }

    fun addAi(n: Int) {
        val names = Scoring.AI_NAMES.shuffled(rnd).filter { nm -> contestants.none { it.name == nm } }
        for (i in 0 until n) {
            val c = Contestant(100 + i, names[i % names.size], Avatar.random(rnd), Outfit(), ai = true, local = false)
            c.skill = 0.25f + rnd.nextFloat() * 0.6f
            contestants.add(c)
        }
    }

    fun dressAi() {
        for (c in contestants) if (c.ai) c.outfit = planFor(c).copy()
    }

    fun planFor(c: Contestant): Outfit = c.plan ?: Scoring.aiOutfit(theme, Random(seed + c.id), c.skill).also { c.plan = it }

    fun scoreAll() { for (c in contestants) c.score = Scoring.score(c.outfit, theme) }

    // ---------------------------------------------------------------- flow

    /** Local player has finished shopping. */
    fun finishShopping(game: Game) {
        val me = me
        me.done = true
        game.profile.lastOutfit = me.outfit.copy()
        if (net == null) { toRunway(game); return }
        if (net.isHost) hostCheckRunway(game)
        else net.send(JSONObject().put("t", "done").put("o", me.outfit.toJson()))
    }

    private fun toRunway(game: Game) {
        dressAi(); scoreAll()
        phase = "runway"
        game.go(RunwayScene(game, this))
    }

    fun hostCheckRunway(game: Game, force: Boolean = false) {
        val n = net ?: return
        if (runwaySent) return
        if (!force && contestants.any { !it.ai && !it.done }) return
        runwaySent = true
        dressAi(); scoreAll()
        val msg = JSONObject().put("t", "runway").put("c", JSONArray().apply { for (c in contestants) put(c.toJson(true)) })
        n.broadcast(msg)
        phase = "runway"
        game.go(RunwayScene(game, this))
    }

    fun vote(game: Game, targetId: Int) {
        myVote = targetId
        me.votedFor = targetId
        if (net == null) { tallyAndShow(game); return }
        if (net.isHost) { votesIn.add(me.id); hostCheckVotes(game) }
        else net.send(JSONObject().put("t", "vote").put("v", targetId))
    }

    fun hostCheckVotes(game: Game, force: Boolean = false) {
        val n = net ?: return
        if (resultsSent) return
        if (!force && contestants.any { !it.ai && it.id !in votesIn && n.players.containsKey(it.id) }) return
        resultsSent = true
        tally()
        val v = JSONObject(); for (c in contestants) v.put(c.id.toString(), c.votes)
        n.broadcast(JSONObject().put("t", "results").put("v", v))
        showResults(game)
    }

    private fun tallyAndShow(game: Game) { tally(); showResults(game) }

    /** Humans vote, AI contestants vote, and a panel of audience judges vote. */
    private fun tally() {
        for (c in contestants) c.votes = 0
        for (c in contestants) if (!c.ai && c.votedFor >= 0) byId(c.votedFor)?.let { it.votes++ }
        val scores = contestants.map { it.score?.total ?: 0 }
        val r = Random(seed xor 0x5eed)
        for ((i, c) in contestants.withIndex()) if (c.ai) contestants[Scoring.aiVote(scores, i, r, 8f)].votes++
        repeat(judges) { contestants[Scoring.aiVote(scores, -1, r, 10f)].votes++ }
    }

    private fun showResults(game: Game) {
        phase = "results"
        game.go(ResultsScene(game, this))
    }

    /** Handles network messages for the whole round. Installed by whichever scene is active. */
    fun onNet(game: Game, type: String, m: JSONObject, from: Int) {
        when (type) {
            "pos" -> {
                val id = if (net?.isHost == true) from else m.optInt("id")
                val c = byId(id) ?: return
                if (c.local) return
                c.x = m.optDouble("x").toFloat(); c.y = m.optDouble("y").toFloat(); c.moving = m.optBoolean("mv"); c.flip = m.optBoolean("f")
                m.optJSONArray("o")?.let { c.outfit = Outfit.fromJson(it) }
                if (net?.isHost == true) { m.put("id", id); net.broadcast(m) }
            }
            "done" -> if (net?.isHost == true) {
                val c = byId(from) ?: return
                c.outfit = Outfit.fromJson(m.optJSONArray("o")); c.done = true
                hostCheckRunway(game)
            }
            "runway" -> if (net?.isHost == false && phase != "runway") {
                val a = m.getJSONArray("c")
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val c = byId(o.getInt("id")) ?: continue
                    if (!c.local || !c.done) c.outfit = Outfit.fromJson(o.optJSONArray("o"))
                }
                if (!me.done) me.done = true
                scoreAll()
                phase = "runway"
                game.go(RunwayScene(game, this))
            }
            "vote" -> if (net?.isHost == true) {
                byId(from)?.votedFor = m.optInt("v", -1)
                votesIn.add(from)
                if (phase == "vote") hostCheckVotes(game)
            }
            "results" -> if (net?.isHost == false) {
                val v = m.getJSONObject("v")
                for (c in contestants) c.votes = v.optInt(c.id.toString())
                showResults(game)
            }
            "lobby_again" -> if (net?.isHost == false) game.go(LobbyScene(game, net, joined = true))
            "_leave" -> {
                if (net?.isHost == true) {
                    contestants.firstOrNull { it.id == from }?.let { it.done = true; votesIn.add(it.id) }
                    if (phase == "shop") hostCheckRunway(game)
                    if (phase == "vote") hostCheckVotes(game)
                }
            }
            "_hostgone" -> { game.toast("The host left the game"); net?.close(); game.net = null; game.go(MenuScene(game)) }
        }
    }

    fun attach(game: Game) {
        net?.handler = { t, m, from -> onNet(game, t, m, from) }
    }

    /** Host safety nets so one slow phone cannot stall the room. */
    fun hostTick(game: Game, dt: Float) {
        if (net?.isHost != true) return
        if (phase == "shop" && me.done) { runwayDeadline += dt; if (runwayDeadline > 25f) hostCheckRunway(game, force = true) }
        if (phase == "vote" && me.votedFor >= 0) { voteDeadline += dt; if (voteDeadline > 25f) hostCheckVotes(game, force = true) }
    }
}
