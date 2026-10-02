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
        # cheeks
        if ((abs(u) - 0.78) ** 2 * 6 + (v - 0.35) ** 2 * 3) < 0.06 and expr in ('happy', '', 'blink'):
            return 'b'
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


def stomper() -> Model:
    """The Stomper: a huge, shaggy mammoth-bullfrog in the Sift's pink and cyan. A warty coral-pink
    frog body on four cyan-furred pillar legs, a glowing sculk vein or two, and - like a Sniffer - a
    whole little garden growing on its back: a carpet of lumen moss with coral bushes, glimmer
    sprouts and blooms that sway as it walks. Four eyes, a long ringed trunk, floppy ears and three
    spiracles that puff."""
    pal = {
        'skin': '#f37d8a', 'skin_l': '#ffa0ab', 'skin_d': '#d65866', 'wart': '#c44a5a', 'spot': '#62d6d6',
        'belly': '#ffd9de', 'belly_l': '#fff0f2', 'belly_d': '#efb3bf',
        'fur': '#43bcc4', 'fur_l': '#7fe3e6', 'fur_d': '#29889a', 'moss': '#50c8bb', 'moss_l': '#69dbca', 'moss_d': '#3cb3ab',
        'sculk': '#12303a', 'sculk_l': '#1f5a66', 'glow': '#3ff5e6', 'glow2': '#c8fffb',
        'mouth': '#3a1a3a', 'lip': '#a8445a', 'tongue': '#ff8fb0', 'teeth': '#fff8ec', 'blush': '#ff6f9a',
        'eye': '#14182e', 'iris': '#7ff0ff', 'iris_d': '#2fb8d8', 'eye_hi': '#ffffff', 'lid': '#f590a0', 'lid_d': '#a8445a',
        'eye2': '#c3a9ec', 'eye2_d': '#8770cf',
        'nail': '#e3ddcc', 'nail_d': '#b3ab96', 'stone': '#c9d3e8', 'stone_d': '#93a0bf', 'hole': '#0c1a24', 'chrome': '#a8fbff',
        'trunk': '#f37d8a', 'trunk_l': '#ffa0ab', 'trunk_d': '#d65866', 'ring': '#43bcc4', 'ring_d': '#29889a', 'plant': '#50c8bb',
        'drum': '#b0643c', 'drum_l': '#c98154', 'drum_d': '#8a4a2a', 'drumhead': '#fbeedb', 'drumhead_d': '#e2cfb3',
    }
    m = Model('stomper', (256, 256), pal, {'stomper': {}}, res=2, expressions=EXPRS)
    skin = mc('skin', clusters=0.35, spots=0.18, accent='spot')
    fur = mc('fur', clusters=0.25, streaks=0.55)
    moss_img = block_tex('lumen_moss_block')
    turf_img = block_tex('sift_grass_block_top')

    body = m.part('body', pivot=(0, 11, 2))
    vk = {'v': 'sculk', 'V': 'sculk_l', 'g': 'glow'}
    # the warty pink barrel, a pale belly band, sculk veins creeping up from below
    body.cube((-16, -20, -18), (32, 20, 36), **skin, faces={
        'down': mc('belly', clusters=0.3),
        'up': dict(color='moss', image=moss_img),
        'west': mc('skin', clusters=0.35, spots=0.18, accent='spot', bands=[(13, 'belly')], map=veins(36, 20, 3, 10), keys=vk, glow_keys='g'),
        'east': mc('skin', clusters=0.35, spots=0.18, accent='spot', bands=[(13, 'belly')], map=veins(36, 20, 7, 10), keys=vk, glow_keys='g'),
        'north': mc('belly', clusters=0.3),
        'south': mc('skin', clusters=0.35, spots=0.18, accent='spot', bands=[(13, 'belly')], map=veins(32, 20, 11, 10), keys=vk, glow_keys='g'),
    })
    # mossy cyan fur hanging round the belly
    body.cube((-17, -7, -19), (34, 9, 38), **fur, fringe=2, faces={'up': dict(skip=True), 'down': dict(skip=True)})
    # the moss carpet on its back, hanging over the edges, with its garden and three spiracles
    hump = body.part('hump', pivot=(0, -20, 0))
    spiracle = ['..rrrr..', '.rHHHHr.', 'rHhhhhHr', 'rHhGGhHr', 'rHhGGhHr', 'rHhhhhHr', '.rHHHHr.', '..rrrr..']
    up_map = ['.' * 48 for _ in range(52)]
    for (cx, cy) in ((12, 10), (28, 10), (20, 26)):
        up_map = put(up_map, cx, cy, spiracle)
    side_moss = dict(color='fur', pattern='mc', clusters=0.3, streaks=0.5, image=moss_img, image_rows=4)
    hump.cube((-12, -6, -13), (24, 6, 26), color='moss', faces={
        'up': dict(color='moss', image=turf_img, hd=True, map=up_map, keys={'r': 'ring', 'H': 'moss_d', 'h': 'hole', 'G': 'glow'}, glow_keys='G'),
        'north': side_moss, 'south': side_moss, 'east': side_moss, 'west': side_moss, 'down': dict(skip=True),
    })
    # a lumpy second layer of moss
    hump.cube((-8, -8, -8), (16, 2, 13), color='moss', faces={
        'up': dict(color='moss', image=moss_img), 'north': dict(color='moss', image=moss_img), 'south': dict(color='moss', image=moss_img),
        'east': dict(color='moss', image=moss_img), 'west': dict(color='moss', image=moss_img), 'down': dict(skip=True)})
    for i, (sx, sz) in enumerate(((-6, -5), (6, -5), (0, 3))):
        sp = hump.part(f'spiracle_{i}', pivot=(sx * 0.75, -8, sz))
        sp.cube((-1.5, -1.5, -1.5), (3, 1.5, 3), color='fur_d', pattern='mc', clusters=0.0, rim=False, faces={
            'up': mc('hole', clusters=0.0, rim=False, hd=True, map=['rrrrrr', 'rhhhhr', 'rhGGhr', 'rhGGhr', 'rhhhhr', 'rrrrrr'],
                     keys={'r': 'ring', 'h': 'hole', 'G': 'glow'}, glow_keys='G'),
        })
    # the garden: coral, sprouts and blooms on cross planes that sway (see StomperModel)
    garden = [('coral_bush', -8, -6, -9, 11, 11), ('glimmer_sprouts', 8, -6, -9, 8, 8), ('dreambloom', -3, -8, -5, 8, 9),
              ('lullaby_bell', 4, -8, 1, 8, 9), ('coral_bush', 8, -6, 9, 10, 10), ('glimmer_sprouts', -8, -6, 8, 8, 7),
              ('echo_orchid', -3, -8, 3, 7, 8), ('blushgrass', 2, -6, 10, 8, 7)]
    for i, (tex, px, py, pz, w, h) in enumerate(garden):
        plant(hump, f'plant_{i}', (px, py, pz), tex, w, h, rot=(0, 0.6 * i, 0), glow_bright=235 if tex in ('glimmer_sprouts', 'echo_orchid') else None)
    # more growing on its shoulders and rump
    for i, (tex, px, pz, w, h) in enumerate((('coral_fern', -13, -15, 8, 8), ('dreambloom', 13, -14, 7, 8), ('lullaby_bell', -13, 15, 7, 8),
                                            ('coral_bush', 13, 14, 8, 8))):
        plant(body, f'body_plant_{i}', (px, -20, pz), tex, w, h, rot=(0, 0.9 * i + 0.3, 0))
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
    head.cube((-15, -10, -13), (30, 12, 14), **skin, faces={
        'north': mc('skin', clusters=0.2, spots=0.08, accent='spot', hd=True, map=stomper_mouth(''),
                    keys={'m': 'mouth', 'L': 'lip', 'b': 'blush', 'w': 'skin_d', 'W': 'wart', 't': 'teeth'},
                    expr={e: stomper_mouth(e) for e in EXPRS}),
        'down': mc('belly', clusters=0.2),
        'up': dict(color='moss', image=moss_img),
    })
    # a mossy brow, a little bloom and some sprouts growing on it
    head.cube((-12, -13, -11), (24, 4, 12), color='moss', faces={
        'up': dict(color='moss', image=turf_img), 'north': dict(color='fur', pattern='mc', clusters=0.2, streaks=0.5, image=moss_img, image_rows=2),
        'east': dict(color='moss', image=moss_img), 'west': dict(color='moss', image=moss_img), 'south': dict(color='moss', image=moss_img)})
    plant(head, 'head_plant_0', (-7, -13, -6), 'dreambloom', 7, 8, rot=(0, 0.5, 0))
    plant(head, 'head_plant_1', (8, -13, -4), 'glimmer_sprouts', 7, 7, rot=(0, -0.4, 0), glow_bright=235)
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
    # the four eyes: two big glowing frog domes on top, two small lilac ones low on the cheeks
    for side, sx in (('left', 1), ('right', -1)):
        mir = sx < 0
        big = head.part(f'{side}_eye', pivot=(8.5 * sx, -12, -7))
        eyekeys = {'r': 'skin_d', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}
        big.cube((-4, -5, -4), (8, 6, 8), **mc('skin', clusters=0.2), faces={
            'north': mc('skin', clusters=0.0, hd=True, map=eye(16, 12, '', mir), keys=eyekeys, expr=eye_exprs(16, 12, mir), glow_keys='iI'),
            ('east' if sx > 0 else 'west'): mc('skin', clusters=0.0, hd=True, map=eye(16, 12, '', not mir, shine=False), keys=eyekeys,
                                                 expr=eye_exprs(16, 12, not mir, shine=False), glow_keys='iI'),
            'up': dict(color='moss', image=moss_img),
        })
        lid = big.part(f'{side}_eyelid')
        lid.cube((-4, -5, -4), (8, 3, 8), inflate=0.15, **mc('lid', clusters=0.0, rim=False), faces={
            'north': mc('lid', clusters=0.0, rim=False, hd=True, map=['.' * 16] * 5 + ['d' * 16], keys={'d': 'lid_d'})})
        small = head.part(f'{side}_small_eye', pivot=(12.5 * sx, -5.5, -13))
        small.cube((-2, -2, -1.5), (4, 4, 2), **mc('skin', clusters=0.0), faces={
            'north': mc('skin', clusters=0.0, hd=True, map=eye(8, 8, '', mir, pupil='dot', rim=0.7),
                        keys={'r': 'skin_d', 'i': 'eye2', 'I': 'eye2_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'},
                        expr=eye_exprs(8, 8, mir, pupil='dot', rim=0.7)),
        })
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
            'up': dict(color='moss', image=moss_img)})
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
def fanfare_eel() -> Model:
    """Fanfare Eel: a long sculk eel of the Chrome lakes. Teal-black hide with pale bone ribs and a
    glowing lateral line, a translucent glowing frill, and for a mouth a pale-gold trumpet bell
    ringed with four glowing bell fins that flare when it blasts."""
    pal = {
        'hide': '#16303e', 'hide_l': '#24485a', 'hide_d': '#0c1a24', 'belly': '#2a5a66', 'belly_l': '#3a7480', 'belly_d': '#1f4450',
        'bone': '#e3ddcc', 'bone_d': '#b3ab96', 'glow': '#3ff5e6', 'glow2': '#c8fffb',
        'bell': '#f2d27a', 'bell_l': '#fff0b0', 'bell_d': '#b8923a', 'throat': '#0a1a22', 'throat_d': '#04080c',
        'fin': '#3fd8d0', 'fin_d': '#1f8a8a', 'fin_l': '#a8fbff', 'eye': '#04080c', 'iris': '#3ff5e6', 'iris_d': '#15a89f', 'eye_hi': '#ffffff',
        'lid': '#24485a', 'lid_d': '#0c1a24', 'tooth': '#fff8e0',
    }
    m = Model('fanfare_eel', (128, 64), pal, {'fanfare_eel': {}}, res=2, expressions=['blink', 'angry', 'hurt', 'dead'])
    head = m.part('head', pivot=(0, 20, -6))
    ek = {'r': 'hide_d', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}

    def side_eye(e, mirror, x):
        g = put(blank(12, 10), x, 1, eye(5, 5, e, mirror, pupil='slit', rim=0.7))
        return put(g, 0, 7, ['gg.g.gg.g.gg'])
    head.cube((-2.5, -2.5, -6), (5, 5, 6), **mc('hide', clusters=0.3, bands=[(3, 'belly')]), faces={
        'east': mc('hide', clusters=0.2, bands=[(3, 'belly')], hd=True, map=side_eye('', True, 1), keys=dict(ek, g='glow'), glow_keys='iIg',
                   expr={e: side_eye(e, True, 1) for e in ['blink', 'angry', 'hurt', 'dead']}),
        'west': mc('hide', clusters=0.2, bands=[(3, 'belly')], hd=True, map=side_eye('', False, 6), keys=dict(ek, g='glow'), glow_keys='iIg',
                   expr={e: side_eye(e, False, 6) for e in ['blink', 'angry', 'hurt', 'dead']}),
        'up': mc('hide', clusters=0.2, hd=True, map=['..bbbbbb..', '..........', '..bbbbbb..', '..........', '..bbbbbb..'] + ['..........'] * 7,
                 keys={'b': 'bone'}),
        'down': mc('belly', clusters=0.2),
    })
    pipe = head.part('pipe', pivot=(0, 0.5, -6))
    pipe.cube((-1.5, -1.5, -3), (3, 3, 3), **mc('bell', clusters=0.15, rim=False))
    bell = pipe.part('bell', pivot=(0, 0, -3))
    bell.cube((-4, -4, -2), (8, 8, 2), **mc('bell', clusters=0.1, rim=False), faces={
        'north': mc('bell', clusters=0.0, rim=False, hd=True, map=rows_of(16, 16, lambda x, y, u, v: (
            '_' if u * u + v * v > 1.0 else 'G' if u * u + v * v > 0.86 else 'B' if u * u + v * v > 0.74 else 'L' if u * u + v * v > 0.62 else
            't' if (u * u + v * v > 0.5 and (x + y) % 3 == 0) else 'T' if u * u + v * v < 0.18 else 'D')),
            keys={'B': 'bell_d', 'L': 'bell_l', 'D': 'throat', 'T': 'throat_d', 't': 'tooth', 'G': 'glow'}, glow_keys='G'),
        'south': mc('bell_d', clusters=0.0, rim=False),
    })
    # four glowing fins flaring round the bell
    for i, (rz, nm) in enumerate(((0.0, 'top'), (math.pi / 2, 'right'), (math.pi, 'bottom'), (-math.pi / 2, 'left'))):
        bf = bell.part(f'bell_fin_{i}', pivot=(0, 0, -1), rot=(0, 0, rz))
        bf.cube((0, -8, -1), (0, 5, 4), **mc('fin', clusters=0.0, rim=False, glow=True, ribs=1, accent='fin_l', alpha='membrane', edge='outer',
                                             edge_depth=1, scallop=2))
    fin = head.part('crest', pivot=(0, -2.5, -3))
    fin.cube((0, -3, -2), (0, 3, 5), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_l', alpha='membrane', edge='bottom', glow=True))
    prev = head
    dims = [(4.5, 4.5, 7), (4, 4, 7), (3.5, 3.5, 6), (3, 3, 6), (2, 2, 5)]
    pivot = (0, 0, 0)
    for i, (w, h, d) in enumerate(dims):
        seg = prev.part(f'segment_{i}', pivot=pivot)
        iw, ih, idd = int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d))
        ribs_up = [('bb' * iw if k % 3 == 1 else '.' * (iw * 2)) for k in range(idd * 2)]
        lateral = ['.' * (idd * 2)] * (ih - 1) + [''.join('g' if k % 3 == 0 else '.' for k in range(idd * 2))] + ['.' * (idd * 2)] * (ih + 1)
        ribs_side = [''.join('b' if (k % 6) in (2, 3) else '.' for k in range(idd * 2))] * ih
        side = overlay(ribs_side + ['.' * (idd * 2)] * ih, lateral)[:ih * 2]
        seg.cube((-w / 2, -h / 2, 0), (w, h, d), **mc('hide', clusters=0.3, bands=[(int(h / 2), 'belly')]), faces={
            'up': mc('hide', clusters=0.25, hd=True, map=ribs_up, keys={'b': 'bone'}),
            'east': mc('hide', clusters=0.25, bands=[(int(h / 2), 'belly')], hd=True, map=side, keys={'b': 'bone', 'g': 'glow'}, glow_keys='g'),
            'west': mc('hide', clusters=0.25, bands=[(int(h / 2), 'belly')], hd=True, map=side, keys={'b': 'bone', 'g': 'glow'}, glow_keys='g'),
            'down': mc('belly', clusters=0.2),
        })
        seg.cube((0, -h / 2 - 2.5, 0.5), (0, 2.5, d - 1), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_l', alpha='membrane', edge='bottom',
                                                                glow=True))
        if i == len(dims) - 1:
            seg.cube((0, -3.5, d - 0.5), (0, 7, 5), **mc('fin', clusters=0.0, rim=False, ribs=2, accent='fin_l', alpha='membrane', edge='outer',
                                                         edge_depth=1, scallop=2, glow=True))
        prev = seg
        pivot = (0, 0, d)
    for side, sx in (('left', 1), ('right', -1)):
        pf = head.part(f'{side}_fin', pivot=(2.5 * sx, 1.5, -1), rot=(0, 0.4 * sx, 0.5 * sx))
        pf.cube((0 if sx > 0 else -3, 0, -1), (3, 0, 3), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_l', glow=True))
    return m


def kazoo_fish() -> Model:
    """Kazoo Fish: a little teal schooling fish freckled with coral pink, a pink kazoo for a snout,
    mismatched googly eyes, fins and tail of living coral and a tuft of moss on its head with a
    glimmer sprout growing out of it."""
    pal = {
        'teal': '#3fd0c4', 'teal_l': '#72e3d8', 'teal_d': '#22a8a0', 'belly': '#c8fff0', 'belly_d': '#8fd8c8', 'pink': '#f87d8d', 'pink_l': '#ffb0bb',
        'pink_d': '#d65866', 'kazoo': '#f87d8d', 'kazoo_l': '#ffb0bb', 'kazoo_d': '#c44a5a', 'cap': '#fff4a8',
        'hole': '#3a1a2a', 'eye': '#101820', 'eye_w': '#ffffff', 'eye_hi': '#ffffff', 'lid': '#22a8a0', 'lid_d': '#0f5a5a', 'spot': '#ff9fb0',
        'moss': '#34a2b0', 'plant': '#57c7a2',
    }
    m = Model('kazoo_fish', (64, 32), pal, {'kazoo_fish': {}}, res=2, expressions=['blink', 'hurt', 'dead'])
    coral = block_tex('coral_fern')
    bush = block_tex('coral_bush')
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-1.5, -2.5, -3), (3, 5, 6), **mc('teal', clusters=0.3, bands=[(3, 'belly')], spots=0.45, accent='spot'), faces={
        'up': mc('teal', clusters=0.2, hd=True, map=['......', '..pp..', '......', '.p..p.', '......', '..pp..'] * 2, keys={'p': 'pink'}),
        'down': mc('belly', clusters=0.2),
    })
    ek = {'r': 'eye', 'i': 'eye_w', 'I': 'eye_w', 'p': 'eye', 'h': 'eye_w', 'l': 'lid', 'd': 'lid_d'}
    for side, sx, size in (('left', 1, 3), ('right', -1, 2)):
        e = body.part(f'{side}_eye', pivot=(1.5 * sx, -1.5, -2))
        e.cube((0 if sx > 0 else -1, -size / 2, -size / 2), (1, size, size), **mc('eye_w', clusters=0.0, rim=False), faces={
            ('east' if sx > 0 else 'west'): mc('eye_w', clusters=0.0, rim=False, hd=True, map=eye(size * 2, size * 2, '', sx < 0, pupil='dot', rim=0.75, shine=False),
                                                 keys=ek, expr={x: eye(size * 2, size * 2, x, sx < 0, pupil='dot', rim=0.75, shine=False)
                                                                for x in ['blink', 'hurt', 'dead']}),
            'north': mc('eye_w', clusters=0.0, rim=False),
        })
    snout = body.part('kazoo', pivot=(0, 0.5, -3))
    snout.cube((-1, -1, -4), (2, 2, 4), **mc('kazoo', clusters=0.15, rim=False), faces={
        'north': mc('kazoo', clusters=0.0, rim=False, hd=True, map=['.hh.', 'hhhh', 'hhhh', '.hh.'], keys={'h': 'hole'}),
    })
    snout.cube((-0.75, -2, -3), (1.5, 1, 1.5), **mc('cap', clusters=0.0, rim=False))
    # the moss tuft and its sprout
    tuft = body.part('tuft', pivot=(0, -2.5, -0.5))
    tuft.cube((-1, -1, -1.5), (2, 1, 3), color='moss', faces={f: dict(color='moss', image=block_tex('lumen_moss_block'))
                                                             for f in ('up', 'north', 'south', 'east', 'west')})
    plant(tuft, 'tuft_sprout', (0, -1, 0), 'glimmer_sprouts', 3, 3, rot=(0, 0.6, 0), glow_bright=235)
    # living coral fins
    dorsal = body.part('dorsal', pivot=(0, -2.5, 1))
    dorsal.cube((0, -3, -0.5), (0, 3, 3), color='pink', faces={'east': dict(color='pink', image=bush, image_mode='stretch'),
                                                               'west': dict(color='pink', image=mirror_img(bush), image_mode='stretch')})
    tail = body.part('tail', pivot=(0, 0, 3))
    tail.cube((0, -3, 0), (0, 6, 5), color='pink', faces={'east': dict(color='pink', image=coral, image_mode='stretch'),
                                                          'west': dict(color='pink', image=mirror_img(coral), image_mode='stretch')})
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(1.5 * sx, 1.5, -0.5), rot=(0, 0, 0.6 * sx))
        pf.cube((0 if sx > 0 else -2.5, 0, 0), (2.5, 0, 2.5), color='pink', faces={'up': dict(color='pink', image=coral, image_mode='stretch'),
                                                                                  'down': dict(color='pink', image=coral, image_mode='stretch')})
    return m


def tubafish() -> Model:
    """Tubafish: a huge, round periwinkle pufferfish freckled with glowing cyan spots. A pale-gold
    tuba bell on its back is crowned with a little anemone of swaying pink and cyan tentacles, its
    spikes are branches of pink coral, and when it panics it swells up and they all stand out."""
    pal = {
        'skin': '#78a5e3', 'skin_l': '#9cc3f3', 'skin_d': '#5d86cc', 'belly': '#ffe6ef', 'belly_l': '#fff6fa', 'belly_d': '#f0c4d4',
        'spot': '#3ff5e6', 'bell': '#f2d27a', 'bell_l': '#fff0b0', 'bell_d': '#b8923a', 'throat': '#1a1030',
        'coral': '#f37d84', 'coral_l': '#ffa9aa', 'coral_d': '#c44a5a',
        'lip': '#f59ab8', 'lip_d': '#c96a8c', 'eye': '#1a1420', 'iris': '#3ff5e6', 'iris_d': '#15a89f', 'eye_hi': '#ffffff', 'lid': '#8fb3ec',
        'lid_d': '#3f4f8a', 'fin': '#ffb0c4', 'fin_d': '#e07a9a', 'valve': '#e3ddcc', 'valve_l': '#f4f0e5', 'tent': '#ff9fd0', 'tent2': '#7ff0ff',
        'glow': '#3ff5e6',
    }
    m = Model('tubafish', (128, 128), pal, {'tubafish': {}}, res=2, expressions=EXPRS)
    body = m.part('body', pivot=(0, 17, 0))
    ek = {'r': 'skin_d', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}

    def front(expr):
        g = blank(24, 22)
        g = put(g, 1, 3, eye(8, 8, expr, False, pupil='dot', rim=0.7))
        g = put(g, 15, 3, eye(8, 8, expr, True, pupil='dot', rim=0.7))
        return g
    dots = ['............', '.g....g...g.', '............', '...g.....g..', '.........g..', 'g...g.......', '............', '..g....g....',
            '............', '............', '............']
    body.cube((-6, -6, -6), (12, 11, 12), **mc('skin', clusters=0.3, bands=[(7, 'belly')]), faces={
        'north': mc('skin', clusters=0.2, bands=[(7, 'belly')], hd=True, map=front(''), keys=ek, expr={e: front(e) for e in EXPRS}, glow_keys='iI'),
        'down': mc('belly', clusters=0.2),
        'up': mc('skin', clusters=0.2, map=dots + ['............'], keys={'g': 'spot'}, glow_keys='g'),
        'east': mc('skin', clusters=0.2, bands=[(7, 'belly')], map=dots, keys={'g': 'spot'}, glow_keys='g'),
        'west': mc('skin', clusters=0.2, bands=[(7, 'belly')], map=[r[::-1] for r in dots], keys={'g': 'spot'}, glow_keys='g'),
        'south': mc('skin', clusters=0.2, bands=[(7, 'belly')], map=dots, keys={'g': 'spot'}, glow_keys='g'),
    })
    mouth = body.part('mouth', pivot=(0, 1.5, -6))
    mouth.cube((-2, -1.5, -2), (4, 3, 2), **mc('lip', clusters=0.0, rim=False), faces={
        'north': mc('lip', clusters=0.0, rim=False, hd=True, map=['..llll..', '.lLLLLl.', '.lLooLl.', '.lLLLLl.', '..llll..', '........'],
                    keys={'l': 'lip_d', 'L': 'lip', 'o': 'throat'}),
    })
    tuba = body.part('tuba', pivot=(0, -6, 1))
    tuba.cube((-1.5, -3, -1.5), (3, 3, 3), **mc('bell', clusters=0.1, rim=False))
    tbell = tuba.part('tuba_bell', pivot=(0, -3, 0), rot=(0.2, 0, 0))
    tbell.cube((-3.5, -3, -3.5), (7, 3, 7), **mc('bell', clusters=0.1, rim=False), faces={
        'up': mc('bell', clusters=0.0, rim=False, hd=True, map=rows_of(14, 14, lambda x, y, u, v: (
            '_' if u * u + v * v > 1.0 else 'B' if u * u + v * v > 0.8 else 'L' if u * u + v * v > 0.6 else 'D')), keys={'B': 'bell_d', 'L': 'bell_l', 'D': 'throat'}),
    })
    # the anemone crown round the bell's rim
    for i in range(6):
        a = i * math.pi / 3
        t = tbell.part(f'tentacle_{i}', pivot=(math.cos(a) * 2.6, -3, math.sin(a) * 2.6), rot=(-math.sin(a) * 0.5, 0, math.cos(a) * 0.5))
        t.cube((-0.5, -3.5, -0.5), (1, 3.5, 1), **mc('tent' if i % 2 == 0 else 'tent2', clusters=0.0, rim=False, glow=i % 2 == 1), faces={
            'up': mc('belly_l', clusters=0.0, glow=True)})
    for i, vx in enumerate((-3.5, 3.5)):
        valve = body.part(f'valve_{i}', pivot=(vx, -6, -2.5))
        valve.cube((-0.75, -2, -0.75), (1.5, 2, 1.5), **mc('valve', clusters=0.0, rim=False), faces={'up': mc('valve_l', clusters=0.0)})
    # the spikes: little branches of coral, folded flat until it puffs up
    k = 0
    for (px, py, pz, rx, rz) in ((0, -6, -3, -0.6, 0), (-6, -1, 0, 0, 1.2), (6, -1, 0, 0, -1.2), (0, -1, 6, 0.9, 0), (-4.5, -5, 4, 0.6, 0.7),
                                 (4.5, -5, 4, 0.6, -0.7), (-4.5, 3, -4, -1.2, 0.6), (4.5, 3, -4, -1.2, -0.6), (-6, -4, -3, -0.4, 1.0),
                                 (6, -4, -3, -0.4, -1.0), (0, 5, 0, 3.14, 0), (-4, 4, 4, 2.2, 0.6), (4, 4, 4, 2.2, -0.6)):
        sp = body.part(f'spike_{k}', pivot=(px, py, pz), rot=(rx, 0, rz))
        sp.cube((-0.5, -3, -0.5), (1, 3, 1), **mc('coral', clusters=0.0, rim=False), faces={'up': mc('coral_l', clusters=0.0)})
        sp.cube((0.5 if k % 2 else -1.5, -2.5, -0.5), (1, 1, 1), **mc('coral_l', clusters=0.0, rim=False))
        k += 1
    for side, sx in (('left', 1), ('right', -1)):
        pf = body.part(f'{side}_fin', pivot=(6 * sx, 0, -1), rot=(0, 0.3 * sx, 0))
        pf.cube((0 if sx > 0 else -3, -2, 0), (3, 4, 0), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='outer'))
    tail = body.part('tail', pivot=(0, -0.5, 6))
    tail.cube((0, -3, 0), (0, 6, 4), **mc('fin', clusters=0.0, rim=False, ribs=1, accent='fin_d', alpha='membrane', edge='outer', edge_depth=1, scallop=2))
    return m


ALL = {'stomper': stomper, 'sky_whale': sky_whale, 'fanfare_eel': fanfare_eel, 'kazoo_fish': kazoo_fish, 'tubafish': tubafish}
