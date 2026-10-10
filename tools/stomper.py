"""S1 Stomper: the Sift's elephant (geometry, high-res texture, rig check, spawn egg).

A tall, long-legged elephant that looks like the Sift Plains it roams: a hide of the plains' mauve turf
earth with wrinkle rings and a darker belly, a blanket of pink coral turf over its back that hangs down
its flanks in ragged grass tufts, a garden of coral, blushgrass, glimmer sprouts, flowers and teal Sift
grass growing on its domed back, big flapping ears with veined coral-pink insides, small bone tusks,
bone toenails, and a long five-part wrinkled trunk that hangs near the ground and grabs.

Painted in the Sculk-mob pipeline (like tools/cave_creatures.py and tools/parasite.py): modelkit
`Model(..., res=2)` with crisp hd maps for the eyes, wrinkles, veins and nails, the material pass on
top (an explicit x2 detail: the 256x256 layout becomes a 1024x1024 sheet, four texels per unit), glowing
sprout bulbs on the emissive layer, and an expression per mood. The White Forest coat is a palette variant.

Registered into mobs.ALL from tools/mobs_wild.py. Java: StomperModel (animation), Stomper (behaviour)
and StomperRig (the grab's trunk rig, checked against this geometry by check_rig())."""
import os
import zlib

from landkit import blades, bells, blooms, coral, creases, mirror, overlay, rows, speckle, sprouts, tones
from modelkit import Model

ROOT = os.path.join(os.path.dirname(__file__), '..')
EXPRS = ['blink', 'happy', 'angry', 'hurt', 'dead']

# --------------------------------------------------------------------------- palettes
# the Sift Plains: mauve turf earth (coral turf side / sift soil), pink coral turf, teal Sift grass, pink blushgrass
PAL = tones(hide='#8d5e6d', shin='#7f5363', belly='#6a4454', turf='#ec717f', moss='#c95566', teal='#43bcb1', bone='#e6dac0',
            pad='#3e2232', ear='#dc7d8e', tuftw='#f89fc3')
PAL.update({'turf_d': '#cf6474', 'turf_l': '#ff9cab', 'eye': '#1c0f18', 'iris': '#b8742a', 'iris_d': '#8a5220', 'lash': '#2a1422', 'vein': '#b4566c',
            'mouth': '#7a2c44', 'mouth_l': '#a8455e', 'mouth_d': '#46182c', 'nostril': '#2a1020', 'flower': '#fff4ec', 'pollen': '#ffd64e'})
# the White Forest coat: frost-pale lilac earth under snowy turf with icy-blue grass
WHITE = tones(hide='#a39bb6', shin='#938aa8', belly='#7c7394', turf='#eef0fa', moss='#c8c8de', teal='#86bedc', bone='#f2eee4',
              pad='#3e3650', ear='#d8b8d8', tuftw='#ffffff')
WHITE.update({'turf_d': '#c8c6dc', 'iris': '#5a8ab8', 'iris_d': '#3e6890', 'vein': '#b090b4', 'mouth': '#7e5672', 'mouth_l': '#a07898', 'mouth_d': '#4a2a40',
              'lash': '#2e2440', 'flower': '#ffffff', 'pollen': '#c8e8ff'})
MATERIALS = {'hide': 'skin', 'shin': 'skin', 'belly': 'skin', 'turf': 'plant', 'moss': 'plant', 'teal': 'plant', 'ear': 'skin',
             'pad': 'stone', 'tuftw': 'plant'}

# plant ramps (dark -> light) for the garden sprites, per coat
GARDEN_RAMPS = {
    'normal': dict(coral=['#8e2e46', '#b4445c', '#d85868', '#f07880', '#ff9aa6', '#ffc6c8'],
                   blush=['#9c3c66', '#c45a88', '#d9709c', '#f08cb6', '#ffb0d2'],
                   teal=['#145a5c', '#1e7272', '#2a8c88', '#43bcb1', '#7aeede', '#b8fff2'],
                   bulb=['#2fb8b0', '#5ee8de', '#9ffcf4', '#e8fffc'],
                   petal=['#e0b8c8', '#f4d8e0', '#fff0f4', '#ffffff'], centre=('#f0a428', '#ffd64e'),
                   bell=['#a83c6a', '#d0588c', '#f07cac', '#ffa8cc']),
    'white': dict(coral=['#9c8cac', '#b4a6c2', '#cabed6', '#dcd4e6', '#f4f1f9', '#ffffff'],
                  blush=['#9c8cb0', '#b4a6c8', '#cabed8', '#e2dcee', '#ffffff'],
                  teal=['#3c6a88', '#4e84a6', '#68a2c4', '#86bedc', '#a6d6ee', '#e8f8ff'],
                  bulb=['#7fc8e8', '#a8e4ff', '#d8f4ff', '#ffffff'],
                  petal=['#c8d8f0', '#e0ecff', '#f4f8ff', '#ffffff'], centre=('#8cc4ec', '#c8e8ff'),
                  bell=['#8c7cb0', '#a898c8', '#c8bce0', '#ece4ff']),
}

# --------------------------------------------------------------------------- geometry
# the garden: (sprite, x, z, top y of the surface it grows from, y rotation, width, height) in model units
GARDEN = [('coral', 0, -2, -23, 0.3, 9, 10), ('bloom', -4, 4, -23, 1.1, 7, 9), ('tuft', 4, -7, -23, 0.7, 8, 8),
          ('sprout', 3, 6, -23, -0.4, 7, 8), ('teal', -5, -6, -23, -0.6, 7, 7), ('bell', -9, -11, -20, 0.2, 7, 8),
          ('tall', 9, 1, -20, 1.3, 8, 10), ('coral', 9, -10, -20, -0.5, 8, 9), ('teal', -9, 9, -20, 0.9, 7, 7),
          ('tall', 8, 11, -20, -1.0, 8, 10), ('tuft', -1, 12, -20, 0.4, 8, 7), ('bloom', -9, -1, -20, 0.6, 7, 9),
          ('sprout', 1, -12, -20, 0.5, 6, 7), ('coral', -5, 12, -20, 1.4, 8, 9), ('bloom', 10, 7, -20, -0.3, 7, 8),
          ('tall', -2, -7, -23, 0.9, 8, 10)]
# (width, length, rest x rotation) of each trunk segment, root to tip (StomperRig.SEG / REST)
TRUNK = [(7, 7, -0.05), (6, 7, 0.0), (5, 6, -0.05), (4, 6, -0.3), (3, 5, -0.6)]
LEGS = [('front_left', 7.5, -10), ('front_right', -7.5, -10), ('back_left', 7.5, 10), ('back_right', -7.5, 10)]
BP = (0, -3, 10)    # body pivot (hind hips)
HP = (0, -10, -18)  # head pivot (neck)
TP = (0, 2, 0)      # torso pivot (under the belly)


def _sprite(kind, w, h, seed, coat):
    R = GARDEN_RAMPS[coat]
    W, H = w * 2, h * 2
    if kind == 'coral':
        return coral(W, H, seed, R['coral'])
    if kind == 'tuft':
        return blades(W, H, seed, R['blush'])
    if kind == 'tall':
        return blades(W, H, seed, R['blush'], n=W // 2 + 4, tips=R['petal'][-1])
    if kind == 'teal':
        return blades(W, H, seed, R['teal'], lean=0.4)
    if kind == 'sprout':
        return sprouts(W, H, seed, R['teal'], R['bulb'])
    if kind == 'bloom':
        return blooms(W, H, seed, R['teal'], R['petal'], R['centre'])
    return bells(W, H, seed, R['teal'], R['bell'])


# --------------------------------------------------------------------------- texel maps
EYE_KEYS = {'p': 'eye', 'i': 'iris', 'I': 'iris_d', 'd': 'lash', 'l': 'hide_l', 'f': 'hide_d'}


def eye_face(w, h, x0, y0, expr, flip):
    """A side of the skull: wrinkle folds round the eye socket and a small elephant eye (vanilla-style: a dark pupil
    beside an amber iris under a dark lid line, with a lit fold above it) at (x0, y0) texels from the front edge."""
    from landkit import place, small_eye
    base = creases(w, h, period=6, seed=31 + flip, wob=0.8, gap=0.25, start=3)
    base = place(base, ['.lllll.', 'f.....f'], x0 - 1, y0 - 2)  # the lit fold of skin over the eye
    out = place(base, small_eye('' if expr == 'neutral' else expr, 5, 3), x0, y0)
    return [r[::-1] for r in out] if flip else out


def ear_map(w, h, inner, flip):
    """An elephant ear, w x h texels along it (from the head) and down: a rounded fan with a hanging lobe.
    Inside: coral pink with veins fanning out from the root and a hide-coloured rim; outside: wrinkled hide."""
    import math
    import random
    rnd = random.Random(7 + inner)

    def keep(u, v):
        r = (u - 0.05) ** 2 + ((v - 0.42) / 0.62) ** 2
        lobe = u < 0.45 and v > 0.7 and (u - 0.18) ** 2 / 0.06 + (v - 0.86) ** 2 / 0.05 < 1.0
        return (r < 1.0 and v < 0.92) or lobe or (u < 0.12 and v < 0.75)
    veins = [(0.35 + k * 0.32 + rnd.random() * 0.1) for k in range(5)]

    def px(x, y):
        u, v = (x + 0.5) / w, (y + 0.5) / h
        if not keep(u, v):
            return '_'
        # the rim: two texels in from the edge
        edge = not all(keep(u + du / w, v + dv / h) for du, dv in ((2, 0), (-2, 0), (0, 2), (0, -2)))
        if not inner:
            if edge:
                return 'l' if v < 0.4 else 'd'
            return '.'
        if edge:
            return 'h'
        ang = math.atan2(v - 0.38, u + 0.05)
        for a in veins:
            if abs(ang - (a - 0.9)) < 0.045 + 0.02 * (1 - u) and u > 0.08:
                return 'v'
        return 'p' if (v < 0.3 and u > 0.25 and (x + y) % 7 == 0) else '.'
    out = rows(w, h, px)
    if not inner:
        # the back of the ear: wrinkled hide inside the same outline
        folds = creases(w, h, period=5, seed=55, wob=1.6, gap=0.3)
        out = [''.join(b if b != '.' else a for a, b in zip(ra, rb)) for ra, rb in zip(folds, out)]
    return [r[::-1] for r in out] if flip else out


def nails(w, h, n=3):
    """Bone toenails along the bottom of a foot's face, a dark pad rim under them."""
    out = []
    for y in range(h):
        if y == h - 1:
            out.append('k' * w)
            continue
        if y < h - 4:
            out.append('.' * w)
            continue
        row = ''
        for x in range(w):
            slot = (x * n) // w
            cx = (slot + 0.5) * w / n - 0.5
            dx = abs(x - cx)
            if dx < w / n * 0.32:
                row += 'B' if y == h - 4 and dx < 1 else ('b' if y < h - 2 else 'n')
            else:
                row += '.' if y < h - 2 else 'd'
        out.append(row)
    return out


NAIL_KEYS = {'B': 'bone_l', 'b': 'bone', 'n': 'bone_d', 'k': 'pad', 'd': 'shin_d'}
WRINKLE = {'w': 'hide_d', 'h': 'hide_l'}
SHIN_WRINKLE = {'w': 'shin_d', 'h': 'shin_l'}
TURF_KEYS = {'t': 'teal', 'T': 'teal_l', 'f': 'flower', 'y': 'pollen', 'u': 'tuftw', 'U': 'tuftw_l', 'm': 'moss'}


def hide(color='hide', seed=1, period=6, wob=1.0, gap=0.35, w=8, h=8, keys=None, **kw):
    """A hide face: clustered mauve earth with wrinkle folds (the folds get the material pass too)."""
    d = dict(color=color, pattern='mc', clusters=0.14, hd=True, map_material=True,
             map=creases(w * 2, h * 2, period=period, seed=seed, wob=wob, gap=gap), keys=keys or WRINKLE)
    d.update(kw)
    return d


def turf(w, h, top=False, seed=1, fringe=2, phase=0):
    """Pink coral turf: grass strands on the sides with a ragged hem; teal tufts and tiny flowers on top."""
    if top:
        return dict(color='turf', pattern='mc', clusters=0.6, hd=True, map_material=True,
                    map=speckle(w * 2, h * 2, seed, 'ttTffym', 0.05), keys=TURF_KEYS)
    return dict(color='turf', pattern='mc', clusters=0.4, streaks=0.6, fringe=fringe, fringe_phase=phase, rim=True, hd=True,
                map_material=True, map=speckle(w * 2, h * 2, seed, 'tTm', 0.025), keys=TURF_KEYS)


class StomperModel(Model):
    pass


def stomper() -> Model:
    variants = {'stomper': {}, 'stomper_white': dict(WHITE)}
    m = Model('stomper', (256, 256), dict(PAL), variants, res=2, expressions=EXPRS, detail=2, materials=MATERIALS)

    def box(part, piv, origin, size, **paint):
        """A cube given in rest-pose world coordinates, relative to its part's world pivot."""
        part.cube((origin[0] - piv[0], origin[1] - piv[1], origin[2] - piv[2]), size, **paint)

    def rel(p, q):
        return (p[0] - q[0], p[1] - q[1], p[2] - q[2])

    def cross(parent, name, pivot, kind, rot, w, h, seed):
        normal, white = _sprite(kind, w, h, seed, 'normal'), _sprite(kind, w, h, seed, 'white')
        key = f'img_{name}'
        m.palette[key], m.palette[key + '_m'] = normal, mirror(normal)
        variants['stomper_white'][key], variants['stomper_white'][key + '_m'] = white, mirror(white)
        glow = 200 if kind == 'sprout' else None
        front = dict(image=key, image_mode='stretch', glow_bright=glow, color='turf')
        back = dict(image=key + '_m', image_mode='stretch', glow_bright=glow, color='turf')
        p = parent.part(name, pivot=pivot, rot=rot)
        p.cube((-w / 2, -h, 0), (w, h, 0), color='turf', faces={'north': front, 'south': back, 'up': dict(skip=True), 'down': dict(skip=True)})
        p.cube((0, -h, -w / 2), (0, h, w), color='turf', faces={'east': front, 'west': back, 'up': dict(skip=True), 'down': dict(skip=True)})

    body = m.part('body', pivot=BP)
    # the barrel, turf and garden ride on a torso that breathes without moving the legs
    torso = body.part('torso', pivot=rel(TP, BP))
    belly_band = [(11, 'belly')]
    box(torso, TP, (-12, -17, -16), (24, 16, 32), color='hide', pattern='mc', clusters=0.15, faces={
        'east': hide(seed=3, period=7, w=32, h=16, bands=belly_band), 'west': hide(seed=4, period=7, w=32, h=16, bands=belly_band),
        'down': dict(color='belly', pattern='mc', clusters=0.3)})
    box(torso, TP, (-11, -14, -19), (22, 13, 3), color='hide', pattern='mc', clusters=0.15, faces={
        'north': hide(seed=5, period=6, w=22, h=13, bands=[(9, 'belly')])})
    box(torso, TP, (-11, -14, 16), (22, 13, 3), color='hide', pattern='mc', clusters=0.15, faces={
        'south': hide(seed=6, period=6, w=22, h=13, bands=[(9, 'belly')])})
    box(torso, TP, (-9, -1, -12), (18, 3, 23), color='belly', pattern='mc', clusters=0.3, faces={
        'east': hide('belly', seed=7, period=3, w=23, h=3), 'west': hide('belly', seed=8, period=3, w=23, h=3)})
    # a blanket of pink coral turf over the back, shoulders and rump, hanging down in ragged grass tufts
    box(torso, TP, (-12.5, -17.5, -16.5), (25, 7, 33), color='turf', pattern='mc', faces={
        'north': turf(25, 7, seed=11, phase=0), 'south': turf(25, 7, seed=12, phase=5), 'east': turf(33, 7, seed=13, phase=2),
        'west': turf(33, 7, seed=14, phase=7), 'up': turf(25, 33, True, seed=15), 'down': dict(skip=True)})
    box(torso, TP, (-11.5, -14.5, -19.5), (23, 5, 4), color='turf', pattern='mc', faces={
        'north': turf(23, 5, seed=16, phase=3), 'east': turf(4, 5, seed=17, fringe=1), 'west': turf(4, 5, seed=18, fringe=1),
        'up': turf(23, 4, True, seed=19), 'down': dict(skip=True), 'south': dict(skip=True)})
    box(torso, TP, (-11.5, -14.5, 15.5), (23, 5, 4), color='turf', pattern='mc', faces={
        'south': turf(23, 5, seed=20, phase=4), 'east': turf(4, 5, seed=21, fringe=1), 'west': turf(4, 5, seed=22, fringe=1),
        'up': turf(23, 4, True, seed=23), 'down': dict(skip=True), 'north': dict(skip=True)})
    # the domed back: two layers of turf, a garden on top
    box(torso, TP, (-11, -20, -14), (22, 3, 28), color='turf', pattern='mc', faces={
        'north': turf(22, 3, seed=24, fringe=1), 'south': turf(22, 3, seed=25, fringe=1), 'east': turf(28, 3, seed=26, fringe=1),
        'west': turf(28, 3, seed=27, fringe=1), 'up': turf(22, 28, True, seed=28), 'down': dict(skip=True)})
    box(torso, TP, (-8, -23, -10), (16, 3, 20), color='turf', pattern='mc', faces={
        'north': turf(16, 3, seed=29, fringe=1), 'south': turf(16, 3, seed=30, fringe=1), 'east': turf(20, 3, seed=31, fringe=1),
        'west': turf(20, 3, seed=32, fringe=1), 'up': turf(16, 20, True, seed=33), 'down': dict(skip=True)})
    garden = torso.part('garden', pivot=rel((0, -20, 0), TP))
    for i, (sp, x, z, y, ry, w, h) in enumerate(GARDEN):
        cross(garden, f'plant_{i}', (x, y + 20 + 0.5, z), sp, (0, ry, 0), w, h, 100 + i * 7)
    tail = torso.part('tail', pivot=rel((0, -11, 19), TP), rot=(0.35, 0, 0))
    tail.cube((-1, 0, -1), (2, 10, 2), **hide(seed=40, period=3, w=2, h=10, clusters=0.1))
    tuft = tail.part('tail_tuft', pivot=(0, 9, 0))
    tuft.cube((-1.5, 0, -1.5), (3, 4, 3), color='turf', pattern='mc', clusters=0.3, streaks=1.2, fringe=1, faces={
        'up': dict(color='moss', pattern='mc', clusters=0.2)})

    head = body.part('head', pivot=rel(HP, BP))
    eyes = {}
    for flip, face in ((0, 'east'), (1, 'west')):
        eyes[face] = dict(color='hide', pattern='mc', clusters=0.2, hd=True, map=eye_face(24, 30, 7, 13, 'neutral', flip),
                          keys=dict(WRINKLE, **EYE_KEYS), expr={e: eye_face(24, 30, 7, 13, e, flip) for e in EXPRS})
    box(head, HP, (-8, -23, -30), (16, 15, 12), color='hide', pattern='mc', clusters=0.15, faces={
        'north': hide(seed=50, period=5, w=16, h=15), 'east': eyes['east'], 'west': eyes['west']})
    box(head, HP, (-9, -14, -28), (18, 6, 9), color='hide', pattern='mc', clusters=0.15, faces={
        'east': hide(seed=51, period=4, w=9, h=6), 'west': hide(seed=52, period=4, w=9, h=6), 'north': hide(seed=53, period=4, w=18, h=6)})
    box(head, HP, (-6, -21, -31), (12, 5, 1), color='hide', pattern='mc', clusters=0.2, faces={'north': hide('hide', seed=54, period=4, w=12, h=5)})
    box(head, HP, (-4.5, -17, -33), (9, 9, 3), color='hide', pattern='mc', clusters=0.25, faces={
        'north': hide(seed=55, period=3, w=9, h=9, wob=0.6), 'east': hide(seed=56, period=3, w=3, h=9), 'west': hide(seed=57, period=3, w=3, h=9)})
    # a little turf cap on its crown, its fringe hanging over the brow like a forelock, a teal tuft on top
    box(head, HP, (-8.5, -26, -30.5), (17, 5, 12), color='turf', pattern='mc', faces={
        'north': turf(17, 5, seed=60, fringe=3, phase=4), 'east': turf(12, 5, seed=61, fringe=3, phase=1),
        'west': turf(12, 5, seed=62, fringe=3, phase=8), 'south': turf(17, 5, seed=63, fringe=2, phase=2), 'up': turf(17, 12, True, seed=64),
        'down': dict(skip=True)})
    cross(head, 'crown_tuft', rel((2, -26, -24), HP), 'teal', (0, 0.6, 0), 7, 7, 71)
    jaw = head.part('jaw', pivot=rel((0, -8, -25), HP))
    jaw.cube((-4, 0, -6), (8, 2, 7), color='hide', pattern='mc', clusters=0.2, rim=False, faces={
        'up': dict(color='mouth', pattern='mc', clusters=0.2, hd=True, map=rows(16, 14, lambda x, y: 'l' if y < 3 and 3 < x < 12 else '.'),
                   keys={'l': 'mouth_l'}),
        'south': dict(color='mouth_d', pattern='mc', clusters=0.0), 'down': dict(color='belly', pattern='mc', clusters=0.2)})
    for side, sx in (('left', 1), ('right', -1)):
        tusk = head.part(f'{side}_tusk', pivot=rel((4 * sx, -9, -30.5), HP), rot=(0.55, 0, 0))
        tusk.cube((-1, -1, -5), (2, 2, 5), color='bone', pattern='mc', clusters=0.1, rim=False, faces={
            'south': dict(color='hide_d', pattern='mc', clusters=0.0)})
        tip = tusk.part(f'{side}_tusk_tip', pivot=(0, 0, -4.5), rot=(-0.85, 0, 0))
        tip.cube((-1, -1, -3), (2, 2, 3), color='bone', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='bone_l', pattern='mc', clusters=0.0)})
        ear = head.part(f'{side}_ear', pivot=rel((8 * sx, -23, -23), HP), rot=(0, 0.75 * sx, 0.22 * sx))
        # two planes: the coral-pink inside (in front when the ear is spread) and the hide behind it
        flip = sx < 0
        inner = dict(color='ear', pattern='mc', clusters=0.3, hd=True, map_material=True, keys={'v': 'vein', 'h': 'hide', 'p': 'ear_l'},
                     map=ear_map(28, 32, True, flip))
        inner_b = dict(inner, map=ear_map(28, 32, True, not flip))
        outer = dict(color='hide', pattern='mc', clusters=0.15, hd=True, map_material=True, keys={'w': 'hide_d', 'h': 'hide_l', 'l': 'hide_l', 'd': 'hide_d'},
                     map=ear_map(28, 32, False, flip))
        outer_b = dict(outer, map=ear_map(28, 32, False, not flip))
        ear.cube((0.6 * sx, 0, 0), (0, 16, 14), color='ear', faces={'east': inner, 'west': inner_b, 'up': dict(skip=True), 'down': dict(skip=True)})
        ear.cube((0, 0, 0), (0, 16, 14), color='hide', faces={'east': outer, 'west': outer_b, 'up': dict(skip=True), 'down': dict(skip=True)})
    seg = head
    piv = rel((0, -10, -31.5), HP)
    for i, (w, ln, rx) in enumerate(TRUNK):
        seg = seg.part(f'trunk_{i}', pivot=piv, rot=(rx, 0, 0))
        tip_seg = i == len(TRUNK) - 1
        side = lambda s: hide(seed=80 + i * 4 + s, period=3, wob=0.5, gap=0.1, w=w, h=ln + 1, clusters=0.2)
        faces = {'north': side(0), 'south': hide('belly', seed=81 + i * 4, period=3, wob=0.4, gap=0.0, w=w, h=ln + 1, clusters=0.1),
                 'east': side(2), 'west': side(3)}
        if tip_seg:
            faces['down'] = dict(color='hide_l', pattern='mc', clusters=0.0, hd=True,
                                 map=rows(w * 2, w * 2, lambda x, y: 'n' if y in (2, 3) and x in (1, 4) else ('r' if x in (0, 5) or y in (0, 5) else '.')),
                                 keys={'n': 'nostril', 'r': 'hide'})
        seg.cube((-w / 2, 0, -w / 2), (w, ln + 1, w), color='hide', pattern='mc', clusters=0.2, faces=faces)
        if tip_seg:
            # the "finger" at the trunk's tip that it grips with
            seg.cube((-1, ln, -w / 2 - 0.5), (2, 1.5, 1.5), color='hide_l', pattern='mc', clusters=0.0, rim=False)
        piv = (0, ln, 0)

    for name, x, z in LEGS:
        leg = body.part(f'{name}_leg', pivot=rel((x, -3, z), BP))
        leg.cube((-4, -1, -4), (8, 13, 8), color='hide', pattern='mc', clusters=0.15, faces={
            f: hide(seed=zlib.crc32(f'{name}{f}'.encode()) % 997, period=7, gap=0.5, w=8, h=13) for f in ('north', 'south', 'east', 'west')})
        shin = leg.part(f'{name}_shin', pivot=(0, 12, 0))
        shin.cube((-3.5, 0, -3.5), (7, 11, 7), color='shin', pattern='mc', clusters=0.15, faces={
            f: hide('shin', seed=zlib.crc32(f'{name}{f}1'.encode()) % 997, period=5, gap=0.45, w=7, h=11, keys=SHIN_WRINKLE) for f in ('north', 'south', 'east', 'west')})
        # a knobbly knee
        shin.cube((-3, -0.5, -4.25), (6, 3, 1), color='hide', pattern='mc', clusters=0.1, rim=False, faces={
            'north': hide(seed=zlib.crc32(f'{name}2'.encode()) % 997, period=2, w=6, h=3, wob=0.3, gap=0.0)})
        foot = shin.part(f'{name}_foot', pivot=(0, 11, 0))
        foot.cube((-4, 0, -4), (8, 4, 8), color='shin', pattern='mc', clusters=0.25, faces={
            'north': dict(color='shin', pattern='mc', clusters=0.2, hd=True, map=nails(16, 8, 3), keys=NAIL_KEYS),
            'east': dict(color='shin', pattern='mc', clusters=0.2, hd=True, map=nails(16, 8, 2), keys=NAIL_KEYS),
            'west': dict(color='shin', pattern='mc', clusters=0.2, hd=True, map=nails(16, 8, 2), keys=NAIL_KEYS),
            'south': dict(color='shin', pattern='mc', clusters=0.2, hd=True, map=['.' * 16] * 7 + ['k' * 16], keys=NAIL_KEYS),
            'down': dict(color='pad', pattern='mc', clusters=0.4)})
    check_rig()
    return m


def check_rig():
    """StomperRig.java (the grab's trunk rig) must agree with this geometry."""
    import re
    src = open(os.path.join(ROOT, 'src/main/java/com/thesift/entity/StomperRig.java')).read()

    def const(name):
        return float(re.search(name + r' = (-?[0-9.]+)F;', src).group(1))
    want = {'BODY_Y': BP[1], 'BODY_Z': BP[2], 'HEAD_Y': HP[1] - BP[1], 'HEAD_Z': HP[2] - BP[2], 'TRUNK_Y': -10 - HP[1], 'TRUNK_Z': -31.5 - HP[2]}
    for k, v in want.items():
        assert abs(const(k) - v) < 1e-6, f'StomperRig.{k} is {const(k)}, tools/stomper.py has {v}'
    seg = [float(x.rstrip('F')) for x in src.split('SEG = {')[1].split('}')[0].split(', ')]
    rest = [float(x.rstrip('F')) for x in src.split('REST = {')[1].split('}')[0].split(', ')]
    assert seg == [float(t[1]) for t in TRUNK] and rest == [t[2] for t in TRUNK], 'StomperRig SEG/REST differ from TRUNK'
