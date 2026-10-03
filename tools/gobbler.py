"""The Gobbler (sea & sky, agent F; remade by C2): a Warden-kin alien catfish grown out of sculk.
Blue-black sculk skin threaded with cyan glowing veins, a crust of living sculk down its back with
sensor spines, rib-like gill slits under flaring gill covers, rows of bioluminescent photophores,
a huge fanged maw glowing at the gullet, Warden ear tendrils, and two whisker barbels that end in
soul-lantern lures. The bone ribcage of trapped souls still hangs under its chest.

Registered into mobs_wild.ALL (one line at its bottom); GobblerModel.java animates it.
"""
import random

from modelkit import Model
from mobs_wild import mc

PAL = {
    'skin': '#0b1a26', 'skin_l': '#13283a', 'skin_d': '#060d15', 'belly': '#18293a', 'belly_l': '#22384c', 'belly_d': '#0f1b28',
    'crust': '#0d3340', 'crust_l': '#134b5c', 'crust_d': '#071d26',
    'vein': '#1ee3e8', 'vein_d': '#0a7f8e', 'glow2': '#c8fffb', 'spot': '#5ff8ff', 'spot_d': '#178e9c',
    'bone': '#c4cfc9', 'bone_l': '#e2ebe4', 'bone_d': '#87948f',
    'tooth': '#e6eee4', 'tooth_d': '#a3b0a8', 'throat': '#050a10', 'throat_d': '#020407', 'gullet': '#29dfeb', 'pit': '#010305',
    'gum': '#1d2c40', 'gum_d': '#121c2a',
    'iron': '#2a3137', 'iron_l': '#46515a', 'iron_d': '#151a1f', 'soul': '#8afaff', 'soul_d': '#16aebb',
    'barbel': '#10273a', 'barbel_l': '#1b3a52', 'barbel_d': '#08141f',
    'fin': '#0d2433', 'fin_l': '#173e52', 'fin_d': '#071520', 'gill': '#09131d',
}
MATERIALS = {'skin': 'sculk', 'belly': 'sculk', 'crust': 'sculk', 'barbel': 'sculk', 'gum': 'skin', 'fin': 'membrane', 'iron': 'metal'}
GLOW = 'vVoOgGsS'  # every glowing map key
KEYS = {'v': 'vein_d', 'V': 'vein', 'o': 'glow2', 'O': 'spot', 's': 'spot_d', 'g': 'vein', 'G': 'glow2', 'S': 'soul', 'b': 'bone', 'B': 'bone_d',
        'p': 'pit', 'c': 'crust_d', 'k': 'skin_d', 'i': 'iron', 'I': 'iron_l', 'd': 'iron_d', 't': 'tooth', 'T': 'throat', 'u': 'gum'}


def _grid(w, h):
    return [['.'] * w for _ in range(h)]


def _rows(g):
    return [''.join(r) for r in g]


def veins(w, h, seed, roots=3, side='left', reach=0.8, grid=None):
    """Branching glowing veins (hd: one char per texel) crawling in from one edge of a face: a dim
    vein ('v') with bright nodes ('V') where branches split."""
    rnd = random.Random(seed)
    g = grid or _grid(w, h)
    for n in range(roots):
        if side == 'left':
            x, y, dx, dy = 0, int((n + 0.5) * h / roots) + rnd.randint(-1, 1), 1, 0
        elif side == 'right':
            x, y, dx, dy = w - 1, int((n + 0.5) * h / roots) + rnd.randint(-1, 1), -1, 0
        elif side == 'top':
            x, y, dx, dy = int((n + 0.5) * w / roots) + rnd.randint(-1, 1), 0, 0, 1
        else:
            x, y, dx, dy = int((n + 0.5) * w / roots) + rnd.randint(-1, 1), h - 1, 0, -1
        stack = [(x, y, dx, dy, int((w if dx else h) * reach))]
        while stack:
            x, y, dx, dy, life = stack.pop()
            for _ in range(life):
                if not (0 <= x < w and 0 <= y < h):
                    break
                g[y][x] = 'v' if g[y][x] in '.v' else g[y][x]
                if rnd.random() < 0.08 and life > 6:
                    g[y][x] = 'V'
                    ndx, ndy = (dx, rnd.choice((-1, 1))) if dx else (rnd.choice((-1, 1)), dy)
                    stack.append((x, y, ndx, ndy, life // 2))
                # wander sideways now and then, mostly forward
                if rnd.random() < 0.35:
                    if dx:
                        y += rnd.choice((-1, 1))
                    else:
                        x += rnd.choice((-1, 1))
                    if 0 <= x < w and 0 <= y < h:
                        g[y][x] = 'v' if g[y][x] == '.' else g[y][x]
                x, y = x + dx, y + dy
                life -= 1
    return g


def photophores(g, y, x0, x1, every=4, phase=0):
    """A lateral line of glowing spots: bright core, dim halo."""
    for x in range(x0 + phase, x1, every):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if 0 <= y + dy < len(g) and 0 <= x + dx < len(g[0]):
                g[y + dy][x + dx] = 'O' if (dx, dy) != (0, 0) else 'o'
        for dx, dy in ((-1, 0), (2, 1), (0, -1), (1, 2)):
            if 0 <= y + dy < len(g) and 0 <= x + dx < len(g[0]) and g[y + dy][x + dx] == '.':
                g[y + dy][x + dx] = 's'
    return g


def sculk_crust(w, h, seed, density=0.06):
    """The top of a sculk block, grown over skin: dark clumps and pinpricks of cyan."""
    rnd = random.Random(seed)
    g = _grid(w, h)
    for _ in range(int(w * h * density)):
        x, y = rnd.randrange(w), rnd.randrange(h)
        for dx, dy in rnd.choice((((0, 0), (1, 0)), ((0, 0), (0, 1)), ((0, 0), (1, 0), (0, 1)), ((0, 0),))):
            if 0 <= x + dx < w and 0 <= y + dy < h:
                g[y + dy][x + dx] = 'c'
    for _ in range(int(w * h * density * 0.35)):
        x, y = rnd.randrange(w), rnd.randrange(h)
        g[y][x] = 'g' if rnd.random() < 0.75 else 'G'
    return g


def gill_slits(w, h, n=4, x0=1):
    """Rib-like gill slits (hd): bone ribs with dark slits between them, lit cyan deep inside."""
    g = _grid(w, h)
    for i in range(n):
        x = x0 + i * 3
        for y in range(2, h - 2):
            if x + 2 < w:
                g[y][x] = 'B'
                g[y][x + 1] = 'p'
                g[y][x + 2] = 'v' if 3 < y < h - 4 and (y + i) % 3 else 'p'
    return g


def teeth(n, offset=0):
    return [''.join('t' if (x + offset) % 2 == 0 else 'T' for x in range(n))]


def gobbler() -> Model:
    m = Model('gobbler', (128, 128), PAL, {'gobbler': {}}, res=2, materials=MATERIALS)
    body = m.part('body', pivot=(0, 14, 2))

    def skin(**kw):
        d = dict(clusters=0.35)
        d.update(kw)
        return mc('skin', **d)

    def hd(rows, base='skin', **kw):
        d = dict(clusters=0.3, map=_rows(rows) if isinstance(rows[0], list) else rows, keys=KEYS, glow_keys=GLOW, hd=True)
        d.update(kw)
        return mc(base, **d)

    # ---- the torso: veins crawl back from the gills, a line of photophores, rib-like gill slits at the front
    flank = veins(28, 24, 11, roots=3, side='left', reach=0.9)
    flank = photophores(flank, 17, 3, 27, every=4)
    for y, row in enumerate(gill_slits(13, 16, n=4)):
        for x, ch in enumerate(row):
            if ch != '.':
                flank[y + 1][x] = ch
    flank_w = [r[::-1] for r in flank]
    belly_down = photophores(photophores(_grid(28, 28), 6, 2, 26, every=5), 20, 4, 26, every=5)
    body.cube((-7, -6, -4), (14, 12, 14), **skin(bands=[(9, 'belly')]), faces={
        'up': hd(sculk_crust(28, 28, 3)),
        'east': hd(flank, bands=[(9, 'belly')]),
        'west': hd(flank_w, bands=[(9, 'belly')]),
        'down': hd(belly_down, base='belly'),
        'south': skin(),
    })
    # a crust of living sculk over the back, its spine ridged with bone
    crust = body.part('crust', pivot=(0, -6, 0))
    crust.cube((-6, -1.5, -3), (12, 2, 12), **mc('crust', clusters=0.5), faces={
        'up': hd(sculk_crust(24, 24, 5, 0.09), base='crust'),
        'east': hd(sculk_crust(24, 4, 6, 0.12), base='crust'), 'west': hd(sculk_crust(24, 4, 7, 0.12), base='crust'),
        'north': hd(sculk_crust(24, 4, 8, 0.12), base='crust'), 'south': hd(sculk_crust(24, 4, 9, 0.12), base='crust'),
    })
    for i, z in enumerate((-2, 1, 4, 7)):
        crust.cube((-1, -2.5 - (i in (1, 2)), z), (2, 1 + (i in (1, 2)), 2), **mc('bone_d', clusters=0.3),
                   faces={'up': mc('bone', clusters=0.2)})
    # sculk sensor spines along the back: crossed tendril planes with glowing tips
    for i, (z, hgt) in enumerate(((0, 6), (4, 8), (8, 5))):
        sp = body.part(f'spine_{i}', pivot=(0, -7.5, z - 4 + 2), rot=(-0.25, 0, 0))
        tip = ['.GG.', 'gVVg', '.gg.'] + ['.vv.'] * (2 * hgt - 5) + ['.kk.', '.kk.']
        sp.cube((-1, -hgt, 0), (2, hgt, 0), **mc('crust', clusters=0.0, rim=False, map=tip, keys=KEYS, glow_keys=GLOW, hd=True))
        sp.cube((0, -hgt, -1), (0, hgt, 2), **mc('crust', clusters=0.0, rim=False, map=tip, keys=KEYS, glow_keys=GLOW, hd=True))
    # ---- the chest: a bone ribcage over trapped, glowing souls (the Warden's chest, slung under a fish)
    cage = []
    for y in range(9):
        cage.append(''.join('b' if x % 2 == 0 else ('G' if (x + y) % 4 == 1 else 'S') for x in range(10)))
    chest = body.part('chest', pivot=(0, 6, 1))
    side_cage = [''.join('b' if x % 2 == 0 else 'S' for x in range(9))] * 3
    chest.cube((-5, -1, -3), (10, 3, 9), **mc('belly', clusters=0.15), faces={
        'down': mc('belly', clusters=0.0, rim=False, map=cage, keys={'b': 'bone', 'S': 'soul_d', 'G': 'soul'}, glow_keys='SG'),
        'east': mc('belly', clusters=0.0, map=side_cage, keys={'b': 'bone_d', 'S': 'soul'}, glow_keys='S'),
        'west': mc('belly', clusters=0.0, map=side_cage, keys={'b': 'bone_d', 'S': 'soul'}, glow_keys='S'),
    })
    # ---- the great flat head: no eyes, a bone brow, sensory pits, veins - and a maw wider than the body
    head = body.part('head', pivot=(0, -1, -4))
    skull = sculk_crust(40, 22, 21, 0.03)
    skull = veins(40, 22, 22, roots=4, side='top', reach=0.6, grid=skull)
    for x in range(4, 37, 4):  # two rows of blind sensory pits towards the snout
        for y in (13, 17):
            skull[y][x] = 'p'
            skull[y][x + 1] = 'p'
            skull[y + 1][x] = 'p'
            skull[y + 1][x + 1] = 's'
    snout = _grid(40, 16)
    for x in range(40):
        snout[0][x] = 'B'
        snout[1][x] = 'b' if x % 6 else 'B'
    for x in range(3, 38, 5):
        snout[5][x] = snout[5][x + 1] = 'p'
        snout[6][x] = 'p'
        snout[6][x + 1] = 's'
    photophores(snout, 10, 4, 38, every=6, phase=1)
    cheek = veins(22, 16, 23, roots=2, side='left', reach=0.9)
    photophores(cheek, 11, 2, 20, every=5)
    roof = _grid(40, 22)
    for x in range(10, 30):
        roof[0][x] = 'G' if 12 < x < 27 else 'g'
        roof[1][x] = 'g' if 13 < x < 26 else '.'
    head.cube((-10, -6, -11), (20, 8, 11), **skin(), faces={
        'up': hd(skull),
        'north': hd(snout),
        'east': hd([r[::-1] for r in cheek]), 'west': hd(cheek),
        # the roof of the mouth: dark throat, the gullet glowing at the back
        'down': hd(roof, base='throat', clusters=0.0, rim=False),
    })
    # a bone brow ridge over where eyes should be
    brow = sculk_crust(32, 10, 24, 0.10)
    for x in range(1, 32, 4):
        brow[8][x] = brow[8][x + 1] = 'p'
    for x in range(0, 32, 3):
        brow[0][x] = 'B'
    head.cube((-8, -7, -10), (16, 1, 5), **mc('crust', clusters=0.4), faces={
        'up': mc('crust', clusters=0.3, map=_rows(brow), keys=KEYS, glow_keys=GLOW, hd=True),
        'north': mc('crust', clusters=0.0, rim=False, map=['B.' * 16, 'bB' * 16], keys=KEYS, hd=True)})
    # the upper teeth: a row of small ones and four great fangs
    head.cube((-9.5, 2, -10.5), (19, 1, 10), **mc('gum', clusters=0.0, rim=False), faces={
        'north': mc('tooth', clusters=0.0, rim=False, map=teeth(19), keys=KEYS),
        'east': mc('tooth', clusters=0.0, rim=False, map=teeth(10, 1), keys=KEYS),
        'west': mc('tooth', clusters=0.0, rim=False, map=teeth(10, 1), keys=KEYS),
        'up': mc('gum', clusters=0.0, rim=False), 'down': mc('gum_d', clusters=0.0, rim=False),
    })
    for x, ln in ((-8, 4), (7, 4), (-4, 3), (3, 3)):  # overbite fangs hanging in front of the jaw
        head.cube((x, 2, -11.5), (1, ln, 1), **mc('tooth', clusters=0.0, rim=False), faces={
            'down': mc('tooth_d', clusters=0.0, rim=False), 'south': mc('tooth_d', clusters=0.0, rim=False)})
    # Warden ear tendrils on the crown, glowing and twitching when it listens
    for side, sx in (('left', 1), ('right', -1)):
        t = head.part(f'{side}_tendril', pivot=(7 * sx, -6, -4), rot=(-0.15, 0, 0.3 * sx))
        tend = _grid(14, 18)
        for y in range(18):
            for x in range(14):
                if x % 4 == 1:
                    tend[y][x] = 'V' if y < 3 else 'v'
                elif y < 2 and x % 4 in (0, 2):
                    tend[y][x] = 'g'
        t.cube((0, -9, -2), (0, 9, 7), **mc('crust', clusters=0.0, rim=False, alpha='membrane', edge='outer', edge_depth=1, scallop=2,
                                             map=_rows(tend), keys=KEYS, glow_keys=GLOW, hd=True))
    # gill covers: bony plates hinged at the back of the head, flared open as it breathes and hunts
    cover = _grid(10, 18)
    for y in range(18):
        for x in range(10):
            if x % 3 == 0:
                cover[y][x] = 'b' if y % 5 else 'B'
            elif x % 3 == 1 and 2 < y < 15:
                cover[y][x] = 'v' if (x + y) % 4 else 'V'
    for side, sx in (('left', 1), ('right', -1)):
        gp = head.part(f'{side}_gill', pivot=(10 * sx, -2, -2), rot=(0, 0.2 * sx, 0))
        gp.cube((0, -4, 0), (0, 9, 5), **mc('gill', clusters=0.0, rim=False, alpha='membrane', edge='bottom', edge_depth=1, scallop=2,
                                             map=_rows(cover), keys=KEYS, glow_keys=GLOW, hd=True))
    # ---- the lower jaw, hinged at the back of the head: deep, fanged, glowing inside
    jaw = head.part('jaw', pivot=(0, 2, -1))
    tongue = _grid(40, 20)
    for y in range(0, 7):
        for x in range(8 + y, 32 - y):
            tongue[y][x] = 'G' if y < 3 and 14 < x < 26 else 'g'
    chin = _grid(40, 10)
    for x in range(40):
        chin[0][x] = 'b'
        chin[1][x] = 'B' if x % 4 else 'b'
    for x in range(2, 38, 4):
        chin[5][x] = chin[5][x + 1] = 'p'
    photophores(chin, 7, 3, 38, every=6, phase=2)
    jaw_side = veins(20, 10, 31, roots=2, side='left', reach=0.8)
    jaw.cube((-10, 0, -10), (20, 5, 10), **skin(bands=[(3, 'belly')]), faces={
        'up': hd(tongue, base='throat', clusters=0.0, rim=False),
        'north': hd(chin, bands=[(3, 'belly')]),
        'east': hd([r[::-1] for r in jaw_side], bands=[(3, 'belly')]), 'west': hd(jaw_side, bands=[(3, 'belly')]),
        'down': mc('belly', clusters=0.3),
    })
    jaw.cube((-9.5, -1, -9.5), (19, 1, 9), **mc('gum', clusters=0.0, rim=False), faces={
        'north': mc('tooth', clusters=0.0, rim=False, map=teeth(19, 1), keys=KEYS),
        'east': mc('tooth', clusters=0.0, rim=False, map=teeth(9), keys=KEYS),
        'west': mc('tooth', clusters=0.0, rim=False, map=teeth(9), keys=KEYS),
        'down': mc('gum_d', clusters=0.0, rim=False), 'up': mc('gum', clusters=0.0, rim=False),
    })
    for x, ln in ((-7, 4), (6, 4), (-2, 3), (1, 3)):
        jaw.cube((x, -ln, -10.5), (1, ln, 1), **mc('tooth', clusters=0.0, rim=False), faces={
            'up': mc('tooth_d', clusters=0.0, rim=False), 'south': mc('tooth_d', clusters=0.0, rim=False)})
    # ---- whisker barbels from the corners of the mouth, each ending in a soul-lantern lure
    lantern = ['dIIIId', 'I_SS_I', 'I_GG_I', 'I_SS_I', 'I_SS_I', 'I_SS_I', 'I_GS_I', 'dIIIId']
    for side, sx in (('left', 1), ('right', -1)):
        b0 = head.part(f'{side}_barbel', pivot=(9.5 * sx, 0, -10), rot=(-0.45, 0.75 * sx, 0))
        b0.cube((-0.5, -0.5, -8), (1, 1, 8), **mc('barbel', clusters=0.0, rim=False), faces={
            'east': mc('barbel', clusters=0.0, rim=False, map=['.' * 8, 'v.v.v.v.'], keys=KEYS, glow_keys=GLOW, center=False, at=(0, 0), hd=True),
            'west': mc('barbel', clusters=0.0, rim=False, map=['.' * 8, '.v.v.v.v'], keys=KEYS, glow_keys=GLOW, center=False, at=(0, 0), hd=True)})
        b1 = b0.part(f'{side}_barbel_tip', pivot=(0, 0, -8), rot=(0.75, 0.2 * sx, 0))
        b1.cube((-0.5, -0.5, -7), (1, 1, 7), **mc('barbel_l', clusters=0.0, rim=False), faces={
            'east': mc('barbel_l', clusters=0.0, rim=False, map=['vv..v..v.....'], keys=KEYS, glow_keys=GLOW, center=False, at=(0, 0), hd=True),
            'west': mc('barbel_l', clusters=0.0, rim=False, map=['.....v..v..vv'], keys=KEYS, glow_keys=GLOW, center=False, at=(0, 0), hd=True)})
        lure = b1.part(f'{side}_lure', pivot=(0, 0, -7), rot=(-0.3, 0, 0))
        lure.cube((-0.5, 0, -0.5), (1, 2, 1), **mc('iron', clusters=0.0, rim=False, map=['d', 'i', 'd', 'i'], keys=KEYS, hd=True))
        lure.cube((-1, 2, -1), (2, 1, 2), **mc('iron', clusters=0.0), faces={'up': mc('iron_l', clusters=0.0)})
        lure.cube((-1.5, 3, -1.5), (3, 4, 3), **mc('iron', clusters=0.0, rim=False), faces={
            f: mc('iron', clusters=0.0, rim=False, map=lantern, keys=KEYS, glow_keys=GLOW, hd=True) for f in ('north', 'south', 'east', 'west')},
            )
        lure.cube((-0.5, 4, -0.5), (1, 2, 1), **mc('soul', clusters=0.0, rim=False, glow=True), faces={
            'up': mc('glow2', clusters=0.0, rim=False, glow=True)})
        lure.cube((-1, 7, -1), (2, 1, 2), **mc('iron_d', clusters=0.0))
        c0 = jaw.part(f'{side}_chin_barbel', pivot=(3.5 * sx, 5, -7), rot=(-0.9, 0.2 * sx, 0))
        c0.cube((-0.5, 0, -0.5), (1, 6, 1), **mc('barbel', clusters=0.0, rim=False), faces={
            'down': mc('vein', clusters=0.0, glow=True),
            'north': mc('barbel', clusters=0.0, rim=False, map=['.', '.', '.', '.', 'v', 'v', 'V', 'V', 'g', 'G', 'G', 'G'], keys=KEYS,
                        glow_keys=GLOW, hd=True)})
    # ---- fins: dark membranes strung on glowing rays
    ray = lambda w, h, every: [''.join('V' if x % every == 0 and y < 2 else ('v' if x % every == 0 else '.') for x in range(w)) for y in range(h)]
    dorsal = body.part('dorsal', pivot=(0, -5, 8))
    dorsal.cube((0, -4, -1), (0, 4, 8), **mc('fin', clusters=0.0, rim=False, alpha='membrane', edge='bottom', map=ray(16, 8, 3), keys=KEYS,
                                            glow_keys=GLOW, hd=True))
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(7 * sx, 3, -1), rot=(0, 0.35 * sx, 0.35 * sx))
        pf.cube((0 if sx > 0 else -8, 0, -1), (8, 0, 7), **mc('fin', clusters=0.0, rim=False, alpha='membrane', edge='outer',
                                                               map=[r for r in ray(16, 14, 3)], keys=KEYS, glow_keys=GLOW, hd=True))
    # ---- the long tail: veins and photophores run on to the fluke
    tail = body.part('tail', pivot=(0, -0.5, 10))
    tflank = photophores(veins(16, 18, 41, roots=2, side='left', reach=1.0), 12, 1, 16, every=4)
    tail.cube((-5.5, -4.5, 0), (11, 9, 8), **skin(bands=[(6, 'belly')]), faces={
        'up': hd(sculk_crust(22, 16, 42)),
        'east': hd(tflank, bands=[(6, 'belly')]), 'west': hd([r[::-1] for r in tflank], bands=[(6, 'belly')]),
    })
    tail.cube((-1, -5.5, 1), (2, 1, 6), **mc('bone_d', clusters=0.3), faces={
        'up': mc('bone', clusters=0.0, map=['b', '.', 'b', '.', 'b', '.'], keys={'b': 'bone_d'})})
    tail2 = tail.part('tail2', pivot=(0, 0, 8))
    t2 = photophores(veins(14, 12, 43, roots=2, side='left', reach=1.0), 8, 1, 14, every=4, phase=2)
    tail2.cube((-3.5, -3, 0), (7, 6, 7), **skin(bands=[(4, 'belly')]), faces={
        'up': hd(sculk_crust(14, 14, 44)),
        'east': hd(t2, bands=[(4, 'belly')]), 'west': hd([r[::-1] for r in t2], bands=[(4, 'belly')]),
    })
    tail2.cube((0, -5, 1), (0, 2, 5), **mc('fin', clusters=0.0, rim=False, alpha='membrane', edge='bottom', map=ray(10, 4, 2), keys=KEYS,
                                          glow_keys=GLOW, hd=True))
    fluke = tail2.part('fluke', pivot=(0, 0, 7))
    fray = _grid(20, 32)
    for y in range(32):
        for x in range(20):
            if (y - 16) % 4 == 0 and x <= 18:
                fray[y][x] = 'v' if x < 14 else 'V'
            elif x == 0 and abs(y - 16) < 6:
                fray[y][x] = 'v'
            elif x >= 17:
                fray[y][x] = 'g'
    fluke.cube((0, -8, -1), (0, 16, 10), **mc('fin', clusters=0.0, rim=False, alpha='membrane', edge='outer', edge_depth=2, scallop=3,
                                              map=_rows(fray), keys=KEYS, glow_keys=GLOW, hd=True))
    return m
