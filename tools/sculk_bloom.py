"""A1: the Sculk Bloom - declared from spec.py, drawn for gen_textures.py, and its tags, sounds and
text for gen_assets.py. It grows on Bulbs' backs (Bulb.java) and calms Wardens (SculkBloomBlock)."""
from PIL import Image

# a dark sculk tulip on a teal stem: petals of the sculk block's own near-black teals with cyan veins,
# a soul-bright core welling up the middle and three sensor tendrils rising out of it
SPRITE = [
    '................',
    '................',
    '................',
    '........g.......',
    '....g...t...g...',
    '....t..kGk..t...',
    '...kvk.pGp.kvk..',
    '...kpPkpWpkPpk..',
    '....kpvpGpvpk...',
    '.....kkpppkk....',
    '.......kSk......',
    '........S.......',
    '....lL..S..Ll...',
    '.....lLLsLLl....',
    '.......lSl......',
    '........S.......',
]
PAL = {'g': '#29dfeb', 't': '#05625d', 'k': '#0d1217', 'p': '#052a32', 'P': '#034150', 'v': '#009295', 'G': '#29dfeb', 'W': '#c8fffb',
       'S': '#034150', 's': '#05625d', 'l': '#052a32', 'L': '#05625d'}


def sprite():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(SPRITE):
        assert len(row) == 16, row
        for x, ch in enumerate(row):
            if ch != '.':
                h = PAL[ch].lstrip('#')
                px[x, y] = (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)
    return img


def declare(block, item):
    """spec.py: the flower (its item is SculkBloomItem, which calms Wardens while held) and its pot."""
    block('sculk_bloom', 'flower', 'BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).mapColor(MapColor.COLOR_CYAN).sound(SoundType.SCULK)',
          effect='MobEffects.DARKNESS', secs='4.0F', light=3, tags=['flowers', 'small_flowers'], cls='SculkBloomBlock', item=False)
    block('potted_sculk_bloom', 'pot', 'BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY).lightLevel(s -> 3)', plant='sculk_bloom',
          item=False, loot='pot:sculk_bloom', cls='PottedSculkBloomBlock')
    item('sculk_bloom', cls='SculkBloomItem', props='new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.RARE)', tab='nature')


def textures(out):
    """gen_textures.py: the flower (cross model, potted too) and its item."""
    img = sprite()
    out('block/sculk_bloom', img, {'texture': {'mipmap_strategy': 'strict_cutout'}})
    out('item/sculk_bloom', img)


SOUNDS = {
    'entity.bulb.nibble': [('event:entity.sniffer.eat', 0.5, 1.9), ('event:entity.goat.eat', 0.5, 2.0)],
    'entity.bulb.pluck': [('event:block.sweet_berry_bush.pick_berries', 0.9, 1.4), ('event:entity.mooshroom.shear', 0.5, 1.8)],
    'block.sculk_bloom.puff': [('event:block.sculk.charge', 0.8, 0.7), ('event:block.sculk_catalyst.bloom', 0.6, 0.85)],
    'block.sculk_bloom.calm': [('event:entity.warden.listening', 0.9, 1.3)],
    'entity.stomper.shake': [('event:entity.wolf.shake', 1.0, 0.5), ('event:block.azalea_leaves.break', 0.8, 0.7)],
}
SUBTITLES = {
    'entity.bulb.nibble': 'Bulb nibbles a flower', 'entity.bulb.pluck': 'Bulb plucks a flower',
    'block.sculk_bloom.puff': 'Sculk Bloom puffs smoke', 'block.sculk_bloom.calm': 'Warden calms down',
    'entity.stomper.shake': 'Stomper shakes its garden',
}


def assets(tag, rl, lang):
    """gen_assets.py: the item counts as a (small) flower too, and its Codex page."""
    for t in ('minecraft:flowers', 'minecraft:small_flowers'):
        tag('item', t, rl('sculk_bloom'))
    lang.update({
        'codex.thesift.sculk_bloom.title': 'Sculk Bloom', 'codex.thesift.sculk_bloom.tagline': 'Hush for Wardens, smoke for you',
        'codex.thesift.sculk_bloom.body': 'A dark sculk tulip with a soul-bright heart. It grows on a Bulb\'s back now and then: give a '
                                          'Bulb any small flower and it may pluck you this one. Wherever it is - planted, potted, on a '
                                          'Bulb or in your hand - every Warden within 16 blocks forgets its anger and stops hunting. '
                                          'But a planted bloom puffs dark Sculk smoke every few seconds, and anything that breathes it '
                                          'in is briefly Sculk Corrupted.',
    })
