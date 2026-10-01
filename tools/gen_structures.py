"""Builds The Sift's exploration structures (NBT templates) and their worldgen JSON."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_assets as GA  # noqa: E402
from structlib import AIR, B, Build, chest, sigil, spawner, stairs, suspicious  # noqa: E402

NS = 'thesift'
OUT = os.path.join(GA.RES, 'data', NS, 'structure')
D = os.path.join(GA.RES, 'data', NS, 'worldgen')

# ------------------------------------------------------------------ palette
BLUSH = B('blush_bricks')
BLUSH_CR = B('cracked_blush_bricks')
BLUSH_CH = B('chiseled_blush_bricks')
DS_BR = B('dreamstone_bricks')
DS_CR = B('cracked_dreamstone_bricks')
DS_MOSS = B('mossy_dreamstone_bricks')
DS_POL = B('polished_dreamstone')
DS_TILE = B('dreamstone_tiles')
DS_CH = B('chiseled_dreamstone')
DS = B('dreamstone')
DS_COB = B('cobbled_dreamstone')
PILLAR = B('dreamstone_pillar', axis='y')
LULL_PL = B('lullwood_planks')
WISH_PL = B('wishwood_planks')
HUSH_BR = B('hushslate_bricks')
HUSH_CR = B('cracked_hushslate_bricks')
HUSH_TILE = B('hushslate_tiles')
HUSH_POL = B('polished_hushslate')
HUSH_CH = B('chiseled_hushslate')
SANDSTONE = B('dreamsandstone')
CUT_SS = B('cut_dreamsandstone')
SAND = B('dreamsand')
CHROME = B('chrome', level=0)
GLASS = B('chrome_glass')
MOSS_CARPET = B('lumen_moss_carpet')
HANG = B('hanging_lullwood_leaves', tip='false')
LANTERN = B('bulb_lantern', hanging='false', waterlogged='false')
LANTERN_H = B('bulb_lantern', hanging='true', waterlogged='false')
CHIME = B('soul_chime', powered='false')
DRUM = B('sift_drum', hit='0', core='false', powered='false')
ALTAR = B('euphory_altar')
SNARE = B('dream_snare', spent='false')
CRUMBLE = B('crumbling_dreamstone')
SEAL = B('harmony_seal')
FRAME = B('echo_frame')
SENSOR = B('minecraft:sculk_sensor', power=0, sculk_sensor_phase='inactive', waterlogged='false')
SCULK = B('minecraft:sculk')
FLOWERS = [B('lullaby_bell', resonating='false'), B('dreambloom', resonating='false'), B('soulpetal', resonating='false'),
           B('nebula_iris', resonating='false'), B('blushgrass'), B('blushgrass'), B('coral_fern'), B('glimmer_sprouts')]
TONE_PEDESTAL = [B('minecraft:light_blue_concrete'), B('minecraft:pink_concrete'), B('minecraft:yellow_concrete'), B('minecraft:purple_concrete')]


def harmony(key, tone=None):
    return B('harmony_stone', key=key, tone=(key + 1) % 4 if tone is None else tone)


def glyph(i):
    return B('glyph_stone', glyph=i % 8)


def pot(flower):
    return B(f'potted_{flower}')


def harmony_puzzle(b: Build, spots, seed):
    """Places harmony stones on colour-coded pedestals. Each stone must be tuned to its pedestal's colour."""
    for i, (x, y, z) in enumerate(spots):
        key = (seed + i * 3) % 4
        b.set(x, y - 1, z, TONE_PEDESTAL[key])
        b.set(x, y, z, harmony(key))


# ================================================================== collapsed / intact towers


def tower(seed, collapsed, roof):
    H = 26 + seed % 5
    R = 5
    size = 15
    b = Build(size, H + 12, size, seed)
    c = size // 2
    b.disc(c, c, 0, R + 1.4, DS_POL)
    wall = b.mix((BLUSH, 14), (BLUSH_CR, 3), (DS_BR, 1))
    b.cyl(c, c, 1, H, R, BLUSH, hollow=True, pick=wall)
    for y in range(1, H + 1):
        if y % 7 == 0:
            b.cyl(c, c, y, y, R + 0.6, DS_BR, hollow=True, pick=b.mix((DS_BR, 6), (DS_MOSS, 3), (DS_CR, 1)))
    floors = [f for f in range(7, H, 7)]
    for f in floors:
        b.disc(c, c, f, R - 0.6, LULL_PL, pick=b.mix((LULL_PL, 8), (CRUMBLE if f == floors[len(floors) // 2] else LULL_PL, 2)))
    # spiral stair hugging the wall
    for y in range(1, H):
        ang = y * math.pi / 4.0
        sx = c + round(math.cos(ang) * (R - 1.5))
        sz = c + round(math.sin(ang) * (R - 1.5))
        b.set(sx, y, sz, B('lullwood_planks'))
        for yy in range(y + 1, y + 4):
            if b.get(sx, yy, sz) is not None and b.get(sx, yy, sz).name == 'thesift:lullwood_planks' and yy not in floors:
                b.set(sx, yy, sz, AIR)
        # cut a hole in each floor above the stair so it can be climbed
        if y + 1 in floors:
            b.set(sx, y + 1, sz, AIR)
            b.set(sx, y + 2, sz, AIR)
    # door and windows
    for y in (1, 2, 3):
        b.set(c, y, c + R, AIR)
    b.set(c, 4, c + R, BLUSH_CH)
    for f in floors:
        for dx, dz in ((R, 0), (-R, 0), (0, -R)):
            for y in (f + 2, f + 3):
                b.set(c + dx, y, c + dz, GLASS)
            b.set(c + dx, f + 4, c + dz, DS_CH)
    # roof: pink conical spire like the Sift's old watchtowers
    if roof:
        for i, r in enumerate([R + 1.2, R + 0.4, R - 0.4, R - 1.2, R - 2.0, R - 2.8, R - 3.5, R - 4.2]):
            y = H + 1 + i * 1
            b.cyl(c, c, y, y, max(0.4, r), WISH_PL, hollow=r > 1.5, pick=b.mix((WISH_PL, 6), (B('wishwood_slab', type='top', waterlogged='false'), 1)))
        b.set(c, H + 9, c, B('wishwood_fence', east='false', north='false', south='false', west='false', waterlogged='false'))
        b.set(c, H + 10, c, LANTERN)
        b.set(c, H + 0, c, CHIME)
    # top room: treasure sealed behind harmony seals in the floor
    top = floors[-1]
    b.set(c, top + 1, c, chest('chests/tower_top'))
    b.set(c + 1, top + 1, c, pot('nebula_iris'))
    b.set(c - 2, top + 1, c + 1, SNARE)
    sealed_floor = floors[-2] if len(floors) >= 2 else floors[-1]
    for (x, z) in [(c + round(math.cos(a) * (R - 1.5)), c + round(math.sin(a) * (R - 1.5))) for a in [((top - 1) * math.pi / 4.0)]]:
        b.set(x, top, z, SEAL)
    harmony_puzzle(b, [(c - 2, sealed_floor + 2, c - 1), (c + 2, sealed_floor + 2, c - 1), (c, sealed_floor + 2, c + 2)], seed)
    b.set(c, sealed_floor + 1, c - 2, glyph(seed))
    # interior life
    for f in [0] + floors:
        for _ in range(4):
            x, z = c + b.rnd.randint(-3, 3), c + b.rnd.randint(-3, 3)
            if b.get(x, f + 1, z) is None or b.get(x, f + 1, z).name == AIR.name:
                b.set(x, f + 1, z, MOSS_CARPET)
    b.set(c + 2, floors[0] + 1, c + 2, LANTERN)
    # the Whistler's chicks nest in the tower; the Whistler itself answers at the door of the roofed ones
    b.set(c - 1, floors[0] + 1, c + 1, spawner('whistling', 2, 4, 10))
    if roof or seed % 2 == 0:
        b.set(c, 0, c + R + 1, sigil(1))
    if collapsed:
        # shear the top off at a jagged, tilted line and pile rubble around the base
        cut = max(top + 3, H - 5) - seed % 2
        for (x, y, z), blk in list(b.blocks.items()):
            tilt = (x - c) * 0.35 + math.sin(z * 1.3 + seed) * 1.2
            if y > cut + tilt:
                b.blocks.pop((x, y, z))
        for _ in range(70):
            x, z = b.rnd.randrange(size), b.rnd.randrange(size)
            if math.hypot(x - c, z - c) > R + 0.5:
                b.set(x, 1, z, b.rnd.choice([BLUSH, BLUSH_CR, DS_COB, B('blush_brick_slab', type='bottom', waterlogged='false')]))
        b.decay(0.03, top_bias=0.08, protect=('minecraft:chest', 'thesift:harmony_stone', 'thesift:harmony_seal', 'minecraft:spawner'), min_y=2)
    else:
        b.decay(0.015, top_bias=0.02, protect=('minecraft:chest', 'thesift:harmony_stone', 'thesift:harmony_seal'), min_y=2)
    b.drape(HANG.with_(tip='false'), 0.03, 4)
    b.overgrow(['polished_dreamstone'], FLOWERS, 0.25)
    return b


# ================================================================== musical temple


def temple(seed):
    S = 27
    V = 6  # vault depth below the platform
    b = Build(S, V + 16, S, seed)
    c = S // 2
    y0 = V
    b.fill(1, y0, 1, S - 2, y0, S - 2, DS_TILE, pick=b.mix((DS_TILE, 10), (DS_CR, 2), (DS_POL, 2)))
    b.fill(0, y0 - 1, 0, S - 1, y0 - 1, S - 1, DS_BR)
    for i in range(S):
        for j in (0, S - 1):
            b.set(i, y0, j, DS_POL)
            b.set(j, y0, i, DS_POL)
    # ring of pillars, some snapped
    for k in range(12):
        a = k * math.tau / 12
        px, pz = c + round(math.cos(a) * 10), c + round(math.sin(a) * 10)
        h = 9 if k % 3 else 5 + seed % 3
        for y in range(y0 + 1, y0 + 1 + h):
            b.set(px, y, pz, PILLAR)
        b.set(px, y0 + 1 + h, pz, DS_CH if h == 9 else DS_CR)
    # architraves joining neighbouring intact pillars
    for k in range(0, 12, 3):
        for t in range(1, 10):
            a = (k + t / 10) * math.tau / 12
            x, z = c + round(math.cos(a) * 10), c + round(math.sin(a) * 10)
            b.set(x, y0 + 11, z, B('dreamstone_brick_slab', type='bottom', waterlogged='false'))
    # raised dais with the Euphory Altar and its drums
    b.disc(c, c, y0 + 1, 4.4, DS_POL)
    b.set(c, y0 + 2, c, ALTAR)
    for k in range(6):
        a = k * math.tau / 6
        b.set(c + round(math.cos(a) * 3), y0 + 2, c + round(math.sin(a) * 3), DRUM)
    # harmony puzzle guards the vault hatch
    harmony_puzzle(b, [(c - 6, y0 + 2, c + 2), (c + 6, y0 + 2, c + 2), (c - 4, y0 + 2, c - 5), (c + 4, y0 + 2, c - 5)], seed)
    for x in range(c - 1, c + 2):
        for z in range(c + 5, c + 7):
            b.set(x, y0, z, SEAL)
            b.set(x, y0 - 1, z, AIR)
    # vault
    b.fill(c - 5, 0, c + 2, c + 5, V - 1, c + 10, HUSH_BR, hollow=True)
    b.fill(c - 4, 1, c + 3, c + 4, V - 2, c + 9, AIR)
    for i, z in enumerate(range(c + 5, c + 8)):
        b.set(c, V - 2 - i, z, stairs('dreamstone_brick_stairs', 'north'))
    b.set(c, 1, c + 9, chest('chests/temple_vault', 'north'))
    b.set(c - 3, 1, c + 9, CHIME)
    b.set(c + 3, 1, c + 9, LANTERN)
    for x in range(c - 4, c + 5):
        b.set(x, V - 2, c + 10, glyph(x + seed))
    b.set(c - 2, 1, c + 6, CRUMBLE)
    b.set(c - 3, 1, c + 4, spawner('thumpling', 2, 4, 10))
    b.set(c, y0, c - 7, sigil(0))
    # trap snares on the stairs up to the dais
    for (x, z) in ((c - 2, c + 12), (c + 2, c + 12), (c + 12, c)):
        b.set(x, y0 + 1, z, SNARE)
    b.decay(0.05, top_bias=0.06, protect=('minecraft:chest', 'thesift:harmony_stone', 'thesift:harmony_seal', 'thesift:euphory_altar',
                                          'thesift:encore_sigil', 'minecraft:spawner'),
            min_y=y0 + 2)
    b.overgrow(['dreamstone_tiles', 'polished_dreamstone', 'cracked_dreamstone_bricks'], FLOWERS, 0.12)
    return b


# ================================================================== chrome well


def chrome_well(seed):
    b = Build(11, 12, 11, seed)
    c = 5
    D_ = 5
    b.cyl(c, c, 0, D_, 3, DS_POL, hollow=True, pick=b.mix((DS_POL, 6), (DS_MOSS, 2), (DS_CR, 1)))
    b.cyl(c, c, 1, D_, 2.2, CHROME)
    b.cyl(c, c, D_ + 1, D_ + 1, 3, DS_CH, hollow=True)
    for dx, dz in ((-3, -3), (3, -3), (-3, 3), (3, 3)):
        for y in range(D_ + 1, D_ + 5):
            b.set(c + dx, y, c + dz, B('wishwood_log', axis='y'))
    b.fill(c - 3, D_ + 5, c - 3, c + 3, D_ + 5, c + 3, B('wishwood_slab', type='bottom', waterlogged='false'))
    b.set(c, D_ + 4, c, CHIME)
    for dx in range(-5, 6):
        for dz in range(-5, 6):
            d = math.hypot(dx, dz)
            if 3.6 < d < 5.4:
                b.set(c + dx, D_, c + dz, suspicious('archaeology/sift_rare' if b.rnd.random() < 0.15 else 'archaeology/sift_common')
                      if b.rnd.random() < 0.3 else SAND)
    b.overgrow(['dreamsand'], [B('blushgrass'), B('chrome_reeds', age=0)], 0.1)
    return b


# ================================================================== abandoned altar


def abandoned_altar(seed):
    b = Build(13, 7, 13, seed)
    c = 6
    b.disc(c, c, 0, 6, DS_TILE, pick=b.mix((DS_TILE, 6), (DS_CR, 3), (DS_MOSS, 3), (SAND, 1)))
    b.disc(c, c, 1, 2.5, DS_POL)
    b.set(c, 2, c, ALTAR)
    for k in range(8):
        a = k * math.tau / 8
        x, z = c + round(math.cos(a) * 4), c + round(math.sin(a) * 4)
        if k % 2 == 0 or seed % 2:
            b.set(x, 1, z, DRUM)
        else:
            b.set(x, 1, z, DS_CR)
    for k in range(4):
        a = k * math.tau / 4 + 0.4
        b.set(c + round(math.cos(a) * 6), 1, c + round(math.sin(a) * 6), glyph(seed + k))
    b.set(c + 1, 1, c - 3, chest('chests/sift_ruins', 'south'))
    b.set(c - 2, 1, c + 3, pot('soulpetal'))
    b.decay(0.06, min_y=1, protect=('thesift:euphory_altar', 'minecraft:chest'))
    b.overgrow(['dreamstone_tiles', 'mossy_dreamstone_bricks', 'cracked_dreamstone_bricks'], FLOWERS, 0.3)
    return b


# ================================================================== giant stone instruments


def giant_harp(seed):
    b = Build(19, 26, 7, seed)
    z = 3
    b.fill(0, 0, 1, 18, 1, 5, DS_POL)
    # curved neck and pillar
    for y in range(2, 24):
        t = (y - 2) / 21
        x = round(2 + 2.5 * math.sin(t * math.pi))
        b.fill(x, y, z - 1, x + 1, y, z + 1, DS_BR, pick=b.mix((DS_BR, 6), (DS_MOSS, 2), (DS_CR, 1)))
    for x in range(2, 17):
        t = (x - 2) / 14
        y = round(23 - 9 * t + 3 * math.sin(t * math.pi))
        b.fill(x, y, z - 1, x, y + 1, z + 1, BLUSH, pick=b.mix((BLUSH, 5), (BLUSH_CR, 2)))
    for y in range(2, 14):
        b.fill(16, y, z - 1, 17, y, z + 1, DS_BR)
    # strings of chain down from the neck to the soundboard
    for x in range(5, 16, 2):
        top = max(y for (xx, y, zz) in b.blocks if xx == x and zz == z) - 1
        for y in range(2, top):
            b.set(x, y, z, B('minecraft:iron_chain', axis='y', waterlogged='false'))
    for x in (4, 9, 14):
        b.set(x, 2, z + 2, CHIME)
    b.set(9, 2, z - 2, DRUM)
    # the harp's strings are home to a Strumling brood, and their webs
    b.set(12, 2, z - 2, spawner('strumling', 3, 6, 10))
    for (x, y) in ((6, 6), (8, 9), (11, 7), (13, 4), (7, 3)):
        b.set(x, y, z, B('minecraft:cobweb'))
    b.decay(0.02, top_bias=0.05, min_y=3, protect=('minecraft:iron_chain', 'minecraft:spawner', 'minecraft:cobweb'))
    b.drape(HANG, 0.02, 3)
    return b


def giant_drum(seed):
    b = Build(15, 9, 15, seed)
    c = 7
    b.cyl(c, c, 0, 5, 6, B('stripped_lullwood_log', axis='y'), hollow=True)
    b.cyl(c, c, 1, 1, 6.2, B('minecraft:yellow_terracotta'), hollow=True)
    b.cyl(c, c, 4, 4, 6.2, B('minecraft:yellow_terracotta'), hollow=True)
    b.disc(c, c, 6, 5.6, DRUM)
    for k in range(8):
        a = k * math.tau / 8
        b.set(c + round(math.cos(a) * 6.4), 6, c + round(math.sin(a) * 6.4), B('lullwood_planks'))
    b.set(c, 1, c, chest('chests/sift_ruins'))
    b.set(c + 2, 1, c, spawner('thumpling', 2, 4, 10))
    b.decay(0.03, min_y=1, protect=('minecraft:chest', 'minecraft:spawner'))
    return b


# ================================================================== ruined bridge


def bridge(seed):
    L = 33
    b = Build(L, 16, 7, seed)
    for x in range(L):
        t = x / (L - 1)
        y = round(10 + 4 * math.sin(t * math.pi))
        b.fill(x, y - 1, 1, x, y, 5, DS_BR, pick=b.mix((DS_BR, 8), (DS_MOSS, 3), (DS_CR, 2)))
        b.set(x, y + 1, 1, BLUSH)
        b.set(x, y + 1, 5, BLUSH)
        if x % 4 == 0:
            b.set(x, y + 2, 1, LANTERN)
    for px in (6, 16, 26):
        top = max(y for (x, y, z) in b.blocks if x == px)
        b.fill(px - 1, 0, 2, px + 1, top - 2, 4, DS_BR, pick=b.mix((DS_BR, 6), (DS_MOSS, 3)))
    # the middle span has fallen away
    gap0, gap1 = 12 + seed % 3, 20 - seed % 3
    for (x, y, z) in list(b.blocks):
        if gap0 <= x <= gap1 and y > 4:
            b.blocks.pop((x, y, z))
    for _ in range(25):
        b.set(b.rnd.randint(gap0, gap1), 0, b.rnd.randint(0, 6), b.rnd.choice([DS_COB, DS_BR, BLUSH]))
    b.drape(HANG, 0.05, 4)
    return b


# ================================================================== buried settlement


def settlement(seed):
    S = 23
    b = Build(S, 11, S, seed)
    houses = [(2, 2, 8, 7), (12, 3, 9, 8), (4, 12, 10, 8)]
    for i, (hx, hz, w, d) in enumerate(houses):
        h = 6
        b.fill(hx, 0, hz, hx + w - 1, 0, hz + d - 1, CUT_SS)
        b.fill(hx, 1, hz, hx + w - 1, h, hz + d - 1, BLUSH, hollow=True, pick=b.mix((BLUSH, 6), (SANDSTONE, 3), (BLUSH_CR, 2)))
        b.fill(hx + 1, 1, hz + 1, hx + w - 2, h - 1, hz + d - 2, AIR)
        # sand has drifted in; suspicious sand hides the settlement's belongings
        for x in range(hx + 1, hx + w - 1):
            for z in range(hz + 1, hz + d - 1):
                depth = 1 + b.rnd.randrange(3)
                for y in range(1, depth + 1):
                    r = b.rnd.random()
                    b.set(x, y, z, suspicious('archaeology/sift_rare') if r < 0.03 else suspicious('archaeology/sift_common') if r < 0.12 else SAND)
        b.set(hx + 1, 2 + 1, hz + 1, chest('chests/sift_ruins', 'south'))
        b.set(hx + w - 2, 4, hz + d - 2, B('minecraft:decorated_pot', facing='north', cracked='false', waterlogged='false'))
        for y in (2, 3):
            b.set(hx + w // 2, y, hz, AIR)
        b.set(hx + 2, 4, hz + d - 1, GLASS)
        b.set(hx + w // 2, h - 1, hz + d // 2, LANTERN_H)
    b.set(10, 1, 10, B('minecraft:bell', attachment='floor', facing='north', powered='false')) if seed % 2 else None
    b.decay(0.08, top_bias=0.25, protect=('minecraft:chest',), min_y=5)
    return b


# ================================================================== statues


def enchoer_statue(seed):
    b = Build(11, 24, 9, seed)
    c = 5
    b.fill(1, 0, 1, 9, 1, 7, DS_POL)
    b.fill(2, 2, 2, 8, 2, 6, DS_CH)
    mat = b.mix((DS, 8), (DS_POL, 2), (DS_MOSS, 1))
    b.fill(c - 2, 3, 3, c - 1, 9, 5, DS, pick=mat)   # legs
    b.fill(c + 1, 3, 3, c + 2, 9, 5, DS, pick=mat)
    b.fill(c - 3, 10, 3, c + 3, 16, 5, DS, pick=mat)  # torso
    b.fill(c - 4, 11, 4, c - 4, 16, 6, BLUSH)         # folded wings
    b.fill(c + 4, 11, 4, c + 4, 16, 6, BLUSH)
    b.fill(c - 2, 17, 3, c + 2, 20, 6, DS, pick=mat)  # head
    b.set(c - 1, 19, 3, GLASS)
    b.set(c + 1, 19, 3, GLASS)
    for s in (-1, 1):  # antler crest
        for i in range(4):
            b.set(c + s * (2 + i // 2), 21 + i, 4, DS_POL)
        b.set(c + s * 4, 23, 4, B('minecraft:end_rod', facing='up'))
    b.set(c, 13, 2, B('minecraft:amethyst_block'))
    for x in (2, 8):
        b.set(x, 2, 1, pot('dreambloom'))
    b.set(c, 2, 1, pot('lullaby_bell'))
    b.decay(0.02, top_bias=0.03, min_y=3)
    b.drape(HANG, 0.02, 3)
    return b


def bulb_statue(seed):
    b = Build(9, 15, 9, seed)
    c = 4
    b.fill(0, 0, 0, 8, 0, 8, DS_POL)
    b.cyl(c, c, 1, 4, 3.4, B('minecraft:light_blue_terracotta'))
    b.cyl(c, c, 5, 7, 2.6, B('minecraft:light_blue_terracotta'))
    b.set(c - 1, 6, c - 3, B('minecraft:black_terracotta'))
    b.set(c + 1, 6, c - 3, B('minecraft:black_terracotta'))
    for s in (-1, 1):
        for y in range(8, 13):
            b.set(c + s, y, c, B('minecraft:light_blue_terracotta') if y < 12 else B('minecraft:pink_terracotta'))
    b.set(c, 9, c - 1, B('minecraft:glowstone'))
    b.set(c, 1, c - 4, chest('chests/sift_ruins', 'north')) if seed % 3 == 0 else None
    b.overgrow(['polished_dreamstone'], FLOWERS, 0.3)
    return b


# ================================================================== deep sift shrine


def deep_shrine(seed, boss=False):
    S = 17
    H = 11
    b = Build(S, H, S, seed)
    b.fill(0, 0, 0, S - 1, H - 1, S - 1, HUSH_BR, hollow=True, pick=b.mix((HUSH_BR, 8), (HUSH_CR, 3), (SCULK, 2)))
    b.fill(1, 0, 1, S - 2, 0, S - 2, HUSH_TILE, pick=b.mix((HUSH_TILE, 8), (SCULK, 2)))
    c = S // 2
    # an Echo Frame gate on the north wall: the way home
    for dx in range(-2, 3):
        for dy in range(1, 7):
            edge = abs(dx) == 2 or dy in (1, 6)
            b.set(c + dx, dy, 2, FRAME if edge else AIR)
    b.set(c, 1, 5, DRUM)
    for (x, z) in ((c - 3, 4), (c + 3, 4), (c - 3, 7), (c + 3, 7)):
        b.set(x, 1, z, SENSOR)
    b.set(c, 1, S - 3, chest('chests/deep_shrine', 'north'))
    for x in (2, S - 3):
        for z in (2, S - 3):
            for y in range(1, H - 1):
                b.set(x, y, z, HUSH_CH if y % 3 == 0 else HUSH_POL)
    for x in range(3, S - 3, 3):
        b.set(x, H - 2, c, LANTERN_H)
    b.set(c - 4, 1, S - 4, B('minecraft:sculk_shrieker', can_summon='true', shrieking='false', waterlogged='false'))
    b.set(c + 4, 1, S - 4, B('minecraft:sculk_catalyst', bloom='false'))
    for i in range(5):
        b.set(c - 2 + i, H - 1, c, glyph(seed + i))
    # the Strummer's lair: webs in the corners, a brood spawner, and in some shrines its sigil
    for x in (3, S - 4):
        for z in (3, S - 4):
            for (dx, dy, dz) in ((0, H - 2, 0), (1, H - 2, 0), (0, H - 2, 1), (0, H - 3, 0)):
                b.set(x + dx * (1 if x < c else -1), dy, z + dz * (1 if z < c else -1), B('minecraft:cobweb'))
    b.set(c + 4, 1, c, spawner('strumling', 3, 6, 10))
    if boss:
        b.set(c, 0, c + 2, sigil(2))
    b.decay(0.02, min_y=2, protect=('thesift:echo_frame', 'minecraft:chest', 'thesift:sift_drum', 'minecraft:sculk_sensor', 'minecraft:spawner',
                                     'minecraft:cobweb'))
    return b


# ================================================================== scattered ruins


def ruins(seed):
    S = 19
    b = Build(S, 9, S, seed)
    c = S // 2
    for x in range(2, S - 2):
        for z in range(2, S - 2):
            if b.rnd.random() < 0.8:
                b.set(x, 0, z, BLUSH if (x + z) % 4 == 0 else DS_TILE)
    walls = [((2, 2), (S - 3, 2)), ((2, 2), (2, S - 6)), ((S - 3, 5), (S - 3, S - 3))]
    for (x0, z0), (x1, z1) in walls:
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                h = 1 + b.rnd.randrange(5)
                for y in range(1, h + 1):
                    b.set(x, y, z, b.rnd.choice([DS_BR, DS_BR, DS_MOSS, DS_CR]))
    # a half-standing arch
    for y in range(1, 7):
        b.set(c - 3, y, S - 4, PILLAR)
        b.set(c + 3, y, S - 4, PILLAR)
    for x in range(c - 3, c + 4):
        b.set(x, 7, S - 4, DS_CH if x == c else DS_BR)
    b.set(c, 1, c, chest('chests/sift_ruins', 'south'))
    if seed % 2 == 0:
        b.set(c + 2, 1, c - 2, spawner(('thumpling', 'whistling', 'strumling')[seed % 3], 2, 4, 9))
    for _ in range(6):
        b.set(b.rnd.randrange(3, S - 3), 0, b.rnd.randrange(3, S - 3), suspicious('archaeology/sift_common'))
    b.decay(0.06, top_bias=0.2, min_y=2, protect=('minecraft:chest', 'minecraft:spawner'))
    b.overgrow(['dreamstone_tiles', 'blush_bricks', 'mossy_dreamstone_bricks'], FLOWERS, 0.3)
    b.drape(HANG, 0.03, 2)
    return b


# ================================================================== the Dictator's Sculk Castle


def sculk_castle(seed):
    """A tall tower of hushslate and sculk. Inside, a spiral of jump-apart steps climbs the wall
    past checkpoint landings to a hatch in the roof; on the roof is the arena, ringed with spiked
    battlements, and the Conductor's Podium where the Dictator waits. A slime-block floor catches
    anyone who falls."""
    S, H = 27, 72
    c = S // 2
    R = 10                      # outer wall radius
    STEPS = 58
    TOP = 2 + STEPS             # roof / arena floor level
    b = Build(S, H, S, seed)
    DT = B('minecraft:deepslate_tiles')
    wall = b.mix((HUSH_BR, 9), (HUSH_CR, 3), (DT, 2), (SCULK, 2))
    SOUL_L = B('minecraft:soul_lantern', hanging='false', waterlogged='false')
    SLIME = B('minecraft:slime_block')
    # foundation plinth and a sculk-stained skirt
    b.disc(c, c, 0, 13, HUSH_TILE, pick=b.mix((HUSH_TILE, 8), (SCULK, 3), (DT, 2)))
    b.cyl(c, c, 1, 2, 12.4, HUSH_BR, hollow=True, thickness=1.0, pick=wall)
    # the tower
    b.cyl(c, c, 1, TOP, R, HUSH_BR, hollow=True, thickness=2.0, pick=wall)
    for y in range(6, TOP, 10):
        b.cyl(c, c, y, y, R + 0.4, HUSH_CH, hollow=True, thickness=1.0)
    # a soft landing: slime blocks across the ground floor around a sculk catalyst
    b.disc(c, c, 0, R - 2, SLIME)
    b.set(c, 0, c, B('minecraft:sculk_catalyst', bloom='false'))
    # the central pillar, veined with glowing moss
    b.cyl(c, c, 1, TOP - 1, 1.2, HUSH_POL, pick=lambda x, y, z: B('lumen_moss_block') if y % 7 == 3 else HUSH_POL)
    # entrance on the south side
    for dx in range(-1, 2):
        for y in range(1, 5):
            for z in range(c + R - 2, c + R + 1):
                b.set(c + dx, y, z, AIR)
    for dx in (-2, 2):
        for y in range(1, 6):
            b.set(c + dx, y, c + R, HUSH_CH)
    for dx in range(-2, 3):
        b.set(c + dx, 5, c + R, HUSH_CH)
    # window slits
    for k in range(8):
        a = k * math.pi / 4 + 0.2
        for y0 in range(10 + (k % 3) * 4, TOP - 4, 13):
            for dy in range(3):
                for rr in (R - 1, R):
                    b.set(c + round(math.cos(a) * rr), y0 + dy, c + round(math.sin(a) * rr), AIR)
    # the spiral of steps: 16 per turn, one block up each, ~1.7 blocks of air between them
    a0 = math.pi / 2 + 0.6
    last = []
    for i in range(STEPS):
        a = a0 + i * (2 * math.pi / 16)
        y = 1 + i
        landing = i > 0 and i % 16 == 0
        for rr in (6.6, 7.6):
            x, z = c + round(math.cos(a) * rr), c + round(math.sin(a) * rr)
            blk = HUSH_TILE
            if not landing and i % 7 == 5 and i > 8:
                blk = CRUMBLE
            b.set(x, y, z, blk)
        if landing:
            # a checkpoint: a wider ledge with a lantern, a sensor and a small supply chest
            for da in (-0.14, 0.0, 0.14):
                for rr in (5.6, 6.6, 7.6):
                    b.set(c + round(math.cos(a + da) * rr), y, c + round(math.sin(a + da) * rr), HUSH_POL)
            b.set(c + round(math.cos(a + 0.14) * 7.6), y + 1, c + round(math.sin(a + 0.14) * 7.6), SOUL_L)
            b.set(c + round(math.cos(a - 0.14) * 7.6), y + 1, c + round(math.sin(a - 0.14) * 7.6), chest('chests/sculk_castle_landing', 'north'))
        if i >= STEPS - 3:
            last.append(a)
    # the roof: the arena floor, with a hatch above the last steps
    b.disc(c, c, TOP, 12, HUSH_TILE, pick=lambda x, y, z: HUSH_CH if abs(math.hypot(x - c, z - c) - 5.0) < 0.6 else
           HUSH_POL if math.hypot(x - c, z - c) < 2.6 else HUSH_TILE)
    for a in last:
        for rr in (5.6, 6.6, 7.6):
            b.set(c + round(math.cos(a) * rr), TOP, c + round(math.sin(a) * rr), AIR)
    # battlements with sculk spikes
    for k in range(48):
        a = k * 2 * math.pi / 48
        x, z = c + round(math.cos(a) * 12), c + round(math.sin(a) * 12)
        b.set(x, TOP + 1, z, HUSH_BR)
        if k % 2 == 0:
            b.set(x, TOP + 2, z, HUSH_BR)
        if k % 6 == 0:
            for dy in range(3, 8):
                b.set(x, TOP + dy, z, SCULK if dy < 6 else B('minecraft:deepslate_tile_wall', up='true', north='none', south='none', east='none',
                                                                  west='none', waterlogged='false'))
    # eight pillars around the arena, crowned with soul lanterns and sensors
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        x, z = c + round(math.cos(a) * 9), c + round(math.sin(a) * 9)
        for dy in range(1, 6):
            b.set(x, TOP + dy, z, HUSH_CH if dy in (1, 5) else HUSH_POL)
        b.set(x, TOP + 6, z, SOUL_L if k % 2 else SENSOR)
    # the Grand Stage: a raised round stage, the podium at its heart and three Instrument Altars
    # waiting for the drum, the flute and the guitar; the vault chest beside it
    b.disc(c, c, TOP + 1, 5.4, HUSH_POL, pick=lambda x, y, z: HUSH_CH if math.hypot(x - c, z - c) > 4.6 else
           B('lumen_moss_block') if abs(math.hypot(x - c, z - c) - 2.0) < 0.5 else HUSH_POL)
    for k in range(3):
        a = math.pi / 2 + k * math.tau / 3
        ax, az = c + round(math.cos(a) * 3.6), c + round(math.sin(a) * 3.6)
        b.set(ax, TOP + 2, az, B('instrument_altar'))
    for k in range(6):
        a = k * math.tau / 6 + math.pi / 6
        b.set(c + round(math.cos(a) * 5.0), TOP + 2, c + round(math.sin(a) * 5.0), SOUL_L if k % 2 else DRUM)
    b.set(c, TOP + 2, c, B('conductors_podium'))
    b.set(c + 7, TOP + 1, c, chest('chests/sculk_castle', 'west'))
    b.decay(0.015, min_y=3, protect=('thesift:conductors_podium', 'thesift:instrument_altar', 'thesift:sift_drum', 'minecraft:chest', 'thesift:hushslate_tiles', 'thesift:polished_hushslate',
                                      'thesift:crumbling_dreamstone', 'minecraft:slime_block', 'minecraft:soul_lantern'))
    return b


# ================================================================== registration

STRUCTURES = {
    # name: (builders, biomes tag values, step, adaptation, start_height, spacing, separation, heightmap)
    'collapsed_tower': ([lambda s: tower(s, True, False), lambda s: tower(s + 7, True, False), lambda s: tower(s + 3, False, True)],
                        ['sift_plains', 'forest_mountains', 'wishing_grove'], 'surface_structures', 'beard_thin', 0, 30, 10),
    'musical_temple': ([temple, lambda s: temple(s + 5)], ['sift_plains', 'wishing_grove'], 'surface_structures', 'beard_thin', -6, 44, 14),
    'chrome_well': ([chrome_well, lambda s: chrome_well(s + 4)], ['sift_plains', 'rocky_dunes', 'chrome_lakes'], 'surface_structures', 'beard_thin', -5, 22,
                    7),
    'abandoned_altar': ([abandoned_altar, lambda s: abandoned_altar(s + 1)], ['forest_mountains', 'sift_plains', 'wishing_grove'], 'surface_structures',
                        'beard_thin', -1, 26, 8),
    'stone_instrument': ([giant_harp, giant_drum], ['sift_plains', 'rocky_dunes', 'forest_mountains'], 'surface_structures', 'beard_thin', 0, 34, 10),
    'ruined_bridge': ([bridge, lambda s: bridge(s + 2)], ['chrome_lakes', 'sift_plains'], 'surface_structures', 'beard_thin', -2, 32, 10),
    'buried_settlement': ([settlement, lambda s: settlement(s + 9)], ['rocky_dunes'], 'underground_structures', 'bury', -7, 28, 9),
    'dream_statue': ([enchoer_statue, bulb_statue], ['sift_plains', 'wishing_grove', 'forest_mountains', 'rocky_dunes'], 'surface_structures', 'beard_thin',
                     0, 28, 9),
    'deep_shrine': ([deep_shrine, lambda s: deep_shrine(s + 3, True)], ['deep_sift'], 'underground_structures', 'beard_box', None, 24, 8),
    'sculk_castle': ([sculk_castle], ['rocky_dunes', 'forest_mountains', 'sift_plains'], 'surface_structures', 'beard_thin', 0, 40, 16),
    'sift_ruins': ([ruins, lambda s: ruins(s + 11), lambda s: ruins(s + 23)], ['sift_plains', 'forest_mountains', 'wishing_grove', 'rocky_dunes'],
                   'surface_structures', 'beard_thin', -1, 18, 6),
}


def generate():
    total = 0
    for idx, (name, (builders, biomes, step, adapt, start_y, spacing, sep)) in enumerate(STRUCTURES.items()):
        os.makedirs(os.path.join(OUT, name), exist_ok=True)
        elements = []
        for i, fn in enumerate(builders):
            b = fn(1000 + idx * 37 + i * 11)
            total += b.save(os.path.join(OUT, name, f'{name}_{i}.nbt'))
            elements.append({'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{name}/{name}_{i}',
                                         'processors': 'minecraft:empty', 'projection': 'rigid'}, 'weight': 1})
        GA.write(os.path.join(D, 'template_pool', name, 'start.json'), {'elements': elements, 'fallback': 'minecraft:empty'})
        for bio in biomes:
            GA.tag('worldgen/biome', f'{NS}:has_structure/{name}', f'{NS}:{bio}')
        s = {'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/{name}', 'max_distance_from_center': 80, 'size': 1, 'spawn_overrides': {},
             'start_pool': f'{NS}:{name}/start', 'step': step, 'terrain_adaptation': adapt, 'use_expansion_hack': False}
        if start_y is None:
            s['start_height'] = {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -40}, 'max_inclusive': {'absolute': 0}}
        else:
            s['start_height'] = {'absolute': start_y}
            s['project_start_to_heightmap'] = 'WORLD_SURFACE_WG'
        GA.write(os.path.join(D, 'structure', name + '.json'), s)
        GA.write(os.path.join(D, 'structure_set', name + '.json'), {
            'placement': {'type': 'minecraft:random_spread', 'salt': 77310000 + idx * 7919, 'separation': sep, 'spacing': spacing},
            'structures': [{'structure': f'{NS}:{name}', 'weight': 1}]})
    print(f'structures ok: {len(STRUCTURES)} structures, {total} blocks')


if __name__ == '__main__':
    generate()
    GA.flush_tags()
