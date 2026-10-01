"""32x item sprites.

Two tools:

* ``upscale(img)``: takes a 16x16 sprite to 32x32 the way a pixel artist would - Scale2x/EPX
  rounds the diagonal staircases, then the doubled two-texel outline is thinned back to one
  texel (the inner ring takes the colour of the fill next to it), the lit top-left edge gets a
  one-texel highlight line and the shaded bottom-right edge a one-texel shadow. The result keeps
  the original's silhouette and palette but with twice the room for detail.
* ``egg(...)``: mob-portrait spawn eggs in the modern vanilla style - an egg shape in the mob's
  colours carrying its face.
"""
from __future__ import annotations

import colorsys

import numpy as np
from PIL import Image


def _key(px):
    return tuple(int(v) for v in px)


def epx(img):
    a = np.array(img.convert('RGBA'))
    h, w = a.shape[:2]
    out = np.zeros((h * 2, w * 2, 4), dtype=np.uint8)

    def P(x, y):
        if 0 <= x < w and 0 <= y < h:
            return _key(a[y, x])
        return (0, 0, 0, 0)
    for y in range(h):
        for x in range(w):
            p = P(x, y)
            A, B, C, D = P(x, y - 1), P(x + 1, y), P(x - 1, y), P(x, y + 1)
            e0 = A if (C == A and C != D and A != B) else p
            e1 = B if (A == B and A != C and B != D) else p
            e2 = C if (D == C and D != B and C != A) else p
            e3 = D if (B == D and B != A and D != C) else p
            # never let transparency eat into a solid pixel's own block more than EPX intends
            out[y * 2, x * 2] = e0
            out[y * 2, x * 2 + 1] = e1
            out[y * 2 + 1, x * 2] = e2
            out[y * 2 + 1, x * 2 + 1] = e3
    return out


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def adjust(c, dl, ds=0.0, dh=0.0):
    r, g, b = c[0] / 255, c[1] / 255, c[2] / 255
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    l = max(0.0, min(1.0, l + dl))
    s = max(0.0, min(1.0, s + ds))
    h = (h + dh) % 1.0
    r, g, b = colorsys.hls_to_rgb(h, l, s)
    return (int(r * 255), int(g * 255), int(b * 255), c[3])


def upscale(img, highlight=True):
    a = epx(img)
    H, W = a.shape[:2]
    solid = a[:, :, 3] > 0

    def is_solid(x, y):
        return 0 <= x < W and 0 <= y < H and solid[y, x]

    edge = np.zeros_like(solid)
    for y in range(H):
        for x in range(W):
            if solid[y, x] and not all(is_solid(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge[y, x] = True
    # outline colours: the dark colours that sit on the silhouette edge
    edge_cols = {}
    for y in range(H):
        for x in range(W):
            if edge[y, x]:
                k = _key(a[y, x])
                edge_cols[k] = edge_cols.get(k, 0) + 1
    fill_lums = [lum(_key(a[y, x])) for y in range(H) for x in range(W) if solid[y, x] and not edge[y, x]]
    mean_fill = sum(fill_lums) / max(1, len(fill_lums))
    outline = {k for k in edge_cols if lum(k) < mean_fill * 0.8}
    b = a.copy()
    # thin the doubled outline: an outline-coloured texel not touching transparency takes the
    # colour of its nearest inner neighbour
    for y in range(H):
        for x in range(W):
            if not solid[y, x] or edge[y, x] or _key(a[y, x]) not in outline:
                continue
            for dx, dy in ((1, 1), (-1, -1), (1, -1), (-1, 1), (2, 0), (-2, 0), (0, 2), (0, -2), (1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if is_solid(xx, yy) and not edge[yy, xx] and _key(a[yy, xx]) not in outline:
                    b[y, x] = a[yy, xx]
                    break
    if highlight:
        c = b.copy()
        for y in range(H):
            for x in range(W):
                if not solid[y, x] or edge[y, x] or _key(b[y, x]) in outline:
                    continue
                up = not is_solid(x, y - 2) or edge[y - 1, x] if y >= 1 else True
                left = not is_solid(x - 2, y) or edge[y, x - 1] if x >= 1 else True
                down = edge[y + 1, x] if y + 1 < H else True
                right = edge[y, x + 1] if x + 1 < W else True
                px = _key(b[y, x])
                if (up and edge[y - 1, x]) and (left and edge[y, x - 1]):
                    c[y, x] = adjust(px, 0.16)
                elif up and y >= 1 and edge[y - 1, x]:
                    c[y, x] = adjust(px, 0.08)
                elif (down or right):
                    c[y, x] = adjust(px, -0.07)
        b = c
    return Image.fromarray(b, 'RGBA')


# ----------------------------------------------------------------------------- spawn eggs

def egg(base, spot, face=None, keys=None, top=None, seed=0):
    """A 32x32 spawn egg: a shaded egg of `base` (light from the top left), a few `spot`
    speckles, and optionally a face map (list of strings, any size, centred a little below the
    middle) and a `top` map drawn over the crown (ears, antlers, crests)."""
    import random
    rnd = random.Random(seed)
    W = H = 32
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    px = img.load()
    cx, cy = 15.5, 17.0
    ramp = [adjust(base, -0.30), adjust(base, -0.16), adjust(base, -0.06), base, adjust(base, 0.10), adjust(base, 0.22)]
    for y in range(H):
        for x in range(W):
            dx = (x + 0.5 - cx) / 10.5
            dy = (y + 0.5 - cy) / (13.5 if y + 0.5 < cy else 12.0)
            d = dx * dx + dy * dy
            if d > 1.0:
                continue
            # pseudo-normal for top-left light
            nz = max(0.0, 1.0 - d) ** 0.5
            light = (-dx * 0.55 - dy * 0.6 + nz * 0.9)
            k = 0 if d > 0.86 else (1 if light < 0.15 else 2 if light < 0.45 else 3 if light < 0.8 else 4 if light < 1.05 else 5)
            px[x, y] = ramp[k]
    for _ in range(7):
        x, y = rnd.randrange(8, 24), rnd.randrange(8, 28)
        if px[x, y][3] and px[x, y] != ramp[0]:
            px[x, y] = spot
            if px[x + 1, y][3] and px[x + 1, y] != ramp[0]:
                px[x + 1, y] = adjust(spot, -0.1)
    px[11, 9] = adjust(base, 0.35)
    px[12, 9] = adjust(base, 0.3)
    px[11, 10] = adjust(base, 0.3)

    def draw(rows, ox, oy):
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch in '. ':
                    continue
                x, y = ox + i, oy + j
                if 0 <= x < W and 0 <= y < H:
                    px[x, y] = keys[ch]
    if face:
        draw(face, 16 - len(face[0]) // 2, 18 - len(face) // 2)
    if top:
        draw(top, 16 - len(top[0]) // 2, 0)
    return img
