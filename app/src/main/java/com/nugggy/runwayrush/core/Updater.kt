package com.nugggy.runwayrush.core

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import com.nugggy.runwayrush.BuildConfig
import com.nugggy.runwayrush.scene.MenuScene
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Update channel: asks GitHub Releases for the newest version when the app starts,
 * then downloads and installs the APK if the player agrees.
 */
class Updater(private val game: Game) {
    companion object {
        const val REPO = "nugggy/runway-rush"
        const val RELEASES = "https://github.com/$REPO/releases"
        const val APK_NAME = "MakeoverWithEdie.apk"
        /** Always resolves to the APK attached to the newest release. */
        const val LATEST_APK = "https://github.com/$REPO/releases/latest/download/$APK_NAME"
    }

    enum class State { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, READY, FAILED }

    @Volatile var state = State.IDLE; private set
    @Volatile var latest: String? = null; private set
    @Volatile private var apkUrl: String? = null
    @Volatile var notes: String = ""; private set
    private var downloadId = -1L
    private var pollT = 0f
    private var pausedFor = 0f
    var progress = 0f; private set
    private val banner = RectF()
    private val laterBtn = RectF()
    private var dismissed = false
    private var anim = 0f

    fun check() {
        if (state == State.CHECKING) return
        state = State.CHECKING
        Thread({
            try {
                val con = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
                con.connectTimeout = 8000; con.readTimeout = 8000
                con.setRequestProperty("Accept", "application/vnd.github+json")
                con.setRequestProperty("User-Agent", "MakeoverWithEdie/${BuildConfig.VERSION_NAME}")
                if (con.responseCode != 200) { state = if (con.responseCode == 404) State.CURRENT else State.FAILED; return@Thread }
                val body = con.inputStream.bufferedReader().use { it.readText() }
                val o = JSONObject(body)
                val tag = o.optString("tag_name").removePrefix("v").trim()
                notes = o.optString("name", "")
                val assets = o.optJSONArray("assets")
                var url: String? = null
                if (assets != null) for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) { url = a.optString("browser_download_url"); break }
                }
                latest = tag
                apkUrl = url
                state = if (newer(tag, BuildConfig.VERSION_NAME) && url != null) State.AVAILABLE else State.CURRENT
            } catch (e: Exception) {
                state = State.FAILED
            }
        }, "update-check").start()
    }

    private fun newer(a: String, b: String): Boolean {
        val x = a.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val y = b.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val p = x.getOrElse(i) { 0 }; val q = y.getOrElse(i) { 0 }
            if (p != q) return p > q
        }
        return false
    }

    private fun startDownload() {
        val url = apkUrl ?: return
        val act = game.activity
        if (android.os.Build.VERSION.SDK_INT >= 26 && !act.packageManager.canRequestPackageInstalls()) {
            game.toast("Allow installs from this app, then tap Update again")
            act.runOnUiThread {
                try { act.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${act.packageName}"))) } catch (_: Exception) {}
            }
            return
        }
        try {
            val dm = act.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle("Makeover with Edie ${latest ?: ""}")
                .setDescription("Downloading update")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            act.getExternalFilesDir(null)?.let { req.setDestinationInExternalFilesDir(act, null, "update-${latest}.apk") }
            downloadId = dm.enqueue(req)
            state = State.DOWNLOADING
        } catch (e: Exception) {
            game.toast("Download failed. Opening the releases page.")
            openReleases()
        }
    }

    private fun poll() {
        if (downloadId < 0) return
        val dm = game.activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.query(DownloadManager.Query().setFilterById(downloadId))?.use { cur ->
            if (!cur.moveToFirst()) return
            val st = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            if (total > 0) progress = done.toFloat() / total
            if (st == DownloadManager.STATUS_PAUSED) {
                pausedFor += 0.5f
                if (pausedFor > 20f) {
                    // flaky network: hand over to the browser rather than sitting at a stuck percentage
                    dm.remove(downloadId); downloadId = -1; pausedFor = 0f
                    state = State.AVAILABLE
                    game.toast("Download stalled. Opening it in your browser.")
                    val act = game.activity
                    act.runOnUiThread { try { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LATEST_APK))) } catch (_: Exception) {} }
                }
            } else pausedFor = 0f
            when (st) {
                DownloadManager.STATUS_SUCCESSFUL -> { state = State.READY; install(dm) }
                DownloadManager.STATUS_FAILED -> { state = State.FAILED; game.toast("Update download failed") }
            }
        }
    }

    private fun install(dm: DownloadManager) {
        val uri = dm.getUriForDownloadedFile(downloadId) ?: return
        val act = game.activity
        act.runOnUiThread {
            try {
                act.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) { openReleases() }
        }
    }

    fun openReleases() {
        val act = game.activity
        act.runOnUiThread { try { act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES))) } catch (_: Exception) {} }
    }

    fun statusText(): String = when (state) {
        State.IDLE, State.CHECKING -> "Checking for updates..."
        State.CURRENT -> "Up to date (v${BuildConfig.VERSION_NAME})"
        State.AVAILABLE -> "Update v$latest available"
        State.DOWNLOADING -> "Downloading update ${(progress * 100).toInt()}%"
        State.READY -> "Update downloaded"
        State.FAILED -> "Could not check for updates (v${BuildConfig.VERSION_NAME})"
    }

    private fun bannerVisible() = game.scene is MenuScene && !dismissed && (state == State.AVAILABLE || state == State.DOWNLOADING || state == State.READY)

    fun draw(c: Canvas, dt: Float) {
        if (state == State.DOWNLOADING) { pollT += dt; if (pollT > 0.5f) { pollT = 0f; poll() } }
        val vis = bannerVisible()
        anim = (anim + if (vis) dt * 4 else -dt * 4).coerceIn(0f, 1f)
        if (anim <= 0f) return
        val w = 560f; val h = 92f
        val y = -h + (h + 18f) * easeOut(anim)
        banner.set(game.w / 2 - w / 2, y, game.w / 2 + w / 2, y + h)
        Ui.panel(c, banner, 0xF0FFD86B.toInt(), 24f, 0)
        Icons.draw(c, "download", banner.left + 46f, banner.centerY(), 40f, Pal.INK)
        val title = when (state) { State.DOWNLOADING -> "Downloading v$latest  ${(progress * 100).toInt()}%"; State.READY -> "Ready to install"; else -> "New version v$latest is here" }
        Ui.text(c, title, banner.left + 84f, banner.top + 40f, 28f, Pal.INK, shadow = false)
        Ui.text(c, if (state == State.AVAILABLE) "Tap to update now" else "Installing will keep your saved look", banner.left + 84f, banner.top + 72f, 20f, 0xFF5A4060.toInt(), tf = Ui.regular, shadow = false)
        laterBtn.set(banner.right - 70f, banner.top + 16f, banner.right - 14f, banner.bottom - 16f)
        Icons.draw(c, "close", laterBtn.centerX(), laterBtn.centerY(), 30f, 0xFF5A4060.toInt())
    }

    fun touch(t: Touch): Boolean {
        if (!bannerVisible() || t.type != TouchType.UP) return bannerVisible() && banner.contains(t.x, t.y)
        if (laterBtn.contains(t.x, t.y)) { dismissed = true; return true }
        if (banner.contains(t.x, t.y)) {
            game.sound.play(Sfx.CLICK)
            when (state) { State.AVAILABLE -> startDownload(); State.READY -> install(game.activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager); else -> {} }
            return true
        }
        return false
    }

    private fun easeOut(x: Float) = 1 - (1 - x) * (1 - x)
}
