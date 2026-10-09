"""F3 Knowledge & lore: the Knowledge Book, the Lore Books and Scrolls, the story they tell, the ancient
script, Music Sheet art and the Mini Creator's quest lines.

Hooked in from one line each in spec.py (declare), gen_assets.py (gen_block for the placed books,
assets, recipe advancements in finalize), gen_data.py (relic archaeology entries), gen_textures.py
(textures, in tools/knowledge_art.py), items16.py (sprites) and mobs.py (tools/mini_creator.py).

The lore ids, origins and kinds here must match com.thesift.knowledge.Lore (Java); the quest order
must match com.thesift.knowledge.Quest.
"""
import json
import os

NS = 'thesift'
ORIGINS = ['creator', 'pillager', 'cultist', 'ocean', 'soul']
ORIGIN_TEXT = {
    'creator': ("Creator's Tome", "Creator's Scroll", 'The Creator', 'MapColor.SNOW'),
    'pillager': ('Pillager Journal', 'Pillager Dispatch', 'The Pillagers', 'MapColor.COLOR_BROWN'),
    'cultist': ('Sculk-Bound Grimoire', 'Sculk-Bound Scroll', 'The Sculk Cult', 'MapColor.COLOR_CYAN'),
    'ocean': ("Tide-Keeper's Ledger", 'Clamshell Scroll', 'The Tide-Keepers', 'MapColor.METAL'),
    'soul': ('Soul Tome', 'Soul Scroll', 'The Soul Dimension', 'MapColor.COLOR_BLUE'),
}

# (id, origin, kind, title, body, where it is found)
LORE = [
    ('creator_rifts', 'creator', 'book', 'On the Rifts',
     "I found the first Rift by listening. A hum under the riverbank, a note the water could not hold. I pressed my bill to "
     "the mud and the world sifted: grains of my world fell through, and grains of another fell back. That other world was the "
     "Sift. Its skies were full of floating stone and its rocks sang when the wind touched them. A Rift does not tear, it "
     "sifts - it lets through what belongs on the other side and keeps the rest. I learned to open them with a chord. I learned "
     "to close them with silence. Never leave one half open.",
     'Sift relics, stronghold libraries'),
    ('creator_songs', 'creator', 'scroll', 'The Sift Sings',
     "Everything here has a voice. The jelly Bulbs hum when they hop, the whales sing to the fruit, the sand chimes under every "
     "step. I gave names to the songs I heard and wrote them down, so that others could play them back: the Offering, the "
     "Lullaby, the Whale Song. Play a song true and the Sift answers. Creatures gather and play along; stone remembers the tune. "
     "This is the first law of the Sift: what is sung is real. I built instruments of copper and prism so that small hands could "
     "play what the great creatures sing.",
     'Sift relics, Caravan crystals'),
    ('creator_hall', 'creator', 'book', 'The Opera Hall',
     "Deep under the dunes I am building a hall for the songs. Brick corridors behind the stage, rooms for rehearsing, rooms for "
     "instruments, archives for every sheet I have written. Then the hall itself: a stage wide enough for a whale, balconies "
     "stacked like cliffs, and seats for every creature of the Sift. I carved myself above the stage, holding my baton - forgive "
     "an old platypus his pride. When it is finished the whole Sift will sing at once, and the Rifts will hum in tune. I will "
     "call it the Conductor Opera Hall.",
     'stronghold libraries, desert and jungle temples'),
    ('creator_kin', 'creator', 'scroll', 'My Kin of the Riverbanks',
     "Before the Rifts there were many of us. Platypuses of the Overworld, on every riverbank, singing to the water in the "
     "evening. We built little stone weirs and hung chimes in the reeds. Then the grey-coated ones came with crossbows and "
     "banners. They burned the reeds and broke the weirs and took our chimes for trophies. I was the last to dive through a "
     "Rift. Sometimes I hear their banners flapping in my sleep. If you find a white-and-gold stone by an Overworld river, it "
     "was ours.",
     'desert and jungle temples, Sift relics'),
    ('pillager_purge', 'pillager', 'book', "Captain Vask's Log",
     "Day 41. The duck-beaks are gone from the southern river. The men collected their bells and gold trinkets - the Captain "
     "wants every piece. One of them, a fat one in white robes, fell into the water and never came up. The men swear the river "
     "glowed and swallowed him whole. Day 44. Found carvings in the reeds: circles within circles, and that same fat duck-beak "
     "with blocks floating around him. The scholars say the carvings are a map. The Captain says they are a door. We march at "
     "dawn.",
     'pillager outposts, woodland mansions'),
    ('pillager_rift', 'pillager', 'scroll', "Engineer's Notes: The Copper Rift",
     "The duck-beaks opened their doors with music. We cannot sing, so we built. Copper coils, one inside another, wound to the "
     "pitch of the carvings; a drum of Warden hide to strike it. On the ninth try the air folded and the ground beneath the frame "
     "fell UP. Three men went through and came back with pockets full of glowing crystal. The Captain calls it the Copper Rift. "
     "It is loud, it smells of burning, and it does not close properly. We will fix that later.",
     'pillager outposts, the Drum Pit armoury'),
    ('pillager_camp', 'pillager', 'book', 'Camp Ledger',
     "Stores: 40 crossbows, 12 cages, 3 drills (one broken). Prism mined this week: 9 crates. Siftite dust: 2 sacks - the smiths "
     "cannot work it without the singing tables. Creatures taken: 4 jelly-hoppers, 1 whale calf (escaped), 2 chiming beetles. "
     "The laboratory wants more. The men complain the ground sings at night and the cages hum along. Note: do not cage anything "
     "that sings in tune with another cage. Last week's laboratory is now a crater.",
     'the Drum Pit armoury, abandoned mineshafts'),
    ('pillager_stranded', 'pillager', 'scroll', 'The Last Entry',
     "The Copper Rift is shut. Nobody shut it. It went quiet one morning like a held breath and never let go. No supplies for "
     "sixty days. The men have stopped arguing and started listening to the sand. Some of them have walked into the dunes to "
     "follow the music and not come back. I have stopped wearing my banner. If anyone reads this: the duck-beak did not die in "
     "that river. He went home. We followed him into his home and broke it. I am sorry.",
     'the Drum Pit armoury, Sift relics'),
    ('cultist_deal', 'cultist', 'book', 'The Bargain Below',
     "Hear the truth the Creator hid. Beneath the Sift lies the Soul Dimension, where every spent soul sinks like silt. The "
     "Creator went down there seeking a deeper note, a song beneath all songs. In the dark he met the Sculk. It spoke without a "
     "mouth. It offered him a voice to make the whole Sift sing as one, forever, never out of tune. He said yes. Now the Sculk "
     "hums in his throat and we hum with it. Join the chorus. It does not hurt for long.",
     'ancient cities, woodland mansions'),
    ('cultist_heralds', 'cultist', 'scroll', 'Hymn of the Five Heralds',
     "With the Destroyer Tool the Sculk unmade and remade. From the deep river it raised the Crocodile, armoured drum of the "
     "deep. From the high nests, the Owl, whose breath is a gale. From the drowned reef, the Octopus, eight arms upon the organ. "
     "From the dark threads, the Weaver, who strums the webs of the world. From the throne of dust, the Dictator, whose word is "
     "law. Five Heralds, five voices of one song. Bow when they sing.",
     'ancient cities, the Sculk Castle'),
    ('cultist_conductor', 'cultist', 'scroll', 'The Baton Unbroken',
     "The Creator's hall did not fall silent. The Sculk sent a conductor to keep the music going: a tall thing in a tailcoat, "
     "his face a porcelain mask, his baton black as the deep. He gathers the lost and the broken and teaches them to play. The "
     "audience never leaves. Those who find the Opera Hall should know: the Conductor is not one of the five Heralds. He is "
     "older, or newer, or something the Sculk has not finished.",
     'the Sculk Castle'),
    ('ocean_reef', 'ocean', 'scroll', 'Song of the Brass Reef',
     "Where the Rift opens under the sea, the coral grows in brass. Trumpet coral, horn coral, branching like the bells of "
     "instruments, singing when the current passes through. The Creator taught the reef its first tune and the reef has never "
     "stopped playing it. Silver fish keep time, the clams clap along. We, the keepers of the tide, wrote the reef-song on "
     "shells so it would never be lost. If the reef ever plays out of tune, something has gone wrong in the deep.",
     'shipwrecks, ocean ruins, buried treasure'),
    ('ocean_octopus', 'ocean', 'book', 'The Organist',
     "Once the eight-armed one played the great organ of the reef, a pipe in every arm, and the tide came in and out to his "
     "music. Then the Sculk's song came down through the water. His notes went sour. His pipes grew dark. Now he plays only for "
     "the Sculk, and the currents he calls drag ships and swimmers into the deep. The keepers of the tide still leave a pearl "
     "on the shell-altar each moon, hoping he will remember his old song.",
     'buried treasure, shipwrecks'),
    ('soul_sculk', 'soul', 'book', 'What Hungers Below',
     "The Sculk is not a plant and not a beast. It is a soul - long, thin and patient, older than the Rifts. It learned what no "
     "other soul knew: how to press souls together, tighter and tighter, until they become a living crust that listens. Every "
     "sensor, every shrieker, every vein of Sculk is a choir of compressed souls. It does not hate. It wants every song to be "
     "its song. It was waiting for someone who could sing loudly enough.",
     'ancient cities, the Sculk Castle'),
    ('soul_creator', 'soul', 'scroll', 'He Is Still Singing',
     "Deeper than the Heralds, deeper than any Warden walks, the real Creator is held. Enormous now - the Sculk fed him until he "
     "filled the cavern. Tendrils of Sculk pin his arms and wrap his robe; through his chest you can see his ribs, white as the "
     "old robe was. The Pillagers did not seal him: the Sculk did, with his own bargain. But listen closely in the Soul "
     "Dimension and under the drone you will hear a small, stubborn tune. He is still singing. Somebody has to answer.",
     'the Sculk Castle'),
]

# Where the lore turns up: loot table -> ([(lore id, weight)], chance of one piece per chest)
LORE_LOOT = {
    'minecraft:chests/pillager_outpost': ([('pillager_purge', 3), ('pillager_rift', 2)], 0.45),
    'minecraft:chests/woodland_mansion': ([('cultist_deal', 2), ('pillager_purge', 2), ('pillager_stranded', 1)], 0.5),
    'minecraft:chests/abandoned_mineshaft': ([('pillager_camp', 2), ('pillager_rift', 1)], 0.08),
    'minecraft:chests/stronghold_library': ([('creator_rifts', 2), ('creator_hall', 2), ('creator_kin', 1)], 0.5),
    'minecraft:chests/desert_pyramid': ([('creator_kin', 3), ('creator_hall', 1)], 0.2),
    'minecraft:chests/jungle_temple': ([('creator_kin', 3), ('creator_hall', 1)], 0.25),
    'minecraft:chests/ancient_city': ([('cultist_deal', 2), ('cultist_heralds', 2), ('soul_sculk', 1)], 0.35),
    'minecraft:chests/shipwreck_treasure': ([('ocean_reef', 2), ('ocean_octopus', 1)], 0.35),
    'minecraft:chests/underwater_ruin_big': ([('ocean_reef', 2), ('ocean_octopus', 1)], 0.3),
    'minecraft:chests/buried_treasure': ([('ocean_octopus', 2), ('ocean_reef', 1)], 0.5),
    f'{NS}:chests/sculk_castle': ([('cultist_heralds', 2), ('cultist_conductor', 2), ('soul_sculk', 2), ('soul_creator', 1)], 0.6),
    f'{NS}:chests/drum_pit_armory': ([('pillager_camp', 2), ('pillager_stranded', 2), ('pillager_rift', 1)], 0.4),
    f'{NS}:gameplay/frozen_crystal': ([('creator_songs', 2), ('creator_rifts', 1)], 0.15),
}
# brushed out of the Sift's buried relics (archaeology tables roll exactly one item, so these join their pools)
RELIC_LORE = {'common': [('creator_songs', 1), ('pillager_stranded', 1)], 'rare': [('creator_rifts', 1), ('creator_kin', 1), ('ocean_reef', 1)]}

# the Mini Creator's quest lines (order = com.thesift.knowledge.Quest)
QUESTS = [
    ('arrival', 'A Small Hello', 'Meet the Mini Creator',
     "Oh! A visitor through the Rift! Don't be frightened - I'm only a little piece of the Creator's song that slipped free "
     "when the big one was taken. Here, take this Knowledge Book: it writes itself as you discover things.", ''),
    ('instrument', 'Something to Play', 'Hold an instrument',
     "The Sift listens to music before it listens to anything else. Find yourself an instrument - Wind Chimes are easy: "
     "sticks, string, iron and an amethyst shard. Better ones lie in old ruins.",
     'Listen to that! Now you have a voice the Sift can hear.'),
    ('sheet', 'A Song on Paper', 'Find a Music Sheet',
     "Instruments are only half of it. The Creator wrote every song down on Music Sheets. They're buried in relics, kept in "
     "old armouries, even swallowed by Sifters. Find one!",
     'A real sheet! I remember that one. Hum it with me...'),
    ('perform', 'Play for the Sift', 'Perform a song',
     "Carry the sheet, take up the right instrument and play its notes in order. Creatures will gather round - some might even "
     "join your band!",
     "Did you see them listen? That's how it starts. That's how it always started."),
    ('europhy', 'The Europhy Table', 'Obtain a Europhy Table',
     "The Creator made machines that run on music. The greatest is the Europhy Table: copper, prism and song, and it turns "
     "Siftite Dust and Copper into Siftite. Find out how to build one.",
     'The table hums for you! The big one would be proud.'),
    ('lore', 'Old Stories', 'Read three Lore Books or Scrolls',
     "There are books and scrolls scattered everywhere - his, the Pillagers', even the cultists'. Some are written in the old "
     "script. Read a few and the letters will start to make sense.",
     "So now you know some of it. I'm sorry it isn't a happier story."),
    ('heralds', 'The Heralds', 'Face a Herald or the Conductor',
     "The Sculk made five Heralds with the Destroyer Tool: the Crocodile, the Owl, the Octopus, the Weaver and the Dictator. "
     "And the Conductor plays in the Creator's own hall. Find one of them... carefully.",
     "You faced it and you're still here! I knew you would be."),
    ('soul', 'He Is Still Singing', 'Reach the Soul Dimension - one day',
     "Deep under everything, in the Soul Dimension, the real Creator is trapped. I can hear him. Grow strong, gather the "
     "Heralds' instruments, and one day... answer him.", ''),
]

# the Knowledge Book's chapters (index = CodexEntries chapter constant)
CHAPTERS = {
    'creatures': ('Creatures', 'Every creature you meet, alive on its page: where it lives, how it behaves, what it eats and leaves.'),
    'recipes': ('Items & Recipes', 'Things you have held, and every Sift recipe you know.'),
    'structures': ('Places', 'The lands of the Sift and what was built in them, written down as you arrive.'),
    'machines': ('Music & Machines', 'Blocks and machines that run on music, and how to wake them.'),
    'heralds': ('Heralds & Bosses', 'The five Heralds of the Sculk, the Conductor, and how to beat them.'),
    'songs': ('Songs', 'Each song: its sheet, its instrument and the creatures that answer it.'),
    'enchantments': ('Enchantments', 'Sift enchantments, played into gear at the Music Band Table.'),
    'lore': ('Lore', 'The story of the Sift. Every piece you read makes the ancient script clearer.'),
    'guide': ("The Guide's Notes", 'What the Mini Creator has asked of you, and what comes next.'),
}

FIELD_NOTES = {
    'bulb': 'Habitat: Sift Plains, Wishing Grove, White Forest|Temper: Gentle, hops to music|Diet: Pitcher Pods|Drops: Glowing Slime Balls',
    'harmoner': 'Habitat: Sift Plains, Wishing Grove|Temper: Friendly guide|Diet: Seeds|Drops: Nothing - it leads you instead',
    'sniffer': 'Habitat: Plains, Forest Mountains, Rocky Dunes|Temper: Neutral, rams when hit|Diet: Pink grass, Torchflower seeds|Drops: Flowers from its back',
    'enchoer': 'Habitat: Plains and meadows|Temper: Peaceful, takes offerings|Diet: Music|Drops: Chrome Pearls, Star Shards',
    'soul_golem': 'Habitat: Echoer hearths|Temper: Helpful|Diet: Soul energy|Drops: Its core',
    'nib': 'Habitat: Flower meadows, Sound Garden|Temper: Shy|Diet: Nectar of light|Drops: Nib Dust and treasure (by song)',
    'slumbler': 'Habitat: Chrome Lakes|Temper: Sleepy, bites when woken|Diet: Chrome plankton|Drops: Thick Hide, Chrome Pearls',
    'sifter': 'Habitat: Rocky Dunes, Deep Sift|Temper: Neutral, calls its neighbours|Diet: Sand it sifts|Drops: Dreamsand, Star Shards, lost sheets',
    'stomper': 'Habitat: Plains and White Forest|Temper: Neutral giant|Diet: Chrome, Hummingblooms|Drops: Stomper Meat, Thick Hide',
    'sky_whale': 'Habitat: The skies over the islands|Temper: Gentle, rare|Diet: Giant sky fruit|Drops: Star Shards, Skysong Gems',
    'fanfare_eel': 'Habitat: Chrome Lakes|Temper: Hostile|Diet: Anything swimming|Drops: Eel meat, gold, copper',
    'kazoo_fish': 'Habitat: Chrome Lakes, seas|Temper: Skittish schools|Diet: Plankton|Drops: Kazoo Fish',
    'tubafish': 'Habitat: Chrome Lakes, seas|Temper: Defensive, stings when puffed|Diet: Plankton|Drops: Tubafish, Tuba Bubbles',
    'caravan': 'Habitat: Caravans Cavern|Temper: Territorial swarm|Diet: Raw ore|Drops: Crystal shards',
    'gobbler': 'Habitat: Sculk Ocean|Temper: Hostile, blind|Diet: Whatever it finds|Drops: Gobbler Fillet, Sculk Bladder',
    'sculk_fish': 'Habitat: Sculk Ocean, Sculk Water|Temper: Hostile schools|Diet: Anything that moves|Drops: Sculk fish',
    'coral_organ': 'Habitat: Sculk Ocean floor|Temper: Hostile, never moves|Diet: Drowned swimmers|Drops: Echo shards',
    'swifter': 'Habitat: White Forest|Temper: Neutral, defends its family|Diet: Bulbs, alien chickens|Drops: Swifter Fluff',
    'jailer': 'Habitat: Deep caves|Temper: Hostile, blind|Diet: Souls|Drops: Sculkite',
    'sculkling': 'Habitat: Dark caves|Temper: Hostile packs, thieves|Diet: Shiny things|Drops: What it stole',
    'cypole': 'Habitat: Sculk Swamp|Temper: Neutral, territorial|Diet: Swamp flies|Drops: Brass plates',
    'mini_creator': 'Habitat: Wherever you are|Temper: Kind and talkative|Diet: Songs, mostly|Drops: Advice',
    'dictator': 'Habitat: The Grand Stage|Temper: Sculk-made|Diet: Music and souls|Drops: Warden Core, Siftite, the Lullaby disc',
    'thumper': 'Habitat: The Drum Pit|Temper: Hostile|Diet: Sculk|Drops: Conga Drum, scutes, Siftite',
    'strummer': 'Habitat: Under the ground it shakes|Temper: Hostile|Diet: Anything in its webs|Drops: Weaver\'s Guitar, Sculk String',
    'strumling': 'Habitat: Wherever the Weaver nests|Temper: Hostile|Diet: Prey in webs|Drops: Sculk String, bones',
    'sculk_parasite': 'Habitat: The Conductor\'s stage|Temper: Hostile|Diet: Your health|Drops: Nothing',
}

# new Places pages (CodexEntries adds them with an icon; title, tagline, body)
PLACES = {
    'sift_plains': ('Sift Plains', 'The pink plains', 'Salmon Coral Turf as far as you can see, Coral Bushes and weeping Lullwood '
                    'trees. Bulbs hop in families here, Harmoners sing in the trees, Sift Sniffers graze and dig, and the odd '
                    'Echoer drifts by with its chimes.'),
    'chrome_lakes': ('Chrome Lakes', 'Liquid rainbow', 'Lakes of shimmering Chrome where Slumblers nap half-submerged, Kazoo Fish '
                     'school and Fanfare Eels hunt. Chime Sand settles along the shores where Chrome meets flowing water.'),
    'rocky_dunes': ('Rocky Dunes', 'The singing desert', 'Dunes of Dreamsand and rocky spires. Sifters, living bells, nap half-'
                    'buried and ring when trodden on. Buried relics hide under suspicious sand: brush them out.'),
    'forest_mountains': ('Forest Mountains', 'Lullwood heights', 'Steep slopes under pale Lullwood forests. Sift Sniffers wander '
                         'the clearings; the views reach the floating islands.'),
    'wishing_grove': ('Wishing Grove', 'Where wishes grow', 'Pink Wishwood trees over a carpet of blooms. Bulbs, Echoers, Harmoners '
                      'and allays gather here - it is the gentlest place in the Sift.'),
    'sound_garden': ('Sound Garden', 'Flowers that listen', 'A meadow of musical flowers where the Nibs flutter. Play the Song of the '
                     'Nibs here and watch them swirl into treasure.'),
    'caravans_cavern': ('Caravans Cavern', 'Crystal caves', 'Deep caves glittering with Music Crystals, Prism ore and the colonies of '
                        'the Caravans. Never break a colony\'s crystals while the soldiers are watching.'),
    'deep_sift': ('The Deep Sift', 'Below everything', 'Dark hushslate caves under the Sift where a few lost Sifters ring in the '
                  'dark. Siftite Ore hides deep down.'),
    'drum_pit': ('The Drum Pit', 'The Thumper\'s arena', 'A sunken arena ringed by cannon towers. The Thumper sleeps beneath it. The '
                 'towers keep cannonballs - and an old Pillager armoury.'),
}

SONG_TEXT = {
    'offering': 'Ring it on Wind Chimes while an Echoer waits beside your offering - a Siftite or Serbim ingot or a Prism Gem. '
                'The Echoer dances and gives something precious back.',
    'nib': 'Played on strings in the Sound Garden, it calls every Nib within twelve blocks into a swirl around you - and each one '
           'turns into treasure.',
    'golem': 'Any instrument will do. Soul Golems wake, recharge and follow you a while.',
    'crystal': "The Caravans' own hymn, rung on chimes. It calms an angry colony - or lures its queen out of her lair.",
    'whale': 'Played on a flute under an open sky, it calls a Sky Whale down to sing back to you.',
    'lullaby': 'On strings. Nearby monsters fall asleep, and even the Gobbler is lulled.',
    'aurora': 'A Prism song: every note in its colour of light, on a Prism instrument. It lights up the dark, outlines monsters and '
              'lends you night eyes.',
}
SONG_CREATURE = {'offering': 'Echoer', 'nib': 'Nibs', 'golem': 'Soul Golems', 'crystal': 'Caravans', 'whale': 'Sky Whale',
                 'lullaby': 'Gobbler and monsters', 'aurora': 'The dark itself'}

SOUNDS = {
    'knowledge.whisper': [('ambient/nether/soulsand_valley/voices1', 0.35, 1.1), ('ambient/nether/soulsand_valley/voices3', 0.35, 1.2),
                          ('ambient/nether/soulsand_valley/voices5', 0.35, 1.0), ('block/sculk/charge2', 0.25, 0.7)],
    'knowledge.decipher': [('block/sculk/spread1', 0.45, 1.4), ('block/sculk/spread3', 0.45, 1.3), ('block/enchantment_table/enchant1', 0.4, 1.3)],
    'knowledge.unlock': [('item/book/open_flip1', 0.8, 1.15), ('item/book/open_flip2', 0.8, 1.2), ('block/amethyst/shimmer', 0.6, 1.4)],
    'knowledge.read': [('block/chiseled_bookshelf/pickup1', 0.8, 0.95), ('block/chiseled_bookshelf/pickup2', 0.8, 0.9)],
    'entity.mini_creator.ambient': [('mob/frog/idle1', 0.6, 1.5), ('mob/frog/idle2', 0.6, 1.55), ('mob/armadillo/ambient1', 0.6, 1.3),
                                    ('mob/armadillo/ambient2', 0.6, 1.35)],
    'entity.mini_creator.talk': [('mob/allay/idle_with_item1', 0.5, 0.85), ('mob/allay/idle_with_item2', 0.5, 0.9),
                                 ('mob/frog/idle5', 0.5, 1.6), ('mob/frog/idle6', 0.5, 1.65)],
    'entity.mini_creator.celebrate': [('mob/allay/item_given1', 0.8, 1.0), ('mob/allay/item_given2', 0.8, 1.05), ('random/levelup', 0.35, 1.5)],
    'entity.mini_creator.step': [('mob/frog/step1', 0.4, 1.3), ('mob/frog/step2', 0.4, 1.35), ('mob/frog/step3', 0.4, 1.3)],
    'entity.mini_creator.appear': [('block/end_portal/eyeplace1', 0.6, 1.2), ('block/end_portal/eyeplace2', 0.6, 1.3),
                                   ('block/amethyst/shimmer', 0.7, 1.2)],
    'entity.mini_creator.poof': [('block/amethyst/resonate1', 0.6, 1.2), ('block/amethyst/resonate2', 0.6, 1.3),
                                 ('block/end_portal/eyeplace3', 0.5, 1.4)],
}
SUBTITLES = {
    'knowledge.whisper': 'Sculk whispers', 'knowledge.decipher': 'Ancient script shifts', 'knowledge.unlock': 'Knowledge recorded',
    'knowledge.read': 'Pages turn', 'entity.mini_creator.ambient': 'Mini Creator chirps', 'entity.mini_creator.talk': 'Mini Creator talks',
    'entity.mini_creator.celebrate': 'Mini Creator cheers', 'entity.mini_creator.step': 'Mini Creator waddles',
    'entity.mini_creator.appear': 'Mini Creator appears', 'entity.mini_creator.poof': 'Mini Creator vanishes',
}


def lore_ids():
    return [l[0] for l in LORE]


# ============================================================================ registry declarations


def declare(block, item):
    """The Knowledge Book, a Lore Book block + item and a Lore Scroll per origin, the Mini Creator's egg."""
    item('knowledge_book', cls='SiftKnowledgeBookItem', props='new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)', name='Knowledge Book')
    for o in ORIGINS:
        book, scroll, _, colour = ORIGIN_TEXT[o]
        c = o.upper()
        block(f'{o}_lore_book', 'custom', f'BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN).strength(0.5F).noOcclusion().mapColor({colour})',
              cls='LoreBookBlock', model='lore_book', item=False, loot='none', name=book, tags=['axe'], tab='lore')
        item(f'{o}_lore_book', cls='LoreBookItem', factory=f'p -> new LoreBookItem(ModBlocks.{c}_LORE_BOOK.get(), p)',
             props='new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)', name=book, tab='lore')
        item(f'{o}_lore_scroll', cls='LoreScrollItem', props='new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)', name=scroll, tab='lore')
    item('mini_creator_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModKnowledge.MINI_CREATOR.get())', tab='eggs')


# ============================================================================ assets


def gen_book_block(GA, b):
    """A placed Lore Book: a thick tome lying on its side, its cover, clasp and ornament in its origin's style.
    Facing north/east/south/west; the item uses the same model in the hand (a flat sprite in the GUI)."""
    bid = b['id']
    o = bid[:-len('_lore_book')]
    tex = {'cover': f'{NS}:block/{o}_lore_book_cover', 'side': f'{NS}:block/{o}_lore_book_side', 'pages': f'{NS}:block/lore_book_pages',
           'trim': f'{NS}:block/{o}_lore_book_trim', 'particle': f'{NS}:block/{o}_lore_book_cover'}

    def box(frm, to, faces, rot=None):
        x0, y0, z0 = frm
        x1, y1, z1 = to
        uv = {'north': [x0, 16 - y1, x1, 16 - y0], 'south': [16 - x1, 16 - y1, 16 - x0, 16 - y0],
              'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
              'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}
        e = {'from': list(frm), 'to': list(to), 'faces': {}}
        for f, t in faces.items():
            e['faces'][f] = {'texture': t, 'uv': [max(0.0, min(16.0, v)) for v in uv[f]]}
        if rot:
            e['rotation'] = rot
        return e
    all6 = ('north', 'south', 'east', 'west', 'up', 'down')
    els = [
        # back cover, page block, front cover (the book lies flat, spine to the west)
        box([2, 0, 2], [14, 1, 14], {f: '#cover' if f in ('up', 'down') else '#side' for f in all6}),
        box([2.5, 1, 2.5], [13.5, 4, 13.5], {'north': '#pages', 'south': '#pages', 'east': '#pages', 'up': '#pages', 'down': '#pages', 'west': '#side'}),
        box([2, 4, 2], [14, 5, 14], {f: '#cover' if f in ('up', 'down') else '#side' for f in all6}),
        box([1.5, 0, 2], [2.5, 5, 14], {f: '#side' for f in all6}),                                       # the rounded spine
        box([13.5, 1.5, 7], [14.5, 3.5, 9], {f: '#trim' for f in all6}),                                  # the clasp
        box([6, 5, 6], [10, 5.5, 10], {f: '#trim' for f in all6}),                                        # the raised emblem
        box([10, 0.5, 13.5], [11, 3, 14.5], {'north': '#trim', 'south': '#trim', 'east': '#trim', 'west': '#trim'}),  # a ribbon
    ]
    for corner in ((2, 2), (12.5, 2), (2, 12.5), (12.5, 12.5)):                                          # corner fittings
        x, z = corner
        els.append(box([x, 5, z], [x + 1.5, 5.25, z + 1.5], {f: '#trim' for f in all6}))
    model = {'parent': 'minecraft:block/block', 'textures': tex, 'elements': els,
             'display': {
                 'gui': {'rotation': [30, 225, 0], 'translation': [0, 2, 0], 'scale': [0.85, 0.85, 0.85]},
                 'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
                 'fixed': {'rotation': [90, 0, 0], 'translation': [0, 0, -3], 'scale': [0.75, 0.75, 0.75]},
                 'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
                 'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.5, 0.5, 0.5]},
                 'firstperson_righthand': {'rotation': [10, 45, 15], 'translation': [0, 3, 0], 'scale': [0.55, 0.55, 0.55]},
                 'firstperson_lefthand': {'rotation': [10, 225, -15], 'translation': [0, 3, 0], 'scale': [0.55, 0.55, 0.55]},
                 'head': {'rotation': [0, 0, 0], 'translation': [0, 13, 0], 'scale': [0.8, 0.8, 0.8]}}}
    GA.note_textures(model)
    GA.write(os.path.join(GA.A, 'models/block', bid + '.json'), model)
    variants = {f'facing={f}': {'model': f'{NS}:block/{bid}', **({'y': r} if r else {})}
                for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})


def _scroll_model(o):
    """A Lore Scroll in the hand: a rolled sheet on a wooden rod with knobbed ends, a length of written sheet hanging
    from it and a wax seal, its rod, knobs and seal in its origin's style."""
    t = f'{NS}:item/{o}_lore_scroll_3d'

    def box(frm, to, uv_by_face, rot=None):
        e = {'from': frm, 'to': to, 'faces': {f: {'texture': '#s', 'uv': uv} for f, uv in uv_by_face.items()}}
        if rot:
            e['rotation'] = rot
        return e
    roll = [0, 0, 16, 2]
    knob = [0, 2, 2, 4]
    sheet = [0, 4, 12, 16]
    seal = [12, 4, 14, 6]
    rolls = {f: roll for f in ('north', 'south', 'up', 'down')}
    rolls.update({'east': knob, 'west': knob})
    els = [
        box([1, 13, 7], [15, 15, 9], rolls),                                                  # the rolled top
        box([0, 12.5, 6.5], [1, 15.5, 9.5], {f: knob for f in ('north', 'south', 'east', 'west', 'up', 'down')}),
        box([15, 12.5, 6.5], [16, 15.5, 9.5], {f: knob for f in ('north', 'south', 'east', 'west', 'up', 'down')}),
        box([2, 1, 7.9], [14, 13, 8.1], {'north': sheet, 'south': sheet}),                     # the hanging sheet
        box([1.5, 0, 7], [14.5, 2, 9], rolls),                                                # the rolled bottom
        box([6.5, 3, 7.6], [9.5, 6, 7.9], {'north': seal, 'south': seal, 'east': seal, 'west': seal}),  # the wax seal
    ]
    return {'textures': {'s': t, 'particle': t}, 'gui_light': 'front', 'elements': els,
            'display': {
                'thirdperson_righthand': {'rotation': [0, 90, -35], 'translation': [0, 1.25, -1], 'scale': [0.7, 0.7, 0.7]},
                'thirdperson_lefthand': {'rotation': [0, -90, 35], 'translation': [0, 1.25, -1], 'scale': [0.7, 0.7, 0.7]},
                'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.6, 0.6, 0.6]},
                'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.6, 0.6, 0.6]},
                'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
                'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.75, 0.75, 0.75]},
                'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [0.8, 0.8, 0.8]}}}


def _select(GA, iid, held):
    GA.write(os.path.join(GA.A, 'items', iid + '.json'), {'model': {
        'type': 'minecraft:select', 'property': 'minecraft:display_context',
        'cases': [{'when': ['gui'], 'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{iid}'}}],
        'fallback': {'type': 'minecraft:model', 'model': held}}})


def _lore_entry(GD, lid, weight):
    o = next(l[1] for l in LORE if l[0] == lid)
    kind = next(l[2] for l in LORE if l[0] == lid)
    return GD.item(f'{o}_lore_{kind}', weight, extra=[{'type': 'minecraft:set_components', 'components': {f'{NS}:lore': lid}}])


def relic_entries(which):
    """Lore pieces for the Sift's buried-relic archaeology pools (gen_data.chest_loot)."""
    import gen_data as GD
    return [_lore_entry(GD, lid, w) for lid, w in RELIC_LORE[which]]


def item_models(GA):
    """The 3D in-hand models (a flat sprite in the GUI); runs after the standard item models are written."""
    for o in ORIGINS:
        _select(GA, f'{o}_lore_book', f'{NS}:block/{o}_lore_book')
        m = _scroll_model(o)
        GA.note_textures(m)
        GA.write(os.path.join(GA.A, 'models/item', f'{o}_lore_scroll_in_hand.json'), m)
        _select(GA, f'{o}_lore_scroll', f'{NS}:item/{o}_lore_scroll_in_hand')


def assets(GA):
    """Loot, recipe, sounds and every string (before the sounds are written)."""
    import gen_data as GD
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    for o in ORIGINS:
        # a placed book drops itself with its text
        GD.table('block', f'blocks/{o}_lore_book', [GD.pool([{
            'type': 'minecraft:item', 'name': f'{NS}:{o}_lore_book',
            'modifier': {'type': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'{NS}:lore']}}],
            condition={'type': 'minecraft:survives_explosion'})])
    # lore loot: one table per source, added to the chest by a loot modifier
    for table, (entries, chance) in LORE_LOOT.items():
        name = table.split(':')[0] + '_' + table.split('/')[-1]
        GD.table('chest', f'gameplay/lore/{name}', [GD.pool([_lore_entry(GD, lid, w) for lid, w in entries], condition=GD.chance(chance))])
        GA.write(os.path.join(GA.D, 'loot_modifiers', f'lore_{name}.json'), {
            'type': 'neoforge:add_table',
            'condition': {'type': 'neoforge:loot_table_id', 'loot_table_id': table},
            'table': f'{NS}:gameplay/lore/{name}'})
    # the Knowledge Book: an ordinary book, ink and a gold nugget - the Creator's white and gold
    GA.shapeless('knowledge_book', ['minecraft:book', 'minecraft:ink_sac', 'minecraft:gold_nugget'], 'knowledge_book', category='misc')
    lang(GA)


def lang(GA):
    L = {}
    for o in ORIGINS:
        book, scroll, who, _ = ORIGIN_TEXT[o]
        L[f'lore.{NS}.origin.{o}'] = who
    for lid, o, kind, title, body, found in LORE:
        L[f'lore.{NS}.{lid}.title'] = title
        L[f'lore.{NS}.{lid}.body'] = body
        L[f'lore.{NS}.{lid}.found'] = found
    L.update({
        f'lore.{NS}.blank': 'The pages are blank.',
        f'lore.{NS}.tooltip.read': 'Use to read',
        f'lore.{NS}.tooltip.place': 'Sneak and use on a block to set it down',
        f'lore.{NS}.tooltip.unread': 'Unread',
        f'lore.{NS}.deciphered': 'Ancient script deciphered: %s%%',
        f'lore.{NS}.decipher_up': 'The ancient script grows clearer (%s%%)',
        f'lore.{NS}.scroll_hint': 'Scroll to read on',
        f'item.{NS}.knowledge_book.desc': 'Records every creature, song, enchantment, story, place and recipe you discover.',
        f'knowledge.{NS}.title': 'Knowledge Book',
        f'knowledge.{NS}.contents': 'Contents',
        f'knowledge.{NS}.discovered': 'Discovered %s of %s',
        f'knowledge.{NS}.undiscovered': 'Undiscovered',
        f'knowledge.{NS}.unlocked': 'Knowledge Book: %s',
        f'knowledge.{NS}.hint.entity': 'Meet this creature in the wild to record it.',
        f'knowledge.{NS}.hint.item': 'Hold one to record it.',
        f'knowledge.{NS}.hint.biome': 'Set foot in this land to record it.',
        f'knowledge.{NS}.hint.structure': 'Find this place to record it.',
        f'knowledge.{NS}.hint.lore': 'Find this book or scroll in the world and read it.',
        f'knowledge.{NS}.hint.song': 'Find its Music Sheet, or play it, to record it.',
        f'knowledge.{NS}.hint.ench': 'Hold something bearing this enchantment to record it.',
        f'knowledge.{NS}.hint.quest': 'The Mini Creator has not told you about this yet.',
        f'knowledge.{NS}.hint.dim': 'Travel to the Sift to record it.',
        f'knowledge.{NS}.notes': 'Field Notes',
        f'knowledge.{NS}.voice': 'Band voice: %s',
        f'knowledge.{NS}.recipe': 'Recipe',
        f'knowledge.{NS}.recipe.unknown': 'You do not know a recipe for this yet.',
        f'knowledge.{NS}.recipe.smelt': 'Smelt',
        f'knowledge.{NS}.recipes.none': 'You have not learned any recipes of the Sift yet. Pick up their ingredients to learn them.',
        f'knowledge.{NS}.recipes.title': 'Known Recipes',
        f'knowledge.{NS}.song.instrument': 'Played on: %s',
        f'knowledge.{NS}.song.answers': 'Answered by: %s',
        f'knowledge.{NS}.song.band': 'Joins the band: %s',
        f'knowledge.{NS}.song.notes': 'Notes',
        f'knowledge.{NS}.song.played': 'You have played it',
        f'knowledge.{NS}.ench.max': 'Up to level %s',
        f'knowledge.{NS}.ench.fits': 'Fits:',
        f'knowledge.{NS}.ench.none_title': 'The Band Table',
        f'knowledge.{NS}.ench.none': 'No Sift enchantment recorded yet. They are played into gear at the Music Band Table: choose an '
                                     'item and an enchantment, then play its notes - well for the full enchantment, badly for a '
                                     'weaker one. Some need creatures from your band nearby.',
        f'knowledge.{NS}.lore.from': 'From %s',
        f'knowledge.{NS}.lore.found': 'Found in %s',
        f'knowledge.{NS}.lore.open': 'Click the page to read it whole',
        f'knowledge.{NS}.quest.current': 'Current goal',
        f'knowledge.{NS}.quest.done': 'Done',
        f'knowledge.{NS}.quest.goal': 'Goal: %s',
        f'knowledge.{NS}.scroll': 'Scroll for more',
        f'entity.{NS}.mini_creator': 'Mini Creator',
        f'entity.{NS}.mini_creator.says': '<%s> %s',
        f'band.{NS}.instrument.creator_bells': 'Creator Bells',
        f'codex.{NS}.mini_creator.title': 'Mini Creator',
        f'codex.{NS}.mini_creator.tagline': 'Your guide - a small piece of a big song',
        f'codex.{NS}.mini_creator.body': 'A small platypus in the Creator\'s white and gold, with four little blocks floating over his '
                                         'back. He says he is a piece of the Creator\'s song that slipped free when the Creator was '
                                         'taken. He appears when you first arrive in the Sift, gives you this book and tells you '
                                         'what to look for next; he pops back whenever you finish a goal. Talk to him (use him) to '
                                         'hear your current goal again. He cannot be hurt, and he plays bells in your band.',
    })
    for key, (name, intro) in CHAPTERS.items():
        L[f'knowledge.{NS}.chapter.{key}'] = name
        L[f'knowledge.{NS}.chapter.{key}.intro'] = intro
    for i, (qid, title, goal, line, done) in enumerate(QUESTS):
        L[f'quest.{NS}.{qid}.title'] = title
        L[f'quest.{NS}.{qid}.goal'] = goal
        L[f'quest.{NS}.{qid}.line'] = line
        if done:
            L[f'quest.{NS}.{qid}.done'] = done
    for k, v in FIELD_NOTES.items():
        L[f'codex.{NS}.{k}.notes'] = v
    for k, (title, tag, body) in PLACES.items():
        L[f'codex.{NS}.{k}.title'] = title
        L[f'codex.{NS}.{k}.tagline'] = tag
        L[f'codex.{NS}.{k}.body'] = body
    for s, t in SONG_TEXT.items():
        L[f'knowledge.{NS}.song.{s}'] = t
        L[f'knowledge.{NS}.song.{s}.creature'] = SONG_CREATURE[s]
    for s in ('sculk_castle', 'caravan_colony', 'thumper_arena'):
        L[f'structure.{NS}.{s}'] = {'sculk_castle': 'The Sculk Castle', 'caravan_colony': 'Caravan Colony', 'thumper_arena': 'The Drum Pit'}[s]
    GA.LANG.update(L)


# ============================================================================ recipe advancements


def recipe_advancements(GA):
    """Unlock advancements for every Sift recipe (vanilla style: holding any ingredient teaches it), so the recipe book -
    and the Knowledge Book's recipe pages - learn the Sift's recipes as you play. Runs last, when every recipe is written."""
    rdir = os.path.join(GA.D, 'recipe')
    if not os.path.isdir(rdir):
        return
    written = {os.path.normpath(p) for p in GA.WRITTEN}
    for f in sorted(os.listdir(rdir)):
        rel = os.path.normpath(os.path.join('data', NS, 'recipe', f))
        if not f.endswith('.json') or rel not in written:
            continue
        name = f[:-5]
        with open(os.path.join(rdir, f)) as fh:
            r = json.load(fh)
        ings = []

        def add(v):
            if isinstance(v, list):
                for x in v:
                    add(x)
            elif isinstance(v, str) and v not in ings:
                ings.append(v)
            elif isinstance(v, dict):
                for key in ('item', 'tag'):
                    if key in v:
                        add(v[key] if key == 'item' else '#' + v[key])
        for key in ('ingredient', 'base', 'addition', 'template'):
            if key in r:
                add(r[key])
        if 'key' in r:
            for v in r['key'].values():
                add(v)
        if 'ingredients' in r:
            add(r['ingredients'])
        ings = ings[:6]
        criteria = {'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipes': f'{NS}:{name}'}}}
        for i, ing in enumerate(ings):
            criteria[f'has_{i}'] = {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': ing}]}}
        GA.write(os.path.join(GA.D, 'advancement', 'recipes', name + '.json'), {
            'parent': 'minecraft:recipes/root', 'criteria': criteria, 'requirements': [list(criteria)],
            'rewards': {'recipes': [f'{NS}:{name}']}})


# ============================================================================ art (tools/knowledge_art.py)


def textures(out):
    __import__('knowledge_art').textures(out)

