"""Builds The Sift's exploration structures (NBT templates) and their worldgen JSON."""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_assets as GA  # noqa: E402
from structlib import AIR, B, Build, chest, sigil, stairs  # noqa: E402

NS = 'thesift'
OUT = os.path.join(GA.RES, 'data', NS, 'structure')
D = os.path.join(GA.RES, 'data', NS, 'worldgen')

# ------------------------------------------------------------------ palette
DS_BR = B('dreamstone_bricks')
DS_CR = B('cracked_dreamstone_bricks')
DS_POL = B('polished_dreamstone')
DS_TILE = B('dreamstone_tiles')
PILLAR = B('dreamstone_pillar', axis='y')
HUSH_BR = B('hushslate_bricks')
HUSH_CR = B('cracked_hushslate_bricks')
HUSH_TILE = B('hushslate_tiles')
HUSH_POL = B('polished_hushslate')
HUSH_CH = B('chiseled_hushslate')
LANTERN = B('bulb_lantern', hanging='false', waterlogged='false')
DRUM = B('sift_drum', hit='0', core='false', powered='false')
CRUMBLE = B('crumbling_dreamstone')
SENSOR = B('minecraft:sculk_sensor', power=0, sculk_sensor_phase='inactive', waterlogged='false')
SCULK = B('minecraft:sculk')


# W1 World & terrain: the towers, temples, wells, altars, stone instruments, bridges, buried settlements, statues,
# deep shrines, scattered ruins and the Echoer's Hut are gone. What they held now comes from buried relic caches
# (worldgen/RelicCacheFeature: archaeology), creature drops and the Caravan colony; the Weaver's Encore Sigil lies
# in the Sculk Swamp's weaver hollow (tools/sculk_world.py).


# ================================================================== the Dictator's Sculk Castle


def sculk_castle(seed):
    """A tall tower of hushslate and sculk. Inside, a spiral of jump-apart steps climbs the wall
    past checkpoint landings to a hatch in the roof; on the roof is the arena, ringed with spiked
    battlements, and the Conductor's Podium where the Dictator waits. A slime-block floor catches
    anyone who falls."""
    S, H = 27, 72
    c = S // 2
    R = 10                      # outer wall radius
    STEPS = 58
    TOP = 2 + STEPS             # roof / arena floor level
    b = Build(S, H, S, seed)
    DT = B('minecraft:deepslate_tiles')
    wall = b.mix((HUSH_BR, 9), (HUSH_CR, 3), (DT, 2), (SCULK, 2))
    SOUL_L = B('minecraft:soul_lantern', hanging='false', waterlogged='false')
    SLIME = B('minecraft:slime_block')
    # foundation plinth and a sculk-stained skirt
    b.disc(c, c, 0, 13, HUSH_TILE, pick=b.mix((HUSH_TILE, 8), (SCULK, 3), (DT, 2)))
    b.cyl(c, c, 1, 2, 12.4, HUSH_BR, hollow=True, thickness=1.0, pick=wall)
    # the tower
    b.cyl(c, c, 1, TOP, R, HUSH_BR, hollow=True, thickness=2.0, pick=wall)
    for y in range(6, TOP, 10):
        b.cyl(c, c, y, y, R + 0.4, HUSH_CH, hollow=True, thickness=1.0)
    # a soft landing: slime blocks across the ground floor around a sculk catalyst
    b.disc(c, c, 0, R - 2, SLIME)
    b.set(c, 0, c, B('minecraft:sculk_catalyst', bloom='false'))
    # the central pillar, veined with glowing moss
    b.cyl(c, c, 1, TOP - 1, 1.2, HUSH_POL, pick=lambda x, y, z: B('lumen_moss_block') if y % 7 == 3 else HUSH_POL)
    # entrance on the south side
    for dx in range(-1, 2):
        for y in range(1, 5):
            for z in range(c + R - 2, c + R + 1):
                b.set(c + dx, y, z, AIR)
    for dx in (-2, 2):
        for y in range(1, 6):
            b.set(c + dx, y, c + R, HUSH_CH)
    for dx in range(-2, 3):
        b.set(c + dx, 5, c + R, HUSH_CH)
    # window slits
    for k in range(8):
        a = k * math.pi / 4 + 0.2
        for y0 in range(10 + (k % 3) * 4, TOP - 4, 13):
            for dy in range(3):
                for rr in (R - 1, R):
                    b.set(c + round(math.cos(a) * rr), y0 + dy, c + round(math.sin(a) * rr), AIR)
    # the spiral of steps: 16 per turn, one block up each, ~1.7 blocks of air between them
    a0 = math.pi / 2 + 0.6
    last = []
    for i in range(STEPS):
        a = a0 + i * (2 * math.pi / 16)
        y = 1 + i
        landing = i > 0 and i % 16 == 0
        for rr in (6.6, 7.6):
            x, z = c + round(math.cos(a) * rr), c + round(math.sin(a) * rr)
            blk = HUSH_TILE
            if not landing and i % 7 == 5 and i > 8:
                blk = CRUMBLE
            b.set(x, y, z, blk)
        if landing:
            # a checkpoint: a wider ledge with a lantern, a sensor and a small supply chest
            for da in (-0.14, 0.0, 0.14):
                for rr in (5.6, 6.6, 7.6):
                    b.set(c + round(math.cos(a + da) * rr), y, c + round(math.sin(a + da) * rr), HUSH_POL)
            b.set(c + round(math.cos(a + 0.14) * 7.6), y + 1, c + round(math.sin(a + 0.14) * 7.6), SOUL_L)
            b.set(c + round(math.cos(a - 0.14) * 7.6), y + 1, c + round(math.sin(a - 0.14) * 7.6), chest('chests/sculk_castle_landing', 'north'))
        if i >= STEPS - 3:
            last.append(a)
    # the roof: the arena floor, with a hatch above the last steps
    b.disc(c, c, TOP, 12, HUSH_TILE, pick=lambda x, y, z: HUSH_CH if abs(math.hypot(x - c, z - c) - 5.0) < 0.6 else
           HUSH_POL if math.hypot(x - c, z - c) < 2.6 else HUSH_TILE)
    for a in last:
        for rr in (5.6, 6.6, 7.6):
            b.set(c + round(math.cos(a) * rr), TOP, c + round(math.sin(a) * rr), AIR)
    # battlements with sculk spikes
    for k in range(48):
        a = k * 2 * math.pi / 48
        x, z = c + round(math.cos(a) * 12), c + round(math.sin(a) * 12)
        b.set(x, TOP + 1, z, HUSH_BR)
        if k % 2 == 0:
            b.set(x, TOP + 2, z, HUSH_BR)
        if k % 6 == 0:
            for dy in range(3, 8):
                b.set(x, TOP + dy, z, SCULK if dy < 6 else B('minecraft:deepslate_tile_wall', up='true', north='none', south='none', east='none',
                                                                  west='none', waterlogged='false'))
    # eight pillars around the arena, crowned with soul lanterns and sensors
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        x, z = c + round(math.cos(a) * 9), c + round(math.sin(a) * 9)
        for dy in range(1, 6):
            b.set(x, TOP + dy, z, HUSH_CH if dy in (1, 5) else HUSH_POL)
        b.set(x, TOP + 6, z, SOUL_L if k % 2 else SENSOR)
    # the Grand Stage: a raised round stage, the podium at its heart and three Instrument Altars
    # waiting for the drum, the flute and the guitar; the vault chest beside it
    b.disc(c, c, TOP + 1, 5.4, HUSH_POL, pick=lambda x, y, z: HUSH_CH if math.hypot(x - c, z - c) > 4.6 else
           B('lumen_moss_block') if abs(math.hypot(x - c, z - c) - 2.0) < 0.5 else HUSH_POL)
    for k in range(3):
        a = math.pi / 2 + k * math.tau / 3
        ax, az = c + round(math.cos(a) * 3.6), c + round(math.sin(a) * 3.6)
        b.set(ax, TOP + 2, az, B('instrument_altar'))
    for k in range(6):
        a = k * math.tau / 6 + math.pi / 6
        b.set(c + round(math.cos(a) * 5.0), TOP + 2, c + round(math.sin(a) * 5.0), SOUL_L if k % 2 else DRUM)
    b.set(c, TOP + 2, c, B('conductors_podium'))
    b.set(c + 7, TOP + 1, c, chest('chests/sculk_castle', 'west'))
    b.decay(0.015, min_y=3, protect=('thesift:conductors_podium', 'thesift:instrument_altar', 'thesift:sift_drum', 'minecraft:chest', 'thesift:hushslate_tiles', 'thesift:polished_hushslate',
                                      'thesift:crumbling_dreamstone', 'minecraft:slime_block', 'minecraft:soul_lantern'))
    return b


# ================================================================== the drum pit


def thumper_arena(seed):
    """The Drum Pit: the Thumper's sunken amphitheatre. A round floor laid out like a drum skin,
    tiers of hushslate seats stepping up all round it, four ramps down into it, and four cannon
    towers - each with a spiral stair inside, an Ancient Cannon on top and chests of cannonballs.
    Walls and pillars of crumbling dreamstone stand about the floor: the Thumper smashes through
    them (the solid towers stop it dead). The sigil it sleeps under is in the middle."""
    S = 51
    y0 = 4
    H = y0 + 17
    b = Build(S, H, S, seed)
    c = S // 2
    R = 17          # the floor's edge
    TOP = 5         # the seats rise five steps above the floor
    rnd = b.rnd
    b.cyl(c, c, y0 + 1, H - 1, R + TOP + 2.6, AIR)
    b.cyl(c, c, 0, y0 - 1, R + TOP + 2.6, DS_BR)

    def skin(x, y, z):
        d = math.hypot(x - c, z - c)
        if d < 2.6:
            return HUSH_CH
        ring = int(d) % 6
        if ring == 0:
            return HUSH_POL
        if ring == 3:
            return DS_POL
        if d > 6 and rnd.random() < 0.07:
            return SCULK
        return DS_CR if rnd.random() < 0.12 else DS_TILE
    b.cyl(c, c, y0, y0, R + 0.6, DS_TILE, pick=skin)
    # the drum's tension cords: lines of bone from the rim to the middle
    for k in range(8):
        a = k * math.tau / 8 + math.tau / 16
        for r in range(4, R):
            b.set(c + math.cos(a) * r, y0, c + math.sin(a) * r, B('minecraft:bone_block', axis='y'))
    # the amphitheatre: tiers of seats, then the outer wall with lanterns along it
    for x in range(S):
        for z in range(S):
            d = math.hypot(x - c, z - c)
            if R + 0.6 <= d < R + TOP + 0.6:
                k = int(d - R - 0.6) + 1
                for y in range(y0, y0 + k):
                    b.set(x, y, z, HUSH_BR if (x * 3 + z + y) % 7 else HUSH_CR)
                b.set(x, y0 + k, z, HUSH_TILE)
            elif R + TOP + 0.6 <= d < R + TOP + 1.8:
                for y in range(y0, y0 + TOP + 3):
                    b.set(x, y, z, HUSH_BR if (x + z + y) % 5 else HUSH_CR)
    for k in range(16):
        a = k * math.tau / 16 + math.tau / 32
        b.set(c + round(math.cos(a) * (R + TOP + 1.1)), y0 + TOP + 3, c + round(math.sin(a) * (R + TOP + 1.1)), LANTERN)
    # four ramps down into the pit from the cardinal points
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        facing = {(1, 0): 'east', (-1, 0): 'west', (0, 1): 'south', (0, -1): 'north'}[(dx, dz)]
        for w in range(-1, 2):
            for r in range(R, R + TOP + 3):
                x, z = c + dx * r + dz * w, c + dz * r + dx * w
                k = r - R
                for y in range(y0 + 1, y0 + TOP + 4):
                    b.set(x, y, z, AIR)
                if 1 <= k <= TOP:
                    for y in range(y0, y0 + k):
                        b.set(x, y, z, DS_BR)
                    b.set(x, y0 + k, z, stairs('dreamstone_brick_stairs', facing))
                elif k > TOP:
                    for y in range(y0, y0 + TOP + 1):
                        b.set(x, y, z, DS_BR)
                    b.set(x, y0 + TOP + 1, z, DS_POL)
                else:
                    b.set(x, y0, z, DS_POL)
    # crumbling walls ringing the floor (gaps at the ramps) and crumbling pillars nearer the middle
    for x in range(S):
        for z in range(S):
            d = math.hypot(x - c, z - c)
            if R - 1.6 <= d < R - 0.6 and min(abs(x - c), abs(z - c)) > 2:
                for y in range(y0 + 1, y0 + 4 - (1 if rnd.random() < 0.2 else 0)):
                    b.set(x, y, z, CRUMBLE)
    for k in range(6):
        a = k * math.tau / 6
        px, pz = c + round(math.cos(a) * 8), c + round(math.sin(a) * 8)
        for (ox, oz) in ((0, 0), (1, 0), (0, 1), (1, 1)):
            for y in range(y0 + 1, y0 + 4):
                b.set(px + ox, y, pz + oz, CRUMBLE)
        b.set(px, y0 + 4, pz, CRUMBLE)
    # four cannon towers on the diagonals: solid stone, a spiral stair inside, a cannon on top
    ring = [(-1, -1), (0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0)]
    step_dir = ['east', 'east', 'south', 'south', 'west', 'west', 'north', 'north']
    TT = 10  # the platform's floor: you stand at y0 + TT + 1
    for i, (sx, sz) in enumerate(((1, 1), (-1, 1), (-1, -1), (1, -1))):
        tx, tz = c + sx * 9, c + sz * 9
        b.fill(tx - 2, y0, tz - 2, tx + 2, y0 + TT, tz + 2, HUSH_BR, hollow=True)
        b.fill(tx - 2, y0, tz - 2, tx + 2, y0, tz + 2, DS_POL)
        for y in range(y0 + 1, y0 + TT + 1):
            b.set(tx, y, tz, PILLAR)
            # arrow slits on every side
            if y % 4 == 1:
                for (ox, oz) in ((2, 0), (-2, 0), (0, 2), (0, -2)):
                    b.set(tx + ox, y, tz + oz, B('chrome_glass'))
        # the stair winds up round the pillar; the first step is just inside the door
        for h in range(1, TT + 1):
            j = h % 8
            x, z = tx + ring[j][0], tz + ring[j][1]
            b.set(x, y0 + h, z, stairs('polished_dreamstone_stairs', step_dir[j]))
            if h < TT:
                for y in range(y0 + h + 1, min(y0 + h + 3, y0 + TT + 1)):
                    if b.get(x, y, z) is None or b.get(x, y, z).name != 'thesift:polished_dreamstone_stairs':
                        b.set(x, y, z, AIR)
        # the door: in the wall beside the first step, facing the pit's middle
        dx, dz = ring[1]
        door = (tx + dx * 2, tz + dz * 2) if abs(dz) else (tx + dx * 2, tz)
        for y in (y0 + 1, y0 + 2):
            b.set(door[0], y, door[1], AIR)
        b.set(door[0], y0 + 3, door[1], HUSH_CH)
        b.set(tx + ring[0][0], y0 + 1, tz + ring[0][1], chest('chests/drum_pit_armory', 'south'))
        # the platform, a parapet round it, the cannon in the middle aimed at the floor
        b.fill(tx - 3, y0 + TT, tz - 3, tx + 3, y0 + TT, tz + 3, DS_POL)
        for h in (TT - 2, TT - 1):
            j = h % 8
            b.set(tx + ring[j][0], y0 + TT, tz + ring[j][1], AIR)
        j = TT % 8
        b.set(tx + ring[j][0], y0 + TT, tz + ring[j][1], stairs('polished_dreamstone_stairs', step_dir[j]))
        for x in range(tx - 3, tx + 4):
            for z in range(tz - 3, tz + 4):
                if max(abs(x - tx), abs(z - tz)) == 3:
                    b.set(x, y0 + TT + 1, z, HUSH_TILE if (x + z) % 2 else HUSH_CR)
        face = ('north' if sz > 0 else 'south') if (i % 2 == 0) else ('west' if sx > 0 else 'east')
        b.set(tx, y0 + TT + 1, tz, B('ancient_cannon', facing=face, loaded='false', powered='false'))
        b.set(tx + sx * 2, y0 + TT + 1, tz + sz * 2, chest('chests/drum_pit_armory', 'north' if sz > 0 else 'south'))
        for (ox, oz) in ((3, 3), (-3, 3), (3, -3), (-3, -3)):
            b.set(tx + ox, y0 + TT + 2, tz + oz, LANTERN)
        # sculk creeping up its feet
        for k in range(8):
            a = rnd.random() * math.tau
            b.set(tx + round(math.cos(a) * 3.5), y0, tz + round(math.sin(a) * 3.5), SCULK)
    # the sigil in the middle of the skin, catalysts round it
    b.set(c, y0, c, sigil(0))
    for k in range(4):
        a = k * math.tau / 4
        b.set(c + round(math.cos(a) * 5), y0, c + round(math.sin(a) * 5), B('minecraft:sculk_catalyst', bloom='false'))
    b.decay(0.02, top_bias=0.03, protect=('minecraft:chest', 'thesift:encore_sigil', 'thesift:ancient_cannon', 'thesift:polished_dreamstone',
                                         'thesift:polished_dreamstone_stairs', 'thesift:dreamstone_brick_stairs', 'minecraft:sculk_catalyst',
                                         'thesift:hushslate_bricks', 'thesift:dreamstone_pillar', 'thesift:crumbling_dreamstone'),
            min_y=y0 + TOP + 2)
    return b


# ================================================================== registration

STRUCTURES = {
    # name: (builders, biomes tag values, step, adaptation, start_height, spacing, separation, heightmap)
    'sculk_castle': ([sculk_castle], ['rocky_dunes', 'forest_mountains', 'sift_plains'], 'surface_structures', 'beard_thin', 0, 40, 16),
    'thumper_arena': ([thumper_arena, lambda s: thumper_arena(s + 5)], ['sift_plains', 'rocky_dunes', 'wishing_grove', 'forest_mountains'],
                      'surface_structures', 'beard_thin', -7, 34, 12),
}
# W1: the two that stayed keep their old places in the list for their template seeds and placement salts (unchanged worlds)
KEPT_INDEX = {'sculk_castle': 9, 'thumper_arena': 11}


def generate():
    total = 0
    for n, (name, (builders, biomes, step, adapt, start_y, spacing, sep)) in enumerate(STRUCTURES.items()):
        idx = KEPT_INDEX.get(name, n)
        os.makedirs(os.path.join(OUT, name), exist_ok=True)
        elements = []
        for i, fn in enumerate(builders):
            b = fn(1000 + idx * 37 + i * 11)
            total += b.save(os.path.join(OUT, name, f'{name}_{i}.nbt'))
            elements.append({'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{name}/{name}_{i}',
                                         'processors': 'minecraft:empty', 'projection': 'rigid'}, 'weight': 1})
        GA.write(os.path.join(D, 'template_pool', name, 'start.json'), {'elements': elements, 'fallback': 'minecraft:empty'})
        for bio in biomes:
            GA.tag('worldgen/biome', f'{NS}:has_structure/{name}', f'{NS}:{bio}')
        s = {'type': 'minecraft:jigsaw', 'biomes': f'#{NS}:has_structure/{name}', 'max_distance_from_center': 80, 'size': 1, 'spawn_overrides': {},
             'start_pool': f'{NS}:{name}/start', 'step': step, 'terrain_adaptation': adapt, 'use_expansion_hack': False}
        if start_y is None:
            s['start_height'] = {'type': 'minecraft:uniform', 'min_inclusive': {'absolute': -40}, 'max_inclusive': {'absolute': 0}}
        else:
            s['start_height'] = {'absolute': start_y}
            s['project_start_to_heightmap'] = 'WORLD_SURFACE_WG'
        GA.write(os.path.join(D, 'structure', name + '.json'), s)
        GA.write(os.path.join(D, 'structure_set', name + '.json'), {
            'placement': {'type': 'minecraft:random_spread', 'salt': 77310000 + idx * 7919, 'separation': sep, 'spacing': spacing},
            'structures': [{'structure': f'{NS}:{name}', 'weight': 1}]})
    __import__('caravans').structures(sys.modules[__name__])  # C: the Caravan colony
    print(f'structures ok: {len(STRUCTURES)} structures, {total} blocks')


if __name__ == '__main__':
    generate()
    GA.flush_tags()
