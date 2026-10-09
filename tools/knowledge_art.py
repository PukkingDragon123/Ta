"""F3 Knowledge & lore: every texture of the Knowledge Book, the Lore Books and Scrolls and the Music Sheet art.

  textures/gui/knowledge_book.png 512x512 (layout in KB below; client/knowledge/KnowledgeBookScreen.java)
  textures/gui/lore_pages.png 1024x256: one 192x232 reading page per origin at (200 * i, 0)
  textures/gui/lore_rollers.png 256x96: the scroll rollers, 208x16 per origin at (0, 16 * i)
  textures/gui/sheets/<song>.png 176x112: each song's sheet - staff, notes and ink diagrams of its instrument and creature
  block/<origin>_lore_book_{cover,side,trim}, block/lore_book_pages, item/<origin>_lore_{book,scroll}, item/<origin>_lore_scroll_3d,
  item/knowledge_book, item/mini_creator_spawn_egg

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


# ============================================================================ the Knowledge Book GUI

BW, BH = 312, 196
LEATHER = [hx('#1a2238'), hx('#202a44'), hx('#243050'), hx('#2a375a'), hx('#313f66')]
GOLD = hx('#d9a72c')
GOLD_L = hx('#f2cc5a')
GOLD_D = hx('#9a7015')
GOLD_S = hx('#6a4c12')
PAPER = [hx('#e2d0a8'), hx('#e9d9b4'), hx('#eee0bd'), hx('#f2e6c6'), hx('#f5eacd')]
INK = hx('#3a2a18')

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


def _book_spread(img):
    W, H = BW, BH
    cover = material(W, H, LEATHER, 11, cell=4)
    cp = cover.load()
    px = img.load()
    for y in range(H):
        for x in range(W):
            # rounded corners
            cx = min(x, W - 1 - x)
            cy = min(y, H - 1 - y)
            if cx + cy < 2:
                continue
            c = cp[x, y]
            if cx == 0 or cy == 0:
                c = hx('#0e1424')
            elif cx == 1 or cy == 1:
                c = lighten(c, 0.12) if (x < W // 2 and y < H // 2) else darken(c, 0.25)
            px[x, y] = c
    # gold tooling: a fine line inset round the cover
    for x in range(4, W - 4):
        for y in (4, H - 5):
            px[x, y] = GOLD_D if (x // 2) % 3 else GOLD
    for y in range(4, H - 4):
        for x in (4, W - 5):
            px[x, y] = GOLD_D if (y // 2) % 3 else GOLD
    # the page block: stacked page edges, then the two pages
    x0, y0, x1, y1 = 9, 8, W - 10, H - 9
    paper = material(W, H, PAPER, 23, cell=3, grain=0.05)
    pp = paper.load()
    mid = W // 2
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            edge = (y > y1 - 3) or (x < x0 + 2) or (x > x1 - 2)
            if edge:
                px[x, y] = PAPER[0] if (y + x // 7) % 2 == 0 else PAPER[2]
                continue
            c = pp[x, y]
            d = abs(x + 0.5 - mid)
            # the pages curve into the gutter and darken towards their outer edges
            if d < 2:
                c = darken(PAPER[0], 0.35)
            elif d < 4:
                c = darken(c, 0.22)
            elif d < 8:
                c = darken(c, 0.12)
            elif d < 12:
                c = darken(c, 0.05)
            out = min(x - x0, x1 - x)
            if out < 4:
                c = darken(c, 0.06 * (4 - out))
            if y < y0 + 2:
                c = darken(c, 0.08)
            px[x, y] = c
    # foxing: a few faint age spots
    rnd = random.Random(5)
    for _ in range(26):
        sx, sy = rnd.randrange(x0 + 6, x1 - 6), rnd.randrange(y0 + 6, y1 - 8)
        if abs(sx - mid) < 10:
            continue
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            px[sx + dx, sy + dy] = mix(px[sx + dx, sy + dy], PAPER[0], 0.55)
    # gold corner caps with a rivet
    for (cx, cy, fx, fy) in ((0, 0, 1, 1), (W - 1, 0, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)):
        for i in range(12):
            for j in range(12 - i):
                if i + j < 2:
                    continue
                c = GOLD_L if (i == 1 or j == 1) else (GOLD if i + j < 8 else GOLD_D)
                if i + j == 11:
                    c = GOLD_S
                px[cx + fx * i, cy + fy * j] = c
        px[cx + fx * 4, cy + fy * 4] = GOLD_S
        px[cx + fx * 3, cy + fy * 3] = GOLD_L


def _ribbons(img):
    """9 ribbons 22x26 at (24 * i, 200); the selected versions (lighter, with a gold tip) at (24 * i, 228)."""
    for row, sel in ((200, False), (228, True)):
        for i, (key, col) in enumerate(zip(ICON_ORDER, RIBBON_COLOURS)):
            base = hx(col)
            if sel:
                base = lighten(base, 0.12)
            x0 = i * 24
            tones = [darken(base, 0.18), darken(base, 0.08), base, lighten(base, 0.08)]
            cloth = material(22, 26, tones, 31 + i, cell=2, grain=0.05)
            cp = cloth.load()
            for y in range(26):
                for x in range(22):
                    if y < 2 and (x < 2 - y or x > 19 + y):
                        continue
                    c = cp[x, y]
                    if x in (0, 21) or y == 0:
                        c = darken(base, 0.45)
                    elif x == 1 or y == 1:
                        c = lighten(base, 0.2)
                    elif x == 20:
                        c = darken(base, 0.25)
                    # woven texture: faint horizontal threads
                    elif y % 3 == 0:
                        c = darken(c, 0.06)
                    put(img, x0 + x, row + y, c)
            if sel:
                for x in range(3, 19):
                    put(img, x0 + x, row + 2, GOLD_L if x % 2 else GOLD)
            icon = ICONS[key]
            ink = hx('#f6efdc') if key != 'guide' else hx('#4a3a20')
            shadow = darken(base, 0.5) if key != 'guide' else hx('#b8a882')
            for j, r in enumerate(icon):
                for k, ch in enumerate(r):
                    if ch == '#':
                        put(img, x0 + 5 + k + 1, row + 5 + j + 1, shadow)
            for j, r in enumerate(icon):
                for k, ch in enumerate(r):
                    if ch == '#':
                        put(img, x0 + 5 + k, row + 5 + j, ink)


def _arrows(img):
    """Page arrows 18x10: next (0, 256), previous (20, 256); hover versions 12 rows lower."""
    arrow = ['.......#..........', '.......##.........', '########o#........', '#oooooooo##.......', '#ooooooooo##......',
             '#oooooooo##.......', '########o#........', '.......##.........', '.......#..........', '..................']
    for row, hover in ((256, False), (268, True)):
        body = GOLD_L if hover else GOLD
        edge = GOLD_D if not hover else GOLD
        for j, line in enumerate(arrow):
            for i, ch in enumerate(line):
                if ch == '.':
                    continue
                c = edge if ch == '#' else body
                if ch == 'o' and j == 3:
                    c = lighten(body, 0.3)
                put(img, i, row + j, c)
                put(img, 20 + 17 - i, row + j, c)


def _small_icons(img):
    """9x9 marks at (44 + 10 * i, 256) in SMALL order; then a 120x7 gold divider at (0, 282) and a progress bar
    (frame 102x7 at (0, 292), fill 100x5 at (0, 300))."""
    pal = {'#': hx('#5a3e1c'), 'o': hx('#c9a24a')}
    for i, key in enumerate(SMALL):
        rows = SMALL[key]
        c = {'lock': None, 'check': hx('#3f8a3a'), 'bullet': hx('#8a5a20'), 'star': hx('#c9952a'), 'up': hx('#6a4a24'),
             'down': hx('#6a4a24')}[key]
        p = dict(pal) if c is None else {'#': c}
        draw_rows(img, 44 + 10 * i, 256, rows, p)
    # the divider: a fine gold-ink rule with a diamond and two dots in the middle
    for x in range(120):
        d = abs(x - 59.5)
        if d < 4:
            continue
        a = 1.0 if d < 40 else max(0.0, 1.0 - (d - 40) / 20)
        put(img, x, 285, mix(hx('#f1e4c2'), hx('#9a7228'), a))
    draw_rows(img, 56, 282, ['...#...', '..#o#..', '.#ooo#.', '..#o#..', '...#...'], {'#': hx('#9a7228'), 'o': hx('#e2b84a')})
    for x in (50, 69):
        put(img, x, 284, hx('#9a7228'))
        put(img, x, 286, hx('#9a7228'))
    # progress bar
    for x in range(102):
        for y in range(7):
            edge = x in (0, 101) or y in (0, 6)
            put(img, x, 292 + y, hx('#6a4c22') if edge else hx('#d8c49a'))
    for x in range(100):
        for y in range(5):
            put(img, x, 300 + y, [hx('#f2cc5a'), hx('#e2b84a'), hx('#d9a72c'), hx('#b8861e'), hx('#9a7015')][y])
    # a recipe slot 18x18 at (128, 256) and the recipe arrow 22x15 at (148, 256)
    for y in range(18):
        for x in range(18):
            c = hx('#d8c49a')
            if x == 0 or y == 0:
                c = hx('#8a7048')
            elif x == 17 or y == 17:
                c = hx('#fbf3dc')
            put(img, 128 + x, 256 + y, c)
    ar = ['.............#........', '.............##.......', '.............###......', '#############o###.....',
          '#ooooooooooooooo###...', '#oooooooooooooooo###..', '#ooooooooooooooooo###.', '#oooooooooooooooooo###',
          '#ooooooooooooooooo###.', '#oooooooooooooooo###..', '#ooooooooooooooo###...', '#############o###.....',
          '.............###......', '.............##.......', '.............#........']
    draw_rows(img, 148, 256, ar, {'#': hx('#7a5a2c'), 'o': hx('#c9a86a')})


def book_gui(out):
    img = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    _book_spread(img)
    _ribbons(img)
    _arrows(img)
    _small_icons(img)
    out('gui/knowledge_book', img)


# ============================================================================ lore pages and rollers

PAGE_W, PAGE_H = 192, 232
PAGE_STYLE = {
    'creator': dict(paper=['#e6dcc4', '#efe6d0', '#f5eedc', '#faf5e8'], border='#c99a2c', border_d='#8a6616', accent='#e9c25a'),
    'pillager': dict(paper=['#b8925e', '#c6a06c', '#d2ae7a', '#dcbb8a'], border='#5e3a1e', border_d='#3a2412', accent='#c7713f'),
    'cultist': dict(paper=['#9aa49a', '#a8b2a6', '#b4bdb0', '#c0c8ba'], border='#1f4f52', border_d='#0f2e30', accent='#3fd8d0'),
    'ocean': dict(paper=['#c8d8dc', '#d6e2e6', '#e2ecee', '#edf4f4'], border='#8a9ca8', border_d='#5a6a78', accent='#e8707a'),
    'soul': dict(paper=['#141a3c', '#18204a', '#1c2554', '#212b5e'], border='#3f8fd8', border_d='#1f4a8a', accent='#5ff0ff'),
}


def _page(o, seed):
    st = PAGE_STYLE[o]
    tones = [hx(c) for c in st['paper']]
    img = material(PAGE_W, PAGE_H, tones, seed, cell=6, grain=0.08)
    px = img.load()
    W, H = PAGE_W, PAGE_H
    rnd = random.Random(seed)
    # darker, worn edges
    for y in range(H):
        for x in range(W):
            e = min(x, y, W - 1 - x, H - 1 - y)
            if e < 5:
                px[x, y] = darken(px[x, y], 0.06 * (5 - e)) if o != 'soul' else lighten(px[x, y], 0.03 * (5 - e))
    b, bd, acc = hx(st['border']), hx(st['border_d']), hx(st['accent'])
    if o == 'creator':
        # a double gold rule and a gold sun in each corner
        for x in range(8, W - 8):
            for y in (8, 11, H - 12, H - 9):
                px[x, y] = b if y in (8, H - 9) else bd
        for y in range(8, H - 8):
            for x in (8, 11, W - 12, W - 9):
                px[x, y] = b if x in (8, W - 9) else bd
        sun = ['...#...', '.#.#.#.', '..###..', '###o###', '..###..', '.#.#.#.', '...#...']
        for cx, cy in ((3, 3), (W - 10, 3), (3, H - 10), (W - 10, H - 10)):
            draw_rows(img, cx, cy, sun, {'#': b, 'o': acc})
    elif o == 'pillager':
        # burnt, stained edges, a stitched binding on the left and copper rivets
        for _ in range(9):
            cx, cy, r = rnd.randrange(W), rnd.randrange(H), rnd.uniform(6, 16)
            for y in range(int(cy - r), int(cy + r)):
                for x in range(int(cx - r), int(cx + r)):
                    if 0 <= x < W and 0 <= y < H and math.hypot(x - cx, y - cy) < r and rnd.random() < 0.85:
                        px[x, y] = darken(px[x, y], 0.08)
        for y in range(H):
            for x in range(W):
                e = min(x, y, W - 1 - x, H - 1 - y)
                jag = (x * 7 + y * 13) % 5
                if e < 2 + jag // 2:
                    px[x, y] = darken(px[x, y], 0.45) if e > jag // 3 else (0, 0, 0, 0)
        for y in range(10, H - 10, 6):
            px[6, y] = bd
            px[6, y + 1] = bd
            px[7, y + 2] = b
        for cx, cy in ((12, 12), (W - 14, 12), (12, H - 14), (W - 14, H - 14)):
            draw_rows(img, cx, cy, ['.oo.', 'oOOo', 'oOoo', '.oo.'], {'o': hx('#8a4a24'), 'O': hx('#e39a6a')})
    elif o == 'cultist':
        # Sculk creeping in from the corners, veined and specked with glowing points
        seeds = [(0, 0), (W, 0), (0, H), (W, H), (W // 2, H)]
        n = value_noise(W, H, 9, seed + 5)
        for y in range(H):
            for x in range(W):
                d = min(math.hypot(x - sx, y - sy) for sx, sy in seeds)
                v = d / 60.0 - n[y][x] * 0.6
                if v < 0.12:
                    px[x, y] = hx('#0c2a30') if (x + y) % 3 else hx('#123a40')
                    if rnd.random() < 0.03:
                        px[x, y] = acc
                elif v < 0.2:
                    px[x, y] = mix(px[x, y], hx('#0f3a40'), 0.6)
        for _ in range(14):
            x, y = rnd.randrange(W), rnd.randrange(H)
            for i in range(rnd.randrange(8, 24)):
                x += rnd.choice((-1, 0, 1))
                y += rnd.choice((-1, 0, 1))
                if 0 <= x < W and 0 <= y < H:
                    px[x, y] = mix(px[x, y], hx('#1f5a5e'), 0.5)
    elif o == 'ocean':
        # coral at the corners, a clam shell at the head of the page, a silver rule
        for x in range(10, W - 10):
            px[x, 22] = b
            px[x, H - 12] = b
        clam = ['....######....', '..##.#..#.##..', '.#..#.##.#..#.', '#..#..##..#..#', '#.#...##...#.#', '##....##....##',
                '.############.', '..#..####..#..']
        draw_rows(img, W // 2 - 7, 6, clam, {'#': hx('#b89a8a'), '.': hx('#f0dcd0')})
        coral = ['..#...#.', '..#..#..', '#.#.#...', '.###..#.', '..#..#..', '..####..', '...#....', '...#....']
        for cx, cy, flip in ((6, H - 16, False), (W - 14, H - 16, True), (6, 26, False), (W - 14, 26, True)):
            rows = [r[::-1] for r in coral] if flip else coral
            draw_rows(img, cx, cy, rows, {'#': acc})
    elif o == 'soul':
        # star dust and a glowing border of runes
        for _ in range(110):
            x, y = rnd.randrange(W), rnd.randrange(H)
            px[x, y] = mix(px[x, y], hx('#8fd8ff'), rnd.uniform(0.3, 0.8))
        runes = ['#.#', '.#.', '#.#'], ['###', '#..', '###'], ['#.#', '###', '#.#'], ['.#.', '###', '.#.'], ['##.', '.##', '##.']
        i = 0
        for x in range(10, W - 12, 6):
            for y in (6, H - 9):
                draw_rows(img, x, y, runes[i % 5], {'#': acc if i % 3 else b})
                i += 1
        for y in range(14, H - 14, 6):
            for x in (5, W - 8):
                draw_rows(img, x, y, runes[i % 5], {'#': acc if i % 3 else b})
                i += 1
    return img


def lore_pages(out):
    sheet = Image.new('RGBA', (1024, 256), (0, 0, 0, 0))
    for i, o in enumerate(ORIGINS):
        sheet.paste(_page(o, 101 + i * 17), (200 * i, 0))
    out('gui/lore_pages', sheet)
    # the rollers a scroll's sheet is wound on
    rollers = Image.new('RGBA', (256, 96), (0, 0, 0, 0))
    rod = {'creator': ('#f2ead6', '#d9a72c'), 'pillager': ('#7a4e2c', '#c7713f'), 'cultist': ('#d8d2c0', '#1f8a8a'),
           'ocean': ('#c0ccd4', '#f0d8c8'), 'soul': ('#24306a', '#5ff0ff')}
    for i, o in enumerate(ORIGINS):
        body, knob = hx(rod[o][0]), hx(rod[o][1])
        y0 = i * 16
        for x in range(10, 198):
            for y in range(4, 12):
                t = (y - 4) / 7.0
                c = lighten(body, 0.25) if t < 0.2 else (body if t < 0.65 else darken(body, 0.25))
                if (x * 3 + y) % 17 == 0:
                    c = darken(c, 0.08)
                put(rollers, x, y0 + y, c)
        for x0 in (0, 198):
            for x in range(10):
                for y in range(1, 15):
                    if (x in (0, 9) and y in (1, 14)):
                        continue
                    t = (y - 1) / 13.0
                    c = lighten(knob, 0.3) if t < 0.25 else (knob if t < 0.7 else darken(knob, 0.3))
                    if x == 0 or x == 9:
                        c = darken(c, 0.2)
                    put(rollers, x0 + x, y0 + y, c)
    out('gui/lore_rollers', rollers)


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
    tones, trim, trim_l, trim_d = COVERS[o]
    img = material(16, 16, [hx(c) for c in tones], 400 + ORIGINS.index(o), cell=2, grain=0.1)
    px = img.load()
    for x in range(16):
        px[x, 0] = darken(px[x, 0], 0.3)
        px[x, 15] = darken(px[x, 15], 0.3)
    for y in (4, 11):          # raised bands on the spine
        for x in range(16):
            px[x, y] = hx(trim)
            px[x, y + 1] = hx(trim_d)
    return img


def _trim(o):
    tones, trim, trim_l, trim_d = COVERS[o]
    return material(16, 16, [hx(trim_d), hx(trim), hx(trim), hx(trim_l)], 500 + ORIGINS.index(o), cell=2, grain=0.1)


def _pages_tex():
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    rnd = random.Random(9)
    for y in range(16):
        for x in range(16):
            c = hx('#efe4c6') if y % 2 else hx('#e2d3ae')
            if rnd.random() < 0.08:
                c = hx('#d6c39a')
            px[x, y] = c
    return img


def _book_item(o):
    """The tome lying at an angle, vanilla book style: cover, gilt edge, pages."""
    tones, trim, trim_l, trim_d = COVERS[o]
    rows = [
        '................',
        '....cccccccccc..',
        '...cCCCCCCCCCcp.',
        '..cCCttCCCCttCpP',
        '..cCtCCCCCCCtcpP',
        '.cCCCCCeeeCCCcpP',
        '.cCCCCeEEEeCCcpP',
        '.cCCCCeEEEeCCcpP',
        '.cCCCCCeeeCCCcpP',
        'cCtCCCCCCCCCtcpP',
        'cCCttCCCCCCttcpP',
        'ccccccccccccccpP',
        '.ppppppppppppppP',
        '..PPPPPPPPPPPPP.',
        '................',
        '................',
    ]
    c0, c1, c2, c3 = (hx(c) for c in tones)
    pal = {'c': darken(c0, 0.3), 'C': c2, 't': hx(trim), 'e': hx(trim), 'E': hx(trim_l), 'p': hx('#efe4c6'), 'P': hx('#bfae88')}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r[:16]):
            if ch in pal:
                col = pal[ch]
                if ch == 'C' and (x + y * 3) % 7 == 0:
                    col = c3
                if ch == 'C' and (x * 5 + y) % 9 == 0:
                    col = c1
                img.putpixel((x, y), col)
    if o == 'cultist':
        for x, y in ((9, 9), (10, 10), (11, 9), (10, 11), (12, 10)):
            img.putpixel((x, y), hx('#0c3a3e'))
        img.putpixel((10, 10), hx('#4ff0e6'))
    if o == 'soul':
        for x, y in ((3, 6), (12, 4), (4, 10)):
            img.putpixel((x, y), hx('#5ff0ff'))
    return img


SCROLL_ROD = {'creator': ('#f2ead6', '#d9a72c', '#9a7015'), 'pillager': ('#8a5634', '#c7713f', '#5a3420'),
              'cultist': ('#d8d2c0', '#1f8a8a', '#0c3a3e'), 'ocean': ('#c0ccd4', '#f0d8c8', '#8a9aa6'),
              'soul': ('#2a3870', '#5ff0ff', '#121838')}
SCROLL_PAPER = {'creator': '#f5eedc', 'pillager': '#d2ae7a', 'cultist': '#b4bdb0', 'ocean': '#e2ecee', 'soul': '#1c2554'}


def _scroll_item(o):
    """A Lore Scroll held open between its two rollers, a few lines of writing, the seal on the lower roll."""
    rod, knob, knob_d = (hx(c) for c in SCROLL_ROD[o])
    paper = hx(SCROLL_PAPER[o])
    ink = hx('#5a4630') if o != 'soul' else hx('#7fdcff')
    seal = {'creator': '#c03a3a', 'pillager': '#7a2a1a', 'cultist': '#1f8a8a', 'ocean': '#e8707a', 'soul': '#5ff0ff'}[o]
    rows = [
        '................',
        '.K............K.',
        '.KRRRRRRRRRRRRK.',
        '.Krrrrrrrrrrrrk.',
        '..PPPPPPPPPPPS..',
        '..PIIIIIIIIPPS..',
        '..PPPPPPPPPPPS..',
        '..PIIIIIIPPPPS..',
        '..PPPPPPPPPPPS..',
        '..PIIIIIIIIIPS..',
        '..PPPPPPPPPPPS..',
        '..PIIIIIPPPPPS..',
        '..PPPPPPPPPWWS..',
        '.KRRRRRRRRRWwRK.',
        '.Krrrrrrrrrrrrk.',
        '.k............k.',
    ]
    pal = {'K': knob, 'k': knob_d, 'R': lighten(rod, 0.15), 'r': darken(rod, 0.2), 'P': paper, 'S': darken(paper, 0.18),
           'I': ink, 'W': hx(seal), 'w': lighten(hx(seal), 0.3)}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in pal:
                c = pal[ch]
                if ch == 'P' and (x * 5 + y * 3) % 11 == 0:
                    c = darken(paper, 0.07)
                img.putpixel((x, y), c)
    return img


def _scroll_3d(o):
    """32x32 zones (in 16-unit uv): roll [0,0,16,2], knob [0,2,2,4], sheet [0,4,12,16], seal [12,4,14,6]."""
    rod, knob, knob_d = (hx(c) for c in SCROLL_ROD[o])
    paper = hx(SCROLL_PAPER[o])
    ink = hx('#4a3a28') if o != 'soul' else hx('#8fe8ff')
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(0, 4):
        for x in range(32):
            c = paper if y in (1, 2) else darken(paper, 0.15)
            if x % 9 == 0:
                c = darken(c, 0.1)
            px[x, y] = c
    for y in range(4, 8):
        for x in range(4):
            px[x, y] = knob if y in (5, 6) else knob_d
    for y in range(8, 32):
        for x in range(24):
            c = paper if (x * 3 + y) % 11 else darken(paper, 0.05)
            px[x, y] = c
    for y in range(11, 30, 3):
        for x in range(3, 21):
            if (x * 5 + y * 3) % 6:
                px[x, y] = ink
    seal = {'creator': '#c03a3a', 'pillager': '#7a2a1a', 'cultist': '#1f8a8a', 'ocean': '#e8707a', 'soul': '#5ff0ff'}[o]
    for y in range(8, 12):
        for x in range(24, 28):
            px[x, y] = lighten(hx(seal), 0.2) if (x, y) == (25, 9) else hx(seal)
    return img


def _knowledge_item():
    """The Knowledge Book: a thick navy tome with gilt corners, a gold clasp and a gold sift star."""
    rows = [
        '................',
        '..cccccccccccc..',
        '.cLLLLLLLLLLLLp.',
        '.cLggLLLLLLggLpP',
        '.cLgLLLLLLLLgLpP',
        '.cLLLLLsLLLLLLpP',
        '.cLLLLsSsLLLLGpP',
        '.cLLLsSWSsLLLGpP',
        '.cLLLLsSsLLLLGpP',
        '.cLLLLLsLLLLLLpP',
        '.cLgLLLLLLLLgLpP',
        '.cLggLLLLLLggLpP',
        '.cccccccccccccpP',
        '..pppppppppppppP',
        '...PPPPPPPPPPPP.',
        '................',
    ]
    pal = {'c': hx('#121a2c'), 'L': hx('#2a375a'), 'g': hx('#d9a72c'), 'G': hx('#f2cc5a'), 's': hx('#d9a72c'), 'S': hx('#f2cc5a'),
           'W': hx('#fff2c0'), 'p': hx('#efe4c6'), 'P': hx('#bfae88')}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in pal:
                col = pal[ch]
                if ch == 'L' and (x * 3 + y * 5) % 7 == 0:
                    col = hx('#313f66')
                if ch == 'L' and (x + y * 2) % 9 == 0:
                    col = hx('#243050')
                img.putpixel((x, y), col)
    return img


# ============================================================================ Music Sheet art

SHEET_W, SHEET_H = 176, 112
SHEET_THEME = {
    'offering': ('#d9a72c', 'chimes'), 'nib': ('#e07ab8', 'butterflies'), 'golem': ('#2aa8a4', 'lamps'),
    'crystal': ('#8f86e0', 'crystals'), 'whale': ('#4fb2d8', 'waves'), 'lullaby': ('#a88ad8', 'moons'), 'aurora': ('#e070c8', 'rays'),
}
SONG_INSTRUMENT_ITEM = {'chimes': 'wind_chimes', 'strings': 'star_lute', 'flute': 'crane_flute', 'drum': 'conga_drum', 'prism': 'prism_harp',
                        'any': 'guitar'}
SONG_CREATURE_MODEL = {'offering': ('enchoer', 'enchoer'), 'nib': ('nib', 'nib'), 'golem': ('soul_golem', 'soul_golem'),
                       'crystal': ('caravan', 'caravan_amber'), 'whale': ('sky_whale', 'sky_whale'), 'lullaby': ('gobbler', 'gobbler')}
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
        out(f'item/{o}_lore_book', _book_item(o))
        out(f'item/{o}_lore_scroll', _scroll_item(o))
        out(f'item/{o}_lore_scroll_3d', _scroll_3d(o))
    out('block/lore_book_pages', _pages_tex())
    out('item/knowledge_book', _knowledge_item())
    out('item/mini_creator_spawn_egg', __import__('mini_creator').spawn_egg())
    sheets(out, gen_textures.TEX)
