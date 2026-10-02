"""The vanilla 26.3 sounds that The Sift's sounds.json is allowed to point at.

gen_assets.check_sounds() refuses to write a sounds.json entry whose vanilla name is not listed
here: a sound file that does not exist (vanilla renames and renumbers its .ogg files between
versions) plays as silence, and a misspelled event plays nothing at all - neither shows up as an
error in game. Both lists come from the real 26.3 assets/minecraft/sounds.json.

To use a new vanilla sound, add it to the SOUNDS table in gen_assets.py and rebuild this file from
the vanilla assets (every name the table uses is checked against the real sounds.json first):

    VANILLA_ASSETS=/path/to/assets/minecraft python3 tools/vanilla_sounds.py
"""
import json
import os
import sys

# Vanilla sound events used with 'event:...' (and the note block events 'block/note_block/x' becomes).
EVENTS = frozenset({
    'block.amethyst_cluster.break', 'block.anvil.land', 'block.beacon.activate', 'block.note_block.basedrum', 'block.note_block.bass',
    'block.note_block.bell', 'block.note_block.bit', 'block.note_block.chime', 'block.note_block.didgeridoo', 'block.note_block.flute',
    'block.note_block.guitar', 'block.note_block.harp', 'block.note_block.hat', 'block.note_block.snare', 'block.note_block.trumpet',
    'block.note_block.trumpet_exposed', 'block.note_block.trumpet_weathered', 'block.portal.ambient', 'block.portal.travel',
    'block.portal.trigger', 'block.sand.break', 'block.sculk_catalyst.bloom', 'block.tripwire.click_on', 'entity.chicken.ambient',
    'entity.chicken.death', 'entity.chicken.hurt', 'entity.enderman.teleport', 'entity.evoker.prepare_summon',
    'entity.firework_rocket.twinkle', 'entity.generic.explode', 'entity.guardian.attack', 'entity.llama.spit', 'entity.parrot.ambient',
    'entity.parrot.fly', 'entity.parrot.imitate.phantom', 'entity.phantom.ambient', 'entity.phantom.death', 'entity.phantom.hurt',
    'entity.phantom.swoop', 'entity.player.attack.sweep', 'entity.ravager.ambient', 'entity.ravager.death', 'entity.ravager.roar',
    'entity.ravager.stunned', 'entity.sniffer.digging', 'entity.sniffer.happy', 'entity.sniffer.sniffing', 'entity.spider.ambient',
    'entity.spider.death', 'entity.spider.hurt', 'entity.spider.step', 'entity.turtle.ambient_land', 'entity.turtle.death',
    'entity.turtle.death_baby', 'entity.turtle.hurt', 'entity.turtle.hurt_baby', 'entity.turtle.shamble', 'entity.warden.ambient',
    'entity.warden.attack_impact', 'entity.warden.death', 'entity.warden.emerge', 'entity.warden.heartbeat', 'entity.warden.hurt',
    'entity.warden.listening', 'entity.warden.roar', 'entity.warden.sonic_boom', 'entity.warden.sonic_charge', 'entity.wither.spawn',
    'item.crossbow.loading_middle', 'item.crossbow.shoot', 'item.elytra.flying', 'item.shield.block', 'item.trident.riptide_1',
})

# Vanilla sound files (assets/minecraft/sounds/<name>.ogg) used directly.
FILES = frozenset({
    'ambient/cave/cave11', 'ambient/cave/cave13', 'ambient/cave/cave7', 'ambient/cave/cave9', 'block/amethyst/break1',
    'block/amethyst/resonate1', 'block/amethyst/resonate2', 'block/amethyst/resonate3', 'block/amethyst/shimmer',
    'block/amethyst_cluster/break1', 'block/amethyst_cluster/break2', 'block/beacon/activate', 'block/beacon/ambient',
    'block/beacon/deactivate', 'block/beacon/power1', 'block/bell/resonate', 'block/bubble_column/bubble1', 'block/bubble_column/bubble2',
    'block/bubble_column/bubble3', 'block/enchantment_table/enchant1', 'block/end_portal/endportal', 'block/pointed_dripstone/drip_lava1',
    'block/sculk/break1', 'block/sculk/spread1', 'block/sculk_sensor/sculk_clicking1', 'block/sculk_sensor/sculk_clicking2',
    'block/sculk_shrieker/shriek1', 'block/sculk_shrieker/shriek2', 'block/trial_spawner/ominous_activate', 'dig/gravel1', 'dig/stone1',
    'item/crossbow/loading_start', 'item/goat_horn/call0', 'item/goat_horn/call1', 'item/goat_horn/call2', 'item/goat_horn/call3',
    'item/goat_horn/call4', 'item/goat_horn/call5', 'item/goat_horn/call6', 'item/goat_horn/call7', 'liquid/splash', 'liquid/splash2',
    'liquid/swim3', 'liquid/water', 'mob/allay/death1', 'mob/allay/hurt1', 'mob/allay/idle_with_item1', 'mob/allay/idle_with_item2',
    'mob/allay/idle_without_item1', 'mob/allay/idle_without_item2', 'mob/allay/idle_without_item3', 'mob/allay/item_given1',
    'mob/allay/item_given2', 'mob/allay/item_taken1', 'mob/axolotl/idle_air1', 'mob/bat/death', 'mob/bat/hurt1', 'mob/bat/hurt2',
    'mob/bat/idle1', 'mob/bat/idle2', 'mob/camel/step1', 'mob/cow/hurt1', 'mob/cow/hurt3', 'mob/cow/say1', 'mob/cow/say2', 'mob/cow/say3',
    'mob/dolphin/blowhole1', 'mob/dolphin/blowhole2', 'mob/fox/bite1', 'mob/fox/bite2', 'mob/frog/death1', 'mob/frog/death2',
    'mob/frog/hurt1', 'mob/frog/hurt2', 'mob/frog/idle1', 'mob/frog/idle2', 'mob/frog/idle4', 'mob/frog/idle6', 'mob/frog/idle7',
    'mob/frog/tongue1', 'mob/frog/tongue2', 'mob/guardian/flop1', 'mob/guardian/flop2', 'mob/guardian/guardian_death',
    'mob/guardian/guardian_hit1', 'mob/guardian/guardian_hit2', 'mob/guardian/guardian_idle1', 'mob/guardian/guardian_idle3',
    'mob/happy_ghast/ambient3', 'mob/happy_ghast/death', 'mob/happy_ghast/hurt1', 'mob/parrot/death1', 'mob/parrot/death2',
    'mob/parrot/hurt1', 'mob/parrot/hurt2', 'mob/parrot/idle1', 'mob/parrot/idle2', 'mob/parrot/idle3', 'mob/phantom/flap1',
    'mob/phantom/flap2', 'mob/pufferfish/blow_out1', 'mob/pufferfish/blow_out2', 'mob/pufferfish/blow_up1', 'mob/pufferfish/blow_up2',
    'mob/pufferfish/death1', 'mob/pufferfish/death2', 'mob/pufferfish/flop1', 'mob/pufferfish/flop2', 'mob/pufferfish/flop3',
    'mob/pufferfish/flop4', 'mob/pufferfish/hurt1', 'mob/pufferfish/hurt2', 'mob/rabbit/hurt1', 'mob/rabbit/hurt2', 'mob/rabbit/hurt3',
    'mob/rabbit/idle1', 'mob/rabbit/idle2', 'mob/ravager/death1', 'mob/ravager/hurt1', 'mob/ravager/hurt3', 'mob/ravager/step1',
    'mob/ravager/step2', 'mob/ravager/step4', 'mob/ravager/stun1', 'mob/silverfish/hit1', 'mob/silverfish/kill', 'mob/silverfish/say1',
    'mob/silverfish/say2', 'mob/silverfish/step1', 'mob/silverfish/step2', 'mob/slime/attack1', 'mob/slime/big1', 'mob/slime/big2',
    'mob/slime/small1', 'mob/slime/small2', 'mob/slime/small3', 'mob/slime/small4', 'mob/slime/small5', 'mob/sniffer/happy1',
    'mob/sniffer/happy2', 'mob/sniffer/happy3', 'mob/sniffer/idle3', 'mob/sniffer/step1', 'mob/sniffer/step2', 'mob/squid/ambient1',
    'mob/squid/death1', 'mob/squid/hurt1', 'mob/turtle/baby/egg_hatched1', 'mob/turtle/egg/drop_egg1', 'mob/turtle/egg/drop_egg2',
    'mob/turtle/egg/egg_break1', 'mob/turtle/egg/egg_crack1', 'music/game/an_ordinary_day', 'music/game/ancestry',
    'music/game/comforting_memories', 'music/game/echo_in_the_wind', 'music/game/infinite_amethyst', 'music/game/left_to_bloom',
    'music/game/one_more_day', 'random/bow', 'random/drink', 'random/eat1', 'random/eat2', 'random/explode1', 'random/explode2',
    'random/levelup', 'random/pop',
})


def vanilla_names(sounds_json):
    """(events, files) defined by a vanilla sounds.json."""
    with open(sounds_json) as f:
        data = json.load(f)
    files = set()
    for event in data.values():
        for s in event.get('sounds', []):
            if isinstance(s, str):
                files.add(s)
            elif s.get('type', 'file') != 'event':
                files.add(s['name'])
    return set(data), files


def _block(names):
    lines, line = [], ''
    for n in sorted(names):
        item = repr(n) + ','
        if line and len(line) + 1 + len(item) > 136:
            lines.append(line)
            line = item
        else:
            line = (line + ' ' + item) if line else item
    if line:
        lines.append(line)
    return '\n    '.join(lines)


def _replace_set(src, name, names):
    start = src.index(name + ' = frozenset({\n') + len(name + ' = frozenset({\n')
    end = src.index('})\n', start)
    body = '    ' + _block(names) + '\n' if names else ''
    return src[:start] + body + src[end:]


def main():
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import gen_assets  # noqa: E402
    events, files = gen_assets.used_vanilla_sounds()
    known_events, known_files = vanilla_names(os.path.join(gen_assets.VA, 'sounds.json'))
    bad = sorted(f'event {e}' for e in events - known_events) + sorted(f'file {f}' for f in files - known_files)
    if bad:
        sys.exit('not in the vanilla sounds.json: ' + ', '.join(bad))
    path = os.path.abspath(__file__)
    with open(path) as f:
        src = f.read()
    src = _replace_set(src, 'EVENTS', events)
    src = _replace_set(src, 'FILES', files)
    with open(path, 'w') as f:
        f.write(src)
    print(f'vanilla_sounds.py: {len(events)} events, {len(files)} files')


if __name__ == '__main__':
    main()
