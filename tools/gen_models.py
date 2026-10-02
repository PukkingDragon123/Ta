"""Generates mob model geometry (Java), textures and preview renders from tools/mobs.py."""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from modelkit import Pose, preview, render_textures  # noqa: E402
import mobs  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..')
JAVA = os.path.join(ROOT, 'src/main/java/com/thesift/client/model/ModelGeometry.java')
TEX = os.path.join(ROOT, 'src/main/resources/assets/thesift/textures/entity')
PREVIEW = os.path.join(ROOT, 'build/previews')

POSES = {
    'bulb': {'rest': Pose(), 'jump': Pose(body={'scale': (0.85, 1.3, 0.85)}, left_ear={'rot': (0.6, 0, 0)}, right_ear={'rot': (0.6, 0, 0)},
                                                 left_ear_tip={'rot': (0.5, 0, 0)}, right_ear_tip={'rot': (0.4, 0, 0)}),
             'land': Pose(body={'scale': (1.25, 0.7, 1.25)}, left_ear={'rot': (-0.5, 0, 0.3)}, right_ear={'rot': (-0.5, 0, -0.3)})},
    'slumbler': {'rest': Pose(), 'yawn': Pose(head={'rot': (-0.35, 0, 0)}, jaw={'rot': (0.9, 0, 0)})},
    'sifter': {'rest': Pose(), 'chomp': Pose(lid={'rot': (-0.9, 0, 0)}), 'walk': Pose(left_leg={'rot': (0.5, 0, 0)}, right_leg={'rot': (-0.5, 0, 0)})},
    'enchoer': {'rest': Pose(), 'ponder': Pose(right_arm={'rot': (-1.25, 0.45, 0)}, right_forearm={'rot': (-1.3, 0, 0)}, head={'rot': (0.15, 0, 0.12)}),
                'sing': Pose(left_arm={'rot': (0, 0, -1.1)}, right_arm={'rot': (0, 0, 1.1)}, left_forearm={'rot': (0, 0, -0.5)},
                             right_forearm={'rot': (0, 0, 0.5)}, head={'rot': (-0.45, 0, 0)})},
    'riveter': {'rest': Pose(),
                'hang': Pose(body={'rot': (0, 0, 3.14159), 'pos': (0, -30, 0)}, left_wing={'rot': (3.0, 0, 0.1)}, right_wing={'rot': (3.0, 0, -0.1)},
                             head={'rot': (-0.5, 0, 0)}),
                'scream': Pose(left_wing={'rot': (0, 0, -1.3)}, right_wing={'rot': (0, 0, 1.3)}, jaw={'rot': (0.8, 0, 0)}, head={'rot': (-0.2, 0, 0)},
                               **{f'{s}_claw_{i}': {'rot': (0, 0, (i - 1.5) * 0.25 * (1 if s == 'left' else -1))} for s in ('left', 'right') for i in range(4)})},
    'harmoner': {'rest': Pose(), 'fly': Pose(left_wing={'rot': (0, 0, -1.3)}, right_wing={'rot': (0, 0, 1.3)}, left_leg={'rot': (0.9, 0, 0)},
                                              right_leg={'rot': (0.9, 0, 0)}),
                 'sing': Pose(head={'rot': (-0.5, 0, 0)}, jaw={'rot': (0.6, 0, 0)}, crest={'rot': (0.4, 0, 0)}, plume_0={'rot': (0, 0, -0.3)},
                              plume_2={'rot': (0, 0, 0.3)})},
}

# the wild creatures (tools/mobs_wild.py)
POSES.update({
    'stomper': {'rest': Pose(), 'spray': Pose(head={'rot': (-0.35, 0, 0)}, trunk_0={'rot': (-1.6, 0, 0)}, trunk_1={'rot': (-0.4, 0, 0)},
                                              trunk_2={'rot': (-0.2, 0, 0)}, trunk_3={'rot': (0.1, 0, 0)}),
                'stomp': Pose(body={'rot': (-0.35, 0, 0)}, front_left_leg={'rot': (-0.6, 0, 0)}, front_right_leg={'rot': (-0.6, 0, 0)},
                              jaw={'rot': (0.5, 0, 0)}, throat={'scale': (1.3, 1.6, 1.3)})},
    'sky_whale': {'rest': Pose(), 'sing': Pose(jaw={'rot': (0.55, 0, 0)}, head={'rot': (-0.2, 0, 0)}, left_flipper={'rot': (0, 0, -0.6)},
                                               right_flipper={'rot': (0, 0, 0.6)}, tail1={'rot': (0.2, 0, 0)}, tail2={'rot': (0.25, 0, 0)})},
    'tubafish': {'rest': Pose(), 'puffed': Pose(body={'scale': (1.7, 1.7, 1.7)}, **{f'spike_{i}': {'scale': (1.6, 1.8, 1.6)} for i in range(13)})},
    'fanfare_eel': {'rest': Pose(), 'swim': Pose(**{f'segment_{i}': {'rot': (0, 0.35 * (1 if i % 2 else -1), 0)} for i in range(5)})},
})
WILD_SCALE = {'stomper': 4.5, 'sky_whale': 3.2, 'fanfare_eel': 9, 'kazoo_fish': 18, 'tubafish': 11}


PREVIEW_SCALE = {'enchoer': 7, 'slumbler': 7, 'dictator': 5, 'harmoner': 14, 'thumper': 4, 'whistler': 4, 'strummer': 4, 'thumpling': 12,
                 'whistling': 12, 'strumling': 12, 'conductor_mask': 10, **WILD_SCALE}
# the Sculk Parasite (tools/parasite.py)
# 'coil' is the wind-up of its lunge, as SculkParasiteModel poses it: reared up, folded into a zigzag, sting raised, jaws open
POSES['sculk_parasite'] = {'rest': Pose(), 'coil': Pose(
    head={'rot': (-0.45, 0, 0), 'pos': (0, -3, 1.5)}, segment_0={'rot': (-0.3, 0, 0)}, segment_2={'rot': (0.75, 0.25, 0)},
    **{f'segment_{i}': {'rot': (0, 0.5 if i % 2 == 0 else -0.5, 0)} for i in range(3, 7)}, **{f'spine_{i}': {'rot': (0.35, 0, 0)} for i in range(7)},
    left_mandible={'rot': (0, -0.45, 0)}, right_mandible={'rot': (0, 0.45, 0)}, tail={'rot': (0.45, 0, 0)}, stinger={'rot': (0.35, 0, 0)})}
PREVIEW_SCALE['sculk_parasite'] = 14
ONLY = [a for a in sys.argv[1:] if not a.startswith('-')]


def main(previews=True):
    methods = []
    for name, fn in mobs.ALL.items():
        m = fn()
        m.pack()
        methods.append(m.java_method(name))
        texs = render_textures(m)
        os.makedirs(os.path.join(TEX, name), exist_ok=True)
        glowing = any(g is not None for _, g in texs.values())
        for vname, (img, glow) in texs.items():
            img.save(os.path.join(TEX, name, f'{vname}.png'))
            # every face gets its own emissive layer (an empty one if the face has nothing glowing)
            if glowing:
                (glow if glow is not None else Image.new('RGBA', img.size, (0, 0, 0, 0))).save(os.path.join(TEX, name, f'{vname}_glow.png'))
        if previews and (not ONLY or name in ONLY):
            os.makedirs(PREVIEW, exist_ok=True)
            first = next(iter(texs.values()))[0]
            for pname, pose in POSES.get(name, {'rest': Pose()}).items():
                for lid in ('eyelids', 'left_eyelid', 'right_eyelid'):
                    pose.parts.setdefault(lid, {'visible': False})
                for yaw in (35, 150):
                    img = preview(m, first, pose, yaw=yaw, scale=PREVIEW_SCALE.get(name, 9))
                    img.save(os.path.join(PREVIEW, f'{name}_{pname}_{yaw}.png'))
    src = ['// GENERATED by tools/gen_models.py from tools/mobs.py - do not edit by hand.',
           'package com.thesift.client.model;', '',
           'import net.minecraft.client.model.geom.PartPose;',
           'import net.minecraft.client.model.geom.builders.CubeDeformation;',
           'import net.minecraft.client.model.geom.builders.CubeListBuilder;',
           'import net.minecraft.client.model.geom.builders.LayerDefinition;',
           'import net.minecraft.client.model.geom.builders.MeshDefinition;',
           'import net.minecraft.client.model.geom.builders.PartDefinition;', '',
           'public final class ModelGeometry {',
           '    private ModelGeometry() {', '    }', '']
    src.append('\n\n'.join(methods))
    src.append('}')
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, 'w') as f:
        f.write('\n'.join(src) + '\n')
    print('models ok')


if __name__ == '__main__':
    main('--no-preview' not in sys.argv)
