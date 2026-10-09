"""F2 Band Table & songs: the Music Band Table (model, block model, textures, score screen art), the Sift enchantments
(data-driven 26.3 enchantments, tags, their enchanted books' own look), the new songs' loot, recipes, lang and Codex.

Java: com.thesift.block.BandTableBlock / entity.BandTableBlockEntity, com.thesift.enchant.*, registry.ModBandTable,
client.bandtable.* (renderer, model, screen).

Hooks (one line each): spec.py (the block), mobs.ALL (MODELS: the table's moving parts, one texture with its body),
gen_assets.gen_block (model kind 'band_table'), gen_assets.generate (assets), gen_textures.main (textures).

The table is ONE modelkit model: parts named 'base*' are its still body - exported here as the block model (so it is
lit and culled like a block, and is the item) - and every other part is drawn and animated by BandTableRenderer.
Model space is entity space (y down, floor at 24, front at -z); the renderer draws it as an entity renderer would
(scale -1, -1, 1), so model (x, y, z) is block pixel (8 - x, 24 - y, 8 + z).
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

from modelkit import Model, Pose, preview, render_textures

NS = 'thesift'
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')
RES = os.path.join(ROOT, 'src/main/resources')
ASSETS = os.path.join(RES, 'assets', NS)
DATA = os.path.join(RES, 'data', NS)

PAL = {
    # lacquered dark walnut, a warmer plank for the top, brass fittings
    'wood': '#5c3a2a', 'wood_l': '#7a513a', 'wood_d': '#3d2519',
    'woodtop': '#8a5c3c', 'woodtop_l': '#a87650', 'woodtop_d': '#6a4329',
    'brass': '#c8922e', 'brass_l': '#f2cc6a', 'brass_d': '#8c5c1e',
    'steel': '#c8d2dc', 'steel_l': '#f0f6ff', 'steel_d': '#8a96a6',
    'cloth': '#9a2f3e', 'cloth_l': '#c4485a', 'cloth_d': '#68202c',
    'drumhead': '#ecdcb6', 'drumhead_l': '#fbf1d8', 'drumhead_d': '#c8b48c',
    'leather': '#2e4a7a', 'leather_l': '#4a6aa0', 'leather_d': '#1e3054',
    'paper': '#f4ead0', 'paper_l': '#fffaf0', 'paper_d': '#d8c8a0',
    'ink': '#2a1c14', 'hole': '#1a0f0a', 'gemcore': '#b48cff',
    'gem': '#9a6ef0', 'gem_l': '#d8c0ff', 'gem_d': '#5e3aa8',
    'n0': '#5fe9ff', 'n1': '#ff8ae0', 'n2': '#ffd34a', 'n3': '#b48cff', 'n4': '#7cf0a0', 'n5': '#ff9a5a', 'n6': '#9ad8ff', 'n7': '#ff6a8a',
    'note_rim': '#ffffff',
}
NOTE_COLOURS = ['n0', 'n1', 'n2', 'n3', 'n4', 'n5', 'n6', 'n7']
NOTES = 8


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


def grid(w, h, fn):
    return [''.join(fn(x, y) for x in range(w)) for y in range(h)]


# ----------------------------------------------------------------------------- face maps (hd: one char per texel)

def _front_body(w, h):
    """The cabinet's front: a carved inset panel and a round brass speaker grille."""
    cx, cy = w / 2 - 0.5, h / 2 - 0.5

    def f(x, y):
        d = math.hypot(x - cx, y - cy)
        if d < 6.6:
            if d >= 5.4:
                return 'B' if (x + y) % 5 else 'b'
            if (x % 2 == 0) and (y % 2 == 0):
                return 'k'
            return 'b' if d > 4.6 else 'D'
        if x in (1, w - 2) and 1 <= y <= h - 2 or y in (1, h - 2) and 1 <= x <= w - 2:
            return 'd'
        if x in (2, w - 3) and 2 <= y <= h - 3 or y in (2, h - 3) and 2 <= x <= w - 3:
            return 'L'
        return '.'
    return grid(w, h, f)


def _side_body(w, h):
    """A side of the cabinet: two violin f-holes in a carved panel."""
    holes = set()
    for cx, flip in ((w // 3, 1), (w - 1 - w // 3, -1)):
        for y in range(3, h - 3):
            t = (y - 3) / max(1, h - 7)
            x = cx + round(1.6 * math.sin(t * math.pi * 1.4 - 0.4) * flip)
            holes.add((x, y))
        holes.add((cx + 2 * flip, 3))
        holes.add((cx - 2 * flip, h - 4))
        holes.add((cx - 1, h // 2))
        holes.add((cx + 1, h // 2))

    def f(x, y):
        if (x, y) in holes:
            return 'k'
        if (x - 1, y) in holes or (x, y - 1) in holes:
            return 'L'
        if x in (1, w - 2) and 1 <= y <= h - 2 or y in (1, h - 2) and 1 <= x <= w - 2:
            return 'd'
        return '.'
    return grid(w, h, f)


def _drum_side(w, h):
    """The drum's shell: a darker band round its waist and two brass tuning lugs."""
    def f(x, y):
        if x in (1, w - 2) and 1 <= y <= h - 2:
            return 'B' if y in (2, h - 3) else 'b'
        if y in (h // 2, h // 2 + 1):
            return 'd'
        return '.'
    return grid(w, h, f)


def _back_body(w, h):
    """The back: a drawer for the sheet music, with a brass pull."""
    def f(x, y):
        if y in (3, h - 3) and 3 <= x <= w - 4 or x in (3, w - 4) and 3 <= y <= h - 3:
            return 'd'
        if y == h // 2 and w // 2 - 3 <= x <= w // 2 + 2:
            return 'B'
        if y == h // 2 + 1 and w // 2 - 2 <= x <= w // 2 + 1:
            return 'b'
        return '.'
    return grid(w, h, f)


def _top_slab(w, h):
    """The table top: a brass border and an inlaid stave running across it, with a few inlaid notes."""
    rnd = random.Random(7)
    staff = [h // 2 - 6 + 3 * i for i in range(5)]
    heads = {(w // 4, staff[3] - 1), (w // 4 + 7, staff[2] - 1), (w // 2 + 3, staff[1] - 1), (w - w // 4, staff[2] + 1)}

    def f(x, y):
        if x in (0, w - 1) or y in (0, h - 1):
            return 'B'
        if x in (1, w - 2) or y in (1, h - 2):
            return 'b'
        for hx, hy in heads:
            if abs(x - hx) <= 1 and abs(y - hy) <= 1 and (x - hx, y - hy) != (1, -1):
                return 'k'
            if x == hx + 1 and hy - 5 <= y < hy:
                return 'k'
        if y in staff and 3 <= x <= w - 4:
            return 'L'
        if (x * 7 + y * 3) % 11 == 0 and rnd.random() < 0.3:
            return 'd'
        return '.'
    return grid(w, h, f)


def _book_page(w, h):
    """An open page of the songbook: staff lines and notes."""
    def f(x, y):
        if y in (2, 4, 6, 8) and 1 <= x <= w - 2:
            return 'l'
        if (x, y) in ((2, 3), (3, 5), (5, 3), (6, 7), (8, 5)):
            return 'k'
        return '.'
    return grid(w, h, f)


def _note_glyph(w, h, key):
    """A quaver: a round head, a stem and a flag, rimmed in white."""
    shape = set()
    for x in range(w):
        for y in range(h):
            if (x - 2) ** 2 + (y - (h - 3)) ** 2 <= 4.2:
                shape.add((x, y))
    for y in range(1, h - 2):
        shape.add((4, y))
    for x, y in ((5, 1), (6, 2), (6, 3), (5, 4), (5, 2)):
        shape.add((x, y))

    def f(x, y):
        if (x, y) in shape:
            return 'n'
        if any((x + dx, y + dy) in shape for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            return 'w'
        return '_'
    return grid(w, h, f)


# ----------------------------------------------------------------------------- the model

def model():
    m = Model('band_table', (128, 96), dict(PAL), {'band_table': {}}, res=2, detail=1)
    wood_keys = {'d': 'wood_d', 'L': 'wood_l', 'k': 'hole', 'B': 'brass_l', 'b': 'brass', 'D': 'brass_d'}
    # ---- the still body (block model)
    plinth = m.part('base_plinth', pivot=(0, 24, 0))
    plinth.cube((-8, -2, -8), (16, 2, 16), **mc('wood_d', clusters=0.15), faces={
        'up': mc('wood_d', clusters=0.1)})
    body = m.part('base_body', pivot=(0, 24, 0))
    body.cube((-7, -10, -7), (14, 8, 14), **mc('wood', clusters=0.25), faces={
        'north': mc('wood', clusters=0.1, hd=True, map=_front_body(28, 16), keys=wood_keys),
        'south': mc('wood', clusters=0.2, hd=True, map=_back_body(28, 16), keys=wood_keys),
        'west': mc('wood', clusters=0.2, hd=True, map=_side_body(28, 16), keys=wood_keys),
        'east': mc('wood', clusters=0.2, hd=True, map=_side_body(28, 16), keys=wood_keys)})
    for i, (x, z) in enumerate(((-8, -8), (6, -8), (-8, 6), (6, 6))):
        post = m.part(f'base_post_{i}', pivot=(0, 24, 0))
        post.cube((x, -10, z), (2, 8, 2), **mc('wood_d', clusters=0.1, bands=[(3, 'wood'), (4, 'brass_d'), (5, 'wood')]))
        post.cube((x, -13, z), (2, 1, 2), **mc('brass', clusters=0.0, rim=False))
    top = m.part('base_top', pivot=(0, 24, 0))
    top.cube((-8, -12, -8), (16, 2, 16), **mc('woodtop', clusters=0.2), faces={
        'up': mc('woodtop', clusters=0.15, hd=True, map=_top_slab(32, 32),
                 keys={'B': 'brass', 'b': 'brass_d', 'L': 'brass_l', 'k': 'brass_d', 'd': 'woodtop_d'})})
    # the hand drum (front, the right as you face it): crimson shell, brass hoops, a pale skin
    drum = m.part('base_drum', pivot=(0, 24, 0))
    side = mc('cloth', clusters=0.0, hd=True, map=_drum_side(8, 9), keys={'b': 'brass_l', 'd': 'cloth_d', 'B': 'brass'})
    drum.cube((3, -16.5, -7), (4, 4.5, 4), **mc('cloth', clusters=0.15), faces={'north': side, 'south': side, 'west': side, 'east': side})
    # the top hoop holds the skin: its top face is the drumhead in a brass ring
    drum.cube((2.5, -17.5, -7.5), (5, 1, 5), **mc('brass', clusters=0.0, rim=False), faces={
        'up': mc('drumhead', clusters=0.0, hd=True, map=grid(10, 10, lambda x, y: 'B' if x in (0, 9) or y in (0, 9) else (
            'd' if (x - 4.5) ** 2 + (y - 4.5) ** 2 > 13 else ('l' if (x - 3) ** 2 + (y - 3) ** 2 < 3 else '.'))),
            keys={'B': 'brass_l', 'd': 'drumhead_d', 'l': 'drumhead_l'})})
    drum.cube((2.5, -12.5, -7.5), (5, 0.5, 5), **mc('brass_d', clusters=0.0, rim=False))
    # the metronome (front left): a wooden pyramid with a brass scale
    metro = m.part('base_metronome', pivot=(0, 24, 0))
    metro.cube((-7, -15, -7), (3, 3, 3), **mc('wood', clusters=0.1), faces={
        'north': mc('wood', clusters=0.0, hd=True, map=['..BB..', '..bb..', '..BB..', '..bb..', '.BBBB.', '......'],
                    keys={'B': 'brass_l', 'b': 'brass_d'})})
    metro.cube((-6.5, -18, -6.5), (2, 3, 2), **mc('wood', clusters=0.1))
    metro.cube((-6, -19, -6), (1, 1, 1), **mc('brass', clusters=0.0, rim=False))
    # the chime tree (back left): a post and an arm the tubes hang from
    chime = m.part('base_chime_tree', pivot=(0, 24, 0))
    chime.cube((-7.5, -13, 4.5), (2, 1, 2), **mc('brass_d', clusters=0.0, rim=False))
    chime.cube((-7, -23, 5), (1, 10, 1), **mc('wood_d', clusters=0.1, rim=False))
    chime.cube((-7, -24, 5), (6, 1, 1), **mc('brass', clusters=0.0, rim=False))
    chime.cube((-1.5, -24.5, 4.5), (1, 2, 2), **mc('brass_l', clusters=0.0, rim=False))
    # the horn's crank box (back right)
    horn_box = m.part('base_horn_box', pivot=(0, 24, 0))
    horn_box.cube((2.5, -15, 2.5), (4, 3, 4), **mc('wood', clusters=0.15), faces={
        'north': mc('wood', clusters=0.0, hd=True, map=['........', '.bbbbbb.', '.b....b.', '.bbbbbb.', '........', '........'],
                    keys={'b': 'brass_d'})})
    horn_box.cube((4, -16, 4), (1, 1, 1), **mc('brass', clusters=0.0, rim=False))
    # the songbook's stand (back middle)
    stand = m.part('base_stand', pivot=(0, 24, 0))
    stand.cube((-1.5, -13, 1), (3, 1, 3), **mc('brass_d', clusters=0.0, rim=False))
    stand.cube((-0.5, -16, 2), (1, 3, 1), **mc('brass', clusters=0.0, rim=False))

    # ---- the moving parts (BandTableRenderer)
    # the songbook: covers open about the spine, a page turns over
    book = m.part('book', pivot=(0, 8, 2.5), rot=(0.6, 0, 0))
    for side, sx in (('left', -1), ('right', 1)):
        half = book.part(f'book_{side}', pivot=(0, 0, 0))
        half.cube((0 if sx > 0 else -4.5, -0.5, -3), (4.5, 0.5, 6), **mc('leather', clusters=0.1, rim=False), faces={
            'down': mc('leather', clusters=0.0, hd=True, map=grid(9, 12, lambda x, y: 'g' if x in (0, 8) and y in (0, 11) or (x, y) in (
                (4, 5), (4, 6), (5, 4)) else '.'), keys={'g': 'brass_l'})})
        half.cube((0.25 if sx > 0 else -4.25, -1.25, -2.75), (4, 0.75, 5.5), **mc('paper', clusters=0.0, rim=False), faces={
            'up': mc('paper', clusters=0.0, hd=True, map=_book_page(8, 11), keys={'l': 'paper_d', 'k': 'ink'})})
    leaf = book.part('book_page', pivot=(0, -1.3, 0))
    leaf.cube((0, 0, -2.75), (4, 0, 5.5), **mc('paper_l', clusters=0.0, rim=False), faces={
        'up': mc('paper_l', clusters=0.0, hd=True, map=_book_page(8, 11), keys={'l': 'paper_d', 'k': 'ink'}),
        'down': mc('paper_l', clusters=0.0, hd=True, map=_book_page(8, 11), keys={'l': 'paper_d', 'k': 'ink'})})
    # the metronome's pendulum
    pend = m.part('pendulum', pivot=(-5.5, 10.5, -7.2))
    pend.cube((-0.25, -5.5, -0.25), (0.5, 5.5, 0.5), **mc('steel', clusters=0.0, rim=False))
    pend.cube((-0.75, -4, -0.5), (1.5, 1, 1), **mc('brass_l', clusters=0.0, rim=False))
    # the drumsticks: they rest across the drum and tap it
    for i, sx in enumerate((1, -1)):
        stick = m.part(f'stick_{i}', pivot=(5 + 1.2 * sx, 6.0, -1.5), rot=(0.18, 0, 0))
        stick.cube((-0.25, -0.25, -5.0), (0.5, 0.5, 5.0), **mc('woodtop_l', clusters=0.0, rim=False))
        stick.cube((-0.5, -0.5, -5.6), (1, 1, 1), **mc('drumhead', clusters=0.0, rim=False))
    # four chime tubes hanging from the arm, long to short
    for i, (x, length) in enumerate(((-5.3, 6), (-3.9, 5), (-2.5, 4))):
        tube = m.part(f'chime_{i}', pivot=(x, 1.0, 5.5))
        tube.cube((-0.15, 0, -0.15), (0.3, 0.8, 0.3), **mc('ink', clusters=0.0, rim=False))
        tube.cube((-0.4, 0.8, -0.4), (0.8, length, 0.8), **mc('steel', clusters=0.0, rim=False), faces={
            'down': mc('steel_l', clusters=0.0, rim=False)})
    # the horn: a gramophone bell on the crank box, turning to the music
    horn = m.part('horn', pivot=(4.5, 8.0, 4.5))
    horn.cube((-0.5, -2, -0.5), (1, 2, 1), **mc('brass_d', clusters=0.0, rim=False))
    flare = horn.part('horn_flare', pivot=(0, -2, 0), rot=(0.75, 0, 0))
    flare.cube((-0.75, -1.5, -0.75), (1.5, 1.5, 1.5), **mc('brass', clusters=0.0, rim=False))
    flare.cube((-1.25, -2.5, -1.25), (2.5, 1, 2.5), **mc('brass', clusters=0.0, rim=False))
    flare.cube((-1.75, -3.25, -1.75), (3.5, 0.75, 3.5), **mc('brass_l', clusters=0.0, rim=False), faces={
        'up': mc('hole', clusters=0.0, hd=True, map=grid(7, 7, lambda x, y: 'b' if x in (0, 6) or y in (0, 6) else (
            'd' if x in (1, 5) or y in (1, 5) else 'k')), keys={'b': 'brass_l', 'd': 'brass_d', 'k': 'hole'})})
    # the glowing notes the song is written in, hung over the table by the renderer
    for i in range(NOTES):
        note = m.part(f'note_{i}', pivot=(0, 0, 0))
        key = NOTE_COLOURS[i]
        spec = dict(color=key, pattern='flat', rim=False, hd=True, map=_note_glyph(8, 10, key), keys={'n': key, 'w': 'note_rim'},
                    glow_keys='nw')
        note.cube((-2, -5, 0), (4, 5, 0), color=key, pattern='flat', faces={'north': spec, 'south': spec})
    return m


MODELS = {'band_table': model}


# ----------------------------------------------------------------------------- block model (the still body)

def _uv(u, v, w, h, d, face):
    """JSON face uv (texture units) for a box-UV cube face, so the block shows what the entity renderer would."""
    if face == 'north':
        return [u + d, v + d, u + d + w, v + d + h]
    if face == 'south':
        return [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h]
    if face == 'east':      # model -X ('west' region) is block +X
        return [u, v + d, u + d, v + d + h]
    if face == 'west':      # model +X ('east' region) is block -X
        return [u + d + w, v + d, u + 2 * d + w, v + d + h]
    if face == 'up':        # the model's top, turned half round (x is mirrored, z is not)
        return [u + d + w, v + d, u + d, v]
    return [u + d + 2 * w, v, u + d + w, v + d]   # down


def block_model(m):
    """The 'base*' parts as a block model, one element per cube."""
    m.pack()
    elements = []
    sx, sy = 16.0 / m.tex_w, 16.0 / m.tex_h
    for part in m.root.children:
        if not part.name.startswith('base'):
            continue
        px, py, pz = part.pivot
        for c in part.cubes:
            ox, oy, oz = c.origin
            w, h, d = c.size
            x0, y0, z0 = px + ox, py + oy, pz + oz
            frm = [8 - (x0 + w), 24 - (y0 + h), 8 + z0]
            to = [8 - x0, 24 - y0, 8 + z0 + d]
            u, v = c.uv
            faces = {}
            for f in ('north', 'south', 'east', 'west', 'up', 'down'):
                uv = _uv(u, v, w, h, d, f)
                faces[f] = {'uv': [round(uv[0] * sx, 4), round(uv[1] * sy, 4), round(uv[2] * sx, 4), round(uv[3] * sy, 4)],
                            'texture': '#all'}
            elements.append({'from': [round(a, 4) for a in frm], 'to': [round(a, 4) for a in to], 'faces': faces})
    return {'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'textures': {'all': f'{NS}:block/band_table', 'particle': f'{NS}:block/band_table_particle'},
            'elements': elements,
            'display': {
                'gui': {'rotation': [30, 225, 0], 'translation': [0, -1.5, 0], 'scale': [0.55, 0.55, 0.55]},
                'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
                'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
                'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
                'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
                'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]}}}


def gen_block(GA, bid):
    """gen_assets hook: the block model, the four-way blockstate and the item."""
    GA.write(os.path.join(GA.A, 'models/block', bid + '.json'), block_model(model()))
    GA.TEXTURES.add('block/band_table')
    GA.TEXTURES.add('block/band_table_particle')
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': {
        f'facing={f}': ({'model': f'{NS}:block/{bid}', 'y': r} if r else {'model': f'{NS}:block/{bid}'})
        for f, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
    GA.item_block(bid)


# ============================================================================= the Sift enchantments (data)

# id: (name, description, max level, supported items, slots, anvil cost, weight, exclusive set, data effects)
SPEED = {'minecraft:attributes': [{'amount': {'type': 'minecraft:linear', 'base': 0.04, 'per_level_above_first': 0.03},
                                   'attribute': 'minecraft:movement_speed', 'id': f'{NS}:enchantment.melody_steps',
                                   'operation': 'add_multiplied_base'}]}
ENCHANTS = {
    'melody_steps': ('Melody Steps', 'Steps leave glowing notes and chime a scale; you walk a little faster.',
                     3, '#minecraft:enchantable/foot_armor', ['feet'], 4, 2, 'footfall', SPEED),
    'hushed_step': ('Hushed Step', 'Your steps make no vibrations: Sensors, Shriekers and Wardens never hear you walk.',
                    1, '#minecraft:enchantable/foot_armor', ['feet'], 8, 1, 'footfall', None),
    'sculk_ward': ('Sculk Ward', 'Shrugs off Sculk Corruption (all armour adds up) and softens sonic booms.', 4, '#minecraft:enchantable/armor', ['armor'], 4, 2, None, None),
    'echo_strike': ('Echo Strike', 'Every 4th hit (3rd at II) rings a sonic echo through the monsters around your target.', 2, '#minecraft:enchantable/weapon', ['mainhand'], 4, 2, None, None),
    'resonance': ('Resonance', 'Your blows ring with your band: more damage for every creature playing in it.',
                  3, '#minecraft:enchantable/weapon', ['mainhand'], 4, 1, None, None),
    'crescendo': ('Crescendo', 'Quick successive hits build up, each a note higher and harder - five steps at most.',
                  3, '#minecraft:enchantable/weapon', ['mainhand'], 4, 2, None, None),
    'reverb': ('Reverb', 'Songs played on this instrument reach further and last longer.',
               3, f'#{NS}:instruments', ['hand'], 4, 2, None, None),
    'fortissimo': ('Fortissimo', 'Finishing a song blasts the monsters around you back with a wall of sound.',
                   2, f'#{NS}:instruments', ['hand'], 8, 1, None, None),
}
# which song wins each at the Band Table, and how many tamed band members it needs (mirrors SiftEnchant.java)
ENCHANT_SONGS = {'melody_steps': ('nib', 0), 'hushed_step': ('lullaby', 0), 'sculk_ward': ('requiem', 0), 'echo_strike': ('requiem', 0),
                 'resonance': ('heartbeat', 2), 'crescendo': ('heartbeat', 0), 'reverb': ('canon', 0), 'fortissimo': ('canon', 1)}
# the new songs' sheets in vanilla chests: table -> ([(song, weight)], chance)
VANILLA_SHEETS = {
    'minecraft:chests/ancient_city': ([('requiem', 3), ('heartbeat', 1)], 0.35),
    'minecraft:chests/stronghold_library': ([('canon', 2), ('requiem', 1)], 0.35),
    'minecraft:chests/shipwreck_treasure': ([('dolphin', 1)], 0.3),
    'minecraft:chests/buried_treasure': ([('dolphin', 1)], 0.4),
    'minecraft:chests/underwater_ruin_big': ([('dolphin', 1)], 0.25),
}


def _enchantment(eid):
    name, desc, max_level, items, slots, anvil, weight, exclusive, effects = ENCHANTS[eid]
    d = {'anvil_cost': anvil, 'description': {'translate': f'enchantment.{NS}.{eid}'},
         'max_cost': {'base': 50, 'per_level_above_first': 10}, 'max_level': max_level,
         'min_cost': {'base': 10, 'per_level_above_first': 10}, 'slots': slots, 'supported_items': items, 'weight': weight}
    if effects:
        d['effects'] = effects
    if exclusive:
        d['exclusive_set'] = f'#{NS}:exclusive_set/{exclusive}'
    return d


def assets(GA):
    """gen_assets.generate hook: enchantments, their tags and books, the table's recipe, sheet loot, lang, Codex."""
    for eid in ENCHANTS:
        GA.write(os.path.join(DATA, 'enchantment', eid + '.json'), _enchantment(eid))
    GA.write(os.path.join(DATA, 'tags', 'enchantment', 'exclusive_set', 'footfall.json'),
             {'values': [f'{NS}:melody_steps', f'{NS}:hushed_step']})
    # every Sift enchantment, for anyone who wants them (none is in a vanilla table, trade or loot roll)
    GA.write(os.path.join(DATA, 'tags', 'enchantment', 'band_table.json'), {'values': [f'{NS}:{e}' for e in ENCHANTS]})
    # the enchanted books' own look: a select on the stored enchantments (any one Sift enchantment, any level)
    cases = []
    for eid, spec in ENCHANTS.items():
        cases.append({'when': [{f'{NS}:{eid}': lvl} for lvl in range(1, spec[2] + 1)],
                      'model': {'type': 'minecraft:model', 'model': f'{NS}:item/sift_book_{eid}'}})
        m = {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:item/sift_book_{eid}'}}
        GA.note_textures(m)
        GA.write(os.path.join(GA.A, 'models/item', f'sift_book_{eid}.json'), m)
    GA.write(os.path.join(RES, 'assets', 'minecraft', 'items', 'enchanted_book.json'), {'model': {
        'type': 'minecraft:select', 'property': 'minecraft:component', 'component': 'minecraft:stored_enchantments', 'cases': cases,
        'fallback': {'type': 'minecraft:model', 'model': 'minecraft:item/enchanted_book'}}})
    GA.shaped('band_table', [' B ', 'GNG', 'PPP'], {'B': 'minecraft:book', 'G': 'minecraft:gold_ingot', 'N': 'minecraft:note_block',
                                                   'P': '#minecraft:planks'}, 'band_table', 1, 'misc')
    for table, (entries, chance) in VANILLA_SHEETS.items():
        name = table.split('/')[-1]
        GA.write(os.path.join(DATA, 'loot_table', 'gameplay', f'music_sheets_vanilla_{name}.json'), {
            'type': 'minecraft:chest', 'random_sequence': f'{NS}:gameplay/music_sheets_vanilla_{name}',
            'pools': [{'rolls': 1, 'condition': {'type': 'minecraft:random_chance', 'chance': chance},
                       'entries': [{'type': 'minecraft:item', 'name': f'{NS}:music_sheet_{s}', 'weight': w} for s, w in entries]}]})
        GA.write(os.path.join(DATA, 'loot_modifiers', f'music_sheets_vanilla_{name}.json'), {
            'type': 'neoforge:add_table', 'condition': {'type': 'neoforge:loot_table_id', 'loot_table_id': table},
            'table': f'{NS}:gameplay/music_sheets_vanilla_{name}'})
    GA.LANG.update(lang())


def lang():
    L = {}
    for eid, spec in ENCHANTS.items():
        L[f'enchantment.{NS}.{eid}'] = spec[0]
        L[f'enchantment.{NS}.{eid}.desc'] = spec[1]
    m = f'message.{NS}.band_table'
    L.update({
        f'{m}.begin': 'The table listens... play %s on %s',
        f'{m}.playing': 'The table hears %s on %s: %s of %s notes',
        f'{m}.stopped': 'The table stops listening.',
        f'{m}.wrong': 'A wrong note! The song wanted %s (%s more and it falls apart)',
        f'{m}.flawless': 'A flawless performance: %s!',
        f'{m}.shaky': 'A shaky performance: %s',
        f'{m}.failed': 'The song falls apart... an amethyst shard cracks.',
        f'{m}.cannot': 'The table cannot enchant that.',
        f'{m}.busy': 'The table is busy - take its item first.',
        f'{m}.too_far': 'You are too far from the table.',
        f'{m}.needs_sheet': 'You need its music sheet: %s',
        f'{m}.needs_levels': 'You need %s experience levels.',
        f'{m}.needs_shards': 'You need %s amethyst shards to tune the table.',
        f'{m}.needs_band': 'It needs %s of your own tamed creatures playing in your band nearby (%s now).',
        f'{m}.cannot_pay': 'You can no longer pay the table - the song is lost.',
    })
    g = f'gui.{NS}.band_table'
    L.update({
        f'{g}.items': 'Your things', f'{g}.enchantments': 'Enchantments',
        f'{g}.nothing': 'Bring an instrument, armour, a weapon or a book.',
        f'{g}.song': '♪ %s on %s', f'{g}.sheet': 'Sheet carried', f'{g}.no_sheet': 'No sheet!',
        f'{g}.levels': '%s lv (uses %s)', f'{g}.shards': '%s amethyst', f'{g}.band': '%s tamed',
        f'{g}.play': 'Play',
    })
    c = f'codex.{NS}'
    L.update({
        f'{c}.band_table.title': 'Music Band Table', f'{c}.band_table.tagline': 'Enchanting by playing',
        f'{c}.band_table.body': (
            'Pick an item and a Sift enchantment on its score. Each is bound by a song: carry its sheet, pay '
            'levels and amethyst, and for some bring tamed creatures in your band. Then play the song nearby on its '
            'instrument. Every right note lights a note over the table; three wrong and it falls apart. Clean playing wins '
            'the full level.'),
        f'{c}.sift_enchantments.title': 'Sift Enchantments', f'{c}.sift_enchantments.tagline': 'Songs bound into things',
        f'{c}.sift_enchantments.body': (
            'Only the Band Table gives them. Nibs: Melody Steps. Lullaby: Hushed Step. Sculk Requiem: Sculk Ward, Echo '
            'Strike. The Heartbeat, drummed: Crescendo, Resonance (two tamed band members). Enchanter\'s Canon: Reverb, '
            'Fortissimo (one). Their books have their own covers.'),
    })
    return L


# ============================================================================= textures

def textures(out):
    """gen_textures hook (runs after gen_models): the block model's copy of the table's texture, its particle, the
    score screen and the eight enchanted books."""
    ent = os.path.join(ASSETS, 'textures', 'entity', 'band_table', 'band_table.png')
    out('block/band_table', Image.open(ent).convert('RGBA'))
    out('block/band_table_particle', _particle())
    out('gui/band_table', gui())
    for eid, img in books().items():
        out(f'item/sift_book_{eid}', img)


def _particle():
    rnd = random.Random(3)
    img = Image.new('RGBA', (16, 16))
    base = [(92, 58, 42), (122, 81, 58), (61, 37, 25)]
    for y in range(16):
        for x in range(16):
            c = base[0] if (x + y // 4) % 5 else base[2]
            if rnd.random() < 0.25:
                c = base[1]
            img.putpixel((x, y), c + (255,))
    for i in range(16):
        img.putpixel((i, 0), (200, 146, 46, 255))
        img.putpixel((i, 15), (140, 92, 30, 255))
    return img


def _hex(c, a=255):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def _noise_fill(img, box, cols, seed, grain=0.22):
    rnd = random.Random(seed)
    x0, y0, x1, y1 = box
    px = img.load()
    for y in range(y0, y1):
        for x in range(x0, x1):
            k = 0 if rnd.random() > grain else (1 if rnd.random() < 0.5 else 2)
            px[x, y] = cols[k]


def gui():
    """The score: a walnut frame with brass corners round a parchment page, slots, rows, buttons and icons."""
    img = Image.new('RGBA', (512, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    W, H = 276, 232
    wood = [_hex('#5c3a2a'), _hex('#6a4430'), _hex('#4a2d20')]
    paper = [_hex('#efe0bc'), _hex('#e6d4aa'), _hex('#f6ead0')]
    _noise_fill(img, (0, 0, W, H), wood, 11)
    rnd = random.Random(5)
    for _ in range(90):  # wood grain streaks
        y = rnd.randrange(0, H)
        x = rnd.randrange(0, W - 20)
        d.line([(x, y), (x + rnd.randrange(6, 20), y)], fill=_hex('#3d2519'))
    d.rectangle([0, 0, W - 1, H - 1], outline=_hex('#24150d'))
    d.rectangle([1, 1, W - 2, H - 2], outline=_hex('#8a5c3c'))
    # the parchment
    _noise_fill(img, (6, 18, W - 6, H - 6), paper, 12, 0.18)
    px = img.load()
    for y in range(18, H - 6):  # the page darkens towards its edges, like old paper
        for x in range(6, W - 6):
            e = min(x - 6, W - 7 - x, y - 18, H - 7 - y)
            if e < 7:
                k = (7 - e) * 0.025
                r, g, b, a = px[x, y]
                px[x, y] = (int(r * (1 - k) + 120 * k), int(g * (1 - k) + 86 * k), int(b * (1 - k) + 40 * k), a)
    d.rectangle([6, 18, W - 7, H - 7], outline=_hex('#a88a5a'))
    d.rectangle([5, 17, W - 6, H - 6], outline=_hex('#2e1a10'))
    # the title band and its rivets
    d.rectangle([4, 3, W - 5, 15], fill=_hex('#3d2519'))
    d.line([(4, 15), (W - 5, 15)], fill=_hex('#c8922e'))
    d.line([(4, 3), (W - 5, 3)], fill=_hex('#8a5c3c'))
    for x in (12, W - 13):
        d.rectangle([x - 1, 8, x + 1, 10], fill=_hex('#f2cc6a'))
        d.point((x + 1, 10), fill=_hex('#8c5c1e'))
    # brass corner plates
    for cx, cy in ((0, 0), (W - 10, 0), (0, H - 10), (W - 10, H - 10)):
        d.rectangle([cx, cy, cx + 9, cy + 9], fill=_hex('#c8922e'), outline=_hex('#8c5c1e'))
        d.line([(cx + 1, cy + 1), (cx + 8, cy + 1)], fill=_hex('#f2cc6a'))
        d.line([(cx + 1, cy + 1), (cx + 1, cy + 8)], fill=_hex('#f2cc6a'))
        d.rectangle([cx + 4, cy + 4, cx + 5, cy + 5], fill=_hex('#5c3a1a'))
    # a faint stave in the paper under the item well
    for i in range(5):
        y = 114 + i * 3
        d.line([(12, y), (100, y)], fill=_hex('#ddc898'))
    # the divider over the details: a brass rule with a little knot
    d.line([(10, 134), (W - 11, 134)], fill=_hex('#a87a2a'))
    d.line([(10, 135), (W - 11, 135)], fill=_hex('#f2dca0'))
    for dx, dy in ((0, -2), (1, -3), (2, -2), (1, -1), (0, 0), (1, 1), (2, 2), (1, 3), (0, 2)):
        d.point((W // 2 + dx, 134 + dy), fill=_hex('#8c5c1e'))
    # the stave box of the chosen song
    d.rectangle([10, 170, 174, 214], fill=_hex('#f6ecd2'), outline=_hex('#b49a6a'))
    d.line([(11, 213), (173, 213)], fill=_hex('#d8c49a'))
    # the item well and the list well
    d.rectangle([11, 31, 102, 104], outline=_hex('#c4ab7c'))
    d.rectangle([109, 31, 266, 133], outline=_hex('#c4ab7c'))

    # slots: normal, chosen, hovered (18 x 18 at 280/298/316, 0)
    for i, (fill, edge_d, edge_l) in enumerate(((_hex('#cdb88c'), _hex('#7a5f3c'), _hex('#fbf1d8')),
                                                (_hex('#f2d27a'), _hex('#8c5c1e'), _hex('#fff4c8')),
                                                (_hex('#e2cfa2'), _hex('#7a5f3c'), _hex('#ffffff')))):
        x = 280 + i * 18
        d.rectangle([x, 0, x + 17, 17], fill=fill)
        d.line([(x, 0), (x + 16, 0)], fill=edge_d)
        d.line([(x, 0), (x, 16)], fill=edge_d)
        d.line([(x + 1, 17), (x + 17, 17)], fill=edge_l)
        d.line([(x + 17, 1), (x + 17, 17)], fill=edge_l)
        if i == 1:
            d.rectangle([x + 1, 1, x + 16, 16], outline=_hex('#c8922e'))
    # rows: ready, hovered, chosen, locked (156 x 12 at 280, 20/36/52/68)
    for i, (fill, line) in enumerate(((_hex('#ead9b0'), _hex('#c8b080')), (_hex('#f6e8c4'), _hex('#d8a84a')),
                                      (_hex('#f6d888'), _hex('#a8741e')), (_hex('#d6c8aa'), _hex('#b4a484')))):
        y = 20 + i * 16
        d.rectangle([280, y, 280 + 155, y + 11], fill=fill, outline=line)
        if i == 2:
            d.line([(281, y + 1), (434, y + 1)], fill=_hex('#fff2c0'))
    # buttons: ready, hovered, unavailable (82 x 16 at 280, 84/102/120)
    for i, (fill, top, bot) in enumerate(((_hex('#4a7a3a'), _hex('#7ab05a'), _hex('#2a4a1e')),
                                          (_hex('#5a9a46'), _hex('#9ad07a'), _hex('#2a4a1e')),
                                          (_hex('#6a5a4a'), _hex('#8a7a6a'), _hex('#3a2e24')))):
        y = 84 + i * 18
        d.rectangle([280, y, 361, y + 15], fill=fill, outline=_hex('#24150d'))
        d.line([(281, y + 1), (360, y + 1)], fill=top)
        d.line([(281, y + 14), (360, y + 14)], fill=bot)
        for bx in (283, 357):
            d.rectangle([bx, y + 6, bx + 1, y + 8], fill=_hex('#f2cc6a') if i < 2 else _hex('#9a8a78'))
    # icons (10 x 10 at y 0): sheet, band, levels, shards, note
    _icon(img, 440, 0, ['..pppppp..', '.pPPPPPPp.', '.pPllllPp.', '.pPPPPPPp.', '.pPllllPp.', '.pPPPkPPp.', '.pPllklPp.', '.pPPkkPPp.',
                        '.pPPPPPPp.', '..pppppp..'], {'p': '#6e4c2a', 'P': '#f6ecd0', 'l': '#c9b28a', 'k': '#2a1c14'})
    _icon(img, 450, 0, ['..p....p..', '.ppp..ppp.', '..p....p..', '....pp....', '.p.pppp.p.', '.ppppppp..', '..pppppp..', '..pppppp..',
                        '...pppp...', '..........'], {'p': '#8a4a2a'})
    _icon(img, 460, 0, ['...gggg...', '..gGGGGg..', '.gGYYYYGg.', '.gGYyyYGg.', '.gGYyyYGg.', '.gGYYYYGg.', '..gGGGGg..', '...gggg...',
                        '..........', '..........'], {'g': '#2e7a10', 'G': '#7ae030', 'Y': '#c8f86a', 'y': '#ffffd0'})
    _icon(img, 470, 0, ['....v.....', '...vVv....', '...vVVv...', '..vVWVv...', '..vVWVVv..', '.vVVWVVv..', '.vVVVVVv..', '..vVVVv...',
                        '...vvv....', '..........'], {'v': '#5e3aa8', 'V': '#9a6ef0', 'W': '#d8c0ff'})
    _icon(img, 480, 0, ['......k...', '......kk..', '......k.k.', '......k..k', '......k...', '......k...', '..kkkkk...', '.knnnnk...',
                        '.knnnnk...', '..kkkk....'], {'k': '#3a2414', 'n': '#c8922e'})
    # the families (9 x 9 at y 20): flute, drum, strings, chimes, any
    fam = [
        ['........f', '.......f.', '......F..', '.....f...', '....F....', '...f.....', '..F......', '.f.......', 'f........'],
        ['.........', '.ttttttt.', 'tTTTTTTTt', 'tRRRRRRRt', 'tRbRbRbRt', 'tRRRRRRRt', 'tRbRbRbRt', '.ttttttt.', '.........'],
        ['..ggg....', '.g...g...', '.g.s.g...', '..gsgg...', '...s.....', '...s.....', '..sss....', '.ggggg...', '.ggggg...'],
        ['ccccccccc', '.s..s..s.', '.S..S..S.', '.S..S..S.', '.S..S..S.', '.S.....S.', '.S.....s.', '.s.......', '.........'],
        ['....k....', '....kk...', '....k.k..', '....k....', '....k....', '..kkk....', '.kkkk....', '.kkkk....', '..kk.....'],
    ]
    pals = [{'f': '#c8922e', 'F': '#2a1c14'}, {'t': '#5a3a1a', 'T': '#ecdcb6', 'R': '#9a2f3e', 'b': '#f2cc6a'},
            {'g': '#8a5c3c', 's': '#d8d0bc'}, {'c': '#8a5c3c', 's': '#2a2a36', 'S': '#c8d2dc'}, {'k': '#3a2414'}]
    for i, (rows, p) in enumerate(zip(fam, pals)):
        _icon(img, 440 + i * 9, 20, rows, p)
    return img


def _icon(img, x0, y0, rows, pal):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                img.putpixel((x0 + x, y0 + y), _hex(pal[ch]))


# the enchanted books: the vanilla book recoloured in each enchantment's colours, with its emblem on the cover
BOOK_COVER = {  # cover (light, mid, dark), ribbon, emblem colour, emblem
    'melody_steps': (('#7fdcb4', '#3fae8a', '#1e6a52'), '#ffd34a', '#fffbe6', 'note'),
    'hushed_step': (('#9a8ad0', '#5a4a8a', '#2e2450'), '#c4b8ff', '#e8e0ff', 'hush'),
    'sculk_ward': (('#2a8a96', '#0f4a55', '#062a30'), '#5fe9ff', '#7ff3ff', 'shield'),
    'echo_strike': (('#2c4a58', '#13262e', '#081418'), '#29dfeb', '#5fe9ff', 'rings'),
    'resonance': (('#d04a5c', '#8e2c3c', '#4e1420'), '#f2cc6a', '#fbf1d8', 'drum'),
    'crescendo': (('#f0a050', '#b8642a', '#6a3412'), '#fff0a0', '#fff4d8', 'hairpin'),
    'reverb': (('#7ab8f0', '#3a7ab8', '#1a3e6a'), '#ffffff', '#e8f6ff', 'waves'),
    'fortissimo': (('#f2cc6a', '#b8862a', '#6a4a10'), '#ff6a8a', '#fff8e0', 'ff'),
}
EMBLEMS = {  # 5 x 5, slanted onto the cover from (8, 3)
    'note': ['...x.', '...xx', '...x.', '.xxx.', '.xx..'],
    'hush': ['.....', 'x...x', '.xxx.', '.....', '..x..'],
    'shield': ['xxxxx', 'x.x.x', 'xxxxx', '.x.x.', '..x..'],
    'rings': ['.xxx.', 'x...x', 'x.x.x', 'x...x', '.xxx.'],
    'drum': ['xxxxx', 'x.x.x', 'xx.xx', 'x.x.x', 'xxxxx'],
    'hairpin': ['....x', '..xx.', 'xx...', '..xx.', '....x'],
    'waves': ['x.x..', '.x.x.', '..x.x', '.x.x.', 'x.x..'],
    'ff': ['.x..x', 'x..x.', 'xx.xx', 'x..x.', 'x..x.'],
}
# the vanilla enchanted book's colours by role: cover (light 0, mid 1, dark 2), ribbon, pages; the brass clasp stays
_ROLES = {(0x31, 0x21, 0x04): 'a', (0x52, 0x2e, 0x10): 'b', (0x65, 0x4b, 0x17): 'c', (0x6c, 0x17, 0x17): 'd', (0xa4, 0x2c, 0x2b): 'e',
          (0xc5, 0x13, 0x39): 'f', (0x44, 0x33, 0x10): 'g', (0x54, 0x3e, 0x13): 'h', (0x9d, 0x1b, 0x37): 'i', (0x44, 0x25, 0x0a): 'j',
          (0x5b, 0x5b, 0x5b): 'k', (0xb7, 0xb7, 0xb7): 'l', (0xd3, 0xa2, 0x16): 'm', (0x99, 0x99, 0x99): 'n', (0x16, 0x10, 0x05): 'o',
          (0xd6, 0xd6, 0xd6): 'p', (0xd4, 0x96, 0x1a): 'q', (0x89, 0x21, 0x20): 'r', (0x61, 0x14, 0x14): 's'}
_COVER = {'a': 2, 'o': 2, 'j': 2, 'b': 1, 'g': 1, 'h': 1, 'c': 0}
_RIBBON = {'d', 'e', 'f', 'i', 'r', 's'}
_PAGES = {'k': '#8a7a5a', 'l': '#e6d8b0', 'n': '#c4b48a', 'p': '#fbf1d8'}
VANILLA_ITEMS = os.environ.get('VANILLA_ITEM_TEX', '/home/user/ref/mc-tex/assets/minecraft/textures/item')


def books():
    path = os.path.join(VANILLA_ITEMS, 'enchanted_book.png')
    base = Image.open(path).convert('RGBA') if os.path.exists(path) else None
    out = {}
    for eid, (cover, ribbon, ink, emblem) in BOOK_COVER.items():
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        for y in range(16):
            for x in range(16):
                c = base.getpixel((x, y)) if base else ((0x65, 0x4b, 0x17, 255) if 2 <= x + y <= 26 and abs(x - y) < 9 else (0, 0, 0, 0))
                if c[3] == 0:
                    continue
                r = _ROLES.get(c[:3], 'c')
                if r in _COVER:
                    col = _hex(_darker(cover[2])) if r in ('o', 'a') else _hex(cover[_COVER[r]])
                    if r == 'a' and (x + y) % 3 == 0:
                        col = _hex(cover[2])
                elif r in _RIBBON:
                    col = _hex(ribbon) if r in ('e', 'f', 'i') else _hex(_darker(ribbon))
                elif r in _PAGES:
                    col = _hex(_PAGES[r])
                else:
                    col = c
                img.putpixel((x, y), col)
        for j, row in enumerate(EMBLEMS[emblem]):
            for i, ch in enumerate(row):
                if ch == 'x':
                    x, y = 8 + i - j // 2, 3 + j
                    if img.getpixel((x, y))[3]:
                        img.putpixel((x, y), _hex(ink))
        out[eid] = img
    return out


def _darker(c):
    r, g, b, _ = _hex(c)
    return '#%02x%02x%02x' % (int(r * 0.55), int(g * 0.5), int(b * 0.55))
