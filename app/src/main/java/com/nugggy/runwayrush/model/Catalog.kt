package com.nugggy.runwayrush.model

import kotlin.random.Random

enum class Slot(val title: String) {
    TOP("Tops"), BOTTOM("Bottoms"), DRESS("Dresses & Suits"), OUTER("Outerwear"), SHOES("Shoes"),
    HAT("Hats & Headwear"), GLASSES("Eyewear"), NECK("Necklaces & Ties"), EARS("Earrings"), BAG("Bags"), BACK("Wings & Capes")
}

class Colour(val name: String, val rgb: Int, val tags: List<String>)

class Style(val slot: Slot, val key: String, val name: String, val tags: List<String>, val count: Int,
            val fabrics: List<String> = Catalog.F_SOFT, val colours: List<Int>? = null)

class Item(val id: Int, val style: Style, val colour: Colour, val accent: Colour, val fabric: String, val name: String, val tags: Set<String>) {
    val slot get() = style.slot
    val price: Int = 20 + (id * 37 % 180)
}

/**
 * The shop's stock. Generated deterministically, so every device running the same version
 * agrees on item ids (multiplayer sends ids, not drawings).
 */
object Catalog {
    val F_SOFT = listOf("solid", "solid", "solid", "stripes", "dots", "check", "hearts", "stars", "silk", "satin", "linen", "knit",
        "floral", "gingham", "lace", "sheer", "velvet", "sequin", "glitter", "tiedye", "leopard", "paisley", "chevron", "zebra")
    val F_TOUGH = listOf("solid", "solid", "denim", "denim", "leather", "camo", "tartan", "corduroy", "tweed", "snakeskin", "quilted",
        "pinstripe", "houndstooth", "check", "stripes", "leopard")
    val F_FORMAL = listOf("solid", "solid", "silk", "satin", "velvet", "sequin", "lace", "gold", "glitter", "sheer", "pinstripe", "floral")
    val F_SHOE = listOf("solid", "solid", "solid", "leather", "leather", "snakeskin", "glitter", "sequin", "velvet", "denim", "leopard", "gold", "satin", "dots", "zebra")
    val F_PLAIN = listOf("solid")
    val F_SPORT = listOf("solid", "solid", "stripes", "mesh", "chevron", "camo", "stars", "tiedye")
    val F_WARM = listOf("solid", "knit", "knit", "fur", "quilted", "tartan", "tweed", "corduroy", "velvet", "check", "stripes", "leather", "houndstooth")

    val FABRIC_TAGS = mapOf(
        "solid" to listOf("sleek"), "stripes" to listOf("casual", "ocean", "preppy"), "dots" to listOf("cute", "retro"),
        "check" to listOf("preppy", "school"), "hearts" to listOf("cute", "romantic"), "stars" to listOf("cute", "space", "party"),
        "chevron" to listOf("retro", "sporty"), "denim" to listOf("casual", "cowboy", "street", "rock"),
        "leather" to listOf("edgy", "punk", "rock", "cowboy"), "silk" to listOf("elegant", "romantic", "formal"),
        "velvet" to listOf("royal", "elegant", "winter", "vintage"), "lace" to listOf("romantic", "wedding", "vintage", "gothic"),
        "sequin" to listOf("sparkly", "party", "disco", "glam"), "tartan" to listOf("punk", "preppy", "winter", "school"),
        "tweed" to listOf("vintage", "business", "preppy"), "knit" to listOf("cosy", "winter", "cute"),
        "fur" to listOf("winter", "glam", "cosy"), "satin" to listOf("elegant", "formal", "glam"),
        "linen" to listOf("summer", "beach", "boho"), "corduroy" to listOf("retro", "vintage", "cosy"),
        "floral" to listOf("romantic", "summer", "boho", "nature"), "leopard" to listOf("glam", "edgy", "wild"),
        "camo" to listOf("military", "adventure", "street"), "gold" to listOf("glam", "royal", "sparkly", "gold"),
        "snakeskin" to listOf("edgy", "glam", "wild"), "houndstooth" to listOf("vintage", "business"),
        "paisley" to listOf("boho", "retro"), "tiedye" to listOf("retro", "boho", "colourful", "festival"),
        "glitter" to listOf("sparkly", "party", "cute"), "mesh" to listOf("sporty", "edgy", "futuristic"),
        "zebra" to listOf("wild", "edgy"), "gingham" to listOf("cute", "picnic", "retro", "cowboy"),
        "sheer" to listOf("romantic", "elegant", "fantasy"), "quilted" to listOf("winter", "cosy"),
        "pinstripe" to listOf("business", "formal"),
    )

    val FABRIC_NAMES = mapOf("solid" to "", "stripes" to "Striped", "dots" to "Polka Dot", "check" to "Checked", "hearts" to "Heart Print",
        "stars" to "Star Print", "chevron" to "Chevron", "denim" to "Denim", "leather" to "Leather", "silk" to "Silk", "velvet" to "Velvet",
        "lace" to "Lace", "sequin" to "Sequin", "tartan" to "Tartan", "tweed" to "Tweed", "knit" to "Knitted", "fur" to "Faux Fur",
        "satin" to "Satin", "linen" to "Linen", "corduroy" to "Corduroy", "floral" to "Floral", "leopard" to "Leopard Print",
        "camo" to "Camo", "gold" to "Gold Lame", "snakeskin" to "Snakeskin", "houndstooth" to "Houndstooth", "paisley" to "Paisley",
        "tiedye" to "Tie-Dye", "glitter" to "Glitter", "mesh" to "Mesh", "zebra" to "Zebra Print", "gingham" to "Gingham",
        "sheer" to "Chiffon", "quilted" to "Quilted", "pinstripe" to "Pinstripe")

    val COLOURS = listOf(
        Colour("Black", 0xFF1E1C22.toInt(), listOf("black", "dark", "formal", "gothic", "edgy", "sleek")),
        Colour("White", 0xFFF7F5F2.toInt(), listOf("white", "wedding", "sleek", "summer", "futuristic")),
        Colour("Ivory", 0xFFF2E8D2.toInt(), listOf("white", "wedding", "vintage", "elegant")),
        Colour("Grey", 0xFF9C9AA3.toInt(), listOf("grey", "business", "sleek", "rain")),
        Colour("Charcoal", 0xFF45434C.toInt(), listOf("grey", "dark", "business", "winter")),
        Colour("Navy", 0xFF1F2E5C.toInt(), listOf("blue", "dark", "ocean", "preppy", "business", "military")),
        Colour("Royal Blue", 0xFF2F55D4.toInt(), listOf("blue", "bright", "royal", "sporty")),
        Colour("Sky Blue", 0xFF8FCBF2.toInt(), listOf("blue", "pastel", "summer", "cute")),
        Colour("Teal", 0xFF1E9E9A.toInt(), listOf("blue", "green", "ocean", "tropical")),
        Colour("Mint", 0xFFA8EBCF.toInt(), listOf("green", "pastel", "cute", "summer")),
        Colour("Emerald", 0xFF14875A.toInt(), listOf("green", "royal", "elegant", "nature")),
        Colour("Olive", 0xFF6B7334.toInt(), listOf("green", "military", "adventure", "nature")),
        Colour("Lime", 0xFFB6E33B.toInt(), listOf("green", "bright", "colourful", "sporty")),
        Colour("Lemon", 0xFFFFE45C.toInt(), listOf("yellow", "bright", "summer", "cute")),
        Colour("Mustard", 0xFFD6A12B.toInt(), listOf("yellow", "retro", "vintage", "autumn")),
        Colour("Gold", 0xFFE3B341.toInt(), listOf("gold", "glam", "royal", "sparkly")),
        Colour("Silver", 0xFFC7CBD3.toInt(), listOf("silver", "futuristic", "space", "sparkly")),
        Colour("Tangerine", 0xFFFF8C2E.toInt(), listOf("orange", "bright", "summer", "autumn", "tropical")),
        Colour("Coral", 0xFFFF7A6B.toInt(), listOf("orange", "pink", "summer", "tropical", "beach")),
        Colour("Red", 0xFFE0263A.toInt(), listOf("red", "bright", "romantic", "party")),
        Colour("Crimson", 0xFFA3122E.toInt(), listOf("red", "dark", "royal", "gothic", "romantic")),
        Colour("Burgundy", 0xFF6E1630.toInt(), listOf("red", "dark", "autumn", "vintage", "winter")),
        Colour("Bubblegum", 0xFFFF9EC7.toInt(), listOf("pink", "pastel", "cute", "party")),
        Colour("Hot Pink", 0xFFFF2E8C.toInt(), listOf("pink", "bright", "glam", "party", "disco")),
        Colour("Blush", 0xFFF4C6C4.toInt(), listOf("pink", "pastel", "romantic", "wedding")),
        Colour("Lilac", 0xFFC9AEF0.toInt(), listOf("purple", "pastel", "fantasy", "cute")),
        Colour("Purple", 0xFF7A35C9.toInt(), listOf("purple", "royal", "fantasy", "spooky")),
        Colour("Plum", 0xFF4B1F4F.toInt(), listOf("purple", "dark", "gothic", "elegant")),
        Colour("Chocolate", 0xFF5A3420.toInt(), listOf("brown", "autumn", "cosy", "cowboy")),
        Colour("Tan", 0xFFC79A6B.toInt(), listOf("brown", "cowboy", "adventure", "boho", "safari")),
        Colour("Camel", 0xFFB98A4E.toInt(), listOf("brown", "business", "autumn", "vintage")),
        Colour("Neon Green", 0xFF39FF6E.toInt(), listOf("green", "bright", "futuristic", "festival", "colourful")),
        Colour("Electric Blue", 0xFF2EE6FF.toInt(), listOf("blue", "bright", "futuristic", "disco", "space")),
    )
    private fun cs(vararg n: String) = n.map { name -> COLOURS.indexOfFirst { it.name == name } }
    private val METAL = cs("Gold", "Silver")
    private val NATURAL = cs("Chocolate", "Tan", "Camel", "Black", "Ivory", "Olive", "Grey")

    val STYLES: List<Style> = listOf(
        // ------------------------------------------------------------ tops
        Style(Slot.TOP, "tee", "T-Shirt", listOf("casual", "street", "summer"), 26),
        Style(Slot.TOP, "tank", "Tank Top", listOf("casual", "summer", "beach", "sporty"), 22),
        Style(Slot.TOP, "crop", "Crop Top", listOf("party", "summer", "street", "dance"), 24),
        Style(Slot.TOP, "blouse", "Puff Sleeve Blouse", listOf("romantic", "vintage", "elegant", "cute"), 22, F_SOFT),
        Style(Slot.TOP, "shirt", "Button-Up Shirt", listOf("business", "preppy", "formal", "school"), 24, F_SOFT + listOf("pinstripe", "denim", "tartan")),
        Style(Slot.TOP, "hawaii", "Holiday Shirt", listOf("tropical", "beach", "summer", "casual", "colourful"), 18, listOf("floral", "floral", "paisley", "tiedye", "stripes", "dots", "stars")),
        Style(Slot.TOP, "turtle", "Turtleneck", listOf("winter", "sleek", "cosy", "business"), 20, F_WARM),
        Style(Slot.TOP, "hoodie", "Hoodie", listOf("casual", "street", "cosy", "sporty", "school"), 22, F_SPORT + listOf("tiedye", "camo")),
        Style(Slot.TOP, "sweater", "Chunky Jumper", listOf("winter", "cosy", "cute"), 24, F_WARM),
        Style(Slot.TOP, "corset", "Corset Top", listOf("gothic", "romantic", "vintage", "glam", "fantasy"), 18, F_FORMAL + listOf("leather", "lace")),
        Style(Slot.TOP, "offshoulder", "Off-Shoulder Top", listOf("romantic", "summer", "boho", "party"), 20, F_SOFT),
        Style(Slot.TOP, "tube", "Tube Top", listOf("party", "summer", "disco", "glam"), 18, F_SOFT),
        Style(Slot.TOP, "sportstop", "Sports Top", listOf("sporty", "dance", "summer"), 20, F_SPORT),
        Style(Slot.TOP, "polo", "Polo Shirt", listOf("preppy", "sporty", "casual", "school"), 18, F_SPORT),
        Style(Slot.TOP, "halter", "Halter Top", listOf("summer", "party", "disco", "beach"), 18, F_SOFT),
        Style(Slot.TOP, "jersey", "Team Jersey", listOf("sporty", "street", "casual"), 20, F_SPORT),
        Style(Slot.TOP, "tuxshirt", "Bow-Tie Dress Shirt", listOf("formal", "elegant", "business"), 12, F_PLAIN),
        Style(Slot.TOP, "peasant", "Peasant Top", listOf("boho", "festival", "vintage", "fantasy", "picnic", "romantic"), 18, listOf("linen", "floral", "paisley", "lace", "solid", "gingham")),
        Style(Slot.TOP, "bikini", "Bikini Top", listOf("beach", "summer", "tropical", "ocean"), 22, listOf("solid", "stripes", "dots", "floral", "leopard", "tiedye", "hearts", "stars", "gingham")),
        Style(Slot.TOP, "armourtop", "Knight Breastplate", listOf("fantasy", "costume", "history", "military"), 8, F_PLAIN, cs("Silver", "Gold", "Charcoal", "Chocolate")),
        // ------------------------------------------------------------ bottoms
        Style(Slot.BOTTOM, "jeans", "Straight Jeans", listOf("casual", "street", "cowboy", "rock"), 20, listOf("denim", "denim", "denim", "solid", "leather", "corduroy")),
        Style(Slot.BOTTOM, "skinny", "Skinny Jeans", listOf("casual", "rock", "edgy", "street"), 20, listOf("denim", "denim", "leather", "solid", "snakeskin", "leopard")),
        Style(Slot.BOTTOM, "wide", "Wide-Leg Trousers", listOf("elegant", "business", "retro"), 22, F_SOFT + listOf("pinstripe", "tweed", "houndstooth")),
        Style(Slot.BOTTOM, "flares", "Flared Pants", listOf("retro", "disco", "boho", "festival"), 20, listOf("denim", "corduroy", "paisley", "tiedye", "sequin", "velvet", "solid", "chevron")),
        Style(Slot.BOTTOM, "shorts", "Shorts", listOf("summer", "beach", "casual", "sporty"), 22, F_SOFT + listOf("denim", "camo")),
        Style(Slot.BOTTOM, "cargo", "Cargo Pants", listOf("adventure", "street", "military"), 18, F_TOUGH),
        Style(Slot.BOTTOM, "mini", "Mini Skirt", listOf("party", "retro", "street", "glam"), 22, F_SOFT + listOf("denim", "leather", "tartan")),
        Style(Slot.BOTTOM, "pleated", "Pleated Skirt", listOf("preppy", "school", "cute", "retro"), 22, listOf("tartan", "check", "solid", "solid", "gingham", "houndstooth", "satin", "stripes")),
        Style(Slot.BOTTOM, "pencil", "Pencil Skirt", listOf("business", "elegant", "vintage"), 18, listOf("solid", "solid", "pinstripe", "tweed", "leather", "houndstooth", "satin")),
        Style(Slot.BOTTOM, "maxi", "Maxi Skirt", listOf("boho", "summer", "elegant", "festival"), 20, F_SOFT),
        Style(Slot.BOTTOM, "tutu", "Tulle Tutu", listOf("cute", "dance", "fantasy", "party"), 16, listOf("solid", "solid", "glitter", "sheer", "stars", "sequin")),
        Style(Slot.BOTTOM, "leggings", "Leggings", listOf("sporty", "dance", "casual", "school"), 20, F_SPORT + listOf("leopard", "leather", "snakeskin")),
        Style(Slot.BOTTOM, "joggers", "Joggers", listOf("sporty", "casual", "street", "cosy", "sleepwear", "school"), 18, F_SPORT),
        Style(Slot.BOTTOM, "sarong", "Sarong", listOf("beach", "tropical", "summer", "boho"), 16, listOf("floral", "tiedye", "paisley", "linen", "sheer", "solid")),
        Style(Slot.BOTTOM, "culottes", "Culottes", listOf("vintage", "elegant", "summer"), 16, F_SOFT),
        Style(Slot.BOTTOM, "bikebottom", "Bike Shorts", listOf("sporty", "street", "dance"), 16, F_SPORT),
        Style(Slot.BOTTOM, "bikinibottom", "Swim Bottoms", listOf("beach", "summer", "ocean", "tropical"), 18, listOf("solid", "stripes", "dots", "floral", "leopard", "tiedye", "hearts", "gingham")),
        Style(Slot.BOTTOM, "kilt", "Kilt", listOf("history", "punk", "preppy", "winter"), 10, listOf("tartan", "tartan", "check")),
        Style(Slot.BOTTOM, "mermaidtail", "Mermaid Tail", listOf("ocean", "fantasy", "costume", "sparkly"), 12, listOf("sequin", "glitter", "solid", "snakeskin"), cs("Teal", "Emerald", "Electric Blue", "Purple", "Hot Pink", "Gold", "Silver", "Lilac", "Mint", "Coral")),
        // ------------------------------------------------------------ dresses and one-pieces
        Style(Slot.DRESS, "slip", "Slip Dress", listOf("elegant", "romantic", "party", "sleek"), 20, F_FORMAL),
        Style(Slot.DRESS, "aline", "A-Line Dress", listOf("cute", "retro", "romantic", "picnic"), 22, F_SOFT),
        Style(Slot.DRESS, "gown", "Ball Gown", listOf("royal", "formal", "elegant", "fairytale", "glam"), 24, F_FORMAL),
        Style(Slot.DRESS, "mermaid", "Mermaid Gown", listOf("glam", "formal", "elegant", "ocean"), 20, F_FORMAL),
        Style(Slot.DRESS, "minidress", "Mini Dress", listOf("party", "disco", "glam", "retro"), 22, F_SOFT),
        Style(Slot.DRESS, "wrap", "Wrap Dress", listOf("business", "elegant", "summer"), 18, F_SOFT),
        Style(Slot.DRESS, "sundress", "Sundress", listOf("summer", "beach", "picnic", "cute", "nature"), 22, listOf("floral", "floral", "gingham", "linen", "dots", "stripes", "solid", "hearts")),
        Style(Slot.DRESS, "shirtdress", "Shirt Dress", listOf("casual", "business", "preppy"), 16, F_SOFT + listOf("denim")),
        Style(Slot.DRESS, "jumpsuit", "Jumpsuit", listOf("sleek", "disco", "futuristic", "glam"), 20, F_SOFT + listOf("sequin", "denim", "leather")),
        Style(Slot.DRESS, "overalls", "Dungarees", listOf("casual", "cute", "farm", "retro", "picnic", "work"), 16, listOf("denim", "denim", "corduroy", "gingham", "check", "solid")),
        Style(Slot.DRESS, "bridal", "Bridal Gown", listOf("wedding", "romantic", "elegant", "fairytale", "white"), 14, listOf("lace", "satin", "silk", "sheer", "glitter"), cs("White", "Ivory", "Blush", "Silver", "Lilac", "Sky Blue")),
        Style(Slot.DRESS, "flapper", "Fringe Flapper Dress", listOf("vintage", "party", "glam", "dance", "history"), 16, listOf("sequin", "gold", "glitter", "satin", "solid")),
        Style(Slot.DRESS, "kaftan", "Kaftan", listOf("boho", "beach", "festival", "summer"), 16, listOf("paisley", "tiedye", "floral", "linen", "sheer", "solid")),
        Style(Slot.DRESS, "bodycon", "Bodycon Dress", listOf("party", "glam", "sleek", "edgy"), 20, F_SOFT + listOf("leather", "snakeskin")),
        Style(Slot.DRESS, "prom", "Tulle Prom Dress", listOf("party", "formal", "cute", "fairytale", "dance"), 20, listOf("solid", "glitter", "sequin", "sheer", "stars", "satin")),
        Style(Slot.DRESS, "sailor", "Sailor Dress", listOf("ocean", "cute", "school", "retro"), 12, listOf("solid", "stripes")),
        Style(Slot.DRESS, "suit", "Two-Piece Suit", listOf("business", "formal", "sleek", "elegant"), 22, listOf("solid", "solid", "pinstripe", "tweed", "houndstooth", "velvet", "check", "satin")),
        Style(Slot.DRESS, "tux", "Tuxedo", listOf("formal", "elegant", "glam", "royal"), 14, listOf("solid", "velvet", "satin", "sequin")),
        Style(Slot.DRESS, "tracksuit", "Tracksuit", listOf("sporty", "street", "retro", "casual", "school"), 18, F_SPORT),
        Style(Slot.DRESS, "onesie", "Pyjama Onesie", listOf("sleepwear", "cosy", "cute", "costume"), 18, listOf("solid", "stars", "hearts", "dots", "check", "knit", "fur")),
        Style(Slot.DRESS, "spacesuit", "Space Suit", listOf("space", "futuristic", "costume", "adventure"), 10, listOf("solid", "mesh"), cs("White", "Silver", "Tangerine", "Electric Blue", "Black", "Neon Green")),
        Style(Slot.DRESS, "wizard", "Wizard Robe", listOf("fantasy", "costume", "spooky", "fairytale", "magic"), 14, listOf("solid", "stars", "velvet", "glitter", "satin")),
        Style(Slot.DRESS, "superhero", "Super Suit", listOf("superhero", "costume", "sporty", "bright"), 16, listOf("solid", "solid", "stars", "chevron", "mesh")),
        Style(Slot.DRESS, "wetsuit", "Wetsuit", listOf("ocean", "beach", "sporty", "adventure"), 12, listOf("solid", "chevron", "stripes")),
        Style(Slot.DRESS, "swimsuit", "One-Piece Swimsuit", listOf("beach", "summer", "ocean", "retro"), 18, listOf("solid", "stripes", "dots", "floral", "leopard", "tiedye", "gingham", "glitter")),
        Style(Slot.DRESS, "skeleton", "Skeleton Suit", listOf("spooky", "costume", "gothic", "dark"), 6, F_PLAIN, cs("Black", "Charcoal", "Plum", "Navy")),
        Style(Slot.DRESS, "princess", "Princess Gown", listOf("fairytale", "royal", "fantasy", "cute"), 16, listOf("satin", "glitter", "sheer", "sequin", "silk", "velvet")),
        Style(Slot.DRESS, "safari", "Safari Suit", listOf("adventure", "safari", "travel", "nature"), 10, listOf("solid", "linen", "camo"), NATURAL),
        // ------------------------------------------------------------ outerwear
        Style(Slot.OUTER, "denimjkt", "Denim Jacket", listOf("casual", "street", "rock", "cowboy"), 14, listOf("denim", "denim", "denim", "corduroy")),
        Style(Slot.OUTER, "biker", "Biker Jacket", listOf("edgy", "punk", "rock", "street"), 18, listOf("leather", "leather", "leather", "snakeskin", "quilted", "sequin")),
        Style(Slot.OUTER, "blazer", "Blazer", listOf("business", "preppy", "formal", "school"), 24, F_TOUGH + listOf("velvet", "sequin", "satin")),
        Style(Slot.OUTER, "trench", "Trench Coat", listOf("rain", "business", "vintage", "elegant"), 16, listOf("solid", "solid", "leather", "check", "houndstooth", "tweed")),
        Style(Slot.OUTER, "puffer", "Puffer Jacket", listOf("winter", "street", "cosy", "adventure"), 20, listOf("quilted", "quilted", "solid", "glitter", "camo", "satin")),
        Style(Slot.OUTER, "cardigan", "Cardigan", listOf("cosy", "preppy", "vintage", "cute", "school"), 20, F_WARM),
        Style(Slot.OUTER, "furcoat", "Faux Fur Coat", listOf("glam", "winter", "royal", "cosy"), 16, listOf("fur", "fur", "leopard", "zebra", "velvet")),
        Style(Slot.OUTER, "bomber", "Bomber Jacket", listOf("street", "military", "sporty", "retro"), 18, listOf("satin", "solid", "camo", "leather", "quilted", "sequin")),
        Style(Slot.OUTER, "varsity", "Varsity Jacket", listOf("sporty", "school", "retro", "preppy"), 14, listOf("solid", "solid", "leather", "knit")),
        Style(Slot.OUTER, "raincoat", "Raincoat", listOf("rain", "cute", "adventure"), 14, listOf("solid", "solid", "dots", "stripes", "stars")),
        Style(Slot.OUTER, "poncho", "Poncho", listOf("boho", "festival", "cowboy", "rain"), 14, listOf("knit", "paisley", "tiedye", "stripes", "chevron", "solid")),
        Style(Slot.OUTER, "waistcoat", "Waistcoat", listOf("formal", "vintage", "cowboy", "business"), 16, listOf("solid", "tweed", "pinstripe", "velvet", "satin", "leather", "paisley")),
        Style(Slot.OUTER, "labcoat", "Lab Coat", listOf("science", "costume", "work"), 6, F_PLAIN, cs("White", "Ivory", "Sky Blue", "Mint")),
        Style(Slot.OUTER, "kimono", "Duster Robe", listOf("boho", "elegant", "festival", "summer"), 16, listOf("floral", "silk", "satin", "sheer", "paisley", "velvet")),
        Style(Slot.OUTER, "military", "Military Coat", listOf("military", "royal", "history", "rock"), 12, listOf("solid", "velvet", "tweed")),
        Style(Slot.OUTER, "cropjkt", "Cropped Sequin Jacket", listOf("disco", "party", "sparkly", "glam"), 14, listOf("sequin", "glitter", "gold", "sequin")),
        // ------------------------------------------------------------ shoes
        Style(Slot.SHOES, "sneakers", "Sneakers", listOf("casual", "sporty", "street", "school"), 22, F_SHOE + listOf("mesh", "stars")),
        Style(Slot.SHOES, "heels", "Stiletto Heels", listOf("glam", "formal", "party", "elegant"), 24, F_SHOE),
        Style(Slot.SHOES, "ankleboots", "Ankle Boots", listOf("edgy", "rock", "autumn", "street"), 18, F_SHOE),
        Style(Slot.SHOES, "kneeboots", "Knee-High Boots", listOf("glam", "retro", "winter", "edgy"), 18, F_SHOE),
        Style(Slot.SHOES, "cowboyboots", "Cowboy Boots", listOf("cowboy", "western", "festival"), 14, listOf("leather", "leather", "snakeskin", "solid", "glitter"), NATURAL + cs("Red", "Hot Pink", "White", "Silver")),
        Style(Slot.SHOES, "sandals", "Strappy Sandals", listOf("summer", "beach", "boho", "tropical"), 18, F_SHOE),
        Style(Slot.SHOES, "flats", "Ballet Flats", listOf("cute", "dance", "preppy", "school"), 18, F_SHOE),
        Style(Slot.SHOES, "platforms", "Platform Shoes", listOf("disco", "retro", "party", "punk"), 18, F_SHOE),
        Style(Slot.SHOES, "loafers", "Loafers", listOf("preppy", "business", "school", "vintage"), 14, listOf("leather", "solid", "velvet", "snakeskin")),
        Style(Slot.SHOES, "gumboots", "Gumboots", listOf("rain", "farm", "adventure", "festival"), 14, listOf("solid", "solid", "dots", "camo", "stars")),
        Style(Slot.SHOES, "combat", "Combat Boots", listOf("punk", "military", "edgy", "gothic", "spooky"), 14, listOf("leather", "solid", "camo", "glitter")),
        Style(Slot.SHOES, "slippers", "Fluffy Slippers", listOf("sleepwear", "cosy", "cute"), 12, listOf("fur", "knit", "solid")),
        Style(Slot.SHOES, "rollers", "Roller Skates", listOf("retro", "disco", "sporty", "cute"), 12, listOf("solid", "glitter", "stars")),
        Style(Slot.SHOES, "flippers", "Flippers", listOf("ocean", "beach", "costume"), 8, F_PLAIN),
        Style(Slot.SHOES, "glass", "Glass Slippers", listOf("fairytale", "royal", "sparkly", "wedding"), 6, listOf("glitter"), cs("Silver", "Electric Blue", "Lilac", "White")),
        // ------------------------------------------------------------ headwear
        Style(Slot.HAT, "cap", "Baseball Cap", listOf("sporty", "street", "casual"), 16, F_SPORT + listOf("denim")),
        Style(Slot.HAT, "beanie", "Beanie", listOf("winter", "cosy", "street"), 14, listOf("knit", "knit", "solid", "stripes", "fur")),
        Style(Slot.HAT, "sunhat", "Wide Sun Hat", listOf("beach", "summer", "elegant", "boho"), 14, listOf("solid", "linen", "linen", "floral", "stripes")),
        Style(Slot.HAT, "fedora", "Fedora", listOf("vintage", "business", "rock", "travel"), 12, listOf("solid", "tweed", "leather", "houndstooth", "velvet")),
        Style(Slot.HAT, "tophat", "Top Hat", listOf("formal", "vintage", "magic", "royal"), 10, listOf("solid", "velvet", "sequin", "glitter")),
        Style(Slot.HAT, "beret", "Beret", listOf("art", "vintage", "travel", "military"), 12, listOf("solid", "knit", "velvet", "tartan")),
        Style(Slot.HAT, "cowboyhat", "Cowboy Hat", listOf("cowboy", "western", "festival"), 12, listOf("solid", "leather", "glitter", "sequin"), NATURAL + cs("Hot Pink", "White", "Silver", "Black")),
        Style(Slot.HAT, "crown", "Crown", listOf("royal", "fairytale", "glam", "sparkly"), 6, F_PLAIN, METAL + cs("Crimson", "Purple", "Emerald", "Black")),
        Style(Slot.HAT, "tiara", "Tiara", listOf("royal", "wedding", "fairytale", "sparkly"), 6, F_PLAIN, METAL + cs("Bubblegum", "Electric Blue", "Lilac", "White")),
        Style(Slot.HAT, "witch", "Witch Hat", listOf("spooky", "fantasy", "costume", "gothic"), 8, listOf("solid", "velvet", "stars", "glitter")),
        Style(Slot.HAT, "bucket", "Bucket Hat", listOf("street", "festival", "retro", "summer"), 12, F_SOFT + listOf("denim", "camo")),
        Style(Slot.HAT, "headband", "Headband", listOf("retro", "sporty", "cute", "preppy"), 12, F_SOFT),
        Style(Slot.HAT, "flowercrown", "Flower Crown", listOf("boho", "festival", "nature", "romantic", "wedding"), 10, F_PLAIN),
        Style(Slot.HAT, "bunny", "Bunny Ears", listOf("cute", "costume", "party", "spring"), 8, listOf("solid", "fur", "glitter")),
        Style(Slot.HAT, "cat", "Cat Ears", listOf("cute", "costume", "spooky", "wild"), 8, listOf("solid", "fur", "leopard", "glitter")),
        Style(Slot.HAT, "partyhat", "Party Hat", listOf("party", "cute", "colourful", "costume"), 10, listOf("stripes", "dots", "stars", "glitter", "chevron")),
        Style(Slot.HAT, "halo", "Halo", listOf("fantasy", "costume", "romantic", "white"), 4, F_PLAIN, METAL + cs("White", "Electric Blue")),
        Style(Slot.HAT, "horns", "Devil Horns", listOf("spooky", "costume", "edgy", "gothic"), 6, listOf("solid", "glitter")),
        Style(Slot.HAT, "chef", "Chef Hat", listOf("food", "work", "costume"), 4, F_PLAIN, cs("White", "Ivory", "Black", "Bubblegum")),
        Style(Slot.HAT, "helmet", "Space Helmet", listOf("space", "futuristic", "costume"), 6, F_PLAIN, cs("White", "Silver", "Tangerine", "Electric Blue", "Black", "Gold")),
        Style(Slot.HAT, "bandana", "Bandana", listOf("cowboy", "rock", "festival", "pirate"), 12, listOf("paisley", "paisley", "solid", "dots", "tiedye", "camo")),
        Style(Slot.HAT, "pirate", "Pirate Hat", listOf("pirate", "costume", "adventure", "ocean", "history"), 6, listOf("solid", "leather", "velvet"), cs("Black", "Chocolate", "Burgundy", "Navy", "Crimson", "Plum")),
        Style(Slot.HAT, "antlers", "Reindeer Antlers", listOf("christmas", "costume", "winter", "cute"), 4, F_PLAIN, cs("Chocolate", "Gold", "Tan", "Red")),
        Style(Slot.HAT, "santa", "Festive Hat", listOf("christmas", "winter", "costume", "cosy"), 6, listOf("solid", "velvet", "glitter"), cs("Red", "Crimson", "Emerald", "Gold", "Silver", "Hot Pink")),
        // ------------------------------------------------------------ eyewear
        Style(Slot.GLASSES, "roundsun", "Round Sunglasses", listOf("retro", "boho", "summer", "festival"), 10, F_PLAIN),
        Style(Slot.GLASSES, "aviator", "Aviators", listOf("cool", "military", "travel", "rock"), 8, F_PLAIN),
        Style(Slot.GLASSES, "cateye", "Cat-Eye Glasses", listOf("vintage", "glam", "retro"), 10, F_PLAIN),
        Style(Slot.GLASSES, "heartglasses", "Heart Glasses", listOf("cute", "party", "romantic", "festival"), 8, F_PLAIN),
        Style(Slot.GLASSES, "specs", "Smart Specs", listOf("school", "business", "preppy", "science"), 10, F_PLAIN),
        Style(Slot.GLASSES, "visor", "Cyber Visor", listOf("futuristic", "space", "disco"), 8, F_PLAIN),
        Style(Slot.GLASSES, "starglasses", "Star Glasses", listOf("party", "disco", "glam", "space"), 6, F_PLAIN),
        Style(Slot.GLASSES, "goggles", "Swim Goggles", listOf("ocean", "sporty", "beach"), 6, F_PLAIN),
        Style(Slot.GLASSES, "eyepatch", "Eye Patch", listOf("pirate", "costume", "edgy"), 4, F_PLAIN, cs("Black", "Chocolate", "Crimson", "Gold")),
        Style(Slot.GLASSES, "monocle", "Monocle", listOf("vintage", "formal", "royal"), 4, F_PLAIN, METAL),
        // ------------------------------------------------------------ neck
        Style(Slot.NECK, "pearls", "Pearl Necklace", listOf("elegant", "vintage", "formal", "wedding"), 6, F_PLAIN, cs("White", "Ivory", "Blush", "Black", "Silver", "Gold")),
        Style(Slot.NECK, "chain", "Chunky Chain", listOf("street", "rock", "glam"), 6, F_PLAIN, METAL + cs("Black", "Hot Pink")),
        Style(Slot.NECK, "choker", "Choker", listOf("gothic", "edgy", "retro", "punk"), 10, listOf("solid", "velvet", "lace", "leather")),
        Style(Slot.NECK, "scarf", "Woolly Scarf", listOf("winter", "cosy", "school"), 16, F_WARM),
        Style(Slot.NECK, "bowtie", "Bow Tie", listOf("formal", "cute", "preppy", "magic"), 12, listOf("solid", "dots", "velvet", "satin", "check", "glitter")),
        Style(Slot.NECK, "tie", "Necktie", listOf("business", "school", "formal", "preppy"), 14, listOf("solid", "stripes", "dots", "check", "silk", "paisley", "tartan")),
        Style(Slot.NECK, "pendant", "Gem Pendant", listOf("elegant", "fantasy", "romantic", "glam"), 10, F_PLAIN),
        Style(Slot.NECK, "lei", "Flower Lei", listOf("tropical", "beach", "summer", "festival"), 8, F_PLAIN),
        Style(Slot.NECK, "boa", "Feather Boa", listOf("glam", "party", "disco", "vintage"), 10, listOf("fur", "solid")),
        Style(Slot.NECK, "medal", "Gold Medal", listOf("sporty", "winner", "costume"), 4, F_PLAIN, cs("Gold", "Silver", "Tangerine", "Red")),
        Style(Slot.NECK, "silkscarf", "Silk Neck Scarf", listOf("travel", "elegant", "vintage", "cowboy"), 12, listOf("silk", "paisley", "dots", "floral", "satin")),
        // ------------------------------------------------------------ ears
        Style(Slot.EARS, "studs", "Stud Earrings", listOf("elegant", "sleek", "business"), 8, F_PLAIN),
        Style(Slot.EARS, "hoops", "Hoop Earrings", listOf("street", "glam", "retro", "boho"), 8, F_PLAIN),
        Style(Slot.EARS, "drops", "Drop Earrings", listOf("formal", "elegant", "romantic", "glam"), 8, F_PLAIN),
        Style(Slot.EARS, "tassels", "Tassel Earrings", listOf("boho", "festival", "party", "colourful"), 8, F_PLAIN),
        Style(Slot.EARS, "starsear", "Star Earrings", listOf("space", "cute", "party", "sparkly"), 6, F_PLAIN),
        // ------------------------------------------------------------ bags
        Style(Slot.BAG, "handbag", "Handbag", listOf("elegant", "business", "glam"), 18, F_SHOE),
        Style(Slot.BAG, "clutch", "Clutch", listOf("formal", "party", "glam", "sparkly"), 16, F_FORMAL),
        Style(Slot.BAG, "tote", "Tote Bag", listOf("casual", "travel", "beach", "summer"), 14, listOf("linen", "stripes", "solid", "floral", "denim", "check")),
        Style(Slot.BAG, "basket", "Picnic Basket", listOf("picnic", "farm", "summer", "fairytale"), 6, F_PLAIN, NATURAL),
        Style(Slot.BAG, "briefcase", "Briefcase", listOf("business", "work", "travel"), 8, listOf("leather", "solid")),
        Style(Slot.BAG, "wand", "Magic Wand", listOf("fantasy", "fairytale", "magic", "costume", "sparkly"), 6, F_PLAIN, METAL + cs("Lilac", "Bubblegum", "Electric Blue", "White")),
        Style(Slot.BAG, "umbrella", "Umbrella", listOf("rain", "vintage", "elegant"), 10, listOf("solid", "dots", "stripes", "check", "floral", "hearts")),
        Style(Slot.BAG, "guitar", "Electric Guitar", listOf("rock", "music", "punk", "costume"), 8, listOf("solid", "glitter", "stars", "chevron")),
        Style(Slot.BAG, "racket", "Tennis Racket", listOf("sporty", "preppy", "summer"), 6, F_PLAIN),
        Style(Slot.BAG, "bouquet", "Flower Bouquet", listOf("wedding", "romantic", "nature", "spring"), 8, F_PLAIN),
        // ------------------------------------------------------------ back
        Style(Slot.BACK, "angel", "Angel Wings", listOf("fantasy", "romantic", "costume", "white"), 6, F_PLAIN, cs("White", "Ivory", "Gold", "Silver", "Blush", "Black")),
        Style(Slot.BACK, "fairy", "Fairy Wings", listOf("fantasy", "fairytale", "cute", "nature", "sparkly"), 8, listOf("sheer", "glitter")),
        Style(Slot.BACK, "bat", "Bat Wings", listOf("spooky", "gothic", "costume", "dark"), 6, F_PLAIN, cs("Black", "Plum", "Crimson", "Charcoal", "Purple", "Burgundy")),
        Style(Slot.BACK, "butterfly", "Butterfly Wings", listOf("nature", "fantasy", "colourful", "cute"), 8, listOf("solid", "dots", "tiedye")),
        Style(Slot.BACK, "cape", "Royal Cape", listOf("royal", "fairytale", "history", "elegant", "magic"), 10, listOf("velvet", "velvet", "satin", "fur", "glitter")),
        Style(Slot.BACK, "herocape", "Hero Cape", listOf("superhero", "costume", "bright"), 8, listOf("solid", "satin", "stars")),
        Style(Slot.BACK, "backpack", "Backpack", listOf("school", "adventure", "travel", "casual"), 14, F_SPORT + listOf("denim", "leather", "check")),
        Style(Slot.BACK, "jetpack", "Jetpack", listOf("space", "futuristic", "costume", "adventure"), 4, F_PLAIN, cs("Silver", "Tangerine", "Red", "Electric Blue")),
        Style(Slot.BACK, "dragonwings", "Dragon Wings", listOf("fantasy", "costume", "edgy", "wild"), 6, listOf("solid", "snakeskin"), cs("Emerald", "Crimson", "Purple", "Black", "Gold", "Teal")),
        Style(Slot.BACK, "surfboard", "Surfboard", listOf("beach", "ocean", "sporty", "summer", "tropical"), 8, listOf("solid", "stripes", "floral", "chevron", "tiedye")),

        // ------------------------------------------------------------ v1.1 additions (appended so earlier item ids stay the same)
        // gym and sport
        Style(Slot.TOP, "petee", "PE Uniform Tee", listOf("sporty", "school", "casual", "bright"), 16, listOf("solid", "solid", "stripes", "mesh")),
        Style(Slot.BOTTOM, "gymshorts", "Gym Shorts", listOf("sporty", "school", "casual", "summer"), 18, listOf("solid", "solid", "mesh", "stripes", "chevron")),
        Style(Slot.BOTTOM, "skort", "Tennis Skort", listOf("sporty", "preppy", "school", "summer"), 12, listOf("solid", "solid", "pinstripe", "check")),
        Style(Slot.SHOES, "hightops", "High-Top Trainers", listOf("sporty", "school", "street", "casual", "retro"), 16, F_SHOE + listOf("mesh", "stars")),
        Style(Slot.HAT, "sweatband", "Sweatband", listOf("sporty", "retro", "dance", "school"), 10, listOf("solid", "stripes", "knit")),
        Style(Slot.NECK, "whistle", "Coach's Whistle", listOf("sporty", "school", "work"), 6, F_PLAIN, cs("Silver", "Gold", "Red", "Royal Blue", "Lime", "Black")),
        Style(Slot.BAG, "gymbag", "Gym Bag", listOf("sporty", "school", "travel", "casual"), 12, F_SPORT),
        Style(Slot.BAG, "basketball", "Basketball", listOf("sporty", "school", "street"), 4, F_PLAIN, cs("Tangerine", "Mustard", "Red", "Royal Blue")),
        // kitchen
        Style(Slot.TOP, "chefjacket", "Chef's Jacket", listOf("food", "work", "costume", "white", "formal"), 12, listOf("solid", "solid", "pinstripe", "check"), cs("White", "Ivory", "Black", "Charcoal", "Bubblegum")),
        Style(Slot.BOTTOM, "chefpants", "Chef's Trousers", listOf("food", "work", "casual"), 10, listOf("check", "houndstooth", "pinstripe", "solid", "gingham")),
        Style(Slot.OUTER, "apron", "Apron", listOf("food", "work", "picnic", "cute", "farm"), 14, listOf("solid", "gingham", "stripes", "floral", "dots", "denim", "linen")),
        Style(Slot.SHOES, "clogs", "Kitchen Clogs", listOf("food", "work", "comfy", "casual"), 10, listOf("solid", "solid", "dots", "leather")),
        Style(Slot.BAG, "spoon", "Wooden Spoon", listOf("food", "work", "costume", "picnic"), 4, F_PLAIN, cs("Tan", "Camel", "Chocolate", "Mustard")),
        // heroes and emergency services
        Style(Slot.SHOES, "heroboots", "Hero Boots", listOf("superhero", "costume", "bright", "sporty"), 12, listOf("solid", "solid", "leather", "glitter")),
        Style(Slot.GLASSES, "heromask", "Hero Mask", listOf("superhero", "costume", "magic", "edgy"), 10, listOf("solid", "satin", "glitter")),
        Style(Slot.OUTER, "firecoat", "Firefighter Coat", listOf("superhero", "work", "costume", "rain", "bright"), 6, F_PLAIN, cs("Tangerine", "Mustard", "Red", "Charcoal", "Lemon", "Black")),
        Style(Slot.HAT, "firehelmet", "Fire Helmet", listOf("superhero", "work", "costume", "red"), 4, F_PLAIN, cs("Red", "Lemon", "White", "Black")),
        Style(Slot.BOTTOM, "utility", "Utility Trousers", listOf("work", "superhero", "adventure", "military"), 12, listOf("solid", "solid", "camo", "denim", "quilted")),
        Style(Slot.SHOES, "workboots", "Work Boots", listOf("work", "adventure", "farm", "superhero", "rain"), 12, listOf("leather", "leather", "solid", "camo")),
        // pirates and the high seas
        Style(Slot.TOP, "pirateshirt", "Billowy Pirate Shirt", listOf("pirate", "adventure", "history", "romantic"), 12, listOf("linen", "solid", "silk", "stripes")),
        Style(Slot.BOTTOM, "breeches", "Breeches", listOf("pirate", "history", "adventure", "royal"), 12, listOf("solid", "stripes", "velvet", "leather", "linen")),
        Style(Slot.SHOES, "pirateboots", "Cuffed Pirate Boots", listOf("pirate", "adventure", "history", "costume"), 10, listOf("leather", "leather", "solid", "velvet"), NATURAL + cs("Burgundy", "Black")),
        Style(Slot.BAG, "cutlass", "Toy Cutlass", listOf("pirate", "costume", "adventure", "history", "military"), 4, F_PLAIN, cs("Silver", "Gold", "Charcoal")),
        // knights and the military
        Style(Slot.BOTTOM, "chainmail", "Chainmail Leggings", listOf("history", "fantasy", "military", "costume", "silver"), 6, listOf("mesh"), cs("Silver", "Charcoal", "Grey", "Gold")),
        Style(Slot.SHOES, "sabatons", "Armoured Boots", listOf("history", "fantasy", "military", "costume", "silver"), 6, F_PLAIN, cs("Silver", "Gold", "Charcoal", "Grey")),
        Style(Slot.BAG, "shield", "Knight's Shield", listOf("history", "fantasy", "military", "costume", "royal"), 8, F_PLAIN),
        Style(Slot.BOTTOM, "uniform", "Uniform Trousers", listOf("military", "formal", "history", "business"), 12, listOf("solid", "solid", "pinstripe", "tweed")),
        Style(Slot.SHOES, "ridingboots", "Polished Riding Boots", listOf("military", "history", "formal", "royal", "elegant"), 12, listOf("leather", "leather", "solid", "velvet"), cs("Black", "Chocolate", "Burgundy", "Navy", "Tan")),
        // spooky
        Style(Slot.TOP, "tattered", "Tattered Shirt", listOf("spooky", "costume", "dark", "gothic", "edgy"), 10, listOf("solid", "linen", "check", "lace")),
        Style(Slot.BOTTOM, "ripped", "Ripped Trousers", listOf("spooky", "costume", "punk", "edgy", "rock"), 12, listOf("solid", "denim", "tartan", "leather")),
        Style(Slot.DRESS, "ghost", "Ghost Sheet", listOf("spooky", "costume", "white", "cute"), 6, F_PLAIN, cs("White", "Ivory", "Lilac", "Mint", "Sky Blue", "Grey")),
        Style(Slot.SHOES, "spookyboots", "Spiked Goth Boots", listOf("spooky", "gothic", "costume", "dark", "punk"), 10, listOf("leather", "leather", "velvet", "glitter"), cs("Black", "Plum", "Crimson", "Charcoal", "Purple")),
        // magic
        Style(Slot.OUTER, "tailcoat", "Tailcoat", listOf("magic", "formal", "vintage", "elegant", "history"), 12, listOf("solid", "velvet", "satin", "sequin", "pinstripe")),
        Style(Slot.SHOES, "spats", "Spats and Brogues", listOf("magic", "formal", "vintage", "dance", "business"), 10, listOf("leather", "solid", "velvet")),
        // western
        Style(Slot.TOP, "westernshirt", "Western Shirt", listOf("cowboy", "western", "festival", "casual"), 14, listOf("check", "check", "denim", "solid", "gingham", "paisley")),
        Style(Slot.BOTTOM, "fringeskirt", "Fringed Suede Skirt", listOf("cowboy", "western", "boho", "festival"), 10, listOf("solid", "leather", "denim"), NATURAL + cs("Hot Pink", "White", "Mustard", "Crimson")),
        Style(Slot.DRESS, "prairie", "Prairie Dress", listOf("cowboy", "western", "picnic", "romantic", "vintage", "farm"), 14, listOf("floral", "gingham", "solid", "linen", "lace")),
        // nature and fairytale footwear
        Style(Slot.SHOES, "fairyslippers", "Petal Slippers", listOf("fantasy", "fairytale", "nature", "romantic", "cute"), 10, listOf("solid", "glitter", "satin", "floral")),
        Style(Slot.SHOES, "espadrilles", "Espadrilles", listOf("summer", "beach", "boho", "picnic", "nature", "adventure", "casual"), 14, listOf("solid", "stripes", "linen", "floral", "denim")),
        Style(Slot.SHOES, "maryjanes", "Mary Janes", listOf("cute", "picnic", "vintage", "romantic", "school", "fairytale"), 14, listOf("solid", "solid", "leather", "velvet", "glitter", "satin")),
    )

    val items: List<Item>
    private val byIdMap: Map<Int, Item>
    val bySlot: Map<Slot, List<Item>>
    val byStyle: Map<String, List<Item>>

    init {
        val list = ArrayList<Item>()
        val rnd = Random(20261029)
        var id = 1
        for (st in STYLES) {
            val used = HashSet<String>()
            val palette = st.colours ?: COLOURS.indices.toList()
            var attempts = 0
            var made = 0
            while (made < st.count && attempts < st.count * 30) {
                attempts++
                val ci = palette[rnd.nextInt(palette.size)]
                val fab = st.fabrics[rnd.nextInt(st.fabrics.size)]
                val key = "$ci/$fab"
                if (key in used) continue
                used.add(key)
                val col = COLOURS[ci]
                val acc = COLOURS[accentFor(ci, rnd)]
                val fName = FABRIC_NAMES[fab] ?: ""
                val natural = fab in setOf("leopard", "camo", "snakeskin", "zebra", "tiedye", "floral", "paisley", "tartan", "gold")
                val name = when {
                    natural -> "$fName ${st.name}"
                    fName.isEmpty() -> "${col.name} ${st.name}"
                    else -> "${col.name} $fName ${st.name}"
                }
                val tags = HashSet<String>()
                tags.addAll(st.tags); tags.addAll(col.tags); FABRIC_TAGS[fab]?.let { tags.addAll(it) }; tags.add(fab); tags.add(col.name.lowercase())
                if (natural) tags.removeAll(col.tags.toSet() - st.tags.toSet())
                list.add(Item(id++, st, col, acc, fab, name, tags))
                made++
            }
        }
        items = list
        byIdMap = list.associateBy { it.id }
        bySlot = list.groupBy { it.slot }
        byStyle = list.groupBy { it.style.key }
    }

    private fun accentFor(ci: Int, r: Random): Int {
        val opts = listOf("White", "Black", "Gold", "Silver", "Ivory", "Navy", "Red", "Hot Pink", "Lemon")
        val n = opts[r.nextInt(opts.size)]
        val idx = COLOURS.indexOfFirst { it.name == n }
        return if (idx == ci) COLOURS.indexOfFirst { it.name == "White" }.let { if (it == ci) 0 else it } else idx
    }

    fun byId(id: Int): Item? = byIdMap[id]
    fun style(key: String): Style? = STYLES.firstOrNull { it.key == key }
    val size get() = items.size
}
