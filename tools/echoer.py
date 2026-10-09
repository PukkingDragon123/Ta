"""A2 Echoer: the Echoer (entity id `enchoer`), Soul Golems and Nibs - model geometry and paint
(see modelkit.py), their sound events, and the data/lang that goes with them (hooked from
mobs.ALL, gen_assets and gen_data)."""
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


# =========================================================================== THE ECHOER
# CR1 Echoer: a speaker-bat. Its furry body is a loudspeaker - a plum baffle with a big pulsing woofer
# in its chest - and it flies on bat wings with a little speaker cone in each membrane. Its ears are
# tweeters, its snout a spiralling brass drill that whirrs round when it sings, and its tail is a
# coiled cable ending in a jack plug. Glowing cyan eyes, cone rims and a groove up the drill.
ECHOER_PAL = {
    'fur': '#e6dcef', 'fur_l': '#faf6ff', 'fur_d': '#b8a8cf',
    'ruff': '#d2c0ea', 'ruff_l': '#efe5fb', 'ruff_d': '#a48ec8',
    'cabinet': '#4b3b66', 'cabinet_l': '#6c5a8c', 'cabinet_d': '#2e2443',
    'grille': '#2b2240', 'grille_l': '#43375c', 'grille_d': '#1c1630',
    'cone': '#3a3050', 'cone_l': '#5e507c', 'cone_d': '#241c34',
    'surround': '#1d1829', 'surround_l': '#3a3150',
    'glow': '#8ff6ff', 'glow_d': '#3fc4dc', 'glow_l': '#e4ffff',
    'brass': '#e0b25a', 'brass_l': '#fff0b4', 'brass_d': '#a5742c', 'groove': '#6e4c1e',
    'wing': '#b7a2e0', 'wing_l': '#d2c3f2', 'wing_d': '#8e78bf', 'vein': '#7d68ac', 'bone': '#e6dcef', 'bone_d': '#c8b8e0',
    'ear_in': '#f0bfd4', 'ear_in_d': '#cf90ad',
    'eye': '#bffcff', 'eye_core': '#ffffff', 'pupil': '#1b2846', 'lid': '#7b6a9e', 'lid_d': '#4e4170',
    'mouth': '#3a2848', 'fang': '#ffffff', 'claw': '#43385a', 'cable': '#2f2840', 'cable_l': '#4d4466',
}
ECHOER_MATERIALS = {'fur': 'fur', 'ruff': 'fur', 'cabinet': 'wood', 'grille': 'cloth', 'cone': 'cloth', 'surround': 'flat', 'brass': 'metal',
                    'wing': 'membrane', 'claw': 'chitin', 'cable': 'skin', 'bone': 'bone'}
ECHOER_EXPRS = ['blink', 'happy', 'sleep', 'hurt', 'dead']


def _disc(n, keys='scC', cap=0.0, rim_glow=True):
    """A round speaker cone on an n x n face, seen head on: '_' outside it (cut away), a dark rubber
    surround (s), a glowing rim line (g), the paper cone in rings (c, C) and a dust cap (k) in the middle."""
    import math
    rows = []
    c = (n - 1) / 2.0
    R = n / 2.0
    for y in range(n):
        row = ''
        for x in range(n):
            d = math.hypot(x - c, y - c) / R
            if d > 1.0:
                row += '_'
            elif d > 0.82:
                row += 's'
            elif d > 0.72 and rim_glow:
                row += 'g'
            elif cap and d < cap:
                row += 'k' if d < cap * 0.6 else 'K'
            else:
                ring = int(d * n * 0.5) % 3
                row += 'C' if ring == 0 else 'c'
        rows.append(row)
    return rows


def _ear(w, h):
    """A bat ear's front, w x h texels: pointed tip ('_' cut away round it), fur rim (f), pink inside (p, P)."""
    rows = []
    for y in range(h):
        row = ''
        t = y / (h - 1)                  # 0 at the tip, 1 at the base
        half = (w / 2.0) * min(1.0, 0.25 + t * 1.15)
        for x in range(w):
            u = abs(x + 0.5 - w / 2.0)
            if u > half:
                row += '_'
            elif u > half - 1.2 or y == 0:
                row += 'f'
            else:
                row += 'P' if (x + y) % 5 == 0 else 'p'
        rows.append(row)
    return rows


def _membrane(w, h, seed, fingers, scallop=3, taper=0.0):
    """A bat-wing membrane (w x h texels, row 0 the trailing edge): veins (v) fanning back from the
    leading edge, bone struts (b) along the fingers, the trailing edge cut into scallops between them."""
    import math
    import random
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    # fingers: straight struts from the joint (front, x=0) out to points on the trailing edge
    tips = [int(w * f) for f in fingers]
    for tx in tips:
        for y in range(h):
            t = 1.0 - y / (h - 1)            # 0 at the front edge, 1 at the trailing edge
            x = int(round(tx * t))
            if 0 <= x < w:
                g[y][x] = 'b'
    # veins: thin wandering lines from the front edge backwards
    for _ in range(max(2, w // 5)):
        x = rnd.randrange(w)
        for y in range(h - 1, 0, -1):
            if rnd.random() < 0.35:
                x += rnd.choice((-1, 1))
            if 0 <= x < w and g[y][x] == '.':
                g[y][x] = 'v'
    # a hand's membrane narrows towards the wing tip
    if taper:
        for x in range(w):
            cut = int(taper * h * (x / (w - 1)) ** 1.4)
            for y in range(min(cut, h - 2)):
                g[y][x] = '_'
    # scallops along the trailing edge, between the finger tips
    edges = [0] + tips + [w - 1]
    for a, b in zip(edges, edges[1:]):
        span = max(1, b - a)
        for x in range(a, b + 1):
            k = math.sin((x - a) / span * math.pi)
            depth = int(round(k * scallop))
            top = next((y for y in range(h) if g[y][x] != '_'), h)
            for y in range(top, min(h - 2, top + depth)):
                if 0 <= x < w and g[y][x] != 'b':
                    g[y][x] = '_'
    return [''.join(r) for r in g]


def _spiral(w, h, phase):
    """A drill segment's side: diagonal brass flutes with a glowing groove (g) between them."""
    rows = []
    for y in range(h):
        row = ''
        for x in range(w):
            k = (x + y + phase) % 6
            row += 'g' if k == 0 else 'd' if k == 1 else 'l' if k == 4 else '.'
        rows.append(row)
    return rows


def enchoer() -> Model:
    """CR1 Echoer: the speaker-bat (see the notes above). Model space: it hovers with its body over
    y = 14; folded up on the ground (asleep) its feet touch y = 24."""
    from mobs_wild import eye
    m = Model('enchoer', (128, 128), dict(ECHOER_PAL), {'enchoer': {}}, res=2, expressions=ECHOER_EXPRS, materials=ECHOER_MATERIALS)
    fur = dict(color='fur', pattern='mc', clusters=0.15, streaks=0.8)
    cone_keys = {'s': 'surround', 'g': 'glow', 'c': 'cone', 'C': 'cone_l', 'k': 'glow_l', 'K': 'glow'}

    # ---- the body: a furry loudspeaker, its baffle the chest
    body = m.part('body', pivot=(0, 14, 0))
    grille = ['g' + 'G.' * 9 + 'g'] * 2
    baffle = hd_rows({0: 'B' * 20, 1: 'b' * 20, 20: 'b' * 20, 21: 'B' * 20}, 20, 22)
    body.cube((-5, -6, -4), (10, 11, 8), **fur, faces={
        'north': dict(color='grille', pattern='mc', clusters=0.1, rim=False, hd=True, map=baffle,
                      keys={'B': 'brass', 'b': 'brass_d', 'g': 'grille_l', 'G': 'grille_d'}),
        'down': dict(color='fur_d', pattern='mc', clusters=0.1, streaks=0.5),
    })
    # brass corner caps on the baffle, like a speaker cabinet's
    for i, (cx, cy) in enumerate(((-5, -6), (4, -6), (-5, 4), (4, 4))):
        body.cube((cx - 0.25, cy - 0.25, -4.5), (1.5, 1.5, 1.5), **mc('brass', clusters=0.0, rim=False))
    # the woofer: a big round cone in the chest that pumps with the sound
    woofer = body.part('woofer', pivot=(0, -0.5, -4.1))
    woofer.cube((-4, -4, -0.6), (8, 8, 1), color='cone', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='cone', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(16), keys=cone_keys, glow_keys='g'),
        'up': dict(skip=True), 'down': dict(skip=True), 'east': dict(skip=True), 'west': dict(skip=True), 'south': dict(skip=True)})
    cap = woofer.part('woofer_cap', pivot=(0, 0, -0.6))
    cap.cube((-1.5, -1.5, -1), (3, 3, 1), **mc('glow', clusters=0.0, rim=False, glow=True), faces={
        'north': dict(color='glow', pattern='mc', clusters=0.0, rim=False, glow=True, hd=True, map=_disc(6, rim_glow=False),
                      keys={'_': 'glow_d', 's': 'glow_d', 'c': 'glow', 'C': 'glow_l'})})
    # a fluffy ruff round its neck and shoulders, and a fur cap over the top of the cabinet
    body.cube((-5.5, -7.5, -4.5), (11, 2, 9), **mc('ruff', clusters=0.1, streaks=1.1, fringe=1, rim=False))
    tuft = body.part('tuft', pivot=(0, 5, -1))
    tuft.cube((-3.5, 0, -3), (7, 2, 7), **mc('ruff', clusters=0.1, streaks=1.0, fringe=2, rim=False))

    # ---- the head: a round bat face with big glowing eyes
    head = body.part('head', pivot=(0, -7, -1))
    ek = {'r': 'lid', 'i': 'eye', 'I': 'glow_d', 'p': 'pupil', 'h': 'eye_core', 'l': 'lid', 'd': 'lid_d', 'f': 'fur', 'F': 'fur_l'}

    def face(expr):
        g = [list('F' * 16)] + [list('f' * 16) for _ in range(13)]
        for x0, mirror in ((1, False), (9, True)):
            e = eye(6, 6, expr, mirror, pupil='dot', rim=0.62)
            for j, row in enumerate(e):
                for i, ch in enumerate(row):
                    if ch != '.':
                        g[2 + j][x0 + i] = ch
        return [''.join(r) for r in g]
    head.cube((-4, -7, -3.5), (8, 7, 6), **dict(fur, streaks=0.5), faces={
        'north': dict(color='fur', pattern='mc', clusters=0.0, rim=False, hd=True, map=face(''), keys=ek, glow_keys='iIh',
                      expr={x: face(x) for x in ECHOER_EXPRS})})
    head.cube((-4.5, -3.5, -3.6), (9, 3.5, 4), **mc('ruff', clusters=0.0, streaks=1.0, fringe=1, rim=False))
    jaw = head.part('jaw', pivot=(0, -0.5, -2.5))
    jaw.cube((-2, 0, -1.5), (4, 1, 2), **mc('fur_d', clusters=0.0, rim=False), faces={
        'north': dict(color='mouth', pattern='mc', clusters=0.0, rim=False, hd=True, map=['FmmmmmmF', 'mmmmmmmm'], keys={'F': 'fang', 'm': 'mouth'}),
        'up': dict(color='mouth', pattern='mc', clusters=0.0, rim=False)})
    # the drill: a brass snout of five twisted segments that spins round when it sings
    drill = head.part('drill', pivot=(0, -2.5, -3.6))
    sizes = [(3.5, 2), (3, 2), (2.5, 2), (1.75, 2), (1, 2)]
    parent = drill
    z = 0.0
    for i, (w, d) in enumerate(sizes):
        seg = parent if i == 0 else parent.part(f'drill_{i}', pivot=(0, 0, -z), rot=(0, 0, 0.45))
        z = d
        side = dict(color='brass', pattern='mc', clusters=0.0, rim=False, hd=True, map=_spiral(int(w * 2 + 1), d * 2, i * 2),
                    keys={'g': 'glow', 'd': 'groove', 'l': 'brass_l'}, glow_keys='g')
        tip = dict(color='glow', pattern='mc', clusters=0.0, rim=False, glow=True) if i == len(sizes) - 1 else mc('brass_d', clusters=0.0, rim=False)
        seg.cube((-w / 2, -w / 2, -d), (w, w, d), **mc('brass', clusters=0.0, rim=False), faces={
            'up': side, 'down': side, 'east': side, 'west': side, 'north': tip, 'south': mc('brass_d', clusters=0.0, rim=False)})
        parent = seg
    # the ears: tall bat ears, each a tweeter - a little cone that pulses near its tip
    for side_name, sx in (('left', 1), ('right', -1)):
        ear = head.part(f'{side_name}_ear', pivot=(2.8 * sx, -6.5, -0.5), rot=(-0.15, -0.3 * sx, 0.5 * sx))
        ear.cube((-2.5, -7, -0.5), (5, 7, 1), color='fur', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='ear_in', pattern='mc', clusters=0.0, rim=False, hd=True, map=_ear(10, 14),
                          keys={'f': 'fur', 'p': 'ear_in', 'P': 'ear_in_d'}),
            'south': dict(color='fur', pattern='mc', clusters=0.1, streaks=0.6, rim=False, hd=True, map=[r.replace('p', 'f').replace('P', 'f') for r in _ear(10, 14)],
                          keys={'f': 'fur'}),
            'east': dict(color='fur', pattern='mc', clusters=0.0, rim=False), 'west': dict(color='fur', pattern='mc', clusters=0.0, rim=False),
            'up': dict(skip=True)})
        tw = ear.part(f'{side_name}_tweeter', pivot=(0, -2.8, -0.6))
        tw.cube((-1.5, -1.5, -0.5), (3, 3, 0.5), color='cone', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='cone', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(6, cap=0.35), keys=cone_keys, glow_keys='gkK'),
            'up': dict(skip=True), 'down': dict(skip=True), 'east': dict(skip=True), 'west': dict(skip=True), 'south': dict(skip=True)})

    # ---- the wings: an arm and a hand, membranes with a speaker cone in each. A membrane is a flat
    # plane whose two faces lie in one place, so both are painted alike (whichever the game draws last shows).
    wk = {'v': 'vein', 'b': 'bone_d'}
    for side_name, sx in (('left', 1), ('right', -1)):
        def mir(rows):
            return rows if sx > 0 else [r[::-1] for r in rows]
        wing = body.part(f'{side_name}_wing', pivot=(4.5 * sx, -5, 0), rot=(0, -0.25 * sx, -0.4 * sx))
        wing.cube((0 if sx > 0 else -9, -0.75, -1), (9, 1.5, 1.5), **mc('fur', clusters=0.0, rim=False, streaks=0.4))
        arm_mem = mir(_membrane(18, 22, 3 if sx > 0 else 4, [0.55], scallop=2))
        wing.cube((0 if sx > 0 else -9, 0, -0.5), (9, 0, 11), color='wing', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='wing', pattern='mc', clusters=0.15, rim=False, hd=True, map=arm_mem, keys=wk, map_material=True),
            'down': dict(color='wing', pattern='mc', clusters=0.15, rim=False, hd=True, map=arm_mem, keys=wk, map_material=True)})
        cone = wing.part(f'{side_name}_wing_cone', pivot=(4.6 * sx, -0.05, 5.0))
        cone.cube((-1.75, -0.4, -1.75), (3.5, 0.5, 3.5), color='cone', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='cone', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(7, cap=0.3), keys=cone_keys, glow_keys='gkK'),
            'down': dict(color='cone', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(7, cap=0.3), keys=cone_keys, glow_keys='gkK'),
            'north': dict(skip=True), 'south': dict(skip=True), 'east': dict(skip=True), 'west': dict(skip=True)})
        tip = wing.part(f'{side_name}_wing_tip', pivot=(9 * sx, 0, 0), rot=(0, -0.3 * sx, 0.75 * sx))
        tip.cube((0 if sx > 0 else -12, -0.5, -1), (12, 1, 1), **mc('bone', clusters=0.0, rim=False))
        tip.cube((11.25 if sx > 0 else -12.75, -0.5, -2), (1.5, 1, 1.5), **mc('claw', clusters=0.0, rim=False))
        tip_mem = mir(_membrane(24, 24, 7 if sx > 0 else 8, [0.28, 0.58, 0.86], scallop=3, taper=0.62))
        tip.cube((0 if sx > 0 else -12, 0, -0.5), (12, 0, 12), color='wing', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='wing', pattern='mc', clusters=0.15, rim=False, hd=True, map=tip_mem, keys=wk, map_material=True),
            'down': dict(color='wing', pattern='mc', clusters=0.15, rim=False, hd=True, map=tip_mem, keys=wk, map_material=True)})
        tcone = tip.part(f'{side_name}_tip_cone', pivot=(3.6 * sx, -0.05, 5.2))
        tcone.cube((-1.25, -0.4, -1.25), (2.5, 0.5, 2.5), color='cone', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='cone', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(5), keys=cone_keys, glow_keys='g'),
            'down': dict(color='cone', pattern='mc', clusters=0.0, rim=False, hd=True, map=_disc(5), keys=cone_keys, glow_keys='g'),
            'north': dict(skip=True), 'south': dict(skip=True), 'east': dict(skip=True), 'west': dict(skip=True)})

    # ---- little clawed feet, and a coiled cable tail ending in a brass jack plug
    for side_name, sx in (('left', 1), ('right', -1)):
        foot = body.part(f'{side_name}_foot', pivot=(2.2 * sx, 5, 1))
        foot.cube((-1, 0, -1), (2, 3, 2), **mc('fur_d', clusters=0.0, rim=False, streaks=0.5))
        foot.cube((-1.25, 3, -1.75), (2.5, 2, 2.5), **mc('claw', clusters=0.0, rim=False), faces={
            'north': mc('claw', clusters=0.0, rim=False, hd=True, map=['....', 'c.c.', 'cccc', 'CCCC'], keys={'c': 'fang', 'C': 'claw'})})
    tail = body.part('tail', pivot=(0, 3, 4), rot=(0.6, 0, 0))
    tail.cube((-0.5, -0.5, 0), (1, 1, 3), **mc('cable', clusters=0.0, rim=False))
    t1 = tail.part('tail_1', pivot=(0, 0, 3), rot=(0.5, 0, 0))
    t1.cube((-0.5, -0.5, 0), (1, 1, 3), **mc('cable', clusters=0.0, rim=False))
    t2 = t1.part('tail_2', pivot=(0, 0, 3), rot=(-0.9, 0, 0))
    t2.cube((-0.5, -0.5, 0), (1, 1, 2.5), **mc('cable', clusters=0.0, rim=False))
    plug = t2.part('plug', pivot=(0, 0, 2.5), rot=(-0.6, 0, 0))
    plug.cube((-1, -1, 0), (2, 2, 1.5), **mc('brass_d', clusters=0.0, rim=False))
    plug.cube((-0.5, -0.5, 1.5), (1, 1, 2), **mc('brass', clusters=0.0, rim=False), faces={
        'south': dict(color='glow', pattern='mc', clusters=0.0, rim=False, glow=True)})
    return m


# =========================================================================== SOUL GOLEM
GOLEM_EYE = {
    'neutral': {1: '.EEEE.', 2: 'EEIIEE', 3: 'EEIIEE', 4: '.EEEE.'},
    'blink': {3: '.llll.', 4: 'l....l'},
    'happy': {2: '.EEEE.', 3: 'E....E'},
    'sleep': {4: '.llll.'},
    'hurt': {1: 'E....E', 2: '.E..E.', 3: '..EE..', 4: '.E..E.'},
    'dead': {1: 'l....l', 2: '.l..l.', 3: '..ll..', 4: '.l..l.'},
}


def soul_golem() -> Model:
    """A small ancient construct of carved soulstone - a round frog-like body, two big domed eyes
    lit by the soul fire inside, a wide carved grin, stubby arms and legs, cyan cracks glowing
    along its seams and a soul-lamp stalk on top (the Copper Golem's lightning rod, if it were
    a lantern)."""
    pal = {
        'stone': '#6c5a4b', 'stone_l': '#87725f', 'stone_d': '#4b3e34',
        'belly': '#7d6a58', 'belly_l': '#97826d', 'belly_d': '#5c4c3f',
        'crack': '#5fe9ff', 'crack_d': '#2aa9c8', 'mouth': '#251c16',
        'eye': '#a9faff', 'eye_core': '#ffffff', 'lid': '#3a2f27',
        'lamp': '#7ff3ff', 'lamp_l': '#d6fdff', 'lamp_d': '#3cc3dc',
    }
    m = Model('soul_golem', (64, 64), pal, {'soul_golem': {}}, res=2, expressions=['blink', 'happy', 'sleep', 'hurt', 'dead'])
    ck = {'c': 'crack', 'C': 'crack_d', 'm': 'mouth'}
    front = hd_rows({1: '...c..........', 2: '...cc.........', 3: '....c.........', 7: '..mmmmmmmmmm..', 8: '...m......m...',
                     9: '....mmmmmm....', 11: '......CC......', 12: '.....CccC.....', 13: '......CC......'}, 16, 14)
    side = hd_rows({2: '.....c........', 3: '....cc........', 4: '....c....C....', 5: '.........CC...', 9: '..C...........',
                    10: '..CC..........'}, 14, 14)
    for name, (px, pz) in (('left_leg', (2, 0.5)), ('right_leg', (-2, 0.5))):
        leg = m.part(name, pivot=(px, 21, pz))
        leg.cube((-1.5, 0, -1.5), (3, 2, 3), **mc('stone', clusters=0.1, rim=False))
        leg.cube((-1.75, 2, -2.5), (3.5, 1, 4), **mc('stone_d', clusters=0.0, rim=False), faces={
            'north': dict(color='stone_d', pattern='mc', clusters=0.0, rim=False, hd=True, map=['c.c.c.c'], keys=ck, glow_keys='c'),
        })
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-4, -7, -3.5), (8, 7, 7), **mc('stone', clusters=0.25, bands=[(4, 'belly')]), faces={
        'north': dict(**mc('stone', clusters=0.0, bands=[(4, 'belly')]), hd=True, map=front, keys=ck, glow_keys='cC'),
        'west': dict(**mc('stone', clusters=0.2, bands=[(4, 'belly')]), hd=True, map=side, keys=ck, glow_keys='cC'),
        'east': dict(**mc('stone', clusters=0.2, bands=[(4, 'belly')]), hd=True, map=[r[::-1] for r in side], keys=ck, glow_keys='cC'),
    })
    # a rounder silhouette: soft bevels on the top and the sides
    body.cube((-3.5, -7.5, -3), (7, 0.5, 6), **mc('stone_l', clusters=0.2, rim=False))
    eyes = {k: hd_rows(v, 6, 6) for k, v in GOLEM_EYE.items()}
    for side_name, sx in (('left', 1), ('right', -1)):
        eye = body.part(f'{side_name}_eye', pivot=(2.2 * sx, -7, -1))
        eye.cube((-1.5, -2.5, -1.5), (3, 3, 3), **mc('stone', clusters=0.0, rim=False), faces={
            'north': dict(color='stone', pattern='mc', clusters=0.0, rim=False, hd=True, map=eyes['neutral'],
                          keys={'E': 'eye', 'I': 'eye_core', 'l': 'lid'}, glow_keys='EI',
                          expr={k: v for k, v in eyes.items() if k != 'neutral'}),
        })
        arm = body.part(f'{side_name}_arm', pivot=(4 * sx, -4.5, 0), rot=(0, 0, -0.15 * sx))
        arm.cube((0 if sx > 0 else -1.5, -0.5, -1), (1.5, 4, 2), **mc('stone', clusters=0.1, rim=False), faces={
            'down': dict(color='stone_d', pattern='mc', clusters=0.0),
        })
    stalk = body.part('stalk', pivot=(0, -7.5, 0.5))
    stalk.cube((-0.5, -3, -0.5), (1, 3, 1), **mc('stone_d', clusters=0.0, rim=False))
    lamp = stalk.part('lamp', pivot=(0, -3, 0))
    lamp.cube((-1, -2, -1), (2, 2, 2), **mc('lamp', clusters=0.3, rim=False, glow=True))
    return m


# =========================================================================== NIB
def nib() -> Model:
    """A tiny glowing wisp-butterfly: a bright little body and two pairs of see-through wings
    veined with light."""
    pal = {
        'glow': '#fff3b0', 'glow_l': '#fffbe6', 'glow_d': '#ffd36b',
        'wing': '#9ff4ff', 'wing_l': '#dafcff', 'wing_d': '#62c9f0', 'vein': '#ffffff', 'spot': '#ff9be3',
    }
    m = Model('nib', (32, 32), pal, {'nib': {}}, res=2)
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-0.5, -0.5, -1.5), (1, 1, 3), **mc('glow', clusters=0.0, rim=False, glow=True))
    body.cube((-0.75, -0.75, -2.75), (1.5, 1.5, 1.25), **mc('glow_l', clusters=0.0, rim=False, glow=True))
    wk = {'v': 'vein', 's': 'spot', 'w': 'wing_l'}
    up_wing = hd_rows({0: '......ww..', 1: '....wwvvw.', 2: '..wwvvsvw.', 3: '.wvv.ssvw.', 4: 'wvv...vvw.', 5: 'vv....vw..',
                       6: 'v....vw...', 7: 'v...vw....', 8: '.vvvw.....', 9: '..ww......'}, 10, 10)
    lo_wing = hd_rows({0: 'vvvv..', 1: 'v..vw.', 2: 'v.s.vw', 3: '.v..vw', 4: '..vvw.', 5: '...w..'}, 7, 7)
    for side, sx in (('left', 1), ('right', -1)):
        def mir(rows):
            return rows if sx > 0 else [r[::-1] for r in rows]
        wing = body.part(f'{side}_wing', pivot=(0.5 * sx, -0.5, -0.5), rot=(0, 0, -0.3 * sx))
        wspec = dict(color='wing', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True, hd=True, map=mir(up_wing), keys=wk)
        wing.cube((0 if sx > 0 else -5, 0, -4), (5, 0, 5), color='wing', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True,
                  faces={'up': wspec, 'down': wspec})
        low = body.part(f'{side}_wing_low', pivot=(0.5 * sx, -0.25, 0.5), rot=(0, 0, -0.2 * sx))
        lspec = dict(color='wing_d', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True, hd=True, map=mir(lo_wing), keys=wk)
        low.cube((0 if sx > 0 else -3.5, 0, 0), (3.5, 0, 3.5), color='wing_d', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True,
                 faces={'up': lspec, 'down': lspec})
        ant = body.part(f'{side}_antenna', pivot=(0.3 * sx, -0.75, -2.5), rot=(-0.6, 0, 0.3 * sx))
        ant.cube((0, -2, 0), (0, 2, 1), color='glow_d', pattern='mc', clusters=0.0, rim=False, glow=True)
    return m


ALL = {'soul_golem': soul_golem, 'nib': nib}
POSES = {}

# --------------------------------------------------------------------------- sounds (vanilla files)
SOUNDS = {
    'entity.soul_golem.ambient': [('block/amethyst/resonate1', 0.5, 1.6), ('block/amethyst/resonate3', 0.5, 1.7),
                                  ('mob/allay/idle_without_item1', 0.4, 1.3)],
    'entity.soul_golem.hurt': [('block/basalt/break1', 0.8, 1.3), ('block/basalt/break2', 0.8, 1.4)],
    'entity.soul_golem.death': [('block/basalt/break3', 1.0, 0.9), ('block/amethyst_cluster/break1', 0.8, 1.2)],
    'entity.soul_golem.step': [('block/basalt/step1', 0.3, 1.6), ('block/basalt/step3', 0.3, 1.7)],
    'entity.soul_golem.dig': [('item/brush/brushing_sand1', 0.6, 1.2), ('item/brush/brushing_sand2', 0.6, 1.3)],
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
    # CR1 Echoer: the speaker-bat - echolocation pings, wingbeats, the drill whirring up, and a bat's squeaks in its voice
    'entity.enchoer.chimes': [('event:block.sculk_sensor.clicking', 0.3, 1.9), ('block/note_block/chime', 0.22, 1.5),
                              ('block/note_block/chime', 0.22, 1.9)],
    'entity.enchoer.flap': [('event:entity.phantom.flap', 0.35, 1.6), ('event:entity.phantom.flap', 0.3, 1.85)],
    'entity.enchoer.drill': [('event:entity.breeze.whirl', 0.5, 1.7), ('event:block.grindstone.use', 0.22, 1.9)],
    'entity.enchoer.ambient': [('event:entity.bat.ambient', 0.5, 1.3), ('mob/allay/idle_without_item1', 0.7, 0.75),
                               ('block/amethyst/resonate1', 0.6, 0.9)],
    'entity.enchoer.hurt': [('event:entity.bat.hurt', 0.8, 1.1), ('block/amethyst_cluster/break1', 0.8, 0.9)],
    'entity.enchoer.death': [('event:entity.bat.death', 0.9, 0.9), ('block/amethyst_cluster/break2', 1.0, 0.6)],
}
SUBTITLES = {
    'entity.soul_golem.ambient': 'Soul Golem hums', 'entity.soul_golem.hurt': 'Soul Golem chips', 'entity.soul_golem.death': 'Soul Golem crumbles',
    'entity.soul_golem.step': 'Soul Golem waddles', 'entity.soul_golem.dig': 'Soul Golem digs', 'entity.soul_golem.find': 'Soul Golem finds something',
    'entity.soul_golem.slump': 'Soul Golem runs down', 'entity.soul_golem.recharge': 'Soul Golem recharges',
    'entity.nib.ambient': 'Nib twinkles', 'entity.nib.hurt': 'Nib flickers', 'entity.nib.transform': 'Nib turns to treasure',
    'entity.enchoer.ambient': 'Echoer chirps', 'entity.enchoer.hum': 'Echoer hums', 'entity.enchoer.trade': 'Echoer waits, humming',
    'entity.enchoer.yes': 'Echoer accepts', 'entity.enchoer.no': 'Echoer sighs', 'entity.enchoer.hurt': 'Echoer hurts', 'entity.enchoer.death': 'Echoer fades',
    'entity.enchoer.chimes': 'Echoer pings', 'entity.enchoer.flap': 'Echoer flaps', 'entity.enchoer.drill': 'Echoer\'s drill whirs',
    'block.echoer_device.charge': 'Echoer Drill listens', 'block.echoer_device.fire': 'Echoer Drill fires', 'block.echoer_device.fizzle': 'Echoer Drill jams',
    **__import__('echoer_drill').SUBTITLES,  # RR
}

NS = 'thesift'
