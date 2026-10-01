"""Turns the world written by the headless smoke server into a singleplayer save the client test can quick-play."""
import sys

import nbtlib

path = sys.argv[1]
f = nbtlib.load(path)
data = f['Data'] if 'Data' in f else f
data['confirmedExperimentalSettings'] = nbtlib.Byte(1)
data['allowCommands'] = nbtlib.Byte(1)
data['LevelName'] = nbtlib.String('The Sift smoke test')
f.save()
print('prepared', path, sorted(data.keys())[:40])
