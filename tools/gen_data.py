"""Loot tables, loot modifiers, Enchoer trades and other data for The Sift."""
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
        pool([item('chrome_pearl')], condition={'type': 'minecraft:all_of', 'terms': [PLAYER_KILL, {
            'type': 'minecraft:random_chance_with_enchanted_bonus', 'enchanted_chance': {'type': 'minecraft:linear', 'base': 0.2, 'per_level_above_first': 0.05},
            'enchantment': 'minecraft:looting', 'unenchanted_chance': 0.15}]})])
    table('entity', 'entities/sifter', [pool([item('dreamsand', count=(0, 2)), item('star_shard', 1)], condition=None),
                                        pool([item('glowing_slime_ball', count=(0, 1), extra=[LOOTING])])])
    table('entity', 'entities/enchoer', [pool([item('chrome_pearl', count=(1, 2)), item('star_shard', count=(1, 2))])])
    table('entity', 'entities/riveter', [pool([item('minecraft:echo_shard', count=(0, 1), extra=[LOOTING])]),
                                         pool([item('minecraft:sculk', count=(0, 2))])])
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
    table('entity', 'entities/tubafish', [pool([item('tuba_bubble', count=(2, 4), extra=[LOOTING])]),
                                          pool([item('minecraft:bone_meal')], condition=chance(0.05))])
    table('entity', 'entities/fanfare_eel', [pool([item('minecraft:gold_nugget', count=(1, 3), extra=[LOOTING])]),
                                             pool([item('minecraft:copper_ingot', count=(0, 1), extra=[LOOTING])])])
    # the Sculk Parasite bursts when it bites: nothing is left of it
    table('entity', 'entities/sculk_parasite', [])
    table('entity', 'entities/sky_whale', [pool([item('star_shard', count=(2, 4), extra=[LOOTING])]),
                                           pool([item('minecraft:white_wool', count=(2, 5))])])


def chest_loot():
    table('chest', 'chests/sift_ruins', [
        pool([item('glowing_slime_ball', 10, (2, 6)), item('pitcher_bulb', 10, (1, 3)), item('dream_journal_fragment', 8), item('chrome_pearl', 3),
              item('raw_serbim', 8, (1, 4)), item('lullwood_sapling', 6, (1, 2)), item('wishwood_sapling', 4, (1, 2)), item('choir_pod', 3),
              item('echo_seed', 3), item('minecraft:book', 5, extra=[ENCHANT])], (3, 6)),
        pool([item('star_shard', 4), item('serbim_ingot', 6, (1, 3)), item('siftite_nugget', 5, (2, 5)), item('minecraft:empty', 10)], (1, 2)),
        pool([item('minecraft:turtle_scute', 3, (1, 2)), item('minecraft:string', 5, (2, 6)), item('minecraft:feather', 5, (2, 5)),
              item('minecraft:name_tag', 1), item('minecraft:empty', 8)]),
    ])
    # the Thumper's arena: what the cannon towers keep
    table('chest', 'chests/drum_pit_armory', [
        pool([item('cannonball', 1, (5, 9))]),
        pool([item('minecraft:gunpowder', 6, (2, 5)), item('minecraft:iron_nugget', 6, (3, 9)), item('cobbled_dreamstone', 4, (4, 12)),
              item('minecraft:torch', 3, (2, 6)), item('minecraft:empty', 4)], (1, 2)),
    ])
    table('chest', 'chests/tower_top', [
        pool([item('dream_journal_fragment', 10, (1, 2)), item('chrome_pearl', 6, (1, 2)), item('serbim_ingot', 8, (2, 5)), item('siftite_ingot', 3),
              item('music_disc_lullaby', 2), item('slingshot', 3), item('minecraft:book', 8, extra=[ENCHANT]), item('star_shard', 5, (1, 3))], (3, 5)),
        pool([item('siftite_upgrade_smithing_template', 1), item('minecraft:empty', 3)]),
        pool([item('minecraft:feather', 4, (3, 8)), item('minecraft:phantom_membrane', 2, (1, 3)), item('minecraft:golden_apple', 2),
              item('minecraft:empty', 4)]),
    ])
    table('chest', 'chests/temple_vault', [
        pool([item('siftite_upgrade_smithing_template', 1, (1, 2))]),
        pool([item('chrome_pearl', 8, (2, 4)), item('siftite_ingot', 6, (1, 3)), item('sift_drum', 4), item('sift_gate_frame', 1),
              item('music_disc_lullaby', 3), item('minecraft:enchanted_book', 4, extra=[ENCHANT]), item('star_shard', 6, (2, 4))], (3, 5)),
        pool([item('warden_core', 1), item('minecraft:empty', 7)]),
        pool([item('minecraft:enchanted_golden_apple', 1), item('minecraft:totem_of_undying', 1), item('minecraft:heart_of_the_sea', 1),
              item('minecraft:empty', 9)]),
    ])
    table('chest', 'chests/deep_shrine', [
        pool([item('warden_core', 1)], condition=chance(0.35)),
        pool([item('minecraft:echo_shard', 8, (2, 5)), item('minecraft:sculk_sensor', 6, (1, 3)), item('sift_gate_frame', 1),
              item('siftite_ingot', 4, (1, 2)), item('siftite_upgrade_smithing_template', 2), item('glowbell_vine', 6, (2, 4)),
              item('music_disc_lullaby', 2)], (3, 5)),
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
    table('chest', 'chests/chrome_well', [
        pool([item('chrome_bucket', 3), item('chrome_pearl', 5, (1, 2)), item('glowing_slime_ball', 10, (2, 5)), item('chrome_reeds', 8, (2, 6)),
              item('star_shard', 3)], (2, 4)),
    ])
    table('archaeology', 'archaeology/sift_common', [pool([
        item('glowing_slime_ball', 3), item('dream_journal_fragment', 3), item('raw_serbim', 2), item('pitcher_bulb', 2), item('choir_pod', 2),
        item('echo_seed', 2), item('minecraft:pink_dye', 1), item('minecraft:light_blue_dye', 1), item('blush_bricks', 2), item('minecraft:brick', 1)])])
    table('archaeology', 'archaeology/sift_rare', [pool([
        item('chrome_pearl', 3), item('star_shard', 3), item('music_disc_lullaby', 1), item('siftite_upgrade_smithing_template', 1),
        item('siftite_nugget', 3)])])


def sniffer_and_modifiers():
    # Sniffers in The Sift dig up the dimension's exclusive seeds.
    table('gift', 'gameplay/sniffer_digging_sift', [pool([item('choir_pod', 3), item('echo_seed', 3), item('pitcher_bulb', 2)])])
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


def trades():
    def trade(name, wants, gives, max_uses=8, xp=5, wants_count=1, gives_count=1, wants2=None):
        t = {'gives': {'id': rl(gives), **({'count': gives_count} if gives_count > 1 else {})}, 'max_uses': max_uses, 'reputation_discount': 0.05,
             'wants': {'id': rl(wants), **({'count': wants_count} if wants_count > 1 else {})}, 'xp': xp}
        if wants2:
            t['additional_wants'] = {'id': rl(wants2[0]), 'count': wants2[1]}
        w(f'villager_trade/enchoer/{name}', t)
        return f'{NS}:enchoer/{name}'

    common = [
        trade('pearl_for_saplings', 'chrome_pearl', 'lullwood_sapling', gives_count=3),
        trade('pearl_for_wish_saplings', 'chrome_pearl', 'wishwood_sapling', gives_count=2),
        trade('slime_for_pearl', 'glowing_slime_ball', 'chrome_pearl', wants_count=12, max_uses=12),
        trade('hide_for_pearl', 'thick_hide', 'chrome_pearl', wants_count=4, max_uses=12),
        trade('pearl_for_choir_pods', 'chrome_pearl', 'choir_pod', gives_count=2),
        trade('pearl_for_echo_seeds', 'chrome_pearl', 'echo_seed', gives_count=2),
        trade('pearl_for_bulbs', 'chrome_pearl', 'pitcher_bulb', gives_count=4),
        trade('pearl_for_glowbells', 'chrome_pearl', 'glowbell_vine', gives_count=3),
        trade('shards_for_pearls', 'star_shard', 'chrome_pearl', gives_count=2, max_uses=16),
        trade('emerald_for_pearl', 'minecraft:emerald', 'chrome_pearl', wants_count=3, max_uses=16),
        trade('pearl_for_raw_serbim', 'chrome_pearl', 'raw_serbim', gives_count=3),
    ]
    rare = [
        trade('pearls_for_drum', 'chrome_pearl', 'sift_drum', wants_count=6, max_uses=4, xp=15),
        trade('pearls_for_template', 'chrome_pearl', 'siftite_upgrade_smithing_template', wants_count=16, max_uses=2, xp=30),
        trade('pearls_for_disc', 'chrome_pearl', 'music_disc_lullaby', wants_count=10, max_uses=1, xp=20),
        trade('pearls_for_slingshot', 'chrome_pearl', 'slingshot', wants_count=8, max_uses=2, xp=15),
        trade('shards_for_warden_core', 'star_shard', 'warden_core', wants_count=24, max_uses=1, xp=40, wants2=('chrome_pearl', 16)),
    ]
    for t in common:
        GA.tag('villager_trade', f'{NS}:enchoer/common', t)
    for t in rare:
        GA.tag('villager_trade', f'{NS}:enchoer/rare', t)
    w('trade_set/enchoer/common', {'amount': 5, 'random_sequence': f'{NS}:trade_set/enchoer/common', 'trades': f'#{NS}:enchoer/common'})
    w('trade_set/enchoer/rare', {'amount': 2, 'random_sequence': f'{NS}:trade_set/enchoer/rare', 'trades': f'#{NS}:enchoer/rare'})


def jukebox():
    w('jukebox_song/lullaby', {'comparator_output': 7, 'description': {'translate': f'jukebox_song.{NS}.lullaby'}, 'length_in_seconds': 154.0,
                               'sound_event': f'{NS}:music_disc.lullaby'})


def generate():
    entity_loot()
    wild_creature_loot()
    chest_loot()
    sniffer_and_modifiers()
    trades()
    jukebox()
    __import__('songs').generate()  # songs & instruments (agent D)
    __import__('caravans').generate()  # C: Caravans, music crystals, prism
    print('data ok')
