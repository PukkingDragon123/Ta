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
SOIL = ramp('#4f3552', '#5f4262', '#715173', '#846285', '#977596')
GRASS = ramp('#2d9a98', '#3cb3ab', '#50c8bb', '#69dbca', '#8cebd9')
LUMEN = ramp('#1f6f80', '#29889a', '#34a2b0', '#43bcc4', '#62d6d6')
SERBIM = ramp('#1f6d94', '#2b8fbb', '#3fb0da', '#66d2f0', '#9aeefc', '#d4fbff')
SIFTITE = ramp('#2a8ea8', '#3fb6cb', '#63d6e2', '#9aeaf0', '#d8fbfd')
SIFTITE_PINK = hx('#f29bd6')
LULL_BARK = ramp('#4f4a6d', '#625c82', '#766f97', '#8b84ab')
LULL_WOOD = ramp('#9f97c6', '#b1a9d4', '#c2bbe0', '#d3cdea', '#e3def3')
WISH_BARK = ramp('#5a2744', '#6f3355', '#844267', '#9a5379')
WISH_WOOD = ramp('#c9738f', '#d98aa4', '#e6a0b8', '#f0b6ca', '#f8cbdb')
LULL_LEAF = ramp('#2fa9b2', '#43c3c6', '#60d8d6', '#86e9e2', '#b3f5ef', '#e2fffb')
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
    n = fbm(16, 16, seed, (8, 4, 2))
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            v = n[y, x] + (0.15 if y < 5 else 0)
            if v > 1 - density:
                t.set(x, y, pal[min(len(pal) - 1, int((v - (1 - density)) / density * len(pal)))])
                if glow and rnd.random() < 0.06:
                    t.set(x, y, glow)
    return t


def chiseled(pal, motif, seed, keys):
    t = polished(pal, seed)
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
    out('block/dreamstone', stone(DREAM[1:5], 11, flecks=(DREAM[5], 6)))
    out('block/cobbled_dreamstone', cobbled(DREAM[:5], 12, DREAM_MORTAR))
    out('block/polished_dreamstone', polished(DREAM, 13))
    out('block/dreamstone_bricks', bricks(DREAM[1:], DREAM_MORTAR, 14))
    out('block/cracked_dreamstone_bricks', crack(bricks(DREAM[1:], DREAM_MORTAR, 14), 15, DREAM_MORTAR, 3))
    out('block/mossy_dreamstone_bricks', moss_over(bricks(DREAM[1:], DREAM_MORTAR, 14), 16, LUMEN, hx('#c8fff6')))
    out('block/dreamstone_tiles', tiles(DREAM[1:], DREAM_MORTAR, 17))
    out('block/chiseled_dreamstone', chiseled(DREAM, RUNE, 18, {'L': DREAM[5], 'd': DREAM[0]}))
    pil = stone(DREAM[2:5], 19, cells=(16, 4, 2))
    for y in range(16):
        pil.set(0, y, DREAM[5]); pil.set(1, y, DREAM[4]); pil.set(14, y, DREAM[1]); pil.set(15, y, DREAM[0])
        if y % 5 == 0:
            for x in range(2, 14):
                pil.set(x, y, DREAM[1])
    out('block/dreamstone_pillar_side', pil)
    out('block/dreamstone_pillar_top', polished(DREAM, 20))
    hs = stone(HUSH[1:5], 21, flecks=(hx('#3fd6d0'), 2), cells=(16, 4, 2))
    for y in range(0, 16, 4):
        for x in range(16):
            if random.Random(y * 16 + x).random() < 0.6:
                hs.set(x, y, HUSH[1])
    out('block/hushslate', hs)
    out('block/hushslate_top', polished(HUSH, 22, inner=False))
    out('block/cobbled_hushslate', cobbled(HUSH[:5], 23, HUSH_MORTAR))
    out('block/polished_hushslate', polished(HUSH, 24))
    out('block/hushslate_bricks', bricks(HUSH[1:], HUSH_MORTAR, 25))
    out('block/cracked_hushslate_bricks', crack(bricks(HUSH[1:], HUSH_MORTAR, 25), 26, HUSH_MORTAR, 3))
    out('block/hushslate_tiles', tiles(HUSH[1:], HUSH_MORTAR, 27, 4))
    out('block/chiseled_hushslate', chiseled(HUSH, RUNE, 28, {'L': hx('#4fe8e0'), 'd': HUSH[0]}))
    # sand & sandstone
    out('block/dreamsand', stone(SAND[1:5], 31, flecks=(hx('#fff4fa'), 10), cells=(4, 2, 1)))
    ss = stone(SAND[1:5], 32, cells=(8, 4, 2))
    for x in range(16):
        ss.set(x, 0, SAND[5]); ss.set(x, 1, SAND[4]); ss.set(x, 2, SAND[2])
        ss.set(x, 10, SAND[1]); ss.set(x, 11, SAND[2])
        ss.set(x, 15, SAND[0])
    out('block/dreamsandstone', ss)
    out('block/dreamsandstone_top', stone(SAND[2:6], 33, cells=(8, 4, 2)))
    bot = stone(SAND[1:4], 34)
    out('block/dreamsandstone_bottom', crack(bot, 35, SAND[0], 2))
    out('block/smooth_dreamsandstone', stone(SAND[2:5], 36, cells=(16, 8, 8)))
    cut = stone(SAND[2:5], 37, cells=(16, 8, 4))
    for x in range(16):
        cut.set(x, 0, SAND[5]); cut.set(x, 15, SAND[0]); cut.set(x, 7, SAND[1]); cut.set(x, 8, SAND[4])
    out('block/cut_dreamsandstone', cut)
    ch = polished(SAND, 38)
    draw_map(ch, ['..........', '..hh..hh..', '.h..hh..h.', '.h..pp..h.', '..h.pp.h..', '...hppg...', '..h.pp.h..', '.h..pp..h.', '..hh..hh..',
                  '..........'], {'h': SAND[0], 'p': hx('#c95b8f'), 'g': hx('#ffe89a')}, 3, 3)
    out('block/chiseled_dreamsandstone', ch)
    for i in range(4):
        sus = stone(SAND[1:5], 31, flecks=(hx('#fff4fa'), 10 - i * 2), cells=(4, 2, 1))
        rnd = random.Random(40 + i)
        for _ in range(2 + i * 2):
            x, y = rnd.randrange(3, 13), rnd.randrange(3, 13)
            sus.set(x, y, hx('#7fe8ff')); sus.set(x + 1, y, hx('#4fb9d6'))
        for _ in range(i * 6):
            sus.set(rnd.randrange(16), rnd.randrange(16), SAND[0])
        out(f'block/suspicious_dreamsand_{i}', sus)
    # blush bricks
    out('block/blush_bricks', bricks(BLUSH[1:], BLUSH_MORTAR, 41, rows=4, width=8))
    out('block/cracked_blush_bricks', crack(bricks(BLUSH[1:], BLUSH_MORTAR, 41), 42, BLUSH_MORTAR, 3))
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
    soil = stone(SOIL[:4], 51, flecks=(SOIL[4], 7), cells=(4, 2, 1))
    out('block/sift_soil', soil)
    top = stone(GRASS[:5], 52, cells=(4, 2, 1))
    rnd = random.Random(53)
    for _ in range(9):
        top.set(rnd.randrange(16), rnd.randrange(16), GRASS[4])
    for _ in range(3):
        top.set(rnd.randrange(16), rnd.randrange(16), hx('#f7a8d2'))
    out('block/sift_grass_block_top', top)
    out('block/sift_grass_block_side', grass_side(GRASS, SOIL, 54))
    moss = stone(LUMEN, 55, cells=(4, 2, 1))
    rnd = random.Random(56)
    for _ in range(10):
        x, y = rnd.randrange(16), rnd.randrange(16)
        moss.set(x, y, hx('#c8fff6'))
    out('block/lumen_moss_block', moss)
    # ores
    for name, base, seed in (('serbim_ore', DREAM[1:5], 61), ('deep_serbim_ore', HUSH[1:5], 62)):
        t = stone(base, seed)
        rnd = random.Random(seed)
        for _ in range(4):
            cx, cy = rnd.randrange(2, 13), rnd.randrange(2, 13)
            for dx, dy, c in ((0, 0, 4), (1, 0, 3), (0, 1, 2), (1, 1, 1), (-1, 0, 3), (0, -1, 5)):
                if rnd.random() < 0.85:
                    t.set(cx + dx, cy + dy, SERBIM[c])
        out(f'block/{name}', t)
    rb = stone(SERBIM[:4], 63, cells=(4, 4, 2))
    out('block/raw_serbim_block', crack(rb, 64, SERBIM[0], 3))
    sb = polished(SERBIM[1:], 65)
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        sb.set(x, y, SERBIM[0]); sb.set(x + 1, y + 1, SERBIM[5])
    out('block/serbim_block', sb)
    sf = Tex()
    for y in range(16):
        for x in range(16):
            c = iridescent(x, y, 1.0, 0.35)
            c = lerp(c, SIFTITE[2], 0.45)
            sf.set(x, y, c)
    for i in range(16):
        sf.set(i, 0, SIFTITE[4]); sf.set(0, i, SIFTITE[4]); sf.set(i, 15, SIFTITE[0]); sf.set(15, i, SIFTITE[0])
    for i in range(3, 13):
        sf.set(i, i, lighten(SIFTITE[3], 0.3))
        sf.set(i, 15 - i, SIFTITE_PINK if i % 3 == 0 else SIFTITE[3])
    out('block/siftite_block', sf)
    g = Tex()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                g.set(x, y, with_alpha(iridescent(x, y), 230))
            elif (x + y) % 13 == 0 or (x - y) % 11 == 3:
                g.set(x, y, with_alpha(hx('#ffffff'), 120))
            else:
                g.set(x, y, with_alpha(iridescent(x, y, 2.0), 50))
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
    t = planks(wood, seed)
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
    t = planks(wood, seed)
    for i in range(16):
        t.set(i, 0, bark[1]); t.set(i, 15, bark[1]); t.set(0, i, bark[1]); t.set(15, i, bark[1])
    for y in (4, 5, 10, 11):
        for x in (4, 5, 10, 11):
            t.set(x, y, (0, 0, 0, 0))
    return t


def woods():
    for w, bark, wood, leafp, blossom in (('lullwood', LULL_BARK, LULL_WOOD, LULL_LEAF, hx('#fff4a8')),
                                          ('wishwood', WISH_BARK, WISH_WOOD, WISH_LEAF, hx('#ffe89a'))):
        seed = 70 if w == 'lullwood' else 80
        out(f'block/{w}_log', log_side(bark, seed))
        out(f'block/{w}_log_top', log_top(bark, wood, seed))
        out(f'block/stripped_{w}_log', log_side(wood[1:], seed + 1))
        out(f'block/stripped_{w}_log_top', log_top(wood[:3], wood, seed + 1))
        out(f'block/{w}_planks', planks(wood, seed + 2))
        out(f'block/{w}_leaves', leaves(leafp, seed + 3, blossom if w == 'wishwood' else None), LEAVES_META)
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
                       ('drift_petals_stem', S.DRIFT_PETALS_STEM), ('dream_snare', S.DREAM_SNARE)):
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
    for i in range(4):
        t = Tex()
        rnd = random.Random(90 + i)
        size = 4 + i * 3
        for y in range(16 - size, 16):
            for x in range(8 - size // 2, 8 + size // 2):
                if rnd.random() < 0.55:
                    t.set(x, y, hx('#3f9d80') if rnd.random() < 0.5 else hx('#5bbf95'))
        if i >= 2:
            for _ in range(2 if i == 2 else 4):
                x, y = rnd.randrange(8 - size // 2, 8 + size // 2 - 1), rnd.randrange(16 - size, 14)
                t.set(x, y, hx('#7fe3e6')); t.set(x + 1, y, hx('#c2f7f3')); t.set(x, y + 1, hx('#4fb9c4')); t.set(x + 1, y + 1, hx('#7fe3e6'))
        out(f'block/pitcher_bulb_bush_stage{i}', t, CUTOUT)


# ================================================================== functional blocks


def functional():
    drum_side = Tex()
    for y in range(16):
        for x in range(16):
            c = LULL_WOOD[2] if (x // 2) % 2 == 0 else LULL_WOOD[1]
            if y in (3, 12):
                c = hx('#d9a94a')
            if y in (4, 11) and x % 4 == 0:
                c = hx('#fff0b0')
            drum_side.set(x, y, c)
    for i in range(16):
        drum_side.set(i, 7 + (i % 4 in (1, 2)), hx('#e6d6ff'))
    out('block/sift_drum_side', drum_side)
    core = drum_side.copy()
    for y in range(5, 11):
        for x in range(5, 11):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d < 3.5:
                core.set(x, y, hx('#1ec8c8') if d < 1.8 else hx('#0f3945'))
    out('block/sift_drum_side_core', core)
    top = Tex()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = hx('#f3e6f5') if d < 6 else hx('#caa7d9') if d < 7 else hx('#8f6aa8')
            if 2.5 < d < 3.3:
                c = hx('#d8c2e8')
            top.set(x, y, c)
    out('block/sift_drum_top', top)
    beat = top.copy()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6:
                beat.set(x, y, lerp(hx('#7fe8ff'), hx('#ff9fd8'), d / 6))
    out('block/sift_drum_top_beat', beat)
    rim = Tex()
    for y in range(16):
        for x in range(16):
            rim.set(x, y, hx('#d9a94a') if (x + y) % 4 else hx('#fff0b0'))
    out('block/sift_drum_rim', rim)
    out('block/sift_drum_bottom', planks(LULL_WOOD, 99))
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
    # echo frame
    ef = bricks(HUSH[1:], HUSH_MORTAR, 111, rows=2, width=8)
    for i in range(16):
        ef.set(i, 7, hx('#1ec8c8')); ef.set(i, 15, hx('#0f3945'))
        if i % 4 == 2:
            ef.set(i, 3, hx('#5ff5f0'))
    out('block/echo_frame_side', ef)
    eft = polished(HUSH, 112)
    for i in range(3, 13):
        eft.set(i, 7, hx('#1ec8c8')); eft.set(7, i, hx('#1ec8c8'))
    eft.set(7, 7, hx('#dffffc'))
    out('block/echo_frame_top', eft)
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
    # chrome fluid: animated shimmering still + flow textures and an overlay
    frames = 32
    st = Image.new('RGBA', (16, 16 * frames))
    sp = st.load()
    for f in range(frames):
        t = f / frames * math.tau
        for y in range(16):
            for x in range(16):
                c = iridescent(x, y, t, 0.4)
                sp[x, y + f * 16] = (c[0], c[1], c[2], 235)
    out('block/chrome_still', st, {'animation': {'frametime': 3}})
    fl = Image.new('RGBA', (32, 32 * frames))
    fp = fl.load()
    for f in range(frames):
        t = f / frames * math.tau
        for y in range(32):
            for x in range(32):
                c = iridescent(x * 0.5, (y - f) * 0.5, t, 0.5)
                fp[x, y + f * 32] = (c[0], c[1], c[2], 235)
    out('block/chrome_flow', fl, {'animation': {'frametime': 2}})
    ov = Tex()
    for y in range(16):
        for x in range(16):
            c = iridescent(x, y, 0.0, 0.3)
            ov.set(x, y, with_alpha(c, 200))
    out('block/chrome_overlay', ov)
    misc = Image.new('RGBA', (64, 64))
    mp = misc.load()
    for y in range(64):
        for x in range(64):
            c = iridescent(x * 0.25, y * 0.25, 0.0, 0.3)
            mp[x, y] = (c[0], c[1], c[2], 150)
    out('misc/in_chrome', misc)


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
    out('item/thick_hide', pal_sprite(S.HIDE, {'d': hx('#4f6f8a'), 'm': hx('#6f94ad'), 'l': hx('#9fc4d6'), 's': hx('#c9b1ee')}))
    out('item/star_shard', pal_sprite(S.STAR_SHARD, {'W': hx('#fffbe0'), 'l': hx('#ffe89a'), 'm': hx('#d9a94a')}))
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
    tools = {'sword': S.SWORD, 'pickaxe': S.PICKAXE, 'axe': S.AXE, 'shovel': S.SHOVEL, 'hoe': S.HOE, 'spear': S.SPEAR}
    for t, rows in tools.items():
        k = tool_keys(SIFTITE, '#4a3a6e')
        img = pal_sprite(rows, k)
        # a pink glint along the edge, like light on Siftite
        for y in range(16):
            for x in range(16):
                if img.get(x, y)[:3] == SIFTITE[-1][:3] and (x + y) % 4 == 0:
                    img.set(x, y, SIFTITE_PINK)
        out(f'item/siftite_{t}', img)
    out('item/siftite_spear_in_hand', pal_sprite(S.SPEAR, tool_keys(SIFTITE, '#4a3a6e')))
    for a, rows in (('helmet', S.HELMET), ('chestplate', S.CHESTPLATE), ('leggings', S.LEGGINGS), ('boots', S.BOOTS)):
        k = tool_keys(SIFTITE)
        img = pal_sprite(rows, k)
        for y in range(16):
            for x in range(16):
                if img.get(x, y)[:3] == SIFTITE[-3][:3] and (x * 3 + y) % 7 == 0:
                    img.set(x, y, SIFTITE_PINK)
        out(f'item/siftite_{a}', img)
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
    eggs = {'bulb': ('#7fe3e6', '#ff9ccf'), 'slumbler': ('#9fd6e2', '#6f8fd0'), 'sifter': ('#58cbe3', '#ffd23f'),
            'enchoer': ('#3fb8b0', '#a9f5ff'), 'riveter': ('#dcd7ea', '#1ec8c8')}
    for mob, (b, s) in eggs.items():
        out(f'item/{mob}_spawn_egg', pal_sprite(S.EGG, {'b': hx(b), 's': hx(s), 'd': darken(hx(b), 0.45)}))


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

    for i in range(4):
        t = blob(8, hx('#ffffff'), lambda d, i=i: max(0.0, 1 - d) ** (0.6 + i * 0.2))
        t.set(3, 3 - (i % 2), hx('#ffffff'))
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
    for i in range(2):
        out(f'particle/sift_mist_{i}', blob(16, hx('#ffffff'), lambda d, i=i: max(0.0, 1 - d) ** (1.5 + i)))
    for i in range(2):
        st = Tex(8, 8)
        arms = 3 - i
        for k in range(-arms, arms + 1):
            st.set(3 + k, 3, hx('#ffffff')); st.set(3, 3 + k, hx('#ffffff'))
        st.set(3, 3, hx('#ffffff'))
        out(f'particle/star_sparkle_{i}', st)
    for i in range(3):
        t = blob(8, hx('#ffffff'), lambda d, i=i: max(0.0, 1 - d * (1.2 - i * 0.15)))
        out(f'particle/portal_soul_{i}', t)
    for i in range(3):
        out(f'particle/footstep_puff_{i}', blob(8, hx('#ffffff'), lambda d, i=i: 0.8 if (d < 0.9 - i * 0.2) else 0))
    for i in range(2):
        out(f'particle/glow_splat_{i}', blob(4 + i * 2, hx('#ffffff'), lambda d: 1.0 if d < 0.85 else 0))
    ws = Tex(16, 16)
    for k in range(-7, 8):
        a = int(255 * (1 - abs(k) / 8))
        ws.set(7 + k, 7, with_alpha(hx('#ffffff'), a)); ws.set(7, 7 + k, with_alpha(hx('#ffffff'), a))
    for k in range(-3, 4):
        a = int(200 * (1 - abs(k) / 4))
        ws.set(7 + k, 7 + k, with_alpha(hx('#ffffff'), a)); ws.set(7 + k, 7 - k, with_alpha(hx('#ffffff'), a))
    out('particle/wishing_star', ws)
    for i in range(2):
        sp = Tex(8, 8)
        draw_map(sp, ['..w..', '.wWw.', 'wWWWw', '.wWw.', '..w..'] if i == 0 else ['.w.w.', 'wWWWw', '.WWW.', 'wWWWw', '.w.w.'],
                 {'w': with_alpha(hx('#ffffff'), 160), 'W': hx('#ffffff')}, 1, 1)
        out(f'particle/sleep_spore_{i}', sp)
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
        n1 = fbm3(d)
        n2 = fbm3(d[..., [1, 2, 0]] * 1.3)
        n3 = fbm3(d[..., [2, 0, 1]] * 0.8)
        col = np.broadcast_to(deep, d.shape).copy()
        c1 = np.clip((n1 - 0.45) * 3.0, 0, 1)[..., None]
        c2 = np.clip((n2 - 0.5) * 3.2, 0, 1)[..., None]
        c3 = np.clip((n3 - 0.5) * 2.5, 0, 1)[..., None]
        col = col * (1 - c3 * 0.7) + purple * c3 * 0.7
        col = col * (1 - c1 * 0.75) + cyan * c1 * 0.75
        col = col * (1 - c2 * 0.7) + pink * c2 * 0.7
        # soft aurora ribbon around the horizon
        horizon = np.exp(-((d[..., 1] - 0.12) ** 2) / 0.02)[..., None]
        wave = (0.5 + 0.5 * np.sin(np.arctan2(d[..., 2], d[..., 0]) * 5 + n1 * 6))[..., None]
        col = col + (cyan * wave + pink * (1 - wave)) * horizon * 0.25
        # below the horizon the sky fades into the Sift's pink haze
        below = np.clip(-d[..., 1] * 1.5, 0, 1)[..., None]
        col = col * (1 - below) + np.array([0.55, 0.42, 0.7]) * below
        # stars
        star = rng.random(d.shape[:2])
        bright = (star > 0.9965).astype(np.float32)[..., None]
        col = col + bright * np.array([1.0, 0.97, 0.9]) * (0.6 + 0.4 * rng.random(d.shape[:2]))[..., None]
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


def main():
    stone_textures()
    soils()
    woods()
    flora()
    functional()
    items()
    armor_layers()
    particles()
    nebula()
    logo()
    need = os.path.join(ROOT, 'build/textures_needed.txt')
    if os.path.exists(need):
        missing = [n for n in open(need).read().split() if not os.path.exists(os.path.join(TEX, n + '.png'))]
        if missing:
            print('MISSING TEXTURES:', ' '.join(missing))
            sys.exit(1)
    print('textures ok')


if __name__ == '__main__':
    main()
