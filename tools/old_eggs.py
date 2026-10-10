"""Classic two-colour spawn eggs (the user: "use old version eggs, Minecraft with just 2 colours").

Before 1.21.5 every spawn egg was one grey egg tinted with a base colour plus a spotted overlay tinted with a second
colour. This pass redraws every Sift spawn egg that way, after all the generators have run: the two colours are taken
from the egg each creature's generator already painted (its most common body colour, and the most common clearly
different colour as the spots), so every egg keeps its creature's colours.
"""
import glob
import json
import os

from PIL import Image

# the classic egg: brightness steps (4 rim .. 9 highlight), lit from the top left
EGG = [
    '................',
    '......4444......',
    '.....488884.....',
    '....48899884....',
    '....48999884....',
    '...4889988874...',
    '...4888888874...',
    '..488888887774..',
    '..488888877774..',
    '..488887777774..',
    '..477777777664..',
    '..477777776664..',
    '...4777776664...',
    '...4466666644...',
    '.....444444.....',
    '................',
]
# the classic spots
SPOTS = [
    '................',
    '................',
    '................',
    '......SS........',
    '......SS...S....',
    '...........SS...',
    '....S...........',
    '...SS....S......',
    '...S....SS......',
    '........S...S...',
    '.....SS.....SS..',
    '.....SS.........',
    '.........S......',
    '........SS......',
    '................',
    '................',
]
LEVEL = {'4': 0.58, '6': 0.78, '7': 0.89, '8': 1.0, '9': 1.16}


def _tint(c, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c)


def _colours(path):
    """(base, spots) from an existing egg sprite."""
    im = Image.open(path).convert('RGBA')
    counts = {}
    for r, g, b, a in im.getdata():
        if a < 128:
            continue
        luma = 0.299 * r + 0.587 * g + 0.114 * b
        if luma < 28:
            continue
        key = (r // 24, g // 24, b // 24)
        n, sr, sg, sb = counts.get(key, (0, 0, 0, 0))
        counts[key] = (n + 1, sr + r, sg + g, sb + b)
    if not counts:
        return (128, 128, 128), (64, 64, 64)
    ranked = sorted(((n, (sr // n, sg // n, sb // n)) for n, sr, sg, sb in counts.values()), reverse=True)
    base = ranked[0][1]

    def dist(c):
        return sum((x - y) ** 2 for x, y in zip(c, base)) ** 0.5

    best, score = None, 0.0
    for n, c in ranked[1:]:
        d = dist(c)
        if d > 70 and n * min(d, 160) > score:
            best, score = c, n * min(d, 160)
    if best is None:
        best = _tint(base, 0.55) if sum(base) > 300 else _tint(base, 1.6)
    return base, best


def egg(base, spots):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            ch = EGG[y][x]
            if ch == '.':
                continue
            k = LEVEL[ch]
            colour = spots if SPOTS[y][x] == 'S' else base
            im.putpixel((x, y), _tint(colour, k) + (255,))
    return im


TABLE = os.path.join(os.path.dirname(__file__), 'egg_colours.json')


def _hex(c):
    return '#%02x%02x%02x' % tuple(c)


def _rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def repaint(tex_root):
    """Redraws every Sift spawn egg. Colours live in tools/egg_colours.json (edit it to retune an egg); an egg not in the
    table yet gets its colours read from the sprite its generator painted, and is added."""
    table = json.load(open(TABLE)) if os.path.exists(TABLE) else {}
    n = 0
    for path in sorted(glob.glob(os.path.join(tex_root, 'item', '*_spawn_egg.png'))):
        name = os.path.basename(path)[:-len('_spawn_egg.png')]
        if name not in table:
            base, spots = _colours(path)
            table[name] = [_hex(base), _hex(spots)]
        base, spots = (_rgb(h) for h in table[name])
        egg(base, spots).save(path)
        n += 1
    with open(TABLE, 'w') as f:
        json.dump(dict(sorted(table.items())), f, indent=1)
        f.write('\n')
    print(f'old eggs: {n} spawn eggs redrawn as classic two-colour eggs')
