"""W-deep caves: the art for tools/caves.py - crystal dripstone in three colours, the Cave Jungle's plants and acid,
the Sculk Caves' writhing sculk, swaying tendrils and the Sculk Grasper, and the acid particles.

Every block texture starts from its closest vanilla Mojang texture (same 16x16 layout and pixel clusters) and is
re-themed by a luminance-rank gradient map onto a Sift palette, then hand-finished (glints, glowing veins, sparks,
drops). They are final when written, so tools/vanilla_remap.py skips them (SKIP). Animated textures (writhing sculk,
tendrils, acid) keep vanilla's frame-strip format with their .mcmeta.
"""
from __future__ import annotations

import math
import os
import random

import numpy as np
from PIL import Image
import zlib

VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')
CUTOUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}

# crystal colours: dark -> light (7 tones), the glint and the glow they cast
CRYSTALS = {
    'rose': ['#4e1840', '#7d2862', '#ab3f85', '#d260a8', '#ec88c6', '#f9b3df', '#ffe2f3'],
    'azure': ['#0f2a55', '#18477f', '#2368ad', '#3790d2', '#5fb9ea', '#9cdcf8', '#def6ff'],
    'amber': ['#4f2606', '#80440d', '#ad6917', '#d59127', '#efb846', '#fad784', '#fff1c9'],
}
SCULK = ['#020b0e', '#05181e', '#0a2830', '#103a44', '#17505a', '#21707a', '#2f9aa0']
SCULK_GLOW = ['#1f8f92', '#36c3c0', '#5cebe0', '#b6fff6']
BONE = ['#6f7a72', '#9aa59a', '#c3cbbe', '#e4e9dc']
ACID = ['#1d2a06', '#34480b', '#527012', '#78a01c', '#a3cc2c', '#c9ec4f', '#effd9a']
LEAF = ['#0c2422', '#123632', '#1a4b44', '#246157', '#33796b', '#4b9481']
ELECTRIC = ['#1b2a6e', '#2c49a8', '#3f7ad8', '#5fb4f2', '#9be4ff', '#e6fbff', '#fffbd0']
DULL = ['#1c2333', '#283246', '#36435b', '#465570', '#5a6a84', '#73839c']


# ------------------------------------------------------------------ basics

def _hx(h):
    h = h.lstrip('#')
    return np.array([int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)], dtype=np.float64)


def _load(rel):
    return np.asarray(Image.open(os.path.join(VANILLA, rel + '.png')).convert('RGBA'), dtype=np.float64)


def _img(a):
    return Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA')


def _lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def _rank(v):
    """Rank of each value in [0, 1]; equal values share their average rank."""
    v = np.asarray(v, dtype=np.float64)
    if v.size <= 1:
        return np.full(v.shape, 0.5)
    u, inv, cnt = np.unique(v, return_inverse=True, return_counts=True)
    avg = (np.cumsum(cnt) - cnt) + (cnt - 1) / 2.0
    return avg[inv.reshape(-1)].reshape(v.shape) / (v.size - 1)


def gmap(a, ramp, mask=None, lo=0.0, hi=1.0, gamma=1.0):
    """Gradient-maps the masked pixels of `a` onto `ramp` by luminance rank (Mojang's value structure, our palette)."""
    out = a.copy()
    m = (a[..., 3] > 0) if mask is None else mask
    if not m.any():
        return out
    t = _rank(_lum(a)[m]) ** gamma
    t = lo + (hi - lo) * t
    cols = np.array([_hx(c) for c in ramp])
    idx = np.clip(np.round(t * (len(cols) - 1)).astype(int), 0, len(cols) - 1)
    out[..., :3][m] = cols[idx]
    return out


def _put(a, x, y, c, alpha=255):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x, :3] = _hx(c) if isinstance(c, str) else c
        a[y, x, 3] = alpha


def _frames(a):
    w = a.shape[1]
    return [a[i * w:(i + 1) * w].copy() for i in range(a.shape[0] // w)]


def _strip(frames):
    return np.concatenate(frames, 0)


# ------------------------------------------------------------------ crystal dripstone

def _facets(a, ramp, seed, glints=3):
    """Crystal finish on a re-themed sprite: a lit left rim and a shaded right rim on every row of the shape, a few
    bright facet edges inside and small white-hot glints."""
    rnd = random.Random(seed)
    m = a[..., 3] > 0
    h, w = m.shape
    light, dark = _hx(ramp[-2]), _hx(ramp[1])
    for y in range(h):
        xs = np.nonzero(m[y])[0]
        if len(xs) >= 3:
            a[y, xs[0], :3] = a[y, xs[0], :3] * 0.4 + light * 0.6
            a[y, xs[-1], :3] = a[y, xs[-1], :3] * 0.5 + dark * 0.5
    ys, xs = np.nonzero(m)
    L = _lum(a)
    if len(ys):
        order = np.argsort(-L[ys, xs])
        for i in order[:max(1, len(order) // 14)]:
            a[ys[i], xs[i], :3] = a[ys[i], xs[i], :3] * 0.5 + light * 0.5
        for _ in range(glints):
            i = rnd.randrange(len(ys))
            a[ys[i], xs[i], :3] = _hx(ramp[-1])
    return a


def crystal_block(color):
    """The crystal dripstone block: vanilla amethyst's faceted crystal mass with a faint trace of dripstone's flow,
    kept a shade darker than the clusters and spikes that grow from it."""
    ramp = CRYSTALS[color]
    drip, ame = _load('block/dripstone_block'), _load('block/amethyst_block')
    mix = ame.copy()
    lum = _rank(_lum(drip).ravel()).reshape(16, 16) * 0.2 + _rank(_lum(ame).ravel()).reshape(16, 16) * 0.8
    mix[..., 0] = mix[..., 1] = mix[..., 2] = lum * 255.0
    a = gmap(mix, ramp, lo=0.1, hi=0.82)
    rnd = random.Random(len(color) * 31)
    # crystal veins catching the light: short diagonal facets and a few white glints
    for _ in range(4):
        x, y = rnd.randrange(1, 14), rnd.randrange(1, 14)
        for k in range(rnd.randrange(2, 4)):
            _put(a, x + k, y - k, ramp[5])
        _put(a, x - 1, y + 1, ramp[2])
    for _ in range(3):
        _put(a, rnd.randrange(16), rnd.randrange(16), ramp[6])
    return a


def pointed_crystal(color, part):
    """One piece of a pointed crystal (part like 'down_tip'): vanilla pointed dripstone's silhouette, cut as a crystal."""
    ramp = CRYSTALS[color]
    a = gmap(_load(f'block/pointed_dripstone_{part}'), ramp, lo=0.12, hi=0.92)
    return _facets(a, ramp, zlib.crc32(f'{color}/{part}'.encode()) & 0xffff, glints=2 if 'tip' in part else 3)


def crystal_cluster(color):
    ramp = CRYSTALS[color]
    a = gmap(_load('block/amethyst_cluster'), ramp, lo=0.08, hi=1.0, gamma=0.9)
    return a


def pointed_crystal_item(color):
    ramp = CRYSTALS[color]
    a = gmap(_load('item/pointed_dripstone'), ramp, lo=0.1, hi=0.95)
    return _facets(a, ramp, len(color), glints=2)


# ------------------------------------------------------------------ the Cave Jungle

def _hue_sat(a):
    rgb = a[..., :3] / 255.0
    mx, mn = rgb.max(-1), rgb.min(-1)
    d = mx - mn + 1e-9
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) * 60.0
    s = np.where(mx > 0, (mx - mn) / (mx + 1e-9), 0)
    return h, s


def shocker_plant(charged):
    """The Shocker: vanilla torchflower's stalk and leaves gone deep jungle-teal, its flame a crackling electric bulb."""
    a = _load('block/torchflower')
    m = a[..., 3] > 0
    h, s = _hue_sat(a)
    bloom = m & (((h < 70) | (h > 330)) & (s > 0.35))
    leaves = m & ~bloom
    a = gmap(a, LEAF, mask=leaves, lo=0.0, hi=1.0)
    a = gmap(a, ELECTRIC if charged else DULL, mask=bloom, lo=0.15 if charged else 0.1, hi=1.0 if charged else 0.85, gamma=0.8)
    ys, xs = np.nonzero(bloom)
    if len(ys):
        top = ys.min()
        cx = int(round(xs.mean()))
        if charged:
            # a white-hot core and forked sparks jumping off the bulb
            _put(a, cx, top + 2, '#fffef0')
            _put(a, cx, top + 3, '#fff6a8')
            _put(a, cx - 1, top + 2, '#e6fbff')
            for x, y in ((cx - 3, top + 1), (cx - 4, top), (cx - 4, top - 1), (cx + 3, top + 2), (cx + 4, top + 1), (cx + 5, top + 1),
                         (cx + 1, top - 2), (cx + 2, top - 3)):
                if 0 <= y < 16 and 0 <= x < 16 and a[y, x, 3] == 0:
                    _put(a, x, y, '#cff6ff' if (x + y) % 2 else '#fff6a8')
        else:
            _put(a, cx, top + 2, '#8fa3c4')
    return a


def acid_weeper():
    """Acid Weeper: vanilla weeping vines' drooping strands, their buds swollen into glowing sacs of acid, beads of it
    gathering at the tips."""
    src = _load('block/weeping_vines_plant')
    m = src[..., 3] > 0
    h, s = _hue_sat(src)
    L = _lum(src)
    buds = m & (L >= np.quantile(L[m], 0.72))
    a = gmap(src, ACID[0:4], mask=m & ~buds, lo=0.0, hi=1.0)
    a = gmap(a, ACID[4:7], mask=buds, lo=0.0, hi=1.0)
    # trim the strands so the lowest rows are free for the drops
    a[13:, :, 3] = 0
    m = a[..., 3] > 0
    ys, xs = np.nonzero(m)
    bottom = {}
    for y, x in zip(ys, xs):
        bottom[x] = max(bottom.get(x, -1), y)
    for x, y in sorted(bottom.items(), key=lambda kv: (-kv[1], kv[0]))[:4:2]:
        _put(a, x, y + 1, ACID[5])
        _put(a, x, y + 2, ACID[6], 235)
    return a


def acid_puddle_frames():
    """A shallow acid puddle: vanilla still water's ripples in acid green, bubbles welling up and popping."""
    still = _load('block/water_still')
    src = _frames(still)
    n = 16
    rnd = random.Random(11)
    bubbles = [(rnd.randrange(1, 15), rnd.randrange(1, 15), rnd.randrange(n), rnd.choice((4, 5, 6))) for _ in range(7)]
    out = []
    for f in range(n):
        fr = src[(f * len(src)) // n]
        a = gmap(fr, ACID[1:6], lo=0.0, hi=1.0)
        a[..., 3] = 205
        for bx, by, start, life in bubbles:
            t = (f - start) % n
            if t < life:
                r = 0 if t < life // 2 else 1
                c = ACID[6] if t < life - 1 else ACID[4]
                if r == 0:
                    _put(a, bx, by, c, 240)
                else:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        _put(a, bx + dx, by + dy, c, 235)
        out.append(a)
    return _strip(out)


# ------------------------------------------------------------------ the Sculk Caves

def writhing_sculk():
    """Writhing Sculk: vanilla sculk's breathing frames, with waves of light running along its veins."""
    src = _frames(_load('block/sculk'))
    n = 16
    out = []
    base0 = src[0]
    L0 = _lum(base0)
    vein = L0 >= np.quantile(L0, 0.82)
    yy, xx = np.mgrid[0:16, 0:16]
    for f in range(n):
        fr = src[(f * len(src)) // n]
        a = gmap(fr, SCULK, lo=0.0, hi=0.9)
        wave = 0.5 + 0.5 * np.sin(2 * math.pi * f / n - (xx * 0.9 + yy * 0.6) * 0.55)
        glow = np.array([_hx(c) for c in SCULK_GLOW])
        k = np.clip((wave * (len(glow) - 0.01)).astype(int), 0, len(glow) - 1)
        a[..., :3] = np.where(vein[..., None], glow[k] * (0.55 + 0.45 * wave[..., None]) + a[..., :3] * (0.45 - 0.45 * wave[..., None]),
                              a[..., :3])
        out.append(a)
    return _strip(out)


def _tendril_frame(f, n, tip, seed):
    """One frame of a hanging sculk tendril (tip at the bottom when `tip`): two twisting strands swaying with the frame."""
    a = np.zeros((16, 16, 4))
    # (x, phase, sway, width): a thick ribbed main tendril and a thinner one twisting round it
    strands = [(6.0, 0.0, 1.0, 2), (10.5, 2.1, 0.8, 1)]
    phase = 2 * math.pi * f / n
    for sx, ph, amp, width in strands:
        end = 12 if tip and width == 1 else (13 if tip else 16)
        last = None
        for y in range(end):
            sway = amp * 1.7 * math.sin(phase + ph + y * 0.36) * (0.3 + y / 16.0)
            x = int(round(sx + sway + 0.5 * math.sin(y * 0.9 + ph)))
            rib = (y + int(sx)) % 3 == 0
            _put(a, x - 1, y, SCULK[0])
            _put(a, x, y, SCULK[5] if rib else SCULK[4])
            if width == 2:
                _put(a, x + 1, y, SCULK[3] if rib else SCULK[2])
                _put(a, x + 2, y, SCULK[0])
                if (y + seed) % 4 == 1:
                    _put(a, x + 1, y, SCULK_GLOW[1 + (f + y) % 2])
            last = (x, y)
        if tip and last is not None and width == 2:
            x, y = last
            # a hooked tip with a glowing bud, grasping at whatever passes
            for dx, dy, c in ((0, 1, SCULK[4]), (1, 1, SCULK_GLOW[1]), (1, 2, SCULK_GLOW[3]), (2, 2, SCULK_GLOW[2]), (0, 2, SCULK[2]),
                              (2, 1, SCULK[0]), (-1, 1, SCULK[0])):
                if 0 <= y + dy < 16:
                    _put(a, x + dx, y + dy, c)
    return a


def sculk_tendril(tip, hanging):
    n = 8
    frames = [_tendril_frame(f, n, tip, 17 + tip) for f in range(n)]
    if not hanging:
        frames = [fr[::-1].copy() for fr in frames]
    return _strip(frames)


def grasper_textures():
    """The Sculk Grasper: a squat sculk pod with a bone-ringed maw and a crown of feelers (closed / open and glowing)."""
    o = {}
    sculk = gmap(_frames(_load('block/sculk'))[0], SCULK, lo=0.05, hi=0.85)
    side = sculk.copy()
    rnd = random.Random(23)
    for x in range(16):
        # a pale, ribbed bony lip at the top of the pod's walls (rows 10-11 show on the 6-px pod)
        _put(side, x, 10, BONE[2] if x % 3 else BONE[3])
        _put(side, x, 11, BONE[1] if x % 2 else SCULK[2])
    for _ in range(6):
        _put(side, rnd.randrange(16), rnd.randrange(12, 16), SCULK_GLOW[rnd.randrange(2)])
    o['sculk_grasper_side'] = side
    o['sculk_grasper_bottom'] = sculk.copy()
    for active in (False, True):
        top = sculk.copy()
        yy, xx = np.mgrid[0:16, 0:16]
        d = np.hypot(xx - 7.5, yy - 7.5)
        ring = (d > 4.3) & (d < 5.6)
        mouth = d <= 4.3
        top[ring, :3] = _hx(BONE[2])
        if active:
            k = np.clip(((4.3 - d[mouth]) / 4.3 * 3.99).astype(int), 0, 3)
            top[mouth, :3] = np.array([_hx(c) for c in SCULK_GLOW])[k]
        else:
            top[mouth, :3] = _hx(SCULK[1])
            for x in range(4, 12):
                _put(top, x, 7 + (x % 2), SCULK[0])
                _put(top, x, 8 - (x % 2), SCULK_GLOW[0])
        # teeth pointing into the maw
        for ang in range(0, 360, 40):
            r = math.radians(ang)
            for rr, c in ((4.0, BONE[3]), (3.2, BONE[2] if active else BONE[1])):
                _put(top, int(round(7.5 + math.cos(r) * rr)), int(round(7.5 + math.sin(r) * rr)), c)
        o['sculk_grasper_top' + ('_active' if active else '')] = top
        fr = np.zeros((16, 16, 4))
        feelers = (2, 5, 8, 11, 14) if active else (4, 7, 10, 12)
        for i, fx in enumerate(feelers):
            length = 9 + (i * 5) % 5 if active else 6 + (i * 3) % 4
            for k in range(length):
                y = 15 - k
                x = int(round(fx + (math.sin(k * 0.6 + i) * (1.5 if active else 0.8))))
                _put(fr, x, y, SCULK[4] if k % 3 else SCULK[3])
                _put(fr, x - 1, y, SCULK[2])
            ty = 15 - length
            _put(fr, int(round(fx + math.sin(length * 0.6 + i) * (1.5 if active else 0.8))), ty,
                 SCULK_GLOW[3] if active else SCULK_GLOW[1])
        o['sculk_grasper_feelers' + ('_active' if active else '')] = fr
    return o


def grasper_tendril():
    """The lunging tendril's skin (drawn by SculkGrasperRenderer): ribbed dark sculk with glowing pores, along v."""
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    for y in range(16):
        for x in range(16):
            band = (y % 4 == 0)
            c = SCULK[2] if band else SCULK[3 + (x * 7 + y * 3) % 2]
            a[y, x, :3] = _hx(c)
    rnd = random.Random(29)
    for _ in range(10):
        _put(a, rnd.randrange(16), rnd.randrange(16), SCULK_GLOW[rnd.randrange(1, 4)])
    return a


def grasper_claw():
    """The tendril's grasping tip: a bony, hooked claw (two crossed cut-out planes in the renderer)."""
    a = np.zeros((16, 16, 4))
    # a fleshy sculk knuckle on the left (where the tendril joins) opening into two bony, hooked talons (tips at the right)
    for y in range(5, 11):
        for x in range(0, 5):
            if (x - 1.5) ** 2 / 9 + (y - 7.5) ** 2 / 10 <= 1.0:
                _put(a, x, y, SCULK[4] if (x + y) % 3 else SCULK[3])
    _put(a, 2, 7, SCULK_GLOW[2])
    _put(a, 3, 8, SCULK_GLOW[3])
    for side, sgn in ((0, -1), (1, 1)):
        for x in range(4, 15):
            t = (x - 4) / 10.0
            y = 7.5 + sgn * (1.5 + 4.5 * math.sin(t * math.pi * 0.85))
            thick = 2 if t < 0.6 else 1
            for k in range(thick):
                _put(a, x, int(round(y)) - sgn * k, BONE[3] if k == 0 else BONE[1])
            if t > 0.85:
                _put(a, x, int(round(y)) - sgn, BONE[2])
    return a


# ------------------------------------------------------------------ particles

def acid_drip():
    a = np.zeros((8, 8, 4))
    for x, y, c in ((3, 2, ACID[6]), (3, 3, ACID[5]), (4, 3, ACID[4]), (2, 4, ACID[4]), (3, 4, ACID[5]), (4, 4, ACID[5]), (5, 4, ACID[3]),
                    (2, 5, ACID[3]), (3, 5, ACID[4]), (4, 5, ACID[4]), (5, 5, ACID[3]), (3, 6, ACID[3]), (4, 6, ACID[2])):
        _put(a, x, y, c)
    return a


def acid_fizz(size):
    a = np.zeros((8, 8, 4))
    c = 3.5
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - c, y - c)
            if abs(d - size) < 0.6:
                _put(a, x, y, ACID[6] if (x + y) % 3 else ACID[5], 220)
    return a


# ------------------------------------------------------------------ everything

def all_textures():
    """name -> (RGBA array, mcmeta or None); names are paths under textures/."""
    o = {}
    for c in CRYSTALS:
        o[f'block/{c}_crystal_dripstone'] = (crystal_block(c), None)
        for d in ('down', 'up'):
            for part in ('tip', 'tip_merge', 'frustum', 'middle', 'base'):
                o[f'block/pointed_{c}_crystal_{d}_{part}'] = (pointed_crystal(c, f'{d}_{part}'), None)
        o[f'block/{c}_crystal_cluster'] = (crystal_cluster(c), CUTOUT)
        o[f'item/pointed_{c}_crystal'] = (pointed_crystal_item(c), None)
    o['block/shocker_plant'] = (shocker_plant(False), CUTOUT)
    o['block/shocker_plant_charged'] = (shocker_plant(True), CUTOUT)
    o['block/acid_weeper'] = (acid_weeper(), CUTOUT)
    o['block/acid_puddle'] = (acid_puddle_frames(), {'animation': {'frametime': 3}})
    o['block/writhing_sculk'] = (writhing_sculk(), {'animation': {'frametime': 3, 'interpolate': True}})
    for tip in (False, True):
        for hanging in (True, False):
            name = f"block/sculk_tendril_{'hang' if hanging else 'up'}_{'tip' if tip else 'body'}"
            o[name] = (sculk_tendril(tip, hanging), {'animation': {'frametime': 4, 'interpolate': True}})
    for k, v in grasper_textures().items():
        o[f'block/{k}'] = (v, CUTOUT if 'feelers' in k else None)
    o['entity/sculk_grasper/tendril'] = (grasper_tendril(), None)
    o['entity/sculk_grasper/claw'] = (grasper_claw(), None)
    o['particle/acid_drip'] = (acid_drip(), None)
    for i, s in enumerate((1.0, 2.0, 3.0)):
        o[f'particle/acid_fizz_{i}'] = (acid_fizz(s), None)
    return o


def textures(out):
    """gen_textures.main() hook (before vanilla_remap): writes every cave texture; they are final, so the remap skips them."""
    import vanilla_remap as VR
    for name, (arr, meta) in all_textures().items():
        out(name, _img(arr), meta)
        if name.startswith('block/'):
            VR.SKIP.add(name[len('block/'):])
