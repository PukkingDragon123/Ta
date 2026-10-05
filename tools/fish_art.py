"""CR3 Fish & Coral Organs: the fish, drawn as hand-made pixel sprites and extruded into rounded bodies
(tools/fishkit.py): the Kazoo Fish, the Tubafish, the Fanfare Eel and the Sculk Fish, each with its own
colour variants (the entities pick one when they spawn and keep it in a bucket).

Model space: one sprite pixel = one unit, built at twice vanilla size and drawn at half size, y down,
ground at y = 24, the fish facing -z (its snout at the low columns of every sprite).
"""
from __future__ import annotations

import math
import os

import numpy as np
from PIL import Image

from fishkit import Sprite, SpriteModel, edge_mask, rgb, scale_pattern, shape_fin, tone

BLOCK_TEX = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/thesift/textures/block')


def _block_img(name):
    im = Image.open(os.path.join(BLOCK_TEX, name + '.png')).convert('RGBA')
    w, h = im.size
    return im.crop((0, 0, w, w)) if h > w else im


# ============================================================================ shared colouring

def paint_map(spr, pal, keys, *, rim=0.32, scales='', scale_phase=0, glow_keys='', overlay=None, overlay_on=''):
    """Colours a region map: keys {char: palette key}, a soft outline (each edge pixel a darker shade of
    itself), subtle overlapping scales over the regions in `scales`, glowing regions in `glow_keys`, and an
    overlay [(row, col, palette key, glows)] painted over the regions in `overlay_on`."""
    g = spr.grid
    m = spr.mask()
    h, w = g.shape
    out = np.zeros((h, w, 3), np.uint8)
    glow = np.zeros((h, w), bool)
    for y in range(h):
        for x in range(w):
            ch = g[y, x]
            if ch == '.':
                continue
            c = rgb(pal[keys[ch]]) if ch in keys else (255, 0, 255)
            if ch in scales:
                s = scale_pattern(x, y, scale_phase)
                c = tone(c, -0.09) if s < 0 else tone(c, 0.05) if s > 0 else c
            out[y, x] = c
            glow[y, x] = ch in glow_keys
    for (y, x, key, lit) in overlay or ():
        if 0 <= y < h and 0 <= x < w and g[y, x] in overlay_on:
            out[y, x] = rgb(pal[key])
            glow[y, x] = lit
    if rim:
        edge = edge_mask(m)
        for y, x in zip(*np.nonzero(edge)):
            if not glow[y, x]:
                out[y, x] = tone(out[y, x], -rim)
    return out, glow


FIN_KEYS = {'d': 'fin_base', 'f': 'fin', 'r': 'fin_ray', 't': 'fin_tip'}


def eye_rows(expr, kind):
    """Eyes as rows: w white, p pupil, i iris, h highlight, l lid, x dead cross."""
    sets = {
        'googly4': {'': ['.ww.', 'ppww', 'ppww', '.ww.'], 'blink': ['.ll.', 'llll', 'llll', '.ww.'], 'hurt': ['.pw.', 'wwpp', 'ppww', '.wp.'],
                    'dead': ['.ww.', 'wpwp', 'wwpw', '.pw.']},
        'googly3': {'': ['wpp', 'wpp', 'www'], 'blink': ['lll', 'lll', 'www'], 'hurt': ['pww', 'wpw', 'wwp'], 'dead': ['pwp', 'wpw', 'pwp']},
    }
    s = sets[kind]
    return s.get(expr, s[''])


# ============================================================================ the Kazoo Fish

KAZOO_PAL = {
    'back': '#1f8f96', 'sheen': '#86ece2', 'flank': '#3fd0c4', 'belly': '#d2fff0', 'gill': '#17707a', 'mouth': '#7a2238',
    'spot': '#ff6f8c', 'spot_d': '#d84a6a',
    'fin': '#f87d8d', 'fin_tip': '#ffc8d0', 'fin_ray': '#c8485c', 'fin_base': '#d65866',
    'kazoo': '#f87d8d', 'kazoo_l': '#ffb0bb', 'kazoo_d': '#b03a52', 'cap': '#fff4a8', 'cap_d': '#d8b850', 'hole': '#2a0e1c',
    'eye_w': '#ffffff', 'eye_p': '#101820', 'eye_rim': '#d8e0e6',
    'moss': '#34a2b0', 'moss_l': '#74dcd2', 'moss_d': '#1d6a78', 'stem': '#3f9a74', 'leaf': '#57c7a2', 'leaf_d': '#2f8a6a', 'bud': '#fff4a8',
    'bud_l': '#ffffff',
    'pattern': 'freckles',
}
KAZOO_VARIANTS = {
    # a teal reef fish freckled with coral, coral fins, a pink kazoo
    'kazoo_fish': {},
    # sunset gold banded in plum, violet fins, a teal kazoo
    'kazoo_fish_sunset': {'back': '#c4561c', 'sheen': '#ffd08a', 'flank': '#f59a2c', 'belly': '#fff0c8', 'gill': '#9a3a10', 'spot': '#7a2a72',
                          'spot_d': '#5a1a5a', 'fin': '#a24ad8', 'fin_tip': '#e8b8ff', 'fin_ray': '#6a2a9a', 'fin_base': '#7a3ab0',
                          'kazoo': '#3fc8c0', 'kazoo_d': '#1f8a8a', 'kazoo_l': '#9af0e8', 'pattern': 'bands'},
    # a sky-blue lagoon fish with white polka dots, lemon fins, a coral kazoo
    'kazoo_fish_lagoon': {'back': '#2a5cb0', 'sheen': '#a8dcff', 'flank': '#4a9eea', 'belly': '#eaf8ff', 'gill': '#1a3a80', 'spot': '#f6fcff',
                          'spot_d': '#c8e4fa', 'fin': '#f2cc34', 'fin_tip': '#fff6b0', 'fin_ray': '#c8961a', 'fin_base': '#d8a82a',
                          'kazoo': '#ff7a6a', 'kazoo_d': '#c84a4a', 'kazoo_l': '#ffb8a8', 'pattern': 'spots'},
    # a pale mint fish with a rose saddle and lilac fins
    'kazoo_fish_mint': {'back': '#2f8a5e', 'sheen': '#bff8d8', 'flank': '#78d6a2', 'belly': '#f0fff4', 'gill': '#1f6a48', 'spot': '#ff86ac',
                        'spot_d': '#e0608a', 'fin': '#c08af0', 'fin_tip': '#f2dcff', 'fin_ray': '#8a5ac0', 'fin_base': '#a070d8',
                        'pattern': 'saddle'},
    # rare: an indigo night fish whose freckles glow, with golden fins and kazoo
    'kazoo_fish_midnight': {'back': '#1c1852', 'sheen': '#6a64d0', 'flank': '#3a3492', 'belly': '#8a86d8', 'gill': '#100c38', 'spot': '#5ff8ff',
                            'spot_d': '#2ab8c8', 'fin': '#f2c040', 'fin_tip': '#fff2a8', 'fin_ray': '#b8862a', 'fin_base': '#d09a30',
                            'kazoo': '#f2c040', 'kazoo_d': '#a8782a', 'kazoo_l': '#fff0a0', 'pattern': 'glow'},
}

KAZOO_BODY = [
    '.....BBBBBBB......',
    '...BBhhhhhBBBBB...',
    '..BhhBBBBBBBBBBBB.',
    '.BhBBBBgBBBBBBBBBB',
    '.bbbbbbbgbbbbbbbbb',
    'mbbbbbbbgbbbbbbbbb',
    'mbbbbbbbgbbbbbbbbb',
    '.bbbbbbbgbbbbbbbbb',
    '.wwwwwwgwwwwwwwwww',
    '..wwwwwwwwwwwwwwww',
    '...wwwwwwwwwwwwww.',
    '.....wwwwwwwwww...',
    '.......wwwwww.....',
]
KAZOO_STEM = [
    'BB....',
    'BBBBBB',
    'bbbbbb',
    'bbbbbb',
    'bbbbbb',
    'wwwwww',
    'ww....',
]
KAZOO_PATTERNS = {
    'freckles': [(1, 12), (2, 9), (3, 14), (4, 11), (5, 15), (6, 12), (2, 16), (7, 9)],
    'spots': [(y, x) for (y0, x0) in ((2, 10), (5, 13), (2, 15), (6, 9)) for (y, x) in ((y0, x0), (y0, x0 + 1), (y0 + 1, x0), (y0 + 1, x0 + 1))],
    'bands': [(y, x) for x in (9, 10, 14, 15) for y in range(0, 9)],
    'saddle': [(y, x) for x in range(7, 15) for y in range(0, 3 if 8 <= x <= 13 else 2)],
}
KAZOO_TUBE = ['KKKKKKK', 'kkkkkkk', 'kkkkkkk', 'DDDDDDD']
KAZOO_MOUTH = ['.kk.', 'kHHk', 'kHHk', '.kk.']
KAZOO_CAP = ['.C.', 'cCc']
KAZOO_TUFT = ['.mMm.', 'mmmMm', 'dmmmd']
KAZOO_SPROUT = [
    '..Y..',
    '.yYy.',
    '..s..',
    'LLs..',
    '.Lsl.',
    '..sll',
]


def _fins_kazoo():
    tail = shape_fin(9, 15, [(0, 5), (2, 4), (5, 0.6), (7.4, -0.2), (8.9, 0.8), (9, 4), (8.3, 7.5), (9, 11), (8.9, 14.2), (7.4, 15.2), (5, 14.4),
                             (2, 11), (0, 10)],
                     [((0, 7.5), (7.2, 0.4)), ((0, 7.5), (8.6, 4.0)), ((0, 7.5), (8.6, 11.0)), ((0, 7.5), (7.2, 14.6))], base_cols=2)
    dorsal = shape_fin(9, 6, [(0, 6), (0.4, 3), (1.8, 0.2), (3.2, 0.4), (5.6, 2.4), (8.6, 4.2), (9, 6)],
                       [((1, 6), (2, 0.8)), ((3.5, 6), (4.6, 2.2)), ((6, 6), (7.2, 3.8))])
    dorsal[-1] = 'd' * len(dorsal[-1])
    anal = ['ddddd', 'frfrf', '.trt.']
    pectoral = ['ddf.', 'drft', 'dfrf', '.rft', '..t.']
    pelvic = ['dd', 'rf', 't.']
    return tail, dorsal, anal, pectoral, pelvic


def _kazoo_colour(name, spr, pal, expr):
    if name in ('tail', 'dorsal', 'anal', 'pectoral', 'pelvic'):
        return paint_map(spr, pal, FIN_KEYS, rim=0)
    if name == 'tube':
        return paint_map(spr, pal, {'K': 'kazoo_l', 'k': 'kazoo', 'D': 'kazoo_d'}, rim=0)
    if name == 'mouthring':
        return paint_map(spr, pal, {'k': 'kazoo_d', 'H': 'hole'}, rim=0)
    if name == 'cap':
        return paint_map(spr, pal, {'C': 'cap', 'c': 'cap_d'}, rim=0)
    if name == 'tuft':
        return paint_map(spr, pal, {'m': 'moss', 'M': 'moss_l', 'd': 'moss_d'}, rim=0.18)
    if name == 'sprout':
        return paint_map(spr, pal, {'Y': 'bud_l', 'y': 'bud', 's': 'stem', 'L': 'leaf', 'l': 'leaf_d'}, rim=0, glow_keys='Yy')
    if name in ('eye_l', 'eye_r'):
        rows = eye_rows(expr, 'googly4' if name == 'eye_l' else 'googly3')
        out, glow = paint_map(Sprite(rows), pal, {'w': 'eye_w', 'p': 'eye_p', 'l': 'back'}, rim=0)
        return out, glow
    if name == 'eye_rim':
        return paint_map(spr, pal, {'e': 'eye_rim'}, rim=0)
    pattern = pal.get('pattern', 'freckles')
    if name == 'body':
        pts = KAZOO_PATTERNS['freckles' if pattern == 'glow' else pattern]
    elif pattern in ('freckles', 'glow'):
        pts = [(2, 2), (3, 4)]
    elif pattern == 'bands':
        pts = [(y, x) for x in (1, 2) for y in range(7)]
    else:
        pts = []
    overlay = [(y, x, 'spot', pattern == 'glow') for (y, x) in pts]
    keys = {'B': 'back', 'h': 'sheen', 'b': 'flank', 'w': 'belly', 'g': 'gill', 'm': 'kazoo_d'}
    return paint_map(spr, pal, keys, scales='b', scale_phase=1 if name == 'stem' else 0, overlay=overlay, overlay_on='Bbhw')


def kazoo_fish():
    """Kazoo Fish: a chubby little reef fish with a kazoo for a snout, googly eyes that never agree, a tuft
    of moss with a glowing sprout on its head and fins like living coral."""
    m = SpriteModel('kazoo_fish', (128, 64), KAZOO_VARIANTS, _kazoo_colour, ['blink', 'hurt', 'dead'], palette=KAZOO_PAL)
    tail, dorsal, anal, pectoral, pelvic = _fins_kazoo()
    for n, rows in (('body', KAZOO_BODY), ('stem', KAZOO_STEM), ('tail', tail), ('dorsal', dorsal), ('anal', anal), ('pectoral', pectoral),
                    ('pelvic', pelvic), ('tube', KAZOO_TUBE), ('mouthring', KAZOO_MOUTH), ('cap', KAZOO_CAP), ('tuft', KAZOO_TUFT),
                    ('sprout', KAZOO_SPROUT), ('eye_l', ['eeee'] * 4), ('eye_r', ['eee'] * 3), ('eye_rim', ['e'])):
        m.sprite(n, Sprite(rows))
    # the body: 18 long (its snout at z = -9), 13 tall, 6 thick at the shoulders
    body = m.part('body', pivot=(0, 17.5, 0))
    prof = [1.2, 1.8, 2.3, 2.7, 3.0, 3.0, 3.0, 3.0, 3.0, 2.9, 2.8, 2.6, 2.4, 2.2, 2.0, 1.9, 1.8, 1.7]
    m.extrude(body, 'body', (-9, -6.5), lambda c: prof[c])
    snout = body.part('kazoo', pivot=(0, -0.5, -8))
    m.extrude(snout, 'tube', (-7, -2), lambda c: 2.0, layers=((1.0, 0.5), (0.5, 1.0)))
    m.plane(snout, 'mouthring', (-2, -2), axis='z')
    snout.cubes[-1].origin = (-2, -2, -7.02)
    m.extrude(snout, 'cap', (-5, -4), lambda c: 1.0, layers=((1.0, 1.0),))
    for side, sx, nm, size in (('left', 1, 'eye_l', 4), ('right', -1, 'eye_r', 3)):
        e = body.part(f'{side}_eye', pivot=(2.9 * sx, -1.8, -4.5))
        x0 = 0 if sx > 0 else -1
        m.block(e, (x0, -size / 2, -size / 2), (1, size, size), nm, {'east' if sx > 0 else 'west': nm, 'north': 'eye_rim', 'up': 'eye_rim',
                                                                      'down': 'eye_rim', 'south': 'eye_rim'})
    tuft = body.part('tuft', pivot=(0, -6.2, -2.5))
    m.extrude(tuft, 'tuft', (-2.5, -2.2), lambda c: 1.6 if 0 < c < 4 else 1.0, layers=((1.0, 0.6), (0.67, 1.0)))
    sprout = tuft.part('tuft_sprout', pivot=(0, -2.3, 0.5), rot=(0, 0.6, 0))
    m.plane(sprout, 'sprout', (-2.5, -6))
    sprout2 = tuft.part('tuft_sprout2', pivot=(0, -2.3, 0.5), rot=(0, 0.6 + math.pi / 2, 0))
    m.plane(sprout2, 'sprout', (-2.5, -6))
    fin_d = body.part('dorsal', pivot=(0, -5.5, 1))
    m.plane(fin_d, 'dorsal', (0, -5))
    fin_a = body.part('anal_fin', pivot=(0, 4.5, 4))
    m.plane(fin_a, 'anal', (0, 0))
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(2.6 * sx, 1.5, -2.5), rot=(0.2, 0.3 * sx, 0.6 * sx))
        m.plane(pf, 'pectoral', (0, 0 if sx > 0 else -4), axis='y')
        pel = body.part(f'{side}_pelvic', pivot=(1.2 * sx, 5.0, -1.5), rot=(0.35, 0, 0.3 * sx))
        m.plane(pel, 'pelvic', (0, 0))
    stem = body.part('tail_stem', pivot=(0, 0, 8.5))
    m.extrude(stem, 'stem', (0, -3.5), lambda c: 1.6 - c * 0.12, layers=((1.0, 0.65), (0.6, 1.0)))
    tail = stem.part('tail', pivot=(0, 0, 5.5))
    m.plane(tail, 'tail', (0, -7.5))
    return m


# ============================================================================ the Tubafish

TUBA_PAL = {
    'back': '#4f74c2', 'sheen': '#c4dcff', 'body': '#78a5e3', 'belly': '#ffe6ef', 'belly_d': '#f2c4d6', 'gill': '#3d5aa8',
    'spot': '#5ff8ff', 'lip': '#f59ab8', 'lip_d': '#c96a8c', 'throat': '#2a1030',
    'fin': '#ffb0c4', 'fin_tip': '#fff0f4', 'fin_ray': '#e07a9a', 'fin_base': '#ec90ae',
    'coral': '#f37d84', 'coral_l': '#ffb4b4', 'coral_d': '#c44a5a',
    'brass': '#f2d27a', 'brass_l': '#fff4c0', 'brass_d': '#b8923a', 'brass_dd': '#7a5a20', 'bell_in': '#1a1030',
    'tent': '#ff9fd0', 'tent2': '#7ff0ff', 'tent_tip': '#ffffff',
    'eye_w': '#ffffff', 'iris': '#3ff5e6', 'iris_d': '#15a89f', 'eye_p': '#1a1420', 'eye_rim': '#5d86cc', 'lid': '#5d86cc',
}
TUBA_VARIANTS = {
    # periwinkle, its freckles glowing cyan
    'tubafish': {},
    # lilac with golden freckles and a silver tuba
    'tubafish_lilac': {'back': '#7a4ab8', 'sheen': '#ecd4ff', 'body': '#b48ae8', 'belly': '#fff0f8', 'belly_d': '#ecc8e0', 'gill': '#5a2a98',
                       'spot': '#ffe46a', 'fin': '#f8c0ff', 'fin_tip': '#fff4ff', 'fin_ray': '#c88ae0', 'fin_base': '#e0a8f0',
                       'brass': '#d8e0ea', 'brass_l': '#ffffff', 'brass_d': '#8a96a8', 'brass_dd': '#4a5468', 'lid': '#8a5ac8',
                       'eye_rim': '#8a5ac8', 'iris': '#ffd84a', 'iris_d': '#c89a1a'},
    # sea-foam green with rose freckles
    'tubafish_seafoam': {'back': '#2f9a7a', 'sheen': '#d0fff0', 'body': '#62d0a8', 'belly': '#fffbe8', 'belly_d': '#efe4c4', 'gill': '#1f7058',
                         'spot': '#ff7aa8', 'fin': '#ffd0a0', 'fin_tip': '#fff8ea', 'fin_ray': '#e8a060', 'fin_base': '#f2b884',
                         'lid': '#2f9a7a', 'eye_rim': '#2f9a7a', 'iris': '#ff8ab4', 'iris_d': '#c8507a'},
    # rare: sunrise peach with sky-blue freckles and a copper tuba
    'tubafish_sunrise': {'back': '#e0704a', 'sheen': '#fff0d0', 'body': '#ffa278', 'belly': '#fff6e8', 'belly_d': '#f6dcc4', 'gill': '#b04a2a',
                         'spot': '#7ad0ff', 'fin': '#ffd27a', 'fin_tip': '#fff6d0', 'fin_ray': '#e8a040', 'fin_base': '#f2b860',
                         'brass': '#e8946a', 'brass_l': '#ffd0b0', 'brass_d': '#a85a34', 'brass_dd': '#6a3418', 'lid': '#e0704a',
                         'eye_rim': '#e0704a', 'iris': '#7ad0ff', 'iris_d': '#3a8ac8'},
}


def _tuba_body():
    """A round puffer, 22 x 20: its back, body and belly, a sheen arc, gill slit and freckles."""
    w, h = 22, 20
    rows = []
    for y in range(h):
        row = ''
        for x in range(w):
            u = (x + 0.5 - 11.5) / 11.0
            v = (y + 0.5 - 10.0) / 10.0
            # a little fuller in front, tapering towards the tail
            k = abs(u) ** 2.2 + abs(v) ** 2.1 + (0.22 * max(0.0, u) ** 3)
            if k > 1.0:
                row += '.'
                continue
            t = (y - 0.0) / h
            ch = 'B' if v < -0.6 else 'w' if v > 0.4 else 'b'
            # the sheen: a lit arc just inside the upper front edge
            if -0.95 < u < 0.15 and -0.86 < v < -0.45 and 0.62 < k < 0.82:
                ch = 'h'
            row += ch
        rows.append(row)
    g = [list(r) for r in rows]
    for (y, x) in ((6, 9), (7, 10), (8, 10), (9, 10), (10, 10), (11, 9)):
        if g[y][x] in 'bBw':
            g[y][x] = 'g'
    for (y, x) in ((4, 13), (6, 16), (5, 19), (8, 14), (10, 17), (12, 13), (9, 19), (3, 9), (12, 6), (7, 3)):
        if g[y][x] in 'bBh':
            g[y][x] = 's'
    for y in (11, 12):
        g[y][0] = 'm' if g[y][0] != '.' else g[y][0]
    return [''.join(r) for r in g]


TUBA_TUBE = [  # the tuba's tubing along its back, rising at the rear into the bell, with two valves
    '.....kKKk',
    '.....kKKk',
    '.vv..kKKk',
    '.VV..kKKk',
    'kKKKKKKKk',
    'kKKKKKKk.',
    '.kkkkkk..',
]
TUBA_BELL = ['bBBBBBBb', '.bBBBBb.', '..bBBb..']
TUBA_RING = ['..BBBB..', '.BbbbbB.', 'BbnnnnbB', 'BbnnnnbB', 'BbnnnnbB', 'BbnnnnbB', '.BbbbbB.', '..BBBB..']
TUBA_LIPS = ['.LL.', 'LllL', 'LooL', 'LllL', '.LL.']
TUBA_SPIKE = ['c.c', 'cC.', '.C.', '.C.']
TUBA_TENTACLE = ['T', 'a', 'A', 'a', 'A']


def _tuba_fins():
    tail = shape_fin(8, 13, [(0, 4), (3, 2), (6.2, -0.1), (8, 1.2), (7.4, 6.5), (8, 11.8), (6.2, 13.1), (3, 11), (0, 9)],
                     [((0, 6.5), (6.4, 1.0)), ((0, 6.5), (7.6, 6.5)), ((0, 6.5), (6.4, 12.0))], base_cols=2)
    pect = shape_fin(6, 7, [(0, 1), (2, 0), (5, 0.6), (6.2, 3.5), (5, 6.4), (2, 7), (0, 6)],
                     [((0, 3.5), (5.6, 1.0)), ((0, 3.5), (6, 3.5)), ((0, 3.5), (5.6, 6))], base_cols=1)
    dorsal = shape_fin(6, 5, [(0, 5), (1, 1.5), (2.6, 0), (4.4, 1.6), (6, 5)], [((1.5, 5), (2.5, 1)), ((3.8, 5), (4.4, 2.2))])
    dorsal[-1] = 'd' * len(dorsal[-1])
    anal = ['dddd', 'frrf', '.tt.']
    return tail, pect, dorsal, anal


def _tuba_eye(expr):
    sets = {'': ['.www.', 'wiiiw', 'wipiw', 'wiiiw', '.www.'], 'blink': ['.lll.', 'lllll', 'lllll', 'wwwww', '.www.'],
            'happy': ['.....', '.lll.', 'l...l', '.....', '.....'], 'angry': ['ll...', 'wllll', 'wipiw', 'wiiiw', '.www.'],
            'hurt': ['l...l', '.l.l.', '..l..', '.l.l.', 'l...l'], 'dead': ['x...x', '.x.x.', '..x..', '.x.x.', 'x...x']}
    return sets.get(expr, sets[''])


def _tuba_colour(name, spr, pal, expr):
    if name in ('tail', 'pect', 'dorsal', 'anal'):
        return paint_map(spr, pal, FIN_KEYS, rim=0)
    if name == 'tube':
        return paint_map(spr, pal, {'b': 'brass_d', 'B': 'brass', 'k': 'brass_d', 'K': 'brass', 'v': 'brass_l', 'V': 'brass_dd'}, rim=0.25)
    if name == 'bell':
        return paint_map(spr, pal, {'b': 'brass_d', 'B': 'brass'}, rim=0.2)
    if name == 'ring':
        return paint_map(spr, pal, {'B': 'brass_d', 'b': 'brass_l', 'n': 'bell_in'}, rim=0)
    if name == 'lips':
        return paint_map(spr, pal, {'L': 'lip', 'l': 'lip_d', 'o': 'throat'}, rim=0)
    if name == 'spike':
        return paint_map(spr, pal, {'c': 'coral', 'C': 'coral_l'}, rim=0)
    if name == 'tent':
        return paint_map(spr, pal, {'T': 'tent_tip', 'a': 'tent', 'A': 'tent2'}, rim=0, glow_keys='TA')
    if name in ('eye_l', 'eye_r'):
        out, glow = paint_map(Sprite(_tuba_eye(expr)), pal, {'w': 'eye_w', 'i': 'iris', 'p': 'eye_p', 'l': 'lid', 'x': 'eye_p'}, rim=0,
                              glow_keys='i')
        return out, glow
    if name == 'eye_rim':
        return paint_map(spr, pal, {'e': 'eye_rim'}, rim=0)
    keys = {'B': 'back', 'h': 'sheen', 'b': 'body', 'w': 'belly', 'g': 'gill', 's': 'spot', 'm': 'lip_d'}
    return paint_map(spr, pal, keys, scales='Bb', glow_keys='s')


def tubafish():
    """Tubafish: a big round puffer with glowing freckles, pouting lips and bulging eyes; a little tuba grows out
    of its back, its bell crowned with swaying anemone tentacles; coral spikes lie flat until it puffs up."""
    m = SpriteModel('tubafish', (256, 256), TUBA_VARIANTS, _tuba_colour, ['blink', 'happy', 'angry', 'hurt', 'dead'], palette=TUBA_PAL)
    tail, pect, dorsal, anal = _tuba_fins()
    for n, rows in (('body', _tuba_body()), ('tube', TUBA_TUBE), ('bell', TUBA_BELL), ('ring', TUBA_RING), ('lips', TUBA_LIPS), ('spike', TUBA_SPIKE),
                    ('tent', TUBA_TENTACLE), ('tail', tail), ('pect', pect), ('dorsal', dorsal), ('anal', anal),
                    ('eye_l', ['eeeee'] * 5), ('eye_r', ['eeeee'] * 5), ('eye_rim', ['e'])):
        m.sprite(n, Sprite(rows))
    body = m.part('body', pivot=(0, 13, 0))
    # round in every direction: three nested layers per column
    m.extrude(body, 'body', (-11, -10), lambda c: 9.5 * math.sqrt(max(0.05, 1 - ((c + 0.5 - 11.2) / 11.6) ** 2)) + 0.6,
              layers=((1.0, 0.36), (0.9, 0.62), (0.74, 0.84), (0.5, 1.0)))
    lips = body.part('mouth', pivot=(0, 1.5, -11))
    m.extrude(lips, 'lips', (-2, -2.5), lambda c: 2.0 if c else 1.5, layers=((1.0, 0.5), (0.6, 1.0)))
    for side, sx in (('left', 1), ('right', -1)):
        e = body.part(f'{side}_eye', pivot=(7.9 * sx, -3.0, -6.0), rot=(0, -0.35 * sx, 0))
        m.block(e, (0 if sx > 0 else -2, -2.5, -2.5), (2, 5, 5), f'eye_{side[0]}', {('east' if sx > 0 else 'west'): f'eye_{side[0]}',
                                                                                   'north': 'eye_rim', 'up': 'eye_rim', 'down': 'eye_rim',
                                                                                   'south': 'eye_rim'})
    # the tuba: tubing lying along its back (its foot sunk into the body), rising at the rear into the bell
    tuba = body.part('tuba', pivot=(0, -8.5, 1.0))
    m.extrude(tuba, 'tube', (-5, -6), lambda c: 1.5, layers=((1.0, 0.67), (0.6, 1.0)))
    bell = tuba.part('tuba_bell', pivot=(0, -6, 2.0))
    m.extrude(bell, 'bell', (-4, -3), lambda c: [1.9, 2.8, 3.4, 3.9, 3.9, 3.4, 2.8, 1.9][c], layers=((1.0, 0.75), (0.67, 1.0)))
    m.plane(bell, 'ring', (-4, -4), axis='y')
    bell.cubes[-1].origin = (-4, -3.05, -4)
    for i in range(6):
        a = i * math.pi / 3 + 0.3
        t = bell.part(f'tentacle_{i}', pivot=(math.cos(a) * 3.3, -3, math.sin(a) * 3.3), rot=(-math.sin(a) * 0.45, a, math.cos(a) * 0.45))
        m.plane(t, 'tent', (-0.5, -5))
    # the coral spikes: folded flat against the body until it puffs up
    spots = [(-8.0, -5.5, -6.0, 0.0), (-9.5, 0.0, 0.0, -1.57), (-8.0, 5.5, 4.0, 3.14), (0.0, -9.5, -2.0, 0.0), (6.0, -7.5, 4.0, 0.0),
             (9.0, 0.0, 2.0, 1.57), (6.0, 7.5, 0.0, 3.14), (-3.0, 9.3, -4.0, 3.14), (3.0, -9.0, 8.0, 0.0), (8.0, 4.0, -6.0, 2.2),
             (-6.0, -6.5, 6.0, 0.0), (-2.0, 9.4, 6.0, 3.14), (9.0, -3.0, -4.0, 0.8)]
    for k, (sxx, syy, szz, roll) in enumerate(spots):
        sp = body.part(f'spike_{k}', pivot=(sxx, syy, szz), rot=(0.9, 0, roll))
        m.plane(sp, 'spike', (-1.5, -4))
        sp2 = sp.part(f'spike_{k}_x', rot=(0, math.pi / 2, 0))
        m.plane(sp2, 'spike', (-1.5, -4))
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(8.0 * sx, 2.0, -1.0), rot=(0, 0.5 * sx, 0))
        m.plane(pf, 'pect', (0, -3.5))
    tailp = body.part('tail', pivot=(0, 0.5, 10.5))
    m.plane(tailp, 'tail', (0, -6.5))
    dors = body.part('dorsal', pivot=(0, -7.0, 6.5))
    m.plane(dors, 'dorsal', (0, -4))
    an = body.part('anal_fin', pivot=(0, 7.5, 6.0))
    m.plane(an, 'anal', (0, -0.5))
    return m


# ============================================================================ the Fanfare Eel

EEL_PAL = {
    'back': '#0f2430', 'hide': '#1a3a4a', 'belly': '#2f6a76', 'bone': '#e3ddcc', 'bone_d': '#b3ab96', 'glow': '#3ff5e6', 'glow_l': '#c8fffb',
    'brass': '#f2d27a', 'brass_l': '#fff0b0', 'brass_d': '#b8923a', 'brass_dd': '#7a5a20', 'throat': '#0a1a22', 'tooth': '#fff8e0',
    'fin': '#3fd8d0', 'fin_tip': '#c8fffb', 'fin_ray': '#1f9a96', 'fin_base': '#2ab8b4',
    'eye_w': '#04080c', 'iris': '#3ff5e6', 'eye_p': '#04080c', 'lid': '#24485a',
}
EEL_VARIANTS = {
    # the abyss eel: teal-black hide, cyan glow, a golden bell
    'fanfare_eel': {},
    # violet hide, magenta glow, a silver bell
    'fanfare_eel_violet': {'back': '#1c0f2e', 'hide': '#30184a', 'belly': '#5a3a7a', 'glow': '#ff6ad8', 'glow_l': '#ffd0f4', 'fin': '#e05ac8',
                           'fin_tip': '#ffd0f4', 'fin_ray': '#9a2a8a', 'fin_base': '#c048b0', 'brass': '#d8e0ea', 'brass_l': '#ffffff',
                           'brass_d': '#8a96a8', 'brass_dd': '#4a5468', 'iris': '#ff6ad8', 'lid': '#30184a'},
    # rare: an ember eel, charcoal hide and orange glow, a copper bell
    'fanfare_eel_ember': {'back': '#1e120c', 'hide': '#33201a', 'belly': '#5a3a2a', 'glow': '#ffa040', 'glow_l': '#ffe0b0', 'fin': '#ff8a3a',
                          'fin_tip': '#ffe0b0', 'fin_ray': '#b04a1a', 'fin_base': '#d8682a', 'brass': '#e8946a', 'brass_l': '#ffd0b0',
                          'brass_d': '#a85a34', 'brass_dd': '#6a3418', 'iris': '#ffa040', 'lid': '#33201a'},
}
EEL_SEGMENTS = [(0, 14), (14, 26), (26, 37), (37, 47), (47, 55), (55, 62)]  # head + five body segments (sprite columns)


def _eel_body():
    w, h = 62, 10

    def half(c):
        if c < 14:
            return [3.0, 3.6, 4.0, 4.3, 4.6, 4.8, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0][c]
        return 5.0 - (c - 14) / (w - 14) * 3.2
    g = [['.'] * w for _ in range(h)]
    for c in range(w):
        hh = half(c)
        t, b = int(round(4.5 - hh + 0.5)), int(round(4.5 + hh - 0.5))
        for y in range(max(0, t), min(h - 1, b) + 1):
            v = (y - t) / max(1, b - t)
            ch = 'B' if v < 0.28 else 'b' if v < 0.64 else 'w'
            if c >= 14:
                if (c - 14) % 4 == 1 and 0.12 < v < 0.5:
                    ch = 'o'  # a bone rib showing through the hide
                if 0.55 <= v < 0.68 and c % 2 == 0:
                    ch = 'l'  # the glowing lateral line
            else:
                if v < 0.2 and 4 <= c <= 11 and c % 3 != 0:
                    ch = 'o'  # bone plates over the skull
                if c in (10, 12) and 0.35 <= v <= 0.7:
                    ch = 'g'  # glowing gill slits
            g[y][c] = ch
    for (y, x) in ((0, 0), (9, 0)):
        g[y][x] = '.'
    # the mouth where the bell grows out
    for y in (4, 5):
        g[y][0] = 'm'
    return [''.join(r) for r in g]


def _eel_bell():
    """The trumpet bell, side view: narrow where it leaves the head (right), flaring forwards (left)."""
    w, h = 7, 13
    rows = []
    for y in range(h):
        row = ''
        for c in range(w):
            r = 1.6 + 4.9 * (1 - c / (w - 1)) ** 2
            v = abs(y + 0.5 - 6.5)
            row += ('.' if v > r else 'R' if v > r - 1.0 else 'B')
        rows.append(row)
    return rows


def _eel_ring():
    """The bell's mouth from the front: a rolled golden rim, a ring of glowing fin-roots, teeth and the dark throat."""
    rows = []
    for y in range(13):
        row = ''
        for x in range(13):
            d = math.hypot(x - 6, y - 6) / 6.5
            row += ('.' if d > 1.0 else 'R' if d > 0.84 else 'r' if d > 0.72 else 'G' if d > 0.62 else
                    't' if d > 0.48 and (x + y) % 2 == 0 else 'T' if d < 0.22 else 'D')
        rows.append(row)
    return rows


def _eel_eye(expr):
    sets = {'': ['iii', 'ipi', 'iii'], 'blink': ['...', 'lll', '...'], 'angry': ['ll.', 'ipl', 'iii'], 'hurt': ['l.l', '.l.', 'l.l'],
            'dead': ['x.x', '.x.', 'x.x']}
    return sets.get(expr, sets[''])


def _eel_colour(name, spr, pal, expr):
    if name in ('frill', 'tail', 'bellfin', 'pect', 'gillfrill'):
        return paint_map(spr, pal, FIN_KEYS, rim=0, glow_keys='ftrd')
    if name == 'bell':
        return paint_map(spr, pal, {'R': 'brass_d', 'B': 'brass'}, rim=0.2)
    if name == 'ring':
        return paint_map(spr, pal, {'R': 'brass_l', 'r': 'brass', 'G': 'glow', 't': 'tooth', 'D': 'throat', 'T': 'back'}, rim=0, glow_keys='G')
    if name == 'whisker':
        return paint_map(spr, pal, {'o': 'bone', 'O': 'bone_d', 'g': 'glow'}, rim=0, glow_keys='g')
    out, glow = paint_map(spr, pal, {'B': 'back', 'b': 'hide', 'w': 'belly', 'o': 'bone_d', 'l': 'glow', 'g': 'glow', 'm': 'brass_d'},
                          rim=0.25, scales='bw', glow_keys='lg')
    if name == 'body':
        # the slit-pupilled glowing eye
        for j, row in enumerate(_eel_eye(expr)):
            for i, ch in enumerate(row):
                y, x = 2 + j, 3 + i
                if ch == 'i':
                    out[y, x], glow[y, x] = rgb(pal['iris']), True
                elif ch in 'px':
                    out[y, x], glow[y, x] = rgb(pal['eye_p']), False
                elif ch == 'l':
                    out[y, x], glow[y, x] = rgb(pal['lid']), False
    return out, glow


def fanfare_eel():
    """Fanfare Eel: a long sculk eel with bone ribs showing through its dark hide, a glowing lateral line and
    frill, and for a mouth a golden trumpet bell ringed with glowing fins."""
    m = SpriteModel('fanfare_eel', (256, 128), EEL_VARIANTS, _eel_colour, ['blink', 'angry', 'hurt', 'dead'], palette=EEL_PAL)
    frill = shape_fin(10, 4, [(0, 4), (1, 1.2), (3, 0), (7, 0.4), (10, 2.4), (10, 4)], [((2, 4), (3, 0.6)), ((5, 4), (6, 0.6)), ((8, 4), (8.6, 1.6))])
    frill[-1] = 'd' * len(frill[-1])
    tail = shape_fin(8, 11, [(0, 3.5), (3, 1.5), (6.5, -0.2), (8.2, 1.0), (7, 5.5), (8.2, 10), (6.5, 11.2), (3, 9.5), (0, 7.5)],
                     [((0, 5.5), (6.5, 0.6)), ((0, 5.5), (7.2, 5.5)), ((0, 5.5), (6.5, 10.4))], base_cols=1)
    bellfin = shape_fin(4, 5, [(0, 0), (2.5, 0.2), (4.2, 2.5), (2.5, 4.8), (0, 5)], [((0, 2.5), (3.8, 2.5))], base_cols=1)
    pect = ['ddf', 'drt', 'frt', '.t.']
    gillfrill = ['dft', 'drt', 'dft', 'drt']
    for n, rows in (('body', _eel_body()), ('bell', _eel_bell()), ('ring', _eel_ring()), ('frill', frill), ('tail', tail),
                    ('bellfin', bellfin), ('pect', pect), ('gillfrill', gillfrill), ('whisker', ['oOoOg'])):
        m.sprite(n, Sprite(rows))
    thick = lambda c: 3.0 if c < 14 else 3.0 - (c - 14) / 48 * 1.8  # noqa: E731
    head = m.part('head', pivot=(0, 19.5, -18))
    m.extrude(head, 'body', (0, -4.5), thick, cols=range(*EEL_SEGMENTS[0]))
    pipe = head.part('pipe', pivot=(0, 0.0, 0))
    m.extrude(pipe, 'bell', (-7, -6.5), lambda c: 1.6 + 4.9 * (1 - c / 6) ** 2, layers=((1.0, 0.45), (0.8, 0.8), (0.5, 1.0)))
    bell = pipe.part('bell', pivot=(0, 0, -7))
    m.plane(bell, 'ring', (-6.5, -6.5), axis='z')
    bell.cubes[-1].origin = (-6.5, -6.5, -0.05)
    for i, rz in enumerate((0.0, math.pi / 2, math.pi, -math.pi / 2)):
        bf = bell.part(f'bell_fin_{i}', pivot=(0, 0, 0.5), rot=(0, 0, rz))
        sub = bf.part(f'bell_fin_{i}_blade', pivot=(0, -6.0, 0), rot=(-0.5, 0, 0))
        m.plane(sub, 'bellfin', (0, -5))
    for side, sx in (('left', 1), ('right', -1)):
        wh = pipe.part(f'{side}_whisker', pivot=(2.2 * sx, 2.5, -3.5), rot=(0.55, 0.65 * sx, 0))
        m.extrude(wh, 'whisker', (0, 0), lambda c: 0.5, layers=((1.0, 1.0),), min_half=0.5)
        pf = head.part(f'{side}_fin', pivot=(2.6 * sx, 2.5, 8), rot=(0, 0.5 * sx, 0.5 * sx))
        m.plane(pf, 'pect', (0, -1))
        gf = head.part(f'{side}_gill', pivot=(2.9 * sx, -1.5, 10.5), rot=(0, 0.45 * sx, 0))
        m.plane(gf, 'gillfrill', (0, -2))
    crest = head.part('crest', pivot=(0, -5.0, 4))
    m.plane(crest, 'frill', (0, -3))
    prev, start = head, EEL_SEGMENTS[0][1]
    for i, (a, b) in enumerate(EEL_SEGMENTS[1:]):
        seg = prev.part(f'segment_{i}', pivot=(0, 0, start - (EEL_SEGMENTS[i][0] if i else 0)))
        m.extrude(seg, 'body', (-a, -4.5), thick, cols=range(a, b))
        fr = seg.part(f'frill_{i}', pivot=(0, -4.6 + (a - 14) * 3.2 / 48, 0.5))
        m.plane(fr, 'frill', (0, -3))
        if i == len(EEL_SEGMENTS) - 2:
            tf = seg.part('tail_fin', pivot=(0, 0, b - a))
            m.plane(tf, 'tail', (0, -5.5))
        prev, start = seg, b
    return m


# ============================================================================ the Sculk Fish

SCULK_PAL = {
    'back': '#06161d', 'flank': '#0d2a35', 'belly': '#184652', 'vein': '#0f5a62', 'glow': '#3ff5e6', 'glow_l': '#d6fffb',
    'tooth': '#e3ddcc', 'tooth_d': '#a8a08a', 'gum': '#3a1220', 'jaw': '#0b2430',
    'fin': '#0b2532', 'fin_ray': '#123a48', 'fin_tip': '#29dfeb', 'fin_base': '#081c24',
    'eye': '#9ffcff', 'eye_p': '#02080a', 'lid': '#0d2a35', 'stalk': '#123a48',
}
SCULK_VARIANTS = {
    # sculk-dark with a cyan glow, like the deep dark's sensors
    'sculk_fish': {},
    # bone-pale glow, like a shrieker's
    'sculk_fish_pale': {'glow': '#f4f0e5', 'glow_l': '#ffffff', 'fin_tip': '#e3ddcc', 'eye': '#ffffff', 'vein': '#4a5a58', 'belly': '#2a3e44'},
    # a blue-violet abyss glow
    'sculk_fish_deep': {'back': '#08081e', 'flank': '#141a3e', 'belly': '#25305e', 'vein': '#3a3a8a', 'glow': '#8a8cff', 'glow_l': '#e0e0ff',
                        'fin': '#10143a', 'fin_ray': '#1e2458', 'fin_tip': '#8a8cff', 'fin_base': '#0a0c26', 'eye': '#c0c4ff', 'lid': '#141a3e',
                        'jaw': '#10143a', 'stalk': '#1e2458'},
}
SCULK_BODY = [
    '.....BBBBBBB........',
    '...BBBBBBBBBBBB.....',
    '..BBBBBBBBBBBBBBBB..',
    '.BBBBBBBBBBBBBBBBBBB',
    '.bbbbbgbbbbbbbbbbbbb',
    'tbbbbbgbvbbbbbbbbbbb',
    '.Gbbbbbgbbvbbbbbbbbb',
    '..wwwwwgwwwwvwwwwwww',
    '...wwwwwwwwwwwwwwwww',
    '....wwwwwwwwwwwwwww.',
    '.....wwwwwwwwwwwww..',
    '.......wwwwwwwwww...',
    '.........wwwww......',
]
SCULK_JAW = [
    't.t.t..',
    'jjjjjjj',
    'jjjjjj.',
    '.jjjj..',
]
SCULK_STEM = [
    'BBBB..',
    'bbbbbb',
    'bbbbbb',
    'wwwwww',
    'wwww..',
]
SCULK_TENDRIL = ['.g.', 'g..', '.s.', '..s', '.s.', 's..']
SCULK_LIGHTS = [(5, 12), (5, 15), (6, 18), (4, 9), (6, 14)]


def _sculk_fins():
    tail = shape_fin(9, 13, [(0, 4), (3, 2.4), (6.5, -0.2), (9.2, 0.0), (6.4, 6.5), (9.2, 13.0), (6.5, 13.2), (3, 10.6), (0, 9)],
                     [((0, 6.5), (7.6, 0.6)), ((0, 6.5), (7.6, 12.4)), ((0, 6.5), (5.4, 6.5))], base_cols=2)
    dorsal = ['..t...t..', '.tr..tr.t', 'trf.trftr', 'rfftrffrf', 'ddddddddd']
    anal = ['dddddd', 'rfrfrt', '.tt.t.']
    pect = ['ddt', 'drt', '.rt', '..t']
    return tail, dorsal, anal, pect


def _sculk_colour(name, spr, pal, expr):
    if name in ('tail', 'dorsal', 'anal', 'pect'):
        return paint_map(spr, pal, FIN_KEYS, rim=0, glow_keys='t')
    if name == 'tendril':
        return paint_map(spr, pal, {'g': 'glow_l', 's': 'stalk'}, rim=0, glow_keys='g')
    if name == 'jaw':
        return paint_map(spr, pal, {'t': 'tooth', 'j': 'jaw'}, rim=0.25)
    keys = {'B': 'back', 'b': 'flank', 'w': 'belly', 'g': 'vein', 'v': 'vein', 't': 'tooth', 'G': 'gum'}
    lights = SCULK_LIGHTS if name == 'body' else [(2, 2)]
    out, glow = paint_map(spr, pal, keys, rim=0.3, scales='bw', overlay=[(y, x, 'glow', True) for (y, x) in lights], overlay_on='bwBv')
    if name == 'body':
        eye = {'': ['.EE', 'EpE', 'EE.'], 'blink': ['...', 'lll', '...'], 'angry': ['ll.', 'Epl', 'EE.'], 'hurt': ['l.l', '.l.', 'l.l'],
               'dead': ['x.x', '.x.', 'x.x']}.get(expr if expr != 'happy' else '', None) or ['.EE', 'EpE', 'EE.']
        for j, row in enumerate(eye):
            for i, ch in enumerate(row):
                y, x = 2 + j, 3 + i
                if ch == 'E':
                    out[y, x], glow[y, x] = rgb(pal['eye']), True
                elif ch in 'px':
                    out[y, x], glow[y, x] = rgb(pal['eye_p']), False
                elif ch == 'l':
                    out[y, x], glow[y, x] = rgb(pal['lid']), False
    return out, glow


def sculk_fish():
    """Sculk Fish: a deep-bodied, underbitten little biter of the Sculk Ocean, sculk-dark with glowing eyes,
    lights down its flank, two sensor tendrils on its brow and glowing fin tips. It hunts in schools."""
    m = SpriteModel('sculk_fish', (128, 64), SCULK_VARIANTS, _sculk_colour, ['blink', 'angry', 'hurt', 'dead'], palette=SCULK_PAL)
    tail, dorsal, anal, pect = _sculk_fins()
    for n, rows in (('body', SCULK_BODY), ('jaw', SCULK_JAW), ('stem', SCULK_STEM), ('tendril', SCULK_TENDRIL), ('tail', tail),
                    ('dorsal', dorsal), ('anal', anal), ('pect', pect)):
        m.sprite(n, Sprite(rows))
    body = m.part('body', pivot=(0, 17.0, 0))
    prof = [1.2, 1.8, 2.3, 2.6, 2.8, 3.0, 3.0, 3.0, 3.0, 2.9, 2.8, 2.6, 2.5, 2.3, 2.2, 2.0, 1.9, 1.8, 1.7, 1.6]
    m.extrude(body, 'body', (-10, -6.5), lambda c: prof[c])
    jaw = body.part('jaw', pivot=(0, -0.5, -6))
    m.extrude(jaw, 'jaw', (-5, 0), lambda c: [1.6, 1.8, 2.0, 2.0, 1.8, 1.6, 1.4][c], layers=((1.0, 0.6), (0.6, 1.0)))
    for side, sx in (('left', 1), ('right', -1)):
        t = body.part(f'{side}_tendril', pivot=(1.2 * sx, -5.2, -5), rot=(-0.25, 0.4 * sx, 0.3 * sx))
        m.plane(t, 'tendril', (-1.5, -6))
        pf = body.part(f'{side}_fin', pivot=(2.6 * sx, 1.5, -3), rot=(0, 0.5 * sx, 0.55 * sx))
        m.plane(pf, 'pect', (0, -1))
    fd = body.part('dorsal', pivot=(0, -5.0, 1))
    m.plane(fd, 'dorsal', (0, -4))
    fa = body.part('anal_fin', pivot=(0, 4.5, 3))
    m.plane(fa, 'anal', (0, 0))
    stem = body.part('tail_stem', pivot=(0, 0, 9.5))
    m.extrude(stem, 'stem', (0, -3.5), lambda c: 1.5 - c * 0.1, layers=((1.0, 0.65), (0.6, 1.0)))
    tl = stem.part('tail', pivot=(0, -1.0, 5.5))
    m.plane(tl, 'tail', (0, -6.5))
    return m


# ============================================================================ the Sculk Coral Organ

ORGAN_PAL = {
    'rock': '#0c3b47', 'rock_d': '#082a35', 'rock_l': '#18626b', 'polyp': '#54ecde', 'polyp_d': '#28968f',
    'pipe': '#114d58', 'pipe_l': '#23787e', 'pipe_d': '#082a35', 'rim': '#348f90', 'rim_l': '#7ff7ea', 'hole': '#020a0e', 'lip': '#e3ddcc',
    'lip_d': '#a8a08a', 'vein': '#3ff5e6', 'sac': '#0f3440', 'sac_l': '#1d5a66', 'horn': '#e3ddcc', 'horn_d': '#9e957c', 'horn_l': '#fbf8ee',
    'core': '#5ff8ff', 'stalk': '#123a48', 'tip': '#c8fffb',
}
ORGAN_PIPES = ((-9.5, 16, 3, 0.5), (-5, 22, 4, -1.5), (0, 28, 5, 0.0), (5, 22, 4, -1.5), (9.5, 16, 3, 0.5))  # x, height, width, z


def _organ_pipe(h, w):
    """One organ pipe, side view (w wide): its glowing rim, the body (lit on the left, shaded right) with a glowing
    sculk vein climbing it, the lip over the flue mouth, a cone foot, and a few polyps."""
    rows = []
    for y in range(h):
        if y == 0:
            rows.append('R' * w)
        elif y == 1:
            rows.append('r' * w)
        elif y == h - 1:
            rows.append('.' + 'P' * (w - 2) + '.')
        else:
            row = list('L' + 'P' * (w - 2) + 'D')
            if y == h - 7:
                row = list('M' * w)
            elif 2 < y < h - 8:
                vx = 1 + int(round((w - 3) * (0.5 + 0.5 * math.sin(y * 0.55))))
                row[vx] = 'v'
                if (y * 7) % 11 == 3:
                    row[w - 2 if vx < w - 2 else 1] = 'o'
            rows.append(''.join(row))
    return rows


def _organ_mouth(h, w):
    """The front of a pipe: only the flue mouth's dark slot and its pale lip, the rest see-through."""
    rows = ['.' * w] * h
    rows[h - 7] = 'M' * w
    rows[h - 6] = 'h' * w
    rows[h - 5] = 'h' * w
    rows[h - 4] = '.' + 'l' * (w - 2) + '.'
    return rows


def _organ_base():
    w, h = 24, 8
    rows = []
    for y in range(h):
        row = ''
        for x in range(w):
            u = (x + 0.5 - 12) / 12.0
            top = 7.6 - 6.8 * math.sqrt(max(0.0, 1 - u ** 2)) - 0.7 * math.sin(x * 1.3) ** 2
            if y < top:
                row += '.'
                continue
            ch = 'r' if y > top + 1.5 else 'R'
            if (x * 3 + y * 5) % 13 == 0:
                ch = 'o'
            if (x * 7 + y * 3) % 17 == 0 and y < h - 1:
                ch = 'p'
            row += ch
        rows.append(row)
    return rows


ORGAN_SAC = ['..ssss..', '.sSssSs.', 'sSvssvSs', 'ssvsSvss', 'sssvvsss', 'sSsssSss', '.ssssss.']
ORGAN_HORN = [  # the harpoon horn: a bone horn flaring forwards, a glowing core in its throat
    'HHH.......',
    'hcHHH.....',
    'hcchHHHHH.',
    'hcccchhhhH',
    'hcchhhhhh.',
    'hcHHH.....',
    'HHH.......',
]
ORGAN_RING = ['.RR.', 'RhhR', 'RhhR', '.RR.']
ORGAN_TENDRIL = ['.t.', 't..', '.s.', '..s', '.s.', 's..', '.s.']


def _organ_colour(name, spr, pal, expr):
    if name.startswith('pipe') or name == 'base_pipe':
        return paint_map(spr, pal, {'R': 'rim_l', 'r': 'rim', 'L': 'pipe_l', 'P': 'pipe', 'D': 'pipe_d', 'M': 'lip', 'o': 'polyp', 'v': 'vein'},
                         rim=0.2, glow_keys='Rov')
    if name.startswith('mouth'):
        return paint_map(spr, pal, {'M': 'lip', 'h': 'hole', 'l': 'lip_d'}, rim=0)
    if name == 'ring':
        return paint_map(spr, pal, {'R': 'rim_l', 'h': 'hole'}, rim=0, glow_keys='R')
    if name == 'base':
        return paint_map(spr, pal, {'R': 'rock_l', 'r': 'rock', 'o': 'polyp_d', 'p': 'polyp'}, rim=0.25, glow_keys='p', scales='r')
    if name == 'sac':
        return paint_map(spr, pal, {'s': 'sac', 'S': 'sac_l', 'v': 'vein'}, rim=0.25, glow_keys='v')
    if name == 'horn':
        return paint_map(spr, pal, {'H': 'horn', 'h': 'horn_d', 'c': 'core'}, rim=0.2, glow_keys='c')
    if name == 'tendril':
        return paint_map(spr, pal, {'t': 'tip', 's': 'stalk'}, rim=0, glow_keys='t')
    if name in ('coral', 'fan'):
        img = np.asarray(_block_img('sculk_coral' if name == 'coral' else 'sculk_coral_fan').resize((10, 10), Image.NEAREST))
        a = img[..., :3]
        return a, a.mean(-1) > 170
    return paint_map(spr, pal, {}, rim=0)


def coral_organ():
    """The Sculk Coral Organ: a living reef of sculk coral grown into organ pipes on a lumpy mound. Its pipes
    glow and swell as it plays its chords, a sac at the back breathes, sensor tendrils twitch, and a bone
    harpoon horn in front turns to aim at swimmers."""
    m = SpriteModel('coral_organ', (256, 128), {'coral_organ': {}}, _organ_colour, [], palette=ORGAN_PAL)
    for n, rows in (('base', _organ_base()), ('sac', ORGAN_SAC), ('horn', ORGAN_HORN), ('ring', ORGAN_RING), ('tendril', ORGAN_TENDRIL)):
        m.sprite(n, Sprite(rows))
    for (_x, h, w, _z) in ORGAN_PIPES:
        m.sprite(f'pipe{h}', Sprite(_organ_pipe(h, w)))
        m.sprite(f'mouth{h}', Sprite(_organ_mouth(h, w)))
        m.no_inner.add(f'pipe{h}')
    cimg = _block_img('sculk_coral').resize((10, 10), Image.NEAREST)
    fimg = _block_img('sculk_coral_fan').resize((10, 10), Image.NEAREST)
    m.sprite('coral', Sprite([''.join('c' if v[3] > 127 else '.' for v in row) for row in np.asarray(cimg)]))
    m.sprite('fan', Sprite([''.join('c' if v[3] > 127 else '.' for v in row) for row in np.asarray(fimg)]))
    base = m.part('base', pivot=(0, 24, 0))
    m.extrude(base, 'base', (-12, -8), lambda c: 10.8 * math.sqrt(max(0.06, 1 - ((c + 0.5 - 12) / 12.4) ** 2)) + 0.4,
              layers=((1.0, 0.55), (0.75, 0.85), (0.45, 1.0)))
    for i, (x, h, w, z) in enumerate(ORGAN_PIPES):
        p = base.part(f'pipe_{i}', pivot=(x, -5, z), rot=(0, x * 0.03, x * -0.018))
        hw = w / 2.0
        m.extrude(p, f'pipe{h}', (-hw, -h + 2), lambda c, hw=hw: hw, layers=((1.0, 0.5), (0.94, 1.0)), min_half=1)
        m.plane(p, f'mouth{h}', (-hw, -h + 2), axis='z')
        p.cubes[-1].origin = (-hw, -h + 2, -hw - 0.05)
        m.plane(p, 'ring', (-2, -2), axis='y')
        p.cubes[-1].origin = (-hw, -h + 1.95, -hw)
        p.cubes[-1].size = (w, 0, w)
    sac = base.part('sac', pivot=(0, -6, 6))
    m.extrude(sac, 'sac', (-4, -5), lambda c: [2, 3, 3.6, 4, 4, 3.6, 3, 2][c], layers=((1.0, 0.6), (0.7, 1.0)))
    horn = base.part('launcher', pivot=(0, -8.5, -3.5))
    m.extrude(horn, 'horn', (-10, -3.5), lambda c: [3.0, 2.8, 2.4, 2.0, 1.8, 1.6, 1.5, 1.4, 1.2, 1.0][c], layers=((1.0, 0.6), (0.7, 1.0)))
    for i, (x, z, ry) in enumerate(((-10, 2, 0.5), (10, 3, -0.6), (-6, 7, 0.2), (7, 7, -0.3))):
        t = base.part(f'tendril_{i}', pivot=(x, -4, z), rot=(-0.15, ry, 0.2 if x < 0 else -0.2))
        m.plane(t, 'tendril', (-1.5, -7))
    for i, (x, z, ry, nm) in enumerate(((-11, -4, 0.6, 'fan'), (11, -3, -0.7, 'coral'), (-12, 6, 0.2, 'coral'), (12, 6, -0.3, 'fan'),
                                         (-3, -10, 0.9, 'coral'), (5, -10, -0.4, 'fan'))):
        c = base.part(f'coral_{i}', pivot=(x, -1, z), rot=(0, ry, 0))
        m.plane(c, nm, (-5, -9))
        c2 = c.part(f'coral_{i}_x', rot=(0, math.pi / 2, 0))
        m.plane(c2, nm, (-5, -9))
    return m


MODELS = {'kazoo_fish': kazoo_fish, 'tubafish': tubafish, 'fanfare_eel': fanfare_eel, 'sculk_fish': sculk_fish, 'coral_organ': coral_organ}
