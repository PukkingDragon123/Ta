"""Songs & instruments (agent D): Music Sheet loot, the #thesift:instruments tag, the gem-inlaid
instrument recipes, lang and Codex text (generate(), called from gen_data.generate()) and their
16x16 item art (art(), merged into items16.all_items())."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_assets as GA  # noqa: E402
import spec  # noqa: E402

NS = 'thesift'
D = os.path.join(GA.RES, 'data', NS)

# Song ids, notes (pitch 0-24) and colours; the notes must match music/Song.java.
SONGS = {
    'offering': ([6, 10, 13, 18, 13, 10, 6], '#eebd4a'),
    'nib': ([18, 20, 22, 18, 15, 13], '#f29bd6'),
    'golem': ([6, 6, 13, 13, 11, 6], '#22c7c4'),
    'crystal': ([13, 17, 20, 17, 13, 8], '#b9b8ff'),
    'whale': ([8, 6, 3, 6, 8, 13], '#66d2f0'),
    'lullaby': ([13, 11, 10, 8, 10, 6], '#c7a6f0'),
    'aurora': ([6, 10, 13, 18, 15, 13], '#ff8ae0'),  # M1: a Prism song
    # F2 Band Table & songs: a drum song, the Dolphin's symphony, the two enchanting songs
    'heartbeat': ([1, 3, 1, 3, 10, 8, 6, 1], '#d0404a'),
    'dolphin': ([10, 15, 13, 18, 15, 10, 13, 18], '#4ab8e8'),
    'canon': ([6, 13, 11, 16, 13, 18, 16, 21], '#e8b040'),
    'requiem': ([18, 15, 13, 8, 10, 6, 8, 6], '#1f8a8a'),
}
# M1 instrument play: a drum song's rhythm (half-beats per note) and a Prism song's lights (0 rose, 1 amber, 2 cyan,
# 3 violet) - mirrors music/Song.java
SONG_BEATS = {'heartbeat': [1, 3, 1, 3, 2, 2, 2, 4]}  # F2: the Heartbeat (CLEAN: the Tide Song is gone)
SONG_LIGHTS = {'aurora': [0, 1, 2, 3, 2, 0]}
LIGHT_RGB = ['#ff5fa2', '#ffc341', '#3fe6e0', '#a67bff']
SONG_DESC = {
    'offering': 'Ring it on Wind Chimes while an Echoer holds your offering.',
    'nib': 'The Nibs of the Sound Garden dance to it.',
    'golem': 'Wakes and recharges Soul Golems.',
    'crystal': "The Caravans' hymn: it calms a colony.",
    'whale': 'A Sky Whale nearby will come and sing back.',
    'lullaby': 'Puts nearby monsters to sleep and lulls the Gobbler.',
    'aurora': 'Lights up the dark, outlines monsters and lends night eyes.',
    'heartbeat': 'The deep pulse under the Sift, drummed. At the Band Table it wakes Resonance and Crescendo in a weapon.',
    'dolphin': 'Dolphins dance to it, and the Clam Chests of the deep open for it.',
    'canon': 'Each phrase answers the last. At the Band Table it binds Reverb and Fortissimo into an instrument.',
    'requiem': "The Ancient Cities' lament. At the Band Table it binds Sculk Ward and Echo Strike.",
}

# Where the sheets are found: chest table -> [(song, weight)], plus how often a sheet turns up at all.
SHEET_LOOT = {
    'chests/sculk_castle': ([('lullaby', 2), ('whale', 2), ('aurora', 1), ('requiem', 2), ('canon', 1), ('heartbeat', 1)], 0.5),
    # W1: the old ruins are gone; their sheets are brushed out of buried relics (gen_data's archaeology tables), the
    # Sifter swallows some, the Gobbler some, and the Thumper's cannon towers keep a few
    'chests/drum_pit_armory': ([('whale', 2), ('golem', 2), ('crystal', 1), ('heartbeat', 3), ('canon', 1)], 0.3),
    'gameplay/frozen_crystal': ([('aurora', 1)], 0.25),  # M1: the Caravans' frozen treasure keeps the Aurora
}

# M1 instrument play: every instrument version, its recipes (the Prism ones from the upgraded versions) and
# its loot are in tools/instruments.py.

# C4 songs: what each song must be played on (mirrors Song.java; None = any instrument)
SONG_INSTRUMENT = {'offering': 'chimes', 'nib': 'strings', 'golem': None, 'crystal': 'chimes', 'whale': 'flute',
                   'lullaby': 'strings', 'aurora': 'prism', 'heartbeat': 'drum', 'dolphin': 'flute', 'canon': 'strings', 'requiem': 'chimes'}
INSTRUMENT_NAMES = {
    'any': 'any instrument', 'flute': 'a flute (Crane, Serbim or Prism Flute)', 'drum': 'a drum (Conga, Thunder or Prism Drum)',
    'strings': "strings (Guitar, Star Lute, Weaver's Guitar or Prism Harp)", 'chimes': 'chimes (Wind Chimes, Glass Bells or Prism Chimes)',
    'prism': 'a Prism instrument, each note in its light',
}
INSTRUMENT_SHORT = {'any': 'Any instrument', 'flute': 'Flute', 'drum': 'Drum', 'strings': 'Strings', 'chimes': 'Chimes', 'prism': 'Prism'}
# Wind Chimes: a stick crossbar, two strings, an iron and an amethyst tube
WIND_CHIMES_RECIPE = (['SSS', 'T T', 'IAI'], {'S': 'minecraft:stick', 'T': 'minecraft:string', 'I': 'minecraft:iron_ingot',
                                              'A': 'minecraft:amethyst_shard'})
OPTIONAL_INSTRUMENTS = ['weaver_guitar']  # another agent's; tagged as optional


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


def generate():
    for table, (entries, chance) in SHEET_LOOT.items():
        name = table.split('/')[-1]
        GA.write(os.path.join(D, 'loot_table', 'gameplay', f'music_sheets_{name}.json'), {
            'type': 'minecraft:chest', 'random_sequence': f'{NS}:gameplay/music_sheets_{name}',
            'pools': [{'rolls': 1, 'condition': {'type': 'minecraft:random_chance', 'chance': chance},
                       'entries': [{'type': 'minecraft:item', 'name': f'{NS}:music_sheet_{s}', 'weight': wt} for s, wt in entries]}]})
        GA.write(os.path.join(D, 'loot_modifiers', f'music_sheets_{name}.json'), {
            'type': 'neoforge:add_table',
            'condition': {'type': 'neoforge:loot_table_id', 'loot_table_id': f'{NS}:{table}'},
            'table': f'{NS}:gameplay/music_sheets_{name}'})
    for i in OPTIONAL_INSTRUMENTS:
        GA.tag('item', f'{NS}:instruments', {'id': rl(i), 'required': False})
    recipe_dir = os.path.join(D, 'recipe')
    __import__('instruments').generate()  # M1 instrument play: every version's recipe, loot, tag and text
    pattern, key = WIND_CHIMES_RECIPE
    GA.write(os.path.join(recipe_dir, 'wind_chimes.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'equipment',
        'key': {k: rl(v) for k, v in key.items()}, 'pattern': pattern, 'result': {'count': 1, 'id': rl('wind_chimes')}})
    lang()


def lang():
    L = {f'song.{NS}.{s}': t for s, t in spec.SONG_TITLES.items()}
    L.update({f'song.{NS}.{s}.desc': t for s, t in SONG_DESC.items()})
    L.update({
        # C4 songs: which instrument, which notes, where to look
        f'item.{NS}.music_sheet.hint': 'Carry it and use the instrument: its play screen writes the song out its own way. Use the sheet to pin it.',
        f'item.{NS}.music_sheet.instrument': 'Play on %s',
        f'item.{NS}.music_sheet.scale': 'Notes from F#3 to F#5.',
        f'item.{NS}.music_sheet.rhythm': 'Keep its rhythm: ♩ one beat, ♪ half a beat',
        f'item.{NS}.music_sheet.lights': 'Each note in its light: %s',
        f'message.{NS}.song.played': 'You played %s',
        f'message.{NS}.song.wrong_instrument': '%s must be played on %s',
        f'music.{NS}.aim.up': '%s° up', f'music.{NS}.aim.down': '%s° down', f'music.{NS}.aim.ahead': 'straight ahead',
        f'music.{NS}.guide.next': 'Next: %s, look %s',
        f'music.{NS}.guide.play': 'Use the instrument to play - next: %s',
        f'music.{NS}.guide.next_note': 'Next: %s',
        f'music.{NS}.guide.needs': 'Needs %s',
        f'music.{NS}.guide.done': 'Played!',
        f'item.{NS}.wind_chimes.desc': 'Use to play: strike the chimes as they swing past the mark.',
        f'codex.{NS}.songs.title': 'Songs & Music Sheets', f'codex.{NS}.songs.tagline': 'Eleven songs, eleven sheets',
        f'codex.{NS}.songs.body': 'Sheets lie in relics, castles, the Drum Pit, Ancient Cities, libraries and sunken treasure. Play a carried sheet\'s notes in order on the right instrument; one slip is forgiven. Chimes: Offering, Crystal, Requiem. Strings: Nibs, Lullaby, Canon. Flute: Whale, Dolphin. Drum, on the beat: Heartbeat. Prism: Aurora. Golem Hymn: any.',
        f'codex.{NS}.prism_instruments.title': 'Prism Instruments', f'codex.{NS}.prism_instruments.tagline': 'Notes of light',
        f'codex.{NS}.prism_instruments.body': 'Ring an upgraded instrument with prism gems: Star Lute to Prism Harp (a string for every note), Serbim Flute, Thunder Drums and Glass Bells to their Prism kin. Every note is played in a colour of light - keys 1-4 or the mouse wheel - and lights the air. Prism songs such as the Aurora ask for the lights too.',
    })
    L.update({f'instrument.{NS}.{k}': v for k, v in INSTRUMENT_NAMES.items()})
    L.update({f'instrument.{NS}.{k}.short': v for k, v in INSTRUMENT_SHORT.items()})
    L[f'music.{NS}.guide.sneak'] = 'Sneak + use plays a single note'
    GA.LANG.update(L)


# ============================================================================ item art

def _sheet(song):
    """A curled parchment page: four faint staff lines, the song's notes in note-block colours,
    a wax seal in the song's own colour."""
    import items16 as I
    notes, seal = SONGS[song]
    cells = [['.'] * 16 for _ in range(16)]
    for y in range(1, 15):
        for x in range(3, 13):
            cells[y][x] = 'p' if x == 12 or y == 14 else 'P'
    cells[1][12] = '.'
    cells[1][11] = 'c'
    cells[2][12] = 'c'
    cells[2][11] = 'p'
    for x in range(4, 8):
        cells[2][x] = 't' if x % 2 == 0 else 'P'
    for y in (5, 7, 9, 11):
        for x in range(4, 12):
            cells[y][x] = 'l'
    keys = 'ABCDEFGHIJKLMNOQRSTUVWXYZ'
    pal = {'P': ('#f6ecd0', '#6e4c2a'), 'p': ('#dcc89a', '#6e4c2a'), 'c': ('#c8ae7a', '#6e4c2a'), 'l': ('#c9b28a', '#6e4c2a'),
           't': ('#8a6a4a', '#6e4c2a'), 's': ('#4a3020', '#6e4c2a'), 'r': (seal, '#3a2018'), 'R': (I.mix(seal, '#ffffff', 0.45), '#3a2018')}
    for p in range(25):
        h = '#%06x' % _note_colour(p)
        pal[keys[p]] = (h, '#3a2018')
    for i, p in enumerate(notes):
        x = 4 + round(i * 7 / (len(notes) - 1))
        y = 12 - round(p / 24 * 9)
        cells[y][x] = keys[p]
        if y - 1 >= 3 and i % 2 == 0:
            cells[y - 1][x] = 's'
    cells[13][3] = 'r'
    cells[12][3] = 'R'
    cells[13][4] = 'r'
    # M1: a Prism song's lights glow along the foot of the page; a drum song's beats tick under its staff
    for i, light in enumerate(SONG_LIGHTS.get(song, [])):
        x = 5 + round(i * 6 / max(1, len(notes) - 1))
        cells[13][x] = '0123'[light]
    for i, beat in enumerate(SONG_BEATS.get(song, [])):
        x = 4 + round(i * 7 / (len(notes) - 1))
        if beat > 1:
            cells[12][x] = 't'
    for k, rgb in enumerate(LIGHT_RGB):
        pal['0123'[k]] = (rgb, '#3a2018')
    return I.grid([''.join(r) for r in cells], pal, ol=True)


def _note_colour(p):
    import math
    f = p / 24.0
    out = 0
    for off in (0.0, 1 / 3, 2 / 3):
        out = out << 8 | int(max(0.0, math.sin((f + off) * math.pi * 2) * 0.65 + 0.35) * 255)
    return out


PRISM = {'x': ('#ff8ae0', '#5a1a48'), 'y': ('#7ff5f0', '#0a4048'), 'z': ('#c4b8ff', '#2e2470')}


def prism_flute():
    """The crane flute in pale pearl, prism gems set in its finger holes and a prism-pink ribbon."""
    import items16 as I
    rows = [
        '................',
        '.............Mo.',
        '............LMD.',
        '...........LMD..',
        '..........xMD...',
        '.........LMD....',
        '........yMD.....',
        '.......LMD......',
        '......zMD.......',
        '.....LMD........',
        '....RrD.........',
        '...RrrD.........',
        '.Ww..Ww.........',
        '.Ww..Wk.........',
        '.Wk..k..........',
        '.k..............',
    ]
    pal = I.ramp('LMD', ['#ffffff', '#dfe2f6', '#a8acd8'], '#3a3a6a')
    pal.update(PRISM)
    pal.update({'o': ('#2a1a32', '#3a3a6a'), 'r': ('#d04ab8', '#3a1038'), 'R': ('#ff8ae0', '#3a1038'),
                'W': ('#ffffff', '#3a3a4a'), 'w': ('#c8ccd8', '#3a3a4a'), 'k': ('#2a2a36', '#101018')})
    return I.grid(rows, pal, ol=True)


def prism_drum():
    """The conga hooped in gold with a band of prism gems round its violet crystal-shell body."""
    import items16 as I
    rows = [
        '................',
        '....tTTTTTtt....',
        '...tTTTTTTttt...',
        '....sttttsss....',
        '...RRRRRRRRrr...',
        '...lGxGlyGzhl...',
        '...glGlglglhl...',
        '...gGlggglhhh...',
        '...RRRRRRRRrr...',
        '....GGbggbhh....',
        '....GyzGxbhh....',
        '.....bbbbbb.....',
        '.....Gbggbh.....',
        '.....Gbggbh.....',
        '.....RRRRrr.....',
        '................',
    ]
    pal = {'t': '#f0e0ff', 'T': '#ffffff', 's': '#c8b8e8', 'R': '#fde58f', 'r': '#c9862c', 'l': '#f6f0dc',
           'G': '#9a8ae6', 'g': '#6e5cc0', 'h': '#463a8a', 'b': '#3a2a6a'}
    pal = {k: (v, '#1a1430') for k, v in pal.items()}
    pal.update(PRISM)
    return I.grid(rows, pal, ol=True)


def prism_harp():
    """A small gold harp: a pillar on the left, a curling neck, a wooden soundbox on the diagonal,
    white strings between - and prism gems at its head, its pillar and its scroll."""
    import items16 as I
    cells = [['.'] * 16 for _ in range(16)]
    neck = {1: 1, 2: 1, 3: 2, 4: 2, 5: 2, 6: 3, 7: 3, 8: 3, 9: 4, 10: 4, 11: 5, 12: 5}
    for x, y in neck.items():
        cells[y][x] = 'G'
        if x > 2 and x < 12:
            cells[y + 1][x] = 'g' if cells[y + 1][x] == '.' else cells[y + 1][x]
    for y in range(2, 14):
        cells[y][1] = 'G'
        cells[y][2] = 'g'
    for x in range(3, 12):
        y = 6 + (11 - x)
        cells[y][x] = 'B'
        cells[y][x + 1] = 'b'
    for x in range(1, 4):
        cells[14][x] = 'G'
    for x in (4, 6, 8, 10):
        top = neck[x] + 2
        bottom = 6 + (11 - x)
        for y in range(top, bottom):
            cells[y][x] = 'w'
    cells[1][2] = 'x'
    cells[8][1] = 'y'
    cells[5][12] = 'z'
    pal = {'G': ('#eebd4a', '#4a2418'), 'g': ('#c9862c', '#4a2418'), 'B': ('#b18a52', '#2a1a16'), 'b': ('#6e4c2a', '#2a1a16'),
           'w': ('#f6f2e6', '#8d8070')}
    pal.update(PRISM)
    return I.grid([''.join(r) for r in cells], pal, ol=True, no_ol='w')


def wind_chimes():
    """C4 songs: Wind Chimes - a ring hook, a stick crossbar, four tuned tubes of moon-silver
    (long to short, each tipped with a cyan glint) and an amethyst clapper with a little sail."""
    import items16 as I
    cells = [['.'] * 16 for _ in range(16)]
    for x, y in ((7, 0), (8, 0), (6, 1), (9, 1), (7, 2), (8, 2)):
        cells[y][x] = 'k'
    cells[3][7] = 's'
    for x in range(1, 15):
        cells[4][x] = 'W'
        cells[5][x] = 'w'
    cells[4][1] = cells[4][14] = 'w'
    for x0, length in ((1, 6), (4, 8), (10, 7), (13, 5)):
        cells[6][x0] = 's'
        for y in range(7, 7 + length):
            cells[y][x0] = 'T'
            cells[y][x0 + 1] = 't'
        cells[7][x0 + 1] = 'd'
        cells[6 + length][x0] = 'g'
        cells[6 + length][x0 + 1] = 'G'
    for y in range(6, 10):
        cells[y][7] = 's'
    cells[10][7], cells[10][8], cells[11][7], cells[11][8] = 'C', 'c', 'c', 'e'
    cells[12][7] = 's'
    cells[13][7], cells[13][8], cells[14][7] = 'S', 'S', 'S'
    pal = {
        'k': ('#a8adbb', '#2a2a36'), 's': '#d8d0bc', 'W': ('#c49464', '#3a2414'), 'w': ('#8a5c36', '#3a2414'),
        'T': ('#f0f8ff', '#28364a'), 't': ('#a9c8dc', '#28364a'), 'd': ('#7f9cb6', '#28364a'),
        'g': ('#9ffaff', '#1a4a58'), 'G': ('#4fd8e8', '#1a4a58'),
        'C': ('#e6c8ff', '#3a1a5a'), 'c': ('#b48ae8', '#3a1a5a'), 'e': ('#7c52c0', '#3a1a5a'), 'S': ('#c8f6ff', '#2a5a6a'),
    }
    return I.grid([''.join(r) for r in cells], pal, ol=True, no_ol='s')


def art():
    out = {f'music_sheet_{s}': _sheet(s) for s in SONGS}
    out['prism_flute'] = prism_flute()
    out['prism_drum'] = prism_drum()
    out['prism_harp'] = prism_harp()
    out['wind_chimes'] = wind_chimes()  # C4 songs
    out.update(__import__('instruments').art())  # M1 instrument play: the upgraded versions and the Conga Drum's pair of drums
    return out
