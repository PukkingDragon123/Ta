"""A2 / M3 Echoer: the Echoer (entity id `enchoer`), Soul Golems and Nibs - model geometry and paint in the
Sculk-mob pipeline (modelkit `Model(..., res=2)` + the automatic x2 detail and materials pass, glow layers and
expressions), their spawn eggs, sound events and the data/lang that goes with them (hooked from mobs.ALL,
gen_assets and gen_data)."""
import math
import random

from modelkit import Model


def hd_rows(spec, w, h):
    rows = []
    for y in range(h):
        t = spec.get(y, '')
        rows.append((t + '.' * w)[:w])
    return rows


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def grid(w, h, fn):
    """A w x h texel map from fn(x, y) -> char."""
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def overlay(base, top, at=(0, 0)):
    """Lays map `top` over `base` with its corner at `at`; '.' in top keeps base."""
    out = [list(r) for r in base]
    ox, oy = at
    for j, row in enumerate(top):
        for i, ch in enumerate(row):
            y, x = oy + j, ox + i
            if ch != '.' and 0 <= y < len(out) and 0 <= x < len(out[y]):
                out[y][x] = ch
    return [''.join(r) for r in out]


def flip(rows):
    return [r[::-1] for r in rows]


SKIP = dict(skip=True)


# =========================================================================== THE ECHOER
# M3: the Echoer is a deer spirit again (the user's reference: a teal spirit with pale antlers, a skull-pale
# face, shingled leaf-scales and a pale star of light on its chest). A slender deer with a deep chest and long
# jointed legs, its pelt the teal of the Sift plains' grass shingled with leaf-scales, a pale bone mask over its
# long face with eyes of cyan light, great pale antlers whose tines glow with song and carry little crystal
# chimes, a leafy ruff and crest, a golden star on its chest, glowing song-marks (dots down the spine, a spiral
# on each haunch, tear-lines under its eyes, a line down its nose), pale socks over dark hooves whose soles glow
# where it treads on the air, and a perky pale scut.
ECHOER_PAL = {
    'coat': '#2e9286', 'coat_l': '#5cc4b3', 'coat_d': '#17564f', 'coat_m': '#3ea395', 'coat_n': '#277f74', 'coat_o': '#1d675e', 'coat_h': '#56bcac',
    'crest': '#4fbfa8', 'crest_l': '#8fe6cf', 'crest_d': '#25786a',
    'mane': '#b4ece0', 'mane_l': '#e6fcf5', 'mane_d': '#77bfb1',
    'belly': '#c9ede3', 'belly_l': '#effdf8', 'belly_d': '#94c9bc',
    'mask': '#e9e2cc', 'mask_l': '#fffbf0', 'mask_d': '#b2a890',
    'bone': '#ece5cf', 'bone_l': '#fffcf3', 'bone_d': '#b3a991',
    'hoof': '#26394b', 'hoof_l': '#3e576c', 'hoof_d': '#142130',
    'ear_in': '#ecb4c8', 'ear_in_d': '#c5849f',
    'nose': '#22383f', 'nose_l': '#43606a', 'mouth': '#142228',
    'glow': '#8ff6ff', 'glow_l': '#eaffff', 'glow_d': '#38c4d6',
    'star': '#f3d27a', 'star_l': '#fff6cf', 'star_d': '#c3923e',
    'socket': '#0c1b24', 'eye': '#a6f8ff', 'eye_core': '#ffffff', 'lid': '#8a8068',
    'crystal': '#9df3ff', 'crystal_l': '#e6ffff', 'crystal_d': '#4fbfd6', 'thread': '#d8cfb6',
}
ECHOER_MATERIALS = {'coat': 'scales', 'crest': 'plant', 'mane': 'fur', 'belly': 'fur', 'mask': 'bone', 'bone': 'bone',
                    'hoof': 'chitin', 'ear_in': 'skin', 'nose': 'skin', 'star': 'metal', 'crystal': 'crystal', 'thread': 'cloth'}
ECHOER_EXPRS = ['blink', 'happy', 'sleep', 'hurt', 'dead']
SCALE_KEYS = {'o': 'coat_o', 'h': 'coat_h', 'm': 'coat_m', 'n': 'coat_n', 'g': 'glow', 'G': 'glow_l'}

# the left eye on the mask (outer corner at column 0); the right eye is its mirror image
# S1 land (the user's rule: never cartoon eyes): small almond eyes of light in dark sockets, the warden's way - no
# white core, no smiling arcs, no crosses; moods only move the lids
DEER_EYES = {
    '': ['....', 'sIIs', 'sIIs', '....'],
    'blink': ['....', '....', 'llll', '....'],
    'happy': ['....', 'llll', 'sIIs', '....'],
    'sleep': ['....', '....', 'llll', '....'],
    'hurt': ['....', 'llll', 'llll', '....'],
    'dead': ['....', 'ssss', 'ssss', '....'],
}
STAR7 = ['___g___', '_g_G_g_', '__GWG__', 'gGWWWGg', '__GWG__', '_g_G_g_', '___g___']
SPIRAL7 = ['.ggggg.', 'g.....g', 'g.ggg.g', 'g.g.g.g', 'g.g..g.', 'g..gg..', '.g.....']


def _leaf_scales(w, h, seed, sw=4, sh=3, mottle=0.18):
    """Shingled leaf-scales (hd map): rows of rounded scales, every other row offset by half a scale. Each
    scale has a dark lower rim (o), a lit texel under its upper edge (h), and now and then a lighter or
    darker fill (m / n) so the pelt is mottled."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for row in range(-1, h // sh + 2):
        off = (row % 2) * (sw // 2)
        for col in range(-2, w // sw + 2):
            x0, y0 = col * sw + off, row * sh
            t = rnd.random()
            fill = 'm' if t < mottle * 0.55 else 'n' if t < mottle else '.'
            for dy in range(sh):
                for dx in range(sw):
                    x, y = x0 + dx, y0 + dy
                    if not (0 <= x < w and 0 <= y < h):
                        continue
                    if (dy == sh - 1 and 0 < dx < sw - 1) or (dy == sh - 2 and dx in (0, sw - 1)):
                        ch = 'o'
                    elif dy == 0 and dx == 1:
                        ch = 'h'
                    else:
                        ch = fill
                    g[y][x] = ch
    return [''.join(r) for r in g]


def _deer_face(expr):
    """The bone mask's front (12 x 10 texels; the muzzle covers the middle of the lower half): two eyes of
    light, a jewel of song on the brow and glowing tear-lines running down from the eyes."""
    g = [['.'] * 12 for _ in range(10)]
    for j, row in enumerate(DEER_EYES[expr]):
        for i, ch in enumerate(row):
            if ch != '.':
                g[1 + j][i] = ch
                g[1 + j][11 - i] = ch
    for x in (5, 6):
        g[0][x], g[1][x], g[2][x] = 'g', 'G', 'g'
    g[0][4] = g[0][7] = 'd'
    for y, x in ((5, 1), (6, 1), (7, 2), (8, 2)):
        g[y][x] = g[y][11 - x] = 'g' if expr != 'dead' else 'd'
    return [''.join(r) for r in g]


def _deer_ear(w, h, mirror, back=False):
    """A deer's leaf-shaped ear seen from the front (tip at the far end): a coat rim, the pink inside with a
    darker crease, '_' cut away round the leaf."""
    def px(x, y):
        u = x / (w - 1)                                    # 0 at the root, 1 at the tip
        half = (h / 2.0) * (0.55 + 0.9 * u) * (1.0 - u ** 3)
        d = abs(y + 0.5 - h / 2.0)
        if d > half + 0.2:
            return '_'
        if back or d > half - 1.0 or x == 0:
            return 'r' if (x + y) % 5 else 'R'
        return 'P' if abs(y + 0.5 - h / 2.0) < 0.6 and 0.2 < u < 0.8 else 'p'
    out = grid(w, h, px)
    return flip(out) if mirror else out


def _crest(w, h, seed):
    """A crest of leaf blades along the back of the neck, side view (hd map): each blade a sawtooth whose
    tip sweeps back, '_' cut away above them, veins (v) and lit edges (l)."""
    rnd = random.Random(seed)
    tips = [int(w * (0.55 + 0.45 * rnd.random())) for _ in range(h // 3 + 2)]

    def px(x, y):
        k, t = y // 3, y % 3
        reach = tips[k] - t * 1
        if x >= reach:
            return '_'
        if x == reach - 1:
            return 'l'
        return 'v' if x == 1 and t == 1 else '.'
    return grid(w, h, px)


def enchoer() -> Model:
    """M3 Echoer: the deer spirit (see the notes above). Model space: standing, its hooves touch y = 24
    and its antler tips reach about y = -16 (2.5 blocks); it mostly skips and hovers in the air."""
    m = Model('enchoer', (128, 64), dict(ECHOER_PAL), {'enchoer': {}}, res=2, expressions=ECHOER_EXPRS, materials=ECHOER_MATERIALS)
    m.exact_uv = True

    def coat(wu, hu, seed, top=None, at=None, big=True, **kw):
        w, h = int(round(wu * 2)), int(round(hu * 2))
        mp = _leaf_scales(w, h, seed, 6, 4) if big and min(w, h) >= 8 else _leaf_scales(w, h, seed)
        if top:
            if at is None:
                at = ((len(mp[0]) - len(top[0])) // 2, (len(mp) - len(top)) // 2)
            mp = overlay(mp, top, at)
        d = dict(color='coat', pattern='mc', clusters=0.15, hd=True, map=mp, keys=dict(SCALE_KEYS), glow_keys='gG', map_material=True)
        d.update(kw)
        return d

    def pale(key='belly', **kw):
        d = mc(key, clusters=0.15, streaks=0.8)
        d.update(kw)
        return d

    # ---- the body: a barrel, a deep chest with a leafy ruff, a rounded haunch and the withers
    body = m.part('body', pivot=(0, 11, 0))
    spine = grid(12, 20, lambda x, y: 'g' if x in (5, 6) and y % 5 == 2 else 'G' if x in (5, 6) and y % 5 == 3 else '.')
    body.cube((-3, -3, -5), (6, 6.5, 10), **mc('coat'), faces={
        'east': coat(10, 6.5, 1), 'west': coat(10, 6.5, 2), 'up': coat(6, 10, 3, top=spine, at=(0, 0)), 'down': pale()})
    # the chest: scaled, a pale bib down its front
    bib = grid(14, 16, lambda x, y: 'p' if abs(x - 6.5) < 5.5 - y * 0.3 else '.')
    body.cube((-3.5, -3.75, -9.5), (7, 8, 5), **mc('coat'), faces={
        'east': coat(5, 8, 4), 'west': coat(5, 8, 5), 'up': coat(7, 5, 6), 'down': pale(),
        'north': coat(7, 8, 41, top=bib, at=(0, 0), keys=dict(SCALE_KEYS, p='belly'))})
    rump = grid(14, 14, lambda x, y: 'p' if abs(x - 6.5) < 3.4 - abs(y - 7.0) * 0.4 and 2 < y < 12 else '.')
    body.cube((-3.5, -3.5, 4.5), (7, 7, 5), **mc('coat'), faces={
        'east': coat(5, 7, 7, top=SPIRAL7), 'west': coat(5, 7, 8, top=flip(SPIRAL7)), 'up': coat(7, 5, 9),
        'south': coat(7, 7, 10, top=rump, at=(0, 0), keys=dict(SCALE_KEYS, p='belly')), 'down': pale()})
    body.cube((-2.5, -4.5, -8.5), (5, 1.5, 5.5), **mc('coat'), faces={
        'up': coat(5, 5.5, 11), 'east': coat(5.5, 1.5, 12), 'west': coat(5.5, 1.5, 13), 'north': coat(5, 1.5, 14)})
    # a collar of leaves where the neck meets the chest, hanging in tufts
    body.cube((-3.75, -3.5, -10.0), (7.5, 4, 1.5), **mc('crest', clusters=0.15, fringe=2, rim=False), faces={'up': SKIP})
    # the golden star on its chest (cut to a star; it flares with every note it sings)
    star = body.part('star', pivot=(0, 1.25, -10.0))
    star.cube((-1.75, -1.75, -1), (3.5, 3.5, 1), **mc('star', clusters=0.0, rim=False), faces={
        'north': dict(mc('star', clusters=0.0, rim=False), hd=True, map=STAR7, keys={'g': 'star', 'G': 'star_l', 'W': 'glow_l'}, glow_keys='gGW'),
        'south': SKIP, 'east': SKIP, 'west': SKIP, 'up': SKIP, 'down': SKIP})

    # ---- the neck: scaled, pale at the throat, with a leafy collar, a beard of pale leaves and a crest
    neck = body.part('neck', pivot=(0, -2.5, -7.0), rot=(0.42, 0, 0))
    dots = grid(8, 21, lambda x, y: 'g' if x in (3, 4) and y % 6 == 3 else '.')
    neck.cube((-2, -10, -2.25), (4, 10.5, 4.5), **mc('coat'), faces={
        'east': coat(4.5, 10.5, 15), 'west': coat(4.5, 10.5, 16), 'south': coat(4, 10.5, 17, top=dots, at=(0, 0)),
        'north': pale('belly', streaks=1.0), 'up': mc('coat_d', clusters=0.1)})
    neck.cube((-1.5, -7, -2.75), (3, 6, 1), **mc('mane', clusters=0.1, streaks=1.2, fringe=2, rim=False), faces={'up': SKIP})
    neck.cube((-2.5, -2.5, -2.75), (5, 3, 5.5), **mc('crest', clusters=0.15, fringe=2, fringe_phase=3, rim=False))
    crest = _crest(6, 20, 3)
    neck.cube((0, -10.5, 1.75), (0, 10, 3), **mc('crest', clusters=0.0, rim=False), faces={
        'east': dict(mc('crest', clusters=0.1, rim=False), hd=True, map=crest, keys={'l': 'crest_l', 'v': 'crest_d'}, map_material=True),
        'west': dict(mc('crest', clusters=0.1, rim=False), hd=True, map=flip(crest), keys={'l': 'crest_l', 'v': 'crest_d'}, map_material=True)})

    # ---- the head: a long face under a bone mask, eyes of light, a dark nose, a jaw that sings
    head = neck.part('head', pivot=(0, -10, -0.5), rot=(-0.17, 0, 0))
    ek = {'s': 'socket', 'I': 'eye', 'H': 'eye_core', 'l': 'lid', 'g': 'glow', 'G': 'glow_l', 'd': 'mask_d'}
    brow = grid(12, 11, lambda x, y: 'g' if (x in (5, 6) and 3 <= y <= 7 and y % 2 == 1) else 'd' if y == 0 and 2 <= x <= 9 else '.')
    head.cube((-3, -4.5, -3), (6, 5, 5.5), **mc('coat'), faces={
        'north': dict(mc('mask', clusters=0.0), hd=True, map=_deer_face(''), keys=ek, glow_keys='IHgG',
                      expr={e: _deer_face(e) for e in ECHOER_EXPRS}),
        'up': dict(mc('mask', clusters=0.2), hd=True, map=brow, keys=ek, glow_keys='g'),
        'east': coat(5.5, 5, 18), 'west': coat(5.5, 5, 19), 'south': coat(6, 5, 20), 'down': mc('coat_d')})
    nose = ['.nnnn.', 'nknnkn', '.nNNn.', '..dd..', '......', '......']
    bridge = grid(6, 9, lambda x, y: 'g' if x in (2, 3) and y % 2 == 0 and 1 <= y <= 7 else 'd' if x in (0, 5) else '.')
    head.cube((-1.5, -2.5, -7.5), (3, 3, 4.5), **mc('mask', clusters=0.2), faces={
        'north': dict(mc('mask', clusters=0.0), hd=True, map=nose, keys={'n': 'nose', 'N': 'nose_l', 'k': 'mouth', 'd': 'mask_d'}),
        'up': dict(mc('mask', clusters=0.2), hd=True, map=bridge, keys={'g': 'glow', 'd': 'mask_d'}, glow_keys='g'),
        'down': mc('belly', clusters=0.1)})
    head.cube((-3.25, -2, -2.5), (6.5, 2.5, 3), **mc('mane', clusters=0.1, streaks=1.0, fringe=1, rim=False), faces={'up': SKIP})
    jaw = head.part('jaw', pivot=(0, 0.5, -3.0))
    jaw.cube((-1.25, 0, -4.25), (2.5, 1, 4.25), **mc('belly', clusters=0.1), faces={
        'up': mc('mouth', clusters=0.0, rim=False), 'north': mc('belly_d', clusters=0.0, rim=False)})
    for side, sx in (('left', 1), ('right', -1)):
        ear = head.part(f'{side}_ear', pivot=(2.6 * sx, -3.75, 0.0), rot=(0.15, -0.3 * sx, -0.4 * sx))
        ekeys = {'r': 'coat', 'R': 'coat_d', 'p': 'ear_in', 'P': 'ear_in_d'}
        ear.cube((0 if sx > 0 else -4, -1.25, -0.25), (4, 2.5, 0.5), **mc('coat', clusters=0.0, rim=False), faces={
            'north': dict(mc('ear_in', clusters=0.0, rim=False), hd=True, map=_deer_ear(8, 5, sx < 0), keys=ekeys),
            'south': dict(mc('coat', clusters=0.0, rim=False), hd=True, map=_deer_ear(8, 5, sx > 0, back=True), keys=ekeys),
            'up': SKIP, 'down': SKIP, 'east': SKIP, 'west': SKIP})

        # ---- the antlers: a burr, a beam sweeping up, out and back, a brow tine forward, three tines on the
        # upper beam and a crown tine, every tip a bead of light; a crystal chime hangs from each upper beam
        a = head.part(f'{side}_antler', pivot=(1.6 * sx, -4.5, -0.75), rot=(-0.3, 0, 0.38 * sx))
        a.cube((-0.9, -1.0, -0.9), (1.8, 1.0, 1.8), **mc('bone_d', clusters=0.2, rim=False))
        a.cube((-0.5, -7, -0.5), (1, 6, 1), **mc('bone', clusters=0.2, rim=False))
        bt = a.part(f'{side}_brow_tine', pivot=(0, -2.5, -0.3), rot=(1.15, 0, -0.15 * sx))
        bt.cube((-0.4, -3, -0.4), (0.8, 3, 0.8), **mc('bone', clusters=0.1, rim=False))
        bt.cube((-0.45, -3.8, -0.45), (0.9, 0.9, 0.9), **mc('glow', clusters=0.0, rim=False, glow=True))
        up = a.part(f'{side}_antler_upper', pivot=(0, -7, 0), rot=(0.3, 0, 0.32 * sx))
        up.cube((-0.5, -7, -0.5), (1, 7, 1), **mc('bone', clusters=0.2, rim=False))
        for i, (y, rx, rz, ln) in enumerate(((-1.5, 1.05, 0.35, 3.5), (-4.0, 0.8, 0.2, 4.0), (-6.25, 0.45, 0.35, 3.0))):
            t = up.part(f'{side}_tine_{i}', pivot=(0, y, 0), rot=(rx, 0, -rz * sx))
            t.cube((-0.35, -ln, -0.35), (0.7, ln, 0.7), **mc('bone', clusters=0.1, rim=False))
            t.cube((-0.45, -ln - 0.9, -0.45), (0.9, 0.9, 0.9), **mc('glow', clusters=0.0, rim=False, glow=True))
        crown = up.part(f'{side}_crown_tine', pivot=(0, -7, 0), rot=(-0.2, 0, 0.5 * sx))
        crown.cube((-0.35, -2.4, -0.35), (0.7, 2.4, 0.7), **mc('bone', clusters=0.1, rim=False))
        crown.cube((-0.45, -3.3, -0.45), (0.9, 0.9, 0.9), **mc('glow', clusters=0.0, rim=False, glow=True))
        chime = up.part(f'{side}_chime', pivot=(0, -2.8, 0.2), rot=(-0.25, 0, -0.7 * sx))  # hangs plumb at rest
        chime.cube((-0.1, 0, -0.1), (0.2, 2.4, 0.2), **mc('thread', clusters=0.0, rim=False))
        chime.cube((-0.5, 2.4, -0.5), (1, 1.5, 1), **mc('crystal', clusters=0.0, rim=False, glow=True), faces={
            'down': mc('crystal_l', clusters=0.0, rim=False, glow=True)})

    # ---- legs: thighs in the pelt, slender shins with pale socks, dark hooves whose soles glow
    for side, sx in (('left', 1), ('right', -1)):
        for end, (px, py, pz), (r1, r2, r3) in (('front', (2.1, 1.0, -7.0), (0.0, 0.0, 0.0)),
                                                ('hind', (2.2, 1.5, 6.0), (-0.25, 0.6, -0.35))):
            nm = f'{side}_{end}'
            leg = body.part(nm + '_leg', pivot=(px * sx, py, pz), rot=(r1, 0, 0))
            if end == 'front':
                leg.cube((-1.25, -1, -1.5), (2.5, 6.5, 3), **mc('coat'), faces={
                    'east': coat(3, 6.5, 21 + sx), 'west': coat(3, 6.5, 23 + sx), 'north': coat(2.5, 6.5, 25 + sx), 'south': coat(2.5, 6.5, 27 + sx)})
            else:
                leg.cube((-1.5, -1.5, -1.75), (3, 7, 3.5), **mc('coat'), faces={
                    'east': coat(3.5, 7, 31 + sx), 'west': coat(3.5, 7, 33 + sx), 'north': coat(3, 7, 35 + sx), 'south': coat(3, 7, 37 + sx)})
            shin = leg.part(nm + '_shin', pivot=(0, 5.5, 0), rot=(r2, 0, 0))
            shin.cube((-0.75, 0, -0.75), (1.5, 5, 1.5), **mc('coat', clusters=0.2, bands=[(3, 'belly')]))
            hoof = shin.part(nm + '_hoof', pivot=(0, 5, 0), rot=(r3, 0, 0))
            hoof.cube((-1, 0, -1.1), (2, 1.5, 2), **mc('hoof', clusters=0.1), faces={
                'north': dict(mc('hoof', clusters=0.0), hd=True, map=['.d..', '.d..', '.d..'][:3], keys={'d': 'hoof_d'}),
                'down': mc('glow', clusters=0.0, rim=False, glow=True)})

    # ---- the scut: a perky tail, pale beneath, tipped with a tuft of pale leaves
    tail = body.part('tail', pivot=(0, -2.5, 9.0), rot=(0.35, 0, 0))
    tail.cube((-1, -0.5, -0.5), (2, 1, 2.5), **mc('coat'), faces={
        'up': coat(2, 2.5, 40), 'down': pale(), 'south': pale()})
    tail.cube((-0.75, -0.25, 2.0), (1.5, 1, 1), **mc('mane', clusters=0.1, streaks=1.0, rim=False))
    return m


# =========================================================================== SOUL GOLEM
# CAVE v4: a copper mole. The Soul Golem is built like vanilla's Copper Golem - riveted copper plates going to
# green patina in the seams and drips, small dark button eyes with a glint of soul light, a lightning-rod
# antenna on its head with a soul lamp in its cap - but in the shape of a mole: low and wide, a tapered head
# with a copper drill for a nose, huge spade forepaws turned out with polished claws, stubby hind legs, a
# riveted stub of a tail and a soul-fire vent on its back. It burrows into the ground and brings up finds.
GOLEM_PAL = {
    'copper': '#c87456', 'copper_l': '#e3826c', 'copper_d': '#9a5038',
    'plate': '#b8664a', 'plate_l': '#d67b5b', 'plate_d': '#8a4632',
    'seam': '#6b3424', 'rivet': '#f0a888', 'rivet_d': '#7a3e2a',
    'patina': '#59b292', 'patina_l': '#7ac5a6', 'patina_d': '#396e59',
    'claw': '#f2c9a8', 'claw_l': '#fff0e0', 'claw_d': '#b07a58',
    'drill': '#d98a68', 'drill_l': '#f6b896', 'drill_d': '#8a4632', 'groove': '#4e2418',
    'eye': '#26181a', 'eye_l': '#d6fdff', 'socket': '#5a2a1c',
    'soul': '#5fe9ff', 'soul_l': '#d6fdff', 'soul_d': '#2aa9c8', 'vent': '#2a1410',
}
GOLEM_MATERIALS = {'copper': 'metal', 'plate': 'metal', 'drill': 'metal', 'claw': 'metal', 'patina': 'stone', 'soul': 'flat'}
GOLEM_EXPRS = ['blink', 'happy', 'sleep', 'hurt', 'dead']
PLATE_KEYS = {'s': 'seam', 'r': 'rivet', 'R': 'rivet_d', 'p': 'patina', 'P': 'patina_l', 'q': 'patina_d', 'h': 'copper_l',
              'g': 'soul', 'G': 'soul_l', 'v': 'vent'}
# the left button eye (3 x 3 texels; the right one is its mirror image)
MOLE_EYES = {
    '': ['.e.', 'eie', '.e.'],
    'blink': ['...', 'eee', '...'],
    'happy': ['...', '.e.', 'e.e'],
    'sleep': ['...', '...', 'eee'],
    'hurt': ['e..', '.ee', 'e..'],
    'dead': ['e.e', '.e.', 'e.e'],
}


def _plates(w, h, seed, every=10, patina=0.14, vent=None, spine=False):
    """Riveted copper plates (hd map): a dark seam every `every` rows with a domed rivet (lit r over a dark R)
    every four texels along it, a vertical seam staggered between the rows, brushed highlights along the top
    of each plate, and patina - the oxidation creeps out of the seams (q in them, p / P beside them) and runs
    down in short drips. `vent` puts the glowing soul-fire grille (g / G) at that rectangle."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    for y in range(h):
        band = y // every
        if y % every == every - 1:
            for x in range(w):
                g[y][x] = 's'
            for x in range(2 + (band % 2) * 2, w, 5):
                g[y - 1][x] = 'r'
                g[y][x] = 'R'
        elif y % every == 0:
            for x in range(w):
                if rnd.random() < 0.3:
                    g[y][x] = 'h'
        vx = (band * 7 + 5) % 14
        for x in range(vx, w, 14):
            if g[y][x] == '.':
                g[y][x] = 's'
    for y in range(h):
        for x in range(w):
            if g[y][x] == 's' and rnd.random() < patina * 1.6:
                g[y][x] = 'q'
                for dy in range(1, rnd.randrange(2, 4)):
                    if y + dy < h and g[y + dy][x] in '.h':
                        g[y + dy][x] = 'p' if dy == 1 else 'P'
            elif g[y][x] in '.h' and rnd.random() < patina * 0.05:
                g[y][x] = 'P' if rnd.random() < 0.4 else 'p'
    if spine:
        cx = w // 2
        for y in range(h):
            g[y][cx - 1] = g[y][cx] = 's' if y % 4 else 'r'
    if vent:
        x0, y0, vw, vh = vent
        for y in range(y0, y0 + vh):
            for x in range(x0, x0 + vw):
                if 0 <= y < h and 0 <= x < w:
                    g[y][x] = 'v' if (y - y0) % 2 == 0 else ('G' if x in (x0, x0 + vw - 1) else 'g')
    return [''.join(r) for r in g]


def _mole_face(expr):
    """The front of the head (14 x 10 texels; the snout covers the lower middle): two small dark button eyes
    with a glint of soul light just above the snout, riveted brow plates and patina in the seams."""
    g = [list(r) for r in _plates(14, 10, 77, every=10, patina=0.1)]
    for j, row in enumerate(MOLE_EYES[expr]):
        for i, ch in enumerate(row):
            if ch != '.':
                g[1 + j][2 + i] = ch
                g[1 + j][11 - i] = ch
    for x in (1, 5, 8, 12):
        g[0][x] = 'r'
    return [''.join(r) for r in g]


def soul_golem() -> Model:
    """CAVE v4: the copper mole (see the notes above). Model space: it stands low, the top of its back at
    y = 15, the antenna's lamp at about y = 5, its drill nose reaching z = -14."""
    m = Model('soul_golem', (96, 64), dict(GOLEM_PAL), {'soul_golem': {}}, res=2, expressions=GOLEM_EXPRS, materials=GOLEM_MATERIALS)
    m.exact_uv = True

    def plates(wu, hu, seed, color='copper', **kw):
        d = dict(color=color, pattern='mc', clusters=0.15, hd=True, map=_plates(int(wu * 2), int(hu * 2), seed, **kw), keys=PLATE_KEYS,
                 glow_keys='gG', map_material=True)
        return d

    # ---- stubby hind legs (from the root, so they stay planted while the body rolls)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(3 * sx, 21, 4))
        leg.cube((-1, 0, -1.25), (2, 2, 2.5), **mc('copper_d', clusters=0.2), faces={'east': plates(2.5, 2, 90 + sx), 'west': plates(2.5, 2, 92 + sx)})
        foot = leg.part(f'{side}_foot', pivot=(0, 2, 0))
        foot.cube((-1.25, 0, -2), (2.5, 1, 3), **mc('copper_d', clusters=0.1, rim=False), faces={
            'north': dict(mc('claw', clusters=0.0, rim=False), hd=True, map=['c.c.c'], keys={'c': 'claw_d'})})

    # ---- the body: a low, wide riveted barrel, a hump of plates over the shoulders, a soul-fire vent on its back
    body = m.part('body', pivot=(0, 19, 0))
    # a stepped dome of plates: the barrel, a narrower course over it, the hump over the shoulders
    body.cube((-4.5, -3, -5.5), (9, 6, 11), **mc('copper'), faces={
        'east': plates(11, 6, 1), 'west': plates(11, 6, 2), 'south': plates(9, 6, 3),
        'up': plates(9, 11, 4), 'down': plates(9, 11, 5, color='plate_d', patina=0.3), 'north': plates(9, 6, 6)})
    body.cube((-3.5, -4.5, -5), (7, 1.5, 10), **mc('plate'), faces={
        'up': plates(7, 10, 7, color='plate', spine=True, vent=(4, 12, 6, 5)), 'north': plates(7, 1.5, 8, color='plate'),
        'east': plates(10, 1.5, 9, color='plate'), 'west': plates(10, 1.5, 10, color='plate'), 'south': plates(7, 1.5, 12, color='plate')})
    body.cube((-2.5, -5.5, -4.5), (5, 1, 5), **mc('plate_l'), faces={'up': plates(5, 5, 13, color='plate_l')})
    body.cube((-3.5, -2.5, 5.5), (7, 5, 1), **mc('plate_d', clusters=0.2), faces={'south': plates(7, 5, 11, color='plate_d', patina=0.25)})
    tail = body.part('tail', pivot=(0, -0.5, 6.5), rot=(-0.35, 0, 0))
    tail.cube((-0.5, -0.5, 0), (1, 1, 2.5), **mc('copper_d', clusters=0.0, rim=False), faces={'south': mc('rivet', clusters=0.0, rim=False)})

    # ---- the head: tapered, riveted, button eyes, a drill for a nose and a lightning rod on top
    head = body.part('head', pivot=(0, -0.5, -5.5))
    head.cube((-3.5, -3.5, -3.5), (7, 5, 3.5), **mc('copper'), faces={
        'north': dict(mc('copper', clusters=0.1), hd=True, map=_mole_face(''), keys=dict(PLATE_KEYS, e='eye', i='eye_l'), glow_keys='i',
                      map_material=True, expr={e: _mole_face(e) for e in GOLEM_EXPRS}),
        'up': plates(7, 3.5, 21), 'east': plates(3.5, 5, 22), 'west': plates(3.5, 5, 23), 'down': mc('plate_d', clusters=0.2)})
    head.cube((-2.5, -2, -6), (5, 3, 2.5), **mc('copper'), faces={
        'up': plates(5, 2.5, 24), 'east': plates(2.5, 3, 25), 'west': plates(2.5, 3, 26), 'down': mc('plate_d', clusters=0.2),
        'north': dict(mc('copper_d', clusters=0.1), hd=True, map=['..........', '.r......r.', '..........', '..........', '.r......r.', '..........'],
                      keys=PLATE_KEYS)})
    for side, sx in (('left', 1), ('right', -1)):
        # rivet bosses on its cheeks, where a mole's whiskers would be
        head.cube((2.5 * sx - 0.5, -1.25, -5.5), (1, 1, 1), **mc('rivet', clusters=0.0, rim=False))
    drill = head.part('drill', pivot=(0, -0.5, -6))
    spiral = lambda w, h, ph: grid(w, h, lambda x, y: 'g' if (x + y + ph) % 4 == 0 else 'l' if (x + y + ph) % 4 == 2 else '.')
    dk = {'g': 'groove', 'l': 'drill_l'}
    for i, (s, d, z) in enumerate(((3, 1.5, 0), (2, 1.5, -1.5), (1, 1, -3))):
        side_map = dict(mc('drill', clusters=0.0, rim=False), hd=True, map=spiral(int(s * 2), int(d * 2), i), keys=dk)
        drill.cube((-s / 2, -s / 2, z - d), (s, s, d), **mc('drill', clusters=0.0, rim=False), faces={
            'up': side_map, 'down': side_map, 'east': side_map, 'west': side_map,
            'north': mc('drill_l' if i == 2 else 'drill_d', clusters=0.0, rim=False)})
    antenna = head.part('antenna', pivot=(0, -3.5, -1.5), rot=(-0.12, 0, 0))
    antenna.cube((-0.75, -0.5, -0.75), (1.5, 0.5, 1.5), **mc('copper_d', clusters=0.0, rim=False))
    antenna.cube((-0.5, -5, -0.5), (1, 4.5, 1), **mc('copper_l', clusters=0.0, rim=False), faces={
        'north': dict(mc('copper_l', clusters=0.0, rim=False), hd=True, map=['..', 'ss', '..', '..', 'qq', '..', '..', 'ss', '..'], keys=PLATE_KEYS),
        'south': dict(mc('copper_l', clusters=0.0, rim=False), hd=True, map=['..', 'ss', '..', '..', 'qq', '..', '..', 'ss', '..'], keys=PLATE_KEYS)})
    cap = antenna.part('antenna_tip', pivot=(0, -5, 0))
    lamp = ['rssr', 'sGGs', 'sgGs', 'rssr']
    cap.cube((-1, -2, -1), (2, 2, 2), **mc('copper', clusters=0.0, rim=False), faces={
        'north': dict(mc('copper', clusters=0.0, rim=False), hd=True, map=lamp, keys=PLATE_KEYS, glow_keys='gG'),
        'south': dict(mc('copper', clusters=0.0, rim=False), hd=True, map=lamp, keys=PLATE_KEYS, glow_keys='gG'),
        'east': dict(mc('copper', clusters=0.0, rim=False), hd=True, map=lamp, keys=PLATE_KEYS, glow_keys='gG'),
        'west': dict(mc('copper', clusters=0.0, rim=False), hd=True, map=lamp, keys=PLATE_KEYS, glow_keys='gG'),
        'up': dict(mc('soul', clusters=0.0, rim=False, glow=True))})

    # ---- the forepaws: huge spades turned out to the sides, palms patinated, four polished claws each
    for side, sx in (('left', 1), ('right', -1)):
        arm = body.part(f'{side}_arm', pivot=(4.25 * sx, 0.5, -3.5), rot=(0, 0, -0.35 * sx))
        arm.cube((0 if sx > 0 else -2.5, -1, -1.5), (2.5, 3, 3), **mc('copper'), faces={
            'up': plates(2.5, 3, 30 + sx), 'north': plates(2.5, 3, 32 + sx), 'south': plates(2.5, 3, 34 + sx)})
        paw = arm.part(f'{side}_paw', pivot=(1.75 * sx, 2.25, -0.5), rot=(0, -0.3 * sx, 0.3 * sx))
        paw.cube((-1.75, 0, -2.5), (3.5, 1.5, 4), **mc('copper_d'), faces={
            'up': plates(3.5, 4, 36 + sx, color='copper_d', patina=0.3), 'down': mc('patina_d', clusters=0.3)})
        for k, cx in enumerate((-1.5, -0.5, 0.5, 1.5)):
            paw.cube((cx - 0.25, 0.5, -4.5 + abs(cx) * 0.3), (0.5, 0.75, 2), **mc('claw', clusters=0.0, rim=False), faces={
                'north': mc('claw_d', clusters=0.0, rim=False), 'up': mc('claw_l', clusters=0.0, rim=False)})
    return m


# =========================================================================== NIB
# A tiny glowing wisp-butterfly (its old design, kept): a bright little body - a round head with two bead
# eyes, a fluffy thorax and a glowing tail of two segments - two pairs of see-through wings veined with light
# (each forewing hinged in two so it flexes as it beats, pink eye-spots), feathery antennae with beads of light
# and two pairs of tiny legs.
NIB_PAL = {
    'glow': '#fff3b0', 'glow_l': '#fffbe6', 'glow_d': '#ffd36b',
    'fluff': '#fff4d0', 'fluff_l': '#ffffff', 'fluff_d': '#f0d084',
    'wing': '#9ff4ff', 'wing_l': '#dafcff', 'wing_d': '#62c9f0', 'edge': '#58bdea', 'vein': '#ecfeff',
    'spot': '#ff9be3', 'spot_l': '#ffd8f4', 'spot_d': '#d860b8',
    'eye': '#2c1838', 'eye_l': '#ffffff', 'leg': '#f0bd56', 'leg_d': '#c48a2c',
}
NIB_MATERIALS = {'fluff': 'fur', 'leg': 'chitin'}


def _nib_wing(w, h, hind=False):
    """A Nib's wing laid flat (hd map, one char per texel): columns run from the root (0) out to the tip, rows
    from the back (0) to the front - the way the up and down faces of a flat plane read. '_' is cut away round
    the wing; c is the see-through wing itself, e its rim, v the veins of light fanning from the root, w the pale
    root, s / S a pink eye-spot with its pale heart."""
    def inside(u, v):          # u 0 root .. 1 tip, v 0 front .. 1 back
        if hind:
            return ((u - 0.48) / 0.52) ** 2 + ((v - 0.42) / 0.5) ** 2 < 1.0 and not (u < 0.12 and abs(v - 0.3) > 0.25)
        return 0.42 - 0.38 * u <= v <= 0.6 + 0.36 * u and u <= 1.0 - 0.3 * v and not (u > 0.82 and v < 0.12 and (u - 0.82) > v * 1.2)

    root = (0.0, 0.35 if hind else 0.5)
    tips = [(0.95, 0.2), (0.75, 0.85)] if hind else [(1.0, 0.08), (0.9, 0.45), (0.7, 0.85)]
    spot = (0.62, 0.45, 1.3) if hind else (0.7, 0.32, 1.5)
    g = [['_'] * w for _ in range(h)]
    cells = {}
    for y in range(h):
        for x in range(w):
            u, v = (x + 0.5) / w, 1.0 - (y + 0.5) / h
            cells[(x, y)] = inside(u, v)
    for (x, y), on in cells.items():
        if not on:
            continue
        u, v = (x + 0.5) / w, 1.0 - (y + 0.5) / h
        rim = any(not cells.get((x + dx, y + dy), False) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        ch = 'e' if rim else 'w' if u < 0.22 else 'c'
        for tx, tv in tips:
            # distance (in texels) from the vein running from the root out to this tip
            ax, ay = root[0] * w, (1.0 - root[1]) * h
            bx, by = tx * w, (1.0 - tv) * h
            px, py = x + 0.5, y + 0.5
            t = max(0.0, min(1.0, ((px - ax) * (bx - ax) + (py - ay) * (by - ay)) / ((bx - ax) ** 2 + (by - ay) ** 2)))
            if math.hypot(px - (ax + t * (bx - ax)), py - (ay + t * (by - ay))) < 0.5 and not rim and u < 0.85:
                ch = 'v'
        d = math.hypot(x + 0.5 - spot[0] * w, y + 0.5 - (1.0 - spot[1]) * h)
        if d < spot[2] * 0.55:
            ch = 'S'
        elif d < spot[2] and not rim:
            ch = 's'
        g[y][x] = ch
    return [''.join(r) for r in g]


def nib() -> Model:
    m = Model('nib', (32, 32), dict(NIB_PAL), {'nib': {}}, res=2, materials=NIB_MATERIALS)
    m.exact_uv = True
    # the wings: see-through (alpha 150) and glowing texel by texel through glow_keys, so the cut-away corners
    # stay dark in the glow layer too (a glowing face would light its whole rectangle there)
    wk = {'v': 'vein', 's': 'spot', 'S': 'spot_l', 'w': 'wing_l', 'e': 'edge'}
    see = lambda key: tuple(int(NIB_PAL[key][i:i + 2], 16) for i in (1, 3, 5)) + (150,)
    body = m.part('body', pivot=(0, 21, 0))
    # the thorax, in a collar of fluff
    body.cube((-0.75, -0.75, -1), (1.5, 1.5, 2), **mc('glow', clusters=0.0, rim=False, glow=True))
    body.cube((-1, -1, -1.5), (2, 1.5, 1), **mc('fluff', clusters=0.2, streaks=1.0, rim=False, glow=True))
    head = body.part('head', pivot=(0, -0.25, -1.5))
    # (a face whose map has dark texels is painted without the face glow, so its glow layer has no light under
    # the eyes; every other texel glows through glow_keys)
    head.cube((-1, -1, -1.5), (2, 1.5, 1.5), **mc('glow_l', clusters=0.0, rim=False, glow=True), faces={
        'north': dict(mc('glow_l', clusters=0.0, rim=False), hd=True, map=['hLLh', 'ELLE', 'LddL'],
                      keys={'h': 'eye_l', 'E': 'eye', 'd': 'glow_d', 'L': 'glow_l'}, glow_keys='hdL')})
    for side, sx in (('left', 1), ('right', -1)):
        ant = head.part(f'{side}_antenna', pivot=(0.5 * sx, -1, -1.0), rot=(-0.55, 0, 0.35 * sx))
        feather = ['.f', 'ff', '.f', 'ff', '.f']
        ant.cube((0, -2.5, -0.5), (0, 2.5, 1), color='glow_d', pattern='mc', clusters=0.0, rim=False, glow=True, faces={
            'east': dict(mc('glow_d', clusters=0.0, rim=False, glow=True), hd=True, map=feather, keys={'f': 'glow'}),
            'west': dict(mc('glow_d', clusters=0.0, rim=False, glow=True), hd=True, map=flip(feather), keys={'f': 'glow'})})
        bead = ant.part(f'{side}_antenna_tip', pivot=(0, -2.5, 0), rot=(-0.4, 0, 0))
        bead.cube((-0.5, -1, -0.5), (1, 1, 1), **mc('glow_d', clusters=0.0, rim=False, glow=True), faces={
            'north': dict(mc('glow_d', clusters=0.0, rim=False, glow=True), hd=True, map=['l.', '..'], keys={'l': 'glow_l'})})
    # the glowing tail: two segments, the tip brightest
    tail = body.part('tail', pivot=(0, 0, 1), rot=(-0.1, 0, 0))
    tail.cube((-0.75, -0.75, 0), (1.5, 1.5, 2), **mc('glow', clusters=0.0, rim=False, glow=True), faces={
        'up': dict(mc('glow', clusters=0.0, rim=False, glow=True), hd=True, map=['...', 'ddd', '...', 'ddd'], keys={'d': 'glow_d'}, glow_keys='d'),
        'east': dict(mc('glow', clusters=0.0, rim=False, glow=True), hd=True, map=['.d.d', '.d.d', '.d.d'], keys={'d': 'glow_d'}, glow_keys='d'),
        'west': dict(mc('glow', clusters=0.0, rim=False, glow=True), hd=True, map=['d.d.', 'd.d.', 'd.d.'], keys={'d': 'glow_d'}, glow_keys='d')})
    tip = tail.part('tail_tip', pivot=(0, 0, 2), rot=(0.15, 0, 0))
    tip.cube((-0.5, -0.5, 0), (1, 1, 1.5), **mc('glow_l', clusters=0.0, rim=False, glow=True))
    # two pairs of tiny legs under the thorax
    for nm, z, r in (('front_legs', -0.5, -0.35), ('hind_legs', 0.5, 0.35)):
        legs = body.part(nm, pivot=(0, 0.75, z), rot=(r, 0, 0))
        for lx in (-0.75, 0.25):
            legs.cube((lx, 0, -0.25), (0.5, 1, 0.5), **mc('leg_d', clusters=0.0, rim=False))
    # the wings: see-through, veined with light; each forewing in two panels so its tip flexes behind the beat
    fore = _nib_wing(12, 12)
    hind = _nib_wing(8, 8, hind=True)
    for side, sx in (('left', 1), ('right', -1)):
        def mir(rows):
            return rows if sx > 0 else flip(rows)

        def plane(part, x0, size, rows, color):
            keys = {k: see(v) for k, v in wk.items()}
            keys['c'] = see(color)
            spec = dict(mc(color, clusters=0.0, rim=False, opacity=150), hd=True, map=mir(rows), keys=keys, glow_keys='cevwsS')
            part.cube((x0, 0, size[2][0]), (size[0], 0, size[2][1]), **mc(color, clusters=0.0, rim=False, opacity=150),
                      faces={'up': spec, 'down': spec})
        wing = body.part(f'{side}_wing', pivot=(0.6 * sx, -0.6, -0.5), rot=(0, 0, -0.3 * sx))
        inner = [r[:6] for r in fore]
        outer = [r[6:] for r in fore]
        plane(wing, 0 if sx > 0 else -3, (3, 0, (-4.5, 6)), inner, 'wing')
        wtip = wing.part(f'{side}_wing_tip', pivot=(3 * sx, 0, 0))
        plane(wtip, 0 if sx > 0 else -3, (3, 0, (-4.5, 6)), outer, 'wing')
        low = body.part(f'{side}_wing_low', pivot=(0.5 * sx, -0.25, 0.5), rot=(0, 0, -0.2 * sx))
        plane(low, 0 if sx > 0 else -4, (4, 0, (-1, 4)), hind, 'wing_d')
    return m


# =========================================================================== spawn eggs (16x16, vanilla's per-mob style)
# Each egg is the creature's own little portrait on an egg silhouette: the egg in the mob's main colour, lit
# from the top left like every vanilla item, outlined in its darkest tone, the mob's features drawn over it
# and spilling past its edge where they stick out (antlers, wings, claws, a lantern).

class EggCanvas:
    """A 16x16 canvas: put(x, y, key) by palette key; the egg body is shaded in three tones from the top left
    and outlined; render(pal) gives the PIL sprite."""

    def __init__(self, top=1.4, bottom=15.0, half=6.1, cx=7.5):
        self.g = [['.'] * 16 for _ in range(16)]
        cy = (top + bottom) / 2.0
        ry = (bottom - top) / 2.0
        for y in range(16):
            for x in range(16):
                v = (y + 0.5 - cy) / ry
                if abs(v) >= 1.0:
                    continue
                w = half * math.sqrt(1.0 - v * v) * (0.86 + 0.14 * (v + 1.0) / 2.0)
                if abs(x + 0.5 - (cx + 0.5)) <= w:
                    self.g[y][x] = 'B'
        self.body = {(x, y) for y in range(16) for x in range(16) if self.g[y][x] == 'B'}

    def inside(self, x, y):
        return (x, y) in self.body

    def put(self, x, y, key):
        if 0 <= x < 16 and 0 <= y < 16:
            self.g[y][x] = key

    def sym(self, x, y, key):
        """Puts a texel and its mirror image across the middle."""
        self.put(x, y, key)
        self.put(15 - x, y, key)

    def render(self, pal, outline):
        from PIL import Image
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        px = img.load()
        lit, mid, dark = pal['B']
        for y in range(16):
            for x in range(16):
                k = self.g[y][x]
                if k == '.':
                    continue
                if k == 'B':
                    if x + y < 12 and x < 9 and y < 9:
                        c = lit
                    elif x + y > 19 or x > 12 or y > 12:
                        c = dark
                    else:
                        c = mid
                else:
                    c = pal[k]
                px[x, y] = _rgb(c) + (255,)
        # a dark outline round the egg's silhouette (and round the features listed in pal['ol']); thin features
        # that stick out past the egg keep their own colours
        filled = {(x, y) for y in range(16) for x in range(16) if self.g[y][x] != '.'}
        for (x, y) in list(filled):
            if self.g[y][x] not in 'B' + pal.get('ol', ''):
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + dx, y + dy) not in filled and 0 <= x + dx < 16 and 0 <= y + dy < 16:
                    px[x, y] = _rgb(outline) + (255,)
                    break
        return img


def _rgb(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def ascii_egg(half_rows, pal, body=('B',), mirror=True):
    """A 16x16 sprite from rows of palette keys (the left halves, mirrored, unless mirror=False); every key in
    `body` is a three-tone body colour (pal[key] = (lit, mid, dark)) shaded from the top left like vanilla."""
    from PIL import Image
    rows = [h + h[::-1] for h in half_rows] if mirror else list(half_rows)
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, r in enumerate(rows):
        for x, k in enumerate(r):
            if k == '.':
                continue
            c = pal[k]
            if k in body:
                lit, mid, dark = c
                c = lit if (x + y < 12 and x < 9 and y < 10) else dark if (x + y > 20 or x > 12 or y > 12) else mid
            px[x, y] = _rgb(c) + (255,)
    return img


def _rgb(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def echoer_egg():
    """The Echoer: its teal head with the pale bone mask, eyes of light in dark sockets, a glowing tear-line,
    pink-lined leaf ears, the dark nose, the great pale antlers branching off the top with glowing tips, and
    its golden star hanging below."""
    return ascii_egg([
        '..T.T...',
        'T.t.t...',
        '.tttt...',
        '....ttaa',
        '.....aMM',
        '.aaaaBMM',
        'appBBmmm',
        '.aaakemm',
        '...aBBmm',
        '...aBgmm',
        '....aBmm',
        '....aBnm',
        '.....anm',
        '.....aoo',
        '......aS',
        '.......a',
    ], {'B': ('#5cc4b3', '#2e9286', '#1d675e'), 'a': '#0f3a40', 'm': '#e9e2cc', 'M': '#fffbf0', 'n': '#b2a890', 'e': '#a6f8ff',
        'k': '#0c1b24', 'g': '#8ff6ff', 'o': '#22383f', 't': '#ece5cf', 'T': '#8ff6ff', 'p': '#ecb4c8', 'S': '#f3d27a'})


def golem_egg():
    """CAVE v4: the Soul Golem, the copper mole, face on: a low riveted copper mound with patina in its seams,
    small dark button eyes, the copper drill of its nose, the lightning-rod antenna with its soul lamp on top
    and the great spade forepaws spread at its sides, polished claws below."""
    return ascii_egg([
        '......aL',
        '......aL',
        '.......k',
        '.......k',
        '...aaaaB',
        '..aBrBBB',
        '.aBBiBBB',
        '.aBpeeDD',
        'aBBBBBgD',
        'aBrBBBDD',
        'aBBBBBgD',
        'aPPPaBTT',
        'aPPPPaBB',
        'acPcPcaa',
        '.c.c.c..',
        '........',
    ], {'B': ('#e3826c', '#c87456', '#9a5038'), 'P': ('#c87456', '#9a5038', '#7a3e2a'), 'a': '#4a2418', 'L': '#5fe9ff',
        'k': '#e3826c', 'r': '#f0a888', 'e': '#1c1012', 'i': '#d6fdff', 'p': '#59b292', 'D': '#d98a68', 'g': '#4e2418', 'T': '#f6b896',
        'c': '#f2c9a8'}, body=('B', 'P'))


def nib_egg():
    """The Nib: a little glowing cream body with two bead eyes and a bright tail tip, its see-through cyan
    wings (veined with light, spotted pink) spread wide, and feathery antennae tipped with beads of light."""
    return ascii_egg([
        '..A.....',
        '...a....',
        '....a...',
        '.OO..aOO',
        'OwwO.OBB',
        'OwswOOkB',
        'OwSwwOBB',
        'OwvwwwOB',
        '.OwvwwOB',
        '..OOwOBB',
        '..OwwOBB',
        '.OwswwOB',
        '.OwwwOOB',
        '..OOO.OB',
        '......OG',
        '.......O',
    ], {'B': ('#fffbe6', '#fff3b0', '#ffd36b'), 'O': '#2f6e9c', 'w': '#9ff4ff', 'v': '#ecfeff', 's': '#ff9be3', 'S': '#ffd8f4',
        'k': '#2c1838', 'a': '#ffd36b', 'A': '#fffbe6', 'G': '#fffbe6'})


def caravan_egg():
    """The Caravan: a crab - its brown gem-crusted shell with amber crystals growing on top, eyes on stalks,
    the pale belly and big claws held out at its sides, legs below."""
    return ascii_egg([
        '.......G',
        '..e...gG',
        '..s..gGg',
        '..saaagg',
        '..aBBkBB',
        '.aBBBBBk',
        'aBBkBBBB',
        'adddddDd',
        'CCappppp',
        'CcCapppp',
        'CCCappjp',
        '.CCaapp.',
        '..l.aaaa',
        '.l..l...',
        'l..l....',
        '........',
    ], {'B': ('#8c603e', '#6a452e', '#3c261a'), 'a': '#1e120a', 'k': '#c0681a', 'G': '#ffe08a', 'g': '#ffb43a', 'e': '#ffb43a',
        's': '#3c261a', 'd': '#3c261a', 'D': '#ffb43a', 'p': '#d2a878', 'j': '#a88058', 'C': '#5a3a24', 'c': '#ffe08a', 'l': '#4a3020'})


def caravan_queen_egg():
    """The Caravan Queen: the great stone whorl of her shell with its spiral, gems crusting its crown, the
    pearly arch of its mouth with her soft body in it, her eyes on their stalks and her claws below."""
    return ascii_egg([
        '.....g.G',
        '...gGg.g',
        '..aaaaaa',
        '.aBBBBBB',
        'aBBrrrrB',
        'aBrBBBrB',
        'aBrBrrBB',
        'aBrBBrBk',
        'aBBrrrBB',
        'aBBBBnnn',
        'aBBBnhhh',
        'aaanffff',
        'CCaesfff',
        'CcCaffff',
        '.CCaafff',
        '....aaaa',
    ], {'B': ('#958b84', '#7a716c', '#544c49'), 'a': '#2a2422', 'r': '#544c49', 'k': '#ffb43a', 'g': '#ffb43a', 'G': '#ffe08a',
        'n': '#f6ecee', 'h': '#2a2024', 'f': '#c99e84', 'e': '#140c1c', 's': '#6a452e', 'C': '#6a452e', 'c': '#ffe08a'})


def caravan_larva_egg():
    """The Caravan larva: a little grub curled round on itself - shelled segments in the caravan's brown, a
    glowing pore on each, two gem nubs, glowing pinprick eyes and pale snapping mandibles."""
    return ascii_egg([
        '................',
        '.....aaaaa......',
        '...aaBBBBBaa....',
        '..aBBgBBBgBBa...',
        '.aBBaaaaaaaBBa..',
        '.aBa.......aBa..',
        'aBBa..aaa..aBBa.',
        'aBga.aBBBa.agBa.',
        'aBBa.aKBBa.aBBa.',
        'aBBa.aaeBea.aBa.',
        '.aBBa.aBBBmamBa.',
        '.aBgBaaaaaa.aa..',
        '..aBBBgBBBBa....',
        '...aaBBBBBa.....',
        '.....aaaaa......',
        '................',
    ], {'B': ('#a87448', '#8a5a36', '#4a2e1a'), 'a': '#22140a', 'g': '#ffb43a', 'K': '#ffe08a', 'e': '#ffb43a', 'm': '#d2a878'},
        mirror=False)


ALL = {'soul_golem': soul_golem, 'nib': nib}
POSES = {}

# --------------------------------------------------------------------------- sounds (vanilla files)
SOUNDS = {
    # CAVE v4: the copper mole - the Copper Golem's own voice and steps, soil and gravel as it burrows, a copper bulb's
    # ping when it feels ore
    'entity.soul_golem.ambient': [('event:entity.copper_golem.spin', 0.4, 1.3), ('block/amethyst/resonate1', 0.35, 1.6)],
    'entity.soul_golem.hurt': [('event:entity.copper_golem.hurt', 1.0, 1.1)],
    'entity.soul_golem.death': [('event:entity.copper_golem.death', 1.0, 1.0)],
    'entity.soul_golem.step': [('event:entity.copper_golem.step', 0.6, 1.2)],
    'entity.soul_golem.dig': [('event:entity.sniffer.digging', 0.6, 1.4), ('event:block.rooted_dirt.break', 0.7, 1.1)],
    'entity.soul_golem.burrow': [('event:block.rooted_dirt.break', 1.0, 0.8), ('event:block.gravel.break', 0.8, 0.9)],
    'entity.soul_golem.emerge': [('event:block.mud.break', 1.0, 1.0), ('event:entity.copper_golem.spawn', 0.6, 1.3)],
    'entity.soul_golem.ping': [('event:block.copper_bulb.turn_on', 1.0, 1.5), ('block/amethyst/resonate2', 0.6, 1.8)],
    'entity.soul_golem.deliver': [('event:entity.copper_golem.item_drop', 1.0, 1.0)],
    'entity.soul_golem.find': [('block/amethyst/shimmer', 1.0, 1.4), ('mob/allay/item_given1', 0.8, 1.3)],
    'entity.soul_golem.slump': [('block/beacon/deactivate', 0.6, 1.6)],
    'entity.soul_golem.recharge': [('block/beacon/power2', 0.6, 1.8), ('block/amethyst/resonate2', 0.8, 1.5)],
    'entity.nib.ambient': [('block/amethyst/shimmer', 0.25, 1.9), ('mob/allay/idle_without_item2', 0.15, 1.9)],
    'entity.nib.hurt': [('block/amethyst_cluster/break2', 0.5, 1.9)],
    'entity.nib.transform': [('block/amethyst/resonate4', 1.0, 1.4), ('mob/allay/item_thrown1', 0.8, 1.5)],
    'block.echoer_device.charge': [('mob/warden/sonic_charge1', 0.7, 1.4), ('mob/warden/sonic_charge2', 0.7, 1.5)],
    'block.echoer_device.fire': [('mob/warden/sonic_boom1', 0.7, 1.6), ('mob/warden/sonic_boom2', 0.7, 1.7)],
    'block.echoer_device.fizzle': [('block/amethyst_cluster/break3', 0.8, 0.8)],
    **__import__('echoer_drill').SOUNDS,  # RR: the Echoer Drill hears a beat, bites into the rock
    # M3 Echoer, a deer spirit: its endless song is a breathy flute pitched note by note (played at volume 3: heard 48 blocks
    # off), a soft glassy chime under each hoof as it skips on the air, a resonant shimmer as a gift rises from its antlers
    'entity.enchoer.chimes': [('block/note_block/chime', 0.22, 1.5), ('block/note_block/chime', 0.22, 1.9)],
    'entity.enchoer.sing': [('block/note_block/flute', 0.8, 1.0)],
    'entity.enchoer.step': [('block/amethyst/step5', 0.5, 1.6), ('block/note_block/chime', 0.12, 2.0)],
    'entity.enchoer.gift': [('block/amethyst/resonate2', 1.0, 1.0), ('mob/allay/item_given1', 0.8, 0.8), ('block/amethyst/shimmer', 1.0, 1.1)],
    'entity.enchoer.ambient': [('mob/allay/idle_without_item1', 0.6, 0.6), ('mob/allay/idle_without_item2', 0.6, 0.55),
                               ('block/amethyst/resonate1', 0.5, 0.8)],
    'entity.enchoer.hurt': [('mob/allay/hurt1', 0.9, 0.6), ('block/amethyst_cluster/break1', 0.8, 0.9)],
    'entity.enchoer.death': [('mob/allay/death1', 1.0, 0.5), ('block/amethyst_cluster/break2', 1.0, 0.6)],
}
SUBTITLES = {
    'entity.soul_golem.ambient': 'Soul Golem hums', 'entity.soul_golem.hurt': 'Soul Golem chips', 'entity.soul_golem.death': 'Soul Golem crumbles',
    'entity.soul_golem.step': 'Soul Golem clanks', 'entity.soul_golem.dig': 'Soul Golem digs', 'entity.soul_golem.find': 'Soul Golem finds something',
    'entity.soul_golem.slump': 'Soul Golem runs down', 'entity.soul_golem.recharge': 'Soul Golem recharges',
    'entity.soul_golem.burrow': 'Soul Golem burrows', 'entity.soul_golem.emerge': 'Soul Golem digs its way out',
    'entity.soul_golem.ping': 'Soul Golem senses ore', 'entity.soul_golem.deliver': 'Soul Golem puts a find away',
    'entity.nib.ambient': 'Nib twinkles', 'entity.nib.hurt': 'Nib flickers', 'entity.nib.transform': 'Nib turns to treasure',
    'entity.enchoer.ambient': 'Echoer calls', 'entity.enchoer.hum': 'Echoer hums', 'entity.enchoer.trade': 'Echoer waits, humming',
    'entity.enchoer.yes': 'Echoer answers your song', 'entity.enchoer.no': 'Echoer sighs', 'entity.enchoer.hurt': 'Echoer hurts',
    'entity.enchoer.death': 'Echoer fades', 'entity.enchoer.chimes': 'Echoer\'s antlers chime', 'entity.enchoer.sing': 'Echoer sings',
    'entity.enchoer.step': 'Echoer treads on the air', 'entity.enchoer.gift': 'A gift rises from the Echoer\'s antlers',
    'block.echoer_device.charge': 'Echoer Drill listens', 'block.echoer_device.fire': 'Echoer Drill fires', 'block.echoer_device.fizzle': 'Echoer Drill jams',
    **__import__('echoer_drill').SUBTITLES,  # RR
}

NS = 'thesift'
