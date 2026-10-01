"""Chunky, glossy RPG-style item sprites (bold dark outlines, round lit volumes, saturated
hue-shifted ramps, white glints), drawn from shapes rather than pixel by pixel.

A Sprite is a stack of layers; each layer is a mask (built from primitives) with a material and a
volume profile. Rendering gives every layer its own height field (round 'dome', flat 'bevel' or
'flat'), lights it from the top left, quantises the light into the material's ramp, adds glints,
then draws a one-texel outline around the whole silhouette in the darkest tone of the layer it
touches and a darker seam where layers overlap.
"""
from __future__ import annotations

import colorsys
import math

import numpy as np
from PIL import Image

N = 32
LIGHT = np.array([-0.55, -0.65, 0.53])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


# ----------------------------------------------------------------------------- colour

def hexc(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def ramp(base, n=5, spread=0.72, hue_shift=0.07, sat=0.12):
    """A hue-shifted ramp around `base` (hex): shadows lean cool/purple, lights lean warm."""
    r, g, b = (v / 255 for v in hexc(base))
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    out = []
    for i in range(n):
        t = i / (n - 1) - 0.5                      # -0.5 .. 0.5
        ll = max(0.03, min(0.97, l + t * spread))
        hh = (h - t * hue_shift) % 1.0 if h > 0.08 else (h + t * hue_shift * 0.4) % 1.0
        ss = max(0.0, min(1.0, s + (sat if abs(t) < 0.3 else -sat * 0.5)))
        rr, gg, bb = colorsys.hls_to_rgb(hh, ll, ss)
        out.append((int(rr * 255), int(gg * 255), int(bb * 255), 255))
    return out


def outline_of(rmp):
    c = rmp[0]
    return (int(c[0] * 0.45), int(c[1] * 0.4), int(c[2] * 0.5), 255)


# ----------------------------------------------------------------------------- masks

_yy, _xx = np.mgrid[0:N, 0:N]
_cx, _cy = _xx + 0.5, _yy + 0.5


def ellipse(cx, cy, rx, ry, angle=0.0):
    a = math.radians(angle)
    dx, dy = _cx - cx, _cy - cy
    u = dx * math.cos(a) + dy * math.sin(a)
    v = -dx * math.sin(a) + dy * math.cos(a)
    return (u / rx) ** 2 + (v / ry) ** 2 <= 1.0


def poly(points):
    """Pixel-centre inside test (even-odd)."""
    m = np.zeros((N, N), dtype=bool)
    pts = list(points)
    for i in range(len(pts)):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % len(pts)]
        cond = ((y1 > _cy) != (y2 > _cy)) & (_cx < (x2 - x1) * (_cy - y1) / ((y2 - y1) + 1e-9) + x1)
        m ^= cond
    return m


def capsule(x1, y1, x2, y2, r):
    px, py = _cx - x1, _cy - y1
    dx, dy = x2 - x1, y2 - y1
    L = dx * dx + dy * dy + 1e-9
    t = np.clip((px * dx + py * dy) / L, 0, 1)
    ex, ey = px - t * dx, py - t * dy
    return ex * ex + ey * ey <= r * r


def rect(x0, y0, x1, y1):
    return (_cx >= x0) & (_cx < x1) & (_cy >= y0) & (_cy < y1)


def rrect(x0, y0, x1, y1, r):
    m = rect(x0, y0, x1, y1)
    for (cx, cy) in ((x0 + r, y0 + r), (x1 - r, y0 + r), (x0 + r, y1 - r), (x1 - r, y1 - r)):
        corner = ((_cx < x0 + r) | (_cx > x1 - r)) & ((_cy < y0 + r) | (_cy > y1 - r))
        m &= ~corner | ((_cx - cx) ** 2 + (_cy - cy) ** 2 <= r * r) | ~(((_cx - cx) * (cx - (x0 + x1) / 2) >= 0) & ((_cy - cy) * (cy - (y0 + y1) / 2) >= 0))
    return m


def from_rows(rows, x0=0, y0=0, ch='#'):
    m = np.zeros((N, N), dtype=bool)
    for j, row in enumerate(rows):
        for i, c in enumerate(row):
            if c == ch and 0 <= y0 + j < N and 0 <= x0 + i < N:
                m[y0 + j, x0 + i] = True
    return m


# ----------------------------------------------------------------------------- distance

def _dist(mask):
    """Chamfer distance (texels) from each solid texel to the nearest empty one."""
    INF = 1e9
    d = np.where(mask, INF, 0.0)
    H, W = mask.shape
    for y in range(H):
        for x in range(W):
            if d[y, x]:
                best = d[y, x]
                for dx, dy, w in ((-1, 0, 1), (0, -1, 1), (-1, -1, 1.41), (1, -1, 1.41)):
                    xx, yy = x + dx, y + dy
                    v = d[yy, xx] + w if 0 <= xx < W and 0 <= yy < H else w
                    best = min(best, v)
                d[y, x] = best
    for y in range(H - 1, -1, -1):
        for x in range(W - 1, -1, -1):
            if d[y, x]:
                best = d[y, x]
                for dx, dy, w in ((1, 0, 1), (0, 1, 1), (1, 1, 1.41), (-1, 1, 1.41)):
                    xx, yy = x + dx, y + dy
                    v = d[yy, xx] + w if 0 <= xx < W and 0 <= yy < H else w
                    best = min(best, v)
                d[y, x] = best
    return d


# ----------------------------------------------------------------------------- sprite

class Sprite:
    def __init__(self):
        self.layers = []   # (mask, ramp, profile, depth, gloss)

    def add(self, mask, material, profile='dome', depth=None, gloss=1.0, outline=True):
        """material: hex base colour or a ramp list. profile: dome (round), bevel (flat face with
        rounded edges), flat. depth: radius of the rounding in texels (default: auto)."""
        if isinstance(material, list):
            rmp = [(*hexc(c), 255) if isinstance(c, str) else c for c in material]
        else:
            rmp = ramp(material)
        self.layers.append((mask.copy(), rmp, profile, depth, gloss, outline))
        return self

    def render(self, glints=True):
        img = np.zeros((N, N, 4), dtype=np.uint8)
        owner = -np.ones((N, N), dtype=int)
        for i, (mask, rmp, profile, depth, gloss, _) in enumerate(self.layers):
            d = _dist(mask)
            R = depth or max(2.0, min(d.max(), 7.0))
            if profile == 'dome':
                t = np.clip(d / R, 0, 1)
                h = np.sqrt(1 - (1 - t) ** 2) * R
            elif profile == 'bevel':
                h = np.clip(d, 0, depth or 2.0)
            else:
                h = np.zeros_like(d)
            gy, gx = np.gradient(h)
            n = np.dstack([-gx, -gy, np.ones_like(h)])
            n /= np.linalg.norm(n, axis=2, keepdims=True)
            diff = np.clip(n @ LIGHT, 0, 1)
            # reflected light for glints
            half = LIGHT + np.array([0, 0, 1.0])
            half /= np.linalg.norm(half)
            spec = np.clip(n @ half, 0, 1) ** 40 * gloss
            k = len(rmp)
            v = diff ** 0.85
            # ambient occlusion along the lower right of each part
            v = v - np.where((d <= 1.5) & (n[:, :, 0] > -0.05) & (n[:, :, 1] > -0.05), 0.16, 0.0)
            bands = [0.22, 0.48, 0.74, 0.92] if k == 5 else list(np.linspace(0, 1, k + 1)[1:-1])
            idx = np.zeros_like(v, dtype=int)
            for bnd in bands:
                idx += (v > bnd).astype(int)
            idx = np.clip(idx, 0, k - 1)
            # tidy: a texel alone in its band takes its neighbours' band
            for y in range(1, N - 1):
                for x in range(1, N - 1):
                    if mask[y, x]:
                        nb = [idx[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if mask[y + dy, x + dx]]
                        if len(nb) >= 3 and all(b != idx[y, x] for b in nb):
                            idx[y, x] = max(set(nb), key=nb.count)
            for y in range(N):
                for x in range(N):
                    if mask[y, x]:
                        img[y, x] = rmp[idx[y, x]]
                        owner[y, x] = i
            # a crisp glint on round, glossy parts: a bright dab up and to the left of the centre
            if glints and gloss > 0 and profile != 'flat' and mask.sum() > 12:
                ys, xs = np.nonzero(mask)
                cy, cx = ys.mean(), xs.mean()
                ry, rx = (ys.max() - ys.min()) / 2, (xs.max() - xs.min()) / 2
                gx, gy = int(round(cx - rx * 0.45)), int(round(cy - ry * 0.48))
                pts = [(gx, gy), (gx + 1, gy), (gx, gy + 1)] if min(rx, ry) >= 4 else [(gx, gy)]
                if min(rx, ry) >= 7:
                    pts += [(gx + 2, gy), (gx, gy + 2), (gx + 1, gy + 1)]
                for (x, y) in pts:
                    if 0 <= x < N and 0 <= y < N and mask[y, x] and d[y, x] >= 1.5:
                        img[y, x] = (255, 255, 255, 255)
                if min(rx, ry) >= 5:
                    x, y = int(round(cx + rx * 0.35)), int(round(cy + ry * 0.45))
                    if 0 <= x < N and 0 <= y < N and mask[y, x] and d[y, x] >= 1.5:
                        img[y, x] = rmp[-2]
        # seams: where an upper layer covers a lower one, darken the lower layer's texels next to it
        for y in range(N):
            for x in range(N):
                o = owner[y, x]
                if o < 0:
                    continue
                for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < N and 0 <= yy < N and owner[yy, xx] > o and dy >= 0 and self.layers[owner[yy, xx]][5]:
                        img[y, x] = self.layers[o][1][0]
                        break
        # outline around the silhouette, in each touching layer's darkest tone
        out = img.copy()
        for y in range(N):
            for x in range(N):
                if owner[y, x] >= 0:
                    continue
                near = [owner[y + dy, x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                        if 0 <= x + dx < N and 0 <= y + dy < N and owner[y + dy, x + dx] >= 0]
                near = [o for o in near if self.layers[o][5]]
                if near:
                    out[y, x] = outline_of(self.layers[max(near)][1])
        return Image.fromarray(out.copy(), 'RGBA').copy()


def save_sheet(sprites, path, scale=6, cols=8, bg=(139, 139, 139, 255)):
    rows = (len(sprites) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * (N * scale + 8), rows * (N * scale + 8)), bg)
    for i, im in enumerate(sprites):
        sheet.alpha_composite(im.resize((N * scale, N * scale), Image.NEAREST), ((i % cols) * (N * scale + 8) + 4, (i // cols) * (N * scale + 8) + 4))
    sheet.save(path)
