"""CR3 Fish & Coral Organs: the sprite-fish kit.

Every fish is drawn as hand-made side-view pixel art - a region map (one character per pixel,
naming the region: back, flank, belly, fin, eye...) that each fish colours per variant - and then
extruded into a solid, rounded body. Each column of the sprite becomes a few nested boxes: a tall,
thin keel and shorter, wider flanks (like the rings of a rounded hull), so the side view is
exactly the sprite while from above and in front the body is plump in the middle and tapers to the
snout, the back and the belly. Fins and tails are cut-out sprite planes on parts of their own so
they can flex and wag.

Scale: one sprite pixel is one model unit and one texel; the fish are built at twice the size of a
vanilla model and drawn at half size (see SiftFishRenderer), so a pixel is 1/32 of a block.

A SpriteModel is a modelkit.Model (same Java generation, packing and previews) that paints its own
textures: modelkit.render_textures() calls SpriteModel.render_textures().
"""
from __future__ import annotations

import colorsys
import math

import numpy as np
from PIL import Image

from modelkit import Model


# ============================================================================ colour helpers

def rgb(c):
    """'#rrggbb' or a tuple -> (r, g, b)."""
    if isinstance(c, str):
        c = c.lstrip('#')
        return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))
    return tuple(int(v) for v in c[:3])


def tone(c, f, hue=0.04):
    """f > 0: lighter and a touch warmer; f < 0: darker, more saturated, drifting towards blue-violet
    (the way a pixel artist shades)."""
    r, g, b = (v / 255.0 for v in rgb(c))
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    if f >= 0:
        l = l + (1.0 - l) * f
        s = s * (1.0 - 0.25 * f)
        target = 0.13  # warm yellow
    else:
        l = l * (1.0 + f)
        s = min(1.0, s * (1.0 - 0.35 * f))
        target = 0.68  # blue-violet
    d = (target - h + 0.5) % 1.0 - 0.5
    h = (h + max(-hue, min(hue, d)) * abs(f) * 2.0) % 1.0
    r, g, b = colorsys.hls_to_rgb(h, max(0.0, min(1.0, l)), max(0.0, min(1.0, s)))
    return (round(r * 255), round(g * 255), round(b * 255))


def mixc(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ============================================================================ sprites

class Sprite:
    """A region map: rows of equal width, '.' (or ' ') empty, any other character a region."""

    def __init__(self, rows):
        w = max(len(r) for r in rows)
        self.rows = [r.ljust(w, '.').replace(' ', '.') for r in rows]
        self.grid = np.array([list(r) for r in self.rows])
        self.h, self.w = self.grid.shape

    def mask(self):
        return self.grid != '.'

    def flipped(self):
        return Sprite([r[::-1] for r in self.rows])


def canvas(w, h):
    return np.full((h, w), '.', dtype='<U1')


def to_sprite(grid):
    return Sprite([''.join(r) for r in grid])


def fill_profile(grid, x0, x1, top, bottom, ch):
    """Fills columns x0..x1-1 between top(x) and bottom(x) (inclusive, float rows rounded)."""
    for x in range(x0, x1):
        t, b = top(x), bottom(x)
        if t is None or b is None:
            continue
        for y in range(int(round(t)), int(round(b)) + 1):
            if 0 <= y < grid.shape[0] and 0 <= x < grid.shape[1]:
                grid[y, x] = ch


def stamp(grid, x, y, rows, keep='.'):
    """Draws rows of characters at (x, y); `keep` characters are see-through."""
    for j, r in enumerate(rows):
        for i, ch in enumerate(r):
            if ch in keep:
                continue
            yy, xx = y + j, x + i
            if 0 <= yy < grid.shape[0] and 0 <= xx < grid.shape[1]:
                grid[yy, xx] = ch


def edge_mask(mask):
    """Pixels of the mask that touch an empty pixel edge-on (or the border)."""
    m = mask
    p = np.pad(m, 1, constant_values=False)
    inner = p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]
    return m & ~inner


def column_runs(mask):
    """{col: [(top, bottom), ...]} of the opaque runs of every column."""
    out = {}
    h, w = mask.shape
    for c in range(w):
        runs, y = [], 0
        while y < h:
            if mask[y, c]:
                y0 = y
                while y < h and mask[y, c]:
                    y += 1
                runs.append((y0, y - 1))
            else:
                y += 1
        out[c] = runs
    return out


# ============================================================================ the model

class SpriteModel(Model):
    """colour(name, sprite, palette, expression) -> (rgb uint8 [h, w, 3], glow bool [h, w]) colours one
    sprite; every cube face is painted from the coloured sprites through a 'job' (see the add_* methods)."""

    def __init__(self, name, tex, variants, colour, expressions=None, palette=None):
        super().__init__(name, tex, dict(palette or {}), variants, res=1, expressions=expressions or [])
        self.material_pass = False
        self.colour = colour
        self.sprites = {}
        self.jobs = []  # (cube, face, fn(coloured) -> (rgba [fh, fw, 4], glow [fh, fw]))
        self.no_inner = set()  # sprites whose box ends and caps show their own edge colours (thin pipes, tubes)

    # ------------------------------------------------------------ sprites and geometry
    def sprite(self, name, sprite):
        self.sprites[name] = sprite
        return sprite

    def extrude(self, part, name, origin, half_width, layers=((1.0, 0.5), (0.6, 1.0)), right=None, cols=None, min_half=1):
        """Turns the columns `cols` (default all) of sprite `name` into nested boxes on `part`.
        origin: (z, y) of the sprite's top-left pixel in the part's space; the body is centred on x = 0.
        half_width(col) -> half the thickness of the column (model units); layers: (height fraction,
        width fraction) of each nested box, outermost-tallest first. right: the sprite drawn on the
        fish's right side (default: the same sprite, mirrored)."""
        spr = self.sprites[name]
        rname = right or name
        mask = spr.mask()
        runs = column_runs(mask)
        oz, oy = origin
        cols = list(cols if cols is not None else range(spr.w))
        boxes = []  # (layer, col, top, bottom, hw)
        for c in cols:
            for (t, b) in runs.get(c, []):
                h = b - t + 1
                hw_full = max(min_half, int(round(half_width(c))))
                seen = []
                for li, (fy, fx) in enumerate(layers):
                    lh = max(1, int(round(h * fy)))
                    if (h - lh) % 2:
                        lh = min(h, lh + 1)
                    lt = t + (h - lh) // 2
                    lw = max(min_half, int(round(hw_full * fx)))
                    lw = lw if lw >= 1 else min_half
                    box = (lt, lt + lh - 1, lw)
                    # drop a layer that another one already contains
                    if any(s[0] <= box[0] and s[1] >= box[1] and s[2] >= box[2] for s in seen):
                        continue
                    seen = [s for s in seen if not (box[0] <= s[0] and box[1] >= s[1] and box[2] >= s[2])]
                    seen.append(box)
                for (lt, lb, lw) in seen:
                    boxes.append((lt, lb, lw, c))
        # merge neighbouring columns with the same box
        boxes.sort(key=lambda b: (b[0], b[1], b[2], b[3]))
        merged = []
        for lt, lb, lw, c in boxes:
            if merged and merged[-1][:3] == (lt, lb, lw) and merged[-1][4] == c:
                m = merged[-1]
                merged[-1] = (lt, lb, lw, m[3], c + 1)
            else:
                merged.append((lt, lb, lw, c, c + 1))
        geo = []
        for lt, lb, lw, c0, c1 in merged:
            part.cube((-lw, oy + lt, oz + c0), (2 * lw, lb - lt + 1, c1 - c0))
            geo.append((part.cubes[-1], lt, lb, lw, c0, c1))
        for cube, lt, lb, lw, c0, c1 in geo:
            self._box_jobs(cube, name, rname, lt, lb, lw, c0, c1, geo)
        return part

    def _box_jobs(self, cube, name, rname, lt, lb, lw, c0, c1, geo):
        h, n, w = lb - lt + 1, c1 - c0, int(round(2 * lw))

        def covered(face, x, y, z):
            """True where a wider box of the same body has a face in the same plane over this texel."""
            for (o, t2, b2, w2, a0, a1) in geo:
                if o is cube or w2 < lw or (w2 == lw and (b2 - t2) <= (lb - lt)):
                    continue
                if face in ('north', 'south'):
                    zz = c0 if face == 'north' else c1
                    if (a0 if face == 'north' else a1) == zz and -w2 <= x < w2 and t2 <= y <= b2:
                        return True
                else:
                    yy = lt if face == 'up' else lb + 1
                    if (t2 if face == 'up' else b2 + 1) == yy and -w2 <= x < w2 and a0 <= z < a1:
                        return True
            return False

        def side(flip):
            def job(C):
                rgbs, glow = C[rname if flip else name]
                a = rgbs[lt:lb + 1, c0:c1]
                g = glow[lt:lb + 1, c0:c1]
                if flip:
                    a, g = a[:, ::-1], g[:, ::-1]
                return a, g
            return job

        def cap(face):
            # up/down: u along x (-lw..lw), v from the tail end (z1) to the snout (z0)
            row = lt if face == 'up' else lb
            cut = np.array([[covered(face, -lw + u + 0.5, None, z) for u in range(w)] for z in range(c1 - 1, c0 - 1, -1)])

            def job(C):
                rgbs, glow = C[name + '#inner']
                a = np.zeros((n, w, 4), np.uint8)
                g = np.zeros((n, w), bool)
                for v in range(n):
                    col = c1 - 1 - v
                    base = rgbs[row, col]
                    for u in range(w):
                        if cut[v, u]:
                            continue
                        edge = u == 0 or u == w - 1
                        c = tone(base, (0.05 if face == 'up' else -0.10) if not edge else -0.06)
                        a[v, u] = (*c, 255)
                        g[v, u] = glow[row, col]
                return a, g
            return job

        def end(face):
            col = c0 if face == 'north' else c1 - 1
            cut = np.array([[covered(face, -lw + u + 0.5, y, None) for u in range(w)] for y in range(lt, lb + 1)])

            def job(C):
                rgbs, glow = C[name + '#inner']
                a = np.zeros((h, w, 4), np.uint8)
                g = np.zeros((h, w), bool)
                for j in range(h):
                    base = rgbs[lt + j, col]
                    for u in range(w):
                        if cut[j, u]:
                            continue
                        a[j, u] = (*tone(base, -0.06 if face == 'north' else -0.14), 255)
                        g[j, u] = glow[lt + j, col]
                return a, g
            return job
        self.jobs += [(cube, 'east', side(False)), (cube, 'west', side(True)), (cube, 'up', cap('up')), (cube, 'down', cap('down')),
                      (cube, 'north', end('north')), (cube, 'south', end('south'))]

    def plane(self, part, name, origin, axis='x', flip_back=True):
        """A cut-out sprite plane on `part`. axis 'x': standing in the YZ plane (dorsal, anal and tail fins;
        sprite columns run along +z from origin (z, y)); axis 'y': lying flat (pectoral fins; origin (z, x),
        sprite rows run along +z, columns along +x); axis 'z': facing forward (origin (x, y), columns along +x)."""
        spr = self.sprites[name]
        a, b = origin
        if axis == 'x':
            part.cube((0, b, a), (0, spr.h, spr.w))
            cube = part.cubes[-1]

            def east(C):
                return C[name][0][:, :], C[name][1]

            def west(C):
                r, g = C[name]
                return (r[:, ::-1], g[:, ::-1]) if flip_back else (r, g)
            self.jobs += [(cube, 'east', east), (cube, 'west', west)]
        elif axis == 'y':
            part.cube((b, 0, a), (spr.w, 0, spr.h))
            cube = part.cubes[-1]

            def up(C):
                r, g = C[name]
                return r[::-1, :], g[::-1, :]
            self.jobs += [(cube, 'up', up), (cube, 'down', up)]
        else:
            part.cube((a, b, 0), (spr.w, spr.h, 0))
            cube = part.cubes[-1]

            def front(C):
                return C[name]

            def back(C):
                r, g = C[name]
                return r[:, ::-1], g[:, ::-1]
            self.jobs += [(cube, 'north', front), (cube, 'south', back)]
        return part

    def block(self, part, origin, size, name, faces):
        """A plain box whose faces each show a whole sprite (resized if needed): faces {face: sprite name}."""
        part.cube(origin, size)
        cube = part.cubes[-1]
        for face, sname in faces.items():
            def job(C, sname=sname, face=face):
                return C[sname]
            self.jobs.append((cube, face, job))
        return part

    # ------------------------------------------------------------ painting
    def colour_all(self, pal, expr):
        out = {}
        for n, s in self.sprites.items():
            r, g = self.colour(n, s, pal, expr)
            r = np.asarray(r, np.uint8)
            m = s.mask()
            a = np.where(m, 255, 0).astype(np.uint8)
            g = np.asarray(g, bool) & m
            out[n] = (np.concatenate([r, a[..., None]], -1), g)
            # the tops, bottoms and ends of the extruded boxes show the body's own colour, not its outline:
            # each edge pixel borrows the colour of the nearest pixel inside the silhouette
            inner = r.copy()
            e = edge_mask(m) if n not in self.no_inner else np.zeros_like(m)
            h, w = m.shape
            for y, x in zip(*np.nonzero(e)):
                for dy, dx in ((1, 0), (0, 1), (0, -1), (-1, 0), (1, 1), (1, -1), (-1, 1), (-1, -1), (2, 0), (0, 2), (0, -2), (-2, 0)):
                    yy, xx = y + dy, x + dx
                    if 0 <= yy < h and 0 <= xx < w and m[yy, xx] and not e[yy, xx]:
                        inner[y, x] = r[yy, xx]
                        break
            out[n + '#inner'] = (np.concatenate([inner, a[..., None]], -1), g)
        return out

    def paint(self, pal, expr):
        W, H = self.tex_w, self.tex_h
        img = np.zeros((H, W, 4), np.uint8)
        glow = np.zeros((H, W, 4), np.uint8)
        C = self.colour_all(pal, expr)
        for cube, face, fn in self.jobs:
            fx, fy, fw, fh = cube.faces()[face]
            if fw <= 0 or fh <= 0:
                continue
            a, g = fn(C)
            if a.shape[0] != fh or a.shape[1] != fw:
                a = np.asarray(Image.fromarray(a, 'RGBA').resize((fw, fh), Image.NEAREST))
                g = np.asarray(Image.fromarray(g.astype(np.uint8) * 255, 'L').resize((fw, fh), Image.NEAREST)) > 127
            img[fy:fy + fh, fx:fx + fw] = a
            gl = np.where(g[..., None] & (a[..., 3:] > 0), a, 0)
            glow[fy:fy + fh, fx:fx + fw] = np.maximum(glow[fy:fy + fh, fx:fx + fw], gl)
        return Image.fromarray(img, 'RGBA'), Image.fromarray(glow, 'RGBA')

    def render_textures(self, seed=1):
        out = {}
        for vname, overrides in self.variants.items():
            pal = dict(self.palette)
            pal.update(overrides)
            for ex in [''] + list(self.expressions):
                img, glow = self.paint(pal, ex)
                out[vname + (f'_{ex}' if ex else '')] = (img, glow if glow.getbbox() else None)
        return out


# ============================================================================ shared shading for fish sprites

def scale_pattern(x, y, phase=0):
    """Overlapping fish scales, 4 x 3 pixels: -1 the shaded lower rim of a scale, +1 its lit crown, 0 plain."""
    r = (y + phase) // 3
    cx = (x + (r % 2) * 2) % 4
    cy = (y + phase) % 3
    if (cy == 2 and cx in (1, 2)) or (cy == 1 and cx in (0, 3)):
        return -1
    if cy == 0 and cx in (1, 2):
        return 1
    return 0


def body_shade(mask, x, y, runs_top, runs_bot):
    """0 at the top of the body's column, 1 at its bottom (for countershading)."""
    t, b = runs_top[x], runs_bot[x]
    return (y - t) / max(1, b - t)


def column_extents(mask):
    h, w = mask.shape
    top = np.full(w, -1)
    bot = np.full(w, -1)
    for x in range(w):
        ys = np.nonzero(mask[:, x])[0]
        if len(ys):
            top[x], bot[x] = ys[0], ys[-1]
    return top, bot


def ellipse_profile(length, height, blunt=0.42, taper=0.9, hump=0.0, keel=0.0):
    """Top and bottom (float rows, 0 = the body's top line at its tallest) of a fish body of the given length
    and height: a rounded head (blunt: 0 pointed .. 1 square), a long taper to the tail."""
    def half(t):
        if t < 0 or t > 1:
            return None
        head = (min(1.0, t / max(0.01, blunt))) ** 0.5 if t < blunt else 1.0
        tail = 1.0 - ((t - blunt) / (1.0 - blunt)) ** 1.6 * taper if t > blunt else 1.0
        return head * tail

    def top(x):
        t = (x + 0.5) / length
        k = half(t)
        if k is None:
            return None
        return height / 2.0 * (1.0 - k) - hump * math.sin(math.pi * min(1.0, t / 0.8)) * k

    def bottom(x):
        t = (x + 0.5) / length
        k = half(t)
        if k is None:
            return None
        return height / 2.0 * (1.0 + k) - 1.0 + keel * math.sin(math.pi * t) * k
    return top, bottom


def fan(width, height, base=3, spread=1.0, rays=0.42, scallop=3, notch=0.0, sweep=0.0, base_cols=2):
    """A fan-shaped fin as rows of region characters ('d' fleshy base, 'r' ray, 'f' membrane, 't' tip): it
    grows from a `base`-pixel root on the left edge (vertically centred) out to `width` x `height`.
    rays: angle between rays (radians); scallop: lobes on the outer edge; notch: how deep the middle
    of the edge is cut (a forked tail); sweep: rays lean back by this much (dorsal fins)."""
    cy = (height - 1) / 2.0
    rows = []
    for y in range(height):
        row = ''
        for x in range(width):
            dx, dy = x + 0.5 + base * 0.6, (y - cy) * spread
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            reach = width + base * 0.6 - 0.3 - notch * width * max(0.0, math.cos(a * 2.2)) ** 3
            reach -= abs(math.sin(a * scallop * 2.0)) * 1.1 if scallop else 0
            half = height / 2.0 + 0.2
            inside = r <= reach and abs(y - cy) <= half * min(1.0, (x + base) / max(1.0, base + width * 0.35))
            if not inside:
                row += '.'
                continue
            ray = abs(((a + sweep) / rays) - round((a + sweep) / rays)) < 0.16 + 0.35 / max(1.0, r)
            if x < base_cols:
                ch = 'd'
            elif r > reach - 1.2:
                ch = 't'
            elif ray:
                ch = 'r'
            else:
                ch = 'f'
            row += ch
        rows.append(row)
    return rows


def shape_fin(w, h, outline, rays=(), root=None, base_cols=0, tips=True):
    """A fin drawn like vector art and snapped to pixels: `outline` is a polygon (pixel coordinates) filled with
    membrane 'f', `rays` are line segments drawn as 'r' (inside the fin only), the rim is 't' (unless tips is
    False), and pixels within `base_cols` of x = 0 (or of the `root` point) are the fleshy base 'd'."""
    from PIL import ImageDraw
    m = Image.new('L', (w, h), 0)
    d = ImageDraw.Draw(m)
    d.polygon([tuple(p) for p in outline], fill=255)
    fill = np.asarray(m) > 127
    rl = Image.new('L', (w, h), 0)
    dr = ImageDraw.Draw(rl)
    for a, b in rays:
        dr.line([tuple(a), tuple(b)], fill=255, width=1)
    ray = (np.asarray(rl) > 127) & fill
    rim = edge_mask(fill)
    rows = []
    for y in range(h):
        row = ''
        for x in range(w):
            if not fill[y, x]:
                row += '.'
            elif base_cols and (x < base_cols if root is None else math.hypot(x - root[0], y - root[1]) < base_cols):
                row += 'd'
            elif tips and rim[y, x]:
                row += 't'
            elif ray[y, x]:
                row += 'r'
            else:
                row += 'f'
        rows.append(row)
    return rows
