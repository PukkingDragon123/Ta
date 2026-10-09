"""F3 Knowledge & lore: the Mini Creator, the small platypus who guides you through the Sift.
S1 land: remade as a godly little explorer in the Sculk-mob pipeline (modelkit res=2 + x2 detail + materials).

A low, long platypus on four splayed legs with webbed feet, olive-brown fur, a wide flat bill and a broad
tail - dressed for exploring in the Creator's ceremonial white and gold: a white explorer's hat with a
wide brim, a gold band and a glowing sun gem under a floating halo; a white field coat with gold trim,
pockets and buttons; a gold neckerchief; a leather satchel with a gold buckle on a strap across his back;
gold tassels. Floating round him: four magical blocks with glowing runes (gold, prism, chrome, quartz)
and two glowing rune plates circling the other way. His gold trims, gems and runes glow softly.

Hooked in from one line in mobs.py. Part names are what client/knowledge/MiniCreatorModel.java animates
(body, head, jaw, crown = the hat, tail, tassel_0..3, the legs, orb_0..3, rune_0..1).
Top-level imports stay light: mobs.py imports this module while it is loading.
"""
from modelkit import Model

# the head's front at res 2 (7 x 5 units = 14 x 10 texels): small dark eyes with a golden glint, a gold-lined lid
FACE_W, FACE_H = 14, 10


def _face(blink=False):
    g = [['.'] * FACE_W for _ in range(FACE_H)]
    for x0 in (1, 11):
        if blink:
            g[4][x0] = g[4][x0 + 1] = 'd'
            g[5][x0] = g[5][x0 + 1] = 'L'
        else:
            g[2][x0] = g[2][x0 + 1] = 'd'
            g[3][x0], g[3][x0 + 1] = 'E', 'G'
            g[4][x0] = g[4][x0 + 1] = 'E'
            g[5][x0] = g[5][x0 + 1] = 'L'
    return [''.join(r) for r in g]


def _side_eye(blink=False):
    """The eye seen from the side: the platypus's eyes sit on the sides of its head."""
    g = [['.'] * 10 for _ in range(10)]
    if blink:
        g[4][1] = g[4][2] = 'd'
    else:
        g[3][1], g[3][2] = 'G', 'E'
        g[4][1] = g[4][2] = 'E'
        g[5][1] = g[5][2] = 'L'
    return [''.join(r) for r in g]


SUN = ['...g...', '.g.G.g.', '..GGG..', 'gGGgGGg', '..GGG..', '.g.G.g.', '...g...']
# glowing glyphs (6 x 6 texels) for the floating blocks and rune plates
RUNES = [['..RR..', '.R..R.', 'RRRRRR', '..RR..', '.R..R.', 'R....R'], ['R....R', '.R..R.', '..RR..', '..RR..', '.RRRR.', 'R....R'],
         ['.RRRR.', 'R....R', 'R.RR.R', 'R.RR.R', 'R....R', '.RRRR.'], ['..R...', '.RRR..', 'R.R.R.', '..R..R', '..R.R.', '..RR..']]


def _rune_face(i, frame=True):
    """A floating block's face: a bevelled frame and a glowing rune in the middle (5 units = 10 texels... here 6)."""
    glyph = RUNES[i % len(RUNES)]
    g = [['.'] * 6 for _ in range(6)]
    for y in range(6):
        for x in range(6):
            if glyph[y][x] == 'R':
                g[y][x] = 'R'
    return [''.join(r) for r in g]


def _coat_side(w, h, flip):
    """The coat's flank: a gold sun on a pocket flap, gold buttons along the hem line, stitching."""
    g = [['.'] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            if y in (1,) and x % 3 == 1:
                g[y][x] = 's'  # stitching under the collar
    # the pocket: an outline with a gold sun on the flap
    px0, py0 = (3, 3)
    for y in range(py0, py0 + 4):
        for x in range(px0, px0 + 6):
            if y == py0 or x in (px0, px0 + 5) or y == py0 + 3:
                g[y][x] = 'p'
    g[py0 + 1][px0 + 2] = g[py0 + 1][px0 + 3] = 'G'
    g[py0 + 2][px0 + 2] = g[py0 + 2][px0 + 3] = 'g'
    for k, sy in enumerate(SUN):
        for x, ch in enumerate(sy):
            if ch != '.' and 0 <= 2 + k < h and 0 <= w - 9 + x < w:
                g[2 + k][w - 9 + x] = ch
    out = [''.join(r) for r in g]
    return [r[::-1] for r in out] if flip else out


def mini_creator() -> Model:
    pal = {
        'fur': '#7a6243', 'fur_l': '#8f7652', 'fur_d': '#5e4a32',
        'belly': '#9a835e', 'belly_l': '#ab9470', 'belly_d': '#7d6849',
        'bill': '#3b3330', 'bill_l': '#4d4440', 'bill_d': '#2a2421',
        'web': '#3f3530', 'web_l': '#524640', 'web_d': '#2b2420',
        'tail': '#6a5338', 'tail_l': '#7f6646', 'tail_d': '#4f3d29',
        'robe': '#f2ecdc', 'robe_l': '#fffaf0', 'robe_d': '#cfc4a8',
        'gold': '#e0b23a', 'gold_l': '#ffd86a', 'gold_d': '#a07818',
        'leather': '#8a5a32', 'leather_l': '#a8743e', 'leather_d': '#5e3a1e',
        'gem': '#ff7ad0', 'gem_l': '#ffc0ec', 'gem_d': '#b03c8c',
        'eye': '#16100d', 'lid': '#a08a66',
        'q': '#efeae0', 'q_l': '#fffdf8', 'q_d': '#c8c0b0',
        'prism': '#d870c0', 'prism_l': '#f2a6de', 'prism_d': '#9a3e86',
        'chrome': '#6fc9ae', 'chrome_l': '#a6e8d2', 'chrome_d': '#3f8f7a',
        'rune': '#fff2a8', 'halo': '#ffe27a', 'halo_l': '#fff6c8',
    }
    materials = {'robe': 'cloth', 'leather': 'cloth', 'gold': 'metal', 'q': 'stone', 'prism': 'crystal', 'chrome': 'metal', 'tail': 'skin',
                 'bill': 'skin', 'web': 'skin'}
    m = Model('mini_creator', (128, 64), pal, {'mini_creator': {}}, res=2, expressions=['blink'], materials=materials)
    fur = dict(color='fur', pattern='mc', clusters=0.3, streaks=0.35, noise=0.8)
    robe = dict(color='robe', pattern='mc', clusters=0.2)
    gold = dict(color='gold', pattern='mc', clusters=0.25)
    eyes = {'E': 'eye', 'L': 'lid', 'd': 'fur_d', 'G': 'gold_l'}
    coat_keys = {'s': 'robe_d', 'p': 'robe_d', 'G': 'gold_l', 'g': 'gold', 'R': 'rune'}

    body = m.part('body', pivot=(0, 19, 0))
    body.cube((-4, -3.5, -6), (8, 6, 12), **dict(fur, bands=[(5, 'belly')]))
    # the field coat: white with a gold hem, pockets, stitching and a gold sun on each flank
    coat = dict(robe, bands=[(4, 'gold')])
    body.cube((-4.5, -4.25, -4.75), (9, 5, 10), **coat, faces={
        'east': dict(coat, hd=True, map=_coat_side(20, 10, False), keys=coat_keys, glow_keys='G'),
        'west': dict(coat, hd=True, map=_coat_side(20, 10, True), keys=coat_keys, glow_keys='G'),
        'south': dict(coat, hd=True, map=['......................'] * 2 + ['.....s.s.s.s.s.s......'] + ['..........gg..........'] * 2, keys=coat_keys),
        'up': dict(robe, hd=True, map=['..................'] * 3 + ['ssssssssssssssssss'] + ['..................'] * 16, keys=coat_keys),
        'down': dict(skip=True)})
    # the coat's tails, hanging a little lower at the back
    body.cube((-4.25, 0.75, 2.5), (8.5, 1.5, 3), **coat, faces={'up': dict(skip=True), 'down': dict(skip=True), 'north': dict(skip=True)})
    # the satchel's strap across his back, the satchel on his right flank with a gold buckle
    body.cube((-4.75, -4.6, -0.75), (9.5, 0.5, 1.5), color='leather', pattern='mc', clusters=0.2, faces={'down': dict(skip=True)})
    body.cube((-6.0, -2.0, -2.25), (1.5, 3, 4.5), color='leather', pattern='mc', clusters=0.3, faces={
        'west': dict(color='leather', pattern='mc', clusters=0.3, hd=True, map=['.........', 'lllllllll', '...ggg...', '...gGg...', '...ggg...', '.........'],
                     keys={'l': 'leather_d', 'g': 'gold', 'G': 'gold_l'}, glow_keys='G'),
        'up': dict(color='leather_l', pattern='mc', clusters=0.1)})
    for i, (x, z) in enumerate(((-4.75, -4.5), (4.25, -4.5), (-4.75, 4.75), (4.25, 4.75))):
        tassel = body.part(f'tassel_{i}', pivot=(x + 0.25, 0.75, z), rot=(0, 0, 0))
        tassel.cube((-0.25, 0, -0.25), (0.5, 1.5, 0.5), **gold)
        tassel.cube((-0.4, 1.5, -0.4), (0.8, 0.8, 0.8), color='gold_l', pattern='mc', clusters=0.0, glow=True)
    tail = body.part('tail', pivot=(0, -0.5, 6), rot=(0.2, 0, 0))
    hatch = ['t.t.t.t.t.t.', '.t.t.t.t.t.t'] * 7
    tail.cube((-3, -0.75, 0), (6, 1.5, 7), color='tail', pattern='mc', clusters=0.3,
              faces={'up': dict(color='tail', pattern='mc', clusters=0.1, hd=True, map_material=True, map=hatch, keys={'t': 'tail_d'})})

    head = body.part('head', pivot=(0, -0.5, -6))
    head.cube((-3.5, -3, -5), (7, 5, 5), **fur,
              faces={'north': dict(fur, clusters=0.2, hd=True, map=_face(), keys=eyes, glow_keys='G', expr={'blink': _face(True)}),
                     'east': dict(fur, hd=True, map=_side_eye(), keys=eyes, at=(0, 0), center=False, glow_keys='G', expr={'blink': _side_eye(True)}),
                     'west': dict(fur, hd=True, map=[r[::-1] for r in _side_eye()], keys=eyes, at=(0, 0), center=False, glow_keys='G',
                                  expr={'blink': [r[::-1] for r in _side_eye(True)]})})
    # the gold neckerchief, knotted at his throat
    head.cube((-3.75, -0.5, -1.25), (7.5, 2.75, 1.25), **gold)
    head.cube((-1, 1.75, -1.75), (2, 1.5, 1), color='gold_d', pattern='mc', clusters=0.0)
    bill = head.part('bill', pivot=(0, 0.5, -5))
    bill.cube((-3, -0.75, -5), (6, 1.25, 5), color='bill', pattern='mc', clusters=0.3, faces={
        'up': dict(color='bill', pattern='mc', clusters=0.2, hd=True, map=['............'] * 3 + ['...d....d...'] + ['............'] * 6, keys={'d': 'bill_d'})})
    bill.cube((-3.5, -0.75, -6), (7, 1.25, 1.5), color='bill', pattern='mc', clusters=0.3)
    jaw = head.part('jaw', pivot=(0, 1, -5))
    jaw.cube((-2.75, -0.25, -5), (5.5, 0.75, 5), color='bill_d', pattern='mc', clusters=0.2)
    # the explorer's hat: a wide white brim edged in gold, a domed crown, a gold band with a glowing sun gem, a halo floating over it
    crown = head.part('crown', pivot=(0, -3, -2.5))
    crown.cube((-5, -0.6, -4.5), (10, 0.6, 9), **robe, faces={
        'up': dict(robe, hd=True, map=[('g' * 20)] + ['g' + '.' * 18 + 'g'] * 16 + [('g' * 20)], keys={'g': 'gold'}),
        'down': dict(color='robe_d', pattern='mc', clusters=0.1),
        'north': dict(color='gold', pattern='mc', clusters=0.0), 'south': dict(color='gold', pattern='mc', clusters=0.0),
        'east': dict(color='gold', pattern='mc', clusters=0.0), 'west': dict(color='gold', pattern='mc', clusters=0.0)})
    crown.cube((-3, -3.4, -2.5), (6, 2.8, 5), **robe, faces={'up': dict(robe, hd=True, map=['..........'] * 4 + ['....ss....'] + ['..........'] * 5, keys={'s': 'robe_d'})})
    crown.cube((-3.1, -1.6, -2.6), (6.2, 1.0, 5.2), **gold, faces={
        'north': dict(gold, hd=True, map=['.....GG.....', '.....GG.....'], keys={'G': 'gem'}, glow_keys='G')})
    crown.cube((-0.6, -2.0, -3.0), (1.2, 1.4, 0.5), color='gem', pattern='mc', clusters=0.0, glow=True)
    halo = crown.part('halo', pivot=(0, -5.0, 0))
    for (x, z, w, d) in ((-3, -3, 6, 0.6), (-3, 2.4, 6, 0.6), (-3, -2.4, 0.6, 4.8), (2.4, -2.4, 0.6, 4.8)):
        halo.cube((x, -0.3, z), (w, 0.6, d), color='halo', pattern='mc', clusters=0.0, rim=False, glow=True)

    for side, sx in (('left', 1), ('right', -1)):
        for end, z in (('front', -3.5), ('back', 3.5)):
            leg = m.part(f'{end}_{side}_leg', pivot=(3.5 * sx, 21, z), rot=(0, 0, -0.25 * sx))
            leg.cube((-1, 0, -1), (2, 2.5, 2), **fur)
            leg.cube((-1.5, 2.5, -2.5), (3, 0.5, 3.5), color='web', pattern='mc', clusters=0.2, faces={
                'up': dict(color='web', pattern='mc', clusters=0.1, hd=True, map=['d.d.d.', 'd.d.d.', '......', '......', '......', '......', '......'],
                           keys={'d': 'web_l'})})

    # the four magical blocks, each carved with a glowing rune
    for i, mat in enumerate(('gold', 'prism', 'chrome', 'q')):
        orb = m.part(f'orb_{i}', pivot=(0, 10, 0))
        rune = dict(color=mat, pattern='mc', clusters=0.3, hd=True, map=_rune_face(i), keys={'R': 'rune'}, glow_keys='R')
        orb.cube((-1.5, -1.5, -1.5), (3, 3, 3), color=mat, pattern='mc', clusters=0.3, glow=mat in ('prism', 'chrome'),
                 faces={f: rune for f in ('north', 'south', 'east', 'west', 'up', 'down')})
    # two glowing rune plates circling the other way
    for i in range(2):
        plate = m.part(f'rune_{i}', pivot=(0, 12, 0))
        glyph = [r.replace('.', '_') for r in RUNES[(i + 2) % len(RUNES)]]
        spec = dict(color='halo', pattern='mc', clusters=0.0, rim=False, hd=True, map=glyph, keys={'R': 'halo_l'}, glow=True)
        plate.cube((-1.5, -1.5, 0), (3, 3, 0), color='halo', faces={'north': spec, 'south': spec})
    return m


MODELS = {'mini_creator': mini_creator}


def spawn_egg():
    """S1 land: his egg is drawn with the other land creatures' (tools/land_eggs.py)."""
    return __import__('land_eggs').mini_creator()

