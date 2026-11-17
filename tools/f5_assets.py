#!/usr/bin/env python3
"""Ассеты этажа 5 (0.12): иконки, оружие тира 5 (3D), скафандр «Орбита», яйца призыва."""
import json, os, importlib.util, colorsys
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
TEX = os.path.join(ROOT, 'textures', 'item'); MOD = os.path.join(ROOT, 'models', 'item')
def hx(s, a=255): s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)
def art(name, rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.' and ch in pal: im.putpixel((x, y), pal[ch])
    im.save(os.path.join(TEX, name + '.png'))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/" + name}}, open(os.path.join(MOD, name + '.json'), 'w'))
def recolor(src, name, hue, sat=0.8, light=1.0):
    s = Image.open(os.path.join(TEX, src + '.png')).convert('RGBA'); o = s.copy()
    for y in range(o.height):
        for x in range(o.width):
            r, g, b, a = s.getpixel((x, y))
            if not a: continue
            h, l, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hls_to_rgb(hue, min(1, l * light), sat if ss > 0.08 else ss)
            o.putpixel((x, y), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
    o.save(os.path.join(TEX, name + '.png'))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/" + name}}, open(os.path.join(MOD, name + '.json'), 'w'))

art('cmp_lazuratron', [
    "................",
    "......oooo......",
    ".....owwlmo.....",
    "....owlllmmo....",
    "...owllmmmmdo...",
    "...olllmmmmdo...",
    "..owlllmmmddo...",
    "..olllmmmmddo...",
    "..olllmmmdddo...",
    "..olmmmmmddoo...",
    "...olmmmdddo....",
    "...oommmddoo....",
    ".....oooddo.....",
    ".......ooo......",
    "................",
    "................"], {'o': hx('060a24'), 'w': hx('f0f8ff'), 'l': hx('7a9cff'), 'm': hx('3a4aff'), 'd': hx('20208a')})
art('star_alloy', [
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...owwsssssso...",
    "..owsssssssgso..",
    ".owsssssssgssso.",
    ".osssssssssssso.",
    ".ossssddddddsso.",
    "..oddddddddddo..",
    "...oooooooooo...",
    "................",
    "................",
    "................",
    "................",
    "................"], {'o': hx('1a1c24'), 'w': hx('ffffff'), 's': hx('c8d0e8'), 'g': hx('f0d070'), 'd': hx('7a8098')})
art('void_shard', [
    "................",
    ".........o......",
    "........opo.....",
    ".......opwpo....",
    "......opwppo....",
    ".....oppwpo.....",
    "....oppppo......",
    "...opdppo.......",
    "..opddpo........",
    "..oddpo.........",
    "..oddo..........",
    "...oo...........",
    "................",
    "................",
    "................",
    "................"], {'o': hx('0a0614'), 'p': hx('6a3aaa'), 'w': hx('e0c8ff'), 'd': hx('2a1a4a')})
art('circuit', [
    "................",
    "................",
    "..oooooooooooo..",
    "..oggggggggggo..",
    "..ogyyyggcggggo.",
    "..oggggygcccggo.",
    "..ogbbgyggggggo.",
    "..ogbbgyyyyygo..",
    "..oggggggggygo..",
    "..ogccggrrgygo..",
    "..oggggrrgggo...",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
    "................"], {'o': hx('101410'), 'g': hx('2a6a3a'), 'y': hx('e8c84a'), 'c': hx('8ad0ff'), 'b': hx('1a1a1a'), 'r': hx('d04040')})
recolor('quantum_chip', 'astro_core', 0.12, 0.85, 1.05)
recolor('plasma_cell', 'quasar_cell', 0.5, 0.9, 1.1)
recolor('nano_pocket', 'grav_pouch', 0.76, 0.7)

for egg in ['android', 'xeno', 'void_crawler', 'rogue_astro', 'drone', 'collector']:
    json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(MOD, egg + '_spawn_egg.json'), 'w'))

spec = importlib.util.spec_from_file_location('wm', os.path.join(HERE, 'weapon_models.py'))
W = importlib.util.module_from_spec(spec); spec.loader.exec_module(W)
for w in [W.sword('photon_blade', length=17, width=2.4, core='#7AE8FF', edge='#FFFFFF', guard_w=5.6, guard_c='#E8ECF0', wood='#2A2C32', pommel='#30E0FF'),
          W.dagger('void_knife', length=9, width=1.8, core='#3A1A6A', edge='#C090FF', guard_c='#1A1424', wood='#0A0A12'),
          W.saber('comet_saber', length=18, width=1.9, curve=2.6, core='#FFB060', edge='#FFF4E0', guard_c='#E8C84A', wood='#3A2238', pommel='#FF7A30'),
          W.axe('meteor_axe', head='#4A3A34', edge='#FF7A30', wood='#2A2C32', band='#E8C84A', double=False),
          W.hammer('supernova', shaft=19, head='#E8C84A', face='#FFF0B0', dark='#3A2238', wood='#2A2C32', band='#FF7A30', accent='#FFFFFF', head_w=10, head_h=6)]:
    n, p = W.build(w); print(w.name, n)

spec = importlib.util.spec_from_file_location('at', os.path.join(HERE, 'armor_tex.py'))
src = open(spec.origin, encoding='utf-8').read().rsplit('\nsamurai(); fox()', 1)[0]
A = {'__file__': spec.origin}; exec(src, A)
P, faces, shade, c, HEAD, BODY, ARM, LEG = A['P'], A['faces'], A['shade'], A['c'], A['HEAD'], A['BODY'], A['ARM'], A['LEG']
OUT = os.path.join(ROOT, 'textures', 'models', 'armor') + '/'
white, orange, gold, gray, dark = c(0xF2F2F2), c(0xFF8A20), c(0xE0B040), c(0x8A9098), c(0x2A2C32)
p = P('orbit1')
faces(p, HEAD, lambda f, a: p.rect(*a, white, 0.03))
x, y, w, h = p.box(*HEAD)['front']; p.rect(x + 1, y + 2, w - 2, 4, gold, 0.02); p.hline(x, y + 7, w, gray)
faces(p, BODY, lambda f, a: p.rect(*a, white, 0.04))
x, y, w, h = p.box(*BODY)['front']; p.rect(x + 2, y + 3, 4, 3, dark, 0.02); p.rect(x + 3, y + 4, 1, 1, orange, 0); p.hline(x, y + 8, w, orange)
x, y, w, h = p.box(*BODY)['back']; p.rect(x + 1, y + 1, 6, 8, gray, 0.03)
faces(p, ARM, lambda f, a: p.rect(*a, white, 0.04))
for f in ('front', 'right', 'left', 'back'):
    x, y, w, h = p.box(*ARM)[f]; p.hline(x, y + 4, w, orange); p.rect(x, y + 9, w, 3, gray, 0.03)
faces(p, LEG, lambda f, a: p.rect(*a, gray, 0.04))
for f in ('front', 'right', 'left', 'back'):
    x, y, w, h = p.box(*LEG)[f]; p.clear(x, y, w, 7)
p.im.save(OUT + 'orbit_layer_1.png')
q = P('orbit2')
faces(q, LEG, lambda f, a: q.rect(*a, white, 0.04))
for f in ('front', 'right', 'left', 'back'):
    x, y, w, h = q.box(*LEG)[f]; q.hline(x, y + 5, w, orange); q.rect(x, y + 3, w, 1, gray, 0)
faces(q, BODY, lambda f, a: q.rect(*a, dark))
q.im.save(OUT + 'orbit_layer_2.png')
for part, hue in (('helmet', 0.08), ('chestplate', 0.08), ('leggings', 0.08), ('boots', 0.08)):
    recolor('exo_' + part, 'orbit_' + part, hue, 0.15, 1.08)
print('f5 assets ok')
