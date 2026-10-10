"""Every Sift item sprite, hand-drawn at 16x16 in the vanilla item style.

Tools and armour are the vanilla diamond textures recoloured tone-for-tone into the Siftite
palette (so the silhouettes match Minecraft 1:1), with a few hand-placed detail pixels. Every
other sprite is drawn by hand below as rows of characters with its own palette: one character
per pixel, '.' is transparent. Most sprites list only their fill and shading; `ol=True` then adds
the one-pixel outline in a dark, hue-shifted shade of each material (a little lighter on the
lit top-left side), exactly where a pixel artist would put it.

Light always comes from the top left.
"""
from __future__ import annotations

import colorsys
import os

from PIL import Image

VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')


# ============================================================================ helpers

def rgba(c):
    if isinstance(c, tuple):
        return c if len(c) == 4 else (*c, 255)
    c = c.lstrip('#')
    a = int(c[6:8], 16) if len(c) == 8 else 255
    return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16), a)


def shade(c, v=0.32, hue=0.07, sat=0.18):
    """A darker shade of colour c, hue-shifted toward violet the way shadows are painted."""
    r, g, b, a = rgba(c)
    h, s, val = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    target = 0.72
    d = (target - h + 0.5) % 1.0 - 0.5  # shortest way round the hue circle toward violet
    h = (h + max(-hue, min(hue, d))) % 1.0
    s = min(1.0, s + sat) if s > 0.05 else s
    r, g, b = colorsys.hsv_to_rgb(h, s, val * v)
    return (round(r * 255), round(g * 255), round(b * 255), 255)


def mix(a, b, t):
    a, b = rgba(a), rgba(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def grid(rows, pal, ol=False, size=None, no_ol=''):
    """Paints rows of characters with palette pal. A palette value is a colour, or a pair
    (colour, outline colour) to give that material its own outline. With ol=True every empty
    pixel that touches a filled one edge-on becomes outline: darkest on the bottom/right
    (shadow) side, a step lighter on the top/left (lit) side. Characters in no_ol (glints,
    sparkles, strings) cast no outline."""
    w, h = size or (16, 16)
    assert len(rows) == h, f'{len(rows)} rows, expected {h}'
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    cells = {}
    for y, row in enumerate(rows):
        assert len(row) == w, f'row {y} is {len(row)} wide: {row!r}'
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            if ch not in pal:
                raise KeyError(f'no colour for {ch!r} in row {y}: {row!r}')
            v = pal[ch]
            col = rgba(v[0] if isinstance(v, (list, tuple)) and not isinstance(v[0], int) else v)
            px[x, y] = col
            cells[(x, y)] = ch
    if ol:
        def ol_of(ch):
            v = pal[ch]
            if isinstance(v, (list, tuple)) and not isinstance(v[0], int):
                return rgba(v[1])
            return shade(v)
        add = {}
        for y in range(h):
            for x in range(w):
                if (x, y) in cells:
                    continue
                # prefer the neighbour that puts this pixel in shadow (right/bottom edges)
                best = None
                for dx, dy, lit in ((-1, 0, False), (0, -1, False), (1, 0, True), (0, 1, True)):
                    ch = cells.get((x + dx, y + dy))
                    if ch is None or ch in no_ol:
                        continue
                    if best is None or (best[1] and not lit):
                        best = (ch, lit)
                if best:
                    c = ol_of(best[0])
                    add[(x, y)] = mix(c, pal_col(pal, best[0]), 0.28) if best[1] else c
        for (x, y), c in add.items():
            px[x, y] = c
    return img


def pal_col(pal, ch):
    v = pal[ch]
    return rgba(v[0] if isinstance(v, (list, tuple)) and not isinstance(v[0], int) else v)


def ramp(chars, colours, ol=None):
    """Assigns a ramp of colours (dark to light) to chars, all sharing one outline colour."""
    o = ol or shade(colours[len(colours) // 2], 0.3)
    return {ch: (c, o) for ch, c in zip(chars, colours)}


def vanilla(name):
    return Image.open(os.path.join(VANILLA, 'item', name + '.png')).convert('RGBA')


def recolour(img, cmap, name=''):
    out = img.copy()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            c = px[x, y]
            if c[3] == 0:
                continue
            key = '#%02x%02x%02x' % c[:3]
            if key not in cmap:
                raise KeyError(f'{name}: vanilla colour {key} at {x},{y} has no Siftite tone')
            px[x, y] = rgba(cmap[key])[:3] + (c[3],)
    return out


def dots(img, spec, pal):
    """Hand-places detail pixels from a string 'x,y,ch x,y,ch ...'."""
    px = img.load()
    for tok in spec.split():
        x, y, ch = tok.split(',')
        px[int(x), int(y)] = rgba(pal[ch])
    return img


# ============================================================================ palettes

# Siftite: the dreamy pastel metal. Indigo outlines, cyan body, blush-white highlights.
S_OUT, S_OUT2 = '#261a56', '#44388e'
S_RAMP = ['#5a4aa8', '#6a7ad8', '#6ab0e8', '#7ed8ee', '#b8f0f6', '#eefbff', '#fff0fa']
PINK = ['#6e2a66', '#b04d98', '#f29bd6', '#ffd6f0']
GOLD = ['#4a2418', '#8a4e22', '#c9862c', '#eebd4a', '#fde58f', '#fffbe0']
WOOD = ['#2a1a16', '#4d3220', '#6e4c2a', '#8f6a36', '#b18a52']
BONE = ['#4a3e48', '#8d8070', '#bdb29a', '#e2dac4', '#f6f2e6', '#ffffff']
SCULK = ['#06141c', '#0b2532', '#123a48', '#1b5462', '#27707c']
GLOW = ['#0f8f99', '#22c7c4', '#3ff5e6', '#a8fff6', '#ffffff']
CHROME = ['#2e2470', '#4b3f9e', '#7f86e6', '#b9b8ff', '#ffd2f2', '#ffffff']
SERBIM = ['#173a6e', '#1f6d94', '#2b8fbb', '#3fb0da', '#66d2f0', '#9aeefc', '#e4fdff']
LEAF = ['#163a32', '#25603f', '#3a8f4a', '#5cba58', '#9ade78']
WHITE = '#ffffff'

# vanilla diamond tone -> Siftite tone of the same rank
TOOL_MAP = {
    # blade / head
    '#082520': S_OUT, '#0e3f36': S_OUT2, '#156355': S_RAMP[0], '#1e8a77': S_RAMP[1], '#27b29a': S_RAMP[2],
    '#2bc7ac': S_RAMP[2], '#33ebcb': S_RAMP[3], '#a4fdf0': S_RAMP[5], '#f2fffd': S_RAMP[6],
    # armour
    '#1aaaa7': S_RAMP[1], '#20c5b5': S_RAMP[2], '#4aedd9': S_RAMP[3], '#a1fbe8': S_RAMP[4], '#ffffff': S_RAMP[6],
    # wooden handle (kept vanilla-ish, a touch warmer)
    '#281e0b': '#2b1b10', '#493615': '#4d3419', '#684e1e': '#6e4f24', '#896727': '#906c31',
    # the spear's grip, re-wrapped in plum leather
    '#411805': '#3a1a3c', '#67290b': '#5e2a56', '#834121': '#7a3c6c', '#a34b22': '#94507e', '#ba5f34': '#b06c98',
}
GEM = {'p': PINK[2], 'P': PINK[1], 'q': PINK[3], 'w': WHITE, 'h': S_RAMP[5], 'o': PINK[0], 'g': GOLD[3], 'G': GOLD[2]}

def item_animations():
    """{texture name: (frames, frametime)} for every animated item texture (RR: none since the Siftite tools are gone)."""
    return {}


# ============================================================================ materials


def glowing_slime_ball():
    """The vanilla slime ball, lime and glowing from its core."""
    rows = [
        '................',
        '................',
        '......bbbb......',
        '....bbeffeba....',
        '...befggfgeca...',
        '...bfihgggfea...',
        '..beghhfggffca..',
        '..beggfyyfffda..',
        '..befggyYfefda..',
        '..bdefeyffdcda..',
        '...bdeffedcca...',
        '...acddccccda...',
        '....aaddddaa....',
        '......aaaa......',
        '................',
        '................',
    ]
    pal = {'a': '#2c5a1e', 'b': '#4a8424', 'c': '#78b42c', 'd': '#94cc36', 'e': '#b2e048', 'f': '#cbee5e', 'g': '#e2f98a',
           'h': '#f6ffc8', 'i': WHITE, 'y': '#fbffb0', 'Y': WHITE}
    return grid(rows, pal)


def warden_core():
    """A glowing sculk heart."""
    rows = [
        '................',
        '................',
        '...3443..3442...',
        '..345543344432..',
        '.34554G43G44322.',
        '.3455GgG3Gg3321.',
        '.3444GwgGgG3221.',
        '.33444GggG32211.',
        '..33444GG32211..',
        '...3344G43221...',
        '....33443221....',
        '.....334221.....',
        '......3221......',
        '.......21.......',
        '................',
        '................',
    ]
    pal = ramp('12345', SCULK[1:] + ['#3a8c94'], '#04101a')
    pal.update({'G': ('#22c7c4', '#04101a'), 'g': ('#3ff5e6', '#04101a'), 'w': ('#c8fffa', '#04101a')})
    return grid(rows, pal, ol=True)


def thick_hide():
    """A vanilla-leather-shaped hide in blue-grey, stitched down the middle."""
    rows = [
        '................',
        '......bbbb......',
        '..bb.bdfecb.bb..',
        '..bdbefffdebda..',
        '...beedsfeeda...',
        '...bdfdffdeca...',
        '....bfcsfeda....',
        '....bdeefeda....',
        '....beesfeea....',
        '....beedfeea....',
        '...bcdesededa...',
        '...beeeeeeeca...',
        '...bedddcdeda...',
        '..bcdaaccaadca..',
        '..baa..aa..aaa..',
        '................',
    ]
    pal = {'a': '#1e2a44', 'b': '#2f4562', 'c': '#4a6a88', 'd': '#5f86a3', 'e': '#7aa2bc', 'f': '#9cc0d4', 's': '#e6f2f8'}
    return grid(rows, pal)


# ============================================================================ food, tools of the trade, blocks


def soul_chime():
    """A wind chime: a wooden bar hung with soul-glass tubes."""
    rows = [
        '................',
        '........k.......',
        '..BBBBBBBBBBBB..',
        '..bbbbbbbbbbbb..',
        '...k..k..k..k...',
        '...k..k..k..k...',
        '...Tt.Tt.Tt.Tt..',
        '...Tt.Tt.Tt.Tt..',
        '...Tt.Tt.Tt.Tt..',
        '...Tt.Tt.Tt.gg..',
        '...Tt.Tt.Tt.ee..',
        '...gg.Tt.Tt.....',
        '...ee.Tt.gg.....',
        '......gg.ee.....',
        '......ee........',
        '................',
    ]
    pal = {'B': (WOOD[4], WOOD[0]), 'b': (WOOD[2], WOOD[0]), 'k': '#c8b4a0',
           'T': ('#c8f4ff', '#163a5a'), 't': ('#6fb8e0', '#163a5a'), 'g': ('#3ff5e6', '#163a5a'), 'e': ('#2a7aa8', '#163a5a')}
    return grid(rows, pal, ol=True, no_ol='k')


def conductors_staff():
    """The Conductor's Staff: a sculk-dark shaft bound in gold, a human skull with glowing cyan
    eyes, and two bone prongs rising from its crown to cradle a glowing orb."""
    rows = [
        '................',
        '........bcgcb...',
        '.......b.gwg.b..',
        '.......B.cgc.B..',
        '........b...b...',
        '........45554...',
        '.......4555553..',
        '.......4vg5gv3..',
        '.......4vv5vv3..',
        '........44n43...',
        '........t3t3t...',
        '.......G.333....',
        '......k.........',
        '.....K..........',
        '....G...........',
        '...k............',
    ]
    bone = '#3a2e34'
    pal = {'b': (BONE[3], bone), 'B': (BONE[2], bone), '5': (BONE[4], bone), '4': (BONE[3], bone), '3': (BONE[2], bone),
           'n': ('#5e5048', bone), 't': (WHITE, bone), 'v': ('#141020', bone), 'g': (GLOW[2], '#0a3038'), 'c': (GLOW[1], '#0a3038'),
           'w': (GLOW[4], '#0a3038'), 'G': (GOLD[3], GOLD[0]), 'k': (SCULK[3], SCULK[0]), 'K': (SCULK[4], SCULK[0])}
    return grid(rows, pal, ol=True)


# ============================================================================ the new orchestra's spoils

def conga_drum():
    """A tall tapered conga: tan skin, metal hoops, rope lacing and a turtle-shell body."""
    rows = [
        '................',
        '....tTTTTTtt....',
        '...tTTTTTTttt...',
        '....sttttsss....',
        '...RRRRRRRRrr...',
        '...lGGglgghlh...',
        '...glGlglglhl...',
        '...gGlggglhhh...',
        '...RRRRRRRRrr...',
        '....GGbggbhh....',
        '....GGbggbhh....',
        '.....bbbbbb.....',
        '.....Gbggbh.....',
        '.....Gbggbh.....',
        '.....RRRRrr.....',
        '................',
    ]
    pal = {'t': '#e8c890', 'T': '#f8e2b8', 's': '#c8a46a', 'R': '#d8dae6', 'r': '#8a90a8', 'l': '#f6f0dc',
           'G': '#8fbf5a', 'g': '#5e9a40', 'h': '#3e6a32', 'b': '#7a4e2a'}
    pal = {k: (v, '#1e2416') for k, v in pal.items()}
    return grid(rows, pal, ol=True)


def crane_flute():
    """A bone carved into a flute: soul-glow in its holes, an amethyst reed, white feathers tied at the foot."""
    rows = [
        '................',
        '.............Mo.',
        '............LMD.',
        '...........LMD..',
        '..........gMD...',
        '.........LMD....',
        '........gMD.....',
        '.......LMD......',
        '......gMD.......',
        '.....LMD........',
        '....RrD.........',
        '...RrrD.........',
        '.Ww..Ww.........',
        '.Ww..Wk.........',
        '.Wk..k..........',
        '.k..............',
    ]
    pal = ramp('LMD', ['#fff4d6', '#ecd29a', '#c49a5a'], '#5a3a22')
    pal.update({'g': (GLOW[2], '#5a3a22'), 'o': ('#2a1a12', '#5a3a22'), 'r': ('#c83a4a', '#3a1018'), 'R': ('#f06a6a', '#3a1018'),
                'W': ('#ffffff', '#3a3a4a'), 'w': ('#c8ccd8', '#3a3a4a'), 'k': ('#2a2a36', '#101018')})
    return grid(rows, pal, ol=True)


def magic_strings():
    """Two glowing strands, violet and cyan, wound into loose interlocking loops."""
    rows = [
        '................',
        '..v.............',
        '...vv...........',
        '.....vvwv.......',
        '...vv....vv.....',
        '..v........v....',
        '..v.....cccc....',
        '..v...cc...vcc..',
        '...vvc...vv...c.',
        '.....vvvv.....c.',
        '.....c........c.',
        '......cc....cc..',
        '........ccwc....',
        '............cc..',
        '..............c.',
        '................',
    ]
    pal = {'v': '#c08af8', 'c': '#8ff0ff', 'w': '#ffffff'}
    img = grid(rows, pal)
    halo = grid(rows, {'v': '#8a4ae070', 'c': '#22c7c470', 'w': '#ffffff70'})
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    hp, op = halo.load(), out.load()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < 16 and 0 <= y + dy < 16 and hp[x + dx, y + dy][3]:
                    op[x, y] = hp[x + dx, y + dy]
                    break
    out.alpha_composite(img)
    return out


def guitar():
    """An acoustic guitar held like a tool, its magical strings glowing down the neck."""
    rows = [
        '................',
        '............phh.',
        '.............hhp',
        '............gN..',
        '...........gN...',
        '.......ab.gN....',
        '......aabgN.....',
        '.....aabgNc.....',
        '....aabgbbc.....',
        '..aaaogbbc......',
        '.aabogoc........',
        '.aBbgobc........',
        '.bbBbbcc........',
        '..bbBcc.........',
        '...ccc..........',
        '................',
    ]
    pal = {'a': ('#f4c070', '#4a2414'), 'b': ('#d8963e', '#4a2414'), 'c': ('#a8682a', '#4a2414'),
           'o': ('#3a1e14', '#4a2414'), 'B': ('#5a3420', '#4a2414'), 'N': ('#6a3e26', '#24120c'), 'h': ('#4a2a1a', '#24120c'),
           'p': ('#e0e4ee', '#3a3a4a'), 'g': ('#b8fff8', '#24120c')}
    return grid(rows, pal, ol=True)


def stomper_meat():
    """Raw Stomper: vanilla beef proportions, marbled pink flesh under a chrome-blue rind."""
    rows = [
        '................',
        '................',
        '................',
        '.........rrrr...',
        '........rRRRRr..',
        '.......rRhfdgRc.',
        '......rRfmedfdb.',
        '....rrRmeefdmhca',
        '...rRRmedfdehdba',
        '...rRfdgmefdbaa.',
        '..rRhdeefgdbba..',
        '..rRmefggdbba...',
        '..aRhhgdccba....',
        '...accccbaa.....',
        '....aaaaa.......',
        '................',
    ]
    pal = {'a': '#3a0a20', 'b': '#7a1a32', 'c': '#a82a40', 'd': '#d84a5a', 'e': '#e6606a', 'f': '#ee7a80', 'g': '#f4928e',
           'h': '#f8a8a0', 'm': '#ffd6dc', 'r': '#5a6ad0', 'R': '#a8b8ff'}
    return grid(rows, pal)


def stomper_egg():
    """A big Stomper egg, pale grey-blue with chrome speckles."""
    tones = ['#7a8aa8', '#a0b0c8', '#c4d2e2', '#e2ecf6']
    return egg(tones, '#2e3450', {
        4: '........Cc......',
        6: '....Cc..........',
        7: '....cc.....C....',
        8: '...........c....',
        9: '.......Cc.......',
        10: '.......cc.......',
        12: '.....c..........',
    }, {'C': CHROME[3], 'c': CHROME[1]})




# ============================================================================ spawn eggs
# The vanilla 26.x egg: a shaded egg in the mob's colours, wearing its most recognisable features
# (ears, horns, legs, a drum...). The base egg is shaded with tones 1-4 (dark to light) and a glint
# 'w'; each mob paints its features over it (and, for things behind the egg, under it).

EGG = [
    '................',
    '................',
    '......4433......',
    '.....444433.....',
    '....44w44333....',
    '....4w443332....',
    '...4444333332...',
    '...4443333322...',
    '...4433333322...',
    '...4333333222...',
    '...3333332221...',
    '...3333322221...',
    '....33322221....',
    '.....222211.....',
    '......1111......',
    '................',
]

# a ring of water (or cloud) around the bottom of the egg, as on vanilla fish eggs
RING_UNDER = {12: '..VV........VV..', 13: '..VXV......VXV..'}
RING_OVER = {14: '...ZVXVVVVXVZ...', 15: '....ZZZZZZZZ....'}
WATER = {'V': ('#3e8ac0', '#18305a'), 'X': ('#7ad8ff', '#18305a'), 'Z': ('#2a5a9a', '#18305a')}
CLOUD = {'V': ('#eef2ff', '#5a6290'), 'X': ('#ffffff', '#5a6290'), 'Z': ('#c4cbe8', '#5a6290')}


def egg(tones, ol, over=None, pal=None, under=None, no_ol='', ring=None):
    over, under = dict(over or {}), dict(under or {})
    if ring:
        for y, r in RING_UNDER.items():
            under.setdefault(y, r)
        for y, r in RING_OVER.items():
            over.setdefault(y, r)
    rows = []
    for y in range(16):
        r = list(EGG[y])
        for x, ch in enumerate(under.get(y, '.' * 16)):
            if ch != '.' and r[x] == '.':
                r[x] = ch
        for x, ch in enumerate(over.get(y, '.' * 16)):
            if ch != '.':
                r[x] = ch
        rows.append(''.join(r))
    p = ramp('1234', tones, ol)
    p['w'] = (mix(tones[3], WHITE, 0.6), ol)
    for k, v in (pal or {}).items():
        p[k] = v if isinstance(v, tuple) or k in no_ol else (v, ol)
    if ring:
        for k, v in (WATER if ring == 'water' else CLOUD).items():
            p.setdefault(k, v)
    return grid(rows, p, ol=True, no_ol=no_ol)


def spawn_eggs():
    E = {}
    # ---- the Sift's own creatures
    E['bulb'] = egg(['#3f5fae', '#5a80cf', '#78a5e3', '#a8c8f4'], '#1e2a6a', {
        0: '.....44..44.....',
        1: '.....43..43.....',
        2: '.....43..43.....',
        7: '.....hE..hE.....',
        8: '.....EE..EE.....',
        9: '....c......c....',
        10: '.......mm.......',
        14: '....ff....ff....',
    }, {'E': '#2f2777', 'h': '#ffffff', 'm': '#4a3a9f', 'c': '#f2a6d8', 'f': '#4a6ac0'})
    E['slumbler'] = egg(['#4a8aa8', '#6aaec4', '#8fd0dc', '#b4e4ec'], '#1e4a62', {
        5: '....iii..iii....',
        6: '..g.iEi..iEi.g..',
        7: '.gG.iii..iii.Gg.',
        8: '..g..........g..',
        9: '.gG.tMtMMtMt.Gg.',
        10: '....MmmmmmmM....',
        11: '.....MmmmmM.....',
    }, {'i': '#ffd66b', 'E': '#1a1d38', 'g': ('#f59ad0', '#6a2050'), 'G': ('#c85f9f', '#6a2050'), 't': '#fff8ec', 'M': '#9c2f55',
        'm': '#d9577f'})
    E['sifter'] = egg(['#0e5a7d', '#157e9f', '#1fa3c1', '#3ec0d6'], '#08304a', {
        4: '.....ee..ee.....',
        5: '.....eE..Ee.....',
        7: '...tvtvtvtvtv...',
        8: '...vvvvvvvvvv...',
        9: '..fvvvvvvvvvvf..',
        10: '..fvtvtvtvtvtf..',
        11: '...TTTTTTTTTU...',
        12: '....TTTTTTUU....',
        13: '.....TTTUUU.....',
    }, {'e': '#d9f6ff', 'E': '#0c1e5c', 't': '#eaf7ff', 'v': '#17328c', 'T': ('#f2cd98', '#5a3a1a'), 'U': ('#d7a46c', '#5a3a1a'),
        'f': '#0f6a8e'})
    # A2 Echoer: the redesigned Echoer - a pale egg, two swept horns with glowing tips, glowing eyes, flank runes
    E['enchoer'] = egg(['#a99fb8', '#c4bccf', '#d9d2e2', '#e9e4ef'], '#3a3050', {
        0: '....R......R....',
        1: '....hh....hh....',
        2: '.....hh..hh.....',
        5: '.....EE..EE.....',
        6: '.....EI..IE.....',
        8: '.......nn.......',
        10: '....r......r....',
        11: '...rRr....rRr...',
        12: '....r......r....',
    }, {'R': '#7ff7ff', 'h': ('#b9a6e6', '#3a3050'), 'E': '#8ffaff', 'I': '#ffffff', 'n': '#6e6190', 'r': '#3cc9dc'}, no_ol='rR')
    E['harmoner'] = egg(['#9a2a52', '#c43e66', '#e8577f', '#f88aa8'], '#4a1028', {
        0: '.......yy.......',
        1: '......yooy......',
        2: '......oooo......',
        5: '....hE....Eh....',
        6: '....EE....EE....',
        7: '......llll......',
        8: '......bbbb......',
        9: '......BBBB......',
        10: '.....cccccc.....',
        11: '.....cccccC.....',
        12: '......ccCC......',
    }, {'y': ('#ffe25a', '#6a2a10'), 'o': ('#ff8a3d', '#6a2a10'), 'E': '#1a1830', 'h': '#ffffff', 'l': '#ffe98a', 'b': '#ffd23f',
        'B': '#c99a1f', 'c': '#ffc4d4', 'C': '#f0a0b8'})
    E['dictator'] = egg(['#200912', '#2e0e1a', '#3a1322', '#55203a'], '#0c0408', {
        0: '...l........l...',
        1: '..hh........hh..',
        2: '...hH......Hh...',
        3: '....hH....Hh....',
        5: '..4.bb....bb.4..',
        6: '..4..vE..Ev..4..',
        7: '.....vv..vv.....',
        9: '....m......m....',
        10: '....mfmmmmfm....',
        11: '.....mmmmmm.....',
    }, {'l': ('#f4f0e5', '#3a3428'), 'h': ('#e3ddcc', '#3a3428'), 'H': ('#9e957c', '#3a3428'), 'b': '#0c0408', 'v': '#04080c',
        'E': '#2ef2e2', 'm': '#04080c', 'f': '#f4f0e5'})
    # ---- the new orchestra
    E['thumper'] = egg(['#1e5a2a', '#2e7a3a', '#3fa442', '#6ac85a'], '#0e2a16', {
        0: '.....ssssss.....',
        1: '....GGGGGGGG....',
        2: '....rlrrlrrR....',
        3: '....rrlrrlrR....',
        4: '....GGGGGGGg....',
        7: '....hE....Eh....',
        9: '.....m....m.....',
        10: '..f...mmmm...f..',
        11: '.ff..........ff.',
        12: '.f..cccccc....f.',
        13: '.....cccc.......',
    }, {'s': ('#f2e2c0', '#5a3a2a'), 'G': ('#f0c040', '#5a3410'), 'g': ('#b88a2a', '#5a3410'), 'r': ('#d03a3a', '#4a1018'),
        'R': ('#902030', '#4a1018'), 'l': ('#fff4e0', '#4a1018'), 'E': '#0e1a10', 'h': '#ffffff', 'm': '#0e2a16',
        'f': '#4a9a3a', 'c': '#d8d08a'})
    # S2: the Kazoo Fish and Fanfare Eel eggs are drawn with the other water creatures' in tools/fish_items.py
    E['sky_whale'] = egg(['#3a6ab0', '#5a94d8', '#82bcf0', '#b4dcff'], '#1a2a5a', {
        1: '.....h....h.....',
        2: '.....H....H.....',
        3: '......ffff......',
        4: '.....ff44ff.....',
        8: '....EE....EE....',
        9: '...p........p...',
        10: '......mmmm......',
        11: '.......mm.......',
    }, {'h': ('#fff4d6', '#5a4a2a'), 'H': ('#d8c89a', '#5a4a2a'), 'f': '#eef6ff', 'E': '#14204a', 'p': '#f2a6d8', 'm': '#14204a'},
        ring='cloud')
    # a black-and-teal centipede curled round a glowing sculk egg, its bone mandibles over the top
    E['sculk_parasite'] = egg(['#034150', '#05625d', '#0b7c78', '#1a9a94'], '#020a10', {
        0: '..........m..m..',
        1: '..........mhhm..',
        2: '...........ehe..',
        3: '............KK..',
        4: '......c...c.kkl.',
        5: '....k.......KK..',
        6: '..lKK....c..kkl.',
        7: '..kk........KK..',
        8: '.lKK........kkl.',
        9: '..kk..c.....KK..',
        10: '.lKK....c...kkl.',
        11: '...kk......KK...',
        12: '....KK....kk....',
        13: '...l..kkKK..l...',
        14: '......l..l......',
    }, {'m': '#d1d6b6', 'e': '#29dfeb', 'h': '#0d1217', 'K': '#2aa8ae', 'k': '#0d1217', 'l': '#0d1217', 'c': '#3ff5e6'})
    return {f'{k}_spawn_egg': v for k, v in E.items()}


# ============================================================================ mob effect + block sprites

def sculk_corruption_icon():
    """18x18 like every mob effect icon: a glowing sculk eye held in the curl of two tentacles,
    their tips hooked over it and glowing suckers down their insides."""
    rows = [
        '..................',
        '....tTT....TTt....',
        '...tT..T..T..Tt...',
        '..tT...t..t...Tt..',
        '..tT..........Tt..',
        '.tT...kkkkkk...Tt.',
        '.tT..kgGGGGgk..Tt.',
        '.ts.kgGWKKGGgk.st.',
        '.tT.kgGGKKGGgk.Tt.',
        '.tT..kgGGGGgk..Tt.',
        '.ts...kkkkkk...st.',
        '.tT............Tt.',
        '..tT..........Tt..',
        '..ts..........st..',
        '...tT........Tt...',
        '....tT......Tt....',
        '.....tt....tt.....',
        '..................',
    ]
    o = '#020a10'
    pal = {'t': ('#16222a', o), 'T': ('#05625d', o), 's': ('#29dfeb', o), 'k': ('#06141c', o), 'g': ('#0f8c99', o),
           'G': ('#3ff5e6', o), 'K': ('#041016', o), 'W': ('#e8fffc', o)}
    return grid(rows, pal, ol=True, size=(18, 18))


def hummingbloom():
    """A cross-model flower: a lavender trumpet with a golden throat on a stem with two leaves."""
    rows = [
        '................',
        '.....bccccb.....',
        '...bcdeeeedcb...',
        '..bcdyYYYYydcb..',
        '..abcdyooydcba..',
        '...abccddccba...',
        '.....bccdcb.....',
        '......bcdb......',
        '.......bc.......',
        '.......mM.......',
        '..lLL..s........',
        '...lLLss........',
        '.......s.LLl....',
        '.......sLLl.....',
        '.......s........',
        '.......s........',
    ]
    pal = {'a': '#5a3a9a', 'b': '#8a62c8', 'c': '#b08ae6', 'd': '#d4b8f8', 'e': '#f0e4ff', 'y': '#ffd65a', 'Y': '#fff2a8',
           'o': '#d89a2a', 'm': '#3f9d80', 'M': '#78d4a8', 's': '#3f9d80', 'l': '#3f9d80', 'L': '#78d4a8'}
    return grid(rows, pal)


# ============================================================================ registry

def all_items():
    out = {}
    # I1 items: the materials, food, books, Prism gear, slingshot, discs, lanterns and doors are drawn in tools/itemart.py
    for f in (glowing_slime_ball, warden_core, thick_hide, soul_chime):
        out[f.__name__] = f()
    for f in (conductors_staff, conga_drum, crane_flute, magic_strings, guitar, stomper_meat, stomper_egg):
        out[f.__name__] = f()
    out.update(spawn_eggs())
    out.update(__import__('songs').art())  # songs & instruments (agent D)
    import plants_h_art  # H: the three Pitcher soups (W1: Pitcher Nectar removed)
    out.update(plants_h_art.item_sprites())
    import siege  # the Thumper's arena: the cannonball
    out.update(siege.items())
    out.update(__import__('caravans').items())  # C: prism gem and armour, Caravan egg
    out.update(__import__('sea_art').items())  # sea & sky: fish meats, sculk bladder, sushi, Gobbler egg
    out.update(__import__('echoer_world').item_sprites())  # A2 Echoer: Soul Golem Core, Nib Dust, eggs
    out.update(__import__('sift_sniffer').item_sprites())  # E1 Sniffer & rot: the Sift Sniffer spawn egg
    out.update(__import__('swifter_art').item_sprites())  # A2 Swifter & White Forest: Swifter Fluff, Swifter egg
    out.update(__import__('cave_creatures').items())  # A4 cave creatures: Jailer and Sculkling eggs
    out.update(__import__('sculk_sea').item_sprites())  # CR3 Fish & Coral Organs: fish meats, sushi, fish buckets, eggs (replaces older art)
    out.update(__import__('slumbler').items())  # CR2: Slumbler gill, tadpole bucket, Slumbler and tadpole eggs (replace older art)
    out.update(__import__('sky_whale').items())  # CR2: a Sky Whale egg that matches the whale
    out.update(__import__('land_eggs').items())  # S1 land: Stomper, Sifter, Swifter, Harmoner and Sky Whale eggs (tools/land_eggs.py)
    out.update(__import__('dunes_creatures').items())  # P4-DESERT: dunes creature eggs, Camouflage Scale, Kerkorer Cloak
    return out


def effect_icons():
    return {'sculk_corruption': sculk_corruption_icon()}


def flower_textures():
    return {'hummingbloom': hummingbloom()}
