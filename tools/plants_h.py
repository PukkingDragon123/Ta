"""Agent H: the Sift's plants and portal - potted pitcher plants, their three soups, plant habitats,
the Sift Gate Frame and Siftite's new cost.

Called from gen_assets.generate() (assets, recipes, tags, text), gen_assets.gen_block() (the
planter's model), items16.all_items() (item sprites) and gen_textures.main() (block textures).
"""
from __future__ import annotations

import os

NS = 'thesift'
SOUPS = ('lullaby_soup', 'echo_chowder', 'chrome_bisque')


# ============================================================================ models

def gen_planter(GA, bid):
    """Five models: an open planter of wet mud, then the potted Pitcher Bulb bush growing out of it
    through the bush's own four stages (so it always matches the wild bush)."""
    side, top, bottom = f'{NS}:block/{bid}_side', f'{NS}:block/{bid}_top', f'{NS}:block/{bid}_bottom'
    rim = f'{NS}:block/{bid}_rim'

    def box(frm, to, tex_side, tex_top, tex_bottom, uv_side=None):
        f = {}
        for d in ('north', 'south', 'east', 'west'):
            f[d] = {'texture': '#side' if tex_side == side else '#rim', **({'uv': uv_side} if uv_side else {})}
        f['up'] = {'texture': '#top' if tex_top == top else '#rim'}
        f['down'] = {'texture': '#bottom' if tex_bottom == bottom else '#rim', 'cullface': 'down'} if frm[1] == 0 else {'texture': '#rim'}
        return {'from': frm, 'to': to, 'faces': f}

    planter = [
        box([1, 0, 1], [15, 9, 15], side, top, bottom, [1, 7, 15, 16]),
        # a lip of glazed brick around the mud, one pixel proud
        box([0, 9, 0], [16, 10, 2], rim, rim, rim, [0, 0, 16, 1]),
        box([0, 9, 14], [16, 10, 16], rim, rim, rim, [0, 0, 16, 1]),
        box([0, 9, 2], [2, 10, 14], rim, rim, rim, [0, 0, 12, 1]),
        box([14, 9, 2], [16, 10, 14], rim, rim, rim, [0, 0, 12, 1]),
        # the mud surface, sunk just below the lip
        {'from': [2, 8.5, 2], 'to': [14, 9.5, 14], 'faces': {'up': {'texture': '#top'}}},
    ]

    def cross(tex):
        out = []
        for angle in (45, -45):
            out.append({'from': [0.8, 9, 8], 'to': [15.2, 25, 8], 'shade': False,
                        'rotation': {'origin': [8, 17, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                        'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#plant'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#plant'}}})
        return out

    variants = {}
    for stage in range(5):
        name = bid if stage == 0 else f'{bid}_stage{stage}'
        tex = {'particle': side, 'side': side, 'top': top, 'bottom': bottom, 'rim': rim}
        elements = list(planter)
        if stage > 0:
            tex['plant'] = f'{NS}:block/pitcher_bulb_bush_stage{stage - 1}'
            elements += cross(tex['plant'])
        m = {'parent': 'minecraft:block/block', 'ambientocclusion': False, 'render_type': 'minecraft:cutout', 'textures': tex, 'elements': elements}
        GA.note_textures(m)
        GA.write(os.path.join(GA.A, 'models/block', name + '.json'), m)
        variants[f'stage={stage}'] = {'model': f'{NS}:block/{name}'}
    GA.write(os.path.join(GA.A, 'blockstates', bid + '.json'), {'variants': variants})
    GA.item_block(bid, f'{bid}_stage4')


# ============================================================================ data and text

def generate(GA):
    shaped, shapeless, tag, rl, LANG = GA.shaped, GA.shapeless, GA.tag, GA.rl, GA.LANG
    # --- the potted pitcher and its soups
    shaped('pitcher_planter', ['B B', 'BMB', 'BBB'], {'B': 'blush_bricks', 'M': 'minecraft:mud'}, 'pitcher_planter', 1, 'decorations')
    shapeless('lullaby_soup', ['minecraft:bowl', 'pitcher_nectar', 'lullaby_bell', 'choir_pod'], 'lullaby_soup', 1, 'food')
    shapeless('echo_chowder', ['minecraft:bowl', 'pitcher_nectar', 'echo_orchid', 'glowcap'], 'echo_chowder', 1, 'food')
    shapeless('chrome_bisque', ['minecraft:bowl', 'pitcher_nectar', 'chrome_reeds', 'glowing_slime_ball'], 'chrome_bisque', 1, 'food')
    # Bulbs love the nectar as much as the bulbs themselves
    tag('item', f'{NS}:bulb_food', rl('pitcher_nectar'))
    # --- the Sift Gate Frame: the only craftable portal frame, and priced like one
    shaped('sift_gate_frame', ['SES', 'ENE', 'SES'], {'S': 'siftite_ingot', 'E': 'minecraft:echo_shard', 'N': 'minecraft:nether_star'},
           'sift_gate_frame', 4, 'misc')
    tag('block', 'minecraft:wither_immune', rl('sift_gate_frame'))
    tag('block', 'minecraft:dragon_immune', rl('sift_gate_frame'))
    LANG.update({
        f'message.{NS}.drum.already_open': 'This gate is already open. Keep the core for another one.',
        f'message.{NS}.drum.gate_broken': 'The frame broke while the gate closed! Mend it and strike the drum again.',
        f'codex.{NS}.pitcher_planter.title': 'Pitcher Planter', f'codex.{NS}.pitcher_planter.tagline': 'Soup grows on it',
        f'codex.{NS}.pitcher_planter.body': 'Pot a Pitcher Bulb in this planter of wet mud: it grows anywhere until its pitchers brim with nectar. Pick it by hand and it refills. Nectar in a bowl with Lullaby Bell and Choir Pod: Lullaby Soup (heals, golden hearts). Echo Orchid and Glowcap: Echo Chowder (night vision, haste). Chrome Reeds and Glowing Slime: Chrome Bisque (breathe and glide in water).',
        f'codex.{NS}.sift_gardening.title': 'Sift Gardening', f'codex.{NS}.sift_gardening.tagline': 'Every seed has its place',
        f'codex.{NS}.sift_gardening.body': 'Each seed a Sniffer digs up here wants its own place. Choir Pods grow only under open sky (magenta dye, Lullaby Soup). Echo Seeds grow only in the dark, faster on sculk (light blue dye, Echo Chowder). Pitcher bushes need water or Chrome within 4 blocks (food, Bulb breeding, planters). Music speeds only a happy plant.',
    })

