"""Tiny entity-model toolkit.

Models are described once in Python (parts, pivots, cubes and how each cube is painted). From that
single description we generate:
  * the Java LayerDefinition code (with texture offsets packed automatically),
  * hand-shaded pixel-art textures (+ emissive layers and palette variants),
  * preview renders (a small software rasteriser) so poses can be checked without the game.

Coordinates follow vanilla entity models: 1 unit = 1/16 block, Y points DOWN, ground is y = 24,
the mob faces -Z (north).
"""
from __future__ import annotations

import math
import random
from dataclasses import dataclass, field
from typing import Callable, Optional

from PIL import Image, ImageDraw

# --------------------------------------------------------------------------- colours


def hex_rgb(h: str) -> tuple:
    h = h.lstrip('#')
    if len(h) == 6:
        return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), int(h[6:8], 16))


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(round(v))))


def shade(c, f: float):
    """f > 0 lightens towards white, f < 0 darkens (with a cool shadow hue like Mojang art)."""
    r, g, b, a = c
    if f >= 0:
        return (clamp(r + (255 - r) * f), clamp(g + (255 - g) * f), clamp(b + (255 - b) * f), a)
    f = -f
    # shadows drift slightly towards blue/purple
    return (clamp(r * (1 - f) + 30 * f * 0.4), clamp(g * (1 - f) + 20 * f * 0.3), clamp(b * (1 - f * 0.8) + 60 * f * 0.3), a)


def mix(c1, c2, t: float):
    return tuple(clamp(c1[i] * (1 - t) + c2[i] * t) for i in range(4))


# --------------------------------------------------------------------------- model description


@dataclass
class Cube:
    origin: tuple
    size: tuple
    paint: dict = field(default_factory=dict)
    inflate: float = 0.0
    uv: Optional[tuple] = None
    part: Optional['Part'] = None

    @property
    def footprint(self):
        w, h, d = (int(math.ceil(s)) for s in self.size)
        return (2 * d + 2 * w, d + h)

    def faces(self):
        """Texture rectangles (x, y, w, h) of each face, vanilla box-UV layout."""
        u, v = self.uv
        w, h, d = (int(math.ceil(s)) for s in self.size)
        return {
            'up': (u + d, v, w, d),
            'down': (u + d + w, v, w, d),
            'west': (u, v + d, d, h),       # model -X  (the mob's right side)
            'north': (u + d, v + d, w, h),  # front
            'east': (u + d + w, v + d, d, h),  # model +X (the mob's left side)
            'south': (u + 2 * d + w, v + d, w, h),  # back
        }


@dataclass
class Part:
    name: str
    pivot: tuple = (0, 0, 0)
    rot: tuple = (0, 0, 0)
    cubes: list = field(default_factory=list)
    children: list = field(default_factory=list)
    parent: Optional['Part'] = None

    def cube(self, origin, size, inflate=0.0, **paint):
        c = Cube(tuple(origin), tuple(size), paint, inflate)
        c.part = self
        self.cubes.append(c)
        return self

    def part(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        p = Part(name, tuple(pivot), tuple(rot))
        p.parent = self
        self.children.append(p)
        return p

    def walk(self):
        yield self
        for c in self.children:
            yield from c.walk()


class Model:
    """res: texels per model unit in the painted texture (the Java model keeps unit UVs, the PNG is
    res times larger, so the game samples it at that density). expressions: extra textures to paint,
    one per facial expression, from the 'expr' entries of the face specs."""

    def __init__(self, name: str, tex: tuple, palette: dict, variants: Optional[dict] = None, res: int = 1,
                 expressions: Optional[list] = None):
        self.name = name
        self.tex_w, self.tex_h = tex
        self.root = Part('root')
        self.palette = palette
        self.variants = variants or {'': {}}
        self.pixel_maps = []  # (cube, face, rows, key->palette, glow chars)
        self.res = res
        self.expressions = list(expressions or [])

    def part(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        return self.root.part(name, pivot, rot)

    def parts(self):
        for c in self.root.children:
            yield from c.walk()

    def cubes(self):
        for p in self.parts():
            yield from p.cubes

    # ------------------------------------------------------------ packing
    def pack(self):
        cubes = sorted(self.cubes(), key=lambda c: (-c.footprint[1], -c.footprint[0]))
        shelves = []  # [y, height, x_cursor]
        y_cursor = 0
        for c in cubes:
            fw, fh = c.footprint
            fw = max(fw, 1)
            fh = max(fh, 1)
            placed = False
            for s in shelves:
                if fh <= s[1] and s[2] + fw <= self.tex_w:
                    c.uv = (s[2], s[0])
                    s[2] += fw
                    placed = True
                    break
            if not placed:
                if y_cursor + fh > self.tex_h or fw > self.tex_w:
                    raise ValueError(f'{self.name}: texture {self.tex_w}x{self.tex_h} too small')
                shelves.append([y_cursor, fh, fw])
                c.uv = (0, y_cursor)
                y_cursor += fh

    # ------------------------------------------------------------ java
    def java_method(self, method_name: str) -> str:
        out = [f'    public static LayerDefinition {method_name}() {{',
               '        MeshDefinition mesh = new MeshDefinition();',
               '        PartDefinition root = mesh.getRoot();']
        counter = [0]

        def f(v):
            s = f'{v:.4f}'.rstrip('0').rstrip('.')
            if s in ('-0', ''):
                s = '0'
            return s + 'F'

        def emit(p: Part, parent_var: str):
            var = 'p' + str(counter[0])
            counter[0] += 1
            b = 'CubeListBuilder.create()'
            for c in p.cubes:
                ox, oy, oz = c.origin
                w, h, d = c.size
                b += f'.texOffs({c.uv[0]}, {c.uv[1]})'
                if c.inflate:
                    b += f'.addBox({f(ox)}, {f(oy)}, {f(oz)}, {f(w)}, {f(h)}, {f(d)}, new CubeDeformation({f(c.inflate)}))'
                else:
                    b += f'.addBox({f(ox)}, {f(oy)}, {f(oz)}, {f(w)}, {f(h)}, {f(d)})'
            px, py, pz = p.pivot
            rx, ry, rz = p.rot
            pose = f'PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})'
            decl = f'PartDefinition {var} = ' if p.children else ''
            out.append(f'        {decl}{parent_var}.addOrReplaceChild("{p.name}", {b}, {pose});')
            for ch in p.children:
                emit(ch, var)

        for p in self.root.children:
            emit(p, 'root')
        out.append(f'        return LayerDefinition.create(mesh, {self.tex_w}, {self.tex_h});')
        out.append('    }')
        return '\n'.join(out)


# --------------------------------------------------------------------------- painting
#
# Paint keys on a cube:
#   color='key'          base palette key (required)
#   pattern=...          'jelly', 'speckle', 'spots', 'stripes', 'bands', 'crystal', 'fur', 'scales', 'membrane', 'stars'
#   accent='key'         colour used by patterns
#   faces={face: {...}}  per-face overrides (same keys as the cube + 'map': ascii art)
#   glow=True            whole cube also painted on the emissive layer
#   alpha=...            'membrane' edge cut-outs for thin planes
#   outline=True/False   darken the silhouette edge of each face


FACE_LIGHT = {'up': 0.16, 'north': 0.0, 'south': -0.12, 'west': -0.07, 'east': -0.07, 'down': -0.28}


class Painter:
    def __init__(self, model: Model, palette: dict, seed: int, expression: str = ''):
        self.m = model
        self.r = model.res
        self.W, self.H = model.tex_w * self.r, model.tex_h * self.r
        self.pal = palette
        self.img = Image.new('RGBA', (self.W, self.H), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (self.W, self.H), (0, 0, 0, 0))
        self.px = self.img.load()
        self.gx = self.glow.load()
        self.seed = seed
        self.any_glow = False
        self.expression = expression

    def col(self, key):
        if isinstance(key, tuple):
            return key
        v = self.pal[key]
        return hex_rgb(v) if isinstance(v, str) else v

    def put(self, x, y, c, glow=False):
        if 0 <= x < self.W and 0 <= y < self.H:
            self.px[x, y] = c
            if glow:
                self.gx[x, y] = c
                self.any_glow = True

    def paint_all(self):
        for i, c in enumerate(self.m.cubes()):
            self.paint_cube(c, i)
        return self.img, (self.glow if self.any_glow else None)

    def paint_cube(self, cube: Cube, idx: int):
        rnd = random.Random(self.seed * 7919 + idx * 104729)
        r = self.r
        for face, (fx, fy, fw, fh) in cube.faces().items():
            if fw <= 0 or fh <= 0:
                continue
            spec = dict(cube.paint)
            spec.pop('faces', None)
            spec.update(cube.paint.get('faces', {}).get(face, {}))
            if spec.get('skip'):
                continue
            # a facial expression swaps in its own map (or any spec keys) for this face
            ex = spec.get('expr', {}).get(self.expression) if self.expression else None
            if ex is not None:
                spec = dict(spec)
                if isinstance(ex, dict):
                    spec.update(ex)
                else:
                    spec['map'] = ex
            self.paint_face(face, fx * r, fy * r, fw * r, fh * r, spec, rnd)

    def paint_mc(self, face, fx, fy, fw, fh, spec, rnd):
        """Vanilla-style face: flat base colour, a lit top rim, a shaded bottom rim and a few hand-sized
        tone clusters (no per-pixel noise, no baked directional light - the engine shades faces).

        fw/fh are in texels. At res > 1 every feature gets the extra texels as detail rather than
        as fatter pixels: two-step rims, highlight and core texels inside clusters, fine grain,
        one-texel fur strands with lit tips and jagged fringe tufts."""
        r = self.r
        uw, uh = max(1, fw // r), max(1, fh // r)  # face size in model units
        base = self.col(spec['color'])
        lite = self.col(spec.get('lite', spec['color'] + '_l')) if (spec.get('lite') or (spec['color'] + '_l') in self.pal) else shade(base, 0.12)
        dark = self.col(spec.get('dark', spec['color'] + '_d')) if (spec.get('dark') or (spec['color'] + '_d') in self.pal) else shade(base, -0.12)
        hi = shade(lite, 0.12)
        lo = shade(dark, -0.12)
        vertical = face not in ('up', 'down')
        grid = [[base for _ in range(fw)] for _ in range(fh)]
        if face == 'down':
            grid = [[dark for _ in range(fw)] for _ in range(fh)]
        rim = vertical and spec.get('rim', True) and uh >= 4
        bands = spec.get('bands')  # [(row_from, colour_key), ...] for two-tone bodies, e.g. a lighter belly band
        if bands and vertical:
            for row_from, key in bands:
                bc = self.col(key)
                y_from = max(0, row_from * r)
                for yy in range(y_from, fh):
                    for xx in range(fw):
                        grid[yy][xx] = bc
                if r > 1 and 0 < y_from < fh:
                    # a soft seam where the band starts
                    for xx in range(fw):
                        grid[y_from][xx] = mix(bc, shade(bc, 0.25), 0.6)
        if rim:
            for xx in range(fw):
                grid[0][xx] = lite
                grid[fh - 1][xx] = dark
                if r > 1:
                    grid[1][xx] = mix(grid[1][xx], lite, 0.45)
                    grid[fh - 2][xx] = mix(grid[fh - 2][xx], dark, 0.45)
        protected = (lambda y: vertical and rim and (y <= (1 if r > 1 else 0) or y >= fh - (2 if r > 1 else 1)))
        unit_shapes = [((0, 0), (1, 0)), ((0, 0), (0, 1)), ((0, 0), (1, 0), (0, 1)), ((0, 0), (1, 0), (1, 1)), ((0, 0),), ((0, 0), (1, 0), (2, 0))]

        def cluster(x0, y0, sh, tone, accent_core=None):
            """Paints a unit-shape cluster at texel scale, roughening its edges at res > 1."""
            cells = []
            for dx, dy in sh:
                for sy in range(r):
                    for sx in range(r):
                        cells.append((x0 + dx * r + sx, y0 + dy * r + sy))
            if r > 1 and len(cells) > 2:
                # knock out a corner texel or two and let one spill over, so clusters aren't square
                for _ in range(rnd.randrange(0, 3)):
                    cells.pop(rnd.randrange(len(cells)))
                ex, ey = rnd.choice(cells)
                cells.append((ex + rnd.choice((-1, 1)), ey) if rnd.random() < 0.5 else (ex, ey + rnd.choice((-1, 1))))
            for x, y in cells:
                if 0 <= x < fw and 0 <= y < fh and not protected(y):
                    grid[y][x] = tone
            if accent_core is not None and r > 1 and cells:
                x, y = min(cells, key=lambda c: c[0] + c[1])
                if 0 <= x < fw and 0 <= y < fh and not protected(y):
                    grid[y][x] = accent_core

        count = int(uw * uh * spec.get('clusters', 1.0) / 13)
        for _ in range(count):
            sh = rnd.choice(unit_shapes)
            x0, y0 = rnd.randrange(fw), rnd.randrange(fh)
            upper = y0 < fh / 2 or face == 'up'
            if rnd.random() < (0.55 if upper else 0.25):
                cluster(x0, y0, sh, lite, hi if rnd.random() < 0.6 else None)
            else:
                cluster(x0, y0, sh, dark, lo if rnd.random() < 0.4 else None)
        # fine grain: single texels of the neighbouring tones (only where there is room for it)
        if r > 1:
            grain = spec.get('grain', 0.035) * spec.get('clusters', 1.0)
            for _ in range(int(fw * fh * grain)):
                x, y = rnd.randrange(fw), rnd.randrange(fh)
                if protected(y):
                    continue
                upper = y < fh / 2 or face == 'up'
                grid[y][x] = lite if rnd.random() < (0.6 if upper else 0.3) else dark
        # spots: hand-placed looking clusters of an accent colour (axolotl / frog style markings)
        if spec.get('spots'):
            acc = self.col(spec.get('accent', spec['color']))
            acc_d = shade(acc, -0.18)
            for _ in range(max(1, int(uw * uh * spec['spots'] / 13))):
                sh = rnd.choice(unit_shapes[:4])
                x0, y0 = rnd.randrange(fw), rnd.randrange(fh)
                cells = [(x0 + dx * r + sx, y0 + dy * r + sy) for dx, dy in sh for sy in range(r) for sx in range(r)]
                cs = set(cells)
                for x, y in cells:
                    if 0 <= x < fw and 0 <= y < fh and not (vertical and y == 0 and rim):
                        # a darker lower-right edge gives each marking a little depth
                        edge = r > 1 and ((x + 1, y) not in cs or (x, y + 1) not in cs)
                        grid[y][x] = acc_d if edge else acc
        # ribs: every n-th unit column in the accent colour (fins, membranes), with a lit edge
        if spec.get('ribs'):
            acc = self.col(spec.get('accent', spec['color']))
            for xx in range(0, fw, spec['ribs'] * r):
                for yy in range(fh):
                    grid[yy][xx] = acc
                    if r > 1 and xx + 1 < fw:
                        grid[yy][xx + 1] = mix(grid[yy][xx + 1], shade(acc, 0.3), 0.5)
        # fur: short vertical strands of the dark (sometimes light) tone, one texel wide at res > 1
        streaks = spec.get('streaks', 0.0)
        if streaks and uw > 1 and uh > 2:
            for _ in range(int(fw * fh * streaks / 10)):
                x0, y0 = rnd.randrange(fw), rnd.randrange(fh)
                light_strand = rnd.random() >= 0.7
                tone = lite if light_strand else dark
                length = rnd.choice((2, 2, 3)) * r - (rnd.randrange(r) if r > 1 else 0)
                for dy in range(length):
                    y = y0 + dy
                    if 0 <= y < fh and not (vertical and y == 0 and rim):
                        tip = r > 1 and ((light_strand and dy == 0) or (not light_strand and dy == length - 1))
                        grid[y][x0] = (hi if light_strand else lo) if tip else tone
        alpha = spec.get('opacity', 255)
        glow_all = spec.get('glow', False)
        # ragged fur hem: the bottom rows of a side face are cut into tufts
        fringe = spec.get('fringe', 0) if vertical else 0
        for yy in range(fh):
            for xx in range(fw):
                if fringe:
                    cut = min(fringe, (0, 2, 1, 2, 0, 1, 2)[(xx // r + spec.get('fringe_phase', 0)) % 7]) * r
                    if r > 1 and cut > 0 and xx % r == (xx // r) % r:
                        cut -= 1  # jagged tuft tips
                    if yy >= fh - cut:
                        continue
                if self.cut(spec, xx // r, yy // r, uw, uh):
                    continue
                c = grid[yy][xx]
                self.put(fx + xx, fy + yy, (c[0], c[1], c[2], alpha), glow_all)
        if 'map' in spec:
            self.draw_map(fx, fy, fw, fh, spec)

    @staticmethod
    def cut(spec, xx, yy, fw, fh):
        """True where an alpha mode cuts a texel out of a thin plane."""
        alpha_mode = spec.get('alpha')
        if alpha_mode == 'membrane':
            # ragged trailing edge on the bottom rows (or the outer columns)
            edge = spec.get('edge', 'bottom')
            depth = spec.get('edge_depth', 2)
            if edge == 'bottom':
                scallop = depth - int(abs(math.sin((xx + 1) * math.pi / spec.get('scallop', 3))) * depth + 0.5)
                return yy >= fh - 1 - scallop
            if edge == 'outer':
                scallop = int(abs(math.sin((yy + 1) * math.pi / spec.get('scallop', 3))) * depth + 0.5)
                return xx >= fw - 1 - (depth - scallop)
        elif alpha_mode == 'frill':
            # feathery frill: comb teeth
            return yy < fh // 3 and xx % 2 == 1
        return False

    def paint_face(self, face, fx, fy, fw, fh, spec, rnd):
        if spec.get('pattern') == 'mc':
            return self.paint_mc(face, fx, fy, fw, fh, spec, rnd)
        r = self.r
        uw, uh = max(1, fw // r), max(1, fh // r)
        base = self.col(spec['color'])
        accent = self.col(spec.get('accent', spec['color']))
        light = FACE_LIGHT[face] + spec.get('light', 0.0)
        pattern = spec.get('pattern', 'speckle')
        glow_all = spec.get('glow', False)
        grad = spec.get('gradient', 0.18)  # vertical light falloff on side faces
        vertical = face not in ('up', 'down')
        for yy in range(fh):
            for xx in range(fw):
                ux, uy = xx // r, yy // r
                c = base
                t = yy / max(1, fh - 1)
                f = light
                if vertical:
                    f += grad * (0.5 - t)  # lighter towards the top
                n = rnd.random()
                if pattern == 'speckle':
                    f += (n - 0.5) * 0.10
                elif pattern == 'jelly':
                    f += (n - 0.5) * 0.06
                    if vertical and uy == 0:
                        f += 0.12
                elif pattern == 'fur':
                    f += (rnd.random() - 0.5) * 0.08 + (0.06 if (ux + (uy // 2)) % 3 == 0 else -0.02)
                elif pattern == 'spots':
                    if rnd.random() < spec.get('density', 0.12):
                        c = accent
                    f += (n - 0.5) * 0.08
                elif pattern == 'stripes':
                    if (uy + spec.get('phase', 0)) % spec.get('period', 3) == 0:
                        c = accent
                    f += (n - 0.5) * 0.06
                elif pattern == 'bands':
                    if (ux + spec.get('phase', 0)) % spec.get('period', 4) < spec.get('width', 1):
                        c = accent
                    f += (n - 0.5) * 0.06
                elif pattern == 'crystal':
                    diag = (ux + uy * 2 + spec.get('phase', 0)) % 7
                    if diag == 0:
                        f += 0.22
                    elif diag == 1:
                        f += 0.10
                    elif diag == 5:
                        f -= 0.08
                    f += (n - 0.5) * 0.05
                elif pattern == 'scales':
                    if (ux + (uy % 2) * 2) % 4 == 0 and uy % 2 == 0:
                        f -= 0.12
                    elif (ux + (uy % 2) * 2) % 4 == 1:
                        f += 0.05
                    f += (n - 0.5) * 0.05
                elif pattern == 'stars':
                    f += (n - 0.5) * 0.06
                    if rnd.random() < 0.05 / (r * r):
                        c = self.col(spec.get('star', 'star'))
                        f = 0.1
                elif pattern == 'membrane':
                    f += (n - 0.5) * 0.05
                    if ux % spec.get('rib', 4) == 0 and xx % r == 0:
                        c = accent
                elif pattern == 'flat':
                    pass
                col = shade(c, f)
                if spec.get('outline', True) and (xx < 1 or yy >= fh - 1 or xx >= fw - 1) and uw > 2 and uh > 2 and vertical:
                    col = shade(col, -0.10)
                if self.cut(spec, ux, uy, uw, uh):
                    continue
                self.put(fx + xx, fy + yy, col, glow_all)
        # ascii pixel map on top
        if 'map' in spec:
            self.draw_map(fx, fy, fw, fh, spec)
        if spec.get('shine') and face in ('up', 'north'):
            self.put(fx + 1 * r, fy + (0 if face == 'north' else fh - 2 * r), shade(base, 0.6))
            self.put(fx + 2 * r, fy + (0 if face == 'north' else fh - 2 * r), shade(base, 0.45))

    def draw_map(self, fx, fy, fw, fh, spec):
        """ASCII art on a face. Maps are in model units (each character covers res x res texels)
        unless the spec says hd=True, in which case every character is one texel - that is how
        faces get their fine detail. 'shine' chars get a one-texel highlight in their top-left
        corner when drawn at unit scale."""
        rows = spec['map']
        keys = spec.get('keys', {})
        glow_keys = set(spec.get('glow_keys', ''))
        shine_keys = set(spec.get('shine_keys', ''))
        shine = self.col(spec['shine_color']) if spec.get('shine_color') else (255, 255, 255, 255)
        k = 1 if spec.get('hd') else self.r
        at = spec.get('at', (0, 0))
        ox, oy = at[0] * (1 if spec.get('hd') else self.r), at[1] * (1 if spec.get('hd') else self.r)
        if spec.get('center', True) and 'at' not in spec:
            ox = (fw - len(rows[0]) * k) // 2
            oy = 0
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch in '. ':
                    continue
                for sy in range(k):
                    for sx in range(k):
                        x, y = fx + ox + i * k + sx, fy + oy + j * k + sy
                        if not (fx <= x < fx + fw and fy <= y < fy + fh):
                            continue
                        if ch == '_':
                            self.px[x, y] = (0, 0, 0, 0)
                            continue
                        c = self.col(keys.get(ch, ch))
                        if ch in shine_keys and k > 1 and sx == 0 and sy == 0:
                            c = shine
                        self.put(x, y, c, ch in glow_keys)


def render_textures(model: Model, seed=1):
    """Returns {variant: (texture, emissive or None)} plus {variant_expression: ...} for every
    expression of the model."""
    out = {}
    for vname, overrides in model.variants.items():
        pal = dict(model.palette)
        pal.update(overrides)
        out[vname] = Painter(model, pal, seed).paint_all()
        for ex in model.expressions:
            out[f'{vname}_{ex}'] = Painter(model, pal, seed, ex).paint_all()
    return out


# --------------------------------------------------------------------------- preview renderer


def _rot_matrix(rx, ry, rz):
    # JOML rotationZYX: R = Rz * Ry * Rx
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return _mm(_mm(Rz, Ry), Rx)


def _mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def _mv(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


class Pose:
    """Per-part overrides: {part: {'rot': (x,y,z) added, 'pos': (x,y,z) added, 'scale': (x,y,z), 'visible': bool}}"""

    def __init__(self, **parts):
        self.parts = parts


def preview(model: Model, tex: Image.Image, pose: Optional[Pose] = None, yaw=35.0, pitch=22.0, scale=8, size=(360, 360), bg=(40, 44, 70, 255)):
    pose = pose or Pose()
    res = model.res
    img = Image.new('RGBA', size, bg)
    draw = ImageDraw.Draw(img)
    tp = tex.load()
    quads = []
    yawr, pitr = math.radians(yaw), -math.radians(pitch)

    def cam(p):
        # model space (y down) -> view space. Flip y so up is up.
        x, y, z = p[0], 24 - p[1], p[2]
        # rotate yaw around y
        x, z = x * math.cos(yawr) - z * math.sin(yawr), x * math.sin(yawr) + z * math.cos(yawr)
        # pitch around x
        y, z = y * math.cos(pitr) - z * math.sin(pitr), y * math.sin(pitr) + z * math.cos(pitr)
        return (size[0] / 2 + x * scale, size[1] * 0.8 - (y - 0) * scale + 0 * scale, z)

    def visit(part: Part, M, T, S):
        ov = pose.parts.get(part.name, {})
        if ov.get('visible') is False:
            return
        rx, ry, rz = part.rot
        d = ov.get('rot', (0, 0, 0))
        rx, ry, rz = rx + d[0], ry + d[1], rz + d[2]
        px, py, pz = part.pivot
        dp = ov.get('pos', (0, 0, 0))
        px, py, pz = px + dp[0], py + dp[1], pz + dp[2]
        sc = ov.get('scale', (1, 1, 1))
        # world = T + M * (S * local)
        _o = _mv(M, [px * S[0], py * S[1], pz * S[2]])
        piv = [T[0] + _o[0], T[1] + _o[1], T[2] + _o[2]]
        R = _rot_matrix(rx, ry, rz)
        M2 = _mm(M, R)
        S2 = (S[0] * sc[0], S[1] * sc[1], S[2] * sc[2])
        Mscaled = [[M2[i][j] * S2[j] for j in range(3)] for i in range(3)]
        for c in part.cubes:
            add_cube(c, Mscaled, piv)
        # children see scaled frame
        for ch in part.children:
            visit(ch, M2, piv, S2)

    def add_cube(c: Cube, Ms, T):
        x0, y0, z0 = c.origin
        w, h, d = c.size
        g = c.inflate
        x0, y0, z0 = x0 - g, y0 - g, z0 - g
        x1, y1, z1 = x0 + w + 2 * g, y0 + h + 2 * g, z0 + d + 2 * g
        faces = c.faces()
        iw, ih, idd = (int(math.ceil(s)) for s in c.size)

        def P(x, y, z):
            v = _mv(Ms, [x, y, z])
            return [T[0] + v[0], T[1] + v[1], T[2] + v[2]]

        def face(name, corner, du, dv, nu, nv):
            fx, fy, fw, fh = faces[name]
            if fw <= 0 or fh <= 0:
                return
            fx, fy, fw, fh, nu, nv = fx * res, fy * res, fw * res, fh * res, nu * res, nv * res
            for j in range(fh):
                for i in range(fw):
                    col = tp[fx + i, fy + j]
                    if col[3] < 128:
                        continue
                    a = [corner[k] + du[k] * i / nu + dv[k] * j / nv for k in range(3)]
                    b = [a[k] + du[k] / nu for k in range(3)]
                    cc = [b[k] + dv[k] / nv for k in range(3)]
                    dd = [a[k] + dv[k] / nv for k in range(3)]
                    pts = [cam(P(*q)) for q in (a, b, cc, dd)]
                    z = sum(p[2] for p in pts) / 4
                    quads.append((z, [(p[0], p[1]) for p in pts], col, name))

        # north (z0): u along +x, v along +y
        face('north', (x0, y0, z0), (x1 - x0, 0, 0), (0, y1 - y0, 0), iw, ih)
        # south (z1): u along -x
        face('south', (x1, y0, z1), (x0 - x1, 0, 0), (0, y1 - y0, 0), iw, ih)
        # up (y0): u along +x, v from z1 to z0
        face('up', (x0, y0, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1), iw, idd)
        # down (y1)
        face('down', (x0, y1, z1), (x1 - x0, 0, 0), (0, 0, z0 - z1), iw, idd)
        # west (x0): u from z1 to z0
        face('west', (x0, y0, z1), (0, 0, z0 - z1), (0, y1 - y0, 0), idd, ih)
        # east (x1): u from z0 to z1
        face('east', (x1, y0, z0), (0, 0, z1 - z0), (0, y1 - y0, 0), idd, ih)

    I = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for p in model.root.children:
        visit(p, I, [0, 0, 0], (1, 1, 1))
    quads.sort(key=lambda q: -q[0])
    light = {'up': 1.0, 'north': 0.9, 'south': 0.75, 'east': 0.8, 'west': 0.8, 'down': 0.6}
    for z, pts, col, name in quads:
        f = light[name]
        c = (int(col[0] * f), int(col[1] * f), int(col[2] * f), 255)
        draw.polygon(pts, fill=c)
    return img
