"""Geometry + paint for the Sift's wild creatures: the Stomper, the three music fish and the Sky Whale
(see modelkit.py; registered into mobs.ALL at the bottom of mobs.py)."""
import math

from modelkit import Model

EXPRS = ['blink', 'happy', 'angry', 'hurt', 'dead']


# =========================================================================== face helpers
def rows_of(w, h, fn):
    """Builds a w x h texel map; fn(x, y, u, v) returns a char (u, v in -1..1 across the face)."""
    out = []
    for y in range(h):
        r = ''
        for x in range(w):
            u = (x + 0.5) / w * 2 - 1
            v = (y + 0.5) / h * 2 - 1
            r += fn(x, y, u, v)
        out.append(r)
    return out


def eye(w, h, expr, mirror=False, pupil='bar', rim=0.78, shine=True):
    """A round painted eye filling a w x h texel face, in one of the facial expressions.
    keys: r rim, i iris, I lower iris, p pupil, h highlight, l lid, d lash / crease line."""
    s = -1.0 if mirror else 1.0

    def fn(x, y, u, v):
        rr = u * u + v * v
        if rr > 1.0:
            return '.'
        uu = u * s
        if expr in ('blink', 'sleep'):
            return 'd' if abs(v - 0.15 - 0.25 * u * u) < 1.6 / h else 'l'
        if expr == 'happy':
            return 'd' if abs(v - (0.45 - 0.8 * (1 - u * u))) < 1.8 / h and v > -0.6 else 'l'
        if expr == 'hurt':
            line = abs(v - 0.05) < 1.4 / h and abs(u) < 0.85
            tick = abs(abs(u) - 0.55) < 1.3 / w and abs(v + 0.25) < 0.3
            return 'd' if line or tick else 'l'
        if rr > rim:
            return 'r'
        if expr == 'angry':
            lid = -0.15 + 0.55 * uu
            if v < lid:
                return 'l'
            if v < lid + 2.2 / h:
                return 'd'
        if expr == 'dead':
            if rr < 0.55 and (abs(u - v) < 2.0 / w or abs(u + v) < 2.0 / w):
                return 'p'
            return 'i' if v < 0.25 else 'I'
        if shine and abs(uu + 0.42) < 1.6 / w + 0.08 and abs(v + 0.38) < 1.6 / h + 0.08:
            return 'h'
        if pupil == 'bar' and abs(v - 0.05) < 0.2 and abs(u) < 0.58:
            return 'p'
        if pupil == 'dot' and (u * u + (v - 0.05) ** 2) < 0.16:
            return 'p'
        if pupil == 'slit' and abs(u) < 0.16 and abs(v) < 0.75:
            return 'p'
        return 'i' if v < 0.3 else 'I'
    return rows_of(w, h, fn)


def eye_exprs(w, h, mirror=False, **kw):
    return {e: eye(w, h, e, mirror, **kw) for e in EXPRS}


def overlay(base, top):
    """Lays map `top` over map `base` (same size); '.' in top keeps base."""
    return [''.join(t if t != '.' else b for b, t in zip(rb, rt)) for rb, rt in zip(base, top)]


def blank(w, h):
    return ['.' * w for _ in range(h)]


def put(grid, x, y, glyph, mirror=False):
    g = [list(r) for r in grid]
    for j, row in enumerate(glyph):
        if mirror:
            row = row[::-1]
        for i, ch in enumerate(row):
            if ch != '.' and 0 <= y + j < len(g) and 0 <= x + i < len(g[0]):
                g[y + j][x + i] = ch
    return [''.join(r) for r in g]


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


# =========================================================================== STOMPER
def stomper_mouth(expr, w=60, h=24):
    """The front of the Stomper's wide frog head: a long lip line with corners that curl with its
    mood, little nostril wrinkles round the trunk base and blush on the cheeks."""
    def fn(x, y, u, v):
        # lip line along the bottom, corners bending with the mood
        bend = {'happy': -0.55, 'angry': 0.35, 'hurt': 0.3, 'dead': 0.0, 'blink': -0.12}.get(expr, -0.12)
        edge = abs(u) ** 3
        lip_y = 0.82 + bend * edge
        if expr == 'hurt':
            lip_y += 0.06 * math.sin(u * 14)
        if abs(v - lip_y) < 1.3 / h and abs(u) < 0.96:
            return 'm'
        if abs(v - lip_y + 2.0 / h) < 0.8 / h and abs(u) < 0.9:
            return 'L'
        # cheeks
        if ((abs(u) - 0.78) ** 2 * 6 + (v - 0.35) ** 2 * 3) < 0.06 and expr in ('happy', '', 'blink'):
            return 'b'
        # wrinkles round the trunk base
        if abs(u) < 0.18 and abs(v + 0.15) < 0.5 and (y % 4 == 0) and abs(u) > 0.06:
            return 'w'
        # warts
        if (x * 7 + y * 13) % 53 == 0 and abs(v) < 0.6:
            return 'W'
        return '.'
    rows = rows_of(w, h, fn)
    if expr == 'angry':
        # bared teeth peeking out
        rows = put(rows, 18, h - 4, ['t.t.t.t.t.t.t.t.t.t.t.t'])
    if expr == 'happy':
        rows = put(rows, 27, h - 3, ['tttttt'])
    return rows


def stomper() -> Model:
    """The Stomper: a huge, shaggy mammoth-bullfrog. A wide warty frog body on four pillar legs, a
    broad frog head with a long four-part trunk, four eyes (two big frog domes, two little ones),
    floppy ears, a shaggy fur fringe and hump, and three spiracles on its back that puff."""
    pal = {
        'skin': '#4fa38e', 'skin_l': '#71c2a7', 'skin_d': '#357a6e', 'wart': '#2f6b60',
        'spot': '#3d8a7c', 'belly': '#efe0a8', 'belly_l': '#fbf0c8', 'belly_d': '#d4bf82',
        'fur': '#8a5c86', 'fur_l': '#a97aa3', 'fur_d': '#663f66', 'mane': '#c58bb8',
        'mouth': '#3a1a3a', 'lip': '#2c5c55', 'tongue': '#ff8fb0', 'teeth': '#fff8ec', 'blush': '#f59ab8',
        'eye': '#14182e', 'iris': '#ffd66b', 'iris_d': '#e09a2e', 'eye_hi': '#ffffff', 'lid': '#5db39b', 'lid_d': '#2c5c55',
        'eye2': '#7ff0ff', 'eye2_d': '#2fb8d8',
        'nail': '#f2e7cf', 'nail_d': '#c9b994', 'hole': '#1a2a33', 'chrome': '#a8fbff', 'chrome_l': '#f0d9f2',
        'trunk': '#4fa38e', 'trunk_l': '#71c2a7', 'trunk_d': '#357a6e', 'ring': '#2f6b60',
    }
    m = Model('stomper', (256, 256), pal, {'stomper': {}}, res=2, expressions=EXPRS)
    skin = mc('skin', clusters=0.35, spots=0.22, accent='spot')
    fur = mc('fur', clusters=0.25, streaks=0.55)

    body = m.part('body', pivot=(0, 11, 2))
    # the big warty barrel, pale belly band on the sides
    body.cube((-16, -20, -18), (32, 20, 36), **skin, faces={
        'down': mc('belly', clusters=0.3),
        'west': mc('skin', clusters=0.35, spots=0.22, accent='spot', bands=[(13, 'belly')]),
        'east': mc('skin', clusters=0.35, spots=0.22, accent='spot', bands=[(13, 'belly')]),
        'north': mc('belly', clusters=0.3),
        'south': mc('skin', clusters=0.35, spots=0.22, accent='spot', bands=[(13, 'belly')]),
    })
    # shaggy fur skirt hanging round the belly
    body.cube((-17, -7, -19), (34, 9, 38), **fur, fringe=2, faces={
        'up': dict(skip=True), 'down': dict(skip=True),
    })
    # the fur hump with its three spiracles
    hump = body.part('hump', pivot=(0, -20, 0))
    spiracle = ['..rrrr..', '.rHHHHr.', 'rHhhhhHr', 'rHhGGhHr', 'rHhGGhHr', 'rHhhhhHr', '.rHHHHr.', '..rrrr..']
    up_map = ['.' * 48 for _ in range(52)]
    for (cx, cy) in ((12, 10), (28, 10), (20, 26)):
        up_map = put(up_map, cx, cy, spiracle)
    hump.cube((-12, -6, -13), (24, 6, 26), **fur, fringe=1, faces={
        'up': mc('fur', clusters=0.25, streaks=0.5, hd=True, map=up_map,
                 keys={'r': 'ring', 'H': 'skin_d', 'h': 'hole', 'G': 'chrome'}, glow_keys='G'),
    })
    hump.cube((-8, -9, -8), (16, 3, 14), **fur, faces={'up': mc('mane', clusters=0.25, streaks=0.5)})
    for i, (sx, sz) in enumerate(((-6, -5), (6, -5), (0, 3))):
        sp = hump.part(f'spiracle_{i}', pivot=(sx * 0.75, -9, sz))
        sp.cube((-1.5, -1.5, -1.5), (3, 1.5, 3), color='skin_d', pattern='mc', clusters=0.0, rim=False, faces={
            'up': mc('hole', clusters=0.0, rim=False, hd=True, map=['rrrrrr', 'rhhhhr', 'rhGGhr', 'rhGGhr', 'rhhhhr', 'rrrrrr'],
                     keys={'r': 'ring', 'h': 'hole', 'G': 'chrome'}, glow_keys='G'),
        })
    # tail with a tuft
    tail = body.part('tail', pivot=(0, -15, 18), rot=(-0.7, 0, 0))
    tail.cube((-1.5, -1.5, 0), (3, 3, 7), **mc('skin', clusters=0.3))
    tail.cube((-2.5, -2.5, 6), (5, 5, 4), **fur, fringe=1)

    # ---- head
    head = body.part('head', pivot=(0, -6, -18))
    head.cube((-15, -10, -13), (30, 12, 14), **skin, faces={
        'north': mc('skin', clusters=0.2, spots=0.1, accent='spot', hd=True, map=stomper_mouth(''),
                    keys={'m': 'mouth', 'L': 'lip', 'b': 'blush', 'w': 'skin_d', 'W': 'wart', 't': 'teeth'},
                    expr={e: stomper_mouth(e) for e in EXPRS}),
        'down': mc('belly', clusters=0.2),
    })
    # brow ridge with a fur fringe hanging over the forehead
    head.cube((-12, -13, -11), (24, 4, 12), **fur, fringe=1, faces={'north': mc('fur', clusters=0.2, streaks=0.5, fringe=2)})
    jaw = head.part('jaw', pivot=(0, 2, 0))
    jaw.cube((-15, 0, -13), (30, 5, 14), **mc('belly', clusters=0.3), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, map=put(blank(60, 28), 18, 6, [
            '......tttttttttttttttttt......',
            '....ttTTTTTTTTTTTTTTTTTTtt....',
            '...tTTTTTTTTTTTTTTTTTTTTTTt...',
            '...TTTTTTTTTTTTTTTTTTTTTTTT...',
            '....TTTTTTTTTTTTTTTTTTTTTT....',
            '.....TTTTTTTTTTTTTTTTTTTT.....',
            '.......TTTTTTTTTTTTTTTT.......',
            '..........TTTTTTTTTT..........']), keys={'T': 'tongue', 't': 'teeth'}),
        'north': mc('belly', clusters=0.2, hd=True, map=['L' * 60, '.' * 60, '.' * 60, '..' + 'W.........' * 5 + '........'],
                    keys={'L': 'lip', 'W': 'belly_d'}),
        'west': mc('skin', clusters=0.3, bands=[(1, 'belly')]),
        'east': mc('skin', clusters=0.3, bands=[(1, 'belly')]),
        'down': mc('belly_d', clusters=0.2),
    })
    # the bullfrog's throat sac: it swells when the Stomper sings, dances or drinks
    sac = jaw.part('throat', pivot=(0, 5, -6))
    sac.cube((-9, -1, -5), (18, 5, 10), **mc('belly', clusters=0.15, rim=False), faces={'north': mc('belly', clusters=0.1, rim=False, hd=True,
              map=['.' * 36] * 2 + ['....' + 'd...' * 7 + '....'] + ['.' * 36] * 7, keys={'d': 'belly_d'})})
    # the four eyes: two big frog domes on top, two small ones low on the cheeks
    for side, sx in (('left', 1), ('right', -1)):
        mir = sx < 0
        big = head.part(f'{side}_eye', pivot=(8.5 * sx, -12, -7))
        eyekeys = {'r': 'skin_d', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}
        big.cube((-4, -5, -4), (8, 6, 8), **mc('skin', clusters=0.2), faces={
            'north': mc('skin', clusters=0.0, hd=True, map=eye(16, 12, '', mir), keys=eyekeys, expr=eye_exprs(16, 12, mir)),
            ('east' if sx > 0 else 'west'): mc('skin', clusters=0.0, hd=True, map=eye(16, 12, '', not mir, shine=False), keys=eyekeys,
                                                 expr=eye_exprs(16, 12, not mir, shine=False)),
        })
        lid = big.part(f'{side}_eyelid')
        lid.cube((-4, -5, -4), (8, 3, 8), inflate=0.15, **mc('lid', clusters=0.0, rim=False), faces={
            'north': mc('lid', clusters=0.0, rim=False, hd=True, map=['.' * 16] * 5 + ['d' * 16], keys={'d': 'lid_d'})})
        small = head.part(f'{side}_small_eye', pivot=(12.5 * sx, -5.5, -13))
        small.cube((-2, -2, -1.5), (4, 4, 2), **mc('skin', clusters=0.0), faces={
            'north': mc('skin', clusters=0.0, hd=True, map=eye(8, 8, '', mir, pupil='dot', rim=0.7),
                        keys={'r': 'skin_d', 'i': 'eye2', 'I': 'eye2_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'},
                        expr=eye_exprs(8, 8, mir, pupil='dot', rim=0.7)),
        })
        ear = head.part(f'{side}_ear', pivot=(15 * sx, -8, -4), rot=(0, -0.3 * sx, 0.35 * sx))
        ear.cube((0 if sx > 0 else -2, -1, -4), (2, 10, 8), **fur, fringe=2, faces={
            ('west' if sx > 0 else 'east'): mc('blush', clusters=0.2),
        })
    # the trunk: four segments hanging from the nose, curling forward at the tip
    trunk = head
    widths = [(8, 6, 7), (7, 5, 6), (6, 5, 5), (5, 4, 4)]
    curl = [(-0.1, 0), (-0.2, 0), (-0.35, 0), (-0.6, 0)]
    pivot = (0, -5, -14)
    for i, (w, h, d) in enumerate(widths):
        seg = trunk.part(f'trunk_{i}', pivot=pivot, rot=(curl[i][0], 0, 0))
        ring_map = ['.' * (w * 2)] * (h * 2 - 2) + ['R' * (w * 2), 'r' * (w * 2)]
        faces = {f: mc('trunk', clusters=0.15, hd=True, map=ring_map, keys={'R': 'trunk_d', 'r': 'ring'}) for f in ('north', 'south', 'east', 'west')}
        if i == 3:
            faces['down'] = mc('trunk_d', clusters=0.0, rim=False, hd=True, map=['..rrrrrr..', '.rRRRRRRr.', 'rRhhRRhhRr', 'rRhhRRhhRr', '.rRRRRRRr.',
                                                                                 '..rrrrrr..', '..........', '..........'],
                               keys={'r': 'ring', 'R': 'trunk_l', 'h': 'hole'})
        seg.cube((-w / 2, 0, -d / 2), (w, h, d), **mc('trunk', clusters=0.15), faces=faces)
        trunk = seg
        pivot = (0, h, 0)

    # ---- legs: four pillars ending in broad toed pads
    for name, x, z in (('front_left', 1, -1), ('front_right', -1, -1), ('back_left', 1, 1), ('back_right', -1, 1)):
        leg = body.part(f'{name}_leg', pivot=(11 * x, -2, 10 * z))
        leg.cube((-5, 0, -5), (10, 11, 10), **fur, fringe=2, fringe_phase=3 if x > 0 else 0)
        foot = leg.part(f'{name}_foot', pivot=(0, 9, 0))
        foot.cube((-5.5, 0, -5.5), (11, 4, 11), **mc('skin_d', clusters=0.3), faces={
            'north': mc('skin_d', clusters=0.2, hd=True, map=['......................'] * 3 + ['.NN...NNN...NNN...NN..', 'NNNN.NNNNN.NNNNN.NNNN.',
                                                                                            'NnnN.NnnnN.NnnnN.NnnN.', '......................',
                                                                                            '......................'],
                        keys={'N': 'nail', 'n': 'nail_d'}),
            'down': mc('skin_d', clusters=0.0),
        })
    return m


# =========================================================================== SKY WHALE
def whale_eye(expr, mirror):
    base = eye(8, 8, expr, mirror, pupil='dot', rim=0.72)
    return base


def sky_whale() -> Model:
    """The Sky Whale: a vast, shaggy flying whale-bull. A deep barrel body with a cream belly and
    glowing freckles, a broad bull head with a pink snout and a ring of light through the nose,
    great curling horns and floppy ears, fluffy fur on the brow, cheeks and back, flipper-wings and
    wide tail flukes."""
    pal = {
        'hide': '#7f9ae0', 'hide_l': '#a3bbf2', 'hide_d': '#6580c8', 'belly': '#f3ead8', 'belly_l': '#fffaf0', 'belly_d': '#d9cbb0',
        'fur': '#ddd0f5', 'fur_l': '#f6f0ff', 'fur_d': '#a996d8', 'horn': '#f2e2b0', 'horn_l': '#fff4d0', 'horn_d': '#c4a868',
        'tip': '#6a5a8a', 'tip_l': '#8a7aaa', 'snout': '#eaa4bf', 'snout_l': '#f8c4d6', 'snout_d': '#c97a9c', 'nostril': '#5a2f4a',
        'glow': '#bff8ff', 'glow2': '#ffd6f5', 'eye': '#1a1830', 'eye_hi': '#ffffff', 'iris': '#3f5fb8', 'iris_d': '#2a3f88',
        'lid': '#6f8ad4', 'lid_d': '#3f4f8a', 'mouth': '#4a2a5a', 'tongue': '#f59ab8', 'fin': '#90a9ea', 'fin_l': '#b9cbf7', 'fin_d': '#6a80cc',
        'ring': '#ffe08a', 'ear_in': '#f0b8d0',
    }
    m = Model('sky_whale', (256, 256), pal, {'sky_whale': {}}, res=2, expressions=EXPRS)
    hide = mc('hide', clusters=0.2)
    fur = mc('fur', clusters=0.25, streaks=0.45)
    freckles = []
    for j in range(26):
        row = ''
        for i in range(44):
            row += 'g' if (i * 5 + j * 11) % 19 == 0 and 3 < j < 14 else ('G' if (i * 7 + j * 3) % 37 == 0 and 2 < j < 13 else '.')
        freckles.append(row)
    body = m.part('body', pivot=(0, 4, 0))
    body.cube((-16, -14, -22), (32, 26, 44), **hide, faces={
        'down': mc('belly', clusters=0.25),
        'west': mc('hide', clusters=0.2, bands=[(16, 'belly')], map=freckles, keys={'g': 'glow', 'G': 'glow2'}, glow_keys='gG'),
        'east': mc('hide', clusters=0.2, bands=[(16, 'belly')], map=[r[::-1] for r in freckles], keys={'g': 'glow', 'G': 'glow2'}, glow_keys='gG'),
        'north': mc('hide', clusters=0.2, bands=[(16, 'belly')]),
        'south': mc('hide', clusters=0.2, bands=[(16, 'belly')]),
    })
    # throat grooves under the chin, like a real whale's
    body.cube((-13, 12, -20), (26, 2, 30), **mc('belly', clusters=0.0, rim=False), faces={
        'down': mc('belly', clusters=0.0, rim=False, map=[''.join('d' if i % 4 == 1 else '.' for i in range(26))] * 30, keys={'d': 'belly_d'})})
    # big fluffy cushions of fur along the back
    for i, (z, w, h, d) in enumerate(((-14, 20, 6, 13), (0, 22, 7, 13), (13, 16, 6, 11))):
        tuft = body.part(f'back_tuft_{i}', pivot=(0, -14, z))
        tuft.cube((-w / 2, -h + 1, -d / 2), (w, h, d), **fur, fringe=2, fringe_phase=i * 2)
        tuft.cube((-w / 2 + 3 + i, -h - 2, -d / 2 + 2), (w / 2, 3, d / 2), **fur, fringe=1)
        tuft.cube((1 - i, -h - 1, -1), (w / 2 - 3, 2, d / 2), **fur, fringe=1)
    # ---- head
    head = body.part('head', pivot=(0, -1, -22))
    head.cube((-15, -12, -15), (30, 24, 15), **hide, faces={
        'down': mc('belly', clusters=0.2),
        'north': mc('hide', clusters=0.15, bands=[(15, 'belly')]),
        'west': mc('hide', clusters=0.2, bands=[(15, 'belly')]),
        'east': mc('hide', clusters=0.2, bands=[(15, 'belly')]),
    })
    # forehead mop between the horns
    head.cube((-10, -16, -14), (20, 6, 12), **fur, fringe=2, faces={'north': mc('fur', clusters=0.25, streaks=0.45, fringe=3)})
    # the bull snout: a broad pink muzzle with flared nostrils and a ring of light
    snout = head.part('snout', pivot=(0, 3, -15))
    nostril = ['..nnnn..', '.nNNNNn.', 'nNNNNNNn', '.nNNNNn.', '..nnnn..']
    snout.cube((-10, -5, -6), (20, 10, 6), **mc('snout', clusters=0.12), faces={
        'north': mc('snout', clusters=0.0, hd=True, map=put(put(put(blank(40, 20), 5, 4, nostril), 27, 4, nostril), 4, 14, ['l' * 32]),
                    keys={'n': 'snout_d', 'N': 'nostril', 'l': 'snout_d'}),
        'up': mc('snout_l', clusters=0.12),
    })

    def hoop(x, y, u, v):
        return 'R' if 0.55 < u * u + v * v <= 1.0 and v > -0.85 else '_'
    ring = snout.part('nose_ring', pivot=(0, 1, -6), rot=(0.15, 0, 0))
    ring.cube((-3, 0, -0.5), (6, 6, 1), **mc('ring', clusters=0.0, rim=False, glow=True), faces={
        'north': mc('ring', clusters=0.0, rim=False, glow=True, hd=True, map=rows_of(12, 12, hoop), keys={'R': 'ring'}),
        'south': mc('ring', clusters=0.0, rim=False, glow=True, hd=True, map=rows_of(12, 12, hoop), keys={'R': 'ring'}),
    })
    jaw = head.part('jaw', pivot=(0, 8, 0))
    jaw.cube((-14, 0, -19), (28, 5, 19), **mc('belly', clusters=0.2), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, map=put(blank(56, 38), 14, 8, [
            '....TTTTTTTTTTTTTTTTTTTT....', '..TTTTTTTTTTTTTTTTTTTTTTTT..', '.TTTTTTTTTTTTTTTTTTTTTTTTTT.', 'TTTTTTTTTTTTTTTTTTTTTTTTTTTT',
            'TTTTTTTTTTTTTTTTTTTTTTTTTTTT', '.TTTTTTTTTTTTTTTTTTTTTTTTTT.', '..TTTTTTTTTTTTTTTTTTTTTTTT..', '....TTTTTTTTTTTTTTTTTTTT....']),
            keys={'T': 'tongue'}),
        'west': mc('hide', clusters=0.2, bands=[(2, 'belly')]),
        'east': mc('hide', clusters=0.2, bands=[(2, 'belly')]),
    })
    jaw.cube((-7, 5, -17), (14, 4, 10), **fur, fringe=2)
    for side, sx in (('left', 1), ('right', -1)):
        mir = sx < 0
        ek = {'r': 'eye', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}
        e = head.part(f'{side}_eye', pivot=(15 * sx, -3, -9))
        e.cube((-1, -4, -4), (2, 8, 8), **mc('hide', clusters=0.0), faces={
            ('east' if sx > 0 else 'west'): mc('hide', clusters=0.0, hd=True, map=eye(16, 16, '', not mir, pupil='dot', rim=0.8), keys=ek,
                                                 expr=eye_exprs(16, 16, not mir, pupil='dot', rim=0.8)),
        })
        cheek = head.part(f'{side}_cheek', pivot=(14 * sx, 5, -8), rot=(0, 0, -0.2 * sx))
        cheek.cube((0 if sx > 0 else -5, -5, -6), (5, 10, 12), **fur, fringe=2)
        ear = head.part(f'{side}_ear', pivot=(15 * sx, -7, -5), rot=(0, 0, 0.5 * sx))
        ear.cube((0 if sx > 0 else -7, -1, -2.5), (7, 2, 5), **mc('hide', clusters=0.1), faces={'down': mc('ear_in', clusters=0.1)})
        # bull horns: out sideways, then sweeping up and forward
        horn = head.part(f'{side}_horn', pivot=(12 * sx, -12, -7), rot=(0, 0.2 * sx, -0.25 * sx))
        horn.cube((0 if sx > 0 else -8, -2.5, -2.5), (8, 5, 5), **mc('horn', clusters=0.2))
        tip = horn.part(f'{side}_horn_tip', pivot=(7.5 * sx, 0, 0), rot=(-0.3, 0, -0.9 * sx))
        tip.cube((0 if sx > 0 else -7, -2, -2), (7, 4, 4), **mc('horn', clusters=0.15))
        tip2 = tip.part(f'{side}_horn_point', pivot=(6.5 * sx, 0, 0), rot=(-0.2, 0, -0.7 * sx))
        tip2.cube((0 if sx > 0 else -5, -1.5, -1.5), (5, 3, 3), **mc('tip', clusters=0.1))
        # flipper-wings
        fl = body.part(f'{side}_flipper', pivot=(16 * sx, 6, -10), rot=(0, 0, 0.3 * sx))
        fl.cube((0 if sx > 0 else -20, -1, -7), (20, 2, 15), **mc('fin', clusters=0.2), faces={
            'up': mc('fin', clusters=0.2, ribs=5, accent='fin_d', alpha='membrane', edge='outer', edge_depth=3, scallop=4),
            'down': mc('belly', clusters=0.2, alpha='membrane', edge='outer', edge_depth=3, scallop=4),
        })
        fl.cube((0 if sx > 0 else -7, -2.5, -6), (7, 4, 12), **fur, fringe=1)
    # ---- tail and flukes
    t1 = body.part('tail1', pivot=(0, -1, 22))
    t1.cube((-12, -10, 0), (24, 20, 12), **hide, faces={'down': mc('belly', clusters=0.25),
                                                         'west': mc('hide', clusters=0.2, bands=[(13, 'belly')]),
                                                         'east': mc('hide', clusters=0.2, bands=[(13, 'belly')])})
    t1.cube((-6, -15, 0), (12, 6, 10), **fur, fringe=2)
    t2 = t1.part('tail2', pivot=(0, 0, 12))
    t2.cube((-8, -7, 0), (16, 14, 10), **hide, faces={'down': mc('belly', clusters=0.25),
                                                       'west': mc('hide', clusters=0.2, bands=[(9, 'belly')]),
                                                       'east': mc('hide', clusters=0.2, bands=[(9, 'belly')])})
    t3 = t2.part('tail3', pivot=(0, 0, 10))
    t3.cube((-5, -4, 0), (10, 8, 8), **hide, faces={'down': mc('belly', clusters=0.25)})
    fluke = t3.part('flukes', pivot=(0, 0, 7))
    fluke.cube((-21, -1.5, -2), (42, 3, 15), **mc('fin', clusters=0.2), faces={
        'up': mc('fin', clusters=0.2, ribs=5, accent='fin_d', alpha='membrane', edge='bottom', edge_depth=3, scallop=7),
        'down': mc('belly', clusters=0.2, alpha='membrane', edge='bottom', edge_depth=3, scallop=7),
    })
    fluke.cube((-4, -4, -1), (8, 6, 7), **fur, fringe=1)
    return m


# =========================================================================== FISH
def fanfare_eel() -> Model:
    """Fanfare Eel: a long, brass-orange predator eel. Its head ends in a flared trumpet bell instead
    of a mouth; five tapering segments with a ragged dorsal frill undulate behind it."""
    pal = {
        'brass': '#e0882c', 'brass_l': '#ffb85a', 'brass_d': '#a3521c', 'belly': '#ffd98a', 'belly_l': '#fff0c0', 'belly_d': '#e0a85a',
        'stripe': '#2fb7b0', 'bell': '#ffc94a', 'bell_l': '#fff0a0', 'bell_d': '#c47a1c', 'throat': '#2a1020', 'throat_d': '#140810',
        'fin': '#ff6a3d', 'fin_d': '#c23a1c', 'eye': '#1a0a10', 'iris': '#ff3b4a', 'iris_d': '#b01a2a', 'eye_hi': '#ffffff',
        'lid': '#c0661e', 'lid_d': '#6a2a10', 'tooth': '#fff8e0',
    }
    m = Model('fanfare_eel', (128, 64), pal, {'fanfare_eel': {}}, res=2, expressions=['blink', 'angry', 'hurt', 'dead'])
    head = m.part('head', pivot=(0, 20, -6))
    ek = {'r': 'brass_d', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}
    head.cube((-2.5, -2.5, -6), (5, 5, 6), **mc('brass', clusters=0.3, bands=[(3, 'belly')]), faces={
        'east': mc('brass', clusters=0.2, bands=[(3, 'belly')], hd=True, map=put(blank(12, 10), 1, 1, eye(5, 5, '', True, pupil='slit', rim=0.7)), keys=ek,
                   expr={e: put(blank(12, 10), 1, 1, eye(5, 5, e, True, pupil='slit', rim=0.7)) for e in ['blink', 'angry', 'hurt', 'dead']}),
        'west': mc('brass', clusters=0.2, bands=[(3, 'belly')], hd=True, map=put(blank(12, 10), 6, 1, eye(5, 5, '', False, pupil='slit', rim=0.7)), keys=ek,
                   expr={e: put(blank(12, 10), 6, 1, eye(5, 5, e, False, pupil='slit', rim=0.7)) for e in ['blink', 'angry', 'hurt', 'dead']}),
        'up': mc('brass', clusters=0.2, hd=True, map=['..ssssss..'] + ['..........'] * 11, keys={'s': 'stripe'}),
        'down': mc('belly', clusters=0.2),
    })
    # the trumpet: a short pipe flaring into a bell
    pipe = head.part('pipe', pivot=(0, 0.5, -6))
    pipe.cube((-1.5, -1.5, -3), (3, 3, 3), **mc('bell', clusters=0.15, rim=False))
    bell = pipe.part('bell', pivot=(0, 0, -3))
    bell.cube((-4, -4, -2), (8, 8, 2), **mc('bell', clusters=0.1, rim=False), faces={
        'north': mc('bell', clusters=0.0, rim=False, hd=True, map=rows_of(16, 16, lambda x, y, u, v: (
            '_' if u * u + v * v > 1.0 else 'B' if u * u + v * v > 0.8 else 'L' if u * u + v * v > 0.62 else
            't' if (u * u + v * v > 0.5 and (x + y) % 3 == 0) else 'T' if u * u + v * v < 0.18 else 'D')),
            keys={'B': 'bell_d', 'L': 'bell_l', 'D': 'throat', 'T': 'throat_d', 't': 'tooth'}),
        'south': mc('bell_d', clusters=0.0, rim=False),
    })
    fin = head.part('crest', pivot=(0, -2.5, -3))
    fin.cube((0, -3, -2), (0, 3, 5), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='bottom'))
    prev = head
    dims = [(4.5, 4.5, 7), (4, 4, 7), (3.5, 3.5, 6), (3, 3, 6), (2, 2, 5)]
    pivot = (0, 0, 0)
    for i, (w, h, d) in enumerate(dims):
        seg = (head if i == 0 else prev).part(f'segment_{i}', pivot=pivot if i else (0, 0, 0))
        stripe_map = ['.' * int(w * 2)] * int(h * 2)
        seg.cube((-w / 2, -h / 2, 0), (w, h, d), **mc('brass', clusters=0.3, bands=[(int(h / 2) + 0, 'belly')]), faces={
            'up': mc('brass', clusters=0.25, hd=True, map=[('ss' if (k // 3) % 2 == 0 else '..').center(int(w * 2), '.') for k in range(int(d * 2))],
                     keys={'s': 'stripe'}),
            'down': mc('belly', clusters=0.2),
        })
        seg.cube((0, -h / 2 - 2.5, 0.5), (0, 2.5, d - 1), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='bottom'))
        if i == len(dims) - 1:
            seg.cube((0, -3.5, d - 0.5), (0, 7, 5), **mc('fin', clusters=0.0, rim=False, ribs=2, accent='fin_d', alpha='membrane', edge='outer',
                                                         edge_depth=1, scallop=2))
        prev = seg
        pivot = (0, 0, d)
    for side, sx in (('left', 1), ('right', -1)):
        pf = head.part(f'{side}_fin', pivot=(2.5 * sx, 1.5, -1), rot=(0, 0.4 * sx, 0.5 * sx))
        pf.cube((0 if sx > 0 else -3, 0, -1), (3, 0, 3), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_d'))
    return m


def kazoo_fish() -> Model:
    """Kazoo Fish: a little teal and orange schooling fish with a kazoo for a snout (resonator bump
    and all) and mismatched googly eyes."""
    pal = {
        'teal': '#2fb7b0', 'teal_l': '#5fe0d0', 'teal_d': '#1f8a8a', 'belly': '#c8fff0', 'belly_d': '#8fd8c8', 'orange': '#ff9a4a',
        'orange_l': '#ffc890', 'orange_d': '#e8702c', 'kazoo': '#ff8a3d', 'kazoo_l': '#ffc890', 'kazoo_d': '#c95a1c', 'cap': '#ffe07a',
        'hole': '#3a1a10', 'eye': '#101820', 'eye_w': '#ffffff', 'eye_hi': '#ffffff', 'lid': '#1f8a8a', 'lid_d': '#0f5a5a', 'spot': '#ffd06a',
    }
    m = Model('kazoo_fish', (64, 32), pal, {'kazoo_fish': {}}, res=2, expressions=['blink', 'hurt', 'dead'])
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-1.5, -2.5, -3), (3, 5, 6), **mc('teal', clusters=0.3, bands=[(3, 'belly')], spots=0.4, accent='spot'), faces={
        'up': mc('teal', clusters=0.2, hd=True, map=['......', '..oo..', '......', '..oo..', '......', '..oo..'] * 2, keys={'o': 'orange'}),
        'down': mc('belly', clusters=0.2),
    })
    ek = {'r': 'eye', 'i': 'eye_w', 'I': 'eye_w', 'p': 'eye', 'h': 'eye_w', 'l': 'lid', 'd': 'lid_d'}
    # odd eyes: a big googly one on the left, a little one on the right
    for side, sx, size in (('left', 1, 3), ('right', -1, 2)):
        e = body.part(f'{side}_eye', pivot=(1.5 * sx, -1.5, -2))
        e.cube((0 if sx > 0 else -1, -size / 2, -size / 2), (1, size, size), **mc('eye_w', clusters=0.0, rim=False), faces={
            ('east' if sx > 0 else 'west'): mc('eye_w', clusters=0.0, rim=False, hd=True, map=eye(size * 2, size * 2, '', sx < 0, pupil='dot', rim=0.75, shine=False),
                                                 keys=ek, expr={x: eye(size * 2, size * 2, x, sx < 0, pupil='dot', rim=0.75, shine=False)
                                                                for x in ['blink', 'hurt', 'dead']}),
            'north': mc('eye_w', clusters=0.0, rim=False),
        })
    snout = body.part('kazoo', pivot=(0, 0.5, -3))
    snout.cube((-1, -1, -4), (2, 2, 4), **mc('kazoo', clusters=0.15, rim=False), faces={
        'north': mc('kazoo', clusters=0.0, rim=False, hd=True, map=['.hh.', 'hhhh', 'hhhh', '.hh.'], keys={'h': 'hole'}),
    })
    snout.cube((-0.75, -2, -3), (1.5, 1, 1.5), **mc('cap', clusters=0.0, rim=False))
    dorsal = body.part('dorsal', pivot=(0, -2.5, -1))
    dorsal.cube((0, -2, 0), (0, 2, 4), **mc('orange', clusters=0.0, rim=False, ribs=2, accent='orange_l', alpha='membrane', edge='bottom'))
    tail = body.part('tail', pivot=(0, 0, 3))
    tail.cube((0, -3, 0), (0, 6, 4), **mc('orange', clusters=0.0, rim=False, ribs=2, accent='orange_l', alpha='membrane', edge='outer', edge_depth=1,
                                          scallop=2))
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(1.5 * sx, 1.5, -0.5), rot=(0, 0, 0.6 * sx))
        pf.cube((0 if sx > 0 else -2, 0, 0), (2, 0, 2), **mc('orange', clusters=0.0, rim=False))
    return m


def tubafish() -> Model:
    """Tubafish: a huge, round brass pufferfish. A tuba bell sits on its back like a blowhole, a
    pursed mouthpiece pouts at the front, and when it panics it swells up and its spikes stand out."""
    pal = {
        'brass': '#e8b33a', 'brass_l': '#ffe08a', 'brass_d': '#a8701c', 'belly': '#fff2c8', 'belly_l': '#fffbe8', 'belly_d': '#e8d29a',
        'spot': '#f4c45a', 'bell': '#ffd24a', 'bell_l': '#fff4b0', 'bell_d': '#b07a1c', 'throat': '#3a2010', 'spike': '#fff0c0', 'spike_d': '#c9a35a',
        'lip': '#ff9a6a', 'lip_d': '#c95a3a', 'eye': '#1a1420', 'iris': '#5ad8ff', 'iris_d': '#2a8ac8', 'eye_hi': '#ffffff', 'lid': '#d8a032',
        'lid_d': '#8a5a1c', 'fin': '#ffcf6a', 'fin_d': '#c48a2c', 'valve': '#c9c9d8', 'valve_l': '#f0f0ff',
    }
    m = Model('tubafish', (128, 128), pal, {'tubafish': {}}, res=2, expressions=EXPRS)
    body = m.part('body', pivot=(0, 17, 0))
    ek = {'r': 'brass_d', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}

    def front(expr):
        g = blank(24, 22)
        g = put(g, 1, 3, eye(8, 8, expr, False, pupil='dot', rim=0.7))
        g = put(g, 15, 3, eye(8, 8, expr, True, pupil='dot', rim=0.7))
        return g
    body.cube((-6, -6, -6), (12, 11, 12), **mc('brass', clusters=0.3, spots=0.35, accent='spot', bands=[(7, 'belly')]), faces={
        'north': mc('brass', clusters=0.2, bands=[(7, 'belly')], hd=True, map=front(''), keys=ek, expr={e: front(e) for e in EXPRS}),
        'down': mc('belly', clusters=0.2),
    })
    mouth = body.part('mouth', pivot=(0, 1.5, -6))
    mouth.cube((-2, -1.5, -2), (4, 3, 2), **mc('lip', clusters=0.0, rim=False), faces={
        'north': mc('lip', clusters=0.0, rim=False, hd=True, map=['..llll..', '.lLLLLl.', '.lLooLl.', '.lLLLLl.', '..llll..', '........'],
                    keys={'l': 'lip_d', 'L': 'lip', 'o': 'throat'}),
    })
    tuba = body.part('tuba', pivot=(0, -6, 1))
    tuba.cube((-1.5, -3, -1.5), (3, 3, 3), **mc('bell', clusters=0.1, rim=False))
    tbell = tuba.part('tuba_bell', pivot=(0, -3, 0), rot=(0.2, 0, 0))
    tbell.cube((-3.5, -3, -3.5), (7, 3, 7), **mc('bell', clusters=0.1, rim=False), faces={
        'up': mc('bell', clusters=0.0, rim=False, hd=True, map=rows_of(14, 14, lambda x, y, u, v: (
            '_' if u * u + v * v > 1.0 else 'B' if u * u + v * v > 0.8 else 'L' if u * u + v * v > 0.6 else 'D')), keys={'B': 'bell_d', 'L': 'bell_l', 'D': 'throat'}),
    })
    for i, vx in enumerate((-3.5, 3.5)):
        valve = body.part(f'valve_{i}', pivot=(vx, -6, -2.5))
        valve.cube((-0.75, -2, -0.75), (1.5, 2, 1.5), **mc('valve', clusters=0.0, rim=False), faces={'up': mc('valve_l', clusters=0.0)})
    # the spikes: folded flat until it puffs up
    k = 0
    for (px, py, pz, rx, rz) in ((0, -6, -3, -0.6, 0), (-6, -1, 0, 0, 1.2), (6, -1, 0, 0, -1.2), (0, -1, 6, 0.9, 0), (-4.5, -5, 4, 0.6, 0.7),
                                 (4.5, -5, 4, 0.6, -0.7), (-4.5, 3, -4, -1.2, 0.6), (4.5, 3, -4, -1.2, -0.6), (-6, -4, -3, -0.4, 1.0),
                                 (6, -4, -3, -0.4, -1.0), (0, 5, 0, 3.14, 0), (-4, 4, 4, 2.2, 0.6), (4, 4, 4, 2.2, -0.6)):
        sp = body.part(f'spike_{k}', pivot=(px, py, pz), rot=(rx, 0, rz))
        sp.cube((-0.5, -3, -0.5), (1, 3, 1), **mc('spike', clusters=0.0, rim=False), faces={'up': mc('spike_d', clusters=0.0)})
        k += 1
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(6 * sx, 0, -1), rot=(0, 0.3 * sx, 0))
        pf.cube((0 if sx > 0 else -3, -2, 0), (3, 4, 0), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='outer'))
    tail = body.part('tail', pivot=(0, -0.5, 6))
    tail.cube((0, -3, 0), (0, 6, 4), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='outer', edge_depth=1, scallop=2))
    return m


ALL = {'stomper': stomper, 'sky_whale': sky_whale, 'fanfare_eel': fanfare_eel, 'kazoo_fish': kazoo_fish, 'tubafish': tubafish}
