"""Block art: every Sift block texture at 16x16, made to look like Mojang's own blocks from another world.

Each texture is a vanilla texture used as the template - its pixel clusters, shapes and light (top-left) kept exactly -
recoloured onto a hand-picked, hue-shifted Sift ramp (cool shadows, warm lights; images 1, 2, 8): every texel keeps its
luminance offset from the texture's mean (Weber-scaled when the brightness moves, so the contrast reads like vanilla's),
so every distinct vanilla shade stays one colour - no new noise, no gradients, no outlines, no drawn shapes. Regions of
other materials (ore lumps, turf overhangs, bark rings, stems and petals, berries, metal bands, glass) take ramps of
their own. Families share one ramp and one brightness: the stone, cobbled, bricks, polished, tiles, chiseled and pillar
textures of a rock read as one set; ores are vanilla's ore layouts with the rock mapped exactly as the stone they
replace; every wood has log, top, stripped log, planks, door, trapdoor, leaves and sapling from one bark and one wood
ramp; plants keep vanilla's silhouettes; crystals are amethyst.

Also here: the Sift Drum's 16x block model and textures (the 32x drum sheet and tools/hdblocks.py are gone; the drum is
built from barrel, wool, iron block and sea lantern) and the Sift Portal's swirl (vanilla's 32-frame portal).

Runs last among the block painters in gen_textures.main() (after tools/vanilla_remap.py) and rewrites only the names in
recipes(). Left to their owners: caves_art.py, sea_reefs_art.py, sky_islands.py, wland_art.py (Chime Sand family,
Dunestone, White Lullwood, frost flowers, Rainbow Snow), chrome.py (Chime Sand/Glass, fluids), the Sculk Water, the
Sniffer egg (vanilla's own egg re-themed), the lore books, and the blocks being removed (encore sigil, instrument
altar, conductor's podium, glyph stone, echoer device).
"""
from __future__ import annotations

import json
import math
import os

import numpy as np
from PIL import Image

VANILLA = os.environ.get('VANILLA_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures/block')
NS = 'thesift'
CUTOUT = {'texture': {'mipmap_strategy': 'strict_cutout'}}


# ============================================================================ palettes (dark -> light, hue-shifted)

def R(*c):
    return list(c)


# rock
DREAM = R('#6c6678', '#847e8e', '#9d97a3', '#b5afb5', '#cac4c3', '#dbd6d0', '#e9e5dd', '#f6f3eb')     # pale Sift stone
DREAM_MOSS = R('#1d555a', '#276b6c', '#33827f', '#429a92', '#58b1a4', '#78c8b8')
HUSH = R('#121925', '#1a2331', '#232e3f', '#2d3b4e', '#38495e', '#46586f', '#576a82')               # deep slate-blue
BLUSH = R('#3e1626', '#561f33', '#6e2a42', '#873851', '#9e4861', '#b45b72', '#c87285', '#d98c9a')  # maroon-rose (image 2)
MAGNESITE = R('#8a7b6d', '#a19283', '#b7a998', '#cbbfae', '#dcd2c4', '#eae2d7', '#f6f1e9')
CORAL_SAND = R('#a2445a', '#b85467', '#cb6675', '#dc7a84', '#e98f94', '#f3a6a6', '#fabfba')
# glows and inlays
CYAN_GLOW = R('#1c8a98', '#2fb2bc', '#52d4d6', '#94efe7', '#dcfff8')
ROSE_GLOW = R('#b8406e', '#dc6290', '#f490b4', '#ffc8dc')
GOLD = R('#6e420e', '#946016', '#ba8124', '#d9a23a', '#efc45e', '#fce39c')
# ground
SOIL = R('#43263a', '#543046', '#653c54', '#764962', '#875870', '#98687f', '#a87a8e')
CORAL_TURF = R('#ad3c50', '#bf4a5d', '#cf596a', '#de6977', '#ea7a85', '#f48d95', '#fba4a7')       # the pink ground (image 8)
SIFT_GRASS = R('#145e5c', '#1b736f', '#248a83', '#2fa196', '#3eb6a8', '#55cab9', '#79dccb')        # teal Sift grass
WHITE_TURF = R('#8399b4', '#9aafc7', '#b1c4d7', '#c6d6e5', '#d9e5f0', '#eaf1f8', '#f8fbfe')
LUMEN = R('#0d4553', '#135a6a', '#1b7183', '#25899a', '#33a1ae', '#47b9c0', '#68d1cf')
SCULK_MUD = R('#06121a', '#0a1a22', '#0f232b', '#152d35', '#1c3840', '#24434b')
CLOUD = R('#9fb3c9', '#b4c6d9', '#c7d6e5', '#d8e4ef', '#e7eff6', '#f3f8fb', '#ffffff')
# woods: bark, wood, leaves
LULL_BARK = R('#161f27', '#1e2a33', '#283640', '#32434e', '#3e515d', '#4b606d')
LULL_WOOD = R('#696487', '#7c779a', '#908bad', '#a49fbf', '#b8b4d0', '#cbc8df', '#dddbec')
LULL_LEAF = R('#6f88a0', '#86a0b5', '#9db6c8', '#b4cad9', '#cbdde8', '#e1edf4', '#f4f9fb')
WISH_BARK = R('#34132b', '#4a1b3b', '#61264c', '#78335e', '#8f4471', '#a55985')
WISH_WOOD = R('#a65676', '#ba6a89', '#cb819c', '#da98af', '#e7b0c2', '#f2c8d5', '#fadde6')
WISH_LEAF = R('#b54b85', '#cb6299', '#de7cae', '#ec98c3', '#f6b5d6', '#fcd2e7')
BLIGHT_BARK = R('#091013', '#0f181c', '#152126', '#1c2b31', '#24363d', '#2e434a')
BLIGHT_WOOD = R('#0f272b', '#153236', '#1b3e42', '#234b4f', '#2c595c', '#37686a')
BLIGHT_LEAF = R('#071a1c', '#0b2527', '#103133', '#173f3e', '#1f4f4b', '#2b625b')
# plants
CORAL = R('#982c42', '#b43e54', '#cc5266', '#e0687a', '#ef8290', '#f9a2ab')            # coral-pink fronds (images 1, 8)
STEM = R('#13403a', '#1b544b', '#25695b', '#317f6b', '#40957d', '#55ab91')
CYAN_PETAL = R('#1a5784', '#2777a8', '#3899c8', '#5abbe0', '#8ed6f0', '#ccf0fb')
ROSE_PETAL = R('#922a54', '#b03e6b', '#ca5584', '#e0719c', '#f090b7', '#fab3cf')
LAVENDER = R('#523483', '#6a4aa1', '#8463bc', '#a081d2', '#bca2e3', '#d9c6f1')
INDIGO = R('#272572', '#373894', '#4b50b4', '#6569cd', '#8a90e0', '#b4bbf0')
WHITE_PETAL = R('#8396ae', '#a1b2c6', '#bdcbdb', '#d5e0eb', '#e9f0f6', '#fbfdff')
SCULK = R('#020a0d', '#05161b', '#0a252c', '#10363f', '#174b55', '#206a73')
CHROME = R('#3a6577', '#558598', '#75a7b7', '#97c7d3', '#bee2ea', '#e8fbff')
REED = R('#3e2a6e', '#53398a', '#6a4ca4', '#8463bc', '#a081d0', '#c0a6e4')
# ore minerals
SIFTITE = R('#143068', '#1f4c98', '#3274c0', '#4fa2e0', '#86d2f4', '#d6f6ff')
PRISM = R('#3c2168', '#62389e', '#9058c8', '#c670d4', '#f29ed8', '#fff0fa')
BAUXITE = R('#4a160c', '#70260f', '#982f1a', '#bf5428', '#de7e44', '#f2b078')
GALENA = R('#171a24', '#272c38', '#3d4556', '#5a6479', '#848fa4', '#c4cede')
EGG = R('#7a4826', '#a66c44', '#d09a70', '#eac49e', '#f8e2c8', '#fff8ee')
SCULKITE = R('#0b3a48', '#126c7c', '#1aa0aa', '#44e6e0', '#c2fff8')
# metals, glass, machines
SOUL = R('#0b1846', '#132d76', '#1d4ca6', '#367ad0', '#62b0ec', '#b0e4ff')
SLIME = R('#1d6a72', '#2a898e', '#3fa9a8', '#5cc6be', '#88e0d4', '#c4f8ee')
SILVER = R('#465466', '#617388', '#7f91a6', '#9eb0c3', '#c0cedc', '#e6eef6')
VERDIGRIS = R('#1d4c4a', '#286660', '#378278', '#4a9e90', '#66baa8', '#90d4c2', '#c6efe2')
BRONZE = R('#3a2010', '#583116', '#7a4620', '#9c5f2c', '#bd7c3c', '#da9e58', '#f0c486')
IRON = R('#14171d', '#1f242c', '#2c323c', '#3b434f', '#4d5663', '#626c7a', '#7d8896')
DARK_WALNUT = R('#22130d', '#321c14', '#43271b', '#563323', '#6a412d', '#80523a')
CLAY = R('#6e3240', '#86404c', '#9c5058', '#b06266', '#c37676', '#d48c88')
GLAZE = R('#155a64', '#1d7682', '#2a94a0', '#42b2b8', '#6ccfcc', '#a8ece4')
HIDE = R('#8a7468', '#a48e80', '#bea898', '#d4c2b0', '#e6d8c6', '#f4ebdc', '#fdf8ee')
DRUM_WOOD = R('#24163a', '#33204e', '#432b63', '#563878', '#6b488e', '#8360a6')
DRUM_HOOP = R('#0f3348', '#174a64', '#216482', '#2e7fa0', '#4499bb', '#66b8d2', '#9ad8e8')
TWIG = R('#4a3a36', '#5e4b44', '#735e55', '#8a7467', '#a28c7c', '#baa592')
FROSTING = R('#c8a2b8', '#dcb8ca', '#ead0dc', '#f4e2ea', '#fbf0f4', '#ffffff')
SPONGE = R('#8a3f63', '#a54f78', '#bd648c', '#d17ca0', '#e296b3')


# ============================================================================ basics

def _hx(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float64)


def van(name, frame=0):
    a = np.asarray(Image.open(os.path.join(VANILLA, name + '.png')).convert('RGBA'), dtype=np.float64)
    w = a.shape[1]
    if frame is None:
        return a.copy()
    return a[frame * w:(frame + 1) * w].copy()


def lum(a):
    return a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114


def hue_sat(a):
    rgb = a[..., :3] / 255.0
    mx, mn = rgb.max(-1), rgb.min(-1)
    d = mx - mn + 1e-9
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    h = np.where(mx == r, ((g - b) / d) % 6, np.where(mx == g, (b - r) / d + 2, (r - g) / d + 4)) * 60.0
    s = np.where(mx > 0, (mx - mn) / (mx + 1e-9), 0)
    return h, s


def opaque(a):
    return a[..., 3] > 0


def _ramp(ramp):
    """A ramp as (luminance, rgb) arrays sorted by luminance."""
    cols = np.array([_hx(c) for c in ramp])
    Lr = lum(cols)
    o = np.argsort(Lr, kind='stable')
    return Lr[o], cols[o]


def remap(a, ramp, mask=None, at=0.5, k=None, mv=None):
    """Recolours the masked texels onto a Sift ramp keeping vanilla's value structure exactly: every texel keeps its
    luminance offset from the texture's mean (times k, a contrast factor: by default the Weber-style scaling that
    keeps the texture's apparent contrast when the mean moves), around the ramp's luminance at position `at` (0-1;
    a value above 1 is a target mean luminance itself), and
    takes the ramp's hue there. Every distinct vanilla shade stays one distinct colour: no new noise, no gradients."""
    out = a.copy()
    m = opaque(a) if mask is None else (mask & opaque(a))
    if not m.any():
        return out
    Lr, cols = _ramp(ramp)
    L = lum(a)[m]
    mv = L.mean() if mv is None else mv
    T = at if at > 1 else np.interp(at, np.linspace(0, 1, len(Lr)), Lr)  # at > 1: a target mean luminance
    if k is None:
        k = float(np.clip((T / max(mv, 1.0)) ** 0.7, 0.75, 2.6))
    Lt = T + (L - mv) * k
    out[..., :3][m] = np.stack([np.interp(Lt, Lr, cols[:, c]) for c in range(3)], -1)
    return out


def regions_on(a, parts):
    """parts: [(mask_fn(a) -> bool array or None, ramp, at, k)]; each claims its texels in turn (None: all the rest),
    and each region keeps its own vanilla value structure on its own ramp."""
    out = a.copy()
    claimed = np.zeros(a.shape[:2], bool)
    for fn, ramp, at, k in parts:
        m = (fn(a) if fn else np.ones(a.shape[:2], bool)) & ~claimed & opaque(a)
        if m.any():
            out = remap(out, ramp, m, at, k)
        claimed |= m
    return out


def box_mask(x0, y0, x1, y1):
    m = np.zeros((16, 16), bool)
    m[y0:y1 + 1, x0:x1 + 1] = True
    return m


# ============================================================================ builders

def simple(ref, ramp, at=0.5, k=None, frame=0):
    return lambda: remap(van(ref, frame), ramp, at=at, k=k)


def regions(ref, parts, frame=0):
    return lambda: regions_on(van(ref, frame), parts)


def green(a):
    h, s = hue_sat(a)
    return (h > 60) & (h < 175) & (s > 0.18)


def warm(a):
    h, s = hue_sat(a)
    return ((h < 70) | (h > 300)) & (s > 0.3)


def yellow(a):
    h, s = hue_sat(a)
    return (h > 35) & (h < 70) & (s > 0.45)


def grey(a):
    return hue_sat(a)[1] < 0.1


def border(a):
    m = np.zeros(a.shape[:2], bool)
    m[0, :] = m[-1, :] = m[:, 0] = m[:, -1] = True
    return m


def lighter(q):
    def fn(a):
        L = lum(a)
        m = opaque(a)
        return m & (L >= np.quantile(L[m], q))
    return fn


# ---------------------------------------------------------------------------- ground

def soil(ref='dirt'):
    """Dirt with its pebbles in the pale stone's lilac-greys."""
    return regions(ref, [(grey, DREAM, 92, 1.0), (None, SOIL, 0.45, None)])


def turf_side(top_ramp, top_at, overlay='grass_block_side_overlay', k=0.85):
    """The soil below with the turf's overhang over its top rows, exactly like vanilla's grass block side."""
    def make():
        a = soil()()
        ov = van(overlay)
        m = opaque(ov) & (lum(ov) > 165) if overlay == 'grass_block_snow' else opaque(ov)
        top = remap(ov, top_ramp, m, top_at, k)
        a[m] = top[m]
        return a
    return make


# ---------------------------------------------------------------------------- wood

def log_top(ref, bark, wood, bark_at=0.5, wood_at=0.5):
    return regions(ref, [(border, bark, bark_at, None), (None, wood, wood_at, None)])


def plank_door(ref, wood, at=0.5):
    """Doors and trapdoors: the wood in its ramp, the iron hinges and handles in iron."""
    def make():
        a = van(ref)
        h, s = hue_sat(a)
        metal = opaque(a) & (s < 0.12) & (lum(a) < 110)
        return remap(remap(a, wood, opaque(a) & ~metal, at), IRON, metal, 0.4)
    return make


# ---------------------------------------------------------------------------- plants

def plant(ref, petal, stem=STEM, frame=0, petal_at=0.55, accent=None, stem_at=0.5):
    """A flower: green texels are stem and leaves, the rest petals (accent: (mask_fn, ramp) for a centre)."""
    parts = []
    if accent:
        parts.append((accent[0], accent[1], 0.6, None))
    parts += [(green, stem, stem_at, None), (None, petal, petal_at, None)]
    return regions(ref, parts, frame)


# ---------------------------------------------------------------------------- ores

def ore(ref_ore, ref_stone, stone_ramp, stone_at, gem_ramp, gem_at=0.5, sat=0.12, gem_k=1.0, mineral=None):
    """The vanilla ore convention: vanilla's ore texture with its rock mapped exactly as our stone texture maps
    vanilla's stone (same mean, same contrast: the ore sits on the very rock it replaces) and its mineral texels
    on the gem's ramp."""
    def make():
        o = van(ref_ore)
        s = van(ref_stone)
        h, st = hue_sat(o)
        diff = np.abs(o - s).sum(-1) > 0
        mine = diff & (st > sat) if mineral is None else diff & mineral(o)
        out = remap(o, stone_ramp, ~mine, stone_at, mv=lum(s).mean())
        out = remap(out, gem_ramp, mine, gem_at, gem_k)
        out[..., 3] = 255
        return out
    return make


# ---------------------------------------------------------------------------- crystals

def frozen_crystal(ramp):
    """The crystal seen through clear ice: amethyst's facets in the gem's pale half, packed ice's streaks over it."""
    def make():
        a = remap(van('amethyst_block'), ramp, at=0.65, k=0.8)
        ice = van('packed_ice')
        L = lum(ice)
        frost = np.array([232, 246, 255.0])
        w = np.clip((L - L.mean()) / (L.max() - L.mean() + 1e-9), 0, 1)[..., None] * 0.55 + 0.2
        a[..., :3] = a[..., :3] * (1 - w) + frost * w
        return a
    return make


# ============================================================================ designed blocks (all on vanilla bodies)

def lantern():
    """The Bulb Lantern: vanilla's lantern in dark teal iron, a glowing slime-bulb in place of the flame."""
    f = van('lantern', 0)
    h, s = hue_sat(f)
    light = opaque(f) & (((h < 70) & (s > 0.3)) | (lum(f) > 200))
    out = remap(f, IRON, ~light, 0.45)
    return remap(out, R('#2a9a9a', '#45c2b8', '#7ee2d2', '#bff6ea', '#f0fffb'), light, 0.6)


def gate_frame(part):
    """The Sift Gate frame: reinforced deepslate's clamped block in hushslate, its clamps in pale siftite steel,
    and the lighter texels of its core grown through with cyan gate-crystal."""
    def make():
        a = van(f'reinforced_deepslate_{part}')
        h, s = hue_sat(a)
        L = lum(a)
        clamp = opaque(a) & (s > 0.12) & (L > 120)
        if part == 'side':  # the pill-shaped core between the clamps
            vein = box_mask(5, 5, 10, 9) & ~clamp
        else:  # the round boss in the middle of the top
            yy, xx = np.mgrid[0:16, 0:16]
            vein = (np.hypot(xx - 7.5, yy - 7.5) < 4.2) & ~clamp
        out = remap(a, HUSH, ~clamp & ~vein, 0.45, mv=lum(van('reinforced_deepslate_side')).mean())
        out = remap(out, R('#5d7f98', '#7c9fb6', '#9cbfd2', '#bcdbe8', '#dcf2f8'), clamp, 0.5)
        return remap(out, CYAN_GLOW, vein, 0.4, 1.8)
    return make


def portal():
    """The Sift portal: vanilla's 32-frame swirl, blue to cyan to white, with drifting rose bands."""
    a = van('nether_portal', None)
    n = a.shape[0] // 16
    cyan = R('#1d4f92', '#2670b0', '#3392c8', '#4fb4da', '#7ad2e8', '#b4ecf2', '#eefffd')
    rose = R('#7a3a92', '#a04ea6', '#c268b6', '#de88c6', '#f0aad6', '#fad0e6', '#fff2f8')
    yy, xx = np.mgrid[0:a.shape[0], 0:16]
    f = yy // 16
    field = np.sin((xx * 0.55 + (yy % 16) * 0.35) + f * (2 * math.pi / n)) + 0.6 * np.sin((xx - (yy % 16)) * 0.3 - f * (4 * math.pi / n))
    pink = field > 0.75
    mv = lum(a).mean()
    out = remap(a, cyan, ~pink, 0.5, 1.6, mv=mv)
    return remap(out, rose, pink, 0.5, 1.6, mv=mv)


def planter(part):
    """The Pitcher Planter: blush bricks with a band of glazed teal terracotta and a glazed lip, wet mud inside."""
    def make():
        if part == 'top':
            return remap(van('mud'), R('#1e1219', '#2a1923', '#37222e', '#452b3a', '#543648', '#65445a'), at=0.45)
        glaze = remap(van('cyan_terracotta'), GLAZE, at=0.55, k=3.0)
        if part == 'rim':
            return glaze
        a = remap(van('nether_bricks'), BLUSH, at=0.5)
        if part == 'side':  # the band under the lip (the model shows the side's rows 7-15)
            a[7:9] = glaze[7:9]
        return a
    return make


def cannon(part):
    def make():
        if part == 'barrel':  # cast bronze (the model shows its top rows)
            return remap(van('copper_block'), BRONZE, at=0.5)
        if part == 'carriage':  # walnut beams
            return remap(van('dark_oak_planks'), DARK_WALNUT, at=0.55)
        if part == 'ball':  # the cannonball (its centre 4x4 is shown): dark cast iron
            return remap(van('anvil'), IRON, at=0.35)
        # the muzzle: bronze round a dark bore (the model shows the centre 6x6 and 7x7)
        a = remap(van('copper_block'), BRONZE, at=0.55)
        bore = box_mask(6, 6, 9, 9)
        dark = remap(van('hopper_inside'), IRON, at=0.12, k=1.2)
        a[bore] = dark[bore]
        return a
    return make


def hut_heart(part, spent=False):
    """The Echoer's Hearthstone: chiseled tuff in magnesite cream, its carved grooves glowing cyan (dark when spent)."""
    def make():
        a = van('chiseled_tuff' if part == 'side' else 'chiseled_tuff_top')
        L = lum(a)
        inner = box_mask(1, 1, 14, 14) if part == 'top' else box_mask(0, 3, 15, 12)
        groove = opaque(a) & inner & (L <= np.quantile(L[inner], 0.3))
        out = remap(a, MAGNESITE, ~groove, 0.55)
        if spent:
            return remap(out, R('#3c4a50', '#4c5c62', '#5e6e74', '#728286'), groove, 0.5)
        return remap(out, CYAN_GLOW, groove, 0.45 if part == 'side' else 0.6, 1.3)
    return make


def europhy_side():
    """The Europhy Table's Magnesite plinth: lodestone's banded stone in cream, its band in bronze."""
    a = van('lodestone_side')
    L = lum(a)
    band = opaque(a) & (L < np.quantile(L[opaque(a)], 0.25))
    return remap(remap(a, MAGNESITE, ~band, 0.55), BRONZE, band, 0.4)


def cake(part):
    """The Sift Cake: vanilla's cake, rose sponge under pale frosting, glowing slime drops for cherries and a seam of
    slime jelly through the sponge."""
    def make():
        a = van(f'cake_{part}')
        h, s = hue_sat(a)
        L = lum(a)
        red = opaque(a) & (s > 0.55) & ((h < 12) | (h > 340))
        frost = opaque(a) & (L > 180) & ~red
        sponge = opaque(a) & ~red & ~frost
        jelly = np.zeros_like(sponge)
        if part in ('side', 'inner'):
            rows = [y for y in range(9, 16) if sponge[y].sum() > 6]
            if rows:
                jelly[rows[len(rows) // 2 - 1]] = True
        jelly &= sponge
        out = remap(a, FROSTING, frost, 0.6)
        out = remap(out, SPONGE, sponge & ~jelly, 0.5)
        out = remap(out, SLIME, jelly, 0.55)
        return remap(out, R('#1f8f96', '#3dbcbc', '#7ee4dc', '#d0fff6'), red, 0.5)
    return make


def den(part):
    """The Swifter den: woven twigs (hay's weave in driftwood greys) with white fluff caught in the weave."""
    def make():
        if part == 'fluff':
            return remap(van('white_wool'), CLOUD, at=0.6)
        if part == 'bottom':
            return remap(van('packed_mud'), TWIG, at=0.45)
        a = van('hay_block_side' if part == 'side' else 'hay_block_top')
        fluff = lighter(0.9 if part == 'side' else 0.82)(a)
        return remap(remap(a, TWIG, ~fluff, 0.5), CLOUD, fluff, 0.75, 0.8)
    return make


def glowbell(ref, lit):
    def make():
        a = van(ref)
        h, s = hue_sat(a)
        berry = opaque(a) & (((h < 60) | (h > 330)) & (s > 0.45))
        out = remap(a, R('#0f3b3a', '#185048', '#236757', '#317f68', '#43987a', '#5cb08e', '#7ac8a4'), ~berry, 0.5)
        return remap(out, GOLD if lit else R('#1e4f5a', '#2a6670', '#3a7f86', '#4f979a', '#68aeae'), berry, 0.55)
    return make


# ============================================================================ the Sift Drum (16x model + textures)

def drum_textures():
    """Mojang-built drum: barrel's staves and iron bands for the shell (violet lacquer, cyan hoops), a hide head of
    wool's soft weave, brushed iron for the hoops and foot, and sea lantern's glow behind the core window."""
    o = {}
    b = van('barrel_side')
    h, s = hue_sat(b)
    band = opaque(b) & (s < 0.2)
    side = remap(remap(b, DRUM_WOOD, ~band, 0.5), DRUM_HOOP, band, 0.45)
    o['sift_drum_side'] = side
    head = remap(van('white_wool'), HIDE, at=0.6)
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.maximum(np.abs(xx - 7.5), np.abs(yy - 7.5))
    rim = d > 5.0  # where the hide rolls over the rim (the model shows x, y 2-13)
    head = remap(head, HIDE, rim & opaque(head), 0.35, mv=lum(van('white_wool')).mean())
    o['sift_drum_head'] = head
    r = np.hypot(xx - 7.5, yy - 7.5)
    ripple = (r > 3.0) & (r < 4.4)
    o['sift_drum_head_struck'] = remap(head.copy(), HIDE, ripple, 0.8, mv=lum(van('white_wool')).mean())
    o['sift_drum_hoop'] = remap(van('iron_block'), DRUM_HOOP, at=0.55)
    o['sift_drum_base'] = remap(van('iron_block'), DRUM_HOOP, at=0.4)
    return o


def drum_model(hit, ns=NS, bid='sift_drum'):
    """The drum: a hooped foot, a lacquered stave shell laced with rope, a hide head inside four cyan hoop bars.
    hit: 0 at rest, 1 struck (head pressed in), 2 rebound. (MANSION: no Warden Core window any more.)"""
    def f(tex, uv):
        return {'texture': tex, 'uv': uv}
    shell = '#side'
    head_top = {0: 13.25, 1: 12.5, 2: 13.75}[hit]
    head = '#struck' if hit else '#head'
    side_uv = [1.5, 3, 14.5, 13]
    els = [
        {'from': [1, 0, 1], 'to': [15, 2, 15], 'faces': {d: f('#base', [1, 14, 15, 16]) for d in ('north', 'south', 'east', 'west')}
         | {'up': f('#base', [1, 1, 15, 15]), 'down': f('#base', [1, 1, 15, 15])}},
        {'from': [1.5, 2, 1.5], 'to': [14.5, 12, 14.5], 'faces': {d: f(shell, side_uv) for d in ('north', 'south', 'east', 'west')}},
        {'from': [2, 11, 2], 'to': [14, head_top, 14], 'faces': {'up': f(head, [2, 2, 14, 14])}},
    ]
    for frm, to, outer, inner in (([1, 11.5, 1], [15, 14, 2], 'north', 'south'), ([1, 11.5, 14], [15, 14, 15], 'south', 'north'),
                                  ([1, 11.5, 1], [2, 14, 15], 'west', 'east'), ([14, 11.5, 1], [15, 14, 15], 'east', 'west')):
        ends = ('east', 'west') if outer in ('north', 'south') else ('north', 'south')
        faces = {outer: f('#hoop', [1, 5.5, 15, 8]), inner: f('#hoop', [1, 8, 15, 10.5]), 'up': f('#hoop', [1, 6, 15, 7]),
                 'down': f('#hoop', [1, 9, 15, 10]), ends[0]: f('#hoop', [0, 5.5, 1, 8]), ends[1]: f('#hoop', [15, 5.5, 16, 8])}
        els.append({'from': frm, 'to': to, 'faces': faces})
    tex = {'particle': f'{ns}:block/{bid}_side', 'side': f'{ns}:block/{bid}_side',
           'head': f'{ns}:block/{bid}_head', 'struck': f'{ns}:block/{bid}_head_struck', 'hoop': f'{ns}:block/{bid}_hoop',
           'base': f'{ns}:block/{bid}_base'}
    return {'parent': 'minecraft:block/block', 'textures': tex, 'elements': els}


# ============================================================================ the recipe table
# Brightness: `at` above 1 is the texture's target mean luminance (vanilla: stone 125, deepslate 80, calcite 224,
# dirt 104, grass top 147); contrast follows vanilla's own (Weber-scaled), so no texture is noisier than its template.

def recipes(old):
    r = {}
    # ---- pale Sift stone (dreamstone) and its family: one ramp, one brightness
    r['dreamstone'] = simple('stone', DREAM, 192)
    r['cobbled_dreamstone'] = simple('cobblestone', DREAM, 186)
    r['polished_dreamstone'] = simple('polished_andesite', DREAM, 194)
    r['dreamstone_bricks'] = simple('stone_bricks', DREAM, 188)
    r['cracked_dreamstone_bricks'] = simple('cracked_stone_bricks', DREAM, 184)
    r['mossy_dreamstone_bricks'] = regions('mossy_stone_bricks', [(green, DREAM_MOSS, 0.5, None), (None, DREAM, 186, None)])
    r['dreamstone_tiles'] = simple('deepslate_tiles', DREAM, 184, 1.1)
    r['chiseled_dreamstone'] = simple('chiseled_stone_bricks', DREAM, 188)
    r['dreamstone_pillar_side'] = simple('quartz_pillar_side', DREAM, 196, 1.3)
    r['dreamstone_pillar_top'] = simple('quartz_pillar_top', DREAM, 196, 1.3)
    r['magnesite'] = simple('calcite', MAGNESITE, 205)
    # ---- hushslate (the deep stone)
    r['hushslate'] = simple('deepslate', HUSH, 70)
    r['hushslate_top'] = simple('deepslate_top', HUSH, 70)
    r['cobbled_hushslate'] = simple('cobbled_deepslate', HUSH, 68)
    r['polished_hushslate'] = simple('polished_deepslate', HUSH, 70)
    r['hushslate_bricks'] = simple('deepslate_bricks', HUSH, 68)
    r['cracked_hushslate_bricks'] = simple('cracked_deepslate_bricks', HUSH, 66)
    r['hushslate_tiles'] = simple('deepslate_tiles', HUSH, 64)
    r['chiseled_hushslate'] = simple('chiseled_deepslate', HUSH, 68)
    # ---- maroon-rose bricks (image 2's towers)
    r['blush_bricks'] = simple('nether_bricks', BLUSH, 100)
    r['cracked_blush_bricks'] = simple('cracked_nether_bricks', BLUSH, 98)
    r['chiseled_blush_bricks'] = simple('chiseled_nether_bricks', BLUSH, 100)
    # ---- sand, soils, turf, moss, mud, cloud
    r['coral_sand'] = simple('red_sand', CORAL_SAND, 165, 1.4)
    r['sift_soil'] = soil()
    r['coral_turf_top'] = simple('grass_block_top', CORAL_TURF, 140, 0.8)
    r['coral_turf_side'] = turf_side(CORAL_TURF, 140)
    r['sift_grass_block_top'] = simple('grass_block_top', SIFT_GRASS, 118, 0.8)
    r['sift_grass_block_side'] = turf_side(SIFT_GRASS, 118)
    r['white_turf_top'] = simple('grass_block_top', WHITE_TURF, 218, 0.6)
    r['white_turf_side'] = turf_side(WHITE_TURF, 222, 'grass_block_snow', 0.8)
    r['lumen_moss_block'] = simple('moss_block', LUMEN, 112)
    r['sculk_mud'] = simple('mud', SCULK_MUD, 38)
    r['cloud_block'] = simple('white_wool', CLOUD, 226)
    # ---- woods: lullwood (spruce's build), wishwood (cherry's), blightwood (dark oak's)
    for w, (bark, wood, ref, bL, wL) in {'lullwood': (LULL_BARK, LULL_WOOD, 'spruce', 56, 160),
                                         'wishwood': (WISH_BARK, WISH_WOOD, 'cherry', 68, 176),
                                         'blightwood': (BLIGHT_BARK, BLIGHT_WOOD, 'dark_oak', 30, 62)}.items():
        r[f'{w}_log'] = simple(f'{ref}_log', bark, bL)
        r[f'{w}_log_top'] = log_top(f'{ref}_log_top', bark, wood, bL, wL)
        r[f'stripped_{w}_log'] = simple(f'stripped_{ref}_log', wood, wL)
        r[f'stripped_{w}_log_top'] = log_top(f'stripped_{ref}_log_top', wood, wood, wL - 25, wL)
        r[f'{w}_planks'] = simple(f'{ref}_planks', wood, wL)
        r[f'{w}_door_top'] = plank_door(f'{ref}_door_top', wood, wL)
        r[f'{w}_door_bottom'] = plank_door(f'{ref}_door_bottom', wood, wL)
        r[f'{w}_trapdoor'] = plank_door(f'{ref}_trapdoor', wood, wL)
    r['lullwood_leaves'] = simple('pale_oak_leaves', LULL_LEAF, 188, 1.0)
    r['lullwood_sapling'] = regions('pale_oak_sapling', [(lighter(0.35), LULL_LEAF, 180, 1.0), (None, LULL_BARK, 60, None)])
    r['hanging_lullwood_leaves'] = simple('pale_hanging_moss', LULL_LEAF, 184, 1.0)
    r['hanging_lullwood_leaves_tip'] = simple('pale_hanging_moss_tip', LULL_LEAF, 184, 1.0)
    r['wishwood_leaves'] = simple('cherry_leaves', WISH_LEAF, 176, 1.0)
    r['wishwood_sapling'] = plant('cherry_sapling', WISH_LEAF, stem=WISH_BARK, petal_at=0.6, stem_at=0.5)
    r['blightwood_leaves'] = simple('dark_oak_leaves', BLIGHT_LEAF, 46, 0.7)
    r['blightwood_sapling'] = plant('dark_oak_sapling', BLIGHT_BARK, stem=BLIGHT_LEAF, petal_at=0.5, stem_at=0.6)
    # ---- ground plants and flowers: vanilla's silhouettes, Sift colours
    r['blushgrass'] = simple('short_grass', CORAL, 140, 0.85)
    r['tall_blushgrass_bottom'] = simple('tall_grass_bottom', CORAL, 134, 0.85)
    r['tall_blushgrass_top'] = simple('tall_grass_top', CORAL, 140, 0.85)
    r['coral_bush'] = simple('fire_coral', CORAL, 140)
    r['coral_thicket_bottom'] = lambda: regions_on(old['coral_thicket_bottom'], [(None, CORAL, 136, None)])  # its own fronds, re-toned
    r['coral_thicket_top'] = lambda: regions_on(old['coral_thicket_top'], [(None, CORAL, 142, None)])
    r['dreambloom'] = plant('poppy', ROSE_PETAL)
    r['echo_orchid'] = plant('blue_orchid', CYAN_PETAL)
    for i in range(3):
        r[f'echo_orchid_crop_stage{i}'] = plant(f'potatoes_stage{i}', CYAN_PETAL)
    r['hummingbloom'] = plant('red_tulip', LAVENDER)
    r['nebula_iris'] = plant('cornflower', INDIGO)
    r['soulpetal'] = plant('oxeye_daisy', WHITE_PETAL, accent=(yellow, CYAN_PETAL), petal_at=0.7)
    r['puffbloom'] = plant('allium', WHITE_PETAL, petal_at=0.65)
    r['sculk_bloom'] = plant('wither_rose', R('#0a2a30', '#123e46', '#1c5860', '#2f8a90', '#58d0cc', '#b4fff4'), stem=SCULK, stem_at=0.6)
    r['lullaby_bell'] = plant('lily_of_the_valley', CYAN_PETAL, petal_at=0.7)
    r['chime_bell'] = plant('closed_eyeblossom', CYAN_PETAL, frame=0)
    r['chime_bell_ringing'] = plant('open_eyeblossom', R('#2a7fb0', '#4aa6d6', '#7ccaee', '#b4e8fa', '#ecfcff'), frame=0, petal_at=0.6)
    r['glimmer_sprouts'] = simple('warped_roots', SIFT_GRASS, 0.55)
    r['glowcap'] = regions('warped_fungus', [(warm, CYAN_PETAL, 0.55, None),
                                             (green, R('#1e7a86', '#2a9aa6', '#40bac2', '#66d6da', '#a0f0ee'), 0.5, None),
                                             (None, R('#8a8ea6', '#a8acc0', '#c6cad8', '#e2e4ee'), 0.5, None)])
    r['chrome_reeds'] = simple('sugar_cane', CHROME, 0.55)
    r['organ_reed_bottom'] = simple('sugar_cane', REED, 0.5)
    r['organ_reed_top'] = simple('sugar_cane', REED, 0.5)
    for c, ramp in (('amber', R('#6a2e08', '#90460c', '#b66214', '#d6822a', '#ecaa4c', '#f8d084')),
                    ('azure', R('#123a6a', '#1c5590', '#2a74b4', '#4498d2', '#6cbce8', '#a8def8')),
                    ('rose', R('#7a1e48', '#9c2c5e', '#bc4076', '#d65c90', '#ea7eac', '#f8a8ca'))):
        r[f'{c}_glowkelp'] = simple('kelp_plant', ramp, 0.6)
        r[f'{c}_glowkelp_tip'] = simple('kelp', ramp, 0.6)
    r['abyss_anemone'] = simple('tube_coral', R('#0c3a48', '#14566a', '#1e7488', '#3496a8', '#5cc0cc', '#a0ecea'), 0.5)
    r['musical_cobweb'] = simple('cobweb', R('#3fa8b4', '#6cccd0', '#a6eae6', '#e4fffb'), 0.6)
    r['glowbell_vine'] = glowbell('cave_vines', False)
    r['glowbell_vine_lit'] = glowbell('cave_vines_lit', True)
    r['glowbell_vine_plant'] = glowbell('cave_vines_plant', False)
    r['glowbell_vine_plant_lit'] = glowbell('cave_vines_plant_lit', True)
    r['sculk_coral'] = simple('tube_coral', SCULK, 0.75)
    r['sculk_coral_fan'] = simple('tube_coral_fan', SCULK, 0.75)
    r['sculk_coral_block'] = simple('tube_coral_block', SCULK, 0.7)
    # ---- ores: vanilla's ore layouts, the rock mapped exactly as the stone they replace
    dream = dict(ref_stone='stone', stone_ramp=DREAM, stone_at=192)
    hush = dict(ref_stone='deepslate', stone_ramp=HUSH, stone_at=70, sat=0.2)
    r['siftite_ore'] = ore('diamond_ore', gem_ramp=SIFTITE, **dream)
    r['deep_siftite_ore'] = ore('deepslate_diamond_ore', gem_ramp=SIFTITE, **hush)
    r['prism_ore'] = ore('emerald_ore', gem_ramp=PRISM, **dream)
    r['deep_prism_ore'] = ore('deepslate_emerald_ore', gem_ramp=PRISM, **hush)
    r['bauxite_ore'] = ore('copper_ore', gem_ramp=BAUXITE, **dream)
    r['deep_bauxite_ore'] = ore('deepslate_copper_ore', gem_ramp=BAUXITE, **hush)
    r['galena_ore'] = ore('iron_ore', gem_ramp=GALENA, gem_at=0.6, **dream)
    r['deep_galena_ore'] = ore('deepslate_iron_ore', gem_ramp=GALENA, gem_at=0.85, **hush)
    coal = lambda o: lum(o) < 80  # noqa: E731 - coal's lumps are its dark texels
    r['egg_laden_ore'] = ore('coal_ore', gem_ramp=EGG, gem_at=0.6, gem_k=2.2, mineral=coal, **dream)
    r['deep_egg_laden_ore'] = ore('deepslate_coal_ore', gem_ramp=EGG, gem_at=0.6, gem_k=2.2, mineral=lambda o: lum(o) < 40,
                                  ref_stone='deepslate', stone_ramp=HUSH, stone_at=70)
    r['sculkite_ore'] = ore('deepslate_diamond_ore', ref_stone='deepslate', stone_ramp=SCULK_MUD, stone_at=38, sat=0.2, gem_ramp=SCULKITE)
    r['sculkite_block'] = simple('netherite_block', R('#03141a', '#082a33', '#0f4450', '#166470', '#228a94', '#46c4c4'), 0.45)
    r['siftite_block'] = simple('diamond_block', SIFTITE, 0.55)
    r['prism_block'] = simple('amethyst_block', PRISM, 0.5)
    r['pure_soul_block'] = simple('glowstone', SOUL, 0.55)
    r['glowing_slime_block'] = simple('slime_block', SLIME, 0.55)
    for c, ramp in (('amber', R('#6a2e08', '#90460c', '#b66214', '#d6822a', '#ecaa4c', '#f8d084', '#fff0c8')),
                    ('gold', R('#6e4a0e', '#986c16', '#c09424', '#dcb83c', '#f0d460', '#fcec9c', '#fffbe0')),
                    ('rose', R('#6e1a40', '#922856', '#b43c70', '#d0588a', '#e67ca6', '#f6a6c6', '#ffdcea')),
                    ('teal', R('#0c4e52', '#14686c', '#1e8686', '#30a6a2', '#52c6bc', '#86e2d6', '#d0fff6')),
                    ('violet', R('#3a2470', '#523490', '#6c4aae', '#8a66c8', '#aa8ade', '#cab0ee', '#efe2ff'))):
        r[f'music_crystal_{c}'] = simple('amethyst_block', ramp, 0.5)
        r[f'music_crystal_{c}_frozen'] = frozen_crystal(ramp)
    r['chrome_glass'] = simple('glass', R('#3a8fae', '#5ab0cc', '#8ad2e6', '#c6eef8', '#effcff'), 0.6)
    # ---- functional blocks (all on vanilla bodies)
    r['bulb_lantern'] = lantern
    r['sift_gate_frame_side'] = gate_frame('side')
    r['sift_gate_frame_top'] = gate_frame('top')
    r['echoer_hut_heart_side'] = hut_heart('side')
    r['echoer_hut_heart_top'] = hut_heart('top')
    r['echoer_hut_heart_top_spent'] = hut_heart('top', spent=True)
    for p in ('side', 'rim', 'bottom', 'top'):
        r[f'pitcher_planter_{p}'] = planter(p)
    for p in ('ball', 'barrel', 'carriage', 'muzzle'):
        r[f'ancient_cannon_{p}'] = cannon(p)
    r['soul_chime_core'] = simple('sea_lantern', R('#0b173a', '#11234e', '#1a3466', '#26487e', '#355e96', '#4a74a8', '#6890c0'), 0.4)
    r['soul_chime_core_lit'] = simple('sea_lantern', SOUL, 0.6)
    r['soul_chime_tube'] = simple('oxidized_copper', VERDIGRIS, 0.5)
    r['soul_chime_wood'] = simple('cherry_planks', WISH_WOOD, 176)
    r['europhy_table_side'] = europhy_side
    r['europhy_table_top'] = simple('lodestone_top', MAGNESITE, 0.55)
    r['europhy_table_base'] = simple('smooth_stone', MAGNESITE, 0.6)
    r['europhy_table_bottom'] = simple('smooth_stone', MAGNESITE, 0.45)
    r['europhy_table_column'] = simple('quartz_pillar_side', MAGNESITE, 0.55, 1.3)
    r['europhy_table_copper'] = simple('cut_copper', BRONZE, 0.55)
    r['europhy_table_prism'] = simple('amethyst_block', PRISM, 0.5)
    r['band_table_particle'] = simple('dark_oak_planks', DARK_WALNUT, 0.55)
    for p in ('top', 'side', 'bottom', 'inner'):
        r[f'sift_cake_{p}'] = cake(p)
    for p in ('top', 'side', 'bottom', 'fluff'):
        r[f'swifter_den_{p}'] = den(p)
    return r


def paint(tex_root):
    """gen_textures hook: rewrites every block texture in the recipe table, the drum's and the portal's;
    returns the names it painted."""
    block = os.path.join(tex_root, 'block')
    if not os.path.isdir(VANILLA):
        print('blockart: no vanilla textures at', VANILLA, '- block textures left as generated (the Sift Drum needs them)')
        return []
    old = {}
    for f in os.listdir(block):
        if f.endswith('.png'):
            old[f[:-4]] = np.asarray(Image.open(os.path.join(block, f)).convert('RGBA'), dtype=np.float64)
    done = []

    def save(name, a):
        Image.fromarray(np.clip(np.round(a), 0, 255).astype(np.uint8), 'RGBA').save(os.path.join(block, name + '.png'))
        done.append(name)

    for name, make in recipes(old).items():
        if name not in old:
            continue  # a block that no longer exists
        a = make()
        if a is not None:
            save(name, a)
    for name, a in drum_textures().items():
        save(name, a)
        meta = os.path.join(block, name + '.png.mcmeta')
        if os.path.exists(meta):
            os.remove(meta)
    for name in ('sift_drum_side_core', 'sift_drum_glow', 'sift_drum_head_glow'):  # MANSION: the drum's core window is gone
        for ext in ('.png', '.png.mcmeta'):
            if os.path.exists(os.path.join(block, name + ext)):
                os.remove(os.path.join(block, name + ext))
    if 'sift_portal' in old:
        save('sift_portal', portal())
        with open(os.path.join(block, 'sift_portal.png.mcmeta'), 'w') as f:
            json.dump({'animation': {'frametime': 2}, 'texture': {'mipmap_strategy': 'mean'}}, f, indent=2)
    stale = os.path.join(block, 'sift_drum.png')  # the old 32x drum sheet
    if os.path.exists(stale):
        os.remove(stale)
    return done
