package com.nugggy.runwayrush.model

import kotlin.math.exp
import kotlin.random.Random

class ScoreBreakdown(val theme: Int, val complete: Int, val accessories: Int, val colour: Int) {
    val total get() = (theme + complete + accessories + colour).coerceIn(0, 100)
}

object Scoring {
    private fun weights(t: Theme) = t.tags.mapIndexed { i, tag -> tag to (3.2f - i * 0.45f).coerceAtLeast(1f) }

    /** How well one item suits the theme, roughly 0 to 10. */
    fun itemFit(item: Item, t: Theme): Float {
        var s = 0f
        for ((tag, w) in weights(t)) if (tag in item.tags) s += w
        return s
    }

    fun score(o: Outfit, t: Theme): ScoreBreakdown {
        val items = o.all()
        if (items.isEmpty()) return ScoreBreakdown(0, 0, 0, 0)
        val ws = weights(t)
        val maxW = ws.sumOf { it.second.toDouble() }.toFloat()
        // coverage: how many of the theme's ideas appear somewhere in the outfit
        var cover = 0f
        for ((tag, w) in ws) if (items.any { tag in it.tags }) cover += w
        // average fit of the key pieces
        val avg = items.map { (itemFit(it, t) / 6f).coerceAtMost(1f) * slotWeight(it.slot) }.sum() / items.sumOf { slotWeight(it.slot).toDouble() }.toFloat()
        val theme = ((cover / maxW) * 34f + avg * 26f).toInt()

        val hasBase = o.get(Slot.DRESS) != null || (o.get(Slot.TOP) != null && o.get(Slot.BOTTOM) != null)
        val complete = (if (hasBase) 12 else if (o.get(Slot.TOP) != null || o.get(Slot.BOTTOM) != null) 5 else 0) + (if (o.get(Slot.SHOES) != null) 6 else 0)
        val accSlots = listOf(Slot.HAT, Slot.GLASSES, Slot.NECK, Slot.EARS, Slot.BAG, Slot.OUTER, Slot.BACK).count { o.get(it) != null }
        val accessories = when (accSlots) { 0 -> 0; 1 -> 4; 2 -> 7; 3 -> 9; 4 -> 10; 5 -> 9; else -> 7 }
        // colour harmony: fewer colour families reads as a deliberate look
        val families = items.map { family(it) }.toSet().size
        val colour = when { families <= 1 -> 6; families == 2 -> 12; families == 3 -> 10; families == 4 -> 6; else -> 3 } +
            if ("colourful" in t.tags && families >= 4) 6 else 0
        return ScoreBreakdown(theme, complete, accessories, colour.coerceAtMost(12))
    }

    private fun slotWeight(s: Slot) = when (s) { Slot.DRESS -> 2.2f; Slot.TOP, Slot.BOTTOM, Slot.OUTER -> 1.4f; Slot.SHOES -> 1f; else -> 0.7f }

    private val FAMILIES = listOf("black", "white", "grey", "blue", "green", "yellow", "gold", "silver", "orange", "pink", "red", "purple", "brown")
    private fun family(i: Item) = FAMILIES.firstOrNull { it in i.colour.tags } ?: "other"

    /** Dresses an AI contestant. Higher [skill] means more on-theme choices. */
    fun aiOutfit(t: Theme, r: Random, skill: Float): Outfit {
        val o = Outfit()
        fun pick(slot: Slot, samples: Int): Item? {
            val pool = Catalog.bySlot[slot] ?: return null
            var best: Item? = null; var bestS = -1f
            repeat(samples) {
                val it = pool[r.nextInt(pool.size)]
                val s = itemFit(it, t) * skill + r.nextFloat() * 4f
                if (s > bestS) { bestS = s; best = it }
            }
            return best
        }
        val n = (3 + skill * 22).toInt()
        val dress = pick(Slot.DRESS, n)
        val top = pick(Slot.TOP, n); val bottom = pick(Slot.BOTTOM, n)
        if (dress != null && top != null && bottom != null && itemFit(dress, t) * 1.6f >= (itemFit(top, t) + itemFit(bottom, t)) * 0.6f + r.nextFloat() * 2f) o.wear(dress)
        else { top?.let { o.wear(it) }; bottom?.let { o.wear(it) } }
        pick(Slot.SHOES, n)?.let { o.wear(it) }
        for (s in listOf(Slot.HAT, Slot.GLASSES, Slot.NECK, Slot.EARS, Slot.BAG, Slot.OUTER, Slot.BACK)) {
            val cand = pick(s, n / 2 + 2) ?: continue
            val chance = (0.05f + itemFit(cand, t) / 14f).coerceAtMost(0.5f) * (if (s == Slot.BACK) 0.4f else 1f)
            if (r.nextFloat() < chance) o.wear(cand)
        }
        return o
    }

    /** Index of the contestant an AI judge votes for. */
    fun aiVote(scores: List<Int>, exclude: Int, r: Random, temperature: Float = 9f): Int {
        val ws = scores.mapIndexed { i, s -> if (i == exclude) 0.0 else exp((s - (scores.maxOrNull() ?: 0)) / temperature.toDouble()) }
        val sum = ws.sum()
        if (sum <= 0) return (0 until scores.size).firstOrNull { it != exclude } ?: 0
        var x = r.nextDouble() * sum
        for ((i, w) in ws.withIndex()) { x -= w; if (x <= 0) return i }
        return ws.indices.last { it != exclude }
    }

    val AI_NAMES = listOf("Ava", "Mia", "Zara", "Leo", "Kai", "Ruby", "Isla", "Noah", "Ivy", "Jasper", "Luna", "Oscar", "Willow", "Felix",
        "Sienna", "Theo", "Pearl", "Hugo", "Matilda", "Ari", "Nova", "Ezra", "Olive", "Remy", "Indi", "Billie", "Sage", "Frankie", "Coco", "Jules")
}
