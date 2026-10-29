package com.nugggy.runwayrush.model

class Theme(val id: Int, val name: String, val blurb: String, val image: String, val tags: List<String>)

object Themes {
    // name | blurb | photo | tags (first tags matter most)
    private val RAW = """
Beach Day|Sun, sand and a sea breeze. Dress for the shoreline.|beach|beach summer tropical casual bright
Winter Wonderland|Snowflakes are falling. Stay warm and look magical.|winter|winter cosy white silver sparkly
Disco Fever|The mirror ball is spinning. Shine bright on the dance floor.|party|disco sparkly party glam retro
Red Carpet Premiere|Cameras flash as the stars arrive. Pure glamour.|formal|glam formal elegant sparkly black
Royal Ball|An invitation from the palace. Dress like royalty.|history|royal elegant formal fairytale gold
Space Explorer|Countdown complete. Ready for lift-off to the stars.|space|space futuristic silver adventure white
Fairy Forest|Hidden among the mushrooms and moonlit flowers.|fantasy|fantasy nature fairytale green sparkly
Rock Star|Sold-out stadium tonight. Bring the attitude.|music|rock edgy punk black leather
Pyjama Party|Movies, popcorn and the comfiest clothes you own.|party|sleepwear cosy cute pastel
Tropical Island|Palm trees, coconuts and turquoise water.|beach|tropical beach summer colourful bright
Gothic Romance|Candlelight, velvet and a little bit of mystery.|horror|gothic dark romantic black lace
Spooky Night|Something is lurking in the shadows. Embrace the eerie.|horror|spooky costume dark gothic purple
Cowboy Country|Saddle up for a rodeo in the dusty west.|western|cowboy western brown festival denim
Business Boss|Big meeting, bigger ambitions. Power dressing only.|work|business formal sleek preppy grey
Back to School|First day of term. Smart, neat and ready to learn.|work|school preppy casual cute
Garden Party|Afternoon tea among the roses.|nature|romantic picnic pastel nature elegant
Music Festival|Three days of bands, glitter and muddy fields.|festival|festival boho colourful retro summer
Under the Sea|Dive deep with mermaids and coral reefs.|ocean|ocean fantasy blue sparkly tropical
Wedding Guest|Celebrate the happy couple without upstaging them.|wedding|elegant romantic formal pastel
Dream Wedding|It is your big day. Something old, something new.|wedding|wedding white romantic elegant fairytale
Sporty Spice|Game day. Fast, fit and full of energy.|sport|sporty street bright casual
Eighties Throwback|Big hair, neon and leg warmers.|retro|retro bright colourful disco dance
Seventies Groove|Flares, florals and peace signs.|retro|retro boho festival orange brown
Fifties Diner|Milkshakes, jukeboxes and polka dots.|retro|retro cute vintage red dots
Roaring Twenties|Jazz, pearls and champagne at a speakeasy.|history|vintage glam party gold black
Superhero Squad|The city needs saving. Suit up.|city|superhero costume bright sporty
Pirate Adventure|Hoist the sails and hunt for buried treasure.|ocean|pirate adventure costume history brown
Fairytale Princess|Once upon a time, in a faraway castle.|fantasy|fairytale royal cute pastel sparkly
Witches and Wizards|Spells, potions and pointy hats.|fantasy|fantasy magic spooky purple costume
Safari Expedition|Binoculars ready. The savannah awaits.|nature|safari adventure travel brown nature
Arctic Expedition|Minus thirty degrees. Fashion meets survival.|winter|winter adventure cosy white
Rainy Day|Puddles everywhere. Make the grey skies brighter.|city|rain cute bright casual
Valentine's Date|Hearts, roses and a candlelit dinner.|food|romantic red pink cute elegant
Christmas Party|Tinsel, twinkle lights and festive cheer.|winter|christmas party red green sparkly
New Year's Eve|Countdown to midnight in your sparkliest look.|party|party sparkly glam gold silver
Spring Blossom|Flowers bloom and the days get longer.|nature|spring nature pastel romantic pink
Autumn Leaves|Crunchy leaves, warm drinks and cosy layers.|nature|autumn cosy brown orange vintage
Summer Holiday|Postcards, ice cream and endless sunshine.|travel|summer travel casual bright beach
Paris Fashion Week|The most stylish city on earth. Be chic.|city|elegant sleek vintage black art
Street Style|Sneakers, attitude and city cool.|city|street casual sporty edgy
Punk Rebellion|Safety pins, tartan and loud music.|music|punk edgy rock tartan black
Boho Chic|Free spirit, flowing fabrics and fringe.|festival|boho festival nature brown summer
Futuristic City|The year is 3025. Fashion has evolved.|space|futuristic silver sleek space bright
Candy Land|Lollipops, cupcakes and sugar-sweet colours.|food|cute pastel pink colourful party
All Black Everything|One colour only. Make it dramatic.|horror|black dark sleek edgy formal
White Party|Head to toe in white. Crisp and clean.|party|white sleek elegant summer
Pretty in Pink|Every shade of pink you can find.|party|pink cute romantic glam
Feeling Blue|Ocean, sky and sapphire tones.|ocean|blue ocean sleek
Go Green|Leafy, natural and fresh.|nature|green nature summer
Golden Hour|Warm sunlight and shimmering gold.|beach|gold glam sparkly summer elegant
Silver Screen|Old Hollywood glamour in black and white.|formal|vintage glam elegant black white formal
Rainbow Bright|Every colour at once. More is more.|festival|colourful bright party cute festival
Monochrome Magic|Black and white only. Bold contrast.|city|black white sleek business
Animal Instinct|Prints from the wild side.|nature|wild edgy glam safari
Farm Life|Early mornings, hay bales and friendly animals.|nature|farm casual cowboy picnic
Picnic in the Park|Gingham blanket, sandwiches and sunshine.|nature|picnic cute summer nature casual
Ski Resort|Fresh powder and après-ski hot chocolate.|winter|winter sporty cosy adventure
Surf's Up|Catch the perfect wave.|beach|beach ocean sporty summer
Yacht Party|Sailing the Mediterranean in style.|ocean|ocean elegant summer white preppy
Ballet Recital|Pirouettes, pointe shoes and grace.|music|dance cute pastel elegant
Dance Battle|Busting moves on the street.|music|dance street sporty party
Jazz Club|Smoky lounges and smooth saxophones.|music|vintage elegant formal dark
K-Pop Concert|Stage lights and synchronised choreography.|music|party street bright dance sparkly
Opera Night|Velvet seats and a grand performance.|formal|formal elegant royal velvet
Museum Gala|Fundraising among the masterpieces.|history|formal art elegant glam
Art Gallery Opening|Creative, bold and a little bit quirky.|city|art colourful edgy sleek
Science Fair|Lab coats and big ideas.|work|science school preppy costume
Chef's Kitchen|Cooking up something delicious.|food|food work costume white
Camping Trip|Tents, campfires and starry skies.|nature|adventure nature casual cosy
Mountain Hike|Peak views and sturdy boots.|nature|adventure sporty nature brown
Road Trip|Windows down, music up, open road ahead.|travel|travel casual retro denim
Airport Chic|Comfortable yet stylish for a long-haul flight.|travel|travel casual sleek cosy
Holiday in Tokyo|Neon streets and cutting-edge style.|city|street futuristic cute bright
Holiday in Rome|Ancient ruins and gelato.|history|travel summer elegant vintage
Desert Mirage|Heat haze, sand dunes and golden light.|western|safari boho brown gold
Midsummer Night|Flower crowns under the long twilight.|nature|fantasy romantic nature boho
Masquerade Ball|Masks, mystery and midnight waltzes.|formal|formal royal gothic elegant glam
Vampire Castle|Dark velvet and a dramatic cape.|horror|gothic spooky royal crimson dark
Zombie Walk|Undead and fabulous.|horror|spooky costume dark edgy
Haunted House|Creaky floors and ghostly whispers.|horror|spooky costume gothic purple
Alien Invasion|They come in peace. Hopefully.|space|space futuristic costume green bright
Robot Revolution|Metallic, angular and beep-boop.|space|futuristic silver costume sleek
Mermaid Lagoon|Shimmering scales and seashells.|ocean|ocean fantasy sparkly tropical
Ice Queen|Frozen, frosty and fabulous.|winter|winter royal white silver sparkly
Sun Goddess|Radiant, golden and warm.|beach|gold glam summer elegant
Moon and Stars|Celestial dreams in midnight blue.|space|space fantasy dark sparkly blue
Enchanted Garden|Talking flowers and secret paths.|fantasy|fantasy nature fairytale romantic
Dragon Rider|Soaring above mountains on a scaly friend.|fantasy|fantasy adventure edgy wild
Knight in Shining Armour|Protect the realm with honour.|history|history fantasy costume silver military
Ancient Royalty|Crowns, capes and thrones.|history|royal history gold costume
Victorian Tea|Lace gloves and dainty cups.|history|vintage elegant romantic lace
Regency Romance|Balls, letters and longing glances.|history|romantic vintage elegant pastel
Military Parade|Sharp lines and polished boots.|history|military formal history
Gym Class|Warm-ups, laps and team spirit.|sport|sporty school casual
Tennis Match|Game, set and match.|sport|sporty preppy white summer
Grand Prix|Speed, champagne and the chequered flag.|sport|sporty glam bright street
Olympic Champion|Going for gold.|sport|sporty winner gold bright
Circus Spectacular|Roll up for acrobats and ringmasters.|festival|costume colourful party bright
Magic Show|Pull a rabbit out of a hat.|formal|magic formal costume black
Carnival Parade|Feathers, sequins and samba.|festival|festival sparkly colourful glam party
Lantern Festival|Glowing lights drifting into the night.|festival|festival elegant red gold
Movie Night|Popcorn and comfy clothes.|party|casual cosy sleepwear
Birthday Bash|Cake, balloons and party hats.|party|party cute colourful bright
Prom Night|Corsages, limousines and the last dance.|formal|formal party glam dance
Graduation Day|Caps off to your achievements.|work|formal school elegant
Job Interview|First impressions count.|work|business formal sleek
Influencer Photoshoot|Ring lights and the perfect angle.|city|glam street bright party
Cosy Cabin|Fireplace, knitted blankets and snow outside.|winter|cosy winter knit brown
Hot Chocolate Day|Marshmallows and mittens.|food|cosy winter cute brown
Ice Cream Parlour|Scoops of strawberry, mint and vanilla.|food|cute pastel summer retro
Pizza Party|Cheesy, casual and fun.|food|casual party colourful red
Fruit Salad|Juicy colours from the market.|food|colourful bright summer tropical
Tea Party in Wonderland|Down the rabbit hole and late for tea.|fantasy|fairytale cute costume vintage colourful
Superstar Pop Diva|Headline the world tour.|music|glam sparkly party disco bright
Country Music Awards|Rhinestones and boots.|western|cowboy western sparkly glam
Rodeo Queen|Ride the bull and win the crowd.|western|cowboy western bright
Haute Couture|Avant-garde fashion. Push the limits.|formal|glam edgy elegant art
Minimalist Chic|Less is more. Clean lines only.|city|sleek white black business
Maximalist Madness|More is more. Layer everything.|festival|colourful bright party wild
Vintage Thrift|Treasures from the op shop.|retro|vintage retro casual brown
Neon Nights|Glow-in-the-dark city lights.|city|bright futuristic party disco
Cyberpunk|Rain-soaked streets and holograms.|city|futuristic edgy dark street
Steampunk|Gears, goggles and brass.|history|vintage adventure brown history
Nineties Grunge|Flannel, ripped denim and combat boots.|retro|punk rock casual tartan
Y2K Pop|Butterfly clips and glossy everything.|retro|retro cute sparkly pink party
Preppy Academy|Blazers, pleats and loafers.|work|preppy school vintage
Dark Academia|Old libraries and tweed.|history|vintage preppy dark brown
Cottagecore|Baking bread in a meadow cottage.|nature|picnic romantic nature cute vintage
Beach Volleyball|Spike it in the sand.|beach|beach sporty summer bright
Pool Party|Splash, float and sunbathe.|beach|beach summer party bright
Shipwrecked|Stranded on a desert island.|ocean|adventure beach pirate casual
Deep Space Gala|A formal dinner on a starship.|space|space formal glam silver futuristic
Volcano Island|Fiery reds and jungle greens.|nature|tropical adventure red orange
Snow Day|School is cancelled. Time for snowball fights.|winter|winter cosy cute
Northern Lights|Green and purple waves across the sky.|winter|winter fantasy green purple sparkly
Firefighter Hero|Bravery and bright safety colours.|work|superhero costume red work
Pastel Dream|Soft, sweet and dreamy.|party|pastel cute romantic
Rock and Roll Fifties|Leather jackets and swing skirts.|retro|retro rock vintage dance
Tourist Trap|Cameras, maps and souvenir hats.|travel|travel casual colourful summer
Glamping|Camping, but make it fabulous.|festival|festival boho glam nature
""".trimIndent()

    val all: List<Theme> = RAW.lines().filter { it.isNotBlank() }.mapIndexed { i, line ->
        val p = line.split("|")
        Theme(i, p[0], p[1], p[2], p[3].split(" ").filter { it.isNotBlank() })
    }

    val count get() = all.size
}
