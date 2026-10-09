"""W-sea: the Sift's oceans - the Brass Coral Reef, the Chrome Coral Ocean, clearer water and Chrome everywhere, and a
smooth meeting of water and Chrome.

* Brass Coral Reef (`brass_coral_reef`, real water): a warm, humid shelf sea over Copper Sand (smelts into vanilla
  Copper). Living Trumpet Coral in four metals - Brass (gold), Silver, Copper (orange) and Verdigris (oxidized green) -
  grows in branching instrument shapes: tubes that bend and fork and end in flared bells. Every bell is a note of the
  reef's pentatonic chord: they toot on their own under water, and whenever something swims through them or a player
  taps them (the notes reach the song tracker). Bells grow (bone-meal free, slowly, under water), can be placed, mined
  and eaten. Tube Seaweed sways in the gaps, green Algae coats the rocks and the coral.
* Chrome Coral Ocean (`chrome_coral_ocean`, Chrome): the warm, humid deep sea. Its Chrome is a clear, sparkling
  green-copper (client tint and a far thinner fog), over pale Chime Sand. Reefs of Bubble Coral in six colours (Rose,
  Amber, Lime, Azure, Violet, Pearl - each a block, a coral and a fan, all mineable building blocks) grow into strange
  shapes - trees, domes, striped spires, arches, bubble clusters - with glowing Rainbow Anemones between them. No
  seaweed at all.
* Water meets Chrome gently: Chrome is heavier than water, so where a water sea borders a Chrome sea the Chrome sinks
  into a bed under the water that thins out over ~12 blocks (worldgen/SeaFloodFeature's `blend`), instead of a sheer
  wall; the Chrome there takes on the water's colour (client/SeaReefsClient) and the fogs match.
* Every sea is clearer: longer water fog in the kelp forest and the reef, a little more in the Sculk Ocean; Chrome is
  more see-through everywhere (texture alpha) and its fog reaches 56 blocks (128 in the Chrome Coral Ocean).

Hooks (one line each): spec.py -> declare(block, item); gen_assets.gen_block ('wsea_*' models) -> gen_block(GA, b);
gen_assets.generate() -> assets(GA) (before gen_sounds; its sound check reads ModSeaReefs.java); gen_world.generate()
-> world(GW) (after the other seas); gen_textures.main() -> sea_reefs_art.textures(out) (before vanilla_remap).
Java: registry/ModSeaReefs, block/SeaLogging, block/TrumpetCoral*, block/BubbleCoral*, block/RainbowAnemoneBlock,
block/TubeSeaweedBlock, worldgen/TrumpetCoralFeature, worldgen/SiftReefFeature, worldgen/SeaFloodFeature (blend),
client/SeaReefsClient.
"""
from __future__ import annotations

import json
import os

NS = 'thesift'
METALS = ('brass', 'silver', 'copper', 'verdigris')
METAL_NAMES = {'brass': 'Brass', 'silver': 'Silver', 'copper': 'Copper', 'verdigris': 'Verdigris'}
METAL_MAP = {'brass': 'MapColor.GOLD', 'silver': 'MapColor.METAL', 'copper': 'MapColor.COLOR_ORANGE', 'verdigris': 'MapColor.WARPED_STEM'}
BUBBLES = ('rose', 'amber', 'lime', 'azure', 'violet', 'pearl')
BUBBLE_MAP = {'rose': 'MapColor.COLOR_PINK', 'amber': 'MapColor.COLOR_ORANGE', 'lime': 'MapColor.COLOR_LIGHT_GREEN',
              'azure': 'MapColor.COLOR_LIGHT_BLUE', 'violet': 'MapColor.COLOR_PURPLE', 'pearl': 'MapColor.QUARTZ'}
BUBBLE_DYES = {'rose': 'pink', 'amber': 'orange', 'lime': 'lime', 'azure': 'light_blue', 'violet': 'purple', 'pearl': 'white'}
REEF = 'brass_coral_reef'
CHROME_OCEAN = 'chrome_coral_ocean'
BIOMES = (REEF, CHROME_OCEAN)
# the seas of real water (worldgen/SeaFloodFeature turns their Chrome into water) - the reef joins the two older ones
WATER_SEAS = ('magic_kelp_forest', 'deep_dark_ocean', REEF)
# how far the water sinks into a neighbouring Chrome sea (and the Chrome into the water), in blocks
BLEND = 12


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def trumpet_ids(m):
    return f'{m}_trumpet_coral_block', f'{m}_trumpet_coral', f'{m}_trumpet_coral_bell'


def bubble_ids(c):
    return f'{c}_bubble_coral_block', f'{c}_bubble_coral', f'{c}_bubble_coral_fan', f'{c}_bubble_coral_wall_fan'


# ============================================================================ spec (registries)

def declare(block, item):
    # Copper Sand: the reef's floor, smelts into a vanilla Copper Ingot
    block('copper_sand', 'falling', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_ORANGE)', dust='0xC8703C',
          tags=['shovel', 'sand'], tab='nature')
    # Trumpet Coral: a block, its branching tube and its flared, growing (and edible) bell, in four metals
    for m in METALS:
        blk, pipe, bell = trumpet_ids(m)
        name = METAL_NAMES[m]
        base = f'BlockBehaviour.Properties.ofFullCopy(Blocks.TUBE_CORAL_BLOCK).mapColor({METAL_MAP[m]}).sound(SoundType.COPPER)'
        block(blk, 'custom', base, cls='TrumpetCoralBlock', model='cube_all', tags=['pickaxe'], tab='nature', name=f'{name} Trumpet Coral Block')
        block(pipe, 'custom', f'BlockBehaviour.Properties.ofFullCopy(Blocks.TUBE_CORAL_BLOCK).mapColor({METAL_MAP[m]}).sound(SoundType.COPPER)'
              '.strength(0.8F).noOcclusion()', cls='TrumpetCoralPipeBlock', model='wsea_pipe', tags=['pickaxe'], tab='nature',
              name=f'{name} Trumpet Coral')
        block(bell, 'custom', f'BlockBehaviour.Properties.ofFullCopy(Blocks.TUBE_CORAL).mapColor({METAL_MAP[m]}).sound(SoundType.COPPER)'
              '.strength(0.3F).noOcclusion()', cls='TrumpetCoralBellBlock', model='wsea_bell', item=False, loot='self', tab='nature',
              name=f'{name} Trumpet Coral Bell')
        # the bell is the coral you carry: place it on any face to start a new branch, or eat it
        item(bell, cls='BlockItem', factory=f'p -> new BlockItem(ModBlocks.{bell.upper()}.get(), p)',
             props='new Item.Properties().useBlockDescriptionPrefix().food(com.thesift.registry.ModSeaReefs.TRUMPET_BELL_FOOD, '
                   'com.thesift.registry.ModSeaReefs.TRUMPET_BELL_CONSUMABLE)', tab='nature', name=f'{name} Trumpet Coral Bell')
    # Tube Seaweed and Algae (the reef's greens)
    block('tube_seaweed', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.KELP_PLANT).mapColor(MapColor.PLANT)', cls='TubeSeaweedBlock',
          model='wsea_seaweed', tab='nature')
    block('algae', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.GLOW_LICHEN).mapColor(MapColor.PLANT).lightLevel(s -> 0)',
          cls='GlowLichenBlock', model='wsea_algae', loot='none', tab='nature')
    # Bubble Coral: six colours, each a block, a coral and a fan (standing or on a wall); they never die, in water or in Chrome
    for c in BUBBLES:
        blk, plant, fan, wall = bubble_ids(c)
        cname = c.capitalize()
        block(blk, 'cube', f'BlockBehaviour.Properties.ofFullCopy(Blocks.BUBBLE_CORAL_BLOCK).mapColor({BUBBLE_MAP[c]})', tags=['pickaxe'],
              tab='nature', name=f'{cname} Bubble Coral Block')
        soft = f'BlockBehaviour.Properties.ofFullCopy(Blocks.BUBBLE_CORAL).mapColor({BUBBLE_MAP[c]})'
        block(plant, 'custom', soft, cls='BubbleCoralBlock', model='wsea_cross', tab='nature', name=f'{cname} Bubble Coral')
        block(fan, 'custom', soft, cls='BubbleCoralFanBlock', model='wsea_fan', item=False, loot='self', tab='nature',
              name=f'{cname} Bubble Coral Fan')
        block(wall, 'custom', soft, cls='BubbleCoralWallFanBlock', model='wsea_wall_fan', item=False, loot='none',
              name=f'{cname} Bubble Coral Wall Fan')
        item(fan, cls='StandingAndWallBlockItem', tab='nature', name=f'{cname} Bubble Coral Fan',
             factory=f'p -> new StandingAndWallBlockItem(ModBlocks.{fan.upper()}.get(), ModBlocks.{wall.upper()}.get(), '
                     'net.minecraft.core.Direction.DOWN, p)',
             props='new Item.Properties().useBlockDescriptionPrefix()')
    # the Chrome Coral Ocean's glowing, rainbow-tipped anemones
    block('rainbow_anemone', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.BUBBLE_CORAL).mapColor(MapColor.COLOR_MAGENTA).lightLevel(s -> 10)',
          cls='RainbowAnemoneBlock', model='wsea_cross', tab='nature')


# ============================================================================ block models (gen_assets.gen_block)

def _faces(tex, cull=None, skip=()):
    out = {}
    for f in ('north', 'south', 'east', 'west', 'up', 'down'):
        if f in skip:
            continue
        out[f] = {'texture': tex}
        if cull == f:
            out[f]['cullface'] = f
    return out


def _model(GA, name, textures, elements, parent='minecraft:block/block'):
    m = {'parent': parent, 'ambientocclusion': False, 'textures': {k: rl(v) for k, v in textures.items()}, 'elements': elements}
    GA.note_textures(m)
    GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
    return f'{NS}:block/{name}'


def _pipe(GA, bid):
    """A Trumpet Coral tube: a 6-pixel brass tube that joins every neighbour of its family (and the sea floor), like a chorus
    plant."""
    tex = {'particle': f'block/{bid}', 'pipe': f'block/{bid}'}
    core = _model(GA, bid + '_core', tex, [{'from': [5, 5, 5], 'to': [11, 11, 11], 'faces': _faces('#pipe')}])
    side = _model(GA, bid + '_side', tex, [{'from': [5, 5, 0], 'to': [11, 11, 5], 'faces': _faces('#pipe', cull='north', skip=('south',))}])
    parts = [{'apply': {'model': core}}]
    rot = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    for d, r in rot.items():
        parts.append({'when': {d: 'true'}, 'apply': {'model': side, 'uvlock': True, **r}})
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'multipart': parts})
    # in the hand: a length of tube with one branch
    _model(GA, bid + '_inventory', tex, [{'from': [5, 0, 5], 'to': [11, 16, 11], 'faces': _faces('#pipe')},
                                         {'from': [11, 8, 5], 'to': [16, 14, 11], 'faces': _faces('#pipe', skip=('west',))}])
    GA.item_block(bid, bid + '_inventory')


def _bell(GA, bid):
    """The bell at the end of a tube: a short stem flaring out in three steps to a wide lip; looking in you see the dark
    throat (the model points up; the blockstate turns it to face any way, like an end rod)."""
    metal = bid.split('_')[0]
    tex = {'particle': f'block/{bid}', 'pipe': f'block/{metal}_trumpet_coral', 'bell': f'block/{bid}', 'mouth': f'block/{bid}_mouth'}
    def flare(top=None):
        # the banded flare round the sides; plain tube metal underneath; the mouth on top of the lip
        f = {**_faces('#bell', skip=('up', 'down')), 'down': {'texture': '#pipe'}}
        if top:
            f['up'] = top
        return f
    els = [
        {'from': [5, 0, 5], 'to': [11, 5, 11], 'faces': _faces('#pipe', skip=('up',))},
        {'from': [4, 5, 4], 'to': [12, 8, 12], 'faces': flare()},
        {'from': [3, 8, 3], 'to': [13, 10, 13], 'faces': flare()},
        {'from': [1, 10, 1], 'to': [15, 12, 15], 'faces': flare({'uv': [1, 1, 15, 15], 'texture': '#mouth'})},
    ]
    model = _model(GA, bid, tex, els)
    rot = {'up': {}, 'down': {'x': 180}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180}, 'east': {'x': 90, 'y': 90}, 'west': {'x': 90, 'y': 270}}
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'),
             {'variants': {f'facing={d}': {'model': model, **r} for d, r in rot.items()}})


def gen_block(GA, b):
    bid, k = b['id'], b['model']
    st = os.path.join(GA.A, 'blockstates', bid + '.json')
    if k == 'wsea_pipe':
        _pipe(GA, bid)
    elif k == 'wsea_bell':
        _bell(GA, bid)
    elif k == 'wsea_cross':
        GA.block_model(bid, 'minecraft:block/cross', {'cross': f'block/{bid}'})
        GA.simple_state(bid)
        GA.item_generated(bid, f'block/{bid}')
    elif k == 'wsea_fan':
        GA.block_model(bid, 'minecraft:block/coral_fan', {'fan': f'block/{bid}'})
        GA.simple_state(bid)
    elif k == 'wsea_wall_fan':
        GA.block_model(bid, 'minecraft:block/coral_wall_fan', {'fan': f'block/{bid.replace("_wall_fan", "_fan")}'})
        GA.write(st, {'variants': {f'facing={f}': ({'model': f'{NS}:block/{bid}', 'y': y} if y else {'model': f'{NS}:block/{bid}'})
                                   for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
    elif k == 'wsea_seaweed':
        GA.block_model(bid, 'minecraft:block/cross', {'cross': f'block/{bid}'})
        GA.block_model(bid + '_tip', 'minecraft:block/cross', {'cross': f'block/{bid}_tip'})
        GA.write(st, {'variants': {'tip=false': {'model': f'{NS}:block/{bid}'}, 'tip=true': {'model': f'{NS}:block/{bid}_tip'}}})
        GA.item_generated(bid)
    elif k == 'wsea_algae':
        GA.copy_template('glow_lichen', bid, GA.token_tex('glow_lichen', bid))
    else:
        raise ValueError(f'no W-sea asset rule for {bid} ({k})')


# ============================================================================ sounds

SOUNDS = {
    # each metal its own voice: Mojang's copper trumpet notes, from bright brass to the muffled green of verdigris
    'block.trumpet_coral.brass': [('block/note_block/trumpet', 1.0, 1.0)],
    'block.trumpet_coral.silver': [('block/note_block/trumpet_weathered', 1.0, 1.0)],
    'block.trumpet_coral.copper': [('block/note_block/trumpet_exposed', 1.0, 1.0)],
    'block.trumpet_coral.verdigris': [('block/note_block/trumpet_oxidized', 1.0, 1.0)],
    # a bell pushing out a new length of tube: a gurgle of bubbles
    'block.trumpet_coral.grow': [('ambient/underwater/additions/bubbles4', 0.7, 1.1), ('ambient/underwater/additions/bubbles5', 0.7, 0.9),
                                 ('block/bubble_column/bubble2', 0.6, 0.8)],
    'music.brass_coral_reef': [('music/game/water/axolotl', 0.6, 1.0), ('music/game/water/dragon_fish', 0.6, 1.0),
                               ('music/game/water/shuniji', 0.6, 1.0), ('music/game/left_to_bloom', 0.5, 1.0)],
    'music.chrome_coral_ocean': [('music/game/water/shuniji', 0.6, 1.0), ('music/game/floating_dream', 0.6, 1.0),
                                 ('music/game/komorebi', 0.55, 1.0), ('music/game/infinite_amethyst', 0.6, 1.0)],
    # the reef hums with bubbles, clicking life and now and then a far-off brass note; the Chrome sea shimmers and drips
    'ambient.brass_coral_reef.loop': [('ambient/underwater/underwater_ambience', 0.35, 1.15)],
    'ambient.brass_coral_reef.additions': [('ambient/underwater/additions/bubbles4', 0.45, 1.1), ('ambient/underwater/additions/bubbles6', 0.45, 1.2),
                                           ('ambient/underwater/additions/animal2', 0.4, 1.25), ('ambient/underwater/additions/crackles1', 0.4, 1.0),
                                           ('ambient/underwater/additions/crackles2', 0.4, 1.1), ('block/note_block/trumpet', 0.22, 0.8),
                                           ('block/note_block/trumpet_exposed', 0.2, 1.2)],
    'ambient.chrome_coral_ocean.loop': [('ambient/underwater/underwater_ambience', 0.3, 1.35)],
    'ambient.chrome_coral_ocean.additions': [('block/amethyst/shimmer', 0.45, 1.3), ('ambient/underwater/additions/driplets1', 0.4, 1.2),
                                             ('ambient/underwater/additions/driplets2', 0.4, 1.35), ('block/amethyst/resonate1', 0.35, 1.5),
                                             ('ambient/underwater/additions/bubbles5', 0.4, 1.4)],
}
SUBTITLES = {
    'block.trumpet_coral.brass': 'Trumpet Coral toots', 'block.trumpet_coral.silver': 'Trumpet Coral sings',
    'block.trumpet_coral.copper': 'Trumpet Coral blares', 'block.trumpet_coral.verdigris': 'Trumpet Coral hoots',
    'block.trumpet_coral.grow': 'Trumpet Coral grows', 'ambient.brass_coral_reef.loop': 'The reef murmurs',
    'ambient.brass_coral_reef.additions': 'The reef plays', 'ambient.chrome_coral_ocean.loop': 'Chrome hums',
    'ambient.chrome_coral_ocean.additions': 'Chrome shimmers',
}
STREAM = {'music.brass_coral_reef', 'music.chrome_coral_ocean', 'ambient.brass_coral_reef.loop', 'ambient.chrome_coral_ocean.loop'}


# ============================================================================ assets, recipes, loot, tags, text (gen_assets.generate)

def assets(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    GA.STREAM.update(STREAM)
    shaped, shapeless, smelt, tag, LANG = GA.shaped, GA.shapeless, GA.smelt, GA.tag, GA.LANG
    # --- recipes: Copper Sand smelts into vanilla Copper; four lengths of tube make a block; coral fans make dyes
    smelt('copper_ingot_from_copper_sand', 'copper_sand', 'minecraft:copper_ingot', 0.35, 200, ('smelting', 'blasting'))
    smelt('dried_kelp_from_tube_seaweed', 'tube_seaweed', 'minecraft:dried_kelp', 0.1, 200, ('smelting', 'smoking'))
    smelt('dried_kelp_from_tube_seaweed_campfire', 'tube_seaweed', 'minecraft:dried_kelp', 0.1, 600, ('campfire_cooking',))
    for m in METALS:
        blk, pipe, bell = trumpet_ids(m)
        shaped(blk, ['##', '##'], {'#': pipe}, blk, 1, 'building')
        tag('block', 'minecraft:coral_blocks', rl(blk))
        tag('block', f'{NS}:resonant', rl(bell))
        tag('item', f'{NS}:trumpet_coral_bells', rl(bell))
    for c in BUBBLES:
        blk, plant, fan, wall = bubble_ids(c)
        shaped(blk, ['##', '##'], {'#': plant}, blk, 1, 'building')
        dye = BUBBLE_DYES[c]
        shapeless(f'{dye}_dye_from_{fan}', [fan], f'minecraft:{dye}_dye', 1, 'misc', f'{dye}_dye')
        GA.loot(wall, 'dirt', {'dirt': fan})
        tag('block', 'minecraft:coral_blocks', rl(blk))
        for b in (plant, fan, wall):
            tag('block', 'minecraft:sword_efficient', rl(b))
            tag('block', f'{NS}:bubble_corals', rl(b))
    shapeless('green_dye_from_algae', ['algae', 'algae'], 'minecraft:green_dye', 1, 'misc', 'green_dye')
    shapeless('magenta_dye_from_rainbow_anemone', ['rainbow_anemone'], 'minecraft:magenta_dye', 2, 'misc', 'magenta_dye')
    # --- loot: Algae, like Glow Lichen, comes off with shears, one per face
    GA.loot('algae', 'glow_lichen', {'glow_lichen': 'algae'})
    # --- tags
    for b in ('tube_seaweed', 'rainbow_anemone', 'algae'):
        tag('block', 'minecraft:sword_efficient', rl(b))
    tag('block', f'{NS}:sift_plantable', rl('copper_sand'))
    for b in BIOMES:
        tag('worldgen/biome', f'{NS}:is_sift', rl(b))
    tag('worldgen/biome', 'minecraft:allows_tropical_fish_spawns_at_any_height', rl(REEF))
    # --- text
    LANG.update({
        f'biome.{NS}.{REEF}': 'Brass Coral Reef', f'biome.{NS}.{CHROME_OCEAN}': 'Chrome Coral Ocean',
        f'codex.{NS}.{REEF}.title': 'Brass Coral Reef', f'codex.{NS}.{REEF}.tagline': 'A reef that plays itself',
        f'codex.{NS}.{REEF}.body': 'A warm, clear shelf sea over Copper Sand (smelt it into Copper). Its coral is living brass: Trumpet Coral '
                                   'in gold Brass, Silver, orange Copper and green Verdigris grows in tubes that bend and fork like the '
                                   'pipes of a horn and end in flared bells. Every bell sounds its own note of the reef\'s chord - on its '
                                   'own now and then, and whenever something swims through it or you tap it, so you can play a song on '
                                   'a reef. Bells grow slowly under water; plant one on any face to start a new branch, or eat it. Tube '
                                   'Seaweed sways between the corals and green Algae coats the rocks. Kazoo Fish and tropical fish crowd '
                                   'the water.',
        f'codex.{NS}.{CHROME_OCEAN}.title': 'Chrome Coral Ocean', f'codex.{NS}.{CHROME_OCEAN}.tagline': 'A garden at the bottom of the Chrome',
        f'codex.{NS}.{CHROME_OCEAN}.body': 'The warm deep sea is Chrome, not water - clear and sparkling, tinted a green-copper, and so thin a '
                                           'haze that you can see far along its floor. You will not swim in it: you sink, slowly and safely, '
                                           'to a garden of Bubble Coral in rose, amber, lime, azure, violet and pearl, grown into trees, '
                                           'domes, striped spires, arches and clusters, with glowing Rainbow Anemones between them. Every '
                                           'Bubble Coral block, coral and fan can be mined for building. Sneak to wade back up. Where the '
                                           'Chrome meets a water sea it sinks under the water as a shining bed.',
        f'codex.{NS}.sea_and_sky.body': 'Three of the Sift\'s seas hold real water. The warm Magic Kelp Forest glows with rose, azure and amber '
                                        'glowkelp over coral-pink sand, full of Kazoo Fish. The Brass Coral Reef plays itself on living '
                                        'brass. The cold Sculk Ocean is dark teal water over trenches, ridges and glowing Sculk Coral reefs - '
                                        'mind the Gobblers. Every other sea is Chrome, heavier than water: where the two meet, the Chrome '
                                        'sinks beneath the water. Far above the land is the Sound Garden: cloud islands where Chime Bells ring '
                                        'and Organ Reeds hum, and Sky Whales sing.',
    })


# ============================================================================ worldgen (gen_world.generate, after the other seas)

def _patch(GW, rel, fn):
    path = os.path.join(GW.D, rel + '.json')
    with open(path) as f:
        obj = json.load(f)
    obj = fn(obj) or obj
    GW.w(rel, obj)


def _features(GW):
    feature, placed, state, count, rarity, survive, BIOME = GW.feature, GW.placed, GW.state, GW.count, GW.rarity, GW.survive, GW.BIOME
    sq = {'type': 'minecraft:in_square'}
    floor = {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'}

    def fluid(f):
        return {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_fluids', 'fluids': f}}
    in_water, in_chrome = fluid('minecraft:water'), fluid(rl('chrome'))

    def noisy(factor, ratio):
        return {'type': 'minecraft:noise_based_count', 'noise_factor': factor, 'noise_to_count_ratio': ratio}
    blocks = [state(trumpet_ids(m)[0]) for m in METALS]
    pipes = [state(trumpet_ids(m)[1]) for m in METALS]
    bells = [state(trumpet_ids(m)[2]) for m in METALS]
    # ------------------------------------------------------------------ the Brass Coral Reef (placed in this order: rock, horns, weed, algae)
    # the reef's rock: mounds, arches and clusters of metal coral, crowned with bells (worldgen/SiftReefFeature)
    feature('brass_reef', {'type': f'{NS}:sift_reef', 'blocks': blocks, 'tops': bells, 'sides': bells, 'top_chance': 0.22,
                           'side_chance': 0.08, 'shapes': ['dome', 'cluster', 'arch', 'dome', 'tree']})
    placed('brass_reef', 'brass_reef', [noisy(220.0, 4), sq, floor, in_water, BIOME])
    # instrument-shaped growths: organ ranks, branching horns, trumpets and brass bushes of tube and bell (worldgen/TrumpetCoralFeature)
    feature('trumpet_coral', {'type': f'{NS}:trumpet_coral', 'blocks': blocks, 'pipes': pipes, 'bells': bells})
    placed('trumpet_coral', 'trumpet_coral', [noisy(140.0, 7), sq, floor, in_water, BIOME])
    placed('trumpet_coral_sparse', 'trumpet_coral', [count(1), sq, floor, in_water, BIOME])
    # Tube Seaweed in swaying clumps, Algae over the rocks and the coral
    feature('tube_seaweed', {'type': 'minecraft:block_column', 'direction': 'up', 'prioritize_tip': True,
                             'allowed_placement': {'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water'},
                             'layers': [{'height': {'type': 'minecraft:biased_to_bottom', 'min_inclusive': 1, 'max_inclusive': 6},
                                         'provider': state('tube_seaweed', tip=False, waterlogged=True)},
                                        {'height': 1, 'provider': state('tube_seaweed', tip=True, waterlogged=True)}]})
    placed('patch_tube_seaweed', 'tube_seaweed', [noisy(90.0, 14), sq, floor, in_water, survive('tube_seaweed'), BIOME])
    feature('reef_algae', {'type': 'minecraft:multiface_growth', 'block': rl('algae'), 'can_be_placed_on': [
        rl(b) for b in [trumpet_ids(m)[0] for m in METALS] + ['copper_sand', 'dreamstone', 'cobbled_dreamstone', 'coral_sand']],
        'can_place_on_ceiling': True, 'can_place_on_floor': True, 'can_place_on_wall': True, 'chance_of_spreading': 0.75, 'search_range': 8})
    placed('reef_algae', 'reef_algae', [count(26), sq, floor, {'type': 'minecraft:offset', 'x': 0, 'y': {'type': 'minecraft:uniform', 'min_inclusive': 0,
                                                                                                          'max_inclusive': 4}, 'z': 0},
                                        in_water, BIOME])
    # ------------------------------------------------------------------ the Chrome Coral Ocean
    bubble_blocks = [state(bubble_ids(c)[0]) for c in BUBBLES]
    tops = [state(bubble_ids(c)[1]) for c in BUBBLES] + [state(bubble_ids(c)[2]) for c in BUBBLES]
    sides = [state(bubble_ids(c)[3]) for c in BUBBLES]
    feature('bubble_reef', {'type': f'{NS}:sift_reef', 'blocks': bubble_blocks, 'tops': tops, 'sides': sides, 'top_chance': 0.45,
                            'side_chance': 0.2, 'shapes': ['tree', 'dome', 'spire', 'arch', 'cluster', 'tree', 'spire']})
    placed('bubble_reef', 'bubble_reef', [noisy(160.0, 9), sq, floor, in_chrome, BIOME])
    placed('bubble_reef_sparse', 'bubble_reef', [count(2), sq, floor, in_chrome, BIOME])
    garden = [(state(bubble_ids(c)[1], chromelogged=True), 3) for c in BUBBLES] + [(state(bubble_ids(c)[2], chromelogged=True), 3) for c in BUBBLES]
    garden += [(state('rainbow_anemone', chromelogged=True), 4)]
    feature('bubble_coral_garden', {'type': 'minecraft:simple_block', 'to_place': GW.weighted(garden)})
    placed('bubble_coral_garden', 'bubble_coral_garden', [count(40), sq, floor, in_chrome, BIOME])
    feature('rainbow_anemone', {'type': 'minecraft:simple_block', 'to_place': state('rainbow_anemone', chromelogged=True)})
    placed('patch_rainbow_anemone', 'rainbow_anemone', [count(4), sq, floor, in_chrome, BIOME] + GW.surface_patch(18, 4, 1)[:2] + [in_chrome])
    # a few verdigris horns grow down here too, in the green Chrome
    placed('trumpet_coral_in_chrome', 'trumpet_coral', [rarity(2), sq, floor, in_chrome, BIOME])


def _biomes(GW):
    sea = __import__('sea_sky')
    common = GW.COMMON_UNDERGROUND
    sea._biome(GW, REEF, fog='#c4f2dc', sky='#7fdcea', water='#3fdcc4', water_fog='#1a9e8c', grass='#d9b26a', foliage='#e0c070',
               temp=0.9, down=0.8, music='music.brass_coral_reef', loop='ambient.brass_coral_reef.loop',
               additions='ambient.brass_coral_reef.additions', mood='ambient.sift.mood',
               spawns=sea._spawns(water_ambient=[('kazoo_fish', 14, 4, 7), ('minecraft:tropical_fish', 12, 4, 8)],
                                  water_creature=[('fanfare_eel', 2, 1, 1)]),
               parts=[('minecraft:underwater', 0.004), ('sift_note', 0.0012), ('glow_dust', 0.002), ('chrome_bubble', 0.0015)],
               extra={'minecraft:visual/water_fog_end_distance': {'argument': 1.6, 'modifier': 'multiply'}},
               feats=[(0, 'sea_flood'), (0, 'biome_blend')] + common
                     + [(9, 'brass_reef'), (9, 'trumpet_coral'), (9, 'trumpet_coral_sparse'), (9, 'patch_tube_seaweed'), (9, 'reef_algae'),
                        (9, 'minecraft:sea_pickle')])
    sea._biome(GW, CHROME_OCEAN, fog='#bff5e2', sky='#6fdcd0', water='#5fe0a8', water_fog='#2fae86', grass='#7fe0b0', foliage='#86e9c2',
               temp=0.8, down=0.8, music='music.chrome_coral_ocean', loop='ambient.chrome_coral_ocean.loop',
               additions='ambient.chrome_coral_ocean.additions', mood='ambient.sift.mood',
               spawns=sea._spawns(water_ambient=[('kazoo_fish', 12, 3, 7)], water_creature=[('fanfare_eel', 3, 1, 2)]),
               parts=[('chrome_bubble', 0.004), ('star_sparkle', 0.003), ('glow_dust', 0.0025), ('wishing_star', 0.0002)],
               feats=[(0, 'sea_flood'), (0, 'biome_blend')] + common
                     + [(9, 'bubble_reef'), (9, 'bubble_reef_sparse'), (9, 'bubble_coral_garden'), (9, 'patch_rainbow_anemone'),
                        (9, 'trumpet_coral_in_chrome')])


def _placement(dim):
    """Warm, humid seas: the shallow shelf is the Brass Coral Reef, the deep the Chrome Coral Ocean; the Magic Kelp Forest keeps
    the drier warm seas (shallow and deep). Cold seas stay Chrome Lakes and the Sculk Ocean."""
    pts = dim['generator']['biome_source']['biomes']

    def pt(b, t, h, c):
        return {'biome': rl(b), 'parameters': {'temperature': t, 'humidity': h, 'continentalness': c, 'erosion': [-1.0, 1.0], 'depth': 0.0,
                                               'weirdness': [-1.0, 1.0], 'offset': 0.0}}
    for p in pts:
        if p['biome'] == rl('magic_kelp_forest') and not isinstance(p['parameters'].get('depth'), list):
            p['parameters']['humidity'] = [-1.0, 0.0]
    if not any(p['biome'] == rl(REEF) for p in pts):
        pts.append(pt(REEF, [0.1, 1.0], [0.0, 1.0], [-0.455, -0.19]))
        pts.append(pt(CHROME_OCEAN, [0.1, 1.0], [0.0, 1.0], [-1.2, -0.455]))
    return dim


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
        # the reef: Copper Sand, pale sand drifts, rocky outcrops where the floor steepens
        cond(biome_is(REEF), seq(
            cond('minecraft:on_floor', seq(cond(steep, block('cobbled_dreamstone')),
                                           cond(noise('minecraft:surface', 0.42), block('chime_sand')),
                                           cond(noise('minecraft:gravel', -1.0, -0.7), block('cobbled_dreamstone')),
                                           block('copper_sand'))),
            cond('minecraft:under_floor', block('copper_sand')),
            cond('minecraft:deep_under_floor', block('chime_sandstone')))),
        # the Chrome garden: pale Chime Sand with drifts of Copper Sand
        cond(biome_is(CHROME_OCEAN), seq(
            cond('minecraft:on_floor', seq(cond(steep, block('dreamstone')),
                                           cond(noise('minecraft:surface', 0.38), block('copper_sand')),
                                           block('chime_sand'))),
            cond('minecraft:under_floor', block('chime_sand')),
            cond('minecraft:deep_under_floor', block('chime_sandstone')))),
    ]
    for r in rule['sequence']:
        if isinstance(r, dict) and r.get('type') == 'minecraft:condition' and r.get('if_true', {}).get('type') == 'minecraft:above_preliminary_surface':
            r['then_run']['sequence'][0:0] = mine
            return rule
    raise ValueError('the_sift material rule: no above_preliminary_surface branch')


def _sort_step(GW, step):
    order = [rl(n) for n in GW.PLACED]
    step.sort(key=lambda f: (order.index(f) if f in order else len(order), f))


def _seas(GW, dim):
    """Water and Chrome meet gently: the reef floods too, every flood blends into its Chrome neighbours, and every surface biome
    runs the flood (a cheap look at the climate when no water sea is near), so no chunk border cuts a seam."""
    def flood(obj):
        obj['biomes'] = sorted(set(obj['biomes']) | {rl(b) for b in WATER_SEAS})
        obj['blend'] = BLEND
        return obj
    _patch(GW, 'worldgen/feature/sea_flood', flood)
    surface = sorted({p['biome'] for p in dim['generator']['biome_source']['biomes'] if not isinstance(p['parameters'].get('depth'), list)})
    for b in surface:
        ns, path = b.split(':', 1)
        if ns != NS or not os.path.exists(os.path.join(GW.D, f'worldgen/biome/{path}.json')):
            continue

        def add(obj):
            if rl('sea_flood') not in obj['features'][0]:
                obj['features'][0].append(rl('sea_flood'))
                _sort_step(GW, obj['features'][0])
            return obj
        _patch(GW, f'worldgen/biome/{path}', add)
    # the seams of sea floor blend like any other biome edge (worldgen/BiomeBlendFeature)

    def blend(obj):
        mine = {rl(REEF): ('copper_sand', 'copper_sand'), rl(CHROME_OCEAN): ('chime_sand', 'chime_sand')}
        if not any(set(p['biomes']) & set(mine) for p in obj['palettes']):
            for b, (top, under) in mine.items():
                obj['palettes'].append({'biomes': [b], 'top': GW.state(top), 'under': GW.state(under)})
        return obj
    _patch(GW, 'worldgen/feature/biome_blend', blend)

    # clearer water: the kelp forest sees further, the Sculk Ocean a little further than its gloom used to allow
    def clearer(mult):
        def fn(obj):
            obj['attributes']['minecraft:visual/water_fog_end_distance'] = {'argument': mult, 'modifier': 'multiply'}
            return obj
        return fn
    _patch(GW, 'worldgen/biome/magic_kelp_forest', clearer(1.35))
    _patch(GW, 'worldgen/biome/deep_dark_ocean', clearer(0.65))

    # a little green Algae on the kelp forest's sea floor too
    def algae(obj):
        if rl('reef_algae') not in obj['features'][9]:
            obj['features'][9].append(rl('reef_algae'))
            _sort_step(GW, obj['features'][9])
        return obj
    _patch(GW, 'worldgen/biome/magic_kelp_forest', algae)


def world(GW):
    _features(GW)
    _biomes(GW)
    _patch(GW, 'dimension/the_sift', _placement)
    _patch(GW, 'worldgen/material_rule/the_sift', lambda r: _surface(GW, r))
    with open(os.path.join(GW.D, 'dimension/the_sift.json')) as f:
        _seas(GW, json.load(f))
