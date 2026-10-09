"""S2 art overhaul: hand-painted mob textures at vanilla density (one texel per model unit).

The look of the user's Blockbench references: every face painted like a little picture - a lit top row
and a shaded foot on side faces, painted shadow under overhangs and in crevices, a darker texel where
faces meet, hue-shifted shading (cool violet shadows, warm or cyan lights), clustered material noise in
2-4 texel blobs, mottling and spots that run on across cube seams (they are worked out in 3D, so a
pattern never stops at an edge), wet 1-texel highlights, and crisp hand-placed decals (eyes, mouths,
toes, claws) on top. Every tone comes from a short hand-picked ramp, so the result stays clean pixel
art with no gradients.

Usage: build the model with modelkit (cubes carry `mat=` and optional `faces={face: {...}}` paint
specs instead of modelkit's colours), give materials as {name: Material}, then
`paint(model, materials)` returns the RGBA atlas (model.tex_w x model.tex_h). Models opt in with
`use(model, materials, name)`, which makes modelkit.render_textures call us.

Per-cube / per-face spec keys:
  mat        material name (the face may override it)
  light      added to the painted light (e.g. -0.1 for an inner part)
  ao         multiplier for painted shadow (0 switches it off)
  rim        multiplier for the lit top row / shaded foot (0 off)
  belly_y    model-space y below which side faces turn to the material's belly ramp (a wavy line);
  belly      the same in cube-local y; back: cube-local y above which the material's dorsal ramp is used
  shape      fn(lx, ly, lz) -> bool mask in cube-local units; False texels are cut out (fins, frills)
  tone       fn(ctx) -> array of light offsets (painted ribs, bands)
  decal      rows of characters drawn crisp on the face; keys maps char -> '#rrggbb'; '_' cuts a hole;
             at=(x, y) texel offset, or anchor 'top'/'bottom'/'center' (default top-left)
  opacity    alpha of the painted texels (translucent jelly)
  glow       True: the whole face also goes on the emissive layer
  lights     fn(ctx) -> bool mask of texels repainted from the material's light ramp and made emissive (only
             when the material has a light ramp, so one variant can glow where the others do not)
  glow_keys  decal characters that glow
  marks      [(fn(ctx) -> bool mask, ramp or material ramp name, step)]: stripes and patches in another ramp
  mirror     True draws the decal mirrored (for the right-hand side of a symmetric pair)
"""
import math

import numpy as np
from PIL import Image

from modelkit import _rest_frames


def hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*cols):
    return np.array([hx(c) for c in cols], np.float32)


class Material:
    """A painted material.

    ramps: list of tone ramps (dark -> light, same length); a mottle noise picks between them (the
    first is the main colour). belly: an optional ramp for the underside. gloss: '#rrggbb' wet highlight
    texels on lit tops. spots: (ramp_index_or_None, cell, threshold, darken) blotches worked out in 3D.
    base: the middle light (0..1); contrast: how strongly light moves along the ramp; noise: cluster
    noise strength; cell: cluster size in texels; mottle: (cell, threshold) for the second ramp.
    """

    def __init__(self, ramps, base=0.55, contrast=1.0, noise=0.09, cell=2.2, mottle=(3.0, 0.58), mottle2=(2.4, 0.72), belly=None,
                 gloss=None, gloss_rate=0.06, spots=None, speck=None, ao=1.0, rim=1.0, sym=True, flat=False, back=None, back2=None, light=None):
        self.ramps = [r if isinstance(r, np.ndarray) else ramp(*r) for r in ramps]
        self.base, self.contrast, self.noise, self.cell = base, contrast, noise, cell
        self.mottle, self.mottle2 = mottle, mottle2
        self.belly = belly if belly is None or isinstance(belly, np.ndarray) else ramp(*belly)
        self.gloss = None if gloss is None else np.array(hx(gloss), np.float32)
        self.gloss_rate = gloss_rate
        if spots and isinstance(spots[0], (tuple, list)):
            spots = (ramp(*spots[0]),) + tuple(spots[1:])
        self.spots = spots
        self.speck = speck
        self.ao, self.rim, self.sym, self.flat = ao, rim, sym, flat
        self.back = None if back is None else ramp(*back)
        self.back2 = None if back2 is None else ramp(*back2)
        self.light = None if light is None else ramp(*light)


# ---------------------------------------------------------------- 3D noise

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


# ---------------------------------------------------------------- geometry of a face

_GEO = {
    'north': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z0), (x1 - x0, 0, 0), (0, y1 - y0, 0)), (0, 0, -1)),
    'south': (lambda x0, y0, z0, x1, y1, z1: ((x1, y0, z1), (x0 - x1, 0, 0), (0, y1 - y0, 0)), (0, 0, 1)),
    'up': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)), (0, -1, 0)),
    'down': (lambda x0, y0, z0, x1, y1, z1: ((x0, y1, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)), (0, 1, 0)),
    'west': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z1), (0, 0, z0 - z1), (0, y1 - y0, 0)), (-1, 0, 0)),
    'east': (lambda x0, y0, z0, x1, y1, z1: ((x1, y0, z0), (0, 0, z1 - z0), (0, y1 - y0, 0)), (1, 0, 0)),
}


def _face_points(cube, face, fw, fh):
    x0, y0, z0 = (o - cube.inflate for o in cube.origin)
    w, h, d = (s + 2 * cube.inflate for s in cube.size)
    corner, du, dv = _GEO[face][0](x0, y0, z0, x0 + w, y0 + h, z0 + d)
    jj, ii = np.mgrid[0:fh, 0:fw]
    u = (ii + 0.5) / fw
    v = (jj + 0.5) / fh
    pts = np.stack([corner[q] + du[q] * u + dv[q] * v for q in range(3)], -1)
    return pts


class _Scene:
    def __init__(self, model):
        self.m = model
        self.frames = _rest_frames(model)
        self.solids = [c for c in model.cubes() if min(c.size) >= 0.9 and not c.paint.get('no_occlude')]
        allp = []
        for c in model.cubes():
            M, T = self.frames[id(c)]
            o = np.array(c.origin, float)
            s = np.array(c.size, float)
            for k in range(8):
                corner = o + s * np.array([(k >> 0) & 1, (k >> 1) & 1, (k >> 2) & 1])
                allp.append(M @ corner + T)
        allp = np.array(allp)
        self.ymin, self.ymax = allp[:, 1].min(), allp[:, 1].max()

    def world(self, cube, pts):
        M, T = self.frames[id(cube)]
        return pts @ M.T + T

    def inside(self, q, skip):
        """True where world points q (N, 3) fall inside any solid cube other than skip."""
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
        """Painted shadow 0..1: crevices (another cube just in front of the face) and overhangs (a cube
        somewhere above it)."""
        shp = wp.shape[:2]
        q0 = wp.reshape(-1, 3)
        occ = np.zeros(len(q0))
        # crevice: straight out from the face
        for dist, wgt in ((0.5, 0.45), (1.5, 0.3), (2.8, 0.15)):
            occ += self.inside(q0 + wn * dist, cube) * wgt
        # overhang: straight up (y is down), from just off the face
        if wn[1] > -0.5:  # not a top face
            base = q0 + wn * 0.45
            sky = np.zeros(len(q0))
            for k, dist in enumerate((1.0, 2.0, 3.5, 5.5, 8.0)):
                sky = np.maximum(sky, self.inside(base + np.array([0, -dist, 0]), cube) * (1.0 - k * 0.12))
            occ += sky * 0.55
        return np.clip(occ, 0, 1).reshape(shp)


def _quant(L, n):
    return np.clip(np.round(L * (n - 1)), 0, n - 1).astype(int)


def _smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


def paint(model, materials, seed=1, glow_out=None):
    """Paints the packed model's atlas (RGBA, tex_w x tex_h). glow_out, if a list, receives the emissive
    layer (only the glowing texels) as its one element."""
    W, H = model.tex_w, model.tex_h
    img = np.zeros((H, W, 4), np.uint8)
    glow = np.zeros((H, W, 4), np.uint8)
    sc = _Scene(model)
    for ci, cube in enumerate(model.cubes()):
        rects = cube.faces()
        for face, (fx, fy, fw, fh) in rects.items():
            if fw <= 0 or fh <= 0:
                continue
            spec = {k: v for k, v in cube.paint.items() if k != 'faces'}
            spec.update(cube.paint.get('faces', {}).get(face, {}))
            if spec.get('skip'):
                continue
            mat = materials[spec.get('mat', 'skin')]
            loc = _face_points(cube, face, fw, fh)
            wp = sc.world(cube, loc)
            M, _ = sc.frames[id(cube)]
            wn = M @ np.array(_GEO[face][1], float)
            rgb, alpha, lit = _paint_face(sc, cube, face, fw, fh, loc, wp, wn, mat, spec, seed + ci * 7)
            if 'decal' in spec:
                _decal(rgb, alpha, spec, fw, fh, lit)
            px = np.clip(np.round(rgb), 0, 255).astype(np.uint8)
            img[fy:fy + fh, fx:fx + fw, :3] = px
            img[fy:fy + fh, fx:fx + fw, 3] = np.where(alpha > 0, spec.get('opacity', 255), 0).astype(np.uint8)
            lit &= alpha > 0
            if lit.any():
                region = glow[fy:fy + fh, fx:fx + fw]
                region[lit, :3] = px[lit]
                region[lit, 3] = 255
    if glow_out is not None:
        glow_out.append(Image.fromarray(glow, 'RGBA'))
    return Image.fromarray(img, 'RGBA')


def _paint_face(sc, cube, face, fw, fh, loc, wp, wn, mat, spec, seed):
    vertical = face not in ('up', 'down')
    jj, ii = np.mgrid[0:fh, 0:fw]
    plane = min(cube.size) == 0
    sym = np.array([abs(wp[..., 0]) if mat.sym else wp[..., 0], wp[..., 1], wp[..., 2]]).transpose(1, 2, 0) if wp.ndim == 3 else wp
    if mat.flat:
        L = np.full((fh, fw), mat.base, np.float32) + spec.get('light', 0.0)
    else:
        L = np.full((fh, fw), mat.base, np.float32) + spec.get('light', 0.0)
        # the creature is lit from above: higher is lighter
        hgt = (sc.ymax - wp[..., 1]) / max(1.0, sc.ymax - sc.ymin)
        L += (hgt - 0.5) * 0.22
        # the face's own turn to the light (top-left, a touch from the front)
        L += 0.12 * max(0.0, -wn[1]) - 0.2 * max(0.0, wn[1]) + 0.03 * max(0.0, -wn[2])
        # rims: a lit top row, a shaded foot, a darker texel where faces meet
        r = mat.rim * spec.get('rim', 1.0)
        if not plane and r:
            if vertical and fh >= 3:
                L += np.where(jj == 0, 0.16 * r, 0) + np.where(jj == 1, 0.05 * r, 0) + np.where(jj == fh - 1, -0.15 * r, 0)
                if fw >= 3:
                    L += np.where((ii == 0) | (ii == fw - 1), -0.05 * r, 0)
            elif face == 'up' and fw >= 3 and fh >= 3:
                L += np.where((ii == 0) | (ii == fw - 1) | (jj == 0) | (jj == fh - 1), 0.06 * r, 0)
            elif face == 'down' and fw >= 3 and fh >= 3:
                L += np.where((ii == 0) | (ii == fw - 1) | (jj == 0) | (jj == fh - 1), -0.05 * r, 0)
        # painted shadow in crevices and under overhangs
        a = mat.ao * spec.get('ao', 1.0)
        if a and not plane:
            L -= sc.occlusion(cube, wp, wn) * 0.42 * a
        # clustered noise, 2-4 texel blobs
        if mat.noise:
            n = fbm(sym + seed * 0.013, mat.cell, 11 + seed % 5) - 0.5
            L += np.clip(np.round(n * 4.0), -2, 2) * mat.noise * 0.5
    if 'tone' in spec:
        L = L + spec['tone'](dict(loc=loc, wp=wp, ii=ii, jj=jj, fw=fw, fh=fh, face=face))
    L = (L - 0.5) * mat.contrast + 0.5
    # which ramp: the main colour, mottled with the second (and third)
    rid = np.zeros((fh, fw), int)
    if len(mat.ramps) > 1 and mat.mottle:
        m1 = fbm(sym, mat.mottle[0], 31)
        rid = np.where(m1 > mat.mottle[1], 1, rid)
    if len(mat.ramps) > 2 and mat.mottle2:
        m2 = fbm(sym + 17.0, mat.mottle2[0], 47)
        rid = np.where(m2 > mat.mottle2[1], 2, rid)
    n = len(mat.ramps[0])
    idx = _quant(L, n)
    rgb = np.zeros((fh, fw, 3), np.float32)
    for k, rp in enumerate(mat.ramps):
        sel = rid == k
        if sel.any():
            rgb[sel] = rp[np.clip(idx[sel], 0, len(rp) - 1)]
    wav = (fbm(sym + 9.0, 2.0, 71, 1) - 0.5) * 2.2
    # the back: a darker dorsal colour above a wavy line (cube-local y, so it follows the part) and on tops
    dors = np.zeros((fh, fw), bool)
    if mat.back is not None and spec.get('back') is not None:
        dors = loc[..., 1] + wav < spec['back']
        if face == 'up':
            dors[:] = True
        if dors.any():
            dk = rid == 1
            rgb[dors & ~dk] = mat.back[np.clip(idx[dors & ~dk], 0, len(mat.back) - 1)]
            if mat.back2 is not None:
                rgb[dors & dk] = mat.back2[np.clip(idx[dors & dk], 0, len(mat.back2) - 1)]
    # spots: pale (or dark) markings worked out in 3D, on the back only when the material has one
    if mat.spots:
        sr, cell, thr, delta = mat.spots
        sp = fbm(sym + 3.3, cell, 59, 1) > thr
        if mat.back is not None:
            sp &= dors
        if sr is None:
            dark = np.maximum(0, idx - 2)
            rgb[sp] = mat.ramps[0][dark[sp]]
        else:
            rgb[sp] = sr[np.clip(idx[sp] + delta, 0, len(sr) - 1)]
    # the belly: below a wavy line on the sides (world y, or cube-local y with 'belly'), and faces turned down
    if mat.belly is not None:
        by = spec.get('belly_y')
        bl = spec.get('belly')
        bel = np.zeros((fh, fw), bool)
        if wn[1] > 0.6 and spec.get('belly_down', True):
            bel[:] = True
        if by is not None:
            bel |= wp[..., 1] + wav > by
        if bl is not None:
            bel |= loc[..., 1] + wav > bl
        if spec.get('belly_all'):
            bel[:] = True
        if bel.any():
            bidx = _quant(L + 0.08, len(mat.belly))
            rgb[bel] = mat.belly[bidx[bel]]
            # a darker seam texel where the belly meets the flank
            if (by is not None or bl is not None) and vertical:
                edge = bel & ~np.roll(bel, 1, 0)
                edge[0] = False
                rgb[edge] = rgb[edge] * 0.84
    # marks: extra painted patterns, each (fn(ctx) -> bool mask, ramp, step): stripes, bands, saddles
    for fn, rp, step in spec.get('marks', ()):
        mk = fn(dict(loc=loc, wp=wp, ii=ii, jj=jj, fw=fw, fh=fh, face=face, L=L))
        if mk is not None and mk.any():
            rr = getattr(mat, rp) if isinstance(rp, str) else rp if isinstance(rp, np.ndarray) else ramp(*rp)
            if rr is None:
                continue
            rgb[mk] = rr[np.clip(idx[mk] + step, 0, len(rr) - 1)]
    # wet highlights: lone bright texels on lit tops and top rows
    if mat.gloss is not None and not plane:
        h = _hash3(ii + int(wp[0, 0, 0] * 7), jj + int(wp[0, 0, 2] * 5), np.full_like(ii, seed), 133)
        lit = (L > 0.7) & ((face == 'up') | (vertical & (jj == 0)))
        g = lit & (h < mat.gloss_rate)
        rgb[g] = mat.gloss
    lit = np.zeros((fh, fw), bool)
    if spec.get('glow'):
        lit[:] = True
    if spec.get('lights') is not None and mat.light is not None:
        m = spec['lights'](dict(loc=loc, wp=wp, ii=ii, jj=jj, fw=fw, fh=fh, face=face, L=L))
        if m is not None and m.any():
            lr = mat.light if mat.light is not None else mat.ramps[0][-3:]
            li = np.clip(np.round((L - 0.35) * len(lr)), 0, len(lr) - 1).astype(int)
            rgb[m] = lr[li[m]]
            lit |= m
    alpha = np.full((fh, fw), 255, np.uint8)
    if 'shape' in spec:
        keep = spec['shape'](loc[..., 0], loc[..., 1], loc[..., 2])
        alpha = np.where(keep, 255, 0).astype(np.uint8)
    return rgb, alpha, lit


def _decal(rgb, alpha, spec, fw, fh, lit=None):
    rows = spec['decal']
    if spec.get('mirror'):
        rows = [r[::-1] for r in rows]
    keys = spec.get('keys', {})
    dw, dh = max(len(r) for r in rows), len(rows)
    at = spec.get('at', 'top')
    if isinstance(at, tuple):
        ox, oy = at
        if spec.get('mirror'):
            ox = fw - dw - ox
    elif at == 'bottom':
        ox, oy = (fw - dw) // 2, fh - dh
    elif at == 'center':
        ox, oy = (fw - dw) // 2, (fh - dh) // 2
    else:
        ox, oy = (fw - dw) // 2, 0
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            x, y = ox + i, oy + j
            if ch in '. ' or not (0 <= x < fw and 0 <= y < fh):
                continue
            if ch == '_':
                alpha[y, x] = 0
                continue
            c = keys[ch]
            if isinstance(c, (int, float)):  # a light multiplier on what is painted
                rgb[y, x] = np.clip(rgb[y, x] * c, 0, 255)
            else:
                rgb[y, x] = hx(c)
            alpha[y, x] = 255
            if lit is not None:
                lit[y, x] = ch in spec.get('glow_keys', '')


def pack(model):
    """Skyline packing of the cubes' box-UV footprints (tighter than modelkit's shelves, so a vanilla
    sized sheet holds a many-cubed model). Big footprints first, each at the lowest, then leftmost spot."""
    W, H = model.tex_w, model.tex_h
    used = np.zeros((H, W), bool)
    cubes = sorted(model.cubes(), key=lambda c: (-c.footprint[0] * c.footprint[1], -c.footprint[1]))
    for c in cubes:
        w, h, d = (int(math.ceil(s)) for s in c.size)
        # the box-UV footprint is an L: the top strip (up and down faces) and the side strip
        mask = np.zeros((d + h, 2 * d + 2 * w), bool)
        mask[:d, d:d + 2 * w] = True
        mask[d:, :] = True
        mh, mw = mask.shape
        if mh == 0 or mw == 0 or not mask.any():
            c.uv = (0, 0)
            continue
        spot = None
        for y in range(0, H - mh + 1):
            for x in range(0, W - mw + 1):
                if not (used[y:y + mh, x:x + mw] & mask).any():
                    spot = (x, y)
                    break
            if spot:
                break
        if spot is None:
            raise ValueError(f'{model.name}: texture {W}x{H} too small')
        x, y = spot
        used[y:y + mh, x:x + mw] |= mask
        c.uv = (x, y)


def use(model, materials, name, seed=1, variants=None, glow_layer=False):
    """Makes modelkit.render_textures paint this model by hand and packs its UVs with the L-shape
    packer. variants: {texture name: materials} for colour variants (default: just `name`);
    glow_layer: always write an emissive layer (empty where nothing glows), for renderers that draw one."""
    def render(s=seed):
        out = {}
        for vname, mats in (variants or {name: materials}).items():
            g = []
            img = paint(model, mats, seed, g)
            has = g[0].getbbox() is not None
            out[vname] = (img, g[0] if (has or glow_layer) else None)
        return out
    model.render_textures = render
    model.pack = lambda: pack(model)
    return model
