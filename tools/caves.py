"""W-deep caves: the Sift's underground - real caves (no grass), rich ores, crystal dripstone, the Cave Jungle and the
Sculk Caves - placed by depth under every surface biome.

Underground layout (dimension multi_noise, depth > 0; see _placement):
  * Sift Caves (`sift_caves`): the normal caves, depth 0.2-0.9 everywhere the special cave biomes are not. Bare pale
    stone with seams of Tuff, Calcite and Magnesite, lots of vanilla Copper (scattered ore, rich pockets, and the big
    vanilla copper ore veins - in Magnesite here - through the material rule), extra Bauxite/Galena/Magnesite, glow
    lichen and crystal dripstone in rose, azure and amber (vanilla `speleothem` features on our own blocks).
  * Cave Jungle (`cave_jungle`): warm and humid ground (temperature >= 0.05, humidity 0.1-0.55), depth 0.2-0.9. Glowing
    Lumen Moss floors and ceilings, Glowbell vines, vines, spore blossoms, clay pools with dripleaf, Shocker plants, Acid
    Weepers dripping into Acid Puddles, and `thesift:cave_jungle_pitcher_spot` (a decorative dripleaf stalk for now:
    the Phase 4 Pitcher Plants replace its configured feature).
  * Sculk Caves (`sculk_caves`): cold ground (temperature <= -0.15, humidity <= 0.55), depth 0.35-1.1. Sculk floors and
    ceilings, pulsing Writhing Sculk, swaying Sculk Tendrils, Sculk Water lakes, catalysts/sensors/shriekers and the
    Sculk Grasper (block entity: lunges, holds and drags players in).
  * The Deep Sift keeps depth 0.9-1.1 (minus the Sculk Caves) but loses its moss carpet: deep hushslate, tuff, sculk,
    geodes, azure crystals and a few glowbells. The Caravans Cavern is untouched.
No grass underground: cave biomes never take the grass/turf surface rule, and `thesift:cave_scrub` (Java, step 0 of
every surface biome) turns grass left on any roofed cave floor back into stone.

Hooks (one line each): spec.py -> declare(block, item); gen_assets.gen_block ('wd_*' models) -> gen_block(GA, b);
gen_assets.generate() -> assets(GA); gen_world.generate() -> ores(GW) (before biomes()) and world(GW) (after every
biome); gen_textures.main() -> caves_art.textures(out). Java: registry/ModCaves, block/PointedCrystalBlock,
CrystalClusterBlock, ShockerPlantBlock, AcidWeeperBlock, AcidPuddleBlock, SculkTendrilBlock, SculkGrasperBlock,
block/entity/SculkGrasperBlockEntity, worldgen/CaveScrubFeature, client/CavesClient (+ SculkGrasperRenderer, AcidParticle).
"""
from __future__ import annotations

import copy
import json
import os

NS = 'thesift'
CRYSTALS = {'rose': 'MapColor.COLOR_PINK', 'azure': 'MapColor.COLOR_LIGHT_BLUE', 'amber': 'MapColor.COLOR_ORANGE'}
CAVE_BIOMES = ('sift_caves', 'cave_jungle', 'sculk_caves')
UNDERGROUND = CAVE_BIOMES + ('deep_sift', 'caravans_cavern')
SKY = ('sound_garden',)
GRASSES = ('sift_grass_block', 'coral_turf', 'white_turf')
FULL = [-2.0, 2.0]
# the cave biomes' boxes in the climate (depth is the vanilla depth parameter: 0 at the surface, ~1 = 128 blocks down)
BOXES = {
    'cave_jungle': {'temperature': [0.05, 2.0], 'humidity': [0.1, 0.55], 'continentalness': FULL, 'erosion': FULL, 'weirdness': FULL,
                    'depth': [0.2, 0.9]},
    'sculk_caves': {'temperature': [-2.0, -0.15], 'humidity': [-2.0, 0.55], 'continentalness': FULL, 'erosion': FULL, 'weirdness': FULL,
                    'depth': [0.35, 1.1]},
}
CAVE_LAYER = {'temperature': FULL, 'humidity': FULL, 'continentalness': FULL, 'erosion': FULL, 'weirdness': FULL, 'depth': [0.2, 0.9]}
AXES = ('temperature', 'humidity', 'continentalness', 'erosion', 'weirdness', 'depth')


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def title(s):
    return ' '.join(w.capitalize() for w in s.split('_'))


# ============================================================================ spec (blocks)

def declare(block, item):
    for c, mc in CRYSTALS.items():
        # crystal dripstone: a glassy, glowing rock that pointed crystals grow down from (and up towards)
        block(f'{c}_crystal_dripstone', 'cube', f'BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor({mc}).lightLevel(s -> 3)',
              tags=['pickaxe'], tab='nature')
        block(f'pointed_{c}_crystal', 'custom', f'BlockBehaviour.Properties.ofFullCopy(Blocks.POINTED_DRIPSTONE).mapColor({mc})'
              '.sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 5)', cls='PointedCrystalBlock', model='wd_pointed', tags=['pickaxe'], tab='nature')
        block(f'{c}_crystal_cluster', 'custom', f'BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor({mc}).lightLevel(s -> 7)',
              cls='CrystalClusterBlock', model='wd_cluster', tags=['pickaxe'], tab='nature')
    # the Cave Jungle
    block('shocker_plant', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.TORCHFLOWER).mapColor(MapColor.COLOR_YELLOW)'
          '.lightLevel(s -> s.getValue(com.thesift.block.ShockerPlantBlock.CHARGED) ? 9 : 2)', cls='ShockerPlantBlock', model='wd_shocker', tab='nature')
    block('acid_weeper', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.HANGING_ROOTS).mapColor(MapColor.COLOR_LIGHT_GREEN).lightLevel(s -> 6)'
          '.randomTicks()', cls='AcidWeeperBlock', model='wd_cross', loot='shears', tab='nature')
    block('acid_puddle', 'custom', 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).replaceable().noCollision().noOcclusion()'
          '.noLootTable().instabreak().randomTicks().lightLevel(s -> 5).sound(SoundType.SLIME_BLOCK)'
          '.pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)', cls='AcidPuddleBlock', model='wd_puddle', item=False, loot='none')
    # the Sculk Caves
    block('writhing_sculk', 'cube', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SCULK).lightLevel(s -> 3)', tags=['hoe'], loot='silk', tab='nature')
    block('sculk_tendril', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SCULK_VEIN).noOcclusion()'
          '.lightLevel(s -> s.getValue(com.thesift.block.SculkTendrilBlock.TIP) ? 6 : 2)', cls='SculkTendrilBlock', model='wd_tendril', tags=['hoe'],
          loot='shears', tab='nature')
    block('sculk_grasper', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SCULK_CATALYST).noOcclusion()'
          '.lightLevel(s -> s.getValue(com.thesift.block.SculkGrasperBlock.ACTIVE) ? 11 : 4)', cls='SculkGrasperBlock', model='wd_grasper',
          tags=['hoe'], loot='silk', tab='nature')


# ============================================================================ block models (gen_assets.gen_block)

def gen_block(GA, b):
    bid, k = b['id'], b['model']
    A = GA.A
    if k == 'wd_pointed':
        # vanilla pointed dripstone's ten models (thickness x direction) and its item, on our textures
        GA.copy_template('pointed_dripstone', bid, GA.token_tex('pointed_dripstone', bid))
    elif k == 'wd_cluster':
        GA.copy_template('amethyst_cluster', bid, GA.token_tex('amethyst_cluster', bid))
    elif k == 'wd_shocker':
        GA.block_model(bid, 'minecraft:block/cross', {'cross': f'block/{bid}'})
        GA.block_model(bid + '_charged', 'minecraft:block/cross', {'cross': f'block/{bid}_charged'})
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'charged=false': {'model': f'{NS}:block/{bid}'}, 'charged=true': {'model': f'{NS}:block/{bid}_charged'}}})
        GA.item_generated(bid, f'block/{bid}_charged')
    elif k == 'wd_cross':
        GA.block_model(bid, 'minecraft:block/cross', {'cross': f'block/{bid}'})
        GA.simple_state(bid)
        if b.get('item', True):
            GA.item_generated(bid, f'block/{bid}')
    elif k == 'wd_puddle':
        # a film of acid one pixel deep over the floor
        GA.write(os.path.join(A, 'models/block', bid + '.json'), {
            'ambientocclusion': False,
            'textures': {'particle': f'{NS}:block/{bid}', 'acid': f'{NS}:block/{bid}'},
            'elements': [{'from': [0, 0, 0], 'to': [16, 0.5, 16], 'faces': {
                'up': {'uv': [0, 0, 16, 16], 'texture': '#acid'},
                'down': {'uv': [0, 0, 16, 16], 'texture': '#acid', 'cullface': 'down'}}}]})
        GA.TEXTURES.add(f'block/{bid}')
        GA.simple_state(bid)
    elif k == 'wd_tendril':
        variants = {}
        for hanging in (True, False):
            for tip in (True, False):
                name = f"{bid}_{'hang' if hanging else 'up'}_{'tip' if tip else 'body'}"
                GA.block_model(name, 'minecraft:block/cross', {'cross': f'block/{name}'})
                variants[f'hanging={str(hanging).lower()},tip={str(tip).lower()}'] = {'model': f'{NS}:block/{name}'}
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': variants})
        GA.item_generated(bid, f'block/{bid}_up_tip')
    elif k == 'wd_grasper':
        for active in (False, True):
            sfx = '_active' if active else ''
            feelers = [{'from': [0.8, 5, 8], 'to': [15.2, 16 if active else 13, 8],
                        'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
                        'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#feelers'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#feelers'}}},
                       {'from': [8, 5, 0.8], 'to': [8, 16 if active else 13, 15.2],
                        'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
                        'faces': {'west': {'uv': [0, 0, 16, 16], 'texture': '#feelers'}, 'east': {'uv': [0, 0, 16, 16], 'texture': '#feelers'}}}]
            pod = {'from': [2, 0, 2], 'to': [14, 6, 14], 'faces': {
                'down': {'uv': [2, 2, 14, 14], 'texture': '#bottom', 'cullface': 'down'},
                'up': {'uv': [2, 2, 14, 14], 'texture': '#top'},
                'north': {'uv': [2, 10, 14, 16], 'texture': '#side'}, 'south': {'uv': [2, 10, 14, 16], 'texture': '#side'},
                'west': {'uv': [2, 10, 14, 16], 'texture': '#side'}, 'east': {'uv': [2, 10, 14, 16], 'texture': '#side'}}}
            lip = {'from': [3, 6, 3], 'to': [13, 7, 13], 'faces': {
                'up': {'uv': [3, 3, 13, 13], 'texture': '#top'},
                'north': {'uv': [3, 10, 13, 11], 'texture': '#side'}, 'south': {'uv': [3, 10, 13, 11], 'texture': '#side'},
                'west': {'uv': [3, 10, 13, 11], 'texture': '#side'}, 'east': {'uv': [3, 10, 13, 11], 'texture': '#side'}}}
            m = {'textures': {'particle': f'{NS}:block/{bid}_side', 'side': f'{NS}:block/{bid}_side', 'bottom': f'{NS}:block/{bid}_bottom',
                              'top': f'{NS}:block/{bid}_top{sfx}', 'feelers': f'{NS}:block/{bid}_feelers{sfx}'},
                 'elements': [pod, lip] + feelers}
            GA.note_textures(m)
            GA.write(os.path.join(A, 'models/block', bid + sfx + '.json'), m)
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'active=false': {'model': f'{NS}:block/{bid}'}, 'active=true': {'model': f'{NS}:block/{bid}_active'}}})
        GA.item_block(bid)
    else:
        raise ValueError(f'no W-deep asset rule for {bid} ({k})')


# ============================================================================ data, tags and text (gen_assets.generate)

def assets(GA):
    for c in CRYSTALS:
        GA.tag('block', 'minecraft:speleothems', rl(f'pointed_{c}_crystal'))
        GA.tag('block', f'{NS}:crystal_dripstone', rl(f'{c}_crystal_dripstone'))
        GA.tag('block', 'minecraft:crystal_sound_blocks', rl(f'{c}_crystal_dripstone'))
        # four pointed crystals pack back into a block, like dripstone
        GA.shaped(f'{c}_crystal_dripstone', ['##', '##'], {'#': f'pointed_{c}_crystal'}, f'{c}_crystal_dripstone')
    GA.tag('block', 'minecraft:supports_big_dripleaf', rl('lumen_moss_block'))
    GA.tag('block', 'minecraft:climbable', rl('sculk_tendril'))
    for b in CAVE_BIOMES:
        GA.tag('worldgen/biome', f'{NS}:is_sift', rl(b))
    # damage types: acid eats armour as it burns; the Sculk Grasper's grip
    for name, msg in (('acid', 'acid'), ('sculk_grasp', 'sculkGrasp')):
        GA.write(os.path.join(GA.RES, 'data', NS, 'damage_type', name + '.json'),
                 {'exhaustion': 0.1, 'message_id': f'{NS}.{msg}', 'scaling': 'when_caused_by_living_non_player'})
    # particles
    for p, n in (('acid_drip', 1), ('acid_fizz', 3)):
        texs = [f'{NS}:{p}_{i}' if n > 1 else f'{NS}:{p}' for i in range(n)]
        GA.write(os.path.join(GA.A, 'particles', p + '.json'), {'textures': texs})
        for t in texs:
            GA.TEXTURES.add('particle/' + t.split(':')[1])
    GA.LANG.update(lang())


def lang():
    c = f'codex.{NS}'
    L = {
        f'biome.{NS}.sift_caves': 'Sift Caves', f'biome.{NS}.cave_jungle': 'Cave Jungle', f'biome.{NS}.sculk_caves': 'Sculk Caves',
        f'death.attack.{NS}.acid': '%1$s dissolved in acid',
        f'death.attack.{NS}.acid.player': '%1$s dissolved in acid while fighting %2$s',
        f'death.attack.{NS}.sculkGrasp': '%1$s was dragged into the sculk',
        f'death.attack.{NS}.sculkGrasp.player': '%1$s was dragged into the sculk while fighting %2$s',
        f'{c}.sift_caves.title': 'Sift Caves', f'{c}.sift_caves.tagline': 'Bare stone, bright ore, singing crystal',
        f'{c}.sift_caves.body': ('Under every meadow and dune the Sift is hollow. Its caves are bare pale stone, seamed with Tuff, Calcite '
                                 'and creamy Magnesite, and rich in ore: Copper everywhere - scattered, in fat pockets and in long veins '
                                 'threaded through the Magnesite - with Bauxite, Galena and Prism among it. Glow lichen lights the walls. '
                                 'In places the rock has grown crystal: rose, azure or amber Crystal Dripstone, hung with pointed '
                                 'crystals and crusted with glowing clusters. Like dripstone they grow, slowly, under a block of their '
                                 'own kind - and like dripstone they fall when you cut them loose. Do not stand under one.'),
        f'{c}.cave_jungle.title': 'Cave Jungle', f'{c}.cave_jungle.tagline': 'Warm, wet and hungry',
        f'{c}.cave_jungle.body': ('Beneath the warm, rainy lands the caves are choked with life. Glowing Lumen Moss carpets floor and '
                                  'ceiling, Glowbell vines and creepers hang in curtains, spore blossoms drift and dripleaf grows in '
                                  'clay-bottomed pools. Mind the Shockers: brush one and its bulb discharges into you, numbing your legs '
                                  'for a moment before it recharges. Acid Weepers hang from the ceiling, dripping acid that pools on '
                                  'the floor below - it burns, and eats through armour. Tall stalks mark the places where the jungle\'s '
                                  'greatest plants take root.'),
        f'{c}.sculk_caves.title': 'Sculk Caves', f'{c}.sculk_caves.tagline': 'The sculk is awake down here',
        f'{c}.sculk_caves.body': ('Under the cold lands the sculk has eaten the caves whole. Its floors and ceilings breathe with light '
                                  'that runs along the veins of Writhing Sculk; tendrils sway from the roof with nothing to move them, and '
                                  'still pools of Sculk Water lie where other caves would have lava. Sensors listen, shriekers wait. '
                                  'Worst are the Sculk Graspers: squat pods that lash out a tendril at anyone who comes near, catch hold '
                                  'and drag them in to be chewed. Strike the pod, or struggle (keep moving) to tear free. Jailers, '
                                  'Sculklings and Sculk Parasites hunt here.'),
        f'{c}.crystal_dripstone.title': 'Crystal Dripstone', f'{c}.crystal_dripstone.tagline': 'Rose, azure and amber',
        f'{c}.crystal_dripstone.body': ('Glassy, glowing rock grown in the Sift Caves and the Deep Sift. Pointed crystals hang from it and '
                                        'rise to meet them, and grow a little at a time when they hang from a block of crystal dripstone. '
                                        'Break the block above one and it falls, point first - deadly from a height - and landing on an '
                                        'upright one hurts as much as any stalagmite. Four pointed crystals pack back into a block; the '
                                        'clusters that crust the crystal floors make fine lamps.'),
    }
    return L


# ============================================================================ worldgen (gen_world)

def ores(GW):
    """Before gen_world.biomes(): vanilla Copper ore scattered through the stone of every Sift biome (COMMON_UNDERGROUND)."""
    GW.feature('ore_copper_sift', {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': 10, 'targets': [
        {'state': GW.state('minecraft:copper_ore'), 'target': {'predicate_type': 'minecraft:block_match', 'block': GW.rl('dreamstone')}},
        {'state': GW.state('minecraft:deepslate_copper_ore'), 'target': {'predicate_type': 'minecraft:block_match', 'block': GW.rl('hushslate')}}]})
    GW.placed('ore_copper_sift', 'ore_copper_sift', [GW.count(12), {'type': 'minecraft:in_square'}, _height(-16, 112, 'trapezoid'), GW.BIOME])
    GW.COMMON_UNDERGROUND.append((6, 'ore_copper_sift'))


def _height(lo, hi, kind='uniform'):
    def anchor(v):
        return v if isinstance(v, dict) else {'absolute': v}
    return {'type': 'minecraft:height_range', 'height': {'type': f'minecraft:{kind}', 'max_inclusive': anchor(hi), 'min_inclusive': anchor(lo)}}


SQ = {'type': 'minecraft:in_square'}
BOTTOM = {'above_bottom': 6}


def _scan(direction, target, steps=12):
    return {'type': 'minecraft:environment_scan', 'allowed_search_condition': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
            'direction_of_search': direction, 'max_steps': steps, 'target_condition': target}


def _floor():
    return [_scan('down', {'type': 'minecraft:solid'}), {'type': 'minecraft:offset', 'x': 0, 'y': 1, 'z': 0}]


def _ceiling():
    return [_scan('up', {'type': 'minecraft:has_sturdy_face', 'direction': 'down'}), {'type': 'minecraft:offset', 'x': 0, 'y': -1, 'z': 0}]


def _crystals(GW):
    st, feature, placed, count, rarity, BIOME = GW.state, GW.feature, GW.placed, GW.count, GW.rarity, GW.BIOME
    vanilla = json.load(open(os.path.join(GW.VD, 'worldgen/feature/dripstone_cluster.json')))
    names = {}
    for c in CRYSTALS:
        cl = copy.deepcopy(vanilla)
        cl['base_block'] = rl(f'{c}_crystal_dripstone')
        cl['pointed_block'] = rl(f'pointed_{c}_crystal')
        cl['radius'] = {'type': 'minecraft:uniform', 'max_inclusive': 6, 'min_inclusive': 2}
        cl['height'] = {'type': 'minecraft:uniform', 'max_inclusive': 5, 'min_inclusive': 2}
        cl['wetness'] = {'type': 'minecraft:clamped_normal', 'deviation': 0.1, 'max': 0.3, 'mean': 0.05, 'min': 0.0}
        feature(f'crystal_cluster_{c}', cl)
        placed(f'crystal_cluster_{c}', f'crystal_cluster_{c}', [rarity(3), count(40), SQ, _height(BOTTOM, 120), BIOME])
        spike = {'type': 'minecraft:speleothem', 'base_block': rl(f'{c}_crystal_dripstone'), 'pointed_block': rl(f'pointed_{c}_crystal'),
                 'replaceable_blocks': '#minecraft:dripstone_replaceable_blocks'}
        feature(f'crystal_spike_{c}', {'type': 'minecraft:simple_random_selector', 'features': [
            {'feature': spike, 'placement': [_scan('down', {'type': 'minecraft:solid'}), {'type': 'minecraft:offset', 'x': 0, 'y': 1, 'z': 0}]},
            {'feature': spike, 'placement': [_scan('up', {'type': 'minecraft:solid'}), {'type': 'minecraft:offset', 'x': 0, 'y': -1, 'z': 0}]}]})
        placed(f'crystal_spikes_{c}', f'crystal_spike_{c}', [
            rarity(3), count({'type': 'minecraft:uniform', 'min_inclusive': 24, 'max_inclusive': 48}), SQ, _height(BOTTOM, 120),
            count({'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 3}),
            {'type': 'minecraft:offset', 'x': {'type': 'minecraft:clamped_normal', 'deviation': 3.0, 'max_inclusive': 8, 'mean': 0.0, 'min_inclusive': -8},
             'y': {'type': 'minecraft:clamped_normal', 'deviation': 0.6, 'max_inclusive': 2, 'mean': 0.0, 'min_inclusive': -2},
             'z': {'type': 'minecraft:clamped_normal', 'deviation': 3.0, 'max_inclusive': 8, 'mean': 0.0, 'min_inclusive': -8}}, BIOME])
        # crystal gardens: a crust of crystal dripstone on the floor (or ceiling) bristling with clusters and little spikes
        for surf, facing, tip in (('floor', 'up', 'up'), ('ceiling', 'down', 'down')):
            feature(f'crystal_garden_plants_{c}_{surf}', {'type': 'minecraft:simple_block', 'to_place': GW.weighted([
                (st(f'{c}_crystal_cluster', facing=facing, waterlogged=False), 4),
                (st(f'pointed_{c}_crystal', vertical_direction=tip, thickness='tip', waterlogged=False), 2)])})
            feature(f'crystal_garden_{c}_{surf}', {
                'type': 'minecraft:vegetation_patch', 'depth': 1, 'extra_bottom_block_chance': 0.0, 'extra_edge_column_chance': 0.3,
                'ground_state': st(f'{c}_crystal_dripstone'), 'replaceable': '#minecraft:dripstone_replaceable_blocks', 'surface': surf,
                'vegetation_chance': 0.25, 'vegetation_feature': {'feature': rl(f'crystal_garden_plants_{c}_{surf}'), 'placement': []},
                'vertical_range': 4, 'xz_radius': {'type': 'minecraft:uniform', 'max_inclusive': 4, 'min_inclusive': 2}})
            placed(f'crystal_garden_{c}_{surf}', f'crystal_garden_{c}_{surf}', [
                rarity(4), count(6), SQ, _height(BOTTOM, 120)] + (_floor() if surf == 'floor' else _ceiling()) + [BIOME])
        names[c] = [(7, f'crystal_cluster_{c}'), (7, f'crystal_spikes_{c}'), (9, f'crystal_garden_{c}_floor'), (9, f'crystal_garden_{c}_ceiling')]
    return names


def _rock_and_ore(GW):
    st, feature, placed, count, BIOME = GW.state, GW.feature, GW.placed, GW.count, GW.BIOME

    def ore(name, size, pairs, air=0.0):
        feature(name, {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': air, 'size': size, 'targets': [
            {'state': st(s), 'target': {'predicate_type': 'minecraft:block_match', 'block': GW.rl(b)}} for b, s in pairs]})
    # fat copper pockets (vanilla's "large copper", as in dripstone caves)
    ore('ore_copper_cave_rich', 20, [('dreamstone', 'minecraft:copper_ore'), ('hushslate', 'minecraft:deepslate_copper_ore')])
    placed('ore_copper_cave_rich', 'ore_copper_cave_rich', [count(10), SQ, _height(-16, 96, 'trapezoid'), BIOME])
    # more of F1's Bauxite, Galena and Magnesite in the caves (their own configured features, placed again)
    placed('cave_ore_bauxite', 'ore_bauxite', [count(4), SQ, _height(-32, 80), BIOME])
    placed('cave_ore_galena', 'ore_galena', [count(4), SQ, _height(-64, 48, 'trapezoid'), BIOME])
    placed('cave_ore_magnesite', 'ore_magnesite', [count(3), SQ, _height(-40, 96), BIOME])
    # varied stone: tuff blobs below, calcite seams above
    ore('ore_cave_tuff', 40, [('dreamstone', 'minecraft:tuff'), ('hushslate', 'minecraft:tuff')])
    placed('ore_cave_tuff', 'ore_cave_tuff', [count(2), SQ, _height(BOTTOM, 24), BIOME])
    ore('ore_cave_calcite', 32, [('dreamstone', 'minecraft:calcite')])
    placed('ore_cave_calcite', 'ore_cave_calcite', [count(2), SQ, _height(0, 110), BIOME])
    stones = [GW.rl(b) for b in ('dreamstone', 'hushslate', 'cobbled_hushslate', 'magnesite')] + [
        'minecraft:tuff', 'minecraft:calcite'] + [GW.rl(f'{c}_crystal_dripstone') for c in CRYSTALS]
    feature('cave_glow_lichen', {'type': 'minecraft:multiface_growth', 'block': 'minecraft:glow_lichen', 'can_be_placed_on': stones,
                                 'can_place_on_ceiling': True, 'can_place_on_wall': True, 'search_range': 20})
    placed('cave_glow_lichen', 'cave_glow_lichen', [count(56), _height(BOTTOM, 160), SQ,
                                                    {'type': 'minecraft:surface_relative_threshold_filter', 'heightmap': 'OCEAN_FLOOR_WG',
                                                     'max_inclusive': -13}, BIOME])


def _jungle(GW):
    st, feature, placed, count, rarity, BIOME = GW.state, GW.feature, GW.placed, GW.count, GW.rarity, GW.BIOME
    on_moss = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
        {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
        {'type': 'minecraft:matching_blocks', 'blocks': [GW.rl('lumen_moss_block'), 'minecraft:moss_block'], 'offset': [0, -1, 0]}]}}
    feature('jungle_floor_plants', {'type': 'minecraft:simple_block', 'to_place': GW.weighted([
        (st('glimmer_sprouts'), 6), (st('lumen_moss_carpet'), 3), (st('glowcap'), 2), (st('shocker_plant', charged=True), 3),
        (st('echo_orchid'), 1), (st('minecraft:fern'), 3), (st('minecraft:short_grass'), 2)])})
    placed('jungle_floor_plants', 'jungle_floor_plants', [count(96), SQ, _height(BOTTOM, 120)] + _floor() + [on_moss, BIOME])
    placed('jungle_glowbell_vine', 'glowbell_vine', [count(64), SQ, _height(BOTTOM, 120)] + _ceiling() + [BIOME])
    # Acid Weepers on the ceiling; the acid they drip pools on the floor straight below
    feature('jungle_acid_weeper', {'type': 'minecraft:sequence', 'features': [
        {'feature': {'type': 'minecraft:simple_block', 'to_place': st('acid_weeper')}, 'placement': []},
        {'feature': {'type': 'minecraft:simple_block', 'to_place': st('acid_puddle')}, 'placement': [
            {'type': 'minecraft:offset', 'x': 0, 'y': -1, 'z': 0}, _scan('down', {'type': 'minecraft:solid'}, 16),
            {'type': 'minecraft:offset', 'x': 0, 'y': 1, 'z': 0},
            {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}}]}]})
    placed('jungle_acid_weeper', 'jungle_acid_weeper', [count(14), SQ, _height(BOTTOM, 120)] + _ceiling() + [BIOME])
    # the giant carnivorous plants' places (Phase 4: the Pitcher Plants replace this configured feature); a tall dripleaf stalk for now
    cols = []
    for f in ('north', 'east', 'south', 'west'):
        cols.append({'feature': {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
                                 'direction': 'up', 'prioritize_tip': True, 'layers': [
                                     {'height': {'type': 'minecraft:uniform', 'min_inclusive': 2, 'max_inclusive': 5},
                                      'provider': st('minecraft:big_dripleaf_stem', facing=f, waterlogged=False)},
                                     {'height': 1, 'provider': st('minecraft:big_dripleaf', facing=f, tilt='none', waterlogged=False)}]},
                     'placement': []})
    feature('cave_jungle_pitcher_spot', {'type': 'minecraft:simple_random_selector', 'features': cols})
    placed('cave_jungle_pitcher_spot', 'cave_jungle_pitcher_spot', [count(3), SQ, _height(BOTTOM, 120)] + _floor() + [on_moss, BIOME])
    return [(9, 'jungle_floor_plants'), (9, 'jungle_glowbell_vine'), (9, 'jungle_acid_weeper'), (9, 'cave_jungle_pitcher_spot'),
            (9, 'minecraft:spore_blossom'), (9, 'minecraft:lush_caves_clay'), (9, 'minecraft:classic_vines_cave_feature')]


def _sculk(GW):
    st, feature, placed, count, rarity, BIOME = GW.state, GW.feature, GW.placed, GW.count, GW.rarity, GW.BIOME
    feature('sculk_caves_patch', {'type': 'minecraft:sequence', 'features': [
        {'feature': {'type': 'minecraft:sculk_patch', 'amount_per_charge': 24, 'charge_count': 8, 'growth_rounds': 0, 'spread_attempts': 48,
                     'spread_rounds': 1}, 'placement': []},
        {'feature': {'type': 'minecraft:simple_block', 'to_place': st('minecraft:sculk_catalyst', bloom=False)},
         'placement': [{'type': 'minecraft:random_chance', 'chance': 0.4},
                       {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:has_sturdy_face', 'direction': 'up',
                                                                                 'offset': [0, -1, 0]}}]}]})
    placed('sculk_caves_patch', 'sculk_caves_patch', [count(64), SQ, _height(BOTTOM, 120), BIOME])
    feature('sculk_caves_vein', {'type': 'minecraft:multiface_growth', 'block': 'minecraft:sculk_vein', 'can_be_placed_on': [
        GW.rl('dreamstone'), GW.rl('hushslate'), GW.rl('cobbled_hushslate'), GW.rl('sculk_mud'), GW.rl('writhing_sculk'), 'minecraft:sculk',
        'minecraft:tuff', 'minecraft:calcite'], 'can_place_on_ceiling': True, 'can_place_on_floor': True, 'can_place_on_wall': True,
        'chance_of_spreading': 1.0, 'search_range': 20})
    placed('sculk_caves_vein', 'sculk_caves_vein', [count({'type': 'minecraft:uniform', 'min_inclusive': 120, 'max_inclusive': 180}), SQ,
                                                    _height(BOTTOM, 120), BIOME])
    # writhing sculk: a share of every sculk surface, pulsing with light
    feature('ore_writhing_sculk', {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': 24, 'targets': [
        {'state': st('writhing_sculk'), 'target': {'predicate_type': 'minecraft:block_match', 'block': 'minecraft:sculk'}}]})
    placed('ore_writhing_sculk', 'ore_writhing_sculk', [count(28), SQ, _height(BOTTOM, 120), BIOME])
    for hanging in (True, False):
        name = 'sculk_tendrils_' + ('hanging' if hanging else 'standing')
        feature(name, {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
                       'direction': 'down' if hanging else 'up', 'prioritize_tip': True, 'layers': [
                           {'height': {'type': 'minecraft:uniform', 'min_inclusive': 0, 'max_inclusive': 3 if hanging else 2},
                            'provider': st('sculk_tendril', hanging=hanging, tip=False)},
                           {'height': 1, 'provider': st('sculk_tendril', hanging=hanging, tip=True)}]})
        placed(name, name, [count(40 if hanging else 28), SQ, _height(BOTTOM, 120)] + (_ceiling() if hanging else [
            _scan('down', {'type': 'minecraft:has_sturdy_face', 'direction': 'up'}), {'type': 'minecraft:offset', 'x': 0, 'y': 1, 'z': 0}]) + [BIOME])
    feature('sculk_grasper', {'type': 'minecraft:simple_block', 'to_place': st('sculk_grasper', active=False)})
    placed('sculk_grasper', 'sculk_grasper', [count(6), SQ, _height(BOTTOM, 120),
                                              _scan('down', {'type': 'minecraft:has_sturdy_face', 'direction': 'up'}),
                                              {'type': 'minecraft:offset', 'x': 0, 'y': 1, 'z': 0},
                                              {'type': 'minecraft:block_predicate_filter', 'predicate': {
                                                  'type': 'minecraft:matching_blocks', 'offset': [0, -1, 0], 'blocks': [
                                                      'minecraft:sculk', GW.rl('writhing_sculk'), GW.rl('sculk_mud'), GW.rl('hushslate'),
                                                      GW.rl('dreamstone')]}}, BIOME])
    # Sculk Water lakes where other caves would have lava
    not_protected = {'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:features_cannot_replace'}}
    feature('sculk_lake', {'type': 'minecraft:lake', 'barrier': st('minecraft:sculk'), 'can_place_feature': {'type': 'minecraft:true'},
                           'can_replace_with_air_or_fluid': not_protected,
                           'can_replace_with_barrier': {'type': 'minecraft:not', 'predicate': {
                               'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:lava_pool_stone_cannot_replace'}},
                           'fluid': st('sculk_water', level=0)})
    placed('sculk_lake_underground', 'sculk_lake', [
        rarity(3), SQ, _height(BOTTOM, 96),
        {'type': 'minecraft:environment_scan', 'direction_of_search': 'down', 'max_steps': 32, 'target_condition': {
            'type': 'minecraft:all_of', 'predicates': [{'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_block_tag',
                                                                                             'tag': 'minecraft:air'}},
                                                     {'type': 'minecraft:inside_world_bounds', 'offset': [0, -5, 0]}]}},
        {'type': 'minecraft:surface_relative_threshold_filter', 'heightmap': 'OCEAN_FLOOR_WG', 'max_inclusive': -5}, BIOME])
    return [(1, 'sculk_lake_underground'), (7, 'sculk_caves_patch'), (8, 'ore_writhing_sculk'), (9, 'sculk_caves_vein'),
            (9, 'sculk_tendrils_hanging'), (9, 'sculk_tendrils_standing'), (9, 'sculk_grasper')]


def _carvers(GW):
    cave = json.load(open(os.path.join(GW.VD, 'worldgen/carver/cave.json')))
    jungle = copy.deepcopy(cave)
    # big round halls for the jungle to fill
    jungle['probability'] = 0.2
    jungle['horizontal_radius_multiplier'] = {'type': 'minecraft:uniform', 'min_inclusive': 1.2, 'max_exclusive': 2.3}
    jungle['vertical_radius_multiplier'] = {'type': 'minecraft:uniform', 'min_inclusive': 1.0, 'max_exclusive': 1.8}
    jungle['y'] = {'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 8}, 'max_inclusive': {'absolute': 110}}
    GW.w('worldgen/carver/jungle_cave', jungle)
    # wide, low burrows: the sculk's tunnels press in on you
    burrow = copy.deepcopy(cave)
    burrow['probability'] = 0.18
    burrow['horizontal_radius_multiplier'] = {'type': 'minecraft:uniform', 'min_inclusive': 1.0, 'max_exclusive': 1.8}
    burrow['vertical_radius_multiplier'] = {'type': 'minecraft:uniform', 'min_inclusive': 0.5, 'max_exclusive': 0.9}
    burrow['y'] = {'type': 'minecraft:uniform', 'min_inclusive': {'above_bottom': 8}, 'max_inclusive': {'absolute': 90}}
    GW.w('worldgen/carver/sculk_burrow', burrow)


def _biomes(GW, crystals, jungle, sculk):
    mobs, particles = GW.mobs, GW.particles
    cu = list(GW.COMMON_UNDERGROUND)
    rock = [(6, 'ore_copper_cave_rich'), (6, 'cave_ore_bauxite'), (6, 'cave_ore_galena'), (6, 'cave_ore_magnesite'), (6, 'ore_cave_tuff'),
            (6, 'ore_cave_calcite'), (9, 'cave_glow_lichen')]
    GW.biome('sift_caves', fog='#8bb4b0', sky='#3d6670', water='#4fd0e0', grass='#63d6c6', foliage='#6fe2dc', temp=0.6, down=0.4,
             spawns=mobs(), parts=particles(('glow_dust', 0.004), ('star_sparkle', 0.0012), ('drifting_soul', 0.0006)),
             music=f'{NS}:music.deep_sift', ambient_loop=f'{NS}:ambient.deep_sift.loop',
             feats=cu + rock + [f for c in CRYSTALS for f in crystals[c]])
    GW.biome('cave_jungle', fog='#4aa58a', sky='#2b6655', water='#3fd8b0', grass='#4fc98a', foliage='#3fb87a', temp=0.95, down=1.0,
             spawns=mobs(), parts=particles(('minecraft:spore_blossom_air', 0.004), ('glow_dust', 0.006), ('dream_pollen', 0.002),
                                            ('sift_mist', 0.0012)),
             music='minecraft:music.overworld.lush_caves', ambient_loop=f'{NS}:ambient.deep_sift.loop',
             feats=cu + [(6, 'ore_copper_cave_rich'), (6, 'cave_ore_bauxite'), (9, 'cave_glow_lichen')] + jungle)
    GW.biome('sculk_caves', fog='#0f2e33', sky='#123a40', water='#0f5258', grass='#2e6b66', foliage='#2a5f5c', temp=0.2, down=0.5,
             spawns=mobs(monster=[('sculk_parasite', 30, 1, 2), ('strumling', 8, 1, 1)]),
             parts=particles(('minecraft:sculk_soul', 0.0015), ('minecraft:sculk_charge_pop', 0.0025), ('glow_dust', 0.003), ('drifting_soul', 0.0015)),
             music='minecraft:music.overworld.deep_dark', ambient_loop=f'{NS}:ambient.sculk_swamp.loop',
             feats=cu + [(6, 'cave_ore_galena'), (6, 'ore_cave_tuff')] + sculk)
    # the Deep Sift: deep hushslate, tuff and sculk, geodes, azure crystal and a few glowbells - no more moss carpet
    GW.biome('deep_sift', fog='#1a2f3f', sky='#223a5a', water='#3fc8d8', grass='#2f8f9e', foliage='#37a9b5', temp=0.5, down=0.4,
             spawns=mobs(creature=[('sifter', 5, 1, 1)]),  # CR1: a few lost bells in the dark
             parts=particles(('glow_dust', 0.006), ('drifting_soul', 0.0015)),
             music=f'{NS}:music.deep_sift', ambient_loop=f'{NS}:ambient.deep_sift.loop',
             feats=cu + [(2, 'minecraft:amethyst_geode'), (6, 'ore_copper_cave_rich'), (6, 'ore_cave_tuff'), (7, 'minecraft:sculk_patch_deep_dark'),
                         (9, 'sculk_caves_vein'), (9, 'cave_glow_lichen'), (9, 'deep_glowbell_vine')] + crystals['azure'])

    def patch(name, fn):
        path = os.path.join(GW.D, f'worldgen/biome/{name}.json')
        obj = json.load(open(path))
        fn(obj)
        GW.w(f'worldgen/biome/{name}', obj)

    def jungle_bits(b):
        b['carvers'] = [rl('jungle_cave'), 'minecraft:cave', 'minecraft:canyon']
        b['has_precipitation'] = False

    def sculk_bits(b):
        b['carvers'] = [rl('sculk_burrow'), 'minecraft:cave', 'minecraft:cave_extra_underground']
        a = b['attributes']
        a['minecraft:audio/ambient_sounds']['additions'] = {'sound': f'{NS}:ambient.sculk_swamp.additions', 'tick_chance': 0.016}
        a['minecraft:audio/ambient_sounds']['mood'] = {'block_search_extent': 8, 'offset': 2.0, 'sound': f'{NS}:ambient.sculk_swamp.mood',
                                                      'tick_delay': 3000}
        a['minecraft:visual/water_fog_color'] = '#06272b'
    patch('cave_jungle', jungle_bits)
    patch('sculk_caves', sculk_bits)


# ------------------------------------------------------------------ surface rules

def _rules(GW):
    def biome_is(*b):
        return {'type': 'minecraft:biome', 'biome_is': [rl(x) for x in b]}

    def cond(c, then):
        return {'type': 'minecraft:condition', 'if_true': c, 'then_run': then}

    def block(b, **props):
        return {'type': 'minecraft:block', 'result_state': GW.state(b, **props)}

    def seq(*r):
        return {'type': 'minecraft:sequence', 'sequence': list(r)}

    def noise(name, lo, hi=1.0):
        return {'type': 'minecraft:noise_threshold', 'noise': name, 'min_threshold': lo, 'max_threshold': hi}
    sculk = cond(biome_is('sculk_caves'), seq(
        cond('minecraft:on_floor', seq(cond(noise('minecraft:surface', -0.45), block('minecraft:sculk')),
                                       cond(noise('minecraft:patch', -0.1), block('sculk_mud')))),
        cond('minecraft:on_ceiling', cond(noise('minecraft:surface_secondary', -0.15), block('minecraft:sculk')))))
    jungle = cond(biome_is('cave_jungle'), seq(
        cond('minecraft:on_floor', seq(cond(noise('minecraft:surface', -0.55), block('lumen_moss_block')), block('minecraft:rooted_dirt'))),
        cond('minecraft:on_ceiling', cond(noise('minecraft:surface_secondary', -0.2), block('lumen_moss_block')))))
    deep = cond(biome_is('deep_sift'), cond('minecraft:on_floor', seq(
        cond(noise('minecraft:gravel', 0.45), block('minecraft:tuff')), cond(noise('minecraft:patch', 0.55), block('cobbled_hushslate')))))
    return [sculk, jungle, deep], cond(biome_is(*UNDERGROUND), seq(sculk, jungle, deep, cond('minecraft:on_floor', block('dreamstone')),
                                                                  cond('minecraft:under_floor', block('dreamstone'))))


def _material_rule(GW):
    path = os.path.join(GW.D, 'worldgen/material_rule/the_sift.json')
    rule = json.load(open(path))
    underground_rules, above = _rules(GW)
    top = rule['sequence']
    # vanilla's great copper ore veins, run through Magnesite here (y 0-50, every biome, before any surface rule)
    GW.w('worldgen/material_rule/caves/copper_vein', {
        'type': 'minecraft:ore_vein', 'density': 'minecraft:overworld/ore_vein/copper_density', 'filler_block': rl('magnesite'),
        'filler_gap': 'minecraft:overworld/ore_vein/gap', 'ore_block': 'minecraft:copper_ore', 'raw_ore_block': 'minecraft:raw_copper_block',
        'raw_ore_chance': 0.02, 'richness': 'minecraft:overworld/ore_vein/richness'})
    top.insert(1, rl('caves/copper_vein'))
    for r in top:
        if isinstance(r, dict) and r.get('if_true', {}).get('type') == 'minecraft:above_preliminary_surface':
            # cave biomes never take the grass/turf/soil of the surface rules, even right under the surface
            r['then_run']['sequence'].insert(0, above)
    underground = top[-1]
    assert underground.get('type') == 'minecraft:sequence', 'the_sift material rule: underground sequence expected last'
    # the Deep Sift's moss carpet goes (that was the "grass" covering the deep caves); our cave floors and ceilings come first
    underground['sequence'] = underground_rules + [r for r in underground['sequence']
                                                   if rl('deep_sift') not in json.dumps(r.get('if_true', {}) if isinstance(r, dict) else '')]
    GW.w('worldgen/material_rule/the_sift', rule)


# ------------------------------------------------------------------ biome placement by depth

def _subtract(a, b):
    """Boxes covering box a minus box b, over AXES (shared faces do not count as overlap)."""
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


def _box(par):
    return {k: (list(par[k]) if isinstance(par[k], list) else [par[k], par[k]]) for k in AXES}


def _placement(dim):
    pts = dim['generator']['biome_source']['biomes']
    out, holes = [], []
    for p in pts:
        par = p['parameters']
        if p['biome'] in (rl('deep_sift'),):
            pieces = [_box(par)]
            for b in BOXES.values():
                pieces = [q for piece in pieces for q in _subtract(piece, b)]
            out += [{'biome': p['biome'], 'parameters': {**par, **piece}} for piece in pieces]
            continue
        if isinstance(par.get('depth'), list) and p['biome'] not in (rl(b) for b in SKY):
            holes.append(_box(par))  # the Caravans Cavern (and any other cave biome) keeps its slice
        out.append(p)
    for name, b in BOXES.items():
        out.append({'biome': rl(name), 'parameters': {**{k: list(v) for k, v in b.items()}, 'offset': 0.0}})
    layer = [dict(CAVE_LAYER)]
    for h in holes + list(BOXES.values()):
        layer = [q for piece in layer for q in _subtract(piece, h)]
    out += [{'biome': rl('sift_caves'), 'parameters': {**piece, 'offset': 0.0}} for piece in layer]
    dim['generator']['biome_source']['biomes'] = out
    return dim


# ------------------------------------------------------------------ no grass in caves

def _scrub(GW):
    GW.feature('cave_scrub', {'type': rl('cave_scrub'), 'cover': [rl(g) for g in GRASSES], 'floor': GW.state('dreamstone'), 'roof': 3,
                              'max_gap': 24, 'depth': 64})
    GW.placed('cave_scrub', 'cave_scrub', [])
    folder = os.path.join(GW.D, 'worldgen/biome')
    for fn in sorted(os.listdir(folder)):
        name = fn[:-5]
        if not fn.endswith('.json') or name in UNDERGROUND or name in SKY:
            continue
        obj = json.load(open(os.path.join(folder, fn)))
        if rl('cave_scrub') not in obj['features'][0]:
            obj['features'][0].append(rl('cave_scrub'))  # last in step 0, after the edge blend, in every surface biome
        GW.w(f'worldgen/biome/{name}', obj)


def world(GW):
    """After every biome exists (last world hook): features, the three cave biomes, the reworked Deep Sift, carvers,
    surface rules, ore veins, the placement by depth and the cave scrub."""
    crystals = _crystals(GW)
    _rock_and_ore(GW)
    GW.placed('deep_glowbell_vine', 'glowbell_vine', [GW.count(24), SQ, _height(BOTTOM, 60)] + _ceiling() + [GW.BIOME])
    jungle = _jungle(GW)
    sculk = _sculk(GW)
    _carvers(GW)
    _biomes(GW, crystals, jungle, sculk)
    _material_rule(GW)
    path = os.path.join(GW.D, 'dimension/the_sift.json')
    GW.w('dimension/the_sift', _placement(json.load(open(path))))
    _scrub(GW)
