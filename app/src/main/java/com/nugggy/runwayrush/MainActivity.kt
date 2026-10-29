package com.nugggy.runwayrush

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.EditText
import com.nugggy.runwayrush.core.Game
import com.nugggy.runwayrush.core.GameView

class MainActivity : Activity() {

    lateinit var game: Game
    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        game = Game(this)
        view = GameView(this, game)
        setContentView(view)
        hideSystemUi()
        game.start()
    }

    private fun hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onResume() {
        super.onResume()
        game.sound.resume()
    }

    override fun onPause() {
        super.onPause()
        game.sound.pause()
        game.saveProfile()
    }

    override fun onDestroy() {
        game.shutdown()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        game.post { if (!game.onBack()) runOnUiThread { finish() } }
    }

    /** Opens the system share sheet with a link to the newest APK. */
    fun shareApp() {
        runOnUiThread {
            val text = "Come and play Makeover with Edie with me. Download the latest Android version here: ${com.nugggy.runwayrush.core.Updater.LATEST_APK}"
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Makeover with Edie")
                putExtra(android.content.Intent.EXTRA_TEXT, text)
            }
            try { startActivity(android.content.Intent.createChooser(send, "Share Makeover with Edie")) } catch (_: Exception) {}
        }
    }

    /** Shows a native text prompt. The callback is delivered on the game thread. */
    fun promptText(title: String, initial: String, maxLen: Int, numeric: Boolean, cb: (String?) -> Unit) {
        runOnUiThread {
            val input = EditText(this).apply {
                setText(initial)
                setSelection(initial.length)
                filters = arrayOf(InputFilter.LengthFilter(maxLen))
                inputType = if (numeric) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                setPadding(48, 32, 48, 32)
            }
            AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("OK") { _, _ -> val t = input.text.toString().trim(); game.post { cb(t) } }
                .setNegativeButton("Cancel") { _, _ -> game.post { cb(null) } }
                .setOnCancelListener { game.post { cb(null) } }
                .show()
        }
    }
}
