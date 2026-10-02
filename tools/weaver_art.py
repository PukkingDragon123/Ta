"""The Weaver's things (E2), drawn at 16x16 in the items16.py style: Sculk String, the Guitar strung
with it, the Weaver's own Guitar, and the Musical Cobweb block. Called once from gen_textures.main()
after the item sprites, so these replace any older drawings of the same names."""
import math

from PIL import Image

from items16 import grid

CUTOUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}


def sculk_string():
    """A loose coil of sculk silk: dark teal thread with a glowing core, beaded with light."""
    rows = [
        '................',
        '..........a.....',
        '.........bcb....',
        '.........a.c....',
        '.........cba....',
        '....b.....ccb...',
        '...ac.......ca..',
        '..bc..abwb...cb.',
        '..a..bccccab..a.',
        '..cb.ca...cca.b.',
        '...cabcc...bc.a.',
        '....ccabwbac.bc.',
        '......ccccc.ac..',
        '........cbabc...',
        '.........ccc....',
        '................',
    ]
    pal = {'a': ('#29dfeb', '#062e37'), 'b': ('#b8fff8', '#062e37'), 'c': ('#0f6e7a', '#04161b'), 'w': '#ffffff'}
    return grid(rows, pal)


GUITAR_ROWS = [
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


def guitar():
    """An acoustic guitar of plain planks, strung with Sculk String that glints teal down the neck."""
    pal = {'a': ('#f4c070', '#4a2414'), 'b': ('#d8963e', '#4a2414'), 'c': ('#a8682a', '#4a2414'),
           'o': ('#3a1e14', '#4a2414'), 'B': ('#5a3420', '#4a2414'), 'N': ('#6a3e26', '#24120c'), 'h': ('#4a2a1a', '#24120c'),
           'p': ('#bbc39b', '#3a3a4a'), 'g': ('#5fd4dc', '#24120c')}
    return grid(GUITAR_ROWS, pal, ol=True)


def weaver_guitar():
    """The Weaver's Guitar: a body of black sculk hide bound in bone, a sound hole glowing like a
    sculk sensor, a neck of bone, and a headstock that is a little spider's head - two glowing eyes
    and bone legs for tuning pegs - with bright silk strings."""
    rows = [
        '...........l..l.',
        '..........lpHHl.',
        '.............HHp',
        '...........lgN.l',
        '...........gN...',
        '.......ab.gN....',
        '......aabgN.....',
        '.....aabgNc.....',
        '....aaGgbbc.....',
        '..aaaGoGbc......',
        '.aabGoGbc.......',
        '.aBbgGbc........',
        '.bbBbbcc........',
        '..bBBcc.........',
        '...ccc..........',
        '................',
    ]
    pal = {'a': ('#4a7480', '#04090c'), 'b': ('#2c4e59', '#04090c'), 'c': ('#173039', '#04090c'),
           'o': ('#9ffbff', '#04090c'), 'G': ('#0f6e7a', '#04090c'), 'B': ('#d1d6b6', '#04090c'),
           'N': ('#d1d6b6', '#3a4440'), 'H': ('#16222a', '#04090c'), 'p': ('#29dfeb', '#04090c'), 'l': ('#bbc39b', '#3a4440'),
           'g': ('#c8fffa', '#04090c')}
    return grid(rows, pal, ol=True)


def _line(px, x0, y0, x1, y1, c):
    """A one-pixel strand from (x0, y0) to (x1, y1) (Bresenham)."""
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        if px[x0, y0][3] < c[3]:
            px[x0, y0] = c
        if (x0, y0) == (x1, y1):
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def musical_cobweb():
    """An orb web of glowing sculk silk: eight spokes out to the corners and edges, two rings of
    straight strands strung between them like harp strings, bright knots where they cross and a
    glowing hub."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    spoke = (41, 190, 204, 200)
    ring = (127, 247, 255, 230)
    knot = (232, 255, 255, 255)
    c = 7
    dirs = [(1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1)]
    for dx, dy in dirs:
        _line(px, c, c, c + dx * 7, c + dy * 7, spoke)
    for r in (3, 6):
        pts = [(c + dx * (r if dx == 0 or dy == 0 else round(r * 0.72)), c + dy * (r if dx == 0 or dy == 0 else round(r * 0.72))) for dx, dy in dirs]
        for i in range(8):
            (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % 8]
            _line(px, x0, y0, x1, y1, ring)
        for (x, y) in pts:
            px[x, y] = knot
    for (x, y) in ((c, c), (c + 1, c), (c - 1, c), (c, c + 1), (c, c - 1)):
        px[x, y] = knot
    # strands caught on the far edge and corner, so it tiles as a web, not a sticker
    _line(px, 14, 14, 15, 15, spoke)
    _line(px, 14, 7, 15, 7, spoke)
    _line(px, 7, 14, 7, 15, spoke)
    return img


def textures(out):
    out('item/sculk_string', sculk_string())
    out('item/guitar', guitar())
    out('item/weaver_guitar', weaver_guitar())
    out('block/musical_cobweb', musical_cobweb(), CUTOUT)
