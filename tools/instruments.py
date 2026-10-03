"""M1 instrument play: the instrument versions.

Every family is played its own way (music/Instrument.java, client/music/*Screen.java) and comes
in versions found or crafted further into the Sift - more notes, a new way to play, and at the top
the Prism versions, whose every note is a colour of light:

  strings: Guitar -> Star Lute (six strings, chords) -> Prism Harp (a string a note, lit)
  flute:   Crane Flute -> Serbim Flute (seventh hole, overblowing) -> Prism Flute (lit)
  drum:    Conga Drum -> Thunder Drums (eight pads, rolls) -> Prism Drum (lit)
  chimes:  Wind Chimes -> Glass Bells (ten chimes, the gust) -> Prism Chimes (lit)

declare() adds the new items to spec.py; generate() (from songs.generate()) writes their recipes,
chest loot and text; art() (from songs.art()) draws them.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))

NS = 'thesift'

# item id -> (music.Instrument constant, name, rarity, item model)
NEW = {
    'star_lute': ('LUTE', 'Star Lute', 'RARE', 'handheld'),
    'serbim_flute': ('SERBIM_FLUTE', 'Serbim Flute', 'RARE', 'handheld'),
    'thunder_drums': ('THUNDER_DRUMS', 'Thunder Drums', 'RARE', 'handheld'),
    'glass_bells': ('GLASS_BELLS', 'Glass Bells', 'RARE', 'generated'),
    'prism_chimes': ('PRISM_CHIMES', 'Prism Chimes', 'EPIC', 'generated'),
}

# the existing instruments, now plain InstrumentItems (spec.py uses these factories)
FACTORY = 'p -> new InstrumentItem(com.thesift.music.Instrument.{}, p)'

# every version, for the #thesift:instruments tag
ALL = ['guitar', 'star_lute', 'prism_harp', 'crane_flute', 'serbim_flute', 'prism_flute', 'conga_drum', 'thunder_drums', 'prism_drum',
       'wind_chimes', 'glass_bells', 'prism_chimes']

# recipes: the plain ones from common things, each upgrade from the one below and rare Sift materials
RECIPES = {
    'conga_drum': (['LSL', 'P P', 'PPP'], {'L': 'minecraft:leather', 'S': 'minecraft:string', 'P': '#minecraft:planks'}),
    'star_lute': ([' S ', 'TGT', 'ISI'], {'S': 'star_shard', 'T': 'sculk_string', 'G': 'guitar', 'I': 'serbim_ingot'}),
    'serbim_flute': ([' I ', 'IFI', ' E '], {'I': 'serbim_ingot', 'F': 'crane_flute', 'E': 'minecraft:echo_shard'}),
    'thunder_drums': (['HHH', 'IDI', 'WSW'], {'H': 'thick_hide', 'I': 'serbim_ingot', 'D': 'conga_drum', 'W': 'lullwood_planks',
                                              'S': 'star_shard'}),
    'glass_bells': (['ISI', 'GWG', 'G G'], {'I': 'serbim_ingot', 'S': 'star_shard', 'G': 'chime_glass', 'W': 'wind_chimes'}),
}
# the Prism versions: the upgraded instrument ringed with prism gems (skipped while the gem is missing)
PRISM_RECIPES = {
    'prism_flute': ([' G ', 'GFG', ' G '], {'G': 'prism_gem', 'F': 'serbim_flute'}),
    'prism_drum': ([' G ', 'GDG', ' G '], {'G': 'prism_gem', 'D': 'thunder_drums'}),
    'prism_harp': (['GIG', 'ILI', 'GIG'], {'G': 'prism_gem', 'I': 'minecraft:gold_ingot', 'L': 'star_lute'}),
    'prism_chimes': ([' G ', 'GBG', ' G '], {'G': 'prism_gem', 'B': 'glass_bells'}),
}

# where they are found: chest table -> ([(item, weight)], chance of one turning up at all)
LOOT = {
    'chests/sift_ruins': ([('guitar', 3), ('crane_flute', 2), ('wind_chimes', 3), ('conga_drum', 2)], 0.12),
    'chests/drum_pit_armory': ([('conga_drum', 3), ('thunder_drums', 2)], 0.3),
    'chests/tower_top': ([('star_lute', 3), ('glass_bells', 1)], 0.14),
    'chests/temple_vault': ([('serbim_flute', 3), ('glass_bells', 2), ('star_lute', 1)], 0.16),
    'chests/echoer_hut': ([('glass_bells', 3), ('wind_chimes', 2)], 0.18),
    'chests/deep_shrine': ([('serbim_flute', 2), ('thunder_drums', 1), ('prism_flute', 1)], 0.12),
    'chests/chrome_well': ([('glass_bells', 2), ('prism_chimes', 1)], 0.1),
    'chests/sculk_castle': ([('thunder_drums', 2), ('star_lute', 2), ('prism_harp', 1), ('prism_drum', 1), ('prism_flute', 1),
                             ('prism_chimes', 1)], 0.2),
}


def declare(block, item):
    """spec.py: the new versions (the older ones keep their own lines there)."""
    for iid, (const, name, rarity, model) in NEW.items():
        item(iid, cls='InstrumentItem', factory=FACTORY.format(const), props=f'new Item.Properties().stacksTo(1).rarity(Rarity.{rarity})',
             model=model, tab='combat', name=name)


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def _key(v):
    return '#' + rl(v[1:]) if v.startswith('#') else rl(v)


def generate():
    """Recipes, chest loot, the instruments tag and every line of text (from songs.generate())."""
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


# ============================================================================ text

def lang():
    L = {}
    p = f'instrument.{NS}'
    L.update({
        # how each family plays (tooltips)
        f'{p}.play.strings': 'Use to play: pick its strings on the fretboard',
        f'{p}.play.flute': 'Use to play: hold your breath, finger the holes',
        f'{p}.play.drum': 'Use to play: beat its pads in rhythm',
        f'{p}.play.chimes': 'Use to play: strike the chimes as they swing past the mark',
        f'{p}.range': '%s notes, %s to %s',
        f'{p}.mechanic.strings': 'Shift-pick strums a chord',
        f'{p}.mechanic.flute': 'Overblows an octave higher',
        f'{p}.mechanic.drum': 'A held pad rolls',
        f'{p}.mechanic.chimes': 'Blow a gust to swing them faster',
        f'{p}.mechanic.prism': 'Every note is a light: plays Prism songs',
        # the play screens
        f'{p}.controls.strings': 'Click a string at a fret · keys: hold Q W E R (fret), pick J K L ;',
        f'{p}.controls.strings.more': 'Click a string at a fret · hold Q W E R, pick H J K L ; \' · Shift: chord',
        f'{p}.controls.flute': 'Hold Space (or the mouse) to blow · cover the holes: A S D, J K L',
        f'{p}.controls.flute.more': 'Hold Space to blow · holes A S D F, J K L · Shift / right mouse: overblow',
        f'{p}.controls.drum': 'Strike S D F J K L (or click the heads) on the beat · Shift: accent',
        f'{p}.controls.drum.more': 'Strike A S D F J K L ; on the beat · hold a pad to roll · Shift: accent',
        f'{p}.controls.chimes': 'Strike a chime (A S D F G H J) as it swings past its mark',
        f'{p}.controls.chimes.more': 'Strike A S D F G H J K L ; as a chime passes its mark · hold Space: gust',
        f'{p}.controls.harp': 'Click a string, or sweep the mouse across them for a glissando',
        f'{p}.controls.with_light': '%s · 1-4 or the mouse wheel: the colour of light',
        f'{p}.controls.close': 'Esc: put it away',
        f'{p}.open': 'open',
        f'{p}.guide.no_sheet': 'Carry a Music Sheet and its song is written here',
        f'{p}.light': 'Light',
        f'{p}.chord': 'Shift: chord',
        f'{p}.gust': 'Space: gust',
        f'{p}.overblow': 'Shift: overblow',
        f'{p}.breath': 'Breath',
        f'{p}.judge.perfect': 'Perfect!',
        f'{p}.judge.good': 'Good',
        f'{p}.judge.early': 'Good - a little early',
        f'{p}.judge.late': 'Good - a little late',
        f'{p}.judge.off': 'Off the beat!',
        f'{p}.judge.lost': 'The rhythm is lost',
        f'{p}.judge.wait': 'Wait for the mark...',
        f'{p}.judge.breath': 'Out of breath - let go',
        f'music.{NS}.light.rose': 'Rose', f'music.{NS}.light.amber': 'Amber', f'music.{NS}.light.cyan': 'Cyan', f'music.{NS}.light.violet': 'Violet',
    })
    # Codex: the instruments page and one page per new version (each well under the 15 lines a page holds)
    c = f'codex.{NS}'
    L.update({
        f'{c}.instruments.title': 'Instruments', f'{c}.instruments.tagline': 'Four ways to play',
        f'{c}.instruments.body': 'Use an instrument to play it. Strings: pick notes on a fretboard. Flutes: hold your breath, finger the holes. Drums: beat the pads in rhythm. Chimes: strike each as it swings past its mark. Instruments have no powers - better ones play more notes or in new ways. Prism ones play every note as a light.',
        f'{c}.star_lute.title': 'Star Lute', f'{c}.star_lute.tagline': 'Six strings and a star',
        f'{c}.star_lute.body': 'A round-backed lute with a star-shard rosette. Six strings tuned a third apart reach every note of both octaves, and Shift (or the right mouse button) strums the whole chord on a note. Found high in Collapsed Towers and in temple vaults, or made from a Guitar with star shards, Serbim and Sculk String.',
        f'{c}.serbim_flute.title': 'Serbim Flute', f'{c}.serbim_flute.tagline': 'Silver breath',
        f'{c}.serbim_flute.body': 'A transverse flute of Serbim with a seventh hole and a longer breath. Hold Shift (or the right mouse button) while you blow to overblow it an octave higher - fifteen notes in all. Found in temple vaults and deep shrines, or made from a Crane Flute, Serbim and an Echo Shard.',
        f'{c}.thunder_drums.title': 'Thunder Drums', f'{c}.thunder_drums.tagline': 'Four drums, eight voices',
        f'{c}.thunder_drums.body': 'Four lacquered drums, a low and a high stroke on each: eight pads on A S D F J K L ;. Hold a pad and it rolls. The drum pits keep them, and the castle; or hoop a Conga Drum with Thick Hide, Serbim and lullwood. Drum songs keep a rhythm - land each beat on the line.',
        f'{c}.glass_bells.title': 'Glass Bells', f'{c}.glass_bells.tagline': 'Ten chimes of chime glass',
        f'{c}.glass_bells.body': 'Ten tubes of Chime Glass on a Serbim bar - two octaves. Hold Space and a gust sets them swinging wider and twice as fast: they pass their marks twice as often, but the moment to strike is shorter. Found in Echoer huts and temple vaults, or made from Wind Chimes, Chime Glass and Serbim.',
        f'{c}.prism_chimes.title': 'Prism Chimes', f'{c}.prism_chimes.tagline': 'Bells of light',
        f'{c}.prism_chimes.body': 'Glass Bells hung from a halo of prism gems. They play the Glass Bells\' way - and every chime rings in a colour of light: choose it with the keys 1-4 or the mouse wheel. Prism songs ask for their lights as well as their notes. Rarely found in Chrome Wells and the castle.',
        f'{c}.wind_chimes.title': 'Wind Chimes', f'{c}.wind_chimes.tagline': "The Echoer's voice",
        f'{c}.wind_chimes.body': 'Seven tuned tubes on a stick crossbar, swinging in the wind - the long low ones slowly, the short high ones fast. A chime only rings true when you strike it as it swings past its mark below. The Offering and the Crystal Hymn are rung on chimes.',
    })
    return L


# ============================================================================ art

def _grid(rows, pal, no_ol=''):
    import items16 as I
    return I.grid([(r + '.' * 16)[:16] for r in rows], pal, ol=True, no_ol=no_ol)


def star_lute():
    """A round-backed lute held like the guitar: a pear-shaped spruce top shaded from its lit edge,
    a dark binding, a star-shard rosette, a dark bridge, glinting strings up a walnut neck, and the
    pegbox bent sharply back with its white pegs - one star twinkling at its tip."""
    rows = [
        '..............w.',
        '............pkpW',
        '...........kKKkw',
        '..........NnPp..',
        '.........Nn.....',
        '.....bbbNn......',
        '...bbaasNcc.....',
        '..baaaasscc.....',
        '.baaasaYsscc....',
        '.baasYoYsscc....',
        'baasssaYsscc....',
        'baasssssBccc....',
        'baasssBBsccc....',
        '.bssBBssccc.....',
        '..bsscccccc.....',
        '...bcccc........',
    ]
    pal = {'a': ('#f8e2a8', '#4a2414'), 's': ('#e8c27a', '#4a2414'), 'b': ('#c88a4a', '#4a2414'), 'c': ('#a8682a', '#4a2414'),
           'Y': ('#fff0a0', '#6a3a14'), 'o': ('#3a1e14', '#24120c'), 'B': ('#5a3420', '#24120c'),
           'N': ('#8a5a32', '#24120c'), 'n': ('#5a3420', '#24120c'), 'k': ('#4a2a1a', '#24120c'), 'K': ('#6a4026', '#24120c'),
           'p': ('#f4f0e6', '#3a3a4a'), 'P': ('#fff6c8', '#24120c'), 'w': '#fff2a8', 'W': '#ffffff'}
    return _grid(rows, pal, no_ol='wW')


def serbim_flute():
    """A transverse flute of Serbim on the diagonal: a pale-blue silver tube lit along its top edge
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
    """Glass Bells: a Serbim bar on a ring hook and three bells of Chime Glass hung from it at
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
    return {'star_lute': star_lute(), 'serbim_flute': serbim_flute(), 'thunder_drums': thunder_drums(), 'glass_bells': glass_bells(),
            'prism_chimes': prism_chimes(), 'conga_drum': conga_drum()}
