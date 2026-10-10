"""P4 Cave Jungle art: 16x16 Mojang-style textures for the Giant Pitcher Plant, its sprout, the tadpole-bored log, the
jungle's items and the six spawn eggs.

Every block and item texture is a vanilla texture re-themed here (its own value structure - Mojang's clusters, outline
and light from the top left - mapped onto Cave Jungle palettes: lush greens, the pitcher's wine-red veins, glowing
juice); the Scythe Arm and the Cruncher Tooth have no vanilla counterpart and are drawn as masks shaded by the vanilla
item rule (tools/mcitems.py). The eggs follow the per-mob vanilla 1.21.5 spawn eggs (tools/fish_items.py _egg): the
creature's own face and colours, small pixel eyes. Hooked in from gen_textures.main() (textures(out)).
"""
import os

import numpy as np
from PIL import Image

VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')
CUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}


def hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*cs):
    return [hx(c) for c in cs]


def vanilla(name):
    a = np.asarray(Image.open(os.path.join(VANILLA, name + '.png')).convert('RGBA'), dtype=np.float64)
    return a[:a.shape[1]]  # an animated strip: its first frame


def img(a):
    return Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA')


def lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def recolor(a, pal, mask=None):
    """A vanilla texture's own values (ranked) mapped onto a palette (dark -> light), optionally only where mask is set."""
    out = a.copy()
    m = a[..., 3] > 0
    if mask is not None:
        m &= mask
    if not m.any():
        return out
    L = lum(a)[m]
    r = np.argsort(np.argsort(L, kind='stable'), kind='stable') / max(1, len(L) - 1)
    out[m, :3] = np.array(pal, dtype=np.float64)[np.clip(np.round(r * (len(pal) - 1)).astype(int), 0, len(pal) - 1)]
    return out


def hash2(x, y, s):
    v = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    v = (v ^ (v >> 13)) * 1274126177 & 0xffffffff
    return ((v ^ (v >> 16)) & 0xffff) / 65535.0


# palettes (dark -> light)
PITCHER = ramp('#1c2e14', '#28421a', '#365a22', '#46722a', '#5a8a34', '#74a444', '#94bc5a')
VEIN = ramp('#3a0e14', '#561620', '#72202a', '#8e2e34', '#a8423e')
RIM = ramp('#3e0c16', '#5c1420', '#7c1e2a', '#9c2c34', '#b8423e', '#d0604c', '#e2845e')
JUICE = ramp('#2a3a08', '#3e560c', '#567612', '#74961c', '#94b42a', '#b6d040', '#d6e866')
LEAF = ramp('#14280e', '#1e3a14', '#2a4e1a', '#386622', '#4a7e2c', '#629838', '#80b04a')
POD = ramp('#2a1210', '#4a1e18', '#6a3420', '#6e5a24', '#7e8a30', '#9cae40', '#bed058')
ROAST = ramp('#24120a', '#43240e', '#683c14', '#8e5a1e', '#b27e30', '#d0a248', '#e8c46a')
GLAND = ramp('#5a3a08', '#8a6410', '#b8901c', '#e2bc2c', '#f8d846', '#ffec80', '#fff8c4')
MAGNESIUM = ramp('#3e444a', '#5c646c', '#7e868e', '#a2aab2', '#c4cad0', '#e2e6ea', '#fbfcfd')
EGG = ramp('#26321a', '#3a4a24', '#556a32', '#728a44', '#94a85c', '#b6c47c', '#d8e2a8')
BAKED = ramp('#3a2410', '#5c3a16', '#845620', '#ac7a30', '#cc9e48', '#e4be6a', '#f6dc9c')


# ============================================================================ the Giant Pitcher Plant

def pitcher_side():
    """The pitcher's wall: melon-rind stripes in jungle green, splashed with wine-red veins like a Nepenthes."""
    a = recolor(vanilla('block/melon_side'), PITCHER)
    L = lum(a)
    for y in range(16):
        for x in range(16):
            n = hash2(x // 2, y // 3, 7) * 0.7 + hash2(x, y, 9) * 0.3
            if n > 0.68:
                a[y, x, :3] = VEIN[min(4, int(L[y, x] / 52))]
    return a


def pitcher_rim():
    """The peristome: a ribbed lip of deep red, its ridges catching the light."""
    a = vanilla('block/melon_side')
    return recolor(np.ascontiguousarray(np.rot90(a)), RIM)


def pitcher_lid():
    """The hood: a leaf of the pitcher's green, veined red underneath."""
    a = recolor(vanilla('block/big_dripleaf_top'), PITCHER)
    L = lum(a)
    for y in range(16):
        for x in range(16):
            if a[y, x, 3] > 0 and (x + y) % 5 == 0 and hash2(x, y, 3) > 0.45:
                a[y, x, :3] = VEIN[min(4, int(L[y, x] / 52))]
    return a


def pitcher_juice():
    """The mouth seen from above: a ring of dark throat round a pool of glowing digestive juice."""
    a = recolor(vanilla('block/honey_block_top'), JUICE)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.0:
                a[y, x, :3] = (np.array(VEIN[0]) * 0.7)
            elif d > 5.0:
                a[y, x, :3] = VEIN[1]
    return a


def pitcher_leaves():
    """The leaves round its foot (cross planes): a vanilla pitcher's base leaves in jungle green."""
    return recolor(vanilla('block/pitcher_crop_bottom_stage_4'), LEAF)


def pitcher_pod_block():
    """The pods round the lip."""
    return recolor(vanilla('block/melon_side'), ramp('#3a0e14', '#5a1a1c', '#7c2c22', '#9a4a2a', '#b46a34', '#cc8c44'))


def sprout(stage):
    a = recolor(vanilla(f'block/pitcher_crop_bottom_stage_{stage + 1}'), LEAF)
    # wine-red tips on the youngest leaves
    for y in range(16):
        for x in range(16):
            if a[y, x, 3] > 0 and y < 6 + stage * 2 and hash2(x, y, stage) > 0.6:
                a[y, x, :3] = VEIN[3]
    return a


# ============================================================================ the bored log

def bored_log():
    """A jungle log pocked with round tadpole holes: dark, wet hollows with a chewed, paler rim."""
    a = vanilla('block/jungle_log').copy()
    holes = [(4, 3), (11, 6), (6, 11), (12, 13)]
    for y in range(16):
        for x in range(16):
            for hx_, hy in holes:
                d = ((x - hx_) ** 2 + (y - hy) ** 2) ** 0.5
                if d < 1.3:
                    a[y, x, :3] = (22, 16, 8) if d < 0.8 else (40, 28, 14)
                elif d < 2.2:
                    a[y, x, :3] = np.minimum(255, a[y, x, :3] * (1.35 if (x - hx_) + (y - hy) < 0 else 0.7))
    return a


# ============================================================================ items

def _mask(rows):
    return [list(r) for r in rows]


def scythe_arm():
    """The Mantis's raptorial arm: a thick, segmented green limb (a crystal growing from it) ending in a great curved,
    serrated blade of pale chitin - shaded by the vanilla item rule, the arm and the blade each in its own material."""
    import maskshade as MI
    rows = [
        '................',
        '....######......',
        '..##########....',
        '.####....####...',
        '.##.#.#.#..###..',
        '.#..........##..',
        '............###.',
        '...........####.',
        '..........####..',
        '.........####...',
        '........####....',
        '.......####.....',
        '......####......',
        '.....####.......',
        '....###.........',
        '................',
    ]
    blade = _mask([''.join(ch if y <= 5 else '.' for ch in r) for y, r in enumerate(rows)])
    arm = _mask([''.join(ch if y > 5 else '.' for ch in r) for y, r in enumerate(rows)])
    a = MI.shade(arm, [hx(c) + (255,) for c in ('#0e1e0c', '#24461f', '#3a6a2e', '#5a9a44', '#7cbc5c')]).a.astype(np.float64)
    b = MI.shade(blade, [hx(c) + (255,) for c in ('#3a3a20', '#98985e', '#c6c690', '#e4e4b4', '#fafae0')]).a.astype(np.float64)
    a = np.where(b[..., 3:4] > 0, b, a)
    for x, y in ((10, 9), (7, 12)):  # segment joints
        a[y, x, :3] = hx('#1b3f1c')
    a[10, 9, :3] = hx('#5ab8dc')  # a crystal growing from the arm
    a[9, 10, :3] = hx('#9ae6f8')
    return a


def cruncher_tooth():
    """A Cruncher's fang: a curved cone of bone, yellowed at the root, worn white at the tip."""
    import maskshade as MI
    g = _mask([
        '................',
        '................',
        '........####....',
        '.......######...',
        '.......######...',
        '......######....',
        '......#####.....',
        '.....#####......',
        '.....####.......',
        '....####........',
        '....###.........',
        '...###..........',
        '...##...........',
        '..##............',
        '..#.............',
        '................',
    ])
    t = MI.shade(g, [hx(c) + (255,) for c in ('#4e4636', '#a8987a', '#d6ccb0', '#ece6d2', '#fbf8ee')])
    a = t.a.astype(np.float64)
    for y in range(2, 6):
        for x in range(6, 14):
            if a[y, x, 3] > 0 and tuple(a[y, x, :3].astype(int)) != hx('#4e4636'):
                a[y, x, :3] = a[y, x, :3] * np.array([0.92, 0.84, 0.66])
    return a


def glow_lamp():
    """A Glow Fly's light gland in a copper lantern cage: the vanilla copper lantern, its flame a golden glow."""
    a = vanilla('item/copper_lantern').copy()
    rgb = a[..., :3]
    mx, mn = rgb.max(-1), rgb.min(-1)
    sat = (mx - mn) / np.maximum(mx, 1)
    core = (a[..., 3] > 0) & (rgb[..., 1] > rgb[..., 0] * 1.05) & (sat > 0.18)
    return recolor(a, GLAND[1:], core)


def items():
    """{item texture name: RGBA array}."""
    pod = vanilla('item/pitcher_pod')
    egg = vanilla('item/turtle_egg')
    return {
        'pitcher_pod': recolor(pod, POD),
        'roasted_pitcher_pod': recolor(pod, ROAST),
        'glow_gland': recolor(vanilla('item/glow_ink_sac'), GLAND),
        'glow_lamp': glow_lamp(),
        'mantis_scythe': scythe_arm(),
        'ponder_egg': recolor(egg, EGG),
        'baked_ponder_egg': recolor(egg, BAKED),
        'magnesium': recolor(vanilla('item/glowstone_dust'), MAGNESIUM),
        'cruncher_tooth': cruncher_tooth(),
    }


# ============================================================================ spawn eggs (front views, small pixel eyes)

def eggs():
    import fish_items as FI
    out = {}
    out['glow_fly_spawn_egg'] = FI._egg([
        '........',
        '.w......',
        'www..a..',
        '.www.a..',
        '..ww..a.',
        '....bbbb',
        '...bnbbb',
        '...bbbbb',
        '..wbbbmb',
        '.wwbbbbb',
        '..LLLLLL',
        '.LLlLLLL',
        '.LLLLLLL',
        '..LLLLLL',
        '...LLLLL',
        '........',
    ], {'a': '#3a2408', 'b': '#cca02a', 'c': '#f6d65c', 'd': '#8a6412', 'n': '#141006', 'm': '#2a1e0a', 'w': '#b49e50',
        'L': '#ffde4c', 'l': '#fff4a8'}, x_lit=6, y_lit=8, x_dark=11, y_dark=8)
    out['crocotodo_spawn_egg'] = FI._egg([
        '.....RR.',
        '....RrrR',
        '...abbbb',
        '..abbbbb',
        '..abnbbb',
        '..abbbbb',
        '..abbsss',
        '...absss',
        '...assss',
        '....asst',
        '....asss',
        '....asst',
        '....asss',
        '.....aas',
        '......aa',
        '........',
    ], {'a': '#081a18', 'b': '#2e6e5a', 'c': '#58a084', 'd': '#184036', 'n': '#05070a', 'R': '#c45224', 'r': '#f08e4c',
        's': '#5c6232', 't': '#e6dcc0'}, x_lit=6, y_lit=6, x_dark=11, y_dark=11)
    out['mantis_spawn_egg'] = FI._egg([
        '..a.....',
        '...a....',
        '...a....',
        '..bbbbbb',
        '..Ebbbbb',
        '...bbbbb',
        '....bbbb',
        '....bbmm',
        '.s...bbb',
        '.ss..pbb',
        '.s.s.pbb',
        '.s..spbb',
        '..s..pbb',
        '...s.pKb',
        '.....bbb',
        '........',
    ], {'a': '#132e15', 'b': '#316b2e', 'c': '#5aa04a', 'd': '#1b3f1c', 'E': '#52aed8', 'm': '#98985e', 's': '#c6da88',
        'p': '#8ca64c', 'K': '#9ae6f8'}, x_lit=6, y_lit=7, x_dark=11, y_dark=11)
    out['colossus_ponder_spawn_egg'] = FI._egg([
        '........',
        '...f....',
        '..ffg...',
        '.GGGGGGG',
        '.GgGGGgG',
        'aBBbbbbb',
        'aBnBbbbb',
        'abbbbbbb',
        'abbbbbbb',
        'abmmmmmm',
        'akkkkkkk',
        'akkkkkkk',
        '.akkkkkk',
        '..aakkkk',
        'wwwaaaaa',
        '........',
    ], {'a': '#0d1c10', 'b': '#316333', 'c': '#549452', 'd': '#1c3b1f', 'n': '#7a3c08', 'B': '#3f7a40', 'G': '#2f969e',
        'g': '#7ad8d4', 'f': '#408a40', 'm': '#330b1c', 'k': '#b0ac6a', 'w': '#264e28'}, x_lit=6, y_lit=7, x_dark=11, y_dark=10)
    out['ponder_tadpole_spawn_egg'] = FI._egg([
        '........',
        '........',
        '....GGGG',
        '...abbbb',
        '..abnbbb',
        '..abbbbb',
        '..abbbbb',
        '..ammmmm',
        '..atmtmt',
        '..akkkkk',
        '...akkkk',
        '....aabb',
        '......ab',
        '......ff',
        '.....fff',
        '........',
    ], {'a': '#13140c', 'b': '#444a2a', 'c': '#6c7642', 'd': '#282a18', 'n': '#7a3c08', 'G': '#2f969e', 'm': '#330b1c',
        't': '#ddd3c0', 'k': '#8c8c6a', 'f': '#a2aa78'}, x_lit=6, y_lit=6, x_dark=11, y_dark=10)
    out['cruncher_spawn_egg'] = FI._egg([
        '........',
        '...PP...',
        '..PPPPPP',
        '..aPbbbb',
        '..abbbbb',
        '..abnbbb',
        '..abbbbb',
        '..abbbbb',
        '..atbtbt',
        '..ammmmm',
        '..atbtbt',
        '...abbbb',
        '...akkkk',
        '....akkk',
        '.....aaa',
        '........',
    ], {'a': '#1a1b1c', 'b': '#53575a', 'c': '#7c8083', 'd': '#333638', 'n': '#d65a18', 'P': '#d3c7b8', 'm': '#330b1c',
        't': '#efe6cc', 'k': '#c2b5a6'}, x_lit=6, y_lit=6, x_dark=11, y_dark=10)
    return out


# ============================================================================ hook (gen_textures.main, before vanilla_remap)

BLOCKS = ['giant_pitcher_side', 'giant_pitcher_rim', 'giant_pitcher_lid', 'giant_pitcher_juice', 'giant_pitcher_leaves', 'giant_pitcher_pod',
          'pitcher_sprout_stage0', 'pitcher_sprout_stage1', 'pitcher_sprout_stage2', 'bored_log']


def textures(out):
    import vanilla_remap as VR
    out('block/giant_pitcher_side', img(pitcher_side()))
    out('block/giant_pitcher_rim', img(pitcher_rim()))
    out('block/giant_pitcher_lid', img(pitcher_lid()), CUT)
    out('block/giant_pitcher_juice', img(pitcher_juice()))
    out('block/giant_pitcher_leaves', img(pitcher_leaves()), CUT)
    out('block/giant_pitcher_pod', img(pitcher_pod_block()))
    for s in range(3):
        out(f'block/pitcher_sprout_stage{s}', img(sprout(s)), CUT)
    out('block/bored_log', img(bored_log()))
    for name, a in items().items():
        out(f'item/{name}', img(a))
    for name, im in eggs().items():
        out(f'item/{name}', im)
    VR.SKIP.update(BLOCKS)
