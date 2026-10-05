"""Geometry + paint for the Sift's wild creatures: the Stomper, the three music fish and the Sky Whale
(see modelkit.py; registered into mobs.ALL at the bottom of mobs.py)."""
import math
import os

from PIL import Image

from modelkit import Model

EXPRS = ['blink', 'happy', 'angry', 'hurt', 'dead']


# =========================================================================== face helpers
def rows_of(w, h, fn):
    """Builds a w x h texel map; fn(x, y, u, v) returns a char (u, v in -1..1 across the face)."""
    out = []
    for y in range(h):
        r = ''
        for x in range(w):
            u = (x + 0.5) / w * 2 - 1
            v = (y + 0.5) / h * 2 - 1
            r += fn(x, y, u, v)
        out.append(r)
    return out


def eye(w, h, expr, mirror=False, pupil='bar', rim=0.78, shine=True):
    """A round painted eye filling a w x h texel face, in one of the facial expressions.
    keys: r rim, i iris, I lower iris, p pupil, h highlight, l lid, d lash / crease line."""
    s = -1.0 if mirror else 1.0

    def fn(x, y, u, v):
        rr = u * u + v * v
        if rr > 1.0:
            return '.'
        uu = u * s
        if expr in ('blink', 'sleep'):
            return 'd' if abs(v - 0.15 - 0.25 * u * u) < 1.6 / h else 'l'
        if expr == 'happy':
            return 'd' if abs(v - (0.45 - 0.8 * (1 - u * u))) < 1.8 / h and v > -0.6 else 'l'
        if expr == 'hurt':
            line = abs(v - 0.05) < 1.4 / h and abs(u) < 0.85
            tick = abs(abs(u) - 0.55) < 1.3 / w and abs(v + 0.25) < 0.3
            return 'd' if line or tick else 'l'
        if rr > rim:
            return 'r'
        if expr == 'angry':
            lid = -0.15 + 0.55 * uu
            if v < lid:
                return 'l'
            if v < lid + 2.2 / h:
                return 'd'
        if expr == 'dead':
            if rr < 0.55 and (abs(u - v) < 2.0 / w or abs(u + v) < 2.0 / w):
                return 'p'
            return 'i' if v < 0.25 else 'I'
        if shine and abs(uu + 0.42) < 1.6 / w + 0.08 and abs(v + 0.38) < 1.6 / h + 0.08:
            return 'h'
        if pupil == 'bar' and abs(v - 0.05) < 0.2 and abs(u) < 0.58:
            return 'p'
        if pupil == 'dot' and (u * u + (v - 0.05) ** 2) < 0.16:
            return 'p'
        if pupil == 'slit' and abs(u) < 0.16 and abs(v) < 0.75:
            return 'p'
        return 'i' if v < 0.3 else 'I'
    return rows_of(w, h, fn)


def eye_exprs(w, h, mirror=False, **kw):
    return {e: eye(w, h, e, mirror, **kw) for e in EXPRS}


def overlay(base, top):
    """Lays map `top` over map `base` (same size); '.' in top keeps base."""
    return [''.join(t if t != '.' else b for b, t in zip(rb, rt)) for rb, rt in zip(base, top)]


def blank(w, h):
    return ['.' * w for _ in range(h)]


def put(grid, x, y, glyph, mirror=False):
    g = [list(r) for r in grid]
    for j, row in enumerate(glyph):
        if mirror:
            row = row[::-1]
        for i, ch in enumerate(row):
            if ch != '.' and 0 <= y + j < len(g) and 0 <= x + i < len(g[0]):
                g[y + j][x + i] = ch
    return [''.join(r) for r in g]


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


# =========================================================================== plants & markings
BLOCK_TEX = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/thesift/textures/block')


def block_tex(name):
    """A Sift block sprite (the first 16 x 16 frame), used to paint moss, turf and plants on mobs."""
    try:
        return Image.open(os.path.join(BLOCK_TEX, name + '.png')).convert('RGBA').crop((0, 0, 16, 16))
    except OSError:
        return Image.new('RGBA', (16, 16), (80, 200, 187, 255))


def mirror_img(img):
    return img.transpose(Image.FLIP_LEFT_RIGHT)


def _cross(p, img, w, top, h, glow_bright):
    face = dict(color='plant', image=img, image_mode='stretch')
    if glow_bright is not None:
        face['glow_bright'] = glow_bright
    back = dict(face, image=mirror_img(img))
    p.cube((-w / 2, top, 0), (w, h, 0), color='plant', faces={'north': face, 'south': back})
    p.cube((0, top, -w / 2), (0, h, w), color='plant', faces={'east': face, 'west': back})


def plant(parent, name, pivot, tex, w, h, rot=(0, 0, 0), glow_bright=None):
    """A plant growing up from `pivot`: two crossed planes painted with a block sprite (they sway in
    the Java model)."""
    p = parent.part(name, pivot=pivot, rot=rot)
    _cross(p, block_tex(tex), w, -h, h, glow_bright)
    return p


def vine(parent, name, pivot, tex, w, h, rot=(0, 0, 0), glow_bright=None):
    """A vine hanging down from `pivot` on two crossed planes (it swings in the Java model)."""
    p = parent.part(name, pivot=pivot, rot=rot)
    img = block_tex(tex)
    _cross(p, img, w, 0, h, glow_bright)
    return p


def veins(w, h, seed, rise):
    """Sculk veins creeping up a face from its bottom edge, `rise` units high, with glowing beads."""
    import random
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(max(1, w // 9)):
        x = rnd.randrange(w)
        for y in range(h - 1, max(-1, h - 1 - rise), -1):
            g[y][x] = 'v'
            if x + 1 < w and g[y][x + 1] == '.':
                g[y][x + 1] = 'V'
            if rnd.random() < 0.35:
                x = max(0, min(w - 1, x + rnd.choice((-1, 1))))
                g[y][x] = 'v'
            if rnd.random() < 0.18:
                g[y][x] = 'g'
        g[max(0, h - rise)][x] = 'g'
    return [''.join(r) for r in g]


def swirls(w, h, seed):
    """Glowing sculk markings swirling along a flank: a wavy dark band with bright beads."""
    import random
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    mid = h * 0.45
    for x in range(w):
        y = int(mid + math.sin(x * 0.45 + seed) * 2.2)
        for yy in (y, y + 1):
            if 0 <= yy < h:
                g[yy][x] = 'v'
        if x % 5 == 0 and 0 <= y - 1 < h:
            g[y - 1][x] = 'g'
        if x % 7 == 3 and 0 <= y + 3 < h:
            g[y + 3][x] = 'G'
        if rnd.random() < 0.08 and 0 <= y + 2 < h:
            g[y + 2][x] = 'V'
    return [''.join(r) for r in g]


# =========================================================================== STOMPER
def stomper_mouth(expr, w=60, h=24):
    """The front of the Stomper's wide frog head: a long lip line with corners that curl with its
    mood, little nostril wrinkles round the trunk base and blush on the cheeks."""
    def fn(x, y, u, v):
        # lip line along the bottom, corners bending with the mood
        bend = {'happy': -0.55, 'angry': 0.35, 'hurt': 0.3, 'dead': 0.0, 'blink': -0.12}.get(expr, -0.12)
        edge = abs(u) ** 3
        lip_y = 0.82 + bend * edge
        if expr == 'hurt':
            lip_y += 0.06 * math.sin(u * 14)
        if abs(v - lip_y) < 1.3 / h and abs(u) < 0.96:
            return 'm'
        if abs(v - lip_y + 2.0 / h) < 0.8 / h and abs(u) < 0.9:
            return 'L'
        # cheeks: a blush with a dusting of freckles (CR1)
        cheek = (abs(u) - 0.78) ** 2 * 6 + (v - 0.35) ** 2 * 3
        if cheek < 0.085 and expr in ('happy', '', 'blink'):
            return 'f' if (x * 3 + y * 5) % 11 == 0 else 'b'
        if cheek < 0.12 and (x * 7 + y * 3) % 13 == 0:
            return 'f'
        # wrinkles round the trunk base
        if abs(u) < 0.18 and abs(v + 0.15) < 0.5 and (y % 4 == 0) and abs(u) > 0.06:
            return 'w'
        # warts
        if (x * 7 + y * 13) % 53 == 0 and abs(v) < 0.6:
            return 'W'
        return '.'
    rows = rows_of(w, h, fn)
    if expr == 'angry':
        # bared teeth peeking out
        rows = put(rows, 18, h - 4, ['t.t.t.t.t.t.t.t.t.t.t.t'])
    if expr == 'happy':
        rows = put(rows, 27, h - 3, ['tttttt'])
    return rows


def stomper_eye(w, h, expr, mirror=False, side=False):
    """The Stomper's big frog-dome eye: a glossy iris that darkens towards the bottom, round a golden
    ring and a wide frog pupil, two catchlights and a crease under the brow. It blinks shut, smiles,
    glares, winces or crosses out with its moods. keys: r rim, c crease, j/i/I iris (light to dark),
    o ring, p pupil, h catchlight, l lid, d lash line."""
    s = -1.0 if mirror else 1.0

    def fn(x, y, u, v):
        rr = u * u + v * v
        if rr > 1.0:
            return '.'
        uu = u * s
        if rr > 0.8:
            return 'r'
        if expr in ('blink', 'sleep'):
            return 'd' if abs(v - 0.18 - 0.22 * u * u) < 1.5 / h else 'l'
        if expr == 'happy':
            return 'd' if abs(v - (0.45 - 0.8 * (1 - u * u))) < 1.7 / h and v > -0.6 else 'l'
        if expr == 'hurt':
            line = abs(v - 0.05) < 1.4 / h and abs(u) < 0.8
            tick = abs(abs(u) - 0.5) < 1.3 / w and abs(v + 0.25) < 0.3
            return 'd' if line or tick else 'l'
        if expr == 'angry':
            lid = -0.2 + 0.6 * uu
            if v < lid:
                return 'l'
            if v < lid + 2.0 / h:
                return 'd'
        if v < -0.55 and rr > 0.5:
            return 'c'
        if expr == 'dead':
            if rr < 0.5 and (abs(u - v) < 1.8 / w or abs(u + v) < 1.8 / w):
                return 'p'
            return 'i' if v < 0.3 else 'I'
        if not side and ((uu + 0.34) ** 2 + (v + 0.3) ** 2 < 0.05 or (uu - 0.36) ** 2 + (v - 0.36) ** 2 < 0.014):
            return 'h'
        pupil = (u / 0.36) ** 2 + ((v - 0.06) / 0.34) ** 2
        if pupil < 1.0:
            return 'p'
        if (u / 0.47) ** 2 + ((v - 0.06) / 0.45) ** 2 < 1.0:
            return 'o'
        return 'j' if v < -0.2 else ('i' if v < 0.4 else 'I')
    return rows_of(w, h, fn)


def x2(rows):
    """A map in model units as texels (each character 2 x 2), to lay texel detail over it."""
    return [''.join(ch * 2 for ch in r) for r in rows for _ in (0, 1)]


def warts(w, h, seed, n, top=None):
    """Raised warts scattered over a face (texels): each a little bump lit on top (w) and shaded
    below (W), only above row `top` (the pale belly band has none)."""
    import random
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for _ in range(n):
        x, y = rnd.randrange(w - 2), rnd.randrange(max(1, (top or h) - 2))
        size = rnd.choice((1, 2, 2))
        for i in range(size):
            g[y][x + i] = 'w'
            g[y + 1][x + i] = 'W'
        if size == 2 and rnd.random() < 0.5:
            g[y][x + 2] = 'W'
    return [''.join(r) for r in g]


def frost(img, caps=True, seed=0, strength=0.7):
    """A plant or moss picture under hoarfrost, for the White Forest Stomper's garden: pale ice that
    keeps the light and shade (and a hint of the plant's own colour), snow on every top edge and a
    few glints."""
    import random
    rnd = random.Random(seed)
    w, h = img.size
    src = img.load()
    out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a < 128:
                continue
            lum = (0.3 * r + 0.55 * g + 0.15 * b) / 255.0
            # plants keep more of their shading; a moss carpet goes almost snow-white
            k = 0.9 * (0.8 + 0.2 * lum) if caps else 0.96 * (0.9 + 0.1 * lum)
            c = [int((ch * (1 - strength) + ice * strength) * k) for ch, ice in ((r, 236), (g, 246), (b, 252))]
            if (caps and (y == 0 or src[x, y - 1][3] < 128)) or rnd.random() < 0.035:
                c = [255, 255, 255]
            px[x, y] = (min(255, c[0]), min(255, c[1]), min(255, c[2]), 255)
    return out


def rosy(img, hue=0.965, sat=0.62):
    """A teal moss picture turned the rose-pink of the Sift's pink grass (CR1): every pixel keeps its
    light and shade (and its white sparkles), only the hue and saturation change."""
    import colorsys
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    src, px = img.load(), out.load()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            r, g, b, a = src[x, y]
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if ss > 0.15:
                r2, g2, b2 = colorsys.hsv_to_rgb(hue, min(1.0, ss * sat + 0.1), min(1.0, vv * 1.15 + 0.05))
                r, g, b = int(r2 * 255), int(g2 * 255), int(b2 * 255)
            px[x, y] = (r, g, b, a)
    return out


def kplant(parent, name, pivot, key, w, h, rot=(0, 0, 0), glow_bright=None):
    """plant(), with the picture a palette key (so the white coat can frost it)."""
    p = parent.part(name, pivot=pivot, rot=rot)
    face = dict(color='plant', image=key, image_mode='stretch')
    if glow_bright is not None:
        face['glow_bright'] = glow_bright
    back = dict(face, image=key + '|m')
    p.cube((-w / 2, -h, 0), (w, h, 0), color='plant', faces={'north': face, 'south': back})
    p.cube((0, -h, -w / 2), (0, h, w), color='plant', faces={'east': face, 'west': back})
    return p


# the garden on its back (texture, x, surface y, z, width, height): blooms crowd the mossy mound in
# the middle round the spiracles, coral and sprouts fill the corners, a white bloom at the rump
STOMPER_GARDEN = [('dreambloom', 0, -8, -1.5, 9, 11), ('hummingbloom', -5.5, -8, -1, 9, 10), ('hummingbloom', 5.5, -8, 0, 8, 9),
                  ('lullaby_bell', -3, -8, 4.5, 8, 9), ('nebula_iris', 3.5, -8, 4.5, 8, 9), ('coral_bush', -9.5, -6, -10, 9, 9),
                  ('coral_bush', 9.5, -6, 10, 9, 9), ('glimmer_sprouts', 9.5, -6, -9.5, 7, 7), ('glimmer_sprouts', -9.5, -6, 9.5, 7, 6),
                  ('echo_orchid', -10, -6, 0, 7, 8), ('blushgrass', 10, -6, -1, 8, 7), ('soulpetal', 0, -6, 10.5, 7, 8),
                  # CR1: tufts of the Sift's pink blushgrass all over its back
                  ('blushgrass', -3.5, -8, -6.5, 7, 7), ('blushgrass', 6.5, -6, 6.5, 7, 6), ('blushgrass', -6.5, -6, 10.5, 6, 6),
                  ('blushgrass', 2.5, -6, -11, 6, 5)]
STOMPER_SHOULDERS = [('coral_fern', -13.5, -15, 8, 8), ('dreambloom', 13.5, -14, 7, 8), ('lullaby_bell', -13.5, 15, 7, 8),
                     ('glimmer_sprouts', 13.5, 14, 7, 7)]
STOMPER_BROW = [('hummingbloom', -7, -6, 7, 8), ('glimmer_sprouts', 8, -4, 7, 7)]
_GLOWING = ('glimmer_sprouts', 'echo_orchid')
# petals shed by the garden, lying on the moss (texels on the hump's top)
_PETALS = [(6, 22), (16, 4), (31, 6), (40, 18), (8, 40), (24, 46), (38, 38), (44, 30), (2, 30)]


def stomper() -> Model:
    """The Stomper: a huge, shaggy mammoth-bullfrog in the Sift's green and pink. A warty mint-green
    frog body on four pink-furred pillar legs, a glowing sculk vein or two, and - like a Sniffer - a
    whole little garden growing on its back: a carpet of the Sift's pink Coral Turf strewn with petals
    and blushgrass tufts, a crown of blooms round the three puffing spiracles and coral and sprouts in
    the corners, all swaying as it walks. Two big glossy violet frog-dome eyes, a long ringed trunk,
    floppy ears. In the White Forest it is frosted mint with a snowy pink garden (stomper_white)."""
    # CR1: its body wears the green of the Sift's grass and its back grows the Sift's pink grass (coral turf and
    # blushgrass) - the old pink body and teal garden swapped round. Violet eyes with a golden ring stand out on the mint.
    pal = {
        'skin': '#4cc4b4', 'skin_l': '#7fe3d2', 'skin_d': '#2f9a92', 'wart': '#257f78', 'wart_l': '#9df0e0', 'spot': '#ff8ea4',
        'belly': '#dcf7ee', 'belly_l': '#f2fffa', 'belly_d': '#a9dccf',
        'fur': '#e9779b', 'fur_l': '#ff9fbd', 'fur_d': '#b84e74', 'moss': '#f07a92', 'moss_l': '#ff9aac', 'moss_d': '#c8566e',
        'sculk': '#12303a', 'sculk_l': '#1f5a66', 'glow': '#3ff5e6', 'glow2': '#c8fffb',
        'mouth': '#2a1a34', 'lip': '#22706b', 'tongue': '#ff8fb0', 'teeth': '#fff8ec', 'blush': '#ff6f9a',
        'eye': '#1a1430', 'iris': '#b58cf0', 'iris_d': '#7b55c9', 'iris_l': '#e2d0ff', 'eye_ring': '#ffd36b', 'eye_hi': '#ffffff',
        'lid': '#5fd0c2', 'lid_d': '#22706b', 'freckle': '#e0587e',
        'nail': '#e3ddcc', 'nail_d': '#b3ab96', 'stone': '#c9d3e8', 'stone_d': '#93a0bf', 'hole': '#0c1a24', 'chrome': '#a8fbff',
        'trunk': '#4cc4b4', 'trunk_l': '#7fe3d2', 'trunk_d': '#2f9a92', 'ring': '#ff8595', 'ring_d': '#d85c74', 'plant': '#f07a92',
        'drum': '#b0643c', 'drum_l': '#c98154', 'drum_d': '#8a4a2a', 'drumhead': '#fbeedb', 'drumhead_d': '#e2cfb3',
        'petal': '#fff3c4', 'petal_l': '#ffffff', 'petal_d': '#f2c766',
    }
    # the White Forest coat: a frosted-mint body under a snowy pink garden
    white = {
        'skin': '#e2f4ef', 'skin_l': '#ffffff', 'skin_d': '#b8d8d0', 'wart': '#a8cfc6', 'wart_l': '#ffffff', 'spot': '#ffd0dc',
        'belly': '#f6fffc', 'belly_l': '#ffffff', 'belly_d': '#d4ece6',
        'fur': '#f8dbe5', 'fur_l': '#ffffff', 'fur_d': '#ddb3c3', 'moss': '#f6d6e0', 'moss_l': '#fff0f5', 'moss_d': '#e0b2c2',
        'sculk': '#7fb3d4', 'sculk_l': '#a6d0e8', 'glow': '#c4f8ff', 'glow2': '#ffffff',
        'mouth': '#3d2b48', 'lip': '#8fb2ab', 'tongue': '#ffb3c9', 'teeth': '#ffffff', 'blush': '#ffc2d6',
        'eye': '#1c1838', 'iris': '#c3a9ec', 'iris_d': '#8f72d0', 'iris_l': '#e9ddff', 'eye_ring': '#ffe9a8', 'eye_hi': '#ffffff',
        'lid': '#e8f6f2', 'lid_d': '#9fbab5', 'freckle': '#e8a9c2',
        'nail': '#f6f3ea', 'nail_d': '#c8c1ae', 'stone': '#eaf0f8', 'stone_d': '#b6c2d6', 'hole': '#1a2a3a', 'chrome': '#e2fdff',
        'trunk': '#e2f4ef', 'trunk_l': '#ffffff', 'trunk_d': '#b8d8d0', 'ring': '#f7c3d0', 'ring_d': '#e19ab0', 'plant': '#f6d6e0',
        'drum': '#d8c7a8', 'drum_l': '#eadcc0', 'drum_d': '#b09a78', 'drumhead': '#ffffff', 'drumhead_d': '#e4e9f0',
        'petal': '#e6f4ff', 'petal_l': '#ffffff', 'petal_d': '#b9dcf0',
    }
    # the pictures (moss, turf, every plant) are palette entries too, so the white coat gets them frosted
    # CR1: the turf on its back is the Sift's pink Coral Turf, the moss under it lumen moss gone rose-pink
    for i, (key, tex, caps) in enumerate([('img:moss', 'lumen_moss_block', False), ('img:turf', 'coral_turf_top', False)]
                                         + [(f'img:{t}', t, True) for t in sorted({g[0] for g in STOMPER_GARDEN + STOMPER_SHOULDERS + STOMPER_BROW})]):
        img = block_tex(tex)
        if key == 'img:moss':
            img = rosy(img)
        pal[key], pal[key + '|m'] = img, mirror_img(img)
        cold = frost(img, caps, seed=i, strength=0.7 if caps else 0.55)
        white[key], white[key + '|m'] = cold, mirror_img(cold)
    m = Model('stomper', (256, 256), pal, {'stomper': {}, 'stomper_white': white}, res=2, expressions=EXPRS)
    skin = mc('skin', clusters=0.35, spots=0.18, accent='spot')
    fur = mc('fur', clusters=0.25, streaks=0.55)

    body = m.part('body', pivot=(0, 11, 2))
    vk = {'v': 'sculk', 'V': 'sculk_l', 'g': 'glow', 'w': 'wart_l', 'W': 'wart'}

    def flank(w, h, seed):
        # warts on the pink above the belly band, sculk veins creeping up from below
        return mc('skin', clusters=0.35, spots=0.18, accent='spot', bands=[(13, 'belly')], hd=True,
                  map=overlay(warts(w * 2, h * 2, seed, w // 2, top=24), x2(veins(w, h, seed, 10))), keys=vk, glow_keys='g')
    # the warty pink barrel, a pale belly band, sculk veins creeping up from below
    body.cube((-16, -20, -18), (32, 20, 36), **skin, faces={
        'down': mc('belly', clusters=0.3),
        'up': dict(color='moss', image='img:moss'),
        'west': flank(36, 20, 3), 'east': flank(36, 20, 7), 'north': mc('belly', clusters=0.3), 'south': flank(32, 20, 11),
    })
    # mossy cyan fur hanging round the belly
    body.cube((-17, -7, -19), (34, 9, 38), **fur, fringe=2, faces={'up': dict(skip=True), 'down': dict(skip=True)})
    # the moss carpet on its back, hanging over the edges, strewn with petals, with three spiracles
    hump = body.part('hump', pivot=(0, -20, 0))
    spiracle = ['..rrrr..', '.rHHHHr.', 'rHhhhhHr', 'rHhGGhHr', 'rHhGGhHr', 'rHhhhhHr', '.rHHHHr.', '..rrrr..']
    up_map = ['.' * 48 for _ in range(52)]
    for (cx, cy) in ((12, 10), (28, 10), (20, 26)):
        up_map = put(up_map, cx, cy, spiracle)
    for i, (px, py) in enumerate(_PETALS):
        up_map = put(up_map, px, py, ['.P.', 'PRQ', '.P.'] if i % 2 else ['QP', 'PR'])
    side_moss = dict(color='fur', pattern='mc', clusters=0.3, streaks=0.5, image='img:moss', image_rows=4)
    hump.cube((-12, -6, -13), (24, 6, 26), color='moss', faces={
        'up': dict(color='moss', image='img:turf', hd=True, map=up_map,
                   keys={'r': 'ring', 'H': 'moss_d', 'h': 'hole', 'G': 'glow', 'P': 'petal', 'Q': 'petal_l', 'R': 'petal_d'}, glow_keys='G'),
        'north': side_moss, 'south': side_moss, 'east': side_moss, 'west': side_moss, 'down': dict(skip=True),
    })
    # a lumpy second layer of moss
    moss = dict(color='moss', image='img:moss')
    hump.cube((-8, -8, -8), (16, 2, 13), color='moss', faces={'up': moss, 'north': moss, 'south': moss, 'east': moss, 'west': moss,
                                                              'down': dict(skip=True)})
    for i, (sx, sz) in enumerate(((-6, -5), (6, -5), (0, 3))):
        sp = hump.part(f'spiracle_{i}', pivot=(sx * 0.75, -8, sz))
        sp.cube((-1.5, -1.5, -1.5), (3, 1.5, 3), color='fur_d', pattern='mc', clusters=0.0, rim=False, faces={
            'up': mc('hole', clusters=0.0, rim=False, hd=True, map=['rrrrrr', 'rhhhhr', 'rhGGhr', 'rhGGhr', 'rhhhhr', 'rrrrrr'],
                     keys={'r': 'ring', 'h': 'hole', 'G': 'glow'}, glow_keys='G'),
        })
    # the garden: blooms, coral and sprouts on cross planes that sway (see StomperModel)
    for i, (tex, px, py, pz, w, h) in enumerate(STOMPER_GARDEN):
        kplant(hump, f'plant_{i}', (px, py, pz), f'img:{tex}', w, h, rot=(0, 0.6 * i, 0), glow_bright=235 if tex in _GLOWING else None)
    # more growing on its shoulders and rump
    for i, (tex, px, pz, w, h) in enumerate(STOMPER_SHOULDERS):
        kplant(body, f'body_plant_{i}', (px, -20, pz), f'img:{tex}', w, h, rot=(0, 0.9 * i + 0.3, 0), glow_bright=235 if tex in _GLOWING else None)
    # the baby's little drum, strapped on its rump (StomperModel shows it only on babies, who tap it
    # with their tail for their owner)
    drum = body.part('baby_drum', pivot=(0, -20, 14))
    zig = ''.join('Z' if (i % 4) in (0, 1) else '.' for i in range(28))
    zag = ''.join('Z' if (i % 4) in (2, 3) else '.' for i in range(28))
    drum_side = mc('drum', clusters=0.2, hd=True, map=['RRRRRRRRRRRRRRRRRRRRRRRRRRRR', 'rrrrrrrrrrrrrrrrrrrrrrrrrrrr', zig, zag, zig, zag, '................................',
                                                      'rrrrrrrrrrrrrrrrrrrrrrrrrrrr', 'RRRRRRRRRRRRRRRRRRRRRRRRRRRR', '............................'],
                   keys={'R': 'ring', 'r': 'ring_d', 'Z': 'teeth'})
    drum.cube((-7, -5, -4), (14, 5, 8), **mc('drum', clusters=0.2), faces={
        'up': mc('drumhead', clusters=0.0, rim=False, hd=True, map=put(blank(28, 16), 9, 5, ['..dddddd..', '.d......d.', '.d......d.', '..dddddd..']),
                 keys={'d': 'drumhead_d'}),
        'north': drum_side, 'south': drum_side,
        'east': mc('drum', clusters=0.2, hd=True, map=[r[:16] for r in drum_side['map']], keys=drum_side['keys']),
        'west': mc('drum', clusters=0.2, hd=True, map=[r[:16] for r in drum_side['map']], keys=drum_side['keys']),
        'down': dict(skip=True)})
    # tail with a mossy tuft
    tail = body.part('tail', pivot=(0, -15, 18), rot=(-0.7, 0, 0))
    tail.cube((-1.5, -1.5, 0), (3, 3, 7), **mc('skin', clusters=0.3))
    tail.cube((-2.5, -2.5, 6), (5, 5, 4), **fur, fringe=1)

    # ---- head
    head = body.part('head', pivot=(0, -6, -18))
    head_side = mc('skin', clusters=0.3, spots=0.15, accent='spot', hd=True, map=warts(28, 24, 21, 9, top=20), keys=vk)
    head.cube((-15, -10, -13), (30, 12, 14), **skin, faces={
        'north': mc('skin', clusters=0.2, spots=0.08, accent='spot', hd=True, map=stomper_mouth(''),
                    keys={'m': 'mouth', 'L': 'lip', 'b': 'blush', 'f': 'freckle', 'w': 'skin_d', 'W': 'wart', 't': 'teeth'},
                    expr={e: stomper_mouth(e) for e in EXPRS}),
        'east': head_side, 'west': dict(head_side, map=warts(28, 24, 22, 9, top=20)),
        'down': mc('belly', clusters=0.2),
        'up': dict(color='moss', image='img:moss'),
    })
    # a mossy brow, a little bloom and some sprouts growing on it
    head.cube((-12, -13, -11), (24, 4, 12), color='moss', faces={
        'up': dict(color='moss', image='img:turf'), 'north': dict(color='fur', pattern='mc', clusters=0.2, streaks=0.5, image='img:moss', image_rows=2),
        'east': moss, 'west': moss, 'south': moss})
    for i, (tex, px, pz, w, h) in enumerate(STOMPER_BROW):
        kplant(head, f'head_plant_{i}', (px, -13, pz), f'img:{tex}', w, h, rot=(0, 0.5 - 0.9 * i, 0), glow_bright=235 if tex in _GLOWING else None)
    jaw = head.part('jaw', pivot=(0, 2, 0))
    jaw.cube((-15, 0, -13), (30, 5, 14), **mc('belly', clusters=0.3), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, map=put(blank(60, 28), 18, 6, [
            '......tttttttttttttttttt......',
            '....ttTTTTTTTTTTTTTTTTTTtt....',
            '...tTTTTTTTTTTTTTTTTTTTTTTt...',
            '...TTTTTTTTTTTTTTTTTTTTTTTT...',
            '....TTTTTTTTTTTTTTTTTTTTTT....',
            '.....TTTTTTTTTTTTTTTTTTTT.....',
            '.......TTTTTTTTTTTTTTTT.......',
            '..........TTTTTTTTTT..........']), keys={'T': 'tongue', 't': 'teeth'}),
        'north': mc('belly', clusters=0.2, hd=True, map=['L' * 60, '.' * 60, '.' * 60, '..' + 'W.........' * 5 + '........'],
                    keys={'L': 'lip', 'W': 'belly_d'}),
        'west': mc('skin', clusters=0.3, bands=[(1, 'belly')]),
        'east': mc('skin', clusters=0.3, bands=[(1, 'belly')]),
        'down': mc('belly_d', clusters=0.2),
    })
    # the bullfrog's throat sac: it swells when the Stomper sings, dances or drinks
    sac = jaw.part('throat', pivot=(0, 5, -6))
    sac.cube((-9, -1, -5), (18, 5, 10), **mc('belly', clusters=0.15, rim=False), faces={'north': mc('belly', clusters=0.1, rim=False, hd=True,
              map=['.' * 36] * 2 + ['....' + 'd...' * 7 + '....'] + ['.' * 36] * 7, keys={'d': 'belly_d'})})
    # CR1: its two eyes - big glossy frog domes at the front corners of its brow
    eyekeys = {'r': 'skin_d', 'c': 'lid_d', 'j': 'iris_l', 'i': 'iris', 'I': 'iris_d', 'o': 'eye_ring', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid',
               'd': 'lid_d'}
    for side, sx in (('left', 1), ('right', -1)):
        mir = sx < 0
        big = head.part(f'{side}_eye', pivot=(8.5 * sx, -11.5, -8.5))
        big.cube((-4, -5, -4), (8, 6, 8), **mc('skin', clusters=0.2), faces={
            'north': mc('skin', clusters=0.0, hd=True, map=stomper_eye(16, 12, '', mir), keys=eyekeys,
                        expr={e: stomper_eye(16, 12, e, mir) for e in EXPRS}, glow_keys='jiIo'),
            ('east' if sx > 0 else 'west'): mc('skin', clusters=0.0, hd=True, map=stomper_eye(16, 12, '', not mir, side=True), keys=eyekeys,
                                                 expr={e: stomper_eye(16, 12, e, not mir, side=True) for e in EXPRS}, glow_keys='jiIo'),
            'up': moss,
        })
        lid = big.part(f'{side}_eyelid')
        lid.cube((-4, -5, -4), (8, 3, 8), inflate=0.15, **mc('lid', clusters=0.0, rim=False), faces={
            'north': mc('lid', clusters=0.0, rim=False, hd=True, map=['.' * 16] * 5 + ['d' * 16], keys={'d': 'lid_d'})})
        ear = head.part(f'{side}_ear', pivot=(15 * sx, -8, -4), rot=(0, -0.3 * sx, 0.35 * sx))
        ear.cube((0 if sx > 0 else -2, -1, -4), (2, 10, 8), **fur, fringe=2, faces={
            ('west' if sx > 0 else 'east'): mc('blush', clusters=0.2),
        })
    # the trunk: four pink segments with cyan rings, curling forward at the tip
    trunk = head
    widths = [(8, 6, 7), (7, 5, 6), (6, 5, 5), (5, 4, 4)]
    curl = [(-0.1, 0), (-0.2, 0), (-0.35, 0), (-0.6, 0)]
    pivot = (0, -5, -14)
    for i, (w, h, d) in enumerate(widths):
        seg = trunk.part(f'trunk_{i}', pivot=pivot, rot=(curl[i][0], 0, 0))
        ring_map = ['.' * (w * 2)] * (h * 2 - 3) + ['R' * (w * 2), 'r' * (w * 2), 'R' * (w * 2)]
        faces = {f: mc('trunk', clusters=0.15, hd=True, map=ring_map, keys={'R': 'ring_d', 'r': 'ring'}) for f in ('north', 'south', 'east', 'west')}
        if i == 3:
            faces['down'] = mc('trunk_d', clusters=0.0, rim=False, hd=True, map=['..rrrrrr..', '.rRRRRRRr.', 'rRhhRRhhRr', 'rRhhRRhhRr', '.rRRRRRRr.',
                                                                                 '..rrrrrr..', '..........', '..........'],
                               keys={'r': 'ring', 'R': 'trunk_l', 'h': 'hole'})
        seg.cube((-w / 2, 0, -d / 2), (w, h, d), **mc('trunk', clusters=0.15), faces=faces)
        trunk = seg
        pivot = (0, h, 0)

    # ---- legs: cyan-furred pillars on pale stone pads, sculk veins glowing up the fur
    for li, (name, x, z) in enumerate((('front_left', 1, -1), ('front_right', -1, -1), ('back_left', 1, 1), ('back_right', -1, 1))):
        leg = body.part(f'{name}_leg', pivot=(11 * x, -2, 10 * z))
        leg.cube((-5, 0, -5), (10, 11, 10), **fur, fringe=2, fringe_phase=3 if x > 0 else 0, faces={
            'north': mc('fur', clusters=0.25, streaks=0.55, fringe=2, map=veins(10, 11, 20 + li, 4), keys=vk, glow_keys='g'),
            'up': moss})
        foot = leg.part(f'{name}_foot', pivot=(0, 9, 0))
        foot.cube((-5.5, 0, -5.5), (11, 4, 11), **mc('stone', clusters=0.3), faces={
            'north': mc('stone', clusters=0.2, hd=True, map=['......................'] * 3 + ['.NN...NNN...NNN...NN..', 'NNNN.NNNNN.NNNNN.NNNN.',
                                                                                          'NnnN.NnnnN.NnnnN.NnnN.', '......................',
                                                                                          '......................'],
                        keys={'N': 'nail', 'n': 'nail_d'}),
            'down': mc('stone_d', clusters=0.0),
        })
    return m


# =========================================================================== SKY WHALE
def sky_whale() -> Model:
    """The Sky Whale: a vast, gentle flying whale-bull. A periwinkle hide with a blush-pink belly,
    glowing sculk markings swirling down its flanks, clouds of fluffy cyan-white fur, a lumen-moss
    meadow on its back full of blooms, glowbell vines trailing from its sides and flippers, curling
    pale-stone horns with glowing tips and a pink bull snout with a ring of light."""
    pal = {
        'hide': '#78a5e3', 'hide_l': '#9cc3f3', 'hide_d': '#5d86cc', 'belly': '#ffd9e8', 'belly_l': '#fff0f6', 'belly_d': '#efb3cb',
        'fur': '#e8fbff', 'fur_l': '#ffffff', 'fur_d': '#a9dff0', 'pinkfur': '#ffd0e6', 'pinkfur_l': '#fff0f8', 'pinkfur_d': '#f0a8cc',
        'horn': '#e3ddcc', 'horn_l': '#f4f0e5', 'horn_d': '#b3ab96',
        'tip': '#2ef2e2', 'snout': '#f59ab8', 'snout_l': '#ffc0d6', 'snout_d': '#d06f94', 'nostril': '#5a2f4a',
        'sculk': '#12303a', 'sculk_l': '#1f5a66', 'glow': '#3ff5e6', 'glow2': '#ffd6f5', 'eye': '#1a1830', 'eye_hi': '#ffffff',
        'iris': '#3fd8e8', 'iris_d': '#2a8ac8', 'lid': '#8fb3ec', 'lid_d': '#3f4f8a', 'mouth': '#4a2a5a', 'tongue': '#f59ab8',
        'fin': '#9cc3f3', 'fin_l': '#c8e0ff', 'fin_d': '#5d86cc', 'ring': '#ffe08a', 'baleen': '#e9dcc0', 'baleen_d': '#b8a684', 'ear_in': '#ffb8d6', 'moss': '#50c8bb', 'plant': '#50c8bb',
    }
    m = Model('sky_whale', (256, 256), pal, {'sky_whale': {}}, res=2, expressions=EXPRS)
    hide = mc('hide', clusters=0.2)
    fur = mc('fur', clusters=0.3, streaks=0.4)
    pink = mc('pinkfur', clusters=0.3, streaks=0.4)
    moss_img = block_tex('lumen_moss_block')
    turf_img = block_tex('sift_grass_block_top')
    sk = {'v': 'sculk', 'V': 'sculk_l', 'g': 'glow', 'G': 'glow2'}
    body = m.part('body', pivot=(0, 4, 0))
    body.cube((-16, -14, -22), (32, 26, 44), **hide, faces={
        'down': mc('belly', clusters=0.25),
        'up': dict(color='moss', image=turf_img),
        'west': mc('hide', clusters=0.2, bands=[(16, 'belly')], map=swirls(44, 26, 5), keys=sk, glow_keys='gG',
                   image=moss_img, image_rows=2),
        'east': mc('hide', clusters=0.2, bands=[(16, 'belly')], map=[r[::-1] for r in swirls(44, 26, 5)], keys=sk, glow_keys='gG',
                   image=moss_img, image_rows=2),
        'north': mc('hide', clusters=0.2, bands=[(16, 'belly')], image=moss_img, image_rows=2),
        'south': mc('hide', clusters=0.2, bands=[(16, 'belly')], image=moss_img, image_rows=2),
    })
    # long belly grooves from the chin to the navel, like a rorqual's: they balloon out when it sings
    pleats = body.part('throat_pleats', pivot=(0, 12, -6))
    groove = [''.join('d' if i % 4 == 1 else ('l' if i % 4 == 2 else '.') for i in range(52))] * 64
    pleats.cube((-13, -1, -15), (26, 3, 32), **mc('belly', clusters=0.0, rim=False), faces={
        'down': mc('belly', clusters=0.0, rim=False, hd=True, map=groove, keys={'d': 'belly_d', 'l': 'belly_l'}),
        'east': mc('belly', clusters=0.0, rim=False, hd=True, map=['d.' * 32] * 6, keys={'d': 'belly_d'}),
        'west': mc('belly', clusters=0.0, rim=False, hd=True, map=['d.' * 32] * 6, keys={'d': 'belly_d'}),
        'up': dict(skip=True)})
    # big cloud-puffs of fur on its mossy back
    for i, (x, z, w, h, d) in enumerate(((-6, -14, 14, 6, 12), (5, 0, 16, 7, 12), (-4, 13, 12, 6, 10))):
        tuft = body.part(f'back_tuft_{i}', pivot=(x, -14, z))
        style = pink if i == 1 else fur
        tuft.cube((-w / 2, -h + 1, -d / 2), (w, h, d), **style, fringe=2, fringe_phase=i * 2)
        tuft.cube((-w / 2 + 2, -h - 2, -d / 2 + 2), (w / 2 + 1, 3, d / 2), **style, fringe=1)
        tuft.cube((1, -h - 1, -2), (w / 2 - 2, 2, d / 2), **fur, fringe=1)
    # a meadow of blooms in the moss
    meadow = [('dreambloom', 10, -14, -15, 9, 10), ('lullaby_bell', -12, -14, -4, 9, 10), ('soulpetal', 11, -14, 10, 8, 9),
              ('echo_orchid', -11, -14, 9, 8, 9), ('dreambloom', -1, -14, 19, 8, 9), ('glimmer_sprouts', 12, -14, -3, 8, 8),
              ('lullaby_bell', 3, -14, -19, 8, 9), ('soulpetal', -13, -14, -16, 8, 8), ('glimmer_sprouts', -6, -14, 3, 7, 7),
              ('dreambloom', 13, -14, 18, 7, 8)]
    for i, (tex, px, py, pz, w, h) in enumerate(meadow):
        plant(body, f'back_flower_{i}', (px, py, pz), tex, w, h, rot=(0, 0.7 * i, 0),
              glow_bright=230 if tex in ('glimmer_sprouts', 'echo_orchid', 'soulpetal') else None)
    # glowbell vines trailing from its flanks
    for i, (x, z, h, tex) in enumerate(((17, -12, 16, 'glowbell_vine'), (17, 4, 12, 'hanging_lullwood_leaves'), (17, 16, 14, 'glowbell_vine'),
                                        (-17, -8, 14, 'hanging_lullwood_leaves'), (-17, 8, 16, 'glowbell_vine'), (-17, 18, 11, 'glowbell_vine'))):
        vine(body, f'vine_{i}', (x, -13, z), tex, 7, h, glow_bright=200)
    # ---- head
    head = body.part('head', pivot=(0, -1, -22))
    head.cube((-15, -12, -15), (30, 24, 15), **hide, faces={
        'down': mc('belly', clusters=0.2),
        'up': dict(color='moss', image=moss_img),
        'north': mc('hide', clusters=0.15, bands=[(15, 'belly')], map=put(blank(30, 24), 3, 2, ['g.....................g', '.v...................v.']),
                    keys=sk, glow_keys='gG'),
        'west': mc('hide', clusters=0.2, bands=[(15, 'belly')]),
        'east': mc('hide', clusters=0.2, bands=[(15, 'belly')]),
    })
    # forehead mop between the horns, with a bloom tucked in
    head.cube((-10, -16, -14), (20, 6, 12), **fur, fringe=2, faces={'north': mc('fur', clusters=0.25, streaks=0.4, fringe=3)})
    plant(head, 'head_flower', (4, -16, -9), 'dreambloom', 8, 9, rot=(0, 0.4, 0))
    snout = head.part('snout', pivot=(0, 3, -15))
    nostril = ['..nnnn..', '.nNNNNn.', 'nNNNNNNn', '.nNNNNn.', '..nnnn..']
    snout.cube((-10, -5, -6), (20, 10, 6), **mc('snout', clusters=0.12), faces={
        'north': mc('snout', clusters=0.0, hd=True, map=put(put(put(blank(40, 20), 5, 4, nostril), 27, 4, nostril), 4, 14, ['l' * 32]),
                    keys={'n': 'snout_d', 'N': 'nostril', 'l': 'snout_d'}),
        'up': mc('snout_l', clusters=0.12),
    })

    def hoop(x, y, u, v):
        return 'R' if 0.55 < u * u + v * v <= 1.0 and v > -0.85 else '_'
    ring = snout.part('nose_ring', pivot=(0, 1, -6), rot=(0.15, 0, 0))
    ring.cube((-3, 0, -0.5), (6, 6, 1), **mc('ring', clusters=0.0, rim=False, glow=True), faces={
        'north': mc('ring', clusters=0.0, rim=False, glow=True, hd=True, map=rows_of(12, 12, hoop), keys={'R': 'ring'}),
        'south': mc('ring', clusters=0.0, rim=False, glow=True, hd=True, map=rows_of(12, 12, hoop), keys={'R': 'ring'}),
    })
    baleen_side = ['BbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBb', 'BbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBbBb',
                   'B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_', 'B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_B_',
                   '_B___B___B___B___B___B___B___B___B___B___B___B___B', '__________________________________________________']
    baleen = head.part('baleen', pivot=(0, 8, -1))
    baleen.cube((-12.5, 0, -17), (25, 3, 16), **mc('baleen', clusters=0.0, rim=False), faces={
        'north': mc('baleen', clusters=0.0, rim=False, hd=True, map=baleen_side, keys={'B': 'baleen', 'b': 'baleen_d'}),
        'east': mc('baleen', clusters=0.0, rim=False, hd=True, map=[r[:32] for r in baleen_side], keys={'B': 'baleen', 'b': 'baleen_d'}),
        'west': mc('baleen', clusters=0.0, rim=False, hd=True, map=[r[:32] for r in baleen_side], keys={'B': 'baleen', 'b': 'baleen_d'}),
        'up': dict(skip=True), 'south': dict(skip=True), 'down': dict(skip=True)})
    jaw = head.part('jaw', pivot=(0, 8, 0))
    jaw.cube((-14, 0, -19), (28, 5, 19), **mc('belly', clusters=0.2), faces={
        'up': mc('mouth', clusters=0.0, rim=False, hd=True, map=put(blank(56, 38), 14, 8, [
            '....TTTTTTTTTTTTTTTTTTTT....', '..TTTTTTTTTTTTTTTTTTTTTTTT..', '.TTTTTTTTTTTTTTTTTTTTTTTTTT.', 'TTTTTTTTTTTTTTTTTTTTTTTTTTTT',
            'TTTTTTTTTTTTTTTTTTTTTTTTTTTT', '.TTTTTTTTTTTTTTTTTTTTTTTTTT.', '..TTTTTTTTTTTTTTTTTTTTTTTT..', '....TTTTTTTTTTTTTTTTTTTT....']),
            keys={'T': 'tongue'}),
        'west': mc('hide', clusters=0.2, bands=[(2, 'belly')]),
        'east': mc('hide', clusters=0.2, bands=[(2, 'belly')]),
    })
    jaw.cube((-7, 5, -17), (14, 4, 10), **pink, fringe=2)
    for side, sx in (('left', 1), ('right', -1)):
        mir = sx < 0
        ek = {'r': 'eye', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}
        e = head.part(f'{side}_eye', pivot=(15 * sx, -3, -9))
        e.cube((-1, -4, -4), (2, 8, 8), **mc('hide', clusters=0.0), faces={
            ('east' if sx > 0 else 'west'): mc('hide', clusters=0.0, hd=True, map=eye(16, 16, '', not mir, pupil='dot', rim=0.8), keys=ek,
                                                 expr=eye_exprs(16, 16, not mir, pupil='dot', rim=0.8), glow_keys='iI'),
        })
        cheek = head.part(f'{side}_cheek', pivot=(14 * sx, 5, -8), rot=(0, 0, -0.2 * sx))
        cheek.cube((0 if sx > 0 else -5, -5, -6), (5, 10, 12), **pink, fringe=2)
        ear = head.part(f'{side}_ear', pivot=(15 * sx, -7, -5), rot=(0, 0, 0.5 * sx))
        ear.cube((0 if sx > 0 else -7, -1, -2.5), (7, 2, 5), **mc('hide', clusters=0.1), faces={'down': mc('ear_in', clusters=0.1)})
        horn = head.part(f'{side}_horn', pivot=(12 * sx, -12, -7), rot=(0, 0.2 * sx, -0.25 * sx))
        horn.cube((0 if sx > 0 else -8, -2.5, -2.5), (8, 5, 5), **mc('horn', clusters=0.2), faces={
            'up': dict(color='moss', image=moss_img)})
        tip = horn.part(f'{side}_horn_tip', pivot=(7.5 * sx, 0, 0), rot=(-0.3, 0, -0.9 * sx))
        tip.cube((0 if sx > 0 else -7, -2, -2), (7, 4, 4), **mc('horn', clusters=0.15))
        tip2 = tip.part(f'{side}_horn_point', pivot=(6.5 * sx, 0, 0), rot=(-0.2, 0, -0.7 * sx))
        tip2.cube((0 if sx > 0 else -5, -1.5, -1.5), (5, 3, 3), **mc('tip', clusters=0.1, glow=True))
        fl = body.part(f'{side}_flipper', pivot=(16 * sx, 6, -10), rot=(0, 0, 0.3 * sx))
        fl.cube((0 if sx > 0 else -20, -1, -7), (20, 2, 15), **mc('fin', clusters=0.2), faces={
            'up': mc('fin', clusters=0.2, ribs=5, accent='fin_d', alpha='membrane', edge='outer', edge_depth=3, scallop=4,
                     map=put(blank(20, 15), 2 if sx > 0 else 4, 3, ['g...g...g...g', '.............', '..g...g...g..']), keys=sk, glow_keys='g'),
            'down': mc('belly', clusters=0.2, alpha='membrane', edge='outer', edge_depth=3, scallop=4),
        })
        fl.cube((0 if sx > 0 else -7, -2.5, -6), (7, 4, 12), **fur, fringe=1)
        vine(fl, f'{side}_flipper_vine', (13 * sx, 1, 2), 'glowbell_vine', 6, 12, glow_bright=200)
    # ---- tail and flukes
    t1 = body.part('tail1', pivot=(0, -1, 22))
    t1.cube((-12, -10, 0), (24, 20, 12), **hide, faces={'down': mc('belly', clusters=0.25), 'up': dict(color='moss', image=moss_img),
                                                         'west': mc('hide', clusters=0.2, bands=[(13, 'belly')], map=swirls(12, 20, 9), keys=sk,
                                                                    glow_keys='gG'),
                                                         'east': mc('hide', clusters=0.2, bands=[(13, 'belly')], map=swirls(12, 20, 13), keys=sk,
                                                                    glow_keys='gG')})
    t1.cube((-6, -15, 0), (12, 6, 10), **fur, fringe=2)
    dorsal = t1.part('dorsal_fin', pivot=(0, -14, 6), rot=(-0.55, 0, 0))
    dorsal.cube((-1, -9, -3), (2, 9, 9), **mc('fin', clusters=0.2), faces={
        'east': mc('fin', clusters=0.2, ribs=3, accent='fin_d', alpha='membrane', edge='outer', edge_depth=2, scallop=3),
        'west': mc('fin', clusters=0.2, ribs=3, accent='fin_d', alpha='membrane', edge='outer', edge_depth=2, scallop=3)})
    for side, sx in (('left', 1), ('right', -1)):
        sf = t1.part(f'{side}_tail_fin', pivot=(12 * sx, 6, 6), rot=(0, 0, 0.45 * sx))
        sf.cube((0 if sx > 0 else -9, -0.5, -3), (9, 1, 7), **mc('fin', clusters=0.2), faces={
            'up': mc('fin', clusters=0.2, ribs=3, accent='fin_d', alpha='membrane', edge='outer', edge_depth=2, scallop=3),
            'down': mc('belly', clusters=0.2, alpha='membrane', edge='outer', edge_depth=2, scallop=3)})
    t2 = t1.part('tail2', pivot=(0, 0, 12))
    t2.cube((-8, -7, 0), (16, 14, 10), **hide, faces={'down': mc('belly', clusters=0.25),
                                                       'west': mc('hide', clusters=0.2, bands=[(9, 'belly')]),
                                                       'east': mc('hide', clusters=0.2, bands=[(9, 'belly')])})
    t3 = t2.part('tail3', pivot=(0, 0, 10))
    t3.cube((-5, -4, 0), (10, 8, 8), **hide, faces={'down': mc('belly', clusters=0.25)})
    fluke = t3.part('flukes', pivot=(0, 0, 7))
    fluke.cube((-25, -1.5, -2), (50, 3, 18), **mc('fin', clusters=0.2), faces={
        'up': mc('fin', clusters=0.2, ribs=5, accent='fin_d', alpha='membrane', edge='bottom', edge_depth=3, scallop=7,
                 map=put(blank(50, 18), 6, 6, ['g.....g.....g.....g.....g.....g.....g', '...g.....g.....g.....g.....g.....g...']), keys=sk,
                 glow_keys='g'),
        'down': mc('belly', clusters=0.2, alpha='membrane', edge='bottom', edge_depth=3, scallop=7),
    })
    fluke.cube((-4, -4, -1), (8, 6, 7), **pink, fringe=1)
    return m


# =========================================================================== FISH
# CR3 Fish & Coral Organs: the music fish are hand-drawn sprites extruded into rounded bodies (tools/fish_art.py)
from fish_art import fanfare_eel, kazoo_fish, tubafish  # noqa: E402


ALL = {'stomper': stomper, 'sky_whale': sky_whale, 'fanfare_eel': fanfare_eel, 'kazoo_fish': kazoo_fish, 'tubafish': tubafish}
ALL['gobbler'] = __import__('gobbler').gobbler  # sea & sky (F): the blind deep-sea catfish, tools/gobbler.py
ALL['sculk_fish'] = __import__('fish_art').sculk_fish  # CR3: the Sculk Ocean's biting schools
ALL['coral_organ'] = __import__('fish_art').coral_organ  # CR3: the living Sculk Coral Organ
