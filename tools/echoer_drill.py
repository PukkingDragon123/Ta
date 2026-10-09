"""RR: the Echoer Drill (`thesift:echoer_device`, the old Echoer horn remade as a drill cannon).

A soulstone breech on a riveted back plate, caged by four iron rails and a brass collar (the static block model,
rotated by the blockstate like the old horn), and the moving gun drawn by EchoerDrillRenderer from the modelkit
model below: a chamfered iron barrel with a glowing rune channel, two copper coils wound round it (cyan light
between their windings), a brass muzzle ring, a stepped and twisted Siftite drill bit with a pink tip that spins,
the Echoer's listening horn on top that twitches at every beat it hears, and a brass music drum on the side that
turns a notch per beat. The whole gun recoils on every bite. The behaviour is Java (block/EchoerDeviceBlock and
block/entity/EchoerDeviceBlockEntity): the rhythm it hears picks the pattern, the direction and the depth.

Hooks (one line each): mobs.py -> ALL.update(MODELS); echoer_world.gen_block (kind 'echoer_device') -> gen_block;
echoer_world.block_textures -> block_textures(out); echoer.py SOUNDS/SUBTITLES (beat, bite); echoer_world.lang ->
LANG; vanilla_remap.SKIP |= TEXTURES (these are drawn here, not re-themed). Java: client/EchoerDrillClient,
client/renderer/EchoerDrillRenderer, client/model/EchoerDrillModel.

Model units: 1/16 block, Y down, centred on the block (the renderer translates to the centre, turns to the facing
and flips like a mob model), the front (where it drills) towards -Z.
"""
from __future__ import annotations

import math
import os
import random

from PIL import Image

from modelkit import Model

NS = 'thesift'

# ============================================================================ the moving gun (modelkit)

PAL = {
    'iron': '#3d4452', 'iron_l': '#66718a', 'iron_d': '#1d2129',
    'copper': '#c4693b', 'copper_l': '#f0a070', 'copper_d': '#7f3c1d',
    'verdigris': '#5cb89c',
    'brass': '#c9a046', 'brass_l': '#f3d98e', 'brass_d': '#7e5c20',
    'bit': '#9fc1d6', 'bit_l': '#eef9ff', 'bit_d': '#4f7591',
    'pink': '#f29bd6', 'pink_l': '#ffe0f4', 'pink_d': '#b5579a',
    'glow': '#5fe9ff', 'glow_l': '#d4fdff', 'glow_d': '#1f8fae',
    'soulstone': '#5a4a3d', 'soulstone_l': '#806a55', 'soulstone_d': '#30271f',
    'void': '#0a0f13',
}
MATS = {'bit': 'metal', 'pink': 'crystal', 'iron': 'metal', 'copper': 'metal', 'brass': 'metal', 'glow': 'flat', 'void': 'flat',
        'soulstone': 'stone'}

BARREL_FRONT = -7.0  # the barrel runs from here back into the breech (z 2)
DRILL_AT = -8.0      # the bit's hub, in front of the muzzle ring
COILS = (-3.4, -0.6)  # where each coil starts (each is 2.4 deep)


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def _ring(parent, name, r, t, z, w, paint, n=8, overlap=0.35):
    """A ring of n tangent segments round the Z axis: inner radius r, thickness t, from z to z + w."""
    seg_len = 2.0 * r * math.tan(math.pi / n) + overlap
    for i in range(n):
        seg = parent.part(f'{name}_{i}', rot=(0, 0, i * math.tau / n))
        seg.cube((-seg_len / 2.0, -r - t, z), (seg_len, t, w), **paint)


def echoer_drill() -> Model:
    m = Model('echoer_drill', (128, 64), PAL, {'echoer_drill': {}}, res=2, materials=MATS)
    gun = m.part('gun')

    # ---- the barrel: two crossed boxes make a chamfered tube; a rune channel glows along its top
    rune = ['.dd.', '.gd.', '.dg.', '.gg.', '.dg.', '.gd.', '.dd.', '.gg.', '.gd.', '.dg.', '.dd.', '.gg.', '.dg.', '.gd.', '.dd.',
            '.gg.', '.gd.', '.dg.']
    rivets = ['r..........r', '............', '............', '.r........r.', '............', '............'] * 3
    length = 2.0 - BARREL_FRONT
    gun.cube((-3, -2, BARREL_FRONT), (6, 4, length), **mc('iron', clusters=0.5, accent='iron_l', faces={
        f: mc('iron', clusters=0.4, hd=True, map=rivets, keys={'r': 'brass_l'}) for f in ('east', 'west')}))
    gun.cube((-2, -3, BARREL_FRONT), (4, 6, length), **mc('iron', clusters=0.5, faces={
        'up': mc('iron', clusters=0.3, hd=True, map=[r * 2 for r in rune], keys={'d': 'void', 'g': 'glow'}, glow_keys='g'),
        'down': mc('iron_d', clusters=0.3)}))
    # the bore: a dark mouth with a glowing throat behind the bit
    gun.cube((-2.5, -2.5, BARREL_FRONT - 0.3), (5, 5, 0.3), **mc('void', rim=False, faces={
        'north': dict(color='void', pattern='flat', hd=True, map=['..gggggg..', '.g......g.', 'g...gg...g', 'g..g..g..g', 'g..g..g..g',
                                                                    'g...gg...g', '.g......g.', '..gggggg..'],
                      keys={'g': 'glow_d'}, glow_keys='g')}))
    # the breech flange where it enters the housing
    _ring(gun, 'flange', 3.0, 1.6, 0.8, 1.2, mc('copper', clusters=0.4, accent='verdigris', spots=0.15), n=8)

    # ---- two coils: copper | light | copper, wound round the barrel
    wind = {'up': mc('copper', clusters=0.0, rim=False, hd=True, map=['LLLLLLLL', 'dddddddd'], keys={'L': 'copper_l', 'd': 'copper_d'})}
    for k, z in enumerate(COILS):
        coil = gun.part(f'coil_{k}')
        _ring(coil, f'coil_{k}_a', 3.1, 1.7, z, 0.9, mc('copper', clusters=0.2, faces=wind))
        _ring(coil, f'coil_{k}_glow', 3.0, 1.5, z + 0.9, 0.6, dict(color='glow', pattern='flat', glow=True))
        _ring(coil, f'coil_{k}_b', 3.1, 1.7, z + 1.5, 0.9, mc('copper', clusters=0.2, faces=wind))

    # ---- the brass muzzle ring
    _ring(gun, 'muzzle', 3.0, 1.4, BARREL_FRONT - 1.0, 1.0, mc('brass', clusters=0.3, faces={
        'up': mc('brass', clusters=0.0, rim=False, hd=True, map=['l.l.l.l.', 'dddddddd'], keys={'l': 'brass_l', 'd': 'brass_d'})}))

    # ---- the drill bit: a hub, three twisted star tiers of Siftite steel, a pink Siftite tip
    drill = gun.part('drill', pivot=(0, 0, DRILL_AT))
    drill.cube((-2.5, -1.8, -1.2), (5, 3.6, 1.2), **mc('brass', clusters=0.3))
    drill.cube((-1.8, -2.5, -1.2), (3.6, 5, 1.2), **mc('brass', clusters=0.3))
    z = -1.2
    flute = {f: mc('bit', clusters=0.2, hd=True, map=['L.......', '.L......', '..L.....', '...L....'], keys={'L': 'bit_l'})
             for f in ('up', 'down', 'east', 'west')}
    for k, (s, ln) in enumerate(((4.6, 1.8), (3.4, 1.7), (2.2, 1.6))):
        tier = drill.part(f'tier_{k}', rot=(0, 0, k * math.pi / 8))
        tier.cube((-s / 2, -s / 2, z - ln), (s, s, ln), **mc('bit', clusters=0.35, accent='bit_l', faces=flute))
        twin = tier.part(f'tier_{k}_twin', rot=(0, 0, math.pi / 4))
        twin.cube((-s / 2 + 0.25, -s / 2 + 0.25, z - ln + 0.15), (s - 0.5, s - 0.5, ln - 0.3), **mc('bit_d', clusters=0.3, accent='bit'))
        z -= ln
    tip = drill.part('tip', rot=(0, 0, math.pi / 4))
    tip.cube((-0.7, -0.7, z - 1.3), (1.4, 1.4, 1.3), **dict(color='pink', pattern='crystal', glow=True))

    # ---- the Echoer's listening horn on top of the breech (it twitches at every beat)
    ear = m.part('ear', pivot=(0, -6, 4))
    ear.cube((-1, -2.4, -1), (2, 2.4, 2), **mc('brass', clusters=0.3))
    horn = ear.part('horn', pivot=(0, -2.4, 0), rot=(-0.45, 0, 0))
    horn.cube((-1.5, -1.6, -1.5), (3, 1.6, 3), **mc('copper', clusters=0.4, accent='verdigris', spots=0.2))
    horn.cube((-2.3, -2.7, -2.3), (4.6, 1.1, 4.6), **mc('copper_l', clusters=0.3, accent='copper'))
    horn.cube((-3, -3.3, -3), (6, 0.6, 6), **mc('brass', clusters=0.2, faces={
        'up': dict(color='brass', pattern='flat', hd=True, map=['bbbbbbbbbbbb', 'bBBBBBBBBBBb', 'bBvvvvvvvvBb', 'bBvggggggvBb', 'bBvgvvvvgvBb',
                                                                 'bBvgvccvgvBb', 'bBvgvccvgvBb', 'bBvgvvvvgvBb', 'bBvggggggvBb', 'bBvvvvvvvvBb',
                                                                 'bBBBBBBBBBBb', 'bbbbbbbbbbbb'],
                   keys={'b': 'brass_d', 'B': 'brass_l', 'v': 'void', 'g': 'glow', 'c': 'glow_l'}, glow_keys='gc')}))

    # ---- the music drum on its side: a brass cylinder studded with pins (it turns a notch per beat)
    dial = m.part('dial', pivot=(6.9, 0, 4))
    pins = ['..P...P...', 'P....P...P', '...P....P.', '.P...P....', '....P...P.', 'P.P.....P.', '......P...', '.P..P....P']
    drum = dict(color='brass', pattern='mc', clusters=0.2, faces={f: mc('brass', clusters=0.0, rim=False, hd=True, map=pins,
                                                                           keys={'P': 'brass_l'}) for f in ('up', 'down', 'north', 'south')})
    dial.cube((-0.8, -2.5, -1.8), (1.6, 5, 3.6), **drum)
    dial.cube((-0.8, -1.8, -2.5), (1.6, 3.6, 5), **drum)
    dial.cube((0.8, -0.7, -0.7), (0.6, 1.4, 1.4), **mc('copper_d', clusters=0.0, rim=False))
    return m


MODELS = {'echoer_drill': echoer_drill}

# ============================================================================ the static housing (block model)

TEXTURES = ('echoer_device_plate', 'echoer_device_casing', 'echoer_device_breech', 'echoer_device_rail', 'echoer_device_collar',
            'echoer_device_barrel', 'echoer_device_coil', 'echoer_device_bit')
ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def _uv(a, b, d, local):
    """Explicit face UVs: the vanilla default (where the face sits in the block) for parts inside the block, or the
    face's own size from the texture's corner (local) for parts that reach out of it."""
    (x0, y0, z0), (x1, y1, z1) = a, b
    if local:
        w, h = {'north': (x1 - x0, y1 - y0), 'south': (x1 - x0, y1 - y0), 'east': (z1 - z0, y1 - y0), 'west': (z1 - z0, y1 - y0),
                'up': (x1 - x0, z1 - z0), 'down': (x1 - x0, z1 - z0)}[d]
        return [0, 0, round(min(16.0, w), 3), round(min(16.0, h), 3)]
    uv = {'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0],
          'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0], 'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0]}[d]
    return [round(min(16.0, max(0.0, v)), 3) for v in uv]


def _box(a, b, tex, cull=None, local=False, **kw):
    faces = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        t = tex.get(d, tex['all']) if isinstance(tex, dict) else tex
        f = {'uv': _uv(a, b, d, local), 'texture': t}
        if cull and d in cull:
            f['cullface'] = d
        faces[d] = f
    e = {'from': a, 'to': b, 'faces': faces}
    e.update(kw)
    return e


def housing():
    """North-facing: the back plate (z 14-16), the breech (z 10-14), four corner rails and the brass collar (z 2-4)."""
    el = [
        _box([0, 0, 14], [16, 16, 16], {'all': '#rail', 'north': '#plate', 'south': '#plate'}, cull=('south', 'east', 'west', 'up', 'down')),
        _box([2, 2, 10], [14, 14, 14], {'all': '#casing', 'north': '#breech', 'south': '#casing'}),
    ]
    for x0, y0 in ((0, 0), (14, 0), (0, 14), (14, 14)):
        cull = tuple(d for d, on in (('west', x0 == 0), ('east', x0 == 14), ('down', y0 == 0), ('up', y0 == 14)) if on)
        el.append(_box([x0, y0, 2], [x0 + 2, y0 + 2, 14], {'all': '#rail', 'north': '#collar'}, cull=cull))
    el += [
        _box([2, 14, 2], [14, 16, 4], {'all': '#rail', 'north': '#collar', 'south': '#collar'}, cull=('up',)),
        _box([2, 0, 2], [14, 2, 4], {'all': '#rail', 'north': '#collar', 'south': '#collar'}, cull=('down',)),
        _box([0, 2, 2], [2, 14, 4], {'all': '#rail', 'north': '#collar', 'south': '#collar'}, cull=('west',)),
        _box([14, 2, 2], [16, 14, 4], {'all': '#rail', 'north': '#collar', 'south': '#collar'}, cull=('east',)),
    ]
    return el


def still_gun():
    """The gun, standing still, for the item (in the world the block entity draws it, moving)."""
    el = [
        _box([5, 5, 1], [11, 11, 10], '#barrel', local=True),
        _box([4, 4, 0], [12, 12, 1], '#collar', local=True),
    ]
    for z in (4.6, 7.4):
        el.append(_box([3.6, 3.6, z], [12.4, 12.4, z + 2.4], {'all': '#coil', 'north': '#collar', 'south': '#collar'}, local=True))
    z = 0.0
    for k, (s, ln) in enumerate(((5.0, 1.2), (4.6, 1.8), (3.4, 1.7), (2.2, 1.6), (1.2, 1.3))):
        lo, hi = 8 - s / 2, 8 + s / 2
        kw = {'rotation': {'origin': [8, 8, z - ln / 2], 'axis': 'z', 'angle': 45 if k % 2 else 22.5}} if k else {}
        el.append(_box([lo, lo, z - ln], [hi, hi, z], '#bit' if k else '#collar', local=True, **kw))
        z -= ln
    # the listening horn and the music drum
    el += [
        _box([7, 14, 11], [9, 16.4, 13], '#collar', local=True),
        _box([6.5, 16.4, 10.5], [9.5, 17.6, 13.5], '#coil', local=True),
        _box([5.5, 17.6, 9.5], [10.5, 18.3, 14.5], '#collar', local=True),
        _box([0.2, 5.5, 10.2], [1.8, 10.5, 13.8], '#collar', local=True),
    ]
    return el


def gen_block(GA, bid):
    tex = {'particle': f'{NS}:block/{bid}_casing'}
    for n in ('plate', 'casing', 'breech', 'rail', 'collar'):
        tex[n] = f'{NS}:block/{bid}_{n}'
    m = {'parent': 'minecraft:block/block', 'textures': tex, 'elements': housing()}
    GA.note_textures(m)
    GA.write(os.path.join(GA.A, 'models/block', bid + '.json'), m)
    itex = dict(tex)
    for n in ('barrel', 'coil', 'bit'):
        itex[n] = f'{NS}:block/{bid}_{n}'
    item = {'parent': 'minecraft:block/block', 'textures': itex, 'elements': housing() + still_gun(), 'display': {
        'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.45, 0.45, 0.45]},
        'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.35, 0.35, 0.35]},
        'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.38, 0.38, 0.38]},
        'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.38, 0.38, 0.38]}}}
    GA.note_textures(item)
    GA.write(os.path.join(GA.A, 'models/block', bid + '_item.json'), item)
    variants = {}
    for facing, r in ROT.items():
        for charging in ('false', 'true'):
            for powered in ('false', 'true'):
                for rng in range(3):
                    v = {'model': f'{NS}:block/{bid}'}
                    v.update(r)
                    variants[f'charging={charging},facing={facing},powered={powered},range={rng}'] = v
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})
    GA.item_block(bid, bid + '_item')


# ============================================================================ block textures (16x16, drawn here)

def _ramp(*cs):
    return [tuple(int(c.lstrip('#')[i:i + 2], 16) for i in (0, 2, 4)) + (255,) for c in cs]


SOUL = _ramp('#1f1814', '#2c231c', '#3a2f26', '#4a3d31', '#5b4b3c', '#6f5c49', '#88725c', '#a38c73')
IRON = _ramp('#121519', '#1b1f26', '#262b34', '#323944', '#414a58', '#56606f', '#727e8f', '#97a3b3')
COPPER = _ramp('#4a2210', '#6b3218', '#8f4523', '#b55c30', '#cf7443', '#e5925f', '#f5b388', '#ffd8b8')
BRASS = _ramp('#4a3510', '#6d5019', '#8f6c24', '#b08933', '#c9a046', '#ddb95e', '#efd48a', '#fff0c4')
CYAN = _ramp('#0d3a48', '#16627a', '#1f8fae', '#3fc6e0', '#5fe9ff', '#a8f8ff', '#e6feff')
BIT = _ramp('#26394a', '#36506a', '#4f7591', '#6f95b0', '#93b7cd', '#b8d6e6', '#ddf0f9', '#f6fcff')
PINK = _ramp('#7a2f63', '#b5579a', '#f29bd6', '#ffd6f0')


class _T:
    """A 16x16 canvas painted with ramp indices plus noise, then resolved to colours."""

    def __init__(self, ramp, base, seed, noise=0.9):
        self.ramp = ramp
        self.rnd = random.Random(seed)
        self.v = [[float(base) for _ in range(16)] for _ in range(16)]
        self.fixed = {}
        # clustered tone noise: a coarse 4x4 field plus fine speckle
        coarse = [[self.rnd.uniform(-1, 1) for _ in range(5)] for _ in range(5)]
        for y in range(16):
            for x in range(16):
                cx, cy = x / 4.0, y / 4.0
                ix, iy = int(cx), int(cy)
                fx, fy = cx - ix, cy - iy
                c = (coarse[iy][ix] * (1 - fx) * (1 - fy) + coarse[iy][ix + 1] * fx * (1 - fy) + coarse[iy + 1][ix] * (1 - fx) * fy
                     + coarse[iy + 1][ix + 1] * fx * fy)
                self.v[y][x] += noise * (0.55 * c + 0.45 * self.rnd.uniform(-1, 1))

    def add(self, x, y, d):
        if 0 <= x < 16 and 0 <= y < 16:
            self.v[y][x] += d

    def set(self, x, y, i):
        if 0 <= x < 16 and 0 <= y < 16:
            self.v[y][x] = i

    def put(self, x, y, rgba):
        if 0 <= x < 16 and 0 <= y < 16:
            self.fixed[(x, y)] = rgba

    def bevel(self, x0, y0, x1, y1, lit=1.4, dark=-1.4):
        """A raised panel edge: lit top and left, shaded bottom and right."""
        for x in range(x0, x1 + 1):
            self.add(x, y0, lit)
            self.add(x, y1, dark)
        for y in range(y0 + 1, y1):
            self.add(x0, y, lit * 0.7)
            self.add(x1, y, dark * 0.7)

    def groove(self, x0, y0, x1, y1):
        """A carved line: a dark cut with a lit lip under it."""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                self.set(x, y, 0.6)
        if y0 == y1:
            for x in range(x0, x1 + 1):
                self.add(x, y1 + 1, 0.9)
        else:
            for y in range(y0, y1 + 1):
                self.add(x1 + 1, y, 0.9)

    def rivet(self, x, y, pal):
        """A 2x2 brass rivet: highlight, body, shadow - and a dark ring under it."""
        self.put(x, y, pal[7])
        self.put(x + 1, y, pal[5])
        self.put(x, y + 1, pal[4])
        self.put(x + 1, y + 1, pal[2])
        self.add(x + 2, y + 1, -1.0)
        self.add(x + 1, y + 2, -1.0)

    def img(self):
        im = Image.new('RGBA', (16, 16))
        px = im.load()
        n = len(self.ramp)
        for y in range(16):
            for x in range(16):
                if (x, y) in self.fixed:
                    px[x, y] = self.fixed[(x, y)]
                else:
                    px[x, y] = self.ramp[max(0, min(n - 1, int(round(self.v[y][x]))))]
        return im


def _plate():
    """The back plate: a bevelled soulstone slab, brass rivets at its corners, the Echoer's ear carved in cyan."""
    t = _T(SOUL, 3.6, 501)
    t.bevel(0, 0, 15, 15, 1.6, -1.6)
    t.bevel(2, 2, 13, 13, -0.8, 0.8)  # a sunken field
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        t.rivet(x, y, BRASS)
    # concentric sound arcs (the Echoer's ear), cut and glowing
    for x in range(3, 13):
        for y in range(3, 13):
            r = math.hypot(x - 7.5, y - 8.5)
            if (1.6 < r < 2.4 or 3.6 < r < 4.4) and y <= 9:
                t.put(x, y, CYAN[3] if r < 3 else CYAN[2])
                t.add(x, y + 1, -1.0)
            elif r <= 1.0:
                t.put(x, y, CYAN[5])
    return t.img()


def _casing():
    """The breech's sides: two soulstone panels with vent slots, a copper band between them."""
    t = _T(SOUL, 3.5, 502)
    t.bevel(0, 0, 15, 6, 1.3, -1.3)
    t.bevel(0, 9, 15, 15, 1.3, -1.3)
    for x in range(16):
        for y in (7, 8):
            c = COPPER[5 if y == 7 else 3] if (x + y) % 5 else COPPER[6]
            t.put(x, y, c)
    for x in (2, 7, 12):
        t.rivet(x, 7, BRASS)
    for vx in (3, 6, 9, 12):  # vent slots in the upper panel
        for vy in range(2, 5):
            t.put(vx, vy, SOUL[0])
            t.put(vx + 1, vy, SOUL[1])
        t.add(vx, 5, 1.0)
        t.add(vx + 1, 5, 1.0)
    # a running rune in the lower panel
    for x, y in ((3, 12), (4, 11), (5, 12), (7, 11), (7, 12), (8, 12), (10, 11), (11, 12), (12, 11)):
        t.put(x, y, CYAN[2])
    return t.img()


def _breech():
    """The breech's face: a copper socket ring round the barrel's mouth."""
    t = _T(SOUL, 3.4, 503)
    t.bevel(0, 0, 15, 15, 1.2, -1.2)
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 4.2:
                t.put(x, y, IRON[1] if r < 3.2 else IRON[3])
            elif r < 6.2:
                lit = (x - 7.5) + (y - 7.5) < 0
                t.put(x, y, COPPER[5 if lit else 3] if r < 5.4 else COPPER[2 if not lit else 4])
    for a in range(8):
        x = round(7.5 + math.cos(a * math.pi / 4) * 5.0 - 0.5)
        y = round(7.5 + math.sin(a * math.pi / 4) * 5.0 - 0.5)
        t.put(x, y, BRASS[7])
    return t.img()


def _rail():
    """Dark riveted iron. The rails and the plate's rims are 2-texel strips of its border bands, so those carry the
    detail: a lit outer edge, a row of bolts, a dark seam; the middle is a worn plate with a cross seam."""
    t = _T(IRON, 3.3, 504, noise=0.8)
    for i in range(16):
        for a_, b_ in ((0, 1), (15, 14)):
            t.add(i, a_, 1.8 if a_ == 0 else -1.6)
            t.add(a_, i, 1.4 if a_ == 0 else -1.4)
            t.add(i, b_, 0.5)
            t.add(b_, i, 0.4)
        t.add(i, 2, -1.3)
        t.add(2, i, -1.1)
        t.add(i, 13, -1.0)
        t.add(13, i, -0.9)
    for i in range(1, 16, 4):  # bolts along every band
        for x, y in ((i, 0), (i, 14), (0, i), (14, i)):
            t.put(x, y, IRON[7])
            t.put(x + 1, y, IRON[5])
            t.put(x, y + 1, IRON[5])
            t.put(x + 1, y + 1, IRON[2])
    for i in range(3, 13):  # the middle plate's cross seam and a scratch of bare metal
        t.set(i, 8, 1.0)
        t.add(i, 9, 1.0)
        t.set(8, i, 1.0)
        t.add(9, i, 0.9)
    for x, y in ((4, 4), (5, 5), (11, 11), (10, 12)):
        t.add(x, y, 2.0)
    return t.img()


def _collar():
    """The brass collar: polished bands (the bars are its 2-texel borders) with copper rivets and engraved notes; the
    middle (seen on the item's muzzle and horn) a sunken field with an engraved ring."""
    t = _T(BRASS, 4.0, 505, noise=0.7)
    for i in range(16):
        t.add(i, 0, 2.6)
        t.add(0, i, 2.0)
        t.add(i, 1, 0.8)
        t.add(1, i, 0.6)
        t.add(i, 15, -2.4)
        t.add(15, i, -2.0)
        t.add(i, 14, -0.6)
        t.add(14, i, -0.5)
        t.add(i, 2, -1.6)
        t.add(2, i, -1.4)
    for x in range(3, 13):
        for y in range(3, 13):
            t.add(x, y, -0.9)
    for i in range(3, 13, 4):  # engraved notes running along the bands
        for x, y in ((i, 1), (i + 1, 0), (i, 14), (1, i), (14, i + 1)):
            t.set(x, y, 1.0)
    for x, y in ((0, 0), (14, 0), (0, 14), (14, 14), (7, 0), (7, 14), (0, 7), (14, 7)):
        t.put(x, y, COPPER[7])
        t.put(x + 1, y, COPPER[5])
        t.put(x, y + 1, COPPER[4])
        t.put(x + 1, y + 1, COPPER[2])
    for x in range(16):  # an engraved ring in the middle field
        for y in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 3.4 < r < 4.3:
                t.add(x, y, -1.6)
            elif 4.3 <= r < 4.9:
                t.add(x, y, 1.2)
    return t.img()


def _barrel():
    """(item) The barrel: gunmetal with a glowing rune channel down the middle."""
    t = _T(IRON, 3.4, 506)
    for y in range(16):
        t.add(0, y, 1.4)
        t.add(15, y, -1.4)
        t.put(7, y, CYAN[3] if y % 3 else CYAN[5])
        t.put(8, y, CYAN[2] if y % 3 else CYAN[4])
        t.add(6, y, -1.2)
        t.add(9, y, 0.9)
    for y in (2, 13):
        for x in range(16):
            if x not in (7, 8):
                t.put(x, y, COPPER[4] if x % 3 else COPPER[6])
    return t.img()


def _coil():
    """(item) A copper coil: windings, cyan light between the two halves."""
    t = _T(COPPER, 4.0, 507, noise=0.4)
    for y in range(16):
        for x in range(16):
            if 7 <= x <= 8:
                t.put(x, y, CYAN[4] if (y % 4) else CYAN[6])
            else:
                t.add(x, y, 1.6 if y % 2 == 0 else -1.4)
    return t.img()


def _bit():
    """(item) The Siftite drill bit: pale steel with spiralling flutes and pink glints."""
    t = _T(BIT, 4.2, 508, noise=0.5)
    for y in range(16):
        for x in range(16):
            d = (x + y * 2) % 8
            if d == 0:
                t.set(x, y, 7.0)
            elif d == 1:
                t.add(x, y, 1.2)
            elif d in (5, 6):
                t.add(x, y, -1.8)
    for x, y in ((3, 4), (11, 9), (6, 13)):
        t.put(x, y, PINK[2])
        t.put(x + 1, y, PINK[3])
    return t.img()


def block_textures(out):
    for name, fn in (('plate', _plate), ('casing', _casing), ('breech', _breech), ('rail', _rail), ('collar', _collar),
                     ('barrel', _barrel), ('coil', _coil), ('bit', _bit)):
        out(f'block/echoer_device_{name}', fn())


# ============================================================================ sounds and text

SOUNDS = {
    'block.echoer_device.beat': [('event:block.note_block.hat', 0.6, 1.3), ('block/sculk_sensor/sculk_clicking1', 0.5, 1.6)],
    'block.echoer_device.bite': [('event:block.grindstone.use', 0.7, 0.8), ('dig/stone1', 0.8, 0.7)],
}
SUBTITLES = {'block.echoer_device.beat': 'Echoer Drill hears a beat', 'block.echoer_device.bite': 'Echoer Drill bites'}

LANG = {
    f'message.{NS}.echoer_device.range': 'The Echoer Drill will reach %s blocks',
    f'message.{NS}.echoer_device.program': 'Echoer Drill: %s, %s, %s blocks',
    f'echoer_drill.{NS}.pattern.0': 'a single echo shot', f'echoer_drill.{NS}.pattern.1': 'a bore',
    f'echoer_drill.{NS}.pattern.2': 'a wide bore', f'echoer_drill.{NS}.pattern.3': 'a vein hunt',
    f'echoer_drill.{NS}.slope.0': 'stairs down', f'echoer_drill.{NS}.slope.1': 'straight ahead', f'echoer_drill.{NS}.slope.2': 'stairs up',
    f'codex.{NS}.echoer_device.title': 'Echoer Drill', f'codex.{NS}.echoer_device.tagline': 'A drill cannon that digs to a rhythm',
    f'codex.{NS}.echoer_device.body': (
        'An Echoer\'s gift: a soulstone drill cannon with coils round its barrel and a horn that listens. Give it a rhythm - '
        'redstone pulses, taps by hand, notes played or note blocks within eight blocks - and when the rhythm rests it drills. '
        'The beats pick the pattern: 1 an echo shot at the first block in reach, 2 a bore, 3 a wide 3x3 bore, 4 or more a vein '
        'hunt that eats a whole ore vein. The tempo picks the way: fast digs stairs down, slow stairs up, steady straight '
        'ahead. The loudest beat sets the depth: a pulse\'s strength, a note\'s pitch or the dial (sneak-use: 4, 8 or 16). '
        'Drops go into a touching container. It stops at unbreakable blocks and anything that holds things.'),
}
