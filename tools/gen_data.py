"""Loot tables, loot modifiers and other data for The Sift (the Echoer no longer trades: see tools/echoer_world.py)."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_assets as GA  # noqa: E402

NS = 'thesift'
D = os.path.join(GA.RES, 'data', NS)


def w(path, obj):
    GA.write(os.path.join(D, path + '.json'), obj)


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def item(name, weight=1, count=None, extra=None):
    if name == 'minecraft:empty':
        return {'type': 'minecraft:empty', 'weight': weight}
    e = {'type': 'minecraft:item', 'name': rl(name), 'weight': weight}
    mods = []
    if count is not None:
        if isinstance(count, tuple):
            mods.append({'type': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': count[0], 'max': count[1]}})
        else:
            mods.append({'type': 'minecraft:set_count', 'count': count})
    if extra:
        mods += extra
    if mods:
        e['modifier'] = mods if len(mods) > 1 else mods[0]
    return e


def pool(entries, rolls=1, condition=None):
    p = {'entries': entries, 'rolls': rolls if not isinstance(rolls, tuple) else {'type': 'minecraft:uniform', 'min': rolls[0], 'max': rolls[1]}}
    if condition:
        p['condition'] = condition
    return p


def table(kind, name, pools):
    w(f'loot_table/{name}', {'type': f'minecraft:{kind}', 'pools': pools, 'random_sequence': f'{NS}:{name}'})


LOOTING = {'type': 'minecraft:enchanted_count_increase', 'count': {'type': 'minecraft:uniform', 'max': 1.0, 'min': 0.0}, 'enchantment': 'minecraft:looting'}
ENCHANT = {'type': 'minecraft:enchant_randomly', 'options': '#minecraft:on_random_loot'}
PLAYER_KILL = {'type': 'minecraft:killed_by_player'}


def chance(p):
    return {'type': 'minecraft:random_chance', 'chance': p}


def entity_loot():
    table('entity', 'entities/bulb', [pool([item('glowing_slime_ball', count=(0, 2), extra=[LOOTING])])])
    table('entity', 'entities/slumbler', [
        pool([item('thick_hide', count=(1, 3), extra=[LOOTING])]),
        pool([item('slumbler_gill', count=(1, 2), extra=[LOOTING])]),  # CR2: its frilled gills, for a helmet to breathe in Chrome
        pool([item('chrome_pearl')], condition={'type': 'minecraft:all_of', 'terms': [PLAYER_KILL, {
            'type': 'minecraft:random_chance_with_enchanted_bonus', 'enchanted_chance': {'type': 'minecraft:linear', 'base': 0.2, 'per_level_above_first': 0.05},
            'enchantment': 'minecraft:looting', 'unenchanted_chance': 0.15}]})])
    table('entity', 'entities/sifter', [pool([item('chime_sand', count=(0, 2)), item('star_shard', 1)], condition=None),
                                        pool([item('glowing_slime_ball', count=(0, 1), extra=[LOOTING])]),
                                        # W1: what it swallowed in the dunes - now and then a lost Music Sheet
                                        pool([item('music_sheet_offering', 2), item('music_sheet_nib', 2), item('music_sheet_lullaby', 1)],
                                             condition={'type': 'minecraft:all_of', 'terms': [PLAYER_KILL, chance(0.07)]})])
    table('entity', 'entities/enchoer', [pool([item('chrome_pearl', count=(1, 2)), item('star_shard', count=(1, 2))])])
    table('entity', 'entities/dictator', [pool([item('warden_core', 1)]), pool([item('minecraft:echo_shard', count=(6, 12))]),
                                          pool([item('siftite_ingot', count=(3, 6))]), pool([item('music_disc_lullaby', 1)])])
    table('entity', 'entities/thumper', [pool([item('conga_drum', 1)]), pool([item('minecraft:turtle_scute', count=(2, 5))]),
                                         pool([item('minecraft:echo_shard', count=(2, 4))]), pool([item('siftite_ingot', count=(1, 3))])])
    table('entity', 'entities/strummer', [pool([item('weaver_guitar', 1)]), pool([item('sculk_string', count=(6, 12))]),
                                          pool([item('minecraft:echo_shard', count=(2, 4))]), pool([item('siftite_ingot', count=(1, 3))])])
    for minion in ('strumling',):
        extra = {'strumling': 'sculk_string'}[minion]
        table('entity', f'entities/{minion}', [pool([item(extra, count=(0, 2), extra=[LOOTING])]),
                                              pool([item('minecraft:echo_shard', count=(0, 1), extra=[LOOTING])]),
                                              pool([item('minecraft:bone', count=(0, 2))])])


# the wild creatures: cooked when they die on fire, like vanilla meat and fish
SMELT_IF_BURNING = {'type': 'minecraft:furnace_smelt', 'condition': {'type': 'minecraft:any_of', 'terms': [
    {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': {'minecraft:flags': {'is_on_fire': True}}},
    {'type': 'minecraft:entity_properties', 'entity': 'direct_attacker', 'predicate': {'minecraft:equipment': {'mainhand': {'predicates': {
        'minecraft:enchantments': [{'enchantments': '#minecraft:smelts_loot'}]}}}}}]}}


def wild_creature_loot():
    table('entity', 'entities/stomper', [pool([item('stomper_meat', count=(2, 5), extra=[SMELT_IF_BURNING, LOOTING])]),
                                         pool([item('thick_hide', count=(0, 2), extra=[LOOTING])])])
    table('entity', 'entities/kazoo_fish', [pool([item('kazoo_fish', extra=[SMELT_IF_BURNING])]),
                                            pool([item('minecraft:bone_meal')], condition=chance(0.05))])
    table('entity', 'entities/fanfare_eel', [pool([item('fanfare_eel', extra=[SMELT_IF_BURNING, LOOTING])]),
                                             pool([item('minecraft:gold_nugget', count=(1, 3), extra=[LOOTING])]),
                                             pool([item('minecraft:copper_ingot', count=(0, 1), extra=[LOOTING])])])
    # the Sculk Parasite bursts when it bites: nothing is left of it
    table('entity', 'entities/sculk_parasite', [])
    table('entity', 'entities/sky_whale', [pool([item('star_shard', count=(2, 4), extra=[LOOTING])]),
                                           pool([item('minecraft:white_wool', count=(2, 5))])])


def chest_loot():
    # the Thumper's arena: what the cannon towers keep
    table('chest', 'chests/drum_pit_armory', [
        pool([item('cannonball', 1, (5, 9))]),
        pool([item('minecraft:gunpowder', 6, (2, 5)), item('minecraft:iron_nugget', 6, (3, 9)), item('cobbled_dreamstone', 4, (4, 12)),
              item('minecraft:torch', 3, (2, 6)), item('minecraft:empty', 4)], (1, 2)),
    ])
    table('chest', 'chests/sculk_castle', [
        pool([item('minecraft:echo_shard', 8, (3, 8)), item('siftite_ingot', 6, (2, 4)), item('chrome_pearl', 6, (2, 4)),
              item('minecraft:enchanted_book', 5, extra=[ENCHANT]), item('minecraft:diamond', 4, (1, 3)), item('siftite_upgrade_smithing_template', 3),
              item('music_disc_lullaby', 2), item('star_shard', 5, (2, 4))], (4, 6)),
        pool([item('warden_core', 1), item('minecraft:empty', 2)]),
    ])
    table('chest', 'chests/sculk_castle_landing', [
        pool([item('minecraft:golden_apple', 3), item('minecraft:cooked_beef', 8, (2, 5)), item('glowing_slime_ball', 8, (2, 6)),
              item('minecraft:arrow', 6, (4, 12)), item('chrome_pearl', 2), item('minecraft:empty', 4)], (2, 4)),
    ])
    # W1: the old ruins crumbled into the ground; their relics are brushed out of buried Suspicious Chime Sand
    # (worldgen/RelicCacheFeature in the dunes, plains, lake shores, kelp forest, swamp and sculk sea) - lost Music Sheets too
    table('archaeology', 'archaeology/sift_common', [pool([
        item('glowing_slime_ball', 3), item('siftite_dust', 2), item('minecraft:pitcher_pod', 2),
        item('echo_seed', 2), item('minecraft:pink_dye', 1), item('minecraft:light_blue_dye', 1), item('blush_bricks', 2), item('minecraft:brick', 1),
        item('glyph_stone', 1), item('chrome_pearl', 1), item('music_sheet_offering', 2), item('music_sheet_nib', 1), item('music_sheet_lullaby', 1),
        *__import__('knowledge').relic_entries('common')])])  # F3: lore brushed out of the relics
    table('archaeology', 'archaeology/sift_rare', [pool([
        item('chrome_pearl', 3), item('star_shard', 3), item('music_disc_lullaby', 1), item('siftite_upgrade_smithing_template', 2),
        item('siftite_nugget', 3), item('music_sheet_offering', 1), item('music_sheet_golem', 1), item('music_sheet_crystal', 1),
        item('music_sheet_whale', 1), item('music_sheet_lullaby', 1), item('sift_gate_frame', 1),
        *__import__('knowledge').relic_entries('rare')])])  # F3: lore brushed out of the relics


def sniffer_and_modifiers():
    # Sniffers in The Sift dig up the dimension's exclusive seeds.
    table('gift', 'gameplay/sniffer_digging_sift', [pool([item('echo_seed', 3), item('minecraft:pitcher_pod', 2),
                                                           item('minecraft:torchflower_seeds', 1)])])
    GA.write(os.path.join(D, 'loot_modifiers', 'sniffer_digging_sift.json'), {
        'type': 'neoforge:add_table',
        'condition': {'type': 'minecraft:all_of', 'terms': [
            {'type': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:gameplay/sniffer_digging'},
            {'type': 'minecraft:location_check', 'predicate': {'dimension': f'{NS}:the_sift'}}]},
        'table': f'{NS}:gameplay/sniffer_digging_sift'})
    # Wardens drop a Warden Core; Ancient City chests sometimes hold one.
    table('entity', 'gameplay/warden_core_drop', [pool([item('warden_core')])])
    GA.write(os.path.join(D, 'loot_modifiers', 'warden_core_from_warden.json'), {
        'type': 'neoforge:add_table',
        'condition': {'type': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:entities/warden'},
        'table': f'{NS}:gameplay/warden_core_drop'})
    table('chest', 'gameplay/ancient_city_extras', [
        pool([item('warden_core')], condition=chance(0.12)),
        pool([item('sift_gate_frame', 1), item('minecraft:empty', 9)])])
    GA.write(os.path.join(D, 'loot_modifiers', 'ancient_city_extras.json'), {
        'type': 'neoforge:add_table',
        'condition': {'type': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:chests/ancient_city'},
        'table': f'{NS}:gameplay/ancient_city_extras'})


def jukebox():
    w('jukebox_song/lullaby', {'comparator_output': 7, 'description': {'translate': f'jukebox_song.{NS}.lullaby'}, 'length_in_seconds': 154.0,
                               'sound_event': f'{NS}:music_disc.lullaby'})


def generate():
    entity_loot()
    wild_creature_loot()
    chest_loot()
    sniffer_and_modifiers()
    jukebox()
    __import__('songs').generate()  # songs & instruments (agent D)
    __import__('caravans').generate()  # C: Caravans, music crystals, prism
    __import__('gear_art').generate()  # B4 gear: prism tool recipes, tags and lore
    print('data ok')
