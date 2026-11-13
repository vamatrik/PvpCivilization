#!/usr/bin/env python3
"""Иконки этажа 3 (0.9.0): тома, Альманах, обломки катаны Юки; модель катаны Юки (перекраска самурайской)."""
import json, os, colorsys
from PIL import Image
import importlib.util
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
TEX = os.path.join(ROOT, 'textures', 'item'); MOD = os.path.join(ROOT, 'models', 'item')
def hx(s, a=255): s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)
def art(name, rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and ch in pal: im.putpixel((x, y), pal[ch])
    im.save(os.path.join(TEX, name + '.png'))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/" + name}}, open(os.path.join(MOD, name + '.json'), 'w'))
BOOK = [
    "................",
    "...oooooooooo...",
    "..occcccccccpo..",
    "..ocllllllllpo..",
    "..ocllgglllpwo..",
    "..oclgllgllpwo..",
    "..oclgllgllpwo..",
    "..ocllgglllpwo..",
    "..ocllllllllpo..",
    "..oclssssssllpo.",
    "..ocllllllllpwo.",
    "..occcccccccpwo.",
    "..oddddddddddwo.",
    "...owwwwwwwwwwo.",
    "....oooooooooo..",
    "................"]
for name, c, g in [('tome_ash', '5a4a3a', 'd08040'), ('tome_neon', '3a2a5a', 'ff4fd8'), ('tome_silence', '2a3a3a', 'a8e8ff')]:
    pal = {'o': hx('1a1418'), 'c': hx(c), 'l': hx(c), 'p': hx('3a2a20'), 'w': hx('efe6d0'), 'g': hx(g), 's': hx(g), 'd': hx('2a2020')}
    pal['l'] = tuple(min(255, int(v * 1.25)) for v in hx(c)[:3]) + (255,)
    art(name, BOOK, pal)
art('almanac', BOOK, {'o': hx('1a1418'), 'c': hx('6a1a2a'), 'l': hx('8a2436'), 'p': hx('c8a86a'), 'w': hx('f4ecd8'), 'g': hx('ffd166'), 's': hx('ffd166'), 'd': hx('3a1018')})
art('yuki_remains', [
    "................",
    "...........ww...",
    "..........wlw...",
    ".........wlw....",
    "........wlw.....",
    ".......wlw......",
    "................",
    ".....wlw........",
    "....wlw.........",
    "...ggg..........",
    "..rrg...........",
    ".rrr............",
    ".rr.............",
    "................",
    "................",
    "................"], {'w': hx('c8d8e8'), 'l': hx('f4faff'), 'g': hx('e8c45a'), 'r': hx('c02840')})
# катана Юки: перекраска текстуры самурайской — клинок холоднее, обмотка алая
src = Image.open(os.path.join(TEX, 'w3d', 'samurai_katana.png')).convert('RGBA')
out = src.copy()
for y in range(src.height):
    for x in range(src.width):
        r, g, b, a = src.getpixel((x, y))
        if a == 0: continue
        h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
        if s < 0.18 and l > 0.45: h, s, l = 0.58, 0.35, min(0.97, l * 1.08)        # сталь → ледяная
        elif l < 0.45: h, s = 0.97, max(0.55, s)                                   # тёмное → алое
        r2, g2, b2 = colorsys.hls_to_rgb(h, l, s)
        out.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
out.save(os.path.join(TEX, 'w3d', 'yuki_katana.png'))
m = json.load(open(os.path.join(MOD, 'samurai_katana.json')))
m['textures'] = {"0": "pvpciv:item/w3d/yuki_katana", "particle": "pvpciv:item/w3d/yuki_katana"}
m.pop('overrides', None)
json.dump(m, open(os.path.join(MOD, 'yuki_katana.json'), 'w'))
print('ok')
