"""Палитры объёмных деталей мобов этажа 3 (Mob3DLayer): 256×256, 16 плиток по 64×64, плитка = цвет.
Каждая плитка — цвет с лёгким пиксельным шумом и тёмными краями-прожилками, чтобы детали не выглядели «пластиком»."""
import random, os
from PIL import Image
OUT = 'src/main/resources/assets/pvpciv/textures/entity/mob3d'
PAL = {
    'samurai':  [0x7A1A1A, 0xD8B048, 0x1E1A1E, 0x55585E, 0xE8E0D0],
    'ninja':    [0x1A1A24, 0xC02040, 0xA8ACB4, 0x2E2E38],
    'ashigaru': [0x8A7A40, 0x3A2A1A, 0x8A2A1A, 0x6A4A2A, 0xD8D0B0],
    'werewolf': [0x4A3E34, 0x8A7A68, 0x141012, 0xE8E0C8, 0x7A1018],
    'kitsune':  [0xF4ECE0, 0xD02030, 0xE0782A, 0x1A1A1A, 0xD8B048],
    'botanist': [0xD8DCC8, 0x4AA83A, 0x2A5A1E, 0xF070B0, 0x5A5048],
    'thawed':   [0xA8E0F8, 0x5A9AC8, 0xF0FAFF, 0x1E3A5A, 0xF0C830],
    'robot':    [0x8A8E94, 0xFF8A20, 0x2A2C30, 0xC8CCD4, 0xE8C020],
    'chimera':  [0x6A2A5E, 0xA8F040, 0xE8DCC0, 0x241A22, 0xFF2020],
    'guard':    [0x22304E, 0x30D0FF, 0x34363C, 0xE8ECF0],
    'labtech':  [0xF0F2F6, 0xC040FF, 0x2A2A34, 0x60E0FF],
    'turret':   [0x4A4E56, 0x9AA0A8, 0xFF2020, 0x1A1A1E, 0xE8C020],
}
FUR = {'werewolf': (0, 1), 'kitsune': (2,), 'chimera': (0,)}
def rgb(c): return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
os.makedirs(OUT, exist_ok=True)
for name, pal in PAL.items():
    rnd = random.Random(name)
    im = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    px = im.load()
    for i, c in enumerate(pal):
        r0, g0, b0 = rgb(c)
        tx, ty = (i % 4) * 64, (i // 4) * 64
        fur = i in FUR.get(name, ())
        for y in range(64):
            for x in range(64):
                n = rnd.uniform(-0.07, 0.07)
                if fur and (x + (y // 2)) % 3 == 0: n -= 0.12          # волокна меха
                if not fur and rnd.random() < 0.04: n += 0.12            # блики лака/металла
                k = 1 + n
                px[tx + x, ty + y] = (max(0, min(255, int(r0 * k))), max(0, min(255, int(g0 * k))), max(0, min(255, int(b0 * k))), 255)
    im.save(f'{OUT}/{name}.png')
    print('ok', name)
