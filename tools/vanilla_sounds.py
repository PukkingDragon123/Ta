"""The vanilla 26.3 sounds that The Sift's sounds.json is allowed to point at.

gen_assets.check_sounds() refuses to write a sounds.json entry whose vanilla name is not listed
here: a sound file that does not exist (vanilla renames and renumbers its .ogg files between
versions) plays as silence, and a misspelled event plays nothing at all - neither shows up as an
error in game. Both lists come from the real 26.3 assets/minecraft/sounds.json.

To use a new vanilla sound, add it to the SOUNDS table in gen_assets.py and rebuild this file from
the vanilla assets (every name the table uses is checked against the real sounds.json first):

    VANILLA_ASSETS=/path/to/assets/minecraft python3 tools/vanilla_sounds.py
"""
import json
import os
import sys

# Vanilla sound events used with 'event:...' (and the note block events 'block/note_block/x' becomes).
EVENTS = frozenset({
    'block.anvil.land', 'block.beacon.activate', 'block.note_block.basedrum', 'block.note_block.bass', 'block.note_block.bell',
    'block.note_block.bit', 'block.note_block.chime', 'block.note_block.didgeridoo', 'block.note_block.flute', 'block.note_block.guitar',
    'block.note_block.harp', 'block.note_block.hat', 'block.note_block.snare', 'block.note_block.trumpet',
    'block.note_block.trumpet_exposed', 'block.note_block.trumpet_weathered', 'block.portal.ambient', 'block.portal.travel',
    'block.portal.trigger', 'block.sand.break', 'block.sculk_catalyst.bloom', 'block.tripwire.click_on', 'entity.chicken.ambient',
    'entity.chicken.death', 'entity.chicken.hurt', 'entity.enderman.teleport', 'entity.evoker.prepare_summon',
    'entity.firework_rocket.twinkle', 'entity.generic.drink', 'entity.generic.explode', 'entity.guardian.attack', 'entity.guardian.flop',
    'entity.guardian.hurt', 'entity.llama.spit', 'entity.parrot.ambient', 'entity.parrot.fly', 'entity.parrot.imitate.phantom',
    'entity.phantom.ambient', 'entity.phantom.death', 'entity.phantom.hurt', 'entity.phantom.swoop', 'entity.player.attack.sweep',
    'entity.player.burp', 'entity.ravager.ambient', 'entity.ravager.death', 'entity.ravager.roar', 'entity.ravager.stunned',
    'entity.sniffer.digging', 'entity.sniffer.happy', 'entity.sniffer.sniffing', 'entity.spider.ambient', 'entity.spider.death',
    'entity.spider.hurt', 'entity.spider.step', 'entity.turtle.ambient_land', 'entity.turtle.death', 'entity.turtle.hurt',
    'entity.turtle.shamble', 'entity.warden.ambient', 'entity.warden.angry', 'entity.warden.attack_impact', 'entity.warden.death',
    'entity.warden.emerge', 'entity.warden.heartbeat', 'entity.warden.hurt', 'entity.warden.listening', 'entity.warden.roar',
    'entity.warden.sniff', 'entity.warden.sonic_boom', 'entity.warden.sonic_charge', 'entity.wither.spawn', 'item.crossbow.loading_middle',
    'item.crossbow.shoot', 'item.elytra.flying', 'item.shield.block', 'item.trident.riptide_1',
    'block.portal.trigger', 'block.sand.break', 'block.sculk_catalyst.bloom', 'block.tripwire.click_on', 'entity.enderman.teleport',
    'entity.evoker.prepare_summon', 'entity.firework_rocket.twinkle', 'entity.generic.explode', 'entity.llama.spit',
    'entity.player.attack.sweep', 'entity.ravager.ambient', 'entity.ravager.death', 'entity.ravager.roar', 'entity.ravager.stunned',
    'entity.spider.ambient', 'entity.spider.death', 'entity.spider.hurt', 'entity.spider.step', 'entity.turtle.ambient_land',
    'entity.turtle.death', 'entity.turtle.hurt', 'entity.warden.ambient', 'entity.warden.attack_impact', 'entity.warden.death',
    'entity.warden.emerge', 'entity.warden.heartbeat', 'entity.warden.hurt', 'entity.warden.listening', 'entity.warden.roar',
    'entity.warden.sonic_boom', 'entity.warden.sonic_charge', 'entity.wither.spawn', 'item.crossbow.loading_middle', 'item.crossbow.shoot',
    'item.shield.block',
    # A1 Bulb & Stomper: flower swap, Sculk Bloom, garden shake
    'entity.sniffer.eat', 'entity.goat.eat', 'block.sweet_berry_bush.pick_berries', 'entity.mooshroom.shear', 'block.sculk.charge',
    'entity.wolf.shake', 'block.azalea_leaves.break',
})

# A4 cave creatures: the Jailer and Sculklings
EVENTS = EVENTS | frozenset({
    'block.anvil.destroy', 'block.chain.break', 'block.chain.hit', 'block.chain.place', 'block.chain.step', 'block.iron.hit',
    'block.iron_door.close', 'block.sculk_sensor.clicking', 'block.sculk_shrieker.shriek', 'block.vault.close_shutter',
    'entity.allay.ambient_without_item', 'entity.allay.item_taken', 'entity.bat.ambient', 'entity.bat.death', 'entity.bat.hurt',
    'entity.fox.screech', 'entity.iron_golem.damage', 'entity.vex.ambient', 'entity.vex.death', 'entity.vex.hurt',
    'entity.warden.dig', 'entity.warden.listening_angry', 'entity.warden.step', 'entity.warden.tendril_clicks',
    'entity.witch.celebrate', 'entity.zombie.attack_iron_door', 'item.mace.smash_ground_heavy',
})

# CR1: the Sifter's bell, the Echoer's wings, drill and pings
EVENTS = EVENTS | frozenset({
    'block.amethyst_block.chime', 'block.amethyst_block.resonate', 'block.bell.resonate', 'block.bell.use', 'block.copper.step',
    'block.grindstone.use', 'entity.bat.takeoff', 'entity.breeze.whirl', 'entity.phantom.flap',
})

# Vanilla sound files (assets/minecraft/sounds/<name>.ogg) used directly.
FILES = frozenset({
    'random/fizz', 'random/fuse',  # F1: the Europhy Table fizzling, Bauxite's hiss
    # F3 Knowledge and lore: whispers, pages, the Mini Creator
    'ambient/nether/soulsand_valley/voices1', 'ambient/nether/soulsand_valley/voices3', 'ambient/nether/soulsand_valley/voices5',
    'block/chiseled_bookshelf/pickup1', 'block/chiseled_bookshelf/pickup2', 'block/end_portal/eyeplace1', 'block/end_portal/eyeplace2',
    'block/end_portal/eyeplace3', 'block/sculk/spread3', 'item/book/open_flip1', 'item/book/open_flip2', 'mob/armadillo/ambient1',
    'mob/armadillo/ambient2', 'mob/frog/idle5', 'mob/frog/step1', 'mob/frog/step2', 'mob/frog/step3',
    'ambient/cave/cave11', 'ambient/cave/cave13', 'ambient/cave/cave7', 'ambient/cave/cave9', 'ambient/nether/soulsand_valley/wind1',
    'ambient/underwater/additions/animal1', 'ambient/underwater/additions/bass_whale1', 'ambient/underwater/additions/bass_whale2',
    'ambient/underwater/additions/bubbles1', 'ambient/underwater/additions/bubbles2', 'ambient/underwater/additions/bubbles3',
    'ambient/underwater/additions/dark1', 'ambient/underwater/additions/dark2', 'ambient/underwater/additions/dark3',
    'ambient/underwater/additions/earth_crack', 'ambient/underwater/underwater_ambience', 'block/amethyst/break1',
    'block/amethyst/resonate1', 'block/amethyst/resonate2', 'block/amethyst/resonate3', 'block/amethyst/shimmer',
    'block/amethyst_cluster/break1', 'block/amethyst_cluster/break2', 'block/beacon/activate', 'block/beacon/ambient',
    'block/beacon/deactivate', 'block/beacon/power1', 'block/bell/resonate', 'block/bubble_column/bubble1', 'block/bubble_column/bubble2',
    'block/bubble_column/bubble3', 'block/enchantment_table/enchant1', 'block/end_portal/endportal', 'block/pointed_dripstone/drip_lava1',
    'block/sculk/break1', 'block/sculk/break2', 'block/sculk/break3', 'block/sculk/charge2', 'block/sculk/spread1', 'block/sculk/step2',
    'block/sculk_catalyst/break3', 'block/sculk_sensor/sculk_clicking1', 'block/sculk_sensor/sculk_clicking2',
    'block/sculk_sensor/sculk_clicking3', 'block/sculk_sensor/sculk_clicking5', 'block/sculk_shrieker/shriek1',
    'block/sculk_shrieker/shriek2', 'block/trial_spawner/ominous_activate', 'dig/gravel1', 'dig/stone1', 'item/crossbow/loading_start',
    'item/elytra/elytra_loop', 'item/goat_horn/call0', 'item/goat_horn/call1', 'item/goat_horn/call2', 'item/goat_horn/call3',
    'item/goat_horn/call4', 'item/goat_horn/call5', 'item/goat_horn/call6', 'item/goat_horn/call7', 'liquid/splash', 'liquid/splash2',
    'liquid/swim3', 'liquid/water', 'mob/allay/death1', 'mob/allay/hurt1', 'mob/allay/idle_with_item1', 'mob/allay/idle_with_item2',
    'mob/allay/idle_without_item1', 'mob/allay/idle_without_item2', 'mob/allay/idle_without_item3', 'mob/allay/item_given1',
    'mob/allay/item_given2', 'mob/allay/item_taken1', 'mob/axolotl/idle_air1', 'mob/bat/death', 'mob/bat/hurt1', 'mob/bat/hurt2',
    'mob/bat/idle1', 'mob/bat/idle2', 'mob/camel/step1', 'mob/cow/hurt1', 'mob/cow/hurt3', 'mob/cow/say1', 'mob/cow/say2', 'mob/cow/say3',
    'mob/dolphin/blowhole1', 'mob/dolphin/blowhole2', 'mob/fox/bite1', 'mob/fox/bite2', 'mob/frog/death1', 'mob/frog/death2',
    'mob/frog/hurt1', 'mob/frog/hurt2', 'mob/frog/idle1', 'mob/frog/idle2', 'mob/frog/idle4', 'mob/frog/idle6', 'mob/frog/idle7',
    'mob/frog/tongue1', 'mob/frog/tongue2', 'mob/guardian/flop1', 'mob/guardian/flop2', 'mob/guardian/guardian_death',
    'mob/guardian/guardian_hit1', 'mob/guardian/guardian_hit2', 'mob/guardian/guardian_idle1', 'mob/guardian/guardian_idle3',
    'mob/happy_ghast/ambient3', 'mob/happy_ghast/death', 'mob/happy_ghast/hurt1', 'mob/parrot/death1', 'mob/parrot/death2',
    'mob/parrot/hurt1', 'mob/parrot/hurt2', 'mob/parrot/idle1', 'mob/parrot/idle2', 'mob/parrot/idle3', 'mob/phantom/flap1',
    'mob/phantom/flap2', 'mob/pufferfish/blow_out1', 'mob/pufferfish/blow_out2', 'mob/pufferfish/blow_up1', 'mob/pufferfish/blow_up2',
    'mob/pufferfish/death1', 'mob/pufferfish/death2', 'mob/pufferfish/flop1', 'mob/pufferfish/flop2', 'mob/pufferfish/flop3',
    'mob/pufferfish/flop4', 'mob/pufferfish/hurt1', 'mob/pufferfish/hurt2', 'mob/rabbit/hurt1', 'mob/rabbit/hurt2', 'mob/rabbit/hurt3',
    'mob/rabbit/idle1', 'mob/rabbit/idle2', 'mob/ravager/death1', 'mob/ravager/hurt1', 'mob/ravager/hurt3', 'mob/ravager/step1',
    'mob/ravager/step2', 'mob/ravager/step4', 'mob/ravager/stun1', 'mob/silverfish/hit1', 'mob/silverfish/hit2', 'mob/silverfish/kill',
    'mob/silverfish/say1', 'mob/silverfish/say2', 'mob/silverfish/say3', 'mob/silverfish/step1', 'mob/silverfish/step2',
    'mob/silverfish/step3', 'mob/slime/attack1', 'mob/slime/big1', 'mob/slime/big2', 'mob/slime/small1', 'mob/slime/small2',
    'mob/slime/small3', 'mob/slime/small4', 'mob/slime/small5', 'mob/sniffer/happy1', 'mob/sniffer/happy2', 'mob/sniffer/happy3',
    'mob/sniffer/idle3', 'mob/sniffer/step1', 'mob/sniffer/step2', 'mob/squid/ambient1', 'mob/squid/death1', 'mob/squid/hurt1',
    'mob/turtle/baby/egg_hatched1', 'mob/turtle/egg/drop_egg1', 'mob/turtle/egg/drop_egg2', 'mob/turtle/egg/egg_break1',
    'mob/turtle/egg/egg_crack1', 'mob/warden/heartbeat_1', 'mob/warden/listening_angry_2', 'mob/warden/nearby_close_1',
    'mob/warden/tendril_clicks_2', 'mob/warden/tendril_clicks_4', 'music/game/an_ordinary_day', 'music/game/ancestry',
    'music/game/comforting_memories', 'music/game/deeper', 'music/game/echo_in_the_wind', 'music/game/eld_unknown',
    'music/game/featherfall', 'music/game/floating_dream', 'music/game/infinite_amethyst', 'music/game/komorebi',
    'music/game/left_to_bloom', 'music/game/one_more_day', 'music/game/water/axolotl', 'music/game/water/dragon_fish',
    'music/game/water/shuniji', 'random/bow', 'random/drink', 'random/eat1', 'random/eat2', 'random/explode1', 'random/explode2',
    'ambient/cave/cave11', 'ambient/cave/cave13', 'ambient/cave/cave7', 'ambient/cave/cave9', 'block/amethyst/break1',
    'block/amethyst/resonate1', 'block/amethyst/resonate2', 'block/amethyst/resonate3', 'block/amethyst/resonate4',
    'block/amethyst/shimmer', 'block/amethyst_cluster/break1', 'block/amethyst_cluster/break2', 'block/amethyst_cluster/break3',
    'block/basalt/break1', 'block/basalt/break2', 'block/basalt/break3', 'block/basalt/step1', 'block/basalt/step3',
    'block/beacon/activate', 'block/beacon/ambient', 'block/beacon/deactivate', 'block/beacon/power1', 'block/beacon/power2',
    'block/bell/resonate', 'block/bubble_column/bubble1', 'block/bubble_column/bubble2', 'block/bubble_column/bubble3',
    'block/enchantment_table/enchant1', 'block/end_portal/endportal', 'block/pointed_dripstone/drip_lava1', 'block/sculk/break1',
    'block/sculk/break2', 'block/sculk/break3', 'block/sculk/charge2', 'block/sculk/spread1', 'block/sculk/step2',
    'block/sculk_catalyst/break3', 'block/sculk_sensor/sculk_clicking1', 'block/sculk_sensor/sculk_clicking2',
    'block/sculk_sensor/sculk_clicking3', 'block/sculk_sensor/sculk_clicking5', 'block/sculk_shrieker/shriek1',
    'block/sculk_shrieker/shriek2', 'block/trial_spawner/ominous_activate', 'dig/stone1', 'item/brush/brushing_sand1',
    'item/brush/brushing_sand2', 'item/crossbow/loading_start', 'item/goat_horn/call0', 'item/goat_horn/call1', 'item/goat_horn/call2',
    'item/goat_horn/call3', 'item/goat_horn/call4', 'item/goat_horn/call5', 'item/goat_horn/call6', 'item/goat_horn/call7', 'liquid/splash',
    'liquid/splash2', 'liquid/swim3', 'liquid/water', 'mob/allay/death1', 'mob/allay/hurt1', 'mob/allay/idle_with_item1',
    'mob/allay/idle_with_item2', 'mob/allay/idle_without_item1', 'mob/allay/idle_without_item2', 'mob/allay/idle_without_item3',
    'mob/allay/item_given1', 'mob/allay/item_given2', 'mob/allay/item_taken1', 'mob/allay/item_thrown1', 'mob/axolotl/idle_air1',
    'mob/bat/death', 'mob/bat/hurt1', 'mob/bat/hurt2', 'mob/bat/idle1', 'mob/bat/idle2', 'mob/camel/step1', 'mob/cow/hurt1',
    'mob/cow/hurt3', 'mob/cow/say1', 'mob/cow/say2', 'mob/cow/say3', 'mob/dolphin/blowhole1', 'mob/dolphin/blowhole2', 'mob/fox/bite1',
    'mob/fox/bite2', 'mob/frog/death1', 'mob/frog/death2', 'mob/frog/hurt1', 'mob/frog/hurt2', 'mob/frog/idle1', 'mob/frog/idle2',
    'mob/frog/idle4', 'mob/frog/idle6', 'mob/frog/idle7', 'mob/frog/tongue1', 'mob/frog/tongue2', 'mob/guardian/flop1',
    'mob/guardian/flop2', 'mob/guardian/guardian_death', 'mob/guardian/guardian_hit1', 'mob/guardian/guardian_hit2',
    'mob/guardian/guardian_idle1', 'mob/guardian/guardian_idle3', 'mob/happy_ghast/ambient3', 'mob/happy_ghast/death',
    'mob/happy_ghast/hurt1', 'mob/parrot/death1', 'mob/parrot/death2', 'mob/parrot/hurt1', 'mob/parrot/hurt2', 'mob/parrot/idle1',
    'mob/parrot/idle2', 'mob/parrot/idle3', 'mob/phantom/flap1', 'mob/phantom/flap2', 'mob/pufferfish/blow_out1',
    'mob/pufferfish/blow_out2', 'mob/pufferfish/blow_up1', 'mob/pufferfish/blow_up2', 'mob/pufferfish/death1', 'mob/pufferfish/death2',
    'mob/pufferfish/flop1', 'mob/pufferfish/flop2', 'mob/pufferfish/flop3', 'mob/pufferfish/flop4', 'mob/pufferfish/hurt1',
    'mob/pufferfish/hurt2', 'mob/rabbit/hurt1', 'mob/rabbit/hurt2', 'mob/rabbit/hurt3', 'mob/rabbit/idle1', 'mob/rabbit/idle2',
    'mob/ravager/death1', 'mob/ravager/hurt1', 'mob/ravager/hurt3', 'mob/ravager/step1', 'mob/ravager/step2', 'mob/ravager/step4',
    'mob/ravager/stun1', 'mob/silverfish/hit1', 'mob/silverfish/hit2', 'mob/silverfish/kill', 'mob/silverfish/say1', 'mob/silverfish/say2',
    'mob/silverfish/say3', 'mob/silverfish/step1', 'mob/silverfish/step2', 'mob/silverfish/step3', 'mob/slime/attack1', 'mob/slime/big1',
    'mob/slime/big2', 'mob/slime/small1', 'mob/slime/small2', 'mob/slime/small3', 'mob/slime/small4', 'mob/slime/small5',
    'mob/sniffer/happy1', 'mob/sniffer/happy2', 'mob/sniffer/happy3', 'mob/sniffer/idle3', 'mob/sniffer/step1', 'mob/sniffer/step2',
    'mob/squid/ambient1', 'mob/squid/death1', 'mob/squid/hurt1', 'mob/turtle/baby/egg_hatched1', 'mob/turtle/egg/drop_egg1',
    'mob/turtle/egg/drop_egg2', 'mob/turtle/egg/egg_break1', 'mob/turtle/egg/egg_crack1', 'mob/warden/listening_angry_2',
    'mob/warden/sonic_boom1', 'mob/warden/sonic_boom2', 'mob/warden/sonic_charge1', 'mob/warden/sonic_charge2',
    'mob/warden/tendril_clicks_2', 'mob/warden/tendril_clicks_4', 'music/game/an_ordinary_day', 'music/game/ancestry',
    'music/game/comforting_memories', 'music/game/echo_in_the_wind', 'music/game/infinite_amethyst', 'music/game/left_to_bloom',
    'music/game/one_more_day', 'random/bow', 'random/drink', 'random/eat1', 'random/eat2', 'random/explode1', 'random/explode2',
    'random/levelup', 'random/pop',
    # A2 Swifter & White Forest
    'block/cherry_leaves/break1', 'block/cherry_leaves/break2', 'block/cherry_leaves/break3', 'entity/wind_charge/wind_burst1',
    'entity/wind_charge/wind_burst2', 'fireworks/launch1', 'item/mace/smash_ground_heavy', 'mob/breeze/whirl', 'mob/cat/purr1',
    'mob/cat/purr2', 'mob/fox/aggro1', 'mob/fox/aggro2', 'mob/fox/aggro3', 'mob/fox/bite3', 'mob/fox/death1', 'mob/fox/death2',
    'mob/fox/eat1', 'mob/fox/eat2', 'mob/fox/hurt1', 'mob/fox/hurt2', 'mob/fox/idle1', 'mob/fox/idle2', 'mob/fox/idle3',
    'mob/fox/idle4', 'mob/fox/screech1', 'mob/fox/sleep1', 'mob/fox/sleep2', 'mob/fox/sleep3', 'mob/fox/sniff1', 'mob/phantom/swoop1',
    'mob/phantom/swoop2', 'mob/wolf/baby/whine1', 'mob/wolf/baby/whine2',
})

# W1 World & terrain: the Sculk Swamp's music and ambience
FILES = FILES | frozenset({
    'ambient/nether/soulsand_valley/ambience', 'ambient/nether/soulsand_valley/mood1', 'ambient/nether/soulsand_valley/whisper2',
    'block/pointed_dripstone/drip_water1', 'block/pointed_dripstone/drip_water3', 'music/game/swamp/aerie', 'music/game/swamp/firebugs',
    'music/game/swamp/labyrinthine',
})

# CR3 Fish & Coral Organs: the Sculk Fish, the Sculk Coral Organ and its hooked line
FILES = FILES | frozenset({
    'ambient/underwater/additions/dark1', 'ambient/underwater/additions/dark3', 'block/chain/step2', 'block/chain/step4', 'block/conduit/ambient',
    'block/sculk/break1', 'block/sculk_catalyst/break1', 'block/sculk_catalyst/break3', 'block/sculk_sensor/sculk_clicking2',
    'block/sculk_sensor/sculk_clicking5', 'block/sculk_shrieker/break2', 'block/sculk_shrieker/shriek2', 'block/sculk_shrieker/shriek4',
    'dig/coral1', 'dig/coral3', 'entity/bobber/retrieve1', 'entity/bobber/retrieve2', 'entity/fish/flop1', 'entity/fish/flop3',
    'entity/fish/hurt1', 'entity/fish/hurt2', 'entity/fish/hurt3', 'entity/leashknot/break', 'item/trident/pierce1', 'item/trident/pierce2',
    'item/trident/throw1', 'item/trident/throw2', 'mob/evocation_illager/fangs', 'mob/warden/tendril_clicks_1', 'random/break',
})

# E1 Sniffer & rot: the Sift Sniffer, its egg and the rot's groan
FILES = FILES | frozenset({
    'mob/chicken/plop',
    'mob/goat/impact1',
    'mob/goat/impact2',
    'mob/goat/pre_ram1',
    'mob/goat/pre_ram2',
    'mob/goat/pre_ram3',
    'mob/goat/pre_ram4',
    'mob/sniffer/death1',
    'mob/sniffer/death2',
    'mob/sniffer/eat1',
    'mob/sniffer/eat2',
    'mob/sniffer/eat3',
    'mob/sniffer/happy4',
    'mob/sniffer/happy5',
    'mob/sniffer/hurt1',
    'mob/sniffer/hurt2',
    'mob/sniffer/hurt3',
    'mob/sniffer/idle1',
    'mob/sniffer/idle2',
    'mob/sniffer/idle4',
    'mob/sniffer/idle5',
    'mob/sniffer/idle6',
    'mob/sniffer/longdig1',
    'mob/sniffer/longdig2',
    'mob/sniffer/scenting1',
    'mob/sniffer/scenting2',
    'mob/sniffer/scenting3',
    'mob/sniffer/sniffing1',
    'mob/sniffer/sniffing2',
    'mob/sniffer/sniffing3',
    'mob/sniffer/step3',
    'mob/sniffer/step4',
    'mob/sniffer/step5',
    'mob/sniffer/step6',
    'mob/turtle/egg/egg_break2',
    'mob/turtle/egg/egg_crack2',
    'mob/turtle/egg/egg_crack3',
    'mob/turtle/egg/egg_crack4',
    'mob/turtle/egg/egg_crack5',
    'mob/zombie/say1',
    'mob/zombie/say2',
    'mob/zombie/say3',
})


# CR2: the Caravan Queen, Caravan larvae and egg-laden ore; the Slumbler's spit, eggs and tadpoles
FILES = FILES | frozenset({
    'block/amethyst/break2', 'block/amethyst/step5', 'block/amethyst_cluster/break4', 'block/amethyst_cluster/place1',
    'block/amethyst_cluster/place2', 'block/bell/bell_use01', 'block/frogspawn/hatch1', 'block/frogspawn/hatch2', 'block/frogspawn/hatch3',
    'entity/fish/flop2', 'entity/fish/flop4', 'entity/player/attack/sweep1', 'entity/player/attack/sweep3', 'mob/axolotl/attack1',
    'mob/axolotl/attack2', 'mob/dolphin/splash1', 'mob/dolphin/splash2', 'mob/dolphin/splash3', 'mob/frog/eat1', 'mob/frog/eat2',
    'mob/frog/eat3', 'mob/frog/lay_spawn1', 'mob/frog/lay_spawn2', 'mob/llama/spit1', 'mob/llama/spit2', 'mob/silverfish/hit3',
    'mob/silverfish/say4', 'mob/silverfish/step4', 'mob/sniffer/eat1', 'mob/sniffer/eat2', 'mob/sniffer/longdig1', 'mob/sniffer/longdig2',
    'mob/tadpole/death1', 'mob/tadpole/death2', 'mob/tadpole/hurt1', 'mob/tadpole/hurt2', 'mob/tadpole/hurt3', 'mob/wolf/shake',
    'random/glass1', 'random/glass2', 'random/glass3',
})

# W-sea: Trumpet Coral's four voices, the Brass Coral Reef's and the Chrome Coral Ocean's music and ambience
EVENTS = EVENTS | frozenset({'block.note_block.trumpet', 'block.note_block.trumpet_exposed', 'block.note_block.trumpet_weathered',
                             'block.note_block.trumpet_oxidized'})
FILES = FILES | frozenset({
    'ambient/underwater/additions/animal2', 'ambient/underwater/additions/bubbles4', 'ambient/underwater/additions/bubbles5',
    'ambient/underwater/additions/bubbles6', 'ambient/underwater/additions/crackles1', 'ambient/underwater/additions/crackles2',
    'ambient/underwater/additions/driplets1', 'ambient/underwater/additions/driplets2', 'ambient/underwater/underwater_ambience',
    'block/amethyst/resonate1', 'block/amethyst/shimmer', 'block/bubble_column/bubble2', 'music/game/water/axolotl',
    'music/game/water/dragon_fish', 'music/game/water/shuniji', 'music/game/left_to_bloom', 'music/game/floating_dream',
    'music/game/komorebi', 'music/game/infinite_amethyst',
})

# P4 Cave Jungle: the jungle creatures' and the Giant Pitcher's voices
EVENTS = EVENTS | frozenset({
    'entity.bee.loop', 'entity.bee.hurt', 'entity.bee.pollinate', 'entity.allay.hurt', 'entity.allay.death', 'entity.firework_rocket.blast',
    'block.beacon.power_select', 'entity.parrot.hurt', 'entity.parrot.death', 'entity.parrot.eat', 'entity.chicken.step',
    'entity.evoker_fangs.attack', 'entity.fox.bite', 'entity.silverfish.ambient', 'entity.silverfish.hurt', 'entity.silverfish.death',
    'block.amethyst_cluster.break', 'entity.player.attack.strong', 'entity.generic.eat', 'entity.frog.ambient', 'entity.frog.hurt',
    'entity.frog.death', 'entity.frog.lay_spawn', 'item.mace.smash_ground', 'entity.ravager.step', 'block.slime_block.place',
    'entity.tadpole.flop', 'entity.tadpole.hurt', 'entity.tadpole.death', 'block.wood.break', 'block.rooted_dirt.break',
    'entity.hoglin.ambient', 'entity.hoglin.hurt', 'entity.hoglin.death', 'entity.hoglin.attack', 'entity.hoglin.step', 'block.calcite.break',
    'block.stone.break', 'block.tuff.break', 'block.big_dripleaf.tilt_down', 'block.bubble_column.bubble_pop',
})


def vanilla_names(sounds_json):
    """(events, files) defined by a vanilla sounds.json."""
    with open(sounds_json) as f:
        data = json.load(f)
    files = set()
    for event in data.values():
        for s in event.get('sounds', []):
            if isinstance(s, str):
                files.add(s)
            elif s.get('type', 'file') != 'event':
                files.add(s['name'])
    return set(data), files


def _block(names):
    lines, line = [], ''
    for n in sorted(names):
        item = repr(n) + ','
        if line and len(line) + 1 + len(item) > 136:
            lines.append(line)
            line = item
        else:
            line = (line + ' ' + item) if line else item
    if line:
        lines.append(line)
    return '\n    '.join(lines)


def _replace_set(src, name, names):
    start = src.index(name + ' = frozenset({\n') + len(name + ' = frozenset({\n')
    end = src.index('})\n', start)
    body = '    ' + _block(names) + '\n' if names else ''
    return src[:start] + body + src[end:]


def main():
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import gen_assets  # noqa: E402
    events, files = gen_assets.used_vanilla_sounds()
    known_events, known_files = vanilla_names(os.path.join(gen_assets.VA, 'sounds.json'))
    bad = sorted(f'event {e}' for e in events - known_events) + sorted(f'file {f}' for f in files - known_files)
    if bad:
        sys.exit('not in the vanilla sounds.json: ' + ', '.join(bad))
    path = os.path.abspath(__file__)
    with open(path) as f:
        src = f.read()
    src = _replace_set(src, 'EVENTS', events)
    src = _replace_set(src, 'FILES', files)
    with open(path, 'w') as f:
        f.write(src)
    print(f'vanilla_sounds.py: {len(events)} events, {len(files)} files')


if __name__ == '__main__':
    main()
