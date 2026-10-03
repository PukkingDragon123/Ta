"""A3 Chrome: the rainbow Chrome fluid, Chime Sand and Chime Glass, the Chrome fish buckets and
the Rainbow Daze.

Hooked in from one line each: spec.py (declare), gen_assets.py (gen_block for the pane, assets),
gen_textures.py (textures). The Java side lives in registry/ModChrome.java, block/Chime*.java,
item/ChromeFishBucketItem.java, effect/RainbowDazeEffect.java, world/ChromeReactions.java and
client/ChromeClient.java.

How the moving colour works: Chrome's textures are silver ripples whose thin-film colour sweeps
once round the colour wheel per loop (smoothly interpolated), and the fluid's tint (baked per block
when a chunk is meshed, see ChromeClient.tint) is a slow rainbow gradient across the world. The
two multiply, so wherever the texture's hue lines up with the tint the surface lights up in vivid
colour, and those bands of colour sweep across every connected lake as the texture's hue turns.
"""
import colorsys
import math
import os
import random

import numpy as np
from PIL import Image

NS = 'thesift'
FISH = ('kazoo_fish', 'tubafish', 'fanfare_eel')
FISH_NAMES = {'kazoo_fish': 'Kazoo Fish', 'tubafish': 'Tubafish', 'fanfare_eel': 'Fanfare Eel'}
FISH_FOOD = {'kazoo_fish': 'ModFoods.KAZOO_FISH', 'tubafish': 'ModSeaFoods.TUBAFISH', 'fanfare_eel': 'ModSeaFoods.FANFARE_EEL'}
VANILLA = os.environ.get('MC_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures')

# one loop of the fluid's colour: 40 frames x 3 ticks = 6 seconds (ChromeClient.TINT_* and the fog
# hue are tuned to the same rhythm)
FRAMES = 40
FRAMETIME = 3


# ============================================================================ spec (registries)

def declare(block, item):
    block("chime_sand", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.QUARTZ)", cls="ChimeSandBlock", model="cube_all", tags=["shovel"], tab="nature")
    block("chime_glass", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).sound(com.thesift.block.ChimeGlassBlock.SOUND)",
          cls="ChimeGlassBlock", model="glass", loot="silk")
    block("chime_glass_pane", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS_PANE).sound(com.thesift.block.ChimeGlassBlock.SOUND)",
          cls="ChimeGlassPaneBlock", model="chime_pane", loot="silk")
    for f in FISH:
        # exactly like vanilla's fish buckets: one fish, its data kept in BUCKET_ENTITY_DATA
        item(f"chrome_{f}_bucket", cls="ChromeFishBucketItem",
             factory=f"p -> new ChromeFishBucketItem(ModEntities.{f.upper()}.get(), p)",
             props="new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA, "
                   f"net.minecraft.world.item.component.CustomData.EMPTY).component(net.minecraft.core.component.DataComponents.FOOD, {FISH_FOOD[f]})",
             name=f"Chrome Bucket of {FISH_NAMES[f]}")


# ============================================================================ assets + data (gen_assets)

def gen_block(GA, b):
    """The Chime Glass Pane: vanilla's glass pane blockstate and models, retextured."""
    GA.copy_template('glass_pane', b['id'], GA.exact_tex({'block/glass_pane_top': 'block/chime_glass_pane_top', 'block/glass': 'block/chime_glass'}))


PARTICLES = {'chrome_ripple': 1, 'chrome_spark': 3, 'chrome_chord': 1, 'rainbow_mote': 2}


def assets(GA):
    # Chime Sand smelts into Chime Glass, which makes panes - just like sand and glass
    GA.smelt('chime_glass', 'chime_sand', 'chime_glass', 0.1, 200, ('smelting',))
    GA.shaped('chime_glass_pane', ['###', '###'], {'#': 'chime_glass'}, 'chime_glass_pane', 16, 'building')
    GA.tag('block', 'minecraft:impermeable', GA.rl('chime_glass'))
    GA.tag('block', 'c:glass_blocks', GA.rl('chime_glass'))
    GA.tag('item', 'c:glass_blocks', GA.rl('chime_glass'))
    GA.tag('block', 'c:glass_panes', GA.rl('chime_glass_pane'))
    GA.tag('item', 'c:glass_panes', GA.rl('chime_glass_pane'))
    GA.tag('block', f'{NS}:resonant', GA.rl('chime_glass'))
    for p, n in PARTICLES.items():
        texs = [f'{NS}:{p}_{i}' if n > 1 else f'{NS}:{p}' for i in range(n)]
        GA.write(os.path.join(GA.A, 'particles', p + '.json'), {'textures': texs})
        for t in texs:
            GA.TEXTURES.add('particle/' + t.split(':')[1])
    GA.TEXTURES.add('mob_effect/rainbow_daze')
    GA.LANG[f'effect.{NS}.rainbow_daze'] = 'Rainbow Daze'
    post_effects(GA)


# ============================================================================ the daze's post effect

def post_effects(GA):
    """Rainbow Daze post effects at four strengths (the client steps through them as the effect
    fades in and out), all one shader: thesift:post/rainbow_daze."""
    for i in range(1, 5):
        GA.write(os.path.join(GA.A, 'post_effect', f'rainbow_daze_{i}.json'), {
            'targets': {'swap': {}},
            'passes': [
                {'vertex_shader': f'{NS}:post/rainbow_daze', 'fragment_shader': f'{NS}:post/rainbow_daze',
                 'inputs': [{'sampler_name': 'In', 'target': 'minecraft:main', 'bilinear': True}], 'output': 'swap',
                 'uniforms': {'DazeConfig': [{'name': 'Strength', 'type': 'float', 'value': i / 4.0}]}},
                {'vertex_shader': 'minecraft:core/screenquad', 'fragment_shader': 'minecraft:post/blit',
                 'inputs': [{'sampler_name': 'In', 'target': 'swap'}], 'output': 'minecraft:main',
                 'uniforms': {'BlitConfig': [{'name': 'ColorModulate', 'type': 'vec4', 'value': [1.0, 1.0, 1.0, 1.0]}]}},
            ]})
    sh = os.path.join(GA.A, 'shaders', 'post')
    os.makedirs(sh, exist_ok=True)
    with open(os.path.join(sh, 'rainbow_daze.vsh'), 'w') as f:
        f.write(VSH)
    with open(os.path.join(sh, 'rainbow_daze.fsh'), 'w') as f:
        f.write(FSH)


VSH = """#version 330
#extension GL_ARB_separate_shader_objects : require

// A3 Chrome: the Rainbow Daze. One triangle that covers the screen (drawn as 3 vertices, no buffer).
layout(location = 0) out vec2 texCoord;

void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = uv;
}
"""

FSH = """#version 330
#extension GL_ARB_separate_shader_objects : require

// A3 Chrome: the Rainbow Daze. The world swims gently, splits into soft rainbow fringes and its
// colours slowly turn round the colour wheel, strongest towards the edges of your sight.
uniform sampler2D InSampler;

// the game's global settings block (GlobalSettingsUniform, std140)
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};

layout(std140) uniform DazeConfig {
    float Strength;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

vec3 hueRotate(vec3 c, float a) {
    // rotation about the grey axis: keeps brightness, turns the hue
    const vec3 k = vec3(0.57735);
    float ca = cos(a);
    return c * ca + cross(k, c) * sin(a) + k * dot(k, c) * (1.0 - ca);
}

void main() {
    // GameTime is the fraction of the day (24000 ticks); 1200 per second. Every rate below is a whole
    // number of cycles per day so nothing jumps when the day wraps.
    float t = GameTime * 1200.0;
    vec2 centred = texCoord - 0.5;
    float edge = smoothstep(0.08, 0.75, length(centred * vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0)));
    float s = Strength;
    // a slow, seasick swim of the whole image
    vec2 wave = vec2(sin(texCoord.y * 9.0 + t * TAU / 4.0), cos(texCoord.x * 7.0 + t * TAU / 5.0)) * 0.0045 * s;
    vec2 uv = texCoord + wave * (0.4 + edge);
    // rainbow fringes: red and blue pulled apart along the swirl
    vec2 split = vec2(cos(t * TAU / 6.0), sin(t * TAU / 6.0)) * (0.0015 + 0.0045 * edge) * s;
    vec3 c;
    c.r = texture(InSampler, uv + split).r;
    c.g = texture(InSampler, uv).g;
    c.b = texture(InSampler, uv - split).b;
    // the colours turn, more at the edges, in a band that sweeps across the screen
    float sweep = sin(dot(centred, vec2(3.0, 2.0)) * 3.0 - t * TAU / 3.0) * 0.5 + 0.5;
    c = mix(c, hueRotate(c, t * TAU / 6.0 + centred.x * 2.5), clamp(s * (0.35 + 0.45 * edge + 0.2 * sweep), 0.0, 1.0));
    // a little extra saturation for that candy-coloured haze
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(grey), c, 1.0 + 0.35 * s);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""


# ============================================================================ textures (gen_textures)

def hsv(h, s, v, a=255):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0.0, min(1.0, s)), max(0.0, min(1.0, v)))
    return (round(r * 255), round(g * 255), round(b * 255), a)


def strip(frames):
    w, h = frames[0].size
    img = Image.new('RGBA', (w, h * len(frames)), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        img.paste(f, (0, i * h))
    return img


def anim(frametime, interpolate=True):
    return {'animation': {'frametime': frametime, 'interpolate': interpolate}}


def _ripple(x, y, t, n=16):
    # integer spatial and temporal frequencies, so the texture tiles and the animation loops
    k = math.tau / n
    return math.sin(k * (x + y) + t) + 0.6 * math.sin(k * (x - 2 * y) - t) + 0.35 * math.sin(k * (3 * x + y) + 2 * t)


def _chrome_px(x, y, f, n=16, alpha=150):  # W1: more see-through (was 220)
    t = f / FRAMES * math.tau
    w = _ripple(x, y, t, n)
    # liquid metal: silver ripples, a step brighter on every crest (8 tones, pixel-art banding)
    v = 0.66 + 0.24 * (w + 1.95) / 3.9
    v = round(v * 16) / 16
    # thin-film colour: the hue sweeps once round the wheel per loop, bent a little by the ripples
    h = f / FRAMES + 0.05 * w + 0.03 * math.sin(math.tau / n * (2 * x - y) + t)
    s = 0.5 - 0.1 * max(0.0, w - 0.5)
    if w > 1.6 and (x * 7 + y * 5 + f * 3) % 13 == 0:
        return hsv(h, 0.08, 1.0, alpha)  # a glint
    return hsv(h, s, v, alpha)


def fluid(out):
    frames = []
    for f in range(FRAMES):
        img = Image.new('RGBA', (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                px[x, y] = _chrome_px(x, y, f)
        frames.append(img)
    out('block/chrome_still', strip(frames), anim(FRAMETIME))
    # the flow scrolls two blocks' worth along the stream per loop (the renderer shows half of it)
    frames = []
    for f in range(FRAMES):
        img = Image.new('RGBA', (32, 32))
        px = img.load()
        for y in range(32):
            for x in range(32):
                px[x, y] = _chrome_px(x, y - 32.0 * f / FRAMES, f)
        frames.append(img)
    out('block/chrome_flow', strip(frames), anim(FRAMETIME))
    ov = Image.new('RGBA', (16, 16))
    px = ov.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _chrome_px(x, y, 0, alpha=170)
    out('block/chrome_overlay', ov)
    # seen through your eyes when you sink in: a soft opal swirl (drawn at 10% by the game)
    misc = Image.new('RGBA', (64, 64))
    px = misc.load()
    for y in range(64):
        for x in range(64):
            k = math.tau / 64
            w = math.sin(k * (x + y)) + math.sin(k * (2 * x - y) + 1.3) * 0.7 + math.sin(k * (x - 3 * y) + 0.4) * 0.4
            px[x, y] = hsv(0.5 + w * 0.22 + (x + y) / 128, 0.45, 0.9 + 0.05 * w)
    out('misc/in_chrome', misc)


def daze_overlays(out):
    """The Rainbow Daze on your screen: a white vignette (tinted in-game as its hue turns) and a
    rainbow swirl that slowly rotates round the edge of your sight."""
    n = 256
    yy, xx = np.mgrid[0:n, 0:n]
    dx = (xx + 0.5) / n * 2 - 1
    dy = (yy + 0.5) / n * 2 - 1
    r = np.sqrt(dx * dx + dy * dy)
    a = np.clip((r - 0.42) / 0.78, 0, 1) ** 1.7
    vig = np.zeros((n, n, 4), dtype=np.uint8)
    vig[..., :3] = 255
    vig[..., 3] = (a * 255).astype(np.uint8)
    out('misc/rainbow_daze_vignette', Image.fromarray(vig, 'RGBA'))
    th = np.arctan2(dy, dx)
    sw = np.zeros((n, n, 4), dtype=np.uint8)
    band = (0.5 + 0.5 * np.sin(5 * th + r * 13.0)) ** 2.2
    alpha = band * np.clip((r - 0.32) / 0.55, 0, 1) * (1 - np.clip((r - 1.25) / 0.2, 0, 1)) * 190
    for y in range(n):
        for x in range(n):
            c = hsv(th[y, x] / math.tau + r[y, x] * 0.9, 0.6, 1.0)
            sw[y, x] = (c[0], c[1], c[2], int(alpha[y, x]))
    out('misc/rainbow_daze_swirl', Image.fromarray(sw, 'RGBA'))


def _sand():
    """Chime Sand: pale pearly grains, like vanilla sand in lilac-white, with prismatic flecks."""
    rnd = random.Random(31)
    base = ['#bdb2dc', '#cbc2e6', '#d8d1ee', '#e4dff4', '#efebf9']
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    k = math.tau / 16
    for y in range(16):
        for x in range(16):
            n = (math.sin(k * (x * 2 + y) + 1.1) + math.sin(k * (x - y * 3) + 0.3) * 0.7 + math.sin(k * (5 * x + 2 * y)) * 0.35) / 2.05
            n += (rnd.random() - 0.5) * 0.9
            i = max(0, min(4, int((n + 1.0) / 2.0 * 5)))
            px[x, y] = tuple(int(base[i][j:j + 2], 16) for j in (1, 3, 5)) + (255,)
    for (x, y) in ((3, 2), (11, 5), (6, 9), (13, 12), (1, 13), (9, 14), (14, 1)):
        px[x, y] = (0xa3, 0x96, 0xc8, 255)  # darker grains
    flecks = [('#7fe3ff', '#4fb4d8'), ('#ff9fd6', '#d070a8'), ('#ffe27a', '#d0a840'), ('#9ff2c4', '#5cc08e'), ('#c4a8ff', '#8f72d8')]
    for i, (x, y) in enumerate(((2, 4), (9, 2), (13, 8), (5, 12), (10, 11), (7, 6), (1, 9), (14, 14))):
        hi, lo = flecks[i % len(flecks)]
        px[x, y] = tuple(int(hi[j:j + 2], 16) for j in (1, 3, 5)) + (255,)
        px[(x + 1) % 16, (y + 1) % 16] = tuple(int(lo[j:j + 2], 16) for j in (1, 3, 5)) + (255,)
    return img


def _glass(f, n_frames):
    """Chime Glass: an opal frame whose colours chase round the edge, a faint pearly pane and
    vanilla's two highlight streaks."""
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    ph = f / n_frames
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                # position round the frame, 0..1, so the colours chase clockwise
                if y == 0:
                    p = x / 60
                elif x == 15:
                    p = (15 + y) / 60
                elif y == 15:
                    p = (30 + 15 - x) / 60
                else:
                    p = (45 + 15 - y) / 60
                lit = x == 0 or y == 0
                px[x, y] = hsv(p + ph, 0.32, 0.98 if lit else 0.86, 235)
            else:
                d = (x + y) / 30
                px[x, y] = hsv(d * 0.5 + ph + 0.5, 0.22, 1.0, 44 + int(18 * (0.5 + 0.5 * math.sin(math.tau * (d - ph)))))
    for (x, y) in ((4, 2), (3, 3), (2, 4), (13, 12), (12, 13)):
        px[x, y] = (255, 255, 255, 215)
    return img


def _pane_top():
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        px[7, y] = hsv(y / 16, 0.3, 0.98, 235)
        px[8, y] = hsv(y / 16 + 0.08, 0.32, 0.84, 235)
    return img


def chime_blocks(out):
    out('block/chime_sand', _sand())
    n = 16
    out('block/chime_glass', strip([_glass(f, n) for f in range(n)]), anim(6))
    out('block/chime_glass_pane_top', _pane_top())


def particles(out):
    # ripple: a soft one-pixel ring, white so each particle can take its own hue
    n = 16
    ring = Image.new('RGBA', (n, n))
    px = ring.load()
    for y in range(n):
        for x in range(n):
            d = math.hypot(x + 0.5 - n / 2, y + 0.5 - n / 2)
            a = max(0.0, 1.0 - abs(d - 6.2) / 1.1)
            if a > 0:
                px[x, y] = (255, 255, 255, int(255 * a))
    out('particle/chrome_ripple', ring)
    # sparks: little four-point twinkles in three sizes
    for i, size in enumerate((1, 2, 3)):
        sp = Image.new('RGBA', (8, 8))
        p = sp.load()
        p[3, 3] = p[4, 3] = p[3, 4] = p[4, 4] = (255, 255, 255, 255)
        for k in range(1, size + 1):
            a = 255 if k < size else 150
            for (x, y) in ((3, 3 - k), (4, 3 - k), (3, 4 + k), (4, 4 + k), (3 - k, 3), (3 - k, 4), (4 + k, 3), (4 + k, 4)):
                if 0 <= x < 8 and 0 <= y < 8:
                    p[x, y] = (255, 255, 255, a)
        out(f'particle/chrome_spark_{i}', sp)
    # the chord burst is an emitter and never drawn; it still needs a sprite
    dot = Image.new('RGBA', (8, 8))
    dot.load()[3, 3] = (255, 255, 255, 255)
    out('particle/chrome_chord', dot)
    # motes: a soft round glow, and a glow with a twinkle
    for i in range(2):
        m = Image.new('RGBA', (8, 8))
        p = m.load()
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - 3.5, y - 3.5)
                a = max(0.0, 1.0 - d / (3.2 if i == 0 else 2.4)) ** 1.5
                if i == 1 and (x in (3, 4) or y in (3, 4)) and d < 3.6:
                    a = max(a, 0.75)
                if a > 0.02:
                    p[x, y] = (255, 255, 255, int(255 * a))
        out(f'particle/rainbow_mote_{i}', m)


def effect_icon(out):
    """18x18 like every vanilla mob effect icon: a rainbow swirl spinning out from a white spark,
    outlined in dark violet."""
    n = 18
    cells = {}
    cx = cy = 8.5
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - cx - 0.5, y + 0.5 - cy - 0.5
            r = math.hypot(dx, dy)
            th = (math.atan2(dy, dx)) % math.tau
            for turn in range(3):
                ang = th + turn * math.tau
                rs = 1.0 + ang * 0.62
                if abs(r - rs) < 0.75 and rs < 8.2:
                    cells[(x, y)] = ang / (2.6 * math.tau)
    img = Image.new('RGBA', (n, n))
    px = img.load()
    for (x, y), t in cells.items():
        px[x, y] = hsv(0.0 + t * 0.85, 0.75, 1.0)
    ol = (0x2a, 0x16, 0x46, 255)
    for y in range(n):
        for x in range(n):
            if (x, y) in cells:
                continue
            if any((x + dx, y + dy) in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = ol
    for (x, y) in ((8, 8), (9, 9)):
        px[x, y] = (255, 255, 255, 255)
    out('mob_effect/rainbow_daze', img)


# ---------------------------------------------------------------------------- items

BUCKET = [
    '................',
    '.....aaaaaa.....',
    '...aabcccddaa...',
    '..abbeeebbbcba..',
    '..aefffebbbbea..',
    '..aaaffeebbaaa..',
    '..agdaaaaaabca..',
    '..aggggddcbbca..',
    '..agghgddcbbda..',
    '..adghgddcbbda..',
    '..abghgddcbbca..',
    '...agggddcbca...',
    '...adggdccbca...',
    '....adgdcbca....',
    '.....aaaaaa.....',
    '................',
]
BUCKET_PAL = {'a': '#353535', 'b': '#727272', 'c': '#969696', 'd': '#a8a8a8', 'e': '#5f5f5f', 'f': '#545454', 'g': '#d8d8d8', 'h': '#ffffff'}
# the Chrome in the bucket's mouth (vanilla water_bucket's water pixels), shade 0 dark .. 4 light
SURFACE = {(4, 3): 0, (5, 3): 1, (6, 3): 3, (7, 3): 2, (8, 3): 2, (9, 3): 1, (10, 3): 1, (11, 3): 0,
           (3, 4): 0, (4, 4): 3, (5, 4): 4, (6, 4): 4, (7, 4): 3, (8, 4): 2, (9, 4): 4, (10, 4): 2, (11, 4): 3, (12, 4): 0,
           (5, 5): 0, (6, 5): 2, (7, 5): 3, (8, 5): 3, (9, 5): 2, (10, 5): 0}
# what spills over the rim and runs down the side when a fish is in it (vanilla cod_bucket's drips)
SPILL = {(5, 6): 3, (6, 6): 2, (7, 6): 3, (8, 6): 3, (9, 6): 2, (10, 6): 0, (7, 7): 2, (8, 7): 0, (8, 8): 2, (8, 9): 2, (8, 11): 2,
         (4, 7): 1, (4, 8): 0}
SHADE_V = (0.62, 0.72, 0.82, 0.92, 1.0)

FISH_ART = {
    'kazoo_fish': ([
        '......oOo.......',
        '...tTTTTTTt..oO.',
        'kKtwTsTTsTTTtoOo',
        'kKteTsTTsTTTtto.',
        '..tbTTTTTTTbtoO.',
        '....bo...ob..o..',
        '................',
    ], {'o': '#f07a2a', 'O': '#ffb05a', 't': '#2a9a98', 'T': '#5ad0c8', 's': '#f08a3a', 'b': '#155a62', 'e': '#0e1418', 'w': '#ffffff',
        'k': '#c8962e', 'K': '#ffe08a'}),
    'tubafish': ([
        '......gGGg......',
        '.......gg.......',
        '....bBBBBBBb....',
        '..lLBwBsBBsBLl..',
        '.lLbBeBBBsBBbLl.',
        '..lbBsBBsBBBbl..',
        '....bpppppppb...',
    ], {'b': '#5d86cc', 'B': '#78a5e3', 's': '#3ff5e6', 'p': '#ffe6ef', 'e': '#1a1420', 'w': '#ffffff', 'L': '#c96a8c', 'l': '#f59ab8',
        'g': '#b8923a', 'G': '#f2d27a'}),
    'fanfare_eel': ([
        '.GgG............',
        'GhhhG.bbbbb.....',
        'ghDhgbHrHrHbb...',
        '.gGgbHHfHHfHHb..',
        '....bcc...bHHb..',
        '...........bHb..',
        '............b...',
    ], {'b': '#0c1a24', 'H': '#24485a', 'r': '#e3ddcc', 'c': '#16303e', 'f': '#3ff5e6', 'G': '#f2d27a', 'g': '#b8923a', 'h': '#fff0b0',
        'D': '#0a1a22'}),
}


def _hex(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def _chrome_item_px(x, y, shade, f, n_frames):
    h = f / n_frames + x * 0.055 + y * 0.03 + shade * 0.02
    return hsv(h, 0.55 - shade * 0.06, SHADE_V[shade])


def _fish_layer(fish):
    import items16  # the shared 16x16 sprite painter (outlines in the vanilla item style)
    rows, pal = FISH_ART[fish]
    return items16.grid(rows, pal, ol=True, size=(16, len(rows)))


def bucket_frames(fish=None, n_frames=8):
    frames = []
    layer = _fish_layer(fish) if fish else None
    for f in range(n_frames):
        img = Image.new('RGBA', (16, 16))
        px = img.load()
        for y, row in enumerate(BUCKET):
            for x, ch in enumerate(row):
                if ch != '.':
                    px[x, y] = _hex(BUCKET_PAL[ch])
        chrome = dict(SURFACE)
        if fish:
            chrome.update(SPILL)
        for (x, y), shade in chrome.items():
            px[x, y] = _chrome_item_px(x, y, shade, f, n_frames)
        if not fish:
            px[6, 4] = (255, 255, 255, 255)  # a glint on the surface
        if layer:
            img.alpha_composite(layer, (0, 0))
        frames.append(img)
    return frames


def items(out):
    # the Chrome Bucket (replaces items16's old pearl-only bucket) and the three fish buckets; the
    # Chrome in them slowly turns through the rainbow like the real thing
    out('item/chrome_bucket', strip(bucket_frames()), anim(4))
    for f in FISH:
        out(f'item/chrome_{f}_bucket', strip(bucket_frames(f)), anim(4))


def textures(out):
    fluid(out)
    daze_overlays(out)
    chime_blocks(out)
    particles(out)
    effect_icon(out)
    items(out)
