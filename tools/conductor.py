"""C3 Conductor: the Conductor (entity id dictator) remade, his kaiju final form, and the 3D Conductor's Staff.

* ``dictator()`` - the Conductor himself: tall, gaunt and stooped, in a tattered tailcoat that sculk
  has grown through, a cracked porcelain mask with glowing eye slits and a stitched grin, four
  horn-ears swept back from his skull, a gold crest, long clawed fingers and a black baton. Painted
  at res 2; DictatorRenderer draws him 1.5x (about six blocks to the ear tips).
* ``dictator_kaiju()`` - his final movement: a hunched sculk colossus. The mask has split in two
  and swings open on a maw full of glowing notes; a bank of bone-and-brass organ pipes rises from
  his back; arms long enough to walk on their knuckles; the rags of the tailcoat still hang from
  his shoulders and hips. Painted at res 3 like the Thumper Titan (tools/thumper_titan.py).
* ``assets(GA)`` / ``textures(out)`` - the Conductor's Staff as a real 3D item model (shaft, a
  two-faced mask head with four horn-ears, gems, orb and floating notes), the flat sprite kept for
  the inventory through ``minecraft:select`` on ``minecraft:display_context``.

Part names are what client/model/boss/DictatorModel.java and DictatorKaijuModel.java animate.
Top-level imports stay stdlib-only: mobs.py -> bosses.py imports this module while mobs is loading.
"""
import math
import os
import random

from PIL import Image


def _gen(w, h, fn):
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def _tex(size, res):
    return int(math.ceil(size)) * res


# =========================================================================== shared texture maps

def cloth(w, h, seed, tears=0.0, stripe=4, crease=True):
    """Tailcoat cloth: a faint pinstripe, worn creases and tears where sculk has grown through -
    ragged dark holes crusted with teal sculk and glowing specks, their edges frayed."""
    rnd = random.Random(seed)
    holes = [(rnd.uniform(1, w - 2), rnd.uniform(2, h - 2), rnd.uniform(1.8, 3.8)) for _ in range(int(w * h * tears / 70))]

    def fn(x, y):
        best = 0.0
        for (bx, by, r) in holes:
            best = max(best, 1.0 - math.hypot(x - bx, (y - by) * 0.7) / r)
        if best > 0.55:
            return 'g' if rnd.random() < 0.22 else ('S' if best > 0.75 else 's')
        if best > 0.32:
            return 's' if rnd.random() < 0.55 else 'v'
        if best > 0.12:
            return 'k'
        if crease and (x * 5 + y * 3 + seed) % 23 == 0:
            return 'k'
        if stripe and x % stripe == 0 and (y + x) % 9 != 0:
            return 'p'
        return '.'
    return _gen(w, h, fn)


def tatter(rows, depth, seed, holes=0):
    """Rips the bottom edge of a plane into rags (transparent texels), plus a few holes."""
    rnd = random.Random(seed)
    h, w = len(rows), len(rows[0])
    cut = [int(depth * (0.25 + 0.75 * abs(math.sin(x * 0.83 + seed)) * (0.5 + 0.5 * rnd.random()))) for x in range(w)]
    pits = [(rnd.uniform(1, w - 1), rnd.uniform(h * 0.3, h * 0.85), rnd.uniform(0.8, 1.6)) for _ in range(holes)]
    out = []
    for y, r in enumerate(rows):
        row = []
        for x, ch in enumerate(r):
            if y >= h - cut[x] or any(math.hypot(x - px, y - py) < pr for (px, py, pr) in pits):
                row.append('_')
            elif y >= h - cut[x] - 1 or any(math.hypot(x - px, y - py) < pr + 0.9 for (px, py, pr) in pits):
                row.append('k')
            else:
                row.append(ch)
        out.append(''.join(row))
    return out


NOTE_GLYPH = ['...GG.', '...GgG', '...G.g', '...G..', '.ggG..', 'gGGG..', '.gg...']


def stamp(rows, glyph, at):
    rows = [list(r) for r in rows]
    for j, gr in enumerate(glyph):
        for i, ch in enumerate(gr):
            x, y = at[0] + i, at[1] + j
            if ch != '.' and 0 <= y < len(rows) and 0 <= x < len(rows[0]):
                rows[y][x] = ch
    return [''.join(r) for r in rows]


def crack_path(rows, start, seed, steps, ch='c', edge='k', drift=(0.35, 1.0)):
    """A branching crack walked down a face: glowing core, dark edge on one side."""
    rnd = random.Random(seed)
    rows = [list(r) for r in rows]
    h, w = len(rows), len(rows[0])
    x, y = start
    for _ in range(steps):
        ix, iy = int(round(x)), int(round(y))
        if 0 <= ix < w and 0 <= iy < h and rows[iy][ix] not in '_v':
            rows[iy][ix] = ch
            if 0 <= ix + 1 < w and rows[iy][ix + 1] not in '_vgGc':
                rows[iy][ix + 1] = edge
        x += rnd.choice((-1, 0, 0, 1)) * drift[0] + (rnd.random() - 0.5) * 0.6
        y += drift[1]
        if rnd.random() < 0.15:
            bx, by = x, y
            d = rnd.choice((-1, 1))
            for _ in range(rnd.randint(2, 4)):
                bx += d
                by += rnd.choice((0, 1))
                if 0 <= int(bx) < w and 0 <= int(by) < h and rows[int(by)][int(bx)] not in '_v':
                    rows[int(by)][int(bx)] = edge
    return [''.join(r) for r in rows]


# =========================================================================== the Conductor

MASK_EXPR = {
    # eye slant (outer end higher), slit half-thickness, mouth half-thickness, glowing?, extra cracks
    'neutral': (2.2, 0.75, 0.45, True, 0),
    'angry': (3.4, 0.55, 0.9, True, 1),
    'hurt': (1.2, 0.45, 0.55, True, 3),
    'dead': (1.6, 0.6, 0.45, False, 3),
}


def mask_face(expr='neutral', w=14, h=22):
    """The Conductor's mask, front: cracked porcelain shaped like a shield that narrows to a pointed
    chin; two slanted eye slits glowing sculk-cyan under a heavy brow; dark tear streaks; a long
    thin grin stitched shut; a crack running from the brow through the right eye."""
    slant, eye_t, mouth_t, lit, extra = MASK_EXPR[expr]
    cx = (w - 1) / 2

    def fn(x, y):
        dx = abs(x - cx)
        half = 7.0 if y >= 2 else 5.0 + y * 1.2
        if y > 13:
            half = 7.0 - (y - 13) * 0.62
        if dx > half:
            return '_'
        if dx > half - 0.9:
            return 'd' if y > 6 else 'l'
        # eye slits
        if 1.2 <= dx <= 5.8:
            u = (dx - 1.2) / 4.6
            c = 9.8 - u * slant
            d = abs(y - c)
            if d < eye_t + 0.05 * (1 - abs(u - 0.5) * 2):
                if not lit:
                    return 'v'
                return 'G' if (0.25 < u < 0.7 and d < eye_t * 0.6) else 'g'
            if d < eye_t + 0.55:
                return 'k'
        # thin tear streaks running down from the outer corners
        if 11 <= y <= 15 and abs(dx - (4.9 + (y - 11) * 0.25)) < 0.5:
            return 'k'
        # the stitched grin
        if dx < 5.2:
            m = 17.2 - (dx / 5.2) ** 2 * 2.6
            d = abs(y - m)
            if d < mouth_t:
                return ('g' if lit else 'v') if dx < 4.6 else 'v'
            if d < mouth_t + 1.2 and x % 2 == 0 and dx < 4.4:
                return 'k'
        if 11 <= y <= 13 and 2.5 < dx < 5.5 and (x + y) % 3 == 0:
            return 'd'
        if y < 5 and x < cx and (x + y) % 4 == 0:
            return 'l'
        return '.'
    rows = _gen(w, h, fn)
    rows = crack_path(rows, (w - 5, 0), 7, 9, ch='c' if lit else 'k', drift=(0.8, 1.0))
    for i in range(extra):
        rows = crack_path(rows, (2 + i * 4, 12 + (i % 2) * 3), 20 + i, 6, ch='c' if lit else 'k')
    return rows


def chest_front(w, h, seed):
    """The tailcoat from the front: crimson-piped lapels in a deep V over a dark waistcoat with brass
    buttons, a white jabot pinned with a sculk-gem brooch, and a tear on one side where sculk has
    burst through."""
    cx = (w - 1) / 2
    tear = cloth(w, h, seed, tears=0.0, stripe=4)
    rnd = random.Random(seed)
    holes = [(w * 0.18, h * 0.72, 2.6), (w * 0.85, h * 0.35, 1.8)]

    def fn(x, y):
        for (bx, by, r) in holes:
            k = 1.0 - math.hypot(x - bx, (y - by) * 0.8) / r
            if k > 0.45:
                return 'g' if rnd.random() < 0.2 else ('S' if k > 0.7 else 's')
            if k > 0.15:
                return 'v' if rnd.random() < 0.5 else 'k'
        dx = abs(x - cx)
        vw = 4.6 - y * 0.2
        if y < 7 and dx < 2.6 - y * 0.18:
            if 3 <= y <= 5 and dx < 1.0:
                return 'G' if y == 4 and dx < 0.6 else 'g'
            return 'J' if y % 2 else 'j'
        if dx < vw:
            if dx < 0.8 and y > 7 and y % 4 == 2:
                return 'b'
            if dx < 0.8 and y > 7 and y % 4 == 3:
                return 'B'
            return 'w' if (x + y) % 6 else 'W'
        if dx < vw + 0.7:
            return 'P'
        if dx < vw + 2.2:
            return 'L'
        return tear[y][x]
    return _gen(w, h, fn)


def human_palette():
    from mobs import SCULK
    from bosses import WARDEN
    pal = dict(SCULK)
    pal.update({k: v for k, v in WARDEN.items() if k.startswith('sculk')})
    pal.update({
        'porc': '#e6dfcf', 'porc_l': '#fbf6ea', 'porc_d': '#b4ab99', 'porc_k': '#5e584d', 'glow_c': '#c8fdff',
        'coat': '#141927', 'coat_l': '#222a3f', 'coat_d': '#090c14',
        'lining': '#5a1a4c', 'lining_l': '#7a3168', 'lining_d': '#2a0a24',
        'vest': '#2b2033', 'vest_l': '#3f3049', 'vest_d': '#171019',
        'brass': '#b89a52', 'brass_l': '#e2c77e', 'brass_d': '#7f6a35',
        'skin': '#2b303b', 'skin_l': '#3e4553', 'skin_d': '#171a21',
        'claw': '#d8d1bd', 'claw_l': '#efe9d8', 'claw_d': '#8b846f',
        'horn': '#2a2030', 'horn_l': '#43344c', 'horn_d': '#150e19',
        'shirt': '#d9d3c4', 'shirt_l': '#f2ede1', 'shirt_d': '#a39c8c',
        'baton': '#17121c', 'baton_l': '#2c2433', 'baton_d': '#0b080e', 'tooth': '#e2e6cc',
    })
    return pal


CK = {'p': 'coat_l', 'k': 'coat_d', 'v': 'void', 's': 'sculk', 'S': 'sculk_l', 'g': 'glow', 'G': 'glow_c'}
MK = {'l': 'porc_l', 'd': 'porc_d', 'k': 'porc_k', 'v': 'void', 'g': 'glow', 'G': 'glow_c', 'c': 'glow'}


def dictator():
    """The Conductor (see the module docstring). Model units: the ground is y = 24."""
    from modelkit import Model
    import bosses as BS
    R = 2
    pal = human_palette()
    m = Model('dictator', (128, 192), pal, {'dictator': {}}, res=R, expressions=['angry', 'hurt', 'dead'])

    def coat(w, h, seed, tears=0.25, **kw):
        return dict(color='coat', pattern='mc', clusters=0.0, hd=True, map=cloth(w, h, seed, tears), keys=CK, glow_keys='gG', **kw)

    def coat_box(sz, seed, tears=0.25, front=None, top=False):
        """Face specs for a coat-covered box."""
        w, h, d = (_tex(s, R) for s in sz)
        faces = {'north': coat(w, h, seed, tears), 'south': coat(w, h, seed + 1, tears * 1.6), 'east': coat(d, h, seed + 2, tears),
                 'west': coat(d, h, seed + 3, tears)}
        if front:
            faces['north'] = front
        if top:
            faces['up'] = dict(color='coat_l', pattern='mc', clusters=0.2)
        return dict(color='coat', pattern='mc', clusters=0.0, faces=faces)

    skin = dict(color='skin', pattern='mc', clusters=0.3)
    horn = dict(color='horn', pattern='mc', clusters=0.0, rim=False)

    # --- long thin legs in striped trousers, spats, pointed shoes
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(2.2 * sx, -1, 0.5), rot=(-0.12, 0, 0))
        leg.cube((-1.3, 0, -1.3), (2.6, 13, 2.6), **coat_box((2.6, 13, 2.6), 10 + sx, 0.15))
        shin = leg.part(f'{side}_shin', pivot=(0, 13, 0), rot=(0.25, 0, 0))
        shin.cube((-1.1, 0, -1.1), (2.2, 10.5, 2.2), **coat_box((2.2, 10.5, 2.2), 14 + sx, 0.1))
        shin.cube((-1.3, 8, -1.3), (2.6, 2.5, 2.6), color='shirt_d', pattern='mc', clusters=0.0, rim=False,
                  faces={'north': dict(color='shirt_d', pattern='mc', clusters=0.0, hd=True, map=['......', '..bb..', '......', '..bb..', '......'],
                                       keys={'b': 'brass'})})
        foot = shin.part(f'{side}_foot', pivot=(0, 10.5, 0), rot=(-0.13, 0, 0))
        foot.cube((-1.3, 0, -4.2), (2.6, 1.6, 5.6), color='baton', pattern='mc', clusters=0.0,
                  faces={'up': dict(color='baton', pattern='mc', clusters=0.0, hd=True, map=['......'] * 2 + ['..ll..'] + ['......'] * 9,
                                    keys={'l': 'baton_l'})})
        foot.cube((-0.6, 0.6, -6.0), (1.2, 1.0, 2.0), color='baton_l', pattern='mc', clusters=0.0, rim=False)

    body = m.part('body', pivot=(0, -1, 0))
    body.cube((-3, -2, -2), (6, 3, 4), **coat_box((6, 3, 4), 20, 0.1))
    torso = body.part('torso', pivot=(0, -2, 0), rot=(0.16, 0, 0))
    torso.cube((-2.6, -6, -1.8), (5.2, 6, 3.6), color='vest', pattern='mc', clusters=0.2, faces={
        'north': dict(color='vest', pattern='mc', clusters=0.0, hd=True, keys={'b': 'brass', 'B': 'brass_d', 'w': 'vest', 'W': 'vest_l'},
                      map=['wwwwbbwwwww', 'wwwwBBwwwww'] + ['wwwwwwwwwww'] * 2 + ['wwwwbbwwwww', 'wwwwBBwwwww'] + ['wwwwwwwwwww'] * 6)})
    cw, ch = _tex(9, R), _tex(11, R)
    front = dict(color='coat', pattern='mc', clusters=0.0, hd=True, map=chest_front(cw, ch, 31),
                 keys=dict(CK, j='shirt', J='shirt_d', b='brass', B='brass_d', w='vest', W='vest_l', P='lining', L='coat_l'), glow_keys='gG')
    torso.cube((-4.5, -17, -2.75), (9, 11, 5.5), **coat_box((9, 11, 5.5), 30, 0.3, front=front))
    # high, sharp shoulders and a tall stiff collar that stands up behind the head
    torso.cube((-6, -18, -2.5), (12, 2, 5), **coat_box((12, 2, 5), 40, 0.1, top=True))
    torso.cube((-3.5, -22.5, 0.75), (7, 5, 1), color='coat_d', pattern='mc', clusters=0.0, rim=False,
               faces={'north': dict(color='lining', pattern='mc', clusters=0.0, hd=True, map=['l' * 14] + ['.' * 14] * 9, keys={'l': 'lining_l'}),
                      'south': coat(14, 10, 41, 0.2)})
    # coat tails: two long rags split up the back, sculk growing through the cloth
    for side, sx in (('left', 1), ('right', -1)):
        tail = torso.part(f'{side}_tail', pivot=(2.2 * sx, -7, 2.8), rot=(0.12, 0, 0.04 * sx))
        rows = tatter(cloth(10, 44, 50 + sx, tears=0.35), 9, 3 + sx, holes=2)
        tail.cube((-2.2, 0, 0), (4.4, 22, 0), color='coat', pattern='mc', clusters=0.0, rim=False, faces={
            'south': dict(color='coat', pattern='mc', clusters=0.0, rim=False, hd=True, map=rows, keys=CK, glow_keys='gG'),
            'north': dict(color='coat', pattern='mc', clusters=0.0, rim=False, hd=True, map=rows, keys=CK, glow_keys='gG')})
    # sculk bursting out between the shoulder blades: crusts and two Warden tendrils
    growth = torso.part('back_growth', pivot=(0, -14, 2.75))
    for (x, y, s) in ((-2.5, -1, 2.2), (1.2, -3, 1.6), (2.6, 1.5, 1.2), (-0.5, 2.5, 1.4)):
        growth.cube((x - s / 2, y - s / 2, -0.2), (s, s, 0.8), color='sculk', pattern='mc', clusters=0.0, rim=False,
                    faces={'south': dict(color='sculk', pattern='mc', clusters=0.0, hd=True,
                                         map=_gen(_tex(s, R), _tex(s, R), lambda xx, yy: 'g' if (xx + yy) % 3 == 0 else 'S'), keys=CK, glow_keys='g')})
    for side, sx in (('left', 1), ('right', -1)):
        BS.tendril(growth, f'{side}_tendril', (2.0 * sx, -2, 0.3), (-0.5, 0, 0.5 * sx), 5)

    # --- the head: a gaunt dark skull behind the mask
    neck = torso.part('neck', pivot=(0, -17, -0.4), rot=(0.12, 0, 0))
    neck.cube((-1, -3.5, -1), (2, 3.5, 2), **skin)
    neck.cube((-1.8, -1.5, -1.6), (3.6, 1.5, 3.2), color='shirt', pattern='mc', clusters=0.0, rim=False)
    head = neck.part('head', pivot=(0, -3.5, -0.2))
    head.cube((-2.75, -9, -2.6), (5.5, 9, 5.5), color='skin_d', pattern='mc', clusters=0.3, faces={
        f: dict(color='skin_d', pattern='mc', clusters=0.0, hd=True, map=BS.sculk_patches(_tex(5.5, R), 18, 60 + i, 0.35, 0.1),
                keys={'s': 'sculk', 'S': 'sculk_l', 'g': 'glow', 'k': 'skin'}, glow_keys='g')
        for i, f in enumerate(('south', 'east', 'west', 'up'))})
    mask = head.part('mask', pivot=(0, -4.5, -2.6))
    mrows = {k: mask_face(k) for k in MASK_EXPR}
    mask.cube((-3.5, -5, -1.0), (7, 11, 1.0), color='porc', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='porc', pattern='mc', clusters=0.0, rim=False, hd=True, map=mrows['neutral'], keys=MK, glow_keys='gGc',
                      expr={k: v for k, v in mrows.items() if k != 'neutral'}),
        'south': dict(color='porc_d', pattern='mc', clusters=0.0, rim=False)})
    # the heavy brow and the chin point
    mask.cube((-3.75, -3.0, -1.4), (7.5, 0.8, 0.5), color='porc', pattern='mc', clusters=0.0, rim=False,
              faces={'up': dict(color='porc_l', pattern='mc', clusters=0.0), 'down': dict(color='porc_k', pattern='mc', clusters=0.0)})
    mask.cube((-0.75, 5.8, -0.9), (1.5, 1.2, 0.8), color='porc_d', pattern='mc', clusters=0.0, rim=False)
    crown = head.part('crown', pivot=(0, -9, -0.5))
    for i, x in enumerate((-2.25, -0.75, 0.75, 2.25)):
        tall = 1.6 + (i in (1, 2)) * 1.4
        crown.cube((x - 0.4, -tall, -0.4), (0.8, tall, 0.8), color='brass', pattern='mc', clusters=0.0, rim=False,
                   faces={'up': dict(color='glow', pattern='mc', clusters=0.0, glow=True) if i in (1, 2) else dict(color='brass_l', pattern='mc', clusters=0.0)})
    crown.cube((-2.75, -0.6, -0.6), (5.5, 0.6, 1.2), color='brass_d', pattern='mc', clusters=0.0, rim=False)
    # four horn-ears: long, flat, ridged horns shaped like ears, glowing along the inner ridge
    for side, sx in (('left', 1), ('right', -1)):
        for tier, (py, pz, length, tip, rot, rot2) in (('upper', (-8.0, 0.2, 5, 4, (-0.35, 0.25 * sx, 0.45 * sx), (-0.3, 0, 0.35 * sx))),
                                                       ('lower', (-5.2, 0.8, 4, 3, (0.25, 0.55 * sx, 1.05 * sx), (0.1, 0, 0.4 * sx)))):
            ear = head.part(f'{side}_ear_{tier}', pivot=(2.6 * sx, py, pz), rot=rot)
            ridge = _gen(_tex(2.4, R), length * R, lambda x, y: 'g' if x in (2, 3) and y % 3 else ('l' if x == 0 else ('d' if y % 3 == 0 or x == 5 else '.')))
            ear.cube((-1.2, -length, -0.6), (2.4, length, 1.2), **horn, faces={
                'north': dict(**horn, hd=True, map=ridge, keys={'g': 'glow_d', 'l': 'horn_l', 'd': 'horn_d'}, glow_keys='g'),
                'south': dict(**horn, hd=True, map=ridge, keys={'g': 'horn_l', 'l': 'horn_l', 'd': 'horn_d'})})
            t = ear.part(f'{side}_ear_{tier}_tip', pivot=(0, -length, 0), rot=rot2)
            t.cube((-0.75, -tip, -0.45), (1.5, tip, 0.9), color='horn_l', pattern='mc', clusters=0.0, rim=False,
                   faces={'up': dict(color='glow', pattern='mc', clusters=0.0, glow=True),
                          'north': dict(color='horn_l', pattern='mc', clusters=0.0, hd=True, map=['.g..'] * (tip * R - 2) + ['gggg', 'gggg'],
                                        keys={'g': 'glow'}, glow_keys='g')})

    # --- long arms in tailcoat sleeves, white cuffs, grey hands with long clawed fingers
    for side, sx in (('left', 1), ('right', -1)):
        arm = torso.part(f'{side}_arm', pivot=(5.4 * sx, -16.6, 0), rot=(0, 0, -0.06 * sx))
        arm.cube((-1.6, -1.6, -2.1), (3.2, 3.2, 4.2), **coat_box((3.2, 3.2, 4.2), 70 + sx, 0.1, top=True))
        arm.cube((-1.1, 0, -1.1), (2.2, 12.5, 2.2), **coat_box((2.2, 12.5, 2.2), 72 + sx, 0.25))
        fore = arm.part(f'{side}_forearm', pivot=(0, 12.5, 0), rot=(-0.12, 0, 0))
        fore.cube((-1, 0, -1), (2, 9, 2), **coat_box((2, 9, 2), 76 + sx, 0.25))
        fore.cube((-1.35, 7.6, -1.35), (2.7, 1.6, 2.7), color='shirt', pattern='mc', clusters=0.0, rim=False,
                  faces={f: dict(color='shirt', pattern='mc', clusters=0.0, hd=True, map=['ssssss', '......', '.s.s.s', '......'], keys={'s': 'shirt_d'})
                         for f in ('north', 'south', 'east', 'west')})
        hand = fore.part(f'{side}_hand', pivot=(0, 9.2, 0))
        hand.cube((-1.2, 0, -0.85), (2.4, 2.4, 1.7), **skin)
        for k, fx in enumerate((-0.8, 0.0, 0.8)):
            f = hand.part(f'{side}_finger_{k}', pivot=(fx, 2.3, -0.2), rot=(-0.12, 0, (k - 1) * 0.12))
            f.cube((-0.32, 0, -0.32), (0.64, 3.2 + (k == 1) * 0.6, 0.64), color='skin', pattern='mc', clusters=0.0, rim=False,
                   faces={'north': dict(color='skin', pattern='mc', clusters=0.0, hd=True, map=['..', 'dd', '..', '..', 'dd', '..', '..', '..'],
                                        keys={'d': 'skin_d'})})
            f.cube((-0.28, 3.2 + (k == 1) * 0.6, -0.5), (0.56, 1.8, 0.6), color='claw', pattern='mc', clusters=0.0, rim=False,
                   faces={'down': dict(color='claw_d', pattern='mc', clusters=0.0)})
        th = hand.part(f'{side}_thumb', pivot=(-1.1 * sx, 1.2, -0.4), rot=(-0.3, 0, 0.55 * sx))
        th.cube((-0.3, 0, -0.3), (0.6, 2.2, 0.6), color='skin', pattern='mc', clusters=0.0, rim=False)
        th.cube((-0.26, 2.2, -0.45), (0.52, 1.2, 0.55), color='claw', pattern='mc', clusters=0.0, rim=False)
        if sx < 0:
            # the baton: a black shaft with a bone grip and a glowing tip, held between the claws
            baton = hand.part('baton', pivot=(0, 2.0, -0.6), rot=(-1.45, 0, 0))
            baton.cube((-0.45, -1.2, -0.45), (0.9, 3.2, 0.9), color='claw', pattern='mc', clusters=0.0, rim=False,
                       faces={f: dict(color='claw', pattern='mc', clusters=0.0, hd=True, map=['bb', '..', 'dd', '..', 'dd', '..', 'bb'],
                                      keys={'b': 'brass', 'd': 'claw_d'}) for f in ('north', 'south', 'east', 'west')})
            baton.cube((-0.25, 2.0, -0.25), (0.5, 12, 0.5), color='baton', pattern='mc', clusters=0.0, rim=False,
                       faces={f: dict(color='baton', pattern='mc', clusters=0.0, hd=True, map=['l.'] * 24, keys={'l': 'baton_l'}) for f in ('north', 'east')})
            baton.cube((-0.35, 13.6, -0.35), (0.7, 1.0, 0.7), color='glow', pattern='mc', clusters=0.0, rim=False, glow=True)
        # threads of song: he plays the air with them once he leaves the ground
        strings = hand.part(f'{side}_strings', pivot=(0, 2, 0))
        strings.cube((0, 0, -1.5), (0, 16, 3), color='glow', pattern='mc', clusters=0.0, rim=False, faces={
            f: dict(color='glow', pattern='mc', clusters=0.0, rim=False, hd=True, map=['g_g_g_' if f == 'east' else '_g_g_g'] * 32, keys={'g': 'glow'}, glow_keys='g')
            for f in ('east', 'west')})
    return m


# =========================================================================== the kaiju

def kaiju_mask_half(side, w, h, seed):
    """One half of the split mask, front: cracked porcelain with a slanted glowing eye slit, the
    split edge jagged and lit from inside, chunks broken out of it."""
    rnd = random.Random(seed)
    inner = 0 if side == 'left' else w - 1   # texture x of the split edge (the left half's inner edge is its -x side)
    teeth = [int(2.5 + 2.2 * abs(math.sin(y * 0.55 + seed)) + rnd.random() * 1.5) for y in range(h)]
    chips = [(rnd.uniform(4, w - 4), rnd.uniform(4, h - 4), rnd.uniform(1.2, 2.2)) for _ in range(3)]

    def fn(x, y):
        e = abs(x - inner)
        if e < teeth[y] - 2:
            return '_'
        if e < teeth[y]:
            return 'c' if (y + e) % 2 == 0 else 'G'
        if e < teeth[y] + 1:
            return 'k'
        if any(math.hypot(x - cx, y - cy) < r for (cx, cy, r) in chips):
            return '_'
        if any(math.hypot(x - cx, y - cy) < r + 1 for (cx, cy, r) in chips):
            return 'k'
        # the eye slit: from the split edge out and up
        ox = abs(x - inner) / max(1, w - 1)
        if 0.2 < ox < 0.85:
            c = h * 0.38 - (ox - 0.2) * h * 0.22
            d = abs(y - c)
            if d < 1.6:
                return 'G' if d < 0.7 and 0.35 < ox < 0.7 else 'g'
            if d < 2.8:
                return 'k'
            if c - 5 < y < c - 3:
                return 'd'
        if y > h - 3:
            return 'd'
        if (x + 2 * y) % 13 == 0 and y > h * 0.5:
            return 'd'
        if y < 4 and (x + y) % 3 == 0:
            return 'l'
        return '.'
    rows = _gen(w, h, fn)
    for i in range(3):
        rows = crack_path(rows, (int(w * (0.3 + 0.25 * i)), 0 if i != 1 else int(h * 0.55)), seed + i, int(h * 0.45), ch='c')
    return rows


def maw_rows(w, h):
    """The maw behind the mask: a throat of glowing sculk, ringed in teeth, full of floating notes."""
    cx, cy = (w - 1) / 2, (h - 1) / 2

    def fn(x, y):
        e = max(abs(x - cx) / (w / 2), abs(y - cy) / (h / 2))
        if e > 0.9:
            return 'w' if (x + y) % 3 else 'd'
        if e > 0.78:
            return 'v'
        r = math.hypot((x - cx) / (w / 2), (y - cy) / (h / 2))
        if r < 0.3:
            return 'G'
        if r < 0.55:
            return 'g' if (x + y) % 2 else 's'
        return 's' if (x * 3 + y) % 5 else 'g'
    rows = _gen(w, h, fn)
    for (nx, ny) in ((int(w * 0.12), int(h * 0.2)), (int(w * 0.7), int(h * 0.15)), (int(w * 0.25), int(h * 0.55)), (int(w * 0.62), int(h * 0.5))):
        rows = stamp(rows, [r.replace('g', 'G').replace('G', 'w') if (nx + ny) % 2 else r for r in NOTE_GLYPH], (nx, ny))
    return rows


def pipe_rows(w, h, seed):
    """An organ pipe from the front: bone bound in tarnished brass, lit down its left side, with
    the pipe's mouth - an arched slit glowing with the note inside it - low on its body."""
    mouth = int(h * 0.72)

    def fn(x, y):
        if y in (0, 1) or y in (h - 3, h - 2) or (y - seed) % 13 == 0:
            return 'b' if x % 4 else 'B'
        if mouth - 4 <= y <= mouth and 2 <= x <= w - 3:
            if y == mouth - 4 and (x in (2, w - 3)):
                return 'k'
            return 'G' if (y == mouth - 1 and 3 <= x <= w - 4) else ('g' if y == mouth else 'v')
        if y == mouth + 1 and 2 <= x <= w - 3:
            return 'l'
        if x == 0:
            return 'l'
        if x == w - 1:
            return 'd'
        if (x * 7 + y * 3 + seed) % 29 == 0:
            return 'k'
        return '.'
    return _gen(w, h, fn)


def kaiju_palette():
    from mobs import SCULK
    from bosses import WARDEN
    import thumper_titan as TT
    pal = dict(SCULK)
    pal.update(WARDEN)
    pal.update(TT.PAL)
    pal.update({
        'porc': '#e3dccb', 'porc_l': '#fbf6ea', 'porc_d': '#ada492', 'porc_k': '#4e483f',
        'coat': '#141927', 'coat_l': '#222a3f', 'coat_d': '#090c14',
        'lining': '#5a1a4c', 'lining_l': '#7a3168', 'lining_d': '#2a0a24',
        'brass': '#9c8248', 'brass_l': '#cdb06a', 'brass_d': '#5e4e2a',
        'pipe': '#a99f80', 'pipe_l': '#cfc6a6', 'pipe_d': '#6b644f',
        'horn': '#2a2030', 'horn_l': '#43344c', 'horn_d': '#150e19',
        'baton': '#17121c', 'baton_l': '#2c2433',
    })
    return pal


def dictator_kaiju():
    """The Conductor's kaiju form (see the module docstring). Ground at y = 24."""
    from modelkit import Model
    import bosses as BS
    import thumper_titan as TT
    R = 3
    m = Model('dictator_kaiju', (256, 256), kaiju_palette(), {'dictator_kaiju': {}}, res=R)
    WKG = dict(BS.WK, G='glow_c', k='hide_l', w='tooth', c='glow')

    def hide(w, h, seed, density=0.22, glow=0.07):
        return dict(color='hide', pattern='mc', clusters=0.0, hd=True, map=BS.sculk_patches(w, h, seed, density, glow), keys=WKG, glow_keys='gG')

    def hide_box(sz, seed, density=0.22, skip=(), **over):
        def fn(f, w, h):
            if f in skip:
                return None
            return over.get(f) or hide(w, h, seed + len(f) * 7, density * (0.6 if f == 'down' else 1.0))
        return dict(color='hide', pattern='mc', clusters=0.0, faces=TT._sides(*sz, fn))

    bone_keys = dict(TT.BK, r='bone_d', o='bone_k', B='bone_l', M='sculk', n='sculk_l', N='sculk')

    def bone(w, h, seed, crack=0.5):
        return TT.pf('bone_d', TT.plates(w, h, seed, cell=10, crack=crack, sculk=0.25), keys=bone_keys)

    def bone_box(sz, seed, crack=0.5):
        return dict(color='bone', pattern='mc', clusters=0.0, faces=TT._sides(*sz, lambda f, w, h: bone(w, h, seed + len(f), crack) if f != 'down' else None))

    def coat(w, h, seed, tears=0.4, rag=0):
        rows = cloth(w, h, seed, tears, stripe=6)
        if rag:
            rows = tatter(rows, rag, seed, holes=3)
        return dict(color='coat', pattern='mc', clusters=0.0, rim=False, hd=True, map=rows, keys=CK, glow_keys='gG')

    # --- legs: thick scaled thighs, bone knee plates, broad clawed feet
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(8.5 * sx, -3, 4))
        leg.cube((-4.5, -2, -4.5), (9, 13, 9), **hide_box((9, 13, 9), 100 + sx, skip=('up',)))
        leg.cube((-3.5, 6.5, -5.25), (7, 4.5, 1.5), **bone_box((7, 4.5, 1.5), 110 + sx))
        shin = leg.part(f'{side}_shin', pivot=(0, 11, 0))
        shin.cube((-3.75, 0, -3.75), (7.5, 13, 7.5), color='scale', pattern='mc', clusters=0.0, faces=TT._sides(7.5, 13, 7.5, lambda f, w, h, s=sx: (
            TT.pf('scale', TT.scales(w, h, 120 + s * 5 + len(f), sculk=0.3), keys=TT.SK) if f not in ('up', 'down') else None)))
        shin.cube((-2.25, 1, -4.4), (4.5, 8, 1), **bone_box((4.5, 8, 1), 125 + sx, 0.3))
        foot = shin.part(f'{side}_foot', pivot=(0, 13, -0.5))
        foot.cube((-4.75, 0, -6.5), (9.5, 3, 10), color='scale_d', pattern='mc', clusters=0.0, faces=TT._sides(9.5, 3, 10, lambda f, w, h: (
            TT.pf('scale_d', TT.scales(w, h, 130 + len(f), sculk=0.1), keys=TT.SK) if f != 'down' else None)))
        for k, cx in enumerate((-3.0, 0.0, 3.0)):
            claw = foot.part(f'{side}_toe_{k}', pivot=(cx, 1.5, -6.5), rot=(0.25, -cx * 0.06, 0))
            claw.cube((-0.9, -1, -2.6), (1.8, 2, 2.6), color='claw', pattern='mc', clusters=0.0, rim=False,
                      faces={'up': dict(color='claw', pattern='mc', clusters=0.0, hd=True, map=['dCCCd'] + ['.....'] * 7, keys={'C': 'claw_d', 'd': 'claw_d'})})
            claw.cube((-0.5, -0.2, -3.8), (1.0, 1.4, 1.2), color='claw_d', pattern='mc', clusters=0.0, rim=False)

    # --- pelvis and the rags of the tailcoat
    body = m.part('body', pivot=(0, -3, 4))
    body.cube((-9.5, -6, -6.5), (19, 9, 13), **hide_box((19, 9, 13), 140))
    for side, sx in (('left', 1), ('right', -1)):
        body.cube((9.0 if sx > 0 else -11.5, -5, -4), (2.5, 6, 8), **bone_box((2.5, 6, 8), 145 + sx))
        rag = body.part(f'{side}_tatter', pivot=(5 * sx, -4, 6.6), rot=(0.1, 0, 0.06 * sx))
        rag.cube((-5.5, 0, 0), (11, 24, 0), color='coat', pattern='mc', clusters=0.0, rim=False, faces={
            'south': coat(33, 72, 150 + sx, 0.45, rag=20), 'north': coat(33, 72, 150 + sx, 0.45, rag=20)})

    # --- the torso, hunched far forward
    torso = body.part('torso', pivot=(0, -5, 0), rot=(0.42, 0, 0))
    torso.cube((-11, -7, -8), (22, 8, 15), **hide_box((22, 8, 15), 160, skip=('up',)))
    cw, ch = _tex(28, R), _tex(21, R)
    rib = BS.ribcage(cw, ch, heart=False)
    rib = [''.join(c if c != '.' else p for c, p in zip(r, pr)) for r, pr in zip(rib, BS.sculk_patches(cw, ch, 171, 0.35, 0.12))]
    torso.cube((-14, -27, -9.5), (28, 21, 19), **hide_box((28, 21, 19), 170, north=dict(
        color='hide', pattern='mc', clusters=0.0, hd=True, map=rib, keys=dict(WKG, d='bone_d', l='bone_l', b='bone'), glow_keys='gG'),
        up=bone(cw, _tex(19, R), 172, 0.6)))
    heart = torso.part('heart', pivot=(0, -18, -9.6))
    heart.cube((-3, -3, -0.6), (6, 6, 1.2), color='glow', pattern='mc', clusters=0.0, rim=False, glow=True, faces={
        'north': dict(color='glow', pattern='mc', clusters=0.0, rim=False, hd=True, glow=True,
                      map=_gen(18, 18, lambda x, y: 'G' if math.hypot(x - 8.5, y - 8.5) < 4 else ('g' if math.hypot(x - 8.5, y - 8.5) < 7 else 'd')),
                      keys={'G': 'glow_c', 'g': 'glow', 'd': 'glow_d'})})
    # the tailcoat burst open at every seam, hanging in rags over his shoulders and back
    mw, mh, md = 31, 9, 13.5
    torso.cube((-15.5, -28.5, -2.5), (mw, mh, md), color='coat', pattern='mc', clusters=0.0, faces=TT._sides(mw, mh, md, lambda f, w, h: (
        dict(color='coat_l', pattern='mc', clusters=0.0, hd=True, map=cloth(w, h, 180, 0.5, stripe=6), keys=CK, glow_keys='gG') if f == 'up' else
        None if f == 'down' else coat(w, h, 181 + len(f), 0.5, rag=9))))
    # vertebrae standing proud between the shoulders, then the organ pipes
    for i in range(4):
        torso.cube((-1.25, -27 + i * 4.5, 10.5), (2.5, 2.5, 1.8), color='bone', pattern='mc', clusters=0.0, rim=False,
                   faces={'south': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=BS.vertebrae(9, 9), keys=WKG)})
    pipes = torso.part('pipes', pivot=(0, -28, 5.5), rot=(-0.5, 0, 0))
    pipe_paint = lambda h, seed: dict(color='pipe', pattern='mc', clusters=0.0, faces=TT._sides(2.5, h, 2.5, lambda f, w, hh: (
        dict(color='pipe', pattern='mc', clusters=0.0, hd=True, map=pipe_rows(w, hh, seed), keys={'b': 'brass', 'B': 'brass_d', 'l': 'pipe_l', 'd': 'pipe_d',
                                                                                                   'k': 'bone_k', 'v': 'void', 'g': 'glow', 'G': 'glow_c'},
             glow_keys='gG') if f == 'north' else
        dict(color='void', pattern='mc', clusters=0.0, hd=True, map=_gen(w, hh, lambda x, y: 'G' if abs(x - 4) + abs(y - 4) < 2 else ('g' if abs(x - 4) + abs(y - 4) < 3 else 'b')),
             keys={'G': 'glow_c', 'g': 'glow', 'b': 'brass_d'}, glow_keys='gG') if f == 'up' else
        dict(color='pipe', pattern='mc', clusters=0.0, hd=True, map=pipe_rows(w, hh, seed + 1), keys={'b': 'brass', 'B': 'brass_d', 'l': 'pipe_l', 'd': 'pipe_d',
                                                                                                       'k': 'bone_k', 'v': 'pipe_d', 'g': 'pipe_d', 'G': 'pipe_l'}))))
    banks = [(pipes, [(-9, 10), (-6, 15), (-3, 20), (0, 25), (3, 20), (6, 15), (9, 10)])]
    for side, sx in (('left', 1), ('right', -1)):
        banks.append((torso.part(f'{side}_pipes', pivot=(11.5 * sx, -28, 1.5), rot=(-0.45, 0, 0.42 * sx)), [(-2.6 * sx, 6), (0, 9), (2.6 * sx, 7)]))
    n = 0
    for bank, row in banks:
        for (x, h) in row:
            p = bank.part(f'pipe_{n}', pivot=(x, 0, 0))
            p.cube((-1.25, -h, -1.25), (2.5, h, 2.5), **pipe_paint(h, 190 + n))
            p.cube((-1.6, -h - 0.8, -1.6), (3.2, 0.8, 3.2), color='brass', pattern='mc', clusters=0.0, rim=False,
                   faces={'up': dict(color='void', pattern='mc', clusters=0.0, hd=True, map=_gen(10, 10, lambda xx, yy: 'g' if 2 <= xx <= 7 and 2 <= yy <= 7 else 'b'),
                                     keys={'g': 'glow', 'b': 'brass_l'}, glow_keys='g')})
            p.cube((-1.6, -1.2, -1.6), (3.2, 1.2, 3.2), color='brass_d', pattern='mc', clusters=0.0, rim=False)
            n += 1
    # sculk growths on the shoulders, like shriekers with glowing mouths, and tendrils down the back
    for i, (x, z) in enumerate(((-12.5, -6), (12.5, -5.5), (-7, 8))):
        g = torso.part(f'growth_{i}', pivot=(x, -27, z))
        g.cube((-1.5, -3, -1.5), (3, 3, 3), color='sculk', pattern='mc', clusters=0.3, faces={
            'up': TT.pf('void', _gen(9, 9, lambda xx, yy: 'G' if abs(xx - 4) + abs(yy - 4) < 2 else ('g' if abs(xx - 4) + abs(yy - 4) < 3.5 else 's')), keys=TT.PK)})
        g.cube((-2.25, -3.75, -0.3), (4.5, 0.75, 0.6), color='bone', pattern='mc', clusters=0.0, rim=False)
    for i, (x, rz) in enumerate(((-6, -0.5), (6, 0.5), (0, 0.0))):
        TT.tendril(torso, f'back_tendril_{i}', (x, -20 + abs(x), 9.6), (-0.9, 0, rz), 8 if i < 2 else 10)

    # --- neck and head: the mask split open on a maw of notes
    neck = torso.part('neck', pivot=(0, -25, -7.5), rot=(-0.6, 0, 0))
    neck.cube((-6, -6, -8), (12, 11, 9), **hide_box((12, 11, 9), 200))
    neck.cube((-1.25, -7.5, -7), (2.5, 1.5, 7), color='bone', pattern='mc', clusters=0.0, rim=False)
    head = neck.part('head', pivot=(0, -2, -7), rot=(0.18, 0, 0))
    head.cube((-10, -11, -12), (20, 13, 13), **hide_box((20, 13, 13), 210, north=dict(color='hide_d', pattern='mc', clusters=0.0)))
    head.cube((-10.5, -12, -11.5), (21, 2, 11), **bone_box((21, 2, 11), 215, 0.7))
    maw = head.part('maw', pivot=(0, 0, -12.05))
    maw.cube((-8, -10, 0), (16, 11, 0), color='sculk', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='sculk', pattern='mc', clusters=0.0, rim=False, hd=True, map=maw_rows(48, 33),
                      keys={'w': 'tooth', 'd': 'bone_d', 'v': 'void', 's': 'sculk', 'g': 'glow', 'G': 'glow_c'}, glow_keys='gG')})
    hk = {'l': 'porc_l', 'd': 'porc_d', 'k': 'porc_k', 'v': 'void', 'g': 'glow', 'G': 'glow_c', 'c': 'glow'}
    for side, sx in (('left', 1), ('right', -1)):
        half = head.part(f'mask_{side}', pivot=(10 * sx, -4.5, -12))
        x0 = -10.0 if sx > 0 else 0.0
        half.cube((x0, -6.5, -1.5), (10, 11, 1.5), color='porc', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='porc', pattern='mc', clusters=0.0, rim=False, hd=True, map=kaiju_mask_half(side, 30, 33, 220 + sx), keys=hk, glow_keys='gGc'),
            'south': dict(color='porc_k', pattern='mc', clusters=0.0, rim=False),
            'west' if sx > 0 else 'east': dict(color='glow', pattern='mc', clusters=0.0, rim=False, glow=True)})
        # a heavy brow over each eye
        half.cube((x0 + (1.2 if sx > 0 else 0.6), -5.2, -2.3), (8.2, 1.3, 1), color='porc', pattern='mc', clusters=0.0, rim=False,
                  faces={'up': dict(color='porc_l', pattern='mc', clusters=0.0), 'down': dict(color='porc_k', pattern='mc', clusters=0.0)})
    # hanging upper fangs and the hinged jaw
    head.cube((-7.5, 1.5, -12.2), (15, 2.5, 1), color='tooth', pattern='mc', clusters=0.0, rim=False,
              faces={'north': dict(color='tooth', pattern='mc', clusters=0.0, rim=False, hd=True, map=[r.replace('.', '_') for r in BS.fangs(45, 8, 3)],
                                   keys={'l': 'tooth', 'b': 'bone_l', 'd': 'bone_d'})})
    jaw = head.part('jaw', pivot=(0, 1.5, -3.5))
    jw, jd = _tex(17, R), _tex(9.5, R)
    jaw.cube((-8.5, 0, -9.5), (17, 4.5, 9.5), **hide_box((17, 4.5, 9.5), 230, up=TT.pf('void', maw_rows(jw, jd), keys={
        'w': 'tooth', 'd': 'bone_d', 'v': 'void', 's': 'sculk', 'g': 'glow', 'G': 'glow_c'})))
    jaw.cube((-7.5, -2, -9.2), (15, 2, 1), color='tooth', pattern='mc', clusters=0.0, rim=False,
             faces={'north': dict(color='tooth', pattern='mc', clusters=0.0, rim=False, hd=True,
                                  map=[r.replace('.', '_') for r in reversed(BS.fangs(45, 6, 3))], keys={'l': 'tooth', 'b': 'bone_l', 'd': 'bone_d'})})
    # four great horn-ears of cracked bone, segmented and ridged, the tips burning
    for side, sx in (('left', 1), ('right', -1)):
        for tier, (pivot, rots, segs) in (('upper', ((8 * sx, -10.5, -5), [(-0.35, 0.3 * sx, 0.5 * sx), (-0.25, 0, 0.35 * sx), (-0.3, 0, 0.3 * sx)], (9, 7, 5))),
                                          ('lower', ((10 * sx, -5, -4), [(0.2, 0.55 * sx, 1.1 * sx), (0.15, 0, 0.4 * sx)], (7, 6)))):
            parent = head
            names = [f'{side}_ear_{tier}', f'{side}_ear_{tier}_mid', f'{side}_ear_{tier}_tip'][:len(segs)]
            if len(segs) == 2:
                names[1] = f'{side}_ear_{tier}_tip'
            for k, (name, rot, length) in enumerate(zip(names, rots, segs)):
                wdt = 3.6 - k * 1.0
                seg = parent.part(name, pivot=pivot if k == 0 else (0, -segs[k - 1], 0), rot=rot)
                tipseg = k == len(segs) - 1
                W = _tex(wdt, R)
                ridge = _gen(W, length * R, lambda x, y, W=W: 'g' if x == W // 2 and y % 4 != 0 else ('l' if x == 0 else ('d' if y % 4 == 0 or x == W - 1 else '.')))
                seg.cube((-wdt / 2, -length, -wdt / 3), (wdt, length, wdt * 0.66), color='bone', pattern='mc', clusters=0.0, rim=False, faces={
                    'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=ridge, keys={'g': 'glow_d' if not tipseg else 'glow', 'l': 'bone_l', 'd': 'bone_k'},
                                  glow_keys='g'),
                    'south': dict(color='bone_d', pattern='mc', clusters=0.0, hd=True, map=ridge, keys={'g': 'bone_k', 'l': 'bone', 'd': 'bone_k'}),
                    'up': dict(color='glow', pattern='mc', clusters=0.0, glow=True) if tipseg else dict(color='bone_l', pattern='mc', clusters=0.0)})
                parent = seg
        TT.tendril(head, f'{side}_tendril', (7 * sx, -11, 0), (-0.6, 0, 0.75 * sx), 8)

    # --- arms long enough to walk on: bone pauldrons, sculk-crusted hide, great clawed hands
    for side, sx in (('left', 1), ('right', -1)):
        arm = torso.part(f'{side}_arm', pivot=(15.5 * sx, -23, -2), rot=(-0.42, 0, -0.1 * sx))
        arm.cube((-6, -6, -6), (12, 7, 12), **bone_box((12, 7, 12), 240 + sx, 0.6))
        for k, (spx, spz) in enumerate(((1.5, -3), (3, 2), (-1, 0))):
            arm.cube((spx * sx - 0.75, -9 + k, spz - 0.75), (1.5, 3 - k * 0.5, 1.5), color='tooth', pattern='mc', clusters=0.0, rim=False)
        arm.cube((-4.25, -1, -4.25), (8.5, 19, 8.5), **hide_box((8.5, 19, 8.5), 245 + sx))
        arm.cube((-1, 15, 3.5), (2, 2, 3.5), color='bone', pattern='mc', clusters=0.0, rim=False)
        fore = arm.part(f'{side}_forearm', pivot=(0, 17, 0), rot=(-0.3, 0, 0))
        fore.cube((-4.75, 0, -4.75), (9.5, 17, 9.5), **hide_box((9.5, 17, 9.5), 250 + sx, 0.6))
        fore.cube((-5.25 if sx > 0 else 3.75, 2, -3.5), (1.5, 12, 7), **bone_box((1.5, 12, 7), 255 + sx, 0.4))
        if sx < 0:
            # his baton, grown through the wrist into a long black spur
            spur = fore.part('baton', pivot=(-4.5, 15, 1), rot=(-0.35, 0, 0.12))
            spur.cube((-0.8, -26, -0.8), (1.6, 26, 1.6), color='baton', pattern='mc', clusters=0.0, rim=False,
                      faces={f: dict(color='baton', pattern='mc', clusters=0.0, hd=True, map=['l....'] * 78, keys={'l': 'baton_l'}) for f in ('north', 'east', 'west')})
            spur.cube((-1.1, -28, -1.1), (2.2, 2.2, 2.2), color='glow', pattern='mc', clusters=0.0, rim=False, glow=True)
        hand = fore.part(f'{side}_hand', pivot=(0, 17, 0))
        hand.cube((-5.5, 0, -5), (11, 5, 10), **hide_box((11, 5, 10), 260 + sx, 0.4))
        for k, fx in enumerate((-3.6, 0.0, 3.6)):
            f = hand.part(f'{side}_finger_{k}', pivot=(fx, 4.5, -3), rot=(-0.25, 0, (k - 1) * 0.08))
            f.cube((-1.3, 0, -1.3), (2.6, 7, 2.6), **hide_box((2.6, 7, 2.6), 270 + k + sx * 5, 0.3))
            f.cube((-1.0, 6.5, -1.7), (2.0, 4.5, 2.4), color='claw', pattern='mc', clusters=0.0, rim=False,
                   faces={'north': dict(color='claw', pattern='mc', clusters=0.0, hd=True, map=['dd....'] + ['d.....'] * 12, keys={'d': 'claw_d'})})
        th = hand.part(f'{side}_thumb', pivot=(-4.5 * sx, 2.5, -3), rot=(-0.5, 0, 0.5 * sx))
        th.cube((-1.2, 0, -1.2), (2.4, 6, 2.4), color='hide', pattern='mc', clusters=0.3)
        th.cube((-0.9, 5.5, -1.5), (1.8, 3.5, 2.1), color='claw', pattern='mc', clusters=0.0, rim=False)
    return m


# =========================================================================== the Conductor's Staff (3D item)
#
# Authored upright along the y axis at x = z = 8, then every element is turned -45 degrees about z
# round (8, 8, 8) so it lies on the same diagonal as a handheld sprite - the vanilla handheld
# transforms then hold it like a sword, staff head up and out. Unrotated decorations (the upper
# horn-ears, the floating notes) end up at 45 degrees to the shaft.

STAFF_TEX = 'item/conductors_staff_model'
# UV zones on the 32x32 texture, in 0-16 model UV units (2 px each)
Z = {
    'shaft': (0, 0, 2, 16), 'brass': (2, 0, 3, 4), 'brass_l': (2, 4, 3, 6), 'grip': (2, 6, 3, 12), 'bone': (3, 0, 4, 6),
    'face': (4, 0, 10, 7), 'face2': (4, 8, 10, 15), 'porc': (10, 0, 12, 7), 'horn': (12, 0, 13, 6), 'horn_tip': (13, 0, 14, 2),
    'gem': (14, 0, 16, 2), 'orb': (14, 2, 16, 4), 'note': (13, 2, 14, 5), 'gold': (10, 8, 16, 10), 'sculk': (10, 10, 14, 14),
    'pommel': (14, 10, 16, 12), 'void': (14, 12, 16, 14),
}


def _box(frm, to, zone, glow=0, faces_zone=None, rot=True):
    faces = {}
    for f in ('north', 'south', 'east', 'west', 'up', 'down'):
        z = (faces_zone or {}).get(f, zone)
        faces[f] = {'uv': list(Z[z]), 'texture': '#staff'}
    e = {'from': list(frm), 'to': list(to), 'faces': faces}
    if rot:
        e['rotation'] = {'origin': [8, 8, 8], 'axis': 'z', 'angle': -45}
    if glow:
        e['light_emission'] = glow
        e['shade'] = False
    return e


def staff_model():
    els = [
        # pommel spike and brass foot
        _box((7.4, -4.5, 7.4), (8.6, -2.5, 8.6), 'pommel'),
        _box((7.0, -2.5, 7.0), (9.0, -1.5, 9.0), 'brass'),
        # the shaft in three lengths, bound in gold, a bone grip
        _box((7.25, -1.5, 7.25), (8.75, 4.0, 8.75), 'shaft'),
        _box((7.05, 0.0, 7.05), (8.95, 3.5, 8.95), 'grip'),
        _box((7.0, 4.0, 7.0), (9.0, 5.0, 9.0), 'brass_l'),
        _box((7.3, 5.0, 7.3), (8.7, 12.0, 8.7), 'shaft'),
        _box((7.0, 12.0, 7.0), (9.0, 13.0, 9.0), 'brass_l'),
        _box((7.3, 13.0, 7.3), (8.7, 17.0, 8.7), 'shaft'),
        # a sculk vine twisting up the shaft
        _box((8.7, 6.0, 7.6), (9.3, 9.0, 8.4), 'sculk'), _box((6.7, 9.5, 7.6), (7.3, 12.0, 8.4), 'sculk'), _box((8.7, 13.5, 7.6), (9.3, 16.0, 8.4), 'sculk'),
        # gold collar and the mask head: a two-faced mask, grinning one side and weeping the other
        _box((6.5, 17.0, 6.5), (9.5, 18.0, 9.5), 'gold'),
        _box((5.5, 18.0, 6.75), (10.5, 24.0, 9.25), 'porc', faces_zone={'south': 'face', 'north': 'face2'}),
        _box((5.0, 21.5, 6.5), (11.0, 22.25, 9.5), 'porc'),   # brow
        _box((7.0, 17.25, 6.9), (9.0, 18.25, 9.1), 'porc'),   # chin
        # gems: a brow gem on each face, and the orb the crest holds up
        _box((7.5, 22.25, 6.3), (8.5, 23.25, 6.8), 'gem', glow=12), _box((7.5, 22.25, 9.2), (8.5, 23.25, 9.7), 'gem', glow=12),
        _box((6.8, 24.0, 6.8), (9.2, 24.75, 9.2), 'gold'),
        _box((6.75, 24.0, 7.6), (7.5, 26.5, 8.4), 'gold'), _box((8.5, 24.0, 7.6), (9.25, 26.5, 8.4), 'gold'),
        _box((7.1, 25.25, 7.1), (8.9, 27.05, 8.9), 'orb', glow=15),
        # the lower horn-ears, swept along the shaft's diagonal
        _box((10.5, 19.5, 7.5), (13.0, 20.5, 8.5), 'horn'), _box((3.0, 19.5, 7.5), (5.5, 20.5, 8.5), 'horn'),
    ]
    # the upper horn-ears: unrotated, so they leave the head at 45 degrees (one up, one out)
    # from the mask's top corners, which the turn puts at about (17.5, 21) and (21, 17.5)
    els += [
        _box((17.0, 20.4, 7.5), (18.0, 25.4, 8.5), 'horn', rot=False), _box((17.15, 25.4, 7.65), (17.85, 27.2, 8.35), 'horn_tip', glow=10, rot=False),
        _box((20.4, 17.0, 7.5), (25.4, 18.0, 8.5), 'horn', rot=False), _box((25.4, 17.15, 7.65), (27.2, 17.85, 8.35), 'horn_tip', glow=10, rot=False),
        # floating notes round the head
        _box((13.0, 22.0, 8.0), (14.5, 23.3, 8.6), 'note', glow=8, rot=False), _box((14.0, 23.0, 8.0), (14.5, 26.0, 8.6), 'note', glow=8, rot=False),
        _box((22.5, 12.0, 7.4), (23.8, 13.2, 8.0), 'note', glow=8, rot=False), _box((23.3, 12.8, 7.4), (23.8, 15.5, 8.0), 'note', glow=8, rot=False),
    ]
    return {
        'textures': {'staff': f'thesift:{STAFF_TEX}', 'particle': f'thesift:{STAFF_TEX}'},
        'gui_light': 'front',
        'elements': els,
        'display': {
            'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
            'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4.0, 0.5], 'scale': [0.85, 0.85, 0.85]},
            'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
            'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68, 0.68, 0.68]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.42, 0.42, 0.42]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.62, 0.62, 0.62]},
            'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [0.8, 0.8, 0.8]},
            'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.6, 0.6, 0.6]},
        },
    }


def assets(GA):
    """Writes the 3D in-hand model and the item definition (flat sprite in the GUI only)."""
    m = staff_model()
    GA.note_textures(m)
    GA.write(os.path.join(GA.A, 'models/item', 'conductors_staff_in_hand.json'), m)
    GA.write(os.path.join(GA.A, 'items', 'conductors_staff.json'), {'model': {
        'type': 'minecraft:select', 'property': 'minecraft:display_context',
        'cases': [{'when': ['gui'], 'model': {'type': 'minecraft:model', 'model': f'{GA.NS}:item/conductors_staff'}}],
        'fallback': {'type': 'minecraft:model', 'model': f'{GA.NS}:item/conductors_staff_in_hand'}}})


def staff_texture():
    """32x32 texture for the 3D staff, painted zone by zone (see Z)."""
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(5)

    def hexc(h):
        h = h.lstrip('#')
        return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

    def zone(name, fn):
        u0, v0, u1, v1 = (int(c * 2) for c in Z[name])
        for y in range(v0, v1):
            for x in range(u0, u1):
                c = fn(x - u0, y - v0, u1 - u0, v1 - v0)
                if c:
                    px[x, y] = hexc(c)

    zone('shaft', lambda x, y, w, h: '#1b1424' if (x + y // 2) % 4 else ('#3a2d4a' if x < 2 else '#0d0a12'))
    zone('brass', lambda x, y, w, h: '#e2c77e' if y == 0 else ('#7f6a35' if y == h - 1 else '#b89a52'))
    zone('brass_l', lambda x, y, w, h: '#f2dc96' if y == 0 else '#c9a95a')
    zone('grip', lambda x, y, w, h: '#d8d1bd' if y % 3 else '#8b846f')
    zone('bone', lambda x, y, w, h: '#e3ddcc' if (x + y) % 5 else '#b3ab96')
    zone('porc', lambda x, y, w, h: '#fbf6ea' if y == 0 else ('#b4ab99' if (x * 3 + y) % 11 == 0 else '#e6dfcf'))
    zone('horn', lambda x, y, w, h: '#43344c' if x == 0 else ('#150e19' if y % 3 == 0 else '#2a2030'))
    zone('horn_tip', lambda x, y, w, h: '#7ff9f0' if y < 2 else '#2ef2e2')
    zone('gem', lambda x, y, w, h: '#c8fdff' if (x, y) in ((1, 1), (2, 1)) else ('#2ef2e2' if 0 < x < w - 1 and 0 < y < h - 1 else '#15a89f'))
    zone('orb', lambda x, y, w, h: '#ffffff' if (x, y) == (1, 1) else ('#c8fdff' if 0 < x < w - 1 and 0 < y < h - 1 else '#2ef2e2'))
    zone('note', lambda x, y, w, h: '#7ff9f0' if (x + y) % 2 else '#2ef2e2')
    zone('gold', lambda x, y, w, h: '#f2d88a' if y == 0 else ('#7f6a35' if y == h - 1 else ('#d4b25a' if x % 3 else '#b89a52')))
    zone('sculk', lambda x, y, w, h: '#2ef2e2' if rnd.random() < 0.18 else ('#074857' if (x + y) % 2 else '#034150'))
    zone('pommel', lambda x, y, w, h: '#3a2d4a' if y < 2 else '#d8d1bd')
    zone('void', lambda x, y, w, h: '#04080c')

    def face(rows):
        def fn(x, y, w, h):
            ch = rows[y][x] if y < len(rows) and x < len(rows[y]) else '.'
            return {'.': '#e6dfcf', 'l': '#fbf6ea', 'd': '#b4ab99', 'k': '#5e584d', 'v': '#04080c', 'g': '#2ef2e2', 'G': '#c8fdff', 'c': '#2ef2e2'}.get(ch)
        return fn
    # 12x14 texels: the grinning face (south) and the weeping one (north)
    grin = ['llllllllllll', 'l..........l', '..k......k..', '.kk......kk.', 'gGgk....kgGg', '.vv......vv.', 'd..........d',
            '.....dd.....', 'd..........d', 'vk.......kv', '.vvvvvvvvvv.', '..vkvkvkvv..', '...vvvvvv...', '.....dd.....']
    weep = ['llllllllllll', 'l..........l', '..kk....kk..', '...kk..kk...', '.vgGv..vGgv.', '..vv....vv..', '..g......g..',
            '..g..dd..g..', '..k......k..', '...........', '...vvvvvv...', '..vk....kv..', '.vk......kv.', '.....dd.....']
    zone('face', face(grin))
    zone('face2', face(weep))
    return img


def textures(out):
    out(STAFF_TEX, staff_texture())
