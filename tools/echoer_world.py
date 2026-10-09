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
    for i in ('siftite_ingot', 'siftite_dust'):
        tag('item', f'{NS}:echoer_offerings', rl(i))
    tag('item', f'{NS}:echoer_offerings', {'id': f'{NS}:prism_gem', 'required': False})

    book = D.item('minecraft:book', 8, extra=[D.ENCHANT])
    sheets = [D.item(f'music_sheet_{s}', 3) for s in ('nib', 'golem', 'crystal', 'whale', 'lullaby')]
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
        f'message.{NS}.echoer.waiting': 'The Echoer tucks your offering away and waits, its chimes stirring... ring The Offering on Wind Chimes.',  # C4
        f'message.{NS}.soul_golem.energy': 'Soul energy: %s%%',
        f'message.{NS}.soul_golem.slumped': 'Your Soul Golem has run down. Play it some music - the Golem Hymn fills it up.',
        f'message.{NS}.soul_golem_core.needs_soil': 'The core needs a body: use it on a block of soul soil.',
        f'codex.{NS}.enchoer.title': 'Echoer', f'codex.{NS}.enchoer.tagline': 'The singing speaker-bat',  # CR1 Echoer
        f'codex.{NS}.enchoer.body': 'A flying speaker-bat: woofer chest, tweeter ears, a brass drill snout that whirrs when it sings. It pings about with echolocation rings and takes offerings, not trades: drop a Siftite or Serbim ingot or Prism Gem by it, then play The Offering on Wind Chimes. It dances and gives something precious - no song, and you get the gift back.',
        f'codex.{NS}.soul_golem.title': 'Soul Golem', f'codex.{NS}.soul_golem.tagline': 'A little digger of the old days',
        f'codex.{NS}.soul_golem.body': 'Round soulstone constructs with lamp-lit eyes that keep house for Echoers at their pale hearths in the meadows, picking up anything shiny and peering at suspicious blocks. Use one empty-handed to be shown its find. Set a Soul Golem Core into soul soil to build your own: it follows you and sifts the ground nearby, now and then turning up a gem. It runs on soul energy, which drains as it works; at zero it slumps. Any music recharges it a little - the Golem Hymn completely.',
        f'codex.{NS}.nib.title': 'Nibs', f'codex.{NS}.nib.tagline': 'Wisps of the flower meadows',
        f'codex.{NS}.nib.body': 'Tiny glowing butterfly-wisps that flutter in loose flocks over the Sift\'s meadows, trailing sparkles and resting on flowers. Play the Song of the Nibs and every Nib within twelve blocks swirls up around you and turns into treasure: mostly Nib Dust, sometimes gold, emeralds, amethyst or echo shards, and once in a long while a diamond.',
        f'block.{NS}.echoer_hut_heart': 'Echoer\'s Hearthstone',
    })
    L.update(__import__('echoer_drill').LANG)  # RR: the Echoer Drill
    return L
