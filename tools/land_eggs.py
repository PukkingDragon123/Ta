"""S1 land: the spawn eggs of the remade land creatures (16 x 16, the vanilla per-mob egg: the egg shape
shaded in the creature's own colours, its face and features drawn on, outlined one shade darker).

Stomper, Sifter, Swifter, Harmoner, Mini Creator and Sky Whale. Drawn with items16.egg (the shared egg
shape, ramps and outline rules); hooked from items16.all_items (one line) and from the creatures' own
generators (tools/stomper.py, tools/sky_whale.py, tools/swifter_art.py, tools/mini_creator.py)."""


def _egg(tones, ol, over, pal, ring=None, no_ol='', under=None):
    import items16 as I
    for y, r in over.items():
        assert len(r) == 16, (y, r)
    return I.egg(tones, ol, over, pal, under=under, no_ol=no_ol, ring=ring)


def stomper():
    """The Stomper's head: mauve earth hide, a cap of pink coral turf with a flower and a teal tuft, big ears out to the
    sides with pink insides, small dark eyes under a lit fold (never cartoon eyes), bone tusks and the wrinkled trunk
    hanging down the front to a curl."""
    return _egg(['#5e3a4a', '#7a4e5e', '#8d5e6d', '#a87b88'], '#3c2234', {
        0: '.......tT.......',
        1: '......tTt.......',
        2: '......pPPp......',
        3: '.....pPfPPp.....',
        4: '....pPpPPpPp....',
        5: '.ee.........ee..',
        6: 'eiie.hh..hh.eiie',
        7: 'eiie.k....k..iie',
        8: '.eie.........ie.',
        9: '..e....mM.....e.',
        10: '.....b.mM.b.....',
        11: '.....B.mM.B.....',
        12: '.......mM.......',
        13: '.......mM.......',
        14: '......Mm........',
    }, {'t': ('#2a8c88', '#0e3a3a'), 'T': ('#7aeede', '#0e3a3a'), 'p': ('#d85868', '#5a1a2a'), 'P': ('#ff8a96', '#5a1a2a'),
        'f': '#fff4ec', 'e': ('#6c4656', '#2e1624'), 'i': ('#e48a9a', '#2e1624'), 'h': '#a87b88', 'k': '#1c0f18',
        'm': ('#6c4656', '#2e1624'), 'M': ('#9a7080', '#2e1624'), 'b': ('#fbf4e4', '#5a4a3a'), 'B': ('#d8ccb0', '#5a4a3a')},
        no_ol='hkf')


def sifter():
    """The Sifter: a bronze bell with its handle, two small amber eyes under bronze lids on its shoulder, a band of
    glowing gold runes, verdigris drips, a crust of lavender chime sand round the lip and teal feet."""
    return _egg(['#86552a', '#b67c3c', '#d9a65e', '#f2c88a'], '#3e2412', {
        0: '......hhhh......',
        1: '......h..h......',
        5: '.....dd..dd.....',
        6: '.....yk..ky.....',
        8: '...eReReReRe....',
        9: '...p......p.....',
        10: '...p...p..q.....',
        11: '.......q........',
        12: '....sSsgsSss....',
        13: '.....sSsSsg.....',
        14: '....ff....ff....',
        15: '....FF....FF....',
    }, {'h': ('#6a4228', '#2a170c'), 'y': '#c8902e', 'k': '#101830', 'd': '#6e4420', 'e': '#5c3a1c', 'R': '#ffd56c', 'p': '#5fae96',
        'q': '#93d6bf', 's': ('#d8d1ee', '#5a4a7a'), 'S': ('#efebf9', '#5a4a7a'), 'g': '#d070a8', 'f': ('#22a6c2', '#0a3a4a'),
        'F': ('#f0e6cc', '#5a4a3a')},
        no_ol='ykdeRpqg')


def swifter():
    """The Swifter: snow-white cloud fur, tall ears with ice-blue insides, small fox eyes (a dark pupil beside an ice-blue
    iris under a dark lid line), a dark nose, lilac shadows and a few Rainbow Snow glints."""
    return _egg(['#a4abe0', '#cfdcf6', '#eef3ff', '#ffffff'], '#4a5288', {
        0: '....e......e....',
        1: '...eie....eie...',
        2: '...eie....eie...',
        3: '....e......e....',
        6: '....ll....ll....',
        7: '....EP....PE....',
        9: '.......NN.......',
        10: '......m..m......',
        4: '.........y......',
        11: '....r......v....',
        12: '.......c........',
    }, {'e': ('#ffffff', '#4a5288'), 'i': ('#97bdf0', '#4a5288'), 'l': '#2a3a64', 'E': '#5a9ccc', 'P': '#14213f',
        'N': '#34466e', 'm': '#5b6e98', 'y': '#fff2b8', 'r': '#ffc6e6', 'v': '#d8ccff', 'c': '#c2f2ff'},
        no_ol='lEPNmyrvc')


def harmoner():
    """The Harmoner, parrot-like: a rose body with a crest of three flame-tipped feathers, small dark eyes in bare pale
    rings on the sides of the head, a hooked gold beak, a pale chest, wings folded at its sides with their flight
    feathers tipped, and a long tail."""
    return _egg(['#c4466e', '#ee6a8e', '#ff9ab4', '#ffd2dd'], '#6a1a3a', {
        0: '......o.o.......',
        1: '......yoy.......',
        2: '.......y........',
        4: '....rK....Kr....',
        6: '.......BB.......',
        7: '.......Bb.......',
        8: '..W....ll....W..',
        9: '..WW..llll..WW..',
        10: '..WW..llll..WW..',
        11: '..Wt..llll..tW..',
        12: '...t...TT...t...',
        13: '.......TT.......',
        14: '......TttT......',
    }, {'o': ('#ff8a3d', '#6a2a10'), 'y': ('#ffd86b', '#6a2a10'), 'K': '#120c14', 'r': '#e8ded8', 'B': '#ffc23f', 'b': '#a87818',
        'W': ('#c4466e', '#4a1028'), 't': ('#ffd86b', '#4a1028'), 'l': '#ffe0e8', 'T': ('#c4466e', '#4a1028')},
        no_ol='KrBbl')


def mini_creator():
    """The Mini Creator as a saint: olive-brown fur, a halo of gold light behind his head, small glowing eyes, the wide
    dark bill, prayer beads, the white robe with its gold V-neck and sash, crossed legs."""
    return _egg(['#5e4a32', '#7a6243', '#8f7652', '#a8916a'], '#3a2c1c', {
        0: '....hhhhhhh.....',
        1: '...h.......h....',
        2: '...h.......h....',
        5: '.....G...G......',
        7: '...BbbbbbbbbB...',
        8: '..BBBBBBBBBBBB..',
        9: '....dGdGdGd.....',
        10: '....wwgwwww.....',
        11: '....wwwgwww.....',
        12: '....gwwwgww.....',
        13: '...FFwwwwwFF....',
    }, {'h': ('#ffe27a', '#8a6418'), 'G': '#fff4b0', 'B': ('#3b3330', '#1a1410'), 'b': ('#5a4e48', '#1a1410'),
        'd': '#5a3a22', 'w': ('#f2ecdc', '#5a4a30'), 'g': ('#e0b23a', '#5a3a08'), 'F': ('#3f3530', '#1a1410')},
        no_ol='Gdg')


def sky_whale():
    """The Sky Whale: a sky-blue egg wrapped in a ring of cloud, a puff of cloud on top, six glowing eyes
    (three a side), soft glowing runes over its back, a veil-like fin and the pale grooved throat."""
    return _egg(['#5579c4', '#6f9ae0', '#8fb6f0', '#c8defa'], '#24305e', {
        1: '.....cc.........',
        2: '....cCCc........',
        3: '.....cc...R.....',
        4: '.........R.R....',
        5: '...r.....R......',
        6: '...kI......Ik...',
        7: '....kI....Ik....',
        8: '...kI......Ik...',
        9: '..fF.mmmmmm.Ff..',
        10: '.fF.bgbgbgbg.Ff.',
        11: '....bgbgbgbg....',
    }, {'c': ('#f3f7ff', '#5a6290'), 'C': ('#ffffff', '#5a6290'), 'R': '#aef8ff', 'r': '#ffe9a8', 'k': '#1a1c3a', 'I': '#7ff0ff',
        'm': '#3e2a52', 'b': '#f2f5ff', 'g': '#bcc8ec', 'f': ('#86aef0', '#24305e'), 'F': ('#cfe0ff', '#24305e')},
        no_ol='RrkImbg', ring='cloud')


def items():
    """items16.all_items hook: the land creatures' eggs (overriding older art; the Mini Creator's is written by
    tools/knowledge_art.py through mini_creator.spawn_egg())."""
    return {'stomper_spawn_egg': stomper(), 'sifter_spawn_egg': sifter(), 'swifter_spawn_egg': swifter(),
            'harmoner_spawn_egg': harmoner(), 'sky_whale_spawn_egg': sky_whale()}
