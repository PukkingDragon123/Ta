"""The Thumper's arena: the Ancient Cannon block (model, blockstates, textures) and the Cannonball
item sprite. Hooked from gen_assets.gen_block (model 'cannon'), gen_textures.functional and
items16.all_items."""
import os
import random

from PIL import Image

NS = 'thesift'
BRONZE = ['#3a2412', '#5a3a1e', '#83552a', '#a8733a', '#c99450', '#e8bf72']
PATINA = ['#245a54', '#3a8a80', '#62b6a6']
STONE = ['#1c2228', '#2a3038', '#3a424c', '#4c5662', '#5e6a76']
IRON = ['#141618', '#26292e', '#3c4148', '#5a6068', '#8a929c', '#c8d0d8']


def _hx(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def _tex(fn):
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = _hx(fn(x, y))
    return img


def barrel(seed=7):
    """Cast bronze, banded every few texels with raised reinforcing rings, green patina weeping
    down from the bands, a highlight running along the top."""
    rnd = random.Random(seed)
    noise = [[rnd.random() for _ in range(16)] for _ in range(16)]

    def fn(x, y):
        band = y in (2, 3, 9, 10, 14)
        base = 4 if x in (6, 7) else 3 if 4 <= x <= 9 else 2 if 2 <= x <= 12 else 1
        if band:
            base = min(5, base + 1) if y in (2, 9) else max(0, base - 1)
        if not band and y > 3 and noise[y][x] < 0.16 and (y - 1) % 7 < 4:
            return PATINA[1] if noise[y][x] < 0.08 else PATINA[0]
        if noise[y][x] > 0.92:
            base = max(0, base - 1)
        return BRONZE[base]
    return _tex(fn)


def muzzle():
    """The cannon's mouth: a thick bronze lip round a deep black bore."""
    def fn(x, y):
        d = max(abs(x - 7.5), abs(y - 7.5))
        if d < 3.0:
            return IRON[0] if d < 2.0 else IRON[1]
        if d < 4.0:
            return BRONZE[1]
        return BRONZE[4] if (x + y) % 5 == 0 else BRONZE[3]
    return _tex(fn)


def ball():
    """A loaded ball seen down the bore: dark iron with a dull glint."""
    def fn(x, y):
        d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
        if d < 2.0 and x < 8 and y < 8:
            return IRON[4]
        if d < 4.5:
            return IRON[3] if (x < 8 and y < 8) else IRON[2]
        return IRON[1]
    return _tex(fn)


def carriage(seed=11):
    """The squat stone carriage: hushslate blocks with bronze strapping and a rivet at each end."""
    rnd = random.Random(seed)

    def fn(x, y):
        if y in (0, 15):
            return STONE[0]
        if y in (4, 11):
            return BRONZE[2] if x not in (0, 15) else BRONZE[1]
        if y in (3, 12) and x in (2, 13):
            return BRONZE[5]
        if x in (0, 7, 15) and y not in (4, 11):
            return STONE[1]
        return STONE[2 + (rnd.random() < 0.3) + (rnd.random() < 0.12)]
    return _tex(fn)


def block_textures(out):
    out('block/ancient_cannon_barrel', barrel())
    out('block/ancient_cannon_muzzle', muzzle())
    out('block/ancient_cannon_ball', ball())
    out('block/ancient_cannon_carriage', carriage())


def gen_cannon(bid):
    """A stubby siege cannon on a stone carriage. Its barrel points out of the block's north face
    (the blockstate turns it to face the way it was last aimed), tilted up; loaded, a ball shows in
    the bore."""
    import gen_assets as GA
    el, faces = GA.el, GA.faces
    tx = {'particle': f'{NS}:block/{bid}_barrel', 'barrel': f'{NS}:block/{bid}_barrel', 'muzzle': f'{NS}:block/{bid}_muzzle',
          'carriage': f'{NS}:block/{bid}_carriage', 'ball': f'{NS}:block/{bid}_ball'}
    tilt = {'origin': [8, 7, 9], 'axis': 'x', 'angle': -22.5}
    els = [
        # the carriage: a low plinth and two cheeks holding the trunnions
        el([2, 0, 2], [14, 3, 14], faces('#carriage', '#carriage', '#carriage', uv_side=[2, 0, 14, 3], uv_top=[2, 2, 14, 14])),
        el([2, 3, 5], [4, 8, 13], faces('#carriage', '#carriage', '#carriage', uv_side=[5, 4, 13, 9], uv_top=[2, 5, 4, 13])),
        el([12, 3, 5], [14, 8, 13], faces('#carriage', '#carriage', '#carriage', uv_side=[5, 4, 13, 9], uv_top=[12, 5, 14, 13])),
        # the barrel, its breech knob and the thick muzzle ring
        el([5, 4, 3], [11, 10, 15], {**faces('#barrel', '#barrel', '#barrel', uv_side=[2, 0, 14, 6]),
                                     'north': {'texture': '#muzzle', 'uv': [5, 5, 11, 11]}}, rotation=tilt),
        el([6.5, 5.5, 15], [9.5, 8.5, 16], faces('#barrel', '#barrel', '#barrel', uv_side=[6, 2, 9, 3]), rotation=tilt),
        el([4.5, 3.5, 2], [11.5, 10.5, 4], {**faces('#barrel', '#barrel', '#barrel', uv_side=[1, 2, 8, 4]),
                                             'north': {'texture': '#muzzle', 'uv': [4.5, 4.5, 11.5, 11.5]}}, rotation=tilt),
    ]
    for loaded in (False, True):
        name = bid + ('_loaded' if loaded else '')
        e = list(els)
        if loaded:
            e.append(el([6, 5, 1.9], [10, 9, 2], {'north': {'texture': '#ball', 'uv': [6, 6, 10, 10]}}, rotation=tilt))
        m = {'parent': 'minecraft:block/block', 'textures': tx, 'elements': e}
        GA.note_textures(m)
        GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
    variants = {}
    for facing, rot in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for loaded in ('false', 'true'):
            v = {'model': f'{NS}:block/{bid}' + ('_loaded' if loaded == 'true' else '')}
            if rot:
                v['y'] = rot
            variants[f'facing={facing},loaded={loaded}'] = v
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})
    GA.item_block(bid)


def items():
    """The Cannonball: a heavy black iron ball, a hard glint top-left, a seam round its middle."""
    import items16 as I
    rows = [
        '................',
        '................',
        '.....kkkkkk.....',
        '....kHhmmmmk....',
        '...kHhmmmmmmk...',
        '..khhmmmmmmmdk..',
        '..kmmmmmmmmmdk..',
        '..kssssssssssk..',
        '..kmmmmmmmmddk..',
        '..kmmmmmmmdddk..',
        '...kmmmmmdddk...',
        '....kmmddddk....',
        '.....kkkkkk.....',
        '................',
        '................',
        '................',
    ]
    pal = {'k': ('#141618', '#0a0b0c'), 'H': '#e8eef4', 'h': '#8a929c', 'm': '#3c4148', 'd': '#26292e', 's': '#5a6068'}
    return {'cannonball': I.grid(rows, pal, ol=False)}
