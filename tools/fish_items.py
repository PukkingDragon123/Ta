"""CR3 Fish & Coral Organs: the item sprites (16 x 16, in the items16.py style: hand-shaded, outlined, lit from the top
left) for every Sift fish - raw and cooked meat that look like the fish they came from, the sushi and the platter,
the plain water buckets and the Chrome buckets with the fish peeking out, the new spawn eggs - and the Coral
Organ's barb. Used by tools/sculk_sea.py (items16.all_items, gen_textures) and tools/chrome.py (bucket fish).

The whole fish are laid like vanilla's cod: head bottom-left, tail top-right. They are drawn along that diagonal
from a few shapes (a body profile, fins, a tail) in item-space coordinates, then hand-placed details on top.
"""
from __future__ import annotations

import math

from PIL import Image

import items16 as I

R2 = math.sqrt(2.0)


def _uv(x, y, cx=7.5, cy=8.0):
    """Along (u, towards the tail: up-right) and across (v, towards the belly: down-right) the item's diagonal."""
    dx, dy = x + 0.5 - cx, y + 0.5 - cy
    return (dx - dy) / R2, (dx + dy) / R2


def _canvas():
    return [['.'] * 16 for _ in range(16)]


def _rows(g):
    return [''.join(r) for r in g]


def _put(g, pts, ch):
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            g[y][x] = ch


def _body(g, shape, cx=7.5, cy=8.0):
    """Paints every pixel for which shape(u, v) returns a character."""
    for y in range(16):
        for x in range(16):
            u, v = _uv(x, y, cx, cy)
            ch = shape(u, v)
            if ch:
                g[y][x] = ch


# ============================================================================ the whole fish

def _kazoo_shape(u, v):
    if -8.2 <= u < -5.0 and abs(v + 0.1) <= 1.05:  # the kazoo
        return 'K' if v < -0.35 else 'k'
    if -5.0 <= u <= 3.6:
        h = 1.55 + 1.75 * math.sin(math.pi * min(1.0, (u + 5.0) / 9.0)) ** 0.7
        if abs(v) <= h:
            if v < -h + 1.25:
                return 'd'
            if v > h - 1.5:
                return 'w'
            return 'T'
        if -2.2 <= u <= 1.6 and -h - 1.7 <= v < -h:  # the coral dorsal fin
            return 'f' if (u + v) % 2.0 < 1.0 else 'F'
    if 3.6 < u <= 5.0 and abs(v) <= 1.25:
        return 'd' if v < -0.4 else 'T'
    if 5.0 < u <= 8.8:  # the fan tail, a little forked
        h = 1.3 + (u - 5.0) * 0.95
        if abs(v) <= h and not (u > 7.7 and abs(v) < 1.0):
            return 'F' if abs(v) > h - 0.9 or u > 8.0 else 'f'
    return None


def kazoo_fish(cooked=False):
    g = _canvas()
    _body(g, _kazoo_shape)
    if not cooked:
        _put(g, [(9, 8), (10, 6), (7, 9), (11, 8)], 's')  # coral freckles
    _put(g, [(4, 10)], 'W')
    _put(g, [(5, 10)], 'e')  # the googly eye
    _put(g, [(3, 10)], 'c')  # the kazoo's cap
    if cooked:
        pal = {'K': '#c89068', 'k': '#a8704a', 'c': '#d8b060', 'd': '#7a4a2a', 'T': '#b07a48', 'w': '#e0b88a', 'f': '#a0623a', 'F': '#c88a58',
               's': '#8a5434', 'W': '#f0e0c8', 'e': '#2a1810'}
    else:
        pal = {'K': '#ffb0bb', 'k': '#f87d8d', 'c': ('#fff4a8', '#8a6a20'), 'd': '#1f8f96', 'T': '#3fd0c4', 'w': '#d2fff0', 'f': '#f87d8d',
               'F': '#ffc8d0', 's': '#ff6f8c', 'W': '#ffffff', 'e': '#101820'}
    return I.grid(_rows(g), pal, ol=True)


def _tuba_shape(u, v):
    r = math.hypot(u + 0.6, v * 1.05)
    if r <= 5.4:
        if v < -2.6:
            return 'd'
        if v > 1.9:
            return 'w'
        return 'B'
    if 4.8 < u <= 8.0 and abs(v) <= 1.0 + (u - 4.8) * 0.75 and r > 5.0:  # a stubby fan tail
        return 'F' if u > 7.2 else 'f'
    if -7.4 <= u < -5.4 and abs(v - 0.6) <= 1.0:  # pouting lips
        return 'L'
    return None


def tubafish(cooked=False):
    g = _canvas()
    _body(g, _tuba_shape, 7.0, 8.5)
    # the tuba on its back (up-left) and the bell
    _put(g, [(4, 3), (5, 4), (3, 2), (4, 2), (2, 2), (3, 1), (2, 1), (4, 1)], 'G')
    _put(g, [(3, 2)], 'g')
    # coral spikes round its outline
    _put(g, [(1, 7), (8, 2), (13, 9), (9, 14), (2, 12)], 'p')
    if not cooked:
        _put(g, [(6, 7), (8, 8), (9, 6), (6, 10), (10, 9), (7, 5)], 's')  # glowing freckles
    _put(g, [(4, 8), (5, 8)], 'W')
    _put(g, [(4, 9)], 'e')
    if cooked:
        pal = {'d': '#7a4a2a', 'B': '#b07a48', 'w': '#e8c49a', 'f': '#a0623a', 'F': '#c88a58', 'L': '#c07850', 'G': '#c09040', 'g': '#5a3a1a',
               'p': '#9a5a3a', 's': '#d8a868', 'W': '#f0e0c8', 'e': '#2a1810'}
    else:
        pal = {'d': '#4f74c2', 'B': '#78a5e3', 'w': '#ffe6ef', 'f': '#ffb0c4', 'F': '#fff0f4', 'L': ('#f59ab8', '#8a3a5a'), 'G': ('#f2d27a', '#7a5a20'),
               'g': '#1a1030', 'p': ('#f37d84', '#8a2a3a'), 's': '#5ff8ff', 'W': '#ffffff', 'e': '#1a1420'}
    return I.grid(_rows(g), pal, ol=True)


def _eel_shape(u, v):
    if -8.6 <= u < -5.6:  # the trumpet bell
        h = 0.9 + (-5.6 - u) * 0.75
        if abs(v) <= h:
            return 'G' if abs(v) > h - 0.8 or u < -8.0 else 'g'
        return None
    if -5.6 <= u <= 8.8:
        h = 1.55 - max(0.0, u - 1.0) * 0.12
        wave = 0.7 * math.sin(u * 0.55)
        vv = v - wave
        if abs(vv) <= h:
            if vv < -h + 0.9:
                return 'd'
            if abs(vv) < 0.5 and int(u * 2) % 3 == 0:
                return 'l'  # the glowing lateral line
            return 'b' if int(u + 20) % 3 else 'r'
        if -2.0 < u < 7.0 and -h - 1.1 <= vv < -h:  # the glowing frill
            return 'f'
    return None


def fanfare_eel(cooked=False):
    g = _canvas()
    _body(g, _eel_shape, 7.5, 8.0)
    _put(g, [(4, 9)], 'e')
    if cooked:
        pal = {'G': '#c89a50', 'g': '#8a6030', 'd': '#4a2e1c', 'b': '#7a5034', 'r': '#d8c09a', 'l': '#e0a860', 'f': '#a07040', 'e': '#1a100a'}
    else:
        pal = {'G': ('#f2d27a', '#6a4a10'), 'g': '#1a1030', 'd': '#0f2430', 'b': '#1a3a4a', 'r': '#b3ab96', 'l': '#3ff5e6',
               'f': ('#3fd8d0', '#0a3a3a'), 'e': '#3ff5e6'}
    return I.grid(_rows(g), pal, ol=True, no_ol='')


def gobbler_fillet(cooked=False):
    """A thick slab of deep-sea fillet with the Gobbler's dark, photophore-studded skin along its back."""
    g = _canvas()

    def shape(u, v):
        if -6.5 <= u <= 6.8:
            h = 2.7 - abs(u) * 0.12
            if abs(v + 0.2 * u * 0.1) <= h:
                if v < -h + 1.1:
                    return 'K'
                return 'v' if abs(v - 0.3 * math.sin(u * 0.8)) < 0.45 and abs(u) < 5.5 else 'm' if (int((u + 10) * 1.1) % 3) else 'M'
        return None
    _body(g, shape)
    if not cooked:
        _put(g, [(6, 5), (9, 3), (11, 2)], 'p')
    if cooked:
        pal = {'K': '#5a3a24', 'm': '#c08850', 'M': '#e0b080', 'v': '#8a5a30', 'p': '#d8a060'}
    else:
        pal = {'K': '#0b1e24', 'm': '#a8d0cc', 'M': '#d8f0ec', 'v': ('#3ff5e6', '#0f4a50'), 'p': '#5ff8ff'}
    return I.grid(_rows(g), pal, ol=True)


def sculk_fish_raw():
    """(only used for the spawn egg's silhouette checks)"""
    return None


# ============================================================================ sushi

RICE = {'r': ('#f6f2e6', '#6a6458'), 'R': ('#ffffff', '#6a6458'), 'q': ('#d8d2c2', '#6a6458')}
NORI = {'k': ('#1f3a2a', '#0a1410'), 'K': ('#2f5a3a', '#0a1410')}


def _nigiri(topping, pal, tail=None, extra=None, band=False):
    """Nigiri: a rice pad, a slice of the fish's own skin over it, its tail fin sticking out at the back (the way a
    shrimp nigiri shows the shrimp)."""
    rows = [
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '..TTTTTTTTTT....',
        '.TTTTTTTTTTTT...',
        '.SSSSSSSSSSSS...',
        '.rRrrrrrrrrrr...',
        '.rrrrrrrrrrrq...',
        '..qrrrrrrrrq....',
        '...qqqqqqqq.....',
        '................',
        '................',
        '................',
    ]
    g = [list(r) for r in rows]
    for (x, y, ch) in topping:
        g[y][x] = ch
    for (x, y, ch) in tail or ():
        g[y][x] = ch
    if band:
        for y in range(6, 13):
            if g[y][6] != '.':
                g[y][6] = 'k'
                g[y][7] = 'K' if y == 6 else 'k'
    for (x, y, ch) in extra or ():
        g[y][x] = ch
    p = dict(RICE)
    p.update(NORI)
    p.update(pal)
    return I.grid(_rows(g), p, ol=True, no_ol='gG')


def kazoo_fish_sushi():
    """Kazoo nigiri: teal skin with coral freckles over the rice, the coral fan tail flicking up at the back, a
    pink kazoo-ring of pickled ginger in front."""
    top = [(4, 6, 's'), (8, 7, 's'), (10, 6, 's'), (2, 8, 'U'), (12, 8, 'U')]
    tail = [(12, 5, 'f'), (13, 4, 'F'), (13, 5, 'f'), (14, 3, 'F'), (14, 4, 'f'), (12, 6, 'f'), (13, 6, 'F')]
    extra = [(0, 11, 'p'), (1, 12, 'p'), (0, 12, 'P')]
    pal = {'T': ('#3fd0c4', '#0f5a5a'), 'S': ('#d2fff0', '#3a7a70'), 'U': ('#1f8f96', '#0f5a5a'), 's': ('#ff6f8c', '#0f5a5a'),
           'f': ('#f87d8d', '#7a2a3a'), 'F': ('#ffc8d0', '#7a2a3a'), 'p': ('#ff9aa8', '#7a2a3a'), 'P': ('#ffd0d8', '#7a2a3a')}
    return _nigiri(top, pal, tail, extra)


def tubafish_sushi():
    """Tubafish gunkan: a roll wrapped in nori, heaped with pale puffer flesh freckled with its glowing cyan
    spots, a tiny brass tuba-bell of a garnish on top."""
    rows = [
        '................',
        '................',
        '.......Gg.......',
        '......GGG.......',
        '....wwswwswW....',
        '...wWswwwwswW...',
        '...kKkkkkkkkk...',
        '...kKkkkkkkkk...',
        '...kKkkkkkkkk...',
        '...kKkkkkkkkk...',
        '...kkkkkkkkkk...',
        '....kkkkkkkk....',
        '................',
        '................',
        '................',
        '................',
    ]
    pal = dict(NORI)
    pal.update({'w': ('#ffe6ef', '#7a4a6a'), 'W': ('#fff6fa', '#7a4a6a'), 's': '#5ff8ff', 'G': ('#f2d27a', '#6a4a10'), 'g': '#1a1030'})
    return I.grid(rows, pal, ol=True, no_ol='s')


def fanfare_eel_sushi():
    """Fanfare Eel nigiri (like unagi): a glazed dark eel fillet, its bone ribs and glowing line showing, bound to
    the rice with a nori band, a curl of golden bell at the tip."""
    top = [(3, 6, 'L'), (6, 6, 'L'), (9, 6, 'L'), (4, 7, 'l'), (8, 7, 'l'), (11, 7, 'l')]
    extra = [(12, 6, 'G'), (12, 7, 'G'), (13, 7, 'G'), (13, 6, 'g')]
    pal = {'T': ('#5a3a24', '#1a0e08'), 'S': ('#c8963a', '#3a2008'), 'L': ('#e3ddcc', '#1a0e08'), 'l': ('#3ff5e6', '#1a0e08'),
           'G': ('#f2d27a', '#5a3a10'), 'g': '#2a1810'}
    return _nigiri(top, pal, None, extra, band=True)


def gobbler_sushi():
    """Gobbler roll: a thick maki of pale deep-sea fillet round a glowing sculk-bladder core, its dark skin and a
    photophore on the edge."""
    rows = [
        '................',
        '................',
        '.....kkkkkk.....',
        '...kkDDDDDDkk...',
        '..kDmmmmmmmmDk..',
        '..kDmMMMMMmmDk..',
        '.kDmMSSSSMMmmDk.',
        '.kDmMSWWSSMmmDk.',
        '.kDmMSSSSSMmpDk.',
        '.kKDmMMMMMmmDKk.',
        '.kKKDmmmmmmDKKk.',
        '.kkKKDDDDDDKKkk.',
        '.kkkkkkkkkkkkkk.',
        '..kkkkkkkkkkkk..',
        '....kkkkkkkk....',
        '................',
    ]
    pal = dict(NORI)
    pal.update({'D': ('#0b1e24', '#020a0e'), 'm': ('#a8d0cc', '#3a5a5a'), 'M': ('#d8f0ec', '#3a5a5a'), 'S': '#3ff5e6', 'W': '#d6fffb',
                'p': '#5ff8ff'})
    return I.grid(rows, pal, ol=True, no_ol='SWp')


def sushi_platter():
    """All four on a wooden board: kazoo nigiri, a tubafish gunkan, eel nigiri and a gobbler roll."""
    rows = [
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '..F.......Gg....',
        '.TsT.wsw.LLL.kkk',
        '.rrr.kkk.rKr.kSk',
        '.qrq.kkk.rKr.kkk',
        'WWWWWWWWWWWWWWWW',
        'VVVVVVVVVVVVVVVV',
        'VVVVVVVVVVVVVVVV',
        '.dd..........dd.',
        '................',
        '................',
    ]
    pal = dict(RICE)
    pal.update(NORI)
    pal.update({'T': ('#3fd0c4', '#0f5a5a'), 's': '#ff6f8c', 'F': ('#f87d8d', '#7a2a3a'), 'w': ('#ffe6ef', '#7a4a6a'),
                'G': ('#f2d27a', '#6a4a10'), 'g': '#1a1030', 'L': ('#5a3a24', '#1a0e08'), 'S': '#3ff5e6',
                'W': ('#b18a52', '#2a1a16'), 'V': ('#8f6a36', '#2a1a16'), 'd': ('#4d3220', '#2a1a16')})
    return I.grid(rows, pal, ol=True, no_ol='sS')


# ============================================================================ buckets

# the fish peeking out of a bucket (the top rows of a vanilla fish bucket), shared by the water and Chrome buckets
BUCKET_FISH = {
    'kazoo_fish': ([
        '........FfF.....',
        '...kk.dTTTdF....',
        '.ckKKWeTsTTTFf..',
        '..kKKTTTTsTTdF..',
        '.....wwwwww.....',
        '................',
        '................',
    ], {'k': '#f87d8d', 'K': '#ffb0bb', 'c': ('#fff4a8', '#8a6a20'), 'd': '#1f8f96', 'T': '#3fd0c4', 'w': '#d2fff0', 's': '#ff6f8c',
        'F': '#ffc8d0', 'f': '#f87d8d', 'W': '#ffffff', 'e': '#101820'}),
    'tubafish': ([
        '......GgG.......',
        '..p..dBBBd..p...',
        '.LLdBWBsBBBd....',
        '.LLBBeBBBsBBFf..',
        '..wwwwwwwwwwF...',
        '................',
        '................',
    ], {'G': ('#f2d27a', '#6a4a10'), 'g': '#1a1030', 'p': ('#f37d84', '#8a2a3a'), 'd': '#4f74c2', 'B': '#78a5e3', 'L': '#f59ab8',
        'W': '#ffffff', 'e': '#1a1420', 's': '#5ff8ff', 'w': '#ffe6ef', 'F': '#fff0f4', 'f': '#ffb0c4'}),
    'fanfare_eel': ([
        '.GGg............',
        'GDDGddbbbb......',
        'GDDGlbrblbrbb...',
        '.GGg.dbbbbbbrbf.',
        '.........dbbbf..',
        '................',
        '................',
    ], {'G': ('#f2d27a', '#6a4a10'), 'g': '#b8923a', 'D': '#0a1a22', 'd': '#0f2430', 'b': '#1a3a4a', 'r': '#b3ab96', 'l': '#3ff5e6',
        'f': ('#3fd8d0', '#0a3a3a')}),
}

WATER_BUCKET = [
    '................',
    '.....aaaaaa.....',
    '...aabcccddaa...',
    '..abbeeebbbcba..',
    '..aefffebbbbea..',
    '..aaaffeebbaaa..',
    '..agdaaaaaabca..',
    '..aggggddcbbca..',
    '..agghgddcbbda..',
    '..adghgddcbbda..',
    '..abghgddcbbca..',
    '...agggddcbca...',
    '...adggdccbca...',
    '....adgdcbca....',
    '.....aaaaaa.....',
    '................',
]
WATER_PAL = {'a': '#353535', 'b': '#727272', 'c': '#969696', 'd': '#a8a8a8', 'e': '#5f5f5f', 'f': '#545454', 'g': '#d8d8d8', 'h': '#ffffff'}
# vanilla's water: the surface in the mouth (dark .. light) and the drips down the side when a fish is in
WATER_SURFACE = {(4, 3): 0, (5, 3): 1, (6, 3): 3, (7, 3): 2, (8, 3): 2, (9, 3): 1, (10, 3): 1, (11, 3): 0,
                 (3, 4): 0, (4, 4): 3, (5, 4): 4, (6, 4): 4, (7, 4): 3, (8, 4): 2, (9, 4): 4, (10, 4): 2, (11, 4): 3, (12, 4): 0,
                 (5, 5): 0, (6, 5): 2, (7, 5): 3, (8, 5): 3, (9, 5): 2, (10, 5): 0}
WATER_SPILL = {(5, 6): 3, (6, 6): 2, (7, 6): 3, (8, 6): 3, (9, 6): 2, (10, 6): 0, (7, 7): 2, (8, 7): 0, (8, 8): 2, (8, 9): 2, (8, 11): 2,
               (4, 7): 1, (4, 8): 0}
WATER_TONES = ['#1a3a8a', '#2a52b0', '#3a6ad0', '#4a82e8', '#6aa0f8']


def bucket_fish(fish):
    rows, pal = BUCKET_FISH[fish]
    return I.grid(rows, pal, ol=True, size=(16, len(rows)))


def water_bucket(fish):
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y, row in enumerate(WATER_BUCKET):
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = I.rgba(WATER_PAL[ch])
    water = dict(WATER_SURFACE)
    water.update(WATER_SPILL)
    for (x, y), t in water.items():
        px[x, y] = I.rgba(WATER_TONES[t])
    img.alpha_composite(bucket_fish(fish), (0, 0))
    return img


# ============================================================================ spawn eggs

def eggs():
    E = {}
    E['sculk_fish_spawn_egg'] = I.egg(['#06161d', '#0b2532', '#123a48', '#1b5462'], '#020a0e', {
        2: '.....t...t......',
        3: '......t.t.......',
        5: '.....gE..Eg.....',
        6: '......g..g......',
        8: '...s........s...',
        9: '....wtwtwtw.....',
        10: '.....jjjjj......',
        12: '...s...s...s....',
    }, {'t': ('#d6fffb', '#020a0e'), 'g': ('#3ff5e6', '#020a0e'), 'E': ('#9ffcff', '#020a0e'), 's': ('#3ff5e6', '#020a0e'),
        'w': ('#e3ddcc', '#020a0e'), 'j': ('#0b2430', '#020a0e')}, no_ol='tgEs', ring='water')
    E['coral_organ_spawn_egg'] = I.egg(['#082a35', '#0c3b47', '#114d58', '#18626b'], '#03141a', {
        1: '.......R........',
        2: '.....R.P.R......',
        3: '.....P.P.P......',
        4: '...R.P.P.P.R....',
        5: '...P.P.P.P.P....',
        6: '...P.M.M.M.P....',
        7: '...M.h.h.h.M....',
        8: '...h.......h....',
        10: '....oHHHHo......',
        12: '..p...p...p.p...',
    }, {'R': ('#7ff7ea', '#03141a'), 'P': ('#23787e', '#03141a'), 'M': ('#e3ddcc', '#03141a'), 'h': ('#020a0e', '#03141a'),
        'H': ('#e3ddcc', '#03141a'), 'o': ('#5ff8ff', '#03141a'), 'p': ('#54ecde', '#03141a')}, no_ol='Rop')
    return E


# ============================================================================ the Coral Organ's barb (entity texture)

def barb():
    """The hook, side on, pointing right (+u): a bone shank, a curved barbed point, a glowing sculk knot at its eye."""
    rows = [
        '................',
        '................',
        '................',
        '..............o.',
        '.............oO.',
        '............oO..',
        '..k.......boO...',
        '.kKk.....bb.....',
        '.kKkbbbbbbb.....',
        '..kbbbbbb.......',
        '...........B....',
        '..........B.....',
        '................',
        '................',
        '................',
        '................',
    ]
    pal = {'k': ('#3ff5e6', '#0a3a3a'), 'K': '#d6fffb', 'b': ('#e3ddcc', '#5a5040'), 'B': ('#b3ab96', '#5a5040'), 'o': ('#e3ddcc', '#5a5040'),
           'O': ('#fbf8ee', '#5a5040')}
    return I.grid(rows, pal, ol=True, no_ol='kK')


# ============================================================================ all of it

def sprites():
    out = {
        'kazoo_fish': kazoo_fish(), 'cooked_kazoo_fish': kazoo_fish(True), 'tubafish': tubafish(), 'cooked_tubafish': tubafish(True),
        'fanfare_eel': fanfare_eel(), 'cooked_fanfare_eel': fanfare_eel(True), 'gobbler_fillet': gobbler_fillet(),
        'cooked_gobbler_fillet': gobbler_fillet(True),
        'kazoo_fish_sushi': kazoo_fish_sushi(), 'tubafish_sushi': tubafish_sushi(), 'fanfare_eel_sushi': fanfare_eel_sushi(),
        'gobbler_sushi': gobbler_sushi(), 'sushi_platter': sushi_platter(),
    }
    for f in BUCKET_FISH:
        out[f'{f}_bucket'] = water_bucket(f)
    out.update(eggs())
    return out
