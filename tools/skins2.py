#!/usr/bin/env python3
"""Скины 64×64 (раскладка игрока/зомби) для мобов и NPC этажа 2. Запуск: python3 tools/skins2.py
Каждая часть тела — коробка с UV по стандартной схеме; художник грани получает (face, x, y, w, h) и возвращает цвет."""
import os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv', 'textures', 'entity')
R = random.Random(5)
def hx(s): s = s.lstrip('#'); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def mul(c, k): return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)
SH = {'top': 1.12, 'bottom': 0.7, 'front': 1.0, 'back': 0.88, 'right': 0.92, 'left': 0.92}

def box(im, u, v, w, h, d, paint):
    faces = {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
             'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}
    for f, (x0, y0, fw, fh) in faces.items():
        for y in range(fh):
            for x in range(fw):
                c = paint(f, x, y, fw, fh)
                if c: im.putpixel((x0 + x, y0 + y), mul(c, SH[f] * (0.94 + 0.1 * R.random())))

def skin(name, head, body, arm, leg, sub='mob'):
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    box(im, 0, 0, 8, 8, 8, head)
    box(im, 16, 16, 8, 12, 4, body)
    box(im, 40, 16, 4, 12, 4, arm); box(im, 32, 48, 4, 12, 4, arm)
    box(im, 0, 16, 4, 12, 4, leg); box(im, 16, 48, 4, 12, 4, leg)
    path = os.path.join(ROOT, sub, name + '.png') if sub else os.path.join(ROOT, name + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)

# ---------------------------------------------------------------- детали
def face(skin_c, hair_c, eye=hx('202020'), hair_rows=2, cap=None, cap_rows=0, mouth=hx('6a3a3a'), beard=None, mask=None):
    def p(f, x, y, w, h):
        if cap and (y < cap_rows or f == 'top'): return cap if not (f == 'front' and y == cap_rows - 1) else mul(cap, 0.6)
        if f == 'top': return hair_c
        if f == 'back': return hair_c if y < 6 else skin_c
        if f in ('left', 'right'): return hair_c if y < hair_rows + 1 or (f == 'right' and x < 2 and y < 5) or (f == 'left' and x > 5 and y < 5) else skin_c
        if f == 'bottom': return skin_c
        if y < hair_rows: return hair_c
        if mask and y >= 2 and y <= 7: return mask if not (y == 4 and x in (2, 5)) else hx('ffffff') if y == 4 else mask
        if y == 4 and x in (2, 5): return eye
        if y == 4 and x in (1, 6): return hx('f0f0f0')
        if y == 3 and x in (1, 2, 5, 6): return mul(hair_c, 0.8)
        if beard and y >= 5: return beard
        if y == 6 and 3 <= x <= 4: return mouth
        return skin_c
    return p

def cloth(base, front=None, belt=None, belt_row=10):
    def p(f, x, y, w, h):
        if belt and y == belt_row: return belt
        if front and f == 'front':
            c = front(x, y, w, h)
            if c: return c
        return base
    return p

def limb(base, cuff=None, cuff_rows=0, hand=None, hand_rows=0, stripe=None):
    def p(f, x, y, w, h):
        if f == 'bottom': return hand or cuff or base
        if hand and y >= h - hand_rows: return hand
        if cuff and y >= h - hand_rows - cuff_rows and y < h - hand_rows: return cuff
        if stripe and f in ('right', 'left') and x == 1: return stripe
        return base
    return p

# ---------------------------------------------------------------- мобы
SKIN = hx('e0b090'); SKIN2 = hx('c89070'); PALE = hx('9aa890')
navy, gold, black, white = hx('1e2a4a'), hx('e8c84a'), hx('1a1a1e'), hx('e8e8e8')
skin('police', face(SKIN, hx('3a2a1a'), cap=navy, cap_rows=3),
     cloth(navy, lambda x, y, w, h: gold if (x, y) == (2, 2) or (x, y) == (2, 3) else hx('c0c0c0') if x == 4 and y < 9 and y % 3 == 0 else None, belt=black),
     limb(navy, hand=SKIN, hand_rows=3), limb(hx('16203a'), hand=black, hand_rows=2))
skin('thug', face(SKIN, hx('2a2018'), hair_rows=1, mouth=hx('5a2a2a')),
     cloth(hx('2a2a30'), lambda x, y, w, h: white if x in (1, 6) else (gold if y == 1 and 2 <= x <= 5 else None)),
     limb(hx('2a2a30'), hand=SKIN, hand_rows=3, stripe=white), limb(hx('2a2a30'), hand=white, hand_rows=2, stripe=white))
skin('sewer_ghoul', face(PALE, hx('3a4a2a'), eye=hx('c0ff40'), mouth=hx('203010')),
     cloth(hx('4a5a3a'), lambda x, y, w, h: hx('6a8a2a') if (x * 3 + y * 5) % 7 == 0 else hx('2a3018') if (x + y) % 5 == 0 else None),
     limb(hx('4a5a3a'), hand=PALE, hand_rows=4), limb(hx('3a4430'), hand=hx('2a2a20'), hand_rows=3))
skin('gunner', face(SKIN2, black, mask=black),
     cloth(hx('3a3a2a'), lambda x, y, w, h: hx('5a5a40') if y in (2, 3, 6, 7) and x in (1, 2, 5, 6) else None, belt=hx('2a2018')),
     limb(hx('3a3a2a'), hand=black, hand_rows=3), limb(hx('2a2a20'), hand=black, hand_rows=3))
# ---------------------------------------------------------------- NPC
skin('timofey', face(SKIN, hx('6a4a2a'), hair_rows=1, beard=None),
     cloth(hx('d03030'), lambda x, y, w, h: white if 2 <= x <= 5 and 3 <= y <= 5 else None),
     limb(SKIN, cuff=hx('d03030'), cuff_rows=0), limb(hx('2a2a40'), hand=white, hand_rows=2, stripe=white), sub=None)
skin('parachutist', face(SKIN, hx('e0c060'), cap=hx('e07020'), cap_rows=2),
     cloth(hx('e07020'), lambda x, y, w, h: black if x in (1, 6) or y == 6 else None, belt=black),
     limb(hx('e07020'), hand=hx('3a3a3a'), hand_rows=2), limb(hx('5a5a3a'), hand=black, hand_rows=3), sub=None)
skin('plumber', face(SKIN, hx('4a3020'), cap=hx('2a6ad0'), cap_rows=2, beard=hx('4a3020')),
     cloth(hx('2a6ad0'), lambda x, y, w, h: hx('e04040') if y < 4 else (gold if (x, y) in ((1, 5), (6, 5)) else None)),
     limb(hx('e04040'), hand=white, hand_rows=3), limb(hx('2a6ad0'), hand=hx('5a3a20'), hand_rows=3), sub=None)
skin('smuggler', face(SKIN2, hx('1a1a1e'), mask=hx('3a2a4a'), cap=hx('2a1e36'), cap_rows=3),
     cloth(hx('2a1e36'), lambda x, y, w, h: gold if (x, y) in ((2, 1), (5, 1)) else hx('4a3a5a') if x in (3, 4) else None, belt=hx('5a3a20')),
     limb(hx('2a1e36'), hand=hx('1a1a1a'), hand_rows=3), limb(hx('1e1e24'), hand=hx('3a2a1a'), hand_rows=3), sub=None)
print('ok')
