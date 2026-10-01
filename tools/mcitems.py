"""Vanilla-style item sprites: silhouettes that follow the base game's proportions, shaded by rule.

Every Mojang tool is a 2-pixel stick running diagonally from the bottom-left corner with the head
in the top-right, outlined in the material's darkest tone, lit from the top-left. Rather than
painting each sprite pixel by pixel, the silhouettes here are drawn as masks (tools in diagonal
coordinates, armour as ASCII) and the shading is applied by one rule, so the whole set is
consistent:

* an edge pixel (touching transparency) gets the outline tone,
* an inner pixel whose upper or left neighbour is an edge is lit, one whose lower or right
  neighbour is an edge is shaded, everything else is the base tone,
* an inner pixel lit from both above and the left gets the highlight.
"""
from texlib import Tex, hx

# vanilla stick browns: outline, dark, light
STICK = (hx('#2e2013'), hx('#5a3e22'), hx('#86602f'))

EMPTY = '.'


def blank():
    return [[EMPTY] * 16 for _ in range(16)]


def put_uv(g, u, v, ch):
    """Diagonal coordinates: u runs along the bottom-left to top-right diagonal, v across it."""
    if (u + v) % 2:
        return
    x, y = (u + v) // 2, (v - u) // 2
    if 0 <= x < 16 and 0 <= y < 16:
        g[y][x] = ch


def shade(g, pal, accent=None):
    """Applies the shading rule to a mask; returns a Tex. pal: outline, dark, base, light, highlight."""
    t = Tex()

    def solid(x, y, kinds='#g'):
        return 0 <= x < 16 and 0 <= y < 16 and g[y][x] in kinds

    def edge(x, y):
        return solid(x, y) and not all(solid(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    for y in range(16):
        for x in range(16):
            ch = g[y][x]
            if ch == EMPTY:
                continue
            if ch == 'h':
                t.set(x, y, STICK[2])
                continue
            if ch == 'H':
                t.set(x, y, STICK[1] if any(g[y + dy][x + dx] == 'h' for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                                            if 0 <= x + dx < 16 and 0 <= y + dy < 16) else STICK[0])
                continue
            if ch == 'g' and accent:
                t.set(x, y, accent)
                continue
            if edge(x, y):
                t.set(x, y, pal[0])
                continue
            up, left = edge(x, y - 1) or not solid(x, y - 1), edge(x - 1, y) or not solid(x - 1, y)
            down, right = edge(x, y + 1) or not solid(x, y + 1), edge(x + 1, y) or not solid(x + 1, y)
            if up and left:
                c = pal[4]
            elif up or left:
                c = pal[3]
            elif down or right:
                c = pal[1]
            else:
                c = pal[2]
            t.set(x, y, c)
    return t


# ------------------------------------------------------------------ tools
# '#' material, 'h' lit side of the stick, 'H' dark side of the stick.

def handle(g, top_x, bottom_x=1):
    """The vanilla stick: lit pixels on x + y = 15, dark ones on x + y = 16."""
    for x in range(bottom_x, top_x + 1):
        if g[15 - x][x] == EMPTY:
            g[15 - x][x] = 'h'
        if 16 - x <= 15 and g[16 - x][x] == EMPTY:
            g[16 - x][x] = 'H'
    return g


def mirrored(half):
    """Completes a shape symmetric about the handle diagonal: (x, y) -> (15 - y, 15 - x)."""
    g = blank()
    for y, row in enumerate(half):
        for x, ch in enumerate(row):
            if ch != EMPTY:
                g[y][x] = ch
                g[15 - x][15 - y] = ch
    return g


SWORD = [
    '...............#',
    '.............###',
    '............###.',
    '...........###..',
    '..........###...',
    '.........###....',
    '........###.....',
    '..##...###......',
    '...##.###.......',
    '....####........',
    '....###.........',
    '...hH###........',
    '..hH...##.......',
    '##H.............',
    '##..............',
    '................',
]

PICKAXE_HALF = [
    '......#####.....',
    '....#########...',
    '...##########...',
    '..####..........',
    '..###...........',
    '..##............',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]

AXE = [
    '................',
    '.......####.....',
    '......######....',
    '.....########...',
    '.....#########..',
    '......####.###..',
    '.......##..##...',
    '...........#....',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]

SHOVEL = [
    '................',
    '...........###..',
    '..........#####.',
    '.........######.',
    '.........######.',
    '..........####..',
    '...........##...',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]

HOE = [
    '................',
    '.......######...',
    '......########..',
    '......###..###..',
    '...........###..',
    '............#...',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]

SPEAR = [
    '..............##',
    '............####',
    '...........####.',
    '..........#####.',
    '.........####...',
    '........#.##....',
    '.......#..#.....',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
]


def sword():
    return mask(SWORD)


def pickaxe():
    return handle(mirrored(PICKAXE_HALF), 12)


def axe():
    return handle(mask(AXE), 10)


def shovel():
    return handle(mask(SHOVEL), 10)


def hoe():
    return handle(mask(HOE), 11)


def spear():
    return handle(mask(SPEAR), 9, 0)


TOOLS = {'sword': sword, 'pickaxe': pickaxe, 'axe': axe, 'shovel': shovel, 'hoe': hoe, 'spear': spear}

# ------------------------------------------------------------------ armour (front views)

HELMET = [
    '................',
    '................',
    '................',
    '....########....',
    '...##########...',
    '..############..',
    '..############..',
    '..####....####..',
    '..###......###..',
    '..###......###..',
    '..###......###..',
    '................',
    '................',
    '................',
    '................',
    '................',
]
CHESTPLATE = [
    '................',
    '..####....####..',
    '.######..######.',
    '.##############.',
    '.##############.',
    '..############..',
    '...##########...',
    '...##########...',
    '...##########...',
    '...##########...',
    '...##########...',
    '...####..####...',
    '...##########...',
    '................',
    '................',
    '................',
]
LEGGINGS = [
    '................',
    '................',
    '...##########...',
    '...##########...',
    '...##########...',
    '...####..####...',
    '...####..####...',
    '...####..####...',
    '...####..####...',
    '...####..####...',
    '...####..####...',
    '...###....###...',
    '................',
    '................',
    '................',
    '................',
]
BOOTS = [
    '................',
    '................',
    '................',
    '..####....####..',
    '..####....####..',
    '..####....####..',
    '..####....####..',
    '..####....####..',
    '.#####....#####.',
    '.#####....#####.',
    '.#####....#####.',
    '................',
    '................',
    '................',
    '................',
    '................',
]
ARMOR = {'helmet': HELMET, 'chestplate': CHESTPLATE, 'leggings': LEGGINGS, 'boots': BOOTS}


def mask(rows):
    return [list(r) for r in rows]
