"""Pixel-art painting helpers for The Sift's block/item textures."""
import math
import random

import numpy as np
from PIL import Image


def hx(h):
    h = h.lstrip('#')
    a = int(h[6:8], 16) if len(h) == 8 else 255
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def ramp(*hexes):
    return [hx(h) for h in hexes]


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def lighten(c, f):
    return tuple(min(255, int(c[i] + (255 - c[i]) * f)) if i < 3 else c[3] for i in range(4))


def darken(c, f):
    return tuple(int(c[i] * (1 - f)) if i < 3 else c[3] for i in range(4))


def with_alpha(c, a):
    return (c[0], c[1], c[2], a)


class Tex:
    """16x16 (or any size) RGBA canvas with array access."""

    def __init__(self, w=16, h=16, fill=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.a = np.zeros((h, w, 4), dtype=np.uint8)
        self.a[:, :] = fill

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.a[y, x] = c

    def get(self, x, y):
        return tuple(int(v) for v in self.a[y % self.h, x % self.w])

    def img(self):
        return Image.fromarray(self.a, 'RGBA')

    def save(self, path):
        self.img().save(path)

    def blit(self, other, ox=0, oy=0):
        for y in range(other.h):
            for x in range(other.w):
                c = other.a[y, x]
                if c[3] > 0:
                    self.set(ox + x, oy + y, tuple(int(v) for v in c))

    def copy(self):
        t = Tex(self.w, self.h)
        t.a = self.a.copy()
        return t


# ----------------------------------------------------------------------------- noise


def value_noise(w, h, cell, seed, wrap=True):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    grid = rng.random((gh + 1, gw + 1))
    if wrap:
        grid[gh, :] = grid[0, :]
        grid[:, gw] = grid[:, 0]
    out = np.zeros((h, w))
    for y in range(h):
        for x in range(w):
            gx, gy = x / cell, y / cell
            x0, y0 = int(gx) % gw, int(gy) % gh
            tx, ty = gx - int(gx), gy - int(gy)
            tx = tx * tx * (3 - 2 * tx)
            ty = ty * ty * (3 - 2 * ty)
            a = grid[y0, x0] * (1 - tx) + grid[y0, x0 + 1] * tx
            b = grid[y0 + 1, x0] * (1 - tx) + grid[y0 + 1, x0 + 1] * tx
            out[y, x] = a * (1 - ty) + b * ty
    return out


def fbm(w, h, seed, cells=(8, 4, 2), weights=(0.5, 0.3, 0.2)):
    n = np.zeros((h, w))
    for i, (c, wt) in enumerate(zip(cells, weights)):
        n += value_noise(w, h, c, seed + i * 101) * wt
    n -= n.min()
    if n.max() > 0:
        n /= n.max()
    return n


def quantize(t: Tex, field, pal, mask=None, bias=0.0):
    """Map field (0..1) onto palette indices."""
    k = len(pal)
    for y in range(t.h):
        for x in range(t.w):
            if mask is not None and not mask[y, x]:
                continue
            v = min(0.999, max(0.0, field[y, x] + bias))
            t.set(x, y, pal[int(v * k)])


# ----------------------------------------------------------------------------- materials


def stone(pal, seed, flecks=None, cells=(8, 4, 2)):
    t = Tex()
    n = fbm(16, 16, seed, cells)
    rnd = random.Random(seed)
    n = n * 0.8 + np.array([[rnd.random() * 0.2 for _ in range(16)] for _ in range(16)])
    quantize(t, n, pal)
    if flecks:
        for _ in range(flecks[1]):
            t.set(rnd.randrange(16), rnd.randrange(16), flecks[0])
    return t


def cobbled(pal, seed, mortar=None, cells=9):
    rnd = random.Random(seed)
    pts = [(rnd.uniform(0, 16), rnd.uniform(0, 16)) for _ in range(cells)]
    shade = [rnd.randrange(1, len(pal) - 1) for _ in pts]
    t = Tex()
    mortar = mortar or pal[0]

    def nearest(x, y):
        best = []
        for i, (px, py) in enumerate(pts):
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    d = (x - px - ox) ** 2 + (y - py - oy) ** 2
                    best.append((d, i, px + ox, py + oy))
        best.sort()
        return best[0], best[1]

    for y in range(16):
        for x in range(16):
            (d0, i0, cx, cy), (d1, _, _, _) = nearest(x + 0.5, y + 0.5)
            edge = math.sqrt(d1) - math.sqrt(d0)
            if edge < 0.9:
                t.set(x, y, mortar)
                continue
            s = shade[i0]
            # highlight towards the top-left of each stone, shadow bottom-right
            if (x + 0.5 - cx) + (y + 0.5 - cy) < -2.2:
                s = min(len(pal) - 1, s + 1)
            elif (x + 0.5 - cx) + (y + 0.5 - cy) > 2.2:
                s = max(1, s - 1)
            if rnd.random() < 0.08:
                s = max(1, min(len(pal) - 1, s + rnd.choice((-1, 1))))
            t.set(x, y, pal[s])
    return t


def bricks(pal, mortar, seed, rows=4, width=8, stagger=True, bevel=True, h=None):
    """pal: dark..light brick colours."""
    rnd = random.Random(seed)
    t = Tex()
    rh = h or 16 // rows
    for r in range(16 // rh):
        off = (width // 2) if (stagger and r % 2) else 0
        for bx in range(-1, 16 // width + 1):
            x0 = bx * width + off
            base = rnd.randrange(1, len(pal) - 1)
            for yy in range(rh):
                for xx in range(width):
                    x, y = x0 + xx, r * rh + yy
                    if not (0 <= x < 16):
                        continue
                    if yy == rh - 1 or xx == width - 1:
                        t.set(x, y, mortar)
                        continue
                    s = base
                    if bevel and (yy == 0 or xx == 0):
                        s = min(len(pal) - 1, s + 1)
                    elif bevel and (yy == rh - 2 or xx == width - 2):
                        s = max(0, s - 1)
                    if rnd.random() < 0.12:
                        s = max(0, min(len(pal) - 1, s + rnd.choice((-1, 1))))
                    t.set(x, y, pal[s])
    return t


def tiles(pal, mortar, seed, n=2):
    return bricks(pal, mortar, seed, rows=n, width=16 // n, stagger=False)


def polished(pal, seed, inner=True):
    t = stone(pal[1:-1] if len(pal) > 3 else pal, seed, cells=(8, 8, 4))
    L, D = pal[-1], pal[0]
    for i in range(16):
        t.set(i, 0, L)
        t.set(0, i, L)
        t.set(i, 15, D)
        t.set(15, i, D)
    if inner:
        for i in range(2, 14):
            t.set(i, 2, pal[1])
            t.set(2, i, pal[1])
            t.set(i, 13, pal[-2])
            t.set(13, i, pal[-2])
    return t


def draw_map(t: Tex, rows, keys, ox=0, oy=0):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in '. ':
                continue
            if ch == '_':
                t.set(ox + i, oy + j, (0, 0, 0, 0))
            else:
                t.set(ox + i, oy + j, keys[ch])
    return t


def outline_alpha(t: Tex, color, only_empty=True):
    """Add a 1px outline around opaque pixels."""
    src = t.a.copy()
    for y in range(t.h):
        for x in range(t.w):
            if src[y, x, 3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < t.w and 0 <= yy < t.h and src[yy, xx, 3] > 0:
                        t.set(x, y, color)
                        break
    return t


def shade_sprite(t: Tex, light=0.25, dark=0.3):
    """Mojang-style sprite shading: lighter top-left edges, darker bottom-right edges."""
    src = t.a.copy()
    for y in range(t.h):
        for x in range(t.w):
            if src[y, x, 3] == 0:
                continue
            c = tuple(int(v) for v in src[y, x])
            up = y == 0 or src[y - 1, x, 3] == 0
            left = x == 0 or src[y, x - 1, 3] == 0
            down = y == t.h - 1 or src[y + 1, x, 3] == 0
            right = x == t.w - 1 or src[y, x + 1, 3] == 0
            if down or right:
                t.set(x, y, darken(c, dark))
            elif up or left:
                t.set(x, y, lighten(c, light))
    return t


def mask_fill(t: Tex, mask_rows, pal, seed, field=None, keys=None):
    """Fill cells marked '#' in mask with palette by noise; other chars via keys."""
    rnd = random.Random(seed)
    n = field if field is not None else fbm(16, 16, seed, (4, 2, 1))
    for y, row in enumerate(mask_rows):
        for x, ch in enumerate(row):
            if ch == '#':
                v = min(0.999, n[y % n.shape[0], x % n.shape[1]] * 0.85 + rnd.random() * 0.15)
                t.set(x, y, pal[int(v * len(pal))])
            elif keys and ch in keys:
                t.set(x, y, keys[ch])
    return t


def iridescent(x, y, t=0.0, scale=0.25):
    """Chrome's shifting cyan/pink/pearl colour at a pixel."""
    v = (math.sin(x * scale + t) + math.sin(y * scale * 1.3 - t * 0.7) + math.sin((x + y) * scale * 0.7 + t * 1.3)) / 3.0
    v = (v + 1) / 2
    stops = [hx('#7fe8ff'), hx('#a8c8ff'), hx('#e7b6ff'), hx('#ff9fd8'), hx('#fff0fa'), hx('#9ff5f0')]
    f = v * (len(stops) - 1)
    i = int(f)
    return lerp(stops[i], stops[min(i + 1, len(stops) - 1)], f - i)
