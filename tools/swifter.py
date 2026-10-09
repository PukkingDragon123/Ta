"""A2 Swifter & White Forest: the White Forest biome, the Swifter (a fierce, fluffy three-tailed
cloud fox), its dens, cubs and hunting.

Everything generated for it lives here (art in tools/swifter_art.py), hooked from one line each in
spec.py (blocks and items), mobs.py (the model), gen_assets.py (sounds, the den model, recipes, tags,
loot, text), gen_world.py (worldgen), gen_textures.py and items16.py (art). The Java side is
registry/ModSwifter, entity/Swifter, block/SwifterDenBlock, worldgen/SwifterDenFeature and
client/SwifterClient.
"""
import json
import os

from modelkit import Model

NS = 'thesift'


# ============================================================================ blocks and items


def declare(block, item):
    # the White Forest: snow-pale turf, white-leaved lullwood, puffball flowers
    block('white_turf', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.SNOW)',
          cls='SiftGrassBlock', model='grass_block', tags=['shovel', 'dirt'], loot='silk:sift_soil', tab='nature')
    # (W-land: White Lullwood's leaves, sapling and potted sapling are declared with its wood family, tools/wland.py WOODS)
    block('puffbloom', 'flower', 'BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)', effect='MobEffects.SLOW_FALLING', secs='6.0F', light=0,
          tags=['flowers', 'small_flowers'], cls='SiftFlowerBlock', tab='nature')
    block('potted_puffbloom', 'pot', 'BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY)', plant='puffbloom', item=False,
          loot='pot:puffbloom')
    # the Swifters' den: a nest of fluff and twigs (its loot table is written in assets())
    block('swifter_den', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).mapColor(MapColor.SNOW).strength(0.8F)'
          '.sound(SoundType.WOOL).noOcclusion()', cls='SwifterDenBlock', model='swifter_den', tags=['hoe'], loot='none', tab='nature')
    item('swifter_fluff')
    item('swifter_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModSwifter.SWIFTER.get())', tab='eggs')


# ============================================================================ the model


def _rows(spec, w, h):
    return [((spec.get(y, '')) + '.' * w)[:w] for y in range(h)]


def _fur(color='fur', **kw):
    d = dict(color=color, pattern='mc', clusters=0.2, streaks=0.3)
    d.update(kw)
    return d


FACE_KEYS = {'l': 'lid', 'E': 'eye', 'P': 'pupil', 'w': 'white', 'T': 'tear', 'b': 'blush'}

# the adult's face: 14 x 12 texels, the snout covers the lower middle. Fierce almond eyes under
# brows that slant down towards the nose, ice-blue irises, slit pupils.
ADULT_FACE = {
    'neutral': {3: '.ll........ll.', 4: '..lll....lll..', 5: '.wEEPl..lPEEw.', 6: '..EEPl..lPEE..', 7: '...ll....ll...'},
    'blink': {3: '.ll........ll.', 4: '..lll....lll..', 5: '.lllll..lllll.'},
    'angry': {3: '.l..........l.', 4: '.lll......lll.', 5: '..llll..llll..', 6: '..wEPl..lPEw..', 7: '...ll....ll...'},
    'sleep': {5: '.l...l..l...l.', 6: '..lll....lll..'},
    'hurt': {4: '.l..........l.', 5: '..ll......ll..', 6: '.l..........l.'},
    'happy': {4: '...ll....ll...', 5: '..l..l..l..l..', 7: '.bb........bb.'},
    'dead': {4: '.l.l......l.l.', 5: '..l........l..', 6: '.l.l......l.l.'},
}
# the cub's face: 10 x 9 texels, big round eyes; crying, it screws them shut and the tears run
CUB_FACE = {
    'neutral': {3: '.EEE..EEE.', 4: '.wPE..wPE.', 5: '.EPE..EPE.'},
    'blink': {4: '.lll..lll.'},
    'angry': {3: '.l......l.', 4: '..ll..ll..', 5: '.EPE..EPE.'},
    'sleep': {4: '.l.l..l.l.', 5: '..l....l..'},
    'hurt': {3: '.l......l.', 4: '..l....l..', 5: '.l......l.', 6: '.T......T.', 7: '.T......T.', 8: 'T........T'},
    'happy': {4: '..l....l..', 5: '.l.l..l.l.', 6: 'b........b'},
    'dead': {3: '.l.l..l.l.', 4: '..l....l..', 5: '.l.l..l.l.'},
}
SIDES = (('left', 1), ('right', -1))
TAILS = (('left', 1.0), ('middle', 0.0), ('right', -1.0))


def _face(rows, w, h):
    return dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True, map=_rows(rows['neutral'], w, h), keys=FACE_KEYS,
                expr={k: _rows(v, w, h) for k, v in rows.items() if k != 'neutral'})


def _adult(m):
    """The grown Swifter, about wolf-sized: a lean body under a great collar of cloud fur, fluffy
    cheeks and haunches, tall pointed ears, three big cloud tails rising behind."""
    root = m.part('adult', pivot=(0, 15, 0))
    body = root.part('body')
    body.cube((-3.5, -3.5, -6), (7, 7, 13), **_fur(bands=[(5, 'belly')], fringe=1), faces={
        'down': dict(color='belly_d', pattern='mc', clusters=0.1), 'up': _fur(clusters=0.3, streaks=0.4)})
    body.cube((-4.5, -4.5, -7.5), (9, 9, 5), **_fur('fluff', fringe=2, streaks=0.45, bands=[(6, 'belly')]))   # the cloud collar
    body.cube((-4, -2.5, 3), (8, 5.5, 4.5), **_fur('fluff', fringe=1))                                          # fluffy haunches
    body.cube((-2.5, -4.5, -1.5), (5, 1, 5), **_fur('fluff', rim=False))                                       # cloud lumps on the back
    body.cube((-2, -4.25, 4), (4, 1, 3), **_fur('fluff', rim=False))
    head = root.part('head', pivot=(0, -2, -7.5))
    head.cube((-3.5, -4, -5), (7, 6, 5), **_fur(clusters=0.12, streaks=0.15), faces={'north': _face(ADULT_FACE, 14, 12)})
    head.cube((-4.5, -1.5, -4), (9, 4, 3.5), **_fur('fluff', fringe=1, streaks=0.4))                          # cheek ruff
    head.cube((-1.5, -0.25, -8), (3, 2.25, 3), **_fur(clusters=0.0, streaks=0.0, rim=False), faces={
        'north': dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True, map=['.NNNN.', '..NN..', '..mm..', '.m..m.'],
                      keys={'N': 'nose', 'm': 'mouth'}),
        'down': dict(color='belly', pattern='mc', clusters=0.0)})
    jaw = head.part('jaw', pivot=(0, 2, -5))
    jaw.cube((-1.25, -0.25, -2.75), (2.5, 1, 2.75), **_fur('belly', clusters=0.0, streaks=0.0, rim=False))
    for side, sx in SIDES:
        ear = head.part(f'{side}_ear', pivot=(2.1 * sx, -4, -1.5), rot=(-0.12, 0, 0.16 * sx))
        ear.cube((-1.5, -3.5, -0.5), (3, 3.5, 1.25), **_fur(clusters=0.0, streaks=0.0, rim=False), faces={
            'north': dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True,
                          map=['......', '..ii..', '..ii..', '.iiii.', '.iiii.', '.iiii.', '.iiii.'], keys={'i': 'ear_in'})})
        tip = ear.part(f'{side}_ear_tip', pivot=(0, -3.5, 0.1))
        tip.cube((-0.85, -2.25, -0.4), (1.7, 2.25, 1), **_fur(clusters=0.0, streaks=0.0, rim=False), faces={
            'north': dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True, map=['....', '....', '.ii.', '.ii.'], keys={'i': 'ear_in'})})
    for name, px, pz, back in (('front_left_leg', 2, -4, False), ('front_right_leg', -2, -4, False),
                               ('back_left_leg', 2, 4.5, True), ('back_right_leg', -2, 4.5, True)):
        leg = root.part(name, pivot=(px, 3, pz))
        if back:
            leg.cube((-1.5, -1.5, -1.75), (3, 4.5, 3.5), **_fur('fluff', fringe=1))
            leg.cube((-1, 3, -0.5), (2, 3, 2), **_fur(bands=[(1, 'sock')], streaks=0.1))
        else:
            leg.cube((-1, 0, -1), (2, 6, 2), **_fur(bands=[(4, 'sock')], streaks=0.15))
        leg.cube((-1.25, 5.25, -1.6), (2.5, 0.75, 2.6), **_fur('sock', clusters=0.0, streaks=0.0, rim=False))
    for side, sx in TAILS:
        base = root.part(f'tail_{side}', pivot=(1.3 * sx, -2.5, 6.5), rot=(0.42 if sx else 0.6, 0.56 * sx, 0))
        base.cube((-1.5, -1.5, 0), (3, 3, 4), **_fur('shade', rim=False))
        mid = base.part(f'tail_{side}_mid', pivot=(0, 0, 3.5), rot=(0.12, 0, 0))
        mid.cube((-2.5, -2.5, 0), (5, 5, 5.5), **_fur('fluff', streaks=0.45))
        mid.cube((-2, -3.25, 1), (4, 1, 3.5), **_fur('fluff', rim=False))
        tip = mid.part(f'tail_{side}_tip', pivot=(0, 0, 5), rot=(0.1, 0, 0))
        tip.cube((-2, -2, 0), (4, 4, 4.5), **_fur('tip', streaks=0.2))
        tip.cube((-1.25, -1.25, 4.5), (2.5, 2.5, 1.25), **_fur('tip', rim=False))


def _cub(m):
    """A cub: a round little body on stubby legs, a big head with big eyes and big ears, three
    small puffball tails. The same part names as the adult, so one rig animates both."""
    root = m.part('cub', pivot=(0, 20.5, 0))
    body = root.part('body')
    body.cube((-2, -2, -3), (4, 4, 6.5), **_fur(bands=[(3, 'belly')], streaks=0.2))
    body.cube((-2.5, -2.5, -3.5), (5, 4.5, 2.5), **_fur('fluff', fringe=1))
    head = root.part('head', pivot=(0, -1, -3))
    head.cube((-2.5, -4, -3.75), (5, 4.5, 3.75), **_fur(clusters=0.1, streaks=0.1), faces={'north': _face(CUB_FACE, 10, 9)})
    head.cube((-3, -1.5, -3), (6, 2, 2.25), **_fur('fluff', fringe=1, streaks=0.3))
    head.cube((-1, -1, -5), (2, 1.5, 1.25), **_fur(clusters=0.0, streaks=0.0, rim=False), faces={
        'north': dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True, map=['.NN.', '.mm.'], keys={'N': 'nose', 'm': 'mouth'})})
    jaw = head.part('jaw', pivot=(0, 0.5, -3.75))
    jaw.cube((-0.75, 0, -1.25), (1.5, 0.5, 1.25), **_fur('belly', clusters=0.0, streaks=0.0, rim=False))
    for side, sx in SIDES:
        ear = head.part(f'{side}_ear', pivot=(1.5 * sx, -4, -1.5), rot=(-0.05, 0, 0.3 * sx))
        ear.cube((-0.9, -2, -0.4), (1.8, 2, 0.9), **_fur(clusters=0.0, streaks=0.0, rim=False), faces={
            'north': dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True, map=['....', '.ii.', '.ii.', '.ii.'], keys={'i': 'ear_in'})})
        tip = ear.part(f'{side}_ear_tip', pivot=(0, -2, 0.05))
        tip.cube((-0.5, -1.25, -0.3), (1, 1.25, 0.7), **_fur(clusters=0.0, streaks=0.0, rim=False))
    for name, px, pz, back in (('front_left_leg', 1.1, -1.75, False), ('front_right_leg', -1.1, -1.75, False),
                               ('back_left_leg', 1.1, 2.25, True), ('back_right_leg', -1.1, 2.25, True)):
        leg = root.part(name, pivot=(px, 1.5, pz))
        if back:
            leg.cube((-0.8, -0.5, -0.8), (1.6, 2.5, 1.6), **_fur('fluff', bands=[(2, 'sock')], streaks=0.0, rim=False))
        else:
            leg.cube((-0.7, 0, -0.7), (1.4, 2, 1.4), **_fur(bands=[(1, 'sock')], streaks=0.0, rim=False))
    for side, sx in TAILS:
        base = root.part(f'tail_{side}', pivot=(0.6 * sx, -1.25, 3.25), rot=(0.6 if sx else 0.72, 0.5 * sx, 0))
        base.cube((-0.6, -0.6, 0), (1.2, 1.2, 1.5), **_fur('shade', streaks=0.0, rim=False))
        mid = base.part(f'tail_{side}_mid', pivot=(0, 0, 1.25), rot=(0.12, 0, 0))
        mid.cube((-1.25, -1.25, 0), (2.5, 2.5, 2.5), **_fur('fluff', streaks=0.3, rim=False))
        tip = mid.part(f'tail_{side}_tip', pivot=(0, 0, 2.25), rot=(0.1, 0, 0))
        tip.cube((-0.9, -0.9, 0), (1.8, 1.8, 2), **_fur('tip', streaks=0.0, rim=False))


def swifter() -> Model:
    """One layer holding both the adult and the cub; SwifterModel shows the one that fits."""
    pal = {
        'fur': '#f1f6ff', 'fur_l': '#ffffff', 'fur_d': '#cfdff6',
        'fluff': '#f8fbff', 'fluff_l': '#ffffff', 'fluff_d': '#d9e6f9',
        'belly': '#e0ebfb', 'belly_l': '#f2f7ff', 'belly_d': '#b8cdee',
        'shade': '#c2d6f2', 'shade_l': '#d8e6f8', 'shade_d': '#9db9e3',
        'sock': '#a9c6ee', 'sock_l': '#c6daf5', 'sock_d': '#84a8dc',
        'tip': '#ffffff', 'tip_l': '#ffffff', 'tip_d': '#e3ecfa',
        'ear_in': '#97bdf0', 'nose': '#34466e', 'mouth': '#5b6e98',
        'eye': '#4fc2ff', 'pupil': '#14213f', 'lid': '#2a3a64', 'white': '#ffffff', 'tear': '#7fd6ff', 'blush': '#ffc2da',
    }
    m = Model('swifter', (128, 64), pal, {'swifter': {}}, res=2, expressions=['blink', 'angry', 'sleep', 'hurt', 'happy', 'dead'])
    _adult(m)
    _cub(m)
    return m


MODELS = {'swifter': swifter}

# ============================================================================ sounds (vanilla files)

SOUNDS = {
    'entity.swifter.ambient': [('mob/fox/idle1', 0.7, 1.25), ('mob/fox/idle2', 0.7, 1.3), ('mob/fox/idle3', 0.7, 1.2), ('mob/fox/sniff1', 0.6, 1.3)],
    'entity.swifter.hurt': [('mob/fox/hurt1', 0.9, 1.2), ('mob/fox/hurt2', 0.9, 1.25)],
    'entity.swifter.death': [('mob/fox/death1', 1.0, 1.15), ('mob/fox/death2', 1.0, 1.1)],
    'entity.swifter.snarl': [('mob/fox/aggro1', 1.0, 1.05), ('mob/fox/aggro2', 1.0, 1.1), ('mob/fox/aggro3', 1.0, 1.0)],
    'entity.swifter.dash': [('mob/phantom/swoop1', 0.9, 1.7), ('mob/phantom/swoop2', 0.9, 1.8), ('fireworks/launch1', 0.8, 1.5)],
    'entity.swifter.rocket': [('fireworks/launch1', 1.0, 1.1), ('mob/breeze/whirl', 0.8, 1.5)],
    'entity.swifter.grab': [('mob/fox/bite1', 1.0, 1.1), ('mob/fox/bite2', 1.0, 1.15), ('mob/fox/bite3', 1.0, 1.05)],
    'entity.swifter.slam': [('item/mace/smash_ground_heavy', 1.0, 1.1)],
    'entity.swifter.gust': [('entity/wind_charge/wind_burst1', 1.0, 0.9), ('entity/wind_charge/wind_burst2', 1.0, 0.95)],
    'entity.swifter.sleep': [('mob/fox/sleep1', 0.6, 1.25), ('mob/fox/sleep2', 0.6, 1.3), ('mob/fox/sleep3', 0.6, 1.2)],
    'entity.swifter.cry': [('mob/wolf/baby/whine1', 0.9, 1.35), ('mob/wolf/baby/whine2', 0.9, 1.4), ('mob/fox/screech1', 0.5, 1.9)],
    'entity.swifter.calm': [('mob/cat/purr1', 1.0, 1.3), ('mob/cat/purr2', 1.0, 1.35)],
    'entity.swifter.eat': [('mob/fox/eat1', 0.9, 1.2), ('mob/fox/eat2', 0.9, 1.25)],
    # the White Forest: leaves rustling high above, and a Swifter yipping somewhere far off
    'ambient.white_forest.additions': [('block/cherry_leaves/break1', 0.22, 1.5), ('block/cherry_leaves/break2', 0.22, 1.6),
                                       ('block/cherry_leaves/break3', 0.2, 1.4), ('mob/fox/idle4', 0.12, 1.45)],
}
SUBTITLES = {
    'entity.swifter.ambient': 'Swifter yips', 'entity.swifter.hurt': 'Swifter hurts', 'entity.swifter.death': 'Swifter dies',
    'entity.swifter.snarl': 'Swifter snarls', 'entity.swifter.dash': 'Swifter whooshes', 'entity.swifter.rocket': 'Swifter rockets up',
    'entity.swifter.grab': 'Swifter snaps', 'entity.swifter.slam': 'Swifter slams down', 'entity.swifter.gust': 'Air bursts',
    'entity.swifter.sleep': 'Swifter snores', 'entity.swifter.cry': 'Swifter cub cries', 'entity.swifter.calm': 'Swifter cub purrs',
    'entity.swifter.eat': 'Swifter eats', 'ambient.white_forest.additions': 'Leaves rustle',
}

# ============================================================================ assets


def _el(frm, to, tex, uv=None, rot=None, up=None, down=None):
    """One model element: every face uses `tex` (the up/down faces can differ); uv per face is derived
    from the element's footprint unless given."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    side_uv = {'north': [x0, 16 - y1, x1, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
               'west': [z0, 16 - y1, z1, 16 - y0], 'east': [z0, 16 - y1, z1, 16 - y0]}
    faces = {}
    for f in ('north', 'south', 'west', 'east'):
        faces[f] = {'texture': tex, 'uv': uv or [max(0, min(16, v)) for v in side_uv[f]]}
    flat = [max(0, min(16, x0)), max(0, min(16, z0)), max(0, min(16, x1)), max(0, min(16, z1))]
    faces['up'] = {'texture': up or tex, 'uv': uv or flat}
    faces['down'] = {'texture': down or tex, 'uv': uv or flat}
    e = {'from': list(frm), 'to': list(to), 'faces': faces}
    if rot:
        e['rotation'] = rot
    return e


def gen_den(GA, bid):
    """The den: a low woven bowl of twigs, lined with white fluff, tufts of fluff caught on its rim
    and a few long twigs poking out (they reach a little past the block, like a real nest)."""
    side, top, fluff, bottom = '#side', '#top', '#fluff', '#bottom'
    els = [
        _el([1, 0, 1], [15, 1, 15], side, up=fluff, down=bottom),
        _el([0, 1, 0], [16, 5, 3], side, up=top),
        _el([0, 1, 13], [16, 5, 16], side, up=top),
        _el([0, 1, 3], [3, 5, 13], side, up=top),
        _el([13, 1, 3], [16, 5, 13], side, up=top),
        _el([3, 1, 3], [13, 2.5, 13], fluff),
        _el([1, 5, 1.5], [4.5, 6.5, 4.5], fluff),
        _el([11, 5, 2], [14.5, 6.25, 5], fluff),
        _el([2, 5, 11], [5.5, 7, 14.5], fluff),
        _el([12, 5, 12], [15, 6, 15], fluff),
        _el([-3, 3.5, 7.5], [19, 4.5, 8.5], side, uv=[0, 7, 16, 8], rot={'origin': [8, 4, 8], 'axis': 'y', 'angle': 22.5}),
        _el([7.5, 2.5, -3], [8.5, 3.5, 19], side, uv=[0, 10, 16, 11], rot={'origin': [8, 3, 8], 'axis': 'y', 'angle': -22.5}),
        _el([-2, 4.25, 3], [10, 5, 4], side, uv=[2, 4, 14, 5], rot={'origin': [8, 4.5, 8], 'axis': 'y', 'angle': 45}),
    ]
    m = {'parent': 'minecraft:block/block', 'ambientocclusion': False,
         'textures': {'particle': f'{NS}:block/{bid}_side', 'side': f'{NS}:block/{bid}_side', 'top': f'{NS}:block/{bid}_top',
                      'fluff': f'{NS}:block/{bid}_fluff', 'bottom': f'{NS}:block/{bid}_bottom'},
         'elements': els}
    GA.note_textures(m)
    GA.write(os.path.join(GA.A, 'models/block', bid + '.json'), m)
    GA.simple_state(bid)
    GA.item_block(bid)


def assets(GA):
    """Sounds, recipes, tags, loot, particles and text (called before gen_sounds)."""
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    tag, rl = GA.tag, GA.rl
    # four tufts of cloud-fox fluff make two blocks of real cloud; the puffball flower gives white dye
    GA.shaped('cloud_block_from_swifter_fluff', ['##', '##'], {'#': 'swifter_fluff'}, 'cloud_block', 2, 'building')
    GA.shapeless('white_dye_from_puffbloom', ['puffbloom'], 'minecraft:white_dye', 1, 'misc', 'white_dye')
    tag('worldgen/biome', f'{NS}:is_sift', rl('white_forest'))
    tag('block', 'minecraft:overworld_carver_replaceables', rl('white_turf'))
    tag('block', f'{NS}:sift_plantable', rl('white_turf'))
    # a Bulb carried in a Swifter's jaws must not steer it (as with slimes)
    tag('entity_type', 'minecraft:non_controlling_rider', rl('bulb'))
    # drifting fluff: the forest's air, the white leaves' falling particle, a running Swifter's shedding
    texs = [f'{NS}:white_fluff_{i}' for i in range(3)]
    GA.write(os.path.join(GA.A, 'particles', 'white_fluff.json'), {'textures': texs})
    for t in texs:
        GA.TEXTURES.add('particle/' + t.split(':')[1])
    import gen_data as GD
    survives = {'type': 'minecraft:survives_explosion'}
    GD.table('block', 'blocks/swifter_den', [
        GD.pool([GD.item('swifter_fluff', count=(1, 3))], condition=survives),
        GD.pool([GD.item('minecraft:stick', count=(1, 3))], condition=survives),
        GD.pool([GD.item('glowing_slime_ball', 10, (1, 3)), GD.item('chrome_pearl', 5, (1, 2)), GD.item('minecraft:gold_nugget', 8, (3, 8)),
                 GD.item('minecraft:gold_ingot', 3, (1, 2)), GD.item('minecraft:emerald', 5, (1, 2)), GD.item('siftite_nugget', 1, (1, 2))],
                rolls=(2, 4)),
        GD.pool([GD.item('siftite_ingot')], condition=GD.chance(0.03)),
    ])
    GD.table('entity', 'entities/swifter', [GD.pool([GD.item('swifter_fluff', count=(1, 2), extra=[GD.LOOTING])])])
    GA.LANG.update({
        f'entity.{NS}.swifter': 'Swifter',
        f'biome.{NS}.white_forest': 'White Forest',
        f'codex.{NS}.swifter.title': 'Swifter',
        f'codex.{NS}.swifter.tagline': 'Neutral - a three-tailed cloud fox, fast as a jet',
        f'codex.{NS}.swifter.body': 'A fierce, fluffy fox of white cloud fur with three great tails. It runs like the wind and dashes like '
                                    'a jet, three vapour trails streaming behind it. Swifters hunt Bulbs: a crouch, a pounce, a snatch, a '
                                    'rocket ten blocks straight up - and a slam back down. Leave them be and they leave you be; hit one '
                                    'and its family dashes at you. They raise their cubs in fluffy dens full of treasure. Break a den and '
                                    'the cubs cry while their parents attack without rest. Feed every crying cub a Glowing Slime Ball to '
                                    'calm it, and the parents stand down. Drops Swifter Fluff.',
        f'codex.{NS}.white_forest.title': 'White Forest',
        f'codex.{NS}.white_forest.tagline': 'Pale trees, soft mist and drifting fluff',
        f'codex.{NS}.white_forest.body': 'A cold, dreamy forest of tall weeping White Lullwood with pearl bark, its ground dusted with '
                                         'Rainbow Snow that keeps falling, its air full of soft white mist. Puffblooms, Halo Lilies, '
                                         'Snowglobes and Shiver Thistles grow between the trunks and little clouds sit on the ground like '
                                         'bushes. White Bulbs, white Stompers and Harmoners wander here - and the Swifters, who weave '
                                         'their dens of fluff and twigs between the trees. Every den hides valuables, but its cubs are '
                                         'never far away... Four Swifter Fluff make two Clouds.',
    })


# ============================================================================ worldgen


def _patch(GW, rel, fn):
    path = os.path.join(GW.D, rel + '.json')
    with open(path) as f:
        obj = json.load(f)
    GW.w(rel, fn(obj) or obj)


def _placement(dim):
    """The White Forest takes the coldest inland climates (the plains and the grove keep the rest);
    multi-noise placement blends the edges into gradual borders."""
    pts = dim['generator']['biome_source']['biomes']
    cold = -0.4
    for p in pts:
        par = p['parameters']
        if (p['biome'] in (f'{NS}:sift_plains', f'{NS}:wishing_grove') and par['continentalness'] == [-0.19, 1.0]
                and par['erosion'] == [-0.22, 1.0] and par['temperature'][0] == -1.0):
            par['temperature'] = [cold, par['temperature'][1]]
    pts.append({'biome': f'{NS}:white_forest', 'parameters': {'temperature': [-1.0, cold], 'humidity': [-1.0, 1.0], 'continentalness': [-0.19, 1.0],
                                                              'erosion': [-0.22, 1.0], 'depth': 0.0, 'weirdness': [-1.0, 1.0], 'offset': 0.0}})
    return dim


def _surface(GW, rule):
    turf = {'type': 'minecraft:condition', 'if_true': {'type': 'minecraft:biome', 'biome_is': [f'{NS}:white_forest']},
            'then_run': {'type': 'minecraft:condition', 'if_true': 'minecraft:on_floor',
                         'then_run': {'type': 'minecraft:condition', 'if_true': 'minecraft:not_underwater',
                                      'then_run': {'type': 'minecraft:block', 'result_state': GW.state('white_turf', snowy=False)}}}}
    for r in rule['sequence']:
        if isinstance(r, dict) and r.get('type') == 'minecraft:condition' and r.get('if_true', {}).get('type') == 'minecraft:above_preliminary_surface':
            r['then_run']['sequence'].insert(0, turf)
            return rule
    raise ValueError('the_sift material rule: no above_preliminary_surface branch')


def world(GW):
    feature, placed, state, count, rarity, survive, BIOME = GW.feature, GW.placed, GW.state, GW.count, GW.rarity, GW.survive, GW.BIOME
    on_surface = [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, BIOME]
    # --- white lullwood: tall weeping pale trees (their features are written by tools/wland.py)
    # (W-land: white_lullwood_tree and grand_white_lullwood_tree are tall weeping pale trees now, defined in tools/wland.py)
    def tree(name):
        return {'feature': GW.rl(name), 'placement': [survive('white_lullwood_sapling')]}
    feature('trees_white_forest', {'type': 'minecraft:random_selector', 'default': tree('white_lullwood_tree'),
                                   'features': [{'chance': 0.22, 'feature': tree('grand_white_lullwood_tree')}]})
    placed('trees_white_forest', 'trees_white_forest', [count(6), {'type': 'minecraft:in_square'},
                                                        {'type': 'minecraft:surface_water_depth_filter', 'max_water_depth': 0},
                                                        {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR'}, BIOME])
    # --- puffblooms (and a few soulpetals) in drifts between the trunks
    feature('white_forest_flowers', {'type': 'minecraft:simple_block', 'to_place': GW.weighted([(state('puffbloom'), 6), (state('soulpetal'), 1)])})
    placed('patch_white_forest_flowers', 'white_forest_flowers', [count(3)] + on_surface + GW.surface_patch(28) + [survive('puffbloom')])
    # --- cloud-puff bushes: little clouds resting on the forest floor
    feature('cloud_bush', {'type': 'minecraft:block_blob', 'state': state('cloud_block'), 'can_place_on': {
        'type': 'minecraft:matching_blocks', 'blocks': [GW.rl('white_turf'), GW.rl('sift_grass_block'), GW.rl('sift_soil')]}})
    placed('cloud_bush', 'cloud_bush', [count(2)] + on_surface)
    # --- the Swifters' dens, each with its family (worldgen/SwifterDenFeature)
    feature('swifter_den', {'type': f'{NS}:swifter_den', 'max_adults': 2, 'max_cubs': 3})
    placed('swifter_den', 'swifter_den', [rarity(6), {'type': 'minecraft:in_square'},
                                          {'type': 'minecraft:heightmap', 'heightmap': 'MOTION_BLOCKING_NO_LEAVES'}, BIOME])
    # --- the biome: pale and dreamy, white tints, soft white fog (the fog closes in on the client, see SwifterClient)
    GW.biome('white_forest', fog='#eaf2ff', sky='#c4e2ff', water='#d6efff', grass='#eef3fb', foliage='#f6f9ff', temp=0.35, down=0.7,
             spawns=GW.mobs(creature=[('swifter', 5, 1, 2), ('bulb', 12, 2, 4), ('stomper', 3, 1, 2), ('harmoner', 4, 1, 2), ('enchoer', 1, 1, 1)],
                            ambient=[('nib', 6, 2, 3)]),
             parts=GW.particles(('white_fluff', 0.004), ('rainbow_snowflake', 0.008), ('sift_mist', 0.0015), ('star_sparkle', 0.0006)),
             feats=[(4, 'cloud_bush')] + GW.COMMON_UNDERGROUND +
                   [(9, 'trees_white_forest'), (9, 'patch_white_forest_flowers'), (9, 'minecraft:patch_grass_forest'),
                    (9, 'patch_glimmer_sprouts'), (9, 'swifter_den')] + __import__('wland').WHITE_FOREST_FEATURES)  # W-land

    def ambience(b):
        b['attributes']['minecraft:audio/ambient_sounds']['additions'] = {'sound': f'{NS}:ambient.white_forest.additions', 'tick_chance': 0.012}
        return b
    _patch(GW, 'worldgen/biome/white_forest', ambience)
    _patch(GW, 'dimension/the_sift', _placement)
    _patch(GW, 'worldgen/material_rule/the_sift', lambda r: _surface(GW, r))
