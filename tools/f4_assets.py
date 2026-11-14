#!/usr/bin/env python3
"""Ассеты этажа 4 (0.11, НТЦ «NaZa»): иконки материалов/модулей/катализаторов/прибора Стеллы, 3D-оружие тира 4,
текстура экзокостюма, модели яиц призыва. Запуск: python3 tools/f4_assets.py"""
import json, os, sys, importlib.util
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

CRYSTAL = [
    "................",
    ".......o........",
    "......owo.......",
    "......olo...o...",
    ".....owlmo.owo..",
    ".....olmmo.olo..",
    "..o..olmmo.oldo.",
    ".owo.olmmdoolmo.",
    ".olo.olmmdolmdo.",
    ".olmoolmmdolmdo.",
    ".olmdolmmdolmdo.",
    "..omdolmmdolmo..",
    "..oodolmmdoooo..",
    "...ooommmdoo....",
    ".....oooooo.....",
    "................"]
art('lazuratron', CRYSTAL, {'o': hx('0a1640'), 'w': hx('e8f8ff'), 'l': hx('6ac8ff'), 'm': hx('2a6aff'), 'd': hx('1a3aa0')})
art('nano_scrap', [
    "................",
    "................",
    "....oo....oo....",
    "...oggo..ogso...",
    "..ogsgsooosggo..",
    "..osgggsgggsgo..",
    "...oggcgggggo...",
    "...ogggggcggo...",
    "..ogsgggggsgo...",
    "..ogggcgggggso..",
    "...osgggsgggo...",
    "....oggoosggo...",
    ".....oo..ooo....",
    "................",
    "................",
    "................"], {'o': hx('1a1c20'), 'g': hx('6a7078'), 's': hx('b8c0c8'), 'c': hx('40e0ff')})
art('biogel', [
    "................",
    "......oooo......",
    ".....ollllo.....",
    "....olwlllgo....",
    "...olwlllllgo...",
    "...ollllllggo...",
    "..olllllllgggo..",
    "..ollllllggggo..",
    "..olllllgggggo..",
    "..olllgggggddo..",
    "...ogggggdddo...",
    "....oogddddo....",
    "......oooo......",
    "................",
    "................",
    "................"], {'o': hx('1a3010'), 'l': hx('a8f060'), 'w': hx('f0ffd0'), 'g': hx('5ab830'), 'd': hx('2a7018')})
art('cryo_shard', [
    "................",
    "..........ow....",
    ".........owlo...",
    "........owllo...",
    ".......owlmo....",
    "......owlmo.....",
    ".....owlmmo.....",
    "....owlmmo......",
    "...owlmmo.......",
    "...olmmo........",
    "..olmmo.........",
    "..ommo..........",
    "..oo............",
    "................",
    "................",
    "................"], {'o': hx('1a3a5a'), 'w': hx('ffffff'), 'l': hx('c8f0ff'), 'm': hx('6ab8e8')})
CHIP = [
    "................",
    "...p.p.p.p.p....",
    "..oooooooooooo..",
    "..obbbbbbbbbbo..",
    ".pobccccccccbop.",
    "..obcggggggcbo..",
    ".pobcgwwwwgcbop.",
    "..obcgwqqwgcbo..",
    ".pobcgwqqwgcbop.",
    "..obcgwwwwgcbo..",
    ".pobcggggggcbop.",
    "..obccccccccbo..",
    "..obbbbbbbbbbo..",
    "..oooooooooooo..",
    "...p.p.p.p.p....",
    "................"]
art('quantum_chip', CHIP, {'o': hx('101418'), 'b': hx('1e3a2a'), 'c': hx('2a5a3a'), 'g': hx('40c070'), 'w': hx('a0ffd0'), 'q': hx('ffffff'), 'p': hx('d8b048')})
art('plasma_cell', [
    "................",
    "......oooo......",
    ".....ossso......",
    "....oooooooo....",
    "....ogvvvvgo....",
    "....ogvwwvgo....",
    "....ogvwpvgo....",
    "....ogvppvgo....",
    "....ogvwpvgo....",
    "....ogvppvgo....",
    "....ogvvvvgo....",
    "....oooooooo....",
    ".....ossso......",
    "......oooo......",
    "................",
    "................"], {'o': hx('1a1a22'), 's': hx('8a8e94'), 'g': hx('4a4e56'), 'v': hx('8a30e0'), 'p': hx('c060ff'), 'w': hx('f0d0ff')})
art('vine_heart', [
    "................",
    "...gg......gg...",
    "..gooo...oooog..",
    ".goorro.orrrog..",
    ".orrrrrorrwrro..",
    ".orrrrrrrrwrro..",
    ".orrrrrrrrrrro..",
    "..orrrrrrrrro.g.",
    "..gorrrrrrrog.g.",
    "...gorrrrrog.g..",
    "....gorrrog.g...",
    ".....gorogg.....",
    "......gog.......",
    ".......g........",
    "................",
    "................"], {'o': hx('3a0a14'), 'r': hx('c02040'), 'w': hx('ff90a0'), 'g': hx('3a8a2a')})
art('naza_trophy', [
    "................",
    "....oooooooo....",
    "...ocyyyyyyco...",
    "...oyyccccyyo...",
    "...oyc.gg.cyo...",
    "...oyc.gg.cyo...",
    "...oyyccccyyo...",
    "....oyyyyyyo....",
    ".....oyyyyo.....",
    "......oyyo......",
    "......oyyo......",
    ".....oyyyyo.....",
    "....oooooooo....",
    "....obbbbbbo....",
    "....oooooooo....",
    "................"], {'o': hx('2a2010'), 'y': hx('e8c84a'), 'c': hx('40d0ff'), 'g': hx('3ad040'), 'b': hx('22304e')})
art('naza_badge', [
    "................",
    "................",
    ".......ss.......",
    "......s..s......",
    "..oooooooooooo..",
    "..owwwwwwwwwwo..",
    "..owbbwwwwwwwo..",
    "..owbbwcccccwo..",
    "..owbbwwwwwwwo..",
    "..owwwwcccwwwo..",
    "..owccccccccwo..",
    "..owwwwwwwwwwo..",
    "..oooooooooooo..",
    "................",
    "................",
    "................"], {'o': hx('22304e'), 'w': hx('f0f2f6'), 'b': hx('8a6a4a'), 'c': hx('30a0e0'), 's': hx('c8ccd4')})
MODULE = [
    "................",
    "................",
    "...oooooooooo...",
    "..ommmmmmmmmmo..",
    "..omllllllllmo..",
    "..omlggggggl.o..",
    "..omlgwwwwglmo..",
    "..omlgwccwglmo..",
    "..omlgwccwglmo..",
    "..omlgwwwwglmo..",
    "..omlggggggl.o..",
    "..omllllllllmo..",
    "..ommmmmmmmmmo..",
    "...oooooooooo...",
    "....p..p..p.....",
    "................"]
for name, glow in [('module_basic', '40d0ff'), ('module_improved', '60ff90'), ('module_advanced', 'c060ff')]:
    g = hx(glow)
    art(name, MODULE, {'o': hx('101418'), 'm': hx('5a5e66'), 'l': hx('8a9098'), 'g': tuple(int(v * 0.6) for v in g[:3]) + (255,), 'w': g, 'c': hx('ffffff'), 'p': hx('d8b048')})
VIAL = [
    "................",
    "......oooo......",
    "......owwo......",
    ".......ww.......",
    "......owwo......",
    ".....occcco.....",
    "....occlcccо....",
    "...occlcccccо...",
    "...ocllccccco...",
    "...occcccccdo...",
    "...occcccccdo...",
    "....occccddo....",
    ".....oooooo.....",
    "................",
    "................",
    "................"]
VIAL = [r.replace('о', 'o') for r in VIAL]
for name, col in [('cat_focus', '40d0ff'), ('cat_anchor', 'e8c040'), ('cat_resonator', 'ff50c0')]:
    cc = hx(col)
    art(name, VIAL, {'o': hx('1a1a22'), 'w': hx('d8dce4'), 'c': cc, 'l': hx('ffffff'), 'd': tuple(int(v * 0.6) for v in cc[:3]) + (255,)})
art('phase_anchor', [
    "................",
    ".......oo.......",
    "......ocao......",
    ".....oc..ao.....",
    "....oc.ss.ao....",
    "...oc.sccs.ao...",
    "..oc.scwwcs.ao..",
    "..oa.scwwcs.co..",
    "...oa.sccs.co...",
    "....oa.ss.co....",
    ".....oa..co.....",
    "......oaco......",
    ".......oo.......",
    "......gggg......",
    ".....gg..gg.....",
    "................"], {'o': hx('1a1430'), 'c': hx('40d0ff'), 'a': hx('c060ff'), 's': hx('8a90a0'), 'w': hx('ffffff'), 'g': hx('d8b048')})

# ---------------------------------------------------------------- яйца призыва
for egg in ['botanist', 'sporeling', 'thawed', 'naza_guard', 'tech_robot', 'turret', 'chimera', 'lab_tech']:
    json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(MOD, egg + '_spawn_egg.json'), 'w'))

# ---------------------------------------------------------------- оружие тира 4 (3D)
spec = importlib.util.spec_from_file_location('wm', os.path.join(HERE, 'weapon_models.py'))
W = importlib.util.module_from_spec(spec); spec.loader.exec_module(W)
for w in [W.sword('plasma_cutter', length=16, width=2.6, core='#5A6070', edge='#FF7A30', guard_w=5.4, guard_c='#2A2C30', wood='#1A1A1E', pommel='#FF7A30'),
          W.dagger('ion_dagger', length=8.5, width=1.8, core='#40D0FF', edge='#E8FAFF', guard_c='#3A3E46', wood='#1A1A22'),
          W.saber('cryo_saber', length=17, width=1.8, curve=2.2, core='#8AD8FF', edge='#F4FDFF', guard_c='#C8CCD4', wood='#22304E', pommel='#40D0FF'),
          W.hammer('gravity_hammer', shaft=18, head='#3A3E46', face='#8A30E0', dark='#1A1A22', wood='#5A5E66', band='#C060FF', accent='#40D0FF', head_w=9.5, head_h=5.5),
          W.axe('vine_axe', head='#2A5A1E', edge='#A8F040', wood='#4A2E1E', band='#C02040', double=True)]:
    n, p = W.build(w); print(f"{w.name}: {n} элементов, {p} цветов")

# ---------------------------------------------------------------- экзокостюм (64×32, слои 1 и 2)
spec = importlib.util.spec_from_file_location('at', os.path.join(HERE, 'armor_tex.py'))
src = open(spec.origin, encoding='utf-8').read().rsplit('\nsamurai(); fox()', 1)[0]
A = {'__file__': spec.origin}; exec(src, A)
P, faces, shade, c, HEAD, BODY, ARM, LEG = A['P'], A['faces'], A['shade'], A['c'], A['HEAD'], A['BODY'], A['ARM'], A['LEG']
OUT = os.path.join(ROOT, 'textures', 'models', 'armor') + '/'
def exo():
    white, gray, dark, cyan, gold = c(0xE8ECF0), c(0x8A9098), c(0x24262C), c(0x30D0FF), c(0xD8B048)
    def panel(p, a, base):
        x, y, w, h = a
        p.rect(x, y, w, h, base, 0.04)
        for j in range(h):
            if j % 4 == 3: p.hline(x, y + j, w, shade(base, 0.75))
    p = P('exo1')
    faces(p, HEAD, lambda f, a: panel(p, a, white))
    x, y, w, h = p.box(*HEAD)['front']
    p.rect(x, y + 3, w, 2, cyan, 0); p.hline(x, y + 2, w, dark); p.hline(x, y + 5, w, dark)   # визор
    p.rect(x + 2, y + 6, 4, 2, gray, 0.03)
    faces(p, BODY, lambda f, a: panel(p, a, white))
    x, y, w, h = p.box(*BODY)['front']
    p.rect(x + 3, y + 3, 2, 2, cyan, 0); p.rect(x, y + 9, w, 1, gold, 0); p.vline(x + 1, y, 9, gray); p.vline(x + 6, y, 9, gray)
    x, y, w, h = p.box(*BODY)['back']; p.rect(x + 2, y + 1, 4, 7, dark, 0.03); p.rect(x + 3, y + 2, 2, 5, cyan, 0)
    faces(p, ARM, lambda f, a: panel(p, a, white))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*ARM)[f]; p.rect(x, y + 8, w, 4, dark, 0.03); p.hline(x, y + 3, w, cyan)
    faces(p, LEG, lambda f, a: panel(p, a, dark))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*LEG)[f]; p.clear(x, y, w, 6); p.hline(x, y + 8, w, cyan)
    p.im.save(OUT + 'exo_layer_1.png')
    q = P('exo2')
    faces(q, LEG, lambda f, a: panel(q, a, white))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = q.box(*LEG)[f]; q.rect(x, y + 4, w, 2, gray, 0.03); q.hline(x, y + 7, w, cyan)
    faces(q, BODY, lambda f, a: q.rect(*a, dark))
    q.im.save(OUT + 'exo_layer_2.png')
exo()
# иконки экзокостюма: перекраска иконок инеевой брони в бело-голубое
import colorsys
for part in ('helmet', 'chestplate', 'leggings', 'boots'):
    s = Image.open(os.path.join(TEX, 'rime_' + part + '.png')).convert('RGBA'); o = s.copy()
    for yy in range(16):
        for xx in range(16):
            r, g, b, a = s.getpixel((xx, yy))
            if a == 0: continue
            h, l, sat = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if l > 0.6: r2, g2, b2 = colorsys.hls_to_rgb(0.58, min(0.95, l * 1.05), 0.15)
            elif l > 0.3: r2, g2, b2 = colorsys.hls_to_rgb(0.53, l, 0.85)
            else: r2, g2, b2 = colorsys.hls_to_rgb(0.62, l * 0.8, 0.2)
            o.putpixel((xx, yy), (int(r2 * 255), int(g2 * 255), int(b2 * 255), a))
    o.save(os.path.join(TEX, 'exo_' + part + '.png'))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/exo_" + part}}, open(os.path.join(MOD, 'exo_' + part + '.json'), 'w'))
print('f4 assets ok')
