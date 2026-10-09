"""I1 items: every Sift item sprite at 16x16, made the way Minecraft's own are, so each belongs among vanilla items.

Every sprite starts from the vanilla item it is kin to and keeps that sprite's silhouette, outline treatment, pixel
clusters and shading: the vanilla colours are re-toned shade-for-shade into a Sift material's ramp (retone), or mapped
colour-for-colour (remap), then reshaped where the Sift item differs. The currency is one family on vanilla's shapes:

  Siftite ...... ingot (gold ingot), nugget (gold nugget), dust (glowstone dust)              cyan metal
  Prism ........ gem (diamond), tools and armour (diamond)                                    opal: pastel lights drifting over lavender
  Skysong gem .. emerald      Star shard .. amethyst shard     Chrome pearl .. ender pearl
  Scukite ...... crystal (quartz), raw (raw gold)                                             sculk teal
  Bauxite ...... raw (raw copper), stable (brick)        Galena .. raw iron
  Soul ......... dust (redstone), piece (lapis lazuli), chunk (prismarine crystals)

Food sits in vanilla's bowls and shapes (stews, cake, cooked fish and meat); things vanilla has no sprite for (sushi,
the skewer, the slingshot, scrolls) are drawn texel by texel in the same manner: flat clustered tones, light from the
top left, a dark hue-shifted outline. Books are vanilla books re-covered (book, enchanted book).

Hook: gen_textures.main() -> textures(out), after every older item writer (so these sprites win). The worn Prism
armour (vanilla humanoid layouts) is painted here too; the book GUI and the placed Lore Book live in knowledge_art.py.
"""
from __future__ import annotations

import colorsys
import os

from PIL import Image

VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')

# ============================================================================ colour helpers


def rgba(c, a=255):
    if isinstance(c, (tuple, list)):
        c = tuple(int(v) for v in c)
        return c if len(c) == 4 else (*c, a)
    c = c.lstrip('#')
    if len(c) == 8:
        a = int(c[6:8], 16)
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)


def mix(a, b, t):
    a, b = rgba(a), rgba(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def ramp(*hexes):
    return [rgba(h) for h in hexes]


def van(name):
    return Image.open(os.path.join(VANILLA, 'item', name + '.png')).convert('RGBA')


def blank(w=16, h=16):
    return Image.new('RGBA', (w, h), (0, 0, 0, 0))


# ============================================================================ re-toning vanilla sprites

def retone(img, tones, sel=None):
    """Maps each distinct colour picked by sel (default: every opaque colour) onto the ramp tones (dark to light) by
    where it sits between the darkest and lightest picked colour - half by luminance, half by rank, so vanilla's
    clusters and steps both survive. The darkest picked colour (the outline) alone takes tones[0]."""
    out = img.copy()
    px = out.load()
    w, h = out.size
    cols = sorted({px[x, y][:3] for y in range(h) for x in range(w) if px[x, y][3] and (sel is None or sel(px[x, y]))}, key=lum)
    if not cols:
        return out
    lo, hi = lum(cols[0]), lum(cols[-1])
    n = len(cols)
    cmap = {}
    for i, c in enumerate(cols):
        t_rank = 0.0 if n == 1 else i / (n - 1)
        t_lum = 0.0 if hi == lo else (lum(c) - lo) / (hi - lo)
        k = round((0.5 * t_rank + 0.5 * t_lum) * (len(tones) - 1))
        if i > 0 and k == 0 and len(tones) > 2:
            k = 1
        cmap[c] = rgba(tones[k])[:3]
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            if c[3] and c[:3] in cmap:
                px[x, y] = cmap[c[:3]] + (c[3],)
    return out


def remap(img, cmap):
    """Explicit colour map {colour: colour}; colours not listed stay."""
    out = img.copy()
    px = out.load()
    m = {rgba(k)[:3]: rgba(v)[:3] for k, v in cmap.items()}
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            c = px[x, y]
            if c[3] and c[:3] in m:
                px[x, y] = m[c[:3]] + (c[3],)
    return out


def colours_of(name):
    im = van(name)
    p = im.load()
    return {p[x, y][:3] for y in range(im.size[1]) for x in range(im.size[0]) if p[x, y][3]}


def hue_between(lo, hi, min_sat=0.25):
    def sel(c):
        h, s, v = hsv(c)
        return s >= min_sat and lo <= h * 360 <= hi
    return sel


def recolour_where(img, sel, fn):
    out = img.copy()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            c = px[x, y]
            if c[3] and sel(c, x, y):
                px[x, y] = fn(c, x, y)
    return out


def opal(img, sel=lambda c: True, strength=0.5, period=8.0):
    """The Prism's opal play of colour: the lighter tones take pastel hues (pink, peach, mint, cyan, lavender) that
    drift diagonally across the sprite; the dark tones keep the lavender body."""
    hues = [0.93, 0.06, 0.36, 0.51, 0.72]

    def fn(c, x, y):
        h, s, v = hsv(c)
        t = ((x - y) / period) % 1.0
        f = t * len(hues)
        i = int(f) % len(hues)
        hh, nh = hues[i], hues[(i + 1) % len(hues)]
        d = (nh - hh + 0.5) % 1.0 - 0.5
        hue = (hh + d * (f - int(f))) % 1.0
        k = strength * min(1.0, max(0.0, (v - 0.55) / 0.35))
        if k <= 0 or (v > 0.985 and s < 0.05):
            return c
        r, g, b = colorsys.hsv_to_rgb(hue, min(1.0, s * (1 - k) + 0.3 * k), v)
        return (round(r * 255), round(g * 255), round(b * 255), c[3])
    return recolour_where(img, lambda c, x, y: sel(c), fn)


def put(img, pts, col):
    px = img.load()
    for x, y in pts:
        px[x, y] = rgba(col)
    return img


def clear(img, pts):
    px = img.load()
    for x, y in pts:
        px[x, y] = (0, 0, 0, 0)
    return img


# ============================================================================ drawing texel by texel

def _auto_ol(c):
    """Vanilla's outline: a deep, slightly hue-shifted shade of the material, never pure black."""
    h, s, v = hsv(rgba(c))
    h = (h + 0.03) % 1.0 if 0.1 < h < 0.6 else h
    r, g, b = colorsys.hsv_to_rgb(h, min(1.0, s + 0.2), max(0.07, v * 0.3))
    return (round(r * 255), round(g * 255), round(b * 255), 255)


def paint(rows, pal, outline=True, no_ol=''):
    """Rows of characters (one per texel, '.' empty) in palette pal {ch: colour or (colour, outline colour)}. With
    outline, every empty texel touching a filled one edge-on takes that material's outline colour - a step lighter on
    the lit top/left side, as vanilla items are rimmed. Characters in no_ol cast no outline."""
    h, w = len(rows), len(rows[0])
    img = blank(w, h)
    px = img.load()
    cells = {}

    def col(v):
        return rgba(v[0] if isinstance(v, (tuple, list)) and not isinstance(v[0], int) else v)
    for y, row in enumerate(rows):
        assert len(row) == w, f'row {y} is {len(row)} wide: {row!r}'
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            px[x, y] = col(pal[ch])
            cells[(x, y)] = ch
    if outline:
        add = {}
        for y in range(h):
            for x in range(w):
                if (x, y) in cells:
                    continue
                best = None
                for dx, dy, lit in ((1, 0, True), (0, 1, True), (-1, 0, False), (0, -1, False)):
                    ch = cells.get((x + dx, y + dy))
                    if ch is None or ch in no_ol:
                        continue
                    if best is None or (best[1] and not lit):
                        best = (ch, lit)
                if best:
                    v = pal[best[0]]
                    ol = rgba(v[1]) if isinstance(v, (tuple, list)) and not isinstance(v[0], int) else _auto_ol(col(v))
                    add[(x, y)] = mix(ol, col(v), 0.2) if best[1] else ol
        for (x, y), c in add.items():
            px[x, y] = c
    return img


# ============================================================================ Sift materials (outline first, highlight last)

SIFTITE = ramp('#0b2c44', '#13506e', '#1f7896', '#2fa2bd', '#4cc6da', '#86e4ee', '#c6f7fb', '#ffffff')
PRISM = ramp('#2a2048', '#54488a', '#8478bc', '#ac9fe0', '#d0c8f4', '#f0ecff', '#ffffff')
SKYSONG = ramp('#0d2c5c', '#17509a', '#2a7fd0', '#4aa8f0', '#7fcbff', '#c4ecff', '#ffffff')
STAR = ramp('#4a2a06', '#8a5210', '#c47e18', '#e8aa26', '#fbd040', '#ffe98a', '#fffbe0')
CHROME = ramp('#221c3a', '#463e74', '#7468aa', '#a294d4', '#c8baf0', '#ece4ff', '#ffffff')
SCULK = ramp('#04161c', '#0a2c34', '#0f4650', '#14666c', '#1f8f8f', '#3fc2bc', '#8ff5ec')
SCULK_RAW = ramp('#061a20', '#0c2e36', '#124650', '#1b6066', '#2a8a88', '#56c4bc', '#a8f2ea')
BAUXITE = ramp('#3a1206', '#6a240c', '#9a3a14', '#c45a22', '#e07e3a', '#f4a866')
GALENA = ramp('#141822', '#2a3040', '#464e62', '#6a7488', '#98a2b6', '#d0d8e6')
SOUL = ramp('#0a1640', '#13287a', '#1f46b0', '#2f6ee0', '#58a0ff', '#9cd2ff', '#e4f6ff')
SOUL_DUST = ramp('#0c1a48', '#1a3a90', '#2a5cd0', '#4a8cf4', '#86c0ff', '#d0ecff')
SEED = ramp('#0e2448', '#1d4a88', '#3a7ccc', '#6ab0f0', '#b8e6ff')


# ============================================================================ currency

def siftite_ingot():
    return retone(van('gold_ingot'), SIFTITE[:7])


def siftite_nugget():
    return retone(van('gold_nugget'), SIFTITE[:2] + SIFTITE[3:])


def siftite_dust():
    return retone(van('glowstone_dust'), SIFTITE[1:])


def prism_gem():
    return opal(retone(van('diamond'), PRISM), strength=0.4, period=9.0)


def skysong_gem():
    return retone(van('emerald'), SKYSONG)


def star_shard():
    return retone(van('amethyst_shard'), STAR)


def chrome_pearl():
    """The ender pearl's sphere in Chrome: its dark pupil filled with a soft swirl of lilac, the opal sheen over it."""
    img = retone(van('ender_pearl'), CHROME)
    px = img.load()
    for y in range(4, 12):
        for x in range(4, 12):
            c = px[x, y]
            if c[3] and lum(c) < lum(CHROME[2]):
                px[x, y] = CHROME[3] if (x + y) % 3 else CHROME[2]
    return opal(img, strength=0.4, period=10.0)


def sculkite():
    return retone(van('quartz'), SCULK)


def raw_sculkite():
    return retone(van('raw_gold'), SCULK_RAW)


def bauxite():
    return retone(van('raw_copper'), BAUXITE)


def stable_bauxite():
    """A fired bar of Bauxite, stabilised: a vein of cyan Nib dust set through it."""
    img = retone(van('brick'), BAUXITE)
    return put(put(img, [(5, 9), (6, 8), (8, 8), (10, 7)], '#9ff0f0'), [(7, 8), (9, 7), (11, 7)], '#4ab8c4')


def galena():
    return retone(van('raw_iron'), GALENA)


def soul_dust():
    return retone(van('redstone'), SOUL_DUST)


def soul_piece():
    return retone(van('lapis_lazuli'), SOUL)


def soul_chunk():
    return retone(van('prismarine_crystals'), SOUL)


def echo_seed():
    return retone(van('melon_seeds'), SEED)


def g_currency():
    return {f.__name__: f() for f in (siftite_ingot, siftite_nugget, siftite_dust, prism_gem, skysong_gem, star_shard, chrome_pearl,
                                      sculkite, raw_sculkite, bauxite, stable_bauxite, galena, soul_dust, soul_piece, soul_chunk,
                                      echo_seed)}



# ============================================================================ food

BOWL_COLS = colours_of('bowl')


def _soup(base, tones, bits=(), sel=None):
    """A vanilla bowl of stew, the stew re-toned (the bowl stays vanilla's), bits (x, y, colour) floating in it."""
    img = retone(van(base), tones, sel or (lambda c: c[:3] not in BOWL_COLS and not (25 <= hsv(c)[0] * 360 <= 50 and lum(c) < 70)))
    for x, y, c in bits:
        img.putpixel((x, y), rgba(c))
    return img


def chrome_bisque():
    """Chrome Bisque: a lilac cream with Chrome's colours drifting through it."""
    return opal(_soup('beetroot_soup', ramp('#5a3a7a', '#8a6ab8', '#b49ae0', '#dccaf8')), lambda c: c[:3] not in BOWL_COLS, 0.5, 5.0)


def dream_stew():
    """Dream Stew: a violet broth with drops of gold and pink."""
    return _soup('mushroom_stew', ramp('#4a3480', '#7a5ab8', '#a486e0', '#c8b0f4'), [(5, 7, '#ffd75e'), (9, 8, '#ff9fd8'), (11, 7, '#ffd75e')])


def echo_chowder():
    """Echo Chowder: a thick teal cream, shells and sculk pearls in it."""
    return _soup('rabbit_stew', ramp('#0e4a52', '#1f7a7e', '#3fa8a4', '#7ed2c8', '#bcefe4', '#e8fff8'))


def lullaby_soup():
    """Lullaby Soup: a pale moon-blue broth with gold star croutons."""
    img = _soup('suspicious_stew', ramp('#3a5a9a', '#6a8ed0', '#9cbcec', '#cfe2fc'))
    return put(img, [(6, 7), (10, 6), (8, 8)], '#ffd75e')


def sift_cake():
    """The Sift Cake: vanilla's cake under coral-pink icing, glowing teal berries on top."""
    img = van('cake')
    reds = lambda c: hue_between(340, 360, 0.5)(c) or hue_between(0, 12, 0.5)(c)  # noqa: E731
    img = retone(img, ramp('#0c5a62', '#1f9a9e', '#3fd6cc', '#a8fff4'), reds)
    icing = lambda c: hsv(c)[1] < 0.25 and lum(c) > 150  # noqa: E731
    return retone(img, ramp('#c45a88', '#e88ab0', '#f8b8d0', '#ffe0ec'), icing)


def cooked_kazoo_fish():
    """A grilled Kazoo Fish: vanilla's cooked salmon shape, its skin a toasted teal, its flesh golden."""
    img = van('cooked_salmon')
    skin = hue_between(90, 170, 0.15)
    flesh = lambda c: hue_between(5, 40, 0.35)(c) and lum(c) > 60  # noqa: E731
    img = retone(img, ramp('#123a3a', '#1f5a58', '#2f7a72', '#4a9a8a'), skin)
    return retone(img, ramp('#7a3a10', '#a85a1c', '#d08a34', '#eab060', '#f8d08a'), flesh)


def cooked_gobbler_fillet():
    """A seared Gobbler Fillet: vanilla's cooked cod, the Gobbler's white flesh with a blue-grey skin edge."""
    img = retone(van('cooked_cod'), ramp('#140e10', '#4a3a3a', '#8a6248', '#b88a58', '#d4aa72', '#e8cc9c', '#f6e6c8', '#fff8ea'))
    return img


def cooked_fanfare_eel():
    """A grilled Fanfare Eel: a long glazed fillet with a dark skin edge, char bars across it, a tail at the end."""
    rows = [
        '................',
        '...........ss...',
        '..........sggs..',
        '.........sgGgs..',
        '........sgGgss..',
        '.......sgGgss...',
        '......sgGgss....',
        '.....sgGgss.....',
        '....sgGgss......',
        '...sgGgss.......',
        '..sgGgss........',
        '..sgggs.........',
        '...sss..........',
        '..ss............',
        '.s..............',
        '................',
    ]
    img = paint(rows, {'s': ('#5a2e14', '#24100a'), 'g': ('#c46a24', '#24100a'), 'G': ('#eaa04a', '#24100a')})
    return put(put(img, [(11, 3), (9, 5), (7, 7), (5, 9), (3, 11)], '#6a3010'), [(12, 2), (10, 4)], '#f8cc7a')


def stomper_steak():
    """A Stomper Steak: vanilla's cooked chop, darker and redder, the Stomper's hide-grey rind along its edge."""
    return retone(van('cooked_mutton'), ramp('#24100a', '#4a1e12', '#6e2e1c', '#8e4228', '#a85a3a', '#c07a5a'))


RICE = ('#f4f2ec', '#dad6cc', '#b8b2a6', '#5e584e')   # light, mid, shade, outline


def _nigiri(fish, stripe=None, nori=False):
    """Nigiri in vanilla's manner: a block of rice, a slice of fish laid over it (fish = light, mid, dark, outline)."""
    rows = [
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '....FFFFFFFF....',
        '..FFFFFFFFFFff..',
        '.FfFFFFFFFFffff.',
        '.rRRRRRRRRRRRRr.',
        '.rRRRrRRRRrRRRr.',
        '.rrRRRRRrRRRRrr.',
        '..rrrrrrrrrrrr..',
        '................',
        '................',
        '................',
    ]
    pal = {'F': (fish[0], fish[3]), 'f': (fish[1], fish[3]), 'R': (RICE[0], RICE[3]), 'r': (RICE[1], RICE[3])}
    img = paint(rows, pal)
    put(img, [(6, 6), (7, 6), (5, 7)], mix(fish[0], '#ffffff', 0.35))
    put(img, [(12, 8), (13, 8), (14, 8)], fish[2])
    put(img, [(3, 12), (8, 12), (12, 12)], RICE[2])
    if stripe:
        put(img, [(4, 8), (5, 7), (9, 8), (10, 7)], stripe)
    if nori:
        for y in range(6, 13):
            for x in (7, 8):
                if img.getpixel((x, y))[3]:
                    img.putpixel((x, y), rgba('#1c3026' if x == 7 else '#12201a'))
    return img


def kazoo_fish_sushi():
    return _nigiri(('#f08a6a', '#d0604a', '#a8402e', '#4a1810'), stripe='#ffd8c8')


def fanfare_eel_sushi():
    return _nigiri(('#c8843a', '#9a5a24', '#6a3414', '#2a1208'), nori=True)


def gobbler_sushi():
    return _nigiri(('#d8eef0', '#a8ccd4', '#7aa0ac', '#2a3e48'), stripe='#ffffff')


def sushi_platter():
    """A wishwood board laid with kazoo, eel and gobbler nigiri and a sprig of sea-green garnish."""
    rows = [
        '................',
        '................',
        '................',
        '................',
        '................',
        '..........gG....',
        '.ff..ee..gGgg...',
        'fFFfeEEe.pPPp...',
        'RRRRRRRRRRRRRR..',
        'rrrrrrrrrrrrrr..',
        'wwwwwwwwwwwwwwww',
        'WWWWWWWWWWWWWWWW',
        '.d............d.',
        '................',
        '................',
        '................',
    ]
    rows = ['.' + r[:15] if i < 10 else r for i, r in enumerate(rows)]
    rows = [r if i not in (10, 11) else '.' + r[1:15] + '.' for i, r in enumerate(rows)]
    pal = {'f': ('#d0604a', '#4a1810'), 'F': ('#f08a6a', '#4a1810'), 'e': ('#9a5a24', '#2a1208'), 'E': ('#c8843a', '#2a1208'),
           'p': ('#a8ccd4', '#2a3e48'), 'P': ('#d8eef0', '#2a3e48'), 'R': (RICE[0], RICE[3]), 'r': (RICE[1], RICE[3]),
           'g': ('#2a7a4a', '#0e2a18'), 'G': ('#4aa86a', '#0e2a18'), 'w': ('#b0607a', '#2a0e1a'), 'W': ('#7a3450', '#2a0e1a'),
           'd': ('#4e1c32', '#2a0e1a')}
    img = paint(rows, pal)
    for x in (5, 9):
        img.putpixel((x, 9), rgba(RICE[3]))
        img.putpixel((x, 8), rgba(RICE[2]))
    return img


def glowcap_skewer():
    """A Glowcap Skewer: three grilled glowcap caps threaded on a stick, glowing spots on their tops."""
    rows = [
        '................',
        '..............s.',
        '...........CCs..',
        '..........CWcC..',
        '..........cccd..',
        '..........sdd...',
        '.......CC.s.....',
        '......CWcC......',
        '......cccd......',
        '......sdd.......',
        '...CC.s.........',
        '..CWcC..........',
        '..cccd..........',
        '..sdd...........',
        '.s..............',
        '................',
    ]
    pal = {'C': ('#4ad8d0', '#0a3038'), 'W': ('#c8fff6', '#0a3038'), 'c': ('#1f9aa0', '#0a3038'), 'd': ('#126670', '#0a3038'),
           's': ('#896727', '#281e0b')}
    return paint(rows, pal)


def g_food():
    return {f.__name__: f() for f in (chrome_bisque, dream_stew, echo_chowder, lullaby_soup, glowcap_skewer, sift_cake, cooked_kazoo_fish,
                                      cooked_fanfare_eel, cooked_gobbler_fillet, stomper_steak, kazoo_fish_sushi, fanfare_eel_sushi,
                                      gobbler_sushi, sushi_platter)}



# ============================================================================ gear

DIAMOND = hue_between(150, 200, 0.2)    # vanilla diamond (tools, armour)
STICK = hue_between(25, 50, 0.45)       # vanilla stick wood


PRISM_GEAR = ramp('#2a2048', '#6a5ea8', '#9a8ed0', '#c0b6ec', '#dcd6f8', '#f4f2ff', '#ffffff')


def _prism(name):
    """A vanilla diamond tool or armour piece cut in Prism: the diamond re-toned to opal, the handle left as it is."""
    return opal(retone(van(name), PRISM_GEAR, DIAMOND), lambda c: not STICK(c), 0.5, 7.0)


def prism_sword():
    return _prism('diamond_sword')


def prism_pickaxe():
    return _prism('diamond_pickaxe')


def prism_axe():
    return _prism('diamond_axe')


def prism_shovel():
    return _prism('diamond_shovel')


def prism_hoe():
    return _prism('diamond_hoe')


def prism_helmet():
    return _prism('diamond_helmet')


def prism_chestplate():
    return _prism('diamond_chestplate')


def prism_leggings():
    return _prism('diamond_leggings')


def prism_boots():
    return _prism('diamond_boots')


def _line(rows, a, b, ch, over=''):
    """Writes ch along a straight texel line from a to b (only over '.' and the characters in over)."""
    (x0, y0), (x1, y1) = a, b
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    for k in range(n + 1):
        x = round(x0 + (x1 - x0) * k / n)
        y = round(y0 + (y1 - y0) * k / n)
        if rows[y][x] == '.' or rows[y][x] in over:
            rows[y][x] = ch


def slingshot(pull=-1):
    """A forked stick held like a vanilla tool (grip bottom left, fork top right) with a teal band of sculk string and
    a leather pouch; pull 0-2 draws the pouch back toward the grip."""
    rows = [['.'] * 16 for _ in range(16)]
    fork = (7, 9)
    _line(rows, (1, 14), fork, 'v')                 # the grip, its lit edge above it
    _line(rows, (2, 12), (6, 8), 'w')
    _line(rows, fork, (6, 1), 'v')                  # the upper prong
    _line(rows, (6, 8), (5, 1), 'w')
    _line(rows, fork, (14, 7), 'v')                 # the right prong
    _line(rows, (7, 8), (14, 6), 'w')
    for x, y in ((2, 12), (3, 11), (2, 13), (3, 12)):
        rows[y][x] = 'g' if (x + y) % 2 else 'G'    # a leather grip
    tips = [(6, 2), (13, 7)]
    pouch = [(10, 4), (9, 6), (8, 8), (6, 10)][pull + 1]
    for t in tips:
        _line(rows, t, pouch, 'b', over='vw' if pull >= 0 else '')
    px_, py_ = pouch
    rows[py_][px_] = 'p'
    pal = {'w': ('#a88a48', '#281e0b'), 'v': ('#684e1e', '#281e0b'), 'g': ('#8a5a34', '#2e1a0c'), 'G': ('#5a3a1e', '#2e1a0c'),
           'b': ('#3fc2bc', '#0c3a44'), 'p': ('#8a5a34', '#2e1a0c')}
    img = paint([''.join(r) for r in rows], pal, no_ol='bp')
    put(img, [(5, 1), (14, 6)], '#c8a868')
    return img


def _faces(u, v, w, h, d):
    """Box UV faces (x, y, width, height) of a cuboid at (u, v), as vanilla lays them out."""
    return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
            'west': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h)}


PLATE = ramp('#2c2448', '#5a5288', '#8a86b8', '#b4b2d8', '#d4d2ec', '#f0effa')
PLATE_BAND = (rgba('#f2b6d6'), rgba('#b6f0dc'))


def _plates(px, rect, rows=None, seed=0, trim_rows=(), skip=()):
    """One face as armour plate in the diamond armour's manner: lit top row and left column, shadowed bottom row and
    right column, a few clustered sheen texels, and an optional band of pastel facets."""
    t = PLATE
    x0, y0, w, h = rect
    lo, hi = rows or (0, h)
    for y in range(lo, hi):
        for x in range(w):
            if (x, y) in skip:
                continue
            if y in trim_rows:
                c = PLATE_BAND[(x + seed) % 2]
            elif y == lo:
                c = t[5]
            elif y == hi - 1:
                c = t[2]
            elif x == 0:
                c = t[4]
            elif x == w - 1:
                c = t[2]
            else:
                c = t[4] if (x * 7 + y * 13 + seed * 5) % 11 == 0 else t[3]
            px[x0 + x, y0 + y] = c


def armour_layers():
    """The worn Prism armour: {texture path: image} for the humanoid (helmet, chestplate, boots) and leggings layers."""
    hum = blank(64, 32)
    hp = hum.load()
    for k, r in _faces(0, 0, 8, 8, 8).items():
        if k == 'north':
            # the visor: a rim round the face, open over the eyes and mouth
            skip = {(x, y) for y in range(3, 8) for x in range(1, 7)}
            _plates(hp, r, seed=1, skip=skip)
            for x in range(8):
                hp[r[0] + x, r[1] + 2] = PLATE_BAND[x % 2]
        elif k != 'down':
            _plates(hp, r, seed=2, trim_rows=(2,))
    body = _faces(16, 16, 8, 12, 4)
    for k, r in body.items():
        _plates(hp, r, seed=3 if k in ('up', 'down') else 4, trim_rows=() if k in ('up', 'down') else (9,))
    nx, ny = body['north'][:2]
    for (x, y, c) in ((3, 3, PLATE[5]), (4, 3, PLATE[5]), (3, 4, PLATE_BAND[0]), (4, 4, PLATE_BAND[1]), (3, 5, PLATE[2]), (4, 5, PLATE[2])):
        hp[nx + x, ny + y] = c
    for k, r in _faces(40, 16, 4, 12, 4).items():
        if k != 'down':
            _plates(hp, r, rows=(0, 5) if k != 'up' else None, seed=5, trim_rows=(4,))
    for k, r in _faces(0, 16, 4, 12, 4).items():
        if k != 'up':
            _plates(hp, r, rows=(6, 12) if k != 'down' else None, seed=6, trim_rows=(6,))
    lg = blank(64, 32)
    lp = lg.load()
    for k, r in _faces(16, 16, 8, 12, 4).items():
        if k != 'up':
            _plates(lp, r, rows=(7, 12) if k != 'down' else None, seed=7, trim_rows=(7,))
    for k, r in _faces(0, 16, 4, 12, 4).items():
        if k != 'up':
            _plates(lp, r, rows=(0, 10) if k != 'down' else None, seed=8, trim_rows=(5,))
    return {'entity/equipment/humanoid/prism': hum, 'entity/equipment/humanoid_leggings/prism': lg}


def g_gear():
    out = {f.__name__: f() for f in (prism_sword, prism_pickaxe, prism_axe, prism_shovel, prism_hoe, prism_helmet, prism_chestplate,
                                     prism_leggings, prism_boots)}
    out['slingshot'] = slingshot(-1)
    for i in range(3):
        out[f'slingshot_pulling_{i}'] = slingshot(i)
    return out



# ============================================================================ music sheets and the disc

SHEET_BORDER = {'#736041', '#877251', '#947f5d'}


def music_sheet(song):
    """A vanilla map's folded parchment written as a music sheet: a staff of three lines, the song's first notes rising
    and falling as it goes, the note heads in the song's colour, and its wax seal in the corner."""
    import songs
    notes, seal = songs.SONGS[song]
    img = van('map')
    px = img.load()
    border = {rgba(c)[:3] for c in SHEET_BORDER}
    for y in range(16):
        for x in range(16):
            c = px[x, y]
            if c[3] and c[:3] not in border and c[:3] != rgba('#baa57f')[:3]:
                px[x, y] = rgba('#fff0d1') if x + y < 17 else rgba('#f2e2c2')
    line = rgba('#baa57f')
    for y in (5, 8, 11):
        for x in range(3, 13):
            if px[x, y][3] and px[x, y][:3] not in border:
                px[x, y] = line
    ink = rgba('#4a3a22')
    head = rgba(seal)
    shade = mix(seal, '#000000', 0.35)
    shown = notes[:4]
    for i, p_ in enumerate(shown):
        x = 3 + i * 3
        y = 12 - round(p_ / 24 * 7)
        px[x, y] = head
        px[x + 1, y] = shade
        for yy in range(y - 3, y):
            if 1 < yy:
                px[x + 1, yy] = ink
    px[12, 12] = head
    px[13, 12] = shade
    px[12, 13] = shade
    px[13, 13] = mix(seal, '#000000', 0.55)
    return img


def music_disc_lullaby():
    """A vanilla disc with a lullaby-lilac label."""
    return remap(van('music_disc_cat'), {'#1f6800': '#6a3aa8', '#4cff00': '#c8a0ff'})


# ============================================================================ books and scrolls

# the vanilla book's colours: outline, cover shadow, spine dark, spine, cover marks, cover; and its pages
_BOOK = ('#161005', '#312104', '#44250a', '#522e10', '#543e13', '#654b17')
_PAGES = ('#5b5b5b', '#999999', '#b7b7b7', '#d6d6d6')
CREAM = ('#5e5446', '#a89c86', '#ccc0a6', '#ece2c8')

# leathers in the same order: outline, shadow, spine dark, spine, marks, cover
LEATHER = {
    'knowledge': ('#070b1e', '#141c44', '#6a4a10', '#b88a2a', '#c49a3a', '#24346e'),
    'creator': ('#3a2a12', '#a89e86', '#8a6412', '#c99a2c', '#d9ae3a', '#e8e2d2'),
    'pillager': ('#1e0f06', '#3e2414', '#4e2c16', '#6a3c22', '#9a5a30', '#7a4a2a'),
    'cultist': ('#03100f', '#082628', '#0a2c34', '#123e46', '#2fa8a4', '#16505a'),
    'ocean': ('#1a2a3c', '#5a6e82', '#a8404a', '#d8606a', '#e0e8ee', '#90a4b6'),
    'soul': ('#060a20', '#101c50', '#0e1640', '#1a2a70', '#5fb0ff', '#22357e'),
    'sift': ('#1e0c16', '#3e1e2e', '#4a2236', '#5e2e46', '#6e3a56', '#7e4864'),
}
PAGES = {
    'knowledge': CREAM, 'creator': ('#6a604a', '#c8bea4', '#e2d8bc', '#f8f2e0'), 'pillager': ('#5a4a30', '#9a8460', '#b8a078', '#d2bc90'),
    'cultist': ('#3a4440', '#7e8a84', '#9eaaa2', '#bcc8c0'), 'ocean': ('#4a5a66', '#a4b4be', '#c8d4dc', '#e8f0f4'),
    'soul': ('#2a3a5e', '#7a90c0', '#a8bce0', '#d0e0f8'),
}
ORIGINS = ('creator', 'pillager', 'cultist', 'ocean', 'soul')


def _leather(base, key):
    cmap = dict(zip(_BOOK, LEATHER[key]))
    cmap.update(zip(_PAGES, PAGES.get(key, CREAM)))
    return remap(van(base), cmap)


def knowledge_book():
    """The vanilla book in navy leather with gilt marks and a gold spine."""
    return _leather('book', 'knowledge')


def lore_book(o):
    """A vanilla book in its writer's leather: the Creator's white and gold, a Pillager's hide, the cult's sculk, the
    Tide-Keepers' silver with a coral spine, the Soul Dimension's deep blue with glowing marks."""
    return _leather('book', o)


def _shades(c):
    """Four tones of colour c, shadow to light, hue-shifted (cool shadows, warm lights)."""
    h, s_, v = hsv(rgba(c))
    out = []
    for i in range(4):
        t = i / 3
        hh = (h + (0.03 * (0.5 - t) if 0.1 < h < 0.75 else -0.02 * (0.5 - t))) % 1.0
        r, g, b = colorsys.hsv_to_rgb(hh, min(1.0, s_ * (1.15 - 0.35 * t)), min(1.0, v * (0.45 + 0.75 * t)))
        out.append((round(r * 255), round(g * 255), round(b * 255), 255))
    return out


ENCHANT_RIBBON = {'melody_steps': '#f07ac8', 'hushed_step': '#a67bff', 'sculk_ward': '#22b0a8', 'echo_strike': '#3fd8f0',
                  'resonance': '#e0404a', 'crescendo': '#ff8a3d', 'reverb': '#4a8ae8', 'fortissimo': '#e8b830'}


def sift_book(eid):
    """The vanilla enchanted book bound in Sift leather (the plum of the turf-side earth), its ribbon in the
    enchantment's colour, the gold clasp kept."""
    r = _shades(ENCHANT_RIBBON[eid])
    cmap = dict(zip(_BOOK, LEATHER['sift']))
    cmap.update(zip(_PAGES, CREAM))
    cmap.update({'#443310': '#5a2e44', '#611414': r[0], '#6c1717': r[0], '#892120': r[1], '#a42c2b': r[2], '#9d1b37': r[2],
                 '#c51339': r[3]})
    return remap(van('enchanted_book'), cmap)


SCROLL = {  # rod light, rod dark, knob, paper light, paper, paper shade, ink, seal
    'creator': ('#f6efdc', '#d9c8a0', '#d9a72c', '#fffaf0', '#f2ead6', '#d8ccb0', '#b08a3a', '#d9a72c'),
    'pillager': ('#8a5634', '#5a3420', '#c7713f', '#e8d2a6', '#d2b483', '#b0915e', '#5a3a22', '#a83a2a'),
    'cultist': ('#14484e', '#0a2c34', '#3fc2bc', '#c8d4cc', '#aebcb2', '#8a9a90', '#0f4650', '#3fc2bc'),
    'ocean': ('#d0dae0', '#8a98a4', '#e87078', '#f4f8fa', '#e0eaee', '#c0ccd4', '#4a6a88', '#e87078'),
    'soul': ('#22357e', '#101c50', '#5fb0ff', '#e4f0ff', '#c8dcf8', '#a0b8e0', '#1f3a90', '#5fb0ff'),
}


def lore_scroll(o):
    """A sheet hanging between two rods: written lines, a wax seal, rods and knobs in the writer's colours."""
    rows = [
        '................',
        '..kRRRRRRRRRRk..',
        '..krrrrrrrrrrk..',
        '...PPPPPPPPPpp..',
        '...PiiiiiiPPpp..',
        '...PPPPPPPPPpp..',
        '...PiiiiPiiiPp..',
        '...PPPPPPPPPpp..',
        '...PiiiiiiiPpp..',
        '...PPPPPPPPPpp..',
        '...PiiiiPPPSpp..',
        '...PPPPPPPPSSp..',
        '..kRRRRRRRRRRk..',
        '..krrrrrrrrrrk..',
        '................',
        '................',
    ]
    rl, rd, kn, pl, pp, ps, ink, seal = SCROLL[o]
    ol = _auto_ol(rgba(rd))
    pal = {'R': (rl, ol), 'r': (rd, ol), 'k': (kn, ol), 'P': (pp, ol), 'p': (ps, ol), 'i': (ink, ol), 'S': (seal, ol)}
    img = paint(rows, pal)
    put(img, [(4, 3), (5, 3), (3, 3), (3, 4)], pl)
    put(img, [(3, 1), (3, 12)], mix(rl, '#ffffff', 0.5))
    return img


def g_books():
    out = {'knowledge_book': knowledge_book()}
    for o in ORIGINS:
        out[f'{o}_lore_book'] = lore_book(o)
    for o in ORIGINS:
        out[f'{o}_lore_scroll'] = lore_scroll(o)
    for eid in ENCHANT_RIBBON:
        out[f'sift_book_{eid}'] = sift_book(eid)
    return out


def g_music():
    import songs
    out = {f'music_sheet_{s_}': music_sheet(s_) for s_ in songs.SONGS}
    out['music_disc_lullaby'] = music_disc_lullaby()
    return out



# ============================================================================ placeable things with item sprites

DOORS = {  # vanilla door item, wood ramp (outline -> light)
    'lullwood_door': ('birch_door', ramp('#2a2440', '#5a5280', '#8a82b4', '#9f97c6', '#b1a9d4', '#c2bbe0', '#d3cdea')),
    'wishwood_door': ('cherry_door', ramp('#3a1424', '#6f3355', '#a85070', '#c9738f', '#d98aa4', '#e6a0b8', '#f0b6ca')),
    'blightwood_door': ('dark_oak_door', ramp('#08181a', '#163236', '#1d4044', '#244e52', '#2d5d60', '#386d6e', '#467f7d')),
}
GLOW = ramp('#0a5a60', '#1fa8a8', '#4ae8de', '#a8fff4', '#e8fffc')


def door(name):
    base, tones = DOORS[name]
    return retone(van(base), tones)


def bulb_lantern():
    """Vanilla's lantern, its flame swapped for a Bulb's cyan glow."""
    flame = lambda c: hsv(c)[1] > 0.35 and 15 <= hsv(c)[0] * 360 <= 65 and lum(c) > 90  # noqa: E731
    return retone(van('lantern'), GLOW, flame)


def glowbell_vine():
    """Vanilla's glow berries on a teal vine, the berries become glowing cyan bells."""
    berry = lambda c: hsv(c)[1] > 0.35 and 15 <= hsv(c)[0] * 360 <= 60  # noqa: E731
    leaf = lambda c: hsv(c)[1] > 0.2 and 45 <= hsv(c)[0] * 360 <= 170  # noqa: E731
    img = retone(van('glow_berries'), ramp('#0a3a40', '#1a7a80', '#3ab8b8', '#7ae8e0', '#c8fff8'), berry)
    return retone(img, ramp('#0a2a26', '#145048', '#1f7a6a', '#3aa08a', '#6ac8a8'), leaf)


WATER = hue_between(185, 260, 0.25)


def sculk_water_bucket():
    return retone(van('water_bucket'), ramp('#031014', '#06222a', '#0c3a44', '#145660', '#1f8a8a'), WATER)


def chrome_bucket_frames(n=8):
    """Vanilla's water bucket full of Chrome: its rainbow runs across the surface, frame by frame."""
    base = van('water_bucket')
    frames = []
    for f in range(n):
        def fn(c, x, y, f=f):
            v = hsv(c)[2]
            hue = ((x + y * 0.5) / 12.0 + f / n) % 1.0
            r, g, b = colorsys.hsv_to_rgb(hue, 0.35, min(1.0, 0.55 + 0.5 * v))
            return (round(r * 255), round(g * 255), round(b * 255), c[3])
        frames.append(recolour_where(base, lambda c, x, y: WATER(c), fn))
    return frames


def g_misc():
    out = {n: door(n) for n in DOORS}
    for f in (bulb_lantern, glowbell_vine, sculk_water_bucket):
        out[f.__name__] = f()
    out['chrome_bucket'] = chrome_bucket_frames()[0]
    return out


GROUPS = {'currency': g_currency, 'food': g_food, 'gear': g_gear, 'books': g_books, 'music': g_music, 'misc': g_misc}


def _strip(frames):
    w, h = frames[0].size
    img = Image.new('RGBA', (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        img.paste(f, (0, i * h))
    return img


def textures(out):
    for g in GROUPS.values():
        for name, img in g().items():
            if name == 'chrome_bucket':
                continue
            out(f'item/{name}', img)
    out('item/chrome_bucket', _strip(chrome_bucket_frames()), {'animation': {'frametime': 4, 'interpolate': True}})
    for name, img in armour_layers().items():
        out(name, img)
