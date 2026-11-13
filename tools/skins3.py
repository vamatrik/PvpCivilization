#!/usr/bin/env python3
"""Скины NPC этажа 3 (0.9.0): библиотекарь и убийца. Раскладка 64×64 как у игрока."""
import os, random, importlib.util
from PIL import Image
spec = importlib.util.spec_from_file_location('s2h', os.path.join(os.path.dirname(__file__), 'skins2.py'))
src = open(spec.origin, encoding='utf-8').read().split('# ---------------------------------------------------------------- мобы')[0]
ns = {'__file__': spec.origin}; exec(src, ns)
hx, mul, skin, face, cloth, limb = ns['hx'], ns['mul'], ns['skin'], ns['face'], ns['cloth'], ns['limb']
SKIN = hx('e8c0a0'); PALE = hx('f0d8c8')
# Хранитель Сёдо: седой старик, тёмно-синее хаори с белым гербом-кругом, серая борода
gray = hx('b8b8c0')
skin('librarian', face(SKIN, gray, hair_rows=1, beard=gray, eye=hx('3a3a4a')),
     cloth(hx('1e2a4a'), lambda x, y, w, h: hx('e8e8f0') if (x - 3.5) ** 2 + (y - 2.5) ** 2 < 2.2 and (x, y) != (3, 2) else (hx('c8a86a') if x in (3, 4) and y > 5 else None), belt=hx('6a4a2a'), belt_row=7),
     limb(hx('1e2a4a'), cuff=hx('2a3a6a'), cuff_rows=2, hand=SKIN, hand_rows=2), limb(hx('3a3a44'), hand=hx('e8e8e8'), hand_rows=2), sub=None)
# Юки, Тихая тень: чёрные волосы, бледная кожа, алые глаза, тёмная маска на нижней половине лица, красный пояс
black, red, dark = hx('15151c'), hx('c02840'), hx('24202c')
def yuki_head(f, x, y, w, h):
    if f == 'top': return black
    if f == 'back': return black
    if f in ('left', 'right'): return black if y < 6 else PALE
    if f == 'bottom': return PALE
    if y < 2 or (y < 4 and x in (0, 7)): return black
    if y == 3 and x in (1, 2, 5, 6): return mul(black, 1.3)
    if y == 4 and x in (2, 5): return hx('e02040')
    if y == 4 and x in (1, 6): return hx('f4f0f0')
    if y >= 5: return dark if y != 5 else mul(dark, 1.2)   # маска
    return PALE
skin('assassin', yuki_head,
     cloth(dark, lambda x, y, w, h: hx('3a3048') if (x + y) % 4 == 0 and y < 7 else (mul(red, 0.8) if x == 3 and y < 5 else None), belt=red, belt_row=8),
     limb(dark, cuff=red, cuff_rows=1, hand=hx('2a2630'), hand_rows=2), limb(hx('1c1a22'), hand=black, hand_rows=3), sub=None)
print('ok')
# ---------------------------------------------------------------- 0.9.0 этап 3–4: мобы и повар
SK = hx('e0b898')
lac, lac2, gold = hx('7a1a1a'), hx('3a0e10'), hx('d8b048')
def kabuto(f, x, y, w, h):
    if f == 'front' and y >= 3: return SK if not (y == 4 and x in (2, 5)) else hx('201010')
    if f == 'front' and y == 2 and x in (1, 6): return gold
    if f in ('left', 'right') and y >= 5: return SK
    if f == 'bottom': return SK
    return lac2 if (y % 2 == 0) else lac
skin('samurai', kabuto, cloth(lac, lambda x, y, w, h: gold if y % 3 == 0 else (lac2 if x in (0, 7) else None), belt=hx('1a1a1a')),
     limb(lac2, cuff=lac, cuff_rows=3, hand=hx('2a2020'), hand_rows=2), limb(hx('2a2a30'), cuff=lac, cuff_rows=4, hand=hx('e8e0d0'), hand_rows=2))
nb = hx('1a1a24')
skin('ninja', face(SK, nb, eye=hx('c02040'), mask=nb, cap=nb, cap_rows=3),
     cloth(nb, lambda x, y, w, h: hx('2a2a3a') if (x + y) % 5 == 0 else None, belt=hx('5a1a2a')),
     limb(nb, cuff=hx('3a3a4a'), cuff_rows=2, hand=nb, hand_rows=2), limb(nb, cuff=hx('3a3a4a'), cuff_rows=3, hand=hx('2a2a30'), hand_rows=2))
skin('ashigaru', face(SK, hx('2a2018'), cap=hx('6a5a3a'), cap_rows=2),
     cloth(hx('4a3a24'), lambda x, y, w, h: hx('6a2a1a') if 2 <= y <= 5 else None, belt=hx('2a1a10')),
     limb(hx('5a4a30'), hand=SK, hand_rows=3), limb(hx('3a3020'), hand=hx('d8d0c0'), hand_rows=2))
wf, wf2 = hx('6a5a4a'), hx('4a3e34')
def wolf_head(f, x, y, w, h):
    if f == 'front':
        if y == 3 and x in (1, 6): return hx('ffd040')
        if y >= 5 and 2 <= x <= 5: return hx('2a2020') if y == 7 else hx('8a7a6a')
    return wf if (x + y) % 3 else wf2
skin('werewolf', wolf_head, cloth(wf, lambda x, y, w, h: wf2 if (x * y) % 4 == 0 else (hx('5a3a2a') if y > 8 else None)),
     limb(wf, hand=hx('2a2420'), hand_rows=2), limb(wf2, hand=hx('2a2420'), hand_rows=3))
fo, fw = hx('e0782a'), hx('f4ece0')
def kitsune_head(f, x, y, w, h):
    if f == 'front':
        if y < 2: return hx('1a1a1a')
        if y == 3 and x in (1, 2, 5, 6): return hx('d02030')        # маска кицунэ: алые глаза
        if y >= 2: return fw
    if f == 'top' or y < 2: return hx('1a1a1a')
    return fw if f != 'back' else hx('1a1a1a')
skin('kitsune_archer', kitsune_head, cloth(fw, lambda x, y, w, h: hx('d02030') if x in (3, 4) else (fo if y > 8 else None), belt=hx('d02030')),
     limb(fw, cuff=fo, cuff_rows=2, hand=hx('2a2020'), hand_rows=2), limb(hx('2a2a30'), hand=hx('e8e0d0'), hand_rows=2))
skin('chef', face(SK, hx('2a2018'), hair_rows=1, cap=hx('f4f4f4'), cap_rows=2),
     cloth(hx('f0f0f0'), lambda x, y, w, h: hx('2a3a6a') if y > 6 else (hx('d02030') if (x, y) == (3, 2) else None), belt=hx('2a3a6a')),
     limb(hx('f0f0f0'), hand=SK, hand_rows=3), limb(hx('2a2a30'), hand=hx('e8e0d0'), hand_rows=2), sub=None)
print('mobs ok')
