# Makeover with Edie

An Android dress-up and runway game. You get a random theme, five minutes to walk a big department store and put together an outfit, then you strut the runway and the audience votes for the best look.

## Download

Get the latest APK: https://github.com/nugggy/runway-rush/releases/latest/download/MakeoverWithEdie.apk

On your phone, open the link, download the file and allow your browser to install apps when Android asks. The game needs Android 8.0 or newer.

## What's in the game

- A character creator covering skin tone, body shape, height, 22 hairstyles, 16 hair colours, eyes, brows, lips, makeup, freckles and facial hair.
- 2,709 clothing items across 201 styles: tops, bottoms, dresses and suits, outerwear, shoes, hats, eyewear, necklaces, earrings, bags, and wings and capes. Fabrics use real textile photos (denim, sequin, tartan, velvet, lace and more).
- 140 themes, from Beach Day and Disco Fever to Cyberpunk and Fairytale Princess. Every theme is checked so each department stocks pieces that suit it, and racks with on-theme stock show a gold star in the store and on the minimap.
- A walkable store with 11 departments, a minimap, fitting rooms and a mirror, plus AI rivals shopping beside you.
- A runway show with lights, camera flashes and a crowd, then a vote judged by players, AI rivals and an audience panel.
- WiFi party mode for up to 8 phones on the same network. Phones find each other automatically, or you can join by IP address. AI rivals can fill empty spots.
- An update check at launch. When a newer release is on GitHub, the game offers to download and install it.
- A Share button on the main menu that sends a link to the latest APK.

Music and sound effects are synthesised by the game itself, so it ships no audio files.

## Building

Requirements: JDK 17 and the Android SDK (platform 36).

```
./gradlew assembleRelease
```

Release signing reads `keystore/keystore.properties`, which is kept out of the repository. Without it, Gradle produces an unsigned release build. Debug builds (`./gradlew assembleDebug`) always work.

## Publishing an update

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build the release APK.
3. Create a GitHub release tagged `vX.Y.Z` and attach the APK named exactly `MakeoverWithEdie.apk`. That fixed name keeps the share link and the in-app update channel working.

All phones in a WiFi party must run the same version.

## Image credits

Photos and fabric textures come from Unsplash and Pexels under their free licences. See [ASSET_CREDITS.md](ASSET_CREDITS.md) for each file's source.
