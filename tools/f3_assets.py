#!/usr/bin/env python3
"""Ассеты этажа 3 (0.9.0, этапы 3–4): скользкий камень, иконки материалов/еды/брони, слои брони, перекраска 3D-оружия, яйца."""
import json, os, colorsys, shutil, random
from PIL import Image, ImageDraw
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
TEX = os.path.join(ROOT, 'textures', 'item'); MOD = os.path.join(ROOT, 'models', 'item')
def hx(s, a=255): s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)
def save_icon(name, im):
    im.save(os.path.join(TEX, name + '.png'))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/" + name}}, open(os.path.join(MOD, name + '.json'), 'w'))
def canvas(): im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); return im, ImageDraw.Draw(im)
def outline(im, col=hx('1a1418')):
    px = im.load(); out = im.copy(); po = out.load()
    for x in range(16):
        for y in range(16):
            if px[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and px[x + dx, y + dy][3] > 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))): po[x, y] = col
    return out
# ---- скользкий камень
src = Image.open('/tmp/mc/assets/minecraft/textures/block/polished_andesite.png').convert('RGBA')
for x in range(16):
    for y in range(16):
        r, g, b, a = src.getpixel((x, y))
        k = 1.12 if (x + y) % 7 in (0, 1) else 1.0
        src.putpixel((x, y), (min(255, int(r * 0.92 * k)), min(255, int(g * 0.98 * k)), min(255, int(b * 1.1 * k)), a))
src.save(os.path.join(ROOT, 'textures', 'block', 'slick_stone.png'))
# ---- материалы
im, d = canvas(); d.ellipse([3, 3, 12, 12], fill=hx('8a5a3a')); d.ellipse([6, 6, 9, 9], fill=(0, 0, 0, 0)); d.point([(4, 6), (11, 9), (7, 3)], fill=hx('c07040')); save_icon('rusty_tsuba', outline(im))
im, d = canvas(); d.polygon([(2, 10), (8, 3), (14, 6), (9, 13)], fill=hx('c03048')); d.line([(4, 9), (10, 5)], fill=hx('ff8aa0')); d.line([(6, 11), (12, 7)], fill=hx('e8c45a')); save_icon('silk_scrap', outline(im))
im, d = canvas(); d.rectangle([4, 3, 11, 12], fill=hx('3aa86a')); d.rectangle([5, 4, 10, 6], fill=hx('7ae0a0')); d.rectangle([6, 8, 9, 11], fill=hx('e8c45a')); d.point([(7, 9), (8, 10)], fill=hx('8a2030')); save_icon('jade_seal', outline(im))
im, d = canvas(); d.ellipse([2, 5, 13, 11], fill=hx('e8e8d8')); d.ellipse([4, 6, 11, 10], fill=hx('5aaaff')); d.point([(6, 7), (9, 8)], fill=hx('e0f4ff')); save_icon('kappa_dish', outline(im))
im, d = canvas(); d.rectangle([5, 12, 10, 14], fill=hx('6a4a2a')); d.ellipse([3, 3, 12, 11], fill=hx('5aae4a')); d.ellipse([5, 3, 10, 5], fill=hx('5aaaff')); d.rectangle([6, 8, 9, 9], fill=hx('d8c048')); save_icon('kappa_trophy', outline(im))
im, d = canvas(); d.ellipse([2, 4, 13, 12], fill=hx('e0782a')); d.ellipse([9, 6, 14, 11], fill=hx('f4ece0')); d.line([(4, 7), (8, 9)], fill=hx('b05a1a')); save_icon('fox_fur', outline(im))
im, d = canvas(); d.ellipse([4, 5, 11, 13], fill=hx('3a7aff')); d.polygon([(5, 8), (8, 1), (10, 8)], fill=hx('5aa0ff')); d.ellipse([6, 8, 9, 11], fill=hx('e0f0ff')); save_icon('foxfire', outline(im))
im, d = canvas(); d.polygon([(2, 13), (6, 6), (11, 2), (14, 4), (10, 9), (5, 14)], fill=hx('f0a040')); d.polygon([(11, 2), (14, 4), (12, 6)], fill=hx('f8f4f0')); save_icon('kumiho_tail', outline(im))
im, d = canvas(); d.rectangle([5, 12, 10, 14], fill=hx('6a4a2a')); d.polygon([(3, 11), (5, 3), (7, 7), (8, 7), (10, 3), (12, 11)], fill=hx('f8f4f0')); d.point([(6, 9), (9, 9)], fill=hx('ffb030')); save_icon('kumiho_trophy', outline(im))
# ---- еда
im, d = canvas(); d.polygon([(3, 12), (8, 3), (13, 12)], fill=hx('f4f4f0')); d.rectangle([5, 9, 10, 12], fill=hx('1a2a1a')); save_icon('onigiri', outline(im))
im, d = canvas(); d.pieslice([2, 2, 13, 14], 0, 180, fill=hx('c03030')); d.rectangle([3, 7, 12, 8], fill=hx('f0d8a0')); d.line([(9, 1), (12, 7)], fill=hx('8a6a3a')); d.line([(11, 1), (13, 7)], fill=hx('8a6a3a')); d.point([(5, 7), (8, 7)], fill=hx('e8a040')); save_icon('ramen', outline(im))
im, d = canvas(); d.rectangle([2, 7, 7, 11], fill=hx('f4f4f0')); d.rectangle([2, 5, 7, 7], fill=hx('ff7a50')); d.rectangle([8, 7, 13, 11], fill=hx('f4f4f0')); d.rectangle([8, 6, 13, 7], fill=hx('1a2a1a')); d.rectangle([9, 5, 12, 6], fill=hx('e8c040')); save_icon('sushi', outline(im))
im, d = canvas(); d.ellipse([2, 6, 8, 12], fill=hx('ffb0c8')); d.ellipse([6, 4, 12, 10], fill=hx('f4f4f0')); d.ellipse([9, 7, 14, 12], fill=hx('a8e0a0')); save_icon('mochi', outline(im))
im, d = canvas()
for c in ((4, 6), (9, 6), (6, 10)): d.ellipse([c[0] - 2, c[1] - 2, c[0] + 2, c[1] + 2], fill=hx('c8803a')); d.point([(c[0], c[1] - 1)], fill=hx('5a3a1a'))
d.line([(2, 13), (14, 2)], fill=hx('d8c8a0')); save_icon('takoyaki', outline(im))
im, d = canvas(); d.rectangle([2, 4, 13, 12], fill=hx('2a1a1a')); d.rectangle([3, 5, 12, 11], fill=hx('8a1a2a')); d.rectangle([4, 6, 7, 10], fill=hx('f4f4f0')); d.rectangle([8, 6, 11, 8], fill=hx('ff7a50')); d.rectangle([8, 9, 11, 10], fill=hx('5aae4a')); d.line([(2, 4), (13, 4)], fill=hx('e8c45a')); save_icon('imperial_bento', outline(im))
# ---- броня: иконки и слои — перекраска инеевой
def recolor_img(path_in, path_out, fn):
    im = Image.open(path_in).convert('RGBA'); px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if a == 0: continue
            px[x, y] = fn(r, g, b, a)
    im.save(path_out)
def hue(h0, sat, lk=1.0):
    def fn(r, g, b, a):
        h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
        r2, g2, b2 = colorsys.hls_to_rgb(h0, min(1, l * lk), min(1, s * 0.3 + sat))
        return (int(r2 * 255), int(g2 * 255), int(b2 * 255), a)
    return fn
for part in ('helmet', 'chestplate', 'leggings', 'boots'):
    recolor_img(os.path.join(TEX, 'rime_' + part + '.png'), os.path.join(TEX, 'samurai_' + part + '.png'), hue(0.99, 0.6, 0.75))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/samurai_" + part}}, open(os.path.join(MOD, 'samurai_' + part + '.json'), 'w'))
    recolor_img(os.path.join(TEX, 'rime_' + part + '.png'), os.path.join(TEX, 'fox_' + part + '.png'), hue(0.07, 0.75, 1.0))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "pvpciv:item/fox_" + part}}, open(os.path.join(MOD, 'fox_' + part + '.json'), 'w'))
AR = os.path.join(ROOT, 'textures', 'models', 'armor')
for n in (1, 2):
    recolor_img(os.path.join(AR, 'rime_layer_%d.png' % n), os.path.join(AR, 'samurai_layer_%d.png' % n), hue(0.99, 0.6, 0.7))
    recolor_img(os.path.join(AR, 'rime_layer_%d.png' % n), os.path.join(AR, 'foxsuit_layer_%d.png' % n), hue(0.07, 0.75, 1.05))
# ---- оружие: перекраска 3D-моделей
W3 = os.path.join(TEX, 'w3d')
WEAP = {'nodachi': ('samurai_katana', 0.0, 0.05, 0.85), 'kama': ('ash_saber', 0.33, 0.35, 1.0), 'kunai': ('icicle_dagger', 0.72, 0.15, 0.6),
        'tetsubo': ('nail_bat', 0.08, 0.1, 0.55), 'kappa_naginata': ('keeper_spear', 0.07, 0.75, 1.0), 'fox_fang': ('bone_dagger', 0.08, 0.6, 1.05),
        'foxfire_blade': ('frost_rapier', 0.6, 0.85, 1.05), 'nine_tails': ('ash_saber', 0.06, 0.8, 1.1)}
for new, (base, h0, sat, lk) in WEAP.items():
    recolor_img(os.path.join(W3, base + '.png'), os.path.join(W3, new + '.png'), hue(h0, sat, lk))
    if os.path.exists(os.path.join(W3, base + '.png.mcmeta')): shutil.copy(os.path.join(W3, base + '.png.mcmeta'), os.path.join(W3, new + '.png.mcmeta'))
    m = json.load(open(os.path.join(MOD, base + '.json')))
    m['textures'] = {"0": "pvpciv:item/w3d/" + new, "particle": "pvpciv:item/w3d/" + new}
    m.pop('overrides', None)
    json.dump(m, open(os.path.join(MOD, new + '.json'), 'w'))
for egg in ('samurai', 'ninja', 'ashigaru', 'werewolf', 'fox_spirit', 'kitsune_archer'):
    json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(MOD, egg + '_spawn_egg.json'), 'w'))
print('f3 assets ok')
