#!/usr/bin/env python3
"""1.0.1 — блок «Алтарь босса»: ступенчатый постамент с рунами, 4 колонны со светящимися навершиями, парящий кристалл."""
import json, os, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
T = os.path.join(R, 'textures', 'block')

def stone(name, base, seed, runes=False, glow=(190, 90, 255)):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (32, 32))
    px = im.load()
    for y in range(32):
        for x in range(32):
            n = rnd.randint(-10, 10)
            px[x, y] = tuple(max(0, min(255, c + n)) for c in base) + (255,)
    d = ImageDraw.Draw(im)
    # кладка
    for y in (0, 10, 21):
        d.line([0, y, 31, y], fill=tuple(int(c * 0.6) for c in base) + (255,))
    for y0, y1, xs in ((0, 10, (7, 23)), (10, 21, (0, 15)), (21, 32, (9, 26))):
        for x in xs: d.line([x, y0, x, y1], fill=tuple(int(c * 0.6) for c in base) + (255,))
    if runes:
        for _ in range(5):
            x, y = rnd.randint(3, 26), rnd.randint(3, 26)
            pts = [(x, y)]
            for _ in range(3):
                x += rnd.choice((-3, 0, 3)); y += rnd.choice((-3, 0, 3))
                pts.append((max(1, min(30, x)), max(1, min(30, y))))
            d.line(pts, fill=glow + (255,), width=1)
    im.save(os.path.join(T, name + '.png'))

def trim(name):
    im = Image.new('RGBA', (32, 32), (200, 160, 70, 255))
    d = ImageDraw.Draw(im)
    for i in range(0, 32, 4): d.line([i, 0, i, 31], fill=(150, 110, 40, 255))
    d.rectangle([0, 0, 31, 2], fill=(250, 220, 130, 255)); d.rectangle([0, 29, 31, 31], fill=(120, 85, 30, 255))
    im.save(os.path.join(T, name + '.png'))

def crystal(name):
    im = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(im)
    for y in range(32):
        for x in range(32):
            k = (x + y) / 62
            im.putpixel((x, y), (int(150 + 100 * k), int(80 + 60 * (1 - k)), 255, 255))
    for i in range(-32, 32, 7): d.line([i, 0, i + 32, 32], fill=(255, 230, 255, 255))
    im.save(os.path.join(T, name + '.png'))

def glowtip(name):
    im = Image.new('RGBA', (32, 32), (120, 230, 255, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([8, 8, 23, 23], fill=(230, 255, 255, 255))
    im.save(os.path.join(T, name + '.png'))

stone('altar_stone', (52, 44, 66), 1)
stone('altar_rune', (40, 34, 52), 2, runes=True)
trim('altar_gold'); crystal('altar_crystal'); glowtip('altar_glow')

def box(f, t, tex, rot=None, uvs=None):
    faces = {s: {'texture': '#' + (tex if isinstance(tex, str) else tex.get(s, tex['_']))} for s in ('north', 'south', 'east', 'west', 'up', 'down')}
    e = {'from': f, 'to': t, 'faces': faces}
    if rot: e['rotation'] = rot
    return e

el = [
    box([0, 0, 0], [16, 2, 16], {'_': 'r', 'up': 'r'}),
    box([0.5, 2, 0.5], [15.5, 2.6, 15.5], 'g'),
    box([2, 2.6, 2], [14, 5, 14], 'r'),
    box([3.5, 5, 3.5], [12.5, 6, 12.5], 'g'),
]
for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
    el.append(box([x, 2.6, z], [x + 2, 11, z + 2], 's'))
    el.append(box([x - 0.25, 11, z - 0.25], [x + 2.25, 12, z + 2.25], 'g'))
    el.append(box([x + 0.4, 12, z + 0.4], [x + 1.6, 13.6, z + 1.6], 'l'))
# парящий кристалл: два куба под 45°
el.append(box([6, 8, 6], [10, 14, 10], 'c', rot={'origin': [8, 11, 8], 'axis': 'y', 'angle': 45}))
el.append(box([6.8, 14, 6.8], [9.2, 16, 9.2], 'c', rot={'origin': [8, 15, 8], 'axis': 'y', 'angle': 45}))
el.append(box([6.8, 6.4, 6.8], [9.2, 8, 9.2], 'c', rot={'origin': [8, 7, 8], 'axis': 'y', 'angle': 45}))
model = {'parent': 'block/block', 'render_type': 'minecraft:cutout', 'ambientocclusion': False,
         'textures': {'r': 'pvpciv:block/altar_rune', 's': 'pvpciv:block/altar_stone', 'g': 'pvpciv:block/altar_gold',
                      'c': 'pvpciv:block/altar_crystal', 'l': 'pvpciv:block/altar_glow', 'particle': 'pvpciv:block/altar_stone'},
         'elements': el}
json.dump(model, open(os.path.join(R, 'models', 'block', 'boss_altar.json'), 'w'), indent=1)
json.dump({'variants': {'': {'model': 'pvpciv:block/boss_altar'}}}, open(os.path.join(R, 'blockstates', 'boss_altar.json'), 'w'), indent=1)
print('altar ok')
