"""Sea & sky (agents F + W): the Magic Kelp Forest, the Deep Dark Ocean and the sky flora (Chime Bells, Organ Reeds, cloud
puffs; the sky biome itself is the Sky Island, tools/sky_islands.py); the Gobbler; fish meat and sushi.

Hooks (one line each):
  * gen_assets.generate()  -> assets(GA): sounds, block models' text, recipes, tags, lang, Codex
  * gen_assets.gen_block() -> gen_block(GA, b) for the 'sea_*' model kinds
  * gen_world.generate()   -> world(GW): features, biomes, biome placement, surface rules, loot
"""
from __future__ import annotations

import json
import os

NS = 'thesift'
KELPS = ('rose', 'azure', 'amber')
SEA_BIOMES = ('magic_kelp_forest', 'deep_dark_ocean')
BIOMES = SEA_BIOMES  # W-sky: the Sound Garden became the Sky Island biome (tools/sky_islands.py)


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# ============================================================================ sounds

SOUNDS = {
    'entity.gobbler.ambient': [('event:entity.warden.ambient', 0.9, 0.55), ('ambient/underwater/additions/bass_whale1', 0.7, 1.3),
                               ('event:entity.warden.listening', 0.8, 0.6)],
    'entity.gobbler.hurt': [('event:entity.warden.hurt', 1.0, 0.7), ('event:entity.guardian.hurt', 0.8, 0.5)],
    'entity.gobbler.death': [('event:entity.warden.death', 1.0, 0.75)],
    'entity.gobbler.flop': [('event:entity.guardian.flop', 1.0, 0.5)],
    'entity.gobbler.sniff': [('event:entity.warden.sniff', 1.0, 0.7), ('block/sculk_sensor/sculk_clicking2', 0.8, 0.6)],
    'entity.gobbler.lunge': [('event:entity.warden.roar', 0.9, 1.2), ('event:entity.warden.angry', 1.0, 0.7)],
    'entity.gobbler.gulp': [('event:entity.generic.drink', 1.0, 0.4), ('event:entity.player.burp', 1.0, 0.45)],
    'entity.gobbler.spit': [('event:entity.llama.spit', 1.0, 0.5), ('ambient/underwater/additions/bubbles2', 1.0, 0.8)],
    'entity.gobbler.calm': [('event:entity.warden.listening', 0.8, 1.3), ('block/amethyst/resonate2', 0.8, 0.8)],
    'music.magic_kelp_forest': [('music/game/water/axolotl', 0.6, 1.0), ('music/game/water/dragon_fish', 0.6, 1.0),
                                ('music/game/water/shuniji', 0.6, 1.0), ('music/game/infinite_amethyst', 0.6, 1.0)],
    'music.deep_dark_ocean': [('music/game/deeper', 0.6, 1.0), ('music/game/ancestry', 0.6, 1.0), ('music/game/eld_unknown', 0.6, 1.0)],
    'music.sound_garden': [('music/game/featherfall', 0.6, 1.0), ('music/game/floating_dream', 0.6, 1.0), ('music/game/komorebi', 0.6, 1.0),
                           ('music/game/echo_in_the_wind', 0.6, 1.0)],
    # layers: the kelp forest bubbles and shimmers; the deep rumbles with whale song; the garden sings with wind chimes
    'ambient.magic_kelp_forest.loop': [('ambient/underwater/underwater_ambience', 0.35, 1.25)],
    'ambient.magic_kelp_forest.additions': [('block/amethyst/shimmer', 0.4, 1.4), ('ambient/underwater/additions/bubbles1', 0.5, 1.2),
                                            ('ambient/underwater/additions/bubbles3', 0.5, 1.3), ('block/amethyst/resonate1', 0.35, 1.6),
                                            ('ambient/underwater/additions/animal1', 0.4, 1.3)],
    'ambient.deep_dark_ocean.loop': [('ambient/underwater/underwater_ambience', 0.5, 0.6)],
    'ambient.deep_dark_ocean.additions': [('ambient/underwater/additions/bass_whale1', 0.6, 0.8), ('ambient/underwater/additions/bass_whale2', 0.6, 0.7),
                                          ('ambient/underwater/additions/dark1', 0.6, 0.7), ('ambient/underwater/additions/dark3', 0.6, 0.6),
                                          ('mob/warden/heartbeat_1', 0.25, 0.6), ('ambient/underwater/additions/earth_crack', 0.5, 0.6)],
    'ambient.deep_dark_ocean.mood': [('ambient/cave/cave11', 0.7, 0.5), ('mob/warden/nearby_close_1', 0.5, 0.5),
                                     ('ambient/underwater/additions/dark2', 0.7, 0.5)],
    'ambient.sound_garden.loop': [('item/elytra/elytra_loop', 0.18, 0.55)],
    'ambient.sound_garden.additions': [('block/amethyst/resonate1', 0.45, 1.2), ('block/amethyst/resonate3', 0.45, 1.5),
                                       ('block/note_block/chime', 0.35, 1.0), ('block/note_block/bell', 0.3, 0.8), ('block/bell/resonate', 0.25, 1.5),
                                       ('item/goat_horn/call1', 0.35, 0.5), ('ambient/underwater/additions/bass_whale2', 0.4, 1.5),
                                       ('ambient/nether/soulsand_valley/wind1', 0.3, 1.3)],
}
SUBTITLES = {
    'entity.gobbler.ambient': 'Gobbler groans', 'entity.gobbler.hurt': 'Gobbler hurts', 'entity.gobbler.death': 'Gobbler dies',
    'entity.gobbler.flop': 'Gobbler flops', 'entity.gobbler.sniff': 'Gobbler senses something', 'entity.gobbler.lunge': 'Gobbler lunges',
    'entity.gobbler.gulp': 'Gobbler gulps', 'entity.gobbler.spit': 'Gobbler spits', 'entity.gobbler.calm': 'Gobbler is lulled',
    'ambient.magic_kelp_forest.loop': 'Kelp sways', 'ambient.magic_kelp_forest.additions': 'Kelp shimmers',
    'ambient.deep_dark_ocean.loop': 'The deep presses in', 'ambient.deep_dark_ocean.additions': 'Something vast sings',
    'ambient.deep_dark_ocean.mood': 'The deep rumbles', 'ambient.sound_garden.loop': 'Wind blows', 'ambient.sound_garden.additions': 'Wind chimes ring',
}
STREAM = {'music.magic_kelp_forest', 'music.deep_dark_ocean', 'music.sound_garden', 'ambient.magic_kelp_forest.loop',
          'ambient.deep_dark_ocean.loop', 'ambient.sound_garden.loop'}


# ============================================================================ block models

def _cross(GA, name, tex, light, render='minecraft:cutout'):
    els = []
    for angle in (45, -45):
        els.append({'from': [0.8, 0, 8], 'to': [15.2, 16, 8], 'shade': False, 'light_emission': light,
                    'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                    'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#cross'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#cross'}}})
    m = {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'render_type': render,
         'textures': {'particle': f'{NS}:block/{tex}', 'cross': f'{NS}:block/{tex}'}, 'elements': els}
    GA.note_textures(m)
    GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
    return f'{NS}:block/{name}'


def gen_block(GA, b):
    bid, k = b['id'], b['model']
    st = os.path.join(GA.A, 'blockstates', bid + '.json')
    if k == 'sea_kelp':
        body, tip = _cross(GA, bid, bid, 7), _cross(GA, bid + '_tip', bid + '_tip', 12)
        GA.write(st, {'variants': {'tip=false': {'model': body}, 'tip=true': {'model': tip}}})
        GA.item_generated(bid, f'block/{bid}_tip')
    elif k == 'sea_anemone':
        GA.write(st, {'variants': {'': {'model': _cross(GA, bid, bid, 9)}}})
        GA.item_generated(bid, f'block/{bid}')
    elif k == 'sea_bell':
        GA.write(st, {'variants': {'ringing=false': {'model': _cross(GA, bid, bid, 6)},
                                   'ringing=true': {'model': _cross(GA, bid + '_ringing', bid + '_ringing', 15)}}})
        GA.item_generated(bid, f'block/{bid}')
    elif k == 'sea_reed':
        GA.write(st, {'variants': {'half=lower': {'model': _cross(GA, bid + '_bottom', bid + '_bottom', 3)},
                                   'half=upper': {'model': _cross(GA, bid + '_top', bid + '_top', 5)}}})
        GA.item_generated(bid, f'block/{bid}_top')
    else:
        raise ValueError(f'no sea asset rule for {bid} ({k})')
    GA.LANG[f'block.{NS}.{bid}'] = b['name']


# ============================================================================ assets, recipes, tags, text

def assets(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    GA.STREAM.update(STREAM)
    shaped, shapeless, smelt, tag, LANG = GA.shaped, GA.shapeless, GA.smelt, GA.tag, GA.LANG
    # --- cooking: every fish has raw and cooked meat
    for raw, cooked in (('fanfare_eel', 'cooked_fanfare_eel'), ('gobbler_fillet', 'cooked_gobbler_fillet')):
        smelt(cooked, raw, cooked, 0.35, 200, ('smelting', 'smoking'))
        smelt(f'{cooked}_campfire', raw, cooked, 0.35, 600, ('campfire_cooking',))
        for t in (raw, cooked):
            tag('item', 'minecraft:fishes', rl(t))
        tag('item', f'{NS}:slumbler_food', rl(raw))
    # --- sushi: raw fish rolled in dried kelp with a strand of glowkelp
    for k in KELPS:
        tag('item', f'{NS}:glowkelp', rl(f'{k}_glowkelp'))
        tag('block', f'{NS}:glowkelp', rl(f'{k}_glowkelp'))
        tag('block', 'minecraft:sword_efficient', rl(f'{k}_glowkelp'))
    for fish in ('kazoo_fish', 'fanfare_eel'):
        shapeless(f'{fish}_sushi', [fish, 'minecraft:dried_kelp', f'#{NS}:glowkelp'], f'{fish}_sushi', 2, 'food')
    shapeless('gobbler_sushi', ['gobbler_fillet', 'sculk_bladder', 'minecraft:dried_kelp', f'#{NS}:glowkelp'], 'gobbler_sushi', 3, 'food')
    shapeless('sushi_platter', ['kazoo_fish_sushi', 'fanfare_eel_sushi', 'gobbler_sushi', '#minecraft:wooden_slabs'],
              'sushi_platter', 1, 'food')
    # --- dyes from the new plants
    for src, dye in (('rose_glowkelp', 'pink'), ('azure_glowkelp', 'light_blue'), ('amber_glowkelp', 'orange'), ('chime_bell', 'cyan'),
                     ('organ_reed', 'purple'), ('abyss_anemone', 'cyan')):
        shapeless(f'{dye}_dye_from_{src}', [src], f'minecraft:{dye}_dye', 2 if src == 'organ_reed' else 1, 'misc', f'{dye}_dye')
    # --- tags
    for b in ('cloud_block', 'coral_sand'):
        tag('block', f'{NS}:sift_plantable', rl(b))       # Sift plants grow on them, Sift creatures spawn on them
    tag('block', 'minecraft:dampens_vibrations', rl('cloud_block'))
    tag('block', 'minecraft:flowers', rl('organ_reed'))
    tag('block', f'{NS}:resonant', rl('chime_bell'))
    tag('block', f'{NS}:resonant', rl('organ_reed'))
    for e in ('gobbler',):
        tag('entity_type', 'minecraft:aquatic', rl(e))
        tag('entity_type', f'{NS}:chrome_dwellers', rl(e))
    for b in BIOMES:
        tag('worldgen/biome', f'{NS}:is_sift', rl(b))
    # --- text
    LANG.update({
        f'entity.{NS}.gobbler': 'Gobbler',
        f'biome.{NS}.magic_kelp_forest': 'Magic Kelp Forest', f'biome.{NS}.deep_dark_ocean': 'Sculk Ocean',  # W1: renamed, id kept
        f'codex.{NS}.gobbler.title': 'Gobbler', f'codex.{NS}.gobbler.tagline': 'Hostile - blind, and very hungry',
        f'codex.{NS}.gobbler.body': 'A Warden-kin catfish of the Sculk Ocean: sculk skin threaded with glowing veins, a fanged mouth wider than you, soul-lantern lures on its whiskers and a ribcage of glowing souls. It has no eyes. It feels you - fast swimming, thrashing and music carry to it through the water, so swim slowly or sneak past. If it finds you it lunges, gulps you down and spits you out. Sneak to wriggle free. The Lullaby lulls it for two minutes. Drops fillets and a glowing sculk bladder.',
        f'codex.{NS}.sea_and_sky.title': 'Seas and Skies', f'codex.{NS}.sea_and_sky.tagline': 'Real water, and clouds you can walk on',
        f'codex.{NS}.sea_and_sky.body': 'Only two seas in the Sift hold real water. The warm Magic Kelp Forest glows with rose, azure and amber glowkelp over coral-pink sand, full of Kazoo Fish and coral. The cold Sculk Ocean is dark teal water over trenches, ridges and glowing Sculk Coral reefs - mind the Gobblers. Far above the land drift the Sky Islands, where Chime Bells ring and Organ Reeds hum as you walk through them, and Sky Whales sing.',
        f'codex.{NS}.sushi.title': 'Sushi', f'codex.{NS}.sushi.tagline': 'Raw fish, dried kelp, a strand of glowkelp',
        f'codex.{NS}.sushi.body': 'Every Sift fish has raw and cooked meat (fish killed by fire drop it cooked). Roll a raw fish with dried kelp and any glowkelp. Kazoo Fish Sushi: water breathing. Fanfare Eel Sushi: dolphin\'s grace. Gobbler Sushi (add the sculk bladder) cures Sculk Corruption. Set all four on a wooden slab for a Sushi Platter: a long, safe dive.',
    })


# ============================================================================ worldgen

def _patch_json(GW, rel, fn):
    path = os.path.join(GW.D, rel + '.json')
    with open(path) as f:
        obj = json.load(f)
    obj = fn(obj) or obj
    GW.w(rel, obj)


def _spawns(**cats):
    def entries(lst):
        return [{'type': rl(t), 'count': ({'type': 'minecraft:uniform', 'min_inclusive': a, 'max_inclusive': b} if a != b else a), 'weight': wt}
                for t, wt, a, b in lst]
    by = {c: [] for c in ('creature', 'monster', 'ambient', 'water_creature', 'underground_water_creature', 'water_ambient', 'axolotls', 'misc')}
    for c, lst in cats.items():
        by[c] = entries(lst)
    return {'argument': {'spawn_costs': {}, 'spawns_by_category': by}, 'modifier': 'overlay'}


def _biome(GW, name, *, fog, sky, water, water_fog, grass, foliage, temp, down, spawns, parts, feats, music, loop, additions, mood,
           extra=None):
    steps = [[] for _ in range(11)]
    for step, f in feats:
        steps[step].append(rl(f))
    order = [rl(n) for n in GW.PLACED]
    for st in steps:
        st.sort(key=lambda f: (order.index(f) if f in order else len(order), f))
    music_entry = {'max_delay': 15000, 'min_delay': 5000, 'replace_current_music': True, 'sound': rl(music)}
    attrs = {
        'minecraft:audio/ambient_sounds': {'loop': rl(loop), 'additions': {'sound': rl(additions), 'tick_chance': 0.0125},
                                           'mood': {'block_search_extent': 8, 'offset': 2.0, 'sound': rl(mood), 'tick_delay': 5000}},
        'minecraft:audio/background_music': {'default': music_entry, 'underwater': music_entry},
        'minecraft:gameplay/natural_mob_spawns': spawns,
        'minecraft:visual/ambient_particles': GW.particles(*parts),
        'minecraft:visual/fog_color': fog,
        'minecraft:visual/sky_color': sky,
        'minecraft:visual/water_fog_color': water_fog,
    }
    attrs.update(extra or {})
    GW.w(f'worldgen/biome/{name}', {
        'attributes': attrs, 'carvers': GW.CARVERS, 'downfall': down,
        'effects': {'water_color': water, 'grass_color': grass, 'foliage_color': foliage},
        'features': steps, 'has_precipitation': False, 'temperature': temp,
    })


def _features(GW):
    feature, placed, state, count, rarity, survive, BIOME = GW.feature, GW.placed, GW.state, GW.count, GW.rarity, GW.survive, GW.BIOME
    sea_floor = [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'}]
    in_water = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water'}}
    # real water for the two oceans, before anything else decorates them
    feature('sea_flood', {'type': f'{NS}:sea_flood', 'biomes': [rl(b) for b in SEA_BIOMES], 'sea_level': 63})
    placed('sea_flood', 'sea_flood', [])
    # glowkelp columns, one colour per column
    for k in KELPS:
        feature(f'glowkelp_{k}', {'type': 'minecraft:block_column', 'direction': 'up', 'prioritize_tip': True,
                                  'allowed_placement': {'type': 'minecraft:matching_fluids', 'fluids': 'minecraft:water'},
                                  'layers': [{'height': {'type': 'minecraft:uniform', 'min_inclusive': 3, 'max_inclusive': 17},
                                              'provider': state(f'{k}_glowkelp', tip=False, waterlogged=True)},
                                             {'height': 1, 'provider': state(f'{k}_glowkelp', tip=True, waterlogged=True)}]})
    feature('glowkelp', {'type': 'minecraft:simple_random_selector',
                         'features': [{'feature': rl(f'glowkelp_{k}'), 'placement': []} for k in KELPS]})
    placed('glowkelp', 'glowkelp', [count(60)] + sea_floor + [in_water, survive('rose_glowkelp'), BIOME])
    # the deep: rock pillars, sculk-veined anemones
    feature('deep_pillar', {'type': f'{NS}:jagged_pillar', 'body': state('hushslate', axis='y'), 'accent': state('cobbled_hushslate'),
                            'coat': state('minecraft:sculk'), 'glow': state('minecraft:sculk_catalyst', bloom=False),
                            'min_height': 34, 'max_height': 96, 'radius': 5.5})
    placed('deep_pillar', 'deep_pillar', [rarity(3)] + sea_floor + [BIOME])
    feature('abyss_anemone', {'type': 'minecraft:simple_block', 'to_place': state('abyss_anemone', waterlogged=True)})
    placed('patch_abyss_anemone', 'abyss_anemone', [count(28)] + sea_floor + [in_water, BIOME])
    # the Sound Garden: bell meadows and reed stands on the cloud islands, and loose puffs of cloud above them
    feature('chime_bell', {'type': 'minecraft:simple_block', 'to_place': state('chime_bell', ringing=False)})
    feature('organ_reed', {'type': 'minecraft:simple_block', 'to_place': state('organ_reed', half='lower')})
    on_top = [{'type': 'minecraft:in_square'}, {'type': 'minecraft:heightmap', 'heightmap': 'WORLD_SURFACE_WG'}, BIOME]
    placed('patch_chime_bell', 'chime_bell', [count(5)] + on_top + GW.surface_patch(40, 6, 2) + [survive('chime_bell')])
    placed('patch_organ_reed', 'organ_reed', [count(2)] + on_top + GW.surface_patch(14, 4, 2) + [survive('organ_reed')])
    cloud = state('cloud_block')
    feature('cloud_puff', {'type': f'{NS}:floating_island', 'top': cloud, 'soil': cloud, 'stone': cloud, 'min_radius': 3, 'max_radius': 7,
                           'min_lift': 6, 'max_lift': 26})
    placed('cloud_puff', 'cloud_puff', [rarity(3)] + on_top)


def _biomes(GW):
    common = GW.COMMON_UNDERGROUND
    _biome(GW, 'magic_kelp_forest', fog='#c7b8ff', sky='#86d8ff', water='#3fe6d6', water_fog='#1fb8c4', grass='#f59ac6', foliage='#f9b3d4',
           temp=0.8, down=0.8, music='music.magic_kelp_forest', loop='ambient.magic_kelp_forest.loop',
           additions='ambient.magic_kelp_forest.additions', mood='ambient.sift.mood',
           spawns=_spawns(water_ambient=[('kazoo_fish', 16, 4, 8), ('minecraft:tropical_fish', 10, 4, 8)],
                          water_creature=[('fanfare_eel', 4, 1, 1), ('minecraft:dolphin', 1, 1, 2)],
                          creature=[('bulb', 3, 1, 3), ('slumbler', 2, 1, 1)]),
           parts=[('chrome_bubble', 0.004), ('glow_dust', 0.003), ('sift_note', 0.0008), ('star_sparkle', 0.0015)],
           feats=[(0, 'sea_flood')] + common + [(9, 'glowkelp'), (9, 'minecraft:warm_ocean_vegetation'), (9, 'minecraft:seagrass_warm'),
                                                 (9, 'minecraft:sea_pickle')])
    # W1: the Sculk Ocean (biome id kept: deep_dark_ocean) is built in tools/sculk_world.py (_ocean_biome)
    # W-sky: the Sound Garden is the Sky Island biome now (tools/sky_islands.py); its bells, reeds and cloud puffs are made above


def _placement(dim):
    """The two oceans split the Chrome seas by temperature (W-sky: the sky itself is the Sky Island, placed in
    tools/sky_islands.py). Multi-noise placement blends neighbouring climates, so every edge is a gradual shore or a
    gradual rise."""
    pts = dim['generator']['biome_source']['biomes']
    F = [-1.0, 1.0]

    def pt(b, t=F, h=F, c=F, e=F, d=0.0, wd=F):
        return {'biome': rl(b), 'parameters': {'temperature': t, 'humidity': h, 'continentalness': c, 'erosion': e, 'depth': d, 'weirdness': wd,
                                               'offset': 0.0}}
    for p in pts:
        if p['biome'] == rl('chrome_lakes'):
            p['parameters']['temperature'] = [-1.0, 0.1]
            p['parameters']['continentalness'] = [-0.455, -0.19]
    pts.append(pt('magic_kelp_forest', t=[0.1, 1.0], c=[-1.2, -0.19]))
    pts.append(pt('deep_dark_ocean', t=[-1.0, 0.1], c=[-1.2, -0.455]))
    return dim


def _surface(GW, rule):
    """Coral-pink sand under the kelp (W-sky: the Sky Islands' ground is in tools/sky_islands.py)."""
    def biome_is(b):
        return {'type': 'minecraft:biome', 'biome_is': [rl(b)]}

    def cond(c, then):
        return {'type': 'minecraft:condition', 'if_true': c, 'then_run': then}

    def block(b, **props):
        return {'type': 'minecraft:block', 'result_state': GW.state(b, **props)}

    def seq(*r):
        return {'type': 'minecraft:sequence', 'sequence': list(r)}
    mine = [
        cond(biome_is('magic_kelp_forest'), seq(cond('minecraft:on_floor', block('coral_sand')), cond('minecraft:under_floor', block('coral_sand')),
                                                cond('minecraft:deep_under_floor', block('chime_sandstone')))),
        # W1: the Sculk Ocean's floor rules live in tools/sculk_world.py (_surface)
    ]
    for r in rule['sequence']:
        if isinstance(r, dict) and r.get('type') == 'minecraft:condition' and r.get('if_true', {}).get('type') == 'minecraft:above_preliminary_surface':
            r['then_run']['sequence'][0:0] = mine
            return rule
    raise ValueError('the_sift material rule: no above_preliminary_surface branch')


def world(GW):
    _features(GW)
    _biomes(GW)
    _patch_json(GW, 'dimension/the_sift', _placement)
    _patch_json(GW, 'worldgen/material_rule/the_sift', lambda r: _surface(GW, r))
    import gen_data as GD
    GD.table('entity', 'entities/gobbler', [
        GD.pool([GD.item('gobbler_fillet', count=(1, 3), extra=[GD.SMELT_IF_BURNING, GD.LOOTING])]),
        GD.pool([GD.item('sculk_bladder', extra=[GD.LOOTING])], condition=GD.chance(0.65)),
        GD.pool([GD.item('minecraft:bone', count=(0, 2), extra=[GD.LOOTING])]),
        GD.pool([GD.item('minecraft:echo_shard')], condition={'type': 'minecraft:all_of', 'terms': [GD.PLAYER_KILL, GD.chance(0.12)]}),
        # W1: swallowed with some drowned traveller - a sheet (CLEAN: the Lullaby that lulls it; the Tide Song is gone)
        GD.pool([GD.item('music_sheet_lullaby')], condition={'type': 'minecraft:all_of', 'terms': [GD.PLAYER_KILL, GD.chance(0.15)]}),
    ])
