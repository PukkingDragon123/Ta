"""WATER: skins for the water creatures and the Slumbler family, in the Sculk-mob (modelkit) pipeline.

The models are ordinary modelkit Models at res=2 - the automatic x2 detail upsampling and the C2 material
pass then finish them at 4 texels per model unit, exactly like the Jailer, the Sculkling and the Sculk
Parasite. Their cubes carry skin specs instead of flat colours (mat='skin', back=, belly=, marks=, lights=,
tone=, decal=, shape=, ...). finish() works every face out in 3D at the model's texel density and writes it
into the face as an hd map of palette keys:

* ramp shading lit from above (higher is lighter, tops lighter, undersides darker), soft rims and crevice
  shade where another cube sits close in front of a face;
* the material's colours: one 7-tone ramp per role (main, mottle, back, belly, spots, lights, gloss), so
  colour variants are plain palette swaps;
* markings worked out along the body in 3D: mottling, a darker back above a wavy line, a pale belly with
  a seam, spots, stripes and saddles (marks), overlapping scales, pores, warts, wet gloss highlights;
* glowing lights (photophores, veins, rings) on the emissive layer;
* cut-out fin, frill and crest outlines ('shape');
* crisp painted eyes, mouths and slits (decals) that the material noise leaves alone.

The material pass then adds its clustered noise, edge light and crevices on top (map_material=True).
"""
import math

import numpy as np
from PIL import Image

import modelkit as MK

RES = 2  # texels per model unit painted here (x2 more after the auto detail upsampling)


def hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


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


# ---------------------------------------------------------------- materials

class Mat:
    """A painted material (all colours are '#rrggbb' strings, ramps dark -> light).

    ramps: the main colour ramp, then mottle ramps picked by 3D noise (mottle=(cell, threshold), mottle2).
    back/back2: the darker dorsal ramp(s) above the 'back' line of a cube; belly: the underside ramp.
    spots: (ramp or None, cell, threshold, step) blotches; light: the glow ramp of 'lights' texels; gloss:
    one wet-highlight colour (gloss_rate per lit texel). scales: (width, height) of overlapping scales in
    model units; pores: rate of dark pore texels; base/contrast/noise/cell: the light model; rim/ao: rim
    light and crevice shade strength; material/mnoise/edge: the modelkit material pass (name, noise
    strength, edge light); opacity: alpha of every texel (jelly). Any other keyword is a named extra ramp
    for marks (flank, warts, patina...)."""

    def __init__(self, ramps, base=0.55, contrast=1.0, noise=0.09, cell=2.2, mottle=(3.0, 0.58), mottle2=(2.4, 0.72), belly=None,
                 gloss=None, gloss_rate=0.06, spots=None, back=None, back2=None, light=None, rim=1.0, ao=1.0, sym=True, flat=False,
                 material='skin', mnoise=0.7, edge=1.0, scales=None, pores=0.0, opacity=255, **extra):
        self.ramps = [tuple(r) for r in ramps]
        self.base, self.contrast, self.noise, self.cell = base, contrast, noise, cell
        self.mottle, self.mottle2 = mottle, mottle2
        self.belly = None if belly is None else tuple(belly)
        self.back = None if back is None else tuple(back)
        self.back2 = None if back2 is None else tuple(back2)
        self.light = None if light is None else tuple(light)
        self.gloss = gloss
        self.gloss_rate = gloss_rate
        if spots and isinstance(spots[0], (tuple, list)):
            self.spot = tuple(spots[0])
            spots = ('spot',) + tuple(spots[1:])
        self.spots = spots
        self.rim, self.ao, self.sym, self.flat = rim, ao, sym, flat
        self.material, self.mnoise, self.edge = material, mnoise, edge
        self.scales, self.pores, self.opacity = scales, pores, opacity
        for k, v in extra.items():
            setattr(self, k, tuple(v))

    def slots(self):
        """{slot name: colours} of every ramp this material paints with."""
        out = {f'r{i}': r for i, r in enumerate(self.ramps)}
        for k, v in self.__dict__.items():
            if k in ('ramps', 'spots', 'mottle', 'mottle2', 'scales'):
                continue
            if isinstance(v, tuple) and v and isinstance(v[0], str) and v[0].startswith('#'):
                out[k] = v
        if self.gloss:
            out['gloss'] = (self.gloss,)
        return out


# ---------------------------------------------------------------- one face

_GEO = {
    'north': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z0), (x1 - x0, 0, 0), (0, y1 - y0, 0)), (0, 0, -1)),
    'south': (lambda x0, y0, z0, x1, y1, z1: ((x1, y0, z1), (x0 - x1, 0, 0), (0, y1 - y0, 0)), (0, 0, 1)),
    'up': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)), (0, -1, 0)),
    'down': (lambda x0, y0, z0, x1, y1, z1: ((x0, y1, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)), (0, 1, 0)),
    'west': (lambda x0, y0, z0, x1, y1, z1: ((x0, y0, z1), (0, 0, z0 - z1), (0, y1 - y0, 0)), (-1, 0, 0)),
    'east': (lambda x0, y0, z0, x1, y1, z1: ((x1, y0, z0), (0, 0, z1 - z0), (0, y1 - y0, 0)), (1, 0, 0)),
}


def _dims(cube, face):
    w, h, d = (int(math.ceil(s)) for s in cube.size)
    return {'north': (w, h), 'south': (w, h), 'up': (w, d), 'down': (w, d), 'west': (d, h), 'east': (d, h)}[face]


class _Scene:
    def __init__(self, model):
        self.model = model
        self.frames = MK._rest_frames(model)
        ys = []
        for c in model.cubes():
            M, T = self.frames[id(c)]
            for dy in (0, c.size[1]):
                ys.append((M @ np.array([c.origin[0], c.origin[1] + dy, c.origin[2]]) + T)[1])
        self.ymin, self.ymax = min(ys), max(ys)


def _quant(L, n):
    return np.clip(np.floor(L * n), 0, n - 1).astype(int)


def _face(sc, cube, face, mat, spec, seed):
    """Works one face out: returns (slot names, slot id per texel, ramp index per texel, fixed colour
    per texel or None, cut mask, glow mask)."""
    fw, fh = _dims(cube, face)
    fw, fh = fw * RES, fh * RES
    g = cube.inflate
    x0, y0, z0 = (o - g for o in cube.origin)
    x1, y1, z1 = x0 + cube.size[0] + 2 * g, y0 + cube.size[1] + 2 * g, z0 + cube.size[2] + 2 * g
    geo, nrm = _GEO[face]
    corner, du, dv = geo(x0, y0, z0, x1, y1, z1)
    jj, ii = np.mgrid[0:fh, 0:fw]
    u = (ii + 0.5) / fw
    v = (jj + 0.5) / fh
    loc = np.stack([corner[q] + du[q] * u + dv[q] * v for q in range(3)], -1)
    M, T = sc.frames[id(cube)]
    wp = loc @ M.T + T
    wn = M @ np.array(nrm, np.float64)
    vertical = face not in ('up', 'down')
    plane = min(cube.size) == 0
    sym = np.stack([np.abs(wp[..., 0]) if mat.sym else wp[..., 0], wp[..., 1], wp[..., 2]], -1)
    ctx = dict(loc=loc, wp=wp, ii=ii, jj=jj, fw=fw, fh=fh, face=face, res=RES)

    L = np.full((fh, fw), mat.base, np.float32) + spec.get('light', 0.0)
    if not mat.flat:
        hgt = (sc.ymax - wp[..., 1]) / max(1.0, sc.ymax - sc.ymin)
        L += (hgt - 0.5) * 0.22
        L += 0.12 * max(0.0, -wn[1]) - 0.2 * max(0.0, wn[1]) + 0.03 * max(0.0, -wn[2])
        r = mat.rim * spec.get('rim', 1.0)
        if not plane and r:
            if vertical and fh >= 5:
                L += np.where(jj == 0, 0.12 * r, 0) + np.where(jj == 1, 0.05 * r, 0) + np.where(jj == fh - 1, -0.12 * r, 0)
                if fw >= 5:
                    L += np.where((ii == 0) | (ii == fw - 1), -0.04 * r, 0)
            elif face == 'up' and fw >= 5 and fh >= 5:
                L += np.where((ii == 0) | (ii == fw - 1) | (jj == 0) | (jj == fh - 1), 0.05 * r, 0)
            elif face == 'down' and fw >= 5 and fh >= 5:
                L += np.where((ii == 0) | (ii == fw - 1) | (jj == 0) | (jj == fh - 1), -0.05 * r, 0)
        a = mat.ao * spec.get('ao', 1.0)
        if a and not plane:
            L -= MK._crevice(sc.model, cube, face, fw, fh).astype(np.float32) * 0.3 * a
        if mat.noise:
            n = fbm(sym * 2.0 + seed * 0.013, mat.cell, 11 + seed % 5) - 0.5
            L += np.clip(np.round(n * 4.0), -2, 2) * mat.noise * 0.5
    if 'tone' in spec:
        L = L + spec['tone'](ctx)
    L = (L - 0.5) * mat.contrast + 0.5
    ctx['L'] = L

    names = []

    def sid(name):
        if name not in names:
            names.append(name)
        return names.index(name)

    slot = np.zeros((fh, fw), int)
    sid('r0')
    if len(mat.ramps) > 1 and mat.mottle:
        slot = np.where(fbm(sym, mat.mottle[0], 31) > mat.mottle[1], sid('r1'), slot)
    if len(mat.ramps) > 2 and mat.mottle2:
        slot = np.where(fbm(sym + 17.0, mat.mottle2[0], 47) > mat.mottle2[1], sid('r2'), slot)
    idx = _quant(L, 7)
    wav = (fbm(sym + 9.0, 2.0, 71, 1) - 0.5) * 2.2
    # the back: a darker dorsal colour above a wavy line (cube-local y) and on tops
    dors = np.zeros((fh, fw), bool)
    if mat.back is not None and spec.get('back') is not None:
        dors = loc[..., 1] + wav < spec['back']
        if face == 'up':
            dors[:] = True
        mott = slot == (names.index('r1') if 'r1' in names else -1)
        slot = np.where(dors & ~mott, sid('back'), slot)
        if mat.back2 is not None:
            slot = np.where(dors & mott, sid('back2'), slot)
    # scales: overlapping rows, a lit upper edge and a dark curved lower edge on each
    if mat.scales and not plane and spec.get('scales', True):
        sw, sh = mat.scales
        if face in ('east', 'west'):
            pa, pb = loc[..., 2], loc[..., 1]
        elif face in ('north', 'south'):
            pa, pb = loc[..., 0], loc[..., 1]
        else:
            pa, pb = loc[..., 0], loc[..., 2]
        row = np.floor(pb / sh)
        fa = (pa / sw + (row % 2) * 0.5) % 1.0
        fb = (pb / sh) % 1.0
        edge = fb + 0.45 * (1 - ((fa - 0.5) * 2) ** 2) > 1.18
        lit = (fb < 0.34) & (np.abs(fa - 0.45) < 0.28)
        idx = idx + np.where(edge, -1, 0) + np.where(lit & ~edge, 1, 0)
    # spots: pale (or dark) markings worked out in 3D, on the back only when the material has one
    if mat.spots:
        sr, cell, thr, delta = mat.spots
        sp = fbm(sym + 3.3, cell, 59, 1) > thr
        if mat.back is not None:
            sp &= dors
        if spec.get('spots', True):
            if sr is None:
                idx = np.where(sp, np.maximum(0, idx - 2), idx)
                slot = np.where(sp, sid('r0'), slot)
            else:
                # a darker rim round each spot gives it a little depth
                rim = sp & ~(np.roll(sp, -1, 0) & np.roll(sp, -1, 1))
                slot = np.where(sp, sid(sr), slot)
                idx = np.where(sp, idx + delta - rim.astype(int), idx)
    # the belly: below a wavy line on the sides (world y, or cube-local y with 'belly'), and faces turned down
    if mat.belly is not None:
        by, bl = spec.get('belly_y'), spec.get('belly')
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
            slot = np.where(bel, sid('belly'), slot)
            idx = np.where(bel, _quant(L + 0.08, 7), idx)
            if (by is not None or bl is not None) and vertical:
                seam = bel & ~np.roll(bel, 1, 0)
                seam[0] = False
                idx = np.where(seam, idx - 1, idx)
    # marks: extra painted patterns, each (fn(ctx) -> bool mask, ramp name, step): stripes, bands, saddles
    for fn, rp, step in spec.get('marks', ()):
        mk = fn(ctx)
        if mk is not None and np.any(mk):
            mk = np.broadcast_to(mk, (fh, fw))
            slot = np.where(mk, sid(rp), slot)
            idx = np.where(mk, _quant(L, 7) + step, idx)
    # pores: lone darker texels
    if mat.pores and not plane:
        h = _hash3(ii + int(abs(wp[0, 0, 0]) * 7), jj + int(wp[0, 0, 2] * 5 + 50), np.full_like(ii, seed), 177)
        idx = np.where(h < mat.pores, idx - 1, idx)
    # wet highlights: little bright streaks on lit tops and top rows
    if mat.gloss is not None and not plane:
        h = _hash3(ii // 2 + int(abs(wp[0, 0, 0]) * 7), jj + int(wp[0, 0, 2] * 5 + 50), np.full_like(ii, seed), 133)
        lit = (L > 0.66) & ((face == 'up') | (vertical & (jj <= 2)))
        gl = lit & (h < mat.gloss_rate)
        slot = np.where(gl, sid('gloss'), slot)
        idx = np.where(gl, 0, idx)
    glow = np.zeros((fh, fw), bool)
    if spec.get('glow'):
        glow[:] = True
    if spec.get('lights') is not None:
        m = spec['lights'](ctx)
        if m is not None and np.any(m):
            m = np.broadcast_to(m, (fh, fw))
            slot = np.where(m, sid('light'), slot)
            idx = np.where(m, np.clip(np.round((L - 0.3) * 4.5), 0, 3).astype(int), idx)
            glow |= m
    cut = np.zeros((fh, fw), bool)
    if 'shape' in spec:
        cut = ~np.asarray(spec['shape'](loc[..., 0], loc[..., 1], loc[..., 2]), bool)
    # decals: rows of chars at texel scale; keys map a char to '#rrggbb' (a crisp painted colour), a float
    # (the skin under it lighter or darker) or '_' (cut away); glow_keys glow
    fixed = np.full((fh, fw), None, object)
    dec = spec.get('decal')
    if dec:
        keys = spec.get('keys', {})
        gk = set(spec.get('glow_keys', ''))
        rows = [row[::-1] for row in dec] if spec.get('mirror') else list(dec)
        at = spec.get('at', None)
        dw, dh = max(len(rw) for rw in rows), len(rows)
        if at is None:
            ox, oy = (fw - dw) // 2, 0
        elif at == 'bottom':
            ox, oy = (fw - dw) // 2, fh - dh
        else:
            ox, oy = at
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                x, y = ox + i, oy + j
                if ch in '. ' or not (0 <= x < fw and 0 <= y < fh):
                    continue
                if ch == '_':
                    cut[y, x] = True
                    continue
                val = keys.get(ch, ch)
                if isinstance(val, (int, float)):
                    idx[y, x] += int(round((val - 1.0) * 6))
                    continue
                if isinstance(val, str) and not val.startswith('#'):
                    slot[y, x] = sid(val)
                    idx[y, x] = int(np.clip(L[y, x] * 7, 0, 6))
                    continue
                fixed[y, x] = val
                if ch in gk:
                    glow[y, x] = True
    return names, slot, idx, fixed, cut, glow


# ---------------------------------------------------------------- the model

_SKIN_KEYS = ('mat', 'back', 'belly', 'belly_y', 'belly_all', 'belly_down', 'tone', 'marks', 'lights', 'shape', 'light', 'decal', 'keys',
              'at', 'mirror', 'glow_keys', 'no_occlude', 'ao', 'rim', 'glow', 'spots', 'scales', 'skip', 'opacity')


def finish(model, variants, glow_variants=None, seed=1):
    """Paints a model built with skin specs. variants: {texture name: {material name: Mat}} (the first one
    is the default texture); glow_variants: the variants whose lights glow (default: all) - the others
    keep their lights as plain painted colours and an empty emissive layer. Returns the model, ready for
    modelkit.render_textures (gen_models) and modelkit.preview."""
    vnames = list(variants)
    first = variants[vnames[0]]
    sc = _Scene(model)
    used = set()  # (material, slot) painted somewhere
    fixed_cols = {'fx000000': '#000000'}
    for ci, cube in enumerate(model.cubes()):
        paint = cube.paint
        if 'mat' not in paint:
            continue
        faces_over = paint.get('faces', {})
        out_faces = {}
        for face in _GEO:
            fw, fh = _dims(cube, face)
            if fw <= 0 or fh <= 0:
                continue
            spec = {k: v for k, v in paint.items() if k != 'faces'}
            spec.update(faces_over.get(face, {}))
            if spec.get('skip'):
                out_faces[face] = dict(skip=True)
                continue
            mname = spec['mat']
            mat = first[mname]
            names, slot, idx, fixed, cut, glow = _face(sc, cube, face, mat, spec, seed * 7919 + ci * 31 + list(_GEO).index(face))
            keys, gk, rows = {}, [], []
            lut = {}
            H, W = slot.shape
            img = None
            for y in range(H):
                row = []
                for x in range(W):
                    if cut[y, x]:
                        row.append('_')
                        continue
                    col = fixed[y, x]
                    if col is not None and not glow[y, x]:
                        # a crisp painted texel (eyes, mouths): drawn from a picture the material noise leaves alone
                        if img is None:
                            img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
                        img.putpixel((x, y), hx(col) + (255,))
                        row.append('.')
                        continue
                    if col is not None:
                        key = 'fx' + col.lstrip('#')
                        fixed_cols[key] = col
                    else:
                        sname = names[slot[y, x]]
                        n = len(mat.slots().get(sname, ())) or (4 if sname == 'light' else 7)
                        used.add((mname, sname))
                        key = f'{mname}:{sname}:{int(np.clip(idx[y, x], 0, n - 1))}'
                    ck = (key, bool(glow[y, x]))
                    if ck not in lut:
                        lut[ck] = chr(0x100 + len(lut))
                        keys[lut[ck]] = key
                        if ck[1]:
                            gk.append(lut[ck])
                    row.append(lut[ck])
                rows.append(''.join(row))
            fs = dict(color='fx000000', pattern='mc', clusters=0.0, rim=False, hd=True, map=rows, keys=keys, glow_keys=''.join(gk),
                      map_material=True, material=mat.material, noise=mat.mnoise, edge_light=mat.edge, opacity=mat.opacity)
            if img is not None:
                fs.update(image=img, image_mode='stretch', image_cut=False)
            out_faces[face] = fs
        rest = {k: v for k, v in paint.items() if k not in _SKIN_KEYS and k != 'faces'}
        cube.paint = dict(rest, color='fx000000', pattern='mc', clusters=0.0, rim=False, faces=out_faces)
    # one palette per variant: every used slot as 7 (lights: 4) tones; a slot a variant lacks borrows its main ramp
    pals = {}
    for vn in vnames:
        mats = variants[vn]
        pal = {k: v for k, v in fixed_cols.items()}
        for mname, sname in sorted(used):
            mat = mats[mname]
            cols = mat.slots().get(sname)
            if cols is None:
                cols = mat.ramps[0][-4:] if sname == 'light' else mat.ramps[0]
            alpha = '' if mat.opacity >= 255 else f'{mat.opacity:02x}'
            for i in range(max(7, len(cols))):
                pal[f'{mname}:{sname}:{i}'] = cols[min(i, len(cols) - 1)] + alpha
        pals[vn] = pal
    model.palette = dict(pals[vnames[0]])
    model.variants = {vn: pals[vn] for vn in vnames}
    if glow_variants is not None:
        glowing = set(glow_variants)

        def render(s=1):
            out = {}
            for vn, ov in model.variants.items():
                pal = dict(model.palette)
                pal.update(ov)
                img, g = MK.Painter(model, pal, s).paint_all()
                if vn not in glowing:
                    g = Image.new('RGBA', img.size, (0, 0, 0, 0))
                out[vn] = (img, g)
            return out
        model.render_textures = render
    return model


def up(rows, k=RES):
    """Unit-scale decal rows at texel scale (every char k x k)."""
    return [''.join(ch * k for ch in r) for r in rows for _ in range(k)]
