"""B4 gear art: the angelic Siftite tools and the Seraphim Prism gear, hand-drawn at 16x16 in the
items16.py style (rows of characters, one per pixel, outlined by items16.grid), plus the worn
Prism armour layers to match and fresh silhouettes for the newer instruments and weapons.

Siftite = the angel: white-gold and pale-cyan metal, feathered wing blades, halo rings.
Prism   = the Seraphim: six wings, many eyes, a radiant rainbow of hexagonal gem facets.

RR: the Siftite tools are gone (Siftite is a material only), and their icons with them.
gen_textures.main() calls textures(out) last,
which writes the Prism gear, the prism tools and the reworked instruments over any older drawings
of the same names.
"""
from __future__ import annotations

import colorsys

from PIL import Image

import items16 as I

# ============================================================================ palettes

G_OL = '#4a2418'

P_OL = '#24203f'


def _hue(h, s=0.55, v=1.0):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return '#%02x%02x%02x' % (round(r * 255), round(g * 255), round(b * 255))


# the six wing colours of the Seraphim, red round to violet (one per wing pair, light and shadow)
RAINBOW = [0.98, 0.07, 0.15, 0.36, 0.52, 0.72]
PRISM = {
    # pale pearl-silver metal
    '1': ('#4a4672', P_OL), '2': ('#8a88b4', P_OL), '3': ('#c4c4de', P_OL), '4': ('#ecebf8', P_OL), '5': ('#ffffff', P_OL),
    # rainbow facets: A..F light, a..f shadow
    **{k: (_hue(h, 0.5, 1.0), P_OL) for k, h in zip('ABCDEF', RAINBOW)},
    **{k: (_hue(h, 0.78, 0.8), P_OL) for k, h in zip('abcdef', RAINBOW)},
    # eyes: white, iris, pupil
    'w': ('#ffffff', P_OL), 'k': ('#2a1648', P_OL), 'o': ('#ffd54a', P_OL),
    # gold
    'G': ('#a6622a', G_OL), 'g': ('#e3a73c', G_OL), 'y': ('#ffe48a', G_OL),
    # pearl grip
    'i': ('#d8d0ec', '#221c34'), 'I': ('#8c80b0', '#221c34'),
    'h': '#fff6c8', 's': '#ffffff',
}
PRISM_NO_OL = 'hs'


def _grid(rows, pal, no_ol='hs'):
    """items16.grid with outlines; '_' marks a hole that must stay clear (a halo's middle)."""
    img = I.grid([r.replace('_', '.') for r in rows], pal, ol=True, no_ol=no_ol)
    px = img.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == '_':
                px[x, y] = (0, 0, 0, 0)
    return img


def _fit(rows):
    """Clips or pads each row to 16 pixels (so a drawing can be sketched a pixel wide)."""
    return [(r + '.' * 16)[:16] for r in rows]


# ============================================================================ worn armour

def _faces(u, v, w, h, d):
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'west': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
            'east': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}


def _put(px, x, y, c):
    px[x, y] = I.rgba(c)


def _plate(px, rect, top, body, bottom, trim=None):
    fx, fy, fw, fh = rect
    for y in range(fh):
        for x in range(fw):
            c = body
            if y == 0 or x == 0:
                c = top
            elif y == fh - 1 or x == fw - 1:
                c = bottom
            _put(px, fx + x, fy + y, c)
    if trim:
        for x in range(fw):
            _put(px, fx + x, fy + fh - 1, trim)


# ============================================================================ Prism (Seraphim) icons

PRISM_ROWS = {
    # a straight pearl blade with a rainbow core, its guard six rainbow feathers fanned round an eye
    'prism_sword': [
        '..............5F',
        '.............5F3',
        '............5F32',
        '...........5E32.',
        '..........5E32..',
        '.........5D32...',
        '........5D32....',
        '.......5C32.....',
        '.b.c..5C32......',
        '..BC.5B32.......',
        '.aAgwk32........',
        '...ykwDd........',
        '...iFE..........',
        '..iIf.e.........',
        '.iI.............',
        'wk..............',
    ],
    # two straight wings in a chevron, banded red to violet, an eye at the boss
    'prism_pickaxe': [
        '................',
        '.5AAABBBCC555...',
        'aAAABBBCCC4wk4..',
        '.a.ab.bc.Ii4kw4.',
        '........Ii..4DDd',
        '.......Ii..d4DDd',
        '......Ii....4Dd.',
        '.....Ii.....4EEe',
        '....Ii.....e4EEe',
        '...Ii.......4Ee.',
        '..Ii........4FFf',
        '.Ii........f4FFf',
        'Ii..........4Ff.',
        'k............f..',
        '................',
        '................',
    ],
    # a labrys of wings: two rainbow wings back to back on the haft, an eye where they meet
    'prism_axe': [
        '...........F....',
        '.........FfEF...',
        '..A.....fEEeDE..',
        '.aBA....eDDdwkD.',
        '.bCBA..eCCcwk5C.',
        '..cCBBdBBb.4CBB.',
        '...dDDCbAa..aAA.',
        '....eEdIA....aa.',
        '.....FIi........',
        '....Ii..........',
        '...Ii...........',
        '..Ii............',
        '.Ii.............',
        'Ii..............',
        'k...............',
        '................',
    ],
    # a hexagonal gem for a spade, cut in six rainbow facets round an open eye
    'prism_shovel': [
        '..........555...',
        '........55AAB4..',
        '.......5FAABBC3.',
        '.......5FFwwBC3.',
        '.......4FwkkwC2.',
        '.......4EEwwDC2.',
        '.......3EEDDDC2.',
        '........32DD22..',
        '........gy222...',
        '.......IgG......',
        '.....Ii.........',
        '....Ii..........',
        '...Ii...........',
        '..Ii............',
        '.Ii.............',
        'wk..............',
    ],
    # a sickle of one rainbow wing hooking over from the haft's crown, an eye at its root
    'prism_hoe': [
        '................',
        '..........5AA...',
        '.........5ABBb..',
        '........5wkBCCc.',
        '........4kwa.CDd',
        '.......Ii....DEe',
        '......Ii.....eEF',
        '.....Ii.......Ff',
        '....Ii.........f',
        '...Ii...........',
        '..Ii............',
        '.Ii.............',
        'Ii..............',
        'wk..............',
        'k...............',
        '................',
    ],
    # the Seraph's helm: a pearl dome watching with three eyes, a rainbow wing folded over each
    # cheek and a halo above
    'prism_helmet': [
        '.....hhhhhh.....',
        'A...h......h...A',
        'BA...hhhhhh...AB',
        'CBA.55555554.ABC',
        '.CB5wk4wk4wk3BC.',
        '..D5444444433D..',
        '.ED4333333332DE.',
        'FE.3gyyyyyyg3.EF',
        'F..2E......E2..F',
        '...1D......D1...',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
    ],
    # three pairs of wings - over the shoulders, round the ribs and folded at the waist - a great
    # eye at the heart and smaller eyes watching from the wings
    'prism_chestplate': [
        '................',
        'AAa45......54aAA',
        'BwkBa4....4aBkwB',
        'CBBCb34..43bCBBC',
        '.cCDc4yyyy4cDCc.',
        '..dDE4g55g4EDd..',
        '...eE45wk54Ee...',
        '..eEF4wkko4FEe..',
        '.fFF.45ww54.FFf.',
        '.aA..4344434.Aa.',
        '..aAa4F33F4aAa..',
        '...bB4wk4wk4Bb..',
        '....43333334....',
        '....4C3333D4....',
        '....2gE22Fg2....',
        '................',
    ],
    # a hex-gem belt, an eye on each thigh, rainbow-edged greaves
    'prism_leggings': [
        '................',
        '................',
        '...gABCDEFAgg...',
        '...G5444443gG...',
        '...54wk3.4wk4...',
        '...443A...4A3...',
        '...443B...4B3...',
        '...443C...4C3...',
        '...443D...4D3...',
        '...44E3...4E3...',
        '...4F33...4F3...',
        '...wk33...wk3...',
        '...433A...43A...',
        '...222a...22a...',
        '................',
        '................',
    ],
    # winged at every heel, an eye on each toe
    'prism_boots': [
        '................',
        '................',
        '................',
        '................',
        'A..............A',
        'aB............Ba',
        'bCB5443..5443BCb',
        '.cDy443..544yDc.',
        '..dEE33..54EEd..',
        '...eF33..54F3...',
        '...4433..5433...',
        '..5wk33..5wk333.',
        '..44333..443333.',
        '..22222..222222.',
        '................',
        '................',
    ],
}


def prism_sprites():
    return {n: _grid(_fit(r), PRISM) for n, r in PRISM_ROWS.items()}


PW = {'ol': P_OL, 'm_lo': '#8a88b4', 'm': '#c4c4de', 'm_hi': '#ecebf8', 'hi': '#ffffff',
      'g_lo': '#a6622a', 'g': '#e3a73c', 'g_hi': '#ffe48a', 'iris': '#2a1648', 'gold_iris': '#ffd54a'}


def _rb(i, light=True):
    h = RAINBOW[i % 6]
    return _hue(h, 0.5, 1.0) if light else _hue(h, 0.78, 0.8)


def _eye(px, x, y, c=PW):
    """A small watching eye: a white almond with a dark pupil, a gold lid above."""
    for dx, col in ((0, c['m_lo']), (1, c['hi']), (2, c['iris']), (3, c['hi']), (4, c['m_lo'])):
        _put(px, x + dx, y, col)
    for dx in (1, 2, 3):
        _put(px, x + dx, y - 1, c['g'] if dx == 2 else c['g_hi'])


def _eye2(px, x, y, c=PW):
    """The narrowest eye, for 4-pixel faces: white and pupil, a gold lash above."""
    _put(px, x, y, c['hi'])
    _put(px, x + 1, y, c['iris'])
    _put(px, x, y - 1, c['g_hi'])
    _put(px, x + 1, y - 1, c['g'])


def _rainbow_wing(px, ox, oy, w, h, hue, mirror=False, c=PW):
    """A seraph's wing painted on a face: a gold root, rainbow feathers whose tips step down
    toward the far end, banded through two neighbouring hues."""
    for x in range(w):
        xx = w - 1 - x if mirror else x
        reach = 1 + (h - 2) * (x + 1) // w
        for y in range(h):
            if y > reach:
                continue
            band = hue + (1 if y > reach // 2 + 1 else 0)
            if y == 0:
                col = c['g'] if x == 0 else _rb(band, True)
            elif y == reach:
                col = c['ol'] if x % 2 else _rb(band, False)
            else:
                col = _rb(band, x % 2 == 0)
            _put(px, ox + xx, oy + y, col)


def prism_layers():
    """The worn Seraphim armour: a pearl helm watching with three eyes, rainbow wings folded
    over its cheeks and a gold halo; six wings down the back in three pairs, more over the
    shoulders and ribs, a great eye on the breast; eyes on the arms, thighs and boots."""
    c = PW
    hum = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    hp = hum.load()
    head = _faces(0, 0, 8, 8, 8)
    for k, r in head.items():
        _plate(hp, r, c['m_hi'], c['m'], c['m_lo'])
    fx, fy, fw, fh = head['north']
    _eye(hp, fx + 1, fy + 2)
    for x in range(fw):
        _put(hp, fx + x, fy + 3, _rb(x, True) if x % 2 else c['g'])
    for (x, y) in ((1, 4), (2, 4), (5, 4), (6, 4)):
        hp[fx + x, fy + y] = (0, 0, 0, 0)
    for y in range(5, 8):
        for x in range(2, 6):
            hp[fx + x, fy + y] = (0, 0, 0, 0)
    for side, hue in (('west', 0), ('east', 0)):
        sx, sy, sw, sh = head[side]
        _rainbow_wing(hp, sx, sy, sw, 8, hue, mirror=(side == 'west'))
    bx, by, bw, bh = head['south']
    _rainbow_wing(hp, bx, by + 1, 4, 7, 4, mirror=True)
    _rainbow_wing(hp, bx + 4, by + 1, 4, 7, 4)
    ux, uy, uw, uh = head['up']
    _eye(hp, ux + 1, uy + 4)
    # halo: a gold ring with rainbow sparks on the hat layer's crown
    hx, hy, hw, hh = _faces(32, 0, 8, 8, 8)['up']
    k = 0
    for y in range(hh):
        for x in range(hw):
            if (x in (0, 7) and 1 < y < 6) or (y in (0, 7) and 1 < x < 6) or (x, y) in ((1, 1), (6, 1), (1, 6), (6, 6)):
                _put(hp, hx + x, hy + y, _rb(k // 2, True) if k % 4 == 0 else c['g_hi'])
                k += 1
    # body: the great eye on the breast, wings over the ribs; six wings on the back
    body = _faces(16, 16, 8, 12, 4)
    for kk, r in body.items():
        _plate(hp, r, c['m_hi'], c['m'], c['m_lo'])
    nx, ny, nw, nh = body['north']
    for (x, y) in ((2, 2), (3, 1), (4, 1), (5, 2), (1, 4), (6, 4), (2, 6), (3, 7), (4, 7), (5, 6)):
        _put(hp, nx + x, ny + y, c['g_hi'] if y < 4 else c['g'])
    for (x, y, col) in ((1, 3, c['m_lo']), (2, 3, c['hi']), (3, 3, c['hi']), (4, 3, c['hi']), (5, 3, c['hi']), (6, 3, c['m_lo']),
                        (2, 4, c['hi']), (3, 4, c['iris']), (4, 4, c['iris']), (5, 4, c['hi']),
                        (2, 5, c['m_lo']), (3, 5, c['gold_iris']), (4, 5, c['iris']), (5, 5, c['m_lo'])):
        _put(hp, nx + x, ny + y, col)
    for x in range(nw):
        _put(hp, nx + x, ny + 9, _rb(x, x % 2 == 0))
        _put(hp, nx + x, ny + nh - 1, c['g'] if x % 2 else c['g_lo'])
    sx, sy, sw, sh = body['south']
    for i, top in enumerate((0, 4, 8)):  # three pairs: upper, middle, lower
        _rainbow_wing(hp, sx, sy + top, 4, 4, i * 2, mirror=True)
        _rainbow_wing(hp, sx + 4, sy + top, 4, 4, i * 2)
    for side in ('west', 'east'):
        r = body[side]
        _rainbow_wing(hp, r[0], r[1] + 2, r[2], 5, 2, mirror=(side == 'west'))
        _eye2(hp, r[0] + 1, r[1] + 9)
    # arms: rainbow feather pauldrons, an eye on every face of the vambrace
    arm = _faces(40, 16, 4, 12, 4)
    for kk, r in arm.items():
        _plate(hp, r, c['m_hi'], c['m'], c['m_lo'])
        if kk in ('up', 'down'):
            continue
        _rainbow_wing(hp, r[0], r[1], r[2], 5, 0)
        for x in range(r[2]):
            _put(hp, r[0] + x, r[1] + 5, c['g'] if x % 2 else c['g_hi'])
        _eye2(hp, r[0] + 1, r[1] + 8)
    ux, uy, uw, uh = arm['up']
    for y in range(uh):
        for x in range(uw):
            _put(hp, ux + x, uy + y, _rb(y, (x + y) % 2 == 0))
    # boots: pearl greaves, a rainbow wing at each heel, an eye on the toe
    leg = _faces(0, 16, 4, 12, 4)
    for kk, r in leg.items():
        if kk == 'up':
            continue
        rx, ry, rw, rh = r
        top = 0 if kk == 'down' else 6
        for y in range(top, rh):
            for x in range(rw):
                col = c['m_hi'] if (y == top or x == 0) else c['m_lo'] if (y == rh - 1 or x == rw - 1) else c['m']
                _put(hp, rx + x, ry + y, col)
        if kk == 'down':
            continue
        for x in range(rw):
            _put(hp, rx + x, ry + 6, _rb(x + 3, True) if x % 2 else c['g'])
        if kk in ('west', 'east'):
            _rainbow_wing(hp, rx, ry + 7, rw, 3, 4, mirror=(kk == 'west'))
        if kk == 'north':
            _eye2(hp, rx + 1, ry + 10)
    lg = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    lp = lg.load()
    for kk, r in _faces(16, 16, 8, 12, 4).items():
        rx, ry, rw, rh = r
        if kk in ('up', 'down'):
            _plate(lp, r, c['m_hi'], c['m'], c['m_lo'])
            continue
        for y in range(7, rh):
            for x in range(rw):
                col = c['g'] if y == 7 else _rb(x // 2 + y, (x % 2) == 0) if y == 8 else c['m'] if y < rh - 1 else c['m_lo']
                _put(lp, rx + x, ry + y, col)
    for kk, r in _faces(0, 16, 4, 12, 4).items():
        rx, ry, rw, rh = r
        if kk in ('up', 'down'):
            _plate(lp, r, c['m_hi'], c['m'], c['m_lo'])
            continue
        for y in range(rh):
            for x in range(rw):
                col = c['m_hi'] if (y == 0 or x == 0) else c['m_lo'] if x == rw - 1 else c['m']
                _put(lp, rx + x, ry + y, col)
        for y in range(rh):
            _put(lp, rx + (rw - 1 if kk in ('north', 'east') else 0), ry + y, _rb(y // 2, True))
        if kk in ('north', 'south'):
            _eye2(lp, rx + 1, ry + 3)
        for x in range(rw):
            _put(lp, rx + x, ry + 7, c['g'])
    return {'entity/equipment/humanoid/prism': hum, 'entity/equipment/humanoid_leggings/prism': lg}


# ============================================================================ instruments and weapons

INSTRUMENT_ROWS = {
    # the Seraph's flute: a pearl tube, rainbow gems for finger holes, a pair of little rainbow
    # wings at the mouthpiece and an eye watching from the gold bell
    'prism_flute': [
        '..........ba....',
        '..........bAgy5.',
        '...........gy43C',
        '...........443cD',
        '..........4A2..d',
        '.........432....',
        '........4B2.....',
        '.......432......',
        '......4C2.......',
        '.....432........',
        '....4D2.........',
        '...432..........',
        '..gE2...........',
        '.gyg............',
        'wkg.............',
        'k...............',
    ],
    # a winged harp: a pearl pillar crowned with an eye, its neck a rainbow wing, white strings
    'prism_harp': [
        '..wk............',
        '.gyAABBCC.......',
        '.g5aabbccDD.....',
        '.g4_s_s_s_dEE...',
        '.g4_s_s_s_s_eFF.',
        '.g4_s_s_s_s_s5f.',
        '.g4_s_s_s_s_s43.',
        '.g4_s_s_s_s_43..',
        '.g4_s_s_s_s43...',
        '.g4_s_s_s_43....',
        '.g4_s_s_s43.....',
        '.g4_s_s_43......',
        '.g4_s_s43.......',
        '.g4_s_43........',
        '.g4_s43.........',
        '.Gyyy3..........',
    ],
    # a conga in pearl and gold, an eye in its waist band and three rainbow wings on each side
    'prism_drum': [
        '................',
        '.....555544.....',
        '....5ssssss3....',
        '....gyyyyyyg....',
        'A...54433332...A',
        'aB.g4A3B3C32g.Ba',
        'bCBg4w3kk3w2gBCb',
        '.cDg43wkkw32gDc.',
        '..dEg433332gEd..',
        '...e.gyyyyg.e...',
        '.....4D33E2.....',
        '.....43F332.....',
        '.....433332.....',
        '.....gyyyyg.....',
        '......2222......',
        '................',
    ],
}

# the Weaver's Guitar: a black-widow abdomen for a body (a glowing hourglass on it), bone spider
# legs splayed from its sides, a bone neck and a spider's head for a headstock
WEAVER_ROWS = [
    '.............l.l',
    '...........lHHl.',
    '..........lpHpH.',
    '...........HHl..',
    '..........gN....',
    '.l.......gN.....',
    '..l.....gN......',
    '..laab.gN..l....',
    '.laabbgNcll.....',
    'l.aabGgbc.......',
    '..abGoGbc.l.....',
    '.labbGbcc..l....',
    'l.abbbcc........',
    '..lbccc.l.......',
    '.l..l...l.......',
    '................',
]
WEAVER_PAL = {'a': ('#4a7480', '#04090c'), 'b': ('#2c4e59', '#04090c'), 'c': ('#173039', '#04090c'),
              'o': ('#9ffbff', '#04090c'), 'G': ('#0f6e7a', '#04090c'), 'N': ('#d1d6b6', '#3a4440'),
              'H': ('#16222a', '#04090c'), 'p': ('#29dfeb', '#04090c'), 'l': ('#bbc39b', '#3a4440'), 'g': ('#c8fffa', '#04090c')}


def cannonball():
    """The Cannonball: a heavy iron ball shaded in five hard tones from a top-left glint, a riveted
    iron band round its middle and a brass fuse plug with a spark on top."""
    tones = ['#121418', '#22262c', '#363b43', '#4f5660', '#78808c']
    rows = []
    cx, cy, r = 7.5, 8.5, 6.2
    for y in range(16):
        row = ''
        for x in range(16):
            dx, dy = x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5
            d = (dx * dx + dy * dy) ** 0.5
            if d > r:
                row += '.'
                continue
            lit = -(dx * 0.62 + dy * 0.78) / r + (1 - d / r) * 0.6  # facing the top-left light
            k = 0 if lit < -0.35 else 1 if lit < 0.0 else 2 if lit < 0.35 else 3 if lit < 0.7 else 4
            row += '01234'[k]
        rows.append(row)
    rows = [list(r_) for r_ in rows]
    for x in range(16):  # the iron band and its rivets
        if rows[9][x] != '.':
            rows[9][x] = 'r' if x % 3 == 1 else 'b'
            rows[10][x] = 'B' if rows[10][x] != '.' else '.'
    rows[3][9] = 'H'
    rows[4][4] = 'H'
    rows[4][5] = 'h'
    rows[5][4] = 'h'
    rows[2][7], rows[2][8] = 'f', 'F'  # the fuse plug
    rows[1][8] = 'y'
    rows[0][9] = 's'
    rows = [''.join(r_) for r_ in rows]
    pal = {str(i): (t, '#08090b') for i, t in enumerate(tones)}
    pal.update({'b': ('#5a6068', '#08090b'), 'B': ('#26292e', '#08090b'), 'r': ('#a8b0bc', '#08090b'),
                'H': '#f0f4f8', 'h': '#a8b0bc', 'f': ('#c9862c', '#4a2418'), 'F': ('#eebd4a', '#4a2418'),
                'y': '#ffd54a', 's': '#fff6c8'})
    return I.grid(rows, pal, ol=True, no_ol='Hhys')


def textures(out):
    """Called last from gen_textures.main(): the Seraphim prism gear (icons and worn layers), the
    prism tools, the gem-inlaid instruments, the Weaver's Guitar and the Cannonball."""
    for name, img in prism_sprites().items():
        out(f'item/{name}', img)
    for name, img in prism_layers().items():
        out(name, img)
    for name, rows in INSTRUMENT_ROWS.items():
        out(f'item/{name}', _grid(_fit(rows), {**PRISM, 's': ('#ffffff', P_OL)} if name != 'prism_harp' else PRISM))
    out('item/weaver_guitar', _grid(WEAVER_ROWS, WEAVER_PAL, no_ol=''))
    out('item/cannonball', cannonball())


# ============================================================================ data (gen_data.generate)

PRISM_TOOLS = {'sword': ['G', 'G', 'S'], 'pickaxe': ['GGG', ' S ', ' S '], 'axe': ['GG', 'GS', ' S'],
               'shovel': ['G', 'S', 'S'], 'hoe': ['GG', ' S', ' S']}


def generate():
    import gen_assets as GA
    NS = GA.NS
    for t, pat in PRISM_TOOLS.items():
        GA.shaped(f'prism_{t}', pat, {'G': 'prism_gem', 'S': 'minecraft:stick'}, f'prism_{t}', category='equipment')
        GA.tag('item', f'minecraft:{t}s' if t != 'pickaxe' else 'minecraft:pickaxes', f'{NS}:prism_{t}')
    GA.tag('item', f'{NS}:prism_tool_materials', f'{NS}:prism_gem')
    key = f'codex.{NS}.prism.body'
    if key in GA.LANG:
        GA.LANG[key] += ' Prism tools sit between Diamond and Netherite; the Prism Sword makes every monster near you glow.'
