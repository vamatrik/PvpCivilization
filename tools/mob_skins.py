#!/usr/bin/env python3
"""Скины мобов: из пепельных делает инеевые (локация 2) и Громилу. Запуск: python3 tools/mob_skins.py"""
import os, random
from PIL import Image
D = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv', 'textures', 'entity', 'mob')

def ramp(stops, t):
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if t <= b:
            k = (t - a) / max(1e-6, b - a)
            return tuple(int(ca[i] + (cb[i] - ca[i]) * k) for i in range(3))
    return stops[-1][1]

ICE = [(0, (14, 22, 44)), (0.35, (48, 92, 140)), (0.65, (120, 182, 222)), (0.85, (200, 236, 250)), (1, (250, 254, 255))]
ROT = [(0, (16, 18, 10)), (0.4, (58, 70, 34)), (0.7, (112, 120, 60)), (1, (200, 190, 120))]

def remap(src, dst, stops, seed, frost=0.0, gamma=1.0, ember=(120, 240, 255)):
    im = Image.open(os.path.join(D, src)).convert('RGBA')
    px = im.load(); rnd = random.Random(seed)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0: continue
            if r > g + 50 and r > 120: px[x, y] = ember + (a,); continue   # угольки/глаза — светятся своим цветом
            l = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            l = min(1, l ** gamma)
            c = ramp(stops, l)
            if frost and rnd.random() < frost: c = tuple(min(255, v + 70) for v in c)
            px[x, y] = c + (a,)
    im.save(os.path.join(D, dst))

remap('ash_zombie.png', 'frost_zombie.png', ICE, 1, frost=0.06, gamma=0.8)
remap('ash_archer.png', 'frost_archer.png', ICE, 2, frost=0.05, gamma=0.7)
remap('ash_spider.png', 'frost_spider.png', ICE, 3, frost=0.08, gamma=0.9)
remap('ash_zombie.png', 'rotten_brute.png', ROT, 4, frost=0.0, gamma=1.1, ember=(220, 255, 90))
# глаза паука и зомби светятся — оставим яркими пиксели исходника, что были почти белыми/красными
print('ok')
