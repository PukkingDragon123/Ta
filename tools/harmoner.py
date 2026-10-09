"""S1 land: the Harmoner (and the Sculk Harmoner, its 'sculk' colouring), a small, round, fluffy songbird.

A puffball of a bird: a fluffy round body under layered tufts of feathers (overlapping fringed layers,
so the silhouette is soft), a big round head with a fluffy cap, huge glossy eyes and blushing cheeks,
a tiny beak, a crest of three curly flame-tipped plumes, short layered wings, a fanned tail and little
twig legs. Each colouring leads to its own structure (Harmoner.java); the 'sculk' one is the Conductor's
staff bird, whose crest tips, wing tips and eye glints glow.

Same pipeline as the Sculk mobs (modelkit res=2 + auto x2 detail + materials pass: 256x256 sheets).
Part names match HarmonerModel.java (body, head, jaw, crest, plume_0..2, left/right_wing, tail,
tail_fan, left/right_leg); the extra fluff parts are only drawn. mobs.harmoner() delegates here."""
from landkit import rows, shade3, tones
from modelkit import Model

# name: body, belly, wing, crest, crest tip, beak.  Each colour leads to its own structure.
VARIANTS = {
    'rose': ('#ee6a8e', '#ffd2dd', '#c4466e', '#ffd86b', '#ff8a3d', '#ffc23f'),
    'azure': ('#5aa2ee', '#d6f0ff', '#3470c4', '#86f2ff', '#ffffff', '#ffc23f'),
    'gold': ('#f4c048', '#fff4c8', '#cc8a24', '#ff7a4a', '#fff0a0', '#6a4a8a'),
    'violet': ('#9a6ae6', '#ecdcff', '#6a44b4', '#ff92dc', '#ffe0f4', '#ffd23f'),
    'jade': ('#46cc9a', '#d6ffea', '#259472', '#d0ff66', '#fff7a0', '#ff9a3d'),
    'coral': ('#ff8658', '#ffe2cc', '#cc5232', '#4ad8f2', '#d8faff', '#46386a'),
    'night': ('#2e3858', '#56688e', '#1a2038', '#36f4e4', '#d2fffb', '#e3ddcc'),
    'sculk': ('#173a46', '#2a6672', '#0c222c', '#3ff5e6', '#e8fffd', '#e3ddcc'),
}

FACE_KEYS = {'K': 'eye', 'w': 'eye_hi', 'i': 'eye_l', 'b': 'blush', 'B': 'blush_l', 'l': 'belly', 'd': 'body_d'}
# 4 x 5 texels per eye (the left one; the right one is mirrored)
EYE = {
    'neutral': ['.KK.', 'KwKK', 'KKwK', 'KKiK', '.KK.'],
    'blink': ['....', '....', '....', 'KKKK', '.KK.'],
    'happy': ['....', '.KK.', 'K..K', '....', '....'],
    'hurt': ['K...', '.KK.', '...K', '.KK.', 'K...'],
    'dead': ['K..K', '.KK.', '.KK.', 'K..K', '....'],
    'sleep': ['....', '....', 'K..K', '.KK.', '....'],
}
EXPRS = ['blink', 'happy', 'hurt', 'dead', 'sleep']


def face(expr):
    """The head's front, 12 x 11 texels: a pale mask, two huge glossy eyes and pink cheeks."""
    g = [['.'] * 12 for _ in range(11)]
    # a pale heart-shaped mask round the eyes, fading into the fluff
    for y in range(11):
        for x in range(12):
            dx = abs(x - 5.5)
            if 3 <= y <= 9 and dx < 4.9 - max(0, y - 7) * 1.5 and not (y == 3 and 1.5 < dx < 3.5):
                g[y][x] = 'l'
    e = EYE[expr]
    # the right eye is the same glyph (its glints on the same side, as the light falls); only the
    # wincing '> <' is mirrored
    for j, row in enumerate(e):
        for i, ch in enumerate(row):
            if ch != '.':
                g[3 + j][1 + i] = ch
                if expr == 'hurt':
                    g[3 + j][10 - i] = ch
                else:
                    g[3 + j][7 + i] = ch
    if expr != 'dead':
        for x in (0, 1, 10, 11):
            g[8][x] = 'b'
        g[8][1], g[8][10] = 'B', 'B'
    return [''.join(r) for r in g]


def scallops(w, h, seed=0, rowh=3, light='l', dark='d'):
    """Rows of overlapping rounded feathers: a dark crescent under each one, a lit tip on top."""
    def px(x, y):
        r = y // rowh
        xx = x + (2 if r % 2 else 0) + seed
        k = xx % 4
        yy = y % rowh
        if yy == rowh - 1 and k in (0, 3):
            return dark
        if yy == 0 and k in (1, 2):
            return light
        return '.'
    return rows(w, h, px)


def feather_plane(w, h, tip_rows, curl=0):
    """A feather seen flat (w x h texels): a soft vane narrowing to the quill, tip rows in the tip colour, '_' cut away."""
    def px(x, y):
        v = y / max(1, h - 1)
        half = (w / 2) * (0.35 + 0.65 * (1 - abs(v - 0.35) / 0.65) ** 0.7)
        cx = (w - 1) / 2 + curl * (1 - v) ** 2
        if abs(x - cx) > half:
            return '_'
        if y < tip_rows:
            return 't' if abs(x - cx) < half - 0.5 or y > 0 else 'T'
        if abs(x - cx) < 0.6:
            return 'q'
        return '.'
    return rows(w, h, px)


def harmoner() -> Model:
    variants = {}
    for name, (body, belly, wing, crest, tip, beak) in VARIANTS.items():
        v = tones(crest=crest, tip=tip, beak=beak)
        for key, c in (('body', body), ('belly', belly), ('wing', wing)):  # soft fluff: close tones
            v[key], v[key + '_l'], v[key + '_d'] = shade3(c, 0.07, -0.08)
        if name in ('night', 'sculk'):
            v.update({'eye': '#06141a', 'eye_l': '#36f4e4', 'blush': '#2a8890', 'blush_l': '#5ad8d8'})
        variants[f'harmoner_{name}'] = v
    pal = dict(variants['harmoner_rose'])
    pal.update({'eye': '#1a1426', 'eye_l': '#6a5aa0', 'eye_hi': '#ffffff', 'blush': '#ff8aa8', 'blush_l': '#ffb4c8',
                'leg': '#5a4250', 'leg_l': '#7a6070', 'leg_d': '#3a2a34', 'claw': '#2a1e26'})
    materials = {'body': 'fur', 'belly': 'fur', 'wing': 'fur', 'crest': 'fur', 'tip': 'fur', 'beak': 'bone', 'leg': 'skin'}
    m = Model('harmoner', (64, 64), pal, variants, res=2, expressions=EXPRS, materials=materials)
    FK = {'l': 'body_l', 'd': 'body_d'}
    BK = {'l': 'belly_l', 'd': 'belly_d'}
    WK = {'l': 'wing_l', 'd': 'wing_d', 't': 'crest', 'T': 'crest_l', 'q': 'wing_d'}
    CK = {'t': 'tip', 'T': 'tip_l', 'q': 'crest_d'}

    # ---- little twig legs with three toes
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(1.3 * sx, 22, 0.3))
        leg.cube((-0.4, 0, -0.4), (0.8, 1.6, 0.8), color='leg', pattern='mc', clusters=0.0, rim=False)
        leg.cube((-0.9, 1.5, -1.5), (1.8, 0.5, 2.1), color='leg', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='leg', pattern='mc', clusters=0.0, rim=False, hd=True, map=['c.c.', 'lcl.', 'l.l.', '.l..'] if sx > 0 else ['.c.c', '.lcl', '.l.l', '..l.'],
                       keys={'c': 'claw', 'l': 'leg_l'})})

    # ---- the round fluffy body: a soft core, a pale chest puff, and layers of fringed feather tufts over it
    body = m.part('body', pivot=(0, 22, 0))
    fluffy = dict(color='body', pattern='mc', clusters=0.15, streaks=0.25, noise=0.5)
    body.cube((-3.5, -6.0, -3.25), (7, 6.0, 6.5), **fluffy, faces={
        'north': dict(color='belly', pattern='mc', clusters=0.12, streaks=0.2, noise=0.5, hd=True, map_material=True, map=scallops(14, 12, 1), keys=BK),
        'east': dict(fluffy, hd=True, map_material=True, map=scallops(13, 12, 2), keys=FK, bands=[(4, 'belly')]),
        'west': dict(fluffy, hd=True, map_material=True, map=scallops(13, 12, 3), keys=FK, bands=[(4, 'belly')]),
        'south': dict(fluffy, hd=True, map_material=True, map=scallops(14, 12, 0), keys=FK),
        'down': dict(color='belly_d', pattern='mc', clusters=0.2)})
    # the chest puff, hanging in a fringe
    body.cube((-2.75, -5.5, -3.9), (5.5, 4.75, 1), color='belly', pattern='mc', clusters=0.12, streaks=0.3, noise=0.5, fringe=1, fringe_phase=2, faces={
        'north': dict(color='belly', pattern='mc', clusters=0.12, streaks=0.3, noise=0.5, fringe=1, fringe_phase=2, hd=True, map_material=True, map=scallops(11, 10, 3), keys=BK),
        'down': dict(skip=True), 'south': dict(skip=True)})
    # a shoulder ruff and a skirt of tufts: layered, fringed shells over the core
    body.cube((-3.8, -6.3, -3.55), (7.6, 2.6, 7.1), color='body_l', pattern='mc', clusters=0.15, streaks=0.4, noise=0.5, fringe=2, fringe_phase=1, faces={
        'down': dict(skip=True), 'north': dict(skip=True)})
    body.cube((-3.75, -2.6, -3.5), (7.5, 2.2, 7.0), color='body', pattern='mc', clusters=0.15, streaks=0.4, noise=0.5, fringe=1, fringe_phase=4, faces={
        'down': dict(skip=True), 'up': dict(skip=True), 'north': dict(color='belly', pattern='mc', clusters=0.12, streaks=0.4, noise=0.5, fringe=1, fringe_phase=4)})

    # ---- the big round head with a fluffy cap
    head = body.part('head', pivot=(0, -4.9, -0.8))
    expr = {e: face(e) for e in EXPRS}
    head.cube((-3.2, -5.5, -3), (6.4, 5.5, 5.6), **fluffy, faces={
        'north': dict(color='body', pattern='mc', clusters=0.0, hd=True, map=face('neutral'), keys=FACE_KEYS, glow_keys='w', expr=expr),
        'east': dict(fluffy, hd=True, map_material=True, map=scallops(11, 11, 1, rowh=4), keys=FK),
        'west': dict(fluffy, hd=True, map_material=True, map=scallops(11, 11, 2, rowh=4), keys=FK),
        'down': dict(color='belly', pattern='mc', clusters=0.2)})
    head.cube((-3.25, -5.9, -3.25), (6.5, 1.6, 6.0), color='body_l', pattern='mc', clusters=0.15, streaks=0.5, noise=0.5, fringe=1, fringe_phase=3, faces={
        'down': dict(skip=True)})
    # cheek tufts sticking out sideways, like a fledgling's
    for side, sx in (('left', 1), ('right', -1)):
        cheek = head.part(f'{side}_cheek', pivot=(3.0 * sx, -2.2, -1.2), rot=(0, 0, -0.35 * sx))
        cheek.cube((0 if sx > 0 else -1.6, -1.2, -1.0), (1.6, 2.2, 2.4), color='body_l', pattern='mc', clusters=0.15, streaks=0.5, noise=0.5, fringe=1,
                   fringe_phase=2 + sx, faces={'down': dict(skip=True)})
    # a tiny beak, the lower half on its own hinge
    head.cube((-0.8, -2.6, -4.0), (1.6, 1.0, 1.1), color='beak', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='beak_l', pattern='mc', clusters=0.0), 'north': dict(color='beak', pattern='mc', clusters=0.0, hd=True, map=['ll.', '...'], keys={'l': 'beak_l'})})
    jaw = head.part('jaw', pivot=(0, -1.6, -3.0))
    jaw.cube((-0.6, 0, -0.9), (1.2, 0.6, 0.9), color='beak_d', pattern='mc', clusters=0.0, rim=False)
    # the crest: three curly plumes, each a crossed pair of feather planes, flame-tipped
    crest = head.part('crest', pivot=(0, -5.6, -0.4), rot=(-0.35, 0, 0))
    for i, (cx, h, lean) in enumerate(((-1.0, 3.0, -0.4), (0.0, 4.2, 0.05), (1.0, 3.0, 0.4))):
        plume = crest.part(f'plume_{i}', pivot=(cx, 0, 0), rot=(0, 0, lean))
        th = int(round(h * 2))
        tipmap = rows(2, th, lambda x, y: 'T' if y == 0 else ('t' if y < 3 else ('q' if y == th - 1 else '.')))
        side = rows(3, th, lambda x, y: 'T' if y == 0 else ('t' if y < 3 else '.'))
        plume.cube((-0.5, -h, -0.75), (1, h, 1.5), color='crest', pattern='mc', clusters=0.0, rim=False, faces={
            'north': dict(color='crest', pattern='mc', clusters=0.0, rim=False, hd=True, map=tipmap, keys=CK, glow_keys='tT'),
            'south': dict(color='crest', pattern='mc', clusters=0.0, rim=False, hd=True, map=tipmap, keys=CK, glow_keys='tT'),
            'east': dict(color='crest', pattern='mc', clusters=0.0, rim=False, hd=True, map=side, keys=CK, glow_keys='tT'),
            'west': dict(color='crest', pattern='mc', clusters=0.0, rim=False, hd=True, map=side, keys=CK, glow_keys='tT'),
            'up': dict(color='tip_l', pattern='mc', clusters=0.0, glow=True)})
        # the plume curls forward at its tip
        curl = plume.part(f'plume_{i}_curl', pivot=(0, -h + 0.2, -0.5), rot=(-0.9, 0, 0))
        curl.cube((-0.45, -1.4, -0.6), (0.9, 1.4, 1.2), color='tip', pattern='mc', clusters=0.0, rim=False, glow=True)

    # ---- short wings: layered coverts over primaries that fan out in flight
    for side, sx in (('left', 1), ('right', -1)):
        wing = body.part(f'{side}_wing', pivot=(3.3 * sx, -5.0, -1.4))
        out = 'east' if sx > 0 else 'west'
        wing.cube((0 if sx > 0 else -0.7, 0, 0), (0.7, 4.0, 5.0), color='wing', pattern='mc', clusters=0.15, noise=0.5, faces={
            out: dict(color='wing', pattern='mc', clusters=0.3, hd=True, map_material=True, keys=WK,
                      map=scallops(10, 8, 1 if sx > 0 else 2, rowh=3)[:5] + rows(10, 3, lambda x, y: 'd' if y == 0 and x % 4 == 0 else ('t' if y > 0 else '.')),
                      glow_keys='t')})
        prim = wing.part(f'{side}_primaries', pivot=(0.35 * sx, 0.8, 4.4), rot=(0.25, 0, 0))
        pm = rows(8, 8, lambda x, y: '_' if y > 6 - (1 if x % 3 == 0 else 0) else ('t' if y > 4 else ('d' if x % 3 == 0 and y > 1 else '.')))
        prim.cube((0, 0, 0), (0, 3.6, 4), color='wing', faces={
            'east': dict(color='wing', pattern='mc', clusters=0.0, rim=False, hd=True, map=[r[::-1] for r in pm] if sx < 0 else pm, keys=WK, glow_keys='t'),
            'west': dict(color='wing', pattern='mc', clusters=0.0, rim=False, hd=True, map=pm if sx < 0 else [r[::-1] for r in pm], keys=WK, glow_keys='t')})

    # ---- the fanned tail
    tail = body.part('tail', pivot=(0, -2.6, 3.0), rot=(0.3, 0, 0))
    tail.cube((-1.5, -0.4, 0), (3, 0.8, 2.4), color='wing', pattern='mc', clusters=0.2, rim=False)
    fan = tail.part('tail_fan', pivot=(0, 0, 2.2), rot=(0.2, 0, 0))
    tf = rows(12, 9, lambda x, y: '_' if y >= 9 - (1 if x % 3 == 1 else 0) - (2 if x % 3 == 0 and x not in (0, 11) else 0) and y > 5 else
              ('t' if y >= 6 else ('d' if x % 3 == 0 else '.')))
    fan.cube((-3, 0, 0), (6, 0, 4.5), color='crest', faces={
        'up': dict(color='crest', pattern='mc', clusters=0.0, rim=False, hd=True, map=tf, keys=dict(CK, d='crest_d'), glow_keys='t'),
        'down': dict(color='crest_d', pattern='mc', clusters=0.0, rim=False, hd=True, map=tf, keys=dict(CK, d='crest_d'), glow_keys='t')})
    return m


MODELS = {'harmoner': harmoner}
