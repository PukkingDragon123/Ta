"""A2 Echoer: the Echoer (entity id `enchoer`), Soul Golems and Nibs - model geometry and paint
(see modelkit.py), their sound events, and the data/lang that goes with them (hooked from
mobs.ALL, gen_assets and gen_data)."""
from modelkit import Model


def hd_rows(spec, w, h):
    rows = []
    for y in range(h):
        t = spec.get(y, '')
        rows.append((t + '.' * w)[:w])
    return rows


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


# =========================================================================== THE ECHOER
# C4 Echoer: a furry god-deer. The skull's front is 14 x 11 texels (7 x 5.5 units at res 2), the eyes above the snout; E a soft
# glowing iris, I its bright core, l a lid line, R the glowing rune on its brow.
_BROW = {0: '.....RR.....'}
ECHOER_FACE = {
    'neutral': {**_BROW, 1: '..ll....ll..', 2: '.EEI....IEE.', 3: '..EE....EE..'},
    'blink': {**_BROW, 2: '.lll....lll.'},
    'happy': {**_BROW, 2: '..EE....EE..', 3: '.E..E..E..E.'},
    'sleep': {**_BROW, 3: '.lll....lll.'},
    'hurt': {**_BROW, 1: '.E..l..l..E.', 2: '..EE....EE..', 3: '.E........E.'},
    'dead': {**_BROW, 1: '.l.l....l.l.', 2: '..l......l..', 3: '.l.l....l.l.'},
}
# the glowing runes in the fur of each flank (centred on the 20 x 9 unit side face)
RUNES = {
    3: '...RRR.....R.R......RRR.......',
    4: '..R...R....RRR.....R...R..r...',
    5: '..R.R.R.....R......R.R.R.rrr..',
    6: '..R...R...RRRRR....R...R..r...',
    7: '...RRR......R.......RRR.......',
    8: '.....r......R.........r.......',
    9: '....rrr....R.R.......rrr......',
    10: '.....r.....................R..',
    11: '..........................RRR.',
}

# The antlers, left side (x mirrored for the right): (part, parent, pivot, rot, cube origin, size, tip glows)
ANTLER = [
    ('antler', None, (2.2, -5.5, 0.5), (-0.3, 0.0, 0.6), (-1, -7, -1), (2, 7, 2), False),
    ('antler_brow', 'antler', (0, -1.5, 0), (1.1, 0.0, -0.45), (-0.5, -3.5, -0.5), (1, 3.5, 1), True),
    ('antler_mid', 'antler', (0, -7, 0), (-0.15, 0.0, -0.45), (-0.75, -6, -0.75), (1.5, 6, 1.5), False),
    ('antler_fork', 'antler_mid', (0, -2, 0), (0.25, 0.0, 1.0), (-0.5, -5, -0.5), (1, 5, 1), True),
    ('antler_back', 'antler_mid', (0, -3.5, 0), (-0.9, 0.0, 0.2), (-0.5, -4, -0.5), (1, 4, 1), True),
    ('antler_top', 'antler_mid', (0, -6, 0), (0.3, 0.0, -0.4), (-0.5, -5, -0.5), (1, 5, 1), True),
]
# The wind chimes hung in the antlers: (antler part, point on it, cord length, tube length, tube colour)
CHIMES = [
    ('antler', (0, -5, 0), 1.5, 4.0, 'chime_a'),
    ('antler_fork', (0, -4, 0), 1.0, 5.5, 'chime_b'),
    ('antler_top', (0, -3.5, 0), 2.0, 3.5, 'chime_c'),
    ('antler_back', (0, -3, 0), 1.5, 4.5, 'chime_a'),
]


def _rot(v, r):
    """A point turned the way ModelPart turns its children (X, then Y, then Z)."""
    import math
    x, y, z = v
    rx, ry, rz = r
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return x, y, z


def _mirror(sx, pivot, rot):
    return (pivot[0] * sx, pivot[1], pivot[2]), (rot[0], rot[1] * sx, rot[2] * sx)


def _head_point(name, point, sx):
    """Where a point on one antler segment sits in the head's space."""
    by_name = {a[0]: a for a in ANTLER}
    p = point
    seg = by_name[name]
    while seg is not None:
        pivot, rot = _mirror(sx, seg[2], seg[3])
        q = _rot(p, rot)
        p = (q[0] + pivot[0], q[1] + pivot[1], q[2] + pivot[2])
        seg = by_name[seg[1]] if seg[1] else None
    return p


def _halo_map(n=28):
    """A soft ring of light: '_' is cut away, H the bright band, h its gentle rim, s a few motes inside."""
    import math
    rows = []
    c = (n - 1) / 2.0
    for y in range(n):
        row = ''
        for x in range(n):
            d = math.hypot(x - c, y - c)
            if n / 2 - 3.4 <= d <= n / 2 - 0.4:
                row += 'H' if n / 2 - 2.6 <= d <= n / 2 - 1.3 else 'h'
            elif n / 2 - 6.0 < d < n / 2 - 4.6 and round(math.degrees(math.atan2(y - c, x - c))) % 30 == 0:
                row += 's'
            else:
                row += '_'
        rows.append(row)
    return rows


def enchoer() -> Model:
    """C4 Echoer: a majestic furry god-deer. A deep-chested body in thick ivory fur with glowing
    runes in its flanks, a great lilac-white mane round the shoulders and a ruff under the chest, a
    short strong neck, a gentle deer face with soft glowing eyes, leaf ears and a fluffy cheek
    ruff - and huge branching moon-bone antlers hung with swaying wind-chime tubes, a soft halo of
    light behind them. Shaggy fetlocks over dark hooves, an up-flicked fluffy tail."""
    pal = {
        'fur': '#efe6d8', 'fur_l': '#fbf6ec', 'fur_d': '#cdbfae',
        'belly': '#e2d6c4', 'belly_l': '#efe6d6', 'belly_d': '#bfae98',
        'mane': '#f4eefc', 'mane_l': '#ffffff', 'mane_d': '#cbbfe2',
        'snout': '#e8dece', 'snout_l': '#f6efe2', 'snout_d': '#c4b6a2', 'nose': '#8a6a86', 'nose_l': '#a888a2', 'nose_d': '#6a4e68', 'nostril': '#4a3a52',
        'ear_in': '#f2c8d6', 'ear_in_d': '#d49ab0',
        'hoof': '#5a4a66', 'hoof_l': '#6e5e7c', 'hoof_d': '#3e3248',
        'antler': '#eadfbc', 'antler_l': '#fff7de', 'antler_d': '#b9a77c',
        'rune': '#8ff6ff', 'rune_d': '#46c8dc', 'eye': '#bffcff', 'eye_core': '#ffffff', 'lid': '#7a6a8e',
        'halo': '#fff1b8', 'halo_l': '#ffffff', 'halo_d': '#f2c86a',
        'chime_a': '#e6eeff', 'chime_a_l': '#ffffff', 'chime_a_d': '#9aa8c4',
        'chime_b': '#ffe08a', 'chime_b_l': '#fff3c4', 'chime_b_d': '#c99a3a',
        'chime_c': '#9ff4ff', 'chime_c_l': '#dcffff', 'chime_c_d': '#4cc4dc',
        'cord': '#cbbf9e', 'cord_l': '#e2d8bc', 'cord_d': '#9c9070',
    }
    m = Model('enchoer', (128, 128), pal, {'enchoer': {}}, res=2, expressions=['blink', 'happy', 'sleep', 'hurt', 'dead'])
    rk = {'R': 'rune', 'r': 'rune_d'}
    fur = dict(color='fur', pattern='mc', clusters=0.12, streaks=0.9)
    mane = dict(color='mane', pattern='mc', clusters=0.1, streaks=1.2)
    rune_side = hd_rows(RUNES, 30, 16)

    # --- four strong deer legs: a furred thigh, a slim shin, a shaggy fetlock, a dark hoof
    for name, (px, pz) in (('front_left_leg', (3.2, -6.5)), ('front_right_leg', (-3.2, -6.5)),
                           ('back_left_leg', (3.2, 6.5)), ('back_right_leg', (-3.2, 6.5))):
        leg = m.part(name, pivot=(px, 11, pz))
        leg.cube((-1.75, -3, -1.75), (3.5, 8, 3.5), **dict(fur, rim=False))
        shin = leg.part(name.replace('leg', 'shin'), pivot=(0, 5, 0))
        shin.cube((-1.25, 0, -1.25), (2.5, 6, 2.5), **mc('fur', clusters=0.0, rim=False, streaks=0.5))
        shin.cube((-1.75, 3.5, -1.75), (3.5, 2.5, 3.5), **mc('mane', clusters=0.0, rim=False, streaks=1.0, fringe=1))
        hoof = shin.part(name.replace('leg', 'hoof'), pivot=(0, 6, 0))
        hoof.cube((-1.25, 0, -1.5), (2.5, 2, 3), **mc('hoof', clusters=0.0, rim=False))

    # --- the deep body in thick fur, runes in its flanks, a shaggy belly and a round rump
    body = m.part('body', pivot=(0, 4, 0))
    body.cube((-5.5, -4.5, -10), (11, 10, 20), **fur, faces={
        'west': dict(**fur, hd=True, map=[r[::-1] for r in rune_side], keys=rk, glow_keys='Rr'),
        'east': dict(**fur, hd=True, map=rune_side, keys=rk, glow_keys='Rr'),
        'up': dict(color='fur', pattern='mc', clusters=0.2, streaks=0.6, hd=True,
                   map=hd_rows({8: '.........RR.........', 9: '........R..R........', 10: '.........RR.........',
                                30: '.........rr.........'}, 20, 40), keys=rk, glow_keys='Rr'),
        'down': dict(color='belly_d', pattern='mc', clusters=0.1),
    })
    body.cube((-5, 5, -8), (10, 2, 16), **mc('belly', clusters=0.0, streaks=1.0, fringe=2, rim=False))
    body.cube((-5, -4, 9), (10, 8, 2), **mc('fur', clusters=0.15, streaks=0.8, rim=False))
    # the great mane round the shoulders, and the ruff under the chest
    body.cube((-6.5, -6.5, -11.5), (13, 13, 7), **mane, fringe=2)
    ruff = body.part('ruff', pivot=(0, 5, -9.5))
    ruff.cube((-4, 0, -2), (8, 4, 4), **mane, fringe=2, rim=False)
    tail = body.part('tail', pivot=(0, -3, 10.5), rot=(0.35, 0, 0))
    tail.cube((-1.5, -1.5, 0), (3, 3, 2.5), **mc('mane', clusters=0.1, streaks=0.8, rim=False))
    tail_tip = tail.part('tail_tip', pivot=(0, 0, 2.5), rot=(0.4, 0, 0))
    tail_tip.cube((-2, -1.75, 0), (4, 3.5, 3.5), **mc('mane_l', clusters=0.0, streaks=0.6, rim=False, dark='mane_d'))

    # --- a short, strong neck, its mane flowing down the back
    neck = body.part('neck', pivot=(0, -3, -9), rot=(0.45, 0, 0))
    neck.cube((-3, -7, -3), (6, 7, 6), **fur, faces={'north': dict(color='belly', pattern='mc', clusters=0.0, streaks=0.8)})
    neck_mane = neck.part('mane', pivot=(0, -7.5, 1.0), rot=(-0.15, 0, 0))
    neck_mane.cube((-3.5, 0, 0), (7, 8, 3.5), **mane, fringe=2)

    # --- the gentle deer head
    head = neck.part('head', pivot=(0, -7, -0.5), rot=(-0.45, 0, 0))
    face_keys = {'E': 'eye', 'I': 'eye_core', 'l': 'lid', 'R': 'rune'}
    head.cube((-3.5, -5.5, -4), (7, 5.5, 7), **mc('fur', clusters=0.08, streaks=0.5, rim=False), faces={
        'north': dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True, map=hd_rows(ECHOER_FACE['neutral'], 12, 11),
                      keys=face_keys, glow_keys='EIR', expr={k: hd_rows(v, 12, 11) for k, v in ECHOER_FACE.items() if k != 'neutral'}),
    })
    head.cube((-3, -6.5, -3), (6, 1, 5), **mc('mane', clusters=0.0, rim=False, streaks=0.6))
    head.cube((-2.25, -3, -8), (4.5, 3.5, 4), **mc('snout', clusters=0.0, rim=False), faces={
        'north': dict(color='snout', pattern='mc', clusters=0.0, rim=False, hd=True, map=['.........', '.........', '..nn.nn..'],
                      keys={'n': 'nostril'}),
    })
    head.cube((-1.25, -3.25, -8.5), (2.5, 1, 1), **mc('nose', clusters=0.0, rim=False))
    head.cube((-4, -3, -3), (8, 3.5, 4.5), **mc('mane', clusters=0.0, streaks=1.0, fringe=1, rim=False))
    jaw = head.part('jaw', pivot=(0, 0, -3.5))
    jaw.cube((-1.75, -0.5, -4.25), (3.5, 1, 4.25), **mc('snout_d', clusters=0.0, rim=False))
    for side, sx in (('left', 1), ('right', -1)):
        ear = head.part(f'{side}_ear', pivot=(3.5 * sx, -4.5, 0.5), rot=(0.3, 0.35 * sx, 0.75 * sx))
        ear.cube((0 if sx > 0 else -4, -0.75, -1), (4, 1.5, 2), **mc('fur', clusters=0.0, rim=False), faces={
            'north': dict(color='ear_in', pattern='mc', clusters=0.0, rim=False),
            'up': dict(color='ear_in', pattern='mc', clusters=0.0, rim=False),
        })
        # the antlers: moon-bone beams branching into tines, their tips alight
        parts = {}
        for pname, parent, pivot, rot, origin, size, tip in ANTLER:
            pv, rt = _mirror(sx, pivot, rot)
            owner = head if parent is None else parts[parent]
            seg = owner.part(f'{side}_{pname}', pivot=pv, rot=rt)
            faces = {}
            if tip:
                faces = {'up': dict(color='rune', pattern='mc', clusters=0.0, glow=True),
                         'north': dict(color='antler_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['RR', 'rr'],
                                       keys=rk, glow_keys='Rr'),
                         'south': dict(color='antler_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['RR', 'rr'],
                                       keys=rk, glow_keys='Rr')}
            seg.cube(origin, size, **mc('antler', clusters=0.2, rim=False), faces=faces)
            parts[pname] = seg
        # wind chimes hang from the branches, straight down in the head's space
        for i, (on, point, cord, tube, colour) in enumerate(CHIMES):
            hx, hy, hz = _head_point(on, point, sx)
            chime = head.part(f'{side}_chime_{i}', pivot=(round(hx, 3), round(hy, 3), round(hz, 3)))
            chime.cube((-0.25, 0, -0.25), (0.5, cord, 0.5), **mc('cord', clusters=0.0, rim=False))
            chime.cube((-0.75, cord - 0.25, -0.75), (1.5, 0.5, 1.5), **mc('antler_d', clusters=0.0, rim=False))
            chime.cube((-0.5, cord, -0.5), (1, tube, 1), **mc(colour, clusters=0.0, rim=False, glow=colour == 'chime_c'),
                       faces={'down': dict(color='rune', pattern='mc', clusters=0.0, glow=True)})

    # --- the soft halo of light behind the antlers
    halo = head.part('halo', pivot=(0, -10, 4.5), rot=(0.12, 0, 0))
    ring = _halo_map(32)
    hk = {'H': 'halo_l', 'h': 'halo', 's': 'halo_d'}
    halo.cube((-8, -8, 0), (16, 16, 0), color='halo', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='halo', pattern='mc', clusters=0.0, rim=False, hd=True, map=ring, keys=hk, glow_keys='Hhs'),
        'south': dict(color='halo', pattern='mc', clusters=0.0, rim=False, hd=True, map=[r[::-1] for r in ring], keys=hk, glow_keys='Hhs'),
    })
    return m


# =========================================================================== SOUL GOLEM
GOLEM_EYE = {
    'neutral': {1: '.EEEE.', 2: 'EEIIEE', 3: 'EEIIEE', 4: '.EEEE.'},
    'blink': {3: '.llll.', 4: 'l....l'},
    'happy': {2: '.EEEE.', 3: 'E....E'},
    'sleep': {4: '.llll.'},
    'hurt': {1: 'E....E', 2: '.E..E.', 3: '..EE..', 4: '.E..E.'},
    'dead': {1: 'l....l', 2: '.l..l.', 3: '..ll..', 4: '.l..l.'},
}


def soul_golem() -> Model:
    """A small ancient construct of carved soulstone - a round frog-like body, two big domed eyes
    lit by the soul fire inside, a wide carved grin, stubby arms and legs, cyan cracks glowing
    along its seams and a soul-lamp stalk on top (the Copper Golem's lightning rod, if it were
    a lantern)."""
    pal = {
        'stone': '#6c5a4b', 'stone_l': '#87725f', 'stone_d': '#4b3e34',
        'belly': '#7d6a58', 'belly_l': '#97826d', 'belly_d': '#5c4c3f',
        'crack': '#5fe9ff', 'crack_d': '#2aa9c8', 'mouth': '#251c16',
        'eye': '#a9faff', 'eye_core': '#ffffff', 'lid': '#3a2f27',
        'lamp': '#7ff3ff', 'lamp_l': '#d6fdff', 'lamp_d': '#3cc3dc',
    }
    m = Model('soul_golem', (64, 64), pal, {'soul_golem': {}}, res=2, expressions=['blink', 'happy', 'sleep', 'hurt', 'dead'])
    ck = {'c': 'crack', 'C': 'crack_d', 'm': 'mouth'}
    front = hd_rows({1: '...c..........', 2: '...cc.........', 3: '....c.........', 7: '..mmmmmmmmmm..', 8: '...m......m...',
                     9: '....mmmmmm....', 11: '......CC......', 12: '.....CccC.....', 13: '......CC......'}, 16, 14)
    side = hd_rows({2: '.....c........', 3: '....cc........', 4: '....c....C....', 5: '.........CC...', 9: '..C...........',
                    10: '..CC..........'}, 14, 14)
    for name, (px, pz) in (('left_leg', (2, 0.5)), ('right_leg', (-2, 0.5))):
        leg = m.part(name, pivot=(px, 21, pz))
        leg.cube((-1.5, 0, -1.5), (3, 2, 3), **mc('stone', clusters=0.1, rim=False))
        leg.cube((-1.75, 2, -2.5), (3.5, 1, 4), **mc('stone_d', clusters=0.0, rim=False), faces={
            'north': dict(color='stone_d', pattern='mc', clusters=0.0, rim=False, hd=True, map=['c.c.c.c'], keys=ck, glow_keys='c'),
        })
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-4, -7, -3.5), (8, 7, 7), **mc('stone', clusters=0.25, bands=[(4, 'belly')]), faces={
        'north': dict(**mc('stone', clusters=0.0, bands=[(4, 'belly')]), hd=True, map=front, keys=ck, glow_keys='cC'),
        'west': dict(**mc('stone', clusters=0.2, bands=[(4, 'belly')]), hd=True, map=side, keys=ck, glow_keys='cC'),
        'east': dict(**mc('stone', clusters=0.2, bands=[(4, 'belly')]), hd=True, map=[r[::-1] for r in side], keys=ck, glow_keys='cC'),
    })
    # a rounder silhouette: soft bevels on the top and the sides
    body.cube((-3.5, -7.5, -3), (7, 0.5, 6), **mc('stone_l', clusters=0.2, rim=False))
    eyes = {k: hd_rows(v, 6, 6) for k, v in GOLEM_EYE.items()}
    for side_name, sx in (('left', 1), ('right', -1)):
        eye = body.part(f'{side_name}_eye', pivot=(2.2 * sx, -7, -1))
        eye.cube((-1.5, -2.5, -1.5), (3, 3, 3), **mc('stone', clusters=0.0, rim=False), faces={
            'north': dict(color='stone', pattern='mc', clusters=0.0, rim=False, hd=True, map=eyes['neutral'],
                          keys={'E': 'eye', 'I': 'eye_core', 'l': 'lid'}, glow_keys='EI',
                          expr={k: v for k, v in eyes.items() if k != 'neutral'}),
        })
        arm = body.part(f'{side_name}_arm', pivot=(4 * sx, -4.5, 0), rot=(0, 0, -0.15 * sx))
        arm.cube((0 if sx > 0 else -1.5, -0.5, -1), (1.5, 4, 2), **mc('stone', clusters=0.1, rim=False), faces={
            'down': dict(color='stone_d', pattern='mc', clusters=0.0),
        })
    stalk = body.part('stalk', pivot=(0, -7.5, 0.5))
    stalk.cube((-0.5, -3, -0.5), (1, 3, 1), **mc('stone_d', clusters=0.0, rim=False))
    lamp = stalk.part('lamp', pivot=(0, -3, 0))
    lamp.cube((-1, -2, -1), (2, 2, 2), **mc('lamp', clusters=0.3, rim=False, glow=True))
    return m


# =========================================================================== NIB
def nib() -> Model:
    """A tiny glowing wisp-butterfly: a bright little body and two pairs of see-through wings
    veined with light."""
    pal = {
        'glow': '#fff3b0', 'glow_l': '#fffbe6', 'glow_d': '#ffd36b',
        'wing': '#9ff4ff', 'wing_l': '#dafcff', 'wing_d': '#62c9f0', 'vein': '#ffffff', 'spot': '#ff9be3',
    }
    m = Model('nib', (32, 32), pal, {'nib': {}}, res=2)
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-0.5, -0.5, -1.5), (1, 1, 3), **mc('glow', clusters=0.0, rim=False, glow=True))
    body.cube((-0.75, -0.75, -2.75), (1.5, 1.5, 1.25), **mc('glow_l', clusters=0.0, rim=False, glow=True))
    wk = {'v': 'vein', 's': 'spot', 'w': 'wing_l'}
    up_wing = hd_rows({0: '......ww..', 1: '....wwvvw.', 2: '..wwvvsvw.', 3: '.wvv.ssvw.', 4: 'wvv...vvw.', 5: 'vv....vw..',
                       6: 'v....vw...', 7: 'v...vw....', 8: '.vvvw.....', 9: '..ww......'}, 10, 10)
    lo_wing = hd_rows({0: 'vvvv..', 1: 'v..vw.', 2: 'v.s.vw', 3: '.v..vw', 4: '..vvw.', 5: '...w..'}, 7, 7)
    for side, sx in (('left', 1), ('right', -1)):
        def mir(rows):
            return rows if sx > 0 else [r[::-1] for r in rows]
        wing = body.part(f'{side}_wing', pivot=(0.5 * sx, -0.5, -0.5), rot=(0, 0, -0.3 * sx))
        wspec = dict(color='wing', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True, hd=True, map=mir(up_wing), keys=wk)
        wing.cube((0 if sx > 0 else -5, 0, -4), (5, 0, 5), color='wing', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True,
                  faces={'up': wspec, 'down': wspec})
        low = body.part(f'{side}_wing_low', pivot=(0.5 * sx, -0.25, 0.5), rot=(0, 0, -0.2 * sx))
        lspec = dict(color='wing_d', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True, hd=True, map=mir(lo_wing), keys=wk)
        low.cube((0 if sx > 0 else -3.5, 0, 0), (3.5, 0, 3.5), color='wing_d', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True,
                 faces={'up': lspec, 'down': lspec})
        ant = body.part(f'{side}_antenna', pivot=(0.3 * sx, -0.75, -2.5), rot=(-0.6, 0, 0.3 * sx))
        ant.cube((0, -2, 0), (0, 2, 1), color='glow_d', pattern='mc', clusters=0.0, rim=False, glow=True)
    return m


ALL = {'soul_golem': soul_golem, 'nib': nib}
POSES = {}

# --------------------------------------------------------------------------- sounds (vanilla files)
SOUNDS = {
    'entity.soul_golem.ambient': [('block/amethyst/resonate1', 0.5, 1.6), ('block/amethyst/resonate3', 0.5, 1.7),
                                  ('mob/allay/idle_without_item1', 0.4, 1.3)],
    'entity.soul_golem.hurt': [('block/basalt/break1', 0.8, 1.3), ('block/basalt/break2', 0.8, 1.4)],
    'entity.soul_golem.death': [('block/basalt/break3', 1.0, 0.9), ('block/amethyst_cluster/break1', 0.8, 1.2)],
    'entity.soul_golem.step': [('block/basalt/step1', 0.3, 1.6), ('block/basalt/step3', 0.3, 1.7)],
    'entity.soul_golem.dig': [('item/brush/brushing_sand1', 0.6, 1.2), ('item/brush/brushing_sand2', 0.6, 1.3)],
    'entity.soul_golem.find': [('block/amethyst/shimmer', 1.0, 1.4), ('mob/allay/item_given1', 0.8, 1.3)],
    'entity.soul_golem.slump': [('block/beacon/deactivate', 0.6, 1.6)],
    'entity.soul_golem.recharge': [('block/beacon/power2', 0.6, 1.8), ('block/amethyst/resonate2', 0.8, 1.5)],
    'entity.nib.ambient': [('block/amethyst/shimmer', 0.25, 1.9), ('mob/allay/idle_without_item2', 0.15, 1.9)],
    'entity.nib.hurt': [('block/amethyst_cluster/break2', 0.5, 1.9)],
    'entity.nib.transform': [('block/amethyst/resonate4', 1.0, 1.4), ('mob/allay/item_thrown1', 0.8, 1.5)],
    'block.echoer_device.charge': [('mob/warden/sonic_charge1', 0.7, 1.4), ('mob/warden/sonic_charge2', 0.7, 1.5)],
    'block.echoer_device.fire': [('mob/warden/sonic_boom1', 0.7, 1.6), ('mob/warden/sonic_boom2', 0.7, 1.7)],
    'block.echoer_device.fizzle': [('block/amethyst_cluster/break3', 0.8, 0.8)],
    # C4 Echoer: the wind chimes in its antlers clink as it moves
    'entity.enchoer.chimes': [('block/note_block/chime', 0.35, 1.0), ('block/note_block/chime', 0.3, 1.26),
                              ('block/note_block/chime', 0.3, 1.5), ('block/amethyst/shimmer', 0.6, 1.4)],
}
SUBTITLES = {
    'entity.soul_golem.ambient': 'Soul Golem hums', 'entity.soul_golem.hurt': 'Soul Golem chips', 'entity.soul_golem.death': 'Soul Golem crumbles',
    'entity.soul_golem.step': 'Soul Golem waddles', 'entity.soul_golem.dig': 'Soul Golem digs', 'entity.soul_golem.find': 'Soul Golem finds something',
    'entity.soul_golem.slump': 'Soul Golem runs down', 'entity.soul_golem.recharge': 'Soul Golem recharges',
    'entity.nib.ambient': 'Nib twinkles', 'entity.nib.hurt': 'Nib flickers', 'entity.nib.transform': 'Nib turns to treasure',
    'entity.enchoer.ambient': 'Echoer chimes', 'entity.enchoer.hum': 'Echoer hums', 'entity.enchoer.trade': 'Echoer waits, humming',
    'entity.enchoer.yes': 'Echoer accepts', 'entity.enchoer.no': 'Echoer sighs', 'entity.enchoer.hurt': 'Echoer hurts', 'entity.enchoer.death': 'Echoer fades',
    'entity.enchoer.chimes': 'Echoer\'s chimes clink',
    'block.echoer_device.charge': 'The Echoer charges', 'block.echoer_device.fire': 'The Echoer fires', 'block.echoer_device.fizzle': 'The Echoer fizzles',
}

NS = 'thesift'
