#!/usr/bin/env python3
"""Текстуры неба этажа 5 (0.12): планета внизу (Шпиль-мир), туманность, солнце. Детерминированно (seed)."""
import math, os, random
from PIL import Image, ImageFilter
OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv', 'textures', 'environment')
R = random.Random(505)

def noise2(w, h, oct=5, seed=1):
    rnd = random.Random(seed)
    acc = [[0.0] * w for _ in range(h)]
    amp, tot = 1.0, 0.0
    for o in range(oct):
        n = 4 * 2 ** o
        g = [[rnd.random() for _ in range(n + 1)] for _ in range(n + 1)]
        for y in range(h):
            fy = y / h * n; y0 = int(fy); ty = fy - y0; ty = ty * ty * (3 - 2 * ty)
            for x in range(w):
                fx = x / w * n; x0 = int(fx); tx = fx - x0; tx = tx * tx * (3 - 2 * tx)
                a = g[y0][x0] * (1 - tx) + g[y0][x0 + 1] * tx
                b = g[y0 + 1][x0] * (1 - tx) + g[y0 + 1][x0 + 1] * tx
                acc[y][x] += (a * (1 - ty) + b * ty) * amp
        tot += amp; amp *= 0.5
    return [[v / tot for v in row] for row in acc]

# планета: океаны, материки, облака, атмосферное свечение по краю, терминатор (ночная сторона)
S = 256
land, cloud = noise2(S, S, 6, 11), noise2(S, S, 5, 23)
im = Image.new('RGBA', (S, S), (0, 0, 0, 0)); px = im.load()
c = S / 2; rad = S * 0.42
for y in range(S):
    for x in range(S):
        dx, dy = (x - c) / rad, (y - c) / rad
        d = math.hypot(dx, dy)
        if d > 1.12: continue
        if d > 1:   # атмосфера
            a = int(255 * (1 - (d - 1) / 0.12) ** 2 * 0.7)
            px[x, y] = (90, 170, 255, a); continue
        z = math.sqrt(1 - d * d)
        l = land[y][x]
        if l > 0.53: col = (70 + int(60 * (l - 0.53) * 4), 110 + int(40 * (l - 0.53) * 4), 60)   # суша
        elif l > 0.5: col = (200, 190, 140)
        else: col = (20, 50 + int(60 * l), 120 + int(80 * l))
        cl = cloud[y][x]
        if cl > 0.55: k = min(1, (cl - 0.55) * 5); col = tuple(int(col[i] * (1 - k) + 240 * k) for i in range(3))
        light = max(0.08, min(1, 0.25 + 0.95 * (dx * -0.55 + dy * -0.35 + z * 0.75)))
        rim = (1 - z) ** 3 * 0.6
        col = tuple(min(255, int(col[i] * light + (90, 170, 255)[i] * rim)) for i in range(3))
        if light < 0.2 and l > 0.53 and R.random() < 0.02: col = (255, 210, 120)   # огни городов
        px[x, y] = col + (255,)
im.save(os.path.join(OUT, 'planet.png'))

# туманность: фиолетово-бирюзовая дымка (полупрозрачная, бесшовная по X не нужна — квадрат на небе)
S2 = 256
n1, n2 = noise2(S2, S2, 6, 31), noise2(S2, S2, 4, 47)
neb = Image.new('RGBA', (S2, S2)); q = neb.load()
for y in range(S2):
    for x in range(S2):
        dx, dy = (x - S2 / 2) / (S2 / 2), (y - S2 / 2) / (S2 / 2)
        fall = max(0, 1 - (dx * dx + dy * dy))
        v = max(0, n1[y][x] - 0.42) * 3 * fall
        t = n2[y][x]
        col = (int(150 + 100 * t), int(60 + 120 * (1 - t)), int(230))
        q[x, y] = col + (int(min(1, v) * 150),)
neb = neb.filter(ImageFilter.GaussianBlur(1.2))
neb.save(os.path.join(OUT, 'nebula.png'))

# солнце: белое ядро + корона
S3 = 64
sun = Image.new('RGBA', (S3, S3)); q = sun.load()
for y in range(S3):
    for x in range(S3):
        d = math.hypot(x - S3 / 2 + 0.5, y - S3 / 2 + 0.5) / (S3 / 2)
        if d < 0.28: q[x, y] = (255, 255, 245, 255)
        else:
            a = max(0, 1 - (d - 0.28) / 0.72) ** 2.2
            q[x, y] = (255, 230, 180, int(255 * a))
sun.save(os.path.join(OUT, 'space_sun.png'))
print('space sky ok')
