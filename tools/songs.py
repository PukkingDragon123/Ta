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
    'tide': ([3, 8, 10, 8, 3, 1], '#2b8fbb'),
    'lullaby': ([13, 11, 10, 8, 10, 6], '#c7a6f0'),
}
SONG_DESC = {
    'offering': 'Play it beside an offering and the Echoer will trade.',
    'nib': 'The Nibs of the Sound Garden dance to it.',
    'golem': 'Wakes and recharges Soul Golems.',
    'crystal': "The Caravans' hymn: it calms a colony.",
    'whale': 'A Sky Whale nearby will come and sing back.',
    'tide': 'Opens the drowned vaults of the deep and calms the Gobbler.',
    'lullaby': 'Puts nearby monsters to sleep and opens Harmony Seals.',
}

# Where the sheets are found: chest table -> [(song, weight)], plus how often a sheet turns up at all.
SHEET_LOOT = {
    'chests/sift_ruins': ([('offering', 3), ('nib', 3), ('lullaby', 2), ('golem', 1)], 0.35),
    'chests/tower_top': ([('whale', 3), ('crystal', 2), ('lullaby', 1)], 0.45),
    'chests/temple_vault': ([('golem', 2), ('lullaby', 2), ('crystal', 2), ('offering', 1)], 0.5),
    'chests/deep_shrine': ([('tide', 3), ('golem', 2), ('lullaby', 1)], 0.4),
    'chests/sculk_castle': ([('lullaby', 2), ('whale', 2), ('tide', 1)], 0.5),
    'chests/chrome_well': ([('tide', 3), ('nib', 1)], 0.3),
}

# The gem-inlaid instruments need thesift:prism_gem (added by the Caravans agent). Each recipe also
# carries a neoforge:registered condition, so it is skipped (not an error) while the gem is missing.
# Set ENABLE_PRISM_RECIPES = False to leave the files out entirely.
ENABLE_PRISM_RECIPES = True
PRISM_RECIPES = {
    'prism_flute': ([' G ', 'GFG', ' G '], {'G': 'prism_gem', 'F': 'crane_flute'}),
    'prism_drum': ([' G ', 'GDG', ' G '], {'G': 'prism_gem', 'D': 'conga_drum'}),
    'prism_harp': (['GIG', 'ISI', 'GIG'], {'G': 'prism_gem', 'I': 'minecraft:gold_ingot', 'S': 'minecraft:string'}),
}

INSTRUMENTS = ['crane_flute', 'conga_drum', 'guitar', 'prism_flute', 'prism_harp', 'prism_drum']
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
    for i in INSTRUMENTS:
        GA.tag('item', f'{NS}:instruments', rl(i))
    for i in OPTIONAL_INSTRUMENTS:
        GA.tag('item', f'{NS}:instruments', {'id': rl(i), 'required': False})
    recipe_dir = os.path.join(D, 'recipe')
    for name, (pattern, key) in PRISM_RECIPES.items():
        path = os.path.join(recipe_dir, name + '.json')
        if not ENABLE_PRISM_RECIPES:
            if os.path.exists(path):
                os.remove(path)
            continue
        GA.write(path, {
            'neoforge:conditions': [{'type': 'neoforge:registered', 'registry': 'minecraft:item', 'value': f'{NS}:prism_gem'}],
            'type': 'minecraft:crafting_shaped', 'category': 'equipment',
            'key': {k: rl(v) for k, v in key.items()}, 'pattern': pattern, 'result': {'count': 1, 'id': rl(name)}})
    lang()


def lang():
    L = {f'song.{NS}.{s}': t for s, t in spec.SONG_TITLES.items()}
    L.update({f'song.{NS}.{s}.desc': t for s, t in SONG_DESC.items()})
    L.update({
        f'item.{NS}.music_sheet.hint': 'Use to pin it on screen. Carry it and play these notes on any instrument.',
        f'message.{NS}.song.played': 'You played %s',
        f'codex.{NS}.songs.title': 'Songs & Music Sheets', f'codex.{NS}.songs.tagline': 'Seven songs, written down',
        f'codex.{NS}.songs.body': 'Every instrument plays single notes: look up for higher notes, down for lower - a ladder beside the crosshair shows the note. Sheets of music hide in the chests of the old ruins, towers, temples and shrines. Hold one to read its notes; use it to pin it on screen. Carry the sheet and play its notes in order, with no more than two seconds between them, to perform the song. The Lullaby puts monsters to sleep and opens Harmony Seals; the Whale Song calls a Sky Whale. Other songs wake golems, calm colonies, open drowned vaults and begin the Echoer\'s trade.',
        f'codex.{NS}.prism_instruments.title': 'Prism Instruments', f'codex.{NS}.prism_instruments.tagline': 'Gem-inlaid',
        f'codex.{NS}.prism_instruments.body': 'Inlay an instrument with prism gems and it sings stronger. The Prism Flute\'s beam reaches further, hits harder and pierces everything along its line. The Prism Drum\'s shockwave rolls half as far again and rings out sooner. The Prism Harp heals you, your friends and your tamed creatures a little with every note it plays.',
    })
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


def art():
    out = {f'music_sheet_{s}': _sheet(s) for s in SONGS}
    out['prism_flute'] = prism_flute()
    out['prism_drum'] = prism_drum()
    out['prism_harp'] = prism_harp()
    return out
