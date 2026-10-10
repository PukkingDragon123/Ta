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
    """Five models: an open planter of wet mud, then a vanilla Pitcher Plant growing out of it through
    the pitcher crop's own stages (W1: the old Pitcher Bulb bush is gone), the last one the full bloom,
    scaled down a little so the two-block plant fits over the pot."""
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

    def cross(key, y0, y1):
        out = []
        half = (y1 - y0) * 0.45  # as wide as a vanilla cross (0.8-15.2) for a full-height plane
        for angle in (45, -45):
            out.append({'from': [8 - half, y0, 8], 'to': [8 + half, y1, 8], 'shade': False,
                        'rotation': {'origin': [8, (y0 + y1) / 2.0, 8], 'axis': 'y', 'angle': angle, 'rescale': True},
                        'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': '#' + key}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#' + key}}})
        return out

    variants = {}
    for stage in range(5):
        name = bid if stage == 0 else f'{bid}_stage{stage}'
        tex = {'particle': side, 'side': side, 'top': top, 'bottom': bottom, 'rim': rim}
        elements = list(planter)
        if stage in (1, 2):  # a young pitcher, one block tall
            tex['plant'] = f'minecraft:block/pitcher_crop_bottom_stage_{stage}'
            elements += cross('plant', 9, 25)
        elif stage >= 3:  # two blocks tall: the stalk and the pitcher flower, scaled to fit
            tex['plant'] = f'minecraft:block/pitcher_crop_bottom_stage_{stage}'
            tex['bloom'] = f'minecraft:block/pitcher_crop_top_stage_{stage}'
            elements += cross('plant', 9, 20.5) + cross('bloom', 20.5, 32)
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
    # W1: the soups are cooked from a vanilla Pitcher Plant (grown in the planter, or picked wild)
    shapeless('lullaby_soup', ['minecraft:bowl', 'minecraft:pitcher_plant', 'lullaby_bell', 'soulpetal'], 'lullaby_soup', 1, 'food')
    shapeless('echo_chowder', ['minecraft:bowl', 'minecraft:pitcher_plant', 'echo_orchid', 'glowcap'], 'echo_chowder', 1, 'food')
    shapeless('chrome_bisque', ['minecraft:bowl', 'minecraft:pitcher_plant', 'chrome_reeds', 'glowing_slime_ball'], 'chrome_bisque', 1, 'food')
    # --- the Sift Gate Frame: the only craftable portal frame, and priced like one
    shaped('sift_gate_frame', ['SES', 'ENE', 'SES'], {'S': 'siftite_ingot', 'E': 'minecraft:echo_shard', 'N': 'minecraft:nether_star'},
           'sift_gate_frame', 4, 'misc')
    tag('block', 'minecraft:wither_immune', rl('sift_gate_frame'))
    tag('block', 'minecraft:dragon_immune', rl('sift_gate_frame'))
    LANG.update({
        # MANSION: the drum's ritual messages are gone (the Sculk Summoner's are in tools/mansion.py)
        f'codex.{NS}.pitcher_planter.title': 'Pitcher Planter', f'codex.{NS}.pitcher_planter.tagline': 'Soup grows on it',
        f'codex.{NS}.pitcher_planter.body': 'Pot a Pitcher Pod in this planter of wet mud: it grows anywhere into a full Pitcher Plant. Pick the bloom by hand - now and then a pod drops with it - and the plant grows back. A Pitcher Plant in a bowl with a Lullaby Bell and a Soulpetal: Lullaby Soup (heals, golden hearts). With an Echo Orchid and a Glowcap: Echo Chowder (night vision, haste). With Chrome Reeds and a Glowing Slime Ball: Chrome Bisque (breathe and glide in water).',
        f'codex.{NS}.sift_gardening.title': 'Sift Gardening', f'codex.{NS}.sift_gardening.tagline': 'Every seed has its place',
        f'codex.{NS}.sift_gardening.body': 'Each seed a Sniffer digs up here wants its own place. Echo Seeds grow only in the dark, faster on sculk (light blue dye, Echo Chowder). Wild Pitcher Plants grow in the plains, the groves and the Sculk Swamp; their pods grow anywhere in a Pitcher Planter (Bulb breeding, the Pitcher soups). Music speeds only a happy plant.',
    })

