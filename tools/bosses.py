"""The mini-bosses of the Sift - the Thumper and the Strummer - their little
ones, and the Conductor's Mask. Same DSL as mobs.py (see modelkit.py); every face is painted at
texel density (hd=True maps) with generated patterns: shell scutes, feather scallops, chitin
plates."""
import math

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


CRANE_EYE = {
    'neutral': {1: '..rr..', 2: '.rEEr.', 3: '.rEhr.', 4: '..rr..'},
    'blink': {3: '.kkkk.', 4: '..rr..'},
    'angry': {1: 'kk....', 2: '.kEEr.', 3: '.rEEr.', 4: '..rr..'},
    'hurt': {1: '.k..k.', 2: '..kk..', 3: '.k..k.'},
    'dead': {1: '.E..E.', 2: '..EE..', 3: '.E..E.'},
}


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
    """The Weaver's brood grew up into full Sculk Spiders: see sculk_spider() at the bottom."""
    return sculk_spider()


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
    # C3 Conductor: slanted glowing eye slits, as on the Conductor's own mask (tools/conductor.py)
    def _slit(x, y):
        u = (8 - x) if x < 10 else (x - 11)
        return 0 <= u <= 5 and abs(y - (11.5 - u * 0.5)) < 0.9
    face = gen(20, 28, lambda x, y: (
        ('g' if 1 <= ((8 - x) if x < 10 else (x - 11)) <= 4 else 'v') if _slit(x, y) else
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
    for side, sx in (('left', 1), ('right', -1)):
        # C3 Conductor: the lower pair of his four horn-ears, swept out to the sides
        low = mask.part(f'{side}_horn_lower', pivot=(4.5 * sx, -10, 0), rot=(0, 0, 1.15 * sx))
        low.cube((-0.75, -3.5, -0.75), (1.5, 3.5, 1.5), color='horn', pattern='mc', clusters=0.0, rim=False)
        low.cube((-0.4, -6, -0.4), (0.8, 2.5, 0.8), color='horn_l', pattern='mc', clusters=0.0, rim=False,
                 faces={'up': dict(color='glow', pattern='mc', clusters=0.0, glow=True)})
    crown = mask.part('crown', pivot=(0, -14, 0))
    for i, x in enumerate((-3, -1, 1, 3)):
        crown.cube((x - 0.5, -2 - (i in (1, 2)) * 1.5, -0.5), (1, 2 + (i in (1, 2)) * 1.5, 1), color='gold', pattern='mc', clusters=0.0, rim=False,
                   faces={'up': dict(color='gold_l', pattern='mc', clusters=0.0)})
    return m


ALL = {'strummer': strummer, 'strumling': strumling,
       'conductor_mask': conductor_mask}


# =========================================================================== SCULK REMAKE
# The three great players, remade as Warden-kin: near-black hide crusted with teal sculk that
# glows in specks, bone showing through everywhere - ribcages, skulls, claws, teeth - glowing
# hearts in their chests and the Warden's twitching tendrils.
WARDEN = {
    'hide': '#0d1217', 'hide_l': '#16222a', 'hide_d': '#070a0d',
    'sculk': '#034150', 'sculk_l': '#074857', 'sculk_d': '#062e37',
    'bone': '#bbc39b', 'bone_l': '#d1d6b6', 'bone_d': '#819988', 'bone_k': '#4e5c55',
    'glow': '#29dfeb', 'glow_d': '#0f8c99', 'void': '#04070a', 'tooth': '#e2e6cc',
}


def sculk_patches(w, h, seed, density=0.5, glow=0.18):
    """Hide crusted with sculk: soft-edged teal blobs, with glowing specks inside them."""
    import random
    rnd = random.Random(seed)
    blobs = [(rnd.uniform(0, w), rnd.uniform(0, h), rnd.uniform(1.5, 4.5)) for _ in range(max(1, int(w * h * density / 40)))]

    def fn(x, y):
        best = 0.0
        for (bx, by, r) in blobs:
            d = ((x - bx) ** 2 + (y - by) ** 2) ** 0.5
            best = max(best, 1.0 - d / r)
        if best <= 0.0:
            return 'k' if rnd.random() < 0.04 else '.'
        if best > 0.55 and rnd.random() < glow:
            return 'g'
        return 'S' if best > 0.45 else 's'
    return gen(w, h, fn)


def ribcage(w, h, heart=True):
    """A ribcage seen from the front: a sternum down the middle, ribs arching out from it, and a
    glowing heart showing through the gaps."""
    cx = (w - 1) / 2

    def fn(x, y):
        dx = abs(x - cx)
        if dx < 1.0 and y < h - 2:
            return 'l' if y % 3 == 0 else 'b'
        if heart and dx < 3.2 and abs(y - h * 0.42) < 2.6 - dx * 0.5:
            return 'G' if dx < 1.6 and abs(y - h * 0.42) < 1.2 else 'g'
        rib = (y - dx * 0.35) % 4
        reach = w / 2 - 1 - y * 0.12
        if dx < reach and dx > 1.0:
            if rib < 1.0:
                return 'l'
            if rib < 2.0:
                return 'b'
            if rib < 2.6:
                return 'd'
        return '.'
    return gen(w, h, fn)


def fangs(w, h, step=3):
    """Hanging teeth like the Warden's: pale at the root, darkening and tapering to points."""
    def fn(x, y):
        i = x % step
        length = h - (x * 7 % 3)
        if i == step - 1 or y >= length:
            return '.'
        width = step - 1 - int(y / max(1, length) * (step - 1) + 0.4)
        if i >= width:
            return '.'
        return 'l' if y == 0 else ('b' if y < length * 0.6 else 'd')
    return gen(w, h, fn)


def vertebrae(w, h):
    def fn(x, y):
        if y % 3 == 2:
            return 'k'
        return 'l' if x == 0 or y % 3 == 0 else 'b'
    return gen(w, h, fn)


WK = {'s': 'sculk', 'S': 'sculk_l', 'g': 'glow', 'G': 'glow', 'k': 'hide_l', 'b': 'bone', 'l': 'bone_l', 'd': 'bone_d', 'v': 'void', 'w': 'tooth'}
WGLOW = 'gG'


def hide_face(w, h, seed, density=0.5, glow=0.18, **extra):
    """A face spec: dark hide crusted with glowing sculk."""
    spec = dict(color='hide', pattern='mc', clusters=0.3, hd=True, map=sculk_patches(w, h, seed, density, glow), keys=WK, glow_keys=WGLOW)
    spec.update(extra)
    return spec


def tendril(part, name, pivot, rot, length=6):
    """A Warden tendril: a flat glowing curl."""
    t = part.part(name, pivot=pivot, rot=rot)
    rows = gen(4, length * 2, lambda x, y: ('g' if (x == 1 or x == 2) else ('G' if x == 0 and y % 3 == 0 else '.'))
               if y < length * 2 - 3 else ('g' if x == (y % 2) + 1 else '.'))
    t.cube((-1, -length, 0), (2, length, 0), color='glow_d', pattern='mc', clusters=0.0, rim=False, faces={
        f: dict(color='glow_d', pattern='mc', clusters=0.0, rim=False, hd=True, map=[r.replace('.', '_') for r in rows], keys={'g': 'glow', 'G': 'tooth'},
                glow_keys='gG') for f in ('north', 'south')})
    return t


WARDEN_SKULL = {
    # 18 x 14 texels: deep sockets with a glowing pupil each, a ridge of bone, nostril pits
    'neutral': {1: '.llllllllllllllll.', 2: 'lbbbbbbbbbbbbbbbbl', 3: 'bvvvvbbbbbbbbvvvvb', 4: 'vvggvvbbbbbbvvggvv', 5: 'vvGgvvbbbbbbvvgGvv',
                6: 'bvvvvbbbbbbbbvvvvb', 7: '.bbbbbbbddbbbbbbb.', 8: '..bbbbbvbbvbbbbb..', 9: '..dbbbbbbbbbbbbd..'},
    'blink': {1: '.llllllllllllllll.', 2: 'lbbbbbbbbbbbbbbbbl', 3: 'bddddbbbbbbbbddddb', 4: 'vvvvvvbbbbbbvvvvvv', 6: 'bbbbbbbbbbbbbbbbbb',
              7: '.bbbbbbbddbbbbbbb.', 8: '..bbbbbvbbvbbbbb..', 9: '..dbbbbbbbbbbbbd..'},
    'angry': {1: 'll..............ll', 2: 'lbll..........llbl', 3: 'bvvvvlbbbbbblvvvvb', 4: 'vvGGvvbbbbbbvvGGvv', 5: 'vvGGvvbbbbbbvvGGvv',
              6: 'bvvvvbbbbbbbbvvvvb', 7: '.bbbbbbbddbbbbbbb.', 8: '..bbbbbvbbvbbbbb..', 9: '..dbbbbbbbbbbbbd..'},
    'hurt': {1: '.llllllllllllllll.', 2: 'lbbbbbbbbbbbbbbbbl', 3: 'bvbbvbbbbbbbbvbbvb', 4: 'bbvvbbbbbbbbbbvvbb', 5: 'bvbbvbbbbbbbbvbbvb',
             7: '.bbbbbbbddbbbbbbb.', 8: '..bbbbbvbbvbbbbb..'},
    'dead': {1: '.llllllllllllllll.', 2: 'lbbbbbbbbbbbbbbbbl', 3: 'bvbbvbbbbbbbbvbbvb', 4: 'bbvvbbbbbbbbbbvvbb', 5: 'bvbbvbbbbbbbbvbbvb',
             7: '.bbbbbbbddbbbbbbb.', 8: '..bbbbbbbbbbbbbb..'},
}


def vent(body, name, pivot, rot):
    """A sculk vent (the Thumper's weak point): a pit of living sculk with a glowing heart, a raised
    ring of bone round it, a bone lid hinged at its front edge, and the pit's lit twin - a glowing
    shell just over it that ThumperModel shows while the vent is open."""
    v = body.part(name, pivot=pivot, rot=rot)
    pit = gen(10, 10, lambda x, y: 'G' if abs(x - 4.5) + abs(y - 4.5) < 2.2 else 'g' if abs(x - 4.5) + abs(y - 4.5) < 3.4 else
              ('v' if (x * 7 + y * 3) % 9 == 0 else ('S' if (x + y) % 3 == 0 else 's')))
    v.cube((-2.5, -0.75, -2.5), (5, 1, 5), color='sculk', pattern='mc', clusters=0.2, rim=False, faces={
        'up': dict(color='sculk', pattern='mc', clusters=0.0, hd=True, map=pit, keys=WK, glow_keys=WGLOW)})
    bone = dict(color='bone', pattern='mc', clusters=0.0, rim=False)
    for (x, z, w, d) in ((-3, -3, 6, 0.75), (-3, 2.25, 6, 0.75), (-3, -2.25, 0.75, 4.5), (2.25, -2.25, 0.75, 4.5)):
        v.cube((x, -1.5, z), (w, 1.5, d), **bone)
    lit = v.part(f'{name}_lit', pivot=(0, 0, 0))
    lit.cube((-2.5, -0.75, -2.5), (5, 1, 5), inflate=0.2, color='glow', pattern='mc', clusters=0.0, rim=False, glow=True, faces={
        'up': dict(color='glow', pattern='mc', clusters=0.0, rim=False, hd=True, map=[r.replace('s', 'g').replace('S', 'G').replace('v', 'g') for r in pit],
                   keys=WK, glow_keys=WGLOW)})
    lid = v.part(f'{name}_lid', pivot=(0, -1.5, -3))
    plate = gen(12, 12, lambda x, y: 'd' if min(x, y, 11 - x, 11 - y) == 0 else ('l' if (x + y) % 5 == 0 else ('k' if abs(x - y) < 1 or abs(x + y - 11) < 1 else 'b')))
    lid.cube((-3, -0.75, 0), (6, 0.75, 6), **bone, faces={'up': dict(**bone, hd=True, map=plate, keys=WK),
                                                        'down': dict(color='sculk_d', pattern='mc', clusters=0.0, rim=False)})
    return v


def thumper_sculk() -> Model:
    """The Thumper, remade: a Warden-kin turtle. Its shell is sculk-crusted hide with great bone
    ribs arching over it like a cage, glowing specks in every seam; under it a ribcage with a
    beating heart of light. Its head is a turtle's skull - deep sockets with glowing pupils, a
    hooked bone beak, a jaw of hanging fangs - with two Warden tendrils that twitch. Along its shell,
    three sculk vents under bone lids: its only weak points, and only while they gape open."""
    pal = dict(SCULK)
    pal.update(WARDEN)
    m = Model('thumper', (256, 352), pal, {'thumper': {}}, res=2, expressions=EXPR)
    body = m.part('body', pivot=(0, 14, 0))
    # the belly: a ribcage with the heart glowing through
    body.cube((-10, 1, -12), (20, 3, 24), color='hide', pattern='mc', clusters=0.2, faces={
        'down': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=ribcage(40, 48), keys=WK, glow_keys=WGLOW),
        'north': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=fangs(40, 6), keys=WK),
    })
    # the shell: rim, dome and crown, all crusted with sculk
    body.cube((-12, -3, -14), (24, 4, 28), color='hide', pattern='mc', clusters=0.3, faces={
        **{f: hide_face(56 if f in ('east', 'west') else 48, 8, 11 + i, 0.6) for i, f in enumerate(('north', 'south', 'east', 'west'))},
        'up': hide_face(48, 56, 15, 0.55),
        'down': dict(color='hide_d', pattern='mc', clusters=0.0),
    })
    body.cube((-10, -8, -12), (20, 5, 24), color='hide', pattern='mc', clusters=0.3, faces={
        **{f: hide_face(48 if f in ('east', 'west') else 40, 10, 21 + i, 0.6) for i, f in enumerate(('north', 'south', 'east', 'west'))},
        'up': hide_face(40, 48, 25, 0.6),
    })
    body.cube((-7, -11, -9), (14, 3, 18), color='hide', pattern='mc', clusters=0.3, faces={
        **{f: hide_face(36 if f in ('east', 'west') else 28, 6, 31 + i, 0.7) for i, f in enumerate(('north', 'south', 'east', 'west'))},
        'up': hide_face(28, 36, 35, 0.7, 0.25),
    })
    # great bone ribs arching over the shell, front to back
    bone = dict(color='bone', pattern='mc', clusters=0.0, rim=False)
    for i, z in enumerate((-10, -5, 5, 10)):
        for sx in (1, -1):
            body.cube((10.5 * sx - (1 if sx > 0 else 0) * 0 - 0.75, -7, z - 0.75), (1.5, 9, 1.5), **bone, faces={
                'north': dict(**bone, hd=True, map=['ll', 'bb'] * 8 + ['dd', 'dd'], keys=WK)})
            body.cube((7.5 * sx - 0.75, -10, z - 0.75), (1.5, 3.5, 1.5), **bone)
        body.cube((-7.5, -11.5, z - 0.75), (15, 1.5, 1.5), **bone, faces={
            'up': dict(**bone, hd=True, map=['l' * 30, 'b' * 30, 'd' * 30], keys=WK)})
    # sculk growths along the ridge, like shriekers, each with a glowing mouth
    for i, z in enumerate((-7, 7)):
        g = body.part(f'growth_{i}', pivot=(0, -11, z))
        g.cube((-1.5, -3, -1.5), (3, 3, 3), color='sculk', pattern='mc', clusters=0.2,
               faces={'up': dict(color='void', pattern='mc', clusters=0.0, hd=True, map=['......', '.gggg.', '.gGGg.', '.gGGg.', '.gggg.', '......'],
                                 keys=WK, glow_keys=WGLOW)})
        g.cube((-2.5, -4, -0.25), (5, 1, 0.5), color='bone', pattern='mc', clusters=0.0, rim=False)
    # three sculk vents - the crown of the shell and both flanks - each a pit of living sculk in a
    # ring of bone under a hinged bone lid; the lid flips open when it strains and the pit lights up
    for name, pivot, rot in (('vent_top', (0, -11, 1), (0, 0, 0)), ('vent_left', (12, -1, 0), (0, 0, 1.5708)),
                             ('vent_right', (-12, -1, 0), (0, 0, -1.5708))):
        vent(body, name, pivot, rot)
    # neck and skull head
    neck = body.part('neck', pivot=(0, -1, -12))
    neck.cube((-3.5, -3.5, -6), (7, 7, 7), color='hide', pattern='mc', clusters=0.3, faces={
        'east': hide_face(14, 14, 41), 'west': hide_face(14, 14, 42), 'up': hide_face(14, 14, 43),
        'down': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=vertebrae(14, 14), keys=WK)})
    head = neck.part('head', pivot=(0, -1, -6))
    head.cube((-4.5, -5, -8), (9, 7, 8), color='bone', pattern='mc', clusters=0.2, faces={
        'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=hd_rows(WARDEN_SKULL['neutral'], 18, 14), keys=WK, glow_keys=WGLOW,
                      expr={k: hd_rows(v, 18, 14) for k, v in WARDEN_SKULL.items() if k != 'neutral'}),
        'up': hide_face(18, 16, 44, 0.8),
        'east': hide_face(16, 14, 45, 0.6), 'west': hide_face(16, 14, 46, 0.6),
    })
    # the hooked bone beak
    head.cube((-2, -1, -10), (4, 3, 2), color='bone_l', pattern='mc', clusters=0.0, rim=False, faces={'up': dict(color='bone_l', pattern='mc', clusters=0.0)})
    head.cube((-1, 2, -10), (2, 1, 1), color='bone_d', pattern='mc', clusters=0.0, rim=False)
    for side, sx in (('left', 1), ('right', -1)):
        brow = head.part(f'{side}_brow', pivot=(2.6 * sx, -4.2, -8))
        brow.cube((-2, -0.75, -1), (4, 1.5, 1.5), color='bone_d', pattern='mc', clusters=0.0, rim=False,
                  faces={'up': dict(color='bone', pattern='mc', clusters=0.0)})
        tendril(head, f'{side}_tendril', (3.5 * sx, -5, -3), (0.2, 0, 0.6 * sx), 7)
    jaw = head.part('jaw', pivot=(0, 2, -1))
    jaw.cube((-4, 0, -7), (8, 2, 7), color='bone_d', pattern='mc', clusters=0.3, faces={
        'up': dict(color='void', pattern='mc', clusters=0.0, hd=True, map=['.' * 16, '.wwwwwwwwwwwwww.'] + ['.' * 16] * 12, keys=WK),
        'north': dict(color='bone_d', pattern='mc', clusters=0.0, hd=True, map=fangs(16, 4, 2), keys=WK),
        'down': dict(color='hide', pattern='mc', clusters=0.3)})
    jaw.cube((-1, -1, -8), (2, 3, 1), color='bone', pattern='mc', clusters=0.0, rim=False)
    # columns of legs: hide with bone knee plates and long bone claws
    for name, sx, sz in (('front_left', 1, -1), ('front_right', -1, -1), ('hind_left', 1, 1), ('hind_right', -1, 1)):
        leg = body.part(f'{name}_leg', pivot=(9 * sx, 2, 8.5 * sz))
        leg.cube((-3, -1, -3), (6, 9, 6), color='hide', pattern='mc', clusters=0.3, faces={
            'north': hide_face(12, 18, 50 + sx + sz * 3, 0.6, bands=None),
            'east': hide_face(12, 18, 60 + sx + sz * 3, 0.6), 'west': hide_face(12, 18, 70 + sx + sz * 3, 0.6)})
        leg.cube((-2.5, 0, -3.5), (5, 3, 1), color='bone', pattern='mc', clusters=0.0, rim=False)
        leg.cube((-3.5, 6, -3.5), (7, 2, 7), color='hide_d', pattern='mc', clusters=0.3, faces={
            'north': dict(color='hide_d', pattern='mc', clusters=0.0, hd=True, map=['ww.ww.ww.ww.ww', 'll.ll.ll.ll.ll', 'bb.bb.bb.bb.bb', 'dd.dd.dd.dd.dd'], keys=WK)})
    tail = body.part('tail', pivot=(0, 0, 13))
    tail.cube((-1.5, -1, 0), (3, 3, 5), color='hide', pattern='mc', clusters=0.3, faces={'up': dict(color='bone', pattern='mc', clusters=0.0, hd=True,
                                                                                                  map=vertebrae(6, 10), keys=WK)})
    tail.cube((-1, -0.5, 5), (2, 2, 3), color='bone_d', pattern='mc', clusters=0.0, rim=False)
    return m


WARDEN_SPIDER = {
    # 16 x 10 texels: eight glowing eyes in bone-ringed sockets, and fangs
    'neutral': {0: '.dd..dddd..dd...', 1: 'dggd.dGGd.dggd..', 2: 'dggd.dGGd.dggd..', 3: '.dd..dddd..dd...', 4: 'dgd.......dgd...',
                6: '...wwww..wwww...', 7: '....ww....ww....', 8: '....w......w....'},
    'blink': {1: 'dddd.dddd.dddd..', 4: 'ddd.......ddd...', 6: '...wwww..wwww...', 7: '....ww....ww....'},
    'angry': {0: 'd..........d....', 1: 'dGGd.dGGd.dGGd..', 2: 'dggd.dGGd.dggd..', 3: '.dd..dddd..dd...', 4: 'dGd.......dGd...',
              5: '...wwww..wwww...', 6: '...w..w..w..w...', 7: '....ww....ww....', 8: '....w......w....'},
    'hurt': {1: 'd..d.d..d.d..d..', 2: '.dd...dd...dd...', 3: 'd..d.d..d.d..d..', 6: '....ww....ww....'},
    'dead': {1: 'd..d.d..d.d..d..', 2: '.dd...dd...dd...', 3: 'd..d.d..d.d..d..', 6: '....ww....ww....'},
}
MANTIS_SKULL = {
    'neutral': {1: '..bbbbbb..', 2: '.bvbbbbvb.', 3: '..bwwwwb..', 4: '...wvvw...', 5: '...w..w...'},
    'blink': {1: '..bbbbbb..', 2: '.bdbbbbdb.', 3: '..bwwwwb..', 4: '...wvvw...'},
    'angry': {1: '.bbbbbbbb.', 2: '.bvbbbbvb.', 3: '.bwwwwwwb.', 4: '.wvvvvvvw.', 5: '.w.w..w.w.'},
    'hurt': {1: '..bbbbbb..', 2: '.bdbbbbdb.', 3: '...wwww...'},
    'dead': {1: '..bbbbbb..', 2: '.bdbbbbdb.', 3: '...wwww...'},
}


def strummer_sculk() -> Model:
    """The Strummer, remade: a Warden-kin mantis of bone riding a sculk spider. The spider is a
    black, sculk-crusted body on legs of hide and bone, eight glowing eyes in bone sockets, a
    swollen abdomen like a sculk sensor - glowing sound holes and two twitching tendrils - with
    the strings running from it up to the mantis's hands. The mantis is a skeleton: a spine with
    ribs, a triangular skull with two huge glowing compound eyes and tendril antennae, tattered
    sculk wings, and scythe arms of bare bone."""
    pal = dict(SCULK)
    pal.update(WARDEN)
    pal.update({'string': '#7ff7ff', 'membrane': '#0b2a31', 'membrane_l': '#11414a', 'eye_f': '#0a3f46'})
    m = Model('strummer', (128, 160), pal, {'strummer': {}}, res=2, expressions=EXPR)
    spider = m.part('spider', pivot=(0, 17.5, 0))
    spider.cube((-5, -3, -5), (10, 6, 10), color='hide', pattern='mc', clusters=0.3, faces={
        'up': hide_face(20, 20, 110, 0.7, 0.25), 'east': hide_face(20, 12, 111), 'west': hide_face(20, 12, 112),
        'down': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=ribcage(20, 20, heart=False), keys=WK)})
    head = spider.part('spider_head', pivot=(0, 0, -5))
    head.cube((-4, -2.5, -5), (8, 5, 5), color='bone', pattern='mc', clusters=0.2, faces={
        'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=hd_rows(WARDEN_SPIDER['neutral'], 16, 10), keys=EK_SPIDER, glow_keys=WGLOW,
                      expr={k: hd_rows(v, 16, 10) for k, v in WARDEN_SPIDER.items() if k != 'neutral'}),
        'up': hide_face(16, 10, 113, 0.8), 'east': hide_face(10, 10, 114), 'west': hide_face(10, 10, 115)})
    for side, sx in (('left', 1), ('right', -1)):
        fang = head.part(f'{side}_fang', pivot=(1.5 * sx, 2, -5))
        fang.cube((-0.75, 0, -0.75), (1.5, 3.5, 1.5), color='bone_l', pattern='mc', clusters=0.0, rim=False, faces={
            'down': dict(color='bone_d', pattern='mc', clusters=0.0)})
    # the abdomen: a living sculk sensor - glowing sound holes, crusted hide, and tendrils
    abd = spider.part('abdomen', pivot=(0, -1, 4.5), rot=(-0.15, 0, 0))
    holes = gen(28, 30, lambda x, y: 'G' if any(((x - cx) ** 2 + (y - cy) ** 2) < r for (cx, cy, r) in ((13.5, 12, 9), (6, 22, 4), (21, 22, 4))) else (
        'g' if any(((x - cx) ** 2 + (y - cy) ** 2) < r for (cx, cy, r) in ((13.5, 12, 20), (6, 22, 10), (21, 22, 10))) else (
            'b' if any(abs(((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 - rr) < 0.8 for (cx, cy, rr) in ((13.5, 12, 5.2), (6, 22, 3.7), (21, 22, 3.7))) else
            ('s' if (x * 5 + y * 3) % 7 == 0 else ('S' if (x * 3 + y * 5) % 11 == 0 else '.')))))
    abd.cube((-7, -7, 0), (14, 10, 15), color='hide', pattern='mc', clusters=0.3, faces={
        'up': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=holes, keys=WK, glow_keys=WGLOW),
        'south': hide_face(28, 20, 116, 0.8, 0.3),
        'east': hide_face(30, 20, 117, 0.7), 'west': hide_face(30, 20, 118, 0.7),
        'down': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=fangs(28, 30, 4), keys=WK),
    })
    abd.cube((-1.5, -2, 15), (3, 3, 2), color='bone', pattern='mc', clusters=0.0, rim=False, faces={'south': dict(color='glow', pattern='mc', clusters=0.0, glow=True)})
    for side, sx in (('left', 1), ('right', -1)):
        tendril(abd, f'{side}_abdomen_tendril', (4 * sx, -7, 6), (-0.3, 0, 0.5 * sx), 8)
    # eight legs: a femur of crusted hide, a shin of bare bone with a claw
    for i, z in enumerate((-3.5, -1.2, 1.2, 3.5)):
        for side, sx in (('left', 1), ('right', -1)):
            yaw = (0.55, 0.2, -0.2, -0.55)[i] * sx
            leg = spider.part(f'{side}_leg_{i}', pivot=(4.5 * sx, 0, z), rot=(0, yaw, -0.75 * sx))
            leg.cube((0 if sx > 0 else -10, -0.75, -0.75), (10, 1.5, 1.5), color='hide', pattern='mc', clusters=0.0, rim=False,
                     faces={'up': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=['s.g.' * 5, '.S..' * 5, '....' * 5], keys=WK, glow_keys=WGLOW)})
            shin = leg.part(f'{side}_shin_{i}', pivot=(10 * sx, 0, 0), rot=(0, 0, 1.75 * sx))
            shin.cube((0 if sx > 0 else -15, -0.6, -0.6), (15, 1.2, 1.2), color='bone', pattern='mc', clusters=0.0, rim=False,
                      faces={'up': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=['ld' * 15, 'bb' * 15], keys=WK)})
    # the mantis: a skeleton on the spider's back
    mantis = spider.part('mantis', pivot=(0, -3, -1.5), rot=(0.08, 0, 0))
    mantis.cube((-2, -11, -2), (4, 11, 4), color='hide', pattern='mc', clusters=0.0, faces={
        'north': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=ribcage(8, 22), keys=WK, glow_keys=WGLOW),
        'south': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=vertebrae(8, 22), keys=WK),
        'east': hide_face(8, 22, 120), 'west': hide_face(8, 22, 121)})
    mantis.cube((-2.5, -4, -2.5), (5, 4, 5), color='bone', pattern='mc', clusters=0.0, faces={'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True,
                                                                                                         map=fangs(10, 8, 2), keys=WK)})
    mk = {'b': 'bone', 'g': 'glow_d', 'G': 'glow', 'm': 'membrane_l', '_': '_'}
    for side, sx in (('left', 1), ('right', -1)):
        w = mantis.part(f'{side}_mantis_wing', pivot=(1.5 * sx, -9, 2), rot=(0.35, 0.2 * sx, 0.12 * sx))
        w.cube((-1.5, 0, 0), (3, 12, 0), color='membrane', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='membrane', pattern='mc', clusters=0.0, rim=False, hd=True, map=gen(6, 24, lambda x, y: '_' if (y > 18 and (x + y) % 3 == 0) or (y == 23)
                                                                                                else 'b' if x in (0, 5) and y < 8 else ('G' if x == 2 and y % 4 == 0 else ('g' if x == 2 else '.'))),
                    keys=mk, glow_keys='gG') for f in ('north', 'south')})
    mhead = mantis.part('mantis_head', pivot=(0, -11, -0.5))
    mhead.cube((-2.5, -3.5, -2.5), (5, 3.5, 3), color='bone', pattern='mc', clusters=0.0, faces={
        'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=hd_rows(MANTIS_SKULL['neutral'], 10, 7), keys=WK,
                      expr={k: hd_rows(v, 10, 7) for k, v in MANTIS_SKULL.items() if k != 'neutral'}),
        'up': hide_face(10, 6, 122, 0.9, 0.3)})
    for side, sx in (('left', 1), ('right', -1)):
        eye = mhead.part(f'{side}_mantis_eye', pivot=(2.5 * sx, -3, -1))
        eye.cube((-1.25, -1.5, -1.5), (2.5, 3, 3), color='glow', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='glow', pattern='mc', clusters=0.0, rim=False, hd=True, map=compound(6, 6), keys={'f': 'eye_f', 'g': 'glow_d', 'G': 'glow'},
                    glow_keys='gG') for f in ('north', 'south', 'east', 'west', 'up')})
        ant = tendril(mhead, f'{side}_antenna', (0.8 * sx, -3.5, -2), (-0.6, 0, 0.25 * sx), 9)
    for side, sx in (('left', 1), ('right', -1)):
        arm = mantis.part(f'{side}_arm', pivot=(2.2 * sx, -9.5, -1.5), rot=(-0.7, 0, -0.15 * sx))
        arm.cube((-0.75, 0, -0.75), (1.5, 5, 1.5), color='bone', pattern='mc', clusters=0.0, rim=False)
        fem = arm.part(f'{side}_femur', pivot=(0, 5, 0), rot=(-1.4, 0, 0))
        fem.cube((-0.75, 0, -0.75), (1.5, 7, 1.5), color='bone', pattern='mc', clusters=0.0, faces={'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True,
                                                                                                                  map=['ll.'] + ['w..', 'd..'] * 6 + ['...'], keys=WK)})
        for k in range(3):
            fem.cube((-0.25, 1.5 + k * 2, -1.5), (0.5, 1, 0.75), color='tooth', pattern='mc', clusters=0.0, rim=False)
        tib = fem.part(f'{side}_hand', pivot=(0, 7, 0), rot=(2.5, 0, 0))
        tib.cube((-0.5, 0, -0.5), (1, 5, 1), color='bone_l', pattern='mc', clusters=0.0, rim=False)
        tib.cube((-0.5, 4.5, -1.5), (1, 1, 1.5), color='tooth', pattern='mc', clusters=0.0, rim=False)
        strings = tib.part(f'{side}_strings', pivot=(0, 4.5, 0), rot=(-1.1, 0, 0))
        strings.cube((0, 0, -1.5), (0, 12, 3), color='string', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='string', pattern='mc', clusters=0.0, rim=False, hd=True, map=['s_s_s_'[::(1 if f == 'east' else -1)]] * 24,
                    keys={'s': 'string'}, glow_keys='s') for f in ('east', 'west')})
    return m


EK_SPIDER = dict(WK)
EK_SPIDER.update({'d': 'bone_d', 'v': 'void'})

def _resculk(fn, colours):
    """A minion, repainted in Warden-kin colours (same shapes, new palette)."""
    def make():
        m = fn()
        m.palette.update(colours)
        return m
    return make


ALL.update({
    'thumper': thumper_sculk, 'strummer': strummer_sculk,
})


# =========================================================================== THE SCULK SPIDER (entity id strumling)
# The Weaver's brood, grown into full Warden-kin spiders. Model units: the body hangs 8 above the
# ground and every leg is a raised femur, a long tibia and a needle tarsus that it walks on tiptoe.
SCULK_SPIDER_EYES = {
    # 12 x 10 texels on the front of the head: two great eyes, a row of four, a scatter of small
    # ones - eight in all, ringed in bone - over the roots of the fangs
    'neutral': {0: '.dd..dd..dd.', 1: 'dGGd.dd.dGGd', 2: 'dGgd.gg.dgGd', 3: '.dd.dGGd.dd.', 4: 'g...dggd...g', 5: '.g..d..d..g.',
                6: '...bbbbbb...', 7: '..wb.vv.bw..', 8: '..w..vv..w..'},
    'blink': {0: '.dd..dd..dd.', 1: 'dddd.dd.dddd', 2: 'dddd.dd.dddd', 3: '.dd.dddd.dd.', 4: 'd...dddd...d', 6: '...bbbbbb...',
              7: '..wb.vv.bw..', 8: '..w..vv..w..'},
    'angry': {0: 'll........ll', 1: 'dGll.dd.llGd', 2: 'dGGd.GG.dGGd', 3: '.dd.dGGd.dd.', 4: 'G...dGGd...G', 5: '.G..d..d..G.',
              6: '..wbbbbbbw..', 7: '..wwvvvvww..', 8: '..w.vvvv.w..'},
    'hurt': {1: 'd..d.dd.d..d', 2: '.dd..gg..dd.', 3: 'd..ddGGdd..d', 4: 'g...dggd...g', 6: '...bbbbbb...', 7: '...b.vv.b...'},
    'dead': {1: 'd..d.dd.d..d', 2: '.dd..dd..dd.', 3: 'd..ddddd...d', 6: '...bbbbbb...', 7: '...b....b...'},
}
SPIDER_LEG_Z = (-2.6, -0.9, 0.9, 2.6)
SPIDER_LEG_YAW = (0.62, 0.22, -0.22, -0.62)


def bone_plate(w, h, seed):
    """A curved plate of bone over the abdomen: a ridge down the middle, worn darker edges, growth
    lines across it and specks of sculk in the cracks."""
    import random
    rnd = random.Random(seed)
    cx = (w - 1) / 2

    def fn(x, y):
        dx = abs(x - cx)
        if dx < 0.6:
            return 'l'
        if dx > w / 2 - 1.2 or y == h - 1:
            return 'd' if rnd.random() < 0.7 else 's'
        if y % 3 == 0:
            return 'd'
        return 'S' if rnd.random() < 0.05 else 'b'
    return gen(w, h, fn)


def sculk_spider() -> Model:
    """A Sculk Spider: the Weaver's brood, grown. A black cephalothorax crusted with sculk and
    ridged with bone; a bone-faced head with eight glowing eyes in sockets and two long hooked
    fangs between a pair of feeling palps; a swollen abdomen armoured in three overlapping bone
    plates, with glowing sculk sacs bulging between them, glowing spinnerets and two Warden
    tendrils; and eight long legs, each a crusted femur raised high, a bone tibia and a needle
    tarsus with a claw - so it stands tall and walks on tiptoe."""
    pal = dict(SCULK)
    pal.update(WARDEN)
    pal.update({'sac': '#2fe8f0', 'sac_d': '#0f8c99'})
    m = Model('strumling', (128, 128), pal, {'strumling': {}}, res=2, expressions=EXPR)
    body = m.part('body', pivot=(0, 16, 0))
    ridge = [''.join('l' if x in (6, 7) else c for x, c in enumerate(row)) for row in sculk_patches(14, 14, 301, 0.8, 0.3)]
    body.cube((-3.5, -2.5, -3.5), (7, 5, 7), color='hide', pattern='mc', clusters=0.3, faces={
        'up': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=ridge, keys=WK, glow_keys=WGLOW),
        'east': hide_face(14, 10, 302), 'west': hide_face(14, 10, 303),
        'down': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=ribcage(14, 14, heart=False), keys=WK)})
    head = body.part('head', pivot=(0, -0.3, -3.5))
    head.cube((-3, -2.5, -5), (6, 5, 5), color='bone', pattern='mc', clusters=0.2, faces={
        'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=hd_rows(SCULK_SPIDER_EYES['neutral'], 12, 10), keys=EK_SPIDER,
                      glow_keys=WGLOW, expr={k: hd_rows(v, 12, 10) for k, v in SCULK_SPIDER_EYES.items() if k != 'neutral'}),
        'up': hide_face(12, 10, 304, 0.6), 'east': hide_face(10, 10, 305, 0.5), 'west': hide_face(10, 10, 306, 0.5)})
    # a brow ridge of bone over the eyes
    head.cube((-3.2, -3.0, -5.2), (6.4, 1, 2), color='bone_l', pattern='mc', clusters=0.0, rim=False,
              faces={'up': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=vertebrae(13, 4), keys=WK)})
    for side, sx in (('left', 1), ('right', -1)):
        fang = head.part(f'{side}_fang', pivot=(1.2 * sx, 1.6, -4.6), rot=(-0.25, 0, 0))
        fang.cube((-0.8, 0, -0.8), (1.6, 2.4, 1.6), color='hide', pattern='mc', clusters=0.0, rim=False,
                  faces={'north': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=['ss.', 'Sg.', '.s.', '...', '...'], keys=WK, glow_keys=WGLOW)})
        fang.cube((-0.5, 2.2, -0.9), (1, 2.2, 1), color='tooth', pattern='mc', clusters=0.0, rim=False,
                  faces={'down': dict(color='bone_d', pattern='mc', clusters=0.0)})
        fang.cube((-0.35, 4.2, -1.6), (0.7, 0.7, 1.2), color='bone_l', pattern='mc', clusters=0.0, rim=False)
        palp = head.part(f'{side}_palp', pivot=(2.4 * sx, 1.2, -4.4), rot=(0.5, 0.25 * sx, 0))
        palp.cube((-0.45, -0.45, -3.2), (0.9, 0.9, 3.2), color='bone_d', pattern='mc', clusters=0.0, rim=False)
        palp.cube((-0.55, -0.55, -3.9), (1.1, 1.1, 0.9), color='hide_l', pattern='mc', clusters=0.0, rim=False)
    # the abdomen: crusted hide under three overlapping plates of bone, glowing sacs between them
    abd = body.part('abdomen', pivot=(0, -0.8, 3.2), rot=(-0.28, 0, 0))
    abd.cube((-5, -5, 0), (10, 8, 11), color='hide', pattern='mc', clusters=0.3, faces={
        'up': hide_face(20, 22, 307, 0.9, 0.35), 'east': hide_face(22, 16, 308, 0.8, 0.3), 'west': hide_face(22, 16, 309, 0.8, 0.3),
        'south': hide_face(20, 16, 310, 0.9, 0.3),
        'down': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=fangs(20, 22, 4), keys=WK)})
    for i, (z, w) in enumerate(((0.6, 9.0), (4.0, 8.4), (7.4, 7.2))):
        abd.cube((-w / 2, -5.9, z), (w, 1.4, 3.4), color='bone', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=bone_plate(int(w * 2 + 0.99), 7, 311 + i), keys=WK),
            'south': dict(color='bone_d', pattern='mc', clusters=0.0, hd=True, map=['d' * 18, 'k' * 18, 'd' * 18], keys=WK)})
    for z in (2.6, 6.0):
        for sx in (1, -1):
            # glowing sacs bulging out between the plates
            abd.cube((3.2 * sx - 0.9, -6.1, z + 0.6), (1.8, 1.4, 1.8), color='sac', pattern='mc', clusters=0.0, rim=False, faces={
                f: dict(color='sac', pattern='mc', clusters=0.0, rim=False, glow=True) for f in ('up', 'north', 'south', 'east', 'west')})
    for sx in (1, -1):
        for i, z in enumerate((2.0, 6.5)):
            abd.cube((5 * sx - (0 if sx > 0 else 1.4), -3.4 + i * 0.6, z), (1.4, 2.2, 2.4), color='sac', pattern='mc', clusters=0.0, rim=False, faces={
                f: dict(color='sac', pattern='mc', clusters=0.0, rim=False, glow=True) for f in ('up', 'north', 'south', 'east', 'west', 'down')})
    abd.cube((-1.2, -1.6, 10.6), (2.4, 2, 1.4), color='bone_d', pattern='mc', clusters=0.0, rim=False, faces={
        'south': dict(color='glow', pattern='mc', clusters=0.0, glow=True)})
    for side, sx in (('left', 1), ('right', -1)):
        tendril(abd, f'{side}_tendril', (2.2 * sx, -6, 9), (-0.6, 0, 0.5 * sx), 4)
    # eight long legs: a raised femur, a long tibia, a needle tarsus
    for i, z in enumerate(SPIDER_LEG_Z):
        for side, sx in (('left', 1), ('right', -1)):
            leg = body.part(f'{side}_leg_{i}', pivot=(3.2 * sx, 0, z), rot=(0, SPIDER_LEG_YAW[i] * sx, -0.8 * sx))
            leg.cube((0 if sx > 0 else -9, -0.7, -0.7), (9, 1.4, 1.4), color='hide', pattern='mc', clusters=0.0, rim=False, faces={
                'up': dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=['s.g.Ss..sg.S.s.s.s', '.S..s..S...s..S...', 'k' * 18], keys=WK, glow_keys=WGLOW),
                'north': dict(color='hide_l', pattern='mc', clusters=0.0, hd=True, map=['b...b...b...b...b.', '..................', 'k' * 18], keys=WK)})
            leg.cube((9 * sx - 0.8, -0.9, -0.9), (1.6, 1.8, 1.8), color='bone_l', pattern='mc', clusters=0.0, rim=False)
            tib = leg.part(f'{side}_tibia_{i}', pivot=(9 * sx, 0, 0), rot=(0, 0, 1.7 * sx))
            tib.cube((0 if sx > 0 else -12, -0.55, -0.55), (12, 1.1, 1.1), color='bone', pattern='mc', clusters=0.0, rim=False, faces={
                'up': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=['ld' * 12, 'bk' * 12], keys=WK),
                'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=['l.' * 12, 'dd' * 12], keys=WK)})
            tar = tib.part(f'{side}_tarsus_{i}', pivot=(12 * sx, 0, 0), rot=(0, 0, 0.5 * sx))
            tar.cube((0 if sx > 0 else -5, -0.4, -0.4), (5, 0.8, 0.8), color='bone_d', pattern='mc', clusters=0.0, rim=False)
            tar.cube((5 * sx - (0 if sx > 0 else 0.9), -0.3, -0.3), (0.9, 0.6, 0.6), color='tooth', pattern='mc', clusters=0.0, rim=False)
    return m


ALL['strumling'] = sculk_spider

# B2 Thumper & cutscenes: the Thumper Titan (layered shell, vents, barnacles, scaled limbs) - see tools/thumper_titan.py
from thumper_titan import thumper_titan  # noqa: E402
ALL['thumper'] = thumper_titan

# C3 Conductor: the Conductor remade (mask, horn-ears, tattered tailcoat) and his kaiju final form - see tools/conductor.py
import conductor as _conductor  # noqa: E402
ALL['dictator'] = _conductor.dictator
ALL['dictator_kaiju'] = _conductor.dictator_kaiju
