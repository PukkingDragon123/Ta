"""CR4: the Cypole (`thesift:cypole`), the one-eyed cymbal frog of the Sculk Swamp.

A big, squat swamp frog with a single huge golden eye on top of its head, brass tympana on its
cheeks and a pair of brass cymbal plates hanging under its chin either side of its vocal sac. It
croaks in rhythm with every other Cypole around (the plates tick together like a hi-hat), loves to
float in Sculk Water with only its eye showing, and guards the water around it: come too close and
it rattles its plates at you; keep coming and it rears up and crashes them together - a ring
shockwave races out across the ground (jump it!) - or shoots its long sticky tongue to yank you in.

Everything generated for it lives here, hooked through tools/cave_creatures.py (the CR4 module):
declare() (spawn egg), MODELS, SOUNDS/SUBTITLES, data() (loot, tags, particle, band and Codex text),
items() (spawn egg art) and textures() (the shockwave ring particle). Java: registry/ModCaveCreatures
(type, sounds, particle, spawn rules, band voice), entity/swamp/Cypole, client/model/CypoleModel,
client/renderer/CypoleRenderer, client/particle/CymbalRingParticle.
"""
from __future__ import annotations

import os

NS = 'thesift'


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# =========================================================================== spec (items)

def declare(block, item):
    item('cypole_spawn_egg', cls='SpawnEggItem', props='new Item.Properties().spawnEgg(ModCaveCreatures.CYPOLE.get())', tab='eggs')


# =========================================================================== the model
# S2: the Cypole's model and hand-painted texture are in tools/waterfolk.py (its mouth stays at (0, 15.6, -10.6):
# Cypole.MOUTH_FORWARD / MOUTH_UP in Java)
MODELS = {'cypole': lambda: __import__('waterfolk').cypole()}


# =========================================================================== sounds (vanilla files and events)
SOUNDS = {
    'entity.cypole.croak': [('mob/frog/idle1', 1.0, 0.62), ('mob/frog/idle2', 1.0, 0.58), ('mob/frog/idle4', 1.0, 0.6), ('mob/frog/idle6', 1.0, 0.64),
                            ('mob/frog/idle7', 1.0, 0.56)],
    'entity.cypole.tick': [('event:block.note_block.hat', 0.9, 1.25), ('event:block.note_block.hat', 0.9, 1.4)],
    'entity.cypole.rattle': [('event:block.note_block.snare', 0.7, 1.9), ('mob/warden/tendril_clicks_2', 0.9, 1.9),
                             ('mob/warden/tendril_clicks_4', 0.9, 1.8)],
    'entity.cypole.angry': [('mob/frog/idle2', 1.0, 0.42), ('mob/frog/idle7', 1.0, 0.45)],
    'entity.cypole.calm': [('mob/frog/idle1', 0.8, 1.15), ('mob/frog/idle4', 0.8, 1.2)],
    'entity.cypole.windup': [('mob/pufferfish/blow_up1', 1.0, 0.6), ('mob/pufferfish/blow_up2', 1.0, 0.65)],
    'entity.cypole.crash': [('event:block.anvil.land', 0.8, 1.85), ('event:block.anvil.land', 0.8, 2.0)],
    'entity.cypole.shimmer': [('block/bell/resonate', 1.0, 1.6), ('block/amethyst/shimmer', 1.0, 1.4)],
    'entity.cypole.shockwave': [('entity/wind_charge/wind_burst1', 1.0, 0.8), ('entity/wind_charge/wind_burst2', 1.0, 0.75),
                                ('item/mace/smash_ground_heavy', 0.9, 1.1)],
    'entity.cypole.tongue': [('mob/frog/tongue1', 1.0, 0.7), ('mob/frog/tongue2', 1.0, 0.75)],
    'entity.cypole.slap': [('mob/slime/attack1', 1.0, 0.8), ('mob/slime/big1', 0.8, 1.2), ('mob/slime/big2', 0.8, 1.1)],
    'entity.cypole.shielded': [('event:item.shield.block', 1.0, 1.1)],
    'entity.cypole.hop': [('mob/slime/small1', 0.6, 0.75), ('mob/slime/small3', 0.6, 0.7), ('mob/slime/small4', 0.6, 0.8)],
    'entity.cypole.hurt': [('mob/frog/hurt1', 1.0, 0.7), ('mob/frog/hurt2', 1.0, 0.72), ('event:block.chain.hit', 0.6, 1.6)],
    'entity.cypole.death': [('mob/frog/death1', 1.0, 0.7), ('mob/frog/death2', 1.0, 0.68)],
}
SUBTITLES = {
    'entity.cypole.croak': 'Cypole croaks', 'entity.cypole.tick': 'Brass plates tick', 'entity.cypole.rattle': 'Cypole rattles its plates',
    'entity.cypole.angry': 'Cypole croaks angrily', 'entity.cypole.calm': 'Cypole croaks contentedly', 'entity.cypole.windup': 'Cypole swells',
    'entity.cypole.crash': 'Cymbals crash', 'entity.cypole.shimmer': 'Cymbals ring', 'entity.cypole.shockwave': 'Shockwave rolls',
    'entity.cypole.tongue': 'Cypole lashes its tongue', 'entity.cypole.slap': 'Tongue slaps', 'entity.cypole.shielded': 'Shield slaps a tongue away',
    'entity.cypole.hop': 'Cypole hops', 'entity.cypole.hurt': 'Cypole hurts', 'entity.cypole.death': 'Cypole dies',
}


def sounds(GA):
    GA.SOUNDS.update(SOUNDS)
    GA.SUBTITLES.update(SUBTITLES)


# =========================================================================== data: loot, tags, the particle, text

def data(GA):
    import gen_data as D
    # brass and a sticky tongue: copper nuggets and a slime ball
    D.table('entity', 'entities/cypole', [
        D.pool([D.item('minecraft:copper_nugget', count=(1, 3), extra=[D.LOOTING])]),
        D.pool([D.item('minecraft:slime_ball', count=(0, 1), extra=[D.LOOTING])]),
    ])
    # at home in Sculk Water: it soaks there without being corrupted
    GA.tag('entity_type', f'{NS}:sculk_water_dwellers', rl('cypole'))
    # the shockwave ring
    GA.write(os.path.join(GA.A, 'particles', 'cymbal_ring.json'), {'textures': [f'{NS}:cymbal_ring']})
    GA.TEXTURES.add('particle/cymbal_ring')
    GA.LANG.update({
        f'entity.{NS}.cypole': 'Cypole',
        f'band.{NS}.instrument.cymbals': 'Cymbals',
        f'codex.{NS}.cypole.title': 'Cypole',
        f'codex.{NS}.cypole.tagline': 'Neutral - a one-eyed cymbal frog',
        f'codex.{NS}.cypole.body': ('A big swamp frog with one great golden eye and brass plates for cheeks and throat. Cypoles float in '
                                    'Sculk Water and croak in rhythm, the whole swamp on one beat. Come close and one rattles its '
                                    'plates; come closer and it fights. It rears up and crashes its plates: a ring shockwave rolls out '
                                    'across the ground - jump it! Or it shoots its tongue to yank you in: keep moving sideways, or '
                                    'raise a shield. Hit one and its neighbours join in. A drum or chimes song calms them, and they '
                                    'may join your band on cymbals.'),
    })


# =========================================================================== art: the spawn egg and the shockwave ring

def items():
    """S2: the Cypole's spawn egg is drawn with the other water creatures' (tools/fish_items.py)."""
    return {'cypole_spawn_egg': __import__('fish_items').cypole_egg()}


def textures(out):
    """The shockwave ring (64 x 64, lying flat): a bright brass crest at the very edge (the quad's
    half-size is the ring's radius), a hot teal line just inside it and a soft fading wake behind."""
    import numpy as np
    from PIL import Image
    n = 64
    c = (n - 1) / 2
    img = np.zeros((n, n, 4), dtype=np.float64)
    yy, xx = np.mgrid[0:n, 0:n]
    d = np.hypot(xx - c, yy - c) / (n / 2)
    ang = np.arctan2(yy - c, xx - c)
    crest = np.clip(1.0 - np.abs(d - 0.955) / 0.035, 0, 1)
    teal = np.clip(1.0 - np.abs(d - 0.9) / 0.03, 0, 1)
    wake = np.where((d < 0.9) & (d > 0.62), ((d - 0.62) / 0.28) ** 2, 0.0)
    ripple = 0.75 + 0.25 * np.cos(ang * 22.0 + d * 30.0)
    brass = np.array([255, 214, 120], dtype=np.float64)
    hot = np.array([150, 255, 240], dtype=np.float64)
    dust = np.array([214, 196, 150], dtype=np.float64)
    col = (brass * crest[..., None] + hot * teal[..., None] * 0.9 + dust * wake[..., None] * 0.5)
    alpha = np.clip(crest * 255 + teal * 200 + wake * 90 * ripple, 0, 255)
    weight = np.clip(crest + teal * 0.9 + wake * 0.5, 1e-6, None)
    img[..., :3] = np.clip(col / weight[..., None], 0, 255)
    img[..., 3] = np.where(d <= 1.0, alpha, 0)
    out('particle/cymbal_ring', Image.fromarray(img.astype(np.uint8), 'RGBA'))
