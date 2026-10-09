"""CR2: the Sky Whale, remade as a real whale (geometry and paint; SkyWhaleModel animates it).

A humpback of the sky: a long body tapering into a muscular tail stock and broad, notched flukes;
a big head with a knobbly rostrum, a blowhole behind it and a long mouth line curving up to a small
kind eye; pleated grooves from chin to navel that balloon out when it sings; and very long white
pectoral fins with a scalloped leading edge. Deep periwinkle above, sky blue down the flanks, cloud
white underneath; a constellation of star freckles glows along its back and the trailing edges of
its fins, and every fluke carries its own dark pattern underneath, like a real humpback's.

Hooks (one line each): mobs_wild.sky_whale (the model) and items16.all_items (its spawn egg)."""
import math
import random

from modelkit import Model

EXPRS = ['blink', 'happy', 'angry', 'hurt', 'dead']

PALETTE = {
    # S1 land: an airy sky palette - sky blue back, pale periwinkle flanks, cloud-white belly, glowing runes
    'hide': '#6f9ae0', 'hide_l': '#8fb6f0', 'hide_d': '#5579c4', 'flank': '#a8c8f4', 'flank_l': '#c8defa', 'flank_d': '#8aaee8',
    'belly': '#f2f5ff', 'belly_l': '#ffffff', 'belly_d': '#d2dcf4', 'groove': '#bcc8ec',
    'spot': '#e8f0ff', 'knob': '#f4f8ff', 'knob_d': '#c4d2f0', 'star': '#fff3b0', 'star2': '#c8faff',
    'mouth': '#3e2a52', 'tongue': '#e98fb2', 'baleen': '#eef0fa', 'baleen_d': '#b8c2e0',
    'eye': '#1a1c3a', 'iris': '#7ff0ff', 'iris_d': '#3aa8e0', 'eye_hi': '#ffffff', 'lid': '#5579c4', 'lid_d': '#2c3c74',
    'hole': '#24305e', 'fin': '#86aef0', 'fin_l': '#a9c8f6', 'fin_d': '#6488d2', 'ink': '#3a4c8e',
    'cloud': '#f3f7ff', 'cloud_l': '#ffffff', 'cloud_d': '#cbd8f4', 'veil': '#cfe0ff', 'veil_l': '#e8f2ff', 'veil_d': '#a8c2f0',
    'rune': '#aef8ff', 'rune2': '#ffe9a8',
}


def _rows(w, h, fn):
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def stars(w, h, seed, density=0.005):
    """Star freckles (hd map): single glowing points and a few small crosses, scattered like a
    constellation."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(max(1, int(w * h * density))):
        x, y = rnd.randrange(1, w - 1), rnd.randrange(1, h - 1)
        if rnd.random() < 0.3:
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                g[y + dy][x + dx] = 'S' if (dx, dy) == (0, 0) else 's'
        else:
            g[y][x] = 'S' if rnd.random() < 0.5 else 's'
    return [''.join(r) for r in g]


def spots(w, h, seed, density=0.02):
    """Pale barnacle spots and scars (hd map)."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(max(1, int(w * h * density))):
        x, y = rnd.randrange(w), rnd.randrange(h)
        for dx, dy in ((0, 0), (1, 0), (0, 1))[:rnd.randrange(1, 4)]:
            if x + dx < w and y + dy < h:
                g[y + dy][x + dx] = 'o'
    return [''.join(r) for r in g]


def overlay(*maps):
    out = [list(r) for r in maps[0]]
    for m in maps[1:]:
        for y, r in enumerate(m):
            for x, ch in enumerate(r):
                if ch != '.' and y < len(out) and x < len(out[0]):
                    out[y][x] = ch
    return [''.join(r) for r in out]


def grooves(w, h, step=3):
    """Throat pleats (hd map) on a down face: grooves running from the chin back to the navel
    (texture columns are across the body, rows along it)."""
    def fn(x, y):
        k = x % step
        if k == 0:
            return 'g'
        if k == step - 1:
            return 'l'
        return '.'
    return _rows(w, h, fn)


def side_pleats(w, h, top):
    """The grooves curving up the side of the throat (hd map), below row `top`."""
    return _rows(w, h, lambda x, y: 'g' if y >= top and (x + y // 2) % 4 == 0 else '.')


def eye_map(n, expr=''):
    """A small whale eye, the way vanilla's dolphin wears it: a few dark pixels in the middle of an
    n x n face, one of them the deep blue of the iris; the expressions only move the lid."""
    c = n // 2 - 2
    eyes = {
        '': ['rrr', 'rir'], 'blink': ['...', 'ddd'], 'sleep': ['...', 'ddd'], 'happy': ['d.d', '.d.'], 'hurt': ['ddd', '...'],
        'angry': ['dd.', 'rir'], 'dead': ['r.r', '.r.'],
    }
    rows = [['.'] * n for _ in range(n)]
    for y, r in enumerate(eyes.get(expr, eyes[''])):
        for x, ch in enumerate(r):
            rows[c + 1 + y][c + x + 1] = ch
    return [''.join(r) for r in rows]


EYE_KEYS = {'r': 'eye', 'i': 'iris_d', 'd': 'lid_d'}
STAR_KEYS = {'S': 'star', 's': 'star2', 'o': 'spot', 'g': 'groove', 'l': 'belly_l', 'k': 'knob', 'K': 'knob_d', 'i': 'ink'}


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def countershade(size, bands, seed, star=True, flank='flank'):
    """A side face shaded dark above, sky blue below and white at the belly (bands: unit rows where
    the flank and the belly begin), with star freckles high on it and pale spots lower down."""
    w, h = int(math.ceil(size[2])) * 2, int(math.ceil(size[1])) * 2
    maps = [spots(w, h, seed + 1, 0.005)]
    if star:
        st = stars(w, h, seed, 0.004)
        maps.append([r if y < bands[0] * 2 else '.' * w for y, r in enumerate(st)])
    return mc('hide', clusters=0.25, bands=[(bands[0], flank), (bands[1], 'belly')], hd=True, map=overlay(*maps), keys=STAR_KEYS,
              glow_keys='Ss')


RUNE_KEYS = dict(STAR_KEYS, R='rune', r='rune2')
CLOUD = dict(color='cloud', pattern='mc', clusters=0.7, noise=0.8, material='jelly')


def cloud(part, x, y, z, w, h, d, seed=0):
    """A lumpy cloud: a soft base with two smaller billows on top, each with a ragged underside."""
    part.cube((x, y, z), (w, h, d), **CLOUD, fringe=1, fringe_phase=seed, faces={'down': dict(color='cloud_d', pattern='mc', clusters=0.4)})
    part.cube((x + w * 0.15, y - h * 0.45, z + d * 0.2), (w * 0.5, h * 0.6, d * 0.55), **CLOUD, faces={'down': dict(skip=True)})
    part.cube((x + w * 0.5, y - h * 0.3, z + d * 0.35), (w * 0.38, h * 0.45, d * 0.5), **CLOUD, faces={'down': dict(skip=True)})


def swirls(w, h, seed, lines=2, dots=0.004):
    """Soft glowing magical markings (hd map): long flowing wave lines that curl at their ends,
    beaded with gold, over a dusting of star freckles."""
    rnd = random.Random(seed)
    g = [list(r) for r in stars(w, h, seed + 5, dots)]
    for k in range(lines):
        y0 = h * (0.2 + 0.6 * (k + 0.5) / lines) + rnd.uniform(-1.5, 1.5)
        amp, f, ph = rnd.uniform(1.0, 2.6), rnd.uniform(0.12, 0.22), rnd.uniform(0, 6.3)
        x0, x1 = int(w * rnd.uniform(0.0, 0.2)), int(w * rnd.uniform(0.75, 1.0))
        for x in range(x0, x1):
            y = int(round(y0 + amp * math.sin(x * f + ph)))
            if 0 <= y < h:
                g[y][x] = 'r' if (x - x0) % 9 == 4 else 'R'
        # a curl at the end of the line
        ex, ey = x1 - 1, int(round(y0 + amp * math.sin((x1 - 1) * f + ph)))
        for a in range(0, 300, 30):
            cx, cy = int(round(ex + 1.6 * math.cos(math.radians(a)))), int(round(ey - 1.6 + 1.6 * math.sin(math.radians(a))))
            if 0 <= cx < w and 0 <= cy < h:
                g[cy][cx] = 'R'
    return [''.join(r) for r in g]


def eye_glyph(n, expr):
    """A round, glowing sky-whale eye on an n x n plate: a dark lid ring, a glowing cyan iris, a deep
    pupil and a white glint (the glint and iris glow). Expressions move the lid."""
    c = (n - 1) / 2
    r = n / 2 - 0.3

    def px(x, y):
        d = math.hypot(x - c, y - c)
        if d > r:
            return '.'
        if expr in ('blink', 'sleep'):
            return 'd' if abs(y - c - 0.5) < 0.7 else ('l' if y < c else '.')
        if expr == 'happy':
            return 'd' if abs(d - r * 0.7) < 0.6 and y < c + 0.2 else ('l' if y < c else '.')
        if expr == 'dead':
            return 'd' if abs(abs(x - c) - abs(y - c)) < 0.7 else 'l'
        if expr == 'hurt':
            return 'd' if abs(y - c) < 0.6 or (abs(y - c) < 1.6 and abs(x - c) > r * 0.6) else 'l'
        if expr == 'angry' and y < c - (x - c) * 0.6 - 0.5:
            return 'l'
        if d > r - 0.9:
            return 'd'
        if x - c < -r * 0.25 and y - c < -r * 0.25 and d > r * 0.25:
            return 'w'
        if d < r * 0.35:
            return 'p'
        return 'I' if y < c else 'i'
    return _rows(n, n, px)


def wing(parent, name, side, sx, pivot, rot, size, tip_size, seed, cloud=True):
    """One of the six fins: a thin flowing blade angled off the body and a trailing tip, both cut to a
    scalloped trailing edge, veined with glowing runes, with a puff of cloud along the leading edge."""
    L, T, D = size
    fin = parent.part(name, pivot=pivot, rot=rot)

    def blade(w, d, s):
        tex_w, tex_d = int(math.ceil(w)) * 2, int(math.ceil(d)) * 2

        def edge(x, y):
            depth = int(round(1.2 + 1.2 * math.sin(x * 0.7 + s) + (x % 4 == 0)))
            return '_' if y < depth else ('R' if y == depth + 2 and x % 5 == 2 else '.')
        cut = _rows(tex_w, tex_d, edge)
        veins = _rows(tex_w, tex_d, lambda x, y: 'R' if (x + s) % 7 == 0 and y > tex_d * 0.45 else ('r' if (x + s) % 7 == 0 and y > tex_d * 0.3 else '.'))
        up = overlay(swirls(tex_w, tex_d, s, 1, 0.006), veins, cut)
        if sx < 0:
            up = [r[::-1] for r in up]
        return {'up': mc('fin', clusters=0.25, noise=0.6, hd=True, map=up, keys=RUNE_KEYS, glow_keys='RrSs'),
                'down': mc('veil', clusters=0.2, noise=0.6, hd=True, map=[r.replace('R', '.').replace('r', '.').replace('S', '.').replace('s', '.') for r in up],
                           keys=RUNE_KEYS),
                'north': mc('cloud', clusters=0.2, rim=False), 'south': dict(skip=True)}
    fin.cube((0 if sx > 0 else -L, -T / 2, -D / 2), (L, T, D), **mc('fin', clusters=0.25), faces=blade(L, D, seed))
    tl, tt, td = tip_size
    tip = fin.part(f'{name}_tip', pivot=(L * sx, 0, 0), rot=(0, -0.2 * sx, 0.15 * sx))
    tip.cube((0 if sx > 0 else -tl, -tt / 2, -td / 2), (tl, tt, td), **mc('fin', clusters=0.25), faces=blade(tl, td, seed + 3))
    if cloud:
        # cloud puffs riding the leading edge
        for k in range(3):
            w = 3.0 - k * 0.5
            x = (1.0 + k * L / 3.2)
            fin.cube(((x if sx > 0 else -x - w), -T / 2 - 1.0, -D / 2 - 1.2), (w, 2.0, 2.2), **CLOUD, fringe=1, fringe_phase=k + seed)
        tip.cube(((tl * 0.55 if sx > 0 else -tl * 0.55 - 2.2), -tt / 2 - 0.9, -td / 2 - 1.0), (2.2, 1.6, 1.8), **CLOUD, fringe=1)
    return fin


def sky_whale() -> Model:
    """See the module notes. The parts SkyWhaleModel moves: body, head, jaw, throat, blowhole, the
    six eyes (painted per expression), the six fins (left/right_fin, _fin_2, _fin_3 and their tips),
    tail1-tail4, left/right fluke and the dorsal fin."""
    m = Model('sky_whale', (256, 256), dict(PALETTE), {'sky_whale': {}}, res=2, expressions=EXPRS, detail=2,
              materials={'knob': 'bone', 'knob_d': 'bone', 'groove': 'skin', 'spot': 'skin', 'ink': 'skin', 'veil': 'membrane'})
    top = mc('hide_d', clusters=0.3, noise=0.7, hd=True, keys=RUNE_KEYS, glow_keys='SsRr')

    def flank(size, bands, seed, star=True):
        d = countershade(size, bands, seed, star)
        w, h = int(math.ceil(size[2])) * 2, int(math.ceil(size[1])) * 2
        d['map'] = overlay(d['map'], [r if y < bands[1] * 2 - 2 else '.' * w for y, r in enumerate(swirls(w, h, seed + 300, 2, 0.0))])
        d['keys'], d['glow_keys'], d['noise'] = RUNE_KEYS, 'SsRr', 0.7
        return d

    # ---- the chest: the deepest part of the body, the front fins hang from it
    body = m.part('body', pivot=(0, 6, 0))
    body.cube((-15, -12, -14), (30, 24, 28), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=overlay(stars(60, 56, 3, 0.006), [r if 24 <= x0 else r for x0, r in enumerate(swirls(60, 56, 4, 3, 0.0))])),
        'east': flank((30, 24, 28), (9, 15), 10), 'west': flank((30, 24, 28), (9, 15), 11),
        'down': mc('belly', clusters=0.15, hd=True, map=grooves(60, 56), keys=STAR_KEYS)})
    body.cube((-12, -14, -12), (24, 2, 24), **mc('hide_d', clusters=0.3), faces={'up': dict(top, map=stars(48, 48, 5))})
    for sx in (1, -1):
        body.cube((15 if sx > 0 else -16, -9, -11), (1, 17, 22), **mc('hide', clusters=0.25), faces={
            'east': flank((1, 17, 22), (6, 11), 12), 'west': flank((1, 17, 22), (6, 11), 13)})
    # clouds drifting along its back
    for i, (x, z, w, d, h) in enumerate(((-6, -11, 10, 9, 3.5), (1, -2, 9, 8, 3), (-7, 4, 8, 8, 3.2), (2, 8, 7, 5, 2.5))):
        cloud(body, x, -14 - h + 0.8, z, w * 1.15, h * 1.2, d * 1.1, i)
    # and a skirt of cloud wrapped round its flanks and belly, so it seems to swim through a cloud bank
    for sx in (1, -1):
        for i, (z, w, h, d) in enumerate(((-12, 5, 5, 9), (-3, 6, 6, 10), (7, 5, 5, 8))):
            cloud(body, (13.5 if sx > 0 else -13.5 - w), 6 + (i % 2), z, w, h, d, i + (sx > 0) * 3)
    # ---- the head: cranium, rostrum and the snout's rounded tip; little cloud tufts on the rostrum
    head = body.part('head', pivot=(0, 0, -14))
    head.cube((-13.5, -11, -18), (27, 15, 18), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=overlay(stars(54, 36, 21, 0.004), swirls(54, 36, 22, 2, 0.0))),
        'east': flank((27, 15, 18), (7, 12), 23), 'west': flank((27, 15, 18), (7, 12), 24),
        'down': mc('belly', clusters=0.15)})
    head.cube((-11, -9, -28), (22, 11, 10), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=spots(44, 20, 25, 0.03)),
        'east': flank((22, 11, 10), (5, 9), 26, star=False), 'west': flank((22, 11, 10), (5, 9), 27, star=False),
        'down': mc('belly', clusters=0.15)})
    head.cube((-8, -7, -31), (16, 8, 3), **mc('hide', clusters=0.25), faces={
        'north': mc('hide', clusters=0.2, bands=[(4, 'flank')], hd=True, map=spots(32, 16, 28, 0.03), keys=STAR_KEYS),
        'up': dict(top, map=spots(32, 6, 29, 0.05))})
    rnd = random.Random(7)
    for i in range(7):
        x, z = rnd.uniform(-8.5, 7.0), rnd.uniform(-29.5, -19.5)
        y = -9 if z > -28 else -7
        head.cube((x, y - 1.4, z), (2.0, 1.4, 1.8), **CLOUD, faces={'down': dict(skip=True)})
    blowhole = head.part('blowhole', pivot=(0, -11, -9))
    blowhole.cube((-3.5, -1.5, -2.5), (7, 1.5, 5), **mc('hide_d', clusters=0.2), faces={
        'up': mc('hide_d', clusters=0.0, hd=True, keys={'h': 'hole', 'l': 'hide_l', 'R': 'rune'}, glow_keys='R',
                 map=['..............', '..RRRR..RRRR..', '.RlhhR..RhhlR.', '.Rlhhh..hhhlR.', '.Rlhhh..hhhlR.', '..RlhhRRhhlR..',
                      '...RRRRRRRR...', '..............', '..............', '..............'])})
    bal = ''.join('B' if x % 2 == 0 else 'b' for x in range(52))
    baleen = head.part('baleen', pivot=(0, 4, -1))
    baleen.cube((-12.5, 0, -27), (25, 3, 26), **mc('baleen', clusters=0.0, rim=False), faces={
        'north': mc('baleen', clusters=0.0, rim=False, hd=True, map=[bal] * 4 + [bal.replace('b', '_')] * 2, keys={'B': 'baleen', 'b': 'baleen_d'}),
        'east': mc('baleen', clusters=0.0, rim=False, hd=True, map=[bal] * 4 + [bal.replace('b', '_')] * 2, keys={'B': 'baleen', 'b': 'baleen_d'}),
        'west': mc('baleen', clusters=0.0, rim=False, hd=True, map=[bal] * 4 + [bal.replace('b', '_')] * 2, keys={'B': 'baleen', 'b': 'baleen_d'}),
        'up': dict(skip=True), 'south': dict(skip=True), 'down': dict(skip=True)})
    # six eyes, three a side in a gentle arc, the front one the biggest; each a glowing iris on a plate
    ek = {'d': 'lid_d', 'l': 'lid', 'w': 'eye_hi', 'p': 'eye', 'I': 'iris', 'i': 'iris_d'}
    for side, sx in (('left', 1), ('right', -1)):
        for k, (y, z, n) in enumerate(((-1.0, -11.0, 4), (-4.2, -6.6, 3), (-0.8, -3.4, 2.5))):
            e = head.part(f'{side}_eye' if k == 0 else f'{side}_eye_{k + 1}', pivot=(13.5 * sx, y, z))
            face = 'east' if sx > 0 else 'west'
            tn = int(math.ceil(n)) * 2
            e.cube((0 if sx > 0 else -0.3, -n / 2, -n / 2), (0.3, n, n), **mc('hide', clusters=0.0, rim=False), faces={
                face: mc('hide', clusters=0.0, rim=False, hd=True, map=eye_glyph(tn, ''), keys=ek, glow_keys='wIi',
                         expr={x: eye_glyph(tn, x) for x in EXPRS})})
    # ---- the lower jaw: long and heavy, the mouth line curving up towards the eyes; pleats begin under the chin
    jaw = head.part('jaw', pivot=(0, 4, 0))
    jaw.cube((-13, 0, -30), (26, 6, 30), **mc('belly', clusters=0.15), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, keys={'T': 'tongue', 'm': 'mouth'},
                 map=_rows(52, 60, lambda x, y: 'T' if ((x - 25.5) / 16) ** 2 + ((y - 34) / 20) ** 2 < 1 else '.')),
        'east': mc('hide', clusters=0.2, bands=[(2, 'flank'), (4, 'belly')], hd=True, map=side_pleats(60, 12, 7), keys=STAR_KEYS),
        'west': mc('hide', clusters=0.2, bands=[(2, 'flank'), (4, 'belly')], hd=True, map=side_pleats(60, 12, 7), keys=STAR_KEYS),
        'north': mc('flank', clusters=0.2, bands=[(3, 'belly')]),
        'down': mc('belly', clusters=0.15, hd=True, map=grooves(52, 60), keys=STAR_KEYS)})
    # ---- the throat: the pleated belly that balloons out when it sings
    throat = body.part('throat', pivot=(0, 12, -6))
    throat.cube((-12, -1, -22), (24, 3, 30), **mc('belly', clusters=0.1, rim=False), faces={
        'down': mc('belly', clusters=0.1, rim=False, hd=True, map=grooves(48, 60), keys=STAR_KEYS),
        'east': mc('belly', clusters=0.0, rim=False, hd=True, map=['g.' * 30] * 6, keys=STAR_KEYS),
        'west': mc('belly', clusters=0.0, rim=False, hd=True, map=['g.' * 30] * 6, keys=STAR_KEYS),
        'up': dict(skip=True)})
    # ---- six fins: the long front pair, a middle pair under the belly and a small rear pair on the tail stock
    for side, sx in (('left', 1), ('right', -1)):
        wing(body, f'{side}_fin', side, sx, (14.5 * sx, 8, -7), (0.1, -0.25 * sx, 0.55 * sx), (14, 1.2, 9), (16, 0.8, 7), 41 + sx)
        wing(body, f'{side}_fin_2', side, sx, (14.0 * sx, 9, 7), (0.15, -0.6 * sx, 0.3 * sx), (10, 1.0, 7), (9, 0.7, 5), 51 + sx)
    # ---- the tail: four tapering segments (each one bends a little further), the dorsal fin, the flukes
    t1 = body.part('tail1', pivot=(0, -0.5, 14))
    t1.cube((-12, -10.5, 0), (24, 21, 14), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=overlay(stars(48, 28, 51), swirls(48, 28, 52, 1, 0.0))),
        'east': flank((24, 21, 14), (8, 14), 52), 'west': flank((24, 21, 14), (8, 14), 53),
        'down': mc('belly', clusters=0.15, hd=True, map=grooves(48, 28, step=4), keys=STAR_KEYS)})
    for side, sx in (('left', 1), ('right', -1)):
        wing(t1, f'{side}_fin_3', side, sx, (11.5 * sx, 5, 7), (0.1, -0.75 * sx, 0.2 * sx), (8, 0.8, 6), (7, 0.6, 4.5), 61 + sx, cloud=False)
    dorsal = t1.part('dorsal_fin', pivot=(0, -10.5, 8), rot=(-0.55, 0, 0))
    dorsal.cube((-0.75, -6, -2), (1.5, 6, 7), **mc('fin_d', clusters=0.2), faces={
        'east': mc('fin_d', clusters=0.2, hd=True, map=swirls(14, 12, 71, 1, 0.0), keys=RUNE_KEYS, glow_keys='Rr'),
        'west': mc('fin_d', clusters=0.2, hd=True, map=swirls(14, 12, 72, 1, 0.0), keys=RUNE_KEYS, glow_keys='Rr')})
    cloud(dorsal, -2, -8.5, -1, 4, 3, 5, 3)  # a puff of cloud caught on its tip
    t2 = t1.part('tail2', pivot=(0, 0.5, 14))
    t2.cube((-9, -8, 0), (18, 16, 12), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=stars(36, 24, 54)),
        'east': flank((18, 16, 12), (7, 12), 55), 'west': flank((18, 16, 12), (7, 12), 56),
        'down': mc('belly', clusters=0.15)})
    cloud(t2, -2.5, -10.5, 1, 5, 3, 9, 5)  # cloud along the tail stock
    for sx in (1, -1):
        cloud(t1, (10.5 if sx > 0 else -10.5 - 4.5), 4, 2, 4.5, 4.5, 8, 7 + (sx > 0))
    t3 = t2.part('tail3', pivot=(0, 0.5, 12))
    t3.cube((-6, -5.5, 0), (12, 11, 10), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=stars(24, 20, 57)),
        'east': flank((12, 11, 10), (5, 8), 58, star=False), 'west': flank((12, 11, 10), (5, 8), 59, star=False),
        'down': mc('belly', clusters=0.15)})
    t3.cube((-1, -7.5, 0.5), (2, 2.5, 9), **mc('hide_d', clusters=0.2))
    t3.cube((-1, 5.5, 0.5), (2, 2, 9), **mc('hide_d', clusters=0.2))  # the keel underneath
    t4 = t3.part('tail4', pivot=(0, 0, 10))
    t4.cube((-3.5, -3.5, 0), (7, 7, 8), **mc('hide', clusters=0.25), faces={'down': mc('flank', clusters=0.2)})
    # the flukes: two broad blades swept back from a notch, cut ragged like a cloud's edge, glowing runes inside it
    for side, sx in (('left', 1), ('right', -1)):
        fl = t4.part(f'{side}_fluke', pivot=(0.5 * sx, 0, 6), rot=(0, -0.32 * sx, 0))

        def serrate(x, y):
            depth = int(round(1.5 + 1.5 * math.sin(x * 0.55) + (x % 3 == 0)))
            return '_' if y < depth else ('R' if y == depth + 2 and x % 6 == 3 else '.')
        cut = _rows(46, 30, serrate)
        up = overlay(swirls(46, 30, 61 + sx, 2, 0.004), cut)
        fl.cube((0 if sx > 0 else -23, -0.8, -3), (23, 1.6, 15), **mc('fin', clusters=0.25), faces={
            'up': mc('fin_d', clusters=0.25, noise=0.7, hd=True, map=up if sx > 0 else [r[::-1] for r in up], keys=RUNE_KEYS, glow_keys='SsRr'),
            'down': mc('veil', clusters=0.15, hd=True, map=[r.replace('R', '.').replace('r', '.') for r in (up if sx > 0 else [r[::-1] for r in up])],
                       keys=RUNE_KEYS),
            'north': mc('fin', clusters=0.2, rim=False), 'south': mc('fin_d', clusters=0.2, rim=False)})
        fl.cube((0 if sx > 0 else -6, -1.5, -3.5), (6, 3, 6), **mc('hide', clusters=0.25))  # the root of the blade
        cloud(fl, 16 if sx > 0 else -21, -2.0, -2.5, 5, 2.2, 4, 6)  # a wisp of cloud at its tip
    return m


MODELS = {'sky_whale': sky_whale}


def items():
    """The Sky Whale's spawn egg (S1 land: drawn with the other land creatures' in tools/land_eggs.py)."""
    return {'sky_whale_spawn_egg': __import__('land_eggs').sky_whale()}
