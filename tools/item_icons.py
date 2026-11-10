#!/usr/bin/env python3
"""Иконки 16×16 для простых предметов (броня, осколки, медальон, плюшевый Пепел, лодка т.2) + модели item/generated
и модели яиц призыва. Запуск: python3 tools/item_icons.py"""
import json, os
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
TEX = os.path.join(ROOT, 'textures', 'item')
MOD = os.path.join(ROOT, 'models', 'item')

def art(name, rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and ch in pal: im.putpixel((x, y), pal[ch])
    im.save(os.path.join(TEX, name + '.png'))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/" + name}}, open(os.path.join(MOD, name + '.json'), 'w'))

def hx(s, a=255): s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)

ICE = {'o': hx('1a2a40'), 'd': hx('3a5a7a'), 'm': hx('6a9abf'), 'l': hx('a8d8f0'), 'w': hx('eafaff'), 'f': hx('d8c8a8'), 'b': hx('5a4030'), 'g': hx('e8c45a')}

art('rime_helmet', [
    "................",
    ".....oooooo.....",
    "....owwllmmo....",
    "...owlllmmmdo...",
    "...olllmmmddo...",
    "..olllmmmmddao..",
    "..ofmmooooddfo..",
    "..ofmo....odfo..",
    "..ofo......ofo..",
    "..oo........oo..",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................"], ICE)
art('rime_chestplate', [
    "................",
    "..ooo......ooo..",
    ".owlmo....omldo.",
    ".olmmdoooodmmdo.",
    ".ommmmdwwdmmmdo.",
    "..odmmmllmmmdo..",
    "...ommmllmmmo...",
    "...ommlwwlmmo...",
    "...ommmllmmdo...",
    "...ofmmllmmfo...",
    "...offffffffo...",
    "...ommmmmmmdo...",
    "...odmmmmmddo...",
    "....oooooooo....",
    "................",
    "................"], ICE)
art('rime_leggings', [
    "................",
    "....oooooooo....",
    "....offffffo....",
    "....olmmmmdo....",
    "....olmmomdo....",
    "....olmo.omdo...",
    "....olmo.olmo...",
    "....olmo.olmo...",
    "....olmo.olmo...",
    "....owmo.owmo...",
    "....olmo.olmo...",
    "....oddo.oddo...",
    "....oooo.oooo...",
    "................",
    "................",
    "................"], ICE)
art('rime_boots', [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...oooo..oooo...",
    "...offo..offo...",
    "...olmo..olmo...",
    "...olmo..olmo...",
    "...olmo..olmo...",
    "..olmmo..olmmo..",
    ".olmmmo..olmmmo.",
    ".owwwwo..owwwwo.",
    ".odwdwo..odwdwo.",
    "................",
    "................"], ICE)
art('rime_shard', [
    "................",
    "................",
    ".........ow.....",
    "........owlo....",
    ".......owllo....",
    "......owllmo....",
    ".....owllmmo....",
    "....owllmmdo....",
    "....owlmmdo.....",
    "...owlmmdo......",
    "...olmmdo.......",
    "...olmdo........",
    "....odo.........",
    ".....o..........",
    "................",
    "................"], ICE)
art('frozen_locket', [
    "................",
    "......ooo.......",
    ".....o...o......",
    "......ooo.......",
    ".....oggggo.....",
    "....oglllwgo....",
    "...ogllwwllgo...",
    "...oglwmmwlgo...",
    "...ogllmmllgo...",
    "...ogllllllgo...",
    "....ogllllgo....",
    ".....oggggo.....",
    "......oooo......",
    "................",
    "................",
    "................"], ICE)
PL = {'o': hx('3a2030'), 'h': hx('6a6a72'), 'H': hx('9a9aa2'), 's': hx('f6e2d2'), 'e': hx('1a1a22'), 'p': hx('ff9ac8'), 'P': hx('d86aa0'), 'r': hx('e85a80')}
art('luna_plush', [
    "................",
    ".....oooooo.....",
    "....ohHHHhho....",
    "...ohHHhhhhho...",
    "...ohhssssshho..",
    "...ohsesssesho..",
    "...ohsrsssrsho..",
    "....ossssssso...",
    "....oppPPPppo...",
    "...opppPPPpppo..",
    "...ospppppppso..",
    "....opppppppo...",
    "....oPPo.oPPo...",
    ".....oo...oo....",
    "................",
    "................"], PL)

# лодка тира 2: перекраска дубовой в инеевую берёзу
src = Image.open(os.path.join(TEX, 'oak_boat_t1.png')).convert('RGBA')
out = Image.new('RGBA', src.size, (0, 0, 0, 0))
for y in range(src.height):
    for x in range(src.width):
        r, g, b, a = src.getpixel((x, y))
        if a == 0: continue
        l = (0.3 * r + 0.59 * g + 0.11 * b) / 255
        out.putpixel((x, y), (int(150 + 100 * l), int(190 + 60 * l), int(215 + 40 * l), a) if l > 0.25 else (40, 70, 100, a))
out.putpixel((7, 6), (234, 250, 255, 255)); out.putpixel((8, 6), (234, 250, 255, 255))
out.save(os.path.join(TEX, 'rime_boat_t2.png'))
json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/rime_boat_t2"}}, open(os.path.join(MOD, 'rime_boat_t2.json'), 'w'))

for egg in ['ash_zombie', 'withered', 'ash_archer', 'ash_spider', 'rotten_brute', 'frost_zombie', 'frost_archer', 'frost_spider']:
    json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(MOD, egg + '_spawn_egg.json'), 'w'))

# слои брони на модели игрока (64×32): инеевая сталь с меховой оторочкой
def armor_layers():
    import random
    rnd = random.Random(5)
    d = os.path.join(ROOT, 'textures', 'models', 'armor'); os.makedirs(d, exist_ok=True)
    def paint(im, boxes, base, fur=None):
        for (x0, y0, x1, y1) in boxes:
            for x in range(x0, x1):
                for y in range(y0, y1):
                    k = 0.85 + 0.3 * rnd.random()
                    c = tuple(min(255, int(v * k)) for v in base)
                    if (x + y) % 7 == 0: c = (234, 250, 255)
                    im.putpixel((x, y), c + (255,))
            if fur:
                for x in range(x0, x1): im.putpixel((x, y1 - 1), fur + (255,))
    l1 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    paint(l1, [(0, 0, 32, 16)], (106, 154, 191), (216, 200, 168))           # шлем
    for x in range(8, 16):
        for y in range(8, 16):
            if 10 <= y <= 13 and 9 <= x <= 14: l1.putpixel((x, y), (0, 0, 0, 0))   # лицо открыто
    paint(l1, [(16, 16, 40, 32)], (90, 140, 180), (216, 200, 168))          # кираса
    paint(l1, [(40, 16, 56, 32)], (106, 154, 191), (216, 200, 168))         # наплечи
    paint(l1, [(0, 16, 16, 32)], (74, 106, 138), (234, 250, 255))           # сапоги
    l1.save(os.path.join(d, 'rime_layer_1.png'))
    l2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    paint(l2, [(16, 16, 40, 32), (0, 16, 16, 32)], (74, 106, 138), (216, 200, 168))
    l2.save(os.path.join(d, 'rime_layer_2.png'))
armor_layers()
print('ok')
