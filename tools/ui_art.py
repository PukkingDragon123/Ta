"""B3 Boss bars & Codex: original pixel art for the HUD and the Sift Codex.

Boss bars (textures/gui/bossbar/<name>.png, 256x64, 1 texel = 1 GUI pixel so the art sits at the
same pixel size as the font):
  frame 240x32 at (0, 0)  - themed end-caps, the rail and its ornaments; the track window is
                            x 29..211, y 15..21
  fill  182x6  at (0, 32) - a glossy, segmented light-grey strip, tinted in game by phase colour
  track 182x6  at (0, 38) - the empty track
  emblem 32x32 at (192, 32), its eye-glow layer 32x32 at (224, 32)
Codex:
  textures/gui/codex_portraits.png 512x256: 48x48 specimen plates, 10 per row (index order = PORTRAITS)
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


# ================================================================== codex specimen plates

# portrait order (index = position in the 10-wide atlas); 'bulb_white' is the White Forest Bulb
PORTRAITS = ['bulb', 'bulb_white', 'harmoner', 'sniffer', 'enchoer', 'soul_golem', 'nib', 'slumbler', 'sifter',
             'stomper', 'sky_whale', 'fanfare_eel', 'kazoo_fish', 'tubafish', 'caravan', 'gobbler', 'swifter', 'jailer', 'sculkling',
             'dictator', 'thumper', 'strummer', 'strumling', 'sculk_parasite']

# plate backdrops: (top, bottom) per habitat
BACK = {'meadow': ('#b8d8a0', '#6f9a58'), 'sculk': ('#1f4a50', '#081e22'), 'sky': ('#cfe6f8', '#8ab8e0'), 'sea': ('#5a9ac8', '#1a3a68'),
        'sand': ('#f0dca8', '#c8a868'), 'cave': ('#4a4258', '#1a1622'), 'chrome': ('#e8d0f8', '#8ad8e8'), 'snow': ('#f4f6fa', '#bcc8d8'),
        'boss': ('#5a2030', '#1a0810'), 'deep': ('#16304a', '#040a16')}
HABITAT = {'bulb': 'meadow', 'bulb_white': 'snow', 'harmoner': 'meadow', 'sniffer': 'meadow', 'enchoer': 'chrome', 'soul_golem': 'cave',
           'nib': 'meadow', 'slumbler': 'chrome', 'sifter': 'sand', 'stomper': 'meadow', 'sky_whale': 'sky',
           'fanfare_eel': 'sea', 'kazoo_fish': 'sea', 'tubafish': 'sea', 'caravan': 'cave', 'gobbler': 'deep', 'swifter': 'snow',
           'jailer': 'sculk', 'sculkling': 'sculk', 'dictator': 'boss', 'thumper': 'boss', 'strummer': 'boss', 'strumling': 'sculk',
           'sculk_parasite': 'sculk'}


def _backdrop(habitat, rnd):
    top, bot = (np.array(RS.hexc(c), float) for c in BACK[habitat])
    img = Image.new('RGBA', (48, 48))
    for y in range(48):
        for x in range(48):
            k = y / 47
            c = top * (1 - k) + bot * k
            # 4-level ordered dither towards a vignette
            v = math.hypot(x - 23.5, y - 20) / 34
            bayer = ((x & 1) * 2 + (y & 1) * 3 + ((x >> 1) & 1)) % 4 / 4
            if v + bayer * 0.18 > 0.82:
                c = c * 0.8
            img.putpixel((x, y), (int(c[0]), int(c[1]), int(c[2]), 255))
    d = ImageDraw.Draw(img)
    if habitat == 'sky':
        for cx, cy in ((10, 34), (36, 38)):
            d.ellipse((cx - 8, cy - 3, cx + 8, cy + 3), fill=(250, 252, 255, 255))
    elif habitat in ('sculk', 'deep'):
        for _ in range(9):
            d.point((rnd.randrange(3, 45), rnd.randrange(3, 45)), fill=GLOW)
    elif habitat == 'chrome':
        for i in range(48):
            h = (i / 48 + 0.1) % 1
            import colorsys
            r, g, b = colorsys.hsv_to_rgb(h, 0.45, 1.0)
            d.point((i, 40 + int(2 * math.sin(i / 5))), fill=(int(r * 255), int(g * 255), int(b * 255), 255))
    elif habitat == 'sea':
        for _ in range(7):
            x, y = rnd.randrange(4, 44), rnd.randrange(4, 30)
            d.ellipse((x, y, x + 2, y + 2), outline=(200, 230, 255, 200))
    elif habitat == 'snow':
        for _ in range(14):
            d.point((rnd.randrange(2, 46), rnd.randrange(2, 46)), fill=(255, 255, 255, 255))
    # ground shadow
    sh = Image.new('RGBA', (48, 48), (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse((10, 39, 38, 44), fill=(0, 0, 0, 70))
    img.alpha_composite(sh)
    return img


def _plate_frame(img):
    d = ImageDraw.Draw(img)
    d.rectangle((0, 0, 47, 47), outline=(58, 40, 22, 255))
    d.rectangle((1, 1, 46, 46), outline=(184, 154, 82, 255))
    d.line((2, 2, 45, 2), fill=(236, 214, 140, 255))
    for x, y in ((0, 0), (44, 0), (0, 44), (44, 44)):
        d.rectangle((x, y, x + 3, y + 3), fill=(220, 194, 122, 255), outline=(58, 40, 22, 255))
    return img


def _creature(name):
    """The creature on a 48x48 canvas, and its glowing details."""
    _size(48)
    s = RS.Sprite()
    ex = []   # (x, y, colour, big)
    if name in ('bulb', 'bulb_white'):
        white = name == 'bulb_white'
        body, ear = ('#f4f4fa', '#f8c8d8') if white else ('#5fb8e8', '#9adcf8')
        s.add(C(17, 18, 13, 3, 3.2), body).add(C(31, 18, 35, 3, 3.2), body)
        s.add(C(17, 16, 14, 6, 1.4), ear, 'flat', outline=False).add(C(31, 16, 34, 6, 1.4), ear, 'flat', outline=False)
        s.add(E(16, 40, 3.5, 2.5) | E(32, 40, 3.5, 2.5), body)
        s.add(RR(11, 16, 37, 40, 8), body)
        s.add(E(20, 15, 2.2, 2.2), '#f070a0').add(E(27, 14, 2.0, 2.0), '#f8d040').add(E(33, 16, 1.8, 1.8), '#b080f0')
        img = s.render()
        for x in (18, 29):
            _px(img, [(x, 26), (x + 1, 26), (x, 27), (x + 1, 27), (x, 28), (x + 1, 28)], (20, 24, 40, 255))
            _px(img, [(x, 26)], (255, 255, 255, 255))
        _px(img, [(23, 31), (24, 31), (25, 31)], (40, 30, 60, 255))
        _px(img, [(15, 30), (33, 30)], (255, 150, 180, 255))
    elif name == 'harmoner':
        s.add(C(10, 30, 4, 36, 2.5), '#8a3a70')                          # tail
        s.add(E(22, 29, 11, 9), '#d070a0')
        s.add(E(30, 19, 7, 7), '#e088b8')
        s.add(P([(36, 18), (43, 20), (36, 22)]), GOLD, 'bevel', depth=1)
        s.add(E(20, 30, 7, 5, -20), '#a04a80')                           # wing
        s.add(C(29, 13, 27, 6, 1.4) | C(31, 13, 33, 7, 1.2), '#f0a0d0')
        s.add(C(19, 38, 19, 43, 1.0) | C(25, 38, 25, 43, 1.0), '#c09040', 'flat')
        img = s.render()
        _px(img, [(32, 18), (33, 18), (32, 19), (33, 19)], (20, 10, 20, 255))
        _px(img, [(32, 18)], (255, 255, 255, 255))
        d = ImageDraw.Draw(img)
        d.ellipse((40, 8, 43, 10), fill=(255, 255, 255, 255))
        d.line((43, 3, 43, 9), fill=(255, 255, 255, 255))
    elif name == 'sniffer':
        s.add(E(22, 28, 16, 11), '#a8402c')
        s.add(E(22, 19, 12, 5), '#5a8a3a')                               # mossy back
        s.add(C(36, 28, 44, 32, 3.6), '#c05038')                         # the long nose
        for x in (12, 18, 26, 32):
            s.add(C(x, 36, x, 42, 2.2), '#7a2c20')
        img = s.render()
        _px(img, [(34, 25), (35, 25)], (20, 10, 10, 255))
        _px(img, [(14, 17), (20, 15), (27, 16)], (240, 100, 140, 255))
    elif name == 'enchoer':
        s.add(E(20, 32, 12, 7), '#e8e4f0')
        s.add(C(28, 28, 34, 10, 3.2), '#e8e4f0')
        s.add(E(36, 9, 5, 3.5, 20), '#f4f2fa')
        for x in (12, 16, 24, 28):
            s.add(C(x, 36, x, 43, 1.6), '#c8c4d8')
        img = s.render()
        _px(img, [(37, 8), (38, 8)], (60, 50, 90, 255))
        for i, (x, y) in enumerate(((14, 30), (18, 29), (22, 30), (26, 29), (31, 22), (32, 17))):
            ex.append((x, y, (46, 242, 226, 255), False))
    elif name == 'soul_golem':
        s.add(C(16, 38, 16, 43, 2.6) | C(32, 38, 32, 43, 2.6), '#4a3e34')
        s.add(E(24, 27, 13, 13), '#6a5a4a')
        s.add(E(24, 14, 6, 3), '#8a7a66', 'bevel', depth=2)
        s.add(C(11, 28, 8, 36, 2.4) | C(37, 28, 40, 36, 2.4), '#5a4c3e')
        img = s.render()
        for x in (18, 28):
            _px(img, [(x - 1, 23), (x + 2, 23), (x - 1, 26), (x + 2, 26), (x - 1, 24), (x - 1, 25), (x + 2, 24), (x + 2, 25)], (34, 28, 22, 255))
            ex.append((x, 24, (110, 240, 255, 255), True))
        _px(img, [(22, 32), (23, 32), (24, 32), (25, 32)], (34, 28, 22, 255))
    elif name == 'nib':
        for (cx, cy, k, hue) in ((24, 22, 1.0, '#f8b0d8'), (11, 34, 0.7, '#f8e080'), (37, 33, 0.7, '#a8e8f8')):
            s.add(E(cx - 5 * k, cy - 3 * k, 5 * k, 4 * k, -25) | E(cx + 5 * k, cy - 3 * k, 5 * k, 4 * k, 25), hue, 'bevel', depth=1)
            s.add(E(cx - 4 * k, cy + 3 * k, 3 * k, 2.5 * k) | E(cx + 4 * k, cy + 3 * k, 3 * k, 2.5 * k), hue, 'bevel', depth=1)
            s.add(C(cx, cy - 4 * k, cx, cy + 5 * k, 1.4 * k + 0.4), '#fff8e8')
        img = s.render()
        for (x, y) in ((24, 17), (11, 31), (37, 30)):
            ex.append((x, y, (255, 250, 220, 255), False))
        rnd = random.Random(3)
        for _ in range(10):
            _px(img, [(rnd.randrange(4, 44), rnd.randrange(6, 44))], (255, 250, 200, 255))
    elif name == 'slumbler':
        s.add(E(24, 30, 19, 12), '#8a7ab8')
        s.add(E(24, 24, 15, 6), '#a898d0', outline=False)
        s.add(C(9, 40, 9, 43, 2.8) | C(39, 40, 39, 43, 2.8), '#6a5a98')
        img = s.render()
        d = ImageDraw.Draw(img)
        d.line((9, 33, 39, 33), fill=(40, 24, 60, 255))
        d.line((11, 34, 37, 34), fill=(120, 70, 110, 255))
        for x in (15, 30):
            d.line((x, 25, x + 4, 25), fill=(40, 24, 60, 255))
        d.text((36, 4), 'z', fill=(255, 255, 255, 255))
        _px(img, [(31, 14), (32, 14), (33, 14), (32, 15), (31, 16), (32, 16), (33, 16)], (255, 255, 255, 255))
    elif name == 'sifter':
        for (x0, y0, x1, y1) in ((14, 32, 6, 42), (20, 34, 16, 43), (28, 34, 32, 43), (34, 32, 42, 42)):
            s.add(C(x0, y0, x1, y1, 1.6), '#d8cfb4')
        s.add(E(24, 30, 14, 7), '#c8a868')
        s.add(E(24, 23, 15, 6), '#e0c890', 'bevel', depth=2)                 # the lid
        s.add(C(24, 18, 24, 10, 0.9), '#8a6a3a', 'flat')
        img = s.render()
        ex.append((23, 8, (255, 230, 120, 255), True))
        d = ImageDraw.Draw(img)
        d.line((12, 30, 36, 30), fill=(60, 40, 20, 255))
        for x in range(13, 36, 3):
            d.point((x, 31), fill=(240, 236, 220, 255))
    elif name == 'stomper':
        s.add(E(22, 30, 15, 10), '#7a8a9a')
        s.add(C(34, 30, 40, 40, 2.6) | C(40, 40, 43, 38, 1.8), '#8a9aaa')      # trunk
        for x in (12, 18, 26, 31):
            s.add(C(x, 36, x, 43, 2.6), '#5a6a7a')
        s.add(E(22, 19, 13, 5), '#5a9a48')
        img = s.render()
        for (x, y) in ((31, 26), (35, 26), (32, 29), (36, 29)):
            _px(img, [(x, y), (x + 1, y)], (20, 24, 34, 255))
        _px(img, [(12, 15), (18, 13), (26, 14), (31, 16)], (240, 120, 160, 255))
        _px(img, [(15, 14), (23, 13), (29, 15)], (250, 220, 80, 255))
    elif name == 'sky_whale':
        s.add(P([(4, 18), (10, 22), (4, 28)]), '#5a8ac8', 'bevel', depth=1)
        s.add(E(25, 24, 18, 10), '#7ab0e0')
        s.add(E(28, 29, 13, 4), '#e8f0f8', outline=False)
        s.add(E(22, 32, 5, 2.5, 30), '#5a8ac8', 'bevel', depth=1)
        img = s.render()
        _px(img, [(36, 22), (37, 22)], (20, 30, 50, 255))
        d = ImageDraw.Draw(img)
        d.line((31, 27, 42, 26), fill=(40, 60, 100, 255))
        d.line((25, 12, 25, 9), fill=(230, 245, 255, 255))
        d.line((23, 8, 27, 8), fill=(230, 245, 255, 255))
    elif name == 'fanfare_eel':
        s.add(C(4, 36, 14, 30, 2.6) | C(14, 30, 24, 34, 3.2) | C(24, 34, 32, 26, 3.4), '#1a4a52')
        s.add(P([(32, 22), (44, 14), (44, 34), (32, 30)]), GOLD, 'dome')        # trumpet-bell mouth
        img = s.render()
        d = ImageDraw.Draw(img)
        for x in range(8, 30, 4):
            d.point((x, 32 - (x % 8 == 0)), fill=(230, 222, 200, 255))
        d.line((6, 35, 30, 29), fill=(41, 223, 235, 255))
        _px(img, [(42, 22), (42, 23), (42, 24), (42, 25), (42, 26)], (60, 36, 10, 255))
        ex.append((29, 26, (255, 240, 160, 255), False))
    elif name == 'kazoo_fish':
        s.add(P([(6, 18), (14, 24), (6, 32)]), '#f07858', 'bevel', depth=1)
        s.add(E(24, 25, 12, 9), '#3ab0a8')
        s.add(C(34, 27, 44, 27, 2.0), '#f0a0c0')
        s.add(E(22, 15, 4, 2.5), '#5a9a48')
        s.add(P([(18, 30), (24, 30), (20, 37)]), '#f07858', 'bevel', depth=1)
        img = s.render()
        _px(img, [(28, 21), (29, 21), (28, 22), (29, 22)], (255, 255, 255, 255))
        _px(img, [(29, 22)], (10, 10, 20, 255))
        _px(img, [(31, 23), (32, 23), (31, 24), (32, 24)], (255, 255, 255, 255))
        _px(img, [(31, 23)], (10, 10, 20, 255))
    elif name == 'tubafish':
        s.add(E(24, 28, 15, 13), '#8a90e0')
        s.add(C(24, 15, 24, 8, 3) | E(24, 7, 6, 2.5), GOLD)
        for a in range(0, 360, 45):
            x, y = 24 + 16 * math.cos(math.radians(a)), 28 + 14 * math.sin(math.radians(a))
            s.add(C(24 + 13 * math.cos(math.radians(a)), 28 + 11 * math.sin(math.radians(a)), x, y, 1.0), '#f07878')
        img = s.render()
        _px(img, [(30, 25), (31, 25), (30, 26), (31, 26)], (20, 20, 50, 255))
        for (x, y) in ((16, 22), (20, 33), (28, 35), (14, 29), (33, 31)):
            ex.append((x, y, (180, 255, 250, 255), False))
        _px(img, [(22, 3), (24, 2), (26, 3)], (250, 140, 200, 255))
    elif name == 'caravan':
        for (x0, y0, x1, y1) in ((14, 32, 8, 42), (20, 33, 18, 43), (28, 33, 30, 43), (34, 32, 40, 42)):
            s.add(C(x0, y0, x1, y1, 1.2), '#3a2a20')
        s.add(E(12, 30, 6, 5) | E(24, 30, 6, 5), '#4a3424')
        s.add(E(36, 28, 6, 5), '#5a4030')
        s.add(C(39, 24, 44, 17, 0.8) | C(36, 24, 37, 16, 0.8), '#3a2a20', 'flat')
        s.add(P([(10, 26), (14, 12), (18, 26)]), '#ffb040', 'bevel', depth=2)
        s.add(P([(18, 27), (23, 6), (28, 27)]), '#ff7ab0', 'bevel', depth=2)
        img = s.render()
        ex += [(14, 18, (255, 230, 160, 255), False), (23, 12, (255, 220, 240, 255), False)]
        _px(img, [(38, 27), (39, 27)], (10, 6, 4, 255))
    elif name == 'gobbler':
        s.add(P([(2, 20), (10, 26), (2, 34)]), '#16282e', 'bevel', depth=1)
        s.add(E(24, 27, 16, 11), '#1e343a')
        s.add(E(34, 30, 10, 6), '#0a1418')                                   # the huge mouth
        s.add(C(40, 24, 46, 34, 0.8) | C(38, 26, 42, 40, 0.8), '#2a4a50', 'flat')
        img = s.render()
        d = ImageDraw.Draw(img)
        for i, x in enumerate(range(14, 28, 3)):
            d.line((x, 21, x + 1, 33), fill=(30, 140, 150, 255))
            ex.append((x, 27, (110, 240, 255, 255), False))
        for x in range(28, 42, 3):
            d.point((x, 25), fill=(230, 230, 210, 255))
    elif name == 'swifter':
        for k, (x1, y1) in enumerate(((2, 18), (4, 28), (8, 36))):
            s.add(C(14, 30, x1, y1, 4.4), '#e8ecf6')
        s.add(E(24, 30, 10, 7), '#f4f4fa')
        s.add(E(35, 22, 7, 6), '#f8f8fc')
        s.add(P([(30, 18), (32, 8), (35, 17)]) | P([(36, 17), (40, 8), (41, 19)]), '#f4f4fa', 'bevel', depth=1)
        s.add(P([(39, 24), (46, 25), (40, 27)]), '#f4f4fa', 'bevel', depth=1)
        for x in (18, 22, 28, 31):
            s.add(C(x, 34, x, 43, 1.6), '#dce0ec')
        img = s.render()
        _px(img, [(32, 13), (32, 14), (38, 13), (38, 14)], (248, 170, 190, 255))
        _px(img, [(37, 21), (38, 21), (37, 22)], (60, 120, 220, 255))
        _px(img, [(46, 25)], (40, 30, 40, 255))
    elif name == 'jailer':
        d0 = Image.new('RGBA', (48, 48), (0, 0, 0, 0))
        dd = ImageDraw.Draw(d0)
        for x in range(4, 22, 4):                                            # the cell it hauls
            dd.line((x, 18, x, 44), fill=(120, 130, 136, 255), width=1)
        dd.rectangle((3, 17, 21, 19), fill=(90, 98, 104, 255))
        dd.rectangle((3, 43, 21, 45), fill=(90, 98, 104, 255))
        s.add(C(30, 14, 26, 30, 3.6), '#103a40')                                 # hunched torso
        s.add(E(32, 10, 5, 4), '#164a50')
        s.add(C(26, 30, 22, 44, 1.8) | C(28, 30, 32, 44, 1.8), '#0c2e34')
        s.add(C(28, 16, 18, 22, 1.4) | C(18, 22, 14, 18, 1.2), '#0c2e34')        # arm on the cell
        img = s.render()
        d0.alpha_composite(img)
        img = d0
        ex += [(29, 20, GLOW, False), (31, 25, GLOW, False), (33, 9, GLOW, False)]
    elif name == 'sculkling':
        s.add(P([(18, 18), (2, 6), (6, 20)]) | P([(30, 18), (46, 6), (42, 20)]), '#0e2a30', 'bevel', depth=2)
        s.add(E(24, 22, 9, 8), '#12343a')
        s.add(E(24, 34, 6, 6), '#103036')
        s.add(C(20, 38, 18, 44, 1.4) | C(28, 38, 30, 44, 1.4), '#0c2a30')
        img = s.render()
        d = ImageDraw.Draw(img)
        d.line((5, 9, 16, 18), fill=(41, 223, 235, 255))
        d.line((43, 9, 32, 18), fill=(41, 223, 235, 255))
        d.line((19, 26, 29, 26), fill=(6, 18, 20, 255))
        for x in (20, 23, 26, 28):
            d.point((x, 27), fill=(230, 230, 210, 255))
    elif name == 'dictator':
        s.add(C(15, 16, 9, 3, 3) | C(9, 3, 5, 6, 1.6), '#e3ddcc')
        s.add(C(33, 16, 39, 3, 3) | C(39, 3, 43, 6, 1.6), '#e3ddcc')
        s.add(C(12, 22, 3, 19, 2.2) | C(36, 22, 45, 19, 2.2), '#c8c0aa')
        s.add(E(24, 26, 14, 16), '#5a1a30')
        s.add(P([(13, 19), (35, 19), (32, 25), (16, 25)]), '#3a0e1e', 'bevel', depth=1)
        img = s.render()
        for x in (17, 29):
            _px(img, [(x - 1, 22), (x + 2, 22), (x - 1, 23), (x, 23), (x + 1, 23), (x + 2, 23)], (4, 8, 12, 255))
            ex.append((x, 22, (46, 242, 226, 255), False))
        d = ImageDraw.Draw(img)
        d.line((16, 34, 32, 34), fill=(4, 8, 12, 255))
        for x in (17, 20, 27, 30):
            d.point((x, 35), fill=(244, 240, 229, 255))
    elif name == 'thumper':
        s.add(E(24, 26, 20, 13), '#2f4a3c')
        for (x, y) in ((14, 22), (24, 18), (34, 22), (19, 30), (29, 30)):
            s.add(P([(x - 5, y), (x - 2, y - 4), (x + 2, y - 4), (x + 5, y), (x + 2, y + 4), (x - 2, y + 4)]), '#5a7a50', 'bevel', depth=2)
        s.add(E(43, 30, 5, 5), BONE)
        s.add(C(10, 36, 8, 43, 3) | C(36, 36, 38, 43, 3), '#8a8268')
        s.add(C(36, 18, 44, 8, 1.8), '#9a6a30')                              # a cannon tower barrel
        img = s.render()
        ex += [(44, 29, GLOW, False), (24, 18, GLOW, True), (19, 30, GLOW, False)]
    elif name in ('strummer', 'strumling'):
        big = name == 'strummer'
        k = 1.0 if big else 0.75
        for (x1, y1, x2, y2) in ((10, 14, 3, 30), (12, 22, 2, 40), (36, 22, 46, 40), (38, 14, 45, 30)):
            s.add(C(24, 26, 24 + (x1 - 24) * k, 26 + (y1 - 26) * k, 1.6 * k + 0.4), '#3a2450')
            s.add(C(24 + (x1 - 24) * k, 26 + (y1 - 26) * k, 24 + (x2 - 24) * k, 26 + (y2 - 26) * k, 1.2 * k + 0.3), BONE)
        s.add(E(24, 30, 10 * k, 8 * k), '#2a1838')
        s.add(E(24, 21, 8 * k, 6 * k), '#4a2c66')
        img = s.render()
        for (x, y) in ((21, 19), (26, 19), (19, 22), (28, 22)):
            ex.append((int(24 + (x - 24) * k), int(21 + (y - 21) * k), (216, 160, 255, 255) if big else GLOW, False))
        if big:
            d = ImageDraw.Draw(img)
            for x in (21, 23, 25, 27):
                d.line((x, 26, x, 38), fill=(127, 247, 255, 190))
        else:
            ex += [(21, 31, GLOW, False), (27, 31, GLOW, False)]
    else:  # sculk_parasite: a tick of sculk with grasping tendrils
        for a in (200, 240, 300, 340):
            x, y = 24 + 18 * math.cos(math.radians(a)), 28 - 14 * math.sin(math.radians(a))
            s.add(C(24, 28, x, y, 1.3), '#0c2a30')
        s.add(E(24, 28, 11, 9), '#12343a')
        s.add(E(24, 24, 7, 4), '#1f5a60', 'bevel', depth=1)
        img = s.render()
        ex += [(23, 27, GLOW, True), (17, 31, GLOW, False), (31, 31, GLOW, False)]
    for (x, y, col, big) in ex:
        _glow_eye(img, x, y, col, big)
    return img


def codex_portraits(out):
    sheet = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    for i, name in enumerate(PORTRAITS):
        rnd = random.Random(i * 31 + 7)
        plate = _backdrop(HABITAT[name], rnd)
        plate.alpha_composite(_creature(name))
        _plate_frame(plate)
        sheet.alpha_composite(plate, ((i % 10) * 48, (i // 10) * 48))
    out('gui/codex_portraits', sheet)
    _size(32)


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
    codex_portraits(out)
    codex_fx(out)
