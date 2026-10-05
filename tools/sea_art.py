"""Sea & sky art (agents F + W): item sprites for the Gobbler's bladder and egg (hand-placed 16x16 rows in the
items16.py style, light from the top left; CR3 redrew the fish meats and sushi in tools/fish_items.py), and the
block textures of the glowkelps, the abyss anemone, coral sand, clouds, chime bells and organ reeds.

items16.all_items() takes items(); gen_textures.main() calls block_textures(out).
"""
from __future__ import annotations

import math
import random

from PIL import Image

import items16 as I

_E = '................'


def _fix(rows):
    """Pads/crops hand-typed rows to 16 x 16 so a stray character never breaks the build."""
    rows = [(r + _E)[:16] for r in rows]
    return (rows + [_E] * 16)[:16]


# ============================================================================ the Gobbler's bladder
# (CR3 Fish & Coral Organs: every fish's raw and cooked meat and the sushi are drawn in tools/fish_items.py now)

BLADDER = [
    '................',
    '.......bb.......',
    '......bBBb......',
    '.....ccbbcc.....',
    '...ccsSSSsscc...',
    '..csSSWWSSSssc..',
    '..csSWWSSvSSsc..',
    '.csSSSSSvSSSssc.',
    '.csSSvSSvSSSssc.',
    '.cssSSvvSSSSssc.',
    '.cssSSSSvSSsssc.',
    '..cssSSSvSssscc.',
    '..ccsssssssscc..',
    '....ccssssc.....',
    '......cccc......',
    '................',
]


def sculk_bladder():
    pal = {'b': I.BONE[3], 'B': I.BONE[4], 'c': ('#0b2532', '#041016'), 's': I.GLOW[0], 'S': I.GLOW[2], 'W': I.GLOW[4], 'v': I.SCULK[1]}
    return I.grid(BLADDER, pal, ol=True)


def gobbler_egg():
    o = '#03141a'
    return I.egg(['#07222a', '#0f3a40', '#1a5560', '#25646a'], o, {
        4: '.....g.g..g.....',
        5: '....b.......b...',
        7: '...g..bbbb..g...',
        9: '....ssSSSss.....',
        10: '....sSSSSSs.....',
        12: '...b.g....g.b...',
    }, pal={'g': (I.GLOW[2], o), 'b': (I.BONE[3], o), 's': (I.GLOW[1], o), 'S': (I.GLOW[3], o)}, ring='water')


def items():
    return {'sculk_bladder': sculk_bladder(), 'gobbler_spawn_egg': gobbler_egg()}


# ============================================================================ blocks

def _img(fn, seed=1):
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = fn(x, y, rnd)
            if c is not None:
                px[x, y] = I.rgba(c)
    return img


KELP = {'rose': ['#6a1a4a', '#b0306e', '#ff6fae', '#ffc2e0', '#fff0f8'],
        'azure': ['#0f3a6a', '#1f6fb0', '#5fc8ff', '#b8ecff', '#f0fcff'],
        'amber': ['#6a3a0a', '#b06a1a', '#ffb040', '#ffe0a0', '#fffaf0']}


def _kelp(ramp, tip, seed):
    """Two wavy ribbon fronds (crossed in the model) with a glowing midrib; the tip ends in bulbs."""
    def fn(x, y, rnd):
        for k, (base, phase) in enumerate(((5.0, 0.0), (10.0, 2.1))):
            cx = base + math.sin(y * 0.55 + phase) * 1.6
            d = abs(x - cx)
            if tip and y < 4 + k * 2:
                continue
            if d < 0.6:
                return ramp[3]
            if d < 1.7:
                return ramp[2] if (y + k) % 4 else ramp[3]
            if d < 2.4 and (y * 3 + k) % 5 != 0:
                return ramp[1]
        if tip:
            for bx, by in ((5, 3), (11, 5), (8, 1)):
                dd = (x - bx) ** 2 + (y - by) ** 2
                if dd <= 1:
                    return ramp[4]
                if dd <= 3:
                    return ramp[3]
        return None
    return _img(fn, seed)


def _anemone():
    """A squat sculk-dark column crowned with glowing cyan tentacles that curl outwards."""
    def fn(x, y, rnd):
        if y >= 11 and 4 <= x <= 11:
            return I.SCULK[2] if (x + y) % 3 else I.SCULK[3]
        if y >= 9 and 5 <= x <= 10:
            return I.SCULK[3]
        for i in range(6):
            a = -math.pi * (0.1 + 0.8 * i / 5)
            for t in range(1, 9):
                tx = 7.5 + math.cos(a) * t * 0.9
                ty = 9 + math.sin(a) * t * 0.9 - (t * t) * 0.02
                if abs(x - tx) < 0.7 and abs(y - ty) < 0.7:
                    return I.GLOW[4] if t >= 7 else I.GLOW[2] if t > 3 else I.GLOW[1]
        return None
    return _img(fn, 5)


def _coral_sand():
    def fn(x, y, rnd):
        r = rnd.random()
        if r < 0.05:
            return '#fff0f4'
        if r < 0.09:
            return '#ff8fb0'  # crushed coral bits
        return rnd.choice(('#f58a9c', '#f6a0ae', '#f49aa8', '#e8788c', '#f6a0ae'))
    return _img(fn, 77)


def _cloud():
    """Soft white with lavender shading in puffy lobes, slightly see-through."""
    def fn(x, y, rnd):
        lob = math.sin(x * 0.9) + math.sin(y * 0.8 + 1.0) + math.sin((x + y) * 0.5)
        if lob > 1.4:
            c, a = '#ffffff', 235
        elif lob > 0.2:
            c, a = '#f2f0ff', 225
        elif lob > -1.0:
            c, a = '#e2e0f8', 215
        else:
            c, a = '#cfcdf0', 205
        r, g, b, _ = I.rgba(c)
        return (r, g, b, a)
    return _img(fn, 3)


def _chime_bell(ringing=False):
    """A stem with two leaves and a hanging glass bell: pale cyan, glowing rim, a golden clapper."""
    rows = [
        '................',
        '.......s........',
        '.......ss.......',
        '....ccccsc......',
        '...cGGGGGcc.....',
        '...cGWWGGGc.....',
        '...cGWGGGGc.....',
        '..cGGGGGGGGc....',
        '..cRRRRRRRRc....',
        '......yy.....s..',
        '.......s....s...',
        '..ll...s...s....',
        '...lll.s..s.....',
        '.....l.sss......',
        '.......s........',
        '.......s........',
    ]
    pal = {'s': '#3f9d80', 'l': '#78d4a8', 'c': '#5a8ab0', 'G': '#a8e8ff' if not ringing else '#d8fbff', 'W': '#ffffff', 'R': I.GLOW[2],
           'y': I.GOLD[3]}
    return I.grid(rows, pal)


def _organ(top):
    """Clustered hollow pipes of lilac reed, banded like organ pipes; the tops open into dark mouths."""
    pipes = [(3, 2 if top else 0), (6, 0 if top else 0), (9, 4 if top else 0), (12, 1 if top else 0)]

    def fn(x, y, rnd):
        for i, (px_, start) in enumerate(pipes):
            if px_ - 1 <= x <= px_ + 1 and y >= start:
                if top and y in (start, start + 1):
                    return '#2a1a3a' if x == px_ else '#e8d8ff'
                if (y + i * 3) % 6 == 0:
                    return '#c8b0ff'
                return '#9a7ad8' if x == px_ - 1 else '#7a5ab8' if x == px_ else '#5a3a98'
        if not top and y >= 12 and rnd.random() < 0.4:
            return '#3f9d80'
        return None
    return _img(fn, 11 if top else 12)


def block_textures(out):
    for k, ramp in KELP.items():
        out(f'block/{k}_glowkelp', _kelp(ramp, False, 1))
        out(f'block/{k}_glowkelp_tip', _kelp(ramp, True, 2))
    out('block/abyss_anemone', _anemone())
    out('block/coral_sand', _coral_sand())
    out('block/cloud_block', _cloud())
    out('block/chime_bell', _chime_bell())
    out('block/chime_bell_ringing', _chime_bell(True))
    out('block/organ_reed_top', _organ(True))
    out('block/organ_reed_bottom', _organ(False))
