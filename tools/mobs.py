"""Geometry + paint definitions for The Sift's mobs (see modelkit.py)."""
from modelkit import Model


# =========================================================================== BULB
def bulb() -> Model:
    pal = {
        'skin': '#7fe3e6', 'skin_d': '#4fb9c4', 'skin_l': '#c2f7f3', 'belly': '#e9fffb',
        'ear_in': '#ff9ccf', 'cheek': '#ff9ccf', 'eye': '#1b2340', 'eye_hi': '#ffffff',
        'lure': '#fff59a', 'lure_d': '#f5c84c', 'stalk': '#6cc28f', 'mouth': '#2a3a5a', 'foot': '#5fc4cf',
        'star': '#fff7c2',
    }
    variants = {
        'bulb_sky': {},
        'bulb_blossom': {'skin': '#f7a8d2', 'skin_d': '#d77fb3', 'skin_l': '#ffd6ec', 'belly': '#fff0f8', 'ear_in': '#8fe6ec',
                         'cheek': '#ff7fb8', 'lure': '#b8fbff', 'lure_d': '#6fdde8', 'foot': '#e98fc2', 'stalk': '#b06fb0'},
        'bulb_dusk': {'skin': '#b7a3f0', 'skin_d': '#8b77cf', 'skin_l': '#dfd5ff', 'belly': '#f6f1ff', 'ear_in': '#ffc1e4',
                      'cheek': '#ffa9d8', 'lure': '#ffe0a3', 'lure_d': '#f0a860', 'foot': '#9d88e0', 'stalk': '#7d6fc0'},
        'bulb_starry': {'skin': '#3d4396', 'skin_d': '#282c6b', 'skin_l': '#6a70c8', 'belly': '#9aa0ee', 'ear_in': '#f59ad0',
                        'cheek': '#f59ad0', 'eye': '#0b0e22', 'lure': '#fff7c2', 'lure_d': '#ffd36b', 'foot': '#343a88', 'stalk': '#5a4fb0'},
    }
    m = Model('bulb', (64, 64), pal, variants)
    jelly = dict(color='skin', pattern='jelly', shine=True)
    body = m.part('body', pivot=(0, 24, 0))
    face_body = dict(jelly)
    face_body['faces'] = {
        'north': dict(color='skin', pattern='jelly', map=[
            '..bbbbb..',
            '.bbbbbbb.',
            '.bbbbbbb.',
            '..bbbbb..',
            '.........',
            '.........',
        ], keys={'b': 'belly'}),
        'down': dict(color='skin_d', pattern='flat'),
        'south': dict(color='skin', pattern='jelly', map=['.........', '...lll...', '..l...l..'], keys={'l': 'skin_l'}),
    }
    body.cube((-4.5, -6, -4), (9, 6, 8), **face_body)
    head = body.part('head', pivot=(0, -6, -0.5))
    head.cube((-4, -7, -4), (8, 7, 7), color='skin', pattern='jelly', shine=True, faces={
        'north': dict(color='skin', pattern='jelly', map=[
            '........',
            '.hE..hE.',
            '.EE..EE.',
            '.EE..EE.',
            'c..mm..c',
            'c......c',
            '........',
        ], keys={'E': 'eye', 'h': 'eye_hi', 'c': 'cheek', 'm': 'mouth'}),
        'up': dict(color='skin', pattern='jelly', map=['........', '........', '..llll..', '.l....l.'], keys={'l': 'skin_l'}),
    })
    # soft cheek puffs round out the silhouette
    head.cube((-4.5, -4, -3.5), (1, 3, 5), color='skin', pattern='jelly')
    head.cube((3.5, -4, -3.5), (1, 3, 5), color='skin', pattern='jelly')
    lids = head.part('eyelids')
    lids.cube((-4, -6, -4.02), (8, 3, 0), color='skin', pattern='flat', outline=False, faces={
        'north': dict(color='skin', pattern='flat', outline=False, map=['_..__.._', '_..__.._', '_dd__dd_'], keys={'d': 'skin_d'}),
        'south': dict(color='skin', skip=True),
    })
    for side, sx in (('left', 1), ('right', -1)):
        ear = head.part(f'{side}_ear', pivot=(2.3 * sx, -6.5, 0.5), rot=(-0.12, 0, 0.18 * sx))
        ear.cube((-1, -5, -0.5), (2, 5, 1), color='skin', pattern='jelly', faces={
            'north': dict(color='skin', pattern='jelly', map=['..', 'ii', 'ii', 'ii', 'ii'], keys={'i': 'ear_in'}),
        })
        tip = ear.part(f'{side}_ear_tip', pivot=(0, -5, 0))
        tip.cube((-1, -4, -0.5), (2, 4, 1), color='skin', pattern='jelly', faces={
            'north': dict(color='skin', pattern='jelly', map=['_.', 'ii', 'ii', 'ii'] if sx > 0 else ['._', 'ii', 'ii', 'ii'], keys={'i': 'ear_in'}),
            'south': dict(color='skin', pattern='jelly', map=['_.'] if sx > 0 else ['._']),
        })
    stalk = head.part('stalk', pivot=(0, -7, -1.5), rot=(-0.25, 0, 0))
    stalk.cube((-0.5, -3, -0.5), (1, 3, 1), color='stalk', pattern='flat')
    lure = stalk.part('lure', pivot=(0, -3, 0))
    lure.cube((-1, -2, -1), (2, 2, 2), color='lure', pattern='flat', glow=True, outline=False, faces={
        'north': dict(color='lure', pattern='flat', glow=True, map=['w.', '..'], keys={'w': (255, 255, 240, 255)}, glow_keys='w'),
        'down': dict(color='lure_d', pattern='flat', glow=True),
    })
    tail = body.part('tail', pivot=(0, -3, 4))
    tail.cube((-1.5, -1.5, 0), (3, 3, 2), color='belly', pattern='fur')
    for side, sx in (('left', 1), ('right', -1)):
        foot = body.part(f'{side}_foot', pivot=(2.6 * sx, 0, 1.5))
        foot.cube((-1, -1, -2.5), (2, 1, 4), color='foot', pattern='flat', faces={
            'up': dict(color='skin', pattern='jelly'),
            'north': dict(color='foot', pattern='flat', map=['ww'], keys={'w': 'belly'}),
        })
        paw = body.part(f'{side}_paw', pivot=(2.2 * sx, 0, -3.2))
        paw.cube((-1, -1.5, -1), (2, 1.5, 2), color='foot', pattern='flat')
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
    pal = {
        'skin': '#58cbe3', 'skin_d': '#2f86b0', 'skin_l': '#a6ecf6', 'ring': '#2a6fa6', 'belly': '#c5f3f8',
        'mouth': '#ffd23f', 'mouth_d': '#d99a16', 'maw': '#5a1f3a', 'teeth': '#fffbe8', 'eye': '#101a33',
        'pupil': '#fff27a', 'tip': '#ffe07a', 'leg': '#3f9fc5', 'leg_d': '#26719a', 'claw': '#e8f7ff',
    }
    m = Model('sifter', (64, 64), pal, {'sifter': {}})
    body = m.part('body', pivot=(0, 17, 0))
    body.cube((-5, -9, -4.5), (10, 10, 9), color='skin', pattern='spots', accent='ring', density=0.08, faces={
        'north': dict(color='skin', pattern='speckle', map=[
            '..........',
            '.ee....ee.',
            'eppe..eppe',
            '.ee....ee.',
            '..........',
            '.mmmmmmmm.',
            'mMMMMMMMMm',
            'mTMTMTMTMm',
            'mMMMMMMMMm',
            '.mmmmmmmm.',
        ], keys={'e': 'eye', 'p': 'pupil', 'm': 'mouth', 'M': 'maw', 'T': 'teeth'}, glow_keys='p'),
        'down': dict(color='belly', pattern='flat'),
    })
    body.cube((-4, -12, -3.5), (8, 3, 7), color='skin', pattern='spots', accent='ring', density=0.12, faces={
        'up': dict(color='skin', pattern='spots', accent='ring', density=0.12, shine=True),
    })
    jaw = body.part('jaw', pivot=(0, 0, -4.5))
    jaw.cube((-4.5, -1, -2), (9, 3, 3), color='mouth', pattern='flat', faces={
        'up': dict(color='maw', pattern='flat', map=['T.T.T.T.T', '.........', '.........'], keys={'T': 'teeth'}),
        'north': dict(color='mouth', pattern='flat', map=['.........', '.........', 'ddddddddd'], keys={'d': 'mouth_d'}),
    })
    for side, sx in (('left', 1), ('right', -1)):
        ant = body.part(f'{side}_antenna', pivot=(3.2 * sx, -11.5, -0.5), rot=(-0.25, 0, 0.45 * sx))
        ant.cube((-0.5, -6, -0.5), (1, 6, 1), color='skin_d', pattern='flat')
        tip = ant.part(f'{side}_antenna_tip', pivot=(0, -6, 0))
        tip.cube((-1.5, -3, -0.5), (3, 3, 1), color='tip', pattern='flat', glow=True, faces={
            'north': dict(color='tip', pattern='flat', glow=True, map=['_._', '...', '_._'] , keys={}),
            'south': dict(color='tip', pattern='flat', glow=True, map=['_._', '...', '_._'], keys={}),
        })
    for i, tx in enumerate((-3, -1, 1, 3)):
        t = body.part(f'tentacle_{i}', pivot=(tx, 0.5, -2.5 + abs(tx) * 0.3))
        t.cube((-0.5, 0, -0.5), (1, 4, 1), color='skin_l', pattern='flat', faces={
            'north': dict(color='skin_l', pattern='flat', map=['.', '.', '.', 'd'], keys={'d': 'ring'}),
        })
    for side, sx in (('left', 1), ('right', -1)):
        for end, sz in (('front', -2.8), ('hind', 2.8)):
            leg = body.part(f'{side}_{end}_leg', pivot=(4.2 * sx, -0.5, sz), rot=(0, 0, -0.35 * sx))
            leg.cube((-1, 0, -1), (2, 8, 2), color='leg', pattern='stripes', accent='leg_d', period=3, faces={
                'north': dict(color='leg', pattern='stripes', accent='leg_d', period=3, map=['..', '..', '..', '..', '..', '..', '..', 'cc'],
                              keys={'c': 'claw'}),
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
