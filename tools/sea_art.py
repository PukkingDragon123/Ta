"""Sea & sky art (agents F + W): item sprites for the fish meats, the Gobbler's bladder, the sushi
and the platter (hand-placed 16x16 rows in the items16.py style, light from the top left), and the
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


# ============================================================================ fish meats

TUBAFISH = [
    '................',
    '.......gG.......',
    '......gGGg......',
    '....bbbbbbbb....',
    '...bBBBsBBBBb...',
    '..bBBsBBBBsBBb..',
    '..bBBBBBBBBBBbtt',
    '..bewBBBsBBBBbtT',
    '.LlBBBBBBBBBBbtt',
    '.LlbBBsBBBBsBb..',
    '..bpppppppppb...',
    '...bppppppppb...',
    '....bbppppbb....',
    '......fbbf......',
    '.....f....f.....',
    _E,
]


def tubafish(cooked=False):
    if cooked:
        pal = {'b': '#7a4a2a', 'B': '#b07a48', 's': '#d8a868', 'p': '#e0b88a', 'e': '#2a1810', 'w': '#f0e0c8', 'L': '#a05a3a', 'l': '#c88060',
               'g': '#9a7438', 'G': '#d0a858', 't': '#8a5a34', 'T': '#b08050', 'f': '#8a5a34'}
    else:
        pal = {'b': '#5d86cc', 'B': '#78a5e3', 's': '#3ff5e6', 'p': '#ffe6ef', 'e': '#1a1420', 'w': '#ffffff', 'L': '#c96a8c', 'l': '#f59ab8',
               'g': '#b8923a', 'G': '#f2d27a', 't': '#e07a9a', 'T': '#ffb0c4', 'f': '#e07a9a'}
    return I.grid(_fix(TUBAFISH), pal, ol=True)


FANFARE_EEL = [
    '................',
    '..........GgG...',
    '.........GhhhG..',
    '.........ghDhg..',
    '..........gGg...',
    '.........bb.....',
    '........bHb.....',
    '.......bHrb.....',
    '......bHrHb.....',
    '.....bHrHbc.....',
    '....bHrHbcc.....',
    '...bHrHbc.......',
    '..bHrHbc........',
    '.bHHbcc.........',
    '.cfcc...........',
    '..c.............',
]


def fanfare_eel(cooked=False):
    if cooked:
        pal = {'b': '#5a3a24', 'H': '#8a5e3a', 'r': '#d8c09a', 'c': '#7a5034', 'f': '#a07040', 'G': '#a07a3a', 'g': '#7a5a2a', 'h': '#d0b070',
               'D': '#2a1a10'}
    else:
        pal = {'b': '#0c1a24', 'H': '#24485a', 'r': '#e3ddcc', 'c': '#16303e', 'f': '#3ff5e6', 'G': '#f2d27a', 'g': '#b8923a', 'h': '#fff0b0',
               'D': '#0a1a22'}
    return I.grid(_fix(FANFARE_EEL), pal, ol=True)


FILLET = [
    '................',
    '................',
    '...........aab..',
    '.........aabbbc.',
    '.......aabvbbcc.',
    '.....aabbvbbbcc.',
    '....abbbvbbbcc..',
    '...abbbvbbbcc...',
    '..abbbvbbbcc....',
    '..abbvbbbcc.....',
    '.abbvbbbcc......',
    '.abbbbbcc.......',
    '.abbbcc.........',
    '.accc...........',
    '................',
    '................',
]


def gobbler_fillet(cooked=False):
    if cooked:
        pal = {'a': '#e0b080', 'b': '#c08850', 'c': '#8a5a30', 'v': '#6a4022'}
    else:
        # pale teal-grey deep-sea flesh, a glowing sculk vein down the middle
        pal = {'a': '#d8f0ec', 'b': '#a8d0cc', 'c': '#6a9a9a', 'v': ('#3ff5e6', '#0f4a50')}
    return I.grid(FILLET, pal, ol=True)


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


# ============================================================================ sushi

ROLL = [
    '................',
    '................',
    '.....kkkkkk.....',
    '...kkrrrrrrkk...',
    '..krrrwrrrrrrk..',
    '..krrFFFFFrrrk..',
    '.krrFfffffFrrrk.',
    '.krrFfffffFrgrk.',
    '.kwrrFFFFFrrrrk.',
    '.kKrrrrrrrrrrKk.',
    '.kKKrrrrrrrrKKk.',
    '.kkKKKKKKKKKKkk.',
    '.kkkkkkkkkkkkkk.',
    '..kkkkGkkkkkkk..',
    '....kkkkkkkk....',
    '................',
]
NORI = {'k': ('#1f3a2a', '#0a1410'), 'K': ('#2f5a3a', '#0a1410'), 'r': ('#f6f2e6', '#6a6458'), 'w': ('#ffffff', '#6a6458')}
SUSHI_FILLING = {
    'kazoo_fish_sushi': ('#3fd0c4', '#f87d8d', '#ff6fae'),      # teal flesh, coral skin, a rose glowkelp sprinkle
    'tubafish_sushi': ('#ffe6ef', '#78a5e3', '#5ff8ff'),        # pale puffer flesh rimmed in periwinkle, azure glowkelp
    'fanfare_eel_sushi': ('#e8c27a', '#24485a', '#ffc04a'),     # glazed gold eel, dark hide, amber glowkelp
    'gobbler_sushi': ('#a8d0cc', '#0f3a40', '#3ff5e6'),         # deep-sea fillet wrapped round a glowing sculk-bladder core
}


def sushi(name):
    f, F, g = SUSHI_FILLING[name]
    pal = dict(NORI)
    pal.update({'f': (f, '#2a2a3a'), 'F': (F, '#1a1a2a'), 'g': (g, '#3a1a3a'), 'G': (g, '#3a1a3a')})
    if name == 'gobbler_sushi':
        rows = list(ROLL)
        rows[6] = '.krrFfSSffFrrrk.'
        rows[7] = '.krrFfSSffFrgrk.'
        pal['S'] = I.GLOW[3]
        return I.grid(rows, pal, ol=True, no_ol='gG')
    return I.grid(ROLL, pal, ol=True, no_ol='gG')


PLATTER = [
    '................',
    '................',
    '................',
    '..AAA.BBB.......',
    '.AaaA.BbbB......',
    '.AaaA.BbbB.CCC..',
    '.kAAk.kBBk.CccC.',
    '.kkkk.kkkk.CccC.',
    '...DDD.....kCCk.',
    '..DddD.....kkkk.',
    '..DddD..........',
    'wwkDDkwwwwwwwwww',
    'WWkkkkWWWWWWWWWW',
    'WWWWWWWWWWWWWWWW',
    '.dd..........dd.',
    '................',
]


def sushi_platter():
    pal = {'k': ('#1f3a2a', '#0a1410'), 'w': ('#b18a52', '#2a1a16'), 'W': ('#8f6a36', '#2a1a16'), 'd': ('#4d3220', '#2a1a16')}
    for ch, name in zip('ABCD', SUSHI_FILLING):
        f, F, _g = SUSHI_FILLING[name]
        pal[ch] = (F, '#0a1410')
        pal[ch.lower()] = (f, '#2a2a3a')
    return I.grid(PLATTER, pal, ol=True)


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
    out = {
        'tubafish': tubafish(), 'cooked_tubafish': tubafish(True), 'fanfare_eel': fanfare_eel(), 'cooked_fanfare_eel': fanfare_eel(True),
        'gobbler_fillet': gobbler_fillet(), 'cooked_gobbler_fillet': gobbler_fillet(True), 'sculk_bladder': sculk_bladder(),
        'sushi_platter': sushi_platter(), 'gobbler_spawn_egg': gobbler_egg(),
    }
    for name in SUSHI_FILLING:
        out[name] = sushi(name)
    return out


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
