"""P4-DESERT: the creatures of the Rocky Dunes - the Kerkorer, the Jaberora, the Reservoir (and the
Monarch Reservoir) and the Grubs - in the Sculk-mob pipeline (modelkit Models at res=2, the automatic x2
detail and the C2 material pass: 4 texels per model unit), skinned with tools/aquakit.py.

* Kerkorer (`thesift:kerkorer`): a giant chameleon of banded dune rock - rose hide striped with plum strata
  like the Banded Dunestone, a bone casque and dorsal crest, turret eyes that roll on their own, a curled
  tail - that lies still in the sand until it vanishes into it (`kerkorer_camo`, the same skin in Chime Sand
  colours, fades in over it), with a valuable stuck to the pad of its tongue as bait.
* Jaberora (`thesift:jaberora`): a jerboa of the dunes (modelled on the real one) - pink fur, tall ears, antennae, dot
  glossy black eyes, no nose at all, an enormous mouth that opens for its operatic arias, long jumping feet
  and a tufted tail.
* Reservoir (`thesift:reservoir`, `thesift:monarch_reservoir`): a worm-cactus of stacked ribbed segments
  striped pink and blue, studded with bone spines, Chrome glowing through its veins, a flower of a mouth on
  top. The Monarch is the same creature grown huge (the renderer scales it), in rose-gold and sapphire.
* Grub (`thesift:grub`): a Nib-like gem creature crusted in sand and dune rock; the shell parts (`shell_*`)
  break away when music cracks it, showing the gem body (variants amethyst / emerald / diamond / prism).

Hooks (one line each): spec.py -> declare(block, item); mobs.py -> ALL.update(MODELS);
gen_assets.generate() -> sounds(GA) and data(GA); items16.all_items() -> items(); gen_textures -> textures(out).
Java: registry/ModDunes, entity/dunes/*, client/DunesClient (+ models/renderers), effect/DeafenedEffect.
"""
from __future__ import annotations

import math
import os

import numpy as np

import aquakit as AK
from modelkit import Model

NS = 'thesift'
M = AK.Mat
up = AK.up


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# ================================================================ the dunes' colours (tools/wland_art.py)

ROSE = ('#5b2e3a', '#74404e', '#8c505d', '#a3626c', '#b8777c', '#c98c8c', '#d9a4a0')      # Dunestone
PLUM = ('#341626', '#4a2235', '#5d2c41', '#73394f', '#89485e', '#9d596c', '#b26e7d')      # Banded Dunestone strata
CHIME = ('#8e82a8', '#a597bd', '#b8acce', '#cac0dd', '#dad2e9', '#e8e2f2', '#f5f2fb')     # Chime Sand
BONE = ('#6d5c5a', '#8a766f', '#a8928a', '#c2ada2', '#d8c7bc', '#e8ddd2', '#f6efe8')      # sun-bleached spines
MOUTH = ('#24060f', '#3d0c1e', '#57142d', '#73203e', '#8f3050', '#aa4562', '#c25f76')
TONGUE = ('#6e2240', '#952f52', '#bb4766', '#da687d', '#f08f98', '#ffb3b0', '#ffd8d0')    # Tuning Fruit coral
EYE = {'P': '#0d0812', 'I': '#e8b03a', 'i': '#9a6418', 'K': '#0a0610', 'o': '#3a1a28', 'g': '#4a3448',
       'd': 0.62, 'D': 0.45, 'l': 1.22, 'L': 1.4, 'm': '#3d0c1e', 't': '#f2e8dc'}


def _strata(cell=1.7, every=3, wobble=1.4):
    """Horizontal strata worked out in world height, wavy like the Banded Dunestone (a band every `every` cells)."""
    def f(c):
        y = c['wp'][..., 1] + (AK.fbm(c['wp'] * 0.55, 3.0, 5) - 0.5) * wobble * 2
        return np.floor(y / cell) % every == 0
    return f


def _granules(rate=0.11, seed=3):
    """Raised granular scales: lone paler texels with a darker one below (a chameleon's tubercles)."""
    def tone(c):
        wp = np.floor(c['wp'] * 4 + 0.5).astype(np.int64)
        h = AK._hash3(wp[..., 0], wp[..., 1], wp[..., 2], seed)
        h2 = AK._hash3(wp[..., 0], wp[..., 1] - 1, wp[..., 2], seed)
        return np.where(h < rate, 0.13, 0.0) + np.where(h2 < rate, -0.09, 0.0)
    return tone


def _fur(strength=0.07, seed=9):
    """Fur: short strands - streaks two texels long, lighter and darker, running down the body."""
    def tone(c):
        wp = c['wp']
        ix = np.floor(wp[..., 0] * 4 + 0.5).astype(np.int64)
        iy = np.floor(wp[..., 1] * 2).astype(np.int64)
        iz = np.floor(wp[..., 2] * 4 + 0.5).astype(np.int64)
        h = AK._hash3(ix, iy, iz, seed)
        return np.where(h < 0.22, -strength, np.where(h > 0.8, strength, 0.0))
    return tone


def _both(*tones):
    def t(c):
        return sum(f(c) for f in tones)
    return t


def _rings(cx, cy, step=1.0):
    """Concentric rings round a point of a face (a chameleon's turret eye)."""
    def tone(c):
        loc = c['loc']
        a = loc[..., 2] if np.ptp(loc[..., 2]) > 0 else loc[..., 0]
        r = np.sqrt((a - cx) ** 2 + (loc[..., 1] - cy) ** 2)
        return np.where(np.floor(r / step) % 2 == 0, 0.08, -0.1)
    return tone


def _saw(height, tooth=2.0, rise=0.0):
    """A sawtooth crest plane (spines or a gular comb): teeth `tooth` long, `height` tall."""
    def keep(lx, ly, lz):
        along = lz - lz.min()
        t = (along % tooth) / tooth
        top = height * (1.0 - 0.75 * t) + rise * along
        return (-ly + ly.max()) < top if np.ptp(ly) > 0 else np.ones_like(lz, bool)
    return keep


def _petal(w, h):
    """A pointed oval petal on a plane (x across, y up the petal)."""
    def keep(lx, ly, lz):
        u = lx / (w / 2)
        v = (-ly) / h
        return (np.abs(u) < np.sin(np.clip(v, 0, 1) * math.pi) ** 0.7 * 1.02) & (v <= 1.0)
    return keep


def _round_plane(w, h):
    def keep(lx, ly, lz):
        a = (lx - (lx.min() + lx.max()) / 2) / max(w / 2, 1e-6) if np.ptp(lx) > 0 else (lz - (lz.min() + lz.max()) / 2) / max(w / 2, 1e-6)
        b = (ly - ly.max()) / max(h, 1e-6)
        return a * a + (b + 0.45) ** 2 * 3.2 < 1.0
    return keep


# ================================================================ the Kerkorer

K_CAMO_BAND = ('#6d5d80', '#806f92', '#9483a6', '#a897b8', '#bbabc9', '#cdbfd8', '#ddd2e6')
K_CAMO_BONE = ('#9f93b5', '#b1a6c4', '#c3b9d2', '#d3cbdf', '#e1dbea', '#eeeaf4', '#fbf9ff')
def _lateral(c):
    """A chameleon's pale lateral stripe, a broken line of tubercles along the flank."""
    wp = c['wp']
    wav = (AK.fbm(wp * 0.8, 2.0, 23) - 0.5) * 1.6
    return (np.abs(wp[..., 1] - 12.0 + wav) < 0.55) & (np.floor(wp[..., 2] * 1.3) % 4 != 0) & (np.abs(wp[..., 0]) > 3.5)


def _k_mats(main, band, bone, belly):
    skin = M([main], base=0.54, noise=0.07, cell=1.8, mottle=None, mottle2=None, belly=belly, scales=(0.85, 0.65), pores=0.03,
             gloss='#fff2ec', gloss_rate=0.012)
    skin.band = band
    skin.stripe = bone
    return {
        'skin': skin,
        'crest': M([bone], base=0.6, noise=0.06, cell=1.5, rim=0.7, ao=0.3, mottle=None, material='bone'),
        'belly': M([belly], base=0.6, noise=0.06, cell=1.7, mottle=None, scales=(0.9, 0.6), ao=0.6),
        'mouth': M([MOUTH], base=0.38, noise=0.04, rim=0.2, ao=0.4),
        'tongue': M([TONGUE], base=0.55, noise=0.04, rim=0.4, ao=0.1, gloss='#fff0ec', gloss_rate=0.1),
        'pad': M([TONGUE], base=0.72, noise=0.05, rim=0.3, ao=0.0, pores=0.12, gloss='#ffffff', gloss_rate=0.15),
        'pupil': M([('#05040a', '#0d0812', '#18101e', '#24182c', '#3a2840', '#5a4060', '#806080')], base=0.3, noise=0.0, rim=0.0, ao=0.0,
                   mottle=None),
        'claw': M([('#2a1a1e', '#3a262a', '#4e3438', '#664448', '#7e5a5a', '#a07c74', '#c4a698')], base=0.55, noise=0.03, rim=0.5,
                  ao=0.2, material='bone'),
    }


def kerkorer() -> Model:
    """The Kerkorer (KerkorerModel animates it; part names and pivots are its contract): a tall, flat-sided
    chameleon body with an arched back, striped with plum strata and a pale lateral line, a sawtooth bone crest,
    a triangular head under a tall casque, turret eyes with their own pupils, a gular comb, a long downturned
    jaw, the tongue with its sticky bait pad tucked on the lip, splayed legs with paired toes and a tail that
    coils under itself."""
    m = Model('kerkorer', (160, 128), {}, {'kerkorer': {}}, res=2)
    S = dict(mat='skin', belly=5.5, marks=[(_strata(), 'band', 0), (_lateral, 'stripe', 1)], tone=_granules(0.09))
    body = m.part('body', pivot=(0, 12, 1))
    body.cube((-4, -8, -10), (8, 14, 21), **S)
    body.cube((-3, -10.5, -7), (6, 2.5, 14), **S)
    body.cube((-2, -11.5, -4), (4, 1, 8), **S)
    body.cube((-3, 6, -8), (6, 1.5, 16), mat='belly')
    crest = body.part('crest', pivot=(0, -11, -8))
    crest.cube((0, -3, 0), (0, 3, 17), mat='crest', shape=_saw(3.0, 1.8))
    # ---- the head: skull, pointed snout, tall casque, jaw, gular comb, turret eyes and the tongue
    head = body.part('head', pivot=(0, -4, -10))
    head.cube((-3.25, -3.5, -8), (6.5, 7, 8), **S, faces={'down': dict(mat='mouth')})
    head.cube((-2.25, -1.5, -10.5), (4.5, 4, 2.5), **S, faces={'down': dict(mat='mouth'),
                                                                'north': dict(decal=up(['....', '....', '....', 'm..m']), keys=EYE, at=(0, 0))})
    head.cube((-1.5, -2.5, -10), (3, 1, 2), **S)
    casque = head.part('casque', pivot=(0, -3.5, -6))
    C = dict(mat='skin', marks=[(_strata(1.0, 2, 0.5), 'band', 0)], tone=_granules(0.09))
    casque.cube((-2, -2.5, -1), (4, 2.5, 9), **C)
    casque.cube((-1.75, -4.5, 1.5), (3.5, 2, 6.5), **C)
    casque.cube((-1.5, -6.5, 4), (3, 2, 4), **C)
    casque.cube((-0.75, -7.5, 5.5), (1.5, 1, 2.5), mat='crest')
    jaw = head.part('jaw', pivot=(0, 3.5, -1))
    jaw.cube((-3.25, 0, -10.5), (6.5, 2.5, 10.5), mat='skin', belly=1.2, tone=_granules(0.09),
             faces={'up': dict(mat='mouth'), 'down': dict(mat='belly')})
    comb = jaw.part('comb', pivot=(0, 2.5, -9.5))
    comb.cube((0, 0, 0), (0, 2.5, 8), mat='crest', shape=lambda lx, ly, lz: (ly - ly.min()) < 2.5 * (1 - 0.7 * ((lz - lz.min()) % 2.0) / 2.0))
    for side, sx in (('left', 1), ('right', -1)):
        eye = head.part(f'{side}_eye', pivot=(3.4 * sx, -1.6, -5.2))
        ox = -0.5 if sx > 0 else -2.5
        face = 'east' if sx > 0 else 'west'
        eye.cube((ox, -2.5, -2.5), (2.5, 5, 5), mat='skin', marks=[(_strata(0.8, 2, 0.3), 'band', 0)],
                 faces={face: dict(tone=_rings(2.5, 0.0, 0.8))})
        eye.cube(((2.0 if sx > 0 else -3.25), -1.75, -1.75), (1.25, 3.5, 3.5), mat='skin', marks=[(_strata(0.6, 2, 0.2), 'band', 0)],
                 faces={face: dict(tone=_rings(1.75, 0.0, 0.6))})
        pupil = eye.part(f'{side}_pupil', pivot=(3.25 * sx, 0, 0))
        pupil.cube(((0 if sx > 0 else -0.75), -1, -1), (0.75, 2, 2), mat='pupil',
                   faces={face: dict(decal=['.iI.', 'iPPI', 'IPPI', '.iI.'], keys=EYE, at=(0, 0))})
    # the tongue: a stalk (stretched along z when it lashes out) and the sticky bait pad on its tip (moved out with it)
    tongue = head.part('tongue', pivot=(0, 2.4, -8.5))
    stalk = tongue.part('tongue_stalk', pivot=(0, 0, 0.5))
    stalk.cube((-1, -0.6, -2.5), (2, 1.2, 2.5), mat='tongue')
    tip = tongue.part('tongue_tip', pivot=(0, 0, -2))
    tip.cube((-1.25, -0.8, -1.75), (2.5, 1.6, 1.75), mat='pad')
    # ---- legs: thin, splayed, bent at the elbow, two bundles of toes on each foot
    for end, z in (('front', -6), ('back', 7)):
        for side, sx in (('left', 1), ('right', -1)):
            leg = body.part(f'{end}_{side}_leg', pivot=(3.6 * sx, 2.5, z), rot=(0, 0, -0.5 * sx))
            leg.cube((-1.25, -1, -1.25), (2.5, 6, 2.5), **S)
            shin = leg.part(f'{end}_{side}_shin', pivot=(0, 4.6, 0), rot=(0, 0, 0.5 * sx))
            shin.cube((-1, 0, -1), (2, 4.6, 2), mat='skin', marks=[(_strata(), 'band', 0)], tone=_granules(0.09))
            foot = shin.part(f'{end}_{side}_foot', pivot=(0, 4.6, 0))
            foot.cube((-1.5, 0, -1.25), (3, 1.0, 2.5), mat='skin', belly=0.8)
            foot.cube((-1.1, 0.1, -3), (2.2, 0.9, 1.75), mat='skin', belly=0.5)
            foot.cube((-1.1, 0.1, 1.25), (2.2, 0.9, 1.75), mat='skin', belly=0.5)
            foot.cube((-0.9, 0.5, -3.5), (1.8, 0.5, 0.5), mat='claw')
            foot.cube((-0.9, 0.5, 3.0), (1.8, 0.5, 0.5), mat='claw')
    # ---- the tail, coiled under itself
    t = body.part('tail', pivot=(0, -5, 10.5), rot=(0.12, 0, 0))
    t.cube((-2.25, -2.25, -0.5), (4.5, 4.5, 6.5), **S)
    t.cube((-0.25, -3.25, 0), (0.5, 1, 5.5), mat='crest')
    sizes = [(3.4, 5.0, -0.6), (2.6, 4.5, -0.9), (1.9, 4.0, -1.1), (1.3, 3.5, -1.25)]
    parent, length = t, 6.5
    for i, (w, ln, rot) in enumerate(sizes):
        seg = parent.part(f'tail_{i + 2}', pivot=(0, 0, length - 0.5), rot=(rot, 0, 0))
        seg.cube((-w / 2, -w / 2, -0.25), (w, w, ln), **S)
        parent, length = seg, ln
    mats = _k_mats(ROSE, PLUM, BONE, CHIME)
    camo = _k_mats(CHIME, K_CAMO_BAND, K_CAMO_BONE, CHIME)
    return AK.finish(m, {'kerkorer': mats, 'kerkorer_camo': camo})


# ================================================================ the Jaberora

J_FUR = ('#7a3a52', '#96506a', '#b26a84', '#c9849c', '#dc9eb2', '#ebbaca', '#f6d6e0')   # soft pink fur
J_BACK = ('#5e2a40', '#783650', '#924662', '#aa5a76', '#c0708a', '#d2889e', '#e2a2b4')  # deeper rose along its back
J_PALE = ('#a88a9e', '#bea2b4', '#d2bcc8', '#e4d4dc', '#f0e6ec', '#f8f2f5', '#fffcfd')  # its pale pink-white belly and legs
J_EAR = ('#7a2244', '#9a3058', '#ba4470', '#d45e88', '#e880a2', '#f4a6be', '#fcd0de')
J_EYE = ('#030306', '#07070c', '#0d0c16', '#161424', '#221e36', '#36304e', '#4c4668')
J_TUFT = ('#4a1630', '#5e1e3e', '#78284e', '#923662', '#ac4a78', '#c46690', '#da88aa')  # the tuft: deep shimmering magenta
J_SHEEN_A = ('#6a2a48', '#8a3a5e', '#ac5076', '#cc6c90', '#e48eaa', '#f4b4c6', '#fde0e8')  # the tail's sheen: pink ...
J_SHEEN_B = ('#4a3462', '#60447e', '#7a5a9c', '#9676b8', '#b496d0', '#d2bae4', '#eeddf4')  # ... and lilac, in turn
J_GLINT = ('#ff9ed0', '#d8b0ff', '#a8e8f4', '#fff0f8')


def _sheen(k):
    """Iridescent rings along the tail: rose-gold, plain, blue-lilac, plain (cube-local length)."""
    def f(c):
        return np.floor(c['loc'][..., 2] / 1.25) % 4 == k
    return f


def _tail_glints(c):
    """Shine along the top of the tail: a broken bright streak (glowing)."""
    loc = c['loc']
    if c['face'] not in ('up', 'east', 'west'):
        return None
    top = loc[..., 1] < loc[..., 1].min() + 0.3 if c['face'] != 'up' else np.ones_like(loc[..., 1], bool)
    return top & (np.floor(loc[..., 2] * 2) % 3 != 0)


def jaberora() -> Model:
    """The Jaberora (JaberoraModel animates it), modelled on the real jerboa: a tiny round body sitting up on very
    long hind legs with long hopping feet, tiny forearms, large rounded ears, big dark eyes set on the sides (drawn
    the vanilla way), no nose, a wide mouth for its arias, and a very long thin shining tail - iridescent rings and a
    glowing sheen - ending in a dark tuft with a white tip. The renderer draws it at 0.7 scale."""
    m = Model('jaberora', (80, 64), {}, {'jaberora': {}}, res=2)
    F = dict(mat='fur', back=-3.0, belly=0.0, tone=_fur())
    body = m.part('body', pivot=(0, 17.5, 1.5), rot=(-0.4, 0, 0))
    body.cube((-2.5, -4.5, -2.5), (5, 5, 5.5), **F, faces={'north': dict(mat='pale')})
    body.cube((-2, -5, -2), (4, 0.5, 4.5), **F)
    body.cube((-3, -3.25, -2), (6, 3, 4.5), **F, faces={'north': dict(mat='pale'), 'down': dict(mat='pale')})
    head = body.part('head', pivot=(0, -4.5, -0.5), rot=(0.4, 0, 0))
    head.cube((-2.5, -4.5, -3), (5, 4.5, 5), mat='fur', back=-3.0, belly=-0.4, tone=_fur(), faces={'down': dict(mat='mouth')})
    head.cube((-2, -5, -2.5), (4, 0.5, 4), mat='fur', back=99, tone=_fur())
    head.cube((-1.75, -2.2, -3.9), (3.5, 2.2, 1), mat='fur', back=-99, belly=-0.6, tone=_fur(0.05, 5),
              faces={'north': dict(decal=['.......', '.......', 'mmmmmmm', '.m...m.'], keys=EYE, at=(0, 0))})
    for side, sx in (('left', 1), ('right', -1)):
        # small dark dot eyes, vanilla style: a single dark pixel-dot each, no glint
        eye = head.part(f'{side}_eye', pivot=(1.35 * sx, -2.9, -3.02), rot=(0, 0.3 * sx, 0))
        eye.cube((-0.5, -0.5, -0.1), (1, 1, 0.2), mat='eye', faces={'north': dict(decal=['KK', 'KK'], keys=EYE, at=(0, 0))})
        lid = eye.part(f'{side}_eyelid', pivot=(0, 0, -0.12))
        lid.cube((-0.6, -0.6, -0.05), (1.2, 1.2, 0.05), mat='fur', tone=_fur())
        # a pair of thin antennae with shimmering bobbles, springing from the crown
        ant = head.part(f'{side}_antenna', pivot=(0.55 * sx, -4.9, -2.2), rot=(0.3, 0, 0.22 * sx))
        ant.cube((-0.2, -2.8, -0.2), (0.4, 2.8, 0.4), mat='antenna')
        atip = ant.part(f'{side}_antenna_tip', pivot=(0, -2.8, 0), rot=(0.35, 0, 0.12 * sx))
        atip.cube((-0.18, -1.9, -0.18), (0.36, 1.9, 0.36), mat='antenna')
        atip.cube((-0.55, -3.0, -0.55), (1.1, 1.1, 1.1), mat='bobble', lights=lambda c: AK.fbm(c['wp'] * 4.0, 0.7, 23, 1) > 0.55)
        # large rounded ears, pink inside
        ear = head.part(f'{side}_ear', pivot=(1.6 * sx, -4.4, 1.0), rot=(-0.3, -0.3 * sx, 0.42 * sx))
        ear.cube((-1.5, -4, 0), (3, 4, 0), mat='ear', shape=_round_plane(3, 4), faces={'south': dict(mat='fur', tone=_fur())})
        # tiny forearms held to the chest
        arm = body.part(f'{side}_arm', pivot=(1.2 * sx, -1.2, -2.4), rot=(-0.6, 0, -0.1 * sx))
        arm.cube((-0.35, 0, -0.35), (0.7, 1.8, 0.7), mat='pale')
        arm.cube((-0.4, 1.6, -0.5), (0.8, 0.5, 0.8), mat='pale')
        # very long hind legs: a round haunch, a thin shin, a long hopping foot
        thigh = m.part(f'{side}_thigh', pivot=(2.1 * sx, 19.5, 2.8))
        thigh.cube((-1.1, -2, -1.8), (2.2, 4, 3.6), **F)
        shin = thigh.part(f'{side}_shin', pivot=(0, 1.8, 1.2), rot=(0.6, 0, 0))
        shin.cube((-0.45, 0, -0.45), (0.9, 3.4, 0.9), mat='pale')
        foot = shin.part(f'{side}_foot', pivot=(0, 3.4, 0), rot=(-0.6, 0, 0))
        foot.cube((-0.7, -0.6, -5), (1.4, 0.6, 5.5), mat='pale')
        foot.cube((-0.6, -0.5, -5.6), (1.2, 0.5, 0.6), mat='fur', back=-99, belly=99)
    jaw = head.part('jaw', pivot=(0, -0.4, 0.6))
    jaw.cube((-2, 0, -4.4), (4, 1, 4.8), mat='fur', back=-99, belly=0.5, tone=_fur(0.06, 7), faces={'up': dict(mat='mouth')})
    jaw.cube((-1.4, -0.4, -3.8), (2.8, 0.4, 3.4), mat='tongue')
    # the very long shining tail, out straight behind for balance, ending in a dark tuft tipped white
    T = dict(mat='tail', marks=[(_sheen(0), 'sheen_a', 0), (_sheen(2), 'sheen_b', 0)], lights=_tail_glints)
    tail = m.part('tail', pivot=(0, 19.0, 5.0), rot=(0.05, 0, 0))
    tail.cube((-0.4, -0.4, 0), (0.8, 0.8, 7), **T)
    mid = tail.part('tail_mid', pivot=(0, 0, 7), rot=(0.08, 0, 0))
    mid.cube((-0.35, -0.35, 0), (0.7, 0.7, 6.5), **T)
    tip = mid.part('tail_tip', pivot=(0, 0, 6.5), rot=(0.15, 0, 0))
    tip.cube((-1, -1, 0), (2, 2, 2.8), mat='tuft', tone=_fur(0.1, 13), lights=lambda c: AK.fbm(c['wp'] * 3.0, 0.8, 19, 1) > 0.72)
    tip.cube((-0.8, -0.8, 2.8), (1.6, 1.6, 1.4), mat='pale', tone=_fur(0.1, 14))
    fur = M([J_FUR], base=0.55, noise=0.07, cell=1.6, mottle=None, mottle2=None, back=J_BACK, belly=J_PALE, material='fur')
    tl = M([J_FUR], base=0.6, noise=0.04, cell=1.2, mottle=None, rim=0.6, ao=0.2, light=J_GLINT, gloss='#ffffff', gloss_rate=0.12,
           material='scales')
    tl.sheen_a = J_SHEEN_A
    tl.sheen_b = J_SHEEN_B
    mats = {
        'fur': fur,
        'pale': M([J_PALE], base=0.58, noise=0.06, cell=1.6, mottle=None, material='fur', ao=0.6),
        'ear': M([J_EAR], base=0.6, noise=0.04, cell=1.5, rim=0.4, ao=0.0, mottle=None, flat=True, material='membrane'),
        'eye': M([J_EYE], base=0.42, noise=0.0, rim=0.3, ao=0.0, mottle=None),
        'mouth': M([MOUTH], base=0.35, noise=0.03, rim=0.2, ao=0.3),
        'tongue': M([TONGUE], base=0.55, noise=0.04, rim=0.3, ao=0.0),
        'tail': tl,
        'tuft': M([J_TUFT], base=0.5, noise=0.08, cell=1.0, mottle=None, material='fur', light=J_GLINT, gloss='#ffe8f4', gloss_rate=0.1),
        'antenna': M([J_BACK], base=0.5, noise=0.03, cell=1.0, mottle=None, rim=0.5, ao=0.0),
        'bobble': M([J_SHEEN_A], base=0.62, noise=0.03, cell=0.8, mottle=None, rim=0.6, ao=0.0, light=J_GLINT, gloss='#ffffff',
                    gloss_rate=0.15, material='crystal'),
    }
    return AK.finish(m, {'jaberora': mats})


# ================================================================ the Reservoir

R_PINK = ('#5e1a3e', '#842853', '#a83a6c', '#c95686', '#e07aa2', '#f0a2be', '#fbcadb')
R_BLUE = ('#18245e', '#22357e', '#2e4ca0', '#4268bc', '#5f8ad4', '#8cb2e6', '#c0daf6')
R_RIB = ('#4c1634', '#6a2148', '#8a2f5e', '#aa4476', '#c4628e', '#da86a8', '#ecb0c6')
R_PETAL = ('#7a2450', '#a03668', '#c44e82', '#de6e9c', '#f094b6', '#fbbcd2', '#fff0f6')
R_LIGHT = ('#7a5cd0', '#5fd8e8', '#bff6ff', '#fff4ff')
M_GOLD = ('#5e2a1a', '#843e22', '#ac5a2e', '#cc7c42', '#e4a25e', '#f2c584', '#fde6b8')
M_SAPH = ('#0c1446', '#141f64', '#1e2f84', '#2c45a4', '#4062c0', '#6488d8', '#9cb6ee')
M_RIB = ('#4a1e18', '#6a2c20', '#8c402a', '#ae5838', '#c8784e', '#dc9c6c', '#ecc496')
M_PETAL = ('#7a3428', '#a44c34', '#c86a44', '#e48e5c', '#f4b47c', '#fcd8a8', '#fff4e0')
R_SEGS = [(11, 7.0), (10, 7.0), (9, 6.5), (8, 6.0)]  # (width, height) of each segment, base to top


def _bands(c):
    """Pink and blue stripes round the body, level all the way round (world height, so the ribs match)."""
    return np.floor(c['wp'][..., 1] / 1.75) % 2 == 1


def _veins(c):
    """Chrome glinting through the skin: little beads of light along the blue stripes."""
    if c['face'] in ('up', 'down'):
        return None
    wp = np.floor(c['wp'] * 4 + 0.5).astype(np.int64)
    h = AK._hash3(wp[..., 0], wp[..., 1], wp[..., 2], 41)
    mid = np.abs((c['wp'][..., 1] / 1.75) % 1.0 - 0.5) < 0.2
    return (h < 0.09) & mid & (np.floor(c['wp'][..., 1] / 1.75) % 2 == 1)


def _areoles(c):
    """The cactus's woolly areoles: little pale tufts in rows where the spines grow."""
    loc = c['loc']
    a = loc[..., 0] if c['face'] in ('north', 'south') else loc[..., 2]
    return (np.abs((a + 0.25) % 2.5 - 1.25) < 0.3) & (np.abs((loc[..., 1] + 0.8) % 3.2 - 1.6) < 0.3)


def reservoir() -> Model:
    """The Reservoir (ReservoirModel animates it): four ribbed segments stacked into a living column, each a
    little narrower, striped pink and blue, spined along its ribs, Chrome glowing in its veins; on top a
    collar, a pool of Chrome and six petals that open into a flower of a mouth; root-feet at the base."""
    m = Model('reservoir', (128, 128), {}, {'reservoir': {}}, res=2)
    base = m.part('base', pivot=(0, 24, 0))
    for i in range(6):
        a = i * math.pi / 3 + 0.3
        rt = base.part(f'root_{i}', pivot=(5.2 * math.sin(a), -1, -5.2 * math.cos(a)), rot=(0, -a, 0))
        rt.cube((-1, -1.2, -2.5), (2, 1.6, 2.5), mat='rib')
    parent, top = base, 0.0
    for i, (w, h) in enumerate(R_SEGS):
        seg = parent.part(f'segment_{i}', pivot=(0, top, 0))
        hw = w / 2
        SK = dict(mat='skin', marks=[(_bands, 'band', 0), (_areoles, 'wool', 1)], lights=_veins, faces={'up': dict(mat='rib'), 'down': dict(mat='rib')})
        # a barrel: a narrower core the full height and a wide belt round its middle, so each segment bulges like a worm's
        seg.cube((-hw + 1, -h, -hw + 1), (w - 2, h, w - 2), **SK)
        seg.cube((-hw, -h + 1.25, -hw), (w, h - 2.5, w), **SK)
        # four ribs, one down the middle of each side, and a spine or two sticking out of each
        for k, (fx, fz) in enumerate(((0, -1), (1, 0), (0, 1), (-1, 0))):
            rw = 2.0
            if fz:
                seg.cube((-rw / 2, -h + 0.25, fz * hw - (1 if fz > 0 else 0) + (0.5 if fz > 0 else -0.5)), (rw, h - 0.5, 1), mat='rib',
                         marks=[(_bands, 'band', 0)])
            else:
                seg.cube((fx * hw - (1 if fx > 0 else 0) + (0.5 if fx > 0 else -0.5), -h + 0.25, -rw / 2), (1, h - 0.5, rw), mat='rib',
                         marks=[(_bands, 'band', 0)])
            for j, yy in enumerate((-h * 0.72, -h * 0.3)):
                if (i + j + k) % 2:
                    continue
                d = hw + 1.0
                if fz:
                    seg.cube((-0.25 + (0.6 if j else -0.6), yy, fz * d - (1.6 if fz < 0 else 0)), (0.5, 0.5, 1.6), mat='spine')
                else:
                    seg.cube((fx * d - (1.6 if fx < 0 else 0), yy, -0.25 + (0.6 if j else -0.6)), (1.6, 0.5, 0.5), mat='spine')
        parent, top = seg, -h
    mouth = parent.part('mouth', pivot=(0, top, 0))
    mouth.cube((-3.5, -2, -3.5), (7, 2, 7), mat='rib', faces={'up': dict(mat='mouth')})
    mouth.cube((-2.5, -1.6, -2.5), (5, 0, 5), mat='chrome', glow=True)
    for i in range(6):
        a = i * math.pi / 3
        r = 3.0
        pet = mouth.part(f'petal_{i}', pivot=(r * math.sin(a), -1.8, -r * math.cos(a)), rot=(0.55, -a, 0))
        pet.cube((-3, -6.5, 0), (6, 6.5, 0), mat='petal', shape=_petal(6, 6.5), marks=[(lambda c: c['loc'][..., 1] < -4.6, 'tip', 0)])
    mats = {
        'skin': M([R_PINK], base=0.55, noise=0.04, cell=1.6, mottle=None, light=R_LIGHT, gloss='#fff4fa', gloss_rate=0.02, pores=0.02),
        'rib': M([R_RIB], base=0.55, noise=0.06, cell=1.5, mottle=None, rim=0.8, ao=0.5),
        'spine': M([BONE], base=0.66, noise=0.03, rim=0.5, ao=0.0, material='bone'),
        'mouth': M([MOUTH], base=0.32, noise=0.03, rim=0.1, ao=0.4),
        'chrome': M([('#4a3a8e', '#4f7ec8', '#5fd8e8', '#9ff0f4', '#e0c8ff', '#ffd8f0', '#ffffff')], base=0.6, noise=0.12, cell=0.9,
                    mottle=None, rim=0.0, ao=0.0, flat=True),
        'petal': M([R_PETAL], base=0.58, noise=0.04, cell=1.4, rim=0.0, ao=0.0, mottle=None, flat=True, material='membrane', light=R_LIGHT),
    }
    mats['skin'].band = R_BLUE
    mats['skin'].wool = BONE
    mats['rib'].band = R_BLUE
    mats['petal'].tip = R_BLUE
    mats['petal'].vein = R_RIB
    mon = {k: v for k, v in mats.items()}
    mon['skin'] = M([M_GOLD], base=0.54, noise=0.06, cell=1.6, mottle=None, light=R_LIGHT, gloss='#fff8ec', gloss_rate=0.03, pores=0.03)
    mon['skin'].band = M_SAPH
    mon['skin'].wool = BONE
    mon['rib'] = M([M_RIB], base=0.55, noise=0.06, cell=1.5, mottle=None, rim=0.8, ao=0.5)
    mon['rib'].band = M_SAPH
    mon['petal'] = M([M_PETAL], base=0.58, noise=0.04, cell=1.4, rim=0.0, ao=0.0, mottle=None, flat=True, material='membrane', light=R_LIGHT)
    mon['petal'].tip = M_SAPH
    mon['petal'].vein = M_RIB
    return AK.finish(m, {'reservoir': mats, 'reservoir_monarch': mon})


# ================================================================ the Grub

GEMS = {  # variant: (gem ramp, glow ramp)
    'amethyst': (('#2a1450', '#3c1e6c', '#542c8c', '#7044ac', '#9468cc', '#bc96e6', '#e6d0fa'), ('#8a5cd8', '#b48cf0', '#dcc4ff', '#f6eeff')),
    'emerald': (('#08341e', '#0e4a2a', '#176638', '#21864a', '#36a860', '#62cc84', '#a8eebc'), ('#2ea860', '#5ee08a', '#b0ffc8', '#f0fff4')),
    'diamond': (('#0c3a4a', '#125262', '#1a6e7e', '#2a909c', '#4cb6be', '#86dade', '#ccf6f6'), ('#3ec4d0', '#7aeef0', '#c8fffc', '#ffffff')),
    'prism': (('#4a1238', '#6a1a4e', '#902a66', '#b8447e', '#da6c98', '#f09cbc', '#ffd0e2'), ('#e05aa0', '#ff8cc0', '#ffd0ec', '#fff4fa')),
}
GRUB_LEG = ('#20101a', '#2e1824', '#3e2230', '#522e3e', '#683e4e', '#805262', '#9a6a78')
GRUB_WING = ('#5a6e9e', '#7088b6', '#8ca4cc', '#aac0de', '#c8daee', '#e2eef8', '#f6fbff')


def _facets(c):
    """Cut-gem facets: flat panels of light and shade worked out in 3D."""
    f = AK.fbm(c['wp'] * 1.0 + 3.0, 1.6, 17, 1)
    return (np.floor(f * 6) / 6 - 0.45) * 0.55


def _gem_veins(c):
    v = AK.fbm(c['wp'] * 1.3, 2.4, 61, 2)
    return np.abs(v - 0.5) < 0.035


def _crust(c):
    """Sand caked over the dune rock: lilac-white patches."""
    return AK.fbm(c['wp'] * 1.1 + 7.0, 1.4, 83, 2) > 0.63


def grub() -> Model:
    """The Grub (GrubModel animates it): a Nib-like gem creature on six stubby legs - a faceted body, a head with
    four tiny gem eyes, hooked mandibles and glowing antennae, crystal wings folded along its back - all crusted
    in a shell of dune rock and caked sand (the shell_* parts) that breaks away when music cracks it."""
    m = Model('grub', (64, 64), {}, {'grub': {}}, res=2)
    G = dict(mat='gem', tone=_facets, lights=_gem_veins)
    body = m.part('body', pivot=(0, 20.5, 0))
    body.cube((-2.5, -2.5, -1), (5, 4, 6), **G)
    body.cube((-2, -2.2, -3.5), (4, 3.5, 2.5), **G)
    body.cube((-1.5, -2, 5), (3, 3, 1.5), **G)
    head = body.part('head', pivot=(0, -0.5, -3.5))
    head.cube((-2, -1.75, -3), (4, 3.25, 3), **G, faces={'north': dict(decal=['........', '..e..e..', '.e....e.', '........', '........', '........'],
                                                                       keys={'e': '#05060a'}, at=(0, 0))})
    for side, sx in (('left', 1), ('right', -1)):
        mand = head.part(f'{side}_mandible', pivot=(1.4 * sx, 1.0, -2.8), rot=(0, -0.35 * sx, 0))
        mand.cube((-0.4, -0.4, -1.6), (0.8, 0.8, 1.6), mat='claw')
        mand.cube(((-0.9 if sx > 0 else 0.1), -0.4, -2.2), (0.8, 0.8, 0.8), mat='claw')
        ant = head.part(f'{side}_antenna', pivot=(1.0 * sx, -1.75, -2.2), rot=(-0.55, 0, 0.35 * sx))
        ant.cube((0, -3.5, 0), (0, 3.5, 1.2), mat='wing', lights=lambda c: c['loc'][..., 1] < -2.6, no_occlude=True)
        for k, z in enumerate((-2.5, -0.5, 1.8)):
            leg = body.part(f'{side}_leg_{k}', pivot=(2.2 * sx, 0.8, z), rot=(0.25 * (k - 1), 0, -0.6 * sx))
            leg.cube((-0.45, 0, -0.45), (0.9, 2.9, 0.9), mat='leg')
        wing = body.part(f'{side}_wing', pivot=(0.8 * sx, -2.5, -1.6), rot=(0, -0.22 * sx, 0))
        wing.cube(((0 if sx > 0 else -3.5), 0, 0), (3.5, 0, 6.5), mat='wing', shape=lambda lx, ly, lz: (
            ((np.abs(lx) - 1.75) / 1.75) ** 2 + ((lz - 3.25) / 3.25) ** 2 < 1.05), lights=lambda c: (np.floor(c['loc'][..., 2] * 2) % 3 == 0)
            & (np.abs(c['loc'][..., 0]) < 2.8))
    # the shell: dune rock caked with sand
    R = dict(mat='rock', marks=[(_strata(1.1, 3, 0.8), 'band', 0), (_crust, 'sand', 1)])
    top = body.part('shell_top', pivot=(0, -2.4, 0))
    top.cube((-3.25, -2.2, -4), (6.5, 2.4, 9.5), **R)
    top.cube((-2.25, -3.2, -2.5), (4.5, 1, 6), **R)
    for side, sx in (('left', 1), ('right', -1)):
        sh = body.part(f'shell_{side}', pivot=(2.6 * sx, -1.5, 0.5), rot=(0, 0, 0.18 * sx))
        sh.cube(((-0.2 if sx > 0 else -1.2), -0.6, -3.8), (1.4, 3.2, 7.5), **R)
    cap = head.part('shell_head', pivot=(0, -1.6, -1.2))
    cap.cube((-2.6, -1.4, -2.1), (5.2, 1.7, 3.6), **R)
    rock = body.part('shell_rock', pivot=(0.6, -5.4, 0.5), rot=(0.1, 0.4, 0.15))
    rock.cube((-1.5, -1.4, -1.6), (3, 1.6, 3.2), **R)
    rock.cube((-0.8, -2.2, -0.8), (1.6, 0.9, 1.6), **R)
    variants = {}
    for name, (gem, glow) in GEMS.items():
        rk = M([ROSE, PLUM], base=0.52, noise=0.1, cell=1.2, mottle=(1.6, 0.66), mottle2=None, pores=0.08, material='stone', mnoise=0.9)
        rk.band = PLUM
        rk.sand = CHIME[:5] + ('#cfc5e0', '#ddd5ea')
        variants[f'grub_{name}'] = {
            'gem': M([gem], base=0.56, noise=0.03, cell=1.4, mottle=None, light=glow, gloss='#ffffff', gloss_rate=0.1, material='crystal'),
            'rock': rk,
            'leg': M([GRUB_LEG], base=0.5, noise=0.04, rim=0.5, ao=0.2),
            'claw': M([gem], base=0.62, noise=0.02, rim=0.6, ao=0.0, material='crystal'),
            'wing': M([GRUB_WING], base=0.62, noise=0.03, cell=1.2, rim=0.0, ao=0.0, mottle=None, flat=True, material='membrane', light=glow,
                      opacity=200),
        }
    return AK.finish(m, variants)


MODELS = {'kerkorer': kerkorer, 'jaberora': jaberora, 'reservoir': reservoir, 'grub': grub}


# ================================================================ spec (items)

def declare(block, item):
    for mob, reg in (('kerkorer', 'KERKORER'), ('jaberora', 'JABERORA'), ('reservoir', 'RESERVOIR'), ('monarch_reservoir', 'MONARCH_RESERVOIR'),
                     ('grub', 'GRUB')):
        item(f'{mob}_spawn_egg', cls='SpawnEggItem', props=f'new Item.Properties().spawnEgg(ModDunes.{reg}.get())', tab='eggs')
    # the Kerkorer's camouflage scale, and the cloak sewn from them (undetectable while you stand still)
    item('kerkorer_scale', props='new Item.Properties().rarity(Rarity.UNCOMMON)', name='Camouflage Scale')
    item('kerkorer_cloak', cls='Item', props='new Item.Properties().humanoidArmor(ModDunes.CLOAK_MATERIAL, ArmorType.CHESTPLATE).rarity(Rarity.RARE)',
         name='Kerkorer Cloak')


# ================================================================ sounds (vanilla events)

SOUNDS = {
    # the Kerkorer's name is its call: "ker" - "ko" - "rer", a croak, a knock and a rolling purr (Kerkorer.java plays them in turn)
    'entity.kerkorer.ker': [('event:entity.frog.ambient', 1.0, 0.55), ('event:entity.camel.ambient', 0.35, 0.75)],
    'entity.kerkorer.ko': [('event:entity.frog.ambient', 1.0, 0.85), ('event:entity.turtle.ambient_land', 0.7, 0.7)],
    'entity.kerkorer.rer': [('event:entity.cat.purr', 1.0, 0.55), ('event:entity.ravager.ambient', 0.22, 1.5)],
    'entity.kerkorer.snap': [('event:entity.evoker_fangs.attack', 1.0, 0.75), ('event:entity.player.attack.strong', 0.7, 0.6)],
    'entity.kerkorer.tongue': [('event:entity.frog.tongue', 1.0, 0.6)],
    'entity.kerkorer.lure': [('event:block.amethyst_block.chime', 0.5, 1.3), ('event:entity.item.pickup', 0.25, 0.7)],
    'entity.kerkorer.shift': [('event:item.brush.brushing.sand', 0.5, 0.8), ('event:block.sand.break', 0.3, 0.7)],
    'entity.kerkorer.hiss': [('event:entity.cat.hiss', 1.0, 0.45)],
    'entity.kerkorer.hurt': [('event:entity.frog.hurt', 1.0, 0.6), ('event:entity.armadillo.hurt', 0.6, 0.8)],
    'entity.kerkorer.death': [('event:entity.frog.death', 1.0, 0.5), ('event:entity.camel.death', 0.5, 0.9)],
    'entity.kerkorer.step': [('event:entity.camel.step_sand', 0.35, 0.9)],
    # the Jaberora: chirps, its aria (one sung note; Jaberora.java sings a phrase of them), the stunning pulse, hops
    'entity.jaberora.chirp': [('event:entity.rabbit.ambient', 0.8, 1.6), ('event:entity.allay.ambient_without_item', 0.25, 2.0)],
    'entity.jaberora.sing': [('event:entity.allay.ambient_without_item', 1.0, 1.0), ('event:entity.allay.ambient_with_item', 1.0, 1.0)],
    'entity.jaberora.pulse': [('event:block.amethyst_block.resonate', 1.0, 2.0), ('event:entity.allay.item_thrown', 1.0, 2.0),
                              ('event:block.bell.resonate', 0.6, 2.0)],
    'entity.jaberora.hop': [('event:entity.rabbit.jump', 0.6, 1.1)],
    'entity.jaberora.eat': [('event:entity.generic.eat', 0.7, 1.4)],
    'entity.jaberora.snore': [('event:entity.fox.sleep', 0.8, 1.5)],
    'entity.jaberora.hurt': [('event:entity.rabbit.hurt', 1.0, 1.2)],
    'entity.jaberora.death': [('event:entity.rabbit.death', 1.0, 1.1), ('event:entity.allay.death', 0.5, 1.4)],
    # the Reservoir: sloshing, drinking, being drained, the burst, its blooming mouth, the Monarch's deep hum
    'entity.reservoir.ambient': [('event:block.bubble_column.bubble_pop', 0.6, 0.6), ('event:entity.slime.squish_small', 0.4, 0.7)],
    'entity.reservoir.gulp': [('event:item.bucket.fill', 0.8, 0.8), ('event:entity.generic.drink', 0.6, 0.6)],
    'entity.reservoir.drain': [('event:item.bucket.empty', 0.8, 0.8)],
    'entity.reservoir.burst': [('event:entity.generic.splash', 1.0, 0.8), ('event:entity.slime.squish', 1.0, 0.5),
                               ('event:entity.puffer_fish.blow_out', 1.0, 0.6)],
    'entity.reservoir.swell': [('event:entity.puffer_fish.blow_up', 0.8, 0.7)],
    'entity.reservoir.bloom': [('event:block.big_dripleaf.tilt_up', 0.7, 1.2)],
    'entity.reservoir.hum': [('event:block.beacon.ambient', 0.6, 0.5), ('event:block.conduit.ambient', 0.6, 0.7)],
    'entity.reservoir.hurt': [('event:entity.slime.hurt', 0.8, 0.8), ('event:entity.puffer_fish.sting', 0.6, 0.9)],
    'entity.reservoir.death': [('event:entity.slime.death', 1.0, 0.6), ('event:entity.generic.splash', 0.8, 0.9)],
    # the Grub: skittering in its rock, the shell creaking with music and cracking, the gem creature inside
    'entity.grub.ambient': [('event:entity.silverfish.ambient', 0.5, 0.7), ('event:block.gravel.step', 0.4, 0.9)],
    'entity.grub.step': [('event:entity.silverfish.step', 0.3, 0.8)],
    'entity.grub.creak': [('event:block.stone.hit', 0.6, 1.6), ('event:block.amethyst_block.hit', 0.5, 1.6)],
    'entity.grub.crack': [('event:block.stone.break', 1.0, 1.2), ('event:block.suspicious_sand.break', 0.8, 0.9),
                          ('event:block.amethyst_block.chime', 1.0, 1.4)],
    'entity.grub.shine': [('event:block.amethyst_block.chime', 0.6, 1.5), ('event:entity.allay.ambient_without_item', 0.15, 2.0)],
    'entity.grub.drink': [('event:entity.generic.drink', 0.6, 1.4)],
    'entity.grub.hurt': [('event:entity.silverfish.hurt', 0.8, 0.7), ('event:block.stone.hit', 0.5, 1.2)],
    'entity.grub.death': [('event:entity.silverfish.death', 0.8, 0.8), ('event:block.amethyst_cluster.break', 0.8, 1.2)],
}
SUBTITLES = {
    'entity.kerkorer.ker': 'Kerkorer calls', 'entity.kerkorer.ko': 'Kerkorer calls', 'entity.kerkorer.rer': 'Kerkorer rumbles',
    'entity.kerkorer.snap': 'Kerkorer snaps', 'entity.kerkorer.tongue': 'Tongue lashes', 'entity.kerkorer.lure': 'Something glints',
    'entity.kerkorer.shift': 'Sand shifts', 'entity.kerkorer.hiss': 'Kerkorer hisses', 'entity.kerkorer.hurt': 'Kerkorer hurts',
    'entity.kerkorer.death': 'Kerkorer dies', 'entity.kerkorer.step': 'Heavy footsteps',
    'entity.jaberora.chirp': 'Jaberora chirps', 'entity.jaberora.sing': 'Jaberora sings', 'entity.jaberora.pulse': 'Piercing note',
    'entity.jaberora.hop': 'Jaberora hops', 'entity.jaberora.eat': 'Jaberora eats', 'entity.jaberora.snore': 'Jaberora snores',
    'entity.jaberora.hurt': 'Jaberora hurts', 'entity.jaberora.death': 'Jaberora dies',
    'entity.reservoir.ambient': 'Reservoir sloshes', 'entity.reservoir.gulp': 'Reservoir drinks', 'entity.reservoir.drain': 'Chrome drained',
    'entity.reservoir.burst': 'Reservoir bursts', 'entity.reservoir.swell': 'Reservoir swells', 'entity.reservoir.bloom': 'Reservoir blooms',
    'entity.reservoir.hum': 'Monarch Reservoir hums', 'entity.reservoir.hurt': 'Reservoir hurts', 'entity.reservoir.death': 'Reservoir dies',
    'entity.grub.ambient': 'Grub skitters', 'entity.grub.step': 'Grub skitters', 'entity.grub.creak': 'Grub shell creaks',
    'entity.grub.crack': 'Grub shell cracks', 'entity.grub.shine': 'Grub chimes', 'entity.grub.drink': 'Grub drinks',
    'entity.grub.hurt': 'Grub hurts', 'entity.grub.death': 'Grub dies',
}


def sounds(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)


# ================================================================ data: loot, recipe, spawns, equipment, text

def _spawner(t, w, a, b):
    return {'type': rl(t), 'count': ({'type': 'minecraft:uniform', 'min_inclusive': a, 'max_inclusive': b} if a != b else a), 'weight': w}


def data(GA):
    import gen_data as D
    D.table('entity', 'entities/kerkorer', [
        D.pool([D.item('kerkorer_scale', count=(1, 3), extra=[D.LOOTING])]),
        D.pool([D.item('minecraft:bone', count=(0, 2))]),
    ])
    D.table('entity', 'entities/jaberora', [])  # it drops nothing (the user's wish): it is a friend, not a meal
    D.table('entity', 'entities/reservoir', [
        D.pool([D.item('minecraft:cactus', count=(0, 2))]),
        D.pool([D.item('chrome_pearl')], condition={'type': 'minecraft:all_of', 'terms': [D.PLAYER_KILL, D.chance(0.25)]}),
    ])
    D.table('entity', 'entities/monarch_reservoir', [
        D.pool([D.item('minecraft:cactus', count=(3, 6))]),
        D.pool([D.item('chrome_pearl', count=(2, 4))]),
        D.pool([D.item('star_shard')], condition=D.chance(0.5)),
    ])
    # a shelled Grub drops its rock; the cracked gem creature's gem is dropped by Grub.java (by its colour)
    D.table('entity', 'entities/grub', [
        D.pool([D.item('chime_sand', 2), D.item('minecraft:flint', 1)]),
    ])
    GA.shaped('kerkorer_cloak', ['S S', 'SLS', 'SSS'], {'S': 'kerkorer_scale', 'L': 'minecraft:leather'}, 'kerkorer_cloak', category='equipment')
    GA.tag('item', 'minecraft:chest_armor', rl('kerkorer_cloak'))
    GA.tag('item', f'{NS}:repairs_kerkorer_cloak', rl('kerkorer_scale'))
    GA.write(os.path.join(GA.A, 'equipment', 'kerkorer_cloak.json'), {'layers': {'humanoid': [{'texture': f'{NS}:kerkorer_cloak'}]}})
    GA.TEXTURES.add('entity/equipment/humanoid/kerkorer_cloak')
    # spawns: the Rocky Dunes only (ModDunes' spawn rules keep them to its sand; the Monarch is rare and alone)
    GA.write(os.path.join(GA.RES, 'data', NS, 'neoforge', 'biome_modifier', 'dunes_creatures.json'), {
        'type': 'neoforge:add_spawns', 'biomes': [f'{NS}:rocky_dunes'],
        'spawners': [_spawner('kerkorer', 3, 1, 1), _spawner('jaberora', 8, 3, 5), _spawner('reservoir', 5, 1, 2),
                     _spawner('monarch_reservoir', 1, 1, 1)]})
    GA.write(os.path.join(GA.RES, 'data', NS, 'neoforge', 'biome_modifier', 'dunes_grubs.json'), {
        'type': 'neoforge:add_spawns', 'biomes': [f'{NS}:rocky_dunes'], 'spawners': [_spawner('grub', 10, 2, 4)]})
    GA.LANG.update(lang())


def lang():
    c = f'codex.{NS}'
    L = {
        f'entity.{NS}.kerkorer': 'Kerkorer', f'entity.{NS}.jaberora': 'Jaberora', f'entity.{NS}.reservoir': 'Reservoir',
        f'entity.{NS}.monarch_reservoir': 'Monarch Reservoir', f'entity.{NS}.grub': 'Grub',
        f'band.{NS}.instrument.aria': 'Aria', f'band.{NS}.instrument.chrome_gurgle': 'Chrome Gurgle',
        f'band.{NS}.instrument.monarch_drone': 'Monarch Drone', f'band.{NS}.instrument.gem_chimes': 'Gem Chimes',
        f'message.{NS}.jaberora.tamed': 'The Jaberora sings for you now!',
        f'message.{NS}.jaberora.sit': 'Your Jaberora sits and waits.', f'message.{NS}.jaberora.follow': 'Your Jaberora follows you.',
        f'message.{NS}.reservoir.empty': 'The Reservoir is nearly dry - let it drink a while.',
        f'message.{NS}.cloak.hidden': 'Hidden - stay still...',
        f'message.{NS}.deafened.fumble': 'You cannot hear what you play!',
        f'{c}.kerkorer.title': 'Kerkorer', f'{c}.kerkorer.tagline': 'The dune that bites',
        f'{c}.kerkorer.body': ('A giant chameleon of the Rocky Dunes. Lying still it fades into the sand until only a glint is left: a '
                               'Diamond, a Golden Apple, a pearl or a lump of Siftite stuck to the pad of its tongue. Whatever walks up '
                               'to the treasure is snapped up - the jaws, then the tongue. It hunts Sifters, ignores sneakers, and calls '
                               'its own name ("ker-ko-rer") across the dunes. Jaberoras ride on it, sing prey towards it and sleep '
                               'curled against it. Its scales make the Kerkorer Cloak.'),
        f'{c}.kerkorer.notes': 'Habitat: Rocky Dunes|Temper: Ambush hunter|Diet: Sifters, the curious|Drops: Camouflage Scales, its bait (sometimes)',
        f'{c}.jaberora.title': 'Jaberora', f'{c}.jaberora.tagline': 'The diva of the dunes',
        f'{c}.jaberora.body': ('A jerboa of the dunes - pink, with tall ears, two bobbled antennae, small dot eyes, no nose, a mouth made for opera and a long shining tail. They live in noisy groups, bound '
                               'across the sand and sing arias at dusk - songs that lure prey to the Kerkorers they live with. Threaten '
                               'one and it lets out a single piercing note that stuns and Deafens. Sifters hunt them. Feed one Tuning Fruit '
                               'to tame it: it follows you, sings in your band and stuns your foes. Sneak-use to make it sit. It drops '
                               'nothing.'),
        f'{c}.jaberora.notes': 'Habitat: Rocky Dunes|Temper: Shy, sociable|Diet: Tuning Fruit|Drops: Nothing',
        f'{c}.reservoir.title': 'Reservoir', f'{c}.reservoir.tagline': 'A living cistern of Chrome',
        f'{c}.reservoir.body': ('A worm-cactus striped pink and blue, spined and slow, with a flower for a mouth. It drinks Chrome from the '
                                'air and the ground and stores it: the fuller it is the more it swells, pulses and glows. Use an empty '
                                'bucket on it to draw a Chrome Bucket; pour one in to feed it. Hit a full one and it may burst, spraying '
                                'Chrome all around. Grubs feed on it. Rarely a Monarch Reservoir grows huge and friendly - four blocks '
                                'wide and taller than a tree - and shares its Chrome freely.'),
        f'{c}.reservoir.notes': 'Habitat: Rocky Dunes|Temper: Peaceful, prickly|Diet: Chrome|Drops: Cactus, Chrome Pearls',
        f'{c}.grub.title': 'Grub', f'{c}.grub.tagline': 'A gem in a crust of rock',
        f'{c}.grub.body': ('Nib-like creatures crusted in sand and dune rock that skitter out of the dunes and bite. They gather round '
                           'Reservoirs to drink their Chrome. Play music near one: every note cracks its shell a little more until it '
                           'bursts open, freeing a shining gem creature - amethyst, emerald, diamond or prism. The gem creature is '
                           'calm, and drops its gem.'),
        f'{c}.grub.notes': 'Habitat: Rocky Dunes|Temper: Aggressive (until cracked)|Diet: Reservoir Chrome|Drops: Sand; gems once cracked',
        f'{c}.kerkorer_cloak.title': 'Kerkorer Cloak', f'{c}.kerkorer_cloak.tagline': 'Stand still and vanish',
        f'{c}.kerkorer_cloak.body': ('Camouflage Scales sewn onto leather, worn in place of a chestplate. Stand still for a moment and '
                                     'you fade from sight: no creature can find you until you move again.'),
        f'{c}.deafened.title': 'Deafened', f'{c}.deafened.tagline': 'A ringing in your ears',
        f'{c}.deafened.body': ('A Jaberora\'s piercing note, a Screecher\'s scream or a Glowball can deafen you. While Deafened the world '
                               'goes muffled and then silent but for the ringing - you will not hear creatures coming - and you cannot '
                               'play music: you cannot hear the notes, so no song comes out right. Deafened Wardens lose you.'),
    }
    return L


# ================================================================ art: spawn eggs, scale and cloak icons (16 x 16), the worn cloak

def _egg(half_rows, pal, body='b', lit='c', dark='d', **kw):
    import slumbler as SL
    rows = SL._mirror(half_rows)
    rows = SL._shade(rows, body, lit, dark, **kw)
    return SL._sprite(rows, pal)


def kerkorer_egg():
    """The Kerkorer: rose hide with plum strata, the bone crest, a turret eye on each side, the pink bait pad at its lip."""
    return _egg([
        '........',
        '.......w',
        '......ww',
        '.....oww',
        '....obbb',
        '...obBBB',
        '..obbbbb',
        '.oIIbbbb',
        '.oIPIbbb',
        'obIIbBBB',
        'obbbbbbb',
        'obbbbbmm',
        '.obbbbmp',
        '.obkkkkk',
        '..ookkkk',
        '....oooo',
    ], {'o': '#4a2232', 'b': '#a3626c', 'c': '#c98c8c', 'd': '#7e4652', 'B': '#5d2c41', 'w': '#e8ddd2', 'I': '#e8b03a', 'P': '#0d0812',
        'm': '#3d0c1e', 'p': '#f08f98', 'k': '#cac0dd'}, x_lit=6, y_lit=8, x_dark=11, y_dark=11)


def jaberora_egg():
    """The Jaberora, a jerboa: sandy fur, tall rounded ears pink inside, big dark eyes on the sides, no nose, a wide red
    mouth, a pale belly."""
    return _egg([
        '.oo.....',
        '.oeo....',
        '.oEo....',
        '.oEeo.oo',
        '..oobbbb',
        '.obbbbbb',
        '.oKKbbbb',
        '.oKKbbbb',
        'obbbbbbb',
        'obbmmmmm',
        'obbbmMMM',
        '.obbbmmm',
        '.okkkkkk',
        '.okkkkkk',
        '..ookkkk',
        '....oooo',
    ], {'o': '#4a2c24', 'b': '#c99876', 'c': '#dcb494', 'd': '#97654a', 'e': '#b27e5e', 'E': '#e08c9c', 'K': '#0a0610',
        'm': '#3d0c1e', 'M': '#aa4562', 'k': '#e8e2ec'}, x_lit=6, y_lit=8, x_dark=12, y_dark=12)


def _reservoir_egg(pink, blue, petal, mouth, outline, spine):
    return _egg([
        '......pp',
        '....p.Pp',
        '....pPPm',
        '.....ooo',
        '....oBBB',
        '...obbbb',
        '..soBBBB',
        '..obbbbb',
        '.ooBBBBB',
        '.obbbbbb',
        'soBBBBBB',
        '.obbbbbb',
        '.oBBBBBB',
        '.obbbbbb',
        '..ooBBBB',
        '....oooo',
    ], {'o': outline, 'b': pink[0], 'c': pink[1], 'd': pink[2], 'B': blue, 'p': petal[0], 'P': petal[1], 'm': mouth, 's': spine},
        x_lit=6, y_lit=8, x_dark=11, y_dark=12)


def reservoir_egg():
    return _reservoir_egg(('#c95686', '#e07aa2', '#a83a6c'), '#4268bc', ('#de6e9c', '#f6b0c8'), '#3d0c1e', '#3a1230', '#e8ddd2')


def monarch_reservoir_egg():
    return _reservoir_egg(('#cc7c42', '#e4a25e', '#ac5a2e'), '#2c45a4', ('#e48e5c', '#fcd8a8'), '#3d0c1e', '#3a1a10', '#fff4e0')


def grub_egg():
    """The Grub: a crust of rose dune rock and lilac sand on top, its amethyst gem body and two tiny eyes below."""
    return _egg([
        '........',
        '......ss',
        '....osss',
        '...orrsr',
        '..orrRrr',
        '..orsrrr',
        '.orrrRRR',
        '.orrrrrr',
        'oggggggg',
        'ogGggKgg',
        'oggggggg',
        'ogglgggg',
        '.ogggggg',
        '.lgggggg',
        '..oogggg',
        '....oooo',
    ], {'o': '#2e1424', 'r': '#a3626c', 'R': '#5d2c41', 's': '#dad2e9', 'g': '#7044ac', 'c': '#9468cc', 'd': '#542c8c', 'G': '#e6d0fa',
        'K': '#05060a', 'l': '#3e2230'}, body='g', lit='c', dark='d', x_lit=6, y_lit=12, x_dark=11, y_dark=13)


def scale_icon():
    """A Camouflage Scale: a rounded rose scale (vanilla scute shape) striped with plum strata, a pale lateral line, a bone rim."""
    import slumbler as SL
    rows = [
        '................',
        '................',
        '................',
        '.......aaa......',
        '......abccw.....',
        '.....acddcce....',
        '.....aBBBBcbe...',
        '....acddkdcfe...',
        '....abbcdfcfg...',
        '....eBBBBBBg....',
        '....ebbfbgg.....',
        '.....eggg.......',
        '................',
        '................',
        '................',
        '................',
    ]
    pal = {'a': '#8c505d', 'b': '#a3626c', 'c': '#b8777c', 'd': '#c98c8c', 'e': '#74404e', 'f': '#965960', 'g': '#5b2e3a',
           'B': '#73394f', 'k': '#e8ddd2', 'w': '#f6efe8'}
    return SL._sprite(rows, pal)


def cloak_icon():
    """The Kerkorer Cloak: a hooded cape of overlapping rose and plum scales, a gold-and-bone clasp, a lilac hem."""
    import slumbler as SL
    rows = [
        '................',
        '.....ohhhho.....',
        '....ohHHHHho....',
        '....ohH..Hho....',
        '...oohhwwhhoo...',
        '..oabaabbaabao..',
        '..obccbccbccbo..',
        '.oabaabkkbaabao.',
        '.obccbckkcbccbo.',
        '.oabaabaabaabao.',
        'oabccbccbccbccbo',
        'obaabaabaabaabao',
        'olllllllllllllo.',
        '.oolllllllllo...',
        '...oooooooo.....',
        '................',
    ]
    pal = {'o': '#4a2232', 'h': '#a3626c', 'H': '#c98c8c', 'w': '#f6efe8', 'a': '#8c505d', 'b': '#b8777c', 'c': '#73394f',
           'k': '#e8b03a', 'l': '#b8acce'}
    return SL._sprite(rows, pal)


def items():
    return {'kerkorer_spawn_egg': kerkorer_egg(), 'jaberora_spawn_egg': jaberora_egg(), 'reservoir_spawn_egg': reservoir_egg(),
            'monarch_reservoir_spawn_egg': monarch_reservoir_egg(), 'grub_spawn_egg': grub_egg(), 'kerkorer_scale': scale_icon(),
            'kerkorer_cloak': cloak_icon()}


def worn_cloak():
    """The cloak on a player (humanoid layer, 64 x 32; a chestplate slot draws the body and arm boxes): rows of
    overlapping scales in banded rose and plum, a lilac hem, a bone and gold clasp at the collar."""
    from PIL import Image
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    px = img.load()
    rose = [AK.hx(c) + (255,) for c in ROSE]
    plum = [AK.hx(c) + (255,) for c in PLUM]
    lilac = [AK.hx(c) + (255,) for c in CHIME]

    def faces(u, v, w, h, d):
        return {'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d), 'west': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
                'east': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h)}

    for (u, v, w, h, d) in ((16, 16, 8, 12, 4), (40, 16, 4, 12, 4)):
        for k, (fx, fy, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    row = y // 2
                    band = plum if row % 3 == 2 else rose
                    sx = (x + (row % 2)) % 2
                    t = 4 if (y % 2 == 0 and sx == 0) else 3 if y % 2 == 0 else 2 if sx else 1
                    if k in ('up', 'down'):
                        t = 3
                    col = band[t]
                    if k not in ('up', 'down') and y >= fh - 2:
                        col = lilac[4 if y == fh - 2 else 2]
                    px[fx + x, fy + y] = col
    for (x, y, c) in ((23, 20, '#e8ddd2'), (24, 20, '#e8ddd2'), (23, 21, '#e8b03a'), (24, 21, '#c4902a')):
        px[x, y] = AK.hx(c) + (255,)
    return img


def textures(out):
    out('entity/equipment/humanoid/kerkorer_cloak', worn_cloak())
