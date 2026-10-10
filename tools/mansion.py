"""MANSION: the Woodland Mansion's secret and the new way into the Sift.

* Every Woodland Mansion hides a SECRET ROOM: a bookcase in the front-north corner corridor of the ground floor hides a
  stair down to a vault under the mansion. Its door opens when the three Loose Bookshelves in the bookcase are pulled
  from the lowest up (a lever inside lets you out). The vault keeps the Music Sheet of THE SIFT SYMPHONY (the song that
  opens the Sift gates), Sift loot, a practice summoner of the illagers' and two guards. Built by the structure
  `thesift:mansion_vault` (worldgen/MansionVaultStructure.java), which shares the mansions' placement, so every mansion
  gets one. Mansion chests also roll Sift loot.
* Two new Pillager types live in mansions and join raids: the Hornblower (a GIANT GOAT HORN blast: a knockback cone) and
  the Bard (his guitar heals the illagers around him): the vanilla illager model, the vanilla pillager's
  texture re-dressed (illager_texture()).
* The GIANT GOAT HORN: crafted from goat horns, copper and leather; use it to blast a cone of knockback (3D in the hand,
  a 16x16 sprite in the inventory).
* The SCULK SUMMONER: right-click a Warden Core into a vanilla Sculk Catalyst; four Sculk Sensors within 8 blocks and a
  Sift gate frame within 16; play the Sift Symphony on any instrument nearby - the sensors light up note by note and the
  gate wakes (block/entity/SculkSummonerBlockEntity.java).

Hooks: spec.py -> declare(); mobs.py -> ALL.update(MODELS); gen_assets.gen_block -> gen_block() for 'mansion_*' kinds;
gen_assets.generate() -> sounds() (before gen_sounds) and data() (loot, recipes, tags, structures, models, text);
items16.all_items() -> items() (spawn eggs, the horn's sprite); gen_textures -> textures() (the summoner's core, the
horn's 3D texture).
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))

NS = 'thesift'
VAN_TEX = os.environ.get('VANILLA_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# =========================================================================== spec

def declare(block, item):
    item('giant_goat_horn', cls='GiantGoatHornItem', props='new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)', tab='combat')
    item('hornblower_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModMansion.HORNBLOWER.get())', tab='eggs')
    item('bard_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModMansion.BARD.get())', tab='eggs')
    # the Sculk Summoner: a Sculk Catalyst with a Warden Core in it (made in the world, never an item)
    block('sculk_summoner', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SCULK_CATALYST).lightLevel(s -> 9)', cls='SculkSummonerBlock',
          model='mansion_summoner', item=False, loot='none', tags=['hoe'], name='Sculk Summoner')
    # the secret room's door and its three loose books (players may build their own)
    block('secret_bookshelf', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).noOcclusion()', cls='SecretBookshelfBlock',
          model='mansion_door', loot='self', tags=['axe'], tab='functional', name='Bookshelf Door')
    block('loose_bookshelf', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF)', cls='LooseBookshelfBlock',
          model='mansion_loose', loot='self', tags=['axe'], tab='functional', name='Loose Bookshelf')


# =========================================================================== block models

def _f(tex, uv, **kw):
    d = {'texture': tex, 'uv': uv}
    d.update(kw)
    return d


def summoner_model(ready):
    side = 'side_bloom' if ready else 'side'
    top = 'top_bloom' if ready else 'top'
    els = [{'from': [0, 0, 0], 'to': [16, 12, 16], 'faces': {
        **{d: _f(f'#{side}', [0, 4, 16, 16]) for d in ('north', 'south', 'east', 'west')},
        'down': _f('#bottom', [0, 0, 16, 16], cullface='down'), 'up': _f(f'#{top}', [0, 0, 16, 16])}}]
    # four bone claws at the corners, curling in over the core
    for x0, z0 in ((0, 0), (12, 0), (0, 12), (12, 12)):
        els.append({'from': [x0 + 0.5, 12, z0 + 0.5], 'to': [x0 + 3.5, 16, z0 + 3.5],
                    'faces': {**{d: _f('#bone', [x0, 0, x0 + 3, 4]) for d in ('north', 'south', 'east', 'west')},
                              'up': _f('#bone_top', [x0, z0, x0 + 3, z0 + 3])}})
        ix = 3.5 if x0 == 0 else 10.5
        iz = 3.5 if z0 == 0 else 10.5
        els.append({'from': [min(ix, x0 + 2), 15, min(iz, z0 + 2)], 'to': [max(ix, x0 + 2) + 2, 17, max(iz, z0 + 2) + 2],
                    'faces': {**{d: _f('#bone', [4, 2, 6, 4]) for d in ('north', 'south', 'east', 'west')},
                              'up': _f('#bone_top', [6, 6, 8, 8]), 'down': _f('#bone_top', [6, 6, 8, 8])}})
    # the Warden Core cradled in the claws, glowing and beating (its texture is animated)
    els.append({'from': [4.5, 12, 4.5], 'to': [11.5, 18.5, 11.5], 'shade': False, 'light_emission': 15,
                'faces': {d: _f('#core', [1, 1, 15, 14]) for d in ('north', 'south', 'east', 'west', 'up')}})
    tex = {'particle': 'minecraft:block/sculk_catalyst_side', 'side': 'minecraft:block/sculk_catalyst_side',
           'side_bloom': 'minecraft:block/sculk_catalyst_side_bloom', 'top': 'minecraft:block/sculk_catalyst_top',
           'top_bloom': 'minecraft:block/sculk_catalyst_top_bloom', 'bottom': 'minecraft:block/sculk_catalyst_bottom',
           'bone': 'minecraft:block/bone_block_side', 'bone_top': 'minecraft:block/bone_block_top', 'core': f'{NS}:block/sculk_summoner_core'}
    return {'parent': 'minecraft:block/block', 'textures': tex, 'elements': els}


def loose_model(pulled):
    """A vanilla bookshelf with one red book loose on its front: it sticks out a hair, or (pulled) tips out."""
    els = [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
        **{d: _f('#side', [0, 0, 16, 16], cullface=d) for d in ('north', 'south', 'east', 'west')},
        'up': _f('#end', [0, 0, 16, 16], cullface='up'), 'down': _f('#end', [0, 0, 16, 16], cullface='down')}}]
    out = 3.0 if pulled else 0.6
    book = {'from': [12, 9, -out], 'to': [14, 15, 0.01], 'faces': {
        'north': _f('#side', [2, 1, 4, 7]), 'east': _f('#side', [2, 1, 3, 7]), 'west': _f('#side', [3, 1, 4, 7]),
        'up': _f('#side', [2, 1, 4, 2]), 'down': _f('#side', [2, 6, 4, 7])}}
    if pulled:
        book['rotation'] = {'origin': [13, 9, 0], 'axis': 'x', 'angle': -22.5}
    els.append(book)
    return {'parent': 'minecraft:block/block', 'textures': {'particle': 'minecraft:block/bookshelf', 'side': 'minecraft:block/bookshelf',
                                                             'end': 'minecraft:block/oak_planks'}, 'elements': els}


def gen_block(GA, b):
    bid, k = b['id'], b.get('model')
    A = GA.A
    if k == 'mansion_summoner':
        for ready in (False, True):
            GA.write(os.path.join(A, 'models/block', bid + ('_ready' if ready else '') + '.json'), summoner_model(ready))
        GA.TEXTURES.add('block/sculk_summoner_core')
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'ready=false': {'model': f'{NS}:block/{bid}'}, 'ready=true': {'model': f'{NS}:block/{bid}_ready'}}})
    elif k == 'mansion_door':
        GA.write(os.path.join(A, 'models/block', bid + '_open.json'), {'textures': {'particle': 'minecraft:block/bookshelf'}})
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'open=false': {'model': 'minecraft:block/bookshelf'}, 'open=true': {'model': f'{NS}:block/{bid}_open'}}})
        GA.write(os.path.join(A, 'items', bid + '.json'), {'model': {'type': 'minecraft:model', 'model': 'minecraft:block/bookshelf'}})
    elif k == 'mansion_loose':
        for pulled in (False, True):
            GA.write(os.path.join(A, 'models/block', bid + ('_pulled' if pulled else '') + '.json'), loose_model(pulled))
        rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
        variants = {}
        for f, y in rot.items():
            for pulled in (False, True):
                v = {'model': f'{NS}:block/{bid}' + ('_pulled' if pulled else '')}
                if y:
                    v['y'] = y
                variants[f'facing={f},pulled={str(pulled).lower()}'] = v
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': variants})
        GA.item_block(bid)


# =========================================================================== sounds

SOUNDS = {
    'entity.hornblower.ambient': [('event:entity.pillager.ambient', 1.0, 0.82)],
    'entity.hornblower.hurt': [('event:entity.pillager.hurt', 1.0, 0.82)],
    'entity.hornblower.death': [('event:entity.pillager.death', 1.0, 0.8)],
    'entity.hornblower.windup': [('event:entity.evoker.prepare_attack', 1.0, 0.62)],
    'entity.bard.ambient': [('event:entity.pillager.ambient', 1.0, 1.12)],
    'entity.bard.hurt': [('event:entity.pillager.hurt', 1.0, 1.12)],
    'entity.bard.death': [('event:entity.pillager.death', 1.0, 1.1)],
    'entity.bard.heal': [('block/amethyst/resonate1', 1.0, 1.25), ('block/amethyst/resonate2', 1.0, 1.3), ('block/amethyst/resonate3', 1.0, 1.2)],
    # the giant horn: a goat horn call two octaves... well, one octave down, and the gust it throws
    'item.giant_goat_horn.blast': [(f'item/goat_horn/call{i}', 1.0, 0.5) for i in (0, 1, 2, 3, 5, 7)],
    'item.giant_goat_horn.gust': [(f'entity/wind_charge/wind_burst{i}', 1.0, 0.62) for i in (1, 2, 3)],
    'block.sculk_summoner.arm': [('event:entity.warden.heartbeat', 1.0, 0.7)],
    'block.secret_bookshelf.open': [('event:block.piston.extend', 0.6, 0.6)],
    'block.loose_bookshelf.pull': [('event:block.chiseled_bookshelf.pickup', 1.0, 0.8)],
}
SUBTITLES = {
    'entity.hornblower.ambient': 'Hornblower murmurs', 'entity.hornblower.hurt': 'Hornblower hurts', 'entity.hornblower.death': 'Hornblower dies',
    'entity.hornblower.windup': 'Hornblower draws breath', 'entity.bard.ambient': 'Bard hums', 'entity.bard.hurt': 'Bard hurts',
    'entity.bard.death': 'Bard dies', 'entity.bard.heal': 'Bard\'s song heals', 'item.giant_goat_horn.blast': 'Giant Goat Horn blasts',
    'item.giant_goat_horn.gust': 'A gust roars', 'block.sculk_summoner.arm': 'Warden Core beats', 'block.secret_bookshelf.open': 'Bookcase swings',
    'block.loose_bookshelf.pull': 'Book clicks',
}


def sounds(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)


# =========================================================================== data

# Sift things a mansion's illagers have hoarded (the vault's loot, and a roll in every mansion chest)
SIFT_LOOT = [('star_shard', 6, (1, 4)), ('chrome_pearl', 4, (1, 3)), ('glowing_slime_ball', 5, (2, 6)), ('sculk_string', 4, (2, 5)),
             ('music_sheet_golem', 2, None), ('music_sheet_lullaby', 2, None), ('music_sheet_whale', 1, None), ('music_sheet_canon', 1, None),
             ('guitar', 2, None), ('crane_flute', 2, None), ('conga_drum', 2, None), ('wind_chimes', 2, None), ('giant_goat_horn', 2, None),
             ('minecraft:echo_shard', 3, (1, 3)), ('minecraft:sculk_sensor', 2, None), ('warden_core', 1, None)]


def _entries(D, rows):
    return [D.item(n, w, count=c) for n, w, c in rows]


def data(GA):
    import gen_data as D
    d = os.path.join(GA.RES, 'data', NS)
    # ---- loot: the two illagers, the vault's chests, a Sift roll in every mansion chest
    D.table('entity', 'entities/hornblower', [
        D.pool([D.item('minecraft:emerald', count=(0, 1), extra=[D.LOOTING])]),
        D.pool([D.item('minecraft:leather', count=(0, 2), extra=[D.LOOTING])]),
        # (his Giant Goat Horn drops as held equipment does, now and then)
    ])
    D.table('entity', 'entities/bard', [
        D.pool([D.item('minecraft:emerald', count=(0, 2), extra=[D.LOOTING])]),
        # (his guitar drops as held equipment does, now and then; a sheet from his satchel sometimes)
        D.pool([D.item('music_sheet_lullaby', 1), D.item('music_sheet_golem', 1), D.item('music_sheet_canon', 1)],
               condition={'type': 'minecraft:all_of', 'terms': [D.PLAYER_KILL, D.chance(0.08)]}),
    ])
    D.table('chest', 'chests/mansion_vault', [
        D.pool([D.item('music_sheet_symphony')]),  # the Sift Symphony: always here
        D.pool(_entries(D, SIFT_LOOT), rolls=(3, 5)),
        D.pool([D.item('warden_core')], condition=D.chance(0.3)),
        D.pool([D.item('minecraft:emerald', count=(2, 6)), D.item('minecraft:book', count=(1, 3)), D.item('minecraft:golden_apple')], rolls=(1, 2)),
    ])
    D.table('chest', 'chests/mansion_vault_barrel', [
        D.pool(_entries(D, SIFT_LOOT[:-1]), rolls=(2, 4)),
        D.pool([D.item('minecraft:emerald', count=(1, 4)), D.item('minecraft:candle', count=(1, 3)), D.item('minecraft:paper', count=(2, 6))]),
    ])
    D.table('chest', 'gameplay/mansion_sift_loot', [
        D.pool(_entries(D, SIFT_LOOT[:-1]) + [D.item('warden_core', 1)], condition=D.chance(0.55)),
    ])
    GA.write(os.path.join(d, 'loot_modifiers', 'mansion_sift_loot.json'), {
        'type': 'neoforge:add_table', 'condition': {'type': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:chests/woodland_mansion'},
        'table': f'{NS}:gameplay/mansion_sift_loot'})
    # an Echoer answers the Symphony too (every song has its gift, see tools/echoer_world.py)
    D.table('gift', 'gameplay/echoer_gift/symphony', [
        D.pool([D.item('star_shard', 8, count=(2, 4)), D.item('minecraft:echo_shard', 6, count=(1, 3)), D.item('minecraft:sculk_sensor', 4, count=(1, 2)),
                D.item('minecraft:diamond', 3), D.item('minecraft:book', 4, extra=[D.ENCHANT])]),
        D.pool([{'type': 'minecraft:loot_table', 'value': f'{NS}:gameplay/echoer_gift/rare', 'weight': 1}], condition=D.chance(0.05))])
    # the summoner gives its catalyst back (the core is popped out, or spent)
    D.table('block', 'blocks/sculk_summoner', [D.pool([D.item('minecraft:sculk_catalyst')], condition={'type': 'minecraft:survives_explosion'})])
    # ---- recipes
    rdir = os.path.join(d, 'recipe')
    GA.write(os.path.join(rdir, 'giant_goat_horn.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': ['HCH', ' L ', ' H '],
        'key': {'H': 'minecraft:goat_horn', 'C': 'minecraft:copper_ingot', 'L': 'minecraft:leather'},
        'result': {'count': 1, 'id': rl('giant_goat_horn')}})
    GA.write(os.path.join(rdir, 'secret_bookshelf.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['B', 'P'],
        'key': {'B': 'minecraft:bookshelf', 'P': 'minecraft:piston'}, 'result': {'count': 1, 'id': rl('secret_bookshelf')}})
    GA.write(os.path.join(rdir, 'loose_bookshelf.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'redstone', 'pattern': ['B', 'L'],
        'key': {'B': 'minecraft:bookshelf', 'L': 'minecraft:lever'}, 'result': {'count': 1, 'id': rl('loose_bookshelf')}})
    GA.tag('block', 'minecraft:enchantment_power_provider', rl('secret_bookshelf'))
    GA.tag('block', 'minecraft:enchantment_power_provider', rl('loose_bookshelf'))
    GA.tag('entity_type', 'minecraft:raiders', rl('hornblower'))
    GA.tag('entity_type', 'minecraft:raiders', rl('bard'))
    GA.tag('entity_type', 'minecraft:illager', rl('hornblower'))
    GA.tag('entity_type', 'minecraft:illager', rl('bard'))
    # ---- the vault: a structure that shares the mansions' placement exactly (same spread, spacing, separation, salt)
    GA.write(os.path.join(d, 'worldgen', 'structure', 'mansion_vault.json'), {
        'type': rl('mansion_vault'), 'biomes': '#minecraft:has_structure/woodland_mansion', 'spawn_overrides': {}, 'step': 'strongholds'})
    GA.write(os.path.join(d, 'worldgen', 'structure_set', 'mansion_vaults.json'), {
        'placement': {'type': 'minecraft:random_spread', 'salt': 10387319, 'separation': 20, 'spacing': 80, 'spread_type': 'triangular'},
        'structures': [{'structure': rl('mansion_vault'), 'weight': 1}]})
    horn_models(GA)
    GA.LANG.update(lang())


# =========================================================================== the giant horn's 3D model

# The held horn is a small block-style model in Mojang's manner (like the spyglass or the bell): whole-pixel boxes on a
# 16x16 texture of flat horn tones with darker ridges, a brass rim and mouthpiece and a leather grip. It is laid out like
# the vanilla goat horn's sprite (bell top left, mouthpiece right), so the vanilla goat horn's hand transforms fit it.
HORN_SEGMENTS = [  # (centre x, centre y, length, thickness, angle) - from the mouthpiece round the bottom up to the bell
    (12.4, 3.6, 3, 2, 45.0), (10.0, 2.4, 3, 2, 0.0), (7.3, 3.0, 3, 3, -22.5), (5.1, 4.9, 3, 3, -45.0), (3.9, 7.6, 3, 4, -67.5),
    (3.9, 10.6, 3, 4, -90.0),
]


def horn_model(display):
    els = []
    for i, (cx, cy, length, th, ang) in enumerate(HORN_SEGMENTS):
        x0, x1 = cx - length / 2, cx + length / 2
        y0, y1 = cy - th / 2, cy + th / 2
        z0, z1 = 8 - th / 2, 8 + th / 2
        v0 = 0 if i % 2 == 0 else 4  # alternate the ridge rows
        side = _f('#t', [0, v0, length, v0 + th])
        top = _f('#t', [4, v0, 4 + length, v0 + th])
        el = {'from': [x0, y0, z0], 'to': [x1, y1, z1],
              'faces': {'north': side, 'south': side, 'up': top, 'down': top, 'east': _f('#t', [8, 0, 8 + th, th]),
                        'west': _f('#t', [8, 0, 8 + th, th])}}
        if ang:
            el['rotation'] = {'origin': [cx, cy, 8], 'axis': 'z', 'angle': ang}
        els.append(el)
    # the bell: a brass rim round a dark mouth, standing up at the top left
    rim = _f('#t', [0, 8, 6, 10])
    els.append({'from': [0.9, 12.0, 5.0], 'to': [6.9, 14.0, 11.0], 'faces': {
        'north': rim, 'south': rim, 'east': rim, 'west': rim, 'up': _f('#t', [10, 10, 16, 16]), 'down': _f('#t', [10, 10, 16, 16])}})
    # the mouthpiece, and the leather grip with its brass ring
    els.append({'from': [13.2, 4.4, 7.0], 'to': [15.2, 6.4, 9.0], 'rotation': {'origin': [14.2, 5.4, 8], 'axis': 'z', 'angle': 45.0},
                'faces': {d: _f('#t', [6, 8, 8, 10]) for d in ('north', 'south', 'east', 'west', 'up', 'down')}})
    grip = _f('#t', [0, 12, 2, 15])
    els.append({'from': [8.0, 0.6, 6.5], 'to': [10.0, 4.2, 9.5], 'faces': {'north': grip, 'south': grip, 'east': grip, 'west': grip,
                                                                          'up': _f('#t', [2, 12, 4, 15]), 'down': _f('#t', [2, 12, 4, 15])}})
    return {'textures': {'t': f'{NS}:item/giant_goat_horn_model', 'particle': f'{NS}:item/giant_goat_horn_model'}, 'elements': els,
            'display': display, 'gui_light': 'front'}


def horn_texture():
    """The 3D horn's texture: two rows of ridged horn (vanilla goat horn tones), brass, the bell's dark mouth and leather."""
    rows = [
        '3434343424243434', '4545454535353545', '4545454535353545', '2323232312121232',
        '3343343324243343', '4454454535354454', '4454454535354454', '2232232212122232',
        'yYYYYyYoYYYYYYyy', 'YYYYYYoooooooYYY', '..........yYYYYy', '..........YkkkkY',
        'LLlL......YkkkkY', 'LlLl......YkkkkY', 'LLlL......YkkkkY', '..........yYYYYy',
    ]
    return _grid(rows, HORN_PAL)


def horn_models(GA):
    big = 1.3  # a giant horn: the vanilla horn's transforms, a third bigger
    held = {'thirdperson_righthand': {'rotation': [0, 180, 0], 'translation': [0, 3, 1], 'scale': [0.55 * big] * 3},
            'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 3, 1], 'scale': [0.55 * big] * 3},
            'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68 * big] * 3},
            'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68 * big] * 3},
            'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]}}
    tooting = {'thirdperson_righthand': {'rotation': [0, -125, 0], 'translation': [-1, 2, 2], 'scale': [0.5 * big] * 3},
               'thirdperson_lefthand': {'rotation': [0, 55, 0], 'translation': [-1, 2, 2], 'scale': [0.5 * big] * 3},
               'firstperson_righthand': {'rotation': [0, -55, -5], 'translation': [-1, -2.5, -7.5]},
               'firstperson_lefthand': {'rotation': [0, 115, 5], 'translation': [0, -2.5, -7.5]}}
    mdir = os.path.join(GA.A, 'models', 'item')
    GA.write(os.path.join(mdir, 'giant_goat_horn_held.json'), horn_model(held))
    GA.write(os.path.join(mdir, 'giant_goat_horn_tooting.json'), horn_model(tooting))
    GA.TEXTURES.add('item/giant_goat_horn_model')
    GA.item_generated('giant_goat_horn')  # the inventory sprite (models/item/giant_goat_horn.json)
    GA.write(os.path.join(GA.A, 'items', 'giant_goat_horn.json'), {'model': {
        'type': 'minecraft:select', 'property': 'minecraft:display_context',
        'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf'], 'model': {'type': 'minecraft:model', 'model': f'{NS}:item/giant_goat_horn'}}],
        'fallback': {'type': 'minecraft:condition', 'property': 'minecraft:using_item',
                     'on_true': {'type': 'minecraft:model', 'model': f'{NS}:item/giant_goat_horn_tooting'},
                     'on_false': {'type': 'minecraft:model', 'model': f'{NS}:item/giant_goat_horn_held'}}}})


# =========================================================================== the two illagers' textures

# The vanilla pillager's texture, re-dressed: its own layout and shading, the clothes recoloured by colour class and a few
# vanilla-manner details drawn on (the Hornblower's fleece hood with curled horns and his horn's baldric; the Bard's
# burgundy cap with a gold band and a feather, gold buttons and a sash). The face stays the vanilla illager's.
PILLAGER_CLASSES = {
    'shirt': ['#231014', '#36181e', '#4b262d', '#5a2f38'],
    'trousers': ['#1e2a2a', '#1f3636', '#264747'],
    'leather': ['#321709', '#3e1e0f', '#4f2b19', '#5f3621', '#76432a'],
    'accent': ['#668785', '#93aead', '#c7cece'],
}
DRESS = {
    'hornblower': {'shirt': ['#171d29', '#222b3b', '#2f3a50', '#3a4760'], 'trousers': ['#241c14', '#2d241a', '#382d21'],
                   'leather': ['#321709', '#3e1e0f', '#4f2b19', '#5f3621', '#76432a'], 'accent': ['#9a8f76', '#cfc5ab', '#ece6d4']},
    'bard': {'shirt': ['#0d2826', '#143936', '#1d4f4b', '#27625d'], 'trousers': ['#2c0e17', '#3b1420', '#4c1b2a'],
             'leather': ['#3a2410', '#4a2e14', '#5e3a1a', '#704621', '#8a5a2a'], 'accent': ['#94701e', '#d4a53c', '#f2d27a']},
}
FLEECE = {'f': '#d9d0b8', 'F': '#e8e1cd', 'g': '#b9ae93', 'h': '#4a4642', 'H': '#7d786e', 'k': '#2e2b28', 'm': '#625e57'}


def _paint(img, x0, y0, rows, pal):
    px = img.load()
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch == '.':
                continue
            if ch == '_':
                px[x0 + i, y0 + j] = (0, 0, 0, 0)
                continue
            c = pal[ch].lstrip('#')
            px[x0 + i, y0 + j] = (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255)


def illager_texture(kind):
    from PIL import Image
    src = Image.open(os.path.join(VAN_TEX, 'entity', 'illager', 'pillager.png')).convert('RGBA')
    img = src.copy()
    px = img.load()
    cmap = {}
    for cls, cols in PILLAGER_CLASSES.items():
        to = DRESS[kind][cls]
        for i, c in enumerate(cols):
            cmap[c] = to[min(i, len(to) - 1)]
    for y in range(20, 64):  # the clothes only (the head and its hat region stay)
        for x in range(64):
            c = px[x, y]
            if c[3] == 0:
                continue
            h = '#%02x%02x%02x' % c[:3]
            if h in cmap:
                t = cmap[h].lstrip('#')
                px[x, y] = (int(t[0:2], 16), int(t[2:4], 16), int(t[4:6], 16), 255)
    if kind == 'hornblower':
        # the fleece hood (the illager's hat layer, 8x12x8 at 32,0): top, sides and back shaggy to the collar, a fringe over
        # the brow and a ruff under the chin; a curled goat horn on each side
        shag = ['ffFfffff', 'fffffgff', 'fFffffff', 'ffffFfff', 'fffffffF', 'fgffffff', 'ffffffgf', 'fFffFfff']
        _paint(img, 40, 0, shag, FLEECE)
        side = ['ffFfffff', 'fffffgff', 'fFffffff', 'ffffFfff', 'ffgffffF', 'fFffffff', 'ffffffgf', 'fgffFfff', 'ffffffff',
                'gfFfgffg', 'f.g.f.g.', '.g...g..']
        for x0 in (32, 48, 56):
            _paint(img, x0, 8, side, FLEECE)
        _paint(img, 40, 8, ['FfgfFfFf', 'f.F..f.F'], FLEECE)
        _paint(img, 40, 18, ['.f.FF.f.', 'FfFggFfF'], FLEECE)
        horn = ['.mHHm...', 'mHkkHm..', 'Hk..kH..', 'Hk.mkH..', 'mHkkHm..', '..mmh...', '...hk...']
        _paint(img, 33, 8, horn, FLEECE)
        _paint(img, 49, 8, [r[::-1] for r in horn], FLEECE)
        # the horn's baldric across the tunic, from the left shoulder to the right hip, with a brass buckle
        L = {'l': '#3e1e0f', 'L': '#5f3621', 'b': '#c9973a', 'B': '#efcb68'}
        _paint(img, 22, 26, ['......Ll', '.....Ll.', '....bB..', '...Bb...', '..Ll....', '.Ll.....', 'Ll......', 'l.......'], L)
    else:
        # the burgundy cap with a gold band, and a cream feather swept back from its left side
        C = {'c': '#6a1f30', 'C': '#83293d', 'd': '#4a1420', 'g': '#d4a53c', 'G': '#f2d27a', 'p': '#ece3cc', 'P': '#fbf6e8', 't': '#3fb3ad'}
        _paint(img, 40, 0, ['cCcccCcc', 'CcccCccc', 'ccCdcccC', 'cccddCcc', 'cCcddccc', 'ccccCccC', 'CcccccCc', 'ccCcccCc'], C)
        band = ['cCccCcCc', 'ccCcccCc', 'gGgggGgg']
        for x0 in (32, 40, 48, 56):
            _paint(img, x0, 8, band, C)
        _paint(img, 48, 8, ['..tPpPp.', '.tPpP...', 'tPp.....'], C)
        _paint(img, 32, 8, ['.pPpPt..', '...PpPt.', '.....pPt'], C)
        _paint(img, 40, 0, ['......tP', '.......p'], C)
        # gold buttons down the doublet and a burgundy sash from the right shoulder to the left hip
        S = {'s': '#6a1f30', 'S': '#83293d', 'g': '#d4a53c', 'G': '#f2d27a'}
        _paint(img, 22, 28, ['Ss..G...', '.Ss.....', '..SsG...', '...Ss...', '....Ss..', '.....Ss.'], S)
    return img


# =========================================================================== text

def lang():
    c = f'codex.{NS}'
    m = f'message.{NS}.summoner'
    return {
        f'entity.{NS}.hornblower': 'Hornblower', f'entity.{NS}.bard': 'Bard',
        f'item.{NS}.giant_goat_horn.desc': 'Use: a blast that throws everything in front of you back',
        f'item.{NS}.secret_bookshelf.desc': 'Opens while powered - or when the Loose Bookshelves near it are pulled from the lowest up',
        f'item.{NS}.loose_bookshelf.desc': 'Pull it (use). Pull them all from the lowest up to open the Bookshelf Doors near them',
        f'band.{NS}.instrument.giant_horn': 'Giant Goat Horn', f'band.{NS}.instrument.mansion_guitar': 'Guitar',
        # the summoner's voice
        f'{m}.need_sensors': 'The summoner needs %2$s Sculk Sensors within 8 blocks to listen (%1$s found).',
        f'{m}.no_frame': 'No Sift gate answers. Build a frame of Sift Gate Frames (or find an Ancient City gate) within 16 blocks.',
        f'{m}.ready': 'The sculk is listening. Play the Sift Symphony on any instrument.',
        f'{m}.need_sheet': 'The sensors wait for a song you do not know: carry the Sift Symphony\'s sheet.',
        f'{m}.note': 'The Sift Symphony: %s of %s',
        f'{m}.lost': 'The sculk loses the tune. Begin the Symphony again.',
        f'{m}.already_open': 'That gate already stands open.',
        f'{m}.opening': 'The Sift is waking!',
        f'{m}.opened': 'The way to The Sift is open.',
        f'{m}.gate_broken': 'The gate\'s frame broke - the membrane tears. Mend it and play again.',
        f'{m}.core_removed': 'The Warden Core slips free.',
        f'{m}.core_spent': 'The Warden Core is pouring itself into the gate.',
        f'message.{NS}.secret.locked': 'The books click back. Pull them from the lowest up.',
        f'message.{NS}.secret.opened': 'Something heavy shifts behind the bookcase...',
        # the Knowledge Book's pages: the Symphony's song page, the two Pillagers' field notes
        f'knowledge.{NS}.song.symphony': ('Any instrument will do. Played at a Sculk Summoner - a Warden Core in a Sculk Catalyst, four '
                                          'Sculk Sensors round it, a Sift gate near - the sensors light note by note and the gate wakes.'),
        f'knowledge.{NS}.song.symphony.creature': 'The Sift gates',
        f'codex.{NS}.hornblower.notes': 'Habitat: Woodland Mansions, raids|Temper: Hostile|Diet: -|Drops: Emeralds, leather, his Giant Goat Horn',
        f'codex.{NS}.bard.notes': 'Habitat: Woodland Mansions, raids|Temper: Hostile, heals his kin|Diet: -|Drops: Emeralds, his guitar, sheets',
        # Codex
        f'{c}.hornblower.title': 'Hornblower', f'{c}.hornblower.tagline': 'Hostile - a pillager with a giant horn',
        f'{c}.hornblower.body': ('A burly illager in goat fleece who guards the Woodland Mansions and marches with raids. He keeps his distance, '
                                 'draws a long breath - the horn rises to his lips, the bell trembles - and blasts: everything in the cone in '
                                 'front of him is thrown back with the gust. Step aside of the bell while he winds up, then close in before '
                                 'he catches his breath. He may drop his Giant Goat Horn.'),
        f'{c}.bard.title': 'Bard', f'{c}.bard.tagline': 'Hostile - a pillager minstrel who heals',
        f'{c}.bard.body': ('A mansion minstrel in a plumed hat with a guitar on his chest. He keeps away from you and strums for his kin: '
                           'green notes drift out of his song and every illager around him heals, and keeps healing while he plays. '
                           'Raids bring him along. Silence him first. He may drop his guitar or a music sheet.'),
        f'{c}.giant_goat_horn.title': 'Giant Goat Horn', f'{c}.giant_goat_horn.tagline': 'A blast you can hold',
        f'{c}.giant_goat_horn.body': ('Three goat horns, a copper mouthpiece and a leather grip, or the Hornblower\'s own. Use it and it bellows '
                                      'a deep call: a gust throws everything in front of you back and up. It needs a breath before the next.'),
        f'{c}.sculk_summoner.title': 'Sculk Summoner', f'{c}.sculk_summoner.tagline': 'A Warden Core in a Sculk Catalyst',
        f'{c}.sculk_summoner.body': ('Use a Warden Core on a Sculk Catalyst near a Sift gate frame (within 16 blocks): the core settles in its '
                                     'claws and beats. Place four Sculk Sensors within 8 blocks of it - it blooms when it has them all. '
                                     'Then, carrying the sheet, play the Sift Symphony on any instrument nearby: the sensors light up note by '
                                     'note, and on the last the gate wakes. Sneak and use it to take the core back.'),
        f'{c}.woodland_mansion.title': 'The Mansion\'s Secret', f'{c}.woodland_mansion.tagline': 'A bookcase that is a door',
        f'{c}.woodland_mansion.body': ('Every Woodland Mansion hides a vault. In the ground floor\'s front corner corridor stands a tall '
                                       'bookcase with three loose books in it: pull them from the lowest shelf up and the bookcase opens on '
                                       'a stair. Below lie the Sift Symphony\'s sheet, Sift treasure and guards. Hornblowers and Bards live '
                                       'in the mansion too, and its chests keep Sift things.'),
    }


# =========================================================================== art

def _grid(rows, pal):
    from PIL import Image
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != '.':
                c = pal[ch].lstrip('#')
                px[x, y] = (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255)
    return img


HORN_PAL = {'a': '#2e2b28', 'k': '#1b1816', '1': '#4a4642', '2': '#625e57', '3': '#7d786e', '4': '#9f9a8b', '5': '#bdb7a6',
            'Y': '#c9973a', 'y': '#efcb68', 'o': '#7d5418', 'L': '#6b4423', 'l': '#462a14'}


def horn_sprite():
    """The Giant Goat Horn's 16x16 sprite, in the vanilla goat horn's manner and palette but filling the slot: a wide bell
    with a brass rim at the top left, ridged horn sweeping down and round, a leather grip, a brass mouthpiece at the right."""
    rows = [
        '................',
        '.aaaaaa.........',
        'aYyyyyYa........',
        'ayokkkoYa.......',
        'aYkkkkkYa.......',
        'aYokkkoYa.......',
        '.aYYYYYa........',
        '.a4532a.........',
        '.a35422a.....aaa',
        '..a4322a....ayYa',
        '..a3542a...a12a.',
        '...aLlL1aaa232a.',
        '...alLl2343421a.',
        '....aa13342a1a..',
        '......aaaaaa.a..',
        '................',
    ]
    return _grid(rows, HORN_PAL)


def _egg_from_pillager(face_top, collar, pal):
    """A vanilla-style illager spawn egg: the pillager egg's grey face (unibrow, nose) with the collar/clothes rows redrawn."""
    from PIL import Image
    src = Image.open(os.path.join(VAN_TEX, 'item', 'pillager_spawn_egg.png')).convert('RGBA')
    img = src.copy()
    px = img.load()
    for y, row in {**face_top, **collar}.items():
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            if ch == '_':
                px[x, y] = (0, 0, 0, 0)
                continue
            c = pal[ch].lstrip('#')
            px[x, y] = (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), 255)
    return img


def hornblower_egg():
    pal = {'f': '#b9ae93', 'F': '#d9d0b8', 'W': '#e8e1cd', 'r': '#171d29', 'R': '#2f3a50', 'S': '#3a4760', 'd': '#241c14', 'D': '#1c1510',
           'b': '#c9a043', 'h': '#7d786e', 'H': '#4a4642', 'c': '#5c3b22'}
    top = {0: '....h......h....', 1: '...hH......Hh...', 2: '..hH........Hh..'}  # two curled goat horns
    collar = {
        9: '..FWfF....FfWF..',
        10: '..fFWRSRRSRWFf..',
        11: '..dfRRSbbRRSfd..',
        12: '...drRRbbRRrd...',
        13: '....drRccRrd....',
        14: '.....dDDDDd.....',
    }
    img = _egg_from_pillager(top, collar, pal)
    return img


def bard_egg():
    pal = {'h': '#6a1f30', 'H': '#83293d', 'g': '#d4a53c', 'G': '#f2d27a', 'p': '#ece3cc', 't': '#3fb3ad',
           'T': '#1f6a68', 'U': '#2c827f', 'V': '#144a49', 's': '#7d2236', 'S': '#982f46', 'd': '#201d28', 'D': '#141218'}
    top = {
        0: '......tp........',
        1: '...hhhhHhhh.....',
        2: '..hHHHHHHHHh....',
        3: '.hhggggGgggh....',
    }
    collar = {
        9: '..ppTVppppVTpp..',
        10: '..TUTsGSsGTUTV..',
        11: '..VTUsSSssUTVV..',
        12: '...VTUsGGsUTV...',
        13: '....VdUTTUdV....',
        14: '.....dDDDDd.....',
    }
    return _egg_from_pillager(top, collar, pal)


def items():
    """Item sprites (items16.all_items): the spawn eggs and the horn's inventory sprite."""
    return {'hornblower_spawn_egg': hornblower_egg(), 'bard_spawn_egg': bard_egg(), 'giant_goat_horn': horn_sprite()}


def core_frames():
    """The Warden Core in the summoner's claws: the vanilla sculk block's own pixels, its glowing cyan pores swelling with
    a double heartbeat (lub-dub, rest) - eight frames."""
    import numpy as np
    from PIL import Image
    sculk = np.asarray(Image.open(os.path.join(VAN_TEX, 'block', 'sculk.png')).convert('RGBA')).astype(np.float64)[:16] / 255.0
    lum = sculk[..., :3] @ np.array([0.3, 0.59, 0.11])
    cyan = sculk[..., 2] > sculk[..., 0] * 1.6
    glow = (lum > np.quantile(lum, 0.72)) | cyan
    yy, xx = np.mgrid[0:16, 0:16]
    heart = np.exp(-(((xx - 7.5) / 3.2) ** 2 + ((yy - 8.0) / 3.6) ** 2))
    beat = [0.25, 1.0, 0.55, 0.9, 0.4, 0.25, 0.2, 0.2]
    frames = []
    for b in beat:
        base = sculk[..., :3] * (0.75 + 0.25 * b)
        core = np.array([0.18, 0.95, 0.92])
        k = np.clip(heart * (0.35 + 0.65 * b), 0, 1)[..., None]
        col = base * (1 - 0.55 * k) + core * 0.55 * k
        col = np.where(glow[..., None], np.minimum(1.0, col + np.array([0.15, 0.55, 0.55]) * (0.45 + 0.55 * b)), col)
        img = np.concatenate([np.clip(col, 0, 1), np.ones((16, 16, 1))], -1)
        frames.append(Image.fromarray((img * 255).round().astype(np.uint8), 'RGBA'))
    return frames


def textures(out):
    import gen_textures as GT
    out('block/sculk_summoner_core', GT.strip(core_frames()), {'animation': {'frametime': 3, 'interpolate': True}})
    out('item/giant_goat_horn_model', horn_texture())
    for kind in ('hornblower', 'bard'):
        out(f'entity/mansion/{kind}', illager_texture(kind))
