#!/usr/bin/env python3
"""Скины мобов этажа 4 (0.11, НТЦ «NaZa»). Раскладка 64×64 как у игрока; споровый ползун — перекраска паука 64×32."""
import os, random, importlib.util
from PIL import Image
spec = importlib.util.spec_from_file_location('s2h', os.path.join(os.path.dirname(__file__), 'skins2.py'))
src = open(spec.origin, encoding='utf-8').read().split('# ---------------------------------------------------------------- мобы')[0]
ns = {'__file__': spec.origin}; exec(src, ns)
hx, mul, skin, face, cloth, limb, ROOT = ns['hx'], ns['mul'], ns['skin'], ns['face'], ns['cloth'], ns['limb'], ns['ROOT']
SK = hx('e0b898'); PALE = hx('d8d0d8')

# Обезумевший ботаник: грязный халат, зелёные пятна, резиновые перчатки
coat = hx('d8dcc8')
skin('botanist', face(hx('c8c8a0'), hx('5a6a2a'), eye=hx('a0ff40'), hair_rows=2),
     cloth(coat, lambda x, y, w, h: hx('4aa83a') if (x * 7 + y * 3) % 11 == 0 else (hx('9a9a88') if x in (3, 4) else None), belt=hx('5a5048'), belt_row=10),
     limb(coat, cuff=hx('4a7a2a'), cuff_rows=1, hand=hx('3a8a2a'), hand_rows=3), limb(hx('4a4a40'), hand=hx('2a2a20'), hand_rows=2))

# Размороженный образец: синеватая кожа, больничная роба, иней
ice_skin = hx('9ac8e0')
def thawed_head(f, x, y, w, h):
    if f in ('top', 'back') or y < 1: return hx('d8f0ff') if (x + y) % 3 else hx('b0d8f0')
    if f == 'front' and y == 3 and x in (1, 2, 5, 6): return hx('f0faff') if x in (2, 5) else hx('1e3a5a')
    if f == 'front' and y == 6 and 2 <= x <= 5: return hx('3a5a7a')
    return ice_skin if (x * y) % 5 else mul(ice_skin, 1.2)
skin('thawed', thawed_head, cloth(hx('a8b8c8'), lambda x, y, w, h: hx('f0faff') if (x + y) % 6 == 0 else None),
     limb(ice_skin, hand=hx('c8e8f8'), hand_rows=2), limb(hx('8a9aa8'), hand=ice_skin, hand_rows=2))

# Робот-техник: сталь и швы
steel = hx('8a8e94')
skin('tech_robot', lambda f, x, y, w, h: hx('ff8a20') if f == 'front' and y == 3 and 1 <= x <= 6 else (mul(steel, 0.8) if y % 4 == 0 else steel),
     cloth(steel, lambda x, y, w, h: hx('2a2c30') if y % 3 == 0 else None, belt=hx('e8c020'), belt_row=10),
     limb(steel, stripe=hx('2a2c30'), hand=hx('2a2c30'), hand_rows=3), limb(mul(steel, 0.85), stripe=hx('2a2c30'), hand=hx('2a2c30'), hand_rows=2))

# Химера: фиолетовая шкура, кислотные пятна
hide, hide2 = hx('6a2a5e'), hx('4a1a42')
def chimera_head(f, x, y, w, h):
    if f == 'front' and y >= 6: return hx('241a22')
    return hide if (x * 3 + y) % 4 else hide2
skin('chimera', chimera_head, cloth(hide, lambda x, y, w, h: hx('a8f040') if (x * 5 + y * 7) % 13 == 0 else (hide2 if x in (3, 4) else None)),
     limb(hide, hand=hx('241a22'), hand_rows=3), limb(hide2, hand=hx('e8dcc0'), hand_rows=2))

# Охранник NaZa: тёмно-синяя форма
navy = hx('22304e')
skin('naza_guard', face(SK, hx('2a2018'), cap=navy, cap_rows=3),
     cloth(navy, lambda x, y, w, h: hx('30d0ff') if (x, y) == (2, 2) else (hx('34363c') if y > 8 else None), belt=hx('34363c')),
     limb(navy, hand=hx('1a1a20'), hand_rows=3), limb(hx('1a2238'), cuff=hx('34363c'), cuff_rows=3, hand=hx('1a1a20'), hand_rows=2))

# Лаборант: белый халат, фиолетовые перчатки
skin('lab_tech', face(SK, hx('6a4a2a'), hair_rows=2),
     cloth(hx('f0f2f6'), lambda x, y, w, h: hx('c040ff') if (x, y) == (5, 3) else (hx('c8ccd4') if x in (3, 4) else None), belt=hx('f0f2f6')),
     limb(hx('f0f2f6'), hand=hx('8a30c0'), hand_rows=3), limb(hx('2a2a34'), hand=hx('1a1a20'), hand_rows=2))

# Турель: прозрачный скин (видны только объёмные детали)
Image.new('RGBA', (64, 64), (0, 0, 0, 0)).save(os.path.join(ROOT, 'mob', 'turret.png'))

# Споровый ползун: паук в зелёно-жёлтой гамме
def ramp(stops, t):
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if t <= b:
            k = (t - a) / max(1e-6, b - a)
            return tuple(int(ca[i] + (cb[i] - ca[i]) * k) for i in range(3))
    return stops[-1][1]
SPORE = [(0, (14, 22, 8)), (0.35, (54, 90, 30)), (0.65, (120, 160, 50)), (0.85, (200, 220, 90)), (1, (240, 250, 180))]
im = Image.open(os.path.join(ROOT, 'mob', 'ash_spider.png')).convert('RGBA'); px = im.load(); rnd = random.Random(44)
for y in range(im.height):
    for x in range(im.width):
        r, g, b, a = px[x, y]
        if a == 0: continue
        if r > g + 50 and r > 120: px[x, y] = (230, 255, 80, a); continue
        c = ramp(SPORE, (0.3 * r + 0.59 * g + 0.11 * b) / 255)
        if rnd.random() < 0.05: c = (240, 120, 200)   # розовые споры
        px[x, y] = c + (a,)
im.save(os.path.join(ROOT, 'mob', 'sporeling.png'))
print('f4 skins ok')
