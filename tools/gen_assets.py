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
    elif k == 'planter':  # H: the potted pitcher
        import plants_h
        plants_h.gen_planter(sys.modules[__name__], bid)
    elif k == 'cannon':
        import siege  # the Thumper's arena
        siege.gen_cannon(bid)
    elif k == 'music_crystal':  # C: the Caravans' music crystals
        __import__('caravans').gen_crystal(bid)
    elif k.startswith('sea_'):  # sea & sky: glowkelp, anemone, chime bell, organ reed
        __import__('sea_sky').gen_block(sys.modules[__name__], b)
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
    import hdblocks as HB
    variants = {}
    for hit in (0, 1, 2):
        for core in (False, True):
            name = bid + ('' if hit == 0 else f'_hit{hit}') + ('_core' if core else '')
            m = HB.drum_model(hit, core, NS, bid)
            note_textures(m)
            write(os.path.join(A, 'models/block', name + '.json'), m)
            variants[f'hit={hit},core={str(core).lower()}'] = {'model': f'{NS}:block/{name}'}
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
    elif lk.startswith('web:'):  # the Weaver (E2): shears or silk touch for the web, else its string
        loot(bid, 'cobweb', {'cobweb': bid, 'string': lk[4:]})
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
    # the instruments of the Conductor's three great players
    # the Crane Flute is carved, not looted: a bone bored for the notes, an amethyst reed, an echo shard to sing through
    shaped('crane_flute', ['  E', ' A ', 'B  '], {'E': 'minecraft:echo_shard', 'A': 'minecraft:amethyst_shard', 'B': 'minecraft:bone'}, 'crane_flute',
           category='equipment')
    # the Thumper's arena: cannonballs (four to a craft)
    shaped('cannonball', ['NIN', 'GCG', 'NIN'], {'N': 'minecraft:iron_nugget', 'I': 'minecraft:iron_ingot', 'G': 'minecraft:gunpowder',
                                               'C': 'cobbled_dreamstone'}, 'cannonball', count=4, category='equipment')
    shaped('guitar', ['  S', 'PS ', 'PP '], {'S': 'sculk_string', 'P': '#minecraft:planks'}, 'guitar', category='equipment')
    shapeless('serbim_ingot_from_block', ['serbim_block'], 'serbim_ingot', 9)
    shaped('raw_serbim_block', ['###', '###', '###'], {'#': 'raw_serbim'}, 'raw_serbim_block')
    shapeless('raw_serbim_from_block', ['raw_serbim_block'], 'raw_serbim', 9)
    # Siftite: 4 Serbim + 4 Echo Shards around a Netherite Ingot = 2 Siftite (H: costlier than Netherite itself)
    shaped('siftite_ingot', ['SES', 'ENE', 'SES'], {'S': 'serbim_ingot', 'E': 'minecraft:echo_shard', 'N': 'minecraft:netherite_ingot'}, 'siftite_ingot', 2,
           'misc')
    shaped('siftite_block', ['###', '###', '###'], {'#': 'siftite_ingot'}, 'siftite_block')
    shapeless('siftite_ingot_from_block', ['siftite_block'], 'siftite_ingot', 9)
    shaped('siftite_ingot_from_nuggets', ['###', '###', '###'], {'#': 'siftite_nugget'}, 'siftite_ingot', 1, 'misc')
    shapeless('siftite_nugget', ['siftite_ingot'], 'siftite_nugget', 9)
    shaped('siftite_upgrade_smithing_template', ['#S#', '#C#', '###'], {'#': 'serbim_ingot', 'C': 'dreamstone', 'S': 'siftite_upgrade_smithing_template'},
           'siftite_upgrade_smithing_template', 2, 'misc')
    for t in ['sword', 'pickaxe', 'axe', 'shovel', 'hoe', 'spear', 'helmet', 'chestplate', 'leggings', 'boots']:
        write(os.path.join(D, 'recipe', f'siftite_{t}_smithing.json'),
              {'type': 'minecraft:smithing_transform', 'addition': f'#{NS}:siftite_tool_materials', 'base': f'minecraft:netherite_{t}',
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
              'lullwood_leaf': 3, 'wishwood_leaf': 3, 'sift_mist': 2, 'star_sparkle': 2, 'portal_soul': 3, 'footstep_puff': 3, 'glow_splat': 2, 'slime_trail': 3, 'guide_note': 1,
              'wishing_star': 1, 'sleep_spore': 2, 'kill_star': 4}
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
        f'entity.{NS}.harmoner': 'Harmoner', f'entity.{NS}.sculk_harmoner': 'Sculk Harmoner',
        f'codex.{NS}.chapter.creatures': 'Creatures',
        f'codex.{NS}.chapter.items': 'Items & Gear',
        f'codex.{NS}.chapter.places': 'Places',
        f'codex.{NS}.chapter.magic': 'Music & Magic',
        f'codex.{NS}.chapter.dictator': 'The Dictator',
        f'codex.{NS}.bulb.title': 'Bulb', f'codex.{NS}.bulb.tagline': 'Squishy jelly bunny',
        f'codex.{NS}.bulb.body': 'Bulbs hop all over the plains, squatting before every hop and splatting on every landing. They sniff the air, groom their long ears and curl up asleep at night. Feed them Pitcher Bulbs to breed them - they wiggle with joy - and happy Bulbs plop out Glowing Slime Balls. Play any note and they bounce to the beat. Four colours: sky, blossom, dusk and the rare starry.',
        f'codex.{NS}.harmoner.title': 'Harmoner', f'codex.{NS}.harmoner.tagline': 'Songbird and guide',
        f'codex.{NS}.harmoner.body': 'Harmoners peck for seeds by day, preen each other and roost with their heads under a wing at night; when one sings, the flock joins in harmony. Feed one seeds and it sings, takes off and leads you somewhere, waiting if you fall behind. Its colour tells you where: Rose - Abandoned Altar. Azure - Chrome Well. Gold - Dream Statue. Violet - Collapsed Tower. Jade - Sift Ruins. Coral - Musical Temple. The rare Night - the Sculk Castle.',
        f'codex.{NS}.sniffer.title': 'Sniffer', f'codex.{NS}.sniffer.tagline': 'The Sift remembers its seeds',
        f'codex.{NS}.sniffer.body': 'Ordinary Sniffers wander the Sift\'s plains and forests. Wherever one digs here it turns up the dimension\'s own ancient seeds as well as its usual finds: Choir Pods, Echo Seeds and Pitcher Bulbs. Breed them with torchflower seeds, or bring a Sniffer egg through the gate yourself.',
        f'codex.{NS}.enchoer.title': 'Enchoer', f'codex.{NS}.enchoer.tagline': 'Gentle, sad trader',
        f'codex.{NS}.enchoer.body': 'A big mound of mint fur with moose antlers and a melancholy face. Enchoers trade saplings, seeds, drums, pearls and the occasional Warden Core. Play music near one and it spreads its arms and hums along. It hides its face when hurt.',
        f'codex.{NS}.slumbler.title': 'Slumbler', f'codex.{NS}.slumbler.tagline': 'Chrome lake salamander',
        f'codex.{NS}.slumbler.body': 'Huge, wide-mouthed and mostly asleep. Slumblers gulp Chrome plankton from the shallows, nuzzle each other, hum along to music and wade into shallow Chrome to nap half-submerged. They drop Thick Hide - and sometimes a Chrome Pearl. Let sleeping Slumblers lie: they bite.',
        f'codex.{NS}.sifter.title': 'Sifter', f'codex.{NS}.sifter.tagline': 'Hostile - dune lurker',
        f'codex.{NS}.sifter.body': 'A sandstone-and-bone trap on crab legs. Sifters dig into the dunes until only the lid and a glowing lure show, then burst out and slam the lid shut on whatever came to look. A glow on the sand is never just a glow. Music makes one forget its prey.',
        f'codex.{NS}.riveter.title': 'Riveter', f'codex.{NS}.riveter.tagline': 'Hostile - the sculk bat',
        f'codex.{NS}.riveter.body': 'It hangs head-down from cave ceilings in the Deep Sift. By day it roosts asleep in its wings and only wakes if you walk right under it; by night it chitters with its neighbours and flits out to snap glow dust. When it sees you it screams - and every Warden nearby comes running. Sneak, come by day, or play music to lull it.',
        f'codex.{NS}.siftite.title': 'Siftite Gear', f'codex.{NS}.siftite.tagline': "The Sift's finest metal",
        f'codex.{NS}.siftite.body': 'Serbim is very rare, deep down. 4 Serbim Ingots and 4 Echo Shards round a Netherite Ingot make 2 Siftite; the Siftite template upgrades Netherite gear. Tools beat Netherite and knock foes flying. Armour: no Deafening, softer sonic booms, helmet breathes water, legs and boots swim fast, full set halves Sculk Corruption.',
        f'codex.{NS}.slingshot.title': 'Slingshot', f'codex.{NS}.slingshot.tagline': 'Glowing slime, at speed',
        f'codex.{NS}.slingshot.body': 'Fires Glowing Slime Balls. A fully drawn shot bursts into light where it lands - and a direct hit on a Warden leaves it Deafened, unable to hear you for a while.',
        f'codex.{NS}.chrome.title': 'Chrome', f'codex.{NS}.chrome.tagline': 'Liquid that heals',
        f'codex.{NS}.chrome.body': 'A shifting cyan pearl liquid. Soaking in Chrome heals you, but it is thick like quicksand and you sink slowly: hold Shift to rise. Collect it with a bucket.',
        f'codex.{NS}.warden_core.title': 'Warden Core', f'codex.{NS}.warden_core.tagline': 'The heart of the ritual',
        f'codex.{NS}.warden_core.body': "Taken from a Warden or found in the deepest shrines. In a Sift Drum it leads the ritual that opens the way to The Sift, and speaks for you: sensors hear the beats, shriekers can't tell who played. Sneak and use the drum to take it out.",
        f'codex.{NS}.sift_cake.title': 'Sift Cake', f'codex.{NS}.sift_cake.tagline': 'A treat from the plains',
        f'codex.{NS}.sift_cake.body': 'Baked from Glowing Slime Balls and Sift produce. Each slice restores a little hunger and leaves you glowing softly for a moment.',
        f'codex.{NS}.baton.title': "Conductor's Baton", f'codex.{NS}.baton.tagline': 'Taken from the Dictator',
        f'codex.{NS}.baton.body': 'Strikes as hard as a sword. Use it to flick a single sonic note down the line you point at, hurting the first creature in its way. It needs a moment to recover between notes.',
        f'codex.{NS}.portal.title': 'The Way In', f'codex.{NS}.portal.tagline': 'A rhythm at the Ancient City',
        f'codex.{NS}.portal.body': 'Find the great gate in an Ancient City, or build a frame of Sift Gate Frames. Set a Sift Drum near it with three Sculk Sensors around and slot in a Warden Core. The drum calls a rhythm, a rising note per beat: play it back on the drum. Three rounds right and the gate wakes.',
        f'codex.{NS}.musical_temple.title': 'Musical Temple', f'codex.{NS}.musical_temple.tagline': 'Puzzles of tone',
        f'codex.{NS}.musical_temple.body': 'Old temples of song. Tune each Harmony Stone to the colour of its pedestal to open the vault below.',
        f'codex.{NS}.chrome_well.title': 'Chrome Well', f'codex.{NS}.chrome_well.tagline': 'Pearls in the pool',
        f'codex.{NS}.chrome_well.body': 'Little wells of Chrome in the plains and dunes, often with a chest of Chrome Pearls and buckets hidden nearby.',
        f'codex.{NS}.ruins.title': 'Ruins & Statues', f'codex.{NS}.ruins.tagline': 'Dig, brush, explore',
        f'codex.{NS}.ruins.body': 'Sift Ruins, Collapsed Towers, Abandoned Altars and Dream Statues are scattered across the surface. Brush suspicious dreamsand for relics and read the Dream Journal Fragments you find.',
        f'codex.{NS}.deep_shrine.title': 'Deep Shrine', f'codex.{NS}.deep_shrine.tagline': 'Below the Sift',
        f'codex.{NS}.deep_shrine.body': 'Hushslate shrines in the caves of the Deep Sift. Each holds an ancient gate of reinforced deepslate capped with Sift Gate Frame keystones - your way home - and a chest that sometimes keeps a Warden Core. Riveters roost nearby.',
        f'codex.{NS}.sculk_castle.title': 'The Sculk Castle', f'codex.{NS}.sculk_castle.tagline': 'Climb, if you dare',
        f'codex.{NS}.sculk_castle.body': "A tower of sculk and hushslate. Inside, a spiral of steps climbs the wall - jump the gaps, mind the crumbling ones, rest at the lantern ledges. Slime catches you if you fall. On the roof waits the Grand Stage, and its three empty altars.",
        f'codex.{NS}.sift_drum.title': 'Sift Drum', f'codex.{NS}.sift_drum.tagline': 'Play it with either hand',
        f'codex.{NS}.sift_drum.body': 'Left- or right-click to play a beat; hold for a drum roll. Sneak and left-click to break it. Stone beneath booms low, wood thumps, anything else taps high. Redstone plays it too. Every beat ripples out to altars and Sift creatures and sets off Sculk Sensors, like a note block.',
        f'codex.{NS}.euphory_altar.title': 'Euphory Altar', f'codex.{NS}.euphory_altar.tagline': 'Enchanting by music',
        f'codex.{NS}.euphory_altar.body': 'Set an item on the altar, surround it with Sift Drums and feed it a Chrome Pearl. The drums play themselves, the rings spin up and the item comes out enchanted beyond what a table can do.',
        f'codex.{NS}.music.title': 'Music & Chimes', f'codex.{NS}.music.tagline': 'The Sift listens',
        f'codex.{NS}.music.body': 'Many Sift creatures react to music: Bulbs dance, Enchoers hum, Harmoners sing along and Riveters fall still. Soul Chimes ring when powered; Dream Snares lull whatever steps in them to sleep.',
        f'codex.{NS}.flora.title': 'Coral Flora', f'codex.{NS}.flora.tagline': 'The pink plains',
        f'codex.{NS}.flora.body': 'Coral Bushes and tall Coral Thickets grow thick across the salmon Coral Turf of the Sift Plains, under pale weeping Lullwood trees.',
        f'codex.{NS}.dictator.title': 'The Conductor', f'codex.{NS}.dictator.tagline': 'Three movements, each more godlike',
        f'codex.{NS}.dictator.body': "When the music ends, only his Mask is left on the stage floor - and then souls, notes and sculk pour in and build him again around it. Every blow of his leaves Sculk Corruption. First he duels you on the stage: blinks, lunges and slashes. Then he lifts off his feet: barrages of notes, and chords that shake rings across the floor - jump them. At the last he soars high and rains notes down; when the whole stage starts to glow, run for a lit circle. Between movements he rises in a storm of song and cannot be hurt.",
        f'codex.{NS}.thumper.title': 'The Thumper', f'codex.{NS}.thumper.tagline': 'Percussion - a turtle with a heart of sculk',
        f'codex.{NS}.thumper.body': 'A giant Warden-kin turtle that crawls up out of its sunken arena. Its shell turns every blade and arrow. Only a CANNONBALL on one of its glowing sculk vents hurts it - and the vents open only when it strains: after a stomp, while it breathes its beam, when it bursts from the ground, and when it rams a solid wall. Man the Ancient Cannons on the towers. Jump its stomp rings, back away from its tail. Hurt, it hurls boulders; enraged, it burrows after you. Drops its Conga Drum.',
        f'codex.{NS}.strummer.title': 'The Weaver', f'codex.{NS}.strummer.tagline': 'Strings - a sculk spider and its bone musician',
        f'codex.{NS}.strummer.body': "A great sculk spider that crawls up out of the ground when woken, a bone mantis on its back playing the glowing strings of its silk. It fights in three movements. First it slashes, spits silk, pounces, snaps a string to drag you in, lays Sculk Spiders and strums them stronger. Two thirds down it roars and rings itself in webs: now it runs up the walls, fires a thread to the ceiling and swings across the arena to drop on you, and weaves an orb of Musical Cobwebs around you in a blink - watch for the ring of light. At one third it roars again: faster, and every strum makes its webs sing and hurt. Drops the Weaver's Guitar.",
        f'codex.{NS}.strumling.title': 'Sculk Spider', f'codex.{NS}.strumling.tagline': "The Weaver's brood",
        f'codex.{NS}.strumling.body': 'Long-legged spiders of bone and sculk, eight eyes glowing over hooked fangs and glowing sacs bulging between the plates of their backs. They skitter up walls, walk through webs and, when they sink down and rear their front legs, pounce. They drop Sculk String.',
        f'codex.{NS}.conga_drum.title': 'Conga Drum', f'codex.{NS}.conga_drum.tagline': "The Thumper's drum",
        f'codex.{NS}.conga_drum.body': 'Use it to beat a massive shockwave: soft blocks around you shatter and everything nearby is hurled away. It takes half a minute to ring out before you can play it again. Stompers love it.',
        f'codex.{NS}.crane_flute.title': 'Crane Flute', f'codex.{NS}.crane_flute.tagline': 'Bone, amethyst and an echo',
        f'codex.{NS}.crane_flute.body': 'Hold use on a creature: a thin red line marks it, and a moment later the flute locks on - a sonic beam that hurts and slows it for as long as you keep playing and keep it in sight, up to 24 blocks away. Sneak and use it to play single notes: look up for higher notes, down for lower. Carry a Music Sheet and play its notes in order to perform the song.',
        f'codex.{NS}.guitar.title': 'Guitar', f'codex.{NS}.guitar.tagline': 'Strung with Sculk String',
        f'codex.{NS}.guitar.body': "Planks and Sculk String from the Sculk Spiders. Every strum plays one note, and where you look picks it: look up for the high notes, down for the low ones - two octaves, like a note block. Learn a Music Sheet and you can play its song.",
        f'codex.{NS}.weaver_guitar.title': "Weaver's Guitar", f'codex.{NS}.weaver_guitar.tagline': "The Weaver's own instrument",
        f'codex.{NS}.weaver_guitar.body': "It plays like any guitar, one note a strum. Sneak and strum to weave: a ring of Musical Cobwebs springs up around you and every hostile creature nearby is snared in silk where it stands - bounced, bound and slowed. Then the strings need a few seconds to settle. Only the Weaver drops it, and only it will sound on the Grand Stage.",
        f'codex.{NS}.musical_cobweb.title': 'Musical Cobweb', f'codex.{NS}.musical_cobweb.tagline': 'Tuned silk',
        f'codex.{NS}.musical_cobweb.body': "The Weaver's glowing webs. They barely hold you - a little drag and a springy bounce - but every strand is tuned and plays its note when touched, so a web plays runs as you push through it. Shears or Silk Touch keep the web; otherwise it leaves Sculk String.",
        f'codex.{NS}.stage.title': 'The Grand Stage', f'codex.{NS}.stage.tagline': 'Three altars, three instruments',
        f'codex.{NS}.stage.body': "On the roof of the Sculk Castle stands a stage with three empty altars. Place the Conga Drum, the Crane Flute and the Weaver's Guitar on them and they begin to play together. The sky darkens. The world falls silent. And from beneath the stage, a mask rises...",
        f'codex.{NS}.vocals.title': 'The Vocals', f'codex.{NS}.vocals.tagline': 'A Warden answers',
        f'codex.{NS}.vocals.body': 'When the Dictator reaches his crescendo, a Warden claws up through the floor to sing for him. A Glowing Slime Ball from a slingshot leaves it Deafened.',
        f'entity.{NS}.dictator': 'The Conductor', f'entity.{NS}.thumper': 'The Thumper',
        f'entity.{NS}.strummer': 'The Weaver', f'entity.{NS}.cannonball': 'Cannonball',
        f'entity.{NS}.strumling': 'Sculk Spider', f'entity.{NS}.web_shot': 'Silk', f'entity.{NS}.conductor_mask': "The Conductor's Mask",
        f'message.{NS}.dictator.wakes': 'The Conductor raises his staff. The performance begins...',
        f'message.{NS}.stage.begins': 'The three instruments begin to play together...',
        f'message.{NS}.harmoner.tamed': 'The Harmoner chirps and settles on your shoulder. It will follow you - and sing along with your flute.',
        f'message.{NS}.harmoner.lost': 'The Harmoner tilts its head. It cannot sense any place of its colour nearby.',
        f'message.{NS}.harmoner.guide.rose': 'The Rose Harmoner sings and takes off towards an Abandoned Altar!',
        f'message.{NS}.harmoner.guide.azure': 'The Azure Harmoner sings and takes off towards a Chrome Well!',
        f'message.{NS}.harmoner.guide.gold': 'The Gold Harmoner sings and takes off towards a Dream Statue!',
        f'message.{NS}.harmoner.guide.violet': 'The Violet Harmoner sings and takes off towards a Collapsed Tower!',
        f'message.{NS}.harmoner.guide.jade': 'The Jade Harmoner sings and takes off towards the Sift Ruins!',
        f'message.{NS}.harmoner.guide.coral': 'The Coral Harmoner sings and takes off towards a Musical Temple!',
        f'message.{NS}.harmoner.guide.night': 'The Night Harmoner sings a dark little tune and takes off towards the Sculk Castle...',
        f'effect.{NS}.deafened': 'Deafened', f'effect.{NS}.euphoria': 'Euphoria', f'effect.{NS}.entranced': 'Entranced',
        f'message.{NS}.encore.0': 'The ground shakes to a war drum... something is digging its way up!',
        # the Thumper's arena
        f'message.{NS}.thumper.emerge': 'The Thumper hauls itself out of the earth! Its shell turns every blade - man the cannons!',
        f'message.{NS}.thumper.shell': 'It clanks off the shell. Only a cannonball on its glowing vents will hurt it.',
        f'message.{NS}.thumper.closed': 'The shot rings off the shell - wait for its vents to open!',
        f'message.{NS}.thumper.miss': 'Close! Land the cannonball right on a glowing vent.',
        f'message.{NS}.thumper.vents': 'Its sculk vents are open - fire the cannons now!',
        f'message.{NS}.thumper.phase2': 'The Thumper bellows - sculk song wells up in its throat!',
        f'message.{NS}.thumper.phase3': 'The Thumper is enraged! It tears into the ground...',
        f'message.{NS}.cannon.empty': 'The cannon is empty - load it with a cannonball.',
        f'codex.{NS}.ancient_cannon.title': 'Ancient Cannon', f'codex.{NS}.ancient_cannon.tagline': 'Load, aim, fire',
        f'codex.{NS}.ancient_cannon.body': 'Bronze cannons on the towers of the Drum Pit. Use one with a Cannonball to load it, then use it again to fire: it swings round to face where you look, and the higher you look, the higher and further the ball arcs. A redstone pulse fires it along its last heading. Cannonballs are crafted from iron, gunpowder and cobbled dreamstone, four at a time.',
        f'message.{NS}.dictator.rebuild': 'Souls and song pour into the fallen Mask... the Conductor is remade!',
        f'message.{NS}.dictator.phase2': 'The Conductor rises off the stage, notes swirling about him!',
        f'message.{NS}.dictator.phase3': 'The Conductor soars above the stage - find the light when the stage glows!',
        f'message.{NS}.encore.2': 'Silk trembles in the dark... the Weaver claws its way up!',
        f'effect.{NS}.sculk_corruption': 'Sculk Corruption',
        f'message.{NS}.encore.1': 'A flute sings from the sky... the Whistler answers the call!',
        f'effect.{NS}.sculk_corruption': 'Sculk Corruption',
        f'codex.{NS}.sculk_corruption.title': 'Sculk Corruption', f'codex.{NS}.sculk_corruption.tagline': "The Conductor's curse",
        f'codex.{NS}.sculk_corruption.body': 'A slow wither: one heart of harm every few seconds - but the longer it lasts, the more the dark closes in from the edges of your sight, until the world is a pinhole. Every blow from the Conductor adds to it. Milk washes it away.',
        f'codex.{NS}.encore_sigil.title': 'Encore Sigils', f'codex.{NS}.encore_sigil.tagline': 'Where the great players wait',
        f'codex.{NS}.encore_sigil.body': 'Violet sigils in old floors. The Thumper sleeps under its Drum Pit - a sunken arena ringed by cannon towers - and the Strummer in some Deep Shrines. Walk up to one and its player comes out, once.',
        f'message.{NS}.staff.summon': 'The orchestra answers - a Sculk Harmoner sings for you!',
        f'codex.{NS}.staff.title': "Conductor's Staff", f'codex.{NS}.staff.tagline': 'The Dictator conducts no more',
        f'codex.{NS}.staff.body': 'His tuning-fork staff, taken from his hand. Use it to cast a beam of song: every creature it touches is Entranced for three seconds - it stops, forgets its target and just sings. Sneak and use it to call the orchestra: a Sculk Harmoner appears and circles you for twenty seconds, its song keeping you strong, fast, healing and hard to hurt.',
        f'biome.{NS}.sift_plains': 'Sift Plains', f'biome.{NS}.forest_mountains': 'Forest Mountains', f'biome.{NS}.rocky_dunes': 'Rocky Dunes',
        f'biome.{NS}.chrome_lakes': 'Chrome Lakes', f'biome.{NS}.deep_sift': 'Deep Sift', f'biome.{NS}.wishing_grove': 'Wishing Grove',
        f'fluid_type.{NS}.chrome': 'Chrome',
        f'item.{NS}.smithing_template.siftite_upgrade.applies_to': 'Netherite Equipment',
        f'item.{NS}.smithing_template.siftite_upgrade.ingredients': 'Siftite Ingot',
        f'item.{NS}.smithing_template.siftite_upgrade.base_slot_description': 'Add netherite armor, weapon, or tool',
        f'item.{NS}.smithing_template.siftite_upgrade.additions_slot_description': 'Add Siftite Ingot',
        f'upgrade.{NS}.siftite_upgrade': 'Siftite Upgrade',
        f'item.{NS}.music_disc_lullaby.desc': 'Sift - Lullaby of the Deep',
        f'jukebox_song.{NS}.lullaby': 'Sift - Lullaby of the Deep',
        f'message.{NS}.drum.need_sensors': 'The drum needs three Sculk Sensors within 8 blocks to listen (%s found).',
        f'message.{NS}.drum.need_core': 'The gate is listening! Slot a Warden Core into the drum to begin the ritual.',
        f'message.{NS}.drum.no_frame': 'No portal frame answers the drum. Build one of Sift Gate Frames or find an Ancient City gate.',
        f'message.{NS}.drum.listen': 'Listen... (round %s of %s)',
        f'message.{NS}.drum.your_turn': 'Your turn! Play the rhythm back on the drum (round %s of %s)',
        f'message.{NS}.drum.too_early': 'Too early! The sculk shrieks.',
        f'message.{NS}.drum.too_late': 'Too slow! The sculk shrieks.',
        f'message.{NS}.drum.round_done': 'The sculk hums in harmony... %s of %s',
        f'message.{NS}.drum.opening': 'The Sift is waking!',
        f'message.{NS}.drum.opened': 'The way to The Sift is open.',
        f'message.{NS}.drum.core_removed': 'The Warden Core slips free.',
        f'message.{NS}.drum.core_spent': 'The Warden Core is pouring itself into the gate.',
        f'message.{NS}.drum.waiting': 'The drum falls quiet. Strike it when you are ready to play.',
        f'title.{NS}.awakening': 'The Sift Awakens',
        f'title.{NS}.awakening.sub': 'The way is open',
        f'message.{NS}.altar.need_drums': 'The altar needs at least two Sift Drums around it.',
        f'message.{NS}.altar.need_levels': 'You need more experience to perform the ritual.',
    })
    # sound subtitles
    for s in SOUNDS:
        LANG[f'subtitles.{NS}.{s}'] = subtitle(s)
    items = sorted(LANG.items())
    write(os.path.join(A, 'lang', 'en_us.json'), dict(items))


# Hand-written subtitles, in the style of vanilla's ("Bulb squeaks", "Chrome splashes").
SUBTITLES = {
    'entity.dictator.ambient': 'The Dictator breathes',
    'entity.dictator.hurt': 'The Dictator hurts',
    'entity.dictator.death': 'The Dictator falls',
    'entity.dictator.roar': 'The Dictator roars',
    'entity.dictator.blink': 'The Dictator vanishes',
    'entity.dictator.summon': 'The Dictator calls his orchestra',
    'entity.dictator.crescendo': 'The Dictator builds to a crescendo',
    'entity.thumper.ambient': 'Thumper rumbles',
    'entity.thumper.hurt': 'Thumper groans',
    'entity.thumper.death': 'Thumper falls silent',
    'entity.thumper.windup': 'Thumper rears up',
    'entity.thumper.slam': 'Thumper slams',
    'entity.thumper.roar': 'Thumper bellows',
    'entity.thumper.dazed': 'Thumper is dazed',
    'entity.thumper.clank': 'Shell clanks',
    'entity.strummer.ambient': 'Weaver clicks',
    'entity.strummer.hurt': 'Weaver hurts',
    'entity.strummer.death': 'Weaver dies',
    'entity.strummer.slash': 'Scythes slash',
    'entity.strummer.spit': 'Weaver spits silk',
    'entity.strummer.hiss': 'Spider hisses',
    'entity.strummer.land': 'Spider lands',
    'entity.strummer.chord': 'Weaver strums a chord',
    'entity.strummer.draw': 'Silk tightens',
    'entity.strummer.pluck': 'Silk snaps',
    'entity.strumling.ambient': 'Sculk Spider skitters',
    'entity.strumling.hurt': 'Sculk Spider hurts',
    'entity.strumling.death': 'Sculk Spider dies',
    'entity.conductor_mask.rise': 'The Mask rises',
    'entity.conductor_mask.transform': 'The Mask transforms',
    'item.conga_drum.boom': 'Conga Drum booms',
    'item.crane_flute.play': 'Flute plays',
    'item.guitar.strum': 'Guitar strums',
    'item.conductors_baton.note': 'Baton strikes a note',
    'entity.harmoner.ambient': 'Harmoner chirps',
    'entity.harmoner.sing': 'Harmoner sings',
    'entity.harmoner.hurt': 'Harmoner hurts',
    'entity.harmoner.death': 'Harmoner dies',
    'entity.bulb.ambient': 'Bulb squeaks',
    'entity.bulb.hurt': 'Bulb hurts',
    'entity.bulb.death': 'Bulb dies',
    'entity.bulb.hop': 'Bulb boings',
    'entity.bulb.squish': 'Bulb squishes',
    'entity.bulb.happy': 'Bulb chirps happily',
    'entity.bulb.lay': 'Bulb plops out a slime ball',
    'entity.bulb.eat': 'Bulb munches',
    'entity.slumbler.ambient': 'Slumbler croaks',
    'entity.slumbler.hurt': 'Slumbler hurts',
    'entity.slumbler.death': 'Slumbler dies',
    'entity.slumbler.yawn': 'Slumbler yawns',
    'entity.slumbler.bite': 'Slumbler snaps',
    'entity.slumbler.step': 'Footsteps',
    'entity.sifter.ambient': 'Sifter chitters',
    'entity.sifter.hurt': 'Sifter hurts',
    'entity.sifter.death': 'Sifter dies',
    'entity.sifter.chomp': 'Sifter chomps',
    'entity.sifter.leap': 'Sifter bursts from the sand',
    'entity.sifter.step': 'Footsteps',
    'entity.enchoer.ambient': 'Enchoer chimes',
    'entity.enchoer.hum': 'Enchoer hums',
    'entity.enchoer.trade': 'Enchoer trades',
    'entity.enchoer.yes': 'Enchoer agrees',
    'entity.enchoer.no': 'Enchoer disagrees',
    'entity.enchoer.hurt': 'Enchoer hurts',
    'entity.enchoer.death': 'Enchoer shatters',
    'entity.riveter.ambient': 'Riveter clicks',
    'entity.riveter.scream': 'Riveter screams',
    'entity.riveter.hurt': 'Riveter hurts',
    'entity.riveter.death': 'Riveter dies',
    'block.sift_drum.low': 'Sift Drum thumps',
    'block.sift_drum.mid': 'Sift Drum beats',
    'block.sift_drum.high': 'Sift Drum taps',
    'block.sift_drum.boom': 'Sift Drum booms',
    'block.euphory_altar.charge': 'Euphory Altar charges',
    'block.euphory_altar.enchant': 'Euphory Altar enchants',
    'block.euphory_altar.hum': 'Euphory Altar hums',
    'block.sift_portal.ambient': 'Sift Portal whispers',
    'block.sift_portal.activate': 'Sift Portal awakens',
    'block.sift_portal.travel': 'Sift Portal noise fades',
    'event.rhythm.call': 'Sculk calls a rhythm',
    'event.rhythm.good': 'Rhythm matches',
    'event.rhythm.fail': 'Sculk shrieks',
    'event.rhythm.round': 'Sculk hums in harmony',
    'event.gate.swell': 'The gate hums awake',
    'block.chrome.ambient': 'Chrome shimmers',
    'block.chrome.splash': 'Chrome splashes',
    'block.harmony_stone.tone': 'Harmony Stone rings',
    'block.harmony_seal.unlock': 'Harmony Seal unlocks',
    'block.soul_chime.ring': 'Soul Chime rings',
    'block.choir_lily.sing': 'Choir Lily sings',
    'block.dream_snare.trigger': 'Dream Snare springs',
    'block.crumbling_dreamstone.crumble': 'Dreamstone crumbles',
    'item.slingshot.shoot': 'Slingshot fires',
    'item.slingshot.pull': 'Slingshot stretches',
    'entity.glowball.burst': 'Glowball bursts',
    'item.warden_core.pulse': 'Warden Core throbs',
    'ambient.sift.loop': 'The Sift breathes',
    'ambient.sift.additions': 'Distant chimes',
    'ambient.sift.mood': 'Dreamlike murmurs',
    'ambient.deep_sift.loop': 'The Deep Sift rumbles',
}


def subtitle(s):
    if s in SUBTITLES:
        return SUBTITLES[s]
    parts = s.split('.')
    who = spec.title(parts[1]) if len(parts) > 2 else spec.title(parts[0])
    what = parts[-1].replace('_', ' ')
    return f'{who} {what}s' if not what.endswith('s') else f'{who} {what}'


# --------------------------------------------------------------------------- sounds

# event -> list of (vanilla sound, volume, pitch)
SOUNDS = {
    'entity.dictator.ambient': [('event:entity.warden.ambient', 0.8, 1.5), ('event:entity.warden.listening', 0.8, 1.6)],
    'entity.dictator.hurt': [('event:entity.warden.hurt', 1.0, 1.4)],
    'entity.dictator.death': [('event:entity.warden.death', 1.2, 1.3)],
    'entity.dictator.roar': [('event:entity.warden.roar', 1.2, 1.2)],
    'entity.dictator.blink': [('event:entity.enderman.teleport', 1.0, 0.6)],
    'entity.dictator.summon': [('event:entity.evoker.prepare_summon', 1.0, 0.7)],
    'entity.dictator.crescendo': [('event:entity.warden.sonic_charge', 1.0, 0.8)],
    'entity.thumper.ambient': [('event:entity.turtle.ambient_land', 1.0, 0.4), ('event:entity.ravager.ambient', 0.6, 1.4)],
    'entity.thumper.hurt': [('event:entity.turtle.hurt', 1.0, 0.4), ('block/note_block/basedrum', 1.0, 0.6)],
    'entity.thumper.death': [('event:entity.turtle.death', 1.2, 0.35), ('event:entity.ravager.death', 0.8, 1.2)],
    'entity.thumper.windup': [('event:entity.ravager.roar', 0.7, 1.3)],
    'entity.thumper.slam': [('event:entity.warden.attack_impact', 1.0, 0.6), ('event:entity.generic.explode', 0.4, 1.4)],
    'entity.thumper.roar': [('event:entity.ravager.roar', 1.0, 0.9)],
    'entity.thumper.dazed': [('event:entity.ravager.stunned', 1.0, 0.9)],
    'entity.thumper.clank': [('event:block.anvil.land', 0.5, 1.8), ('event:item.shield.block', 0.8, 0.8)],
    'entity.strummer.ambient': [('event:entity.spider.ambient', 0.8, 0.6), ('block/note_block/guitar', 0.5, 0.8)],
    'entity.strummer.hurt': [('event:entity.spider.hurt', 1.0, 0.6), ('block/note_block/guitar', 0.6, 0.5)],
    'entity.strummer.death': [('event:entity.spider.death', 1.2, 0.5), ('block/note_block/guitar', 1.0, 0.5)],
    'entity.strummer.slash': [('event:entity.player.attack.sweep', 1.0, 1.2)],
    'entity.strummer.spit': [('event:entity.llama.spit', 1.0, 0.7)],
    'entity.strummer.hiss': [('event:entity.spider.ambient', 1.0, 1.3)],
    'entity.strummer.land': [('event:entity.spider.step', 1.0, 0.5), ('event:entity.warden.attack_impact', 0.5, 1.5)],
    'entity.strummer.chord': [('block/note_block/guitar', 1.0, 1.0), ('block/note_block/harp', 0.8, 1.0), ('block/note_block/bell', 0.6, 1.0)],
    'entity.strummer.draw': [('event:item.crossbow.loading_middle', 1.0, 0.7)],
    'entity.strummer.pluck': [('block/note_block/guitar', 1.0, 1.3), ('event:item.crossbow.shoot', 0.7, 1.2)],
    'entity.strumling.ambient': [('event:entity.spider.ambient', 0.7, 1.15), ('event:entity.warden.ambient', 0.25, 1.9)],
    'entity.strumling.hurt': [('event:entity.spider.hurt', 0.8, 1.1)],
    'entity.strumling.death': [('event:entity.spider.death', 0.9, 1.0), ('block/sculk/break1', 0.6, 0.8)],
    'entity.conductor_mask.rise': [('event:block.beacon.activate', 1.0, 0.5), ('event:entity.warden.emerge', 0.8, 1.4)],
    'entity.conductor_mask.transform': [('event:entity.warden.sonic_boom', 1.0, 0.6), ('event:entity.wither.spawn', 0.6, 1.6)],
    'item.conga_drum.boom': [('block/note_block/basedrum', 1.0, 0.5), ('event:entity.generic.explode', 0.7, 0.8)],
    'item.crane_flute.play': [('block/note_block/flute', 1.0, 1.0)],
    'item.guitar.strum': [('block/note_block/guitar', 1.0, 1.0), ('block/note_block/harp', 0.7, 1.0)],
    'item.conductors_baton.note': [('event:entity.warden.sonic_boom', 0.6, 1.8), ('block/note_block/bell', 1.0, 1.0)],
    'entity.harmoner.ambient': [('mob/parrot/idle1', 0.7, 1.3), ('mob/parrot/idle2', 0.7, 1.4), ('mob/parrot/idle3', 0.7, 1.2)],
    'entity.harmoner.sing': [('block/note_block/flute', 0.9, 1.0)],
    'entity.harmoner.hurt': [('mob/parrot/hurt1', 0.8, 1.3), ('mob/parrot/hurt2', 0.8, 1.3)],
    'entity.harmoner.death': [('mob/parrot/death1', 0.9, 1.2), ('mob/parrot/death2', 0.9, 1.2)],
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
    'entity.sifter.leap': [('event:block.sand.break', 1.0, 0.8), ('event:block.sand.break', 1.0, 0.7)],
    'entity.sifter.step': [('mob/silverfish/step1', 0.4, 1.2), ('mob/silverfish/step2', 0.4, 1.2)],
    'entity.enchoer.ambient': [('mob/allay/idle_without_item1', 0.8, 0.6), ('mob/allay/idle_without_item2', 0.8, 0.55), ('block/amethyst/resonate1', 0.8, 0.7)],
    'entity.enchoer.hum': [('block/amethyst/resonate2', 1.0, 0.6), ('block/amethyst/resonate3', 1.0, 0.7), ('mob/allay/item_given1', 0.8, 0.5)],
    'entity.enchoer.trade': [('mob/allay/idle_with_item1', 0.8, 0.6), ('mob/allay/idle_with_item2', 0.8, 0.6)],
    'entity.enchoer.yes': [('mob/allay/item_given2', 0.9, 0.7), ('block/amethyst/shimmer', 1.0, 1.0)],
    'entity.enchoer.no': [('mob/allay/item_taken1', 0.9, 0.5)],
    'entity.enchoer.hurt': [('mob/allay/hurt1', 1.0, 0.6), ('block/amethyst_cluster/break1', 1.0, 0.8)],
    'entity.enchoer.death': [('mob/allay/death1', 1.0, 0.55), ('block/amethyst_cluster/break2', 1.0, 0.6)],
    'entity.riveter.ambient': [('mob/bat/idle1', 0.5, 0.5), ('mob/bat/idle2', 0.5, 0.5), ('block/sculk/spread1', 0.6, 0.7)],
    'entity.riveter.scream': [('event:entity.warden.roar', 0.8, 1.8), ('block/sculk_shrieker/shriek1', 1.0, 1.1), ('block/sculk_shrieker/shriek2', 1.0, 1.1)],
    'entity.riveter.hurt': [('mob/bat/hurt1', 0.8, 0.6), ('mob/bat/hurt2', 0.8, 0.6)],
    'entity.riveter.death': [('mob/bat/death', 1.0, 0.5), ('block/sculk/break1', 1.0, 0.8)],
    'block.sift_drum.low': [('block/note_block/basedrum', 1.0, 0.7)],
    'block.sift_drum.mid': [('block/note_block/basedrum', 1.0, 1.0), ('block/note_block/hat', 0.3, 0.8)],
    'block.sift_drum.high': [('block/note_block/snare', 1.0, 1.0)],
    'block.sift_drum.boom': [('block/note_block/basedrum', 1.0, 0.5), ('random/explode1', 0.3, 1.8)],
    'block.euphory_altar.charge': [('block/beacon/activate', 1.0, 1.4), ('block/amethyst/resonate1', 1.0, 1.2)],
    'block.euphory_altar.enchant': [('block/enchantment_table/enchant1', 1.0, 0.8), ('block/beacon/power1', 1.0, 1.5)],
    'block.euphory_altar.hum': [('block/beacon/ambient', 0.8, 1.2), ('block/amethyst/resonate2', 0.6, 1.3)],
    'block.sift_portal.ambient': [('event:block.portal.ambient', 0.4, 1.6), ('block/amethyst/resonate3', 0.3, 1.4)],
    'block.sift_portal.activate': [('block/end_portal/endportal', 1.0, 1.2), ('block/beacon/activate', 1.0, 0.8)],
    'block.sift_portal.travel': [('event:block.portal.travel', 0.8, 1.5)],
    'event.rhythm.call': [('block/sculk_sensor/sculk_clicking1', 1.0, 1.0), ('block/sculk_sensor/sculk_clicking2', 1.0, 1.1)],
    'event.rhythm.good': [('block/note_block/chime', 1.0, 1.2), ('block/amethyst/shimmer', 1.0, 1.0)],
    'event.rhythm.fail': [('block/sculk_shrieker/shriek1', 1.0, 0.8)],
    'event.rhythm.round': [('block/note_block/bell', 1.0, 1.0), ('block/note_block/bell', 1.0, 1.26)],
    # the rising whoosh under the gate's awakening
    'event.gate.swell': [('event:block.portal.trigger', 0.7, 1.2)],
    'block.chrome.ambient': [('liquid/water', 0.3, 1.6), ('block/amethyst/resonate1', 0.2, 1.8)],
    'block.chrome.splash': [('liquid/splash', 0.8, 1.4), ('liquid/splash2', 0.8, 1.5)],
    'block.harmony_stone.tone': [('block/note_block/chime', 1.0, 1.0)],
    'block.harmony_seal.unlock': [('block/trial_spawner/ominous_activate', 1.0, 1.4), ('block/beacon/deactivate', 1.0, 1.5)],
    'block.soul_chime.ring': [('block/bell/resonate', 0.7, 1.6), ('block/note_block/chime', 0.8, 1.2), ('block/note_block/chime', 0.8, 1.5)],
    'block.choir_lily.sing': [('block/note_block/flute', 0.8, 1.0), ('block/amethyst/resonate2', 0.6, 1.4)],
    'block.dream_snare.trigger': [('event:block.tripwire.click_on', 1.0, 0.6), ('event:block.sculk_catalyst.bloom', 1.0, 1.4)],
    'block.crumbling_dreamstone.crumble': [('block/pointed_dripstone/drip_lava1', 1.0, 0.6), ('dig/stone1', 1.0, 0.7)],
    'item.slingshot.shoot': [('random/bow', 1.0, 1.4), ('mob/slime/small1', 0.8, 1.6)],
    'item.slingshot.pull': [('item/crossbow/loading_start', 0.8, 1.5)],
    'entity.glowball.burst': [('block/amethyst/break1', 1.0, 1.3), ('mob/slime/big2', 0.8, 1.5), ('event:entity.firework_rocket.twinkle', 0.6, 1.4)],
    'item.warden_core.pulse': [('event:entity.warden.heartbeat', 0.6, 1.0)],
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


# --------------------------------------------------------------------------- the wild creatures (Stomper, music fish, Sky Whale)
SOUNDS.update({
    'entity.stomper.ambient': [('mob/frog/idle1', 1.0, 0.45), ('mob/frog/idle4', 1.0, 0.4), ('mob/sniffer/idle3', 0.9, 0.6), ('mob/frog/idle7', 1.0, 0.42)],
    'entity.stomper.hurt': [('mob/ravager/hurt1', 1.0, 0.8), ('mob/frog/hurt2', 1.0, 0.45), ('mob/ravager/hurt3', 1.0, 0.75)],
    'entity.stomper.death': [('mob/ravager/death1', 1.0, 0.7), ('mob/frog/death1', 1.0, 0.4)],
    'entity.stomper.step': [('mob/ravager/step1', 0.8, 0.7), ('mob/ravager/step2', 0.8, 0.7), ('mob/camel/step1', 0.8, 0.6), ('mob/ravager/step4', 0.8, 0.65)],
    'entity.stomper.trumpet': [('item/goat_horn/call3', 0.9, 0.75), ('item/goat_horn/call6', 0.9, 0.7), ('block/note_block/trumpet', 1.0, 0.6)],
    'entity.stomper.spray': [('liquid/splash', 1.0, 0.7), ('liquid/splash2', 1.0, 0.65), ('mob/dolphin/blowhole1', 1.0, 0.6)],
    'entity.stomper.drink': [('random/drink', 1.0, 0.5), ('liquid/swim3', 0.8, 0.6), ('random/drink', 1.0, 0.45)],
    'entity.stomper.stomp': [('random/explode2', 0.6, 0.5), ('block/note_block/basedrum', 1.0, 0.4), ('mob/ravager/stun1', 0.8, 0.6)],
    'entity.stomper.puff': [('mob/dolphin/blowhole1', 0.8, 0.8), ('mob/dolphin/blowhole2', 0.8, 0.7)],
    'entity.stomper.happy': [('mob/sniffer/happy1', 1.0, 0.6), ('mob/frog/idle6', 1.0, 0.6), ('mob/sniffer/happy3', 1.0, 0.65)],
    'entity.stomper.lay': [('mob/turtle/egg/drop_egg1', 1.0, 0.7), ('mob/turtle/egg/drop_egg2', 1.0, 0.7)],
    'entity.stomper.hatch': [('mob/turtle/egg/egg_crack1', 1.0, 0.8), ('mob/turtle/baby/egg_hatched1', 1.0, 0.8), ('mob/turtle/egg/egg_break1', 1.0, 0.8)],
    'entity.fanfare_eel.ambient': [('mob/guardian/guardian_idle1', 0.6, 1.4), ('block/note_block/trumpet', 0.4, 1.3), ('mob/guardian/guardian_idle3', 0.6, 1.5)],
    'entity.fanfare_eel.blast': [('block/note_block/trumpet', 1.0, 0.8), ('block/note_block/trumpet_exposed', 1.0, 0.9), ('item/goat_horn/call0', 0.6, 1.5)],
    'entity.fanfare_eel.hurt': [('mob/guardian/guardian_hit1', 0.8, 1.3), ('mob/guardian/guardian_hit2', 0.8, 1.3)],
    'entity.fanfare_eel.death': [('mob/guardian/guardian_death', 0.9, 1.3), ('block/note_block/trumpet_weathered', 0.8, 0.5)],
    'entity.fanfare_eel.flop': [('mob/guardian/flop1', 0.8, 1.3), ('mob/guardian/flop2', 0.8, 1.3)],
    'entity.kazoo_fish.ambient': [('block/note_block/didgeridoo', 0.35, 1.9), ('block/note_block/bit', 0.25, 1.4), ('block/note_block/didgeridoo', 0.35, 1.7)],
    'entity.kazoo_fish.hurt': [('mob/pufferfish/hurt1', 0.8, 1.6), ('block/note_block/didgeridoo', 0.4, 2.0)],
    'entity.kazoo_fish.death': [('mob/pufferfish/death1', 0.8, 1.6), ('mob/pufferfish/death2', 0.8, 1.6)],
    'entity.kazoo_fish.flop': [('mob/pufferfish/flop1', 0.6, 1.4), ('mob/pufferfish/flop2', 0.6, 1.4)],
    'entity.tubafish.ambient': [('block/note_block/bass', 0.6, 0.5), ('block/note_block/didgeridoo', 0.5, 0.5), ('block/bubble_column/bubble1', 0.5, 0.6)],
    'entity.tubafish.puff': [('mob/pufferfish/blow_up1', 1.0, 0.6), ('block/note_block/didgeridoo', 1.0, 0.5), ('mob/pufferfish/blow_up2', 1.0, 0.55)],
    'entity.tubafish.deflate': [('mob/pufferfish/blow_out1', 1.0, 0.6), ('mob/pufferfish/blow_out2', 1.0, 0.6)],
    'entity.tubafish.hurt': [('mob/pufferfish/hurt1', 1.0, 0.6), ('mob/pufferfish/hurt2', 1.0, 0.6)],
    'entity.tubafish.death': [('mob/pufferfish/death1', 1.0, 0.55), ('block/note_block/bass', 1.0, 0.5)],
    'entity.tubafish.flop': [('mob/pufferfish/flop3', 0.8, 0.6), ('mob/pufferfish/flop4', 0.8, 0.6)],
    'item.bubble_gun.shoot': [('block/bubble_column/bubble1', 1.0, 1.2), ('mob/pufferfish/blow_out2', 0.6, 1.6), ('block/bubble_column/bubble3', 1.0, 1.3)],
    'entity.bubble.pop': [('block/bubble_column/bubble2', 1.0, 1.4), ('random/pop', 0.8, 1.2)],
    'entity.sky_whale.ambient': [('item/goat_horn/call1', 0.9, 0.5), ('item/goat_horn/call4', 0.9, 0.45), ('mob/happy_ghast/ambient3', 1.0, 0.5)],
    'entity.sky_whale.song': [('item/goat_horn/call2', 1.2, 0.55), ('item/goat_horn/call5', 1.2, 0.5), ('item/goat_horn/call7', 1.2, 0.5)],
    'entity.sky_whale.moo': [('mob/cow/say2', 1.0, 0.35), ('mob/cow/say3', 1.0, 0.38), ('mob/cow/say1', 1.0, 0.33)],
    'entity.sky_whale.hurt': [('mob/cow/hurt1', 1.0, 0.4), ('mob/happy_ghast/hurt1', 1.0, 0.6)],
    'entity.sky_whale.death': [('mob/happy_ghast/death', 1.0, 0.5), ('mob/cow/hurt3', 1.0, 0.3)],
    'entity.sky_whale.spit': [('random/pop', 1.0, 0.5), ('block/amethyst/shimmer', 1.0, 1.0), ('random/levelup', 0.6, 1.6)],
    'entity.sky_whale.flap': [('mob/phantom/flap1', 0.8, 0.4), ('mob/phantom/flap2', 0.8, 0.4)],
})
SUBTITLES.update({
    'entity.stomper.ambient': 'Stomper rumbles', 'entity.stomper.hurt': 'Stomper hurts', 'entity.stomper.death': 'Stomper dies',
    'entity.stomper.step': 'Heavy footsteps', 'entity.stomper.trumpet': 'Stomper trumpets', 'entity.stomper.spray': 'Stomper sprays Chrome',
    'entity.stomper.drink': 'Stomper slurps', 'entity.stomper.stomp': 'Stomper stomps', 'entity.stomper.puff': 'Stomper puffs',
    'entity.stomper.happy': 'Stomper croaks happily', 'entity.stomper.lay': 'Stomper lays an egg', 'entity.stomper.hatch': 'Stomper Egg hatches',
    'entity.fanfare_eel.ambient': 'Fanfare Eel toots', 'entity.fanfare_eel.blast': 'Fanfare Eel blasts', 'entity.fanfare_eel.hurt': 'Fanfare Eel hurts',
    'entity.fanfare_eel.death': 'Fanfare Eel dies', 'entity.fanfare_eel.flop': 'Fanfare Eel flops',
    'entity.kazoo_fish.ambient': 'Kazoo Fish buzzes', 'entity.kazoo_fish.hurt': 'Kazoo Fish hurts', 'entity.kazoo_fish.death': 'Kazoo Fish dies',
    'entity.kazoo_fish.flop': 'Kazoo Fish flops',
    'entity.tubafish.ambient': 'Tubafish burbles', 'entity.tubafish.puff': 'Tubafish blasts and swells', 'entity.tubafish.deflate': 'Tubafish deflates',
    'entity.tubafish.hurt': 'Tubafish hurts', 'entity.tubafish.death': 'Tubafish dies', 'entity.tubafish.flop': 'Tubafish flops',
    'item.bubble_gun.shoot': 'Bubble Gun blows', 'entity.bubble.pop': 'Bubble pops',
    'entity.sky_whale.ambient': 'Sky Whale sings', 'entity.sky_whale.song': 'Sky Whale answers', 'entity.sky_whale.moo': 'Sky Whale moos',
    'entity.sky_whale.hurt': 'Sky Whale hurts', 'entity.sky_whale.death': 'Sky Whale dies', 'entity.sky_whale.spit': 'Sky Whale spits out a gem',
    'entity.sky_whale.flap': 'Sky Whale flaps',
})


def gen_wild_creatures():
    """Recipes, tags and text for the Stomper, the music fish and the Sky Whale."""
    # cooking
    smelt('stomper_steak', 'stomper_meat', 'stomper_steak', 0.35, 200, ('smelting', 'smoking'))
    smelt('stomper_steak_campfire', 'stomper_meat', 'stomper_steak', 0.35, 1200, ('campfire_cooking',))
    smelt('cooked_kazoo_fish', 'kazoo_fish', 'cooked_kazoo_fish', 0.35, 200, ('smelting', 'smoking'))
    smelt('cooked_kazoo_fish_campfire', 'kazoo_fish', 'cooked_kazoo_fish', 0.35, 1200, ('campfire_cooking',))
    # gear
    shaped('bubble_gun', [' TT', 'GCT', 'G  '], {'T': 'tuba_bubble', 'C': 'minecraft:copper_ingot', 'G': 'minecraft:gold_ingot'}, 'bubble_gun', 1,
           'equipment')
    shaped('enchanted_golden_apple_from_skysong_gem', ['GGG', 'GAG', 'GSG'], {'G': 'minecraft:gold_block', 'A': 'minecraft:apple', 'S': 'skysong_gem'},
           'minecraft:enchanted_golden_apple', 1, 'misc')
    shapeless('hummingbloom_dye', ['hummingbloom'], 'minecraft:purple_dye', 1, 'misc', 'dye')
    # tags
    tag('item', 'minecraft:meat', rl('stomper_meat'))
    tag('item', 'minecraft:meat', rl('stomper_steak'))
    tag('item', 'minecraft:fishes', rl('kazoo_fish'))
    tag('item', 'minecraft:fishes', rl('cooked_kazoo_fish'))
    tag('item', f'{NS}:slumbler_food', rl('kazoo_fish'))
    tag('block', 'minecraft:bee_attractive', rl('hummingbloom'))
    tag('block', f'{NS}:resonant', rl('hummingbloom'))
    for e in ['stomper', 'sky_whale']:
        tag('entity_type', f'{NS}:music_lovers', rl(e))
    tag('entity_type', 'minecraft:fall_damage_immune', rl('sky_whale'))
    for e in ['fanfare_eel', 'kazoo_fish', 'tubafish']:
        tag('entity_type', 'minecraft:aquatic', rl(e))
        tag('entity_type', f'{NS}:chrome_dwellers', rl(e))
    # names, messages and the Codex
    LANG.update({
        f'entity.{NS}.stomper': 'Stomper', f'entity.{NS}.fanfare_eel': 'Fanfare Eel', f'entity.{NS}.kazoo_fish': 'Kazoo Fish',
        f'entity.{NS}.tubafish': 'Tubafish', f'entity.{NS}.sky_whale': 'Sky Whale', f'entity.{NS}.bubble': 'Bubble',
        f'message.{NS}.stomper.tamed': 'The little Stomper trumpets and nuzzles you with its trunk. It is yours now!',
        f'message.{NS}.sky_whale.heard': 'Far above, something vast hears your song...',
        f'message.{NS}.sky_whale.gem': 'The Sky Whale sings back - and spits out a glittering Skysong Gem!',
        f'message.{NS}.sky_whale.no_gem': 'The Sky Whale sings back warmly. It has no gem left to give today.',
        f'codex.{NS}.stomper.title': 'Stomper', f'codex.{NS}.stomper.tagline': 'Mammoth, bullfrog, both',
        f'codex.{NS}.stomper.body': 'Four eyes, one trunk, no tusks - and a whole garden of moss and coral on its back. Stompers slurp Chrome through their trunks and hose any monster (or you, if you hit one) with it. Drums make them dance, ending in two stomps that send monsters flying. Feed two Hummingblooms for an egg and tame the baby with them: you can ride it straight away (jump to hop, attack to stomp monsters away), and it drums on the little drum on its back for you, which puts a spring in your step.',
        f'codex.{NS}.sky_whale.title': 'Sky Whale', f'codex.{NS}.sky_whale.tagline': 'Rare - the singer in the clouds',
        f'codex.{NS}.sky_whale.body': "A shaggy whale-bull with cloud-soft fur, a meadow of blooms on its back and glowbell vines trailing below, rowing through the sky on furry flippers. If you spot one, you're lucky. Play the crane flute up at the sky with a tamed Harmoner beside you and the whale glides down, sings to you and spits out a Skysong Gem - one a day per whale.",
        f'codex.{NS}.fanfare_eel.title': 'Fanfare Eel', f'codex.{NS}.fanfare_eel.tagline': 'Hostile - brass with teeth',
        f'codex.{NS}.fanfare_eel.body': 'A long sculk eel with pale bone ribs, a glowing line down its flank and a golden trumpet bell for a mouth, ringed with glowing fins. It hunts anything swimming in the Chrome lakes - fish and visitors alike - and every bite comes with a blast of sound. Fight it from the shore if you can.',
        f'codex.{NS}.kazoo_fish.title': 'Kazoo Fish', f'codex.{NS}.kazoo_fish.tagline': 'Small, silly, delicious',
        f'codex.{NS}.kazoo_fish.body': 'Teal schooling fish with coral fins, a pink kazoo for a nose, a tuft of moss on top and eyes that never quite agree. Schools follow a leader and buzz little tunes; scare one and they all scatter. Cook them for a decent meal.',
        f'codex.{NS}.tubafish.title': 'Tubafish', f'codex.{NS}.tubafish.tagline': 'Do not poke the tuba',
        f'codex.{NS}.tubafish.body': 'A huge, round periwinkle pufferfish freckled with glowing spots, with a tuba bell on its back crowned by a little waving anemone. Get too close and it blasts a low note, swells up with its coral spikes out and blows a storm of bubbles - and touching it then stings. Drops Tuba Bubbles, which make a Bubble Gun.',
        f'codex.{NS}.bubble_gun.title': 'Bubble Gun', f'codex.{NS}.bubble_gun.tagline': 'Up you go!',
        f'codex.{NS}.bubble_gun.body': 'Built from Tuba Bubbles, copper and gold. Each squeeze blows a big wobbly bubble that pops on whatever it hits, stinging a little and lifting it gently into the air. No ammo needed, just a breath between shots.',
        f'codex.{NS}.skysong_gem.title': 'Skysong Gem', f'codex.{NS}.skysong_gem.tagline': 'A gift from the clouds',
        f'codex.{NS}.skysong_gem.body': 'Spat out by a Sky Whale that answered your song. Set one below an apple in a ring of gold blocks to craft an Enchanted Golden Apple.',
    })


def vanilla_sound_ref(name):
    """('event', id) or ('file', path) for one vanilla sound in the SOUNDS table."""
    if name.startswith('event:'):
        return 'event', name[6:]
    if name.startswith('block/note_block/'):
        # note block sounds live under note/<short name>.ogg; gen_sounds refers to the vanilla events instead
        return 'event', 'block.note_block.' + name.rsplit('/', 1)[1]
    return 'file', name


def used_vanilla_sounds():
    """(events, files): every vanilla sound event and sound file the SOUNDS table points at."""
    events, files = set(), set()
    for lst in SOUNDS.values():
        for name, _vol, _pitch in lst:
            kind, ref = vanilla_sound_ref(name)
            (events if kind == 'event' else files).add(ref)
    return events, files


def check_sounds():
    """Fail the build rather than ship a silent sound.

    A sounds.json entry naming a vanilla .ogg that does not exist in 26.3, or a misspelled vanilla
    event, plays as silence without a word in the log. Every vanilla name the table uses must be in
    the known-good lists of tools/vanilla_sounds.py (checked against the real sounds.json, which is
    also consulted directly when the vanilla assets are around), and every sound event ModSounds
    registers needs an entry here (and the other way round)."""
    import vanilla_sounds
    problems = []
    real = None
    full = os.path.join(VA, 'sounds.json')
    if os.path.exists(full):
        real = vanilla_sounds.vanilla_names(full)
    for ev, lst in SOUNDS.items():
        for name, _vol, _pitch in lst:
            kind, ref = vanilla_sound_ref(name)
            known = vanilla_sounds.EVENTS if kind == 'event' else vanilla_sounds.FILES
            if ref not in known:
                problems.append(f'{ev}: vanilla {kind} {ref!r} is not in tools/vanilla_sounds.py')
            if real is not None and ref not in (real[0] if kind == 'event' else real[1]):
                problems.append(f'{ev}: vanilla {kind} {ref!r} does not exist in 26.3 (silent!)')
    java = os.path.join(ROOT, 'src/main/java/com/thesift/registry/ModSounds.java')
    if os.path.exists(java):
        with open(java) as f:
            registered = set(re.findall(r'\breg\("([^"]+)"\)', f.read()))
        for ev in sorted(registered - set(SOUNDS)):
            problems.append(f'{ev}: registered in ModSounds but has no sounds.json entry (silent!)')
        for ev in sorted(set(SOUNDS) - registered):
            problems.append(f'{ev}: has a sounds.json entry but ModSounds never registers it')
    if problems:
        sys.exit('sounds.json check failed:\n  ' + '\n  '.join(problems)
                 + '\nCheck the names against the vanilla 26.3 sounds.json, then rebuild the lists with tools/vanilla_sounds.py.')


def gen_sounds():
    check_sounds()
    out = {}
    for ev, lst in SOUNDS.items():
        entries = []
        for name, vol, pitch in lst:
            kind, ref = vanilla_sound_ref(name)
            e = {'name': f'minecraft:{ref}', 'volume': vol, 'pitch': pitch}
            if kind == 'event':
                # a vanilla sound event, used as-is
                e = {'name': f'minecraft:{ref}', 'type': 'event', 'volume': vol, 'pitch': pitch}
            if ev in STREAM:
                e['stream'] = True
            entries.append(e)
        out[ev] = {'sounds': entries, 'subtitle': f'subtitles.{NS}.{ev}'}
        if ev.startswith('music'):
            del out[ev]['subtitle']
    write(os.path.join(A, 'sounds.json'), out)


# --------------------------------------------------------------------------- misc tags & data


# --------------------------------------------------------------------------- the Sculk Parasite
SOUNDS.update({
    'entity.sculk_parasite.ambient': [('mob/silverfish/say1', 0.6, 0.7), ('mob/silverfish/say3', 0.6, 0.65), ('mob/warden/tendril_clicks_2', 0.5, 1.6),
                                      ('block/sculk_sensor/sculk_clicking3', 0.5, 1.5)],
    'entity.sculk_parasite.hurt': [('mob/silverfish/hit1', 0.8, 0.7), ('mob/silverfish/hit2', 0.8, 0.75), ('block/sculk/break2', 0.6, 1.4)],
    'entity.sculk_parasite.death': [('mob/silverfish/kill', 0.9, 0.6), ('block/sculk/break3', 1.0, 1.2)],
    'entity.sculk_parasite.step': [('mob/silverfish/step1', 0.25, 1.4), ('mob/silverfish/step3', 0.25, 1.5), ('block/sculk/step2', 0.3, 1.6)],
    'entity.sculk_parasite.hiss': [('mob/warden/listening_angry_2', 0.6, 1.9), ('block/sculk_sensor/sculk_clicking5', 0.8, 1.7),
                                   ('mob/warden/tendril_clicks_4', 0.7, 1.8)],
    'entity.sculk_parasite.burst': [('block/sculk/charge2', 1.0, 1.3), ('block/sculk_catalyst/break3', 1.0, 1.1), ('mob/silverfish/kill', 0.7, 0.5)],
})
SUBTITLES.update({
    'entity.sculk_parasite.ambient': 'Sculk Parasite clicks', 'entity.sculk_parasite.hurt': 'Sculk Parasite hurts',
    'entity.sculk_parasite.death': 'Sculk Parasite dies', 'entity.sculk_parasite.step': 'Something skitters',
    'entity.sculk_parasite.hiss': 'Sculk Parasite coils to strike', 'entity.sculk_parasite.burst': 'Sculk Parasite bursts',
})


def gen_parasite():
    """Name and Codex page of the Sculk Parasite."""
    LANG.update({
        f'entity.{NS}.sculk_parasite': 'Sculk Parasite',
        f'codex.{NS}.sculk_parasite.title': 'Sculk Parasite', f'codex.{NS}.sculk_parasite.tagline': 'One bite, then it pops',
        f'codex.{NS}.sculk_parasite.body': "A Warden-kin centipede the size of your arm: bone plates, a glowing sting and far too many legs. It skitters up walls, coils and lunges - and the moment it bites, it bursts, leaving Sculk Corruption II in you. Every extra parasite that gets you deepens it a level. Squash them before they reach you: they only take a couple of hits.",
    })


def gen_misc_tags():
    for b in ['sift_grass_block', 'coral_turf', 'sift_soil', 'lumen_moss_block']:
        tag('block', f'{NS}:sift_plantable', rl(b))
        tag('block', 'minecraft:sniffer_diggable_block', rl(b))
    tag('block', f'{NS}:sift_plantable', '#minecraft:dirt')
    tag('block', f'{NS}:sift_plantable', rl('dreamsand'))
    tag('block', f'{NS}:portal_frame', 'minecraft:reinforced_deepslate')
    for b in ['hushslate', 'cobbled_hushslate', 'minecraft:sculk', 'minecraft:deepslate', 'lumen_moss_block']:
        tag('block', f'{NS}:deep_sift_ground', rl(b))
    for b in ['hushslate', 'cobbled_hushslate', 'hushslate_bricks', 'minecraft:sculk', 'minecraft:deepslate', 'minecraft:reinforced_deepslate',
              'minecraft:deepslate_bricks', 'minecraft:deepslate_tiles']:
        tag('block', f'{NS}:riveter_roost', rl(b))
    tag('block', f'{NS}:incorrect_for_siftite_tool', '#minecraft:incorrect_for_netherite_tool')
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
    tag('block', 'minecraft:animals_spawnable_on', rl('coral_turf'))
    tag('block', 'minecraft:sniffer_egg_hatch_boost', rl('lumen_moss_block'))
    tag('block', 'minecraft:mineable/hoe', rl('hanging_lullwood_leaves'))
    tag('block', 'minecraft:mineable/axe', rl('sift_drum'))
    tag('block', 'minecraft:dampens_vibrations', rl('lumen_moss_carpet'))
    tag('block', 'minecraft:inside_step_sound_blocks', rl('drift_petals'))
    # items
    tag('item', f'{NS}:bulb_food', rl('pitcher_bulb'))
    for seed in ('echo_seed', 'choir_pod', 'minecraft:wheat_seeds', 'minecraft:melon_seeds', 'minecraft:pumpkin_seeds', 'minecraft:beetroot_seeds',
                 'minecraft:torchflower_seeds', 'minecraft:pitcher_pod'):
        tag('item', f'{NS}:harmoner_food', rl(seed))
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
    for b in ['#minecraft:dirt', '#minecraft:sand', 'minecraft:gravel', 'minecraft:clay', 'minecraft:mud', 'minecraft:moss_block', 'minecraft:snow_block',
              'minecraft:suspicious_sand', 'minecraft:suspicious_gravel', 'dreamsand', 'suspicious_dreamsand', 'sift_soil', 'sift_grass_block',
              'coral_turf', 'lumen_moss_block']:
        tag('block', f'{NS}:sniffer_mineable', rl(b))
    for b in ['minecraft:suspicious_sand', 'minecraft:suspicious_gravel', 'suspicious_dreamsand', 'serbim_ore', 'deep_serbim_ore', 'minecraft:chest',
              'minecraft:barrel', 'minecraft:decorated_pot', 'minecraft:diamond_ore', 'minecraft:deepslate_diamond_ore', 'minecraft:emerald_ore']:
        tag('block', f'{NS}:sniffer_treasure', rl(b))
    tag('item', 'minecraft:frog_food', rl('glowing_slime_ball')) if False else None
    # entity types
    tag('entity_type', f'{NS}:chrome_dwellers', rl('slumbler'))
    # a tamed Sift Sniffer takes a saddle (26.3 checks this tag before equipping one)
    for e in ['bulb', 'enchoer', 'harmoner', 'minecraft:allay', 'minecraft:sniffer', 'slumbler']:
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
    for b in ('sift_grass_block', 'coral_turf', 'sift_soil'):
        tag('block', 'minecraft:turns_into_farmland', rl(b))
    tag('block', 'minecraft:turns_into_dirt_path', rl('sift_grass_block'))
    tag('block', 'minecraft:turns_into_dirt_path', rl('coral_turf'))


def flush_tags():
    for (reg, ns, path), values in TAGS.items():
        write(os.path.join(RES, 'data', ns, 'tags', reg, path + '.json'), {'replace': False, 'values': values})


# --------------------------------------------------------------------------- main


def generate():
    __import__('sea_sky').assets(sys.modules[__name__])  # sea & sky: sounds, recipes, tags, text (before gen_sounds)
    for b in spec.BLOCKS:
        gen_block(b)
        gen_loot(b)
        gen_block_tags(b)
    for i in spec.ITEMS:
        gen_item(i)
    gen_recipes()
    gen_particles()
    gen_equipment()
    __import__('caravans').sounds(sys.modules[__name__])  # C: Caravans and music crystals
    gen_sounds()
    gen_misc_tags()
    gen_transformers()
    gen_wild_creatures()
    gen_parasite()
    import plants_h  # H: potted pitchers, soups, the Sift Gate Frame
    plants_h.generate(sys.modules[__name__])


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
