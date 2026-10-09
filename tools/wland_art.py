"""W-land art: 16x16 Mojang-style textures for the Rocky Dunes and the White Forest (see tools/wland.py).

Stone, sand and wood are vanilla textures re-themed by tools/vanilla_remap.py (the swatches written here only give it
their palettes: Chime Sandstone <- sandstone, Dunestone <- dripstone block, Banded Dunestone <- red sandstone, White
Lullwood <- birch). Plants, flowers, Rainbow Snow, the items and the snowflake particle are painted here by hand (pixel
maps, 4-6 shades a material, light from the top left) and kept out of the remap (vanilla_remap.SKIP).
"""
import colorsys
import os
import random

import numpy as np
from PIL import Image

VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')
CUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}


def hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*cs):
    return [hx(c) for c in cs]


# palettes (dark -> light), sampled to sit with the Sift's pale stone, pink coral ground and teal flora
CHIME = ramp('#a597bd', '#b8acce', '#cac0dd', '#dad2e9', '#e8e2f2', '#f5f2fb')      # Chime Sandstone (Chime Sand's lilac-white)
DUNE = ramp('#74404e', '#8c505d', '#a3626c', '#b8777c', '#c98c8c', '#d9a4a0')       # Dunestone: dusty rose rock
BANDED = ramp('#4a2235', '#5d2c41', '#73394f', '#89485e', '#9d596c', '#b26e7d')     # Banded Dunestone: plum strata
BARK = ramp('#8a8fae', '#a6abc6', '#c3c7dc', '#dcdeec', '#eceef6', '#fafbfe')       # White Lullwood bark: pearl, soft periwinkle marks
PLANK = ramp('#9c93b0', '#b1a9c3', '#c4bdd4', '#d5cfe2', '#e4e0ee', '#f0edf6')      # White Lullwood wood: lilac pearl
CACTUS = ramp('#1f4b52', '#2a6166', '#377a7a', '#4b9590', '#68afa5', '#90cbbd')     # Tuning Cactus: dusty teal
MINT = ramp('#3d7a72', '#579a8e', '#77b8a9', '#9dd2c2', '#c7e9dc', '#ecfaf3')       # the crown's tuning-fork prongs
FRUIT = ramp('#6e2240', '#952f52', '#bb4766', '#da687d', '#f08f98', '#ffc0bb')      # Tuning Fruit: coral pink
THORN = ramp('#5a2f3a', '#74404b', '#8e535c', '#a8686f', '#c08485')                 # Rattlethorn wood: sun-bleached rose
SPINE = ramp('#b9a49b', '#d8c7bc', '#f1e6dc')
RATTLE = ramp('#7a3a58', '#a35478', '#c97a98', '#e9a9c0')
STEM = ramp('#2c5a55', '#3b7268', '#4f8c7d', '#6aa893', '#8fc4ad')                  # frost-flower stems (Sift teal-sage)
PETAL = ramp('#9d8fc4', '#b8aedb', '#d3cceb', '#e9e5f6', '#fbf9ff')
ICE = ramp('#2f5c86', '#3f7aa8', '#5a9cc6', '#83bfdf', '#b4dcf0', '#e4f4fb')


def img(a):
    return Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA')


def vanilla(name):
    return np.asarray(Image.open(os.path.join(VANILLA, name + '.png')).convert('RGBA'), dtype=np.float64)


def lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def recolor(a, pal):
    """A vanilla texture's own values (ranked) mapped onto a palette: Mojang's clusters, our colours."""
    out = a.copy()
    m = a[..., 3] > 0
    L = lum(a)[m]
    r = np.argsort(np.argsort(L, kind='stable'), kind='stable') / max(1, len(L) - 1)
    out[m, :3] = np.array(pal, dtype=np.float64)[np.clip(np.round(r * (len(pal) - 1)).astype(int), 0, len(pal) - 1)]
    return out


def swatch(pal, seed):
    """A 16x16 of the palette's shades in clustered noise: vanilla_remap clones the vanilla layout in these colours."""
    rnd = random.Random(seed)
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    for y in range(16):
        for x in range(16):
            a[y, x, :3] = pal[(x // 2 + y // 2 + rnd.randrange(len(pal))) % len(pal)]
    return a


def pixmap(rows, key, size=16):
    """Paints a sprite from a character map ('.' = clear)."""
    a = np.zeros((size, size, 4))
    for y, row in enumerate(rows[:size]):
        for x, ch in enumerate(row[:size]):
            if ch != '.' and ch != ' ':
                c = key[ch]
                a[y, x, :3] = c[:3]
                a[y, x, 3] = c[3] if len(c) > 3 else 255
    return a


# ============================================================================ the desert plants

def rattlethorn():
    """Vanilla's dead bush, re-grown as a spiky rose-brown bramble with bone spines and rattling seed pods."""
    a = recolor(vanilla('block/dead_bush'), THORN)
    m = a[..., 3] > 0
    rnd = random.Random(5)
    # spines: off the branch tips and sides, 1 px, pale bone with a shaded root
    for y in range(1, 15):
        for x in range(1, 15):
            if m[y, x] and rnd.random() < 0.33:
                dx, dy = rnd.choice([(-1, -1), (1, -1), (-1, 0), (1, 0)])
                if not m[y + dy, x + dx]:
                    a[y + dy, x + dx] = (*SPINE[2 if dy < 0 else 1], 255)
    # seed pods: 2x2 capsules on the outer twigs, lit from the top left
    for x, y in ((3, 4), (11, 3), (7, 2), (12, 8), (2, 9)):
        if m[max(0, y - 2):y + 3, max(0, x - 2):x + 3].any():
            a[y, x] = (*RATTLE[3], 255)
            a[y, x + 1] = (*RATTLE[2], 255)
            a[y + 1, x] = (*RATTLE[1], 255)
            a[y + 1, x + 1] = (*RATTLE[0], 255)
    return a


def cactus_stem():
    """Vertical ribs (light ridge, dark groove), a pale lilac spine on every other rib every four pixels."""
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    rnd = random.Random(21)
    for y in range(16):
        for x in range(16):
            rib = x % 3
            base = [2, 4, 3][rib]
            if rnd.random() < 0.18:
                base += rnd.choice((-1, 1))
            a[y, x, :3] = CACTUS[int(np.clip(base, 0, 5))]
    for x in range(1, 16, 3):
        for y in range((x // 3) % 2 * 2, 16, 4):
            a[y, x, :3] = hx('#e9dcf2')
            if y + 1 < 16:
                a[y + 1, x, :3] = CACTUS[1]
    return a


def cactus_bud():
    """The growing tip: the stem's ribs closing up into a coral-pink point."""
    a = cactus_stem()
    for y in range(0, 7):
        for x in range(16):
            t = (7 - y) / 7
            if (x + y) % 3 == 0 or t > 0.55:
                a[y, x, :3] = FRUIT[int(np.clip(2 + t * 3 + (x % 3 == 1), 0, 5))]
    return a


def cactus_crown():
    """The tuning-fork prongs: smooth pale mint, a bright highlight edge and a darker shadow edge."""
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    rnd = random.Random(23)
    for y in range(16):
        for x in range(16):
            v = 3 + (1 if (x % 4 == 0) else 0) - (1 if (x % 4 == 3) else 0) + (1 if rnd.random() < 0.12 else 0)
            a[y, x, :3] = MINT[int(np.clip(v, 0, 5))]
    return a


def fruit_pod():
    """Tuning Fruit on the vine: coral skin, lilac freckles, a soft highlight."""
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    rnd = random.Random(29)
    for y in range(16):
        for x in range(16):
            v = 3 + (1 if (x + y) % 4 < 2 and rnd.random() < 0.5 else 0) - (1 if rnd.random() < 0.2 else 0)
            a[y, x, :3] = FRUIT[int(np.clip(v - (y % 4 == 3), 0, 5))]
            if rnd.random() < 0.07:
                a[y, x, :3] = hx('#f6e2ff')
    return a


# ============================================================================ the frost flowers

W = (255, 255, 255)
G = {'d': STEM[0], 's': STEM[1], 'm': STEM[2], 'l': STEM[3], 'h': STEM[4]}


def halo_lily_stem():
    return pixmap([
        '................', '................', '................', '.......m........', '.......m........', '.......sm.......',
        '........m.......', '..hl....m...l...', '...lm..sm..lh...', '....ls.m..sm....', '.....sm.msd.....', '.......md.......',
        '.......sd.......', '.......md.......', '......ssmd......', '.....d.sd.d.....'], G)


def halo_lily_halo():
    """Seen from above: eight petals in a floating ring, pearl with lilac veins and rose tips, open in the middle."""
    a = np.zeros((16, 16, 4))
    c = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - c, y - c
            r = np.hypot(dx, dy)
            ang = (np.arctan2(dy, dx) / (2 * np.pi)) % 1.0
            petal = abs(((ang * 8) % 1.0) - 0.5) * 2  # 0 at a petal's midline
            if 3.6 <= r <= 7.2 - 1.6 * petal:
                light = 0.5 - 0.5 * (dx + dy) / 10.0
                k = int(np.clip(1 + light * 3 + (r > 6.2) * -1, 0, 4))
                col = PETAL[k]
                if r > 6.0 and petal < 0.35:
                    col = hx('#f3a6c8')
                elif petal < 0.12 and r < 5.5:
                    col = PETAL[0]
                a[y, x, :3] = col
                a[y, x, 3] = 255
    return a


def glow_core(pal):
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    for y in range(16):
        for x in range(16):
            a[y, x, :3] = pal[(x * 3 + y * 5) % len(pal)] if (x + y) % 2 else pal[-1]
    return a


def snowglobe_stem():
    return pixmap([
        '................', '................', '................', '................', '................', '................',
        '................', '.......m........', '.......m........', '..hl...sm..lh...', '...lms.m..sml...', '.....smmsd......',
        '.......md.......', '.......sd.......', '......smd.......', '.....d.sd.d.....'], G)


def snowglobe_glass():
    """Vanilla glass made round: an 8x8 face (uv 4..12) with a pale frame, clipped corners, two glints and a few frost
    flakes on the clear middle."""
    a = np.zeros((16, 16, 4))
    frame, edge, glint = (214, 232, 246, 255), (176, 200, 226, 255), (255, 255, 255, 255)
    for i in range(5, 11):
        a[4, i] = frame
        a[i, 4] = frame
        a[11, i] = edge
        a[i, 11] = edge
    for (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)):
        a[y, x] = frame if x == 5 or y == 5 else edge
    for (x, y) in ((6, 6), (7, 6), (6, 7), (9, 9)):
        a[y, x] = glint
    for (x, y, col) in ((8, 8, '#ffd6ec'), (7, 9, '#d6f2ff'), (9, 7, '#fff3c4')):
        a[y, x] = (*hx(col), 255)
    return a


def snowglobe_core():
    """The swirl of rainbow snow inside: pastel flakes round the colour wheel."""
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    rnd = random.Random(31)
    for y in range(16):
        for x in range(16):
            h = ((x + y) / 16.0 + rnd.random() * 0.15) % 1.0
            r, g, b = colorsys.hsv_to_rgb(h, 0.32, 1.0)
            a[y, x, :3] = (r * 255, g * 255, b * 255)
            if rnd.random() < 0.25:
                a[y, x, :3] = (250, 252, 255)
    return a


def thistle_stem():
    key = dict(G)
    key['f'] = hx('#d9eef2')
    return pixmap([
        '................', '................', '................', '................', '................', '.......m........',
        '.......m........', '.f.....sm....f..', '..lf...m....fl..', '...ls..m...sl...', '....lsmm..sl.....', '......smssm.....',
        '.......md.......', '...f...sd...f...', '....lssmdssl....', '.....d.sd.d.....'], key)


def thistle_head():
    """Icy scales: pale blue cells with darker joins, a frost-white tip on each."""
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    for y in range(16):
        for x in range(16):
            cx, cy = (x + (y // 2) % 2) % 2, y % 2
            k = 3 if (cx == 0 and cy == 0) else (2 if cy == 0 else 1)
            a[y, x, :3] = ICE[k + (1 if (x + y) % 5 == 0 else 0)]
    for y in range(0, 16, 2):
        for x in range((y // 2) % 2, 16, 2):
            a[y, x, :3] = ICE[5]
    return a


def thistle_spikes():
    """Radiating ice spines round the head (shown as two crossed planes, uv 2..14 x 0..10)."""
    a = np.zeros((16, 16, 4))
    cx, cy = 7.5, 5.5
    for k in range(14):
        ang = k * 2 * np.pi / 14 + 0.2
        for t in np.linspace(2.4, 6.6, 9):
            x, y = int(round(cx + np.cos(ang) * t)), int(round(cy + np.sin(ang) * t * 0.95))
            if 2 <= x < 14 and 0 <= y < 11:
                a[y, x] = (*ICE[5 if t > 5.4 else (4 if t > 4 else 3)], 255)
    return a


# ============================================================================ Rainbow Snow

FRAMES, FRAMETIME = 32, 4


def rainbow_snow(variant):
    """Vanilla snow, its whites drifting slowly round the rainbow (32 frames, interpolated); four variants a few frames
    apart (the blockstate picks one per position) so a snowfield shimmers in patches instead of changing all at once."""
    base = vanilla('block/snow')[:16]
    L = lum(base)
    lo, hi = L.min(), L.max()
    v = 0.86 + 0.14 * (L - lo) / max(1.0, hi - lo)
    rnd = random.Random(70 + variant)
    sparkles = [(rnd.randrange(16), rnd.randrange(16), rnd.random()) for _ in range(10)]
    frames = []
    for f in range(FRAMES):
        a = np.zeros((16, 16, 4))
        a[..., 3] = 255
        t = (f / FRAMES + variant * 0.09) % 1.0
        for y in range(16):
            for x in range(16):
                h = (t + 0.05 * np.sin(2 * np.pi * (x + y) / 16.0)) % 1.0
                r, g, b = colorsys.hsv_to_rgb(h, 0.12, min(1.0, v[y, x] + 0.04))
                a[y, x, :3] = (r * 255, g * 255, b * 255)
        for (x, y, ph) in sparkles:
            s = 0.5 + 0.5 * np.cos(2 * np.pi * (f / FRAMES * 2 + ph))
            r, g, b = colorsys.hsv_to_rgb((t + 0.5 + ph * 0.3) % 1.0, 0.6 * s, 1.0)
            a[y, x, :3] = (r * 255, g * 255, b * 255)
        frames.append(a)
    return np.concatenate(frames, 0)


# ============================================================================ items

def outline(a, darken=0.45):
    """Mojang item outline: 1 px of a darker shade of the neighbouring colour round the sprite."""
    out = a.copy()
    m = a[..., 3] > 0
    for y in range(16):
        for x in range(16):
            if m[y, x]:
                continue
            nb = [(y + dy, x + dx) for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)) if 0 <= y + dy < 16 and 0 <= x + dx < 16 and m[y + dy, x + dx]]
            if nb:
                c = np.mean([a[p][:3] for p in nb], axis=0) * darken
                out[y, x] = (*c, 255)
    return out


def tuning_fruit_item():
    k = {'1': FRUIT[0], '2': FRUIT[1], '3': FRUIT[2], '4': FRUIT[3], '5': FRUIT[4], '6': FRUIT[5], 'w': hx('#fff1f6'),
         'f': hx('#e8d4ff'), 't': CACTUS[2], 'T': CACTUS[4], 'd': CACTUS[0]}
    a = pixmap([
        '................', '......T.t.......', '.....tTtTt......', '......dtd.......', '....3445543.....', '...345665543....',
        '..3456w65f43....', '..345w6554432...', '..34565545f32...', '..2f4554443321..', '..23444f433321..', '...233333221...',
        '....2232221.....', '.....11111......', '................', '................'], k)
    return outline(a)


def flower_item(name):
    if name == 'halo_lily':
        k = dict(G)
        k.update({'p': PETAL[3], 'P': PETAL[4], 'q': PETAL[1], 'r': hx('#f3a6c8'), 'y': hx('#fff4b8'), 'Y': hx('#ffe07a')})
        rows = ['................', '.......yy.......', '.......YY.......', '...rqpPPPPpqr...', '..rpq.....qpr...', '...rqppppppqr....', '.......m........',
                '.......m........', '..hl...sm.......', '...lm..m...lh...', '....ls.m..sm....', '.....smmmsd.....', '.......md.......', '.......sd.......',
                '......ssmd......', '................']
    elif name == 'snowglobe_bloom':
        k = dict(G)
        k.update({'g': (226, 238, 248), 'G': (196, 214, 232), 'w': W, 'a': hx('#ffd6ec'), 'b': hx('#d6f2ff'), 'c': hx('#fff3c4'),
                  'e': hx('#e2d6ff'), 'x': (238, 246, 252)})
        rows = ['................', '.....GggggG.....', '....gwx.....g...', '...gw.a..c..g...', '...gx...b...g...', '...g..e...a.g...', '...g...c..b.g...',
                '....g.b..e.g....', '.....GgggggG....', '.......sm.......', '..hl...m....lh..', '...lm..m...sl...', '....lssmmssl....', '.......md.......',
                '......ssmd......', '................']
    else:
        k = dict(G)
        k.update({'i': ICE[2], 'I': ICE[3], 'j': ICE[4], 'J': ICE[5], 'k': ICE[1], 'f': hx('#d9eef2')})
        rows = ['....J..J..J.....', '.J..j..j..j..J..', '..jj.jIjIj.jj...', '...jIjIJIjIj....', 'Jj.IiIjIjIiI.jJ.', '...jIiIiIiIj....', '..jj.kIkIk.jj...',
                '.J...kkikk...J..', '.......m........', '.f.....sm....f..', '..lf...m....fl..', '...ls..m...sl...', '....lsmmmmsl.....', '.......md.......',
                '......ssmd......', '................']
    return outline(pixmap(rows, k))


def door_item():
    return recolor(vanilla('item/birch_door'), PLANK)


# ============================================================================ particles

def snowflakes():
    flakes = [
        ['..x..', '.xxx.', 'xxxxx', '.xxx.', '..x..'],
        ['x.x.x', '.xxx.', 'xx.xx', '.xxx.', 'x.x.x'],
        ['.x.', 'xxx', '.x.'],
    ]
    out = []
    for f in flakes:
        a = np.zeros((8, 8, 4))
        o = (8 - len(f)) // 2
        for y, row in enumerate(f):
            for x, ch in enumerate(row):
                if ch == 'x':
                    a[o + y, o + x] = (255, 255, 255, 255 if (x + y) % 2 == 0 or len(f) == 3 else 215)
        out.append(a)
    return out


# ============================================================================ hook (gen_textures.main, before vanilla_remap)

HAND = ['rattlethorn', 'tuning_cactus', 'tuning_cactus_bud', 'tuning_cactus_crown', 'tuning_fruit_pod', 'halo_lily_stem', 'halo_lily_halo',
        'halo_lily_bud', 'snowglobe_bloom_stem', 'snowglobe_bloom_glass', 'snowglobe_bloom_core', 'shiver_thistle_stem', 'shiver_thistle_head',
        'shiver_thistle_spikes'] + [f'rainbow_snow_{v}' for v in range(4)]


def textures(out):
    import vanilla_remap as VR
    # --- re-themed vanilla (palettes here, layouts cloned from vanilla by vanilla_remap)
    sw = {'chime_sandstone': CHIME, 'chime_sandstone_top': CHIME, 'chime_sandstone_bottom': CHIME, 'smooth_chime_sandstone': CHIME,
          'cut_chime_sandstone': CHIME, 'chiseled_chime_sandstone': CHIME, 'dunestone': DUNE, 'banded_dunestone_side': BANDED,
          'banded_dunestone_top': BANDED, 'white_lullwood_log': BARK, 'stripped_white_lullwood_log': PLANK,
          'stripped_white_lullwood_log_top': PLANK, 'white_lullwood_planks': PLANK}
    for i in range(4):
        sw[f'suspicious_chime_sand_{i}'] = CHIME[1:]
    for n, pal in sw.items():
        out(f'block/{n}', img(swatch(pal, len(n))))
    out('block/white_lullwood_log_top', img(swatch(PLANK, 3)))
    for n in ('white_lullwood_door_top', 'white_lullwood_door_bottom', 'white_lullwood_trapdoor'):
        out(f'block/{n}', img(recolor(vanilla('block/' + n.replace('white_lullwood', 'birch')), PLANK)), CUT)
    C, F = VR.C, VR.F
    VR.MAP.update({
        'chime_sandstone': C('sandstone'), 'chime_sandstone_top': C('sandstone_top'), 'chime_sandstone_bottom': C('sandstone_bottom'),
        'smooth_chime_sandstone': C('sandstone_top'), 'cut_chime_sandstone': C('cut_sandstone'),
        'chiseled_chime_sandstone': C('chiseled_sandstone'),
        **{f'suspicious_chime_sand_{i}': C(f'suspicious_sand_{i}') for i in range(4)},
        'dunestone': C('dripstone_block'), 'banded_dunestone_side': C('red_sandstone'), 'banded_dunestone_top': C('red_sandstone_top'),
        'white_lullwood_log': C('birch_log', spread=48), 'white_lullwood_log_top': F(VR._ring, 'birch_log_top', 'white_lullwood_log'),
        'stripped_white_lullwood_log': C('stripped_birch_log'), 'stripped_white_lullwood_log_top': C('stripped_birch_log_top'),
        'white_lullwood_planks': C('birch_planks'), 'white_lullwood_door_top': C('birch_door_top'),
        'white_lullwood_door_bottom': C('birch_door_bottom'), 'white_lullwood_trapdoor': C('birch_trapdoor'),
    })
    # --- painted by hand
    out('block/rattlethorn', img(rattlethorn()), CUT)
    out('block/tuning_cactus', img(cactus_stem()))
    out('block/tuning_cactus_bud', img(cactus_bud()))
    out('block/tuning_cactus_crown', img(cactus_crown()))
    out('block/tuning_fruit_pod', img(fruit_pod()))
    out('block/halo_lily_stem', img(halo_lily_stem()), CUT)
    out('block/halo_lily_halo', img(halo_lily_halo()), CUT)
    out('block/halo_lily_bud', img(glow_core([hx('#bff4ff'), hx('#fff6c8'), hx('#ffe9a0')])))
    out('block/snowglobe_bloom_stem', img(snowglobe_stem()), CUT)
    out('block/snowglobe_bloom_glass', img(snowglobe_glass()))
    out('block/snowglobe_bloom_core', img(snowglobe_core()))
    out('block/shiver_thistle_stem', img(thistle_stem()), CUT)
    out('block/shiver_thistle_head', img(thistle_head()))
    out('block/shiver_thistle_spikes', img(thistle_spikes()), CUT)
    for v in range(4):
        out(f'block/rainbow_snow_{v}', img(rainbow_snow(v)), {'animation': {'frametime': FRAMETIME, 'interpolate': True}})
    out('item/tuning_fruit', img(tuning_fruit_item()))
    for f in ('halo_lily', 'snowglobe_bloom', 'shiver_thistle'):
        out(f'item/{f}', img(flower_item(f)))
    out('item/white_lullwood_door', img(door_item()))
    for i, a in enumerate(snowflakes()):
        out(f'particle/rainbow_snowflake_{i}', img(a))
    VR.SKIP.update(HAND)
