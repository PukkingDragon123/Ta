"""S1 Stomper remake: the Sift's elephant (geometry, hand-painted texture, rig check, spawn egg).

A tall, long-legged elephant that looks like the Sift Plains it roams: a hide of the plains' mauve-rose
earth with wrinkle rings and a darker underbelly, a blanket of pink coral turf over its back that
hangs down its flanks in ragged tufts, coral bushes, pink grass, a few teal Sift-grass tufts and
flowers growing on its domed back, big flapping ears with coral-pink insides, small bone tusks, bone
toenails, and a long five-part trunk that hangs near the ground and grabs.

Painting (one texel per model unit, like vanilla): every texel's light is worked out in 3D at the rest
pose - higher is lighter, faces turn to the light, a lit top row and shaded foot on side faces,
painted shadow in crevices and under overhangs, clustered noise, wrinkle rings that run on across
cube seams - then quantised onto short hand-picked ramps (cool shadows, warm lights), so it stays
clean pixel art. Eyes, toenails, nostrils, the mouth and the ears are crisp decals on top. (The 3D
field idea follows S2's tools/handpaint.py.)

Registered into mobs.ALL from tools/mobs_wild.py. Java: StomperModel (animation), Stomper (behaviour)
and StomperRig (the grab's trunk rig, checked against this geometry by check_rig())."""
import math
import os

import numpy as np
from PIL import Image

from modelkit import Model, _rest_frames

ROOT = os.path.join(os.path.dirname(__file__), '..')
EXPRS = ['blink', 'happy', 'angry', 'hurt', 'dead']

# --------------------------------------------------------------------------- palettes (dark -> light)
# the Sift Plains: mauve-rose earth (turf side), pink coral turf and coral bushes, teal Sift grass
NORMAL = {
    'hide': ['#3c2234', '#4e2c40', '#5e3a4a', '#6c4656', '#785060', '#885868', '#986878', '#ac8088', '#c09aa0'],
    'hide2': ['#40263a', '#523246', '#644050', '#724a5a', '#805666', '#906270', '#a07280', '#b28890', '#c4a2a8'],
    'belly': ['#24121e', '#2e1828', '#3a2032', '#46283c', '#523044', '#5e3a4a', '#6a4454', '#76505e', '#825a68'],
    'turf': ['#8e2e46', '#a83c54', '#c04c60', '#d85868', '#e87078', '#f07880', '#f8909a', '#ffaab0', '#ffc6c8'],
    'turf2': ['#842a42', '#9c3650', '#b4445c', '#c85264', '#d85868', '#e8687a', '#f07880', '#f8949c', '#ffb4b8'],
    'teal': ['#145a5c', '#1e7272', '#2a8c88', '#40b8b0', '#50c8b8', '#78dcc8', '#a8f0dc'],
    'bone': ['#6c5c50', '#94826e', '#bcaa90', '#d8cbb0', '#ece4cf', '#fbf6ea'],
    'pad': ['#1c0e18', '#2a1622', '#3a2030', '#4a2a3c'],
    'ear': ['#7a2c48', '#9a3c58', '#b84e68', '#d06478', '#e47e8c', '#f49ca6', '#ffc0c4'],
    'eye': ['#120a12', '#2a1a26', '#ffffff', '#e8902e', '#ffc860'],
    'mouth': ['#2a0e1c', '#46182c', '#7a2c44', '#b04c62'],
    'flower': ['#fff2ea', '#ffffff', '#ffd64e', '#f0a428', '#fff8c8'],
    'glow': ['#178a96', '#3fd8e2', '#9ffcff', '#e8ffff'],
    'leaf': ['#1a5248', '#246a5c', '#348870', '#4aa486'],
}
# the White Forest coat: snow-pale lilac earth under a snowy turf with icy-blue grass
WHITE = {
    'hide': ['#4a4058', '#5e5470', '#726886', '#857b98', '#988fab', '#aaa2bc', '#bcb5cc', '#d0cadc', '#e4e0ec'],
    'hide2': ['#4e4460', '#625a76', '#76708c', '#8a829e', '#9c96b0', '#aea8c0', '#c0bcd0', '#d4d0e0', '#e8e6f0'],
    'belly': ['#2c2638', '#383044', '#443c52', '#50485e', '#5c546a', '#686078', '#746c84', '#807890', '#8c849c'],
    'turf': ['#9c8cac', '#b4a6c2', '#cabed6', '#dcd4e6', '#ebe6f2', '#f4f1f9', '#fbfaff', '#ffffff', '#ffffff'],
    'turf2': ['#a690b0', '#bea8c6', '#d4c2da', '#e6d8ea', '#f2eaf4', '#f8f2fa', '#fdfaff', '#ffffff', '#ffffff'],
    'teal': ['#3c6a88', '#4e84a6', '#68a2c4', '#86bedc', '#a6d6ee', '#c8e8f8', '#e8f8ff'],
    'bone': ['#77706a', '#a0988a', '#c6beac', '#e0dacb', '#f2eee4', '#ffffff'],
    'pad': ['#262032', '#342c42', '#443a54', '#544a66'],
    'ear': ['#8a6a8a', '#a080a2', '#b898b8', '#ceb0cc', '#e2c8de', '#f0dcec', '#fff0f8'],
    'eye': ['#120a12', '#2a1a26', '#ffffff', '#6cb8e6', '#bfe6ff'],
    'mouth': ['#2e1a2a', '#4a2a40', '#7e5672', '#ae86a0'],
    'flower': ['#ffffff', '#ffffff', '#c8e8ff', '#8cc4ec', '#eef8ff'],
    'glow': ['#3a9ab0', '#7fe4f2', '#c8fcff', '#ffffff'],
    'leaf': ['#4e7a88', '#62909c', '#7aa8b0', '#9cc4c8'],
}
PAL_LEN = {k: len(v) for k, v in NORMAL.items()}
assert all(len(v) == PAL_LEN[k] for k, v in WHITE.items())


def _rgb(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# --------------------------------------------------------------------------- 3D noise
def _hash3(ix, iy, iz, seed):
    x = (ix.astype(np.int64) * 73856093) ^ (iy.astype(np.int64) * 19349663) ^ (iz.astype(np.int64) * 83492791) ^ (seed * 2654435761)
    x &= 0xffffffff
    x ^= x >> 13
    x = (x * 1274126177) & 0xffffffff
    x ^= x >> 16
    return (x & 0xffff).astype(np.float32) / 65535.0


def vnoise(p, cell, seed):
    """Smooth 3D value noise in [0, 1] at points p (..., 3)."""
    q = p / cell
    i = np.floor(q)
    f = q - i
    f = f * f * (3 - 2 * f)
    i = i.astype(np.int64)
    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[..., 0] if dx else 1 - f[..., 0]) * (f[..., 1] if dy else 1 - f[..., 1]) * (f[..., 2] if dz else 1 - f[..., 2])
                out = out + w * _hash3(i[..., 0] + dx, i[..., 1] + dy, i[..., 2] + dz, seed)
    return out


def fbm(p, cell, seed, octaves=2):
    tot, amp, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        tot = tot + amp * vnoise(p, cell / (2 ** o), seed + o * 101)
        norm += amp
        amp *= 0.5
    return tot / norm


# --------------------------------------------------------------------------- the scene at rest
_GEO = {
    'north': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z0), (x1 - x0, 0, 0), (0, y1 - y0, 0)), (0, 0, -1)),
    'south': (lambda x0, y0, z0, x1, y1, z1: ((x1, y0, z1), (x0 - x1, 0, 0), (0, y1 - y0, 0)), (0, 0, 1)),
    'up': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)), (0, -1, 0)),
    'down': (lambda x0, y0, z0, x1, y1, z1: ((x0, y1, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)), (0, 1, 0)),
    'west': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z1), (0, 0, z0 - z1), (0, y1 - y0, 0)), (-1, 0, 0)),
    'east': (lambda x0, y0, z0, x1, y1, z1: ((x1, y0, z0), (0, 0, z1 - z0), (0, y1 - y0, 0)), (1, 0, 0)),
}
VERT = ('north', 'south', 'east', 'west')


class Scene:
    """The model at its rest pose: where every face's texels are, and what shadows them."""

    def __init__(self, model):
        self.frames = _rest_frames(model)
        self.solids = [c for c in model.cubes() if min(c.size) >= 0.9 and not c.paint.get('no_occlude')]
        pts = []
        for c in self.solids:
            M, T = self.frames[id(c)]
            o, s = np.array(c.origin, float), np.array(c.size, float)
            for k in range(8):
                pts.append(M @ (o + s * np.array([k & 1, (k >> 1) & 1, (k >> 2) & 1])) + T)
        pts = np.array(pts)
        self.ymin, self.ymax = pts[:, 1].min(), pts[:, 1].max()

    def inside(self, q, skip):
        hit = np.zeros(len(q), bool)
        for o in self.solids:
            if o is skip:
                continue
            Mo, To = self.frames[id(o)]
            loc = (q - To) @ Mo
            lo = np.array(o.origin, np.float64) - o.inflate
            hi = lo + np.array(o.size, np.float64) + 2 * o.inflate
            hit |= np.all((loc > lo + 0.02) & (loc < hi - 0.02), axis=1)
        return hit

    def occlusion(self, cube, wp, wn):
        """Painted shadow 0..1: a cube just in front of the face (crevices), or somewhere above it."""
        shp = wp.shape[:2]
        q0 = wp.reshape(-1, 3)
        occ = np.zeros(len(q0))
        for dist, wgt in ((0.5, 0.45), (1.5, 0.3), (2.8, 0.15)):
            occ += self.inside(q0 + wn * dist, cube) * wgt
        if wn[1] > -0.5:
            base = q0 + wn * 0.45
            sky = np.zeros(len(q0))
            for k, dist in enumerate((1.0, 2.0, 3.5, 5.5, 8.0)):
                sky = np.maximum(sky, self.inside(base + np.array([0, -dist, 0]), cube) * (1.0 - k * 0.12))
            occ += sky * 0.55
        return np.clip(occ, 0, 1).reshape(shp)


class Ctx:
    """Everything a face painter knows: the face size, its texels' cube-local and rest-pose world
    positions (loc, wp: h x w x 3), the world normal, the painted shadow and the expression."""

    def __init__(self, scene, cube, face, fw, fh, expr, seed):
        self.cube, self.face, self.w, self.h, self.expr, self.seed = cube, face, fw, fh, expr, seed
        x0, y0, z0 = (o - cube.inflate for o in cube.origin)
        w, h, d = (s + 2 * cube.inflate for s in cube.size)
        corner, du, dv = _GEO[face][0](x0, y0, z0, x0 + w, y0 + h, z0 + d)
        jj, ii = np.mgrid[0:fh, 0:fw]
        u, v = (ii + 0.5) / fw, (jj + 0.5) / fh
        self.loc = np.stack([corner[q] + du[q] * u + dv[q] * v for q in range(3)], -1)
        M, T = scene.frames[id(cube)]
        self.wp = self.loc @ M.T + T
        self.wn = M @ np.array(_GEO[face][1], float)
        self.ii, self.jj = ii, jj
        self.plane = min(cube.size) == 0
        self.vertical = face in VERT
        self.scene = scene
        self._occ = None

    @property
    def occ(self):
        if self._occ is None:
            self._occ = np.zeros((self.h, self.w)) if self.plane else self.scene.occlusion(self.cube, self.wp, self.wn)
        return self._occ

    def sym(self, off=0.0):
        """World positions mirrored across the middle, so both flanks get the same pattern."""
        p = self.wp.copy()
        p[..., 0] = np.abs(p[..., 0])
        return p + off


# --------------------------------------------------------------------------- keyed grids
class Grid:
    """A face as (material, shade) keys (None = cut out); `glow` holds the emissive texels."""

    def __init__(self, w, h, mat='hide', shade=4):
        self.w, self.h = w, h
        self.k = [[(mat, shade) for _ in range(w)] for _ in range(h)]
        self.glow = set()

    def get(self, x, y):
        return self.k[y][x] if 0 <= x < self.w and 0 <= y < self.h else None

    def set(self, x, y, key, glow=False):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.k[y][x] = key
            if glow:
                self.glow.add((x, y))
            else:
                self.glow.discard((x, y))

    def shift(self, x, y, d):
        c = self.get(x, y)
        if c is not None:
            self.k[y][x] = (c[0], max(0, min(PAL_LEN[c[0]] - 1, c[1] + d)))

    def cut(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.k[y][x] = None
            self.glow.discard((x, y))


def from_field(mats, idx, alpha=None):
    """A Grid from per-texel material names (or one name) and ramp indices."""
    h, w = idx.shape
    g = Grid(w, h)
    for y in range(h):
        for x in range(w):
            if alpha is not None and not alpha[y, x]:
                g.k[y][x] = None
            else:
                m = mats[y][x] if isinstance(mats, list) else mats
                g.k[y][x] = (m, int(min(PAL_LEN[m] - 1, max(0, idx[y, x]))))
    return g


def quant(L, n):
    return np.clip(np.round(L * (n - 1)), 0, n - 1).astype(int)


def light_field(c, base=0.55, rim=1.0, ao=1.0, noise=0.09, cell=2.2):
    """The painted light of a face (0..1, before quantising)."""
    L = np.full((c.h, c.w), base, np.float32)
    hgt = (c.scene.ymax - c.wp[..., 1]) / max(1.0, c.scene.ymax - c.scene.ymin)
    L += (hgt - 0.5) * 0.22
    L += 0.12 * max(0.0, -c.wn[1]) - 0.2 * max(0.0, c.wn[1]) + 0.03 * max(0.0, -c.wn[2])
    ii, jj, fw, fh = c.ii, c.jj, c.w, c.h
    if rim and not c.plane:
        if c.vertical and fh >= 3:
            L += np.where(jj == 0, 0.16 * rim, 0) + np.where(jj == 1, 0.05 * rim, 0) + np.where(jj == fh - 1, -0.15 * rim, 0)
            if fw >= 3:
                L += np.where((ii == 0) | (ii == fw - 1), -0.05 * rim, 0)
        elif c.face == 'up' and fw >= 3 and fh >= 3:
            L += np.where((ii == 0) | (ii == fw - 1) | (jj == 0) | (jj == fh - 1), 0.06 * rim, 0)
        elif c.face == 'down' and fw >= 3 and fh >= 3:
            L += np.where((ii == 0) | (ii == fw - 1) | (jj == 0) | (jj == fh - 1), -0.05 * rim, 0)
    if ao and not c.plane:
        L -= c.occ * 0.42 * ao
    if noise:
        n = fbm(c.sym(c.seed * 0.013), cell, 11 + c.seed % 5) - 0.5
        L += np.clip(np.round(n * 4.0), -2, 2) * noise * 0.5
    return L


def wrinkles(c, period, wob=1.4, gap=0.42):
    """Wrinkle rings worked out in 3D: a dark crease every `period` units down the body, wandering
    with the noise and broken into segments, with a lit fold just above each crease."""
    if not period or not c.vertical or c.plane:
        return 0.0
    p = c.sym(0.0)
    y = p[..., 1] + wob * (fbm(p * np.array([1.0, 0.0, 1.0]) + 3.0, 3.0, 91) - 0.5) * 2.0
    ph = np.mod(y, period) / period
    seg = fbm(p + 5.0, 2.6, 93) > gap
    crease = (ph < 1.0 / period) & seg
    fold = (ph >= 1.0 - 1.0 / period) & seg
    return np.where(crease, -0.2, 0.0) + np.where(fold, 0.08, 0.0)


# --------------------------------------------------------------------------- face painters
def painter(fn, expressive=False):
    fn.expressive = expressive
    return fn


def P_hide(light=0.0, period=4.0, wob=1.6, belly=None, rim=1.0, ao=1.0, mottle=0.6, decal=None, expressive=False, under=0.0):
    """The mauve earth hide: light, wrinkle rings, a dustier mottle, a darker underbelly below the
    wavy line at model y `belly` (and on faces turned down). `under` lightens a south face (the pale
    throat of the trunk)."""
    def fn(c):
        L = light_field(c, 0.62 + light + (under if c.face == 'south' else 0.0), rim, ao, 0.14, 1.8)
        L = L + wrinkles(c, period, wob)
        # pebbles and clods, like the turf's earth: lone lighter texels and a few darker ones
        pk = _hash3(np.floor(c.wp[..., 0] * 1.01).astype(np.int64), np.floor(c.wp[..., 1] * 1.01).astype(np.int64),
                    np.floor(c.wp[..., 2] * 1.01).astype(np.int64), 404)
        L = L + np.where(pk < 0.035, 0.16, 0.0) - np.where(pk > 0.982, 0.16, 0.0)
        idx = quant(L, PAL_LEN['hide'])
        mot = fbm(c.sym(17.0), 3.0, 31) > mottle
        bel = np.zeros((c.h, c.w), bool)
        if c.wn[1] > 0.6:
            bel[:] = True
        if belly is not None:
            wav = (fbm(c.sym(9.0), 2.0, 71, 1) - 0.5) * 2.2
            bel |= c.wp[..., 1] + wav > belly
        mats = [['belly' if bel[y, x] else ('hide2' if mot[y, x] else 'hide') for x in range(c.w)] for y in range(c.h)]
        if bel.any():
            idx = np.where(bel, quant(L + 0.06, PAL_LEN['belly']), idx)
        g = from_field(mats, idx)
        if decal:
            decal(g, c)
        return g
    return painter(fn, expressive)


def P_turf(drape=0, phase=0, flowers=0.0, teal=0.0, light=0.0):
    """The pink coral turf: clustered pinks mottled with deeper coral, blade streaks on its sides
    and a ragged hanging edge (`drape` rows deep), teal grass tufts and tiny flowers on top."""
    def fn(c):
        L = light_field(c, 0.6 + light, 0.7, 0.85, 0.13, 1.6)
        if c.vertical:
            col = _hash3(np.round(c.wp[..., 0] * 3 + c.wp[..., 2] * 7).astype(np.int64), np.zeros_like(c.ii), np.zeros_like(c.ii), 5)
            L += (col - 0.5) * 0.14
        idx = quant(L, PAL_LEN['turf'])
        mot = fbm(c.sym(4.0), 2.6, 37) > 0.6
        g = from_field([['turf2' if mot[y, x] else 'turf' for x in range(c.w)] for y in range(c.h)], idx)
        if c.face == 'up':
            if teal:
                tt = fbm(c.wp + 2.0, 1.4, 77) > 1.0 - teal
                for y, x in zip(*np.nonzero(tt)):
                    g.set(x, y, ('teal', int(min(6, max(1, idx[y, x] - 2)))))
            if flowers:
                h = _hash3(np.round(c.wp[..., 0]).astype(np.int64), np.round(c.wp[..., 2]).astype(np.int64), np.zeros_like(c.ii), 9)
                for y, x in zip(*np.nonzero(h < flowers)):
                    g.set(x, y, ('flower', 1 if (x + y) % 3 else 2))
        if c.vertical and drape:
            pat = [1, 3, 2, 4, 2, 3, 1, 2, 4, 3, 2, 1, 3, 4, 2, 3]
            for x in range(c.w):
                cut = min(drape, pat[(x + phase) % len(pat)])
                for y in range(c.h - cut, c.h):
                    g.cut(x, y)
                tip = c.h - cut - 1
                if tip >= 0:
                    g.shift(x, tip, -2)
                    g.shift(x, max(0, tip - 1), -1)
        return g
    return painter(fn)


def P_bone(root=None):
    """Tusks: bone lit by its faces, a dark collar where a tusk leaves the gum."""
    def fn(c):
        L = light_field(c, 0.62, 0.8, 0.0, 0.0)
        g = from_field('bone', quant(L, PAL_LEN['bone']))
        if root is not None:
            for y, x in zip(*np.nonzero(c.loc[..., 2] > root)):
                g.set(x, y, ('hide', 2))
        return g
    return painter(fn)


def P_pad():
    def fn(c):
        return from_field('pad', quant(light_field(c, 0.5, 0.0, 0.0, 0.1, 1.5), PAL_LEN['pad']))
    return painter(fn)


def P_mouth():
    def fn(c):
        return from_field('mouth', quant(light_field(c, 0.45, 0.0, 0.0, 0.1, 1.5), PAL_LEN['mouth']))
    return painter(fn)


def foot_decal(g, c):
    """Three bone toenails along the bottom of a foot's front, the outer ones peeking round its
    sides, and a dark pad rim along the ground."""
    if c.face in VERT:
        for x in range(c.w):
            g.set(x, c.h - 1, ('pad', 2))
    if c.face == 'north':
        for x0 in (0, 3, 6):
            for dx in range(2):
                g.set(x0 + dx, c.h - 1, ('bone', 2 if dx else 3))
                g.set(x0 + dx, c.h - 2, ('bone', 5 if dx == 0 else 4))
                g.set(x0 + dx, c.h - 3, ('hide', 1))
    elif c.face in ('east', 'west'):
        x = 0 if c.face == 'east' else c.w - 1
        g.set(x, c.h - 1, ('bone', 2))
        g.set(x, c.h - 2, ('bone', 4))


def nostrils(g, c):
    if c.face != 'down':
        return
    cx, cy = c.w // 2, c.h // 2
    for y in range(c.h):
        for x in range(c.w):
            g.set(x, y, ('hide', 6 if x in (0, c.w - 1) or y in (0, c.h - 1) else 5))
    g.set(cx - 1, cy, ('mouth', 0))
    g.set(cx + 1 if c.w > 3 else cx, cy, ('mouth', 0))


def eye(g, c):
    """The elephant's eye on a side of the skull (found by its position, so the right side is drawn
    mirrored): a dark pupil with a white glint at the front, an amber lower lid, a dark lid line under
    a lit brow fold. It blinks, smiles, glares, winces and crosses out with its mood."""
    if c.face not in ('east', 'west'):
        return
    z0, y0 = c.cube.origin[2], c.cube.origin[1]
    pos = {}
    for y in range(c.h):
        for x in range(c.w):
            pos[(int(math.floor(c.loc[y, x, 2] - (z0 + 3))), int(math.floor(c.loc[y, x, 1] - (y0 + 7))))] = (x, y)

    def at(dx, dy, key):
        p = pos.get((dx, dy))
        if p:
            g.set(p[0], p[1], key)
    e = c.expr
    for dx in range(-1, 4):
        for dy in range(-1, 3):
            at(dx, dy, ('hide', 3))
    for dx in range(-1, 4):
        at(dx, -2, ('hide', 7))
    if e in ('blink', 'happy'):
        for dx in range(3):
            at(dx, -1, ('hide', 2))
            at(dx, 0, ('hide', 1) if e == 'blink' else ('hide', 5))
            at(dx, 1, ('eye', 3) if e == 'blink' else ('hide', 4))
        if e == 'happy':
            at(0, 1, ('hide', 1))
            at(1, 0, ('hide', 1))
            at(2, 1, ('hide', 1))
        return
    if e == 'dead':
        for dx, dy in ((0, -1), (2, -1), (1, 0), (0, 1), (2, 1)):
            at(dx, dy, ('eye', 0))
        return
    if e == 'hurt':
        for dx, dy in ((0, -1), (1, 0), (0, 1)):
            at(dx, dy, ('eye', 0))
        at(2, 0, ('hide', 1))
        return
    for dx in range(3):
        at(dx, -1, ('hide', 0))
    at(0, 0, ('eye', 1) if e == 'angry' else ('eye', 2))
    at(1, 0, ('eye', 0))
    at(2, 0, ('hide', 2))
    at(0, 1, ('eye', 1))
    at(1, 1, ('eye', 0))
    at(2, 1, ('eye', 4))
    at(0, 2, ('eye', 3))
    at(1, 2, ('eye', 3))
    if e == 'angry':
        at(-1, -1, ('hide', 0))
        at(0, -2, ('hide', 1))
        at(1, -2, ('hide', 3))
        at(-1, -2, ('hide', 5))


def ear_keep(u, v):
    """An elephant ear (u along it from the head 0..1, v down 0..1): a rounded fan with a lobe."""
    r = (u - 0.05) ** 2 + ((v - 0.42) / 0.62) ** 2
    lobe = (u < 0.45) & (v > 0.7) & ((u - 0.18) ** 2 / 0.06 + (v - 0.86) ** 2 / 0.05 < 1.0)
    return ((r < 1.0) & (v < 0.92)) | lobe | ((u < 0.12) & (v < 0.75))


def P_ear(inner, length, height):
    """A side of an ear plane: the coral-pink inside or the outer hide, cut to the ear's shape (from
    its cube-local position, so both faces of a plane cut the same outline), veins fanning out."""
    def fn(c):
        u = np.clip(c.loc[..., 2] / length, 0, 1)
        v = np.clip(c.loc[..., 1] / height, 0, 1)
        keep = ear_keep(u, v)
        vein = np.zeros((c.h, c.w), bool)
        for k in range(4 if inner else 3):
            ang = (-0.6 + k * 0.45) if inner else (-0.5 + k * 0.5)
            vein |= np.abs(v - (0.42 + np.tan(ang) * u * 0.6 * length / height)) < 0.5 / height
        n = np.clip(np.round((fbm(c.sym(2.0 if inner else 6.0), 1.8, 23) - 0.5) * 4), -2, 2)
        if inner:
            L = 0.42 + u * 0.38 - (v < 0.2) * 0.1 + n * 0.04
            L = np.where(vein & (u > 0.12), L - 0.18, L)
            g = from_field('ear', quant(L, PAL_LEN['ear']), keep)
        else:
            L = 0.5 + (0.25 - v * 0.3) - u * 0.05 + n * 0.05
            L = np.where(vein & (u > 0.15), L - 0.14, L)
            mot = fbm(c.sym(17.0), 3.0, 31) > 0.6
            g = from_field([['hide2' if mot[y, x] else 'hide' for x in range(c.w)] for y in range(c.h)], quant(L, PAL_LEN['hide']), keep)
        # a lit rim along the top of the ear, a darker one along its hanging edge
        for y in range(c.h):
            for x in range(c.w):
                if g.get(x, y) is None:
                    continue
                if y == 0 or g.get(x, y - 1) is None:
                    g.shift(x, y, 2)
                elif g.get(x, y + 1) is None:
                    g.shift(x, y, -2)
        return g
    return painter(fn)


def sprite_painter(rows, keys, glow_chars=''):
    """A plant picture for both sides of a cross plane: rows of chars -> (material, shade)."""
    def make(mirror):
        def fn(c):
            g = Grid(c.w, c.h)
            for y in range(c.h):
                row = rows[y] if y < len(rows) else '.' * c.w
                if mirror:
                    row = row[::-1]
                for x in range(c.w):
                    ch = row[x] if x < len(row) else '.'
                    if ch == '.':
                        g.cut(x, y)
                    else:
                        g.set(x, y, keys[ch], ch in glow_chars)
            return g
        return painter(fn)
    return make(False), make(True)


GK = {'1': ('turf', 1), '2': ('turf', 3), '3': ('turf', 4), '4': ('turf', 6), '5': ('turf', 7), 'c': ('turf2', 2), 'C': ('turf2', 5),
      't': ('teal', 1), 'T': ('teal', 3), 'u': ('teal', 5), 'w': ('flower', 1), 'W': ('flower', 0), 'y': ('flower', 2), 'Y': ('flower', 3),
      'o': ('glow', 2), 'O': ('glow', 3), 's': ('leaf', 1), 'S': ('leaf', 3)}
SPRITES = {
    'tuft': ['..4...4.', '.43..4..', '.3.4.3.4', '43.3.32.', '.32.432.', '.2.32.2.', '..322...', '..22....'],
    'tall': ['...4.....', '..43..4..', '..3..43.4', '.43..3.3.', '.3.4.3.2.', '43.3.32..', '.32.322..', '.2.322...', '..222....', '..22.....'],
    'teal': ['.u...u.', '.T..uT.', 'uT.T.T.', '.T.TT.u', '.tTT.T.', '..tTt..', '..tt...'],
    'coral': ['..5....5.', '.54..454.', '..4.C4.4.', '.C.44.C4.', '..4C4.4..', '...34.3..', '....33...', '....c3...', '....cc...'],
    'bloom': ['..WwW..', '.WwywW.', '.wyYyw.', '.WwywW.', '..WwW..', '...S...', '..sS...', '...s.S.', '...sS..'],
    'orbs': ['.O.....', 'ooo..O.', '.o..ooo', '.s...o.', '.s..s..', '..s.s..', '..ss...', '...s...'],
    'bell': ['.4334..', '453354.', '533335.', '.2..2..', '..s....', '..s.S..', '..sS...', '..s....'],
}

# --------------------------------------------------------------------------- geometry
# the garden: (sprite, x, z, top y of the surface it grows from, y rotation)
GARDEN = [('coral', 0, -2, -23, 0.3), ('bloom', -4, 4, -23, 1.1), ('tuft', 4, -7, -23, 0.7), ('orbs', 3, 6, -23, -0.4),
          ('teal', -5, -6, -23, -0.6), ('bell', -9, -11, -20, 0.2), ('tall', 9, 1, -20, 1.3), ('coral', 9, -10, -20, -0.5),
          ('teal', -9, 9, -20, 0.9), ('tall', 8, 11, -20, -1.0), ('tuft', -1, 12, -20, 0.4), ('bloom', -9, -1, -20, 0.6),
          ('teal', 1, -12, -20, 0.5), ('coral', -5, 12, -20, 1.4), ('bloom', 10, 7, -20, -0.3), ('tall', -2, -7, -23, 0.9)]
# (width, length, rest x rotation) of each trunk segment, root to tip (StomperRig.SEG / REST)
TRUNK = [(7, 7, -0.05), (6, 7, 0.0), (5, 6, -0.05), (4, 6, -0.3), (3, 5, -0.6)]
LEGS = [('front_left', 7.5, -10), ('front_right', -7.5, -10), ('back_left', 7.5, 10), ('back_right', -7.5, 10)]
BP = (0, -3, 10)    # body pivot (hind hips)
HP = (0, -10, -18)  # head pivot (neck)
TP = (0, 2, 0)      # torso pivot (under the belly)


class StomperModel(Model):
    """A Model that paints itself (modelkit.render_textures calls render_textures)."""

    def render_textures(self, seed=1):
        scene = Scene(self)
        base = paint_all(self, scene, seed, '')
        exprs = {ex: paint_all(self, scene, seed, ex, base) for ex in EXPRS}
        out = {}
        for vname, pal in (('stomper', NORMAL), ('stomper_white', WHITE)):
            out[vname] = assemble(self, base, pal)
            for ex in EXPRS:
                out[f'{vname}_{ex}'] = assemble(self, exprs[ex], pal)
        return out


def assemble(m, grids, pal):
    img = Image.new('RGBA', (m.tex_w, m.tex_h), (0, 0, 0, 0))
    glow = Image.new('RGBA', (m.tex_w, m.tex_h), (0, 0, 0, 0))
    px, gx = img.load(), glow.load()
    cols = {k: [_rgb(c) for c in v] for k, v in pal.items()}
    any_glow = False
    for (fx, fy, g) in grids:
        for y in range(g.h):
            for x in range(g.w):
                key = g.k[y][x]
                if key is None:
                    continue
                c = cols[key[0]][max(0, min(len(cols[key[0]]) - 1, key[1]))]
                px[fx + x, fy + y] = c
                if (x, y) in g.glow:
                    gx[fx + x, fy + y] = c
                    any_glow = True
    return img, (glow if any_glow else None)


def paint_all(m, scene, seed, expr, base=None):
    """Paints every face: [(x, y, Grid)]. With `base` (the neutral paint) only the faces that change
    with the expression are repainted."""
    out = []
    for i, c in enumerate(m.cubes()):
        for face, (fx, fy, fw, fh) in c.faces().items():
            if fw <= 0 or fh <= 0:
                continue
            fn = c.paint.get('faces', {}).get(face, c.paint.get('all'))
            if fn is None or (base is not None and not getattr(fn, 'expressive', False)):
                continue
            out.append((fx, fy, fn(Ctx(scene, c, face, fw, fh, expr, seed + i * 7))))
    if base is not None:
        rep = {(fx, fy): g for fx, fy, g in out}
        return [(fx, fy, rep.get((fx, fy), g)) for fx, fy, g in base]
    return out


def stomper() -> Model:
    m = StomperModel('stomper', (256, 256), {}, {'stomper': {}, 'stomper_white': {}}, res=1, detail=1)
    m.material_pass = False

    def box(part, piv, origin, size, **paint):
        """A cube given in rest-pose world coordinates, relative to its part's world pivot."""
        part.cube((origin[0] - piv[0], origin[1] - piv[1], origin[2] - piv[2]), size, **paint)

    def rel(p, q):
        return (p[0] - q[0], p[1] - q[1], p[2] - q[2])

    def cross(parent, name, pivot, sprite, rot):
        rows = SPRITES[sprite]
        w, h = len(rows[0]), len(rows)
        front, back = sprite_painter(rows, GK, 'oO')
        p = parent.part(name, pivot=pivot, rot=rot)
        p.cube((-w / 2, -h, 0), (w, h, 0), faces={'north': front, 'south': back}, no_occlude=True)
        p.cube((0, -h, -w / 2), (0, h, w), faces={'east': front, 'west': back}, no_occlude=True)

    body = m.part('body', pivot=BP)
    # the barrel, turf and garden ride on a torso that breathes without moving the legs
    torso = body.part('torso', pivot=rel(TP, BP))
    hide_body = P_hide(0.0, 4.5, 2.0, belly=-4)
    box(torso, TP, (-12, -17, -16), (24, 16, 32), all=hide_body)
    box(torso, TP, (-11, -14, -19), (22, 13, 3), all=hide_body)
    box(torso, TP, (-11, -14, 16), (22, 13, 3), all=hide_body)
    box(torso, TP, (-9, -1, -12), (18, 3, 23), all=P_hide(-0.05, 3.0, belly=-4))
    # a blanket of pink coral turf over the back, shoulders and rump, hanging down in ragged tufts
    box(torso, TP, (-12.5, -17.5, -16.5), (25, 7, 33), faces={'north': P_turf(3, 0), 'south': P_turf(3, 5), 'east': P_turf(3, 2),
                                                              'west': P_turf(3, 7), 'up': P_turf(0, 0, 0.004, 0.06)})
    box(torso, TP, (-11.5, -14.5, -19.5), (23, 5, 4), faces={'north': P_turf(3, 3), 'east': P_turf(2, 1), 'west': P_turf(2, 6),
                                                             'up': P_turf(0, 0, 0.0, 0.08)})
    box(torso, TP, (-11.5, -14.5, 15.5), (23, 5, 4), faces={'south': P_turf(3, 9), 'east': P_turf(2, 4), 'west': P_turf(2, 2),
                                                            'up': P_turf(0, 0, 0.0, 0.08)})
    # the domed back: two layers of turf, a garden on top
    box(torso, TP, (-11, -20, -14), (22, 3, 28), all=P_turf(1, 0, 0.006, 0.07, 0.04))
    box(torso, TP, (-8, -23, -10), (16, 3, 20), all=P_turf(1, 3, 0.008, 0.06, 0.06))
    garden = torso.part('garden', pivot=rel((0, -20, 0), TP))
    for i, (sp, x, z, y, ry) in enumerate(GARDEN):
        cross(garden, f'plant_{i}', (x, y + 20 + 0.5, z), sp, (0, ry, 0))
    tail = torso.part('tail', pivot=rel((0, -11, 19), TP), rot=(0.35, 0, 0))
    tail.cube((-1, 0, -1), (2, 10, 2), all=P_hide(0.05, 2.0, 0.6, ao=0.0))
    tuft = tail.part('tail_tuft', pivot=(0, 9, 0))
    tuft.cube((-1.5, 0, -1.5), (3, 4, 3), all=P_turf(2, 1, light=-0.05))

    head = body.part('head', pivot=rel(HP, BP))
    box(head, HP, (-8, -23, -30), (16, 15, 12), faces={'north': P_hide(0.02, 3.5), 'up': P_hide(), 'down': P_hide(), 'south': P_hide(),
                                                         'east': P_hide(0.02, 4.0, decal=eye, expressive=True),
                                                         'west': P_hide(0.02, 4.0, decal=eye, expressive=True)})
    box(head, HP, (-9, -14, -28), (18, 6, 9), all=P_hide(0.0, 3.0))
    box(head, HP, (-6, -21, -31), (12, 5, 1), all=P_hide(0.05, 2.5))
    box(head, HP, (-4.5, -17, -33), (9, 9, 3), all=P_hide(0.02, 2.0, 0.6))
    # a little turf cap on its crown, its fringe hanging over the brow like a forelock, a teal tuft on top
    box(head, HP, (-8.5, -26, -30.5), (17, 5, 12), faces={'north': P_turf(3, 4), 'east': P_turf(3, 1), 'west': P_turf(3, 8),
                                                           'south': P_turf(2, 2), 'up': P_turf(0, 0, 0.01, 0.1)})
    cross(head, 'crown_tuft', rel((2, -26, -24), HP), 'teal', (0, 0.6, 0))
    jaw = head.part('jaw', pivot=rel((0, -8, -25), HP))
    jaw.cube((-4, 0, -6), (8, 2, 7), faces={'up': P_mouth(), 'south': P_mouth(), 'north': P_hide(0.05, 0), 'east': P_hide(0, 0),
                                              'west': P_hide(0, 0), 'down': P_hide(-0.05, 0)})
    for side, sx in (('left', 1), ('right', -1)):
        tusk = head.part(f'{side}_tusk', pivot=rel((4 * sx, -9, -30.5), HP), rot=(0.55, 0, 0))
        tusk.cube((-1, -1, -5), (2, 2, 5), all=P_bone(root=-1.0))
        tip = tusk.part(f'{side}_tusk_tip', pivot=(0, 0, -4.5), rot=(-0.85, 0, 0))
        tip.cube((-1, -1, -3), (2, 2, 3), all=P_bone())
        ear = head.part(f'{side}_ear', pivot=rel((8 * sx, -23, -23), HP), rot=(0, 0.75 * sx, 0.22 * sx))
        # two planes: the coral-pink inside (the outer plane faces forward when the ear is spread) and the hide behind it
        ear.cube((0.6 * sx, 0, 0), (0, 16, 14), all=P_ear(True, 14, 16), no_occlude=True)
        ear.cube((0, 0, 0), (0, 16, 14), all=P_ear(False, 14, 16), no_occlude=True)
    seg = head
    piv = rel((0, -10, -31.5), HP)
    for i, (w, ln, rx) in enumerate(TRUNK):
        seg = seg.part(f'trunk_{i}', pivot=piv, rot=(rx, 0, 0))
        tip_seg = i == len(TRUNK) - 1
        seg.cube((-w / 2, 0, -w / 2), (w, ln + 1, w), all=P_hide(0.04, 2.0, 0.5, ao=0.4, under=0.08, decal=nostrils if tip_seg else None))
        piv = (0, ln, 0)

    for name, x, z in LEGS:
        leg = body.part(f'{name}_leg', pivot=rel((x, -3, z), BP))
        leg.cube((-4, -1, -4), (8, 13, 8), all=P_hide(0.0, 3.0, 1.2))
        shin = leg.part(f'{name}_shin', pivot=(0, 12, 0))
        shin.cube((-3.5, 0, -3.5), (7, 11, 7), all=P_hide(-0.08, 2.5, 0.8))
        foot = shin.part(f'{name}_foot', pivot=(0, 11, 0))
        side = P_hide(-0.06, 0, decal=foot_decal)
        foot.cube((-4, 0, -4), (8, 4, 8), faces={'north': side, 'south': side, 'east': side, 'west': side, 'up': P_hide(), 'down': P_pad()})
    check_rig()
    return m


def check_rig():
    """StomperRig.java (the grab's trunk rig) must agree with this geometry."""
    import re
    src = open(os.path.join(ROOT, 'src/main/java/com/thesift/entity/StomperRig.java')).read()

    def const(name):
        return float(re.search(name + r' = (-?[0-9.]+)F;', src).group(1))
    want = {'BODY_Y': BP[1], 'BODY_Z': BP[2], 'HEAD_Y': HP[1] - BP[1], 'HEAD_Z': HP[2] - BP[2], 'TRUNK_Y': -10 - HP[1], 'TRUNK_Z': -31.5 - HP[2]}
    for k, v in want.items():
        assert abs(const(k) - v) < 1e-6, f'StomperRig.{k} is {const(k)}, tools/stomper.py has {v}'
    seg = [float(x.rstrip('F')) for x in src.split('SEG = {')[1].split('}')[0].split(', ')]
    rest = [float(x.rstrip('F')) for x in src.split('REST = {')[1].split('}')[0].split(', ')]
    assert seg == [float(t[1]) for t in TRUNK] and rest == [t[2] for t in TRUNK], 'StomperRig SEG/REST differ from TRUNK'


# --------------------------------------------------------------------------- spawn egg
EGG = [
    '.......tT.......',
    '.....pPPPPp.....',
    '....pPPPPPPp....',
    '.ee.hpPPPPph.ee.',
    'eiEhhhhhhhhhhEie',
    'eiEhHhhhhhhhhEie',
    'eiEhgkhhhhkghEie',
    'eiEhHhhhhhhhhEie',
    '.eEhhhhmmhhhhEe.',
    '..Ehhhhmmhhhhh..',
    '...hbhhmmhhbh...',
    '....bhhmmhhb....',
    '.....hhmmhh.....',
    '......dmmd......',
    '.......mm.......',
    '.......dd.......',
]


def spawn_egg():
    """The vanilla per-mob spawn egg style: the egg is the Stomper's head - mauve earth hide, coral
    turf on its crown with a teal tuft, big ears out to the sides with pink insides, small eyes with
    glints, bone tusks and the trunk hanging down the front; outlined in a darker tone of the hide."""
    pal = {'h': '#885868', 'H': '#a87c88', 'd': '#5e3a4a', 'm': '#785060', 'p': '#d85868', 'P': '#f07880', 't': '#2a8c88', 'T': '#50c8b8',
           'e': '#6c4656', 'E': '#5e3a4a', 'i': '#e47e8c', 'g': '#ffffff', 'k': '#120a12', 'b': '#ece4cf'}
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(EGG):
        for x, ch in enumerate(row):
            if ch != '.':
                c = _rgb(pal[ch])
                if ch in 'hm':
                    # lit from the top left, shaded towards the bottom right
                    f = 1.08 if x + y < 11 else (0.84 if x + y > 20 else 1.0)
                    c = (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)), 255)
                px[x, y] = c
    out = img.copy()
    po = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                po[x, y] = _rgb('#3c2234')
    return out


def items():
    """items16.all_items hook: the Stomper's spawn egg."""
    return {'stomper_spawn_egg': spawn_egg()}
