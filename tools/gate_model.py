#!/usr/bin/env python3
"""Врата телепортации: geo-модель (GeckoLib), анимации active/broken, текстуры по этажам + маски свечения.
Запуск: python3 tools/gate_model.py"""
import json, os, random
from PIL import Image
R = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv')

def face(u, v, w, h):
    return {"uv": [u, v], "uv_size": [w, h]}
def cube(o, s, region, rot=None, pivot=None):
    u, v, w, h = region
    c = {"origin": o, "size": s, "uv": {k: face(u, v, w, h) for k in ("north", "south", "east", "west", "up", "down")}}
    if rot: c["rotation"] = rot; c["pivot"] = pivot or [0, 0, 0]
    return c
STONE, RUNE, CRYST, DARK = (0, 0, 32, 32), (32, 16, 32, 8), (32, 0, 16, 16), (0, 32, 32, 16)
bones = [
    {"name": "root", "pivot": [0, 0, 0]},
    {"name": "base", "parent": "root", "pivot": [0, 0, 0], "cubes": [
        cube([-12, 0, -12], [24, 2, 24], STONE), cube([-9, 2, -9], [18, 2, 18], DARK),
        cube([-8, 4, -8], [16, 0.25, 16], RUNE),
        cube([-11.5, 2, -11.5], [3, 9, 3], STONE), cube([8.5, 2, -11.5], [3, 9, 3], STONE),
        cube([-11.5, 2, 8.5], [3, 9, 3], STONE), cube([8.5, 2, 8.5], [3, 9, 3], STONE),
        cube([-11, 11, -11], [2, 1, 2], RUNE), cube([9, 11, -11], [2, 1, 2], RUNE), cube([-11, 11, 9], [2, 1, 2], RUNE), cube([9, 11, 9], [2, 1, 2], RUNE)]},
    {"name": "crystal", "parent": "root", "pivot": [0, 20, 0], "cubes": [
        cube([-3.5, 16.5, -3.5], [7, 7, 7], CRYST, [45, 45, 0], [0, 20, 0]),
        cube([-1.5, 12, -1.5], [3, 16, 3], CRYST, [0, 45, 0], [0, 20, 0])]},
    {"name": "ring", "parent": "crystal", "pivot": [0, 20, 0], "cubes": [
        cube([-6, 19.75, -6], [12, 0.5, 1], RUNE), cube([-6, 19.75, 5], [12, 0.5, 1], RUNE),
        cube([-6, 19.75, -5], [1, 0.5, 10], RUNE), cube([5, 19.75, -5], [1, 0.5, 10], RUNE)]},
]
geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": "geometry.gate", "texture_width": 64, "texture_height": 64,
        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]}, "bones": bones}]}
json.dump(geo, open(os.path.join(R, 'geo', 'gate.geo.json'), 'w'), indent=1)

anim = {"format_version": "1.8.0", "animations": {
    "active": {"loop": True, "animation_length": 4, "bones": {
        "crystal": {"rotation": {"0": [0, 0, 0], "4": [0, 360, 0]}, "position": {"0": [0, 0, 0], "1": [0, 1.2, 0], "2": [0, 0, 0], "3": [0, -1.2, 0], "4": [0, 0, 0]}},
        "ring": {"rotation": {"0": [0, 0, 0], "4": [0, -720, 0]}}}},
    "broken": {"loop": True, "animation_length": 1, "bones": {
        "crystal": {"rotation": [20, 30, 75], "position": [3, -14, 2]},
        "ring": {"rotation": [0, 0, -40], "position": [0, -2, 0]}}}}}
json.dump(anim, open(os.path.join(R, 'animations', 'gate.animation.json'), 'w'), indent=1)

COL = {'ash': ((255, 150, 60), (255, 200, 120)), 'ice': ((90, 200, 255), (190, 240, 255)), 'neon': ((255, 70, 200), (255, 170, 240))}
rnd = random.Random(7)
for name, (c1, c2) in COL.items():
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0)); gm = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    for x in range(32):
        for y in range(32):   # камень
            g = 70 + rnd.randint(-10, 10) - (12 if (x % 8 == 0 or y % 8 == 0) else 0)
            im.putpixel((x, y), (g, g - 2, g + 6, 255))
        for y in range(32, 48):   # тёмный ярус
            g = 40 + rnd.randint(-6, 6)
            im.putpixel((x, y), (g, g, g + 8, 255))
    for x in range(32, 48):
        for y in range(16):   # кристалл: градиент
            k = (x - 32 + y) / 30
            c = tuple(int(c1[i] * (1 - k) + c2[i] * k) for i in range(3))
            if (x + y) % 7 == 0: c = (255, 255, 255)
            im.putpixel((x, y), c + (255,)); gm.putpixel((x, y), c + (255,))
    for x in range(32, 64):
        for y in range(16, 24):   # руны
            on = ((x * 3 + y * 5) % 7 < 2) or y in (16, 23)
            c = c2 if on else tuple(v // 3 for v in c1)
            im.putpixel((x, y), c + (255,))
            if on: gm.putpixel((x, y), c2 + (255,))
    im.save(os.path.join(R, 'textures', 'entity', f'gate_{name}.png'))
    gm.save(os.path.join(R, 'textures', 'entity', f'gate_{name}_glowmask.png'))
print('ok')
