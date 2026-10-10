"""INS free play: the instruments themselves, built with the kit in tools/instruments3d.py.

Every builder returns a list of elements authored upright: the instrument's length runs along +y, its playing
face looks south (+z). Moving parts carry tags the frames use (``str_lo``/``str_mid``/``str_hi`` strings,
``head_l``/``head_r`` drum heads, ``tube_<i>`` chimes with their cords, ``key_<i>`` flute keys).

FAMILY says how each family is held while playing (third person: the right arm's reach, the instrument's axes
in the world, its scale; first person: where it sits in front of the camera). ``place()`` turns that into the
display transforms of the play models and into the points the free hand reaches for - those are mirrored in
client/music/InstrumentPoses.java (model-space pixels).
"""
import math

import numpy as np

import instruments3d as K
from instruments3d import El, hexc, mix, smooth, fbm, white, profile_slab, cyl_y

# ============================================================================ decorations


def _rows_hw(rows, Y):
    ys = np.array([(a + b) / 2 for a, b, _ in rows])
    hw = np.array([w for _, _, w in rows])
    return np.interp(Y, ys, hw, left=hw[0], right=hw[-1])


def guitar_top(rows, cx=8.0, hole=(8.0, 6.55, 1.0), burst=('#7a3a14', 0.65), guard=True, star=False, binding='#efe2c4', purfling='#2a1a10',
               rosette=('#3a2414', '#e8d8b0', '#8a4a26')):
    """The soundboard: a sunburst toward the rim, ivory binding and dark purfling round the outline, a mosaic
    rosette round the sound hole (or a glowing star), the hole itself, and a tortoiseshell pickguard."""
    y_lo, y_hi = rows[0][0], rows[-1][1]
    bc, bp, rd, rl, rm = hexc(binding), hexc(purfling), hexc(rosette[0]), hexc(rosette[1]), hexc(rosette[2])
    bcol, bstr = hexc(burst[0]), burst[1]
    wmax = max(r[2] for r in rows)

    def paint(col, X, Y, Z, c):
        if c.face != 'south':
            return col
        hw = _rows_hw(rows, Y)
        d = np.minimum(hw - np.abs(X - cx), np.minimum(Y - y_lo, y_hi - Y))
        mid = (y_lo + y_hi) / 2
        e = np.sqrt(((X - cx) / wmax) ** 2 + ((Y - mid) / ((y_hi - y_lo) / 2)) ** 2)
        col = mix(col, bcol, smooth(0.72, 1.12, e) * bstr)
        hx, hy, hr = hole
        r = np.sqrt((X - hx) ** 2 + (Y - hy) ** 2)
        if guard:
            g = ((X - hx - 1.25) / 0.95) ** 2 + ((Y - hy + 1.05) / 1.25) ** 2 < 1.0
            g &= r > hr + 0.5
            tort = fbm(X * 3, Y * 3, 0, 61, 3)
            gc = mix(hexc('#5a2410'), hexc('#c07a3a'), np.clip((tort - 0.45) * 2.5, 0, 1))
            col = np.where(g[..., None], gc, col)
        if star:
            ang = np.arctan2(Y - hy, X - hx)
            spikes = hr + 0.42 + 0.3 * np.cos(ang * 5)
            sel = (r < spikes) & (r > hr)
            col = np.where(sel[..., None], mix(hexc('#fff6c0'), hexc('#e0a020'), np.clip((r - hr) / 0.7, 0, 1)), col)
        else:
            band = (r > hr + 0.08) & (r < hr + 0.48)
            tile = (np.floor(np.arctan2(Y - hy, X - hx) * 9) + np.floor(r * 14)) % 3
            rc = np.where((tile == 0)[..., None], rd, np.where((tile == 1)[..., None], rl, rm))
            col = np.where(band[..., None], rc, col)
            ring = (np.abs(r - hr - 0.04) < 0.05) | (np.abs(r - hr - 0.53) < 0.05)
            col = np.where(ring[..., None], rd, col)
        inside = r <= hr
        dark = mix(hexc('#2a160c'), hexc('#060302'), np.clip(1 - r / hr, 0, 1) * 0.6 + 0.4)
        col = np.where(inside[..., None], dark, col)
        col = np.where((d < 0.11)[..., None], bc * (0.92 + 0.08 * c.t[..., None]), col)
        col = np.where(((d >= 0.11) & (d < 0.19))[..., None], bp, col)
        return col
    return paint


def frets_paint(nut, scale, x0, x1, dots=(3, 5, 7, 9, 12), fret_col='#e8a070', dot_col='#f4f0e0', n=14):
    fc, dc = hexc(fret_col), hexc(dot_col)

    def paint(col, X, Y, Z, c):
        if c.face != 'south':
            return col
        for k in range(1, n + 1):
            yf = nut - scale * (1 - 2 ** (-k / 12.0))
            col = np.where((np.abs(Y - yf) < 0.06)[..., None], fc, col)
            if k in dots:
                prev = nut - scale * (1 - 2 ** (-(k - 1) / 12.0))
                ym = (yf + prev) / 2
                cxs = [(x0 + x1) / 2] if k != 12 else [x0 + (x1 - x0) * 0.3, x0 + (x1 - x0) * 0.7]
                for cx in cxs:
                    col = np.where((((X - cx) ** 2 + (Y - ym) ** 2) < 0.016)[..., None], dc, col)
        return col
    return paint


def mark_paint(cx, cy, ink, face='south', size=1.0):
    """A small inlaid eighth note - the maker's mark of the Sift's instrument makers."""
    k = hexc(ink)

    def paint(col, X, Y, Z, c):
        if c.face != face:
            return col
        s = size
        head = ((X - cx + 0.15 * s) ** 2 + (Y - cy + 0.25 * s) ** 2) < 0.05 * s * s
        stem = (np.abs(X - cx) < 0.05 * s) & (Y > cy - 0.25 * s) & (Y < cy + 0.45 * s)
        flag = (X > cx) & (X < cx + 0.3 * s) & (np.abs(Y - (cy + 0.4 * s - (X - cx) * 0.6)) < 0.06 * s)
        return np.where((head | stem | flag)[..., None], k, col)
    return paint


def holes_paint(points, r=0.2, face='south', rim='#3a2a10'):
    """Finger holes / an embouchure: dark wells with a lighter worn rim."""
    rc = hexc(rim)

    def paint(col, X, Y, Z, c):
        if c.face != face:
            return col
        for (hx, hy, hrr) in points:
            rr = hrr or r
            d = np.sqrt(((X - hx) / 1.0) ** 2 + ((Y - hy) / (rr * 1.25 / rr)) ** 2)
            col = np.where((d < rr * 1.35)[..., None], mix(col, rc, 0.6), col)
            col = np.where((d < rr)[..., None], mix(hexc('#140a04'), hexc('#000000'), np.clip(1 - d / rr, 0, 1)), col)
        return col
    return paint


def bands_paint(ys, width, colour, axis='y', stripes=False):
    cc = hexc(colour)

    def paint(col, X, Y, Z, c):
        A = {'x': X, 'y': Y, 'z': Z}[axis]
        for y in ys:
            on = np.abs(A - y) < width
            if stripes:
                on &= (np.floor((A - y) * 12) % 2) == 0
            col = np.where(on[..., None], cc * (0.85 + 0.25 * white(X, Y, Z, 77)[..., None]), col)
        return col
    return paint


def chain(*painters):
    def paint(col, X, Y, Z, c):
        for p in painters:
            if p is not None:
                col = p(col, X, Y, Z, c)
        return col
    return paint


# ============================================================================ STRINGS

GUITAR_ROWS = [(0.0, 0.5, 2.4), (0.5, 1.2, 3.1), (1.2, 2.0, 3.45), (2.0, 3.6, 3.6), (3.6, 4.4, 3.45), (4.4, 5.0, 3.05), (5.0, 5.7, 2.65),
               (5.7, 6.3, 2.75), (6.3, 7.6, 2.95), (7.6, 8.3, 2.8), (8.3, 8.8, 2.4), (8.8, 9.2, 1.7)]


def strings(xs, y0, y1, z0, z1, mat='gut', w=0.075, splits=None, key='str', glow=0, tops=None, bottoms=None):
    """Strings as three tagged segments each (lower, middle, upper) so the frames can make them vibrate."""
    out = []
    for i, x in enumerate(xs):
        lo = bottoms[i] if bottoms else y0
        hi = tops[i] if tops else y1
        a, b = splits if splits else (lo + (hi - lo) * 0.25, lo + (hi - lo) * 0.72)
        for (s0, s1, tag) in ((lo, a, 'str_lo'), (a, b, 'str_mid'), (b, hi, 'str_hi')):
            out.append(El((x - w / 2, s0, z0), (x + w / 2, s1, z1), mat if isinstance(mat, str) else mat[i], tag=tag,
                          faces=('south', 'west', 'east', 'north'), key=key if isinstance(mat, str) else None, glow=glow, shade=not glow))
    return out


def weaver_top(rows):
    """The Weaver's Guitar: black chitin with glowing cyan veins spreading from the sound hole like a web."""
    def paint(col, X, Y, Z, c):
        if c.face != 'south':
            return col
        hx, hy = 8.0, 6.55
        r = np.sqrt((X - hx) ** 2 + (Y - hy) ** 2)
        ang = np.arctan2(Y - hy, X - hx)
        spokes = np.abs(np.sin(ang * 4)) < 0.09 + 0.02 * r
        rings = np.abs(np.sin(r * 2.6)) < 0.12
        web = (spokes | rings) & (r > 1.0)
        col = np.where(web[..., None], mix(hexc('#2ef2e2'), hexc('#0a6a70'), np.clip(r / 6, 0, 1)), col)
        col = np.where((r <= 1.0)[..., None], hexc('#041014'), col)
        hw = _rows_hw(rows, Y)
        d = np.minimum(hw - np.abs(X - 8.0), np.minimum(Y - rows[0][0], rows[-1][1] - Y))
        col = np.where((d < 0.12)[..., None], hexc('#2ef2e2') * 0.8, col)
        return col
    return paint


def guitar(variant='guitar'):
    """The Guitar: a small spruce-topped parlour guitar - mahogany back and sides, ivory binding, a mosaic rosette,
    a tortoiseshell guard, a rosewood fretboard with copper frets and pearl dots, four gut strings, copper tuners.
    The Weaver's Guitar is the same shape in black chitin, webbed with glowing veins and strung with silk."""
    els = []
    weaver = variant == 'weaver'
    top, side = ('chitin', 'chitin') if weaver else ('spruce', 'mahogany')
    rows = GUITAR_ROWS
    deco = weaver_top(rows) if weaver else guitar_top(rows, hole=(8.0, 6.55, 1.0))
    els += profile_slab(8.0, rows, 6.9, 9.1, side, mats={'south': top}, paint=deco)
    els.append(El((7.35, 8.7, 7.45), (8.65, 17.0, 8.9), side, faces=('north', 'west', 'east')))
    els.append(El((7.15, 8.4, 7.0), (8.85, 9.6, 8.95), side, faces=('north', 'west', 'east', 'down')))
    nut, scale = 17.0, 14.45
    fb = 'ebony' if weaver else 'rosewood'
    els.append(El((7.22, 7.6, 8.9), (8.78, 17.0, 9.24), fb, faces=('south', 'west', 'east', 'down'),
                  paint=frets_paint(nut, scale, 7.22, 8.78, fret_col='#9ff6ff' if weaver else '#e8a070')))
    fret_mat = 'silver_x' if weaver else 'copper_x'
    for k in range(1, 13):
        yf = nut - scale * (1 - 2 ** (-k / 12.0))
        els.append(El((7.22, yf - 0.045, 9.24), (8.78, yf + 0.045, 9.3), fret_mat, faces=('south', 'up', 'down'), key='fret'))
    els.append(El((7.22, 17.0, 8.9), (8.78, 17.25, 9.42), 'bone', faces=('south', 'up', 'west', 'east')))
    hrot = ('x', -15.0, (8.0, 17.25, 8.85))
    els.append(El((6.95, 17.25, 8.05), (9.05, 20.1, 8.85), side, mats={'south': fb}, rot=hrot,
                  paint=mark_paint(8.0, 19.25, '#2ef2e2' if weaver else '#f4f0e0')))
    for yk in (18.15, 19.25):
        for sx in (-1, 1):
            xa = 6.95 if sx < 0 else 9.05
            x0, x1 = (xa - 0.55, xa) if sx < 0 else (xa, xa + 0.55)
            b0, b1 = (xa - 1.05, xa - 0.55) if sx < 0 else (xa + 0.55, xa + 1.05)
            els.append(El((x0, yk - 0.16, 8.25), (x1, yk + 0.16, 8.6), 'silver_x' if weaver else 'copper_x', rot=hrot,
                          faces=('north', 'south', 'up', 'down'), key='tuner'))
            els.append(El((b0, yk - 0.33, 8.12), (b1, yk + 0.33, 8.72), 'sculk_glow' if weaver else 'ivory', rot=hrot, key='tbtn',
                          glow=9 if weaver else 0))
    els.append(El((6.55, 2.05, 9.1), (9.45, 2.85, 9.42), fb, faces=('south', 'up', 'down', 'west', 'east')))
    els.append(El((7.2, 2.42, 9.42), (8.8, 2.58, 9.56), 'bone', faces=('south', 'up', 'down', 'west', 'east')))
    els.append(El((7.8, -0.25, 7.8), (8.2, 0.0, 8.2), 'ivory', faces=('down', 'north', 'south', 'west', 'east')))
    els += strings([7.5, 7.83, 8.17, 8.5], 2.5, 17.1, 9.38, 9.46, 'silk' if weaver else 'gut', splits=(6.0, 12.6), glow=12 if weaver else 0)
    return els


LUTE_ROWS = [(0.0, 0.5, 2.2), (0.5, 1.3, 3.0), (1.3, 2.5, 3.55), (2.5, 4.0, 3.75), (4.0, 5.5, 3.6), (5.5, 6.8, 3.2), (6.8, 7.9, 2.6),
             (7.9, 8.9, 1.95), (8.9, 9.7, 1.35)]


def ribs_paint(rows, light='#d8b078', dark='#5a3418'):
    """A lute's bowl: ribs of pale maple and dark walnut fanning from the neck, with fine dark seams."""
    lc, dc = hexc(light), hexc(dark)

    def paint(col, X, Y, Z, c):
        if c.face == 'south':
            return col
        hw = np.maximum(_rows_hw(rows, Y), 0.3)
        u = (X - 8.0) / hw
        rib = np.floor((u + 1.0) * 4.5)
        base = np.where((rib % 2 == 0)[..., None], lc, dc)
        grain = fbm(X * 6, Y * 0.6, Z * 6, 91, 2)
        base = base * (0.85 + 0.3 * grain[..., None])
        seam = np.abs(((u + 1.0) * 4.5) % 1.0 - 0.5) > 0.44
        return np.where(seam[..., None], base * 0.45, base)
    return paint


def star_lute():
    """The Star Lute: a round-backed lute - a bowl of maple and walnut ribs, a spruce top with a glowing star-shard
    rosette, six silver strings over gold frets, and the pegbox bent sharply back with its ivory pegs."""
    rows = LUTE_ROWS
    els = []
    top = guitar_top(rows, hole=(8.0, 6.1, 0.85), guard=False, star=True, burst=('#8a5a24', 0.45), binding='#3a2414', purfling='#efe2c4')
    els += profile_slab(8.0, rows, 8.4, 9.0, 'walnut', mats={'south': 'spruce'}, paint=chain(top, ribs_paint(rows)))
    for z0, z1, k in ((7.4, 8.4, 0.92), (6.6, 7.4, 0.76), (6.0, 6.6, 0.52)):
        rr = [(a, b, max(0.3, w * k)) for a, b, w in rows]
        els += profile_slab(8.0, rr, z0, z1, 'walnut', paint=ribs_paint(rows))
    nut, scale = 15.6, 12.4
    els.append(El((7.2, 9.4, 7.7), (8.8, 15.6, 8.95), 'walnut', faces=('north', 'west', 'east')))
    els.append(El((7.1, 7.4, 8.95), (8.9, 15.6, 9.25), 'ebony', faces=('south', 'west', 'east', 'down'),
                  paint=frets_paint(nut, scale, 7.1, 8.9, fret_col='#f0c23e', dot_col='#fff2a0', n=10)))
    for k in range(1, 9):
        yf = nut - scale * (1 - 2 ** (-k / 12.0))
        els.append(El((7.1, yf - 0.04, 9.25), (8.9, yf + 0.04, 9.31), 'gold_x', faces=('south', 'up', 'down'), key='fret'))
    els.append(El((7.1, 15.6, 8.9), (8.9, 15.85, 9.4), 'ivory', faces=('south', 'up', 'west', 'east')))
    prot = ('x', -60.0, (8.0, 15.85, 8.6))
    els.append(El((7.15, 15.85, 7.9), (8.85, 19.2, 8.85), 'walnut', rot=prot, mats={'south': 'ebony'}))
    for i, yk in enumerate((16.6, 17.5, 18.4)):
        for sx in (-1, 1):
            x0, x1 = (6.45, 7.15) if sx < 0 else (8.85, 9.55)
            els.append(El((x0, yk - 0.17, 8.2), (x1, yk + 0.17, 8.55), 'ivory', rot=prot, key='peg'))
    els += K.cyl_y(8.0, 8.35, 0.42, 19.2, 19.7, 'star', n=3, rot=prot, glow=13, shade=False)
    els.append(El((6.4, 1.6, 9.0), (9.6, 2.3, 9.35), 'ebony', faces=('south', 'up', 'down', 'west', 'east')))
    xs = [7.35 + i * 0.26 for i in range(6)]
    els += strings(xs, 1.95, 15.7, 9.36, 9.43, 'wire', w=0.06, splits=(5.6, 11.5))
    return els


def prism_harp():
    """The Prism Harp: a golden lyre-harp - a pearl soundbox, a curved golden neck, a forepillar crowned with a
    prism gem, and a string of light for every note, warm red at the bass to violet at the top."""
    els = []
    # base and soundbox (right), forepillar (left), curved neck (top)
    els.append(El((2.6, 0.0, 6.9), (13.2, 1.3, 9.1), 'gold_x', paint=bands_paint([0.65], 0.12, '#fff6c8', 'y')))
    els += K.cyl_y(12.0, 8.0, 1.3, 1.3, 13.4, 'pearl', n=3, caps=('up',))
    els.append(El((11.2, 1.3, 8.95), (12.8, 13.0, 9.35), 'gold', faces=('south', 'west', 'east')))
    els += K.cyl_y(3.4, 8.0, 0.62, 1.3, 15.8, 'gold', n=3, caps=())
    for y, h in ((4.0, 0.5), (8.0, 0.5), (12.0, 0.5)):
        els += K.cyl_y(3.4, 8.0, 0.75, y, y + h, 'gold', n=3, key='pring')
    neck = [(2.8, 15.3, 4.6, 16.6), (4.6, 15.0, 6.4, 16.3), (6.4, 14.5, 8.2, 15.8), (8.2, 13.9, 10.0, 15.2), (10.0, 13.2, 12.6, 14.6)]
    for x0, y0, x1, y1 in neck:
        els.append(El((x0, y0, 7.3), (x1, y1, 8.7), 'gold_x'))
    els += K.cyl_y(3.4, 8.0, 0.75, 15.8, 16.4, 'gold', n=3)
    els.append(El((2.95, 16.4, 7.55), (3.85, 17.6, 8.45), 'prism', glow=14, shade=False))
    els.append(El((3.1, 17.6, 7.7), (3.7, 18.1, 8.3), 'prism_cyan', glow=14, shade=False))
    # thirteen strings of light (every other semitone drawn), bass at the left
    xs = [4.4 + i * 0.6 for i in range(12)]
    tops = [float(np.interp(x, [2.8, 4.6, 6.4, 8.2, 10.0, 12.6], [15.3, 15.0, 14.5, 13.9, 13.2, 13.0])) for x in xs]
    cols = ['harp_s%d' % i for i in range(len(xs))]
    els += strings(xs, 1.3, 15.0, 7.95, 8.05, cols, w=0.09, tops=tops, glow=11)
    # tuning pins along the neck
    for x, t in zip(xs, tops):
        els.append(El((x - 0.1, t + 0.05, 8.7), (x + 0.1, t + 0.4, 8.9), 'ivory', key='pin', faces=('south', 'up', 'west', 'east')))
    return els


HARP_COLOURS = ['#ff6a5a', '#ff9a4a', '#ffc84a', '#f4f06a', '#a8f06a', '#5ae88a', '#4ae8d0', '#4ac8ff', '#5a9aff', '#8a7aff', '#b46aff', '#e86ae8']
for _i, _c in enumerate(HARP_COLOURS):
    K.mat('harp_s%d' % _i, K.gut(_c, '#ffffff'))


# ============================================================================ FLUTES

def crane_flute():
    """The Crane Flute: a side-blown flute of a bamboo cane - two nodes, dark lacquered silk bindings, crane-bone
    end caps, an amethyst ring by the mouth hole and six finger holes."""
    els = []
    holes = [(8.0, y, 0.2) for y in (2.4, 3.35, 4.3, 6.9, 7.85, 8.8)]
    holes.append((8.0, 13.6, 0.27))
    els += K.cyl_y(8.0, 8.0, 0.55, 0.4, 15.6, 'bamboo', n=3, caps=(), paint=chain(nodes_paint((5.6, 11.0)), holes_paint(holes)))
    for y in (1.1, 4.95, 10.2, 14.6):
        els += K.cyl_y(8.0, 8.0, 0.61, y, y + 0.32, 'lacquer_dark', n=3, caps=(), key='bind', paint=bands_paint([y + 0.08, y + 0.24], 0.03, '#d8a84a'))
    els += K.cyl_y(8.0, 8.0, 0.64, 14.95, 15.2, 'amethyst', n=3, caps=())
    els += K.cyl_y(8.0, 8.0, 0.66, -0.2, 0.45, 'bone', n=3, caps=('down',))
    els += K.cyl_y(8.0, 8.0, 0.66, 15.55, 16.25, 'bone', n=3, caps=('up',))
    return els


def nodes_paint(ys):
    def paint(col, X, Y, Z, c):
        for nd in ys:
            k = np.exp(-((Y - nd) ** 2) / 0.006)
            col = mix(col, hexc('#5a5a20'), k * 0.8)
            k2 = np.exp(-((Y - nd - 0.12) ** 2) / 0.01)
            col = mix(col, hexc('#f4f0b8'), k2 * 0.55)
        return col
    return paint


def concert_flute(prism=False):
    """The Silver Flute: a three-joint concert flute of bright iron - lip plate, crown, joint rings, a rod along its
    side and seven key cups that close as you finger lower notes. The Prism Flute: pearl, gold keys, prism gems."""
    els = []
    body, trim, cup = ('pearl', 'gold', 'prism') if prism else ('silver', 'silver', 'silver')
    rod = 'gold' if prism else 'silver'
    els += K.cyl_y(8.0, 8.0, 0.45, 0.0, 15.6, body, n=3, caps=('down',))
    for y in (4.4, 11.8):
        els += K.cyl_y(8.0, 8.0, 0.53, y - 0.18, y + 0.18, trim, n=3, key='joint')
    els += K.cyl_y(8.0, 8.0, 0.5, 15.6, 16.3, trim, n=3, caps=('up',))
    if prism:
        els.append(El((7.7, 16.3, 7.7), (8.3, 16.8, 8.3), 'prism_cyan', glow=13, shade=False))
    # the lip plate with its embouchure hole
    els.append(El((7.45, 13.4, 8.4), (8.55, 14.6, 8.62), trim, faces=('south', 'up', 'down', 'west', 'east'),
                  paint=holes_paint([(8.0, 14.0, 0.26)], face='south', rim='#8a96aa' if not prism else '#c89a2a')))
    els.append(El((8.48, 1.0, 7.9), (8.62, 11.0, 8.1), rod, faces=('south', 'east', 'up', 'down')))
    for i, y in enumerate((1.6, 2.9, 4.0, 5.4, 6.7, 8.0, 9.3)):
        els.append(El((7.68, y - 0.32, 8.42), (8.32, y + 0.32, 8.62), cup, tag='key_%d' % i, key='cup', glow=10 if prism else 0,
                      shade=not prism))
        els.append(El((8.3, y - 0.07, 8.0), (8.55, y + 0.07, 8.5), rod, key='arm', faces=('south', 'up', 'down', 'east')))
    return els


# ============================================================================ DRUMS

def shell_paint(cx, cz, r, y0, y1, staves=10, hoop='#d8dae6', hoop_lo=None, light=None, bolt=False, rope=False):
    """A drum shell: wood staves by angle with dark seams, a polished hoop band at the rim (and the foot), lugs."""
    hc = hexc(hoop)

    def paint(col, X, Y, Z, c):
        if c.face in ('up', 'down'):
            return col
        ang = (np.arctan2(Z - cz, X - cx) / (2 * np.pi) + 0.5) * staves
        seam = np.abs(ang % 1.0 - 0.5) > 0.46
        col = col * (0.88 + 0.22 * white(np.floor(ang), 0, 0, 5)[..., None])
        col = np.where(seam[..., None], col * 0.55, col)
        band = (Y > y1 - 0.55) | (Y < y0 + 0.4)
        shine = 0.75 + 0.4 * np.exp(-((Y - (y1 - 0.3)) ** 2) / 0.02)
        col = np.where(band[..., None], hc * shine[..., None], col)
        if rope:
            zig = np.abs(((Y - y0) / (y1 - y0) * 3 + ang * 0.5) % 1.0 - 0.5) < 0.07
            col = np.where((zig & (Y > y0 + 0.5) & (Y < y1 - 0.6))[..., None], hexc('#e2cfa0'), col)
        if bolt:
            u = (np.arctan2(Z - cz, X - cx) + np.pi / 2) * r
            v = (Y - (y0 + y1) / 2)
            zz = np.abs(u - (np.where(v > 0, 0.3, -0.3) - v * 0.35)) < 0.22
            col = np.where((zz & (np.abs(v) < 1.6) & (np.abs(u) < 1.0))[..., None], hexc('#ffd54a'), col)
        return col
    return paint


def head_paint(cx, cz, r, base='#f4e0b6', rim='#7a5a3a', glow=False):
    b, rm = hexc(base), hexc(rim)

    def paint(col, X, Y, Z, c):
        if c.face != 'up':
            return col
        rr = np.sqrt((X - cx) ** 2 + (Z - cz) ** 2) / r
        n = fbm(X * 1.4, Z * 1.4, 3.0, 17, 4)
        hc = mix(b, b * 0.82, np.clip((0.55 - n) * 1.6, 0, 1))
        hc = mix(hc, b * 0.78, np.exp(-((rr - 0.38) ** 2) / 0.015) * 0.5)
        hc = mix(hc, rm, smooth(0.8, 0.97, rr))
        if glow:
            hc = mix(hc, hexc('#ffffff'), np.exp(-(rr ** 2) / 0.08) * 0.5)
        return hc
    return paint


def drum(cx, cz, r, y0, y1, side, tag, hoop, head='hide', staves=10, bolt=False, rope=False, glow=0, head_base='#f4e0b6', lugs=4, lug='iron'):
    """One drum: a shell with an inner floor, a separate head that can dip, and lugs round the shell."""
    els = []
    els += cyl_y(cx, cz, r, y0, y1 - 0.34, side, n=5, caps=('up', 'down'), paint=shell_paint(cx, cz, r, y0, y1, staves, hoop, bolt=bolt, rope=rope),
                 mats={'up': 'hole'})
    els += cyl_y(cx, cz, r - 0.03, y1 - 0.3, y1, head, n=5, caps=('up',), tag=tag, glow=glow, shade=not glow,
                 paint=head_paint(cx, cz, r - 0.03, base=head_base, glow=bool(glow)))
    for k in range(lugs):
        a = (k + 0.5) * 2 * math.pi / lugs + math.pi / 4
        lx, lz = cx + math.cos(a) * (r * 0.96), cz + math.sin(a) * (r * 0.96)
        els.append(El((lx - 0.22, y1 - 2.2, lz - 0.22), (lx + 0.22, y1 - 0.5, lz + 0.22), lug, key='lug'))
    return els


K.mat('hide', K.hide('#eadcbc', '#b8a27a', '#fff4dc', 4))


def conga_drum():
    """The Conga Drum: a pair of hand drums joined by a block - a tall tumba and a smaller quinto of oak staves,
    copper hoops and lugs, pale rawhide heads - on a leather strap."""
    els = []
    # model +x is the player's left: the big low tumba sits on the right (right hand), the quinto on the left
    els += drum(5.1, 8.0, 2.85, 0.0, 6.4, 'oak', 'head_r', '#d98a5a', staves=11, lug='copper')
    els += drum(11.2, 8.0, 2.35, 0.8, 6.0, 'oak', 'head_l', '#d98a5a', staves=9, lug='copper')
    els.append(El((7.6, 2.4, 7.2), (9.0, 4.6, 8.8), 'oak', mats={'up': 'oak', 'down': 'oak'}))
    els.append(El((7.4, 3.2, 7.0), (9.2, 3.8, 9.0), 'copper_x', key='bridgeband'))
    # the leather strap over both drums
    els.append(El((2.0, 5.2, 7.75), (2.35, 7.8, 8.25), 'strap'))
    els.append(El((13.65, 4.8, 7.75), (14.0, 7.8, 8.25), 'strap'))
    els.append(El((2.0, 7.8, 7.75), (14.0, 8.15, 8.25), 'strap'))
    return els


def thunder_drums():
    """The Thunder Drums: four lacquered war drums on an iron frame - red shells with iron hoops and gold lightning
    bolts, thick hide heads - two big ones behind, two smaller in front."""
    els = []
    els += drum(5.0, 6.2, 2.25, 1.0, 7.2, 'lacquer', 'head_r', '#c8ccd4', staves=8, bolt=True)
    els += drum(11.0, 6.2, 2.25, 1.0, 7.2, 'lacquer', 'head_l', '#c8ccd4', staves=8, bolt=True)
    els += drum(5.6, 11.0, 1.8, 0.4, 5.6, 'lacquer', 'head_r', '#c8ccd4', staves=7)
    els += drum(10.4, 11.0, 1.8, 0.4, 5.6, 'lacquer', 'head_l', '#c8ccd4', staves=7)
    els.append(El((2.4, 0.0, 8.2), (13.6, 0.5, 9.0), 'dark_iron'))
    els.append(El((7.6, 0.0, 4.2), (8.4, 0.5, 13.0), 'dark_iron'))
    els.append(El((7.7, 0.5, 7.9), (8.3, 6.4, 8.5), 'dark_iron'))
    els.append(El((2.6, 6.4, 7.95), (13.4, 6.8, 8.45), 'dark_iron'))
    return els


def prism_drum():
    """The Prism Drum: a pair of golden kettle drums with pearl hoops and glowing heads of light, joined by a block
    set with a prism gem."""
    els = []
    els += drum(5.1, 8.0, 2.8, 0.0, 6.2, 'gold', 'head_r', '#f6eedb', staves=12, head='prism_head', glow=12, head_base='#ffe0fb', lug='gold')
    els += drum(11.2, 8.0, 2.35, 0.8, 5.9, 'gold', 'head_l', '#f6eedb', staves=10, head='prism_head', glow=12, head_base='#d8fbff', lug='gold')
    els.append(El((7.6, 2.4, 7.2), (9.0, 4.6, 8.8), 'pearl'))
    els.append(El((7.85, 3.0, 8.8), (8.75, 4.0, 9.15), 'prism', glow=14, shade=False))
    els.append(El((2.0, 5.2, 7.75), (2.35, 7.8, 8.25), 'gold'))
    els.append(El((13.65, 4.8, 7.75), (14.0, 7.8, 8.25), 'gold'))
    els.append(El((2.0, 7.8, 7.75), (14.0, 8.15, 8.25), 'gold_x'))
    return els


K.mat('prism_head', K.solid('#fff4ff', 0.04, 70, '#e8c8ff'))


# ============================================================================ CHIMES

def chimes(n, xs, lengths, top, bar, tube_mat, r, cord='rope', glow=0, clapper='amethyst', sail=True):
    """A row of tuned tubes hung by cords from a bar: each tube with its cord is one tagged part (``tube_<i>``) that
    swings about the point where its cord meets the bar."""
    els = []
    for i, (x, length) in enumerate(zip(xs, lengths)):
        tag = 'tube_%d' % i
        els.append(El((x - 0.05, top, 7.95), (x + 0.05, bar, 8.05), cord, tag=tag, key='cord', faces=('south', 'north', 'west', 'east')))
        els += K.cyl_y(x, 8.0, r, top - length, top, tube_mat, n=3, tag=tag, glow=glow, shade=not glow,
                       paint=tube_paint(top - length, top))
    return els


def tube_paint(y0, y1):
    def paint(col, X, Y, Z, c):
        if c.face in ('up', 'down'):
            return col * 0.8
        # a bright reflection running down each tube, and the darker ends
        k = np.exp(-((c.s - 0.3) ** 2) / 0.012)
        col = mix(col, hexc('#ffffff'), k * 0.45)
        col = np.where(((Y < y0 + 0.18) | (Y > y1 - 0.18))[..., None], col * 0.7, col)
        return col
    return paint


def wind_chimes():
    """The Wind Chimes: seven copper tubes on string from an oak crossbar, a copper hanging ring, and an amethyst
    striker with an oak wind-sail at the centre."""
    els = []
    xs = [2.6 + i * 1.8 for i in range(7)]
    lengths = [9.4, 8.6, 7.8, 7.1, 6.4, 5.8, 5.2]
    els += chimes(7, xs, lengths, 12.7, 14.0, 'copper', 0.36)
    els.append(El((1.6, 14.0, 7.55), (14.4, 14.75, 8.45), 'oak_x'))
    els.append(El((1.35, 13.95, 7.5), (1.6, 14.8, 8.5), 'copper', key='cap'))
    els.append(El((14.4, 13.95, 7.5), (14.65, 14.8, 8.5), 'copper', key='cap'))
    # the hanging ring
    els.append(El((7.85, 14.75, 7.9), (8.15, 15.4, 8.1), 'rope'))
    els.append(El((7.3, 15.4, 7.85), (8.7, 15.65, 8.15), 'copper_x'))
    els.append(El((7.3, 16.5, 7.85), (8.7, 16.75, 8.15), 'copper_x'))
    els.append(El((7.05, 15.4, 7.85), (7.3, 16.75, 8.15), 'copper'))
    els.append(El((8.7, 15.4, 7.85), (8.95, 16.75, 8.15), 'copper'))
    # striker and sail behind the tubes
    els.append(El((7.97, 4.6, 7.0), (8.03, 14.0, 7.06), 'rope', faces=('south', 'north', 'west', 'east')))
    els.append(El((7.3, 8.4, 6.8), (8.7, 9.0, 7.3), 'amethyst'))
    els.append(El((7.0, 2.2, 6.98), (9.0, 4.6, 7.08), 'oak', paint=mark_paint(8.0, 3.5, '#3a2414', size=0.9)))
    return els


def glass_bells():
    """The Glass Bells: ten tubes of chime glass on silk cords from an iron bar with an iron ring."""
    els = []
    xs = [1.9 + i * 1.36 for i in range(10)]
    lengths = [10.2, 9.5, 8.8, 8.2, 7.6, 7.0, 6.5, 6.0, 5.5, 5.0]
    els += chimes(10, xs, lengths, 12.9, 14.1, 'chime_glass', 0.34, cord='silk', glow=4)
    els.append(El((1.0, 14.1, 7.6), (15.0, 14.7, 8.4), 'iron_x'))
    els.append(El((7.85, 14.7, 7.9), (8.15, 15.3, 8.1), 'iron'))
    els.append(El((7.3, 15.3, 7.85), (8.7, 15.55, 8.15), 'iron_x'))
    els.append(El((7.3, 16.4, 7.85), (8.7, 16.65, 8.15), 'iron_x'))
    els.append(El((7.05, 15.3, 7.85), (7.3, 16.65, 8.15), 'iron'))
    els.append(El((8.7, 15.3, 7.85), (8.95, 16.65, 8.15), 'iron'))
    return els


def prism_chimes():
    """The Prism Chimes: a golden halo hung from a gem, ten pearl tubes round it, each tipped with a facet of light."""
    els = []
    # the halo: eight golden bars round a circle (each turned about the centre)
    R = 5.0
    side = 2 * R * math.tan(math.pi / 8) + 0.12
    sides = {
        'south': ((8.0 - side / 2, 13.6, 8.0 + R - 0.3), (8.0 + side / 2, 14.2, 8.0 + R + 0.3), 'gold_x'),
        'north': ((8.0 - side / 2, 13.6, 8.0 - R - 0.3), (8.0 + side / 2, 14.2, 8.0 - R + 0.3), 'gold_x'),
        'east': ((8.0 + R - 0.3, 13.6, 8.0 - side / 2), (8.0 + R + 0.3, 14.2, 8.0 + side / 2), 'gold'),
        'west': ((8.0 - R - 0.3, 13.6, 8.0 - side / 2), (8.0 - R + 0.3, 14.2, 8.0 + side / 2), 'gold'),
    }
    for frm, to, m in sides.values():
        els.append(El(frm, to, m, key='halo'))
        els.append(El(frm, to, m, key='halo', rot=('y', 45.0, (8.0, 13.9, 8.0))))
    # four golden hangers leaning in to the gem above the halo
    L = 5.41
    els.append(El((7.88, 13.9, 7.88 + R), (8.12, 13.9 + L, 8.12 + R), 'gold', rot=('x', -67.5, (8.0, 13.9, 8.0 + R)), key='hanger'))
    els.append(El((7.88, 13.9, 7.88 - R), (8.12, 13.9 + L, 8.12 - R), 'gold', rot=('x', 67.5, (8.0, 13.9, 8.0 - R)), key='hanger'))
    els.append(El((7.88 + R, 13.9, 7.88), (8.12 + R, 13.9 + L, 8.12), 'gold', rot=('z', 67.5, (8.0 + R, 13.9, 8.0)), key='hanger'))
    els.append(El((7.88 - R, 13.9, 7.88), (8.12 - R, 13.9 + L, 8.12), 'gold', rot=('z', -67.5, (8.0 - R, 13.9, 8.0)), key='hanger'))
    # tubes round the halo, longest at the front left
    for i in range(10):
        th = math.radians(-162 + i * 36)
        x, z = 8.0 + 4.6 * math.cos(th), 8.0 + 4.6 * math.sin(th)
        length = 9.0 - i * 0.45
        tag = 'tube_%d' % i
        els.append(El((x - 0.05, 12.8, z - 0.05), (x + 0.05, 13.7, z + 0.05), 'gold', tag=tag, key='cord'))
        els += K.cyl_y(x, z, 0.32, 12.8 - length, 12.8, 'pearl', n=3, tag=tag, paint=tube_paint(12.8 - length, 12.8))
        els.append(El((x - 0.28, 12.8 - length - 0.55, z - 0.28), (x + 0.28, 12.8 - length, z + 0.28), 'harp_s%d' % (i + 1), tag=tag, glow=13,
                      shade=False, key='tip%d' % i))
    els.append(El((7.4, 15.6, 7.4), (8.6, 16.8, 8.6), 'prism', glow=14, shade=False))
    els.append(El((7.55, 8.0, 7.55), (8.45, 9.2, 8.45), 'prism_cyan', glow=13, shade=False))
    els.append(El((7.97, 9.2, 7.97), (8.03, 13.9, 8.03), 'gold', faces=('south', 'north', 'west', 'east')))
    return els


# ============================================================================ every instrument

INSTRUMENTS = {
    # id: builder, family (stance), stance anchor (model point in the right hand), free-hand grips, carry anchor, GUI turn
    'guitar': dict(build=lambda: guitar('guitar'), fam='strings', anchor=(8.0, 4.2, 9.45), grips={'lo': (8.0, 16.3, 9.0), 'hi': (8.0, 12.2, 9.0)},
                   carry=(8.0, 14.5, 8.2), gui=(10, 30, -45)),
    'weaver_guitar': dict(build=lambda: guitar('weaver'), fam='strings', anchor=(8.0, 4.2, 9.45), grips={'lo': (8.0, 16.3, 9.0), 'hi': (8.0, 12.2, 9.0)},
                          carry=(8.0, 14.5, 8.2), gui=(10, 30, -45)),
    'star_lute': dict(build=star_lute, fam='strings', anchor=(8.0, 3.8, 9.4), grips={'lo': (8.0, 14.9, 9.0), 'hi': (8.0, 11.0, 9.0)},
                      carry=(8.0, 13.5, 8.3), gui=(10, 30, -45)),
    'prism_harp': dict(build=prism_harp, fam='harp', anchor=(10.4, 6.0, 8.0), grips={'lo': (4.6, 7.5, 8.0), 'hi': (9.8, 7.5, 8.0)},
                       carry=(7.0, 15.2, 8.0), gui=(5, -25, 0)),
    'crane_flute': dict(build=crane_flute, fam='flute', anchor=(8.0, 4.0, 8.4), grips={'lo': (8.0, 8.0, 8.4), 'hi': (8.0, 10.0, 8.4)},
                        carry=(8.0, 2.5, 8.0), gui=(10, 30, -45)),
    'serbim_flute': dict(build=lambda: concert_flute(False), fam='flute', anchor=(8.0, 4.0, 8.3), grips={'lo': (8.0, 8.0, 8.3), 'hi': (8.0, 10.0, 8.3)},
                         carry=(8.0, 2.5, 8.0), gui=(10, 30, -45)),
    'prism_flute': dict(build=lambda: concert_flute(True), fam='flute', anchor=(8.0, 4.0, 8.3), grips={'lo': (8.0, 8.0, 8.3), 'hi': (8.0, 10.0, 8.3)},
                        carry=(8.0, 2.5, 8.0), gui=(10, 30, -45)),
    'conga_drum': dict(build=conga_drum, fam='drum', anchor=(5.1, 6.4, 8.0), grips={'lo': (11.2, 6.0, 8.0), 'hi': (11.2, 6.0, 8.0)},
                       carry=(8.0, 8.0, 8.0), gui=(28, -38, 0)),
    'thunder_drums': dict(build=thunder_drums, fam='drum', anchor=(5.0, 7.2, 6.2), grips={'lo': (10.4, 5.6, 11.0), 'hi': (11.0, 7.2, 6.2)},
                          carry=(8.0, 6.8, 8.2), gui=(28, -38, 0)),
    'prism_drum': dict(build=prism_drum, fam='drum', anchor=(5.1, 6.2, 8.0), grips={'lo': (11.2, 5.9, 8.0), 'hi': (11.2, 5.9, 8.0)},
                       carry=(8.0, 8.0, 8.0), gui=(28, -38, 0)),
    'wind_chimes': dict(build=wind_chimes, fam='chimes', anchor=(8.0, 16.6, 8.0), grips={'lo': (2.6, 8.0, 8.4), 'hi': (13.4, 10.0, 8.4)},
                        carry=(8.0, 16.6, 8.0), gui=(8, -20, 0)),
    'glass_bells': dict(build=glass_bells, fam='chimes', anchor=(8.0, 16.5, 8.0), grips={'lo': (1.9, 8.0, 8.4), 'hi': (14.1, 10.0, 8.4)},
                        carry=(8.0, 16.5, 8.0), gui=(8, -20, 0)),
    'prism_chimes': dict(build=prism_chimes, fam='chimes', anchor=(8.0, 16.2, 8.0), grips={'lo': (3.6, 7.0, 6.6), 'hi': (12.0, 8.5, 10.0)},
                         carry=(8.0, 16.2, 8.0), gui=(18, -20, 0)),
}

# the order of each family's frames (the client property thesift:instrument_play returns 1 + the index; 0 = at rest)
FRAMES = {'strings': ['a', 'b'], 'harp': ['a', 'b'], 'drum': ['l', 'r', 'up'], 'chimes': ['a', 'b', 'c', 'd'], 'flute': ['k6', 'k4', 'k2', 'k0']}


def frames(fam, els):
    """The moving parts, frame by frame."""
    out = {}
    if fam in ('strings', 'harp'):
        for name, sgn in (('a', 1), ('b', -1)):
            fe = []
            for i, e in enumerate(els):
                if e.tag in ('str_lo', 'str_mid', 'str_hi'):
                    amp = 0.13 if e.tag == 'str_mid' else 0.05
                    k = (1 if (i // 3) % 2 == 0 else -1) * sgn
                    n = e.moved(dx=amp * k)
                    if e.tag == 'str_mid':
                        w = (e.to[0] - e.frm[0]) * 0.6
                        n.frm[0] -= w
                        n.to[0] += w
                    fe.append(n)
                else:
                    fe.append(e)
            out[name] = fe
    elif fam == 'drum':
        for name, dl, dr in (('l', -0.26, 0.04), ('r', 0.04, -0.26), ('up', 0.1, 0.1)):
            out[name] = [e.moved(dy=dl) if e.tag == 'head_l' else e.moved(dy=dr) if e.tag == 'head_r' else e for e in els]
    elif fam == 'chimes':
        for name, amp, ph in (('a', 16.0, 0.0), ('b', -16.0, 0.0), ('c', 7.0, 1.3), ('d', -7.0, 1.3)):
            fe = []
            for e in els:
                if e.tag and e.tag.startswith('tube_'):
                    i = int(e.tag[5:])
                    ang = amp * math.cos(i * 0.9 + ph)
                    n = e.copy()
                    pivot = (e.frm[0] + e.to[0]) / 2
                    top = max(x.to[1] for x in els if x.tag == e.tag)
                    cz = (e.frm[2] + e.to[2]) / 2
                    if e.rot:
                        fe.append(n)
                        continue
                    n.rot = ('z', round(ang, 2), (pivot, top, cz))
                    fe.append(n)
                else:
                    fe.append(e)
            out[name] = fe
    elif fam == 'flute':
        if any(e.tag and e.tag.startswith('key_') for e in els):
            for name, closed in (('k6', 6), ('k4', 4), ('k2', 2), ('k0', 0)):
                out[name] = [e.moved(dz=-0.14) if e.tag and e.tag.startswith('key_') and int(e.tag[4:]) < closed else e for e in els]
    return out


# ============================================================================ how each family is held
# World space: x = the player's left, y = up, z = forward (the player faces +z). Model space (Java ModelPart):
# pixels, x = left, y = DOWN from the neck, z = BACKWARD. ``hand``: the right fist's target, model space.

FAMILY = {
    'strings': dict(hand=(-1.7, 10.3, -4.4), up=(0.75, 0.5, 0.43), front=(-0.45, 0.15, 0.88), scale=0.9, nudge=(0.0, 0.0, 0.02),
                    fp_base=(0.44, -0.46, -0.8), fp_up=(-0.85, 0.48, -0.2), fp_front=(0.15, 0.3, 0.94), fp_scale=0.62),
    'harp': dict(hand=(-2.2, 7.5, -7.4), up=(0.05, 1.0, 0.12), front=(0.25, 0.1, -1.0), scale=0.75, nudge=(0.0, 0.0, -0.02),
                 fp_base=(0.4, -0.3, -0.85), fp_up=(0.05, 1.0, 0.1), fp_front=(-0.35, 0.0, 0.94), fp_scale=0.5),
    'flute': dict(hand=(-4.6, -2.6, -8.9), mouth=(0.6, -2.0, -4.7), front=(0.0, 1.0, -0.3), scale=None, nudge=(0.0, -0.01, 0.0),
                  fp_base=(0.4, -0.22, -0.55), fp_up=(-0.88, -0.12, 0.46), fp_front=(0.05, 0.95, 0.2), fp_scale=0.55),
    'drum': dict(hand=(-2.4, 9.4, -6.6), up=(0.0, 1.0, -0.25), front=(0.0, 0.25, 1.0), scale=0.75, nudge=(0.0, -0.04, 0.0),
                 fp_base=(0.12, -0.33, -0.68), fp_up=(0.0, 0.75, 0.66), fp_front=(0.0, 0.66, -0.75), fp_scale=0.45),
    'chimes': dict(hand=(-6.2, -2.2, -8.9), up=(0.0, 1.0, 0.0), front=(-0.5, 0.0, 0.87), scale=0.7, nudge=(0.0, 0.03, 0.0),
                   fp_base=(0.36, 0.08, -0.85), fp_up=(0.0, 1.0, 0.0), fp_front=(-0.45, 0.0, 0.9), fp_scale=0.55),
}
# the flute anchor's distance to its mouth hole decides the scale (the mouth hole sits at the lips)
FLUTE_MOUTH_Y = 14.0


def model_to_world(p):
    return np.array([p[0] / 16.0, 1.501 - p[1] / 16.0, -p[2] / 16.0])


def world_to_model(w):
    return [round(float(w[0]) * 16, 2), round((1.501 - float(w[1])) * 16, 2), round(-float(w[2]) * 16, 2)]


def ik(target, arm='left'):
    """Arm angles (x, y, 0) pointing the arm from its shoulder at a model-space target (InstrumentPoses.reach)."""
    p = K.ARM_PIVOT[arm]
    d = np.array(target, dtype=float) - np.array(p)
    d /= np.linalg.norm(d)
    a = -math.acos(max(-1.0, min(1.0, d[1])))
    s = math.sin(a)
    b = math.atan2(d[0] / s, d[2] / s) if abs(s) > 1e-6 else 0.0
    return (a, b, 0.0)


def arm_angles(fam):
    return ik(FAMILY[fam]['hand'], 'right')


def place(iid, els, spec):
    """Display transforms for the stance (third and first person), and the free hand's grips in model space."""
    fam = spec['fam']
    f = FAMILY[fam]
    arm = arm_angles(fam)
    chain = K.arm_chain(arm)
    hand = K.hand_point(arm)
    anchor = spec['anchor']
    if fam == 'flute':
        mouth = model_to_world(f['mouth'])
        axis = mouth - hand
        scale = float(np.linalg.norm(axis) / ((FLUTE_MOUTH_Y - anchor[1]) / 16.0))
        R = K.basis(axis, f['front'])
    else:
        scale = f['scale']
        R = K.basis(f['up'], f['front'])
    tp = K.solve_display(chain, R, anchor, hand + np.array(f['nudge']), round(scale, 4))
    fp = K.solve_display(K._T(*f['fp_base']), K.basis(f['fp_up'], f['fp_front']), anchor, f['fp_base'], f['fp_scale'])
    disp = K._T(*(np.array(tp['translation']) / 16.0)) @ K._RX(tp['rotation'][0] * K.D2R) @ K._RY(tp['rotation'][1] * K.D2R) \
        @ K._RZ(tp['rotation'][2] * K.D2R) @ np.diag([tp['scale'][0]] * 3 + [1.0]) @ K._T(-0.5, -0.5, -0.5)
    grips = {k: world_to_model((chain @ disp @ np.append(np.asarray(g, dtype=float) / 16.0, 1.0))[:3]) for k, g in spec['grips'].items()}
    return {'thirdperson_righthand': tp, 'thirdperson_lefthand': K.mirror_left(tp), 'firstperson_righthand': fp,
            'firstperson_lefthand': K.mirror_left(fp)}, grips


CARRY = {
    # how each family is carried at rest: the instrument's up axis and face (world) under an idle arm, first person
    'strings': dict(up=(0, 1, 0.15), front=(-1, 0, 0.2), scale=0.8, fp_up=(-0.25, 0.9, -0.35), fp_front=(-0.9, 0.1, 0.45), fp_scale=0.6),
    'harp': dict(up=(0, 1, 0.1), front=(-1, 0, 0.3), scale=0.7, fp_up=(-0.2, 0.95, -0.2), fp_front=(-0.8, 0.0, 0.6), fp_scale=0.5),
    'flute': dict(up=(0, 0.75, 0.66), front=(-1, 0, 0), scale=0.75, fp_up=(-0.35, 0.8, -0.5), fp_front=(-0.9, 0.1, 0.4), fp_scale=0.6),
    'drum': dict(up=(0, 1, 0), front=(-1, 0, 0.15), scale=0.62, fp_up=(0.0, 0.85, 0.5), fp_front=(-0.6, -0.3, 0.7), fp_scale=0.45),
    'chimes': dict(up=(0, 1, 0), front=(-0.6, 0, 0.8), scale=0.6, fp_up=(0, 1, 0), fp_front=(-0.5, 0, 0.85), fp_scale=0.45),
}


def rest_display(els, spec):
    fam = spec['fam']
    c = CARRY[fam]
    idle = (-math.pi / 10, 0.0, 0.0)
    chain = K.arm_chain(idle)
    tp = K.solve_display(chain, K.basis(c['up'], c['front']), spec['carry'], K.hand_point(idle), c['scale'])
    fpc = K._T(0.56, -0.52, -0.72)
    fp = K.solve_display(fpc, K.basis(c['fp_up'], c['fp_front']), spec['carry'], (0.56, -0.5, -0.72), c['fp_scale'])
    return {
        'gui': K.fit_display(els, list(spec['gui']), 15.4),
        'ground': K.fit_display(els, [0, 0, 0], 10.0),
        'fixed': K.fit_display(els, [0, 180, spec['gui'][2]], 14.0),
        'head': K.fit_display(els, [0, 180, 0], 12.0),
        'thirdperson_righthand': tp, 'thirdperson_lefthand': K.mirror_left(tp),
        'firstperson_righthand': fp, 'firstperson_lefthand': K.mirror_left(fp),
    }


_CACHE = {}


def build(iid):
    """The finished instrument: kit object (texture + layout), rest/play displays, frames and grips."""
    if iid in _CACHE:
        return _CACHE[iid]
    spec = INSTRUMENTS[iid]
    els = spec['build']()
    fam = spec['fam']
    inst = K.Instrument3D(iid, els, density=spec.get('density', K.DENSITY))
    # the frames reuse the base elements' texture patches (copies made after the layout carry their uv)
    fr = frames(fam, els)
    play, grips = place(iid, els, spec)
    res = dict(inst=inst, fam=fam, rest=rest_display(els, spec), play=play, frames=fr, grips=grips, arm=arm_angles(fam))
    _CACHE[iid] = res
    return res
