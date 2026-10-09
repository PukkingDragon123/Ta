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
    """CR2: the Slumbler, a living instrument in rainbow scales (tools/slumbler.py)."""
    return __import__('slumbler').slumbler()


# =========================================================================== SIFTER
# CR1: the Sifter is a living bell now - its model lives in tools/sifter.py
from sifter import sifter  # noqa: E402,F401


# =========================================================================== ENCHOER
# A2 Echoer: the Echoer (still entity id `enchoer`) was redesigned - its model lives in tools/echoer.py
from echoer import enchoer  # noqa: E402,F401


# =========================================================================== HARMONER
def harmoner() -> Model:
    """S1 land: the small, fluffy Harmoner (and the Sculk Harmoner's colouring) lives in tools/harmoner.py."""
    return __import__('harmoner').harmoner()


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


ALL = {'bulb': bulb, 'slumbler': slumbler, 'sifter': sifter, 'enchoer': enchoer, 'harmoner': harmoner,
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
ALL.update(__import__('slumbler').EXTRA)  # CR2: the Slumbler's tadpole and its eggs (tools/slumbler.py)

# A2 Echoer: Soul Golems and Nibs (tools/echoer.py)
import echoer  # noqa: E402

ALL.update(echoer.ALL)
ALL.update(__import__('swifter').MODELS)  # A2 Swifter & White Forest: the Swifter and its cubs (tools/swifter.py)
ALL.update(__import__('cave_creatures').MODELS)  # A4 cave creatures: the Jailer and Sculklings (tools/cave_creatures.py)
ALL.update(__import__('sift_sniffer').MODELS)  # E1 Sniffer & rot: the Sift Sniffer (tools/sift_sniffer.py)
ALL.update(__import__('materials').MODELS)  # F1: the Europhy Table's clockwork (tools/materials.py)
ALL.update(__import__('echoer_drill').MODELS)  # RR: the Echoer Drill's gun - barrel, coils, bit, horn, drum (tools/echoer_drill.py)
ALL.update(__import__('band_table').MODELS)  # F2 Band Table: the Music Band Table (tools/band_table.py)
ALL.update(__import__('mini_creator').MODELS)  # F3 Knowledge and lore: the Mini Creator (tools/mini_creator.py)
