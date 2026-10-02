"""The Thumper Titan (agent B2 "Thumper & cutscenes"): the Thumper rebuilt as a giant sculk turtle.

A heavy, layered shell of slate plates - a scalloped rim of marginal plates, two tiers of scutes
and a crown - every plate cracked, the cracks near its vents glowing; barnacles crusted along the
rim and moss hanging from it in ragged curtains. Three glowing sculk vents (its only weak points)
sit in the crown and both flanks, each a pit of living sculk ringed in bone with a hinged lid.
Chunky scaled limbs in three segments (thigh, shin, foot) with bone elbow plates and hooked claws;
a heavily armoured head - brow plate, cheek guards, a hooked beak, a hinged jaw full of teeth, the
Warden's tendrils - on a thick scaled neck; a two-segment tail ending in a bone club.

Painted at res 3 (three texels per model unit) with texel-level maps. Part names are what
client/model/boss/ThumperModel.java animates.
"""
import math
import random

from modelkit import Model
from mobs import SCULK

R = 3  # texels per model unit

PAL = {
    'plate': '#1c2b34', 'plate_l': '#2d4352', 'plate_d': '#0e171d',
    'scale': '#17232c', 'scale_l': '#263843', 'scale_d': '#0b1217',
    'moss': '#2c5a3a', 'moss_l': '#4c8454', 'moss_d': '#183624',
    'barn': '#bab49a', 'barn_l': '#dcd7bf', 'barn_d': '#7a7563',
    'claw': '#d6d0b6', 'claw_d': '#8c8772', 'glow_c': '#c2fcff',
}
# plate faces: seams, lit edges, growth rings, sculk, cracks, barnacles, moss
PK = {'d': 'plate_d', 'l': 'plate_l', 'r': 'plate_d', 'v': 'void', 'g': 'glow', 'G': 'glow_c', 's': 'sculk', 'S': 'sculk_l',
      'b': 'barn', 'B': 'barn_l', 'o': 'barn_d', 'M': 'moss', 'n': 'moss_l', 'N': 'moss_d'}
SK = {'d': 'scale_d', 'l': 'scale_l', 's': 'sculk', 'S': 'sculk_l', 'g': 'glow', 'b': 'bone', 'B': 'bone_l', 'k': 'bone_d', 'v': 'void',
      'c': 'claw', 'C': 'claw_d', 'w': 'tooth', 'M': 'moss', 'n': 'moss_l'}
BK = {'b': 'bone', 'l': 'bone_l', 'd': 'bone_d', 'k': 'bone_k', 'v': 'void', 'g': 'glow', 'G': 'glow_c', 'w': 'tooth', 's': 'sculk', 'S': 'sculk_l'}
GLOW = 'gG'


def gen(w, h, fn):
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def _blobs(rnd, w, h, n, r0, r1):
    return [(rnd.uniform(0, w), rnd.uniform(0, h), rnd.uniform(r0, r1)) for _ in range(max(0, n))]


def _inside(blobs, x, y):
    best = 0.0
    for (bx, by, r) in blobs:
        d = math.hypot(x - bx, y - by)
        best = max(best, 1.0 - d / r)
    return best


def plates(w, h, seed, cell=13, crack=0.5, sculk=0.35, barnacles=0, moss_top=0, glow=0.5):
    """Shell scutes as an irregular mosaic (jittered Voronoi): dark seams, a lit upper-left rim on
    every plate and a growth ring inside it; branching cracks (glowing ones glow, others are
    black), sculk crusting the seams, barnacle clusters and moss along the top edge."""
    rnd = random.Random(seed)
    gx, gy = max(1, w // cell + 2), max(1, h // cell + 2)
    pts = {}
    for i in range(-1, gx):
        for j in range(-1, gy):
            pts[(i, j)] = (i * cell + rnd.uniform(0.2, 0.8) * cell, j * cell + rnd.uniform(0.2, 0.8) * cell)
    grid = [['.'] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            ci, cj = int(x // cell), int(y // cell)
            near = []
            for di in (-1, 0, 1):
                for dj in (-1, 0, 1):
                    p = pts.get((ci + di, cj + dj))
                    if p:
                        near.append((math.hypot(x - p[0], y - p[1]), p))
            near.sort(key=lambda a: a[0])
            (d1, p1), (d2, _) = near[0], near[1]
            edge = d2 - d1
            if edge < 1.3:
                grid[y][x] = 'd'
            elif edge < 2.6 and (y < p1[1] - 1 or x < p1[0] - 1):
                grid[y][x] = 'l'
            elif abs(d1 - cell * 0.28) < 0.55:
                grid[y][x] = 'r'
    # sculk creeping along the seams
    sb = _blobs(rnd, w, h, int(w * h * sculk / 60), 1.5, 4.0)
    for y in range(h):
        for x in range(w):
            k = _inside(sb, x, y)
            if k > 0.0 and (grid[y][x] in 'dr' or k > 0.5):
                grid[y][x] = 'g' if (k > 0.65 and rnd.random() < 0.12) else ('S' if k > 0.45 else 's')
    # cracks: random walks from seams, branching
    for _ in range(max(1, int(w * h / 500))):
        x, y = rnd.uniform(0, w), rnd.uniform(0, h)
        lit = rnd.random() < crack
        ang = rnd.uniform(0, math.tau)
        for step in range(rnd.randint(6, 16)):
            ix, iy = int(x), int(y)
            if 0 <= ix < w and 0 <= iy < h:
                grid[iy][ix] = ('G' if step % 5 == 2 else 'g') if (lit and rnd.random() < glow + 0.3) else 'v'
            ang += rnd.uniform(-0.7, 0.7)
            x += math.cos(ang)
            y += math.sin(ang)
            if rnd.random() < 0.12:
                bx, by, ba = x, y, ang + rnd.choice((-1.1, 1.1))
                for _ in range(rnd.randint(2, 5)):
                    bx += math.cos(ba)
                    by += math.sin(ba)
                    if 0 <= int(bx) < w and 0 <= int(by) < h:
                        grid[int(by)][int(bx)] = 'g' if lit else 'v'
    # barnacles: little cones - a pale ring round a dark mouth
    for _ in range(barnacles):
        cx, cy = rnd.randint(2, max(2, w - 3)), rnd.randint(2, max(2, h - 3))
        for _ in range(rnd.randint(2, 5)):
            bx, by = cx + rnd.randint(-3, 3), cy + rnd.randint(-2, 2)
            for (dx, dy, ch) in ((0, 0, 'o'), (-1, 0, 'b'), (1, 0, 'b'), (0, -1, 'B'), (0, 1, 'b'), (-1, -1, 'B'), (1, 1, 'o')):
                if 0 <= bx + dx < w and 0 <= by + dy < h:
                    grid[by + dy][bx + dx] = ch
    # moss along the upper edge, thinning downwards
    for x in range(w):
        depth = int(moss_top * (0.5 + 0.5 * math.sin(x * 0.7 + seed) * math.sin(x * 0.23 + seed * 3))) if moss_top else 0
        for y in range(min(h, max(0, depth))):
            grid[y][x] = 'n' if (y == 0 or (x + y) % 4 == 0) else ('N' if y == depth - 1 else 'M')
    return [''.join(r) for r in grid]


def marginals(w, h, seed, barnacles=4, moss=3):
    """The rim seen from the side: a row of marginal plates, serrated at the bottom edge, lit on
    top, cracked, barnacled, moss curling over the top."""
    rnd = random.Random(seed)
    step = 5 * R

    def fn(x, y):
        k = (x + 2) % step
        if y >= h - 1 - (abs(k - step // 2) < 3):
            return '_' if (y == h - 1 and abs(k - step // 2) >= 3) else 'd'
        if k in (0, 1):
            return 'd'
        if k == 2 or y in (0, 1):
            return 'l'
        if y == h - 3 or y == h - 4:
            return 'r'
        return '.'
    rows = [list(r) for r in gen(w, h, fn)]
    for _ in range(int(w / 8)):
        x, y = rnd.randint(0, w - 1), rnd.randint(2, h - 4)
        lit = rnd.random() < 0.4
        for _ in range(rnd.randint(3, 7)):
            if 0 <= x < w and 0 <= y < h:
                rows[y][x] = 'g' if lit else 'v'
            x += rnd.choice((-1, 0, 1))
            y += 1
    for _ in range(barnacles):
        bx, by = rnd.randint(1, w - 2), rnd.randint(3, h - 4)
        rows[by][bx] = 'o'
        rows[by][bx - 1] = rows[by][bx + 1] = 'b'
        rows[by - 1][bx] = 'B'
    for x in range(w):
        d = int(moss * max(0.0, math.sin(x * 0.31 + seed) + 0.4 * math.sin(x * 1.3)))
        for y in range(min(h, d)):
            rows[y][x] = 'n' if y == 0 else 'M'
    return [''.join(r) for r in rows]


def scales(w, h, seed, sculk=0.2, plate_rows=()):
    """Thick reptile scales: staggered rounded scales, dark lower rims, lit crowns, a scatter of
    sculk; `plate_rows`: texel rows painted as a bone band (elbow and knee plates)."""
    rnd = random.Random(seed)
    sb = _blobs(rnd, w, h, int(w * h * sculk / 50), 1.2, 3.0)

    def fn(x, y):
        if y in plate_rows:
            return 'B' if y == plate_rows[0] else ('k' if y == plate_rows[-1] else 'b')
        row = y // 4
        cx = (x + (3 if row % 2 else 0)) % 6
        cy = y % 4
        k = _inside(sb, x, y)
        if k > 0.35:
            return 'g' if k > 0.75 and rnd.random() < 0.2 else ('S' if k > 0.55 else 's')
        # rounded scales: a dark scalloped lower rim, a lit crown
        if (cy == 3 and 1 <= cx <= 4) or (cy == 2 and cx in (0, 5)):
            return 'd'
        if (cy == 0 and cx in (2, 3)) or (cy == 1 and cx in (1, 4)):
            return 'l'
        return '.'
    return gen(w, h, fn)


def moss_curtain(w, h, seed):
    """Ragged moss hanging off the rim: columns of uneven length, lit at the top."""
    rnd = random.Random(seed)
    lens = [int(h * (0.35 + 0.65 * rnd.random())) for _ in range(w)]

    def fn(x, y):
        if y >= lens[x]:
            return '_'
        if y == lens[x] - 1:
            return 'N'
        return 'n' if (y < 2 or (x * 3 + y) % 7 == 0) else 'M'
    return gen(w, h, fn)


def face_rows(expr):
    """The front of the armoured head: 30 x 21 texels. A bone brow ridge over two deep sockets with
    glowing pupils, scar cracks across the plate, nostril pits low in the middle."""
    w, h = 30, 21

    def eye(x, y, cx):
        dx, dy = (x - cx) / 3.2, (y - 9.5) / 2.3
        return dx * dx + dy * dy

    def fn(x, y):
        cx = 5.0 if x < 15 else 24.0
        e = eye(x, y, cx)
        sgn = -1 if x < 15 else 1
        if y <= 1:
            return 'l' if y == 0 else 'b'
        if y in (2, 3) and abs(x - 14.5) > 2:
            return 'd' if y == 3 else 'b'
        if e < 1.0:
            if expr == 'blink':
                return 'd' if abs(y - 9.5) < 0.8 else 'b'
            if expr == 'angry':
                # the brow plate drops over the inner corner
                if (y - 7.0) < -sgn * (x - cx) * 0.6:
                    return 'b'
                return 'G' if e < 0.25 else ('g' if e < 0.55 else 'v')
            if expr in ('hurt', 'dead'):
                xx = abs(abs(x - cx) - abs(y - 9.5)) < 0.8
                return ('g' if expr == 'hurt' else 'k') if xx else 'v'
            return 'G' if e < 0.12 else ('g' if e < 0.4 else 'v')
        if e < 1.6:
            return 'd'
        if 14 <= y <= 16 and (x in (12, 13, 16, 17)):
            return 'v'
        if y == 13 and abs(x - 14.5) < 5:
            return 'd'
        if (x == 3 and 12 <= y <= 18) or (x == 4 and 17 <= y <= 19) or (x + y == 36 and x > 21):
            return 'k'
        return '.'
    return gen(w, h, fn)


def tendril(part, name, pivot, rot, length=7):
    t = part.part(name, pivot=pivot, rot=rot)
    rows = gen(2 * R, length * R, lambda x, y: ('g' if 1 <= x <= 4 else ('G' if x == 0 and y % 4 == 0 else '_'))
               if y < length * R - 4 else ('g' if x == (y % 3) + 1 else '_'))
    t.cube((-1, -length, 0), (2, length, 0), color='glow_d', pattern='mc', clusters=0.0, rim=False, faces={
        f: dict(color='glow_d', pattern='mc', clusters=0.0, rim=False, hd=True, map=rows, keys={'g': 'glow', 'G': 'glow_c'}, glow_keys='gG')
        for f in ('north', 'south')})
    return t


def vent(body, name, pivot, rot):
    """A sculk vent: a pit of living sculk with a glowing heart, a ring of bone round it, a bone
    lid hinged at its front edge, and the pit's lit twin (shown while the vent breathes or gapes)."""
    v = body.part(name, pivot=pivot, rot=rot)
    n = 6 * R
    c = (n - 1) / 2
    pit = gen(n, n, lambda x, y: 'G' if abs(x - c) + abs(y - c) < 2.6 else 'g' if abs(x - c) + abs(y - c) < 4.4 else
              ('g' if (x * 7 + y * 3) % 11 == 0 else ('v' if (x * 5 + y) % 9 == 0 else ('S' if (x + y) % 3 == 0 else 's'))))
    v.cube((-3, -0.75, -3), (6, 1, 6), color='sculk', pattern='mc', clusters=0.2, rim=False, faces={
        'up': dict(color='sculk', pattern='mc', clusters=0.0, hd=True, map=pit, keys=BK, glow_keys=GLOW)})
    bone = dict(color='bone', pattern='mc', clusters=0.0, rim=False)
    ring = gen(7 * R, R, lambda x, y: 'l' if y == 0 else ('d' if y == R - 1 else ('k' if x % 5 == 0 else 'b')))
    for (x, z, w, d) in ((-3.5, -3.5, 7, 1), (-3.5, 2.5, 7, 1), (-3.5, -2.5, 1, 5), (2.5, -2.5, 1, 5)):
        v.cube((x, -1.75, z), (w, 1.75, d), **bone, faces={'up': dict(**bone, hd=True, map=ring if w > d else [r[:R] for r in ring], keys=BK)})
    # little teeth of bone round the rim
    for (x, z) in ((-3.5, -3.5), (2.5, -3.5), (-3.5, 2.5), (2.5, 2.5)):
        v.cube((x + 0.25, -2.5, z + 0.25), (0.5, 0.75, 0.5), color='tooth', pattern='mc', clusters=0.0, rim=False)
    lit = v.part(f'{name}_lit', pivot=(0, 0, 0))
    lit.cube((-3, -0.75, -3), (6, 1, 6), inflate=0.2, color='glow', pattern='mc', clusters=0.0, rim=False, glow=True, faces={
        'up': dict(color='glow', pattern='mc', clusters=0.0, rim=False, hd=True,
                   map=[r.replace('s', 'g').replace('S', 'G').replace('v', 'g') for r in pit], keys=BK, glow_keys=GLOW)})
    lid = v.part(f'{name}_lid', pivot=(0, -1.75, -3.5))
    m = 7 * R
    plate = gen(m, m, lambda x, y: 'd' if min(x, y, m - 1 - x, m - 1 - y) == 0 else ('l' if min(x, y) == 1 else (
        'g' if (abs(x - y) < 1 and 4 < x < m - 5 and x % 3 != 0) else ('k' if abs(x + y - m + 1) < 1 else 'b'))))
    lk = dict(BK, b='plate_l', l='bone_d', d='plate_d', k='plate_d')
    lid.cube((-3.5, -0.75, 0), (7, 0.75, 7), color='plate_l', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='plate_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=plate, keys=lk, glow_keys=GLOW),
                                                         'down': dict(color='sculk_d', pattern='mc', clusters=0.0, rim=False)})
    return v


def _sides(w, h, d, fn):
    """Face specs for a box from fn(face, texel_w, texel_h)."""
    W, H, D = (int(math.ceil(s)) * R for s in (w, h, d))
    dims = {'up': (W, D), 'down': (W, D), 'north': (W, H), 'south': (W, H), 'east': (D, H), 'west': (D, H)}
    out = {}
    for f, (fw, fh) in dims.items():
        spec = fn(f, fw, fh)
        if spec:
            out[f] = spec
    return out


def pf(base, rows, keys=PK, **kw):
    return dict(color=base, pattern='mc', clusters=0.0, hd=True, map=rows, keys=keys, glow_keys=GLOW, **kw)


def thumper_titan() -> Model:
    pal = dict(SCULK)
    from bosses import WARDEN
    pal.update(WARDEN)
    pal.update(PAL)
    m = Model('thumper', (256, 128), pal, {'thumper': {}}, res=R, expressions=['blink', 'angry', 'hurt', 'dead'])
    body = m.part('body', pivot=(0, 13, 0))
    # the plastron: a belly of pale bone plates with sculk in the seams
    body.cube((-10.5, 2, -12.5), (21, 3, 25), color='hide', pattern='mc', clusters=0.2, faces=_sides(21, 3, 25, lambda f, w, h: (
        pf('bone_d', plates(w, h, 3, cell=15, crack=0.2, sculk=0.25), keys=dict(PK, l='bone', d='bone_k', r='bone_k')) if f == 'down' else
        pf('scale', scales(w, h, 4 + len(f))) if f in ('north', 'south', 'east', 'west') else None)))
    shell = body.part('shell', pivot=(0, 0, 0))
    # the rim of marginal plates, then two tiers of scutes and the crown
    shell.cube((-13, -2, -15), (26, 4, 30), color='plate', pattern='mc', clusters=0.2, faces=_sides(26, 4, 30, lambda f, w, h: (
        pf('plate', plates(w, h, 10, cell=16, crack=0.35, barnacles=7, sculk=0.3)) if f == 'up' else
        pf('plate_d', plates(w, h, 11, cell=18, crack=0.1, sculk=0.1)) if f == 'down' else
        pf('plate', marginals(w, h, 12 + len(f), barnacles=3 + len(f) % 3, moss=4)))))
    shell.cube((-11, -6, -12.5), (22, 4, 25), color='plate', pattern='mc', clusters=0.2, faces=_sides(22, 4, 25, lambda f, w, h: (
        pf('plate', plates(w, h, 20, cell=14, crack=0.55, barnacles=4, sculk=0.35)) if f == 'up' else
        pf('plate', plates(w, h, 21 + len(f), cell=11, crack=0.6, sculk=0.35, moss_top=2)) if f != 'down' else None)))
    shell.cube((-8.5, -9, -9.5), (17, 3, 19), color='plate', pattern='mc', clusters=0.2, faces=_sides(17, 3, 19, lambda f, w, h: (
        pf('plate', plates(w, h, 30, cell=13, crack=0.7, barnacles=2, sculk=0.4)) if f == 'up' else
        pf('plate', plates(w, h, 31 + len(f), cell=10, crack=0.7, sculk=0.4)) if f != 'down' else None)))
    shell.cube((-5.5, -11, -6.5), (11, 2, 13), color='plate_l', pattern='mc', clusters=0.2, faces=_sides(11, 2, 13, lambda f, w, h: (
        pf('plate_l', plates(w, h, 40, cell=11, crack=0.9, sculk=0.5, glow=0.8), keys=dict(PK, l='bone_d')) if f == 'up' else
        pf('plate', plates(w, h, 41 + len(f), cell=8, crack=0.8)) if f != 'down' else None)))
    # overlapping raised plates over the flanks and front - the layered look
    for i, (x, z, sx) in enumerate(((9.5, -7, 1), (9.5, 6, 1), (-9.5, -7, -1), (-9.5, 6, -1))):
        p = shell.part(f'flank_plate_{i}', pivot=(x, -6, z), rot=(0, 0, 0.45 * sx))
        p.cube((-1 if sx > 0 else -2, 0, -4.5), (3, 1, 9), color='plate_l', pattern='mc', clusters=0.0, faces=_sides(3, 1, 9, lambda f, w, h: (
            pf('plate_l', plates(w, h, 50 + i, cell=8, crack=0.5), keys=dict(PK, l='bone_d')) if f in ('up', 'east', 'west') else None)))
    for i, x in enumerate((-6, 0, 6)):
        p = shell.part(f'nuchal_plate_{i}', pivot=(x, -5.5, -12.5), rot=(-0.4, 0, 0))
        p.cube((-2.5, 0, -2), (5, 1, 3), color='plate_l', pattern='mc', clusters=0.0, faces=_sides(5, 1, 3, lambda f, w, h: (
            pf('plate_l', plates(w, h, 60 + i, cell=7, crack=0.3), keys=dict(PK, l='bone_d')) if f == 'up' else None)))
    # barnacle clusters standing proud of the rim and moss curtains hanging off it
    rnd = random.Random(77)
    for i, (x, z) in enumerate(((-11, -12), (12, -9), (-12, 7), (11, 11), (-4, 13.5), (6, -13.5), (-12.5, -2), (12.5, 3))):
        b = shell.part(f'barnacles_{i}', pivot=(x, -2, z))
        for k in range(3):
            dx, dz = rnd.uniform(-1.2, 1.2), rnd.uniform(-1.2, 1.2)
            hgt = rnd.choice((0.75, 1.0, 1.5))
            b.cube((dx - 0.5, -hgt, dz - 0.5), (1, hgt, 1), color='barn', pattern='mc', clusters=0.0, rim=False,
                   faces={'up': pf('barn', gen(R, R, lambda xx, yy: 'o' if (xx, yy) == (1, 1) else 'B'))})
    for i, (x, z, ry) in enumerate(((0, -15, 0), (-8, -14.9, 0), (8, -14.9, 0), (13, -6, 1.5708), (13, 6, 1.5708), (-13, -6, -1.5708),
                                    (-13, 6, -1.5708), (0, 15, 3.1416), (-9, 15, 3.1416), (9, 15, 3.1416))):
        mc = shell.part(f'moss_{i}', pivot=(x, 1.5, z), rot=(0.08, ry, 0))
        wdt = 6 if i in (0, 7) else 4
        mc.cube((-wdt / 2, 0, 0), (wdt, 3, 0), color='moss', pattern='mc', clusters=0.0, rim=False, faces={
            f: pf('moss', moss_curtain(wdt * R, 3 * R, 90 + i), keys=PK) for f in ('north', 'south')})
    # sculk growths on the crown, like shriekers, each with a glowing mouth
    for i, (x, z) in enumerate(((-3, -6.5), (3.5, 6), (-6, 4))):
        g = shell.part(f'growth_{i}', pivot=(x, -9 if abs(z) < 7 else -9, z))
        g.cube((-1.25, -2.5, -1.25), (2.5, 2.5, 2.5), color='sculk', pattern='mc', clusters=0.3, faces={
            'up': pf('void', gen(3 * R - 1, 3 * R - 1, lambda xx, yy: 'G' if abs(xx - 3.5) + abs(yy - 3.5) < 1.6 else ('g' if abs(xx - 3.5) + abs(yy - 3.5) < 3 else 's')),
                     keys=PK)})
        g.cube((-2, -3.25, -0.25), (4, 0.75, 0.5), color='bone', pattern='mc', clusters=0.0, rim=False)
    # the three vents: the crown and both flanks (Thumper.vents() puts the weak points here)
    for name, pivot, rot in (('vent_top', (0, -11, 0), (0, 0, 0)), ('vent_left', (11.25, -4, 0), (0, 0, 1.25)),
                             ('vent_right', (-11.25, -4, 0), (0, 0, -1.25))):
        vent(shell, name, pivot, rot)

    # neck and armoured head
    neck = body.part('neck', pivot=(0, -0.5, -13))
    neck.cube((-4, -3.5, -7), (8, 7, 8), color='scale', pattern='mc', clusters=0.3, faces=_sides(8, 7, 8, lambda f, w, h: (
        pf('scale', scales(w, h, 100 + len(f), sculk=0.3), keys=SK) if f != 'down' else
        pf('scale_l', scales(w, h, 105, sculk=0.0, plate_rows=tuple(range(0, h, 4))), keys=SK))))
    # loose skin folds where the neck leaves the shell
    neck.cube((-5, -4, -1.5), (10, 2, 2), color='scale_d', pattern='mc', clusters=0.2, rim=False)
    head = neck.part('head', pivot=(0, -0.5, -7))
    head.cube((-5, -5, -9), (10, 7, 9), color='plate', pattern='mc', clusters=0.2, faces=_sides(10, 7, 9, lambda f, w, h: (
        pf('bone_d', face_rows('neutral'), keys=BK, expr={k: face_rows(k) for k in ('blink', 'angry', 'hurt', 'dead')}) if f == 'north' else
        pf('plate', plates(w, h, 110 + len(f), cell=9, crack=0.5, sculk=0.3)) if f in ('up', 'east', 'west') else
        pf('scale', scales(w, h, 115), keys=SK))))
    # the helmet: a heavy brow plate, a crest, cheek guards and knobs of bone
    head.cube((-5.5, -6.5, -9.5), (11, 2, 7), color='bone_d', pattern='mc', clusters=0.0, faces=_sides(11, 2, 7, lambda f, w, h: (
        pf('bone_d', plates(w, h, 120 + len(f), cell=10, crack=0.4, sculk=0.2), keys=dict(BK, r='bone_d', o='bone_k', B='bone_l', M='sculk', n='sculk_l', N='sculk'))
        if f in ('up', 'north') else None)))
    head.cube((-1, -8, -8), (2, 1.5, 6), color='bone_l', pattern='mc', clusters=0.0, rim=False)
    for side, sx in (('left', 1), ('right', -1)):
        cheek = head.part(f'{side}_cheek', pivot=(5 * sx, -1, -5), rot=(0, 0.15 * sx, -0.12 * sx))
        cheek.cube((-0.5 if sx > 0 else -1, -2.5, -4), (1.5, 5, 6), color='bone_d', pattern='mc', clusters=0.0, faces=_sides(1.5, 5, 6, lambda f, w, h: (
            pf('bone_d', gen(w, h, lambda x, y: 'b' if y == 0 else ('k' if y == h - 1 or x == 0 else ('k' if (x + y) % 7 == 0 else '.'))), keys=BK)
            if f in ('east', 'west') else None)))
        brow = head.part(f'{side}_brow', pivot=(2.8 * sx, -5, -9.5))
        brow.cube((-2.25, -1, -1.25), (4.5, 1.75, 1.75), color='bone_d', pattern='mc', clusters=0.0, rim=False,
                  faces={'up': dict(color='bone_l', pattern='mc', clusters=0.0)})
        knob = head.part(f'{side}_horn', pivot=(4 * sx, -6.5, -5), rot=(-0.5, 0, 0.5 * sx))
        knob.cube((-0.75, -2.5, -0.75), (1.5, 2.5, 1.5), color='bone', pattern='mc', clusters=0.0, rim=False,
                  faces={'up': dict(color='bone_l', pattern='mc', clusters=0.0)})
        tendril(head, f'{side}_tendril', (4 * sx, -6, -2), (0.25, 0, 0.7 * sx), 7)
    # the hooked beak
    head.cube((-2, -1.5, -11.5), (4, 3.5, 2.5), color='bone_l', pattern='mc', clusters=0.0, rim=False, faces=_sides(4, 3.5, 2.5, lambda f, w, h: (
        pf('bone_l', gen(w, h, lambda x, y: 'd' if x in (0, w - 1) else ('k' if y == h - 1 else ('l' if y == 0 else '.'))), keys=BK) if f == 'north' else None)))
    head.cube((-1.25, 2, -11.5), (2.5, 1.25, 1.25), color='bone_d', pattern='mc', clusters=0.0, rim=False)
    jaw = head.part('jaw', pivot=(0, 2, -1))
    jaw.cube((-4.5, 0, -8), (9, 2.5, 8), color='scale', pattern='mc', clusters=0.3, faces=_sides(9, 2.5, 8, lambda f, w, h: (
        pf('void', gen(w, h, lambda x, y: 'w' if (y <= 1 and x % 4 != 3) or (x <= 1 or x >= w - 2) and y % 4 != 3 else ('s' if (x + y) % 5 == 0 else '.')), keys=BK)
        if f == 'up' else pf('bone_d', gen(w, h, lambda x, y: 'w' if (y < 3 and x % 4 < 2) else ('l' if y == 3 else '.')), keys=BK) if f == 'north' else
        pf('scale_l', scales(w, h, 130, sculk=0.0, plate_rows=(0, 1, 2)), keys=SK) if f == 'down' else pf('scale', scales(w, h, 131 + len(f)), keys=SK))))
    jaw.cube((-1.25, -1, -9.5), (2.5, 3, 1.5), color='bone', pattern='mc', clusters=0.0, rim=False)

    # four chunky legs: scaled thigh with a bone elbow plate, a shin, a broad foot with hooked claws
    for name, sx, sz in (('front_left', 1, -1), ('front_right', -1, -1), ('hind_left', 1, 1), ('hind_right', -1, 1)):
        front = sz < 0
        leg = body.part(f'{name}_leg', pivot=(10 * sx, 1, 9.5 * sz))
        tw = 7 if front else 7.5
        leg.cube((-tw / 2, -2, -tw / 2), (tw, 6, tw), color='scale', pattern='mc', clusters=0.3, faces=_sides(tw, 6, tw, lambda f, w, h, s=sx + 3 * sz: (
            pf('scale', scales(w, h, 140 + s * 7 + len(f), sculk=0.25), keys=SK) if f != 'up' else None)))
        leg.cube((-tw / 2 - 0.5 if sx > 0 else tw / 2 - 1, -1.5, -3), (1.5, 4.5, 6), color='bone', pattern='mc', clusters=0.0, rim=False,
                 faces=_sides(1.5, 4.5, 6, lambda f, w, h: pf('bone', gen(w, h, lambda x, y: 'l' if y == 0 else ('d' if y == h - 1 else ('k' if x % 6 == 0 else '.'))), keys=BK)
                              if f in ('east', 'west') else None))
        shin = leg.part(f'{name}_shin', pivot=(0, 4, 0))
        shin.cube((-3, 0, -3), (6, 5, 6), color='scale', pattern='mc', clusters=0.3, faces=_sides(6, 5, 6, lambda f, w, h, s=sx + 3 * sz: (
            pf('scale', scales(w, h, 160 + s * 5 + len(f), sculk=0.15, plate_rows=(0, 1) if f == 'north' else ()), keys=SK) if f not in ('up', 'down') else None)))
        foot = shin.part(f'{name}_foot', pivot=(0, 5, 0))
        foot.cube((-3.5, 0, -4.5), (7, 1, 8), color='scale_d', pattern='mc', clusters=0.2, rim=False, faces=_sides(7, 1, 8, lambda f, w, h: (
            pf('scale_d', gen(w, h, lambda x, y: 'l' if (x + y) % 5 == 0 else '.'), keys=SK) if f == 'up' else None)))
        for k, cx in enumerate((-2.25, 0, 2.25)):
            claw = foot.part(f'{name}_claw_{k}', pivot=(cx, 0.5, -4.5), rot=(0.25, (cx / 2.25) * -0.2, 0))
            claw.cube((-0.6, -0.5, -2), (1.2, 1, 2), color='claw', pattern='mc', clusters=0.0, rim=False, faces=_sides(1.2, 1, 2, lambda f, w, h: (
                pf('claw', gen(w, h, lambda x, y: 'C' if y == h - 1 or x == 0 else '.'), keys=SK) if f in ('east', 'west', 'up') else None)))
            claw.cube((-0.35, 0, -3), (0.7, 0.6, 1), color='claw_d', pattern='mc', clusters=0.0, rim=False)

    # the tail: two segments, a ridge of bone and a spiked club
    tail = body.part('tail', pivot=(0, 0.5, 14.5))
    tail.cube((-2.25, -1.75, 0), (4.5, 3.5, 5), color='scale', pattern='mc', clusters=0.3, faces=_sides(4.5, 3.5, 5, lambda f, w, h: (
        pf('scale', scales(w, h, 180 + len(f)), keys=SK) if f != 'north' else None)))
    tail.cube((-0.5, -2.75, 0.5), (1, 1, 4), color='bone', pattern='mc', clusters=0.0, rim=False)
    tip = tail.part('tail_tip', pivot=(0, 0, 5))
    tip.cube((-1.5, -1.25, 0), (3, 2.5, 4), color='scale', pattern='mc', clusters=0.3, faces=_sides(3, 2.5, 4, lambda f, w, h: (
        pf('scale', scales(w, h, 190 + len(f)), keys=SK) if f != 'north' else None)))
    tip.cube((-1.75, -1.5, 3.5), (3.5, 3, 2.5), color='bone', pattern='mc', clusters=0.0, faces=_sides(3.5, 3, 2.5, lambda f, w, h: (
        pf('bone', plates(w, h, 195 + len(f), cell=5, crack=0.6, sculk=0.2), keys=dict(BK, r='bone_d', o='bone_k', B='bone_l', M='sculk', n='sculk_l', N='sculk'))
        if f != 'north' else None)))
    for k, (x, y, rz) in enumerate(((0, -1.5, 0), (1.75, 0, 1.5708), (-1.75, 0, -1.5708))):
        sp = tip.part(f'tail_spike_{k}', pivot=(x, y, 4.75), rot=(0, 0, rz))
        sp.cube((-0.4, -1.5, -0.4), (0.8, 1.5, 0.8), color='tooth', pattern='mc', clusters=0.0, rim=False)
    return m
