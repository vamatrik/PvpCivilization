#!/usr/bin/env python3
"""Скины мобов этажа 5 (0.12, станция «Орбита»)."""
import os, random, importlib.util
from PIL import Image
spec = importlib.util.spec_from_file_location('s2h', os.path.join(os.path.dirname(__file__), 'skins2.py'))
src = open(spec.origin, encoding='utf-8').read().split('# ---------------------------------------------------------------- мобы')[0]
ns = {'__file__': spec.origin}; exec(src, ns)
hx, mul, skin, face, cloth, limb, ROOT = ns['hx'], ns['mul'], ns['skin'], ns['face'], ns['cloth'], ns['limb'], ns['ROOT']
SK = hx('e0b898')

white = hx('eef0f4')
skin('android', lambda f, x, y, w, h: hx('30e0ff') if f == 'front' and y == 3 else (mul(white, 0.92) if (x + y) % 5 == 0 else white),
     cloth(white, lambda x, y, w, h: hx('2a2c32') if x in (0, 7) else None, belt=hx('2a2c32'), belt_row=10),
     limb(white, stripe=hx('b8bec8'), hand=hx('2a2c32'), hand_rows=3), limb(mul(white, 0.9), hand=hx('2a2c32'), hand_rows=2))

chit = hx('16161c')
skin('xeno', lambda f, x, y, w, h: hx('2a2a36') if (x * 3 + y) % 4 == 0 else chit,
     cloth(chit, lambda x, y, w, h: hx('2a2a36') if y % 3 == 0 else None),
     limb(chit, stripe=hx('2a2a36'), hand=hx('d8d0b8'), hand_rows=2), limb(chit, stripe=hx('2a2a36'), hand=hx('2a2a36'), hand_rows=2))

suit = hx('f2f2f2')
skin('rogue_astro', face(hx('c8a890'), hx('3a2a1a'), eye=hx('ff6a3a'), hair_rows=2),
     cloth(suit, lambda x, y, w, h: hx('ff8a20') if y in (4,) else (hx('d8d8d8') if (x + y) % 4 == 0 else None), belt=hx('8a9098')),
     limb(suit, cuff=hx('ff8a20'), cuff_rows=1, hand=hx('8a9098'), hand_rows=3), limb(suit, cuff=hx('8a9098'), cuff_rows=3, hand=hx('8a9098'), hand_rows=2))

sc = hx('2a2238')
skin('collector', face(hx('b8c0a8'), hx('1a1420'), eye=hx('e8c84a'), hair_rows=2),
     cloth(sc, lambda x, y, w, h: hx('f0f0f0') if 3 <= x <= 4 and y < 4 else None, belt=hx('1a1420')),
     limb(sc, cuff=hx('f0f0f0'), cuff_rows=1, hand=hx('b8c0a8'), hand_rows=2), limb(hx('1a1420'), hand=hx('0a0a10'), hand_rows=2))

Image.new('RGBA', (64, 64), (0, 0, 0, 0)).save(os.path.join(ROOT, 'mob', 'drone.png'))

def ramp(stops, t):
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if t <= b:
            k = (t - a) / max(1e-6, b - a); return tuple(int(ca[i] + (cb[i] - ca[i]) * k) for i in range(3))
    return stops[-1][1]
VOID = [(0, (6, 4, 14)), (0.4, (40, 20, 70)), (0.7, (90, 50, 150)), (0.9, (180, 120, 255)), (1, (240, 220, 255))]
im = Image.open(os.path.join(ROOT, 'mob', 'ash_spider.png')).convert('RGBA'); px = im.load(); rnd = random.Random(55)
for y in range(im.height):
    for x in range(im.width):
        r, g, b, a = px[x, y]
        if a == 0: continue
        if r > g + 50 and r > 120: px[x, y] = (200, 140, 255, a); continue
        c = ramp(VOID, (0.3 * r + 0.59 * g + 0.11 * b) / 255)
        if rnd.random() < 0.04: c = (255, 255, 255)
        px[x, y] = c + (a,)
im.save(os.path.join(ROOT, 'mob', 'void_crawler.png'))
print('f5 skins ok')
