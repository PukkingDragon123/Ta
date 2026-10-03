"""CR1 Sifter: the living bell of the Rocky Dunes - model geometry and paint (see modelkit.py), its
sounds, its bell-ring particle and its text (hooked from mobs.ALL, gen_assets and gen_textures).

A weathered bronze bell that walks the dunes on four stubby teal legs. Its iron clapper hangs inside
like a tongue and really strikes the lip (see Sifter.java / SifterModel.java): every tink lights the
gold runes round its waist. Eyes sit on its shoulder; verdigris runs down its sides and the dunes
have caked sand round its sound bow and in its crown."""
import math
import os
import random

from PIL import Image

from modelkit import Model

NS = 'thesift'


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


PAL = {
    # cast bronze, its polished sound bow (where it is struck) and the dark inside of the bell
    'bronze': '#b67c3c', 'bronze_l': '#d9a65e', 'bronze_d': '#86552a',
    'lip': '#d7a85c', 'lip_l': '#ffe2a2', 'lip_d': '#9a6a34',
    'inside': '#4a2c1a', 'inside_l': '#6a4228', 'inside_d': '#2a170c',
    # weathering: verdigris drips and the dune sand caked on it
    'patina': '#5fae96', 'patina_l': '#93d6bf', 'patina_d': '#3d806e',
    'sand': '#e8c890', 'sand_l': '#f7e0b2', 'sand_d': '#c49a62', 'grit': '#a8814c',
    # the inscription band: engraved grooves round gold runes (the runes glow when it rings)
    'engrave': '#5c3a1c', 'rune': '#ffd56c', 'rune_l': '#fff4c8',
    # the iron clapper
    'iron': '#5c5a66', 'iron_l': '#8e8c9a', 'iron_d': '#3a3842', 'strike': '#c9c4b8',
    # the living bit: stubby teal legs with bone toenails
    'skin': '#22a6c2', 'skin_l': '#46c4d8', 'skin_d': '#167f9e', 'toe': '#f0e6cc', 'toe_d': '#c6b892',
    # eyes on its shoulder
    'eye': '#101830', 'iris': '#ffcf5e', 'iris_d': '#dd8f28', 'eye_hi': '#ffffff', 'lid': '#c38a46', 'lid_d': '#6e4420',
}
MATERIALS = {'bronze': 'metal', 'lip': 'metal', 'inside': 'flat', 'iron': 'metal', 'skin': 'skin', 'sand': 'stone', 'lid': 'metal'}
EXPRS = ['blink', 'happy', 'angry', 'hurt', 'sleep', 'dead']
WEATHER_KEYS = {'p': 'patina', 'P': 'patina_l', 'q': 'patina_d', 's': 'sand', 'S': 'sand_l', 'g': 'grit', 'd': 'sand_d',
                'e': 'engrave', 'R': 'rune', 'r': 'rune_l', 'L': 'lip_l', 'b': 'bronze_d', 'B': 'bronze_l'}

# the bell, lip to crown: (name, outer width, top y, bottom y, hollow) in the bell's own space (y = 0 at
# the lip, up is negative). The bottom three tiers are hollow walls so the clapper has room to swing.
TIERS = [('lip', 14, -2, 0, True), ('bow', 12, -4, -2, True), ('skirt', 11, -7, -4, True),
         ('waist', 11, -11, -7, False), ('shoulder', 10, -13, -11, False), ('cap', 8, -14, -13, False), ('crown', 6, -15, -14, False)]
WALL = 2
# where the clapper hangs from (inside the waist) and how far below the lip its bob swings
CLAPPER_PIVOT = -9.0


def weather(w, h, seed, drips=0.0, sand=0, specks=0.0, polish=False, round_=False):
    """An hd overlay for one face (w x h texels): verdigris drips running down from the top, dune sand
    caked along the bottom with a ragged edge, a few grains stuck above it, and (polish) a bright
    worn line along the very bottom where the clapper strikes."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    if round_:
        # a box face shaded like the curve of a bell: a soft sheen left of centre, darker towards both edges
        for y in range(h):
            for x in range(w):
                u = (x + 0.5) / w
                if u < 0.08 or u > 0.93:
                    g[y][x] = 'b'
                elif 0.24 <= u <= 0.34 and (x + y) % 5:
                    g[y][x] = 'B'
    x = rnd.randrange(3)
    while x < w:
        if rnd.random() < drips:
            length = rnd.randrange(max(2, h // 3), h + 2)
            wide = rnd.random() < 0.35 and x + 1 < w
            for y in range(min(h, length)):
                tip = y >= length - 2
                g[y][x] = 'P' if y == 0 else ('q' if tip else 'p')
                if wide and y < length - 3:
                    g[y][x + 1] = 'p' if y else 'P'
            x += 3 if wide else 2
        x += 1 + rnd.randrange(3)
    if sand:
        for x in range(w):
            top = h - sand - (rnd.choice((0, 0, 1, 1, 2)) if sand > 1 else rnd.choice((0, 0, 1)))
            for y in range(max(0, top), h):
                if rnd.random() < 0.12:
                    continue
                g[y][x] = 's' if y > top else 'S'
                if y > top and rnd.random() < 0.18:
                    g[y][x] = rnd.choice('dg')
    for _ in range(int(w * h * specks)):
        sx, sy = rnd.randrange(w), rnd.randrange(h)
        if g[sy][sx] == '.':
            g[sy][sx] = rnd.choice('ssd')
    if polish:
        for x in range(w):
            if g[h - 1][x] in '.':
                g[h - 1][x] = 'L'
    return [''.join(r) for r in g]


GLYPHS = [['R', 'R', 'R'], ['R.R', '.R.', 'R.R'], ['.R.', 'RRR', '.R.'], ['RR', '.R', 'RR'], ['R', '.', 'R'], ['RRR', 'R.R', 'RRR'],
          ['.R', 'RR', '.R']]


def rune_band(w, h, seed):
    """The waist's inscription: two engraved lines with a row of gold runes between them."""
    rnd = random.Random(seed)
    g = [['.'] * w for _ in range(h)]
    top, bot = 1, h - 2
    for x in range(w):
        g[top][x] = 'e'
        g[bot][x] = 'e'
    space = bot - top - 1
    x = 1 + rnd.randrange(2)
    while x < w - 1:
        glyph = rnd.choice(GLYPHS)
        oy = top + 1 + max(0, (space - len(glyph)) // 2)
        for j, row in enumerate(glyph[:space]):
            for i, ch in enumerate(row):
                if ch != '.' and x + i < w - 1:
                    g[oy + j][x + i] = 'R'
        x += len(glyph[0]) + 2
    return [''.join(r) for r in g]


def merge(base, top):
    return [''.join(t if t != '.' else b for b, t in zip(rb, rt)) for rb, rt in zip(base, top)]


def sifter() -> Model:
    """The Sifter as a living bell (see the notes at the top of this file)."""
    from mobs_wild import eye
    m = Model('sifter', (128, 128), dict(PAL), {'sifter': {}}, res=2, expressions=EXPRS, materials=MATERIALS)
    seed = [11]

    def face(color, w, h, **kw):
        seed[0] += 7
        rows = weather(w * 2, h * 2, seed[0], round_=True, **kw)
        return mc(color, clusters=0.25, hd=True, map=rows, keys=WEATHER_KEYS, map_material=True)

    body = m.part('body', pivot=(0, 20, 0))
    bell = body.part('bell', pivot=(0, 0, 0))
    # ---- the bell: hollow lip, sound bow and skirt, then a solid waist, shoulder, cap and crown
    for i, (name, width, y0, y1, hollow) in enumerate(TIERS):
        hw = width / 2
        h = y1 - y0
        sand = {'lip': 2, 'bow': 1}.get(name, 0)
        drips = {'lip': 0.15, 'bow': 0.25, 'skirt': 0.35, 'waist': 0.4, 'shoulder': 0.45}.get(name, 0.2)
        polish = name == 'lip'
        top = mc('bronze', clusters=0.3, hd=True, map=weather(width * 2, width * 2, 900 + i, specks=0.08), keys=WEATHER_KEYS, map_material=True)
        if not hollow:
            faces = {f: face('bronze', width, h, drips=drips, specks=0.02) for f in ('north', 'south', 'east', 'west')}
            if name == 'waist':
                # the inscription band (gold runes that light up when the bell rings)
                for f in ('north', 'south', 'east', 'west'):
                    rows = merge(weather(width * 2, h * 2, 70 + len(f), drips=0.15, round_=True), rune_band(width * 2, h * 2, 40 + len(f)))
                    faces[f] = mc('bronze', clusters=0.2, hd=True, map=rows, keys=WEATHER_KEYS, glow_keys='R', map_material=True)
            faces['up'] = top if name != 'crown' else mc('sand', clusters=0.6, spots=0.2, accent='grit')
            # seen from below, the waist is the dark inside of the bell
            faces['down'] = mc('inside', clusters=0.0, rim=False, hd=True, map=_inside_ceiling(width * 2), keys={'k': 'inside_d', 'K': 'iron_d'})
            bell.cube((-hw, y0, -hw), (width, h, width), **mc('bronze', clusters=0.3), faces=faces)
            continue
        inner = mc('inside', clusters=0.15, rim=False)
        rim = mc('lip', clusters=0.0, rim=False, hd=True, map=weather(width * 2, WALL * 2, 300 + i, specks=0.2), keys=WEATHER_KEYS) \
            if name == 'lip' else inner
        out = {f: face('bronze', width, h, drips=drips, sand=sand, polish=polish) for f in ('north', 'south', 'east', 'west')}
        ends = mc('bronze', clusters=0.2, rim=False)
        # the four walls: front and back run the full width, the sides fit between them
        bell.cube((-hw, y0, -hw), (width, h, WALL), **mc('bronze', clusters=0.3), faces={
            'north': out['north'], 'south': inner, 'east': ends, 'west': ends, 'up': top, 'down': rim})
        bell.cube((-hw, y0, hw - WALL), (width, h, WALL), **mc('bronze', clusters=0.3), faces={
            'south': out['south'], 'north': inner, 'east': ends, 'west': ends, 'up': top, 'down': rim})
        bell.cube((hw - WALL, y0, -hw + WALL), (WALL, h, width - 2 * WALL), **mc('bronze', clusters=0.3), faces={
            'east': out['east'], 'west': inner, 'up': top, 'down': rim, 'north': dict(skip=True), 'south': dict(skip=True)})
        bell.cube((-hw, y0, -hw + WALL), (WALL, h, width - 2 * WALL), **mc('bronze', clusters=0.3), faces={
            'west': out['west'], 'east': inner, 'up': top, 'down': rim, 'north': dict(skip=True), 'south': dict(skip=True)})
    # ---- the canon: the loop it would hang by, crusted with sand
    loop = bell.part('canon', pivot=(0, -15, 0))
    lk = mc('bronze_d', clusters=0.2, rim=False, dark='bronze_d', lite='bronze')
    loop.cube((-2.5, -2, -1), (1, 2, 2), **lk)
    loop.cube((1.5, -2, -1), (1, 2, 2), **lk)
    loop.cube((-2.5, -3, -1), (5, 1, 2), **lk, faces={'up': mc('sand', clusters=0.5, spots=0.3, accent='grit')})

    # ---- eyes on its shoulder, bulging a little, rimmed with bronze lids
    ek = {'r': 'lid', 'i': 'iris', 'I': 'iris_d', 'p': 'eye', 'h': 'eye_hi', 'l': 'lid', 'd': 'lid_d'}
    for side, sx in (('left', 1), ('right', -1)):
        e = bell.part(f'{side}_eye', pivot=(2.6 * sx, -12.4, -4.6), rot=(-0.2, 0.24 * sx, 0))
        e.cube((-2, -2, -1.5), (4, 4, 2), **mc('lid', clusters=0.0, rim=False), faces={
            'north': mc('lid', clusters=0.0, rim=False, hd=True, map=eye(8, 8, '', sx < 0, pupil='dot', rim=0.66), keys=ek,
                        expr={x: eye(8, 8, x, sx < 0, pupil='dot', rim=0.66) for x in EXPRS})})

    # ---- the clapper: an iron tongue hung inside, its bob swinging just below the lip
    clap = bell.part('clapper', pivot=(0, CLAPPER_PIVOT, 0))
    clap.cube((-0.5, 0, -0.5), (1, 8, 1), **mc('iron', clusters=0.0, rim=False))
    bob = clap.part('clapper_bob', pivot=(0, 9.5, 0))
    bob.cube((-1.5, -1.5, -1.5), (3, 3, 3), **mc('iron', clusters=0.2, rim=False), faces={
        f: mc('iron', clusters=0.2, rim=False, hd=True, map=['......', '......', 'ssssss', 'SSSSSS', '......', '......'],
              keys={'s': 'strike', 'S': 'iron_l'}) for f in ('north', 'south', 'east', 'west')})

    # ---- four stubby teal legs under the lip, with bone toenails
    toes = ['......', '......', 'T.TT.T', 'tttttt']
    for name, sx, sz in (('front_left', 1, -1), ('front_right', -1, -1), ('back_left', 1, 1), ('back_right', -1, 1)):
        leg = body.part(f'{name}_leg', pivot=(4.5 * sx, -1, 4.5 * sz), rot=(0.12 * -sz, 0, -0.18 * sx))
        leg.cube((-1, 0, -1), (2, 3, 2), **mc('skin', clusters=0.3, spots=0.2, accent='skin_l', rim=False))
        foot = leg.part(f'{name}_foot', pivot=(0, 3, 0))
        foot.cube((-1.5, -0.5, -2), (3, 2, 4), **mc('skin', clusters=0.2, rim=False), faces={
            'north': mc('skin', clusters=0.0, rim=False, hd=True, map=toes, keys={'T': 'toe', 't': 'toe_d'}),
            'down': mc('skin_d', clusters=0.0)})
    return m


def _inside_ceiling(n):
    """The underside of the waist, seen up through the mouth of the bell: dark, with the clapper's
    iron mount in the middle."""
    g = [['.'] * n for _ in range(n)]
    c = n / 2 - 0.5
    for y in range(n):
        for x in range(n):
            d = max(abs(x - c), abs(y - c))
            if d > n / 2 - 2:
                g[y][x] = 'k'
            elif d < 2:
                g[y][x] = 'K'
    return [''.join(r) for r in g]


# --------------------------------------------------------------------------- sounds (vanilla events)
SOUNDS = {
    'entity.sifter.ambient': [('event:block.note_block.bell', 0.25, 1.5), ('event:block.amethyst_block.chime', 0.5, 1.2),
                              ('event:block.note_block.chime', 0.25, 1.3)],
    'entity.sifter.hurt': [('event:block.bell.use', 0.7, 1.45), ('event:block.bell.use', 0.7, 1.6)],
    'entity.sifter.death': [('event:block.bell.resonate', 1.0, 1.3), ('event:block.bell.use', 0.9, 1.2)],
    'entity.sifter.chomp': [('event:block.bell.use', 1.0, 1.2), ('event:block.bell.use', 1.0, 1.3)],
    'entity.sifter.leap': [('event:block.sand.break', 1.0, 0.8), ('event:block.sand.break', 1.0, 0.7)],
    'entity.sifter.step': [('event:block.copper.step', 0.5, 1.2), ('event:block.sand.break', 0.25, 1.4)],
    # the clapper striking the lip: a small tink (its pitch is the bell's own, see Sifter.java)
    'entity.sifter.tink': [('event:block.note_block.bell', 0.9, 1.0)],
    # a full ring: it pops out of the sand, startled
    'entity.sifter.ring': [('event:block.bell.use', 0.8, 1.0), ('event:block.bell.resonate', 0.5, 1.4)],
}
SUBTITLES = {
    'entity.sifter.ambient': 'Sifter hums', 'entity.sifter.hurt': 'Sifter clangs', 'entity.sifter.death': 'Sifter rings out',
    'entity.sifter.chomp': 'Sifter bonks', 'entity.sifter.leap': 'Sifter bursts from the sand', 'entity.sifter.step': 'Sifter waddles',
    'entity.sifter.tink': 'Sifter tinks', 'entity.sifter.ring': 'Sifter rings',
}
LANG = {
    f'entity.{NS}.sifter': 'Sifter',
    f'codex.{NS}.sifter.title': 'Sifter',
    f'codex.{NS}.sifter.tagline': 'Neutral - the living bell',
    f'codex.{NS}.sifter.body': (
        'A bronze bell that walked off into the dunes. Its clapper swings inside like a tongue: every waddle or bump '
        'makes it tink and lights the gold runes on its waist. It naps half-buried in sand and pops out ringing if '
        'trodden on. Neutral, but hit one and its neighbours clang to help. Chimes may call it into your band.'),
    f'band.{NS}.instrument.dune_bell': 'Dune Bell',
}


# --------------------------------------------------------------------------- the ring particles (CR1)
RINGS = ('bell_ring', 'echo_ring')


def assets(GA):
    """The two ring particles (the Sifter's bell ring, the Echoer's echolocation ping) and the Sifter's text."""
    for p in RINGS:
        GA.write(os.path.join(GA.A, 'particles', p + '.json'), {'textures': [f'{NS}:{p}']})
        GA.TEXTURES.add('particle/' + p)
    GA.LANG.update(LANG)


def _ring(n, radius, width, echo=0.0, dashes=0):
    """A soft white ring on an n x n canvas (the particle tints it): a bright core line with a glow
    falling off either side, an optional fainter inner echo ring, an optional dashed look."""
    img = Image.new('RGBA', (n, n))
    px = img.load()
    c = n / 2
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - c, y + 0.5 - c)
            a = max(0.0, 1.0 - abs(d - radius) / width) ** 1.6
            if echo:
                a = max(a, echo * max(0.0, 1.0 - abs(d - radius * 0.68) / (width * 0.7)) ** 1.6)
            if dashes and a > 0:
                ang = math.atan2(y + 0.5 - c, x + 0.5 - c)
                if math.cos(ang * dashes) < -0.55:
                    a *= 0.25
            if a > 0.02:
                core = max(0.0, 1.0 - abs(d - radius) / (width * 0.45))
                v = int(225 + 30 * core)
                px[x, y] = (v, v, v, int(255 * min(1.0, a)))
    return img


def textures(out):
    # the bell's ring: a bright line with a fainter echo inside it, like a struck bell's partials
    out('particle/bell_ring', _ring(32, 13.0, 2.6, echo=0.45))
    # the echolocation ping: a crisp ring broken into soft dashes, like a sonar wave
    out('particle/echo_ring', _ring(32, 12.5, 2.0, dashes=6))
