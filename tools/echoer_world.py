"""A2 Echoer: the data, assets and art around the Echoer - its ceremony rewards, Soul Golems and
their cores, Nibs and Nib Dust, The Echoer (mining-beam device), the Echoer's Hut structure and
its musical gardens, codex pages and lang.

Hooked from: gen_assets.gen_block (models 'echoer_device', 'echoer_hut_heart'), gen_assets.generate
(generate), gen_assets.gen_lang (lang), gen_textures (block_textures), items16 (item_sprites),
gen_structures (echoer_hut)."""
import math
import os
import random

from PIL import Image

NS = 'thesift'

SOUL = ['#2a221c', '#3a2f27', '#4b3e34', '#5d4d40', '#6c5a4b', '#87725f']
CYAN = ['#16566a', '#2aa9c8', '#5fe9ff', '#b8fbff', '#ffffff']
PALE = ['#a99fb8', '#c4bccf', '#d9d2e2', '#e9e4ef', '#f6f3f9']


def _hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def _tex(fn, w=16, h=16):
    img = Image.new('RGBA', (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            px[x, y] = _hx(fn(x, y))
    return img


# ============================================================================ block models


def gen_block(GA, bid, kind):
    if kind == 'echoer_device':
        _device(GA, bid)
    else:
        _heart(GA, bid)


def _device(GA, bid):
    """A soulstone horn: the facing side is the mouth (concentric rings that blaze while it
    charges), the back a vent grille, the sides carved with a running rune."""
    for lit in (False, True):
        name = bid + ('_charging' if lit else '')
        m = {'parent': 'minecraft:block/block', 'textures': {
            'particle': f'{NS}:block/{bid}_side', 'front': f'{NS}:block/{bid}_front' + ('_lit' if lit else ''),
            'side': f'{NS}:block/{bid}_side', 'top': f'{NS}:block/{bid}_top', 'back': f'{NS}:block/{bid}_back'},
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
                'down': {'uv': [0, 0, 16, 16], 'texture': '#top', 'cullface': 'down'},
                'up': {'uv': [0, 16, 16, 0], 'texture': '#top', 'cullface': 'up'},
                'north': {'uv': [0, 0, 16, 16], 'texture': '#front', 'cullface': 'north'},
                'south': {'uv': [0, 0, 16, 16], 'texture': '#back', 'cullface': 'south'},
                'west': {'uv': [0, 0, 16, 16], 'texture': '#side', 'cullface': 'west'},
                'east': {'uv': [0, 0, 16, 16], 'texture': '#side', 'cullface': 'east'}}}]}
        GA.note_textures(m)
        GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
    rot = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    variants = {}
    for facing, r in rot.items():
        for charging in ('false', 'true'):
            for powered in ('false', 'true'):
                for rng in range(3):
                    v = {'model': f'{NS}:block/{bid}' + ('_charging' if charging == 'true' else '')}
                    v.update(r)
                    variants[f'charging={charging},facing={facing},powered={powered},range={rng}'] = v
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})
    GA.item_block(bid)


def _heart(GA, bid):
    for spent in (False, True):
        name = bid + ('_spent' if spent else '')
        GA.block_model(name, 'minecraft:block/cube_bottom_top', {
            'top': f'block/{bid}_top' + ('_spent' if spent else ''), 'side': f'block/{bid}_side', 'bottom': f'block/{bid}_side'})
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': {
        'spent=false': {'model': f'{NS}:block/{bid}'}, 'spent=true': {'model': f'{NS}:block/{bid}_spent'}}})


# ============================================================================ block textures


def _device_front(lit):
    def fn(x, y):
        d = max(abs(x - 7.5), abs(y - 7.5))
        r = math.hypot(x - 7.5, y - 7.5)
        if d >= 7.0:
            return SOUL[1] if (x + y) % 2 else SOUL[2]
        if d >= 6.0:
            return SOUL[4] if y < 8 else SOUL[3]
        if r < 1.6:
            return CYAN[4] if lit else SOUL[0]
        ring = int(r) % 2 == 0
        if lit:
            return CYAN[3] if ring else CYAN[2]
        return CYAN[1] if ring and r < 5.5 else (SOUL[2] if r > 4 else SOUL[1])
    return _tex(fn)


def _device_side(seed=5):
    rnd = random.Random(seed)
    noise = [[rnd.random() for _ in range(16)] for _ in range(16)]
    rune = {(3, 7), (4, 6), (4, 8), (5, 7), (7, 6), (7, 7), (7, 8), (8, 7), (10, 6), (11, 7), (10, 8), (12, 7)}

    def fn(x, y):
        if (x, y) in rune:
            return CYAN[2]
        if y in (0, 15) or x in (0, 15):
            return SOUL[1]
        if y in (4, 11):
            return SOUL[2]
        if (y < 4 and x == 8) or (y > 11 and x == 5):
            return SOUL[2]
        t = 4 if noise[y][x] > 0.8 else 3 if noise[y][x] > 0.25 else 2
        return SOUL[t] if y < 8 else SOUL[t - 1] if t > 2 else SOUL[2]
    return _tex(fn)


def _device_top():
    def fn(x, y):
        r = math.hypot(x - 7.5, y - 7.5)
        if x in (0, 15) or y in (0, 15):
            return SOUL[1]
        if 4.0 < r < 5.2:
            return CYAN[1]
        if r <= 2.2:
            return SOUL[0] if r < 1.2 else CYAN[0]
        return SOUL[4] if (x * 7 + y * 3) % 11 == 0 else SOUL[3]
    return _tex(fn)


def _device_back():
    def fn(x, y):
        if x in (0, 15) or y in (0, 15):
            return SOUL[1]
        if 3 <= x <= 12 and 3 <= y <= 12:
            return SOUL[0] if y % 2 else SOUL[3]
        return SOUL[3] if (x + y) % 4 else SOUL[2]
    return _tex(fn)


def _heart_top(spent):
    def fn(x, y):
        r = math.hypot(x - 7.5, y - 7.5)
        if x in (0, 15) or y in (0, 15):
            return PALE[1]
        on = 5.0 < r < 6.2 or r < 1.8 or (abs(x - 7.5) < 0.6 and r < 5.0) or (abs(y - 7.5) < 0.6 and r < 5.0)
        if on:
            return (CYAN[1] if spent else (CYAN[3] if r < 1.8 else CYAN[2]))
        return PALE[3] if (x + 2 * y) % 7 else PALE[2]
    return _tex(fn)


def _heart_side():
    def fn(x, y):
        if y in (0, 7, 15):
            return PALE[1]
        if (y < 7 and x == 7) or (y > 7 and x in (3, 12)):
            return PALE[1]
        return PALE[3] if (x * 5 + y) % 9 else PALE[4]
    return _tex(fn)


def block_textures(out):
    out('block/echoer_device_front', _device_front(False))
    out('block/echoer_device_front_lit', _device_front(True))
    out('block/echoer_device_side', _device_side())
    out('block/echoer_device_top', _device_top())
    out('block/echoer_device_back', _device_back())
    out('block/echoer_hut_heart_top', _heart_top(False))
    out('block/echoer_hut_heart_top_spent', _heart_top(True))
    out('block/echoer_hut_heart_side', _heart_side())


# ============================================================================ item sprites


def item_sprites():
    import items16 as I
    core = [
        '................',
        '......kkkk......',
        '....kk4433kk....',
        '...k443322ddk...',
        '..k4432CC21ddk..',
        '..k432CccC21dk..',
        '.k443CcWWcC21dk.',
        '.k43CcWWWWcC1dk.',
        '.k43CcWWWWcC1dk.',
        '.k432CcWWcC21dk.',
        '..k432CccC21dk..',
        '..kd432CC221dk..',
        '...kdd3322ddk...',
        '....kkdd11kk....',
        '......kkkk......',
        '................',
    ]
    pal = {'k': '#1a1410', '4': '#87725f', '3': '#6c5a4b', '2': '#5d4d40', '1': '#4b3e34', 'd': '#3a2f27',
           'C': '#2aa9c8', 'c': '#5fe9ff', 'W': '#e8fdff'}
    dust = [
        '................',
        '.......S........',
        '......SWS....s..',
        '.......S........',
        '...s............',
        '..........S.....',
        '.........SWS....',
        '....s.....S.....',
        '.......pp.......',
        '.....ppYYpp.....',
        '....pYYyyYYp....',
        '...pYyyWyyyYp...',
        '..pYyyyyyyyyYp..',
        '..ppppppppppppp.',
        '................',
        '................',
    ]
    dust = [r[:16].ljust(16, '.') for r in dust]
    dpal = {'p': ('#d98ac9', '#5a2a55'), 'Y': ('#ffe7a0', '#5a2a55'), 'y': ('#ffd36b', '#5a2a55'), 'W': '#ffffff',
            'S': '#fff3b0', 's': '#ffb8ea'}
    out = {
        'soul_golem_core': I.grid(core, pal, ol=False),
        'nib_dust': I.grid(dust, dpal, ol=True, no_ol='SWs'),
        # the Soul Golem: two glowing eye domes and its lamp stalk on a soulstone egg
        'soul_golem_spawn_egg': I.egg(['#3a2f27', '#4b3e34', '#6c5a4b', '#87725f'], '#1a1410', {
            0: '.......l........',
            1: '......lLl.......',
            2: '.......s........',
            4: '....EEE..EEE....',
            5: '....EeE..EeE....',
            8: '.....mmmmmm.....',
            11: '....c.....c.....',
            12: '.....c...c......',
        }, {'l': '#7ff3ff', 'L': '#d6fdff', 's': '#3a2f27', 'E': '#a9faff', 'e': '#ffffff', 'm': '#1a1410', 'c': '#5fe9ff'},
            no_ol='c'),
        # the Nib: a little glowing body with two wing-pairs spread across a twilight egg
        'nib_spawn_egg': I.egg(['#3b4aa0', '#5566c0', '#7a8ce0', '#a0b4f4'], '#141a40', {
            3: '..WW........WW..',
            4: '.WvWW......WWvW.',
            5: '.WWvWW.bb.WWvWW.',
            6: '..WWvW.bb.WvWW..',
            7: '...WWW.bb.WWW...',
            8: '....ww.bb.ww....',
            9: '...wsw....wsw...',
            10: '....ww....ww....',
        }, {'W': '#9ff4ff', 'v': '#ffffff', 'w': '#62c9f0', 's': '#ff9be3', 'b': '#fff3b0'}),
    }
    return out


# ============================================================================ data


def generate(GA):
    import gen_data as D
    tag, rl = GA.tag, GA.rl
    # --- the Echoer's offerings (the prism gem comes from the Caravans' cavern, if it is there)
    for i in ('siftite_ingot', 'serbim_ingot'):
        tag('item', f'{NS}:echoer_offerings', rl(i))
    tag('item', f'{NS}:echoer_offerings', {'id': f'{NS}:prism_gem', 'required': False})

    book = D.item('minecraft:book', 8, extra=[D.ENCHANT])
    sheets = [D.item(f'music_sheet_{s}', 3) for s in ('nib', 'golem', 'crystal', 'whale', 'tide', 'lullaby')]
    D.table('gift', 'gameplay/echoer_reward', [D.pool(sheets + [
        book,
        D.item('minecraft:diamond', 6, count=(1, 2)),
        D.item('minecraft:emerald', 8, count=(2, 5)),
        D.item('star_shard', 8, count=(1, 3)),
        D.item('chrome_pearl', 6, count=(2, 4)),
        D.item('skysong_gem', 2),
        D.item('minecraft:amethyst_shard', 5, count=(3, 6)),
        D.item('soul_golem_core', 3),
        D.item('echoer_device', 1),
    ])])
    D.table('gift', 'gameplay/soul_golem_dig', [D.pool([
        D.item('minecraft:gold_nugget', 10, count=(2, 5)), D.item('minecraft:lapis_lazuli', 8, count=(2, 4)),
        D.item('minecraft:amethyst_shard', 8, count=(1, 3)), D.item('minecraft:quartz', 6, count=(1, 3)),
        D.item('minecraft:emerald', 6), D.item('star_shard', 3), D.item('raw_serbim', 3), D.item('chrome_pearl', 2),
        D.item('minecraft:diamond', 1)])])
    D.table('gift', 'gameplay/nib_transform', [D.pool([
        D.item('nib_dust', 60, count=(1, 2)), D.item('minecraft:gold_nugget', 12, count=(2, 4)), D.item('minecraft:gold_ingot', 5),
        D.item('minecraft:emerald', 6), D.item('minecraft:amethyst_shard', 8, count=(1, 2)), D.item('minecraft:echo_shard', 3),
        D.item('minecraft:diamond', 1)])])
    # --- the hut's chest: always the Offering's sheet, so a visitor can learn the ceremony
    D.table('chest', 'chests/echoer_hut', [
        D.pool([D.item('music_sheet_offering')]),
        D.pool([D.item('nib_dust', 4, count=(1, 3)), D.item('chrome_pearl', 4, count=(1, 3)), D.item('star_shard', 3),
                D.item('siftite_nugget', 3, count=(1, 4)), D.item('glowcap', 3, count=(2, 4)), D.item('minecraft:amethyst_shard', 3, count=(2, 4)),
                D.item('minecraft:book', 2, extra=[D.ENCHANT]), D.item('serbim_ingot', 1)], rolls=(3, 5)),
        D.pool([D.item('music_sheet_golem'), D.item('music_sheet_nib')], condition=D.chance(0.35)),
        D.pool([D.item('soul_golem_core')], condition=D.chance(0.08)),
    ])
    # --- creature drops
    D.table('entity', 'entities/soul_golem', [D.pool([D.item('minecraft:soul_soil', count=(0, 1))]),
                                              D.pool([D.item('soul_golem_core')], condition={'type': 'minecraft:all_of', 'terms': [
                                                  D.PLAYER_KILL, D.chance(0.12)]})])
    D.table('entity', 'entities/nib', [D.pool([D.item('nib_dust', count=(0, 1), extra=[D.LOOTING])])])
    # --- Nibs live in the Sift's flower meadows (the Sound Garden adds its own)
    GA.write(os.path.join(GA.RES, 'data', NS, 'neoforge', 'biome_modifier', 'nib_meadows.json'), {
        'type': 'neoforge:add_spawns', 'biomes': [f'{NS}:wishing_grove', f'{NS}:sift_plains'],
        'spawners': [{'type': f'{NS}:nib', 'count': {'type': 'minecraft:uniform', 'min_inclusive': 3, 'max_inclusive': 6}, 'weight': 10}]})


def lang():
    L = {}
    for e, n in (('soul_golem', 'Soul Golem'), ('nib', 'Nib')):
        L[f'entity.{NS}.{e}'] = n
    L.update({
        f'message.{NS}.echoer.waiting': 'The Echoer tucks your offering away and waits, humming... play it The Offering.',
        f'message.{NS}.soul_golem.energy': 'Soul energy: %s%%',
        f'message.{NS}.soul_golem.slumped': 'Your Soul Golem has run down. Play it some music - the Golem Hymn fills it up.',
        f'message.{NS}.soul_golem_core.needs_soil': 'The core needs a body: use it on a block of soul soil.',
        f'message.{NS}.echoer_device.range': 'The Echoer will reach %s blocks',
        f'codex.{NS}.enchoer.title': 'Echoer', f'codex.{NS}.enchoer.tagline': 'Keeper of the offering song',
        f'codex.{NS}.enchoer.body': 'A tall, moon-pale grazer with an endless swaying neck and runes of light on its flanks. It lives in a dome-shaped hut among singing gardens. It does not trade - it accepts offerings. Drop a Siftite or Serbim ingot (or a Prism Gem) near it: it sniffs the gift, tucks it away and waits a minute, humming the first notes of The Offering. Play the whole song and it dances and gives you something precious - music sheets, gems, enchanted books, rarely a Soul Golem Core, very rarely The Echoer itself. No song, and it hands the gift back, crestfallen. It bows to visitors, hums along to music and naps when alone.',
        f'codex.{NS}.soul_golem.title': 'Soul Golem', f'codex.{NS}.soul_golem.tagline': 'A little digger of the old days',
        f'codex.{NS}.soul_golem.body': 'Round soulstone constructs with lamp-lit eyes that waddle around the Echoer\'s Hut, picking up anything shiny and peering at suspicious blocks. Use one empty-handed to be shown its find. Set a Soul Golem Core into soul soil to build your own: it follows you and sifts the ground nearby, now and then turning up a gem. It runs on soul energy, which drains as it works; at zero it slumps. Any music recharges it a little - the Golem Hymn completely.',
        f'codex.{NS}.nib.title': 'Nibs', f'codex.{NS}.nib.tagline': 'Wisps of the flower meadows',
        f'codex.{NS}.nib.body': 'Tiny glowing butterfly-wisps that flutter in loose flocks over the Sift\'s meadows, trailing sparkles and resting on flowers. Play the Song of the Nibs and every Nib within twelve blocks swirls up around you and turns into treasure: mostly Nib Dust, sometimes gold, emeralds, amethyst or echo shards, and once in a long while a diamond.',
        f'codex.{NS}.echoer_device.title': 'The Echoer', f'codex.{NS}.echoer_device.tagline': 'A horn that mines with sound',
        f'codex.{NS}.echoer_device.body': 'An ancient soulstone horn, given only by Echoers. Power it with redstone and it charges, then fires an echo beam out of its face that shatters the first block in its path - as far as the signal strength in blocks. Use it to fire at its dial range; sneak-use turns the dial (4, 8 or 16). Drops go into a container touching it, or pop out of its top. It will not break unbreakable blocks, or chests and anything else that holds things.',
        f'codex.{NS}.echoer_hut.title': 'Echoer\'s Hut', f'codex.{NS}.echoer_hut.tagline': 'A pale dome among singing gardens',
        f'codex.{NS}.echoer_hut.body': 'A rare igloo of pale stone with glowing windows, home to an Echoer and its Soul Golems. Around it grow musical gardens: Choir Lilies and Echo Orchids, chime arches, a still pond. Inside are its bed, shelves, a soul chime and a chest that always holds the sheet of The Offering.',
        f'block.{NS}.echoer_hut_heart': 'Echoer\'s Hearthstone',
    })
    return L


# ============================================================================ the Echoer's Hut


def echoer_hut(seed):
    """A pale dome (an igloo of calcite and quartz) with glowing froglight windows and a short
    entrance tunnel, inside a garden ring: Choir Lily and Echo Orchid beds, chime arches, a pond
    with lily pads and chrome reeds, stepping stones and lanterns. Inside: bed, shelves, a soul
    chime, a chest and the hearthstone that wakes the household."""
    from structlib import AIR, B, Build, chest
    S = 35
    c = S // 2
    b = Build(S, 13, S, seed)
    rnd = b.rnd
    GRASS = B('sift_grass_block', snowy='false')
    SOIL = B('sift_soil')
    POL = B('polished_dreamstone')
    CALCITE = B('minecraft:calcite')
    QUARTZ = B('minecraft:smooth_quartz')
    WINDOW = B('minecraft:pearlescent_froglight', axis='y')
    LOG = B('lullwood_log', axis='y')
    PLANK = B('lullwood_planks')
    CHIME = B('soul_chime', powered='false')
    LANTERN = B('bulb_lantern', hanging='false', waterlogged='false')
    LANTERN_H = B('bulb_lantern', hanging='true', waterlogged='false')
    FLOWERS = [B('lullaby_bell', resonating='false'), B('soulpetal', resonating='false'), B('echo_orchid'), B('glimmer_sprouts'),
               B('dreambloom', resonating='false')]

    # ground: soil below, grass on top, a round garden plot
    b.cyl(c, c, 0, 0, c - 0.5, SOIL)
    b.cyl(c, c, 1, 1, c - 0.5, GRASS)

    # the dome: a hollow half-sphere, radius 7, floor at y=2
    R = 7.0
    shell = b.mix((CALCITE, 7), (QUARTZ, 2), (B('minecraft:polished_diorite'), 1))
    for x in range(c - 8, c + 9):
        for z in range(c - 8, c + 9):
            for y in range(1, 11):
                d = math.sqrt((x - c) ** 2 + (z - c) ** 2 + ((y - 1) * 1.05) ** 2)
                if d <= R + 0.35:
                    b.set(x, y, z, shell(x, y, z) if d > R - 0.85 or y == 1 else AIR)
    # the floor inside
    b.cyl(c, c, 1, 1, R - 0.6, POL)
    b.cyl(c, c, 1, 1, 2.2, B('chiseled_dreamstone'))
    b.set(c, 1, c, B('echoer_hut_heart', spent='false'))
    # glowing windows round the dome and a skylight
    for k in range(8):
        a = k * math.pi / 4
        if k == 2:
            continue  # the door side (south, +z)
        x, z = c + round(math.cos(a) * 6.6), c + round(math.sin(a) * 6.6)
        for y in (4, 5):
            b.set(x, y, z, WINDOW)
    b.set(c, 8, c, WINDOW)
    # the entrance tunnel (south) and its doorway
    for z in range(c + 5, c + 10):
        for x in range(c - 2, c + 3):
            for y in range(1, 6):
                edge = x in (c - 2, c + 2) or y == 5
                b.set(x, y, z, shell(x, y, z) if edge else (POL if y == 1 else AIR))
    for x in (c - 1, c, c + 1):
        b.set(x, 5, c + 9, CALCITE)
    b.set(c, 4, c + 9, LANTERN_H)

    # inside: bed (north-west), shelves along the north wall, chest, a chime from the ceiling, lights
    b.set(c - 4, 2, c - 2, B('minecraft:white_bed', part='foot', facing='north', occupied='false'))
    b.set(c - 4, 2, c - 3, B('minecraft:white_bed', part='head', facing='north', occupied='false'))
    for x in range(c - 2, c + 3):
        b.set(x, 2, c - 5, B('minecraft:bookshelf'))
        b.set(x, 3, c - 5, B('minecraft:bookshelf') if x % 2 else B('minecraft:chiseled_bookshelf', facing='south', **{
            f'slot_{i}_occupied': 'false' for i in range(6)}))
    b.set(c + 4, 2, c - 2, chest('chests/echoer_hut', 'west'))
    b.set(c + 4, 2, c + 1, B('sift_drum', hit='0', core='false', powered='false'))
    b.set(c, 7, c - 2, B('minecraft:iron_chain', axis='y'))
    b.set(c, 6, c - 2, CHIME)
    b.set(c - 3, 7, c + 2, LANTERN_H)
    b.set(c + 3, 7, c + 2, LANTERN_H)
    for (x, z) in ((c - 3, c), (c + 3, c - 3), (c - 1, c + 3)):
        b.set(x, 2, z, B('lumen_moss_carpet'))

    # --- the musical gardens
    def ground_ok(x, z):
        g = b.get(x, 1, z)
        return g is not None and g.name.endswith('sift_grass_block') and (b.get(x, 2, z) is None or b.get(x, 2, z).name == AIR.name)

    # a still pond to the east, with lily pads and reeds
    pc, pz = c + 11, c + 3
    for x in range(pc - 4, pc + 5):
        for z in range(pz - 4, pz + 5):
            d = math.hypot((x - pc) / 3.6, (z - pz) / 3.0)
            if d < 1.0:
                b.set(x, 1, z, B('minecraft:water', level=0))
                b.set(x, 0, z, B('minecraft:water', level=0) if d < 0.6 else SOIL)
                if rnd.random() < 0.15:
                    b.set(x, 2, z, B('minecraft:lily_pad'))
            elif d < 1.3 and ground_ok(x, z) and rnd.random() < 0.4:
                b.set(x, 2, z, B('chrome_reeds') if rnd.random() < 0.5 else B('coral_fern'))
    # stepping stones south from the door
    for i, z in enumerate(range(c + 10, S - 1, 2)):
        for dx in (-1, 0) if i % 2 else (0, 1):
            b.set(c + dx, 1, z, POL)
    # chime arches: two log posts, a plank beam, soul chimes hanging under it
    for (ax, az, along_x) in ((c - 11, c - 4, False), (c + 4, c - 12, True), (c - 9, c + 9, True)):
        p1 = (ax, az)
        p2 = (ax + 4, az) if along_x else (ax, az + 4)
        for (x, z) in (p1, p2):
            for y in range(2, 6):
                b.set(x, y, z, LOG)
        for t in range(5):
            x, z = (ax + t, az) if along_x else (ax, az + t)
            b.set(x, 6, z, PLANK)
            if 0 < t < 4:
                b.set(x, 5, z, CHIME)
    # singing flower beds: choir lilies, echo orchids, bells; lanterns on short posts
    for k in range(14):
        a = rnd.random() * math.tau
        r = 9.5 + rnd.random() * 6.0
        x, z = round(c + math.cos(a) * r), round(c + math.sin(a) * r)
        if not ground_ok(x, z):
            continue
        if k % 3 == 0 and ground_ok(x, z):
            b.set(x, 2, z, B('choir_lily', half='lower'))
            b.set(x, 3, z, B('choir_lily', half='upper'))
        for _ in range(5):
            fx, fz = x + rnd.randrange(-2, 3), z + rnd.randrange(-2, 3)
            if ground_ok(fx, fz):
                b.set(fx, 2, fz, rnd.choice(FLOWERS))
    for (x, z) in ((c - 4, c + 11), (c + 4, c + 11), (c - 13, c + 2), (c + 6, c - 9)):
        if ground_ok(x, z):
            b.set(x, 2, z, B('lullwood_fence', north='false', south='false', east='false', west='false', waterlogged='false'))
            b.set(x, 3, z, LANTERN)
    b.overgrow(['sift_grass_block'], [B('blushgrass'), B('blushgrass'), B('glimmer_sprouts'), B('coral_fern')], 0.18)
    return b
