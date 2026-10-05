"""CR4: the Cypole (`thesift:cypole`), the one-eyed cymbal frog of the Sculk Swamp.

A big, squat swamp frog with a single huge golden eye on top of its head, brass tympana on its
cheeks and a pair of brass cymbal plates hanging under its chin either side of its vocal sac. It
croaks in rhythm with every other Cypole around (the plates tick together like a hi-hat), loves to
float in Sculk Water with only its eye showing, and guards the water around it: come too close and
it rattles its plates at you; keep coming and it rears up and crashes them together - a ring
shockwave races out across the ground (jump it!) - or shoots its long sticky tongue to yank you in.

Everything generated for it lives here, hooked through tools/cave_creatures.py (the CR4 module):
declare() (spawn egg), MODELS, SOUNDS/SUBTITLES, data() (loot, tags, particle, band and Codex text),
items() (spawn egg art) and textures() (the shockwave ring particle). Java: registry/ModCaveCreatures
(type, sounds, particle, spawn rules, band voice), entity/swamp/Cypole, client/model/CypoleModel,
client/renderer/CypoleRenderer, client/particle/CymbalRingParticle.
"""
from __future__ import annotations

import math
import os

from modelkit import Model

NS = 'thesift'


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def rows(w, h, fn):
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# =========================================================================== spec (items)

def declare(block, item):
    item('cypole_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModCaveCreatures.CYPOLE.get())', tab='eggs')


# =========================================================================== the model
# Where the mouth is (root model units): the tongue shoots out from here. CypoleModel reads the
# tongue's pivot, so only this file needs to know the numbers.
MOUTH = (0.0, 15.6, -10.6)

PAL = {
    # dark swamp-teal hide, a near-black back with glowing sculk warts, a pale sage belly
    'skin': '#1f4744', 'skin_l': '#2e6560', 'skin_d': '#122c2b',
    'back': '#16332f', 'back_l': '#21493f', 'back_d': '#0b1e1d',
    'mottle': '#2c5a46', 'mottle_d': '#1c3f33',
    'wart': '#3ff5e6', 'wart_d': '#1aa3a0',
    'belly': '#9db39f', 'belly_l': '#c4d5c0', 'belly_d': '#6f8775',
    'sac': '#b7c6a9', 'sac_l': '#dde6cc', 'sac_d': '#86977c',
    'lip': '#122c2b',
    'mouth': '#3b1426', 'gum': '#6c2a45',
    'tongue': '#b14c76', 'tongue_l': '#d9789d', 'tongue_d': '#7b2b4f', 'tip': '#7ff8ec', 'tip_l': '#dafffb',
    # sculk-tarnished brass
    'brass': '#c4912c', 'brass_l': '#ecc865', 'brass_d': '#7c5419', 'brass_hi': '#fff3c2', 'patina': '#4f9a83', 'patina_d': '#2f6b5c',
    'groove': '#93661f',
    # the eye: a dark glossy ball, a golden iris with a frog's flat pupil
    'eyeball': '#14201f', 'eyeball_l': '#26393a', 'gloss': '#cfe9e4',
    'iris': '#f2bd36', 'iris_l': '#ffe58a', 'iris_d': '#b9761c', 'iris_r': '#7a3f12', 'pupil': '#06080a',
    'lid': '#1f4744', 'lid_l': '#2e6560', 'lid_d': '#122c2b',
    'toe': '#2a5a55', 'toe_l': '#3b7871', 'toe_d': '#173634', 'claw': '#c9c0a2',
}
MATS = {'skin': 'skin', 'back': 'sculk', 'belly': 'skin', 'sac': 'jelly', 'brass': 'metal', 'lid': 'skin', 'toe': 'skin', 'tongue': 'jelly',
        'mottle': 'skin'}


def _disc(n, mirror=False, bell=True, patina_seed=0):
    """A brass cymbal at texel scale (n x n): a lit rim, turned grooves running round it, a domed
    bell in the middle with a glowing knot of sculk, and sculk-green patina creeping in from one
    edge. '_' is cut away (the disc is round)."""
    c = (n - 1) / 2

    def px(x, y):
        dx, dy = x - c, y - c
        d = math.hypot(dx, dy) / (n / 2)
        if d > 1.0:
            return '_'
        if d > 0.86:
            # the rim: lit along the top left, dark along the bottom right
            return 'L' if dx + dy < -0.3 * n / 2 else ('D' if dx + dy > 0.4 * n / 2 else 'b')
        if bell and d < 0.24:
            return 'V' if d < 0.1 else ('H' if dx + dy < 0 else 'L')
        if bell and d < 0.33:
            return 'D'
        # tarnish: a patch of patina from one side
        ang = math.atan2(dy, dx) + patina_seed
        if d > 0.55 and math.cos(ang - 2.2) > 0.82 - 0.25 * (d - 0.55):
            return 'p' if (x + y + patina_seed) % 3 else 'P'
        ring = (d * 7.0) % 1.0
        if ring < 0.18:
            return 'g'                                # a turned groove
        # the lit quarter: a soft highlight swept round the top left
        light = -(dx + dy) / (n * 0.7)
        if light > 0.35 and ring > 0.55:
            return 'H' if light > 0.6 and ring > 0.8 else 'L'
        return 'b'
    out = rows(n, n, px)
    return [r[::-1] for r in out] if mirror else out


DISC_KEYS = {'L': 'brass_l', 'D': 'brass_d', 'b': 'brass', 'H': 'brass_hi', 'g': 'groove', 'p': 'patina', 'P': 'patina_d', 'V': 'wart'}


def _warts(w, h, seed, dense=0.07):
    """Glowing sculk warts and dark mottles scattered over the back (texel scale)."""
    import random
    rnd = random.Random(seed)
    grid = [['.'] * w for _ in range(h)]
    for _ in range(int(w * h * dense / 4)):
        x, y = rnd.randrange(1, w - 1), rnd.randrange(1, h - 1)
        if rnd.random() < 0.45:
            grid[y][x] = 'W'
            if rnd.random() < 0.5 and x + 1 < w:
                grid[y][x + 1] = 'w'
            if y + 1 < h:
                grid[y + 1][x] = 'w'
        else:
            ch = 'm' if rnd.random() < 0.6 else 'M'
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)):
                if 0 <= x + dx < w and 0 <= y + dy < h and rnd.random() < 0.8:
                    grid[y + dy][x + dx] = ch
    return [''.join(r) for r in grid]


WART_KEYS = {'W': 'wart', 'w': 'wart_d', 'm': 'mottle_d', 'M': 'mottle'}


def _eye_front(w, h):
    """The eye's socket seen from the front: a round eye in a thin brass ring, skin in the corners
    (the domed cornea and the iris sit in front of it as their own cubes)."""
    cx, cy = (w - 1) / 2, (h - 1) / 2
    rad = min(w, h) / 2

    def px(x, y):
        d = math.hypot(x - cx, (y - cy) * 1.05) / rad
        if d > 1.0:
            return 's' if (x + y) % 5 else 'S'
        if d > 0.84:
            return 'R' if (x - cx) + (y - cy) < 0 else 'r'
        return 'e' if d > 0.7 else 'E'
    return rows(w, h, px)


def _cornea(w, h):
    """The domed front of the eyeball: dark and glossy, a wet highlight up on the left."""
    def px(x, y):
        if 1 <= x <= 2 and 1 <= y <= 1 + (x == 1):
            return 'G'
        if x == w - 2 and y == h - 2:
            return 'g'
        return 'E' if (x in (0, w - 1) or y in (0, h - 1)) else 'e'
    return rows(w, h, px)


# A golden iris (darker under the brow, glowing warmer underneath) with a frog's wide, flat pupil
# and a white glint, at texel scale.
IRIS = [
    '__rrrr__',
    '_rddddr_',
    'rdGGiidr',
    'rikkkkir',
    'rikkkkir',
    'rillllir',
    '_rllllr_',
    '__rrrr__',
]


def cypole() -> Model:
    m = Model('cypole', (128, 96), PAL, {'cypole': {}}, res=2, materials=MATS)
    skin = dict(color='skin', pattern='mc', clusters=0.3)
    back = dict(color='back', pattern='mc', clusters=0.35)

    def warty(w, h, seed, dense=0.07, glow='Ww', **kw):
        d = dict(back, hd=True, map=_warts(w, h, seed, dense), keys=WART_KEYS, glow_keys=glow)
        d.update(kw)
        return d

    # ---- the hind legs: big folded haunches hugging the flanks, long webbed feet splayed on the ground
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(5.2 * sx, 19.6, 4.0), rot=(0.12, 0.2 * sx, 0))
        x0 = -1.5 if sx > 0 else -2.5
        leg.cube((x0, -2.8, -4.0), (4, 5.0, 8.5), **mc('skin', clusters=0.3), faces={
            'up': warty(8, 17, 11 + sx),
            'east' if sx > 0 else 'west': dict(skin, hd=True, map=_warts(17, 10, 21 + sx, 0.05), keys=WART_KEYS, glow_keys='W',
                                               bands=[(4, 'belly')]),
            'north': mc('skin', clusters=0.2, bands=[(3, 'belly')]),
            'down': mc('belly_d', clusters=0.1),
        })
        leg.cube((x0 + 0.5, -3.4, -2.5), (3, 0.6, 5.5), **back)                       # the haunch's rounded top
        shin = leg.part(f'{side}_shin', pivot=(0.6 * sx, 1.6, 3.6))
        shin.cube((-1.25, -1.0, -7.0), (2.5, 2.3, 7.5), **mc('skin_d', clusters=0.2), faces={'down': mc('belly_d', clusters=0.0)})
        foot = shin.part(f'{side}_foot', pivot=(0.2 * sx, 1.3, -6.5), rot=(0, -0.45 * sx, 0))
        foot.cube((-2.5, -0.25, -5.0), (5, 0.75, 5.5), **mc('toe', clusters=0.1, rim=False), faces={
            'up': mc('toe', clusters=0.0, rim=False, hd=True, map=rows(10, 11, lambda x, y: (
                'c' if y == 0 and x in (0, 3, 6, 9) else 't' if x in (0, 3, 6, 9) and y < 8 else 'T' if x in (1, 2, 4, 5, 7, 8) and y < 2 else '.')),
                keys={'c': 'claw', 't': 'toe_l', 'T': 'toe_d'}),
        })

    # ---- the body: a squat, rounded toad body, sitting up a little at the front
    body = m.part('body', pivot=(0, 19.6, 2.0), rot=(-0.26, 0, 0))
    body.cube((-6.5, -4.0, -6.5), (13, 3.25, 12.5), **skin, faces={            # the widest band, round the middle
        'east': dict(skin, hd=True, map=_warts(25, 7, 7, 0.05), keys=WART_KEYS, glow_keys='W'),
        'west': dict(skin, hd=True, map=_warts(25, 7, 9, 0.05), keys=WART_KEYS, glow_keys='W'),
        'south': warty(26, 7, 5, 0.05),
        'up': warty(26, 25, 3),
        'north': mc('belly', clusters=0.15),
    })
    body.cube((-5.75, -0.75, -6.0), (11.5, 2.25, 11.5), **mc('belly', clusters=0.2), faces={   # the belly, pale and soft
        'east': mc('belly', clusters=0.2, bands=[(0, 'belly')]),
        'south': mc('skin', clusters=0.2, bands=[(1, 'belly')]),
    })
    body.cube((-5.75, -5.25, -6.0), (11.5, 1.25, 11.25), **back, faces={'up': warty(23, 23, 13)})   # the back, rising to a hump
    body.cube((-3.0, -5.75, -4.0), (6, 0.5, 8), **back, faces={'up': warty(12, 16, 15, 0.1)})
    body.cube((-4.5, -4.25, 6.0), (9, 4.0, 0.75), **back)                                        # the rump

    # ---- the head: wide and flat, a broad lipped mouth; the one great eye on top
    head = body.part('head', pivot=(0, -2.6, -6.25), rot=(0.26, 0, 0))
    head.cube((-5.75, -3.0, -7.0), (11.5, 3.0, 7.25), **skin, faces={
        'up': warty(23, 15, 17, 0.05),
        'north': mc('skin', clusters=0.1, hd=True, map=rows(23, 6, lambda x, y: 'l' if y == 5 or (y == 4 and x in (0, 22)) else '.'),
                    keys={'l': 'lip'}),
        'east': mc('skin', clusters=0.15, hd=True, map=rows(15, 6, lambda x, y: 'l' if y == 5 else '.'), keys={'l': 'lip'}),
        'west': mc('skin', clusters=0.15, hd=True, map=rows(15, 6, lambda x, y: 'l' if y == 5 else '.'), keys={'l': 'lip'}),
        'down': mc('mouth', clusters=0.0, rim=False, hd=True, map=rows(23, 15, lambda x, y: 'g' if 3 < x < 19 and 2 < y < 13 else '.'),
                   keys={'g': 'gum'}),
    })
    head.cube((-4.75, -2.6, -8.0), (9.5, 2.6, 1), **skin, faces={                         # the snout, rounded off
        'north': mc('skin', clusters=0.1, hd=True, map=['...................', '....n.........n....', '.' * 19, '.' * 19, 'l' * 19,
                                                          'l' * 19], keys={'n': 'lip', 'l': 'lip'}),
        'up': mc('back', clusters=0.2, hd=True, map=['...................', '.....n.......n.....'], keys={'n': 'lip'}),
    })
    head.cube((-4.5, -3.6, -6.25), (9, 0.6, 5.75), **back)                                 # the mound the eye sits in

    # the great eye: a domed ball sunk in a fleshy socket, framed by a brass ring
    eye = head.part('eye', pivot=(0, -3.5, -3.25), rot=(0.08, 0, 0))
    eye.cube((-3.25, -5.0, -3.25), (6.5, 4.75, 6.5), **mc('skin', clusters=0.2), faces={
        'north': dict(color='skin', pattern='mc', clusters=0.0, rim=False, hd=True, map=_eye_front(13, 10),
                      keys={'R': 'brass_l', 'r': 'brass_d', 'e': 'eyeball', 'E': 'eyeball_l', 's': 'skin', 'S': 'skin_l'}),
        'up': warty(13, 13, 19, 0.06),
    })
    eye.cube((-2.5, -5.75, -2.75), (5, 0.75, 5.75), **back, faces={'up': warty(10, 11, 23, 0.08)})   # the domed brow
    eye.cube((-1.5, -6.25, -1.75), (3, 0.5, 3.75), **back)
    eye.cube((3.25, -4.25, -2.5), (0.6, 3.75, 5), **skin)                                    # the socket's fleshy sides
    eye.cube((-3.85, -4.25, -2.5), (0.6, 3.75, 5), **skin)
    eye.cube((-2.5, -4.25, 3.25), (5, 3.75, 0.6), **skin)
    eye.cube((-2.25, -4.5, -3.75), (4.5, 4.0, 0.5), **mc('eyeball', clusters=0.0, rim=False), faces={   # the cornea bulging out
        'north': dict(color='eyeball', pattern='mc', clusters=0.0, rim=False, hd=True, map=_cornea(9, 8),
                      keys={'e': 'eyeball', 'E': 'eyeball_l', 'G': 'gloss', 'g': 'eyeball_l'}),
    })
    iris = eye.part('iris', pivot=(0, -2.5, -3.86))
    iris.cube((-2.0, -2.0, 0), (4, 4, 0), color='iris', pattern='mc', faces={
        'north': dict(color='iris', pattern='mc', clusters=0.0, rim=False, hd=True, map=IRIS,
                      keys={'k': 'pupil', 'G': 'tip_l', 'r': 'iris_r', 'd': 'iris_d', 'l': 'iris_l', 'i': 'iris'}, glow_keys='ildr'),
        'south': mc('eyeball', clusters=0.0, rim=False),
    })
    # eyelids: full-size flaps the model folds away (yScale) to blink, narrow and shut the eye
    upper = eye.part('upper_lid', pivot=(0, -5.05, -3.96))
    upper.cube((-3.3, 0, -0.3), (6.6, 5.0, 0.3), **mc('lid', clusters=0.2, rim=False), faces={
        'north': mc('lid', clusters=0.2, rim=False, hd=True, map=['.' * 14] * 8 + ['L' * 14, 'l' * 14], keys={'l': 'lid_d', 'L': 'skin_l'}),
    })
    lower = eye.part('lower_lid', pivot=(0, -0.2, -3.96))
    lower.cube((-3.3, -4.85, -0.3), (6.6, 4.85, 0.3), **mc('lid', clusters=0.2, rim=False), faces={
        'north': mc('lid', clusters=0.2, rim=False, hd=True, map=['l' * 14] + ['.' * 14] * 9, keys={'l': 'lid_l'}),
    })

    # brass tympana on the cheeks (they ring after every crash)
    for side, sx in (('left', 1), ('right', -1)):
        tym = head.part(f'{side}_tympanum', pivot=(5.8 * sx, -1.6, -3.0))
        tym.cube((0, -2.5, -2.5), (0, 5, 5), color='brass', pattern='mc', faces={
            'east': dict(color='brass', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(10, sx < 0, True, 1.0 + sx),
                         keys=DISC_KEYS, glow_keys='V'),
            'west': dict(color='brass', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(10, sx > 0, True, 1.0 + sx),
                         keys=DISC_KEYS, glow_keys='V'),
        })
        tym.cube((0.0 if sx > 0 else -0.6, -0.75, -0.75), (0.6, 1.5, 1.5), **mc('brass_d', clusters=0.0, rim=False))

    # the lower jaw, the vocal sac under it, and the two cymbal plates folded back either side of the sac
    jaw = head.part('jaw', pivot=(0, 0, -0.25))
    jaw.cube((-5.5, 0, -6.75), (11, 1.5, 7), **mc('belly', clusters=0.15), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, map=rows(22, 14, lambda x, y: (
            'T' if 7 < x < 14 and 2 < y < 12 else 't' if 6 < x < 15 and 1 < y < 13 else 'g' if 2 < x < 19 and 0 < y else '.')),
            keys={'T': 'tongue_l', 't': 'tongue', 'g': 'gum'}),
        'north': mc('belly', clusters=0.0, hd=True, map=['l' * 22] + ['.' * 22] * 2, keys={'l': 'lip'}),
        'east': mc('skin', clusters=0.1, bands=[(1, 'belly')]),
        'west': mc('skin', clusters=0.1, bands=[(1, 'belly')]),
    })
    jaw.cube((-4.5, 0.25, -7.75), (9, 1.0, 1), **mc('belly', clusters=0.1, rim=False))        # the round chin
    sac = jaw.part('throat', pivot=(0, 1.4, -4.0))
    sac.cube((-2.75, -0.4, -2.75), (5.5, 2.4, 5.25), **mc('sac', clusters=0.2), faces={
        'north': mc('sac', clusters=0.1, hd=True, map=rows(11, 5, lambda x, y: 'l' if (y == 1 and 2 < x < 7) else '.'), keys={'l': 'sac_l'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        plate = jaw.part(f'{side}_plate', pivot=(3.1 * sx, 1.2, -6.25), rot=(0.4, 0, -0.32 * sx))
        plate.cube((0, 0, 0), (0, 5.5, 5.5), color='brass', pattern='mc', faces={
            'east': dict(color='brass', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(11, sx < 0, True, 2.0 * sx),
                         keys=DISC_KEYS, glow_keys='V'),
            'west': dict(color='brass', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(11, sx > 0, True, 2.0 * sx),
                         keys=DISC_KEYS, glow_keys='V'),
        })
        plate.cube((0.0 if sx > 0 else -0.6, 2.0, 2.0), (0.6, 1.5, 1.5), **mc('brass_d', clusters=0.0, rim=False))     # the bell
        plate.cube((-0.5 if sx > 0 else 0.0, -0.4, -0.25), (0.5, 1.0, 1.0), **mc('skin_d', clusters=0.0, rim=False))   # its hinge

    # ---- the front legs: short, splayed, webbed hands with pale claw tips
    for side, sx in (('left', 1), ('right', -1)):
        arm = m.part(f'{side}_arm', pivot=(4.4 * sx, 19.0, -4.25), rot=(-0.15, 0, -0.22 * sx))
        arm.cube((-1.0, -0.5, -1.0), (2, 4.0, 2), **mc('skin', clusters=0.2), faces={'north': mc('belly', clusters=0.1)})
        fore = arm.part(f'{side}_forearm', pivot=(0, 3.5, 0), rot=(0.15, 0, 0.22 * sx))
        fore.cube((-0.75, 0, -0.75), (1.5, 1.5, 1.5), **mc('skin_d', clusters=0.0, rim=False))
        hand = fore.part(f'{side}_hand', pivot=(0, 1.0, 0), rot=(0, -0.3 * sx, 0))
        hand.cube((-2.0, 0.0, -3.25), (4, 0.5, 3.75), **mc('toe', clusters=0.0, rim=False), faces={
            'up': mc('toe', clusters=0.0, rim=False, hd=True, map=rows(8, 7, lambda x, y: (
                'c' if y == 0 and x in (0, 3, 4, 7) else 't' if x in (0, 3, 4, 7) and y < 5 else '.')), keys={'c': 'claw', 't': 'toe_l'}),
        })

    # ---- the tongue (hidden until it shoots; CypoleModel aims and stretches it at its target)
    tongue = m.part('tongue', pivot=MOUTH)
    tongue.cube((-0.75, -0.5, -1.0), (1.5, 1.0, 1.0), **mc('tongue', clusters=0.0, rim=False))
    tip = m.part('tongue_tip', pivot=MOUTH)
    tip.cube((-1.25, -1.0, -2.25), (2.5, 2.0, 2.5), **mc('tip', clusters=0.0, rim=False, glow=True), faces={
        'north': mc('tip_l', clusters=0.0, rim=False, glow=True)})
    return m


MODELS = {'cypole': cypole}


# =========================================================================== sounds (vanilla files and events)
SOUNDS = {
    'entity.cypole.croak': [('mob/frog/idle1', 1.0, 0.62), ('mob/frog/idle2', 1.0, 0.58), ('mob/frog/idle4', 1.0, 0.6), ('mob/frog/idle6', 1.0, 0.64),
                            ('mob/frog/idle7', 1.0, 0.56)],
    'entity.cypole.tick': [('event:block.note_block.hat', 0.9, 1.25), ('event:block.note_block.hat', 0.9, 1.4)],
    'entity.cypole.rattle': [('event:block.note_block.snare', 0.7, 1.9), ('mob/warden/tendril_clicks_2', 0.9, 1.9),
                             ('mob/warden/tendril_clicks_4', 0.9, 1.8)],
    'entity.cypole.angry': [('mob/frog/idle2', 1.0, 0.42), ('mob/frog/idle7', 1.0, 0.45)],
    'entity.cypole.calm': [('mob/frog/idle1', 0.8, 1.15), ('mob/frog/idle4', 0.8, 1.2)],
    'entity.cypole.windup': [('mob/pufferfish/blow_up1', 1.0, 0.6), ('mob/pufferfish/blow_up2', 1.0, 0.65)],
    'entity.cypole.crash': [('event:block.anvil.land', 0.8, 1.85), ('event:block.anvil.land', 0.8, 2.0)],
    'entity.cypole.shimmer': [('block/bell/resonate', 1.0, 1.6), ('block/amethyst/shimmer', 1.0, 1.4)],
    'entity.cypole.shockwave': [('entity/wind_charge/wind_burst1', 1.0, 0.8), ('entity/wind_charge/wind_burst2', 1.0, 0.75),
                                ('item/mace/smash_ground_heavy', 0.9, 1.1)],
    'entity.cypole.tongue': [('mob/frog/tongue1', 1.0, 0.7), ('mob/frog/tongue2', 1.0, 0.75)],
    'entity.cypole.slap': [('mob/slime/attack1', 1.0, 0.8), ('mob/slime/big1', 0.8, 1.2), ('mob/slime/big2', 0.8, 1.1)],
    'entity.cypole.shielded': [('event:item.shield.block', 1.0, 1.1)],
    'entity.cypole.hop': [('mob/slime/small1', 0.6, 0.75), ('mob/slime/small3', 0.6, 0.7), ('mob/slime/small4', 0.6, 0.8)],
    'entity.cypole.hurt': [('mob/frog/hurt1', 1.0, 0.7), ('mob/frog/hurt2', 1.0, 0.72), ('event:block.chain.hit', 0.6, 1.6)],
    'entity.cypole.death': [('mob/frog/death1', 1.0, 0.7), ('mob/frog/death2', 1.0, 0.68)],
}
SUBTITLES = {
    'entity.cypole.croak': 'Cypole croaks', 'entity.cypole.tick': 'Brass plates tick', 'entity.cypole.rattle': 'Cypole rattles its plates',
    'entity.cypole.angry': 'Cypole croaks angrily', 'entity.cypole.calm': 'Cypole croaks contentedly', 'entity.cypole.windup': 'Cypole swells',
    'entity.cypole.crash': 'Cymbals crash', 'entity.cypole.shimmer': 'Cymbals ring', 'entity.cypole.shockwave': 'Shockwave rolls',
    'entity.cypole.tongue': 'Cypole lashes its tongue', 'entity.cypole.slap': 'Tongue slaps', 'entity.cypole.shielded': 'Shield slaps a tongue away',
    'entity.cypole.hop': 'Cypole hops', 'entity.cypole.hurt': 'Cypole hurts', 'entity.cypole.death': 'Cypole dies',
}


def sounds(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)


# =========================================================================== data: loot, tags, the particle, text

def data(GA):
    import gen_data as D
    # brass and a sticky tongue: copper nuggets and a slime ball
    D.table('entity', 'entities/cypole', [
        D.pool([D.item('minecraft:copper_nugget', count=(1, 3), extra=[D.LOOTING])]),
        D.pool([D.item('minecraft:slime_ball', count=(0, 1), extra=[D.LOOTING])]),
    ])
    # at home in Sculk Water: it soaks there without being corrupted
    GA.tag('entity_type', f'{NS}:sculk_water_dwellers', rl('cypole'))
    # the shockwave ring
    GA.write(os.path.join(GA.A, 'particles', 'cymbal_ring.json'), {'textures': [f'{NS}:cymbal_ring']})
    GA.TEXTURES.add('particle/cymbal_ring')
    GA.LANG.update({
        f'entity.{NS}.cypole': 'Cypole',
        f'band.{NS}.instrument.cymbals': 'Cymbals',
        f'codex.{NS}.cypole.title': 'Cypole',
        f'codex.{NS}.cypole.tagline': 'Neutral - a one-eyed cymbal frog',
        f'codex.{NS}.cypole.body': ('A big swamp frog with one great golden eye and brass plates for cheeks and throat. Cypoles float in '
                                    'Sculk Water and croak in rhythm, the whole swamp on one beat. Come close and one rattles its '
                                    'plates; come closer and it fights. It rears up and crashes its plates: a ring shockwave rolls out '
                                    'across the ground - jump it! Or it shoots its tongue to yank you in: keep moving sideways, or '
                                    'raise a shield. Hit one and its neighbours join in. A drum or chimes song calms them, and they '
                                    'may join your band on cymbals.'),
    })


# =========================================================================== art: the spawn egg and the shockwave ring

def items():
    import items16 as I
    o = '#071514'
    egg = I.egg(['#122c2b', '#1f4744', '#2e6560', '#3f7d76'], o, {
        2: '......dddd......',
        3: '.....dGiiid.....',
        4: '....diikkiid....',
        5: '....dilkklid....',
        6: '.....dlllld.....',
        7: '......dddd......',
        8: '....w.mmmm.w....',
        9: '...bb......bb...',
        10: '...bBb....bBb...',
        11: '....bb.ww.bb....',
        13: '......w...w.....',
    }, pal={'y': ('#c4912c', o), 'd': ('#0b1e1d', o), 'G': ('#fff6d0', o), 'i': ('#f2bd36', o), 'l': ('#ffe58a', o), 'k': ('#06080a', o), 'm': ('#0b1e1d', o), 'b': ('#d9a53c', o),
            'B': ('#fff2c2', o), 'w': (I.GLOW[2], o)}, no_ol='wBG')
    return {'cypole_spawn_egg': egg}


def textures(out):
    """The shockwave ring (64 x 64, lying flat): a bright brass crest at the very edge (the quad's
    half-size is the ring's radius), a hot teal line just inside it and a soft fading wake behind."""
    import numpy as np
    from PIL import Image
    n = 64
    c = (n - 1) / 2
    img = np.zeros((n, n, 4), dtype=np.float64)
    yy, xx = np.mgrid[0:n, 0:n]
    d = np.hypot(xx - c, yy - c) / (n / 2)
    ang = np.arctan2(yy - c, xx - c)
    crest = np.clip(1.0 - np.abs(d - 0.955) / 0.035, 0, 1)
    teal = np.clip(1.0 - np.abs(d - 0.9) / 0.03, 0, 1)
    wake = np.where((d < 0.9) & (d > 0.62), ((d - 0.62) / 0.28) ** 2, 0.0)
    ripple = 0.75 + 0.25 * np.cos(ang * 22.0 + d * 30.0)
    brass = np.array([255, 214, 120], dtype=np.float64)
    hot = np.array([150, 255, 240], dtype=np.float64)
    dust = np.array([214, 196, 150], dtype=np.float64)
    col = (brass * crest[..., None] + hot * teal[..., None] * 0.9 + dust * wake[..., None] * 0.5)
    alpha = np.clip(crest * 255 + teal * 200 + wake * 90 * ripple, 0, 255)
    weight = np.clip(crest + teal * 0.9 + wake * 0.5, 1e-6, None)
    img[..., :3] = np.clip(col / weight[..., None], 0, 255)
    img[..., 3] = np.where(d <= 1.0, alpha, 0)
    out('particle/cymbal_ring', Image.fromarray(img.astype(np.uint8), 'RGBA'))
