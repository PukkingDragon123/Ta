"""CR3 Fish & Coral Organs: the remade music fish (their art is in tools/fish_art.py), the new Sculk Fish and the
Sculk Coral Organ - spec (spawn eggs, plain water buckets for the music fish), sounds, text, loot, tags, spawns,
worldgen, and the item art: every fish's raw and cooked meat, every sushi and the platter, the fish buckets
(water and Chrome), the new spawn eggs and the organ's barb.

Hooked in from one line each: spec.py (declare), gen_assets.py (assets; its sound check reads ModSculkSea.java),
gen_world.py (world), gen_textures.py (textures), items16.py (item_sprites), chrome.py (bucket_fish); the fish
and organ models come from tools/fish_art.py through mobs_wild.py.
Java: registry/ModSculkSea, entity/SculkFish, entity/CoralOrgan, entity/CoralHook, worldgen/CoralOrganFeature,
client/SculkSeaClient and its models and renderers; SiftFish keeps the colour variants and buckets.
"""
from __future__ import annotations

import json
import math
import os

from PIL import Image

NS = 'thesift'
MUSIC_FISH = ('kazoo_fish', 'tubafish', 'fanfare_eel')
NAMES = {'kazoo_fish': 'Kazoo Fish', 'tubafish': 'Tubafish', 'fanfare_eel': 'Fanfare Eel'}


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# ============================================================================ spec (registries)

def declare(block, item):
    for f in MUSIC_FISH:
        # exactly like vanilla's fish buckets: the fish (name, health, colour variant...) kept in BUCKET_ENTITY_DATA
        item(f'{f}_bucket', cls='MobBucketItem',
             factory=f'p -> new MobBucketItem(ModEntities.{f.upper()}.get(), net.minecraft.world.level.material.Fluids.WATER, '
                     'net.minecraft.sounds.SoundEvents.BUCKET_EMPTY_FISH, p)',
             props='new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA, '
                   'net.minecraft.world.item.component.CustomData.EMPTY)',
             name=f'Bucket of {NAMES[f]}', tab='tools')
    item('sculk_fish_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModSculkSea.SCULK_FISH.get())', tab='eggs')
    item('coral_organ_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModSculkSea.CORAL_ORGAN.get())', tab='eggs',
         name='Sculk Coral Organ Spawn Egg')


# ============================================================================ sounds

SOUNDS = {
    'entity.sculk_fish.ambient': [('block/sculk_sensor/sculk_clicking2', 0.35, 1.6), ('mob/warden/tendril_clicks_1', 0.3, 1.8),
                                  ('block/sculk_sensor/sculk_clicking5', 0.35, 1.5)],
    'entity.sculk_fish.bite': [('mob/evocation_illager/fangs', 0.6, 1.7), ('mob/evocation_illager/fangs', 0.6, 1.95)],
    'entity.sculk_fish.hurt': [('entity/fish/hurt1', 0.8, 0.8), ('entity/fish/hurt3', 0.8, 0.75)],
    'entity.sculk_fish.death': [('block/sculk/break1', 0.8, 1.3), ('entity/fish/hurt2', 0.8, 0.6)],
    'entity.sculk_fish.flop': [('entity/fish/flop1', 0.7, 0.8), ('entity/fish/flop3', 0.7, 0.8)],
    # the organ: a deep flute pipe and a reedy drone for every note of its chords
    'entity.coral_organ.pipe': [('block/note_block/flute', 1.0, 0.5)],
    'entity.coral_organ.reed': [('block/note_block/didgeridoo', 0.7, 1.0)],
    'entity.coral_organ.drone': [('block/conduit/ambient', 0.7, 0.55), ('ambient/underwater/additions/dark1', 0.5, 0.7),
                                 ('ambient/underwater/additions/dark3', 0.5, 0.6)],
    'entity.coral_organ.charge': [('block/sculk_shrieker/shriek2', 0.6, 1.25), ('block/sculk_shrieker/shriek4', 0.6, 1.2)],
    'entity.coral_organ.fire': [('item/trident/throw1', 1.0, 0.7), ('item/trident/throw2', 1.0, 0.65)],
    'entity.coral_organ.reel': [('entity/bobber/retrieve1', 0.9, 0.6), ('entity/bobber/retrieve2', 0.9, 0.55)],
    'entity.coral_organ.clamp': [('mob/evocation_illager/fangs', 1.0, 0.55)],
    'entity.coral_organ.hurt': [('dig/coral1', 1.0, 0.8), ('dig/coral3', 1.0, 0.75), ('block/sculk_catalyst/break3', 1.0, 0.9)],
    'entity.coral_organ.death': [('block/sculk_catalyst/break1', 1.2, 0.6), ('block/sculk_shrieker/break2', 1.2, 0.6)],
    'entity.coral_hook.hit': [('item/trident/pierce1', 1.0, 1.2), ('item/trident/pierce2', 1.0, 1.15)],
    'entity.coral_hook.strain': [('block/chain/step2', 0.8, 1.2), ('block/chain/step4', 0.8, 1.1)],
    'entity.coral_hook.snap': [('entity/leashknot/break', 1.0, 1.0), ('random/break', 0.8, 1.4)],
}
SUBTITLES = {
    'entity.sculk_fish.ambient': 'Sculk Fish clicks', 'entity.sculk_fish.bite': 'Sculk Fish bites', 'entity.sculk_fish.hurt': 'Sculk Fish hurts',
    'entity.sculk_fish.death': 'Sculk Fish dies', 'entity.sculk_fish.flop': 'Sculk Fish flops',
    'entity.coral_organ.pipe': 'Coral Organ plays', 'entity.coral_organ.reed': 'Coral Organ drones', 'entity.coral_organ.drone': 'Coral Organ hums',
    'entity.coral_organ.charge': 'Coral Organ shrieks', 'entity.coral_organ.fire': 'Coral Organ fires a hook', 'entity.coral_organ.reel': 'Line reels in',
    'entity.coral_organ.clamp': 'Coral Organ clamps', 'entity.coral_organ.hurt': 'Coral Organ cracks', 'entity.coral_organ.death': 'Coral Organ crumbles',
    'entity.coral_hook.hit': 'Hook bites', 'entity.coral_hook.strain': 'Line strains', 'entity.coral_hook.snap': 'Line snaps',
}


# ============================================================================ assets, data and text (gen_assets.generate, before gen_sounds)

def assets(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    tag, LANG = GA.tag, GA.LANG
    for e in ('sculk_fish', 'coral_organ'):
        tag('entity_type', f'{NS}:sculk_water_dwellers', rl(e))
    tag('entity_type', 'minecraft:aquatic', rl('sculk_fish'))
    tag('entity_type', 'minecraft:aquatic', rl('coral_organ'))
    tag('entity_type', f'{NS}:chrome_dwellers', rl('sculk_fish'))
    for f in MUSIC_FISH:
        tag('item', 'minecraft:fishes', rl(f))
        tag('item', 'minecraft:fishes', rl('cooked_' + f))
    import gen_data as GD
    # Sculk Fish: a mouthful of teeth and a little sculk
    GD.table('entity', 'entities/sculk_fish', [
        GD.pool([GD.item('minecraft:bone_meal', count=(0, 2), extra=[GD.LOOTING])]),
        GD.pool([GD.item('minecraft:sculk_vein', extra=[GD.LOOTING])], condition=GD.chance(0.35)),
    ])
    # the Coral Organ: its voice crystals, its coral, and something it dragged down before you
    GD.table('entity', 'entities/coral_organ', [
        GD.pool([GD.item('minecraft:echo_shard', count=(1, 2), extra=[GD.LOOTING])]),
        GD.pool([GD.item('sculk_coral', count=(2, 4)), GD.item('sculk_coral_fan', count=(1, 3)), GD.item('sculk_coral_block', count=(1, 2))],
                rolls=2),
        GD.pool([GD.item('star_shard', weight=4, count=(1, 2)), GD.item('minecraft:gold_ingot', weight=3),
                 GD.item('siftite_nugget', weight=2, count=(1, 3)), GD.item('music_sheet_lullaby', weight=2), GD.item('minecraft:nautilus_shell', weight=2)],
                condition={'type': 'minecraft:all_of', 'terms': [GD.PLAYER_KILL, GD.chance(0.75)]}),
    ])
    LANG.update({
        f'entity.{NS}.sculk_fish': 'Sculk Fish',
        f'entity.{NS}.coral_organ': 'Sculk Coral Organ',
        f'entity.{NS}.coral_hook': 'Hooked Line',
        f'band.{NS}.instrument.chattering_teeth': 'Chattering Teeth',
        f'band.{NS}.instrument.coral_organ': 'Coral Organ',
        f'codex.{NS}.sculk_fish.title': 'Sculk Fish',
        f'codex.{NS}.sculk_fish.tagline': 'Hostile - they hunt by sound',
        f'codex.{NS}.sculk_fish.body': 'Small, deep-bodied biters of the Sculk Ocean and the swamp\'s Sculk Water, sculk-dark with glowing eyes, '
                                       'a row of lights down the flank and two sensor tendrils on the brow. Like all sculk they hunt by '
                                       'vibration: fast swimming, splashing fish and played notes carry far, while a sneaking swimmer is '
                                       'only noticed up close. Once one fish finds you the whole school turns at once - each darts in, '
                                       'bites and circles back. Some glow cyan, some bone-pale, a few abyss-violet.',
        f'codex.{NS}.coral_organ.title': 'Sculk Coral Organ',
        f'codex.{NS}.coral_organ.tagline': 'Hostile - it plays, then it fishes',
        f'codex.{NS}.coral_organ.body': 'A living reef of sculk coral grown into organ pipes, rooted on the Sculk Ocean floor. Its eerie chords '
                                        'carry far through the water. It harpoons anyone swimming in its reach - or floating on the surface '
                                        'above - with a bone hook on a glowing line, and drags them down to its pipes to drown. Hit the line '
                                        'to break it (whatever you swing at, you strike the line) or destroy the organ. Inside it lie echo '
                                        'shards, coral and whatever it dragged down before you.',
    })


# ============================================================================ worldgen (gen_world.generate, after W1's Sculk Ocean)

def _patch(GW, rel, fn):
    path = os.path.join(GW.D, rel + '.json')
    with open(path) as f:
        obj = json.load(f)
    GW.w(rel, fn(obj) or obj)


def _spawn(lst, mob, weight, lo, hi):
    if not any(e['type'] == rl(mob) for e in lst):
        lst.append({'type': rl(mob), 'count': {'type': 'minecraft:uniform', 'min_inclusive': lo, 'max_inclusive': hi}, 'weight': weight})


def world(GW):
    # the Coral Organs: rooted on the floor of the Sculk Ocean among its reefs, where the surface is in reach of their lines
    GW.feature('coral_organ', {'type': f'{NS}:coral_organ', 'max_depth': 28})
    GW.placed('coral_organ', 'coral_organ', [GW.rarity(5), {'type': 'minecraft:in_square'},
                                             {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'}, GW.BIOME])

    def ocean(b):
        b['features'][9].append(rl('coral_organ'))
        cats = b['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']
        _spawn(cats['water_ambient'], 'sculk_fish', 12, 3, 6)
        return b

    def swamp(b):
        cats = b['attributes']['minecraft:gameplay/natural_mob_spawns']['argument']['spawns_by_category']
        _spawn(cats['water_ambient'], 'sculk_fish', 8, 2, 4)
        return b
    _patch(GW, 'worldgen/biome/deep_dark_ocean', ocean)
    _patch(GW, 'worldgen/biome/sculk_swamp', swamp)


# ============================================================================ item art (items16.all_items, last)

def _grid(rows, pal, no_ol=''):
    import items16 as I
    rows = [(r + '.' * 16)[:16] for r in rows]
    rows = (rows + ['.' * 16] * 16)[:16]
    return I.grid(rows, pal, ol=True, no_ol=no_ol)


def item_sprites():
    from fish_items import sprites
    return sprites()


# ============================================================================ textures (gen_textures.main, before vanilla_remap)

def textures(out):
    from fish_items import barb
    out('entity/coral_organ/hook', barb())
