"""INS free play: every instrument as a real 3D item model - its inventory icon, how it is carried, how it is held
while you play it, and the animation frames of its moving parts.

A small modelling kit: an instrument is a list of boxes (``El``) made of named materials. Every visible face gets
its own patch of one texture per instrument, painted texel by texel from the face's real 3D position by a
material painter (wood grain that runs along the instrument, brushed brass, twisted gut, mottled hide, glass,
pearl...), so grain and decorations (rosettes, sound holes, inlays, painted motifs) flow across the faces.
Faces that look alike can share a patch (``key``) to keep the textures small.

Per instrument ``build_all()`` returns the base elements plus the frames of its moving parts (strings that
vibrate, drum heads that dip and spring back, chimes that swing, flute keys that close), and ``assets(GA)``
writes:
  * ``models/item/<id>.json``            - the model at rest (inventory icon, ground, item frame, carried)
  * ``models/item/<id>_play.json``      - the same instrument held up to play (third / first person)
  * ``models/item/<id>_play_<f>.json``  - each frame of its moving parts while playing
  * ``items/<id>.json``                 - display context select -> using_item -> thesift:instrument_play
The client property ``thesift:instrument_play`` (client/music/InstrumentPlayProperty) picks the frame from the
player's latest notes; ``client/music/InstrumentPoses`` poses the arms and ``client/music/FreePlayHand`` places the
instrument in first person.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))

NS = 'thesift'
TEX_DIR = 'item/instrument'
# texels per model unit for the instrument textures (vanilla items are 1; the Sculk mobs 4)
DENSITY = 8


# ============================================================================ noise

def _hash(ix, iy, iz, seed):
    h = (ix * 374761393 + iy * 668265263 + iz * 1442695041 + seed * 2654435761) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h = h ^ (h >> 16)
    return (h & 0xFFFFFF) / float(0xFFFFFF)


def vnoise(x, y, z, seed=0):
    """Smooth 3D value noise in 0..1 (numpy arrays)."""
    x = np.asarray(x, dtype=np.float64)
    y = np.asarray(y, dtype=np.float64)
    z = np.asarray(z, dtype=np.float64)
    xi, yi, zi = np.floor(x).astype(np.int64), np.floor(y).astype(np.int64), np.floor(z).astype(np.int64)
    xf, yf, zf = x - xi, y - yi, z - zi
    u, v, w = xf * xf * (3 - 2 * xf), yf * yf * (3 - 2 * yf), zf * zf * (3 - 2 * zf)
    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                wt = (u if dx else 1 - u) * (v if dy else 1 - v) * (w if dz else 1 - w)
                out = out + wt * _hash(xi + dx, yi + dy, zi + dz, seed)
    return out


def fbm(x, y, z, seed=0, octaves=3):
    tot, amp, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        f = 2.0 ** o
        tot = tot + amp * vnoise(x * f, y * f, z * f, seed + o * 31)
        norm += amp
        amp *= 0.5
    return tot / norm


def white(x, y, z, seed=0):
    """Per-texel speckle (stable for a texel's position)."""
    return _hash(np.floor(np.asarray(x) * 97.0).astype(np.int64), np.floor(np.asarray(y) * 97.0).astype(np.int64),
                 np.floor(np.asarray(z) * 97.0).astype(np.int64), seed)


def hexc(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float64) / 255.0


def mix(a, b, t):
    t = np.asarray(t)[..., None]
    return a * (1 - t) + b * t


def ramp(stops, t):
    """Colour ramp: stops = [(pos, '#hex'), ...] sorted by pos."""
    t = np.clip(np.asarray(t, dtype=np.float64), 0, 1)
    out = np.zeros(t.shape + (3,))
    pos = [p for p, _ in stops]
    cols = [hexc(c) for _, c in stops]
    for i in range(len(stops) - 1):
        p0, p1 = pos[i], pos[i + 1]
        m = (t >= p0) & (t <= p1)
        k = np.where(m, (t - p0) / max(1e-9, p1 - p0), 0)
        seg = cols[i] * (1 - k[..., None]) + cols[i + 1] * k[..., None]
        out = np.where(m[..., None], seg, out)
    out = np.where((t < pos[0])[..., None], cols[0], out)
    out = np.where((t > pos[-1])[..., None], cols[-1], out)
    return out


def smooth(e0, e1, x):
    t = np.clip((np.asarray(x) - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


# ============================================================================ materials
# A painter takes the texel centres' model coordinates (X, Y, Z arrays), and a context (face, element, the
# texel's distance to the face's border in texels) and returns RGB (h, w, 3) in 0..1.

class Ctx:
    def __init__(self, face, el, edge, s, t):
        self.face, self.el, self.edge, self.s, self.t = face, el, edge, s, t


def _across(X, Y, Z, axis):
    """A coordinate across the grain: the sum of the two coordinates that are not the grain's axis."""
    return {'x': Y + Z, 'y': X + Z * 0.85, 'z': X + Y * 0.85}[axis]


def wood(base, dark, light, axis='y', lines=2.2, warp=0.5, seed=1, contrast=1.0, figure=0.0):
    b, d, l = hexc(base), hexc(dark), hexc(light)

    def paint(X, Y, Z, c):
        A = {'x': X, 'y': Y, 'z': Z}[axis]
        B = _across(X, Y, Z, axis)
        wv = fbm(A * 0.18, B * 0.6, 0.0, seed, 3)
        g = B * lines + (wv - 0.5) * warp * 6.0
        ring = 0.5 + 0.5 * np.sin(g * 2 * np.pi)
        ring = ring ** 4
        fine = fbm(A * 0.9, B * 9.0, 0.0, seed + 7, 2)
        col = mix(b, d, np.clip(ring * 0.55 * contrast + (fine - 0.5) * 0.35, 0, 1))
        col = mix(col, l, np.clip((fine - 0.62) * 1.6 * contrast, 0, 1) * 0.6)
        if figure:
            fig = 0.5 + 0.5 * np.sin(A * 3.1 + fbm(A * 0.5, B * 0.5, 1.0, seed + 3) * 6.0)
            col = mix(col, l, (fig ** 6) * figure)
        sp = white(X, Y, Z, seed)
        col = col * (0.94 + 0.08 * sp[..., None])
        return col
    return paint


def metal(base, hi, lo, axis='y', seed=2, brushed=0.5, glint=0.6):
    b, h, l = hexc(base), hexc(hi), hexc(lo)

    def paint(X, Y, Z, c):
        A = {'x': X, 'y': Y, 'z': Z}[axis]
        B = _across(X, Y, Z, axis)
        streak = fbm(A * 0.2, B * 14.0, 0.0, seed, 2)
        col = mix(b, l, np.clip((0.5 - streak) * brushed * 1.6, 0, 1))
        col = mix(col, h, np.clip((streak - 0.5) * brushed * 1.8, 0, 1))
        # a soft highlight band across each face, brightest near its top-left
        band = np.exp(-((c.s * 0.8 + c.t * 0.6 - 0.45) ** 2) / 0.035)
        col = mix(col, h, band * glint * 0.55)
        # darker toward the face's lower edge
        col = mix(col, l, np.clip(c.t - 0.55, 0, 1) * 0.6)
        return col
    return paint


def gut(base, dark, seed=3, twist=True):
    b, d = hexc(base), hexc(dark)

    def paint(X, Y, Z, c):
        A = X + Y + Z
        tw = 0.5 + 0.5 * np.sin(A * 9.0 * np.pi) if twist else 0.5
        col = mix(b, d, np.clip(tw * 0.45 + (white(X, Y, Z, seed) - 0.5) * 0.15, 0, 1))
        return col
    return paint


def hide(base, dark, light, seed=4, centre=None, radius=1.0, rim='#8a6a48'):
    b, d, l, r = hexc(base), hexc(dark), hexc(light), hexc(rim)

    def paint(X, Y, Z, c):
        n = fbm(X * 0.9, Z * 0.9, Y * 0.9, seed, 4)
        fib = fbm(X * 4.0, Z * 4.0, Y * 0.5, seed + 11, 2)
        col = mix(b, d, np.clip((0.55 - n) * 1.6, 0, 1) * 0.55)
        col = mix(col, l, np.clip((fib - 0.6) * 2.2, 0, 1) * 0.5)
        if centre is not None:
            cx, cz = centre
            rr = np.sqrt((X - cx) ** 2 + (Z - cz) ** 2) / radius
            col = mix(col, r, smooth(0.78, 1.0, rr) * 0.75)
            # the worn, darker playing spot
            col = mix(col, d, np.exp(-((rr - 0.35) ** 2) / 0.02) * 0.18)
        return col
    return paint


def leather(base, dark, seed=5, stitch=None):
    b, d = hexc(base), hexc(dark)

    def paint(X, Y, Z, c):
        n = fbm(X * 2.5, Y * 2.5, Z * 2.5, seed, 3)
        pebble = white(X * 0.5, Y * 0.5, Z * 0.5, seed)
        col = mix(b, d, np.clip((0.6 - n) * 1.3 + (pebble - 0.5) * 0.25, 0, 1))
        if stitch:
            st = hexc(stitch)
            on = (c.edge >= 1.0) & (c.edge < 2.0) & ((np.floor((c.s + c.t) * 24) % 2) == 0)
            col = np.where(on[..., None], st, col)
        return col
    return paint


def bamboo(base, dark, light, axis='y', nodes=(), seed=6):
    b, d, l = hexc(base), hexc(dark), hexc(light)

    def paint(X, Y, Z, c):
        A = {'x': X, 'y': Y, 'z': Z}[axis]
        B = _across(X, Y, Z, axis)
        fib = fbm(A * 0.3, B * 12.0, 0.0, seed, 2)
        col = mix(b, l, np.clip((fib - 0.5) * 1.5, 0, 1) * 0.6)
        col = mix(col, d, np.clip((0.4 - fib) * 1.5, 0, 1) * 0.4)
        for nd in nodes:
            k = np.exp(-((A - nd) ** 2) / 0.02)
            col = mix(col, d, k * 0.75)
            k2 = np.exp(-((A - nd - 0.25) ** 2) / 0.02)
            col = mix(col, l, k2 * 0.5)
        return col
    return paint


def solid(base, var=0.06, seed=7, dark=None):
    b = hexc(base)
    dk = hexc(dark) if dark else b * 0.7

    def paint(X, Y, Z, c):
        n = fbm(X * 1.5, Y * 1.5, Z * 1.5, seed, 3)
        col = mix(b, dk, np.clip((0.5 - n) * 2.0 * var * 4, 0, 1))
        return col * (1 - var + var * 2 * white(X, Y, Z, seed)[..., None])
    return paint


def glass(base, edge, core, seed=8):
    b, e, k = hexc(base), hexc(edge), hexc(core)

    def paint(X, Y, Z, c):
        side = np.minimum(c.s, 1 - c.s)
        col = mix(e, b, smooth(0.0, 0.35, side))
        col = mix(col, k, np.exp(-((c.s - 0.32) ** 2) / 0.01) * 0.8)
        n = fbm(X * 2, Y * 0.5, Z * 2, seed, 2)
        return col * (0.92 + 0.12 * n[..., None])
    return paint


def pearl(seed=9, tint=0.5):
    def paint(X, Y, Z, c):
        h = (X * 0.07 + Y * 0.05 + Z * 0.06 + fbm(X * 0.6, Y * 0.6, Z * 0.6, seed, 2) * 0.6) % 1.0
        r = 0.86 + 0.12 * np.sin(h * 2 * np.pi)
        g = 0.86 + 0.12 * np.sin(h * 2 * np.pi + 2.1)
        bb = 0.92 + 0.08 * np.sin(h * 2 * np.pi + 4.2)
        col = np.stack([r, g, bb], -1)
        col = mix(col, hexc('#ffffff'), np.exp(-((c.s * 0.7 + c.t * 0.7 - 0.4) ** 2) / 0.03) * 0.6)
        return col * (1 - tint * 0.06 * c.t[..., None])
    return paint


def gem(base, hi, lo, seed=10):
    b, h, l = hexc(base), hexc(hi), hexc(lo)

    def paint(X, Y, Z, c):
        facet = (np.floor(c.s * 3) + np.floor(c.t * 3)) % 3 / 2.0
        col = mix(l, b, 0.4 + 0.5 * facet)
        col = mix(col, h, np.exp(-((c.s - 0.3) ** 2 + (c.t - 0.3) ** 2) / 0.03))
        return col
    return paint


def rope(base, dark, seed=11):
    b, d = hexc(base), hexc(dark)

    def paint(X, Y, Z, c):
        tw = 0.5 + 0.5 * np.sin((c.s * 3 + c.t * 6) * np.pi * 2)
        return mix(b, d, tw * 0.6)
    return paint


MATERIALS = {}


def mat(name, painter):
    MATERIALS[name] = painter
    return name


# the shared palette of real materials
mat('spruce', wood('#f6cc84', '#cf9c58', '#feeac2', 'y', lines=2.6, warp=0.35, seed=11, contrast=0.7))
mat('maple', wood('#e8c890', '#b98f58', '#fbe7c0', 'y', lines=1.8, warp=0.5, seed=12, contrast=0.6, figure=0.35))
mat('mahogany', wood('#7a3a20', '#3e180a', '#a65a32', 'y', lines=1.6, warp=0.8, seed=13, contrast=0.9))
mat('walnut', wood('#5e3a22', '#2e1a0e', '#8a5a34', 'y', lines=1.4, warp=0.9, seed=14))
mat('rosewood', wood('#3a2216', '#1c0f08', '#5e3824', 'y', lines=3.5, warp=0.4, seed=15, contrast=0.9))
mat('oak', wood('#b88a52', '#7e5a30', '#d8b078', 'y', lines=1.8, warp=0.7, seed=16))
mat('oak_x', wood('#b88a52', '#7e5a30', '#d8b078', 'x', lines=1.8, warp=0.7, seed=17))
mat('lullwood', wood('#5fb3a6', '#2f6d6a', '#9fe3cf', 'y', lines=2.0, warp=0.6, seed=18))
mat('ebony', wood('#2a1e1c', '#120a0a', '#4a3632', 'y', lines=3.0, warp=0.5, seed=19, contrast=0.7))
mat('lacquer', solid('#b0322a', 0.05, 20, '#6a1610'))
mat('lacquer_dark', solid('#701a14', 0.05, 21, '#3a0a08'))
mat('copper', metal('#e0905e', '#ffdab8', '#94522e', 'y', 22))
mat('copper_x', metal('#e0905e', '#ffdab8', '#94522e', 'x', 23))
mat('brass', metal('#e2b452', '#fff4c0', '#8a6420', 'y', 24))
mat('brass_x', metal('#e2b452', '#fff4c0', '#8a6420', 'x', 25))
mat('gold', metal('#f8cf4c', '#fffae0', '#ac7c1a', 'y', 26, glint=0.8))
mat('gold_x', metal('#f8cf4c', '#fffae0', '#ac7c1a', 'x', 27, glint=0.8))
mat('iron', metal('#d6dae2', '#ffffff', '#7c828e', 'y', 28))
mat('iron_x', metal('#d6dae2', '#ffffff', '#7c828e', 'x', 29))
mat('silver', metal('#eef4fb', '#ffffff', '#9aaac0', 'y', 30, glint=0.9))
mat('silver_x', metal('#eef4fb', '#ffffff', '#9aaac0', 'x', 31, glint=0.9))
mat('dark_iron', metal('#4a4e58', '#9aa0ac', '#22252c', 'y', 32))
mat('gut', gut('#f1e3c2', '#b8a27a', 33))
mat('wire', gut('#e8edf4', '#8f9aac', 34))
mat('silk', gut('#c8fbff', '#58d8e8', 35))
mat('bone', solid('#ece0c4', 0.05, 36, '#b8a888'))
mat('ivory', solid('#f6eedb', 0.04, 37, '#cdbf9e'))
mat('hole', solid('#1a100a', 0.05, 38, '#000000'))
mat('felt', solid('#3a2a2e', 0.08, 39, '#1c1418'))
mat('leather', leather('#7a4826', '#4a2a14', 40, stitch='#e8d8b0'))
mat('strap', leather('#5a3218', '#341c0c', 41, stitch='#d8c08a'))
mat('rope', rope('#e2cfa0', '#9a8456', 42))
mat('bamboo', bamboo('#c9c46e', '#7a7a32', '#e8e4a0', 'y', (), 43))
mat('bamboo_x', bamboo('#c9c46e', '#7a7a32', '#e8e4a0', 'x', (), 44))
mat('amethyst', gem('#b48cf0', '#f4e8ff', '#5a3a9a', 45))
mat('chime_glass', glass('#bff2ff', '#5fc6e0', '#ffffff', 46))
mat('pearl', pearl(47))
mat('sculk', solid('#123038', 0.1, 48, '#061418'))
mat('sculk_glow', solid('#2ef2e2', 0.08, 49, '#14a8a0'))
mat('chitin', solid('#1a1c26', 0.12, 50, '#06070c'))
mat('prism', gem('#f59af0', '#ffffff', '#8a4ad8', 51))
mat('prism_cyan', gem('#7ff5ff', '#ffffff', '#2a9ad8', 52))
mat('star', gem('#fff2a0', '#ffffff', '#e0a020', 53))


# ============================================================================ the kit

class El:
    """One box. ``mat`` names a material (``mats`` overrides it per face); ``faces`` limits which faces exist;
    ``key`` shares a texture patch between faces that look alike; ``tag`` marks a moving part for the frames;
    ``rot`` = (axis, angle, origin); ``paint`` = an extra painter applied after the material (decorations)."""

    def __init__(self, frm, to, mat, tag=None, rot=None, glow=0, shade=True, faces=None, mats=None, key=None, paint=None, keys=None):
        self.frm, self.to = [float(v) for v in frm], [float(v) for v in to]
        self.mat, self.tag, self.rot, self.glow, self.shade = mat, tag, rot, glow, shade
        self.faces = faces or ('north', 'south', 'west', 'east', 'up', 'down')
        self.mats = mats or {}
        self.key = key
        self.keys = keys or {}
        self.paint = paint
        self.uv = {}

    def size(self, face):
        (x0, y0, z0), (x1, y1, z1) = self.frm, self.to
        if face in ('north', 'south'):
            return x1 - x0, y1 - y0
        if face in ('west', 'east'):
            return z1 - z0, y1 - y0
        return x1 - x0, z1 - z0

    def points(self, face, s, t):
        """Model coordinates of the face texels (s: 0..1 left->right seen from outside, t: 0..1 top->bottom)."""
        (x0, y0, z0), (x1, y1, z1) = self.frm, self.to
        one = np.ones_like(s)
        if face == 'north':
            return x1 - s * (x1 - x0), y1 - t * (y1 - y0), z0 * one
        if face == 'south':
            return x0 + s * (x1 - x0), y1 - t * (y1 - y0), z1 * one
        if face == 'west':
            return x0 * one, y1 - t * (y1 - y0), z0 + s * (z1 - z0)
        if face == 'east':
            return x1 * one, y1 - t * (y1 - y0), z1 - s * (z1 - z0)
        if face == 'up':
            return x0 + s * (x1 - x0), y1 * one, z0 + t * (z1 - z0)
        return x0 + s * (x1 - x0), y0 * one, z1 - t * (z1 - z0)

    def copy(self):
        e = El(self.frm, self.to, self.mat, self.tag, self.rot, self.glow, self.shade, self.faces, dict(self.mats), self.key, self.paint,
               dict(self.keys))
        e.uv = dict(self.uv)
        return e

    def moved(self, dx=0.0, dy=0.0, dz=0.0):
        e = self.copy()
        e.frm = [self.frm[0] + dx, self.frm[1] + dy, self.frm[2] + dz]
        e.to = [self.to[0] + dx, self.to[1] + dy, self.to[2] + dz]
        if e.rot:
            ax, ang, o = e.rot
            e.rot = (ax, ang, (o[0] + dx, o[1] + dy, o[2] + dz))
        return e

    def json(self, tex='#t'):
        d = {'from': [round(v, 4) for v in self.frm], 'to': [round(v, 4) for v in self.to], 'faces': {}}
        for f in self.faces:
            if f in self.uv:
                d['faces'][f] = {'uv': [round(v, 4) for v in self.uv[f]], 'texture': tex}
        if self.rot:
            ax, ang, o = self.rot
            d['rotation'] = {'origin': [round(v, 4) for v in o], 'axis': ax, 'angle': round(ang, 3)}
        if self.glow:
            d['light_emission'] = self.glow
        if not self.shade:
            d['shade'] = False
        return d


def _pack(rects, width, height):
    """Shelf packer: rects = [(w, h, id)] in texels -> {id: (x, y)} or None if they do not fit."""
    out = {}
    x = y = shelf = 0
    for w, h, rid in sorted(rects, key=lambda r: (-r[1], -r[0])):
        if w > width:
            return None
        if x + w > width:
            x = 0
            y += shelf
            shelf = 0
        if y + h > height:
            return None
        out[rid] = (x, y)
        x += w
        shelf = max(shelf, h)
    return out


class Instrument3D:
    """One instrument: its elements, its texture, and how to write its models."""

    def __init__(self, iid, els, density=DENSITY):
        self.iid = iid
        self.els = els
        self.density = density
        self.texture = None
        self.tex_size = 0
        self._layout()

    def _face_px(self, el, face):
        w, h = el.size(face)
        return max(1, int(math.ceil(w * self.density - 1e-6))), max(1, int(math.ceil(h * self.density - 1e-6)))

    def _layout(self):
        # every face (or shared key) gets a patch with a 1-texel gutter all round
        patches = {}
        owners = {}
        for i, el in enumerate(self.els):
            for f in el.faces:
                w, h = self._face_px(el, f)
                k = el.keys.get(f) or (f'{el.key}:{f in ("up", "down")}:{w}x{h}' if el.key else f'{i}:{f}')
                if k not in patches:
                    patches[k] = (w + 2, h + 2)
                    owners[k] = (el, f)
                el._patch = getattr(el, '_patch', {})
                el._patch[f] = k
        # the smallest texture (square, or twice as wide as tall) the patches fit
        sizes = []
        s = 32
        while s <= 1024:
            sizes += [(s, s // 2), (s, s)]
            s *= 2
        for tw, th in sizes:
            placed = _pack([(w, h, k) for k, (w, h) in patches.items()], tw, th)
            if placed is not None:
                break
        else:
            raise ValueError(f'{self.iid}: faces do not fit a 1024 texture')
        self.tex_size = (tw, th)
        self.patches = {k: (placed[k][0] + 1, placed[k][1] + 1, patches[k][0] - 2, patches[k][1] - 2) for k in patches}
        self.owners = owners
        ux, uy = tw / 16.0, th / 16.0
        for el in self.els:
            for f in el.faces:
                x, y, w, h = self.patches[el._patch[f]]
                el.uv[f] = (x / ux, y / uy, (x + w) / ux, (y + h) / uy)

    def paint(self):
        if self.texture is not None:
            return self.texture
        tw, th = self.tex_size
        img = np.zeros((th, tw, 4), dtype=np.float64)
        for k, (x, y, w, h) in self.patches.items():
            el, face = self.owners[k]
            jj, ii = np.mgrid[0:h, 0:w]
            s = (ii + 0.5) / w
            t = (jj + 0.5) / h
            X, Y, Z = el.points(face, s, t)
            edge = np.minimum(np.minimum(ii, w - 1 - ii), np.minimum(jj, h - 1 - jj)).astype(np.float64)
            c = Ctx(face, el, edge, s, t)
            m = el.mats.get(face, el.mat)
            col = MATERIALS[m](X, Y, Z, c)
            alpha = np.ones((h, w))
            if el.paint is not None:
                res = el.paint(col, X, Y, Z, c)
                if isinstance(res, tuple):
                    col, alpha = res
                else:
                    col = res
            # a darker rim on every face, a little light along its top edge: crisp, readable edges
            if w >= 3 and h >= 3 and not el.glow:
                col = np.where((edge < 1)[..., None], col * 0.84, col)
                col = np.where(((jj == 0) & (ii > 0) & (ii < w - 1))[..., None], np.minimum(1, col * 1.1), col)
            patch = np.concatenate([np.clip(col, 0, 1), alpha[..., None]], -1)
            img[y:y + h, x:x + w] = patch
            # gutters: repeat the edge texels so mipmaps do not bleed
            img[y - 1, x:x + w] = patch[0]
            img[y + h, x:x + w] = patch[-1]
            img[y - 1:y + h + 1, x - 1] = img[y - 1:y + h + 1, x]
            img[y - 1:y + h + 1, x + w] = img[y - 1:y + h + 1, x + w - 1]
        self.texture = Image.fromarray((img * 255).round().astype(np.uint8), 'RGBA')
        return self.texture

    def tex_ref(self):
        return f'{NS}:{TEX_DIR}/{self.iid}'

    def model(self, els, display, gui_light='side'):
        return {'textures': {'t': self.tex_ref(), 'particle': self.tex_ref()}, 'gui_light': gui_light,
                'elements': [e.json() for e in els], 'display': display}


# ============================================================================ shape helpers

ROUND = {
    # half extents (fractions of the radius) of the axis-aligned boxes whose union is a stepped circle
    2: [(1.0, 0.55), (0.55, 1.0)],
    3: [(1.0, 0.5), (0.5, 1.0), (0.82, 0.82)],
    5: [(1.0, 0.38), (0.92, 0.62), (0.78, 0.78), (0.62, 0.92), (0.38, 1.0)],
}


def cyl_y(cx, cz, r, y0, y1, mat, n=5, tag=None, caps=('up', 'down'), mats=None, paint=None, glow=0, shade=True, rot=None, key=None):
    """A round body along y: n axis-aligned boxes whose union is a stepped circle (no z-fighting: every box's caps
    sit a hair apart, and its sides lie in planes of their own). Painters see true positions, so staves, hoops and
    heads can be painted by angle and radius."""
    out = []
    for i, (a, b) in enumerate(ROUND[n]):
        dy = i * 0.003
        faces = ('north', 'south', 'west', 'east') + tuple(caps)
        out.append(El((cx - a * r, y0 - (dy if 'down' in caps else 0), cz - b * r), (cx + a * r, y1 + (dy if 'up' in caps else 0), cz + b * r),
                      mat, tag=tag, faces=faces, mats=dict(mats or {}), paint=paint, glow=glow, shade=shade, rot=rot, key=key))
    return out


def cyl_x(cy, cz, r, x0, x1, mat, n=3, tag=None, caps=('west', 'east'), mats=None, paint=None, glow=0, rot=None, key=None):
    out = []
    for i, (a, b) in enumerate(ROUND[n]):
        dx = i * 0.003
        faces = ('north', 'south', 'up', 'down') + tuple(caps)
        out.append(El((x0 - (dx if 'west' in caps else 0), cy - a * r, cz - b * r), (x1 + (dx if 'east' in caps else 0), cy + a * r, cz + b * r),
                      mat, tag=tag, faces=faces, mats=dict(mats or {}), paint=paint, glow=glow, rot=rot, key=key))
    return out


def cyl_z(cx, cy, r, z0, z1, mat, n=3, tag=None, caps=('north', 'south'), mats=None, paint=None, glow=0, rot=None, key=None):
    out = []
    for i, (a, b) in enumerate(ROUND[n]):
        dz = i * 0.003
        faces = ('west', 'east', 'up', 'down') + tuple(caps)
        out.append(El((cx - a * r, cy - b * r, z0 - (dz if 'north' in caps else 0)), (cx + a * r, cy + b * r, z1 + (dz if 'south' in caps else 0)),
                      mat, tag=tag, faces=faces, mats=dict(mats or {}), paint=paint, glow=glow, rot=rot, key=key))
    return out


def profile_slab(cx, rows, z0, z1, mat, tag=None, mats=None, paint=None, key=None):
    """A flat rounded outline as stacked boxes: rows = [(y0, y1, half_width)]. Inner faces are dropped."""
    out = []
    for i, (y0, y1, hw) in enumerate(rows):
        faces = ['north', 'south', 'west', 'east']
        below = rows[i - 1][2] if i > 0 else 0
        above = rows[i + 1][2] if i + 1 < len(rows) else 0
        if hw > below - 1e-6:
            faces.append('down')
        if hw > above - 1e-6:
            faces.append('up')
        out.append(El((cx - hw, y0, z0), (cx + hw, y1, z1), mat, tag=tag, faces=tuple(faces), mats=dict(mats or {}), paint=paint, key=key))
    return out


# ============================================================================ particles

def particle_textures():
    """White sprites the particles tint: an eighth note, two beamed notes, a soft ring of sound, a wisp of breath."""
    out = {}

    def glyph(mask_fn, size=16):
        yy, xx = np.mgrid[0:size, 0:size] + 0.5
        m = mask_fn(xx, yy).astype(float)
        # a soft dark rim one texel out, so the note reads on bright skies
        pad = np.pad(m, 1)
        near = np.zeros_like(m)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                near = np.maximum(near, pad[1 + dy:1 + dy + size, 1 + dx:1 + dx + size])
        rim = np.clip(near - m, 0, 1)
        shade = 0.82 + 0.18 * (1 - yy / size)
        rgb = np.stack([m * shade + rim * 0.25] * 3, -1)
        a = np.clip(m + rim * 0.55, 0, 1)
        return Image.fromarray((np.concatenate([rgb, a[..., None]], -1) * 255).astype(np.uint8), 'RGBA')

    def note(xx, yy):
        head = ((xx - 6.0) / 2.6) ** 2 + ((yy - 11.5) / 2.0) ** 2 < 1.0
        stem = (xx > 7.6) & (xx < 9.0) & (yy > 2.5) & (yy < 11.5)
        flag = (xx > 8.5) & (xx < 12.5) & (yy > 2.5 + (xx - 8.5) * 0.8) & (yy < 5.0 + (xx - 8.5) * 0.9)
        return head | stem | flag

    def notes(xx, yy):
        h1 = ((xx - 4.5) / 2.3) ** 2 + ((yy - 12.0) / 1.8) ** 2 < 1.0
        h2 = ((xx - 11.5) / 2.3) ** 2 + ((yy - 10.5) / 1.8) ** 2 < 1.0
        s1 = (xx > 6.0) & (xx < 7.3) & (yy > 3.6) & (yy < 12.0)
        s2 = (xx > 13.0) & (xx < 14.3) & (yy > 2.2) & (yy < 10.5)
        beam = (xx > 6.0) & (xx < 14.3) & (np.abs(yy - (4.4 - (xx - 6.0) * 0.17)) < 1.3)
        return h1 | h2 | s1 | s2 | beam

    out['instrument_note'] = glyph(note)
    out['instrument_notes'] = glyph(notes)
    size = 32
    yy, xx = np.mgrid[0:size, 0:size] + 0.5
    r = np.sqrt((xx - size / 2) ** 2 + (yy - size / 2) ** 2) / (size / 2)
    ring = np.exp(-((r - 0.82) ** 2) / 0.006) + 0.35 * np.exp(-((r - 0.62) ** 2) / 0.004)
    a = np.clip(ring, 0, 1)
    out['sound_ring'] = Image.fromarray((np.stack([np.ones_like(a)] * 3 + [a], -1) * 255).astype(np.uint8), 'RGBA')
    size = 16
    yy, xx = np.mgrid[0:size, 0:size] + 0.5
    r = np.sqrt((xx - size / 2) ** 2 + (yy - size / 2) ** 2) / (size / 2)
    puff = np.clip(np.exp(-(r ** 2) / 0.35) * (0.8 + 0.2 * vnoise(xx * 0.5, yy * 0.5, 0.0, 5)), 0, 1)
    out['instrument_breath'] = Image.fromarray((np.stack([np.ones_like(puff)] * 3 + [puff], -1) * 255).astype(np.uint8), 'RGBA')
    return out


# ============================================================================ placing the instrument (display transforms)
# Minecraft's chains, as matrices (block units). Third person: the entity root (y flipped, facing +z), the arm's
# pivot and rotation (ModelPart: rotationZYX), the ItemInHandLayer's turn and nudge, then the model's display
# transform (translate, rotationXYZ, scale, -0.5). First person: camera space, then FreePlayHand's translate.

D2R = math.pi / 180.0


def _T(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def _RX(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], dtype=float)


def _RY(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], dtype=float)


def _RZ(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], dtype=float)


ROOT = _RY(math.pi) @ np.diag([-1.0, -1.0, 1.0, 1.0]) @ _T(0, -1.501, 0)
ARM_PIVOT = {'right': (-5.0, 2.0, 0.0), 'left': (5.0, 2.0, 0.0)}


def arm_chain(rot, arm='right'):
    """World <- item frame of a hand holding an item (third person), for arm angles rot = (x, y, z) radians."""
    p = ARM_PIVOT[arm]
    m = ROOT @ _T(p[0] / 16, p[1] / 16, p[2] / 16) @ _RZ(rot[2]) @ _RY(rot[1]) @ _RX(rot[0])
    return m @ _RX(-math.pi / 2) @ _RY(math.pi) @ _T((1 if arm == 'right' else -1) / 16.0, 2 / 16.0, -10 / 16.0)


def hand_point(rot, arm='right'):
    """World position of the bottom-front of the fist (where a held item's origin sits)."""
    return (arm_chain(rot, arm) @ np.array([0, 0, 0, 1.0]))[:3]


def basis(up, front):
    """Rotation whose columns are where the model's x, y (up) and z (front) axes should point."""
    y = np.array(up, dtype=float)
    y /= np.linalg.norm(y)
    z = np.array(front, dtype=float)
    z = z - y * (z @ y)
    z /= np.linalg.norm(z)
    x = np.cross(y, z)
    return np.stack([x, y, z], 1)


def euler_xyz(R):
    """Angles (degrees) a, b, c with R = Rx(a) Ry(b) Rz(c) (JOML rotationXYZ)."""
    b = math.asin(max(-1.0, min(1.0, R[0, 2])))
    a = math.atan2(-R[1, 2], R[2, 2])
    c = math.atan2(-R[0, 1], R[0, 0])
    return [round(a / D2R, 3), round(b / D2R, 3), round(c / D2R, 3)]


def solve_display(chain, R_world, anchor, target, scale):
    """The display transform that puts model point ``anchor`` (0..16 units) at ``target`` (chain space) with the
    model's axes along R_world's columns, at ``scale``."""
    Cr = chain[:3, :3]
    Rd = np.linalg.inv(Cr) @ R_world
    U, _, Vt = np.linalg.svd(Rd)
    Rd = U @ Vt
    Ci = np.linalg.inv(chain)
    tgt = (Ci @ np.append(np.asarray(target, dtype=float), 1.0))[:3]
    q = np.asarray(anchor, dtype=float) / 16.0 - 0.5
    t = tgt - Rd @ (q * scale)
    tr = [round(v * 16, 3) for v in t]
    if max(abs(v) for v in tr) > 80:
        raise ValueError(f'display translation out of range: {tr}')
    return {'rotation': euler_xyz(Rd), 'translation': tr, 'scale': [scale, scale, scale]}


def mirror_left(tr):
    r, t = tr['rotation'], tr['translation']
    return {'rotation': [r[0], -r[1], -r[2]], 'translation': [-t[0], t[1], t[2]], 'scale': tr['scale']}


def bounds(els):
    lo = np.array([min(e.frm[i] for e in els) for i in range(3)])
    hi = np.array([max(e.to[i] for e in els) for i in range(3)])
    return lo, hi


def fit_display(els, rotation, fit, centre=None, max_scale=1.6):
    """A display transform (rotation in degrees XYZ) scaled so the turned instrument's x/y extent fits ``fit``
    units, centred on the item's middle (GUI, ground, frame)."""
    R = (_RX(rotation[0] * D2R) @ _RY(rotation[1] * D2R) @ _RZ(rotation[2] * D2R))[:3, :3]
    pts = []
    for e in els:
        for x in (e.frm[0], e.to[0]):
            for y in (e.frm[1], e.to[1]):
                for z in (e.frm[2], e.to[2]):
                    pts.append(R @ (np.array([x, y, z]) - 8.0))
    pts = np.array(pts)
    lo, hi = pts.min(0), pts.max(0)
    ext = max(hi[0] - lo[0], hi[1] - lo[1])
    s = min(max_scale, fit / ext)
    mid = (lo + hi) / 2 if centre is None else R @ (np.asarray(centre) - 8.0)
    t = -mid * s
    return {'rotation': [float(v) for v in rotation], 'translation': [round(float(v), 3) for v in t], 'scale': [round(s, 4)] * 3}
