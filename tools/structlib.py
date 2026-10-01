"""Tiny voxel-building DSL that writes vanilla structure template NBT."""
import gzip
import io
import math
import random

import nbtlib
from nbtlib import Compound, Int, List, String

DATA_VERSION = 5023


def rl(x):
    return x if ':' in x else f'thesift:{x}'


class B:
    """A block state: B('dreamstone_bricks'), B('lullwood_stairs', facing='north')."""

    def __init__(self, name, nbt=None, **props):
        self.name = rl(name)
        self.props = {k: str(v).lower() for k, v in props.items()}
        self.nbt = nbt

    def key(self):
        return (self.name, tuple(sorted(self.props.items())))

    def with_(self, **props):
        p = dict(self.props)
        p.update({k: str(v).lower() for k, v in props.items()})
        return B(self.name, self.nbt, **p)


AIR = B('minecraft:air')


class Build:
    def __init__(self, sx, sy, sz, seed=0):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.rnd = random.Random(seed)

    # ---------------------------------------------------------------- primitives
    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def set(self, x, y, z, b):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if self.inside(x, y, z):
            if b is None:
                self.blocks.pop((x, y, z), None)
            else:
                self.blocks[(x, y, z)] = b

    def get(self, x, y, z):
        return self.blocks.get((x, y, z))

    def fill(self, x0, y0, z0, x1, y1, z1, b, hollow=False, pick=None):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    edge = x in (x0, x1) or y in (y0, y1) or z in (z0, z1)
                    if hollow and not edge:
                        self.set(x, y, z, AIR)
                    else:
                        self.set(x, y, z, pick(x, y, z) if pick else b)

    def cyl(self, cx, cz, y0, y1, r, b, hollow=False, pick=None, thickness=1.0):
        ir = int(math.ceil(r)) + 1
        for y in range(y0, y1 + 1):
            for x in range(cx - ir, cx + ir + 1):
                for z in range(cz - ir, cz + ir + 1):
                    d = math.hypot(x - cx, z - cz)
                    if d <= r + 0.35:
                        if hollow and d < r + 0.35 - thickness:
                            self.set(x, y, z, AIR)
                        else:
                            self.set(x, y, z, pick(x, y, z) if pick else b)

    def disc(self, cx, cz, y, r, b, pick=None):
        self.cyl(cx, cz, y, y, r, b, pick=pick)

    def mix(self, *options):
        """Returns a pick function choosing weighted blocks: mix((B, w), ...)."""
        total = sum(w for _, w in options)

        def pick(x, y, z):
            v = self.rnd.random() * total
            for b, w in options:
                v -= w
                if v <= 0:
                    return b
            return options[-1][0]

        return pick

    # ---------------------------------------------------------------- decay & life
    def decay(self, prob, top_bias=0.0, protect=(), min_y=0):
        """Randomly knocks blocks out; higher blocks are more likely to go."""
        sy = self.size[1]
        for pos, b in list(self.blocks.items()):
            if b.name == AIR.name or b.name in protect or pos[1] < min_y:
                continue
            p = prob + top_bias * (pos[1] / max(1, sy - 1))
            if self.rnd.random() < p:
                self.blocks[pos] = AIR

    def replace(self, frm, to, prob=1.0):
        frm = rl(frm)
        for pos, b in list(self.blocks.items()):
            if b.name == frm and self.rnd.random() < prob:
                self.blocks[pos] = to if isinstance(to, B) else B(to, **b.props)

    def overgrow(self, ground_blocks, plants, prob):
        """Places plants on top of solid ground blocks that have air above."""
        g = {rl(x) for x in ground_blocks}
        for (x, y, z), b in list(self.blocks.items()):
            if b.name in g and self.rnd.random() < prob:
                above = self.get(x, y + 1, z)
                if (above is None or above.name == AIR.name) and self.inside(x, y + 1, z):
                    self.set(x, y + 1, z, self.rnd.choice(plants))

    def drape(self, block, prob, max_len=3):
        """Hangs strands (e.g. hanging leaves) under overhangs."""
        for (x, y, z), b in list(self.blocks.items()):
            if b.name == AIR.name or self.rnd.random() >= prob:
                continue
            below = self.get(x, y - 1, z)
            if below is not None and below.name != AIR.name:
                continue
            n = 1 + self.rnd.randrange(max_len)
            for i in range(1, n + 1):
                if not self.inside(x, y - i, z) or (self.get(x, y - i, z) not in (None,) and self.get(x, y - i, z).name != AIR.name):
                    break
                self.set(x, y - i, z, block.with_(tip='true' if i == n else 'false') if 'tip' in block.props else block)

    # ---------------------------------------------------------------- output
    def save(self, path):
        palette, index = [], {}
        blocks = List[Compound]()
        for (x, y, z), b in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            k = b.key()
            if k not in index:
                index[k] = len(palette)
                # 26.3 block state format: "id" + lowercase "properties" (no datafixing happens at the current DataVersion)
                entry = Compound({'id': String(b.name)})
                if b.props:
                    entry['properties'] = Compound({kk: String(vv) for kk, vv in b.props.items()})
                palette.append(entry)
            e = Compound({'pos': List[Int]([Int(x), Int(y), Int(z)]), 'state': Int(index[k])})
            if b.nbt:
                e['nbt'] = to_nbt(b.nbt)
            blocks.append(e)
        root = Compound({
            'DataVersion': Int(DATA_VERSION),
            'size': List[Int]([Int(v) for v in self.size]),
            'palette': List[Compound](palette),
            'blocks': blocks,
            'entities': List[Compound]([]),
        })
        # gzip with a fixed mtime so regenerating unchanged structures leaves the files byte-identical
        raw = io.BytesIO()
        nbtlib.File(root).write(raw)
        with open(path, 'wb') as f, gzip.GzipFile(filename='', mode='wb', fileobj=f, mtime=0) as gz:
            gz.write(raw.getvalue())
        return len(self.blocks)


def to_nbt(v):
    if isinstance(v, dict):
        return Compound({k: to_nbt(x) for k, x in v.items()})
    if isinstance(v, bool):
        return nbtlib.Byte(1 if v else 0)
    if isinstance(v, int):
        return Int(v)
    if isinstance(v, float):
        return nbtlib.Float(v)
    if isinstance(v, list):
        items = [to_nbt(x) for x in v]
        return List(items)
    return String(v)


def chest(loot, facing='north'):
    return B('minecraft:chest', nbt={'id': 'minecraft:chest', 'LootTable': rl(loot)}, facing=facing, type='single', waterlogged='false')


def suspicious(loot):
    return B('suspicious_dreamsand', nbt={'id': 'minecraft:brushable_block', 'LootTable': rl(loot)}, dusted=0)


def stairs(name, facing, half='bottom'):
    return B(name, facing=facing, half=half, shape='straight', waterlogged='false')
