"""Generates block/item assets and data for The Sift from tools/spec.py.

Vanilla 26.3 JSON is used as a template wherever a block follows a vanilla shape (stairs, doors,
logs, leaves, cakes ...) so formats always match the game version exactly. Set VANILLA to the
extracted vanilla assets/data (see tools/README.md).
"""
import copy
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
import spec  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
RES = os.path.join(ROOT, 'src/main/resources')
VA = os.environ.get('VANILLA_ASSETS', '/home/user/ref/mc-26.3-assets-json/assets/minecraft')
VD = os.environ.get('VANILLA_DATA', '/home/user/ref/mc-26.3-data-json/data/minecraft')
NS = spec.MODID
A = os.path.join(RES, 'assets', NS)
D = os.path.join(RES, 'data', NS)
DMC = os.path.join(RES, 'data', 'minecraft')

WRITTEN = set()
TEXTURES = set()   # every thesift texture referenced (block/..., item/...)
LANG = {}
TAGS = {}          # (registry, namespace, path) -> list


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')
    WRITTEN.add(os.path.relpath(path, RES))


def vload(kind, name):
    with open(os.path.join(VA if kind != 'data' else VD, name)) as f:
        return json.load(f)


def tag(registry, name, value):
    ns, path = name.split(':') if ':' in name else (NS, name)
    TAGS.setdefault((registry, ns, path), [])
    if value not in TAGS[(registry, ns, path)]:
        TAGS[(registry, ns, path)].append(value)


def rl(x):
    return x if ':' in x else f'{NS}:{x}'


# --------------------------------------------------------------------------- template copying


def note_textures(model):
    for v in model.get('textures', {}).values():
        s = v['sprite'] if isinstance(v, dict) else v
        if isinstance(s, str) and s.startswith(NS + ':'):
            TEXTURES.add(s.split(':', 1)[1])


def remap_textures(model, texfn):
    tex = model.get('textures')
    if not tex:
        return model
    for k, v in list(tex.items()):
        s = v['sprite'] if isinstance(v, dict) else v
        if not isinstance(s, str) or s.startswith('#'):
            continue
        ns, path = s.split(':', 1) if ':' in s else ('minecraft', s)
        folder, name = path.split('/', 1)
        new = texfn(folder, name)
        if new:
            if isinstance(v, dict):
                v = dict(v)
                v['sprite'] = new
                tex[k] = v
            else:
                tex[k] = new
    return model


CURRENT = {'item': True}


def copy_template(tmpl, our, texfn, extra_models=None, item=None, model_rename=None):
    """Copy vanilla blockstate+models(+item definition) of `tmpl` as block `our`."""
    extra_models = extra_models or {}
    if item is None:
        item = CURRENT['item']
    copied = {}

    def remap_model_ref(ref):
        ns, path = ref.split(':', 1) if ':' in ref else ('minecraft', ref)
        folder, name = path.split('/', 1)
        if ref in extra_models:
            return extra_models[ref]
        if name.startswith(tmpl) or (model_rename and name in model_rename):
            new_name = model_rename[name] if model_rename and name in model_rename else our + name[len(tmpl):]
            key = f'{folder}/{name}'
            if key not in copied:
                m = copy.deepcopy(vload('assets', f'models/{folder}/{name}.json'))
                if 'parent' in m:
                    m['parent'] = remap_model_ref(m['parent'])
                remap_textures(m, texfn)
                note_textures(m)
                write(os.path.join(A, 'models', folder, new_name + '.json'), m)
                copied[key] = f'{NS}:{folder}/{new_name}'
            return copied[key]
        return ref

    def walk(o):
        if isinstance(o, dict):
            for k, v in o.items():
                if k == 'model' and isinstance(v, str):
                    o[k] = remap_model_ref(v)
                else:
                    walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)

    bs = copy.deepcopy(vload('assets', f'blockstates/{tmpl}.json'))
    walk(bs)
    write(os.path.join(A, 'blockstates', our + '.json'), bs)
    if item:
        it = copy.deepcopy(vload('assets', f'items/{tmpl}.json'))
        walk(it)
        write(os.path.join(A, 'items', our + '.json'), it)


def token_tex(token, replacement, folders=('block', 'item')):
    """texfn replacing `token` in vanilla texture names with `replacement`."""

    def fn(folder, name):
        if folder in folders and token in name:
            return f'{NS}:{folder}/{name.replace(token, replacement)}'
        return None

    return fn


def exact_tex(mapping):
    def fn(folder, name):
        key = f'{folder}/{name}'
        if key in mapping:
            return f'{NS}:{mapping[key]}'
        if name in mapping:
            return f'{NS}:{folder}/{mapping[name]}'
        return None

    return fn


# --------------------------------------------------------------------------- simple generated models


def simple_state(bid, model=None):
    write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'': {'model': f'{NS}:block/{model or bid}'}}})


def block_model(name, parent, textures, **extra):
    m = {'parent': parent, 'textures': {k: (v if isinstance(v, dict) else rl(v)) for k, v in textures.items()}}
    m.update(extra)
    note_textures(m)
    write(os.path.join(A, 'models/block', name + '.json'), m)


def item_block(bid, model=None):
    write(os.path.join(A, 'items', bid + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{model or bid}'}})


def item_generated(iid, texture=None, parent='minecraft:item/generated', extra_layers=None):
    tex = {'layer0': rl(texture or f'item/{iid}')}
    for i, t in enumerate(extra_layers or []):
        tex[f'layer{i + 1}'] = rl(t)
    m = {'parent': parent, 'textures': tex}
    note_textures(m)
    write(os.path.join(A, 'models/item', iid + '.json'), m)
    write(os.path.join(A, 'items', iid + '.json'), {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/{iid}'}})


def cube_all(bid, tex=None, item=True, translucent=False):
    t = f'block/{tex or bid}'
    block_model(bid, 'minecraft:block/cube_all', {'all': {'sprite': rl(t), 'force_translucent': True} if translucent else t})
    simple_state(bid)
    if item:
        item_block(bid)


# --------------------------------------------------------------------------- blocks


def gen_block(b):
    bid, kind = b['id'], b['kind']
    tex = b.get('tex', bid)
    model = b.get('model')
    has_item = b.get('item', True)
    CURRENT['item'] = has_item
    k = model if kind == 'custom' else kind
    if k in ('cube', 'cube_all', 'falling', 'ore'):
        cube_all(bid, tex, has_item)
    elif k == 'glass':
        cube_all(bid, tex, has_item, translucent=True)
    elif k == 'stairs':
        copy_template('andesite_stairs', bid, exact_tex({'andesite': tex}))
    elif k == 'slab':
        copy_template('andesite_slab', bid, exact_tex({'andesite': tex}), extra_models={'minecraft:block/andesite': f'{NS}:block/{b["basis"]}'})
    elif k == 'wall':
        copy_template('andesite_wall', bid, exact_tex({'andesite': tex}))
    elif k == 'pillar':
        if bid == 'hushslate':
            copy_template('deepslate', bid, token_tex('deepslate', 'hushslate'))
        else:
            copy_template('quartz_pillar', bid, token_tex('quartz_pillar', bid))
    elif k == 'sandstone':
        copy_template('sandstone', bid, token_tex('sandstone', 'dreamsandstone'))
    elif k == 'sandstone_cut':
        copy_template('cut_sandstone', bid, token_tex('sandstone', 'dreamsandstone'))
    elif k == 'sandstone_chiseled':
        copy_template('chiseled_sandstone', bid, token_tex('sandstone', 'dreamsandstone'))
    elif k == 'carpet':
        copy_template('moss_carpet', bid, exact_tex({'moss_block': tex}))
    elif kind in ('log', 'wood', 'fence', 'fence_gate', 'door', 'trapdoor', 'button', 'pressure_plate', 'leaves', 'sapling', 'pot') and b.get('wood'):
        w = b['wood']
        tmpl = bid.replace(w, 'cherry')
        copy_template(tmpl, bid, token_tex('cherry', w))
    elif kind == 'pot':
        plant = b['plant']
        copy_template('potted_poppy', bid, token_tex('poppy', plant))
    elif kind == 'flower':
        copy_template('poppy', bid, token_tex('poppy', bid))
    elif k == 'grass_block':
        block_model(bid, 'minecraft:block/cube_bottom_top', {'top': f'block/{bid}_top', 'side': f'block/{bid}_side', 'bottom': 'block/sift_soil'})
        block_model(bid + '_snow', 'minecraft:block/cube_bottom_top', {'top': 'minecraft:block/snow', 'side': 'minecraft:block/grass_block_snow',
                                                                      'bottom': 'block/sift_soil'})
        write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'snowy=false': [{'model': f'{NS}:block/{bid}'}, {'model': f'{NS}:block/{bid}', 'y': 90}, {'model': f'{NS}:block/{bid}', 'y': 180},
                            {'model': f'{NS}:block/{bid}', 'y': 270}],
            'snowy=true': {'model': f'{NS}:block/{bid}_snow'}}})
        item_block(bid)
    elif k == 'suspicious':
        copy_template('suspicious_sand', bid, token_tex('suspicious_sand', bid))
    elif k == 'hanging_leaves':
        copy_template('pale_hanging_moss', bid, token_tex('pale_hanging_moss', bid))
    elif k == 'cross':
        block_model(bid, 'minecraft:block/cross', {'cross': f'block/{tex}'})
        simple_state(bid)
        if has_item:
            item_generated(bid, f'block/{tex}')
    elif k == 'double_cross':
        copy_template('peony' if 'lily' in bid else 'tall_grass', bid, token_tex('peony' if 'lily' in bid else 'tall_grass', bid))
    elif k == 'flowerbed':
        copy_template('pink_petals', bid, token_tex('pink_petals', bid))
    elif k in ('crop4', 'crop3'):
        n = 4 if k == 'crop4' else 3
        for i in range(n):
            block_model(f'{bid}_stage{i}', 'minecraft:block/cross', {'cross': f'block/{bid}_stage{i}'})
        write(os.path.join(A, 'blockstates', bid + '.json'),
              {'variants': {f'age={i}': {'model': f'{NS}:block/{bid}_stage{min(i, n - 1)}'} for i in range(4)}})
    elif k == 'bush4':
        for i in range(4):
            block_model(f'{bid}_stage{i}', 'minecraft:block/cross', {'cross': f'block/{bid}_stage{i}'})
        write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {f'age={i}': {'model': f'{NS}:block/{bid}_stage{i}'} for i in range(4)}})
    elif k in ('glowbell', 'glowbell_plant'):
        block_model(bid, 'minecraft:block/cross', {'cross': f'block/{bid}'})
        block_model(bid + '_lit', 'minecraft:block/cross', {'cross': f'block/{bid}_lit'})
        write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'bell=false': {'model': f'{NS}:block/{bid}'}, 'bell=true': {'model': f'{NS}:block/{bid}_lit'}}})
        if has_item:
            item_generated(bid, 'item/glowbell_vine')
    elif k == 'cube_column':
        block_model(bid, 'minecraft:block/cube_column', {'end': f'block/{bid}_top', 'side': f'block/{bid}_side'})
        simple_state(bid)
        item_block(bid)
    elif k == 'portal':
        copy_template('nether_portal', bid, token_tex('nether_portal', bid), item=False)
    elif k == 'harmony':
        for i in range(4):
            block_model(f'{bid}_{i}', 'minecraft:block/cube_all', {'all': f'block/{bid}_{i}'})
        write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {f'tone={i}': {'model': f'{NS}:block/{bid}_{i}'} for i in range(4)}})
        item_block(bid, f'{bid}_0')
    elif k == 'glyph':
        for i in range(8):
            block_model(f'{bid}_{i}', 'minecraft:block/cube_column', {'end': 'block/polished_dreamstone', 'side': f'block/{bid}_{i}'})
        write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {f'glyph={i}': {'model': f'{NS}:block/{bid}_{i}'} for i in range(8)}})
        item_block(bid, f'{bid}_0')
    elif k == 'snare':
        block_model(bid, 'minecraft:block/carpet', {'wool': f'block/{bid}'})
        block_model(bid + '_spent', 'minecraft:block/carpet', {'wool': f'block/{bid}_spent'})
        write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {
            'spent=false': {'model': f'{NS}:block/{bid}'}, 'spent=true': {'model': f'{NS}:block/{bid}_spent'}}})
        item_generated(bid, f'block/{bid}')
    elif k == 'cake':
        copy_template('cake', bid, token_tex('cake', bid))
    elif k == 'lantern':
        copy_template('lantern', bid, exact_tex({'block/lantern': 'block/bulb_lantern', 'item/lantern': 'item/bulb_lantern'}))
    elif k == 'slime':
        copy_template('slime_block', bid, token_tex('slime_block', bid))
    elif k == 'drum':
        gen_drum(bid)
    elif k == 'altar':
        gen_altar(bid)
    elif k == 'chime':
        gen_chime(bid)
    elif k == 'none':
        write(os.path.join(A, 'models/block', bid + '.json'), {'textures': {'particle': f'{NS}:block/glow_particle'}})
        TEXTURES.add('block/glow_particle')
        simple_state(bid)
    elif k == 'liquid':
        write(os.path.join(A, 'models/block', bid + '.json'), {'textures': {'particle': f'{NS}:block/chrome_still'}})
        simple_state(bid)
    else:
        raise ValueError(f'no asset rule for {bid} ({kind}/{model})')
    LANG[f'block.{NS}.{bid}'] = b['name']


def el(frm, to, faces, **kw):
    e = {'from': frm, 'to': to, 'faces': faces}
    e.update(kw)
    return e


def faces(side, top, bottom, uv_side=None, uv_top=None):
    f = {}
    for d in ('north', 'south', 'east', 'west'):
        f[d] = {'texture': side, **({'uv': uv_side} if uv_side else {})}
    f['up'] = {'texture': top, **({'uv': uv_top} if uv_top else {})}
    f['down'] = {'texture': bottom}
    return f


def gen_drum(bid):
    variants = {}
    for beat in (False, True):
        for core in (False, True):
            name = bid + ('_beat' if beat else '') + ('_core' if core else '')
            top = f'{NS}:block/{bid}_top' + ('_beat' if beat else '')
            side = f'{NS}:block/{bid}_side' + ('_core' if core else '')
            h = 13 if beat else 14
            m = {'parent': 'minecraft:block/block', 'textures': {'particle': side, 'top': top, 'side': side, 'rim': f'{NS}:block/{bid}_rim',
                                                                    'bottom': f'{NS}:block/{bid}_bottom'},
                 'elements': [
                     el([0, 0, 0], [16, 2, 16], faces('#rim', '#rim', '#bottom', uv_side=[0, 14, 16, 16])),
                     el([1, 2, 1], [15, h - 1, 15], faces('#side', '#top', '#bottom', uv_side=[1, 16 - (h - 1), 15, 14])),
                     el([0, h - 2, 0], [16, h, 16], faces('#rim', '#top', '#rim', uv_side=[0, 0, 16, 2])),
                 ]}
            note_textures(m)
            write(os.path.join(A, 'models/block', name + '.json'), m)
            variants[f'beat={str(beat).lower()},core={str(core).lower()}'] = {'model': f'{NS}:block/{name}'}
    write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': variants})
    item_block(bid)


def gen_altar(bid):
    m = {'parent': 'minecraft:block/block', 'textures': {'particle': f'{NS}:block/{bid}_side', 'top': f'{NS}:block/{bid}_top',
                                                            'side': f'{NS}:block/{bid}_side', 'bottom': f'{NS}:block/{bid}_bottom',
                                                            'gem': f'{NS}:block/{bid}_gem'},
         'elements': [
             el([0, 0, 0], [16, 4, 16], faces('#side', '#bottom', '#bottom', uv_side=[0, 12, 16, 16])),
             el([3, 4, 3], [13, 9, 13], faces('#side', '#top', '#bottom', uv_side=[3, 7, 13, 12])),
             el([1, 9, 1], [15, 12, 15], faces('#side', '#top', '#bottom', uv_side=[1, 0, 15, 3])),
             el([6, 12, 6], [10, 13, 10], faces('#gem', '#gem', '#gem'), shade=False),
         ]}
    note_textures(m)
    write(os.path.join(A, 'models/block', bid + '.json'), m)
    simple_state(bid)
    item_block(bid)


def gen_chime(bid):
    tubes = [(3, 3, 9), (11, 4, 7), (6, 11, 10), (10, 11, 6), (7, 6, 12)]
    els = [el([2, 14, 2], [14, 16, 14], faces('#wood', '#wood', '#wood')),
           el([7.5, 16, 7.5], [8.5, 16, 8.5], faces('#wood', '#wood', '#wood'))]
    for (x, z, length) in tubes:
        els.append(el([x, 14 - length, z], [x + 2, 14, z + 2], faces('#tube', '#tube', '#tube', uv_side=[0, 0, 2, length])))
    els.append(el([7, 2, 7], [9, 6, 9], faces('#core', '#core', '#core'), shade=False))
    els.append(el([7.5, 6, 7.5], [8.5, 14, 8.5], faces('#wood', '#wood', '#wood', uv_side=[0, 0, 1, 8])))
    for powered in (False, True):
        name = bid + ('_lit' if powered else '')
        m = {'parent': 'minecraft:block/block', 'textures': {'particle': f'{NS}:block/{bid}_tube', 'wood': f'{NS}:block/{bid}_wood',
                                                                'tube': f'{NS}:block/{bid}_tube', 'core': f'{NS}:block/{bid}_core' + ('_lit' if powered else '')},
             'elements': els}
        note_textures(m)
        write(os.path.join(A, 'models/block', name + '.json'), m)
    write(os.path.join(A, 'blockstates', bid + '.json'), {'variants': {'powered=false': {'model': f'{NS}:block/{bid}'},
                                                                      'powered=true': {'model': f'{NS}:block/{bid}_lit'}}})
    item_generated(bid, 'item/soul_chime')


# --------------------------------------------------------------------------- loot


def loot(bid, tmpl, replace):
    d = copy.deepcopy(vload('data', f'loot_table/blocks/{tmpl}.json'))
    s = json.dumps(d)
    for a, b in replace.items():
        s = s.replace(f'"minecraft:{a}"', f'"{rl(b)}"')
    s = s.replace(f'"minecraft:blocks/{tmpl}"', f'"{NS}:blocks/{bid}"')
    write(os.path.join(D, 'loot_table/blocks', bid + '.json'), json.loads(s))


def gen_loot(b):
    bid = b['id']
    lk = b['loot']
    if lk == 'none':
        return
    if lk == 'self':
        loot(bid, 'dirt', {'dirt': bid})
    elif lk.startswith('drop:'):
        loot(bid, 'stone', {'stone': bid, 'cobblestone': lk[5:]})
    elif lk.startswith('silk:'):
        loot(bid, 'grass_block', {'grass_block': bid, 'dirt': lk[5:]})
    elif lk == 'silk':
        loot(bid, 'glass', {'glass': bid})
    elif lk == 'slab':
        loot(bid, 'andesite_slab', {'andesite_slab': bid})
    elif lk == 'door':
        loot(bid, 'cherry_door', {'cherry_door': bid})
    elif lk.startswith('leaves:'):
        loot(bid, 'cherry_leaves', {'cherry_leaves': bid, 'cherry_sapling': lk[7:]})
    elif lk.startswith('pot:'):
        loot(bid, 'potted_cherry_sapling', {'cherry_sapling': lk[4:]})
    elif lk in ('grass', 'shears'):
        loot(bid, 'vine', {'vine': bid})
    elif lk == 'double_grass':
        loot(bid, 'tall_grass', {'tall_grass': bid, 'short_grass': bid, 'wheat_seeds': 'blushgrass'})
    elif lk == 'petals':
        loot(bid, 'pink_petals', {'pink_petals': bid})
    elif lk == 'double_flower':
        loot(bid, 'peony', {'peony': bid})
    elif lk.startswith('crop:'):
        loot(bid, 'torchflower_crop', {'torchflower_seeds': lk[5:]})
    elif lk.startswith('bush:'):
        loot(bid, 'sweet_berry_bush', {'sweet_berry_bush': bid, 'sweet_berries': lk[5:]})
    elif lk.startswith('ore:'):
        loot(bid, 'iron_ore', {'iron_ore': bid, 'raw_iron': lk[4:]})
    elif lk == 'glowbell':
        d = copy.deepcopy(vload('data', 'loot_table/blocks/cave_vines.json'))
        s = json.dumps(d).replace('"minecraft:cave_vines"', f'"{NS}:{bid}"').replace('"minecraft:glow_berries"', f'"{NS}:glowbell_vine"')
        s = s.replace('"berries"', '"bell"').replace('"minecraft:blocks/cave_vines"', f'"{NS}:blocks/{bid}"')
        write(os.path.join(D, 'loot_table/blocks', bid + '.json'), json.loads(s))
    else:
        raise ValueError(f'loot {lk} for {bid}')


# --------------------------------------------------------------------------- tags

TAG_MAP = {
    'pickaxe': [('block', 'minecraft:mineable/pickaxe')],
    'axe': [('block', 'minecraft:mineable/axe')],
    'shovel': [('block', 'minecraft:mineable/shovel')],
    'hoe': [('block', 'minecraft:mineable/hoe')],
    'needs_iron': [('block', 'minecraft:needs_iron_tool')],
    'needs_diamond': [('block', 'minecraft:needs_diamond_tool')],
    'logs_that_burn': [('block', 'minecraft:logs_that_burn'), ('item', 'minecraft:logs_that_burn')],
    'planks': [('block', 'minecraft:planks'), ('item', 'minecraft:planks')],
    'wooden_stairs': [('block', 'minecraft:wooden_stairs'), ('item', 'minecraft:wooden_stairs')],
    'wooden_slabs': [('block', 'minecraft:wooden_slabs'), ('item', 'minecraft:wooden_slabs')],
    'wooden_fences': [('block', 'minecraft:wooden_fences'), ('item', 'minecraft:wooden_fences')],
    'fence_gates': [('block', 'minecraft:fence_gates'), ('item', 'minecraft:fence_gates')],
    'wooden_doors': [('block', 'minecraft:wooden_doors'), ('item', 'minecraft:wooden_doors')],
    'wooden_trapdoors': [('block', 'minecraft:wooden_trapdoors'), ('item', 'minecraft:wooden_trapdoors')],
    'wooden_buttons': [('block', 'minecraft:wooden_buttons'), ('item', 'minecraft:wooden_buttons')],
    'wooden_pressure_plates': [('block', 'minecraft:wooden_pressure_plates'), ('item', 'minecraft:wooden_pressure_plates')],
    'leaves': [('block', 'minecraft:leaves'), ('item', 'minecraft:leaves')],
    'saplings': [('block', 'minecraft:saplings'), ('item', 'minecraft:saplings')],
    'flowers': [('block', 'minecraft:flowers'), ('item', 'minecraft:flowers')],
    'small_flowers': [('block', 'minecraft:small_flowers'), ('item', 'minecraft:small_flowers')],
    'walls': [('block', 'minecraft:walls'), ('item', 'minecraft:walls')],
    'sand': [('block', 'minecraft:sand'), ('item', 'minecraft:sand')],
    'dirt': [('block', 'minecraft:dirt'), ('item', 'minecraft:dirt')],
    'glass': [],
    'replaceable_plants': [('block', 'minecraft:replaceable_by_trees')],
    'sword_efficient': [('block', 'minecraft:sword_efficient')],
    'beacon': [('block', 'minecraft:beacon_base_blocks')],
    'sift_stone': [('block', f'{NS}:sift_stone')],
    'serbim_ores': [('block', f'{NS}:serbim_ores'), ('item', f'{NS}:serbim_ores')],
    'portal_frame': [('block', f'{NS}:portal_frame')],
}


def gen_block_tags(b):
    bid = rl(b['id'])
    for t in b['tags']:
        if t.endswith('_logs') and t not in TAG_MAP:
            tag('block', f'{NS}:{t}', bid)
            tag('item', f'{NS}:{t}', bid)
            tag('block', 'minecraft:logs', f'#{NS}:{t}')
            tag('item', 'minecraft:logs', f'#{NS}:{t}')
            tag('block', 'minecraft:overworld_natural_logs', bid) if False else None
            continue
        for reg, name in TAG_MAP[t]:
            if reg == 'item' and not b.get('item', True):
                continue
            tag(reg, name, bid)
    kind = b['kind']
    if kind == 'stairs':
        tag('block', 'minecraft:stairs', bid)
        tag('item', 'minecraft:stairs', bid)
    if kind == 'slab':
        tag('block', 'minecraft:slabs', bid)
        tag('item', 'minecraft:slabs', bid)
    if kind == 'pot':
        tag('block', 'minecraft:flower_pots', bid)


# --------------------------------------------------------------------------- items


def gen_item(i):
    iid = i['id']
    model = i.get('model', 'generated')
    LANG[f'item.{NS}.{iid}'] = i['name']
    cls = i.get('cls', '')
    if cls.startswith('BlockItem:'):
        item_generated(iid)
        return
    if model == 'handheld':
        item_generated(iid, parent='minecraft:item/handheld')
    elif model == 'spear':
        # copy vanilla copper spear item definition (in-hand + throwing models)
        it = copy.deepcopy(vload('assets', 'items/copper_spear.json'))
        s = json.dumps(it)
        for ref in sorted(set(re.findall(r'"minecraft:item/(copper_spear[a-z_]*)"', s)), key=len, reverse=True):
            new = ref.replace('copper_spear', iid)
            m = copy.deepcopy(vload('assets', f'models/item/{ref}.json'))
            remap_textures(m, token_tex('copper_spear', iid, folders=('item',)))
            note_textures(m)
            write(os.path.join(A, 'models/item', new + '.json'), m)
            s = s.replace(f'"minecraft:item/{ref}"', f'"{NS}:item/{new}"')
        write(os.path.join(A, 'items', iid + '.json'), json.loads(s))
    elif model == 'slingshot':
        # bow-like pulling item definition
        it = copy.deepcopy(vload('assets', 'items/bow.json'))
        s = json.dumps(it)
        for ref in sorted(set(re.findall(r'"minecraft:item/(bow[a-z_0-9]*)"', s)), key=len, reverse=True):
            new = ref.replace('bow', 'slingshot')
            m = copy.deepcopy(vload('assets', f'models/item/{ref}.json'))
            if 'parent' in m and m['parent'] == 'minecraft:item/bow':
                m['parent'] = f'{NS}:item/slingshot'
            remap_textures(m, token_tex('bow', 'slingshot', folders=('item',)))
            note_textures(m)
            write(os.path.join(A, 'models/item', new + '.json'), m)
            s = s.replace(f'"minecraft:item/{ref}"', f'"{NS}:item/{new}"')
        write(os.path.join(A, 'items', iid + '.json'), json.loads(s))
    elif model == 'armor':
        item_generated(iid)
    else:
        item_generated(iid)


# --------------------------------------------------------------------------- recipes


VALID_CATEGORIES = {'building', 'redstone', 'equipment', 'misc'}


def shaped(name, pattern, key, result, count=1, category='building', group=None):
    category = category if category in VALID_CATEGORIES else 'misc'
    r = {'type': 'minecraft:crafting_shaped', 'category': category, 'key': {k: rl(v) if not v.startswith('#') else '#' + rl(v[1:]) for k, v in key.items()},
         'pattern': pattern, 'result': {'count': count, 'id': rl(result)}}
    if group:
        r['group'] = group
    write(os.path.join(D, 'recipe', name + '.json'), r)


def shapeless(name, ingredients, result, count=1, category='misc', group=None):
    category = category if category in VALID_CATEGORIES else 'misc'
    r = {'type': 'minecraft:crafting_shapeless', 'category': category,
         'ingredients': [rl(v) if not v.startswith('#') else '#' + rl(v[1:]) for v in ingredients], 'result': {'count': count, 'id': rl(result)}}
    if group:
        r['group'] = group
    write(os.path.join(D, 'recipe', name + '.json'), r)


def cutting(src, result, count=1):
    write(os.path.join(D, 'recipe', f'{result}_from_{src}_stonecutting.json'),
          {'type': 'minecraft:stonecutting', 'ingredient': rl(src), 'result': {'count': count, 'id': rl(result)}})


def smelt(name, src, result, xp=0.1, time=200, kinds=('smelting',)):
    for k in kinds:
        t = time if k == 'smelting' else time // 2
        write(os.path.join(D, 'recipe', f'{name}_from_{k}.json'),
              {'type': f'minecraft:{k}', 'cookingtime': t, 'experience': xp, 'ingredient': rl(src) if not src.startswith('#') else '#' + rl(src[1:]),
               'result': {'id': rl(result)}})


def gen_recipes():
    by_id = {b['id']: b for b in spec.BLOCKS}
    # stone families: stairs/slabs/walls + stonecutting from their basis
    for b in spec.BLOCKS:
        if b['kind'] in ('stairs', 'slab', 'wall') and not b.get('wood'):
            base = b['basis']
            if b['kind'] == 'stairs':
                shaped(b['id'], ['#  ', '## ', '###'], {'#': base}, b['id'], 4)
                cutting(base, b['id'])
            elif b['kind'] == 'slab':
                shaped(b['id'], ['###'], {'#': base}, b['id'], 6)
                cutting(base, b['id'], 2)
            else:
                shaped(b['id'], ['###', '###'], {'#': base}, b['id'], 6, category='misc')
                cutting(base, b['id'])
    fam = [('cobbled_dreamstone', 'dreamstone', 'smelt'), ('dreamstone', 'polished_dreamstone', 'square'),
           ('polished_dreamstone', 'dreamstone_bricks', 'square'), ('dreamstone_bricks', 'dreamstone_tiles', 'square'),
           ('dreamstone_bricks', 'cracked_dreamstone_bricks', 'smelt'), ('dreamstone_bricks', 'chiseled_dreamstone', 'chisel'),
           ('dreamstone', 'dreamstone_pillar', 'pillar'),
           ('cobbled_hushslate', 'polished_hushslate', 'square'), ('polished_hushslate', 'hushslate_bricks', 'square'),
           ('hushslate_bricks', 'hushslate_tiles', 'square'), ('hushslate_bricks', 'cracked_hushslate_bricks', 'smelt'),
           ('hushslate_bricks', 'chiseled_hushslate', 'chisel'),
           ('dreamsand', 'dreamsandstone', 'square'), ('dreamsandstone', 'cut_dreamsandstone', 'square'),
           ('dreamsandstone', 'smooth_dreamsandstone', 'smelt'), ('dreamsandstone', 'chiseled_dreamsandstone', 'chisel'),
           ('blush_bricks', 'cracked_blush_bricks', 'smelt'), ('blush_bricks', 'chiseled_blush_bricks', 'chisel')]
    for src, dst, how in fam:
        if how == 'square':
            shaped(dst, ['##', '##'], {'#': src}, dst, 4)
            cutting(src, dst)
        elif how == 'smelt':
            smelt(dst, src, dst, 0.1)
        elif how == 'chisel':
            slab = next((b['id'] for b in spec.BLOCKS if b['kind'] == 'slab' and b.get('basis') == src), None)
            if slab:
                shaped(dst, ['#', '#'], {'#': slab}, dst)
            cutting(src, dst)
        elif how == 'pillar':
            shaped(dst, ['#', '#'], {'#': src}, dst, 2)
            cutting(src, dst)
    shaped('mossy_dreamstone_bricks', ['#M'], {'#': 'dreamstone_bricks', 'M': 'lumen_moss_block'}, 'mossy_dreamstone_bricks')
    shaped('blush_bricks', ['#S', 'S#'], {'#': 'dreamsand', 'S': 'minecraft:brick'}, 'blush_bricks', 4)
    shaped('lumen_moss_carpet', ['##'], {'#': 'lumen_moss_block'}, 'lumen_moss_carpet', 3, category='misc')
    # woods
    for w in spec.WOODS:
        shapeless(f'{w}_planks', [f'#{w}_logs'], f'{w}_planks', 4, 'building', 'planks')
        shaped(f'{w}_wood', ['##', '##'], {'#': f'{w}_log'}, f'{w}_wood', 3, group='bark')
        shaped(f'stripped_{w}_wood', ['##', '##'], {'#': f'stripped_{w}_log'}, f'stripped_{w}_wood', 3, group='bark')
        p = f'{w}_planks'
        shaped(f'{w}_stairs', ['#  ', '## ', '###'], {'#': p}, f'{w}_stairs', 4, group='wooden_stairs')
        shaped(f'{w}_slab', ['###'], {'#': p}, f'{w}_slab', 6, group='wooden_slab')
        shaped(f'{w}_fence', ['W#W', 'W#W'], {'W': p, '#': 'minecraft:stick'}, f'{w}_fence', 3, 'misc', 'wooden_fence')
        shaped(f'{w}_fence_gate', ['#W#', '#W#'], {'W': p, '#': 'minecraft:stick'}, f'{w}_fence_gate', 1, 'redstone', 'wooden_fence_gate')
        shaped(f'{w}_door', ['##', '##', '##'], {'#': p}, f'{w}_door', 3, 'redstone', 'wooden_door')
        shaped(f'{w}_trapdoor', ['###', '###'], {'#': p}, f'{w}_trapdoor', 2, 'redstone', 'wooden_trapdoor')
        shapeless(f'{w}_button', [p], f'{w}_button', 1, 'redstone', 'wooden_button')
        shaped(f'{w}_pressure_plate', ['##'], {'#': p}, f'{w}_pressure_plate', 1, 'redstone', 'wooden_pressure_plate')
    # metals
    smelt('serbim_ingot', 'raw_serbim', 'serbim_ingot', 0.9, 200, ('smelting', 'blasting'))
    smelt('serbim_ingot_ore', f'#serbim_ores', 'serbim_ingot', 0.9, 200, ('smelting', 'blasting'))
    shaped('serbim_block', ['###', '###', '###'], {'#': 'serbim_ingot'}, 'serbim_block')
    shapeless('serbim_ingot_from_block', ['serbim_block'], 'serbim_ingot', 9)
    shaped('raw_serbim_block', ['###', '###', '###'], {'#': 'raw_serbim'}, 'raw_serbim_block')
    shapeless('raw_serbim_from_block', ['raw_serbim_block'], 'raw_serbim', 9)
    # Serbim + Copper = Siftite
    shapeless('siftite_ingot', ['serbim_ingot', 'minecraft:copper_ingot'], 'siftite_ingot', 1)
    shaped('siftite_block', ['###', '###', '###'], {'#': 'siftite_ingot'}, 'siftite_block')
    shapeless('siftite_ingot_from_block', ['siftite_block'], 'siftite_ingot', 9)
    shaped('siftite_ingot_from_nuggets', ['###', '###', '###'], {'#': 'siftite_nugget'}, 'siftite_ingot', 1, 'misc')
    shapeless('siftite_nugget', ['siftite_ingot'], 'siftite_nugget', 9)
    shaped('siftite_upgrade_smithing_template', ['#S#', '#C#', '###'], {'#': 'serbim_ingot', 'C': 'dreamstone', 'S': 'siftite_upgrade_smithing_template'},
           'siftite_upgrade_smithing_template', 2, 'misc')
    for t in ['sword', 'pickaxe', 'axe', 'shovel', 'hoe', 'spear', 'helmet', 'chestplate', 'leggings', 'boots']:
        write(os.path.join(D, 'recipe', f'siftite_{t}_smithing.json'),
              {'type': 'minecraft:smithing_transform', 'addition': f'#{NS}:siftite_tool_materials', 'base': f'minecraft:copper_{t}',
               'result': {'id': f'{NS}:siftite_{t}'}, 'template': f'{NS}:siftite_upgrade_smithing_template'})
    # gear & food
    shaped('slingshot', ['#S#', ' # ', ' # '], {'#': 'lullwood_planks', 'S': 'thick_hide'}, 'slingshot', 1, 'equipment')
    shaped('sift_cake', ['GGG', 'BEB', 'PPP'], {'G': 'glowing_slime_ball', 'B': 'pitcher_bulb', 'E': '#minecraft:eggs', 'P': 'dreambloom'}, 'sift_cake', 1, 'food')
    shaped('bulb_lantern', ['###', '#G#', '###'], {'#': 'minecraft:iron_nugget', 'G': 'glowing_slime_ball'}, 'bulb_lantern', 1, 'decorations')
    shaped('glowing_slime_block', ['###', '###', '###'], {'#': 'glowing_slime_ball'}, 'glowing_slime_block', 1, 'redstone')
    shapeless('glowing_slime_ball_from_block', ['glowing_slime_block'], 'glowing_slime_ball', 9)
    shaped('chrome_glass', [' # ', '#C#', ' # '], {'#': 'minecraft:glass', 'C': 'chrome_bucket'}, 'chrome_glass', 4)
    shaped('sift_drum', ['HHH', 'W W', 'WSW'], {'H': 'thick_hide', 'W': 'lullwood_planks', 'S': 'serbim_ingot'}, 'sift_drum', 1, 'redstone')
    # the first drum has to be built in the Overworld: echo shards come from the Ancient City the portal is opened in
    shaped('sift_drum_from_overworld', ['LLL', 'PEP', 'PNP'], {'L': 'minecraft:leather', 'P': '#minecraft:planks', 'E': 'minecraft:echo_shard',
                                                             'N': 'minecraft:note_block'}, 'sift_drum', 1, 'redstone')
    shaped('euphory_altar', [' P ', 'SDS', 'DDD'], {'P': 'chrome_pearl', 'S': 'siftite_ingot', 'D': 'polished_dreamstone'}, 'euphory_altar', 1, 'misc')
    shaped('echo_frame', ['#S#', 'SPS', '#S#'], {'#': 'hushslate_bricks', 'S': 'minecraft:echo_shard', 'P': 'chrome_pearl'}, 'echo_frame', 4, 'misc')
    shaped('soul_chime', [' I ', 'NGN', 'N N'], {'I': 'minecraft:iron_chain', 'N': 'serbim_ingot', 'G': 'soulpetal'}, 'soul_chime', 1, 'decorations')
    shapeless('dream_stew', ['minecraft:bowl', 'glowcap', 'pitcher_bulb', 'lullaby_bell'], 'dream_stew', 1, 'food')
    shaped('glowcap_skewer', ['  G', ' G ', '#  '], {'G': 'glowcap', '#': 'minecraft:stick'}, 'glowcap_skewer', 1, 'food')
    shaped('dream_snare', ['S S', ' P ', 'S S'], {'S': 'minecraft:string', 'P': 'glimmer_sprouts'}, 'dream_snare', 2, 'redstone')
    for f in spec.FLOWERS:
        shapeless(f'{f}_dye', [f], {'lullaby_bell': 'minecraft:cyan_dye', 'dreambloom': 'minecraft:pink_dye', 'soulpetal': 'minecraft:white_dye',
                                    'nebula_iris': 'minecraft:purple_dye'}[f], 1, 'misc', 'dye')
    shapeless('echo_orchid_dye', ['echo_orchid'], 'minecraft:light_blue_dye', 2, 'misc', 'dye')
    shapeless('choir_lily_dye', ['choir_lily'], 'minecraft:magenta_dye', 2, 'misc', 'dye')
    smelt('glowing_slime_ball_cooked', 'pitcher_bulb', 'glowing_slime_ball', 0.2, 200, ('smelting', 'smoking'))


# --------------------------------------------------------------------------- misc assets


def gen_particles():
    counts = {'drifting_soul': 4, 'chrome_droplet': 3, 'chrome_bubble': 1, 'dream_pollen': 2, 'sift_note': 1, 'resonance_ring': 1, 'glow_dust': 2,
              'lullwood_leaf': 3, 'wishwood_leaf': 3, 'sift_mist': 2, 'star_sparkle': 2, 'portal_soul': 3, 'footstep_puff': 3, 'glow_splat': 2,
              'wishing_star': 1, 'sleep_spore': 2}
    for p, n in counts.items():
        texs = [f'{NS}:{p}_{i}' if n > 1 else f'{NS}:{p}' for i in range(n)]
        write(os.path.join(A, 'particles', p + '.json'), {'textures': texs})
        for t in texs:
            TEXTURES.add('particle/' + t.split(':')[1])


def gen_equipment():
    write(os.path.join(A, 'equipment', 'siftite.json'), {'layers': {
        'humanoid': [{'texture': f'{NS}:siftite'}], 'humanoid_leggings': [{'texture': f'{NS}:siftite'}]}})
    TEXTURES.add('entity/equipment/humanoid/siftite')
    TEXTURES.add('entity/equipment/humanoid_leggings/siftite')


def gen_lang():
    LANG.update({
        f'itemGroup.{NS}.blocks': 'The Sift: Blocks', f'itemGroup.{NS}.items': 'The Sift: Items & Gear',
        f'entity.{NS}.bulb': 'Bulb', f'entity.{NS}.slumbler': 'Slumbler', f'entity.{NS}.sifter': 'Sifter', f'entity.{NS}.enchoer': 'Enchoer',
        f'entity.{NS}.riveter': 'Riveter', f'entity.{NS}.glowball': 'Glowball',
        f'effect.{NS}.deafened': 'Deafened', f'effect.{NS}.euphoria': 'Euphoria',
        f'biome.{NS}.sift_plains': 'Sift Plains', f'biome.{NS}.forest_mountains': 'Forest Mountains', f'biome.{NS}.rocky_dunes': 'Rocky Dunes',
        f'biome.{NS}.chrome_lakes': 'Chrome Lakes', f'biome.{NS}.deep_sift': 'Deep Sift', f'biome.{NS}.wishing_grove': 'Wishing Grove',
        f'fluid_type.{NS}.chrome': 'Chrome',
        f'item.{NS}.smithing_template.siftite_upgrade.applies_to': 'Copper Equipment',
        f'item.{NS}.smithing_template.siftite_upgrade.ingredients': 'Siftite Ingot',
        f'item.{NS}.smithing_template.siftite_upgrade.base_slot_description': 'Add copper armor, weapon, or tool',
        f'item.{NS}.smithing_template.siftite_upgrade.additions_slot_description': 'Add Siftite Ingot',
        f'upgrade.{NS}.siftite_upgrade': 'Siftite Upgrade',
        f'item.{NS}.music_disc_lullaby.desc': 'Sift - Lullaby of the Deep',
        f'jukebox_song.{NS}.lullaby': 'Sift - Lullaby of the Deep',
        f'message.{NS}.drum.need_sensors': 'The drum needs at least three Sculk Sensors nearby to listen...',
        f'message.{NS}.drum.no_frame': 'No portal frame answers the drum. Build one of Echo Frames or find an Ancient City gate.',
        f'message.{NS}.drum.listen': 'Listen...',
        f'message.{NS}.drum.your_turn': 'Your turn - play the rhythm back!',
        f'message.{NS}.drum.too_early': 'Too early! The sculk shrieks.',
        f'message.{NS}.drum.too_late': 'Too slow! The sculk shrieks.',
        f'message.{NS}.drum.round_done': 'The sculk hums in harmony...',
        f'message.{NS}.drum.opening': 'The Sift is waking!',
        f'message.{NS}.drum.opened': 'The way to The Sift is open.',
        f'message.{NS}.drum.core_removed': 'The Warden Core slips free.',
        f'message.{NS}.altar.need_drums': 'The altar needs at least two Sift Drums around it.',
        f'message.{NS}.altar.need_levels': 'You need more experience to perform the ritual.',
    })
    # sound subtitles
    for s in SOUNDS:
        LANG[f'subtitles.{NS}.{s}'] = subtitle(s)
    items = sorted(LANG.items())
    write(os.path.join(A, 'lang', 'en_us.json'), dict(items))


def subtitle(s):
    parts = s.split('.')
    who = spec.title(parts[1]) if len(parts) > 2 else spec.title(parts[0])
    what = parts[-1].replace('_', ' ')
    return f'{who} {what}s' if not what.endswith('s') else f'{who} {what}'


# --------------------------------------------------------------------------- sounds

# event -> list of (vanilla sound, volume, pitch)
SOUNDS = {
    'entity.bulb.ambient': [('mob/rabbit/idle1', 0.6, 1.4), ('mob/rabbit/idle2', 0.6, 1.5), ('mob/slime/small1', 0.5, 1.6)],
    'entity.bulb.hurt': [('mob/rabbit/hurt1', 0.8, 1.3), ('mob/rabbit/hurt2', 0.8, 1.3)],
    'entity.bulb.death': [('mob/rabbit/hurt3', 0.9, 1.1), ('mob/slime/big1', 0.6, 1.8)],
    'entity.bulb.hop': [('mob/slime/small1', 0.4, 1.6), ('mob/slime/small2', 0.4, 1.7), ('mob/slime/small3', 0.4, 1.8)],
    'entity.bulb.squish': [('mob/slime/small4', 0.3, 1.9), ('mob/slime/small5', 0.3, 2.0)],
    'entity.bulb.happy': [('block/note_block/chime', 0.7, 1.5), ('mob/allay/idle_with_item1', 0.5, 1.6)],
    'entity.bulb.lay': [('mob/slime/attack1', 0.6, 1.6), ('random/pop', 0.6, 0.8)],
    'entity.bulb.eat': [('random/eat1', 0.6, 1.4), ('random/eat2', 0.6, 1.4)],
    'entity.slumbler.ambient': [('mob/frog/idle1', 1.0, 0.5), ('mob/frog/idle2', 1.0, 0.45), ('mob/axolotl/idle_air1', 1.0, 0.5)],
    'entity.slumbler.hurt': [('mob/frog/hurt1', 1.0, 0.5), ('mob/frog/hurt2', 1.0, 0.5)],
    'entity.slumbler.death': [('mob/frog/death1', 1.0, 0.45), ('mob/frog/death2', 1.0, 0.45)],
    'entity.slumbler.yawn': [('mob/sniffer/happy1', 1.0, 0.7), ('mob/sniffer/happy2', 1.0, 0.65)],
    'entity.slumbler.bite': [('mob/frog/tongue1', 1.0, 0.5), ('mob/frog/tongue2', 1.0, 0.5)],
    'entity.slumbler.step': [('mob/sniffer/step1', 0.8, 1.0), ('mob/sniffer/step2', 0.8, 1.0)],
    'entity.sifter.ambient': [('mob/silverfish/say1', 0.7, 0.7), ('mob/silverfish/say2', 0.7, 0.75), ('mob/squid/ambient1', 0.6, 1.4)],
    'entity.sifter.hurt': [('mob/silverfish/hit1', 0.8, 0.7), ('mob/squid/hurt1', 0.8, 1.3)],
    'entity.sifter.death': [('mob/silverfish/kill', 0.9, 0.7), ('mob/squid/death1', 0.9, 1.2)],
    'entity.sifter.chomp': [('mob/fox/bite1', 1.0, 0.8), ('mob/fox/bite2', 1.0, 0.8)],
    'entity.sifter.leap': [('block/sand/break1', 1.0, 0.8), ('block/sand/break2', 1.0, 0.8)],
    'entity.sifter.step': [('mob/silverfish/step1', 0.4, 1.2), ('mob/silverfish/step2', 0.4, 1.2)],
    'entity.enchoer.ambient': [('mob/allay/idle_without_item1', 0.8, 0.6), ('mob/allay/idle_without_item2', 0.8, 0.55), ('block/amethyst/resonate1', 0.8, 0.7)],
    'entity.enchoer.hum': [('block/amethyst/resonate2', 1.0, 0.6), ('block/amethyst/resonate3', 1.0, 0.7), ('mob/allay/item_given1', 0.8, 0.5)],
    'entity.enchoer.trade': [('mob/allay/idle_with_item1', 0.8, 0.6), ('mob/allay/idle_with_item2', 0.8, 0.6)],
    'entity.enchoer.yes': [('mob/allay/item_given2', 0.9, 0.7), ('block/amethyst/shimmer', 1.0, 1.0)],
    'entity.enchoer.no': [('mob/allay/item_taken1', 0.9, 0.5)],
    'entity.enchoer.hurt': [('mob/allay/hurt1', 1.0, 0.6), ('block/amethyst_cluster/break1', 1.0, 0.8)],
    'entity.enchoer.death': [('mob/allay/death1', 1.0, 0.55), ('block/amethyst_cluster/break2', 1.0, 0.6)],
    'entity.riveter.ambient': [('mob/bat/idle1', 0.5, 0.5), ('mob/bat/idle2', 0.5, 0.5), ('block/sculk/spread1', 0.6, 0.7)],
    'entity.riveter.scream': [('mob/warden/roar1', 0.8, 1.8), ('block/sculk_shrieker/shriek1', 1.0, 1.1), ('block/sculk_shrieker/shriek2', 1.0, 1.1)],
    'entity.riveter.hurt': [('mob/bat/hurt1', 0.8, 0.6), ('mob/bat/hurt2', 0.8, 0.6)],
    'entity.riveter.death': [('mob/bat/death', 1.0, 0.5), ('block/sculk/break1', 1.0, 0.8)],
    'block.sift_drum.low': [('block/note_block/basedrum', 1.0, 0.7)],
    'block.sift_drum.mid': [('block/note_block/basedrum', 1.0, 1.0), ('block/note_block/hat', 0.3, 0.8)],
    'block.sift_drum.high': [('block/note_block/snare', 1.0, 1.0)],
    'block.sift_drum.boom': [('block/note_block/basedrum', 1.0, 0.5), ('random/explode1', 0.3, 1.8)],
    'block.euphory_altar.charge': [('block/beacon/activate', 1.0, 1.4), ('block/amethyst/resonate1', 1.0, 1.2)],
    'block.euphory_altar.enchant': [('block/enchantment_table/enchant1', 1.0, 0.8), ('block/beacon/power1', 1.0, 1.5)],
    'block.euphory_altar.hum': [('block/beacon/ambient', 0.8, 1.2), ('block/amethyst/resonate2', 0.6, 1.3)],
    'block.sift_portal.ambient': [('block/portal/portal', 0.4, 1.6), ('block/amethyst/resonate3', 0.3, 1.4)],
    'block.sift_portal.activate': [('block/end_portal/endportal', 1.0, 1.2), ('block/beacon/activate', 1.0, 0.8)],
    'block.sift_portal.travel': [('block/portal/travel', 0.8, 1.5)],
    'event.rhythm.call': [('block/sculk_sensor/sculk_clicking1', 1.0, 1.0), ('block/sculk_sensor/sculk_clicking2', 1.0, 1.1)],
    'event.rhythm.good': [('block/note_block/chime', 1.0, 1.2), ('block/amethyst/shimmer', 1.0, 1.0)],
    'event.rhythm.fail': [('block/sculk_shrieker/shriek1', 1.0, 0.8)],
    'event.rhythm.round': [('block/note_block/bell', 1.0, 1.0), ('block/note_block/bell', 1.0, 1.26)],
    'block.chrome.ambient': [('liquid/water', 0.3, 1.6), ('block/amethyst/resonate1', 0.2, 1.8)],
    'block.chrome.splash': [('liquid/splash', 0.8, 1.4), ('liquid/splash2', 0.8, 1.5)],
    'block.harmony_stone.tone': [('block/note_block/chime', 1.0, 1.0)],
    'block.harmony_seal.unlock': [('block/trial_spawner/ominous_activate', 1.0, 1.4), ('block/beacon/deactivate', 1.0, 1.5)],
    'block.soul_chime.ring': [('block/bell/resonate', 0.7, 1.6), ('block/note_block/chime', 0.8, 1.2), ('block/note_block/chime', 0.8, 1.5)],
    'block.choir_lily.sing': [('block/note_block/flute', 0.8, 1.0), ('block/amethyst/resonate2', 0.6, 1.4)],
    'block.dream_snare.trigger': [('block/tripwire/click_on', 1.0, 0.6), ('block/sculk_catalyst/bloom1', 1.0, 1.4)],
    'block.crumbling_dreamstone.crumble': [('block/pointed_dripstone/drip_lava1', 1.0, 0.6), ('dig/stone1', 1.0, 0.7)],
    'item.slingshot.shoot': [('random/bow', 1.0, 1.4), ('mob/slime/small1', 0.8, 1.6)],
    'item.slingshot.pull': [('item/crossbow/loading_start', 0.8, 1.5)],
    'entity.glowball.burst': [('block/amethyst/break1', 1.0, 1.3), ('mob/slime/big2', 0.8, 1.5), ('random/firework/twinkle1', 0.6, 1.4)],
    'item.warden_core.pulse': [('mob/warden/heartbeat1', 0.3, 1.0), ('mob/warden/heartbeat2', 0.3, 1.0)],
    'ambient.sift.loop': [('ambient/cave/cave13', 0.4, 1.4)],
    'ambient.sift.additions': [('block/amethyst/resonate1', 0.4, 1.5), ('block/amethyst/resonate2', 0.4, 1.7), ('mob/allay/idle_without_item3', 0.3, 1.2),
                               ('block/note_block/chime', 0.3, 0.8)],
    'ambient.sift.mood': [('ambient/cave/cave7', 0.6, 1.3), ('ambient/cave/cave9', 0.6, 1.4)],
    'ambient.deep_sift.loop': [('ambient/cave/cave11', 0.5, 0.8)],
    'music.sift': [('music/game/an_ordinary_day', 0.6, 1.0), ('music/game/infinite_amethyst', 0.6, 1.0), ('music/game/left_to_bloom', 0.6, 1.0),
                   ('music/game/one_more_day', 0.6, 1.0)],
    'music.deep_sift': [('music/game/ancestry', 0.6, 1.0), ('music/game/echo_in_the_wind', 0.6, 1.0)],
    'music_disc.lullaby': [('music/game/comforting_memories', 1.0, 1.0)],
}
STREAM = {'music.sift', 'music.deep_sift', 'music_disc.lullaby', 'ambient.sift.loop', 'ambient.deep_sift.loop'}


def gen_sounds():
    out = {}
    for ev, lst in SOUNDS.items():
        entries = []
        for name, vol, pitch in lst:
            e = {'name': f'minecraft:{name}', 'volume': vol, 'pitch': pitch}
            if ev in STREAM:
                e['stream'] = True
            entries.append(e)
        out[ev] = {'sounds': entries, 'subtitle': f'subtitles.{NS}.{ev}'}
        if ev.startswith('music'):
            del out[ev]['subtitle']
    write(os.path.join(A, 'sounds.json'), out)


# --------------------------------------------------------------------------- misc tags & data


def gen_misc_tags():
    for b in ['sift_grass_block', 'sift_soil', 'lumen_moss_block']:
        tag('block', f'{NS}:sift_plantable', rl(b))
        tag('block', 'minecraft:sniffer_diggable_block', rl(b))
    tag('block', f'{NS}:sift_plantable', '#minecraft:dirt')
    tag('block', f'{NS}:portal_frame', 'minecraft:reinforced_deepslate')
    for b in ['hushslate', 'cobbled_hushslate', 'minecraft:sculk', 'minecraft:deepslate', 'lumen_moss_block']:
        tag('block', f'{NS}:deep_sift_ground', rl(b))
    for b in ['hushslate', 'cobbled_hushslate', 'hushslate_bricks', 'minecraft:sculk', 'minecraft:deepslate', 'minecraft:reinforced_deepslate',
              'minecraft:deepslate_bricks', 'minecraft:deepslate_tiles']:
        tag('block', f'{NS}:riveter_roost', rl(b))
    tag('block', f'{NS}:incorrect_for_siftite_tool', '#minecraft:incorrect_for_diamond_tool')
    for f in list(spec.FLOWERS) + ['echo_orchid', 'choir_lily', 'pitcher_bulb_bush', 'glowbell_vine', 'soul_chime']:
        tag('block', f'{NS}:resonant', rl(f))
    tag('block', f'{NS}:sift_stone', rl('hushslate'))
    tag('block', 'minecraft:base_stone_overworld', rl('dreamstone'))
    tag('block', 'minecraft:base_stone_overworld', rl('hushslate'))
    tag('block', 'minecraft:climbable', rl('glowbell_vine'))
    tag('block', 'minecraft:climbable', rl('glowbell_vine_plant'))
    tag('block', 'minecraft:bee_attractive', rl('dreambloom'))
    tag('block', 'minecraft:enderman_holdable', rl('dreamsand'))
    tag('block', 'minecraft:enderman_holdable', rl('sift_grass_block'))
    tag('block', 'minecraft:animals_spawnable_on', rl('sift_grass_block'))
    tag('block', 'minecraft:sniffer_egg_hatch_boost', rl('lumen_moss_block'))
    tag('block', 'minecraft:mineable/hoe', rl('hanging_lullwood_leaves'))
    tag('block', 'minecraft:mineable/axe', rl('sift_drum'))
    tag('block', 'minecraft:dampens_vibrations', rl('lumen_moss_carpet'))
    tag('block', 'minecraft:inside_step_sound_blocks', rl('drift_petals'))
    # items
    tag('item', f'{NS}:bulb_food', rl('pitcher_bulb'))
    for i in ['glowcap', 'minecraft:tropical_fish', 'minecraft:cod', 'glowcap_skewer']:
        tag('item', f'{NS}:slumbler_food', rl(i))
    tag('item', f'{NS}:slingshot_ammo', rl('glowing_slime_ball'))
    tag('item', f'{NS}:siftite_tool_materials', rl('siftite_ingot'))
    tag('item', f'{NS}:repairs_siftite_armor', rl('siftite_ingot'))
    tag('item', f'{NS}:altar_fuel', rl('chrome_pearl'))
    for t, vt in [('sword', 'swords'), ('pickaxe', 'pickaxes'), ('axe', 'axes'), ('shovel', 'shovels'), ('hoe', 'hoes'), ('spear', 'spears')]:
        tag('item', f'minecraft:{vt}', rl(f'siftite_{t}'))
    for a, vt in [('helmet', 'head_armor'), ('chestplate', 'chest_armor'), ('leggings', 'leg_armor'), ('boots', 'foot_armor')]:
        tag('item', f'minecraft:{vt}', rl(f'siftite_{a}'))
        tag('item', 'minecraft:trimmable_armor', rl(f'siftite_{a}'))
    tag('item', 'minecraft:sniffer_food', rl('pitcher_bulb'))
    tag('item', 'minecraft:frog_food', rl('glowing_slime_ball')) if False else None
    # entity types
    tag('entity_type', f'{NS}:chrome_dwellers', rl('slumbler'))
    for e in ['bulb', 'enchoer', 'minecraft:allay', 'minecraft:sniffer', 'slumbler']:
        tag('entity_type', f'{NS}:music_lovers', rl(e))
    for b in ['sift_plains', 'forest_mountains', 'rocky_dunes', 'chrome_lakes', 'deep_sift', 'wishing_grove']:
        tag('worldgen/biome', f'{NS}:is_sift', rl(b))


def gen_transformers():
    """Axe stripping via NeoForge's block transformer data map; tilling/paths via vanilla tags."""
    values = {}
    for w in spec.WOODS:
        for src, dst in ((f'{w}_log', f'stripped_{w}_log'), (f'{w}_wood', f'stripped_{w}_wood')):
            values[rl(src)] = {'transformer': 'minecraft:axe', 'transform_data': {
                'block_state_provider': {'type': 'minecraft:rule_based', 'rules': [
                    {'if_true': {'type': 'minecraft:matching_blocks', 'blocks': rl(src)},
                     'then': {'type': 'minecraft:copy_properties', 'source': {'id': rl(dst), 'properties': {'axis': 'y'}}}}]},
                'item_damage_per_use': 1, 'sound': 'minecraft:item.axe.strip'}}
    write(os.path.join(RES, 'data', 'neoforge', 'data_maps', 'block', 'transformables.json'), {'values': values})
    for b in ('sift_grass_block', 'sift_soil'):
        tag('block', 'minecraft:turns_into_farmland', rl(b))
    tag('block', 'minecraft:turns_into_dirt_path', rl('sift_grass_block'))


def flush_tags():
    for (reg, ns, path), values in TAGS.items():
        write(os.path.join(RES, 'data', ns, 'tags', reg, path + '.json'), {'replace': False, 'values': values})


# --------------------------------------------------------------------------- main


def generate():
    for b in spec.BLOCKS:
        gen_block(b)
        gen_loot(b)
        gen_block_tags(b)
    for i in spec.ITEMS:
        gen_item(i)
    gen_recipes()
    gen_particles()
    gen_equipment()
    gen_sounds()
    gen_misc_tags()
    gen_transformers()


def finalize():
    flush_tags()
    gen_lang()
    os.makedirs(os.path.join(ROOT, 'build'), exist_ok=True)
    with open(os.path.join(ROOT, 'build', 'textures_needed.txt'), 'w') as f:
        f.write('\n'.join(sorted(TEXTURES)) + '\n')
    print(f'assets ok: {len(WRITTEN)} files, {len(TEXTURES)} textures referenced')


def main():
    generate()
    finalize()


if __name__ == '__main__':
    main()
