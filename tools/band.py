"""M2 band: every creature has its own instrument and may join the player's band (Java:
com.thesift.music.band). This module only writes the text: the band panel, the instrument names
(band.thesift.instrument.<id>, see BandVoices.java) and the Codex page.

Hook (one line): gen_assets.generate."""

NS = 'thesift'

# instrument id (BandVoice.Builder.instrument) -> name on the band panel
INSTRUMENTS = {
    'pling_bells': 'Pling Bells',
    'piccolo': 'Piccolo',
    'wisp_bells': 'Wisp Bells',
    'kazoo': 'Kazoo',
    'thunder_drums': 'Thunder Drums',
    'snore_drone': 'Snore Drone',
    'echo_chimes': 'Echo Chimes',
    'banjo': 'Banjo',
    'tuba': 'Tuba',
    'whale_song': 'Whale Song',
    'soul_vibraphone': 'Soul Vibraphone',
    'snare_shell': 'Snare Shell',
    'crystal_xylophone': 'Crystal Xylophone',
    'jailers_bell': "Jailer's Bell",
    'sculk_clicks': 'Sculk Clicks',
    'fanfare_horn': 'Fanfare Horn',
    'deep_bass': 'Deep Bass',
}


def assets(GA):
    L = {f'band.{NS}.instrument.{k}': v for k, v in INSTRUMENTS.items()}
    L.update({
        f'band.{NS}.hud.title': 'Your Band',
        f'codex.{NS}.band.title': 'Your Band',
        f'codex.{NS}.band.tagline': 'Every creature has a voice',
        f'codex.{NS}.band.body': (
            "Every creature plays its own instrument. Finish a song near one that likes it and it may join your band: eager "
            "ones at once, shy ones the second time, tamed ones always. Monsters only answer back. Your band follows you, plays "
            "every note you play and makes your songs stronger. Walk off or fall silent and it drifts home."),
    })
    GA.LANG.update(L)
