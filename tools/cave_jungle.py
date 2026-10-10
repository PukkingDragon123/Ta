"""P4 Cave Jungle: the life of the Cave Jungle (`thesift:cave_jungle`, the warm humid caves of tools/caves.py).

* Giant Pitcher Plants (`giant_pitcher`, two blocks tall; `pitcher_sprout`): carnivorous pitchers on the moss that lure
  and swallow Glow Flies; pick their Pitcher Pods (`pitcher_pod`, roast them: `roasted_pitcher_pod`; #thesift:stomper_food);
  pods planted on Sift dirt/grass or moss sprout and grow when watered with Chrome. They replace the configured feature
  `thesift:cave_jungle_pitcher_spot` (a dripleaf stalk until now).
* Glow Fly, Crocotodo, Mantis, Colossus Ponder (+ Ponder Tadpoles in bored logs: `bored_log`), Cruncher (also in the
  Sift Caves) - models in tools/jungle_mobs.py, art in tools/cave_jungle_art.py, Java in registry/ModCaveJungle,
  entity/jungle/*, block/GiantPitcherBlock, PitcherSproutBlock, BoredLogBlock, item/GlowLampItem, client/CaveJungleClient.
* Drops: Glow Gland (-> Glow Lamp, a rechargeable pulsing light), Mantis Scythe (for the Reaper Scythe), Giant Ponder Egg
  (-> Baked Ponder Egg), Magnesium (the Cruncher's waste), Cruncher Tooth (for the Chain Dagger). The Glow Fly joins
  #thesift:bauxite_stabilizers.

Hooks (one line each): spec.py -> declare(block, item); mobs.py -> ALL.update(jungle_mobs.MODELS); gen_assets.gen_block ('jl_*') ->
gen_block(GA, b); gen_assets.generate() -> assets(GA) (before gen_sounds); gen_assets.check_sounds reads ModCaveJungle.java;
gen_world.generate() -> world(GW) (after caves.world); gen_textures.main() -> cave_jungle_art.textures(out).
"""
import os

NS = 'thesift'
MOBS = ('glow_fly', 'crocotodo', 'mantis', 'colossus_ponder', 'ponder_tadpole', 'cruncher')


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# ============================================================================ spec (blocks, items)

def declare(block, item):
    popped = '.pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)'
    block('giant_pitcher', 'custom', 'BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().noOcclusion().strength(0.5F)'
          '.sound(SoundType.BIG_DRIPLEAF).lightLevel(s -> 4)' + popped, cls='GiantPitcherBlock', model='jl_pitcher', item=False, loot='none',
          name='Giant Pitcher Plant', tab='nature')
    block('pitcher_sprout', 'custom', 'BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().noOcclusion().instabreak()'
          '.sound(SoundType.CROP).randomTicks()' + popped, cls='PitcherSproutBlock', model='jl_sprout', item=False, loot='none', tab='nature')
    block('bored_log', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG).randomTicks()', cls='BoredLogBlock', model='jl_log',
          tags=['axe'], loot='none', tab='nature')
    item('pitcher_pod', cls='BlockItem:pitcher_sprout', props='new Item.Properties().food(ModCaveJungle.PITCHER_POD_FOOD).useItemDescriptionPrefix()',
         tab='items')
    item('roasted_pitcher_pod', props='new Item.Properties().food(ModCaveJungle.ROASTED_PITCHER_POD_FOOD)')
    item('glow_gland', props='new Item.Properties()', name='Glow Gland')
    item('glow_lamp', cls='GlowLampItem', props='new Item.Properties().durability(128)', tab='tools')
    item('mantis_scythe', props='new Item.Properties().rarity(Rarity.UNCOMMON)', name='Mantis Scythe Arm')
    item('ponder_egg', props='new Item.Properties().stacksTo(16)', name='Giant Ponder Egg')
    item('baked_ponder_egg', props='new Item.Properties().stacksTo(16).food(ModCaveJungle.BAKED_PONDER_EGG_FOOD, ModCaveJungle.BAKED_PONDER_EGG_CONSUMABLE)')
    item('magnesium', props='new Item.Properties()')
    item('cruncher_tooth', props='new Item.Properties()')
    for m in MOBS:
        item(f'{m}_spawn_egg', cls='SpawnEggItem', props=f'new Item.Properties().spawnEgg(ModCaveJungle.{m.upper()}.get())', tab='eggs')


# ============================================================================ block models (gen_assets.gen_block)

def _faces(tex, uv=None, skip=(), cull=None):
    f = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        if d in skip:
            continue
        e = {'texture': tex, 'uv': list(uv or [0, 0, 16, 16])}
        if cull and d in cull:
            e['cullface'] = d
        f[d] = e
    return f


def _box(frm, to, faces, rot=None):
    e = {'from': list(frm), 'to': list(to), 'faces': faces}
    if rot:
        e['rotation'] = rot
    return e


def _cross(h, tex='#leaves'):
    out = []
    for a in (45, -45):
        out.append(_box([0.8, 0, 8], [15.2, h, 8], {'north': {'texture': tex, 'uv': [0, 16 - h, 16, 16]},
                                                    'south': {'texture': tex, 'uv': [0, 16 - h, 16, 16]}},
                        {'origin': [8, 8, 8], 'axis': 'y', 'angle': a, 'rescale': True}))
    return out


def gen_block(GA, b):
    bid, k = b['id'], b['model']
    A = GA.A
    if k == 'jl_pitcher':
        tex = {'particle': f'{NS}:block/giant_pitcher_side', 'side': f'{NS}:block/giant_pitcher_side', 'rim': f'{NS}:block/giant_pitcher_rim',
               'lid': f'{NS}:block/giant_pitcher_lid', 'juice': f'{NS}:block/giant_pitcher_juice', 'leaves': f'{NS}:block/giant_pitcher_leaves',
               'pod': f'{NS}:block/giant_pitcher_pod'}
        # the lower half: leaves round the foot, a bulging belly narrowing into the neck
        lower = _cross(12) + [
            _box([3, 0, 3], [13, 11, 13], _faces('#side', [3, 5, 13, 16], skip=('up',), cull=('down',))),
            _box([4, 11, 4], [12, 16, 12], _faces('#side', [4, 0, 12, 5], skip=('up', 'down'))),
        ]
        # the upper half: the neck, a flaring mouth full of juice, the ribbed red lip and the hood leaning over it
        upper = [
            _box([4, 0, 4], [12, 5, 12], _faces('#side', [4, 11, 12, 16], skip=('up', 'down'))),
            _box([3, 5, 3], [13, 9, 13], dict(_faces('#side', [3, 0, 13, 4], skip=('up',)), up={'texture': '#juice', 'uv': [3, 3, 13, 13]})),
            _box([2, 9, 2], [14, 11, 4], _faces('#rim', [2, 0, 14, 2])),
            _box([2, 9, 12], [14, 11, 14], _faces('#rim', [2, 4, 14, 6])),
            _box([2, 9, 4], [4, 11, 12], _faces('#rim', [4, 8, 12, 10])),
            _box([12, 9, 4], [14, 11, 12], _faces('#rim', [4, 12, 12, 14])),
            _box([3, 10, 12.5], [13, 19, 13.5], _faces('#lid', [3, 0, 13, 9]), {'origin': [8, 10, 13], 'axis': 'x', 'angle': -22.5}),
        ]
        pods = [
            _box([0.5, 6, 6], [2.5, 9, 8.5], _faces('#pod', [6, 4, 8, 7])),
            _box([13.5, 5, 8], [15.5, 8, 10.5], _faces('#pod', [2, 8, 4, 11])),
            _box([6.5, 6, 0.5], [9, 9, 2.5], _faces('#pod', [10, 2, 12, 5])),
        ]
        for name, els in ((bid + '_lower', lower), (bid + '_upper', upper), (bid + '_upper_pods', upper + pods)):
            m = {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'textures': tex, 'elements': els}
            GA.note_textures(m)
            GA.write(os.path.join(A, 'models/block', name + '.json'), m)
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'half=lower,pods=false': {'model': f'{NS}:block/{bid}_lower'}, 'half=lower,pods=true': {'model': f'{NS}:block/{bid}_lower'},
            'half=upper,pods=false': {'model': f'{NS}:block/{bid}_upper'}, 'half=upper,pods=true': {'model': f'{NS}:block/{bid}_upper_pods'}}})
    elif k == 'jl_sprout':
        variants = {}
        for age in range(3):
            GA.block_model(f'{bid}_stage{age}', 'minecraft:block/cross', {'cross': f'block/{bid}_stage{age}'})
            for wet in ('false', 'true'):
                variants[f'age={age},watered={wet}'] = {'model': f'{NS}:block/{bid}_stage{age}'}
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': variants})
    elif k == 'jl_log':
        GA.block_model(bid, 'minecraft:block/cube_column', {'side': f'block/{bid}', 'end': 'minecraft:block/jungle_log_top'})
        GA.block_model(bid + '_horizontal', 'minecraft:block/cube_column_horizontal', {'side': f'block/{bid}', 'end': 'minecraft:block/jungle_log_top'})
        variants = {}
        for n in range(4):
            variants[f'axis=y,tadpoles={n}'] = {'model': f'{NS}:block/{bid}'}
            variants[f'axis=z,tadpoles={n}'] = {'model': f'{NS}:block/{bid}_horizontal', 'x': 90}
            variants[f'axis=x,tadpoles={n}'] = {'model': f'{NS}:block/{bid}_horizontal', 'x': 90, 'y': 90}
        GA.write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': variants})
        GA.item_block(bid)
    else:
        raise ValueError(f'no P4 Cave Jungle asset rule for {bid} ({k})')


# ============================================================================ sounds (vanilla events)

SOUNDS = {
    'entity.glow_fly.ambient': [('event:entity.bee.loop', 0.25, 1.6), ('event:block.amethyst_block.chime', 0.4, 1.6)],
    'entity.glow_fly.hurt': [('event:entity.bee.hurt', 0.8, 1.3), ('event:entity.allay.hurt', 0.5, 1.2)],
    'entity.glow_fly.death': [('event:entity.allay.death', 0.8, 1.1), ('event:block.amethyst_block.chime', 0.8, 0.7)],
    'entity.glow_fly.flash': [('event:entity.firework_rocket.blast', 0.7, 1.6), ('event:block.beacon.power_select', 0.8, 1.9)],
    'entity.glow_fly.absorb': [('event:block.beacon.power_select', 0.4, 1.4), ('event:block.amethyst_block.chime', 0.5, 1.2)],
    'entity.glow_fly.give': [('event:entity.bee.pollinate', 0.8, 1.2), ('event:entity.firework_rocket.twinkle', 0.4, 1.5)],
    'entity.crocotodo.ambient': [('event:entity.parrot.ambient', 0.9, 0.6), ('event:entity.chicken.ambient', 0.7, 0.6)],
    'entity.crocotodo.hurt': [('event:entity.parrot.hurt', 0.9, 0.7)],
    'entity.crocotodo.death': [('event:entity.parrot.death', 1.0, 0.7)],
    'entity.crocotodo.snap': [('event:entity.evoker_fangs.attack', 0.7, 1.3), ('event:entity.fox.bite', 0.8, 0.8)],
    'entity.crocotodo.peck': [('event:entity.parrot.eat', 0.8, 0.8)],
    'entity.crocotodo.step': [('event:entity.chicken.step', 0.6, 0.7)],
    'entity.mantis.ambient': [('event:entity.silverfish.ambient', 0.8, 0.5), ('event:entity.spider.step', 0.4, 0.6)],
    'entity.mantis.hurt': [('event:entity.silverfish.hurt', 1.0, 0.6)],
    'entity.mantis.death': [('event:entity.silverfish.death', 1.0, 0.5), ('event:block.amethyst_cluster.break', 1.0, 0.8)],
    'entity.mantis.strike': [('event:entity.player.attack.sweep', 1.0, 0.7)],
    'entity.mantis.slice': [('event:entity.player.attack.strong', 1.0, 1.2), ('event:entity.evoker_fangs.attack', 0.6, 1.6)],
    'entity.mantis.dive': [('event:entity.phantom.swoop', 1.0, 1.2), ('event:entity.phantom.flap', 0.8, 1.4)],
    'entity.mantis.crunch': [('event:block.amethyst_cluster.break', 1.0, 0.9), ('event:entity.generic.eat', 0.8, 0.7)],
    'entity.mantis.step': [('event:entity.spider.step', 0.5, 0.8)],
    'entity.colossus_ponder.croak': [('event:entity.frog.ambient', 1.8, 0.35)],
    'entity.colossus_ponder.hurt': [('event:entity.frog.hurt', 1.4, 0.45)],
    'entity.colossus_ponder.death': [('event:entity.frog.death', 1.6, 0.4), ('event:entity.ravager.death', 0.6, 0.8)],
    'entity.colossus_ponder.stomp': [('event:item.mace.smash_ground', 1.2, 0.6), ('event:entity.ravager.step', 1.0, 0.6)],
    'entity.colossus_ponder.roar': [('event:entity.ravager.roar', 1.2, 0.8), ('event:entity.frog.ambient', 1.6, 0.3)],
    'entity.colossus_ponder.lay': [('event:entity.frog.lay_spawn', 1.2, 0.6), ('event:block.slime_block.place', 1.0, 0.6)],
    'entity.ponder_tadpole.ambient': [('event:entity.tadpole.flop', 0.6, 1.0)],
    'entity.ponder_tadpole.hurt': [('event:entity.tadpole.hurt', 0.8, 0.9)],
    'entity.ponder_tadpole.death': [('event:entity.tadpole.death', 0.8, 0.9)],
    'entity.ponder_tadpole.bite': [('event:entity.fox.bite', 0.7, 1.5)],
    'entity.ponder_tadpole.burrow': [('event:block.wood.break', 0.8, 0.8), ('event:block.rooted_dirt.break', 0.6, 1.2)],
    'entity.cruncher.ambient': [('event:entity.hoglin.ambient', 0.7, 1.5)],
    'entity.cruncher.hurt': [('event:entity.hoglin.hurt', 0.8, 1.5)],
    'entity.cruncher.death': [('event:entity.hoglin.death', 0.8, 1.5)],
    'entity.cruncher.bite': [('event:entity.hoglin.attack', 0.8, 1.6), ('event:entity.fox.bite', 0.6, 0.8)],
    'entity.cruncher.crunch': [('event:block.calcite.break', 1.0, 0.7), ('event:block.stone.break', 0.8, 0.8)],
    'entity.cruncher.waste': [('event:block.tuff.break', 0.6, 1.4), ('event:entity.player.burp', 0.5, 0.6)],
    'entity.cruncher.step': [('event:entity.hoglin.step', 0.4, 1.6)],
    'block.giant_pitcher.snap': [('event:block.big_dripleaf.tilt_down', 1.0, 0.7), ('event:entity.fox.bite', 0.6, 0.6)],
    'block.giant_pitcher.gulp': [('event:block.slime_block.place', 0.8, 0.7), ('event:block.bubble_column.bubble_pop', 0.8, 0.8)],
}
SUBTITLES = {
    'entity.glow_fly.ambient': 'Glow Fly hums', 'entity.glow_fly.hurt': 'Glow Fly hurts', 'entity.glow_fly.death': 'Glow Fly dies',
    'entity.glow_fly.flash': 'Glow Fly flashes', 'entity.glow_fly.absorb': 'Glow Fly drinks light', 'entity.glow_fly.give': 'Glow Fly feeds a plant',
    'entity.crocotodo.ambient': 'Crocotodo squawks', 'entity.crocotodo.hurt': 'Crocotodo hurts', 'entity.crocotodo.death': 'Crocotodo dies',
    'entity.crocotodo.snap': 'Crocotodo snaps', 'entity.crocotodo.peck': 'Crocotodo pecks', 'entity.crocotodo.step': 'Footsteps',
    'entity.mantis.ambient': 'Mantis clicks', 'entity.mantis.hurt': 'Mantis hurts', 'entity.mantis.death': 'Mantis dies',
    'entity.mantis.strike': 'Mantis strikes', 'entity.mantis.slice': 'Mantis slices', 'entity.mantis.dive': 'Mantis dives',
    'entity.mantis.crunch': 'Mantis eats crystal', 'entity.mantis.step': 'Footsteps',
    'entity.colossus_ponder.croak': 'Colossus Ponder croaks', 'entity.colossus_ponder.hurt': 'Colossus Ponder hurts',
    'entity.colossus_ponder.death': 'Colossus Ponder dies', 'entity.colossus_ponder.stomp': 'Heavy footstep',
    'entity.colossus_ponder.roar': 'Colossus Ponder roars', 'entity.colossus_ponder.lay': 'Colossus Ponder lays an egg',
    'entity.ponder_tadpole.ambient': 'Tadpole wriggles', 'entity.ponder_tadpole.hurt': 'Tadpole hurts', 'entity.ponder_tadpole.death': 'Tadpole dies',
    'entity.ponder_tadpole.bite': 'Tadpole bites', 'entity.ponder_tadpole.burrow': 'Tadpole bores into wood',
    'entity.cruncher.ambient': 'Cruncher grunts', 'entity.cruncher.hurt': 'Cruncher hurts', 'entity.cruncher.death': 'Cruncher dies',
    'entity.cruncher.bite': 'Cruncher bites', 'entity.cruncher.crunch': 'Cruncher crunches rock', 'entity.cruncher.waste': 'Cruncher leaves Magnesium',
    'entity.cruncher.step': 'Footsteps',
    'block.giant_pitcher.snap': 'Pitcher snaps shut', 'block.giant_pitcher.gulp': 'Pitcher gulps',
}


# ============================================================================ data, tags and text (gen_assets.generate, before gen_sounds)

def assets(GA):
    import gen_data as D
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    # loot
    D.table('entity', 'entities/glow_fly', [D.pool([D.item('glow_gland', count=(1, 1))]),
                                            D.pool([D.item('minecraft:glowstone_dust', count=(0, 2), extra=[D.LOOTING])])])
    D.table('entity', 'entities/crocotodo', [D.pool([D.item('minecraft:feather', count=(1, 3), extra=[D.LOOTING])]),
                                             D.pool([D.item('minecraft:glow_berries', count=(0, 1))])])
    D.table('entity', 'entities/mantis', [D.pool([D.item('mantis_scythe', count=(1, 1), extra=[D.LOOTING])], condition=D.PLAYER_KILL),
                                          D.pool([D.item('minecraft:amethyst_shard', count=(1, 3), extra=[D.LOOTING])])])
    D.table('entity', 'entities/colossus_ponder', [D.pool([D.item('ponder_egg', count=(1, 1))]),
                                                   D.pool([D.item('minecraft:slime_ball', count=(2, 5), extra=[D.LOOTING])]),
                                                   D.pool([D.item('lumen_moss_block', count=(1, 3))])])
    D.table('entity', 'entities/ponder_tadpole', [D.pool([D.item('minecraft:slime_ball', count=(0, 1))])])
    D.table('entity', 'entities/cruncher', [D.pool([D.item('cruncher_tooth', count=(0, 2), extra=[D.LOOTING])]),
                                            D.pool([D.item('magnesium', count=(0, 2))])])
    # the pitcher: pods from its lower half (more when it has set them); the sprout gives its pod back
    lower = {'type': 'minecraft:match_block', 'blocks': rl('giant_pitcher'), 'state': {'half': 'lower'}}
    pods = {'type': 'minecraft:match_block', 'blocks': rl('giant_pitcher'), 'state': {'half': 'lower', 'pods': 'true'}}
    D.table('block', 'blocks/giant_pitcher', [D.pool([D.item('pitcher_pod', count=(1, 2))], condition=lower),
                                              D.pool([D.item('pitcher_pod', count=(1, 2))], condition=pods)])
    D.table('block', 'blocks/pitcher_sprout', [D.pool([D.item('pitcher_pod')])])
    D.table('block', 'blocks/bored_log', [D.pool([D.item('minecraft:jungle_log')])])
    # tags
    for i in ('pitcher_pod', 'roasted_pitcher_pod', 'minecraft:pitcher_pod', 'minecraft:torchflower', 'minecraft:pink_petals'):
        GA.tag('item', f'{NS}:stomper_food', rl(i))
    GA.tag('entity_type', f'{NS}:bauxite_stabilizers', rl('glow_fly'))  # P4: a living Glow Fly steadies Bauxite like a Nib
    GA.tag('block', 'minecraft:logs', rl('bored_log'))
    GA.tag('block', 'minecraft:logs_that_burn', rl('bored_log'))
    GA.tag('block', 'minecraft:mineable/hoe', rl('giant_pitcher'))
    GA.tag('block', 'minecraft:mineable/hoe', rl('pitcher_sprout'))
    # recipes: roast the pods and bake the egg (furnace, smoker, campfire); the Glow Lamp
    GA.smelt('roasted_pitcher_pod', 'pitcher_pod', 'roasted_pitcher_pod', xp=0.35, kinds=('smelting', 'smoking', 'campfire_cooking'))
    GA.smelt('baked_ponder_egg', 'ponder_egg', 'baked_ponder_egg', xp=0.5, time=300, kinds=('smelting', 'smoking', 'campfire_cooking'))
    GA.shaped('glow_lamp', [' n ', 'nGn', ' n '], {'n': 'minecraft:copper_nugget', 'G': 'glow_gland'}, 'glow_lamp', 1, 'equipment')
    # spawns: the jungle's creatures in the Cave Jungle; the Cruncher in the ordinary Sift Caves as well
    GA.write(os.path.join(GA.RES, 'data', NS, 'neoforge', 'biome_modifier', 'cave_jungle_creatures.json'), {
        'type': 'neoforge:add_spawns', 'biomes': rl('cave_jungle'),
        'spawners': [_spawn('glow_fly', 20, 2, 4), _spawn('crocotodo', 12, 2, 4), _spawn('colossus_ponder', 2, 1, 1), _spawn('mantis', 4, 1, 1),
                     _spawn('cruncher', 6, 1, 3)]})
    GA.write(os.path.join(GA.RES, 'data', NS, 'neoforge', 'biome_modifier', 'sift_caves_crunchers.json'), {
        'type': 'neoforge:add_spawns', 'biomes': rl('sift_caves'), 'spawners': [_spawn('cruncher', 8, 1, 3)]})
    GA.LANG.update(lang())


def _spawn(mob, weight, lo, hi):
    count = lo if lo == hi else {'type': 'minecraft:uniform', 'min_inclusive': lo, 'max_inclusive': hi}
    return {'type': rl(mob), 'count': count, 'weight': weight}


def lang():
    c = f'codex.{NS}'
    L = {f'entity.{NS}.{m}': ' '.join(w.capitalize() for w in m.split('_')) for m in MOBS}
    L.update({
        f'band.{NS}.instrument.glow_chimes': 'Glow Chimes', f'band.{NS}.instrument.beak_clapper': 'Beak Clapper',
        f'band.{NS}.instrument.bog_tuba': 'Bog Tuba', f'band.{NS}.instrument.bubble_pipe': 'Bubble Pipe',
        f'band.{NS}.instrument.scythe_strings': 'Scythe Strings', f'band.{NS}.instrument.rock_crusher': 'Rock Crusher',
        f'item.{NS}.glow_lamp.desc': 'Pulses with light while held; use it for a great flash. Recharges in bright light.',
        f'{c}.glow_fly.title': 'Glow Fly', f'{c}.glow_fly.tagline': 'Passive - a living lantern',
        f'{c}.glow_fly.body': ('A big golden firefly whose abdomen is a lantern. It drinks light - hovering against glowing moss, '
                               'glowbells and torches until its lantern fills - then carries it to plants and pours it into them, '
                               'and they grow. Giant Pitcher Plants smell like a plant starving for light: a Glow Fly that lingers over '
                               'one is swallowed. Hit one and it flashes, dazzling everything watching, and darts away. Its gland makes '
                               'the Glow Lamp; a living Glow Fly near a Europhy Table steadies Bauxite.'),
        f'{c}.glow_fly.notes': 'Habitat: Cave Jungle|Temper: Shy, flashes when hit|Diet: Light|Drops: Glow Gland',
        f'{c}.crocotodo.title': 'Crocotodo', f'{c}.crocotodo.tagline': 'Neutral - the flightless jungle bird',
        f'{c}.crocotodo.body': ('A plump moss-teal bird with a crocodile\'s long toothed snout, armoured scutes down its back, a flame '
                                'crest and wings far too small to fly with. Flocks waddle about the Cave Jungle pecking at the moss. '
                                'Chicks run; adults stand their ground and snap - those jaws hurt. Breed them with Glow Berries or '
                                'Pitcher Pods.'),
        f'{c}.crocotodo.notes': 'Habitat: Cave Jungle|Temper: Neutral, snaps back|Diet: Glow berries, Pitcher Pods|Drops: Feathers',
        f'{c}.mantis.title': 'Mantis', f'{c}.mantis.tagline': 'Hostile - the jungle\'s great predator',
        f'{c}.mantis.body': ('A towering praying mantis with azure crystals growing out of its back, neck and arms - it eats crystal, '
                             'and it shows. It hunts Caravans above all, and anything else that walks. From afar it opens its wings '
                             'and dives; close in it strikes with its scythes, and may grab you and slice at you, held between the '
                             'blades. Hurt and alone, it cracks open a crystal cluster to heal. Its Scythe Arm is a prize.'),
        f'{c}.mantis.notes': 'Habitat: Cave Jungle|Temper: Hostile, hunts Caravans|Diet: Crystal, prey|Drops: Mantis Scythe Arm, crystal',
        f'{c}.colossus_ponder.title': 'Colossus Ponder', f'{c}.colossus_ponder.tagline': 'Neutral - a frog the size of a house',
        f'{c}.colossus_ponder.body': ('A giant frog wearing the jungle floor: glowing moss, ferns, glowcaps and vines grow on its back. '
                                      'It ponders, and mostly ignores you - but it never looks where it treads, and its steps hurt '
                                      'small things nearby. Its tadpoles live in bored logs; hurt one and every Ponder near goes on a '
                                      'rampage, bounding after you and landing like a falling house. Now and then it lays a giant '
                                      'egg: bake it for a meal that fills you for a day.'),
        f'{c}.colossus_ponder.notes': 'Habitat: Cave Jungle|Temper: Oblivious, rampages for its young|Diet: Whatever wanders by|Drops: Giant Ponder Egg',
        f'{c}.ponder_tadpole.title': 'Ponder Tadpole', f'{c}.ponder_tadpole.tagline': 'Neutral - all teeth',
        f'{c}.ponder_tadpole.body': ('The Colossus Ponder\'s young: a big head, a mouth of needle teeth and a see-through fin of a tail. '
                                     'They wriggle near their parent and bore into logs to live in them, leaving the wood pocked with '
                                     'holes. Break an infested log and they burst out at you. Hurt one and its parent comes.'),
        f'{c}.ponder_tadpole.notes': 'Habitat: Logs of the Cave Jungle|Temper: Bites back|Diet: Wood grubs|Drops: Slime',
        f'{c}.cruncher.title': 'Cruncher', f'{c}.cruncher.tagline': 'Hostile - it eats rock',
        f'{c}.cruncher.body': ('A small, bad-tempered cave raptor plated in crusts of Magnesite and dusted with glittering Magnesium. '
                               'It hunts out Magnesite - seams in the walls, or blocks left lying about - crunches it, and a little '
                               'later leaves a pile of Magnesium behind: the only way to get any. It goes for anything that walks '
                               'into its cave. Its teeth are worth keeping.'),
        f'{c}.cruncher.notes': 'Habitat: Cave Jungle, Sift Caves|Temper: Aggressive|Diet: Magnesite|Drops: Cruncher Teeth, Magnesium',
        f'{c}.giant_pitcher.title': 'Giant Pitcher Plant', f'{c}.giant_pitcher.tagline': 'It eats Glow Flies',
        f'{c}.giant_pitcher.body': ('A carnivorous pitcher two blocks tall, its belly half full of glowing juice. Glow Flies come to feed '
                                    'it and are swallowed; a fed pitcher sets Pitcher Pods round its lip - pick them by hand, roast them '
                                    '(Stompers love them). Plant a pod on Sift dirt, grass or moss and water the sprout with a Chrome '
                                    'Bucket (or grow it within four blocks of Chrome) and it rises into a new pitcher. Pour Chrome into '
                                    'a hungry pitcher and it sets pods at once. Do not climb in.'),
        f'{c}.glow_lamp.title': 'Glow Lamp', f'{c}.glow_lamp.tagline': 'A light that recharges itself',
        f'{c}.glow_lamp.body': ('A Glow Fly\'s light gland in a cage of copper nuggets. Held, it pulses with a soft light that follows '
                                'you, slowly draining its charge; use it for one great flash that hangs in the air and dazzles '
                                'monsters near you. Like the fly it came from, it refills itself in bright light.'),
        f'{c}.magnesium.title': 'Magnesium', f'{c}.magnesium.tagline': 'What the Cruncher leaves behind',
        f'{c}.magnesium.body': ('A silvery, glittering powder - Magnesite that has been through a Cruncher. Collect the piles they leave. '
                                'Smiths of the Sift have plans for it.'),
    })
    return L


# ============================================================================ worldgen (gen_world.generate, after caves.world)

def world(GW):
    import caves as CV
    st, feature, placed, count, BIOME = GW.state, GW.feature, GW.placed, GW.count, GW.BIOME
    on_moss = {'type': 'minecraft:block_predicate_filter', 'predicate': {'type': 'minecraft:all_of', 'predicates': [
        {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
        {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air', 'offset': [0, 1, 0]},
        {'type': 'minecraft:matching_blocks', 'blocks': [GW.rl('lumen_moss_block'), 'minecraft:moss_block', 'minecraft:rooted_dirt'],
         'offset': [0, -1, 0]}]}}

    def pitcher(pods):
        return {'feature': {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
                            'direction': 'up', 'prioritize_tip': False, 'layers': [
                                {'height': 1, 'provider': st('giant_pitcher', half='lower', pods=pods)},
                                {'height': 1, 'provider': st('giant_pitcher', half='upper', pods=pods)}]}, 'placement': []}
    # P4: the Giant Pitcher Plants take the place of the dripleaf stalks (same feature id, so the biome lists keep it)
    feature('cave_jungle_pitcher_spot', {'type': 'minecraft:simple_random_selector', 'features': [pitcher(True), pitcher(False), pitcher(False)]})
    placed('cave_jungle_pitcher_spot', 'cave_jungle_pitcher_spot', [count(4), CV.SQ, CV._height(CV.BOTTOM, 120)] + CV._floor() + [on_moss, BIOME])
    # stumps of jungle wood the Ponder Tadpoles have bored into
    feature('cave_jungle_bored_stump', {'type': 'minecraft:block_column', 'allowed_placement': {'type': 'minecraft:matching_block_tag', 'tag': 'minecraft:air'},
                                        'direction': 'up', 'prioritize_tip': False, 'layers': [
                                            {'height': {'type': 'minecraft:uniform', 'min_inclusive': 1, 'max_inclusive': 2},
                                             'provider': st('minecraft:jungle_log', axis='y')},
                                            {'height': 1, 'provider': st('bored_log', axis='y', tadpoles=2)},
                                            {'height': {'type': 'minecraft:uniform', 'min_inclusive': 0, 'max_inclusive': 1},
                                             'provider': st('minecraft:jungle_log', axis='y')}]})
    placed('cave_jungle_bored_stump', 'cave_jungle_bored_stump', [count(1), GW.rarity(2), CV.SQ, CV._height(CV.BOTTOM, 120)] + CV._floor()
           + [on_moss, BIOME])
    path = os.path.join(GW.D, 'worldgen/biome/cave_jungle.json')
    import json
    b = json.load(open(path))
    if rl('cave_jungle_bored_stump') not in b['features'][9]:
        b['features'][9].append(rl('cave_jungle_bored_stump'))
    GW.w('worldgen/biome/cave_jungle', b)


def items():
    """No items16 sprites: the art is written by tools/cave_jungle_art.py (gen_textures hook)."""
    return {}
