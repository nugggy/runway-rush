package com.nugggy.runwayrush.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import com.nugggy.runwayrush.BuildConfig
import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.model.Avatar
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue

class NetPlayer(val id: Int, var name: String, var avatar: Avatar)

/** A host that other phones on the same WiFi can find and join. */
data class FoundHost(val name: String, val host: String, val port: Int)

/**
 * Local WiFi multiplayer. One phone hosts (and is the referee), the others join.
 * Messages are JSON objects, one per line, over TCP. Everything is delivered to the game thread via [pump].
 */
class NetSession private constructor(private val game: Game, val isHost: Boolean) {
    companion object {
        const val PORT = 47777
        const val SERVICE = "_runwayrush._tcp."
        const val MAX_PLAYERS = 8

        fun host(game: Game): NetSession = NetSession(game, true).also { it.startHost() }
        fun join(game: Game, host: String, port: Int = PORT): NetSession = NetSession(game, false).also { it.startClient(host, port) }

        fun localIp(): String? = try {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }?.hostAddress
        } catch (e: Exception) { null }
    }

    /** Called on the game thread for each incoming message. from = sender id (host sees client ids, clients see 0). */
    var handler: ((type: String, msg: JSONObject, from: Int) -> Unit)? = null
    var onLobbyChanged: (() -> Unit)? = null

    var myId = if (isHost) 0 else -1; private set
    val players = LinkedHashMap<Int, NetPlayer>()
    @Volatile var connected = false; private set
    @Volatile var error: String? = null; private set
    val ip: String? = localIp()

    private val inbox = ConcurrentLinkedQueue<Pair<Int, JSONObject>>()
    private val peers = ConcurrentHashMap<Int, Peer>()
    private var server: ServerSocket? = null
    private var nextId = 1
    @Volatile private var closed = false
    private var nsd: NsdManager? = null
    private var regListener: NsdManager.RegistrationListener? = null
    private var multicast: WifiManager.MulticastLock? = null

    private inner class Peer(val id: Int, val sock: Socket) {
        val out = LinkedBlockingQueue<String>()
        val writer = Thread({
            try {
                val w = OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8)
                while (!closed && !sock.isClosed) { val line = out.take(); if (line == "\u0000") break; w.write(line); w.write("\n"); w.flush() }
            } catch (_: Exception) {}
        }, "net-w-$id")
        val reader = Thread({
            try {
                val r = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                while (!closed) {
                    val line = r.readLine() ?: break
                    try { inbox.add(id to JSONObject(line)) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            inbox.add(id to JSONObject().put("t", "_gone"))
        }, "net-r-$id")
        fun start() { writer.start(); reader.start() }
        fun send(s: String) { out.add(s) }
        fun close() { out.add("\u0000"); try { sock.close() } catch (_: Exception) {} }
    }

    // ---------------------------------------------------------------- host

    private fun startHost() {
        players[0] = NetPlayer(0, game.profile.name, game.profile.avatar)
        Thread({
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(InetSocketAddress(PORT))
                server = ss
                connected = true
                register()
                while (!closed) {
                    val s = ss.accept()
                    s.tcpNoDelay = true
                    val id = synchronized(this) { nextId++ }
                    val peer = Peer(id, s)
                    peers[id] = peer
                    peer.start()
                }
            } catch (e: Exception) {
                if (!closed) error = "Could not host a game: ${e.message}"
            }
        }, "net-accept").start()
    }

    private fun register() {
        try {
            val ctx = game.activity
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicast = wm.createMulticastLock("runwayrush").apply { setReferenceCounted(false); acquire() }
            val info = NsdServiceInfo().apply { serviceName = "Runway-${game.profile.name}"; serviceType = SERVICE; port = PORT }
            val l = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(i: NsdServiceInfo) {}
                override fun onRegistrationFailed(i: NsdServiceInfo, e: Int) {}
                override fun onServiceUnregistered(i: NsdServiceInfo) {}
                override fun onUnregistrationFailed(i: NsdServiceInfo, e: Int) {}
            }
            nsd = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsd?.registerService(info, NsdManager.PROTOCOL_DNS_SD, l)
            regListener = l
        } catch (_: Exception) {}
    }

    fun lobbyJson(): JSONObject {
        val arr = org.json.JSONArray()
        for (p in players.values) arr.put(JSONObject().put("id", p.id).put("name", p.name).put("av", p.avatar.toJson()))
        return JSONObject().put("t", "lobby").put("players", arr)
    }

    fun broadcastLobby() { if (isHost) broadcast(lobbyJson()) }

    /** Host: send to everyone. Client: send to host. */
    fun send(msg: JSONObject) {
        val s = msg.toString()
        if (isHost) {
            for (p in peers.values) if (players.containsKey(p.id)) p.send(s)
        } else {
            peers[0]?.send(s)
        }
    }

    fun broadcast(msg: JSONObject) = send(msg)

    fun sendTo(id: Int, msg: JSONObject) { peers[id]?.send(msg.toString()) }

    fun kickAll() { for (p in peers.values) p.close(); peers.clear() }

    // ---------------------------------------------------------------- client

    private fun startClient(host: String, port: Int) {
        Thread({
            try {
                val s = Socket()
                s.connect(InetSocketAddress(host, port), 6000)
                s.tcpNoDelay = true
                val peer = Peer(0, s)
                peers[0] = peer
                peer.start()
                connected = true
                peer.send(JSONObject().put("t", "hello").put("name", game.profile.name).put("av", game.profile.avatar.toJson())
                    .put("ver", BuildConfig.VERSION_NAME).toString())
            } catch (e: Exception) {
                error = "Could not connect to $host"
            }
        }, "net-connect").start()
    }

    // ---------------------------------------------------------------- pump (game thread)

    fun pump() {
        while (true) {
            val (from, m) = inbox.poll() ?: break
            val t = m.optString("t")
            if (isHost) {
                when (t) {
                    "hello" -> {
                        if (m.optString("ver") != BuildConfig.VERSION_NAME) {
                            sendTo(from, JSONObject().put("t", "reject").put("why", "Version mismatch. Host has v${BuildConfig.VERSION_NAME}. Please update both phones."))
                            continue
                        }
                        if (players.size >= MAX_PLAYERS) { sendTo(from, JSONObject().put("t", "reject").put("why", "The game is full")); continue }
                        players[from] = NetPlayer(from, m.optString("name", "Guest").take(14), Avatar.fromJson(m.optJSONObject("av") ?: JSONObject()))
                        sendTo(from, JSONObject().put("t", "welcome").put("id", from))
                        broadcastLobby()
                        onLobbyChanged?.invoke()
                        handler?.invoke("_join", m, from)
                        continue
                    }
                    "_gone" -> {
                        peers.remove(from)?.close()
                        if (players.remove(from) != null) { broadcastLobby(); onLobbyChanged?.invoke(); handler?.invoke("_leave", m, from) }
                        continue
                    }
                }
                if (!players.containsKey(from)) continue
            } else {
                when (t) {
                    "welcome" -> { myId = m.optInt("id"); continue }
                    "reject" -> { error = m.optString("why"); close(); continue }
                    "lobby" -> {
                        players.clear()
                        val a = m.optJSONArray("players")
                        if (a != null) for (i in 0 until a.length()) { val o = a.getJSONObject(i); players[o.getInt("id")] = NetPlayer(o.getInt("id"), o.optString("name"), Avatar.fromJson(o.optJSONObject("av") ?: JSONObject())) }
                        onLobbyChanged?.invoke()
                        continue
                    }
                    "_gone" -> { if (!closed) error = "Lost connection to the host"; connected = false; handler?.invoke("_hostgone", m, 0); continue }
                }
            }
            handler?.invoke(t, m, from)
        }
    }

    fun close() {
        if (closed) return
        closed = true
        connected = false
        try { regListener?.let { nsd?.unregisterService(it) } } catch (_: Exception) {}
        try { multicast?.release() } catch (_: Exception) {}
        for (p in peers.values) p.close()
        peers.clear()
        try { server?.close() } catch (_: Exception) {}
    }
}

/** Looks for hosts on the local network. */
class HostFinder(private val ctx: Context) {
    val found = ConcurrentHashMap<String, FoundHost>()
    private var nsd: NsdManager? = null
    private var listener: NsdManager.DiscoveryListener? = null
    private var lock: WifiManager.MulticastLock? = null

    fun start() {
        try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            lock = wm.createMulticastLock("runwayrush-find").apply { setReferenceCounted(false); acquire() }
            val m = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsd = m
            val l = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(t: String) {}
                override fun onDiscoveryStopped(t: String) {}
                override fun onStartDiscoveryFailed(t: String, e: Int) {}
                override fun onStopDiscoveryFailed(t: String, e: Int) {}
                override fun onServiceLost(i: NsdServiceInfo) { found.remove(i.serviceName) }
                override fun onServiceFound(i: NsdServiceInfo) {
                    if (!i.serviceType.contains("runwayrush")) return
                    @Suppress("DEPRECATION")
                    try {
                        m.resolveService(i, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(s: NsdServiceInfo, e: Int) {}
                            override fun onServiceResolved(s: NsdServiceInfo) {
                                val h = s.host?.hostAddress ?: return
                                found[s.serviceName] = FoundHost(s.serviceName.removePrefix("Runway-"), h, s.port)
                            }
                        })
                    } catch (_: Exception) {}
                }
            }
            listener = l
            m.discoverServices(NetSession.SERVICE, NsdManager.PROTOCOL_DNS_SD, l)
        } catch (_: Exception) {}
    }

    fun stop() {
        try { listener?.let { nsd?.stopServiceDiscovery(it) } } catch (_: Exception) {}
        try { lock?.release() } catch (_: Exception) {}
        listener = null
    }
}
