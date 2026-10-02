"""Generates The Sift's dimension, terrain, biomes and features (data/thesift/...)."""
import copy
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_assets as GA  # noqa: E402  (reuses write/tag helpers)

NS = 'thesift'
D = os.path.join(GA.RES, 'data', NS)
VD = GA.VD


def w(path, obj):
    GA.write(os.path.join(D, path + '.json'), obj)


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def state(block, **props):
    s = {'id': rl(block)}
    if props:
        s['properties'] = {k: str(v).lower() for k, v in props.items()}
    return s


# ============================================================================ dimension


def dimension():
    w('world_clock/the_sift', {})
    w('timeline/dream_cycle', {
        'clock': f'{NS}:the_sift', 'period_ticks': 12000,
        'tracks': {
            'minecraft:visual/fog_color': {'keyframes': [
                {'ticks': 0, 'value': '#ffffff'}, {'ticks': 3000, 'value': '#ffe3f3'}, {'ticks': 6000, 'value': '#ffffff'},
                {'ticks': 9000, 'value': '#e3f7ff'}], 'modifier': 'multiply'},
            'minecraft:visual/sky_light_color': {'keyframes': [
                {'ticks': 0, 'value': '#ffffff'}, {'ticks': 3000, 'value': '#fff6fb'}, {'ticks': 6000, 'value': '#ffffff'},
                {'ticks': 9000, 'value': '#f4fbff'}], 'modifier': 'multiply'},
        }})
    GA.tag('timeline', f'{NS}:in_sift', '#minecraft:universal')
    GA.tag('timeline', f'{NS}:in_sift', f'{NS}:dream_cycle')
    w('dimension_type/the_sift', {
        'ambient_light': 0.12,
        'attributes': {
            'minecraft:audio/ambient_sounds': {'mood': {'block_search_extent': 8, 'offset': 2.0, 'sound': f'{NS}:ambient.sift.mood', 'tick_delay': 6000}},
            'minecraft:audio/background_music': {'default': {'max_delay': 18000, 'min_delay': 6000, 'replace_current_music': True,
                                                             'sound': f'{NS}:music.sift'}},
            'minecraft:gameplay/bed_rule': {'can_set_spawn': 'always', 'can_sleep': 'never',
                                            'error_message': {'translate': f'message.{NS}.bed.dreaming'}},
            'minecraft:gameplay/respawn_anchor_works': False,
            'minecraft:gameplay/nether_portal_spawns_piglin': False,
            'minecraft:gameplay/sky_light_level': 15.0,
            'minecraft:visual/ambient_light_color': '#161c1e',
            'minecraft:visual/cloud_color': '#00ffffff',
            'minecraft:visual/cloud_height': 236.0,
            'minecraft:visual/fog_color': '#aef0e2',
            'minecraft:visual/sky_color': '#5ed6c6',
            'minecraft:visual/sky_light_color': '#fffdf8',
            'neoforge:custom_skybox': f'{NS}:nebula',
        },
        'coordinate_scale': 1.0,
        'default_clock': f'{NS}:the_sift',
        'has_ceiling': False,
        'has_ender_dragon_fight': False,
        'has_skylight': True,
        'height': 384,
        'infiniburn': '#minecraft:infiniburn_overworld',
        'logical_height': 384,
        'min_y': -64,
        'monster_spawn_block_light_limit': 0,
        'monster_spawn_light_level': 0,
        'timelines': f'#{NS}:in_sift',
    })
    GA.LANG[f'message.{NS}.bed.dreaming'] = 'You are already dreaming.'


# ============================================================================ terrain


def _swap(obj, mapping):
    if isinstance(obj, str):
        return mapping.get(obj, obj)
    if isinstance(obj, list):
        return [_swap(x, mapping) for x in obj]
    if isinstance(obj, dict):
        return {k: _swap(v, mapping) for k, v in obj.items()}
    return obj


def _df(t, **kw):
    return {'type': f'minecraft:{t}', **kw}


def _clamp01(x):
    return _df('clamp', input=x, min=0.0, max=1.0)


def _ramp(x, start, per):
    """0 below `start`, rising by `per` per unit of x, capped at 1."""
    return _clamp01(_df('mul', left=_df('add', left=x, right=-start), right=per))


def terrain_density():
    """Overworld-style terrain with The Sift's own shapes added to the terrain offset.

    Copies of the vanilla offset -> depth -> sloped_cheese -> final_density chain (and the surface
    level estimate built from the offset) point at `thesift:sift/offset`, which is the vanilla offset plus:
      * wind-carved dune crests (ridged noise) wherever the climate makes Rocky Dunes,
      * flat-topped sandstone mesas rising out of those dunes.
    An offset change of 0.01 moves the surface by roughly 1.3 blocks.
    Returns the router entries to use.
    """
    dfs = os.path.join(VD, 'worldgen/density_function/overworld')
    temperature, humidity, continents = 'minecraft:overworld/temperature', 'minecraft:overworld/vegetation', 'minecraft:overworld/continents'
    # 0..1 where the climate matches Rocky Dunes (hot, dry, inland), fading out towards other biomes and coasts
    dunes_mask = _df('mul', left=_df('mul', left=_ramp(temperature, 0.25, 6.0), right=_ramp(_df('mul', left=humidity, right=-1.0), -0.15, 6.0)),
                     right=_ramp(continents, -0.05, 5.0))
    crest = _df('add', left=0.45, right=_df('mul', left=_df('abs', input=_df('noise', noise='minecraft:surface', xz_scale=1.25, y_scale=0.0)),
                                               right=-1.0))
    ripples = _df('noise', noise='minecraft:surface_secondary', xz_scale=5.0, y_scale=0.0)
    mesa = _clamp01(_df('mul', left=_df('add', left=_df('noise', noise='minecraft:pillar_rareness', xz_scale=1.6, y_scale=0.0), right=-0.55),
                         right=9.0))
    shape = _df('add', left=_df('add', left=_df('mul', left=crest, right=0.075), right=_df('mul', left=ripples, right=0.0015)),
                right=_df('mul', left=mesa, right=0.11))
    w('worldgen/density_function/sift/offset', _df('cache', input=_df('add', left='minecraft:overworld/offset', right=_df('mul', left=dunes_mask, right=shape))))
    chain = [('offset', 'depth'), ('depth', 'sloped_cheese'), ('sloped_cheese', 'final_density'), ('offset', 'preliminary_surface_level'),
             ('preliminary_surface_level', 'chunk_surface_level')]
    for ref, name in chain:
        src = json.load(open(os.path.join(dfs, name + '.json')))
        w(f'worldgen/density_function/sift/{name}', _swap(src, {f'minecraft:overworld/{ref}': f'{NS}:sift/{ref}'}))
    return {'final_density': f'{NS}:sift/final_density', 'chunk_surface_level': f'{NS}:sift/chunk_surface_level'}


def noise_settings():
    ov = json.load(open(os.path.join(VD, 'worldgen/noise_settings/overworld.json')))
    ns = copy.deepcopy(ov)
    ns['default_block'] = f'{NS}:dreamstone'
    ns['default_fluid'] = {'Name': f'{NS}:chrome', 'Properties': {'level': '0'}} if isinstance(ov['default_fluid'], dict) else f'{NS}:chrome'
    ns['material_rule'] = f'{NS}:the_sift'
    ns['spawn_target'] = []
    ns['sea_level'] = 63
    ns['aquifers']['lava'] = -1.0
    band = {'type': 'minecraft:mul',
            'left': {'type': 'minecraft:gradient', 'axis': 'y', 'from_coordinate': 176, 'from_value': 0.0, 'to_coordinate': 196, 'to_value': 1.0},
            'right': {'type': 'minecraft:gradient', 'axis': 'y', 'from_coordinate': 206, 'from_value': 1.0, 'to_coordinate': 232, 'to_value': 0.0}}
    islands = {'type': 'minecraft:interpolated', 'cell_size_xz': 4, 'cell_size_y': 8, 'input': {
        'type': 'minecraft:add',
        'left': {'type': 'minecraft:mul', 'left': {'type': 'minecraft:add',
                                                  'left': {'type': 'minecraft:noise', 'noise': 'minecraft:cave_cheese', 'xz_scale': 0.45, 'y_scale': 0.9},
                                                  'right': -0.42},
                 'right': band},
        'right': {'type': 'minecraft:mul', 'left': {'type': 'minecraft:add', 'left': band, 'right': -1.0}, 'right': 1.5}}}
    router = terrain_density()
    ns['noise_router']['final_density'] = {'type': 'minecraft:max', 'left': router['final_density'], 'right': islands}
    ns['noise_router']['chunk_surface_level'] = router['chunk_surface_level']
    w('worldgen/noise_settings/the_sift', ns)
    # surface rules
    grass = {'type': 'minecraft:block', 'result_state': state('sift_grass_block', snowy=False)}
    turf = {'type': 'minecraft:block', 'result_state': state('coral_turf', snowy=False)}
    soil = {'type': 'minecraft:block', 'result_state': state('sift_soil')}
    sand = {'type': 'minecraft:block', 'result_state': state('dreamsand')}
    sandstone = {'type': 'minecraft:block', 'result_state': state('dreamsandstone')}
    dreamstone = {'type': 'minecraft:block', 'result_state': state('dreamstone')}
    cobbled = {'type': 'minecraft:block', 'result_state': state('cobbled_dreamstone')}
    moss = {'type': 'minecraft:block', 'result_state': state('lumen_moss_block')}

    def biome_is(*b):
        return {'type': 'minecraft:biome', 'biome_is': [rl(x) for x in b]}

    def cond(c, then):
        return {'type': 'minecraft:condition', 'if_true': c, 'then_run': then}

    def seq(*rules):
        return {'type': 'minecraft:sequence', 'sequence': list(rules)}

    high = {'type': 'minecraft:y_above', 'add_stone_depth': True, 'anchor': {'absolute': 150}, 'surface_depth_multiplier': 0}
    noise_patch = {'type': 'minecraft:noise_threshold', 'max_threshold': 1.0, 'min_threshold': 0.25, 'noise': 'minecraft:surface'}
    surface = seq(
        cond('minecraft:on_floor', seq(
            cond(biome_is('rocky_dunes'), seq(cond('minecraft:on_ceiling', sandstone), cond({'type': 'minecraft:steep'}, sandstone), sand)),
            cond(biome_is('chrome_lakes'), seq(cond('minecraft:not_underwater', grass), sand)),
            cond(biome_is('sift_plains'), seq(cond('minecraft:not_underwater', turf), sand)),
            cond(biome_is('forest_mountains'), seq(cond(high, seq(cond(noise_patch, cobbled), dreamstone)), cond('minecraft:not_underwater', grass), sand)),
            cond('minecraft:not_underwater', grass),
            sand)),
        cond('minecraft:under_floor', seq(
            cond(biome_is('rocky_dunes'), seq(cond('minecraft:on_ceiling', sandstone), sand)),
            cond(biome_is('chrome_lakes'), sand),
            soil)),
        cond('minecraft:deep_under_floor', seq(cond(biome_is('rocky_dunes'), sandstone))),
    )
    underground = seq(
        cond(biome_is('deep_sift'), cond('minecraft:on_floor', cond({'type': 'minecraft:noise_threshold', 'max_threshold': 1.0, 'min_threshold': 0.1,
                                                                    'noise': 'minecraft:surface'}, moss))),
        cond({'type': 'minecraft:vertical_gradient', 'false_at_and_above': {'absolute': 8}, 'random_name': f'{NS}:hushslate',
              'true_at_and_below': {'absolute': 0}}, {'type': 'minecraft:block', 'result_state': state('hushslate', axis='y')}),
    )
    w('worldgen/material_rule/the_sift', seq(
        'minecraft:bedrock_floor',
        cond({'type': 'minecraft:above_preliminary_surface'}, surface),
        underground))


# ============================================================================ features

FEATURES = {}
PLACED = {}


def feature(name, obj):
    FEATURES[name] = obj
    w(f'worldgen/feature/{name}', obj)
    return f'{NS}:{name}'


def placed(name, feat, placement):
    PLACED[name] = True
    w(f'worldgen/placed_feature/{name}', {'feature': rl(feat), 'placement': placement})
    return f'{NS}:{name}'


def count(n):
    return {'type': 'minecraft:count', 'count': n}


def surface_patch(n_tries, xz=7, y=3):
    return [count(n_tries), {'type': 'minecraft:offset',
                             'x': {'type': 'minecraft:trapezoid', 'max': xz, 'min': -xz, 'plateau': 0},
                             'y': {'type': 'minecraft:trapezoid', 'max': y, 'min': -y, 'plateau': 0},
                             'z': {'type': 'minecraft:trapezoid', 'max': xz, 'min': -xz, 'plateau': 0}},
            {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'}}]


ON_SURFACE = [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, {'type': 'minecraft:biome'}]
BIOME = {'type': 'minecraft:biome'}


def rarity(n):
    return {'type': 'minecraft:rarity_filter', 'chance': n}


def survive(block):
    return {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:would_survive', 'state': rl(block)}}


def weighted(entries):
    return {'type': 'minecraft:weighted', 'entries': [{'data': d, 'weight': wt} for d, wt in entries]}


def features():
    leaves = lambda w_: state(f'{w_}_leaves', distance=7, persistent=False, waterlogged=False)  # noqa: E731
    log = lambda w_: state(f'{w_}_log', axis='y')  # noqa: E731
    hanging = state('hanging_lullwood_leaves', tip=True)
    feature('lullwood_tree', {'type': f'{NS}:sift_tree', 'trunk': log('lullwood'), 'leaves': leaves('lullwood'), 'hanging': hanging,
                              'style': 'puff', 'min_height': 5, 'max_height': 8})
    feature('grand_lullwood_tree', {'type': f'{NS}:sift_tree', 'trunk': log('lullwood'), 'leaves': leaves('lullwood'), 'hanging': hanging,
                                    'style': 'grand', 'min_height': 11, 'max_height': 16})
    feature('wishwood_tree', {'type': f'{NS}:sift_tree', 'trunk': log('wishwood'), 'leaves': leaves('wishwood'), 'style': 'wish',
                              'min_height': 5, 'max_height': 7})
    feature('tall_wishwood_tree', {'type': f'{NS}:sift_tree', 'trunk': log('wishwood'), 'leaves': leaves('wishwood'), 'style': 'tall_wish',
                                   'min_height': 9, 'max_height': 13})

    def tree_placed(name):
        return {'feature': rl(name), 'placement': [survive(f'{"lullwood" if "lullwood" in name else "wishwood"}_sapling')]}

    feature('trees_sift_plains', {'type': 'minecraft:random_selector', 'default': tree_placed('lullwood_tree'),
                                  'features': [{'chance': 0.15, 'feature': tree_placed('wishwood_tree')}]})
    feature('trees_forest_mountains', {'type': 'minecraft:random_selector', 'default': tree_placed('lullwood_tree'),
                                       'features': [{'chance': 0.25, 'feature': tree_placed('grand_lullwood_tree')},
                                                    {'chance': 0.12, 'feature': tree_placed('tall_wishwood_tree')}]})
    feature('trees_wishing_grove', {'type': 'minecraft:random_selector', 'default': tree_placed('wishwood_tree'),
                                    'features': [{'chance': 0.35, 'feature': tree_placed('tall_wishwood_tree')},
                                                 {'chance': 0.1, 'feature': tree_placed('grand_lullwood_tree')}]})
    tree_counts = {'trees_sift_plains': {'type': 'minecraft:weighted_list', 'distribution': [{'data': 0, 'weight': 6}, {'data': 1, 'weight': 3},
                                                                                              {'data': 2, 'weight': 1}]},
                   'trees_forest_mountains': 7, 'trees_wishing_grove': 5}
    for t, c in tree_counts.items():
        placed(t, t, [count(c), {'type': 'minecraft:in_square'}, {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
                      {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'}, BIOME])
    # floating islands & spires
    feature('floating_island', {'type': f'{NS}:floating_island', 'top': state('sift_grass_block', snowy=False), 'soil': state('sift_soil'),
                                'stone': state('dreamstone'), 'min_radius': 4, 'max_radius': 9, 'min_lift': 28, 'max_lift': 60,
                                'decoration': f'{NS}:lullwood_tree'})
    feature('floating_islet', {'type': f'{NS}:floating_island', 'top': state('coral_turf', snowy=False), 'soil': state('sift_soil'),
                               'stone': state('dreamstone'), 'min_radius': 2, 'max_radius': 4, 'min_lift': 14, 'max_lift': 40})
    placed('floating_island', 'floating_island', [rarity(14)] + ON_SURFACE)
    placed('floating_islet', 'floating_islet', [rarity(10)] + ON_SURFACE)
    feature('dreamstone_spire', {'type': f'{NS}:spire', 'body': state('dreamstone'), 'band': state('blush_bricks'),
                                 'cap': state('sift_grass_block', snowy=False), 'min_height': 14, 'max_height': 34, 'radius': 3.5})
    feature('dune_hoodoo', {'type': f'{NS}:spire', 'body': state('dreamsandstone'), 'band': state('blush_bricks'), 'min_height': 7,
                            'max_height': 18, 'radius': 2.6})
    placed('dreamstone_spire', 'dreamstone_spire', [rarity(4)] + ON_SURFACE)
    placed('dune_hoodoo', 'dune_hoodoo', [rarity(3)] + ON_SURFACE)
    # ground cover
    feature('blushgrass', {'type': 'minecraft:simple_block', 'to_place': weighted([(state('blushgrass'), 6), (state('coral_fern'), 1)])})
    feature('tall_blushgrass', {'type': 'minecraft:simple_block', 'to_place': state('tall_blushgrass')})
    feature('coral_bush', {'type': 'minecraft:simple_block', 'to_place': weighted([(state('coral_bush'), 6), (state('coral_fern'), 1)])})
    feature('dune_scrub', {'type': 'minecraft:simple_block', 'to_place': weighted([(state('coral_fern'), 4), (state('coral_bush'), 1)])})
    feature('coral_thicket', {'type': 'minecraft:simple_block', 'to_place': state('coral_thicket')})
    feature('sift_flowers', {'type': 'minecraft:simple_block', 'to_place': weighted([
        (state('lullaby_bell'), 3), (state('dreambloom'), 3), (state('soulpetal'), 2), (state('nebula_iris'), 2)])})
    feature('grove_flowers', {'type': 'minecraft:simple_block', 'to_place': weighted([
        (state('dreambloom'), 3), (state('nebula_iris'), 3), (state('choir_lily'), 1)])})
    petal_entries = []
    for f in ('north', 'east', 'south', 'west'):
        for a in (1, 2, 3, 4):
            petal_entries.append((state('drift_petals', facing=f, flower_amount=a), 1))
    feature('drift_petals', {'type': 'minecraft:simple_block', 'to_place': weighted(petal_entries)})
    feature('glimmer_sprouts', {'type': 'minecraft:simple_block', 'to_place': state('glimmer_sprouts')})
    feature('pitcher_bulb_bush', {'type': 'minecraft:simple_block', 'to_place': weighted([(state('pitcher_bulb_bush', age=3), 2),
                                                                                          (state('pitcher_bulb_bush', age=2), 1)])})
    feature('glowcap', {'type': 'minecraft:simple_block', 'to_place': state('glowcap')})
    feature('echo_orchid', {'type': 'minecraft:simple_block', 'to_place': weighted([(state('echo_orchid'), 1)])})
    feature('chrome_reeds', {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
                             'direction': 'up', 'layers': [{'height': {'type': 'minecraft:biased_to_bottom', 'max_inclusive': 4, 'min_inclusive': 2},
                                                            'provider': state('chrome_reeds', age=0)}], 'prioritize_tip': False})

    def patch(name, feat, tries, per_chunk, extra=None, survive_block=None):
        first = per_chunk if isinstance(per_chunk, dict) and per_chunk.get('type') == 'minecraft:rarity_filter' else count(per_chunk)
        pl = [first] + ON_SURFACE[:2] + [BIOME] + surface_patch(tries)
        if survive_block:
            pl.append(survive(survive_block))
        if extra:
            pl = [extra] + pl
        return placed(name, feat, pl)

    patch('patch_blushgrass', 'blushgrass', 32, 3, survive_block='blushgrass')
    patch('patch_blushgrass_dense', 'blushgrass', 48, 6, survive_block='blushgrass')
    patch('patch_tall_blushgrass', 'tall_blushgrass', 16, 1, survive_block='tall_blushgrass')
    # the reference biome: thick coral growth over pink turf
    patch('patch_coral_bush', 'coral_bush', 64, 7, survive_block='coral_bush')
    patch('patch_coral_thicket', 'coral_thicket', 48, 7, survive_block='coral_thicket')
    patch('patch_dune_scrub', 'dune_scrub', 10, 2, survive_block='coral_fern')
    patch('patch_sift_flowers', 'sift_flowers', 24, 2, survive_block='lullaby_bell')
    patch('patch_grove_flowers', 'grove_flowers', 24, 3, survive_block='dreambloom')
    patch('patch_drift_petals', 'drift_petals', 32, 1, survive_block='drift_petals')
    patch('patch_glimmer_sprouts', 'glimmer_sprouts', 16, 1, survive_block='glimmer_sprouts')
    patch('patch_pitcher_bulb_bush', 'pitcher_bulb_bush', 8, rarity(6), survive_block='pitcher_bulb_bush')
    patch('patch_chrome_reeds', 'chrome_reeds', 20, 4, survive_block='chrome_reeds')
    patch('patch_glowcap_surface', 'glowcap', 12, rarity(4), survive_block='glowcap')
    # the Stomper's favourite flower, in lavender clumps on the plains
    feature('hummingbloom', {'type': 'minecraft:simple_block', 'to_place': state('hummingbloom')})
    patch('patch_hummingbloom', 'hummingbloom', 20, rarity(3), survive_block='hummingbloom')
    # lakes, springs, disks, boulders
    feature('chrome_pool', {'type': 'minecraft:lake', 'barrier': state('dreamstone'), 'can_place_feature': {'type': 'minecraft:true'},
                            'can_replace_with_air_or_fluid': {'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_block_tag',
                                                                                                        'tag': 'minecraft:features_cannot_replace'}},
                            'can_replace_with_barrier': {'type': 'minecraft:not', 'predicate': {'type': 'minecraft:matching_block_tag',
                                                                                                   'tag': 'minecraft:lava_pool_stone_cannot_replace'}},
                            'fluid': state('chrome', level=0)})
    placed('chrome_pool_surface', 'chrome_pool', [rarity(24), {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'},
                                                  BIOME])
    feature('chrome_spring', {'type': 'minecraft:spring_feature', 'state': state('chrome', falling=True),
                              'valid_blocks': [rl('dreamstone'), rl('sift_soil'), rl('hushslate'), rl('dreamsandstone')]})
    placed('chrome_spring', 'chrome_spring', [count(12), {'type': 'minecraft:in_square'},
                                              {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:biased_to_bottom',
                                                                                            'max_inclusive': {'below_top': 8},
                                                                                            'min_inclusive': {'above_bottom': 8}, 'inner': 8}}, BIOME])
    feature('disk_dreamsand', {'type': 'minecraft:disk', 'half_height': 2, 'radius': {'type': 'minecraft:uniform', 'max_inclusive': 6, 'min_inclusive': 2},
                               'state_provider': {'type': 'minecraft:rule_based', 'fallback': state('dreamsand'), 'rules': []},
                               'target': {'type': 'minecraft:matching_blocks', 'blocks': [rl('sift_soil'), rl('sift_grass_block')]}})
    placed('disk_dreamsand', 'disk_dreamsand', [count(3), {'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'},
                                                {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_fluids',
                                                                                                         'fluids': rl('chrome')}}, BIOME])
    feature('dream_boulder', {'type': 'minecraft:block_blob', 'can_place_on': {'type': 'minecraft:matching_blocks',
                                                                                'blocks': [rl('sift_grass_block'), rl('sift_soil'), rl('dreamstone')]},
                              'state': state('cobbled_dreamstone')})
    placed('dream_boulder', 'dream_boulder', [rarity(3)] + ON_SURFACE)
    # ores
    # Serbim (Siftite) ore is extremely rare: small, always buried, and only in one chunk in three
    feature('ore_serbim', {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 1.0, 'size': 3, 'targets': [
        {'state': state('serbim_ore'), 'target': {'predicate_type': 'minecraft:block_match', 'block': rl('dreamstone')}},
        {'state': state('deep_serbim_ore'), 'target': {'predicate_type': 'minecraft:block_match', 'block': rl('hushslate')}}]})
    placed('ore_serbim', 'ore_serbim', [rarity(3), {'type': 'minecraft:in_square'}, {'type': 'minecraft:height_range', 'height': {
        'type': 'minecraft:trapezoid', 'max_inclusive': {'absolute': 16}, 'min_inclusive': {'absolute': -64}}}, BIOME])
    feature('ore_hushslate_blob', {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': 0.0, 'size': 48, 'targets': [
        {'state': state('hushslate', axis='y'), 'target': {'predicate_type': 'minecraft:block_match', 'block': rl('dreamstone')}}]})
    placed('ore_hushslate_blob', 'ore_hushslate_blob', [count(2), {'type': 'minecraft:in_square'}, {'type': 'minecraft:height_range', 'height': {
        'type': 'minecraft:uniform', 'max_inclusive': {'absolute': 40}, 'min_inclusive': {'absolute': 0}}}, BIOME])
    # deep sift
    body = weighted([(state('glowbell_vine_plant', bell=False), 4), (state('glowbell_vine_plant', bell=True), 1)])
    feature('glowbell_vine', {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
                              'direction': 'down', 'layers': [
                                  {'height': {'type': 'minecraft:uniform', 'max_inclusive': 8, 'min_inclusive': 1}, 'provider': body},
                                  {'height': 1, 'provider': weighted([(state('glowbell_vine', age=20, bell=True), 1),
                                                                      (state('glowbell_vine', age=20, bell=False), 2)])}],
                              'prioritize_tip': True})
    placed('glowbell_vine', 'glowbell_vine', [count(90), {'type': 'minecraft:in_square'},
                                              {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'max_inclusive': {'absolute': 60},
                                                                                            'min_inclusive': {'above_bottom': 0}}},
                                              {'type': 'minecraft:environment_scan', 'allowed_search_condition': {'type': 'minecraft:matching_block_tag',
                                                                                                                  'tag': 'minecraft:air'},
                                               'direction_of_search': 'up', 'max_steps': 12,
                                               'target_condition': {'type': 'minecraft:has_sturdy_face', 'direction': 'down'}},
                                              {'type': 'minecraft:offset', 'x': 0, 'y': -1, 'z': 0}, BIOME])
    feature('lumen_moss_vegetation', {'type': 'minecraft:simple_block', 'to_place': weighted([(state('glimmer_sprouts'), 5), (state('glowcap'), 2),
                                                                                              (state('echo_orchid'), 1), (state('lumen_moss_carpet'), 3)])})
    for surf in ('floor', 'ceiling'):
        feature(f'lumen_moss_patch_{surf}', {'type': 'minecraft:vegetation_patch', 'depth': 1, 'extra_bottom_block_chance': 0.0,
                                              'extra_edge_column_chance': 0.3, 'ground_state': state('lumen_moss_block'),
                                              'replaceable': f'#{NS}:deep_sift_ground', 'surface': surf,
                                              'vegetation_chance': 0.35 if surf == 'floor' else 0.0,
                                              'vegetation_feature': {'feature': f'{NS}:lumen_moss_vegetation', 'placement': []},
                                              'vertical_range': 5, 'xz_radius': {'type': 'minecraft:uniform', 'max_inclusive': 6, 'min_inclusive': 3}})
        placed(f'lumen_moss_patch_{surf}', f'lumen_moss_patch_{surf}', [
            count(40), {'type': 'minecraft:in_square'},
            {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:uniform', 'max_inclusive': {'absolute': 60}, 'min_inclusive': {'above_bottom': 0}}},
            {'type': 'minecraft:environment_scan', 'allowed_search_condition': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
             'direction_of_search': 'down' if surf == 'floor' else 'up', 'max_steps': 12,
             'target_condition': {'type': 'minecraft:has_sturdy_face', 'direction': 'up' if surf == 'floor' else 'down'}},
            {'type': 'minecraft:offset', 'x': 0, 'y': 1 if surf == 'floor' else -1, 'z': 0}, BIOME])


# ============================================================================ biomes

CARVERS = ['minecraft:cave', 'minecraft:cave_extra_underground', 'minecraft:canyon']


def mobs(creature=(), monster=(), ambient=(), water=(), water_ambient=()):
    def entries(lst):
        return [{'type': rl(t), 'count': ({'type': 'minecraft:uniform', 'min_inclusive': a, 'max_inclusive': b} if a != b else a), 'weight': wt}
                for t, wt, a, b in lst]

    return {'argument': {'spawn_costs': {}, 'spawns_by_category': {
        'creature': entries(creature), 'monster': entries(monster), 'ambient': entries(ambient), 'water_creature': entries(water),
        'underground_water_creature': [], 'water_ambient': entries(water_ambient), 'axolotls': [], 'misc': []}}, 'modifier': 'overlay'}


def particles(*entries):
    return {'argument': [{'particle': {'type': rl(p)}, 'probability': pr} for p, pr in entries], 'modifier': 'append'}


def biome(name, *, fog, sky, water, grass, foliage, temp, down, spawns, parts, feats, music=f'{NS}:music.sift', ambient_loop=f'{NS}:ambient.sift.loop'):
    steps = [[] for _ in range(11)]
    for step, f in feats:
        steps[step].append(rl(f))
    # Every biome must list shared features in the same relative order (vanilla rejects order cycles),
    # so sort each step by one global order: our features in creation order, then vanilla ones.
    order = [rl(n) for n in PLACED]
    for st in steps:
        st.sort(key=lambda f: (order.index(f) if f in order else len(order), f))
    w(f'worldgen/biome/{name}', {
        'attributes': {
            'minecraft:audio/ambient_sounds': {'loop': ambient_loop,
                                               'additions': {'sound': f'{NS}:ambient.sift.additions', 'tick_chance': 0.0111},
                                               'mood': {'block_search_extent': 8, 'offset': 2.0, 'sound': f'{NS}:ambient.sift.mood', 'tick_delay': 6000}},
            'minecraft:audio/background_music': {'default': {'max_delay': 18000, 'min_delay': 6000, 'replace_current_music': True, 'sound': music}},
            'minecraft:gameplay/natural_mob_spawns': spawns,
            'minecraft:visual/ambient_particles': parts,
            'minecraft:visual/fog_color': fog,
            'minecraft:visual/sky_color': sky,
            'minecraft:visual/water_fog_color': water,
        },
        'carvers': CARVERS,
        'downfall': down,
        'effects': {'water_color': water, 'grass_color': grass, 'foliage_color': foliage},
        'features': steps,
        'has_precipitation': False,
        'temperature': temp,
    })


COMMON_UNDERGROUND = [(6, 'ore_serbim'), (6, 'ore_hushslate_blob'), (8, 'chrome_spring')]
DREAMY_PARTICLES = [('drifting_soul', 0.0008), ('dream_pollen', 0.0012), ('glow_dust', 0.0008), ('sift_mist', 0.0003), ('wishing_star', 0.00005)]


def biomes():
    biome('sift_plains', fog='#aef0e2', sky='#5ed6c6', water='#7fe8ff', grass='#63d6c6', foliage='#6fe2dc', temp=0.7, down=0.6,
          spawns=mobs(creature=[('bulb', 12, 2, 4), ('minecraft:sniffer', 3, 1, 2), ('enchoer', 1, 1, 1), ('harmoner', 6, 1, 3)]
                      + [('stomper', 2, 1, 3), ('sky_whale', 1, 1, 1)],
                      water=[('fanfare_eel', 2, 1, 1), ('tubafish', 2, 1, 1)], water_ambient=[('kazoo_fish', 8, 3, 6)]),
          parts=particles(*DREAMY_PARTICLES),
          feats=[(1, 'chrome_pool_surface'), (2, 'floating_island'), (2, 'floating_islet'), (4, 'dream_boulder')] + COMMON_UNDERGROUND +
                [(9, 'trees_sift_plains'), (9, 'patch_coral_thicket'), (9, 'patch_coral_bush'), (9, 'patch_sift_flowers'),
                 (9, 'patch_drift_petals'), (9, 'patch_pitcher_bulb_bush'), (9, 'patch_glimmer_sprouts')] + [(9, 'patch_hummingbloom')])
    biome('forest_mountains', fog='#a2e8de', sky='#5ed6c6', water='#7fe8ff', grass='#4fc9b8', foliage='#5fd8d0', temp=0.5, down=0.8,
          spawns=mobs(creature=[('bulb', 6, 2, 3), ('minecraft:sniffer', 4, 1, 2), ('enchoer', 2, 1, 1), ('harmoner', 6, 1, 3)]),
          parts=particles(('lullwood_leaf', 0.002), ('drifting_soul', 0.002), ('sift_mist', 0.001), ('glow_dust', 0.002), ('wishing_star', 0.00012)),
          feats=[(2, 'dreamstone_spire'), (2, 'floating_island'), (4, 'dream_boulder')] + COMMON_UNDERGROUND +
                [(9, 'trees_forest_mountains'), (9, 'patch_blushgrass'), (9, 'patch_sift_flowers'), (9, 'patch_glowcap_surface'),
                 (9, 'patch_glimmer_sprouts')])
    biome('rocky_dunes', fog='#bdeee0', sky='#5ed6c6', water='#8ff0ff', grass='#d9a6c4', foliage='#e0b0c8', temp=1.2, down=0.1,
          spawns=mobs(creature=[('bulb', 2, 1, 2), ('minecraft:sniffer', 2, 1, 1)], monster=[('sifter', 60, 1, 3)]),
          parts=particles(('dream_pollen', 0.003), ('glow_dust', 0.0015), ('wishing_star', 0.0002)),
          feats=[(2, 'dune_hoodoo'), (2, 'floating_islet')] + COMMON_UNDERGROUND + [(9, 'patch_dune_scrub'), (9, 'patch_pitcher_bulb_bush')])
    biome('chrome_lakes', fog='#a8eee6', sky='#5ed6c6', water='#9ff5ff', grass='#7fe0d0', foliage='#86e9e2', temp=0.6, down=0.9,
          spawns=mobs(creature=[('slumbler', 10, 1, 2), ('bulb', 3, 1, 2)],
                      water=[('fanfare_eel', 5, 1, 2), ('tubafish', 4, 1, 1)], water_ambient=[('kazoo_fish', 12, 3, 7)]),
          parts=particles(('chrome_bubble', 0.002), ('sift_mist', 0.0012), ('drifting_soul', 0.002), ('wishing_star', 0.00015)),
          feats=[(2, 'floating_islet'), (6, 'disk_dreamsand')] + COMMON_UNDERGROUND +
                [(9, 'patch_chrome_reeds'), (9, 'patch_blushgrass'), (9, 'trees_sift_plains')])
    biome('wishing_grove', fog='#b4eee2', sky='#5ed6c6', water='#ffb8e6', grass='#f59ac6', foliage='#f9b3d4', temp=0.8, down=0.7,
          spawns=mobs(creature=[('bulb', 8, 2, 4), ('enchoer', 3, 1, 2), ('minecraft:allay', 2, 1, 2), ('harmoner', 8, 1, 3)]
                      + [('sky_whale', 1, 1, 1)]),
          parts=particles(('wishwood_leaf', 0.003), ('star_sparkle', 0.002), ('drifting_soul', 0.003), ('wishing_star', 0.0003)),
          feats=[(2, 'floating_island'), (4, 'dream_boulder')] + COMMON_UNDERGROUND +
                [(9, 'trees_wishing_grove'), (9, 'patch_grove_flowers'), (9, 'patch_drift_petals'), (9, 'patch_blushgrass'), (9, 'patch_pitcher_bulb_bush')])
    biome('deep_sift', fog='#1a2f3f', sky='#223a5a', water='#3fc8d8', grass='#2f8f9e', foliage='#37a9b5', temp=0.5, down=0.4,
          spawns=mobs(monster=[('riveter', 40, 1, 2), ('sifter', 5, 1, 1)]),
          parts=particles(('glow_dust', 0.006), ('drifting_soul', 0.0015)),
          music=f'{NS}:music.deep_sift', ambient_loop=f'{NS}:ambient.deep_sift.loop',
          feats=COMMON_UNDERGROUND + [(2, 'minecraft:amethyst_geode'), (7, 'minecraft:sculk_vein'), (7, 'minecraft:sculk_patch_deep_dark'),
                                      (9, 'lumen_moss_patch_floor'), (9, 'lumen_moss_patch_ceiling'), (9, 'glowbell_vine')])


def dimension_json():
    F = [-1.0, 1.0]

    def pt(b, t=F, h=F, c=F, e=F, d=0.0, wd=F, off=0.0):
        return {'biome': rl(b), 'parameters': {'temperature': t, 'humidity': h, 'continentalness': c, 'erosion': e, 'depth': d,
                                               'weirdness': wd, 'offset': off}}

    land = [-0.19, 1.0]
    entries = [
        pt('chrome_lakes', c=[-1.2, -0.19]),
        pt('forest_mountains', c=[0.03, 1.0], e=[-1.0, -0.22]),
        pt('rocky_dunes', t=[0.35, 1.0], h=[-1.0, 0.05], c=land, e=[-0.22, 1.0]),
        pt('wishing_grove', h=[0.35, 1.0], c=land, e=[-0.22, 1.0], wd=[0.2, 1.0]),
        pt('sift_plains', t=[-1.0, 0.35], h=[-1.0, 0.35], c=land, e=[-0.22, 1.0]),
        pt('sift_plains', t=[0.35, 1.0], h=[0.05, 0.35], c=land, e=[-0.22, 1.0]),
        pt('sift_plains', h=[0.35, 1.0], c=land, e=[-0.22, 1.0], wd=[-1.0, 0.2]),
        pt('deep_sift', d=[0.9, 1.1]),
    ]
    w('dimension/the_sift', {'type': f'{NS}:the_sift', 'generator': {
        'type': 'minecraft:noise', 'biome_source': {'type': 'minecraft:multi_noise', 'biomes': entries}, 'settings': f'{NS}:the_sift'}})


def carver_tags():
    for b in ['dreamstone', 'hushslate', 'sift_soil', 'sift_grass_block', 'coral_turf', 'dreamsand', 'dreamsandstone', 'lumen_moss_block', 'cobbled_dreamstone',
              'mossy_dreamstone_bricks']:
        GA.tag('block', 'minecraft:overworld_carver_replaceables', rl(b))
    for b in ['dreamstone', 'hushslate', 'dreamsand', 'sift_soil']:
        GA.tag('block', 'minecraft:moss_replaceable', rl(b))
        GA.tag('block', 'minecraft:sculk_replaceable', rl(b))
        GA.tag('block', 'minecraft:sculk_replaceable_world_gen', rl(b))
    GA.tag('block', 'minecraft:azalea_root_replaceable', rl('dreamstone'))
    GA.tag('block', 'minecraft:dripstone_replaceable_blocks', rl('dreamstone'))
    GA.tag('block', 'minecraft:lush_ground_replaceable', rl('dreamstone'))


def generate():
    dimension()
    noise_settings()
    features()
    biomes()
    dimension_json()
    carver_tags()
    print(f'world ok: {len(FEATURES)} features, {len(PLACED)} placed')
