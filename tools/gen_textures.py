"""Paints every block/item/particle/environment texture The Sift needs.

Run after gen_assets.py (which lists the referenced textures in build/textures_needed.txt).
"""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(__file__))
import hdblocks as HB  # noqa: E402
import hditems as HI  # noqa: E402
import mcitems as MI  # noqa: E402
import mctex as M  # noqa: E402
import sprites as S  # noqa: E402
from texlib import (Tex, bricks, cobbled, darken, draw_map, fbm, hx, iridescent, lerp, lighten, polished, quantize, ramp,  # noqa: E402
                    shade_sprite, stone, tiles, value_noise, with_alpha)

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
TEX = os.path.join(ROOT, 'src/main/resources/assets/thesift/textures')

# ------------------------------------------------------------------ palettes
DREAM = ramp('#4e5a99', '#6572b3', '#7d8bcb', '#97a5de', '#b3c0ee', '#cfd8f7')
DREAM_MORTAR = hx('#3d4580')
HUSH = ramp('#152331', '#1c2e3f', '#243a4d', '#2e485c', '#3a586c', '#4b6c7e')
HUSH_MORTAR = hx('#0e1822')
SAND = ramp('#cf7fa4', '#dd95b6', '#e8a9c6', '#f1bdd4', '#f8d0e1', '#fde3ee')
BLUSH = ramp('#7a2442', '#983457', '#b3496d', '#c96284', '#dc7e9b', '#eb9db4')
BLUSH_MORTAR = hx('#4a1528')
SOIL = ramp('#6a4252', '#7b4f5f', '#8d5e6d', '#9e6e7c', '#b0808c')
GRASS = ramp('#2d9a98', '#3cb3ab', '#50c8bb', '#69dbca', '#8cebd9')
LUMEN = ramp('#1f6f80', '#29889a', '#34a2b0', '#43bcc4', '#62d6d6')
SERBIM = ramp('#1f6d94', '#2b8fbb', '#3fb0da', '#66d2f0', '#9aeefc', '#d4fbff')
SIFTITE = ramp('#2a8ea8', '#3fb6cb', '#63d6e2', '#9aeaf0', '#d8fbfd')
SIFTITE_PINK = hx('#f29bd6')
LULL_BARK = ramp('#1b2830', '#24353e', '#2f444e', '#3c5560', '#4a6670')
LULL_WOOD = ramp('#9f97c6', '#b1a9d4', '#c2bbe0', '#d3cdea', '#e3def3')
WISH_BARK = ramp('#4a1f38', '#5a2744', '#6f3355', '#844267', '#9a5379')
WISH_WOOD = ramp('#c9738f', '#d98aa4', '#e6a0b8', '#f0b6ca', '#f8cbdb')
LULL_LEAF = ramp('#7f97a6', '#a4bac6', '#bfd2db', '#d6e5eb', '#ebf3f6', '#f8fcfd')
WISH_LEAF = ramp('#d86aa4', '#e886b8', '#f3a2cb', '#f9bcdb', '#fdd5e8', '#fff0f7')

NEEDED = []


def out(name, t, meta=None):
    path = os.path.join(TEX, name + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img = t.img() if isinstance(t, Tex) else t
    img.save(path)
    if meta:
        with open(path + '.mcmeta', 'w') as f:
            json.dump(meta, f, indent=2)


def strip(frames):
    """Stacks animation frames into the vertical strip an animated texture is stored as."""
    w, h = frames[0].size
    img = Image.new('RGBA', (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        img.paste(f, (0, i * h))
    return img


CUTOUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}
LEAVES_META = {'texture': {'mipmap_strategy': 'dark_cutout'}}


def sprite(data, shade=False):
    rows, keys = data
    t = Tex(len(rows[0]), len(rows))
    draw_map(t, rows, {k: hx(v) for k, v in keys.items()})
    if shade:
        shade_sprite(t, 0.15, 0.2)
    return t


def pal_sprite(rows, keys):
    t = Tex(len(rows[0]), len(rows))
    draw_map(t, rows, {k: (hx(v) if isinstance(v, str) else v) for k, v in keys.items()})
    return t


# ================================================================== stone families


def crack(t, seed, color, n=2):
    rnd = random.Random(seed)
    for _ in range(n):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for _ in range(rnd.randrange(4, 8)):
            t.set(x, y, color)
            x = (x + rnd.choice((-1, 0, 1))) % 16
            y = (y + rnd.choice((0, 1, 1))) % 16
    return t


def moss_over(t, seed, pal, glow=None, density=0.35):
    """Moss patches creeping down from the top: two tones, lit along their upper edges."""
    n = fbm(16, 16, seed, (8, 4, 2))
    rnd = random.Random(seed)
    moss = [[n[y, x] + (0.25 if y < 4 else 0.1 if y < 8 else 0) > 1 - density for x in range(16)] for y in range(16)]
    for y in range(16):
        for x in range(16):
            if moss[y][x]:
                top = y == 0 or not moss[y - 1][x]
                t.set(x, y, pal[3] if top else pal[1] if (x + y) % 5 else pal[0])
                if glow and rnd.random() < 0.04:
                    t.set(x, y, glow)
    return t


def chiseled(pal, motif, seed, keys):
    t = M.polished(pal[1:6], seed)
    draw_map(t, motif, keys, 3, 3)
    return t


RUNE = [
    '..........',
    '....LL....',
    '...L..L...',
    '..L.dd.L..',
    '.L.dLLd.L.',
    '.L.dLLd.L.',
    '..L.dd.L..',
    '...L..L...',
    '....LL....',
    '..........',
]


def stone_textures():
    D = DREAM
    out('block/dreamstone', M.stone(D[0:5], 11))
    out('block/cobbled_dreamstone', M.cobbled(D[0:5], DREAM_MORTAR, 12))
    out('block/polished_dreamstone', M.polished(D[1:6], 13))
    out('block/dreamstone_bricks', M.bricks(D[1:6], DREAM_MORTAR, 14))
    out('block/cracked_dreamstone_bricks', crack(M.bricks(D[1:6], DREAM_MORTAR, 14), 15, DREAM_MORTAR, 3))
    out('block/mossy_dreamstone_bricks', moss_over(M.bricks(D[1:6], DREAM_MORTAR, 14), 16, LUMEN))
    out('block/dreamstone_tiles', M.tiles(D[1:6], DREAM_MORTAR, 17))
    out('block/chiseled_dreamstone', chiseled(D, RUNE, 18, {'L': D[5], 'd': D[0]}))
    pil = M.stone(D[1:6], 19, light=3, dark=3)
    for y in range(16):
        pil.set(0, y, D[5]); pil.set(1, y, D[4]); pil.set(14, y, D[1]); pil.set(15, y, D[0])
        if y % 8 == 0:
            for x in range(2, 14):
                pil.set(x, y, D[1])
    out('block/dreamstone_pillar_side', pil)
    out('block/dreamstone_pillar_top', M.polished(D[1:6], 20))
    hs = M.stone(HUSH[0:5], 21, flecks=(hx('#3fd6d0'), 2))
    for y in range(0, 16, 4):
        for x in range(16):
            if random.Random(y * 16 + x).random() < 0.5:
                hs.set(x, y, HUSH[1])
    out('block/hushslate', hs)
    out('block/hushslate_top', M.polished(HUSH[0:5], 22, inner=False))
    out('block/cobbled_hushslate', M.cobbled(HUSH[0:5], HUSH_MORTAR, 23))
    out('block/polished_hushslate', M.polished(HUSH[1:6], 24))
    out('block/hushslate_bricks', M.bricks(HUSH[1:6], HUSH_MORTAR, 25))
    out('block/cracked_hushslate_bricks', crack(M.bricks(HUSH[1:6], HUSH_MORTAR, 25), 26, HUSH_MORTAR, 3))
    out('block/hushslate_tiles', M.tiles(HUSH[1:6], HUSH_MORTAR, 27, 4))
    out('block/chiseled_hushslate', chiseled(HUSH, RUNE, 28, {'L': hx('#4fe8e0'), 'd': HUSH[0]}))
    # sand & sandstone
    out('block/dreamsand', M.grain(SAND[0:5], 31))
    ss = M.stone(SAND[1:6], 32, light=3, dark=4, core=0.0)
    for x in range(16):
        ss.set(x, 0, SAND[5]); ss.set(x, 1, SAND[4]); ss.set(x, 2, SAND[2])
        ss.set(x, 10, SAND[1]); ss.set(x, 11, SAND[2])
        ss.set(x, 15, SAND[0])
    out('block/dreamsandstone', ss)
    out('block/dreamsandstone_top', M.stone(SAND[1:6], 33, light=4, dark=3, core=0.0))
    out('block/dreamsandstone_bottom', crack(M.stone(SAND[0:5], 34, light=3, dark=5, core=0.0), 35, SAND[0], 2))
    out('block/smooth_dreamsandstone', M.stone(SAND[1:6], 36, light=2, dark=2, core=0.0))
    cut = M.stone(SAND[1:6], 37, light=2, dark=2, core=0.0)
    for x in range(16):
        cut.set(x, 0, SAND[5]); cut.set(x, 15, SAND[0]); cut.set(x, 7, SAND[1]); cut.set(x, 8, SAND[4])
    out('block/cut_dreamsandstone', cut)
    ch = M.polished(SAND[1:6], 38)
    draw_map(ch, ['..........', '..hh..hh..', '.h..hh..h.', '.h..pp..h.', '..h.pp.h..', '...hppg...', '..h.pp.h..', '.h..pp..h.', '..hh..hh..',
                  '..........'], {'h': SAND[0], 'p': hx('#c95b8f'), 'g': hx('#ffe89a')}, 3, 3)
    out('block/chiseled_dreamsandstone', ch)
    for i in range(4):
        sus = M.grain(SAND[0:5], 31)
        rnd = random.Random(40 + i)
        for _ in range(2 + i * 2):
            x, y = rnd.randrange(3, 13), rnd.randrange(3, 13)
            sus.set(x, y, hx('#7fe8ff')); sus.set(x + 1, y, hx('#4fb9d6'))
        for _ in range(i * 6):
            sus.set(rnd.randrange(16), rnd.randrange(16), SAND[0])
        out(f'block/suspicious_dreamsand_{i}', sus)
    # blush bricks
    out('block/blush_bricks', M.bricks(BLUSH[1:6], BLUSH_MORTAR, 41))
    out('block/cracked_blush_bricks', crack(M.bricks(BLUSH[1:6], BLUSH_MORTAR, 41), 42, BLUSH_MORTAR, 3))
    out('block/chiseled_blush_bricks', chiseled(BLUSH, ['..........', '...gggg...', '..g....g..', '.g.pPPp.g.', '.g.PWWP.g.', '.g.PWWP.g.',
                                                        '.g.pPPp.g.', '..g....g..', '...gggg...', '..........'], 43,
                                                {'g': hx('#ffd98a'), 'p': BLUSH[0], 'P': BLUSH[4], 'W': hx('#ffe8f0')}))


# ================================================================== soils, ores, metals


def grass_side(top_pal, soil_pal, seed):
    t = stone(soil_pal[:4], seed, cells=(8, 4, 2))
    rnd = random.Random(seed)
    for x in range(16):
        depth = 3 + rnd.randrange(3) + (1 if x % 4 == 0 else 0)
        for y in range(depth):
            t.set(x, y, top_pal[min(len(top_pal) - 1, 4 - y if y < 4 else 1)] if y < depth - 1 else top_pal[0])
    for _ in range(5):
        t.set(rnd.randrange(16), rnd.randrange(6, 16), soil_pal[4])
    return t


def soils():
    soil = M.soil(SOIL[0:5], 51)
    out('block/sift_soil', soil)
    top = M.stone(GRASS[0:5], 52, light=8, dark=8, core=0.2)
    rnd = random.Random(53)
    for _ in range(3):
        top.set(rnd.randrange(16), rnd.randrange(16), hx('#f7a8d2'))
    out('block/sift_grass_block_top', top)
    out('block/sift_grass_block_side', M.grass_side(GRASS, soil, 54))
    # coral turf: the salmon-pink ground of the reference biome
    coral = ramp('#d65866', '#e6666f', '#ef7481', '#f87d8d', '#fc8f9d')
    out('block/coral_turf_top', M.stone(coral, 57, light=9, dark=9, core=0.25))
    out('block/coral_turf_side', M.grass_side(coral, soil, 59))
    moss = M.stone(LUMEN, 55, light=8, dark=6, core=0.2)
    rnd = random.Random(56)
    for _ in range(5):
        moss.set(rnd.randrange(16), rnd.randrange(16), hx('#c8fff6'))
    out('block/lumen_moss_block', moss)
    # ores
    for name, base, seed in (('serbim_ore', M.stone(DREAM[0:5], 61), 61), ('deep_serbim_ore', M.stone(HUSH[0:5], 62), 62)):
        out(f'block/{name}', M.ore(base, [SERBIM[0], SERBIM[2], SERBIM[3], SERBIM[5]], seed, outline=None))
    rb = M.cobbled(SERBIM[0:5], SERBIM[0], 63)
    out('block/raw_serbim_block', rb)
    sb = M.polished(SERBIM[1:6], 65)
    for x in range(3, 13):
        sb.set(x, 5, SERBIM[4]); sb.set(x, 10, SERBIM[2])
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        sb.set(x, y, SERBIM[0])
    out('block/serbim_block', sb)
    sf = M.polished(SIFTITE, 66)
    for i in range(3, 13):
        sf.set(i, i, SIFTITE[4])
        if i % 3 == 0:
            sf.set(i, 15 - i, SIFTITE_PINK)
    out('block/siftite_block', sf)
    g = Tex()
    frame = with_alpha(hx('#cdeff5'), 255)
    for i in range(16):
        g.set(i, 0, frame); g.set(0, i, frame); g.set(i, 15, with_alpha(hx('#9fd2e0'), 255)); g.set(15, i, with_alpha(hx('#9fd2e0'), 255))
    for k in range(4):
        g.set(3 + k, 6 - k, with_alpha(hx('#ffffff'), 200))
        g.set(9 + k, 12 - k, with_alpha(hx('#ffffff'), 170))
    g.set(4, 6, with_alpha(hx('#ffc6ea'), 200))
    out('block/chrome_glass', g)


# ================================================================== wood


def log_side(bark, seed):
    t = Tex()
    rnd = random.Random(seed)
    for x in range(16):
        col = rnd.randrange(1, len(bark))
        for y in range(16):
            c = col
            if rnd.random() < 0.18:
                c = max(0, c - 1)
            if (x + seed) % 5 == 0:
                c = 0
            t.set(x, y, bark[c])
    for _ in range(3):
        x, y = rnd.randrange(16), rnd.randrange(14)
        t.set(x, y, bark[0]); t.set(x, y + 1, bark[0])
    return t


def log_top(bark, wood, seed):
    t = Tex()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                t.set(x, y, bark[1 + (x + y) % 2])
            else:
                ring = int(d) % 3
                t.set(x, y, wood[1] if ring == 0 else wood[3] if ring == 1 else wood[2])
    return t


def planks(wood, seed):
    t = Tex()
    rnd = random.Random(seed)
    for board in range(4):
        base = rnd.randrange(1, len(wood) - 1)
        seam = rnd.randrange(3, 13)
        for yy in range(4):
            y = board * 4 + yy
            for x in range(16):
                c = base
                if yy == 3:
                    c = 0
                elif yy == 0:
                    c = min(len(wood) - 1, base + 1)
                elif rnd.random() < 0.15:
                    c = base - 1
                if x == seam and yy < 3:
                    c = 0
                t.set(x, y, wood[c])
    return t


def leaves(pal, seed, blossom=None):
    t = Tex()
    n = fbm(16, 16, seed, (4, 2, 1))
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            v = n[y, x]
            if v < 0.2 and rnd.random() < 0.7:
                continue  # holes let light through
            idx = min(len(pal) - 1, int(v * (len(pal) - 1) + rnd.random() * 0.8))
            t.set(x, y, pal[idx])
    # fluffy highlights on clump tops
    for y in range(1, 16):
        for x in range(16):
            if t.a[y, x, 3] and not t.a[y - 1, x, 3] and rnd.random() < 0.6:
                t.set(x, y, pal[-1])
    if blossom:
        for _ in range(5):
            x, y = rnd.randrange(1, 15), rnd.randrange(1, 15)
            t.set(x, y, blossom); t.set(x + 1, y, lighten(blossom, 0.5))
    return t


def door(wood, bark, seed, top):
    t = M.planks(wood, seed)
    for y in range(16):
        t.set(0, y, bark[1]); t.set(15, y, bark[1])
    if top:
        for y in range(3, 10):
            for x in range(3, 13):
                t.set(x, y, (0, 0, 0, 0) if (x + y) % 5 != 0 else bark[2])
        for x in range(3, 13):
            t.set(x, 2, bark[1]); t.set(x, 10, bark[1])
        for y in range(2, 11):
            t.set(2, y, bark[1]); t.set(13, y, bark[1]); t.set(7, y, bark[2]); t.set(8, y, bark[2])
    else:
        t.set(12, 3, hx('#ffe89a')); t.set(12, 4, hx('#d9a94a'))
        for x in range(1, 15):
            t.set(x, 8, bark[1])
    return t


def trapdoor(wood, bark, seed):
    t = M.planks(wood, seed)
    for i in range(16):
        t.set(i, 0, bark[1]); t.set(i, 15, bark[1]); t.set(0, i, bark[1]); t.set(15, i, bark[1])
    for y in (4, 5, 10, 11):
        for x in (4, 5, 10, 11):
            t.set(x, y, (0, 0, 0, 0))
    return t


def woods():
    for w, bark, wood, leafp, blossom in (('lullwood', LULL_BARK, LULL_WOOD, LULL_LEAF, None),
                                          ('wishwood', WISH_BARK, WISH_WOOD, WISH_LEAF, hx('#ffe89a'))):
        seed = 70 if w == 'lullwood' else 80
        out(f'block/{w}_log', M.log_side(bark, seed))
        out(f'block/{w}_log_top', M.log_top(bark, wood))
        out(f'block/stripped_{w}_log', M.log_side(wood, seed + 1))
        out(f'block/stripped_{w}_log_top', M.log_top(wood, wood))
        out(f'block/{w}_planks', M.planks(wood, seed + 2))
        out(f'block/{w}_leaves', M.leaves(leafp, seed + 3, blossom), LEAVES_META)
        out(f'block/{w}_door_top', door(wood, bark, seed + 4, True), CUTOUT)
        out(f'block/{w}_door_bottom', door(wood, bark, seed + 4, False), CUTOUT)
        out(f'block/{w}_trapdoor', trapdoor(wood, bark, seed + 5), CUTOUT)
        ditem = pal_sprite(S.DOOR_ITEM, {'d': bark[0], 'l': wood[3], 'm': wood[1], 'w': with_alpha(hx('#e8fbff'), 200), 'k': hx('#ffe89a')})
        out(f'item/{w}_door', ditem)
    out('block/lullwood_sapling', sprite(S.LULLWOOD_SAPLING), CUTOUT)
    out('block/wishwood_sapling', sprite(S.WISHWOOD_SAPLING), CUTOUT)
    out('block/hanging_lullwood_leaves', sprite(S.HANGING_LEAVES), CUTOUT)
    out('block/hanging_lullwood_leaves_tip', sprite(S.HANGING_LEAVES_TIP), CUTOUT)


# ================================================================== flora


def flora():
    for name, data in (('lullaby_bell', S.LULLABY_BELL), ('dreambloom', S.DREAMBLOOM), ('soulpetal', S.SOULPETAL), ('nebula_iris', S.NEBULA_IRIS),
                       ('echo_orchid', S.ECHO_ORCHID), ('glowcap', S.GLOWCAP), ('blushgrass', S.BLUSHGRASS), ('coral_fern', S.CORAL_FERN),
                       ('glimmer_sprouts', S.GLIMMER_SPROUTS), ('tall_blushgrass_top', S.TALL_BLUSHGRASS_TOP),
                       ('tall_blushgrass_bottom', S.TALL_BLUSHGRASS_BOTTOM), ('choir_lily_top', S.CHOIR_LILY_TOP),
                       ('choir_lily_bottom', S.CHOIR_LILY_BOTTOM), ('chrome_reeds', S.CHROME_REEDS), ('drift_petals', S.DRIFT_PETALS),
                       ('drift_petals_stem', S.DRIFT_PETALS_STEM), ('dream_snare', S.DREAM_SNARE), ('coral_bush', S.CORAL_BUSH),
                       ('coral_thicket_top', S.CORAL_THICKET_TOP), ('coral_thicket_bottom', S.CORAL_THICKET_BOTTOM)):
        out(f'block/{name}', sprite(data), CUTOUT)
    spent = sprite(S.DREAM_SNARE)
    for y in range(16):
        for x in range(16):
            c = spent.get(x, y)
            if c[3]:
                spent.set(x, y, with_alpha(darken(c, 0.45), 160))
    out('block/dream_snare_spent', spent, CUTOUT)
    out('item/drift_petals', sprite(S.DRIFT_PETALS))
    # glowbell vines
    vine = sprite(S.GLOWBELL_VINE)
    out('block/glowbell_vine', vine, CUTOUT)
    out('block/glowbell_vine_plant', vine, CUTOUT)
    lit = vine.copy()
    draw_map(lit, S.GLOWBELL_VINE_LIT_EXTRA, {k: hx(v) for k, v in S.GLOWBELL_BELL.items()})
    out('block/glowbell_vine_lit', lit, CUTOUT)
    out('block/glowbell_vine_plant_lit', lit, CUTOUT)
    out('item/glowbell_vine', lit)
    # crops
    stem, leaf, leafl = hx('#3f9d80'), hx('#4fb58f'), hx('#78d4a8')
    for i in range(4):
        t = Tex()
        h = 3 + i * 3
        for y in range(16 - h, 16):
            t.set(7, y, stem)
            if (y % 3 == 0):
                t.set(6, y, leaf); t.set(8, y - 1, leafl)
        if i >= 2:
            for dx, dy in ((0, 0), (-1, 1), (1, 1), (0, 1)):
                t.set(7 + dx, 16 - h - 1 + dy, hx('#f3c4ff') if i == 2 else hx('#b760d8'))
        if i == 3:
            t.set(7, 16 - h - 2, hx('#ffd66b'))
        out(f'block/choir_lily_crop_stage{i}', t, CUTOUT)
    for i in range(3):
        t = Tex()
        h = 3 + i * 3
        for y in range(16 - h, 16):
            t.set(7, y, stem)
            if y % 2 == 0:
                t.set(8, y, leaf)
        if i == 2:
            t.set(7, 16 - h - 1, hx('#79b7ff')); t.set(6, 16 - h, hx('#3b7fe0')); t.set(8, 16 - h, hx('#3b7fe0'))
        out(f'block/echo_orchid_crop_stage{i}', t, CUTOUT)
    # pitcher bulb bush: a sweet-berry-style bush; bulbs swell from the third stage
    bush = {'d': hx('#2f7a64'), 'm': hx('#3f9d80'), 'l': hx('#5bbf95'), 'b': hx('#4fb9c4'), 'B': hx('#7fe3e6'), 'W': hx('#d8fbf6'),
            's': hx('#2b6650')}
    stages = [
        ['................'] * 11 + ['.......l........', '......lm.l......', '.....mmlmm......', '......s.s.......', '.......s........'],
        ['................'] * 7 + ['......l..l......', '.....lml.ml.....', '....mmlmmlmm....', '.....dmmmmd.....', '....lmdmmdml....',
                                  '.....dmssmd.....', '......s..s......', '.......ss.......', '.......s........'],
        ['................'] * 4 + ['.....l....l.....', '....lml..lml....', '...mmlmmmmlmm...', '..lmdmBbmmdml...', '...mmdbbmdmm....',
                                  '..lmmdmmmBbml...', '...dmmdmmbbm....', '....dmmssmmd....', '.....dms.smd....', '......s..s......',
                                  '.......ss.......', '.......s........'],
        ['................'] * 2 + ['....l.....l.....', '...lml...lml....', '..mmlmmmmmlmm...', '.lmBbdmmmdmBbl..', '..mbbmWBmmmbbm..',
                                  '.lmmdmbBbmdmml..', '..dmmBbbbmmmd...', '.lmmdmbbmmBbml..', '..dmmmmmdmbbm...', '...dmmdssmmd....',
                                  '....dms..smd....', '.....s....s.....', '......s..s......', '.......ss.......'],
    ]
    for i, rows in enumerate(stages):
        out(f'block/pitcher_bulb_bush_stage{i}', pal_sprite(rows, bush), CUTOUT)


# ================================================================== functional blocks


def functional():
    # the Sift Drum: a 3D drum painted at 32x onto a sheet (tools/hdblocks.py)
    sheet, glow = HB.drum_sheets()
    out('block/sift_drum', sheet)
    out('block/sift_drum_glow', glow, CUTOUT)
    # euphory altar
    at = polished(DREAM, 101)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 4 < d < 5:
                at.set(x, y, hx('#ffd97a'))
            elif d < 1.5:
                at.set(x, y, hx('#ff9fd6'))
    out('block/euphory_altar_top', at)
    side = bricks(DREAM[1:], DREAM_MORTAR, 102, rows=4)
    for x in range(16):
        side.set(x, 0, hx('#ffd97a')); side.set(x, 4, hx('#d9a94a'))
        if x % 3 == 1:
            side.set(x, 8, hx('#7fe8ff'))
    out('block/euphory_altar_side', side)
    out('block/euphory_altar_bottom', stone(DREAM[:4], 103))
    gem = Tex()
    for y in range(16):
        for x in range(16):
            gem.set(x, y, iridescent(x, y, 0.5, 0.6))
    out('block/euphory_altar_gem', gem)
    # portal: 16 animated frames of swirling cyan/pink soul energy
    frames = 16
    p = Image.new('RGBA', (16, 16 * frames))
    px = p.load()
    for f in range(frames):
        t = f / frames * math.tau
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = math.hypot(dx, dy)
                ang = math.atan2(dy, dx)
                v = math.sin(ang * 3 + r * 0.9 - t * 2) * 0.5 + 0.5
                c = lerp(hx('#57d8ff'), hx('#ff8fd0'), v)
                c = lerp(c, hx('#ffffff'), max(0.0, 0.35 - r * 0.04) + (0.25 if math.sin(r * 2 - t * 3) > 0.92 else 0))
                px[x, y + f * 16] = (c[0], c[1], c[2], 190)
    out('block/sift_portal', p, {'animation': {'frametime': 2}, 'texture': {'mipmap_strategy': 'mean'}})
    # harmony stones: each tone has its own crystal colour
    tones = [hx('#7fe8ff'), hx('#ff9fd8'), hx('#ffe07a'), hx('#b9a6ff')]
    for i, c in enumerate(tones):
        h = polished(DREAM, 120 + i)
        notes = [['....cc....', '...c..c...', '...c..c...', '....cc....'],
                 ['...cccc...', '..c....c..', '...cccc...', '..........'],
                 ['.c......c.', '..c....c..', '...c..c...', '....cc....'],
                 ['....c.....', '....cc....', '....c.c...', '..ccc.....']][i]
        draw_map(h, ['..........'] * 3 + notes + ['..........'] * 3, {'c': c}, 3, 3)
        for x in range(4, 12):
            h.set(x, 12, darken(c, 0.3))
        out(f'block/harmony_stone_{i}', h)
    seal = Tex()
    for y in range(16):
        for x in range(16):
            v = (math.sin(x * 0.8) + math.sin(y * 0.8)) * 0.25 + 0.5
            seal.set(x, y, lerp(hx('#1f6f80'), hx('#7fe8ff'), v * 0.6))
            if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                seal.set(x, y, hx('#c8fff6'))
    for i in range(16):
        seal.set(i, 0, HUSH[2]); seal.set(0, i, HUSH[2]); seal.set(i, 15, HUSH[0]); seal.set(15, i, HUSH[0])
    out('block/harmony_seal', seal)
    glyphs = [
        ['..L..', '.L.L.', 'L...L', '.L.L.', '..L..'], ['LLLLL', '..L..', '.L.L.', 'L...L', '.....'], ['L...L', '.L.L.', '..L..', '..L..', '..L..'],
        ['.LLL.', 'L...L', 'L.L.L', 'L...L', '.LLL.'], ['L.L.L', '.L.L.', 'L.L.L', '.L.L.', 'L.L.L'], ['..L..', '..L..', 'LLLLL', '..L..', '..L..'],
        ['L....', 'LL...', 'L.L..', 'L..L.', 'LLLLL'], ['.L.L.', 'L.L.L', '.....', 'L.L.L', '.L.L.']]
    for i, gl in enumerate(glyphs):
        g = polished(DREAM, 130 + i)
        big = []
        for row in gl:
            r2 = ''.join(ch * 2 for ch in row)
            big += [r2, r2]
        draw_map(g, big, {'L': hx('#7fe8ff')}, 3, 3)
        out(f'block/glyph_stone_{i}', g)
    # cake
    top = Tex()
    for y in range(16):
        for x in range(16):
            top.set(x, y, hx('#fff0fa') if (x * 7 + y * 3) % 11 else hx('#ffd6ec'))
    for (x, y) in ((4, 4), (11, 5), (7, 10), (3, 12), (12, 11)):
        top.set(x, y, hx('#7fe3e6')); top.set(x + 1, y, hx('#c2f7f3'))
    out('block/sift_cake_top', top)
    side = Tex()
    for y in range(16):
        for x in range(16):
            if y < 4:
                c = hx('#fff0fa') if not (y == 3 and x % 3 == 0) else hx('#ffd6ec')
            elif y in (8, 9):
                c = hx('#7fe3e6') if x % 4 else hx('#c2f7f3')
            elif y == 15:
                c = hx('#c77fa6')
            else:
                c = hx('#f0a9cb') if (x + y) % 5 else hx('#e28bb5')
            side.set(x, y, c)
    out('block/sift_cake_side', side)
    inner = side.copy()
    for y in range(4, 15):
        for x in range(16):
            if y not in (8, 9):
                inner.set(x, y, hx('#f7c1db') if (x * 3 + y) % 7 else hx('#fff0fa'))
    out('block/sift_cake_inner', inner)
    out('block/sift_cake_bottom', stone(ramp('#b86f97', '#c77fa6', '#d38fb3'), 141))
    # lantern (16x16 layout matching vanilla lantern UVs)
    lan = Tex()
    body_c = [hx('#2c3e66'), hx('#3d5488')]
    for y in range(2, 9):
        for x in range(0, 6):
            lan.set(x, y + 0, lerp(hx('#fff7c2'), hx('#9ff5f0'), (x + y) / 12) if 0 < x < 5 and 2 < y < 8 else body_c[(x + y) % 2])
    for y in range(9, 11):
        for x in range(0, 6):
            lan.set(x, y, body_c[1])
    for x in range(0, 6):
        lan.set(x, 0, body_c[0]); lan.set(x, 1, body_c[1])
    for y in range(10, 16):
        for x in range(11, 14):
            lan.set(x, y, hx('#6a7fb0') if y % 2 else hx('#8ea3d6'))
    for y in range(0, 8):
        for x in range(11, 14):
            lan.set(x, y, hx('#6a7fb0') if y % 2 else hx('#8ea3d6'))
    out('block/bulb_lantern', lan, CUTOUT)
    out('item/bulb_lantern', pal_sprite(S.LANTERN_ITEM, {'d': body_c[0], 'm': body_c[1], 'g': hx('#9ff5f0'), 'G': hx('#fff7c2'),
                                                        'W': hx('#ffffff')}))
    # glowing slime block
    sl = Tex()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            inner_core = 3 <= x <= 12 and 3 <= y <= 12
            if edge:
                c = with_alpha(hx('#6fe8d8'), 220)
            elif inner_core:
                c = with_alpha(lerp(hx('#e8ff9a'), hx('#8ff7c8'), (x + y) / 24), 255)
            else:
                c = with_alpha(hx('#b8fbe6'), 150)
            sl.set(x, y, c)
    sl.set(4, 4, hx('#ffffff')); sl.set(5, 4, hx('#ffffff')); sl.set(4, 5, hx('#ffffff'))
    out('block/glowing_slime_block', sl)
    # soul chime
    out('block/soul_chime_wood', planks(WISH_WOOD, 150))
    tube = Tex()
    for y in range(16):
        for x in range(16):
            tube.set(x, y, SERBIM[3] if x % 2 == 0 else SERBIM[4])
    out('block/soul_chime_tube', tube)
    for lit in (False, True):
        c = Tex()
        for y in range(16):
            for x in range(16):
                c.set(x, y, lerp(hx('#57c6ff'), hx('#ffffff'), 0.6 if lit else 0.1) if (x + y) % 3 else hx('#a6f0ff'))
        out('block/soul_chime_core' + ('_lit' if lit else ''), c)
    out('item/soul_chime', pal_sprite(S.SOUL_CHIME_ITEM, {'d': hx('#6a7fb0'), 'w': WISH_WOOD[1], 't': SERBIM[3], 'c': hx('#57c6ff'),
                                                         'C': hx('#e8fdff')}))
    gp = Tex(16, 16)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        gp.set(x, y, hx('#fff7c2'))
    out('block/glow_particle', gp)
    import siege  # the Thumper's arena: the Ancient Cannon
    siege.block_textures(out)
    __import__('echoer_world').block_textures(out)  # A2 Echoer: The Echoer device, the hut's hearthstone
    # the Chrome fluid's textures are painted by tools/chrome.py (A3 Chrome)


# ================================================================== items


def metal_keys(pal):
    return {'L': pal[-1], 'l': pal[-2], 'm': pal[-3], 'd': pal[0], 'M': pal[-1]}


def tool_keys(mat, handle='#6f4a2e'):
    return {'M': mat[-1], 'm': mat[-3], 'd': mat[0], 'h': hx(handle)}


def items():
    SERB_I = SERBIM[1:]
    out('item/serbim_ingot', pal_sprite(S.INGOT, metal_keys(SERB_I)))
    sift_keys = metal_keys(SIFTITE)
    sift_keys['m'] = SIFTITE_PINK
    out('item/siftite_ingot', pal_sprite(S.INGOT, sift_keys))
    out('item/siftite_nugget', pal_sprite(S.NUGGET, metal_keys(SIFTITE)))
    out('item/raw_serbim', pal_sprite(S.RAW_CHUNK, {'L': SERBIM[4], 'l': SERBIM[2], 'g': SERBIM[5], 'd': SERBIM[0]}))
    pearl = pal_sprite(S.PEARL, {'d': hx('#8c6fb8'), 'm': hx('#d7b8f0'), 'l': hx('#f5e6ff'), 'W': hx('#ffffff')})
    for y in range(16):
        for x in range(16):
            c = pearl.get(x, y)
            if c[3] and c[:3] not in ((255, 255, 255),):
                pearl.set(x, y, lerp(c, iridescent(x, y), 0.35))
    out('item/chrome_pearl', pearl)
    out('item/glowing_slime_ball', pal_sprite(S.SLIME_BALL, {'d': hx('#3fb88f'), 'm': hx('#8ff7c8'), 'l': hx('#e8ff9a'), 'W': hx('#ffffff')}))
    out('item/pitcher_bulb', pal_sprite(S.PITCHER_BULB, {'g': hx('#3f9d80'), 'd': hx('#2f86a0'), 'm': hx('#4fb9c4'), 'l': hx('#7fe3e6'),
                                                        'W': hx('#e9fffb')}))
    out('item/thick_hide', pal_sprite(S.HIDE, {'d': hx('#33485e'), 'm': hx('#5b7f99'), 'l': hx('#86abc2'), 's': hx('#6a90a8')}))
    out('item/star_shard', pal_sprite(S.STAR_SHARD, {'W': hx('#fffbe0'), 'l': hx('#ffe89a'), 'm': hx('#f2c65a'),
                                                       'd': hx('#c48a2c'), 'o': hx('#6e4a16')}))
    out('item/dream_journal_fragment', pal_sprite(S.JOURNAL, {'p': hx('#b89a7a'), 'P': hx('#f0e2c8'), 'i': hx('#6f5fb0')}))
    out('item/dream_stew', pal_sprite(S.STEW, {'S': hx('#c9a6f0'), 'y': hx('#ffe07a'), 'p': hx('#ff9fd8'), 'c': hx('#7fe3e6'), 'b': hx('#8a5a3a'),
                                              'B': hx('#6a4028')}))
    out('item/glowcap_skewer', pal_sprite(S.SKEWER, {'c': hx('#37c9d6'), 'C': hx('#8ff3f0'), 'W': hx('#e8fffb'), 'd': hx('#1f8f9e'),
                                                    's': hx('#b8864f')}))
    out('item/choir_pod', pal_sprite(S.POD, {'s': hx('#3f9d80'), 'g': hx('#8a4fb0'), 'G': hx('#b77fe0'), 'l': hx('#f3c4ff')}))
    out('item/echo_seed', pal_sprite(S.SEED, {'l': hx('#3b7fe0'), 'L': hx('#b8fbff')}))
    out('item/warden_core', pal_sprite(S.WARDEN_CORE, {'d': hx('#062028'), 's': hx('#0f3945'), 'S': hx('#1a5a66'), 'g': hx('#1ec8c8'),
                                                      'G': hx('#5ff5f0'), 'W': hx('#e8fffe')}))
    bucket = pal_sprite(S.BUCKET, {'d': hx('#3a3a44'), 'L': hx('#e8e8f0'), 'l': hx('#b4b4c4'), 'm': hx('#8c8c9c'), 'C': hx('#7fe8ff')})
    for x in range(3, 13):
        for y in (5, 6):
            bucket.set(x, y, iridescent(x, y, 1.0, 0.8))
    out('item/chrome_bucket', bucket)
    out('item/siftite_upgrade_smithing_template', pal_sprite(S.TEMPLATE, {'d': hx('#1c2e3f'), 'S': hx('#3a586c'), 'c': hx('#2e485c'),
                                                                         'W': hx('#9aeefc'), 'p': SIFTITE_PINK}))
    out('item/music_disc_lullaby', pal_sprite(S.DISC, {'d': hx('#101828'), 'm': hx('#26304a'), 'c': hx('#3a4870'), 'p': hx('#7fe8ff'),
                                                      'P': hx('#ff9fd8')}))
    out('item/sift_cake', pal_sprite(S.CAKE_ITEM, {'W': hx('#fff0fa'), 'w': hx('#ffd6ec'), 's': hx('#7fe3e6'), 'p': hx('#f0a9cb'),
                                                  'P': hx('#7fe3e6'), 'g': hx('#fff7c2'), 'd': hx('#c77fa6')}))
    # tools and armour: vanilla silhouettes, shaded by rule (see mcitems.py), with a pink glint
    gear = [hx('#164f66'), SIFTITE[0], SIFTITE[1], SIFTITE[3], SIFTITE[4]]

    def glint(img, n):
        k = 0
        for y in range(16):
            for x in range(16):
                if img.get(x, y)[:3] == gear[4][:3] and k < n:
                    k += 1
                    if k == n:
                        img.set(x, y, SIFTITE_PINK)
        return img

    for t, fn in MI.TOOLS.items():
        out(f'item/siftite_{t}', glint(MI.shade(fn(), gear), 2))
    out('item/siftite_spear_in_hand', glint(MI.shade(MI.spear(), gear), 2))
    for a, rows in MI.ARMOR.items():
        out(f'item/siftite_{a}', glint(MI.shade(MI.mask(rows), gear), 1))
    # slingshot + pulling frames
    base_keys = {'h': hx('#8a6a4a'), 'w': hx('#6fe2dc')}
    out('item/slingshot', pal_sprite(S.SLINGSHOT, base_keys))
    for i in range(3):
        rows = [list(r) for r in S.SLINGSHOT]
        # pull the band back towards the bottom-right as the draw increases
        for y in range(16):
            for x in range(16):
                if rows[y][x] == 'w':
                    rows[y][x] = '.'
        py = 7 + i * 2
        px_ = 6 + i
        for (sx, sy) in ((2, 4), (10, 4)):
            steps = max(abs(px_ - sx), abs(py - sy))
            for s in range(steps + 1):
                xx = round(sx + (px_ - sx) * s / steps)
                yy = round(sy + (py - sy) * s / steps)
                if rows[yy][xx] == '.':
                    rows[yy][xx] = 'w'
        rows[py][px_] = 'g'
        out(f'item/slingshot_pulling_{i}', pal_sprite([''.join(r) for r in rows], dict(base_keys, g=hx('#e8ff9a'))))
    eggs = {'bulb': ('#78a5e3', '#63c6df'), 'slumbler': ('#8fd0dc', '#6d8fd3'), 'sifter': ('#1fa3c1', '#f2cd98'),
            'enchoer': ('#a3dcc5', '#efe2b2'), 'riveter': ('#1d2b47', '#1fa39b'), 'harmoner': ('#e8577f', '#ffd23f'),
            'dictator': ('#141e2c', '#e6e1d3')}
    # modern-style portrait eggs at 32x: each egg wears its mob's face (and ears, antlers, crests)
    E = {
        'bulb': dict(face=['EEEE....EEEE', 'EEEE....EEEE', '.....MM.....', '.....MM.....'],
                     top=['.ee....ee.', 'eiie..eiie', 'eiie..eiie', 'eiie..eiie', 'eiie..eiie', '.ee....ee.'],
                     keys={'E': '#2f2777', 'M': '#4a3a9f', 'e': '#78a5e3', 'i': '#63c6df'}),
        'slumbler': dict(face=['iiii....iiii', 'iEhi....iEhi', 'iEii....iEii', 'iiii....iiii', '............', 'mmmmmmmmmmmm', '.t.t.t.t.t.t'],
                         keys={'i': '#ffd66b', 'E': '#1a1d38', 'h': '#ffffff', 'm': '#d9577f', 't': '#fff8ec'}),
        'sifter': dict(face=['.ee......ee.', '.ee......ee.', '............', 'tTtTtTtTtTtT', 'mmmmmmmmmmmm', 'TtTtTtTtTtTt'],
                       keys={'e': '#d9f6ff', 't': '#eaf7ff', 'T': '#17328c', 'm': '#17328c'}),
        'enchoer': dict(face=['.ffffffff.', 'ffbffffbff', 'feefffeeff', 'ffffnnffff', 'ffffnnffff', 'fffmmmmfff', '.ffffffff.'],
                        top=['a.a......a.a', 'aaa......aaa', '.aaa....aaa.', '..aa....aa..'],
                        keys={'f': '#d5dfd4', 'b': '#4d6870', 'e': '#3a5059', 'n': '#aebdb4', 'm': '#6a807b', 'a': '#efe2b2'}),
        'riveter': dict(face=['BBBBBBBBBB', 'ee..BB..ee', 'eeee..eeee', '.ee....ee.', '..........', '..tvvvvt..'],
                        top=['h........h', '.h......h.', '.hh....hh.'], keys={'B': '#d9d4bf', 'e': '#a6fff5', 't': '#e9f4ef', 'v': '#07101c', 'h': '#1fa39b'}),
        'harmoner': dict(face=['e........e', 'e........e', '...bbbb...', '...bbbb...', '....BB....'],
                         top=['.t..t..t.', '.c..c..c.', '..c.c.c..', '...ccc...'],
                         keys={'e': '#1a1830', 'b': '#ffd23f', 'B': '#c99a1f', 't': '#ff8a3d', 'c': '#ffd86b'}),
        'dictator': dict(face=['.mmmmmmmm.', 'mEEmmmmEEm', 'mEgmmmmgEm', 'mmmmmmmmmm', 'mmmsmmsmmm', 'mmmvvvvmmm', '.mmmmmmmm.'],
                         top=['...g....', 'g..h..g.', 'h..h..h.', 'h.hh.hh.'],
                         keys={'m': '#e6e1d3', 'E': '#04080c', 'g': '#2ef2e2', 's': '#b9b2a0', 'v': '#04080c', 'h': '#141e2c'}),
    }
    for i, (mob, (b, s)) in enumerate(eggs.items()):
        e = E[mob]
        keys = {k: hx(v) for k, v in e['keys'].items()}
        out(f'item/{mob}_spawn_egg', HI.egg(hx(b), hx(s), e.get('face'), keys, e.get('top'), seed=i))


# ================================================================== the Grand Stage and the Encore Sigils


def stage_things():
    teal, teal_d, brass, brass_l = hx('#2ef2e2'), hx('#15a89f'), hx('#b89a52'), hx('#dcc27a')
    violet, violet_l = hx('#7a3ab8'), hx('#c46cff')
    # Encore Sigil: a hushslate floor tile carved with a ring of three notes - drum, flute, strings
    top = M.polished(HUSH[1:6], 170)
    draw_map(top, ['....vvvvvv....', '..vv......vv..', '.v...VVVV...v.', '.v..V....V..v.', 'v..V..bb..V..v', 'v..V.b..b.V..v', 'v..V.b..b.V..v',
                   'v..V..bb..V..v', '.v..V....V..v.', '.v...VVVV...v.', '..vv......vv..', '....vvvvvv....'], {'v': violet, 'V': violet_l, 'b': brass_l}, 1, 2)
    out('block/encore_sigil_top', top)
    side = M.polished(HUSH[1:6], 171)
    for x in range(16):
        side.set(x, 1, brass)
        side.set(x, 14, brass)
        if x % 5 == 2:
            for y in range(4, 12):
                side.set(x, y, violet if y % 2 else violet_l)
    out('block/encore_sigil_side', side)
    # Instrument Altar: a pedestal of polished hushslate with a gold rim and a glowing cradle on top
    top = M.polished(HUSH[1:6], 172)
    draw_map(top, ['bbbbbbbbbbbbbb', 'b............b', 'b..tttttttt..b', 'b..t......t..b', 'b..t.TTTT.t..b', 'b..t.T..T.t..b', 'b..t.T..T.t..b',
                   'b..t.TTTT.t..b', 'b..t......t..b', 'b..tttttttt..b', 'b............b', 'bbbbbbbbbbbbbb'], {'b': brass, 't': teal_d, 'T': teal}, 1, 2)
    out('block/instrument_altar_top', top)
    side = M.polished(HUSH[1:6], 173)
    for x in range(16):
        side.set(x, 0, brass_l)
        side.set(x, 1, brass)
        side.set(x, 15, brass)
        if x in (3, 12):
            for y in range(3, 14):
                side.set(x, y, teal if y % 4 == 0 else teal_d)
    for y in range(5, 11):
        for x in range(6, 10):
            side.set(x, y, teal_d if (x + y) % 2 else teal)
    out('block/instrument_altar_side', side)


# ================================================================== Sculk Corruption overlays
# Painted for client/ClientEffects: tentacles that creep in over your sight, sculk crusting the
# corners of the screen, a closing vignette and veins that pulse with the heartbeat. All in the
# Warden's own palette, from fixed seeds so every build paints them the same.

_SC_HIDE = [hx(h)[:3] for h in ('#06090c', '#0d1217', '#111b21', '#16222a', '#1c2d36')]
_SC_SHEEN = hx('#27434e')[:3]
_SC_TEAL = [hx(h)[:3] for h in ('#052a32', '#034150', '#074857', '#05625d', '#086e68')]
_SC_GLOW = [hx(h)[:3] for h in ('#0f8c99', '#009295', '#29dfeb', '#a8fff8', '#ffffff')]
_SC_BONE = [hx(h)[:3] for h in ('#40576c', '#819988', '#a2af86', '#bbc39b', '#d1d6b6', '#eef0e0')]


def _sc_noise(w, h, cell, rng):
    """Smooth value noise (0..1) on a w x h grid, one random value per cell corner."""
    gw, gh = int(w / cell) + 2, int(h / cell) + 2
    g = rng.random((gh, gw))
    ys, xs = np.mgrid[0:h, 0:w].astype(float)
    xs, ys = (xs + 0.5) / cell, (ys + 0.5) / cell
    x0, y0 = xs.astype(int), ys.astype(int)
    tx, ty = xs - x0, ys - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def _sc_fbm(n, rng, cells, weights):
    return sum(_sc_noise(n, n, c, rng) * w for c, w in zip(cells, weights)) / sum(weights)


def _sc_worley(n, cell, rng):
    """Cellular noise on an n x n grid: distance to the nearest and second-nearest feature point,
    and a random value belonging to the nearest one."""
    g = int(n / cell) + 3
    pts = (np.mgrid[0:g, 0:g].transpose(1, 2, 0)[..., ::-1] + rng.random((g, g, 2))) * cell - cell
    ids = rng.random((g, g))
    Y, X = np.mgrid[0:n, 0:n] + 0.5
    cx, cy = ((X + cell) / cell).astype(int), ((Y + cell) / cell).astype(int)
    ds, iv = [], []
    for oy in (-1, 0, 1):
        for ox in (-1, 0, 1):
            gx, gy = np.clip(cx + ox, 0, g - 1), np.clip(cy + oy, 0, g - 1)
            p = pts[gy, gx]
            ds.append(np.hypot(X - p[..., 0], Y - p[..., 1]))
            iv.append(ids[gy, gx])
    ds, iv = np.array(ds), np.array(iv)
    order = np.argsort(ds, axis=0)
    return (np.take_along_axis(ds, order[:1], 0)[0], np.take_along_axis(ds, order[1:2], 0)[0],
            np.take_along_axis(iv, order[:1], 0)[0])


def _sc_ring(mask):
    """The pixels touching a mask edge-on."""
    out = np.zeros_like(mask)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out |= np.roll(np.roll(mask, dy, 0), dx, 1)
    return out & ~mask


def _sc_image(rgb, alpha):
    return Image.fromarray(np.clip(np.dstack([rgb, alpha * 255]) + 0.5, 0, 255).astype(np.uint8), 'RGBA')


# ---------------------------------------------------------------- the tentacles

_SC_R_ROOT, _SC_R_16, _SC_R_TIP = 7.85, 2.05, 0.6
# how each one curls in its top slice: (straight on for, then curling over, to curvature)
_SC_CURLS = {'barbed': (7, 8, 0.6), 'suckers': (7, 8, -0.7), 'veined': (7, 8, -0.6), 'hook': (6, 14, 1.15)}


def _sc_radius(y):
    """A tentacle's radius on the straight: full width at the root, tapering to 2 at y = 16."""
    u = max(0.0, (y - 16.0) / 112.0)
    return _SC_R_16 + (_SC_R_ROOT - _SC_R_16) * u ** 0.85


def _sc_centreline(ls, lc, kmax):
    """Dense samples (x, y, tangent x, tangent y, arc length, radius) up the middle of the strip
    from the root at (8, 128) to (8, 16), then on straight for ls and curling over lc."""
    pts = [(8.0, y, 0.0, -1.0, 128.0 - y, _sc_radius(y)) for y in np.arange(128.0, 16.0, -0.25)]
    x, y, th = 8.0, 16.0, -math.pi / 2
    ds, total = 0.2, ls + lc
    for i in range(int(total / ds) + 1):
        s = i * ds
        pts.append((x, y, math.cos(th), math.sin(th), 112.0 + s, _SC_R_16 + (_SC_R_TIP - _SC_R_16) * (s / total) ** 0.9))
        th += (0.0 if s < ls else kmax * (s - ls) / lc) * ds
        x += math.cos(th) * ds
        y += math.sin(th) * ds
    return np.array(pts)


def _sc_nearest(px, py, cl):
    """For each point, the distance outside the swept body (negative inside) and the nearest sample."""
    e, idx = np.empty(px.size), np.empty(px.size, int)
    for a in range(0, px.size, 2048):
        d = np.hypot(px[a:a + 2048, None] - cl[None, :, 0], py[a:a + 2048, None] - cl[None, :, 1]) - cl[None, :, 5]
        i = d.argmin(1)
        e[a:a + 2048], idx[a:a + 2048] = d[np.arange(i.size), i], i
    return e, idx


def _sc_tentacle(kind, seed):
    """One 16 x 128 tentacle, root at the bottom. Its body is a dark cylinder of Warden hide lit from
    the strip's left; every slice boundary (y = 16k) is kept clear of detail and the profile is
    continuous, so the slices chain together however they are bent."""
    W, H, SS = 16, 128, 6
    HIDE, TEAL, GLOW, BONE = _SC_HIDE, _SC_TEAL, _SC_GLOW, _SC_BONE
    rng = np.random.default_rng(seed)
    rnd = random.Random(seed)
    cl = _sc_centreline(*_SC_CURLS[kind])
    s_end = cl[-1, 4]
    sy, sx = np.mgrid[0:H * SS, 0:W * SS].astype(float)
    e_ss, _ = _sc_nearest(((sx + 0.5) / SS).ravel(), ((sy + 0.5) / SS).ravel(), cl)
    cov = (e_ss < 0).reshape(H, SS, W, SS).mean(axis=(1, 3))   # anti-aliased coverage
    py, px = np.mgrid[0:H, 0:W].astype(float) + 0.5
    e, idx = _sc_nearest(px.ravel(), py.ravel(), cl)
    e, idx = e.reshape(H, W), idx.reshape(H, W)
    P = cl[idx]
    r, s = P[..., 5], P[..., 4]
    v = np.clip((P[..., 2] * (py - P[..., 1]) - P[..., 3] * (px - P[..., 0])) / r, -1, 1)  # -1 left .. 1 right
    # the hide, with soft rings bowing toward the tip (faded out at every joint), a wet sheen on
    # the lit side and a teal rim of reflected glow on the shadowed one
    tone = np.select([v < -0.86, v < -0.30, v < 0.12, v < 0.50, v < 0.84], [2, 4, 3, 2, 1], 0)
    phase = py / np.clip(0.6 * r, 2.4, 4.4) + 0.4 * (1 - np.sqrt(np.clip(1 - v * v, 0, 1)))
    joint = np.minimum(py % 16, 16 - py % 16)
    rings = (np.mod(phase, 1.0) < 0.22) & (joint > 1.6) & (s < 111)
    tone = np.where(rings, np.maximum(tone - 1, 0), tone)
    img = np.array(HIDE, float)[tone]
    sheen = (v > -0.68) & (v < -0.46) & (_sc_noise(W, H, 2.5, rng) > 0.42) & (r > 2.2) & ~rings
    img[sheen] = _SC_SHEEN
    rim = v >= 0.84
    img[rim] = np.array(TEAL, float)[np.where(v[rim] > 0.94, 2, 1)]

    def put(x, y, c):
        xi, yi = int(math.floor(x)), int(math.floor(y))
        if 0 <= xi < W and 0 <= yi < H and cov[yi, xi] > 0.3:
            img[yi, xi] = c

    def inside(xi, yi, margin):
        return 0 <= xi < W and 0 <= yi < H and e[yi, xi] < -margin

    def spot(xc, yc, rho, glow):
        """A Warden spot: a teal patch foreshortened round the body, some with a glowing heart."""
        vc = (xc - 8) / _sc_radius(yc)
        rx, ry = rho * max(0.5, math.sqrt(max(0.0, 1 - vc * vc))), rho * 1.2
        for yi in range(int(yc - ry) - 1, int(yc + ry) + 2):
            for xi in range(int(xc - rx) - 1, int(xc + rx) + 2):
                if not inside(xi, yi, 0.3):
                    continue
                dx, dy = xi + 0.5 - xc, yi + 0.5 - yc
                d = math.hypot(dx / rx, dy / ry)
                if d < 1:
                    vv = v[yi, xi]
                    c = TEAL[2] if vv < -0.25 else TEAL[0] if vv > 0.5 else TEAL[1]
                    if d > 0.55:
                        c = TEAL[3] if dx + dy < 0 and vv < 0.35 else TEAL[0] if dx + dy > 0 else c
                    img[yi, xi] = c
        if glow:
            put(xc, yc, GLOW[3] if rho > 2.0 else GLOW[2])
            if rho > 2.0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    put(xc + dx, yc + dy, GLOW[1])

    def sucker(xc, yc, rho):
        """A raised lip round a dark cup with a glowing heart."""
        vc = (xc - 8) / _sc_radius(yc)
        rx, ry = rho * max(0.6, math.sqrt(max(0.0, 1 - vc * vc))), rho
        if rho < 1.05:
            put(xc, yc, GLOW[2])
            return
        for yi in range(int(yc - ry) - 1, int(yc + ry) + 2):
            for xi in range(int(xc - rx) - 1, int(xc + rx) + 2):
                if not inside(xi, yi, 0.2):
                    continue
                dx, dy = xi + 0.5 - xc, yi + 0.5 - yc
                d = math.hypot(dx / rx, dy / ry)
                if d < 0.34:
                    img[yi, xi] = GLOW[3] if rho > 2.4 and d < 0.17 else GLOW[2]
                elif d < 0.62:
                    img[yi, xi] = GLOW[0] if rho > 2.4 and d < 0.48 else HIDE[0]
                elif d < 1.02:
                    img[yi, xi] = TEAL[4] if dx + dy * 1.2 < 0 else TEAL[1]

    for k in range(7):  # spots stay inside their slice, clear of the joints
        for j in range({'barbed': 2, 'suckers': 1, 'veined': 1, 'hook': 3}[kind]):
            y = 128 - 16 * k - rnd.uniform(3.5, 12.5)
            rr = _sc_radius(y)
            spot(8 + rnd.uniform(-0.55, 0.45) * rr, y, max(1.0, rnd.uniform(0.24, 0.36) * rr), rnd.random() < 0.6)
    for k in range(7):  # sculk specks: single glowing pixels, as on the sculk block
        for j in range(3):
            y = 128 - 16 * k - rnd.uniform(2.0, 14.0)
            put(8 + rnd.uniform(-0.75, 0.7) * _sc_radius(y), y, GLOW[1] if rnd.random() < 0.7 else GLOW[2])
    if kind == 'suckers':  # two rows of suckers, big ones down the underside
        for k in range(7):
            for off, vv, size in ((4.5, 0.42, 0.42), (11.5, 0.42, 0.42), (8.0, -0.4, 0.27)):
                y = 128 - 16 * k - off
                rr = _sc_radius(y)
                sucker(8 + vv * rr, y, max(0.8, size * rr))
        for y in (13.0, 10.0):
            put(8.6, y, GLOW[2])
    if kind == 'veined':  # a glowing vein meandering up the middle, branching out to the edges
        y = 127.5
        while y > 9:
            rr = _sc_radius(y) if y > 16 else _SC_R_16
            x = 8 + rr * 0.2 * math.sin(y / 7.3 + 1.0)
            put(x, y, GLOW[2] if rr > 2.6 or int(y) % 2 else GLOW[1])
            if rr > 4.2:
                for side in (-1, 1):
                    if inside(int(x) + side, int(y), 0.8):
                        img[int(y), int(x) + side] = TEAL[3] if side < 0 else TEAL[1]
            if int(y) % 16 == 8 and int(y) > 20:
                side = 1 if (int(y) // 16) % 2 else -1
                bx, by = x, y
                for t in range(12):
                    bx += side * 0.72
                    by -= 0.9
                    if abs(bx - 8) > _sc_radius(by) - 1.1:
                        put(bx - side * 0.7, by + 0.9, GLOW[3])
                        break
                    put(bx, by, GLOW[2] if t < 3 else GLOW[1])
            y -= 1.0
    if kind == 'barbed':  # bone thorns along both edges, hooked toward the tip
        thorns = [(side, 128 - 16 * k - off) for k in range(2, 7) for side, off in ((-1, 5.0), (1, 11.0))]
        thorns.append((1, 21.5))
        big = [(0, 1, 0), (0, 0, 2), (1, 0, 2), (0, -1, 2), (1, -1, 3), (1, -2, 3), (2, -2, 4), (2, -3, 4), (3, -4, 5)]
        small = [(0, 1, 0), (0, 0, 2), (0, -1, 2), (1, -1, 3), (1, -2, 4), (2, -3, 5)]
        tiny = [(0, 1, 0), (0, 0, 2), (0, -1, 4), (1, -2, 5)]
        for side, yb in thorns:
            room = 8 - _sc_radius(yb)
            xe = int(math.floor(8 + side * (_sc_radius(yb) - 0.55)))
            for dx, dy, t in (big if room >= 3.5 else small if room >= 2.4 else tiny):
                x, y = xe + dx * side, int(yb) + dy
                if 0 <= x < W and 0 <= y < H:
                    img[y, x] = HIDE[0] if t == 0 else BONE[t]
                    if t:
                        cov[y, x] = 1.0
        for k in range(1, 6):  # and little bone spines down the lit face
            y = 128 - 16 * k - 8
            x = int(8 - 0.25 * _sc_radius(y))
            img[y, x], img[y - 1, x], img[y + 1, x] = BONE[3], BONE[5], HIDE[0]
    if kind == 'hook':  # the hook ends in a bone claw behind a band of glow
        claw = s > s_end - 4.6
        img[claw] = np.array(BONE, float)[np.clip(np.where(v[claw] < 0.1, 4, 2) + (s[claw] > s_end - 1.6), 1, 5)]
        img[(s > s_end - 6.6) & ~claw] = GLOW[2]
        glow_from = s_end - 6.6
    else:  # the others glow brighter to the very tip
        tip = s > s_end - 3.4
        img[tip] = np.array(GLOW, float)[np.clip(((s[tip] - (s_end - 3.4)) / 3.4 * 3).astype(int) + 1, 1, 3)]
        glow_from = s_end - 3.4
    # the body over a soft halo of glow round its tip
    halo = np.where((e > 0) & (e < 1.9) & (s > glow_from - 1.5), (1 - e / 1.9) * 0.5, 0.0)
    alpha = cov + halo * (1 - cov)
    rgb = img * cov[..., None] + np.array(GLOW[2], float) * (halo * (1 - cov))[..., None]
    return _sc_image(np.where(alpha[..., None] > 0, rgb / np.maximum(alpha[..., None], 1e-6), 0), alpha)


# ---------------------------------------------------------------- the crust

def _sc_crust(seed):
    """256 x 256: sculk crusted over the bottom-left corner - lumpy cells, half-buried ribs, a
    catalyst's fangs, a shrieker's mouth, glowing veins and sensor sprouts - its ragged edge
    fraying into a lace of sculk vein that fades to nothing toward the top right."""
    N = 256
    HIDE, TEAL, GLOW, BONE = _SC_HIDE, _SC_TEAL, _SC_GLOW, _SC_BONE
    rng = np.random.default_rng(seed)
    rnd = random.Random(seed)
    Y, X = np.mgrid[0:N, 0:N] + 0.5
    qx, qy = X / N, (N - Y) / N
    rho, phi = np.hypot(qx, qy), np.arctan2(qy, qx)
    # where it has grown: thickest in the corner, furthest along the two walls, a few lobes reaching out
    lo = _sc_fbm(N, rng, (64, 32), (0.6, 0.4))
    hi = _sc_fbm(N, rng, (10, 5, 2.5), (0.5, 0.3, 0.2))
    R = 0.40 + 0.03 * np.sin(5 * phi + 0.7) + 0.18 * (lo - 0.5)
    R += 0.16 * (np.exp(-(phi / 0.22) ** 2) + np.exp(-((math.pi / 2 - phi) / 0.22) ** 2))
    for k in range(4):
        pk = rnd.uniform(0.3, 1.27)
        R += rnd.uniform(0.07, 0.12) * np.exp(-((phi - pk) / rnd.uniform(0.06, 0.1)) ** 2)
    F = R - rho + 0.05 * (hi - 0.5)     # > 0 inside the crust
    depth = np.clip(F / 0.14, 0, 1)     # 0 at the edge .. 1 deep in the corner
    spores = (F > -0.03) & (F <= 0) & (hi > 0.67)
    alpha = np.maximum(np.clip(F / 0.008, 0, 1), spores.astype(float))
    body = alpha > 0.5
    # lumpy sculk cells, big and small, lit from the top left
    f1b, _, idb = _sc_worley(N, 24, rng)
    f1s, f2s, ids = _sc_worley(N, 8, rng)
    h = 0.55 * (1 - np.clip(f1b / 20, 0, 1)) ** 1.5 + 0.35 * (1 - np.clip(f1s / 7, 0, 1)) + 0.1 * hi + 0.25 * depth
    gy, gx = np.gradient(h)
    shade = np.select([(gx + gy) * 6.5 < -0.16, (gx + gy) * 6.5 < 0.08, (gx + gy) * 6.5 < 0.3], [0, 1, 2], 3)
    teal = ((idb < 0.36) | ((ids < 0.22) & (idb < 0.62))) & (depth > 0.08)
    img = np.array(HIDE, float)[shade + 1]
    img[teal] = np.array(TEAL, float)[shade[teal]]
    crev = (f2s - f1s) < 1.1
    img[crev] = HIDE[0]
    thin = (depth < 0.12) & ~crev   # the thin edge of the growth, only just spreading
    img[thin] = np.where(teal[thin, None], np.array(TEAL[0], float), np.array(HIDE[1], float))
    img[spores] = TEAL[1]
    # past the edge, a lace of sculk vein over a faint dark stain, fading out
    fringe = (F <= 0) & (F > -0.11) & ~spores
    fade = np.clip(1 + F / 0.11, 0, 1) ** 1.4
    lace = fringe & ((f2s - f1s) < 1.25) & (lo > 0.3)
    img[lace] = np.where((hi[lace] > 0.55)[:, None], np.array(TEAL[2], float), np.array(TEAL[1], float))
    alpha[lace] = 0.9 * fade[lace]
    stain = fringe & ~lace
    img[stain] = HIDE[1]
    alpha[stain] = 0.25 * fade[stain]
    # bones half buried in it: broken ribs, clusters of fangs, chips, and a shrieker's mouth
    bone = np.zeros((N, N), int)
    throat = np.zeros((N, N), bool)

    def rib(cx, cy, r0, a0, a1, thick, cut_seed):
        cuts = [random.Random(cut_seed).uniform(a0, a1) for _ in range(3)]
        n = int(abs(a1 - a0) * r0 * 1.6)
        for i in range(n):
            a = a0 + (a1 - a0) * i / n
            if any(abs(a - c) < 0.035 for c in cuts):
                continue  # sunk under the crust here
            th = thick * (0.75 + 0.25 * math.sin(math.pi * i / n))
            for t in np.linspace(-th / 2, th / 2, int(th * 2) + 1):
                xi, yi = int(cx + math.cos(a) * (r0 + t)), int(cy - math.sin(a) * (r0 + t))
                if 0 <= xi < N and 0 <= yi < N and depth[yi, xi] > 0.3:
                    bone[yi, xi] = 5 if t > th * 0.28 else 4 if t > -th * 0.05 else 3 if t > -th * 0.32 else 2
    rib(-18, N + 26, 78, 0.42, 1.08, 3.6, 1)
    rib(30, N + 40, 70, 0.55, 1.25, 3.0, 2)
    rib(-40, N + 10, 96, 0.2, 0.62, 3.2, 3)
    clusters = []
    while len(clusters) < 4:  # a catalyst's fangs breaking the surface
        bx, by = rnd.randint(20, 170), rnd.randint(110, 246)
        if 0.3 < depth[by, bx] < 0.75 and depth[by, bx + 12] > 0.3 and all(abs(bx - c[0]) + abs(by - c[1]) > 40 for c in clusters):
            clusters.append((bx, by, rnd.randint(3, 4)))
    for (bx, by, n) in clusters:
        for j in range(n):
            fx, fy, fh = bx + j * 4 + rnd.randint(-1, 1), by + rnd.randint(-1, 1), rnd.randint(4, 7)
            for dy in range(fh):
                w = max(0, int((fh - dy) * 0.45))
                for dx in range(-w, w + 1):
                    bone[fy - dy, fx + dx] = 5 if dx < 0 and dy > 1 else 4 if dx <= 0 else 2
            bone[fy + 1, fx - 1:fx + 2] = 1
    for _ in range(9):  # chips
        xi, yi = int(rnd.uniform(6, 150)), int(N - rnd.uniform(6, 150))
        if depth[yi, xi] > 0.3:
            bone[yi, xi], bone[yi, xi + 1], bone[yi + 1, xi] = 5, 3, 2
    for _ in range(400):  # the shrieker: a ring of bone teeth round a dark throat, a glow deep inside
        sx, sy = rnd.randint(30, 120), rnd.randint(150, 230)
        if depth[sy, sx] > 0.55 and all(abs(sx - c[0]) + abs(sy - c[1]) > 26 for c in clusters):
            for yi in range(sy - 13, sy + 14):
                for xi in range(sx - 13, sx + 14):
                    d = math.hypot(xi + 0.5 - sx, (yi + 0.5 - sy) * 1.3)
                    ang = math.atan2(sy - yi - 0.5, xi + 0.5 - sx)
                    teeth = abs(math.sin(3.5 * ang + 0.4)) ** 3 * (0.8 + 0.4 * math.sin(7 * ang))
                    if d < 4.6 - 1.4 * teeth:
                        img[yi, xi] = (GLOW[2] if d < 1.3 and yi >= sy else GLOW[1] if d < 2.3 and yi >= sy
                                       else GLOW[0] if d < 3.0 else HIDE[0])
                        throat[yi, xi] = True
                    elif d < 6.4 + 3.6 * teeth:
                        lit = math.cos(ang - 2.4)
                        bone[yi, xi] = 1 if d < 5.2 - 1.4 * teeth else 5 if lit > 0.55 else 4 if lit > 0 else 3 if lit > -0.5 else 2
            break
    bm = bone > 0
    img[bm] = np.array(BONE, float)[bone[bm]]
    img[_sc_ring(bm) & body & ~throat] = HIDE[0]   # a dark rim sinks every bone into the crust
    # glowing veins winding out from the walls and the corner, forking, thinning past the edge
    core = np.zeros((N, N))
    knots = []

    def vein(x, y, a, width, life, forks):
        w = 0.0
        while life > 0:
            w = max(-0.045, min(0.045, w + rnd.uniform(-0.02, 0.02)))
            a += w + (math.atan2(N - y, x) - a) * 0.03
            x += math.cos(a) * 0.8
            y -= math.sin(a) * 0.8
            if not (1 <= x < N - 2 and 1 <= y < N - 1):
                return
            xi, yi = int(x), int(y)
            core[yi, xi] = max(core[yi, xi], width)
            if width >= 1.5:
                core[yi, xi + 1] = max(core[yi, xi + 1], 1.2)
            life -= 1 if F[yi, xi] > 0 else 2.2
            if life < 120:
                width = max(1.0, width - 0.01)
            if forks > 0 and rnd.random() < 0.014:
                forks -= 1
                knots.append((xi, yi))
                vein(x, y, a + rnd.choice((-1, 1)) * rnd.uniform(0.45, 0.9), max(1.0, width - 0.7), life * 0.55, forks - 1)
    starts = [(rnd.uniform(1, 4), N - rnd.uniform(20, 90), rnd.uniform(0.3, 0.9)) for _ in range(2)]
    starts += [(rnd.uniform(20, 90), N - rnd.uniform(1, 4), rnd.uniform(0.7, 1.3)) for _ in range(2)]
    starts += [(rnd.uniform(2, 8), N - rnd.uniform(2, 8), rnd.uniform(0.6, 0.95))]
    for (x0, y0, a0) in starts:
        vein(x0, y0, a0, 2.0, rnd.uniform(150, 210), 3)
    lit = (core > 0) & body & ~bm & ~throat
    img[lit] = np.where((core[lit] >= 1.5)[:, None], np.array(GLOW[2], float), np.array(GLOW[1], float))
    ring = _sc_ring((core >= 1.5)) & body & ~bm & ~throat
    img[ring] = np.where((hi[ring] > 0.45)[:, None], np.array(GLOW[0], float), np.array(TEAL[3], float))
    for (xi, yi) in knots:  # glowing knots where they fork
        if body[yi, xi]:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                img[yi + dy, xi + dx] = GLOW[2]
            img[yi, xi] = GLOW[3]
    past = (core > 0) & ~body
    alpha[past] = np.maximum(alpha[past], 0.95 * np.clip(1 + F[past] / 0.2, 0, 1))
    img[past] = TEAL[2]
    # sculk-sensor sprouts along the growing edge: curling stalks with glowing tips
    halo = np.zeros((N, N))
    sprouts = tries = 0
    while sprouts < 14 and tries < 5000:
        tries += 1
        x, y = rnd.uniform(4, 210), N - rnd.uniform(4, 210)
        if not (0.015 < F[int(y), int(x)] < 0.045) or bm[int(y), int(x)]:
            continue
        sprouts += 1
        a = math.atan2(N - y, x) * 0.5 + math.pi / 4 + rnd.uniform(-0.3, 0.3)
        bend = rnd.uniform(-0.06, 0.06)
        steps = int(rnd.uniform(9, 20) * 1.5)
        xi, yi = int(x), int(y)
        for i in range(steps):
            t = i / steps
            a += bend
            x += math.cos(a) * 0.67
            y -= math.sin(a) * 0.67
            if not (0 <= int(x) < N - 1 and 0 <= int(y) < N):
                break
            xi, yi = int(x), int(y)
            img[yi, xi] = GLOW[3] if t > 0.92 else GLOW[2] if t > 0.76 else TEAL[4] if i % 3 else GLOW[1]
            alpha[yi, xi] = 1.0
            if t < 0.5:  # thicker at the base, its shadow side darker
                img[yi, xi + 1] = TEAL[1]
                alpha[yi, xi + 1] = 1.0
        for dy in range(-4, 5):
            for dx in range(-4, 5):
                d = math.hypot(dx, dy)
                if 0 < d < 4.0 and 0 <= yi + dy < N and 0 <= xi + dx < N:
                    halo[yi + dy, xi + dx] = max(halo[yi + dy, xi + dx], (1 - d / 4.0) ** 1.5 * 0.7)
    # specks: single glowing pixels, thickest at the growing edge
    sp = rng.random((N, N))
    speck = body & (sp < 0.004 + 0.016 * (1 - depth)) & ~crev & ~bm & ~throat
    img[speck] = np.where((sp[speck] < 0.003)[:, None], np.array(GLOW[3], float), np.array(GLOW[2], float))
    # all of it over the soft glow round the sprout tips
    ha = halo * (1 - alpha)
    A = np.clip(alpha + ha, 0, 1)
    rgb = (img * alpha[..., None] + np.array(GLOW[2], float) * ha[..., None]) / np.maximum(A, 1e-6)[..., None]
    return _sc_image(np.where(A[..., None] > 0, rgb, 0), A)


# ---------------------------------------------------------------- the vignette and the veins

def _sc_grow_veins(rnd, n, count, inner, branch=0.03, wobble=0.03, pull=0.03):
    """Veins growing in from the border of an n x n square toward its middle, each forking a few
    times into thinner veins. Returns per-pixel strength (about 1 for a trunk, less for each fork)
    and the fork points. inner: the range of radius (0 middle .. 1 border) at which a trunk gives out."""
    core = np.zeros((n, n))
    thick = np.zeros((n, n), bool)
    forks = []

    def walk(x, y, a, strength, stop, depth, budget):
        w = 0.0
        while True:
            dx, dy = x / n * 2 - 1, y / n * 2 - 1
            r = math.hypot(dx, dy)
            if r < stop:
                return
            w = max(-wobble, min(wobble, w + rnd.uniform(-wobble / 3, wobble / 3)))
            to_mid = math.atan2(-dy, -dx)
            a += w + math.atan2(math.sin(to_mid - a), math.cos(to_mid - a)) * pull
            x += math.cos(a) * 0.8
            y += math.sin(a) * 0.8
            if not (0 <= x < n and 0 <= y < n):
                return
            xi, yi = int(x), int(y)
            core[yi, xi] = max(core[yi, xi], strength)
            if depth == 0 and r > 0.78:
                thick[yi, xi] = True
            if budget > 0 and rnd.random() < branch:
                budget -= 1
                forks.append((xi, yi, strength))
                walk(x, y, a + rnd.choice((-1, 1)) * rnd.uniform(0.45, 0.85), strength * 0.72,
                     min(1.0, stop + rnd.uniform(0.05, 0.16)), depth + 1, 1 if depth == 0 else 0)

    for i in range(count):
        side = i % 4
        t = (i // 4 + rnd.uniform(0.15, 0.85)) / math.ceil(count / 4) * n
        x, y = [(t, 0.0), (n - 0.01, t), (n - t, n - 0.01), (0.0, n - t)][side]
        a = [math.pi / 2, math.pi, -math.pi / 2, 0.0][side] + rnd.uniform(-0.45, 0.45)
        walk(x, y, a, rnd.uniform(0.8, 1.0), rnd.uniform(*inner), 0, 4)
    widen = (np.roll(thick, 1, 1) | np.roll(thick, 1, 0)) & (core == 0)   # trunks two pixels wide at the border
    return np.maximum(core, np.where(widen, 0.85, 0)), forks


def _sc_vignette(seed):
    """256 x 256: clear in the middle, closing in to a deep sculk black, faint veins in the dark."""
    N = 256
    rnd = random.Random(seed)
    Y, X = np.mgrid[0:N, 0:N] + 0.5
    r = np.hypot(X / N * 2 - 1, Y / N * 2 - 1)
    k = np.clip((r - 0.5) / 0.68, 0, 1)
    alpha = (k * k * (3 - 2 * k)) ** 1.15 * 0.94
    near, far = np.array(hx('#04161c')[:3], float), np.array(hx('#010507')[:3], float)
    rgb = near + (far - near) * np.clip((r - 0.55) / 0.6, 0, 1)[..., None]
    core, forks = _sc_grow_veins(rnd, N, 20, (0.6, 0.82), branch=0.02)
    soft = np.asarray(Image.fromarray(((core > 0) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.0)), float) / 255
    m = np.clip(soft * 1.5, 0, 1) * np.clip((r - 0.55) / 0.3, 0, 1)
    rgb += (np.array(hx('#0a3a44')[:3], float) - rgb) * (m * 0.8)[..., None]
    alpha = np.maximum(alpha, m * 0.55)
    for (xi, yi, s) in forks:  # the odd glint of cyan where they fork
        if math.hypot(xi / N * 2 - 1, yi / N * 2 - 1) > 0.66 and rnd.random() < 0.6:
            rgb[yi, xi] += (np.array(_SC_GLOW[1], float) - rgb[yi, xi]) * 0.6
    return _sc_image(rgb, alpha)


def _sc_veins(seed):
    """256 x 256: thin glowing veins branching in from every edge, densest at the border and
    giving out before the middle; the overlay pulses them with the heartbeat."""
    N = 256
    GLOW = _SC_GLOW
    rnd = random.Random(seed)
    core, forks = _sc_grow_veins(rnd, N, 24, (0.3, 0.56))
    Y, X = np.mgrid[0:N, 0:N] + 0.5
    edge = np.clip((np.hypot(X / N * 2 - 1, Y / N * 2 - 1) - 0.28) / 0.42, 0, 1) ** 0.9
    for (xi, yi, s) in forks:  # little knots where they fork
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if 0 <= xi + dx < N and 0 <= yi + dy < N:
                core[yi + dy, xi + dx] = max(core[yi + dy, xi + dx], s * 0.85)
    v = core > 0
    soft = np.asarray(Image.fromarray((np.clip(core, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.4)), float) / 255
    alpha = np.clip(soft * 1.4, 0, 1) * 0.45 * edge   # a soft glow round every vein
    rgb = np.zeros((N, N, 3))
    rgb[:] = GLOW[1]
    rgb[v] = np.where((core[v] > 0.9)[:, None], np.array(GLOW[3], float), np.array(GLOW[2], float))
    alpha[v] = np.maximum(alpha[v], (0.5 + 0.5 * core[v]) * edge[v])
    for (xi, yi, s) in forks:
        rgb[yi, xi] = GLOW[3]
    return _sc_image(rgb, alpha)


def corruption_overlays():
    """The Sculk Corruption overlays (textures/misc): four tentacles in one 64 x 128 strip (each
    16 x 128, root at the bottom, eight 16 x 16 slices that chain and bend), the corner crust,
    the vignette and the pulsing veins."""
    strip = Image.new('RGBA', (64, 128), (0, 0, 0, 0))
    for i, kind in enumerate(('barbed', 'suckers', 'veined', 'hook')):
        strip.alpha_composite(_sc_tentacle(kind, 40 + i), (16 * i, 0))
    out('misc/sculk_tentacle', strip)
    crust = _sc_crust(11)
    out('misc/sculk_crust', crust)
    # the other three corners, mirrored here (a mirrored blit could be culled)
    out('misc/sculk_crust_br', crust.transpose(Image.FLIP_LEFT_RIGHT))
    out('misc/sculk_crust_tl', crust.transpose(Image.FLIP_TOP_BOTTOM))
    out('misc/sculk_crust_tr', crust.transpose(Image.ROTATE_180))
    out('misc/sculk_vignette', _sc_vignette(21))
    out('misc/sculk_veins', _sc_veins(31))


# ================================================================== the Dictator's things


def dictator_things():
    teal, teal_d, brass, brass_l = hx('#2ef2e2'), hx('#15a89f'), hx('#b89a52'), hx('#dcc27a')
    # the podium: dark hushslate with a brass rail and glowing grooves; the top is a music stand of runes
    side = M.polished(HUSH[1:6], 160)
    for x in range(16):
        side.set(x, 2, brass); side.set(x, 1, brass_l)
        if x % 4 == 1:
            for y in range(5, 13):
                side.set(x, y, teal_d if y % 3 else teal)
    out('block/conductors_podium_side', side)
    top = M.polished(HUSH[1:6], 161)
    draw_map(top, ['..........', '.bbbbbbbb.', '.b......b.', '.b.tttt.b.', '.b.t..t.b.', '.b.tttt.b.', '.b..tt..b.', '.b......b.', '.bbbbbbbb.',
                   '..........'], {'b': brass, 't': teal}, 3, 3)
    out('block/conductors_podium_top', top)
    # the Conductor's Baton: a long white baton, a dark grip with a brass ferrule, a glowing sculk tip
    out('item/conductors_baton', pal_sprite([
        '..............gG',
        '.............wgg',
        '............ww..',
        '...........wl...',
        '..........wl....',
        '.........wl.....',
        '........wl......',
        '.......wl.......',
        '......wl........',
        '.....bB.........',
        '....dd..........',
        '...dD...........',
        '..dD............',
        '.dD.............',
        'dd..............',
        '................',
    ], {'w': hx('#f4f0e5'), 'l': hx('#c9c2b0'), 'g': teal_d, 'G': teal, 'b': brass, 'B': brass_l, 'd': hx('#141e2c'), 'D': hx('#2b3a52')}))


# ================================================================== the Sift Codex


def codex():
    """The Sift Codex: a two-page spread in a teal leather binding with brass corners (292x180),
    category ribbons, page-turn arrows and the book's item icon. Laid out in one 512x256 sheet:
      book spread at (0, 0) 292x180, tabs at (0, 184) 5 x 24x12 (selected row at y=196),
      arrows at (128, 184): next 18x10, previous 18x10 (hover row at y=194)."""
    W, Hh = 512, 256
    t = Tex(W, Hh)
    leather = [hx('#0f1828'), hx('#16233a'), hx('#1d2f4c'), hx('#28406a'), hx('#35548a')]
    paper = [hx('#c9b78f'), hx('#ddcca6'), hx('#ebdfc2'), hx('#f4ead2'), hx('#fbf5e6')]
    brass, brass_l, brass_d = hx('#b89a52'), hx('#dcc27a'), hx('#7f6a35')
    teal = hx('#2ec9c0')
    BW, BH = 292, 180
    rnd = random.Random(7)
    # cover
    for y in range(BH):
        for x in range(BW):
            edge = x in (0, BW - 1) or y in (0, BH - 1)
            c = leather[0] if edge else leather[2]
            if not edge and (x in (1, BW - 2) or y in (1, BH - 2)):
                c = leather[3]
            if not edge and rnd.random() < 0.06:
                c = leather[1] if rnd.random() < 0.6 else leather[3]
            t.set(x, y, c)
    # stitching along the cover edge
    for x in range(6, BW - 6, 3):
        t.set(x, 4, leather[4]); t.set(x, BH - 5, leather[4])
    for y in range(6, BH - 6, 3):
        t.set(4, y, leather[4]); t.set(BW - 5, y, leather[4])
    # page block with stacked page edges, then the two pages and the spine gutter
    px0, py0, px1, py1 = 9, 9, BW - 10, BH - 10
    for y in range(py0, py1 + 1):
        for x in range(px0, px1 + 1):
            t.set(x, y, paper[0] if (y - py0) % 2 == 0 and (x < px0 + 3 or x > px1 - 3 or y > py1 - 3) else paper[1])
    mid = BW // 2
    for y in range(py0 + 2, py1 - 2):
        for x in range(px0 + 3, px1 - 3):
            d = abs(x + 0.5 - mid)
            c = paper[3]
            if d < 2:
                c = paper[0]
            elif d < 5:
                c = paper[1]
            elif d < 9:
                c = paper[2]
            elif rnd.random() < 0.025:
                c = paper[2] if rnd.random() < 0.7 else paper[4]
            t.set(x, y, c)
    # brass corner caps
    for (cx, cy, fx, fy) in ((0, 0, 1, 1), (BW - 1, 0, -1, 1), (0, BH - 1, 1, -1), (BW - 1, BH - 1, -1, -1)):
        for i in range(10):
            for j in range(10 - i):
                c = brass_l if i == 0 or j == 0 else brass if i + j < 7 else brass_d
                t.set(cx + fx * i, cy + fy * j, c)
        t.set(cx + fx * 3, cy + fy * 3, teal)
    # little sift star motifs in the page corners
    for (sx, sy) in ((px0 + 8, py0 + 6), (mid - 12, py0 + 6), (mid + 8, py0 + 6), (px1 - 12, py0 + 6),
                     (px0 + 8, py1 - 9), (mid - 12, py1 - 9), (mid + 8, py1 - 9), (px1 - 12, py1 - 9)):
        for dx, dy in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
            t.set(sx + dx, sy + dy, hx('#9fd8dc') if (dx, dy) != (1, 1) else hx('#e98fb4'))
    # a ribbon bookmark hanging from the top of the spine
    for y in range(0, 26):
        for x in range(mid - 2, mid + 2):
            c = hx('#c2465c') if x < mid else hx('#9c3448')
            if y > 22 and (x - (mid - 2)) in ((y - 23), 3 - (y - 23)):
                continue
            t.set(x, py0 + y, c)
    # category tabs (unselected, then selected one row lower)
    tab_colors = [hx('#3fb6cb'), hx('#e98fb4'), hx('#e8c25a'), hx('#7fd4a8'), hx('#2b3a52')]
    for row, sel in ((184, False), (196, True)):
        for i, tc in enumerate(tab_colors):
            x0 = i * 26
            for y in range(12):
                for x in range(24):
                    edge = x in (0, 23) or y == 0
                    c = darken(tc, 0.45) if edge else lighten(tc, 0.18) if (sel and y < 3) else tc if sel else darken(tc, 0.15)
                    t.set(x0 + x, row + y, c)
            for x in range(8, 16):
                t.set(x0 + x, row + 5, lighten(tc, 0.5) if sel else lighten(tc, 0.25))
    # page turn arrows: next and previous, normal and hover
    arrow = ['.......#..........', '.......##.........', '########o#........', '#oooooooo##.......', '#ooooooooo##......',
             '#oooooooo##.......', '########o#........', '.......##.........', '.......#..........', '..................']
    for row, hover in ((184, False), (194, True)):
        body = brass_l if hover else brass
        for j, line in enumerate(arrow):
            for i, ch in enumerate(line):
                if ch == '.':
                    continue
                c = brass_d if ch == '#' else body
                t.set(128 + i, row + j, c)          # next (points right)
                t.set(128 + 20 + 17 - i, row + j, c)  # previous (mirrored)
    out('gui/codex', t)
    # the item: a teal leather book with brass corners and a glowing sift star on the cover
    out('item/sift_codex', pal_sprite([
        '................',
        '..bbbbbbbbbbbb..',
        '.bLLLLLLLLLLLLp.',
        '.bLllllllllllLp.',
        '.bLll..ss..llLp.',
        '.bLl..sggs..lLp.',
        '.bLl.sggggs.lLp.',
        '.bLl..sggs..lLp.',
        '.bLll..ss..llLp.',
        '.bLllllllllllLp.',
        '.bLlllllllllllLp',
        '.bBlllllllllllLp',
        '.bBBllllllllllLp',
        '.bbbbbbbbbbbbbpp',
        '..pppppppppppppp',
        '................',
    ], {'b': hx('#0f1828'), 'L': hx('#28406a'), 'l': hx('#1d2f4c'), 's': hx('#15a89f'), 'g': hx('#2ef2e2'), 'B': hx('#b89a52'),
        'p': hx('#f4ead2')}))


# ================================================================== sniffer saddle


# ================================================================== armor layers


def armor_layers():
    from modelkit import Cube
    pal = SIFTITE

    def paint(img, cube):
        px = img.load()
        for face, (fx, fy, fw, fh) in cube.faces().items():
            for y in range(fh):
                for x in range(fw):
                    edge = x == 0 or y == 0 or x == fw - 1 or y == fh - 1
                    c = pal[1] if edge else pal[3 if (x + y) % 5 else 4]
                    if not edge and (x * 2 + y) % 9 == 0:
                        c = SIFTITE_PINK
                    px[fx + x, fy + y] = c

    def cube(uv, size):
        c = Cube((0, 0, 0), size)
        c.uv = uv
        return c

    hum = Image.new('RGBA', (64, 32))
    paint(hum, cube((0, 0), (8, 8, 8)))       # helmet
    paint(hum, cube((16, 16), (8, 12, 4)))    # chest
    paint(hum, cube((40, 16), (4, 12, 4)))    # arms
    paint(hum, cube((0, 16), (4, 12, 4)))     # boots (legs region)
    hp = hum.load()
    for (x, y) in ((8 + 1, 8 + 2), (8 + 2, 8 + 2), (8 + 5, 8 + 2), (8 + 6, 8 + 2)):
        hp[x, y] = (0, 0, 0, 0)  # visor eye slits
    for y in range(8 + 4, 8 + 8):
        for x in range(8 + 2, 8 + 6):
            hp[x, y] = (0, 0, 0, 0)  # open face
    out('entity/equipment/humanoid/siftite', hum)
    leg = Image.new('RGBA', (64, 32))
    paint(leg, cube((16, 16), (8, 12, 4)))
    paint(leg, cube((0, 16), (4, 12, 4)))
    out('entity/equipment/humanoid_leggings/siftite', leg)


# ================================================================== particles & effects


def particles():
    def blob(size, color, alpha_fn):
        t = Tex(size, size)
        c = (size - 1) / 2
        for y in range(size):
            for x in range(size):
                d = math.hypot(x - c, y - c) / (size / 2)
                a = alpha_fn(d)
                if a > 0:
                    t.set(x, y, with_alpha(color, int(255 * a)))
        return t

    # vanilla-style stepped orbs: a white core, a light ring and a grey rim, shrinking frame by frame
    grey = {'W': hx('#ffffff'), 'L': hx('#d6d6d6'), 'M': hx('#a3a3a3')}
    orbs = [
        ['..MMMM..', '.MLLLLM.', 'MLLWWLLM', 'MLWWWWLM', 'MLWWWWLM', 'MLLWWLLM', '.MLLLLM.', '..MMMM..'],
        ['........', '..MMMM..', '.MLLLLM.', '.MLWWLM.', '.MLWWLM.', '.MLLLLM.', '..MMMM..', '........'],
        ['........', '........', '..MLLM..', '..LWWL..', '..LWWL..', '..MLLM..', '........', '........'],
        ['........', '........', '........', '...WL...', '...LM...', '........', '........', '........'],
    ]
    for i, rows in enumerate(orbs):
        t = Tex(8, 8)
        draw_map(t, rows, grey)
        out(f'particle/drifting_soul_{i}', t)
    for i in range(3):
        t = Tex(8, 8)
        s = 2 + i
        for y in range(8 - s, 8):
            for x in range(4 - s // 2, 4 + (s + 1) // 2):
                t.set(x, y, hx('#ffffff'))
        t.set(4, 8 - s - 1, hx('#ffffff'))
        out(f'particle/chrome_droplet_{i}', t)
    bub = Tex(8, 8)
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            if 2.3 < d < 3.5:
                bub.set(x, y, with_alpha(hx('#ffffff'), 220))
    bub.set(2, 2, hx('#ffffff'))
    out('particle/chrome_bubble', bub)
    for i in range(2):
        out(f'particle/dream_pollen_{i}', blob(4 + i * 2, hx('#ffffff'), lambda d: 1.0 if d < 0.8 else 0))
    note = Tex(8, 8)
    draw_map(note, ['...WWW..', '...W.WW.', '...W..W.', '...W....', '.WWW....', 'WWWW....', 'WWW.....', '........'], {'W': hx('#ffffff')})
    out('particle/sift_note', note)
    out('particle/guide_note', note)
    ring = Tex(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 6.2 < d < 7.6:
                ring.set(x, y, with_alpha(hx('#ffffff'), 230))
            elif 5.4 < d <= 6.2:
                ring.set(x, y, with_alpha(hx('#ffffff'), 90))
    out('particle/resonance_ring', ring)
    for i in range(2):
        out(f'particle/glow_dust_{i}', blob(3 + i, hx('#ffffff'), lambda d: 1.0 if d < 0.9 else 0))
    for w, pal in (('lullwood', LULL_LEAF), ('wishwood', WISH_LEAF)):
        shapes = [['..ab..', '.abbc.', 'abbbc.', '.bbc..', '..c...'], ['.ab...', 'abbc..', '.bbbc.', '..bc..'], ['...a..', '..abc.', '.abbc.', 'abbc..',
                                                                                                                 '.c....']]
        for i, sh in enumerate(shapes):
            t = Tex(8, 8)
            draw_map(t, sh, {'a': pal[-1], 'b': pal[2], 'c': pal[0]}, 1, 1)
            out(f'particle/{w}_leaf_{i}', t)
    # mist: blocky cloud puffs in three flat alpha steps (no smooth gradients)
    for i in range(2):
        t = Tex(16, 16)
        rnd = random.Random(1106 + i)
        lobes = [(7.5, 8.5, 5.2)] + [(7.5 + rnd.uniform(-4, 4), 8 + rnd.uniform(-3, 2), rnd.uniform(2.5, 3.6)) for _ in range(3)]
        for y in range(16):
            for x in range(16):
                d = min(math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.25) / r for cx, cy, r in lobes)
                if d < 1.0:
                    a = 255 if d < 0.45 else 170 if d < 0.75 else 90
                    t.set(x, y, with_alpha(hx('#ffffff'), a))
        out(f'particle/sift_mist_{i}', t)
    for i in range(2):
        st = Tex(8, 8)
        arms = 3 - i
        for k in range(-arms, arms + 1):
            st.set(3 + k, 3, hx('#ffffff')); st.set(3, 3 + k, hx('#ffffff'))
        st.set(3, 3, hx('#ffffff'))
        out(f'particle/star_sparkle_{i}', st)
    for i in range(3):
        t = Tex(8, 8)
        draw_map(t, orbs[i + 1] if i < 2 else orbs[3], grey)
        out(f'particle/portal_soul_{i}', t)
    for i in range(3):
        out(f'particle/footstep_puff_{i}', blob(8, hx('#ffffff'), lambda d, i=i: (1.0 if d < 0.45 - i * 0.1 else 0.75) if d < 0.9 - i * 0.2 else 0))
    for i in range(2):
        out(f'particle/glow_splat_{i}', blob(4 + i * 2, hx('#ffffff'), lambda d: 1.0 if d < 0.85 else 0))
    # slime trail: hand-drawn jelly splotches (tinted per Bulb), with a bright gloss pixel
    trails = [
        ['..####..', '.######.', '##w#####', '########', '.#######', '..#####.', '...##...', '........'],
        ['........', '..###...', '.#w####.', '########', '.######.', '..####..', '.##.....', '........'],
        ['...##...', '.#####..', '.#w#####', '.#######', '..######', '...####.', '....#...', '........'],
    ]
    for i, rows in enumerate(trails):
        t = Tex(8, 8)
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == '#':
                    t.set(x, y, with_alpha(hx('#ffffff'), 200))
                elif ch == 'w':
                    t.set(x, y, hx('#ffffff'))
        out(f'particle/slime_trail_{i}', t)
    ws = Tex(8, 8)
    draw_map(ws, ['...M...', '...L...', '..LWL..', 'MLWWWLM', '..LWL..', '...L...', '...M...'], grey)
    out('particle/wishing_star', ws)
    for i in range(2):
        sp = Tex(8, 8)
        draw_map(sp, ['..w..', '.wWw.', 'wWWWw', '.wWw.', '..w..'] if i == 0 else ['.w.w.', 'wWWWw', '.WWW.', 'wWWWw', '.w.w.'],
                 {'w': with_alpha(hx('#ffffff'), 160), 'W': hx('#ffffff')}, 1, 1)
        out(f'particle/sleep_spore_{i}', sp)
    # kill stars: 16x16, white with stepped grey shading so they take the mob's colour
    kill = [
        ['.......W........', '.......W........', '......WWL.......', '......WWL.......', '.....WWWLL......', 'WWWWWWWWLLLLLLM.',
         '.WWWWWWWLLLLLM..', '..WWWWWLLLLLM...', '...WWWWLLLLM....', '...WWWWLLLLM....', '..WWWWLMMLLLM...', '..WWWLM..MLLM...',
         '.WWWLM....MLLM..', '.WWLM......MLM..', '.WLM........MM..', '................'],
        ['......WWWWWWWW..', '......WLLLLLLM..', '......WWWWWWWM..', '......W......M..', '......W......M..', '......W......M..',
         '......W......M..', '......W......M..', '......W......M..', '..WWWWL...WWWWM.', '.WWWLLLL.WWWLLLM', '.WWLLLLM.WWLLLLM',
         '.WLLLLLM.WLLLLLM', '..LLLLM...LLLLM.', '................', '................'],
        ['................', '..WWW.....WWW...', '.WWWWW...WWWWLL.', 'WWWWWWW.WWWWWLLM', 'WWWWWWWWWWWWLLLM', 'WWWWWWWWWWWLLLLM',
         '.WWWWWWWWWLLLLM.', '..WWWWWWWLLLLM..', '...WWWWWLLLLM...', '....WWWLLLLM....', '.....WWLLLM.....', '......WLLM......',
         '.......LM.......', '................', '................', '................'],
        ['.......W........', '.......WL.......', '......WWLL......', '......WWLL......', '.....WWWLLL.....', '.....WWLLLL.....',
         '....WWWLLLLM....', '....WWLLLLLM....', '...WWWLLLLLLM...', '...WWLLLLLLLM...', '...WWLLLLLLLM...', '...WLLLLLLLLM...',
         '....LLLLLLLM....', '.....MMMMMM.....', '................', '................'],
    ]
    for i, rows in enumerate(kill):
        t = Tex(16, 16)
        draw_map(t, rows, {'W': hx('#ffffff'), 'L': hx('#d2d2d2'), 'M': hx('#9a9a9a')})
        out(f'particle/kill_star_{i}', t)
    # mob effect icons (18x18)
    for name, c in (('deafened', '#7fe8e0'), ('euphoria', '#f59ad0')):
        t = Tex(18, 18)
        if name == 'deafened':
            draw_map(t, ['......dddd........', '....ddcccccd......', '...dccccccccd.....', '..dcccwwwwcccd....', '..dccw....wccd....', '..dcw......wcd....',
                         '..dcw.......d.....', '..dccw............', '...dcw..X.....X...', '...dccw..X...X....', '....dcw...X.X.....', '....dcw....X......',
                         '....dcw...X.X.....', '...dccw..X...X....', '...dcw..X.....X...', '....dd............', '..................', '..................'],
                     {'d': hx('#1c5f66'), 'c': hx(c), 'w': hx('#e8fffe'), 'X': hx('#ff6f8f')})
        else:
            draw_map(t, ['..................', '.....dd....dd.....', '....dccd..dccd....', '...dccccddccccd...', '...dcwwcccccccd...', '...dcwccccccccd...',
                         '...dccccccccccd...', '....dccccccccd.....', '.....dccccccd.....', '......dccccd......', '.......dccd.......', '........dd........',
                         '..y............y..', '.yYy..........yYy.', '..y............y..', '..................', '........y.........', '.......yYy........'],
                     {'d': hx('#a8457a'), 'c': hx(c), 'w': hx('#fff0f7'), 'y': hx('#ffe89a'), 'Y': hx('#ffffff')})
        out(f'mob_effect/{name}', t)
    # entranced: a glowing music note in a spiral
    t = Tex(18, 18)
    draw_map(t, ['..................', '.....dddddd.......', '...dd......dd.....', '..d...WWWWW..d....', '.d....WccccW..d...', '.d....Wc..cW..d...',
                 'd.....Wc..cW...d..', 'd.....Wc..cW...d..', 'd.....Wc..cW...d..', 'd...WWWc.WWW...d..', 'd..WcccW.Wccd..d..', '.d.WcccW.Wccd.d...',
                 '.d..WWW...WW..d...', '..d..........d....', '...dd......dd.....', '.....dddddd.......', '..................', '..................'],
             {'d': hx('#15a89f'), 'c': hx('#2ef2e2'), 'W': hx('#e8fffc')})
    out('mob_effect/entranced', t)


# ================================================================== sky + logo


def nebula():
    """Seamless cube-map nebula laid out 3x2 (matches SiftSkyRenderer.dir())."""
    N = 256
    img = np.zeros((N * 2, N * 3, 3), dtype=np.float32)
    rng = np.random.default_rng(7)
    # 3D value noise lattice
    L = 24
    lattice = rng.random((L, L, L)).astype(np.float32)

    def noise3(p):
        q = (p + 1.0) * (L / 2 - 1) * 0.999
        i = np.floor(q).astype(int)
        f = q - i
        f = f * f * (3 - 2 * f)
        out_ = 0
        for dx in (0, 1):
            for dy in (0, 1):
                for dz in (0, 1):
                    w = (f[..., 0] if dx else 1 - f[..., 0]) * (f[..., 1] if dy else 1 - f[..., 1]) * (f[..., 2] if dz else 1 - f[..., 2])
                    out_ = out_ + w * lattice[(i[..., 0] + dx) % L, (i[..., 1] + dy) % L, (i[..., 2] + dz) % L]
        return out_

    def fbm3(p, octaves=5):
        s, a, tot = 0, 1.0, 0
        for o in range(octaves):
            s = s + noise3(np.clip(p * (2 ** o) * 0.5, -1, 1) if o == 0 else ((p * (2 ** o) * 0.5 + 1) % 2) - 1) * a
            tot += a
            a *= 0.55
        return s / tot

    u = (np.arange(N) + 0.5) / N * 2 - 1
    S_, T_ = np.meshgrid(u, u)
    deep = np.array([0.05, 0.05, 0.16])
    cyan = np.array([0.35, 0.85, 0.95])
    pink = np.array([0.98, 0.52, 0.80])
    purple = np.array([0.48, 0.30, 0.78])
    for face in range(6):
        if face == 0:
            d = np.stack([np.ones_like(S_), -T_, -S_], -1)
        elif face == 1:
            d = np.stack([-np.ones_like(S_), -T_, S_], -1)
        elif face == 2:
            d = np.stack([S_, np.ones_like(S_), T_], -1)
        elif face == 3:
            d = np.stack([S_, -np.ones_like(S_), -T_], -1)
        elif face == 4:
            d = np.stack([S_, -T_, np.ones_like(S_)], -1)
        else:
            d = np.stack([-S_, -T_, -np.ones_like(S_)], -1)
        d = d / np.linalg.norm(d, axis=-1, keepdims=True)
        # clean cyan sky (see the biome reference): turquoise overhead, pale mint at the horizon
        zenith = np.array([0.29, 0.78, 0.72])
        mid = np.array([0.37, 0.84, 0.78])
        horizon = np.array([0.66, 0.94, 0.88])
        below = np.array([0.56, 0.88, 0.82])
        e = d[..., 1][..., None]
        up = np.clip(e, 0, 1) ** 0.55
        col = np.where(up < 0.5, horizon * (1 - up * 2) + mid * (up * 2), mid * (2 - up * 2) + zenith * (up * 2 - 1))
        down = np.clip(-e * 3, 0, 1)
        col = np.where(e < 0, horizon * (1 - down) + below * down, col)
        # the faintest high wisps so the dome isn't dead flat
        wisp = np.clip((fbm3(d * 1.4) - 0.58) * 2.0, 0, 1)[..., None] * np.clip(e * 3, 0, 1)
        col = col * (1 - wisp * 0.08) + np.array([0.85, 0.98, 0.95]) * wisp * 0.08
        ox, oy = (face % 3) * N, (face // 3) * N
        img[oy:oy + N, ox:ox + N] = np.clip(col, 0, 1)
    im = Image.fromarray((img * 255).astype(np.uint8), 'RGB').convert('RGBA')
    out('environment/nebula', im)


def logo():
    W, H = 256, 256
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for r in range(120, 0, -1):
        t = r / 120
        c = lerp(hx('#ff9fd8'), hx('#1a1f4a'), t)
        d.ellipse([128 - r, 128 - r, 128 + r, 128 + r], fill=c)
    rnd = random.Random(3)
    for _ in range(60):
        x, y = rnd.randrange(30, 226), rnd.randrange(30, 226)
        if (x - 128) ** 2 + (y - 128) ** 2 < 110 ** 2:
            d.point((x, y), fill=(255, 255, 255, 255))
    # a bulb silhouette
    d.ellipse([88, 120, 168, 190], fill=hx('#7fe3e6'))
    d.ellipse([98, 90, 158, 145], fill=hx('#7fe3e6'))
    d.rounded_rectangle([104, 40, 116, 100], 6, fill=hx('#7fe3e6'))
    d.rounded_rectangle([140, 40, 152, 100], 6, fill=hx('#7fe3e6'))
    d.rounded_rectangle([107, 48, 113, 95], 3, fill=hx('#ff9ccf'))
    d.rounded_rectangle([143, 48, 149, 95], 3, fill=hx('#ff9ccf'))
    d.rectangle([112, 108, 118, 118], fill=hx('#1b2340'))
    d.rectangle([138, 108, 144, 118], fill=hx('#1b2340'))
    d.ellipse([122, 70, 134, 82], fill=hx('#fff59a'))
    im = im.resize((128, 128), Image.NEAREST)
    im.save(os.path.join(ROOT, 'src/main/resources/thesift_logo.png'))


def hd_items():
    """Every item sprite, hand-drawn at 16x in the vanilla style (tools/items16.py), with the
    Siftite gear written as animated strips; plus the worn Siftite armour, the mob effect icons
    and the flower sprites drawn alongside them."""
    import items16
    anims = items16.item_animations()
    for name, img in items16.all_items().items():
        if name not in anims:
            out(f'item/{name}', img)
    for name, (frames, frametime) in anims.items():
        out(f'item/{name}', strip(frames), {'animation': {'frametime': frametime, 'interpolate': False}})
    for name, img in items16.armor_layers().items():
        out(name, img)
    for name, img in items16.effect_icons().items():
        out(f'mob_effect/{name}', img)
    for name, img in items16.flower_textures().items():
        out(f'block/{name}', img)


def boss_bar():
    """The Dictator's health bar (2x texels): a sculk-steel frame with gold trim and phase ticks,
    a glowing fill strip, an empty track, and a devil-head emblem."""
    import rpgsprite as RS
    import rpgitems as RI
    t = Image.new('RGBA', (512, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(t)
    # frame plate 428 x 44
    d.rounded_rectangle((6, 8, 421, 39), radius=8, fill=(14, 22, 32, 255), outline=(6, 10, 14, 255), width=2)
    d.rounded_rectangle((9, 11, 418, 36), radius=6, outline=(42, 58, 82, 255), width=2)
    d.line((14, 13, 413, 13), fill=(70, 96, 128, 255), width=1)
    d.line((14, 34, 413, 34), fill=(10, 16, 22, 255), width=1)
    d.rectangle((30, 18, 397, 31), fill=(184, 154, 82, 255))
    d.rectangle((31, 19, 396, 30), fill=(5, 8, 12, 255))
    for frac in (0.6, 0.25):
        x = 32 + int(364 * frac)
        d.rectangle((x - 1, 15, x, 33), fill=(220, 194, 122, 255))
        d.rectangle((x - 1, 15, x, 16), fill=(255, 240, 180, 255))
    # tuning-fork flourishes at both ends
    for x0, sgn in ((0, 1), (427, -1)):
        for dy in (14, 30):
            d.rectangle((min(x0, x0 + sgn * 10), dy, max(x0, x0 + sgn * 10), dy + 3), fill=(184, 154, 82, 255))
            d.rectangle((min(x0, x0 + sgn * 10), dy, max(x0, x0 + sgn * 10), dy), fill=(236, 214, 140, 255))
        d.rectangle((min(x0 + sgn * 8, x0 + sgn * 12), 14, max(x0 + sgn * 8, x0 + sgn * 12), 33), fill=(184, 154, 82, 255))
    # fill strip (y 44) and empty track (y 54), 364 x 10 each
    fill_rows = [(200, 255, 250), (120, 250, 240), (46, 242, 226), (46, 242, 226), (40, 220, 210), (32, 196, 190), (26, 168, 162),
                 (21, 140, 136), (16, 112, 110), (10, 70, 70)]
    for y, c in enumerate(fill_rows):
        d.line((0, 44 + y, 363, 44 + y), fill=(*c, 255))
    for x in range(10, 364, 26):
        d.rectangle((x, 46, x + 1, 50), fill=(220, 255, 252, 255))
        d.rectangle((x + 2, 46, x + 3, 47), fill=(220, 255, 252, 255))
        d.rectangle((x - 1, 49, x, 51), fill=(220, 255, 252, 255))
    for y in range(10):
        d.line((0, 54 + y, 363, 54 + y), fill=(18, 26, 36, 255) if y not in (0, 9) else (8, 12, 18, 255))
    # devil emblem 32 x 32 at (448, 0)
    sp = RS.Sprite()
    sp.add(RS.capsule(9, 11, 3, 4, 2.0) | RS.capsule(3, 4, 6, 1, 1.4) | RS.capsule(23, 11, 29, 4, 2.0) | RS.capsule(29, 4, 26, 1, 1.4), '#e3ddcc', 'dome')
    sp.add(RS.ellipse(16, 18, 10, 12), '#5a1a30', 'dome')
    head = sp.render()
    RI.overlay(head, ['bbb....bbb', '.eee..eee.', '.eEe..eEe.', '..........', '....nn....', '..........', 'mmmmmmmmmm', 'mfmfmmfmfm', '.mmmmmmmm.'],
               {'b': (23, 8, 16), 'e': (4, 8, 12), 'E': (46, 242, 226), 'n': (4, 8, 12), 'm': (4, 8, 12), 'f': (244, 240, 229)}, 11, 12)
    t.alpha_composite(head, (448, 0))
    out('gui/conductor_bar', t)


def mini_boss_bars():
    """Health bars for the great players, in the same layout as the Conductor's (2x texels): a
    sculk-and-bone frame dressed for each - bone scutes for the Thumper, strings and webs for the
    Strummer - a glowing fill in its colour, and a skull emblem."""
    import math
    BONE, BONE_L, BONE_D = (222, 214, 192), (246, 240, 224), (150, 140, 118)
    HIDE, HIDE_L, HIDE_D = (12, 38, 44), (24, 66, 72), (5, 18, 22)
    GLOW = (41, 223, 235)

    def ramp(c):
        rows = []
        for i in range(10):
            k = 1.0 - i / 9.0
            hi = 0.55 if i < 2 else 0.0
            rows.append(tuple(min(255, int(v * (0.35 + 0.75 * k) + 255 * hi * (1 - i / 2))) for v in c))
        return rows

    def emblem(rows, keys):
        e = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != '.':
                    for dy in (0, 1):
                        for dx in (0, 1):
                            e.putpixel((x * 2 + dx, y * 2 + dy), (*keys[ch], 255))
        return e
    K = {'b': BONE, 'l': BONE_L, 'd': BONE_D, 'k': (8, 12, 14), 'g': GLOW, 'h': HIDE, 'H': HIDE_L, 'r': (205, 80, 52), 'R': (240, 130, 70),
         'y': (226, 190, 96), 'v': (196, 108, 255), 'V': (122, 58, 184), 's': (127, 247, 255), 'G': (180, 255, 255)}
    bars = {
        'thumper': ((255, 159, 58), [
            '................', '......rRRr......', '.....rrrrrr.....', '....dbbbbbbd....', '...dlbbbbbbbd...', '..dbkkbbbbkkbd..', '..dbkgbbbbgkbd..',
            '..dbkkbbbbkkbd..', '..dbbbbddbbbbd..', '...dbbkddkbbd...', '...hdbbbbbbdh...', '..hHhddbbddhHh..', '..hhhhkkkkhhhh..', '...hhkbkbkbhh...',
            '....hhhhhhhh....', '................']),
        'strummer': ((196, 108, 255), [
            '..s..........s..', '...s........s...', '....s.dddd.s....', '.....dbbbbd.....', '....dbbbbbbd....', '...dggbbbbggd...', '..dggkgbbgkggd..',
            '..dgggbbbbgggd..', '...dbbbddbbbd...', '....dbkkkkbd....', '.....dbbbbd.....', '...vV.dbbd.Vv...', '..v..V.dd.V..v..', '.v....V..V....v.',
            '..............v.', '................']),
    }
    for name, (col, em) in bars.items():
        t = Image.new('RGBA', (512, 64), (0, 0, 0, 0))
        d = ImageDraw.Draw(t)
        # the frame: sculk hide over a bone rim
        d.rounded_rectangle((6, 8, 421, 39), radius=8, fill=(*HIDE, 255), outline=(*HIDE_D, 255), width=2)
        d.rounded_rectangle((9, 11, 418, 36), radius=6, outline=(*HIDE_L, 255), width=2)
        d.rectangle((30, 18, 397, 31), fill=(*BONE_D, 255))
        d.rectangle((31, 19, 396, 30), fill=(4, 10, 12, 255))
        # sculk veins and glowing specks creeping over the frame
        rnd = random.Random(sum(map(ord, name)))
        for _ in range(26):
            x = rnd.randrange(14, 410)
            y = rnd.choice((13, 14, 33, 34))
            d.point((x, y), fill=(*GLOW, 255) if rnd.random() < 0.4 else (*HIDE_L, 255))
            d.point((x + 1, y), fill=(*HIDE_L, 255))
        if name == 'thumper':
            # bone scutes along both rims, drum-cord crosses at the ends
            for x in range(18, 410, 22):
                for y0 in (9, 33):
                    d.polygon([(x, y0 + 3), (x + 4, y0), (x + 12, y0), (x + 16, y0 + 3), (x + 12, y0 + 6), (x + 4, y0 + 6)], fill=(*BONE, 255), outline=(*BONE_D, 255))
                    d.point((x + 6, y0 + 2), fill=(*BONE_L, 255))
            for x0 in (0, 412):
                for k in range(4):
                    d.line((x0 + k * 4, 12, x0 + 12 - k * 4, 36), fill=(205, 80, 52, 255), width=2)
        else:
            # four glowing strings over the frame, webs in the corners, spider legs at the ends
            for i, y in enumerate((12, 15, 32, 35)):
                d.line((10, y, 417, y), fill=(127, 247, 255, 255) if i % 2 == 0 else (90, 180, 200, 255), width=1)
            for cx, cy, sx, sy in ((8, 10, 1, 1), (419, 10, -1, 1), (8, 37, 1, -1), (419, 37, -1, -1)):
                for k in range(3):
                    d.line((cx, cy, cx + sx * (6 + k * 4), cy + sy * (12 - k * 4)), fill=(230, 230, 240, 200), width=1)
                for r in (4, 8):
                    d.line((cx + sx * r, cy, cx, cy + sy * r), fill=(230, 230, 240, 200), width=1)
            for x0, sgn in ((6, -1), (421, 1)):
                for k, y in enumerate((14, 22, 30)):
                    d.line((x0, y, x0 + sgn * 6, y - 4), fill=(*BONE, 255), width=2)
                    d.line((x0 + sgn * 6, y - 4, x0 + sgn * 9, y + 4), fill=(*BONE_D, 255), width=2)
        # phase ticks (bone) at its movements
        for frac in (0.66, 0.33):
            x = 32 + int(364 * frac)
            d.rectangle((x - 1, 15, x, 33), fill=(*BONE, 255))
            d.rectangle((x - 1, 15, x, 16), fill=(*BONE_L, 255))
        # fill strip (y 44) with a pulse pattern, empty track (y 54)
        for y, c in enumerate(ramp(col)):
            d.line((0, 44 + y, 363, 44 + y), fill=(*c, 255))
        for x in range(8, 364, 22):
            d.rectangle((x, 46, x + 1, 47), fill=(255, 255, 255, 220))
            d.point((x + 2, 49), fill=(255, 255, 255, 160))
        for y in range(10):
            d.line((0, 54 + y, 363, 54 + y), fill=(10, 26, 30, 255) if y not in (0, 9) else (4, 12, 14, 255))
        t.alpha_composite(emblem(em, K), (448, 0))
        out(f'gui/{name}_bar', t)


def main():
    stone_textures()
    soils()
    woods()
    flora()
    functional()
    items()
    armor_layers()
    dictator_things()
    stage_things()
    codex()
    particles()
    nebula()
    logo()
    hd_items()
    __import__("weaver_art").textures(out)  # the Weaver (E2): sculk string, guitars, musical cobweb
    __import__("caravans").textures(out)  # C: music crystals, prism ore/block, worn prism armour
    __import__("sculk_bloom").textures(out)  # A1: the Sculk Bloom (flower, pot and item)
    corruption_overlays()
    boss_bar()
    mini_boss_bars()
    import plants_h_art  # H: the Sift Gate Frame and the Pitcher Planter
    for name, img in plants_h_art.block_textures().items():
        out(name, img)
    __import__('sea_art').block_textures(out)  # sea & sky: glowkelp, anemone, coral sand, cloud, chime bell, organ reed
    __import__('swifter_art').block_textures(out)  # A2 Swifter & White Forest: white turf/leaves, puffbloom, den, fluff
    __import__('chrome').textures(out)  # A3 Chrome: rainbow fluid, chime sand/glass, fish buckets, Rainbow Daze
    __import__('gear_art').textures(out)  # B4 gear: Seraphim prism gear + tools, instruments, guitars, cannonball
    __import__('gatefx').textures(out)  # B1 Portal & sky FX: portal sky window, rainbows, aurora, colour clouds, shooting stars
    __import__('ui_art').textures(out)  # B3 Boss bars & Codex: themed boss bars, codex specimen plates, page effects
    __import__('vanilla_remap').remap(TEX)  # C1 Block art: every Sift block/plant texture rebuilt from its vanilla reference (keep last)
    need = os.path.join(ROOT, 'build/textures_needed.txt')
    if os.path.exists(need):
        missing = [n for n in open(need).read().split() if not os.path.exists(os.path.join(TEX, n + '.png'))]
        if missing:
            print('MISSING TEXTURES:', ' '.join(missing))
            sys.exit(1)
    print('textures ok')


if __name__ == '__main__':
    main()
