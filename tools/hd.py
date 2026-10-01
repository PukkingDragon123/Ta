"""High-resolution (32x) pixel-art helpers.

The Sift's art is painted at twice vanilla's texel density: 32 texels per block, 2 per model
unit. The rules stay Mojang's (flat tones, hand-sized clusters, a light from the top left,
crisp 1-texel outlines, no smooth gradients) - there is just room for finer detail: bevels one
texel wide, rope twists, rivets, grain, glints.

Block models with several parts use a 64x64 *sheet*: every face is painted at 2 texels per unit
into its own rectangle and the model samples it with UVs of (texels / 4).
"""
from __future__ import annotations

import math
import random

from texlib import Tex, hx, lerp

SHEET = 64          # sheet size in texels
UV_PER_TEXEL = 16 / SHEET


def uv(x, y, w, h):
    """Model UV rectangle for a texel rectangle of a 64x64 sheet."""
    k = UV_PER_TEXEL
    return [x * k, y * k, (x + w) * k, (y + h) * k]


def shift(pal, i, d):
    return pal[max(0, min(len(pal) - 1, i + d))]


# ----------------------------------------------------------------------------- tiny clusters

CLUSTERS = [
    ((0, 0), (1, 0)),
    ((0, 0), (1, 0), (2, 0)),
    ((0, 0), (0, 1)),
    ((0, 0), (1, 0), (0, 1)),
    ((0, 0), (1, 0), (1, 1)),
    ((0, 0), (1, 0), (2, 0), (1, 1)),
    ((0, 0), (1, 0), (1, 1), (2, 1)),
    ((0, 0), (1, 0), (2, 0), (3, 0), (1, 1), (2, 1)),
]


def stamp(t, x, y, c, shape, clip=None):
    for dx, dy in shape:
        xx, yy = x + dx, y + dy
        if clip and not (clip[0] <= xx < clip[0] + clip[2] and clip[1] <= yy < clip[1] + clip[3]):
            continue
        t.set(xx, yy, c)


def rect(t, x, y, w, h, c):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            t.set(xx, yy, c)


def line(t, x0, y0, x1, y1, c):
    """Bresenham line."""
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        t.set(x0, y0, c)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


# ----------------------------------------------------------------------------- materials

def staves(t, x0, y0, w, h, pal, seed, stave=6, top_shadow=2, bottom_shadow=2):
    """Vertical wooden staves: lit left edge, shaded right edge, dark seam, grain dashes, and a
    little ambient occlusion where the staves meet the hoops."""
    rnd = random.Random(seed)
    for xx in range(w):
        k = xx % stave
        for yy in range(h):
            if k == stave - 1:
                c = pal[0]
            elif k == 0:
                c = pal[3]
            elif k == stave - 2:
                c = pal[1]
            else:
                c = pal[2]
            t.set(x0 + xx, y0 + yy, c)
    # grain: short dark dashes and a few lit flecks inside each stave
    for s in range(0, w, stave):
        for _ in range(max(1, h // 5)):
            gx = x0 + s + rnd.randrange(1, max(2, stave - 2))
            gy = y0 + rnd.randrange(h)
            for k in range(rnd.choice((2, 3, 3, 4))):
                if gy + k < y0 + h and gx < x0 + w:
                    t.set(gx, gy + k, pal[1])
        if rnd.random() < 0.7:
            fx, fy = x0 + s + rnd.randrange(1, max(2, stave - 2)), y0 + rnd.randrange(h)
            if fx < x0 + w:
                t.set(fx, fy, pal[4] if len(pal) > 4 else pal[3])
    for yy in range(top_shadow):
        for xx in range(w):
            c = t.get(x0 + xx, y0 + yy)
            t.set(x0 + xx, y0 + yy, lerp(c, pal[0], 0.55 - yy * 0.25))
    for yy in range(bottom_shadow):
        for xx in range(w):
            c = t.get(x0 + xx, y0 + h - 1 - yy)
            t.set(x0 + xx, y0 + h - 1 - yy, lerp(c, pal[0], 0.45 - yy * 0.2))


def metal_band(t, x0, y0, w, h, pal, rivet=None, rivet_every=7, rivet_phase=3):
    """A metal hoop seen side-on: bright top edge, polished middle stripe, dark bottom lip and
    optional rivets (highlight + shadow pixel)."""
    for yy in range(h):
        if yy == 0:
            c = pal[4]
        elif yy == h - 1:
            c = pal[0]
        elif yy == 1:
            c = pal[3]
        elif yy == h - 2:
            c = pal[1]
        else:
            c = pal[2]
        for xx in range(w):
            t.set(x0 + xx, y0 + yy, c)
    # a few scuffs along the polished stripe
    for xx in range(2, w - 2, 9):
        t.set(x0 + xx, y0 + min(1, h - 1), pal[5] if len(pal) > 5 else pal[4])
    if rivet is not None and h >= 3:
        ry = y0 + h // 2
        for xx in range(rivet_phase, w - 1, rivet_every):
            t.set(x0 + xx, ry, rivet[1])
            t.set(x0 + xx + 1, ry, rivet[0])
            t.set(x0 + xx, ry - 1, rivet[2])


def rope(t, pts, pal, width=3):
    """A twisted rope ribbon through a list of points (mostly steep segments): `width` texels
    across, lit on the left, shaded on the right, with a twist mark stepping down it and a dark
    edge so it reads against whatever it crosses."""
    for (ax, ay), (bx, by) in zip(pts, pts[1:]):
        if ay > by:
            ax, ay, bx, by = bx, by, ax, ay
        for y in range(ay, by + 1):
            f = (y - ay) / max(1, by - ay)
            cx = int(round(ax + (bx - ax) * f))
            x0 = cx - width // 2
            for i in range(width):
                if i == 0:
                    c = pal[3]
                elif i == width - 1:
                    c = pal[1]
                else:
                    c = pal[2]
                if (y + i) % 3 == 0:
                    c = pal[1] if i else pal[2]
                t.set(x0 + i, y, c)
            t.set(x0 + width, y, pal[0])


def hide(t, x0, y0, w, h, pal, seed, round_=True):
    """Stretched drum hide: cream base, soft larger mottles, a darker rim where it wraps the
    shell (inset 1 texel) and a ring of tacks."""
    rnd = random.Random(seed)
    cx, cy = x0 + (w - 1) / 2, y0 + (h - 1) / 2
    for yy in range(h):
        for xx in range(w):
            t.set(x0 + xx, y0 + yy, pal[3])
    for _ in range(w * h // 18):
        stamp(t, x0 + rnd.randrange(w), y0 + rnd.randrange(h), pal[2] if rnd.random() < 0.6 else pal[4], rnd.choice(CLUSTERS[:6]),
              clip=(x0, y0, w, h))
    # a worn, slightly darker playing spot in the middle
    for yy in range(h):
        for xx in range(w):
            d = math.hypot(x0 + xx - cx, (y0 + yy - cy))
            if d < w * 0.2 and (xx + yy) % 2 == 0:
                t.set(x0 + xx, y0 + yy, pal[2])
    # rim: the hide pulled over the edge
    for yy in range(h):
        for xx in range(w):
            edge = min(xx, yy, w - 1 - xx, h - 1 - yy)
            if edge == 0:
                t.set(x0 + xx, y0 + yy, pal[0])
            elif edge == 1:
                t.set(x0 + xx, y0 + yy, pal[1])
    return t


def glint(t, x, y, c_hi, c_mid=None):
    """A 4-point sparkle: bright centre, dimmer arms."""
    t.set(x, y, c_hi)
    if c_mid:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            t.set(x + dx, y + dy, c_mid)


# ----------------------------------------------------------------------------- block preview

def preview_model(model, textures, size=(256, 256), yaw=35.0, pitch=28.0, scale=None, bg=(46, 50, 74, 255)):
    """Software render of a block model json (elements with from/to and per-face uv) for checking
    shapes without the game. `textures` maps texture variables (without '#') to PIL images."""
    from PIL import Image, ImageDraw
    img = Image.new('RGBA', size, bg)
    draw = ImageDraw.Draw(img)
    scale = scale or size[0] / 30.0
    yr, pr = math.radians(yaw), math.radians(pitch)
    quads = []

    def cam(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * math.cos(yr) - z * math.sin(yr), x * math.sin(yr) + z * math.cos(yr)
        y, z = y * math.cos(pr) + z * math.sin(pr), -y * math.sin(pr) + z * math.cos(pr)
        return (size[0] / 2 + x * scale, size[1] / 2 - y * scale, z)

    light = {'up': 1.0, 'north': 0.8, 'south': 0.8, 'east': 0.6, 'west': 0.6, 'down': 0.5}
    for e in model['elements']:
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        for name, f in e['faces'].items():
            tex = textures[f['texture'].lstrip('#')]
            px = tex.load()
            tw, th = tex.size
            u0, v0, u1, v1 = f.get('uv', [0, 0, 16, 16])
            # face corners: (origin, u axis, v axis) in model space, v pointing down the texture
            if name == 'north':
                o, du, dv = (x1, y1, z0), (x0 - x1, 0, 0), (0, y0 - y1, 0)
            elif name == 'south':
                o, du, dv = (x0, y1, z1), (x1 - x0, 0, 0), (0, y0 - y1, 0)
            elif name == 'west':
                o, du, dv = (x0, y1, z0), (0, 0, z1 - z0), (0, y0 - y1, 0)
            elif name == 'east':
                o, du, dv = (x1, y1, z1), (0, 0, z0 - z1), (0, y0 - y1, 0)
            elif name == 'up':
                o, du, dv = (x0, y1, z0), (x1 - x0, 0, 0), (0, 0, z1 - z0)
            else:
                o, du, dv = (x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)
            tu0, tv0 = u0 / 16 * tw, v0 / 16 * th
            tu1, tv1 = u1 / 16 * tw, v1 / 16 * th
            nu, nv = max(1, int(round(abs(tu1 - tu0)))), max(1, int(round(abs(tv1 - tv0))))
            for j in range(nv):
                for i in range(nu):
                    sx = int(tu0 + (tu1 - tu0) * (i + 0.5) / nu)
                    sy = int(tv0 + (tv1 - tv0) * (j + 0.5) / nv)
                    col = px[min(tw - 1, max(0, sx)), min(th - 1, max(0, sy))]
                    if col[3] < 128:
                        continue
                    a, b = i / nu, (i + 1) / nu
                    c, d = j / nv, (j + 1) / nv
                    pts = []
                    for (s, r) in ((a, c), (b, c), (b, d), (a, d)):
                        pts.append(cam([o[k] + du[k] * s + dv[k] * r for k in range(3)]))
                    z = sum(p[2] for p in pts) / 4
                    k = light[name] if e.get('shade', True) and not e.get('light_emission') else 1.0
                    quads.append((z, [(p[0], p[1]) for p in pts], (int(col[0] * k), int(col[1] * k), int(col[2] * k), 255)))
    quads.sort(key=lambda q: -q[0])
    for z, pts, col in quads:
        draw.polygon(pts, fill=col)
    return img
