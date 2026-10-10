"""CAVE v4: the Jailer (`thesift:jailer`), designed afresh - a hulking, eyeless sculk warden whose own ribcage is
the cell.

It goes on four limbs like a great ape: digitigrade hind legs under a heavy pelvis hung with a ring of bone keys,
a spine arching forward from the hips into an enormous hollow ribcage - six ribs a side over a backbone of
vertebrae studded with dark Sculkite crystal, the ribs curving in under until the two sides all but meet - and at the front
of it massive shoulders, long arms knuckling on bone claws, and a low-hung, eyeless skull with a bone mask full of
sensory pits, a heavy jaw and two great curling sensory tendrils. Inside the ribcage hangs its heart, a sac of
soul light; chains hang from its backbone, and a bone padlock bars the front. Glowing sculk veins run through all of
its flesh.

The victim is shut inside the ribcage: the cell sits where the old cell was carried (centred CAGE_FWD units in
front of the Jailer, its roof CAGE_TOP, its floor 0.3 blocks off the ground; Jailer.CAGE_FORWARD / CAGE_LIFT), and
its twelve ribs are the twelve loose bars that snap and regrow (Jailer.CAGE_WHOLE). The ribcage, shoulders, arms
and head form the 'cage' part (JailerModel moves it in the slam: it rears, the ribs spread, and it crashes down
over you); the hips, spine and legs hang from 'body' and the root.

Hook: cave_creatures.MODELS['jailer'] (the spawn egg is a classic two-colour egg, tools/egg_colours.json on main).
"""
from __future__ import annotations

import math
import random

from modelkit import Model

CAGE_FWD = 18.4
CAGE_TOP = -12.8
CAGE_H = 32
HALF = 9.5
RIB_Z = (-8.75, -5.25, -1.75, 1.75, 5.25, 8.75)
# the ribs that carry a chain (chain_0..3): left front, right front, left back, right back
CHAIN_RIBS = (0, 6, 5, 11)

PAL = {
    'flesh': '#0b2a33', 'flesh_l': '#164553', 'flesh_d': '#05151b',
    'hide': '#0f3540', 'hide_l': '#1b5361', 'hide_d': '#081f26',
    'bone': '#d9d0b8', 'bone_l': '#f2ecd8', 'bone_d': '#9f967e',
    'rib': '#bdb497', 'rib_l': '#d9d1b6', 'rib_d': '#7f775f',
    'vein': '#2fe6f0', 'vein_d': '#127f8a', 'soul': '#5ff8ff', 'soul_l': '#d2fffc', 'soul_d': '#16a6b0',
    'chain': '#36414a', 'chain_l': '#5a6872', 'chain_d': '#1c2328',
    'crystal': '#122640', 'crystal_l': '#21456c', 'crystal_d': '#0a1424', 'crystal_g': '#4ff0e8',
    'mouth': '#02080b', 'gullet': '#29dfeb', 'tooth': '#e8e0c8', 'pit': '#010506',
    'claw': '#1e262d', 'claw_l': '#3a4854', 'claw_d': '#0e1317',
    'tendril': '#0e4450', 'tendril_l': '#196270', 'tendril_d': '#072a32', 'tip': '#3ff5e6', 'tip_l': '#c8fffb',
}
MATERIALS = {'flesh': 'sculk', 'hide': 'sculk', 'bone': 'bone', 'rib': 'bone', 'chain': 'metal', 'crystal': 'crystal', 'claw': 'chitin',
             'tendril': 'sculk', 'tooth': 'bone'}
SKIN_KEYS = {'v': 'vein', 'V': 'vein_d', 'd': 'flesh_d', 'l': 'flesh_l', 'p': 'pit'}
BONE_KEYS = {'c': 'bone_d', 'h': 'bone_l', 'f': 'flesh', 'F': 'flesh_d', 'n': 'vein'}
RIB_KEYS = {'c': 'rib_d', 'h': 'rib_l', 'f': 'flesh', 'F': 'flesh_d', 'n': 'vein'}


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def grid(w, h, fn):
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def flip(rows):
    return [r[::-1] for r in rows]


SKIP = dict(skip=True)


# --------------------------------------------------------------------------- texel maps

def _skin(w, h, seed, veins=1, pits=0.01):
    """Sculk flesh (hd map): glowing veins wandering through it (v, with a dim halo V), dark sensory pores (p)
    and darker and lighter mottling (d / l)."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(int(w * h * 0.025)):
        x, y = rnd.randrange(w), rnd.randrange(h)
        t = 'd' if rnd.random() < 0.7 else 'l'
        for dx, dy in ((0, 0), (1, 0), (0, 1))[:rnd.randrange(1, 4)]:
            if x + dx < w and y + dy < h:
                g[y + dy][x + dx] = t
    for _ in range(veins):
        x, y = rnd.randrange(w), rnd.randrange(h)
        dx, dy = rnd.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
        for _ in range(int((w + h) * 0.5)):
            if 0 <= x < w and 0 <= y < h:
                g[y][x] = 'v'
                for ex, ey in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 0 <= x + ex < w and 0 <= y + ey < h and g[y + ey][x + ex] in '.dl' and rnd.random() < 0.3:
                        g[y + ey][x + ex] = 'V'
            if rnd.random() < 0.3:
                dx, dy = rnd.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
            x, y = x + dx, y + dy
    for _ in range(int(w * h * pits)):
        x, y = rnd.randrange(w), rnd.randrange(h)
        if g[y][x] == '.':
            g[y][x] = 'p'
    return [''.join(r) for r in g]


def _bone(w, h, seed, cracks=1, creep=0.25):
    """Old bone (hd map): hairline cracks (c), worn highlights (h) and sculk creeping up from its lower edge
    (f / F) with a glowing node or two in it (n)."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(cracks):
        x, y = rnd.randrange(w), rnd.randrange(h)
        for _ in range(rnd.randrange(3, 7)):
            if 0 <= x < w and 0 <= y < h:
                g[y][x] = 'c'
            x += rnd.choice((-1, 0, 1))
            y += 1
    for _ in range(int(w * h * 0.03)):
        x, y = rnd.randrange(w), rnd.randrange(max(1, h // 2))
        if g[y][x] == '.':
            g[y][x] = 'h'
    for x in range(w):
        reach = int(h * creep * (0.4 + 0.6 * abs(math.sin(x * 0.7 + seed))))
        for y in range(h - reach, h):
            g[y][x] = 'F' if y == h - reach else 'f'
    for _ in range(max(1, int(w * creep * 0.3))):
        x, y = rnd.randrange(w), h - 1 - rnd.randrange(max(1, int(h * creep * 0.4)))
        if 0 <= y < h and g[y][x] in 'fF':
            g[y][x] = 'n'
    return [''.join(r) for r in g]


def _rib(w, h, seed):
    """A rib's long face (hd map, rows down the rib): bone, cracked and worn, bound every so often by a ring of
    sculk flesh (f) with a glowing vein bead (n) in it."""
    rnd = random.Random(seed)
    g = [list(r) for r in _bone(w, h, seed, cracks=2, creep=0.0)]
    y = 4 + rnd.randrange(4)
    while y < h - 2:
        for x in range(w):
            g[y][x] = 'f'
            g[y + 1][x] = 'F'
        g[y][rnd.randrange(w)] = 'n'
        y += 6 + rnd.randrange(5)
    return [''.join(r) for r in g]


def _vertebrae(w, h, seed, every=6):
    """A run of vertebrae (hd map, rows along the spine): bone bodies with dark joints (c) and sculk tissue
    swelling between them (f) lit by a vein (n)."""
    rnd = random.Random(seed)
    out = []
    for y in range(h):
        k = y % every
        if k == every - 1:
            out.append(''.join('n' if x == w // 2 and rnd.random() < 0.7 else 'f' for x in range(w)))
        elif k == 0:
            out.append('c' * w)
        elif k == 1:
            out.append('h' * w)
        else:
            out.append(''.join('c' if rnd.random() < 0.06 else '.' for _ in range(w)))
    return out


def _across(rows):
    """A map turned on its side (rows become columns): a run along a cube's length seen on its side faces."""
    return [''.join(r[y] for r in rows) for y in range(len(rows[0]))]


def _mask(w, h):
    """The front of the skull (hd map): an eyeless bone mask - a heavy brow, rows of deep sensory pits where eyes
    would be (p, the deepest faintly lit V), a dark nasal pit, cracks, and sculk creeping in round the edges."""
    cx = (w - 1) / 2.0

    def px(x, y):
        dx = abs(x - cx)
        if y <= 1:
            return 'h' if y == 0 else '.'
        if y == 2 and 1 < dx < w * 0.45:
            return 'c'
        # the sensory pits, two clusters where eyes would sit
        for pxc, pyc in ((w * 0.26, h * 0.38), (w * 0.74, h * 0.38)):
            r = math.hypot(x - pxc, (y - pyc) * 1.3)
            if r < 1.0:
                return 'V'
            if r < 2.4 and (x + y) % 2 == 0:
                return 'p'
        # the nasal pit
        if y >= h * 0.55 and dx < (y - h * 0.55) * 0.55 + 0.6 and y < h - 2:
            return 'p'
        if (x < 2 or x > w - 3) and (x + y) % 3:
            return 'f'
        if y >= h - 2:
            return 'F' if (x * 7) % 5 else 'f'
        return 'c' if (x * 13 + y * 7) % 37 == 0 else '.'
    return grid(w, h, px)


def _teeth(w, h):
    """The jaw from above (hd map): a row of long teeth along the front (t), the dark mouth behind, the gullet
    glowing in its depths (g)."""
    return grid(w, h, lambda x, y: 't' if y < 2 and x % 2 == 0 and 0 < x < w - 1 else 'm' if y < 2 else
                'g' if y > h * 0.55 and abs(x - (w - 1) / 2.0) < w * 0.22 else 'm')


# --------------------------------------------------------------------------- the model

def jailer() -> Model:
    m = Model('jailer', (192, 144), dict(PAL), {'jailer': {}}, res=2, materials=MATERIALS)
    m.exact_uv = True

    def skin(wu, hu, seed, color='flesh', **kw):
        d = dict(color=color, pattern='mc', clusters=0.2, hd=True, map=_skin(int(wu * 2), int(hu * 2), seed, **kw), keys=SKIN_KEYS,
                 glow_keys='vV', map_material=True)
        return d

    def bone(wu, hu, seed, color='bone', keys=None, **kw):
        return dict(color=color, pattern='mc', clusters=0.15, hd=True, map=_bone(int(wu * 2), int(hu * 2), seed, **kw),
                    keys=keys or BONE_KEYS, glow_keys='n', map_material=True)

    def skinned(size, seed, color='flesh', **over):
        w, h, d = size
        f = {'north': skin(w, h, seed), 'south': skin(w, h, seed + 1), 'east': skin(d, h, seed + 2), 'west': skin(d, h, seed + 3),
             'up': skin(w, d, seed + 4), 'down': mc(color + '_d' if color + '_d' in PAL else color, clusters=0.2)}
        for k in f:
            if isinstance(f[k], dict) and f[k].get('hd'):
                f[k]['color'] = color
        f.update(over)
        return f

    # ================================================================ the hind quarters (root: hips, spine, legs)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(5.5 * sx, 2, 4), rot=(-0.5, 0, 0))
        leg.cube((-2.5, -2, -3), (5, 13, 6), **mc('flesh'), faces=skinned((5, 13, 6), 10 + sx))
        shin = leg.part(f'{side}_shin', pivot=(0, 11, 0.5), rot=(1.0, 0, 0))
        shin.cube((-2, 0, -2), (4, 11, 4), **mc('hide'), faces=skinned((4, 11, 4), 20 + sx, color='hide', **{
            'north': bone(4, 11, 24 + sx, creep=0.35)}))
        foot = shin.part(f'{side}_foot', pivot=(0, 11, 0), rot=(-0.5, 0, 0))
        foot.cube((-2.5, 0, -4.5), (5, 2.5, 6.5), **mc('hide'), faces=skinned((5, 2.5, 6.5), 30 + sx, color='hide'))
        for k, cx in enumerate((-1.75, 0, 1.75)):
            foot.cube((cx - 0.5, 1, -7), (1, 1.5, 2.5), **mc('claw', clusters=0.0, rim=False), faces={'north': mc('claw_l', clusters=0.0, rim=False)})

    body = m.part('body', pivot=(0, 2, 4))
    body.cube((-6, -4.5, -4), (12, 9, 8.5), **mc('flesh'), faces=skinned((12, 9, 8.5), 40))
    # the pelvis: wings of bone over the hips
    for sx in (1, -1):
        body.cube((4.5 if sx > 0 else -6.5, -5.5, -3.5), (2, 6, 7), **mc('bone'), faces={
            'east': bone(7, 6, 41 + sx, creep=0.3), 'west': bone(7, 6, 43 + sx, creep=0.3), 'up': bone(2, 7, 45 + sx, creep=0.0)})
    # the ring of bone keys at its hip
    keys = body.part('keys', pivot=(6.5, 1, 1), rot=(0, 0, -0.15))
    keys.cube((-0.25, 0, -1.5), (0.5, 3, 3), **mc('bone_d', clusters=0.0, rim=False), faces={
        'east': dict(mc('bone_d', clusters=0.0, rim=False), hd=True, map=['_cccc_', 'c____c', 'c____c', 'c____c', 'c____c', '_cccc_'],
                     keys={'c': 'bone_d'}),
        'west': dict(mc('bone_d', clusters=0.0, rim=False), hd=True, map=['_cccc_', 'c____c', 'c____c', 'c____c', 'c____c', '_cccc_'],
                     keys={'c': 'bone_d'})})
    for k, (kz, ln, rz) in enumerate(((-1.0, 4.5, 0.2), (0.0, 5.5, -0.05), (1.0, 4.0, -0.25))):
        key = keys.part(f'key_{k}', pivot=(0, 2.5, kz), rot=(0, 0, rz))
        key.cube((-0.25, 0, -0.5), (0.5, ln, 1), **mc('bone', clusters=0.0, rim=False))
        key.cube((-0.25, ln - 1.5, -1.25), (0.5, 1.5, 1), **mc('bone_l', clusters=0.0, rim=False))
    # the spine arching forward from the hips into the ribcage, crested with Sculkite
    spine = body.part('spine', pivot=(0, -3.5, 0), rot=(0.81, 0, 0))
    spine.cube((-2.5, -18, -2.5), (5, 18, 5), **mc('flesh'), faces=skinned((5, 18, 5), 50, **{
        'south': dict(color='bone', pattern='mc', clusters=0.1, hd=True, map=_vertebrae(10, 36, 51), keys=BONE_KEYS, glow_keys='n', map_material=True)}))
    for k in range(4):
        spike = spine.part(f'spine_spike_{k}', pivot=(0, -3 - k * 4.2, 2.5), rot=(0.9, 0, 0))
        spike.cube((-0.75, -0.5, 0), (1.5, 1.5, 2.5 + k * 0.5), **mc('crystal' if k % 2 else 'bone', clusters=0.0, rim=False,
                                                                        glow=bool(k % 2)))

    # ================================================================ the ribcage (the cell), shoulders, arms and head
    # the ribcage hangs from the hips (a child of 'body'), so the torso pivots from them as one creature
    cage = body.part('cage', pivot=(0, CAGE_TOP - 2, -CAGE_FWD - 4))
    # the backbone along the roof: vertebrae, Sculkite crystals and bone spikes along the top
    cage.cube((-2.5, -3, -11), (5, 4, 22), **mc('bone'), faces={
        'up': dict(color='bone', pattern='mc', clusters=0.1, hd=True, map=_vertebrae(10, 44, 60), keys=BONE_KEYS, glow_keys='n', map_material=True),
        'east': dict(color='bone', pattern='mc', clusters=0.1, hd=True, map=_across(_vertebrae(8, 44, 61)), keys=BONE_KEYS,
                     glow_keys='n', map_material=True),
        'west': dict(color='bone', pattern='mc', clusters=0.1, hd=True, map=flip(_across(_vertebrae(8, 44, 62))), keys=BONE_KEYS,
                     glow_keys='n', map_material=True),
        'down': skin(5, 22, 63)})
    for k, z in enumerate((-8, -3.5, 1, 5.5)):
        cr = cage.part(f'crystal_{k}', pivot=(0.5 if k % 2 else -0.5, -3, z), rot=(0.25 * (1 if k % 2 else -1), 0, 0.3 * (1 if k % 2 else -1)))
        cr.cube((-1, -3.5 - k % 2, -1), (2, 3.5 + k % 2, 2), **mc('crystal', clusters=0.0, rim=False), faces={
            'up': mc('crystal_l', clusters=0.0, rim=False, glow=True)})
        cr.cube((-0.5, -5 - k % 2, -0.5), (1, 1.5, 1), **mc('crystal_g', clusters=0.0, rim=False, glow=True))
    # the ribs: the twelve loose bars, each in three pieces - out from the backbone, down and bowed out, and
    # curving in under until the two sides all but meet - sloping back a little, bound with sculk and lit by its veins
    for i in range(12):
        sx = 1 if i < 6 else -1
        z = RIB_Z[i % 6]

        def ribface(wu, hu, seed):
            return {k: dict(color='rib', pattern='mc', clusters=0.1, hd=True, map=_rib(int(wu * 2), int(hu * 2), seed + j), keys=RIB_KEYS,
                            glow_keys='n', map_material=True) for j, k in enumerate(('north', 'south', 'east', 'west'))}
        bar = cage.part(f'bar_{i}', pivot=(2 * sx, -0.5, z), rot=(0, 0, -1.15 * sx))
        bar.cube((-0.75, 0, -0.5), (1.5, 7.5, 1), **mc('rib'), faces=ribface(1.5, 7.5, 80 + i * 5))
        mid = bar.part(f'bar_{i}_mid', pivot=(0, 7, 0), rot=(0.1, 0, 1.07 * sx))
        mid.cube((-0.5, 0, -0.5), (1, 26, 1), **mc('rib'), faces=ribface(1, 26, 140 + i * 5))
        low = mid.part(f'bar_{i}_low', pivot=(0, 25.5, 0), rot=(-0.1, 0, 1.33 * sx))
        low.cube((-0.5, 0, -0.5), (1, 9.5, 1), **mc('rib'), faces=ribface(1, 9.5, 200 + i * 5))
        if i == 0:
            # the bone padlock that locks the cell hangs from the foot of the front left rib, where the ribs meet
            lock = low.part('lock', pivot=(0, 9.5, -0.5), rot=(0, 0, -1.25))
            lock.cube((-1, 0, -0.25), (2, 1.5, 0.5), **mc('chain_d', clusters=0.0, rim=False))
            lock.cube((-2, 1.5, -1), (4, 3.5, 1.5), **mc('bone'), faces={
                'north': dict(mc('bone', clusters=0.1), hd=True, map=['hhhhhhhh', 'h......c', '...gg...', '...gg...', '....g...', 'c......c',
                                                                      'cccccccc'], keys={'h': 'bone_l', 'c': 'bone_d', 'g': 'soul'}, glow_keys='g')})
        if i in CHAIN_RIBS:
            # a chain hangs from the bend of each corner rib, just in front of (or behind) it, swinging as it moves
            front = RIB_Z[i % 6] < 0
            ch = bar.part(f'chain_{CHAIN_RIBS.index(i)}', pivot=(0, 7.5, -1.25 if front else 1.25), rot=(0, 0, 1.15 * sx))
            for n in range(5):
                if n % 2 == 0:
                    ch.cube((-0.5, n * 2, -0.25), (1, 2.5, 0.5), **mc('chain', clusters=0.0, rim=False), faces={'north': mc('chain_l', clusters=0.0, rim=False)})
                else:
                    ch.cube((-0.25, n * 2, -0.5), (0.5, 2.5, 1), **mc('chain_d', clusters=0.0, rim=False))
    # its heart, a sac of soul light hanging from the backbone inside the ribcage, on sculk strands
    heart = cage.part('heart', pivot=(0, 1, -1))
    for hx, hz in ((-1.25, -1.25), (1.25, 1.25), (1.25, -1.25)):
        heart.cube((hx - 0.25, 0, hz - 0.25), (0.5, 1.5, 0.5), **mc('flesh_l', clusters=0.0, rim=False))
    heart.cube((-1.5, 1.5, -1.5), (3, 3, 3), **mc('soul', clusters=0.2, rim=False, glow=True), faces={
        k: dict(mc('soul', clusters=0.0, rim=False, glow=True), hd=True, map=['.VV.V.', 'V.lL.V', '.lLLlV', 'VlLL..', '.V.lV.', '..VV..'],
                keys={'V': 'soul_d', 'l': 'soul', 'L': 'soul_l'}, glow_keys='VlL') for k in ('north', 'south', 'east', 'west')})
    # the floor that grows across the cell when someone is in it: slats of bone webbed with sculk
    floor = cage.part('floor', pivot=(0, CAGE_H - 1.5, 0))
    for fz in (-6, -2, 2, 6):
        floor.cube((-HALF + 1.5, 0, fz - 0.75), (HALF * 2 - 3, 1, 1.5), **mc('rib', clusters=0.2, rim=False))
    floor.cube((-HALF + 1.5, 0.25, -9), (HALF * 2 - 3, 0.5, 18), **mc('flesh', clusters=0.3, rim=False), faces={'up': skin(HALF * 2 - 3, 18, 90)})

    # ---- the shoulders: a great hunched mass over the front of the ribcage, scaled with bone plates
    cage.cube((-12, -6, -14), (24, 8, 8), **mc('flesh'), faces=skinned((24, 8, 8), 100, **{'up': skin(24, 8, 101, veins=3)}))
    for sx in (1, -1):
        cage.cube((7.5 if sx > 0 else -12.5, -7, -13.5), (5, 4, 7), **mc('bone'), faces={
            'up': bone(5, 7, 102 + sx, creep=0.2), 'east': bone(7, 4, 104 + sx), 'west': bone(7, 4, 106 + sx), 'north': bone(5, 4, 108 + sx)})

    # ---- long arms knuckling on bone claws
    for side, sx in (('left', 1), ('right', -1)):
        arm = cage.part(f'{side}_arm', pivot=(11 * sx, -1, -10), rot=(-0.25, 0, -0.1 * sx))
        arm.cube((-2.5, -1, -2.5), (5, 17, 5), **mc('flesh'), faces=skinned((5, 17, 5), 110 + sx))
        arm.cube((-2.75 if sx > 0 else -2.25, 12, -2.75), (5, 3, 5.5), **mc('bone'), faces={'north': bone(5, 3, 112 + sx), 'up': bone(5, 5.5, 114 + sx)})
        fore = arm.part(f'{side}_forearm', pivot=(0, 15.5, 0), rot=(0.25, 0, 0.1 * sx))
        fore.cube((-2, 0, -2), (4, 17, 4), **mc('hide'), faces=skinned((4, 17, 4), 120 + sx, color='hide', **{
            'north': bone(4, 17, 122 + sx, creep=0.3)}))
        hand = fore.part(f'{side}_hand', pivot=(0, 17, 0))
        hand.cube((-2.5, 0, -3), (5, 2.5, 5), **mc('hide'), faces=skinned((5, 2.5, 5), 124 + sx, color='hide'))
        claw = hand.part(f'{side}_claw', pivot=(0, 1.5, -3), rot=(0.35, 0, 0))
        for k, cx in enumerate((-1.75, 0, 1.75)):
            claw.cube((cx - 0.5, -0.5, -5), (1, 1, 5), **mc('bone', clusters=0.0, rim=False), faces={
                'north': mc('claw', clusters=0.0, rim=False), 'up': mc('bone_l', clusters=0.0, rim=False)})

    # ---- the neck and the eyeless head hung low in front, its mask full of sensory pits, two great tendrils
    neck = cage.part('neck', pivot=(0, -2, -13), rot=(0.65, 0, 0))
    neck.cube((-3.5, -3.5, -7), (7, 7, 8), **mc('flesh'), faces=skinned((7, 7, 8), 130, **{
        'up': dict(color='bone', pattern='mc', clusters=0.1, hd=True, map=_vertebrae(14, 16, 131, every=5), keys=BONE_KEYS, glow_keys='n',
                   map_material=True)}))
    head = neck.part('head', pivot=(0, 0, -7), rot=(-0.4, 0, 0))
    head.cube((-6, -5, -10), (12, 9, 10), **mc('bone'), faces={
        'north': dict(mc('bone', clusters=0.0), hd=True, map=_mask(24, 18), keys={'h': 'bone_l', 'c': 'bone_d', 'p': 'pit', 'V': 'vein_d',
                                                                                  'f': 'flesh', 'F': 'flesh_d'}, glow_keys='V', map_material=True),
        'up': bone(12, 10, 132, creep=0.15), 'east': skin(10, 9, 133), 'west': skin(10, 9, 134), 'south': skin(12, 9, 135), 'down': skin(12, 10, 136)})
    head.cube((-6.5, -6, -10.5), (13, 2, 6.5), **mc('bone'), faces={'up': bone(13, 6.5, 137, creep=0.1), 'north': bone(13, 2, 138, creep=0.0)})
    jaw = head.part('jaw', pivot=(0, 3.5, -1.5))
    jaw.cube((-5.5, 0, -9), (11, 3.5, 9), **mc('flesh'), faces=skinned((11, 3.5, 9), 140, **{
        'up': dict(mc('mouth', clusters=0.0, rim=False), hd=True, map=_teeth(22, 18), keys={'t': 'tooth', 'm': 'mouth', 'g': 'gullet'}, glow_keys='g'),
        'north': dict(mc('bone_d', clusters=0.0), hd=True, map=grid(22, 7, lambda x, y: 't' if y < 2 and x % 2 == 1 else '.'), keys={'t': 'tooth'})}))
    head.cube((-5.5, 3, -10), (11, 1, 1), **mc('tooth', clusters=0.0, rim=False), faces={
        'north': dict(mc('mouth', clusters=0.0, rim=False), hd=True, map=grid(22, 2, lambda x, y: 't' if x % 2 == 0 else 'm'),
                      keys={'t': 'tooth', 'm': 'mouth'})})
    for side, sx in (('left', 1), ('right', -1)):
        ten = head.part(f'{side}_tendril', pivot=(4.5 * sx, -5, -3), rot=(-0.35, 0, -0.55 * sx))
        ten.cube((-1.25, -11, -1.5), (2.5, 11, 3), **mc('tendril'), faces={
            'north': skin(2.5, 11, 150 + sx, color='tendril', veins=1), 'south': skin(2.5, 11, 152 + sx, color='tendril', veins=1)})
        tip = ten.part(f'{side}_tendril_tip', pivot=(0, -11, 0), rot=(0.7, 0, 0.35 * sx))
        tip.cube((-0.75, -7, -1), (1.5, 7, 2), **mc('tendril'), faces={
            'north': dict(mc('tendril', clusters=0.0, rim=False), hd=True, map=['TT', 'TT', 'tt', 'tt', 't.', '..', '.t', '..', '..', '..', '..', '..', '..', '..'],
                          keys={'T': 'tip_l', 't': 'tip'}, glow_keys='Tt')})
        tip.cube((-0.5, -8, -0.5), (1, 1, 1), **mc('tip', clusters=0.0, rim=False, glow=True))
        # small sensory feelers along the brow
        for k in range(2):
            fl = head.part(f'{side}_feeler_{k}', pivot=(1.5 * sx + k * 1.75 * sx, -6, -9.5), rot=(-0.9, 0, -0.3 * sx * (k + 1)))
            fl.cube((-0.25, -3, -0.25), (0.5, 3, 0.5), **mc('tendril_l', clusters=0.0, rim=False))
            fl.cube((-0.5, -3.5, -0.5), (1, 1, 1), **mc('tip', clusters=0.0, rim=False, glow=True))
    return m
