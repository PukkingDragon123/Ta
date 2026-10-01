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
    two_tone = dict(color='skin', pattern='mc', bands=[(6, 'belly')], clusters=0.22)
    body.cube((-6, -13, -6), (12, 10, 12), **two_tone, faces={
        'north': dict(color='skin', pattern='mc', bands=[(6, 'belly')], clusters=0.0, map=[
            '............',
            '.g..........',
            '............',
            '............',
            '............',
            '............',
            '..EEE..EEE..',
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
        'skin': '#8fd0dc', 'skin_d': '#6aaec4', 'skin_l': '#b4e4ec', 'pearl': '#f0d9f2', 'lilac': '#c3a9ec',
        'belly': '#efe3f3', 'belly_l': '#fbf4fc', 'belly_d': '#d5c3df', 'spot': '#6d8fd3', 'glow': '#a8fbff',
        'mouth': '#d9577f', 'mouth_d': '#9c2f55', 'lid_d': '#5f9cb8', 'gill_l': '#ffc0e4',
        'tongue': '#ff8fb0', 'teeth': '#fff8ec', 'eye': '#1a1d38', 'iris': '#ffd66b', 'eye_hi': '#ffffff',
        'lid': '#7fb6cf', 'gill': '#f59ad0', 'gill_d': '#c85f9f', 'claw': '#eae3f2', 'fin': '#bfe9f5',
    }
    m = Model('slumbler', (128, 128), pal, {'slumbler': {}})
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
        eye.cube((-1.5, -3, -1.5), (3, 3, 3), color='skin', pattern='mc', clusters=0.3, faces={
            'north': dict(color='iris', pattern='mc', clusters=0.0, rim=False, map=['hE.', '.E.', '.E.'], keys={'E': 'eye', 'h': 'eye_hi'}),
            ('east' if sx > 0 else 'west'): dict(color='iris', pattern='mc', clusters=0.0, rim=False, map=['.E.', '.E.', '.E.'], keys={'E': 'eye'}),
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
        'up': dict(color='skin', pattern='mc', clusters=1.3, spots=0.35, accent='swirl'),
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
    """A big, gentle, melancholy wanderer: a pear-shaped mound of mint fur with a pale sad face, broad
    moose antlers, long arms ending in dark paws and two stumpy dark feet."""
    pal = {
        'fur': '#a3dcc5', 'fur_l': '#c3ecd8', 'fur_d': '#7fbcab',
        'face': '#d5dfd4', 'face_l': '#e9f0e6', 'face_d': '#aebdb2',
        'brow': '#4d6870', 'lash': '#3a5059', 'nose': '#aebdb4', 'mouth': '#6a807b',
        'paw': '#34507a', 'paw_l': '#466a9c', 'paw_d': '#243a5c', 'claw': '#e9e2c8',
        'antler': '#efe2b2', 'antler_l': '#fbf3d2', 'antler_d': '#c9b784',
    }
    m = Model('enchoer', (128, 128), pal, {'enchoer': {}})
    fur = dict(color='fur', pattern='mc', clusters=0.2, streaks=0.45)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(3.5 * sx, 18, 0.5))
        leg.cube((-2.5, -1, -2.5), (5, 5, 5), **fur, fringe=2)
        leg.cube((-3, 4, -3.5), (6, 2, 6), color='paw', pattern='mc', clusters=0.3, rim=False, faces={
            'north': dict(color='paw', pattern='mc', clusters=0.0, rim=False, map=['......', 'c.c.c.'], keys={'c': 'claw'}),
            'up': dict(color='paw_l', pattern='mc', clusters=0.3),
        })
    body = m.part('body', pivot=(0, 18, 0))
    # the big furry skirt, a narrower chest above it and a hunched hump behind the head
    body.cube((-6.5, -13, -5), (13, 14, 10), **fur, fringe=2)
    body.cube((-5.5, -21, -4.5), (11, 9, 9), **fur, fringe=2, fringe_phase=3)
    body.cube((-4.5, -23, -2), (9, 3, 7), **fur, rim=False)
    head = body.part('head', pivot=(0, -19, -4))
    head.cube((-4.5, -9, -4.5), (9, 10, 8), **fur, fringe=1)
    # the pale, long, sad face sits inside the fur hood
    head.cube((-3, -7, -5.5), (6, 8, 1), color='face', pattern='mc', clusters=0.15, rim=False, faces={
        'north': dict(color='face', pattern='mc', clusters=0.0, rim=False, map=[
            '......',
            '.b..b.',
            'b....b',
            'ee..ee',
            '..nn..',
            '..nn..',
            '..mm..',
            '.m..m.',
        ], keys={'b': 'brow', 'e': 'lash', 'n': 'nose', 'm': 'mouth'}),
    })
    head.cube((-1, -3, -6.5), (2, 2, 1), color='face_d', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='face', pattern='mc', clusters=0.0, rim=False, map=['..', 'dd'], keys={'d': 'nose'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        ant = head.part(f'{side}_antler', pivot=(4 * sx, -7.5, -0.5), rot=(0, 0, -0.32 * sx))
        o = (lambda x0, w: x0 if sx > 0 else -x0 - w)  # mirror an x span for the right antler
        ant.cube((o(0, 3), -1.5, -1), (3, 2, 2), color='antler', pattern='mc', clusters=0.3, rim=False)
        # a broad, flat moose palm reaching outwards, short tines along its upper edge
        ant.cube((o(2, 7), -3.5, -1.5), (7, 3, 3), color='antler', pattern='mc', clusters=0.4, rim=False, faces={
            'up': dict(color='antler_l', pattern='mc', clusters=0.4),
            'north': dict(color='antler', pattern='mc', clusters=0.2, rim=False, map=['.......', '.......', 'ddddddd'], keys={'d': 'antler_d'}),
            'south': dict(color='antler', pattern='mc', clusters=0.2, rim=False, map=['.......', '.......', 'ddddddd'], keys={'d': 'antler_d'}),
        })
        for px, ph in ((3, 1), (5, 2), (7, 1), (8, 2)):
            ant.cube((o(px, 1), -3.5 - ph, -1), (1, ph, 1), color='antler', pattern='mc', clusters=0.0, rim=False,
                     faces={'up': dict(color='antler_l', pattern='mc', clusters=0.0)})
        ant.cube((o(9, 1), -2.5, -1), (1, 1, 2), color='antler', pattern='mc', clusters=0.0, rim=False)
    for side, sx in (('left', 1), ('right', -1)):
        arm = body.part(f'{side}_arm', pivot=(7 * sx, -18.5, -1), rot=(0, 0, -0.08 * sx))
        arm.cube((-1.5 if sx > 0 else -2.5, -1, -2.5), (4, 9, 5), **fur, fringe=1)
        fore = arm.part(f'{side}_forearm', pivot=(0.5 * sx, 8, 0))
        fore.cube((-2, -1, -2.5), (4, 7, 5), **fur, fringe=2)
        fore.cube((-2, 5, -2.5), (4, 3, 5), color='paw', pattern='mc', clusters=0.3, faces={
            'north': dict(color='paw', pattern='mc', clusters=0.0, map=['....', '....', 'c.c.'] if sx > 0 else ['....', '....', '.c.c'],
                          keys={'c': 'claw'}),
            'down': dict(color='paw_d', pattern='mc', clusters=0.0, map=['c..c', '....', '....', '....', 'c..c'], keys={'c': 'claw'}),
        })
    return m


# =========================================================================== RIVETER
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
    m = Model('riveter', (64, 64), pal, {'riveter': {}})
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
        'north': dict(color='hide', pattern='mc', clusters=0.0, map=[
            'BBBBB',
            'e.B.e',
            'ee.ee',
            '.....',
        ], keys={'e': 'eye', 'B': 'bone'}, glow_keys='e'),
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
}


def _shades(hexc):
    import colorsys
    r, g, b = (int(hexc[i:i + 2], 16) / 255 for i in (1, 3, 5))
    h, l, s_ = colorsys.rgb_to_hls(r, g, b)

    def mk(dl):
        rr, gg, bb = colorsys.hls_to_rgb(h, max(0, min(1, l + dl)), s_)
        return '#%02x%02x%02x' % (int(rr * 255), int(gg * 255), int(bb * 255))
    return mk(0.0), mk(0.1), mk(-0.12)


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
    m = Model('harmoner', (64, 64), pal, variants)
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
        'north': dict(color='body', pattern='mc', clusters=0.0, map=['.....', '.....', 'e...e', '.....', '.....'], keys={'e': 'eye'}),
        'east': dict(color='body', pattern='mc', clusters=0.0, map=['.....', '.....', '.Ee..', '.ee..', '.....'], keys={'e': 'eye', 'E': 'eye_hi'}),
        'west': dict(color='body', pattern='mc', clusters=0.0, map=['.....', '.....', '..eE.', '..ee.', '.....'], keys={'e': 'eye', 'E': 'eye_hi'}),
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


def dictator() -> Model:
    """The Dictator: a tall, starved conductor of sculk. Long digitigrade legs, a tailcoat with
    tarnished brass epaulettes, arms that reach past his knees, a white baton, a porcelain mask with
    hollow glowing eyes and a stitched slit of a mouth, and a crown of sculk tines."""
    pal = dict(SCULK)
    pal.update({'coat': '#1a2a3e', 'coat_l': '#27405c', 'coat_d': '#101a28', 'brass': '#b89a52', 'brass_l': '#dcc27a', 'brass_d': '#7f6a35',
                'mask': '#e6e1d3', 'mask_l': '#f6f2e8', 'mask_d': '#b9b2a0', 'shirt': '#c9c2b0'})
    m = Model('dictator', (128, 128), pal, {'dictator': {}})
    hide = dict(color='hide', pattern='mc', clusters=0.5)
    coat = dict(color='coat', pattern='mc', clusters=0.4)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(2.2 * sx, 4, 0), rot=(-0.2, 0, 0))
        leg.cube((-1, 0, -1), (2, 10, 2), **hide, faces={'north': dict(**hide, map=['..'] * 8 + ['bb', 'bb'], keys={'b': 'bone_d'})})
        shin = leg.part(f'{side}_shin', pivot=(0, 10, 0), rot=(0.45, 0, 0))
        shin.cube((-1, 0, -1), (2, 10, 2), **hide)
        foot = shin.part(f'{side}_foot', pivot=(0, 10, 0), rot=(-0.25, 0, 0))
        foot.cube((-1.5, 0, -4), (3, 1, 5), **hide, faces={'north': dict(**hide, map=['b.b'], keys={'b': 'bone'})})
    body = m.part('body', pivot=(0, 4, 0))
    body.cube((-3, -2, -1.5), (6, 2, 3), **hide)
    torso = body.part('torso', pivot=(0, -2, 0), rot=(0.12, 0, 0))
    torso.cube((-2, -6, -1.5), (4, 6, 3), **hide)
    torso.cube((-3.5, -14, -2.5), (7, 8, 5), **coat, faces={
        'north': dict(**coat, map=[
            '..sss..',
            '..sss..',
            '.bsssb.',
            '..sss..',
            '.bsssb.',
            '..sss..',
            '.bsssb.',
            '...s...',
        ], keys={'s': 'shirt', 'b': 'brass'}),
    })
    torso.cube((-5, -15, -2.5), (10, 2, 5), **coat, faces={'up': dict(color='coat_l', pattern='mc', clusters=0.3)})
    for sx in (1, -1):
        torso.cube((3 if sx > 0 else -6, -16, -2), (3, 1, 4), color='brass', pattern='mc', clusters=0.3, rim=False,
                   faces={'up': dict(color='brass_l', pattern='mc', clusters=0.2)})
    tail = torso.part('coat_tail', pivot=(0, -7, 2.5), rot=(0.15, 0, 0))
    tail.cube((-3.5, 0, 0), (7, 15, 1), **coat, faces={
        'north': dict(**coat, map=['.......'] * 8 + ['...' + '_' + '...'] * 7),
        'south': dict(**coat, map=['.......'] * 8 + ['...' + '_' + '...'] * 7),
    })
    neck = torso.part('neck', pivot=(0, -15, -0.5), rot=(0.15, 0, 0))
    neck.cube((-1, -4, -1), (2, 4, 2), **hide)
    head = neck.part('head', pivot=(0, -4, 0))
    head.cube((-2.5, -9, -2.5), (5, 9, 5), **hide)
    head.cube((-2.5, -8, -3.5), (5, 8, 1), color='mask', pattern='mc', clusters=0.15, faces={
        'north': dict(color='mask', pattern='mc', clusters=0.0, map=[
            '.....',
            '.....',
            'Ee.eE',
            'EE.EE',
            '.....',
            '.sms.',
            '..m..',
            '.sms.',
        ], keys={'E': 'void', 'e': 'glow', 's': 'mask_d', 'm': 'void'}, glow_keys='e'),
    })
    crown = head.part('crown', pivot=(0, -9, 0))
    for i, (cx, cz, h, lean_x, lean_z) in enumerate(((0, -1.5, 7, -0.3, 0), (-2, -0.5, 6, -0.15, 0.45), (2, -0.5, 6, -0.15, -0.45),
                                                     (-1.5, 1.5, 5, 0.35, 0.35), (1.5, 1.5, 5, 0.35, -0.35))):
        tine = crown.part(f'tine_{i}', pivot=(cx, 0, cz), rot=(lean_x, 0, lean_z))
        tine.cube((-0.5, -h, -0.5), (1, h, 1), color='hide', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='hide', pattern='mc', clusters=0.0, rim=False, map=['g'] + ['.'] * (h - 1), keys={'g': 'glow'}, glow_keys='g'),
            'south': dict(color='hide', pattern='mc', clusters=0.0, rim=False, map=['g'] + ['.'] * (h - 1), keys={'g': 'glow'}, glow_keys='g'),
            'up': dict(color='glow', pattern='mc', clusters=0.0, glow=True),
        })
    for side, sx in (('left', 1), ('right', -1)):
        arm = torso.part(f'{side}_arm', pivot=(5 * sx, -14, 0), rot=(0, 0, -0.08 * sx))
        arm.cube((-1 if sx > 0 else -1, -1, -1), (2, 11, 2), **coat)
        fore = arm.part(f'{side}_forearm', pivot=(0, 10, 0), rot=(-0.15, 0, 0))
        fore.cube((-1, 0, -1), (2, 10, 2), **hide, faces={'north': dict(**hide, map=['bb', 'bb'] + ['..'] * 8, keys={'b': 'brass'})})
        hand = fore.part(f'{side}_hand', pivot=(0, 10, 0))
        hand.cube((-1.5, 0, -1.5), (3, 2, 3), color='bone', pattern='mc', clusters=0.2)
        for f, fx in enumerate((-1, 0, 1)):
            hand.cube((fx - 0.5, 2, -1.5 + (f % 2)), (1, 4 - (f % 2), 1), color='bone', pattern='mc', clusters=0.0, rim=False,
                      faces={'down': dict(color='bone_d', pattern='mc', clusters=0.0)})
        if sx < 0:
            baton = hand.part('baton', pivot=(0, 2, 0), rot=(1.35, 0, 0))
            baton.cube((-0.5, 0, -0.5), (1, 13, 1), color='bone_l', pattern='mc', clusters=0.0, rim=False, faces={
                'north': dict(color='bone_l', pattern='mc', clusters=0.0, rim=False, map=['dd'[0]] * 2 + ['.'] * 9 + ['g', 'g'],
                              keys={'d': 'hide', 'g': 'glow'}, glow_keys='g'),
                'down': dict(color='glow', pattern='mc', clusters=0.0, glow=True),
            })
    return m


def enforcer() -> Model:
    """Enforcer (percussion): a squat living drum. The barrel of its body is laced with sinew
    cords, its top is a taut membrane veined with sculk, three slit eyes glow on the shell and its
    long arms end in bone mallets it beats against itself."""
    pal = dict(SCULK)
    pal.update({'skin': '#e2d5b8', 'skin_l': '#f0e6cf', 'skin_d': '#c4b494', 'cord': '#7a2a3a', 'cord_l': '#9c3a4e', 'cord_d': '#561c29',
                'shell': '#22324a', 'shell_l': '#2f4563', 'shell_d': '#162133'})
    m = Model('enforcer', (128, 64), pal, {'enforcer': {}})
    hide = dict(color='hide', pattern='mc', clusters=0.5)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(4 * sx, 18, 0))
        leg.cube((-2, 0, -2), (4, 6, 4), **hide, faces={'north': dict(**hide, map=['....', '.bb.'] + ['....'] * 4, keys={'b': 'bone'})})
    body = m.part('body', pivot=(0, 18, 0))
    lace = ['c.....c.....c.', '.c...c.c...c..', '..c.c...c.c...', '...c.....c....', '..c.c...c.c...', '.c...c.c...c..',
            'c.....c.....c.', '.c...c.c...c..', '..c.c...c.c...', '...c.....c....', '..c.c...c.c...', '.c...c.c...c..']
    shell = dict(color='shell', pattern='mc', clusters=0.4)
    body.cube((-7, -13, -6), (14, 12, 12), **shell, faces={
        'north': dict(**shell, map=[
            '..............',
            '..............',
            '..EE..EE..EE..',
            '..ee..ee..ee..',
            '..............',
            '..............',
            '..............',
            '..............',
            '..............',
            '..............',
            '..............',
            '..............',
        ], keys={'E': 'void', 'e': 'glow'}, glow_keys='e'),
        'east': dict(**shell, map=lace[:12], keys={'c': 'cord'}),
        'west': dict(**shell, map=lace[:12], keys={'c': 'cord'}),
        'south': dict(**shell, map=lace[:12], keys={'c': 'cord'}),
    })
    # the drum head: a taut membrane veined with sculk, in a bone hoop
    body.cube((-7.5, -14, -6.5), (15, 1, 13), color='bone', pattern='mc', clusters=0.2, rim=False, faces={
        'up': dict(color='skin', pattern='mc', clusters=0.25, map=[
            'BBBBBBBBBBBBBBB',
            'B.............B',
            'B..v.......v..B',
            'B...v.....v...B',
            'B....v...v....B',
            'B.....vvv.....B',
            'B......g......B',
            'B.....vvv.....B',
            'B....v...v....B',
            'B...v.....v...B',
            'B..v.......v..B',
            'B.............B',
            'BBBBBBBBBBBBBBB',
        ], keys={'B': 'bone', 'v': 'glow_d', 'g': 'glow'}, glow_keys='g'),
    })
    body.cube((-7.5, -2, -6.5), (15, 1, 13), color='bone', pattern='mc', clusters=0.2, rim=False)
    jaw = body.part('jaw', pivot=(0, -4, -6))
    jaw.cube((-5, 0, -1), (10, 2, 2), color='bone_d', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='void', pattern='mc', clusters=0.0, rim=False, map=['t.t.t.t.t.', '.t.t.t.t.t'], keys={'t': 'bone'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        arm = body.part(f'{side}_arm', pivot=(7.5 * sx, -11, 0), rot=(0, 0, -0.15 * sx))
        arm.cube((0 if sx > 0 else -3, -1, -1.5), (3, 12, 3), **hide)
        mallet = arm.part(f'{side}_mallet', pivot=(1.5 * sx, 11, 0))
        mallet.cube((-2.5, 0, -2.5), (5, 4, 5), color='bone', pattern='mc', clusters=0.3, faces={
            'down': dict(color='bone_d', pattern='mc', clusters=0.2, map=['.....', '.ggg.', '.g.g.', '.ggg.', '.....'], keys={'g': 'glow_d'}),
        })
    return m


def resonator() -> Model:
    """Resonator (strings): a tall, spidery harp of a creature. Its body is a bone frame strung
    with glowing sculk tendons, topped by a curled scroll of a head, and two long arms are strung
    to its sides like the strings of a lyre. It stalks on four splayed legs."""
    pal = dict(SCULK)
    pal.update({'string': '#3ff0e0', 'string_d': '#1a8f88', 'peg': '#8a6a3c'})
    m = Model('resonator', (64, 64), pal, {'resonator': {}})
    hide = dict(color='hide', pattern='mc', clusters=0.5)
    for i, (sx, sz) in enumerate(((1, -1), (-1, -1), (1, 1), (-1, 1))):
        name = ('front' if sz < 0 else 'hind') + ('_left' if sx > 0 else '_right')
        leg = m.part(f'{name}_leg', pivot=(1.5 * sx, 12, 1.5 * sz), rot=(0.45 * sz, 0, -0.75 * sx))
        leg.cube((-0.5, 0, -0.5), (1, 9, 1), **hide)
        shin = leg.part(f'{name}_shin', pivot=(0, 9, 0), rot=(-0.4 * sz, 0, 1.2 * sx))
        shin.cube((-0.5, 0, -0.5), (1, 10, 1), color='bone', pattern='mc', clusters=0.0, rim=False,
                  faces={'down': dict(color='bone_d', pattern='mc', clusters=0.0)})
    body = m.part('body', pivot=(0, 12, 0))
    body.cube((-2, -2, -2), (4, 3, 4), **hide)
    frame = body.part('frame', pivot=(0, -2, 0))
    # the harp frame: two bone posts, a curved top bar and the strings between them
    frame.cube((-3.5, -14, -0.5), (1, 14, 1), color='bone', pattern='mc', clusters=0.0, rim=False)
    frame.cube((2.5, -14, -0.5), (1, 14, 1), color='bone', pattern='mc', clusters=0.0, rim=False)
    frame.cube((-3.5, -15, -1), (7, 1, 2), color='bone', pattern='mc', clusters=0.2, rim=False)
    frame.cube((-2.5, -14, 0), (5, 14, 0), color='string', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='string', pattern='mc', clusters=0.0, rim=False, map=['._._.'] * 14, keys={}, glow_keys=''),
        'south': dict(color='string', pattern='mc', clusters=0.0, rim=False, map=['._._.'] * 14, keys={}, glow_keys=''),
    })
    frame.cube((-1, -12, -1.5), (2, 10, 1), **hide)
    head = frame.part('head', pivot=(0, -15, 0))
    # a violin scroll: a neck block curling forward into a spiral knob, with tuning pegs
    head.cube((-1, -4, -1), (2, 4, 2), **hide)
    head.cube((-1.5, -7, -2.5), (3, 3, 3), **hide, faces={
        'north': dict(color='hide', pattern='mc', clusters=0.0, map=['g.g', '...', '.v.'], keys={'g': 'glow', 'v': 'void'}, glow_keys='g'),
    })
    head.cube((-1, -8, -4), (2, 2, 2), color='hide_l', pattern='mc', clusters=0.0, rim=False)
    for py, sx in ((-2, 1), (-3, -1), (-1, -1)):
        head.cube((1 if sx > 0 else -2, py, -0.5), (1, 1, 1), color='peg', pattern='mc', clusters=0.0, rim=False)
    for side, sx in (('left', 1), ('right', -1)):
        arm = frame.part(f'{side}_arm', pivot=(3.5 * sx, -13, 0), rot=(0, 0, -0.35 * sx))
        arm.cube((-0.5, 0, -0.5), (1, 12, 1), **hide)
        fore = arm.part(f'{side}_forearm', pivot=(0, 12, 0), rot=(0, 0, 0.5 * sx))
        fore.cube((-0.5, 0, -0.5), (1, 9, 1), color='bone', pattern='mc', clusters=0.0, rim=False)
        fore.cube((-1, 9, -1), (2, 1, 2), color='bone_d', pattern='mc', clusters=0.0, rim=False)
        # strings stretched from the arm back to the frame post
        arm.cube((-3.5 if sx > 0 else 0.5, 1, 0), (3, 10, 0), color='string', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='string', pattern='mc', clusters=0.0, rim=False, map=['._.'] * 10),
            'south': dict(color='string', pattern='mc', clusters=0.0, rim=False, map=['._.'] * 10),
        })
    return m


def howler() -> Model:
    """Howler (wind): a hunched, four-legged pipe organ of sculk. A row of bone pipes rises from its
    back, glowing in their throats as it draws breath, and its head is a flared horn bell."""
    pal = dict(SCULK)
    m = Model('howler', (128, 64), pal, {'howler': {}})
    hide = dict(color='hide', pattern='mc', clusters=0.5)
    for name, sx, sz in (('front_left', 1, -1), ('front_right', -1, -1), ('hind_left', 1, 1), ('hind_right', -1, 1)):
        leg = m.part(f'{name}_leg', pivot=(3.5 * sx, 14, 5 * sz))
        leg.cube((-1.5, 0, -1.5), (3, 10, 3), **hide, faces={'north': dict(**hide, map=['...'] * 8 + ['b.b', 'bbb'], keys={'b': 'bone'})})
    body = m.part('body', pivot=(0, 14, 0), rot=(-0.12, 0, 0))
    body.cube((-5, -8, -7), (10, 8, 14), **hide, faces={
        'up': dict(color='hide_l', pattern='mc', clusters=0.4),
        'down': dict(color='hide_d', pattern='mc', clusters=0.2),
    })
    pipes = body.part('pipes', pivot=(0, -8, 0))
    for i, (px, pz, h) in enumerate(((-3, -3, 7), (-1, -4, 11), (1, -4, 13), (3, -3, 9), (-2, 1, 8), (0, 1, 14), (2, 1, 10))):
        pipe = pipes.part(f'pipe_{i}', pivot=(px, 0, pz))
        pipe.cube((-1, -h, -1), (2, h, 2), color='bone', pattern='mc', clusters=0.2, faces={
            'up': dict(color='void', pattern='mc', clusters=0.0, map=['gg', 'gg'], keys={'g': 'glow_d'}, glow_keys='g'),
            'north': dict(color='bone', pattern='mc', clusters=0.0, map=['dd', '..', '..', 'vv'] + ['..'] * (h - 4), keys={'d': 'bone_d', 'v': 'void'}),
        })
    head = body.part('head', pivot=(0, -4, -7))
    head.cube((-2.5, -2.5, -5), (5, 5, 5), **hide, faces={
        'east': dict(**hide, map=['.....', '.g...', '.....'], keys={'g': 'glow'}, glow_keys='g'),
        'west': dict(**hide, map=['.....', '...g.', '.....'], keys={'g': 'glow'}, glow_keys='g'),
    })
    bell = head.part('bell', pivot=(0, 0, -5))
    bell.cube((-4, -4, -2), (8, 8, 2), color='bone', pattern='mc', clusters=0.2, faces={
        'north': dict(color='bone', pattern='mc', clusters=0.0, map=[
            '.dddddd.',
            'dvvvvvvd',
            'dvvvvvvd',
            'dvvggvvd',
            'dvvggvvd',
            'dvvvvvvd',
            'dvvvvvvd',
            '.dddddd.',
        ], keys={'d': 'bone_d', 'v': 'void', 'g': 'glow_d'}, glow_keys='g'),
    })
    tail = body.part('tail', pivot=(0, -5, 7), rot=(0.6, 0, 0))
    tail.cube((-1, -1, 0), (2, 2, 6), color='bone', pattern='mc', clusters=0.2, faces={
        'south': dict(color='void', pattern='mc', clusters=0.0),
    })
    return m


ALL = {'bulb': bulb, 'slumbler': slumbler, 'sifter': sifter, 'enchoer': enchoer, 'riveter': riveter, 'harmoner': harmoner,
       'dictator': dictator, 'enforcer': enforcer, 'resonator': resonator, 'howler': howler}
