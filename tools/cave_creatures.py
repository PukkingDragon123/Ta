"""A4 cave creatures: the Jailer and the Sculklings.

* The Jailer (`thesift:jailer`): a tall, skinny, blind kin of the Warden - dark sculk skin, an
  exposed ribcage over a glowing soul, long thin limbs and a faceless head crowned with antler-like
  tendrils - hunched over the giant cell of sculk-iron bars it carries. It hunts by sound and slams
  the cell down over whoever it hears, then hauls them around inside it.
* Sculklings (`thesift:sculkling`): small sculk goblins with giant bat ears (and no wings) that
  skitter through dark caves in packs, giggle, screech, swarm, snatch shiny things and run - and
  cover their ears and flee from music.
* CR4: the Jailer's cell is studded with Sculkite (`thesift:sculkite`, item tag #thesift:sculkite), the
  dark sculk crystal it grows its bars from - its only drop, the raw material of echo gear and Stomper
  armour (their recipes and the sculkite ore come later). The Cypole of the Sculk Swamp lives in
  tools/cypole.py and is hooked in through this module's hooks.

Hooks (one line each): spec.py -> declare(block, item); mobs.py -> ALL.update(MODELS);
gen_assets.generate() -> assets(GA) (sounds, lang, Codex text, loot, tags, spawns);
items16.all_items() -> items() (spawn eggs). Java: registry/ModCaveCreatures, entity/cave/*,
client/CaveCreaturesClient (JailerModel / SculklingModel animate the geometry made here).
"""
from __future__ import annotations

import os

from modelkit import Model

NS = 'thesift'


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def rows(w, h, fn):
    """A w x h texel map from fn(x, y) -> char."""
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


# =========================================================================== spec (items)

def declare(block, item):
    item("jailer_spawn_egg", cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModCaveCreatures.JAILER.get())", tab="eggs")
    item("sculkling_spawn_egg", cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModCaveCreatures.SCULKLING.get())", tab="eggs")
    # CR4: the Jailer's drop, a raw material (echo gear, Stomper armour)
    item("sculkite", props="new Item.Properties().rarity(Rarity.UNCOMMON)")
    __import__('cypole').declare(block, item)  # CR4: the Cypole's spawn egg


# =========================================================================== THE JAILER
# Where the carried cell hangs, in model units: centred 18.4 units (1.15 blocks) in front of the
# Jailer, its top 36.8 units (2.3 blocks) above the ground. JailCell.java and Jailer.java use the
# same numbers (CAGE_FORWARD, CAGE_LIFT) to seat the prisoner inside it.
CAGE_FWD = 18.4
CAGE_TOP = -12.8
CAGE_W = 20       # outer width and depth
CAGE_H = 32       # roof to the bottom of the floor ring
BARS = 12         # loose bars, three per side: the ones that snap when the cell is broken

JPAL = {
    'skin': '#0c2c35', 'skin_l': '#15424d', 'skin_d': '#061a21',
    'sinew': '#103a44', 'sinew_l': '#1a5260', 'sinew_d': '#08232b',
    'bone': '#cfc6ad', 'bone_l': '#ebe4cf', 'bone_d': '#958c76',
    'soul': '#5ff8ff', 'soul_l': '#d2fffc', 'soul_d': '#16a6b0', 'cavity': '#03090c',
    'mouth': '#04141a', 'gullet': '#29dfeb', 'tooth': '#e6dfc8',
    'claw': '#1b2228', 'claw_l': '#33404a', 'claw_d': '#0d1216',
    'tendril': '#0f4752', 'tendril_l': '#18636f', 'tendril_d': '#08303a', 'tip': '#3ff5e6', 'tip_l': '#c8fffb',
    'iron': '#39474f', 'iron_l': '#566872', 'iron_d': '#202a31', 'patina': '#1f5b60', 'rivet': '#7d8f97',
    'crystal': '#122640', 'crystal_l': '#1f4166', 'crystal_d': '#0a1424', 'crystal_g': '#4ff0e8',
    'vein': '#2fe6f0', 'vein_d': '#12858f', 'node': '#5ff8ff',
}


def _ribcage_front(w, h):
    """The exposed ribcage, front: a sternum, four ribs curving down and out, dark gaps between
    them and a soul burning deep inside (S bright, s dim, both glowing)."""
    cx = (w - 1) / 2

    def px(x, y):
        dx = abs(x - cx)
        if y <= 1:
            return 'B' if y == 0 else 'b'
        if dx < 1.6:
            return 'B' if dx < 0.8 else 'b'          # the sternum
        for i in range(4):
            ry = 3 + i * 4.6 + dx * 0.42             # each rib droops outward
            if abs(y - ry) < 1.0:
                return 'B' if y < ry else 'b'
        if y > h - 3:
            return '.'                               # the skin of the belly below the cage
        # the gaps: the soul glows brightest at the heart, fading to black at the flanks
        heart = ((x - cx) / 4.6) ** 2 + ((y - h * 0.42) / 5.5) ** 2
        if heart < 0.45:
            return 'S'
        if heart < 1.2:
            return 's'
        return 'c'
    return rows(w, h, px)


def _ribcage_side(w, h):
    def px(x, y):
        for i in range(4):
            ry = 3 + i * 4.6 + (w - 1 - x) * 0.25
            if abs(y - ry) < 1.0:
                return 'B' if y < ry else 'b'
        if y <= 1:
            return 'b'
        if y > h - 3:
            return '.'
        return 's' if x < 3 and 6 < y < h - 6 else 'c'
    return rows(w, h, px)


def _spine_back(w, h):
    """The back of the chest: shoulder blades and a ridge of vertebra knobs."""
    cx = (w - 1) / 2

    def px(x, y):
        if abs(x - cx) < 1.2 and y % 3 != 2:
            return 'B' if y % 3 == 0 else 'b'
        if 2 <= y <= 8 and 2 <= abs(x - cx) <= 6 and (abs(x - cx) - 2) < (8 - y) * 0.9:
            return 'b' if abs(x - cx) > 2.8 else '.'
        return '.'
    return rows(w, h, px)


def _iron_bar(w, h, seed):
    """A sculk-iron bar, one face at texel scale: lit on its left edge, dark on its right like a
    round bar, flecked with teal patina, and every so often a glowing knot of sculk."""
    def px(x, y):
        node = (y + seed * 7) % 19
        if node in (0, 1) and (y + seed) % 38 < 19:
            return 'V' if node == 0 else 'v'
        if x == 0:
            return 'l'
        if x == w - 1:
            return 'd'
        if (x * 5 + y * 3 + seed) % 17 == 0:
            return 'p'
        return '.'
    return rows(w, h, px)


def _roof(w):
    """The cell's roof at texel scale: riveted plates with sculk creeping across them in glowing veins."""
    c = (w - 1) / 2

    def px(x, y):
        if x in (0, w - 1) or y in (0, w - 1):
            return 'r' if (x + y) % 8 == 0 else 'd'
        if x in (1, w - 2) or y in (1, w - 2):
            return 'l'
        if abs(x - c) < 1 or abs(y - c) < 1:
            return 'r' if (x + y) % 6 == 0 else 'd'
        dx, dy = x - c, y - c
        # a vein network creeping out from the lock in the middle
        for ax, ay in ((1.0, 0.45), (-0.6, 1.0), (-1.0, -0.7), (0.5, -1.0)):
            t = dx * ax + dy * ay
            if t > 3:
                off = abs(-dx * ay + dy * ax) - 0.6 * abs((t % 9) - 4.5) / 4.5
                if off < 0.7:
                    return 'V' if t < 9 else 'v'
        return 'p' if (x * 13 + y * 7) % 29 == 0 else '.'
    return rows(w, w, px)


XK = {'g': 'crystal_g', 'c': 'crystal_l'}


def jailer() -> Model:
    m = Model('jailer', (128, 128), JPAL, {'jailer': {}}, res=2)
    rib = {'B': 'bone_l', 'b': 'bone', 'S': 'soul_l', 's': 'soul', 'c': 'cavity'}
    bone_k = {'B': 'bone_l', 'b': 'bone_d'}
    skin = dict(color='skin', pattern='mc', clusters=0.35)

    # ---- long, thin, digitigrade legs: a sinewy thigh, a bony shin, three dark claws for a foot
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(2.6 * sx, -1.0, 2.0), rot=(-0.22, 0, 0))
        leg.cube((-1.5, -1, -1.5), (3, 14, 3), **mc('sinew', clusters=0.3), faces={
            'north': mc('sinew', clusters=0.2, map=['...'] * 5 + ['.B.', 'bbb', '.b.'], keys=bone_k),
        })
        shin = leg.part(f'{side}_shin', pivot=(0, 12.5, 0), rot=(0.42, 0, 0))
        shin.cube((-1, 0, -1), (2, 12, 2), **mc('skin', clusters=0.25), faces={
            'north': mc('skin', clusters=0.1, map=['BB', 'bb'] + ['.b'] * 6 + ['..'] * 4, keys=bone_k),
        })
        foot = shin.part(f'{side}_foot', pivot=(0, 12, 0), rot=(-0.2, 0, 0))
        foot.cube((-1.5, -0.5, -1.5), (3, 1.5, 3), **mc('skin_d', clusters=0.0, rim=False))
        foot.cube((-1.5, 0, -4), (0.75, 1, 2.5), **mc('claw', clusters=0.0, rim=False))
        foot.cube((-0.375, 0, -4.5), (0.75, 1, 3), **mc('claw', clusters=0.0, rim=False))
        foot.cube((0.75, 0, -4), (0.75, 1, 2.5), **mc('claw', clusters=0.0, rim=False))

    # ---- hips and a waist so thin the spine shows
    body = m.part('body', pivot=(0, -2.5, 2.0))
    body.cube((-4, -2.5, -2), (8, 3.5, 4), **skin, faces={
        'north': mc('skin', clusters=0.2, map=['.bbbbbb.', '........', '........'], keys=bone_k),
    })
    body.cube((-1.5, -9, -1.0), (3, 7, 2.5), **mc('sinew', clusters=0.2), faces={
        'south': mc('sinew', clusters=0.0, map=['.B.', '.b.', '...'] * 3, keys=bone_k),
        'north': mc('sinew', clusters=0.1, map=['...', 'bbb', '...', '...', 'bbb', '...', '...'], keys={'b': 'sinew_l'}),
    })

    # ---- the ribcage, hunched forward, a soul glowing between the ribs
    chest = body.part('chest', pivot=(0, -8.5, 0), rot=(0.38, 0, 0))
    chest.cube((-5, -10, -3.5), (10, 10, 7), **skin, faces={
        'north': dict(color='cavity', pattern='mc', clusters=0.0, rim=False, hd=True, map=_ribcage_front(20, 20), keys=rib, glow_keys='Ss'),
        'east': dict(color='skin', pattern='mc', clusters=0.15, hd=True, map=_ribcage_side(14, 20), keys=rib, glow_keys='s'),
        'west': dict(color='skin', pattern='mc', clusters=0.15, hd=True, map=[r[::-1] for r in _ribcage_side(14, 20)], keys=rib,
                     glow_keys='s'),
        'south': dict(color='skin', pattern='mc', clusters=0.3, hd=True, map=_spine_back(20, 20), keys=rib),
        'down': mc('skin_d', clusters=0.1),
    })
    # the bony yoke of the shoulders, and a knob of bone on each shoulder
    chest.cube((-6.5, -11.25, -2.25), (13, 1.5, 4.5), **mc('bone_d', clusters=0.4), faces={'down': mc('skin_d', clusters=0.0)})
    for sx in (1, -1):
        chest.cube((5.5 * sx - 1.5, -12, -2), (3, 2.5, 4), **mc('bone', clusters=0.3, rim=False), faces={'down': mc('skin_d', clusters=0.0)})

    # ---- a thin neck and the faceless head: a long skull, a bone mask without eyes, a jaw
    neck = chest.part('neck', pivot=(0, -10.5, -0.5), rot=(0.6, 0, 0))
    neck.cube((-1.25, -5, -1.25), (2.5, 5.5, 2.5), **mc('sinew', clusters=0.2), faces={
        'south': mc('sinew', clusters=0.0, map=['.B', '..', '.B', '..', '.B'], keys=bone_k),
    })
    head = neck.part('head', pivot=(0, -4.5, 0), rot=(-0.8, 0, 0))
    mask = rows(12, 12, lambda x, y: (
        'B' if (y in (2, 3) and 1 <= x <= 10 and not (y == 3 and x in (1, 10))) else         # the brow ridge
        'b' if (x in (5, 6) and 4 <= y <= 9) else                                           # a ridge down the face
        'o' if (y in (6, 7) and x in (2, 9)) or (y == 9 and x in (3, 8)) else                # blind sensory pits
        'g' if (y in (5, 8) and x in (2, 9)) or (y == 10 and x in (3, 8)) else
        'C' if (y == 0 and x in (3, 4, 8)) or (y == 1 and x in (4, 7)) else '.'))
    head.cube((-3.5, -7, -4), (7, 7, 7), **mc('skin', clusters=0.3), faces={
        'north': dict(color='skin', pattern='mc', clusters=0.1, hd=True, map=[''] and rows(14, 14, lambda x, y: (
            mask[y - 1][x - 1] if 1 <= x <= 12 and 1 <= y <= 12 else '.')),
            keys={'B': 'bone', 'b': 'bone_d', 'o': 'cavity', 'g': 'soul_d', 'C': 'vein_d'}, glow_keys='gC'),
        'up': mc('skin', clusters=0.25, map=['.......', '..b.b..', '.......', '...b...', '.......', '..b.b..', '.......'], keys={'b': 'bone_d'}),
        'east': mc('skin', clusters=0.25, map=['.......', '.......', 'bbb....', '.......', '...g...', '.......', '.......'],
                   keys={'b': 'bone_d', 'g': 'soul_d'}, glow_keys='g'),
        'west': mc('skin', clusters=0.25, map=['.......', '.......', '....bbb', '.......', '...g...', '.......', '.......'],
                   keys={'b': 'bone_d', 'g': 'soul_d'}, glow_keys='g'),
    })
    # the muzzle juts forward under the mask; the jaw hangs from it
    head.cube((-2.5, -2.5, -5.5), (5, 2.5, 2), **mc('skin', clusters=0.1, rim=False), faces={
        'north': mc('skin', clusters=0.0, rim=False, hd=True, map=['..........', '..........', '.b......b.', '..bbbbbb..', '..........'],
                    keys={'b': 'bone_d'}),
    })
    jaw = head.part('jaw', pivot=(0, 0, 0.5))
    jaw.cube((-2.75, 0, -5.75), (5.5, 2, 6), **mc('skin', clusters=0.2), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True,
                 map=rows(11, 12, lambda x, y: 't' if y == 0 and x % 2 == 0 else 'G' if (y > 6 and 3 <= x <= 7) else '.'),
                 keys={'t': 'tooth', 'G': 'gullet'}, glow_keys='G'),
        'north': mc('skin', clusters=0.0, hd=True, map=rows(11, 4, lambda x, y: 't' if y == 0 and x % 2 == 1 else '.'), keys={'t': 'tooth'}),
    })
    # antler-like tendrils: a long beam with two tines each, every tip a glowing sensor
    for side, sx in (('left', 1), ('right', -1)):
        tend = head.part(f'{side}_tendril', pivot=(2.4 * sx, -6.5, 0.5), rot=(-0.35, 0, 0.5 * sx))
        tend.cube((-0.6, -11, -0.6), (1.2, 11.5, 1.2), **mc('tendril', clusters=0.2, rim=False), faces={
            'north': mc('tendril', clusters=0.0, rim=False, map=['.', '.', '.', 'g', '.', '.', '.', 'g', '.', '.', '.'], keys={'g': 'tip'},
                        glow_keys='g'),
        })
        low = tend.part(f'{side}_tine_low', pivot=(0, -4.5, 0), rot=(0.25, 0, 0.9 * sx))
        low.cube((-0.5, -5.5, -0.5), (1, 5.5, 1), **mc('tendril', clusters=0.0, rim=False), faces={
            'north': mc('tendril_l', clusters=0.0, rim=False, map=['g', 'g', '.', '.', '.'], keys={'g': 'tip'}, glow_keys='g'),
            'up': mc('tip', clusters=0.0, rim=False, glow=True),
        })
        high = tend.part(f'{side}_tine_high', pivot=(0, -8.5, 0), rot=(0.6, 0, -0.55 * sx))
        high.cube((-0.5, -4.5, -0.5), (1, 4.5, 1), **mc('tendril', clusters=0.0, rim=False), faces={
            'north': mc('tendril_l', clusters=0.0, rim=False, map=['g', '.', '.', '.'], keys={'g': 'tip'}, glow_keys='g'),
            'up': mc('tip', clusters=0.0, rim=False, glow=True),
        })
        tip = tend.part(f'{side}_tendril_tip', pivot=(0, -11, 0), rot=(0.1, 0, 0.35 * sx))
        tip.cube((-0.5, -3.5, -0.5), (1, 3.5, 1), **mc('tip', clusters=0.0, rim=False, glow=True), faces={
            'up': mc('tip_l', clusters=0.0, rim=False, glow=True),
        })

    # ---- arms longer than the legs, elbows flared, long claws hooked round the cell's side bars
    for side, sx in (('left', 1), ('right', -1)):
        arm = chest.part(f'{side}_arm', pivot=(6.5 * sx, -10, -0.5), rot=(-0.3, 0, -0.6 * sx))
        arm.cube((-1, -1, -1), (2, 15, 2), **mc('sinew', clusters=0.25), faces={
            'east' if sx > 0 else 'west': mc('sinew', clusters=0.2, map=['bb', '..'] + ['..'] * 11 + ['bb', 'BB'], keys=bone_k),
        })
        fore = arm.part(f'{side}_forearm', pivot=(0, 14, 0), rot=(-1.2, 0, 0.8 * sx))
        fore.cube((-0.8, 0, -0.8), (1.6, 13, 1.6), **mc('skin', clusters=0.2, rim=False), faces={
            'south': mc('skin', clusters=0.0, rim=False, map=['b', 'b', '.', '.', '.', '.', '.', '.', '.', '.', '.', 'b', 'b'], keys=bone_k),
        })
        hand = fore.part(f'{side}_hand', pivot=(0, 13, 0), rot=(0.3, 0, 0))
        hand.cube((-1.25, 0, -1.25), (2.5, 2.5, 2.5), **mc('skin_d', clusters=0.0, rim=False))
        claw = hand.part(f'{side}_claw', pivot=(0, 2.5, 0), rot=(0.6, 0, -0.5 * sx))
        for fz in (-1.0, 0.0, 1.0):
            claw.cube((-0.3, 0, fz - 0.3), (0.6, 6, 0.6), **mc('claw', clusters=0.0, rim=False), faces={
                'down': mc('bone_l', clusters=0.0, rim=False),
            })

    # ---- the cell of sculk-iron bars (no floor while it is empty: it is slammed down over you)
    ik = {'r': 'rivet', 'd': 'iron_d', 'p': 'patina', 'v': 'vein_d', 'V': 'vein', 'l': 'iron_l'}
    half = CAGE_W / 2
    cage = m.part('cage', pivot=(0, CAGE_TOP, -CAGE_FWD))
    cage.cube((-half, 0, -half), (CAGE_W, 1.5, CAGE_W), **mc('iron', clusters=0.5, accent='patina', spots=0.1), faces={
        'up': dict(color='iron', pattern='mc', clusters=0.3, hd=True, map=_roof(CAGE_W * 2), keys=ik, glow_keys='vV'),
        'down': dict(color='iron_d', pattern='mc', clusters=0.2, hd=True, map=_roof(CAGE_W * 2), keys=ik, glow_keys='V'),
    })
    lock = cage.part('lock', pivot=(0, 0, 0))
    lock.cube((-2.5, -2, -2.5), (5, 2, 5), **mc('iron_d', clusters=0.2), faces={
        'north': mc('iron_d', clusters=0.0, map=['.....', '..V..'], keys=ik, glow_keys='V'),
        'up': mc('iron', clusters=0.0, map=['.....', '.ddd.', '.dVd.', '.ddd.', '.....'], keys=ik, glow_keys='V'),
    })
    lock.cube((-1, -4.5, -0.5), (2, 2.5, 1), **mc('iron_l', clusters=0.0, rim=False))   # the ring it is lifted by
    # CR4: Sculkite, the dark crystal it grows its bars from, bursting out round the lock and at the corners
    crystal = dict(color='crystal', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='crystal', pattern='mc', clusters=0.0, rim=False, map=['g', 'c'] + ['.'] * 6, keys=XK, glow_keys='g'),
        'east': dict(color='crystal_l', pattern='mc', clusters=0.0, rim=False, map=['g'] + ['.'] * 7, keys=XK, glow_keys='g'),
        'up': mc('crystal_g', clusters=0.0, rim=False, glow=True)})
    for i, (cx, cz, ry, lean) in enumerate(((-2.2, -1.6, 0.4, -0.35), (2.0, 1.8, -0.9, 0.4), (1.6, -2.1, 2.1, 0.3), (-1.9, 2.0, -2.5, -0.3))):
        shard = lock.part(f'sculkite_{i}', pivot=(cx, -1.6, cz), rot=(lean, ry, -lean * 0.6))
        h = 3.0 if i < 2 else 2.2
        shard.cube((-0.6, -h, -0.6), (1.2, h, 1.2), **crystal)
        shard.cube((-0.35, -h - 0.9, -0.35), (0.7, 0.9, 0.7), **mc('crystal_g', clusters=0.0, rim=False, glow=True))
    bottom = CAGE_H - 2
    ring = mc('iron', clusters=0.5, accent='patina', spots=0.2)
    cage.cube((-half, bottom, -half), (CAGE_W, 2, 1.5), **ring)
    cage.cube((-half, bottom, half - 1.5), (CAGE_W, 2, 1.5), **ring)
    cage.cube((half - 1.5, bottom, -half + 1.5), (1.5, 2, CAGE_W - 3), **ring)
    cage.cube((-half, bottom, -half + 1.5), (1.5, 2, CAGE_W - 3), **ring)
    post_h = bottom - 1.5
    for i, (cx, cz) in enumerate(((-half, -half), (half - 2, -half), (-half, half - 2), (half - 2, half - 2))):
        cage.cube((cx, 1.5, cz), (2, post_h, 2), **mc('iron', clusters=0.3, accent='patina', spots=0.1), faces={
            f: mc('iron', clusters=0.2, hd=True, map=_iron_bar(4, int(post_h * 2), i * 3 + j), keys=ik, glow_keys='vV')
            for j, f in enumerate(('north', 'south', 'east', 'west'))
        })
        # a little sculkite growing up out of each corner of the roof (CR4)
        sx, sz = (1 if cx > 0 else -1), (1 if cz > 0 else -1)
        tuft = cage.part(f'corner_sculkite_{i}', pivot=(cx + 1.0, 0.0, cz + 1.0), rot=(0.35 * sz, 0.5 * i, -0.35 * sx))
        tuft.cube((-0.5, -2.2, -0.5), (1, 2.2, 1), **crystal)
        # a knot of sculk growing over each corner, its top glowing
        cage.cube((cx - 0.25, 0.75, cz - 0.25), (2.5, 1.5, 2.5), **mc('vein_d', clusters=0.3, rim=False), faces={
            'up': mc('vein', clusters=0.0, rim=False, glow=True)})
    k = 0
    # bars 0-2 front, 3-5 back, 6-8 left side, 9-11 right side
    for axis, fixed in (('x', -half + 0.75), ('x', half - 0.75), ('z', half - 0.75), ('z', -half + 0.75)):
        for off in (-5, 0, 5):
            px, pz = (off, fixed) if axis == 'x' else (fixed, off)
            p = cage.part(f'bar_{k}', pivot=(px, 1.5, pz))
            p.cube((-0.5, 0, -0.5), (1, post_h, 1), **mc('iron', clusters=0.2, rim=False), faces={
                f: mc('iron', clusters=0.1, rim=False, hd=True, map=_iron_bar(2, int(post_h * 2), k * 5 + j), keys=ik, glow_keys='vV')
                for j, f in enumerate(('north', 'south', 'east', 'west'))})
            k += 1
    floor = cage.part('floor', pivot=(0, bottom + 0.5, 0))
    for fz in (-5, 0, 5):
        floor.cube((-half + 1.5, 0, fz - 0.5), (CAGE_W - 3, 1, 1), **mc('iron_d', clusters=0.2, rim=False))
    floor.cube((-0.5, -0.25, -half + 1.5), (1, 1, CAGE_W - 3), **mc('iron_d', clusters=0.2, rim=False))
    return m


# =========================================================================== SCULKLING
SPAL = {
    'skin': '#123b45', 'skin_l': '#1d5661', 'skin_d': '#09252d',
    'belly': '#1a4b53', 'belly_l': '#25616a', 'belly_d': '#103a42',
    'ear': '#0e3038', 'ear_l': '#174652', 'ear_d': '#071c22', 'vein': '#3ff5e6', 'vein_l': '#c8fffb',
    'claw': '#d8d2bf', 'claw_l': '#f0ead8', 'claw_d': '#a39c88', 'tooth': '#efe8d2', 'mouth': '#04131a', 'gum': '#1f6670',
    'nose': '#215d66', 'nose_l': '#2e7a84', 'nose_d': '#123f46', 'pit': '#020608', 'soul': '#5ff8ff', 'soul_d': '#169aa6',
}


def _ear_main(w, h, mirror):
    """A huge bat ear (the lower, broad part): a straight inner edge, a rounded outer edge, a paler
    hollow in the middle and three thin glowing veins fanning out from the root ('_' is cut away).
    Drawn for the left ear; the right one is mirrored."""
    def px(x, y):
        t = y / (h - 1)                                   # 0 at the top of this segment, 1 at the root
        left = 1.2 * (1.0 - t)
        right = w - 1 - (1.0 - t) ** 1.5 * w * 0.33
        if x < left or x > right:
            return '_'
        if x > right - 1.0 or x < left + 0.7:
            return 'r'
        for ang in (0.12, 0.5, 0.95):                     # veins: from the root, up and outward
            vx = w * 0.22 + (h + 1 - y) * ang
            if abs(x - vx) < 0.5 and y > 1:
                return 'V' if y > h * 0.35 else 'v'
        hollow = ((x - w * 0.5) / (w * 0.3)) ** 2 + ((y - h * 0.58) / (h * 0.42)) ** 2
        return 'l' if hollow < 1.0 else '.'
    out = rows(w, h, px)
    return [r[::-1] for r in out] if mirror else out


def _ear_tip(w, h, mirror):
    """The ear's pointed tip, which flops and twitches on its own."""
    def px(x, y):
        t = y / (h - 1)                                   # 0 at the point, 1 where it joins the ear
        left = w * 0.3 * (1.0 - t)
        right = w * 0.42 + (w * 0.58 - 1) * t ** 0.8
        if x < left or x > right:
            return '_'
        if x > right - 1.0 or x < left + 0.7:
            return 'r'
        vx = w * 0.4 + (h - y) * 0.2
        if abs(x - vx) < 0.5 and y > 2:
            return 'v'
        return 'l' if 0.35 < t and left + 2 < x < right - 2 else '.'
    out = rows(w, h, px)
    return [r[::-1] for r in out] if mirror else out


def sculkling() -> Model:
    m = Model('sculkling', (64, 64), SPAL, {'sculkling': {}}, res=2)
    ek = {'V': 'vein', 'v': 'soul_d', 'r': 'ear_d', 'l': 'ear_l'}
    # ---- short, bent goblin legs with clawed feet
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(1.6 * sx, 18.5, 0.8), rot=(-0.35, 0, 0))
        leg.cube((-1, 0, -1), (2, 3.5, 2), **mc('skin', clusters=0.2, rim=False))
        shin = leg.part(f'{side}_shin', pivot=(0, 3.5, 0), rot=(0.55, 0, 0))
        shin.cube((-0.75, 0, -0.75), (1.5, 2.5, 1.5), **mc('skin_d', clusters=0.0, rim=False))
        foot = shin.part(f'{side}_foot', pivot=(0, 2.5, 0), rot=(-0.2, 0, 0))
        foot.cube((-1, -0.25, -2.25), (2, 0.75, 3), **mc('skin_d', clusters=0.0, rim=False), faces={
            'north': mc('claw', clusters=0.0, rim=False, hd=True, map=['c.c.'], keys={'c': 'claw'}),
            'up': mc('skin_d', clusters=0.0, rim=False, hd=True, map=['c.c.'] + ['....'] * 5, keys={'c': 'claw'}),
        })

    # ---- a small hunched pot-bellied body with a soul flickering in its chest
    body = m.part('body', pivot=(0, 18.5, 0.8), rot=(0.4, 0, 0))
    def flame(x, y):
        # a little soul flame flickering in its chest, as in a Warden's
        dx = abs(x - 4.5)
        bulb = ((dx / 2.0) ** 2 + ((y - 6.5) / 2.2) ** 2) < 1.0
        point = 2 <= y <= 6 and dx < (y - 1.5) * 0.45
        if bulb or point:
            return 'S' if (dx < 1.0 and 4 <= y <= 7) else 's'
        return 'b' if y == 10 and 1 <= x <= 8 else '.'
    belly = rows(10, 12, flame)
    body.cube((-2.5, -6, -2), (5, 6, 4), **mc('skin', clusters=0.25, bands=[(4, 'belly')]), faces={
        'north': dict(color='belly', pattern='mc', clusters=0.1, hd=True, map=belly, keys={'s': 'soul_d', 'S': 'soul', 'b': 'skin_d'}, glow_keys='sS'),
        'south': mc('skin', clusters=0.2, map=['.....', '..g..', '.....', '..g..', '.....', '.....'], keys={'g': 'vein'}, glow_keys='g'),
    })
    for nz in (-1.0, 0.75):
        body.cube((-0.5, -6.5 + nz * 0.5, 1.5 + nz), (1, 1, 1), **mc('vein', clusters=0.0, rim=False, glow=True))  # sculk nubs on the spine

    # ---- the big goblin head: no eyes, sensory pits, a bat's leaf nose and a wide toothy grin
    head = body.part('head', pivot=(0, -6, -0.25), rot=(-0.4, 0, 0))
    grin = rows(14, 11, lambda x, y: (
        'p' if (y == 2 and x in (2, 4, 9, 11)) or (y == 3 and x in (3, 10)) else
        'g' if (y == 1 and x in (2, 3, 10, 11)) else
        'M' if (y == 8 and 1 <= x <= 12) or (y == 7 and x in (1, 12)) or (y == 6 and x in (0, 13)) else
        't' if (y == 9 and 2 <= x <= 11 and x % 2 == 0) else
        'm' if (y == 9 and 2 <= x <= 11) or (y == 10 and 3 <= x <= 10) else '.'))
    head.cube((-3.5, -5.5, -3.5), (7, 5.5, 6), **mc('skin', clusters=0.3), faces={
        'north': dict(color='skin', pattern='mc', clusters=0.1, hd=True, map=grin,
                      keys={'p': 'pit', 'g': 'soul_d', 'M': 'skin_d', 't': 'tooth', 'm': 'mouth'}, glow_keys='g'),
        'up': mc('skin', clusters=0.25, map=['.......', '...g...', '.......', '..g.g..', '.......', '.......'], keys={'g': 'vein'}, glow_keys='g'),
    })
    nose = head.part('nose', pivot=(0, -3.0, -3.5))
    nose.cube((-1.25, -1, -1), (2.5, 1.75, 1.25), **mc('nose', clusters=0.0, rim=False), faces={
        'north': mc('nose', clusters=0.0, rim=False, hd=True, map=['..ll.', '.l..l', '.p.p.', '.....'], keys={'l': 'nose_l', 'p': 'pit'}),
    })
    nose.cube((-0.5, -2.5, -0.75), (1, 1.5, 0.5), **mc('nose_l', clusters=0.0, rim=False))  # the nose leaf
    jaw = head.part('jaw', pivot=(0, -0.5, -0.5))
    jaw.cube((-3, 0, -3), (6, 1.5, 4), **mc('skin_d', clusters=0.1, rim=False), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, map=rows(12, 8, lambda x, y: 't' if y == 0 and x % 2 == 1 else
                                                                      'G' if 4 <= y <= 6 and 3 <= x <= 8 else '.'),
                 keys={'t': 'tooth', 'G': 'gum'}),
        'north': mc('skin_d', clusters=0.0, rim=False, hd=True, map=rows(12, 3, lambda x, y: 't' if y == 0 and x % 2 == 1 else '.'),
                    keys={'t': 'tooth'}),
    })
    # ---- giant ears: two segments each so they can twitch, flop and fold over
    for side, sx in (('left', 1), ('right', -1)):
        mir = sx < 0
        ear = head.part(f'{side}_ear', pivot=(3.2 * sx, -4.5, 0.5), rot=(0.1, -0.35 * sx, 0.45 * sx))
        main = dict(color='ear', pattern='mc', clusters=0.0, rim=False, hd=True, map=_ear_main(16, 18, mir), keys=ek, glow_keys='Vv')
        back = dict(main, map=_ear_main(16, 18, not mir))
        ear.cube((-1 if sx > 0 else -7, -9, 0), (8, 9, 0), color='ear', pattern='mc', faces={'north': main, 'south': back})
        ear.cube((-1.25 if sx < 0 else -0.25, -2, -0.5), (1.5, 2.5, 1), **mc('skin_d', clusters=0.0, rim=False))   # the ear's root
        tip = ear.part(f'{side}_ear_tip', pivot=(1.6 * sx, -9, 0), rot=(0, 0, 0.12 * sx))
        tmain = dict(color='ear', pattern='mc', clusters=0.0, rim=False, hd=True, map=_ear_tip(12, 14, mir), keys=ek, glow_keys='Vv')
        tback = dict(tmain, map=_ear_tip(12, 14, not mir))
        tip.cube((-2.6 if sx > 0 else -3.4, -7, 0), (6, 7, 0), color='ear', pattern='mc', faces={'north': tmain, 'south': tback})
    # ---- skinny arms with big pale claws for snatching
    for side, sx in (('left', 1), ('right', -1)):
        arm = body.part(f'{side}_arm', pivot=(2.6 * sx, -5.2, -0.5), rot=(-0.35, 0, -0.3 * sx))
        arm.cube((-0.6, -0.5, -0.6), (1.2, 4.5, 1.2), **mc('skin', clusters=0.1, rim=False))
        fore = arm.part(f'{side}_forearm', pivot=(0, 4, 0), rot=(-0.5, 0, 0))
        fore.cube((-0.5, 0, -0.5), (1, 3.5, 1), **mc('skin_d', clusters=0.0, rim=False))
        hand = fore.part(f'{side}_hand', pivot=(0, 3.5, 0))
        hand.cube((-0.9, 0, -0.9), (1.8, 1, 1.8), **mc('skin_d', clusters=0.0, rim=False))
        for fx in (-0.6, 0.6):
            hand.cube((fx - 0.25, 1, -0.75), (0.5, 2, 0.5), **mc('claw', clusters=0.0, rim=False))
        hand.cube((-0.25, 1, 0.4), (0.5, 1.5, 0.5), **mc('claw_d', clusters=0.0, rim=False))
    # ---- a thin whip of a tail
    tail = body.part('tail', pivot=(0, -0.75, 1.75), rot=(0.9, 0, 0))
    tail.cube((-0.5, -0.5, 0), (1, 1, 4), **mc('skin', clusters=0.0, rim=False))
    ttip = tail.part('tail_tip', pivot=(0, 0, 4), rot=(-0.6, 0, 0))
    ttip.cube((-0.4, -0.4, 0), (0.8, 0.8, 3.5), **mc('skin_d', clusters=0.0, rim=False), faces={'south': mc('vein', clusters=0.0, glow=True)})
    return m


MODELS = {'jailer': jailer, 'sculkling': sculkling}
MODELS.update(__import__('cypole').MODELS)  # CR4: the Cypole (tools/cypole.py)


# =========================================================================== sounds (vanilla events)
SOUNDS = {
    'entity.jailer.ambient': [('event:entity.warden.ambient', 0.8, 0.7), ('event:entity.warden.tendril_clicks', 0.7, 0.8),
                              ('event:block.chain.step', 0.6, 0.6)],
    'entity.jailer.listen': [('event:entity.warden.listening', 1.0, 0.85), ('event:entity.warden.tendril_clicks', 1.0, 0.7)],
    'entity.jailer.step': [('event:entity.warden.step', 0.7, 1.1), ('event:block.chain.step', 0.4, 0.7)],
    'entity.jailer.hurt': [('event:entity.warden.hurt', 1.0, 0.85)],
    'entity.jailer.death': [('event:entity.warden.death', 1.0, 0.9), ('event:block.anvil.destroy', 0.6, 0.6)],
    'entity.jailer.emerge': [('event:entity.warden.emerge', 1.0, 0.9), ('event:entity.warden.dig', 0.8, 0.8)],
    'entity.jailer.windup': [('event:entity.warden.listening_angry', 1.0, 0.8), ('event:block.chain.hit', 0.8, 0.6)],
    'entity.jailer.slam': [('event:entity.warden.attack_impact', 1.2, 0.8), ('event:item.mace.smash_ground_heavy', 0.9, 0.8),
                           ('event:block.anvil.land', 0.7, 0.6)],
    'entity.jailer.trap': [('event:block.iron_door.close', 1.0, 0.6), ('event:block.vault.close_shutter', 1.0, 0.7),
                           ('event:block.chain.place', 1.0, 0.6)],
    'entity.jailer.squeeze': [('event:entity.iron_golem.damage', 0.8, 0.6), ('event:entity.warden.heartbeat', 1.0, 1.0)],
    'entity.jailer.rattle': [('event:block.chain.hit', 1.0, 0.7), ('event:block.iron.hit', 1.0, 0.8),
                             ('event:entity.zombie.attack_iron_door', 0.5, 1.4)],
    'entity.jailer.break': [('event:block.anvil.destroy', 1.0, 0.8), ('event:block.chain.break', 1.0, 0.6)],
    'entity.jailer.regrow': [('event:block.sculk_catalyst.bloom', 1.0, 0.8), ('event:block.chain.place', 0.8, 0.8)],
    # CR4 the harder cell: the heartbeat cue, the grip loosening, a good heave, squirming against it, a guard's kick
    'entity.jailer.pulse': [('event:entity.warden.heartbeat', 1.0, 1.15)],
    'entity.jailer.loosen': [('event:block.chain.step', 1.0, 0.65), ('event:block.chain.place', 0.9, 0.75)],
    'entity.jailer.heave': [('event:block.chain.break', 1.0, 0.7), ('event:block.anvil.land', 0.5, 1.4), ('event:block.iron.hit', 1.0, 0.6)],
    'entity.jailer.tighten': [('event:entity.iron_golem.damage', 0.6, 0.5), ('event:block.chain.hit', 0.8, 0.5)],
    'entity.jailer.kick': [('event:entity.warden.attack_impact', 1.0, 1.15), ('event:entity.ravager.stunned', 0.5, 1.4)],
    'entity.sculkling.ambient': [('event:entity.witch.celebrate', 0.5, 1.8), ('event:entity.vex.ambient', 0.5, 1.4),
                                 ('event:entity.allay.ambient_without_item', 0.4, 0.9)],
    'entity.sculkling.screech': [('event:entity.fox.screech', 0.8, 1.3), ('event:entity.bat.ambient', 1.0, 0.6),
                                 ('event:block.sculk_shrieker.shriek', 0.4, 1.8)],
    'entity.sculkling.hurt': [('event:entity.bat.hurt', 0.8, 0.8), ('event:entity.vex.hurt', 0.6, 1.2)],
    'entity.sculkling.death': [('event:entity.bat.death', 0.8, 0.8), ('event:entity.vex.death', 0.6, 1.3)],
    'entity.sculkling.step': [('event:entity.spider.step', 0.25, 1.6)],
    'entity.sculkling.snatch': [('event:entity.allay.item_taken', 1.0, 1.0), ('event:entity.witch.celebrate', 0.7, 2.0)],
    'entity.sculkling.scared': [('event:entity.bat.hurt', 0.7, 1.4), ('event:entity.vex.hurt', 0.6, 1.6)],
    'entity.sculkling.twitch': [('event:entity.warden.tendril_clicks', 0.6, 1.8), ('event:block.sculk_sensor.clicking', 0.5, 1.6)],
}
SUBTITLES = {
    'entity.jailer.ambient': 'Jailer rattles', 'entity.jailer.listen': 'Jailer listens', 'entity.jailer.step': 'Heavy footsteps',
    'entity.jailer.hurt': 'Jailer hurts', 'entity.jailer.death': 'Jailer dies', 'entity.jailer.emerge': 'Jailer emerges',
    'entity.jailer.windup': 'Jailer heaves its cell', 'entity.jailer.slam': 'Cell slams down', 'entity.jailer.trap': 'Cell locks shut',
    'entity.jailer.squeeze': 'Cell squeezes', 'entity.jailer.rattle': 'Bars rattle', 'entity.jailer.break': 'Bars break',
    'entity.jailer.regrow': 'Bars regrow', 'entity.jailer.pulse': "Jailer's heart thumps", 'entity.jailer.loosen': 'Grip loosens',
    'entity.jailer.heave': 'Bars buckle', 'entity.jailer.tighten': 'Grip tightens', 'entity.jailer.kick': 'Jailer kicks',
    'entity.sculkling.ambient': 'Sculkling giggles', 'entity.sculkling.screech': 'Sculkling screeches', 'entity.sculkling.hurt': 'Sculkling hurts',
    'entity.sculkling.death': 'Sculkling dies', 'entity.sculkling.step': 'Something skitters', 'entity.sculkling.snatch': 'Sculkling snatches something',
    'entity.sculkling.scared': 'Sculkling whimpers', 'entity.sculkling.twitch': 'Ears twitch',
}


def sounds(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    __import__('cypole').sounds(GA)  # CR4: the Cypole


# =========================================================================== data: loot, tags, spawns, text
SHINIES = ['minecraft:gold_ingot', 'minecraft:gold_nugget', 'minecraft:raw_gold', 'minecraft:iron_ingot', 'minecraft:copper_ingot',
           'minecraft:diamond', 'minecraft:emerald', 'minecraft:amethyst_shard', 'minecraft:lapis_lazuli', 'minecraft:quartz',
           'minecraft:netherite_ingot', 'minecraft:echo_shard', 'minecraft:golden_apple', 'minecraft:clock',
           'siftite_ingot', 'siftite_nugget', 'serbim_ingot', 'raw_serbim', 'chrome_pearl', 'skysong_gem', 'star_shard', 'prism_gem']


def data(GA):
    import gen_data as D
    for i in SHINIES:
        GA.tag('item', f'{NS}:sculkling_shinies', rl(i))
    # the Jailer (CR4): the Sculkite crystal it grows its cell from - its only drop
    D.table('entity', 'entities/jailer', [
        D.pool([D.item('sculkite', count=(2, 4), extra=[D.LOOTING])]),
    ])
    GA.tag('item', f'{NS}:sculkite', rl('sculkite'))
    # Sculklings hoard a little gold (anything they stole is dropped by the entity itself)
    D.table('entity', 'entities/sculkling', [
        D.pool([D.item('minecraft:gold_nugget', count=(0, 2), extra=[D.LOOTING])]),
        D.pool([D.item('minecraft:sculk_vein', count=(0, 1))]),
        D.pool([D.item('minecraft:amethyst_shard', 3), D.item('minecraft:emerald', 1), D.item('minecraft:echo_shard', 1)],
               condition={'type': 'minecraft:all_of', 'terms': [D.PLAYER_KILL, D.chance(0.06)]}),
    ])
    # spawns: all over the Sift, but the spawn rules keep them to dark caves (Jailers below y 0 or in the Deep Sift)
    GA.write(os.path.join(GA.RES, 'data', NS, 'neoforge', 'biome_modifier', 'cave_creatures.json'), {
        'type': 'neoforge:add_spawns', 'biomes': f'#{NS}:is_sift',
        'spawners': [{'type': rl('sculkling'), 'count': {'type': 'minecraft:uniform', 'min_inclusive': 3, 'max_inclusive': 5}, 'weight': 12},
                     {'type': rl('jailer'), 'count': 1, 'weight': 3}]})
    GA.LANG.update(lang())
    __import__('cypole').data(GA)  # CR4: the Cypole's loot, spawn tags, particle, band voice and text


def lang():
    L = {f'entity.{NS}.jailer': 'Jailer', f'entity.{NS}.sculkling': 'Sculkling', f'entity.{NS}.jail_cell': 'Jail Cell'}
    L.update({
        f'message.{NS}.jailer.trapped': "The Jailer has you! When its heart thumps and the bars glow, hit them - or struggle (sneak)!",
        f'message.{NS}.jailer.struggle': 'Its grip is too tight - wait for the bars to glow!',
        f'message.{NS}.jailer.beat': 'Listen for its heartbeat: strike the bars as they glow!',
        f'message.{NS}.sculkling.snatched': 'A Sculkling snatched your %s!',
        f'codex.{NS}.jailer.title': 'Jailer', f'codex.{NS}.jailer.tagline': 'Hostile - blind, and it carries a cell',
        f'codex.{NS}.jailer.body': ('A tall, blind kin of the Warden from the deepest caves, hauling a cell of sculk-iron bars. It hunts '
                                    'by sound - footsteps, fighting, every note - so sneak. Reach you and it slams the cell down over you. '
                                    'Its grip beats with its heart: at each thump the bars glow and loosen - only then do blows (or a '
                                    'struggle: sneak) bend them. It squeezes harder each time, and bars left alone grow back. It kicks '
                                    'away rescuers; a friend can still break the bars from outside. Break free and it guards you, '
                                    'regrows its cell in moments and slams again: run! Drops Sculkite.'),
        f'item.{NS}.sculkite': 'Sculkite',
        f'codex.{NS}.sculkite.title': 'Sculkite', f'codex.{NS}.sculkite.tagline': 'The Jailer\'s dark crystal',
        f'codex.{NS}.sculkite.body': ('A dark crystal of sculk, cold to the touch and humming at a pitch only the sculk can hear. Jailers '
                                      'grow the bars of their cells from it: it studs every cell, and a fallen Jailer leaves a few '
                                      'shards behind. Smiths of the Sift work it into echo gear and into armour for Stompers - never '
                                      'into armour for people, whom its hum drives to distraction.'),
        f'codex.{NS}.sculkling.title': 'Sculkling', f'codex.{NS}.sculkling.tagline': 'Hostile - giggling cave goblins',
        f'codex.{NS}.sculkling.body': 'Small blind sculk goblins with giant bat ears, skittering through dark caves in packs of three to five. They hear everything except a player who sneaks. Hear you, and they screech, swarm and scratch - and one may snatch something shiny from your pockets (gold, gems, ingots) and run, giggling. Kill the thief to get it back. Their ears cannot bear music: play a note and they cover them and flee.',
    })
    return L


# =========================================================================== spawn eggs (16 x 16)

def items():
    import items16 as I
    o = '#03141a'
    jailer = I.egg(['#061a21', '#0c2c35', '#15424d', '#1d5560'], o, {
        1: '......t..t......',
        2: '.....tt..tt.....',
        4: '......hhhh......',
        6: '....bbbbbbbb....',
        7: '....bsSssSsb....',
        8: '....bbSSSSbb....',
        10: '...i.i.i.i.i....',
        11: '...i.i.i.i.i....',
        12: '...iiiiiiiii....',
    }, pal={'t': (I.GLOW[2], o), 'h': (I.BONE[2], o), 'b': (I.BONE[3], o), 's': (I.GLOW[1], o), 'S': (I.GLOW[3], o),
            'i': ('#566872', o)}, no_ol='tsS')
    sculkling = I.egg(['#09252d', '#123b45', '#1d5661', '#25616a'], o, {
        1: '..e..........e..',
        2: '..ee........ee..',
        3: '..eVe......eVe..',
        4: '...eVe....eVe...',
        5: '....ee....ee....',
        7: '.....p....p.....',
        9: '.....mtmtmt.....',
        11: '.......gg.......',
        12: '.......gg.......',
    }, pal={'e': ('#0e3038', o), 'V': (I.GLOW[2], o), 'p': ('#020608', o), 'm': ('#04131a', o), 't': ('#efe8d2', o),
            'g': (I.GLOW[3], o)}, no_ol='Vg')
    out = {'jailer_spawn_egg': jailer, 'sculkling_spawn_egg': sculkling, 'sculkite': sculkite()}
    out.update(__import__('cypole').items())  # CR4: the Cypole's spawn egg
    return out


def sculkite():
    """Sculkite: a cluster of dark sculk crystal - deep teal-black facets, edges lit cyan by the glow
    trapped inside, one bright glint. Light from the top left like every vanilla item."""
    import items16 as I
    rows = [
        '................',
        '.........G......',
        '........gH2.....',
        '.......gH331....',
        '...g..gH33321...',
        '..gHg.H333221...',
        '..H32gH332211...',
        '..H332H322211.g.',
        '...3322H2211.gHg',
        '...3332H2111gH32',
        '....33222211H322',
        '....322211111321',
        '.....2211111.21.',
        '......11111.....',
        '................',
        '................',
    ]
    o = '#030b10'
    pal = I.ramp('123', ['#0a1c26', '#123546', '#1d5466'], o)
    pal.update({'H': ('#2fb8b8', o), 'g': (I.GLOW[2], o), 'G': (I.GLOW[4], o)})
    return I.grid(rows, pal, ol=True, no_ol='G')
