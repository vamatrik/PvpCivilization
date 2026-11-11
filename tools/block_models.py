#!/usr/bin/env python3
"""
Свои 3D-модели и HD-текстуры (32×32) для блоков мода: кузница, разборочный стол, тайник, ядро арены, дверь квартиры.
Модели — JSON-элементы (как оружие). Запуск: python3 tools/block_models.py
"""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
TEX = os.path.join(ROOT, 'textures', 'block'); MOD = os.path.join(ROOT, 'models', 'block')
os.makedirs(TEX, exist_ok=True); os.makedirs(MOD, exist_ok=True)
R = random.Random(7)

def hx(s): s = s.lstrip('#'); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))
def mul(c, k): return tuple(max(0, min(255, int(v * k))) for v in c)

def tex(name, fn, size=32):
    im = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            c = fn(x, y, size)
            im.putpixel((x, y), c if len(c) == 4 else c + (255,))
    im.save(os.path.join(TEX, name + '.png'))

def noise(base, amt=0.12):
    return lambda x, y, s: mul(base, 1 - amt + 2 * amt * R.random())

def bricks(base, mortar, h=8, w=16):
    def f(x, y, s):
        row = y // h; off = (row % 2) * (w // 2)
        if y % h == 0 or (x + off) % w == 0: return mul(mortar, 0.9 + 0.2 * R.random())
        return mul(base, 0.85 + 0.3 * R.random())
    return f

def planks(base, h=8):
    def f(x, y, s):
        if y % h == 0: return mul(base, 0.55)
        k = 0.85 + 0.15 * ((x * 7 + (y // h) * 13) % 5) / 5 + 0.08 * R.random()
        return mul(base, k)
    return f

def metal(base, rivets=True):
    def f(x, y, s):
        edge = x in (0, s - 1) or y in (0, s - 1)
        if edge: return mul(base, 0.5)
        if rivets and (x, y) in ((3, 3), (s - 4, 3), (3, s - 4), (s - 4, s - 4)): return mul(base, 1.6)
        return mul(base, 0.85 + 0.25 * (1 - y / s) + 0.06 * R.random())
    return f

def ember(x, y, s):
    k = R.random()
    return hx('ffb347') if k > 0.75 else hx('ff6a1a') if k > 0.35 else hx('b8320a')

def crystal(x, y, s):
    d = abs(x - s / 2) + abs(y - s / 2)
    c = hx('ffe0a0') if d < 6 else hx('ff9a3a') if d < 12 else hx('d0561a')
    return mul(c, 0.9 + 0.2 * R.random())

def door(top):
    frame, panel, gold = hx('2a2a34'), hx('3c3c4a'), hx('e8c45a')
    def f(x, y, s):
        if x < 2 or x > s - 3 or (top and y < 2) or (not top and y > s - 3): return mul(frame, 0.9 + 0.2 * R.random())
        if x in (2, s - 3) or (top and y == 2) or (not top and y == s - 3): return gold
        if top and 8 <= y <= 22 and 8 <= x <= 23:             # окошко
            return (120, 170, 210, 255) if (x + y) % 9 else (190, 225, 245, 255)
        if not top and 4 <= y <= 9 and 21 <= x <= 26:          # картридер
            return hx('5aff9a') if y == 5 and 22 <= x <= 25 else hx('15151c')
        return mul(panel, 0.88 + 0.12 * ((y // 4) % 2) + 0.06 * R.random())
    return f

# ------------------------------------------------------------------ текстуры
tex('forge_stone', bricks(hx('4a4a52'), hx('2a2a30')))
tex('forge_metal', metal(hx('5a5e6a')))
tex('forge_metal_top', metal(hx('7a7f8c'), rivets=False))
tex('forge_ember', ember)
tex('dismantle_wood', planks(hx('7a5232')))
tex('dismantle_top', planks(hx('9a6a40'), h=16))
tex('dismantle_iron', metal(hx('6a6e78')))
tex('dismantle_saw', lambda x, y, s: hx('d0d4dc') if (x + y) % 6 else hx('8a8e98'))
tex('stash_wood', planks(hx('6a4a2a')))
tex('stash_band', metal(hx('3a3a40')))
tex('stash_lock', metal(hx('d8b04a'), rivets=False))
tex('core_stone', bricks(hx('2a2630'), hx('15131a'), h=16, w=16))
tex('core_crystal', crystal)
tex('door_bottom', door(False))
tex('door_top', door(True))

# ------------------------------------------------------------------ модели
def el(a, b, t, light=0, faces=('north', 'south', 'east', 'west', 'up', 'down'), rot=None):
    e = {"from": a, "to": b, "faces": {f: {"texture": "#" + t} for f in faces}}
    if light: e["neoforge_data"] = {"block_light": light, "sky_light": light}
    if rot: e["rotation"] = rot
    return e

def model(name, textures, elements, particle):
    t = {k: "pvpciv:block/" + v for k, v in textures.items()}
    t["particle"] = "pvpciv:block/" + particle
    json.dump({"textures": t, "elements": elements}, open(os.path.join(MOD, name + '.json'), 'w'), indent=1)

# кузница: каменный горн с тлеющими углями + наковальня сверху
model('forge', {"s": "forge_stone", "m": "forge_metal", "t": "forge_metal_top", "e": "forge_ember"}, [
    el([0, 0, 0], [16, 7, 16], "s"),
    el([2, 2, -0.01], [14, 5, 0.5], "e", 15, faces=('north',)),           # окно горна с углями
    el([4, 7, 5], [12, 9, 11], "m"),                                        # основание наковальни
    el([6, 9, 6.5], [10, 11, 9.5], "m"),                                    # талия
    el([2, 11, 4.5], [14, 15, 11.5], "t"),                                  # рабочая плита
    el([14, 12, 6], [16.5, 14.5, 10], "m"),                                 # рог
    el([0, 11.5, 6.5], [2, 14.5, 9.5], "m"),
], "forge_stone")

# разборочный стол: верстак на ножках, тиски и дисковая пила
model('dismantle_table', {"w": "dismantle_wood", "t": "dismantle_top", "i": "dismantle_iron", "p": "dismantle_saw"}, [
    el([0, 12, 0], [16, 15, 16], "t"),
    el([1, 0, 1], [4, 12, 4], "w"), el([12, 0, 1], [15, 12, 4], "w"), el([1, 0, 12], [4, 12, 15], "w"), el([12, 0, 12], [15, 12, 15], "w"),
    el([1, 4, 2], [15, 6, 14], "w"),                                        # нижняя полка
    el([10, 15, 2], [15, 19, 7], "i"), el([11, 19, 3], [14, 20, 6], "i"),   # тиски
    el([3, 15, 9], [8, 16.5, 14], "i"),                                     # обломки/пластина
    el([7.5, 14, 2], [8.5, 22, 10], "p", rot={"origin": [8, 18, 6], "axis": "x", "angle": 22.5}),   # пила
], "dismantle_wood")

# тайник: окованный сундук-ящик с золотым замком. Крышка — отдельная модель (stash_lid), её рисует
# StashRenderer: закрыта, если тайник готов, и откинута, если ты его уже обчистил (у каждого игрока своё).
tex('stash_inside', lambda x, y, s: mul(hx('1c140c'), 0.8 + 0.4 * R.random()))
model('stash', {"w": "stash_wood", "b": "stash_band", "l": "stash_lock", "i": "stash_inside"}, [
    {**el([1, 0, 2], [15, 10, 14], "w"), "faces": {f: {"texture": "#i" if f == "up" else "#w"} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
    el([0.4, 0, 3], [15.6, 10.2, 4.5], "b"), el([0.4, 0, 11.5], [15.6, 10.2, 13], "b"),
    el([6.5, 7, 1.2], [9.5, 10, 2], "l"),
], "stash_wood")
model('stash_lid', {"w": "stash_wood", "b": "stash_band", "l": "stash_lock"}, [
    el([0.5, 10, 1.5], [15.5, 13, 14.5], "w"),
    el([0.4, 10, 3], [15.6, 13.2, 4.5], "b"), el([0.4, 10, 11.5], [15.6, 13.2, 13], "b"),
    el([6.5, 10, 1.2], [9.5, 11.5, 2], "l"),
], "stash_wood")
model('stash_full', {"w": "stash_wood", "b": "stash_band", "l": "stash_lock"}, [   # для иконки предмета
    el([1, 0, 2], [15, 10, 14], "w"), el([0.5, 10, 1.5], [15.5, 13, 14.5], "w"),
    el([0.4, 0, 3], [15.6, 13.2, 4.5], "b"), el([0.4, 0, 11.5], [15.6, 13.2, 13], "b"), el([6.5, 7, 1.2], [9.5, 11, 2], "l"),
], "stash_wood")

# ядро арены: пьедестал из чернокамня и парящий кристалл (светится)
model('arena_core', {"s": "core_stone", "c": "core_crystal"}, [
    el([1, 0, 1], [15, 4, 15], "s"),
    el([3, 4, 3], [13, 7, 13], "s"),
    el([5.5, 8, 5.5], [10.5, 18, 10.5], "c", 15, rot={"origin": [8, 13, 8], "axis": "y", "angle": 45}),
    el([2, 7, 2], [4, 10, 4], "c", 15), el([12, 7, 12], [14, 10, 14], "c", 15), el([2, 7, 12], [4, 10, 14], "c", 15), el([12, 7, 2], [14, 10, 4], "c", 15),
], "core_stone")

# дверь квартиры: ванильные шаблоны двери со своими текстурами
for part in ('bottom_left', 'bottom_left_open', 'bottom_right', 'bottom_right_open', 'top_left', 'top_left_open', 'top_right', 'top_right_open'):
    json.dump({"parent": "minecraft:block/door_" + part,
               "textures": {"bottom": "pvpciv:block/door_bottom", "top": "pvpciv:block/door_top"}},
              open(os.path.join(MOD, 'apartment_door_' + part + '.json'), 'w'))
bs = os.path.join(ROOT, 'blockstates', 'apartment_door.json')
s = open(bs).read().replace('minecraft:block/iron_door_', 'pvpciv:block/apartment_door_')
open(bs, 'w').write(s)
# ------------------------------------------------------------------ 0.7: алтарь инфузии, пьедестал, стол брони, домашний сундук
def runes(base, glow):
    def f(x, y, s):
        if (x in (4, 5) and 6 < y < 26) or (y in (15, 16) and 6 < x < 26) or abs((x - 16) ** 2 + (y - 16) ** 2 - 100) < 12:
            return mul(glow, 0.85 + 0.3 * R.random())
        return mul(base, 0.8 + 0.3 * R.random())
    return f
def obsidian(x, y, s):
    k = R.random()
    return mul(hx('1c1028'), 0.8 + 0.4 * k) if k > 0.12 else hx('5a2a8a')
tex('altar_stone', bricks(hx('2a2230'), hx('120c18'), h=8, w=16))
tex('altar_top', runes(hx('241a2c'), hx('d0305a')))
tex('altar_obsidian', obsidian)
tex('altar_gold', metal(hx('c9a24a')))
tex('altar_blood', lambda x, y, s: mul(hx('c0103a'), 0.75 + 0.5 * R.random()))
tex('bench_wood', planks(hx('3a4a5a')))
tex('bench_top', planks(hx('5a6e82'), h=16))
tex('bench_steel', metal(hx('8aa0b8')))
tex('bench_ice', crystal_ice := (lambda x, y, s: mul(hx('8fd8ff') if abs(x - s / 2) + abs(y - s / 2) > 8 else hx('e8faff'), 0.85 + 0.25 * R.random())))
tex('home_chest_wood', planks(hx('6a4a7a')))
tex('home_chest_band', metal(hx('e8c45a'), rivets=False))

# алтарь инфузии: ступенчатый постамент из тёмного камня, золотое кольцо, светящаяся руна и кровавый кристалл над центром
model('infusion_altar', {"s": "altar_stone", "t": "altar_top", "o": "altar_obsidian", "g": "altar_gold", "b": "altar_blood"}, [
    el([0, 0, 0], [16, 4, 16], "s"),
    el([2, 4, 2], [14, 9, 14], "o"),
    {**el([1, 9, 1], [15, 11, 15], "s"), "faces": {f: {"texture": "#t" if f == "up" else "#s"} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
    el([0.5, 9.5, 0.5], [2.5, 11.5, 2.5], "g"), el([13.5, 9.5, 0.5], [15.5, 11.5, 2.5], "g"),
    el([0.5, 9.5, 13.5], [2.5, 11.5, 15.5], "g"), el([13.5, 9.5, 13.5], [15.5, 11.5, 15.5], "g"),
    el([6.5, 13, 6.5], [9.5, 19, 9.5], "b", 15, rot={"origin": [8, 16, 8], "axis": "y", "angle": 45}),
    el([7, 11, 7], [9, 12, 9], "g"),
], "altar_stone")
# пьедестал: колонна с золотой чашей и свечным огоньком
model('infusion_pedestal', {"s": "altar_stone", "o": "altar_obsidian", "g": "altar_gold", "b": "altar_blood"}, [
    el([3, 0, 3], [13, 3, 13], "s"),
    el([5, 3, 5], [11, 11, 11], "o"),
    el([3.5, 11, 3.5], [12.5, 13, 12.5], "g"),
    el([4.5, 13, 4.5], [11.5, 13.5, 11.5], "s"),
    el([7.25, 13.5, 7.25], [8.75, 15, 8.75], "b", 12),
], "altar_stone")
# стол брони: стальной верстак с манекеном-стойкой и ледяными кристаллами
model('armor_bench', {"w": "bench_wood", "t": "bench_top", "i": "bench_steel", "c": "bench_ice"}, [
    el([0, 10, 0], [16, 13, 16], "t"),
    el([1, 0, 1], [4, 10, 4], "w"), el([12, 0, 1], [15, 10, 4], "w"), el([1, 0, 12], [4, 10, 15], "w"), el([12, 0, 12], [15, 10, 15], "w"),
    el([1, 3, 2], [15, 5, 14], "w"),
    el([7, 13, 9], [9, 24, 11], "i"),                                       # стойка
    el([4, 19, 8], [12, 23, 12], "i"),                                      # «плечи» нагрудника
    el([5, 15, 8.5], [11, 19, 11.5], "i"),
    el([1.5, 13, 2], [4.5, 17, 5], "c", 10, rot={"origin": [3, 15, 3.5], "axis": "y", "angle": 22.5}),
    el([11.5, 13, 2.5], [14, 15.5, 5], "c", 10),
], "bench_wood")
# домашний сундук: сундук с золотыми накладками (у каждого игрока своё содержимое)
model('home_chest', {"w": "home_chest_wood", "b": "home_chest_band"}, [
    el([1, 0, 1], [15, 14, 15], "w"),
    el([0.6, 9, 0.6], [15.4, 10, 15.4], "b"),
    el([6.5, 7, 0.4], [9.5, 11, 1], "b"),
    el([0.6, 0, 0.6], [2, 14, 2], "b"), el([14, 0, 0.6], [15.4, 14, 2], "b"),
], "home_chest_wood")
for n, facing in (('infusion_altar', False), ('infusion_pedestal', False), ('armor_bench', True), ('home_chest', True)):
    path = os.path.join(ROOT, 'blockstates', n + '.json')
    v = {"facing=" + d: {"model": "pvpciv:block/" + n, **({"y": y} if y and facing else {})} for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    json.dump({"variants": v}, open(path, 'w'), indent=1)
    json.dump({"parent": "pvpciv:block/" + n}, open(os.path.join(ROOT, 'models', 'item', n + '.json'), 'w'))

# 0.8: витрина трофеев — тумба, стеклянный куб, золотые рёбра
model('trophy_case', {"w": "home_chest_wood", "b": "home_chest_band", "g": "glass_pane"}, [
    el([0, 0, 0], [16, 4, 16], "w"),
    el([0.5, 4, 0.5], [1.5, 16, 1.5], "b"), el([14.5, 4, 0.5], [15.5, 16, 1.5], "b"), el([0.5, 4, 14.5], [1.5, 16, 15.5], "b"), el([14.5, 4, 14.5], [15.5, 16, 15.5], "b"),
    el([0.5, 15, 0.5], [15.5, 16, 15.5], "b"),
    el([1, 4, 1], [15, 15, 1.2], "g"), el([1, 4, 14.8], [15, 15, 15], "g"), el([1, 4, 1], [1.2, 15, 15], "g"), el([14.8, 4, 1], [15, 15, 15], "g"),
], "home_chest_wood")
tex('glass_pane', lambda x, y, s: (200, 230, 255, 70) if x not in (0, s - 1) and y not in (0, s - 1) else (230, 245, 255, 140))
m = json.load(open(os.path.join(MOD, 'trophy_case.json'))); m["render_type"] = "minecraft:translucent"
json.dump(m, open(os.path.join(MOD, 'trophy_case.json'), 'w'), indent=1)
v = {"facing=" + d: {"model": "pvpciv:block/trophy_case"} for d in ('north', 'east', 'south', 'west')}
json.dump({"variants": v}, open(os.path.join(ROOT, 'blockstates', 'trophy_case.json'), 'w'), indent=1)
json.dump({"parent": "pvpciv:block/trophy_case"}, open(os.path.join(ROOT, 'models', 'item', 'trophy_case.json'), 'w'))

# 0.8: табло Небесного лифта — стойка с голубым экраном
tex('lift_screen', lambda x, y, s: (20, 40, 70, 255) if x in (0, s - 1) or y in (0, s - 1) else ((140, 230, 255, 255) if (y // 4) % 2 == 0 and 4 < x < s - 5 else (40, 120, 190, 255)))
model('lift_panel', {"m": "bench_steel", "s": "lift_screen"}, [
    el([5, 0, 5], [11, 10, 11], "m"),
    el([2, 10, 6], [14, 20, 10], "m"),
    el([3, 11, 5.6], [13, 19, 6], "s", 15, faces=('north',)),
    el([3, 11, 10], [13, 19, 10.4], "s", 15, faces=('south',)),
], "bench_steel")
v = {"facing=" + d: {"model": "pvpciv:block/lift_panel", **({"y": y} if y else {})} for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
json.dump({"variants": v}, open(os.path.join(ROOT, 'blockstates', 'lift_panel.json'), 'w'), indent=1)
json.dump({"parent": "pvpciv:block/lift_panel"}, open(os.path.join(ROOT, 'models', 'item', 'lift_panel.json'), 'w'))

# иконки предметов-блоков = модели блоков
for n in ('forge', 'dismantle_table', 'stash', 'arena_core'):
    json.dump({"parent": "pvpciv:block/" + (n + '_full' if n == 'stash' else n)}, open(os.path.join(ROOT, 'models', 'item', n + '.json'), 'w'))
print('ok')
