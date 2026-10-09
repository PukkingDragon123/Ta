"""W-land: the ground of The Sift - the Rocky Dunes and the White Forest.

Rocky Dunes (SPEC 2, 6): real wind-shaped dunes of Chime Sand (the old Dreamsand family is gone; Chime
Sandstone replaced Dreamsandstone), huge rocky massifs, tall spires and flat-topped buttes striped with
rose Dunestone strata, hoodoos, arches and balanced rocks, and only two plants: the Rattlethorn (a spiky
bush that rattles and pricks whoever pushes through it) and the Tuning Cactus (a thin alien cactus that
grows like chorus into tuning-fork crowns and bears edible Tuning Fruit - `#thesift:jaberora_food`).

White Forest: White Lullwood is a full wood family now (pearl bark, pale planks) grown into tall weeping
trees; Rainbow Snow (layers and blocks whose colour drifts round the rainbow) dusts the forest floor and
falls through the air; three alien flowers, each with its own shape and trick: the Halo Lily (a floating
ring of petals that glows and heals when music plays), the Snowglobe (a glass bulb full of rainbow snow that
snows around itself; with Snow Blocks it crafts Rainbow Snow) and the Shiver Thistle (an icy thistle that
frosts whoever brushes through it).

Terrain: the ground density (thesift:sift/final_density) is the vanilla Overworld's with The Sift's offset,
plus a per-block rock term for the formations (see rock_formations()). Hooks (one line each): spec.py
(declare, WOODS), gen_assets (gen_block 'wland_*', assets), gen_world (terrain offset, rock term, surface,
world), gen_textures (wland_art.textures). Java: registry/ModWorldLand, block/{Rattlethorn,TuningCactus,
TuningCactusBud,RainbowSnow,FrostFlower}Block, worldgen/{RockFormation,TuningCactus,PaleTree,RainbowSnowfall}
Feature, client/WorldLandClient, client/particle/RainbowSnowParticle.
"""
import json
import os
import random

NS = 'thesift'
VD = os.environ.get('VANILLA_DATA', '/home/user/ref/mc-26.3-data-json/data/minecraft')


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# ============================================================================ terrain (gen_world.terrain_density)

# our noises: name -> (base octave, the vanilla noise whose octave layout and amplitude it copies). Each has its own id
# (so its own seed). The spires' and buttes' noises are sampled at xz_scale 1, so the surface rules can test the very
# same noise (noise_threshold) to cap the formations with rock instead of sand.
NOISES = {'dunes': (-6, 'surface'), 'dune_field': (-8, 'gravel'), 'massif': (-9, 'gravel'), 'massif_ridges': (-8, 'gravel'),
          'spires': (-4, 'surface'), 'spire_regions': (-8, 'gravel'), 'buttes': (-7, 'gravel'), 'crags': (-6, 'surface'),
          'strata': (-6, 'surface'), 'rock_grain': (-6, 'surface')}
SPIRE_CORE, BUTTE_EDGE = 0.40, 0.42  # where the spires' and buttes' walls stand (noise values)
MASSIF_FOOT, MASSIF_ROCK = 0.12, 0.27  # where the massifs start to rise, and where their rock breaks through the sand


def _df(t, **kw):
    return {'type': f'minecraft:{t}', **kw}


def _add(*xs):
    out = xs[0]
    for x in xs[1:]:
        out = _df('add', left=out, right=x)
    return out


def _mul(a, b):
    return _df('mul', left=a, right=b)


def _clamp01(x):
    return _df('clamp', input=x, min=0.0, max=1.0)


def _lin(x, at, per):
    """clamp((x - at) * per, 0, 1)"""
    return _clamp01(_mul(_add(x, -at), per))


def _n(name, xz, y=0.0):
    return _df('noise', noise=f'{NS}:{name}', xz_scale=xz, y_scale=y)


def _cache(x):
    return _df('cache', input=x)


def _ridge(x):
    """1 on the noise's zero line, falling away on both sides: sharp crests, wide troughs."""
    return _df('square', input=_add(1.0, _mul(_df('abs', input=x), -1.0)))


def desert_mask(temperature, humidity, continents, erosion):
    """0..1 where the climate makes Rocky Dunes (hot, dry, inland, not the eroded mountain belt), fading out at its edges."""
    return _cache(_mul(_mul(_lin(temperature, 0.25, 6.0), _lin(_mul(humidity, -1.0), -0.15, 6.0)),
                       _mul(_lin(continents, -0.05, 5.0), _lin(erosion, -0.32, 7.0))))


def massif():
    """0..1: the great rock massifs (broad, a few hundred blocks across)."""
    return _cache(_lin(_n('massif', 1.0), MASSIF_FOOT, 2.2))


def offset_term(write, temperature, humidity, continents, erosion):
    """The Rocky Dunes' share of the terrain offset (0.01 = ~1.3 blocks): ridged dunes of Chime Sand whose size varies from
    field to field, and the massifs - smooth-shouldered highlands up to ~65 blocks, scored by ridgelines. Also writes our
    noises and the mask/massif functions the rock term reuses."""
    for name, (octave, src) in NOISES.items():
        with open(os.path.join(VD, 'worldgen/noise', src + '.json')) as f:
            write(f'worldgen/noise/{name}', dict(json.load(f), base_octave=octave))
    write('worldgen/density_function/sift/rocky_dunes', desert_mask(temperature, humidity, continents, erosion))
    write('worldgen/density_function/sift/massif', massif())
    mask, m = f'{NS}:sift/rocky_dunes', f'{NS}:sift/massif'
    highland = _add(_mul(_df('square', input=m), 0.40), _mul(m, _mul(_ridge(_n('massif_ridges', 1.6)), 0.11)))
    write('worldgen/density_function/sift/highland', _cache(_mul(mask, highland)))
    field = _add(0.3, _mul(_lin(_n('dune_field', 1.1), -0.35, 1.4), 0.7))  # calm dune fields and great sand seas
    dunes = _mul(_mul(_ridge(_n('dunes', 1.35)), _mul(field, 0.09)), _add(1.0, _mul(m, -0.85)))
    return _add(f'{NS}:sift/highland', _mul(mask, dunes))


def rock_formations(GW):
    """Wraps the ground density: sift/final_density = max(ground, rocks), where `rocks` raises rock formations above the
    surface of the Rocky Dunes (per block, so they keep sharp edges):
      * spire forests: clusters of near-vertical needles 25-60 blocks tall, a narrower pinnacle on the thickest ones;
      * buttes: flat-topped towers 12-28 blocks high and 8-25 across;
      * crags: knuckles of rock all over the massifs.
    Their walls wander in and out with height (a strata profile, so they step like eroded layer-cake rock) and with a fine
    3D grain. h = blocks above the ground without its dunes (-128 x depth), so butte tops stay flat; a formation of height
    F is solid for -8 < h < F."""
    path = os.path.join(GW.D, 'worldgen/density_function/sift/final_density.json')
    with open(path) as f:
        ground = json.load(f)
    GW.w('worldgen/density_function/sift/ground', ground)
    mask = f'{NS}:sift/rocky_dunes'
    base = _cache(_add('minecraft:overworld/offset', f'{NS}:sift/highland'))
    depth_b = _mul(_add(_df('gradient', axis='y', from_coordinate=-64, from_value=1.5, to_coordinate=320, to_value=-1.5), base),
                   128.0)  # blocks below the dune-less ground
    F = formation_height(_add(_mul(_n('strata', 0.0, 9.0), 0.05), _mul(_n('rock_grain', 4.0, 4.0), 0.03)))
    inner = _df('min', left=_df('min', left=_add(F, depth_b), right=_add(8.0, _mul(depth_b, -1.0))), right=_mul(F, 4.0))
    rocks = _add(_mul(mask, _mul(inner, 0.1)), _add(mask, -1.0))
    # only worked out inside the Rocky Dunes and in the band of heights a formation can fill; never into the sky islands
    rocks = _df('range_choice', input=depth_b, min_inclusive=-80.0, max_exclusive=8.0, when_in_range=rocks, when_out_of_range=-1.0)
    rocks = _df('range_choice', input=mask, min_inclusive=1.0e-4, max_exclusive=2.0, when_in_range=rocks, when_out_of_range=-1.0)
    rocks = _df('min', left=rocks, right=_df('gradient', axis='y', from_coordinate=150, from_value=1.75, to_coordinate=250, to_value=-3.25))
    GW.w('worldgen/density_function/sift/rock_formations', rocks)
    GW.w('worldgen/density_function/sift/final_density', _df('max', left=f'{NS}:sift/ground', right=f'{NS}:sift/rock_formations'))


def formation_height(wob):
    """Height in blocks of the rock formations standing on each column (0 where there are none); `wob` (strata + grain)
    moves their walls in and out with height."""
    m = f'{NS}:sift/massif'
    s = _cache(_n('spires', 1.0))
    regions = _cache(_lin(_n('spire_regions', 0.9), 0.12, 3.0))
    tall = _cache(_add(24.0, _mul(_lin(_n('spire_regions', 3.1), -0.3, 1.6), 22.0)))
    spires = _mul(_mul(regions, _add(1.0, _mul(m, -0.45))),
                  _add(_mul(tall, _lin(_add(s, wob), SPIRE_CORE, 14.0)), _mul(_lin(_add(s, wob), 0.52, 9.0), 15.0)))
    b = _cache(_n('buttes', 1.0))
    high = _cache(_add(18.0, _mul(_lin(_n('buttes', 0.27), -0.3, 1.6), 26.0)))
    buttes = _mul(high, _lin(_add(b, _mul(wob, 0.6)), BUTTE_EDGE, 16.0))
    crags = _mul(m, _mul(_lin(_add(_cache(_n('crags', 4.0)), wob), 0.22, 5.0), 9.0))
    return _add(spires, buttes, crags)


# ============================================================================ the Rocky Dunes' surface (gen_world.noise_settings)

def _cond(c, then):
    return {'type': 'minecraft:condition', 'if_true': c, 'then_run': then}


def _seq(*rules):
    return {'type': 'minecraft:sequence', 'sequence': list(rules)}


def _block(state):
    return {'type': 'minecraft:block', 'result_state': state}


def _y_above(y, mult=1):
    return {'type': 'minecraft:y_above', 'anchor': {'absolute': y}, 'surface_depth_multiplier': mult, 'add_stone_depth': False}


def _noise_at_least(name, v):
    return {'type': 'minecraft:noise_threshold', 'noise': f'{NS}:{name}', 'min_threshold': v, 'max_threshold': 10.0}


def strata(state):
    """Every rock wall of the Rocky Dunes is striped in level bands like a layer cake: thick rose Dunestone, thin dark
    Banded Dunestone, pale Dreamstone and Chime Sandstone. The bands wave gently with the surface noise."""
    rnd = random.Random(11)
    mats = [('dunestone', 5.0, 3, 8), ('banded_dunestone', 2.2, 1, 3), ('dreamstone', 1.3, 2, 4), ('chime_sandstone', 1.0, 1, 3)]
    rules, y, prev = [], 40, None
    while y < 250:
        options = [m for m in mats if m[0] != prev]
        pick = rnd.choices(options, [m[1] for m in options])[0]
        y += rnd.randint(pick[2], pick[3])
        rules.append(_cond({'type': 'minecraft:not', 'invert': _y_above(y)}, _block(state(pick[0]))))
        prev = pick[0]
    return _seq(*rules, _block(state('dunestone')))


def dunes_surface(state):
    """Surface rule for the Rocky Dunes (inside above_preliminary_surface): Chime Sand on the dunes and Chime Sandstone
    under them; striped rock wherever a massif, butte or spire stands (the surface tests the same noises the terrain
    raised them with), on steep faces and high up."""
    rock = strata(state)
    formations = [_noise_at_least('massif', MASSIF_ROCK), _noise_at_least('buttes', BUTTE_EDGE - 0.03),
                  _noise_at_least('spires', SPIRE_CORE - 0.03)]
    high = _y_above(108, 2)
    floor = _seq(*[_cond(c, rock) for c in formations], _cond({'type': 'minecraft:steep'}, rock), _cond(high, rock),
                 _block(state('chime_sand')))
    fill = _seq(*[_cond(c, rock) for c in formations], _cond(high, rock), _block(state('chime_sandstone')))
    return _seq(_cond('minecraft:on_floor', floor), _cond('minecraft:under_floor', floor), fill)


# ============================================================================ blocks and items (spec.py)

# W-land: White Lullwood becomes a full wood family (its leaves and sapling were A2's; the trunk was grey Lullwood)
WOODS = {'white_lullwood': dict(bark='MapColor.QUARTZ', plank='MapColor.SNOW', leaves_particle='ModSwifter.WHITE_FLUFF', leaf_chance='0.04F',
                                sapling_on='sift', grower='ModSwifter.WHITE_LULLWOOD',
                                leaves_props='BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_LEAVES).mapColor(MapColor.SNOW)')}
SANDSTONE = 'BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE).mapColor(MapColor.QUARTZ)'
STONE = 'BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)'
FLOWER = 'BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)'
FLOWERS = {'halo_lily': 'HaloLily', 'snowglobe_bloom': 'Snowglobe', 'shiver_thistle': 'ShiverThistle'}


def _stone_set(block, base, props, kind='cube', stairs=True, slab=True, wall=True):
    block(base, kind, props, tex=base, tags=['pickaxe'])
    if stairs:
        block(base + '_stairs', 'stairs', props, basis=base, tex=base, tags=['pickaxe'])
    if slab:
        block(base + '_slab', 'slab', props, basis=base, tex=base, tags=['pickaxe'], loot='slab')
    if wall:
        block(base + '_wall', 'wall', props, basis=base, tex=base, tags=['pickaxe', 'walls'])


def declare(block, item):
    # --- the Rocky Dunes: Chime Sandstone (replaces Dreamsandstone), Suspicious Chime Sand, the striped Dunestone rock
    block('suspicious_chime_sand', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SUSPICIOUS_SAND).mapColor(MapColor.QUARTZ)',
          cls='SuspiciousChimeSandBlock', model='suspicious', tags=['shovel'], loot='none', item_kind='block', tab='nature')
    _stone_set(block, 'chime_sandstone', SANDSTONE, kind='sandstone')
    _stone_set(block, 'smooth_chime_sandstone', SANDSTONE, wall=False)
    _stone_set(block, 'cut_chime_sandstone', SANDSTONE, kind='sandstone_cut', stairs=False, wall=False)
    block('chiseled_chime_sandstone', 'sandstone_chiseled', SANDSTONE, tags=['pickaxe'])
    _stone_set(block, 'dunestone', STONE + '.mapColor(MapColor.TERRACOTTA_PINK)')
    block('banded_dunestone', 'custom', STONE + '.mapColor(MapColor.TERRACOTTA_RED)', cls='Block', model='cube_column', tags=['pickaxe'])
    # the two desert plants
    block('rattlethorn', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).mapColor(MapColor.TERRACOTTA_PINK)',
          cls='RattlethornBlock', model='cross', tags=['sword_efficient'], loot='none', tab='nature')
    block('tuning_cactus', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.CHORUS_PLANT).mapColor(MapColor.COLOR_CYAN)',
          cls='TuningCactusBlock', model='wland_cactus', loot='silk', tab='nature')
    block('tuning_cactus_bud', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.CHORUS_FLOWER).mapColor(MapColor.COLOR_CYAN)',
          cls='TuningCactusBudBlock', model='wland_cactus_bud', loot='none', tab='nature')
    item('tuning_fruit', props='new Item.Properties().food(ModWorldLand.TUNING_FRUIT, ModWorldLand.TUNING_FRUIT_CONSUMABLE)')
    # --- the White Forest: Rainbow Snow and three frost flowers (White Lullwood comes through WOODS)
    block('rainbow_snow', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW).mapColor(MapColor.SNOW)', cls='RainbowSnowBlock',
          model='wland_snow', tags=['shovel'], loot='none', tab='nature')
    block('rainbow_snow_block', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW_BLOCK).mapColor(MapColor.SNOW)', cls='Block',
          model='wland_snow_block', tags=['shovel'], loot='none', tab='nature')
    lights = {'halo_lily': '.lightLevel(s -> 9)', 'snowglobe_bloom': '.lightLevel(s -> 5)', 'shiver_thistle': ''}
    for f, cls in FLOWERS.items():
        block(f, 'custom', FLOWER + lights[f], cls=f'FrostFlowerBlock.{cls}', model=f'wland_{f}', tags=['flowers', 'small_flowers'], tab='nature')


# ============================================================================ models (gen_assets.gen_block, 'wland_*')

def _el(frm, to, faces, rot=None, shade=None):
    e = {'from': frm, 'to': to, 'faces': faces}
    if rot:
        e['rotation'] = {'origin': [8, 8, 8], 'axis': 'y', 'angle': rot, 'rescale': True}
    if shade:
        e['shade_direction_override'] = shade
    return e


def _box(frm, to, tex, skip=(), cull=(), uv=None):
    faces = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d not in skip:
            faces[d] = {'texture': tex, 'cullface': d} if d in cull else {'texture': tex}
            if uv:
                faces[d]['uv'] = uv
    return _el(frm, to, faces)


def _cross(tex, lo=0.8, hi=15.2, y0=0, y1=16):
    """Vanilla's two crossed planes (cross.json), optionally narrowed/shortened (the texture is cropped to match)."""
    uv = [lo, 16 - y1, hi, 16 - y0]
    return [_el([lo, y0, 8], [hi, y1, 8], {'north': {'uv': uv, 'texture': tex}, 'south': {'uv': uv, 'texture': tex}}, 45, 'up'),
            _el([8, y0, lo], [8, y1, hi], {'west': {'uv': uv, 'texture': tex}, 'east': {'uv': uv, 'texture': tex}}, 45, 'up')]


def _model(GA, name, textures, elements):
    m = {'ambientocclusion': False, 'textures': {k: GA.rl(v) for k, v in textures.items()}, 'elements': elements}
    GA.note_textures(m)
    GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)


def _arm_parts(sides):
    parts = []
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        if d in sides:
            apply = {'model': f'{NS}:block/tuning_cactus_side'}
            if y:
                apply.update(y=y, uvlock=True)
            parts.append({'when': {d: 'true'}, 'apply': apply})
    for d in ('up', 'down'):
        if d in sides:
            parts.append({'when': {d: 'true'}, 'apply': {'model': f'{NS}:block/tuning_cactus_{d}'}})
    return parts


def gen_block(GA, b):
    bid, k = b['id'], b['model']
    state_path = os.path.join(GA.A, 'blockstates', bid + '.json')
    if k == 'wland_cactus':
        t = {'side': 'block/tuning_cactus', 'particle': 'block/tuning_cactus'}
        _model(GA, 'tuning_cactus_core', t, [_box([5, 5, 5], [11, 11, 11], '#side')])
        _model(GA, 'tuning_cactus_side', t, [_box([5, 5, 0], [11, 11, 5], '#side', skip=('south',), cull=('north',))])
        _model(GA, 'tuning_cactus_up', t, [_box([5, 11, 5], [11, 16, 11], '#side', skip=('down',), cull=('up',))])
        _model(GA, 'tuning_cactus_down', t, [_box([5, 0, 5], [11, 5, 11], '#side', skip=('up',), cull=('down',))])
        GA.write(state_path, {'multipart': [{'apply': {'model': f'{NS}:block/tuning_cactus_core'}}]
                              + _arm_parts(('north', 'east', 'south', 'west', 'up', 'down'))})
        GA.item_block(bid, 'tuning_cactus_core')
    elif k == 'wland_cactus_bud':
        t = {'side': 'block/tuning_cactus', 'bud': 'block/tuning_cactus_bud', 'crown': 'block/tuning_cactus_crown',
             'fruit': 'block/tuning_fruit_pod', 'particle': 'block/tuning_cactus_bud'}
        _model(GA, 'tuning_cactus_bud', t, [_box([5, 5, 5], [11, 11, 11], '#side'), _box([6, 11, 6], [10, 14, 10], '#bud'),
                                            _box([7, 14, 7], [9, 15, 9], '#bud')])
        fork = [_box([5, 5, 5], [11, 9, 11], '#side'), _box([4, 9, 7], [12, 11, 9], '#crown')]
        _model(GA, 'tuning_cactus_crown', t, fork + [_box([4, 11, 7], [6, 16, 9], '#crown'), _box([10, 11, 7], [12, 16, 9], '#crown')])
        _model(GA, 'tuning_cactus_crown_fruit', t, fork + [_box([4, 11, 7], [6, 12, 9], '#crown'), _box([10, 11, 7], [12, 12, 9], '#crown'),
                                                           _box([3, 12, 6], [7, 16, 10], '#fruit'), _box([9, 12, 6], [13, 16, 10], '#fruit')])
        body = [{'when': {'age': '0|1|2|3|4'}, 'apply': {'model': f'{NS}:block/tuning_cactus_bud'}}]
        for fruit, m in (('false', 'tuning_cactus_crown'), ('true', 'tuning_cactus_crown_fruit')):
            body.append({'when': {'age': '5', 'fruit': fruit}, 'apply': [{'model': f'{NS}:block/{m}'}, {'model': f'{NS}:block/{m}', 'y': 90}]})
        GA.write(state_path, {'multipart': body + _arm_parts(('north', 'east', 'south', 'west', 'down'))})
        GA.item_block(bid, 'tuning_cactus_bud')
    elif k == 'wland_snow':
        variants = {}
        for layers in range(1, 9):
            ms = []
            for v in range(4):
                name = f'rainbow_snow_height{layers * 2}_{v}'
                if layers < 8:
                    GA.block_model(name, f'minecraft:block/snow_height{layers * 2}',
                                   {'particle': f'block/rainbow_snow_{v}', 'texture': f'block/rainbow_snow_{v}'})
                else:
                    GA.block_model(name, 'minecraft:block/cube_all', {'particle': f'block/rainbow_snow_{v}', 'all': f'block/rainbow_snow_{v}'})
                ms.append({'model': f'{NS}:block/{name}'})
            variants[f'layers={layers}'] = ms
        GA.write(state_path, {'variants': variants})
        GA.item_block(bid, 'rainbow_snow_height2_0')
    elif k == 'wland_snow_block':
        for v in range(4):
            GA.block_model(f'rainbow_snow_block_{v}', 'minecraft:block/cube_all', {'all': f'block/rainbow_snow_{v}'})
        GA.write(state_path, {'variants': {'': [{'model': f'{NS}:block/rainbow_snow_block_{v}'} for v in range(4)]}})
        GA.item_block(bid, 'rainbow_snow_block_0')
    elif k == 'wland_halo_lily':
        # a stem, and a ring of petals floating round a glowing bud
        t = {'stem': f'block/{bid}_stem', 'halo': f'block/{bid}_halo', 'bud': f'block/{bid}_bud', 'particle': f'block/{bid}_halo'}
        ring = _el([1, 10, 1], [15, 10, 15], {'up': {'uv': [1, 1, 15, 15], 'texture': '#halo'}, 'down': {'uv': [1, 1, 15, 15], 'texture': '#halo'}})
        _model(GA, bid, t, _cross('#stem', 3.2, 12.8, 0, 13) + [ring, _box([7, 10, 7], [9, 14, 9], '#bud')])
        _flower_state(GA, bid)
    elif k == 'wland_snowglobe_bloom':
        # a stem holding up a glass globe with a swirl of rainbow snow inside
        t = {'stem': f'block/{bid}_stem', 'glass': f'block/{bid}_glass', 'core': f'block/{bid}_core', 'particle': f'block/{bid}_glass'}
        _model(GA, bid, t, _cross('#stem', 3.2, 12.8, 0, 9) + [_box([6, 5, 6], [10, 6, 10], '#core'), _box([6, 7, 6], [10, 11, 10], '#core'),
                                                              _box([4, 6, 4], [12, 14, 12], '#glass', uv=[4, 4, 12, 12])])
        _flower_state(GA, bid)
    elif k == 'wland_shiver_thistle':
        # a frosted stalk under a spiky ice-blue head bristling with spines
        t = {'stem': f'block/{bid}_stem', 'head': f'block/{bid}_head', 'spikes': f'block/{bid}_spikes', 'particle': f'block/{bid}_head'}
        _model(GA, bid, t, _cross('#stem', 2.4, 13.6, 0, 11) + [_box([6, 9, 6], [10, 13, 10], '#head')] + _cross('#spikes', 2.0, 14.0, 6, 16))
        _flower_state(GA, bid)
    else:
        raise ValueError(f'wland: no model {k} for {bid}')


def _flower_state(GA, bid):
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': {'resonating=false': {'model': f'{NS}:block/{bid}'},
                                                                            'resonating=true': {'model': f'{NS}:block/{bid}'}}})
    GA.item_generated(bid, f'item/{bid}')


# ============================================================================ recipes, loot, tags, text (gen_assets.generate)

def assets(GA):
    import gen_data as GD
    rl, tag = GA.rl, GA.tag
    # loot: the Rattlethorn drops sticks (itself to shears), snow drops snowballs (itself with Silk Touch), the bud itself + its fruit
    GA.loot('rattlethorn', 'dead_bush', {'dead_bush': 'rattlethorn'})
    GA.loot('rainbow_snow', 'snow', {'snow': 'rainbow_snow'})
    GA.loot('rainbow_snow_block', 'snow_block', {'snow_block': 'rainbow_snow_block'})
    fruit = {'type': 'minecraft:match_block', 'blocks': rl('tuning_cactus_bud'), 'state': {'fruit': 'true'}}
    GD.table('block', 'blocks/tuning_cactus_bud', [
        GD.pool([GD.item('tuning_cactus_bud')], condition={'type': 'minecraft:survives_explosion'}),
        GD.pool([GD.item('tuning_fruit', count=(1, 2))], condition=fruit)])
    # recipes: rainbow snow from a Snowglobe and snow; dyes from the frost flowers
    GA.shaped('rainbow_snow_block', ['SSS', 'SGS', 'SSS'], {'S': 'minecraft:snow_block', 'G': 'snowglobe_bloom'}, 'rainbow_snow_block', 8)
    GA.shaped('rainbow_snow', ['###'], {'#': 'rainbow_snow_block'}, 'rainbow_snow', 6, 'building')
    GA.shapeless('light_blue_dye_from_halo_lily', ['halo_lily'], 'minecraft:light_blue_dye', 2, 'misc', 'light_blue_dye')
    GA.shapeless('cyan_dye_from_shiver_thistle', ['shiver_thistle'], 'minecraft:cyan_dye', 2, 'misc', 'cyan_dye')
    # tags
    for b in ('rainbow_snow', 'rainbow_snow_block'):
        tag('block', 'minecraft:snow', rl(b))
    for f in FLOWERS:
        tag('block', f'{NS}:resonant', rl(f))
    tag('item', f'{NS}:jaberora_food', rl('tuning_fruit'))  # Phase 4: the Jaberoras' favourite
    for b in ('chime_sandstone', 'dunestone', 'banded_dunestone', 'chime_sand'):
        tag('block', 'minecraft:overworld_carver_replaceables', rl(b))
    tag('block', 'minecraft:sniffer_diggable_block', rl('chime_sand'))
    # particles
    texs = [f'{NS}:rainbow_snowflake_{i}' for i in range(3)]
    GA.write(os.path.join(GA.A, 'particles', 'rainbow_snowflake.json'), {'textures': texs})
    for t in texs:
        GA.TEXTURES.add('particle/' + t.split(':')[1])
    c = f'codex.{NS}'
    GA.LANG.update({
        f'{c}.rocky_dunes.title': 'Rocky Dunes', f'{c}.rocky_dunes.tagline': 'Singing sand, striped stone, two prickly plants',
        f'{c}.rocky_dunes.body': (
            'A hot, dry sea of Chime Sand that tinkles under every step, piled into wind-carved dunes. Out of it rise huge rock '
            'massifs, flat-topped buttes and whole forests of tall spires, all striped rose, plum and bone like a layer cake: '
            'Dunestone, Banded Dunestone, Dreamstone and Chime Sandstone. Hoodoos balance caps on thin necks, arches span the '
            'sand. Only two plants live here. The Rattlethorn is a dry spiky bush whose seed pods rattle like a snare drum - '
            'push through it and it pricks you. The Tuning Cactus grows like chorus, branch by branch, into tuning-fork crowns '
            'that ring when you tap them, and bears Tuning Fruit: pick it (use the crown), eat it and you hop like a Jaberora. '
            'Plant a bud on sand to grow your own. Sifters sift the dunes for treasure; Suspicious Chime Sand hides relics.'),
        f'{c}.rainbow_snow.title': 'Rainbow Snow', f'{c}.rainbow_snow.tagline': 'Snow that cannot settle on one colour',
        f'{c}.rainbow_snow.body': (
            'In the White Forest the snow is never quite white: every flake drifts slowly round the rainbow as it falls, and the '
            'drifts on the ground and on the pale treetops shimmer from rose to mint to lilac. It melts by torchlight like any '
            'snow, and a shovel turns it into Snowballs. A Snowglobe in the middle of eight Snow Blocks makes eight blocks of '
            'Rainbow Snow; three blocks make six layers.'),
        f'{c}.frost_flowers.title': 'Frost Flowers', f'{c}.frost_flowers.tagline': "The White Forest's three strange blooms",
        f'{c}.frost_flowers.body': (
            'Halo Lily: a ring of petals floats over a glowing bud and lights the snow around it. Play music nearby and it hums '
            'along, healing everyone close by. Makes Light Blue Dye.\n'
            'Snowglobe: a glass bulb with a swirl of rainbow snow inside. Snow slowly gathers around it; shake it (use it) for a '
            'flurry. With eight Snow Blocks it makes Rainbow Snow.\n'
            'Shiver Thistle: an icy thistle that frosts anything brushing through it, slowing it to a shiver - unless it wears '
            'leather boots. Makes Cyan Dye.'),
    })


# ============================================================================ worldgen (gen_world.generate, before biomes())

DUNES_FEATURES = [(2, 'dune_hoodoo'), (2, 'dune_arch'), (2, 'balanced_rock'), (9, 'patch_rattlethorn'), (9, 'tuning_cactus')]
WHITE_FOREST_FEATURES = [(9, 'patch_halo_lily'), (9, 'patch_snowglobe_bloom'), (9, 'patch_shiver_thistle'), (10, 'rainbow_snowfall')]


def world(GW):
    feature, placed, state, count, rarity, survive, BIOME = GW.feature, GW.placed, GW.state, GW.count, GW.rarity, GW.survive, GW.BIOME
    sq = {'type': 'minecraft:in_square'}

    def hm(h):
        return {'type': 'minecraft:heightmap', 'heightmap': h}
    # --- the Rocky Dunes' rock formations (worldgen/RockFormationFeature), banded like the terrain's strata
    layers = [state('dunestone'), state('dunestone'), state('banded_dunestone'), state('dunestone'), state('dreamstone'),
              state('dunestone'), state('chime_sandstone')]
    feature('dune_hoodoo', {'type': f'{NS}:rock_formation', 'style': 'hoodoo', 'layers': layers, 'cap': state('banded_dunestone'),
                            'min_height': 7, 'max_height': 15})
    feature('dune_arch', {'type': f'{NS}:rock_formation', 'style': 'arch', 'layers': layers, 'cap': state('dunestone'),
                          'min_height': 7, 'max_height': 11})
    feature('balanced_rock', {'type': f'{NS}:rock_formation', 'style': 'balanced', 'layers': layers, 'cap': state('dreamstone'),
                              'min_height': 3, 'max_height': 7})
    placed('dune_hoodoo', 'dune_hoodoo', [rarity(3), sq, hm('WORLD_SURFACE_WG'), BIOME])
    placed('dune_arch', 'dune_arch', [rarity(12), sq, hm('WORLD_SURFACE_WG'), BIOME])
    placed('balanced_rock', 'balanced_rock', [rarity(6), sq, hm('WORLD_SURFACE_WG'), BIOME])
    # --- the two desert plants
    feature('rattlethorn', {'type': 'minecraft:simple_block', 'to_place': state('rattlethorn')})
    placed('patch_rattlethorn', 'rattlethorn', [count(2), sq, hm('WORLD_SURFACE_WG'), BIOME] + GW.surface_patch(10, 5, 2) + [survive('rattlethorn')])
    feature('tuning_cactus', {'type': f'{NS}:tuning_cactus', 'max_spread': 4})
    per_chunk = {'type': 'minecraft:weighted_list', 'distribution': [{'data': 0, 'weight': 2}, {'data': 1, 'weight': 3}, {'data': 2, 'weight': 1}]}
    placed('tuning_cactus', 'tuning_cactus', [count(per_chunk), sq, hm('WORLD_SURFACE_WG'), BIOME])
    # --- the White Forest: tall weeping White Lullwoods (the sapling grows these too: ModSwifter.WHITE_LULLWOOD)
    log = state('white_lullwood_log', axis='y')
    leaves = state('white_lullwood_leaves', distance=7, persistent=False, waterlogged=False)
    hanging = state('hanging_lullwood_leaves', tip=True)
    feature('white_lullwood_tree', {'type': f'{NS}:pale_tree', 'trunk': log, 'leaves': leaves, 'hanging': hanging, 'style': 'weeping',
                                    'min_height': 8, 'max_height': 12})
    feature('grand_white_lullwood_tree', {'type': f'{NS}:pale_tree', 'trunk': log, 'leaves': leaves, 'hanging': hanging, 'style': 'grand',
                                          'min_height': 14, 'max_height': 19})
    for f, (n, tries) in {'halo_lily': (rarity(2), 18), 'snowglobe_bloom': (rarity(4), 10), 'shiver_thistle': (count(1), 14)}.items():
        feature(f, {'type': 'minecraft:simple_block', 'to_place': state(f)})
        placed(f'patch_{f}', f, [n, sq, hm('WORLD_SURFACE_WG'), BIOME] + GW.surface_patch(tries, 5, 2) + [survive(f)])
    feature('rainbow_snowfall', {'type': f'{NS}:rainbow_snowfall', 'max_layers': 3})
    placed('rainbow_snowfall', 'rainbow_snowfall', [hm('MOTION_BLOCKING'), BIOME])
