"""W-sky: the Sky Islands - the Sift's sky biome (renamed from the Sound Garden, id `sky_island`).

Giant floating islands (their own density function, thesift:sift/sky_islands) joined by huge Sky Roots and swingable
Sky Vines; Skypalms whose vines link tree to tree like leads, puffy Cloudpuff trees, Fluffbushes, Sky Grass and
Cirrus Grass; giant Driftfruits hanging at the ends of vines (Sky Whale food) and Skyrinds - the sky bananas that
tame Swingers - in bunches under the Skypalm fronds.

Hooks (one line each):
  * spec.py          -> declare(block, item): blocks and items
  * gen_assets.py    -> assets(GA): text, Codex, tags, recipes, loot;  gen_block(GA, b): the 'sky_*' model kinds
  * gen_world.py     -> density(GW) (in noise_settings: the island layer) and world(GW): features, biome, placement,
                        surface rules
  * gen_textures.py  -> textures(out) (tools/sky_art.py, before vanilla_remap)
Java: com.thesift.world.sky (blocks, swinging physics, the rope entity, worldgen features), dev/SkyTest.
"""
import json
import os

NS = 'thesift'
BIOME = 'sky_island'
J = 'com.thesift.world.sky'

# ---------------------------------------------------------------------------- the island layer (tuned in the scratchpad)
# A 2D island mask (one big smooth noise plus a finer one for bays and capes), a top surface that follows a slow
# altitude noise (islands sit at different heights, always well above the ground under them) and an underside that
# hangs deeper towards each island's middle, ragged with stalactites.
MASK_NOISE, MASK_SCALE = 'minecraft:badlands_pillar_roof', 2.1
DETAIL_NOISE, DETAIL_SCALE, DETAIL_WEIGHT = 'minecraft:packed_ice', 1.7, 0.45
THRESHOLD, SHARPNESS, EDGE = 0.18, 3.2, 0.05
ALT_NOISE, ALT_SCALE, ALT_AMP = 'minecraft:clay_bands_offset', 0.75, 20.0
BUMP_NOISE, BUMP_SCALE, BUMP_AMP = 'minecraft:iceberg_surface', 1.0, 2.5
SPIKE_NOISE, SPIKE_SCALE = 'minecraft:iceberg_pillar', 2.6
ROUGH_NOISE = 'minecraft:ice'
LOWEST, HIGHEST = 96, 300
TERRAIN_OFFSET = f'{NS}:sift/offset'  # W-land's terrain offset (falls back to the vanilla one if it is ever gone)


def _df(t, **kw):
    return {'type': f'minecraft:{t}', **kw}


def _add(a, b):
    return _df('add', left=a, right=b)


def _mul(a, b):
    return _df('mul', left=a, right=b)


def _noise(n, xz, y=0.0):
    return _df('noise', noise=n, xz_scale=xz, y_scale=y)


def density(GW, offset=None):
    """Writes thesift:sift/sky_islands (+ its cached 2D parts) and returns its id for the noise router's max()."""
    offset = offset or TERRAIN_OFFSET
    y = _df('gradient', axis='y', from_coordinate=-64, from_value=-64.0, to_coordinate=320, to_value=320.0)
    mask = _add(_noise(MASK_NOISE, MASK_SCALE), _mul(_noise(DETAIL_NOISE, DETAIL_SCALE), DETAIL_WEIGHT))
    GW.w('worldgen/density_function/sift/sky_island_mask', _df('cache', input=_df('clamp', input=_mul(_add(mask, -THRESHOLD), SHARPNESS),
                                                                                    min=0.0, max=1.0)))
    m = f'{NS}:sift/sky_island_mask'
    # islands float about 85 blocks over the ground they shade (offset 0.01 ~ 1.3 blocks), never lower than y 186
    base = _df('clamp', input=_add(_mul(offset, 128.0), 213.0), min=186.0, max=262.0)
    top = _add(_add(base, _mul(_noise(ALT_NOISE, ALT_SCALE), ALT_AMP)),
               _add(_add(_mul(m, 3.0), _mul(_df('square', input=m), 5.0)), _mul(_noise(BUMP_NOISE, BUMP_SCALE), BUMP_AMP)))
    GW.w('worldgen/density_function/sift/sky_island_top', _df('cache', input=top))
    thick = _add(_add(3.0, _mul(m, 18.0)), _add(_mul(_df('square', input=m), 24.0),
                                                 _mul(_mul(m, _df('abs', input=_noise(SPIKE_NOISE, SPIKE_SCALE))), 20.0)))
    GW.w('worldgen/density_function/sift/sky_island_bottom', _df('cache', input=_df('sub', left=f'{NS}:sift/sky_island_top', right=thick)))
    to_top = _df('sub', left=f'{NS}:sift/sky_island_top', right=y)
    from_bottom = _add(_df('sub', left=y, right=f'{NS}:sift/sky_island_bottom'), _mul(_noise(ROUGH_NOISE, 1.0, 1.5), 4.0))
    shape = _mul(_df('min', left=to_top, right=from_bottom), 0.15)
    solid = _df('min', left=shape, right=_mul(_add(m, -EDGE), 8.0))
    GW.w('worldgen/density_function/sift/sky_islands', _df('interpolated', cell_size_xz=4, cell_size_y=8, input=_df(
        'range_choice', input=y, min_inclusive=float(LOWEST), max_exclusive=float(HIGHEST), when_in_range=_df('clamp', input=solid, min=-1.0, max=1.0),
        when_out_of_range=-1.0)))
    return f'{NS}:sift/sky_islands'


# ---------------------------------------------------------------------------- blocks and items

def declare(block, item):
    plant = 'BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_CYAN)'
    # the islands' ground: Sky Grass (soft - falls onto it hurt half as much) and its wispy Cirrus Grass
    block('sky_grass_block', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_CYAN)',
          cls=f'{J}.SkyGrassBlock', model='grass_block', tags=['shovel', 'dirt'], loot='silk:sift_soil', tab='nature')
    block('cirrus_grass', 'custom', plant, cls='SiftPlantBlock', model='cross', tags=['replaceable_plants', 'sword_efficient'], loot='grass',
          tab='nature')
    block('tall_cirrus_grass', 'custom', plant, cls='SiftDoublePlantBlock', model='double_cross', tags=['replaceable_plants'], loot='shears',
          item_kind='double', tab='nature')
    block('fluffbush', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA).mapColor(MapColor.SNOW).noCollision().sound(SoundType.WOOL)',
          cls=f'{J}.FluffbushBlock', model='sky_bush', tags=['hoe', 'replaceable_plants'], tab='nature')
    # the vines and roots that hold the islands together (Sky Vines are what you swing on)
    block('sky_vine', 'custom', 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollision().noOcclusion().strength(0.6F)'
          '.sound(SoundType.VINE).ignitedByLava().randomTicks().pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)',
          cls=f'{J}.SkyVineBlock', model='sky_vine', tags=['axe', 'sword_efficient'], tab='nature')
    block('sky_root', 'pillar', 'BlockBehaviour.Properties.ofFullCopy(Blocks.MANGROVE_LOG).mapColor(MapColor.TERRACOTTA_WHITE)',
          tags=['axe', 'sky_logs', 'logs_that_burn'], tab='nature')
    # Skypalms: the alien tropical trees (vines link them like leads), and the puffy Cloudpuff trees on Sky Root trunks
    block('skypalm_log', 'pillar', 'BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG).mapColor(MapColor.COLOR_PURPLE)',
          tags=['axe', 'sky_logs', 'logs_that_burn'], tab='nature')
    for w, particle, chance, props in (
            ('skypalm', 'ModParticles.LULLWOOD_LEAF', '0.02F', 'ofFullCopy(Blocks.JUNGLE_LEAVES).mapColor(MapColor.COLOR_CYAN)'),
            ('cloudpuff', 'ModSwifter.WHITE_FLUFF', '0.04F', 'ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.SNOW)')):
        block(f'{w}_leaves', 'leaves', f'BlockBehaviour.Properties.{props}', tags=['hoe', 'leaves'], loot=f'leaves:{w}_sapling', particle=particle,
              chance=chance, wood=w, tab='nature', name='Skypalm Fronds' if w == 'skypalm' else 'Cloudpuff Leaves')
        block(f'{w}_sapling', 'sapling', 'BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)', tags=['saplings'],
              grower=f'{J}.SkyIslands.{w.upper()}', wood=w, tab='nature')
    # the fruit: giant Driftfruits at the ends of vines (Sky Whale food), Skyrind bunches under the fronds
    block('driftfruit', 'custom', 'BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0F).sound(SoundType.WOOD)'
          '.lightLevel(s -> 7).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)',
          cls=f'{J}.DriftfruitBlock', model='sky_fruit', tags=['axe'], loot='none', tab='nature')
    block('skyrind_bunch', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.COCOA).mapColor(MapColor.GOLD).noCollision()',
          cls=f'{J}.SkyrindBunchBlock', model='sky_bunch', item=False, loot='none', name='Skyrind Bunch')
    food = f'{J}.SkyFoods'
    item('skyrind', props=f'new Item.Properties().food({food}.SKYRIND, {food}.SKYRIND_CONSUMABLE)', name='Skyrind')
    item('driftfruit_slice', props=f'new Item.Properties().food({food}.DRIFTFRUIT_SLICE, {food}.DRIFTFRUIT_SLICE_CONSUMABLE)')


# ---------------------------------------------------------------------------- block models

def _face(tex, uv, cull=None, rot=None):
    f = {'texture': tex, 'uv': uv}
    if cull:
        f['cullface'] = cull
    if rot:
        f['rotation'] = rot
    return f


def _box(frm, to, tex, up=None, down=None, cull=None):
    """A box whose every face samples the texture under its own footprint (like a vanilla cube)."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    sides = {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
             'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0]}
    faces = {d: _face(tex, uv, cull if cull == d else None) for d, uv in sides.items()}
    faces['up'] = _face(up or tex, [x0, z0, x1, z1], cull if cull == 'up' else None)
    faces['down'] = _face(down or tex, [x0, 16 - z1, x1, 16 - z0], cull if cull == 'down' else None)
    return {'from': list(frm), 'to': list(to), 'faces': faces}


def _model(GA, name, textures, elements, render='minecraft:cutout', ao=True):
    m = {'parent': 'minecraft:block/block', 'render_type': render, 'textures': {k: f'{NS}:block/{v}' for k, v in textures.items()},
         'elements': elements}
    if not ao:
        m['ambientocclusion'] = False
    GA.note_textures(m)
    GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
    return f'{NS}:block/{name}'


def _vine(GA, bid):
    """A thick, twisted liana: a 6-pixel core with little leaf tufts, and an arm towards every neighbour it holds on to
    (multipart, like the chorus plant)."""
    leaf = []
    for angle, (frm, to) in ((45, ([1.5, 1, 8], [14.5, 15, 8])), (-45, ([1.5, 1, 8], [14.5, 15, 8]))):
        leaf.append({'from': frm, 'to': to, 'shade': False, 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                     'faces': {'north': _face('#leaf', [0, 0, 16, 16]), 'south': _face('#leaf', [16, 0, 0, 16])}})
    core = _model(GA, bid + '_core', {'particle': bid, 'vine': bid, 'leaf': bid + '_leaf'}, [_box([5, 5, 5], [11, 11, 11], '#vine')] + leaf, ao=False)
    arm = _model(GA, bid + '_arm', {'particle': bid, 'vine': bid}, [_box([5, 5, 0], [11, 11, 5], '#vine', cull='north')], ao=False)
    parts = [{'apply': {'model': core}}]
    for d, rot in (('north', {}), ('east', {'y': 90}), ('south', {'y': 180}), ('west', {'y': 270}), ('up', {'x': 270}), ('down', {'x': 90})):
        apply = {'model': arm, **rot}
        if rot:
            apply['uvlock'] = True
        parts.append({'apply': apply, 'when': {d: 'true'}})
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'multipart': parts})
    GA.item_generated(bid, f'item/{bid}')


def _fruit(GA, bid):
    """The Driftfruit: a big ribbed gourd hanging from a short stem, a nub underneath."""
    els = [
        _box([2, 1, 2], [14, 13, 14], '#side', up='#top', down='#bottom'),
        _box([4, 13, 4], [12, 14, 12], '#side', up='#top'),
        _box([7, 14, 7], [9, 16, 9], '#stem'),
        _box([5, 0, 5], [11, 1, 11], '#bottom'),
    ]
    # the body's faces use the middle of the texture (the ribs line up with the cap)
    for f, uv in (('north', [2, 3, 14, 15]), ('south', [2, 3, 14, 15]), ('west', [2, 3, 14, 15]), ('east', [2, 3, 14, 15])):
        els[0]['faces'][f]['uv'] = uv
    for f in ('north', 'south', 'west', 'east'):
        els[1]['faces'][f]['uv'] = [4, 1, 12, 2]
    _model(GA, bid, {'particle': bid + '_side', 'side': bid + '_side', 'top': bid + '_top', 'bottom': bid + '_bottom', 'stem': bid + '_stem'}, els,
           render='minecraft:solid')
    GA.simple_state(bid)
    GA.item_block(bid)


def _bunch(GA, bid):
    """A Skyrind bunch hanging under the fronds: four stages, unripe green to ripe gold."""
    variants = {}
    for age in range(4):
        name = f'{bid}_stage{age}'
        els = []
        for angle in (45, -45):
            els.append({'from': [0.8, 0, 8], 'to': [15.2, 16, 8], 'shade': False,
                        'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                        'faces': {'north': _face('#cross', [0, 0, 16, 16]), 'south': _face('#cross', [16, 0, 0, 16])}})
        variants[f'age={age}'] = {'model': _model(GA, name, {'particle': name, 'cross': name}, els, ao=False)}
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})


def gen_block(GA, b):
    bid, k = b['id'], b['model']
    if k == 'sky_vine':
        _vine(GA, bid)
    elif k == 'sky_bush':  # the azalea bush shape, every texture (the stems' too) our own
        GA.copy_template('azalea', bid, GA.token_tex('azalea', bid), model_rename={'template_azalea': f'template_{bid}'})
    elif k == 'sky_fruit':
        _fruit(GA, bid)
    elif k == 'sky_bunch':
        _bunch(GA, bid)
    else:
        raise ValueError(f'no sky asset rule for {bid} ({k})')
    GA.LANG[f'block.{NS}.{bid}'] = b['name']


# ---------------------------------------------------------------------------- text, tags, recipes, loot

TEXT = {
    f'biome.{NS}.{BIOME}': 'Sky Island',
    f'entity.{NS}.sky_rope': 'Sky Vine',
    f'codex.{NS}.sky_islands.title': 'Sky Islands',
    f'codex.{NS}.sky_islands.tagline': 'Giant floating islands, joined by vines you can swing on',
    f'codex.{NS}.sky_islands.body': 'High above the Sift drift the Sky Islands: giant chunks of pale rock under cyan Sky Grass, '
                                    'hung with cloud and stalactites. Huge Sky Roots and Sky Vines span the gaps between them, and '
                                    'lianas dangle from their roots and from the Skypalms, whose vines run from tree to tree like '
                                    'leads. Grab a vine (use it, or jump into it) and swing: hold forward to pump, sneak to slide '
                                    'down, look up and hold forward to climb, jump to let go and fly off with all your speed. On a '
                                    'span, hold forward to go hand over hand along it. Sky Grass and Fluffbushes soften a landing. '
                                    'Puffy Cloudpuff trees, Cirrus Grass, Chime Bells and Organ Reeds grow up here, and Sky Whales sing.',
    f'codex.{NS}.sky_fruit.title': 'Driftfruit and Skyrind',
    f'codex.{NS}.sky_fruit.tagline': 'The fruit of the sky',
    f'codex.{NS}.sky_fruit.body': 'Driftfruits are giant glowing gourds that grow at the ends of hanging Sky Vines - cut the vine '
                                  'and the fruit drops. Break one for Driftfruit Slices (a springy snack); a vine tip slowly grows a new '
                                  'fruit. Sky Whales graze on them. Skyrinds are the sky bananas: they ripen in bunches under Skypalm '
                                  'fronds, gold with a pink tip. Pick a ripe bunch with an empty hand and it grows again. Eating one '
                                  'lets you drift down gently for a moment - and the Swingers of the islands would do anything for them.',
}


def assets(GA):
    tag, rl = GA.tag, GA.rl
    GA.LANG.update(TEXT)
    # Phase 4 hooks: what Sky Whales eat and what tames a Swinger; every block a creature (or you) can swing from
    tag('block', f'{NS}:sky_whale_food', rl('driftfruit'))
    tag('item', f'{NS}:swinger_food', rl('skyrind'))
    tag('block', f'{NS}:swingable', rl('sky_vine'))
    tag('worldgen/biome', f'{NS}:is_sift', rl(BIOME))
    tag('block', 'minecraft:overworld_carver_replaceables', rl('sky_grass_block'))
    tag('block', 'minecraft:sword_efficient', rl('fluffbush'))
    tag('block', 'minecraft:sword_efficient', rl('skyrind_bunch'))
    tag('block', 'minecraft:sword_efficient', rl('tall_cirrus_grass'))
    # recipes: rope from vines, planks from the two sky woods, fruit back into a block, fluff into cloud
    GA.shaped('lead_from_sky_vine', ['VV ', 'VV ', '  V'], {'V': 'sky_vine'}, 'minecraft:lead', 2, 'misc')
    GA.shapeless('wishwood_planks_from_skypalm_log', ['skypalm_log'], 'wishwood_planks', 4, 'building', 'planks')
    GA.shapeless('lullwood_planks_from_sky_root', ['sky_root'], 'lullwood_planks', 4, 'building', 'planks')
    GA.shaped('driftfruit', ['###', '###', '###'], {'#': 'driftfruit_slice'}, 'driftfruit', 1, 'building')
    GA.shaped('cloud_block_from_fluffbush', ['##', '##'], {'#': 'fluffbush'}, 'cloud_block', 1, 'building')
    GA.shapeless('pink_dye_from_driftfruit_slice', ['driftfruit_slice'], 'minecraft:pink_dye', 1, 'misc', 'pink_dye')
    # loot: the Driftfruit breaks like a melon (Silk Touch keeps it whole), a bunch gives what has ripened
    GA.loot('driftfruit', 'melon', {'melon': 'driftfruit', 'melon_slice': 'driftfruit_slice'})
    import gen_data as GD

    def age(n):
        return {'type': 'minecraft:match_block', 'blocks': rl('skyrind_bunch'), 'state': {'age': str(n)}}
    decay = {'type': 'minecraft:explosion_decay'}
    GD.table('block', 'blocks/skyrind_bunch', [
        GD.pool([GD.item('skyrind', count=(3, 5), extra=[decay])], condition=age(3)),
        GD.pool([GD.item('skyrind', count=1, extra=[decay])], condition=age(2)),
    ])


# ---------------------------------------------------------------------------- worldgen

def _patch(GW, rel, fn):
    path = os.path.join(GW.D, rel + '.json')
    with open(path) as f:
        obj = json.load(f)
    GW.w(rel, fn(obj) or obj)


def _features(GW):
    feature, placed, state, count, rarity, survive, BIOMEF = GW.feature, GW.placed, GW.state, GW.count, GW.rarity, GW.survive, GW.BIOME
    sq = {'type': 'minecraft:in_square'}
    vine = state('sky_vine', north=False, east=False, south=False, west=False, up=False, down=False)
    root = state('sky_root', axis='x')
    fruit = state('driftfruit')
    # roots and vines across the gaps between islands, and the lianas, roots and fruit hanging under them
    feature('sky_bridge', {'type': f'{NS}:sky_bridge', 'mode': 'bridge', 'vine': vine, 'root': root, 'fruit': fruit, 'max_span': 44,
                           'fruit_chance': 0.3})
    feature('sky_dangle', {'type': f'{NS}:sky_bridge', 'mode': 'dangle', 'vine': vine, 'root': root, 'fruit': fruit, 'max_span': 18,
                           'fruit_chance': 0.25})
    placed('sky_bridge', 'sky_bridge', [count(3), sq, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, BIOMEF])
    placed('sky_dangle', 'sky_dangle', [count(14), sq, {'type': 'minecraft:height_range', 'height': {
        'type': 'minecraft:uniform', 'min_inclusive': {'absolute': LOWEST + 24}, 'max_inclusive': {'absolute': HIGHEST - 20}}}, BIOMEF])
    # the trees
    feature('skypalm_tree', {'type': f'{NS}:sky_tree', 'style': 'skypalm', 'trunk': state('skypalm_log', axis='y'),
                             'leaves': state('skypalm_leaves', distance=7, persistent=False, waterlogged=False), 'vine': vine,
                             'fruit': state('skyrind_bunch', age=3), 'min_height': 7, 'max_height': 13})
    feature('cloud_tree', {'type': f'{NS}:sky_tree', 'style': 'cloud', 'trunk': state('sky_root', axis='y'),
                           'leaves': state('cloudpuff_leaves', distance=7, persistent=False, waterlogged=False), 'vine': vine,
                           'fruit': fruit, 'min_height': 5, 'max_height': 9})

    def tree(name, sapling):
        return {'feature': GW.rl(name), 'placement': [survive(sapling)]}
    feature('trees_sky_island', {'type': 'minecraft:random_selector', 'default': tree('skypalm_tree', 'skypalm_sapling'),
                                 'features': [{'chance': 0.42, 'feature': tree('cloud_tree', 'cloudpuff_sapling')}]})
    placed('trees_sky_island', 'trees_sky_island', [
        count({'type': 'minecraft:weighted_list', 'distribution': [{'data': 2, 'weight': 3}, {'data': 3, 'weight': 3}, {'data': 5, 'weight': 1}]}),
        sq, {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0}, {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'},
        BIOMEF])
    # ground cover
    on_top = [sq, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, BIOMEF]
    feature('cirrus_grass', {'type': 'minecraft:simple_block', 'to_place': GW.weighted([
        (state('cirrus_grass'), 8), (state('tall_cirrus_grass', half='lower'), 3)])})
    placed('patch_cirrus_grass', 'cirrus_grass', [count(7)] + on_top + GW.surface_patch(40, 6, 2) + [survive('cirrus_grass')])
    feature('fluffbush', {'type': 'minecraft:simple_block', 'to_place': state('fluffbush')})
    placed('patch_fluffbush', 'fluffbush', [count(2)] + on_top + GW.surface_patch(10, 4, 2) + [survive('fluffbush')])


def _biome(GW):
    feats = [(2, 'cloud_puff'), (4, 'sky_bridge'), (4, 'sky_dangle'), (9, 'trees_sky_island'), (9, 'patch_cirrus_grass'),
             (9, 'patch_fluffbush'), (9, 'patch_chime_bell'), (9, 'patch_organ_reed')]
    steps = [[] for _ in range(11)]
    for step, f in feats:
        steps[step].append(GW.rl(f))
    order = [GW.rl(n) for n in GW.PLACED]
    for st in steps:  # one global feature order in every biome (see gen_world.biome)
        st.sort(key=lambda f: (order.index(f) if f in order else len(order), f))
    music = {'max_delay': 15000, 'min_delay': 5000, 'replace_current_music': True, 'sound': f'{NS}:music.sound_garden'}
    spawns = GW.mobs(creature=[('sky_whale', 6, 1, 1), ('harmoner', 8, 2, 3), ('bulb', 4, 1, 3), ('enchoer', 1, 1, 1)], ambient=[('nib', 12, 2, 4)])
    GW.w(f'worldgen/biome/{BIOME}', {
        'attributes': {
            'minecraft:audio/ambient_sounds': {'loop': f'{NS}:ambient.sound_garden.loop',
                                               'additions': {'sound': f'{NS}:ambient.sound_garden.additions', 'tick_chance': 0.0125},
                                               'mood': {'block_search_extent': 8, 'offset': 2.0, 'sound': f'{NS}:ambient.sift.mood', 'tick_delay': 5000}},
            'minecraft:audio/background_music': {'default': music},
            'minecraft:gameplay/natural_mob_spawns': spawns,
            'minecraft:visual/ambient_particles': GW.particles(('white_fluff', 0.0025), ('sift_note', 0.003), ('sift_mist', 0.002),
                                                               ('star_sparkle', 0.002), ('wishing_star', 0.0004), ('dream_pollen', 0.001)),
            'minecraft:visual/fog_color': '#dcf7f3',
            'minecraft:visual/sky_color': '#86e4ec',
            'minecraft:visual/water_fog_color': '#bff3ff',
        },
        'carvers': [],
        'downfall': 0.6,
        'effects': {'water_color': '#bff3ff', 'grass_color': '#a8f0e8', 'foliage_color': '#c8f8f0'},
        'features': steps,
        'has_precipitation': False,
        'temperature': 0.6,
    })


def _placement(dim):
    """The sky itself: about fifty blocks and more over the ground (depth < -0.85, so the blend with the land
    biomes lies near -0.43), where every island top floats."""
    dim['generator']['biome_source']['biomes'].append({'biome': f'{NS}:{BIOME}', 'parameters': {
        'temperature': [-1.0, 1.0], 'humidity': [-1.0, 1.0], 'continentalness': [-1.2, 1.0], 'erosion': [-1.0, 1.0], 'depth': [-2.0, -0.85],
        'weirdness': [-1.0, 1.0], 'offset': 0.0}})
    return dim


def _surface(GW, rule):
    """Sky Grass on the islands, the Sift's soil under it, pale Dreamstone below and wisps of cloud clinging to the
    undersides."""
    def block(b, **props):
        return {'type': 'minecraft:block', 'result_state': GW.state(b, **props)}

    def cond(c, then):
        return {'type': 'minecraft:condition', 'if_true': c, 'then_run': then}
    cloudy = {'type': 'minecraft:noise_threshold', 'noise': 'minecraft:surface', 'min_threshold': 0.05, 'max_threshold': 1.0}
    mine = cond({'type': 'minecraft:biome', 'biome_is': [f'{NS}:{BIOME}']}, {'type': 'minecraft:sequence', 'sequence': [
        cond('minecraft:on_floor', block('sky_grass_block', snowy=False)),
        cond('minecraft:under_floor', block('sift_soil')),
        cond('minecraft:on_ceiling', cond(cloudy, block('cloud_block'))),
    ]})
    for r in rule['sequence']:
        if isinstance(r, dict) and r.get('type') == 'minecraft:condition' and r.get('if_true', {}).get('type') == 'minecraft:above_preliminary_surface':
            r['then_run']['sequence'].insert(0, mine)
            return rule
    raise ValueError('the_sift material rule: no above_preliminary_surface branch')


def world(GW):
    # the island layer reads the terrain offset; if that density function is ever renamed, fall back to vanilla's
    if not os.path.exists(os.path.join(GW.D, 'worldgen/density_function/sift/offset.json')):
        density(GW, 'minecraft:overworld/offset')
    _features(GW)
    _biome(GW)
    _patch(GW, 'dimension/the_sift', _placement)
    _patch(GW, 'worldgen/material_rule/the_sift', lambda r: _surface(GW, r))


def textures(out):
    __import__('sky_art').textures(out)
