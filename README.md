# The Sift

*A dimension of soul, dreams, music and healing: the Nether's gentle opposite.*

The Sift is a NeoForge mod for **Minecraft Java Edition 26.3**. It adds a whole new dimension
to explore, reached by waking the great gate at the heart of an Ancient City with a Warden Core
and a drum.

---

## Install (CurseForge app)

1. **Get the jar.** Open the latest successful run of the **Build** workflow under this
   repository's [Actions](../../actions) tab and download the `the-sift-neoforge` artifact. It is
   a zip: unzip it to get `the-sift-neoforge-1.0.0+mc26.3.jar`.
2. In the CurseForge app open **Minecraft → Create Custom Profile**.
3. Choose game version **26.3**, mod loader **NeoForge**, loader version **26.3.0.23-beta** or
   any newer 26.3.x build, then click **Create**.
4. On the new profile click **⋯ → Open Folder**, open the `mods` folder (create it if it is
   missing) and drop the jar in.
5. Press **Play**. The Sift's blocks and items appear in two new creative tabs.

The mod has no other dependencies. Java 25 is bundled with the Minecraft launcher, so you don't
need to install it yourself. For a server, put the same jar in the server's `mods` folder; every
player needs it too.

---

## Getting to The Sift

1. **Find an Ancient City** deep in the Overworld's Deep Dark.
2. **Get a Warden Core.** Every Warden drops one, and Ancient City chests sometimes hold one.
3. **Craft a Sift Drum** (3 leather on top, planks around an echo shard, note block in the bottom
   middle):

   ```
   L L L      L = Leather
   P E P      P = any Planks, E = Echo Shard
   P N P      N = Note Block
   ```

4. Stand at the city's great **reinforced deepslate gate**. Place the drum near it, with **at
   least three Sculk Sensors** within 8 blocks of the drum.
5. **Use the Warden Core on the drum.** The drum calls out a rhythm and the sensors flash along.
   **Play it back** by hitting the drum (right-click) in the same rhythm. There are three rounds,
   each a little longer and stricter. Come in too early or too late and the sculk shrieks, and you
   try that round again.
6. Pass the third round and the gate erupts in cyan and pink souls and opens. Step in.

Arriving in The Sift builds an Echo Frame portal that leads back to the gate you came from.
Later you can build your own portals: any vertical frame of **Echo Frame** blocks (or
reinforced deepslate) opened with the same drum ritual.

---

## What's inside

### Biomes
* **Sift Plains**: soft blushgrass meadows, dreamblooms, soulpetals and lullwood groves.
* **Forest Mountains**: high ground thick with fluffy lullwood and wishwood trees.
* **Wishing Grove**: wishwood glades where wishing stars fall.
* **Rocky Dunes**: rolling dreamsand, sandstone spires and boulders, with Sifters lurking below.
* **Chrome Lakes**: wide lakes of Chrome under a nebula sky.
* **Deep Sift**: hushslate caves full of sculk and glowing plants, watched over by Wardens.

Cyan and pink fog, a rotating nebula sky, drifting souls, pollen, mist, glow dust and falling
leaves are everywhere, and your footsteps leave puffs of dream dust.

### Creatures
* **Bulb**: a bouncy, squashy bunny. Breed them with **Pitcher Bulbs**. Happy Bulbs plop out
  **Glowing Slime Balls**.
* **Slumbler**: a huge, wide-mouthed Chrome salamander that lounges in lakes. Drops **Thick
  Hide** and, rarely, a **Chrome Pearl**.
* **Sniffer**: lives in The Sift naturally and digs up its exclusive seeds (Choir Pods, Echo
  Seeds and Pitcher Bulbs).
* **Sifter** (hostile): a cyan, squid-like burrower that bursts out of the dunes.
* **Enchoer**: a tall, crystalline trader with wing-like arms. Trades Sift goods for Chrome
  Pearls.
* **Riveter** (hostile): hangs from cave ceilings and screams to wake nearby Wardens.

### Chrome
A shifting cyan, pink and pearl liquid. It **heals** whatever soaks in it, but it is thick like
quicksand: you slowly sink. **Hold Shift to rise.** Collect it with a bucket.

### Gear
* **Serbim**: a rare ore. **Serbim Ingot + Copper Ingot = Siftite Ingot.**
* **Siftite** armor and tools: upgrade copper gear with Siftite at a smithing table, using the
  **Siftite Upgrade Smithing Template** found in Sift structures. One template plus serbim and
  dreamstone duplicates it.
* **Slingshot**: fires Glowing Slime Balls that burst into a dazzling area of light, outline
  creatures, dazzle monsters and **Deafen Wardens** for a while.
* **Sift Cake**, **Dream Stew**, **Glowcap Skewers** and other dreamy food.

### Music
Music physically changes The Sift. Drums, note blocks and jukeboxes send out resonance pulses
that make plants grow, flowers sing, particles dance and creatures react.

The **Euphory Altar** is a mechanical enchanting altar powered by drums. Place an unenchanted item
or a book on it, ring it with **two or more Sift Drums** (up to 4 blocks away), then use a
**Chrome Pearl** on it. The more drums, the stronger the enchantment, well beyond an enchanting
table's limit.

### Structures
Collapsed towers, broken musical temples, Chrome wells, abandoned altars, giant stone
instruments (a harp and a drum), ruined bridges, buried dune settlements, mysterious statues, Sift
ruins and deep shrines. They are half reclaimed by vegetation and hold harmony-stone puzzles,
sealed vaults, snares and crumbling floors, suspicious dreamsand to brush, and lore in the form of
Dream Journal fragments.

---

## Building from source

Requires JDK 25.

```
./gradlew build          # the jar ends up in build/libs/
./gradlew runClient      # start a development client
```

Assets, textures, models, world generation and structure NBTs are generated by the Python scripts
in `tools/` (`python3 tools/build_all.py`, needs `nbtlib` and `Pillow`). The generated files are
committed, so you only need to run them after changing the generators.

CI builds the jar, then boots a headless server that generates The Sift and plays through the
portal ritual, and finally a real client on a virtual display that takes screenshots of every
showcase (uploaded as the `client-screenshots` artifact).
