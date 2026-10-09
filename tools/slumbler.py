"""CR2: the Slumbler, remade as a living instrument (geometry, paint and the scale shimmer).

A big, sleepy Chrome salamander whose body is a carved wooden instrument: its back is a violin's
arched spruce top in honey varnish, edged with ebony purfling and pierced by two f-holes that glow
when it hums; its tail ends in a carved scroll; its whiskers are strings on ebony tuning pegs.
Everywhere else it is covered in rainbow scales - red at the snout, through orange, gold, green and
blue to violet at the tail - with frilled pink gills, a fin running down its tail and frilled fins
on its legs. A glint of light sweeps along its scales now and then (SlumblerRenderer draws the
shimmer frames painted here).

CR2 also paints the Slumbler's family here: its stingray-like tadpole and the clutch of jelly eggs
it hatches from, the gill and egg sprites, and the tadpole's buckets.

Hooks (one line each): mobs.slumbler and mobs.ALL (the models), gen_assets.generate (sounds, loot,
tags, text), gen_textures.main (the shimmer frames, the Chrome tadpole bucket) and items16.all_items
(item sprites)."""
import math
import os

import numpy as np
from PIL import Image

from modelkit import Model, Painter, _rest_frames

# head to tail: dusky rose, amber, olive gold, moss, teal, slate blue, violet - an oil-slick sheen on dark scales
RAINBOW = ['#9c6a68', '#9d7a55', '#8f8753', '#678660', '#58817f', '#5c6a8f', '#76628a']
HUE_KEYS = '0123456'
LIT_KEYS = 'abcdefg'
RIM_KEYS = 'ABCDEFG'
Z_HEAD, Z_TAIL = -24.0, 44.0  # the model's length, for the run of the rainbow
SHIMMER_FRAMES = 12


def _hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def _hex(rgb):
    return '#%02x%02x%02x' % tuple(max(0, min(255, int(round(v)))) for v in rgb)


def _mix(a, b, t):
    a, b = _hx(a), _hx(b)
    return _hex(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def palette():
    pal = {
        'wood': '#a87442', 'wood_l': '#c4945c', 'wood_d': '#714720', 'grain': '#8a5a2e', 'flame': '#b98652',
        'ebony': '#2c1e1b', 'ebony_l': '#4b3731', 'fhole': '#1c0e0a', 'fglow': '#ffd27e',
        'belly': '#d6cbbd', 'belly_l': '#e6ddd0', 'belly_d': '#b3a697',
        'gill': '#b8607a', 'gill_l': '#cf8196', 'gill_d': '#83405a', 'fin': '#a99fb2', 'fin_l': '#c2b9c9', 'fin_d': '#7e7489',
        'iris': '#c9a03c', 'iris_d': '#94702a', 'pupil': '#15100f', 'lid': '#7d5a3a', 'lid_d': '#4e3522',
        'mouth': '#9c4c5e', 'mouth_d': '#6a2c3c', 'tongue': '#b8687c', 'teeth': '#e8dfcc', 'claw': '#d8cdbd',
        'string': '#d8cdb0', 'peg': '#2c1e1b', 'peg_l': '#5a463e',
    }
    for k, c in enumerate(RAINBOW):
        pal[f'h{k}'] = c
        pal[f'h{k}l'] = _mix(c, '#e8e2d0', 0.3)
        pal[f'h{k}d'] = _mix(c, '#2e2838', 0.3)
    return pal


SCALE_KEYS = {}
for _k in range(7):
    SCALE_KEYS[HUE_KEYS[_k]] = f'h{_k}'
    SCALE_KEYS[LIT_KEYS[_k]] = f'h{_k}l'
    SCALE_KEYS[RIM_KEYS[_k]] = f'h{_k}d'


def _scale_at(x, y, r=3.8, sh=4.0, sw=7.0):
    """The scale lying on top at texel (x, y) - (row, column), offset from its centre - or None.
    Scales are discs in staggered rows, each row overlapping the one below it."""
    xx, yy = x + 0.5, y + 0.5
    r0 = int(math.floor((yy - r) / sh))
    for row in range(r0, r0 + 4):
        off = (row % 2) * sw / 2
        col = math.floor((xx - off) / sw + 0.5)
        cx, cy = col * sw + off, row * sh
        if (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r:
            return (row, col), xx - cx, yy - cy
    return None, 0.0, 0.0


def scales(w, h, z_at, phase=0):
    """Rainbow scales (hd map, one char per texel): staggered rows of rounded scales, lit along the
    edge where they slip out from under the row above and shaded along their lower rim. The hue
    follows the body from head to tail (z_at(x, y) gives the model z of a texel) and every scale is
    nudged a little off it, so neighbours shimmer in slightly different colours like mother-of-pearl."""
    rows = []
    for y in range(h):
        r = ''
        for x in range(w):
            own, dx, dy = _scale_at(x, y + phase)
            below = _scale_at(x, y + phase + 1)[0]
            above = _scale_at(x, y + phase - 1)[0]
            t = (z_at(x, y) - Z_HEAD) / (Z_TAIL - Z_HEAD)
            if own is None:
                k = max(0, min(6, int(t * 7)))
                r += RIM_KEYS[k]
                continue
            jitter = (((own[0] * 73 + own[1] * 151 + 7) * 2654435761) % 1000) / 1000.0 - 0.5
            k = max(0, min(6, int(t * 7 + jitter * 0.9)))
            if below != own:
                r += RIM_KEYS[k]
            elif above != own:
                r += LIT_KEYS[k]
            else:
                r += HUE_KEYS[k]
        rows.append(r)
    return rows


def z_fn(face, z0, z1, w, h):
    """Model z of a texel of a face whose cube spans z0..z1 (modelkit's face orientation)."""
    if face == 'east':
        return lambda x, y: z0 + (x + 0.5) / w * (z1 - z0)
    if face == 'west':
        return lambda x, y: z1 - (x + 0.5) / w * (z1 - z0)
    if face in ('up', 'down'):
        return lambda x, y: z1 - (y + 0.5) / h * (z1 - z0)
    if face == 'north':
        return lambda x, y: z0
    return lambda x, y: z1


def scaled(origin, size, zw, faces=('north', 'south', 'east', 'west', 'up', 'down'), res=2, phase=0, zmap=None, **over):
    """Face specs that cover a cube in rainbow scales (zw: the model z of the cube's part pivot; zmap,
    if given, maps a smaller creature's z onto the Slumbler's length so it carries the whole rainbow)."""
    w, h, d = (int(math.ceil(s)) * res for s in size)
    z0, z1 = zw + origin[2], zw + origin[2] + size[2]
    if zmap is not None:
        z0, z1 = zmap(z0), zmap(z1)
    dims = {'north': (w, h), 'south': (w, h), 'east': (d, h), 'west': (d, h), 'up': (w, d), 'down': (w, d)}
    out = {}
    for f in faces:
        fw, fh = dims[f]
        out[f] = dict(color='h3', pattern='mc', clusters=0.0, rim=False, hd=True, keys=SCALE_KEYS,
                      map=scales(fw, fh, z_fn(f, z0, z1, fw, fh), phase=phase))
    out.update(over)
    return out


def soundboard(w, h, fholes=False, purfling=True, ends=(True, True)):
    """The arched spruce top (hd map): straight grain running down the body in honey varnish, an
    ebony purfling line inset from the edge (along the back end and front end too where `ends` say
    so - the plates of the hourglass join without a line between them), and two f-holes with a warm
    light inside."""
    rows = []
    for y in range(h):
        r = ''
        for x in range(w):
            edge = min(x, w - 1 - x, y if ends[0] else 99, h - 1 - y if ends[1] else 99)
            if purfling and edge == 0:
                r += 'L'
            elif purfling and edge == 1:
                r += 'p'
            elif (x * 3 + (y // 5)) % 7 == 0:
                r += 'g'
            elif (x * 5 + y // 3) % 11 == 0:
                r += 'f'
            else:
                r += '.'
        rows.append(r)
    if fholes:
        # two f-holes facing each other: an S-shaped slot with a round eye at each end, light glowing inside
        g = [list(r) for r in rows]
        top, n = int(h * 0.16), int(h * 0.64)
        for sx, cx in ((-1, w * 0.3), (1, w * 0.7)):
            for k in range(n + 1):
                t = k / n
                x = int(math.floor(cx + sx * 1.2 * math.sin(2 * math.pi * t)))
                for xx in (x, x + 1):
                    if 0 <= xx < w and 0 <= top + k < h:
                        g[top + k][xx] = 'H' if 0.15 < t < 0.85 and xx == x else 'h'
            for ex, ey in ((cx - sx * 1.8, top - 1), (cx + sx * 1.8, top + n)):
                for yy in (ey, ey + 1):
                    for xx in (int(ex) - 1, int(ex)):
                        if 0 <= xx < w and 0 <= yy < h:
                            g[yy][xx] = 'h'
        rows = [''.join(r) for r in g]
    return rows


WOOD_KEYS = {'L': 'wood_l', 'p': 'ebony', 'g': 'grain', 'f': 'flame', 'h': 'fhole', 'H': 'fglow'}


def wood(map_rows, **kw):
    d = dict(color='wood', pattern='mc', clusters=0.2, rim=False, hd=True, map=map_rows, keys=WOOD_KEYS, glow_keys='H')
    d.update(kw)
    return d


def scroll_face(n):
    """The carved scroll at the tip of the tail, seen from the side: a spiral groove in the wood."""
    rows = []
    for y in range(n):
        r = ''
        for x in range(n):
            u, v = (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1
            rr = math.hypot(u, v)
            a = math.atan2(v, u)
            spiral = (rr * 3.2 - a / math.pi) % 1.0
            if rr > 0.98:
                r += '.'
            elif spiral < 0.22:
                r += 'p'
            elif spiral < 0.38:
                r += 'L'
            else:
                r += '.'
        rows.append(r)
    return rows


# the eye on its bump (4 x 6 texels, front and side): a dark pupil with an amber rim, no shine
EYE = ['....', '....', '.Pi.', '.PP.', '....', '....']
EYE_EXPR = {'angry': ['....', 'dddd', '.Pi.', '.PP.', '....', '....'], 'hurt': ['....', '....', 'dddd', '....', '....', '....'],
            'dead': ['....', '....', 'dPPd', '....', '....', '....']}
EYE_KEYS = {'i': 'iris', 'P': 'pupil', 'd': 'lid_d'}


def slumbler() -> Model:
    """See the module notes. Parts the Java model animates: body, head, jaw, the eyes and their lids,
    six gills, four string whiskers (two segments each), four legs with feet and fins, the tail
    (three segments, a fin on each, the scroll) and the f-hole light."""
    pal = palette()
    m = Model('slumbler', (160, 160), pal, {'slumbler': {}}, res=2, expressions=['angry', 'hurt', 'dead'],
              materials={'grain': 'wood', 'flame': 'wood', 'ebony': 'wood', 'peg': 'wood', 'lid': 'wood', 'string': 'flat'})
    belly = dict(color='belly', pattern='mc', clusters=0.25)
    # ---- the body: a violin's hourglass, upper bout, waist and lower bout; spruce on top, scales below
    bz = 0.0
    body = m.part('body', pivot=(0, 14.5, bz))
    for (o, s) in (((-7.5, -4.5, -12), (15, 9, 9)), ((-6, -4.2, -3), (12, 8.4, 5)), ((-8.5, -4.8, 2), (17, 9.6, 10))):
        body.cube(o, s, color='wood', pattern='mc', faces=scaled(o, s, bz, faces=('north', 'south', 'east', 'west'), phase=1,
                                                                    up=wood(soundboard(int(s[0]) * 2, int(s[2]) * 2, purfling=False)),
                                                                    down=belly))
    # the arched top: three plates, f-holes in the lower bout, ebony purfling round every edge
    body.cube((-6.5, -5.6, -11), (13, 1.1, 7), **wood(soundboard(26, 14, ends=(False, True))), faces={
        'east': wood(['LLLLLLLLLLLLLL', 'pppppppppppppp']), 'west': wood(['LLLLLLLLLLLLLL', 'pppppppppppppp']),
        'north': wood(['L' * 26, 'p' * 26]), 'south': wood(['L' * 26, 'p' * 26]), 'down': dict(skip=True)})
    body.cube((-5, -5.6, -4), (10, 1.1, 7), **wood(soundboard(20, 14, ends=(False, False))), faces={
        'east': wood(['L' * 14, 'p' * 14]), 'west': wood(['L' * 14, 'p' * 14]), 'down': dict(skip=True)})
    body.cube((-7.5, -5.6, 3), (15, 1.1, 8), **wood(soundboard(30, 16, fholes=True, ends=(True, False))), faces={
        'east': wood(['L' * 16, 'p' * 16]), 'west': wood(['L' * 16, 'p' * 16]),
        'north': wood(['L' * 30, 'p' * 30]), 'south': wood(['L' * 30, 'p' * 30]), 'down': dict(skip=True)})
    # the pearly belly, a little rounded, and the swell of the flanks
    body.cube((-6.5, 4.4, -10), (13, 1, 20), **belly)
    for sx in (1, -1):
        o, s = ((8.5 if sx > 0 else -9.5), -3.2, 3.5), (1, 6.4, 7)
        body.cube(o, s, color='h2', pattern='mc', faces=scaled(o, s, bz, faces=('east', 'west', 'north', 'south', 'up', 'down'), phase=2))
        o, s = ((7.5 if sx > 0 else -8.5), -3.0, -10.5), (1, 6, 6)
        body.cube(o, s, color='h1', pattern='mc', faces=scaled(o, s, bz, faces=('east', 'west', 'north', 'south', 'up', 'down'), phase=1))
    # ---- the head: broad and flat like a newt's, carved wood on top, scaled cheeks, a huge mouth
    hz = bz - 12
    head = body.part('head', pivot=(0, -0.5, -12))
    head.cube((-7, -4, -9), (14, 5, 9), color='wood', pattern='mc', faces=scaled((-7, -4, -9), (14, 5, 9), hz, faces=('east', 'west', 'north'),
                                                                                   up=wood(soundboard(28, 18)), down=belly))
    for sx in (1, -1):  # full cheeks
        o, s = ((7 if sx > 0 else -8), -3.2, -8.0), (1, 3.6, 6)
        head.cube(o, s, color='h0', pattern='mc', faces=scaled(o, s, hz, faces=('east', 'west', 'north', 'south', 'up', 'down')))
    snout = ['.' * 22] * 2 + ['..oo..............oo..', '.' * 22, 'tttttttttttttttttttttt', 'tttttttttttttttttttttt', '.' * 22]
    head.cube((-5.5, -3.5, -11), (11, 3.5, 2), color='wood', pattern='mc', faces=scaled((-5.5, -3.5, -11), (11, 3.5, 2), hz,
                                                                                         faces=('east', 'west'), up=wood(soundboard(22, 4)),
                                                                                         north=dict(color='h0', pattern='mc', clusters=0.2, hd=True, map=snout,
                                                                                                    keys={'o': 'h0d', 't': 'teeth'}),
                                                                                         down=dict(color='mouth', pattern='mc', clusters=0.0)))
    head.cube((-6.8, 0, -9.2), (13.6, 1, 9.2), **dict(color='mouth', pattern='mc', clusters=0.0, rim=False), faces={
        'down': dict(color='mouth', pattern='mc', clusters=0.0, rim=False, hd=True, keys={'t': 'teeth', 'd': 'mouth_d'},
                     map=['d' * 28] + ['d' + '.' * 26 + 'd'] * 18 + ['t.' * 14]),  # up and down faces run back (row 0) to front
        'up': dict(skip=True)})
    jaw = head.part('jaw', pivot=(0, 1, -0.5))
    jaw.cube((-6.8, 0, -10.3), (13.6, 2.4, 10.3), color='belly', pattern='mc', faces=scaled((-6.8, 0, -10.3), (13.6, 2.4, 10.3), hz,
                                                                                            faces=('east', 'west'), down=belly,
                                                                                            north=dict(color='belly', pattern='mc', clusters=0.0, rim=False, hd=True,
                                                                                                       map=['.t' * 14], keys={'t': 'teeth'}),
                                                                                            up=dict(color='mouth', pattern='mc', clusters=0.0, rim=False, hd=True,
                                                                                                    keys={'t': 'teeth', 'd': 'mouth_d', 'r': 'tongue'},
                                                                                                    map=['d' * 28] + ['d' + '.' * 26 + 'd'] * 4
                                                                                                    + ['d' + '.' * 9 + 'r' * 8 + '.' * 9 + 'd'] * 9
                                                                                                    + ['d' + '.' * 26 + 'd'] * 7 + ['.t' * 14])))
    for side, sx in (('left', 1), ('right', -1)):
        eye = head.part(f'{side}_eye', pivot=(4.6 * sx, -3.6, -6.4))
        eye_face = dict(color='h0', pattern='mc', clusters=0.2, hd=True, keys=EYE_KEYS, map=EYE, expr=EYE_EXPR)
        eye.cube((-1, -2.2, -1), (2, 2.2, 2), color='h0', pattern='mc', clusters=0.2, faces={
            'north': eye_face, ('east' if sx > 0 else 'west'): eye_face, 'up': dict(color='h0d', pattern='mc', clusters=0.2)})
        lid = eye.part(f'{side}_eyelid')
        shut = dict(color='h0', pattern='mc', clusters=0.0, rim=False, hd=True, map=['....', '....', '....', 'dddd', '....', '....'],
                    keys={'d': 'h0d'})
        lid.cube((-1, -2.2, -1), (2, 2.2, 2), inflate=0.08, color='h0', pattern='mc', clusters=0.15, rim=False, faces={
            'north': shut, ('east' if sx > 0 else 'west'): shut})
        # frilled gills: three feathery fans on each side of the head, sweeping back
        for i, (y, ry, rz, ln) in enumerate(((-3.4, 0.7, -0.6, 8.0), (-1.6, 0.9, -0.05, 9.0), (0.2, 1.1, 0.5, 7.5))):
            gill = head.part(f'{side}_gill_{i}', pivot=(6.6 * sx, y, -2.5), rot=(0, -ry * sx, rz * sx))
            gill.cube((0 if sx > 0 else -ln, -0.4, -0.4), (ln, 0.8, 0.8), color='gill_d', pattern='mc', clusters=0.0, rim=False)
            gill.cube((0 if sx > 0 else -ln, -2.2, 0), (ln, 4.4, 0), color='gill', pattern='mc', clusters=0.0, rim=False, faces={
                'north': dict(color='gill', pattern='mc', clusters=0.0, rim=False, ribs=1, accent='gill_l', alpha='membrane', edge='bottom',
                              edge_depth=2, scallop=2),
                'south': dict(color='gill_d', pattern='mc', clusters=0.0, rim=False, ribs=1, accent='gill', alpha='membrane', edge='bottom',
                              edge_depth=2, scallop=2)})
        # string whiskers on ebony tuning pegs, two to a side, each in two lengths so they sway
        for i, (x, y, ry, rx) in enumerate(((5.2, -1.5, 0.55, 0.55), (3.6, -0.8, 0.25, 0.75))):
            peg = head.part(f'{side}_peg_{i}', pivot=(x * sx, y, -10.6))
            peg.cube((-0.6, -0.6, -1.2), (1.2, 1.2, 1.2), color='peg', pattern='mc', clusters=0.0, rim=False, faces={
                'north': dict(color='peg_l', pattern='mc', clusters=0.0, rim=False)})
            wh = head.part(f'{side}_whisker_{i}', pivot=(x * sx, y, -11.6), rot=(rx, -ry * sx, 0))
            wh.cube((-0.15, -0.15, -3.5), (0.3, 0.3, 3.5), color='string', pattern='mc', clusters=0.0, rim=False)
            tip = wh.part(f'{side}_whisker_tip_{i}', pivot=(0, 0, -3.5), rot=(0.5, 0, 0))
            tip.cube((-0.12, -0.12, -3), (0.25, 0.25, 3), color='string', pattern='mc', clusters=0.0, rim=False)
    # ---- legs: sprawling, scaled, webbed feet, a frilled fin on the back of each
    for side, sx in (('left', 1), ('right', -1)):
        for end, lz in (('front', -7.5), ('hind', 7.5)):
            leg = body.part(f'{side}_{end}_leg', pivot=(7.5 * sx, 2.5, lz))
            o, s = ((0 if sx > 0 else -5.5), -1.6, -2.2), (5.5, 3.2, 4.4)
            leg.cube(o, s, color='h3', pattern='mc', faces=scaled(o, s, lz, down=belly))
            fin = leg.part(f'{side}_{end}_fin', pivot=(2.6 * sx, -1.5, 2.2), rot=(0.35, 0, 0))
            fin.cube((-1.8, -3, 0), (3.6, 3, 0), color='fin', pattern='mc', clusters=0.0, rim=False, faces={
                'north': dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='bottom',
                              edge_depth=1, scallop=2),
                'south': dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='bottom',
                              edge_depth=1, scallop=2)})
            foot = leg.part(f'{side}_{end}_foot', pivot=(4.8 * sx, 0.5, 0))
            fo, fs = (-2.1, 0, -2.6), (4.2, 7, 5.2)
            foot.cube(fo, fs, color='h3', pattern='mc', faces=scaled(fo, fs, lz, faces=('east', 'west', 'south', 'up'),
                                                                        north=dict(color='h3', pattern='mc', clusters=0.0, rim=False, hd=True, keys=dict(SCALE_KEYS, c='claw'),
                                                                                   map=scales(10, 14, lambda x, y: lz - 2)[:10] + ['.' * 10] * 2 + ['c.c..c.c.c', 'cc.cc.cc.c']),
                                                                        down=dict(color='belly_d', pattern='mc', clusters=0.0, rim=False)))
            # the web between the toes
            foot.cube((-2.8, 6.4, -3.6), (5.6, 0.6, 2.0), color='fin_d', pattern='mc', clusters=0.0, rim=False)
    # ---- the tail: three tapering segments, a fin running along the top and bottom, a carved scroll at the tip
    tz = bz + 12
    t1 = body.part('tail1', pivot=(0, -1, 12))
    tails = [t1]
    o, s = (-5.5, -3.5, 0), (11, 7, 10)
    t1.cube(o, s, color='h4', pattern='mc', faces=scaled(o, s, tz, faces=('east', 'west', 'up', 'south'), down=belly))
    t2 = t1.part('tail2', pivot=(0, 0.5, 10))
    o2, s2 = (-4, -2.5, 0), (8, 5, 9)
    t2.cube(o2, s2, color='h5', pattern='mc', faces=scaled(o2, s2, tz + 10, faces=('east', 'west', 'up', 'south'), down=belly))
    t3 = t2.part('tail3', pivot=(0, 0.3, 9))
    o3, s3 = (-2.6, -1.8, 0), (5.2, 3.6, 8)
    t3.cube(o3, s3, color='h6', pattern='mc', faces=scaled(o3, s3, tz + 19, faces=('east', 'west', 'up', 'south'), down=belly))
    tails += [t2, t3]
    for i, (t, h, ln, top) in enumerate(((t1, 4.5, 10, -3.5), (t2, 4.0, 9, -2.5), (t3, 3.2, 8, -1.8))):
        # a frilled fin along the top (its free edge combed into frills) and a smaller keel underneath
        fin = t.part(f'tail_fin_{i}', pivot=(0, top, 0))
        frill = dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=2, accent='fin_d', alpha='frill')
        fin.cube((0, -h, 0), (0, h, ln), color='fin', pattern='mc', faces={'east': frill, 'west': frill})
        keel = t.part(f'tail_keel_{i}', pivot=(0, -top, 1))
        hem = dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=2, accent='fin_d', alpha='membrane', edge='bottom', edge_depth=1,
                   scallop=3)
        keel.cube((0, 0, 0), (0, h * 0.55, ln - 2), color='fin', pattern='mc', faces={'east': hem, 'west': hem})
    scroll = t3.part('scroll', pivot=(0, 0, 8))
    scroll.cube((-1.8, -3.2, -0.5), (3.6, 4.8, 4.4), color='wood', pattern='mc', clusters=0.2, faces={
        'east': wood(scroll_face(10)[1:]), 'west': wood([r[::-1] for r in scroll_face(10)[1:]]),
        'up': wood(['L' * 8] + ['pggggggp'] * 8), 'south': wood(['LLLLLLLL', 'pppppppp'] * 5)})
    scroll.cube((-1.2, -4.4, 0.6), (2.4, 1.2, 3.0), color='ebony', pattern='mc', clusters=0.0, rim=False)
    return m


# ---------------------------------------------------------------- the scale shimmer (gen_textures)

def _texel_world_z(model, painter):
    """Model z (rest pose) of every texel of the painted texture, or NaN where nothing is painted."""
    frames = _rest_frames(model)
    H, W = painter.fid.shape
    z = np.full((H, W), np.nan, np.float32)
    keys = np.zeros((H, W), bool)
    for i, (cube, face, fx, fy, fw, fh, spec, idx) in enumerate(painter.face_recs):
        if spec.get('keys') is not SCALE_KEYS and not (isinstance(spec.get('keys'), dict) and 'h0' in spec['keys'].values()):
            continue
        M, T = frames[id(cube)]
        x0, y0, z0 = (o - cube.inflate for o in cube.origin)
        w, h, d = (s + 2 * cube.inflate for s in cube.size)
        x1, y1, z1 = x0 + w, y0 + h, z0 + d
        corner, du, dv = {
            'north': ((x0, y0, z0), (w, 0, 0), (0, h, 0)), 'south': ((x1, y0, z1), (-w, 0, 0), (0, h, 0)),
            'up': ((x0, y0, z1), (w, 0, 0), (0, 0, -d)), 'down': ((x0, y1, z1), (w, 0, 0), (0, 0, -d)),
            'west': ((x0, y0, z1), (0, 0, -d), (0, h, 0)), 'east': ((x1, y0, z0), (0, 0, d), (0, h, 0)),
        }[face]
        jj, ii = np.mgrid[0:fh, 0:fw]
        u = (ii + 0.5) / fw
        v = (jj + 0.5) / fh
        pts = np.stack([corner[q] + du[q] * u + dv[q] * v for q in range(3)], -1).reshape(-1, 3)
        wz = (pts @ M.T + T)[:, 2].reshape(fh, fw)
        region = (slice(fy, fy + fh), slice(fx, fx + fw))
        mine = painter.fid[region] == i
        z[region] = np.where(mine, wz, z[region])
        keys[region] |= mine
    return z, keys


def shimmer_frames():
    """SHIMMER_FRAMES emissive frames: a soft band of light sweeping along the scales from the snout
    to the tip of the tail. Each frame lights the scale texels inside the band, brightest at its
    centre and on the scales' lit edges; SlumblerRenderer cycles them as a glow layer."""
    m = slumbler()
    m.pack()
    p = Painter(m, dict(m.palette), 1)
    img, _ = p.paint_all()
    k = p.detail
    base = np.asarray(img, np.float32)
    z, mask = _texel_world_z(m, p)
    if k > 1:
        z = z.repeat(k, 0).repeat(k, 1)
        mask = mask.repeat(k, 0).repeat(k, 1)
    lum = base[..., :3].mean(-1)
    out = []
    for f in range(SHIMMER_FRAMES):
        centre = Z_HEAD - 6 + (Z_TAIL - Z_HEAD + 12) * f / (SHIMMER_FRAMES - 1)
        band = np.clip(1.0 - np.abs(np.nan_to_num(z, nan=1e4) - centre) / 6.0, 0, 1)
        a = band * mask * (0.2 + 0.3 * np.clip((lum - 90) / 120, 0, 1))  # a faint oil-slick sheen, not a glow
        frame = np.zeros(base.shape, np.uint8)
        rgb = np.clip(base[..., :3] * 0.75 + 255 * 0.25, 0, 255)
        frame[..., :3] = rgb.astype(np.uint8)
        frame[..., 3] = np.clip(a * 255, 0, 255).astype(np.uint8)
        out.append(Image.fromarray(frame, 'RGBA'))
    return out


def textures(out):
    """Writes textures/entity/slumbler/slumbler_shimmer_<n>.png (see shimmer_frames) and, CR2, the
    Chrome Bucket of Slumbler Tadpole (its Chrome turning through the rainbow like the other Chrome
    fish buckets)."""
    for i, img in enumerate(shimmer_frames()):
        out(f'entity/slumbler/slumbler_shimmer_{i}', img)
    import chrome as C
    with _bucket_layer():
        out('item/chrome_slumbler_tadpole_bucket', C.strip(C.bucket_frames('slumbler_tadpole')), C.anim(4))


# ---------------------------------------------------------------- CR2: the tadpole and its eggs (mobs.ALL)

TAD_Z0, TAD_Z1 = -5.4, 13.6  # the tadpole's length, snout to tail tip


def _tad_z(z):
    """A tadpole z on the Slumbler's length: the little ray carries the whole rainbow, snout to tail."""
    return Z_HEAD + (z - TAD_Z0) / (TAD_Z1 - TAD_Z0) * (Z_TAIL - Z_HEAD)


def slumbler_tadpole() -> Model:
    """The Slumbler's tadpole: a little stingray of the Chrome (SlumblerTadpoleModel animates it). A
    flat diamond body in rainbow scales over a pearly belly, a raised back with two gold eyes, two
    wide wing-fins (each with an outer half that ripples), a blunt snout with two curled lobes, small
    pelvic fins and a long whip of a tail with a tiny fin and a glowing tip."""
    pal = palette()
    m = Model('slumbler_tadpole', (64, 64), pal, {'slumbler_tadpole': {}}, res=2)
    belly = dict(color='belly', pattern='mc', clusters=0.2)
    fin = dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=3, accent='fin_d')
    body = m.part('body', pivot=(0, 22.6, 0))
    o, sz = (-2.6, -1.0, -3.6), (5.2, 1.8, 7.2)
    body.cube(o, sz, color='h3', pattern='mc', faces=scaled(o, sz, 0, zmap=_tad_z, down=belly))
    o, sz = (-1.6, -1.6, -2.8), (3.2, 0.6, 5.4)
    body.cube(o, sz, color='h3', pattern='mc', faces=scaled(o, sz, 0, faces=('north', 'south', 'east', 'west', 'up'), zmap=_tad_z,
                                                            down=dict(skip=True)))
    for side, sx in (('left', 1), ('right', -1)):
        # a gold eye on top of the head, a highlight on its upper inner corner
        body.cube((0.75 * sx - 0.45, -2.1, -2.6), (0.9, 0.5, 0.9), color='iris', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='iris', pattern='mc', clusters=0.0, rim=False, hd=True, keys=EYE_KEYS, map=['iP', 'PP'] if sx > 0 else ['Pi', 'PP'])})
        # the wing-fin: an inner half in scales, an outer half of fin that ripples
        wing = body.part(f'{side}_wing', pivot=(2.6 * sx, -0.2, 0.0))
        o, sz = ((0 if sx > 0 else -3.4), -0.3, -3.2), (3.4, 0.6, 6.2)
        wing.cube(o, sz, color='h3', pattern='mc', faces=scaled(o, sz, 0, faces=('up', 'north', 'south', 'east', 'west'), zmap=_tad_z, down=belly))
        tip = wing.part(f'{side}_wing_tip', pivot=(3.4 * sx, 0, 0.3))
        tip.cube(((0 if sx > 0 else -2.8), -0.2, -2.4), (2.8, 0.4, 4.4), **fin, faces={'down': dict(belly, color='belly_d')})
        # the little pelvic fins at the root of the tail
        pf = body.part(f'{side}_pelvic', pivot=(1.4 * sx, 0.3, 3.2), rot=(0, 0.5 * sx, 0))
        pf.cube(((0 if sx > 0 else -1.4), -0.15, 0), (1.4, 0.3, 1.6), **fin)
    snout = body.part('snout', pivot=(0, -0.2, -3.6))
    o, sz = (-1.9, -0.6, -1.6), (3.8, 1.2, 1.6)
    snout.cube(o, sz, color='h0', pattern='mc', faces=scaled(o, sz, -3.6, faces=('east', 'west', 'up'), zmap=_tad_z, down=belly,
                                                             north=dict(color='h0', pattern='mc', clusters=0.0, hd=True, keys={'m': 'mouth_d'},
                                                                        map=['........', '........', '.mmmmmm.', '........'])))
    for side, sx in (('left', 1), ('right', -1)):
        lobe = snout.part(f'{side}_lobe', pivot=(1.5 * sx, 0.1, -1.4), rot=(0, 0.3 * sx, 0))
        lobe.cube((-0.35, -0.3, -1.6), (0.7, 0.6, 1.6), color='h0', pattern='mc', clusters=0.2, faces={'down': belly})
    t1 = body.part('tail1', pivot=(0, -0.3, 3.6))
    o, sz = (-0.6, -0.45, 0), (1.2, 0.9, 3.4)
    t1.cube(o, sz, color='h4', pattern='mc', faces=scaled(o, sz, 3.6, zmap=_tad_z, down=belly))
    t2 = t1.part('tail2', pivot=(0, 0, 3.4))
    o, sz = (-0.4, -0.3, 0), (0.8, 0.6, 3.4)
    t2.cube(o, sz, color='h5', pattern='mc', faces=scaled(o, sz, 7.0, zmap=_tad_z, down=belly))
    t3 = t2.part('tail3', pivot=(0, 0, 3.4))
    t3.cube((-0.25, -0.2, 0), (0.5, 0.4, 2.8), color='h6', pattern='mc', clusters=0.2)
    t3.cube((0, -1.0, 0.4), (0, 1.0, 2.2), **fin)
    t3.cube((-0.35, -0.35, 2.6), (0.7, 0.7, 0.7), color='gill_l', pattern='mc', clusters=0.0, rim=False, glow=True)
    return m


def slumbler_eggs() -> Model:
    """A clutch of Slumbler eggs (SlumblerEggsModel wobbles it): a raft of clear jelly eggs floating
    together, each with a rainbow tadpole curled up inside. Each egg's yolk is its first cube and the
    jelly a child drawn after it, so the yolk shows through the translucent jelly."""
    pal = palette()
    pal.update({'jelly': '#d6f0ff', 'jelly_l': '#f6fcff', 'jelly_d': '#9fc8e2'})
    m = Model('slumbler_eggs', (64, 64), pal, {'slumbler_eggs': {}}, res=2)
    raft = m.part('raft', pivot=(0, 24, 0))
    spots = ((0.0, 0.0, 0.0, 3.4), (3.4, 0.3, 1.0, 2.9), (-3.3, 0.2, 0.8, 3.0), (1.2, 0.3, -3.1, 2.9), (-1.7, 0.2, 3.3, 2.8),
             (2.5, 0.4, 3.7, 2.4), (-3.0, 0.4, -2.7, 2.5))
    for i, (x, y, z, d) in enumerate(spots):
        egg = raft.part(f'egg_{i}', pivot=(x, y - d / 2, z))
        egg.cube((-0.7, -0.5, -0.8), (1.4, 1.0, 1.6), color=f'h{i % 7}', pattern='mc', clusters=0.0, rim=False)
        jelly = egg.part(f'jelly_{i}')
        jelly.cube((-d / 2, -d / 2, -d / 2), (d, d, d), color='jelly', pattern='mc', clusters=0.3, rim=False, opacity=120, faces={
            'up': dict(color='jelly_l', pattern='mc', clusters=0.3, rim=False, opacity=150)})
    return m


EXTRA = {'slumbler_tadpole': slumbler_tadpole, 'slumbler_eggs': slumbler_eggs}
MODELS = {'slumbler': slumbler}


# ---------------------------------------------------------------- CR2: item sprites (items16.all_items)

# the tadpole peeking out of a bucket: a wing-fin curling up out of the water, the whip of its tail
# hanging over the rim (16 x 7, laid over the bucket's mouth like the music fish)
TADPOLE_IN_BUCKET = ([
    '.......wW.......',
    '......wWa.......',
    '..e.wRRYGGa.....',
    '.oRRYYGGBBVv....',
    '..pppppppVVttt..',
    '............tg..',
    '................',
], {'w': ('#a99fb2', '#4e4458'), 'W': ('#c2b9c9', '#4e4458'), 'a': ('#7e7489', '#4e4458'), 'e': '#15100f', 'o': ('#9c6a68', '#4a2a2a'),
    'R': ('#9c6a68', '#4a2a2a'), 'Y': ('#8f8753', '#3a3418'), 'G': ('#678660', '#2a3a20'), 'B': ('#58817f', '#1a3030'),
    'V': ('#76628a', '#2e2440'), 'v': ('#5c6a8f', '#262c40'), 'p': ('#d6cbbd', '#6a5e52'), 't': ('#76628a', '#2e2440'),
    'g': ('#9c6a68', '#4a2a2a')})


class _bucket_layer:
    """Lends the tadpole to the music fishes' bucket painters (tools/fish_items.py, tools/chrome.py) for one call."""

    def __enter__(self):
        import fish_items as FI
        FI.BUCKET_FISH['slumbler_tadpole'] = TADPOLE_IN_BUCKET
        return self

    def __exit__(self, *exc):
        import fish_items as FI
        FI.BUCKET_FISH.pop('slumbler_tadpole', None)
        return False


def slumbler_gill():
    """A Slumbler's gill: a frilled rose plume on a pale bony stalk, its filaments combed off a pale
    rachis like a feather's barbs. Light from the top left like every vanilla item."""
    import items16 as I
    rows = ['................',
            '.......l.l......',
            '.....l.glg.l....',
            '....lgGggGggl...',
            '...lGggGggGgg...',
            '..lggGggGggrg...',
            '..gGggGggGrgd...',
            '..ggGggGgrGd....',
            '...ggGggrgd.....',
            '....dgGrdd......',
            '.....ddbd.......',
            '......bB........',
            '.....bB.........',
            '....bB..........',
            '...bb...........',
            '................']
    ol = '#3e1a28'
    pal = {'l': ('#cf8196', ol), 'G': ('#c06c84', ol), 'g': ('#a0506a', ol), 'd': ('#83405a', ol), 'r': ('#d6cbbd', ol),
           'b': ('#b3a697', ol), 'B': ('#d6cbbd', ol)}
    return I.grid(rows, pal, ol=True)


def items():
    import fish_items as FI
    import items16 as I
    o = {'slumbler_gill': slumbler_gill()}
    with _bucket_layer():
        o['slumbler_tadpole_bucket'] = FI.water_bucket('slumbler_tadpole')
    # the Slumbler as it looks now: a dusky-rose head under the wooden back, small dark eyes on low bumps,
    # pink gill fans to the sides, the wide mouth over its pale jaw with barbels, the scales' sheen below
    o['slumbler_spawn_egg'] = I.egg(['#5e4048', '#7d5656', '#9c6a68', '#b08480'], '#2a1c22', {
        1: '.....wwwwww.....', 2: '....wGwwwGww....', 3: '....wwGwwwGw....', 4: '....ewwwwwwe....',
        5: '.gg..........gg.', 6: 'g.g..oP..Po..g.g', 7: '.gg..........gg.',
        9: '...mmmmmmmmmm...', 10: '...tttttttttt...', 11: '..s..tttttt..s..', 12: '..s.qqqqqqqq.s..', 13: '....vvvvvvvv....'},
        pal={'w': ('#a87442', '#4a2c14'), 'G': ('#8a5a30', '#4a2c14'), 'e': ('#1c0e0a', '#4a2c14'), 'o': '#b08480', 'P': '#15100f',
             'g': ('#b8607a', '#4e2232'), 'm': '#4a2030', 't': '#d6cbbd', 's': '#d8cdb0', 'q': '#58817f', 'v': '#76628a'}, no_ol='s')
    # the tadpole: a little olive ray over the water, pale fins spread, small dark eyes with amber rims on top,
    # its rose snout below and the banded tail trailing down
    o['slumbler_tadpole_spawn_egg'] = I.egg(['#4a4256', '#5c6a8f', '#58817f', '#678660'], '#241e2c', {
        5: '.....aP..Pa.....', 6: '...cYYYYYYYYc...', 7: '.ffYYYGYYGYYYff.', 8: 'fffYYYYYYYYYYfff', 9: '.ffYYGYYYYGYYff.',
        10: '....pYYYYYYp....', 11: '.....pppppp.....', 12: '.......B........', 13: '.......V........'},
        pal={'a': '#c9a03c', 'P': '#15100f', 'Y': ('#8f8753', '#3a3418'), 'G': ('#678660', '#3a3418'), 'c': ('#9d7a55', '#3a2a14'),
             'f': ('#a99fb2', '#4e4458'), 'p': ('#9c6a68', '#4a2a2a'), 'B': ('#58817f', '#1a3030'), 'V': ('#76628a', '#2e2440')},
        no_ol='aP', ring='water')
    return o


# ---------------------------------------------------------------- CR2: sounds, loot, tags and text (gen_assets.generate)

SOUNDS = {
    'entity.slumbler.eat': [('mob/frog/eat1', 1.0, 0.6), ('mob/frog/eat2', 1.0, 0.55), ('mob/frog/eat3', 1.0, 0.6)],
    'entity.slumbler.spit': [('mob/llama/spit1', 1.0, 0.7), ('mob/llama/spit2', 1.0, 0.65), ('mob/dolphin/splash1', 0.6, 1.2)],
    'entity.slumbler.shake': [('mob/wolf/shake', 1.0, 0.6), ('mob/dolphin/splash2', 0.7, 0.9)],
    'entity.slumbler.lay': [('mob/frog/lay_spawn1', 1.0, 0.8), ('mob/frog/lay_spawn2', 1.0, 0.75)],
    'entity.chrome_spit.splash': [('mob/dolphin/splash1', 0.9, 1.3), ('mob/dolphin/splash2', 0.9, 1.4), ('mob/dolphin/splash3', 0.9, 1.35)],
    'entity.slumbler_eggs.hatch': [('block/frogspawn/hatch1', 1.0, 0.9), ('block/frogspawn/hatch2', 1.0, 0.95), ('block/frogspawn/hatch3', 1.0, 0.9)],
    'entity.slumbler_tadpole.bite': [('mob/axolotl/attack1', 0.8, 1.4), ('mob/axolotl/attack2', 0.8, 1.5)],
    'entity.slumbler_tadpole.hurt': [('mob/tadpole/hurt1', 1.0, 0.9), ('mob/tadpole/hurt2', 1.0, 0.9), ('mob/tadpole/hurt3', 1.0, 0.85)],
    'entity.slumbler_tadpole.death': [('mob/tadpole/death1', 1.0, 0.9), ('mob/tadpole/death2', 1.0, 0.85)],
    'entity.slumbler_tadpole.flop': [('entity/fish/flop1', 0.8, 1.2), ('entity/fish/flop2', 0.8, 1.25), ('entity/fish/flop4', 0.8, 1.2)],
    # crash cymbals: a splashy wash of breaking glass over a ringing bell
    'entity.slumbler_tadpole.crash': [('random/glass1', 0.7, 1.35), ('random/glass2', 0.7, 1.4), ('random/glass3', 0.7, 1.3),
                                      ('block/bell/bell_use01', 0.4, 1.9)],
}
SUBTITLES = {
    'entity.slumbler.eat': 'Slumbler gulps a fish', 'entity.slumbler.spit': 'Slumbler spits Chrome', 'entity.slumbler.shake': 'Slumbler shakes itself dry',
    'entity.slumbler.lay': 'Slumbler lays eggs', 'entity.chrome_spit.splash': 'Chrome splashes', 'entity.slumbler_eggs.hatch': 'Slumbler eggs hatch',
    'entity.slumbler_tadpole.bite': 'Slumbler tadpole bites', 'entity.slumbler_tadpole.hurt': 'Slumbler tadpole hurts',
    'entity.slumbler_tadpole.death': 'Slumbler tadpole dies', 'entity.slumbler_tadpole.flop': 'Slumbler tadpole flops',
    'entity.slumbler_tadpole.crash': 'Cymbals crash',
}


def assets(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    tag, rl = GA.tag, GA.rl
    tag('entity_type', 'minecraft:aquatic', rl('slumbler_tadpole'))
    tag('entity_type', f'{GA.NS}:chrome_dwellers', rl('slumbler_tadpole'))
    import gen_data as GD
    GD.table('entity', 'entities/slumbler_tadpole', [GD.pool([GD.item('minecraft:prismarine_crystals', count=(0, 1))], condition=GD.chance(0.3))])
    GA.LANG.update({
        f'entity.{GA.NS}.slumbler_tadpole': 'Slumbler Tadpole',
        f'entity.{GA.NS}.slumbler_eggs': 'Slumbler Eggs',
        f'entity.{GA.NS}.chrome_spit': 'Chrome Spit',
        f'band.{GA.NS}.instrument.crash_cymbals': 'Crash Cymbals',
        f'codex.{GA.NS}.slumbler_tadpole.title': 'Slumbler Tadpole',
        f'codex.{GA.NS}.slumbler_tadpole.tagline': 'Aggressive - tame it with fish and drums',
        f'codex.{GA.NS}.slumbler_tadpole.body': 'Feed two Slumblers fish and they lay a clutch of jelly eggs on the Chrome; a few minutes later '
                                                'two to four tadpoles swim out - little stingrays in their parents\' rainbow scales, with gold '
                                                'eyes and a whip of a tail. They hunt and eat fish, and bite anyone swimming in their pool. Catch '
                                                'one in a Chrome Bucket or a water bucket like any fish. Feed one plenty of fish and it stops '
                                                'biting you; then play it a drum, and it is yours: it follows you through the water and joins your '
                                                'band on crash cymbals.',
    })

