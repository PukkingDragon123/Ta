"""C: the Caravans Cavern - its Caravans (nomadic gem-crusted alien crabs, CR2), their Queen (a vast,
fat hermit crab) and the larvae that hatch from egg-laden ore, their music crystals, the Prism gem
with its ore, block and armour.

Hooks (one line each): gen_assets.gen_block (model 'music_crystal'), gen_assets.generate (sounds),
gen_data.generate (lang, loot, recipes, tags, equipment), gen_world (features, biome, biome-source
entry), mobs.ALL (the Caravan, Queen and larva models), gen_textures.main (block, armour textures)
and items16.all_items (item sprites)."""
import colorsys
import math
import os
import random

from PIL import Image

NS = 'thesift'

# colour id -> (chitin, chitin light, chitin dark, crystal, crystal light, crystal dark); order matches CrystalColor.java
COLORS = {
    'amber': ('#c8741e', '#e8a040', '#7a3c10', '#ffb43a', '#ffe08a', '#c0681a'),
    'rose': ('#c8506e', '#ec7a94', '#7a2440', '#ff7fae', '#ffc6dc', '#c03a72'),
    'teal': ('#2a9a8e', '#4ec8b8', '#145a54', '#4ff0dc', '#bafff4', '#1aa89a'),
    'violet': ('#7a4ac0', '#a072e4', '#40226e', '#b98cff', '#e6d4ff', '#7440d0'),
    'gold': ('#d8b030', '#f8dc60', '#8a6a14', '#ffe45a', '#fff8c0', '#d0a020'),
}
RAINBOW = ['#ff5a7a', '#ffa04a', '#ffe45a', '#6ff07a', '#4fd8ff', '#8a7aff', '#e070ff']
FACINGS = {'up': {}, 'down': {'x': 180}, 'north': {'x': 90}, 'south': {'x': 90, 'y': 180}, 'west': {'x': 90, 'y': 270},
           'east': {'x': 90, 'y': 90}}
ARMOR = ['helmet', 'chestplate', 'leggings', 'boots']


def _hx(c, a=255):
    c = c.lstrip('#')
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)


def _mix(a, b, t):
    a, b = _hx(a) if isinstance(a, str) else a, _hx(b) if isinstance(b, str) else b
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))


def _hue(h, s=0.55, v=1.0, a=255):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return (round(r * 255), round(g * 255), round(b * 255), a)


# ============================================================================ block textures

def crystal_tex(col, seed):
    """One music crystal's facet texture: vertical facets running dark to light across the face, a
    bright edge on each facet, a long glint streak and glassy translucency (more see-through at
    the core, near-opaque at the facet edges)."""
    _, _, _, c, cl, cd = COLORS[col]
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            facet = x // 4
            t = (x % 4) / 3.0
            base = _mix(cd, c, 0.35 + 0.5 * t) if facet % 2 == 0 else _mix(c, cl, 0.2 + 0.5 * t)
            if x % 4 == 0:
                base = _mix(base, '#ffffff', 0.35)
            if (x + y * 2 + seed) % 11 == 0:
                base = _mix(base, cl, 0.6)
            if rnd.random() < 0.05:
                base = _mix(base, '#ffffff', 0.5)
            a = 235 if x % 4 in (0, 3) else 190
            px[x, y] = base[:3] + (a,)
    for k in range(9):  # the glint streak, top left to bottom right
        x, y = 2 + k // 2, 1 + k
        if y < 16:
            px[x, y] = (255, 255, 255, 245)
    return img


def frozen_tex(col, seed):
    """A frozen crystal: clear, pale ice of the colour, a frosted rim and inner fracture lines,
    so the treasure locked inside shows through."""
    _, _, _, c, cl, cd = COLORS[col]
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            rim = x in (1, 14) or y in (1, 14)
            if edge:
                px[x, y] = _mix(cl, '#ffffff', 0.4)[:3] + (235,)
            elif rim:
                px[x, y] = _mix(c, cl, 0.6)[:3] + (190,)
            else:
                px[x, y] = _mix(cl, c, 0.25 + 0.1 * rnd.random())[:3] + (92,)
    for (x0, y0, dx, dy, n) in ((3, 4, 1, 1, 6), (12, 3, -1, 1, 5), (5, 12, 1, -1, 4)):
        for k in range(n):
            x, y = x0 + dx * k, y0 + dy * k
            if 1 < x < 15 and 1 < y < 15:
                px[x, y] = (255, 255, 255, 200)
    return img


def _stone(name):
    import gen_textures as GT
    p = os.path.join(GT.TEX, 'block', name + '.png')
    if os.path.exists(p):
        return Image.open(p).convert('RGBA')
    return Image.new('RGBA', (16, 16), _hx('#5a6a7a'))


def _hexagon(px, cx, cy, r, shift=0.0, alpha=255):
    """Paints a small flat-topped hexagonal gem: six facets around a pale table, each facet one
    colour of the rainbow, dark rim."""
    s3 = math.sqrt(3)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if abs(dy) <= r * s3 / 2 and s3 * abs(dx) + abs(dy) <= s3 * r:
                outer = abs(dy) > r * s3 / 2 - 0.9 or s3 * abs(dx) + abs(dy) > s3 * r - 1.4
                ang = (math.atan2(dy, dx) / math.tau + 1.0 + shift) % 1.0
                c = _hue(ang, 0.62, 1.0)
                if math.hypot(dx, dy) < r * 0.38:
                    c = _mix(c, '#ffffff', 0.7)
                elif dy < 0 and dx < 0:
                    c = _mix(c, '#ffffff', 0.25)
                if outer:
                    c = _mix(c, '#20143a', 0.55)
                px[x, y] = c[:3] + (alpha,)


def ore_tex(stone):
    img = _stone(stone)
    px = img.load()
    for (cx, cy, r, sh) in ((5.0, 5.0, 2.6, 0.0), (11.5, 10.5, 2.9, 0.3), (4.5, 12.5, 1.8, 0.6), (12.0, 3.5, 1.6, 0.8)):
        _hexagon(px, cx, cy, r, sh)
    return img


def prism_block_tex():
    """Polished prism: a honeycomb of hexagonal gem tiles set in pale silver."""
    img = Image.new('RGBA', (16, 16), _hx('#d8dcf0'))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                px[x, y] = _hx('#9a9cc0')
    for (cx, cy, sh) in ((4.0, 4.5, 0.0), (12.0, 4.5, 0.17), (8.0, 11.5, 0.33), (0.0, 11.5, 0.5), (16.0, 11.5, 0.66)):
        _hexagon(px, cx, cy, 3.6, sh)
    return img


def armor_layers():
    """The worn Prism armour: pale silver plates set with hexagonal rainbow gems; the gems shift
    hue across the body like light through a prism."""
    def boxfaces(u, v, w, h, d):
        return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'west': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
                'east': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}

    def plate(px, rect, gems=True):
        fx, fy, fw, fh = rect
        for y in range(fh):
            for x in range(fw):
                if y == 0 or x == 0:
                    c = _hx('#f4f6ff')
                elif y == fh - 1 or x == fw - 1:
                    c = _hx('#7a7ca8')
                elif gems and (x + 2 * y) % 6 == 0:
                    c = _hue((fx + x + fy + y) / 40.0, 0.6, 1.0)
                elif gems and (x + 2 * y) % 6 == 1:
                    c = _hue((fx + x + fy + y) / 40.0, 0.35, 0.85)
                else:
                    c = _hx('#cfd3ec') if y > fh // 3 else _hx('#e4e8fa')
                px[fx + x, fy + y] = c

    hum = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    hp = hum.load()
    for r in boxfaces(0, 0, 8, 8, 8).values():
        plate(hp, r)
    for (x, y) in ((9, 10), (10, 10), (13, 10), (14, 10)):
        hp[x, y] = (0, 0, 0, 0)
    for y in range(12, 16):
        for x in range(10, 14):
            hp[x, y] = (0, 0, 0, 0)
    for r in boxfaces(16, 16, 8, 12, 4).values():
        plate(hp, r)
    # a big hexagonal gem on the breastplate
    gem = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    _hexagon(gem.load(), 8.0, 8.0, 3.4)
    hum.alpha_composite(gem.crop((4, 4, 12, 12)), (20, 22))
    for (u, v) in ((40, 16), (0, 16)):
        for r in boxfaces(u, v, 4, 12, 4).values():
            plate(hp, r)
    leg = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    lp = leg.load()
    for r in boxfaces(16, 16, 8, 12, 4).values():
        plate(lp, r)
    for r in boxfaces(0, 16, 4, 12, 4).values():
        plate(lp, r)
    return {'entity/equipment/humanoid/prism': hum, 'entity/equipment/humanoid_leggings/prism': leg}


EGG = {'rim': '#8a6a52', 'shell': '#f2dcc0', 'shell_l': '#fff8ec', 'heart': '#ffb43a', 'heart_l': '#ffe08a', 'hollow': '#1c1426'}
# where the eggs sit in the socket: three clusters of two or three (egg centre x, y)
EGG_CLUSTERS = (((4.0, 4.2), (6.4, 5.6), (3.9, 6.9)), ((10.6, 9.4), (12.6, 10.9), (10.4, 12.0)), ((3.6, 11.6), (5.9, 12.9)))


def _paint_eggs(px, alpha=255):
    """The eggs a Caravan packs into an ore socket: dark hollows cut in the rock, pearly ovals in
    them with a warm glowing heart and a lit top-left edge, specks of gem grit round the rim.
    `px[x, y]` takes and returns RGBA tuples."""
    for cluster in EGG_CLUSTERS:
        cx = sum(e[0] for e in cluster) / len(cluster)
        cy = sum(e[1] for e in cluster) / len(cluster)
        for y in range(16):
            for x in range(16):
                d = math.hypot((x + 0.5 - cx) / 2.9, (y + 0.5 - cy) / 2.6)
                if d < 1.0:
                    px[x, y] = _mix(px[x, y], EGG['hollow'], 0.62 if d < 0.8 else 0.35)
    for cluster in EGG_CLUSTERS:
        for (ex, ey) in cluster:
            for y in range(16):
                for x in range(16):
                    u, v = (x + 0.5 - ex) / 1.3, (y + 0.5 - ey) / 1.55
                    r = u * u + v * v
                    if r > 1.0:
                        continue
                    if r > 0.6:
                        c = EGG['rim'] if (u > 0 or v > 0) else EGG['shell']
                    elif u < -0.15 and v < -0.15:
                        c = EGG['shell_l']
                    elif r < 0.22:
                        c = EGG['heart_l'] if v < 0 else EGG['heart']
                    else:
                        c = EGG['shell']
                    px[x, y] = _hx(c)[:3] + (alpha,)
    for i, (x, y) in enumerate(((8, 3), (1, 9), (14, 7), (8, 14), (13, 14))):
        px[x, y] = _hue(i / 5.0, 0.55, 1.0)


def egg_ore_tex(stone):
    """Egg-laden Ore before the vanilla remap (and if the vanilla textures are missing): the eggs on the generated rock."""
    img = _stone(stone)
    _paint_eggs(img.load())
    return img


def _egg_ore_remap(name, ref, stone):
    """vanilla_remap builder for Egg-laden Ore: the vanilla rock (stone or deepslate) in the Sift rock's
    palette, like the Sift's other ores, with the egg clusters painted over it."""
    import numpy as np
    import vanilla_remap as VR
    ro = VR._ref(ref)
    bo = VR._old(stone)
    mb = bo[..., 3] > 0
    out = VR._clone(ro, bo, masks=[np.ones((16, 16), bool)], pals=[VR.Pal(bo[mb & ~VR._accents(bo, mb)][:, :3])])
    img = Image.fromarray(np.clip(np.round(out), 0, 255).astype(np.uint8), 'RGBA').copy()
    _paint_eggs(img.load())
    return np.asarray(img, dtype=np.float64)


def textures(out):
    for i, col in enumerate(COLORS):
        out(f'block/music_crystal_{col}', crystal_tex(col, 31 + i))
        out(f'block/music_crystal_{col}_frozen', frozen_tex(col, 41 + i))
    out('block/prism_ore', ore_tex('dreamstone'))
    out('block/deep_prism_ore', ore_tex('hushslate'))
    out('block/prism_block', prism_block_tex())
    # CR2: Egg-laden Ore; the remap (run last) rebuilds the rock on vanilla stone / deepslate, like the other ores
    out('block/egg_laden_ore', egg_ore_tex('dreamstone'))
    out('block/deep_egg_laden_ore', egg_ore_tex('hushslate'))
    import vanilla_remap as VR
    VR.MAP['egg_laden_ore'] = VR.F(_egg_ore_remap, 'stone', 'dreamstone')
    VR.MAP['deep_egg_laden_ore'] = VR.F(_egg_ore_remap, 'deepslate', 'hushslate')
    for name, img in armor_layers().items():
        out(name, img)


# ============================================================================ item sprites

def prism_gem():
    """A flat-topped hexagonal gem cut into six rainbow facets around a white table, with a sparkle."""
    import items16 as I
    s3 = math.sqrt(3)
    keys = 'ABCDEF'
    rows = []
    for y in range(16):
        r = ''
        for x in range(16):
            dx, dy = x + 0.5 - 8.0, y + 0.5 - 8.5
            R = 6.4
            if abs(dy) <= R * s3 / 2 and s3 * abs(dx) + abs(dy) <= s3 * R:
                d = math.hypot(dx, dy)
                if d < 2.2:
                    r += 'w' if (dx < 0 and dy < 0) else 'W'
                else:
                    sector = int(((math.atan2(dy, dx) / math.tau) % 1.0) * 6) % 6
                    r += keys[sector].lower() if d > 4.6 else keys[sector]
            else:
                r += '.'
        rows.append(r)
    rows[1] = rows[1][:13] + 'S' + rows[1][14:]
    rows[0] = rows[0][:13] + 's' + rows[0][14:]
    rows[2] = rows[2][:13] + 's' + rows[2][14:]
    ol = '#2a1846'
    pal = {'w': ('#ffffff', ol), 'W': ('#f0ecff', ol), 'S': '#ffffff', 's': '#fff6c8'}
    hues = [0.0, 0.83, 0.66, 0.5, 0.33, 0.12]  # top-right round to bottom-right
    for k, h in zip(keys, hues):
        pal[k] = ('#%02x%02x%02x' % _hue(h, 0.5, 1.0)[:3], ol)
        pal[k.lower()] = ('#%02x%02x%02x' % _hue(h, 0.75, 0.82)[:3], ol)
    return I.grid(rows, pal, ol=True, no_ol='Ss')


def _prism_armor(piece):
    """The vanilla diamond armour silhouette, re-toned to pale silver with every gem pixel turned
    into a sliver of rainbow."""
    import items16 as I
    src = I.vanilla(f'diamond_{piece}')
    out = src.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255
            v = min(1.0, 0.2 + lum * 0.95)
            c = _mix('#2a2650', '#f4f4ff', v)
            if 0.45 < lum < 0.85 and (x * 2 + y) % 5 == 0:  # inlaid prism slivers, hue shifting across the piece
                c = _hue((x + y) / 20.0, 0.5, 0.95)
            px[x, y] = c[:3] + (a,)
    return out


def items():
    import items16 as I
    o = {'prism_gem': prism_gem(), 'caravan_pincer': caravan_pincer()}
    for p in ARMOR:
        o[f'prism_{p}'] = _prism_armor(p)
    # CR2: a gem-crusted crab shell, small dark eyes on stalks, claws out to the sides
    o['caravan_spawn_egg'] = I.egg(['#1c3438', '#28525a', '#3a7476', '#5a9a98'], '#0c1c1e', {
        1: '......k..k......', 2: '.....kKk.Kk.....', 3: '......KK.K......', 7: '....Ee....eE....', 8: '....ss....ss....'},
        pal={'k': ('#bafff4', '#1aa89a'), 'K': ('#4ff0dc', '#1aa89a'), 'e': '#4ff0dc', 'E': '#140c1c', 's': '#152e32', 'c': '#28525a',
             'C': '#3a7476'},
        under={9: '.cC..........Cc.', 10: '..c..........c..'}, no_ol='')
    o['caravan_spawn_egg'].putpixel((7, 4), (255, 255, 255, 255))
    # the Queen side on: a block of grey stone shell with the spiral worn into it and amber gems crusting the
    # top; her brown body leans out of its mouth, a small dark eye on a stalk, the big claw low in front,
    # the soft belly sagging under the shell and long legs out below
    o['caravan_queen_spawn_egg'] = I.egg(['#544c49', '#6a625e', '#7a716c', '#958b84'], '#2a2422', {
        0: '.........k......', 1: '.......k.kK..k..', 2: '......KK.KK.KK..', 3: '.......l........', 4: '......ddd.l.....',
        5: '..E..d...d......', 6: '..s..d.dd.d.....', 7: '..s.nd.d..d.....', 8: '.bbbnhd..d......', 9: 'bbbbnh.dd....l..',
        10: 'CCbbnnn.........', 11: 'CcCLppppppppL...', 12: '.CL.Lpppppp.L...', 13: '..L..L....L..L..', 14: '.L...L.....L..L.'},
        pal={'k': ('#ffe08a', '#c0681a'), 'K': ('#ffb43a', '#c0681a'), 'd': '#4a4240', 'l': '#a59b94', 'n': ('#bfa8b0', '#5a4a50'),
             'h': '#2a2024', 'E': '#140c08', 's': ('#4a3020', '#2a1a10'), 'b': ('#6a452e', '#2a1a10'), 'p': ('#b08a7a', '#5a3a30'),
             'L': ('#6a452e', '#2a1a10'), 'C': ('#7a5236', '#2a1a10'), 'c': ('#e3ddcc', '#2a1a10')}, no_ol='kK')
    # the larva: a brown grub of shelled segments with amber crust between them, a small dark eye and a gem nub
    o['caravan_larva_spawn_egg'] = I.egg(['#4a2e1a', '#6a4428', '#8a5a36', '#a87448'], '#22140a', {
        4: '.......k........', 5: '......kK..k.....', 6: '..mhhhhhhhhKh...', 7: '.mhEhHhHhHhHh...', 8: '..mhhhhhhhhhhh..',
        9: '...l.l.l.l.l.l..'},
        pal={'k': ('#ffe08a', '#c0681a'), 'K': ('#ffb43a', '#c0681a'), 'h': ('#7a5030', '#2a180a'), 'H': ('#c8741e', '#2a180a'),
             'E': '#140c08', 'm': '#e3ddcc', 'l': '#2a180a'}, no_ol='kK')
    return o


def caravan_pincer():
    """A Caravan's pincer: the claw of a gem-crab, its fixed finger and the hinged one gaping, pale
    chitin teeth along the bite, gem crust on the palm. Light from the top left like every vanilla item."""
    import items16 as I
    rows = ['................',
            '.........aab....',
            '........aABb....',
            '.......aABbt....',
            '.......aBbt.....',
            '......aBbt......',
            '.....aBkKb..tc..',
            '....aBkKwbtccb..',
            '...aBbKkbbbcCb..',
            '..aBbbbbbbcCb...',
            '..aBbgbbbbbb....',
            '.aBbgGgbbbb.....',
            '.abbbgbbbb......',
            '.abbbbbbb.......',
            '..abbbb.........',
            '................']
    ol = '#140c1c'
    pal = {'a': ('#152e32', ol), 'A': ('#5a9a98', ol), 'B': ('#4a8a88', ol), 'b': ('#28525a', ol), 't': ('#e3ddcc', ol),
           'c': ('#3a7476', ol), 'C': ('#5a9a98', ol), 'k': ('#1aa89a', ol), 'K': ('#4ff0dc', ol), 'w': ('#bafff4', ol),
           'g': ('#1aa89a', ol), 'G': ('#bafff4', ol)}
    return I.grid(rows, pal, ol=True)


# ============================================================================ block models (gen_assets.gen_block)

def gen_crystal(bid):
    """Music crystal models: a cluster of five tilted prisms (one per colour), and a chunky frozen
    block of clear crystal with two spikes, which the block-entity renderer fills with its treasure.
    Blockstates turn them to face away from whatever they grow on, like amethyst."""
    import gen_assets as GA
    el, write = GA.el, GA.write

    def f6(t):
        return {d: {'texture': t} for d in ('north', 'south', 'east', 'west', 'up', 'down')}

    cluster = [
        el([6, 0, 6], [10, 12, 10], f6('#c')),
        el([6.5, 12, 6.5], [9.5, 15, 9.5], f6('#c')),
        el([2.5, 0, 7], [5.5, 8, 10], f6('#c'), rotation={'origin': [5, 0, 8], 'axis': 'z', 'angle': 22.5}),
        el([10.5, 0, 3], [13.5, 9, 6], f6('#c'), rotation={'origin': [11, 0, 6], 'axis': 'x', 'angle': -22.5}),
        el([9, 0, 10.5], [12, 6, 13.5], f6('#c'), rotation={'origin': [10, 0, 10], 'axis': 'x', 'angle': 22.5}),
        el([3.5, 0, 2.5], [6, 5, 5], f6('#c'), rotation={'origin': [5, 0, 5], 'axis': 'z', 'angle': -22.5}),
    ]
    frozen = [
        el([2, 0, 2], [14, 13, 14], f6('#f')),
        el([3, 13, 3], [7, 16, 7], f6('#c')),
        el([9.5, 13, 9], [12.5, 15, 12], f6('#c')),
    ]
    variants = {}
    for col in COLORS:
        c = {'sprite': f'{NS}:block/music_crystal_{col}', 'force_translucent': True}
        f = {'sprite': f'{NS}:block/music_crystal_{col}_frozen', 'force_translucent': True}
        for name, els, tex in ((f'{bid}_{col}', cluster, {'c': c, 'particle': c}),
                               (f'{bid}_{col}_frozen', frozen, {'c': c, 'f': f, 'particle': f})):
            m = {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': tex, 'elements': els}
            GA.note_textures(m)
            write(os.path.join(GA.A, 'models/block', name + '.json'), m)
        for fz in ('false', 'true'):
            for facing, rot in FACINGS.items():
                variants[f'color={col},facing={facing},frozen={fz}'] = {'model': f'{NS}:block/{bid}_{col}' + ('_frozen' if fz == 'true' else ''), **rot}
    write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})
    # the item shows the colour it was mined as (drops keep their colour in a block_state component)
    write(os.path.join(GA.A, 'items', bid + '.json'), {'model': {
        'type': 'minecraft:select', 'property': 'minecraft:block_state', 'block_state_property': 'color',
        'cases': [{'when': col, 'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{bid}_{col}'}} for col in COLORS],
        'fallback': {'type': 'minecraft:model', 'model': f'{NS}:block/{bid}_rose'}}})


# ============================================================================ sounds (gen_assets.generate)

def sounds(GA):
    GA.SOUNDS.update({
        'entity.caravan.ambient': [('mob/silverfish/say1', 0.5, 1.6), ('mob/silverfish/say3', 0.5, 1.7), ('block/amethyst/resonate1', 0.3, 1.8)],
        'entity.caravan.hurt': [('mob/silverfish/hit1', 0.8, 1.3), ('mob/silverfish/hit2', 0.8, 1.35), ('block/amethyst_cluster/break1', 0.5, 1.6)],
        'entity.caravan.death': [('mob/silverfish/kill', 0.9, 1.1), ('block/amethyst/break1', 0.8, 1.4)],
        'entity.caravan.step': [('mob/silverfish/step1', 0.22, 1.4), ('mob/silverfish/step2', 0.22, 1.5), ('mob/silverfish/step3', 0.22, 1.45)],
        'entity.caravan.alarm': [('mob/silverfish/say2', 1.0, 0.7), ('mob/silverfish/say3', 1.0, 0.75), ('block/amethyst/resonate2', 0.6, 0.6)],
        'entity.caravan.build': [('block/amethyst_cluster/break2', 0.7, 1.3), ('block/amethyst/shimmer', 0.8, 1.2)],
        'block.music_crystal.chime': [('block/amethyst/resonate1', 0.9, 1.0), ('block/amethyst/resonate2', 0.9, 1.0),
                                      ('block/amethyst/resonate3', 0.9, 1.0)],
        'block.music_crystal.shatter': [('block/amethyst/break1', 1.0, 0.9), ('block/amethyst/shimmer', 1.0, 0.8)],
        # CR2: the territorial warning, the eggs going into an ore socket
        'entity.caravan.warn': [('event:block.note_block.hat', 0.9, 0.7), ('mob/silverfish/say2', 0.6, 1.3), ('block/amethyst_cluster/place1', 0.6, 0.9)],
        'entity.caravan.lay_eggs': [('mob/turtle/egg/drop_egg1', 0.8, 1.3), ('mob/turtle/egg/drop_egg2', 0.8, 1.4), ('block/amethyst/shimmer', 0.5, 1.4)],
        # CR2: the Queen, her spat gems and the larvae
        'entity.caravan_queen.ambient': [('mob/silverfish/say1', 0.9, 0.55), ('mob/silverfish/say3', 0.9, 0.5), ('block/amethyst/resonate2', 0.7, 0.6)],
        'entity.caravan_queen.hurt': [('mob/silverfish/hit1', 1.0, 0.6), ('mob/ravager/hurt1', 0.8, 1.25), ('block/amethyst_cluster/break2', 0.9, 0.8)],
        'entity.caravan_queen.death': [('mob/ravager/death1', 1.0, 1.15), ('block/amethyst/break1', 1.0, 0.6)],
        'entity.caravan_queen.step': [('mob/ravager/step1', 0.5, 1.3), ('mob/ravager/step2', 0.5, 1.35), ('mob/ravager/step4', 0.5, 1.3)],
        'entity.caravan_queen.spit': [('mob/llama/spit1', 1.0, 0.55), ('mob/llama/spit2', 1.0, 0.6), ('block/amethyst_cluster/break1', 0.7, 1.2)],
        'entity.caravan_queen.swat': [('entity/player/attack/sweep1', 1.0, 0.6), ('entity/player/attack/sweep3', 1.0, 0.55),
                                      ('block/amethyst_cluster/break3', 0.6, 0.7)],
        'entity.caravan_queen.feed': [('mob/sniffer/eat1', 1.0, 0.8), ('mob/sniffer/eat2', 1.0, 0.75), ('block/amethyst/break2', 0.5, 1.2)],
        'entity.caravan_queen.settle': [('mob/sniffer/longdig1', 1.0, 0.7), ('mob/sniffer/longdig2', 1.0, 0.65), ('block/amethyst/resonate3', 0.6, 0.6)],
        'entity.caravan_queen.roar': [('mob/ravager/stun1', 1.0, 1.2), ('mob/silverfish/say2', 1.0, 0.45)],
        'entity.caravan_queen.burst': [('block/amethyst_cluster/break4', 1.0, 0.6), ('block/amethyst/break1', 1.0, 0.5), ('block/amethyst/shimmer', 1.0, 0.7)],
        'entity.spat_gem.hit': [('block/amethyst/break2', 0.7, 1.5), ('block/amethyst/step5', 0.9, 1.4), ('block/amethyst_cluster/place2', 0.8, 1.5)],
        'entity.caravan_larva.ambient': [('mob/silverfish/say1', 0.4, 2.0), ('mob/silverfish/say4', 0.4, 1.9)],
        'entity.caravan_larva.hurt': [('mob/silverfish/hit1', 0.6, 1.8), ('mob/silverfish/hit3', 0.6, 1.9)],
        'entity.caravan_larva.death': [('mob/silverfish/kill', 0.7, 1.7), ('block/amethyst/break2', 0.4, 1.8)],
        'entity.caravan_larva.step': [('mob/silverfish/step1', 0.15, 1.8), ('mob/silverfish/step4', 0.15, 1.9)],
        'entity.caravan_larva.emerge': [('dig/stone1', 1.0, 0.9), ('mob/turtle/egg/egg_break1', 1.0, 1.3), ('mob/silverfish/say2', 0.6, 1.8)],
    })
    GA.SUBTITLES.update({
        'entity.caravan.ambient': 'Caravan chitters', 'entity.caravan.hurt': 'Caravan hurts', 'entity.caravan.death': 'Caravan dies',
        'entity.caravan.step': 'Caravan scuttles', 'entity.caravan.alarm': 'Caravans raise the alarm', 'entity.caravan.build': 'Music Crystal grows',
        'block.music_crystal.chime': 'Music Crystal rings', 'block.music_crystal.shatter': 'Frozen crystal shatters',
        'entity.caravan.warn': 'Caravan clacks its claws', 'entity.caravan.lay_eggs': 'Caravan lays eggs',
        'entity.caravan_queen.ambient': 'Caravan Queen clicks', 'entity.caravan_queen.hurt': 'Caravan Queen hurts',
        'entity.caravan_queen.death': 'Caravan Queen dies', 'entity.caravan_queen.step': 'Caravan Queen stomps',
        'entity.caravan_queen.spit': 'Caravan Queen spits gems', 'entity.caravan_queen.swat': 'Caravan Queen swats',
        'entity.caravan_queen.feed': 'Caravan Queen eats ore', 'entity.caravan_queen.settle': 'Caravan Queen settles in',
        'entity.caravan_queen.roar': 'Caravan Queen screams', 'entity.caravan_queen.burst': 'Caravan Queen bursts',
        'entity.spat_gem.hit': 'Gem clinks', 'entity.caravan_larva.ambient': 'Caravan larva chitters',
        'entity.caravan_larva.hurt': 'Caravan larva hurts', 'entity.caravan_larva.death': 'Caravan larva dies',
        'entity.caravan_larva.step': 'Caravan larva crawls', 'entity.caravan_larva.emerge': 'Caravan larva breaks out',
    })


# ============================================================================ data (gen_data.generate)

def generate():
    import gen_assets as GA
    import gen_data as GD
    D = GA.D
    # loot: crystals keep their colour; frozen treasure; caravans drop crystal shards of chitin
    GA.write(os.path.join(D, 'loot_table/blocks/music_crystal.json'), {
        'type': 'minecraft:block', 'random_sequence': f'{NS}:blocks/music_crystal', 'pools': [{
            'rolls': 1, 'condition': {'type': 'minecraft:survives_explosion'},
            'entries': [{'type': 'minecraft:item', 'name': f'{NS}:music_crystal',
                         'modifier': [{'type': 'minecraft:copy_state', 'block': f'{NS}:music_crystal', 'properties': ['color']}]}]}]})
    GD.table('chest', 'gameplay/frozen_crystal', [GD.pool([
        GD.item('prism_gem', 3), GD.item('minecraft:diamond', 6), GD.item('minecraft:emerald', 6), GD.item('minecraft:gold_ingot', 8, (1, 3)),
        GD.item('minecraft:lapis_lazuli', 6, (2, 6)), GD.item('minecraft:amethyst_shard', 8, (2, 5)), GD.item('siftite_dust', 4, (1, 3)),
        GD.item('minecraft:copper_ingot', 4, (2, 5)), GD.item('star_shard', 3), GD.item('skysong_gem', 1), GD.item('music_sheet_crystal', 2),
        GD.item('music_sheet_golem', 1), GD.item('music_sheet_lullaby', 1),  # W1: songs once kept in the ruins' vaults
        GD.item('minecraft:music_disc_otherside', 1), GD.item('minecraft:echo_shard', 2)])])
    GD.table('entity', 'entities/caravan', [
        GD.pool([GD.item('minecraft:amethyst_shard', count=(0, 2), extra=[GD.LOOTING])]),
        # CR2: a pincer (for the pickaxe that digs out three times the ore)
        GD.pool([GD.item('caravan_pincer', count=(0, 1), extra=[GD.LOOTING])]),
        GD.pool([GD.item('prism_gem')], condition={'type': 'minecraft:all_of', 'terms': [GD.PLAYER_KILL, {
            'type': 'minecraft:random_chance_with_enchanted_bonus', 'enchantment': 'minecraft:looting', 'unenchanted_chance': 0.025,
            'enchanted_chance': {'type': 'minecraft:linear', 'base': 0.035, 'per_level_above_first': 0.01}}]})])
    # CR2: the Queen's own drops (her shower of ores bursts out of her in CaravanQueen) and the larva's shell grit
    GD.table('entity', 'entities/caravan_queen', [
        GD.pool([GD.item('prism_gem', count=(2, 4), extra=[GD.LOOTING])]),
        GD.pool([GD.item('caravan_pincer', count=(2, 3))]),
        GD.pool([GD.item('minecraft:diamond', count=(1, 3)), GD.item('minecraft:emerald', count=(2, 4)), GD.item('music_sheet_crystal')],
                condition=GD.PLAYER_KILL)])
    GD.table('entity', 'entities/caravan_larva', [GD.pool([GD.item('minecraft:amethyst_shard', count=(0, 1))])])
    # recipes
    GA.shaped('prism_block', ['GGG', 'GGG', 'GGG'], {'G': 'prism_gem'}, 'prism_block')
    GA.shapeless('prism_gem_from_prism_block', ['prism_block'], 'prism_gem', count=9)
    for ore in ('prism_ore', 'deep_prism_ore'):
        GA.smelt(f'prism_gem_{ore}', ore, 'prism_gem', xp=1.2, kinds=('smelting', 'blasting'))
    shapes = {'helmet': ['GGG', 'G G'], 'chestplate': ['G G', 'GGG', 'GGG'], 'leggings': ['GGG', 'G G', 'G G'], 'boots': ['G G', 'G G']}
    for p, pat in shapes.items():
        GA.shaped(f'prism_{p}', pat, {'G': 'prism_gem'}, f'prism_{p}', category='equipment')
    # tags
    for p in ARMOR:
        GA.tag('item', f'minecraft:{"head" if p == "helmet" else "chest" if p == "chestplate" else "leg" if p == "leggings" else "foot"}_armor', f'{NS}:prism_{p}')
        GA.tag('item', f'{NS}:prism_armor', f'{NS}:prism_{p}')
    GA.tag('item', f'{NS}:repairs_prism_armor', f'{NS}:prism_gem')
    GA.tag('item', 'minecraft:beacon_payment_items', f'{NS}:prism_gem')
    GA.tag('block', 'minecraft:crystal_sound_blocks', f'{NS}:music_crystal')
    for ore in ('prism_ore', 'deep_prism_ore'):
        GA.tag('block', 'c:ores', f'{NS}:{ore}')
        GA.tag('item', 'c:ores', f'{NS}:{ore}')
        GA.tag('block', f'{NS}:prism_ores', f'{NS}:{ore}')
        GA.tag('item', f'{NS}:prism_ores', f'{NS}:{ore}')
    GA.tag('item', 'c:gems', f'{NS}:prism_gem')
    GA.tag('worldgen/biome', f'{NS}:is_sift', f'{NS}:caravans_cavern')
    # what Caravan workers dig out: natural ore only, embedded in natural rock
    for o in ['#minecraft:coal_ores', '#minecraft:iron_ores', '#minecraft:copper_ores', '#minecraft:gold_ores', '#minecraft:redstone_ores',
              '#minecraft:lapis_ores', '#minecraft:diamond_ores', '#minecraft:emerald_ores', f'{NS}:siftite_ore', f'{NS}:deep_siftite_ore',
              f'{NS}:prism_ore', f'{NS}:deep_prism_ore', 'minecraft:nether_quartz_ore', 'minecraft:amethyst_cluster']:
        GA.tag('block', f'{NS}:caravan_minable', o)
    for r in ['#minecraft:base_stone_overworld', f'#{NS}:sift_stone', 'minecraft:calcite', 'minecraft:amethyst_block', 'minecraft:budding_amethyst',
              'minecraft:smooth_basalt', 'minecraft:tuff', f'#{NS}:caravan_minable', f'{NS}:egg_laden_ore', f'{NS}:deep_egg_laden_ore']:
        GA.tag('block', f'{NS}:caravan_rock', r)
    # equipment asset
    GA.write(os.path.join(GA.A, 'equipment', 'prism.json'), {'layers': {
        'humanoid': [{'texture': f'{NS}:prism'}], 'humanoid_leggings': [{'texture': f'{NS}:prism'}]}})
    GA.TEXTURES.add('entity/equipment/humanoid/prism')
    GA.TEXTURES.add('entity/equipment/humanoid_leggings/prism')
    lang(GA)


def lang(GA):
    L = {
        f'entity.{NS}.caravan': 'Caravan',
        f'entity.{NS}.caravan_queen': 'Caravan Queen',
        f'entity.{NS}.caravan_larva': 'Caravan Larva',
        f'entity.{NS}.spat_gem': 'Spat Gem',
        f'band.{NS}.instrument.hymn_bells': 'Hymn Bells',
        f'band.{NS}.instrument.larva_clicks': 'Larva Clicks',
        f'biome.{NS}.caravans_cavern': 'Caravans Cavern',
        f'message.{NS}.caravan.calm': 'The Caravans hum along and calm down.',
        f'codex.{NS}.caravan.title': 'Caravan', f'codex.{NS}.caravan.tagline': 'Neutral - nomad gem crabs of the deep',
        f'codex.{NS}.caravan.body': 'Caravans are alien crabs, their shells crusted with gems of their caravan\'s colour - amber, rose, '
                                    'teal, violet or rare gold - a crust that grows as they age. They have no home: little columns of '
                                    'them roam the caves behind a leader, digging natural ore and crystal out of the rock (never your '
                                    'buildings) and carrying it to their Queen. In the socket the ore leaves they often lay their eggs. '
                                    'They leave you be - but come into their Queen\'s lair and they rear up and clack their claws; '
                                    'stay, and the whole caravan attacks. At rest they play together as a band: a melody, harmony, '
                                    'a bass from the big soldiers and a clicking of claws. Play the Crystal Hymn and they calm down '
                                    'and play along. They drop their pincers.',
        f'codex.{NS}.caravan_queen.title': 'Caravan Queen', f'codex.{NS}.caravan_queen.tagline': 'Territorial - a hermit crab of gems',
        f'codex.{NS}.caravan_queen.body': 'A vast, very fat hermit crab under a spiral shell crusted with gems - gems grow right out of '
                                          'her soft body too. She is rare: one Queen keeps a whole stretch of the caves. She settles in a '
                                          'cave of her own, her lair, and the Caravans around bring her the ore they dig; she eats it all, '
                                          'and music crystals grow up in her lair from it. Come into her lair and she warns you, then '
                                          'spits gems at you - real gems, they drop where they land - and swats you with her huge claw. '
                                          'Any other creature that strays in gets the same. Hurt her and she screams for her caravans. '
                                          'Slain, she bursts in a shower of ores.',
        f'codex.{NS}.caravan_larva.title': 'Caravan Larva', f'codex.{NS}.caravan_larva.tagline': 'Hostile - grubs in the ore',
        f'codex.{NS}.caravan_larva.body': 'Caravans lay their eggs in the sockets the ore they dig leaves behind, sealed in with a little '
                                          'rock: Egg-laden Ore, the eggs glowing through the stone. Mine it and a larva or two wriggles '
                                          'out - little shelled grubs in their caravan\'s colour that go straight for you, and bring the '
                                          'caravan if it sees. A silk-touched pickaxe lifts the eggs out whole. Left alone, a larva grows '
                                          'into a young Caravan.',
        f'codex.{NS}.egg_laden_ore.title': 'Egg-laden Ore', f'codex.{NS}.egg_laden_ore.tagline': 'Mine it at your peril',
        f'codex.{NS}.egg_laden_ore.body': 'Ore sockets packed with Caravan eggs: pearly ovals with a warm light inside, set in the rock '
                                          'where a Caravan dug its ore out. You find them in the Caravans Cavern and wherever Caravans '
                                          'have been mining. Break one and Caravan larvae come out fighting.',
        f'codex.{NS}.music_crystal.title': 'Music Crystals', f'codex.{NS}.music_crystal.tagline': 'Glowing, tuned, sometimes full',
        f'codex.{NS}.music_crystal.body': 'Every crystal rings its own note when you hit it or use it, and its colour sets the pitch: amber '
                                          'lowest, gold highest. Some are frozen: clear crystal blocks with a treasure spinning inside. '
                                          'They grow wild in the Caravans Cavern and in a Caravan Queen\'s lair from the ore she eats. '
                                          'Break one to set the treasure free - but Caravans swarm anyone who breaks their crystals, unless '
                                          'the Crystal Hymn has calmed them first.',
        f'codex.{NS}.prism.title': 'Prism', f'codex.{NS}.prism.tagline': 'The rainbow gem',
        f'codex.{NS}.prism.body': 'Hexagonal rainbow gems, the first treasure of the Sift: an iron pickaxe frees them. Small veins of Prism '
                                  'Ore run all through the Sift\'s caves and rich ones through the Caravans Cavern. Prism armour is a '
                                  'little tougher than diamond and drinks in music: wear two pieces or more and every note played near '
                                  'you heals you, the full set twice as fast. Prism also inlays the gem instruments and the Europhy Table.',
    }
    GA.LANG.update(L)


# ============================================================================ worldgen (gen_world)

def world(GW):
    """Features of the Caravans Cavern, a very rare Prism ore for the rest of the deep Sift, and the
    biome itself. Called before gen_world.biomes() so the deep-Sift ore joins COMMON_UNDERGROUND."""
    st, feature, placed, count, rarity, BIOME = GW.state, GW.feature, GW.placed, GW.count, GW.rarity, GW.BIOME
    sq = {'type': 'minecraft:in_square'}

    def height(lo, hi, kind='uniform'):
        return {'type': 'minecraft:height_range', 'height': {'type': f'minecraft:{kind}', 'max_inclusive': {'absolute': hi},
                                                            'min_inclusive': {'absolute': lo}}}

    def ore(name, size, pairs, air=0.0):
        return feature(name, {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': air, 'size': size, 'targets': [
            {'state': st(s), 'target': {'predicate_type': 'minecraft:block_match', 'block': GW.rl(b)}} for b, s in pairs]})

    # Prism (F1: the early/mid crystal): small veins all through the Sift's caves, and rich veins in the cavern
    ore('ore_prism_deep', 4, [('dreamstone', 'prism_ore'), ('hushslate', 'deep_prism_ore')], air=0.4)
    placed('ore_prism_deep', 'ore_prism_deep', [count(3), sq, height(-48, 64, 'trapezoid'), BIOME])
    GW.COMMON_UNDERGROUND.append((6, 'ore_prism_deep'))
    ore('ore_prism_cavern', 4, [('dreamstone', 'prism_ore'), ('hushslate', 'deep_prism_ore')], air=0.3)
    placed('ore_prism_cavern', 'ore_prism_cavern', [count(2), sq, height(-64, 40), BIOME])
    ore('ore_siftite_cavern', 4, [('dreamstone', 'siftite_ore'), ('hushslate', 'deep_siftite_ore')], air=0.5)
    placed('ore_siftite_cavern', 'ore_siftite_cavern', [rarity(2), sq, height(-64, 40), BIOME])
    # CR2: sockets Caravans have filled with their eggs, sealed into the rock
    ore('ore_egg_laden_cavern', 3, [('dreamstone', 'egg_laden_ore'), ('hushslate', 'deep_egg_laden_ore'),
                                    ('minecraft:stone', 'egg_laden_ore'), ('minecraft:deepslate', 'deep_egg_laden_ore')], air=0.0)
    placed('ore_egg_laden_cavern', 'ore_egg_laden_cavern', [count(5), sq, height(-60, 40), BIOME])
    for name, n, size, a, b in (('diamond', 3, 6, 'diamond_ore', 'deepslate_diamond_ore'), ('emerald', 4, 3, 'emerald_ore', 'deepslate_emerald_ore'),
                                ('gold', 4, 8, 'gold_ore', 'deepslate_gold_ore'), ('lapis', 3, 7, 'lapis_ore', 'deepslate_lapis_ore'),
                                ('redstone', 3, 7, 'redstone_ore', 'deepslate_redstone_ore')):
        ore(f'ore_{name}_cavern', size, [('dreamstone', f'minecraft:{a}'), ('hushslate', f'minecraft:{b}')])
        placed(f'ore_{name}_cavern', f'ore_{name}_cavern', [count(n), sq, height(-64, 48), BIOME])
    # music crystals on every floor and ceiling, in all five colours, over calcite and amethyst
    for surf, facing in (('floor', 'up'), ('ceiling', 'down')):
        feature(f'music_crystal_{surf}', {'type': 'minecraft:simple_block', 'to_place': GW.weighted(
            [(st('music_crystal', color=c, facing=facing, frozen=False), 3) for c in COLORS]
            + [(st('music_crystal', color=c, facing=facing, frozen=True), 1) for c in ('gold', 'violet')]
            + [(st('minecraft:amethyst_cluster', facing=facing), 2)])})
        feature(f'crystal_patch_{surf}', {
            'type': 'minecraft:vegetation_patch', 'depth': 1, 'extra_bottom_block_chance': 0.0, 'extra_edge_column_chance': 0.4,
            'ground_state': GW.weighted([(st('minecraft:calcite'), 4), (st('minecraft:amethyst_block'), 3), (st('minecraft:smooth_basalt'), 1)]),
            'replaceable': f'#{NS}:deep_sift_ground', 'surface': surf, 'vegetation_chance': 0.22,
            'vegetation_feature': {'feature': f'{NS}:music_crystal_{surf}', 'placement': []},
            'vertical_range': 5, 'xz_radius': {'type': 'minecraft:uniform', 'max_inclusive': 6, 'min_inclusive': 3}})
        placed(f'crystal_patch_{surf}', f'crystal_patch_{surf}', [
            count(48), sq, height(-60, 50),
            {'type': 'minecraft:environment_scan', 'allowed_search_condition': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
             'direction_of_search': 'down' if surf == 'floor' else 'up', 'max_steps': 12,
             'target_condition': {'type': 'minecraft:has_sturdy_face', 'direction': 'up' if surf == 'floor' else 'down'}},
            {'type': 'minecraft:offset', 'x': 0, 'y': 1 if surf == 'floor' else -1, 'z': 0}, BIOME])
    # crystal geodes: hushslate shells lined with amethyst and budding prism, crystals growing inward
    import json
    geo = json.load(open(os.path.join(GW.VD, 'worldgen/feature/amethyst_geode.json')))
    geo['blocks']['outer_layer_provider'] = st('hushslate', axis='y')
    geo['blocks']['middle_layer_provider'] = st('minecraft:calcite')
    geo['blocks']['alternate_inner_layer_provider'] = GW.weighted([(st('prism_ore'), 1), (st('minecraft:amethyst_block'), 6)])
    geo['blocks']['inner_placements'] = [st('music_crystal', color=c, facing='up', frozen=False) for c in COLORS]
    geo['use_alternate_layer0_chance'] = 0.06
    feature('crystal_geode', geo)
    placed('crystal_geode', 'crystal_geode', [rarity(3), sq, height(-56, 30), BIOME])
    feature('cavern_glow_lichen', {'type': 'minecraft:multiface_growth', 'block': 'minecraft:glow_lichen', 'can_be_placed_on': [
        GW.rl('dreamstone'), GW.rl('hushslate'), 'minecraft:calcite', 'minecraft:amethyst_block'], 'can_place_on_ceiling': True,
        'can_place_on_wall': True, 'search_range': 20})
    placed('cavern_glow_lichen', 'cavern_glow_lichen', [count(60), sq, height(-60, 50), {'type': 'minecraft:surface_relative_threshold_filter',
                                                                                       'heightmap': 'OCEAN_FLOOR_WG', 'max_inclusive': -13}, BIOME])
    GW.biome('caravans_cavern', fog='#b48cff', sky='#3a2a6a', water='#7fd8ff', grass='#8f7fe0', foliage='#a08af0', temp=0.6, down=0.4,
             spawns=GW.mobs(monster=[('caravan', 40, 2, 4), ('caravan_queen', 2, 1, 1)]),  # CR2: a rare Queen
             parts=GW.particles(('glow_dust', 0.008), ('star_sparkle', 0.004), ('sift_note', 0.0008)),
             music=f'{NS}:music.deep_sift', ambient_loop=f'{NS}:ambient.deep_sift.loop',
             feats=GW.COMMON_UNDERGROUND + [(2, 'crystal_geode'), (6, 'ore_prism_cavern'), (6, 'ore_siftite_cavern'), (6, 'ore_diamond_cavern'),
                                            (6, 'ore_emerald_cavern'), (6, 'ore_gold_cavern'), (6, 'ore_lapis_cavern'), (6, 'ore_redstone_cavern'),
                                            (6, 'ore_egg_laden_cavern'),
                                            (9, 'crystal_patch_floor'), (9, 'crystal_patch_ceiling'), (9, 'cavern_glow_lichen')])


def biome_entries(pt):
    """The cavern's slice of the dimension's biome source: humid ground between the surface and the deep Sift."""
    return [pt('caravans_cavern', h=[0.55, 1.0], d=[0.25, 0.85])]


# ============================================================================ the Caravan crabs (mobs.ALL)
#
# CR2: Caravans are alien crabs. A broad, mottled shell crusted with gems of their caravan's colour
# (the crust grows as a Caravan ages, see CaravanModel), four pairs of jointed legs tipped with
# crystal, two claws that dig ore out of the rock and carry it to their Queen, glowing eyes on
# stalks and two feelers with crystal beads. The Queen is a vast, fat hermit crab under a spiral
# shell; the larva a little shelled grub.

# caravan colour -> the shell (base, light, dark) and the belly; the gems use the crystal tones of COLORS
SHELLS = {
    'amber': ('#6a452e', '#8c603e', '#3c261a', '#d2a878'),
    'rose': ('#5e3350', '#82496c', '#36192c', '#d4a0b6'),
    'teal': ('#28525a', '#3a7476', '#152e32', '#9ccabe'),
    'violet': ('#3c3062', '#544588', '#1e1838', '#b4a6d8'),
    'gold': ('#5c5028', '#7f7138', '#342c14', '#d4c690'),
}


def _hexs(c):
    return '#%02x%02x%02x' % tuple(c[:3])


def _mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def overlay_rows(base, top):
    """Lays the ascii map `top` over `base`: every character of `top` but '.' wins."""
    return [''.join(t if t != '.' else b for b, t in zip(rb, rt)) for rb, rt in zip(base, top)]


def _crab_palettes(prefix):
    """One palette per caravan colour (variants <prefix>_<colour>): shell, belly, legs, eyes and gems."""
    variants = {}
    for col, (_, _, _, cr, crl, crd) in COLORS.items():
        sh, shl, shd, belly = SHELLS[col]
        variants[f'{prefix}_{col}'] = {
            'shell': sh, 'shell_l': shl, 'shell_d': shd, 'belly': belly, 'belly_l': _hexs(_mix(belly, '#ffffff', 0.45)),
            'belly_d': _hexs(_mix(belly, sh, 0.4)), 'crystal': cr, 'crystal_l': crl, 'crystal_d': crd,
            'leg': _hexs(_mix(shd, sh, 0.5)), 'leg_l': sh, 'leg_d': _hexs(_mix(shd, '#000000', 0.3)),
            'claw_tip': _hexs(_mix(belly, crl, 0.35)), 'eye': cr, 'eye_l': crl, 'pupil': '#140c1c', 'eye_hi': '#ffffff',
            'sac': _hexs(_mix(crd, belly, 0.35)), 'sac_l': _hexs(_mix(cr, '#ffffff', 0.25)), 'sac_d': _hexs(_mix(crd, shd, 0.45)),
            'rune': cr}
    return variants


def _crust(w, h, seed, density=0.2, lit=0.35):
    """Gem grit grown into a shell face (hd map): little clusters of crystal, some of them alight."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(int(w * h * density / 3)):
        x, y = rnd.randrange(w), rnd.randrange(h)
        on = rnd.random() < lit
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1))[:rnd.randrange(1, 5)]:
            if x + dx < w and y + dy < h:
                g[y + dy][x + dx] = 'K' if on else 'k'
        if on and y > 0:
            g[y - 1][x] = 'w'  # a glint on the top facet
    return [''.join(r) for r in g]


def _dots(w, h, step, row, phase=0):
    """A row of glowing pores along a shell edge (hd map)."""
    return [''.join('g' if (y == row and (x + phase) % step == 0) else '.' for x in range(w)) for y in range(h)]


def _runes(w, h, seed):
    """Braided veins of light on the Queen's shell (hd map): two sine strands and a few glyph ticks."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for k, (amp, f, ph) in enumerate(((h * 0.28, 0.33, 0.0), (h * 0.22, 0.27, 2.1))):
        for x in range(w):
            y = int(round(h / 2 + amp * math.sin(x * f + ph)))
            if 0 <= y < h:
                g[y][x] = 'r'
    for _ in range(w // 5):
        x, y = rnd.randrange(1, w - 1), rnd.randrange(1, h - 1)
        g[y][x] = 'r'
        g[y - 1][x] = 'r'
    return [''.join(r) for r in g]


def _leg_bend(pivot_y, l1, a1, spread, l2, ground=23.8):
    """The knee angle that sets a two-part leg's tip on the ground. The thigh is turned by `spread`
    (yaw) and raised by `a1` (roll, up < 0); the shin bends by the returned angle about its own z.
    The tip's height is pivot + l1 sin(a1) cos(spread) + A cos(b) + B sin(b): solved on the rising
    branch (the knee stays up), or the longest reach if the ground is out of reach."""
    c = math.cos(spread)
    a, b = l2 * math.sin(a1) * c, l2 * math.cos(a1)
    target = ground - pivot_y - l1 * math.sin(a1) * c
    r, phi = math.hypot(a, b), math.atan2(b, a)
    return phi - math.acos(max(-1.0, min(1.0, target / r)))


def _gem(parent, name, pivot, rot, w, h):
    """One gem of the crust: a crystal prism with a paler point that glows faintly in the dark."""
    g = parent.part(name, pivot=pivot, rot=rot)
    g.cube((-w / 2, -h * 0.72, -w / 2), (w, h * 0.72, w), color='crystal', pattern='crystal')
    t = w * 0.56
    g.cube((-t / 2, -h, -t / 2), (t, h * 0.3, t), color='crystal_l', pattern='crystal', glow=True)
    return g


CRUST_KEYS = {'k': 'crystal_d', 'K': 'crystal', 'w': 'crystal_l'}


def _crab_eye(stalk, name, size):
    """A small dark eye on top of its stalk, with a faint glint of the caravan's crystal in it that
    glows in the dark the way a spider's eyes do."""
    n = int(math.ceil(size)) * 2
    front = [''.join('g' if (x, y) == (n // 2 - 1, n // 2 - 1) or (n > 2 and (x, y) == (n // 2, n // 2 - 1)) else 'P'
                     for x in range(n)) for y in range(n)]
    e = stalk.part(name, pivot=(0, -stalk.cubes[0].size[1], 0))
    e.cube((-size / 2, -size, -size / 2), (size, size, size), **_mc('pupil', clusters=0.0, rim=False), faces={
        'north': _mc('pupil', clusters=0.0, rim=False, hd=True, map=front, keys={'P': 'pupil', 'g': 'eye'}, glow_keys='g'),
        'up': _mc('shell_d', clusters=0.0, rim=False), 'down': _mc('shell_d', clusters=0.0, rim=False),
        'south': _mc('shell_d', clusters=0.0, rim=False)})
    return e


def _pincer_teeth(w, h):
    return [''.join('t' if (y == h - 1 and x % 2 == 0) else '.' for x in range(w)) for y in range(h)]


def _claw(body, side, sx, pivot, rot, s, crystal_armour):
    """A claw on its arm: palm and fixed finger, the hinged pincer, and (soldiers, Queens) crystal knuckles.
    `s` scales the whole limb."""
    arm = body.part(f'{side}_arm', pivot=pivot, rot=rot)
    arm.cube((-0.7 * s, -0.7 * s, -3.4 * s), (1.4 * s, 1.4 * s, 3.4 * s), **_mc('shell', clusters=0.2, rim=False))
    claw = arm.part(f'{side}_claw', pivot=(0, 0, -3.2 * s), rot=(-0.15, 1.0 * sx, 0))
    claw.cube((-1.25 * s, -1.35 * s, -3.0 * s), (2.5 * s, 2.7 * s, 3.0 * s), **_mc('shell', clusters=0.3, spots=0.3, accent='shell_l'), faces={
        'up': _mc('shell_l', clusters=0.2, hd=True, map=_crust(math.ceil(2.5 * s) * 2, math.ceil(3.0 * s) * 2, 11 + int(s * 3), 0.25),
                  keys=CRUST_KEYS, glow_keys='Kw'),
        'down': _mc('belly_d', clusters=0.2)})
    # the fixed finger, pale at the tip, with a row of teeth along its biting edge
    claw.cube((-0.75 * s, 0.1 * s, -5.3 * s), (1.35 * s, 1.15 * s, 2.4 * s), **_mc('shell_d', clusters=0.1, rim=False), faces={
        'north': _mc('claw_tip', clusters=0.0, rim=False),
        'up': _mc('shell_d', clusters=0.0, rim=False, hd=True, map=_pincer_teeth(int(math.ceil(1.35 * s)) * 2, int(math.ceil(2.4 * s)) * 2)[::-1],
                  keys={'t': 'claw_tip'})})
    pincer = claw.part(f'{side}_pincer', pivot=(0, -0.55 * s, -3.0 * s), rot=(-0.2, 0, 0))
    pincer.cube((-0.6 * s, -0.5 * s, -2.5 * s), (1.2 * s, 0.95 * s, 2.5 * s), **_mc('shell_d', clusters=0.1, rim=False), faces={
        'north': _mc('claw_tip', clusters=0.0, rim=False),
        'down': _mc('shell_d', clusters=0.0, rim=False, hd=True, map=_pincer_teeth(int(math.ceil(1.2 * s)) * 2, int(math.ceil(2.5 * s)) * 2),
                    keys={'t': 'claw_tip'})})
    if crystal_armour:
        knuckles = claw.part(f'{side}_claw_crystal', pivot=(0, -1.35 * s, -1.4 * s))
        _gem(knuckles, f'{side}_knuckle_0', (0.3 * s * sx, 0, 0.4 * s), (-0.35, 0, 0.3 * sx), 1.1 * s, 2.6 * s)
        _gem(knuckles, f'{side}_knuckle_1', (-0.5 * s * sx, 0, -0.9 * s), (-0.6, 0, -0.25 * sx), 0.9 * s, 2.0 * s)
    return arm


def _crab_legs(body, pivot_x, pivot_y, body_y, zs, l1, t1, l2, t2, a1=-0.38, spreads=(0.6, 0.2, -0.22, -0.6)):
    """Pairs of jointed legs (one pair per z in zs): thighs raised and fanned out (by `spreads`), shins
    bent down onto the ground, each tipped with a little crystal. Returns the thighs."""
    legs = []
    for i, z in enumerate(zs):
        for side, sx in (('left', 1), ('right', -1)):
            spread = spreads[i]
            bend = _leg_bend(body_y + pivot_y, l1, a1, spread, l2 + t2 * 0.6)
            leg = body.part(f'{side}_leg_{i}', pivot=(pivot_x * sx, pivot_y, z), rot=(0, spread * sx, a1 * sx))
            leg.cube((0 if sx > 0 else -l1, -t1 / 2, -t1 / 2), (l1, t1, t1), **_mc('shell', clusters=0.25, rim=False), faces={
                'up': _mc('shell_l', clusters=0.3, rim=False)})
            shin = leg.part(f'{side}_shin_{i}', pivot=(l1 * sx, 0, 0), rot=(0, 0, bend * sx))
            shin.cube((0 if sx > 0 else -l2, -t2 / 2, -t2 / 2), (l2, t2, t2), **_mc('leg', clusters=0.15, rim=False), faces={
                'up': _mc('leg_l', clusters=0.2, rim=False)})
            tt = t2 * 0.8
            shin.cube((l2 if sx > 0 else -l2 - t2 * 1.2, -tt / 2, -tt / 2), (t2 * 1.2, tt, tt), color='crystal', pattern='crystal', glow=True)
            legs.append(leg)
    return legs


def caravan():
    """The Caravan worker (and soldier): an alien crab with a gem-crusted shell (see the notes above).
    CaravanModel shows the soldiers' crystal knuckles, grows the crust and turns the crab sideways
    to scuttle."""
    from modelkit import Model
    variants = _crab_palettes('caravan')
    m = Model('caravan', (64, 64), dict(variants['caravan_amber']), variants, res=2,
              materials={'leg': 'chitin', 'claw_tip': 'chitin', 'belly': 'chitin'})
    body = m.part('body', pivot=(0, 19, 0))
    mottled = _mc('shell', clusters=0.35, spots=0.3, accent='shell_l')
    body.cube((-5, -2.5, -4), (10, 3.5, 8), **mottled, faces={
        'up': _mc('shell', clusters=0.3, hd=True, map=_crust(20, 16, 3), keys=CRUST_KEYS, glow_keys='Kw'),
        'north': _mc('shell_d', clusters=0.3, spots=0.2, accent='shell'), 'down': _mc('belly', clusters=0.2)})
    # the flared rim of the shell, pores glowing along its edge
    pores = _mc('shell_d', clusters=0.15, hd=True, map=_dots(14, 4, 3, 1), keys={'g': 'crystal'}, glow_keys='g')
    body.cube((-6, -1, -3.5), (12, 1.5, 7), **_mc('shell_d', clusters=0.2), faces={
        'east': pores, 'west': dict(pores, map=_dots(14, 4, 3, 1, 1)),
        'north': _mc('shell_d', clusters=0.15, hd=True, map=_dots(24, 4, 4, 1, 2), keys={'g': 'crystal'}, glow_keys='g'),
        'up': _mc('shell', clusters=0.3, hd=True, map=_crust(24, 14, 4, 0.12), keys=CRUST_KEYS, glow_keys='Kw'),
        'down': _mc('belly_d', clusters=0.2)})
    body.cube((-4, -3.5, -3), (8, 1, 6), **mottled, faces={
        'up': _mc('shell_l', clusters=0.3, hd=True, map=_crust(16, 12, 5, 0.25), keys=CRUST_KEYS, glow_keys='Kw')})
    body.cube((-3.5, -2, -5), (7, 2, 1), **_mc('shell_d', clusters=0.2))      # the brow over the mouth
    body.cube((-3.5, -2, 4), (7, 2.5, 1), **_mc('shell_d', clusters=0.2))     # the tail plate
    body.cube((-4, 1, -3), (8, 1, 6.5), **_mc('belly', clusters=0.25))        # the belly plate
    # the gem crust: seven gems, the biggest in the middle
    for i, (x, y, z, rx, rz, w, h) in enumerate((
            (0, -3.5, 0.4, -0.1, 0.05, 1.8, 4.2), (-2.2, -3.3, -1.3, -0.35, -0.35, 1.3, 2.9), (2.1, -3.3, 1.7, 0.3, 0.4, 1.4, 3.1),
            (-1.7, -3.3, 2.3, 0.45, -0.2, 1.0, 2.1), (2.6, -3.1, -1.5, -0.3, 0.45, 1.0, 1.9), (-3.8, -2.4, 0.7, 0.1, -0.8, 0.9, 1.6),
            (3.9, -2.4, -0.3, -0.1, 0.85, 0.9, 1.7))):
        _gem(body, f'gem_{i}', (x, y, z), (rx, 0, rz), w, h)
    for side, sx in (('left', 1), ('right', -1)):
        stalk = body.part(f'{side}_eye_stalk', pivot=(1.6 * sx, -2.2, -4.4), rot=(-0.2, 0, 0.18 * sx))
        stalk.cube((-0.35, -2.6, -0.35), (0.7, 2.6, 0.7), **_mc('shell_d', clusters=0.0, rim=False))
        _crab_eye(stalk, f'{side}_eye', 1.0)
        # feelers that tap along in the caravan's music, with a glowing bead at the tip
        feeler = body.part(f'{side}_feeler', pivot=(0.6 * sx, -1.4, -5), rot=(-0.75, -0.35 * sx, 0))
        feeler.cube((-0.2, -0.2, -3.5), (0.4, 0.4, 3.5), **_mc('shell_l', clusters=0.0, rim=False))
        ftip = feeler.part(f'{side}_feeler_tip', pivot=(0, 0, -3.5), rot=(0.95, 0, 0))
        ftip.cube((-0.15, -0.15, -3), (0.3, 0.3, 3), **_mc('shell_l', clusters=0.0, rim=False))
        ftip.cube((-0.45, -0.45, -3.8), (0.9, 0.9, 0.9), color='crystal_l', pattern='crystal', glow=True)
        mouth = body.part(f'{side}_mouthpart', pivot=(0.8 * sx, -0.3, -5), rot=(0.15, 0, 0))
        mouth.cube((-0.6, 0, -0.4), (1.2, 1.7, 0.4), **_mc('belly_d', clusters=0.0, rim=False))
        _claw(body, side, sx, (4.2 * sx, 0.3, -3.2), (0.25, -0.6 * sx, 0.1 * sx), 1.0, True)
    _crab_legs(body, 5.0, 0.4, 19, (-2.1, -0.5, 1.1, 2.7), 4.2, 1.2, 5.6, 0.9)
    return m


def caravan_queen():
    """The Caravan Queen: a vast hermit crab carrying a great block of a shell - mineral grey-brown
    stone with a spiral worn into its sides and a pearly lip round its mouth - crusted with gem
    clusters of her caravans' colour. Gems grow out of her own fat, soft body too, from her claws (the
    right one huge, as a hermit crab's is) and her knees. Long jointed legs arch high over her body,
    small dark eyes sit on thin stalks, long feelers sweep in front."""
    from modelkit import Model
    variants = _crab_palettes('caravan_queen')
    for v in variants.values():
        v.update({'nacre': '#e8d6dc', 'nacre_l': '#f6ecee', 'nacre_d': '#bfa8b0', 'soft': _hexs(_mix(v['belly'], '#c98c84', 0.35)),
                  'soft_l': _hexs(_mix(v['belly'], '#e8c4b8', 0.4)), 'soft_d': _hexs(_mix(v['belly'], v['shell'], 0.35)),
                  'stone': '#7a716c', 'stone_l': '#958b84', 'stone_d': '#544c49', 'hollow': '#2a2024'})
    m = Model('caravan_queen', (192, 192), dict(variants['caravan_queen_amber']), variants, res=2,
              materials={'leg': 'chitin', 'claw_tip': 'chitin', 'belly': 'chitin', 'nacre': 'bone', 'soft': 'skin', 'stone': 'stone'})
    sk = dict(CRUST_KEYS, d='stone_d', l='stone_l')

    def strata(w, h, seed):
        """Rough mineral layers on the shell's faces, gem grit caught between them (hd map)."""
        rnd = random.Random(seed)
        rows = []
        for y in range(h):
            r = ''
            for x in range(w):
                band = (y + int(2.0 * math.sin(x * 0.35 + seed))) % 7
                if band == 0:
                    r += 'd'
                elif band == 1 and rnd.random() < 0.7:
                    r += 'l'
                elif rnd.random() < 0.035:
                    r += 'k' if rnd.random() < 0.7 else 'K'
                else:
                    r += '.'
            rows.append(r)
        return rows

    def spiral(w, h, seed, mirror=False):
        """The side of the shell: the spiral of the whorl worn into the stone, mineral layers between
        the turns, gem grit caught in them (hd map)."""
        rnd = random.Random(seed)
        cx, cy = w * 0.45, h * 0.5
        rows = []
        for y in range(h):
            r = ''
            for x in range(w):
                u = (w - 1 - x) if mirror else x
                dx, dy = (u + 0.5 - cx) / (w * 0.5), (y + 0.5 - cy) / (h * 0.5)
                rad = math.hypot(dx, dy)
                ang = (math.atan2(dy, dx) / math.tau) % 1.0
                turn = (rad / 0.3 - ang) % 1.0
                if rad < 0.92 and turn < 0.14:
                    r += 'd'
                elif rad < 0.92 and turn < 0.24:
                    r += 'l'
                elif rnd.random() < 0.03:
                    r += 'k'
                else:
                    r += '.'
            rows.append(r)
        return rows

    def stone(size, seed, **over):
        w, h, d = (math.ceil(s) * 2 for s in size)
        f = {'east': _mc('stone', clusters=0.35, hd=True, map=strata(d, h, seed), keys=sk, glow_keys='K'),
             'west': _mc('stone', clusters=0.35, hd=True, map=strata(d, h, seed + 1), keys=sk, glow_keys='K'),
             'north': _mc('stone', clusters=0.35, hd=True, map=strata(w, h, seed + 2), keys=sk, glow_keys='K'),
             'south': _mc('stone', clusters=0.35, hd=True, map=strata(w, h, seed + 3), keys=sk, glow_keys='K'),
             'up': _mc('stone_l', clusters=0.35, hd=True, map=_crust(w, d, seed + 4, 0.12), keys=CRUST_KEYS, glow_keys='Kw'),
             'down': _mc('stone_d', clusters=0.3)}
        f.update(over)
        return f
    body = m.part('body', pivot=(0, 12, 0))
    # ---- the soft body: a chitin thorax in front, and a fat, ringed belly sagging out under the shell
    body.cube((-6.5, -4, -11), (13, 8, 11), **_mc('shell', clusters=0.35, spots=0.25, accent='shell_l'), faces={
        'down': _mc('belly_d', clusters=0.25), 'up': _mc('shell_l', clusters=0.3)})
    rings = [('d' if y % 4 == 3 else '.') * 40 for y in range(16)]
    body.cube((-9, -2, 0), (18, 8, 13), **_mc('soft', clusters=0.3, spots=0.2, accent='soft_d'), faces={
        'east': _mc('soft', clusters=0.25, hd=True, map=rings, keys={'d': 'soft_d'}),
        'west': _mc('soft', clusters=0.25, hd=True, map=rings, keys={'d': 'soft_d'}),
        'south': _mc('soft', clusters=0.25, hd=True, map=rings, keys={'d': 'soft_d'}),
        'down': _mc('soft_d', clusters=0.3)})
    # gems grow right out of her belly
    for i, (x, y, z, ry, rz, w, h) in enumerate(((9.0, 2.0, 4.0, 0.2, 1.3, 1.6, 3.6), (9.0, 3.0, 9.0, -0.3, 1.5, 1.2, 2.8),
                                                (-9.0, 2.5, 5.0, -0.1, -1.35, 1.5, 3.4), (-9.0, 3.5, 10.0, 0.25, -1.5, 1.1, 2.4))):
        _gem(body, f'belly_gem_{i}', (x, y, z), (0, ry, rz), w, h)
    # ---- the shell: a great block of stone with a spiral worn into its sides, tilted back on her back
    shell = body.part('shell', pivot=(0, -1, 3), rot=(-0.1, 0, 0))
    sw, sh_, sd = 18, 17, 17
    mw, mh = sw * 2, sh_ * 2

    def mouth(x, y):
        """The mouth of the shell: a dark hollow behind a pearly lip, her soft body filling it."""
        if 2 <= x < mw - 2 and y >= mh // 2:
            if x in (2, 3, mw - 4, mw - 3) or y in (mh // 2, mh // 2 + 1):
                return 'N' if (x + y) % 7 == 0 else 'n'
            return 'h'
        return '.'
    mouth_map = overlay_rows(strata(mw, mh, 13), [''.join(mouth(x, y) for x in range(mw)) for y in range(mh)])
    shell.cube((-sw / 2, -sh_, -4), (sw, sh_, sd), color='stone', pattern='mc', faces=stone((sw, sh_, sd), 11, **{
        'east': _mc('stone', clusters=0.35, hd=True, map=spiral(sd * 2, mh, 21), keys=sk, glow_keys='K'),
        'west': _mc('stone', clusters=0.35, hd=True, map=spiral(sd * 2, mh, 22, mirror=True), keys=sk, glow_keys='K'),
        'north': _mc('stone', clusters=0.3, hd=True, keys=dict(sk, h='hollow', n='nacre', N='nacre_l'), map=mouth_map)}))
    # the worn crown of the whorl: two smaller blocks stepping up at the back
    shell.cube((-7, -21, 1), (14, 4, 11), color='stone', pattern='mc', faces=stone((14, 4, 11), 31))
    shell.cube((-4, -24, 5), (8, 3, 7), color='stone_l', pattern='mc', faces=stone((8, 3, 7), 41))
    # the gem clusters crusting it, thickest on top
    for i, (x, y, z, rx, rz, w, h) in enumerate((
            (-5, -17, -1, -0.2, -0.25, 2.4, 5.5), (4.5, -17, 0.5, 0.1, 0.3, 2.6, 6.5), (0, -21, 3, -0.15, 0.05, 2.8, 7.0),
            (-4.5, -21, 8, 0.25, -0.35, 2.0, 4.6), (4.5, -21, 9, 0.3, 0.3, 1.9, 4.4), (0, -24, 8, 0.2, 0.0, 2.0, 5.0),
            (-2.5, -24, 11, 0.5, -0.3, 1.4, 3.2), (9, -11, 4, 0.0, 1.25, 2.0, 4.4), (9, -5, -1, -0.2, 1.4, 1.5, 3.0),
            (-9, -12, 6, 0.1, -1.2, 2.2, 4.6), (-9, -6, 10, 0.2, -1.4, 1.4, 2.8), (2, -17, 11, 0.6, 0.15, 1.7, 3.8))):
        _gem(shell, f'gem_{i}', (x, y, z), (rx, 0, rz), w, h)
    # ---- the head under the lip of the shell: thin eye stalks with small dark eyes, long feelers, mouthparts
    head = body.part('head', pivot=(0, -1, -11))
    head.cube((-4.5, -3, -3), (9, 5, 3), **_mc('shell_d', clusters=0.3, spots=0.2, accent='shell'))
    for side, sx in (('left', 1), ('right', -1)):
        stalk = head.part(f'{side}_eye_stalk', pivot=(2.4 * sx, -3, -1.5), rot=(-0.25, 0, 0.15 * sx))
        stalk.cube((-0.5, -6, -0.5), (1, 6, 1), **_mc('shell_d', clusters=0.15, rim=False))
        _crab_eye(stalk, f'{side}_eye', 1.6)
        feeler = head.part(f'{side}_feeler', pivot=(1.0 * sx, -1.0, -3), rot=(-0.7, -0.3 * sx, 0))
        feeler.cube((-0.3, -0.3, -9), (0.6, 0.6, 9), **_mc('shell_l', clusters=0.0, rim=False))
        ftip = feeler.part(f'{side}_feeler_tip', pivot=(0, 0, -9), rot=(0.85, 0, 0))
        ftip.cube((-0.2, -0.2, -8), (0.4, 0.4, 8), **_mc('shell_l', clusters=0.0, rim=False))
        mouth = head.part(f'{side}_mouthpart', pivot=(1.4 * sx, 2, -2.6), rot=(0.15, 0, 0))
        mouth.cube((-1.1, 0, -0.6), (2.2, 2.6, 0.6), **_mc('belly_d', clusters=0.2, rim=False))
    # ---- claws held up in front: the right one huge, as a hermit crab's is; crystals grow from both
    _claw(body, 'left', 1, (6.0, 1.5, -10.0), (0.15, -0.5, 0.1), 2.0, True)
    _claw(body, 'right', -1, (-6.0, 1.0, -10.0), (0.1, 0.45, -0.1), 3.0, True)
    # ---- three pairs of long walking legs arching high over her body, crystals at the knees
    legs = _crab_legs(body, 6.0, -1.0, 12, (-9.5, -6.5, -3.5), 12.0, 2.2, 16.0, 1.8, a1=-0.7, spreads=(0.85, 0.2, -0.45))
    for i, leg in enumerate(legs):
        sx = 1 if leg.name.startswith('left') else -1
        _gem(leg, leg.name + '_gem', (10.5 * sx, -1.0, 0), (0, 0, -0.4 * sx), 1.0, 2.2)
    return m


def caravan_larva():
    """A Caravan larva: what hatches from the eggs Caravans leave in ore sockets. A small grub of five
    shelled segments, each plate tinted with its caravan's colour and a gem nub on two of them,
    bristly little legs, a pair of glowing pinprick eyes and snapping mandibles."""
    from modelkit import Model
    variants = _crab_palettes('caravan_larva')
    m = Model('caravan_larva', (48, 48), dict(variants['caravan_larva_amber']), variants, res=2,
              materials={'leg': 'chitin', 'claw_tip': 'chitin', 'belly': 'skin'})
    plate = _mc('shell', clusters=0.3, spots=0.3, accent='shell_l')
    sizes = ((4.0, 3.2, 3.0), (4.6, 3.6, 2.8), (4.2, 3.2, 2.6), (3.4, 2.6, 2.4), (2.4, 1.8, 2.2))
    prev = m.part('head', pivot=(0, 22.2, -3.2))
    head = prev
    w, h, d = sizes[0]
    head.cube((-w / 2, -h / 2, -d), (w, h, d), **plate, faces={
        'north': _mc('shell_d', clusters=0.0, hd=True, map=['........', '.e....e.', '........', '..dddd..', '........', '........'],
                     keys={'e': 'eye', 'd': 'pupil'}, glow_keys='e'),
        'down': _mc('belly', clusters=0.2)})
    for side, sx in (('left', 1), ('right', -1)):
        mand = head.part(f'{side}_mandible', pivot=(1.0 * sx, 0.8, -d), rot=(0, -0.35 * sx, 0))
        mand.cube((-0.4, -0.4, -1.6), (0.8, 0.8, 1.6), **_mc('claw_tip', clusters=0.0, rim=False))
    z = 0.0
    for i, (w, h, d) in enumerate(sizes[1:], start=1):
        seg = prev.part(f'segment_{i}', pivot=(0, 0.3 if i > 1 else 0, z if i > 1 else 0), rot=(0, 0, 0))
        seg.cube((-w / 2, -h / 2, 0), (w, h, d), **plate, faces={'down': _mc('belly', clusters=0.2),
                                                                'up': _mc('shell_l', clusters=0.25, hd=True,
                                                                          map=_crust(math.ceil(w) * 2, math.ceil(d) * 2, 60 + i, 0.25),
                                                                          keys=CRUST_KEYS, glow_keys='Kw')})
        for side, sx in (('left', 1), ('right', -1)):
            seg.cube((w / 2 - 0.1 if sx > 0 else -w / 2 - 0.9, h / 2 - 0.4, d * 0.3), (1.0, 0.4, 0.4), **_mc('leg', clusters=0.0, rim=False))
        if i in (1, 3):
            _gem(seg, f'nub_{i}', (0, -h / 2, d / 2), (0.2 if i == 1 else -0.2, 0, 0), 1.0, 1.8 if i == 1 else 1.4)
        prev = seg
        z = d
    return m


MODELS = {'caravan': caravan, 'caravan_queen': caravan_queen, 'caravan_larva': caravan_larva}

