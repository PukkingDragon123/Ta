"""F3 Knowledge & lore: the Mini Creator, the small platypus who guides you through the Sift.
S1 land: an avatar, a saint - remade in the Sculk-mob pipeline (modelkit res=2 + x2 detail + materials).

He sits cross-legged in the air, meditating, floating a little over the ground: a platypus in the Creator's
white and gold - a robe with a gold-trimmed V-neck and hem, a gold sash, a mantle over his shoulders with
gold tassels, a string of prayer beads - paws resting on his crossed knees, his broad tail behind him. His
eyes glow (small slits of light, never cartoon eyes); a gold circlet on his brow, a halo of light behind his
head, four rune-carved blocks and two glowing rune plates circling him. He waits at the Creator's Ruin until
his hymn is played at its dais (tools/creators_ruin.py, knowledge/CreatorShrine.java).

Hooked in from one line in mobs.py. Part names are what client/knowledge/MiniCreatorModel.java animates
(body, head, jaw, crown + halo, tail, tassel_0..3, the four legs, orb_0..3, rune_0..1).
Top-level imports stay light: mobs.py imports this module while it is loading.
"""
from modelkit import Model

FACE_W, FACE_H = 12, 10


def _face(blink=False):
    """The head's front (12 x 10 texels): two small slits of light for eyes (closed: dark lines), a pale brow line."""
    g = [['.'] * FACE_W for _ in range(FACE_H)]
    for x0 in (2, 8):
        g[2][x0] = g[2][x0 + 1] = 'l'
        if blink:
            g[4][x0] = g[4][x0 + 1] = 'd'
        else:
            g[4][x0] = g[4][x0 + 1] = 'G'
            g[5][x0] = g[5][x0 + 1] = 'g'
    return [''.join(r) for r in g]


def _side_eye(blink=False):
    """The eye seen from the side, at the front edge: a small slit of light."""
    g = [['.'] * 10 for _ in range(10)]
    if blink:
        g[4][1] = g[4][2] = 'd'
    else:
        g[4][1], g[4][2] = 'G', 'g'
        g[5][1] = 'g'
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


def _robe_front(w, h):
    """The robe's front (hd): a V-neck showing the fur of his chest, edged in gold, a gold sash from the left shoulder
    to the right hip, a gold hem with a row of little suns."""
    cx = (w - 1) / 2
    g = [['.'] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            dx = abs(x - cx)
            v = 5.5 - y * 0.75  # the V-neck narrows downwards
            if y < 7 and dx < v - 0.6:
                g[y][x] = 'f'
            elif y < 8 and dx < v + 0.6:
                g[y][x] = 'G'
            if y >= h - 3:
                g[y][x] = 'G' if y == h - 3 else ('g' if (x + y) % 4 == 0 else 'G')
    for y in range(3, h - 3):  # the sash
        x = int(round(2 + (y - 3) * (w - 5) / max(1, h - 7)))
        for k in (0, 1):
            if 0 <= x + k < w and g[y][x + k] != 'f':
                g[y][x + k] = 's'
    return [''.join(r) for r in g]


def mini_creator() -> Model:
    pal = {
        'fur': '#7a6243', 'fur_l': '#8f7652', 'fur_d': '#5e4a32',
        'belly': '#9a835e', 'belly_l': '#ab9470', 'belly_d': '#7d6849',
        'bill': '#3b3330', 'bill_l': '#4d4440', 'bill_d': '#2a2421',
        'web': '#3f3530', 'web_l': '#524640', 'web_d': '#2b2420',
        'tail': '#6a5338', 'tail_l': '#7f6646', 'tail_d': '#4f3d29',
        'robe': '#f2ecdc', 'robe_l': '#fffaf0', 'robe_d': '#cfc4a8',
        'gold': '#e0b23a', 'gold_l': '#ffd86a', 'gold_d': '#a07818',
        'bead': '#5a3a22', 'bead_l': '#7a5232',
        'gem': '#ff7ad0', 'gem_l': '#ffc0ec', 'gem_d': '#b03c8c',
        'eye': '#16100d', 'lid': '#a08a66', 'eyeglow': '#fff4b0', 'eyeglow_d': '#ffcf5a',
        'q': '#efeae0', 'q_l': '#fffdf8', 'q_d': '#c8c0b0',
        'prism': '#d870c0', 'prism_l': '#f2a6de', 'prism_d': '#9a3e86',
        'chrome': '#6fc9ae', 'chrome_l': '#a6e8d2', 'chrome_d': '#3f8f7a',
        'rune': '#fff2a8', 'halo': '#ffe27a', 'halo_l': '#fff6c8',
    }
    materials = {'robe': 'cloth', 'gold': 'metal', 'q': 'stone', 'prism': 'crystal', 'chrome': 'metal', 'tail': 'skin',
                 'bill': 'skin', 'web': 'skin', 'bead': 'wood'}
    m = Model('mini_creator', (128, 128), pal, {'mini_creator': {}}, res=2, expressions=['blink'], materials=materials)
    fur = dict(color='fur', pattern='mc', clusters=0.3, streaks=0.35, noise=0.8)
    robe = dict(color='robe', pattern='mc', clusters=0.2)
    gold = dict(color='gold', pattern='mc', clusters=0.25)
    eyes = {'G': 'eyeglow', 'g': 'eyeglow_d', 'd': 'fur_d', 'l': 'fur_l'}
    rk = {'f': 'belly', 'G': 'gold', 'g': 'gold_d', 's': 'gold_l'}

    # ---- the seated body: fur under a white robe, a mantle over the shoulders with gold tassels, prayer beads
    body = m.part('body', pivot=(0, 18, 0))
    body.cube((-3.5, -8.5, -3), (7, 9, 6), **dict(fur, bands=[(6, 'belly')]))
    body.cube((-3.8, -8.7, -3.3), (7.6, 9.2, 6.6), **robe, faces={
        'north': dict(robe, hd=True, map=_robe_front(16, 20), keys=rk, map_material=True),
        'south': dict(robe, hd=True, map=['.' * 16] * 17 + ['G' * 16] * 3, keys=rk),
        'east': dict(robe, hd=True, map=['.' * 14] * 17 + ['G' * 14] * 3, keys=rk),
        'west': dict(robe, hd=True, map=['.' * 14] * 17 + ['G' * 14] * 3, keys=rk),
        'down': dict(skip=True)})
    body.cube((-4.3, -9.0, -3.6), (8.6, 3.0, 7.2), color='robe_l', pattern='mc', clusters=0.15, bands=[(2, 'gold')], faces={
        'down': dict(skip=True)})
    body.cube((-2.6, -8.4, -3.9), (5.2, 0.6, 0.5), color='bead', pattern='mc', clusters=0.0, rim=False, faces={
        'north': dict(color='bead', pattern='mc', clusters=0.0, rim=False, hd=True, map=['bGbGbGbGbGb'], keys={'b': 'bead_l', 'G': 'gold'})})
    body.cube((-0.5, -7.9, -4.0), (1, 1, 0.6), color='gold', pattern='mc', clusters=0.0, rim=False, glow=True)  # the bead's sun pendant
    for i, (x, z) in enumerate(((-4.5, -3.6), (4.0, -3.6), (-4.5, 3.4), (4.0, 3.4))):
        tassel = body.part(f'tassel_{i}', pivot=(x + 0.25, -6.0, z), rot=(0, 0, 0))
        tassel.cube((-0.25, 0, -0.25), (0.5, 1.5, 0.5), **gold)
        tassel.cube((-0.4, 1.5, -0.4), (0.8, 0.8, 0.8), color='gold_l', pattern='mc', clusters=0.0, glow=True)
    # the broad tail behind him
    tail = body.part('tail', pivot=(0, -0.5, 2.5), rot=(0.25, 0, 0))
    hatch = ['t.t.t.t.t.t.', '.t.t.t.t.t.t'] * 7
    tail.cube((-3, -0.75, 0), (6, 1.5, 7), color='tail', pattern='mc', clusters=0.3,
              faces={'up': dict(color='tail', pattern='mc', clusters=0.1, hd=True, map_material=True, map=hatch, keys={'t': 'tail_d'})})

    # ---- the head: glowing eyes, the wide bill, a gold circlet, a halo of light behind it
    head = body.part('head', pivot=(0, -8.6, -0.5))
    head.cube((-3, -5, -3), (6, 5, 5.5), **fur,
              faces={'north': dict(fur, clusters=0.2, hd=True, map=_face(), keys=eyes, glow_keys='Gg', expr={'blink': _face(True)}),
                     'east': dict(fur, hd=True, map=_side_eye(), keys=eyes, at=(0, 0), center=False, glow_keys='Gg', expr={'blink': _side_eye(True)}),
                     'west': dict(fur, hd=True, map=[r[::-1] for r in _side_eye()], keys=eyes, at=(0, 0), center=False, glow_keys='Gg',
                                  expr={'blink': [r[::-1] for r in _side_eye(True)]})})
    bill = head.part('bill', pivot=(0, -1.0, -3))
    bill.cube((-2.5, -0.5, -4.5), (5, 1.1, 4.5), color='bill', pattern='mc', clusters=0.3, faces={
        'up': dict(color='bill', pattern='mc', clusters=0.2, hd=True, map=['..........'] * 2 + ['..d....d..'] + ['..........'] * 6, keys={'d': 'bill_d'})})
    bill.cube((-3, -0.5, -5.5), (6, 1.1, 1.2), color='bill', pattern='mc', clusters=0.3)
    jaw = head.part('jaw', pivot=(0, 0.1, -3))
    jaw.cube((-2.25, 0, -4.2), (4.5, 0.6, 4.2), color='bill_d', pattern='mc', clusters=0.2)
    crown = head.part('crown', pivot=(0, -5, -0.25))
    # a thin gold circlet round his brow, a gem at the front
    crown.cube((-3.15, 0.6, -3.15), (6.3, 0.7, 0.3), **gold, faces={
        'north': dict(gold, hd=True, map=['.....GG.....', '.....GG.....'], keys={'G': 'gem'}, glow_keys='G')})
    crown.cube((-3.15, 0.6, 2.35), (6.3, 0.7, 0.3), **gold)
    for sx in (1, -1):
        crown.cube((3.0 if sx > 0 else -3.3, 0.6, -2.85), (0.3, 0.7, 5.2), **gold)
    halo = crown.part('halo', pivot=(0, -1.0, 3.4))
    for (x, y, w, h) in ((-4, -4.4, 8, 0.8), (-4, 3.6, 8, 0.8), (-4.4, -4, 0.8, 8), (3.6, -4, 0.8, 8)):
        halo.cube((x, y, 0), (w, h, 0.5), color='halo', pattern='mc', clusters=0.0, rim=False, glow=True)
    for (x, y) in ((-0.4, -6.0), (-0.4, 5.2), (-6.0, -0.4), (5.2, -0.4)):  # four rays
        halo.cube((x, y, 0.1), (0.8, 0.8, 0.3), color='halo_l', pattern='mc', clusters=0.0, rim=False, glow=True)

    # ---- arms resting on the knees, sleeves edged in gold; legs crossed in front, webbed feet up
    for side, sx in (('left', 1), ('right', -1)):
        arm = m.part(f'front_{side}_leg', pivot=(3.7 * sx, 11.8, -1.0), rot=(-0.55, 0, 0.18 * sx))
        arm.cube((-1.3, 0, -1.3), (2.6, 3.4, 2.6), **robe, bands=[(2, 'gold')], faces={'down': dict(skip=True)})
        arm.cube((-0.9, 3.2, -0.9), (1.8, 2.2, 1.8), **fur)
        arm.cube((-1.2, 5.2, -1.6), (2.4, 0.6, 2.4), color='web', pattern='mc', clusters=0.2)
        leg = m.part(f'back_{side}_leg', pivot=(2.2 * sx, 17.6 - 0.4 * (sx > 0), -1.2), rot=(-1.42, 0.75 * sx, 0))
        leg.cube((-1.1, 0, -1.1), (2.2, 4.4, 2.2), **fur)
        # the webbed foot lies flat at the end of the crossed leg, toes forward
        leg.cube((-1.6, 3.8, -0.9), (3.2, 2.6, 0.6), color='web', pattern='mc', clusters=0.2, faces={
            'north': dict(color='web', pattern='mc', clusters=0.1, hd=True, map=['......', '......', '......', 'd.d.d.', 'd.d.d.', 'd.d.d.'],
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

