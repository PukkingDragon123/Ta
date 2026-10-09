"""B3 Boss bars & Codex: original pixel art for the HUD and the Sift Codex.

Boss bars (textures/gui/bossbar/<name>.png, 256x64, 1 texel = 1 GUI pixel so the art sits at the
same pixel size as the font):
  frame 240x32 at (0, 0)  - themed end-caps, the rail and its ornaments; the track window is
                            x 29..211, y 15..21
  fill  182x6  at (0, 32) - a glossy, segmented light-grey strip, tinted in game by phase colour
  track 182x6  at (0, 38) - the empty track
  emblem 32x32 at (192, 32), its eye-glow layer 32x32 at (224, 32)
Codex:
  textures/gui/codex_fx.png 256x128: notes (0,0) 3 x 8x10, soul (32,0) 4 x 6x6 frames, ember (56,0) 3x3,
      sparkle (64,0) 7x7, clouds (0,16) 2 x 32x14, sculk veins (128,0) 64x64 bottom-left + (192,0) bottom-right,
      heart-vein glow (128,64) 64x64 + (192,64) 64x64.
"""
from __future__ import annotations

import math
import random

import numpy as np
from PIL import Image, ImageDraw

import rpgsprite as RS

# ---------------------------------------------------------------- palette shared by every plate and bar
INK = (26, 18, 12, 255)
SCULK, SCULK_L, SCULK_D = '#123a40', '#1f5a60', (6, 22, 26, 255)
GLOW = (41, 223, 235, 255)
GLOW_L = (190, 255, 255, 255)
BONE = '#d8cfb4'
GOLD = '#d8a838'


def _size(n):
    """Point rpgsprite's masks at an n x n canvas."""
    RS.N = n
    RS._yy, RS._xx = np.mgrid[0:n, 0:n]
    RS._cx, RS._cy = RS._xx + 0.5, RS._yy + 0.5


E, C, P, RR = RS.ellipse, RS.capsule, RS.poly, RS.rrect


def _px(img, pts, col):
    for x, y in pts:
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), col)


def _glow_eye(img, x, y, col, big=False):
    """A glowing eye: a hot core with a halo; returns the texels it lit (for the glow layer)."""
    core = [(x, y), (x + 1, y)] + ([(x, y + 1), (x + 1, y + 1)] if big else [])
    _px(img, core, col)
    _px(img, [core[0]], (255, 255, 255, 255))
    return core


def _bar_eye(img, x, y, col):
    """A boss's eye on its emblem: a dark socket, a 3x2 glowing slit with a white-hot centre."""
    _px(img, [(x + dx, y + dy) for dx in range(-1, 4) for dy in (-1, 2)] + [(x - 1, y), (x - 1, y + 1), (x + 3, y), (x + 3, y + 1)], (8, 6, 12, 255))
    _px(img, [(x + dx, y + dy) for dx in range(3) for dy in range(2)], col)
    _px(img, [(x + 1, y)], (255, 255, 255, 255))


# ================================================================== boss bars

def _mirror_cap(cap):
    return cap.transpose(Image.FLIP_LEFT_RIGHT)


def _rail(d, metal, dark):
    """The rail: a bevelled bar wrapping the track window."""
    d.rounded_rectangle((30, 12, 225, 24), radius=4, fill=dark, outline=INK)
    d.line((33, 13, 222, 13), fill=metal[3])
    d.line((33, 14, 222, 14), fill=metal[2])
    d.line((33, 22, 222, 22), fill=metal[0])
    d.line((33, 23, 222, 23), fill=INK)
    d.rectangle((36, 14, 220, 22), outline=metal[1])
    d.rectangle((37, 15, 219, 20), fill=(8, 8, 12, 255))


def _caps_thumper():
    """A bronze cannon on a hexagonal shell plate, a sculk vent glowing in the plate."""
    _size(40)
    sp = RS.Sprite()
    sp.add(P([(18, 6), (32, 6), (39, 20), (32, 34), (18, 34), (11, 20)]), '#3f5a34', 'bevel', depth=3)
    sp.add(P([(21, 10), (30, 10), (34, 20), (30, 30), (21, 30), (17, 20)]), '#6f8a4a', 'dome')
    sp.add(E(34, 30, 3, 2), '#16282c', 'flat')                               # the vent
    sp.add(C(28, 19, 7, 17, 5.0), '#a8723a', 'dome')                          # barrel
    sp.add(E(23, 18, 2.2, 5.6), '#7a4e24', 'bevel', depth=1)                  # reinforcing rings
    sp.add(E(14, 18, 2.0, 5.4), '#7a4e24', 'bevel', depth=1)
    sp.add(E(6, 17, 3.6, 6.2), '#c8904a', 'dome')                             # muzzle
    sp.add(C(25, 26, 20, 33, 3.0) | E(20, 33, 3.4, 3.4), '#4a3020', 'dome')   # carriage wheel
    img = sp.render()
    _px(img, [(5, y) for y in range(15, 20)] + [(6, y) for y in range(15, 20)], (12, 8, 6, 255))
    _px(img, [(33, 30), (34, 30), (35, 30)], GLOW)
    _px(img, [(34, 29)], GLOW_L)
    _px(img, [(20, 33)], (200, 160, 100, 255))
    return img


def _caps_weaver():
    """Three jointed spider legs reaching out, a web strung between them, a tuning peg."""
    _size(40)
    sp = RS.Sprite()
    for (x0, y0, x1, y1, x2, y2) in ((34, 16, 22, 5, 6, 9), (34, 20, 18, 17, 2, 22), (34, 24, 21, 33, 7, 35)):
        sp.add(C(x0, y0, x1, y1, 2.3), '#3a2450', 'dome')
        sp.add(E(x1, y1, 2.6, 2.6), '#5a3a78', 'dome')
        sp.add(C(x1, y1, x2, y2, 1.5), BONE, 'dome')
    sp.add(E(35, 20, 5.5, 7), '#4a2c66', 'dome')
    sp.add(C(37, 12, 37, 8, 1.6), GOLD, 'dome')                                # tuning peg
    img = sp.render()
    d = ImageDraw.Draw(img)
    web = (232, 228, 244, 150)
    for a, b in (((6, 9), (2, 22)), ((2, 22), (7, 35)), ((12, 8), (8, 21)), ((8, 21), (12, 33))):
        d.line((*a, *b), fill=web)
    for p in ((6, 9), (2, 22), (7, 35)):
        d.line((22, 20, *p), fill=web)
    _px(img, [(35, 18), (37, 18)], (216, 160, 255, 255))
    return img


def _caps_conductor():
    """A baton with a gold grip, two notes riding it."""
    _size(40)
    sp = RS.Sprite()
    sp.add(C(36, 24, 3, 12, 1.3), '#f2ede0', 'dome')                         # baton
    sp.add(C(38, 25, 29, 21, 2.6), GOLD, 'dome')                             # grip
    sp.add(E(27, 21, 2.2, 3.4), '#f0d070', 'dome')                           # cork collar
    sp.add(E(9, 29, 3.4, 2.6, -20), '#2ec9c0', 'dome')                        # notes
    sp.add(C(12, 28, 12, 15, 0.8), '#2ec9c0', 'flat')
    sp.add(E(18, 31, 3.0, 2.3, -20), '#2ec9c0', 'dome')
    sp.add(C(20, 30, 20, 17, 0.8), '#2ec9c0', 'flat')
    sp.add(C(12, 15, 20, 17, 1.0), '#2ec9c0', 'flat')
    img = sp.render()
    _px(img, [(3, 12), (4, 12)], (255, 255, 255, 255))
    return img


def _caps_dragon():
    """A membrane wing: four bone fingers ending in claws, the skin between them veined."""
    _size(40)
    sp = RS.Sprite()
    sp.add(P([(39, 16), (3, 5), (8, 14), (1, 20), (8, 24), (3, 32), (14, 29), (17, 37), (26, 28), (39, 24)]), '#7a3fa8', 'bevel', depth=2)
    sp.add(P([(36, 18), (14, 14), (12, 21), (18, 27), (34, 23)]), '#9a5ac8', 'flat', outline=False)
    for (x1, y1) in ((3, 5), (1, 20), (3, 32), (17, 37)):
        sp.add(C(38, 18, x1, y1, 1.1), '#2a1838', 'dome')
    sp.add(E(37, 19, 3.5, 4.5), '#3a2450', 'dome')
    img = sp.render()
    _px(img, [(3, 5), (2, 5), (1, 20), (0, 20), (3, 32), (2, 33), (17, 37)], (240, 230, 250, 255))
    return img


def _caps_wither():
    """A tapering spine of black vertebrae with thorny processes and wisps of soot."""
    _size(40)
    sp = RS.Sprite()
    for i, x in enumerate((34, 26, 19, 13, 8)):
        r = 4.6 - i * 0.7
        sp.add(C(x, 20 - r - 1, x - 3, 20 - r - 6 + i, 1.0), '#26242c', 'dome')
        sp.add(C(x, 20 + r + 1, x - 3, 20 + r + 6 - i, 1.0), '#26242c', 'dome')
        sp.add(E(x, 20, r, r + 0.8), '#4a4850', 'dome')
    sp.add(E(4, 20, 2.2, 2.6), '#3a3840', 'dome')
    img = sp.render()
    rnd = random.Random(9)
    for _ in range(14):
        _px(img, [(rnd.randrange(0, 30), rnd.choice((rnd.randrange(4, 11), rnd.randrange(30, 37))))], (40, 36, 48, 200))
    return img


def _caps_raid():
    """A loaded crossbow with an emerald set in the stock, a war-banner ribbon tied on."""
    _size(40)
    sp = RS.Sprite()
    sp.add(C(39, 20, 9, 20, 2.2), '#7a5530', 'dome')
    sp.add(P([(11, 4), (16, 4), (11, 20), (16, 36), (11, 36), (6, 20)]), '#5a3a22', 'bevel', depth=1)
    sp.add(C(4, 20, 20, 20, 0.9), '#c8c8c8', 'flat')
    sp.add(P([(0, 20), (5, 16), (5, 24)]), '#9aa0a8', 'bevel', depth=1)
    sp.add(E(27, 20, 3.4, 3.0), '#2ab060', 'dome')
    sp.add(P([(30, 23), (35, 23), (36, 34), (33, 31), (30, 35)]), '#a02424', 'bevel', depth=1)
    img = sp.render()
    d = ImageDraw.Draw(img)
    d.line((13, 5, 13, 35), fill=(222, 214, 192, 255))
    _px(img, [(26, 19)], (220, 255, 230, 255))
    return img


def _emblem(kind):
    """32x32 boss face and its eye-glow layer."""
    _size(32)
    sp = RS.Sprite()
    eyes = []
    if kind == 'thumper':
        sp.add(E(16, 19, 15, 10), '#3a5a48', 'dome')                             # shell rim behind
        for x in (5, 16, 27):
            sp.add(P([(x - 4, 12), (x + 4, 12), (x + 6, 17), (x + 4, 22), (x - 4, 22), (x - 6, 17)]), '#5a7a50', 'bevel', depth=2)
        sp.add(E(16, 20, 8.5, 8), BONE, 'dome')                                  # bony head
        sp.add(P([(12, 24), (20, 24), (16, 30)]), '#b8a880', 'bevel', depth=1)   # beak
        img = sp.render(glints=False)
        _px(img, [(14, 27), (15, 27), (16, 27), (17, 27)], (40, 30, 20, 255))
        eyes = [(10, 18), (19, 18)]
        for x, y in eyes:
            _bar_eye(img, x, y, (46, 242, 226, 255))
            _px(img, [(x - 1, y - 2), (x, y - 2), (x + 1, y - 2), (x + 2, y - 2), (x + 3, y - 2)], (90, 80, 56, 255))
        _px(img, [(4, 24), (5, 24), (27, 24), (28, 24), (16, 12)], GLOW)
    elif kind == 'weaver':
        sp.add(E(16, 17, 13, 11), '#3a2450', 'dome')
        sp.add(C(9, 26, 6, 31, 1.6), BONE, 'dome')                               # fangs
        sp.add(C(23, 26, 26, 31, 1.6), BONE, 'dome')
        sp.add(E(16, 9, 8, 4), '#5a3a78', 'bevel', depth=2)
        img = sp.render(glints=False)
        eyes = [(10, 15), (19, 15)]
        for x, y in eyes:
            _bar_eye(img, x, y, (216, 160, 255, 255))
        for (x, y) in ((7, 12), (24, 12), (12, 20), (19, 20), (5, 17), (26, 17)):
            _glow_eye(img, x, y, (216, 160, 255, 255))
            eyes.append((x, y))
        d = ImageDraw.Draw(img)
        for i, x in enumerate((13, 15, 17, 19)):
            d.line((x, 23, x, 31), fill=(127, 247, 255, 150 if i % 2 else 210))    # strings across the jaw
    elif kind == 'conductor':
        sp.add(C(9, 11, 4, 2, 2.2) | C(4, 2, 1, 5, 1.2), '#e3ddcc', 'dome')      # tall horns
        sp.add(C(23, 11, 28, 2, 2.2) | C(28, 2, 31, 5, 1.2), '#e3ddcc', 'dome')
        sp.add(C(7, 16, 1, 13, 1.6), '#c8c0aa', 'dome')                          # side horns
        sp.add(C(25, 16, 31, 13, 1.6), '#c8c0aa', 'dome')
        sp.add(E(16, 18, 10, 12), '#5a1a30', 'dome')
        sp.add(P([(8, 12), (24, 12), (22, 15), (10, 15)]), '#3a0e1e', 'bevel', depth=1)   # mask brow
        img = sp.render(glints=False)
        eyes = [(10, 16), (19, 16)]
        for x, y in eyes:
            _bar_eye(img, x, y, (46, 242, 226, 255))
        d = ImageDraw.Draw(img)
        d.line((11, 24, 21, 24), fill=(4, 8, 12, 255))
        d.line((12, 25, 20, 25), fill=(4, 8, 12, 255))
        for x in (12, 14, 18, 20):
            d.point((x, 25), fill=(244, 240, 229, 255))
    elif kind == 'dragon':
        sp.add(C(9, 10, 3, 1, 1.8) | C(23, 10, 29, 1, 1.8), '#1a1020', 'dome')
        sp.add(E(16, 16, 11, 9), '#241830', 'dome')
        sp.add(RR(10, 18, 22, 31, 3), '#2e2040', 'dome')
        img = sp.render(glints=False)
        eyes = [(9, 14), (20, 14)]
        for x, y in eyes:
            _bar_eye(img, x, y, (230, 120, 255, 255))
        _px(img, [(13, 28), (18, 28)], (10, 6, 14, 255))                         # nostrils
        _px(img, [(11, 30), (13, 31), (18, 31), (20, 30)], (240, 236, 250, 255))  # teeth
    elif kind == 'wither':
        sp.add(E(16, 15, 11, 11), '#3a3840', 'dome')
        sp.add(RR(9, 18, 23, 29, 3), '#323038', 'dome')
        sp.add(C(4, 10, 1, 4, 1.2) | C(28, 10, 31, 4, 1.2), '#26242c', 'dome')
        img = sp.render(glints=False)
        eyes = [(10, 14), (19, 14)]
        for x, y in eyes:
            _bar_eye(img, x, y, (200, 216, 255, 255))
        _px(img, [(15, 20), (16, 20), (15, 21), (16, 21)], (10, 10, 14, 255))
        for x in range(11, 22, 2):
            _px(img, [(x, 25), (x, 26)], (10, 10, 14, 255))
    else:  # raid: an illager's scowl in front of a war banner
        sp.add(P([(5, 1), (27, 1), (27, 30), (16, 24), (5, 30)]), '#a02424', 'bevel', depth=2)
        sp.add(RR(8, 5, 24, 25, 4), '#9aa29a', 'dome')
        sp.add(C(16, 15, 16, 22, 2.4), '#8a928a', 'dome')                        # the big nose
        img = sp.render(glints=False)
        _px(img, [(x, 11) for x in range(9, 24)] + [(x, 12) for x in range(10, 23)], (34, 34, 38, 255))   # unibrow
        eyes = [(10, 13), (19, 13)]
        for x, y in eyes:
            _bar_eye(img, x, y, (255, 72, 56, 255))
        _px(img, [(13, 23), (14, 23), (17, 23), (18, 23)], (40, 40, 44, 255))
        d = ImageDraw.Draw(img)
        d.line((6, 2, 26, 2), fill=(240, 220, 120, 255))                       # banner trim
        d.line((14, 27, 16, 25), fill=(240, 220, 120, 255))
        d.line((16, 25, 18, 27), fill=(240, 220, 120, 255))
    glow = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    for x, y in eyes:
        c = img.getpixel((x, y + 1)) if img.getpixel((x, y))[:3] == (255, 255, 255) else img.getpixel((x, y))
        for dx in range(-2, 5):
            for dy in range(-2, 4):
                xx, yy = x + dx, y + dy
                if 0 <= xx < 32 and 0 <= yy < 32:
                    core = 0 <= dx <= 2 and 0 <= dy <= 1
                    a = 255 if core else (120 if -1 <= dx <= 3 and -1 <= dy <= 2 else 50)
                    if glow.getpixel((xx, yy))[3] < a:
                        glow.putpixel((xx, yy), (min(255, c[0] + 70), min(255, c[1] + 70), min(255, c[2] + 70), a))
    return img, glow


BARS = {
    # name: (rail metal ramp, rail dark, cap builder, emblem kind, rail ornament)
    'thumper': (['#2c3a28', '#4a5e3c', '#6f8a4a', '#9ab070'], '#1c2618', _caps_thumper, 'thumper', 'vents'),
    'strummer': (['#24163a', '#3a2450', '#5a3a78', '#8a68a8'], '#160c22', _caps_weaver, 'weaver', 'strings'),
    'dictator': (['#2a0c18', '#5a1a30', '#8a3048', GOLD], '#180610', _caps_conductor, 'conductor', 'notes'),
    'ender_dragon': (['#1a1020', '#3a2450', '#6a3a90', '#a070d0'], '#10081a', _caps_dragon, 'dragon', 'scales'),
    'wither': (['#1a1a20', '#323038', '#4a4850', '#7a7884'], '#101014', _caps_wither, 'wither', 'thorns'),
    'raid': (['#3a1010', '#6a1a1a', '#a02424', '#e0b050'], '#200808', _caps_raid, 'raid', 'banner'),
}


def _rgb(c):
    return (*RS.hexc(c), 255) if isinstance(c, str) else c


def _ornaments(d, kind, rnd):
    if kind == 'vents':
        for x in range(44, 212, 22):
            if 104 < x < 150:
                continue
            for y0 in (10, 22):
                d.polygon([(x, y0 + 2), (x + 3, y0), (x + 9, y0), (x + 12, y0 + 2), (x + 9, y0 + 4), (x + 3, y0 + 4)], fill=(90, 116, 70, 255), outline=INK)
            d.point((x + 6, 11), fill=GLOW)
            d.point((x + 6, 24), fill=GLOW)
    elif kind == 'strings':
        for i, y in enumerate((11, 25)):
            d.line((32, y, 223, y), fill=(127, 247, 255, 200) if i == 0 else (90, 180, 200, 200))
        for cx in (60, 196):
            for r in (3, 6):
                d.ellipse((cx - r, 18 - r - 6, cx + r, 18 + r - 6), outline=(230, 226, 240, 120))
    elif kind == 'notes':
        for x in range(44, 212, 20):
            if 104 < x < 150:
                continue
            y = 6 + (x // 20) % 2 * 2
            d.ellipse((x, y + 3, x + 3, y + 5), fill=(46, 242, 226, 255))
            d.line((x + 3, y, x + 3, y + 4), fill=(46, 242, 226, 255))
    elif kind == 'scales':
        for x in range(36, 220, 6):
            d.polygon([(x, 12), (x + 3, 9), (x + 6, 12)], fill=(58, 36, 80, 255), outline=INK)
    elif kind == 'thorns':
        for x in range(38, 218, 9):
            up = rnd.random() < 0.5
            y0 = 12 if up else 24
            d.line((x, y0, x + 2, y0 + (-4 if up else 4)), fill=(60, 58, 66, 255))
            d.point((x + 3, y0 + (-5 if up else 5)), fill=(90, 88, 96, 255))
    elif kind == 'banner':
        for x in range(42, 214, 14):
            d.polygon([(x, 24), (x + 8, 24), (x + 4, 28)], fill=(160, 36, 36, 255), outline=INK)
            d.point((x + 4, 25), fill=(240, 220, 120, 255))


def boss_bars(out):
    for name, (metal, dark, caps, kind, orn) in BARS.items():
        t = Image.new('RGBA', (256, 64), (0, 0, 0, 0))
        d = ImageDraw.Draw(t)
        metal = [_rgb(c) for c in metal]
        _rail(d, metal, _rgb(dark))
        _ornaments(d, orn, random.Random(len(name)))
        cap = caps().crop((0, 4, 40, 36))
        t.alpha_composite(cap, (0, 0))
        t.alpha_composite(_mirror_cap(cap), (216, 0))
        # the emblem's backing: a dark medallion the face sits in (drawn under the emblem in game)
        d.ellipse((112, 0, 143, 31), fill=_rgb(dark), outline=INK)
        d.arc((113, 1, 142, 30), 200, 340, fill=metal[3])
        d.arc((113, 1, 142, 30), 20, 160, fill=metal[0])
        # fill: glossy light strip, segment notches every 10 px (tinted in game)
        rows = [(255, 255, 255), (236, 236, 236), (206, 206, 206), (178, 178, 178), (150, 150, 150), (110, 110, 110)]
        for y, c in enumerate(rows):
            d.line((0, 32 + y, 181, 32 + y), fill=(*c, 255))
        for x in range(9, 182, 10):
            d.line((x, 33, x, 37), fill=(96, 96, 96, 255))
            d.point((x + 1, 33), fill=(255, 255, 255, 255))
        for y in range(6):
            d.line((0, 38 + y, 181, 38 + y), fill=(16, 14, 20, 255) if 0 < y < 5 else (6, 6, 8, 255))
        for x in range(9, 182, 10):
            d.line((x, 39, x, 42), fill=(26, 24, 32, 255))
        em, glow = _emblem(kind)
        t.alpha_composite(em, (192, 32))
        t.alpha_composite(glow, (224, 32))
        out(f'gui/bossbar/{name}', t)
    _size(32)


# F3 Knowledge and lore: the creature plates are gone - the Knowledge Book describes its creatures in words.


def codex_fx(out):
    t = Image.new('RGBA', (256, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(t)
    W = (255, 255, 255, 255)
    # notes: eighth, beamed pair, quarter (white, tinted in game)
    d.ellipse((0, 6, 3, 9), fill=W); d.line((3, 0, 3, 7), fill=W); d.line((4, 1, 5, 3), fill=W)
    d.ellipse((8, 7, 10, 9), fill=W); d.ellipse((13, 6, 15, 8), fill=W); d.line((10, 1, 10, 8), fill=W); d.line((15, 0, 15, 7), fill=W)
    d.line((10, 1, 15, 0), fill=W); d.line((10, 2, 15, 1), fill=W)
    d.ellipse((16, 6, 19, 9), fill=W); d.line((19, 0, 19, 7), fill=W)
    # soul flame frames (6x6)
    for f in range(4):
        x0 = 32 + f * 6
        h = 3 + (f % 2)
        d.ellipse((x0 + 1, 2, x0 + 4, 5), fill=(110, 240, 255, 255))
        d.line((x0 + 2 + (f & 1), 5 - h, x0 + 2 + (f & 1), 3), fill=(190, 255, 255, 255))
        d.point((x0 + 2, 3), fill=W)
    # ember 3x3
    d.point((57, 0), fill=(255, 200, 90, 255)); d.line((56, 1, 58, 1), fill=(255, 140, 50, 255)); d.point((57, 1), fill=(255, 240, 180, 255))
    d.point((57, 2), fill=(200, 60, 30, 255))
    # sparkle 7x7
    d.line((67, 0, 67, 6), fill=W); d.line((64, 3, 70, 3), fill=W); d.point((66, 2), fill=W); d.point((68, 4), fill=W)
    d.point((66, 4), fill=W); d.point((68, 2), fill=W)
    # clouds 32x14 (two)
    for i in range(2):
        x0 = i * 32
        rnd = random.Random(i + 11)
        for _ in range(6):
            cx, cy, r = x0 + rnd.randrange(7, 25), 16 + rnd.randrange(5, 9), rnd.randrange(3, 6)
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(255, 255, 255, 255))
        d.rectangle((x0 + 4, 16 + 9, x0 + 28, 16 + 12), fill=(255, 255, 255, 255))
        for x in range(x0 + 4, x0 + 29):
            d.point((x, 16 + 12), fill=(214, 226, 240, 255))
    # sculk veins creeping in from the bottom-left corner (and mirrored), and their glow
    veins = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    glow = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    vd, gd = ImageDraw.Draw(veins), ImageDraw.Draw(glow)
    rnd = random.Random(5)

    def branch(x, y, ang, length, w):
        for _ in range(length):
            nx, ny = x + math.cos(ang) * 2, y - math.sin(ang) * 2
            vd.line((x, y, nx, ny), fill=(18, 58, 64, 230), width=w)
            if rnd.random() < 0.3:
                gd.point((int(nx), int(ny)), fill=GLOW)
            x, y = nx, ny
            ang += rnd.uniform(-0.45, 0.45)
            if rnd.random() < 0.18 and w > 1:
                branch(x, y, ang + rnd.choice((-0.7, 0.7)), length // 2, w - 1)
            if not (0 <= x < 64 and 0 <= y < 64):
                return
    for a in (0.12, 0.55, 1.0, 1.42):
        branch(0, 63, a, 30, 3)
    for _ in range(6):                                       # sculk nodules along the veins
        x, y = rnd.randrange(4, 30), rnd.randrange(34, 62)
        if veins.getpixel((x, y))[3]:
            vd.ellipse((x - 1, y - 1, x + 1, y + 1), fill=(24, 74, 80, 255))
            gd.point((x, y), fill=GLOW_L)
    vd.ellipse((-6, 57, 6, 69), fill=(10, 40, 46, 255))
    gd.ellipse((-3, 60, 3, 66), fill=GLOW)
    t.alpha_composite(veins, (128, 0))
    t.alpha_composite(veins.transpose(Image.FLIP_LEFT_RIGHT), (192, 0))
    t.alpha_composite(glow, (128, 64))
    t.alpha_composite(glow.transpose(Image.FLIP_LEFT_RIGHT), (192, 64))
    out('gui/codex_fx', t)


def textures(out):
    boss_bars(out)
    codex_fx(out)
