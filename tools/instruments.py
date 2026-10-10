"""INS free play: the instrument versions.

There are no play screens. Hold use and an instrument is raised into its playing stance; while it is
raised every family plays the same way (music/Instrument.java, client/music/FreePlay.java): the
number keys 1-9 play the notes of the current register, the mouse wheel shifts the register (with
sneak: a Prism instrument's light), attack accents or strums - plus each family's flourish (sweep the
strings, a flute note that breathes on, a drum roll, chimes that swing and ring). Every family comes
in versions found or crafted further into the Sift - more notes, a new way to play, and at the top the
Prism versions, whose every note is a colour of light:

  strings: Guitar -> Prism Harp (a string a note, lit); the Weaver's Guitar (a boss's) strums chords
  flute:   Crane Flute -> Silver Flute (an octave more) -> Prism Flute (lit)
  drum:    Conga Drum -> Thunder Drums (eight pads, rolls) -> Prism Drum (lit)
  chimes:  Wind Chimes -> Glass Bells (ten chimes) -> Prism Chimes (lit)

Every basic instrument is crafted in the Overworld from vanilla things (planks, string, copper,
leather, bamboo, bone, amethyst); the upgrades keep their Sift materials. Every instrument is a real
3D model, its inventory icon included (tools/instrument_models.py, built with tools/instruments3d.py).

declare() adds the new items to spec.py; generate() (from songs.generate()) writes their recipes,
chest loot, text, 3D models and particles; art() (from songs.art()) draws the flat sprites the Music
Sheet art sketches; textures() (from gen_textures) paints the 3D models' textures and the particles.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

NS = 'thesift'

# item id -> (music.Instrument constant, name, rarity, item model)
NEW = {
    'serbim_flute': ('SERBIM_FLUTE', 'Silver Flute', 'RARE', 'handheld'),  # F1: the Serbim metal is retired (id kept); made of iron now
    'thunder_drums': ('THUNDER_DRUMS', 'Thunder Drums', 'RARE', 'handheld'),
    'glass_bells': ('GLASS_BELLS', 'Glass Bells', 'RARE', 'generated'),
    'prism_chimes': ('PRISM_CHIMES', 'Prism Chimes', 'EPIC', 'generated'),
}

# the existing instruments, now plain InstrumentItems (spec.py uses these factories)
FACTORY = 'p -> new SiftInstrumentItem(com.thesift.music.Instrument.{}, p)'

# every version, for the #thesift:instruments tag
ALL = ['guitar', 'prism_harp', 'crane_flute', 'serbim_flute', 'prism_flute', 'conga_drum', 'thunder_drums', 'prism_drum',
       'wind_chimes', 'glass_bells', 'prism_chimes']
# every instrument with a 3D model (the Weaver's Guitar is another agent's item, but plays here)
MODELS = ALL + ['weaver_guitar']

# recipes. The basic four are made in the Overworld from vanilla things (the Wind Chimes' recipe is
# songs.WIND_CHIMES_RECIPE); each upgrade from the one below and rare Sift materials.
RECIPES = {
    # a parlour guitar: planks for the body, string up the neck, a copper ingot for the frets and tuners
    'guitar': ([' SC', 'PS ', 'PP '], {'S': 'minecraft:string', 'C': 'minecraft:copper_ingot', 'P': '#minecraft:planks'}),
    # a side-blown cane flute: bamboo, crane-bone caps, an amethyst ring by the mouth hole
    'crane_flute': (['  A', ' M ', 'B  '], {'A': 'minecraft:amethyst_shard', 'M': 'minecraft:bamboo', 'B': 'minecraft:bone'}),
    # two staved hand drums: leather heads laced with string, a copper hoop, planks
    'conga_drum': (['LSL', 'P P', 'PCP'], {'L': 'minecraft:leather', 'S': 'minecraft:string', 'P': '#minecraft:planks',
                                           'C': 'minecraft:copper_ingot'}),
    'serbim_flute': ([' I ', 'IFI', ' E '], {'I': 'minecraft:iron_ingot', 'F': 'crane_flute', 'E': 'minecraft:echo_shard'}),
    'thunder_drums': (['HHH', 'IDI', 'WSW'], {'H': 'thick_hide', 'I': 'minecraft:iron_ingot', 'D': 'conga_drum', 'W': 'lullwood_planks',
                                              'S': 'star_shard'}),
    'glass_bells': (['ISI', 'GWG', 'G G'], {'I': 'minecraft:iron_ingot', 'S': 'star_shard', 'G': 'chime_glass', 'W': 'wind_chimes'}),
}
# the Prism versions: the upgraded instrument ringed with prism gems (skipped while the gem is missing)
PRISM_RECIPES = {
    'prism_flute': ([' G ', 'GFG', ' G '], {'G': 'prism_gem', 'F': 'serbim_flute'}),
    'prism_drum': ([' G ', 'GDG', ' G '], {'G': 'prism_gem', 'D': 'thunder_drums'}),
    'prism_harp': (['GIG', 'ILI', 'GIG'], {'G': 'prism_gem', 'I': 'minecraft:gold_ingot', 'L': 'guitar'}),  # MANSION: a Guitar ringed with prism gems
    'prism_chimes': ([' G ', 'GBG', ' G '], {'G': 'prism_gem', 'B': 'glass_bells'}),
}

# where they are found: chest table -> ([(item, weight)], chance of one turning up at all)
# (CLEAN: the ruins, towers, temples, Echoer huts, shrines and wells these also stocked were removed in W1)
LOOT = {
    'chests/drum_pit_armory': ([('conga_drum', 3), ('thunder_drums', 2)], 0.3),
    'chests/sculk_castle': ([('thunder_drums', 2), ('prism_harp', 1), ('prism_drum', 1), ('prism_flute', 1),
                             ('prism_chimes', 1)], 0.2),
}

# INS free play: the particles drawn when an instrument is played (registry/ModInstrumentFx)
PARTICLES = {'instrument_note': 'instrument_note', 'instrument_notes': 'instrument_notes', 'sound_ring': 'sound_ring',
             'instrument_breath': 'instrument_breath'}


def declare(block, item):
    """spec.py: the new versions (the older ones keep their own lines there)."""
    for iid, (const, name, rarity, model) in NEW.items():
        item(iid, cls='SiftInstrumentItem', factory=FACTORY.format(const), props=f'new Item.Properties().stacksTo(1).rarity(Rarity.{rarity})',
             model=model, tab='combat', name=name)


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def _key(v):
    return '#' + rl(v[1:]) if v.startswith('#') else rl(v)


def generate():
    """Recipes, chest loot, the instruments tag, every line of text, the 3D models and the particles (from songs.generate())."""
    import gen_assets as GA
    D = os.path.join(GA.RES, 'data', NS)
    recipe_dir = os.path.join(D, 'recipe')
    for name, (pattern, key) in RECIPES.items():
        GA.write(os.path.join(recipe_dir, name + '.json'), {
            'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'key': {k: _key(v) for k, v in key.items()},
            'pattern': pattern, 'result': {'count': 1, 'id': rl(name)}})
    for name, (pattern, key) in PRISM_RECIPES.items():
        GA.write(os.path.join(recipe_dir, name + '.json'), {
            'neoforge:conditions': [{'type': 'neoforge:registered', 'registry': 'minecraft:item', 'value': f'{NS}:prism_gem'}],
            'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'key': {k: _key(v) for k, v in key.items()},
            'pattern': pattern, 'result': {'count': 1, 'id': rl(name)}})
    for table, (entries, chance) in LOOT.items():
        name = table.split('/')[-1]
        GA.write(os.path.join(D, 'loot_table', 'gameplay', f'instruments_{name}.json'), {
            'type': 'minecraft:chest', 'random_sequence': f'{NS}:gameplay/instruments_{name}',
            'pools': [{'rolls': 1, 'condition': {'type': 'minecraft:random_chance', 'chance': chance},
                       'entries': [{'type': 'minecraft:item', 'name': rl(i), 'weight': w} for i, w in entries]}]})
        GA.write(os.path.join(D, 'loot_modifiers', f'instruments_{name}.json'), {
            'type': 'neoforge:add_table',
            'condition': {'type': 'neoforge:loot_table_id', 'loot_table_id': f'{NS}:{table}'},
            'table': f'{NS}:gameplay/instruments_{name}'})
    for i in ALL:
        GA.tag('item', f'{NS}:instruments', rl(i))
    GA.LANG.update(lang())
    models(GA)


def models(GA):
    """INS free play: every instrument's 3D models (rest, play, frames) and item definition; the particles' definitions."""
    import instrument_models as IM
    for iid in MODELS:
        b = IM.build(iid)
        inst = b['inst']
        rest = inst.model(inst.els, b['rest'])
        play_display = dict(b['rest'])
        play_display.update(b['play'])
        play = inst.model(inst.els, play_display)
        GA.note_textures(rest)
        mdir = os.path.join(GA.A, 'models', 'item')
        GA.write(os.path.join(mdir, iid + '.json'), rest)
        GA.write(os.path.join(mdir, iid + '_play.json'), play)
        entries = []
        for i, fname in enumerate(IM.FRAMES[b['fam']]):
            if fname not in b['frames']:
                continue
            GA.write(os.path.join(mdir, f'{iid}_play_{fname}.json'), inst.model(b['frames'][fname], play_display))
            entries.append({'threshold': float(i + 1), 'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{iid}_play_{fname}'}})
        rest_ref = {'type': 'minecraft:model', 'model': f'{NS}:item/{iid}'}
        play_ref = {'type': 'minecraft:model', 'model': f'{NS}:item/{iid}_play'}
        playing = {'type': 'minecraft:range_dispatch', 'property': f'{NS}:instrument_play', 'entries': entries, 'fallback': play_ref} \
            if entries else play_ref
        GA.write(os.path.join(GA.A, 'items', iid + '.json'), {'model': {
            'type': 'minecraft:select', 'property': 'minecraft:display_context',
            'cases': [{'when': ['gui', 'ground', 'fixed', 'on_shelf', 'head'], 'model': rest_ref}],
            'fallback': {'type': 'minecraft:condition', 'property': 'minecraft:using_item', 'on_true': playing, 'on_false': rest_ref}}})
    for pid, tex in PARTICLES.items():
        GA.write(os.path.join(GA.A, 'particles', pid + '.json'), {'textures': [f'{NS}:{tex}']})


def textures(out):
    """INS free play (from gen_textures): the 3D models' textures and the particles'."""
    import instrument_models as IM
    import instruments3d as K
    for iid in MODELS:
        out(f'{K.TEX_DIR}/{iid}', IM.build(iid)['inst'].paint())
    for name, img in K.particle_textures().items():
        out(f'particle/{name}', img)


# ============================================================================ text

def lang():
    L = {}
    p = f'instrument.{NS}'
    L.update({
        # how each family plays (tooltips)
        f'{p}.play.raise': 'Hold use to raise it and play',
        f'{p}.play.strings': '1-9: notes · wheel: register · hold attack and sweep: strum',
        f'{p}.play.flute': '1-9: notes · hold a key and the note breathes on · wheel: register',
        f'{p}.play.drum': '1-9: pads - low drum in the holding hand, high drum in the other',
        f'{p}.play.chimes': '1-9: chimes - struck, they swing and ring on',
        f'{p}.range': '%s notes, %s to %s',
        f'{p}.registers': '%s registers (mouse wheel)',
        f'{p}.mechanic.strings': 'Attack strums the whole chord',
        f'{p}.mechanic.flute': 'Overblows an octave higher (wheel up)',
        f'{p}.mechanic.drum': 'Hold a pad and it rolls',
        f'{p}.mechanic.chimes': 'Ten chimes over two registers',
        f'{p}.mechanic.prism': 'Every note is a light (sneak + wheel): plays Prism songs',
        f'music.{NS}.light.rose': 'Rose', f'music.{NS}.light.amber': 'Amber', f'music.{NS}.light.cyan': 'Cyan', f'music.{NS}.light.violet': 'Violet',
        f'music.{NS}.guide.key': 'Next: key %s (%s)',
    })
    # Codex: the instruments page and one page per version (each well under the 15 lines a page holds)
    c = f'codex.{NS}'
    L.update({
        f'{c}.instruments.title': 'Instruments', f'{c}.instruments.tagline': 'Played in the world',
        f'{c}.instruments.body': 'Hold use to raise an instrument and play it where you stand - everyone sees and hears you. The number keys 1-9 play its notes (the hotbar stays put), the mouse wheel shifts the register, attack accents. Sweep the mouse with attack held across strings; hold a flute note and it breathes on; chimes swing and ring. Every basic one is crafted in the Overworld.',
        f'{c}.guitar.title': 'Guitar', f'{c}.guitar.tagline': 'Spruce, mahogany and copper',
        f'{c}.guitar.body': 'A little parlour guitar: planks, string and a copper ingot. Raise it and it sits across your body; 1-9 pick the notes of the scale, the wheel moves up or down, and with attack held a sweep of the mouse runs across the strings. Four gut strings, nineteen notes.',
        f'{c}.crane_flute.title': 'Crane Flute', f'{c}.crane_flute.tagline': 'Bamboo, bone and amethyst',
        f'{c}.crane_flute.body': 'A side-blown cane flute with crane-bone caps and an amethyst ring by the mouth hole. Raise it to your lips; 1-7 play its seven notes. Hold a key and the note breathes on. Bamboo, a bone and an amethyst shard.',
        f'{c}.conga_drum.title': 'Conga Drum', f'{c}.conga_drum.tagline': 'Rhythm in two drums',
        f'{c}.conga_drum.body': 'Two hand drums on a strap: the low tumba under the hand that holds them, the high quinto under the other. 1-6 play the six strokes, attack accents. Drum songs keep a rhythm, and any instrument can play the Sift Symphony at a Sculk Summoner. Stompers love it. Leather, string, planks and copper.',
        f'{c}.weaver_guitar.title': "Weaver's Guitar", f'{c}.weaver_guitar.tagline': "The Weaver's own instrument",
        f'{c}.weaver_guitar.body': "The Weaver's own guitar, black chitin webbed with light - a boss's spoils, so it keeps a power. Raise it and its six strings reach both octaves over three registers, and attack strums the chord. Sneak and use it to weave: Musical Cobwebs spring up around you and every hostile creature near is snared in silk.",
        f'{c}.serbim_flute.title': 'Silver Flute', f'{c}.serbim_flute.tagline': 'Silver breath',
        f'{c}.serbim_flute.body': 'A concert flute of bright iron whose keys close as you finger lower notes. Turn the wheel up and it overblows an octave higher - fifteen notes in all. Made from a Crane Flute, Iron and an Echo Shard.',
        f'{c}.thunder_drums.title': 'Thunder Drums', f'{c}.thunder_drums.tagline': 'Four drums, eight voices',
        f'{c}.thunder_drums.body': 'Four lacquered war drums on an iron frame: eight strokes on 1-8, the big drums under the holding hand. Hold a key and it rolls. The drum pits keep them, and the castle; or hoop a Conga Drum with Thick Hide, Iron and lullwood.',
        f'{c}.glass_bells.title': 'Glass Bells', f'{c}.glass_bells.tagline': 'Ten chimes of chime glass',
        f'{c}.glass_bells.body': 'Ten tubes of Chime Glass on silk from an iron bar - two octaves over two registers. Struck, they swing and ring on and glitter in the light. Made from Wind Chimes, Chime Glass and Iron.',
        f'{c}.prism_chimes.title': 'Prism Chimes', f'{c}.prism_chimes.tagline': 'Bells of light',
        f'{c}.prism_chimes.body': 'Pearl tubes hung round a golden halo, each tipped with a facet of light. They play like the Glass Bells - and every chime rings in a colour of light: sneak and turn the wheel to choose it. Prism songs ask for their lights as well as their notes.',
        f'{c}.wind_chimes.title': 'Wind Chimes', f'{c}.wind_chimes.tagline': "The Echoer's voice",
        f'{c}.wind_chimes.body': 'Seven copper tubes on string from an oak crossbar, an amethyst striker and a wind-sail. Raise them by their ring; 1-7 strike them, and they swing and ring on. Sticks, string, copper and an amethyst shard. The Offering and the Crystal Hymn are rung on chimes.',
    })
    L[f'quest.{NS}.instrument.line'] = ("The Sift listens to music before it listens to anything else. Make yourself an instrument - "
                                        "a Guitar, a Crane Flute, a Conga Drum or Wind Chimes, all from things of your own world - "
                                        "then hold use to raise it and play. Better ones lie in old ruins.")
    return L


# ============================================================================ art

def _grid(rows, pal, no_ol=''):
    import items16 as I
    return I.grid([(r + '.' * 16)[:16] for r in rows], pal, ol=True, no_ol=no_ol)


def serbim_flute():
    """A transverse flute of bright iron on the diagonal: a pale-blue silver tube lit along its top edge
    and shaded under, two bright joint rings, key cups standing along its side, the lip plate and
    its dark embouchure hole near the head, a crown cap - and a cyan glint at the foot."""
    c = [['.'] * 16 for _ in range(16)]

    def put(x, y, ch):
        if 0 <= x < 16 and 0 <= y < 16:
            c[y][x] = ch

    for t in range(1, 15):
        put(t, 15 - t, 'L')
        put(t + 1, 15 - t, 'M')
        put(t + 1, 16 - t, 'D')
    for t in (5, 10):
        put(t, 15 - t, 'r')
        put(t + 1, 15 - t, 'R')
        put(t + 1, 16 - t, 'R')
    for t in (2, 3, 4, 7, 8, 9):
        # key pads on the tube, each with a silver lever beside it
        put(t + 1, 15 - t, 'K')
        put(t - 1, 15 - t, 'k')
    put(12, 2, 'P')
    put(11, 2, 'P')
    put(11, 3, 'P')
    put(12, 3, 'o')
    put(14, 0, 'C')
    put(15, 0, 'c')
    put(15, 1, 'c')
    put(0, 15, 'G')
    put(1, 15, 'g')
    pal = {'L': ('#e4fdff', '#173a6e'), 'M': ('#9aeefc', '#173a6e'), 'D': ('#3fb0da', '#173a6e'), 'r': ('#ffffff', '#173a6e'),
           'R': ('#c8dcf0', '#173a6e'), 'k': ('#e6e8f0', '#2a3a5a'), 'K': ('#2b6a94', '#0a1a30'), 'P': ('#f4f8ff', '#2a3a5a'), 'o': ('#173a6e', '#0a1a30'),
           'C': ('#ffffff', '#3a3a4a'), 'c': ('#a8adbb', '#3a3a4a'), 'g': ('#a8fff6', '#0a3038'), 'G': ('#3ff5e6', '#0a3038')}
    return _grid([''.join(r) for r in c], pal)


def thunder_drums():
    """Two lacquered war drums, a big one in front and a small one behind, their cream heads seen
    from above, iron hoops with bright studs, a gold lightning bolt on the big drum - and a
    drumstick resting across the small one."""
    rows = [
        '...........s....',
        '.......tTTt.S...',
        '......tTTTTtSs..',
        '......ttTTttRS..',
        '......iIiIiIrrS.',
        '..tTTTTTt.RRrr..',
        '.tTTTTTTTtRRrr..',
        'tTTTTTTTTTtRr...',
        'ttTTTTTTTttRr...',
        'iIiIiIiIiIir....',
        'RRRRRyyRRRRr....',
        'RRRRyyRRRRrr....',
        'RRRyyyyRRRrr....',
        'RRRRRyyRRRrr....',
        'iIiIiyIiIiIr....',
        '.rrrrrrrrrr.....',
    ]
    pal = {'t': ('#efe2c4', '#4a2a1a'), 'T': ('#fff4dc', '#4a2a1a'), 'R': ('#a8402e', '#2a0e0a'), 'r': ('#6a2218', '#2a0e0a'),
           'i': ('#5a6068', '#1a1a20'), 'I': ('#c8d0dc', '#1a1a20'), 'y': ('#ffd54a', '#6a4a10'), 's': ('#e8c890', '#3a2414'),
           'S': ('#a8784a', '#3a2414')}
    return _grid(rows, pal)


def glass_bells():
    """Glass Bells: an iron bar on a ring hook and three bells of Chime Glass hung from it at
    different heights - each lit cyan through, with a white glint and a dark clapper."""
    rows = [
        '.......kk.......',
        '......k..k......',
        '.......kk.......',
        '.WWWWWWWWWWWWWW.',
        '.wwwwwwwwwwwwww.',
        '..s......s...s..',
        '..s......s...s..',
        '.TTT.....s..TTT.',
        'TgTtt....s.TgTt.',
        'TgTtt...TTT.TTt.',
        'TTTtt..TgTtTTTtt',
        'TTTtt..TgTtt.c..',
        'TTTTtt.TTTtt....',
        '.TTTtt.TTTttt...',
        '..cc..TTTTTtt...',
        '........cc......',
    ]
    pal = {'k': ('#a8adbb', '#2a2a36'), 's': '#d8d0bc', 'W': ('#9aeefc', '#173a6e'), 'w': ('#2b8fbb', '#173a6e'),
           'T': ('#c8f6ff', '#1f5a6e'), 't': ('#6fc8e0', '#1f5a6e'), 'g': ('#ffffff', '#1f5a6e'), 'c': ('#3a2a5a', '#1a1030')}
    return _grid(rows, pal, no_ol='s')


def prism_chimes():
    """Prism Chimes, Seraph-made like the other Prism instruments: a golden halo with a watching
    eye, five pearl tubes hung from it - each tipped with a rainbow facet - and a gem clapper."""
    import gear_art as GAR
    rows = [
        '....gyyyyyyg....',
        '..gy_______yg...',
        '..gyywk__yyyg...',
        '...ggyyyyyygg...',
        '...s..s..s..s...',
        '..43.43.s.43.43.',
        '..43.43.s.43.43.',
        '..43.43.D.43.43.',
        '..43.43.d.43.43.',
        '..43.43...43.43.',
        '..43.43...43.43.',
        '..Aa.43...43.Ee.',
        '.....43...43....',
        '.....Bb...43....',
        '..........Cc....',
        '................',
    ]
    pal = dict(GAR.PRISM)
    return GAR._grid([(r + '.' * 16)[:16] for r in rows], pal)


def conga_drum():
    """The Conga Drum, made by hand now: a pair of tall congas - a low tumba and a slimmer quinto -
    in honey staves with dark seams, chrome hoops and lugs, and pale hide heads."""
    rows = [
        '.........tTTt...',
        '..tTTTt.tTTTTt..',
        '.tTTTTTtRRRRRr..',
        '.RRRRRRrLlLLLl..',
        '.LlLlLLlbLbLbl..',
        '.bLbLbLlbLbLbl..',
        '.bLbLbLlbLbLbl..',
        '.RRRRRRrbLbLbl..',
        '.bLbLbLlRRRRRr..',
        '.bLbLbLl.bLbl...',
        '..bLbLl..bLbl...',
        '..bLbLl..bLbl...',
        '..RRRRr..RRRr...',
        '..bLbLl..bLl....',
        '...bbb....bb....',
        '................',
    ]
    pal = {'t': ('#e8d6b0', '#4a2a1a'), 'T': ('#f8ecd0', '#4a2a1a'), 'R': ('#d8dae6', '#3a3a4a'), 'r': ('#8a90a8', '#3a3a4a'),
           'L': ('#e0a050', '#4a2414'), 'l': ('#a8682a', '#4a2414'), 'b': ('#8a5228', '#4a2414')}
    return _grid(rows, pal)


def art():
    return {'serbim_flute': serbim_flute(), 'thunder_drums': thunder_drums(), 'glass_bells': glass_bells(),
            'prism_chimes': prism_chimes(), 'conga_drum': conga_drum()}
