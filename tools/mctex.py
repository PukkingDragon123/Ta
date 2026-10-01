"""Vanilla-style block texture painters.

Mojang's block textures are built from a flat mid tone plus a handful of hand-sized clusters of a
lighter and a darker tone, with crisp bevels on bricks and planks and no smooth gradients or
per-pixel noise. These painters follow the same rules so every block in The Sift reads like part
of the base game. All textures tile seamlessly (clusters wrap around the edges).

Palettes are ramps from darkest (index 0) to lightest.
"""
import random

from texlib import Tex

# small cluster shapes, the building blocks of every texture (offsets from an anchor pixel)
BLOBS = [
    ((0, 0), (1, 0)),
    ((0, 0), (1, 0), (2, 0)),
    ((0, 0), (1, 0), (0, 1)),
    ((0, 0), (1, 0), (1, 1)),
    ((0, 0), (1, 0), (2, 0), (1, 1)),
    ((0, 0), (1, 0), (2, 0), (3, 0), (1, 1), (2, 1)),
    ((0, 0), (1, 0), (0, 1), (1, 1)),
    ((0, 0), (1, 0), (2, 0), (2, 1), (3, 1)),
]


def fill(pal_c, w=16, h=16):
    t = Tex(w, h)
    t.a[:, :] = pal_c
    return t


def blob(t, x, y, c, shape, wrap=True):
    for dx, dy in shape:
        if wrap:
            t.set((x + dx) % t.w, (y + dy) % t.h, c)
        else:
            t.set(x + dx, y + dy, c)


def stone(pal, seed, light=6, dark=7, core=0.35, flecks=None):
    """Vanilla stone: mid-tone base, lighter and darker chunks, a few deep cores in the dark ones."""
    rnd = random.Random(seed)
    t = fill(pal[2])
    for _ in range(dark):
        x, y = rnd.randrange(16), rnd.randrange(16)
        sh = rnd.choice(BLOBS)
        blob(t, x, y, pal[1], sh)
        if rnd.random() < core:
            t.set((x + 1) % 16, y, pal[0])
    for _ in range(light):
        x, y = rnd.randrange(16), rnd.randrange(16)
        sh = rnd.choice(BLOBS)
        blob(t, x, y, pal[3], sh)
        if len(pal) > 4 and rnd.random() < core:
            t.set(x, y, pal[4])
    if flecks:
        for _ in range(flecks[1]):
            t.set(rnd.randrange(16), rnd.randrange(16), flecks[0])
    return t


def grain(pal, seed, light=34, dark=30, bright=6, deep=3):
    """Sand/soil: fine single and double-pixel speckle on a flat base (like vanilla sand and dirt)."""
    rnd = random.Random(seed)
    t = fill(pal[2])
    dot = ((0, 0),)
    for n, c, shapes in ((dark, pal[1], [dot, dot, dot, BLOBS[0]]), (light, pal[3], [dot, dot, dot, BLOBS[0]]),
                         (bright, pal[min(4, len(pal) - 1)], [dot]), (deep, pal[0], [dot])):
        for _ in range(n):
            blob(t, rnd.randrange(16), rnd.randrange(16), c, rnd.choice(shapes))
    return t


def soil(pal, seed):
    """Dirt: speckle plus a few darker clods."""
    t = grain(pal, seed, light=22, dark=24, bright=4, deep=0)
    rnd = random.Random(seed + 1)
    for _ in range(6):
        blob(t, rnd.randrange(16), rnd.randrange(16), pal[0], rnd.choice(BLOBS[:4]))
    return t


def bricks(pal, mortar, seed, rows=4, width=8, offset=4):
    """Bricks with a lit top/left bevel, shaded bottom/right edge and one or two clusters each."""
    rnd = random.Random(seed)
    t = fill(mortar)
    rh = 16 // rows
    for r in range(rows):
        y0 = r * rh
        shift = (r % 2) * offset
        for b in range(16 // width + 1):
            x0 = b * width - shift
            for yy in range(rh - 1):
                for xx in range(width - 1):
                    x = (x0 + xx) % 16
                    y = y0 + yy
                    c = pal[2]
                    if yy == 0 or xx == 0:
                        c = pal[3]
                    if yy == rh - 2 or xx == width - 2:
                        c = pal[1]
                    if yy == 0 and xx == 0:
                        c = pal[min(4, len(pal) - 1)]
                    t.set(x, y, c)
            # a cluster or two inside each brick
            for _ in range(rnd.randrange(1, 3)):
                xx, yy = rnd.randrange(1, width - 3), rnd.randrange(1, max(2, rh - 2))
                c = pal[1] if rnd.random() < 0.55 else pal[3]
                blob(t, (x0 + xx) % 16, y0 + yy, c, rnd.choice(BLOBS[:2]), wrap=True)
    return t


def tiles(pal, mortar, seed, n=2):
    """Square tiles, each with a bevel."""
    rnd = random.Random(seed)
    t = fill(mortar)
    s = 16 // n
    for ty in range(n):
        for tx in range(n):
            for yy in range(s - 1):
                for xx in range(s - 1):
                    c = pal[2]
                    if yy == 0 or xx == 0:
                        c = pal[3]
                    if yy == s - 2 or xx == s - 2:
                        c = pal[1]
                    t.set(tx * s + xx, ty * s + yy, c)
            if s >= 6:
                blob(t, tx * s + rnd.randrange(2, s - 3), ty * s + rnd.randrange(2, s - 2), pal[1], BLOBS[0])
    return t


def polished(pal, seed, inner=True):
    """Polished stone: a smooth face inside a two-tone bevel frame."""
    rnd = random.Random(seed)
    t = fill(pal[2])
    for i in range(16):
        t.set(i, 0, pal[3]); t.set(0, i, pal[3])
        t.set(i, 15, pal[1]); t.set(15, i, pal[1])
    if inner:
        for i in range(2, 14):
            t.set(i, 2, pal[1]); t.set(2, i, pal[1])
            t.set(i, 13, pal[3]); t.set(13, i, pal[3])
    for _ in range(3):
        blob(t, rnd.randrange(4, 11), rnd.randrange(4, 11), pal[3] if rnd.random() < 0.5 else pal[1], BLOBS[0], wrap=False)
    return t


def cobbled(pal, mortar, seed):
    """Cobblestone: rounded stones (lit top-left, shaded bottom-right) packed between dark gaps."""
    rnd = random.Random(seed)
    t = fill(mortar)
    # stones on a jittered grid, painted as small rounded rectangles that wrap
    for gy in range(4):
        for gx in range(4):
            w, h = rnd.randrange(3, 6), rnd.randrange(3, 5)
            x0 = gx * 4 + rnd.randrange(-1, 2) + (2 if gy % 2 else 0)
            y0 = gy * 4 + rnd.randrange(-1, 1)
            base = pal[rnd.choice((2, 2, 3, 1))]
            for yy in range(h):
                for xx in range(w):
                    if (xx in (0, w - 1)) and (yy in (0, h - 1)):
                        continue  # round the corners
                    c = base
                    if yy == 0 or xx == 0:
                        c = pal[min(len(pal) - 1, pal.index(base) + 1)]
                    if yy == h - 1 or xx == w - 1:
                        c = pal[max(0, pal.index(base) - 1)]
                    t.set((x0 + xx) % 16, (y0 + yy) % 16, c)
    return t


def planks(pal, seed, boards=4):
    """Planks: boards with a dark gap line under each, grain dashes and offset end seams."""
    rnd = random.Random(seed)
    t = fill(pal[2])
    bh = 16 // boards
    for b in range(boards):
        y0 = b * bh
        for x in range(16):
            t.set(x, y0 + bh - 1, pal[0])
            t.set(x, y0, pal[3] if x % 7 else pal[2])
        seam = (rnd.randrange(16) + b * 5) % 16
        for yy in range(bh - 1):
            t.set(seam, y0 + yy, pal[0] if yy else pal[1])
        for _ in range(2):
            gx, gy = rnd.randrange(16), y0 + rnd.randrange(1, bh - 1)
            for k in range(rnd.randrange(2, 5)):
                t.set((gx + k) % 16, gy, pal[1])
    return t


def log_side(pal, seed):
    """Bark: vertical ridges and dark grooves of varying length."""
    rnd = random.Random(seed)
    t = fill(pal[2])
    for x in range(16):
        kind = rnd.random()
        if kind < 0.3:
            # groove
            y, length = rnd.randrange(16), rnd.randrange(5, 12)
            for k in range(length):
                t.set(x, (y + k) % 16, pal[0] if k and k < length - 1 else pal[1])
        elif kind < 0.55:
            y, length = rnd.randrange(16), rnd.randrange(3, 8)
            for k in range(length):
                t.set(x, (y + k) % 16, pal[3])
    for _ in range(4):
        blob(t, rnd.randrange(16), rnd.randrange(16), pal[1], ((0, 0), (0, 1)))
    return t


def log_top(bark, wood):
    """End grain: a bark rim around concentric growth rings."""
    t = Tex()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                t.set(x, y, bark[2] if (x + y) % 3 else bark[1])
            else:
                ring = int(d + 0.5)
                t.set(x, y, wood[1] if ring in (1, 4) else wood[3] if ring == 6 else wood[2])
    t.set(7, 7, wood[1]); t.set(8, 8, wood[1])
    return t


def leaves(pal, seed, blossom=None, holes=0.3):
    """Leaves: overlapping little leaf clumps (lit top-left, shaded bottom-right) over a dark
    undergrowth with a few see-through gaps."""
    rnd = random.Random(seed)
    t = Tex()
    for y in range(16):
        for x in range(16):
            if rnd.random() >= holes:
                t.set(x, y, pal[0])
    clump = [(0, -1), (-1, 0), (0, 0), (1, 0), (0, 1), (1, 1)]
    for gy in range(0, 16, 3):
        for gx in range(0, 16, 3):
            cx, cy = gx + rnd.randrange(0, 3), gy + rnd.randrange(0, 3)
            body = pal[rnd.choice((2, 2, 3))]
            for dx, dy in clump:
                t.set((cx + dx) % 16, (cy + dy) % 16, body)
            t.set((cx - 1) % 16, cy % 16, pal[min(len(pal) - 1, pal.index(body) + 1)])
            t.set(cx % 16, (cy - 1) % 16, pal[min(len(pal) - 1, pal.index(body) + 2)])
            t.set((cx + 1) % 16, (cy + 1) % 16, pal[1])
    if blossom:
        for _ in range(5):
            x, y = rnd.randrange(16), rnd.randrange(16)
            t.set(x, y, blossom)
    return t


def ore(base, ore_pal, seed, count=5, outline=None):
    """Ore veins: little mineral clusters (highlight, body, shade) with a dark rim in the stone."""
    rnd = random.Random(seed)
    t = base.copy()
    shapes = [
        [(0, 0, 3), (1, 0, 2), (0, 1, 2), (1, 1, 1)],
        [(0, 0, 3), (1, 0, 2), (2, 0, 1), (1, 1, 1)],
        [(0, 0, 2), (0, 1, 3), (1, 1, 2), (1, 2, 1)],
        [(1, 0, 3), (0, 1, 2), (1, 1, 2), (2, 1, 1), (1, 2, 1)],
    ]
    spots = []
    while len(spots) < count:
        x, y = rnd.randrange(1, 13), rnd.randrange(1, 13)
        if all(abs(x - a) + abs(y - b) > 4 for a, b in spots):
            spots.append((x, y))
    for x, y in spots:
        sh = rnd.choice(shapes)
        if outline is not None:
            for dx, dy, _ in sh:
                t.set(x + dx + 1, y + dy + 1, outline)
        for dx, dy, k in sh:
            t.set(x + dx, y + dy, ore_pal[k])
    return t


def grass_side(top_pal, soil_tex, seed):
    """Soil with a turf cap that drips down in uneven little tufts."""
    rnd = random.Random(seed)
    t = soil_tex.copy()
    for x in range(16):
        depth = rnd.choice((2, 3, 3, 4)) + (1 if rnd.random() < 0.2 else 0)
        for y in range(depth):
            c = top_pal[3] if y == 0 else top_pal[2]
            if y == depth - 1:
                c = top_pal[1]
            t.set(x, y, c)
    for _ in range(3):
        t.set(rnd.randrange(16), 1, top_pal[4] if len(top_pal) > 4 else top_pal[3])
    return t
