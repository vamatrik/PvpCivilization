#!/usr/bin/env python3
"""
Боссы этажа 1 (GeckoLib):
  • Гнилой Громила — сгорбленный гнилой огр ~3.3 блока с ржавым тесаком, костяные шипы на спине, светящиеся глаза.
  • Хранительница Склепа — ледяная королева (аниме-стиль, как Луна/Кармилла): белые волосы, ледяная корона,
    длинное платье, тёмный плащ, ледяное копьё. Левитирует.
Анимации: brute.animation.json (idle walk attack slam swipe stomp roar), keeper.animation.json (idle walk attack cast raise slam blink).
Запуск: python3 tools/npc/bosses.py
"""
import json, math, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G

G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, mix, strands = G.hexc, G.flat, G.mul, G.mix, G.strands


# ------------------------------------------------------------------ художники
def rotten(base, seed=1, spots=None):
    """Гнилая кожа: пятна, прожилки, шрамы."""
    spots = spots or mul(base, 0.7)
    def paint(fc, d, r):
        x0, y0, x1, y1 = r
        rnd = random.Random(seed * 31 + hash(fc) % 97)
        k = {'up': 1.1, 'down': 0.7, 'north': 1.0, 'south': 0.88, 'east': 0.85, 'west': 0.85}[fc]
        for x in range(x0, x1):
            for y in range(y0, y1):
                d.point((x, y), mul(base, k * (1 + rnd.uniform(-0.07, 0.07))))
        W, H = x1 - x0, y1 - y0
        for _ in range(max(1, W * H // 110)):
            cx, cy, rr = rnd.randint(x0, x1 - 1), rnd.randint(y0, y1 - 1), rnd.uniform(0.6, 2.2)
            d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=mul(spots, k * rnd.uniform(0.85, 1.1)))
        for _ in range(max(0, W * H // 160)):   # шрамы-швы
            ax, ay = rnd.randint(x0, x1 - 1), rnd.randint(y0, y1 - 1)
            bx, by = ax + rnd.randint(-5, 5), ay + rnd.randint(-5, 5)
            d.line([(ax, ay), (bx, by)], fill=mul(hexc('5a2a2a'), k), width=1)
    return paint


def brute_face(skin):
    base = rotten(skin, seed=5)
    def paint(fc, d, r):
        base(fc, d, r)
        if fc != 'north': return
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        # надбровье
        d.rectangle([x0, y0 + H * 0.28, x1 - 1, y0 + H * 0.36], fill=mul(skin, 0.6))
        # глаза: светятся оранжевым
        for ex in (0.27, 0.73):
            cx, cy = x0 + W * ex, y0 + H * 0.45
            d.ellipse([cx - W * 0.11, cy - H * 0.07, cx + W * 0.11, cy + H * 0.07], fill=(40, 10, 5, 255))
            d.ellipse([cx - W * 0.07, cy - H * 0.045, cx + W * 0.07, cy + H * 0.045], fill=(255, 140, 30, 255))
            d.ellipse([cx - W * 0.03, cy - H * 0.02, cx + W * 0.03, cy + H * 0.02], fill=(255, 240, 160, 255))
        # пасть с клыками
        my0, my1 = y0 + H * 0.66, y0 + H * 0.88
        d.rectangle([x0 + W * 0.15, my0, x1 - W * 0.15, my1], fill=(35, 10, 10, 255))
        for i in range(6):
            tx = x0 + W * (0.18 + i * 0.12)
            d.polygon([(tx, my0), (tx + W * 0.07, my0), (tx + W * 0.035, my0 + H * (0.12 if i in (0, 5) else 0.06))], fill=(225, 215, 180, 255))
            d.polygon([(tx, my1), (tx + W * 0.07, my1), (tx + W * 0.035, my1 - H * 0.05)], fill=(205, 195, 160, 255))
    return paint


def metal(base, seed=3, rust=True):
    def paint(fc, d, r):
        x0, y0, x1, y1 = r
        rnd = random.Random(seed + hash(fc) % 53)
        k = {'up': 1.15, 'down': 0.7, 'north': 1.0, 'south': 0.9, 'east': 0.85, 'west': 0.85}[fc]
        for x in range(x0, x1):
            for y in range(y0, y1):
                c = base
                if rust and rnd.random() < 0.18: c = mix(base, hexc('8a4a22'), rnd.uniform(0.4, 0.9))
                d.point((x, y), mul(c, k * (1 + rnd.uniform(-0.06, 0.06))))
        W, H = x1 - x0, y1 - y0
        if W > 6 and H > 6:   # заклёпки
            for (px, py) in ((x0 + 2, y0 + 2), (x1 - 3, y0 + 2), (x0 + 2, y1 - 3), (x1 - 3, y1 - 3)):
                d.point((px, py), mul(base, 1.5))
    return paint


def blade(fc, d, r):
    """Тесак: ржавая сталь, светлая заточенная кромка снизу, зазубрины, засохшая кровь."""
    x0, y0, x1, y1 = r
    rnd = random.Random(77 + hash(fc) % 13)
    W, H = x1 - x0, y1 - y0
    for x in range(x0, x1):
        for y in range(y0, y1):
            t = (y - y0) / max(1, H - 1)
            c = mix(hexc('6e6a64'), hexc('3e3a36'), 1 - t)
            if rnd.random() < 0.12: c = mix(c, hexc('7a3a1a'), 0.7)
            if rnd.random() < 0.05: c = hexc('5a1010')
            if fc in ('east', 'west', 'north', 'south') and t > 0.86: c = hexc('c8c4bc')
            d.point((x, y), c)


def ice(base, seed=9, glow=0.25):
    """Лёд: светлые грани-кристаллы, трещины, лёгкое свечение у краёв."""
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        rnd = random.Random(seed + hash(fc) % 71)
        for x in range(x0, x1):
            for y in range(y0, y1):
                u, v = (x - x0) / max(1, W - 1), (y - y0) / max(1, H - 1)
                e = min(u, 1 - u, v, 1 - v)
                c = mix(base, hexc('ffffff'), glow * (1 - min(1, e * 6)))
                d.point((x, y), mul(c, 1 + rnd.uniform(-0.05, 0.05)))
        for _ in range(max(1, W * H // 90)):
            ax, ay = rnd.randint(x0, x1 - 1), rnd.randint(y0, y1 - 1)
            d.line([(ax, ay), (ax + rnd.randint(-4, 4), ay + rnd.randint(-4, 4))], fill=mix(base, hexc('ffffff'), 0.7), width=1)
    return paint


def gown(top, bottom, trim, seed=4):
    """Платье: градиент от белого к ледяному, узор-снежинки и серебряная кайма снизу."""
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        rnd = random.Random(seed + hash(fc) % 41)
        k = {'up': 1.1, 'down': 0.7, 'north': 1.0, 'south': 0.92, 'east': 0.88, 'west': 0.88}[fc]
        for x in range(x0, x1):
            for y in range(y0, y1):
                t = (y - y0) / max(1, H - 1) if fc not in ('up', 'down') else 0.5
                c = mix(top, bottom, t)
                if (x - x0) % 6 == 0: c = mul(c, 0.93)   # складки
                if fc not in ('up', 'down') and t > 0.9: c = trim
                d.point((x, y), mul(c, k * (1 + rnd.uniform(-0.03, 0.03))))
        if fc in ('north', 'south', 'east', 'west'):
            for _ in range(max(1, W * H // 120)):
                cx, cy = rnd.randint(x0 + 2, max(x0 + 2, x1 - 3)), rnd.randint(y0 + 2, max(y0 + 2, y1 - 3))
                for a in range(3):
                    dx, dy = math.cos(a * math.pi / 3) * 1.6, math.sin(a * math.pi / 3) * 1.6
                    d.line([(cx - dx, cy - dy), (cx + dx, cy + dy)], fill=mix(trim, hexc('ffffff'), 0.5), width=1)
    return paint


# ------------------------------------------------------------------ Громила
def brute():
    m = G.Model('rotten_brute_boss', 1024)
    skin, dark = hexc('76845a'), hexc('4c5638')
    sk = rotten(skin, seed=1, spots=dark)
    sk2 = rotten(mul(skin, 0.92), seed=2, spots=dark)
    cloth = flat(hexc('4a3a2a'), noise=0.12)
    bone = flat(hexc('e0d6b8'), noise=0.08)
    iron = metal(hexc('6a6660'))
    m.bone('root', None, [0, 0, 0])
    # ноги: толстые, короткие, с обмотками
    for side, nm in ((-1, 'right_leg'), (1, 'left_leg')):
        x = 5.5 * side
        m.bone(nm, 'root', [x, 17, 0], [([x - 3.8, 0, -3.8], [7.6, 17, 7.6], sk2, 2),
                                         ([x - 4.1, 7, -4.1], [8.2, 3, 8.2], cloth, 2),
                                         ([x - 4.2, 0, -5.2], [8.4, 3.2, 9.6], flat(hexc('3a3028'), noise=0.1), 2)])
    # туловище: брюхо + набедренная повязка + пояс-цепь
    m.bone('body', 'root', [0, 17, 0], [([-10, 16, -6.5], [20, 15, 13], sk, 2),
                                        ([-10.5, 15, -7], [21, 4, 14], flat(hexc('3e3226'), noise=0.15), 2),
                                        ([-6, 8, -7.2], [12, 8, 0.8], cloth, 2),
                                        ([-6, 8, 6.4], [12, 7, 0.8], cloth, 2),
                                        ([-10.6, 17.2, -7.1], [21.2, 1.4, 14.2], iron, 3)])
    # грудь: широкая, наклонена вперёд (горб)
    chest = [([-12, 29, -7.5], [24, 13, 15], sk, 2),
             ([-12.5, 33, 7.0], [25, 6, 2], sk2, 2)]
    # костяные шипы на спине
    for i, (x, y, h) in enumerate(((-6, 38, 6), (0, 40, 8), (6, 38, 6), (-3, 33, 5), (3, 33, 5))):
        chest.append(([x - 1, y, 6.5], [2, h, 2], bone, 3, ([x, y, 7.5], [-35, 0, (i - 2) * 8])))
    # ржавый наплечник слева
    chest += [([10, 39, -6], [8, 4, 12], iron, 2), ([11, 37, -6.5], [7, 2.5, 13], iron, 2)]
    m.bone('chest', 'body', [0, 30, 0], chest, rotation=[16, 0, 0])
    # голова: маленькая, выдвинута вперёд, под горбом; ржавый шлем-обруч
    m.bone('head', 'chest', [0, 41, -5], [([-4.5, 39, -11.5], [9, 9, 9], brute_face(skin), 10),
                                          ([-5, 46, -12], [10, 2.2, 10], iron, 4),
                                          ([-1, 44, -12.4], [2, 3.5, 0.8], iron, 6),
                                          ([-6.4, 41, -9], [1.6, 3, 2.4], sk2, 6),   # уши
                                          ([4.8, 41, -9], [1.6, 3, 2.4], sk2, 6)])
    # руки: огромные, свисают почти до земли
    for side, nm in ((-1, 'right_arm'), (1, 'left_arm')):
        x = 15.5 * side
        cubes = [([x - 4.5, 24, -4.5], [9, 16, 9], sk2, 2),                       # плечо
                 ([x - 4, 11, -4], [8, 13, 8], sk2, 2),                           # предплечье
                 ([x - 4.4, 13, -4.4], [8.8, 4, 8.8], cloth, 2),                  # обмотка
                 ([x - 5, 4, -5], [10, 8, 10], rotten(mul(skin, 0.8), seed=4, spots=dark), 2)]                             # кулак
        m.bone(nm, 'chest', [x, 37, 0], cubes)
    # тесак в правой руке: рукоять в кулаке, широкое лезвие вперёд
    hx, hy = -15.5, 8
    m.bone('cleaver', 'right_arm', [hx, hy, 0], [
        ([hx - 1, hy - 1, -9], [2, 2, 12], flat(hexc('3a2a1a'), noise=0.15), 4),
        ([hx - 1.4, hy - 1.4, -10], [2.8, 2.8, 1.2], iron, 4),
        ([hx - 0.8, hy - 9, -30], [1.6, 12, 21], blade, 3),
        ([hx - 0.9, hy - 1, -30], [1.8, 2.5, 21], iron, 3),
    ], rotation=[-20, 0, 0])
    m.save('rotten_brute_boss')


# ------------------------------------------------------------------ Хранительница Склепа
def keeper():
    m = G.Model('crypt_keeper', 1024)
    skin = hexc('e8eef8'); hair = hexc('eef4ff')
    navy, ice_c, silver, white = hexc('1a2a4a'), hexc('8fd8ff'), hexc('c8d8ea'), hexc('f6f9ff')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('4ac8ff'), sex='f', expr='stern', bangs='hime',
                             brow=hexc('aab8d0'), lash=hexc('1a2a4a'), blush_c=(150, 200, 255, 90)), long_hair=True)
    icep = ice(ice_c)

    def extra(m):
        hp = strands(mul(hair, 1.0), mix(hair, hexc('3aa0ef'), 0.7), seed=51)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.3, -4.9], [10.2, 2.9, 10.2], hp, 4),
                                             ([-5.3, 19.5, 2.6], [10.6, 11, 2.4], hp, 4),
                                             ([-5.6, 21.5, -3.0], [1.3, 9, 6.2], hp, 4), ([4.3, 21.5, -3.0], [1.3, 9, 6.2], hp, 4)])
        m.bone('hair_mid', 'hair', [0, 20, 4.2], [([-5.4, 10.5, 2.8], [10.8, 9.5, 2.4], hp, 4)])
        tips = []
        for x, ln, ang in ((-5, 6, 5), (-2.4, 8, 2), (0.2, 9, 0), (2.8, 7, -3)):
            tips.append(([x, 10.6 - ln, 2.9], [2.6, ln, 2.0], hp, 4, ([x + 1.3, 10.6, 3.9], [0, 0, ang])))
        m.bone('hair_tip', 'hair_mid', [0, 10.6, 3.9], tips)
        m.bone('lock_r', 'head', [-4.6, 27, -3.9], [([-5.7, 17.5, -4.8], [1.6, 10.5, 2.0], hp, 4)])
        m.bone('lock_l', 'head', [4.6, 27, -3.9], [([4.1, 17.5, -4.8], [1.6, 10.5, 2.0], hp, 4)])
        # ледяная корона: обруч + зубцы
        crown = [([-5.3, 30.6, -5.3], [10.6, 1.2, 10.6], flat(silver, noise=0.06), 6)]
        for i, (x, h) in enumerate(((-4, 3), (-2, 4.5), (0, 6), (2, 4.5), (4, 3))):
            crown.append(([x - 0.5 + 0.5, 31.6, -5.6], [1.0, h, 1.0], icep, 8, ([x + 0.5, 31.6, -5.1], [-8, 0, (i - 2) * -6])))
        crown.append(([-0.6, 32.0, -5.9], [1.2, 1.2, 0.6], flat(hexc('3ab0ff'), noise=0.05), 10))   # самоцвет
        m.bone('crown', 'head', [0, 31, 0], crown)
        # длинное платье до пола (закрывает ноги), шлейф
        m.bone('skirt', 'body', [0, 13, 0], [([-4.6, 0.2, -3.2], [9.2, 13, 6.4], gown(hexc('e6f2ff'), hexc('2f6fb8'), silver), 3),
                                             ([-5.2, 0.0, -3.6], [10.4, 3, 7.2], gown(hexc('9ccff5'), hexc('1f4f90'), silver, seed=6), 3)])
        # плащ за спиной
        m.bone('cape', 'body', [0, 21.5, 2], [([-4.6, 2, 1.8], [9.2, 19.5, 0.8], gown(navy, mul(navy, 0.6), silver, seed=8), 3)], rotation=[6, 0, 0])
        # воротник-льдинки на плечах
        m.bone('collar', 'body', [0, 21, 0], [([-4.2, 20.0, -1.9], [8.4, 1.6, 3.8], icep, 4),
                                              ([-4.8, 20.6, -1.4], [1.2, 2.2, 2.8], icep, 6), ([3.6, 20.6, -1.4], [1.2, 2.2, 2.8], icep, 6)])
        # наручи
        m.bone('cuff_r', 'right_arm', [-4, 20.5, 0], [([-5.25, 12.2, -1.25], [2.5, 2.4, 2.5], icep, 4)])
        m.bone('cuff_l', 'left_arm', [4, 20.5, 0], [([2.75, 12.2, -1.25], [2.5, 2.4, 2.5], icep, 4)])
        # ледяное копьё в правой руке: древко вертикально, наконечник вверху
        hx = -3.95
        m.bone('spear', 'right_arm', [hx, 12.8, 0], [
            ([hx - 0.4, 1, -0.4], [0.8, 33, 0.8], flat(silver, noise=0.08), 8),
            ([hx - 0.9, 33.5, -0.9], [1.8, 1.2, 1.8], flat(hexc('3ab0ff'), noise=0.05), 8),
            ([hx - 0.8, 34.5, -0.3], [1.6, 7, 0.6], icep, 8),
            ([hx - 0.45, 41.3, -0.2], [0.9, 2.5, 0.4], icep, 8),
            ([hx - 1.6, 34.2, -0.25], [3.2, 1.0, 0.5], icep, 8),
        ])

    gw = gown(hexc('2a4a7a'), hexc('1a2f55'), silver, seed=2)
    G.humanoid(m, skin, face, gw, G.split_v(flat(white), flat(skin, noise=0.01), 0.55), flat(white), flat(silver), fem=True,
               top=G.neckline(gw, flat(skin, noise=0.01), silver, 'v', front=0.5, scallop=False), bust=gw, extra=extra)
    m.save('crypt_keeper')


# ------------------------------------------------------------------ анимации
def keys(fn, length, n=8):
    return {str(round(length * k / n, 3)): [round(v, 2) for v in fn(k / n)] for k in range(n + 1)}


def brute_anims():
    S = lambda t: math.sin(2 * math.pi * t)
    A = {}
    breath = {'chest': {'rotation': keys(lambda t: [1.5 * S(t), 0, 0], 2.4), 'scale': keys(lambda t: [1, 1 + 0.015 * S(t), 1], 2.4)},
              'head': {'rotation': keys(lambda t: [2 * S(t), 6 * S(t / 2 + 0.25) if False else 4 * S(t), 0], 2.4)},
              'right_arm': {'rotation': keys(lambda t: [3 * S(t), 0, -2 * S(t)], 2.4)},
              'left_arm': {'rotation': keys(lambda t: [-3 * S(t), 0, 2 * S(t)], 2.4)}}
    A['idle'] = {'loop': True, 'animation_length': 2.4, 'bones': breath}
    A['walk'] = {'loop': True, 'animation_length': 1.3, 'bones': {
        'right_leg': {'rotation': keys(lambda t: [28 * S(t), 0, 0], 1.3)},
        'left_leg': {'rotation': keys(lambda t: [-28 * S(t), 0, 0], 1.3)},
        'right_arm': {'rotation': keys(lambda t: [-18 * S(t), 0, 0], 1.3)},
        'left_arm': {'rotation': keys(lambda t: [18 * S(t), 0, 0], 1.3)},
        'body': {'rotation': keys(lambda t: [0, 0, 4 * S(t)], 1.3), 'position': keys(lambda t: [0, -abs(S(t)) * 1.2, 0], 1.3)},
        'head': {'rotation': keys(lambda t: [0, -5 * S(t), 0], 1.3)}}}
    once = lambda ln, bones: {'animation_length': ln, 'bones': bones}
    # обычный удар тесаком сверху (0.35 c — удар)
    A['attack'] = once(0.7, {
        'right_arm': {'rotation': {'0': [0, 0, 0], '0.25': [-150, 10, 0], '0.38': [-30, 0, 0], '0.7': [0, 0, 0]}},
        'chest': {'rotation': {'0': [0, 0, 0], '0.25': [-8, 10, 0], '0.38': [12, -6, 0], '0.7': [0, 0, 0]}}})
    # ДРОБИТЕЛЬ: обе руки над головой, удар в землю на 1.1 c (22 тика)
    A['slam'] = once(1.7, {
        'right_arm': {'rotation': {'0': [0, 0, 0], '0.8': [-170, 0, 12], '1.0': [-170, 0, 12], '1.12': [-25, 0, 8], '1.7': [0, 0, 0]}},
        'left_arm': {'rotation': {'0': [0, 0, 0], '0.8': [-170, 0, -12], '1.0': [-170, 0, -12], '1.12': [-25, 0, -8], '1.7': [0, 0, 0]}},
        'chest': {'rotation': {'0': [0, 0, 0], '0.8': [-22, 0, 0], '1.0': [-24, 0, 0], '1.12': [30, 0, 0], '1.45': [26, 0, 0], '1.7': [0, 0, 0]}},
        'body': {'position': {'0': [0, 0, 0], '0.8': [0, 1.5, 0], '1.12': [0, -3, 0], '1.45': [0, -2.5, 0], '1.7': [0, 0, 0]}},
        'head': {'rotation': {'0': [0, 0, 0], '0.8': [-20, 0, 0], '1.12': [10, 0, 0], '1.7': [0, 0, 0]}}})
    # РАЗМАХ: замах вправо-назад, удар полукругом на 0.65 c (13 тиков)
    A['swipe'] = once(1.1, {
        'right_arm': {'rotation': {'0': [0, 0, 0], '0.5': [-95, 70, 0], '0.62': [-95, 70, 0], '0.72': [-90, -80, 0], '1.1': [0, 0, 0]}},
        'chest': {'rotation': {'0': [0, 0, 0], '0.5': [0, 35, 0], '0.62': [0, 38, 0], '0.72': [8, -40, 0], '1.1': [0, 0, 0]}},
        'left_arm': {'rotation': {'0': [0, 0, 0], '0.5': [-20, 0, -25], '1.1': [0, 0, 0]}}})
    # ТОПОТ: поднимает ногу и бьёт ею (гнилые выбросы), удар на 0.8 c (16 тиков)
    A['stomp'] = once(1.3, {
        'right_leg': {'rotation': {'0': [0, 0, 0], '0.6': [-70, 0, 8], '0.8': [0, 0, 0]}},
        'chest': {'rotation': {'0': [0, 0, 0], '0.6': [-12, 0, -8], '0.8': [18, 0, 0], '1.3': [0, 0, 0]}},
        'right_arm': {'rotation': {'0': [0, 0, 0], '0.6': [-40, 0, 40], '0.8': [0, 0, 10], '1.3': [0, 0, 0]}},
        'left_arm': {'rotation': {'0': [0, 0, 0], '0.6': [-40, 0, -40], '0.8': [0, 0, -10], '1.3': [0, 0, 0]}},
        'body': {'position': {'0': [0, 0, 0], '0.6': [0, 1.5, 0], '0.8': [0, -1.5, 0], '1.3': [0, 0, 0]}}})
    # РЁВ: голова вверх, руки в стороны, дрожь
    shake = {str(round(0.4 + k * 0.1, 2)): [0, 0, (2 if k % 2 else -2)] for k in range(13)}
    A['roar'] = once(2.2, {
        'head': {'rotation': {'0': [0, 0, 0], '0.4': [-40, 0, 0], '1.7': [-42, 0, 0], '2.2': [0, 0, 0]}},
        'chest': {'rotation': {'0': [0, 0, 0], '0.4': [-18, 0, 0], **{k: [-18, 0, v[2]] for k, v in shake.items()}, '2.2': [0, 0, 0]}},
        'right_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-40, 0, 55], '1.7': [-45, 0, 60], '2.2': [0, 0, 0]}},
        'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-40, 0, -55], '1.7': [-45, 0, -60], '2.2': [0, 0, 0]}}})
    out = {'format_version': '1.8.0', 'animations': {'animation.brute.' + k if False else k: v for k, v in A.items()}}
    json.dump(out, open(G.ensure(os.path.join(G.ROOT, 'animations', 'brute.animation.json')), 'w'), ensure_ascii=False)


def keeper_anims():
    src = json.load(open(os.path.join(G.ROOT, 'animations', 'civ_npc.animation.json')))['animations']
    S = lambda t: math.sin(2 * math.pi * t)
    A = {}
    hover = {'root': {'position': keys(lambda t: [0, 3 + 0.8 * S(t), 0], 3.0)},
             'cape': {'rotation': keys(lambda t: [6 + 4 * S(t), 0, 2 * S(t)], 3.0)},
             'skirt': {'rotation': keys(lambda t: [3 * S(t + 0.2), 0, 0], 3.0)},
             'right_leg': {'rotation': {'0': [8, 0, 3]}}, 'left_leg': {'rotation': {'0': [14, 0, -3]}}}
    idle = {**src['idle']['bones'], **hover, 'right_arm': {'rotation': {'0': [-8, 0, 6]}}}
    A['idle'] = {'loop': True, 'animation_length': 3.0, 'bones': idle}
    A['walk'] = {'loop': True, 'animation_length': 3.0, 'bones': {**idle,
        'body': {'rotation': {'0': [10, 0, 0]}}, 'left_arm': {'rotation': keys(lambda t: [20, 0, -12 + 4 * S(t)], 3.0)},
        'cape': {'rotation': keys(lambda t: [22 + 6 * S(t * 2), 0, 3 * S(t)], 3.0)}}}
    once = lambda ln, bones: {'animation_length': ln, 'bones': bones}
    # укол копьём (удар на 0.35 c)
    A['attack'] = once(0.7, {**hover,
        'right_arm': {'rotation': {'0': [-8, 0, 6], '0.22': [-40, 0, 10], '0.35': [-95, 0, 0], '0.7': [-8, 0, 6]}},
        'body': {'rotation': {'0': [0, 0, 0], '0.35': [12, -10, 0], '0.7': [0, 0, 0]}}})
    # колдовство: левая рука вперёд, копьё вверх (ледяные шипы)
    A['cast'] = once(1.2, {**hover,
        'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-90, 0, -10], '1.2': [-90, 0, -10]}},
        'right_arm': {'rotation': {'0': [-8, 0, 6], '0.4': [-25, 0, 20], '1.2': [-25, 0, 20]}},
        'head': {'rotation': {'0': [0, 0, 0], '0.4': [6, 0, 0]}}})
    # призыв копий с неба: обе руки вверх
    A['raise'] = once(1.6, {**hover,
        'root': {'position': {'0': [0, 3, 0], '0.6': [0, 7, 0], '1.6': [0, 7, 0]}},
        'right_arm': {'rotation': {'0': [-8, 0, 6], '0.6': [-165, 0, 15], '1.6': [-165, 0, 15]}},
        'left_arm': {'rotation': {'0': [0, 0, 0], '0.6': [-165, 0, -15], '1.6': [-165, 0, -15]}},
        'head': {'rotation': {'0': [0, 0, 0], '0.6': [-25, 0, 0]}},
        'cape': {'rotation': {'0': [6, 0, 0], '0.6': [35, 0, 0], '1.6': [30, 0, 0]}}})
    # ледяная волна: вонзает копьё в землю (удар на 1.0 c = 20 тиков)
    A['slam'] = once(1.5, {**hover,
        'root': {'position': {'0': [0, 3, 0], '0.7': [0, 8, 0], '1.0': [0, 0, 0], '1.5': [0, 3, 0]}},
        'right_arm': {'rotation': {'0': [-8, 0, 6], '0.7': [-170, 0, 5], '1.0': [-20, 0, 0], '1.5': [-8, 0, 6]}},
        'left_arm': {'rotation': {'0': [0, 0, 0], '0.7': [-170, 0, -5], '1.0': [-30, 0, 0], '1.5': [0, 0, 0]}},
        'body': {'rotation': {'0': [0, 0, 0], '0.7': [-10, 0, 0], '1.0': [25, 0, 0], '1.5': [0, 0, 0]}}})
    # рывок-телепорт: сжимается
    A['blink'] = once(0.5, {**hover, 'root': {'scale': {'0': [1, 1, 1], '0.25': [0.2, 1.4, 0.2], '0.5': [1, 1, 1]}}})
    json.dump({'format_version': '1.8.0', 'animations': A}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'keeper.animation.json')), 'w'), ensure_ascii=False)


if __name__ == '__main__':
    if '--anim' not in sys.argv:
        brute(); keeper()
    brute_anims(); keeper_anims()
    print('Боссы готовы')
