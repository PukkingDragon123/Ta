"""S2/WATER: the Sift's water creatures: the Kazoo Fish, the Fanfare Eel, the Sculk Fish, the Gobbler, the Sculk
Coral Organ and the Cypole - geometry and high-resolution skins in the Sculk-mob pipeline (tools/aquakit.py on
modelkit: res=2 + auto detail + the material pass, 4 texels per model unit, glow layers). Every palette comes from where the creature lives:
the cyan water, teal grass and pink coral of the Sift Plains, the pale rainbow Chrome of the Chrome Lakes,
the pink kelp and turquoise water of the Magic Kelp Forest, the near-black water and cyan sculk light of
the Sculk Ocean, the murky teal of the Sculk Swamp.

Hooks: mobs_wild.ALL and cypole.MODELS point here for the models; tools/fish_items.py paints the item sprites
(meats, sushi, buckets, spawn eggs) in these palettes. The Java models (KazooFishModel, FanfareEelModel,
SculkFishModel, GobblerModel; CypoleModel and CoralOrganModel keep their part contract) animate them."""
import math

import numpy as np

import aquakit as AK
from modelkit import Model

M = AK.Mat
up = AK.up

# ---------------------------------------------------------------- shared tones

BRASS = ('#4a2f12', '#6e4618', '#94621f', '#b9842c', '#d6a640', '#ecc866', '#fbe9a6')
BONE = ('#5d5866', '#7f7a82', '#a39b98', '#c4baab', '#ddd3c0', '#eee7d6', '#fbf7ec')
CREAM = ('#6f7f7a', '#94a69c', '#bccbbd', '#d9e4d3', '#ecf2e4', '#f7faf0', '#ffffff')
MOUTH = ('#1c0610', '#330b1c', '#4f1426', '#6e2032', '#8f3240', '#ae4c52', '#c86a68')
EYE = {'P': '#0d0b16', 'I': '#e8b03a', 'J': '#f8d26a', 'i': '#a8701e', 'H': '#fff6d6', 'G': '#c8fff8', 'g': '#29dfeb', 'W': '#e9f6f4',
       'w': '#b9b2a2', 'm': '#2a0a16', 'o': '#120f1c', 'K': '#06090c', 'd': 0.6, 'D': 0.45, 'l': 1.25, 'L': 1.4}
# a round fish eye, 4 x 4 texels: a gold ring, a black pupil with a highlight
FISH_EYE = ['.ii.', 'iPHi', 'IPPJ', '.II.']


def _disc(n, rings):
    """An n x n round decal: rings = [(radius, char), ...] from the inside out; outside them '.'."""
    c = (n - 1) / 2
    out = []
    for y in range(n):
        row = ''
        for x in range(n):
            d = math.hypot(x - c, y - c)
            row += next((ch for rad, ch in rings if d <= rad), '.')
        out.append(row)
    return out


def _sail(length, height, front=0.35):
    """A fin plane rising at the front and sweeping back (a dorsal sail), its rays ending in a ragged edge."""
    def keep(lx, ly, lz):
        t = np.clip((lz - lz.min() + 0.5) / max(length, 1e-6), 0, 1)
        top = height * (front + (1 - front) * np.sin(np.clip(t * 1.25, 0, 1) * math.pi * 0.5)) * (1 - 0.6 * np.clip(t - 0.75, 0, 1) * 4)
        rag = (np.floor(lz) % 2 == 1) * 0.6
        return (-ly) < top - rag
    return keep


def _fork(height, length):
    """A forked tail fan: two lobes and a notch, ragged at the trailing edge."""
    def keep(lx, ly, lz):
        u = (lz - lz.min() + 0.5) / max(length, 1e-6)
        v = np.abs(ly) / max(height / 2, 1e-6)
        notch = (v < 0.45 * u) & (u > 0.55)
        return (v < 0.45 + 0.6 * u) & ~notch & (u <= 1.0)
    return keep


def _round(w, d):
    """A small rounded fin plane (pectorals, pelvics)."""
    def keep(lx, ly, lz):
        a = np.abs(lx if np.ptp(lx) > 0 else ly) / max(w, 1e-6)
        b = (lz - lz.min() + 0.5) / max(d, 1e-6)
        return a * a + (b - 0.4) ** 2 * 1.6 < 1.0
    return keep


def _rays(n=2, depth=-0.12):
    """Fin rays: every n-th texel along the fin a shade darker."""
    def tone(c):
        loc = c['loc']
        along = loc[..., 2]
        return np.where(np.floor(along) % n == 0, depth, 0.0) + np.clip(np.abs(loc[..., 1]) / 6.0, 0, 0.2)
    return tone


# ================================================================ the Kazoo Fish

KAZOO = {  # variant: (body, back/stripes, belly, fins, glow) - each from its waters
    'kazoo_fish': (  # reef teal: the Sift Plains' teal grass and cyan water, fins the pink of its coral
        ('#0e3a45', '#14555e', '#1e7477', '#2f948f', '#4cb3a8', '#7dd3c3', '#b9efe2'),
        ('#0c2a3e', '#123d57', '#1b5570', '#276f88', '#3a8aa0', '#5aa8b8', '#8ccad4'), CREAM,
        ('#6e2a47', '#9a3c5e', '#c25475', '#e0738c', '#f096a6', '#f8bcc4', '#fde0e2'), None),
    'kazoo_fish_sunset': (  # the Magic Kelp Forest: rose and coral among the pink kelp, gold fins
        ('#5a1f3a', '#7f2d4c', '#a8405c', '#cc5c6c', '#e8807e', '#f6a896', '#fdd0b8'),
        ('#3a1640', '#55205a', '#722d73', '#8f418c', '#ab5aa4', '#c57cbe', '#dfa6d6'),
        ('#806a66', '#a68d86', '#c9b0a6', '#e3cfc4', '#f2e4dc', '#faf2ec', '#ffffff'),
        ('#6a3410', '#934c14', '#bb6a1c', '#dc8c2a', '#f0ae46', '#f8cc78', '#fde6b0'), None),
    'kazoo_fish_lagoon': (  # the Chrome Lakes: pale sky-cyan, a lavender back, fins like a film of Chrome
        ('#1a4a6a', '#22648a', '#2f82a8', '#45a2c4', '#6cc2da', '#9cdcec', '#d0f2f8'),
        ('#24285a', '#33397a', '#454f99', '#5a69b4', '#7586cc', '#97a6de', '#c0caee'), CREAM,
        ('#4a3a6e', '#64528e', '#8070ae', '#9e90c8', '#bcb0dc', '#d8d0ec', '#f0ecf8'), None),
    'kazoo_fish_mint': (  # the plains' shallows: mint with teal bands and lemon fins
        ('#16483a', '#1f6450', '#2c8266', '#3fa07c', '#5cbc94', '#86d6b0', '#bdeed2'),
        ('#0f3a3c', '#165050', '#1f6866', '#2b827e', '#3e9c94', '#5cb8ae', '#8ad4ca'), CREAM,
        ('#5a5410', '#7f7618', '#a69a22', '#c8bc36', '#e0d656', '#efe888', '#fbf8c4'), None),
    'kazoo_fish_midnight': (  # the Sculk Ocean: near-black teal with cyan lights along its flanks
        ('#071a20', '#0b262e', '#10343e', '#16444e', '#1e5660', '#2b6a74', '#3f828a'),
        ('#050f16', '#081820', '#0c222c', '#112e38', '#173b46', '#1f4a56', '#2b5c68'),
        ('#16313a', '#1f414a', '#2a535c', '#37666e', '#467a80', '#588e94', '#6ea4a8'),
        ('#0a2a30', '#0f3c44', '#15505a', '#1e6670', '#2a7e88', '#3a98a2', '#52b4bc'), ('#0f8c99', '#29dfeb', '#7ff6f0', '#c8fff8')),
}


def _kazoo_mats(body, back, belly, fin, glow):
    return {
        'skin': M([body, body], base=0.55, noise=0.08, cell=1.8, mottle=None, mottle2=None, belly=belly, back=back, gloss='#f4fffb', gloss_rate=0.03,
                  light=glow, scales=(1.5, 1.0), material='scales', mnoise=0.6),
        'fin': M([fin], base=0.56, noise=0.05, cell=1.4, rim=0.0, ao=0.0, mottle=None, flat=True, material='membrane'),
        'brass': M([BRASS], base=0.58, noise=0.06, cell=1.5, rim=1.0, ao=0.5, gloss='#fff8d8', gloss_rate=0.12, material='metal', mnoise=0.5),
        'mouth': M([MOUTH], base=0.4, noise=0.03, rim=0.2, ao=0.3),
    }


def _stripes(c):
    """Two soft bands round the body (a tang's saddles), worked out along the fish."""
    z = c['wp'][..., 2]
    return ((z > -2.6) & (z < -1.2)) | ((z > 1.4) & (z < 2.6))


def _spots(c):
    """Midnight: a row of lights down each flank."""
    wp = c['wp']
    return ((np.abs(wp[..., 1] - 20.5) < 0.6) & (np.floor(wp[..., 2] + 10) % 2 == 0) & (c['face'] in ('east', 'west')))


def kazoo_fish() -> Model:
    """A little deep-bodied reef fish with a brass kazoo for a snout (KazooFishModel animates it): a chunky
    rounded body with saddle stripes, a sail of a dorsal fin, sculling pectorals, a peduncle and a forked tail."""
    m = Model('kazoo_fish', (64, 32), {}, {'kazoo_fish': {}}, res=2)
    S = dict(mat='skin', back=-1.5, belly=1.0, marks=[(_stripes, 'back', 0)])
    body = m.part('body', pivot=(0, 20, 0))
    body.cube((-1.5, -3, -3), (3, 6, 6), lights=_spots, **S)
    body.cube((-1, -4, -2), (2, 1, 4), **S)
    body.cube((-1, 3, -2), (2, 1, 3), **S)
    eye = dict(decal=FISH_EYE, keys=EYE, at=(0, 1))
    body.cube((-1.5, -2, -5), (3, 4, 2), **S, faces={'east': dict(eye), 'west': dict(eye, mirror=True)})
    kazoo = body.part('kazoo', pivot=(0, 0.5, -5))
    kazoo.cube((-1, -1, -4), (2, 2, 3), mat='brass', faces={'north': dict(decal=_disc(4, [(0.8, 'K'), (1.3, 'm'), (2.2, 'r')]), keys=dict(EYE, r=0.7), at=(0, 0))})
    kazoo.cube((-0.5, -2, -3), (1, 1, 1), mat='brass', light=0.1)
    kazoo.cube((-1.5, -1.5, -1), (3, 3, 1), mat='brass', light=-0.12)
    dorsal = body.part('dorsal', pivot=(0, -4, -1))
    dorsal.cube((0, -3, 0), (0, 3, 5), mat='fin', shape=_sail(5, 3), tone=_rays(), no_occlude=True)
    for side, sx in (('left', 1), ('right', -1)):
        fin = body.part(f'{side}_fin', pivot=(1.5 * sx, 1, -2), rot=(0, 0.3 * sx, 0.5 * sx))
        fin.cube(((0 if sx > 0 else -3), 0, 0), (3, 0, 3), mat='fin', shape=_round(3, 3), tone=_rays(), no_occlude=True)
        pel = body.part(f'{side}_pelvic', pivot=(1 * sx, 3, -1), rot=(0, 0, 0.35 * sx))
        pel.cube((0, 0, 0), (0, 2, 2), mat='fin', shape=_round(2, 2), no_occlude=True)
    anal = body.part('anal_fin', pivot=(0, 3, 1))
    anal.cube((0, 0, 0), (0, 2, 3), mat='fin', tone=_rays(), no_occlude=True, shape=lambda lx, ly, lz: ly < 2 - (lz - lz.min()) * 0.5)
    stem = body.part('tail_stem', pivot=(0, 0, 3))
    stem.cube((-1, -1.5, 0), (2, 3, 2), **S)
    tail = stem.part('tail', pivot=(0, 0, 2))
    tail.cube((0, -3.5, 0), (0, 7, 4), mat='fin', shape=_fork(7, 4), tone=_rays(), no_occlude=True)
    variants = {name: _kazoo_mats(*pal) for name, pal in KAZOO.items()}
    return AK.finish(m, variants, glow_variants=['kazoo_fish_midnight'])


# ================================================================ the Fanfare Eel

EEL = {  # variant: (back, flank, belly, frill, glow)
    'fanfare_eel': (  # the plains' and Chrome Lakes' shallows: moss green over gold, like the reeds and the sun on the water
        ('#16301c', '#1f4426', '#2c5a30', '#3d723a', '#548c46', '#73a858', '#9cc674'),
        ('#3a3a12', '#5a5414', '#7f7418', '#a6961e', '#c8b42c', '#e0cc4a', '#f2e486'),
        ('#6a5e2a', '#8e7e36', '#b2a048', '#d0be62', '#e6d688', '#f3e8b2', '#fbf6dc'),
        ('#20402a', '#2e5836', '#3f7244', '#548c52', '#6ea862', '#8cc27a', '#b4dc9e'), None),
    'fanfare_eel_violet': (  # the Magic Kelp Forest: violet with pink frills, the kelp's own colours
        ('#2a1638', '#3c1f50', '#522a68', '#6a3882', '#844a9c', '#a064b4', '#c08ccc'),
        ('#5a1f48', '#7f2d60', '#a63e78', '#c85690', '#e076a8', '#f09cc0', '#f9c6dc'),
        ('#7a6070', '#9e8090', '#c2a4b2', '#dcc4ce', '#eedce4', '#f8eef2', '#ffffff'),
        ('#6a2050', '#8f2e68', '#b44282', '#d45e9c', '#ea82b6', '#f6aacc', '#fdd2e4'), None),
    'fanfare_eel_deep': (  # the Sculk Ocean: near-black teal, a lateral line of cyan lights
        ('#050f14', '#08181e', '#0c2228', '#112e34', '#173b42', '#1f4a52', '#2b5c64'),
        ('#0b2228', '#103038', '#163e48', '#1e4e58', '#285e68', '#36727a', '#4a8a90'),
        ('#1d343a', '#284449', '#35565a', '#44686b', '#567c7e', '#6c9092', '#86a8a8'),
        ('#0c3036', '#12424a', '#1a565e', '#246c74', '#30848a', '#40a0a4', '#58bcbe'), ('#0f8c99', '#29dfeb', '#7ff6f0', '#c8fff8')),
}
EEL_LEN = (5, 5, 5, 4, 4)
EEL_W = (5, 5, 4, 3, 2)


def _eel_mats(back, flank, belly, frill, glow):
    mats = {
        'skin': M([back, flank], base=0.55, noise=0.08, cell=1.8, mottle=(2.6, 0.7), mottle2=None, belly=belly, gloss='#f6fbe0', gloss_rate=0.04,
                  light=glow, pores=0.03),
        'fin': M([frill], base=0.56, noise=0.05, cell=1.4, rim=0.0, ao=0.0, mottle=None, light=glow, flat=True, material='membrane'),
        'brass': M([BRASS], base=0.58, noise=0.06, cell=1.5, rim=1.0, ao=0.5, gloss='#fff8d8', gloss_rate=0.1, material='metal', mnoise=0.5),
        'mouth': M([MOUTH], base=0.35, noise=0.03, rim=0.2, ao=0.2),
    }
    mats['skin'].flank = mats['skin'].ramps[1]
    return mats


def _bands(c):
    """Gold saddles across the green back, one every few texels down the eel, and a gold lower flank."""
    z = c['wp'][..., 2]
    return (np.floor(z + 40) % 5 == 0) | (c['loc'][..., 1] > 0.0)


def _lateral(c):
    """Deep variant: a lateral line of lights along each flank."""
    if c['face'] not in ('east', 'west'):
        return None
    return (np.abs(c['loc'][..., 1]) < 0.6) & (np.floor(c['wp'][..., 2] + 40) % 2 == 0)


def _frill_shape(length, height):
    def keep(lx, ly, lz):
        t = (lz - lz.min() + 0.5)
        top = height * (0.55 + 0.45 * np.abs(np.sin(t * math.pi / 2.5)))
        return (-ly) < top
    return keep


def fanfare_eel() -> Model:
    """A long segmented eel whose mouth is a brass trumpet bell (FanfareEelModel animates it): a broad head with
    gold eyes and gill slits, the bell on its snout, six tapering segments under a scalloped crest, gold saddles
    and flanks under a green back, pectoral fins and a leaf of a tail fin."""
    m = Model('fanfare_eel', (64, 64), {}, {'fanfare_eel': {}}, res=2)
    head = m.part('head', pivot=(0, 20, -9))
    S = dict(mat='skin', belly=1.2, marks=[(_bands, 'flank', 0)])
    eye = dict(decal=['............', '..' + FISH_EYE[0], '..' + FISH_EYE[1], '..' + FISH_EYE[2], '..' + FISH_EYE[3], '',
                      '.......D.D.D', '.......D.D.D', '.......D.D.D', '........d.d.'], keys=EYE, at=(0, 0))
    head.cube((-3, -3, -6), (6, 6, 6), **S, faces={'east': dict(eye), 'west': dict(eye, mirror=True)})
    head.cube((-2, -4, -5), (4, 1, 4), **S)
    head.cube((-2.5, 3, -5), (5, 1, 4), **S)
    pipe = head.part('pipe', pivot=(0, 0.5, -6))
    pipe.cube((-1, -1, -2), (2, 2, 2), mat='brass', light=-0.05)
    bell = pipe.part('bell', pivot=(0, 0, -2))
    bell.cube((-3, -3, -2), (6, 6, 2), mat='brass', faces={'north': dict(decal=_disc(12, [(1.6, 'K'), (2.7, 'm'), (3.6, 'M'), (4.6, 'r'), (5.2, 'L'), (6.2, 's')]),
                                                                           keys=dict(EYE, K='#0a0308', M='#4f1426', r=0.75, s=0.85), at=(0, 0))})
    for side, sx in (('left', 1), ('right', -1)):
        fin = head.part(f'{side}_fin', pivot=(3 * sx, 1.5, -1), rot=(0, 0.5 * sx, 0.3 * sx))
        fin.cube(((0 if sx > 0 else -3), 0, 0), (3, 0, 3), mat='fin', shape=_round(3, 3), tone=_rays(), no_occlude=True)
    prev, z0 = head, 0
    for i, (ln, w) in enumerate(zip(EEL_LEN, EEL_W)):
        seg = prev.part(f'segment_{i}', pivot=(0, 0, z0 - (1 if i else 0)))  # each overlaps the one before
        seg.cube((-w / 2, -w / 2, 0), (w, w, ln), lights=_lateral, **S)
        fr = seg.part(f'frill_{i}', pivot=(0, -w / 2, 0))
        fr.cube((0, -2, 0), (0, 2, ln), mat='fin', shape=_frill_shape(ln, 2), tone=_rays(2, -0.1), no_occlude=True)
        prev, z0 = seg, ln
    tail = prev.part('tail_fin', pivot=(0, 0, z0))
    tail.cube((0, -3, 0), (0, 6, 5), mat='fin', shape=lambda lx, ly, lz: np.abs(ly) < 3.2 - np.abs((lz - lz.min() + 0.5) - 2.2) * 1.1,
              tone=_rays(), no_occlude=True)
    variants = {name: _eel_mats(*pal) for name, pal in EEL.items()}
    return AK.finish(m, variants, glow_variants=['fanfare_eel_deep'])


# ================================================================ the Sculk Fish

SCULK = {  # variant: (skin, back, belly, fin, light)
    'sculk_fish': (  # the Sculk Ocean: near-black teal, its water's own colour, cyan sculk light
        ('#08181e', '#0c232a', '#113038', '#173e46', '#1f4e56', '#2b626a', '#3d7a80'),
        ('#040b10', '#071218', '#0a1a22', '#0e242c', '#133038', '#1a3e46', '#245058'),
        ('#16282c', '#1f3539', '#2a4446', '#365456', '#456668', '#58797a', '#6e8e8e'),
        ('#0a242a', '#0e3238', '#134248', '#1a545a', '#24686e', '#327e84', '#46989c'), ('#0f8c99', '#29dfeb', '#7ff6f0', '#d8fffa')),
    'sculk_fish_pale': (  # the swamp's Sculk Water: bleached bone-grey over murky green-teal
        ('#3a4a46', '#4f625c', '#667a72', '#80948a', '#9caea2', '#b8c8ba', '#d6e2d4'),
        ('#1d3532', '#284642', '#345852', '#426a62', '#527c72', '#668e84', '#7ea49a'),
        ('#6c7a70', '#8a988c', '#a8b4a8', '#c4cec2', '#dce4d8', '#eef2ea', '#fbfcf8'),
        ('#22423e', '#2e5650', '#3c6a62', '#4c7e74', '#5e9288', '#76a89e', '#94c0b6'), ('#2e8a7a', '#4fd6b8', '#a2f2dc', '#e0fff2')),
    'sculk_fish_deep': (  # the ocean floor: black-violet, violet and cyan lights
        ('#0b0716', '#120c22', '#1a1230', '#22193e', '#2c214e', '#382b60', '#4a3a76'),
        ('#05030b', '#090612', '#0e091a', '#130d24', '#1a122e', '#22183a', '#2c2048'),
        ('#1c1828', '#262036', '#322a44', '#3e3454', '#4c4066', '#5c4e7a', '#706090'),
        ('#160e2a', '#20143a', '#2a1c4a', '#36265c', '#443270', '#544086', '#6a529e'), ('#5a3fc0', '#9a7cff', '#c8b4ff', '#eee6ff')),
}


def _sculk_mats(skin, back, belly, fin, light):
    return {
        'skin': M([skin, back], base=0.5, noise=0.09, cell=1.6, mottle=(2.2, 0.66), mottle2=None, belly=belly, back=back, gloss=None, light=light,
                  spots=(None, 2.0, 0.78, 2), scales=(1.5, 1.0), material='scales', mnoise=0.6),
        'fin': M([fin], base=0.52, noise=0.06, cell=1.4, rim=0.0, ao=0.0, mottle=None, light=light, flat=True, material='membrane'),
        'bone': M([BONE], base=0.62, noise=0.04, cell=1.5, rim=0.5, ao=0.3, material='bone'),
        'mouth': M([MOUTH], base=0.32, noise=0.03, rim=0.2, ao=0.2),
    }


def _photophores(c):
    """A row of lights down each flank, every other texel."""
    if c['face'] not in ('east', 'west'):
        return None
    wp = c['wp']
    return (np.abs(wp[..., 1] - 20.5) < 0.6) & (np.floor(wp[..., 2] + 20) % 2 == 0)


def _tendril_tip(c):
    """The last texels of a brow tendril glow."""
    return c['loc'][..., 2] > 3.0


def sculk_fish() -> Model:
    """A small deep-bodied biter of the Sculk Ocean (SculkFishModel animates it): a blunt heavy head with an
    underbite jaw of bone fangs, glowing eyes, two sensor tendrils on the brow, a row of lights down its
    flank, a spiny dorsal, a peduncle and a fan of a tail."""
    m = Model('sculk_fish', (64, 32), {}, {'sculk_fish': {}}, res=2)
    S = dict(mat='skin', back=-1.5, belly=1.5)
    body = m.part('body', pivot=(0, 20, 0))
    body.cube((-1.5, -3, -3), (3, 6, 6), lights=_photophores, **S)
    body.cube((-1, -4, -2), (2, 1, 4), **S)
    eye = dict(decal=['.oo.', 'oGgo', 'oggo', '.oo.'], keys=EYE, at=(0, 1), glow_keys='Gg')
    body.cube((-1.5, -3, -5), (3, 4, 2), **S, faces={'east': dict(eye), 'west': dict(eye, mirror=True)})
    body.cube((-1, -3, -6), (2, 2, 1), **S)
    jaw = body.part('jaw', pivot=(0, 1, -2))
    jaw.cube((-1.5, 0, -4), (3, 2, 4), **S, faces={'north': dict(decal=['W.w..W', 'w....w'], keys=EYE, at=(0, 0)), 'up': dict(mat='mouth')})
    for x, z in ((-1.5, -4), (1, -4), (-1.5, -2.5), (1, -2.5)):
        jaw.cube((x, -1, z), (0.5, 1, 0.5), mat='bone', ao=0)
    dorsal = body.part('dorsal', pivot=(0, -4, -1))
    dorsal.cube((0, -3, 0), (0, 3, 4), mat='fin', no_occlude=True, tone=_rays(),
                shape=lambda lx, ly, lz: ((-ly) < 3 - (lz - lz.min()) * 0.5) & ~((np.floor(lz) % 2 == 1) & (-ly > 1)))
    for side, sx in (('left', 1), ('right', -1)):
        fin = body.part(f'{side}_fin', pivot=(1.5 * sx, 1, -2), rot=(0, 0.35 * sx, 0.6 * sx))
        fin.cube(((0 if sx > 0 else -3), 0, 0), (3, 0, 2), mat='fin', shape=_round(3, 2), tone=_rays(), no_occlude=True)
        ten = body.part(f'{side}_tendril', pivot=(0.8 * sx, -3, -4), rot=(-0.6, 0.3 * sx, 0))
        ten.cube((-0.5, -0.5, 0), (1, 1, 4), mat='fin', lights=_tendril_tip, ao=0, no_occlude=True)
    anal = body.part('anal_fin', pivot=(0, 3, 1))
    anal.cube((0, 0, 0), (0, 2, 3), mat='fin', tone=_rays(), no_occlude=True, shape=lambda lx, ly, lz: ly < 2 - (lz - lz.min()) * 0.5)
    stem = body.part('tail_stem', pivot=(0, -0.5, 3))
    stem.cube((-1, -1.5, 0), (2, 3, 2), **S)
    tail = stem.part('tail', pivot=(0, 0, 2))
    tail.cube((0, -3.5, 0), (0, 7, 4), mat='fin', shape=_fork(7, 4), tone=_rays(), no_occlude=True)
    variants = {name: _sculk_mats(*pal) for name, pal in SCULK.items()}
    return AK.finish(m, variants)


# ================================================================ the Gobbler

ABYSS_LIGHT = ('#0f8c99', '#29dfeb', '#7ff6f0', '#c8fff8')
GOB_SKIN = ('#0a1c22', '#10282f', '#16363e', '#1e464e', '#285860', '#366c72', '#4a8486')
GOB_BACK = ('#060f14', '#0a181e', '#0e2228', '#142d34', '#1a3a42', '#224850', '#2e5a62')
GOB_BELLY = ('#2a3c40', '#3a4e52', '#4e6466', '#647a7a', '#7c9290', '#96aca8', '#b4c8c2')
GOB_FIN = ('#0b2026', '#102c34', '#163a42', '#1e4a52', '#285c64', '#36727a', '#4a8c92')


def _veins(c):
    """Glowing veins meandering over the upper flanks and the back (thin lines of the noise's mid level)."""
    if c['face'] == 'down':
        return None
    v = AK.fbm(c['wp'] * np.array([1.0, 1.6, 1.0]), 5.0, 211, 2)
    return (np.abs(v - 0.5) < 0.02) & (c['loc'][..., 1] < c['loc'][..., 1].max() - 1.5)


def _pits(c):
    """Scattered glowing sensory pits over the blind head, and a row of them along the lip."""
    if c['face'] not in ('east', 'west', 'up', 'north'):
        return None
    wp = np.floor(c['wp'] * 2 + 0.5).astype(np.int64)
    scatter = AK._hash3(wp[..., 0], wp[..., 1], wp[..., 2], 5) < 0.022
    lip = (c['loc'][..., 1] > c['loc'][..., 1].max() - 1.0) & (np.floor(c['wp'][..., 2] * 2 + 80) % 3 == 0) & (c['face'] in ('east', 'west'))
    return scatter | lip


def _tip(at):
    def f(c):
        return c['loc'][..., 2] > at
    return f


def _teeth(part, x0, x1, y, z, down, every=2):
    """A row of bone fangs along a jaw edge (1 x 2 x 1, every other texel)."""
    for x in range(int(x0), int(x1), every):
        part.cube((x, y - (2 if not down else 0), z), (1, 2, 1), mat='bone', ao=0)


def gobbler() -> Model:
    """The blind deep-sea catfish (GobblerModel animates it): a huge eyeless head lined with glowing sensory
    pits, an underbite jaw of bone fangs, long barbels and chin barbels, warden-like sensor tendrils, gill flaps,
    a heavy tapering body threaded with glowing veins, sensor spines on its back and a broad tail fluke."""
    m = Model('gobbler', (128, 128), {}, {'gobbler': {}}, res=2)
    S = dict(mat='skin', back=-2, belly=3.5, lights=_veins)
    body = m.part('body', pivot=(0, 13, 2))
    body.cube((-7, -6, -6), (14, 12, 12), **S)
    body.cube((-6, -7, -5), (12, 1, 10), **S)
    body.cube((-6, 6, -5), (12, 1, 9), **S)
    for i in range(4):
        sp = body.part(f'spine_{i}', pivot=(0, -7, -4 + i * 3), rot=(0.5, 0, 0))
        sp.cube((0, -4 + (i % 2), 0), (0, 4 - (i % 2), 2), mat='fin', lights=lambda c: c['loc'][..., 1] < -2.5, no_occlude=True)
    head = body.part('head', pivot=(0, -1, -6))
    H = dict(S, lights=_pits)
    head.cube((-8, -6, -14), (16, 8, 14), **H, faces={'down': dict(mat='mouth')})
    head.cube((-7, -7, -12), (14, 1, 10), **H)
    head.cube((-7, -5, -16), (14, 6, 2), **H, faces={'down': dict(mat='mouth')})
    _teeth(head, -6, 7, 2, -15.5, True)
    for side, sx in (('left', 1), ('right', -1)):
        x = 7.5 if sx > 0 else -8.5
        for z in (-13, -10, -7):
            head.cube((x, 2, z), (1, 2, 1), mat='bone', ao=0)
    jaw = head.part('jaw', pivot=(0, 2, -2))
    jaw.cube((-7.5, 0, -16), (15, 4, 16), mat='skin', belly=1.5, faces={'up': dict(mat='mouth')})
    jaw.cube((-6, 4, -14), (12, 1, 12), mat='skin', belly_all=True)
    _teeth(jaw, -6, 7, 0, -15.5, False)
    for x in (6.5, -7.5):
        for z in (-12, -9, -6):
            jaw.cube((x, -2, z), (1, 2, 1), mat='bone', ao=0)
    for side, sx in (('left', 1), ('right', -1)):
        cb = jaw.part(f'{side}_chin_barbel', pivot=(3 * sx, 4, -13), rot=(0.25, 0, 0.15 * sx))
        cb.cube((-0.5, 0, -0.5), (1, 5, 1), mat='fin', lights=lambda c: c['loc'][..., 1] > 3.5)
        bar = head.part(f'{side}_barbel', pivot=(7 * sx, 0, -14), rot=(0.35, 0.7 * sx, 0))
        bar.cube((-0.5, -0.5, -7), (1, 1, 7), mat='fin')
        tip = bar.part(f'{side}_barbel_tip', pivot=(0, 0, -7), rot=(0.4, 0, 0))
        tip.cube((-0.5, -0.5, -6), (1, 1, 6), mat='fin', lights=lambda c: c['loc'][..., 2] < -4.5)
        ten = head.part(f'{side}_tendril', pivot=(4 * sx, -7, -6), rot=(0.35, 0.3 * sx, 0.2 * sx))
        ten.cube((0, -6, 0), (0, 6, 5), mat='fin', no_occlude=True, lights=lambda c: c['loc'][..., 1] < -4.5,
                 shape=lambda lx, ly, lz: (lz - lz.min()) < 4.6 - (-ly) * 0.55)
        gill = body.part(f'{side}_gill', pivot=(7 * sx, -1, -5), rot=(0, 0.3 * sx, 0))
        gill.cube((0, -4, 0), (0, 8, 3), mat='fin', no_occlude=True, tone=_rays(1, -0.12))
        fin = body.part(f'{side}_fin', pivot=(7 * sx, 4, -1), rot=(0, 0.4 * sx, 0.5 * sx))
        fin.cube(((0 if sx > 0 else -7), 0, 0), (7, 0, 6), mat='fin', shape=_round(7, 6), tone=_rays(), no_occlude=True)
    tail = body.part('tail', pivot=(0, 0, 6))
    tail.cube((-5.5, -5, 0), (11, 10, 10), **S)
    dorsal = tail.part('dorsal', pivot=(0, -5, 1))
    dorsal.cube((0, -4, 0), (0, 4, 8), mat='fin', shape=_sail(8, 4, 0.5), tone=_rays(), no_occlude=True)
    tail2 = tail.part('tail2', pivot=(0, 0, 10))
    tail2.cube((-3.5, -3.5, 0), (7, 7, 9), **S)
    fluke = tail2.part('fluke', pivot=(0, 0, 9))
    fluke.cube((0, -8, 0), (0, 16, 7), mat='fin', shape=_fork(16, 7), tone=_rays(), no_occlude=True)
    mats = {
        'skin': M([GOB_SKIN, GOB_BACK], base=0.5, noise=0.09, cell=2.0, mottle=(3.0, 0.64), mottle2=None, belly=GOB_BELLY, back=GOB_BACK,
                  spots=(None, 3.0, 0.76, 2), light=ABYSS_LIGHT, gloss='#bfe9ea', gloss_rate=0.03, pores=0.03),
        'fin': M([GOB_FIN], base=0.52, noise=0.06, cell=1.5, rim=0.0, ao=0.0, mottle=None, light=ABYSS_LIGHT, flat=True, material='membrane'),
        'bone': M([BONE], base=0.64, noise=0.04, cell=1.5, rim=0.5, ao=0.2, material='bone'),
        'mouth': M([MOUTH], base=0.3, noise=0.04, rim=0.2, ao=0.4),
    }
    return AK.finish(m, {'gobbler': mats})


# ================================================================ the Sculk Coral Organ

ROCK = ('#081418', '#0d1e24', '#122a30', '#18363e', '#20444c', '#2c565c', '#3c6c70')
PIPE = ('#0b2a30', '#0f3840', '#154852', '#1d5a64', '#286e76', '#36848a', '#4c9ea0')
CORAL = ('#0c3a40', '#104c52', '#166066', '#1e767a', '#2a8e90', '#3ca8a6', '#58c4be')


def _rings(c):
    """Bands of light round each pipe, every few texels up."""
    if c['face'] in ('up', 'down'):
        return None
    return np.floor(c['wp'][..., 1] * 2 + 80) % 9 == 0


def _pipe_mouth(w):
    """The organ pipe's mouth (texel scale): a pale upper lip over a dark slot that narrows into the pipe,
    and a soft shadow under it."""
    n = 2 * w
    return ['.' * n, '.' + 'L' * (n - 2) + '.', '.' + 'l' * (n - 2) + '.', '.' + 'D' * (n - 2) + '.', '..' + 'K' * (n - 4) + '..',
            '..' + 'K' * (n - 4) + '..', '...' + 'D' * (n - 6) + '...', '..' + 'd' * (n - 4) + '..']


def coral_organ() -> Model:
    """The Sculk Coral Organ, a living thing (CoralOrganModel animates it; part names are its contract): a fleshy
    sculk-coral body rooted on a stony footing on the Sculk Ocean floor, split across the front by a GIANT mouth -
    the upper body ('head') lifts back like a clam's lid to bare rows of bone teeth, a red maw and a tongue; the
    lower body ('body') is the toothed bowl of the jaw. A dozen eyes of every size stare out of the head, each with
    its own glowing slit iris ('pupil_<i>') that slides to track prey and a fleshy lid ('lid_<i>') that blinks. Its
    organ pipes ('pipe_<i>') still grow from its back and sound its chords; tendrils and coral fans sway round the
    footing. Glowing veins and pores light its flesh."""
    m = Model('coral_organ', (128, 128), {}, {'coral_organ': {}}, res=2)
    R = dict(mat='rock', back=-1)
    F = dict(mat='flesh', back=-99, lights=_flesh_lights)
    base = m.part('base', pivot=(0, 24, 0))
    base.cube((-10, -3, -9), (20, 3, 18), **R)
    for i, (x, z, w, d) in enumerate(((-11, -4, 3, 5), (9, 2, 3, 6), (-5, 8.5, 7, 2), (2, -10.5, 6, 2))):
        base.cube((x, -2, z), (w, 2, d), **R)  # roots spreading over the sea floor
    body = base.part('body', pivot=(0, -3, 0))
    # the toothed bowl of the jaw; its top is the floor of the maw
    lip = ['RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR', 'rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr']
    side_lip = ['R' * 32, 'r' * 32]
    body.cube((-9, -7, -8), (18, 7, 16), **F, faces={'up': dict(mat='maw', lights=None, decal=_maw_floor(36, 32), keys=MAW_KEYS),
                                                      'north': dict(decal=lip, keys=MAW_KEYS),
                                                      'east': dict(decal=side_lip, keys=MAW_KEYS), 'west': dict(decal=side_lip, keys=MAW_KEYS)})
    body.cube((-8, -1, -7), (16, 1.5, 14), **F)
    for x in range(-8, 9, 2):
        body.cube((x - 0.5, -9, -7.7), (1, 2, 1), mat='bone', ao=0)  # the lower teeth, hidden while the mouth is shut
    for z in range(-5, 7, 3):
        for sx in (1, -1):
            body.cube((sx * 8 - 0.5, -8.5, z - 0.5), (1, 1.5, 1), mat='bone', ao=0)
    for x in (-4.5, 3.5):
        body.cube((x, -9.5, -8.9), (1, 2.5, 1), mat='bone', ao=0)  # two tusks jutting up in front of the upper lip
    tongue = body.part('tongue', pivot=(0, -7.2, 5))
    tongue.cube((-3.5, -1, -11), (7, 1.2, 11), mat='tongue', faces={'up': dict(decal=_tongue_groove(14, 22), keys=MAW_KEYS)})
    # the upper body: the lid of the mouth, the dome of eyes and the organ pipes
    head = body.part('head', pivot=(0, -7, 7))
    head.cube((-9, -10, -15), (18, 10, 15), **F, faces={'down': dict(mat='maw', lights=None, decal=_palate(36, 30), keys=MAW_KEYS),
                                                         'north': dict(decal=['.' * 36] * 18 + ['r' * 36, 'R' * 36], keys=MAW_KEYS),
                                                         'east': dict(decal=['.' * 30] * 18 + ['r' * 30, 'R' * 30], keys=MAW_KEYS),
                                                         'west': dict(decal=['.' * 30] * 18 + ['r' * 30, 'R' * 30], keys=MAW_KEYS)})
    head.cube((-7.5, -13, -13), (15, 3, 12), **F)
    head.cube((-5, -15, -10), (10, 2, 8), **F)
    for x in range(-7, 8, 2):
        head.cube((x - 0.5, 0, -14.6), (1, 2, 1), mat='bone', ao=0)  # the upper teeth
    for z in range(-12, 0, 3):
        for sx in (1, -1):
            head.cube((sx * 8 - 0.5, 0, z), (1, 1.5, 1), mat='bone', ao=0)
    for x in (-6.5, 5.5):
        head.cube((x, -0.5, -16.2), (1, 3.5, 1), mat='bone', ao=0)  # two great fangs hanging over the lower lip
    for i, (x, y, z, s, rx, ry) in enumerate(ORGAN_EYES):
        eye = head.part(f'eye_{i}', pivot=(x, y, z), rot=(rx, ry, 0))
        eye.cube((-s / 2, -s / 2, -s / 2), (s, s, s), mat='sclera', back=99)
        n = s * 0.8
        pupil = eye.part(f'pupil_{i}', pivot=(0, 0, -s / 2 - 0.04))
        tex = int(math.ceil(n)) * 2
        pupil.cube((-n / 2, -n / 2, 0), (n, n, 0), mat='sclera', faces={'north': dict(decal=_slit_eye(tex), keys=EYE_KEYS, glow_keys='Gg'),
                                                                         'south': dict(skip=True)})
        lid = eye.part(f'lid_{i}', pivot=(0, -s / 2 - 0.2, 0))  # CoralOrganModel scales it down over the eye to blink
        lid.cube((-s / 2 - 0.2, 0, -s / 2 - 0.2), (s + 0.4, s + 0.4, s + 0.4), **F, faces={
            'north': dict(decal=['l' * 12], keys=MAW_KEYS, at='bottom'), 'down': dict(decal=['l' * 12] * 12, keys=MAW_KEYS)})
    for i, (x, z, w, h) in enumerate(((-4.5, -4, 3, 9), (-1.5, -2.5, 3, 13), (2, -3.5, 4, 16), (5, -5, 3, 11), (-6, -7, 2, 7))):
        p = head.part(f'pipe_{i}', pivot=(x, -13 if abs(x) > 4 else -15, z), rot=((0.08, -0.05, 0.04, -0.07, 0.1)[i], 0, (0.12, 0.04, -0.03, -0.12, 0.2)[i]))
        p.cube((-w / 2 - 0.5, -2, -w / 2 - 0.5), (w + 1, 2, w + 1), **F, faces={'up': dict(skip=True)})
        p.cube((-w / 2, -h, -w / 2), (w, h, w), mat='pipe', lights=_rings, back=-h + 1,
               faces={'north': dict(decal=_pipe_mouth(w), keys=dict(EYE, L='#cfe8e2', l='#8fb4b0', D='#0a1418', K='#020406'), at=(0, 2 * (h // 3))),
                      'up': dict(decal=_disc(2 * w, [(w - 1.6, 'K'), (w - 0.9, 'D')]), keys=dict(EYE, D='#0a1418', K='#020406'), at=(0, 0))})
        p.cube((-w / 2 - 0.5, -h - 1, -w / 2 - 0.5), (w + 1, 1, w + 1), mat='bone', faces={'up': dict(skip=True)})
    for i, (x, z, ry) in enumerate(((-10, -6, 0.6), (10, -6, -0.6), (-10, 6, 2.4), (10, 6, -2.4))):
        t = base.part(f'tendril_{i}', pivot=(x, -2, z), rot=(0, ry, 0))
        t.cube((0, -9, 0), (0, 9, 2), mat='coral', no_occlude=True, lights=lambda c: c['loc'][..., 1] < -7,
               shape=lambda lx, ly, lz: (np.floor(-ly * 2) % 3 != 2) | ((lz - lz.min()) < 1))
    for i, (x, z, h) in enumerate(((-9, -8, 5), (8, -8, 4), (-11, 3, 6), (11, 4, 5), (-4, 9, 4), (5, 9, 6))):
        c = base.part(f'coral_{i}', pivot=(x, -3, z), rot=(0, 0.6 * i, 0))
        fan = dict(mat='coral', no_occlude=True, lights=lambda c: c['loc'][..., 1] < -3.5,
                   shape=lambda lx, ly, lz: (np.abs(lx + lz) < 0.8 + (-ly) * 0.45) & (((-ly + lx + lz) % 1.0 < 0.5) | ((-ly - lx - lz) % 1.0 < 0.5)))
        c.cube((-2, -h, 0), (4, h, 0), **fan)
        c.cube((0, -h, -2), (0, h, 4), **fan)
    dim = ('#0a4a54', '#0f7480', '#1fb6c2', '#5fe6e6')
    mats = {
        'rock': M([ROCK, PIPE], base=0.5, noise=0.1, cell=2.0, mottle=(2.6, 0.62), back=ROCK, light=dim, spots=(None, 2.4, 0.76, 2), pores=0.06,
                  material='stone'),
        'flesh': M([FLESH, CRUST], base=0.52, noise=0.09, cell=1.8, mottle=(2.4, 0.66), back=FLESH, light=ABYSS_LIGHT, gloss='#bff2ee',
                   gloss_rate=0.035, pores=0.04, spots=(BRUISE, 2.2, 0.74, 0), material='skin'),
        'maw': M([MAW], base=0.48, noise=0.06, cell=1.5, mottle=None, rim=0.4, ao=0.8, gloss='#f2b8c0', gloss_rate=0.05, material='skin'),
        'tongue': M([TONGUE], base=0.55, noise=0.05, cell=1.5, mottle=None, rim=0.3, gloss='#ffd8d8', gloss_rate=0.06, material='skin'),
        'sclera': M([SCLERA], base=0.62, noise=0.04, cell=1.4, mottle=None, rim=0.6, ao=0.5, gloss='#ffffff', gloss_rate=0.08, material='jelly',
                    mnoise=0.4),
        'pipe': M([PIPE, ROCK], base=0.55, noise=0.08, cell=1.8, mottle=(2.2, 0.72), back=PIPE, light=dim, pores=0.04, material='stone'),
        'bone': M([BONE], base=0.6, noise=0.05, cell=1.6, rim=0.7, ao=0.4, material='bone'),
        'coral': M([CORAL], base=0.55, noise=0.05, cell=1.4, rim=0.0, ao=0.0, mottle=None, light=ABYSS_LIGHT, material='plant'),
    }
    return AK.finish(m, {'coral_organ': mats})


FLESH = ('#0c1720', '#13222e', '#1b2f3d', '#253d4c', '#31505e', '#406572', '#567e88')
CRUST = ('#0e2c30', '#143a3e', '#1c4c4e', '#266060', '#337674', '#468e8a', '#62aaa2')  # stony coral crusting the flesh
BRUISE = ('#1e1430', '#2a1c42', '#382654', '#483268', '#5a407c', '#6e5292', '#8a6aac')  # violet blotches
MAW = ('#1a0410', '#2e0818', '#4a0f24', '#681a32', '#872842', '#a63c54', '#c45a6a')
SCLERA = ('#5e5c48', '#7c7a60', '#9c987a', '#bab496', '#d4ceb0', '#e8e4ca', '#f8f6e6')
MAW_KEYS = {'R': '#7a1c30', 'r': '#3a0814', 'D': '#120208', 'T': '#4a0f24', 'l': 0.6, 'v': '#5a1428'}
EYE_KEYS = {'G': '#7ff6f0', 'g': '#1fb6c2', 'P': '#04080a', 'H': '#f4fffc', 'o': '#062a30'}
# the eyes: (x, y, z on the head, size, pitch, yaw) - a big one in the middle of its face, the rest all over
ORGAN_EYES = ((0, -6, -15, 4, 0.0, 0.0), (-5.5, -4.5, -15, 3, 0.0, 0.15), (5.5, -5, -15, 3, 0.0, -0.2), (-3, -9.5, -14, 2, -0.5, 0.1),
              (3.5, -9, -14.5, 2, -0.4, -0.15), (9, -6, -10, 3, 0.0, -0.95), (-9, -4.5, -11, 3, 0.0, 0.9), (9, -3, -4, 2, 0.0, -1.45),
              (-9, -7.5, -5, 2, -0.1, 1.45), (-3.5, -12.5, -12.5, 2, -0.6, 0.1), (3.5, -13, -9, 3, -0.95, -0.3), (-1.5, -15, -6, 2, -1.35, 0.0))


def _flesh_lights(c):
    """Glowing veins wandering over the flesh, and a scatter of glowing pores."""
    if c['face'] == 'down':
        return None
    v = AK.fbm(c['wp'] * np.array([1.0, 1.4, 1.0]), 4.5, 307, 2)
    wp = np.floor(c['wp'] * 2 + 0.5).astype(np.int64)
    return (np.abs(v - 0.5) < 0.012) | (AK._hash3(wp[..., 0], wp[..., 1], wp[..., 2], 41) < 0.008)


def _maw_floor(w, h):
    """The floor of the maw: ridged red flesh round a dark gullet at the back."""
    rows = []
    for y in range(h):
        row = ''
        for x in range(w):
            gx, gy = (x - w / 2 + 0.5) / (w * 0.22), (y - h * 0.22) / (h * 0.16)
            if gx * gx + gy * gy < 1.0:
                row += 'D'
            elif gx * gx + gy * gy < 1.6:
                row += 'r'
            elif y % 4 == 3 and 2 < x < w - 3:
                row += 'v'
            else:
                row += '.'
        rows.append(row)
    return rows


def _palate(w, h):
    """The roof of the maw: ridges across it and a dark groove down the middle."""
    return [''.join('r' if abs(x - w / 2 + 0.5) < 1 else ('v' if y % 4 == 1 else '.') for x in range(w)) for y in range(h)]


def _tongue_groove(w, h):
    return [''.join('T' if abs(x - w / 2 + 0.5) < 1 and y > 2 else '.' for x in range(w)) for y in range(h)]


def _slit_eye(n):
    """An n x n round glowing eye: a pale rim, a cyan iris (bright inside, deeper outside), a black slit of a
    pupil and a white glint; outside the circle cut away."""
    c = (n - 1) / 2
    rows = []
    for y in range(n):
        row = ''
        for x in range(n):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            if r > n / 2 + 0.1:
                row += '_'
            elif abs(dx) < 0.6 and abs(dy) < n * 0.36:
                row += 'P'
            elif r > n / 2 - 0.45:
                row += 'o'
            elif x == int(c - n * 0.22) and y == int(c - n * 0.22):
                row += 'H'
            elif r < n * 0.3:
                row += 'G'
            else:
                row += 'g'
        rows.append(row)
    return rows


# ================================================================ the Cypole

SWAMP_SKIN = ('#13292a', '#1b3836', '#244944', '#2f5c54', '#3d7066', '#50867a', '#6aa092')
SWAMP_BACK = ('#0c1d1c', '#122826', '#183431', '#20413d', '#294f49', '#345e57', '#446f67')
SWAMP_BELLY = ('#4a5a50', '#627266', '#7c8c7e', '#98a696', '#b4c0b0', '#ced8ca', '#e8eee2')
PATINA = ('#1d4a42', '#2a6156', '#3a786a', '#4f9080', '#6aa896', '#8cc2b0', '#b4dccc')
SAC_SW = ('#1f3e3a', '#2a504a', '#38645c', '#4a7a70', '#609084', '#7aa89a', '#9ac2b4')
TONGUE = ('#4c1426', '#6c1f34', '#8e2e44', '#b04256', '#cc5c6a', '#e27e84', '#f2a6a2')
ALGAE = ('#1e3a22', '#2a4e2c', '#3a6438', '#4f7c46', '#689654', '#86b066', '#a8c87e')  # lichen and duckweed blotches on its back
TIP = ('#2b6b5a', '#3a8a74', '#4fae90', '#6ccdaa', '#90e6c4', '#b8f6dc', '#e2fff2')
CYPOLE_MOUTH = (0.0, 15.6, -10.6)  # = tools/cypole.py MOUTH (Cypole.MOUTH_FORWARD / MOUTH_UP in Java)


def _warts(c):
    """Warty bumps on the back: lone darker texels with a lit pixel above."""
    h = AK.fbm(c['wp'] * 1.0, 1.1, 77, 1)
    return h > 0.7


def _patina(c):
    """Verdigris blooming on the brass."""
    return AK.fbm(c['wp'] + 5.0, 1.8, 91, 2) > 0.6


def _disc_grooves(c):
    """Concentric hammered grooves on a cymbal plate."""
    loc = c['loc']
    r = np.sqrt((loc[..., 1] - 3.0) ** 2 + (loc[..., 2] - 3.0) ** 2) if np.ptp(loc[..., 1]) > 0 else 0
    return np.where(np.floor(r * 1.0) % 2 == 0, -0.1, 0.06)


def cypole() -> Model:
    """The one-eyed cymbal frog of the Sculk Swamp (CypoleModel animates it; part names and pivots are its
    contract): a squat warty frog the murky teal of the swamp water, a pale belly, one great golden eye on a
    domed mound (iris, cornea and two lids), brass tympana and two brass cymbal plates by its vocal sac, all
    blooming with verdigris, frog legs and hands, and the sticky tongue with its glowing tip."""
    m = Model('cypole', (128, 64), {}, {'cypole': {}}, res=2)
    S = dict(mat='skin', back=-1)
    W = dict(mat='skin', back=99, marks=[(_warts, 'warts', -2)])
    for side, sx in (('left', 1), ('right', -1)):
        leg = m.part(f'{side}_leg', pivot=(5 * sx, 19.5, 4), rot=(0.12, 0.2 * sx, 0))
        x0 = -1 if sx > 0 else -3
        leg.cube((x0, -3, -4), (4, 5, 8), **S, belly=1.0)
        leg.cube((x0 + 0.5, -4, -2.5), (3, 1, 5), **W)
        shin = leg.part(f'{side}_shin', pivot=(0.5 * sx, 1.5, 3.5))
        shin.cube((-1, -1, -7), (2, 2, 7), **S, belly=0.5)
        foot = shin.part(f'{side}_foot', pivot=(0, 1, -6.5), rot=(0, -0.45 * sx, 0))
        foot.cube((-2.5, 0, -5), (5, 1, 5), mat='web', faces={'north': dict(decal=up(['.D.D.']), keys=EYE, at=(0, 0))})
    body = m.part('body', pivot=(0, 19.5, 2), rot=(-0.26, 0, 0))
    body.cube((-6.5, -4, -6.5), (13, 3, 13), **S, belly=-2.0)
    body.cube((-6, -1, -6), (12, 2, 12), mat='belly')
    body.cube((-5.5, -5, -6), (11, 1, 11), **W)
    body.cube((-3, -6, -4), (6, 1, 8), **W)
    body.cube((-4.5, -4, 6), (9, 4, 1), **S)
    head = body.part('head', pivot=(0, -2.5, -6.5), rot=(0.26, 0, 0))
    head.cube((-6, -3, -7), (12, 3, 7), **S)
    head.cube((-5, -3, -8), (10, 3, 1), **S, faces={'north': dict(decal=up(['..........', '...D..D...']), keys=EYE, at=(0, 0))})
    head.cube((-4.5, -4, -6), (9, 1, 6), **W)
    eye = head.part('eye', pivot=(0, -3.5, -3.5), rot=(0.08, 0, 0))
    eye.cube((-3, -5, -3), (6, 5, 6), mat='skin', back=99, marks=[(_warts, 'warts', -2)])
    eye.cube((-2.5, -6, -2.5), (5, 1, 5), **W)
    eye.cube((-2, -4.5, -4), (4, 4, 1), mat='cornea')
    iris = eye.part('iris', pivot=(0, -2.5, -4.1))
    iris.cube((-2, -2, 0), (4, 4, 0), mat='iris', faces={'north': dict(decal=['ooJJJJoo', 'oJHHJJIo', 'JHJJJJIi', 'PPPPPPPP', 'oPPPPPPo', 'IIJJJIii', 'oiIIIiio', 'ooiiiioo'],
                                                          keys=EYE, at=(0, 0)),
                                                          'south': dict(skip=True)})
    upper = eye.part('upper_lid', pivot=(0, -5, -4.2))
    upper.cube((-3, 0, 0), (6, 5, 0), mat='skin', back=99, faces={'north': dict(decal=up(['......', '......', '......', '......', 'dddddd']),
                                                                                    keys=EYE, at=(0, 0))})
    lower = eye.part('lower_lid', pivot=(0, -0.5, -4.2))
    lower.cube((-3, -5, 0), (6, 5, 0), mat='skin', back=-99, faces={'north': dict(decal=up(['dddddd']), keys=EYE, at=(0, 0))})
    for side, sx in (('left', 1), ('right', -1)):
        tym = head.part(f'{side}_tympanum', pivot=(6 * sx, -1.5, -3))
        tym.cube((0, -2.5, -2.5), (0, 5, 5), mat='brass', tone=_disc_grooves, marks=[(_patina, 'patina', 0)], no_occlude=True)
        tym.cube(((0 if sx > 0 else -1), -1, -1), (1, 2, 2), mat='brass', light=-0.1)
    jaw = head.part('jaw', pivot=(0, 0, -0.5))
    jaw.cube((-5.5, 0, -7), (11, 2, 7), mat='belly', faces={'up': dict(mat='mouth')})
    jaw.cube((-4.5, 0, -8), (9, 1, 1), mat='belly')
    sac = jaw.part('throat', pivot=(0, 1.5, -4))
    sac.cube((-3, -0.5, -3), (6, 2, 5), mat='sac')
    for side, sx in (('left', 1), ('right', -1)):
        plate = jaw.part(f'{side}_plate', pivot=(3 * sx, 1, -6.5), rot=(0.4, 0, -0.32 * sx))
        plate.cube((0, 0, 0), (0, 6, 6), mat='brass', tone=_disc_grooves, marks=[(_patina, 'patina', 0)], no_occlude=True,
                   shape=lambda lx, ly, lz: ((ly - 3) ** 2 + (lz - 3) ** 2) < 10.5)
        plate.cube(((0 if sx > 0 else -1), 2, 2), (1, 2, 2), mat='brass', light=0.08)
        plate.cube(((-1 if sx > 0 else 0), -0.5, -0.5), (1, 1, 1), mat='skin')
        arm = m.part(f'{side}_arm', pivot=(4.5 * sx, 19, -4.5), rot=(-0.15, 0, -0.22 * sx))
        arm.cube((-1, -0.5, -1), (2, 4, 2), **S, faces={'north': dict(mat='belly')})
        fore = arm.part(f'{side}_forearm', pivot=(0, 3.5, 0), rot=(0.15, 0, 0.22 * sx))
        fore.cube((-1, 0, -1), (2, 1, 2), **S)
        hand = fore.part(f'{side}_hand', pivot=(0, 1, 0), rot=(0, -0.3 * sx, 0))
        hand.cube((-2, 0, -3), (4, 1, 4), mat='web', faces={'north': dict(decal=up(['D.D.']), keys=EYE, at=(0, 0))})
    tongue = m.part('tongue', pivot=CYPOLE_MOUTH)
    tongue.cube((-1, -0.5, -1), (2, 1, 1), mat='tongue')
    tip = m.part('tongue_tip', pivot=CYPOLE_MOUTH)
    tip.cube((-1, -1, -2), (2, 2, 2), mat='tip', glow=True)
    mats = {
        'skin': M([SWAMP_SKIN, SWAMP_BACK], base=0.52, noise=0.09, cell=1.8, mottle=(2.6, 0.64), back=SWAMP_BACK, belly=SWAMP_BELLY,
                  gloss='#d8f2ea', gloss_rate=0.04, spots=(ALGAE, 2.0, 0.7, 0), pores=0.05),
        'web': M([SWAMP_SKIN], base=0.45, noise=0.06, cell=1.5, rim=0.6, ao=0.3, mottle=None, pores=0.04),
        'belly': M([SWAMP_BELLY, SWAMP_BELLY], base=0.6, noise=0.07, cell=1.8, mottle=None, mottle2=None, ao=0.6),
        'brass': M([BRASS], base=0.58, noise=0.06, cell=1.5, rim=0.6, ao=0.3, gloss='#fff8d8', gloss_rate=0.08, material='metal', mnoise=0.5),
        'cornea': M([('#2a2a1e', '#3c3a24', '#504c2c', '#665e34', '#7c7240', '#94884e', '#b0a464')], base=0.5, noise=0.03, rim=0.3, ao=0.2),
        'iris': M([('#5a3a0e', '#7c5212', '#a06c18', '#c48a22', '#e0a836', '#f0c860', '#fbe6a0')], base=0.6, noise=0.02, rim=0.0, ao=0.0,
                  mottle=None),
        'sac': M([SAC_SW], base=0.6, noise=0.05, cell=1.6, rim=1.0, ao=0.3, mottle=None, gloss='#e2fff4', gloss_rate=0.08, material='jelly'),
        'mouth': M([MOUTH], base=0.35, noise=0.03, rim=0.2, ao=0.3),
        'tongue': M([TONGUE], base=0.55, noise=0.04, rim=0.3, ao=0.0),
        'tip': M([TIP], base=0.6, noise=0.04, rim=0.3, ao=0.0),
    }
    mats['skin'].warts = mats['skin'].ramps[1]
    mats['brass'].patina = PATINA
    return AK.finish(m, {'cypole': mats})


MODELS = {'kazoo_fish': kazoo_fish, 'fanfare_eel': fanfare_eel, 'sculk_fish': sculk_fish, 'gobbler': gobbler, 'coral_organ': coral_organ,
          'cypole': cypole}
