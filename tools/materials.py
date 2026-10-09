"""F1 Materials & progression, and the Europhy Table.

Progression (SPEC 3): vanilla Copper (no Sift copper ore) -> Prism, the early/mid crystal (iron pickaxe,
common in Sift caves) -> Siftite, the advanced metal: Siftite Ore (diamond pickaxe) drops Siftite Dust, and
Siftite Dust + a Copper Ingot become a Siftite Ingot only in the Europhy Table. Beside them:
* Scukite (`thesift:sculkite`, shown as "Scukite"): Jailer drops, and a rare ore that grows in sculk
  (Sculk Swamp mud, Deep Sift sculk) -> Raw Scukite -> smelted Scukite; Block of Scukite.
* Bauxite: an unstable ore - mining it explodes unless the miner or the ore is in Chrome. Bauxite is a very
  strong furnace fuel; Stable Bauxite (stronger) is made in the Europhy Table from Bauxite + Nib Dust while a
  living creature of `#thesift:bauxite_stabilizers` (the Nib; the Glow Fly joins later) is close by.
* Magnesite: a pale, stone-like rock (the Europhy Table's base; Crunchers will eat it later).
* Galena: a lead-grey ore kept for later.
* Soul materials: 9 Soul Dust = 1 Soul Piece, 9 Pieces = 1 Soul Chunk, 9 Chunks = 1 Pure Soul Block (and back).

The Europhy Table (`thesift:europhy_table`) is a clockwork music machine: a Magnesite plinth, a Copper ring
gear and pinions, a rotating crown of two Copper and two Prism arms holding the ingredients, and a floating
Prism lens. Play notes nearby (any melody - repeated notes do not count) or a whole song and the output
forms in the centre. Its recipes live in Java (block/entity/EurophyRecipes.java); this module makes the art
(the moving parts are a modelkit model drawn by EurophyTableRenderer, the plinth a block model), the GUI
texture, ores and worldgen, recipes, loot, tags, fuel values and text.

Hooks (one line each): spec.py -> declare(block, item); mobs.py -> ALL.update(MODELS);
gen_assets.gen_block (model 'europhy') -> gen_block(GA, bid); gen_assets.generate() -> assets(GA);
gen_world.generate() -> world(GW) (before biomes(), so the ores join COMMON_UNDERGROUND);
gen_textures.main() -> textures(out) (before vanilla_remap). Java: registry/ModEurophy, block/EurophyTableBlock,
block/BauxiteOreBlock, block/entity/EurophyTable*, client/EurophyClient, client/renderer/EurophyTableRenderer,
client/model/EurophyTableModel, client/gui/EurophyTableScreen.
"""
from __future__ import annotations

import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw

from modelkit import Model

NS = 'thesift'
MC_TEX = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')

STONE = "BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)"
DEEP = "BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)"


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# =========================================================================== spec (blocks and items)

def declare(block, item):
    # Scukite: the Jailer's crystal also grows, rarely, through sculk (the item itself is declared in cave_creatures.py)
    block("sculkite_ore", "ore", "BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.COLOR_CYAN).sound(SoundType.SCULK)"
          ".strength(4.0F, 6.0F).requiresCorrectToolForDrops().lightLevel(s -> 3)",
          tags=["pickaxe", "needs_iron", "sculkite_ores"], loot="ore:raw_sculkite", name="Scukite Ore")
    block("sculkite_block", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_CYAN)"
          ".strength(5.0F, 6.0F).requiresCorrectToolForDrops().lightLevel(s -> 5)",
          tags=["pickaxe", "needs_iron", "beacon"], name="Block of Scukite")
    item("raw_sculkite", props="new Item.Properties().rarity(Rarity.UNCOMMON)", name="Raw Scukite")
    # Bauxite: explodes when mined outside Chrome; a very strong fuel
    for bid, props in (("bauxite_ore", STONE), ("deep_bauxite_ore", DEEP)):
        block(bid, "custom", props + ".mapColor(MapColor.TERRACOTTA_ORANGE).strength(3.0F, 3.0F).requiresCorrectToolForDrops()",
              cls="BauxiteOreBlock", model="cube_all", tags=["pickaxe", "needs_iron", "bauxite_ores"], loot="ore:bauxite")
    item("bauxite", props="new Item.Properties().cookingFuel(ModEurophy.BAUXITE_BURN_TIME)")
    item("stable_bauxite", props="new Item.Properties().rarity(Rarity.UNCOMMON).cookingFuel(ModEurophy.STABLE_BAUXITE_BURN_TIME)")
    # Magnesite: pale stone-like rock
    block("magnesite", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.TERRACOTTA_WHITE).strength(1.5F, 6.0F)"
          ".requiresCorrectToolForDrops()", tags=["pickaxe", "sift_stone"])
    # Galena: lead-grey cubes, a mystery for later
    for bid, props in (("galena_ore", STONE), ("deep_galena_ore", DEEP)):
        block(bid, "ore", props + ".mapColor(MapColor.COLOR_GRAY).strength(3.0F, 3.0F).requiresCorrectToolForDrops()",
              tags=["pickaxe", "needs_iron", "galena_ores"], loot="ore:galena")
    item("galena")
    # Soul materials (9:1 each way)
    item("soul_dust")
    item("soul_piece", props="new Item.Properties().rarity(Rarity.UNCOMMON)")
    item("soul_chunk", props="new Item.Properties().rarity(Rarity.RARE)")
    block("pure_soul_block", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN).mapColor(MapColor.COLOR_BLUE).strength(2.0F, 6.0F)"
          ".lightLevel(s -> 12)", tags=["pickaxe"])
    # Siftite Dust, the Siftite Ore's drop (Siftite Ingots are made from it in the Europhy Table)
    item("siftite_dust", props="new Item.Properties().rarity(Rarity.UNCOMMON)")
    # the Europhy Table
    block("europhy_table", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.TERRACOTTA_WHITE).strength(3.0F, 6.0F)"
          ".requiresCorrectToolForDrops().lightLevel(s -> 6).noOcclusion()",
          cls="EurophyTableBlock", model="europhy", tags=["pickaxe"], tab="functional", name="Europhy Table")


# =========================================================================== the moving parts (modelkit)
# Model units: 1/16 block, Y down, the block's floor at y = 24 (EurophyTableRenderer draws it like a mob:
# translate(0.5, 1.5, 0.5), scale(-1, -1, 1)). The static plinth (block model below) is 0..11 px tall:
# foot 0-3, column 3-9 (10 wide), table top 9-11 (14 wide).
PAL = {
    'copper': '#c4693b', 'copper_l': '#e8945f', 'copper_d': '#87401f',
    'verdigris': '#5cb89c', 'pin': '#f2cf86', 'pin_l': '#fff0c2', 'pin_d': '#b58a3c',
    'prism': '#b99cf2', 'prism_l': '#efe3ff', 'prism_d': '#7b5cc6', 'prism_g': '#ffd9f7',
    'rose': '#ff9fd8', 'aqua': '#8ff0ff', 'sun': '#ffe28c',
}
MATS = {'copper': 'metal', 'verdigris': 'metal', 'pin': 'metal', 'prism': 'crystal', 'rose': 'crystal', 'aqua': 'crystal', 'sun': 'crystal'}
RING_Y = 18.5      # centre of the ring gear band (4..7 px up)
CROWN_Y = 13.0     # top of the table (11 px up)
ARM_HINGE = 2.2    # where the arms hinge on the hub
PAN_AT = 3.6       # pan centre, from the hinge (so 5.8 px from the middle)
LENS_Y = 3.0       # the floating Prism lens (21 px up)


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def _prism_face(w, h, phase):
    """A rainbow facet map (hd texels): diagonal bands of rose, aqua and sun through the lavender crystal."""
    keys = 'rAsP'
    rows = []
    for y in range(h):
        row = ''
        for x in range(w):
            d = (x + y + phase) % 9
            row += keys[0] if d == 0 else keys[1] if d == 3 else keys[2] if d == 6 else ('P' if (x + 2 * y) % 7 == 0 else '.')
        rows.append(row)
    return rows


PK = {'r': 'rose', 'A': 'aqua', 's': 'sun', 'P': 'prism_l', 'g': 'prism_g', 'c': 'copper_d', 'v': 'verdigris'}


def europhy_table() -> Model:
    m = Model('europhy_table', (64, 64), PAL, {'europhy_table': {}}, res=2, materials=MATS)
    crystal = lambda col='prism', ph=0, glow=False: dict(color=col, pattern='crystal', phase=ph, glow=glow, faces={  # noqa: E731
        f: dict(color=col, pattern='crystal', phase=ph + i, hd=True, map=_prism_face(8, 8, ph + i * 2), keys=PK, glow_keys='rAsg' if glow else '')
        for i, f in enumerate(('north', 'south', 'east', 'west'))})
    copper = lambda **kw: mc('copper', clusters=0.55, accent='verdigris', spots=kw.pop('spots', 0.12), **kw)  # noqa: E731

    # ---- the ring gear round the column: twelve tangent segments, a tooth on each, a Prism stud on every third
    ring = m.part('ring', pivot=(0, RING_Y, 0))
    for i in range(12):
        seg = ring.part(f'ring_{i}', rot=(0, i * math.pi / 6, 0))
        seg.cube((-1.85, -1.5, 5.3), (3.7, 3, 1.5), **copper(faces={
            'south': mc('copper', clusters=0.3, hd=True, map=['c' * 7, '.' * 7, '.' * 7, '.' * 7, '.' * 7, 'c' * 7], keys=PK)}))
        seg.cube((-0.7, -1.1, 6.8), (1.4, 2.2, 0.9), **mc('copper_l', clusters=0.3, rim=False))
        if i % 3 == 0:
            seg.cube((-0.6, -2.1, 5.65), (1.2, 0.6, 0.8), **mc('prism_g', clusters=0.0, rim=False, glow=True))

    # ---- two pinions on the plinth's corners, meshing with the ring (they turn the other way, faster)
    for j, (px, pz) in enumerate(((6.2, 6.2), (-6.2, -6.2))):
        pin = m.part(f'pinion_{j}', pivot=(px, RING_Y, pz))
        pin.cube((-0.35, -2.0, -0.35), (0.7, 4.5, 0.7), **mc('copper_d', clusters=0.2, rim=False))
        pin.cube((-0.9, -1.25, -0.9), (1.8, 2.5, 1.8), **mc('pin', clusters=0.3))
        pin.cube((-1.7, -0.75, -0.45), (3.4, 1.5, 0.9), **copper(spots=0.0))
        pin.cube((-0.45, -0.75, -1.7), (0.9, 1.5, 3.4), **copper(spots=0.0))
        star = pin.part(f'pinion_{j}_star', rot=(0, math.pi / 4, 0))
        star.cube((-1.6, -0.6, -0.4), (3.2, 1.2, 0.8), **mc('copper_l', clusters=0.2, rim=False))
        star.cube((-0.4, -0.6, -1.6), (0.8, 1.2, 3.2), **mc('copper_l', clusters=0.2, rim=False))

    # ---- the crown: a music-box hub with brass pins, two Copper arms (north/south) and two Prism arms (east/west)
    crown = m.part('crown', pivot=(0, CROWN_Y, 0))
    crown.cube((-2.5, -2.0, -2.5), (5, 2, 5), **copper(faces={
        f: mc('copper', clusters=0.3, hd=True, map=['..........', '.c..c..c..', '..........', '..c..c..c.'], keys=PK) for f in ('north', 'south', 'east', 'west')}))
    crown.cube((-1.6, -2.9, -1.6), (3.2, 0.9, 3.2), **mc('copper_l', clusters=0.3))
    crown.cube((-0.7, -3.5, -0.7), (1.4, 0.6, 1.4), **mc('prism_g', clusters=0.0, rim=False, glow=True))
    for k in range(8):  # the music-box pins round the hub
        a = k * math.pi / 4 + 0.2
        pinp = crown.part(f'hub_pin_{k}', pivot=(math.sin(a) * 2.5, -1.0 - (k % 2) * 0.7, -math.cos(a) * 2.5), rot=(0, -a, 0))
        pinp.cube((-0.3, -0.3, -0.7), (0.6, 0.6, 0.7), **mc('pin', clusters=0.0, rim=False))
    for k in range(4):
        prism_arm = k % 2 == 1
        arm = crown.part(f'arm_{k}', pivot=(0, -1.0, 0), rot=(0, k * math.pi / 2, 0))
        lift = arm.part(f'lift_{k}', pivot=(0, 0, -ARM_HINGE))
        lift.cube((-1.0, -1.0, -0.9), (2.0, 2.0, 1.4), **mc('copper_d' if prism_arm else 'copper', clusters=0.3))  # the hinge knuckle
        if prism_arm:
            lift.cube((-0.6, -0.6, -3.0), (1.2, 1.2, 2.4), **crystal('prism', k))
        else:
            lift.cube((-0.7, -0.55, -3.0), (1.4, 1.1, 2.4), **copper())
        pan = lift.part(f'pan_{k}', pivot=(0, 0.1, -PAN_AT))
        pan.cube((-1.9, -0.3, -1.9), (3.8, 0.7, 3.8), **(mc('prism_d', clusters=0.2) if prism_arm else copper(spots=0.2)))
        rim = mc('prism_l' if prism_arm else 'copper_l', clusters=0.15, rim=False, glow=prism_arm)
        pan.cube((-1.9, -0.9, -1.9), (3.8, 0.6, 0.5), **rim)
        pan.cube((-1.9, -0.9, 1.4), (3.8, 0.6, 0.5), **rim)
        pan.cube((-1.9, -0.9, -1.4), (0.5, 0.6, 2.8), **rim)
        pan.cube((1.4, -0.9, -1.4), (0.5, 0.6, 2.8), **rim)

    # ---- the floating Prism lens in its copper clamp, pointing down at the forming output
    lens = m.part('lens', pivot=(0, LENS_Y, 0))
    core = lens.part('lens_core', rot=(0, math.pi / 4, 0))
    core.cube((-1.8, -2.5, -1.8), (3.6, 5.0, 3.6), **crystal('prism', 1, glow=True))
    lens.cube((-1.2, -3.6, -1.2), (2.4, 1.1, 2.4), **mc('prism_l', clusters=0.2, glow=True))
    lens.cube((-0.9, 2.5, -0.9), (1.8, 1.4, 1.8), **crystal('prism', 4, glow=True))
    lens.cube((-0.45, 3.9, -0.45), (0.9, 1.0, 0.9), **mc('prism_g', clusters=0.0, rim=False, glow=True))
    clamp = lens.part('clamp', pivot=(0, 0, 0))
    band = copper(spots=0.0)
    clamp.cube((-2.7, -0.5, -2.7), (5.4, 1.0, 0.6), **band)
    clamp.cube((-2.7, -0.5, 2.1), (5.4, 1.0, 0.6), **band)
    clamp.cube((-2.7, -0.5, -2.1), (0.6, 1.0, 4.2), **band)
    clamp.cube((2.1, -0.5, -2.1), (0.6, 1.0, 4.2), **band)
    for cx, cz in ((-2.4, -2.4), (2.4, -2.4), (-2.4, 2.4), (2.4, 2.4)):
        clamp.cube((cx - 0.3, 0.5, cz - 0.3), (0.6, 1.6, 0.6), **mc('copper_d', clusters=0.0, rim=False))
        clamp.cube((cx - 0.4, -1.1, cz - 0.4), (0.8, 0.6, 0.8), **mc('pin', clusters=0.0, rim=False))
    return m


MODELS = {'europhy_table': europhy_table}


# =========================================================================== block model (the static plinth)

def gen_block(GA, bid):
    t = lambda n: f'{NS}:block/{bid}_{n}'  # noqa: E731
    tex = {'particle': t('side'), 'side': t('side'), 'top': t('top'), 'base': t('base'), 'bottom': t('bottom'), 'column': t('column'),
           'copper': t('copper'), 'prism': t('prism')}

    def box(a, b, side, top, bottom, uv_side=None, uv_top=None, **kw):
        f = {d: {'texture': side, **({'uv': uv_side} if uv_side else {})} for d in ('north', 'south', 'east', 'west')}
        f['up'] = {'texture': top, **({'uv': uv_top} if uv_top else {})}
        f['down'] = {'texture': bottom}
        e = {'from': a, 'to': b, 'faces': f}
        e.update(kw)
        return e

    plinth = [
        box([0, 0, 0], [16, 3, 16], '#side', '#base', '#bottom', uv_side=[0, 13, 16, 16]),
        box([3, 3, 3], [13, 9, 13], '#column', '#bottom', '#bottom', uv_side=[3, 5, 13, 11]),
        box([1, 9, 1], [15, 11, 15], '#side', '#top', '#bottom', uv_side=[1, 0, 15, 2], uv_top=[1, 1, 15, 15]),
    ]
    GA.write(os.path.join(GA.A, 'models/block', bid + '.json'), {'parent': 'minecraft:block/block', 'textures': tex, 'elements': plinth})
    # the item: the plinth plus a still crown and lens (in the world the block entity draws those, moving)
    crown = [box([6, 11, 6], [10, 13, 10], '#copper', '#copper', '#copper')]
    for (a, b, mat) in (([7.25, 11.5, 2.0], [8.75, 12.5, 6], '#copper'), ([7.25, 11.5, 10], [8.75, 12.5, 14], '#copper'),
                        ([2.0, 11.5, 7.4], [6, 12.6, 8.6], '#prism'), ([10, 11.5, 7.4], [14, 12.6, 8.6], '#prism')):
        crown.append(box(a, b, mat, mat, mat))
    for (cx, cz, mat) in ((8, 2.2, '#copper'), (8, 13.8, '#copper'), (2.2, 8, '#prism'), (13.8, 8, '#prism')):
        crown.append(box([cx - 1.9, 11.4, cz - 1.9], [cx + 1.9, 12.2, cz + 1.9], mat, mat, mat))
    crown.append(box([6.6, 13.6, 6.6], [9.4, 16, 9.4], '#prism', '#prism', '#prism',
                     rotation={'origin': [8, 14.8, 8], 'axis': 'y', 'angle': 45}, shade=False))
    GA.write(os.path.join(GA.A, 'models/block', bid + '_item.json'), {'parent': 'minecraft:block/block', 'textures': tex,
                                                                     'elements': plinth + crown})
    for v in tex.values():
        GA.TEXTURES.add(v.split(':', 1)[1])
    GA.simple_state(bid)
    GA.item_block(bid, bid + '_item')


# =========================================================================== data: recipes, loot, tags, fuel, sounds, text

SOUNDS = {
    'block.europhy_table.charge': [('block/beacon/activate', 1.0, 1.4), ('block/amethyst/resonate1', 1.0, 1.2)],
    'block.europhy_table.craft': [('block/enchantment_table/enchant1', 1.0, 0.8), ('block/beacon/power1', 1.0, 1.5)],
    'block.europhy_table.hum': [('block/beacon/ambient', 0.8, 1.2), ('block/amethyst/resonate2', 0.6, 1.3)],
    'block.europhy_table.note': [('block/amethyst/resonate1', 0.7, 1.6), ('block/note_block/chime', 0.5, 1.2)],
    'block.europhy_table.fizzle': [('random/fizz', 0.6, 1.4)],
    'block.bauxite.hiss': [('random/fuse', 0.9, 1.4)],
}
SUBTITLES = {
    'block.europhy_table.charge': 'Europhy Table charges', 'block.europhy_table.craft': 'Europhy Table forms something',
    'block.europhy_table.hum': 'Europhy Table hums', 'block.europhy_table.note': 'Europhy Table drinks a note',
    'block.europhy_table.fizzle': 'Europhy Table fizzles', 'block.bauxite.hiss': 'Bauxite hisses',
}


def _vanilla_burn(left):
    """A cooking-time provider like vanilla's coal (the fast-cooking blocks burn it twice as fast)."""
    return {'type': 'minecraft:div', 'left': left, 'right': {
        'type': 'minecraft:conditional', 'condition': 'minecraft:block/fast_cooking',
        'on_false': 'minecraft:cooking/normal_burn_time_reduction_factor', 'on_true': 'minecraft:cooking/fast_burn_time_reduction_factor'}}


def assets(GA):
    """Runs at the top of gen_assets.generate(): tag kinds first (the block loop needs them), then data."""
    for t in ('siftite_ores', 'bauxite_ores', 'galena_ores', 'sculkite_ores'):
        GA.TAG_MAP[t] = [('block', f'{NS}:{t}'), ('item', f'{NS}:{t}')]
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    D = os.path.join(GA.RES, 'data', NS)
    GA.write(os.path.join(D, 'context_int_provider', 'cooking', 'time_bauxite.json'), _vanilla_burn(12000))
    GA.write(os.path.join(D, 'context_int_provider', 'cooking', 'time_stable_bauxite.json'), _vanilla_burn(24000))
    # Siftite Ore: a handful of dust like copper ore gives raw copper (fortune adds more)
    GA.loot('siftite_ore', 'copper_ore', {'copper_ore': 'siftite_ore', 'raw_copper': 'siftite_dust'})
    GA.loot('deep_siftite_ore', 'copper_ore', {'copper_ore': 'deep_siftite_ore', 'raw_copper': 'siftite_dust'})
    sh, sl, sm = GA.shaped, GA.shapeless, GA.smelt
    # the Europhy Table: Magnesite, Copper and Prism
    sh('europhy_table', [' P ', 'CPC', 'MMM'], {'P': 'prism_gem', 'C': 'minecraft:copper_ingot', 'M': 'magnesite'}, 'europhy_table', 1, 'misc')
    # Scukite
    sm('sculkite', 'raw_sculkite', 'sculkite', 1.0, 200, ('smelting', 'blasting'))
    sm('sculkite_ore', '#sculkite_ores', 'sculkite', 1.0, 200, ('smelting', 'blasting'))
    sh('sculkite_block', ['###', '###', '###'], {'#': 'sculkite'}, 'sculkite_block')
    sl('sculkite_from_block', ['sculkite_block'], 'sculkite', 9)
    # Galena
    sm('galena', '#galena_ores', 'galena', 0.7, 200, ('smelting', 'blasting'))
    # Soul: 9 to 1 and back, all the way up
    for small, big in (('soul_dust', 'soul_piece'), ('soul_piece', 'soul_chunk'), ('soul_chunk', 'pure_soul_block')):
        sh(big, ['###', '###', '###'], {'#': small}, big, 1, 'misc')
        sl(f'{small}_from_{big}', [big], small, 9)
    # tags
    GA.tag('entity_type', f'{NS}:bauxite_stabilizers', rl('nib'))  # the Glow Fly joins this tag when it arrives
    for i in ('siftite_dust', 'raw_sculkite', 'galena', 'soul_chunk'):
        GA.tag('item', f'{NS}:sculkling_shinies', rl(i))
    GA.tag('item', 'c:gems', rl('sculkite'))
    GA.tag('item', 'c:dusts', rl('siftite_dust'))
    GA.tag('item', 'c:dusts', rl('soul_dust'))
    GA.tag('item', 'minecraft:beacon_payment_items', rl('sculkite'))
    GA.tag('block', 'minecraft:sculk_replaceable', rl('magnesite'))
    for b in ('siftite_ore', 'deep_siftite_ore', 'galena_ore', 'deep_galena_ore', 'bauxite_ore', 'deep_bauxite_ore', 'sculkite_ore', 'prism_ore',
              'deep_prism_ore'):
        GA.tag('block', 'c:ores', rl(b))
        GA.tag('item', 'c:ores', rl(b))
    GA.tag('block', f'{NS}:caravan_minable', rl('galena_ore'))
    GA.tag('block', f'{NS}:caravan_minable', rl('deep_galena_ore'))
    GA.LANG.update(lang())


def lang():
    c = f'codex.{NS}'
    return {
        f'container.{NS}.europhy_table': 'Europhy Table',
        f'europhy.{NS}.status.idle': 'Set ingredients',
        f'europhy.{NS}.status.ready': 'Play a tune!',
        f'europhy.{NS}.status.helper': 'Needs a Nib',
        f'europhy.{NS}.status.full': 'Output full',
        f'europhy.{NS}.status.forming': 'Forming...',
        f'europhy.{NS}.notes': '%s / %s notes',
        f'message.{NS}.europhy.needs_helper': 'The Bauxite will not settle: a living Nib (or Glow Fly) must be close by.',
        f'message.{NS}.europhy.output_full': 'The Europhy Table has nowhere to put what it makes - empty its centre.',
        f'message.{NS}.bauxite.unstable': 'The Bauxite was unstable! Mine it in Chrome.',
        f'{c}.europhy_table.title': 'Europhy Table', f'{c}.europhy_table.tagline': 'A machine that crafts to music',
        f'{c}.europhy_table.body': ('A clockwork of Copper and Prism on a Magnesite plinth. Set the ingredients on its arms, then '
                                    'play music close by: every new note (repeats do not count) winds it up, a whole song fills it '
                                    'at once. The gears race, the arms lift and the output forms in the centre. It makes Siftite '
                                    'Ingots (4 Siftite Dust and a Copper Ingot) and Stable Bauxite (Bauxite and Nib Dust, with a '
                                    'living Nib near), and enchants an item laid beside a Chrome Pearl - the more varied the tune, '
                                    'the stronger. All near it feel Euphoria.'),
        f'{c}.bauxite.title': 'Bauxite', f'{c}.bauxite.tagline': 'Unstable, and a superb fuel',
        f'{c}.bauxite.body': ('Rust-red ore of the Sift caves that bursts when it is broken open in the air. Mine it while you or the '
                              'ore are in Chrome and it comes out whole. Bauxite burns in a furnace for 60 items. Settle it in the '
                              'Europhy Table with Nib Dust - a living Nib must be close, its calm keeps the Bauxite still - and '
                              'Stable Bauxite burns for twice as long.'),
        f'{c}.magnesite.title': 'Magnesite', f'{c}.magnesite.tagline': 'Pale stone of the deep',
        f'{c}.magnesite.body': ('A soft, chalky white rock found in wide seams through the Sift caves. It carries sound without '
                                'swallowing it, which is why the Europhy Table stands on it. Crunchers are said to chew it.'),
        f'{c}.galena.title': 'Galena', f'{c}.galena.tagline': 'Heavy, grey, unexplained',
        f'{c}.galena.body': ('Lead-grey cubes that grow in perfect little steps inside the rock. Galena is cold and much heavier than '
                             'it looks. Nobody in the Sift has found a use for it yet - but someone hoarded it once.'),
        f'{c}.soul_materials.title': 'Soul Materials', f'{c}.soul_materials.tagline': 'Dust, Pieces, Chunks',
        f'{c}.soul_materials.body': ('What the soul stuff of the old days settles into. Nine Soul Dust press into a Soul Piece, nine '
                                     'Pieces into a Soul Chunk and nine Chunks into a glowing Pure Soul Block - and each breaks '
                                     'back down into nine. Soul Golems sometimes dig up the dust.'),
    }


# =========================================================================== worldgen

def world(GW):
    st, feature, placed, count, rarity, BIOME = GW.state, GW.feature, GW.placed, GW.count, GW.rarity, GW.BIOME
    sq = {'type': 'minecraft:in_square'}

    def height(lo, hi, kind='uniform'):
        return {'type': 'minecraft:height_range', 'height': {'type': f'minecraft:{kind}', 'max_inclusive': {'absolute': hi},
                                                            'min_inclusive': {'absolute': lo}}}

    def ore(name, size, pairs, air=0.0):
        return feature(name, {'type': 'minecraft:ore', 'discard_chance_on_air_exposure': air, 'size': size, 'targets': [
            {'state': st(s), 'target': {'predicate_type': 'minecraft:block_match', 'block': GW.rl(b)}} for b, s in pairs]})

    # Bauxite: rust-red seams through the upper caves
    ore('ore_bauxite', 7, [('dreamstone', 'bauxite_ore'), ('hushslate', 'deep_bauxite_ore')], air=0.1)
    placed('ore_bauxite', 'ore_bauxite', [count(3), sq, height(-40, 72), BIOME])
    # Galena: smaller pockets, mostly deep
    ore('ore_galena', 6, [('dreamstone', 'galena_ore'), ('hushslate', 'deep_galena_ore')], air=0.2)
    placed('ore_galena', 'ore_galena', [count(3), sq, height(-64, 40, 'trapezoid'), BIOME])
    # Magnesite: wide pale seams in the stone (like tuff or calcite)
    ore('ore_magnesite', 30, [('dreamstone', 'magnesite'), ('hushslate', 'magnesite')])
    placed('ore_magnesite', 'ore_magnesite', [count(2), sq, height(-48, 80), BIOME])
    # Scukite: crystals in sculk only - the Sculk Swamp's mud floor and the Deep Sift's sculk patches. Step 8,
    # after the sculk is laid down (step 7), so the same feature order holds in every biome.
    ore('ore_sculkite_mud', 4, [('sculk_mud', 'sculkite_ore'), ('minecraft:sculk', 'sculkite_ore')])
    placed('ore_sculkite_mud', 'ore_sculkite_mud', [rarity(2), sq, {'type': 'minecraft:heightmap', 'heightmap': 'OCEAN_FLOOR_WG'}, BIOME])
    ore('ore_sculkite_deep', 3, [('minecraft:sculk', 'sculkite_ore'), ('sculk_mud', 'sculkite_ore')])
    placed('ore_sculkite_deep', 'ore_sculkite_deep', [count(4), sq, height(-64, 48), BIOME])
    GW.COMMON_UNDERGROUND.extend([(6, 'ore_bauxite'), (6, 'ore_galena'), (6, 'ore_magnesite'), (8, 'ore_sculkite_mud'), (8, 'ore_sculkite_deep')])


# =========================================================================== textures

MAGNESITE = ['#a89889', '#bfb0a0', '#d3c7b8', '#e2d9cc', '#eee8de', '#f8f4ed']
COPPER = ['#4a2112', '#7a3a1f', '#a5512c', '#c96c3c', '#e48f5c', '#f6b989']
VERDI = ['#2f7563', '#4f9e86', '#74c4a8']
PRISM = ['#5a3f9c', '#7f63cf', '#a98ce9', '#cdb8fb', '#efe6ff']
RAINBOW = ['#ff9fd8', '#ffe28c', '#8ff0ff', '#b4f59a']
SIFT_RAMP = ['#1d1450', '#33287e', '#4552b6', '#4a8ed8', '#5cc2e6', '#a2ecf4', '#ffd2ee']
SOUL_RAMP = ['#08112e', '#11245e', '#1b4596', '#2a73c6', '#47abe8', '#8ae2ff', '#e4fbff']
SCULK_RAMP = ['#02090c', '#061c24', '#0b313b', '#114a55', '#1a6c76', '#36aeb4', '#a6fbf2']
RUST_RAMP = ['#2a100a', '#541f12', '#82331b', '#ad4b25', '#cf6d33', '#e89a52', '#f7cb88']
LEAD_RAMP = ['#15171e', '#272b36', '#3e4452', '#5b6272', '#7f8798', '#aab2c1', '#e6ebf3']


def _hx(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def _lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def retheme(name, ramp, folder='item', marks=None):
    """A vanilla sprite with its exact shape and value structure, each tone moved to the same rank of our ramp
    (dark outline -> darkest, Mojang's highlights -> lightest). marks: [(x, y, '#hex')] hand-placed details."""
    v = np.asarray(Image.open(os.path.join(MC_TEX, folder, name + '.png')).convert('RGBA')).astype(np.float64)
    v = v[:v.shape[1]]
    m = v[..., 3] > 0
    L = np.round(_lum(v), 1)
    vals = np.unique(L[m])
    cols = np.array([_hx(c)[:3] for c in ramp], dtype=np.float64)
    out = np.zeros_like(v)
    out[..., 3] = v[..., 3]
    for val in vals:
        t = 0.0 if len(vals) == 1 else float(np.searchsorted(vals, val)) / (len(vals) - 1)
        out[(L == val) & m, :3] = cols[int(round(t * (len(cols) - 1)))]
    for x, y, c in marks or []:
        out[y, x] = _hx(c)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), 'RGBA')


def item_sprites():
    s = {}
    # Siftite Dust: the redstone heap in Siftite's indigo-to-cyan, a blush of pink on the brightest grains
    s['siftite_dust'] = retheme('redstone', SIFT_RAMP, marks=[(6, 6, '#ffb8e6'), (10, 9, '#ffb8e6')])
    # Bauxite: a raw chunk in rust and ochre with hot orange specks; Stable Bauxite: a fired brick glazed with Nib Dust
    s['bauxite'] = retheme('raw_copper', RUST_RAMP, marks=[(6, 7, '#ffd36a'), (9, 9, '#ffb347'), (5, 10, '#ffb347')])
    s['stable_bauxite'] = retheme('resin_brick', RUST_RAMP, marks=[(5, 7, '#7ff0e0'), (6, 7, '#c8fff6'), (9, 8, '#7ff0e0'), (10, 8, '#7ff0e0')])
    # Galena: steel-grey stepped cubes
    s['galena'] = retheme('netherite_scrap', LEAD_RAMP)
    # Soul materials: dark blue with glowing cores
    s['soul_dust'] = retheme('glowstone_dust', SOUL_RAMP)
    s['soul_piece'] = retheme('echo_shard', SOUL_RAMP)
    s['soul_chunk'] = retheme('heart_of_the_sea', SOUL_RAMP)
    # Raw Scukite: a dark lump of sculk with crystal glints
    s['raw_sculkite'] = retheme('raw_gold', SCULK_RAMP)
    return s


def _stone(pal, seed, w=16, h=16, light=0.18, dark=0.18):
    """Calcite-like stone: soft blotches of the palette's middle tones with a few darker flecks."""
    rnd = random.Random(seed)
    cols = [_hx(c) for c in pal]
    a = np.zeros((h, w, 4), np.uint8)
    base = len(cols) // 2
    for y in range(h):
        for x in range(w):
            n = rnd.random()
            k = base + (1 if n < light else -1 if n > 1 - dark else 0)
            a[y, x] = cols[max(0, min(len(cols) - 1, k))]
    for _ in range(w * h // 18):
        x, y = rnd.randrange(w), rnd.randrange(h)
        a[y, x] = cols[max(0, base - 2)]
    return a


def _ramp_seed(pal, seed):
    """Seed for a vanilla_remap clone: every tone of the palette present, so the clone gets its full range."""
    rnd = random.Random(seed)
    cols = [_hx(c) for c in pal]
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            a[y, x] = cols[rnd.randrange(len(cols))]
    return Image.fromarray(a, 'RGBA')


def _ore_seed(base_pal, gem, seed):
    """Seed for vanilla_remap._ore: base stone palette with gem spots (only the spots' colours are used)."""
    a = _stone(base_pal, seed)
    rnd = random.Random(seed + 7)
    g = [_hx(c) for c in gem]
    for _ in range(5):
        x, y = rnd.randrange(2, 13), rnd.randrange(2, 13)
        for dx, dy, k in ((0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 0), (2, 0, 3)):
            a[y + dy, x + dx] = g[min(k, len(g) - 1)]
    return Image.fromarray(a, 'RGBA')


def _put(a, x, y, c):
    if 0 <= y < a.shape[0] and 0 <= x < a.shape[1]:
        a[y, x] = _hx(c)


def block_textures():
    o = {}
    dream = ['#9c9682', '#b1ab96', '#c5c0aa', '#d7d3bf', '#e6e3d2']
    hush = ['#152331', '#1c2e3f', '#243a4d', '#2e485c', '#3a586c']
    o['siftite_ore'] = _ore_seed(dream, ['#44388e', '#6ab0e8', '#9aeaf0', '#f29bd6'], 61)
    o['deep_siftite_ore'] = _ore_seed(hush, ['#44388e', '#6ab0e8', '#9aeaf0', '#f29bd6'], 62)
    o['bauxite_ore'] = _ore_seed(dream, ['#82331b', '#ad4b25', '#cf6d33', '#f0a35a'], 63)
    o['deep_bauxite_ore'] = _ore_seed(hush, ['#82331b', '#ad4b25', '#cf6d33', '#f0a35a'], 64)
    o['galena_ore'] = _ore_seed(dream, ['#2a2e3a', '#4a5162', '#7c8597', '#c9d0dc'], 65)
    o['deep_galena_ore'] = _ore_seed(hush, ['#2a2e3a', '#4a5162', '#7c8597', '#c9d0dc'], 66)
    o['sculkite_ore'] = _ore_seed(['#061c24', '#0b313b', '#114a55', '#1a6c76', '#23808a'], ['#0d3d4c', '#1a8c96', '#4ff0e8', '#c8fffb'], 67)
    o['magnesite'] = _ramp_seed(MAGNESITE, 68)
    o['sculkite_block'] = _ramp_seed(['#03141a', '#082a33', '#0f4450', '#166470', '#228a94', '#46c4c4', '#a6fbf2'], 69)
    o['pure_soul_block'] = _ramp_seed(['#0b1a48', '#132f78', '#1f50a8', '#3a80d6', '#62b8f2', '#a8e6ff', '#eafcff'], 70)
    # ---- the Europhy Table's plinth
    side = _stone(MAGNESITE, 71)
    for x in range(16):
        _put(side, x, 0, COPPER[4]); _put(side, x, 1, COPPER[2])        # the table top's copper trim (rows 0-1)
        _put(side, x, 13, COPPER[3])                                     # the foot's inlaid band (rows 13-15)
        _put(side, x, 15, MAGNESITE[1])
    for x in (2, 7, 12):                                                 # rivets in the trim
        _put(side, x, 1, COPPER[5])
    for x, y in ((3, 6), (4, 5), (4, 6), (4, 7), (4, 8), (5, 8), (10, 7), (11, 6), (11, 7), (11, 8), (11, 9), (12, 9)):
        _put(side, x, y, MAGNESITE[1])                                   # carved notes between the bands
    o['europhy_table_side'] = side
    col = _stone(MAGNESITE, 72)
    for y in range(16):
        for x in (4, 8, 12):
            _put(col, x, y, MAGNESITE[1])                                # fluting
        for x in (5, 9, 13):
            _put(col, x, y, MAGNESITE[4])
    for x in range(16):
        _put(col, x, 5, COPPER[4]); _put(col, x, 6, COPPER[2])           # copper collar at the top of the column
        _put(col, x, 10, COPPER[3])                                      # and at its foot
    for (x, y, k) in ((7, 7, 4), (8, 7, 3), (7, 8, 3), (8, 8, 2), (7, 9, 2)):
        _put(col, x, y, PRISM[k])                                        # a Prism gem set in each face
    _put(col, 6, 8, VERDI[1]); _put(col, 9, 9, VERDI[0])
    o['europhy_table_column'] = col
    top = _stone(MAGNESITE, 73)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5.0 < d < 6.0:
                _put(top, x, y, COPPER[3] if (x + y) % 3 else COPPER[4])  # the inlaid copper ring
            elif d < 2.6:
                _put(top, x, y, MAGNESITE[1] if d > 1.6 else MAGNESITE[0])  # the socket under the hub
    for (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        for dx, dy, k in ((0, 0, 4), (1, 0, 3), (0, 1, 2), (1, 1, 1)):
            _put(top, x + dx - 1, y + dy - 1, PRISM[k])                  # Prism inlays on the diagonals
    for i, (x, y) in enumerate(((7, 1), (14, 7), (8, 14), (1, 8))):
        _put(top, x, y, RAINBOW[i])                                      # tiny rainbow chips at the four arm stations
    o['europhy_table_top'] = top
    base = _stone(MAGNESITE, 74)
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            _put(base, x, y, COPPER[3])
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        _put(base, x, y, COPPER[5])
    o['europhy_table_base'] = base
    o['europhy_table_bottom'] = _stone(MAGNESITE, 75)
    cop = np.zeros((16, 16, 4), np.uint8)
    rnd = random.Random(76)
    for y in range(16):
        for x in range(16):
            n = rnd.random()
            cop[y, x] = _hx(COPPER[3] if n < 0.55 else COPPER[2] if n < 0.8 else COPPER[4])
    for _ in range(9):
        _put(cop, rnd.randrange(16), rnd.randrange(16), VERDI[rnd.randrange(3)])
    o['europhy_table_copper'] = cop
    pr = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            d = (x + y) % 9
            pr[y, x] = _hx(RAINBOW[(x + y) // 9 % 4] if d == 0 else PRISM[(x * 3 + y) % 3 + 2 if d > 4 else 1 + (x + 2 * y) % 2])
    o['europhy_table_prism'] = pr
    return {k: (v if isinstance(v, Image.Image) else Image.fromarray(v, 'RGBA')) for k, v in o.items()}


REMAP = {
    'siftite_ore': ('redstone_ore', 'stone', 'dreamstone'), 'deep_siftite_ore': ('deepslate_redstone_ore', 'deepslate', 'hushslate'),
    'bauxite_ore': ('copper_ore', 'stone', 'dreamstone'), 'deep_bauxite_ore': ('deepslate_copper_ore', 'deepslate', 'hushslate'),
    'galena_ore': ('iron_ore', 'stone', 'dreamstone'), 'deep_galena_ore': ('deepslate_iron_ore', 'deepslate', 'hushslate'),
    'sculkite_ore': ('deepslate_diamond_ore', 'deepslate', 'sculk_mud'),
}


def gui_texture():
    """The Europhy Table's screen (176 x 186 on a 256 x 256 sheet): a cream Magnesite panel with a copper trim,
    the dais - a copper ring with two Copper and two Prism arms leading to the four ingredient slots and the
    large centre slot - a parchment score on the right (the screen fills its note heads as you play), and the
    standard inventory below."""
    W, H = 176, 186
    im = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    out_ol, fill, hi, lo = '#2b2118', '#ddd3c2', '#fffaf0', '#8c7e69'
    # panel with the vanilla bevel and rounded corners
    d.rectangle((1, 1, W - 2, H - 2), fill=fill)
    d.line((1, 0, W - 3, 0), fill=out_ol); d.line((1, H - 1, W - 3, H - 1), fill=out_ol)
    d.line((0, 1, 0, H - 3), fill=out_ol); d.line((W - 1, 2, W - 1, H - 2), fill=out_ol)
    d.point((W - 2, 1), fill=out_ol); d.point((1, H - 2), fill=out_ol)
    d.line((1, 1, W - 4, 1), fill=hi); d.line((1, 1, 1, H - 4), fill=hi); d.line((2, 2, 3, 2), fill=hi); d.line((2, 2, 2, 3), fill=hi)
    d.line((3, H - 2, W - 2, H - 2), fill=lo); d.line((W - 2, 3, W - 2, H - 2), fill=lo); d.point((W - 3, H - 3), fill=lo)
    # stone speckle on the panel
    rnd = random.Random(5)
    px = im.load()
    for _ in range(900):
        x, y = rnd.randrange(4, W - 4), rnd.randrange(4, H - 4)
        if px[x, y][:3] == _hx(fill)[:3]:
            px[x, y] = _hx('#d3c8b6' if rnd.random() < 0.6 else '#e7dfd1')
    # copper trim under the title
    for y0 in (17,):
        d.line((6, y0, W - 7, y0), fill=COPPER[4]); d.line((6, y0 + 1, W - 7, y0 + 1), fill=COPPER[2])
        for x in range(10, W - 8, 20):
            d.point((x, y0 + 1), fill=COPPER[5])
    # the dais round the centre slot
    cx, cy = OUTPUT_POS[0] + 8, OUTPUT_POS[1] + 8
    for r, c in ((27, COPPER[1]), (26, COPPER[3]), (25, COPPER[4]), (24, COPPER[2]), (23, '#cbbfad')):
        d.ellipse((cx - r, cy - r, cx + r, cy + r), outline=c)
    for a in range(0, 360, 15):  # gear teeth on the ring
        t = math.radians(a)
        for rr in (27.5, 28.5):
            d.point((round(cx + math.cos(t) * rr), round(cy + math.sin(t) * rr)), fill=COPPER[2])
    for k, (dx, dy) in enumerate(((0, -1), (1, 0), (0, 1), (-1, 0))):
        prism = k % 2 == 1
        c0, c1, c2 = (PRISM[1], PRISM[3], PRISM[4]) if prism else (COPPER[2], COPPER[4], COPPER[5])
        for s in range(12, 24):
            x, y = cx + dx * s, cy + dy * s
            if dx == 0:
                d.line((x - 1, y, x + 1, y), fill=c1); d.point((x - 2, y), fill=c0); d.point((x + 2, y), fill=c0); d.point((x, y), fill=c2)
            else:
                d.line((x, y - 1, x, y + 1), fill=c1); d.point((x, y - 2), fill=c0); d.point((x, y + 2), fill=c0); d.point((x, y), fill=c2)
    for k in range(4):  # Prism gems on the diagonals of the ring
        t = math.radians(45 + 90 * k)
        gx, gy = round(cx + math.cos(t) * 25), round(cy + math.sin(t) * 25)
        d.rectangle((gx - 1, gy - 1, gx + 1, gy + 1), fill=PRISM[2]); d.point((gx - 1, gy - 1), fill=PRISM[4]); d.point((gx + 1, gy + 1), fill=PRISM[0])

    def slot(x, y, big=False):
        """A vanilla slot frame (18 x 18, or 26 x 26 round the centre) with the item at (x, y)."""
        p = 5 if big else 1
        x0, y0, x1, y1 = x - p, y - p, x + 16 + p - 1, y + 16 + p - 1
        d.rectangle((x0, y0, x1, y1), fill='#8f826f')
        d.line((x0, y0, x1, y0), fill='#3f3427'); d.line((x0, y0, x0, y1), fill='#3f3427')
        d.line((x0 + 1, y1, x1, y1), fill='#fbf5ea'); d.line((x1, y0 + 1, x1, y1), fill='#fbf5ea')
        if big:
            for i, c in enumerate(RAINBOW):
                d.point((x0 + 2 + i, y0 + 2), fill=c); d.point((x1 - 2 - i, y1 - 2), fill=c)

    for (x, y) in SLOT_POS:
        slot(x, y)
    slot(*OUTPUT_POS, big=True)
    # the score: parchment with two staves and treble clefs
    sx0, sy0, sx1, sy1 = SCORE
    d.rectangle((sx0, sy0, sx1, sy1), fill='#efe4c8', outline='#9a8460')
    d.line((sx0 + 1, sy1 - 1, sx1 - 1, sy1 - 1), fill='#d8c79f'); d.line((sx1 - 1, sy0 + 1, sx1 - 1, sy1 - 1), fill='#d8c79f')
    for st in STAVES:
        for i in range(5):
            d.line((sx0 + 3, st + i * 3, sx1 - 3, st + i * 3), fill='#a8946c')
        clef = ['.#.', '#.#', '.##', '.#.', '##.', '#.#', '.#.', '.#.', '#..']
        for j, row in enumerate(clef):
            for i, ch in enumerate(row):
                if ch == '#':
                    d.point((sx0 + 4 + i, st - 2 + j), fill='#5b4a33')
    # a copper horn on the left, listening (the table hears music played close by)
    hx0, hy0 = 16, 38
    for i in range(14):
        r = 1 + i // 3
        d.line((hx0 + i, hy0 + 10 - r, hx0 + i, hy0 + 10 + r), fill=COPPER[3 if i % 4 else 2])
        d.point((hx0 + i, hy0 + 10 - r), fill=COPPER[5])
    d.line((hx0 + 14, hy0 + 4, hx0 + 14, hy0 + 16), fill=COPPER[1])
    for j, (nx, ny) in enumerate(((36, 30), (42, 40), (35, 52))):
        d.ellipse((nx, ny + 4, nx + 3, ny + 6), fill=PRISM[1 + j]); d.line((nx + 3, ny, nx + 3, ny + 5), fill=PRISM[1 + j])
    # the player's inventory
    for r in range(3):
        for c in range(9):
            slot(8 + c * 18, INV_Y + r * 18)
    for c in range(9):
        slot(8 + c * 18, INV_Y + 58)
    return im


# slot positions (item top-left, as the menu places them) - EurophyTableMenu uses the same numbers
SLOT_POS = [(80, 20), (108, 48), (80, 76), (52, 48)]
OUTPUT_POS = (80, 48)
SCORE = (130, 24, 170, 74)
STAVES = [32, 56]
INV_Y = 104


def textures(out):
    """Block seeds (vanilla_remap gives them Mojang's material afterwards), item sprites and the GUI."""
    import vanilla_remap as VR
    for name, img in block_textures().items():
        out(f'block/{name}', img)
    for name, (ore, base, base_old) in REMAP.items():
        VR.MAP[name] = VR.F(VR._ore, ore, base, base_old)
    VR.MAP.update({
        'magnesite': VR.C('calcite'),
        'sculkite_block': VR.C('diamond_block'),
        'pure_soul_block': VR.C('glowstone'),
        'europhy_table_side': VR.M('cut_copper', 'calcite'),
        'europhy_table_column': VR.M('cut_copper', 'calcite'),
        'europhy_table_top': VR.M('cut_copper', 'calcite'),
        'europhy_table_base': VR.M('cut_copper', 'calcite'),
        'europhy_table_bottom': VR.C('calcite'),
        'europhy_table_copper': VR.C('cut_copper'),
        'europhy_table_prism': VR.M('amethyst_block'),
    })
    for name, img in item_sprites().items():
        out(f'item/{name}', img)
    out('gui/container/europhy_table', gui_texture())
