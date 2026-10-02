"""The Gobbler (sea & sky, agent F): a huge-mouthed, blind deep-sea catfish in the Warden's visual
language - dark teal hide, pale bone plates, a ribcage over glowing trapped souls, glowing tendril
barbels and rows of blind sensory pits where its eyes should be.

Registered into mobs_wild.ALL (one line at its bottom); GobblerModel.java animates it.
"""
from modelkit import Model
from mobs_wild import mc

PAL = {
    'hide': '#0f3a40', 'hide_l': '#1a5560', 'hide_d': '#07222a', 'belly': '#1b4d52', 'belly_l': '#25646a', 'belly_d': '#123a40',
    'bone': '#d8d2bf', 'bone_l': '#f0ead8', 'bone_d': '#a39c88', 'glow': '#3ff5e6', 'glow2': '#c8fffb', 'soul': '#5ff8ff', 'soul_d': '#0f8f99',
    'throat': '#07131a', 'throat_d': '#03080b', 'gullet': '#29dfeb', 'tooth': '#efe8d2', 'tooth_d': '#b8b09a', 'pit': '#020608',
    'barbel': '#1f6670', 'barbel_l': '#2e8c96', 'barbel_d': '#103a42', 'fin': '#1f8a8a', 'fin_d': '#0f4a50', 'fin_l': '#3fd8d0',
}


def _pits(cols, rows, every=3, rim_row=None):
    """Rows of blind sensory pits: a black hollow ringed with faint glow, set into bone."""
    out = []
    for y in range(rows):
        r = ''
        for x in range(cols):
            if y % 3 == 1 and x % every == 1 and 0 < x < cols - 1:
                r += 'o'
            elif y % 3 == 1 and x % every in (0, 2) and 0 < x < cols - 1:
                r += 'g'
            elif y == rim_row:
                r += 'b'
            else:
                r += '.'
        out.append(r)
    return out


def gobbler() -> Model:
    m = Model('gobbler', (128, 128), PAL, {'gobbler': {}}, res=2)
    body = m.part('body', pivot=(0, 14, 2))
    # ---- the torso: bone vertebrae down the back, ribs and a glowing lateral line along the flanks
    spine = [('...bbbbbbbb...' if y % 2 == 0 else '.....bbbb.....') for y in range(14)]
    flank = []
    for y in range(12):
        if y in (2, 3, 4, 5):
            flank.append(''.join('b' if x % 3 == 0 else '.' for x in range(14)))
        elif y == 7:
            flank.append(''.join('g' if x % 2 == 0 else '.' for x in range(14)))
        else:
            flank.append('.' * 14)
    body.cube((-7, -6, -4), (14, 12, 14), **mc('hide', clusters=0.3, bands=[(8, 'belly')]), faces={
        'up': mc('hide', clusters=0.25, map=spine, keys={'b': 'bone'}),
        'east': mc('hide', clusters=0.25, bands=[(8, 'belly')], map=flank, keys={'b': 'bone_d', 'g': 'glow'}, glow_keys='g'),
        'west': mc('hide', clusters=0.25, bands=[(8, 'belly')], map=[r[::-1] for r in flank], keys={'b': 'bone_d', 'g': 'glow'}, glow_keys='g'),
        'down': mc('belly', clusters=0.2),
    })
    # ---- the chest: a bone ribcage over trapped, glowing souls (the Warden's chest, slung under a fish)
    cage = []
    for y in range(9):
        cage.append(''.join('b' if x % 2 == 0 else ('S' if (x + y) % 4 == 1 else 's') for x in range(10)))
    chest = body.part('chest', pivot=(0, 6, 1))
    chest.cube((-5, -1, -3), (10, 3, 9), **mc('belly', clusters=0.15), faces={
        'down': mc('belly', clusters=0.0, rim=False, map=cage, keys={'b': 'bone', 's': 'soul_d', 'S': 'soul'}, glow_keys='sS'),
        'east': mc('belly', clusters=0.0, map=[''.join('b' if x % 2 == 0 else 's' for x in range(9))] * 3, keys={'b': 'bone_d', 's': 'soul'},
                   glow_keys='s'),
        'west': mc('belly', clusters=0.0, map=[''.join('b' if x % 2 == 0 else 's' for x in range(9))] * 3, keys={'b': 'bone_d', 's': 'soul'},
                   glow_keys='s'),
    })
    # ---- the great flat head: no eyes, only rows of sensory pits in bone plates
    head = body.part('head', pivot=(0, -1, -4))
    skull_top = []
    for y in range(10):
        row = list('..bbbbbbbbbbbbbb..' if y in (2, 5, 8) else '...b..........b...')
        if y in (1, 4, 7):
            row = list(_pits(18, 3)[1])
        skull_top.append(''.join(row))
    snout = ['.' * 18, '.b' + 'o.' * 8, 'b' * 18, '.' * 18, '.' * 18, '.' * 18, '.' * 18]
    head.cube((-9, -5, -10), (18, 7, 10), **mc('hide', clusters=0.3), faces={
        'up': mc('hide', clusters=0.2, map=skull_top, keys={'b': 'bone', 'o': 'pit', 'g': 'glow'}, glow_keys='g'),
        'north': mc('hide', clusters=0.2, map=snout, keys={'b': 'bone_d', 'o': 'pit'}),
        'east': mc('hide', clusters=0.2, map=_pits(10, 7, rim_row=6), keys={'b': 'bone_d', 'o': 'pit', 'g': 'glow'}, glow_keys='g'),
        'west': mc('hide', clusters=0.2, map=_pits(10, 7, rim_row=6), keys={'b': 'bone_d', 'o': 'pit', 'g': 'glow'}, glow_keys='g'),
        # the roof of the mouth: dark throat with the gullet glowing at the back
        'down': mc('throat', clusters=0.0, rim=False, map=['....' + 'gGGGGGGGGg' + '....'] + ['.' * 18] * 9, keys={'g': 'gullet', 'G': 'glow2'},
                   glow_keys='gG'),
    })
    head.cube((-8.5, 2, -9.5), (17, 1, 9), **mc('tooth', clusters=0.0, rim=False), faces={
        'north': mc('tooth', clusters=0.0, rim=False, map=['tTtTtTtTtTtTtTtTt'], keys={'t': 'tooth', 'T': 'throat'}),
        'east': mc('tooth', clusters=0.0, rim=False, map=['tTtTtTtTt'], keys={'t': 'tooth', 'T': 'throat'}),
        'west': mc('tooth', clusters=0.0, rim=False, map=['tTtTtTtTt'], keys={'t': 'tooth', 'T': 'throat'}),
        'up': mc('throat', clusters=0.0, rim=False), 'down': mc('throat_d', clusters=0.0, rim=False),
    })
    # Warden tendrils on the crown of the head, glowing and twitching when it listens
    for side, sx in (('left', 1), ('right', -1)):
        t = head.part(f'{side}_tendril', pivot=(6 * sx, -5, -4), rot=(0, 0, 0.25 * sx))
        t.cube((0, -7, -2), (0, 7, 6), **mc('glow', clusters=0.0, rim=False, glow=True, ribs=2, accent='glow2', alpha='membrane', edge='outer',
                                             edge_depth=1, scallop=2))
    # ---- the lower jaw, hinged at the back of the head
    jaw = head.part('jaw', pivot=(0, 2, -1))
    jaw.cube((-9, 0, -9), (18, 4, 9), **mc('hide', clusters=0.25, bands=[(2, 'belly')]), faces={
        'up': mc('throat', clusters=0.0, rim=False, map=['.' * 18] * 7 + ['...gGGGGGGGGGGg...', '....gggggggggg....'],
                 keys={'g': 'gullet', 'G': 'glow2'}, glow_keys='gG'),
        'north': mc('hide', clusters=0.2, map=['b' * 18, '.' * 18, '.o..o..o..o..o..o.'], keys={'b': 'bone', 'o': 'pit'}),
        'down': mc('belly', clusters=0.2),
    })
    jaw.cube((-8.5, -1, -8.5), (17, 1, 8), **mc('tooth', clusters=0.0, rim=False), faces={
        'north': mc('tooth', clusters=0.0, rim=False, map=['TtTtTtTtTtTtTtTtT'], keys={'t': 'tooth', 'T': 'throat'}),
        'east': mc('tooth', clusters=0.0, rim=False, map=['TtTtTtTt'], keys={'t': 'tooth', 'T': 'throat'}),
        'west': mc('tooth', clusters=0.0, rim=False, map=['TtTtTtTt'], keys={'t': 'tooth', 'T': 'throat'}),
        'down': mc('throat', clusters=0.0, rim=False), 'up': mc('throat_d', clusters=0.0, rim=False),
    })
    # ---- barbels: two long whiskers from the corners of the mouth, two short ones on the chin, glowing tips
    for side, sx in (('left', 1), ('right', -1)):
        b0 = head.part(f'{side}_barbel', pivot=(8.5 * sx, 1, -9), rot=(0.2, 0.9 * sx, 0))
        b0.cube((-0.5, -0.5, -7), (1, 1, 7), **mc('barbel', clusters=0.0, rim=False))
        b1 = b0.part(f'{side}_barbel_tip', pivot=(0, 0, -7), rot=(0.3, 0.3 * sx, 0))
        b1.cube((-0.5, -0.5, -6), (1, 1, 6), **mc('barbel_l', clusters=0.0, rim=False), faces={
            'north': mc('glow', clusters=0.0, rim=False, glow=True),
            'east': mc('barbel_l', clusters=0.0, rim=False, map=['gg....'], keys={'g': 'glow'}, glow_keys='g', center=False, at=(0, 0)),
            'west': mc('barbel_l', clusters=0.0, rim=False, map=['....gg'], keys={'g': 'glow'}, glow_keys='g', center=False, at=(0, 0)),
        })
        c0 = jaw.part(f'{side}_chin_barbel', pivot=(3 * sx, 4, -7), rot=(-0.9, 0.2 * sx, 0))
        c0.cube((-0.5, 0, -0.5), (1, 5, 1), **mc('barbel', clusters=0.0, rim=False), faces={'down': mc('glow', clusters=0.0, glow=True)})
    # ---- fins and the long tail
    dorsal = body.part('dorsal', pivot=(0, -6, 2))
    dorsal.cube((0, -5, -2), (0, 5, 11), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='bone_d', alpha='membrane', edge='bottom'))
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(7 * sx, 3, -1), rot=(0, 0.35 * sx, 0.35 * sx))
        pf.cube((0 if sx > 0 else -7, 0, -1), (7, 0, 6), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='bone_d', alpha='membrane',
                                                               edge='outer'))
    tail = body.part('tail', pivot=(0, -0.5, 10))
    tail.cube((-5.5, -4.5, 0), (11, 9, 8), **mc('hide', clusters=0.3, bands=[(6, 'belly')]), faces={
        'up': mc('hide', clusters=0.2, map=['.....bb....', '...........'] * 4, keys={'b': 'bone'}),
        'east': mc('hide', clusters=0.2, bands=[(6, 'belly')], map=['.' * 8] * 5 + ['g.g.g.g.'], keys={'g': 'glow'}, glow_keys='g'),
        'west': mc('hide', clusters=0.2, bands=[(6, 'belly')], map=['.' * 8] * 5 + ['.g.g.g.g'], keys={'g': 'glow'}, glow_keys='g'),
    })
    tail2 = tail.part('tail2', pivot=(0, 0, 8))
    tail2.cube((-3.5, -3, 0), (7, 6, 7), **mc('hide', clusters=0.3, bands=[(4, 'belly')]), faces={
        'up': mc('hide', clusters=0.2, map=['...b...', '.......'] * 3 + ['...b...'], keys={'b': 'bone'}),
    })
    tail2.cube((0, -4, 1), (0, 2, 5), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='bone_d', alpha='membrane', edge='bottom'))
    fluke = tail2.part('fluke', pivot=(0, 0, 7))
    fluke.cube((0, -7, -1), (0, 14, 9), **mc('fin', clusters=0.0, rim=False, ribs=2, accent='fin_l', alpha='membrane', edge='outer', edge_depth=2,
                                             scallop=3, glow=True))
    return m
