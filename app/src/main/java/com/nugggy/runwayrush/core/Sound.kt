package com.nugggy.runwayrush.core

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { CLICK, PICK, BEEP, GO, SHUTTER, CHEER, WHOOSH, FANFARE, VOTE, DROP }
enum class Track { MENU, SHOP, RUNWAY }

/**
 * Every sound is synthesised at first launch and cached as WAV, so the game ships no audio files.
 */
class Sound {
    private val rate = 22050
    private var pool: SoundPool? = null
    private val ids = HashMap<Sfx, Int>()
    private var player: MediaPlayer? = null
    private var current: Track? = null
    private var wanted: Track? = null
    private val files = HashMap<Track, File>()
    @Volatile private var ready = false
    var enabled = true
        set(v) { field = v; if (!v) player?.pause() else { val t = wanted; current = null; if (t != null) music(t) } }
    private var paused = false
    private lateinit var dir: File

    fun bind(cacheDir: File) { dir = File(cacheDir, "audio_v2").apply { mkdirs() } }

    fun init() {
        Thread({
            try {
                val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                val sp = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
                for (s in Sfx.values()) {
                    val f = File(dir, "sfx_${s.name}.wav")
                    if (!f.exists()) writeWav(f, synthSfx(s))
                    ids[s] = sp.load(f.absolutePath, 1)
                }
                pool = sp
                for (t in Track.values()) {
                    val f = File(dir, "music_${t.name}.wav")
                    if (!f.exists()) writeWav(f, synthMusic(t))
                    files[t] = f
                }
                ready = true
                wanted?.let { w -> current = null; music(w) }
            } catch (e: Exception) {
                android.util.Log.e("RunwayRush", "audio init failed", e)
            }
        }, "audio-synth").start()
    }

    fun play(s: Sfx, vol: Float = 0.8f, rateMul: Float = 1f) {
        if (!enabled) return
        val id = ids[s] ?: return
        pool?.play(id, vol, vol, 1, 0, rateMul)
    }

    @Synchronized
    fun music(t: Track) {
        wanted = t
        if (!ready || !enabled || current == t) return
        current = t
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(files[t]!!.absolutePath)
                isLooping = true
                setVolume(0.45f, 0.45f)
                prepare()
                if (!paused) start()
            }
        } catch (e: Exception) { android.util.Log.e("RunwayRush", "music", e) }
    }

    fun update(dt: Float) {}

    fun pause() { paused = true; try { player?.pause() } catch (_: Exception) {} }
    fun resume() { paused = false; try { if (enabled) player?.start() } catch (_: Exception) {} }
    fun release() { try { player?.release(); pool?.release() } catch (_: Exception) {}; player = null; pool = null }

    // ---------------------------------------------------------------- synthesis

    private fun note(n: Int): Double = 440.0 * Math.pow(2.0, (n - 69) / 12.0)

    private fun synthSfx(s: Sfx): FloatArray {
        val rnd = Random(s.ordinal)
        fun buf(sec: Double) = FloatArray((sec * rate).toInt())
        return when (s) {
            Sfx.CLICK -> buf(0.05).also { b -> for (i in b.indices) { val t = i.toDouble() / rate; b[i] = (sin(2 * PI * 1800 * t) * exp(-t * 90)).toFloat() * 0.5f } }
            Sfx.PICK -> buf(0.18).also { b -> for (i in b.indices) { val t = i.toDouble() / rate; val f = 660 + 900 * t / 0.18; b[i] = (sin(2 * PI * f * t) * exp(-t * 18) * 0.5).toFloat() } }
            Sfx.DROP -> buf(0.16).also { b -> for (i in b.indices) { val t = i.toDouble() / rate; val f = 700 - 1200 * t; b[i] = (sin(2 * PI * f * t) * exp(-t * 20) * 0.45).toFloat() } }
            Sfx.BEEP -> buf(0.15).also { b -> for (i in b.indices) { val t = i.toDouble() / rate; b[i] = (sq(880 * t) * 0.25 * exp(-t * 10)).toFloat() } }
            Sfx.GO -> buf(0.5).also { b -> for (i in b.indices) { val t = i.toDouble() / rate; b[i] = (sq(1320 * t) * 0.25 * exp(-t * 4)).toFloat() } }
            Sfx.SHUTTER -> buf(0.12).also { b -> for (i in b.indices) { val t = i.toDouble() / rate; val env = if (t < 0.02) 1.0 else if (t < 0.05) 0.2 else if (t < 0.07) 0.8 else 0.0; b[i] = ((rnd.nextDouble() * 2 - 1) * env * 0.5 * exp(-t * 20)).toFloat() } }
            Sfx.WHOOSH -> buf(0.5).also { b -> var lp = 0.0; for (i in b.indices) { val t = i.toDouble() / rate; val a = 0.02 + 0.25 * sin(PI * t / 0.5); lp += a * ((rnd.nextDouble() * 2 - 1) - lp); b[i] = (lp * sin(PI * t / 0.5) * 1.2).toFloat() } }
            Sfx.CHEER -> buf(2.2).also { b ->
                var lp = 0.0
                for (i in b.indices) {
                    val t = i.toDouble() / rate
                    val env = (t / 0.3).coerceAtMost(1.0) * exp(-(t - 0.3).coerceAtLeast(0.0) * 1.3)
                    lp += 0.35 * ((rnd.nextDouble() * 2 - 1) - lp)
                    val clap = if ((i / 900) % 3 == 0 && rnd.nextDouble() < 0.4) (rnd.nextDouble() * 2 - 1) * 0.5 else 0.0
                    b[i] = ((lp * 0.8 + clap) * env * 0.6).toFloat()
                }
            }
            Sfx.FANFARE -> {
                val seq = intArrayOf(72, 76, 79, 84)
                val b = buf(1.6)
                for ((k, n) in seq.withIndex()) {
                    val st = k * 0.14; val len = if (k == 3) 0.9 else 0.14
                    val f = note(n)
                    for (i in (st * rate).toInt() until ((st + len) * rate).toInt().coerceAtMost(b.size)) {
                        val t = i.toDouble() / rate - st
                        b[i] += ((saw(f * t) * 0.5 + sin(2 * PI * f * 2 * t) * 0.2) * exp(-t * 2.5) * 0.35).toFloat()
                    }
                }
                b
            }
            Sfx.VOTE -> buf(0.3).also { b -> for (i in b.indices) { val t = i.toDouble() / rate; val f = if (t < 0.1) 988.0 else 1319.0; b[i] = (sin(2 * PI * f * t) * exp(-(t % 0.1) * 12) * 0.4).toFloat() } }
        }
    }

    private fun sq(ph: Double) = if ((ph % 1.0) < 0.5) 1.0 else -1.0
    private fun saw(ph: Double) = 2 * (ph % 1.0) - 1

    private fun synthMusic(t: Track): FloatArray {
        val bpm = when (t) { Track.MENU -> 100.0; Track.SHOP -> 118.0; Track.RUNWAY -> 124.0 }
        val beat = 60.0 / bpm
        val bars = 8
        val total = (bars * 4 * beat * rate).toInt()
        val out = FloatArray(total)
        val rnd = Random(7 + t.ordinal)
        // chord roots (midi) per bar
        val prog = when (t) {
            Track.MENU -> intArrayOf(57, 53, 60, 55, 57, 53, 60, 55)   // Am F C G
            Track.SHOP -> intArrayOf(60, 57, 53, 55, 60, 57, 53, 55)   // C Am F G
            Track.RUNWAY -> intArrayOf(50, 50, 46, 48, 50, 50, 46, 45) // Dm Dm Bb C
        }
        val minor = when (t) { Track.MENU -> booleanArrayOf(true, false, false, false, true, false, false, false)
            Track.SHOP -> booleanArrayOf(false, true, false, false, false, true, false, false)
            Track.RUNWAY -> booleanArrayOf(true, true, false, false, true, true, false, false) }

        fun add(start: Double, len: Double, gen: (Double) -> Double) {
            val s = (start * rate).toInt(); val e = ((start + len) * rate).toInt().coerceAtMost(total)
            for (i in s.coerceAtLeast(0) until e) out[i] += gen(i.toDouble() / rate - start).toFloat()
        }

        for (bar in 0 until bars) {
            val root = prog[bar]
            val third = if (minor[bar]) 3 else 4
            val chord = intArrayOf(root, root + third, root + 7, root + 12)
            for (b in 0 until 4) {
                val bt = (bar * 4 + b) * beat
                // kick
                if (t != Track.MENU || b % 2 == 0) add(bt, 0.25) { x -> sin(2 * PI * (50 + 90 * exp(-x * 30)) * x) * exp(-x * 9) * 0.55 }
                // snare / clap on 2 and 4
                if (b % 2 == 1) add(bt, 0.2) { x -> (rnd.nextDouble() * 2 - 1) * exp(-x * 18) * 0.22 + sin(2 * PI * 190 * x) * exp(-x * 25) * 0.1 }
                // hats on off-beats
                add(bt + beat / 2, 0.06) { x -> (rnd.nextDouble() * 2 - 1) * exp(-x * 70) * (if (t == Track.MENU) 0.06 else 0.1) }
                if (t == Track.RUNWAY) add(bt + beat / 4 * 3, 0.04) { x -> (rnd.nextDouble() * 2 - 1) * exp(-x * 90) * 0.05 }
                // bass
                val bn = note(root - 12)
                val bassPattern = if (t == Track.RUNWAY) doubleArrayOf(0.0, 0.5) else doubleArrayOf(0.0)
                for (o in bassPattern) add(bt + o * beat, beat * 0.45) { x -> (saw(bn * x) * 0.5 + sin(2 * PI * bn * x) * 0.5) * exp(-x * 4) * 0.22 }
                // arpeggio
                for (k in 0 until 2) {
                    val n = chord[(b * 2 + k + bar) % 4] + 12
                    val f = note(n)
                    add(bt + k * beat / 2, beat * 0.45) { x ->
                        val v = if (t == Track.MENU) sin(2 * PI * f * x) + 0.3 * sin(2 * PI * f * 2 * x) else sq(f * x) * 0.35 + sin(2 * PI * f * x) * 0.5
                        v * exp(-x * 6) * 0.08
                    }
                }
            }
            // pad
            add(bar * 4 * beat, 4 * beat) { x ->
                var v = 0.0
                for (n in chord.take(3)) { val f = note(n); v += sin(2 * PI * f * x + sin(2 * PI * 0.3 * x)) }
                v * 0.025 * (1 - exp(-x * 3))
            }
        }
        // soft clip
        for (i in out.indices) out[i] = kotlin.math.tanh(out[i] * 1.2).toFloat() * 0.85f
        return out
    }

    private fun writeWav(f: File, data: FloatArray) {
        val bb = ByteBuffer.allocate(44 + data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()); bb.putInt(36 + data.size * 2); bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()); bb.putInt(16); bb.putShort(1); bb.putShort(1); bb.putInt(rate); bb.putInt(rate * 2); bb.putShort(2); bb.putShort(16)
        bb.put("data".toByteArray()); bb.putInt(data.size * 2)
        for (v in data) bb.putShort((v.coerceIn(-1f, 1f) * 32000).toInt().toShort())
        val tmp = File(f.parentFile, f.name + ".tmp")
        FileOutputStream(tmp).use { it.write(bb.array()) }
        tmp.renameTo(f)
    }
}
