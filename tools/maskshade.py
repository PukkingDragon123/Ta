"""The rule-based mask shader once in mcitems.py (removed with the old item pipeline), kept for the Cave Jungle item art.

An edge pixel gets the outline tone; an inner pixel lit from above/left is light, shaded from below/right is dark; lit from
both above and left gets the highlight. pal: outline, dark, base, light, highlight."""
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
