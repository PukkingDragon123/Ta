"""Siftite's own silhouettes: curved, almost living tools that look sung into shape rather than
forged. Each tool is a few brush strokes along Catmull-Rom curves - the handle, then the blade -
shaded by which way each pixel faces the top-left light, outlined by items16.grid, and given an
engraved centre line (the "staff line" the travelling light runs along) with inlaid notes.

items16.music_frames() uses curved_tool(name) for these tools in place of the vanilla shapes.
"""
from __future__ import annotations

import math

import items16 as I

LIGHT = (-0.7071, -0.7071)
# blade chars dark to light; the handle is plum leather like the spear's grip
BLADE = '12345'
GRIP = 'abcd'


def _catmull(pts, n=80):
    out = []
    p = [pts[0]] + list(pts) + [pts[-1]]
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        for k in range(n):
            t = k / n
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2
                                    + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in (0, 1)))
    out.append(pts[-1])
    return out


def _stroke(cells, pts, w0, w1, chars, centre=None):
    """Paints a tapering stroke (width w0 at the start to w1 at the end) into cells; returns the
    centre-line pixels in order if centre is set."""
    path = _catmull(pts)
    line = []
    for i, (x, y) in enumerate(path):
        a = path[max(0, i - 1)]
        b = path[min(len(path) - 1, i + 1)]
        tx, ty = b[0] - a[0], b[1] - a[1]
        ln = math.hypot(tx, ty) or 1.0
        nx, ny = -ty / ln, tx / ln
        f = i / max(1, len(path) - 1)
        half = (w0 + (w1 - w0) * f) / 2
        steps = max(1, int(half * 6))
        for s in range(-steps, steps + 1):
            off = half * s / steps
            px, py = int(math.floor(x + nx * off)), int(math.floor(y + ny * off))
            if not (0 <= px < 16 and 0 <= py < 16):
                continue
            facing = (nx * LIGHT[0] + ny * LIGHT[1]) * (1 if off >= 0 else -1)
            edge = abs(off) / max(half, 0.01)
            if edge > 0.66:
                k = len(chars) - 1 if facing > 0.2 else 0
            elif edge > 0.33:
                k = len(chars) - 2 if facing > 0 else 1
            else:
                k = len(chars) // 2
            cells[(px, py)] = chars[max(0, min(len(chars) - 1, k))]
        if centre:
            c = (int(math.floor(x)), int(math.floor(y)))
            if 0 <= c[0] < 16 and 0 <= c[1] < 16 and (not line or line[-1] != c):
                line.append(c)
    return line


# per tool: the grip, then the blade (control points, start width, end width), and where on the
# blade's centre line (0..1) the inlaid notes sit
SHAPES = {
    # a crescent shamshir that hooks back at the tip, a clef-like curl for a guard
    'sword': dict(grip=[(0.8, 15.2), (3.6, 12.4)], guard=[(2.2, 10.4), (3.8, 11.6), (5.8, 12.6), (6.4, 11.6)],
                  blade=[(4.2, 11.6), (5.6, 8.4), (7.4, 5.4), (9.8, 3.0), (12.6, 1.8), (14.4, 2.6), (14.2, 4.4)], w=(2.8, 1.0),
                  notes=(0.25, 0.55, 0.82)),
    # a pick whose head is a new moon, both points curling back in like fiddle heads
    'pickaxe': dict(grip=[(1.5, 14.5), (5.0, 11.0), (8.6, 7.4)],
                    blade=[(4.4, 4.6), (3.4, 2.6), (5.4, 1.2), (8.4, 2.0), (11.0, 4.0), (13.4, 7.0), (14.4, 10.2), (12.6, 11.8), (11.4, 10.6)],
                    w=(1.4, 1.4), mid=2.6, notes=(0.3, 0.5, 0.72)),
    # a broad crescent axe blade sweeping round from the haft
    'axe': dict(grip=[(1.5, 14.5), (5.0, 11.0), (9.4, 6.6)],
                blade=[(8.0, 5.2), (8.6, 2.2), (11.4, 1.2), (14.2, 2.8), (14.6, 6.2), (12.6, 8.8), (10.4, 8.4)], w=(1.4, 1.6), mid=3.4,
                notes=(0.35, 0.6)),
    # a leaf-shaped scoop whose tip rolls into a spiral
    'shovel': dict(grip=[(1.5, 14.5), (4.4, 11.6), (7.4, 8.6)],
                   blade=[(7.2, 8.8), (9.2, 6.0), (11.6, 3.6), (13.8, 2.0), (14.6, 3.6), (13.2, 4.4)], w=(4.0, 1.0),
                   notes=(0.3, 0.6)),
    # a sickle hook curling over the top of the haft
    'hoe': dict(grip=[(1.5, 14.5), (5.0, 11.0), (9.6, 6.4)],
                blade=[(9.2, 7.2), (10.6, 4.4), (9.6, 1.8), (6.6, 1.2), (4.2, 2.4), (3.4, 4.6), (4.6, 5.8)], w=(2.4, 1.0),
                notes=(0.3, 0.7)),
}
CURVED = tuple(SHAPES)


def curved_tool(name):
    """(base image, engraved centre line, notes) for a curved Siftite tool."""
    s = SHAPES[name]
    cells = {}
    _stroke(cells, s['grip'], 1.0, 1.0, GRIP)
    w0, w1 = s['w']
    if 'mid' in s:
        # thick in the middle, thin at both points: draw the two halves toward the middle
        pts = s['blade']
        h = len(pts) // 2
        line = _stroke(cells, pts[:h + 1], w0, s['mid'], BLADE, centre=True)
        line += _stroke(cells, pts[h:], s['mid'], w1, BLADE, centre=True)[1:]
    else:
        line = _stroke(cells, s['blade'], w0, w1, BLADE, centre=True)
    if 'guard' in s:
        # the clef-like guard curls over the blade's root
        _stroke(cells, s['guard'], 1.0, 0.8, 'PpPq')
    # keep the engraving inside the blade, off its outermost pixels
    line = [p for p in line if cells.get(p) in BLADE]
    trim = max(1, len(line) // 10)
    line = line[trim:-trim] if len(line) > 2 * trim + 2 else line
    notes = []
    for f in s['notes']:
        if line:
            notes.append(line[min(len(line) - 1, int(f * len(line)))])
    rows = []
    for y in range(16):
        rows.append(''.join(cells.get((x, y), '.') for x in range(16)))
    R = I.S_RAMP
    pal = {'1': (R[0], I.S_OUT), '2': (R[1], I.S_OUT), '3': (R[2], I.S_OUT), '4': (R[3], I.S_OUT), '5': (R[5], I.S_OUT),
           'a': ('#3a1a3c', '#1c0c20'), 'b': ('#5e2a56', '#1c0c20'), 'c': ('#94507e', '#1c0c20'), 'd': ('#b06c98', '#1c0c20'),
           'P': (I.PINK[1], I.PINK[0]), 'p': (I.PINK[2], I.PINK[0]), 'q': (I.PINK[3], I.PINK[0])}
    img = I.grid(rows, pal, ol=True)
    line = [p for p in line if p not in notes]
    return img, line, notes


ECHO = '#3ff5e6'
ECHO_DIM = '#22c7c4'


def echo_lattice(img, mid_tones):
    """Patterned like cut diamond: a diagonal lattice across the armour's plates, glowing echo-teal
    where its lines cross. Only touches the plate's mid tones, never outlines or highlights."""
    px = img.load()
    mids = {I.rgba(c)[:3] for c in mid_tones}
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            c = px[x, y]
            if c[3] == 0 or c[:3] not in mids:
                continue
            a, b = (x + y) % 4 == 0, (x - y) % 4 == 0
            if a and b:
                px[x, y] = I.rgba(ECHO)
            elif a or b:
                px[x, y] = I.rgba(I.mix(c[:3], ECHO_DIM, 0.35))
    return img
