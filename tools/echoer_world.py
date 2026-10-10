"""A2 Echoer: the data, assets and art around the Echoer - its ceremony rewards, Soul Golems and
their cores, Nibs and Nib Dust, codex pages and lang (RR: the Echoer device is the Echoer Drill, tools/echoer_drill.py). (W1: the
Echoer's Hut structure is gone; its Hearthstone now sits in the 'echoer_hearth' feature, tools/sculk_world.py.)

Hooked from: gen_assets.gen_block (models 'echoer_device', 'echoer_hut_heart'), gen_assets.generate
(generate), gen_assets.gen_lang (lang), gen_textures (block_textures), items16 (item_sprites)."""
import math
import os

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
        __import__('echoer_drill').gen_block(GA, bid)  # RR: the Echoer Drill (tools/echoer_drill.py)
    else:
        _heart(GA, bid)


def _heart(GA, bid):
    for spent in (False, True):
        name = bid + ('_spent' if spent else '')
        GA.block_model(name, 'minecraft:block/cube_bottom_top', {
            'top': f'block/{bid}_top' + ('_spent' if spent else ''), 'side': f'block/{bid}_side', 'bottom': f'block/{bid}_side'})
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': {
        'spent=false': {'model': f'{NS}:block/{bid}'}, 'spent=true': {'model': f'{NS}:block/{bid}_spent'}}})


# ============================================================================ block textures


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
    __import__('echoer_drill').block_textures(out)  # RR: the Echoer Drill's housing (tools/echoer_drill.py)
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
    import echoer as E
    out = {
        'enchoer_spawn_egg': E.echoer_egg(),  # M3: the deer spirit's egg (replaces items16's)
        'soul_golem_core': I.grid(core, pal, ol=False),
        'nib_dust': I.grid(dust, dpal, ol=True, no_ol='SWs'),
        # M3: the clawed mole golem and the meadow wisp, in vanilla's per-mob egg style (tools/echoer.py)
        'soul_golem_spawn_egg': E.golem_egg(),
        'nib_spawn_egg': E.nib_egg(),
    }
    return out


# ============================================================================ data


def _gifts():
    """M3: what an Echoer gives for each song (vanilla treasures and the Sift's own, in the song's spirit)."""
    import gen_data as D
    i = D.item
    return {
        'offering': [i('skysong_gem', 2), i('soul_golem_core', 4), i('minecraft:diamond', 6, count=(1, 2)), i('star_shard', 8, count=(2, 4)),
                     i('minecraft:emerald', 6, count=(3, 6))],
        'nib': [i('nib_dust', 10, count=(3, 6)), i('minecraft:gold_ingot', 6, count=(2, 4)), i('minecraft:glow_berries', 4, count=(4, 8)),
                i('minecraft:amethyst_shard', 6, count=(3, 6)), i('music_sheet_nib', 2)],
        'golem': [i('soul_golem_core', 4), i('soul_dust', 8, count=(3, 6)), i('minecraft:lapis_lazuli', 6, count=(6, 12)),
                  i('minecraft:copper_ingot', 4, count=(6, 10)), i('music_sheet_golem', 2)],
        'crystal': [i('prism_gem', 8, count=(1, 3)), i('minecraft:amethyst_shard', 6, count=(4, 8)), i('minecraft:diamond', 4),
                    i('minecraft:quartz', 4, count=(6, 12)), i('music_sheet_crystal', 2)],
        'whale': [i('chrome_pearl', 8, count=(2, 4)), i('minecraft:nautilus_shell', 6, count=(1, 2)), i('minecraft:prismarine_crystals', 6, count=(4, 8)),
                  i('minecraft:heart_of_the_sea', 1), i('music_sheet_whale', 2)],
        'lullaby': [i('music_disc_lullaby', 3), i('swifter_fluff', 6, count=(2, 4)), i('minecraft:phantom_membrane', 4, count=(1, 3)),
                    i('minecraft:emerald', 6, count=(3, 6)), i('music_sheet_lullaby', 2)],
        'aurora': [i('prism_gem', 6, count=(1, 2)), i('star_shard', 8, count=(2, 5)), i('minecraft:glowstone_dust', 4, count=(8, 16)),
                   i('minecraft:firework_rocket', 4, count=(4, 8)), i('minecraft:diamond', 3)],
        'heartbeat': [i('minecraft:golden_apple', 6), i('minecraft:blaze_rod', 4, count=(2, 4)), i('minecraft:experience_bottle', 6, count=(4, 8)),
                      i('minecraft:emerald', 6, count=(3, 6))],
        'dolphin': [i('minecraft:heart_of_the_sea', 2), i('minecraft:nautilus_shell', 6, count=(1, 3)), i('chrome_pearl', 8, count=(2, 4)),
                    i('minecraft:trident', 1), i('minecraft:prismarine_crystals', 5, count=(4, 8))],
        'canon': [i('minecraft:experience_bottle', 8, count=(6, 12)), i('minecraft:lapis_lazuli', 6, count=(8, 16)), i('minecraft:diamond', 4),
                  i('minecraft:amethyst_shard', 4, count=(4, 8))],
        # S1 land: the Creator's Hymn - the Echoer gives back something of the Creator's
        'hymn': [i('minecraft:golden_apple', 4), i('minecraft:gold_ingot', 8, count=(2, 5)), i('minecraft:experience_bottle', 6, count=(3, 6)),
                 i('minecraft:amethyst_shard', 4, count=(3, 6)), i('minecraft:enchanted_golden_apple', 1)],
        'requiem': [i('minecraft:echo_shard', 8, count=(1, 3)), i('minecraft:disc_fragment_5', 6, count=(1, 2)),
                    i('minecraft:recovery_compass', 2), i('sculkite', 6, count=(1, 2)), i('minecraft:sculk_catalyst', 3)],
    }


def generate(GA):
    ECHOER_GIFTS = _gifts()
    import gen_data as D
    tag, rl = GA.tag, GA.rl
    # --- M3: the Echoer's gifts. Every song has its own (thesift:gameplay/echoer_gift/<song>), and one gift in twenty
    # brings something truly rare along with it; the Echoer gives each player one gift every ten minutes (Enchoer.java)
    book = D.item('minecraft:book', 4, extra=[D.ENCHANT])
    D.table('gift', 'gameplay/echoer_gift/rare', [D.pool([
        D.item('minecraft:enchanted_golden_apple', 2), D.item('minecraft:totem_of_undying', 2), D.item('minecraft:heart_of_the_sea', 2),
        D.item('minecraft:music_disc_otherside', 2), D.item('minecraft:music_disc_relic', 2), D.item('skysong_gem', 3),
        D.item('echoer_device', 2), D.item('minecraft:netherite_scrap', 2)])])
    rare = {'type': 'minecraft:loot_table', 'value': f'{NS}:gameplay/echoer_gift/rare', 'weight': 1}
    for song, entries in ECHOER_GIFTS.items():
        D.table('gift', f'gameplay/echoer_gift/{song}', [D.pool(entries + [book]), D.pool([rare], condition=D.chance(0.05))])
    D.table('gift', 'gameplay/soul_golem_dig', [D.pool([
        D.item('minecraft:gold_nugget', 10, count=(2, 5)), D.item('minecraft:lapis_lazuli', 8, count=(2, 4)),
        D.item('minecraft:amethyst_shard', 8, count=(1, 3)), D.item('minecraft:quartz', 6, count=(1, 3)),
        D.item('minecraft:emerald', 6), D.item('star_shard', 3), D.item('siftite_dust', 3), D.item('chrome_pearl', 2), D.item('soul_dust', 8, count=(1, 3)),
        D.item('minecraft:diamond', 1)])])
    D.table('gift', 'gameplay/nib_transform', [D.pool([
        D.item('nib_dust', 60, count=(1, 2)), D.item('minecraft:gold_nugget', 12, count=(2, 4)), D.item('minecraft:gold_ingot', 5),
        D.item('minecraft:emerald', 6), D.item('minecraft:amethyst_shard', 8, count=(1, 2)), D.item('minecraft:echo_shard', 3),
        D.item('minecraft:diamond', 1)])])
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
        f'message.{NS}.echoer.no_gift': 'The Echoer dances to your song, but has no gift for you yet.',  # M3
        f'message.{NS}.soul_golem.energy': 'Soul energy: %s%%',
        f'message.{NS}.soul_golem.slumped': 'Your Soul Golem has run down. Play it some music - the Golem Hymn fills it up.',
        f'message.{NS}.soul_golem_core.needs_soil': 'The core needs a body: use it on a block of soul soil.',
        f'codex.{NS}.enchoer.title': 'Echoer', f'codex.{NS}.enchoer.tagline': 'The deer spirit that sings',  # M3 Echoer
        f'codex.{NS}.enchoer.body': 'A deer spirit of the meadows with great pale antlers whose tines glow as it sings - and it sings all the time, so you hear one long before you see it, skipping through the air on glints of light. Play near it and it stops to listen. Finish a song and it comes to you, bows its antlers, and a gift rises out of them for you to take: each song has gifts of its own, and once in a while something truly rare comes with it. An Echoer gives each player one gift every ten minutes; until then it only dances to your songs.',
        f'codex.{NS}.soul_golem.title': 'Soul Golem', f'codex.{NS}.soul_golem.tagline': 'A little digger of the old days',
        f'codex.{NS}.soul_golem.body': 'Round soulstone constructs with lamp-lit eyes that keep house for Echoers at their pale hearths in the meadows, picking up anything shiny and peering at suspicious blocks. Use one empty-handed to be shown its find. Set a Soul Golem Core into soul soil to build your own: it follows you and sifts the ground nearby, now and then turning up a gem. It runs on soul energy, which drains as it works; at zero it slumps. Any music recharges it a little - the Golem Hymn completely.',
        f'codex.{NS}.nib.title': 'Nibs', f'codex.{NS}.nib.tagline': 'Wisps of the flower meadows',
        f'codex.{NS}.nib.body': 'Tiny glowing butterfly-wisps that flutter in loose flocks over the Sift\'s meadows, trailing sparkles and resting on flowers. Play the Song of the Nibs and every Nib within twelve blocks swirls up around you and turns into treasure: mostly Nib Dust, sometimes gold, emeralds, amethyst or echo shards, and once in a long while a diamond.',
        f'block.{NS}.echoer_hut_heart': 'Echoer\'s Hearthstone',
    })
    L.update(__import__('echoer_drill').LANG)  # RR: the Echoer Drill
    return L
