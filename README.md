# Wayfarer Compass

A Skyrim-style compass strip for RuneLite.

A heading tape sits at the top of the game view. A fixed amber caret marks where your camera
points, and the tape of directions (N, NE, E, SE, S, SW, W, NW) and ticks slides beneath it as you turn. Nearby players,
NPCs and ground items ride along the bottom edge as small dots, so you can tell at a glance what's
around you and which way it is.

## What the dots mean

| Dot | Meaning |
|---|---|
| White | Another player |
| Orange | An NPC you can attack |
| Yellow | Any other NPC |
| Red | Items on the ground, such as drops (one dot per tile, like the minimap) |

Closer things draw brighter, except within the last few tiles, where dots fade back down: something
running right past you swings across the whole strip in a blink, and you can already see it on
screen. Dots also glide rather than jump: each eases toward its true direction and never moves faster
than half a strip width a second, while turning your camera still moves everything instantly. When
someone runs straight through you, their dot fades back in on the other side instead of sliding the
long way round. Only
things that already appear on your minimap are marked.

## Options

Under **Strip**:

- **Shape**: rounded *Pill* ends, *Square* corners, or *Pointed* tips.
- **Centre on game view**: pins the strip to the exact top centre of the game view. RuneLite's own
  top-centre spot centres on the area left of the minimap and inventory, so it sits a little left of
  true centre. On by default; turn it off to drag the strip anywhere or snap it to a corner.
- **Background opacity**: how solid the strip is, from 0 to 100%. The default, 65%, is the lightest
  setting at which the letters stay clearly readable over bright scenery like fog, sand and pale stone.

Under **Markers**, each kind of dot has its own switch, so you can show exactly what you want:

- **Players**: other players (white).
- **Monsters**: NPCs you can attack (red).
- **Other NPCs**: everything else, such as bankers and shopkeepers (yellow).
- **Ground items**: tiles with items on them, such as drops (red).
- **Colours**: each kind of dot has its own colour picker, including transparency.
- **Range**: how far away (1 to 50 tiles) something can be and still get a dot. The default is 25.
- **Distance as height**: raises dots the further away they are, so close things sit at the bottom of
  their lane and distant ones near the top of it. On by default. Dots have their own lane under the
  direction letters, so they never cover them.
- **Distance as size**: shrinks dots the further away they are, so close things draw large and distant
  ones small. Off by default, and works together with distance as height.
- **Range follows zoom**: zoomed all the way out, dots reach your full Range; zooming in narrows them
  to roughly the nearest third, the way the minimap shows less as it zooms in. Off by default.

Turn them all off for a plain compass. The **Reset** button at the bottom of the settings panel
puts everything back to the defaults.

To move the strip, turn off **Centre on game view**, then hold Alt (Option on a Mac) and drag, the way
RuneLite moves any overlay; Alt+right-click puts it back.

## What it doesn't do

It only draws. It never moves your camera, clicks, or sends anything to the game.

## Building

Needs Java 11. Run `./gradlew build` to build and test, or `./gradlew run` to launch a development
client with the plugin loaded.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
