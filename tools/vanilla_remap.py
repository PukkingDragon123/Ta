"""C1 Block art: every Sift block, plant and flower texture rebuilt from its closest vanilla Mojang texture.

Runs last in gen_textures.main().  For each Sift block texture the generators just wrote it loads the matching
vanilla 16x16 texture (the material: noise, pixel clusters, contrast and silhouette) and re-themes it with the Sift
texture's own palette, then applies small hand edits (Sift glyphs kept, glow pixels, notes, sculk veins).
The file name, size class and .mcmeta stay the same, so every model keeps working.

Two ways to re-theme:
  clone  vanilla layout + alpha.  Each region of the vanilla texture (whole sprite, k-means colour regions, grass
         overhang, log-top bark ring, ore spots) is luminance-rank mapped onto the palette of the matching region
         of the Sift texture (a gradient map that keeps Mojang's exact value structure).
  mat    Sift layout (designed devices / UV-mapped parts).  Each colour region of the Sift texture keeps its colours
         but they are re-ordered by the vanilla material's luminance, so the faces get Mojang's noise and clusters.
Animated strips keep their frame count (vanilla frames are resampled to it).
"""
import os

import numpy as np
from PIL import Image

VANILLA = os.environ.get('VANILLA_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures/block')
_OLD = {}


# ------------------------------------------------------------------ basics
def _load(path):
    return np.asarray(Image.open(path).convert('RGBA'), dtype=np.float64)


def _ref(name, anim=False):
    a = _load(os.path.join(VANILLA, name + '.png'))
    return a if anim else a[:a.shape[1]]  # animated vanilla strips: first frame unless asked


def _lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def _frames(a):
    w = a.shape[1]
    return [a[i * w:(i + 1) * w] for i in range(max(1, a.shape[0] // w))]


def _fit(r, size):
    """Tiles (or crops) a vanilla frame to size x size."""
    n = int(np.ceil(size / r.shape[0]))
    return np.tile(r, (n, n, 1))[:size, :size]


def _rank(v):
    """Rank of each value in [0,1]; equal values share their average rank (no invented noise)."""
    v = np.asarray(v, dtype=np.float64)
    if v.size <= 1:
        return np.full(v.shape, 0.5)
    u, inv, cnt = np.unique(v, return_inverse=True, return_counts=True)
    avg = (np.cumsum(cnt) - cnt) + (cnt - 1) / 2.0
    return avg[inv.reshape(-1)].reshape(v.shape) / (v.size - 1)


class Pal:
    """The colours of a Sift region sorted by luminance; at(t) picks the colour at luminance quantile t."""

    def __init__(self, rgb, spread=34.0):
        rgb = np.asarray(rgb, dtype=np.float64).reshape(-1, 3)
        if len(rgb) == 0:
            rgb = np.array([[128.0, 128.0, 128.0]])
        L = _lum(rgb)
        o = np.argsort(L, kind='stable')
        self.c, self.L = rgb[o], L[o]
        lo, hi = np.quantile(self.L, [0.1, 0.9])
        self.extra = max(0.0, spread - (hi - lo))  # flat Sift fills get Mojang-like contrast

    def at(self, t):
        t = np.clip(np.asarray(t, dtype=np.float64), 0, 1)
        c = self.c[np.clip(np.round(t * (len(self.c) - 1)).astype(int), 0, len(self.c) - 1)]
        if self.extra > 0:
            d = (np.round(t * 6) / 6 - 0.5)[..., None] * self.extra
            c = c + d * np.where(d < 0, [1.0, 0.95, 0.8], [0.9, 1.0, 1.05])  # cool shadows, warm lights
        return np.clip(c, 0, 255)

    def top(self, q=0.97):
        return self.c[int(q * (len(self.c) - 1))]


def _cdist(a, r1, r2):
    """Colour distance between two regions: hue/chroma counts fully, plain lightness a quarter (a ramp stays one)."""
    c1, c2 = a[r1][:, :3].mean(0), a[r2][:, :3].mean(0)
    l1, l2 = _lum(c1), _lum(c2)
    return np.linalg.norm((c1 - l1) - (c2 - l2)) + 0.25 * abs(l1 - l2)


def _accents(a, m, thr=38.0, minor=True):
    """Pixels off the texture's main ramp (Sift glyphs, gems, glints): minor off-hue colour regions plus
    scattered off-hue specks."""
    L = _lum(a)
    ch = a[..., :3] - L[..., None]
    out = np.zeros_like(m)
    if m.sum() < 8:
        return out
    if minor:
        regs = sorted(_segment(a, m, 3), key=lambda r: -r.sum())
        for r in regs[1:]:
            if r.sum() < 0.45 * m.sum() and _cdist(a, r, regs[0]) > 30:
                out |= r
    m = m & ~out
    if m.sum() < 8:
        return out
    qs = np.quantile(L[m], np.linspace(0, 1, 6))
    b = np.clip(np.searchsorted(qs[1:-1], L), 0, 4)
    med = np.zeros((5, 3))
    allmed = np.median(ch[m], axis=0)
    for i in range(5):
        s = m & (b == i)
        med[i] = np.median(ch[s], axis=0) if s.sum() >= 3 else allmed
    dev = np.linalg.norm(ch - med[b], axis=-1)
    return out | (m & (dev > thr))


def _kmeans(X, k, iters=25):
    """Deterministic k-means (farthest-point init)."""
    if len(X) <= k:
        return np.arange(len(X)) % max(k, 1), X.copy()
    c = [X[np.argmin(X[:, -1])]]
    for _ in range(1, k):
        d = np.min([((X - ci) ** 2).sum(1) for ci in c], axis=0)
        c.append(X[np.argmax(d)])
    c = np.array(c)
    for _ in range(iters):
        lab = np.argmin(((X[:, None, :] - c[None]) ** 2).sum(-1), axis=1)
        for i in range(k):
            if (lab == i).any():
                c[i] = X[lab == i].mean(0)
    return lab, c


def _segment(a, m, k, by='color', merge=30.0):
    """Splits mask m of image a into up to k colour regions (merged when their colours are close)."""
    ys, xs = np.nonzero(m)
    rgb = a[ys, xs, :3]
    L = _lum(rgb)
    if by == 'lum':
        X = L[:, None]
    else:
        X = np.concatenate([(rgb - L[:, None]) * 1.4, L[:, None] * 0.6], 1)
    if k <= 1 or len(rgb) < 2 * k:
        return [m]
    lab, _ = _kmeans(X, k)
    regs = []
    for i in range(k):
        r = np.zeros_like(m)
        r[ys[lab == i], xs[lab == i]] = True
        if r.sum():
            regs.append(r)
    # merge regions whose mean colours are close (single-material sprite)
    while len(regs) > 1:
        best = None
        for i in range(len(regs)):
            for j in range(i + 1, len(regs)):
                d = _cdist(a, regs[i], regs[j])
                if d < merge and (best is None or d < best[0]):
                    best = (d, i, j)
        if best is None:
            break
        _, i, j = best
        regs[i] = regs[i] | regs[j]
        del regs[j]
    return regs


def _feat(a, r):
    ys, _ = np.nonzero(r)
    rgb = a[r][:, :3]
    return np.array([rgb[:, 1].mean() - (rgb[:, 0].mean() + rgb[:, 2].mean()) / 2, ys.mean(), _lum(rgb).mean()])


def _match(a_ref, regs_ref, a_old, regs_old, how='gyl'):
    """Pairs each vanilla region with the Sift region playing the same part (stem/petal, frame/glow...)."""
    import itertools
    w = {'gyl': (2.0, 1.0, 0.5), 'L': (0, 0, 1.0), 'y': (0, 1.0, 0), 'g': (1.0, 0, 0)}[how]
    fr = np.array([_feat(a_ref, r) for r in regs_ref])
    fo = np.array([_feat(a_old, r) for r in regs_old])

    def rk(f):
        return np.argsort(np.argsort(f, axis=0), axis=0) / max(1, len(f) - 1)

    rr, ro = rk(fr), rk(fo)
    best = None
    for perm in itertools.permutations(range(len(regs_old)), len(regs_ref)):
        cost = sum(((rr[i] - ro[p]) ** 2 * w).sum() for i, p in enumerate(perm))
        if best is None or cost < best[0]:
            best = (cost, perm)
    return [regs_old[p] for p in best[1]]


# ------------------------------------------------------------------ the two re-theme modes
def _plant_split(a, m, key='green'):
    """[foliage/stem, bloom] of a plant sprite: the greenest (or, key='top', the highest) colour region vs the rest."""
    regs = _segment(a, m, 3)
    if len(regs) < 2:
        return [m]
    if key != 'top':  # stems and leaves: every green-to-teal colour region (yellow and blue petals are bloom)
        import colorsys
        stem = np.zeros_like(m)
        for r in regs:
            h, sat, v = colorsys.rgb_to_hsv(*(a[r][:, :3].mean(0) / 255.0))
            if 70 / 360 <= h <= 190 / 360 and sat > 0.2 and v > 0.15:
                stem |= r
        return [m] if not stem.any() or not (m & ~stem).any() else [stem, m & ~stem]
    f = np.array([_feat(a, r) for r in regs])
    score = -f[:, 1]
    big = np.array([r.sum() >= 0.18 * m.sum() for r in regs])
    if big.sum() < 2:
        return [m]
    score[~big] = -1e9  # a few highlight pixels are part of the bloom, not a part of their own
    i = int(np.argmax(score))
    return [regs[i], m & ~regs[i]]


def _clone(ref, old, seg=1, match='gyl', masks=None, pals=None, acc=False, spread=34.0, by='color', key='green',
           allpal=False):
    """Vanilla layout re-themed: ref regions rank-mapped onto the matching Sift palettes."""
    out = np.zeros_like(ref)
    out[..., 3] = ref[..., 3]
    mr = ref[..., 3] > 0
    mo = old[..., 3] > 0
    acc_m = _accents(old, mo) if (seg == 1 and masks is None and not allpal) else np.zeros_like(mo)
    if masks is not None:
        regs_ref = [mr & mk for mk in masks]
        pal_list = pals
    elif seg == 'plant':
        regs_ref, regs_old = _plant_split(ref, mr, key), _plant_split(old, mo, key)
        if len(regs_ref) != len(regs_old):
            regs_ref, regs_old = [mr], [mo]
        pal_list = [Pal(old[r][:, :3], spread) for r in regs_old]
    elif seg > 1:
        regs_ref = _segment(ref, mr, seg, by)
        regs_old = _segment(old, mo, seg, by)
        if len(regs_ref) != len(regs_old):
            regs_ref, regs_old = [mr], [mo]
        regs_old = _match(ref, regs_ref, old, regs_old, match) if len(regs_ref) > 1 else regs_old
        pal_list = [Pal(old[r][:, :3], spread) for r in regs_old]
    else:  # one material: at least ~85% of the vanilla texture's own contrast (dark Sift stones stay readable)
        regs_ref = [mr]
        lo, hi = np.quantile(_lum(ref)[mr], [0.1, 0.9])
        pal_list = [Pal(old[mo & ~acc_m][:, :3], float(np.clip(0.85 * (hi - lo), spread, 56)))]
    L = _lum(ref)
    for r, p in zip(regs_ref, pal_list):
        if r.any():
            out[r, :3] = p.at(_rank(L[r]))
    if acc:
        ov = acc_m & mr
        out[ov, :3] = old[ov, :3]
    return out


def _mat(old, refs, w=0.6, spread=30.0, keep=True):
    """Sift layout kept; each main colour region re-shaded with a vanilla material's luminance structure.
    Minor colour regions (glyphs, glow, gems) and stray specks stay exactly as designed."""
    out = old.copy()
    mo = old[..., 3] > 0
    regs = sorted(_segment(old, mo, len(refs) + 1), key=lambda r: -r.sum())
    main = sorted(regs[:len(refs)], key=lambda r: _lum(old[r][:, :3]).mean())
    if keep:
        specks = _accents(old, mo, thr=48, minor=False)
        main = [r & ~specks for r in main]
    if len(main) < len(refs):  # one material only: pick the vanilla ref by brightness
        refs = [refs[0] if _lum(old[main[0]][:, :3]).mean() < 110 else refs[-1]]
    Lo = _lum(old)
    for r, ref in zip(main, refs):
        if r.sum() < 3:
            continue
        rf = _fit(ref, old.shape[0]) if ref.shape[:2] != old.shape[:2] else ref
        rl = _lum(rf)
        ro = _rank(Lo[r])
        rr = np.where(rf[r][:, 3] > 0, _rank(rl[r]), ro)
        out[r, :3] = Pal(old[r][:, :3], spread).at(_rank((1 - w) * ro + w * rr))
    return out


# ------------------------------------------------------------------ hand edits
NOTE = ['.#.', '.##', '.#.', '##.', '##.']  # an eighth note, 3x5


def _note(a, x, y, rgb, shade=None):
    for j, row in enumerate(NOTE):
        for i, ch in enumerate(row):
            if ch == '#' and 0 <= y + j < a.shape[0] and 0 <= x + i < a.shape[1]:
                a[y + j, x + i, :3] = rgb
                a[y + j, x + i, 3] = 255
    if shade is not None:  # carved shadow under the note
        for j, row in enumerate(NOTE):
            for i, ch in enumerate(row):
                yy, xx = y + j + 1, x + i + 1
                below = NOTE[j + 1][i + 1] if j + 1 < 5 and i + 1 < 3 else '.'
                if ch == '#' and below != '#' and yy < a.shape[0] and xx < a.shape[1]:
                    a[yy, xx, :3] = shade


def _glints(a, pts, rgb):
    for x, y in pts:
        if a[y, x, 3] > 0:
            a[y, x, :3] = rgb


def _sculk(a, seed, frac=0.06, deep=(14, 52, 62), glow=(72, 222, 228)):
    """Sculk veins: the darkest cracks of a stone go deep teal, with a couple of glowing pixels."""
    L = _lum(a)
    m = a[..., 3] > 0
    thr = np.quantile(L[m], frac)
    v = m & (L <= thr)
    a[v, :3] = a[v, :3] * 0.35 + np.array(deep) * 0.65
    ys, xs = np.nonzero(v)
    rng = np.random.RandomState(seed)
    for i in rng.choice(len(ys), size=min(2, len(ys)), replace=False):
        a[ys[i], xs[i], :3] = glow


def _iridesce(a, strength=0.45):
    """Prism shimmer: soft diagonal rainbow bands over the brighter crystal facets."""
    import colorsys
    L = _lum(a)
    m = a[..., 3] > 0
    lo, hi = np.quantile(L[m], [0.35, 1.0])
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            if m[y, x] and L[y, x] >= lo:
                t = (L[y, x] - lo) / max(1.0, hi - lo)
                r, g, b = colorsys.hsv_to_rgb(((x + y) // 3) / 10.0 % 1.0, 0.38, 0.7 + 0.3 * t)
                k = strength * (0.4 + 0.6 * t)
                a[y, x, :3] = a[y, x, :3] * (1 - k) + np.array([r, g, b]) * 255 * k


def _old(name):
    return _OLD[name]


# ------------------------------------------------------------------ special builders
def _ore(name, ore, base, base_old, rainbow=False):
    """Vanilla ore cloned: the stone around the spots in the Sift base-stone palette, the spots in the Sift gem's."""
    ro = _ref(ore)
    L = _lum(ro)
    spots = np.linalg.norm(ro[..., :3] - L[..., None], axis=-1) > 12
    near = np.zeros_like(spots)
    near[1:] |= spots[:-1]; near[:-1] |= spots[1:]; near[:, 1:] |= spots[:, :-1]; near[:, :-1] |= spots[:, 1:]
    spots |= near & (L > np.quantile(L[~spots], 0.97))  # the white glints inside a spot
    old, bo = _old(name), _old(base_old)
    mb = bo[..., 3] > 0
    out = _clone(ro, bo, masks=[~spots, spots], pals=[Pal(bo[mb & ~_accents(bo, mb)][:, :3]), Pal(np.zeros((1, 3)))])
    acc = _accents(old, old[..., 3] > 0, thr=30)
    gem = old[acc][:, :3] if acc.sum() >= 3 else old[..., :3].reshape(-1, 3)
    if not rainbow:  # dark rim, body, bright facets, like a vanilla ore spot
        gp = Pal(np.concatenate([gem * 0.42, gem * 0.68, gem, gem, gem + (255 - gem) * 0.5]))
        out[spots, :3] = gp.at(_rank(L[spots]))
        return out
    import colorsys
    lab = np.zeros(spots.shape, int)
    n = 0
    for y in range(16):
        for x in range(16):
            if spots[y, x] and not lab[y, x]:
                n += 1
                st = [(y, x)]
                lab[y, x] = n
                while st:
                    cy, cx = st.pop()
                    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        yy, xx = cy + dy, cx + dx
                        if 0 <= yy < 16 and 0 <= xx < 16 and spots[yy, xx] and not lab[yy, xx]:
                            lab[yy, xx] = n
                            st.append((yy, xx))
    hues = [0.92, 0.53, 0.76, 0.44, 0.98, 0.62]
    for i in range(1, n + 1):
        r = lab == i
        t = _rank(L[r])
        h = hues[(i - 1) % len(hues)]
        out[r, :3] = np.array([colorsys.hsv_to_rgb(h, 0.66 - 0.42 * tt, 0.5 + 0.5 * tt) for tt in t]) * 255
    return out


def _ring(name, ref, bark_old, spread=34.0):
    """Log top: bark ring from the Sift log side, rings from the Sift log top's inside."""
    r = _ref(ref)
    old = _old(name)
    edge = np.zeros((16, 16), bool)
    edge[0, :] = edge[-1, :] = edge[:, 0] = edge[:, -1] = True
    inner = old[1:-1, 1:-1, :3].reshape(-1, 3)
    return _clone(r, old, masks=[edge, ~edge], pals=[Pal(_old(bark_old)[..., :3].reshape(-1, 3), spread),
                                                     Pal(inner, spread)])


def _grass_side(name, ref, overlay=None):
    r = _ref(ref)
    old = _old(name)
    mo = old[..., 3] > 0
    regs = _segment(old, mo, 2, merge=0)
    regs = sorted(regs, key=lambda m: np.nonzero(m)[0].mean())  # top region = turf
    if overlay:
        top = _ref(overlay)[..., 3] > 0
    else:  # snowy side: the bright band over the dirt
        L = _lum(r)
        top = np.zeros((16, 16), bool)
        for x in range(16):
            for y in range(16):
                if L[y, x] > 175:
                    top[y, x] = True
                else:
                    break
    return _clone(r, old, masks=[top, ~top], pals=[Pal(old[regs[0]][:, :3]), Pal(old[regs[1]][:, :3])])


def _anim_mat(name, ref, w=0.5):
    fo = _frames(_old(name))
    fr = _frames(_ref(ref, True))
    return np.concatenate([_mat(f, [fr[(i * len(fr)) // len(fo)]], w) for i, f in enumerate(fo)], 0)


def _prism(name, ref):
    """Prism block: amethyst cloned in the gem's pale body colour, then a diagonal rainbow shimmer."""
    o = _old(name)
    rgb = o[o[..., 3] > 0][:, :3]
    L = _lum(rgb)[:, None]
    body = L + (rgb - L) * 0.25 + np.array([-4.0, -8.0, 12.0])  # desaturated toward a cool lavender
    return _clone(_ref(ref), o, masks=[np.ones((16, 16), bool)], pals=[Pal(np.clip(body, 0, 255), 60)])


# ------------------------------------------------------------------ the reference map
def C(ref, **kw):
    return ('clone', ref, kw)


def M(*refs, **kw):
    return ('mat', list(refs), kw)


def F(fn, *args):
    return ('fn', fn, args)


MAP = {
    # --- Dreamstone (stone family): pale, bone-white End Stone (W1; the cracked and mossy bricks are rebuilt on the End Stone
    #     Bricks layout by tools/sculk_world.py, which replaces those two entries at build time)
    'dreamstone': C('end_stone'),
    'cobbled_dreamstone': C('cobblestone'),
    'dreamstone_bricks': C('end_stone_bricks'),
    'cracked_dreamstone_bricks': C('cracked_stone_bricks'),
    'mossy_dreamstone_bricks': C('mossy_stone_bricks', seg=2, match='g'),
    'chiseled_dreamstone': C('chiseled_stone_bricks'),
    'dreamstone_tiles': C('deepslate_tiles'),
    'polished_dreamstone': C('polished_andesite'),
    'dreamstone_pillar_side': C('purpur_pillar_side'),
    'dreamstone_pillar_top': C('purpur_pillar_top'),
    # --- Hushslate (deepslate family)
    'hushslate': C('deepslate'),
    'hushslate_top': C('deepslate_top'),
    'cobbled_hushslate': C('cobbled_deepslate'),
    'hushslate_bricks': C('deepslate_bricks'),
    'cracked_hushslate_bricks': C('cracked_deepslate_bricks'),
    'hushslate_tiles': C('deepslate_tiles'),
    'polished_hushslate': C('polished_deepslate'),
    'chiseled_hushslate': C('chiseled_deepslate'),
    # --- Blush bricks (nether brick family)
    'blush_bricks': C('nether_bricks'),
    'cracked_blush_bricks': C('cracked_nether_bricks'),
    'chiseled_blush_bricks': C('chiseled_nether_bricks'),
    # --- sand (W-land: the Dreamsand family is gone; Chime Sandstone and Dunestone are mapped in tools/wland_art.py)
    'coral_sand': C('red_sand', acc=True),
    'chime_sand': C('sand', acc=True),
    # --- soils & turf
    'sift_soil': C('dirt'),
    'sift_grass_block_top': C('grass_block_top'),
    'sift_grass_block_side': F(_grass_side, 'grass_block_side', 'grass_block_side_overlay'),
    'coral_turf_top': C('grass_block_top'),
    'coral_turf_side': F(_grass_side, 'grass_block_side', 'grass_block_side_overlay'),
    'white_turf_top': C('grass_block_top'),
    'white_turf_side': F(_grass_side, 'grass_block_snow'),
    'lumen_moss_block': C('moss_block', acc=True),
    'cloud_block': C('powder_snow'),
    # --- Lullwood (spruce / pale oak) and Wishwood (cherry)
    'lullwood_log': C('spruce_log', spread=48),
    'lullwood_log_top': F(_ring, 'spruce_log_top', 'lullwood_log'),
    'stripped_lullwood_log': C('stripped_spruce_log'),
    'stripped_lullwood_log_top': C('stripped_spruce_log_top'),
    'lullwood_planks': C('spruce_planks'),
    'lullwood_door_top': C('spruce_door_top'),
    'lullwood_door_bottom': C('spruce_door_bottom'),
    'lullwood_trapdoor': C('spruce_trapdoor'),
    'lullwood_leaves': C('pale_oak_leaves'),
    'white_lullwood_leaves': C('birch_leaves'),
    'hanging_lullwood_leaves': C('pale_hanging_moss'),
    'hanging_lullwood_leaves_tip': C('pale_hanging_moss_tip'),
    'lullwood_sapling': C('pale_oak_sapling', seg='plant', key='top'),
    'white_lullwood_sapling': C('birch_sapling', seg='plant', key='top'),
    'wishwood_log': C('cherry_log', spread=48),
    'wishwood_log_top': F(_ring, 'cherry_log_top', 'wishwood_log'),
    'stripped_wishwood_log': C('stripped_cherry_log'),
    'stripped_wishwood_log_top': C('stripped_cherry_log_top'),
    'wishwood_planks': C('cherry_planks'),
    'wishwood_door_top': C('cherry_door_top'),
    'wishwood_door_bottom': C('cherry_door_bottom'),
    'wishwood_trapdoor': C('cherry_trapdoor'),
    'wishwood_leaves': C('cherry_leaves'),
    'wishwood_sapling': C('cherry_sapling', seg='plant', key='top'),
    # --- grasses, flowers, crops
    'blushgrass': C('short_grass'),
    'tall_blushgrass_bottom': C('tall_grass_bottom'),
    'tall_blushgrass_top': C('tall_grass_top'),
    'dreambloom': C('poppy', seg='plant'),
    'echo_orchid': C('blue_orchid', seg='plant'),
    **{f'echo_orchid_crop_stage{i}': C(f'potatoes_stage{i}') for i in range(3)},
    'glimmer_sprouts': C('warped_roots'),
    'glowcap': C('warped_fungus', seg='plant'),
    'hummingbloom': C('red_tulip', seg='plant'),
    'nebula_iris': C('cornflower', seg='plant'),
    'soulpetal': C('oxeye_daisy', seg='plant'),
    'puffbloom': C('allium', seg='plant'),
    'sculk_bloom': C('wither_rose', seg='plant', key='top'),
    'lullaby_bell': C('lily_of_the_valley', seg='plant'),
    'chime_bell': C('closed_eyeblossom', seg='plant'),
    'chime_bell_ringing': C('open_eyeblossom', seg='plant'),
    'musical_cobweb': C('cobweb'),
    'glowbell_vine': C('cave_vines', seg='plant'),
    'glowbell_vine_lit': C('cave_vines_lit', seg='plant'),
    'glowbell_vine_plant': C('cave_vines_plant', seg='plant'),
    'glowbell_vine_plant_lit': C('cave_vines_plant_lit', seg='plant'),
    # --- sea & coral flora
    'coral_bush': C('brain_coral'),
    'coral_thicket_top': C('bubble_coral'),
    'coral_thicket_bottom': C('horn_coral'),
    'abyss_anemone': C('tube_coral', allpal=True),
    'amber_glowkelp': C('kelp_plant'), 'amber_glowkelp_tip': C('kelp'),
    'azure_glowkelp': C('kelp_plant'), 'azure_glowkelp_tip': C('kelp'),
    'rose_glowkelp': C('kelp_plant'), 'rose_glowkelp_tip': C('kelp'),
    'organ_reed_bottom': C('sugar_cane'),
    'organ_reed_top': C('sugar_cane'),
    'chrome_reeds': C('tall_seagrass_top'),
    # --- ores & mineral blocks
    'prism_ore': F(_ore, 'emerald_ore', 'stone', 'dreamstone', True),
    'deep_prism_ore': F(_ore, 'deepslate_emerald_ore', 'deepslate', 'hushslate', True),
    'siftite_block': C('netherite_block'),
    'prism_block': F(_prism, 'amethyst_block'),
    **{f'music_crystal_{c}': C('amethyst_block') for c in ('amber', 'gold', 'rose', 'teal', 'violet')},
    **{f'music_crystal_{c}_frozen': C('packed_ice') for c in ('amber', 'gold', 'rose', 'teal', 'violet')},
    'glowing_slime_block': M('slime_block'),
    # --- glass & fluids
    'chime_glass': ('anim_mat', 'glass', {}),
    'chime_glass_pane_top': C('glass_pane_top'),
    'chrome_glass': M('glass', w=0.3),
    'chrome_still': ('anim_mat', 'water_still', {}),
    'chrome_flow': ('anim_mat', 'water_flow', {}),
    'chrome_overlay': M('water_overlay'),
    'sift_portal': ('anim_mat', 'nether_portal', {}),
    # --- designed blocks: Sift layout, vanilla material
    'bulb_lantern': C('lantern', seg=2, match='L'),
    'echoer_hut_heart_side': M('calcite'),
    'echoer_hut_heart_top': M('calcite'),
    'echoer_hut_heart_top_spent': M('calcite'),
    'sift_gate_frame_side': M('end_portal_frame_side'),
    'sift_gate_frame_top': M('end_portal_frame_top'),
    'pitcher_planter_side': M('bricks'),
    'pitcher_planter_top': M('rooted_dirt', 'terracotta'),
    'pitcher_planter_rim': M('terracotta'),
    'pitcher_planter_bottom': M('terracotta'),
    'ancient_cannon_barrel': M('anvil', 'copper_block'),
    'ancient_cannon_muzzle': M('anvil', 'copper_block'),
    'ancient_cannon_carriage': M('dark_oak_planks'),
    'ancient_cannon_ball': M('anvil'),
    'swifter_den_bottom': M('coarse_dirt', 'snow'),
    'swifter_den_fluff': M('white_wool'),
    'swifter_den_side': M('oak_log', 'stripped_oak_log'),
    'swifter_den_top': M('coarse_dirt', 'white_wool'),
    'sift_cake_side': M('cake_side'),
    'sift_cake_top': M('cake_top'),
    'sift_cake_bottom': M('cake_bottom'),
    'sift_cake_inner': M('cake_inner'),
    'soul_chime_core': M('sea_lantern'),
    'soul_chime_core_lit': M('sea_lantern'),
    'soul_chime_tube': M('iron_block'),
    'soul_chime_wood': C('cherry_planks'),
}
SKIP = {'glow_particle'}  # a particle sprite that only lives in block/
SKIP |= set(__import__('echoer_drill').TEXTURES)  # RR: the Echoer Drill's housing is drawn in tools/echoer_drill.py
# E1 Sniffer & rot: the Sift Sniffer egg's faces are vanilla's Sniffer egg faces re-themed already (tools/sift_sniffer.py)
SKIP |= {f'sift_sniffer_egg_{s}_{f}' for s in ('not_cracked', 'slightly_cracked', 'very_cracked')
         for f in ('north', 'east', 'south', 'west', 'top', 'bottom')}
# Block art: the Sift Drum is a 16x Mojang-style model painted in tools/blockart.py (the 32x drum sheet is gone)
SKIP |= {'sift_drum_' + p for p in ('side', 'side_core', 'head', 'head_struck', 'hoop', 'base', 'glow', 'head_glow')}


def _edits(name, a):
    """Hand edits on top of the re-themed vanilla texture: Sift notes, glints and sculk."""
    o = _old(name)
    mo = o[..., 3] > 0
    acc = _accents(o, mo)
    glow = o[acc][:, :3][np.argmax(_lum(o[acc][:, :3]))] if acc.any() else Pal(o[mo][:, :3]).top()
    L = _lum(a)
    m = a[..., 3] > 0
    dark = a[m][:, :3][np.argmin(L[m])]
    if name == 'chiseled_dreamstone':
        _note(a, 6, 5, glow, shade=dark)
    elif name == 'chiseled_hushslate':
        _note(a, 6, 5, (88, 226, 230), shade=dark)
    elif name == 'chiseled_blush_bricks':
        _note(a, 6, 5, glow, shade=dark)
    elif name == 'chiseled_chime_sandstone':  # the creeper face is sanded off and a note carved instead
        a[4:11, 3:13] = _clone(_ref('sandstone_top'), o)[4:11, 3:13]
        _note(a, 6, 5, glow, shade=dark)
    elif name in ('hushslate', 'cobbled_hushslate', 'hushslate_top'):
        _sculk(a, len(name))
    elif name == 'dreamstone':
        _glints(a, [(3, 11), (12, 4)], Pal(o[mo][:, :3]).top(0.999))
    elif name == 'musical_cobweb':
        _note(a, 9, 2, glow)
    elif name == 'prism_block':
        _iridesce(a, 0.55)
    return a


def remap(tex_root):
    block = os.path.join(tex_root, 'block')
    if not os.path.isdir(VANILLA):
        print('vanilla_remap: no vanilla textures at', VANILLA, '- Sift block textures left as generated')
        return
    names = sorted(f[:-4] for f in os.listdir(block) if f.endswith('.png'))
    _OLD.clear()
    for n in names:
        _OLD[n] = _load(os.path.join(block, n + '.png'))
    unmapped = [n for n in names if n not in MAP and n not in SKIP]
    if unmapped:
        print('vanilla_remap: no vanilla reference for', ' '.join(unmapped))
    for n in names:
        if n not in MAP:
            continue
        kind, ref, kw = MAP[n]
        old = _OLD[n]
        if kind == 'clone':
            a = _clone(_ref(ref), old, **kw)
        elif kind == 'mat':
            a = _mat(old, [_ref(r) for r in ref], **kw)
        elif kind == 'fn':
            a = ref(n, *kw)
        elif kind == 'anim_mat':
            a = _anim_mat(n, ref)
        a = _edits(n, a)
        Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(block, n + '.png'))
