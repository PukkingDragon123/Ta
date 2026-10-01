"""Every Sift item sprite, drawn at 32x in the chunky glossy style of rpgsprite.py."""
from __future__ import annotations

import numpy as np
from PIL import Image

from rpgsprite import (N, Sprite, capsule, ellipse, from_rows, poly, ramp, rect, rrect)

# ----------------------------------------------------------------------------- materials
SIFTITE = '#58d9ea'
SIFT_PINK = '#f39ad8'
SERBIM = '#4a9fe0'
WOOD = '#8f5d34'
DARKWOOD = '#5a3a2a'
GOLD = '#f0b93a'
BONE = '#ece4cc'
SCULK = '#1b4250'
GLOW = '#3ff5e6'
CHROME = ['#4b3f9e', '#7f86e6', '#b9b8ff', '#ffd2f2', '#ffffff']
IRON = '#a9b4c2'
HIDE = '#6f94ad'
LEAF = '#4fbf5a'


def overlay(img, rows, keys, x0, y0):
    px = img.load()
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in '. ':
                continue
            x, y = x0 + i, y0 + j
            if 0 <= x < N and 0 <= y < N:
                c = keys[ch]
                px[x, y] = c if len(c) == 4 else (*c, 255)
    return img


def hexa(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def handle(s, x0=5, y0=27, x1=20, y1=12, r=1.6, mat=WOOD, grip=True):
    s.add(capsule(x0, y0, x1, y1, r), mat, 'dome', gloss=0.4)
    if grip:
        # leather wrap near the end
        s.add(capsule(x0 + 1.5, y0 - 1.5, x0 + 4.5, y0 - 4.5, r + 0.4), '#4a2a3a', 'dome', gloss=0.2)
    return s


# ----------------------------------------------------------------------------- tools

def sword():
    s = Sprite()
    s.add(poly([(24.5, 3), (29, 3), (29, 7.5), (13, 23.5), (8.5, 19)]), SIFTITE, 'bevel', depth=2.2)
    s.add(poly([(26.5, 4.5), (27.5, 5.5), (11.5, 21.5), (10.5, 20.5)]), '#c8fbff', 'flat', gloss=0, outline=False)
    s.add(capsule(5, 27, 9.5, 22.5, 1.7), '#4a2a3a', 'dome', gloss=0.3)
    s.add(poly([(5, 16.5), (8, 16.5), (15.5, 24), (15.5, 27), (12.5, 27), (5, 19.5)]), GOLD, 'bevel', depth=1.5)
    s.add(ellipse(10.3, 21.7, 1.8, 1.8), SIFT_PINK, 'dome', gloss=1.5)
    s.add(ellipse(4.2, 27.8, 2.2, 2.2), GOLD, 'dome')
    return s.render()


def pickaxe():
    s = Sprite()
    handle(s, 5, 27, 21, 11)
    pts = [(5.5, 8.5), (10, 5), (16, 3.5), (22, 4.5), (26.5, 8), (28, 13.5), (27, 19)]
    m = np.zeros((N, N), dtype=bool)
    for (a, b), (c, d), r in zip(pts, pts[1:], (1.4, 2.0, 2.4, 2.4, 2.0, 1.4)):
        m |= capsule(a, b, c, d, r)
    s.add(m, SIFTITE, 'dome')
    s.add(ellipse(19.5, 8.5, 2.2, 2.2), SIFT_PINK, 'dome', gloss=1.5)
    return s.render()


def axe():
    s = Sprite()
    handle(s, 5, 27, 21, 11)
    s.add(poly([(14, 6), (20, 2), (27.5, 7), (28.5, 14.5), (23, 17), (18.5, 12)]), SIFTITE, 'bevel', depth=2.4)
    s.add(poly([(24.5, 4.5), (28.5, 13.5), (27, 14.5), (23.5, 6)]), '#c8fbff', 'flat', gloss=0, outline=False)
    s.add(ellipse(18.5, 9.5, 1.8, 1.8), SIFT_PINK, 'dome', gloss=1.5)
    return s.render()


def shovel():
    s = Sprite()
    handle(s, 5, 27, 18, 14)
    s.add(ellipse(23, 9, 6.6, 4.6, -45), SIFTITE, 'dome')
    s.add(capsule(17.5, 14.5, 19.5, 12.5, 2.0), GOLD, 'dome')
    return s.render()


def hoe():
    s = Sprite()
    handle(s, 5, 27, 21, 11)
    s.add(poly([(14.5, 5), (26, 4), (28, 6.5), (26, 9.5), (24.5, 9.5), (24.5, 15), (21, 15), (21, 9), (15, 9)]), SIFTITE, 'bevel', depth=2.0)
    s.add(ellipse(22.5, 7, 1.6, 1.6), SIFT_PINK, 'dome', gloss=1.5)
    return s.render()


def spear():
    s = Sprite()
    s.add(capsule(3, 29, 20, 12, 1.25), WOOD, 'dome', gloss=0.4)
    s.add(capsule(4.5, 27.5, 7, 25, 1.7), '#4a2a3a', 'dome', gloss=0.2)
    s.add(poly([(18.5, 13.5), (20, 7.5), (29.5, 2.5), (24.5, 12)]), SIFTITE, 'bevel', depth=2.0)
    s.add(poly([(21, 9), (27.5, 4.5), (22.5, 11)]), '#c8fbff', 'flat', gloss=0, outline=False)
    s.add(capsule(18, 14, 20, 12, 2.1), GOLD, 'dome')
    return s.render()


# ----------------------------------------------------------------------------- armour

def helmet():
    s = Sprite()
    m = ellipse(16, 17, 12.5, 12) & (rect(0, 0, 32, 25))
    m &= ~rect(9, 16, 23, 32)
    m |= rect(3.5, 16, 9, 25) | rect(23, 16, 28.5, 25)
    s.add(m, SIFTITE, 'dome')
    s.add(capsule(16, 5.5, 16, 14, 1.6), SIFT_PINK, 'dome', gloss=1.3)
    s.add(rect(3.5, 23, 9, 25.5) | rect(23, 23, 28.5, 25.5), GOLD, 'bevel', depth=1)
    return s.render()


def chestplate():
    s = Sprite()
    m = poly([(4, 7), (11, 4), (16, 7), (21, 4), (28, 7), (28.5, 14), (24.5, 15), (24.5, 28), (7.5, 28), (7.5, 15), (3.5, 14)])
    m &= ~ellipse(16, 4, 4.5, 4.5)
    s.add(m, SIFTITE, 'bevel', depth=3)
    s.add(rect(7.5, 22, 24.5, 25), GOLD, 'bevel', depth=1)
    s.add(ellipse(16, 15, 2.6, 2.6), SIFT_PINK, 'dome', gloss=1.6)
    s.add(capsule(10, 9, 13, 18, 0.8), '#c8fbff', 'flat', gloss=0, outline=False)
    return s.render()


def leggings():
    s = Sprite()
    m = rect(7, 5, 25, 11) | rect(7, 11, 15, 28) | rect(17, 11, 25, 28)
    s.add(m, SIFTITE, 'bevel', depth=2.5)
    s.add(rect(7, 5, 25, 8), GOLD, 'bevel', depth=1)
    s.add(ellipse(16, 6.5, 1.5, 1.5), SIFT_PINK, 'dome', gloss=1.5)
    return s.render()


def boots():
    s = Sprite()
    left = poly([(4, 9), (13, 9), (13, 21), (15.5, 24), (15.5, 28), (3, 28), (3, 23), (4, 21)])
    right = poly([(19, 9), (28, 9), (28, 21), (29, 23), (29, 28), (16.5, 28), (16.5, 24), (19, 21)])
    s.add(left | right, SIFTITE, 'bevel', depth=2.5)
    s.add(rect(3, 25.5, 15.5, 28) | rect(16.5, 25.5, 29, 28), GOLD, 'bevel', depth=1)
    return s.render()


# ----------------------------------------------------------------------------- materials

def ingot(mat, streak=None):
    s = Sprite()
    s.add(poly([(4, 16), (20, 8.5), (28.5, 13), (12.5, 21)]), mat, 'bevel', depth=2.5)
    s.add(poly([(4, 16), (12.5, 21), (12.5, 25.5), (4, 20.5)]), ramp(mat)[1:] + [ramp(mat)[-1]], 'flat', gloss=0)
    s.add(poly([(12.5, 21), (28.5, 13), (28.5, 17.5), (12.5, 25.5)]), ramp(mat)[:4] + [ramp(mat)[3]], 'flat', gloss=0)
    if streak:
        s.add(poly([(10, 15), (19, 11), (21, 12), (12, 16)]), streak, 'flat', gloss=0, outline=False)
    return s.render()


def nugget():
    s = Sprite()
    s.add(ellipse(12, 19, 6, 5, 20), SIFTITE, 'dome')
    s.add(ellipse(21, 14, 5, 4.5, -30), SIFTITE, 'dome')
    s.add(ellipse(19, 23, 3.5, 3, 0), SIFT_PINK, 'dome', gloss=1.4)
    return s.render()


def raw_serbim():
    s = Sprite()
    m = ellipse(14, 17, 9, 7.5, 15) | ellipse(21, 12, 6.5, 5.5) | ellipse(10, 23, 5, 4) | ellipse(22, 21, 5, 4.5)
    s.add(m, '#6e8fb8', 'dome', gloss=0.5)
    for (x, y, r) in ((13, 14, 2.2), (20, 20, 1.8), (9, 21, 1.4), (22, 11, 1.5)):
        s.add(ellipse(x, y, r, r), SERBIM, 'dome', gloss=1.2)
    return s.render()


def pearl():
    s = Sprite()
    s.add(ellipse(16, 16, 10.5, 10.5), CHROME, 'dome', gloss=1.4)
    s.add(ellipse(16, 16, 10.5, 10.5) & ~ellipse(14, 14, 10, 10), ['#6a5bd0', '#8f7ff0', '#a0f0ff', '#d0ffff', '#ffffff'], 'flat', gloss=0, outline=False)
    return s.render()


def slime_ball():
    s = Sprite()
    s.add(ellipse(16, 17, 10, 9) | ellipse(10, 25, 2, 2.5) | ellipse(22, 26, 1.8, 2.2), '#b6f04a', 'dome', gloss=1.4)
    s.add(ellipse(17, 18, 5, 4.5), '#eaff7a', 'dome', gloss=1.0, outline=False)
    return s.render()


def star_shard():
    s = Sprite()
    s.add(poly([(6, 26), (11, 15), (24, 4), (27, 6), (17, 20)]), '#ffd65a', 'bevel', depth=2)
    s.add(poly([(11, 15), (24, 4), (17, 20)]), '#fff2a8', 'flat', gloss=0, outline=False)
    s.add(poly([(20, 22), (24, 15), (28, 13), (26, 19)]), '#ffc23f', 'bevel', depth=1.5)
    return s.render()


def echo_seed():
    s = Sprite()
    for (x, y, a) in ((10, 12, -30), (20, 10, 20), (15, 20, 60), (24, 21, -10)):
        s.add(ellipse(x, y, 4, 2.8, a), '#2a7f8f', 'dome')
        s.add(ellipse(x, y, 1.6, 1.1, a), GLOW, 'dome', gloss=1.2, outline=False)
    return s.render()


def choir_pod():
    s = Sprite()
    s.add(capsule(16, 3, 17, 8, 1.2), '#3a8a4a', 'dome')
    s.add(ellipse(16, 18, 8, 11), '#a46ee8', 'dome')
    s.add(capsule(16, 9, 16, 27, 0.6), '#5c3a9a', 'flat', gloss=0, outline=False)
    s.add(ellipse(21, 6, 4, 2, -25), LEAF, 'dome')
    return s.render()


def pitcher_bulb():
    s = Sprite()
    s.add(ellipse(16, 18, 10, 9.5), '#55c8de', 'dome')
    s.add(ellipse(16, 12, 4, 1.6), '#2f8aa8', 'flat', gloss=0, outline=False)
    s.add(capsule(16, 4, 17.5, 10, 1.4), '#3a8a4a', 'dome')
    s.add(ellipse(21.5, 6.5, 4.2, 2.1, -20), LEAF, 'dome')
    return s.render()


def warden_core():
    s = Sprite()
    h = ellipse(10.5, 12, 6.5, 6.5) | ellipse(21.5, 12, 6.5, 6.5) | poly([(4.5, 14), (27.5, 14), (16, 28.5)])
    s.add(h, SCULK, 'dome')
    s.add(ellipse(16, 15, 4.5, 4.5) | poly([(12, 17), (20, 17), (16, 22)]), GLOW, 'dome', gloss=1.6)
    for (a, b, c, d) in ((5, 8, 2, 4), (27, 8, 30, 4), (16, 28, 16, 31)):
        s.add(capsule(a, b, c, d, 0.9), '#0f2a33', 'dome', gloss=0)
    return s.render()


def hide():
    s = Sprite()
    m = poly([(6, 6), (11, 9), (21, 9), (26, 6), (25, 13), (28, 20), (24, 22), (25, 28), (20, 25), (12, 25), (7, 28), (8, 22), (4, 20), (7, 13)])
    s.add(m, HIDE, 'bevel', depth=3, gloss=0.4)
    s.add(from_rows(['#..#..#..#..#'], 10, 12) | from_rows(['#..#..#..#..#'], 10, 21), '#c9e6f2', 'flat', gloss=0, outline=False)
    return s.render()


def cake():
    s = Sprite()
    s.add(poly([(3, 15), (16, 8), (29, 14), (16, 21)]), '#ffc0dc', 'bevel', depth=2)
    s.add(poly([(3, 15), (16, 21), (16, 28), (3, 22)]), '#f6e3c8', 'flat', gloss=0)
    s.add(poly([(16, 21), (29, 14), (29, 21), (16, 28)]), '#e8cfa8', 'flat', gloss=0)
    s.add(poly([(3, 18), (16, 24), (29, 17), (29, 18.5), (16, 25.5), (3, 19.5)]), '#8fe8f0', 'flat', gloss=0, outline=False)
    for (x, y) in ((11, 13), (16, 11), (21, 13), (16, 15)):
        s.add(ellipse(x, y, 1.8, 1.8), '#ff5a8a', 'dome', gloss=1.5)
    return s.render()


def stew():
    s = Sprite()
    s.add(ellipse(16, 18, 12.5, 9) & rect(0, 16, 32, 32), WOOD, 'dome', gloss=0.3)
    s.add(ellipse(16, 16, 12.5, 4), '#5e3a2a', 'flat', gloss=0)
    s.add(ellipse(16, 16, 11, 3.2), '#b48ae8', 'dome', gloss=1.0)
    for (x, y, c) in ((11, 16, '#ffe07a'), (16, 15, '#ff8ac0'), (20, 17, '#8ff0ff'), (14, 17.5, '#ffffff')):
        s.add(ellipse(x, y, 1.3, 0.9), c, 'flat', gloss=0, outline=False)
    return s.render()


def skewer():
    s = Sprite()
    s.add(capsule(4, 28, 26, 6, 0.9), BONE, 'dome', gloss=0.3)
    for (x, y) in ((11, 21), (16, 16), (21, 11)):
        s.add(ellipse(x, y, 4, 3, -45), '#5af0c8', 'dome', gloss=1.3)
        s.add(ellipse(x - 0.5, y - 0.5, 1.4, 1.0, -45), '#e8fff6', 'flat', gloss=0, outline=False)
    return s.render()


def lantern():
    s = Sprite()
    s.add(rrect(10, 3, 22, 7, 1.5), DARKWOOD, 'bevel', depth=1.2)
    s.add(capsule(16, 1, 16, 3, 1.2), GOLD, 'dome')
    s.add(rrect(9, 7, 23, 25, 2), '#9fe8ff', 'flat', gloss=0)
    s.add(ellipse(16, 16, 4.5, 5.5), '#aef7ff', 'dome', gloss=1.6, outline=False)
    s.add(rect(9, 7, 11, 25) | rect(21, 7, 23, 25), DARKWOOD, 'bevel', depth=1)
    s.add(rrect(8, 24, 24, 29, 1.5), DARKWOOD, 'bevel', depth=1.2)
    return s.render()


def bucket(fill=None):
    s = Sprite()
    s.add(poly([(6, 11), (26, 11), (24, 28), (8, 28)]), IRON, 'bevel', depth=3)
    s.add(ellipse(16, 11, 10, 3.5), '#5c6670', 'flat', gloss=0)
    if fill:
        s.add(ellipse(16, 11, 8.6, 2.6), fill, 'dome', gloss=1.2, outline=False)
    s.add(capsule(6, 11, 6, 6, 0.7) | capsule(26, 11, 26, 6, 0.7) | capsule(6, 6, 26, 6, 0.7), '#7a8692', 'dome', gloss=0.2)
    return s.render()


def journal():
    s = Sprite()
    s.add(poly([(6, 4), (24, 3), (27, 9), (25, 14), (27, 19), (25, 28), (7, 29), (5, 22), (7, 16), (5, 10)]), '#f3e2bc', 'bevel', depth=2.5, gloss=0)
    s.add(from_rows(['##########', '', '#########', '', '#######', '', '##########', '', '######'], 10, 9), '#7a64b8', 'flat', gloss=0, outline=False)
    return s.render()


def codex():
    s = Sprite()
    s.add(rrect(6, 3, 28, 29, 2.5), '#1f3f78', 'bevel', depth=2.5)
    s.add(rect(6, 26, 28, 29), '#f6efd8', 'flat', gloss=0)
    s.add(rect(6, 3, 9, 29), '#152a52', 'bevel', depth=1)
    for (x, y) in ((24.5, 6.5), (24.5, 23), (11.5, 6.5), (11.5, 23)):
        s.add(ellipse(x, y, 1.6, 1.6), GOLD, 'dome')
    s.add(ellipse(18, 14.5, 4, 4), GLOW, 'dome', gloss=1.6)
    return s.render()


def disc():
    s = Sprite()
    s.add(ellipse(16, 16, 12, 12), '#2a2a3a', 'dome', gloss=0.8)
    s.add(ellipse(16, 16, 4.5, 4.5), '#f59ad0', 'dome', gloss=1.0, outline=False)
    s.add(ellipse(16, 16, 1, 1), '#1a1a24', 'flat', gloss=0, outline=False)
    return s.render()


def chime():
    s = Sprite()
    s.add(rrect(5, 3, 27, 6, 1.5), WOOD, 'bevel', depth=1)
    for (x, l) in ((8, 18), (13, 23), (19, 20), (24, 15)):
        s.add(capsule(x, 7, x, 7 + l, 1.3), '#9fdcf2', 'dome', gloss=1.2)
    s.add(ellipse(16, 27, 2.5, 2.5), GLOW, 'dome', gloss=1.5)
    return s.render()


def petals():
    s = Sprite()
    for (x, y, a) in ((10, 11, 30), (21, 9, -20), (13, 21, -60), (23, 20, 45), (17, 15, 0)):
        s.add(ellipse(x, y, 4.2, 2.6, a), '#ff9ccc', 'dome', gloss=1.0)
    return s.render()


def glowbell():
    s = Sprite()
    s.add(capsule(16, 2, 15, 12, 1.2) | capsule(15, 12, 18, 22, 1.2), '#3a8a6a', 'dome', gloss=0.3)
    for (x, y) in ((11, 11), (21, 16), (13, 25)):
        s.add(ellipse(x, y, 3.5, 3.8), '#ffe27a', 'dome', gloss=1.4)
    s.add(ellipse(20, 7, 3.5, 1.8, 30), LEAF, 'dome')
    return s.render()


def door(mat, trim):
    s = Sprite()
    s.add(rrect(7, 2, 25, 30, 2), mat, 'bevel', depth=2)
    s.add(rect(10, 5, 22, 14) | rect(10, 17, 22, 27), trim, 'bevel', depth=1.5)
    s.add(ellipse(21, 16, 1.4, 1.4), GOLD, 'dome')
    return s.render()


def template():
    s = Sprite()
    s.add(rrect(5, 3, 27, 29, 2), '#3a4458', 'bevel', depth=2.5)
    s.add(poly([(16, 7), (22, 14), (16, 25), (10, 14)]), SIFTITE, 'bevel', depth=2)
    s.add(poly([(16, 7), (22, 14), (16, 14)]), '#c8fbff', 'flat', gloss=0, outline=False)
    return s.render()


def slingshot(pull):
    s = Sprite()
    s.add(capsule(8, 28, 14, 18, 1.6), WOOD, 'dome', gloss=0.4)
    s.add(capsule(14, 18, 9, 8, 1.4) | capsule(14, 18, 24, 12, 1.4), WOOD, 'dome', gloss=0.4)
    s.add(ellipse(8.5, 7.5, 1.8, 1.8) | ellipse(24.5, 11.5, 1.8, 1.8), SIFTITE, 'dome', gloss=1.4)
    img = s.render()
    # the band: pulled further back each frame, a glowing slime ball in the pouch
    back = [(17, 13), (14, 17), (11, 21), (8, 24)][pull + 1 if pull >= 0 else 0]
    t = Sprite()
    t.add(capsule(9, 8, back[0], back[1], 0.55) | capsule(24, 12, back[0], back[1], 0.55), '#e8c0a0', 'flat', gloss=0)
    if pull >= 0:
        t.add(ellipse(back[0], back[1], 2.3, 2.3), '#b6f04a', 'dome', gloss=1.4)
    band = t.render()
    img.alpha_composite(band)
    return img


def baton():
    s = Sprite()
    s.add(capsule(5, 27, 26, 6, 1.0), BONE, 'dome', gloss=0.8)
    s.add(capsule(5, 27, 9, 23, 1.9), '#2a3550', 'dome', gloss=0.4)
    s.add(ellipse(10, 22, 1.8, 1.8), GOLD, 'dome')
    s.add(ellipse(26, 6, 1.5, 1.5), GLOW, 'dome', gloss=1.4)
    return s.render()


def staff():
    """The Conductor's Staff: a twisted dark shaft bound in gold, a human skull with glowing eye
    sockets, and two gold prongs rising from it to cradle a sculk orb."""
    s = Sprite()
    s.add(capsule(3, 29, 17, 15, 1.6), '#5e3a78', 'dome', gloss=0.6)
    # twisted gold bindings
    for t in (0.2, 0.45, 0.7):
        x, y = 3 + 14 * t, 29 - 14 * t
        s.add(capsule(x - 1.3, y - 1.3, x + 1.3, y + 1.3, 0.75), GOLD, 'dome')
    s.add(capsule(16, 16, 18.5, 13.5, 2.1), GOLD, 'dome')
    # prongs curving up from behind the skull around the orb
    s.add(capsule(17, 10, 18, 5, 1.0) | capsule(18, 5, 21, 1.8, 1.0), GOLD, 'dome')
    s.add(capsule(24, 14, 28, 13, 1.0) | capsule(28, 13, 30.2, 10, 1.0), GOLD, 'dome')
    s.add(ellipse(25.5, 5.5, 3.6, 3.6), GLOW, 'dome', gloss=1.8)
    # the skull: cranium, cheekbones and jaw
    skull = ['#5e5648', '#a39a84', '#ddd6c2', '#f3efe4', '#ffffff']
    s.add(ellipse(21.5, 10.5, 5.6, 5.0), skull, 'dome', gloss=0.9)
    s.add(rrect(18.5, 13, 24.5, 17.5, 1.5), skull, 'bevel', depth=1.2, gloss=0.3)
    img = s.render()
    v, g, d = (24, 14, 30), (63, 245, 230), (150, 140, 112)
    overlay(img, ['vvv.vvv', 'vgv.vgv', 'vvv.vvv', '...v...', '.ddddd.', '.d.d.d.'], {'v': v, 'g': g, 'd': d}, 18, 10)
    for (x, y) in ((29, 2), (14, 6), (30, 17)):
        overlay(img, ['.w.', 'wWw', '.w.'], {'w': (160, 255, 248), 'W': (255, 255, 255)}, x - 1, y - 1)
    return img


# ----------------------------------------------------------------------------- spawn eggs

def egg(base, spot, face=None, keys=None, top=None):
    s = Sprite()
    if top:
        for mask, mat in top:
            s.add(mask, mat, 'dome')
    m = ellipse(16, 17.5, 10.5, 12.5) & (rect(0, 17.5, 32, 32) | ellipse(16, 17.5, 9.5, 13.5))
    s.add(m, base, 'dome', gloss=1.2)
    for (x, y, r) in ((10, 22, 1.6), (21, 25, 1.3), (22, 10, 1.2)):
        s.add(ellipse(x, y, r, r), spot, 'dome', gloss=0, outline=False)
    img = s.render()
    if face:
        overlay(img, face, {k: hexa(v) for k, v in keys.items()}, 16 - len(face[0]) // 2, 18 - len(face) // 2)
    return img


EGGS = {
    'bulb': ('#78a5e3', '#63c6df', dict(face=['EEEE....EEEE', 'EEEE....EEEE', '.....MM.....', '.....MM.....'], keys={'E': '#2f2777', 'M': '#4a3a9f'},
                                       top=[(capsule(11, 7, 11, 1.5, 2.2) | capsule(21, 7, 21, 1.5, 2.2), '#78a5e3')])),
    'slumbler': ('#8fd0dc', '#6d8fd3', dict(face=['iiii....iiii', 'iEhi....iEhi', 'iEii....iEii', 'iiii....iiii', '............', 'mmmmmmmmmmmm', '.t.t.t.t.t.t'],
                                           keys={'i': '#ffd66b', 'E': '#1a1d38', 'h': '#ffffff', 'm': '#d9577f', 't': '#fff8ec'})),
    'sifter': ('#1fa3c1', '#f2cd98', dict(face=['.ee......ee.', '.ee......ee.', '............', 'tTtTtTtTtTtT', 'mmmmmmmmmmmm', 'TtTtTtTtTtTt'],
                                         keys={'e': '#d9f6ff', 't': '#eaf7ff', 'T': '#17328c', 'm': '#17328c'})),
    'enchoer': ('#a3dcc5', '#efe2b2', dict(face=['.ffffffff.', 'ffbffffbff', 'feefffeeff', 'ffffnnffff', 'ffffnnffff', 'fffmmmmfff', '.ffffffff.'],
                                          keys={'f': '#d5dfd4', 'b': '#4d6870', 'e': '#3a5059', 'n': '#aebdb4', 'm': '#6a807b'},
                                          top=[(capsule(9, 7, 4, 2, 1.3) | capsule(23, 7, 28, 2, 1.3) | capsule(6, 4, 4, 7, 1.1) | capsule(26, 4, 28, 7, 1.1), '#efe2b2')])),
    'riveter': ('#1d2b47', '#1fa39b', dict(face=['BBBBBBBBBB', 'ee..BB..ee', 'eeee..eeee', '.ee....ee.', '..........', '..tvvvvt..'],
                                          keys={'B': '#d9d4bf', 'e': '#a6fff5', 't': '#e9f4ef', 'v': '#07101c'},
                                          top=[(capsule(11, 7, 8, 1.5, 1.2) | capsule(21, 7, 24, 1.5, 1.2), '#1fa39b')])),
    'harmoner': ('#e8577f', '#ffd23f', dict(face=['e........e', 'e........e', '...bbbb...', '...bbbb...', '....BB....'], keys={'e': '#1a1830', 'b': '#ffd23f', 'B': '#c99a1f'},
                                           top=[(capsule(16, 6, 16, 0.8, 1.3) | capsule(13, 6, 11, 1.5, 1.1) | capsule(19, 6, 21, 1.5, 1.1), '#ff8a3d')])),
    'sift_sniffer': ('#8c2f23', '#3f9d80', dict(face=['..e....e..', '..........', '...nnnn...', '...nNNn...', '...nnnn...'], keys={'e': '#1a1a1a', 'n': '#f2b232', 'N': '#a86a12'},
                                               top=[(ellipse(16, 8, 9, 4), '#3f9d80')])),
    'dictator': ('#3a1322', '#b89a52', dict(face=['bb......bb', '.ee....ee.', '.eE....Ee.', '..........', '....nn....', 'mmmmmmmmmm', 'mfmmmmmmfm', '.mmmmmmmm.'],
                                           keys={'b': '#170810', 'e': '#04080c', 'E': '#2ef2e2', 'n': '#04080c', 'm': '#04080c', 'f': '#f4f0e5'},
                                           top=[(capsule(10, 8, 5, 3, 1.6) | capsule(5, 3, 7, 0.8, 1.1) | capsule(22, 8, 27, 3, 1.6) | capsule(27, 3, 25, 0.8, 1.1), '#e3ddcc')])),
    'enforcer': ('#22324a', '#e2d5b8', dict(face=['bbbbbbbbbbbb', '.vgv....vgv.', '.vgv....vgv.', '............', 'tvtvtvtvtvtv', 'vtvtvtvtvtvt'],
                                           keys={'b': '#e3ddcc', 'v': '#04080c', 'g': '#2ef2e2', 't': '#f2ecd8'},
                                           top=[(ellipse(16, 3.5, 7, 1.8), '#c9a24a')])),
    'resonator': ('#2a1f3a', '#3ff0e0', dict(face=['.LLLLLL.', 'lwwggwwl', '.wwgpww.', '..wwww..', '........', '.s.s.s.s', '.s.s.s.s', '.s.s.s.s'],
                                            keys={'L': '#1f2c40', 'l': '#04080c', 'w': '#f4f0e5', 'g': '#2ef2e2', 'p': '#04080c', 's': '#3ff0e0'})),
    'howler': ('#1a2433', '#d8d0b8', dict(face=['.bb....bb.', '.gg....gg.', '..........', '..dddddd..', '.dvvvvvvd.', '.dvvggvvd.', '.dvvvvvvd.', '..dddddd..'],
                                         keys={'b': '#b3ab96', 'g': '#2ef2e2', 'd': '#e3ddcc', 'v': '#04080c'},
                                         top=[(capsule(10, 7, 10, 1.5, 1.4) | capsule(16, 6, 16, 0.5, 1.4) | capsule(22, 7, 22, 1.5, 1.4), '#e3ddcc')])),
}


def all_items():
    out = {
        'siftite_sword': sword(), 'siftite_pickaxe': pickaxe(), 'siftite_axe': axe(), 'siftite_shovel': shovel(), 'siftite_hoe': hoe(),
        'siftite_spear': spear(), 'siftite_spear_in_hand': spear(),
        'siftite_helmet': helmet(), 'siftite_chestplate': chestplate(), 'siftite_leggings': leggings(), 'siftite_boots': boots(),
        'siftite_ingot': ingot(SIFTITE, SIFT_PINK), 'serbim_ingot': ingot(SERBIM), 'siftite_nugget': nugget(), 'raw_serbim': raw_serbim(),
        'chrome_pearl': pearl(), 'glowing_slime_ball': slime_ball(), 'star_shard': star_shard(), 'echo_seed': echo_seed(), 'choir_pod': choir_pod(),
        'pitcher_bulb': pitcher_bulb(), 'warden_core': warden_core(), 'thick_hide': hide(), 'sift_cake': cake(), 'dream_stew': stew(),
        'glowcap_skewer': skewer(), 'bulb_lantern': lantern(), 'chrome_bucket': bucket(CHROME), 'dream_journal_fragment': journal(),
        'sift_codex': codex(), 'music_disc_lullaby': disc(), 'soul_chime': chime(), 'drift_petals': petals(), 'glowbell_vine': glowbell(),
        'lullwood_door': door('#b1a9d4', '#8f86c0'), 'wishwood_door': door('#e6a0b8', '#c9738f'), 'siftite_upgrade_smithing_template': template(),
        'slingshot': slingshot(-1), 'slingshot_pulling_0': slingshot(0), 'slingshot_pulling_1': slingshot(1), 'slingshot_pulling_2': slingshot(2),
        'conductors_baton': baton(), 'conductors_staff': staff(),
    }
    for mob, (b, sp, e) in EGGS.items():
        out[f'{mob}_spawn_egg'] = egg(b, sp, e.get('face'), e.get('keys'), e.get('top'))
    return out
