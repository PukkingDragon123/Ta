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


ALL = {'bulb': bulb, 'slumbler': slumbler, 'sifter': sifter, 'enchoer': enchoer, 'riveter': riveter}
