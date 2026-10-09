"""C: the Caravans Cavern - its crystal-ant Caravans, their music crystals and colony, and the Prism
gem with its ore, block and armour.

Hooks (one line each): gen_assets.gen_block (model 'music_crystal'), gen_assets.generate (sounds),
gen_data.generate (lang, loot, recipes, tags, equipment), gen_world (features, biome, biome-source
entry), gen_structures.generate (the colony), mobs.ALL (the Caravan model), gen_textures.main
(block, armour textures) and items16.all_items (item sprites)."""
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


def textures(out):
    for i, col in enumerate(COLORS):
        out(f'block/music_crystal_{col}', crystal_tex(col, 31 + i))
        out(f'block/music_crystal_{col}_frozen', frozen_tex(col, 41 + i))
    out('block/prism_ore', ore_tex('dreamstone'))
    out('block/deep_prism_ore', ore_tex('hushslate'))
    out('block/prism_block', prism_block_tex())
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
    o = {'prism_gem': prism_gem()}
    for p in ARMOR:
        o[f'prism_{p}'] = _prism_armor(p)
    o['caravan_spawn_egg'] = I.egg(['#7a3c10', '#c8741e', '#e8a040', '#f8c870'], '#3a1a08', {
        1: '.....a....a.....', 2: '......a..a......', 6: '...k........k...', 7: '..k..........k..'}, pal={'a': '#40220a', 'k': '#ffb43a'})
    return o


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
    })
    GA.SUBTITLES.update({
        'entity.caravan.ambient': 'Caravan chitters', 'entity.caravan.hurt': 'Caravan hurts', 'entity.caravan.death': 'Caravan dies',
        'entity.caravan.step': 'Caravan scuttles', 'entity.caravan.alarm': 'Caravans raise the alarm', 'entity.caravan.build': 'Caravan grows a crystal',
        'block.music_crystal.chime': 'Music Crystal rings', 'block.music_crystal.shatter': 'Frozen crystal shatters',
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
        GD.pool([GD.item('prism_gem')], condition={'type': 'minecraft:all_of', 'terms': [GD.PLAYER_KILL, {
            'type': 'minecraft:random_chance_with_enchanted_bonus', 'enchantment': 'minecraft:looting', 'unenchanted_chance': 0.025,
            'enchanted_chance': {'type': 'minecraft:linear', 'base': 0.035, 'per_level_above_first': 0.01}}]})])
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
              'minecraft:smooth_basalt', 'minecraft:tuff', f'#{NS}:caravan_minable']:
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
        f'biome.{NS}.caravans_cavern': 'Caravans Cavern',
        f'message.{NS}.caravan.calm': 'The Caravans hum along and calm down.',
        f'codex.{NS}.caravan.title': 'Caravan', f'codex.{NS}.caravan.tagline': 'Crystal ants of the deep',
        f'codex.{NS}.caravan.body': 'Caravans are crystal-backed ants in amber, rose, teal, violet and rare gold. Workers dig natural ore '
                                    '(never your buildings) and carry the chunks home, where they grow them into new Music Crystals - '
                                    'sometimes freezing a treasure inside. Big soldiers with crystal jaws guard the colony. Touch a colony '
                                    'or break its crystals and the whole swarm comes for you. They tap crystals with their antennae to '
                                    'sing; play the Crystal Hymn and they calm down and sing along.',
        f'codex.{NS}.music_crystal.title': 'Music Crystals', f'codex.{NS}.music_crystal.tagline': 'Glowing, tuned, sometimes full',
        f'codex.{NS}.music_crystal.body': 'Every crystal rings its own note when you hit it or use it, and its colour sets the pitch: amber '
                                          'lowest, gold highest. Some are frozen: clear crystal blocks with a treasure spinning inside. '
                                          'Break one to set the treasure free - but Caravans swarm anyone who breaks their crystals, unless '
                                          'the Crystal Hymn has calmed them first.',
        f'codex.{NS}.prism.title': 'Prism', f'codex.{NS}.prism.tagline': 'The rainbow gem',
        f'codex.{NS}.prism.body': 'Hexagonal rainbow gems, the first treasure of the Sift: an iron pickaxe frees them. Small veins of Prism '
                                  'Ore run all through the Sift\'s caves and rich ones through the Caravans Cavern. Prism armour is a '
                                  'little tougher than diamond and drinks in music: wear two pieces or more and every note played near '
                                  'you heals you, the full set twice as fast. Prism also inlays the gem instruments and the Europhy Table.',
        f'codex.{NS}.caravan_colony.title': 'Caravan Colony', f'codex.{NS}.caravan_colony.tagline': 'A mound of song',
        f'codex.{NS}.caravan_colony.body': 'Deep in the Caravans Cavern stand crystal-studded mounds tunnelled through by Caravans. At the '
                                           'heart lies the queen chamber: one great crystal and the colony\'s frozen treasures. The '
                                           'soldiers will not let you near - unless you bring the Crystal Hymn.',
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
             spawns=GW.mobs(monster=[('caravan', 40, 2, 4)]),
             parts=GW.particles(('glow_dust', 0.008), ('star_sparkle', 0.004), ('sift_note', 0.0008)),
             music=f'{NS}:music.deep_sift', ambient_loop=f'{NS}:ambient.deep_sift.loop',
             feats=GW.COMMON_UNDERGROUND + [(2, 'crystal_geode'), (6, 'ore_prism_cavern'), (6, 'ore_siftite_cavern'), (6, 'ore_diamond_cavern'),
                                            (6, 'ore_emerald_cavern'), (6, 'ore_gold_cavern'), (6, 'ore_lapis_cavern'), (6, 'ore_redstone_cavern'),
                                            (9, 'crystal_patch_floor'), (9, 'crystal_patch_ceiling'), (9, 'cavern_glow_lichen')])


def biome_entries(pt):
    """The cavern's slice of the dimension's biome source: humid ground between the surface and the deep Sift."""
    return [pt('caravans_cavern', h=[0.55, 1.0], d=[0.25, 0.85])]


# ============================================================================ the colony (gen_structures)

def colony(seed):
    """A Caravan colony: a crystal-studded mound of hushslate and calcite. Four tunnels lead in from
    its foot to the queen chamber, where one great crystal stands among the colony's frozen
    treasures; side galleries hold ore piles waiting to be grown into crystals."""
    from structlib import AIR, B, Build
    S = 33
    c = S // 2
    b = Build(S, 18, S, seed)
    rnd = b.rnd
    shell = b.mix((B('hushslate', axis='y'), 5), (B('cobbled_hushslate'), 3), (B('minecraft:calcite'), 3), (B('minecraft:amethyst_block'), 2),
                  (B('minecraft:smooth_basalt'), 1))
    cols = list(COLORS)

    def crystal(col, facing, frozen=False):
        return B('music_crystal', color=col, facing=facing, frozen=frozen)

    # the ground pad
    b.cyl(c, c, 0, 1, 15, B('hushslate', axis='y'), pick=shell)
    # the mound: a squashed dome
    for x in range(S):
        for z in range(S):
            for y in range(2, 17):
                d = math.sqrt(((x - c) / 14.5) ** 2 + ((y - 1) / 14.0) ** 2 + ((z - c) / 14.5) ** 2)
                if d <= 1.0:
                    b.set(x, y, z, shell(x, y, z))
    # hollow it: the queen chamber and a ring gallery
    for x in range(S):
        for z in range(S):
            for y in range(2, 14):
                dq = math.sqrt(((x - c) / 7.0) ** 2 + ((y - 2) / 8.0) ** 2 + ((z - c) / 7.0) ** 2)
                r = math.hypot(x - c, z - c)
                ring = 9.5 <= r <= 11.5 and y <= 4
                if dq <= 1.0 or ring:
                    b.set(x, y, z, AIR)
    # four tunnels from the foot of the mound to the chamber, and four side galleries
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for k in range(5, 17):
            x, z = c + dx * k, c + dz * k
            for w in (-1, 0, 1):
                for y in (2, 3, 4):
                    b.set(x + (w if dz else 0), y, z + (w if dx else 0), AIR)
    for dx, dz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        gx, gz = c + dx * 7, c + dz * 7
        for x in range(gx - 2, gx + 3):
            for z in range(gz - 2, gz + 3):
                for y in (2, 3, 4):
                    b.set(x, y, z, AIR)
        # an ore pile waiting to be grown into crystal
        for (ox, oz), blk in zip(((0, 0), (1, 0), (0, 1), (-1, 0)), ('minecraft:raw_gold_block', 'minecraft:raw_copper_block', 'minecraft:raw_iron_block',
                                                                       'minecraft:amethyst_block')):
            b.set(gx + ox, 2, gz + oz, B(blk))
        b.set(gx, 3, gz, crystal(rnd.choice(cols), 'up'))
    # the floor of the chamber: calcite with amethyst inlay
    for x in range(c - 7, c + 8):
        for z in range(c - 7, c + 8):
            if math.hypot(x - c, z - c) <= 7.2:
                b.set(x, 1, z, B('minecraft:amethyst_block') if (x + z) % 3 == 0 else B('minecraft:calcite'))
    # the great crystal: a tapering amethyst-and-prism spire capped and ringed with music crystals
    for y in range(2, 9):
        r = 1.6 - (y - 2) * 0.2
        for x in range(c - 2, c + 3):
            for z in range(c - 2, c + 3):
                if math.hypot(x - c, z - c) <= r:
                    b.set(x, y, z, B('prism_block') if (y == 4 and x == c and z == c) else B('minecraft:amethyst_block'))
    b.set(c, 9, c, crystal('gold', 'up'))
    for (dx, dz, f) in ((2, 0, 'east'), (-2, 0, 'west'), (0, 2, 'south'), (0, -2, 'north')):
        for y in (3, 5):
            b.set(c + dx, y, c + dz, crystal(rnd.choice(cols), f))
    # frozen treasures around the chamber
    for k in range(6):
        a = k * math.tau / 6 + 0.3
        x, z = c + round(math.cos(a) * 5), c + round(math.sin(a) * 5)
        b.set(x, 2, z, crystal(cols[k % len(cols)], 'up', frozen=True))
    # crystals growing on the chamber ceiling and walls
    for (x, y, z), blk in list(b.blocks.items()):
        if blk.name == AIR.name or blk.name.endswith('music_crystal'):
            continue
        for (ox, oy, oz, f) in ((0, -1, 0, 'down'), (0, 1, 0, 'up'), (1, 0, 0, 'east'), (-1, 0, 0, 'west'), (0, 0, 1, 'south'), (0, 0, -1, 'north')):
            nb = b.get(x + ox, y + oy, z + oz)
            if nb is not None and nb.name == AIR.name and rnd.random() < (0.06 if f in ('up', 'down') else 0.035):
                b.set(x + ox, y + oy, z + oz, crystal(rnd.choice(cols), f))
                break
    # crystals studding the outside of the mound
    for x in range(S):
        for z in range(S):
            top = max((y for y in range(18) if b.get(x, y, z) is not None and b.get(x, y, z).name != AIR.name), default=None)
            if top is not None and top >= 3 and top < 17 and rnd.random() < 0.09:
                b.set(x, top + 1, z, crystal(rnd.choice(cols), 'up'))
    return b


def structures(GS):
    """Writes the colony template and its worldgen JSON (one structure, its own salt)."""
    GA = GS.GA
    name = 'caravan_colony'
    os.makedirs(os.path.join(GS.OUT, name), exist_ok=True)
    elements = []
    for i in range(2):
        colony(4200 + i * 17).save(os.path.join(GS.OUT, name, f'{name}_{i}.nbt'))
        elements.append({'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{name}/{name}_{i}',
                                     'processors': 'minecraft:empty', 'projection': 'rigid'}, 'weight': 1})
    GA.write(os.path.join(GS.D, 'template_pool', name, 'start.json'), {'elements': elements, 'fallback': 'minecraft:empty'})
    GA.tag('worldgen/biome', f'{NS}:has_structure/{name}', f'{NS}:caravans_cavern')
    GA.write(os.path.join(GS.D, 'structure', name + '.json'), {
        'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/{name}', 'max_distance_from_center': 80, 'size': 1,
        'spawn_overrides': {'monster': {'bounding_box': 'piece', 'spawns': [
            {'type': f'{NS}:caravan', 'weight': 1, 'count': {'type': 'minecraft:uniform', 'min_inclusive': 2, 'max_inclusive': 4}}]}},
        'start_pool': f'{NS}:{name}/start', 'step': 'underground_structures', 'terrain_adaptation': 'beard_box', 'use_expansion_hack': False,
        'start_height': {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -36}, 'max_inclusive': {'absolute': 4}}})
    GA.write(os.path.join(GS.D, 'structure_set', name + '.json'), {
        'placement': {'type': 'minecraft:random_spread', 'salt': 91733311, 'separation': 8, 'spacing': 22},
        'structures': [{'structure': f'{NS}:{name}', 'weight': 1}]})


# ============================================================================ the Caravan model (mobs.ALL)

def caravan():
    """The Caravan: a crystal-backed ant. A round head with big glossy eyes, elbowed antennae
    tipped with glowing crystal bulbs, little mandibles (or, on soldiers, great crystal jaws), a
    narrow thorax on six jointed legs and a fat striped gaster studded with crystal shards."""
    from modelkit import Model
    pal = {'eye': '#14101c', 'eye_hi': '#ffffff', 'belly': '#f0d8b0'}
    variants = {}
    for col, (ch, chl, chd, cr, crl, crd) in COLORS.items():
        variants[f'caravan_{col}'] = {'chitin': ch, 'chitin_l': chl, 'chitin_d': chd, 'crystal': cr, 'crystal_l': crl, 'crystal_d': crd,
                                      'leg': chd, 'leg_l': ch, 'leg_d': '#2a1a14'}
    pal.update(variants['caravan_amber'])

    def mc(color, **kw):
        return dict(color=color, pattern='mc', **kw)

    m = Model('caravan', (64, 64), pal, variants, res=2)
    body = m.part('body', pivot=(0, 19, 0))
    body.cube((-2, -1.5, -2.5), (4, 3, 5), **mc('chitin', clusters=0.3))
    head = body.part('head', pivot=(0, -0.5, -2.5))
    eye_map = ['........', '........', 'hk....kh', 'kk....kk', 'kk....kk', '........', '........', '........']
    head.cube((-2.5, -2, -4), (5, 4, 4), **mc('chitin', clusters=0.2), faces={
        'north': mc('chitin', clusters=0.0, hd=True, map=['..........', '..........', '.hk....hk.', '.kk....kk.', '.kk....kk.', '..........',
                                                          '...dddd...', '..........'], keys={'h': 'eye_hi', 'k': 'eye', 'd': 'chitin_d'}),
        'east': mc('chitin', clusters=0.1, hd=True, map=eye_map, keys={'h': 'eye_hi', 'k': 'eye'}),
        'west': mc('chitin', clusters=0.1, hd=True, map=eye_map, keys={'h': 'eye_hi', 'k': 'eye'})})
    for side, sx in (('left', 1), ('right', -1)):
        ant = head.part(f'{side}_antenna', pivot=(1.0 * sx, -2, -3.2), rot=(-0.5, 0.25 * sx, 0.2 * sx))
        ant.cube((-0.3, -3.5, -0.3), (0.6, 3.5, 0.6), **mc('chitin_d', clusters=0.0, rim=False))
        tip = ant.part(f'{side}_antenna_tip', pivot=(0, -3.5, 0), rot=(-0.9, 0, 0))
        tip.cube((-0.25, -3, -0.25), (0.5, 3, 0.5), **mc('chitin_d', clusters=0.0, rim=False))
        tip.cube((-0.6, -4, -0.6), (1.2, 1.2, 1.2), color='crystal', pattern='crystal', glow=True)
        mand = head.part(f'{side}_mandible', pivot=(1.3 * sx, 1.2, -3.8), rot=(0, -0.3 * sx, 0))
        mand.cube((-0.5, -0.5, -2), (1, 1, 2), **mc('chitin_d', clusters=0.0, rim=False))
        jaw = head.part(f'{side}_crystal_jaw', pivot=(1.5 * sx, 1.0, -3.6), rot=(0, -0.35 * sx, 0))
        jaw.cube((-0.75, -1, -4), (1.5, 2, 4), color='crystal', pattern='crystal', glow=True)
        jaw.cube((-0.5 - 0.6 * sx, -0.6, -4.2), (1, 1.2, 1.4), color='crystal_l', pattern='crystal', glow=True)
    gaster = body.part('gaster', pivot=(0, -0.5, 2.5), rot=(-0.2, 0, 0))
    gaster.cube((-3, -3, 0), (6, 5.5, 7), color='chitin', pattern='stripes', accent='chitin_d', period=3, faces={
        'down': mc('belly', clusters=0.1)})
    for i, (x, z, h) in enumerate(((0, 2, 3), (-1.6, 4, 2.4), (1.5, 4.5, 2.2), (0, 5.6, 1.8))):
        shard = gaster.part(f'shard_{i}', pivot=(x, -3, z), rot=(-0.3, 0.4 * (i - 1.5), 0.25 * (1 if i % 2 else -1)))
        shard.cube((-0.6, -h, -0.6), (1.2, h, 1.2), color='crystal', pattern='crystal', glow=True)
    for i, z in enumerate((-1.6, 0.0, 1.6)):
        for side, sx in (('left', 1), ('right', -1)):
            leg = body.part(f'{side}_leg_{i}', pivot=(2 * sx, 1, z), rot=(0, (0.5 - 0.5 * i) * sx, 0.5 * sx))
            leg.cube((0 if sx > 0 else -4, -0.5, -0.5), (4, 1, 1), **mc('leg', clusters=0.0, rim=False))
            foot = leg.part(f'{side}_foot_{i}', pivot=(4 * sx, 0, 0), rot=(0, 0, 0.9 * sx))
            foot.cube((0 if sx > 0 else -4, -0.4, -0.4), (4, 0.8, 0.8), **mc('leg', clusters=0.0, rim=False))
    return m


MODELS = {'caravan': caravan}
