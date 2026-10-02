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
5. **Use the Warden Core on the drum.** The drum calls out a rhythm, a beat and a rising note at a
   time, and the sensors light up along. **Play it back** by hitting the drum (left- or
   right-click) in the same rhythm. There are three rounds, each a little longer and stricter. Come
   in too early or too late and the sculk shrieks, and you try that round again. (Hit the drum
   before slotting the core and it tells you what is still missing.)
6. Pass the third round and the gate wakes: the camera pulls back, the rim lights up note by note,
   souls spiral in and the portal closes from the rim inward. Step in.

Arriving in The Sift builds a small reinforced deepslate gate that leads back to the gate you came
from. Later you can build your own portals: any vertical frame of **Sift Gate Frames** (or
reinforced deepslate) opened with the same drum ritual. A Sift Gate Frame is very expensive:
4 Siftite Ingots + 4 Echo Shards around a Nether Star make 4 frames, and the smallest gate (a 2x3 opening) needs 10.
The drum refuses a second core for a gate that is already open, re-reads the frame before it wakes,
picks a waking gate back up after a world reload, and tears the membrane (keeping its core) if the
frame is broken mid-opening.

---

## What's inside

### Biomes
* **Sift Plains**: salmon Coral Turf thick with Coral Bushes and tall Coral Thickets, under pale
  weeping lullwood trees.
* **Forest Mountains**: high ground thick with fluffy lullwood and wishwood trees.
* **Wishing Grove**: wishwood glades where wishing stars fall.
* **Rocky Dunes**: rolling dreamsand, sandstone spires and boulders, with Sifters lurking below.
* **Chrome Lakes**: wide, calm lakes of Chrome.
* **Deep Sift**: hushslate caves full of sculk and glowing plants, watched over by Wardens.

A clean cyan sky over mint haze, the odd drifting soul, pollen mote and falling leaf, and your
footsteps leave puffs of dream dust.

### Creatures
* **Bulb**: a bouncy, squishy jelly bunny that leaves a slime trail. Breed them with **Pitcher
  Bulbs**. Happy Bulbs plop out **Glowing Slime Balls**.
* **Slumbler**: a huge, wide-mouthed Chrome salamander that lounges in lakes. Drops **Thick
  Hide** and, rarely, a **Chrome Pearl**.
* **Sniffer**: ordinary vanilla Sniffers roam The Sift; wherever one digs here it also turns up the
  dimension's exclusive seeds (Choir Pods, Echo Seeds and Pitcher Bulbs).
* **Harmoner**: a colourful songbird. **Feed it seeds and it leads you to a structure**, singing
  all the way. Rose: Abandoned Altar. Azure: Chrome Well. Gold: Dream Statue. Violet: Collapsed
  Tower. Jade: Sift Ruins. Coral: Musical Temple. The rare Night Harmoner: the Sculk Castle.
* **Sifter** (hostile): a box-headed dune lurker whose lid snaps open like a trap. It burrows
  in the sand and bursts out when you come near.
* **Enchoer**: a big, sad, furry trader with moose antlers. Trades Sift goods for Chrome
  Pearls and hums along to music.
* **Riveter** (hostile): the sculk bat. It hangs head-down from cave ceilings with its long claws
  dangling, and screams to wake nearby Wardens.

### The Conductor's orchestra
Three great players wait in the Sift, each woken by a violet **Encore Sigil**:
* **The Thumper** (percussion) waits in the middle of its **Drum Pit**, a round arena with four
  towers. It is a giant turtle with a war drum on its shell, and only hits on the drum hurt it.
  Jump its slam rings. If its charge hits a wall it is dazed.
  * At half health it wakes the sculk in its shell and grows into a **titan** three times its
    size, plated down its back like a monster-film kaiju. After that, nothing on the ground can
    hurt it.
  * Climb a tower and leap onto its back. The sculk deck there is solid, and every blow struck
    from it lands.
  * It shakes itself to throw you off (crouch to hold on). Sculk Parasites crawl out of its shell.
  * On the ground it stomps (jump the shockwave) and breathes a sweeping beam of sculk song (hide
    behind a tower).
  * Drops the **Conga Drum**.
* **The Whistler** (wind) circles roofed towers. It is a sculk crane whose song-beam locks on until
  you break its line of sight. Drops the **Crane Beak**, which makes a flute.
* **The Strummer** (strings) lurks in deep shrines. It is a mantis riding a spider and playing it
  like a guitar. Drops **Magic Strings**, which make a guitar.

Their young haunt ruins everywhere: Whistlings, Strumlings and **Sculk Parasites**. A parasite is a
small, fragile centipede that bursts when it bites, leaving **Sculk Corruption II** in you. Each
further bite deepens it.

**Sculk Corruption** is a slow wither that takes your sight:
* sculk crust grows in from the corners of the screen
* glowing veins pulse with a heartbeat you can hear
* tentacles writhe in from the edges, more and closer the deeper it goes.

Milk washes it away.

On the roof of the **Sculk Castle** stands the Grand Stage. Set the drum, flute and guitar on its
three altars and the **Conductor** rises from his Mask. He fights in three movements, borrowing
the shell, the wings and the strings of his players. Beat him for the **Conductor's Staff**.

### The Sift Codex
A field guide in the creative Items tab. Its pages show every creature alive and animated, plus
the items, places, music and the boss fight, with the page flipping as you turn it.

### Chrome
A shifting cyan pearl liquid. It **heals** whatever soaks in it, but it is thick like
quicksand: you slowly sink. **Hold Shift to rise.** Collect it with a bucket.

### Gear
* **Serbim**: an extremely rare ore, small veins buried deep. **4 Serbim Ingots + 4 Echo Shards
  around a Netherite Ingot = 2 Siftite Ingots.**
* **Siftite** armor and tools: upgrade **netherite** gear with Siftite at a smithing table, using the
  **Siftite Upgrade Smithing Template** found in Sift structures (one template plus serbim and
  dreamstone duplicates it). Curved, sung-into-shape tools that beat Netherite on durability,
  speed and damage and hit with heavy knockback. Patterned armour with glowing echo inlays: any
  piece keeps you from being Deafened, each piece takes an eighth off sonic damage, the helmet
  breathes under water, leggings and boots swim faster, and the full set halves Sculk Corruption.
* **Sift gardening**: Choir Pods grow only under open sky, Echo Seeds only in the dark (faster on
  sculk), Pitcher Bulb bushes only with water or Chrome within 4 blocks.
* **Pitcher Planter**: pot a Pitcher Bulb in it and it grows anywhere, through four stages, then
  gives **Pitcher Nectar** again and again. Nectar makes three soups: **Lullaby Soup** (lullaby
  bell + choir pod: regeneration, absorption), **Echo Chowder** (echo orchid + glowcap: night
  vision, haste) and **Chrome Bisque** (chrome reeds + glowing slime ball: water breathing,
  dolphin's grace).
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
ruins, deep shrines, the Thumper's Drum Pit and the Sculk Castle. They are half reclaimed by vegetation and hold harmony-stone puzzles,
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
