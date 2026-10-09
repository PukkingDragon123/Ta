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
* **Sculk Swamp**: low, sculk-rotted coastland of Sculk Mud and glowing teal **Sculk Water** (it slowly
  corrupts whatever wades in it), gnarled Blightwood trees and Sculk Parasites crawling out of the mud.
* **Sculk Ocean**: cold, dark teal sea over trenches, ridges and pale pillars, with Sculk Coral reefs.
* **Sky Islands**: giant floating islands of pale rock and cyan Sky Grass high above the land, joined by huge Sky Roots
  and Sky Vines. Grab a vine (use it, or jump into it) and swing: lean forward to pump, sneak to slide down, jump to let
  go and fly. Skypalms (their vines run from tree to tree), puffy Cloudpuff trees, Fluffbushes that catch your fall,
  giant glowing Driftfruits at the ends of vines and bunches of Skyrinds, the sky bananas.

Biome edges blend: ground covers fray into each other and woods thin out across a border.

A clean cyan sky over mint haze, the odd drifting soul, pollen mote and falling leaf, and your
footsteps leave puffs of dream dust.

### Creatures
* **Bulb**: a bouncy, squishy jelly bunny that leaves a slime trail. Breed them with **Pitcher
  Pods**. Happy Bulbs plop out **Glowing Slime Balls**.
* **Slumbler**: a huge, wide-mouthed Chrome salamander that lounges in lakes. Drops **Thick
  Hide** and, rarely, a **Chrome Pearl**.
* **Sniffer**: ordinary vanilla Sniffers roam The Sift; wherever one digs here it also turns up the
  dimension's Echo Seeds, Pitcher Pods and Torchflower Seeds.
* **Harmoner**: a colourful songbird. **Feed it seeds and it leads you to a structure**, singing
  all the way. Rose, Gold and Coral: the Drum Pit. Azure and Jade: a Caravan colony. Violet and
  the rare Night Harmoner: the Sculk Castle.
* **Sifter** (hostile): a box-headed dune lurker whose lid snaps open like a trap. It burrows
  in the sand and bursts out when you come near.
* **Enchoer**: a big, sad, furry trader with moose antlers. Trades Sift goods for Chrome
  Pearls and hums along to music.

### The Conductor's orchestra
Three great players wait in the Sift, each woken by a violet **Encore Sigil**:
* **The Thumper** (percussion) sleeps under its **Drum Pit**, a sunken amphitheatre ringed by four
  cannon towers. Walk up to its sigil and it claws its way up out of the floor.
  * Its shell turns every blade and arrow. Only a **cannonball** landing on one of its glowing
    **sculk vents** hurts it, and the vents open only when it strains: after a stomp, while it
    breathes its beam, when it bursts out of the ground, and when it rams solid stone.
  * Each tower has an **Ancient Cannon** on top and chests of cannonballs. Use a cannon with a
    cannonball to load it, then use it again to fire where you look (look higher to lob further).
    Redstone fires it too. Cannonballs are crafted from iron, gunpowder and cobbled dreamstone.
  * At first it stomps (jump the rings), charges and sweeps its tail. Hurt, it breathes a beam of
    sculk song and throws boulders. Enraged, it burrows and erupts under you.
  * Its charges and slams smash the arena's crumbling walls, but never anything you built.
  * Drops the **Conga Drum**.
* **The Weaver** (strings) waits under a ring of humming webs in a hollow of the Sculk Swamp. It is a mantis riding a spider and playing it
  like a guitar. Drops **Magic Strings**, which make a guitar.

Their young haunt the Sculk Swamp: Sculk Spiders and **Sculk Parasites**. A parasite is a
small, fragile centipede that bursts when it bites, leaving **Sculk Corruption II** in you. Each
further bite deepens it.

**Sculk Corruption** is a slow wither that takes your sight:
* sculk crust grows in from the corners of the screen
* glowing veins pulse with a heartbeat you can hear
* tentacles writhe in from the edges, more and closer the deeper it goes.

Milk washes it away.

On the roof of the **Sculk Castle** stands the Grand Stage. Set the drum, flute and guitar on its
three altars. When the music ends only his Mask is left on the floor, and souls, notes and sculk
rebuild the **Conductor** around it. He fights alone in three movements, each more godlike: a
duelist on the stage, then floating with note barrages and chord shockwaves, then soaring high,
raining notes and flooding the stage with sound (stand in a lit circle). Beat him for the
**Conductor's Staff**. The **Crane Flute** is crafted from a bone, an amethyst shard and an echo
shard.

### The Knowledge Book, lore and the Mini Creator
The **Knowledge Book** (a book, an ink sac and a gold nugget) writes itself as you play: creatures you meet
(alive on their page, with field notes), songs and their sheet art, Sift enchantments, lore, places, the items
you hold and every Sift recipe you know. Undiscovered pages stay sealed in glyphs; creative shows everything.
**Lore Books** and **Lore Scrolls** tell the story of the Creator, the Rifts, the Pillagers, the Sculk and the
Heralds. Find them in Sift relics and chests and in Overworld outposts, mansions, libraries, temples, ancient
cities and shipwrecks; each origin has its own look (Creator, Pillager, cultist, Tide-Keeper, Soul). Sneak-use a
book on a block to set it down. Much of the old writing is in the enchanting-table script: every piece you read
deciphers more, and the words morph into English with a Sculk whisper. **Music Sheets** show their notes and
ink diagrams of their instrument and creature. On your first steps in the Sift the **Mini Creator**, a small
platypus in the Creator's white and gold, appears with your first goals (an instrument, a sheet, a song, the
Europhy Table, the lore, the Heralds); talk to him to hear your goal again.

### Chrome
A shifting, translucent cyan pearl liquid. It **heals** whatever soaks in it, but it is thick like
quicksand: you slowly sink. **Hold Shift to rise.** Collect it with a bucket.

### Gear
* **Materials**: vanilla **Copper**; **Prism** gems (iron pickaxe, all through the Sift caves); **Siftite Ore**
  (diamond pickaxe, rare) drops **Siftite Dust** - 4 Dust and a Copper Ingot become a **Siftite Ingot** in the
  **Europhy Table**; **Scukite** grows rarely in sculk (Raw Scukite smelts into Scukite); **Bauxite** explodes when
  mined unless you or the ore are in Chrome - a strong fuel, and Stable Bauxite (Europhy Table, with Nib Dust
  and a living Nib nearby) burns twice as long; **Magnesite** and **Galena** seams; Soul Dust, Pieces, Chunks
  and Pure Soul Blocks (9 to 1, both ways).
* **Siftite** tools and weapons: upgrade **netherite** tools with Siftite at a smithing table, using the
  **Siftite Upgrade Smithing Template** found in Sift structures (one template plus Siftite Dust and
  dreamstone duplicates it). Curved, sung-into-shape tools that beat Netherite on durability,
  speed and damage and hit with heavy knockback. There is no Siftite armour.
* **Sift gardening**: Echo Seeds grow only in the dark (faster on sculk). Vanilla Pitcher Plants grow
  wild in the plains and groves.
* **Pitcher Planter**: pot a Pitcher Pod in it and it grows anywhere, through four stages, then
  gives **Pitcher Plants** again and again. They make three soups: **Lullaby Soup** (lullaby
  bell + soulpetal: regeneration, absorption), **Echo Chowder** (echo orchid + glowcap: night
  vision, haste) and **Chrome Bisque** (chrome reeds + glowing slime ball: water breathing,
  dolphin's grace).
* **Slingshot**: fires Glowing Slime Balls that burst into a dazzling area of light, outline
  creatures, dazzle monsters and **Deafen Wardens** for a while.
* **Sift Cake**, **Dream Stew**, **Glowcap Skewers** and other dreamy food.

### Music
Music physically changes The Sift. Drums, note blocks and jukeboxes send out resonance pulses
that make plants grow, flowers sing, particles dance and creatures react.

The **Europhy Table** is a clockwork crafting machine (Magnesite, Copper and Prism). Set the ingredients on
its four arms, then play music nearby: every new note winds it up, a whole song fills it at once, and the
output forms in the centre. It makes Siftite Ingots and Stable Bauxite, and - as the old Euphory Altar did -
enchants an item laid beside a Chrome Pearl, better the more varied your tune.

### Structures
The Thumper's Drum Pit, the Sculk Castle and the Caravan colonies. The Sift's older towers, temples
and settlements have crumbled into the ground: their relics lie buried as suspicious dreamsand in
the dunes, plains, lake shores and seas (templates, music sheets, glyph stones), and here
and there an Echoer still keeps its hearth.

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
