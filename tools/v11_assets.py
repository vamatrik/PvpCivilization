#!/usr/bin/env python3
"""Ассеты 1.1: трофей Демоницы, осколок темноты, витрины трофеев по этажам."""
import os, sys, json
HERE = os.path.dirname(os.path.abspath(__file__))
sys.argv = [sys.argv[0]]
src = open(os.path.join(HERE, 'f5_assets.py')).read().split("art('cmp_lazuratron'")[0]
ns = {'__file__': os.path.join(HERE, 'f5_assets.py')}
exec(src, ns)
art, recolor, hx = ns['art'], ns['recolor'], ns['hx']
RES = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'pvpciv')

recolor('naza_trophy', 'demon_trophy', 0.97, 0.85, 0.9)
art('dark_shard', [
    "................",
    "..........p.....",
    ".........pk.....",
    "........pkvp....",
    ".......pkvwk....",
    "......pkvvkp....",
    ".....pkvmvk.....",
    "....pkvmmkp.....",
    "...pkvvmkp......",
    "...kvvvkp.......",
    "..pkvvkp........",
    "..kvkkp.........",
    ".pkkp...........",
    ".pp.............",
    "................",
    "................"], {'p': hx('5a1a8a'), 'k': hx('0a0614'), 'v': hx('2a0e48'), 'w': hx('f0d8ff'), 'm': hx('b030ff')})

STYLES = {
    1: dict(base='minecraft:block/polished_blackstone', back='minecraft:block/basalt_side', acc='minecraft:block/blue_ice'),
    2: dict(base='minecraft:block/black_concrete', back='minecraft:block/gray_concrete', acc='minecraft:block/cyan_concrete'),
    3: dict(base='minecraft:block/cherry_planks', back='minecraft:block/bamboo_planks', acc='minecraft:block/red_concrete'),
    4: dict(base='minecraft:block/quartz_block_bottom', back='minecraft:block/white_concrete', acc='minecraft:block/lime_concrete'),
    5: dict(base='minecraft:block/polished_deepslate', back='minecraft:block/obsidian', acc='minecraft:block/purple_concrete'),
}

def box(f, t, tex, faces=('north', 'south', 'east', 'west', 'up', 'down')):
    return {"from": f, "to": t, "faces": {k: {"texture": tex} for k in faces}}

def model(style, part):
    st = STYLES[style]
    els = [
        box([0, 0, 2], [16, 3, 16], '#base'),               # основание
        box([0, 13, 2], [16, 16, 16], '#base'),             # крышка
        box([0, 3, 15], [16, 13, 16], '#back'),             # задняя стенка
        box([0, 3, 2], [16, 3.6, 3.2], '#acc'),             # кант-подсветка снизу
        box([0, 12.4, 2], [16, 13, 3.2], '#acc'),           # кант сверху
        box([4, 3, 9], [12, 4, 13], '#acc'),                # подставка
        box([0, 3.6, 2.2], [16, 12.4, 2.4], '#glass', ('north', 'south')),
    ]
    if part in ('single', 'left'):   # стойка у x=0
        els.append(box([0, 3, 2], [1.2, 13, 15], '#base'))
    if part in ('single', 'right'):  # стойка у x=16
        els.append(box([14.8, 3, 2], [16, 13, 15], '#base'))
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"base": st['base'], "back": st['back'], "acc": st['acc'], "glass": "minecraft:block/glass", "particle": st['base']},
            "elements": els}

os.makedirs(os.path.join(RES, 'models', 'block', 'trophy_shelf'), exist_ok=True)
variants = {}
ROT = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
for s in STYLES:
    for part in ('single', 'left', 'middle', 'right'):
        name = f'trophy_shelf/s{s}_{part}'
        json.dump(model(s, part), open(os.path.join(RES, 'models', 'block', name + '.json'), 'w'), indent=1)
        for f, y in ROT.items():
            v = {"model": "pvpciv:block/" + name}
            if y: v["y"] = y
            variants[f"facing={f},part={part},style={s}"] = v
json.dump({"variants": variants}, open(os.path.join(RES, 'blockstates', 'trophy_shelf.json'), 'w'), indent=1)
json.dump({"parent": "pvpciv:block/trophy_shelf/s5_single"}, open(os.path.join(RES, 'models', 'item', 'trophy_shelf.json'), 'w'))
print('1.1: витрины готовы')

# ---------------------------------------------------------------- таблички
from PIL import Image, ImageDraw
TEXB = os.path.join(RES, 'textures', 'block')
SIGN = {0: ('minecraft:block/dark_oak_planks', 'minecraft:block/light_blue_concrete'),
        1: ('minecraft:block/polished_blackstone', 'minecraft:block/blue_ice'),
        2: ('minecraft:block/black_concrete', 'minecraft:block/cyan_concrete'),
        3: ('minecraft:block/stripped_dark_oak_log', 'minecraft:block/red_concrete'),
        4: ('minecraft:block/gray_concrete', 'minecraft:block/lime_concrete'),
        5: ('minecraft:block/black_concrete', 'minecraft:block/purple_concrete')}
os.makedirs(os.path.join(RES, 'models', 'block', 'civ_sign'), exist_ok=True)
def sign_model(style, standing):
    plate, acc = SIGN[style]
    if standing:
        els = [box([7, 0, 7], [9, 5, 9], '#acc'), box([0, 5, 7], [16, 15, 9], '#plate'),
               box([0, 14.4, 6.6], [16, 15, 9.4], '#acc'), box([0, 5, 6.6], [16, 5.6, 9.4], '#acc'),
               box([0, 5, 6.6], [0.6, 15, 9.4], '#acc'), box([15.4, 5, 6.6], [16, 15, 9.4], '#acc')]
    else:
        els = [box([0, 3, 14.2], [16, 13, 16], '#plate'),
               box([0, 12.4, 13.6], [16, 13, 14.2], '#acc'), box([0, 3, 13.6], [16, 3.6, 14.2], '#acc'),
               box([0, 3, 13.6], [0.6, 13, 14.2], '#acc'), box([15.4, 3, 13.6], [16, 13, 14.2], '#acc')]
    return {"parent": "minecraft:block/block", "textures": {"plate": plate, "acc": acc, "particle": plate}, "elements": els}
variants = {}
for s in SIGN:
    for standing in (False, True):
        name = f'civ_sign/s{s}_{"post" if standing else "wall"}'
        json.dump(sign_model(s, standing), open(os.path.join(RES, 'models', 'block', name + '.json'), 'w'), indent=1)
        for f, y in ROT.items():
            v = {"model": "pvpciv:block/" + name}
            if y: v["y"] = y
            variants[f"facing={f},standing={'true' if standing else 'false'},style={s}"] = v
json.dump({"variants": variants}, open(os.path.join(RES, 'blockstates', 'civ_sign.json'), 'w'), indent=1)

# ---------------------------------------------------------------- пульты
def screen(name, bg, fg, kind):
    im = Image.new('RGBA', (16, 16), bg); d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(40, 44, 52, 255))
    if kind == 'ui':
        for y in (3, 6, 9): d.line([2, y, 2 + (y * 3) % 9 + 3, y], fill=fg)
        d.rectangle([10, 3, 13, 7], outline=fg); d.point((11, 11), fill=(255, 90, 90, 255)); d.point((13, 11), fill=(120, 255, 140, 255))
    elif kind == 'ring':
        d.ellipse([2, 2, 13, 13], outline=fg); d.ellipse([5, 5, 10, 10], outline=fg)
    im.save(os.path.join(TEXB, name + '.png'))
screen('console_screen', (10, 24, 40, 255), (110, 220, 255, 255), 'ui')
screen('teleport_top', (14, 30, 48, 255), (150, 240, 255, 255), 'ring')
def rot(el, ang, axis='x', origin=(8, 8, 8)): el["rotation"] = {"angle": ang, "axis": axis, "origin": list(origin)}; return el
CONS = {
 'station': ({"body": "minecraft:block/light_gray_concrete", "dark": "minecraft:block/polished_deepslate", "scr": "pvpciv:block/console_screen"},
             [box([2, 0, 3], [14, 9, 13], '#body'), box([1, 0, 2], [15, 1, 14], '#dark'),
              rot(box([2, 9, 4], [14, 11, 13], '#dark'), -22.5, 'x', (8, 10, 8)),
              rot(box([3, 11, 5], [13, 11.2, 12], '#scr', ('up',)), -22.5, 'x', (8, 10, 8)),
              box([3, 9, 12], [13, 15, 13], '#dark'), box([4, 10, 11.9], [12, 14, 12], '#scr', ('north',))]),
 'lab': ({"body": "minecraft:block/white_concrete", "top": "minecraft:block/lime_stained_glass", "dark": "minecraft:block/smooth_stone", "flask": "minecraft:block/lime_concrete"},
         [box([0, 0, 0], [16, 10, 16], '#body'), box([1, 10, 1], [15, 11, 15], '#top'), box([6, 11, 6], [10, 15, 10], '#flask'), box([7, 15, 7], [9, 16, 9], '#dark')]),
 'stand': ({"base": "minecraft:block/lectern_base", "side": "minecraft:block/lectern_sides", "top": "minecraft:block/lectern_top"},
           [box([4, 0, 4], [12, 2, 12], '#base'), box([6, 2, 6], [10, 11, 10], '#side'),
            rot(box([2, 11, 3], [14, 13, 13], '#top'), -22.5, 'x', (8, 12, 8))]),
 'teleport': ({"body": "minecraft:block/polished_deepslate", "glass": "minecraft:block/light_blue_stained_glass", "top": "pvpciv:block/teleport_top", "scr": "pvpciv:block/console_screen"},
              [box([4, 0, 4], [12, 2, 12], '#body'), box([5, 2, 5], [11, 12, 11], '#glass'), box([4, 12, 4], [12, 16, 12], '#body'),
               box([4.5, 16, 4.5], [11.5, 16.01, 11.5], '#top', ('up',)), box([5, 13, 3.9], [11, 15, 4], '#scr', ('north',))]),
}
os.makedirs(os.path.join(RES, 'models', 'block', 'console'), exist_ok=True)
variants = {}
for k, (tex, els) in CONS.items():
    tex = dict(tex); tex["particle"] = list(tex.values())[0]
    json.dump({"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": tex, "elements": els},
              open(os.path.join(RES, 'models', 'block', 'console', k + '.json'), 'w'), indent=1)
    for f, y in ROT.items():
        v = {"model": "pvpciv:block/console/" + k}
        if y: v["y"] = y
        variants[f"facing={f},kind={k}"] = v
json.dump({"variants": variants}, open(os.path.join(RES, 'blockstates', 'console.json'), 'w'), indent=1)

# ---------------------------------------------------------------- ритуал
os.makedirs(os.path.join(RES, 'models', 'block', 'ritual'), exist_ok=True)
core = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"b": "minecraft:block/polished_blackstone", "c": "minecraft:block/crying_obsidian", "g": "minecraft:block/gilded_blackstone", "a": "minecraft:block/amethyst_block", "particle": "minecraft:block/crying_obsidian"},
        "elements": [box([1, 0, 1], [15, 3, 15], '#b'), box([3, 3, 3], [13, 9, 13], '#c'), box([1, 9, 1], [15, 12, 15], '#g'),
                     box([1, 12, 1], [3, 15, 3], '#a'), box([13, 12, 1], [15, 15, 3], '#a'), box([1, 12, 13], [3, 15, 15], '#a'), box([13, 12, 13], [15, 15, 15], '#a')]}
json.dump(core, open(os.path.join(RES, 'models', 'block', 'ritual', 'core.json'), 'w'), indent=1)
def inj(lit):
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"b": "minecraft:block/polished_blackstone", "s": "minecraft:block/obsidian",
                         "h": "minecraft:block/crying_obsidian" if lit else "minecraft:block/obsidian",
                         "t": "minecraft:block/amethyst_block" if lit else "minecraft:block/purple_terracotta", "particle": "minecraft:block/obsidian"},
            "elements": [box([3, 0, 3], [13, 2, 13], '#b'), box([6, 2, 6], [10, 7, 10], '#s'), box([5, 7, 5], [11, 11, 11], '#h'),
                         box([6.5, 8, 2], [9.5, 10, 5], '#t'), box([7.25, 8.5, 0.5], [8.75, 9.5, 2], '#t'),
                         box([4, 11, 4], [5, 13, 5], '#t'), box([11, 11, 4], [12, 13, 5], '#t'), box([4, 11, 11], [5, 13, 12], '#t'), box([11, 11, 11], [12, 13, 12], '#t')]}
for lit in (False, True):
    json.dump(inj(lit), open(os.path.join(RES, 'models', 'block', 'ritual', 'injector' + ('_lit' if lit else '') + '.json'), 'w'), indent=1)
for blk, mk in (('ritual_core', lambda lit: 'core'), ('ritual_injector', lambda lit: 'injector_lit' if lit else 'injector')):
    variants = {}
    for f, y in ROT.items():
        for lit in (False, True):
            v = {"model": "pvpciv:block/ritual/" + mk(lit)}
            if y: v["y"] = y
            variants[f"facing={f},lit={'true' if lit else 'false'}"] = v
    json.dump({"variants": variants}, open(os.path.join(RES, 'blockstates', blk + '.json'), 'w'), indent=1)
print('1.1: таблички, пульты, ритуал готовы')
