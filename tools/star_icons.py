#!/usr/bin/env python3
"""HD-иконки звёзд заточки (32×32): кристаллическая звезда в духе звезды Незера, но своя. Запуск: python3 tools/star_icons.py"""
import math, os
from PIL import Image, ImageDraw, ImageFilter
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv', 'textures', 'item')
S = 32; K = 8; N = S * K

def star(name, light, mid, dark, core, glow, rays=(14.5, 6.0), tilt=0.0):
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    c = N / 2
    # мягкое свечение
    g = Image.new('RGBA', (N, N), (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
    gd.ellipse([c - N * 0.3, c - N * 0.3, c + N * 0.3, c + N * 0.3], fill=glow + (110,))
    g = g.filter(ImageFilter.GaussianBlur(N * 0.08)); im.alpha_composite(g)
    d = ImageDraw.Draw(im)
    def ray(angle, length, width, col_l, col_d):
        a = math.radians(angle + tilt)
        tip = (c + math.cos(a) * length * K, c + math.sin(a) * length * K)
        l = (c + math.cos(a + math.pi / 2) * width * K, c + math.sin(a + math.pi / 2) * width * K)
        r = (c + math.cos(a - math.pi / 2) * width * K, c + math.sin(a - math.pi / 2) * width * K)
        d.polygon([l, tip, (c, c)], fill=col_l)        # освещённая грань
        d.polygon([r, tip, (c, c)], fill=col_d)        # теневая грань
    for i in range(4): ray(45 + i * 90, rays[1] + 3.5, 2.2, mid, dark)      # короткие диагональные
    for i in range(4): ray(i * 90 - 90, rays[0], 3.4, light, mid)          # длинные главные
    # огранённое ядро
    r0 = 4.2 * K
    d.polygon([(c, c - r0), (c + r0, c), (c, c + r0), (c - r0, c)], fill=core)
    d.polygon([(c, c - r0 * 0.55), (c + r0 * 0.55, c), (c, c + r0 * 0.1), (c - r0 * 0.3, c - r0 * 0.1)], fill=(255, 255, 255, 230))
    small = im.resize((S, S), Image.LANCZOS)
    # тёмный контур для читаемости в слоте
    px = small.load(); out = small.copy(); po = out.load()
    for y in range(S):
        for x in range(S):
            if px[x, y][3] < 90:
                near = any(0 <= x + dx < S and 0 <= y + dy < S and px[x + dx, y + dy][3] >= 160 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if near: po[x, y] = dark[:3] + (255,) if False else (24, 16, 28, 235)
    out.save(os.path.join(OUT, name + '.png'))

star('star_w1', (255, 236, 160, 255), (232, 176, 74, 255), (150, 96, 40, 255), (255, 214, 120, 255), (255, 190, 90))
star('blessed_star_w1', (255, 255, 255, 255), (170, 230, 255, 255), (90, 150, 210, 255), (220, 248, 255, 255), (150, 220, 255))
star('blood_star_w1', (255, 120, 130, 255), (200, 30, 50, 255), (90, 8, 22, 255), (60, 0, 10, 255), (220, 30, 50), tilt=0)
print('ok')
