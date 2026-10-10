"""F3 Knowledge & lore: every texture of the Knowledge Book and Lore screens, the placed Lore Books and the Music Sheet art.

  textures/gui/knowledge_book.png 256x256 and textures/gui/lore_pages.png 1024x512: vanilla's book-screen layout (see
  "the book screens" below; client/codex/KnowledgeBookScreen.java, client/knowledge/LoreScreen.java)
  textures/gui/sheets/<song>.png 176x112: each song's sheet - staff, notes and ink diagrams of its instrument and creature
  block/<origin>_lore_book_{cover,side,trim,pages}: the placed Lore Book (an open book, like the one on a
  lectern); item/mini_creator_spawn_egg. The book and scroll item sprites are drawn in tools/itemart.py.

Pixel art in the vanilla manner: flat base tones with clustered noise, a lit top-left, no outlines on the GUI art.
"""
import math
import os
import random

from PIL import Image

ORIGINS = ['creator', 'pillager', 'cultist', 'ocean', 'soul']


def hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (a[3] if len(a) > 3 else 255,)


def lighten(c, f):
    return mix(c, (255, 255, 255, c[3]), f)


def darken(c, f):
    # shadows lean a touch towards violet, like Mojang's
    return (int(c[0] * (1 - f) + 18 * f), int(c[1] * (1 - f) + 10 * f), int(c[2] * (1 - f) + 30 * f), c[3])


def value_noise(w, h, cell, seed):
    rnd = random.Random(seed)
    gw, gh = w // cell + 2, h // cell + 2
    lat = [[rnd.random() for _ in range(gw)] for _ in range(gh)]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        fy = y / cell
        y0 = int(fy)
        ty = fy - y0
        ty = ty * ty * (3 - 2 * ty)
        for x in range(w):
            fx = x / cell
            x0 = int(fx)
            tx = fx - x0
            tx = tx * tx * (3 - 2 * tx)
            a = lat[y0][x0] * (1 - tx) + lat[y0][x0 + 1] * tx
            b = lat[y0 + 1][x0] * (1 - tx) + lat[y0 + 1][x0 + 1] * tx
            out[y][x] = a * (1 - ty) + b * ty
    return out


def material(w, h, tones, seed, cell=3, grain=0.12):
    """A flat material in clustered tones (dark..light), vanilla style: soft blobs of tone and a little grain."""
    n1 = value_noise(w, h, cell, seed)
    n2 = value_noise(w, h, max(1, cell // 2), seed + 7)
    rnd = random.Random(seed + 3)
    img = Image.new('RGBA', (w, h))
    px = img.load()
    k = len(tones)
    for y in range(h):
        for x in range(w):
            v = n1[y][x] * 0.7 + n2[y][x] * 0.3 + (rnd.random() - 0.5) * grain
            i = max(0, min(k - 1, int(v * k)))
            px[x, y] = tones[i]
    return img


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def rect(img, x0, y0, x1, y1, c):
    for y in range(max(0, y0), min(img.height, y1)):
        for x in range(max(0, x0), min(img.width, x1)):
            img.putpixel((x, y), c)


def draw_rows(img, x0, y0, rows, pal):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in pal:
                put(img, x0 + i, y0 + j, pal[ch])


# ============================================================================ the book screens (vanilla book layout)
#
# textures/gui/knowledge_book.png 256x256, read by client/codex/KnowledgeBookScreen (and LoreScreen for the arrows):
#   (0, 0) 192x192 .... the book, open at one page as in vanilla's book screen: text at (36, 32), 114 wide
#   (192, 14i) 22x14 .. chapter tab i;  (214, 14i) its raised (open or hovered) state
#   (0, 192) 23x13 .... next page; (23, 192) hovered; (46, 192) previous page; (69, 192) hovered
#   (92 + 10i, 192) ... 9x9 marks: sealed, done, known, star, more above, more below
#   (152, 192) 18x18 .. recipe slot;  (172, 192) 22x15 recipe arrow
#   (0, 206) 100x5 .... rule;  (0, 212) 102x7 progress frame;  (0, 220) 100x5 progress fill
#   (104 + 13i, 206) .. 12x12 chapter emblem i (drawn large on the chapter's first page)
# textures/gui/lore_pages.png 1024x512, read by client/knowledge/LoreScreen:
#   (192o, 0) the origin's book page, (192o, 192) its scroll sheet, (0, 384 + 12o) 176x12 its scroll roller

INK = hx('#3a2614')
GOLD = hx('#d9a72c')
GOLD_L = hx('#f2cc5a')
GOLD_D = hx('#9a7015')
BOOK_STYLE = {   # leather dark -> light (5), paper dark -> light (4), trim
    'knowledge': (['#0c1230', '#18224c', '#1e2a5c', '#24326a', '#33447e'], ['#b8a682', '#d6c8a6', '#e8dcbc', '#f0e6c8'], '#d9a72c'),
    'creator': (['#6a5a3a', '#bfb498', '#d8cfba', '#e6dfcc', '#f4efe2'], ['#c8bca0', '#e6dcc4', '#f2ead6', '#f8f2e4'], '#d9a72c'),
    'pillager': (['#24120a', '#4a2a18', '#5a3420', '#6a3e26', '#8a5634'], ['#a8885a', '#c6a06c', '#d2ae7a', '#dcbb8a'], '#c7713f'),
    'cultist': (['#040e10', '#0e1c20', '#142629', '#1a3034', '#24424a'], ['#8a968c', '#a8b2a6', '#b4bdb0', '#c0c8ba'], '#2fb8b0'),
    'ocean': (['#3a4a5a', '#7a8a9a', '#8a9aa6', '#9cacb6', '#c0ccd2'], ['#b8c8cc', '#d6e2e6', '#e2ecee', '#edf4f4'], '#e8707a'),
    'soul': (['#04081c', '#0e1438', '#121838', '#161e44', '#22306a'], ['#141a3c', '#18204a', '#1c2554', '#212b5e'], '#5ff0ff'),
}
PAGE_EMBLEM = {  # 7x7, centred between the page arrows
    'knowledge': ['...#...', '..#.#..', '.#.#.#.', '#.###.#', '.#.#.#.', '..#.#..', '...#...'],
    'creator': ['...#...', '.#.#.#.', '..###..', '###.###', '..###..', '.#.#.#.', '...#...'],
    'pillager': ['.#####.', '#.....#', '#.#.#.#', '#..#..#', '#.#.#.#', '#.....#', '.#####.'],
    'cultist': ['...#...', '..#.#..', '.#...#.', '#..#..#', '.#...#.', '..#.#..', '...#...'],
    'ocean': ['.#####.', '#.#.#.#', '#.#.#.#', '.#.#.#.', '..###..', '...#...', '.......'],
    'soul': ['#.....#', '.#...#.', '..#.#..', '...#...', '..#.#..', '.#...#.', '#.....#'],
}


def _book_page(img, x0, y0, style, seed):
    """A book open at one page, in vanilla's book-screen layout (192x192 at x0, y0): the cover round the page, the
    binding's shadow in the gutter, the page block's edge on the right, a small emblem at the foot of the page."""
    leather, paper, trim = BOOK_STYLE[style]
    L = [hx(c) for c in leather]
    P = [hx(c) for c in paper]
    cx0, cy0, cw, ch = 18, 0, 152, 184
    cov = material(cw, ch, L[1:4], seed, cell=3, grain=0.06)
    cp = cov.load()
    for y in range(ch):
        for x in range(cw):
            corner = (x < 2 or x > cw - 4) and (y < 2 or y > ch - 4)
            if corner and (min(x, cw - 1 - x) + min(y, ch - 1 - y)) < 2:
                cp[x, y] = (0, 0, 0, 0)
                continue
            e = min(x, y, cw - 1 - x, ch - 1 - y)
            if e == 0:
                cp[x, y] = L[0]
            elif e == 1:
                cp[x, y] = L[4] if (x <= 1 or y <= 1) else L[1]
    img.alpha_composite(cov, (x0 + cx0, y0 + cy0))
    if style == 'knowledge':
        # gilt corners on the cover's outer edge
        for (cx, cy, fx, fy) in ((cx0 + cw - 2, 1, -1, 1), (cx0 + cw - 2, ch - 2, -1, -1)):
            for i in range(7):
                put(img, x0 + cx + fx * i, y0 + cy, GOLD if i else GOLD_L)
                put(img, x0 + cx, y0 + cy + fy * i, GOLD if i else GOLD_L)
                put(img, x0 + cx + fx * i, y0 + cy + fy, GOLD_D)
                put(img, x0 + cx + fx, y0 + cy + fy * i, GOLD_D)
    # the edges of the pages under this one
    for y in range(7, 176):
        for x in range(162, 167):
            put(img, x0 + x, y0 + y, P[1] if (x + (y // 40)) % 2 else P[3])
    # the page: flat paper, the binding's shade on its left, a darker foot and fore-edge
    pw, ph = 138, 172
    page = Image.new('RGBA', (pw, ph))
    pp = page.load()
    rnd = random.Random(seed)
    for y in range(ph):
        for x in range(pw):
            c = P[2] if rnd.random() < 0.035 else P[3]
            if x < 8:
                c = darken(c, 0.035 * (8 - x))
            if y >= ph - 2 or x >= pw - 1:
                c = darken(c, 0.08)
            pp[x, y] = c
    img.alpha_composite(page, (x0 + 24, y0 + 5))
    t = hx(trim)
    draw_rows(img, x0 + 88, y0 + 161, PAGE_EMBLEM[style], {'#': mix(t, P[2], 0.35)})


def _scroll_page(img, x0, y0, style, seed):
    """A scroll's sheet, unrolled to the same text area as a book page; its rollers are drawn over its ends."""
    leather, paper, trim = BOOK_STYLE[style]
    P = [hx(c) for c in paper]
    rnd = random.Random(seed)
    pw, ph = 148, 170
    page = Image.new('RGBA', (pw, ph))
    pp = page.load()
    for y in range(ph):
        for x in range(pw):
            pp[x, y] = P[2] if rnd.random() < 0.05 else P[3]
    left = [rnd.choice((0, 0, 1)) for _ in range(ph)]
    right = [rnd.choice((0, 0, 1)) for _ in range(ph)]
    for y in range(ph):
        for x in range(pw):
            if x < left[y] or x >= pw - right[y]:
                pp[x, y] = (0, 0, 0, 0)
                continue
            e = min(x - left[y], pw - 1 - right[y] - x)
            if e < 3:
                pp[x, y] = darken(pp[x, y], 0.07 * (3 - e)) if style != 'soul' else lighten(pp[x, y], 0.05 * (3 - e))
    img.alpha_composite(page, (x0 + 20, y0 + 12))
    draw_rows(img, x0 + 88, y0 + 161, PAGE_EMBLEM[style], {'#': mix(hx(trim), P[2], 0.35)})


def _roller(img, x0, y0, style):
    """The rod a scroll is wound on (176x12): a turned body with a knob at each end."""
    rod = {'creator': ('#f2ead6', '#d9a72c'), 'pillager': ('#7a4e2c', '#c7713f'), 'cultist': ('#d8d2c0', '#1f8a8a'),
           'ocean': ('#c0ccd4', '#e8707a'), 'soul': ('#24306a', '#5ff0ff')}[style]
    body, knob = hx(rod[0]), hx(rod[1])
    for x in range(8, 168):
        for y in range(2, 10):
            t = (y - 2) / 7.0
            c = lighten(body, 0.25) if t < 0.2 else (body if t < 0.7 else darken(body, 0.3))
            put(img, x0 + x, y0 + y, c)
        put(img, x0 + x, y0 + 1, darken(body, 0.55))
        put(img, x0 + x, y0 + 10, darken(body, 0.55))
    for kx in (0, 168):
        for x in range(8):
            for y in range(12):
                if (x in (0, 7)) and (y in (0, 11)):
                    continue
                t = y / 11.0
                c = lighten(knob, 0.3) if t < 0.25 else (knob if t < 0.7 else darken(knob, 0.3))
                if x in (0, 7) or y in (0, 11):
                    c = darken(knob, 0.55)
                put(img, x0 + kx + x, y0 + y, c)


# chapter ribbons, in CodexEntries chapter order: creatures, items(recipes), places, magic(machines), dictator(heralds), songs,
# enchantments, lore, guide
RIBBON_COLOURS = ['#4f8a4a', '#b07a2a', '#6c7488', '#2f8f8f', '#8e2f3c', '#b85488', '#6b4fb0', '#c99a36', '#d8d0bc']
ICONS = {
    'creatures': ['....##..##..', '...####.###.', '...####.###.', '....##...#..', '.##.......#.', '####.####...', '####.#####..',
                  '.##.######..', '....######..', '....#####...', '.....###....', '............'],
    'items': ['............', '.##########.', '.#..#..#..#.', '.#..#..#..#.', '.##########.', '.#..#..#..#.', '.#..#..#..#.',
              '.##########.', '.#..#..#..#.', '.#..#..#..#.', '.##########.', '............'],
    'places': ['.....##.....', '....####....', '....#..#....', '....####....', '.#.######.#.', '.##########.', '.#..#..#..#.',
               '.##########.', '.###.##.###.', '.###.##.###.', '.##########.', '............'],
    'magic': ['.....##.....', '..#.####.#..', '..########..', '.###....###.', '..##.##.##..', '####.##.####', '####.##.####',
              '..##.##.##..', '.###....###.', '..########..', '..#.####.#..', '.....##.....'],
    'dictator': ['............', '.#...##...#.', '.##.####.##.', '.##########.', '.#.##..##.#.', '.##########.', '.###.##.###.',
                 '..########..', '...######...', '....#..#....', '............', '............'],
    'songs': ['......######', '......######', '......#....#', '......#....#', '......#....#', '......#....#', '......#....#',
              '..###.#..###', '.####.#.####', '.####..#####', '..##....###.', '............'],
    'enchantments': ['.....#......', '.....#......', '....###.....', '#..#####..#.', '.#########..', '..#######...', '.#########..',
                     '#..#####..#.', '....###.....', '.....#....#.', '.....#...###', '..........#.'],
    'lore': ['..########..', '.#........#.', '.#.######.#.', '..#......#..', '..#.####.#..', '..#......#..', '..#.####.#..',
             '..#......#..', '..#.###..#..', '.#........#.', '.#.######.#.', '..########..'],
    'guide': ['.....##.....', '.....##.....', '....#..#....', '...#.##.#...', '..#..##..#..', '######.#####', '#####.######',
              '..#..##..#..', '...#.##.#...', '....#..#....', '.....##.....', '.....##.....'],
}
ICON_ORDER = ['creatures', 'items', 'places', 'magic', 'dictator', 'songs', 'enchantments', 'lore', 'guide']
SMALL = {  # 9x9 marks
    'lock': ['..#####..', '.#.....#.', '.#.....#.', '#########', '#ooooooo#', '#ooo#ooo#', '#ooo#ooo#', '#ooooooo#', '#########'],
    'check': ['........#', '.......##', '......##.', '#....##..', '##..##...', '.####....', '..##.....', '.........', '.........'],
    'bullet': ['.........', '..#......', '..##.....', '..###....', '..####...', '..###....', '..##.....', '..#......', '.........'],
    'star': ['....#....', '....#....', '...###...', '#########', '.#######.', '..#####..', '.###.###.', '.##...##.', '.#.....#.'],
    'up': ['.........', '.........', '....#....', '...###...', '..#####..', '.#######.', '.........', '.........', '.........'],
    'down': ['.........', '.........', '.#######.', '..#####..', '...###...', '....#....', '.........', '.........', '.........'],
}


ARROW = ['..............##.......', '..............#o#......', '..............#oo#.....', '..............#ooo#....',
         '###############oooo#...', '#oooooooooooooooooooo#.', '#ooooooooooooooooooooo#', '#oooooooooooooooooooo#.',
         '###############oooo#...', '..............#ooo#....', '..............#oo#.....', '..............#o#......',
         '..............##.......']


def _arrow(img, x0, y0, hover, back):
    fill, edge, lit = ((hx('#f2d27a'), hx('#5a4010'), hx('#fff0b8')) if hover else (hx('#cbb68c'), hx('#4a3820'), hx('#ebdfbc')))
    for y, row in enumerate(ARROW):
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            c = edge if ch == '#' else (lit if y <= 5 else (fill if y < 8 else darken(fill, 0.15)))
            put(img, x0 + (22 - x if back else x), y0 + y, c)


def _tab(img, x0, y0, ch, raised):
    """A chapter's ribbon tab (22x14) with a notched end, its emblem on the part that shows past the cover."""
    col = hx(RIBBON_COLOURS[ch])
    if raised:
        col = lighten(col, 0.18)

    def inside(x, y):
        return 0 <= x < 22 and 0 <= y < 14 and not (x >= 19 and abs(y - 6.5) < (x - 18.5))
    for y in range(14):
        for x in range(22):
            if not inside(x, y):
                continue
            edge = not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            c = darken(col, 0.55) if edge else (lighten(col, 0.15) if y <= 2 else (col if y < 11 else darken(col, 0.15)))
            put(img, x0 + x, y0 + y, c)
    icon = ICONS[ICON_ORDER[ch]]
    pale = lighten(col, 0.7)
    for y, row in enumerate(icon):
        for x, c_ in enumerate(row):
            if c_ == '#':
                put(img, x0 + 5 + x, y0 + 1 + y, pale)


def book_gui(out):
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    _book_page(img, 0, 0, 'knowledge', 77)
    for ch in range(9):
        _tab(img, 192, 14 * ch, ch, False)
        _tab(img, 214, 14 * ch, ch, True)
    _arrow(img, 0, 192, False, False)
    _arrow(img, 23, 192, True, False)
    _arrow(img, 46, 192, False, True)
    _arrow(img, 69, 192, True, True)
    marks = {'lock': (hx('#a8302a'), hx('#e0605a')), 'check': (hx('#3f7a3a'), hx('#3f7a3a')), 'bullet': (INK, INK),
             'star': (GOLD, GOLD_L), 'up': (INK, INK), 'down': (INK, INK)}
    for i, (k, rows) in enumerate(SMALL.items()):
        draw_rows(img, 92 + 10 * i, 192, rows, {'#': marks[k][0], 'o': marks[k][1]})
    # the recipe slot (vanilla's, in ink on paper) and the recipe arrow
    for y in range(18):
        for x in range(18):
            c = hx('#d8ccac')
            if x == 0 or y == 0:
                c = hx('#8a7a5a')
            elif x == 17 or y == 17:
                c = hx('#fbf4e0')
            put(img, 152 + x, 192 + y, c)
    craft = ['..........#...........', '..........##..........', '..........###.........', '..........####........',
             '#################.....', '##################....', '###################...', '##################....',
             '#################.....', '..........####........', '..........###.........', '..........##..........',
             '..........#...........', '......................', '......................']
    draw_rows(img, 172, 192, craft, {'#': hx('#8a7a5a')})
    for x in range(100):                      # the rule: a gold line with a lozenge
        put(img, x, 208, GOLD if 6 < x < 94 else GOLD_D)
    draw_rows(img, 46, 206, ['...#...', '.#####.', '...#...'], {'#': GOLD_D})
    put(img, 49, 207, GOLD_L)
    for y in range(7):                        # progress frame and fill
        for x in range(102):
            e = x in (0, 101) or y in (0, 6)
            put(img, x, 212 + y, INK if e else hx('#c8b896'))
    for y in range(5):
        for x in range(100):
            put(img, x, 220 + y, GOLD_L if y == 0 else (GOLD if y < 4 else GOLD_D))
    for ch in range(9):
        icon = ICONS[ICON_ORDER[ch]]
        col = darken(hx(RIBBON_COLOURS[ch]), 0.2)
        for y, row in enumerate(icon):
            for x, c_ in enumerate(row):
                if c_ == '#':
                    put(img, 104 + 13 * ch + x, 206 + y, col)
    out('gui/knowledge_book', img)



def lore_pages(out):
    sheet = Image.new('RGBA', (1024, 512), (0, 0, 0, 0))
    for i, o in enumerate(ORIGINS):
        _book_page(sheet, 192 * i, 0, o, 101 + i * 17)
        _scroll_page(sheet, 192 * i, 192, o, 201 + i * 17)
        _roller(sheet, 0, 384 + 12 * i, o)
    out('gui/lore_pages', sheet)


# ============================================================================ the Lore Books (block + item) and Scrolls

COVERS = {
    'creator': (['#d8cfba', '#e6dfcc', '#efe9d8', '#f8f4e8'], '#d9a72c', '#f2cc5a', '#9a7015'),
    'pillager': (['#5a3420', '#6a3e26', '#7a4a2c', '#8a5634'], '#c7713f', '#e39a6a', '#8a4a24'),
    'cultist': (['#1c2628', '#232f32', '#2a383c', '#324246'], '#1f8a8a', '#4ff0e6', '#0c3a3e'),
    'ocean': (['#8a9aa6', '#9cacb6', '#aebcc4', '#c0ccd2'], '#e8707a', '#ffb0b4', '#9a3a4a'),
    'soul': (['#121838', '#161e44', '#1a2450', '#202c5e'], '#3f8fd8', '#5ff0ff', '#1f4a8a'),
}
EMBLEMS = {
    'creator': ['...g...', '.g.G.g.', '..GGG..', 'gGGwGGg', '..GGG..', '.g.G.g.', '...g...'],
    'pillager': ['.GGGGG.', 'G.....G', 'G.g.g.G', 'G..g..G', 'G.g.g.G', 'G.....G', '.GGGGG.'],
    'cultist': ['...G...', '..GgG..', '.Gg.gG.', 'Gg.w.gG', '.Gg.gG.', '..GgG..', '...G...'],
    'ocean': ['.GGGGG.', 'GgGgGgG', 'GgGgGgG', '.GgGgG.', '..GGG..', '...g...', '.......'],
    'soul': ['G.....G', '.G...G.', '..GwG..', '..wGw..', '..GwG..', '.G...G.', 'G.....G'],
}


def _cover(o):
    tones, trim, trim_l, trim_d = COVERS[o]
    img = material(16, 16, [hx(c) for c in tones], 300 + ORIGINS.index(o), cell=2, grain=0.1)
    px = img.load()
    t, tl, td = hx(trim), hx(trim_l), hx(trim_d)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            px[x, y] = td
    for (cx, cy, fx, fy) in ((1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
        for i in range(3):
            for j in range(3 - i):
                px[cx + fx * i, cy + fy * j] = tl if i + j == 0 else t
    draw_rows(img, 4, 4, EMBLEMS[o], {'G': t, 'g': td, 'w': tl})
    if o == 'cultist':
        # sculk growing over the cover from one corner
        for (x, y) in ((12, 9), (13, 10), (11, 11), (12, 11), (13, 12), (10, 12), (11, 13), (12, 13), (9, 14), (10, 14), (14, 13)):
            px[x, y] = hx('#0c3a3e')
        px[12, 12] = hx('#4ff0e6')
        px[10, 13] = hx('#2fb8b0')
    return img


def _side(o):
    """The page block's edges (an open book's sides): fine stacked lines of its paper."""
    leather, paper, trim = BOOK_STYLE[o]
    P = [hx(c) for c in paper]
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = P[3] if y % 2 else (P[1] if (x // 5 + y // 4) % 3 == 0 else P[2])
    return img


def _trim(o):
    """The ribbon marking the page."""
    leather, paper, trim = BOOK_STYLE[o]
    t = hx(trim)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = lighten(t, 0.2) if x % 4 == 0 else (t if x % 4 < 3 else darken(t, 0.25))
    return img


def _pages_tex(o):
    """The open pages seen from above: two pages either side of the gutter, written in lines of ink."""
    leather, paper, trim = BOOK_STYLE[o]
    P = [hx(c) for c in paper]
    ink = hx('#cfe8ff') if o == 'soul' else hx('#3a2a18')
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    rnd = random.Random(31 + ORIGINS.index(o))
    for y in range(16):
        for x in range(16):
            c = P[2] if rnd.random() < 0.06 else P[3]
            if x in (7, 8):
                c = darken(c, 0.12 if x == 8 else 0.06)
            px[x, y] = c
    for y in range(3, 14, 2):
        for x0, x1 in ((2, 6), (10, 14)):
            end = x1 - (1 if rnd.random() < 0.35 else 0)
            for x in range(x0, end + 1):
                px[x, y] = mix(px[x, y], ink, 0.3)
    return img


# ============================================================================ Music Sheet art

SHEET_W, SHEET_H = 176, 112
SHEET_THEME = {
    'offering': ('#d9a72c', 'chimes'), 'nib': ('#e07ab8', 'butterflies'), 'golem': ('#2aa8a4', 'lamps'),
    'crystal': ('#8f86e0', 'crystals'), 'whale': ('#4fb2d8', 'waves'), 'lullaby': ('#a88ad8', 'moons'), 'aurora': ('#e070c8', 'rays'),
    'hymn': ('#e0b23a', 'rays'),  # S1 land: the Creator's Hymn
}
SONG_INSTRUMENT_ITEM = {'chimes': 'wind_chimes', 'strings': 'star_lute', 'flute': 'crane_flute', 'drum': 'conga_drum', 'prism': 'prism_harp',
                        'any': 'guitar'}
SONG_CREATURE_MODEL = {'offering': ('enchoer', 'enchoer'), 'nib': ('nib', 'nib'), 'golem': ('soul_golem', 'soul_golem'),
                       'crystal': ('caravan', 'caravan_amber'), 'whale': ('sky_whale', 'sky_whale'), 'lullaby': ('gobbler', 'gobbler'),
                       'hymn': ('mini_creator', 'mini_creator')}
SEPIA = [hx('#3a2614'), hx('#5a3e22'), hx('#7a5a34'), hx('#9a7a4c')]


def _ink(img, tex_root):
    """Turns a picture into an ink drawing on the parchment: four sepia tones by brightness and a dark contour."""
    w, h = img.size
    src = img.load()
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    o = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a < 100:
                continue
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255.0
            o[x, y] = SEPIA[min(3, int(lum * 4.2))]
    edge = []
    for y in range(h):
        for x in range(w):
            if o[x, y][3] == 0 and any(0 <= x + dx < w and 0 <= y + dy < h and o[x + dx, y + dy][3] > 0
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.append((x, y))
    for x, y in edge:
        o[x, y] = SEPIA[0]
    return out


def _creature_sketch(tex_root, model_name, tex_name, box):
    """A small ink drawing of the creature the song is for, from its real model and texture."""
    try:
        import mobs
        from modelkit import preview
    except Exception:
        return None
    path = os.path.join(tex_root, 'entity', model_name, tex_name + '.png')
    if model_name not in mobs.ALL or not os.path.exists(path):
        return None
    m = mobs.ALL[model_name]()
    m.pack()
    tex = Image.open(path).convert('RGBA')
    big = preview(m, tex, None, yaw=35, pitch=18, scale=8, size=(360, 360), bg=(0, 0, 0, 0))
    bb = big.getbbox()
    if not bb:
        return None
    big = big.crop(bb)
    bw, bh = box
    k = min(bw / big.width, bh / big.height)
    small = big.resize((max(1, int(big.width * k)), max(1, int(big.height * k))), Image.BOX)
    return _ink(small, tex_root)


def _instrument_sketch(tex_root, item):
    path = os.path.join(tex_root, 'item', item + '.png')
    if not os.path.exists(path):
        return None
    sprite = Image.open(path).convert('RGBA')
    if sprite.height > sprite.width:  # an animated strip: its first frame
        sprite = sprite.crop((0, 0, sprite.width, sprite.width))
    return _ink(sprite.resize((32, 32), Image.NEAREST), tex_root)


def _motif(img, kind, col, rnd):
    """Each song's own border decoration, drawn in the margin in its colour."""
    W, H = img.size
    c = hx(col)
    cd = darken(c, 0.35)
    marks = {
        'chimes': ['.#.', '.#.', '.#.', '.#.', '###'],
        'butterflies': ['#.#', '###', '.#.', '#.#'],
        'lamps': ['.#.', '###', '#o#', '###'],
        'crystals': ['.#.', '###', '###', '.#.'],
        'waves': ['.##..', '#..#.', '....#'],
        'moons': ['.##', '#..', '#..', '.##'],
        'rays': ['#...#', '.#.#.', '..#..'],
    }[kind]
    spots = [(x, 3) for x in range(6, W - 8, 14)] + [(x, H - 8) for x in range(12, W - 8, 14)]
    for i, (x, y) in enumerate(spots):
        draw_rows(img, x, y, marks, {'#': c if i % 2 else cd, 'o': lighten(c, 0.5)})


def _staff(img, notes, colours, col):
    """Five staff lines, a clef, and the song's notes placed by pitch (note-block 0..24, F#3..F#5)."""
    line = hx('#8a6a44')
    top = 28
    for i in range(5):
        y = top + i * 4
        for x in range(10, SHEET_W - 10):
            img.putpixel((x, y), line)
    clef = ['..#..', '.#.#.', '.#.#.', '..#..', '.##..', '#.#..', '#.##.', '#.#.#', '.###.', '..#..', '..#..', '.##..']
    draw_rows(img, 12, top - 3, clef, {'#': hx('#3a2614')})
    n = len(notes)
    span = SHEET_W - 50
    for i, p in enumerate(notes):
        x = 28 + int(i * span / max(1, n - 1))
        # one staff step per two semitones, low F#3 just under the bottom line
        y = top + 16 + 2 - int(round(p / 2.0)) * 2
        head = hx('#3a2614')
        if colours:
            head = hx(['#ff5fa2', '#ffc341', '#3fe6e0', '#a67bff'][colours[i]])
        for dx, dy in ((0, 0), (1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (1, -1), (1, 2)):
            put(img, x + dx, y + dy, head)
        for dy in range(1, 9):
            put(img, x + 3, y - dy + 1, hx('#3a2614'))
        if i % 2 == 0:
            put(img, x + 4, y - 7, hx('#3a2614'))
            put(img, x + 5, y - 6, hx('#3a2614'))
        # ledger lines above or below the staff
        for ly in range(top - 4, y - 1, -4) if y < top else range(top + 20, y + 2, 4):
            for dx in range(-1, 5):
                put(img, x + dx, ly, line)
        put(img, x + 1, top + 24, hx(col))


def _frame(img, x0, y0, w, h, col):
    c = hx(col)
    for x in range(x0, x0 + w):
        for y in (y0, y0 + h - 1):
            if (x // 2) % 2 == 0:
                put(img, x, y, c)
    for y in range(y0, y0 + h):
        for x in (x0, x0 + w - 1):
            if (y // 2) % 2 == 0:
                put(img, x, y, c)


def sheet_art(song, notes, colours, instrument_family, tex_root):
    col, kind = SHEET_THEME.get(song, ('#b07a2a', 'chimes'))
    rnd = random.Random(sum(map(ord, song)))
    img = material(SHEET_W, SHEET_H, [hx('#dcc79c'), hx('#e6d4ac'), hx('#efe0bc'), hx('#f5ead0')], 700 + len(song), cell=6, grain=0.06)
    px = img.load()
    for y in range(SHEET_H):
        for x in range(SHEET_W):
            e = min(x, y, SHEET_W - 1 - x, SHEET_H - 1 - y)
            if e < 4:
                px[x, y] = darken(px[x, y], 0.07 * (4 - e))
    # a curled corner at the bottom right
    for i in range(7):
        for j in range(7 - i):
            put(img, SHEET_W - 1 - i, SHEET_H - 1 - j, (0, 0, 0, 0))
        put(img, SHEET_W - 8 + i, SHEET_H - 1 - i, hx('#bfa878'))
    _motif(img, kind, col, rnd)
    # the title cartouche (the title itself is written by the game)
    for x in range(40, SHEET_W - 40):
        put(img, x, 18, darken(hx(col), 0.2))
    _staff(img, notes, colours, col)
    # the diagrams: the instrument on the left, the creature it is for on the right
    _frame(img, 10, 58, 52, 44, '#9a7a4c')
    _frame(img, 70, 58, 96, 44, '#9a7a4c')
    inst = _instrument_sketch(tex_root, SONG_INSTRUMENT_ITEM.get(instrument_family, 'guitar'))
    if inst:
        img.alpha_composite(inst, (20, 62))
    if song in SONG_CREATURE_MODEL:
        model, tex = SONG_CREATURE_MODEL[song]
        sk = _creature_sketch(tex_root, model, tex, (84, 34))
        if sk:
            img.alpha_composite(sk, (70 + (96 - sk.width) // 2, 60 + (34 - sk.height) // 2))
    else:
        # the Aurora: rays of its four lights spreading over a dark horizon
        for k, lc in enumerate(['#ff5fa2', '#ffc341', '#3fe6e0', '#a67bff']):
            for t in range(30):
                x = 118 + int(math.cos(math.pi * (0.15 + 0.23 * k)) * t * 1.4)
                y = 92 - int(math.sin(math.pi * (0.15 + 0.23 * k)) * t)
                put(img, x, y, hx(lc))
        for x in range(76, 160):
            put(img, x, 93, SEPIA[0])
    return img


def sheets(out, tex_root):
    import songs
    for song, (notes, _) in songs.SONGS.items():
        fam = songs.SONG_INSTRUMENT.get(song) or 'any'
        out(f'gui/sheets/{song}', sheet_art(song, notes, songs.SONG_LIGHTS.get(song, []), fam, tex_root))


# ============================================================================ entry points


def textures(out):
    import gen_textures
    book_gui(out)
    lore_pages(out)
    for o in ORIGINS:
        out(f'block/{o}_lore_book_cover', _cover(o))
        out(f'block/{o}_lore_book_side', _side(o))
        out(f'block/{o}_lore_book_trim', _trim(o))
        out(f'block/{o}_lore_book_pages', _pages_tex(o))
    out('item/mini_creator_spawn_egg', __import__('mini_creator').spawn_egg())
    sheets(out, gen_textures.TEX)
