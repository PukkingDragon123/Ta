"""E1 Sniffer & rot: the Sift Sniffer (model, textures, egg block, spawn egg, sounds, loot, text) and
the rot overlays every Sift creature wears when it rots outside the Sift.

Hooked in from one line each in spec.py, mobs.py, gen_assets.py, gen_textures.py and items16.py.
"""
import math
import os
import random

from PIL import Image

from modelkit import Model

NS = 'thesift'


def declare(block, item):
    block('sift_sniffer_egg', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SNIFFER_EGG).mapColor(MapColor.COLOR_PINK).noOcclusion()',
          cls='SiftSnifferEggBlock', model='sift_sniffer_egg', tab='nature')
    item('sift_sniffer_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModSiftSniffer.SIFT_SNIFFER.get())', tab='eggs')


# ============================================================================ the model

FACE_W, FACE_H = 24, 22
FACE_KEYS = {'E': 'eye', 'W': 'shine', 'b': 'blush', 'm': 'mouth', 'l': 'lash'}
# one eye (the left one; the right is its mirror), 6 x 6 texels from row 4
EYES = {
    'neutral': ['.EEEE.', 'EWWEEE', 'EWWEEE', 'EEEEEE', 'EEEEWE', '.EEEE.'],
    'blink': ['......', '......', '......', '.EEEE.', 'E....E', '......'],
    'sleep': ['......', '......', '......', '.EEEE.', 'E....E', '......'],
    'happy': ['......', '.EEEE.', 'E....E', '......', '......', '......'],
    'angry': ['EE....', '.EEE..', 'EWEEE.', 'EWEEEE', 'EEEEEE', '.EEEE.'],
    'hurt': ['EE....', '..EE..', '....EE', '..EE..', 'EE....', '......'],
    'dead': ['E....E', '.E..E.', '..EE..', '..EE..', '.E..E.', 'E....E'],
}
MOUTHS = {'neutral': ['m..m', '.mm.'], 'happy': ['mmmm', '.mm.'], 'hurt': ['.mm.', 'm..m'], 'dead': ['....', 'mmmm'],
          'angry': ['....', 'mmmm'], 'blink': ['m..m', '.mm.'], 'sleep': ['....', '.mm.']}


def _face(expr):
    g = [['.'] * FACE_W for _ in range(FACE_H)]

    def put(x0, y0, rows, mirror=False):
        for j, row in enumerate(rows):
            for i, ch in enumerate(row[::-1] if mirror else row):
                if ch != '.':
                    g[y0 + j][x0 + i] = ch
    eye = EYES[expr]
    put(2, 4, eye)
    put(16, 4, eye, mirror=True)
    if expr in ('neutral', 'blink', 'happy', 'sleep'):
        put(1, 3, ['l'])            # a little lash at each outer corner
        put(22, 3, ['l'])
    put(0, 11, ['.bbb', 'bbb.'])     # rosy cheeks
    put(20, 11, ['bbb.', '.bbb'])
    put(10, 20, MOUTHS[expr])
    return [''.join(r) for r in g]


def _fur(color='fur', **kw):
    d = dict(color=color, pattern='mc', clusters=0.25, streaks=0.2)
    d.update(kw)
    return d


def _plant(parent, name, pivot, tex, w, h, rot=(0, 0, 0)):
    from mobs_wild import plant
    return plant(parent, name, pivot, tex, w, h, rot=rot)


# the back garden: (texture, x, z, width, height) on the mossy bed; the first MAX_GARDEN are the
# flowers it loses and regrows, the rest are grass tufts that always stay
GARDEN = [('dreambloom', -3.5, -6.5, 7, 8), ('hummingbloom', 3.5, -2.5, 7, 8), ('lullaby_bell', -3, 3, 6, 7),
          ('nebula_iris', 3.5, 6.5, 6, 7), ('puffbloom', -2.5, 9, 6, 6)]
TUFTS = [('blushgrass', 1, -9, 6, 5), ('blushgrass', -4.5, 0, 5, 4), ('glimmer_sprouts', 4.5, 2, 5, 4), ('blushgrass', 2, 9.5, 5, 4)]
LEGS = (('front_left_leg', 5.5, -9), ('middle_left_leg', 6, 0), ('back_left_leg', 5.5, 9),
        ('front_right_leg', -5.5, -9), ('middle_right_leg', -6, 0), ('back_right_leg', -5.5, 9))


def sift_sniffer() -> Model:
    """The Sift Sniffer: a Sniffer's shape - long body on six short legs, a big head and a long nose -
    made soft and round: a snow-white shaggy coat over a pink back, a chest ruff and a rump puff, a mossy
    pink bed on its back where a little garden of Sift flowers grows, floppy pink ears, huge shiny eyes,
    rosy cheeks and a nose that flares into a trumpet bell."""
    pal = {
        'fur': '#fdf6f9', 'fur_l': '#ffffff', 'fur_d': '#f0dfe7',
        'fluff': '#fffafc', 'fluff_l': '#ffffff', 'fluff_d': '#f3e3eb',
        'pink': '#f3a6c3', 'pink_l': '#ffc8dc', 'pink_d': '#d77d9f',
        'rose': '#ec8fb2', 'rose_l': '#f8b4cd', 'rose_d': '#c96a8f',
        'ear_in': '#ffbfd4', 'ear_in_l': '#ffd9e6', 'ear_in_d': '#e79ab7',
        'moss': '#e48fb0', 'moss_l': '#f5afc9', 'moss_d': '#bf6f91',
        'hoof': '#c58aa5', 'hoof_l': '#d9a3bb', 'hoof_d': '#9d6681',
        'eye': '#3a2340', 'shine': '#ffffff', 'blush': '#ff9cc0', 'mouth': '#93506f', 'lash': '#3a2340',
        'nostril': '#7a3456', 'bell': '#ffd0e1', 'plant': '#7fcf9a',
    }
    m = Model('sift_sniffer', (256, 192), pal, {'sift_sniffer': {}}, res=2,
              expressions=['blink', 'happy', 'angry', 'hurt', 'dead'])
    fluff = dict(streaks=0.12, clusters=0.3)
    body = m.part('body', pivot=(0, 12, 0))
    body.cube((-9, -8, -14), (18, 13, 28), **_fur('pink', bands=[(3, 'fur')], clusters=0.2, streaks=0.1))
    body.cube((-10, -6.5, -13), (20, 6, 26), **_fur('fluff', **fluff), faces={'up': dict(skip=True)})            # the round flanks
    body.cube((-10.5, -1, -12.5), (21, 6, 25), **_fur('fluff', fringe=2, **fluff), faces={'up': dict(skip=True)})  # the shaggy hem
    body.cube((-8, -9.5, -13), (16, 1.5, 26), **_fur('pink', clusters=0.3, streaks=0.0), faces={'down': dict(skip=True)})  # the pink back
    body.cube((-9.5, -7.5, -15.5), (19, 12, 3), **_fur('fluff', fringe=2, **fluff))                               # chest ruff
    body.cube((-9, -7, 12), (18, 11, 3.5), **_fur('fluff', fringe=2, **fluff))                                    # rump puff
    for i, (x, y, z, d) in enumerate(((9.75, -4, -7, 6), (9.75, -3.5, 5, 7), (-10.75, -4.5, -2, 7), (-10.75, -3, 8, 5))):
        body.cube((x, y, z), (1, 4.5, d), **_fur('fluff', fringe=1, rim=False, fringe_phase=i, **fluff))            # tufts on its sides
    bed = dict(color='moss', image='img:coral_turf_top', image_mode='tile')
    body.cube((-6.5, -11, -11), (13, 1.5, 22), **_fur('moss', clusters=0.4, streaks=0.0, rim=False),
              faces={'up': bed, 'down': dict(skip=True)})                                                           # the garden bed
    garden = body.part('garden', pivot=(0, -11, 0))
    for i, (tex, x, z, w, h) in enumerate(GARDEN):
        _plant(garden, f'flower_{i}', (x, 0, z), tex, w, h, rot=(0, 0.7 * i + 0.2, 0))
    for i, (tex, x, z, w, h) in enumerate(TUFTS):
        _plant(garden, f'tuft_{i}', (x, 0, z), tex, w, h, rot=(0, 1.1 * i, 0))
    tail = body.part('tail', pivot=(0, -4, 15.5))
    tail.cube((-2.5, -2.5, 0), (5, 5, 3.5), **_fur('fluff', rim=False, **fluff), faces={'south': _fur('pink', rim=False, streaks=0.0)})

    head = body.part('head', pivot=(0, -1, -15.5))
    face = dict(color='pink', pattern='mc', clusters=0.0, rim=False, hd=True, bands=[(3, 'fur')], map=_face('neutral'), keys=FACE_KEYS,
                expr={k: _face(k) for k in EYES if k != 'neutral'})
    head.cube((-6, -6.5, -9), (12, 11, 9), **_fur('pink', bands=[(3, 'fur')], clusters=0.15, streaks=0.1), faces={'north': face})
    head.cube((-7, -0.5, -8), (14, 5, 6), **_fur('fluff', fringe=1, **fluff), faces={'north': dict(skip=True)})  # cheek fluff
    head.cube((-3.5, -8.5, -7.5), (7, 2.5, 5), **_fur('pink', fringe=1, rim=False, streaks=0.0))                 # forehead tuft
    _plant(head, 'head_flower', (3, -8.5, -5), 'dreambloom', 5, 6, rot=(0, 0.5, 0))
    for side, sx in (('left', 1), ('right', -1)):
        ear = head.part(f'{side}_ear', pivot=(6 * sx, -5, -4.5), rot=(0, 0, -0.45 * sx))
        inner = 'west' if sx > 0 else 'east'
        ear.cube((0 if sx > 0 else -1.5, -0.5, -3), (1.5, 9, 6), **_fur('pink', streaks=0.0, fringe=1),
                 faces={inner: _fur('ear_in', clusters=0.1, streaks=0.0, fringe=1, rim=False)})
    nose = head.part('nose', pivot=(0, 1, -9))
    nose.cube((-2.5, -2, -3.5), (5, 4, 3.5), **_fur('rose', clusters=0.2, streaks=0.0))
    tip = nose.part('nose_tip', pivot=(0, 0, -3.5))
    bell = ['.rrrrrrrrrr.', 'r..........r', 'r.NNN..NNN.r', 'r.NNN..NNN.r', 'r..N....N..r', 'r..........r', 'r..........r', 'r..........r',
            'r..........r', '.rrrrrrrrrr.']
    tip.cube((-3, -2.5, -1.5), (6, 5, 1.5), **_fur('rose', clusters=0.0, streaks=0.0, rim=False), faces={
        'north': dict(color='bell', pattern='mc', clusters=0.0, rim=False, hd=True, map=bell, keys={'r': 'rose_l', 'N': 'nostril'})})
    for name, x, z in LEGS:
        leg = m.part(name, pivot=(x, 17, z))
        leg.cube((-2.5, 0, -2.5), (5, 7, 5), **_fur('fur', bands=[(5, 'hoof')], streaks=0.08))
        leg.cube((-3, -0.5, -3), (6, 3, 6), **_fur('fluff', fringe=1, **fluff), faces={'up': dict(skip=True)})
    return m


def _img_palette(m):
    """The bed's tile picture is looked up as a palette key."""
    from mobs_wild import block_tex
    turf = block_tex('coral_turf_top')
    pink = Image.new('RGBA', turf.size, (243, 166, 195, 255))
    m.palette['img:coral_turf_top'] = Image.blend(turf, pink, 0.45)  # the Coral Turf, softened to the coat's pink
    return m


MODELS = {'sift_sniffer': lambda: _img_palette(sift_sniffer())}


# ============================================================================ sounds (vanilla files)

def _files(prefix, n, vol, pitch):
    return [(f'{prefix}{i}', vol, pitch) for i in range(1, n + 1)]


SOUNDS = {
    'entity.sift_sniffer.ambient': _files('mob/sniffer/idle', 6, 0.9, 1.2) + [('mob/sniffer/happy2', 0.7, 1.3)],
    'entity.sift_sniffer.hurt': _files('mob/sniffer/hurt', 3, 1.0, 1.2),
    'entity.sift_sniffer.death': _files('mob/sniffer/death', 2, 1.0, 1.15),
    'entity.sift_sniffer.step': _files('mob/sniffer/step', 6, 0.6, 1.1),
    'entity.sift_sniffer.eat': _files('mob/sniffer/eat', 3, 1.0, 1.15),
    'entity.sift_sniffer.sniff': _files('mob/sniffer/sniffing', 3, 1.0, 1.1) + _files('mob/sniffer/scenting', 3, 1.0, 1.1),
    'entity.sift_sniffer.dig': _files('mob/sniffer/longdig', 2, 1.0, 1.05),
    'entity.sift_sniffer.find': [('random/pop', 0.8, 0.8), ('mob/sniffer/happy3', 1.0, 1.25)],
    'entity.sift_sniffer.trumpet': [('item/goat_horn/call1', 0.45, 1.55), ('item/goat_horn/call4', 0.45, 1.6), ('item/goat_horn/call7', 0.45, 1.5)],
    'entity.sift_sniffer.happy': _files('mob/sniffer/happy', 5, 1.0, 1.2),
    'entity.sift_sniffer.prepare_ram': _files('mob/goat/pre_ram', 4, 1.0, 0.8),
    'entity.sift_sniffer.ram': [('mob/goat/impact1', 1.0, 0.75), ('mob/goat/impact2', 1.0, 0.75)],
    'entity.sift_sniffer.bloom': [('block/cherry_leaves/break1', 1.0, 1.2), ('block/cherry_leaves/break2', 1.0, 1.1), ('mob/sniffer/happy1', 0.8, 1.2)],
    'block.sift_sniffer_egg.plop': [('mob/chicken/plop', 1.0, 0.7)],
    'block.sift_sniffer_egg.crack': _files('mob/turtle/egg/egg_crack', 5, 1.0, 0.9),
    'block.sift_sniffer_egg.hatch': _files('mob/turtle/egg/egg_break', 2, 1.0, 0.9),
    'entity.sift_creature.rot': [('mob/zombie/say1', 0.6, 0.7), ('mob/zombie/say2', 0.6, 0.7), ('mob/zombie/say3', 0.6, 0.75)],
}
SUBTITLES = {
    'entity.sift_sniffer.ambient': 'Sift Sniffer snuffles', 'entity.sift_sniffer.hurt': 'Sift Sniffer hurts',
    'entity.sift_sniffer.death': 'Sift Sniffer dies', 'entity.sift_sniffer.step': 'Sift Sniffer steps',
    'entity.sift_sniffer.eat': 'Sift Sniffer grazes', 'entity.sift_sniffer.sniff': 'Sift Sniffer sniffs',
    'entity.sift_sniffer.dig': 'Sift Sniffer digs', 'entity.sift_sniffer.find': 'Sift Sniffer finds something',
    'entity.sift_sniffer.trumpet': 'Sift Sniffer trumpets', 'entity.sift_sniffer.happy': 'Sift Sniffer delights',
    'entity.sift_sniffer.prepare_ram': 'Sift Sniffer paws the ground', 'entity.sift_sniffer.ram': 'Sift Sniffer rams',
    'entity.sift_sniffer.bloom': 'Sniffer blooms', 'block.sift_sniffer_egg.plop': 'Sift Sniffer lays an egg',
    'block.sift_sniffer_egg.crack': 'Sift Sniffer Egg cracks', 'block.sift_sniffer_egg.hatch': 'Sift Sniffer Egg hatches',
    'entity.sift_creature.rot': 'Rotting creature groans',
}


def assets(GA):
    """Sounds, loot, spawns' text and the Codex page (called before gen_sounds)."""
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    import gen_data as GD
    # what it digs up: the Sift's rare seeds and Pitcher Pods, now and then a Sculk Bloom
    GD.table('gift', 'gameplay/sift_sniffer_digging', [GD.pool([
        GD.item('minecraft:torchflower_seeds', 4), GD.item('minecraft:pitcher_pod', 4), GD.item('echo_seed', 4), GD.item('sculk_bloom', 1)])])
    # a flower or two from its back garden
    GD.table('entity', 'entities/sift_sniffer', [GD.pool([GD.item('dreambloom', 2), GD.item('hummingbloom', 2), GD.item('lullaby_bell', 1)],
                                                         condition=GD.chance(0.6))])
    GA.LANG.update({
        f'entity.{NS}.sift_sniffer': 'Sift Sniffer',
        f'band.{NS}.instrument.nose_trumpet': 'Nose Trumpet',
        # the original Minecraft-looking Sniffer is what a Sift Sniffer rots into outside the Sift
        'entity.minecraft.sniffer': 'Zombified Sniffer',
        'block.minecraft.sniffer_egg': 'Zombified Sniffer Egg',
        'item.minecraft.sniffer_spawn_egg': 'Zombified Sniffer Spawn Egg',
        f'codex.{NS}.sniffer.title': 'Sift Sniffer',
        f'codex.{NS}.sniffer.tagline': 'Neutral - fluffy, flowery and fond of digging',
        f'codex.{NS}.sniffer.body': 'A great fluffy Sniffer, pink and white, with a little flower garden on its back. It grazes the '
                                    'pink grass (a good meal regrows lost flowers) and now and then sniffs out and digs up something '
                                    'rare: Torchflower seeds, Pitcher Pods, Echo Seeds, even a Sculk Bloom. It flees Swifters, but '
                                    'hit one and it rams you like a goat. It trumpets through its nose and plays in bands. Fed '
                                    'Torchflower seeds, two lay a fluffy egg. Away from the Sift it rots into a grey Zombified '
                                    'Sniffer - every Sift creature rots out there, red, dark green and lifeless, and heals at home.',
    })


# ============================================================================ the egg block


def gen_egg(GA, bid):
    """A big pink egg nested in white fluff: a fluffy skirt round its foot, a soft fluffy ring round its
    middle, a tuft on top and a little sprout poking out of it. Three hatch stages, more and more cracked."""
    def el(frm, to, tex, rot=None, faces=('north', 'south', 'west', 'east', 'up', 'down')):
        x0, y0, z0 = frm
        x1, y1, z1 = to
        uv = {'north': [x0, 16 - y1, x1, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
              'east': [z0, 16 - y1, z1, 16 - y0], 'up': [x0, z0, x1, z1], 'down': [x0, z0, x1, z1]}
        e = {'from': list(frm), 'to': list(to),
             'faces': {f: {'texture': tex, 'uv': [max(0, min(16, v)) for v in uv[f]]} for f in faces}}
        if rot:
            e['rotation'] = rot
        return e

    els = [
        el([3, 0, 3], [13, 14, 13], '#shell'),
        el([2, 0, 2], [14, 4, 14], '#fluff'),                     # the fluffy skirt
        el([2.5, 7, 2.5], [13.5, 9, 13.5], '#fluff'),             # a soft ring round its middle
        el([5, 14, 5], [11, 15, 11], '#fluff'),                   # the tuft on top
        el([5, 15, 8], [11, 21, 8], '#sprout', rot={'origin': [8, 15, 8], 'axis': 'y', 'angle': 45}, faces=('north', 'south')),
        el([8, 15, 5], [8, 21, 11], '#sprout', rot={'origin': [8, 15, 8], 'axis': 'y', 'angle': 45}, faces=('west', 'east')),
    ]
    variants = {}
    for stage, name in enumerate((bid, bid + '_slightly_cracked', bid + '_very_cracked')):
        m = {'parent': 'minecraft:block/block', 'ambientocclusion': False,
             'textures': {'particle': f'{NS}:block/{bid}_shell_{stage}', 'shell': f'{NS}:block/{bid}_shell_{stage}',
                          'fluff': f'{NS}:block/{bid}_fluff', 'sprout': f'{NS}:block/{bid}_sprout'},
             'elements': els}
        GA.note_textures(m)
        GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
        variants[f'hatch={stage}'] = {'model': f'{NS}:block/{name}'}
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})
    GA.item_block(bid)


def _px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def _hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def _shell(stage):
    rnd = random.Random(31)
    tones = [_hx('#e98fb3'), _hx('#f2a9c6'), _hx('#f8c3d7'), _hx('#fde0eb')]
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            # lit from the top left, a soft ramp to the shaded bottom right
            t = (x + y) / 30.0 + (rnd.random() - 0.5) * 0.25
            img.putpixel((x, y), tones[3 - max(0, min(3, int(t * 4)))])
    # speckles: white flecks and darker rose spots in little clusters, like a Sniffer egg's
    for _ in range(9):
        x, y = rnd.randrange(16), rnd.randrange(16)
        _px(img, x, y, _hx('#ffffff'))
        if rnd.random() < 0.5:
            _px(img, x + 1, y, _hx('#fff4f8'))
    for _ in range(7):
        x, y = rnd.randrange(16), rnd.randrange(16)
        _px(img, x, y, _hx('#c96a8f'))
        _px(img, x, y + 1, _hx('#d77d9f'))
    crack = _hx('#7a3456')
    crack_l = _hx('#b5547c')
    cracks = [[(3, 4), (4, 5), (4, 6), (5, 7), (6, 7), (7, 8)], [(11, 2), (11, 3), (10, 4), (10, 5), (11, 6), (12, 7), (12, 8)],
              [(6, 10), (7, 11), (8, 11), (9, 12), (9, 13)], [(13, 10), (12, 11), (13, 12)]]
    for line in cracks[:{0: 0, 1: 2, 2: 4}[stage]]:
        for x, y in line:
            _px(img, x, y, crack)
            _px(img, x + 1, y, crack_l)
    return img


def _fluff_tex():
    rnd = random.Random(41)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            # soft curls: rounded clumps of white with lilac-pink shade between them
            v = math.sin(x * 1.3 + math.sin(y * 0.9) * 1.6) + math.cos(y * 1.1 + x * 0.4)
            v += (rnd.random() - 0.5) * 0.7
            c = '#ffffff' if v > 0.9 else '#fbf2f6' if v > 0.0 else '#efdde6' if v > -0.9 else '#dcc3d0'
            img.putpixel((x, y), _hx(c))
    return img


def _sprout_tex():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rows = ['................', '.......pp.......', '......pPPp......', '.....pPYYPp.....', '......pPPp......', '.......pp.......',
            '.......s........', '..ll...s...ll...', '.lLll..s..llLl..', '..lLl..s..lLl...', '....ll.s.ll.....', '......lsl.......',
            '.......s........', '.......s........', '.......s........', '.......s........']
    pal = {'p': '#e57fa6', 'P': '#ffb5d0', 'Y': '#fff2a8', 's': '#4f9a6a', 'l': '#5fb07a', 'L': '#8fd9a3'}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), _hx(pal[ch]))
    return img


def textures(out):
    for stage in range(3):
        out(f'block/sift_sniffer_egg_shell_{stage}', _shell(stage))
    out('block/sift_sniffer_egg_fluff', _fluff_tex())
    out('block/sift_sniffer_egg_sprout', _sprout_tex())


def item_sprites():
    import items16 as I
    # the Sift Sniffer: a pink-and-white egg with its big eyes, rosy cheeks, trumpet nose and flowers on top
    egg = I.egg(['#e48aae', '#f5b3cc', '#fde3ee', '#ffffff'], '#a24d71', {
        0: '.....pp...ww....',
        1: '....pYYp.wYYw...',
        2: '.....ppg..wwg...',
        6: '.....E....E.....',
        7: '.....E....E.....',
        8: '....b......b....',
        9: '......nnnn......',
        10: '......nNNn......',
    }, {'p': '#e86f9d', 'Y': '#ffe36b', 'w': '#ffffff', 'g': '#5fb07a', 'E': '#3a2340', 'b': '#ff8fb8', 'n': '#ec8fb2', 'N': '#7a3456'})
    return {'sift_sniffer_spawn_egg': egg}


# ============================================================================ the rot overlays

ROT_RED = [(0x6e, 0x1c, 0x18), (0x8e, 0x2a, 0x22), (0xa8, 0x40, 0x2e)]
ROT_GREEN = [(0x22, 0x36, 0x1a), (0x2f, 0x4a, 0x22), (0x41, 0x60, 0x2c)]
_EXPR = ('_blink', '_happy', '_angry', '_hurt', '_sleep', '_dead')


def _noise(w, h, cell, seed):
    """Smooth value noise in 0..1 (bilinear over a random lattice)."""
    rnd = random.Random(seed)
    gw, gh = w // cell + 2, h // cell + 2
    lat = [[rnd.random() for _ in range(gw)] for _ in range(gh)]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        fy = y / cell
        y0 = int(fy)
        ty = fy - y0
        ty = ty * ty * (3 - 2 * ty)
        for x in range(w):
            fx = x / cell
            x0 = int(fx)
            tx = fx - x0
            tx = tx * tx * (3 - 2 * tx)
            a = lat[y0][x0] * (1 - tx) + lat[y0][x0 + 1] * tx
            b = lat[y0 + 1][x0] * (1 - tx) + lat[y0 + 1][x0 + 1] * tx
            out[y][x] = a * (1 - ty) + b * ty
    return out


def rot_overlay(base, seed):
    """The rot a creature wears outside the Sift, for one of its textures: blotches of raw red and dark
    green over a sour olive wash, only where the creature itself is painted (so plants on planes and
    cut-out fins rot too, but never the empty air around them). The renderer fades it in with the rot."""
    w, h = base.size
    cell = max(3, w // 22)
    n1 = _noise(w, h, cell, seed)
    n2 = _noise(w, h, max(2, cell // 2), seed + 1)
    n3 = _noise(w, h, cell * 2, seed + 2)
    src = base.load()
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(seed + 3)
    for y in range(h):
        for x in range(w):
            if src[x, y][3] < 128:
                continue
            v = n1[y][x] * 0.7 + n2[y][x] * 0.3
            if v > 0.56:
                ramp = ROT_RED if n3[y][x] > 0.5 else ROT_GREEN
                k = min(2, int((v - 0.56) / 0.07))
                k = max(0, k - (1 if rnd.random() < 0.15 else 0))
                r, g, b = ramp[2 - k]
                px[x, y] = (r, g, b, 235)
            elif v > 0.5:
                r, g, b = ROT_GREEN[0] if n3[y][x] <= 0.5 else ROT_RED[0]
                px[x, y] = (r, g, b, 150)        # a dark rim round each blotch
            else:
                px[x, y] = (0x4a, 0x55, 0x2e, 70)  # the sour wash over the rest
    return img


def rot_textures(tex_root):
    """One rot overlay next to every creature texture (not the expressions: the renderer maps those to
    their base texture, the silhouette is the same; not the emissive layers)."""
    ent = os.path.join(tex_root, 'entity')
    n = 0
    for dirpath, _, files in os.walk(ent):
        for f in sorted(files):
            if not f.endswith('.png') or f.endswith('_glow.png') or f.endswith('_rot.png'):
                continue
            stem = f[:-4]
            if stem.endswith(_EXPR):
                continue
            path = os.path.join(dirpath, f)
            base = Image.open(path).convert('RGBA')
            if base.width > 1024 or base.height > 1024:
                continue
            seed = sum(ord(c) for c in stem) * 131
            rot_overlay(base, seed).save(os.path.join(dirpath, stem + '_rot.png'))
            n += 1
    return n
