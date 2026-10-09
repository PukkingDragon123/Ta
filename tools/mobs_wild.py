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
# S1 Stomper remake: the elephant lives in tools/stomper.py
from stomper import stomper  # noqa: E402,F401


# =========================================================================== SKY WHALE
def sky_whale() -> Model:
    """CR2: the Sky Whale, a real whale of the sky (tools/sky_whale.py)."""
    return __import__('sky_whale').sky_whale()


# =========================================================================== FISH
ALL = {'stomper': stomper, 'sky_whale': sky_whale}
# S2: the water creatures (Kazoo Fish, Fanfare Eel, Sculk Fish, Gobbler, Sculk Coral Organ), hand-painted in tools/waterfolk.py
ALL.update({k: v for k, v in __import__('waterfolk').MODELS.items() if k != 'cypole'})
