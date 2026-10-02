"""B1 Portal & sky FX: the gate's awakening post effect (thesift:post/gate_warp), the textures seen
through the portal's sky window, and the Sift sky's rainbows, aurora ribbons, colour clouds and
shooting stars.

Hooked from gen_assets.py (assets) and gen_textures.py (textures) with one line each.
"""
import colorsys
import math
import os
import random

import numpy as np
from PIL import Image

NS = 'thesift'

# ============================================================================ post effect

WARP_STEPS = 6
SHOCK_STEPS = 8


def assets(GA):
    """gate_warp_1..6: the world bends harder and harder as the gate gathers light;
    gate_shock_1..8: the shockwave ring rolling out of the flash. One shader for all of them."""
    def chain(name, strength, shock):
        GA.write(os.path.join(GA.A, 'post_effect', name + '.json'), {
            'targets': {'swap': {}},
            'passes': [
                {'vertex_shader': f'{NS}:post/gate_warp', 'fragment_shader': f'{NS}:post/gate_warp',
                 'inputs': [{'sampler_name': 'In', 'target': 'minecraft:main', 'bilinear': True}], 'output': 'swap',
                 'uniforms': {'WarpConfig': [{'name': 'Strength', 'type': 'float', 'value': round(strength, 4)},
                                             {'name': 'Shock', 'type': 'float', 'value': round(shock, 4)}]}},
                {'vertex_shader': 'minecraft:core/screenquad', 'fragment_shader': 'minecraft:post/blit',
                 'inputs': [{'sampler_name': 'In', 'target': 'swap'}], 'output': 'minecraft:main',
                 'uniforms': {'BlitConfig': [{'name': 'ColorModulate', 'type': 'vec4', 'value': [1.0, 1.0, 1.0, 1.0]}]}},
            ]})
    for i in range(1, WARP_STEPS + 1):
        chain(f'gate_warp_{i}', i / WARP_STEPS, 0.0)
    for i in range(1, SHOCK_STEPS + 1):
        chain(f'gate_shock_{i}', 0.7 * (1.0 - (i - 1) / SHOCK_STEPS), i / SHOCK_STEPS)
    sh = os.path.join(GA.A, 'shaders', 'post')
    os.makedirs(sh, exist_ok=True)
    with open(os.path.join(sh, 'gate_warp.vsh'), 'w') as f:
        f.write(VSH)
    with open(os.path.join(sh, 'gate_warp.fsh'), 'w') as f:
        f.write(FSH)
    GA.LANG[f'codex.{NS}.sift_sky.title'] = 'The Dreaming Sky'
    GA.LANG[f'codex.{NS}.sift_sky.tagline'] = 'Rainbows, ribbons and falling stars'
    GA.LANG[f'codex.{NS}.sift_sky.body'] = (
        "The Sift's sky keeps its own slow dream cycle. As each cycle dawns and fades, a soft rainbow stands over the horizon, "
        "and a brighter double bow follows every passing rain. Pastel clouds of rose, lilac, peach and mint drift high above. "
        "Deep in the cycle's blue hour, ribbons of violet and emerald light ripple across the sky and shooting stars streak "
        "past - make a wish. Fireflies of glow dust rise from the grass then, and rainbow motes hang in the air beneath a bow.")


VSH = """#version 330
#extension GL_ARB_separate_shader_objects : require

// B1 Portal & sky FX: the gate's awakening. One triangle that covers the screen (3 vertices, no buffer).
layout(location = 0) out vec2 texCoord;

void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = uv;
}
"""

FSH = """#version 330
#extension GL_ARB_separate_shader_objects : require

// B1 Portal & sky FX: the gate's awakening. The camera faces the gate, so the gate is the middle of
// the screen: the world twists and is drawn in towards it, the air shimmers like heat, colours split
// and turn cyan as the light gathers. After the flash a ring of bent light rolls out to the edges.
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

layout(std140) uniform WarpConfig {
    float Strength;
    float Shock;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;

void main() {
    // GameTime is the fraction of the day; x1200 gives seconds. Every period divides 1200 s.
    float t = GameTime * 1200.0;
    float aspect = ScreenSize.x / max(ScreenSize.y, 1.0);
    vec2 p = (texCoord - 0.5) * vec2(aspect, 1.0);
    float r = length(p);
    vec2 dir = r > 0.0001 ? p / r : vec2(0.0, 1.0);
    float s = Strength;
    // 1. a twist round the gate, strongest close to it, breathing in and out
    float twist = s * (0.32 + 0.08 * sin(t * TAU / 3.0)) * exp(-r * 2.6);
    float ct = cos(twist);
    float st = sin(twist);
    vec2 q = vec2(p.x * ct - p.y * st, p.x * st + p.y * ct);
    // 2. the world is drawn in towards the gate
    q *= 1.0 - s * 0.10 * exp(-r * 2.2);
    // 3. heat shimmer
    q += vec2(sin(p.y * 38.0 + t * TAU / 2.0), cos(p.x * 33.0 + t * TAU / 3.0)) * 0.0032 * s;
    // 4. the shockwave: a ring of bent light rolling outwards
    float ring = 0.0;
    if (Shock > 0.0) {
        float d = r - Shock * 1.15;
        ring = exp(-d * d / 0.0045) * (1.0 - Shock * 0.8);
        q += dir * ring * 0.055;
    }
    vec2 uv = q / vec2(aspect, 1.0) + 0.5;
    // colours split along the radius
    vec2 split = dir / vec2(aspect, 1.0) * (0.0035 * s + 0.018 * ring);
    vec3 c;
    c.r = texture(InSampler, uv + split).r;
    c.g = texture(InSampler, uv).g;
    c.b = texture(InSampler, uv - split).b;
    // cyan light gathers: the whole picture cools, the gate's heart glows
    vec3 cyan = vec3(0.45, 1.0, 0.95);
    c = mix(c, c * vec3(0.78, 1.06, 1.12) + vec3(0.0, 0.05, 0.06), clamp(s * 0.7, 0.0, 1.0));
    c += cyan * (exp(-r * 4.0) * s * 0.32 + ring * 0.45);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""


# ============================================================================ textures

def _img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGBA')


def _noise(size, octaves, seed):
    """Tileable value noise in [0, 1] (sum of wrapped, smoothly upscaled random grids)."""
    rng = np.random.default_rng(seed)
    acc = np.zeros((size, size))
    amp, total = 1.0, 0.0
    for o in range(octaves):
        cells = 4 * 2 ** o
        grid = rng.random((cells, cells))
        ys = np.arange(size) * cells / size
        y0 = np.floor(ys).astype(int)
        fy = ys - y0
        fy = fy * fy * (3 - 2 * fy)
        x0, fx = y0, fy
        g00 = grid[np.ix_(y0 % cells, x0 % cells)]
        g01 = grid[np.ix_(y0 % cells, (x0 + 1) % cells)]
        g10 = grid[np.ix_((y0 + 1) % cells, x0 % cells)]
        g11 = grid[np.ix_((y0 + 1) % cells, (x0 + 1) % cells)]
        top = g00 + (g01 - g00) * fx[None, :]
        bot = g10 + (g11 - g10) * fx[None, :]
        acc += amp * (top + (bot - top) * fy[:, None])
        total += amp
        amp *= 0.5
    return acc / total


def gate_sky():
    """What the portal looks out on, straight up to straight down: deep turquoise overhead, a mint and
    rose glow at the horizon, violet dusk below. Faint nebula bands run across it (tiles in u)."""
    w, h = 128, 256
    n = _noise(128, 4, 11)
    n = np.vstack([n, n])
    arr = np.zeros((h, w, 4))
    stops = [(0.0, (14, 92, 120)), (0.28, (40, 170, 186)), (0.46, (150, 236, 222)), (0.52, (255, 214, 236)),
             (0.62, (186, 150, 236)), (1.0, (40, 22, 82))]
    for y in range(h):
        v = y / (h - 1)
        for i in range(len(stops) - 1):
            if stops[i][0] <= v <= stops[i + 1][0]:
                k = (v - stops[i][0]) / (stops[i + 1][0] - stops[i][0])
                k = k * k * (3 - 2 * k)
                col = np.array(stops[i][1]) * (1 - k) + np.array(stops[i + 1][1]) * k
                break
        band = n[y, :] - 0.5
        arr[y, :, 0] = col[0] + band * 40
        arr[y, :, 1] = col[1] + band * 30
        arr[y, :, 2] = col[2] + band * 46
    arr[..., 3] = 255
    return _img(arr)


def gate_stars(seed, count):
    """Black with stars (it is added on top): white, a few cyan and rose, some with a cross glint."""
    s = 256
    arr = np.zeros((s, s, 4))
    arr[..., 3] = 255
    rnd = random.Random(seed)
    for _ in range(count):
        x, y = rnd.randrange(s), rnd.randrange(s)
        b = rnd.uniform(0.35, 1.0)
        col = rnd.choice([(255, 255, 255), (255, 255, 255), (170, 250, 255), (255, 200, 236), (255, 244, 200)])
        big = rnd.random() < 0.08
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = abs(dx) + abs(dy)
                k = 1.0 if d == 0 else (0.45 if d == 1 else (0.22 if big and (dx == 0 or dy == 0) else 0.0))
                if not big and d > 0:
                    k *= 0.5
                xx, yy = (x + dx) % s, (y + dy) % s
                for c in range(3):
                    arr[yy, xx, c] = min(255, arr[yy, xx, c] + col[c] * b * k)
    return _img(arr)


def gate_clouds(seed):
    """Soft billowing cloud light on black (added on top), tileable."""
    n = _noise(256, 5, seed)
    v = np.clip((n - 0.42) * 2.6, 0, 1) ** 1.6
    arr = np.zeros((256, 256, 4))
    arr[..., 0] = v * 255
    arr[..., 1] = v * 255
    arr[..., 2] = v * 255
    arr[..., 3] = 255
    return _img(arr)


def gate_ripple():
    """The membrane itself: thin bright caustic lines on black, tileable."""
    n1 = _noise(128, 3, 21)
    n2 = _noise(128, 3, 22)
    lines = np.abs(np.sin((n1 * 2.0 + n2) * math.tau * 2.0))
    v = np.clip(1.0 - lines * 5.0, 0, 1) ** 2
    arr = np.zeros((128, 128, 4))
    arr[..., 0] = v * 200
    arr[..., 1] = v * 255
    arr[..., 2] = v * 250
    arr[..., 3] = 255
    return _img(arr)


def rainbow(secondary=False):
    """A rainbow band across v (red outside), soft at both edges; the same all along u."""
    w, h = 8, 64
    arr = np.zeros((h, w, 4))
    for y in range(h):
        v = y / (h - 1)
        hue = (0.0 + v * 0.78) if not secondary else (0.78 - v * 0.78)
        r, g, b = colorsys.hsv_to_rgb(hue, 0.62, 1.0)
        edge = math.sin(v * math.pi) ** 1.5
        arr[y, :, :3] = (r * 255, g * 255, b * 255)
        arr[y, :, 3] = 255 * edge
    return _img(arr)


def aurora():
    """A curtain of light: rays along u (tiles), violet fading out at the top, emerald and cyan below,
    a bright hem at the bottom that fades away."""
    w, h = 256, 64
    rng = np.random.default_rng(5)
    rays = np.zeros(w)
    for _ in range(40):
        c = rng.uniform(0, w)
        wd = rng.uniform(1.5, 7)
        a = rng.uniform(0.3, 1.0)
        x = np.arange(w)
        d = np.minimum(np.abs(x - c), w - np.abs(x - c))
        rays += a * np.exp(-(d / wd) ** 2)
    rays = 0.2 + 0.8 * rays / rays.max()
    arr = np.zeros((h, w, 4))
    for y in range(h):
        v = y / (h - 1)  # 0 top, 1 bottom
        top = np.array((214, 80, 230))
        mid = np.array((40, 230, 120))
        hem = np.array((190, 255, 250))
        col = top * (1 - v) + mid * v if v < 0.8 else mid * (1 - (v - 0.8) / 0.2) + hem * ((v - 0.8) / 0.2)
        rise = min(1.0, v / 0.6)
        alpha = (rise * rise * (3 - 2 * rise)) * (1.0 - max(0.0, (v - 0.88) / 0.12)) * 245
        arr[y, :, :3] = col
        arr[y, :, 3] = alpha * rays
    return _img(arr)


def colour_clouds():
    """Four soft pastel-white clouds in a 2x2 atlas (the sky renderer tints each one)."""
    s = 128
    img = np.zeros((s * 2, s * 2, 4))
    for cell in range(4):
        rng = np.random.default_rng(40 + cell)
        n = _noise(s, 4, 60 + cell)
        yy, xx = np.mgrid[0:s, 0:s]
        mask = np.zeros((s, s))
        for _ in range(6):
            cx, cy = rng.uniform(0.25, 0.75) * s, rng.uniform(0.4, 0.6) * s
            rx, ry = rng.uniform(0.14, 0.26) * s, rng.uniform(0.10, 0.18) * s
            mask = np.maximum(mask, 1 - ((xx - cx) / rx) ** 2 - ((yy - cy) / ry) ** 2)
        a = np.clip(mask * 1.6 + (n - 0.5) * 0.9, 0, 1) ** 1.3
        shade = 0.82 + 0.18 * np.clip(1 - (yy / s), 0, 1)
        ox, oy = (cell % 2) * s, (cell // 2) * s
        img[oy:oy + s, ox:ox + s, 0] = 255 * shade
        img[oy:oy + s, ox:ox + s, 1] = 255 * shade
        img[oy:oy + s, ox:ox + s, 2] = 255 * shade
        img[oy:oy + s, ox:ox + s, 3] = a * 210
    return _img(img)


def shooting_star():
    """A streak: a hot white head at the right end and a cyan tail fading to nothing on the left."""
    w, h = 128, 8
    arr = np.zeros((h, w, 4))
    for x in range(w):
        u = x / (w - 1)
        for y in range(h):
            d = abs(y - (h - 1) / 2) / ((h - 1) / 2)
            core = max(0.0, 1 - d * (1.4 - 0.6 * u))
            a = (u ** 2.2) * core
            head = math.exp(-((1 - u) * 18) ** 2)
            arr[y, x, 0] = 160 + 95 * max(head, u ** 3)
            arr[y, x, 1] = 240 + 15 * head
            arr[y, x, 2] = 255
            arr[y, x, 3] = min(255, 255 * (a + head * (1 - d)))
    return _img(arr)


def textures(out):
    out('entity/sift_gate/sky', gate_sky())
    out('entity/sift_gate/stars', gate_stars(3, 220))
    out('entity/sift_gate/clouds', gate_clouds(7))
    out('entity/sift_gate/ripple', gate_ripple())
    out('environment/sift_rainbow', rainbow())
    out('environment/sift_rainbow_outer', rainbow(True))
    out('environment/sift_aurora', aurora())
    out('environment/sift_colour_clouds', colour_clouds())
    out('environment/sift_shooting_star', shooting_star())
