"""32x multi-part blocks painted onto 64x64 sheets (see hd.py), with the model layouts that use them."""
from __future__ import annotations

import math

import hd
from hd import rect, uv
from texlib import Tex, hx, lerp, ramp

# ============================================================================ Sift Drum

DRUM_WOOD = ramp('#3b2a4f', '#523a69', '#6a4d83', '#83659c', '#9f83b6', '#bba6cf')
DRUM_HOOP = ramp('#173f5c', '#22597d', '#2f789f', '#4b9cc0', '#7cc4de', '#c2ecf7')
DRUM_GOLD = (hx('#7a4f12'), hx('#d9a63c'), hx('#ffe79a'))
DRUM_ROPE = ramp('#5e3a4c', '#a87d90', '#d4adbd', '#f0dbe3')
DRUM_HIDE = ramp('#9c8494', '#bba5b4', '#d9c8d2', '#eadfe5', '#f6eff3')
CORE_GLOW = ramp('#0f6f78', '#1ec8c8', '#7ff7ee', '#ffffff')

# texel rectangles on the drum sheet (x, y, w, h)
SHELL = (0, 0, 26, 20)
SHELL_CORE = (0, 20, 26, 20)
HEAD = (28, 0, 24, 24)
HEAD_STRUCK = (28, 24, 24, 24)
HOOP_OUT = (0, 40, 28, 5)
HOOP_IN = (0, 45, 28, 5)
HOOP_TOP = (0, 50, 28, 2)
BASE_SIDE = (0, 52, 28, 4)
BASE_TOP = (0, 56, 28, 2)
HOOP_END = (54, 0, 2, 5)

NOTE_GLYPH = [
    '....WW..',
    '....W.W.',
    '....W..W',
    '....W...',
    '..WWW...',
    '.WWWW...',
    '.WWW....',
    '........',
]


def _shell(t, x0, y0, seed, core=False):
    w, h = SHELL[2], SHELL[3]
    hd.staves(t, x0, y0, w, h, DRUM_WOOD, seed, stave=5, top_shadow=2, bottom_shadow=2)
    # a thin inlaid band of serbim around the middle
    for xx in range(w):
        t.set(x0 + xx, y0 + 9, DRUM_HOOP[3] if xx % 5 else DRUM_HOOP[4])
        t.set(x0 + xx, y0 + 10, DRUM_HOOP[1])
    # rope lacing: a zigzag from top lugs to bottom lugs
    tops = [0, 13, 26]
    bottoms = [6, 19]
    pts = []
    for i, tx in enumerate(tops):
        pts.append((x0 + tx, y0 + 2))
        if i < len(bottoms):
            pts.append((x0 + bottoms[i], y0 + h - 3))
    hd.rope(t, pts, DRUM_ROPE, width=2)
    # lugs where the rope is tied to the hoops
    for tx in tops:
        rect(t, x0 + tx - 1, y0, 3, 2, DRUM_HOOP[2])
        t.set(x0 + tx - 1, y0, DRUM_HOOP[4])
        t.set(x0 + tx + 1, y0 + 1, DRUM_HOOP[0])
    for bx in bottoms:
        rect(t, x0 + bx - 1, y0 + h - 2, 3, 2, DRUM_HOOP[2])
        t.set(x0 + bx - 1, y0 + h - 2, DRUM_HOOP[4])
        t.set(x0 + bx + 1, y0 + h - 1, DRUM_HOOP[0])
    if core:
        # a riveted socket in the middle of the shell, dark until the glow layer lights it
        sx, sy, sw, sh = x0 + 8, y0 + 5, 10, 10
        for yy in range(sh):
            for xx in range(sw):
                corner = (xx in (0, sw - 1)) and (yy in (0, sh - 1))
                if corner:
                    continue
                edge = xx in (0, sw - 1) or yy in (0, sh - 1)
                c = DRUM_HOOP[3] if (edge and (xx == 0 or yy == 0)) else DRUM_HOOP[1] if edge else hx('#0b1a24')
                t.set(sx + xx, sy + yy, c)
        for (rx, ry) in ((1, 1), (sw - 2, 1), (1, sh - 2), (sw - 2, sh - 2)):
            t.set(sx + rx, sy + ry, DRUM_GOLD[1])
        for yy in range(2, sh - 2):
            for xx in range(2, sw - 2):
                d = abs(xx - (sw - 1) / 2) + abs(yy - (sh - 1) / 2)
                if d < 3.6:
                    t.set(sx + xx, sy + yy, hx('#123447'))


def _core_glow(t, x0, y0):
    sx, sy, sw, sh = x0 + 8, y0 + 5, 10, 10
    for yy in range(2, sh - 2):
        for xx in range(2, sw - 2):
            d = abs(xx - (sw - 1) / 2) + abs(yy - (sh - 1) / 2)
            if d < 3.6:
                c = CORE_GLOW[1] if d > 2.2 else CORE_GLOW[2]
                t.set(sx + xx, sy + yy, c)
    t.set(sx + 4, sy + 3, CORE_GLOW[3])
    t.set(sx + 3, sy + 4, CORE_GLOW[3])


def _head(t, x0, y0, seed, struck=False):
    w = HEAD[2]
    hd.hide(t, x0, y0, w, w, DRUM_HIDE, seed)
    # tacks holding the hide, a gold dot every third texel just inside the rim
    for k in range(2, w - 2, 3):
        for (x, y) in ((k, 2), (k, w - 3), (2, k), (w - 3, k)):
            t.set(x0 + x, y0 + y, DRUM_GOLD[1])
    for (x, y) in ((2, 2), (w - 3, 2), (2, w - 3), (w - 3, w - 3)):
        t.set(x0 + x, y0 + y, DRUM_GOLD[2])
    # the Sift's note, worn into the middle of the playing spot
    gx, gy = x0 + 8, y0 + 8
    for j, row in enumerate(NOTE_GLYPH):
        for i, ch in enumerate(row):
            if ch == 'W':
                t.set(gx + i, gy + j, DRUM_HIDE[1])
    if struck:
        cx = cy = (w - 1) / 2
        for yy in range(3, w - 3):
            for xx in range(3, w - 3):
                d = math.hypot(xx - cx, yy - cy)
                if 3.5 < d < 4.6 or 8.0 < d < 9.0:
                    t.set(x0 + xx, y0 + yy, DRUM_HIDE[4])
                elif d < 2.0:
                    t.set(x0 + xx, y0 + yy, DRUM_HIDE[4])


def drum_sheets():
    t = Tex(hd.SHEET, hd.SHEET)
    glow = Tex(hd.SHEET, hd.SHEET)
    _shell(t, SHELL[0], SHELL[1], 71)
    _shell(t, SHELL_CORE[0], SHELL_CORE[1], 71, core=True)
    _core_glow(glow, SHELL_CORE[0], SHELL_CORE[1])
    _head(t, HEAD[0], HEAD[1], 72)
    _head(t, HEAD_STRUCK[0], HEAD_STRUCK[1], 72, struck=True)
    # the note glyph glows on the head while a Warden Core is inside
    for j, row in enumerate(NOTE_GLYPH):
        for i, ch in enumerate(row):
            if ch == 'W':
                glow.set(HEAD[0] + 8 + i, HEAD[1] + 8 + j, CORE_GLOW[2])
    hd.metal_band(t, *HOOP_OUT, DRUM_HOOP, rivet=DRUM_GOLD, rivet_every=7, rivet_phase=3)
    inner = [lerp(c, DRUM_HOOP[0], 0.45) for c in DRUM_HOOP]
    hd.metal_band(t, *HOOP_IN, inner)
    for xx in range(HOOP_TOP[2]):
        t.set(HOOP_TOP[0] + xx, HOOP_TOP[1], DRUM_HOOP[5] if xx % 6 == 2 else DRUM_HOOP[4])
        t.set(HOOP_TOP[0] + xx, HOOP_TOP[1] + 1, DRUM_HOOP[3])
    hd.metal_band(t, *BASE_SIDE, [lerp(c, DRUM_HOOP[0], 0.2) for c in DRUM_HOOP], rivet=DRUM_GOLD, rivet_every=7, rivet_phase=5)
    rect(t, *BASE_TOP, DRUM_HOOP[1])
    hd.metal_band(t, *HOOP_END, DRUM_HOOP)
    return t, glow


def drum_model(hit, core, ns='thesift', bid='sift_drum'):
    """Block model json for one drum state. hit: 0 at rest, 1 struck (head pressed in), 2 rebound."""
    def faces(**f):
        return {k: {'texture': v[0], 'uv': uv(*v[1])} for k, v in f.items()}

    S, G = '#sheet', '#glow'
    shell = SHELL_CORE if core else SHELL
    head_top = {0: 13.25, 1: 12.5, 2: 13.75}[hit]
    els = [
        {'from': [1, 0, 1], 'to': [15, 2, 15], 'faces': faces(north=(S, BASE_SIDE), south=(S, BASE_SIDE), east=(S, BASE_SIDE), west=(S, BASE_SIDE),
                                                             up=(S, BASE_TOP), down=(S, HEAD))},
        {'from': [1.5, 2, 1.5], 'to': [14.5, 12, 14.5], 'faces': faces(north=(S, shell), south=(S, shell), east=(S, shell), west=(S, shell))},
        {'from': [2, 11, 2], 'to': [14, head_top, 14], 'faces': faces(up=(S, HEAD_STRUCK if hit else HEAD))},
    ]
    # the top hoop: four bars around the head
    end = HOOP_END
    for frm, to, outer, inner in (([1, 11.5, 1], [15, 14, 2], 'north', 'south'), ([1, 11.5, 14], [15, 14, 15], 'south', 'north'),
                                  ([1, 11.5, 1], [2, 14, 15], 'west', 'east'), ([14, 11.5, 1], [15, 14, 15], 'east', 'west')):
        ends = ('east', 'west') if outer in ('north', 'south') else ('north', 'south')
        f = {outer: (S, HOOP_OUT), inner: (S, HOOP_IN), 'up': (S, HOOP_TOP), 'down': (S, HOOP_IN), ends[0]: (S, end), ends[1]: (S, end)}
        els.append({'from': frm, 'to': to, 'faces': faces(**f)})
    if core:
        els.append({'from': [1.49, 2, 1.49], 'to': [14.51, 12, 14.51], 'shade': False, 'light_emission': 15,
                    'faces': faces(north=(G, SHELL_CORE), south=(G, SHELL_CORE), east=(G, SHELL_CORE), west=(G, SHELL_CORE))})
        els.append({'from': [2, head_top + 0.02, 2], 'to': [14, head_top + 0.02, 14], 'shade': False, 'light_emission': 15,
                    'faces': faces(up=(G, HEAD))})
    return {'parent': 'minecraft:block/block',
            'textures': {'particle': f'{ns}:block/lullwood_planks', 'sheet': f'{ns}:block/{bid}', 'glow': f'{ns}:block/{bid}_glow'},
            'elements': els}
