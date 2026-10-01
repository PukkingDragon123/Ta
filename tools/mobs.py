"""Geometry + paint definitions for The Sift's mobs (see modelkit.py)."""
from modelkit import Model


# =========================================================================== BULB
def bulb() -> Model:
    """The Sift bunny: a squishy jelly cube with two tall springy ears and four stubby feet."""
    pal = {
        # top of the body is periwinkle blue, the lower band soft cyan (see the biome reference)
        'skin': '#78a5e3', 'skin_l': '#9cc3f3', 'skin_d': '#5d86cc',
        'belly': '#63c6df', 'belly_l': '#86dbee', 'belly_d': '#4aa9c9',
        'ear': '#78a5e3', 'ear_l': '#9cc3f3', 'ear_d': '#5d86cc', 'ear_in': '#63c6df',
        'foot': '#8fdcee', 'foot_l': '#b3ecf7', 'foot_d': '#6cc0da',
        'eye': '#2f2777', 'mouth': '#4a3a9f', 'gloss': '#e3f3ff', 'drip': '#a9e6f5',
    }
    variants = {
        'bulb_sky': {},
        # the all-cyan "this little guy" colouring
        'bulb_blossom': {'skin': '#3fd0ef', 'skin_l': '#72e3fa', 'skin_d': '#27aed6', 'belly': '#3fd0ef', 'belly_l': '#72e3fa',
                         'belly_d': '#27aed6', 'ear': '#3fd0ef', 'ear_l': '#72e3fa', 'ear_d': '#27aed6', 'ear_in': '#27aed6',
                         'foot': '#5fdcf3', 'foot_l': '#8deafa', 'foot_d': '#36bde0', 'eye': '#1b4f6b', 'mouth': '#1b4f6b'},
        'bulb_dusk': {'skin': '#a58fe6', 'skin_l': '#c3b2f6', 'skin_d': '#8770cf', 'belly': '#e59ad0', 'belly_l': '#f4b9e2', 'belly_d': '#c97bb5',
                      'ear': '#a58fe6', 'ear_l': '#c3b2f6', 'ear_d': '#8770cf', 'ear_in': '#e59ad0',
                      'foot': '#f0b6de', 'foot_l': '#fbd2ee', 'foot_d': '#d895c4', 'eye': '#3a1f5e', 'mouth': '#5b2f7a'},
        'bulb_starry': {'skin': '#3b4aa0', 'skin_l': '#5566c0', 'skin_d': '#2b377d', 'belly': '#4e7fd0', 'belly_l': '#6a9be3', 'belly_d': '#3a66b3',
                        'ear': '#3b4aa0', 'ear_l': '#5566c0', 'ear_d': '#2b377d', 'ear_in': '#4e7fd0',
                        'foot': '#6a9be3', 'foot_l': '#8bb5f0', 'foot_d': '#4e7fd0', 'eye': '#fff1a8', 'mouth': '#fff1a8', 'gloss': '#c9d3ff'},
    }
    m = Model('bulb', (64, 64), pal, variants)
    body = m.part('body', pivot=(0, 24, 0))
    two_tone = dict(color='skin', pattern='mc', bands=[(4, 'belly')], clusters=0.22)
    body.cube((-6, -13, -6), (12, 10, 12), **two_tone, faces={
        'north': dict(color='skin', pattern='mc', bands=[(4, 'belly')], clusters=0.0, map=[
            '............',
            '.g..........',
            '............',
            '............',
            '............',
            '.EEE....EEE.',
            '.EEE....EEE.',
            '.....MM.....',
            '............',
            '............',
        ], keys={'E': 'eye', 'M': 'mouth', 'g': 'gloss'}),
        'up': dict(color='skin', pattern='mc', clusters=0.25, map=[
            '............',
            '.gg.........',
            '.g..........',
        ], keys={'g': 'gloss'}),
        'down': dict(color='belly_d', pattern='mc', clusters=0.2),
    })
    for side, sx in (('left', 1), ('right', -1)):
        # ears sit on the back half of the top, three pixels apart
        ear = body.part(f'{side}_ear', pivot=(3 * sx, -13, 1.5))
        ear.cube((-1.5, -4, -1), (3, 4, 2), color='ear', pattern='mc', clusters=0.15, rim=False, faces={
            'north': dict(color='ear', pattern='mc', clusters=0.0, rim=False, map=['...', '.i.', '.i.', '.i.'], keys={'i': 'ear_in'}),
        })
        tip = ear.part(f'{side}_ear_tip', pivot=(0, -4, 0))
        tip.cube((-1.5, -3, -1), (3, 3, 2), color='ear', pattern='mc', clusters=0.15, rim=False, faces={
            'north': dict(color='ear', pattern='mc', clusters=0.0, rim=False, map=['...', '.i.', '.i.'], keys={'i': 'ear_in'}),
            'up': dict(color='ear_l', pattern='mc', clusters=0.0),
        })
    for name, x, z in (('front_left', 1, -1), ('front_right', -1, -1), ('back_left', 1, 1), ('back_right', -1, 1)):
        leg = body.part(f'{name}_leg', pivot=(3.5 * x, -3, 3.5 * z))
        leg.cube((-1.5, 0, -1.5), (3, 3, 3), color='foot', pattern='mc', clusters=0.4)
    return m


# =========================================================================== SLUMBLER
def slumbler() -> Model:
    pal = {
        'skin': '#9fd6e2', 'skin_d': '#6aa7c4', 'skin_l': '#d2f1f5', 'pearl': '#f0d9f2', 'lilac': '#c9b1ee',
        'belly': '#f3e8f6', 'spot': '#6f8fd0', 'glow': '#a8fbff', 'mouth': '#d9577f', 'mouth_d': '#9c2f55',
        'tongue': '#ff8fb0', 'teeth': '#fff8ec', 'eye': '#1a1d38', 'iris': '#ffd66b', 'eye_hi': '#ffffff',
        'lid': '#7fb6cf', 'gill': '#f59ad0', 'gill_d': '#c85f9f', 'claw': '#eae3f2', 'fin': '#bfe9f5',
    }
    m = Model('slumbler', (128, 128), pal, {'slumbler': {}})
    body = m.part('body', pivot=(0, 14, 0))
    body.cube((-8, -5, -11), (16, 9, 22), color='skin', pattern='spots', accent='spot', density=0.05, faces={
        'up': dict(color='skin', pattern='spots', accent='spot', density=0.07, map=[
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
        'down': dict(color='belly', pattern='scales'),
        'west': dict(color='skin', pattern='bands', accent='lilac', period=5, width=1),
        'east': dict(color='skin', pattern='bands', accent='lilac', period=5, width=1),
    })
    crest = body.part('crest', pivot=(0, -5, 0))
    crest.cube((0, -4, -9), (0, 4, 18), color='fin', pattern='membrane', accent='lilac', rib=3, alpha='membrane', edge='bottom',
               faces={'west': dict(color='fin', pattern='membrane', accent='lilac', rib=3, alpha='frill'),
                      'east': dict(color='fin', pattern='membrane', accent='lilac', rib=3, alpha='frill')})
    head = body.part('head', pivot=(0, -1, -11))
    head.cube((-9, -4, -10), (18, 5, 10), color='skin', pattern='spots', accent='spot', density=0.04, faces={
        'down': dict(color='mouth', pattern='flat', map=[
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
        'north': dict(color='skin', pattern='speckle', map=[
            '..................',
            '..................',
            '...o..........o...',
            '..................',
            'tttttttttttttttttt',
        ], keys={'o': 'skin_d', 't': 'teeth'}),
    })
    jaw = head.part('jaw', pivot=(0, 1, 0))
    jaw.cube((-9, 0, -10), (18, 3, 10), color='belly', pattern='scales', faces={
        'up': dict(color='mouth', pattern='flat', map=[
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
        'north': dict(color='belly', pattern='flat', map=['.t.t.t.t.t.t.t.t.t'], keys={'t': 'teeth'}),
        'west': dict(color='skin', pattern='speckle'),
        'east': dict(color='skin', pattern='speckle'),
    })
    for side, sx in (('left', 1), ('right', -1)):
        eye = head.part(f'{side}_eye', pivot=(5.5 * sx, -4, -6))
        eye.cube((-1.5, -3, -1.5), (3, 3, 3), color='skin', pattern='speckle', faces={
            'north': dict(color='eye', pattern='flat', map=['hi.', 'iii', '.i.'], keys={'i': 'iris', 'h': 'eye_hi'}),
            ('east' if sx > 0 else 'west'): dict(color='eye', pattern='flat', map=['.i.', 'iii', '.i.'], keys={'i': 'iris'}),
        })
        lid = eye.part(f'{side}_eyelid')
        lid.cube((-1.5, -3, -1.5), (3, 3, 3), inflate=0.12, color='lid', pattern='flat', faces={
            'north': dict(color='lid', pattern='flat', map=['...', 'ddd', '...'], keys={'d': 'skin_d'}),
        })
        gills = head.part(f'{side}_gills', pivot=(9 * sx, -3, -3), rot=(0, -0.35 * sx, 0))
        gills.cube((0 if sx > 0 else -5, -5, 0), (5, 8, 0), color='gill', pattern='membrane', accent='gill_d', rib=2, alpha='membrane',
                   edge='outer', edge_depth=2, scallop=2)
    for side, sx in (('left', 1), ('right', -1)):
        for end, sz in (('front', -7), ('hind', 7)):
            leg = body.part(f'{side}_{end}_leg', pivot=(7.5 * sx, 2.5, sz))
            leg.cube((0 if sx > 0 else -5, -1.5, -2), (5, 3, 4), color='skin', pattern='speckle')
            foot = leg.part(f'{side}_{end}_foot', pivot=(4.5 * sx, 0.5, 0))
            foot.cube((-2, 0, -2.5), (4, 7, 5), color='skin_d', pattern='speckle', faces={
                'north': dict(color='skin_d', pattern='speckle', map=['....', '....', '....', '....', '....', 'c.c.', 'c.cc'], keys={'c': 'claw'}),
                'down': dict(color='skin_d', pattern='flat'),
            })
    tail1 = body.part('tail1', pivot=(0, -1, 11))
    tail1.cube((-5, -3, 0), (10, 6, 10), color='skin', pattern='bands', accent='lilac', period=4, width=1, faces={
        'up': dict(color='skin', pattern='spots', accent='spot', density=0.06, map=['..........', '....gg....', '..........', '..........', '....gg....'],
                   keys={'g': 'glow'}, glow_keys='g'),
        'down': dict(color='belly', pattern='scales'),
    })
    tail2 = tail1.part('tail2', pivot=(0, 0, 10))
    tail2.cube((-3.5, -2, 0), (7, 4, 10), color='skin', pattern='bands', accent='lilac', period=4, width=1, phase=2, faces={
        'down': dict(color='belly', pattern='scales'),
        'up': dict(color='skin', pattern='speckle', map=['.......', '...g...', '.......', '.......', '...g...'], keys={'g': 'glow'}, glow_keys='g'),
    })
    tail3 = tail2.part('tail3', pivot=(0, 0, 10))
    tail3.cube((-2, -1.5, 0), (4, 3, 9), color='skin', pattern='speckle', faces={'down': dict(color='belly', pattern='scales')})
    tail3.cube((0, -5, 0), (0, 10, 10), color='fin', pattern='membrane', accent='lilac', rib=3, alpha='membrane', edge='bottom',
               faces={'west': dict(color='fin', pattern='membrane', accent='pearl', rib=3), 'east': dict(color='fin', pattern='membrane', accent='pearl', rib=3)})
    return m


# =========================================================================== SIFTER
def sifter() -> Model:
    """A dune lurker that is mostly mouth: a flat box head whose lid snaps open like a trap,
    on a stubby two-legged body with little fins and tan toes."""
    pal = {
        'skin': '#1fa3c1', 'skin_l': '#3ec0d6', 'skin_d': '#157e9f', 'deep': '#0e5a7d',
        'tan': '#f2cd98', 'tan_l': '#fde3b9', 'tan_d': '#d7a46c',
        'mouth': '#17328c', 'mouth_l': '#2246ad', 'mouth_d': '#0c1e5c',
        'tooth': '#eaf7ff', 'tooth_d': '#a9cfe8', 'eye': '#d9f6ff', 'swirl': '#1892b2',
        'fin': '#0f6a8e', 'fin_l': '#1a86a8', 'fin_d': '#0b4f6d',
    }
    m = Model('sifter', (64, 64), pal, {'sifter': {}})
    body = m.part('body', pivot=(0, 24, 0))
    # torso column the head rests on
    body.cube((-3.5, -14, -2.5), (7, 4, 5), color='skin', pattern='mc', clusters=0.8)
    for side, sx in (('left', 1), ('right', -1)):
        leg = body.part(f'{side}_leg', pivot=(1.75 * sx, -10, 0))
        leg.cube((-1.75, 0, -2), (3.5, 10, 4), color='skin', pattern='mc', clusters=0.9, faces={
            'north': dict(color='skin', pattern='mc', clusters=0.7, map=[
                '...', '...', '...', '...', '...', '...', '...', '...', 't.t', 'TTT'], keys={'t': 'tan', 'T': 'tan_d'}),
            'east': dict(color='skin', pattern='mc', clusters=0.7, map=['....'] * 8 + ['t..t', 'TTTT'], keys={'t': 'tan', 'T': 'tan_d'}),
            'west': dict(color='skin', pattern='mc', clusters=0.7, map=['....'] * 8 + ['t..t', 'TTTT'], keys={'t': 'tan', 'T': 'tan_d'}),
            'south': dict(color='skin', pattern='mc', clusters=0.7, map=['...'] * 9 + ['TTT'], keys={'T': 'tan_d'}),
            'down': dict(color='tan_d', pattern='mc', clusters=0.0),
        })
        fin = body.part(f'{side}_fin', pivot=(3.5 * sx, -13.5, 0), rot=(0, 0, -0.35 * sx))
        fin.cube((0 if sx > 0 else -1, 0, -1.5), (1, 5, 3), color='fin', pattern='mc', clusters=0.5)
    head = body.part('head', pivot=(0, -14, 0))
    # lower jaw: a tan tray with a deep navy floor
    head.cube((-7, -2, -7.5), (14, 2, 14), color='tan', pattern='mc', clusters=0.4, rim=False, faces={
        'up': dict(color='mouth', pattern='mc', clusters=0.5, map=[
            'TTTTTTTTTTTTTT',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'T............T',
            'TTTTTTTTTTTTTT',
        ], keys={'T': 'tan_l'}),
        'down': dict(color='skin_d', pattern='mc', clusters=0.4),
        'north': dict(color='tan', pattern='mc', clusters=0.3, rim=False, map=['..............', 'TTTTTTTTTTTTTT'], keys={'T': 'tan_d'}),
    })
    for i, tx in enumerate((-4.5, -0.5, 3.5)):
        tooth = head.part(f'lower_tooth_{i}', pivot=(tx, -2, -7))
        tooth.cube((0, -1, 0), (1, 1, 1), color='tooth', pattern='mc', clusters=0.0, rim=False)
    # upper jaw: the lid, hinged at the back of the head
    lid = head.part('lid', pivot=(0, -2, 6.5))
    lid.cube((-7, -3, -14), (14, 3, 14), color='skin', pattern='mc', clusters=0.8, faces={
        'up': dict(color='skin', pattern='mc', clusters=0.25, map=[
            '..............',
            '..............',
            '...rrrrrrr....',
            '..r.......r...',
            '..r.rrrrr..r..',
            '..r.r....r.r..',
            '..r.r.rr.r.r..',
            '..r.r..r.r.r..',
            '..r..rr..r.r..',
            '..r.....r..r..',
            '...r...r..r...',
            '....rrr..r....',
            '..........',
            '..............',
        ], keys={'r': 'swirl'}),
        'down': dict(color='mouth', pattern='mc', clusters=0.4),
        'north': dict(color='skin', pattern='mc', clusters=0.2, map=['..............', '.e..........e.', '..............'], keys={'e': 'eye'}),
    })
    for i, (tx, h) in enumerate(((-6, 2), (-3.5, 1), (2.5, 1), (5, 2))):
        tooth = lid.part(f'upper_tooth_{i}', pivot=(tx, 0, -13.5))
        tooth.cube((0, 0, 0), (1, h, 1), color='tooth', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='tooth', pattern='mc', clusters=0.0, rim=False, map=['.', 'd'] if h == 2 else ['.'], keys={'d': 'tooth_d'}),
        })
    return m


# =========================================================================== ENCHOER
def enchoer() -> Model:
    pal = {
        'teal': '#3fb8b0', 'teal_d': '#237f86', 'teal_l': '#8ee8dc', 'crystal': '#a9f5ff', 'crystal_d': '#5fc9e6',
        'robe': '#2d6f86', 'robe_d': '#1f4f66', 'gold': '#ffd97a', 'eye': '#fff8c4', 'eye_d': '#0e2230',
        'gem': '#ff9fd6', 'membrane': '#9ae6e6', 'membrane_d': '#5cb8c8', 'antler': '#dff9ff', 'antler_d': '#8fd3ea',
        'hoof': '#1c3f55', 'snout': '#57c9bd',
    }
    m = Model('enchoer', (128, 64), pal, {'enchoer': {}})
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(2.3 * sx, 10, 0))
        leg.cube((-1.5, 0, -1.5), (3, 14, 3), color='robe', pattern='crystal', faces={
            'north': dict(color='robe', pattern='crystal', map=['...', '...', '...', '...', '...', '...', '...', '...', '...', '...', '...', 'ggg', 'hhh', 'hhh'],
                          keys={'g': 'gold', 'h': 'hoof'}),
            'down': dict(color='hoof', pattern='flat'),
        })
    torso = m.part('torso', pivot=(0, 10, 0))
    torso.cube((-4, -14, -2.5), (8, 14, 5), color='teal', pattern='crystal', faces={
        'north': dict(color='teal', pattern='crystal', map=[
            '........',
            '.l....l.',
            '..l..l..',
            '...GG...',
            '..GggG..',
            '..GggG..',
            '...GG...',
            '........',
            'rrrrrrrr',
            'rRrRrRrR',
            'rrrrrrrr',
            'RrRrRrRr',
            'rrrrrrrr',
            'oooooooo',
        ], keys={'l': 'teal_l', 'G': 'gold', 'g': 'gem', 'r': 'robe', 'R': 'robe_d', 'o': 'gold'}, glow_keys='g'),
        'south': dict(color='teal', pattern='crystal', map=['........'] * 8 + ['rrrrrrrr', 'rRrRrRrR', 'rrrrrrrr', 'RrRrRrRr', 'rrrrrrrr', 'oooooooo'],
                      keys={'r': 'robe', 'R': 'robe_d', 'o': 'gold'}),
        'west': dict(color='teal', pattern='crystal', map=['.....'] * 8 + ['rrrrr', 'rRrRr', 'rrrrr', 'RrRrR', 'rrrrr', 'ooooo'],
                     keys={'r': 'robe', 'R': 'robe_d', 'o': 'gold'}),
        'east': dict(color='teal', pattern='crystal', map=['.....'] * 8 + ['rrrrr', 'rRrRr', 'rrrrr', 'RrRrR', 'rrrrr', 'ooooo'],
                     keys={'r': 'robe', 'R': 'robe_d', 'o': 'gold'}),
    })
    tail = torso.part('tail', pivot=(0, -2, 2.5), rot=(0.6, 0, 0))
    tail.cube((-1.5, -1.5, 0), (3, 3, 6), color='teal', pattern='crystal')
    tail_tip = tail.part('tail_tip', pivot=(0, 0, 6), rot=(0.3, 0, 0))
    tail_tip.cube((-1, -1, 0), (2, 2, 6), color='crystal', pattern='crystal', glow=False, faces={
        'south': dict(color='crystal', pattern='flat', glow=True),
    })
    head = torso.part('head', pivot=(0, -14, -0.5))
    head.cube((-3, -8, -3), (6, 8, 6), color='teal', pattern='crystal', faces={
        'north': dict(color='teal', pattern='crystal', map=[
            '......',
            '.l..l.',
            '......',
            'ee..ee',
            'Ek..kE',
            '......',
            '......',
            '......',
        ], keys={'l': 'teal_l', 'e': 'eye', 'E': 'eye_d', 'k': 'eye_d'}, glow_keys='e'),
    })
    head.cube((-2, -4, -5), (4, 3, 2), color='snout', pattern='crystal', faces={
        'north': dict(color='snout', pattern='flat', map=['....', '.dd.', '....'], keys={'d': 'teal_d'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        ant = head.part(f'{side}_antler', pivot=(2 * sx, -8, 0), rot=(-0.15, 0, 0.35 * sx))
        ant.cube((-0.5, -6, -0.5), (1, 6, 1), color='antler', pattern='crystal')
        ant.cube((0 if sx > 0 else -2, -4, -0.5), (2, 1, 1), color='antler', pattern='flat')
        tip = ant.part(f'{side}_antler_tip', pivot=(0, -6, 0), rot=(0, 0, -0.45 * sx))
        tip.cube((-0.5, -5, -0.5), (1, 5, 1), color='crystal', pattern='crystal', glow=True)
        tip.cube((0 if sx > 0 else -2, -3, -0.5), (2, 1, 1), color='crystal', pattern='flat', glow=True)
        tip.cube((-0.5 if sx > 0 else -0.5, -7, -0.5), (1, 2, 1), color='crystal', pattern='flat', glow=True)
    for side, sx in (('left', 1), ('right', -1)):
        wing = torso.part(f'{side}_wing', pivot=(4.2 * sx, -13, 0.5))
        wing.cube((-1 if sx > 0 else -1, -1, -1), (2, 10, 2), color='teal_d', pattern='crystal', faces={
            'north': dict(color='teal_d', pattern='crystal', map=['gg'], keys={'g': 'gold'}),
        })
        wing.cube((0, 0, 1), (0, 10, 7), color='membrane', pattern='membrane', accent='membrane_d', rib=2, alpha='membrane', edge='bottom',
                  edge_depth=2, scallop=2)
        tip = wing.part(f'{side}_wing_tip', pivot=(0, 9, 0))
        tip.cube((-0.5, 0, -0.5), (1, 8, 1), color='crystal', pattern='crystal')
        tip.cube((0, 0, 0.5), (0, 8, 8), color='membrane', pattern='membrane', accent='crystal_d', rib=2, alpha='membrane', edge='bottom',
                 edge_depth=3, scallop=2)
    return m


# =========================================================================== RIVETER
def riveter() -> Model:
    pal = {
        'bone': '#dcd7ea', 'bone_d': '#a39bbd', 'bone_l': '#f4f1fb', 'sculk': '#0f3945', 'sculk_l': '#1ec8c8',
        'glow': '#5ff5f0', 'maw': '#08161c', 'teeth': '#f7f3e6', 'membrane': '#b7aed0', 'membrane_d': '#7d7299',
        'claw': '#3a3550', 'eye': '#7cfff6',
    }
    m = Model('riveter', (64, 64), pal, {'riveter': {}})
    body = m.part('body', pivot=(0, 5, 0))
    for side, sx in (('left', 1), ('right', -1)):
        body.cube((0.5 if sx > 0 else -2.5, 0, -1), (2, 1, 2), color='claw', pattern='flat')
        body.cube((1 if sx > 0 else -2, 1, -0.5), (1, 3, 1), color='bone_d', pattern='flat')
    body.cube((-3, 4, -2.5), (6, 9, 5), color='bone', pattern='stripes', accent='bone_d', period=2, faces={
        'north': dict(color='bone', pattern='flat', map=[
            '......',
            '.dddd.',
            '......',
            '.dddd.',
            '......',
            '.dssd.',
            '..ss..',
            '..gg..',
            '......',
        ], keys={'d': 'bone_d', 's': 'sculk', 'g': 'glow'}, glow_keys='g'),
        'south': dict(color='bone', pattern='flat', map=['..ss..', '.sgs..', '..ss..', '...s..', '..sgs.', '...s..'],
                      keys={'s': 'sculk', 'g': 'glow'}, glow_keys='g'),
    })
    head = body.part('head', pivot=(0, 13, -0.5))
    head.cube((-3.5, 0, -3.5), (7, 5, 7), color='bone', pattern='speckle', faces={
        'north': dict(color='bone', pattern='speckle', map=[
            '.......',
            '.E...E.',
            '.......',
            'ttttttt',
            '.......',
        ], keys={'E': 'eye', 't': 'teeth'}, glow_keys='E'),
        'down': dict(color='bone_d', pattern='speckle'),
        'up': dict(color='bone_d', pattern='flat'),
    })
    jaw = head.part('jaw', pivot=(0, 4, 2))
    jaw.cube((-3, 0, -5.5), (6, 2, 6), color='bone_d', pattern='speckle', faces={
        'up': dict(color='maw', pattern='flat', map=['......', '.gggg.', '.gGGg.', '.gGGg.', '.gggg.', 't.t.t.'],
                   keys={'g': 'sculk_l', 'G': 'glow', 't': 'teeth'}, glow_keys='gG'),
        'north': dict(color='bone_d', pattern='flat', map=['t.t.t.'], keys={'t': 'teeth'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        horn = head.part(f'{side}_horn', pivot=(3.5 * sx, 1.5, 0), rot=(0, 0, -0.6 * sx))
        horn.cube((-0.5 if sx > 0 else -0.5, 0, -0.5), (1, 5, 1), color='sculk', pattern='flat', faces={
            'north': dict(color='sculk', pattern='flat', map=['.', '.', '.', 'g', 'g'], keys={'g': 'glow'}, glow_keys='g'),
            'down': dict(color='glow', pattern='flat', glow=True),
        })
        horn_tip = horn.part(f'{side}_horn_tip', pivot=(0, 5, 0), rot=(0, 0, 0.5 * sx))
        horn_tip.cube((-0.5, 0, -0.5), (1, 3, 1), color='glow', pattern='flat', glow=True)
    for side, sx in (('left', 1), ('right', -1)):
        wing = body.part(f'{side}_wing', pivot=(3 * sx, 5, 0.5), rot=(0, -1.0 * sx, 0))
        wing.cube((0 if sx > 0 else -5, 0, 0), (5, 13, 0), color='membrane', pattern='membrane', accent='membrane_d', rib=2,
                  alpha='membrane', edge='bottom', edge_depth=3, scallop=2)
        wing.cube((0 if sx > 0 else -5, -0.5, -0.5), (5, 1, 1), color='bone_d', pattern='flat')
        tip = wing.part(f'{side}_wing_tip', pivot=(5 * sx, 0, 0), rot=(0, -1.2 * sx, 0))
        tip.cube((0 if sx > 0 else -6, 0, 0), (6, 12, 0), color='membrane', pattern='membrane', accent='membrane_d', rib=2,
                 alpha='membrane', edge='bottom', edge_depth=4, scallop=2)
        tip.cube((0 if sx > 0 else -6, -0.5, -0.5), (6, 1, 1), color='bone_d', pattern='flat')
    return m


ALL = {'bulb': bulb, 'slumbler': slumbler, 'sifter': sifter, 'enchoer': enchoer, 'riveter': riveter}
