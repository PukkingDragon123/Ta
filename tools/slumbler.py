"""S2: the Slumbler, its stingray-like tadpole and its eggs (geometry, hand-painted textures, item art).

The Slumbler is a big, sleepy amphibian of the Sift swamps, sitting up like a toad: a heavy, soft
pear of a body sagging onto the ground, a broad flat head with a wide mouth line and heavy-lidded
eyes, three frilled coral gills fanning behind each cheek, stubby webbed arms and frog-folded hind
legs with long toes, a thick tail with a low crest. Its moist blue-grey skin is mottled lavender and
teal with indigo blotches, wet highlights and a pale belly; its mouth is red. Everything is painted
by hand at vanilla density (tools/handpaint.py), one texel per model unit.

The tadpole is a little ray of the same skin: a flat rounded disc with wing-fins, eyes on top and a
whip of a tail. The eggs are clear jelly with dark curled tadpoles inside.

Hooks (one line each): mobs.slumbler and mobs.ALL (the models), gen_assets.generate (sounds, loot,
tags, text), gen_textures.main (the Chrome tadpole bucket) and items16.all_items (item sprites)."""
import math

import numpy as np

import handpaint as HP
from modelkit import Model

# ---------------------------------------------------------------- the family palette

SKIN = ('#262746', '#353e6a', '#485c8a', '#5f7aa7', '#7d99c3', '#a3bfdc', '#cfe4ef')
LAVENDER = ('#2d2452', '#423772', '#5a4f92', '#7268b0', '#8f87ca', '#b2abdf', '#d8d2f0')
TEAL = ('#1e3448', '#2a4d63', '#3b6b7f', '#52889a', '#6fa6b3', '#97c6cb', '#c6e5e2')
BELLY = ('#56637e', '#71839a', '#93a8b0', '#b5c8c0', '#d1dfcf', '#e6eedd', '#f5f8ec')
MOUTH = ('#2e0717', '#4c0f22', '#6e1a2c', '#922a35', '#b33f40', '#cf5e54', '#e88a74')
TONGUE = ('#5c1426', '#7e2236', '#a23646', '#c14c55', '#d96a6a', '#ec8d82', '#f8b6a4')
GILL = ('#3d1a40', '#652a55', '#90405f', '#b85867', '#d97677', '#ef9c91', '#fbc9b4')
BONE = ('#5d5866', '#7f7a82', '#a39b98', '#c4baab', '#ddd3c0', '#eee7d6', '#fbf7ec')
INDIGO = ('#1b1a37', '#272950', '#35396a', '#464f85', '#5d68a0', '#7c89bb', '#a6b2d5')
VIOLET = ('#211b41', '#30285b', '#433978', '#574d93', '#7065ad', '#9086c6', '#b6addc')
SPOT = ('#2b5867', '#3d7a88', '#559ba6', '#74b9be', '#9cd5d2', '#c3ebe2', '#e6faf2')
GLOSS = '#e4f6f4'
EYE = {'P': '#120e1e', 'I': '#e0a23a', 'i': '#a8661f', 'H': '#fff6d2', 'l': 0.7, 'm': '#3a0b1c', 'n': 0.55}


def materials():
    M = HP.Material
    return {
        'skin': M([SKIN, LAVENDER, TEAL], base=0.55, noise=0.1, cell=2.0, mottle=(3.0, 0.62), mottle2=(2.4, 0.72), belly=BELLY, gloss=GLOSS,
                  gloss_rate=0.035, spots=(SPOT, 1.8, 0.8, 0), back=INDIGO, back2=VIOLET),
        'belly': M([BELLY, BELLY], base=0.66, noise=0.07, cell=2.0, mottle=None, mottle2=None, gloss=GLOSS, gloss_rate=0.02, ao=0.6),
        'mouth': M([MOUTH], base=0.5, noise=0.06, cell=1.6, rim=0.3, ao=0.6, sym=False),
        'tongue': M([TONGUE], base=0.55, noise=0.06, cell=1.6, rim=0.3, ao=0.5),
        'gill': M([GILL], base=0.55, noise=0.05, cell=1.5, rim=0.0, ao=0.0, mottle=None),
        'bone': M([BONE], base=0.62, noise=0.05, cell=1.5, rim=0.6, ao=0.4),
        'web': M([VIOLET, LAVENDER], base=0.45, noise=0.06, cell=1.6, rim=0.0, ao=0.0, mottle=(2.4, 0.6)),
    }


def _folds(ctx):
    """Soft skin folds across a pale throat or chest: every third row a shade darker."""
    v = ctx['jj'] if ctx['face'] not in ('up', 'down') else ctx['ii']
    return np.where(v % 3 == 2, -0.12, 0.0)


def _gill_tone(ctx):
    """A frilled gill frond: a dark rib along its middle, darker roots, pale tips."""
    loc = ctx['loc']
    t = np.clip(np.abs(loc[..., 0]) / 9.0, 0, 1)
    rib = np.where(np.abs(loc[..., 1]) < 0.6, -0.18, 0.0)
    return (t - 0.45) * 0.6 + rib


def _gill_shape(length, half):
    """Feathery outline of a frond: wide at the root, tapering, the edges combed into filaments."""
    def keep(lx, ly, lz):
        x = np.abs(lx)
        w = half * (1.0 - 0.5 * x / length) + 0.2
        comb = ((np.floor(x) % 2) == 1) & (np.abs(ly) > w - 1.0) & (x > 1)
        return (np.abs(ly) < w) & ~comb & (x < length)
    return keep


def _crest_shape(length, height):
    def keep(lx, ly, lz):
        top = height * (0.5 + 0.5 * np.abs(np.sin((lz + 0.5) * math.pi / 3.0)))
        return (-ly) < top
    return keep


def slumbler() -> Model:
    """See the module notes. Parts the Java model animates: body (the rump), chest, head, jaw, throat,
    the cheeks, eyes and eyelids, three gills a side, arms (arm, forearm, hand), hind legs (leg, foot)
    and the tail (three segments)."""
    m = Model('slumbler', (128, 128), {}, {'slumbler': {}}, res=1, detail=1)
    m.material_pass = False
    S = dict(mat='skin')
    # ---- the rump: a heavy pear sagging onto the ground, rounded from layered boxes
    body = m.part('body', pivot=(0, 18, 5))
    body.cube((-9, -5, -8), (18, 11, 15), back=-1, belly_y=21.5, **S)
    body.cube((-10, -2, -6), (20, 7, 11), back=-1, belly_y=21.5, **S)
    body.cube((-7, -7, -6), (14, 2, 11), back=99, **S)
    body.cube((-7, -3, 7), (14, 8, 2), back=0, belly_y=21.5, **S)
    body.cube((-2, -8, -3), (4, 1, 4), back=99, **S)
    # ---- the chest, rising from the rump like a toad sitting up
    chest = body.part('chest', pivot=(0, -3, -6), rot=(-0.75, 0, 0))
    chest.cube((-8, -7, -12), (16, 12, 13), back=-3, faces={'north': dict(mat='belly', tone=_folds)}, **S)
    chest.cube((-9, -4, -10), (18, 8, 9), back=-2, belly=2.5, **S)
    chest.cube((-2, -8, -8), (4, 1, 4), back=99, **S)
    # ---- the head: broad and flat, a wide mouth line all round, eyes on top
    head = chest.part('head', pivot=(0, -4, -12), rot=(0.8, 0, 0))
    lip = ['m' * 20]
    head.cube((-10, -5, -13), (20, 5, 14), back=-3, faces={'down': dict(mat='mouth', light=-0.1),
                                                           'east': dict(decal=['n' + '.' * 13], keys=EYE, at=(0, 4)),
                                                           'west': dict(decal=['.' * 13 + 'n'], keys=EYE, at=(0, 4))}, **S)
    head.cube((-8, -6, -11), (16, 1, 11), back=99, **S)
    head.cube((-9, -4, -14), (18, 4, 1), back=-3, faces={'down': dict(mat='mouth'),
                                                         'north': dict(decal=['.....n......n.....'], keys=EYE, at=(0, 1))}, **S)
    jaw = head.part('jaw', pivot=(0, 0, 0))
    jaw.cube((-9, 0, -13), (18, 3, 13), belly=1.0, faces={'up': dict(mat='mouth'), 'north': dict(decal=['m' * 18], keys=EYE),
                                                          'east': dict(decal=['m' * 12 + 'n'], keys=EYE), 'west': dict(decal=['n' + 'm' * 12], keys=EYE)}, **S)
    jaw.cube((-4, -0.5, -12), (8, 1, 9), mat='tongue', faces={'down': dict(skip=True)})
    throat = jaw.part('throat', pivot=(0, 3, -6))
    throat.cube((-6, 0, -5), (12, 2, 9), mat='belly', tone=_folds)
    for side, sx in (('left', 1), ('right', -1)):
        out = 'east' if sx > 0 else 'west'
        cheek = head.part(f'{side}_cheek', pivot=(9.5 * sx, -1, -5))
        cheek.cube(((0 if sx > 0 else -2), -2, -3), (2, 4, 6), back=-1, **S)
        eye = head.part(f'{side}_eye', pivot=(6 * sx, -5, -10))
        eye.cube((-2.5, -3, -2), (5, 3, 4), back=99, **S, faces={
            'north': dict(decal=['.....', '.HPI.' if sx > 0 else '.IPH.', '.iPi.'], keys=EYE),
            out: dict(decal=['....', 'Ii..', 'ii..'], keys=EYE, mirror=sx < 0)})
        lid = eye.part(f'{side}_eyelid', pivot=(0, -3, 0))  # the model scales it down over the eye
        lid.cube((-2.5, 0, -2), (5, 2, 4), inflate=0.2, back=99, **S, faces={'north': dict(decal=['lllll'], keys=EYE, at='bottom'),
                                                                                 out: dict(decal=['llll'], keys=EYE, at='bottom')})
        for i, (y, ry, rz, ln) in enumerate(((-4.0, 0.7, -0.6, 9), (-2.0, 0.85, -0.05, 10), (0.0, 0.7, 0.5, 8))):
            g = head.part(f'{side}_gill_{i}', pivot=(10 * sx, y, -3), rot=(0, -ry * sx, rz * sx))
            g.cube(((0 if sx > 0 else -ln), -2, 0), (ln, 4, 0), mat='gill', tone=_gill_tone, shape=_gill_shape(ln, 2.3), no_occlude=True)
        # ---- arms: stubby, webbed, reaching down from the chest
        arm = chest.part(f'{side}_arm', pivot=(7.5 * sx, 1, -9), rot=(0.75, 0, 0))
        arm.cube((-2, -1, -2), (4, 6, 4), back=1, **S)
        fore = arm.part(f'{side}_forearm', pivot=(0, 5, 0))
        fore.cube((-1.5, 0, -1.5), (3, 5, 3), **S)
        hand = fore.part(f'{side}_hand', pivot=(0, 5, 0))
        hand.cube((-3, 0, -4), (6, 1, 5), back=99, **S, faces={'down': dict(mat='belly')})
        for tx in (-2.5, 0, 2.5):
            hand.cube((tx - 0.5, 0, -6), (1, 1, 2), back=99, **S, faces={'north': dict(mat='bone')})
        # ---- hind legs: a big haunch on each flank, a knee, a long webbed foot on the ground
        leg = body.part(f'{side}_leg', pivot=(9 * sx, 0, 1))
        leg.cube(((-1 if sx > 0 else -4), -4, -5), (5, 8, 9), back=-1, belly_y=21.5, **S)
        leg.cube(((0 if sx > 0 else -4), 0, -7), (4, 4, 3), back=1, belly_y=21.5, **S)
        foot = leg.part(f'{side}_foot', pivot=(2 * sx, 5, -6))
        foot.cube((-3, 0, -5), (6, 1, 6), back=99, **S, faces={'down': dict(mat='belly')})
        for tx in (-2.5, -0.5, 1.5, 3.5):
            foot.cube(((tx if sx > 0 else -tx - 1) - 0.5, 0, -8), (1, 1, 3), back=99, **S, faces={'north': dict(mat='bone')})
    # ---- the tail: thick at the root, curling round on the ground, a low frilled crest
    t1 = body.part('tail1', pivot=(0, 2, 8), rot=(0, 0.25, 0))
    t1.cube((-5, -3, 0), (10, 7, 7), back=-1, belly_y=21.5, **S)
    t2 = t1.part('tail2', pivot=(0, 1, 7), rot=(0, 0.35, 0))
    t2.cube((-4, -2, 0), (8, 5, 6), back=-0.5, belly_y=21.5, **S)
    t3 = t2.part('tail3', pivot=(0, 1, 6), rot=(0, 0.4, 0))
    t3.cube((-2.5, -1, 0), (5, 3, 6), back=0, belly_y=21.5, **S)
    t2.cube((0, -4, 0), (0, 2, 6), mat='web', shape=_crest_shape(6, 2), no_occlude=True)
    t3.cube((0, -3, 0), (0, 2, 6), mat='web', shape=_crest_shape(6, 2), no_occlude=True)
    return HP.use(m, materials(), 'slumbler')


# ---------------------------------------------------------------- the tadpole and its eggs (mobs.ALL)

JELLY = ('#5f8fa6', '#7fb0c2', '#a2cdd8', '#c2e3e8', '#dcf2f2', '#eefaf8', '#fbfffe')


def _round_plane(w, d):
    """A plane rounded off at its free corners (a ray's wing tip, a fin)."""
    def keep(lx, ly, lz):
        u = np.abs(lx) / max(w, 1e-6)
        v = (lz - lz.min() + 0.5) / max(d, 1e-6) * 2 - 1 if lz.size else lz
        return (u * u + v * v * 0.8) < 1.15
    return keep


def slumbler_tadpole() -> Model:
    """The Slumbler's tadpole: a little ray in its parents' skin (SlumblerTadpoleModel animates it) -
    a flat rounded disc with a pale belly, eyes on a low hump, wing-fins (an inner half and a rounded
    outer half that ripples), two snout lobes, little pelvic fins and a whip of a tail with a crest."""
    m = Model('slumbler_tadpole', (64, 32), {}, {'slumbler_tadpole': {}}, res=1, detail=1)
    m.material_pass = False
    S = dict(mat='skin', back=99)
    body = m.part('body', pivot=(0, 22, 0))
    body.cube((-3, -1, -4), (6, 2, 8), **S, faces={'down': dict(mat='belly')})
    body.cube((-2, -2, -3), (4, 1, 5), **S)
    for side, sx in (('left', 1), ('right', -1)):
        body.cube(((1 if sx > 0 else -2), -3, -3), (1, 1, 1), **S, faces={'up': dict(decal=['I'], keys=EYE), 'north': dict(decal=['P'], keys=EYE)})
        wing = body.part(f'{side}_wing', pivot=(3 * sx, 0, 0))
        wing.cube(((0 if sx > 0 else -3), -1, -3), (3, 1, 6), **S, faces={'down': dict(mat='belly')})
        tip = wing.part(f'{side}_wing_tip', pivot=(3 * sx, 0, 0))
        tip.cube(((0 if sx > 0 else -3), -0.5, -2), (3, 0, 4), mat='web', shape=_round_plane(3, 4), no_occlude=True)
        pel = body.part(f'{side}_pelvic', pivot=(2 * sx, 0, 3), rot=(0, 0.5 * sx, 0))
        pel.cube(((0 if sx > 0 else -2), 0, 0), (2, 0, 2), mat='web', shape=_round_plane(2, 2), no_occlude=True)
    snout = body.part('snout', pivot=(0, 0, -4))
    snout.cube((-2, -1, -1), (4, 2, 1), **S, faces={'north': dict(decal=['....', '.mm.'], keys=EYE), 'down': dict(mat='belly')})
    for side, sx in (('left', 1), ('right', -1)):
        lobe = snout.part(f'{side}_lobe', pivot=(1.5 * sx, 0, -1), rot=(0, 0.3 * sx, 0))
        lobe.cube((-0.5, -0.5, -2), (1, 1, 2), **S)
    t1 = body.part('tail1', pivot=(0, -0.5, 4))
    t1.cube((-1, -0.5, 0), (2, 1, 4), **S)
    t2 = t1.part('tail2', pivot=(0, 0, 4))
    t2.cube((-0.5, -0.5, 0), (1, 1, 4), **S)
    t3 = t2.part('tail3', pivot=(0, 0, 4))
    t3.cube((-0.5, -0.5, 0), (1, 1, 3), **S)
    t3.cube((0, -2, 0), (0, 2, 3), mat='web', shape=_crest_shape(3, 2), no_occlude=True)
    return HP.use(m, materials(), 'slumbler_tadpole')


def slumbler_eggs() -> Model:
    """A clutch of Slumbler eggs (SlumblerEggsModel wobbles it): a raft of clear jelly eggs, each with
    a dark little tadpole curled inside. The tadpole is each egg's first cube and the jelly a child drawn
    after it, so it shows through the translucent jelly."""
    m = Model('slumbler_eggs', (64, 32), {}, {'slumbler_eggs': {}}, res=1, detail=1)
    m.material_pass = False
    raft = m.part('raft', pivot=(0, 24, 0))
    spots = ((0, 0, 0, 4), (3.5, 0.5, 1, 3), (-3.5, 0.5, 1, 3), (1, 0.5, -3.5, 3), (-2, 0.5, 3.5, 3), (2.5, 0.5, 4, 3), (-3, 0.5, -3, 3))
    for i, (x, y, z, d) in enumerate(spots):
        egg = raft.part(f'egg_{i}', pivot=(x, y - d / 2, z))
        egg.cube((-1, -0.5, -1), (2, 1, 2), mat='skin', back=99, ao=0)
        jelly = egg.part(f'jelly_{i}')
        jelly.cube((-d / 2, -d / 2, -d / 2), (d, d, d), mat='jelly', opacity=130, ao=0, no_occlude=True)
    mats = materials()
    mats['jelly'] = HP.Material([JELLY], base=0.62, noise=0.05, cell=1.5, rim=1.0, ao=0.0, mottle=None, gloss='#ffffff', gloss_rate=0.12)
    return HP.use(m, mats, 'slumbler_eggs')


EXTRA = {'slumbler_tadpole': slumbler_tadpole, 'slumbler_eggs': slumbler_eggs}
MODELS = {'slumbler': slumbler}


def textures(out):
    """Writes the Chrome Bucket of Slumbler Tadpole (its Chrome turning through the rainbow like the
    other Chrome fish buckets)."""
    import chrome as C
    with _bucket_layer():
        out('item/chrome_slumbler_tadpole_bucket', C.strip(C.bucket_frames('slumbler_tadpole')), C.anim(4))


# ---------------------------------------------------------------- item sprites (items16.all_items)

def _mirror(half_rows):
    """16-wide rows from their left halves (vanilla eggs are drawn symmetric, then shaded from the top left)."""
    flip = str.maketrans('<>', '><')
    return [h + h[::-1].translate(flip) for h in half_rows]


def _shade(rows, body, lit, dark, x_lit=6, y_lit=7, x_dark=11, y_dark=11):
    """Light from the top left: body texels up and left turn `lit`, down and right turn `dark`."""
    out = []
    for y, r in enumerate(rows):
        r = list(r)
        for x, ch in enumerate(r):
            if ch == body:
                if x < x_lit and y < y_lit and (x + y) < x_lit + y_lit - 3:
                    r[x] = lit
                elif x >= x_dark or y >= y_dark:
                    r[x] = dark
        out.append(''.join(r))
    return out


def _sprite(rows, pal):
    from PIL import Image
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    assert len(rows) == 16
    for y, r in enumerate(rows):
        assert len(r) == 16, (y, r)
        for x, ch in enumerate(r):
            if ch != '.':
                c = HP.hx(pal[ch])
                px[x, y] = c + (255,)
    return img


def slumbler_egg():
    """The Slumbler's spawn egg in vanilla's per-mob style: its broad head with the eyes on top, a
    coral gill frond out of each side, the wide red mouth and the pale belly, outlined in deep indigo."""
    rows = _mirror([
        '........',
        '........',
        '....aa..',
        'rr.aIPa.',
        'grrabbaa',
        '.ggabsbb',
        'rrgabbbb',
        'gg.abbbb',
        '..aammmm',
        '..abkkkk',
        '..abkkkk',
        '...abkkk',
        '...abkkk',
        '....atkk',
        '.....aaa',
        '........',
    ])
    rows = _shade(rows, 'b', 'c', 'd')
    rows = _shade(rows, 'k', 'K', 'l', x_lit=7, y_lit=11, x_dark=11, y_dark=12)
    pal = {'a': '#1b1a37', 'b': '#5d68a0', 'c': '#7c89bb', 'd': '#464f85', 's': '#9cd5d2', 'I': '#e0a23a', 'P': '#120e1e',
           'r': '#652a55', 'g': '#d97677', 'm': '#922a35', 'k': '#b5c8c0', 'K': '#d1dfcf', 'l': '#93a8b0', 't': '#ddd3c0'}
    return _sprite(rows, pal)


def tadpole_egg():
    """The tadpole's spawn egg: a little ray seen from above - the hump with its two gold eyes, wide
    wing-fins, pale spots, and the whip of a tail curling below."""
    rows = _mirror([
        '........',
        '........',
        '........',
        '......aa',
        '....aabb',
        '...abIPb',
        '..abbbbs',
        '.abbsbbb',
        'abbbbbbb',
        '.awwbbbb',
        '..aawbbb',
        '....aabb',
        '......aa',
        '.......a',
        '........',
        '........',
    ])
    rows[12] = '......aata......'
    rows[13] = '.......at.......'
    rows[14] = '......aa........'
    rows = _shade(rows, 'b', 'c', 'd', x_lit=7, y_lit=8, x_dark=11, y_dark=10)
    pal = {'a': '#1b1a37', 'b': '#5d68a0', 'c': '#7c89bb', 'd': '#464f85', 's': '#9cd5d2', 'I': '#e0a23a', 'P': '#120e1e',
           'w': '#7065ad', 't': '#574d93'}
    return _sprite(rows, pal)


def bulb_egg():
    """S2: the Bulb's spawn egg in vanilla's per-mob style (a rabbit-egg silhouette): its pale blue
    jelly with the two ears, the little dark violet eyes and mouth, and the cyan glow in its lower
    half, outlined in the Bulb's navy."""
    rows = _mirror([
        '....aa..',
        '...acb..',
        '...acb..',
        '...abba.',
        '...abbbb',
        '..abbbbb',
        '..abbbbb',
        '..abbEbb',
        '.abbbbbm',
        '.aggggg.',
        '.agggggg',
        '.agggggg',
        '..aggggg',
        '...agggg',
        '....aaaa',
        '........',
    ])
    rows[1] = rows[1][:8] + '..abca...'[:8]
    rows[2] = rows[2][:8] + '..abca...'[:8]
    rows[9] = '.aghhhgggggggia.'
    rows[10] = '.aghjhggggggiia.'
    rows[11] = '.aghhgggggggiia.'
    rows = _shade(rows, 'b', 'c', 'e', x_lit=7, y_lit=8, x_dark=12, y_dark=9)
    rows = [r.replace('ba.', 'ea.').replace('bba', 'bea') if y in (3, 4, 5, 6, 7) else r for y, r in enumerate(rows)]
    pal = {'a': '#243d6d', 'b': '#71b6f1', 'c': '#9ac5f1', 'e': '#4f86c8', 'E': '#3b3478', 'm': '#6e64ad',
           'g': '#5dc6f1', 'h': '#83e0f3', 'j': '#d8fdff', 'i': '#3f97cf'}
    return _sprite(rows, pal)


def glowing_slime_ball():
    """S2: the Bulb's drop - vanilla's slime ball in the Bulb's colours, pale blue jelly glowing cyan
    from a bright core (same shape and shading as the vanilla sprite, recoloured texel by texel)."""
    rows = ['................',
            '................',
            '......aaaa......',
            '....aabccbad....',
            '...abceecebfd...',
            '...acgheeecbd...',
            '..abehhjeeccfd..',
            '..abeejjjccbid..',
            '..abcejjecbfid..',
            '..aibcecccifid..',
            '...aibccbiffd...',
            '...dfiiffffid...',
            '....ddiiiidd....',
            '......dddd......',
            '................',
            '................']
    pal = {'a': '#3f86c8', 'b': '#5dc6f1', 'c': '#69d8f0', 'd': '#243d6d', 'e': '#a3f0ff', 'f': '#3f97cf', 'g': '#ffffff',
           'h': '#d8fdff', 'i': '#4aaee0', 'j': '#c8fbff'}
    return _sprite(rows, pal)


# the tadpole peeking out of a bucket: a wing-fin curling up out of the water, the whip of its tail
# hanging over the rim (16 x 7, laid over the bucket's mouth like the music fish)
_OL = '#1b1a37'
TADPOLE_IN_BUCKET = ([
    '.......wW.......',
    '......wWa.......',
    '..e.wBBbBba.....',
    '.oBBbsBBbBBv....',
    '..pppppppBBttt..',
    '............tg..',
    '................',
], {'w': ('#7065ad', _OL), 'W': ('#9086c6', _OL), 'a': ('#433978', _OL), 'e': '#e0a23a', 'o': ('#5d68a0', _OL),
    'B': ('#5d68a0', _OL), 'b': ('#464f85', _OL), 's': ('#9cd5d2', _OL), 'v': ('#35396a', _OL), 'p': ('#b5c8c0', '#56637e'),
    't': ('#464f85', _OL), 'g': ('#574d93', _OL)})


class _bucket_layer:
    """Lends the tadpole to the music fishes' bucket painters (tools/fish_items.py, tools/chrome.py) for one call."""

    def __enter__(self):
        import fish_items as FI
        FI.BUCKET_FISH['slumbler_tadpole'] = TADPOLE_IN_BUCKET
        return self

    def __exit__(self, *exc):
        import fish_items as FI
        FI.BUCKET_FISH.pop('slumbler_tadpole', None)
        return False


def slumbler_gill():
    """A Slumbler's gill: a frilled coral plume on a pale stalk, its filaments combed off the rib like a
    feather's barbs. Light from the top left like every vanilla item."""
    import items16 as I
    rows = ['................',
            '.......l.l......',
            '.....l.glg.l....',
            '....lgGggGggl...',
            '...lGggGggGgg...',
            '..lggGggGggrg...',
            '..gGggGggGrgd...',
            '..ggGggGgrGd....',
            '...ggGggrgd.....',
            '....dgGrdd......',
            '.....ddbd.......',
            '......bB........',
            '.....bB.........',
            '....bB..........',
            '...bb...........',
            '................']
    ol = '#3d1a40'
    pal = {'l': (GILL[5], ol), 'G': (GILL[4], ol), 'g': (GILL[3], ol), 'd': (GILL[2], ol), 'r': (BELLY[4], ol),
           'b': (BELLY[2], ol), 'B': (BELLY[4], ol)}
    return I.grid(rows, pal, ol=True)


def items():
    import fish_items as FI
    o = {'slumbler_gill': slumbler_gill(), 'slumbler_spawn_egg': slumbler_egg(), 'slumbler_tadpole_spawn_egg': tadpole_egg(),
         # S2: the Bulb is final; only its egg and its drop are redrawn here
         'bulb_spawn_egg': bulb_egg(), 'glowing_slime_ball': glowing_slime_ball()}
    with _bucket_layer():
        o['slumbler_tadpole_bucket'] = FI.water_bucket('slumbler_tadpole')
    return o


# ---------------------------------------------------------------- sounds, loot, tags and text (gen_assets.generate)

# ---------------------------------------------------------------- CR2: sounds, loot, tags and text (gen_assets.generate)

SOUNDS = {
    'entity.slumbler.eat': [('mob/frog/eat1', 1.0, 0.6), ('mob/frog/eat2', 1.0, 0.55), ('mob/frog/eat3', 1.0, 0.6)],
    'entity.slumbler.spit': [('mob/llama/spit1', 1.0, 0.7), ('mob/llama/spit2', 1.0, 0.65), ('mob/dolphin/splash1', 0.6, 1.2)],
    'entity.slumbler.shake': [('mob/wolf/shake', 1.0, 0.6), ('mob/dolphin/splash2', 0.7, 0.9)],
    'entity.slumbler.lay': [('mob/frog/lay_spawn1', 1.0, 0.8), ('mob/frog/lay_spawn2', 1.0, 0.75)],
    'entity.chrome_spit.splash': [('mob/dolphin/splash1', 0.9, 1.3), ('mob/dolphin/splash2', 0.9, 1.4), ('mob/dolphin/splash3', 0.9, 1.35)],
    'entity.slumbler_eggs.hatch': [('block/frogspawn/hatch1', 1.0, 0.9), ('block/frogspawn/hatch2', 1.0, 0.95), ('block/frogspawn/hatch3', 1.0, 0.9)],
    'entity.slumbler_tadpole.bite': [('mob/axolotl/attack1', 0.8, 1.4), ('mob/axolotl/attack2', 0.8, 1.5)],
    'entity.slumbler_tadpole.hurt': [('mob/tadpole/hurt1', 1.0, 0.9), ('mob/tadpole/hurt2', 1.0, 0.9), ('mob/tadpole/hurt3', 1.0, 0.85)],
    'entity.slumbler_tadpole.death': [('mob/tadpole/death1', 1.0, 0.9), ('mob/tadpole/death2', 1.0, 0.85)],
    'entity.slumbler_tadpole.flop': [('entity/fish/flop1', 0.8, 1.2), ('entity/fish/flop2', 0.8, 1.25), ('entity/fish/flop4', 0.8, 1.2)],
    # crash cymbals: a splashy wash of breaking glass over a ringing bell
    'entity.slumbler_tadpole.crash': [('random/glass1', 0.7, 1.35), ('random/glass2', 0.7, 1.4), ('random/glass3', 0.7, 1.3),
                                      ('block/bell/bell_use01', 0.4, 1.9)],
}
SUBTITLES = {
    'entity.slumbler.eat': 'Slumbler gulps a fish', 'entity.slumbler.spit': 'Slumbler spits Chrome', 'entity.slumbler.shake': 'Slumbler shakes itself dry',
    'entity.slumbler.lay': 'Slumbler lays eggs', 'entity.chrome_spit.splash': 'Chrome splashes', 'entity.slumbler_eggs.hatch': 'Slumbler eggs hatch',
    'entity.slumbler_tadpole.bite': 'Slumbler tadpole bites', 'entity.slumbler_tadpole.hurt': 'Slumbler tadpole hurts',
    'entity.slumbler_tadpole.death': 'Slumbler tadpole dies', 'entity.slumbler_tadpole.flop': 'Slumbler tadpole flops',
    'entity.slumbler_tadpole.crash': 'Cymbals crash',
}


def assets(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)
    tag, rl = GA.tag, GA.rl
    tag('entity_type', 'minecraft:aquatic', rl('slumbler_tadpole'))
    tag('entity_type', f'{GA.NS}:chrome_dwellers', rl('slumbler_tadpole'))
    import gen_data as GD
    GD.table('entity', 'entities/slumbler_tadpole', [GD.pool([GD.item('minecraft:prismarine_crystals', count=(0, 1))], condition=GD.chance(0.3))])
    GA.LANG.update({
        f'entity.{GA.NS}.slumbler_tadpole': 'Slumbler Tadpole',
        f'entity.{GA.NS}.slumbler_eggs': 'Slumbler Eggs',
        f'entity.{GA.NS}.chrome_spit': 'Chrome Spit',
        f'band.{GA.NS}.instrument.crash_cymbals': 'Crash Cymbals',
        f'codex.{GA.NS}.slumbler_tadpole.title': 'Slumbler Tadpole',
        f'codex.{GA.NS}.slumbler_tadpole.tagline': 'Aggressive - tame it with fish and drums',
        f'codex.{GA.NS}.slumbler_tadpole.body': 'Feed two Slumblers fish and they lay a clutch of jelly eggs on the Chrome; a few minutes later '
                                                'two to four tadpoles swim out - little stingrays in their parents\' blue-grey skin, with gold '
                                                'eyes and a whip of a tail. They hunt and eat fish, and bite anyone swimming in their pool. Catch '
                                                'one in a Chrome Bucket or a water bucket like any fish. Feed one plenty of fish and it stops '
                                                'biting you; then play it a drum, and it is yours: it follows you through the water and joins your '
                                                'band on crash cymbals.',
    })

