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
    'hide': '#5672bd', 'hide_l': '#7590d6', 'hide_d': '#3d5498', 'flank': '#8fb2ea', 'flank_l': '#b2cdf4', 'flank_d': '#7193d3',
    'belly': '#eef3ff', 'belly_l': '#ffffff', 'belly_d': '#cdd8f2', 'groove': '#b8c4ea',
    'spot': '#e4eeff', 'knob': '#dfe9fb', 'knob_d': '#a9badf', 'star': '#fff3b0', 'star2': '#bff6ff',
    'mouth': '#3e2a52', 'tongue': '#e98fb2', 'baleen': '#e8dcc0', 'baleen_d': '#b6a482',
    'eye': '#181a34', 'iris': '#45d6ea', 'iris_d': '#2a8ac8', 'eye_hi': '#ffffff', 'lid': '#5672bd', 'lid_d': '#2c3c74',
    'hole': '#1f2850', 'fin': '#5672bd', 'fin_l': '#7590d6', 'fin_d': '#3d5498', 'ink': '#24305e',
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


def sky_whale() -> Model:
    """See the module notes. The parts SkyWhaleModel moves: body, head, jaw, throat, blowhole, the
    eyes (painted per expression), left/right fin and fin_tip, tail1-tail4, left/right fluke and
    the dorsal fin."""
    m = Model('sky_whale', (256, 256), dict(PALETTE), {'sky_whale': {}}, res=2, expressions=EXPRS,
              materials={'knob': 'bone', 'knob_d': 'bone', 'groove': 'skin', 'spot': 'skin', 'ink': 'skin'})
    top = mc('hide_d', clusters=0.3, hd=True, keys=STAR_KEYS, glow_keys='Ss')
    # ---- the chest: the deepest part of the body, the fins hang from it
    body = m.part('body', pivot=(0, 6, 0))
    body.cube((-15, -12, -14), (30, 24, 28), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=overlay(stars(60, 56, 3, 0.006), spots(60, 56, 4, 0.003))),
        'east': countershade((30, 24, 28), (9, 15), 10), 'west': countershade((30, 24, 28), (9, 15), 11),
        'down': mc('belly', clusters=0.15, hd=True, map=grooves(60, 56), keys=STAR_KEYS)})
    body.cube((-12, -14, -12), (24, 2, 24), **mc('hide_d', clusters=0.3), faces={'up': dict(top, map=stars(48, 48, 5))})
    for sx in (1, -1):
        body.cube((15 if sx > 0 else -16, -9, -11), (1, 17, 22), **mc('hide', clusters=0.25), faces={
            'east': countershade((1, 17, 22), (6, 11), 12), 'west': countershade((1, 17, 22), (6, 11), 13)})
    # ---- the head: cranium, rostrum and the snout's rounded tip; tubercles on the rostrum
    head = body.part('head', pivot=(0, 0, -14))
    head.cube((-13.5, -11, -18), (27, 15, 18), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=overlay(stars(54, 36, 21, 0.004), spots(54, 36, 22, 0.02))),
        'east': countershade((27, 15, 18), (7, 12), 23), 'west': countershade((27, 15, 18), (7, 12), 24),
        'down': mc('belly', clusters=0.15)})
    head.cube((-11, -9, -28), (22, 11, 10), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=spots(44, 20, 25, 0.05)),
        'east': countershade((22, 11, 10), (5, 9), 26, star=False), 'west': countershade((22, 11, 10), (5, 9), 27, star=False),
        'down': mc('belly', clusters=0.15)})
    head.cube((-8, -7, -31), (16, 8, 3), **mc('hide', clusters=0.25), faces={
        'north': mc('hide', clusters=0.2, bands=[(4, 'flank')], hd=True, map=spots(32, 16, 28, 0.04), keys=STAR_KEYS),
        'up': dict(top, map=spots(32, 6, 29, 0.05))})
    # the knobs of a humpback's rostrum
    rnd = random.Random(7)
    for i in range(9):
        x, z = rnd.uniform(-8.5, 7.5), rnd.uniform(-29.5, -18.5)
        y = -9 if z > -28 else -7
        head.cube((x, y - 1.2, z), (1.4, 1.2, 1.4), **mc('knob', clusters=0.0, rim=False), faces={'down': dict(skip=True)})
    blowhole = head.part('blowhole', pivot=(0, -11, -9))
    blowhole.cube((-3.5, -1.5, -2.5), (7, 1.5, 5), **mc('hide_d', clusters=0.2), faces={
        'up': mc('hide_d', clusters=0.0, hd=True, keys={'h': 'hole', 'l': 'hide_l'},
                 map=['..............', '..............', '...lhh..hhl...', '..lhhh..hhhl..', '..lhhh..hhhl..', '...lhh..hhl...',
                      '..............', '..............', '..............', '..............'])})
    # baleen hanging under the upper jaw, seen when the mouth opens
    bal = ''.join('B' if x % 2 == 0 else 'b' for x in range(52))
    baleen = head.part('baleen', pivot=(0, 4, -1))
    baleen.cube((-12.5, 0, -27), (25, 3, 26), **mc('baleen', clusters=0.0, rim=False), faces={
        'north': mc('baleen', clusters=0.0, rim=False, hd=True, map=[bal] * 4 + [bal.replace('b', '_')] * 2, keys={'B': 'baleen', 'b': 'baleen_d'}),
        'east': mc('baleen', clusters=0.0, rim=False, hd=True, map=[bal] * 4 + [bal.replace('b', '_')] * 2, keys={'B': 'baleen', 'b': 'baleen_d'}),
        'west': mc('baleen', clusters=0.0, rim=False, hd=True, map=[bal] * 4 + [bal.replace('b', '_')] * 2, keys={'B': 'baleen', 'b': 'baleen_d'}),
        'up': dict(skip=True), 'south': dict(skip=True), 'down': dict(skip=True)})
    for side, sx in (('left', 1), ('right', -1)):
        e = head.part(f'{side}_eye', pivot=(13.5 * sx, -1.0, -10.5))
        face = 'east' if sx > 0 else 'west'
        e.cube((0 if sx > 0 else -0.2, -1.5, -1.5), (0.2, 3, 3), **mc('flank', clusters=0.2, rim=False), faces={
            face: mc('flank', clusters=0.2, rim=False, hd=True, map=eye_map(6, ''), keys=EYE_KEYS, glow_keys='',
                     expr={x: eye_map(6, x) for x in EXPRS})})
    # ---- the lower jaw: long and heavy, the mouth line curving up towards the eye; pleats begin under the chin
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
    # ---- the long pectoral fins: two lengths each, white below, a scalloped leading edge, stars on the trailing edge
    for side, sx in (('left', 1), ('right', -1)):
        fin = body.part(f'{side}_fin', pivot=(14.5 * sx, 8, -7), rot=(0.1, -0.25 * sx, 0.55 * sx))
        fin.cube((0 if sx > 0 else -14, -1.5, -4), (14, 3, 8), **mc('fin', clusters=0.25), faces={
            'up': mc('fin', clusters=0.25, hd=True, map=overlay(spots(28, 16, 41 + sx, 0.03), stars(28, 16, 43 + sx, 0.01)), keys=STAR_KEYS,
                     glow_keys='Ss'),
            'down': mc('belly', clusters=0.15), 'north': mc('knob', clusters=0.2, rim=False)})
        tip = fin.part(f'{side}_fin_tip', pivot=(14 * sx, 0, 0), rot=(0, -0.18 * sx, 0.12 * sx))
        trail = _rows(32, 12, lambda x, y: 'S' if y >= 10 and x % 5 == 2 else ('s' if y >= 9 and x % 5 == 2 else '.'))
        tip.cube((0 if sx > 0 else -16, -1, -3.5), (16, 2, 6), **mc('fin', clusters=0.25), faces={
            'up': mc('fin', clusters=0.25, hd=True, map=overlay(spots(32, 12, 45 + sx, 0.04), trail), keys=STAR_KEYS, glow_keys='Ss'),
            'down': mc('belly', clusters=0.15), 'north': mc('knob', clusters=0.2, rim=False)})
        tip.cube((16 if sx > 0 else -19, -0.75, -2.5), (3, 1.5, 4), **mc('fin', clusters=0.2), faces={'down': mc('belly', clusters=0.1)})
        # tubercles along the leading edge
        for k in range(6):
            fx = 1.5 + k * 2.6
            tip.cube(((fx if sx > 0 else -fx - 1.4), -0.8, -4.3), (1.4, 1.4, 1.0), **mc('knob', clusters=0.0, rim=False))
    # ---- the tail: four tapering segments (each one bends a little further), the dorsal fin, the flukes
    t1 = body.part('tail1', pivot=(0, -0.5, 14))
    t1.cube((-12, -10.5, 0), (24, 21, 14), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=stars(48, 28, 51)),
        'east': countershade((24, 21, 14), (8, 14), 52), 'west': countershade((24, 21, 14), (8, 14), 53),
        'down': mc('belly', clusters=0.15, hd=True, map=grooves(48, 28, step=4), keys=STAR_KEYS)})
    dorsal = t1.part('dorsal_fin', pivot=(0, -10.5, 8), rot=(-0.55, 0, 0))
    dorsal.cube((-1, -6, -2), (2, 6, 7), **mc('fin_d', clusters=0.2))
    dorsal.cube((-0.75, -8, 1), (1.5, 2, 3), **mc('fin_d', clusters=0.2))  # the hooked tip
    t2 = t1.part('tail2', pivot=(0, 0.5, 14))
    t2.cube((-9, -8, 0), (18, 16, 12), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=stars(36, 24, 54)),
        'east': countershade((18, 16, 12), (7, 12), 55), 'west': countershade((18, 16, 12), (7, 12), 56),
        'down': mc('belly', clusters=0.15)})
    t2.cube((-1, -10, 1), (2, 2, 10), **mc('hide_d', clusters=0.2))  # the ridge of the tail stock
    t3 = t2.part('tail3', pivot=(0, 0.5, 12))
    t3.cube((-6, -5.5, 0), (12, 11, 10), **mc('hide', clusters=0.25), faces={
        'up': dict(top, map=stars(24, 20, 57)),
        'east': countershade((12, 11, 10), (5, 8), 58, star=False), 'west': countershade((12, 11, 10), (5, 8), 59, star=False),
        'down': mc('belly', clusters=0.15)})
    t3.cube((-1, -7.5, 0.5), (2, 2.5, 9), **mc('hide_d', clusters=0.2))
    t3.cube((-1, 5.5, 0.5), (2, 2, 9), **mc('hide_d', clusters=0.2))  # the keel underneath
    t4 = t3.part('tail4', pivot=(0, 0, 10))
    t4.cube((-3.5, -3.5, 0), (7, 7, 8), **mc('hide', clusters=0.25), faces={'down': mc('flank', clusters=0.2)})
    # the flukes: two broad blades swept back from a notch, each with its own pattern underneath
    for side, sx in (('left', 1), ('right', -1)):
        fl = t4.part(f'{side}_fluke', pivot=(0.5 * sx, 0, 6), rot=(0, -0.32 * sx, 0))
        under = _rows(46, 30, lambda x, y, s=sx: 'i' if (math.sin(x * 0.45 + s) * 3 + y * 0.6 + math.cos(y * 0.7 + x * 0.2) * 2) % 7 < 1.6
                      and y > 6 else '.')
        # the trailing edge (row 0 of the up and down faces is the back) is cut into a ragged scallop,
        # and a row of stars glows just inside it
        def serrate(x, y):
            depth = int(round(1.5 + 1.5 * math.sin(x * 0.55) + (x % 3 == 0)))
            return '_' if y < depth else ('S' if y == depth + 1 and x % 6 == 3 else '.')
        cut = _rows(46, 30, serrate)
        fl.cube((0 if sx > 0 else -23, -0.8, -3), (23, 1.6, 15), **mc('fin', clusters=0.25), faces={
            'up': mc('fin_d', clusters=0.25, hd=True, map=overlay(stars(46, 30, 61 + sx), cut), keys=STAR_KEYS, glow_keys='Ss'),
            'down': mc('belly', clusters=0.15, hd=True, map=overlay(under, [r.replace('S', '.') for r in cut]), keys=STAR_KEYS),
            'north': mc('fin', clusters=0.2, rim=False), 'south': mc('fin_d', clusters=0.2, rim=False)})
        fl.cube((0 if sx > 0 else -6, -1.5, -3.5), (6, 3, 6), **mc('hide', clusters=0.25))  # the root of the blade
    return m


MODELS = {'sky_whale': sky_whale}


def items():
    """The Sky Whale's spawn egg as the whale looks now: a periwinkle back freckled with glowing stars,
    a small dark eye over the long line of its mouth, the pale grooved throat and a long white flipper
    reaching out of a puff of cloud."""
    import items16 as I
    egg = I.egg(['#3d5498', '#5672bd', '#7590d6', '#8fb2ea'], '#1c2450', {
        2: '.......s........', 3: '.....s....s.....', 5: '...s.......s....', 7: '....EE..........', 8: '...mmmmm........',
        9: '...bbbbbbbbbb...', 10: '...gbgbgbgbgb...', 11: '..ffbgbgbgbg....', 12: '.ffff.bbbbb.....', 13: 'fff.............'},
        pal={'s': '#fff3b0', 'E': '#181a34', 'm': '#2a2448', 'b': '#dfe4f0', 'g': '#aab4d4', 'f': ('#dfe4f0', '#5672bd')},
        no_ol='sEm', ring='cloud')
    return {'sky_whale_spawn_egg': egg}
