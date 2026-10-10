"""S1 land: the Creator's Ruin - where the Mini Creator is summoned.

An ancient overgrown shrine of dreamstone: a cracked round floor half lost under moss, a ring of broken pillars
banded with carved glyphs (chiseled hushslate), an arch over the way in, lanterns, flowers, and in the middle a
raised meditation floor with the Creator's Dais - a carved stone block with a gold circle of glyphs on its top.
His hymn is carved into the dais: use it to copy the Music Sheet (The Creator's Hymn); play the hymn beside it
(any instrument) and the Mini Creator rises out of it in a column of light (knowledge/CreatorShrine.java).

Hooks (one line each): spec.py declare(), gen_structures.STRUCTURES, gen_textures.main textures(), gen_assets.generate
assets(). Placement: random spread, spacing 18 chunks, separation 6, in the Sift's open lands - one is always
within a few hundred blocks of where you arrive, and the first-arrival hint tells you which way to go."""
import os

from PIL import Image

from structlib import B, Build

NS = 'thesift'
VANILLA = '/home/user/ref/mc-tex/assets/minecraft/textures/block'


def declare(block, item):
    block('creator_dais', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).mapColor(MapColor.QUARTZ)'
          '.strength(3.0F, 1200.0F).lightLevel(s -> 7)', cls='CreatorDaisBlock', model='cube_column', tags=['pickaxe'], tab='functional',
          name="Creator's Dais")


# ================================================================== the ruin

DS_BR = B('dreamstone_bricks')
DS_CR = B('cracked_dreamstone_bricks')
DS_POL = B('polished_dreamstone')
DS_TILE = B('dreamstone_tiles')
GLYPH = B('chiseled_hushslate')
MOSS = B('minecraft:moss_block')
CARPET = B('minecraft:moss_carpet')
DAIS = B('creator_dais')
LANTERN = B('bulb_lantern', hanging='false', waterlogged='false')


def _pillar(y):
    return B('dreamstone_pillar', axis='y')


def creators_ruin(seed):
    """17 x 11 x 17: the round shrine floor, eight broken pillars, the arch, the raised meditation floor and the dais."""
    import math
    import random
    rnd = random.Random(seed)
    S, H = 17, 11
    c = S // 2
    b = Build(S, H, S, seed)
    floor = b.mix((DS_TILE, 6), (DS_POL, 3), (DS_CR, 2), (MOSS, 3))
    # foundations under the floor, so it sits on uneven ground
    b.disc(c, c, 0, 7.6, DS_BR, pick=b.mix((DS_BR, 5), (DS_CR, 3), (MOSS, 1)))
    b.disc(c, c, 1, 7.4, DS_TILE, pick=floor)
    # the raised meditation floor: two steps up to the dais
    b.disc(c, c, 2, 3.4, DS_POL, pick=b.mix((DS_POL, 6), (DS_TILE, 2)))
    b.disc(c, c, 3, 1.6, DS_POL)
    b.set(c, 3, c, DAIS)
    # glyph stones set into the floor round the dais, like the hours of a clock
    for k in range(8):
        a = k * math.pi / 4
        b.set(c + round(math.cos(a) * 5), 1, c + round(math.sin(a) * 5), GLYPH)
    # eight pillars, broken off at different heights, banded with carved glyphs
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        x, z = c + round(math.cos(a) * 6.6), c + round(math.sin(a) * 6.6)
        top = 2 + rnd.choice((2, 3, 4, 5, 7, 8))
        for y in range(2, top + 1):
            b.set(x, y, z, GLYPH if y in (4, 7) else _pillar(y))
        if top >= 7 and rnd.random() < 0.6:
            b.set(x, top + 1, z, LANTERN)
    # the arch over the way in (south): two tall pillars and a lintel of bricks, half fallen
    for dx in (-2, 2):
        for y in range(2, 9):
            b.set(c + dx, y, S - 2, GLYPH if y == 5 else _pillar(y))
    for dx in range(-2, 3):
        if dx != 1:
            b.set(c + dx, 9, S - 2, DS_BR if dx else GLYPH)
    b.set(c - 1, 1, S - 1, B('polished_dreamstone_stairs', facing='north', half='bottom', shape='straight', waterlogged='false'))
    # fallen blocks lying about
    for _ in range(6):
        a = rnd.random() * math.tau
        r = rnd.uniform(3.6, 7.0)
        x, z = c + round(math.cos(a) * r), c + round(math.sin(a) * r)
        if b.get(x, 2, z) is None:
            b.set(x, 2, z, rnd.choice((DS_CR, DS_BR, MOSS)))
    # overgrowth: moss carpet, ferns and flowers on the floor, vines hanging off the pillars
    plants = [CARPET] * 6 + [B('minecraft:fern')] * 2 + [B('minecraft:short_grass')] * 3 + [B('minecraft:azure_bluet'), B('minecraft:oxeye_daisy'),
                                                                                            B('minecraft:lily_of_the_valley')]
    b.overgrow(['dreamstone_tiles', 'polished_dreamstone', 'cracked_dreamstone_bricks', 'minecraft:moss_block'], plants, 0.35)
    b.decay(0.04, top_bias=0.05, protect=('thesift:creator_dais', 'thesift:polished_dreamstone', 'thesift:chiseled_hushslate'), min_y=4)
    return b


STRUCTURES = {
    # name: (builders, biomes, step, adaptation, start_height, spacing, separation)
    'creators_ruin': ([creators_ruin, lambda s: creators_ruin(s + 3)], ['sift_plains', 'forest_mountains', 'wishing_grove', 'white_forest',
                                                                         'rocky_dunes', 'chrome_lakes'],
                      'surface_structures', 'beard_thin', 0, 18, 6),
}


# ================================================================== the dais's textures (vanilla-based, 16x16)

def _recolour(img, ramp):
    """Maps a vanilla texture onto a ramp of colours by brightness (keeps Mojang's own shading and detail)."""
    img = img.convert('RGBA')
    px = img.load()
    lums = sorted({int(0.3 * px[x, y][0] + 0.59 * px[x, y][1] + 0.11 * px[x, y][2]) for y in range(img.height) for x in range(img.width)})
    lo, hi = lums[0], max(lums[-1], lums[0] + 1)
    out = Image.new('RGBA', img.size)
    po = out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, bb, a = px[x, y]
            t = (0.3 * r + 0.59 * g + 0.11 * bb - lo) / (hi - lo)
            c = ramp[min(len(ramp) - 1, int(t * len(ramp)))]
            po[x, y] = (int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16), a)
    return out


STONE = ['#6e6878', '#8e8896', '#aca6ae', '#c7c1c1', '#d6d0cc', '#ebe7df']
GOLD = ['#8a6418', '#b88a24', '#e0b23a', '#ffd86a']


def textures(out):
    side = _recolour(Image.open(os.path.join(VANILLA, 'chiseled_stone_bricks.png')), STONE)
    top = _recolour(Image.open(os.path.join(VANILLA, 'chiseled_quartz_block_top.png')), STONE)
    # the gold circle of glyphs inlaid in the top: the carved grooves of the template turn to gold
    src = Image.open(os.path.join(VANILLA, 'chiseled_quartz_block_top.png')).convert('RGBA').load()
    tp = top.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = src[x, y]
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            if lum < 150 and 2 <= x <= 13 and 2 <= y <= 13:
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                c = GOLD[3 if d < 2.5 else (2 if lum > 120 else 1)]
                tp[x, y] = (int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16), 255)
    out('block/creator_dais_side', side)
    out('block/creator_dais_top', top)


# ================================================================== text

LANG = {
    f'codex.{NS}.creators_ruin.title': "The Creator's Ruin",
    f'codex.{NS}.creators_ruin.tagline': 'Where the Mini Creator waits',
    f'codex.{NS}.creators_ruin.body': "An overgrown shrine of dreamstone: broken pillars banded with glyphs round a raised floor, and in its "
                                      "middle the Creator's Dais. His hymn is carved into the dais - use it to copy the Music Sheet. Play "
                                      "The Creator's Hymn beside it, on any instrument, and the Mini Creator rises out of the dais in a "
                                      "column of light, gives you a Knowledge Book and your first goal. There is always one within a few "
                                      "hundred blocks of where you arrive; a Gold Harmoner can lead you to it.",
    f'message.{NS}.dais.sheet': "The Creator's Hymn is carved into the dais. You copy it onto a Music Sheet.",
    f'message.{NS}.dais.known': "The hymn is carved into the dais: play it here, on any instrument.",
    f'message.{NS}.hymn.no_dais': 'The hymn rings out... and fades. It must be played at the dais of a Creator\'s Ruin.',
    f'message.{NS}.hymn.summon': 'The dais answers your hymn - something rises out of the light!',
    f'quest.{NS}.arrival.hint': 'A faint hymn drifts on the wind from the %s, about %s blocks away: a Creator\'s Ruin. Find its dais.',
    f'quest.{NS}.arrival.hint_far': 'A faint hymn drifts on the wind... somewhere in the Sift a Creator\'s Ruin waits. Find its dais.',
    f'quest.{NS}.dir.north': 'north', f'quest.{NS}.dir.north_east': 'north-east', f'quest.{NS}.dir.east': 'east',
    f'quest.{NS}.dir.south_east': 'south-east', f'quest.{NS}.dir.south': 'south', f'quest.{NS}.dir.south_west': 'south-west',
    f'quest.{NS}.dir.west': 'west', f'quest.{NS}.dir.north_west': 'north-west',
}


def assets(GA):
    GA.LANG.update(LANG)
