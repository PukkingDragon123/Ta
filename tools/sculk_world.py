"""W1 World & terrain: the Sculk Swamp and the Sculk Ocean, pale bone-white Dreamstone, softer biome edges,
buried relic caches (the Sift's crumbled ruins) and the Weaver's hollow.

Hooked in from one line each:
  * spec.py           -> declare(block, item): Sculk Mud, Sculk Water (+ bucket), Sculk Coral (block, plant, fan, wall fan)
                         (Blightwood is a wood family in spec.WOODS)
  * gen_assets.py     -> gen_block(GA, b) for the 'w1_*' model kinds; assets(GA): sounds, recipes, loot, tags, lang, Codex
  * gen_world.py      -> offset_terms(...) (the swamp's flats, the ocean's trenches and ridges) and world(GW) (features,
                         biomes, placement, surface rules, blending and feature fading for every biome)
  * gen_textures.py   -> textures(out) (+ the vanilla references for tools/vanilla_remap.py)
Java: registry/ModSculkSwamp, block/SculkWater*, block/SculkMudBlock, worldgen/BiomeBlendFeature, worldgen/RelicCacheFeature,
worldgen/SiftTreeFeature ('blight' style), client/SculkSwampClient.
"""
from __future__ import annotations

import json
import math
import os
import random

import numpy as np
from PIL import Image

NS = 'thesift'
VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')
SURFACE_BIOMES = ('sift_plains', 'forest_mountains', 'rocky_dunes', 'chrome_lakes', 'wishing_grove', 'white_forest', 'magic_kelp_forest',
                  'deep_dark_ocean', 'sculk_swamp')
# the Sculk Swamp's slot in the climate: humid coastal lowlands, neither frozen nor scorching
SWAMP_BOX = {'temperature': [-0.4, 0.35], 'humidity': [0.2, 1.0], 'continentalness': [-0.19, 0.06], 'erosion': [-0.22, 1.0],
             'weirdness': [-1.0, 1.0]}


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# ============================================================================ spec (registries)

def declare(block, item):
    # the floor of the swamp and the sculk sea: you sink in a little, it drags at your feet, sculk pulses in it
    block('sculk_mud', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.MUD).mapColor(MapColor.COLOR_CYAN).speedFactor(0.65F)',
          cls='SculkMudBlock', model='cube_all', tags=['shovel', 'dirt'], tab='nature')
    # Sculk Water: a real fluid (registry/ModSculkSwamp), its block and its bucket
    block('sculk_water', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 2)',
          cls='SculkWaterBlock', model='w1_liquid', item=False, loot='none')
    item('sculk_water_bucket', cls='BucketItem', factory='p -> new BucketItem(com.thesift.registry.ModSculkSwamp.SCULK_WATER.get(), p)',
         props='new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)', name='Sculk Water Bucket')
    # Sculk Coral: the Sculk Ocean's reefs (never dying, faintly glowing)
    coral = 'BlockBehaviour.Properties.ofFullCopy(Blocks.TUBE_CORAL).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 4)'
    block('sculk_coral_block', 'cube', 'BlockBehaviour.Properties.ofFullCopy(Blocks.TUBE_CORAL_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 3)',
          tags=['pickaxe'], tab='nature')
    block('sculk_coral', 'custom', coral, cls='BaseCoralPlantBlock', model='cross', loot='silk', tab='nature')
    block('sculk_coral_fan', 'custom', coral, cls='BaseCoralFanBlock', model='w1_coral_fan', item=False, loot='silk')
    block('sculk_coral_wall_fan', 'custom', coral, cls='BaseCoralWallFanBlock', model='w1_coral_wall_fan', item=False, loot='none')
    item('sculk_coral_fan', cls='StandingAndWallBlockItem', tab='nature', name='Sculk Coral Fan',
         factory='p -> new StandingAndWallBlockItem(ModBlocks.SCULK_CORAL_FAN.get(), ModBlocks.SCULK_CORAL_WALL_FAN.get(), '
                 'net.minecraft.core.Direction.DOWN, p)',
         props='new Item.Properties().useBlockDescriptionPrefix()')


# ============================================================================ block models (gen_assets.gen_block)

def gen_block(GA, b):
    bid, k = b['id'], b['model']
    if k == 'w1_liquid':
        GA.write(os.path.join(GA.A, 'models/block', bid + '.json'), {'textures': {'particle': f'{NS}:block/{bid}_still'}})
        GA.TEXTURES.add(f'block/{bid}_still')
        GA.simple_state(bid)
    elif k == 'w1_coral_fan':
        GA.block_model(bid, 'minecraft:block/coral_fan', {'fan': f'block/{bid}'})
        GA.simple_state(bid)
    elif k == 'w1_coral_wall_fan':
        GA.block_model(bid, 'minecraft:block/coral_wall_fan', {'fan': f'block/{bid.replace("_wall_fan", "_fan")}'})
        GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': {
            f'facing={f}': ({'model': f'{NS}:block/{bid}', 'y': y} if y else {'model': f'{NS}:block/{bid}'})
            for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
    else:
        raise ValueError(f'no W1 asset rule for {bid} ({k})')


# ============================================================================ sounds

SOUNDS = {
    'music.sculk_swamp': [('music/game/swamp/aerie', 0.55, 0.9), ('music/game/swamp/firebugs', 0.55, 0.88),
                          ('music/game/swamp/labyrinthine', 0.55, 0.85), ('music/game/deeper', 0.5, 0.95)],
    # a low, wet drone; clicks, bubbles and far-off croaks; now and then the swamp's heart beats
    'ambient.sculk_swamp.loop': [('ambient/nether/soulsand_valley/ambience', 0.35, 0.62)],
    'ambient.sculk_swamp.additions': [('block/sculk_sensor/sculk_clicking2', 0.45, 0.8), ('block/sculk/spread1', 0.35, 0.7),
                                      ('block/bubble_column/bubble1', 0.4, 0.55), ('block/bubble_column/bubble3', 0.4, 0.65),
                                      ('block/pointed_dripstone/drip_water1', 0.5, 0.7), ('block/pointed_dripstone/drip_water3', 0.5, 0.6),
                                      ('mob/frog/idle2', 0.3, 0.55), ('mob/frog/idle6', 0.25, 0.5), ('ambient/underwater/additions/dark1', 0.3, 0.9)],
    'ambient.sculk_swamp.mood': [('ambient/cave/cave13', 0.7, 0.6), ('mob/warden/heartbeat_1', 0.45, 0.7),
                                 ('ambient/nether/soulsand_valley/mood1', 0.6, 0.7), ('ambient/nether/soulsand_valley/whisper2', 0.4, 0.6)],
    'block.sculk_water.ambient': [('block/bubble_column/bubble2', 0.35, 0.6), ('block/sculk/charge2', 0.25, 1.4)],
}
SUBTITLES = {
    'ambient.sculk_swamp.loop': 'The swamp hums', 'ambient.sculk_swamp.additions': 'Sculk clicks in the mud',
    'ambient.sculk_swamp.mood': 'The swamp throbs', 'block.sculk_water.ambient': 'Sculk Water bubbles',
}
STREAM = {'music.sculk_swamp', 'ambient.sculk_swamp.loop'}


# ============================================================================ assets, data and text (gen_assets.generate)

def assets(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    GA.STREAM.update(STREAM)
    tag, LANG = GA.tag, GA.LANG
    # --- recipes
    GA.shapeless('sculk_mud', ['minecraft:mud', 'minecraft:sculk'], 'sculk_mud', 2, 'building')
    GA.shaped('sculk_coral_block', ['##', '##'], {'#': 'sculk_coral'}, 'sculk_coral_block', 1, 'building')
    GA.shapeless('cyan_dye_from_sculk_coral', ['sculk_coral_fan'], 'minecraft:cyan_dye', 1, 'misc', 'cyan_dye')
    # the carved Glyph Stones came only from ruins that have crumbled away: carve them yourself
    GA.cutting('polished_dreamstone', 'glyph_stone')
    # --- loot: the wall fan drops the fan (silk touch), like a vanilla coral
    GA.loot('sculk_coral_wall_fan', 'dead_tube_coral_fan', {'dead_tube_coral_fan': 'sculk_coral_fan'})
    # --- tags
    tag('block', f'{NS}:sift_plantable', rl('sculk_mud'))
    for t in ('minecraft:sculk_replaceable', 'minecraft:sculk_replaceable_world_gen', 'minecraft:overworld_carver_replaceables',
              'minecraft:moss_replaceable', 'minecraft:lush_ground_replaceable'):
        tag('block', t, rl('sculk_mud'))
    tag('block', 'minecraft:dampens_vibrations', rl('sculk_mud'))
    tag('block', f'{NS}:sculk_corals', rl('sculk_coral'))
    tag('block', f'{NS}:sculk_corals', rl('sculk_coral_fan'))
    tag('block', f'{NS}:sculk_wall_corals', rl('sculk_coral_wall_fan'))
    for e in ('sculk_parasite', 'strumling', 'gobbler', 'jailer', 'sculkling', 'minecraft:warden'):
        tag('entity_type', f'{NS}:sculk_water_dwellers', rl(e))
    tag('worldgen/biome', f'{NS}:is_sift', rl('sculk_swamp'))
    # --- text
    LANG.update({
        f'biome.{NS}.sculk_swamp': 'Sculk Swamp',
        f'codex.{NS}.sculk_swamp.title': 'Sculk Swamp',
        f'codex.{NS}.sculk_swamp.tagline': 'Where the sculk soaks into the ground',
        f'codex.{NS}.sculk_swamp.body': 'Low, wet coastlands the sculk has rotted through. Gnarled Blightwood leans out of Sculk Mud that '
                                        'drags at your feet, its bark veined with glowing sculk. Pools of dark Sculk Water glimmer '
                                        'between the roots: each soak corrupts you a little more. Sculk Blooms and wild Pitcher Plants '
                                        'grow on the hummocks. Sculk Parasites and Sculk Spiders crawl up out of the mud around anyone '
                                        'who wades in, light or dark - and in its deepest hollow, under a ring of humming webs, the '
                                        'Weaver waits on its Encore Sigil.',
        f'codex.{NS}.sculk_ocean.title': 'Sculk Ocean',
        f'codex.{NS}.sculk_ocean.tagline': 'A cold sea grown over with sculk',
        f'codex.{NS}.sculk_ocean.body': 'The Sift\'s cold sea, its water stained the dark teal of the sculk below. Its floor of Sculk Mud is '
                                        'cut by deep trenches and long ridges, with huge sculk-crusted pillars rising to the surface. '
                                        'Reefs of glowing Sculk Coral - blocks, branches and fans - light the gloom, and glowing motes '
                                        'hang in the water. Kazoo Fish, Fanfare Eels and Tubafish school over the reefs, glow squid '
                                        'drift in the trenches, and the blind Gobbler hunts by sound. Swim slowly.',
        f'codex.{NS}.relics.title': 'Buried Relics', f'codex.{NS}.relics.tagline': 'All that is left of the old ruins',
        f'codex.{NS}.relics.body': 'The Sift\'s old towers, temples and settlements have crumbled into the ground. Where a cracked brick '
                                   'or two pokes out of the dunes, the shores or the swamp mud, there is Suspicious Dreamsand just '
                                   'below: brush it for Dream Journal Fragments, seeds, Star Shards, lost Music Sheets and - rarely - '
                                   'a Siftite Upgrade Template or the Lullaby disc.',
    })


# ============================================================================ terrain (gen_world.terrain_density)

def offset_terms(df, ramp, clamp01, temperature, humidity, continents, erosion, base):
    """Extra terms for the terrain offset (0.01 = about 1.3 blocks).

    * the Sculk Swamp: pulled down to a soggy flat around sea level, with hummocks and hollows (the hollows flood with
      Sculk Water), fading in and out with the swamp's climate so its edges slope gently;
    * the Sculk Ocean: long winding trenches cut deep into its floor and broad ridges between them, fading out before the
      shore.
    """
    def band(x, lo, hi, soft):
        """1 inside [lo, hi], falling to 0 over `soft` outside either end."""
        return df('mul', left=clamp01(df('mul', left=df('add', left=x, right=-(lo - soft)), right=1.0 / soft)),
                  right=clamp01(df('mul', left=df('add', left=df('mul', left=x, right=-1.0), right=hi + soft), right=1.0 / soft)))
    noise = lambda name, s: df('noise', noise=name, xz_scale=s, y_scale=0.0)  # noqa: E731
    b = SWAMP_BOX
    swamp = df('mul', left=df('mul', left=band(temperature, *b['temperature'], 0.06), right=band(humidity, *b['humidity'], 0.08)),
               right=df('mul', left=band(continents, *b['continentalness'], 0.05), right=band(erosion, *b['erosion'], 0.08)))
    # target: just under sea level (offset -0.512 = y 62.5), rolling by a block or two
    target = df('add', left=-0.512, right=df('add', left=df('mul', left=noise('minecraft:surface_swamp', 1.4), right=0.014),
                                             right=df('mul', left=noise('minecraft:surface_secondary', 3.0), right=0.006)))
    swamp_term = df('mul', left=swamp, right=df('add', left=target, right=df('mul', left=base, right=-1.0)))
    # the ocean: cold and deep
    ocean = df('mul', left=clamp01(df('mul', left=df('add', left=df('mul', left=temperature, right=-1.0), right=0.13), right=12.0)),
               right=clamp01(df('mul', left=df('add', left=df('mul', left=continents, right=-1.0), right=-0.43), right=12.0)))
    trench = clamp01(df('mul', left=df('add', left=0.16, right=df('mul', left=df('abs', input=noise('minecraft:gravel', 0.85)), right=-1.0)),
                        right=8.0))
    ridge = clamp01(df('mul', left=df('add', left=0.3, right=df('mul', left=df('abs', input=noise('minecraft:calcite', 1.3)), right=-1.0)),
                       right=4.0))
    ocean_term = df('mul', left=ocean, right=df('add', left=df('mul', left=ridge, right=0.085), right=df('mul', left=trench, right=-0.24)))
    return [swamp_term, ocean_term]


# ============================================================================ worldgen (gen_world.generate, after the other biomes)

def _uniform(a, b):
    return {'type': 'minecraft:uniform', 'min_inclusive': a, 'max_inclusive': b}


def _patch(GW, rel, fn):
    path = os.path.join(GW.D, rel + '.json')
    with open(path) as f:
        obj = json.load(f)
    GW.w(rel, fn(obj) or obj)


def _features(GW):
    feature, placed, state, count, rarity, survive, BIOME = GW.feature, GW.placed, GW.state, GW.count, GW.rarity, GW.survive, GW.BIOME
    sq = {'type': 'minecraft:in_square'}

    def hm(h):
        return {'type': 'minecraft:heightmap', 'heightmap': h}
    dry = {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0}
    in_water = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water'}}
    air = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}}
    # ------------------------------------------------------------------ step 0: Sculk Water in the swamp's hollows; soft edges everywhere
    feature('swamp_flood', {'type': f'{NS}:sea_flood', 'biomes': [rl('sculk_swamp')], 'sea_level': 63, 'fluid': state('sculk_water', level=0)})
    placed('swamp_flood', 'swamp_flood', [])

    def pal(biomes, top, under):
        return {'biomes': [rl(b) for b in biomes], 'top': top, 'under': under}
    feature('biome_blend', {'type': f'{NS}:biome_blend', 'radius': 12, 'palettes': [
        pal(['sift_plains'], state('coral_turf', snowy=False), state('sift_soil')),
        pal(['white_forest'], state('white_turf', snowy=False), state('sift_soil')),
        pal(['forest_mountains', 'wishing_grove', 'chrome_lakes'], state('sift_grass_block', snowy=False), state('sift_soil')),
        pal(['rocky_dunes'], state('dreamsand'), state('dreamsand')),
        pal(['sculk_swamp', 'deep_dark_ocean'], state('sculk_mud'), state('sculk_mud')),
        pal(['magic_kelp_forest'], state('coral_sand'), state('coral_sand')),
    ]})
    placed('biome_blend', 'biome_blend', [])
    # ------------------------------------------------------------------ the Sculk Swamp
    not_protected = {'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:features_cannot_replace'}}
    feature('sculk_water_pool', {'type': 'minecraft:lake', 'barrier': state('sculk_mud'), 'can_place_feature': {'type': 'minecraft:true'},
                                 'can_replace_with_air_or_fluid': not_protected,
                                 'can_replace_with_barrier': {'type': 'minecraft:not', 'predicate': {
                                     'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:lava_pool_stone_cannot_replace'}},
                                 'fluid': state('sculk_water', level=0)})
    placed('sculk_water_pool', 'sculk_water_pool', [rarity(2), sq, hm('WORLD_SURFACE_WG'), BIOME])
    # pale Dreamstone boulders shouldering out of the mud
    feature('swamp_boulder', {'type': 'minecraft:block_blob', 'state': state('dreamstone'), 'can_place_on': {
        'type': 'minecraft:matching_blocks', 'blocks': [rl('sculk_mud'), 'minecraft:sculk']}})
    placed('swamp_boulder', 'swamp_boulder', [rarity(5), sq, hm('WORLD_SURFACE_WG'), BIOME])
    # buried relics: what is left of the Sift's old ruins (worldgen/RelicCacheFeature)
    feature('relic_cache', {'type': f'{NS}:relic_cache', 'relic': state('suspicious_dreamsand'), 'rare_chance': 0.15,
                            'hosts': [state('dreamsand'), state('sculk_mud'), state('sift_soil'), state('coral_sand')],
                            'common': f'{NS}:archaeology/sift_common', 'rare': f'{NS}:archaeology/sift_rare',
                            'marker': state('cracked_dreamstone_bricks')})
    placed('relic_cache', 'relic_cache', [rarity(14), sq, hm('WORLD_SURFACE_WG'), BIOME])
    placed('relic_cache_dunes', 'relic_cache', [rarity(6), sq, hm('WORLD_SURFACE_WG'), BIOME])
    # Blightwood (worldgen/SiftTreeFeature 'blight')
    log = state('blightwood_log', axis='y')
    leaves = state('blightwood_leaves', distance=7, persistent=False, waterlogged=False)
    feature('blightwood_tree', {'type': f'{NS}:sift_tree', 'trunk': log, 'leaves': leaves, 'style': 'blight', 'min_height': 7, 'max_height': 10})
    feature('gnarled_blightwood_tree', {'type': f'{NS}:sift_tree', 'trunk': log, 'leaves': leaves, 'style': 'blight', 'min_height': 11,
                                        'max_height': 15})

    def tree(name):
        return {'feature': rl(name), 'placement': [survive('blightwood_sapling')]}
    feature('trees_sculk_swamp', {'type': 'minecraft:random_selector', 'default': tree('blightwood_tree'),
                                  'features': [{'chance': 0.3, 'feature': tree('gnarled_blightwood_tree')}]})
    placed('trees_sculk_swamp', 'trees_sculk_swamp', [
        count({'type': 'minecraft:weighted_list', 'distribution': [{'data': 2, 'weight': 3}, {'data': 3, 'weight': 4}, {'data': 5, 'weight': 2}]}),
        sq, dry, hm('OCEAN_FLOOR'), BIOME])
    # sculk spreading over the mud, veins creeping up trunks and stones
    feature('swamp_sculk_patch', {'type': 'minecraft:sculk_patch', 'amount_per_charge': 24, 'charge_count': 6, 'growth_rounds': 0,
                                  'spread_attempts': 40, 'spread_rounds': 1})
    placed('swamp_sculk_patch', 'swamp_sculk_patch', [count(2), sq, hm('WORLD_SURFACE_WG'), BIOME])
    feature('swamp_sculk_vein', {'type': 'minecraft:multiface_growth', 'block': 'minecraft:sculk_vein', 'can_be_placed_on': [
        rl('blightwood_log'), rl('blightwood_wood'), rl('sculk_mud'), 'minecraft:sculk', rl('dreamstone')], 'can_place_on_ceiling': False,
        'can_place_on_floor': True, 'can_place_on_wall': True, 'chance_of_spreading': 0.6, 'search_range': 6})
    placed('swamp_sculk_vein', 'swamp_sculk_vein', [count(14), sq, hm('MOTION_BLOCKING_NO_LEAVES'),
                                                    {'type': 'minecraft:offset', 'x': 0, 'y': _uniform(0, 4), 'z': 0}, BIOME])
    feature('sculk_bloom', {'type': 'minecraft:simple_block', 'to_place': state('sculk_bloom')})
    placed('patch_sculk_bloom', 'sculk_bloom', [count(2), sq, hm('WORLD_SURFACE_WG'), BIOME] + GW.surface_patch(20, 6, 2) + [survive('sculk_bloom')])
    # the Weaver's hollow: its Encore Sigil set into the floor under a ring of humming webs, sculk round it
    feature('weaver_hollow', {'type': 'minecraft:sequence', 'features': [
        {'feature': {'type': 'minecraft:simple_block', 'to_place': state('encore_sigil', boss=2, spent=False)},
         'placement': [{'type': 'minecraft:offset', 'x': 0, 'y': -1, 'z': 0}]},
        {'feature': {'type': 'minecraft:simple_block', 'to_place': state('minecraft:sculk')},
         'placement': [count(36), {'type': 'minecraft:offset', 'x': _uniform(-6, 6), 'y': -1, 'z': _uniform(-6, 6)},
                       {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_blocks',
                                                                                 'blocks': [rl('sculk_mud'), rl('sift_soil')]}}]},
        {'feature': {'type': 'minecraft:simple_block', 'to_place': state('musical_cobweb', ringing=False)},
         'placement': [count(26), {'type': 'minecraft:offset', 'x': _uniform(-7, 7), 'y': _uniform(0, 6), 'z': _uniform(-7, 7)}, air]},
    ]})
    placed('weaver_hollow', 'weaver_hollow', [rarity(24), sq, hm('MOTION_BLOCKING_NO_LEAVES'), dry, BIOME])
    # the Echoer's hearth (re-homed from the removed Echoer's Hut): its Hearthstone set flush in a 5x5 pale pavement, Echo Orchids
    # round it; the first player to come near wakes the household once - an Echoer and two wild Soul Golems (EchoerHutHeartBlockEntity)
    sturdy = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:has_sturdy_face', 'direction': 'up'}}

    def pave(block, dx, dz, chance=None):
        pl = ([{'type': 'minecraft:random_chance', 'chance': chance}] if chance else []) + [
            {'type': 'minecraft:offset', 'x': dx, 'y': -1, 'z': dz}, sturdy]
        return {'feature': {'type': 'minecraft:simple_block', 'to_place': state(block)}, 'placement': pl}
    ring = [(dx, dz) for dx in range(-2, 3) for dz in range(-2, 3) if max(abs(dx), abs(dz)) == 2 and abs(dx) != abs(dz)]
    feature('echoer_hearth', {'type': 'minecraft:sequence', 'features': [
        {'feature': {'type': 'minecraft:simple_block', 'to_place': state('echoer_hut_heart', spent=False)},
         'placement': [{'type': 'minecraft:offset', 'x': 0, 'y': -1, 'z': 0}]},
        *[pave('polished_dreamstone', dx, dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) if dx or dz],
        *[pave('dreamstone_bricks', dx, dz) for dx, dz in ring],
        *[pave('cracked_dreamstone_bricks', dx, dz, 0.3) for dx, dz in ring],
        *[pave('chiseled_dreamstone', dx, dz) for dx in (-2, 2) for dz in (-2, 2)],
        {'feature': {'type': 'minecraft:simple_block', 'to_place': state('echo_orchid')},
         'placement': [count(14), {'type': 'minecraft:offset', 'x': _uniform(-6, 6), 'y': 0, 'z': _uniform(-6, 6)}, hm('WORLD_SURFACE_WG'),
                       air, survive('echo_orchid')]},
    ]})
    placed('echoer_hearth', 'echoer_hearth', [rarity(110), sq, hm('MOTION_BLOCKING_NO_LEAVES'), dry, BIOME])
    # ------------------------------------------------------------------ the Sculk Ocean
    feature('pale_pillar', {'type': f'{NS}:jagged_pillar', 'body': state('dreamstone'), 'accent': state('cobbled_dreamstone'),
                            'coat': state('minecraft:sculk'), 'glow': state('sculk_coral_block'), 'min_height': 16, 'max_height': 44, 'radius': 3.4})
    placed('pale_pillar', 'pale_pillar', [rarity(4), sq, hm('OCEAN_FLOOR_WG'), BIOME])
    # Sculk Coral reefs: vanilla's warm-ocean reef shapes (tree, claw, mushroom) grown in sculk coral
    on_top = {'type': 'minecraft:simple_block', 'to_place': {'type': 'minecraft:random_block', 'blocks': f'#{NS}:sculk_corals'}}
    deco = [{'feature': {'type': 'minecraft:weighted_random_selector', 'features': [
        {'data': {'feature': on_top, 'placement': []}, 'weight': 24}, {'data': {'feature': {'type': 'minecraft:no_op'}, 'placement': []}, 'weight': 46}]},
             'placement': [{'type': 'minecraft:offset', 'x': 0, 'y': 1, 'z': 0}]}]
    for d, (dx, dz) in (('north', (0, -1)), ('east', (1, 0)), ('south', (0, 1)), ('west', (-1, 0))):
        deco.append({'feature': {'type': 'minecraft:simple_block', 'to_place': {
            'type': 'minecraft:rotated', 'direction': d, 'state': {'type': 'minecraft:random_block', 'blocks': f'#{NS}:sculk_wall_corals'}}},
            'placement': [{'type': 'minecraft:random_chance', 'chance': 0.22}, {'type': 'minecraft:offset', 'x': dx, 'y': 0, 'z': dz},
                          {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:water'}}]})
    feature('sculk_coral/decoration', {'type': 'minecraft:overlay', 'features': deco})
    feature('sculk_coral/block', {'type': 'minecraft:overlay', 'features': [
        {'feature': {'type': 'minecraft:simple_block', 'to_place': state('sculk_coral_block')}, 'placement': []},
        {'feature': rl('sculk_coral/decoration'), 'placement': []}]})
    reef_ok = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
        {'type': 'minecraft:any_of', 'predicates': [{'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:water'},
                                                    {'type': 'minecraft:matching_block_tag', 'tag': f'{NS}:sculk_corals'}]},
        {'type': 'minecraft:matching_blocks', 'blocks': 'minecraft:water', 'offset': [0, 1, 0]}]}}
    grown = {'feature': rl('sculk_coral/block'), 'placement': [reef_ok]}
    feature('sculk_reef', {'type': 'minecraft:simple_random_selector', 'features': [
        {'feature': {'type': 'minecraft:coral_tree', 'feature': grown}, 'placement': []},
        {'feature': {'type': 'minecraft:coral_claw', 'feature': grown}, 'placement': []},
        {'feature': rl('sculk_coral/block'), 'placement': [
            {'type': 'minecraft:offset', 'x': 0, 'y': _uniform(-3, -1), 'z': 0},
            {'type': 'minecraft:cuboid', 'include_edges': False, 'include_interior': False, 'xz_size': _uniform(3, 5), 'y_size': _uniform(3, 5)},
            {'type': 'minecraft:random_chance', 'chance': 0.9}, reef_ok]},
    ]})
    placed('sculk_reef', 'sculk_reef', [{'type': 'minecraft:noise_based_count', 'noise_factor': 400.0, 'noise_to_count_ratio': 9}, sq,
                                        hm('OCEAN_FLOOR_WG'), BIOME])
    feature('sculk_coral_scatter', {'type': 'minecraft:simple_block', 'to_place': {'type': 'minecraft:random_block', 'blocks': f'#{NS}:sculk_corals'}})
    placed('sculk_coral_scatter', 'sculk_coral_scatter', [count(22), sq, hm('OCEAN_FLOOR_WG'), in_water, BIOME])
    feature('ocean_glow_lichen', {'type': 'minecraft:multiface_growth', 'block': 'minecraft:glow_lichen', 'can_be_placed_on': [
        rl('hushslate'), rl('dreamstone'), rl('cobbled_hushslate'), rl('cobbled_dreamstone'), 'minecraft:sculk', rl('sculk_mud')],
        'can_place_on_ceiling': True, 'can_place_on_floor': False, 'can_place_on_wall': True, 'chance_of_spreading': 0.5, 'search_range': 12})
    placed('ocean_glow_lichen', 'ocean_glow_lichen', [count(24), sq, {'type': 'minecraft:height_range', 'height': {
        'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -20}, 'max_inclusive': {'absolute': 58}}}, in_water, BIOME])


def _swamp_biome(GW):
    GW.biome('sculk_swamp', fog='#35585b', sky='#4d7d80', water='#0f5258', grass='#2e6b66', foliage='#2a5f5c', temp=0.75, down=0.9,
             spawns=GW.mobs(creature=[('cypole', 10, 1, 3)],  # CR4: Cypoles, the one-eyed cymbal frogs
                            monster=[('sculk_parasite', 40, 1, 2), ('strumling', 10, 1, 1)]),
             parts=GW.particles(('minecraft:sculk_soul', 0.0007), ('glow_dust', 0.005), ('sift_mist', 0.0025), ('minecraft:sculk_charge_pop', 0.0014),
                                ('drifting_soul', 0.0005)),
             music=f'{NS}:music.sculk_swamp', ambient_loop=f'{NS}:ambient.sculk_swamp.loop',
             feats=[(0, 'swamp_flood'), (0, 'biome_blend'), (1, 'sculk_water_pool'), (2, 'swamp_boulder'), (2, 'relic_cache')]
                   + GW.COMMON_UNDERGROUND
                   + [(7, 'swamp_sculk_patch'), (9, 'trees_sculk_swamp'), (9, 'swamp_sculk_vein'), (9, 'patch_sculk_bloom'),
                      (9, 'patch_pitcher_plant'), (9, 'patch_glowcap_surface'), (9, 'weaver_hollow')])

    def ambience(b):
        a = b['attributes']
        a['minecraft:audio/ambient_sounds']['additions'] = {'sound': f'{NS}:ambient.sculk_swamp.additions', 'tick_chance': 0.013}
        a['minecraft:audio/ambient_sounds']['mood'] = {'block_search_extent': 8, 'offset': 2.0, 'sound': f'{NS}:ambient.sculk_swamp.mood',
                                                      'tick_delay': 5000}
        a['minecraft:visual/water_fog_color'] = '#06272b'
        a['minecraft:visual/water_fog_end_distance'] = {'argument': 0.6, 'modifier': 'multiply'}
        return b
    _patch(GW, 'worldgen/biome/sculk_swamp', ambience)


def _ocean_biome(GW):
    """The Sculk Ocean (biome id kept: deep_dark_ocean): replaces the sea & sky agent's version."""
    sea = __import__('sea_sky')
    sea._biome(GW, 'deep_dark_ocean', fog='#24434a', sky='#2f5a66', water='#0d5a63', water_fog='#041a1d', grass='#2f8f9e', foliage='#37a9b5',
               temp=0.3, down=0.5, music='music.deep_dark_ocean', loop='ambient.deep_dark_ocean.loop', additions='ambient.deep_dark_ocean.additions',
               mood='ambient.deep_dark_ocean.mood',
               spawns=sea._spawns(water_creature=[('gobbler', 3, 1, 1), ('fanfare_eel', 5, 1, 2), ('tubafish', 4, 1, 2)],
                                  water_ambient=[('kazoo_fish', 8, 3, 6)], underground_water_creature=[('minecraft:glow_squid', 10, 2, 4)]),
               parts=[('drifting_soul', 0.0015), ('glow_dust', 0.004), ('minecraft:sculk_soul', 0.0005), ('minecraft:glow', 0.0012),
                      ('minecraft:underwater', 0.003)],
               extra={'minecraft:visual/water_fog_end_distance': {'argument': 0.5, 'modifier': 'multiply'}},
               feats=[(0, 'sea_flood'), (0, 'biome_blend'), (2, 'relic_cache'), (4, 'deep_pillar'), (4, 'pale_pillar')] + GW.COMMON_UNDERGROUND
                     + [(7, 'minecraft:sculk_vein'), (7, 'minecraft:sculk_patch_deep_dark'), (9, 'patch_abyss_anemone'), (9, 'sculk_reef'),
                        (9, 'sculk_coral_scatter'), (9, 'ocean_glow_lichen')])


# ------------------------------------------------------------------ placement: carve the swamp's climate out of the land around it

AXES = ('temperature', 'humidity', 'continentalness', 'erosion', 'weirdness')


def _subtract(a, b):
    """Boxes covering parameter box a minus box b (axis-aligned; shared faces do not count as overlap)."""
    if any(a[k][1] <= b[k][0] or a[k][0] >= b[k][1] for k in AXES):
        return [a]
    pieces, cur = [], dict(a)
    for k in AXES:
        lo, hi = cur[k]
        if lo < b[k][0]:
            pieces.append({**cur, k: [lo, b[k][0]]})
        if hi > b[k][1]:
            pieces.append({**cur, k: [b[k][1], hi]})
        cur[k] = [max(lo, b[k][0]), min(hi, b[k][1])]
    return pieces


def _placement(dim):
    pts = dim['generator']['biome_source']['biomes']
    out = []
    for p in pts:
        par = p['parameters']
        if isinstance(par.get('depth'), list) or p['biome'] == rl('sculk_swamp'):
            out.append(p)
            continue
        for piece in _subtract(par, SWAMP_BOX):
            out.append({'biome': p['biome'], 'parameters': {**par, **{k: piece[k] for k in AXES}}})
    out.append({'biome': rl('sculk_swamp'), 'parameters': {**{k: list(v) for k, v in SWAMP_BOX.items()}, 'depth': 0.0, 'offset': 0.0}})
    dim['generator']['biome_source']['biomes'] = out
    return dim


# ------------------------------------------------------------------ surface rules

def _surface(GW, rule):
    def biome_is(b):
        return {'type': 'minecraft:biome', 'biome_is': [rl(b)]}

    def cond(c, then):
        return {'type': 'minecraft:condition', 'if_true': c, 'then_run': then}

    def block(b, **props):
        return {'type': 'minecraft:block', 'result_state': GW.state(b, **props)}

    def seq(*r):
        return {'type': 'minecraft:sequence', 'sequence': list(r)}

    def noise(name, lo, hi=1.0):
        return {'type': 'minecraft:noise_threshold', 'noise': name, 'min_threshold': lo, 'max_threshold': hi}
    steep = {'type': 'minecraft:steep'}
    mine = [
        # the swamp: Sculk Mud, sculk soaking through it in blotches
        cond(biome_is('sculk_swamp'), seq(
            cond('minecraft:on_floor', seq(cond(noise('minecraft:surface_swamp', 0.38), block('minecraft:sculk')),
                                           cond(noise('minecraft:patch', 0.55), block('minecraft:sculk')),
                                           block('sculk_mud'))),
            cond('minecraft:under_floor', block('sculk_mud')))),
        # the sculk sea: a floor of Sculk Mud and sculk, hushslate on the trench walls and in scattered outcrops
        cond(biome_is('deep_dark_ocean'), seq(
            cond('minecraft:on_floor', seq(cond(steep, block('hushslate', axis='y')),
                                           cond(noise('minecraft:surface', 0.3), block('minecraft:sculk')),
                                           cond(noise('minecraft:gravel', -1.0, -0.62), block('hushslate', axis='y')),
                                           block('sculk_mud'))),
            cond('minecraft:under_floor', seq(cond(steep, block('hushslate', axis='y')), block('sculk_mud'))),
            cond('minecraft:deep_under_floor', block('hushslate', axis='y')))),
    ]
    for r in rule['sequence']:
        if isinstance(r, dict) and r.get('type') == 'minecraft:condition' and r.get('if_true', {}).get('type') == 'minecraft:above_preliminary_surface':
            r['then_run']['sequence'][0:0] = mine
            return rule
    raise ValueError('the_sift material rule: no above_preliminary_surface branch')


# ------------------------------------------------------------------ softer edges for every biome

FADED = ('trees_sift_plains', 'trees_forest_mountains', 'trees_wishing_grove', 'trees_white_forest', 'trees_sculk_swamp', 'dream_boulder',
         'swamp_boulder', 'cloud_bush')


def _soften(GW):
    """Every surface biome runs the edge blend first; trees and boulders stray a few blocks over the border (their placement is
    jittered after the biome check), so woods thin out across an edge instead of stopping at it."""
    def add_blend(obj):
        if rl('biome_blend') not in obj['features'][0]:
            obj['features'][0].append(rl('biome_blend'))
        return obj

    def jitter(obj):
        pl = obj['placement']
        if any(isinstance(m, dict) and m.get('type') == 'minecraft:offset' for m in pl):
            return obj
        i = next(n for n, m in enumerate(pl) if isinstance(m, dict) and m.get('type') == 'minecraft:biome')
        height = next((m for m in pl if isinstance(m, dict) and m.get('type') == 'minecraft:heightmap'),
                      {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'})
        extra = [{'type': 'minecraft:offset', 'x': _uniform(-5, 5), 'y': 0, 'z': _uniform(-5, 5)}, height]
        extra += [m for m in pl if isinstance(m, dict) and m.get('type') == 'minecraft:surface_water_depth_filter']
        obj['placement'] = pl[:i + 1] + extra + pl[i + 1:]
        return obj
    for b in SURFACE_BIOMES:
        _patch(GW, f'worldgen/biome/{b}', add_blend)
    # the crumbled ruins' relics, buried in the dunes (most), the plains, the lake shores and the kelp forest's sand
    for b, relic in (('rocky_dunes', 'relic_cache_dunes'), ('sift_plains', 'relic_cache'), ('chrome_lakes', 'relic_cache'),
                     ('magic_kelp_forest', 'relic_cache')):
        def add_relic(obj, relic=relic):
            if rl(relic) not in obj['features'][2]:
                obj['features'][2].append(rl(relic))
            return obj
        _patch(GW, f'worldgen/biome/{b}', add_relic)
    # the Echoer's hearth where its hut used to stand; first in step 9, so later flowers and trees respect the pavement
    for b in ('sift_plains', 'wishing_grove', 'forest_mountains'):
        def add_hearth(obj):
            if rl('echoer_hearth') not in obj['features'][9]:
                obj['features'][9].insert(0, rl('echoer_hearth'))
            return obj
        _patch(GW, f'worldgen/biome/{b}', add_hearth)
    for name in FADED:
        _patch(GW, f'worldgen/placed_feature/{name}', jitter)


def world(GW):
    _features(GW)
    _swamp_biome(GW)
    _ocean_biome(GW)
    _patch(GW, 'dimension/the_sift', _placement)
    _patch(GW, 'worldgen/material_rule/the_sift', lambda r: _surface(GW, r))
    _soften(GW)


# ============================================================================ textures (gen_textures.main, before vanilla_remap)

BARK = ('#0e1418', '#151e23', '#1d292f', '#26343b', '#314349', '#3d5257')       # charcoal bark, a cold teal cast
WOOD = ('#163236', '#1d4044', '#244e52', '#2d5d60', '#386d6e', '#467f7d')       # dark teal heartwood
LEAF = ('#0a2124', '#0f2d2e', '#153b3a', '#1c4a47', '#255b55', '#33706a')       # torn, almost black-teal crowns
MUD = ('#0a1b1f', '#0f252a', '#143036', '#1a3a40', '#21464b', '#2a5357')
CORAL = ('#051a22', '#082a35', '#0c3b47', '#114d58', '#18626b', '#23787e', '#348f90')
GLOW = (84, 236, 222)
GLOW_DIM = (40, 150, 148)
STILL_FRAMES = 32


def _hex(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))


def _vanilla(rel):
    return np.asarray(Image.open(os.path.join(VANILLA, rel + '.png')).convert('RGBA'), dtype=np.float64)


def _img(a):
    return Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA')


def _recolor(a, ramp, mask=None, spread=34.0):
    """Mojang's value structure, our colours: each pixel's luminance rank picks its colour from the ramp."""
    import vanilla_remap as VR
    out = a.copy()
    m = (a[..., 3] > 0) if mask is None else mask
    if m.any():
        out[m, :3] = VR.Pal(np.array([_hex(c) for c in ramp], dtype=np.float64), spread).at(VR._rank(VR._lum(a)[m]))
    return out


def _veins(a, seed, runs=3):
    """Glowing sculk veins along the bark's darkest cracks: short bright runs with a dim halo either side."""
    import vanilla_remap as VR
    rng = random.Random(seed)
    L = VR._lum(a)
    h, w = L.shape
    for _ in range(runs):
        x = rng.randrange(w)
        y = int(np.argmin(L[:, x] + np.array([rng.random() * 40 for _ in range(h)])))
        n = rng.randint(2, 4)
        for k in range(n):
            yy = (y + k) % h
            if a[yy, x, 3] == 0:
                continue
            a[yy, x, :3] = GLOW if 0 < k < n - 1 or n == 2 else GLOW_DIM
            for xx in ((x - 1) % w, (x + 1) % w):
                if a[yy, xx, 3] > 0:
                    a[yy, xx, :3] = a[yy, xx, :3] * 0.6 + np.array(GLOW_DIM) * 0.4
    return a


def _polyps(a, n):
    """A few of a coral's brightest pixels become glowing polyps (a bright core, a dimmer rim)."""
    m = a[..., 3] > 0
    L = a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114
    ys, xs = np.nonzero(m)
    order = np.argsort(-L[ys, xs], kind='stable')
    rng = random.Random(int(L[m].sum()) % 997)
    picks = [i for i in order[:n * 4]]
    rng.shuffle(picks)
    for i in picks[:n]:
        a[ys[i], xs[i], :3] = (150, 255, 240)
    for i in picks[n:n * 2]:
        a[ys[i], xs[i], :3] = GLOW_DIM
    return a


def _sapling():
    """Blightwood sapling: the dark oak sapling's shape, a charcoal stem and dark teal leaves tipped with glow."""
    a = _vanilla('block/dark_oak_sapling')
    m = a[..., 3] > 0
    green = m & (a[..., 1] > a[..., 0] + 6)
    out = _recolor(a, LEAF, green)
    out = _recolor(out, BARK[1:], m & ~green)
    rng = random.Random(5)
    ys, xs = np.nonzero(green)
    for i in rng.sample(range(len(ys)), min(3, len(ys))):
        out[ys[i], xs[i], :3] = GLOW
    return out


def _bucket():
    """The vanilla water bucket, its water turned to Sculk Water with a glowing mote in it."""
    a = _vanilla('item/water_bucket')
    m = (a[..., 3] > 0) & (a[..., 2] > a[..., 0] + 25)
    out = _recolor(a, ('#0b3a40', '#11545a', '#1a6d70', '#2a8a86'), m)
    ys, xs = np.nonzero(m)
    if len(ys):
        i = int(np.argmax(xs + ys * 0.1))
        out[ys[i], xs[i], :3] = GLOW
    return out


def _water_px(v, alpha):
    """Sculk Water's colour for a vanilla water luminance v (0-255)."""
    t = np.clip((v - 120.0) / 120.0, 0.0, 1.0)
    lo, hi = np.array([5.0, 33.0, 39.0]), np.array([38.0, 126.0, 124.0])
    rgb = lo[None] * (1 - t[..., None]) + hi[None] * t[..., None]
    return np.concatenate([rgb, np.full(v.shape + (1,), float(alpha))], -1)


def _motes(frame, n_frames, size, seed, count, flow):
    """Glowing motes drifting up (still) or streaming along (flow) through one frame of Sculk Water."""
    rng = random.Random(seed)
    pts = []
    for _ in range(count):
        x0, y0 = rng.randrange(size), rng.randrange(size)
        speed = rng.choice((1, 2)) if flow else rng.choice((0.25, 0.5))
        phase = rng.random() * math.tau
        pts.append((x0, y0, speed, phase))
    out = []
    for x0, y0, speed, phase in pts:
        if flow:
            y = int(y0 + frame * speed * size / n_frames) % size
            x = x0
        else:
            y = int(round(y0 - frame * speed)) % size
            x = int(round(x0 + math.sin(phase + frame * math.tau / n_frames))) % size
        glow = 0.55 + 0.45 * math.sin(phase + frame * math.tau * 2 / n_frames)
        out.append((x, y, glow))
    return out


def _water(out):
    still = _vanilla('block/water_still')
    frames = []
    for f in range(STILL_FRAMES):
        fr = still[f * 16:(f + 1) * 16]
        a = _water_px(fr[..., 0], 190)
        for x, y, g in _motes(f, STILL_FRAMES, 16, 31, 5, False):
            a[y, x, :3] = np.array(GLOW) * g + a[y, x, :3] * (1 - g)
            a[y, x, 3] = 235
        frames.append(a)
    out('block/sculk_water_still', _img(np.concatenate(frames, 0)), {'animation': {'frametime': 2}})
    flow = _vanilla('block/water_flow')
    n = flow.shape[0] // 32
    frames = []
    for f in range(n):
        fr = flow[f * 32:(f + 1) * 32]
        a = _water_px(fr[..., 0], 190)
        for x, y, g in _motes(f, n, 32, 37, 9, True):
            a[y, x, :3] = np.array(GLOW) * g + a[y, x, :3] * (1 - g)
            a[y, x, 3] = 235
        frames.append(a)
    out('block/sculk_water_flow', _img(np.concatenate(frames, 0)), {'animation': {}})
    ov = _vanilla('block/water_overlay')
    a = _water_px(ov[..., 0], 190)
    a[..., 3] = np.where(ov[..., 3] > 0, 190, 0)
    out('block/sculk_water_overlay', _img(a))
    # seen with your eyes under it: a murky teal swirl with a few motes
    s = 64
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float64)
    k = math.tau / s
    w = np.sin(k * (xx + yy)) + 0.7 * np.sin(k * (2 * xx - yy) + 1.3) + 0.4 * np.sin(k * (xx - 3 * yy) + 0.4)
    t = (w + 2.1) / 4.2
    rgb = np.array([6.0, 30.0, 34.0])[None, None] * (1 - t[..., None]) + np.array([24.0, 92.0, 94.0])[None, None] * t[..., None]
    a = np.concatenate([rgb, np.full((s, s, 1), 255.0)], -1)
    rng = random.Random(41)
    for _ in range(14):
        a[rng.randrange(s), rng.randrange(s), :3] = GLOW
    out('misc/in_sculk_water', _img(a))


def _remap_entries():
    """tools/vanilla_remap.py references for every W1 block texture (it runs last and rebuilds them from these)."""
    import vanilla_remap as VR

    def old(name):
        return VR._old(name).copy()

    def bark(name, ref, seed, runs=3):
        return _veins(VR._clone(VR._ref(ref), VR._old(name)), seed, runs)

    def glow_leaves(name, ref):
        a = VR._clone(VR._ref(ref), VR._old(name))
        m = a[..., 3] > 0
        L = VR._lum(a)
        ys, xs = np.nonzero(m & (L >= np.quantile(L[m], 0.9)))
        rng = random.Random(9)
        for i in rng.sample(range(len(ys)), min(4, len(ys))):
            a[ys[i], xs[i], :3] = GLOW
        return a

    def mud(name, ref):
        a = VR._clone(VR._ref(ref), VR._old(name))
        VR._sculk(a, 17, frac=0.05, deep=(8, 44, 50), glow=GLOW)
        return a

    def coral(name, ref, polyps):
        return _polyps(VR._clone(VR._ref(ref), VR._old(name)), polyps)

    def cracked_bricks(name):
        """End Stone Bricks laid out, cracked where vanilla cracks its stone bricks."""
        a = VR._clone(VR._ref('end_stone_bricks'), VR._old('dreamstone_bricks'))
        crack = (VR._lum(VR._ref('stone_bricks')) - VR._lum(VR._ref('cracked_stone_bricks'))) > 26
        rgb = a[a[..., 3] > 0][:, :3]
        a[crack, :3] = rgb[np.argmin(VR._lum(rgb))] * 0.82
        return a

    def mossy_bricks(name):
        """End Stone Bricks with lumen moss growing where vanilla's mossy stone bricks grow theirs."""
        o = VR._old(name)
        a = VR._clone(VR._ref('end_stone_bricks'), VR._old('dreamstone_bricks'))
        mref = VR._ref('mossy_stone_bricks')
        moss = (mref[..., 3] > 0) & (mref[..., 1] > mref[..., 0] + 8)
        om = o[..., 3] > 0
        sat = o[..., :3].max(-1) - o[..., :3].min(-1)
        mp = o[om & (sat > 40)][:, :3]
        if len(mp) < 4:
            mp = np.array([(31, 111, 128), (41, 136, 154), (52, 162, 176), (67, 188, 196), (98, 214, 214)], dtype=np.float64)
        if moss.any():
            a[moss, :3] = VR.Pal(mp, 30).at(VR._rank(VR._lum(mref)[moss]))
        return a

    F, C = VR.F, VR.C
    return {
        # the pale, End-Stone-like Dreamstone family (the plain stone and bricks are cloned from End Stone in vanilla_remap.MAP)
        'cracked_dreamstone_bricks': F(cracked_bricks),
        'mossy_dreamstone_bricks': F(mossy_bricks),
        # Blightwood
        'blightwood_log': F(bark, 'dark_oak_log', 11, 4),
        'blightwood_log_top': F(VR._ring, 'dark_oak_log_top', 'blightwood_log'),
        'stripped_blightwood_log': F(bark, 'stripped_dark_oak_log', 12, 2),
        'stripped_blightwood_log_top': C('stripped_dark_oak_log_top'),
        'blightwood_planks': C('dark_oak_planks'),
        'blightwood_door_top': C('dark_oak_door_top'),
        'blightwood_door_bottom': C('dark_oak_door_bottom'),
        'blightwood_trapdoor': C('dark_oak_trapdoor'),
        'blightwood_leaves': F(glow_leaves, 'dark_oak_leaves'),
        'blightwood_sapling': F(old),
        # the swamp and the sea floor
        'sculk_mud': F(mud, 'mud'),
        'sculk_coral_block': F(coral, 'tube_coral_block', 7),
        'sculk_coral': F(coral, 'tube_coral', 4),
        'sculk_coral_fan': F(coral, 'tube_coral_fan', 5),
    }


def textures(out):
    import vanilla_remap as VR
    cut = {'texture': {'mipmap_strategy': 'strict_cutout'}}
    leaves_meta = {'texture': {'mipmap_strategy': 'dark_cutout'}}
    # --- Blightwood (base colours; vanilla_remap rebuilds them from the dark oak textures with these palettes)
    out('block/blightwood_log', _img(_recolor(_vanilla('block/dark_oak_log'), BARK)))
    out('block/blightwood_log_top', _img(_recolor(_vanilla('block/dark_oak_log_top'), WOOD)))
    out('block/stripped_blightwood_log', _img(_recolor(_vanilla('block/stripped_dark_oak_log'), WOOD)))
    out('block/stripped_blightwood_log_top', _img(_recolor(_vanilla('block/stripped_dark_oak_log_top'), WOOD)))
    out('block/blightwood_planks', _img(_recolor(_vanilla('block/dark_oak_planks'), WOOD)))
    out('block/blightwood_door_top', _img(_recolor(_vanilla('block/dark_oak_door_top'), WOOD)), cut)
    out('block/blightwood_door_bottom', _img(_recolor(_vanilla('block/dark_oak_door_bottom'), WOOD)), cut)
    out('block/blightwood_trapdoor', _img(_recolor(_vanilla('block/dark_oak_trapdoor'), WOOD)), cut)
    out('block/blightwood_leaves', _img(_recolor(_vanilla('block/dark_oak_leaves'), LEAF)), leaves_meta)
    out('block/blightwood_sapling', _img(_sapling()), cut)
    out('item/blightwood_door', _img(_recolor(_vanilla('item/dark_oak_door'), WOOD)))
    # --- Sculk Mud, Sculk Coral
    out('block/sculk_mud', _img(_recolor(_vanilla('block/mud'), MUD)))
    out('block/sculk_coral_block', _img(_recolor(_vanilla('block/tube_coral_block'), CORAL)))
    for name in ('sculk_coral', 'sculk_coral_fan'):
        out(f'block/{name}', _img(_recolor(_vanilla(f'block/{name.replace("sculk", "tube")}'), CORAL)), cut)
    out('item/sculk_coral_fan', _img(_polyps(_recolor(_vanilla('block/tube_coral_fan'), CORAL), 5)))
    # --- Sculk Water and its bucket
    _water(out)
    out('item/sculk_water_bucket', _img(_bucket()))
    VR.MAP.update(_remap_entries())
    VR.SKIP.update({'sculk_water_still', 'sculk_water_flow', 'sculk_water_overlay'})
