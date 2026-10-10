"""S1 land creatures: shared helpers for the high-res modelkit mobs (Stomper, Sifter, Swifter, Harmoner,
Mini Creator, Sky Whale).

Everything here feeds the same pipeline as the Sculk mobs (tools/cave_creatures.py, tools/parasite.py):
`Model(..., res=2)` with the material pass, so maps are drawn at texel scale (hd=True, one character per
texel = half a model unit) and the PNG ends up at four texels per model unit.

- rows(): an ASCII texel map from a function.
- creases(): wrinkle rings / feather rows as an hd map (lit ridge over a dark crease, wobbling).
- sprite painters (coral, blades, sprouts, blooms, bells, wisps): little cross-plane plants and puffs
  painted at texel scale as RGBA images with their own light (lit tips, dark roots).
- egg(): the 16x16 spawn egg helper used by tools/land_eggs.py.
"""
import math
import random

from PIL import Image


def hexc(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp_c(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def ramp_pick(ramp, t):
    """A colour from a dark->light list of hex colours (no blending: pixel-art steps)."""
    t = max(0.0, min(0.999, t))
    return hexc(ramp[int(t * len(ramp))])


def rows(w, h, fn):
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


def shade3(hx, dl=0.11, dd=-0.13):
    """(base, light, dark) hex of one colour, hue kept."""
    import colorsys
    r, g, b = (int(hx[i:i + 2], 16) / 255 for i in (1, 3, 5))
    hh, ll, ss = colorsys.rgb_to_hls(r, g, b)

    def mk(d):
        rr, gg, bb = colorsys.hls_to_rgb(hh, max(0.0, min(1.0, ll + d)), min(1.0, ss * (1.0 + (0.08 if d < 0 else -0.05))))
        return '#%02x%02x%02x' % (int(rr * 255), int(gg * 255), int(bb * 255))
    return hx, mk(dl), mk(dd)


def tones(**colours):
    """{'hide': '#8d5e6d', ...} -> palette with hide, hide_l, hide_d for each."""
    out = {}
    for k, v in colours.items():
        out[k], out[k + '_l'], out[k + '_d'] = shade3(v)
    return out


def creases(w, h, period=5, seed=1, wob=1.2, ridge='h', crease='w', vertical=False, gap=0.0, start=1):
    """Wrinkle rings across a face (an elephant's trunk and legs): every `period` texels a dark crease
    with a lit ridge just above it, wobbling and now and then breaking off."""
    rnd = random.Random(seed)
    W, H = (h, w) if vertical else (w, h)
    phase = [rnd.random() * 6.28 for _ in range(H // max(1, period) + 2)]
    grid = [['.'] * W for _ in range(H)]
    for k, y0 in enumerate(range(start, H, period)):
        ph = phase[k]
        amp = wob * (0.6 + rnd.random() * 0.8)
        for x in range(W):
            if gap and rnd.random() < gap:
                continue
            y = int(round(y0 + math.sin(x * 0.55 + ph) * amp * 0.6))
            if 0 <= y < H:
                grid[y][x] = crease
            if 0 <= y - 1 < H and grid[y - 1][x] == '.':
                grid[y - 1][x] = ridge
    out = [''.join(r) for r in grid]
    if vertical:
        out = [''.join(out[y][x] for y in range(len(out))) for x in range(len(out[0]))]
    return out


def overlay(base, top):
    """Map `top` drawn over `base` ('.' is see-through)."""
    return [''.join(t if t != '.' else b for b, t in zip(rb, rt)) for rb, rt in zip(base, top)]


def speckle(w, h, seed, chars, density):
    """Sparse single texels of `chars` (e.g. tiny grass tufts or flowers on turf)."""
    rnd = random.Random(seed)
    return rows(w, h, lambda x, y: rnd.choice(chars) if rnd.random() < density else '.')


# --------------------------------------------------------------------------- sprites (cross-plane plants)

class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.px = self.img.load()

    def put(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = c

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[x, y]
        return (0, 0, 0, 0)

    def disc(self, cx, cy, r, fn):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    self.put(x, y, fn(x - cx, y - cy, d / max(r, 0.01)))


def coral(w, h, seed, ramp):
    """A branching coral bush: thick dark roots forking up into thin lit twigs with round knobbly tips."""
    cv = Canvas(w, h)
    rnd = random.Random(seed)

    def branch(x, y, ang, length, thick, depth):
        steps = int(length)
        for i in range(steps):
            x += math.sin(ang)
            y -= math.cos(ang)
            ang += (rnd.random() - 0.5) * 0.25
            t = 1.0 - y / h
            for dx in range(-int(thick) + 1, 1) if thick >= 2 else (0,):
                lit = dx == 0 and thick >= 2
                cv.put(x + dx, y, ramp_pick(ramp, 0.15 + t * 0.6 + (0.18 if lit else 0.0)))
            if thick < 2:
                cv.put(x, y, ramp_pick(ramp, 0.3 + t * 0.6))
        if depth <= 0 or length < 2:
            cv.put(x, y - 1, ramp_pick(ramp, 0.95))
            cv.put(x + 1, y, ramp_pick(ramp, 0.8))
            cv.put(x - 1, y, ramp_pick(ramp, 0.85))
            cv.put(x, y, ramp_pick(ramp, 0.99))
            return
        n = 2 if rnd.random() < 0.75 else 3
        for k in range(n):
            a = ang + (k - (n - 1) / 2) * (0.55 + rnd.random() * 0.35)
            branch(x, y, a, length * (0.62 + rnd.random() * 0.2), max(1, thick - 1), depth - 1)

    branch(w / 2 - 0.5, h - 1, (rnd.random() - 0.5) * 0.3, h * 0.36, 2, 3)
    return cv.img


def blades(w, h, seed, ramp, n=None, lean=0.0, tips=None):
    """A tuft of grass: curved blades, dark at the root and lit at the tip (a few tipped in `tips`)."""
    cv = Canvas(w, h)
    rnd = random.Random(seed)
    n = n or max(5, w // 2 + 2)
    for k in range(n):
        x0 = w / 2 + (rnd.random() - 0.5) * w * 0.55
        length = h * (0.45 + rnd.random() * 0.55)
        bend = (rnd.random() - 0.5) * 1.6 + lean
        for i in range(int(length)):
            t = i / max(1, length - 1)
            x = x0 + bend * t * t * w * 0.25
            y = h - 1 - i
            c = ramp_pick(ramp, 0.12 + t * 0.85)
            if tips and t > 0.85:
                c = hexc(tips)
            cv.put(x, y, c)
            if i < 2:
                cv.put(x + (1 if bend > 0 else -1), y, ramp_pick(ramp, 0.1))
    return cv.img


def sprouts(w, h, seed, stem, bulb, n=3):
    """Glimmer sprouts: thin teal stalks with round glowing bulbs on top. Returns (image, glow mask colour)."""
    cv = Canvas(w, h)
    rnd = random.Random(seed)
    for k in range(n):
        x0 = (k + 0.5) * w / n + (rnd.random() - 0.5) * 1.5
        top = h * (0.25 + rnd.random() * 0.35)
        curve = (rnd.random() - 0.5) * 2.0
        y = h - 1
        x = x0
        while y > top + 2:
            t = (h - 1 - y) / (h - top)
            x = x0 + curve * t * t
            cv.put(x, y, ramp_pick(stem, 0.15 + t * 0.7))
            if rnd.random() < 0.12:  # a little leaf
                cv.put(x + (1 if rnd.random() < 0.5 else -1), y, ramp_pick(stem, 0.6))
            y -= 1
        cv.disc(x, top, 1.6, lambda dx, dy, d: ramp_pick(bulb, 0.95 - d * 0.5 - (dy > 0) * 0.2))
    return cv.img


def blooms(w, h, seed, stem, petal, centre, n=2):
    """Flowers: leafy stalks under five-petal heads with a bright centre."""
    cv = Canvas(w, h)
    rnd = random.Random(seed)
    for k in range(n):
        x0 = (k + 0.5) * w / n + (rnd.random() - 0.5) * 2.0
        top = h * (0.18 + rnd.random() * 0.3) + 2
        lean = (rnd.random() - 0.5) * 2.0
        y = h - 1
        x = x0
        while y > top:
            t = (h - 1 - y) / (h - top)
            x = x0 + lean * t
            cv.put(x, y, ramp_pick(stem, 0.2 + t * 0.6))
            if abs(y - (h - 1 - (h - top) * 0.4)) < 0.6:  # a pair of leaves
                cv.put(x - 1, y, ramp_pick(stem, 0.7))
                cv.put(x - 2, y - 1, ramp_pick(stem, 0.85))
                cv.put(x + 1, y + 1, ramp_pick(stem, 0.55))
                cv.put(x + 2, y, ramp_pick(stem, 0.75))
            y -= 1
        for ddx, ddy in ((0, -2), (-2, -1), (2, -1), (-1, 1), (1, 1)):
            cv.disc(x + ddx * 0.8, top + ddy * 0.8, 1.15, lambda a, b, d: ramp_pick(petal, 0.95 - d * 0.35 - (b > 0) * 0.25))
        cv.put(x, top, hexc(centre[0]))
        cv.put(x, top - 1, hexc(centre[1]))
    return cv.img


def bells(w, h, seed, stem, petal, n=2):
    """Bellflowers: arching stalks with hanging bells."""
    cv = Canvas(w, h)
    rnd = random.Random(seed)
    for k in range(n):
        x0 = (k + 0.5) * w / n
        top = h * (0.15 + rnd.random() * 0.2)
        y = h - 1
        while y > top:
            t = (h - 1 - y) / (h - top)
            cv.put(x0 + math.sin(t * 2.2) * 1.5, y, ramp_pick(stem, 0.2 + t * 0.6))
            y -= 1
        for j, (bx, by) in enumerate(((x0 + 2.5, top + 1.5), (x0 - 1.5, top + 5), (x0 + 2, top + 8))):
            if by > h - 4:
                continue
            cv.put(bx - 1, by - 1, ramp_pick(stem, 0.7))
            for yy in range(3):
                for xx in range(-1 - (yy == 2), 2 + (yy == 2)):
                    cv.put(bx + xx, by + yy, ramp_pick(petal, 0.9 - yy * 0.25 - (xx > 0) * 0.15))
            cv.put(bx, by + 3, ramp_pick(petal, 0.98))
    return cv.img


def puff(w, h, seed, ramp, holes=0.0):
    """A soft cloud puff (front view): overlapping round lobes, lit on top, shaded underneath."""
    cv = Canvas(w, h)
    rnd = random.Random(seed)
    lobes = [(w * (0.2 + 0.6 * rnd.random()), h * (0.35 + 0.35 * rnd.random()), min(w, h) * (0.22 + 0.15 * rnd.random())) for _ in range(5)]
    lobes.append((w / 2, h * 0.6, min(w, h) * 0.38))
    for y in range(h):
        for x in range(w):
            best = None
            for cx, cy, r in lobes:
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r
                if d < 1.0 and (best is None or d < best[0]):
                    best = (d, (y + 0.5 - cy) / r)
            if best is None:
                continue
            d, dy = best
            if holes and rnd.random() < holes * d:
                continue
            cv.put(x, y, ramp_pick(ramp, 0.85 - dy * 0.35 - d * 0.25))
    return cv.img


def mirror(img):
    return img.transpose(Image.FLIP_LEFT_RIGHT)


# --------------------------------------------------------------------------- spawn eggs

def egg(rows_, pal, outline=None, light=True):
    """A 16x16 spawn egg from 16 rows of keys; light=True shades the body keys (upper-case `pal` entries
    marked with a trailing '*') from the top left; outline adds a one-pixel dark rim round the shape."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows_):
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            c = pal[ch]
            if isinstance(c, str):
                c = hexc(c)
            px[x, y] = c
    if outline:
        out = img.copy()
        po = out.load()
        for y in range(16):
            for x in range(16):
                if px[x, y][3]:
                    continue
                if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    po[x, y] = hexc(outline)
        img = out
    return img


# --------------------------------------------------------------------------- small vanilla-style eyes
# The user's rule: never cartoon eyes (no big round eyes, no white highlights, no blush). Eyes are small pixel eyes
# in the manner of the vanilla fox, frog, parrot and warden: a dark pupil beside a coloured iris under a lid line;
# moods only move the lids.

def small_eye(expr='', w=4, h=3, mirror=False, glow=False):
    """A small eye of w x h texels (front edge on the left; mirror for the other side).
    Keys: d lid line / closed lid, i iris, I lower iris, p pupil, '.' the skin around it.
    glow=True: an eye of light (warden-like) - i/I are the light, the pupil is left out."""
    if expr in ('blink', 'sleep', 'dead'):
        out = ['.' * w for _ in range(h)]
        out[h // 2] = 'd' * w
        return out
    if expr == 'hurt':
        out = ['.' * w for _ in range(h)]
        out[h // 2] = 'd' * w
        out[max(0, h // 2 - 1)] = ''.join('d' if x in (0, w - 1) else '.' for x in range(w))
        return [r[::-1] for r in out] if mirror else out
    rows_ = []
    for y in range(h):
        row = ''
        for x in range(w):
            back = x >= w // 2  # the pupil sits towards the back of the eye
            if y == 0:
                row += 'd'
            elif expr == 'angry' and y == 1 and x < (w + 1) // 2:
                row += 'd'  # the lid drops at the front: a glare
            elif expr == 'happy' and y == h - 1:
                row += 'd'  # a contented, half-closed eye
            elif not glow and back and x < w // 2 + max(1, w // 3):
                row += 'p'
            else:
                row += 'i' if y < h - 1 else 'I'
        rows_.append(row)
    return [r[::-1] for r in rows_] if mirror else rows_


def place(base, glyph, x0, y0):
    """Draws a glyph ('.' see-through) into a copy of a map at (x0, y0)."""
    g = [list(r) for r in base]
    for j, row in enumerate(glyph):
        for i, ch in enumerate(row):
            if ch != '.' and 0 <= y0 + j < len(g) and 0 <= x0 + i < len(g[0]):
                g[y0 + j][x0 + i] = ch
    return [''.join(r) for r in g]
