"""E1 Sniffer & rot: the Sift Sniffer (model, textures, egg block, spawn egg, sounds, loot, text) and
the rot overlays every Sift creature wears when it rots outside the Sift.

The Sift Sniffer is Mojang's Sniffer, re-themed: the exact vanilla geometry (SnifferModel and
SniffletModel createBodyLayer - the same parts, pivots, boxes and texture offsets, so the vanilla
SnifferAnimation keyframes drive it) and the vanilla 192x192 / 128x128 textures recoloured pixel for
pixel - the red coat becomes soft cream-white fur with pink patches, the green moss becomes Sift pink
grass, the gold beak a pink snout - plus crossed-plane flowers and plants growing on its back and
shaggy fur fringes (flat planes) round its belly. The egg block, its item and the spawn egg are the
vanilla Sniffer egg / spawn egg sprites recoloured the same way.

Hooked in from one line each in spec.py, mobs.py, gen_assets.py, gen_textures.py and items16.py.
"""
import math
import os
import random

from PIL import Image

from modelkit import Model

NS = 'thesift'
# Mojang's textures (the Sniffer, the Snifflet, the Sniffer egg and its sprites) are the starting point
VANILLA = os.environ.get('VANILLA_TEXTURES', '/home/user/ref/mc-tex/assets/minecraft/textures')
_ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/thesift/textures')


def declare(block, item):
    block('sift_sniffer_egg', 'custom', 'BlockBehaviour.Properties.ofFullCopy(Blocks.SNIFFER_EGG).mapColor(MapColor.COLOR_PINK).noOcclusion()',
          cls='SiftSnifferEggBlock', model='sift_sniffer_egg', tab='nature')
    item('sift_sniffer_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModSiftSniffer.SIFT_SNIFFER.get())', tab='eggs')


def _hx(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def _vanilla(path):
    p = os.path.join(VANILLA, path + '.png')
    return Image.open(p).convert('RGBA') if os.path.exists(p) else None


# ============================================================================ the palette
#
# Every vanilla colour belongs to a family (the coat, the moss, the beak, the claws, the nostrils, the
# eyes) and keeps its rank inside it, so Mojang's shading - strand by strand, cluster by cluster - stays
# exactly where it was and only the colours change.

FUR = ['#8c706e', '#ae948d', '#cab2a9', '#ddcbc1', '#ece0d5', '#f6efe5', '#fffbf2']      # cream-white coat
PATCH = ['#894a66', '#a6607d', '#c27794', '#d68ea8', '#e6a5bb', '#f2bccd', '#fcd5e0']    # its pink patches
GRASS = ['#843a60', '#97466d', '#a9537b', '#ba628a', '#c97298', '#d784a7', '#e398b7', '#eeafc8', '#f8c7da']  # Sift pink grass
BEAK = ['#8d3c5e', '#a24b6e', '#b45a7d', '#c4698b', '#d27a99', '#dd8aa6', '#e69bb3', '#eeacc0', '#f5bdcd', '#fbcfda', '#ffe0e7']  # the pink snout tip
PEACH = ['#8a4e3c', '#a05f44', '#b4714e', '#c4835b', '#d29569', '#dda679', '#e6b68a', '#edc59c', '#f3d2ae', '#f8dfc3', '#fcecd9']  # the beak
CLAW = ['#2c2128', '#3a2b34', '#4a3843', '#5c4855', '#705a67']                            # claws and soles
DARK = ['#3e1529', '#521d36', '#672844', '#7e3454']                                       # nostrils, the mouth line
EYE = '#1d1117'

# the Sniffer's coat, darkest to lightest (fixed, so the adult and the Snifflet match)
FUR_TONES = {(60, 7, 6): 0, (65, 8, 8): 1, (80, 8, 8): 2, (96, 13, 13): 3, (113, 19, 19): 4, (129, 33, 24): 5, (147, 49, 29): 6}
SPECIAL = {(8, 7, 6): ('eye', 0), (68, 43, 17): ('dark', 0), (91, 54, 9): ('dark', 1), (113, 65, 2): ('dark', 2), (146, 65, 0): ('dark', 3)}
RAMPS = {'fur': FUR, 'patch': PATCH, 'grass': GRASS, 'beak': PEACH, 'tip': BEAK, 'claw': CLAW, 'dark': DARK, 'eye': [EYE]}


def _lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def _family(c):
    r, g, b = c[:3]
    if (r, g, b) in SPECIAL:
        return SPECIAL[(r, g, b)][0]
    if (r, g, b) in FUR_TONES:
        return 'fur'
    if g > r and g >= b * 0.85:
        return 'grass'          # the moss (teal highlights included)
    if r > 130 and g > r * 0.4 and b < g * 0.7:
        return 'beak'
    if r > 2 * g and r > 2 * b and r > 45:
        return 'fur'
    if b >= r * 0.9 and r < 100:
        return 'claw'
    return 'grass'              # the olive pixels where the moss meets the coat


def _ranks(colours, ramp_len):
    """Rank-preserving map of a family's vanilla colours onto a ramp of ramp_len tones."""
    cs = sorted(set(colours), key=_lum)
    n = len(cs)
    return {c: (round(i * (ramp_len - 1) / (n - 1)) if n > 1 else ramp_len // 2) for i, c in enumerate(cs)}


def classify(img):
    """(family, tone) for every opaque texel of a vanilla texture, as a dict {(x, y): (fam, idx)}."""
    px = img.load()
    fams = {}
    for y in range(img.height):
        for x in range(img.width):
            c = px[x, y]
            if c[3] >= 128:
                fams[(x, y)] = (_family(c), c[:3])
    out = {}
    by = {}
    for (x, y), (f, c) in fams.items():
        by.setdefault(f, []).append(c)
    rk = {f: _ranks(cs, len(RAMPS[f])) for f, cs in by.items() if f not in ('fur', 'eye', 'dark')}
    for (x, y), (f, c) in fams.items():
        if f == 'fur':
            i = FUR_TONES.get(c)
            if i is None:      # an odd red: nearest coat tone by brightness
                i = min(FUR_TONES.items(), key=lambda kv: abs(_lum(kv[0]) - _lum(c)))[1]
        elif f in ('eye', 'dark'):
            i = SPECIAL[c][1]
        else:
            i = rk[f][c]
        out[(x, y)] = (f, i)
    return out


def recolour(img, choose=None):
    """A vanilla texture re-themed; choose(x, y, family) may swap a texel's family for another ramp of
    the same length (the coat's pink patches, the snout's pink tip)."""
    cls = classify(img)
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    px = out.load()
    for (x, y), (f, i) in cls.items():
        if choose is not None:
            f = choose(x, y, f)
        px[x, y] = _hx(RAMPS[f][i])
    return out


# ============================================================================ the model

EXPRS = ('blink', 'happy', 'angry', 'hurt', 'dead')


class _Mojang(Model):
    """A model on Mojang's texture layout: every box keeps the texture offset it was given (no packing)
    and the texture is painted by `paint` (the vanilla one re-themed) instead of the modelkit painter."""

    def __init__(self, name, size, paint):
        super().__init__(name, size, {}, {name: {}}, expressions=list(EXPRS))
        self._paint = paint

    def box(self, part, uv, origin, size, inflate=0.0):
        part.cube(origin, size, inflate)
        part.cubes[-1].uv = tuple(uv)
        return part

    def pack(self):
        pass

    def render_textures(self, seed=1):
        return self._paint(self)


def _plant(m, parent, name, pivot, uv, w, h, yrot):
    """A plant on two crossed flat planes standing on `pivot` (both share one 2w x h strip of the texture)."""
    p = parent.part(name, pivot=pivot, rot=(0, yrot, 0))
    u, v = uv
    m.box(p, (u, v), (-w / 2, -h, 0), (w, 0 + h, 0))
    m.box(p, (u, v - w), (0, -h, -w / 2), (0, h, w))
    return p


# the back garden of the grown Sniffer: (sprite, x, z) on the mossy back; the first five are the
# flowers it loses and regrows (SiftSniffer.MAX_GARDEN), the tufts always stay
GARDEN = [('dreambloom', -6, -12), ('hummingbloom', 5.5, -4.5), ('lullaby_bell', -5, 4), ('nebula_iris', 6, 11), ('puffbloom', -2, 15)]
TUFTS = [('blushgrass', 1.5, -15.5), ('glimmer_sprouts', -8, -3), ('blushgrass', 8.5, 4), ('glimmer_sprouts', -7, 13.5)]
FLOWER_W, FLOWER_H, TUFT_W, TUFT_H = 7, 9, 7, 5
# where the extras live in the 192x192 texture (all below Mojang's own layout, which ends at row 140)
SKIRT_UV, SKIRT_END_UV, RIDGE_UV = (0, 99), (84, 140), (0, 111)
FLOWER_UV = {n: (84 + 14 * i, 152) for i, n in enumerate(['dreambloom', 'hummingbloom', 'lullaby_bell', 'nebula_iris', 'puffbloom'])}
TUFT_UV = {'blushgrass': (0, 158), 'glimmer_sprouts': (14, 158)}
LEGS = (('right_front_leg', -7.5, -15, (32, 87)), ('right_mid_leg', -7.5, 0, (32, 105)), ('right_hind_leg', -7.5, 15, (32, 123)),
        ('left_front_leg', 7.5, -15, (0, 87)), ('left_mid_leg', 7.5, 0, (0, 105)), ('left_hind_leg', 7.5, 15, (0, 123)))


def sift_sniffer() -> Model:
    """The grown Sift Sniffer: vanilla SnifferModel.createBodyLayer() box for box, plus its garden
    (five flowers and four tufts on crossed planes) and shaggy fringes (flat planes) hanging round
    its belly and a ridge of grass blades along the edges of its back."""
    m = _Mojang('sift_sniffer', (192, 192), _paint_adult)
    bone = m.part('bone', (0, 5, 0))
    body = bone.part('body', (0, 0, 0))
    m.box(body, (62, 68), (-12.5, -14, -20), (25, 29, 40))
    m.box(body, (62, 0), (-12.5, -14, -20), (25, 24, 40), 0.5)
    m.box(body, (87, 68), (-12.5, 12, -20), (25, 0, 40))
    for name, x, z, uv in LEGS:
        m.box(bone.part(name, (x, 10, z)), uv, (-3.5, -1, -4), (7, 10, 8))
    head = body.part('head', (0, 6.5, -19.48))
    m.box(head, (8, 15), (-6.5, -7.5, -11.5), (13, 18, 11))
    m.box(head, (8, 4), (-6.5, 7.5, -11.5), (13, 0, 11))
    m.box(head.part('left_ear', (6.51, -7.5, -4.51)), (2, 0), (0, 0, -3), (1, 19, 7))
    m.box(head.part('right_ear', (-6.51, -7.5, -4.51)), (48, 0), (-1, 0, -3), (1, 19, 7))
    m.box(head.part('nose', (0, -4.5, -11.5)), (10, 45), (-6.5, -2, -9), (13, 2, 9))
    m.box(head.part('lower_beak', (0, 2.5, -12.5)), (10, 57), (-6.5, -7, -8), (13, 12, 9))
    # --- the Sift's additions: shaggy fringes round the belly (they sway in the Java model)
    m.box(body.part('left_fringe', (12.75, 11, 0)), SKIRT_UV, (0, 0, -20.5), (0, 5, 41))
    m.box(body.part('right_fringe', (-12.75, 11, 0)), SKIRT_UV, (0, 0, -20.5), (0, 5, 41))
    m.box(body.part('front_fringe', (0, 11, -20.25)), SKIRT_END_UV, (-12.5, 0, 0), (25, 5, 0))
    m.box(body.part('back_fringe', (0, 11, 20.25)), SKIRT_END_UV, (-12.5, 0, 0), (25, 5, 0))
    # a ridge of grass blades along both edges of the back
    m.box(body, RIDGE_UV, (12.9, -17.5, -20), (0, 3, 40))
    m.box(body, RIDGE_UV, (-12.9, -17.5, -20), (0, 3, 40))
    # the garden
    garden = body.part('garden', (0, -14.5, 0))
    for i, (tex, x, z) in enumerate(GARDEN):
        _plant(m, garden, f'flower_{i}', (x, 0, z), FLOWER_UV[tex], FLOWER_W, FLOWER_H, 0.6 + 0.9 * i)
    for i, (tex, x, z) in enumerate(TUFTS):
        _plant(m, garden, f'tuft_{i}', (x, 0, z), TUFT_UV[tex], TUFT_W, TUFT_H, 0.3 + 1.3 * i)
    return m


BABY_LEGS = (('right_front_leg', -4, -7, (0, 69)), ('right_mid_leg', -4, 0, (0, 78)), ('right_hind_leg', -4, 7, (0, 87)),
             ('left_front_leg', 4, -7, (16, 69)), ('left_mid_leg', 4, 0, (16, 78)), ('left_hind_leg', 4, 7, (16, 87)))
BABY_SKIRT_UV, BABY_SKIRT_END_UV = (0, 76), (40, 100)
BABY_FLOWER_UV, BABY_TUFT_UV = (68, 100), (80, 100)


def sift_snifflet() -> Model:
    """The Sift Snifflet: vanilla SniffletModel.createBodyLayer() box for box (the renderer applies
    vanilla's BABY_TRANSFORM to its head), a first flower and a sprout on its back and little fringes."""
    m = _Mojang('sift_sniffer_baby', (128, 128), _paint_baby)
    bone = m.part('bone', (0, 24, 0))
    body = bone.part('body', (6, -3, -9.5))
    m.box(body, (0, 35), (-13, -14, -0.5), (14, 14, 20), 0.25)
    m.box(body, (0, 0), (-13, -14, -0.5), (14, 15, 20))
    m.box(body, (68, 0), (-13, 0, -0.5), (14, 0, 20))
    head = body.part('head', (-6, -4.75, 0))
    m.box(head, (68, 20), (-5, -4.25, -7.5), (10, 9, 9))
    m.box(head, (88, 20), (-5, 3.75, -7.5), (10, 0, 9))
    m.box(head.part('left_ear', (5, -4.25, -1.5)), (104, 38), (0, 0, -2), (1, 11, 3))
    m.box(head.part('right_ear', (-5, -4.25, -1.5)), (96, 38), (-1, 0, -2), (1, 11, 3))
    m.box(head.part('nose', (0, -1.25, -9.5)), (68, 47), (-5, -3, -2), (10, 3, 4))
    m.box(head.part('lower_beak', (0, 1.25, -9.5)), (68, 38), (-5, -2.5, -2), (10, 5, 4))
    for name, x, z, uv in BABY_LEGS:
        m.box(bone.part(name, (x, -4, z)), uv, (-2, -1, -2), (4, 5, 4))
    m.box(body.part('left_fringe', (1.1, -2, 0)), BABY_SKIRT_UV, (0, 0, -0.75), (0, 4, 20))
    m.box(body.part('right_fringe', (-13.1, -2, 0)), BABY_SKIRT_UV, (0, 0, -0.75), (0, 4, 20))
    m.box(body.part('back_fringe', (-6, -2, 19.75)), BABY_SKIRT_END_UV, (-7, 0, 0), (14, 4, 0))
    garden = body.part('garden', (-6, -14.25, 9.5))
    _plant(m, garden, 'flower_0', (-2.5, 0, -3), BABY_FLOWER_UV, 5, 6, 0.7)
    _plant(m, garden, 'tuft_0', (2.5, 0, 4), BABY_TUFT_UV, 5, 4, 1.9)
    return m


MODELS = {'sift_sniffer': sift_sniffer, 'sift_sniffer_baby': sift_snifflet}


# ============================================================================ painting

def _texel_points(m):
    """Where every painted texel of the model sits in model space at rest: {(u, v): (x, y, z, part name)}."""
    pts = {}

    def visit(part, T):
        T = (T[0] + part.pivot[0], T[1] + part.pivot[1], T[2] + part.pivot[2])
        for c in part.cubes:
            g = c.inflate
            x0, y0, z0 = (c.origin[k] - g for k in range(3))
            x1, y1, z1 = (c.origin[k] + c.size[k] + g for k in range(3))
            corners = {'north': ((x0, y0, z0), (x1 - x0, 0, 0), (0, y1 - y0, 0)), 'south': ((x1, y0, z1), (x0 - x1, 0, 0), (0, y1 - y0, 0)),
                       'up': ((x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)), 'down': ((x0, y1, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1)),
                       'west': ((x0, y0, z1), (0, 0, z0 - z1), (0, y1 - y0, 0)), 'east': ((x1, y0, z0), (0, 0, z1 - z0), (0, y1 - y0, 0))}
            for face, (fx, fy, fw, fh) in c.faces().items():
                if fw <= 0 or fh <= 0:
                    continue
                o, du, dv = corners[face]
                for j in range(fh):
                    for i in range(fw):
                        a, b = (i + 0.5) / fw, (j + 0.5) / fh
                        p = tuple(T[k] + o[k] + du[k] * a + dv[k] * b for k in range(3))
                        pts.setdefault((fx + i, fy + j), p + (part.name,))
        for ch in part.children:
            visit(ch, T)

    for p in m.root.children:
        visit(p, (0, 0, 0))
    return pts


def _vnoise(x, y, z, seed):
    """Smooth 3D value noise in 0..1 (cell size 1)."""
    def h(i, j, k):
        n = (i * 73856093) ^ (j * 19349663) ^ (k * 83492791) ^ (seed * 2654435761)
        n = (n ^ (n >> 13)) * 1274126177
        return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0
    xi, yi, zi = math.floor(x), math.floor(y), math.floor(z)
    tx, ty, tz = x - xi, y - yi, z - zi
    tx, ty, tz = (t * t * (3 - 2 * t) for t in (tx, ty, tz))
    acc = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (tx if dx else 1 - tx) * (ty if dy else 1 - ty) * (tz if dz else 1 - tz)
                acc += w * h(xi + dx, yi + dy, zi + dz)
    return acc


def _patcher(points, blobs, parts_pink=(), seed=5):
    """Pink patches: noisy ellipsoids (centre, radii) in model space, plus whole parts painted pink."""
    def inside(u, v):
        p = points.get((u, v))
        if p is None:
            return False
        x, y, z, part = p
        if part in parts_pink:
            return True
        n = _vnoise(x / 2.6, y / 2.6, z / 2.6, seed) - 0.5
        for (cx, cy, cz), (rx, ry, rz) in blobs:
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
            if d + n * 0.9 < 1.0:
                return True
        return False
    return inside


def _sprite(rows, pal):
    img = Image.new('RGBA', (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x, y), _hx(pal[ch]))
    return img


def _put_plant(img, uv, sprite):
    """A plant sprite on its crossed planes' strip: as drawn on the front faces, mirrored on the backs."""
    u, v = uv
    img.alpha_composite(sprite, (u, v))
    img.alpha_composite(sprite.transpose(Image.Transpose.FLIP_LEFT_RIGHT), (u + sprite.width, v))


STEM = {'s': '#3f9d80', 'l': '#4fb58f', 'L': '#78d4a8'}
FLOWERS = {
    'dreambloom': (['..pPp..', '.pPWPp.', '.dpPpd.', '..dpd..', '...s...', '.l.s...', '.Lls.l.', '..Lsl..', '...s...'],
                   {'p': '#e472a8', 'P': '#f7a2cb', 'W': '#ffd3e6', 'd': '#c2558a'}),
    'hummingbloom': (['.v...v.', 'vVv.vVv', '.vYOYv.', '..vVv..', '...s...', '..ls...', '.Lls.l.', '...sl..', '...s...'],
                     {'v': '#8a62c8', 'V': '#d4b8f8', 'Y': '#fff2a8', 'O': '#d89a2a'}),
    'lullaby_bell': (['..sss..', '.s...s.', 'bBb.bBb', 'bBb.bBb', '.c...c.', '...s...', '..ls...', '..Lsl..', '...s...'],
                     {'b': '#55c9e8', 'B': '#8ae6f7', 'c': '#d7f7fd'}),
    'nebula_iris': (['...I...', '.i.I.i.', 'iIiIiIi', '.iIYIi.', '..iii..', '...s...', '.lLs...', '..lsL..', '...s...'],
                    {'i': '#7b4fcf', 'I': '#a98af0', 'Y': '#d4b8f8'}),
    'puffbloom': (['..www..', '.wWWWw.', 'wWWWWWw', '.wwWww.', '..www..', '...s...', '...s...', '..ls...', '...sl..'],
                  {'w': '#dee7f5', 'W': '#ffffff', 's': '#6f9a8e', 'l': '#93c3b2'}),
    'blushgrass': (['.g...G.', '.g.G.gG', 'Gg.g.g.', 'gGgg.gg', 'ggGgGgg'], {'g': '#d16895', 'G': '#f89fc3'}),
    'glimmer_sprouts': (['.T...T.', 'TtT.TtT', '.s.T.s.', '.s.s.s.', '.s.s.s.'], {'T': '#64d6b2', 't': '#c8f7e4', 's': '#48b996'}),
    'baby_bloom': (['.pPp.', '.dpd.', '..s..', 'l.s..', 'Lls.l', '..s..'], {'p': '#e472a8', 'P': '#f7a2cb', 'd': '#c2558a'}),
    'baby_sprout': (['T...T', 'tT.Tt', 's.s.s', 's.s.s'], {'T': '#64d6b2', 't': '#c8f7e4', 's': '#48b996'}),
}


def _flower(name):
    rows, pal = FLOWERS[name]
    return _sprite(rows, {**STEM, **pal})


def _strands(w, h, seed, density=0.78):
    """Shaggy tufts hanging from a root line, like the fringe of Mojang's Sniffer coat and the
    reference beasts' belly fur: tufts 1-3 texels wide, lighter towards the tips, ragged lengths and
    a few gaps. Returns {(x, y): coat tone}."""
    rnd = random.Random(seed)
    out = {}
    x = 0
    while x < w:
        if rnd.random() > density:
            out[(x, 0)] = rnd.choice((2, 3, 4))  # a gap: just the root
            x += 1
            continue
        sw = rnd.choice((1, 2, 2, 3))
        length = rnd.randint(max(2, h - 3), h)
        tone = rnd.choice((3, 4, 4, 5))
        for k in range(sw):
            if x + k >= w:
                break
            ln = length - (1 if k == sw - 1 and sw > 1 and rnd.random() < 0.5 else 0)
            for y in range(ln):
                t = tone if y < ln - 1 else min(6, tone + 1)
                if k == 0 and sw > 1 and 0 < y < ln - 1:
                    t = max(2, tone - 1)          # the shaded edge of the tuft
                out[(x + k, y)] = rnd.choice((2, 3, 3, 4)) if y == 0 else t
        x += sw
    return out


def _blades(w, h, seed):
    """Grass blades standing up along an edge (bottom row rooted, sparse): {(x, y): grass tone}."""
    rnd = random.Random(seed)
    out = {}
    for x in range(w):
        if rnd.random() < 0.3:
            out[(x, h - 1)] = rnd.choice((3, 4, 5))
            tall = rnd.randint(1, h - 1)
            for k in range(tall):
                out[(x, h - 2 - k)] = 7 if k == tall - 1 else rnd.choice((5, 6))
    return out


SPRINKLE = [['ll'], ['l', 'L'], ['Ll', '.l'], ['w'], ['y'], ['v'], ['p'], ['.L', 'll']]
SPRINKLE_PAL = {'l': '#48b996', 'L': '#64d6b2', 'w': '#ffffff', 'y': '#fff2a8', 'v': '#a98af0', 'p': '#ffd3e6'}


def _sprinkle(img, x0, y0, w, h, n, seed):
    """Little plants and flower buds dotted through the grass on its back (a face of w x h at x0, y0)."""
    rnd = random.Random(seed)
    for _ in range(n):
        shape = rnd.choice(SPRINKLE)
        x, y = x0 + rnd.randrange(1, w - 2), y0 + rnd.randrange(1, h - 2)
        for j, row in enumerate(shape):
            for i, ch in enumerate(row):
                if ch != '.':
                    img.putpixel((x + i, y + j), _hx(SPRINKLE_PAL[ch]))


def _paint_strip(img, uv, back, w, tones, ramp_of):
    """A flat plane's two faces: `tones` on the front face at uv, mirrored on the back face (so the
    two coplanar faces agree texel for texel whichever side the plane is seen from)."""
    for (x, y), t in tones.items():
        for u, v in ((uv[0] + x, uv[1] + y), (back[0] + w - 1 - x, back[1] + y)):
            img.putpixel((u, v), _hx(ramp_of(u, v)[t]))


def _eyes(img, eyes, expr, fur_at):
    """Small vanilla-style eyes, one slit per side; each expression only moves a few pixels.
    eyes: [(x0, y, width, front_dir)] - front_dir +1 when the snout is towards +x on that face."""
    eye = _hx(EYE)
    brow = _hx(DARK[1])
    for x0, y, w, d in eyes:
        cells = [x0 + k for k in range(w)]
        front, back = (cells[-1], cells[0]) if d > 0 else (cells[0], cells[-1])
        if expr == 'neutral':
            continue
        for x in cells:                       # wipe the open eye to bare fur first
            img.putpixel((x, y), fur_at(x, y, 1))
        if expr == 'blink':
            for x in cells:
                img.putpixel((x, y), fur_at(x, y, 0))
        elif expr == 'happy':                 # a little upturned arc (a lifted lid on the Snifflet's short eyes)
            for x in cells:
                img.putpixel((x, y if x in (front, back) and w > 2 else y - 1), eye)
        elif expr == 'angry':                 # the slit under a brow slanting down to the snout
            for x in cells:
                img.putpixel((x, y), eye)
            img.putpixel((front, y - 1), brow)
            img.putpixel((front - d, y - 2), brow)
            img.putpixel((front - 2 * d, y - 2), fur_at(front - 2 * d, y - 2, 0))
        elif expr == 'hurt':                  # screwed shut
            for x in cells:
                img.putpixel((x, y), eye if x != front else fur_at(x, y, 0))
            img.putpixel((front, y - 1), eye)
            img.putpixel((front, y + 1), eye)
        elif expr == 'dead':                  # a tiny x
            mid = cells[len(cells) // 2]
            for dx, dy in ((-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)):
                img.putpixel((mid + dx, y + dy), eye)


def _paint(m, vanilla_name, blobs, pink_parts, tip, extras, eyes, seed):
    base = _vanilla('entity/sniffer/' + vanilla_name)
    if base is None:   # no vanilla textures on this machine: keep the committed ones
        out = {}
        for key in [m.name] + [f'{m.name}_{e}' for e in EXPRS]:
            p = os.path.join(_ROOT, 'entity', m.name, key + '.png')
            out[key] = (Image.open(p).convert('RGBA'), None)
        return out
    points = _texel_points(m)
    pink = _patcher(points, blobs, pink_parts, seed)

    def choose(u, v, f):
        if f == 'fur' and pink(u, v):
            return 'patch'
        if f == 'beak' and (u, v) in points and tip(*points[(u, v)]):
            return 'tip'
        return f
    img = recolour(base, choose)
    extras(img, lambda u, v: PATCH if pink(u, v) else FUR)
    out = {m.name: (img, None)}
    cls = classify(base)

    def fur_at(x, y, k):
        f, i = cls.get((x, y), ('fur', 2))
        if f != 'fur':
            i = 2
        ramp = PATCH if pink(x, y) else FUR
        return _hx(ramp[max(0, i - (1 - k))])
    for x0, y, w, _ in eyes:      # the open eyes (the Snifflet's are drawn in its claw colour)
        for k in range(w):
            img.putpixel((x0 + k, y), _hx(EYE))
    for e in EXPRS:
        ex = img.copy()
        _eyes(ex, eyes, e, fur_at)
        out[f'{m.name}_{e}'] = (ex, None)
    return out


# pink patches of the grown Sniffer (model space at rest: x to its left, y down, z to its tail)
ADULT_PATCHES = [((12.5, 16, 9), (6, 7, 10)), ((-12.5, 15, -9), (6, 6.5, 7.5)), ((3, 18, 20.5), (7, 6, 4)), ((9.5, 17, -15), (4, 4, 4.5)),
                 ((-9.5, 18, 15), (4, 3.5, 4.5)), ((-6.5, 7, -24), (3, 4, 4)), ((0, 4, -22), (4.5, 2, 3))]
BABY_PATCHES = [((7, 16, 4), (3, 4, 5)), ((-7, 15, -4), (3, 3.5, 4)), ((-5, 15.5, -12), (2, 2.5, 2.5))]


def _paint_adult(m):
    def extras(img, ramp_of):
        _paint_strip(img, (0, 140), (41, 140), 41, _strands(41, 5, 3), ramp_of)       # left/right fringes
        _paint_strip(img, (84, 140), (109, 140), 25, _strands(25, 5, 5), ramp_of)     # front/back fringes
        _paint_strip(img, (0, 151), (40, 151), 40, _blades(40, 3, 11), lambda u, v: GRASS)  # the grass ridges
        _sprinkle(img, 102, 0, 25, 40, 16, 21)                                  # plants in the grass on its back
        for name, uv in FLOWER_UV.items():
            _put_plant(img, uv, _flower(name))
        for name, uv in TUFT_UV.items():
            _put_plant(img, uv, _flower(name))

    def tip(x, y, z, part):   # the pink snout tip: the nose plate's top (with the nostrils) and sides
        return part == 'nose' and z > -39.9
    # its eyes: (first x, row, width, snout direction) on the right (west) and left (east) side of the head
    return _paint(m, 'sniffer', ADULT_PATCHES, ('left_ear', 'right_ear'), tip, extras, [(14, 31, 3, 1), (34, 31, 3, -1)], 5)


def _paint_baby(m):
    def extras(img, ramp_of):
        _paint_strip(img, (0, 96), (20, 96), 20, _strands(20, 4, 7), ramp_of)
        _paint_strip(img, (40, 100), (54, 100), 14, _strands(14, 4, 9), ramp_of)
        _sprinkle(img, 20, 35, 14, 20, 5, 23)
        _put_plant(img, BABY_FLOWER_UV, _flower('baby_bloom'))
        _put_plant(img, BABY_TUFT_UV, _flower('baby_sprout'))

    def tip(x, y, z, part):   # its nose's top and sides (the nostrils on its front stay in the beak's colour)
        return part == 'nose' and z > -20.9
    return _paint(m, 'snifflet', BABY_PATCHES, ('left_ear', 'right_ear'), tip, extras, [(73, 32, 2, 1), (89, 32, 2, -1)], 9)


# ============================================================================ sounds (vanilla files)

def _files(prefix, n, vol, pitch):
    return [(f'{prefix}{i}', vol, pitch) for i in range(1, n + 1)]


SOUNDS = {
    'entity.sift_sniffer.ambient': _files('mob/sniffer/idle', 6, 0.9, 1.2) + [('mob/sniffer/happy2', 0.7, 1.3)],
    'entity.sift_sniffer.hurt': _files('mob/sniffer/hurt', 3, 1.0, 1.2),
    'entity.sift_sniffer.death': _files('mob/sniffer/death', 2, 1.0, 1.15),
    'entity.sift_sniffer.step': _files('mob/sniffer/step', 6, 0.6, 1.1),
    'entity.sift_sniffer.eat': _files('mob/sniffer/eat', 3, 1.0, 1.15),
    'entity.sift_sniffer.sniff': _files('mob/sniffer/sniffing', 3, 1.0, 1.1) + _files('mob/sniffer/scenting', 3, 1.0, 1.1),
    'entity.sift_sniffer.dig': _files('mob/sniffer/longdig', 2, 1.0, 1.05),
    'entity.sift_sniffer.find': [('random/pop', 0.8, 0.8), ('mob/sniffer/happy3', 1.0, 1.25)],
    'entity.sift_sniffer.trumpet': [('item/goat_horn/call1', 0.45, 1.55), ('item/goat_horn/call4', 0.45, 1.6), ('item/goat_horn/call7', 0.45, 1.5)],
    'entity.sift_sniffer.happy': _files('mob/sniffer/happy', 5, 1.0, 1.2),
    'entity.sift_sniffer.prepare_ram': _files('mob/goat/pre_ram', 4, 1.0, 0.8),
    'entity.sift_sniffer.ram': [('mob/goat/impact1', 1.0, 0.75), ('mob/goat/impact2', 1.0, 0.75)],
    'entity.sift_sniffer.bloom': [('block/cherry_leaves/break1', 1.0, 1.2), ('block/cherry_leaves/break2', 1.0, 1.1), ('mob/sniffer/happy1', 0.8, 1.2)],
    'block.sift_sniffer_egg.plop': [('mob/chicken/plop', 1.0, 0.7)],
    'block.sift_sniffer_egg.crack': _files('mob/turtle/egg/egg_crack', 5, 1.0, 0.9),
    'block.sift_sniffer_egg.hatch': _files('mob/turtle/egg/egg_break', 2, 1.0, 0.9),
    'entity.sift_creature.rot': [('mob/zombie/say1', 0.6, 0.7), ('mob/zombie/say2', 0.6, 0.7), ('mob/zombie/say3', 0.6, 0.75)],
}
SUBTITLES = {
    'entity.sift_sniffer.ambient': 'Sift Sniffer snuffles', 'entity.sift_sniffer.hurt': 'Sift Sniffer hurts',
    'entity.sift_sniffer.death': 'Sift Sniffer dies', 'entity.sift_sniffer.step': 'Sift Sniffer steps',
    'entity.sift_sniffer.eat': 'Sift Sniffer grazes', 'entity.sift_sniffer.sniff': 'Sift Sniffer sniffs',
    'entity.sift_sniffer.dig': 'Sift Sniffer digs', 'entity.sift_sniffer.find': 'Sift Sniffer finds something',
    'entity.sift_sniffer.trumpet': 'Sift Sniffer trumpets', 'entity.sift_sniffer.happy': 'Sift Sniffer delights',
    'entity.sift_sniffer.prepare_ram': 'Sift Sniffer paws the ground', 'entity.sift_sniffer.ram': 'Sift Sniffer rams',
    'entity.sift_sniffer.bloom': 'Sniffer blooms', 'block.sift_sniffer_egg.plop': 'Sift Sniffer lays an egg',
    'block.sift_sniffer_egg.crack': 'Sift Sniffer Egg cracks', 'block.sift_sniffer_egg.hatch': 'Sift Sniffer Egg hatches',
    'entity.sift_creature.rot': 'Rotting creature groans',
}


def assets(GA):
    """Sounds, loot, spawns' text and the Codex page (called before gen_sounds)."""
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    import gen_data as GD
    # what it digs up: the Sift's rare seeds and Pitcher Pods, now and then a Sculk Bloom
    GD.table('gift', 'gameplay/sift_sniffer_digging', [GD.pool([
        GD.item('minecraft:torchflower_seeds', 4), GD.item('minecraft:pitcher_pod', 4), GD.item('echo_seed', 4), GD.item('sculk_bloom', 1)])])
    # a flower or two from its back garden
    GD.table('entity', 'entities/sift_sniffer', [GD.pool([GD.item('dreambloom', 2), GD.item('hummingbloom', 2), GD.item('lullaby_bell', 1)],
                                                         condition=GD.chance(0.6))])
    GA.LANG.update({
        f'entity.{NS}.sift_sniffer': 'Sift Sniffer',
        f'band.{NS}.instrument.nose_trumpet': 'Nose Trumpet',
        # the original Minecraft-looking Sniffer is what a Sift Sniffer rots into outside the Sift
        'entity.minecraft.sniffer': 'Zombified Sniffer',
        'block.minecraft.sniffer_egg': 'Zombified Sniffer Egg',
        'item.minecraft.sniffer_spawn_egg': 'Zombified Sniffer Spawn Egg',
        f'codex.{NS}.sniffer.title': 'Sift Sniffer',
        f'codex.{NS}.sniffer.tagline': 'Neutral - fluffy, flowery and fond of digging',
        f'codex.{NS}.sniffer.body': 'A great fluffy Sniffer, pink and white, with a little flower garden on its back. It grazes the '
                                    'pink grass (a good meal regrows lost flowers) and now and then sniffs out and digs up something '
                                    'rare: Torchflower seeds, Pitcher Pods, Echo Seeds, even a Sculk Bloom. It flees Swifters, but '
                                    'hit one and it rams you like a goat. It trumpets through its nose and plays in bands. Fed '
                                    'Torchflower seeds, two lay a fluffy egg. Away from the Sift it rots into a grey Zombified '
                                    'Sniffer - every Sift creature rots out there, red, dark green and lifeless, and heals at home.',
    })


# ============================================================================ the egg block, its item and the spawn egg
#
# Mojang's Sniffer egg (block faces, item sprite) and Sniffer spawn egg sprite, re-themed like the
# Sniffer itself: the red shell / coat becomes the cream-white coat, the green moss Sift pink grass,
# the gold beak a pink snout. The block keeps vanilla's egg model (minecraft:block/sniffer_egg).

STAGES = ('not_cracked', 'slightly_cracked', 'very_cracked')
EGG_FACES = ('north', 'east', 'south', 'west', 'top', 'bottom')
# the sprite ramps reach darker than the coat's, for Mojang's dark sprite outlines and the cracks
SPRITE_RAMPS = {'fur': ['#3d2531', '#5e4250'] + FUR, 'grass': ['#43122b'] + GRASS, 'beak': ['#5e3024'] + PEACH, 'claw': CLAW}


def _family_any(c):
    r, g, b = c[:3]
    if g > r:
        return 'grass'
    if r > 130 and g > r * 0.4 and b < g * 0.7:
        return 'beak'
    if r > 80 and g > 40 and b < 20:
        return 'beak'          # the nostrils on the spawn egg's beak
    if r > g * 1.6 and r > b * 1.6:
        return 'fur'
    if b >= r * 0.9:
        return 'claw'
    return 'grass'             # olive, where the moss meets the shell


def retheme(imgs, ramps=None):
    """Re-themes a set of vanilla sprites together: every colour family ranked by brightness over the
    whole set (so all the faces of one egg stay consistent) and mapped onto the Sift ramps."""
    ramps = ramps or SPRITE_RAMPS
    cols = {}
    for im in imgs:
        for c in im.get_flattened_data():
            if c[3] >= 128:
                cols.setdefault(_family_any(c), set()).add(c[:3])
    lut = {}
    for f, cs in cols.items():
        rk = _ranks(cs, len(ramps[f]))
        for c in cs:
            lut[c] = _hx(ramps[f][rk[c]])
    out = []
    for im in imgs:
        o = Image.new('RGBA', im.size, (0, 0, 0, 0))
        pi, po = im.load(), o.load()
        for y in range(im.height):
            for x in range(im.width):
                c = pi[x, y]
                if c[3] >= 128:
                    po[x, y] = lut[c[:3]]
        out.append(o)
    return out


def gen_egg(GA, bid):
    """Vanilla's Sniffer egg model with the re-themed faces, three hatch stages; a flat item sprite."""
    variants = {}
    for stage, name in enumerate((bid, bid + '_slightly_cracked', bid + '_very_cracked')):
        tex = {f: f'{NS}:block/{bid}_{STAGES[stage]}_{f}' for f in EGG_FACES}
        tex['particle'] = tex['north']
        m = {'parent': 'minecraft:block/sniffer_egg', 'textures': tex}
        GA.note_textures(m)
        GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
        variants[f'hatch={stage}'] = {'model': f'{NS}:block/{name}'}
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})
    GA.item_generated(bid)


def _egg_faces():
    names = [f'block/sniffer_egg_{s}_{f}' for s in STAGES for f in EGG_FACES]
    imgs = [_vanilla(n) for n in names]
    if any(i is None for i in imgs):
        return {}
    return {f'block/sift_sniffer_egg_{s}_{f}': img for (s, f), img in zip([(s, f) for s in STAGES for f in EGG_FACES], retheme(imgs))}


def textures(out):
    for name, img in _egg_faces().items():
        out(name, img)


def item_sprites():
    """The egg's item sprite and the spawn egg (vanilla's Sniffer head sprite: moss cap, coat, beak)."""
    sprites = {}
    for ours, theirs in (('sift_sniffer_egg', 'item/sniffer_egg'), ('sift_sniffer_spawn_egg', 'item/sniffer_spawn_egg')):
        img = _vanilla(theirs)
        if img is None:
            p = os.path.join(_ROOT, 'item', ours + '.png')
            img = Image.open(p).convert('RGBA') if os.path.exists(p) else None
        else:
            img = retheme([img])[0]
        if img is not None:
            sprites[ours] = img
    return sprites


# ============================================================================ the rot overlays

ROT_RED = [(0x6e, 0x1c, 0x18), (0x8e, 0x2a, 0x22), (0xa8, 0x40, 0x2e)]
ROT_GREEN = [(0x22, 0x36, 0x1a), (0x2f, 0x4a, 0x22), (0x41, 0x60, 0x2c)]
_EXPR = ('_blink', '_happy', '_angry', '_hurt', '_sleep', '_dead')


def _noise(w, h, cell, seed):
    """Smooth value noise in 0..1 (bilinear over a random lattice)."""
    rnd = random.Random(seed)
    gw, gh = w // cell + 2, h // cell + 2
    lat = [[rnd.random() for _ in range(gw)] for _ in range(gh)]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        fy = y / cell
        y0 = int(fy)
        ty = fy - y0
        ty = ty * ty * (3 - 2 * ty)
        for x in range(w):
            fx = x / cell
            x0 = int(fx)
            tx = fx - x0
            tx = tx * tx * (3 - 2 * tx)
            a = lat[y0][x0] * (1 - tx) + lat[y0][x0 + 1] * tx
            b = lat[y0 + 1][x0] * (1 - tx) + lat[y0 + 1][x0 + 1] * tx
            out[y][x] = a * (1 - ty) + b * ty
    return out


def rot_overlay(base, seed):
    """The rot a creature wears outside the Sift, for one of its textures: blotches of raw red and dark
    green over a sour olive wash, only where the creature itself is painted (so plants on planes and
    cut-out fins rot too, but never the empty air around them). The renderer fades it in with the rot."""
    w, h = base.size
    cell = max(3, w // 22)
    n1 = _noise(w, h, cell, seed)
    n2 = _noise(w, h, max(2, cell // 2), seed + 1)
    n3 = _noise(w, h, cell * 2, seed + 2)
    src = base.load()
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(seed + 3)
    for y in range(h):
        for x in range(w):
            if src[x, y][3] < 128:
                continue
            v = n1[y][x] * 0.7 + n2[y][x] * 0.3
            if v > 0.56:
                ramp = ROT_RED if n3[y][x] > 0.5 else ROT_GREEN
                k = min(2, int((v - 0.56) / 0.07))
                k = max(0, k - (1 if rnd.random() < 0.15 else 0))
                r, g, b = ramp[2 - k]
                px[x, y] = (r, g, b, 235)
            elif v > 0.5:
                r, g, b = ROT_GREEN[0] if n3[y][x] <= 0.5 else ROT_RED[0]
                px[x, y] = (r, g, b, 150)        # a dark rim round each blotch
            else:
                px[x, y] = (0x4a, 0x55, 0x2e, 70)  # the sour wash over the rest
    return img


def rot_textures(tex_root):
    """One rot overlay next to every creature texture (not the expressions: the renderer maps those to
    their base texture, the silhouette is the same; not the emissive layers)."""
    ent = os.path.join(tex_root, 'entity')
    n = 0
    for dirpath, _, files in os.walk(ent):
        for f in sorted(files):
            if not f.endswith('.png') or f.endswith('_glow.png') or f.endswith('_rot.png'):
                continue
            stem = f[:-4]
            if stem.endswith(_EXPR):
                continue
            path = os.path.join(dirpath, f)
            base = Image.open(path).convert('RGBA')
            if base.width > 1024 or base.height > 1024:
                continue
            seed = sum(ord(c) for c in stem) * 131
            rot_overlay(base, seed).save(os.path.join(dirpath, stem + '_rot.png'))
            n += 1
    return n
