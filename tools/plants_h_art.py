"""Agent H's art: the three Pitcher soups, the Sift Gate Frame and the Pitcher
Planter. Item sprites are hand-placed 16x16 rows in the items16.py style; the two blocks are drawn
pixel by pixel. Light comes from the top left, like everything else.

items16.all_items() takes item_sprites(); gen_textures.main() writes block_textures().
"""
from __future__ import annotations

import random

from PIL import Image

import items16 as I

# the vanilla stew bowl (as in dream_stew), with room for three broth tones (h, i, j) and toppings
BOWL = {'a': '#22140e', 'b': '#3a2414', 'c': '#4a2e18', 'd': '#52341c', 'e': '#5e3c20', 'f': '#734a26', 'g': '#8f5e32'}
_EMPTY = '................'
_BOWL_BOTTOM = ['..dgfeeeeeefca..', '...bggggggfca...', '....aafggfaa....', '......aaaa......', _EMPTY, _EMPTY, _EMPTY]

SOUPS = {
    # pale gold broth with cyan lullaby-bell petals (P, p) and white soulpetal flecks (k)
    'lullaby_soup': (['.....dddddd.....', '...ddhpPiihdd...', '..dijjkjjipPha..', '..dhiPpijkiiha..'],
                     {'h': '#c99a3a', 'i': '#e9c35c', 'j': '#fbe39a', 'p': '#3fb8d6', 'P': '#8ee8f6', 'k': '#f6f2ff'}),
    # dark teal chowder with glowing echo bits (u) and a pale glowcap slice (M, m)
    'echo_chowder': (['.....dddddd.....', '...ddhhuihhdd...', '..dhiiMmmiuiha..', '..dhuihMiiihua..'],
                     {'h': '#0f3a48', 'i': '#1b5a66', 'j': '#2a7a80', 'u': '#5ff5e6', 'M': '#b8f0ff', 'm': '#3fc0d8'}),
    # shimmering lavender chrome bisque with a curl of chrome reed (r) and pearly glints (w)
    'chrome_bisque': (['.....dddddd.....', '...ddhijwjihd...', '..dijjrrjjiwha..', '..dhiijjrijiha..'],
                      {'h': '#6a5ac8', 'i': '#9a8ee8', 'j': '#c8c2ff', 'w': '#ffffff', 'r': '#5cba58'}),
}

def item_sprites():
    out = {}
    for name, (top, colours) in SOUPS.items():
        rows = [_EMPTY] * 5 + top + _BOWL_BOTTOM
        pal = dict(BOWL)
        pal.update(colours)
        out[name] = I.grid(rows, pal)
    return out


def _gate_frame(top):
    """Siftite plate engraved with a diagonal lattice, pink rivets in the corners and an inset
    glowing from its core: an echo shard on the sides, the sealed nether star on top."""
    R = I.S_RAMP
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            ax, ay = abs(x - 7.5), abs(y - 7.5)
            d = ax + ay
            if top:
                star = min(d, ax + ay * 0.35, ay + ax * 0.35)  # a four-pointed star
                inset, ring = star <= 4.6, star <= 5.4
                core = star
            else:
                inset, ring = d <= 5.0, d <= 6.0
                core = d
            if x in (0, 15) or y in (0, 15):
                c = I.S_OUT
            elif x == 1 or y == 1:
                c = R[4]
            elif x == 14 or y == 14:
                c = R[1]
            elif inset:
                c = '#ffffff' if core <= 1.0 else '#a8fff6' if core <= 2.0 else '#3ff5e6' if core <= 3.0 else '#22c7c4' if core <= 4.0 else '#123a48'
            elif ring:
                c = I.S_OUT2
            elif (x + y) % 4 == 0 or (x - y) % 4 == 0:
                c = R[2]
            else:
                c = R[3] if x + y < 16 else I.mix(R[3], R[2], 0.5)
            px[x, y] = I.rgba(c)
    for (x, y) in ((3, 3), (11, 3), (3, 11), (11, 11)):
        px[x, y] = I.rgba(I.PINK[3])
        px[x + 1, y] = I.rgba(I.PINK[2])
        px[x, y + 1] = I.rgba(I.PINK[2])
        px[x + 1, y + 1] = I.rgba(I.PINK[1])
    return img


def block_textures():
    out = {'block/sift_gate_frame_side': _gate_frame(False), 'block/sift_gate_frame_top': _gate_frame(True)}
    rnd = random.Random(4242)
    brick = ['#a8445a', '#c25a6c', '#d77385', '#e891a0']
    mortar = '#6a2a3c'

    def bricks(glaze_row=None):
        img = Image.new('RGBA', (16, 16))
        px = img.load()
        for y in range(16):
            for x in range(16):
                xo = (x + (4 if (y // 4) % 2 else 0)) % 8
                if y % 4 == 3 or xo == 7:
                    c = mortar
                else:
                    k = (2 if y % 4 == 0 or xo == 0 else 1) + rnd.choice((-1, 0, 0, 1))
                    c = brick[max(0, min(3, k))]
                px[x, y] = I.rgba(c)
        if glaze_row is not None:
            # the glazed cyan band just under the lip
            for x in range(16):
                px[x, glaze_row] = I.rgba('#eefbff' if x % 4 == 1 else '#7ed8ee')
                px[x, glaze_row + 1] = I.rgba('#3fa2cc')
        return img

    out['block/pitcher_planter_side'] = bricks(7)
    out['block/pitcher_planter_bottom'] = bricks()
    rim = Image.new('RGBA', (16, 16))
    px = rim.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = I.rgba('#eefbff' if y == 0 else rnd.choice(('#7ed8ee', '#6ab0e8', '#7ed8ee', '#b8f0f6')))
    out['block/pitcher_planter_rim'] = rim
    mud = Image.new('RGBA', (16, 16))
    px = mud.load()
    for y in range(16):
        for x in range(16):
            c = rnd.choice(('#3a2a2e', '#45333a', '#4f3c42', '#3a2a2e'))
            if rnd.random() < 0.06:
                c = '#6ab0e8'  # water glinting in the wet mud
            px[x, y] = I.rgba(c)
    out['block/pitcher_planter_top'] = mud
    return out
