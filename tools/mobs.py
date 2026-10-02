"""Geometry + paint definitions for The Sift's mobs (see modelkit.py)."""
from modelkit import Model


# =========================================================================== BULB
# The Bulb, remade 1:1 from the reference art: one see-through jelly cube (the body IS the head) on
# four stubby feet, with two tall flat ears. Its front is the reference's 10 x 7 pixel face read off
# pixel by pixel: periwinkle on top with a soft darker heart, cyan below, two dark-purple sleepy bar
# eyes and a little purple mouth. Every reference pixel is 2 x 2 texels (res=2), so a blink can
# close the bars to half height while the art stays exactly the reference's.
JELLY = 210  # the jelly's alpha, over the darker core (see BulbJellyLayer)

# reference pixels. a-f: the blue top, from the lit rim to the darker heart; g/h/j: the cyan lower
# third; Y eyes, M mouth. The sides, back and top carry the same banding without the face.
BULB_FRONT = ['abccccccba', 'bdeffffedb', 'ceeffffeec', 'ceeeeeeeec', 'gYYeeeeYYg', 'hgjjMMjjgh', 'hhhgggghhh']
BULB_SIDE = ['bccccccccb', 'cdeeeeeedc', 'ceeffffeec', 'ceeeeeeeec', 'gjeeeeeejg', 'hgjjjjjjgh', 'hhhgggghhh']
BULB_TOP = ['bccccccccb', 'cdeeeeeedc', 'ceeeeeeeec', 'ceeffffeec', 'ceeffffeec', 'ceeffffeec', 'ceeeffeeec', 'ceeeeeeeec',
            'cdeeeeeedc', 'bccccccccb']
# an ear, tip to base (both ears alike, as in the reference): a periwinkle rim, then the darker
# inner strip (p/q) beside a second strip (r/s/t) that turns cyan towards the base
BULB_EAR = ['aab', 'bpr', 'cps', 'cqt']
# the colours as the reference shows them (blue Bulb); the texture is lifted to make up for the
# shade the game puts on a mob's faces
BULB_BLUE = {'a': '#93a7bf', 'b': '#82a2bd', 'c': '#799bbe', 'd': '#6c96bf', 'e': '#598fbe', 'f': '#4c8abb', 'g': '#53aabd', 'h': '#67b0bf',
             'j': '#499cbe', 'p': '#5788c0', 'q': '#588fbd', 'r': '#6d95bf', 's': '#479cbf', 't': '#53abbd', 'L': '#9fd6dd', 'l': '#84c3cf',
             'Y': '#464275', 'M': '#58538a', 'k': '#3a64a0', 'k_l': '#4876b4', 'k_d': '#2f548a', 'G': '#c9e6ff'}
# the White Forest's Bulb: a snowy pearl top, pale icy cyan below, white ears with a pale pink strip
BULB_WHITE = {'a': '#ffffff', 'b': '#f7fbff', 'c': '#eef4fc', 'd': '#e5eef9', 'e': '#dce7f5', 'f': '#cddaee', 'g': '#bfeaf4', 'h': '#d3f4f9',
              'j': '#b2dfee', 'p': '#f4bcd1', 'q': '#f0b0c7', 'r': '#f8d3e1', 's': '#f5c6d8', 't': '#f7d6e3', 'L': '#effbfd', 'l': '#cfe8ef',
              'Y': '#4a3d78', 'M': '#7a67a3', 'k': '#c2cfe8', 'k_l': '#d3ddf1', 'k_d': '#aab8d6', 'G': '#ffffff'}
_BULB_SOLID = ('Y', 'M', 'k', 'k_l', 'k_d')  # eyes, mouth and the core are opaque
_BULB_LIFT = 1.27


def _bulb_palette(ref, lift):
    import colorsys
    pal = {}
    for k, c in ref.items():
        r, g, b = (int(c[i:i + 2], 16) / 255 for i in (1, 3, 5))
        if lift and k not in ('k', 'k_l', 'k_d', 'G'):
            h, s, v = colorsys.rgb_to_hsv(r, g, b)
            r, g, b = colorsys.hsv_to_rgb(h, s, min(1.0, v * _BULB_LIFT))
        pal[k] = '#%02x%02x%02x' % (round(r * 255), round(g * 255), round(b * 255)) + ('' if k in _BULB_SOLID else '%02x' % JELLY)
    return pal


def _x2(grid):
    """Reference pixels to texels: every character becomes 2 x 2."""
    return [''.join(ch * 2 for ch in row) for row in grid for _ in (0, 1)]


# the face in each mood, in texels (the left eye; the right one is its mirror). Always the
# reference's minimal style: flat bars that close to half height for a blink, arch into ^ ^ when
# happy, squeeze into > < when hurt, cross when dead; the little mouth widens, wobbles or shrinks.
_BULB_EYES = {'': (8, ['YYYY', 'YYYY']), 'blink': (9, ['YYYY']), 'sleep': (9, ['YYYY']), 'happy': (8, ['.YY.', 'Y..Y']),
              'hurt': (7, ['YY..', '..YY', 'YY..']), 'dead': (7, ['Y..Y', '.YY.', '.YY.', 'Y..Y'])}
_BULB_MOUTH = {'': (8, 10, ['MMMM', 'MMMM']), 'blink': (8, 10, ['MMMM', 'MMMM']), 'sleep': (9, 10, ['MM', 'MM']),
               'happy': (7, 10, ['MMMMMM', '.MMMM.']), 'hurt': (8, 10, ['.MM.', 'M..M']), 'dead': (8, 11, ['MMMM'])}


def bulb_face(expr):
    g = [list(r.replace('Y', 'e').replace('M', 'j')) for r in _x2(BULB_FRONT)]

    def put(x0, y0, rows, mirror=False):
        for j, row in enumerate(rows):
            for i, ch in enumerate(row[::-1] if mirror else row):
                if ch != '.':
                    g[y0 + j][x0 + i] = ch
    ey, eye = _BULB_EYES[expr]
    put(2, ey, eye)
    put(14, ey, eye, mirror=True)
    mx, my, mouth = _BULB_MOUTH[expr]
    put(mx, my, mouth)
    return [''.join(r) for r in g]


BULB_EXPRS = ['blink', 'happy', 'hurt', 'dead', 'sleep']
BULB_FACE = {('neutral' if e == '' else e): bulb_face(e) for e in [''] + BULB_EXPRS}


def hd_rows(spec, w, h):
    """Expands {row: text} into a full w x h texel map (unlisted rows and columns transparent)."""
    rows = []
    for y in range(h):
        t = spec.get(y, '')
        rows.append((t + '.' * w)[:w])
    return rows


def _gloss(rows, at):
    """One-texel highlights on the jelly (G)."""
    g = [list(r) for r in rows]
    for x, y in at:
        g[y][x] = 'G'
    return [''.join(r) for r in g]


def bulb() -> Model:
    """The Sift bunny (see the BULB notes above): 10 x 7 x 10 of jelly with a darker heart, stubby
    2-pixel feet, two flat 3 x 4 ears on springs and a tiny tail. BulbRenderer draws it at 0.6
    scale (half the old Bulb); the flowers on its back are block models (BulbFlowerLayer)."""
    pal = _bulb_palette(BULB_BLUE, True)
    variants = {'bulb_blue': {}, 'bulb_white': _bulb_palette(BULB_WHITE, False)}
    m = Model('bulb', (64, 64), pal, variants, res=2, expressions=BULB_EXPRS)

    def face(rows, **kw):
        return dict(color='e', pattern='mc', clusters=0.0, rim=False, hd=True, map=rows, **kw)
    side = _x2(BULB_SIDE)
    body = m.part('body', pivot=(0, 24, 0))
    body.cube((-5, -9, -5), (10, 7, 10), color='e', pattern='mc', clusters=0.0, rim=False, faces={
        'north': face(BULB_FACE['neutral'], expr={k: v for k, v in BULB_FACE.items() if k != 'neutral'}),
        'south': face(side), 'east': face(_gloss(side, [(3, 2)])), 'west': face(_gloss(side, [(16, 2)])),
        'up': face(_gloss(_x2(BULB_TOP), [(2, 17), (3, 17), (2, 16)])),
        'down': face(_x2(['gggggggggg'] + ['ggjjjjjjgg'] * 8 + ['gggggggggg'])),
    })
    for name, sx in (('left', 1), ('right', -1)):
        # the ears stand on the top, two pixels apart and a little back from the face
        ear = body.part(f'{name}_ear', pivot=(2.5 * sx, -9, -2))
        ear.cube((-1.5, -2, -0.5), (3, 2, 1), color='b', pattern='mc', clusters=0.0, rim=False, faces={
            'north': face(_x2(BULB_EAR[2:])), 'south': face(_x2(['bbb', 'ccc'])), 'east': face(_x2(['b', 'c'])),
            'west': face(_x2(['b', 'c'])), 'up': dict(skip=True), 'down': dict(skip=True)})
        tip = ear.part(f'{name}_ear_tip', pivot=(0, -2, 0))
        tip.cube((-1.5, -2, -0.5), (3, 2, 1), color='b', pattern='mc', clusters=0.0, rim=False, faces={
            'north': face(_x2(BULB_EAR[:2])), 'south': face(_x2(['aaa', 'bbb'])), 'east': face(_x2(['a', 'b'])),
            'west': face(_x2(['a', 'b'])), 'up': face(_gloss(_x2(['aaa']), [(1, 0)])), 'down': dict(skip=True)})
    foot = ['LLLL', 'LLLL', 'LLLL', 'llll']
    for name, x, z in (('front_left', 1, -1), ('front_right', -1, -1), ('back_left', 1, 1), ('back_right', -1, 1)):
        leg = body.part(f'{name}_leg', pivot=(4 * x, -2, 4 * z))
        leg.cube((-1, 0, -1), (2, 2, 2), color='L', pattern='mc', clusters=0.0, rim=False, faces={
            'north': face(foot), 'south': face(foot), 'east': face(foot), 'west': face(foot), 'up': dict(skip=True),
            'down': face(_x2(['ll', 'll']))})
    # a tiny cyan tail that wags when it is happy
    tail = body.part('tail', pivot=(0, -4, 5))
    tail_side = face(_x2(['hh', 'gg']))
    tail.cube((-1, -1, 0), (2, 2, 1), color='h', pattern='mc', clusters=0.0, rim=False, faces={
        'north': tail_side, 'south': face(_gloss(_x2(['hh', 'gg']), [(1, 0)])), 'east': tail_side, 'west': tail_side,
        'up': face(_x2(['hh'])), 'down': face(_x2(['gg']))})
    # the jelly's darker heart, seen through the body (drawn first, opaque)
    core = m.part('core', pivot=(0, 24, 0))
    core.cube((-3, -8, -3), (6, 4, 6), color='k', pattern='mc', clusters=0.35, rim=True)
    return m


# =========================================================================== SLUMBLER
def slumbler() -> Model:
    pal = {
        'skin': '#8fd0dc', 'skin_d': '#6aaec4', 'skin_l': '#b4e4ec', 'pearl': '#f0d9f2', 'lilac': '#c3a9ec',
        'belly': '#efe3f3', 'belly_l': '#fbf4fc', 'belly_d': '#d5c3df', 'spot': '#6d8fd3', 'glow': '#a8fbff',
        'mouth': '#d9577f', 'mouth_d': '#9c2f55', 'lid_d': '#5f9cb8', 'gill_l': '#ffc0e4',
        'tongue': '#ff8fb0', 'teeth': '#fff8ec', 'eye': '#1a1d38', 'iris': '#ffd66b', 'eye_hi': '#ffffff',
        'lid': '#7fb6cf', 'gill': '#f59ad0', 'gill_d': '#c85f9f', 'claw': '#eae3f2', 'fin': '#bfe9f5',
    }
    m = Model('slumbler', (128, 128), pal, {'slumbler': {}}, res=2, expressions=['angry', 'hurt', 'dead'])
    body = m.part('body', pivot=(0, 14, 0))
    body.cube((-8, -5, -11), (16, 9, 22), color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', faces={
        'up': dict(color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', map=[
            '................',
            '.....g....g.....',
            '................',
            '...g........g...',
            '................',
            '.....g....g.....',
            '................',
            '..g..........g..',
            '................',
            '.....g....g.....',
            '................',
            '...g........g...',
            '................',
            '.....g....g.....',
            '................',
            '..g..........g..',
            '................',
            '.....g....g.....',
        ], keys={'g': 'glow'}, glow_keys='g'),
        'down': dict(color='belly', pattern='mc', clusters=0.4),
        'west': dict(color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', bands=[(6, 'belly')]),
        'east': dict(color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', bands=[(6, 'belly')]),
    })
    crest = body.part('crest', pivot=(0, -5, 0))
    crest.cube((0, -4, -9), (0, 4, 18), color='fin', pattern='mc', clusters=0.0, rim=False, ribs=3, accent='lilac', alpha='membrane', edge='bottom',
               faces={'west': dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=3, accent='lilac', alpha='frill'),
                      'east': dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=3, accent='lilac', alpha='frill')})
    head = body.part('head', pivot=(0, -1, -11))
    head.cube((-9, -4, -10), (18, 5, 10), color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', faces={
        'down': dict(color='mouth', pattern='mc', clusters=0.0, rim=False, map=[
            't.t.t.t.t.t.t.t.t.',
            '..................',
            'd................d',
            'd................d',
            'd................d',
            'd................d',
            'd................d',
            'd................d',
            'dd..............dd',
            'dddddddddddddddddd',
        ], keys={'t': 'teeth', 'd': 'mouth_d'}),
        'north': dict(color='skin', pattern='mc', clusters=0.3, map=[
            '..................',
            '..................',
            '...o..........o...',
            '..................',
            'tttttttttttttttttt',
        ], keys={'o': 'skin_d', 't': 'teeth'}),
    })
    jaw = head.part('jaw', pivot=(0, 1, 0))
    jaw.cube((-9, 0, -10), (18, 3, 10), color='belly', pattern='mc', clusters=0.4, faces={
        'up': dict(color='mouth', pattern='mc', clusters=0.0, rim=False, map=[
            'dddddddddddddddddd',
            'd.......rr.......d',
            'd......rrrr......d',
            'd......rrrr......d',
            'd......rrrr......d',
            'd.......rr.......d',
            'd................d',
            'd................d',
            'd................d',
            't.t.t.t.t.t.t.t.t.',
        ], keys={'t': 'teeth', 'd': 'mouth_d', 'r': 'tongue'}),
        'north': dict(color='belly', pattern='mc', clusters=0.0, rim=False, map=['.t.t.t.t.t.t.t.t.t'], keys={'t': 'teeth'}),
        'west': dict(color='skin', pattern='mc', clusters=0.3),
        'east': dict(color='skin', pattern='mc', clusters=0.3),
    })
    for side, sx in (('left', 1), ('right', -1)):
        eye = head.part(f'{side}_eye', pivot=(5.5 * sx, -4, -6))
        # big gold eyes with a slit pupil and a wet highlight (6 x 6 texels per face)
        eye_keys = {'E': 'eye', 'h': 'eye_hi', 'i': 'iris', 'r': 'skin_d', 'l': 'lid', 'L': 'lid_d'}
        eye.cube((-1.5, -3, -1.5), (3, 3, 3), color='skin', pattern='mc', clusters=0.3, faces={
            'north': dict(color='iris', pattern='mc', clusters=0.0, rim=False, hd=True, keys=eye_keys,
                          map=['riiiir', 'iihEii', 'iiEEii', 'iiEEii', 'iiEEii', 'riiiir'],
                          expr={'angry': ['LLLLLL', 'llllLL' if sx > 0 else 'LLllll', 'iiEEhi', 'iiEEii', 'iiEEii', 'riiiir'],
                                'hurt': ['llllll', 'llllll', 'LLllLL', 'llLLll', 'llllll', 'llllll'],
                                'dead': ['riiiir', 'iEiiEi', 'iiEEii', 'iiEEii', 'iEiiEi', 'riiiir']}),
            ('east' if sx > 0 else 'west'): dict(color='iris', pattern='mc', clusters=0.0, rim=False, hd=True, keys=eye_keys,
                                                 map=['riiiir', 'iiEEii', 'iiEEii', 'iiEEii', 'iiEEii', 'riiiir']),
        })
        lid = eye.part(f'{side}_eyelid')
        lid.cube((-1.5, -3, -1.5), (3, 3, 3), inflate=0.12, color='lid', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='lid', pattern='mc', clusters=0.0, rim=False, map=['...', 'ddd', '...'], keys={'d': 'skin_d'}),
        })
        gills = head.part(f'{side}_gills', pivot=(9 * sx, -3, -3), rot=(0, -0.35 * sx, 0))
        gills.cube((0 if sx > 0 else -5, -5, 0), (5, 8, 0), color='gill', pattern='mc', clusters=0.0, rim=False, ribs=2, accent='gill_d', alpha='membrane',
                   edge='outer', edge_depth=2, scallop=2)
    for side, sx in (('left', 1), ('right', -1)):
        for end, sz in (('front', -7), ('hind', 7)):
            leg = body.part(f'{side}_{end}_leg', pivot=(7.5 * sx, 2.5, sz))
            leg.cube((0 if sx > 0 else -5, -1.5, -2), (5, 3, 4), color='skin', pattern='mc', clusters=0.3)
            foot = leg.part(f'{side}_{end}_foot', pivot=(4.5 * sx, 0.5, 0))
            foot.cube((-2, 0, -2.5), (4, 7, 5), color='skin_d', pattern='mc', clusters=0.3, faces={
                'north': dict(color='skin_d', pattern='mc', clusters=0.3, map=['....', '....', '....', '....', '....', 'c.c.', 'c.cc'], keys={'c': 'claw'}),
                'down': dict(color='skin_d', pattern='mc', clusters=0.0, rim=False),
            })
    tail1 = body.part('tail1', pivot=(0, -1, 11))
    tail1.cube((-5, -3, 0), (10, 6, 10), color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', faces={
        'up': dict(color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', map=['..........', '....gg....', '..........', '..........', '....gg....'],
                   keys={'g': 'glow'}, glow_keys='g'),
        'down': dict(color='belly', pattern='mc', clusters=0.4),
    })
    tail2 = tail1.part('tail2', pivot=(0, 0, 10))
    tail2.cube((-3.5, -2, 0), (7, 4, 10), color='skin', pattern='mc', clusters=0.3, spots=0.3, accent='spot', faces={
        'down': dict(color='belly', pattern='mc', clusters=0.4),
        'up': dict(color='skin', pattern='mc', clusters=0.3, map=['.......', '...g...', '.......', '.......', '...g...'], keys={'g': 'glow'}, glow_keys='g'),
    })
    tail3 = tail2.part('tail3', pivot=(0, 0, 10))
    tail3.cube((-2, -1.5, 0), (4, 3, 9), color='skin', pattern='mc', clusters=0.3, faces={'down': dict(color='belly', pattern='mc', clusters=0.4)})
    tail3.cube((0, -5, 0), (0, 10, 10), color='fin', pattern='mc', clusters=0.0, rim=False, ribs=3, accent='lilac', alpha='membrane', edge='bottom',
               faces={'west': dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=3, accent='pearl'), 'east': dict(color='fin', pattern='mc', clusters=0.0, rim=False, ribs=3, accent='pearl')})
    return m


# =========================================================================== SIFTER
def sifter() -> Model:
    """The Sifter: a dune ambusher that is mostly trap. A chunky, layered carapace of sandstone and
    sun-bleached bone forms a box head whose lid slams shut like a bear trap over a ring of teeth; a
    glowing lure dangles from its brow to tempt the curious. It scuttles on four bony crab legs and
    lies buried in Dreamsand with only the lid and lure showing."""
    from mobs_wild import eye, mc, put, blank
    exprs = ['blink', 'angry', 'hurt', 'dead']
    pal = {
        'skin': '#1fa3c1', 'skin_l': '#3ec0d6', 'skin_d': '#157e9f', 'deep': '#0e5a7d',
        'sand': '#e6c48c', 'sand_l': '#f6dcac', 'sand_d': '#c49a62', 'grit': '#b0864f',
        'bone': '#efe6cf', 'bone_l': '#fffaee', 'bone_d': '#c7b996', 'crack': '#9c8a66',
        'mouth': '#17328c', 'mouth_l': '#2246ad', 'mouth_d': '#0c1e5c', 'gum': '#d9587a',
        'tooth': '#fbf6ea', 'tooth_d': '#cdbf9e', 'eye': '#0c1430', 'iris': '#ffd36a', 'iris_d': '#e09a2a', 'eye_hi': '#ffffff',
        'lid': '#c49a62', 'lid_d': '#8e6a3c', 'lure': '#ffe28a', 'lure_l': '#fff6cc', 'lure_d': '#f2b23a', 'stalk': '#157e9f',
        'swirl': '#1892b2',
    }
    m = Model('sifter', (128, 128), pal, {'sifter': {}}, res=2, expressions=exprs)
    sand = mc('sand', clusters=0.6, spots=0.2, accent='grit')
    bone = mc('bone', clusters=0.4, spots=0.12, accent='crack')
    skin = mc('skin', clusters=0.6, spots=0.25, accent='swirl')
    ek = {'r': 'lid_d', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}

    body = m.part('body', pivot=(0, 24, 0))
    # a squat soft-bodied abdomen under a sandstone back plate with a bone ridge
    body.cube((-5, -11, -4), (10, 6, 9), **skin, faces={'down': mc('skin_d', clusters=0.2)})
    body.cube((-5.5, -12, -3), (11, 2, 9), **sand, faces={'up': mc('sand', clusters=0.8, spots=0.3, accent='grit')})
    body.cube((-1.5, -13.5, -1), (3, 1.5, 6), **bone)
    # four bony crab legs splayed out to the sides, tipped with bone claws
    for name, sx, z in (('front_left', 1, -2), ('front_right', -1, -2), ('back_left', 1, 3), ('back_right', -1, 3)):
        leg = body.part(f'{name}_leg', pivot=(4.5 * sx, -8, z), rot=(0, 0, -0.35 * sx))
        leg.cube((0 if sx > 0 else -6, -1.5, -1.5), (6, 3, 3), **sand)
        shin = leg.part(f'{name}_shin', pivot=(5.5 * sx, 0, 0), rot=(0, 0, 0.05 * sx))
        shin.cube((-1, 0, -1), (2, 8.5, 2), **bone, faces={
            'north': mc('bone', clusters=0.3, hd=True, map=['....'] * 7 + ['cccc'] + ['....'] * 9, keys={'c': 'crack'})})
        shin.cube((-0.5, 8.5, -0.5), (1, 2, 1), **mc('bone_d', clusters=0.0, rim=False))

    # ---- the head: a heavy sandstone tray (lower jaw) with a lid of layered carapace
    head = body.part('head', pivot=(0, -12, 1))
    head.cube((-8, -3, -9), (16, 3, 16), **sand, faces={
        'up': mc('mouth', clusters=0.4, hd=True, map=put(blank(32, 32), 0, 0, ['GG' * 16, 'G' + '.' * 30 + 'G'] + ['G' + '.' * 30 + 'G'] * 28 +
                                                                            ['G' + '.' * 30 + 'G', 'GG' * 16]), keys={'G': 'gum'}),
        'down': mc('skin_d', clusters=0.3),
        'north': mc('sand', clusters=0.4, hd=True, map=['B' * 32, 'b' * 32, '.' * 32, '.' * 32, '.' * 32, 'g.' * 16],
                    keys={'B': 'bone_l', 'b': 'bone_d', 'g': 'grit'}),
    })
    # a bone chin guard
    head.cube((-6, -1, -9.5), (12, 2, 1), **bone)
    for i, tx in enumerate((-6.5, -4, -1.5, 1, 3.5, 6)):
        tooth = head.part(f'lower_tooth_{i}', pivot=(tx, -3, -8.5))
        tooth.cube((-0.5, -2 if i % 2 else -1.5, -0.5), (1, 2 if i % 2 else 1.5, 1), **mc('tooth', clusters=0.0, rim=False), faces={
            'north': mc('tooth', clusters=0.0, rim=False, hd=True, map=['..', 'dd', '..', '..'], keys={'d': 'tooth_d'})})
    for i, tz in enumerate((-5, -1, 3)):
        for side, sx in (('left', 1), ('right', -1)):
            st = head.part(f'side_tooth_{side}_{i}', pivot=(7.3 * sx, -3, tz))
            st.cube((-0.5, -1.5, -0.5), (1, 1.5, 1), **mc('tooth', clusters=0.0, rim=False))
    # the lid, hinged at the back: three layers - sandstone shell, bone plate, spined ridge
    lid = head.part('lid', pivot=(0, -3, 7))
    lid.cube((-8.5, -4, -16.5), (17, 4, 17), **sand, faces={
        'down': mc('mouth', clusters=0.4, hd=True, map=put(blank(34, 34), 0, 0, ['GG' * 17] + ['G' + '.' * 32 + 'G'] * 32 + ['GG' * 17]),
                   keys={'G': 'gum'}),
        'north': mc('sand', clusters=0.3, hd=True, map=['.' * 34] * 4 + ['b' * 34, 'B' * 34, 'g.' * 17, '.' * 34], keys={'B': 'bone_l', 'b': 'bone_d', 'g': 'grit'}),
        'up': mc('sand', clusters=1.0, spots=0.35, accent='grit'),
    })
    lid.cube((-7, -6, -14.5), (14, 2, 13), **bone, faces={
        'up': mc('bone', clusters=0.6, spots=0.2, accent='crack', hd=True,
                 map=put(put(blank(28, 26), 4, 6, ['c...', '.c..', '..cc', '....', '..c.']), 18, 14, ['..c', '.c.', 'c..', 'c..']), keys={'c': 'crack'})})
    lid.cube((-3.5, -7.5, -12), (7, 1.5, 9), **sand)
    for i, (sx, sz) in enumerate(((0, -10.5), (0, -7), (0, -3.5), (-6, -5), (6, -5), (-6, -11), (6, -11))):
        spike = lid.part(f'spike_{i}', pivot=(sx, -7.5 if sx == 0 else -6, sz), rot=(-0.25, 0, -0.35 * (1 if sx > 0 else -1 if sx < 0 else 0)))
        spike.cube((-1, -2.5 if sx == 0 else -2, -1), (2, 2.5 if sx == 0 else 2, 2), **mc('bone', clusters=0.0), faces={
            'up': mc('bone_l', clusters=0.0, rim=False)})
    for i, tx in enumerate((-7, -4.5, -2, 0.5, 3, 5.5)):
        tooth = lid.part(f'upper_tooth_{i}', pivot=(tx + 0.5, 0, -15.5))
        h = 2.5 if i in (0, 5) else 2 if i % 2 == 0 else 1.5
        tooth.cube((-0.5, 0, -0.5), (1, h, 1), **mc('tooth', clusters=0.0, rim=False), faces={
            'north': mc('tooth', clusters=0.0, rim=False, hd=True, map=['..'] * (int(h * 2) - 1) + ['dd'], keys={'d': 'tooth_d'})})
    # two small amber eyes peeking out from under the brow
    for side, sx in (('left', 1), ('right', -1)):
        e = lid.part(f'{side}_eye', pivot=(4.5 * sx, -4, -16.2))
        e.cube((-2, -2, -1), (4, 4, 1.5), **mc('sand', clusters=0.0, rim=False), faces={
            'north': mc('sand', clusters=0.0, rim=False, hd=True, map=eye(8, 8, '', sx < 0, pupil='slit', rim=0.72), keys=ek,
                        expr={x: eye(8, 8, x, sx < 0, pupil='slit', rim=0.72) for x in exprs}, glow_keys='iI')})
    # the lure: a jointed stalk curling out of the brow with a glowing bulb on the end
    stalk = lid.part('lure_stalk', pivot=(0, -7.5, -12.5), rot=(0.5, 0, 0))
    stalk.cube((-0.5, -6, -0.5), (1, 6, 1), **mc('stalk', clusters=0.0, rim=False))
    tip = stalk.part('lure_tip', pivot=(0, -6, 0), rot=(1.3, 0, 0))
    tip.cube((-0.5, -5, -0.5), (1, 5, 1), **mc('stalk', clusters=0.0, rim=False))
    lure = tip.part('lure', pivot=(0, -5, 0))
    lure.cube((-1.5, -1.5, -1.5), (3, 3, 3), **mc('lure', clusters=0.0, rim=False, glow=True), faces={
        f: mc('lure', clusters=0.0, rim=False, glow=True, hd=True, map=['ll....', 'l.....', '......', '......', '.....d', '....dd'],
              keys={'l': 'lure_l', 'd': 'lure_d'}, glow_keys='ld') for f in ('north', 'south', 'east', 'west')})
    return m


# =========================================================================== ENCHOER
# A2 Echoer: the Echoer (still entity id `enchoer`) was redesigned - its model lives in tools/echoer.py
from echoer import enchoer  # noqa: E402,F401


# =========================================================================== RIVETER
_SKULL = {0: 'BBBBBBBBBB', 1: 'dBBBBBBBBd'}
RIVETER_FACE = {
    'neutral': {**_SKULL, 2: 'ee..BB..ee', 3: 'eeee..eeee', 4: '.ee....ee.'},
    'blink': {**_SKULL, 4: 'eeee..eeee'},
    'angry': {**_SKULL, 2: 'eee.BB.eee', 3: '.eee..eee.'},
    'hurt': {**_SKULL, 3: 'ee......ee', 4: '..ee..ee..'},
    'dead': {**_SKULL, 2: 'e.e....e.e', 3: '.e......e.', 4: 'e.e....e.e'},
}


def riveter() -> Model:
    """The sculk bat: a tall, starved, hunched thing of dark hide and bone. Its arms are folded wings
    that end in four long teal claws; it hangs head-down from cave ceilings with them dangling."""
    pal = {
        'hide': '#1d2b47', 'hide_l': '#2c4066', 'hide_d': '#121b30',
        'bone': '#d9d4bf', 'bone_l': '#efeadb', 'bone_d': '#a9a28c',
        'claw': '#1fa39b', 'claw_l': '#4fd5c6', 'claw_d': '#11706a',
        'shin': '#1a8a87', 'shin_l': '#35b6ab', 'shin_d': '#0f5f5e',
        'eye': '#a6fff5', 'spot': '#3be6d8', 'maw': '#07101c', 'tooth': '#e9f4ef',
    }
    m = Model('riveter', (64, 64), pal, {'riveter': {}}, res=2, expressions=['blink', 'angry', 'hurt', 'dead'])
    hide = dict(color='hide', pattern='mc', clusters=0.5)
    body = m.part('body', pivot=(0, 24, 0))
    body.cube((-2.5, -13, -1.5), (5, 2, 3), color='bone', pattern='mc', clusters=0.4, rim=False)
    for side, sx in (('left', 1), ('right', -1)):
        leg = body.part(f'{side}_leg', pivot=(1.6 * sx, -12, 0), rot=(-0.12, 0, 0))
        leg.cube((-1, 0, -1), (2, 6, 2), **hide, faces={
            'north': dict(**hide, map=['..', '..', '..', '..', 'bb', 'bb'], keys={'b': 'bone'}),
        })
        shin = leg.part(f'{side}_shin', pivot=(0, 6, 0), rot=(0.24, 0, 0))
        shin.cube((-1, 0, -1), (2, 5, 2), color='shin', pattern='mc', clusters=0.4)
        shin.cube((-1.5, 5, -2.5), (3, 1, 3), color='shin', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='bone', pattern='mc', clusters=0.0, rim=False, map=['b.b'], keys={'b': 'bone_d'}),
        })
    torso = body.part('torso', pivot=(0, -13, 0), rot=(0.22, 0, 0))
    torso.cube((-1.5, -4, -1), (3, 4, 2), **hide, faces={
        'south': dict(**hide, map=['.b.', '...', '.b.', '...'], keys={'b': 'bone'}),
    })
    torso.cube((-3.5, -10, -2), (7, 6, 4), **hide, faces={
        'north': dict(**hide, map=[
            '.......',
            '.bb.bb.',
            '.......',
            '.bbsbb.',
            '.......',
            '..b.b..',
        ], keys={'b': 'bone', 's': 'spot'}, glow_keys='s'),
        'south': dict(**hide, map=['...b...', '..s....', '...b...', '.....s.', '...b...', '.......'], keys={'b': 'bone', 's': 'spot'}, glow_keys='s'),
    })
    torso.cube((-4.5, -12.5, -2.5), (9, 3, 5), color='hide', pattern='mc', clusters=0.5, faces={
        'north': dict(color='hide', pattern='mc', clusters=0.3, map=['B.......B', 'B.......B', '.........'], keys={'B': 'bone'}),
        'up': dict(color='hide_l', pattern='mc', clusters=0.4, map=['B.......B', '.........', '.........', '.........', 'B.......B'],
                   keys={'B': 'bone'}),
    })
    for i, (sz, h) in enumerate(((0.5, 2), (2.0, 3))):
        torso.cube((-0.5, -12.5 - h, sz), (1, h, 1), color='claw', pattern='mc', clusters=0.0, rim=False,
                   faces={'up': dict(color='claw_l', pattern='mc', clusters=0.0)})
    head = torso.part('head', pivot=(0, -11.5, -2.5), rot=(0.08, 0, 0))
    head.cube((-2.5, -4, -4.5), (5, 4, 5), **hide, faces={
        'north': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=hd_rows(RIVETER_FACE['neutral'], 10, 8), keys={'e': 'eye', 'B': 'bone', 'd': 'bone_d'},
                      glow_keys='e', expr={k: hd_rows(v, 10, 8) for k, v in RIVETER_FACE.items() if k != 'neutral'}),
        'up': dict(color='bone', pattern='mc', clusters=0.3, map=['.....', '.....', '..d..', '.d.d.', '.....'], keys={'d': 'bone_d'}),
        'east': dict(color='hide', pattern='mc', clusters=0.2, map=['BBBBB', '...BB'], keys={'B': 'bone'}),
        'west': dict(color='hide', pattern='mc', clusters=0.2, map=['BBBBB', 'BB...'], keys={'B': 'bone'}),
        'down': dict(color='maw', pattern='mc', clusters=0.0),
    })
    jaw = head.part('jaw', pivot=(0, 0, 0))
    jaw.cube((-2, 0, -4.5), (4, 1, 4), color='hide_d', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='hide_d', pattern='mc', clusters=0.0, rim=False, map=['t..t'], keys={'t': 'tooth'}),
        'up': dict(color='maw', pattern='mc', clusters=0.0, map=['t..t', '....', '.ss.', '....'], keys={'t': 'tooth', 's': 'spot'}, glow_keys='s'),
    })
    for side, sx in (('left', 1), ('right', -1)):
        horn = head.part(f'{side}_horn', pivot=(1.5 * sx, -4, -1), rot=(-0.7, 0, 0.35 * sx))
        horn.cube((-0.5, -4, -0.5), (1, 4, 1), color='claw', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='claw_l', pattern='mc', clusters=0.0),
            'north': dict(color='claw', pattern='mc', clusters=0.0, rim=False, map=['l', '.', '.', 'd'], keys={'l': 'claw_l', 'd': 'claw_d'}),
        })
    for side, sx in (('left', 1), ('right', -1)):
        wing = torso.part(f'{side}_wing', pivot=(4.5 * sx, -10.5, 0.5))
        x0 = 0 if sx > 0 else -4
        wing.cube((x0, -4, -1.5), (4, 14, 3), color='hide', pattern='mc', clusters=0.6, faces={
            'north': dict(color='hide', pattern='mc', clusters=0.5, map=['bbbb', '....', '....', '.s..', '....', '....', '....', '...s',
                                                                          '....', '....', '.s..', '....', '....', '....'],
                          keys={'b': 'bone', 's': 'spot'}, glow_keys='s'),
            'up': dict(color='bone', pattern='mc', clusters=0.3),
            'east' if sx > 0 else 'west': dict(color='hide', pattern='mc', clusters=0.5, map=['bbb', '...', '...', '...', '.s.'],
                                               keys={'b': 'bone', 's': 'spot'}, glow_keys='s'),
        })
        for i in range(4):
            cx = (0.5 + i) * sx
            claw = wing.part(f'{side}_claw_{i}', pivot=(cx, 9.5, -0.5 if i % 2 else 0.5), rot=(0.08, 0, (0.05 - 0.035 * i) * sx))
            claw.cube((-0.5, 0, -0.5), (1, 9 - (1 if i in (0, 3) else 0), 1), color='claw', pattern='mc', clusters=0.0, rim=False, faces={
                'north': dict(color='claw', pattern='mc', clusters=0.0, rim=False, map=['d', 'd', '.', '.', '.', '.', 'l', 'l', 'l'],
                              keys={'d': 'claw_d', 'l': 'claw_l'}),
                'south': dict(color='claw', pattern='mc', clusters=0.0, rim=False, map=['d', 'd', 'd', '.', '.', '.', '.', 'l', 'l'],
                              keys={'d': 'claw_d', 'l': 'claw_l'}),
            })
    return m


# =========================================================================== HARMONER
HARMONER_VARIANTS = {
    # name: body, belly, wing, crest, crest tip, beak.  Each colour leads to its own structure.
    'rose': ('#e8577f', '#ffb3c6', '#b8325f', '#ffd86b', '#ff8a3d', '#ffd23f'),
    'azure': ('#3f8fe8', '#bfe8ff', '#2a5fb8', '#7ff0ff', '#ffffff', '#ffc23f'),
    'gold': ('#f2b632', '#fff0b0', '#c97f1f', '#ff6b3d', '#ffef9a', '#4a3a6e'),
    'violet': ('#8c5ae0', '#e0c8ff', '#5e36a8', '#ff8ad8', '#ffd0f0', '#ffd23f'),
    'jade': ('#35c28f', '#c8ffe0', '#1f8a6a', '#c8ff5a', '#fff7a0', '#ff9a3d'),
    'coral': ('#ff7a4a', '#ffd8b8', '#c2462a', '#3fd0ef', '#c8f8ff', '#3a3060'),
    'night': ('#27304d', '#4a5a86', '#161c30', '#2ef2e2', '#c8fffb', '#e3ddcc'),
    # summoned by the Conductor's Staff
    'sculk': ('#12303a', '#1f5a66', '#0a1a22', '#3ff5e6', '#e8fffd', '#e3ddcc'),
}


def _shades(hexc):
    import colorsys
    r, g, b = (int(hexc[i:i + 2], 16) / 255 for i in (1, 3, 5))
    h, l, s_ = colorsys.rgb_to_hls(r, g, b)

    def mk(dl):
        rr, gg, bb = colorsys.hls_to_rgb(h, max(0, min(1, l + dl)), s_)
        return '#%02x%02x%02x' % (int(rr * 255), int(gg * 255), int(bb * 255))
    return mk(0.0), mk(0.1), mk(-0.12)


BIRD_EYES = {
    'neutral': ['.ee.', 'eEee', 'eeee', '.ee.'],
    'blink': ['....', '....', 'eeee', '....'],
    'happy': ['....', '.ee.', 'e..e', '....'],
    'hurt': ['e...', '.ee.', 'e...', '....'],
    'dead': ['e..e', '.ee.', '.ee.', 'e..e'],
    'sleep': ['....', '....', 'e..e', '.ee.'],
}
bird_keys = {'e': 'eye', 'E': 'eye_hi'}


def _place(glyph, x, y, w, h, mirror=False):
    grid = [['.'] * w for _ in range(h)]
    for j, row in enumerate(glyph):
        if mirror:
            row = row[::-1]
        for i, ch in enumerate(row):
            if ch != '.':
                grid[y + j][x + i] = ch
    return [''.join(r) for r in grid]


def bird_side(expr, mirror):
    """The Harmoner's big round side eye on a 10 x 10 texel cheek."""
    return _place(BIRD_EYES[expr], 4 if mirror else 2, 3, 10, 10, mirror)


def bird_front(expr):
    """Seen from the front, the edges of both eyes peek out."""
    g = [r[:2] for r in BIRD_EYES[expr]]
    a = _place(g, 0, 3, 10, 10)
    b = _place([r[::-1] for r in g], 8, 3, 10, 10)
    return [''.join(x if x != '.' else y for x, y in zip(r1, r2)) for r1, r2 in zip(a, b)]


def harmoner() -> Model:
    """A chunky alien songbird: round body, big block beak, a fan crest of flame-tipped plumes and
    a long fanned tail. Six colourings, one for each structure it can lead you to."""
    variants = {}
    for name, (body, belly, wing, crest, tip, beak) in HARMONER_VARIANTS.items():
        v = {}
        for key, c in (('body', body), ('belly', belly), ('wing', wing), ('crest', crest), ('tip', tip), ('beak', beak)):
            v[key], v[key + '_l'], v[key + '_d'] = _shades(c)
        variants[f'harmoner_{name}'] = v
    pal = dict(variants['harmoner_rose'])
    pal.update({'eye': '#1a1830', 'eye_hi': '#ffffff', 'leg': '#4a3a5a', 'leg_l': '#6a5a7a', 'leg_d': '#33283f'})
    m = Model('harmoner', (64, 64), pal, variants, res=2, expressions=['blink', 'happy', 'hurt', 'dead', 'sleep'])
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(1.5 * sx, 20, 0.5))
        leg.cube((-0.5, 0, -0.5), (1, 3, 1), color='leg', pattern='mc', clusters=0.0, rim=False)
        leg.cube((-1, 3, -1.5), (2, 1, 2), color='leg', pattern='mc', clusters=0.0, rim=False)
    body = m.part('body', pivot=(0, 20, 0))
    body.cube((-3, -6, -3.5), (6, 6, 7), color='body', pattern='mc', clusters=0.4, faces={
        'north': dict(color='belly', pattern='mc', clusters=0.3),
        'down': dict(color='belly_d', pattern='mc', clusters=0.2),
        'east': dict(color='body', pattern='mc', clusters=0.3, bands=[(3, 'belly')]),
        'west': dict(color='body', pattern='mc', clusters=0.3, bands=[(3, 'belly')]),
    })
    head = body.part('head', pivot=(0, -6, -2))
    head.cube((-2.5, -5, -2.5), (5, 5, 5), color='body', pattern='mc', clusters=0.3, faces={
        'north': dict(color='body', pattern='mc', clusters=0.0, hd=True, map=bird_front('neutral'), keys=bird_keys,
                      expr={k: bird_front(k) for k in BIRD_EYES if k != 'neutral'}),
        'east': dict(color='body', pattern='mc', clusters=0.0, hd=True, map=bird_side('neutral', False), keys=bird_keys,
                     expr={k: bird_side(k, False) for k in BIRD_EYES if k != 'neutral'}),
        'west': dict(color='body', pattern='mc', clusters=0.0, hd=True, map=bird_side('neutral', True), keys=bird_keys,
                     expr={k: bird_side(k, True) for k in BIRD_EYES if k != 'neutral'}),
    })
    # the big block beak: upper half on the head, lower half on its own hinge
    head.cube((-1.5, -3, -5.5), (3, 2, 3), color='beak', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='beak', pattern='mc', clusters=0.0, rim=False, map=['...', 'ddd'], keys={'d': 'beak_d'}),
        'up': dict(color='beak_l', pattern='mc', clusters=0.0),
    })
    jaw = head.part('jaw', pivot=(0, -1, -2.5))
    jaw.cube((-1, 0, -2.5), (2, 1, 2.5), color='beak_d', pattern='mc', clusters=0.0, rim=False)
    crest = head.part('crest', pivot=(0, -5, 0.5), rot=(-0.35, 0, 0))
    for i, (cx, h, lean) in enumerate(((-1, 4, -0.3), (0, 6, 0.0), (1, 4, 0.3))):
        plume = crest.part(f'plume_{i}', pivot=(cx, 0, 0), rot=(0, 0, lean))
        plume.cube((-0.5, -h, -0.5), (1, h, 1), color='crest', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='crest', pattern='mc', clusters=0.0, rim=False, map=['t'] * 2 + ['.'] * (h - 2), keys={'t': 'tip'}),
            'south': dict(color='crest', pattern='mc', clusters=0.0, rim=False, map=['t'] * 2 + ['.'] * (h - 2), keys={'t': 'tip'}),
            'east': dict(color='crest', pattern='mc', clusters=0.0, rim=False, map=['t'] * 2 + ['.'] * (h - 2), keys={'t': 'tip'}),
            'west': dict(color='crest', pattern='mc', clusters=0.0, rim=False, map=['t'] * 2 + ['.'] * (h - 2), keys={'t': 'tip'}),
            'up': dict(color='tip_l', pattern='mc', clusters=0.0),
        })
    for side, sx in (('left', 1), ('right', -1)):
        wing = body.part(f'{side}_wing', pivot=(3 * sx, -5.5, -2))
        wing.cube((0 if sx > 0 else -1, 0, 0), (1, 5, 7), color='wing', pattern='mc', clusters=0.4, faces={
            ('east' if sx > 0 else 'west'): dict(color='wing', pattern='mc', clusters=0.2, map=[
                '.......', '.......', 'ttt....', 'tttt...', '.ttttt.'] if sx > 0 else ['.......', '.......', '....ttt', '...tttt', '.ttttt.'],
                keys={'t': 'wing_d'}),
        })
    tail = body.part('tail', pivot=(0, -2.5, 3.5), rot=(0.3, 0, 0))
    tail.cube((-2, -0.5, 0), (4, 1, 5), color='wing', pattern='mc', clusters=0.3, rim=False)
    fan = tail.part('tail_fan', pivot=(0, 0, 5), rot=(0.2, 0, 0))
    fan.cube((-3, -0.5, 0), (6, 1, 4), color='crest', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='crest', pattern='mc', clusters=0.0, map=['......', '.c..c.', 'tctctc', 't_tt_t'], keys={'c': 'crest_d', 't': 'tip'}),
        'down': dict(color='crest_d', pattern='mc', clusters=0.0, map=['......', '......', '......', 't_tt_t'], keys={'t': 'tip'}),
    })
    return m


# =========================================================================== THE DICTATOR & HIS ORCHESTRA
SCULK = {
    'hide': '#141e2c', 'hide_l': '#1f2c40', 'hide_d': '#0b1119',
    'bone': '#e3ddcc', 'bone_l': '#f4f0e5', 'bone_d': '#b3ab96',
    'glow': '#2ef2e2', 'glow_d': '#15a89f', 'void': '#04080c',
}


DEVIL = {
    # 12 x 20 texels: a long devil's face - heavy brow, slanted glowing eyes, nostril slits, a wide fanged grin, a chin spike
    'neutral': {3: 'bbbb....bbbb', 4: '.bbbb..bbbb.', 5: 'EEee....eeEE', 6: '.EEee..eeEE.', 7: '..EE....EE..', 10: '....n..n....',
                13: 'mmmmmmmmmmmm', 14: 'mfmmmmmmmmfm', 15: 'mffmmmmmmffm', 16: '.mmtmtmtmmm.', 17: '..mmmmmmmm..', 19: '.....cc.....'},
    'angry': {2: 'b..........b', 3: 'bbb......bbb', 4: '.bbbb..bbbb.', 5: 'EEEe....eEEE', 6: '.EEee..eeEE.', 7: '..EE....EE..',
              10: '....n..n....', 12: 'mmmmmmmmmmmm', 13: 'mfmfmfmfmfmf', 14: 'mmmmmmmmmmmm', 15: 'mfmfmfmfmfmf', 16: '.mmmmmmmmmm.',
              17: '..mmmmmmmm..', 19: '.....cc.....'},
    'hurt': {3: '.bb......bb.', 4: '..bbb..bbb..', 5: 'EE........EE', 6: '..EEE..EEE..', 10: '....n..n....', 14: '..mmmmmmmm..',
             15: '.mffmmmmffm.', 16: '..mmmmmmmm..', 19: '.....cc.....'},
    'dead': {3: 'bbbb....bbbb', 4: '.bbbb..bbbb.', 5: 'E.E....E.E..', 6: '.E......E...', 7: 'E.E....E.E..', 10: '....n..n....',
             14: '.mmmmmmmmmm.', 15: 'mf........fm', 19: '.....cc.....'},
}


def dictator() -> Model:
    """The Dictator, conductor of the sculk orchestra: a towering, gaunt devil in sculk armour. Long
    legs in plated greaves, a breastplate and spiked pauldrons over a tattered cape, gauntleted hands
    that reach past his knees, a tuning-fork staff topped with a glowing orb - and a long devil's
    head with great curling horns, four pointed ears and a fanged grin under glowing eyes."""
    pal = dict(SCULK)
    pal.update({'skin': '#3a1322', 'skin_l': '#55203a', 'skin_d': '#200912',
                'armor': '#1d2a3c', 'armor_l': '#2d4260', 'armor_d': '#0f1724',
                'brass': '#b89a52', 'brass_l': '#dcc27a', 'brass_d': '#7f6a35',
                'cape': '#2a1030', 'cape_l': '#3e1a46', 'cape_d': '#16081a',
                'horn': '#e3ddcc', 'horn_l': '#f4f0e5', 'horn_d': '#9e957c', 'fang': '#f4f0e5', 'brow': '#170810'})
    m = Model('dictator', (128, 192), pal, {'dictator': {}}, res=2, expressions=['angry', 'hurt', 'dead'])
    skin = dict(color='skin', pattern='mc', clusters=0.4)
    armor = dict(color='armor', pattern='mc', clusters=0.35)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(2.4 * sx, 0, 0), rot=(-0.2, 0, 0))
        leg.cube((-1.25, 0, -1.25), (2.5, 12, 2.5), **skin)
        leg.cube((-1.75, 1, -1.75), (3.5, 5, 3.5), **armor, faces={'north': dict(**armor, hd=True, map=['.bbbbb.', 'b.....b', '.......', '...g...'],
                                                                                  keys={'b': 'brass', 'g': 'glow'}, glow_keys='g')})
        shin = leg.part(f'{side}_shin', pivot=(0, 12, 0), rot=(0.45, 0, 0))
        shin.cube((-1, 0, -1), (2, 12, 2), **skin)
        # greave with a knee spike
        shin.cube((-1.5, 0, -2), (3, 8, 3), **armor, faces={'north': dict(**armor, hd=True, keys={'b': 'brass', 'l': 'armor_l'},
                                                                        map=['bbbbbb', 'llllll', '......', '..ll..', '......', '..ll..'])})
        shin.cube((-0.5, -1, -3), (1, 2, 2), color='horn', pattern='mc', clusters=0.0, rim=False)
        foot = shin.part(f'{side}_foot', pivot=(0, 12, 0), rot=(-0.25, 0, 0))
        foot.cube((-1.5, 0, -4.5), (3, 1.5, 5.5), **armor, faces={'north': dict(**armor, hd=True, map=['h.hh.h', 'hhhhhh', 'hhhhhh'], keys={'h': 'horn'})})
    body = m.part('body', pivot=(0, 0, 0))
    body.cube((-3.5, -2, -2), (7, 3, 4), **armor, faces={'north': dict(**armor, hd=True, map=['bbbbbbbbbbbbbb', 'bbbbbBBBBbbbbb', 'bbbbbBggBbbbbb',
                                                                                            'bbbbbBBBBbbbbb', '..............', '..............'],
                                                                     keys={'b': 'brass_d', 'B': 'brass', 'g': 'glow'}, glow_keys='g')})
    torso = body.part('torso', pivot=(0, -2, 0), rot=(0.12, 0, 0))
    torso.cube((-2, -6, -1.5), (4, 6, 3), **skin)
    # breastplate over the ribs
    torso.cube((-4, -15, -2.75), (8, 9, 5.5), **armor, faces={'north': dict(**armor, hd=True, keys={'b': 'brass', 'l': 'armor_l', 'd': 'armor_d', 'g': 'glow', 'G': 'glow_d'},
                                                                           glow_keys='gG', map=[
        'bbbbbbbbbbbbbbbb', 'blllllllllllllb.', 'bl.....dd.....lb', 'bl....dGGd....lb', 'bl...dGggGd...lb', 'bl....dGGd....lb', 'bl.....dd.....lb',
        '.bl..........lb.', '.bl..ll..ll..lb.', '..bl........lb..', '..bl..llll..lb..', '...bl......lb...', '...bl..ll..lb...', '....bl....lb....',
        '.....bbbbbb.....', '......bbbb......', '................', '................'])})
    torso.cube((-5, -16, -2.5), (10, 2, 5), **armor, faces={'up': dict(color='armor_l', pattern='mc', clusters=0.3)})
    tail = torso.part('coat_tail', pivot=(0, -15, 2.75), rot=(0.12, 0, 0))
    tail.cube((-4.5, 0, 0), (9, 22, 1), color='cape', pattern='mc', clusters=0.4, faces={
        'north': dict(color='cape', pattern='mc', clusters=0.4, map=['.........'] * 15 + ['..._..._.', '_.._.._..', '_..__.._.', '__.__._._', '_.___.__.', '__.__.___', '_______._']),
        'south': dict(color='cape', pattern='mc', clusters=0.4, map=['.........'] * 15 + ['.._..._..', '.._.._.._', '._.__..._', '._._.__.__', '.__.___._', '___.__.__', '._._______']),
    })
    neck = torso.part('neck', pivot=(0, -16, -0.5), rot=(0.15, 0, 0))
    neck.cube((-1, -4, -1), (2, 4, 2), **skin)
    neck.cube((-2, -2, -1.75), (4, 2, 3.5), **armor)
    head = neck.part('head', pivot=(0, -4, 0))
    head.cube((-3, -10, -3), (6, 10, 5), **skin, faces={
        'north': dict(**skin, hd=True, map=hd_rows(DEVIL['neutral'], 12, 20),
                      keys={'b': 'brow', 'E': 'void', 'e': 'glow', 'n': 'void', 'm': 'void', 'f': 'fang', 't': 'fang', 'c': 'horn'}, glow_keys='e',
                      expr={k: hd_rows(v, 12, 20) for k, v in DEVIL.items() if k != 'neutral'}),
    })
    # chin spike
    head.cube((-0.5, 0, -3), (1, 2, 1), color='horn', pattern='mc', clusters=0.0, rim=False)
    crown = head.part('crown', pivot=(0, -10, 0))
    for side, sx in (('left', 1), ('right', -1)):
        # great curling horns: three segments sweeping out, up and forward
        h1 = crown.part(f'{side}_horn', pivot=(2.2 * sx, 0, -0.5), rot=(-0.2, 0, 0.75 * sx))
        h1.cube((-1, -4, -1), (2, 4, 2), color='horn', pattern='mc', clusters=0.3, faces={'north': dict(color='horn', pattern='mc', clusters=0.2, hd=True,
                                                                                                         map=['dddd', '....', 'dddd', '....', 'dddd', '....', 'dddd', '....'], keys={'d': 'horn_d'})})
        h2 = h1.part(f'{side}_horn_mid', pivot=(0, -4, 0), rot=(-0.35, 0, -0.7 * sx))
        h2.cube((-0.75, -4, -0.75), (1.5, 4, 1.5), color='horn', pattern='mc', clusters=0.2)
        h3 = h2.part(f'{side}_horn_tip', pivot=(0, -4, 0), rot=(-0.6, 0, -0.6 * sx))
        h3.cube((-0.5, -3, -0.5), (1, 3, 1), color='horn_l', pattern='mc', clusters=0.0, rim=False, faces={'up': dict(color='glow', pattern='mc', clusters=0.0, glow=True)})
    for side, sx in (('left', 1), ('right', -1)):
        # four long pointed ears, an upper pair swept up and a lower pair swept back
        for tier, (py, length, rz, ry) in (('upper', (-7.5, 5, -0.45, 0.35)), ('lower', (-4.5, 4, 0.25, 0.6))):
            ear = head.part(f'{side}_ear_{tier}', pivot=(3 * sx, py, 0), rot=(0, ry * sx, rz * sx))
            ear.cube((0 if sx > 0 else -length, -1, -0.25), (length, 2, 0.5), color='skin', pattern='mc', clusters=0.2, rim=False, faces={
                'north': dict(color='skin', pattern='mc', clusters=0.0, rim=False, hd=True, keys={'i': 'skin_l', '_': '_'},
                              map=(['.' * (length * 2)] if sx > 0 else ['.' * (length * 2)]) + [('ii' * length)[:length * 2 - 2] + '__' if sx > 0 else '__' + ('ii' * length)[:length * 2 - 2],
                                                                                              '.' * (length * 2), ('.' * (length * 2 - 3) + '___') if sx > 0 else ('___' + '.' * (length * 2 - 3))]),
            })
    for side, sx in (('left', 1), ('right', -1)):
        arm = torso.part(f'{side}_arm', pivot=(5.5 * sx, -14, 0), rot=(0, 0, -0.08 * sx))
        arm.cube((-1, -1, -1), (2, 12, 2), **skin)
        # spiked pauldron
        arm.cube((-2.5 if sx > 0 else -2.5, -2.5, -2.5), (5, 4, 5), **armor, faces={'up': dict(color='armor_l', pattern='mc', clusters=0.3, hd=True,
                                                                                                 map=['bbbbbbbbbb', 'b........b', 'b...gg...b', 'b..g..g..b', 'b...gg...b', 'b........b',
                                                                                                      'bbbbbbbbbb', '..........', '..........', '..........'],
                                                                                                 keys={'b': 'brass', 'g': 'glow_d'}, glow_keys='g')})
        for k, (spx, spz) in enumerate(((0.0, -1.0), (0.0, 1.0))):
            arm.cube((spx - 0.5 + 1.5 * sx, -5, spz - 0.5), (1, 3, 1), color='horn', pattern='mc', clusters=0.0, rim=False)
        fore = arm.part(f'{side}_forearm', pivot=(0, 11, 0), rot=(-0.15, 0, 0))
        fore.cube((-1, 0, -1), (2, 10, 2), **skin)
        fore.cube((-1.5, 3, -1.5), (3, 6, 3), **armor, faces={'north': dict(**armor, hd=True, map=['bbbbbb', '......', '.llll.', '......', '.llll.', '......',
                                                                                                  '.llll.', '......', '......', '......', '......', 'bbbbbb'],
                                                                         keys={'b': 'brass', 'l': 'armor_l'})})
        hand = fore.part(f'{side}_hand', pivot=(0, 10, 0))
        hand.cube((-1.5, 0, -1.5), (3, 2, 3), color='armor_d', pattern='mc', clusters=0.2)
        for f, fx in enumerate((-1, 0, 1)):
            hand.cube((fx - 0.5, 2, -1.5 + (f % 2)), (1, 4 - (f % 2), 1), color='skin_d', pattern='mc', clusters=0.0, rim=False,
                      faces={'down': dict(color='horn', pattern='mc', clusters=0.0)})
        if sx < 0:
            # the Conductor's Staff (the same one he drops): a twisted shaft bound in gold, a human
            # skull with glowing sockets, and gold prongs rising from it to cradle a sculk orb
            staff = hand.part('baton', pivot=(0, 2, 0), rot=(1.35, 0, 0))
            staff.cube((-0.5, -8, -0.5), (1, 22, 1), color='cape_l', pattern='mc', clusters=0.0, rim=False, faces={
                'north': dict(color='cape_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['bb', '..', '..', '.c', 'c.', '..'] * 7 + ['bb', '..'],
                              keys={'b': 'brass', 'c': 'cape'})})
            staff.cube((-1, 13, -1), (2, 1, 2), color='brass', pattern='mc', clusters=0.0, rim=False)
            skull = staff.part('staff_skull', pivot=(0, 14, 0))
            skull.cube((-2, 0, -2), (4, 4, 4), color='horn_l', pattern='mc', clusters=0.0, faces={
                # the staff points forward from his hand, so the skull's face looks along +y: paint every side
                'north': dict(color='horn_l', pattern='mc', clusters=0.0, hd=True, keys={'v': 'void', 'g': 'glow', 'd': 'horn_d'}, glow_keys='g',
                              map=['........', '.vv..vv.', '.vg..gv.', '.vv..vv.', '...vv...', '........', '.dddddd.', '.d.dd.d.']),
                'down': dict(color='horn_l', pattern='mc', clusters=0.0, hd=True, keys={'v': 'void', 'g': 'glow', 'd': 'horn_d'}, glow_keys='g',
                             map=['........', '.vv..vv.', '.vg..gv.', '.vv..vv.', '...vv...', '........', '.dddddd.', '.d.dd.d.']),
            })
            for sx_ in (1, -1):
                prong = skull.part(f'staff_prong_{"l" if sx_ > 0 else "r"}', pivot=(1.7 * sx_, 3.5, 0), rot=(0, 0, -0.35 * sx_))
                prong.cube((-0.5, 0, -0.5), (1, 5, 1), color='brass_l', pattern='mc', clusters=0.0, rim=False)
                prong.cube((-0.5 - 0.5 * sx_, 4.5, -0.5), (1, 1, 1), color='brass', pattern='mc', clusters=0.0, rim=False)
            skull.cube((-1.25, 5.5, -1.25), (2.5, 2.5, 2.5), color='glow', pattern='mc', clusters=0.0, rim=False, glow=True)
        # threads of song hanging from each hand: he plays the air with them once he rises
        strings = hand.part(f'{side}_strings', pivot=(0, 2, 0))
        strings.cube((0, 0, -1.5), (0, 16, 3), color='glow', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='glow', pattern='mc', clusters=0.0, rim=False, hd=True, map=['g_g_g_' if f == 'east' else '_g_g_g'] * 32, keys={'g': 'glow'}, glow_keys='g')
            for f in ('east', 'west')})
    # --- his own sculk magic, worn as he grows godlike: glowing threads in his hands (levitating) and
    # great wings of soul-feathers (soaring)
    import bosses as BS
    fk = {'d': 'armor_l', 'l': 'horn_l', 'k': 'void', 'i': 'armor_d', '_': '_'}
    for side, sx in (('left', 1), ('right', -1)):
        wing = torso.part(f'{side}_crane_wing', pivot=(2.5 * sx, -14.5, 3), rot=(0.2, -0.5 * sx, 0))
        wing.cube((0 if sx > 0 else -12, -0.5, 0), (12, 1, 10), color='horn', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='horn', pattern='mc', clusters=0.0, hd=True, map=BS.feathers(24, 20, 6, 5, tips='i', trail=3), keys=fk),
            'down': dict(color='horn_d', pattern='mc', clusters=0.0, hd=True, map=BS.feathers(24, 20, 6, 5, tips='i', trail=3), keys=fk),
        })
        tip = wing.part(f'{side}_crane_wing_tip', pivot=(12 * sx, 0, 0))
        prim = BS.primaries(28, 22, sx, ink='i', gap='g', shaft='s')
        tip.cube((0 if sx > 0 else -14, -0.5, 0), (14, 1, 11), color='armor_d', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='armor_d', pattern='mc', clusters=0.0, hd=True, map=prim, keys={'i': 'void', 'g': 'glow_d', 's': 'armor_l', 'l': 'armor_l', 'd': 'armor'}, glow_keys='g'),
            'down': dict(color='armor_d', pattern='mc', clusters=0.0, hd=True, map=prim, keys={'i': 'void', 'g': 'glow_d', 's': 'armor_l', 'l': 'armor_l', 'd': 'armor'}, glow_keys='g'),
        })
    return m


ALL = {'bulb': bulb, 'slumbler': slumbler, 'sifter': sifter, 'enchoer': enchoer, 'riveter': riveter, 'harmoner': harmoner,
       'dictator': dictator}

import bosses  # noqa: E402  (the mini-bosses, their young and the Conductor's Mask)
ALL.update(bosses.ALL)

# the wild creatures (Stomper, music fish, Sky Whale) live in tools/mobs_wild.py
import mobs_wild  # noqa: E402

ALL.update(mobs_wild.ALL)

# the Sculk Parasite lives in tools/parasite.py
import parasite  # noqa: E402

ALL.update(parasite.ALL)
ALL.update(__import__('caravans').MODELS)  # C: the Caravan (tools/caravans.py)

# A2 Echoer: Soul Golems and Nibs (tools/echoer.py)
import echoer  # noqa: E402

ALL.update(echoer.ALL)
