"""A2 Echoer: the Echoer (entity id `enchoer`), Soul Golems and Nibs - model geometry and paint
(see modelkit.py), their sound events, and the data/lang that goes with them (hooked from
mobs.ALL, gen_assets and gen_data)."""
from modelkit import Model


def hd_rows(spec, w, h):
    rows = []
    for y in range(h):
        t = spec.get(y, '')
        rows.append((t + '.' * w)[:w])
    return rows


def mc(color, **kw):
    d = dict(color=color, pattern='mc')
    d.update(kw)
    return d


# =========================================================================== THE ECHOER
# the skull's front: 10 x 8 texels; the snout covers the lower middle. E glowing iris, I its white
# core, l a dark lid line.
ECHOER_FACE = {
    'neutral': {1: '.EE....EE.', 2: 'EIE....EIE', 3: '.EE....EE.'},
    'blink': {2: '.ll....ll.', 3: 'l..l..l..l'},
    'happy': {1: '.EE....EE.', 2: 'E..E..E..E'},
    'sleep': {3: '.ll....ll.'},
    'hurt': {1: 'E..l..l..E', 2: '.EE....EE.', 3: 'E........E'},
    'dead': {1: 'l.l....l.l', 2: '.l......l.', 3: 'l.l....l.l'},
}
# the glowing runes down each flank (30 x 16 texels on the 15 x 8 unit side face)
RUNES = {
    3: '...RRR.....R.R......RRR.......',
    4: '..R...R....RRR.....R...R..r...',
    5: '..R.R.R.....R......R.R.R.rrr..',
    6: '..R...R...RRRRR....R...R..r...',
    7: '...RRR......R.......RRR.......',
    8: '.....r......R.........r.......',
    9: '....rrr....R.R.......rrr......',
    10: '.....r.....................R..',
    11: '..........................RRR.',
}


def enchoer() -> Model:
    """A tall, graceful, moon-pale grazer: a slender deer-like body on long thin legs, an extremely
    long neck of seven swaying segments, a small elegant head with big glowing eyes and two
    swept-back horns tipped with light. Glowing runes are written down its flanks and a row of
    luminous nodes runs up the back of its neck."""
    pal = {
        'hide': '#e9e3f3', 'hide_l': '#f8f5fc', 'hide_d': '#c8bfdc',
        'belly': '#d7cde8', 'belly_l': '#e6def2', 'belly_d': '#b9addb',
        'neck': '#e2dbef', 'neck_l': '#f3effa', 'neck_d': '#bfb4d6',
        'hoof': '#4a3e66', 'hoof_l': '#5d5180', 'hoof_d': '#332a4a',
        'horn': '#b9a6e6', 'horn_l': '#d2c4f4', 'horn_d': '#8e7ac4',
        'snout': '#d9d0ea', 'snout_l': '#ebe5f5', 'snout_d': '#b6aacd', 'nostril': '#6e6190',
        'rune': '#7ff7ff', 'rune_d': '#3cc9dc', 'eye': '#8ffaff', 'eye_core': '#ffffff', 'lid': '#6e6190',
        'tuft': '#c6b6ee', 'tuft_l': '#ddd2f7', 'tuft_d': '#9d8bd1',
    }
    m = Model('enchoer', (128, 128), pal, {'enchoer': {}}, res=2, expressions=['blink', 'happy', 'sleep', 'hurt', 'dead'])
    hide = dict(color='hide', pattern='mc', clusters=0.18, bands=[(6, 'belly')])
    rk = {'R': 'rune', 'r': 'rune_d'}
    rune_side = hd_rows(RUNES, 30, 16)
    rune_side_r = [r[::-1] for r in rune_side]

    # --- four long, thin legs: thigh, shin, a dark hoof
    for name, (px, pz) in (('front_left_leg', (2.5, -5.5)), ('front_right_leg', (-2.5, -5.5)),
                           ('back_left_leg', (2.5, 5.0)), ('back_right_leg', (-2.5, 5.0))):
        leg = m.part(name, pivot=(px, 12, pz))
        leg.cube((-1.25, -2, -1.5), (2.5, 8, 3), **mc('hide', clusters=0.15))
        shin = leg.part(name.replace('leg', 'shin'), pivot=(0, 6, 0))
        shin.cube((-0.75, 0, -0.75), (1.5, 5, 1.5), **mc('hide', clusters=0.0, rim=False))
        hoof = shin.part(name.replace('leg', 'hoof'), pivot=(0, 5, 0))
        hoof.cube((-1, 0, -1.25), (2, 1, 2.5), **mc('hoof', clusters=0.0, rim=False))

    # --- the slender body, a deeper chest, a narrow rump
    body = m.part('body', pivot=(0, 6, 0))
    body.cube((-4, -4, -8), (8, 8, 15), **hide, faces={
        'west': dict(**hide, hd=True, map=rune_side_r, keys=rk, glow_keys='Rr'),
        'east': dict(**hide, hd=True, map=rune_side, keys=rk, glow_keys='Rr'),
        'up': dict(color='hide', pattern='mc', clusters=0.25, hd=True,
                   map=hd_rows({6: '.......RR.......', 7: '......R..R......', 8: '.......RR.......', 20: '.......rr.......'}, 16, 30),
                   keys=rk, glow_keys='Rr'),
        'down': dict(color='belly_d', pattern='mc', clusters=0.1),
    })
    body.cube((-3.5, -4.5, -9.5), (7, 7, 2), **mc('hide', clusters=0.1))
    body.cube((-3.5, -4.25, 6.5), (7, 6, 2), **mc('hide', clusters=0.15))
    tail = body.part('tail', pivot=(0, -3, 8.5), rot=(0.7, 0, 0))
    tail.cube((-1, -1, 0), (2, 2, 4), **mc('tuft', clusters=0.2, rim=False))
    tail_tip = tail.part('tail_tip', pivot=(0, 0, 4), rot=(0.3, 0, 0))
    tail_tip.cube((-1.5, -0.5, 0), (3, 1, 5), **mc('tuft', clusters=0.3, rim=False), faces={
        'up': dict(color='tuft', pattern='mc', clusters=0.2, hd=True, map=hd_rows({7: '..rr..', 8: '.rRRr.', 9: '..rr..'}, 6, 10),
                   keys=rk, glow_keys='Rr'),
    })

    # --- the neck: seven segments, each a little thinner, a glowing node on the back of each
    widths = [4.0, 3.6, 3.4, 3.2, 3.0, 2.8, 2.6]
    rots = [0.55, -0.05, -0.08, -0.08, -0.1, -0.12, -0.12]
    parent, pivot = body, (0, -3, -8)
    for i, wdt in enumerate(widths):
        seg = parent.part(f'neck_{i}', pivot=pivot, rot=(rots[i], 0, 0))
        ln = 5.0 if i == 0 else 4.5
        h = wdt / 2
        seg.cube((-h, -ln, -h), (wdt, ln, wdt), **mc('neck', clusters=0.12, rim=False), faces={
            'north': dict(color='belly', pattern='mc', clusters=0.0, rim=False),
        })
        seg.cube((-0.5, -ln + 1.5, h - 0.25), (1, 1.5, 1), color='rune', pattern='mc', clusters=0.0, rim=False, glow=True)
        parent, pivot = seg, (0, -ln + 0.3, 0)

    # --- the small elegant head
    head = parent.part('head', pivot=pivot, rot=(0.25, 0, 0))
    face_keys = {'E': 'eye', 'I': 'eye_core', 'l': 'lid'}
    head.cube((-2.5, -4, -2.5), (5, 4, 5), **mc('hide', clusters=0.1, rim=False), faces={
        'north': dict(color='hide', pattern='mc', clusters=0.0, rim=False, hd=True, map=hd_rows(ECHOER_FACE['neutral'], 10, 8),
                      keys=face_keys, glow_keys='EI', expr={k: hd_rows(v, 10, 8) for k, v in ECHOER_FACE.items() if k != 'neutral'}),
    })
    head.cube((-1.5, -2.5, -5.5), (3, 2.5, 3), **mc('snout', clusters=0.0, rim=False), faces={
        'north': dict(color='snout', pattern='mc', clusters=0.0, rim=False, hd=True, map=['......', '......', '.n..n.', '......', '......'],
                      keys={'n': 'nostril'}),
    })
    jaw = head.part('jaw', pivot=(0, 0, -2.5))
    jaw.cube((-1.25, -0.25, -2.75), (2.5, 1, 2.75), **mc('snout_d', clusters=0.0, rim=False))
    for side, sx in (('left', 1), ('right', -1)):
        horn = head.part(f'{side}_horn', pivot=(1.4 * sx, -4, 0.8), rot=(-0.7, 0, 0.18 * sx))
        horn.cube((-0.5, -3.5, -0.5), (1, 3.5, 1), **mc('horn', clusters=0.0, rim=False))
        tip = horn.part(f'{side}_horn_tip', pivot=(0, -3.5, 0), rot=(-0.55, 0, 0))
        tip.cube((-0.5, -2.5, -0.5), (1, 2.5, 1), **mc('horn_l', clusters=0.0, rim=False), faces={
            'north': dict(color='horn_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['RR', 'rr'], keys=rk, glow_keys='Rr'),
            'south': dict(color='horn_l', pattern='mc', clusters=0.0, rim=False, hd=True, map=['RR', 'rr'], keys=rk, glow_keys='Rr'),
            'up': dict(color='rune', pattern='mc', clusters=0.0, glow=True),
        })
        ear = head.part(f'{side}_ear', pivot=(2.5 * sx, -3, 1), rot=(0.2, 0.5 * sx, 0.6 * sx))
        ear.cube((0 if sx > 0 else -3, -0.5, -0.5), (3, 1, 1.5), **mc('tuft', clusters=0.0, rim=False))
    return m


# =========================================================================== SOUL GOLEM
GOLEM_EYE = {
    'neutral': {1: '.EEEE.', 2: 'EEIIEE', 3: 'EEIIEE', 4: '.EEEE.'},
    'blink': {3: '.llll.', 4: 'l....l'},
    'happy': {2: '.EEEE.', 3: 'E....E'},
    'sleep': {4: '.llll.'},
    'hurt': {1: 'E....E', 2: '.E..E.', 3: '..EE..', 4: '.E..E.'},
    'dead': {1: 'l....l', 2: '.l..l.', 3: '..ll..', 4: '.l..l.'},
}


def soul_golem() -> Model:
    """A small ancient construct of carved soulstone - a round frog-like body, two big domed eyes
    lit by the soul fire inside, a wide carved grin, stubby arms and legs, cyan cracks glowing
    along its seams and a soul-lamp stalk on top (the Copper Golem's lightning rod, if it were
    a lantern)."""
    pal = {
        'stone': '#6c5a4b', 'stone_l': '#87725f', 'stone_d': '#4b3e34',
        'belly': '#7d6a58', 'belly_l': '#97826d', 'belly_d': '#5c4c3f',
        'crack': '#5fe9ff', 'crack_d': '#2aa9c8', 'mouth': '#251c16',
        'eye': '#a9faff', 'eye_core': '#ffffff', 'lid': '#3a2f27',
        'lamp': '#7ff3ff', 'lamp_l': '#d6fdff', 'lamp_d': '#3cc3dc',
    }
    m = Model('soul_golem', (64, 64), pal, {'soul_golem': {}}, res=2, expressions=['blink', 'happy', 'sleep', 'hurt', 'dead'])
    ck = {'c': 'crack', 'C': 'crack_d', 'm': 'mouth'}
    front = hd_rows({1: '...c..........', 2: '...cc.........', 3: '....c.........', 7: '..mmmmmmmmmm..', 8: '...m......m...',
                     9: '....mmmmmm....', 11: '......CC......', 12: '.....CccC.....', 13: '......CC......'}, 16, 14)
    side = hd_rows({2: '.....c........', 3: '....cc........', 4: '....c....C....', 5: '.........CC...', 9: '..C...........',
                    10: '..CC..........'}, 14, 14)
    for name, (px, pz) in (('left_leg', (2, 0.5)), ('right_leg', (-2, 0.5))):
        leg = m.part(name, pivot=(px, 21, pz))
        leg.cube((-1.5, 0, -1.5), (3, 2, 3), **mc('stone', clusters=0.1, rim=False))
        leg.cube((-1.75, 2, -2.5), (3.5, 1, 4), **mc('stone_d', clusters=0.0, rim=False), faces={
            'north': dict(color='stone_d', pattern='mc', clusters=0.0, rim=False, hd=True, map=['c.c.c.c'], keys=ck, glow_keys='c'),
        })
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-4, -7, -3.5), (8, 7, 7), **mc('stone', clusters=0.25, bands=[(4, 'belly')]), faces={
        'north': dict(**mc('stone', clusters=0.0, bands=[(4, 'belly')]), hd=True, map=front, keys=ck, glow_keys='cC'),
        'west': dict(**mc('stone', clusters=0.2, bands=[(4, 'belly')]), hd=True, map=side, keys=ck, glow_keys='cC'),
        'east': dict(**mc('stone', clusters=0.2, bands=[(4, 'belly')]), hd=True, map=[r[::-1] for r in side], keys=ck, glow_keys='cC'),
    })
    # a rounder silhouette: soft bevels on the top and the sides
    body.cube((-3.5, -7.5, -3), (7, 0.5, 6), **mc('stone_l', clusters=0.2, rim=False))
    eyes = {k: hd_rows(v, 6, 6) for k, v in GOLEM_EYE.items()}
    for side_name, sx in (('left', 1), ('right', -1)):
        eye = body.part(f'{side_name}_eye', pivot=(2.2 * sx, -7, -1))
        eye.cube((-1.5, -2.5, -1.5), (3, 3, 3), **mc('stone', clusters=0.0, rim=False), faces={
            'north': dict(color='stone', pattern='mc', clusters=0.0, rim=False, hd=True, map=eyes['neutral'],
                          keys={'E': 'eye', 'I': 'eye_core', 'l': 'lid'}, glow_keys='EI',
                          expr={k: v for k, v in eyes.items() if k != 'neutral'}),
        })
        arm = body.part(f'{side_name}_arm', pivot=(4 * sx, -4.5, 0), rot=(0, 0, -0.15 * sx))
        arm.cube((0 if sx > 0 else -1.5, -0.5, -1), (1.5, 4, 2), **mc('stone', clusters=0.1, rim=False), faces={
            'down': dict(color='stone_d', pattern='mc', clusters=0.0),
        })
    stalk = body.part('stalk', pivot=(0, -7.5, 0.5))
    stalk.cube((-0.5, -3, -0.5), (1, 3, 1), **mc('stone_d', clusters=0.0, rim=False))
    lamp = stalk.part('lamp', pivot=(0, -3, 0))
    lamp.cube((-1, -2, -1), (2, 2, 2), **mc('lamp', clusters=0.3, rim=False, glow=True))
    return m


# =========================================================================== NIB
def nib() -> Model:
    """A tiny glowing wisp-butterfly: a bright little body and two pairs of see-through wings
    veined with light."""
    pal = {
        'glow': '#fff3b0', 'glow_l': '#fffbe6', 'glow_d': '#ffd36b',
        'wing': '#9ff4ff', 'wing_l': '#dafcff', 'wing_d': '#62c9f0', 'vein': '#ffffff', 'spot': '#ff9be3',
    }
    m = Model('nib', (32, 32), pal, {'nib': {}}, res=2)
    body = m.part('body', pivot=(0, 21, 0))
    body.cube((-0.5, -0.5, -1.5), (1, 1, 3), **mc('glow', clusters=0.0, rim=False, glow=True))
    body.cube((-0.75, -0.75, -2.75), (1.5, 1.5, 1.25), **mc('glow_l', clusters=0.0, rim=False, glow=True))
    wk = {'v': 'vein', 's': 'spot', 'w': 'wing_l'}
    up_wing = hd_rows({0: '......ww..', 1: '....wwvvw.', 2: '..wwvvsvw.', 3: '.wvv.ssvw.', 4: 'wvv...vvw.', 5: 'vv....vw..',
                       6: 'v....vw...', 7: 'v...vw....', 8: '.vvvw.....', 9: '..ww......'}, 10, 10)
    lo_wing = hd_rows({0: 'vvvv..', 1: 'v..vw.', 2: 'v.s.vw', 3: '.v..vw', 4: '..vvw.', 5: '...w..'}, 7, 7)
    for side, sx in (('left', 1), ('right', -1)):
        def mir(rows):
            return rows if sx > 0 else [r[::-1] for r in rows]
        wing = body.part(f'{side}_wing', pivot=(0.5 * sx, -0.5, -0.5), rot=(0, 0, -0.3 * sx))
        wspec = dict(color='wing', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True, hd=True, map=mir(up_wing), keys=wk)
        wing.cube((0 if sx > 0 else -5, 0, -4), (5, 0, 5), color='wing', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True,
                  faces={'up': wspec, 'down': wspec})
        low = body.part(f'{side}_wing_low', pivot=(0.5 * sx, -0.25, 0.5), rot=(0, 0, -0.2 * sx))
        lspec = dict(color='wing_d', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True, hd=True, map=mir(lo_wing), keys=wk)
        low.cube((0 if sx > 0 else -3.5, 0, 0), (3.5, 0, 3.5), color='wing_d', pattern='mc', clusters=0.0, rim=False, opacity=150, glow=True,
                 faces={'up': lspec, 'down': lspec})
        ant = body.part(f'{side}_antenna', pivot=(0.3 * sx, -0.75, -2.5), rot=(-0.6, 0, 0.3 * sx))
        ant.cube((0, -2, 0), (0, 2, 1), color='glow_d', pattern='mc', clusters=0.0, rim=False, glow=True)
    return m


ALL = {'soul_golem': soul_golem, 'nib': nib}
POSES = {}

# --------------------------------------------------------------------------- sounds (vanilla files)
SOUNDS = {
    'entity.soul_golem.ambient': [('block/amethyst/resonate1', 0.5, 1.6), ('block/amethyst/resonate3', 0.5, 1.7),
                                  ('mob/allay/idle_without_item1', 0.4, 1.3)],
    'entity.soul_golem.hurt': [('block/basalt/break1', 0.8, 1.3), ('block/basalt/break2', 0.8, 1.4)],
    'entity.soul_golem.death': [('block/basalt/break3', 1.0, 0.9), ('block/amethyst_cluster/break1', 0.8, 1.2)],
    'entity.soul_golem.step': [('block/basalt/step1', 0.3, 1.6), ('block/basalt/step3', 0.3, 1.7)],
    'entity.soul_golem.dig': [('item/brush/brushing_sand1', 0.6, 1.2), ('item/brush/brushing_sand2', 0.6, 1.3)],
    'entity.soul_golem.find': [('block/amethyst/shimmer', 1.0, 1.4), ('mob/allay/item_given1', 0.8, 1.3)],
    'entity.soul_golem.slump': [('block/beacon/deactivate', 0.6, 1.6)],
    'entity.soul_golem.recharge': [('block/beacon/power2', 0.6, 1.8), ('block/amethyst/resonate2', 0.8, 1.5)],
    'entity.nib.ambient': [('block/amethyst/shimmer', 0.25, 1.9), ('mob/allay/idle_without_item2', 0.15, 1.9)],
    'entity.nib.hurt': [('block/amethyst_cluster/break2', 0.5, 1.9)],
    'entity.nib.transform': [('block/amethyst/resonate4', 1.0, 1.4), ('mob/allay/item_thrown1', 0.8, 1.5)],
    'block.echoer_device.charge': [('mob/warden/sonic_charge1', 0.7, 1.4), ('mob/warden/sonic_charge2', 0.7, 1.5)],
    'block.echoer_device.fire': [('mob/warden/sonic_boom1', 0.7, 1.6), ('mob/warden/sonic_boom2', 0.7, 1.7)],
    'block.echoer_device.fizzle': [('block/amethyst_cluster/break3', 0.8, 0.8)],
}
SUBTITLES = {
    'entity.soul_golem.ambient': 'Soul Golem hums', 'entity.soul_golem.hurt': 'Soul Golem chips', 'entity.soul_golem.death': 'Soul Golem crumbles',
    'entity.soul_golem.step': 'Soul Golem waddles', 'entity.soul_golem.dig': 'Soul Golem digs', 'entity.soul_golem.find': 'Soul Golem finds something',
    'entity.soul_golem.slump': 'Soul Golem runs down', 'entity.soul_golem.recharge': 'Soul Golem recharges',
    'entity.nib.ambient': 'Nib twinkles', 'entity.nib.hurt': 'Nib flickers', 'entity.nib.transform': 'Nib turns to treasure',
    'entity.enchoer.ambient': 'Echoer chimes', 'entity.enchoer.hum': 'Echoer hums', 'entity.enchoer.trade': 'Echoer waits, humming',
    'entity.enchoer.yes': 'Echoer accepts', 'entity.enchoer.no': 'Echoer sighs', 'entity.enchoer.hurt': 'Echoer hurts', 'entity.enchoer.death': 'Echoer fades',
    'block.echoer_device.charge': 'The Echoer charges', 'block.echoer_device.fire': 'The Echoer fires', 'block.echoer_device.fizzle': 'The Echoer fizzles',
}

NS = 'thesift'
