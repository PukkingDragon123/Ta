"""S1 land: the Harmoner (and the Sculk Harmoner, its 'sculk' colouring), a small, fluffy-feathered Sift parrot.

Built on the vanilla parrot's proportions and feather layout: an upright body leaning forward on short legs with
zygodactyl toes, a small round head with a hooked beak and a bare pale ring round each small eye (the parrot's way
- never cartoon eyes), a crest of three feathers raised from the back of the head, long folded wings lying back
along the body with their flight feathers fanned at the tips, and a long tail hanging down behind. Soft fluffed
feathers (scalloped rows, fringed tufts at the chest and the nape). Each colouring leads to its own structure
(Harmoner.java); the 'sculk' one is the Conductor's staff bird, whose crest tips and flight feathers glow.

Same pipeline as the Sculk mobs (modelkit res=2 + auto x2 detail + materials pass: 256x256 sheets).
Part names match HarmonerModel.java (body, head, jaw, crest, plume_0..2, left/right_wing, tail, tail_fan,
left/right_leg; the body's lean and the wings' sweep sit on child parts, since the Java sets those parts'
own rotations); mobs.harmoner() delegates here."""
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

EXPRS = ['blink', 'happy', 'hurt', 'dead', 'sleep']


def side_face(expr, w, h, flip):
    """A side of the head (w x h texels, front edge on the left): the bare pale eye-ring of a parrot round a small
    dark eye, a cheek patch below it; moods only move the lid."""
    g = [['.'] * w for _ in range(h)]
    ex, ey = 2, 2
    for y in range(ey - 1, ey + 3):
        for x in range(ex - 1, ex + 3):
            corner = (y in (ey - 1, ey + 2)) and (x in (ex - 1, ex + 2))
            if 0 <= x < w and 0 <= y < h and not corner:
                g[y][x] = 'r'
    if expr in ('blink', 'sleep', 'dead'):
        g[ey + 1][ex] = g[ey + 1][ex + 1] = 'd'
    elif expr in ('happy', 'hurt'):
        g[ey + 1][ex] = g[ey + 1][ex + 1] = 'K'
        g[ey][ex] = g[ey][ex + 1] = 'd'
    else:
        g[ey][ex] = g[ey][ex + 1] = 'K'
        g[ey + 1][ex] = 'K'
        g[ey + 1][ex + 1] = 'k'
    for x in range(1, 5):
        if ey + 4 < h:
            g[ey + 4][x] = 'c'
    out = [''.join(r) for r in g]
    return [r[::-1] for r in out] if flip else out


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


def feather_plane(w, h, tip_rows, curl=0.0, root_w=0.35):
    """A feather seen flat (w x h texels, tip at the top): a vane narrowing to the quill, the top rows in the tip
    colour, a dark shaft; '_' is cut away."""
    def px(x, y):
        v = y / max(1, h - 1)
        half = (w / 2) * (root_w + (1 - root_w) * (1 - abs(v - 0.3) / 0.7) ** 0.6)
        cx = (w - 1) / 2 + curl * (1 - v) ** 2
        if abs(x - cx) > half:
            return '_'
        if y < tip_rows:
            return 't'
        if abs(x - cx) < 0.6:
            return 'q'
        return '.'
    return rows(w, h, px)


def harmoner() -> Model:
    variants = {}
    for name, (body, belly, wing, crest, tip, beak) in VARIANTS.items():
        v = tones(crest=crest, tip=tip, beak=beak)
        for key, c in (('body', body), ('belly', belly), ('wing', wing)):  # soft feathers: close tones
            v[key], v[key + '_l'], v[key + '_d'] = shade3(c, 0.07, -0.08)
        if name in ('night', 'sculk'):
            v.update({'ring': '#6a7c88', 'cheek': v['belly_l']})
        variants[f'harmoner_{name}'] = v
    pal = dict(variants['harmoner_rose'])
    pal.update({'eye': '#120c14', 'eye_l': '#3a2a30', 'ring': '#e8ded8', 'ring_d': '#b8aca4', 'cheek': '#ffe8ec',
                'leg': '#5a4a50', 'leg_l': '#786670', 'leg_d': '#3a2e34', 'claw': '#2a1e26'})
    for v in variants.values():
        v.setdefault('cheek', v['belly_l'])
    materials = {'body': 'fur', 'belly': 'fur', 'wing': 'fur', 'crest': 'fur', 'tip': 'fur', 'beak': 'bone', 'leg': 'skin'}
    m = Model('harmoner', (64, 64), pal, variants, res=2, expressions=EXPRS, materials=materials)
    FK = {'l': 'body_l', 'd': 'body_d'}
    BK = {'l': 'belly_l', 'd': 'belly_d'}
    WK = {'l': 'wing_l', 'd': 'wing_d', 't': 'tip', 'q': 'wing_d', 'c': 'crest'}
    CK = {'t': 'tip', 'q': 'crest_d'}
    soft = dict(pattern='mc', clusters=0.15, streaks=0.25, noise=0.5)
    LEAN = 0.45  # the body leans forward like a perched parrot's

    # ---- short legs with zygodactyl toes (two forward, two back)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(1.1 * sx, 21.5, -0.4))
        leg.cube((-0.5, 0, -0.5), (1, 2, 1), color='leg', pattern='mc', clusters=0.0, rim=False)
        leg.cube((-0.75, 2, -1.5), (1.5, 0.5, 2.6), color='leg', pattern='mc', clusters=0.0, rim=False, faces={
            'up': dict(color='leg', pattern='mc', clusters=0.0, rim=False, hd=True, map=['c.c.', 'l.l.', '.ll.', '.ll.', 'l..l', 'c..c'],
                       keys={'c': 'claw', 'l': 'leg_l'})})

    # ---- the body: upright and leaning forward, a pale fluffed chest, a ruff of tufts at the nape
    body = m.part('body', pivot=(0, 21.5, 0))
    torso = body.part('torso', rot=(LEAN, 0, 0))
    torso.cube((-2, -7, -2), (4, 7, 4), color='body', **soft, faces={
        'north': dict(color='belly', **soft, hd=True, map_material=True, map=scallops(8, 14, 1), keys=BK, bands=[(6, 'belly_d')]),
        'east': dict(color='body', **soft, hd=True, map_material=True, map=scallops(8, 14, 2), keys=FK),
        'west': dict(color='body', **soft, hd=True, map_material=True, map=scallops(8, 14, 3), keys=FK),
        'south': dict(color='body', **soft, hd=True, map_material=True, map=scallops(8, 14, 0), keys=FK),
        'down': dict(color='belly_d', pattern='mc', clusters=0.2)})
    # fluffed chest feathers hanging in a fringe, and a ruff round the nape
    torso.cube((-1.75, -6, -2.6), (3.5, 4.5, 0.8), color='belly', **soft, fringe=1, fringe_phase=2, faces={
        'down': dict(skip=True), 'south': dict(skip=True)})
    torso.cube((-2.3, -7.4, -2.3), (4.6, 2.2, 4.6), color='body_l', **soft, fringe=1, fringe_phase=1, faces={'down': dict(skip=True)})
    torso.cube((-2.2, -1.8, -2.2), (4.4, 1.6, 4.4), color='belly_d', **soft, fringe=1, fringe_phase=4, faces={
        'up': dict(skip=True), 'down': dict(skip=True)})

    # ---- the head: small and round, a hooked beak, small eyes in bare pale rings
    head = body.part('head', pivot=(0, -6.2, -2.7))
    sides = {f: dict(color='body', pattern='mc', clusters=0.0, hd=True, map=side_face('', 8, 8, f == 'west'),
                     keys={'r': 'ring', 'K': 'eye', 'k': 'eye_l', 'd': 'ring_d', 'c': 'cheek'},
                     expr={e: side_face(e, 8, 8, f == 'west') for e in EXPRS}) for f in ('east', 'west')}
    head.cube((-1.6, -3.4, -1.6), (3.2, 3.4, 3.2), color='body', **soft, faces={
        'east': sides['east'], 'west': sides['west'],
        'north': dict(color='body', pattern='mc', clusters=0.0, hd=True, map=['........'] * 5 + ['.cccccc.', '........', '........'],
                      keys={'c': 'cheek'}),
        'down': dict(color='belly', pattern='mc', clusters=0.2)})
    head.cube((-1.8, -3.7, -1.2), (3.6, 1.2, 3.2), color='body_l', **soft, fringe=1, fringe_phase=3, faces={'down': dict(skip=True)})
    # the hooked upper beak, and the lower beak on its hinge
    head.cube((-0.5, -2.2, -2.4), (1.0, 1.2, 0.9), color='beak', pattern='mc', clusters=0.0, rim=False, faces={
        'up': dict(color='beak_l', pattern='mc', clusters=0.0)})
    head.cube((-0.4, -1.1, -2.65), (0.8, 0.7, 0.6), color='beak_d', pattern='mc', clusters=0.0, rim=False)
    jaw = head.part('jaw', pivot=(0, -1.0, -1.7))
    jaw.cube((-0.5, 0, -0.8), (1.0, 0.7, 0.9), color='beak_d', pattern='mc', clusters=0.0, rim=False)
    # the crest: three feathers raised from the back of the head, the middle one tallest, flame-tipped
    crest = head.part('crest', pivot=(0, -3.2, 0.6), rot=(-0.3, 0, 0))
    for i, (cx, h, w, lean) in enumerate(((-0.5, 3.0, 2.0, -0.3), (0.0, 4.5, 2.6, 0.0), (0.5, 3.0, 2.0, 0.3))):
        plume = crest.part(f'plume_{i}', pivot=(cx, 0, 0), rot=(0, 0, lean))
        fp = dict(color='crest', pattern='mc', clusters=0.0, rim=False, hd=True, keys=CK, glow_keys='t',
                  map=feather_plane(int(w * 2), int(h * 2), 3, 0.0))
        fp_m = dict(fp, map=[r[::-1] for r in fp['map']])
        plume.cube((0, -h, -w / 2), (0, h, w), color='crest', faces={'east': fp, 'west': fp_m})

    # ---- long wings folded back along the body (the sweep sits on a child part: the Java flaps the wing itself)
    for side, sx in (('left', 1), ('right', -1)):
        wing = body.part(f'{side}_wing', pivot=(2.0 * sx, -6.0, -2.2))
        blade = wing.part(f'{side}_wing_blade', rot=(LEAN + 0.25, 0, 0))
        out = 'east' if sx > 0 else 'west'
        coverts = scallops(6, 8, 1 if sx > 0 else 2, rowh=3)
        flight = rows(6, 6, lambda x, y: 'c' if y < 2 else ('d' if x % 2 == 0 and y < 5 else 't'))
        blade.cube((0 if sx > 0 else -0.8, 0, -1.6), (0.8, 7, 3.2), color='wing', pattern='mc', clusters=0.15, noise=0.5, faces={
            out: dict(color='wing', pattern='mc', clusters=0.15, noise=0.5, hd=True, map_material=True, keys=WK, glow_keys='t',
                      map=[r[::-1] for r in coverts + flight] if sx < 0 else coverts + flight)})
        prim = blade.part(f'{side}_primaries', pivot=(0.4 * sx, 6.8, 0.4))
        pm = rows(6, 6, lambda x, y: '_' if y > 3 + (x % 2) else ('t' if y > 1 else 'd'))
        prim.cube((0, 0, -1.4), (0, 2.6, 3.0), color='wing', faces={
            'east': dict(color='wing', pattern='mc', clusters=0.0, rim=False, hd=True, map=pm, keys=WK, glow_keys='t'),
            'west': dict(color='wing', pattern='mc', clusters=0.0, rim=False, hd=True, map=[r[::-1] for r in pm], keys=WK, glow_keys='t')})

    # ---- the long tail hanging down behind, its feathers fanned and tipped
    tail = body.part('tail', pivot=(0, -1.2, 1.7), rot=(0.75, 0, 0))
    tail.cube((-1.25, 0, -0.4), (2.5, 2.5, 0.8), color='wing', pattern='mc', clusters=0.15, rim=False)
    fan = tail.part('tail_fan', pivot=(0, 2.4, 0), rot=(0.1, 0, 0))
    tf = rows(8, 14, lambda x, y: '_' if y > 13 - (x % 2) * 2 - (1 if x in (0, 7) else 0) * 2 else ('t' if y > 9 else ('d' if x % 2 == 0 else '.')))
    fan.cube((-2, 0, -0.25), (4, 7, 0.5), color='wing', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='wing', pattern='mc', clusters=0.0, rim=False, hd=True, map=tf, keys=dict(WK, t='crest'), glow_keys='t'),
        'south': dict(color='wing', pattern='mc', clusters=0.0, rim=False, hd=True, map=[r[::-1] for r in tf], keys=dict(WK, t='crest'), glow_keys='t')})
    return m


MODELS = {'harmoner': harmoner}
