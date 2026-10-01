"""The three mini-bosses of the Sift - the Thumper, the Whistler and the Strummer - their little
ones, and the Conductor's Mask. Same DSL as mobs.py (see modelkit.py); every face is painted at
texel density (hd=True maps) with generated patterns: shell scutes, feather scallops, chitin
plates."""
from modelkit import Model
from mobs import SCULK, hd_rows

EXPR = ['blink', 'angry', 'hurt', 'dead']


def gen(w, h, fn):
    """A w x h texel map from fn(x, y) -> char."""
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def scutes(w, h, cw=12, ch=10, glow_centre=False):
    """Turtle shell plates: staggered rows of plates with dark seams, a growth ring inside each
    plate, a lit top-left rim and a darker lower-right one."""
    def fn(x, y):
        row = y // ch
        off = (cw // 2) * (row % 2)
        cx, cy = (x + off) % cw, y % ch
        if cy == 0 or cx == 0:
            return 'd'
        if cy == 1 or cx == 1:
            return 'l'
        if cy == ch - 1 or cx == cw - 1:
            return 'm'
        if (cy in (3, ch - 3) and 3 <= cx <= cw - 3) or (cx in (3, cw - 3) and 3 <= cy <= ch - 3):
            return 'r'
        if glow_centre and cx == cw // 2 and cy == ch // 2:
            return 'g'
        return '.'
    return gen(w, h, fn)


def marginals(w, h, step=6):
    """The rim of a shell seen from the side: a row of marginal plates."""
    def fn(x, y):
        if y == 0:
            return 'l'
        if y >= h - 2:
            return 'd'
        if x % step == 0:
            return 'd'
        if x % step == 1:
            return 'l'
        if y == h - 3 and x % step in (2, 3, 4):
            return 'm'
        return '.'
    return gen(w, h, fn)


def feathers(w, h, cell=6, tall=5, tips=None, trail=0):
    """Overlapping feather scallops (each a rounded 'U' with a lit crown). `tips`: a char painted
    on the last rows (dark wing tips). `trail`: rows at the bottom cut into ragged feather ends."""
    def fn(x, y):
        if trail and y >= h - trail:
            k = (x // 2) % 3
            if y >= h - trail + (0, 1, 2)[k] * trail // 3:
                return '_'
        row = y // tall
        off = (cell // 2) * (row % 2)
        cx, cy = (x + off) % cell, y % tall
        if tips and y >= h - max(trail, 2) - 4:
            return tips if not (cy == tall - 1 and cx != 0) else 'k'
        if cy == tall - 1 and 0 < cx < cell - 1:
            return 'd'
        if cx == 0 and cy >= tall // 2:
            return 'd'
        if cy == 0 and 1 < cx < cell - 1:
            return 'l'
        return '.'
    return gen(w, h, fn)


def primaries(w, h, sx, ink='i', gap='k', shaft='s'):
    """The long flight feathers of a wing hand, side by side along the outward axis: each one
    four texels wide with a lit shaft and a darker gap to its neighbour, a rounded tip, the
    longest in the middle of the hand; the coverts over their roots are white scallops."""
    import math
    n = max(1, w // 4)

    def fn(x, y):
        xo = x if sx > 0 else w - 1 - x
        f, fx = xo // 4, xo % 4
        length = int(h * 0.6 + h * 0.4 * math.sin(math.pi * (f + 0.7) / (n + 0.4)))
        if y >= length or (y == length - 1 and fx in (0, 3)):
            return '_'
        if y < h * 0.28:
            return 'd' if (y % 5 == 4 and 0 < (xo + 3 * (y // 5)) % 6 < 5) else ('l' if y % 5 == 0 else '.')
        if fx == 3:
            return gap
        if fx == 1:
            return shaft
        return ink
    return gen(w, h, fn)


def chitin(w, h, step=4):
    """Segmented plates: horizontal seams every `step` texels with a lit edge under each."""
    def fn(x, y):
        if y % step == 0:
            return 'd'
        if y % step == 1:
            return 'l'
        return '.'
    return gen(w, h, fn)


def compound(w, h):
    """A compound eye: a honeycomb of facets, brighter in the middle."""
    def fn(x, y):
        if (x + (y // 2) % 2) % 2 == 0 and y % 2 == 0:
            return 'f'
        cx, cy = abs(x - w / 2 + 0.5), abs(y - h / 2 + 0.5)
        return 'G' if cx + cy < min(w, h) / 3 else 'g'
    return gen(w, h, fn)


# =========================================================================== THE THUMPER (turtle)
# a 18 x 14 texel face on the front of the head: eyes high on the corners under heavy brows, a
# hooked beak in the middle
THUMPER_FACE = {
    'neutral': {2: '..WWWW......WWWW', 3: '..WEEW......WEEW', 4: '..WEhW......WhEW', 5: '...WW........WW', 8: '.......BB', 9: '......BBBB',
                10: '......bBBb', 11: '.......bb'},
    'blink': {4: '..dddd......dddd', 5: '...dd........dd', 8: '.......BB', 9: '......BBBB', 10: '......bBBb', 11: '.......bb'},
    'angry': {2: '..WW..........WW', 3: '..WEEW......WEEW', 4: '..WEEW......WEEW', 5: '...WW........WW', 8: '.......BB', 9: '......BBBB',
              10: '......bBBb', 11: '.......bb'},
    'hurt': {2: '..W..W......W..W', 3: '...WW........WW', 4: '..W..W......W..W', 8: '.......BB', 9: '......BBBB', 10: '......bBBb', 11: '.......bb'},
    'dead': {2: '..E..E......E..E', 3: '...EE........EE', 4: '...EE........EE', 5: '..E..E......E..E', 8: '.......BB', 9: '......BBBB',
             10: '......bBBb', 11: '.......bb'},
}


def thumper() -> Model:
    """The Thumper: a giant snapping turtle carrying a war drum strapped to the top of its shell.
    Plated domed shell with growth-ringed scutes and a jagged marginal rim, a pale plastron,
    columnar legs with claws, a heavy hooked beak under scowling brows, a stubby tail - and on its
    back the drum, laced and roped down, its skin veined with sculk that glows: its only weak
    point."""
    pal = dict(SCULK)
    pal.update({'shell': '#4f7a3a', 'shell_l': '#73a050', 'shell_d': '#2f4e26', 'seam': '#1c2e14', 'ring': '#3d6230',
                'skin': '#7d9a63', 'skin_l': '#a3bf84', 'skin_d': '#57703f', 'plas': '#e3cf8c', 'plas_l': '#f2e3ad', 'plas_d': '#b9a265',
                'drum': '#a8402e', 'drum_l': '#cd5f42', 'drum_d': '#6e2418', 'hide': '#efe0bf', 'hide_l': '#fff4d9', 'hide_d': '#cdb98e',
                'rope': '#d9b46a', 'rope_d': '#9c7a3a', 'brass': '#c9a24a', 'brass_l': '#ead27e', 'brass_d': '#8a6a2a',
                'beak': '#3a3a2c', 'beak_l': '#5a5a44', 'eye': '#16130e', 'claw': '#d8d2bc', 'white': '#f4efe0'})
    m = Model('thumper', (256, 128), pal, {'thumper': {}}, res=2, expressions=EXPR)
    shell_k = {'d': 'seam', 'l': 'shell_l', 'm': 'shell_d', 'r': 'ring', 'g': 'glow'}
    skin = dict(color='skin', pattern='mc', clusters=0.5, spots=0.15, accent='skin_d')
    body = m.part('body', pivot=(0, 14, 0))
    # the plastron and the shell: rim, dome and crown
    body.cube((-10, 1, -12), (20, 3, 24), color='plas', pattern='mc', clusters=0.3, faces={
        'down': dict(color='plas', pattern='mc', clusters=0.0, hd=True, map=scutes(40, 48, 20, 12), keys={'d': 'plas_d', 'l': 'plas_l', 'm': 'plas_d', 'r': 'plas'}),
    })
    body.cube((-12, -3, -14), (24, 4, 28), color='shell', pattern='mc', clusters=0.3, faces={
        **{f: dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=marginals(56 if f in ('east', 'west') else 48, 8), keys=shell_k)
           for f in ('north', 'south', 'east', 'west')},
        'up': dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=scutes(48, 56, 8, 8), keys=shell_k),
        'down': dict(color='shell_d', pattern='mc', clusters=0.0),
    })
    body.cube((-10, -8, -12), (20, 5, 24), color='shell', pattern='mc', clusters=0.3, faces={
        **{f: dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=scutes(48 if f in ('east', 'west') else 40, 10, 12, 10), keys=shell_k)
           for f in ('north', 'south', 'east', 'west')},
        'up': dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=scutes(40, 48, 12, 10), keys=shell_k),
    })
    body.cube((-7, -11, -9), (14, 3, 18), color='shell', pattern='mc', clusters=0.3, faces={
        **{f: dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=scutes(36 if f in ('east', 'west') else 28, 6, 9, 6), keys=shell_k)
           for f in ('north', 'south', 'east', 'west')},
        'up': dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=scutes(28, 36, 14, 12), keys=shell_k),
    })
    # spikes along the ridge of the shell
    for i, z in enumerate((-7, -2, 7)):
        body.cube((-1, -13, z - 1), (2, 2, 2), color='shell_l', pattern='mc', clusters=0.0, rim=False,
                  faces={'up': dict(color='claw', pattern='mc', clusters=0.0)})
    # the drum, roped down on top of the shell: its skin is the weak point and glows
    drum = body.part('drum', pivot=(0, -11, 1))
    lace = gen(24, 18, lambda x, y: 'c' if (x + y) % 8 in (0, 1) or (x - y) % 8 in (0, 1) else ('d' if y in (0, 17) else '.'))
    drum.cube((-6, -9, -6), (12, 9, 12), color='drum', pattern='mc', clusters=0.4, faces={
        f: dict(color='drum', pattern='mc', clusters=0.2, hd=True, map=lace, keys={'c': 'rope', 'd': 'drum_d'}) for f in ('north', 'south', 'east', 'west')
    })
    head_map = gen(26, 26, lambda x, y: 'B' if min(x, y, 25 - x, 25 - y) < 2 else (
        'v' if abs(x - 12.5) + abs(y - 12.5) in (9.0, 10.0) or (abs(x - 12.5) < 1 and abs(y - 12.5) < 9) or (abs(y - 12.5) < 1 and abs(x - 12.5) < 9)
        else 'g' if abs(x - 12.5) + abs(y - 12.5) < 3 else '.'))
    drum.cube((-6.5, -10, -6.5), (13, 1, 13), color='hide', pattern='mc', clusters=0.2, rim=False, faces={
        'up': dict(color='hide', pattern='mc', clusters=0.2, hd=True, map=head_map, keys={'B': 'brass', 'v': 'glow_d', 'g': 'glow'}, glow_keys='vg'),
        **{f: dict(color='brass', pattern='mc', clusters=0.0, rim=False) for f in ('north', 'south', 'east', 'west')},
    })
    drum.cube((-6.5, -1, -6.5), (13, 1, 13), color='brass', pattern='mc', clusters=0.0, rim=False)
    for sx in (1, -1):
        # ropes running from the drum down over the shell
        drum.cube((6.5 * sx - 0.5, -5, -0.75), (1, 8, 1.5), color='rope', pattern='mc', clusters=0.0, rim=False,
                  faces={f: dict(color='rope', pattern='mc', clusters=0.0, rim=False, hd=True, map=['dd', '..'] * 8, keys={'d': 'rope_d'})
                         for f in ('north', 'south', 'east', 'west')})
    for side, sx in (('left', 1), ('right', -1)):
        stick = drum.part(f'{side}_stick', pivot=(4 * sx, -8, 5.5), rot=(0.5, 0, 0.5 * sx))
        stick.cube((-0.5, -9, -0.5), (1, 9, 1), color='claw', pattern='mc', clusters=0.0, rim=False)
        stick.cube((-1, -11, -1), (2, 2, 2), color='drum_l', pattern='mc', clusters=0.0, rim=False)
    # neck, head and jaw
    neck = body.part('neck', pivot=(0, -1, -12))
    neck.cube((-3.5, -3.5, -6), (7, 7, 7), **skin, faces={'down': dict(color='plas', pattern='mc', clusters=0.3)})
    head = neck.part('head', pivot=(0, -1, -6))
    E = {'W': 'white', 'E': 'eye', 'h': 'white', 'B': 'beak', 'b': 'beak_l', 'd': 'skin_d'}
    head.cube((-4.5, -5, -8), (9, 7, 8), **skin, faces={
        'north': dict(**skin, hd=True, map=hd_rows(THUMPER_FACE['neutral'], 18, 14), keys=E,
                      expr={k: hd_rows(v, 18, 14) for k, v in THUMPER_FACE.items() if k != 'neutral'}),
        'up': dict(color='skin_d', pattern='mc', clusters=0.4, spots=0.3, accent='shell_d'),
    })
    # the hooked beak
    head.cube((-2, -1, -10), (4, 3, 2), color='beak', pattern='mc', clusters=0.0, rim=False, faces={'up': dict(color='beak_l', pattern='mc', clusters=0.0)})
    head.cube((-1, 2, -10), (2, 1, 1), color='beak', pattern='mc', clusters=0.0, rim=False)
    for side, sx in (('left', 1), ('right', -1)):
        brow = head.part(f'{side}_brow', pivot=(2.6 * sx, -4.2, -8))
        brow.cube((-2, -0.75, -1), (4, 1.5, 1.5), color='shell_d', pattern='mc', clusters=0.0, rim=False,
                  faces={'up': dict(color='shell', pattern='mc', clusters=0.0)})
    jaw = head.part('jaw', pivot=(0, 2, -1))
    jaw.cube((-4, 0, -7), (8, 2, 7), color='skin_d', pattern='mc', clusters=0.3, faces={
        'up': dict(color='void', pattern='mc', clusters=0.0, hd=True, map=['.' * 16] * 2 + ['.tttttttttttttt.'] + ['.' * 16] * 11, keys={'t': 'claw'}),
        'down': dict(color='plas', pattern='mc', clusters=0.3),
    })
    jaw.cube((-1, -1, -8), (2, 3, 1), color='beak', pattern='mc', clusters=0.0, rim=False)
    # four columns of legs, scaled, with claws
    for name, sx, sz in (('front_left', 1, -1), ('front_right', -1, -1), ('hind_left', 1, 1), ('hind_right', -1, 1)):
        leg = body.part(f'{name}_leg', pivot=(9 * sx, 2, 8.5 * sz))
        leg.cube((-3, -1, -3), (6, 9, 6), **skin, faces={'north': dict(**skin, hd=True, map=['.' * 12] * 14 + ['.c.cc.cc.c..', '.c.cc.cc.c..', '............', '............'],
                                                                     keys={'c': 'claw'})})
        leg.cube((-3.5, 6, -3.5), (7, 2, 7), color='skin_d', pattern='mc', clusters=0.3, faces={
            'north': dict(color='skin_d', pattern='mc', clusters=0.0, hd=True, map=['cc.cc.cc.cc.cc', 'cc.cc.cc.cc.cc', '..............', '..............'],
                          keys={'c': 'claw'})})
    tail = body.part('tail', pivot=(0, 0, 13))
    tail.cube((-1.5, -1, 0), (3, 3, 5), **skin)
    tail.cube((-1, -0.5, 5), (2, 2, 3), color='skin_d', pattern='mc', clusters=0.0, rim=False)
    return m


THUMPLING_FACE = {
    'neutral': {3: '.WW....WW', 4: '.WE....EW', 6: '....bb'},
    'blink': {4: '.dd....dd', 6: '....bb'},
    'angry': {2: '.W......W', 3: '..WW..WW', 4: '.WE....EW', 6: '....bb'},
    'hurt': {3: '.W.W..W.W', 4: '..W....W', 6: '....bb'},
    'dead': {3: '.E.E..E.E', 4: '..E....E', 6: '....bb'},
}


def thumpling() -> Model:
    """A Thumpling: the Thumper's hatchling, a round little turtle with a toy drum on its back
    and big shiny eyes."""
    pal = dict(SCULK)
    pal.update({'shell': '#5d8c42', 'shell_l': '#83b45c', 'shell_d': '#37592a', 'seam': '#223a18', 'ring': '#4a7536',
                'skin': '#93b070', 'skin_l': '#b6cf92', 'skin_d': '#6a8350', 'plas': '#ecd99a', 'drum': '#b84a32', 'drum_l': '#d86848',
                'drum_d': '#7a2a1a', 'hide': '#f2e6c8', 'rope': '#d9b46a', 'eye': '#16130e', 'white': '#ffffff', 'beak': '#4a4a38'})
    m = Model('thumpling', (64, 64), pal, {'thumpling': {}}, res=2, expressions=EXPR)
    shell_k = {'d': 'seam', 'l': 'shell_l', 'm': 'shell_d', 'r': 'ring', 'g': 'glow'}
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-3.5, 0, -4), (7, 1.5, 8), color='plas', pattern='mc', clusters=0.2, rim=False)
    body.cube((-4, -3, -4.5), (8, 3, 9), color='shell', pattern='mc', clusters=0.0, faces={
        'up': dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=scutes(16, 18, 8, 6), keys=shell_k),
        **{f: dict(color='shell', pattern='mc', clusters=0.0, hd=True, map=marginals(18 if f in ('east', 'west') else 16, 6, 4), keys=shell_k)
           for f in ('north', 'south', 'east', 'west')},
    })
    drum = body.part('drum', pivot=(0, -3, 0.5))
    drum.cube((-2, -3, -2), (4, 3, 4), color='drum', pattern='mc', clusters=0.0, faces={
        f: dict(color='drum', pattern='mc', clusters=0.0, hd=True, map=['dddddddd', 'c..cc..c', '.cc..cc.', 'c..cc..c', '.cc..cc.', 'dddddddd'],
                keys={'c': 'rope', 'd': 'drum_d'}) for f in ('north', 'south', 'east', 'west')})
    drum.cube((-2.25, -3.5, -2.25), (4.5, 0.5, 4.5), color='hide', pattern='mc', clusters=0.0, rim=False,
              faces={'up': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=['.........', '.........', '.........', '....g....', '...ggg...', '....g....'],
                                keys={'g': 'glow'}, glow_keys='g')})
    head = body.part('head', pivot=(0, -1, -4.5))
    head.cube((-2.5, -3, -4), (5, 4, 4), color='skin', pattern='mc', clusters=0.2, faces={
        'north': dict(color='skin', pattern='mc', clusters=0.0, hd=True, map=hd_rows(THUMPLING_FACE['neutral'], 10, 8),
                      keys={'W': 'white', 'E': 'eye', 'b': 'beak', 'd': 'skin_d'}, expr={k: hd_rows(v, 10, 8) for k, v in THUMPLING_FACE.items() if k != 'neutral'}),
    })
    for name, sx, sz in (('front_left', 1, -1), ('front_right', -1, -1), ('hind_left', 1, 1), ('hind_right', -1, 1)):
        leg = body.part(f'{name}_leg', pivot=(3 * sx, 0.5, 3 * sz))
        leg.cube((-1, 0, -1), (2, 2.5, 2), color='skin', pattern='mc', clusters=0.0, rim=False, faces={'down': dict(color='skin_d', pattern='mc', clusters=0.0)})
    tail = body.part('tail', pivot=(0, -0.5, 4.5))
    tail.cube((-0.5, -0.5, 0), (1, 1, 2), color='skin_d', pattern='mc', clusters=0.0, rim=False)
    return m


# =========================================================================== THE WHISTLER (crane)
CRANE_EYE = {
    'neutral': {1: '..rr..', 2: '.rEEr.', 3: '.rEhr.', 4: '..rr..'},
    'blink': {3: '.kkkk.', 4: '..rr..'},
    'angry': {1: 'kk....', 2: '.kEEr.', 3: '.rEEr.', 4: '..rr..'},
    'hurt': {1: '.k..k.', 2: '..kk..', 3: '.k..k.'},
    'dead': {1: '.E..E.', 2: '..EE..', 3: '.E..E.'},
}


def whistler() -> Model:
    """The Whistler: a giant red-crowned crane of the sculk. Snow-white plumage in overlapping
    scallops, black secondaries and a black throat, a scarlet crown, a long golden beak that IS a
    flute (finger holes along the top, a mouthpiece at the base), a ridge of flute-pipe spines
    along its back glowing with souls, long stilt legs, and two enormous two-jointed wings that
    trail soul light from their black tips."""
    pal = dict(SCULK)
    pal.update({'plume': '#f2f1ea', 'plume_l': '#ffffff', 'plume_d': '#c6c9d6', 'ink': '#22232e', 'ink_l': '#3a3c4c', 'ink_d': '#121218',
                'crown': '#d6323a', 'crown_l': '#f0545a', 'flute': '#d9b25a', 'flute_l': '#f2d488', 'flute_d': '#9c7a32', 'hole': '#3a2a14',
                'soul': '#4ff0ff', 'soul_d': '#1aa7c9', 'leg': '#3a3a48', 'leg_l': '#55556a', 'eye': '#120a06', 'white': '#ffffff'})
    m = Model('whistler', (128, 128), pal, {'whistler': {}}, res=2, expressions=EXPR)
    fk = {'d': 'plume_d', 'l': 'plume_l', 'k': 'ink_d', 'i': 'ink', '_': '_'}
    body = m.part('body', pivot=(0, 6, 0))
    body.cube((-4, -4, -7), (8, 8, 14), color='plume', pattern='mc', clusters=0.0, faces={
        **{f: dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(28, 16), keys=fk) for f in ('east', 'west')},
        'up': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(16, 28), keys=fk),
        'north': dict(color='ink', pattern='mc', clusters=0.0, hd=True, map=feathers(16, 16), keys={'d': 'ink_d', 'l': 'ink_l'}),
        'down': dict(color='plume_d', pattern='mc', clusters=0.0, hd=True, map=feathers(16, 28), keys=fk),
        'south': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(16, 16), keys=fk),
    })
    # flute-pipe spines along the back, tallest in the middle, each with glowing finger holes
    spines = body.part('spines', pivot=(0, -4, 0))
    for i, (z, h) in enumerate(((-4.5, 5), (-2, 8), (0.5, 10), (3, 8), (5.5, 5))):
        sp = spines.part(f'spine_{i}', pivot=(0, 0, z), rot=(0.55, 0, 0))
        hole_rows = ['..', 'oo', '..', '..'] * (h // 2)
        sp.cube((-0.75, -h, -0.75), (1.5, h, 1.5), color='flute', pattern='mc', clusters=0.0, rim=False, faces={
            **{f: dict(color='flute', pattern='mc', clusters=0.0, rim=False, hd=True, map=(['ll', '..'] + hole_rows)[:h * 2], keys={'o': 'soul', 'l': 'flute_l'},
                       glow_keys='o') for f in ('north', 'south', 'east', 'west')},
            'up': dict(color='soul', pattern='mc', clusters=0.0, glow=True),
        })
    # the long neck: black throat in front, white behind
    neck = body.part('neck', pivot=(0, -2, -6.5), rot=(-0.35, 0, 0))
    nk = {'d': 'plume_d', 'l': 'plume_l'}
    neck.cube((-1.5, -9, -1.5), (3, 9, 3), color='plume', pattern='mc', clusters=0.0, faces={
        'north': dict(color='ink', pattern='mc', clusters=0.0, hd=True, map=feathers(6, 18, 3, 3), keys={'d': 'ink_d', 'l': 'ink_l'}),
        'east': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(6, 18, 3, 3), keys=nk),
        'west': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(6, 18, 3, 3), keys=nk),
    })
    neck2 = neck.part('neck_upper', pivot=(0, -9, 0), rot=(0.5, 0, 0))
    neck2.cube((-1.25, -8, -1.25), (2.5, 8, 2.5), color='ink', pattern='mc', clusters=0.0, faces={
        'south': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(5, 16, 3, 3), keys=nk),
        'east': dict(color='ink', pattern='mc', clusters=0.0, hd=True, map=feathers(5, 16, 3, 3), keys={'d': 'ink_d', 'l': 'ink_l'}),
        'west': dict(color='ink', pattern='mc', clusters=0.0, hd=True, map=feathers(5, 16, 3, 3), keys={'d': 'ink_d', 'l': 'ink_l'}),
    })
    head = neck2.part('head', pivot=(0, -8, 0), rot=(-0.15, 0, 0))
    EK = {'r': 'plume_l', 'E': 'eye', 'h': 'white', 'k': 'ink'}
    head.cube((-2, -3.5, -3.5), (4, 4, 5), color='ink', pattern='mc', clusters=0.0, faces={
        'east': dict(color='ink', pattern='mc', clusters=0.0, hd=True, map=hd_rows({k: '..' + v for k, v in CRANE_EYE['neutral'].items()}, 10, 8), keys=EK,
                     expr={e: hd_rows({k: '..' + v for k, v in r.items()}, 10, 8) for e, r in CRANE_EYE.items() if e != 'neutral'}),
        'west': dict(color='ink', pattern='mc', clusters=0.0, hd=True, map=hd_rows({k: v[::-1] + '..' for k, v in CRANE_EYE['neutral'].items()}, 10, 8),
                     keys=EK, expr={e: hd_rows({k: v[::-1] + '..' for k, v in r.items()}, 10, 8) for e, r in CRANE_EYE.items() if e != 'neutral'}),
        'south': dict(color='plume', pattern='mc', clusters=0.0),
    })
    # the scarlet crown
    head.cube((-1.5, -4, -2.5), (3, 1, 3), color='crown', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='crown', pattern='mc', clusters=0.0, hd=True, map=['.ll...', 'l.....', '......', '...l..', '......', '......'], keys={'l': 'crown_l'})})
    # the flute beak: a mouthpiece ring at the base, six glowing finger holes along the top
    beak = head.part('beak', pivot=(0, -1.75, -3.5))
    holes = ['...'] + sum([['.o.', '...', '...'] for _ in range(6)], []) + ['...'] * 5
    beak.cube((-0.75, -0.75, -13), (1.5, 1.5, 13), color='flute', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='flute', pattern='mc', clusters=0.0, rim=False, hd=True, map=['lll'] * 26, keys={'l': 'flute_l'}),
        'east': dict(color='flute', pattern='mc', clusters=0.0, rim=False, hd=True, map=['l' * 26, '.' * 26, 'd' * 26], keys={'l': 'flute_l', 'd': 'flute_d'}),
        'west': dict(color='flute', pattern='mc', clusters=0.0, rim=False, hd=True, map=['l' * 26, '.' * 26, 'd' * 26], keys={'l': 'flute_l', 'd': 'flute_d'}),
    })
    beak.cube((-0.5, -1.0, -11), (1, 0.25, 9), color='hole', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='flute_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['.o'] * 9, keys={'o': 'soul'}, glow_keys='o')})
    beak.cube((-1.0, -1.0, -1.5), (2, 2, 1.5), color='flute_d', pattern='mc', clusters=0.0, rim=False)
    jaw = beak.part('jaw', pivot=(0, 0.75, 0))
    jaw.cube((-0.6, 0, -12), (1.2, 0.6, 12), color='flute_d', pattern='mc', clusters=0.0, rim=False)
    # the great wings: an inner arm of coverts and secondaries, and an outer hand of long black
    # primaries; both fold back along the body when it lands
    for side, sx in (('left', 1), ('right', -1)):
        wing = body.part(f'{side}_wing', pivot=(4 * sx, -3, -4))
        wing.cube((0 if sx > 0 else -12, -0.5, 0), (12, 1, 12), color='plume', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(24, 24, 6, 5, tips='i', trail=3), keys=fk),
            'down': dict(color='plume_d', pattern='mc', clusters=0.0, hd=True, map=feathers(24, 24, 6, 5, tips='i', trail=3), keys=fk),
        })
        tip = wing.part(f'{side}_wing_tip', pivot=(12 * sx, 0, 0))
        prim = primaries(28, 26, sx)
        tip.cube((0 if sx > 0 else -14, -0.5, 0), (14, 1, 13), color='plume', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=prim, keys={**fk, 'i': 'ink', 'k': 'ink_d', 's': 'ink_l'}),
            'down': dict(color='plume_d', pattern='mc', clusters=0.0, hd=True, map=prim, keys={**fk, 'i': 'ink_l', 'k': 'ink', 's': 'plume_d'}),
        })
        # soul light gathered at the tips of the primaries
        tip.cube((12 * sx - (1 if sx > 0 else 0), -0.75, 9), (1, 1.5, 2), color='soul', pattern='mc', clusters=0.0, rim=False, glow=True)
    tail = body.part('tail', pivot=(0, -2, 6.5), rot=(-0.2, 0, 0))
    tail.cube((-3.5, -1, 0), (7, 2, 7), color='ink', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='ink', pattern='mc', clusters=0.0, hd=True, map=feathers(14, 14, 4, 4, trail=3), keys={'d': 'ink_d', 'l': 'ink_l', '_': '_'})})
    for side, sx in (('left', 1), ('right', -1)):
        thigh = m.part(f'{side}_leg', pivot=(2 * sx, 9, 1))
        thigh.cube((-1, 0, -1), (2, 5, 2), color='plume_d', pattern='mc', clusters=0.0, rim=False)
        shin = thigh.part(f'{side}_shin', pivot=(0, 5, 0))
        shin.cube((-0.5, 0, -0.5), (1, 9.5, 1), color='leg', pattern='mc', clusters=0.0, rim=False)
        foot = shin.part(f'{side}_foot', pivot=(0, 9.5, 0))
        foot.cube((-1.5, 0, -3), (3, 0.5, 4), color='leg_l', pattern='mc', clusters=0.0, rim=False)
    return m


def whistling() -> Model:
    """A Whistling: a crane chick - a ball of white fluff with a red cap, stubby wings and a tiny
    golden flute for a beak."""
    pal = dict(SCULK)
    pal.update({'plume': '#f4f2ea', 'plume_l': '#ffffff', 'plume_d': '#cfd2de', 'ink': '#2a2b36', 'ink_l': '#44465a', 'crown': '#e0424a',
                'flute': '#e0bb62', 'flute_l': '#f6dc92', 'eye': '#120a06', 'white': '#ffffff', 'leg': '#46465a', 'soul': '#4ff0ff'})
    m = Model('whistling', (64, 64), pal, {'whistling': {}}, res=2, expressions=EXPR)
    fk = {'d': 'plume_d', 'l': 'plume_l'}
    body = m.part('body', pivot=(0, 19, 0))
    body.cube((-3, -6, -3), (6, 6, 6), color='plume', pattern='mc', clusters=0.0, faces={
        f: dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(12, 12, 4, 3), keys=fk, fringe=1) for f in ('north', 'south', 'east', 'west', 'up')})
    head = body.part('head', pivot=(0, -6, -0.5))
    face = {
        'neutral': {2: '..ww....ww', 3: '..wE....Ew', 4: '..EE....EE'},
        'blink': {4: '..ii....ii'},
        'angry': {1: '.i......i.', 2: '..ii..ii..', 3: '..wE....Ew', 4: '..EE....EE'},
        'hurt': {2: '..i.i..i.i', 3: '...i....i.', 4: '..i.i..i.i'},
        'dead': {2: '..E.E..E.E', 3: '...E....E.', 4: '..E.E..E.E'},
    }
    head.cube((-2.5, -5, -2.5), (5, 5, 5), color='plume', pattern='mc', clusters=0.0, faces={
        'north': dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=hd_rows(face['neutral'], 10, 10), keys={'w': 'white', 'E': 'eye', 'i': 'ink'},
                      expr={k: hd_rows(v, 10, 10) for k, v in face.items() if k != 'neutral'}),
        'up': dict(color='crown', pattern='mc', clusters=0.0),
    })
    beak = head.part('beak', pivot=(0, -2, -2.5))
    beak.cube((-0.5, -0.5, -3), (1, 1, 3), color='flute', pattern='mc', clusters=0.0, rim=False,
              faces={'up': dict(color='flute_l', pattern='mc', clusters=0.0, hd=True, map=['..', 'o.', '..', 'o.', '..', '..'], keys={'o': 'soul'}, glow_keys='o')})
    for side, sx in (('left', 1), ('right', -1)):
        wing = body.part(f'{side}_wing', pivot=(3 * sx, -5, -1))
        wing.cube((0 if sx > 0 else -1, 0, 0), (1, 4, 4), color='plume_d', pattern='mc', clusters=0.0, rim=False, faces={
            ('east' if sx > 0 else 'west'): dict(color='plume', pattern='mc', clusters=0.0, hd=True, map=feathers(8, 8, 4, 3, tips='i', trail=2),
                                                 keys={**fk, 'i': 'ink', 'k': 'ink_l', '_': '_'})})
        leg = m.part(f'{side}_leg', pivot=(1.2 * sx, 19, 0))
        leg.cube((-0.5, 0, -0.5), (1, 4.5, 1), color='leg', pattern='mc', clusters=0.0, rim=False)
        leg.cube((-1, 4.5, -2), (2, 0.5, 2.5), color='leg', pattern='mc', clusters=0.0, rim=False)
    return m


# =========================================================================== THE STRUMMER (mantis on spider)
SPIDER_EYES = {
    # 16 x 10 texels on the front of the spider's head: eight glowing eyes in two rows and fangs
    'neutral': {1: '..gg..GGGG..gg..', 2: '..gg..GGGG..gg..', 4: '.g..g......g..g.', 7: '....ff....ff....', 8: '....f......f....'},
    'blink': {2: '..dd..dddd..dd..', 4: '.d..d......d..d.', 7: '....ff....ff....', 8: '....f......f....'},
    'angry': {0: '.d............d.', 1: '..gd..GGGG..dg..', 2: '..gg..GGGG..gg..', 4: '.g..g......g..g.', 6: '....ff....ff....', 7: '....f......f....',
              8: '....f......f....'},
    'hurt': {1: '..g...G..G...g..', 2: '...g...GG...g...', 4: '.g..g......g..g.', 7: '.....ff..ff.....'},
    'dead': {1: '..d.d.d..d.d.d..', 2: '...d...dd...d...', 3: '..d.d.d..d.d.d..', 7: '.....ff..ff.....'},
}
MANTIS_FACE = {
    # 10 x 6 texels under the eyes: the mouth parts
    'neutral': {1: '...pppp...', 2: '....pp....', 3: '...p..p...'},
    'blink': {1: '...pppp...', 2: '....pp....', 3: '...p..p...'},
    'angry': {1: '..pppppp..', 2: '..p.pp.p..', 3: '..p....p..', 4: '.p......p.'},
    'hurt': {1: '....pp....', 2: '...p..p...'},
    'dead': {1: '...p..p...', 2: '....pp....', 3: '...p..p...'},
}


def strummer() -> Model:
    """The Strummer: a giant orchid mantis riding a giant spider. The spider is the instrument -
    its abdomen is painted like the body of a guitar, sound hole and all, and glowing strings run
    from it up to the mantis's two scythe hands. The mantis sits upright on the spider's back, a
    tall green body of chitin plates, a triangular head with two huge compound eyes and long
    antennae, folded leaf wings, and raptorial arms it strums and saws with."""
    pal = dict(SCULK)
    pal.update({'chitin': '#7fc04a', 'chitin_l': '#a6dc6a', 'chitin_d': '#4f8a2e', 'leaf': '#9bd45a', 'leaf_d': '#5e9a34', 'pink': '#e88ac8',
                'spider': '#3a2448', 'spider_l': '#523466', 'spider_d': '#24142e', 'mark': '#c46cff', 'mark_d': '#7a3ab8',
                'string': '#7ff7ff', 'fang': '#e8e0d0', 'eye_f': '#3a5a10', 'eye': '#e8f05a', 'eye_l': '#fbffa8', 'mouth': '#2a3a14',
                'wood': '#8a5a2e', 'wood_l': '#b07a42'})
    m = Model('strummer', (128, 128), pal, {'strummer': {}}, res=2, expressions=EXPR)
    sp = dict(color='spider', pattern='mc', clusters=0.5, streaks=0.4)
    spider = m.part('spider', pivot=(0, 17.5, 0))
    spider.cube((-5, -3, -5), (10, 6, 10), **sp, faces={'up': dict(color='spider', pattern='mc', clusters=0.0, hd=True, map=chitin(20, 20, 5),
                                                                   keys={'d': 'spider_d', 'l': 'spider_l'})})
    head = spider.part('spider_head', pivot=(0, 0, -5))
    head.cube((-4, -2.5, -5), (8, 5, 5), **sp, faces={
        'north': dict(color='spider', pattern='mc', clusters=0.0, hd=True, map=SPIDER_EYES_ROWS('neutral'), keys={'g': 'mark', 'G': 'glow', 'f': 'fang', 'd': 'spider_d'},
                      glow_keys='gG', expr={k: SPIDER_EYES_ROWS(k) for k in SPIDER_EYES if k != 'neutral'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        fang = head.part(f'{side}_fang', pivot=(1.5 * sx, 2, -5))
        fang.cube((-0.75, 0, -0.75), (1.5, 3, 1.5), color='fang', pattern='mc', clusters=0.0, rim=False)
    # the abdomen: a guitar body - a sound hole ringed in glowing violet, strings over it
    abd = spider.part('abdomen', pivot=(0, -1, 4.5), rot=(-0.15, 0, 0))
    gtr = gen(28, 30, lambda x, y: 'r' if 8.5 < ((x - 13.5) ** 2 + (y - 12.5) ** 2) ** 0.5 < 10.5 else (
        'h' if ((x - 13.5) ** 2 + (y - 12.5) ** 2) ** 0.5 <= 8.5 else ('s' if x in (10, 12, 15, 17) else ('b' if y in (25, 26) and 6 < x < 21 else '.'))))
    abd.cube((-7, -7, 0), (14, 10, 15), **sp, faces={
        'up': dict(color='spider_l', pattern='mc', clusters=0.0, hd=True, map=gtr, keys={'r': 'mark', 'h': 'void', 's': 'string', 'b': 'fang'}, glow_keys='rs'),
        'south': dict(color='spider', pattern='mc', clusters=0.3, hd=True, map=hd_rows({6: '.' * 10 + 'mmmmmmmm', 7: '.' * 9 + 'm' + '.' * 8 + 'm', 9: '.' * 12 + 'mmmm'}, 28, 20),
                      keys={'m': 'mark'}, glow_keys='m'),
        'east': dict(color='spider', pattern='mc', clusters=0.3, hd=True, map=gen(30, 20, lambda x, y: 'm' if (x + y) % 9 == 0 and 4 < y < 16 else '.'), keys={'m': 'mark_d'}),
        'west': dict(color='spider', pattern='mc', clusters=0.3, hd=True, map=gen(30, 20, lambda x, y: 'm' if (x - y) % 9 == 0 and 4 < y < 16 else '.'), keys={'m': 'mark_d'}),
    })
    abd.cube((-1.5, -2, 15), (3, 3, 2), color='mark_d', pattern='mc', clusters=0.0, rim=False, faces={'south': dict(color='mark', pattern='mc', clusters=0.0, glow=True)})
    # eight legs: a raised femur and a long shin reaching down to the ground
    for i, z in enumerate((-3.5, -1.2, 1.2, 3.5)):
        for side, sx in (('left', 1), ('right', -1)):
            yaw = (0.55, 0.2, -0.2, -0.55)[i] * sx
            leg = spider.part(f'{side}_leg_{i}', pivot=(4.5 * sx, 0, z), rot=(0, yaw, -0.75 * sx))
            leg.cube((0 if sx > 0 else -10, -0.75, -0.75), (10, 1.5, 1.5), color='spider', pattern='mc', clusters=0.0, rim=False,
                     faces={'up': dict(color='spider_l', pattern='mc', clusters=0.0, hd=True, map=['m...' * 5, '....' * 5, '....' * 5], keys={'m': 'mark'}, glow_keys='m')})
            shin = leg.part(f'{side}_shin_{i}', pivot=(10 * sx, 0, 0), rot=(0, 0, 1.75 * sx))
            shin.cube((0 if sx > 0 else -15, -0.6, -0.6), (15, 1.2, 1.2), color='spider_d', pattern='mc', clusters=0.0, rim=False,
                      faces={'up': dict(color='spider', pattern='mc', clusters=0.0, hd=True, map=['d.' * 15, '..' * 15], keys={'d': 'spider_l'})})
    # the mantis rider
    mantis = spider.part('mantis', pivot=(0, -3, -1.5), rot=(0.08, 0, 0))
    ch = dict(color='chitin', pattern='mc', clusters=0.0)
    ck = {'d': 'chitin_d', 'l': 'chitin_l'}
    mantis.cube((-2, -11, -2), (4, 11, 4), **ch, faces={f: dict(**ch, hd=True, map=chitin(8, 22, 4), keys=ck) for f in ('north', 'south', 'east', 'west')})
    mantis.cube((-2.5, -4, -2.5), (5, 4, 5), **ch, faces={'north': dict(**ch, hd=True, map=chitin(10, 8, 3), keys=ck)})
    for side, sx in (('left', 1), ('right', -1)):
        # folded leaf wings, pink edged
        w = mantis.part(f'{side}_mantis_wing', pivot=(1.5 * sx, -9, 2), rot=(0.35, 0.2 * sx, 0.12 * sx))
        w.cube((-1.5, 0, 0), (3, 12, 0), color='leaf', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='leaf', pattern='mc', clusters=0.0, rim=False, hd=True, map=gen(6, 24, lambda x, y: 'p' if x in (0, 5) or y == 23 else (
                'v' if x == 2 or (y % 5 == 0 and x > 2) else '.')), keys={'p': 'pink', 'v': 'leaf_d'}) for f in ('north', 'south')})
    mhead = mantis.part('mantis_head', pivot=(0, -11, -0.5))
    mhead.cube((-2.5, -3.5, -2.5), (5, 3.5, 3), **ch, faces={
        'north': dict(**ch, hd=True, map=hd_rows(MANTIS_FACE['neutral'], 10, 7), keys={'p': 'mouth'}, expr={k: hd_rows(v, 10, 7) for k, v in MANTIS_FACE.items() if k != 'neutral'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        eye = mhead.part(f'{side}_mantis_eye', pivot=(2.5 * sx, -3, -1))
        eye.cube((-1.25, -1.5, -1.5), (2.5, 3, 3), color='eye', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='eye', pattern='mc', clusters=0.0, rim=False, hd=True, map=compound(6 if f in ('north', 'south') else 6, 6), keys={'f': 'eye_f', 'g': 'eye', 'G': 'eye_l'},
                    glow_keys='gG') for f in ('north', 'south', 'east', 'west', 'up')})
        ant = mhead.part(f'{side}_antenna', pivot=(0.8 * sx, -3.5, -2), rot=(-0.6, 0, 0.25 * sx))
        ant.cube((-0.25, -9, -0.25), (0.5, 9, 0.5), color='chitin_d', pattern='mc', clusters=0.0, rim=False,
                 faces={'up': dict(color='mark', pattern='mc', clusters=0.0, glow=True)})
    # the raptorial arms: coxa, a spiked femur folding up, and the hooked tibia - the hand; three
    # glowing strings hang from each hand down to the spider
    for side, sx in (('left', 1), ('right', -1)):
        arm = mantis.part(f'{side}_arm', pivot=(2.2 * sx, -9.5, -1.5), rot=(-0.7, 0, -0.15 * sx))
        arm.cube((-0.75, 0, -0.75), (1.5, 5, 1.5), **ch)
        fem = arm.part(f'{side}_femur', pivot=(0, 5, 0), rot=(-1.4, 0, 0))
        fem.cube((-0.75, 0, -0.75), (1.5, 7, 1.5), **ch, faces={'north': dict(**ch, hd=True, map=['ll.'] + ['f..', '...'] * 6 + ['...'], keys={'l': 'chitin_l', 'f': 'fang'})})
        for k in range(3):
            fem.cube((-0.25, 1.5 + k * 2, -1.5), (0.5, 1, 0.75), color='fang', pattern='mc', clusters=0.0, rim=False)
        tib = fem.part(f'{side}_hand', pivot=(0, 7, 0), rot=(2.5, 0, 0))
        tib.cube((-0.5, 0, -0.5), (1, 5, 1), color='chitin_l', pattern='mc', clusters=0.0, rim=False)
        tib.cube((-0.5, 4.5, -1.5), (1, 1, 1.5), color='fang', pattern='mc', clusters=0.0, rim=False)
        strings = tib.part(f'{side}_strings', pivot=(0, 4.5, 0), rot=(-1.1, 0, 0))
        strings.cube((0, 0, -1.5), (0, 12, 3), color='string', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='string', pattern='mc', clusters=0.0, rim=False, hd=True, map=['s_s_s_'[::(1 if f == 'east' else -1)]] * 24,
                    keys={'s': 'string'}, glow_keys='s') for f in ('east', 'west')})
    return m


def SPIDER_EYES_ROWS(k):
    return hd_rows(SPIDER_EYES[k], 16, 10)


def strumling() -> Model:
    """A Strumling: a spiderling with a glowing string-pattern on its back and a lot of eyes."""
    pal = dict(SCULK)
    pal.update({'spider': '#4a2c5a', 'spider_l': '#653f78', 'spider_d': '#2a1636', 'mark': '#c46cff', 'string': '#7ff7ff', 'fang': '#e8e0d0'})
    m = Model('strumling', (64, 64), pal, {'strumling': {}}, res=2, expressions=EXPR)
    body = m.part('body', pivot=(0, 20, 0))
    body.cube((-2.5, -2, -3), (5, 4, 5), color='spider', pattern='mc', clusters=0.3, streaks=0.4, faces={
        'north': dict(color='spider', pattern='mc', clusters=0.0, hd=True, map=hd_rows(STRUM_EYES['neutral'], 10, 8), keys={'g': 'mark', 'G': 'glow', 'f': 'fang', 'd': 'spider_d'},
                      glow_keys='gG', expr={k: hd_rows(v, 10, 8) for k, v in STRUM_EYES.items() if k != 'neutral'})})
    abd = body.part('abdomen', pivot=(0, -0.5, 2))
    abd.cube((-3, -3, 0), (6, 5, 6), color='spider', pattern='mc', clusters=0.3, streaks=0.4, faces={
        'up': dict(color='spider_l', pattern='mc', clusters=0.0, hd=True, map=gen(12, 12, lambda x, y: 's' if x in (4, 7) else ('r' if (x - 5.5) ** 2 + (y - 5.5) ** 2 < 6 else '.')),
                   keys={'s': 'string', 'r': 'mark'}, glow_keys='sr')})
    for i, z in enumerate((-2, -0.7, 0.6, 1.9)):
        for side, sx in (('left', 1), ('right', -1)):
            leg = body.part(f'{side}_leg_{i}', pivot=(2.5 * sx, 0, z), rot=(0, (0.5, 0.18, -0.18, -0.5)[i] * sx, 0.55 * sx))
            leg.cube((0 if sx > 0 else -6, -0.5, -0.5), (6, 1, 1), color='spider_d', pattern='mc', clusters=0.0, rim=False)
    return m


STRUM_EYES = {
    'neutral': {1: '..g.GG.g..', 2: '.g..GG..g.', 5: '...f..f...', 6: '...f..f...'},
    'blink': {2: '.d..dd..d.', 5: '...f..f...', 6: '...f..f...'},
    'angry': {0: '.d......d.', 1: '..gdGGdg..', 2: '.g..GG..g.', 5: '...ff.ff..', 6: '...f..f...'},
    'hurt': {1: '..g....g..', 2: '...gGGg...', 5: '....ff....'},
    'dead': {1: '..d.dd.d..', 2: '.d.d..d.d.', 5: '....ff....'},
}


# =========================================================================== THE CONDUCTOR'S MASK
def conductor_mask() -> Model:
    """The Conductor's Mask: a porcelain devil mask cracked with sculk, horned and crowned, with
    long ribbons trailing from it - the shape the Conductor takes before he takes his own."""
    pal = dict(SCULK)
    pal.update({'porc': '#ece6da', 'porc_l': '#fffaf0', 'porc_d': '#b8b0a2', 'crack': '#2ef2e2', 'gold': '#d4b25a', 'gold_l': '#f2d88a',
                'ribbon': '#4a1640', 'ribbon_l': '#6a2a5e', 'ribbon_d': '#2a0a24', 'horn': '#2a2030', 'horn_l': '#3e3046'})
    m = Model('conductor_mask', (64, 64), pal, {'conductor_mask': {}}, res=2)
    mask = m.part('mask', pivot=(0, 12, 0))
    face = gen(20, 28, lambda x, y: (
        'v' if (y in range(9, 13) and (3 <= x <= 7 or 12 <= x <= 16) and abs(y - 10.5) + abs(x - (5 if x < 10 else 14)) * 0.6 < 2.6) else
        'g' if (y in (10, 11) and x in (5, 14)) else
        'm' if (y == 20 and 4 <= x <= 15) or (y == 21 and x in (4, 6, 8, 11, 13, 15)) else
        'c' if (x == 15 and 2 <= y <= 9) or (x == 16 and 9 <= y <= 14) or (x == 17 and 14 <= y <= 18) or (x == 4 and 14 <= y <= 24 and y % 3) else
        'G' if (y == 3 and 6 <= x <= 13) or (y == 2 and x in (7, 9, 10, 12)) else
        'l' if (x + y < 6) else '.'))
    mask.cube((-5, -14, -1), (10, 14, 2), color='porc', pattern='mc', clusters=0.0, faces={
        'north': dict(color='porc', pattern='mc', clusters=0.0, hd=True, map=face, keys={'v': 'void', 'g': 'glow', 'm': 'void', 'c': 'crack', 'G': 'gold', 'l': 'porc_l'},
                      glow_keys='gc'),
    })
    mask.cube((-4, -1, -1.5), (8, 2, 2), color='porc_d', pattern='mc', clusters=0.0, rim=False)
    for side, sx in (('left', 1), ('right', -1)):
        horn = mask.part(f'{side}_horn', pivot=(3.5 * sx, -13, 0), rot=(0, 0, 0.6 * sx))
        horn.cube((-1, -4, -1), (2, 4, 2), color='horn', pattern='mc', clusters=0.0, rim=False)
        tip = horn.part(f'{side}_horn_tip', pivot=(0, -4, 0), rot=(0, 0, -0.7 * sx))
        tip.cube((-0.5, -4, -0.5), (1, 4, 1), color='horn_l', pattern='mc', clusters=0.0, rim=False, faces={'up': dict(color='glow', pattern='mc', clusters=0.0, glow=True)})
        rib = mask.part(f'{side}_ribbon', pivot=(5 * sx, -8, 0.5), rot=(0.2, 0, -0.25 * sx))
        rib.cube((-1, 0, 0), (2, 14, 0), color='ribbon', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='ribbon', pattern='mc', clusters=0.0, rim=False, hd=True, map=['ll..'] + ['.l..'] * 25 + ['_.._', '__._'], keys={'l': 'ribbon_l'})
            for f in ('north', 'south')})
    crown = mask.part('crown', pivot=(0, -14, 0))
    for i, x in enumerate((-3, -1, 1, 3)):
        crown.cube((x - 0.5, -2 - (i in (1, 2)) * 1.5, -0.5), (1, 2 + (i in (1, 2)) * 1.5, 1), color='gold', pattern='mc', clusters=0.0, rim=False,
                   faces={'up': dict(color='gold_l', pattern='mc', clusters=0.0)})
    return m


ALL = {'thumper': thumper, 'thumpling': thumpling, 'whistler': whistler, 'whistling': whistling, 'strummer': strummer, 'strumling': strumling,
       'conductor_mask': conductor_mask}
