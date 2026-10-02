"""Every Sift item sprite, hand-drawn at 16x16 in the vanilla item style.

Tools and armour are the vanilla diamond textures recoloured tone-for-tone into the Siftite
palette (so the silhouettes match Minecraft 1:1), with a few hand-placed detail pixels. Every
other sprite is drawn by hand below as rows of characters with its own palette: one character
per pixel, '.' is transparent. Most sprites list only their fill and shading; `ol=True` then adds
the one-pixel outline in a dark, hue-shifted shade of each material (a little lighter on the
lit top-left side), exactly where a pixel artist would put it.

Light always comes from the top left.
"""
from __future__ import annotations

import colorsys
import os

from PIL import Image

VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')


# ============================================================================ helpers

def rgba(c):
    if isinstance(c, tuple):
        return c if len(c) == 4 else (*c, 255)
    c = c.lstrip('#')
    a = int(c[6:8], 16) if len(c) == 8 else 255
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)


def shade(c, v=0.32, hue=0.07, sat=0.18):
    """A darker shade of colour c, hue-shifted toward violet the way shadows are painted."""
    r, g, b, a = rgba(c)
    h, s, val = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    target = 0.72
    d = (target - h + 0.5) % 1.0 - 0.5  # shortest way round the hue circle toward violet
    h = (h + max(-hue, min(hue, d))) % 1.0
    s = min(1.0, s + sat) if s > 0.05 else s
    r, g, b = colorsys.hsv_to_rgb(h, s, val * v)
    return (round(r * 255), round(g * 255), round(b * 255), 255)


def mix(a, b, t):
    a, b = rgba(a), rgba(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def grid(rows, pal, ol=False, size=None, no_ol=''):
    """Paints rows of characters with palette pal. A palette value is a colour, or a pair
    (colour, outline colour) to give that material its own outline. With ol=True every empty
    pixel that touches a filled one edge-on becomes outline: darkest on the bottom/right
    (shadow) side, a step lighter on the top/left (lit) side. Characters in no_ol (glints,
    sparkles, strings) cast no outline."""
    w, h = size or (16, 16)
    assert len(rows) == h, f'{len(rows)} rows, expected {h}'
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    cells = {}
    for y, row in enumerate(rows):
        assert len(row) == w, f'row {y} is {len(row)} wide: {row!r}'
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            if ch not in pal:
                raise KeyError(f'no colour for {ch!r} in row {y}: {row!r}')
            v = pal[ch]
            col = rgba(v[0] if isinstance(v, (list, tuple)) and not isinstance(v[0], int) else v)
            px[x, y] = col
            cells[(x, y)] = ch
    if ol:
        def ol_of(ch):
            v = pal[ch]
            if isinstance(v, (list, tuple)) and not isinstance(v[0], int):
                return rgba(v[1])
            return shade(v)
        add = {}
        for y in range(h):
            for x in range(w):
                if (x, y) in cells:
                    continue
                # prefer the neighbour that puts this pixel in shadow (right/bottom edges)
                best = None
                for dx, dy, lit in ((-1, 0, False), (0, -1, False), (1, 0, True), (0, 1, True)):
                    ch = cells.get((x + dx, y + dy))
                    if ch is None or ch in no_ol:
                        continue
                    if best is None or (best[1] and not lit):
                        best = (ch, lit)
                if best:
                    c = ol_of(best[0])
                    add[(x, y)] = mix(c, pal_col(pal, best[0]), 0.28) if best[1] else c
        for (x, y), c in add.items():
            px[x, y] = c
    return img


def pal_col(pal, ch):
    v = pal[ch]
    return rgba(v[0] if isinstance(v, (list, tuple)) and not isinstance(v[0], int) else v)


def ramp(chars, colours, ol=None):
    """Assigns a ramp of colours (dark to light) to chars, all sharing one outline colour."""
    o = ol or shade(colours[len(colours) // 2], 0.3)
    return {ch: (c, o) for ch, c in zip(chars, colours)}


def vanilla(name):
    return Image.open(os.path.join(VANILLA, 'item', name + '.png')).convert('RGBA')


def recolour(img, cmap, name=''):
    out = img.copy()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            c = px[x, y]
            if c[3] == 0:
                continue
            key = '#%02x%02x%02x' % c[:3]
            if key not in cmap:
                raise KeyError(f'{name}: vanilla colour {key} at {x},{y} has no Siftite tone')
            px[x, y] = rgba(cmap[key])[:3] + (c[3],)
    return out


def dots(img, spec, pal):
    """Hand-places detail pixels from a string 'x,y,ch x,y,ch ...'."""
    px = img.load()
    for tok in spec.split():
        x, y, ch = tok.split(',')
        px[int(x), int(y)] = rgba(pal[ch])
    return img


# ============================================================================ palettes

# Siftite: the dreamy pastel metal. Indigo outlines, cyan body, blush-white highlights.
S_OUT, S_OUT2 = '#261a56', '#44388e'
S_RAMP = ['#5a4aa8', '#6a7ad8', '#6ab0e8', '#7ed8ee', '#b8f0f6', '#eefbff', '#fff0fa']
PINK = ['#6e2a66', '#b04d98', '#f29bd6', '#ffd6f0']
GOLD = ['#4a2418', '#8a4e22', '#c9862c', '#eebd4a', '#fde58f', '#fffbe0']
WOOD = ['#2a1a16', '#4d3220', '#6e4c2a', '#8f6a36', '#b18a52']
BONE = ['#4a3e48', '#8d8070', '#bdb29a', '#e2dac4', '#f6f2e6', '#ffffff']
SCULK = ['#06141c', '#0b2532', '#123a48', '#1b5462', '#27707c']
GLOW = ['#0f8f99', '#22c7c4', '#3ff5e6', '#a8fff6', '#ffffff']
CHROME = ['#2e2470', '#4b3f9e', '#7f86e6', '#b9b8ff', '#ffd2f2', '#ffffff']
SERBIM = ['#173a6e', '#1f6d94', '#2b8fbb', '#3fb0da', '#66d2f0', '#9aeefc', '#e4fdff']
LEAF = ['#163a32', '#25603f', '#3a8f4a', '#5cba58', '#9ade78']
WHITE = '#ffffff'

# vanilla diamond tone -> Siftite tone of the same rank
TOOL_MAP = {
    # blade / head
    '#082520': S_OUT, '#0e3f36': S_OUT2, '#156355': S_RAMP[0], '#1e8a77': S_RAMP[1], '#27b29a': S_RAMP[2],
    '#2bc7ac': S_RAMP[2], '#33ebcb': S_RAMP[3], '#a4fdf0': S_RAMP[5], '#f2fffd': S_RAMP[6],
    # armour
    '#1aaaa7': S_RAMP[1], '#20c5b5': S_RAMP[2], '#4aedd9': S_RAMP[3], '#a1fbe8': S_RAMP[4], '#ffffff': S_RAMP[6],
    # wooden handle (kept vanilla-ish, a touch warmer)
    '#281e0b': '#2b1b10', '#493615': '#4d3419', '#684e1e': '#6e4f24', '#896727': '#906c31',
    # the spear's grip, re-wrapped in plum leather
    '#411805': '#3a1a3c', '#67290b': '#5e2a56', '#834121': '#7a3c6c', '#a34b22': '#94507e', '#ba5f34': '#b06c98',
}
GEM = {'p': PINK[2], 'P': PINK[1], 'q': PINK[3], 'w': WHITE, 'h': S_RAMP[5], 'o': PINK[0], 'g': GOLD[3], 'G': GOLD[2]}

# ---------------------------------------------------------------------------- the music motif
# Siftite gear is engraved like sheet music: staff lines cut into blades and plates, pink notes
# inlaid along them, a pair of beamed quavers on the breastplate (and a treble clef on the worn
# armour, which has the room). The textures animate: a pulse of light runs along each staff line
# and each note glints as the light passes it.

FRAMES, FRAMETIME = 16, 3
ENGRAVE = S_RAMP[1]   # the cut line, a step darker than the metal around it
NOTE, NOTE_HI, NOTE_LO = PINK[2], PINK[3], PINK[1]

# the breastplate's beamed quavers, in the order the light traces them: left head, up its stem,
# across the beam, down the right stem, right head. Capital letters mark the shaded pixels.
QUAVERS = [(5, 10, 'p'), (6, 10, 'p'), (5, 11, 'P'), (6, 11, 'P'), (6, 9, 'p'), (6, 8, 'P'), (6, 7, 'p'), (7, 8, 'P'),
           (7, 7, 'p'), (8, 7, 'P'), (8, 6, 'p'), (9, 7, 'P'), (9, 6, 'p'), (10, 7, 'P'), (10, 6, 'p'), (10, 8, 'p'),
           (10, 9, 'p'), (9, 10, 'p'), (10, 10, 'p'), (9, 11, 'P'), (10, 11, 'P')]
_Q = {(x, y) for x, y, _ in QUAVERS}


def _row(y, x0, x1, skip=()):
    return [(x, y) for x in range(x0, x1 + 1) if (x, y) not in skip]


def _note(x, y):
    """A crotchet: a two-pixel head at (x, y) with its stem rising from the right."""
    return [(x, y, 'p'), (x + 1, y, 'p'), (x + 1, y - 1, 'P'), (x + 1, y - 2, 'P')]


# per item: lines = engraved staff lines (the light runs along them in list order), glows = light
# paths with no engraving (heads too thin to cut), notes = inlaid notes (single pixels, or glyphs
# of (x, y, shade)), detail = static gem / glint pixels.
MUSIC = {
    'sword': dict(lines=[[(7, 8), (8, 7), (9, 6), (10, 5), (11, 4), (12, 3), (13, 2)]],
                  notes=[(8, 8), (10, 4), (13, 3)], detail='5,9,p 4,9,P 5,8,q 14,1,w'),
    'pickaxe': dict(glows=[[(6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (11, 4), (12, 5), (13, 6), (13, 7), (13, 8), (13, 9), (13, 10)]],
                    notes=[(8, 3), (12, 5), (13, 9)]),
    'axe': dict(glows=[[(10, 2), (9, 2), (8, 3), (7, 4), (7, 5)], [(10, 6), (11, 7), (12, 7)]],
                notes=[[(8, 5, 'p'), (9, 5, 'p'), (9, 4, 'P'), (9, 3, 'P')]]),
    'shovel': dict(glows=[[(9, 5), (10, 4), (11, 3), (12, 3), (13, 4), (13, 5), (12, 6), (11, 7)]],
                   notes=[[(10, 5, 'p'), (11, 5, 'p'), (11, 4, 'P')]]),
    'hoe': dict(glows=[[(7, 2), (8, 2), (9, 2), (10, 3), (11, 4), (12, 5)]], notes=[(8, 2), (11, 5)]),
    'spear': dict(lines=[[(10, 6), (11, 5), (12, 4), (13, 3), (14, 2)]], notes=[(10, 4), (12, 2)]),
    'spear_in_hand': dict(lines=[[(6, 5), (5, 4), (4, 3), (3, 2), (2, 1)]], notes=[(2, 3), (4, 5)]),
    # two staff lines around the helmet's brow, notes sitting between them
    'helmet': dict(lines=[_row(4, 5, 10), _row(6, 4, 11)], notes=[(6, 5), (9, 5)]),
    # three faint staff lines across the breastplate, beamed quavers on them, a note on each shoulder
    'chestplate': dict(lines=[_row(8, 4, 11, _Q), _row(10, 4, 11, _Q), _row(12, 4, 11, _Q)], quavers=True,
                       notes=[(3, 4), (12, 4)], engrave=S_RAMP[2]),
    # a staff around the waist, a crotchet on each leg
    'leggings': dict(lines=[_row(3, 4, 10), _row(5, 4, 11)],
                     glows=[[(4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (4, 11)], [(10, 7), (10, 8), (10, 9), (10, 10), (10, 11)]],
                     notes=[(6, 4), (9, 4), _note(4, 10), _note(10, 10)]),
    'boots': dict(glows=[[(4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (3, 9), (2, 10)], [(10, 4), (10, 5), (10, 6), (10, 7), (10, 8), (11, 9), (12, 10)]],
                  notes=[_note(4, 8), _note(10, 8)]),
}


def _pulse(i, length, f, delay):
    """How brightly the travelling light shines on step i of a path in frame f: 2 core, 1 halo."""
    t = (f - delay) % FRAMES
    span = FRAMES * 0.75  # the light crosses in three quarters of the loop, then the metal rests
    if t > span:
        return 0
    s = -1.5 + (length + 3) * t / span
    d = abs(i - s)
    return 2 if d < 0.6 else 1 if d < 1.6 else 0


def _lit(shade, lv):
    """A pink inlay pixel at pulse level lv."""
    if shade == 'P':
        return (NOTE_LO, NOTE, NOTE_HI)[lv]
    return (NOTE, NOTE_HI, WHITE)[lv]


def music_frames(name):
    m = MUSIC[name]
    import siftite_art  # H: the tools get their own curved silhouettes (the armour keeps diamond's)
    if name in siftite_art.CURVED:
        base, line, notes = siftite_art.curved_tool(name)
        m = dict(lines=[line], notes=notes)
    else:
        base = recolour(vanilla('diamond_' + name), TOOL_MAP, name)
        if name in ('helmet', 'chestplate', 'leggings', 'boots'):
            siftite_art.echo_lattice(base, (S_RAMP[2], S_RAMP[3]))
    if m.get('detail'):
        dots(base, m['detail'], GEM)
    tracks = [(p, 'line', k * 3) for k, p in enumerate(m.get('lines', []))]
    tracks += [(p, 'glow', k * 3) for k, p in enumerate(m.get('glows', []))]
    if m.get('quavers'):
        tracks.append(([(x, y) for x, y, _ in QUAVERS], 'quavers', 5))
    shade = {(x, y): s for x, y, s in QUAVERS}

    def nearest(pt):
        best = None
        for p, kind, delay in tracks:
            for i, q in enumerate(p):
                d = abs(q[0] - pt[0]) + abs(q[1] - pt[1])
                if best is None or d < best[0]:
                    best = (d, i, len(p), delay)
        return best[1:]

    notes = []
    for n in m.get('notes', []):
        glyph = n if isinstance(n, list) else [(n[0], n[1], 'p')]
        notes.append((glyph, nearest(glyph[0][:2])))
    frames = []
    for f in range(FRAMES):
        img = base.copy()
        px = img.load()
        for p, kind, delay in tracks:
            for i, (x, y) in enumerate(p):
                lv = _pulse(i, len(p), f, delay)
                if kind == 'line':
                    c = (m.get('engrave', ENGRAVE), S_RAMP[4], WHITE)[lv]
                elif kind == 'quavers':
                    c = _lit(shade[(x, y)], lv)
                else:
                    c = (px[x, y], mix(px[x, y], S_RAMP[5], 0.65), S_RAMP[6])[lv]
                px[x, y] = rgba(c)
        for glyph, (i, length, delay) in notes:
            lv = _pulse(i, length, f, delay)
            for x, y, s in glyph:
                px[x, y] = rgba(_lit(s, lv))
        frames.append(img)
    return frames


def item_animations():
    """{texture name: (frames, frametime)} for every animated item texture."""
    return {'siftite_' + n: (music_frames(n), FRAMETIME) for n in MUSIC}


def tools():
    return {k: v[0][0] for k, v in item_animations().items()}


# ---------------------------------------------------------------------------- worn armour

TREBLE = [  # 5 x 11, for the 8 x 12 front of the worn breastplate
    '...X.',
    '..X.X',
    '..X.X',
    '..XX.',
    '.XX..',
    'X.X..',
    'X.XX.',
    'X.X.X',
    '.XXX.',
    '..X..',
    'XX...',
]


def armor_layers():
    """The worn Siftite armour (static: entity textures cannot animate), engraved to match the
    icons: staff lines wrapping every plate, inlaid pink crotchets, a treble clef on the chest."""
    def boxfaces(u, v, w, h, d):
        return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'west': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
                'east': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}

    def plate(px, rect, lines=(), notes=()):
        fx, fy, fw, fh = rect
        for y in range(fh):
            for x in range(fw):
                if y == 0 or x == 0:
                    c = S_RAMP[4]
                elif y == fh - 1 or x == fw - 1:
                    c = S_RAMP[1]
                elif y in lines:
                    c = S_RAMP[2]
                elif (x + y) % 4 == 0 and (x - y) % 4 == 0:
                    c = '#3ff5e6'  # H: an echo-teal glint where the diamond lattice crosses
                elif (x + y) % 4 == 0 or (x - y) % 4 == 0:
                    c = mix(S_RAMP[3], '#22c7c4', 0.3)
                else:
                    c = mix(S_RAMP[3], S_RAMP[4], 0.35) if y <= fh // 4 else S_RAMP[3]
                px[fx + x, fy + y] = rgba(c)
        for (x, y) in notes:  # a crotchet: two-pixel head, stem rising from its right
            for (dx, dy), c in (((0, 0), NOTE), ((1, 0), NOTE), ((1, -1), NOTE_LO), ((1, -2), NOTE_LO)):
                if 0 < x + dx < fw - 1 and 0 < y + dy < fh - 1:
                    px[fx + x + dx, fy + y + dy] = rgba(c)

    staff = (2, 4, 6, 8, 10)
    hum = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    hp = hum.load()
    # helmet: a two-line staff circling the head, a note on every side
    for i, (k, r) in enumerate(boxfaces(0, 0, 8, 8, 8).items()):
        plate(hp, r, (3, 5), [(1 + (i * 3) % 5, 5 if i % 2 else 3)])
    for (x, y) in ((9, 10), (10, 10), (13, 10), (14, 10)):
        hp[x, y] = (0, 0, 0, 0)  # visor eye slits
    for y in range(12, 16):
        for x in range(10, 14):
            hp[x, y] = (0, 0, 0, 0)  # open face
    # breastplate: a full five-line staff wrapping the body, the clef on the front
    chest = {'north': [], 'south': [(1, 8), (4, 6)], 'west': [(1, 6)], 'east': [(1, 8)]}
    for k, r in boxfaces(16, 16, 8, 12, 4).items():
        plate(hp, r, staff if k in chest else (), chest.get(k, ()))
    for j, row in enumerate(TREBLE):
        for i, ch in enumerate(row):
            if ch == 'X':
                hp[21 + i, 20 + j] = rgba(NOTE_HI if j < 3 else NOTE)
    # arms and boots: the staff runs on, a note climbing each face
    for (u, v) in ((40, 16), (0, 16)):
        for i, (k, r) in enumerate(boxfaces(u, v, 4, 12, 4).items()):
            plate(hp, r, () if k in ('up', 'down') else (4, 6, 8), [] if k in ('up', 'down') else [(1, 6 if i % 2 else 8)])
    leg = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    lp = leg.load()
    for k, r in boxfaces(16, 16, 8, 12, 4).items():
        plate(lp, r, () if k in ('up', 'down') else (2, 4), [(4, 4)] if k in ('north', 'south') else [])
    for i, (k, r) in enumerate(boxfaces(0, 16, 4, 12, 4).items()):
        plate(lp, r, () if k in ('up', 'down') else (4, 6, 8), [] if k in ('up', 'down') else [(1, 8 if i % 2 else 6)])
    return {'entity/equipment/humanoid/siftite': hum, 'entity/equipment/humanoid_leggings/siftite': leg}


# ============================================================================ materials

INGOT = [
    '................',
    '................',
    '..........OO....',
    '.......OOO442...',
    '....OOO4555542..',
    '.OOO45555555542.',
    'Ow5555555555ww52',
    'O4w555555www524o',
    'O44w55www532234o',
    'O444ww532222444o',
    'O344532222443oo.',
    '.O34532233ooo...',
    '..O3431ooo......',
    '...OOoo.........',
    '................',
    '................',
]


def siftite_ingot():
    """The vanilla ingot shape in siftite."""
    rows = list(INGOT)
    # an inlaid streak of pink sift running along the top face, brightest where it meets the light
    rows[5] = '.OOO455555qq542.'
    rows[6] = 'Ow55555ppp55ww52'
    rows[7] = 'O4w55PP55www524o'
    pal = {'o': S_OUT, 'O': S_OUT2, '1': S_RAMP[0], '2': S_RAMP[1], '3': S_RAMP[2], '4': S_RAMP[3], '5': S_RAMP[4], 'w': S_RAMP[5],
           'p': PINK[2], 'P': PINK[1], 'q': PINK[3]}
    return grid(rows, pal)


def serbim_ingot():
    """The vanilla ingot shape in deep serbim blue."""
    rows = list(INGOT)
    rows[4] = '....OOO45555w2..'
    pal = {'o': '#0c1638', 'O': '#16306a', '1': '#1f4a8a', '2': '#2a64aa', '3': '#3a86c8', '4': '#4aa6e0', '5': '#6ec4f0', 'w': '#c4ecff'}
    return grid(rows, pal)


def siftite_nugget():
    """A siftite nugget with a pink inclusion."""
    rows = [
        '................',
        '................',
        '................',
        '................',
        '................',
        '......4554......',
        '.....45ww543....',
        '.....4w5p432....',
        '.....455qP32....',
        '......443221....',
        '.......3321.....',
        '................',
        '................',
        '................',
        '................',
        '................',
    ]
    pal = ramp('12345w', S_RAMP[:6], S_OUT)
    pal.update({'p': (PINK[2], S_OUT), 'P': (PINK[1], S_OUT), 'q': (PINK[3], S_OUT)})
    return grid(rows, pal, ol=True)


def raw_serbim():
    """Raw ore in the vanilla raw-metal shape, with serbim crystal flecks."""
    rows = [
        '................',
        '..bbbbb.........',
        '.beggfebbb......',
        '.bghghgfgebbb...',
        'bfhhhghghggfeb..',
        'bfefhhggfgXfefeb',
        'afddfeddcxXedeb.',
        'aecdedddcbxcddb.',
        'aecceddcbbbbbca.',
        'adcccbXbbghfdda.',
        'addccbxXfffgedca',
        '.adcccaadffecbba',
        '..aaaa..adddcbba',
        '.........addbba.',
        '..........aaaa..',
        '................',
    ]
    pal = {'a': '#141a3c', 'b': '#22306a', 'c': '#34508e', 'd': '#4a6eb0', 'e': '#6a92cc', 'f': '#8cb2e0', 'g': '#b0d0f0',
           'h': '#dcecff', 'x': '#3fd0ff', 'X': '#c8f8ff'}
    return grid(rows, pal)


def chrome_pearl():
    """A glossy pearl of liquid chrome with pink and cyan reflections."""
    rows = [
        '................',
        '................',
        '................',
        '......4554......',
        '....45ww5443....',
        '....5ww55443....',
        '...45w5554432...',
        '...4555544332...',
        '...45544433c2...',
        '...3444333c21...',
        '....33322c1p....',
        '....c3221ppp....',
        '......1pp1......',
        '................',
        '................',
        '................',
    ]
    pal = ramp('12345', ['#4b3f9e', '#6a62cf', '#8f93ee', '#b4b6ff', '#dcd6ff'], '#221a5a')
    pal.update({'w': (WHITE, '#221a5a'), 'p': ('#ffc6ee', '#221a5a'), 'c': ('#a8f2ff', '#221a5a')})
    return grid(rows, pal, ol=True)


def glowing_slime_ball():
    """The vanilla slime ball, lime and glowing from its core."""
    rows = [
        '................',
        '................',
        '......bbbb......',
        '....bbeffeba....',
        '...befggfgeca...',
        '...bfihgggfea...',
        '..beghhfggffca..',
        '..beggfyyfffda..',
        '..befggyYfefda..',
        '..bdefeyffdcda..',
        '...bdeffedcca...',
        '...acddccccda...',
        '....aaddddaa....',
        '......aaaa......',
        '................',
        '................',
    ]
    pal = {'a': '#2c5a1e', 'b': '#4a8424', 'c': '#78b42c', 'd': '#94cc36', 'e': '#b2e048', 'f': '#cbee5e', 'g': '#e2f98a',
           'h': '#f6ffc8', 'i': WHITE, 'y': '#fbffb0', 'Y': WHITE}
    return grid(rows, pal)


def star_shard():
    """A golden shard of a fallen star, with sparkles."""
    rows = [
        '...w.....bbbbb..',
        '..wWw...bffffa..',
        '...w...bdffgea..',
        '......bfddgeca..',
        '.....befdebcca..',
        '....bdefecbca...',
        '...bddegbcba....',
        '..bcccfdbca.....',
        '..abcdcdba......',
        '..aaabcda.......',
        '..aabaca.....w..',
        '...aacb.....wWw.',
        '....bb.......w..',
        '................',
        '................',
        '................',
    ]
    pal = {'a': '#6a2a24', 'b': '#a5501e', 'c': '#d98a26', 'd': '#f2b53a', 'e': '#ffd85e', 'f': '#fff0a0', 'g': WHITE,
           'w': '#fff2a8', 'W': WHITE}
    return grid(rows, pal)


def echo_seed():
    """Three sculk seeds, each split by a glowing seam."""
    rows = [
        '................',
        '................',
        '...........43...',
        '..........4g3...',
        '..........3g2...',
        '..........21....',
        '....43..........',
        '...4g3..........',
        '...3g2..........',
        '...21....43.....',
        '........4w3.....',
        '........3g2.....',
        '........21......',
        '................',
        '................',
        '................',
    ]
    pal = ramp('1234', ['#0f3340', '#1b5a68', '#2a8090', '#3fa8b0'], '#06161e')
    pal.update({'g': (GLOW[2], '#06161e'), 'w': (GLOW[3], '#06161e')})
    return grid(rows, pal, ol=True)


def choir_pod():
    """A plump violet pod on a stem, seamed down one side."""
    rows = [
        '................',
        '..........LL....',
        '.......s.LLLl...',
        '.......sLLll....',
        '......4s43......',
        '.....45w243.....',
        '....45ww2432....',
        '....4w552322....',
        '....45542322....',
        '....44432221....',
        '....34331211....',
        '.....332111.....',
        '......2211......',
        '................',
        '................',
        '................',
    ]
    pal = ramp('12345w', ['#3e2378', '#5c3a9a', '#7d52c4', '#a46ee8', '#c79cf5', '#efe0ff'], '#20104a')
    pal.update(ramp('slL', ['#3a8a4a', '#3a8f4a', '#5cba58'], '#143424'))
    return grid(rows, pal, ol=True)


def pitcher_bulb():
    """A round blue pitcher-plant bulb with its mouth open on top."""
    rows = [
        '................',
        '..........LL....',
        '........sLLLl...',
        '........s.ll....',
        '......rrrr......',
        '.....rmmmmr.....',
        '....4rrrrrr3....',
        '...45w5555443...',
        '...4w55554433...',
        '...4555544332...',
        '...4455443322...',
        '....44433221....',
        '.....333221.....',
        '......2211......',
        '................',
        '................',
    ]
    pal = ramp('12345w', ['#245a8f', '#2f7fb0', '#3fa2cc', '#55c8de', '#8ee4ef', '#e2fbff'], '#132a5a')
    pal.update({'r': ('#b5f0f7', '#132a5a'), 'm': ('#1a3a5a', '#132a5a')})
    pal.update(ramp('sL', ['#3a8a4a', '#5cba58'], '#143424'))
    pal['l'] = ('#3a8f4a', '#143424')
    return grid(rows, pal, ol=True)


def warden_core():
    """A glowing sculk heart."""
    rows = [
        '................',
        '................',
        '...3443..3442...',
        '..345543344432..',
        '.34554G43G44322.',
        '.3455GgG3Gg3321.',
        '.3444GwgGgG3221.',
        '.33444GggG32211.',
        '..33444GG32211..',
        '...3344G43221...',
        '....33443221....',
        '.....334221.....',
        '......3221......',
        '.......21.......',
        '................',
        '................',
    ]
    pal = ramp('12345', SCULK[1:] + ['#3a8c94'], '#04101a')
    pal.update({'G': ('#22c7c4', '#04101a'), 'g': ('#3ff5e6', '#04101a'), 'w': ('#c8fffa', '#04101a')})
    return grid(rows, pal, ol=True)


def thick_hide():
    """A vanilla-leather-shaped hide in blue-grey, stitched down the middle."""
    rows = [
        '................',
        '......bbbb......',
        '..bb.bdfecb.bb..',
        '..bdbefffdebda..',
        '...beedsfeeda...',
        '...bdfdffdeca...',
        '....bfcsfeda....',
        '....bdeefeda....',
        '....beesfeea....',
        '....beedfeea....',
        '...bcdesededa...',
        '...beeeeeeeca...',
        '...bedddcdeda...',
        '..bcdaaccaadca..',
        '..baa..aa..aaa..',
        '................',
    ]
    pal = {'a': '#1e2a44', 'b': '#2f4562', 'c': '#4a6a88', 'd': '#5f86a3', 'e': '#7aa2bc', 'f': '#9cc0d4', 's': '#e6f2f8'}
    return grid(rows, pal)


# ============================================================================ food, tools of the trade, blocks

def sift_cake():
    # vanilla cake proportions: pink frosting, lavender dream-sponge, cyan and gold sprinkles
    rows = [
        '................',
        '....mmmmmmmm....',
        '..mmnoooooonmm..',
        '.mnppepggppppnm.',
        'mnpepoglkgpeppnm',
        'moppopmkgmpopkom',
        'mnppkonmmoopponm',
        'mnooppppppkponnm',
        'bnoooooononnnnmb',
        'bdnooooonnnnnmba',
        'bcfnooonnnonfcba',
        'bdiifnfifcncffba',
        '.bdjijijihhffca.',
        '..bbijjjijfcaa..',
        '....aaaaaaaa....',
        '................',
    ]
    pal = {'m': '#d77fb4', 'n': '#f2aad2', 'o': '#ffc8e6', 'p': '#ffe6f4',
           'e': '#2a8fb0', 'g': '#3fd0ef', 'k': '#ffd65a', 'l': '#ffffff',
           'a': '#2e1a44', 'b': '#4f3270', 'c': '#6a4890', 'd': '#7a58a0', 'f': '#8c6ab4', 'h': '#9a78c0', 'i': '#a886cc', 'j': '#b898d8'}
    return grid(rows, pal)


def dream_stew():
    """A vanilla stew bowl of violet dream stew with bright bits floating in it."""
    rows = [
        '................',
        '................',
        '................',
        '................',
        '................',
        '.....dddddd.....',
        '...ddhiyhhkdd...',
        '..dijjujjiwiha..',
        '..deehikiihuea..',
        '..dgfeeeeeefca..',
        '...bggggggfca...',
        '....aafggfaa....',
        '......aaaa......',
        '................',
        '................',
        '................',
    ]
    pal = {'a': '#22140e', 'b': '#3a2414', 'c': '#4a2e18', 'd': '#52341c', 'e': '#5e3c20', 'f': '#734a26', 'g': '#8f5e32',
           'h': '#7c52c8', 'i': '#9a72e2', 'j': '#bc9af2', 'y': '#ffe07a', 'k': '#ff8ac0', 'u': '#8ff0ff', 'w': '#ffffff'}
    return grid(rows, pal)


def glowcap_skewer():
    """Three glowcap mushrooms threaded on a stick through their stems: domed caps with pale
    spots over frilled gills."""
    rows = [
        '................',
        '...........554s.',
        '..........5w543.',
        '..........uUuUu.',
        '...........s....',
        '.......554s.....',
        '......5w543.....',
        '......uUuUu.....',
        '.......s........',
        '...554s.........',
        '..5w543.........',
        '..uUuUu.........',
        '...s............',
        '..s.............',
        '.S..............',
        '................',
    ]
    pal = ramp('345', ['#1f9a8a', '#33c8a8', '#5af0c8'], '#0c3a3a')
    pal.update({'w': ('#ffffff', '#0c3a3a'), 'u': ('#d8fff0', '#0c3a3a'), 'U': ('#8ad8c0', '#0c3a3a'),
                's': ('#c8a070', '#4a2c1c'), 'S': ('#9a7044', '#4a2c1c')})
    return grid(rows, pal, ol=True)


def bulb_lantern():
    """The vanilla lantern, glowing Bulb-blue."""
    rows = [
        '................',
        '........a.......',
        '.......ac.......',
        '.......ca.......',
        '.......a........',
        '......acca......',
        '......dbbd......',
        '.....acccca.....',
        '.....beffeb.....',
        '.....dfgifd.....',
        '.....dgihgd.....',
        '.....dghggd.....',
        '.....bfggfb.....',
        '.....acccca.....',
        '................',
        '................',
    ]
    pal = {'a': '#1c2240', 'c': '#3a4670', 'b': '#3a2f62', 'd': '#54468a',
           'e': '#2a7fc8', 'f': '#45b4ec', 'g': '#8ff0ff', 'h': '#d0fcff', 'i': '#ffffff'}
    return grid(rows, pal)


def chrome_bucket():
    # the vanilla bucket, brimming with liquid chrome
    rows = [
        '................',
        '.....aaaaaa.....',
        '...aaeeiiiiaa...',
        '..aebdgffddbea..',
        '..abgjjgfwfgba..',
        '..aaabfggfbaaa..',
        '..alkaaaaaacha..',
        '..allllkkhccha..',
        '..allmlkkhccka..',
        '..aklmlkkhccka..',
        '..aclmlkkhccha..',
        '...alllkkhcha...',
        '...akllkhhcha...',
        '....aklkhcha....',
        '.....aaaaaa.....',
        '................',
    ]
    pal = {'a': '#2c2c3a', 'c': '#6c6c80', 'e': '#74748a', 'h': '#9090a6', 'i': '#9494aa', 'k': '#a6a6ba', 'l': '#d6d6e4', 'm': '#ffffff',
           'b': CHROME[1], 'd': '#6a62cf', 'f': CHROME[2], 'g': CHROME[3], 'j': CHROME[4], 'w': CHROME[5]}
    return grid(rows, pal)


def dream_journal_fragment():
    """A torn page from a dream journal, with violet writing and a doodled star."""
    rows = [
        '................',
        '................',
        '....555555554...',
        '...55iiiiI5543..',
        '...5555555554...',
        '...5iiiIi5ii43..',
        '..55555555554...',
        '...5iiIii55543..',
        '...555555p5544..',
        '...5iiiipPp43...',
        '...4555555p543..',
        '...44555544433..',
        '....33..3333....',
        '.........33.....',
        '................',
        '................',
    ]
    pal = ramp('2345', ['#b89a6c', '#d6bc8c', '#ecd8b0', '#fbf0d6'], '#5a3c34')
    pal.update({'i': ('#6a54b0', '#5a3c34'), 'I': ('#9a86e0', '#5a3c34'), 'p': ('#f29bd6', '#5a3c34'), 'P': ('#ffffff', '#5a3c34')})
    return grid(rows, pal, ol=True)


def sift_codex():
    # the vanilla book, bound in midnight-blue leather with gold bands and a glowing sift eye
    rows = [
        '................',
        '........bbb.....',
        '......bbdfeb....',
        '....bbdfffGfb...',
        '..bbdfffGfrfGb..',
        'bbdefffGfrRrfeb.',
        'bdeffGffGfrfGfcb',
        'bbdfGffffGfGccg.',
        'bbidffffffGchih.',
        'adjideffcchiihbc',
        '.adjidcchiihbcaa',
        '..adjihiihbcaa..',
        '...adjihccaa....',
        '....adccaa......',
        '.....aaa........',
        '................',
    ]
    pal = {'a': '#0a0e2a', 'b': '#18204a', 'c': '#1e2a62', 'd': '#263678', 'e': '#2c4088', 'f': '#344ea0',
           'g': '#7a6a5a', 'h': '#c8b894', 'i': '#e6dcc0', 'j': '#fbf4e0', 'G': GOLD[3], 'r': GLOW[1], 'R': GLOW[3]}
    return grid(rows, pal)


def music_disc_lullaby():
    """A vanilla music disc: midnight vinyl with a pink label."""
    rows = [
        '................',
        '................',
        '................',
        '.....bbbbb......',
        '..bbbeeeeebbb...',
        '.beewefeffeeeb..',
        'beeffegggeefeeb.',
        'befeegdcdgeefeb.',
        'beefeegggeffeeb.',
        'bceeeffefeeeecb.',
        '.accceeeeeccca..',
        '..aaacccccaaa...',
        '.....aaaaa......',
        '................',
        '................',
        '................',
    ]
    pal = {'a': '#0c0a1a', 'b': '#1a1630', 'c': '#201c3a', 'e': '#322e58', 'f': '#4a467c', 'w': '#8a86c0',
           'g': '#f59ad0', 'd': '#9a3a78'}
    return grid(rows, pal)


def soul_chime():
    """A wind chime: a wooden bar hung with soul-glass tubes."""
    rows = [
        '................',
        '........k.......',
        '..BBBBBBBBBBBB..',
        '..bbbbbbbbbbbb..',
        '...k..k..k..k...',
        '...k..k..k..k...',
        '...Tt.Tt.Tt.Tt..',
        '...Tt.Tt.Tt.Tt..',
        '...Tt.Tt.Tt.Tt..',
        '...Tt.Tt.Tt.gg..',
        '...Tt.Tt.Tt.ee..',
        '...gg.Tt.Tt.....',
        '...ee.Tt.gg.....',
        '......gg.ee.....',
        '......ee........',
        '................',
    ]
    pal = {'B': (WOOD[4], WOOD[0]), 'b': (WOOD[2], WOOD[0]), 'k': '#c8b4a0',
           'T': ('#c8f4ff', '#163a5a'), 't': ('#6fb8e0', '#163a5a'), 'g': ('#3ff5e6', '#163a5a'), 'e': ('#2a7aa8', '#163a5a')}
    return grid(rows, pal, ol=True, no_ol='k')


def drift_petals():
    """Like vanilla pink petals: little four-petal blossoms on teal stems."""
    rows = [
        '................',
        '...........w....',
        '..........wyP...',
        '....w......P....',
        '...wyP.....s....',
        '....P......s....',
        '....s.....s.....',
        '.....s..w.......',
        '.......wyP......',
        '........P...w...',
        '........s..wyP..',
        '...w....s...P...',
        '..wyP...s...s...',
        '...P.........s..',
        '...s............',
        '................',
    ]
    o = '#8a2a5a'
    pal = {'w': ('#ffe0f0', o), 'P': ('#e98fc0', o), 'y': ('#ffe89a', o), 's': '#3f9d80'}
    return grid(rows, pal, ol=True, no_ol='s')


def glowbell_vine():
    """A teal vine hung with glowing yellow bells."""
    rows = [
        '.......v........',
        '.......v........',
        '........v.......',
        '........vLL.....',
        '.......v.LLl....',
        '...45..v........',
        '..4w54v.........',
        '..4554..........',
        '..3443..V.......',
        '...gg....V......',
        '.........v..45..',
        '........v..4w54.',
        '........v.V4554.',
        '.........V.3443.',
        '............gg..',
        '................',
    ]
    pal = ramp('345', ['#d8962a', '#ffd04a', '#ffe98a'], '#6a3a1a')
    pal.update({'w': ('#ffffff', '#6a3a1a'), 'g': ('#fff6c0', '#6a3a1a'), 'v': '#3fb5a0', 'V': '#2a8a7a',
                'L': ('#5ac8a0', '#143a30'), 'l': ('#3a9a80', '#143a30')})
    return grid(rows, pal, ol=True, no_ol='vV')


def door(wood, window, twig, out):
    """A vanilla-style door item: a twig-latticed window over planks, gold handle."""
    rows = [
        '................',
        '...OOOOOOOOOO...',
        '...OffffffefO...',
        '...OfkkkbkkdO...',
        '...OfkbkbkkdO...',
        '...OfkkbbkbdO...',
        '...OfkkkbbkdO...',
        '...OfkkkbkkdO...',
        '...OeeeeeeedO...',
        '...OggggggYdO...',
        '...OeeeeeeGdO...',
        '...OccccccccO...',
        '...OgggggggdO...',
        '...OeeeeeeedO...',
        '...OccccccccO...',
        '...OOOOOOOOOO...',
    ]
    c, d, e, g = wood
    pal = {'O': out, 'f': g, 'g': g, 'e': e, 'd': d, 'c': c, 'k': window, 'b': twig, 'Y': GOLD[4], 'G': GOLD[2]}
    return grid(rows, pal)


def siftite_upgrade_smithing_template():
    """The vanilla upgrade template on a slate plate, its rune in siftite."""
    rows = [
        '................',
        '....ccccccccc...',
        '...cdeeeeeedec..',
        '...ceeededddda..',
        '...addddbcddda..',
        '...addcbfbddca..',
        '...adcbfhgbcba..',
        '...acbfhpggbca..',
        '...adeegggeeda..',
        '...aedefgfedda..',
        '...adddfffdeca..',
        '...acddeeeddba..',
        '...abcddccccba..',
        '....abbccbaaa...',
        '.....aaaaa......',
        '................',
    ]
    pal = {'a': '#141a30', 'b': '#20283f', 'c': '#2a3450', 'd': '#36425e', 'e': '#46526e',
           'f': S_RAMP[1], 'g': S_RAMP[3], 'h': S_RAMP[5], 'p': PINK[2]}
    return grid(rows, pal)


def slingshot(pull):
    """A forked branch with siftite-capped tips; the band pulls back a glowing slime ball."""
    rows = [
        '................',
        '................',
        '.........c......',
        '........A.......',
        '........A.......',
        '.......A........',
        '.......A........',
        '.......A......c.',
        '.......JJ...aa..',
        '.......hJaaa....',
        '......H.........',
        '.....h..........',
        '....H...........',
        '...g............',
        '..g.............',
        '................',
    ]
    pal = {'a': (WOOD[2], WOOD[0]), 'A': (WOOD[4], WOOD[0]), 'J': (WOOD[3], WOOD[0]), 'h': (WOOD[2], WOOD[0]),
           'H': (WOOD[3], WOOD[0]), 'g': ('#5e2a56', '#24101c'), 'c': (S_RAMP[3], S_OUT)}
    img = grid(rows, pal, ol=True)
    band = {-1: '10,3 11,4 12,5 13,6', 0: '9,3 10,4 13,7 12,6', 1: '9,3 9,4 9,5 13,7 12,7 11,7',
            2: '9,3 9,4 8,5 8,6 13,7 12,7 11,8 10,8'}[pull]
    px = img.load()
    for tok in band.split():
        x, y = map(int, tok.split(','))
        px[x, y] = rgba('#f0d0b0')
    ball = {0: (10, 5), 1: (9, 6), 2: (8, 7)}.get(pull)
    if ball:
        x, y = ball
        for (dx, dy), c in (((0, 0), '#f2ffb0'), ((1, 0), '#cbee5e'), ((0, 1), '#94cc36'), ((1, 1), '#5a9a2a')):
            px[x + dx, y + dy] = rgba(c)
    return img


def conductors_baton():
    """The Conductor's dark baton: an ebony shaft, plum grip, gold ferrule and a glowing tip."""
    rows = [
        '................',
        '................',
        '.............g..',
        '............S...',
        '...........s....',
        '..........S.....',
        '.........s......',
        '........S.......',
        '.......s........',
        '......S.........',
        '.....G..........',
        '...hG...........',
        '..hhH...........',
        '..hH............',
        '................',
        '................',
    ]
    pal = {'s': ('#3a3456', '#110d20'), 'S': ('#5a5280', '#110d20'), 'G': (GOLD[3], '#3a1c12'),
           'h': ('#3a1a3a', '#120812'), 'H': ('#5e2a56', '#120812'), 'g': (GLOW[2], '#0a3a40'), 'w': GLOW[4]}
    return grid(rows, pal, ol=True, no_ol='w')


def conductors_staff():
    """The Conductor's Staff: a sculk-dark shaft bound in gold, a human skull with glowing cyan
    eyes, and two bone prongs rising from its crown to cradle a glowing orb."""
    rows = [
        '................',
        '........bcgcb...',
        '.......b.gwg.b..',
        '.......B.cgc.B..',
        '........b...b...',
        '........45554...',
        '.......4555553..',
        '.......4vg5gv3..',
        '.......4vv5vv3..',
        '........44n43...',
        '........t3t3t...',
        '.......G.333....',
        '......k.........',
        '.....K..........',
        '....G...........',
        '...k............',
    ]
    bone = '#3a2e34'
    pal = {'b': (BONE[3], bone), 'B': (BONE[2], bone), '5': (BONE[4], bone), '4': (BONE[3], bone), '3': (BONE[2], bone),
           'n': ('#5e5048', bone), 't': (WHITE, bone), 'v': ('#141020', bone), 'g': (GLOW[2], '#0a3038'), 'c': (GLOW[1], '#0a3038'),
           'w': (GLOW[4], '#0a3038'), 'G': (GOLD[3], GOLD[0]), 'k': (SCULK[3], SCULK[0]), 'K': (SCULK[4], SCULK[0])}
    return grid(rows, pal, ol=True)


# ============================================================================ the new orchestra's spoils

def conga_drum():
    """A tall tapered conga: tan skin, metal hoops, rope lacing and a turtle-shell body."""
    rows = [
        '................',
        '....tTTTTTtt....',
        '...tTTTTTTttt...',
        '....sttttsss....',
        '...RRRRRRRRrr...',
        '...lGGglgghlh...',
        '...glGlglglhl...',
        '...gGlggglhhh...',
        '...RRRRRRRRrr...',
        '....GGbggbhh....',
        '....GGbggbhh....',
        '.....bbbbbb.....',
        '.....Gbggbh.....',
        '.....Gbggbh.....',
        '.....RRRRrr.....',
        '................',
    ]
    pal = {'t': '#e8c890', 'T': '#f8e2b8', 's': '#c8a46a', 'R': '#d8dae6', 'r': '#8a90a8', 'l': '#f6f0dc',
           'G': '#8fbf5a', 'g': '#5e9a40', 'h': '#3e6a32', 'b': '#7a4e2a'}
    pal = {k: (v, '#1e2416') for k, v in pal.items()}
    return grid(rows, pal, ol=True)



def crane_flute():
    """A bone carved into a flute: soul-glow in its holes, an amethyst reed, white feathers tied at the foot."""
    rows = [
        '................',
        '.............Mo.',
        '............LMD.',
        '...........LMD..',
        '..........gMD...',
        '.........LMD....',
        '........gMD.....',
        '.......LMD......',
        '......gMD.......',
        '.....LMD........',
        '....RrD.........',
        '...RrrD.........',
        '.Ww..Ww.........',
        '.Ww..Wk.........',
        '.Wk..k..........',
        '.k..............',
    ]
    pal = ramp('LMD', ['#fff4d6', '#ecd29a', '#c49a5a'], '#5a3a22')
    pal.update({'g': (GLOW[2], '#5a3a22'), 'o': ('#2a1a12', '#5a3a22'), 'r': ('#c83a4a', '#3a1018'), 'R': ('#f06a6a', '#3a1018'),
                'W': ('#ffffff', '#3a3a4a'), 'w': ('#c8ccd8', '#3a3a4a'), 'k': ('#2a2a36', '#101018')})
    return grid(rows, pal, ol=True)


def magic_strings():
    """Two glowing strands, violet and cyan, wound into loose interlocking loops."""
    rows = [
        '................',
        '..v.............',
        '...vv...........',
        '.....vvwv.......',
        '...vv....vv.....',
        '..v........v....',
        '..v.....cccc....',
        '..v...cc...vcc..',
        '...vvc...vv...c.',
        '.....vvvv.....c.',
        '.....c........c.',
        '......cc....cc..',
        '........ccwc....',
        '............cc..',
        '..............c.',
        '................',
    ]
    pal = {'v': '#c08af8', 'c': '#8ff0ff', 'w': '#ffffff'}
    img = grid(rows, pal)
    halo = grid(rows, {'v': '#8a4ae070', 'c': '#22c7c470', 'w': '#ffffff70'})
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    hp, op = halo.load(), out.load()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < 16 and 0 <= y + dy < 16 and hp[x + dx, y + dy][3]:
                    op[x, y] = hp[x + dx, y + dy]
                    break
    out.alpha_composite(img)
    return out


def guitar():
    """An acoustic guitar held like a tool, its magical strings glowing down the neck."""
    rows = [
        '................',
        '............phh.',
        '.............hhp',
        '............gN..',
        '...........gN...',
        '.......ab.gN....',
        '......aabgN.....',
        '.....aabgNc.....',
        '....aabgbbc.....',
        '..aaaogbbc......',
        '.aabogoc........',
        '.aBbgobc........',
        '.bbBbbcc........',
        '..bbBcc.........',
        '...ccc..........',
        '................',
    ]
    pal = {'a': ('#f4c070', '#4a2414'), 'b': ('#d8963e', '#4a2414'), 'c': ('#a8682a', '#4a2414'),
           'o': ('#3a1e14', '#4a2414'), 'B': ('#5a3420', '#4a2414'), 'N': ('#6a3e26', '#24120c'), 'h': ('#4a2a1a', '#24120c'),
           'p': ('#e0e4ee', '#3a3a4a'), 'g': ('#b8fff8', '#24120c')}
    return grid(rows, pal, ol=True)


def stomper_meat():
    """Raw Stomper: vanilla beef proportions, marbled pink flesh under a chrome-blue rind."""
    rows = [
        '................',
        '................',
        '................',
        '.........rrrr...',
        '........rRRRRr..',
        '.......rRhfdgRc.',
        '......rRfmedfdb.',
        '....rrRmeefdmhca',
        '...rRRmedfdehdba',
        '...rRfdgmefdbaa.',
        '..rRhdeefgdbba..',
        '..rRmefggdbba...',
        '..aRhhgdccba....',
        '...accccbaa.....',
        '....aaaaa.......',
        '................',
    ]
    pal = {'a': '#3a0a20', 'b': '#7a1a32', 'c': '#a82a40', 'd': '#d84a5a', 'e': '#e6606a', 'f': '#ee7a80', 'g': '#f4928e',
           'h': '#f8a8a0', 'm': '#ffd6dc', 'r': '#5a6ad0', 'R': '#a8b8ff'}
    return grid(rows, pal)


def stomper_steak():
    """The cooked cut: seared brown with grill marks, the rind toasted to bronze-blue."""
    rows = [
        '................',
        '................',
        '................',
        '.........rrrr...',
        '........rRRRRr..',
        '.......rRgfhgRc.',
        '......rRhkdgkdb.',
        '....rrRdikdgfda.',
        '...rRRghdkhfeba.',
        '...rRfdkhdiefa..',
        '..rRgkdihfdbba..',
        '..rRigkdfgeba...',
        '...rihhgebba....',
        '...aadbbbaa.....',
        '.....aaaa.......',
        '................',
    ]
    pal = {'a': '#22100c', 'b': '#3f2116', 'c': '#4e2719', 'd': '#5a3020', 'e': '#6e3a26', 'f': '#7c4632', 'g': '#8e543a',
           'h': '#a06644', 'i': '#b47a52', 'k': '#2e140e', 'r': '#3a3e7a', 'R': '#7a80c0'}
    return grid(rows, pal)


def stomper_egg():
    """A big Stomper egg, pale grey-blue with chrome speckles."""
    tones = ['#7a8aa8', '#a0b0c8', '#c4d2e2', '#e2ecf6']
    return egg(tones, '#2e3450', {
        4: '........Cc......',
        6: '....Cc..........',
        7: '....cc.....C....',
        8: '...........c....',
        9: '.......Cc.......',
        10: '.......cc.......',
        12: '.....c..........',
    }, {'C': CHROME[3], 'c': CHROME[1]})


def kazoo_fish(cooked=False):
    """Laid like vanilla cod: a teal fish with orange fins, stripes and a brass kazoo snout."""
    rows = [
        '................',
        '...........oO...',
        '...........ooO..',
        '..........otooO.',
        '.....OO..tTTtoo.',
        '....OTTtTtTttbb.',
        '....tTsTsTsb....',
        '....ttTsTstb....',
        '...tTTsTTstb....',
        '...ttTTsTtb.....',
        '..tTTtTsttb.....',
        '..tewtsttb......',
        '..tTTTttb.......',
        '.kbttttbb.......',
        'kKkbbb..........',
        '.k..............',
    ]
    if cooked:
        pal = {'o': '#a86a2a', 'O': '#d89a4a', 't': '#8a6a48', 'T': '#b08e62', 's': '#6a4a2e', 'b': '#4a3020',
               'e': '#1a1210', 'w': '#e8dcc8', 'k': '#8a6a3a', 'K': '#c8a060'}
    else:
        pal = {'o': '#f07a2a', 'O': '#ffb05a', 't': '#2a9a98', 'T': '#5ad0c8', 's': '#f08a3a', 'b': '#155a62',
               'e': '#0e1418', 'w': '#ffffff', 'k': '#c8962e', 'K': '#ffe08a'}
    return grid(rows, pal, ol=True)


def tuba_bubble():
    """A shimmering bubble blown from a tuba: a brass-tinted film, a window highlight and a
    rainbow sheen sliding round its lower rim."""
    rows = [
        '................',
        '................',
        '......OOOO......',
        '....OOiiiioo....',
        '...Oiwwiiiiio...',
        '..Oiwiiiiiiiio..',
        '..Owiiiiiiiiio..',
        '.OiiiiiiiiiiiiO.',
        '.Oiiiiiiiiiiipo.',
        '.Oiiiiiiiiiiipo.',
        '..oiiiiiiiiiyo..',
        '..oiiiiiiiiyco..',
        '...oiiiiivcco...',
        '....ooiiiioo....',
        '......oooo......',
        '................',
    ]
    pal = {'O': '#f6dc80', 'o': '#c8902e', 'i': '#ffe8a030', 'w': '#ffffff', 'p': '#ff9ad6', 'c': '#8ff0ff', 'y': '#fff07a',
           'v': '#c8a0ff'}
    return grid(rows, pal)


def bubble_gun():
    """A toy brass bubble blaster: belled barrel, red grip, a glass bubble tank on top."""
    rows = [
        '................',
        '................',
        '.....CCw........',
        '....CwCCc.......',
        '....CCbcd.......',
        '.....ccd........',
        '......G.......b.',
        '..vGGGGGGGGGvGG.',
        '..GgggGgggggGgm.',
        '..ddddddddddGgm.',
        '....rRtd.....dd.',
        '....rRt.........',
        '...rRr..........',
        '...rr...........',
        '................',
        '................',
    ]
    pal = {'C': ('#bff4ff', '#1a3a5a'), 'c': ('#7fd0e8', '#1a3a5a'), 'd': ('#a8702a', '#3a1e10'), 'w': ('#ffffff', '#1a3a5a'),
           'b': ('#ffffff', '#3a1e10'), 'G': ('#ffd86a', '#3a1e10'), 'g': ('#e0a83a', '#3a1e10'), 'v': ('#fff4c0', '#3a1e10'),
           'm': ('#5a2e14', '#3a1e10'), 'r': ('#d04a5a', '#3a1020'), 'R': ('#f07a84', '#3a1020'), 't': ('#3a2a2a', '#1a1010')}
    return grid(rows, pal, ol=True)


def skysong_gem():
    """A faceted sky-blue gem with a golden core, sparkling."""
    rows = [
        '..S.............',
        '.SWS............',
        '..S.............',
        '.....5ww554.....',
        '....55w55443....',
        '...4555YY4433...',
        '...4455Yy4332...',
        '....4445y332....',
        '.....445332.....',
        '......4532......',
        '.......42.......',
        '.............S..',
        '............SWS.',
        '.............S..',
        '................',
        '................',
    ]
    pal = ramp('2345', ['#2a6ad0', '#3f9ae8', '#6ec8f8', '#b8ecff'], '#142a6a')
    pal.update({'w': ('#ffffff', '#142a6a'), 'Y': ('#ffe27a', '#142a6a'), 'y': ('#ffb03a', '#142a6a'), 'S': '#fff6c8', 'W': '#ffffff'})
    return grid(rows, pal, ol=True, no_ol='SW')


# ============================================================================ spawn eggs
# The vanilla 26.x egg: a shaded egg in the mob's colours, wearing its most recognisable features
# (ears, horns, legs, a drum...). The base egg is shaded with tones 1-4 (dark to light) and a glint
# 'w'; each mob paints its features over it (and, for things behind the egg, under it).

EGG = [
    '................',
    '................',
    '......4433......',
    '.....444433.....',
    '....44w44333....',
    '....4w443332....',
    '...4444333332...',
    '...4443333322...',
    '...4433333322...',
    '...4333333222...',
    '...3333332221...',
    '...3333322221...',
    '....33322221....',
    '.....222211.....',
    '......1111......',
    '................',
]

# a ring of water (or cloud) around the bottom of the egg, as on vanilla fish eggs
RING_UNDER = {12: '..VV........VV..', 13: '..VXV......VXV..'}
RING_OVER = {14: '...ZVXVVVVXVZ...', 15: '....ZZZZZZZZ....'}
WATER = {'V': ('#3e8ac0', '#18305a'), 'X': ('#7ad8ff', '#18305a'), 'Z': ('#2a5a9a', '#18305a')}
CLOUD = {'V': ('#eef2ff', '#5a6290'), 'X': ('#ffffff', '#5a6290'), 'Z': ('#c4cbe8', '#5a6290')}


def egg(tones, ol, over=None, pal=None, under=None, no_ol='', ring=None):
    over, under = dict(over or {}), dict(under or {})
    if ring:
        for y, r in RING_UNDER.items():
            under.setdefault(y, r)
        for y, r in RING_OVER.items():
            over.setdefault(y, r)
    rows = []
    for y in range(16):
        r = list(EGG[y])
        for x, ch in enumerate(under.get(y, '.' * 16)):
            if ch != '.' and r[x] == '.':
                r[x] = ch
        for x, ch in enumerate(over.get(y, '.' * 16)):
            if ch != '.':
                r[x] = ch
        rows.append(''.join(r))
    p = ramp('1234', tones, ol)
    p['w'] = (mix(tones[3], WHITE, 0.6), ol)
    for k, v in (pal or {}).items():
        p[k] = v if isinstance(v, tuple) or k in no_ol else (v, ol)
    if ring:
        for k, v in (WATER if ring == 'water' else CLOUD).items():
            p.setdefault(k, v)
    return grid(rows, p, ol=True, no_ol=no_ol)


def spawn_eggs():
    E = {}
    # ---- the Sift's own creatures
    E['bulb'] = egg(['#3f5fae', '#5a80cf', '#78a5e3', '#a8c8f4'], '#1e2a6a', {
        0: '.....44..44.....',
        1: '.....43..43.....',
        2: '.....43..43.....',
        7: '.....hE..hE.....',
        8: '.....EE..EE.....',
        9: '....c......c....',
        10: '.......mm.......',
        14: '....ff....ff....',
    }, {'E': '#2f2777', 'h': '#ffffff', 'm': '#4a3a9f', 'c': '#f2a6d8', 'f': '#4a6ac0'})
    E['slumbler'] = egg(['#4a8aa8', '#6aaec4', '#8fd0dc', '#b4e4ec'], '#1e4a62', {
        5: '....iii..iii....',
        6: '..g.iEi..iEi.g..',
        7: '.gG.iii..iii.Gg.',
        8: '..g..........g..',
        9: '.gG.tMtMMtMt.Gg.',
        10: '....MmmmmmmM....',
        11: '.....MmmmmM.....',
    }, {'i': '#ffd66b', 'E': '#1a1d38', 'g': ('#f59ad0', '#6a2050'), 'G': ('#c85f9f', '#6a2050'), 't': '#fff8ec', 'M': '#9c2f55',
        'm': '#d9577f'})
    E['sifter'] = egg(['#0e5a7d', '#157e9f', '#1fa3c1', '#3ec0d6'], '#08304a', {
        4: '.....ee..ee.....',
        5: '.....eE..Ee.....',
        7: '...tvtvtvtvtv...',
        8: '...vvvvvvvvvv...',
        9: '..fvvvvvvvvvvf..',
        10: '..fvtvtvtvtvtf..',
        11: '...TTTTTTTTTU...',
        12: '....TTTTTTUU....',
        13: '.....TTTUUU.....',
    }, {'e': '#d9f6ff', 'E': '#0c1e5c', 't': '#eaf7ff', 'v': '#17328c', 'T': ('#f2cd98', '#5a3a1a'), 'U': ('#d7a46c', '#5a3a1a'),
        'f': '#0f6a8e'})
    E['enchoer'] = egg(['#5a9a8a', '#7fbcab', '#a3dcc5', '#c3ecd8'], '#24504a', {
        0: '.a.a........a.a.',
        1: '.laaa......aaAa.',
        2: '..laaa....aaAa..',
        5: '.....ffffff.....',
        6: '....ffbffbff....',
        7: '....fbeffebF....',
        8: '....fffnnffF....',
        9: '....fffmmfFF....',
        10: '.....ffffFF.....',
        11: '..pP........Pp..',
        12: '..pp........pp..',
        14: '.....pp..pp.....',
    }, {'a': ('#efe2b2', '#5a4a2a'), 'A': ('#c9b784', '#5a4a2a'), 'l': ('#fbf3d2', '#5a4a2a'), 'f': '#e2eadf', 'F': '#c0cdbf',
        'b': '#4d6870', 'e': '#2a3c44', 'n': '#aebdb4', 'm': '#6a807b', 'p': ('#34507a', '#141e36'), 'P': ('#466a9c', '#141e36')})
    E['riveter'] = egg(['#121b30', '#1d2b47', '#2c4066', '#3d5684'], '#070a14', {
        0: '....3......3....',
        1: '....3t....t3....',
        2: '....33....33....',
        5: '....BBBBBBBD....',
        6: '.....ee..ee.....',
        7: '......e..e......',
        8: '..2..........2..',
        9: '..2..tvvvvt..2..',
        10: '..2...vvvv...2..',
        11: '..2..........2..',
        12: '..c..........c..',
    }, {'t': '#1fa39b', 'B': '#d9d4bf', 'D': '#a9a28c', 'e': '#a6fff5', 'v': '#07101c', 'c': '#4fd5c6'})
    E['harmoner'] = egg(['#9a2a52', '#c43e66', '#e8577f', '#f88aa8'], '#4a1028', {
        0: '.......yy.......',
        1: '......yooy......',
        2: '......oooo......',
        5: '....hE....Eh....',
        6: '....EE....EE....',
        7: '......llll......',
        8: '......bbbb......',
        9: '......BBBB......',
        10: '.....cccccc.....',
        11: '.....cccccC.....',
        12: '......ccCC......',
    }, {'y': ('#ffe25a', '#6a2a10'), 'o': ('#ff8a3d', '#6a2a10'), 'E': '#1a1830', 'h': '#ffffff', 'l': '#ffe98a', 'b': '#ffd23f',
        'B': '#c99a1f', 'c': '#ffc4d4', 'C': '#f0a0b8'})
    E['dictator'] = egg(['#200912', '#2e0e1a', '#3a1322', '#55203a'], '#0c0408', {
        0: '...l........l...',
        1: '..hh........hh..',
        2: '...hH......Hh...',
        3: '....hH....Hh....',
        5: '..4.bb....bb.4..',
        6: '..4..vE..Ev..4..',
        7: '.....vv..vv.....',
        9: '....m......m....',
        10: '....mfmmmmfm....',
        11: '.....mmmmmm.....',
    }, {'l': ('#f4f0e5', '#3a3428'), 'h': ('#e3ddcc', '#3a3428'), 'H': ('#9e957c', '#3a3428'), 'b': '#0c0408', 'v': '#04080c',
        'E': '#2ef2e2', 'm': '#04080c', 'f': '#f4f0e5'})
    # ---- the new orchestra
    E['thumper'] = egg(['#1e5a2a', '#2e7a3a', '#3fa442', '#6ac85a'], '#0e2a16', {
        0: '.....ssssss.....',
        1: '....GGGGGGGG....',
        2: '....rlrrlrrR....',
        3: '....rrlrrlrR....',
        4: '....GGGGGGGg....',
        7: '....hE....Eh....',
        9: '.....m....m.....',
        10: '..f...mmmm...f..',
        11: '.ff..........ff.',
        12: '.f..cccccc....f.',
        13: '.....cccc.......',
    }, {'s': ('#f2e2c0', '#5a3a2a'), 'G': ('#f0c040', '#5a3410'), 'g': ('#b88a2a', '#5a3410'), 'r': ('#d03a3a', '#4a1018'),
        'R': ('#902030', '#4a1018'), 'l': ('#fff4e0', '#4a1018'), 'E': '#0e1a10', 'h': '#ffffff', 'm': '#0e2a16',
        'f': '#4a9a3a', 'c': '#d8d08a'})
    E['stomper'] = egg(['#3a4a6a', '#566a8c', '#7890b0', '#a0b4cc'], '#1a2238', {
        4: '.....y....y.....',
        5: '..ff........ff..',
        6: '..fyE......Eyf..',
        7: '..f..........f..',
        8: '.......Tt.......',
        9: '.......Tt.......',
        10: '......kTtk......',
        11: '.....k.Tr.k.....',
        12: '.....k.Tt.k.....',
        13: '........Tt......',
    }, {'y': '#ffd66b', 'E': '#141828', 'f': '#566a8c', 'T': '#4a5a7c', 't': '#2a3654', 'r': '#1e2842', 'k': '#f4f0e5'})
    E['fanfare_eel'] = egg(['#8a3a10', '#c05a18', '#e88a2a', '#ffb85a'], '#3a1608', {
        0: '.......f........',
        1: '......ff........',
        5: '....b......b....',
        6: '.....bE..Eb.....',
        7: '.....Ep..pE.....',
        8: '..f..........f..',
        9: '.ff..tmmmmt..ff.',
        10: '.....mmmmmm.....',
        11: '......tmmt......',
    }, {'f': ('#a8301a', '#3a0c08'), 'b': '#2a0c04', 'E': '#fff07a', 'p': '#2a0c04', 't': '#fff8ec', 'm': '#3a0c10'}, ring='water')
    E['kazoo_fish'] = egg(['#155a62', '#2a8a8a', '#3ab0a8', '#6ad8cc'], '#08282c', {
        0: '......O..O......',
        1: '......OOOO......',
        6: '....h......h....',
        7: '..O.E......E.O..',
        8: '.OOo........oOO.',
        9: '..O.o......o.O..',
        10: '.....o.KK.o.....',
        11: '......okko......',
        12: '.......mm.......',
    }, {'O': ('#ffb05a', '#5a2a08'), 'o': '#f07a2a', 'E': '#0e1418', 'h': '#ffffff', 'K': ('#ffe08a', '#4a3008'),
        'k': ('#c8962e', '#4a3008'), 'm': '#2a1a08'}, ring='water')
    E['tubafish'] = egg(['#8a5a12', '#c08a20', '#e8b030', '#ffd86a'], '#3a2408', {
        0: '.......S........',
        1: '.......s........',
        2: '...S............',
        3: '....s......s....',
        5: '....ww....ww....',
        6: '....wE....Ew....',
        8: '.Ss..........sS.',
        9: '......LmmL......',
        10: '......LmmL......',
        12: '...s........s...',
    }, {'S': '#fff4c0', 's': '#d8a838', 'w': '#ffffff', 'E': '#1a1008', 'L': '#ffe8a0', 'm': '#3a2008'}, no_ol='Ss', ring='water')
    E['sky_whale'] = egg(['#3a6ab0', '#5a94d8', '#82bcf0', '#b4dcff'], '#1a2a5a', {
        1: '.....h....h.....',
        2: '.....H....H.....',
        3: '......ffff......',
        4: '.....ff44ff.....',
        8: '....EE....EE....',
        9: '...p........p...',
        10: '......mmmm......',
        11: '.......mm.......',
    }, {'h': ('#fff4d6', '#5a4a2a'), 'H': ('#d8c89a', '#5a4a2a'), 'f': '#eef6ff', 'E': '#14204a', 'p': '#f2a6d8', 'm': '#14204a'},
        ring='cloud')
    # a black-and-teal centipede curled round a glowing sculk egg, its bone mandibles over the top
    E['sculk_parasite'] = egg(['#034150', '#05625d', '#0b7c78', '#1a9a94'], '#020a10', {
        0: '..........m..m..',
        1: '..........mhhm..',
        2: '...........ehe..',
        3: '............KK..',
        4: '......c...c.kkl.',
        5: '....k.......KK..',
        6: '..lKK....c..kkl.',
        7: '..kk........KK..',
        8: '.lKK........kkl.',
        9: '..kk..c.....KK..',
        10: '.lKK....c...kkl.',
        11: '...kk......KK...',
        12: '....KK....kk....',
        13: '...l..kkKK..l...',
        14: '......l..l......',
    }, {'m': '#d1d6b6', 'e': '#29dfeb', 'h': '#0d1217', 'K': '#2aa8ae', 'k': '#0d1217', 'l': '#0d1217', 'c': '#3ff5e6'})
    return {f'{k}_spawn_egg': v for k, v in E.items()}


# ============================================================================ mob effect + block sprites

def sculk_corruption_icon():
    """18x18 like every mob effect icon: a glowing sculk eye held in the curl of two tentacles,
    their tips hooked over it and glowing suckers down their insides."""
    rows = [
        '..................',
        '....tTT....TTt....',
        '...tT..T..T..Tt...',
        '..tT...t..t...Tt..',
        '..tT..........Tt..',
        '.tT...kkkkkk...Tt.',
        '.tT..kgGGGGgk..Tt.',
        '.ts.kgGWKKGGgk.st.',
        '.tT.kgGGKKGGgk.Tt.',
        '.tT..kgGGGGgk..Tt.',
        '.ts...kkkkkk...st.',
        '.tT............Tt.',
        '..tT..........Tt..',
        '..ts..........st..',
        '...tT........Tt...',
        '....tT......Tt....',
        '.....tt....tt.....',
        '..................',
    ]
    o = '#020a10'
    pal = {'t': ('#16222a', o), 'T': ('#05625d', o), 's': ('#29dfeb', o), 'k': ('#06141c', o), 'g': ('#0f8c99', o),
           'G': ('#3ff5e6', o), 'K': ('#041016', o), 'W': ('#e8fffc', o)}
    return grid(rows, pal, ol=True, size=(18, 18))


def hummingbloom():
    """A cross-model flower: a lavender trumpet with a golden throat on a stem with two leaves."""
    rows = [
        '................',
        '.....bccccb.....',
        '...bcdeeeedcb...',
        '..bcdyYYYYydcb..',
        '..abcdyooydcba..',
        '...abccddccba...',
        '.....bccdcb.....',
        '......bcdb......',
        '.......bc.......',
        '.......mM.......',
        '..lLL..s........',
        '...lLLss........',
        '.......s.LLl....',
        '.......sLLl.....',
        '.......s........',
        '.......s........',
    ]
    pal = {'a': '#5a3a9a', 'b': '#8a62c8', 'c': '#b08ae6', 'd': '#d4b8f8', 'e': '#f0e4ff', 'y': '#ffd65a', 'Y': '#fff2a8',
           'o': '#d89a2a', 'm': '#3f9d80', 'M': '#78d4a8', 's': '#3f9d80', 'l': '#3f9d80', 'L': '#78d4a8'}
    return grid(rows, pal)


# ============================================================================ registry

def all_items():
    out = {}
    out.update(tools())
    for f in (siftite_ingot, serbim_ingot, siftite_nugget, raw_serbim, chrome_pearl, glowing_slime_ball, star_shard, echo_seed,
              choir_pod, pitcher_bulb, warden_core, thick_hide):
        out[f.__name__] = f()
    for f in (sift_cake, dream_stew, glowcap_skewer, bulb_lantern, chrome_bucket, dream_journal_fragment, sift_codex,
              music_disc_lullaby, soul_chime, drift_petals, glowbell_vine, siftite_upgrade_smithing_template, conductors_baton):
        out[f.__name__] = f()
    out['lullwood_door'] = door(('#9f97c6', '#b1a9d4', '#c2bbe0', '#d3cdea'), '#24353e', '#4a6470', '#2a2450')
    out['wishwood_door'] = door(('#c9738f', '#d98aa4', '#e6a0b8', '#f0b6ca'), '#4a1f38', '#8a4a6a', '#4a1f38')
    for f in (conductors_staff, conga_drum, crane_flute, magic_strings, guitar, stomper_meat, stomper_steak, stomper_egg,
              tuba_bubble, bubble_gun, skysong_gem):
        out[f.__name__] = f()
    out['kazoo_fish'] = kazoo_fish()
    out['cooked_kazoo_fish'] = kazoo_fish(cooked=True)
    out.update(spawn_eggs())
    out.update(__import__('songs').art())  # songs & instruments (agent D)
    import plants_h_art  # H: Pitcher Nectar and the three Pitcher soups
    out.update(plants_h_art.item_sprites())
    import siege  # the Thumper's arena: the cannonball
    out.update(siege.items())
    out.update(__import__('caravans').items())  # C: prism gem and armour, Caravan egg
    out['slingshot'] = slingshot(-1)
    for i in range(3):
        out[f'slingshot_pulling_{i}'] = slingshot(i)
    return out


def effect_icons():
    return {'sculk_corruption': sculk_corruption_icon()}


def flower_textures():
    return {'hummingbloom': hummingbloom()}
