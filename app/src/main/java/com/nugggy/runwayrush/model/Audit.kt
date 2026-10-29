package com.nugggy.runwayrush.model

import android.util.Log

/** Debug-only check that every theme has enough on-theme stock in each key department. */
object Audit {
    const val GOOD = 4.5f

    fun run() {
        var flagged = 0
        for (t in Themes.all) {
            fun count(s: Slot) = Catalog.bySlot[s]!!.count { Scoring.itemFit(it, t) >= GOOD }
            val top = count(Slot.TOP); val bottom = count(Slot.BOTTOM); val dress = count(Slot.DRESS); val shoes = count(Slot.SHOES)
            val acc = listOf(Slot.HAT, Slot.NECK, Slot.BAG, Slot.GLASSES, Slot.EARS, Slot.BACK, Slot.OUTER).sumOf { count(it) }
            val weak = (minOf(top, bottom) < 6 && dress < 6) || shoes < 4 || acc < 6
            if (weak) flagged++
            Log.i("RRAudit", "%s %-28s top=%d bottom=%d dress=%d shoes=%d extras=%d".format(if (weak) "WEAK" else "ok  ", t.name, top, bottom, dress, shoes, acc))
        }
        val allTags = Catalog.items.flatMap { it.tags }.toSet()
        for (t in Themes.all) for (tag in t.tags) if (tag !in allTags) Log.i("RRAudit", "MISSING tag '$tag' in ${t.name}")
        Log.i("RRAudit", "flagged $flagged of ${Themes.count}")
    }
}

/** Debug-only: renders one item from each of the newest styles into a contact sheet. */
object ContactSheet {
    fun write(dir: java.io.File) {
        val styles = Catalog.STYLES.dropWhile { it.key != "petee" }
        val cols = 9; val cw = 220; val ch = 260
        val rows = (styles.size + cols - 1) / cols
        val bmp = android.graphics.Bitmap.createBitmap(cols * cw, rows * ch, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        c.drawColor(0xFF2E1A47.toInt())
        val tp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = -1; textSize = 18f; textAlign = android.graphics.Paint.Align.CENTER }
        for ((i, st) in styles.withIndex()) {
            val it = Catalog.byStyle[st.key]!!.first()
            val x = (i % cols) * cw; val y = (i / cols) * ch
            com.nugggy.runwayrush.render.AvatarRenderer.icon(c, it, android.graphics.RectF(x + 10f, y + 10f, x + cw - 10f, y + ch - 40f), 0.3f)
            c.drawText(st.name, x + cw / 2f, y + ch - 14f, tp)
        }
        java.io.FileOutputStream(java.io.File(dir, "contact.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
