"""A4 cave creatures: the Jailer and the Sculklings.

* The Jailer (`thesift:jailer`): a tall, skinny, blind kin of the Warden - dark sculk skin, an
  exposed ribcage over a glowing soul, long thin limbs and a faceless head crowned with antler-like
  tendrils - hunched over the giant cell of sculk-iron bars it carries. It hunts by sound and slams
  the cell down over whoever it hears, then hauls them around inside it.
* Sculklings (`thesift:sculkling`): small sculk goblins with giant bat ears (and no wings) that
  skitter through dark caves in packs, giggle, screech, swarm, snatch shiny things and run - and
  cover their ears and flee from music.
* CR4: the Jailer's cell is studded with Sculkite (`thesift:sculkite`, item tag #thesift:sculkite), the
  dark sculk crystal it grows its bars from - its only drop, the raw material of echo gear and Stomper
  armour (their recipes and the sculkite ore come later). The Cypole of the Sculk Swamp lives in
  tools/cypole.py and is hooked in through this module's hooks.

Hooks (one line each): spec.py -> declare(block, item); mobs.py -> ALL.update(MODELS);
gen_assets.generate() -> assets(GA) (sounds, lang, Codex text, loot, tags, spawns);
items16.all_items() -> items() (spawn eggs). Java: registry/ModCaveCreatures, entity/cave/*,
client/CaveCreaturesClient (JailerModel / SculklingModel animate the geometry made here).
"""
from __future__ import annotations

import os

from modelkit import Model

NS = 'thesift'


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def rows(w, h, fn):
    """A w x h texel map from fn(x, y) -> char."""
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


# =========================================================================== spec (items)

def declare(block, item):
    item("jailer_spawn_egg", cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModCaveCreatures.JAILER.get())", tab="eggs")
    item("sculkling_spawn_egg", cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModCaveCreatures.SCULKLING.get())", tab="eggs")
    # CR4: the Jailer's drop, a raw material (echo gear, Stomper armour)
    item("sculkite", props="new Item.Properties().rarity(Rarity.UNCOMMON)", name="Scukite")  # F1: shown as "Scukite" (id kept)
    __import__('cypole').declare(block, item)  # CR4: the Cypole's spawn egg


# =========================================================================== THE JAILER
# CAVE v4: the Jailer was designed afresh in tools/jailer.py (a hulking sculk warden whose ribcage is the cell).


# =========================================================================== SCULKLING
SPAL = {
    'skin': '#123b45', 'skin_l': '#1d5661', 'skin_d': '#09252d',
    'belly': '#1a4b53', 'belly_l': '#25616a', 'belly_d': '#103a42',
    'ear': '#0e3038', 'ear_l': '#174652', 'ear_d': '#071c22', 'vein': '#3ff5e6', 'vein_l': '#c8fffb',
    'claw': '#d8d2bf', 'claw_l': '#f0ead8', 'claw_d': '#a39c88', 'tooth': '#efe8d2', 'mouth': '#04131a', 'gum': '#1f6670',
    'nose': '#215d66', 'nose_l': '#2e7a84', 'nose_d': '#123f46', 'pit': '#020608', 'soul': '#5ff8ff', 'soul_d': '#169aa6',
}


def _ear_main(w, h, mirror):
    """A huge bat ear (the lower, broad part): a straight inner edge, a rounded outer edge, a paler
    hollow in the middle and three thin glowing veins fanning out from the root ('_' is cut away).
    Drawn for the left ear; the right one is mirrored."""
    def px(x, y):
        t = y / (h - 1)                                   # 0 at the top of this segment, 1 at the root
        left = 1.2 * (1.0 - t)
        right = w - 1 - (1.0 - t) ** 1.5 * w * 0.33
        if x < left or x > right:
            return '_'
        if x > right - 1.0 or x < left + 0.7:
            return 'r'
        for ang in (0.12, 0.5, 0.95):                     # veins: from the root, up and outward
            vx = w * 0.22 + (h + 1 - y) * ang
            if abs(x - vx) < 0.5 and y > 1:
                return 'V' if y > h * 0.35 else 'v'
        hollow = ((x - w * 0.5) / (w * 0.3)) ** 2 + ((y - h * 0.58) / (h * 0.42)) ** 2
        return 'l' if hollow < 1.0 else '.'
    out = rows(w, h, px)
    return [r[::-1] for r in out] if mirror else out


def _ear_tip(w, h, mirror):
    """The ear's pointed tip, which flops and twitches on its own."""
    def px(x, y):
        t = y / (h - 1)                                   # 0 at the point, 1 where it joins the ear
        left = w * 0.3 * (1.0 - t)
        right = w * 0.42 + (w * 0.58 - 1) * t ** 0.8
        if x < left or x > right:
            return '_'
        if x > right - 1.0 or x < left + 0.7:
            return 'r'
        vx = w * 0.4 + (h - y) * 0.2
        if abs(x - vx) < 0.5 and y > 2:
            return 'v'
        return 'l' if 0.35 < t and left + 2 < x < right - 2 else '.'
    out = rows(w, h, px)
    return [r[::-1] for r in out] if mirror else out


def sculkling() -> Model:
    m = Model('sculkling', (64, 64), SPAL, {'sculkling': {}}, res=2)
    ek = {'V': 'vein', 'v': 'soul_d', 'r': 'ear_d', 'l': 'ear_l'}
    # ---- short, bent goblin legs with clawed feet
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(1.6 * sx, 18.5, 0.8), rot=(-0.35, 0, 0))
        leg.cube((-1, 0, -1), (2, 3.5, 2), **mc('skin', clusters=0.2, rim=False))
        shin = leg.part(f'{side}_shin', pivot=(0, 3.5, 0), rot=(0.55, 0, 0))
        shin.cube((-0.75, 0, -0.75), (1.5, 2.5, 1.5), **mc('skin_d', clusters=0.0, rim=False))
        foot = shin.part(f'{side}_foot', pivot=(0, 2.5, 0), rot=(-0.2, 0, 0))
        foot.cube((-1, -0.25, -2.25), (2, 0.75, 3), **mc('skin_d', clusters=0.0, rim=False), faces={
            'north': mc('claw', clusters=0.0, rim=False, hd=True, map=['c.c.'], keys={'c': 'claw'}),
            'up': mc('skin_d', clusters=0.0, rim=False, hd=True, map=['c.c.'] + ['....'] * 5, keys={'c': 'claw'}),
        })

    # ---- a small hunched pot-bellied body with a soul flickering in its chest
    body = m.part('body', pivot=(0, 18.5, 0.8), rot=(0.4, 0, 0))
    def flame(x, y):
        # a little soul flame flickering in its chest, as in a Warden's
        dx = abs(x - 4.5)
        bulb = ((dx / 2.0) ** 2 + ((y - 6.5) / 2.2) ** 2) < 1.0
        point = 2 <= y <= 6 and dx < (y - 1.5) * 0.45
        if bulb or point:
            return 'S' if (dx < 1.0 and 4 <= y <= 7) else 's'
        return 'b' if y == 10 and 1 <= x <= 8 else '.'
    belly = rows(10, 12, flame)
    body.cube((-2.5, -6, -2), (5, 6, 4), **mc('skin', clusters=0.25, bands=[(4, 'belly')]), faces={
        'north': dict(color='belly', pattern='mc', clusters=0.1, hd=True, map=belly, keys={'s': 'soul_d', 'S': 'soul', 'b': 'skin_d'}, glow_keys='sS'),
        'south': mc('skin', clusters=0.2, map=['.....', '..g..', '.....', '..g..', '.....', '.....'], keys={'g': 'vein'}, glow_keys='g'),
    })
    for nz in (-1.0, 0.75):
        body.cube((-0.5, -6.5 + nz * 0.5, 1.5 + nz), (1, 1, 1), **mc('vein', clusters=0.0, rim=False, glow=True))  # sculk nubs on the spine

    # ---- the big goblin head: no eyes, sensory pits, a bat's leaf nose and a wide toothy grin
    head = body.part('head', pivot=(0, -6, -0.25), rot=(-0.4, 0, 0))
    grin = rows(14, 11, lambda x, y: (
        'p' if (y == 2 and x in (2, 4, 9, 11)) or (y == 3 and x in (3, 10)) else
        'g' if (y == 1 and x in (2, 3, 10, 11)) else
        'M' if (y == 8 and 1 <= x <= 12) or (y == 7 and x in (1, 12)) or (y == 6 and x in (0, 13)) else
        't' if (y == 9 and 2 <= x <= 11 and x % 2 == 0) else
        'm' if (y == 9 and 2 <= x <= 11) or (y == 10 and 3 <= x <= 10) else '.'))
    head.cube((-3.5, -5.5, -3.5), (7, 5.5, 6), **mc('skin', clusters=0.3), faces={
        'north': dict(color='skin', pattern='mc', clusters=0.1, hd=True, map=grin,
                      keys={'p': 'pit', 'g': 'soul_d', 'M': 'skin_d', 't': 'tooth', 'm': 'mouth'}, glow_keys='g'),
        'up': mc('skin', clusters=0.25, map=['.......', '...g...', '.......', '..g.g..', '.......', '.......'], keys={'g': 'vein'}, glow_keys='g'),
    })
    nose = head.part('nose', pivot=(0, -3.0, -3.5))
    nose.cube((-1.25, -1, -1), (2.5, 1.75, 1.25), **mc('nose', clusters=0.0, rim=False), faces={
        'north': mc('nose', clusters=0.0, rim=False, hd=True, map=['..ll.', '.l..l', '.p.p.', '.....'], keys={'l': 'nose_l', 'p': 'pit'}),
    })
    nose.cube((-0.5, -2.5, -0.75), (1, 1.5, 0.5), **mc('nose_l', clusters=0.0, rim=False))  # the nose leaf
    jaw = head.part('jaw', pivot=(0, -0.5, -0.5))
    jaw.cube((-3, 0, -3), (6, 1.5, 4), **mc('skin_d', clusters=0.1, rim=False), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, map=rows(12, 8, lambda x, y: 't' if y == 0 and x % 2 == 1 else
                                                                      'G' if 4 <= y <= 6 and 3 <= x <= 8 else '.'),
                 keys={'t': 'tooth', 'G': 'gum'}),
        'north': mc('skin_d', clusters=0.0, rim=False, hd=True, map=rows(12, 3, lambda x, y: 't' if y == 0 and x % 2 == 1 else '.'),
                    keys={'t': 'tooth'}),
    })
    # ---- giant ears: two segments each so they can twitch, flop and fold over
    for side, sx in (('left', 1), ('right', -1)):
        mir = sx < 0
        ear = head.part(f'{side}_ear', pivot=(3.2 * sx, -4.5, 0.5), rot=(0.1, -0.35 * sx, 0.45 * sx))
        main = dict(color='ear', pattern='mc', clusters=0.0, rim=False, hd=True, map=_ear_main(16, 18, mir), keys=ek, glow_keys='Vv')
        back = dict(main, map=_ear_main(16, 18, not mir))
        ear.cube((-1 if sx > 0 else -7, -9, 0), (8, 9, 0), color='ear', pattern='mc', faces={'north': main, 'south': back})
        ear.cube((-1.25 if sx < 0 else -0.25, -2, -0.5), (1.5, 2.5, 1), **mc('skin_d', clusters=0.0, rim=False))   # the ear's root
        tip = ear.part(f'{side}_ear_tip', pivot=(1.6 * sx, -9, 0), rot=(0, 0, 0.12 * sx))
        tmain = dict(color='ear', pattern='mc', clusters=0.0, rim=False, hd=True, map=_ear_tip(12, 14, mir), keys=ek, glow_keys='Vv')
        tback = dict(tmain, map=_ear_tip(12, 14, not mir))
        tip.cube((-2.6 if sx > 0 else -3.4, -7, 0), (6, 7, 0), color='ear', pattern='mc', faces={'north': tmain, 'south': tback})
    # ---- skinny arms with big pale claws for snatching
    for side, sx in (('left', 1), ('right', -1)):
        arm = body.part(f'{side}_arm', pivot=(2.6 * sx, -5.2, -0.5), rot=(-0.35, 0, -0.3 * sx))
        arm.cube((-0.6, -0.5, -0.6), (1.2, 4.5, 1.2), **mc('skin', clusters=0.1, rim=False))
        fore = arm.part(f'{side}_forearm', pivot=(0, 4, 0), rot=(-0.5, 0, 0))
        fore.cube((-0.5, 0, -0.5), (1, 3.5, 1), **mc('skin_d', clusters=0.0, rim=False))
        hand = fore.part(f'{side}_hand', pivot=(0, 3.5, 0))
        hand.cube((-0.9, 0, -0.9), (1.8, 1, 1.8), **mc('skin_d', clusters=0.0, rim=False))
        for fx in (-0.6, 0.6):
            hand.cube((fx - 0.25, 1, -0.75), (0.5, 2, 0.5), **mc('claw', clusters=0.0, rim=False))
        hand.cube((-0.25, 1, 0.4), (0.5, 1.5, 0.5), **mc('claw_d', clusters=0.0, rim=False))
    # ---- a thin whip of a tail
    tail = body.part('tail', pivot=(0, -0.75, 1.75), rot=(0.9, 0, 0))
    tail.cube((-0.5, -0.5, 0), (1, 1, 4), **mc('skin', clusters=0.0, rim=False))
    ttip = tail.part('tail_tip', pivot=(0, 0, 4), rot=(-0.6, 0, 0))
    ttip.cube((-0.4, -0.4, 0), (0.8, 0.8, 3.5), **mc('skin_d', clusters=0.0, rim=False), faces={'south': mc('vein', clusters=0.0, glow=True)})
    return m


MODELS = {'jailer': __import__('jailer').jailer, 'sculkling': sculkling}  # CAVE v4: tools/jailer.py
MODELS.update(__import__('cypole').MODELS)  # CR4: the Cypole (tools/cypole.py)


# =========================================================================== sounds (vanilla events)
SOUNDS = {
    'entity.jailer.ambient': [('event:entity.warden.ambient', 0.8, 0.7), ('event:entity.warden.tendril_clicks', 0.7, 0.8),
                              ('event:block.chain.step', 0.6, 0.6)],
    'entity.jailer.listen': [('event:entity.warden.listening', 1.0, 0.85), ('event:entity.warden.tendril_clicks', 1.0, 0.7)],
    'entity.jailer.step': [('event:entity.warden.step', 0.7, 1.1), ('event:block.chain.step', 0.4, 0.7)],
    'entity.jailer.hurt': [('event:entity.warden.hurt', 1.0, 0.85)],
    'entity.jailer.death': [('event:entity.warden.death', 1.0, 0.9), ('event:block.anvil.destroy', 0.6, 0.6)],
    'entity.jailer.emerge': [('event:entity.warden.emerge', 1.0, 0.9), ('event:entity.warden.dig', 0.8, 0.8)],
    'entity.jailer.windup': [('event:entity.warden.listening_angry', 1.0, 0.8), ('event:block.chain.hit', 0.8, 0.6)],
    'entity.jailer.slam': [('event:entity.warden.attack_impact', 1.2, 0.8), ('event:item.mace.smash_ground_heavy', 0.9, 0.8),
                           ('event:block.anvil.land', 0.7, 0.6)],
    'entity.jailer.trap': [('event:block.iron_door.close', 1.0, 0.6), ('event:block.vault.close_shutter', 1.0, 0.7),
                           ('event:block.chain.place', 1.0, 0.6)],
    'entity.jailer.squeeze': [('event:entity.iron_golem.damage', 0.8, 0.6), ('event:entity.warden.heartbeat', 1.0, 1.0)],
    'entity.jailer.rattle': [('event:block.chain.hit', 1.0, 0.7), ('event:block.iron.hit', 1.0, 0.8),
                             ('event:entity.zombie.attack_iron_door', 0.5, 1.4)],
    'entity.jailer.break': [('event:block.anvil.destroy', 1.0, 0.8), ('event:block.chain.break', 1.0, 0.6)],
    'entity.jailer.regrow': [('event:block.sculk_catalyst.bloom', 1.0, 0.8), ('event:block.chain.place', 0.8, 0.8)],
    # CR4 the harder cell: the heartbeat cue, the grip loosening, a good heave, squirming against it, a guard's kick
    'entity.jailer.pulse': [('event:entity.warden.heartbeat', 1.0, 1.15)],
    'entity.jailer.loosen': [('event:block.chain.step', 1.0, 0.65), ('event:block.chain.place', 0.9, 0.75)],
    'entity.jailer.heave': [('event:block.chain.break', 1.0, 0.7), ('event:block.anvil.land', 0.5, 1.4), ('event:block.iron.hit', 1.0, 0.6)],
    'entity.jailer.tighten': [('event:entity.iron_golem.damage', 0.6, 0.5), ('event:block.chain.hit', 0.8, 0.5)],
    'entity.jailer.kick': [('event:entity.warden.attack_impact', 1.0, 1.15), ('event:entity.ravager.stunned', 0.5, 1.4)],
    'entity.sculkling.ambient': [('event:entity.witch.celebrate', 0.5, 1.8), ('event:entity.vex.ambient', 0.5, 1.4),
                                 ('event:entity.allay.ambient_without_item', 0.4, 0.9)],
    'entity.sculkling.screech': [('event:entity.fox.screech', 0.8, 1.3), ('event:entity.bat.ambient', 1.0, 0.6),
                                 ('event:block.sculk_shrieker.shriek', 0.4, 1.8)],
    'entity.sculkling.hurt': [('event:entity.bat.hurt', 0.8, 0.8), ('event:entity.vex.hurt', 0.6, 1.2)],
    'entity.sculkling.death': [('event:entity.bat.death', 0.8, 0.8), ('event:entity.vex.death', 0.6, 1.3)],
    'entity.sculkling.step': [('event:entity.spider.step', 0.25, 1.6)],
    'entity.sculkling.snatch': [('event:entity.allay.item_taken', 1.0, 1.0), ('event:entity.witch.celebrate', 0.7, 2.0)],
    'entity.sculkling.scared': [('event:entity.bat.hurt', 0.7, 1.4), ('event:entity.vex.hurt', 0.6, 1.6)],
    'entity.sculkling.twitch': [('event:entity.warden.tendril_clicks', 0.6, 1.8), ('event:block.sculk_sensor.clicking', 0.5, 1.6)],
}
SUBTITLES = {
    'entity.jailer.ambient': 'Jailer rattles', 'entity.jailer.listen': 'Jailer listens', 'entity.jailer.step': 'Heavy footsteps',
    'entity.jailer.hurt': 'Jailer hurts', 'entity.jailer.death': 'Jailer dies', 'entity.jailer.emerge': 'Jailer emerges',
    'entity.jailer.windup': 'Jailer heaves its cell', 'entity.jailer.slam': 'Cell slams down', 'entity.jailer.trap': 'Cell locks shut',
    'entity.jailer.squeeze': 'Cell squeezes', 'entity.jailer.rattle': 'Bars rattle', 'entity.jailer.break': 'Bars break',
    'entity.jailer.regrow': 'Bars regrow', 'entity.jailer.pulse': "Jailer's heart thumps", 'entity.jailer.loosen': 'Grip loosens',
    'entity.jailer.heave': 'Bars buckle', 'entity.jailer.tighten': 'Grip tightens', 'entity.jailer.kick': 'Jailer kicks',
    'entity.sculkling.ambient': 'Sculkling giggles', 'entity.sculkling.screech': 'Sculkling screeches', 'entity.sculkling.hurt': 'Sculkling hurts',
    'entity.sculkling.death': 'Sculkling dies', 'entity.sculkling.step': 'Something skitters', 'entity.sculkling.snatch': 'Sculkling snatches something',
    'entity.sculkling.scared': 'Sculkling whimpers', 'entity.sculkling.twitch': 'Ears twitch',
}


def sounds(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    __import__('cypole').sounds(GA)  # CR4: the Cypole


# =========================================================================== data: loot, tags, spawns, text
SHINIES = ['minecraft:gold_ingot', 'minecraft:gold_nugget', 'minecraft:raw_gold', 'minecraft:iron_ingot', 'minecraft:copper_ingot',
           'minecraft:diamond', 'minecraft:emerald', 'minecraft:amethyst_shard', 'minecraft:lapis_lazuli', 'minecraft:quartz',
           'minecraft:netherite_ingot', 'minecraft:echo_shard', 'minecraft:golden_apple', 'minecraft:clock',
           'siftite_ingot', 'siftite_nugget', 'siftite_dust', 'chrome_pearl', 'skysong_gem', 'star_shard', 'prism_gem']


def data(GA):
    import gen_data as D
    for i in SHINIES:
        GA.tag('item', f'{NS}:sculkling_shinies', rl(i))
    # the Jailer (CR4): the Sculkite crystal it grows its cell from - its only drop
    D.table('entity', 'entities/jailer', [
        D.pool([D.item('sculkite', count=(2, 4), extra=[D.LOOTING])]),
    ])
    GA.tag('item', f'{NS}:sculkite', rl('sculkite'))
    # Sculklings hoard a little gold (anything they stole is dropped by the entity itself)
    D.table('entity', 'entities/sculkling', [
        D.pool([D.item('minecraft:gold_nugget', count=(0, 2), extra=[D.LOOTING])]),
        D.pool([D.item('minecraft:sculk_vein', count=(0, 1))]),
        D.pool([D.item('minecraft:amethyst_shard', 3), D.item('minecraft:emerald', 1), D.item('minecraft:echo_shard', 1)],
               condition={'type': 'minecraft:all_of', 'terms': [D.PLAYER_KILL, D.chance(0.06)]}),
    ])
    # spawns: all over the Sift, but the spawn rules keep them to dark caves (Jailers below y 0 or in the Deep Sift)
    GA.write(os.path.join(GA.RES, 'data', NS, 'neoforge', 'biome_modifier', 'cave_creatures.json'), {
        'type': 'neoforge:add_spawns', 'biomes': f'#{NS}:is_sift',
        'spawners': [{'type': rl('sculkling'), 'count': {'type': 'minecraft:uniform', 'min_inclusive': 3, 'max_inclusive': 5}, 'weight': 12},
                     {'type': rl('jailer'), 'count': 1, 'weight': 3}]})
    GA.LANG.update(lang())
    __import__('cypole').data(GA)  # CR4: the Cypole's loot, spawn tags, particle, band voice and text


def lang():
    L = {f'entity.{NS}.jailer': 'Jailer', f'entity.{NS}.sculkling': 'Sculkling', f'entity.{NS}.jail_cell': 'Jail Cell'}
    L.update({
        f'message.{NS}.jailer.trapped': "The Jailer has you! When its heart thumps and the bars glow, hit them - or struggle (sneak)!",
        f'message.{NS}.jailer.struggle': 'Its grip is too tight - wait for the bars to glow!',
        f'message.{NS}.jailer.beat': 'Listen for its heartbeat: strike the bars as they glow!',
        f'message.{NS}.sculkling.snatched': 'A Sculkling snatched your %s!',
        f'codex.{NS}.jailer.title': 'Jailer', f'codex.{NS}.jailer.tagline': 'Hostile - eyeless, and its ribs are a cell',  # CAVE v4
        f'codex.{NS}.jailer.body': ('A hulking, eyeless warden of the deepest sculk caves that walks on its knuckles, its vast hollow '
                                    'ribcage a cell with its heart of soul light hanging inside. It hunts by sound - footsteps, fighting, '
                                    'every note - so sneak. Reach you and it rears, spreads its ribs and crashes down over you. Its grip '
                                    'beats with its heart: at each thump the ribs glow and ease apart - only then do blows (or a struggle: '
                                    'sneak) bend them. It squeezes harder each time, and ribs left alone grow back. It kicks away rescuers; '
                                    'a friend can still break the ribs from outside. Break free and it guards you, regrows its ribs and '
                                    'strikes again: run! Drops Scukite.'),
        f'item.{NS}.sculkite': 'Scukite',
        f'codex.{NS}.sculkite.title': 'Scukite', f'codex.{NS}.sculkite.tagline': 'The dark crystal of the sculk',
        f'codex.{NS}.sculkite.body': ('A dark crystal of sculk, cold to the touch and humming at a pitch only the sculk can hear. Jailers '
                                      'grow the bars of their cells from it and leave a few shards behind; rarely it grows wild as '
                                      'Scukite Ore in the mud of the Sculk Swamp and in the Deep Sift\'s sculk - smelt the raw '
                                      'crystal. Smiths of the Sift work it into echo gear, ammunition, creature gear and the Vine '
                                      'Bola - never into armour for people, whom its hum drives to distraction.'),
        f'codex.{NS}.sculkling.title': 'Sculkling', f'codex.{NS}.sculkling.tagline': 'Hostile - giggling cave goblins',
        f'codex.{NS}.sculkling.body': 'Small blind sculk goblins with giant bat ears, skittering through dark caves in packs of three to five. They hear everything except a player who sneaks. Hear you, and they screech, swarm and scratch - and one may snatch something shiny from your pockets (gold, gems, ingots) and run, giggling. Kill the thief to get it back. Their ears cannot bear music: play a note and they cover them and flee.',
    })
    return L


# =========================================================================== spawn eggs (16 x 16)

def items():
    import items16 as I
    o = '#03141a'
    jailer = I.egg(['#061a21', '#0c2c35', '#15424d', '#1d5560'], o, {
        1: '......t..t......',
        2: '.....tt..tt.....',
        4: '......hhhh......',
        6: '....bbbbbbbb....',
        7: '....bsSssSsb....',
        8: '....bbSSSSbb....',
        10: '...i.i.i.i.i....',
        11: '...i.i.i.i.i....',
        12: '...iiiiiiiii....',
    }, pal={'t': (I.GLOW[2], o), 'h': (I.BONE[2], o), 'b': (I.BONE[3], o), 's': (I.GLOW[1], o), 'S': (I.GLOW[3], o),
            'i': ('#566872', o)}, no_ol='tsS')
    sculkling = I.egg(['#09252d', '#123b45', '#1d5661', '#25616a'], o, {
        1: '..e..........e..',
        2: '..ee........ee..',
        3: '..eVe......eVe..',
        4: '...eVe....eVe...',
        5: '....ee....ee....',
        7: '.....p....p.....',
        9: '.....mtmtmt.....',
        11: '.......gg.......',
        12: '.......gg.......',
    }, pal={'e': ('#0e3038', o), 'V': (I.GLOW[2], o), 'p': ('#020608', o), 'm': ('#04131a', o), 't': ('#efe8d2', o),
            'g': (I.GLOW[3], o)}, no_ol='Vg')
    out = {'jailer_spawn_egg': jailer, 'sculkling_spawn_egg': sculkling, 'sculkite': sculkite()}
    out.update(__import__('cypole').items())  # CR4: the Cypole's spawn egg
    return out


def sculkite():
    """Sculkite: a cluster of dark sculk crystal - deep teal-black facets, edges lit cyan by the glow
    trapped inside, one bright glint. Light from the top left like every vanilla item."""
    import items16 as I
    rows = [
        '................',
        '.........G......',
        '........gH2.....',
        '.......gH331....',
        '...g..gH33321...',
        '..gHg.H333221...',
        '..H32gH332211...',
        '..H332H322211.g.',
        '...3322H2211.gHg',
        '...3332H2111gH32',
        '....33222211H322',
        '....322211111321',
        '.....2211111.21.',
        '......11111.....',
        '................',
        '................',
    ]
    o = '#030b10'
    pal = I.ramp('123', ['#0a1c26', '#123546', '#1d5466'], o)
    pal.update({'H': ('#2fb8b8', o), 'g': (I.GLOW[2], o), 'G': (I.GLOW[4], o)})
    return I.grid(rows, pal, ol=True, no_ol='G')
