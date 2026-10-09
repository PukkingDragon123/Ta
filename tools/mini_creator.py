"""F3 Knowledge & lore: the Mini Creator, the small platypus who guides you through the Sift.

Built like a Mojang animal (reference: refzip/crops/IMG_5659_col4_1.png): a low, long platypus body on
four splayed legs with dark webbed feet, a head that runs straight into a wide, flat, dark bill, small
vanilla-style eyes, olive-brown fur in clustered tones and a broad flat tail. He wears the Creator's
ceremonial white and gold: a draped mantle over his back with a gold hem and a gold sun sigil, a gold
collar, a gold circlet with a Prism gem, and gold tassels. Four little blocks float in a ring over his
back (gold, prism, chrome and quartz) - the Creator's floating blocks in miniature.

Hooked in from one line in mobs.py. Part names are what client/knowledge/MiniCreatorModel.java animates.
Top-level imports stay light: mobs.py imports this module while it is loading.
"""
from modelkit import Model

# the head's front at res 2 (7 x 5 units = 14 x 10 texels): two small dark eyes with a lighter lid
# texel under each, high on the sides of the face, like the reference's
FACE_W, FACE_H = 14, 10


def _face(blink=False):
    g = [['.'] * FACE_W for _ in range(FACE_H)]
    for x0 in (1, 11):
        if blink:
            g[4][x0] = g[4][x0 + 1] = 'd'
        else:
            g[3][x0] = g[3][x0 + 1] = 'E'
            g[4][x0] = g[4][x0 + 1] = 'E'
            g[5][x0] = g[5][x0 + 1] = 'L'
    return [''.join(r) for r in g]


def _side_eye(blink=False):
    """The eye seen from the side: the platypus's eyes sit on the sides of its head."""
    g = [['.'] * 10 for _ in range(10)]
    if blink:
        g[4][1] = g[4][2] = 'd'
    else:
        g[3][1] = g[3][2] = g[4][1] = g[4][2] = 'E'
        g[5][1] = g[5][2] = 'L'
    return [''.join(r) for r in g]


SUN = ['...g...', '.g.G.g.', '..GGG..', 'gGGgGGg', '..GGG..', '.g.G.g.', '...g...']


def mini_creator() -> Model:
    pal = {
        'fur': '#7a6243', 'fur_l': '#8f7652', 'fur_d': '#5e4a32',
        'belly': '#9a835e', 'belly_l': '#ab9470', 'belly_d': '#7d6849',
        'bill': '#3b3330', 'bill_l': '#4d4440', 'bill_d': '#2a2421',
        'web': '#3f3530', 'web_l': '#524640', 'web_d': '#2b2420',
        'tail': '#6a5338', 'tail_l': '#7f6646', 'tail_d': '#4f3d29',
        'robe': '#ebe4d2', 'robe_l': '#f7f2e6', 'robe_d': '#c9bfa8',
        'gold': '#d9a72c', 'gold_l': '#f2cc5a', 'gold_d': '#9a7015',
        'gem': '#e05ab8', 'gem_l': '#ff9ade', 'gem_d': '#9c2f7c',
        'eye': '#16100d', 'lid': '#a08a66',
        'q': '#e8e3d8', 'q_l': '#f6f3ec', 'q_d': '#c4bcae',
        'prism': '#d870c0', 'prism_l': '#f2a6de', 'prism_d': '#9a3e86',
        'chrome': '#6fc9ae', 'chrome_l': '#a6e8d2', 'chrome_d': '#3f8f7a',
    }
    m = Model('mini_creator', (128, 64), pal, {'mini_creator': {}}, res=2, expressions=['blink'])
    fur = dict(color='fur', pattern='mc', clusters=0.55, streaks=0.2)
    robe = dict(color='robe', pattern='mc', clusters=0.35, streaks=0.05)
    gold = dict(color='gold', pattern='mc', clusters=0.4)
    eyes = {'E': 'eye', 'L': 'lid', 'd': 'fur_d'}

    body = m.part('body', pivot=(0, 19, 0))
    body.cube((-4, -3.5, -6), (8, 6, 12), **dict(fur, bands=[(5, 'belly')]))
    # the ceremonial mantle draped over his back: white with a gold hem, a gold sun on each flank
    mantle = dict(robe, bands=[(4, 'gold')])
    body.cube((-4.5, -4.25, -4.5), (9, 5, 9.5), **mantle,
              faces={'east': dict(mantle, hd=True, map=SUN, keys={'g': 'gold_d', 'G': 'gold'}),
                     'west': dict(mantle, hd=True, map=SUN, keys={'g': 'gold_d', 'G': 'gold'}),
                     'down': dict(skip=True)})
    body.cube((-0.75, -4.5, -4.5), (1.5, 0.25, 9.5), **gold, faces={'down': dict(skip=True)})       # the gold stripe down his back
    for i, (x, z) in enumerate(((-4.75, -4.5), (4.25, -4.5), (-4.75, 4.5), (4.25, 4.5))):
        tassel = body.part(f'tassel_{i}', pivot=(x + 0.25, 0.75, z), rot=(0, 0, 0))
        tassel.cube((-0.25, 0, -0.25), (0.5, 1.75, 0.5), **gold)
    tail = body.part('tail', pivot=(0, -0.5, 6), rot=(0.2, 0, 0))
    hatch = ['t.t.t.', '.t.t.t', 't.t.t.', '.t.t.t', 't.t.t.', '.t.t.t', 't.t.t.']
    tail.cube((-3, -0.75, 0), (6, 1.5, 7), color='tail', pattern='mc', clusters=0.3,
              faces={'up': dict(color='tail', pattern='mc', clusters=0.1, map=hatch, keys={'t': 'tail_d'})})

    head = body.part('head', pivot=(0, -0.5, -6))
    head.cube((-3.5, -3, -5), (7, 5, 5), **fur,
              faces={'north': dict(fur, clusters=0.3, hd=True, map=_face(), keys=eyes, expr={'blink': _face(True)}),
                     'east': dict(fur, hd=True, map=_side_eye(), keys=eyes, at=(0, 0), center=False, expr={'blink': _side_eye(True)}),
                     'west': dict(fur, hd=True, map=[r[::-1] for r in _side_eye()], keys=eyes, at=(0, 0), center=False,
                                  expr={'blink': [r[::-1] for r in _side_eye(True)]})})
    head.cube((-3.75, -0.5, -1.25), (7.5, 2.75, 1.25), **gold)                                          # the gold collar
    bill = head.part('bill', pivot=(0, 0.5, -5))
    bill.cube((-3, -0.75, -5), (6, 1.25, 5), color='bill', pattern='mc', clusters=0.45)
    bill.cube((-3.5, -0.75, -6), (7, 1.25, 1.5), color='bill', pattern='mc', clusters=0.45)                # the broad, rounded tip
    jaw = head.part('jaw', pivot=(0, 1, -5))
    jaw.cube((-2.75, -0.25, -5), (5.5, 0.75, 5), color='bill_d', pattern='mc', clusters=0.3)
    crown = head.part('crown', pivot=(0, -3, -2.5))
    crown.cube((-3, -1, -2), (6, 1, 4), **gold, faces={'north': dict(gold, map=['..GG..'], keys={'G': 'gem'}, glow_keys='G')})
    for x in (-2.5, -0.25, 2):
        crown.cube((x, -2, -2), (0.5, 1, 0.5), **gold)
    crown.cube((-0.5, -2.25, -2.25), (1, 1.25, 0.5), color='gem', pattern='mc', clusters=0.0, glow=True)

    for side, sx in (('left', 1), ('right', -1)):
        for end, z in (('front', -3.5), ('back', 3.5)):
            leg = m.part(f'{end}_{side}_leg', pivot=(3.5 * sx, 21, z), rot=(0, 0, -0.25 * sx))
            leg.cube((-1, 0, -1), (2, 2.5, 2), **fur)
            leg.cube((-1.5, 2.5, -2.5), (3, 0.5, 3.5), color='web', pattern='mc', clusters=0.3)              # the webbed foot

    for i, mat in enumerate(('gold', 'prism', 'chrome', 'q')):
        orb = m.part(f'orb_{i}', pivot=(0, 10, 0))
        spec = dict(color=mat, pattern='mc', clusters=0.5)
        if mat in ('prism', 'chrome'):
            spec['glow'] = True
        orb.cube((-1.25, -1.25, -1.25), (2.5, 2.5, 2.5), **spec)
    return m


MODELS = {'mini_creator': mini_creator}


def spawn_egg():
    """His egg: olive-brown speckled with the mantle's white and gold."""
    import items16 as I
    return I.egg(['#5e4a32', '#7a6243', '#8f7652', '#a8916a'], '#3a2c1c', {
        3: '......wwww......',
        4: '.....wwggww.....',
        5: '.....wwwwww.....',
        9: '....bbbbbbbb....',
        10: '....bbbbbbbb....',
        12: '.......g........',
    }, {'w': '#ebe4d2', 'g': '#d9a72c', 'b': '#3b3330'})
