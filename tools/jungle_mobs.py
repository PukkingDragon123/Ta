"""P4 Cave Jungle: the creatures of the Cave Jungle - geometry and high-resolution skins in the Sculk-mob pipeline
(modelkit res=2 + the auto x2 detail + the C2 material pass, finished by tools/aquakit.py like the water creatures).

* Glow Fly (`glow_fly`): a big furry firefly with a lantern of an abdomen - the light gland - feathery antennae,
  amber compound eyes, hard wing cases over two pairs of veined, see-through wings (GlowFlyModel).
* Crocotodo (`crocotodo`): the flightless bird of the Cave Jungle - a plump dodo in moss-teal feathers with a
  crocodile's long, toothed snout, armoured scutes down its back, a flame crest and a fan tail (CrocotodoModel).
* Mantis (`mantis`): a towering praying mantis of jungle-green chitin, glowing azure crystals growing from its
  back, neck and arms, leaf-veined wings, a triangular head of glowing eyes and two huge serrated scythes.
* Colossus Ponder (`colossus_ponder`): a giant frog wearing the jungle floor - a carpet of glowing Lumen Moss,
  ferns, vines and glowcaps on its back - with amber eyes on domes, a pale throat sac and webbed feet.
* Ponder Tadpole (`ponder_tadpole`): its young - a big-headed tadpole with a mouth full of needle teeth,
  budding legs and a see-through fin tail.
* Cruncher (`cruncher`): a small cave raptor plated in creamy Magnesite, dusted with glittering Magnesium,
  with a crushing jaw of fangs and molars.

Part names and pivots are the Java models' contract (client/model/<Mob>Model.java animate them).
Hooked in from mobs.py (ALL.update(MODELS)).
"""
import math

import numpy as np

import aquakit as AK
import waterfolk as WF
from modelkit import Model

M = AK.Mat
up = AK.up

# ---------------------------------------------------------------- Cave Jungle tones (lush greens, glowing moss, humid dark rock)
CHITIN_OLIVE = ('#141410', '#1f1f16', '#2b2b1d', '#3a3926', '#4b4930', '#605c3c', '#7a744c')
LANTERN = ('#7c5a0a', '#a47c10', '#cca01a', '#ecc22c', '#ffde4c', '#fff08a', '#fffbd0')
LANTERN_LIGHT = ('#f6c72a', '#ffe45c', '#fff3a0', '#fffde4')
FUZZ = ('#3c3018', '#544420', '#6e5a2a', '#8a7236', '#a68c46', '#c2a85c', '#dcc67c')
WING = ('#2c3a22', '#3c4e2e', '#506640', '#688054', '#849c6c', '#a2b888', '#c6d8aa')
MOSS_GLOW = ('#2fd8c8', '#62f0dc', '#a4fbea', '#e2fff8')
LUMEN = ('#0c3238', '#12474e', '#1a6068', '#237a82', '#2f969e', '#45b4b8', '#7ad8d4')
LEAF = ('#0f2214', '#16321c', '#1f4626', '#2a5c30', '#38743c', '#4c8e4a', '#68aa5e')
FERN = ('#123018', '#1a4220', '#24582a', '#307034', '#408a40', '#56a450', '#78c068')
CREAM = WF.CREAM
BONE = WF.BONE
MOUTH = WF.MOUTH
EYE = dict(WF.EYE)
EYE.update({'A': '#f2a01e', 'B': '#ffd060', 'C': '#7a3c08', 'R': '#c8361c', 'Y': '#fff4b0', 'E': '#3a1c06', 'n': '#05070a',
            'Q': '#9ff4ff', 'q': '#3cc8e0', 'Z': '#e8fbff'})


def _mask_hash(c, rate, salt):
    wp = np.floor(c['wp'] * 2 + 0.5).astype(np.int64)
    return AK._hash3(wp[..., 0], wp[..., 1], wp[..., 2], salt) < rate


def _below(at):
    return lambda c: c['loc'][..., 1] > at


def _above(at):
    return lambda c: c['loc'][..., 1] < at


def _leaf(length, width, point=0.15):
    """A leaf / wing outline on a horizontal plane running back along +z (or a vertical one along -y)."""
    def keep(lx, ly, lz):
        flat = np.ptp(ly) == 0
        a = lz - lz.min() if flat else -(ly - ly.max())
        side = np.abs(lx) if flat else np.abs(lz - (lz.min() + lz.max()) / 2) * 2
        t = np.clip((a + 0.5) / max(length, 1e-6), 0, 1)
        half = width * np.sin(np.clip(t, 0, 1) * math.pi) ** 0.7 * (1 - point * t)
        return (side < half + 0.25) & (t < 1.0)
    return keep


def _wing_shape(length, width, sx):
    """An insect wing on a horizontal plane: rooted at x=0, broad, rounded at the tip (x grows outward)."""
    def keep(lx, ly, lz):
        u = np.abs(lx) / max(length, 1e-6)
        v = (lz - lz.min() + 0.5) / max(width, 1e-6)
        mid = 0.32 + 0.2 * u
        half = 0.5 * np.sin(np.clip(u * 1.08, 0, 1) * math.pi * 0.5 + 0.35) * (1 - 0.35 * u)
        return (np.abs(v - mid) < half) & (u <= 1.0)
    return keep


def _veins(n=2.2, depth=-0.16):
    """Wing veins: a few darker lines branching out from the root."""
    def tone(c):
        loc = c['loc']
        x = np.abs(loc[..., 0])
        z = loc[..., 2]
        ang = np.arctan2(z - z.min() * 0.0, x + 0.6)
        lines = np.abs(np.sin(ang * n * 3.0)) < 0.18
        return np.where(lines, depth, 0.0) + np.clip(x / 30.0, 0, 0.15)
    return tone


def _segments(step, depth=-0.18):
    """Body segment seams every `step` units along z (abdomens, tails)."""
    def tone(c):
        z = c['loc'][..., 2]
        f = (z - z.min()) % step
        return np.where(f < 0.5, depth, 0.0) + np.where((f >= 0.5) & (f < 1.0), 0.06, 0.0)
    return tone


def _crystal(part, at, size, rot=(0, 0, 0), name=None):
    """A glowing crystal shard growing out of the chitin: a column and a smaller twin beside it."""
    p = part.part(name or f'crystal_{len(part.children)}', pivot=at, rot=rot)
    w, h = size
    p.cube((-w / 2, -h, -w / 2), (w, h, w), mat='crystal', lights=lambda c: c['loc'][..., 1] < -h * 0.35, ao=0)
    p.cube((w / 2 - 0.2, -h * 0.6, -w / 2 + 0.2), (w * 0.6, h * 0.6, w * 0.6), mat='crystal', lights=lambda c: c['loc'][..., 1] < -h * 0.3,
           ao=0)
    return p


# ================================================================ the Glow Fly

def glow_fly() -> Model:
    """A big golden firefly-moth: a thick coat of golden fuzz, glossy black-amber compound eyes, feathery antennae
    tipped with light, amber wing cases spread over two pairs of veined see-through wings, six thin dark legs and a
    huge banded lantern of an abdomen - the light gland - that glows (GlowFlyModel)."""
    m = Model('glow_fly', (96, 64), {}, {'glow_fly': {}}, res=2)
    F = dict(mat='fuzz', back=-1.5, belly=1.4)
    body = m.part('body', pivot=(0, 15, 0))
    body.cube((-3.5, -3.5, -3), (7, 6, 6), **F)
    body.cube((-4, -3, -3.5), (8, 4, 2), mat='fuzz', back=99, light=0.06)
    # the head: big glossy compound eyes, stubby mouthparts
    head = body.part('head', pivot=(0, -0.5, -3.5))
    head.cube((-2.5, -2.5, -3.5), (5, 4.5, 4), **F)
    for sx in (1, -1):  # small glossy compound eyes (no highlight: vanilla-style pixel eyes)
        x0 = 2 if sx > 0 else -3.5
        head.cube((x0, -2.2, -3.2), (1.5, 2, 2), mat='eye', ao=0,
                  faces={'east' if sx > 0 else 'west': dict(decal=['nnnn', 'nCnn', 'nnnC', 'nnnn'], keys=EYE, at=(0, 0), mirror=sx < 0),
                         'north': dict(decal=['nnnn', 'nnCn', 'nnnn', 'nCnn'], keys=EYE, at=(0, 0))})
    head.cube((-1, 2, -3.6), (2, 1, 1.5), mat='leg')
    for side, sx in (('left', 1), ('right', -1)):
        ant = head.part(f'{side}_antenna', pivot=(1.0 * sx, -2.5, -3), rot=(-0.55, 0.35 * sx, 0.3 * sx))
        ant.cube((-0.25, -2.5, -0.25), (0.5, 2.5, 0.5), mat='leg')
        plume = ant.part(f'{side}_plume', pivot=(0, -2.5, 0), rot=(0.2, 0, 0))
        plume.cube((0, -5, -1.5), (0, 5, 3), mat='fuzz', no_occlude=True, lights=_above(-3.5),
                   shape=lambda lx, ly, lz: (np.abs(lz - (lz.min() + lz.max()) / 2) < 1.6 - (-ly) * 0.18) & ((np.floor(-ly * 2) % 2 == 0) | (np.abs(lz - (lz.min() + lz.max()) / 2) < 0.4)))
    # the lantern: a big banded abdomen glowing through its belly plates, the tip brightest of all (the light gland)
    ab = body.part('abdomen', pivot=(0, -0.5, 2.5), rot=(0.3, 0, 0))
    ab.cube((-4, -3.5, 0), (8, 7, 7), mat='lantern', tone=_segments(1.75, -0.32), lights=lambda c: (c['loc'][..., 1] > -1.5) | (c['face'] == 'down'))
    ab.cube((-3.5, -4.2, 0.5), (7, 1, 6), mat='fuzz', back=99, tone=_segments(1.75, -0.25))
    tip = ab.part('abdomen_tip', pivot=(0, 0, 7), rot=(0.15, 0, 0))
    tip.cube((-3, -3, 0), (6, 6, 3.5), mat='lantern', glow=True, tone=_segments(1.75, -0.22))
    tip.cube((-1.75, -1.75, 3.5), (3.5, 3.5, 1), mat='lantern', glow=True, light=0.15)
    # wing cases (hard, half open) over two pairs of veined, see-through wings
    for side, sx in (('left', 1), ('right', -1)):
        case = body.part(f'{side}_wing_case', pivot=(1.2 * sx, -3.6, -1.5), rot=(0.12, -0.35 * sx, -0.35 * sx))
        case.cube(((0 if sx > 0 else -3.5), -0.5, 0), (3.5, 1, 8.5), mat='case', back=99, faces={'up': dict(mat='case', marks=[(_case_stripe, 'stripe', 1)])})
        wing = body.part(f'{side}_wing', pivot=(1.4 * sx, -3.8, -0.5), rot=(0, 0, -0.25 * sx))
        wing.cube(((0 if sx > 0 else -12), 0, -1), (12, 0, 8), mat='wing', shape=_wing_shape(12, 8, sx), tone=_veins(), no_occlude=True)
        hind = body.part(f'{side}_hindwing', pivot=(1.4 * sx, -3.4, 1.0), rot=(0, 0.45 * sx, -0.1 * sx))
        hind.cube(((0 if sx > 0 else -10), 0, -1), (10, 0, 7), mat='wing', shape=_wing_shape(10, 7, sx), tone=_veins(1.8), no_occlude=True)
        for i in range(3):
            leg = body.part(f'{side}_leg_{i}', pivot=(2.4 * sx, 2.0, -1.8 + i * 1.6), rot=(0.45 - i * 0.45, 0, 0.95 * sx))
            leg.cube((-0.25, 0, -0.25), (0.5, 3.5, 0.5), mat='leg')
            foot = leg.part(f'{side}_foot_{i}', pivot=(0, 3.5, 0), rot=(-0.3 + i * 0.15, 0, -0.9 * sx))
            foot.cube((-0.2, 0, -0.2), (0.4, 3, 0.4), mat='leg', light=-0.1)
    mats = {
        'fuzz': M([('#6a4a0c', '#8a6412', '#ac821c', '#cca02a', '#e6bc3e', '#f6d65c', '#fff08e'),
                   ('#704c0e', '#926a16', '#b48a20', '#d4a830', '#ecc446', '#f8dc68', '#fff49c')], base=0.56, noise=0.11, cell=1.0,
                  mottle=(1.4, 0.6), back=('#4e360a', '#66480e', '#805c14', '#9a721c', '#b48a26', '#cca232', '#e2ba44'),
                  belly=('#8a6a1e', '#a8862a', '#c6a43a', '#e0c050', '#f2d86c', '#fcea92', '#fff8c4'), light=LANTERN_LIGHT, material='fur',
                  mnoise=0.8),
        'case': M([('#3a2408', '#52340c', '#6c4812', '#885e1a', '#a47624', '#c09030', '#dcae44')], base=0.55, noise=0.05, cell=1.4, mottle=None,
                  gloss='#fff2c8', gloss_rate=0.08, material='chitin', mnoise=0.5, stripe=LANTERN),
        'leg': M([('#140e06', '#1e160a', '#2a1e0e', '#382814', '#48341a', '#5a4222', '#6e522c')], base=0.5, noise=0.04, cell=1.2, mottle=None,
                 material='chitin'),
        'eye': M([('#05070a', '#0c0e12', '#16181c', '#22242a', '#30323a', '#44464e', '#5c5e66')], base=0.5, noise=0.02, rim=0.3, ao=0.0,
                 mottle=None, material='crystal'),
        'lantern': M([LANTERN, ('#806008', '#a8820e', '#d0aa18', '#f0ca28', '#ffe244', '#fff07c', '#fffbc6')], base=0.62, noise=0.06, cell=1.4,
                     mottle=(1.8, 0.66), light=LANTERN_LIGHT, gloss='#fffff0', gloss_rate=0.05, material='jelly', mnoise=0.5),
        'wing': M([('#4a3c14', '#62501e', '#7c682a', '#98823a', '#b49e50', '#d0bc70', '#ecdc9c')], base=0.62, noise=0.04, cell=1.5, rim=0.0,
                  ao=0.0, mottle=None, flat=True, material='membrane', opacity=140),
    }
    return AK.finish(m, {'glow_fly': mats})


def _case_stripe(c):
    """A pale stripe down each wing case, like a firefly's."""
    x = np.abs(c['loc'][..., 0])
    return (x > 1.2) & (x < 2.1)


# ================================================================ the Crocotodo

TEAL_FEATHER = ('#0b2420', '#12332c', '#19443a', '#22584a', '#2e6e5a', '#3e866c', '#58a084')
TEAL_BACK = ('#081a18', '#0d2622', '#12332c', '#184036', '#204e42', '#2a5e4e', '#386e5c')
CROC_BELLY = ('#5a5432', '#726c44', '#8e8858', '#aaa470', '#c4be8a', '#dad6a6', '#ecead0')
SNOUT = ('#1e2010', '#2a2d16', '#383c1e', '#484e28', '#5c6232', '#72783e', '#8c924e')
CREST = ('#5a1a0c', '#7c2812', '#a03a1a', '#c45224', '#e06e34', '#f08e4c', '#fab46e')
SHANK = ('#4a2410', '#663216', '#84421e', '#a05628', '#bc6c34', '#d48644', '#e8a45c')


def _scutes(c):
    """Rows of armoured scutes along the back (a crocodile's)."""
    loc = c['loc']
    if c['face'] != 'up':
        return None
    x = np.abs(loc[..., 0])
    z = loc[..., 2]
    return ((x > 0.8) & (x < 2.2) | (x > 3.0) & (x < 4.0)) & (np.floor(z) % 2 == 0)


def crocotodo() -> Model:
    """A plump flightless bird with a crocodile's long toothed snout, armoured scutes down its back, stubby wings,
    a flame crest, a fan of a tail and scaly orange legs (CrocotodoModel)."""
    m = Model('crocotodo', (96, 64), {}, {'crocotodo': {}}, res=2)
    F = dict(mat='feather', back=-2.5, belly=1.5)
    body = m.part('body', pivot=(0, 14.5, 1))
    body.cube((-4.5, -5, -5), (9, 8, 11), **F, faces={'up': dict(F, marks=[(_scutes, 'scute', -1)])})
    body.cube((-4, 3, -4), (8, 1.5, 8), mat='feather', belly_all=True)
    body.cube((-3.5, -6, -4), (7, 1, 9), mat='feather', back=99, faces={'up': dict(marks=[(_scutes, 'scute', -1)])})
    for i in range(4):
        body.cube((-0.5, -7, -3 + i * 2.2), (1, 1, 1.2), mat='scute', ao=0)
    for side, sx in (('left', 1), ('right', -1)):
        wing = body.part(f'{side}_wing', pivot=(4.5 * sx, -3.5, -2.5), rot=(-0.12, 0, -0.08 * sx))
        wing.cube(((0 if sx > 0 else -1), 0, 0), (1, 5, 7), mat='feather', back=1.0, faces={'east' if sx > 0 else 'west': dict(marks=[(_wing_bars, 'scute', 0)])})
        tipw = wing.part(f'{side}_wing_tip', pivot=(0.5 * sx, 0.5, 7))
        tipw.cube(((-0.5 if sx > 0 else -0.5), 0, 0), (1, 3, 3), mat='feather', back=0.0)
    tail = body.part('tail', pivot=(0, -3, 6), rot=(-0.7, 0, 0))
    tail.cube((-5, -7, 0), (10, 7, 0), mat='crest', no_occlude=True, tone=WF._rays(1, -0.1),
              shape=lambda lx, ly, lz: (lx / 5.0) ** 2 + ((ly + 7) / 7.2) ** 2 < 1.0 + 0.0 * lz)
    tail.cube((-2, -2, -1), (4, 3, 2), mat='feather', back=99)
    neck = body.part('neck', pivot=(0, -3, -4.5), rot=(0.25, 0, 0))
    neck.cube((-2, -6, -2), (4, 7, 4), mat='feather', back=99, belly=1.0, faces={'north': dict(mat='feather', belly_all=True)})
    head = neck.part('head', pivot=(0, -6, -0.5), rot=(-0.25, 0, 0))
    head.cube((-2.5, -4.5, -3), (5, 5, 5), mat='feather', back=-2.5)
    for sx in (1, -1):  # a small amber-and-black pixel eye, like a parrot's
        head.cube(((2.5 if sx > 0 else -3), -3.0, -1.8), (0.5, 1, 1), mat='eye', ao=0,
                  faces={'east' if sx > 0 else 'west': dict(decal=['IP', 'PP'] if sx > 0 else ['PI', 'PP'], keys=EYE, at=(0, 0))})
    crest = head.part('crest', pivot=(0, -4.5, 0), rot=(-0.35, 0, 0))
    crest.cube((0, -5, -2), (0, 5, 6), mat='crest', no_occlude=True, tone=WF._rays(1, -0.12),
               shape=lambda lx, ly, lz: ((lz - lz.min()) * 0.75 + 0.6 > (-ly) * 0.9 - 1.0) & ((-ly) < 4.6 - np.abs(lz - lz.min() - 3) * 0.5))
    snout = head.part('snout', pivot=(0, -1.5, -3))
    snout.cube((-1.75, -1.5, -7), (3.5, 2, 7), mat='snout', back=-0.6,
               faces={'up': dict(mat='snout', back=99, decal=['.......', '.n...n.'] + ['.......'] * 12, keys=EYE, at=(0, 0))})
    snout.cube((-1.25, -2, -6), (2.5, 0.5, 5), mat='snout', back=99)
    WF._teeth(snout, -1.5, 2, 0.5, -6.5, True)
    snout.cube((1.25, 0.5, -6.5), (0.5, 1, 0.5), mat='bone', ao=0)
    snout.cube((-1.75, 0.5, -6.5), (0.5, 1, 0.5), mat='bone', ao=0)
    jaw = head.part('jaw', pivot=(0, 0.5, -2.5))
    jaw.cube((-1.5, 0, -6.5), (3, 1, 6.5), mat='snout', belly_all=True, faces={'up': dict(mat='mouth')})
    WF._teeth(jaw, -1, 2, 0, -6, False)
    wattle = jaw.part('wattle', pivot=(0, 1, -1.5))
    wattle.cube((-1, 0, -1), (2, 2, 2), mat='crest', ao=0)
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(2.3 * sx, 17, 1.5))
        leg.cube((-1.25, -1, -1.5), (2.5, 3.5, 3), mat='feather', back=99, belly=0.5)
        shin = leg.part(f'{side}_shin', pivot=(0, 2.5, 0))
        shin.cube((-0.5, 0, -0.5), (1, 4.2, 1), mat='shank', tone=_segments(0.8, -0.12))
        foot = shin.part(f'{side}_foot', pivot=(0, 4.2, 0))
        foot.cube((-1.5, 0, -2.6), (3, 0.3, 3.2), mat='shank',
                  faces={'up': dict(decal=['k..k..k', 'k..k..k', '.......', '.......', '.......', '.......'], keys={'k': '#e8dcc0'}, at=(-0, 0))})
        foot.cube((-0.25, 0, 0.6), (0.5, 0.3, 1.2), mat='shank')
    mats = {
        'feather': M([TEAL_FEATHER, ('#0d2a22', '#143a30', '#1c4c3e', '#26604e', '#337860', '#469074', '#62aa8c')], base=0.55, noise=0.1,
                     cell=1.1, mottle=(1.4, 0.62), back=TEAL_BACK, belly=CROC_BELLY, material='fur', mnoise=0.7,
                     spots=(None, 1.0, 0.8, 0), scute=SNOUT),
        'scute': M([SNOUT], base=0.55, noise=0.05, cell=1.2, rim=1.0, mottle=None, material='scales'),
        'snout': M([SNOUT, ('#1a1e0e', '#262c14', '#333a1c', '#424a24', '#545e2e', '#687238', '#7e8a46')], base=0.55, noise=0.08, cell=1.0,
                   mottle=(1.2, 0.62), back=('#121408', '#1b1e0e', '#252a14', '#30361a', '#3e4622', '#4e562a', '#606a34'), belly=CROC_BELLY,
                   scales=(1.0, 1.0), gloss='#e6ecc8', gloss_rate=0.05, material='scales'),
        'eye': M([('#3a1c06', '#5a2c08', '#7a3c08', '#a0560c', '#c87414', '#e8961e', '#ffc04c')], base=0.5, noise=0.0, rim=0.0, ao=0.0, mottle=None),
        'crest': M([CREST], base=0.58, noise=0.06, cell=1.2, rim=0.0, ao=0.0, mottle=None, flat=True, material='fur'),
        'shank': M([SHANK], base=0.55, noise=0.06, cell=1.0, rim=0.8, mottle=None, scales=(0.8, 0.8), material='scales'),
        'bone': M([BONE], base=0.66, noise=0.04, cell=1.5, rim=0.5, ao=0.2, material='bone'),
        'mouth': M([MOUTH], base=0.35, noise=0.03, rim=0.2, ao=0.3),
    }
    return AK.finish(m, {'crocotodo': mats})


def _wing_bars(c):
    """Two pale bars across the folded wing."""
    z = c['loc'][..., 2]
    return (np.floor(z) % 3 == 1) & (c['loc'][..., 1] > 1.5)


# ================================================================ the Mantis

MANTIS_GREEN = ('#0c1f0e', '#132e15', '#1b3f1c', '#255424', '#316b2e', '#42853a', '#5aa04a')
MANTIS_DARK = ('#081408', '#0d1e0e', '#132914', '#1a361a', '#224422', '#2c542a', '#386636')
MANTIS_PALE = ('#34441a', '#465a22', '#5a722c', '#728c3a', '#8ca64c', '#a8c066', '#c6da88')
CRYSTAL = ('#0a2440', '#10365a', '#184c78', '#226698', '#2e86b8', '#52aed8', '#94dcf4')
CRYSTAL_LIGHT = ('#1fb4d8', '#4fe0f2', '#9cf6ff', '#e4feff')


def _spines(part, x, y0, y1, z, every=1.6, length=1.2):
    """A row of pale spines along the inner edge of a raptorial arm."""
    y = y0
    while y < y1:
        part.cube((x - 0.25, y, z - length), (0.5, 0.5, length), mat='spine', ao=0)
        y += every


def mantis() -> Model:
    """A towering praying mantis (MantisModel): an upright neck and triangular head of glowing eyes and mandibles,
    two huge raptorial arms with serrated scythe blades, four long walking legs, a segmented abdomen under folded
    leaf-veined wings - and azure crystals growing out of its back, neck and arms, glowing."""
    m = Model('mantis', (128, 64), {}, {'mantis': {}}, res=2)
    C = dict(mat='chitin', back=-1.5, belly=1.0)
    body = m.part('body', pivot=(0, 12, 2))
    body.cube((-2.5, -2.5, -3.5), (5, 4.5, 7), **C)
    body.cube((-2, 2, -3), (4, 1, 6), mat='chitin', belly_all=True)
    _crystal(body, (1.2, -2.4, -1.0), (1.6, 3.5), rot=(-0.3, 0, 0.35), name='crystal_back_left')
    _crystal(body, (-1.0, -2.4, 1.2), (1.2, 2.6), rot=(0.25, 0, -0.4), name='crystal_back_right')
    # the abdomen: long, segmented, curving up a little at the end
    ab = body.part('abdomen', pivot=(0, -0.5, 3.5), rot=(-0.12, 0, 0))
    ab.cube((-3.5, -2.5, 0), (7, 5, 9), **C, tone=_segments(1.8))
    tip = ab.part('abdomen_tip', pivot=(0, 0, 9), rot=(-0.2, 0, 0))
    tip.cube((-2.5, -2, 0), (5, 4, 5), **C, tone=_segments(1.6))
    tip.cube((-1.5, -1.5, 5), (3, 3, 1.5), **C)
    _crystal(ab, (0.8, -2.4, 3.0), (1.4, 3.0), rot=(-0.5, 0, 0.3), name='crystal_abdomen')
    _crystal(ab, (-1.2, -2.4, 6.0), (1.1, 2.4), rot=(-0.6, 0, -0.35), name='crystal_abdomen_rear')
    for side, sx in (('left', 1), ('right', -1)):
        w = body.part(f'{side}_wing', pivot=(0.6 * sx, -2.6, -1.5), rot=(0.06, 0.06 * sx, -0.05 * sx))
        w.cube(((0 if sx > 0 else -4.5), 0, 0), (4.5, 0, 16), mat='wing', no_occlude=True, tone=_leaf_veins,
               shape=lambda lx, ly, lz: _leafmask(lx, lz, 4.5, 16))
    # the neck (prothorax), rising up and a little forward, crystals along it
    neck = body.part('neck', pivot=(0, -1.5, -3), rot=(0.42, 0, 0))
    neck.cube((-1.5, -12, -1.5), (3, 12, 3), **C)
    neck.cube((-2, -12, -2), (4, 2.5, 4), mat='chitin', back=99)
    _crystal(neck, (0, -7.5, 1.2), (1.4, 3.0), rot=(0.9, 0, 0), name='crystal_neck')
    _crystal(neck, (0.6, -4, 1.2), (1.0, 2.0), rot=(1.1, 0, 0.2), name='crystal_neck_low')
    head = neck.part('head', pivot=(0, -12, -0.5), rot=(-0.55, 0, 0))
    head.cube((-3, -3, -2), (6, 3, 3), mat='chitin', back=-2.0,
              faces={'north': dict(mat='chitin', decal=['............', '....d..d....', '.....dd.....', '............', '............', '............'],
                                   keys=EYE, at=(0, 0))})
    head.cube((-2, 0, -2), (4, 2, 2.5), mat='chitin', belly=1.0)
    head.cube((-1.25, 2, -1.8), (2.5, 1.5, 2), mat='chitin', belly=0.5)
    _crystal(head, (1.2, -3, 0.2), (0.8, 2.2), rot=(-0.4, 0, 0.45), name='crystal_crown_left')
    _crystal(head, (-1.2, -3, 0.2), (0.8, 2.2), rot=(-0.4, 0, -0.45), name='crystal_crown_right')
    for sx in (1, -1):
        e = head.part(('left' if sx > 0 else 'right') + '_eye', pivot=(3.2 * sx, -2.5, -0.5))
        e.cube((-1, -1, -1.2), (2, 2, 2), mat='eye', ao=0, lights=lambda c: _mask_hash(c, 0.55, 3) | (c['loc'][..., 1] < -0.4))
    for sx, sname in ((1, 'left'), (-1, 'right')):
        md = head.part(f'{sname}_mandible', pivot=(0.7 * sx, 3.2, -1.2), rot=(0, 0, 0.15 * sx))
        md.cube(((-0.6 if sx > 0 else -0.4), 0, -0.5), (1, 1.5, 1), mat='spine', ao=0)
        ant = head.part(f'{sname}_antenna', pivot=(0.9 * sx, -3, -1.5), rot=(-0.9, 0.25 * sx, 0.25 * sx))
        ant.cube((-0.2, -9, -0.2), (0.4, 9, 0.4), mat='chitin', light=-0.05)
        # the raptorial arm: coxa and femur swinging down from the neck, a serrated tibia blade folded back up
        arm = neck.part(f'{sname}_scythe', pivot=(1.8 * sx, -9.5, -0.8), rot=(-0.55, 0, -0.08 * sx))
        arm.cube((-0.9, -0.5, -0.9), (1.8, 4, 1.8), **C)
        fem = arm.part(f'{sname}_femur', pivot=(0, 3.5, 0), rot=(-0.4, 0, 0))
        fem.cube((-0.8, 0, -1.0), (1.6, 8, 2.0), **C, faces={'north': dict(C, marks=[(lambda c: c['loc'][..., 0] * 0 == 0, 'pale', 0)])})
        _spines(fem, 0, 1.0, 7.5, -1.0)
        _crystal(fem, (0, 2.5, 1.0), (0.9, 2.0), rot=(1.4, 0, 0), name=f'{sname}_arm_crystal')
        blade = fem.part(f'{sname}_blade', pivot=(0, 8, -0.3), rot=(2.75, 0, 0))
        blade.cube((-0.55, 0, -0.6), (1.1, 8, 1.2), **C)
        blade.cube((0, 0.5, -3), (0, 7.5, 2.5), mat='blade', no_occlude=True, tone=_serration,
                   shape=lambda lx, ly, lz: (lz - lz.min()) > np.clip(2.5 - (ly - ly.min()) * 0.33, 0, 2.5) * (1 - (ly - ly.min()) / 8.0) + (np.floor(ly * 2) % 2) * 0.4)
        hook = blade.part(f'{sname}_hook', pivot=(0, 8, 0), rot=(0.9, 0, 0))
        hook.cube((-0.4, 0, -0.4), (0.8, 2.5, 0.8), mat='spine', ao=0)
        # the walking legs
        for i, (zz, name) in enumerate(((-2.0, 'mid'), (2.2, 'hind'))):
            leg = body.part(f'{sname}_{name}_leg', pivot=(2.3 * sx, 1.2, zz), rot=(-0.5 if i == 0 else 0.6, 0.0, -1.1 * sx))
            leg.cube((-0.45, 0, -0.45), (0.9, 7.5, 0.9), mat='chitin', back=99, tone=_segments(1.2, -0.08))
            shin = leg.part(f'{sname}_{name}_shin', pivot=(0, 7.5, 0), rot=(0, 0, 1.55 * sx))
            shin.cube((-0.35, 0, -0.35), (0.7, 9.5, 0.7), mat='chitin', light=-0.05)
    mats = {
        'chitin': M([MANTIS_GREEN, ('#0e2412', '#16341a', '#1f4724', '#2a5e2e', '#38783a', '#4c9248', '#66ac5c')], base=0.54, noise=0.07,
                    cell=1.6, mottle=(2.6, 0.6), back=MANTIS_DARK, belly=MANTIS_PALE, gloss='#e2f6c8', gloss_rate=0.05, material='chitin',
                    mnoise=0.6, pale=MANTIS_PALE),
        'wing': M([('#163018', '#1f4220', '#2a5628', '#376c32', '#46843e', '#5a9c4e', '#78b866')], base=0.56, noise=0.05, cell=1.4, rim=0.0,
                  ao=0.0, mottle=None, flat=True, material='membrane', opacity=230),
        'crystal': M([CRYSTAL], base=0.58, noise=0.04, cell=1.0, rim=1.2, ao=0.0, mottle=None, light=CRYSTAL_LIGHT, gloss='#f0ffff',
                     gloss_rate=0.12, material='crystal', mnoise=0.4),
        'eye': M([('#06283a', '#0a3a52', '#10506c', '#186a88', '#2a88a6', '#4aaac4', '#86d2e4')], base=0.55, noise=0.03, rim=0.4, ao=0.0,
                 mottle=None, light=CRYSTAL_LIGHT, material='crystal'),
        'spine': M([('#4a4a2c', '#62623a', '#7c7c4a', '#98985e', '#b4b476', '#cece92', '#e4e4b4')], base=0.62, noise=0.03, rim=0.5, ao=0.0,
                   mottle=None, material='bone'),
        'blade': M([('#2c3a1c', '#3c5026', '#506832', '#688442', '#84a056', '#a6bc72', '#d0dca0')], base=0.6, noise=0.04, rim=0.0, ao=0.0,
                   mottle=None, flat=True, material='chitin'),
    }
    return AK.finish(m, {'mantis': mats})


def _leafmask(lx, lz, w, length):
    t = np.clip((lz - lz.min() + 0.5) / length, 0, 1)
    half = w * (0.55 + 0.45 * np.sin(t * math.pi)) * (1 - 0.6 * np.clip(t - 0.7, 0, 1) / 0.3)
    return np.abs(lx) < half


def _leaf_veins(c):
    loc = c['loc']
    x = np.abs(loc[..., 0])
    z = loc[..., 2] - loc[..., 2].min()
    mid = x < 0.5
    side = np.abs((z - x * 1.6) % 3.0) < 0.45
    return np.where(mid, -0.16, 0.0) + np.where(side & ~mid, -0.09, 0.0) + np.clip(x / 10.0, 0, 0.12)


def _serration(c):
    ly = c['loc'][..., 1]
    return np.where(np.floor(ly * 2) % 2 == 0, -0.08, 0.1)


# ================================================================ the Colossus Ponder

PONDER_SKIN = ('#0d1c10', '#142a17', '#1c3b1f', '#264e28', '#316333', '#3f7a40', '#549452')
PONDER_BACK = ('#09140b', '#0e1e10', '#142a16', '#1b371d', '#234624', '#2d562d', '#3a6838')
PONDER_BELLY = ('#4a4a24', '#625f30', '#7c7840', '#969254', '#b0ac6a', '#c8c486', '#e0dca8')
PONDER_SAC = ('#3c4a22', '#4e602c', '#647838', '#7c9248', '#98ac5c', '#b4c676', '#d2e09a')
GLOWCAP = ('#123c4a', '#1a5466', '#226e84', '#2c8aa2', '#3ea8be', '#64c8d8', '#a0e6ee')


def _moss_lights(c):
    """Glowing tufts in the moss carpet."""
    return _mask_hash(c, 0.08, 41) | (AK.fbm(c['wp'] * 1.0, 2.2, 63, 1) > 0.72)


def _ponder_spots(c):
    return AK.fbm(c['wp'] + 2.0, 3.0, 29, 2) > 0.62


def colossus_ponder() -> Model:
    """A giant frog wearing the jungle floor (ColossusPonderModel): a warty green hide under a carpet of glowing
    Lumen Moss with ferns, glowcaps and hanging vines, amber eyes on domes with lids, a wide mouth, a pale throat
    sac, thick arms with webbed hands and huge folded hind legs."""
    m = Model('colossus_ponder', (192, 160), {}, {'colossus_ponder': {}}, res=2)
    S = dict(mat='skin', back=-2, marks=[(_ponder_spots, 'spot', -1)])
    W = dict(mat='skin', back=99, marks=[(WF._warts, 'warts', -2)])
    body = m.part('body', pivot=(0, 13, 4), rot=(-0.22, 0, 0))
    body.cube((-11, -6, -10), (22, 10, 20), **S, belly=2.0)
    body.cube((-10, 4, -9), (20, 2, 17), mat='belly')
    body.cube((-9.5, -7.5, -8), (19, 1.5, 16), mat='moss', lights=_moss_lights)
    body.cube((-7, -8.5, -6), (14, 1, 11), mat='moss', lights=_moss_lights)
    body.cube((-8, -6, 10), (16, 7, 1.5), **S)
    # the garden on its back: ferns, glowcaps and hanging vines
    for i, (x, z, r, s) in enumerate(((-5, -3, 0.2, 1.0), (4, 1, -0.3, 0.8), (-2, 4, 0.5, 0.9), (6, -5, -0.6, 0.7))):
        f = body.part(f'fern_{i}', pivot=(x, -8.5, z), rot=(0, r, 0))
        h = 7 * s
        f.cube((-3, -h, 0), (6, h, 0), mat='fern', no_occlude=True, shape=_frond(h, 3), tone=_frond_tone)
        f.cube((0, -h, -3), (0, h, 6), mat='fern', no_occlude=True, shape=_frond(h, 3, axis='z'), tone=_frond_tone)
    for i, (x, z, h) in enumerate(((2, -6, 2.5), (-7, 2, 2.0), (-1, 6, 3.0))):
        cap = body.part(f'glowcap_{i}', pivot=(x, -8.5, z))
        cap.cube((-0.4, -h, -0.4), (0.8, h, 0.8), mat='stalk', ao=0)
        cap.cube((-1.2, -h - 1, -1.2), (2.4, 1, 2.4), mat='glowcap', glow=True, ao=0)
    for i, (sx, z) in enumerate(((1, -4), (-1, 2), (1, 5))):
        v = body.part(f'vine_{i}', pivot=(11 * sx, -6, z), rot=(0, 0, 0.1 * sx))
        v.cube((0, 0, -1), (0, 9, 2), mat='fern', no_occlude=True, shape=lambda lx, ly, lz: np.abs(lz - (lz.min() + lz.max()) / 2) < 0.6 + 0.5 * (np.floor(ly) % 3 == 0))
    # the head and its great mouth
    head = body.part('head', pivot=(0, -1, -10), rot=(0.22, 0, 0))
    head.cube((-11, -5, -11), (22, 5, 11), **S,
              faces={'north': dict(S, decal=up(['......................', '......n........n......']), keys=EYE, at=(0, 0))})
    head.cube((-9, -6, -9), (18, 1, 8), mat='moss', lights=_moss_lights)
    for side, sx in (('left', 1), ('right', -1)):
        eye = head.part(f'{side}_eye', pivot=(6.5 * sx, -5, -5))
        eye.cube((-3.5, -4.5, -3.5), (7, 5, 7), **W)
        # a small amber eye with a black slit of a pupil on the front of the dome (vanilla-frog style)
        eye.cube((-1.5, -3.2, -3.9), (3, 1, 0.5), mat='iris', faces={'north': dict(decal=['CAnnAC', 'CCnnCC'], keys=EYE, at=(0, 0))})
        lid = eye.part(f'{side}_eyelid', pivot=(0, -3.3, -4.0))
        lid.cube((-1.7, 0, -0.1), (3.4, 1.3, 0.6), mat='skin', back=99)
    jaw = head.part('jaw', pivot=(0, 0, -1))
    jaw.cube((-10.5, 0, -10.5), (21, 3, 11), mat='belly', faces={'up': dict(mat='mouth')})
    jaw.cube((-10.6, -0.4, -10.6), (21.2, 0.8, 0.8), mat='skin', light=-0.1)
    sac = jaw.part('throat', pivot=(0, 3, -5))
    sac.cube((-6, -1, -4), (12, 3, 8), mat='sac')
    # thick arms with webbed hands
    for side, sx in (('left', 1), ('right', -1)):
        arm = body.part(f'{side}_arm', pivot=(9 * sx, 2, -7), rot=(0.25, 0, -0.15 * sx))
        arm.cube((-2, -1, -2), (4, 8, 4), **S, belly=4.0)
        hand = arm.part(f'{side}_hand', pivot=(0, 7, 0), rot=(-0.25, 0, 0.15 * sx))
        hand.cube((-3.5, 0, -5), (7, 1, 7), mat='web', faces={'north': dict(decal=up(['D.D.D.D']), keys=EYE, at=(0, 0))})
        leg = body.part(f'{side}_leg', pivot=(10 * sx, 1, 6))
        leg.cube(((-2 if sx > 0 else -5), -4, -8), (7, 9, 14), **S, belly=3.0)
        shin = leg.part(f'{side}_shin', pivot=(1.5 * sx, 4, 4), rot=(0.22, 0, 0))
        shin.cube((-2.5, -2, -12), (5, 4, 12), **S, belly=1.0)
        foot = shin.part(f'{side}_foot', pivot=(0, 1.5, -11), rot=(0, -0.35 * sx, 0))
        foot.cube((-5, 0, -9), (10, 1.2, 9), mat='web', faces={'north': dict(decal=up(['D.D.D.D.D.']), keys=EYE, at=(0, 0))})
    mats = {
        'skin': M([PONDER_SKIN, PONDER_BACK], base=0.52, noise=0.09, cell=2.4, mottle=(3.2, 0.62), back=PONDER_BACK, belly=PONDER_BELLY,
                  gloss='#d8f2c8', gloss_rate=0.035, pores=0.05, spot=('#18301a', '#1f3c20', '#284c28', '#335e30', '#3e723a', '#4c8846', '#5e9e56')),
        'belly': M([PONDER_BELLY], base=0.58, noise=0.07, cell=2.0, mottle=None, mottle2=None, ao=0.6),
        'moss': M([LUMEN, ('#0e3830', '#145046', '#1c6a5c', '#268472', '#32a088', '#4cbc9e', '#80dcbe')], base=0.55, noise=0.12, cell=1.0,
                  mottle=(1.6, 0.55), light=MOSS_GLOW, material='plant', mnoise=0.9),
        'fern': M([FERN], base=0.56, noise=0.06, cell=1.0, rim=0.0, ao=0.0, mottle=None, flat=True, material='plant'),
        'stalk': M([('#5a6a5a', '#728272', '#8c9c8a', '#a6b6a2', '#c0d0ba', '#d8e6d0', '#ecf6e6')], base=0.6, noise=0.03, rim=0.3, ao=0.0, mottle=None),
        'glowcap': M([GLOWCAP], base=0.62, noise=0.04, rim=0.6, ao=0.0, mottle=None, material='jelly'),
        'iris': M([('#5a3a0e', '#7c5212', '#a06c18', '#c48a22', '#e0a836', '#f0c860', '#fbe6a0')], base=0.6, noise=0.02, rim=0.0, ao=0.0, mottle=None),
        'web': M([PONDER_SKIN], base=0.46, noise=0.06, cell=1.5, rim=0.6, ao=0.3, pores=0.04, mottle=None),
        'sac': M([PONDER_SAC], base=0.6, noise=0.05, cell=1.6, rim=1.0, ao=0.3, mottle=None, gloss='#f2ffe0', gloss_rate=0.08, material='jelly'),
        'mouth': M([MOUTH], base=0.35, noise=0.03, rim=0.2, ao=0.3),
    }
    mats['skin'].warts = mats['skin'].ramps[1]
    return AK.finish(m, {'colossus_ponder': mats})


def _frond(h, w, axis='x'):
    """A fern frond: a midrib and leaflets stepping up it, narrowing to a tip."""
    def keep(lx, ly, lz):
        a = lx if axis == 'x' else lz - (lz.min() + lz.max()) / 2
        t = np.clip(-ly / max(h, 1e-6), 0, 1)
        half = w * (1 - t) ** 0.8
        teeth = (np.floor(-ly * 1.5) % 2 == 0)
        return (np.abs(a) < np.where(teeth, half, half * 0.6)) | (np.abs(a) < 0.35)
    return keep


def _frond_tone(c):
    loc = c['loc']
    a = np.abs(loc[..., 0]) + np.abs(loc[..., 2] - (loc[..., 2].min() + loc[..., 2].max()) / 2)
    return np.where(a < 0.4, -0.14, 0.0) + np.clip(-loc[..., 1] / 30.0, 0, 0.12)


# ================================================================ the Ponder Tadpole

TAD_SKIN = ('#13140c', '#1d1e12', '#282a18', '#353820', '#444a2a', '#565e34', '#6c7642')
TAD_BELLY = ('#4a4a34', '#5e5e44', '#747456', '#8c8c6a', '#a4a482', '#bcbc9c', '#d4d4b8')


def ponder_tadpole() -> Model:
    """A big-headed tadpole with a mouth full of needle teeth, two budding legs and a see-through fin tail
    (PonderTadpoleModel)."""
    m = Model('ponder_tadpole', (64, 32), {}, {'ponder_tadpole': {}}, res=2)
    S = dict(mat='skin', back=-2, belly=0.5)
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-3, -3.5, -3.5), (6, 5, 7), **S)
    body.cube((-1.5, -4.0, -1.5), (3, 0.6, 3), mat='moss', lights=_moss_lights)
    for sx in (1, -1):
        body.cube(((1.6 if sx > 0 else -2.6), -4.5, -3), (1, 1.2, 1.2), mat='eye', ao=0,
                  faces={'north': dict(decal=['nA', 'An'], keys=EYE, at=(0, 0)), 'up': dict(decal=['An', 'nA'], keys=EYE, at=(0, 0))})
    for i in range(5):  # needle teeth
        body.cube((-2.25 + i * 1.0, 1.5, -3.7), (0.5, 1.2, 0.5), mat='bone', ao=0)
    jaw = body.part('jaw', pivot=(0, 1.5, -1), rot=(0.22, 0, 0))
    jaw.cube((-2.75, 0, -2.75), (5.5, 1.2, 4), mat='skin', belly_all=True, faces={'up': dict(mat='mouth')})
    for i in range(5):
        jaw.cube((-2.25 + i * 1.0 + 0.5, -1.0, -2.6), (0.5, 1.0, 0.5), mat='bone', ao=0)
    tail = body.part('tail', pivot=(0, -1, 3.5))
    tail.cube((-1, -1, 0), (2, 2.5, 4), **S)
    tail.cube((0, -3, 0), (0, 6.5, 5), mat='fin', no_occlude=True, shape=lambda lx, ly, lz: np.abs(ly + 0.25) < 3.2 - (lz - lz.min()) * 0.15)
    tip = tail.part('tail_tip', pivot=(0, 0, 4))
    tip.cube((-0.5, -0.5, 0), (1, 1.5, 3), **S)
    tip.cube((0, -2.5, 0), (0, 5, 5), mat='fin', no_occlude=True, shape=lambda lx, ly, lz: np.abs(ly + 0.25) < 2.6 - (lz - lz.min()) * 0.5)
    for side, sx in (('left', 1), ('right', -1)):
        leg = body.part(f'{side}_leg', pivot=(2.5 * sx, 0.8, 2.0), rot=(0, 0, 0.4 * sx))
        leg.cube(((0 if sx > 0 else -1.5), -0.5, -0.5), (1.5, 1, 1), mat='skin', back=99)
        foot = leg.part(f'{side}_foot', pivot=(1.5 * sx, 0, 0), rot=(0, 0, -0.6 * sx))
        foot.cube((-0.75, 0, -1.25), (1.5, 1.5, 1.5), mat='skin', belly_all=True)
    mats = {
        'skin': M([TAD_SKIN, ('#161a0e', '#202616', '#2c321e', '#3a4226', '#4a5430', '#5e6a3c', '#76824a')], base=0.52, noise=0.08, cell=1.2,
                  mottle=(1.6, 0.6), back=('#0c0d07', '#13140b', '#1b1d10', '#242716', '#2f331c', '#3c4224', '#4c542e'), belly=TAD_BELLY,
                  gloss='#e8f0d0', gloss_rate=0.06, pores=0.05),
        'moss': M([LUMEN], base=0.55, noise=0.12, cell=1.0, mottle=None, light=MOSS_GLOW, material='plant'),
        'eye': M([('#3a1c06', '#5a2c08', '#7a3c08', '#a0560c', '#c87414', '#e8961e', '#ffc04c')], base=0.5, noise=0.0, rim=0.0, ao=0.0, mottle=None),
        'fin': M([('#2a2e1c', '#3a4026', '#4e5634', '#666e46', '#828a5c', '#a2aa78', '#c4ca9c')], base=0.6, noise=0.04, rim=0.0, ao=0.0, mottle=None,
                 flat=True, material='membrane', opacity=170),
        'bone': M([BONE], base=0.7, noise=0.03, rim=0.4, ao=0.0, material='bone'),
        'mouth': M([MOUTH], base=0.35, noise=0.03, rim=0.2, ao=0.3),
    }
    return AK.finish(m, {'ponder_tadpole': mats})


# ================================================================ the Cruncher

CRUNCH_SKIN = ('#1a1b1c', '#26282a', '#333638', '#424548', '#53575a', '#666a6d', '#7c8083')
MAGNESITE = ('#6e6458', '#8a7e70', '#a89889', '#c2b5a6', '#d3c7b8', '#e2d9cc', '#f8f4ed')
DUST = ('#8a9096', '#a4aab0', '#bec4ca', '#d6dce0', '#eaeef0', '#f6f8fa', '#ffffff')
DUST_LIGHT = ('#c8d4dc', '#e4ecf2', '#f6fbff', '#ffffff')


def _dust(c):
    return _mask_hash(c, 0.05, 17)


def _plates(part, y, z0, z1, w, rows=2):
    """Overlapping Magnesite plates along a back (each a little crust cube)."""
    z = z0
    i = 0
    while z < z1:
        for r in range(rows):
            x = (-w / 2 + r * (w / rows)) + (0.4 if i % 2 else 0)
            part.cube((x, y - 0.8 - (i % 2) * 0.3, z), (w / rows - 0.3, 1.2 + (i % 2) * 0.3, 1.6), mat='plate', ao=0, lights=_dust)
        z += 1.4
        i += 1


def cruncher() -> Model:
    """A small cave raptor (CruncherModel): a dusty hide plated with creamy Magnesite crusts and glittering with
    Magnesium dust, a heavy head with a crushing jaw of fangs and molars, little clawed arms, strong legs and a
    stiff tail."""
    m = Model('cruncher', (96, 64), {}, {'cruncher': {}}, res=2)
    S = dict(mat='skin', back=-1.5, belly=1.8, lights=_dust)
    body = m.part('body', pivot=(0, 12.5, 1))
    body.cube((-3, -3, -5), (6, 6, 10), **S)
    _plates(body, -3, -4.5, 4.5, 5.0)
    neck = body.part('neck', pivot=(0, -1, -5), rot=(-0.45, 0, 0))
    neck.cube((-1.75, -2.25, -4), (3.5, 4, 4.5), **S)
    _plates(neck, -2.25, -3.5, 0, 3.0, rows=1)
    head = neck.part('head', pivot=(0, -0.5, -3.5), rot=(0.45, 0, 0))
    head.cube((-2.5, -3, -5.5), (5, 3.5, 6), **S, faces={'up': dict(S, decal=up(['.....', '.n.n.'] + ['.....'] * 4), keys=EYE, at=(0, 0))})
    _plates(head, -3, -4.5, 0.5, 4.0)
    for sx in (1, -1):
        head.cube(((2.5 if sx > 0 else -3), -2.2, -3.2), (0.5, 1, 1), mat='eye', ao=0, lights=lambda c: c['loc'][..., 1] > -100,
                  faces={'east' if sx > 0 else 'west': dict(decal=['nA', 'AB'] if sx > 0 else ['An', 'BA'], keys=EYE, glow_keys='AB', at=(0, 0))})
        head.cube(((1.2 if sx > 0 else -2.2), -4.4, -1.5), (1, 1.6, 2.5), mat='plate', ao=0, lights=_dust)  # brow horns
    WF._teeth(head, -2, 3, 0.5, -5.4, True)
    head.cube((-2.4, 0.5, -4.5), (0.6, 1, 4), mat='bone', ao=0)
    head.cube((1.8, 0.5, -4.5), (0.6, 1, 4), mat='bone', ao=0)
    jaw = head.part('jaw', pivot=(0, 0.5, -0.5))
    jaw.cube((-2.25, 0, -5), (4.5, 1.5, 5), mat='skin', belly_all=True, faces={'up': dict(mat='mouth')})
    WF._teeth(jaw, -2, 3, 0, -4.9, False)
    for side, sx in (('left', 1), ('right', -1)):
        arm = body.part(f'{side}_arm', pivot=(2.8 * sx, 1.5, -3.5), rot=(0.6, 0, 0))
        arm.cube((-0.5, 0, -0.5), (1, 2.5, 1), mat='skin')
        claw = arm.part(f'{side}_claw', pivot=(0, 2.5, 0), rot=(-0.9, 0, 0))
        claw.cube((-0.6, 0, -0.5), (1.2, 1.5, 1), mat='skin', faces={'north': dict(decal=['k.k', 'k.k', 'k.k'], keys={'k': '#e8dcc0'}, at=(0, 0))})
        leg = m.part(f'{side}_leg', pivot=(2.6 * sx, 13.5, 2))
        leg.cube((-1.25, -1, -1.75), (2.5, 5, 3.5), **S)
        shin = leg.part(f'{side}_shin', pivot=(0, 4, 1.2), rot=(0.55, 0, 0))
        shin.cube((-0.75, 0, -0.75), (1.5, 4, 1.5), mat='skin', back=99)
        foot = shin.part(f'{side}_foot', pivot=(0, 4, 0), rot=(-0.55, 0, 0))
        foot.cube((-1.25, 0, -3), (2.5, 1, 3.5), mat='skin', back=99,
                  faces={'north': dict(decal=['k.k.k', 'k.k.k'], keys={'k': '#efe6cc'}, at=(0, 0))})
    tail = body.part('tail', pivot=(0, -1.5, 5), rot=(-0.12, 0, 0))
    tail.cube((-2, -1.5, 0), (4, 3.5, 6), **S)
    _plates(tail, -1.5, 0.5, 5.5, 3.0, rows=1)
    tip = tail.part('tail_tip', pivot=(0, 0.5, 6), rot=(0.1, 0, 0))
    tip.cube((-1.25, -1.25, 0), (2.5, 2.5, 6), **S)
    for i in range(3):
        tip.cube((-0.4, -2.5, 0.8 + i * 1.8), (0.8, 1.4, 1.0), mat='plate', ao=0, lights=_dust)
    mats = {
        'skin': M([CRUNCH_SKIN, ('#22201c', '#2e2b26', '#3c3832', '#4c4740', '#5e584e', '#726a5e', '#887e70')], base=0.52, noise=0.09, cell=1.4,
                  mottle=(1.8, 0.6), back=('#121314', '#1a1b1d', '#232527', '#2d3032', '#383b3e', '#45494c', '#55595c'), belly=MAGNESITE,
                  light=DUST_LIGHT, spots=(('#5a5e62', '#6c7074', '#80848a', '#969aa0', '#acb0b6', '#c2c6cc', '#d8dce0'), 0.7, 0.8, 0), pores=0.04),
        'plate': M([MAGNESITE, ('#64584c', '#807264', '#9c8e7e', '#b8aa98', '#ccc0ae', '#ddd2c2', '#f0e8dc')], base=0.58, noise=0.1, cell=0.9,
                   mottle=(1.2, 0.6), rim=1.2, light=DUST_LIGHT, material='stone', mnoise=0.8),
        'eye': M([('#3a1004', '#5c1a06', '#86280a', '#b03c10', '#d65a18', '#f07c28', '#ffa448')], base=0.6, noise=0.0, rim=0.0, ao=0.0, mottle=None,
                 light=('#ff8a2a', '#ffb04a', '#ffd27a', '#fff0c0')),
        'bone': M([BONE], base=0.7, noise=0.03, rim=0.4, ao=0.0, material='bone'),
        'mouth': M([MOUTH], base=0.35, noise=0.03, rim=0.2, ao=0.3),
    }
    return AK.finish(m, {'cruncher': mats})


MODELS = {'glow_fly': glow_fly, 'crocotodo': crocotodo, 'mantis': mantis, 'colossus_ponder': colossus_ponder,
          'ponder_tadpole': ponder_tadpole, 'cruncher': cruncher}
