"""A2 Swifter & White Forest: block, particle and item art (hooked from gen_textures.main and
items16.all_items). Blocks follow tools/mctex.py's vanilla rules: a flat mid tone, hand-sized
clusters of a lighter and a darker tone, no smooth gradients."""
import math
import random

import mctex as M
from texlib import Tex, hx, ramp, with_alpha

CUTOUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}
LEAVES_META = {'texture': {'mipmap_strategy': 'dark_cutout'}}

SOIL = ramp('#6a4252', '#7b4f5f', '#8d5e6d', '#9e6e7c', '#b0808c')          # the Sift's soil (gen_textures.SOIL)
TURF = ramp('#c3d3ea', '#d7e3f3', '#e7eff9', '#f3f7fd', '#fdfeff')
LEAF = ramp('#9fb6d8', '#bccde6', '#d6e2f3', '#e9f0fa', '#f6f9fe', '#ffffff')
FLUFF = ramp('#c9d9f0', '#dde8f7', '#edf3fc', '#f8fbff', '#ffffff')
TWIG = ramp('#4e3d31', '#6a5444', '#866b57', '#a3876e', '#bfa488')


def _paint(rows, keys):
    t = Tex(len(rows[0]), len(rows))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch not in '. ':
                v = keys[ch]
                t.set(x, y, hx(v) if isinstance(v, str) else v)
    return t


# ============================================================================ blocks

SAPLING = [
    '................',
    '.....ww.........',
    '....wWWw..ww....',
    '...wWWWWwwWWw...',
    '...wWWWWWWWWs...',
    '....swWWWWWs....',
    '..ww..sbWss.....',
    '.wWWw..b........',
    '.wWWWs.b..ww....',
    '..sss.bb.wWWw...',
    '.....b.B.sWWs...',
    '......bB.bss....',
    '.......Bb.......',
    '.......B........',
    '.......B........',
    '.......B........',
]
PUFFBLOOM = [
    '................',
    '......W.W.......',
    '...W..wWw..W....',
    '....wwWWWww.....',
    '..W.wWWWWWw.W...',
    '...wWWWcWWWw....',
    '.W.wWWcccWWw.W..',
    '...wWWWcWWWw....',
    '..W.wWWWWWw.W...',
    '....swwwwws.....',
    '...W...s...W....',
    '.......s........',
    '..lL...s..Ll....',
    '...lLL.s.LLl....',
    '.....lLsLl......',
    '.......s........',
]


def _den_side():
    """Woven twigs, wavy and over-under, with tufts of white fluff caught between them."""
    rnd = random.Random(301)
    t = M.fill(TWIG[0])
    for row in range(0, 16, 4):
        ph = rnd.random() * 6.0
        for x in range(16):
            yy = row + (1 if math.sin(x * 0.8 + ph) > 0.3 else 0)
            t.set(x, yy % 16, TWIG[3])
            t.set(x, (yy + 1) % 16, TWIG[2])
            t.set(x, (yy + 2) % 16, TWIG[1])
            if rnd.random() < 0.18:
                t.set(x, yy % 16, TWIG[4])
    for _ in range(3):  # a few sticks crossing the weave
        x0, y0 = rnd.randrange(16), rnd.randrange(16)
        for i in range(6):
            t.set((x0 + i) % 16, (y0 - i // 2) % 16, TWIG[3] if i % 3 else TWIG[4])
    for _ in range(5):  # fluff caught in the twigs
        x, y = rnd.randrange(16), rnd.randrange(16)
        M.blob(t, x, y, FLUFF[3], rnd.choice(M.BLOBS[:4]))
        t.set(x, y, FLUFF[4])
    return t


def _den_top():
    """The rim seen from above: twig ends and loops, fluff tucked in everywhere."""
    rnd = random.Random(302)
    t = M.stone(TWIG, 303, light=9, dark=9, core=0.3)
    for _ in range(9):
        x, y = rnd.randrange(16), rnd.randrange(16)
        M.blob(t, x, y, FLUFF[2], rnd.choice(M.BLOBS))
        t.set(x, y, FLUFF[4])
    return t


def _fluff():
    """The nest's lining: soft cloud-white fluff with pale blue hollows."""
    t = M.stone(FLUFF, 305, light=10, dark=8, core=0.25)
    rnd = random.Random(306)
    for _ in range(4):
        t.set(rnd.randrange(16), rnd.randrange(16), hx('#b9cfee'))
    return t


def _fluff_particles():
    frames = [
        ['..ww....', '.wWWw.w.', 'wWWWWwW.', '.wWWWWw.', '..wWWWw.', '.w.wWw..', '....w...', '........'],
        ['........', '...ww...', '..wWWw..', '.wWWWWw.', '..wWWw..', '...ww...', '........', '........'],
        ['........', '........', '...w.w..', '..wWWw..', '...wW...', '..w..w..', '........', '........'],
    ]
    keys = {'W': with_alpha(hx('#ffffff'), 235), 'w': with_alpha(hx('#d8e6fa'), 170)}
    return [_paint(f, keys) for f in frames]


def block_textures(out):
    soil = M.soil(SOIL, 51)
    top = M.stone(TURF, 91, light=9, dark=8, core=0.25)
    rnd = random.Random(92)
    for _ in range(4):
        top.set(rnd.randrange(16), rnd.randrange(16), hx('#a9c4ea'))
    for _ in range(3):
        top.set(rnd.randrange(16), rnd.randrange(16), hx('#ffffff'))
    out('block/white_turf_top', top)
    out('block/white_turf_side', M.grass_side(TURF, soil, 93))
    out('block/white_lullwood_leaves', M.leaves(LEAF, 95, None, holes=0.25), LEAVES_META)
    out('block/white_lullwood_sapling', _paint(SAPLING, {'W': '#ffffff', 'w': '#dfe8f6', 's': '#b9cbe6', 'b': '#3c5560', 'B': '#2f444e'}), CUTOUT)
    out('block/puffbloom', _paint(PUFFBLOOM, {'W': '#ffffff', 'w': '#e2ebf8', 'c': '#c3d4ee', 's': '#6f9a8e', 'l': '#5f8f84', 'L': '#93c3b2'}),
        CUTOUT)
    out('block/swifter_den_side', _den_side())
    out('block/swifter_den_top', _den_top())
    out('block/swifter_den_fluff', _fluff())
    out('block/swifter_den_bottom', M.stone(TWIG[0:5], 307, light=5, dark=8, core=0.4))
    for i, p in enumerate(_fluff_particles()):
        out(f'particle/white_fluff_{i}', p)


# ============================================================================ items


FLUFF_ITEM = [
    '................',
    '................',
    '.......dde......',
    '......deeee.....',
    '...dd.deeeeedd..',
    '..deeeeeeeeeeed.',
    '..deeeeeeeeeeed.',
    '.cdeeeeeeeeeeedc',
    '.cddeeeeeeeeeddc',
    '.bcdddeeeeedddcb',
    '..bccddddddddcb.',
    '...bbcccccccbb..',
    '.b...bbbbbbb....',
    '..bb............',
    '................',
    '................',
]


def item_sprites():
    import items16 as I
    fluff = I.grid(FLUFF_ITEM, {'e': '#ffffff', 'd': '#eef4ff', 'c': '#d2e1f7', 'b': '#a9c3ea'}, ol=True)
    # the Swifter: two pointed ears, fierce ice-blue eyes and its nose on a cloud-white egg resting on a cloud
    egg = I.egg(['#9fb9e0', '#c3d5f0', '#e3ecf9', '#fbfdff'], '#3f5687', {
        1: '.....p....p.....',
        2: '.....pP..Pp.....',
        5: '....lEE..EEl....',
        6: '.....EP..PE.....',
        8: '.......nn.......',
    }, {'p': '#97bdf0', 'P': '#ffffff', 'l': '#2a3a64', 'E': '#4fc2ff', 'n': '#34466e'}, ring='cloud')
    return {'swifter_fluff': fluff, 'swifter_spawn_egg': egg}
