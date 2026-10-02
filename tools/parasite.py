"""The Sculk Parasite: a small, nasty Warden-kin centipede of the Conductor's orchestra. Same DSL
as mobs.py (see modelkit.py); registered into mobs.ALL at the bottom of mobs.py. Uses the Warden
palette and sculk helpers of tools/bosses.py (read-only here)."""
import math

from bosses import EXPR, WARDEN, WGLOW, WK, gen, hide_face, tendril
from mobs import SCULK, hd_rows
from modelkit import Model

# 12 x 8 texels: a bone face-plate, two big glowing eyes in deep sockets, two little ones between
# them, and the roots of the mandibles
_FACE = {0: 'dlllllllllld', 1: 'bbbbvbbvbbbb', 2: 'bvvbgbbgbvvb', 3: 'vGGvbbbbvGGv', 4: 'vGGvbddbvGGv', 5: 'bvvbbddbbvvb', 6: '.dbbwbbwbbd.',
         7: '..d.w..w.d..'}
PARASITE_FACE = {
    'neutral': _FACE,
    'blink': {**_FACE, 2: 'bvvbdbbdbvvb', 3: 'vddvbbbbvddv', 4: 'vvvvbddbvvvv'},
    'angry': {**_FACE, 0: 'llllllllllll', 1: 'blllbbbblllb', 2: 'bbvvgbbgvvbb', 3: 'vvGGbbbbGGvv', 6: 'wdbwwbbwwbdw', 7: 'w.d.w..w.d.w'},
    'hurt': {**_FACE, 2: 'bvvbvbbvbvvb', 3: 'vgvvbbbbvvgv', 4: 'vvgvbddbvgvv'},
    'dead': {**_FACE, 2: 'bdvdbbbbdvdb', 3: 'vvdvbbbbvdvv', 4: 'vdvdbddbdvdv'},
}

# body segments, head to tail: width, height, length (model units)
SEGMENTS = [(5.5, 3.6, 2.6), (5.5, 3.6, 2.6), (5, 3.4, 2.6), (5, 3.2, 2.6), (4.5, 3, 2.6), (4, 2.6, 2.6), (3.2, 2.2, 2.6)]
SPACING = 2.2
LEG_YAW = [0.45, 0.3, 0.15, 0.0, -0.15, -0.3, -0.45]


def tergite(w, h, seed, glow_spot):
    """A segment's back plate: bone with a raised ridge down the middle, dark worn edges, a crust
    of sculk creeping over the sides and (on some segments) a pair of glowing pores."""
    import random
    rnd = random.Random(seed)
    cx = (w - 1) / 2

    def fn(x, y):
        dx = abs(x - cx)
        if y == h - 1:
            return 'k'
        if dx < 0.6:
            return 'l'
        if dx > w / 2 - 1.6:
            return 's' if rnd.random() < 0.55 else ('S' if rnd.random() < 0.5 else 'g' if rnd.random() < 0.15 else '.')
        if glow_spot and abs(dx - w * 0.24) < 0.6 and abs(y - h * 0.45) < 0.9:
            return 'G'
        if dx > w / 2 - 2.6:
            return 'd'
        if y == 0:
            return 'l'
        return 'b' if (x + y) % 5 else 'd'
    return gen(w, h, fn)


def parasite_pores(w, h, seed):
    """A segment's flank: crusted hide with one glowing spiracle."""
    rows = hide_face(w, h, seed, 0.9, 0.12)['map']
    # the bone back-plate's rim curling over the top, and a dark seam between segments
    rows[0] = 'l' * w
    rows[1] = ''.join('d' if x % 3 else 'b' for x in range(w))
    rows = ['k' + r[1:-1] + 'k' for r in rows]
    cy, cx = h // 2, w // 2
    rows[cy] = rows[cy][:cx] + 'G' + rows[cy][cx + 1:]
    if cx + 1 < w:
        rows[cy] = rows[cy][:cx + 1] + 'g' + rows[cy][cx + 2:]
    return rows


def sculk_parasite() -> Model:
    """The Sculk Parasite: a small, nasty Warden-kin centipede. Seven black, sculk-crusted segments
    with bone back-plates, bone spines and glowing pores, each on a pair of scuttling legs; a bone
    face-plate with four glowing eyes, hooked bone mandibles and two twitching Warden tendrils; and a
    glowing sting held up behind it. A bite leaves you corrupted - and leaves it dead."""
    pal = dict(SCULK)
    pal.update(WARDEN)
    m = Model('sculk_parasite', (64, 64), pal, {'sculk_parasite': {}}, res=2, expressions=EXPR)
    head = m.part('head', pivot=(0, 21, -6))
    head.cube((-3, -2.5, -4), (6, 4, 4), color='bone', pattern='mc', clusters=0.2, faces={
        'north': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=hd_rows(PARASITE_FACE['neutral'], 12, 8), keys=WK, glow_keys=WGLOW,
                      expr={k: hd_rows(v, 12, 8) for k, v in PARASITE_FACE.items() if k != 'neutral'}),
        'up': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=tergite(12, 8, 900, True), keys=WK, glow_keys=WGLOW),
        'east': hide_face(8, 8, 901, 0.9, 0.2), 'west': hide_face(8, 8, 902, 0.9, 0.2),
        'down': dict(color='hide_d', pattern='mc', clusters=0.2),
    })
    # a bony brow ridge over the eyes
    head.cube((-3.25, -3, -4.25), (6.5, 1, 1.5), color='bone_l', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='bone_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['lbdbbdbbdbdl', 'dbddbddbddbd'], keys=WK)})
    for side, sx in (('left', 1), ('right', -1)):
        # hooked bone mandibles that snap shut
        mand = head.part(f'{side}_mandible', pivot=(1.7 * sx, 1.0, -3.8), rot=(0, -0.35 * sx, 0))
        mand.cube((-0.6, -0.6, -2.6), (1.2, 1.2, 2.6), color='bone_l', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='bone_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['ld.', 'lb.', 'bbd', 'bd.', 'dk.', 'dk.'], keys=WK)})
        # a row of little teeth on the inner edge
        for k in range(2):
            mand.cube((-0.6 - 0.4 * sx if sx > 0 else 0.6 - 0.0, 0.0, -1.0 - k * 1.0), (0.4, 0.4, 0.4), color='tooth', pattern='mc', clusters=0.0, rim=False)
        tip = mand.part(f'{side}_mandible_tip', pivot=(0, 0, -2.5), rot=(0, 1.05 * sx, 0))
        tip.cube((-0.45, -0.45, -2.0), (0.9, 0.9, 2.0), color='tooth', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='bone_d', pattern='mc', clusters=0.0, rim=False),
            'up': dict(color='tooth', pattern='mc', clusters=0.0, rim=False, hd=True, map=['l.', 'b.', 'd.', 'k.'], keys=WK)})
        # the Warden's tendrils, swept back
        tendril(head, f'{side}_tendril', (1.8 * sx, -2.5, -1.5), (-0.5, 0, 0.45 * sx), 4)
    # the body: a chain of crusted segments, each with a bone spine and a pair of legs
    prev = head
    pivot = (0, 0, -0.3)
    for i, (w, h, d) in enumerate(SEGMENTS):
        seg = prev.part(f'segment_{i}', pivot=pivot)
        tw, th = math.ceil(w) * 2, math.ceil(d) * 2
        seg.cube((-w / 2, -h / 2, 0), (w, h, d), color='hide', pattern='mc', clusters=0.3, faces={
            'up': dict(color='bone', pattern='mc', clusters=0.0, hd=True, map=tergite(tw, th, 910 + i, i % 2 == 0), keys=WK, glow_keys=WGLOW),
            'east': dict(color='hide', pattern='mc', clusters=0.3, hd=True, map=parasite_pores(6, 8, 920 + i), keys=WK, glow_keys=WGLOW),
            'west': dict(color='hide', pattern='mc', clusters=0.3, hd=True, map=parasite_pores(6, 8, 930 + i), keys=WK, glow_keys=WGLOW),
            'down': dict(color='hide_d', pattern='mc', clusters=0.0, hd=True, map=gen(12, 6, lambda x, y: 'd' if y in (1, 4) and 2 < x < 9 else '.'), keys=WK),
        })
        # a bone spine on the ridge, raked back
        spine = seg.part(f'spine_{i}', pivot=(0, -h / 2, 1.2), rot=(-0.55, 0, 0))
        spine.cube((-0.4, -1.4 + 0.15 * i, -0.4), (0.8, 1.4 - 0.15 * i, 0.8), color='bone_l', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='tooth', pattern='mc', clusters=0.0)})
        for side, sx in (('left', 1), ('right', -1)):
            leg = seg.part(f'{side}_leg_{i}', pivot=((w / 2 - 0.3) * sx, 0.8, 1.2), rot=(0, LEG_YAW[i] * sx, 0.25 * sx))
            leg.cube((0 if sx > 0 else -2.6, -0.4, -0.4), (2.6, 0.8, 0.8), color='hide_l', pattern='mc', clusters=0.0, rim=False, faces={
                'up': dict(color='hide_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['sgk.s' if sx > 0 else '.s.kgs', 'k.s.k'], keys=WK,
                           glow_keys=WGLOW)})
            foot = leg.part(f'{side}_foot_{i}', pivot=(2.5 * sx, 0, 0), rot=(0, 0, 0.85 * sx))
            foot.cube((0 if sx > 0 else -2.0, -0.35, -0.35), (2.0, 0.7, 0.7), color='bone', pattern='mc', clusters=0.0, rim=False, faces={
                'up': dict(color='bone', pattern='mc', clusters=0.0, rim=False, hd=True, map=['lbbd' if sx > 0 else 'dbbl'], keys=WK)})
        prev = seg
        pivot = (0, 0, SPACING)
    last = prev
    w, h, d = SEGMENTS[-1]
    # two long bone prongs trailing behind, and the glowing sting held up over the back
    for side, sx in (('left', 1), ('right', -1)):
        cer = last.part(f'{side}_cercus', pivot=(1.0 * sx, 0.3, d - 0.3), rot=(-0.25, 0.45 * sx, 0))
        cer.cube((-0.3, -0.3, 0), (0.6, 0.6, 3.5), color='bone', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='bone', pattern='mc', clusters=0.0, rim=False, hd=True, map=['l', 'b', 'b', 'd', 'b', 'd', 'k'], keys=WK)})
    tail = last.part('tail', pivot=(0, -0.5, d - 0.4), rot=(0.8, 0, 0))
    tail.cube((-0.6, -0.6, 0), (1.2, 1.2, 2.4), color='hide_l', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='hide_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['sg', 'kS', 'gs', 'Sk', 'sg'], keys=WK, glow_keys=WGLOW)})
    sting = tail.part('stinger', pivot=(0, 0, 2.3), rot=(0.7, 0, 0))
    sting.cube((-0.5, -0.5, 0), (1, 1, 1.8), color='glow', pattern='mc', clusters=0.0, rim=False, glow=True)
    sting.cube((-0.25, -0.25, 1.6), (0.5, 0.5, 1.2), color='tooth', pattern='mc', clusters=0.0, rim=False, glow=True)
    return m


ALL = {'sculk_parasite': sculk_parasite}
