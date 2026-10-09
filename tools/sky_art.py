"""W-sky: Sky Islands art (hooked from gen_textures.main through tools/sky_islands.py, before vanilla_remap).

Every block texture starts from its closest vanilla Mojang texture: the generator paints a seed in the Sky Islands
palette (cyan sky grass over the Sift's mauve soil, lavender-white fluff and cloud, dusk-violet Skypalm bark with a pink
heart, teal fronds and lianas, pale lilac Sky Roots, a glowing pink Driftfruit) and tools/vanilla_remap.py rebuilds it
on the vanilla texture's exact value structure (grass block, short/tall grass, azalea, jungle log/leaves/sapling, cherry
leaves/sapling, pale oak log, chorus plant, vine, pumpkin). Items are re-themed vanilla sprites (melon slice, lead);
the Skyrind (no vanilla banana) and its bunches are drawn by hand in the same style.
"""
import os
import random

import numpy as np
from PIL import Image

import mctex as M
from texlib import Tex, hx

CUTOUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}
LEAVES_META = {'texture': {'mipmap_strategy': 'dark_cutout'}}
VANILLA_ITEM = os.environ.get('VANILLA_TEX_ITEM', '/home/user/ref/mc-tex/assets/minecraft/textures/item')
VANILLA_BLOCK = os.environ.get('VANILLA_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures/block')

# ---------------------------------------------------------------------------- palettes (darkest first)
SKY_GRASS = ['#1f8fa3', '#2ea8b6', '#41c0c8', '#5bd4d6', '#7fe4df', '#aaf2ea']
SOIL = ['#6a4252', '#7b4f5f', '#8d5e6d', '#9e6e7c', '#b0808c']                    # the Sift's soil (gen_textures.SOIL)
CIRRUS = ['#2b98a9', '#4cb6c2', '#79cfd6', '#a7e3e6', '#d3f3f3', '#f6feff']
FLUFF = ['#d9a9cb', '#e6bcd9', '#efcfe5', '#f6e2ef', '#fbf1f7', '#ffffff']
FLUFF_STEM = ['#4f3f66', '#655280', '#7d6a99', '#9784b3', '#b0a0c9']
SKYPALM_BARK = ['#2c2547', '#3b335c', '#4c4372', '#5f5688', '#756d9f', '#8e87b8']
SKYPALM_HEART = ['#c3628f', '#d47ca5', '#e396bb', '#efb1cf', '#f8cde1']
FROND = ['#0f6e74', '#16888a', '#1fa29e', '#2fbbb0', '#4cd1c0', '#7de6d2']
FROND_BLOOM = ['#e9789f', '#ff9ec9', '#ffc1db', '#ffd98a']
CLOUDPUFF = ['#aab6df', '#c3cdea', '#d9e0f3', '#ebf0fa', '#f7f9fe', '#ffffff']
ROOT = ['#5d5674', '#746c8b', '#8c84a2', '#a59eb9', '#bdb7cf', '#d6d1e3']
ROOT_HEART = ['#b9a98f', '#cbbca3', '#dccfb8', '#ebe1cd', '#f6efe1']
VINE = ['#0d4b4f', '#145f61', '#1c7774', '#279087', '#36a99a', '#4fc1ad']
VINE_LEAF = ['#13756e', '#1d9284', '#2cad9a', '#46c6ae', '#6fdcc3', '#a0eedb']
DRIFT = ['#8f2f62', '#ad4278', '#c85a90', '#de77a8', '#ef98c0', '#fbbcd8']
DRIFT_STEM = ['#1e5a4c', '#2a7462', '#398f78', '#4fa98d', '#6cc2a3']


def _hx3(c):
    return hx(c)[:3]


def _seed(pal, seed, w=16, h=16, weights=None):
    """A seed for a vanilla_remap clone: every tone of the palette present (the clone takes its range from it)."""
    rnd = random.Random(seed)
    cols = [hx(c) for c in pal]
    t = Tex(w, h)
    for y in range(h):
        for x in range(w):
            t.set(x, y, cols[rnd.choices(range(len(cols)), weights=weights)[0] if weights else rnd.randrange(len(cols))])
    return t


def _speck(t, pts, colour):
    for x, y in pts:
        t.set(x, y, hx(colour))
    return t


def _paint(rows, keys):
    t = Tex(len(rows[0]), len(rows))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch not in '. ':
                t.set(x, y, hx(keys[ch]))
    return t


def _arr(t):
    return np.asarray(t.img(), dtype=np.float64)


def _img(a):
    return Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA')


def _vanilla_item(name):
    return np.asarray(Image.open(os.path.join(VANILLA_ITEM, name + '.png')).convert('RGBA'), dtype=np.float64)


def _vanilla_block(name):
    a = np.asarray(Image.open(os.path.join(VANILLA_BLOCK, name + '.png')).convert('RGBA'), dtype=np.float64)
    return a[:a.shape[1]]


def _regions_clone(ref, regions):
    """A vanilla sprite re-themed region by region: regions = [(mask over ref, palette)]; each region's pixels are
    luminance-rank mapped onto its palette (Mojang's value structure, our colours)."""
    import vanilla_remap as VR
    out = ref.copy()
    L = VR._lum(ref)
    for mask, pal in regions:
        m = mask & (ref[..., 3] > 0)
        if m.any():
            out[m, :3] = VR.Pal(np.array([_hx3(c) for c in pal], dtype=np.float64), 28).at(VR._rank(L[m]))
    return out


def _hue_masks(ref):
    import colorsys
    hsv = np.zeros(ref.shape[:2] + (3,))
    for y in range(ref.shape[0]):
        for x in range(ref.shape[1]):
            hsv[y, x] = colorsys.rgb_to_hsv(*(ref[y, x, :3] / 255.0))
    return hsv


# ---------------------------------------------------------------------------- hand-drawn: the Skyrind and its bunches

SKYRIND_KEYS = {'o': '#7a4a12', 'd': '#b8761f', 'm': '#d9982e', 'l': '#efbb45', 'L': '#ffd96f', 'W': '#fff0a8',
                'p': '#d2588f', 'P': '#ff9ccb', 's': '#3e2b52', 'S': '#6a5487', 'c': '#5fd6cf'}
# one sky banana: a golden crescent with a violet stalk, a pink tip and a cyan fleck of sky in its skin
SKYRIND = [
    '................',
    '..........sS....',
    '..........ss....',
    '..........od....',
    '.........odl....',
    '.........dlL....',
    '........odlL....',
    '.......odmLW....',
    '......odmlLo....',
    '.....odmlLlo....',
    '....odmmLlo.....',
    '..oodmlLlo......',
    '.ppdmllcoo......',
    'pPPdmmoo........',
    '.ppoo...........',
    '................',
]

BUNCH_KEYS = {'s': '#3e2b52', 'S': '#5a4373', 'T': '#7a6294',
              'g': '#2f7f68', 'G': '#43a083', 'h': '#64bf9d', 'H': '#8fd8b8',
              'y': '#8e9a35', 'Y': '#b3bd4c', 'z': '#d4d977',
              'o': '#8a5414', 'd': '#b8761f', 'm': '#d9982e', 'l': '#efbb45', 'L': '#ffd96f', 'W': '#fff0a8',
              'p': '#d2588f', 'P': '#ff9ccb', 'f': '#f7b8d6', 'F': '#ffe1ef'}
# the bunch hangs from the frond above: a violet stalk, then rows of fingers curving up (stage 0: a pink bud)
BUNCH = [
    [  # stage 0: the flower bud
        '.......sS.......',
        '.......sS.......',
        '.......sS.......',
        '.......sT.......',
        '......sST.......',
        '......pPf.......',
        '.....pPfFf......',
        '.....pPPff......',
        '......pPf.......',
        '.......p........',
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
    ],
    [  # stage 1: small green fingers
        '.......sS.......',
        '.......sS.......',
        '.......sS.......',
        '.....g.sS.g.....',
        '....gG.sS.Gg....',
        '....gH.sS.Hg....',
        '...gGh.sS.hGg...',
        '...gh..sS..hg...',
        '..g.gGhsShGg.g..',
        '..GhgH.sS.HghG..',
        '...gh..sT..hg...',
        '.......sT.......',
        '.......pP.......',
        '................',
        '................',
        '................',
    ],
    [  # stage 2: plump, yellow-green
        '.......sS.......',
        '.......sS.......',
        '..y....sS....y..',
        '..yY...sS...Yy..',
        '..yz.y.sS.y.zy..',
        '.yYz.yYsSYy.zYy.',
        '.yYz.yzsSzy.zYy.',
        '..yY.yzsSzy.Yy..',
        '.y..yYzsSzYy..y.',
        '.yY.yzYsSYzy.Yy.',
        '.yzYyz.sS.zyYzy.',
        '..yz.Y.sT.Y.zy..',
        '...yY..sT..Yy...',
        '.......pP.......',
        '.......pP.......',
        '................',
    ],
    [  # stage 3: ripe, gold with pink tips
        '.......sS.......',
        '.p.....sS.....p.',
        '.Pd....sS....dP.',
        '.dlo...sS...old.',
        '.dLm.P.sS.P.mLd.',
        'odLm.dosSod.mLdo',
        'odWm.dLsSLd.mWdo',
        '.dLmodLsSLdomLd.',
        'P.dmdmWsSWmdmd.P',
        'dooddmLsSLmddood',
        'dLmdmlLsSLlmdmLd',
        '.dWlmdoTSodmlWd.',
        '..dlmo.sT.omld..',
        '...dd..sT..dd...',
        '.......pP.......',
        '.......PP.......',
    ],
]


def _bunch(stage):
    return _paint(BUNCH[stage], BUNCH_KEYS)


# ---------------------------------------------------------------------------- seeds for vanilla_remap

def _grass_top():
    t = M.stone([hx(c) for c in SKY_GRASS], 701, light=8, dark=8, core=0.3)
    # little tufts of cloud caught in the grass, and the odd pink sky-flower
    _speck(t, [(3, 2), (4, 2), (11, 6), (12, 6), (11, 7), (6, 12), (7, 12)], '#f4fdff')
    _speck(t, [(13, 13), (2, 9)], '#ff9fcf')
    return t


def _grass_side():
    soil = M.soil([hx(c) for c in SOIL], 51)
    return M.grass_side([hx(c) for c in SKY_GRASS], soil, 703)


def _leaves_two(main, bloom, seed, frac=0.12):
    """Seed with two colour regions: the leaf tones and a smaller bloom/fruit region."""
    t = _seed(main, seed)
    rnd = random.Random(seed + 1)
    for _ in range(int(256 * frac)):
        t.set(rnd.randrange(16), rnd.randrange(16), hx(rnd.choice(bloom)))
    return t


def _sapling_two(top, stem, seed, top_rows=9):
    """Plant seed: the crown tones on the upper rows, the stem tones below (for the 'plant' split)."""
    t = Tex()
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            pal = top if y < top_rows else stem
            t.set(x, y, hx(rnd.choice(pal)))
    return t


def _ring_seed(heart, seed):
    return _seed(heart, seed)


def _fruit_side():
    t = _seed(DRIFT, 731)
    _speck(t, [(3, 4), (12, 9), (7, 13), (9, 3)], '#8ff3f3')  # cyan flecks of sky in the skin
    _speck(t, [(5, 8), (10, 5)], '#ffd36b')                   # golden glints along the ribs
    return t


def _fruit_top():
    t = _seed(DRIFT, 733)
    for x in range(6, 10):
        for y in range(6, 10):
            t.set(x, y, hx(DRIFT_STEM[(x + y) % len(DRIFT_STEM)]))
    return t


# ---------------------------------------------------------------------------- items and the rope

def _skyrind_item():
    return _paint(SKYRIND, SKYRIND_KEYS)


def _slice_item():
    """Vanilla's melon slice: rind -> a deep magenta rind, red flesh -> pink, seeds -> cyan."""
    ref = _vanilla_item('melon_slice')
    hsv = _hue_masks(ref)
    op = ref[..., 3] > 0
    h, s, v = hsv[..., 0], hsv[..., 1], hsv[..., 2]
    green = op & (h > 0.15) & (h < 0.5) & (s > 0.2)
    dark = op & ~green & (v < 0.25)
    flesh = op & ~green & ~dark
    return _regions_clone(ref, [(green, ['#4a1838', '#6c2652', '#8f366c', '#b34d88', '#d06aa2']),
                                (flesh, ['#e86aa5', '#ff8cbd', '#ffaed0', '#ffcfe3', '#ffe8f2']),
                                (dark, ['#106c78', '#1f9fae', '#5fe0e6'])])


def _vine_item():
    """Vanilla's lead (a coiled rope) as a coil of Sky Vine."""
    ref = _vanilla_item('lead')
    op = ref[..., 3] > 0
    return _regions_clone(ref, [(op, VINE + ['#6fd5bf'])])


def _rope():
    """The swinging rope's strand: vanilla's twisting vines plant, re-themed as a Sky Vine liana."""
    ref = _vanilla_block('twisting_vines_plant')
    op = ref[..., 3] > 0
    return _regions_clone(ref, [(op, VINE + ['#6fd5bf'])])


# ---------------------------------------------------------------------------- the vanilla references

def _pal(colours):
    import vanilla_remap as VR
    return VR.Pal(np.array([_hx3(c) for c in colours], dtype=np.float64))


def _fronds(name, ref):
    """Jungle leaves: the grey leaf tones become teal fronds, the coloured fruit bits pink and gold blossoms."""
    import vanilla_remap as VR
    r = VR._ref(ref)
    op = r[..., 3] > 0
    bloom = op & ((r[..., :3].max(-1) - r[..., :3].min(-1)) > 40)
    return VR._clone(r, VR._old(name), masks=[op & ~bloom, bloom], pals=[_pal(FROND), _pal(FROND_BLOOM)])


def _fruit_cap(name, ref):
    """Pumpkin top: the pink rind all round, a short teal stem in the middle."""
    import vanilla_remap as VR
    r = VR._ref(ref)
    yy, xx = np.mgrid[0:16, 0:16]
    stem = (np.abs(xx - 7.5) < 2.5) & (np.abs(yy - 7.5) < 2.5)
    return VR._clone(r, VR._old(name), masks=[~stem, stem], pals=[_pal(DRIFT), _pal(DRIFT_STEM)])


def _remap_entries():
    import vanilla_remap as VR
    C, F = VR.C, VR.F
    return {
        'sky_grass_block_top': C('grass_block_top', acc=True),
        'sky_grass_block_side': F(VR._grass_side, 'grass_block_side', 'grass_block_side_overlay'),
        'cirrus_grass': C('short_grass'),
        'tall_cirrus_grass_bottom': C('tall_grass_bottom'),
        'tall_cirrus_grass_top': C('tall_grass_top'),
        'fluffbush_top': C('cherry_leaves', acc=True),
        'fluffbush_side': C('cherry_leaves', acc=True),
        'fluffbush_plant': C('azalea_plant'),
        'skypalm_log_side': C('jungle_log', spread=48),
        'skypalm_log_top': F(VR._ring, 'jungle_log_top', 'skypalm_log_side'),
        'skypalm_leaves': F(_fronds, 'jungle_leaves'),
        'skypalm_sapling': C('jungle_sapling', seg='plant'),
        'cloudpuff_leaves': C('cherry_leaves', acc=True),
        'cloudpuff_sapling': C('cherry_sapling', seg='plant', key='top'),
        'sky_root_side': C('pale_oak_log', spread=40),
        'sky_root_top': F(VR._ring, 'pale_oak_log_top', 'sky_root_side'),
        'sky_vine': C('chorus_plant'),
        'sky_vine_leaf': C('vine'),
        'driftfruit_side': C('pumpkin_side', acc=True),
        'driftfruit_top': F(_fruit_cap, 'pumpkin_top'),
        'driftfruit_bottom': C('melon_top'),
        'driftfruit_stem': C('jungle_log'),
    }


def block_textures(out):
    out('block/sky_grass_block_top', _grass_top())
    out('block/sky_grass_block_side', _grass_side())
    out('block/cirrus_grass', _seed(CIRRUS, 711), CUTOUT)
    out('block/tall_cirrus_grass_bottom', _seed(CIRRUS[:5], 712), CUTOUT)
    out('block/tall_cirrus_grass_top', _seed(CIRRUS, 713), CUTOUT)
    fluff_top = _seed(FLUFF, 714)
    _speck(fluff_top, [(4, 5), (11, 3), (8, 11), (13, 12)], '#9ff0f0')
    out('block/fluffbush_top', fluff_top, CUTOUT)
    fluff_side = _seed(FLUFF, 715)
    _speck(fluff_side, [(3, 3), (10, 6), (13, 2)], '#9ff0f0')
    out('block/fluffbush_side', fluff_side, CUTOUT)
    out('block/fluffbush_plant', _seed(FLUFF_STEM, 716), CUTOUT)
    out('block/skypalm_log_side', _seed(SKYPALM_BARK, 721))
    out('block/skypalm_log_top', _ring_seed(SKYPALM_HEART, 722))
    out('block/skypalm_leaves', _leaves_two(FROND, FROND_BLOOM, 723), LEAVES_META)
    out('block/skypalm_sapling', _sapling_two(FROND, SKYPALM_BARK[1:5], 724), CUTOUT)
    cloud = _seed(CLOUDPUFF, 725)
    _speck(cloud, [(2, 3), (9, 8), (13, 13)], '#bff6ff')  # glints of dew
    out('block/cloudpuff_leaves', cloud, LEAVES_META)
    out('block/cloudpuff_sapling', _sapling_two(CLOUDPUFF[1:], FLUFF_STEM, 726, top_rows=8), CUTOUT)
    out('block/sky_root_side', _seed(ROOT, 727))
    out('block/sky_root_top', _ring_seed(ROOT_HEART, 728))
    out('block/sky_vine', _seed(VINE, 729))
    out('block/sky_vine_leaf', _seed(VINE_LEAF, 730), CUTOUT)
    out('block/driftfruit_side', _fruit_side())
    out('block/driftfruit_top', _fruit_top())
    out('block/driftfruit_bottom', _seed(DRIFT[:5], 734))
    out('block/driftfruit_stem', _seed(DRIFT_STEM, 735))
    for age in range(4):
        out(f'block/skyrind_bunch_stage{age}', _bunch(age), CUTOUT)


def textures(out):
    import vanilla_remap as VR
    block_textures(out)
    out('item/skyrind', _skyrind_item())
    out('item/driftfruit_slice', _img(_slice_item()))
    out('item/sky_vine', _img(_vine_item()))
    out('entity/sky_rope', _img(_rope()))
    VR.MAP.update(_remap_entries())
    VR.SKIP.update({f'skyrind_bunch_stage{i}' for i in range(4)})  # drawn by hand (no vanilla bunch of bananas)
