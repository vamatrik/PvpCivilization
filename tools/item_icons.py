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
    paint(l1, [(0, 26, 16, 32), (8, 16, 12, 20)], (74, 106, 138), (234, 250, 255))   # сапоги: только низ ноги + подошва
    l1.save(os.path.join(d, 'rime_layer_1.png'))
    l2 = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    paint(l2, [(16, 26, 40, 32), (0, 20, 16, 26), (4, 16, 8, 20)], (74, 106, 138), (216, 200, 168))   # поножи: пояс + нога до колена
    l2.save(os.path.join(d, 'rime_layer_2.png'))
armor_layers()
print('ok')

# ---------------------------------------------------------------- 0.7: Хранительница Склепа — перекраска иконок Громилы в лёд
def recolor(src, dst, hue=0.56, sat=0.55, light=1.15):
    import colorsys
    im = Image.open(os.path.join(TEX, src + '.png')).convert('RGBA')
    px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if a == 0: continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hls_to_rgb(hue, min(1, l * light), min(1, s * 0.4 + sat))
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    im.save(os.path.join(TEX, dst + '.png'))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/" + dst}}, open(os.path.join(MOD, dst + '.json'), 'w'))

recolor('brute_crystal', 'keeper_crystal')
recolor('brute_trophy', 'keeper_trophy')

# 0.7: ядро брони и свиток закалки
CORE = {'o': hx('101828'), 'd': hx('2a4a7a'), 'm': hx('4a8ac8'), 'l': hx('8fd8ff'), 'w': hx('f0fcff'), 'g': hx('e8c45a'), 'G': hx('9a7a2a')}
art('armor_core', [
    "................",
    "......oooo......",
    "....ooGggGoo....",
    "...oGgddddgGo...",
    "..oGgdmmmmdgGo..",
    "..ogdmllllmdgo..",
    ".oGdmlwwwwlmdGo.",
    ".ogdmlwwwwlmdgo.",
    ".ogdmlwwwwlmdgo.",
    ".oGdmlwwwwlmdGo.",
    "..ogdmllllmdgo..",
    "..oGgdmmmmdgGo..",
    "...oGgddddgGo...",
    "....ooGggGoo....",
    "......oooo......",
    "................"], CORE)
SCR = {'o': hx('3a2a1a'), 'p': hx('e8dcc0'), 'P': hx('c8b890'), 'r': hx('b02030'), 'i': hx('6a5a4a'), 'b': hx('4a8ac8')}
art('temper_scroll', [
    "................",
    "..oooooooooo....",
    ".oPppppppppPo...",
    ".oPoooooooooPo..",
    "..opppppppppo...",
    "..opiiiiiiipo...",
    "..oppbbbbbppo...",
    "..opiiiiiiipo...",
    "..opppppppppo...",
    "..opiiiiirrpo...",
    "..oppppprrrro...",
    "..opppppprrro...",
    ".oPoooooooooPo..",
    ".oPppppppppPo...",
    "..oooooooooo....",
    "................"], SCR)

# ---------------------------------------------------------------- 0.8: этаж 2
for src, dst, hue, sat, light in (('brute_trophy', 'don_trophy', 0.12, 0.35, 0.9), ('brute_trophy', 'rat_king_trophy', 0.8, 0.45, 1.0),
                                  ('rime_shard', 'neon_tube', 0.9, 0.7, 1.2), ('frozen_locket', 'gold_chain', 0.13, 0.7, 1.1),
                                  ('armor_core', 'gas_filter', 0.28, 0.4, 1.0), ('ash_dust', 'rubber_scrap', 0.0, 0.0, 0.6),
                                  ('brute_crystal', 'don_ring', 0.0, 0.7, 1.0), ('keeper_crystal', 'rat_crown_shard', 0.13, 0.7, 1.1),
                                  ('rime_helmet', 'hazmat_helmet', 0.17, 0.55, 1.0), ('rime_chestplate', 'hazmat_chestplate', 0.17, 0.55, 1.0),
                                  ('rime_leggings', 'hazmat_leggings', 0.17, 0.55, 1.0), ('rime_boots', 'hazmat_boots', 0.17, 0.55, 1.0)):
    recolor(src, dst, hue, sat, light)

def recolor_layer(src, dst, hue, sat, light):
    import colorsys
    d = os.path.join(ROOT, 'textures', 'models', 'armor')
    im = Image.open(os.path.join(d, src)).convert('RGBA'); px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if a == 0: continue
            h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hls_to_rgb(hue, min(1, l * light), min(1, s * 0.4 + sat))
            px[x, y] = (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    im.save(os.path.join(d, dst))
recolor_layer('rime_layer_1.png', 'hazmat_layer_1.png', 0.17, 0.55, 1.0)
recolor_layer('rime_layer_2.png', 'hazmat_layer_2.png', 0.17, 0.55, 1.0)

PAR = {'o': hx('3a2010'), 'r': hx('e06020'), 'R': hx('a03a10'), 'w': hx('f0e8d8'), 'l': hx('8a8070'), 'b': hx('5a4a30')}
art('parachute', [
    "................",
    "....oooooooo....",
    "..oorrwwrrwwoo..",
    ".orrrwwrrwwrrro.",
    ".oRRRwwRRwwRRRo.",
    "..l..l....l..l..",
    "...l..l..l..l...",
    "....l..ll..l....",
    ".....l.ll.l.....",
    "......llll......",
    "......obbo......",
    ".....obbbbo.....",
    ".....obbbbo.....",
    "......oooo......",
    "................",
    "................"], PAR)

recolor('bag_buckle', 'smuggler_pack', 0.78, 0.55, 1.0)
recolor('rime_boat_t2', 'neon_taxi_t3', 0.9, 0.7, 1.05)

PH = {'o': hx('101014'), 's': hx('2a3a70'), 'S': hx('1a2440'), 'a': hx('b03040'), 'b': hx('40a050'), 'c': hx('e0b020'), 'w': hx('d0d8f0'), 'k': hx('606878')}
art('phone', [
    "................",
    ".....oooooo.....",
    "....oookoooo....",
    "....osssssso....",
    "....oswwwwso....",
    "....oSSSSSSo....",
    "....oSaaSbbo....",
    "....oSaaSbbo....",
    "....oSSSSSSo....",
    "....oSccSSSo....",
    "....oSccSwSo....",
    "....oSSSSSSo....",
    "....oSwSSSSo....",
    "....oookkooo....",
    ".....oooooo.....",
    "................"], PH)
