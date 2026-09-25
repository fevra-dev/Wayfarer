# Wayfarer Compass

A Skyrim-style compass strip for RuneLite.

A heading tape sits at the top of the game view. A fixed amber caret marks where your camera
points, and the tape of cardinal letters and ticks slides beneath it as you turn. Nearby players
and NPCs ride along the bottom edge as small dots, so you can tell at a glance what's around you
and which way it is.

## What the dots mean

| Dot | Meaning |
|---|---|
| White | Another player |
| Red | An NPC you can attack |
| Yellow | Any other NPC |

Closer things draw brighter. Only players and NPCs that already appear on your minimap are marked.

## Options

Under **Markers**, each kind of dot has its own switch, so you can show exactly what you want:

- **Players**: other players (white).
- **Monsters**: NPCs you can attack (red).
- **Other NPCs**: everything else, such as bankers and shopkeepers (yellow).
- **Range**: how far away (1 to 50 tiles) something can be and still get a dot. The default is 25.

Turn all three off for a plain compass.

The strip starts at the top centre of the game view. Hold Alt and drag it to move it.

## What it doesn't do

It only draws. It never moves your camera, clicks, or sends anything to the game.

## Building

Needs Java 11. Run `./gradlew build` to build and test, or `./gradlew run` to launch a development
client with the plugin loaded.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
