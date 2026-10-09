"""W-sea art: the block and item textures of the Brass Coral Reef and the Chrome Coral Ocean, at Mojang density (16x16).

Every block texture starts from its closest vanilla Mojang texture (the same layout, value structure and pixel clusters)
re-themed with reef palettes from the reference crops (refzip/crops: the coral/shell sprite sheet and the brass, copper
and verdigris block palettes): 4-7 shades per material, light from the top left, accents only as small 2-3 px clusters.
  * Copper Sand          <- sand: copper-orange grains with verdigris and bright metal grains
  * Trumpet Coral Block  <- tube coral block: fused metal tubes seen end on (pores), patina in the deepest pores
  * Trumpet Coral tube   <- chorus plant: a polished, fibrous metal stalk
  * Trumpet Coral bell   <- copper block: the polished flare in bands (rolled lip, flare, throat); the mouth is drawn:
                            a bright rim, the inner wall and a dark throat
  * Bubble Coral         <- bubble coral block / coral / fan: six colours, cool shadows and glassy highlights
  * Algae                <- glow lichen: algae greens, no glow
  * Tube Seaweed         <- drawn like kelp: swaying (animated) algae-green tubes that tile, open rimmed mouths at the tips
  * Rainbow Anemone      <- drawn: a violet column whose tentacles end in glowing bulbs of every colour
Items (16x16, 1 px outline in a darker shade of the item, like vanilla): the four bells, the six fans, Tube Seaweed.
gen_textures.main() calls textures(out) before vanilla_remap; every texture here is final and listed in
vanilla_remap.SKIP (the remap leaves it alone).
"""
from __future__ import annotations

import colorsys
import math
import os
import random

import numpy as np
from PIL import Image

VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')
CUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}

METALS = {
    # dark -> light; brass gold, silver, orange copper, oxidized verdigris (from the brass/copper/verdigris block palettes)
    'brass': ('#3a2306', '#654110', '#93651c', '#c08f2a', '#e0b84c', '#f6dc85', '#fff4c6'),
    'silver': ('#2c3038', '#4f5661', '#78808c', '#a2aab4', '#c9d0d8', '#e9eef2', '#ffffff'),
    'copper': ('#3a160b', '#692b14', '#9c4622', '#c96634', '#e58c4e', '#f6b37a', '#ffdcb6'),
    'verdigris': ('#10362c', '#1c5444', '#2b765f', '#41a07f', '#64c4a0', '#97e0c3', '#d0f6e6'),
}
# a second metal showing through each one: green patina on the bright metals, bare copper where verdigris has worn off
PATINA = {'brass': ('#2f7d64', '#58b090'), 'silver': ('#3f8f80', '#7cc8b4'), 'copper': ('#2f7d64', '#58b090'),
          'verdigris': ('#9c4622', '#d0743a')}
THROAT = {'brass': '#170d03', 'silver': '#101318', 'copper': '#170804', 'verdigris': '#05150f'}
ALGAE = ('#1d3c12', '#2b5a1b', '#3f7c24', '#589c2e', '#78bb3e', '#a2d862')
BUBBLES = {
    'rose': ('#4f0c25', '#831c40', '#b9345c', '#e25a84', '#f78fae', '#ffcbdc'),
    'amber': ('#552604', '#8c440b', '#c27017', '#ea9f2b', '#fac958', '#fff0a8'),
    'lime': ('#20440b', '#386e13', '#599c1e', '#80c730', '#afe75c', '#e3ffab'),
    'azure': ('#0c2b60', '#194b96', '#2c77cc', '#52a5ee', '#8dcdff', '#d0edff'),
    'violet': ('#280d52', '#461d84', '#6a33b8', '#955ade', '#c096f4', '#e9d6ff'),
    # pearl: lavender shadows, pink-white body, white highlights (its sheen is added in clusters)
    'pearl': ('#5c5470', '#887d9f', '#b5aac8', '#dcd1e4', '#f4edf6', '#ffffff'),
}
COPPER_SAND = ('#6e3418', '#8d4723', '#a85b30', '#bd6e3d', '#cf834e', '#de9a64', '#eab27e')


# ============================================================================ helpers

def _hex(c):
    return np.array([int(c[i:i + 2], 16) for i in (1, 3, 5)], dtype=np.float64)


def _vanilla(rel):
    return np.asarray(Image.open(os.path.join(VANILLA, rel + '.png')).convert('RGBA'), dtype=np.float64)


def _img(a):
    return Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA')


def _lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def _rank(v):
    """Rank of each value in [0, 1]; equal values share their average rank."""
    v = np.asarray(v, dtype=np.float64)
    if v.size <= 1:
        return np.full(v.shape, 0.5)
    u, inv, cnt = np.unique(v, return_inverse=True, return_counts=True)
    avg = (np.cumsum(cnt) - cnt) + (cnt - 1) / 2.0
    return avg[inv.reshape(-1)].reshape(v.shape) / (v.size - 1)


def _ramp(ramp, t):
    """Nearest ramp colour for luminance quantile(s) t (pixel-art banding, no invented in-betweens)."""
    cols = np.array([_hex(c) for c in ramp])
    idx = np.clip(np.round(np.asarray(t) * (len(cols) - 1)).astype(int), 0, len(cols) - 1)
    return cols[idx]


def recolor(a, ramp, mask=None, lo=0.0, hi=1.0):
    """Mojang's value structure, our colours: each pixel's luminance rank picks its colour from ramp[lo..hi]."""
    out = a.copy()
    m = (a[..., 3] > 0) if mask is None else mask
    if m.any():
        t = lo + (hi - lo) * _rank(_lum(a)[m])
        out[m, :3] = _ramp(ramp, t)
    return out


def _clusters(a, mask, n, colours, rng, size=3):
    """n small accent clusters (2-`size` px, 4-connected, inside mask): Mojang's clustered noise, never single specks."""
    ys, xs = np.nonzero(mask)
    if not len(ys):
        return a
    h, w = mask.shape
    for i in rng.sample(range(len(ys)), min(n, len(ys))):
        c = _hex(rng.choice(colours))
        y, x = ys[i], xs[i]
        cells = [(y, x)]
        for _ in range(rng.randint(1, size - 1)):
            cy, cx = rng.choice(cells)
            dy, dx = rng.choice(((0, 1), (1, 0), (0, -1), (-1, 0)))
            ny, nx = (cy + dy) % h, (cx + dx) % w
            if mask[ny, nx] and (ny, nx) not in cells:
                cells.append((ny, nx))
        for cy, cx in cells:
            a[cy, cx, :3] = c
    return a


def _brightest(a, frac):
    m = a[..., 3] > 0
    L = _lum(a)
    if not m.any():
        return m
    return m & (L >= np.quantile(L[m], 1.0 - frac))


def _darkest(a, frac):
    m = a[..., 3] > 0
    L = _lum(a)
    if not m.any():
        return m
    return m & (L <= np.quantile(L[m], frac))


def _sprite(rows, pal):
    a = np.zeros((16, 16, 4))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row[:16]):
            if ch in pal:
                a[y, x, :3] = _hex(pal[ch])
                a[y, x, 3] = 255
    return a


# ============================================================================ the reef

def copper_sand():
    a = recolor(_vanilla('block/sand'), COPPER_SAND)
    rng = random.Random(51)
    _clusters(a, _darkest(a, 0.3), 3, ('#2f7d64', '#41a07f'), rng)   # verdigris grains
    _clusters(a, _brightest(a, 0.15), 2, ('#f8cf9c',), rng, 2)       # bright metal grains
    return a


def trumpet_block(metal):
    """Fused tubes seen end on: the tube coral block's pores, in metal; green patina in the deepest pores."""
    a = recolor(_vanilla('block/tube_coral_block'), METALS[metal], lo=0.0, hi=0.92)
    rng = random.Random(sum(map(ord, metal)))
    _clusters(a, _darkest(a, 0.2), 3, PATINA[metal], rng)
    _clusters(a, a[..., 3] > 0, 1, (ALGAE[3],), rng, 2)
    return a


def trumpet_pipe(metal):
    """The tube: the chorus plant's fibres in polished metal (no deep shadows), a fleck of patina."""
    a = recolor(_vanilla('block/chorus_plant'), METALS[metal], lo=0.18, hi=0.96)
    _clusters(a, _darkest(a, 0.15), 1, PATINA[metal][1:], random.Random(7 + len(metal)), 2)
    return a


def trumpet_bell(metal):
    """The flare round a bell, in bands from the top of the texture down (the model maps them in order): the rolled lip
    (rows 4-5, brightest), the flare (6-7), the throat (8-11, patina creeping up from below). Within each band the copper
    block's own clusters pick between two neighbouring shades."""
    ramp = METALS[metal]
    q = _rank(_lum(_vanilla('block/copper_block')))
    bands = [(0, 4, 4), (4, 5, 6), (5, 6, 5), (6, 8, 4), (8, 10, 3), (10, 16, 2)]   # (row from, row to, base shade)
    cols = np.array([_hex(c) for c in ramp])
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    for y0, y1, s in bands:
        for y in range(y0, y1):
            for x in range(16):
                a[y, x, :3] = cols[min(len(cols) - 1, s + (1 if q[y, x] > 0.72 else 0) - (1 if q[y, x] < 0.22 and s > 1 else 0))]
    rng = random.Random(31 + len(metal))
    lower = np.zeros((16, 16), bool)
    lower[9:, :] = True
    _clusters(a, lower, 3, PATINA[metal], rng)
    return a


def trumpet_mouth(metal):
    """Looking into the bell: a bright rolled rim lit from the top left, the inner wall darkening inwards, the dark throat."""
    ramp = METALS[metal]
    cols = np.array([_hex(c) for c in ramp])
    a = np.zeros((16, 16, 4))
    a[..., 3] = 255
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r = math.hypot(dx, dy)
            lit = (-dx - dy) > 0  # the top-left half catches the light
            if r > 6.3:
                s = 6 if lit else 4          # the rolled rim
            elif r > 5.2:
                s = 4 if lit else 3
            elif r > 3.6:
                s = 1 if lit else 3          # the inner wall: the far (bottom-right) side catches the light
            elif r > 2.2:
                s = 0 if lit else 1
            else:
                s = -1
            a[y, x, :3] = _hex(THROAT[metal]) if s < 0 else cols[s]
    return a


def bell_item(metal):
    """The bell in your hand: vanilla's Goat Horn sprite (the closest Mojang shape: a horn flaring to a wide mouth) in
    polished metal, its mouth a dark throat, a little green patina towards the narrow end."""
    a = recolor(_vanilla('item/goat_horn'), METALS[metal])
    for x in (3, 4, 5):
        a[4, x, :3] = _hex(THROAT[metal])
    a[5, 4, :3] = _hex(METALS[metal][1])
    for y, x in ((12, 6), (12, 7)):
        if a[y, x, 3] > 0:
            a[y, x, :3] = _hex(PATINA[metal][1])
    return a


# ---------------------------------------------------------------- the reef's greens

def algae():
    return recolor(_vanilla('block/glow_lichen'), ALGAE)


TUBE_FRAMES = 20
# each tube: its x, its sway phase, and the row of its mouth in the tip texture
TUBES = ((1, 0.0, 4), (5, 1.7, 1), (9, 3.1, 5), (12, 4.4, 2))


def _tube_frame(f, tip):
    """One frame of Tube Seaweed: 2-pixel tubes (lit left edge, shaded right edge) that lean with the current; in the
    tip texture each ends in a flared mouth - a pale rim round a dark opening."""
    a = np.zeros((16, 16, 4))
    c = [_hex(x) for x in ALGAE]
    hole = _hex('#10260b')
    for i, (x0, ph, top) in enumerate(TUBES):
        for y in range(16):
            if tip and y < top:
                continue
            # the lean: periodic in y for the tiling body, growing towards the free end in the tip
            sway = math.sin(math.tau * f / TUBE_FRAMES + ph) * (0.8 * (1.0 - y / 16.0) if tip else 0.4)
            wave = math.sin(math.tau * y / 16.0 + ph) * (0.0 if tip else 0.6)
            x = int(round(x0 + sway + wave))
            for dx, col in ((0, c[4] if i % 2 == 0 else c[3]), (1, c[2] if i % 2 == 0 else c[1])):
                if 0 <= x + dx < 16:
                    a[y, x + dx, :3] = col
                    a[y, x + dx, 3] = 255
            if tip and y == top:
                for dx, col in ((-1, c[4]), (0, c[5]), (1, c[5]), (2, c[3])):        # the rim
                    if 0 <= x + dx < 16:
                        a[y, x + dx, :3] = col
                        a[y, x + dx, 3] = 255
                for dx, col in ((-1, c[3]), (0, hole), (1, hole), (2, c[2])):        # the opening
                    if 0 <= x + dx < 16 and y + 1 < 16:
                        a[y + 1, x + dx, :3] = col
                        a[y + 1, x + dx, 3] = 255
            elif not tip and (y + i * 5) % 11 == 0 and 0 <= x - 1:
                a[y, x - 1, :3] = c[2]         # a little bud on the tube's side
                a[y, x - 1, 3] = 255
    return a


def tube_seaweed(tip):
    return np.concatenate([_tube_frame(f, tip) for f in range(TUBE_FRAMES)], 0)


def tube_seaweed_item():
    """The item: a clump of three tubes with open mouths, outlined in dark green."""
    rows = [
        '................',
        '....orro........',
        '....okko..orro..',
        '.orrolmo..okko..',
        '.okkolmo..olmo..',
        '.olmolmo..olmo..',
        '.olmolmo..olmo..',
        '.olmolmo.olmo...',
        '.olmolmoolmo....',
        '..olmolmolmo....',
        '..olmolmlmo.....',
        '...olmlmmo......',
        '...ollmmmo......',
        '....olmmo.......',
        '....ommmo.......',
        '.....ooo........',
    ]
    return _sprite(rows, {'o': ALGAE[0], 'l': ALGAE[4], 'm': ALGAE[2], 'r': ALGAE[5], 'k': '#10260b'})


# ============================================================================ the Chrome garden

def bubble(colour, part):
    """part: 'block', 'plant' or 'fan' - vanilla bubble coral's own bubbles, in our colours: cool-tinted shadows and the
    brightest clusters lifted to glassy highlights (pearl: its highlights take a pink or aqua sheen, bubble by bubble)."""
    src = {'block': 'block/bubble_coral_block', 'plant': 'block/bubble_coral', 'fan': 'block/bubble_coral_fan'}[part]
    a = recolor(_vanilla(src), BUBBLES[colour])
    glint = _brightest(a, 0.06)
    a[glint, :3] = a[glint, :3] * 0.4 + 255 * 0.6
    deep = _darkest(a, 0.12)
    a[deep, :3] = a[deep, :3] * 0.82 + np.array([24.0, 30.0, 72.0]) * 0.18
    if colour == 'pearl':
        sheen = _brightest(a, 0.3) & ~glint
        ys, xs = np.nonzero(sheen)
        for y, x in zip(ys, xs):
            a[y, x, :3] = _hex('#fde1ee') if ((x // 4) + (y // 4)) % 2 == 0 else _hex('#d8f4f0')
    return a


def rainbow_anemone():
    """A violet column whose tentacles fan up and out, each ending in a glowing bulb of its own colour (cross texture)."""
    a = np.zeros((16, 16, 4))
    body = [_hex(c) for c in ('#2a0f4a', '#41196e', '#5d2a94', '#7c42b8', '#a26ad6')]

    def px(x, y, col):
        if 0 <= x < 16 and 0 <= y < 16:
            a[y, x, :3] = col
            a[y, x, 3] = 255
    for y in range(12, 16):                       # the column, lit from the left
        for x in range(6, 10):
            px(x, y, body[3] if x == 6 else body[2] if x == 7 else body[1])
    for x in range(5, 11):                        # the oral disc
        px(x, 11, body[4] if x < 8 else body[3])
    tentacles = ((-6, 5, 0.0), (-4, 2, 0.13), (-2, 1, 0.27), (1, 0, 0.42), (3, 2, 0.58), (5, 1, 0.72), (6, 5, 0.86), (-1, 4, 0.95))
    for dx, top, hue in tentacles:
        x0, x1 = 7.5 + dx * 0.2, 7.5 + dx
        steps = 10 - top
        for s in range(steps + 1):
            t = s / steps
            x = int(round(x0 + (x1 - x0) * (t ** 0.7)))
            y = int(round(10 - (10 - top) * t))
            px(x, y, body[3] if t > 0.45 else body[2])
        r, g, b = colorsys.hsv_to_rgb(hue, 0.6, 1.0)
        r2, g2, b2 = colorsys.hsv_to_rgb(hue, 0.25, 1.0)
        px(int(round(x1)), top, np.array([r2, g2, b2]) * 255)          # the bulb: a pale glowing tip
        px(int(round(x1)), top + 1, np.array([r, g, b]) * 255)
    return a


# ============================================================================ output

def textures(out):
    import vanilla_remap as VR
    done = []

    def put(name, arr, meta=None):
        out(name, _img(arr), meta)
        if name.startswith('block/'):
            done.append(name.split('/', 1)[1])

    put('block/copper_sand', copper_sand())
    for m in METALS:
        put(f'block/{m}_trumpet_coral_block', trumpet_block(m))
        put(f'block/{m}_trumpet_coral', trumpet_pipe(m))
        put(f'block/{m}_trumpet_coral_bell', trumpet_bell(m))
        put(f'block/{m}_trumpet_coral_bell_mouth', trumpet_mouth(m))
        put(f'item/{m}_trumpet_coral_bell', bell_item(m))
    for c in BUBBLES:
        put(f'block/{c}_bubble_coral_block', bubble(c, 'block'))
        put(f'block/{c}_bubble_coral', bubble(c, 'plant'), CUT)
        fan = bubble(c, 'fan')
        put(f'block/{c}_bubble_coral_fan', fan, CUT)
        put(f'item/{c}_bubble_coral_fan', fan)
    put('block/algae', algae(), CUT)
    anim = {'animation': {'frametime': 3}, 'texture': {'mipmap_strategy': 'strict_cutout'}}
    put('block/tube_seaweed', tube_seaweed(False), anim)
    put('block/tube_seaweed_tip', tube_seaweed(True), anim)
    put('item/tube_seaweed', tube_seaweed_item())
    put('block/rainbow_anemone', rainbow_anemone(), CUT)
    VR.SKIP.update(done)
