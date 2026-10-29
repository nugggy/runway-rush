package com.nugggy.runwayrush.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** Everything about how a character looks, independent of clothing. */
data class Avatar(
    var skin: Int = 3,
    var hairStyle: Int = 4,
    var hairColour: Int = 1,
    var eyeColour: Int = 0,
    var eyeShape: Int = 0,
    var brows: Int = 0,
    var lips: Int = 0,
    var blush: Boolean = true,
    var freckles: Boolean = false,
    var facial: Int = 0,
    var body: Int = 1,
    var height: Int = 2,
    var makeup: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("s", skin); put("hs", hairStyle); put("hc", hairColour); put("ec", eyeColour); put("es", eyeShape)
        put("b", brows); put("l", lips); put("bl", blush); put("f", freckles); put("fh", facial); put("bd", body)
        put("h", height); put("m", makeup)
    }

    companion object {
        val SKIN = intArrayOf(0xFFFCE3D3.toInt(), 0xFFF6D0B1.toInt(), 0xFFEBB894.toInt(), 0xFFD9A07A.toInt(), 0xFFC68A5E.toInt(),
            0xFFA96F45.toInt(), 0xFF8D5A36.toInt(), 0xFF6E4126.toInt(), 0xFF4F2D1A.toInt(), 0xFF3A2013.toInt(),
            0xFFB6E3C6.toInt(), 0xFFB9C9F2.toInt())
        val HAIR = intArrayOf(0xFF1A1110.toInt(), 0xFF3B2416.toInt(), 0xFF6B3E22.toInt(), 0xFFA0662E.toInt(), 0xFFD9A85B.toInt(),
            0xFFF1D58C.toInt(), 0xFFB0401F.toInt(), 0xFFE07A3A.toInt(), 0xFFD8D8DC.toInt(), 0xFF8A8A92.toInt(),
            0xFFFF6FAE.toInt(), 0xFF9B5CFF.toInt(), 0xFF3FA9F5.toInt(), 0xFF2FCB8A.toInt(), 0xFFE53950.toInt(), 0xFFFFFFFF.toInt())
        val EYES = intArrayOf(0xFF5B3A1E.toInt(), 0xFF2D1A0E.toInt(), 0xFF3C7BBF.toInt(), 0xFF4E9A5A.toInt(), 0xFF8B9AA8.toInt(),
            0xFFB07A2A.toInt(), 0xFF8A4FD1.toInt(), 0xFFD1406E.toInt())
        val LIPS = intArrayOf(0xFFC97C74.toInt(), 0xFFE0566E.toInt(), 0xFFB0243A.toInt(), 0xFF8C3B5E.toInt(), 0xFFF19AA6.toInt(),
            0xFF6E2A2A.toInt(), 0xFFE07B39.toInt(), 0xFF5A2E7A.toInt())
        val MAKEUP = arrayOf("None", "Soft glam", "Smoky eye", "Glitter", "Winged liner", "Rainbow")
        const val HAIR_STYLES = 22
        val HAIR_NAMES = arrayOf("Buzz cut", "Crop", "Side part", "Quiff", "Bob", "Long straight", "Long waves", "Ponytail",
            "High bun", "Space buns", "Pigtails", "Afro", "Curly bob", "Braids", "Mohawk", "Pixie", "Mullet", "Bald",
            "Shag", "Twin braids", "Top knot", "Locs")
        val FACIAL = arrayOf("None", "Stubble", "Moustache", "Goatee", "Full beard")
        val BODY = arrayOf("Slim", "Average", "Curvy", "Broad", "Petite")
        val BROWS = arrayOf("Soft", "Arched", "Straight", "Bold")
        val EYE_SHAPES = arrayOf("Round", "Almond", "Sleepy", "Lashes")

        fun fromJson(o: JSONObject) = Avatar(
            o.optInt("s", 3), o.optInt("hs", 4), o.optInt("hc", 1), o.optInt("ec"), o.optInt("es"), o.optInt("b"), o.optInt("l"),
            o.optBoolean("bl", true), o.optBoolean("f"), o.optInt("fh"), o.optInt("bd", 1), o.optInt("h", 2), o.optInt("m"))

        fun random(r: Random): Avatar = Avatar(
            skin = r.nextInt(10), hairStyle = r.nextInt(HAIR_STYLES - 1).let { if (it == 17) 5 else it }, hairColour = if (r.nextFloat() < 0.75f) r.nextInt(10) else r.nextInt(HAIR.size),
            eyeColour = r.nextInt(EYES.size), eyeShape = r.nextInt(EYE_SHAPES.size), brows = r.nextInt(BROWS.size), lips = r.nextInt(LIPS.size),
            blush = r.nextBoolean(), freckles = r.nextFloat() < 0.25f, facial = if (r.nextFloat() < 0.25f) r.nextInt(1, FACIAL.size) else 0,
            body = r.nextInt(BODY.size), height = r.nextInt(5), makeup = if (r.nextFloat() < 0.5f) r.nextInt(MAKEUP.size) else 0)
    }
}

/** A set of worn item ids keyed by slot. */
class Outfit {
    val items = HashMap<Slot, Int>()

    fun wear(item: Item) {
        when (item.slot) {
            Slot.DRESS -> { items.remove(Slot.TOP); items.remove(Slot.BOTTOM) }
            Slot.TOP, Slot.BOTTOM -> items.remove(Slot.DRESS)
            else -> {}
        }
        items[item.slot] = item.id
    }

    fun isWearing(item: Item) = items[item.slot] == item.id
    fun remove(slot: Slot) { items.remove(slot) }
    fun get(slot: Slot): Item? = items[slot]?.let { Catalog.byId(it) }
    fun all(): List<Item> = Slot.values().mapNotNull { get(it) }
    fun copy(): Outfit = Outfit().also { it.items.putAll(items) }
    fun clear() = items.clear()

    fun toJson(): JSONArray = JSONArray().apply { for (v in items.values) put(v) }

    companion object {
        fun fromJson(a: JSONArray?): Outfit {
            val o = Outfit()
            if (a != null) for (i in 0 until a.length()) Catalog.byId(a.optInt(i))?.let { o.wear(it) }
            return o
        }
    }
}

/** Saved player data. */
class Profile {
    var name = "Edie"
    var avatar = Avatar()
    var games = 0
    var wins = 0
    var bestScore = 0
    var sound = true
    var created = false
    var seenThemes = HashSet<Int>()
    var lastOutfit = Outfit()

    fun save(ctx: Context) {
        val o = JSONObject()
        o.put("name", name); o.put("avatar", avatar.toJson()); o.put("games", games); o.put("wins", wins)
        o.put("best", bestScore); o.put("sound", sound); o.put("created", created)
        o.put("seen", JSONArray(seenThemes.toList())); o.put("last", lastOutfit.toJson())
        ctx.getSharedPreferences("profile", Context.MODE_PRIVATE).edit().putString("data", o.toString()).apply()
    }

    companion object {
        fun load(ctx: Context): Profile {
            val p = Profile()
            val s = ctx.getSharedPreferences("profile", Context.MODE_PRIVATE).getString("data", null) ?: return p
            try {
                val o = JSONObject(s)
                p.name = o.optString("name", "Edie"); p.avatar = Avatar.fromJson(o.optJSONObject("avatar") ?: JSONObject())
                p.games = o.optInt("games"); p.wins = o.optInt("wins"); p.bestScore = o.optInt("best")
                p.sound = o.optBoolean("sound", true); p.created = o.optBoolean("created")
                o.optJSONArray("seen")?.let { a -> for (i in 0 until a.length()) p.seenThemes.add(a.getInt(i)) }
                p.lastOutfit = Outfit.fromJson(o.optJSONArray("last"))
            } catch (_: Exception) {}
            return p
        }
    }
}
