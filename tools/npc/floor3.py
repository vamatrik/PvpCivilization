#!/usr/bin/env python3
"""Модели этажа 3 (GeckoLib, 0.9.0): четыре брата-каппы (одна геометрия, свой цвет и оружие у каждого) и лиса-оборотень.
Запуск: python3 tools/npc/floor3.py"""
import json, math, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G
import bosses as B
import floor2 as F2
G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, mix = G.hexc, G.flat, G.mul, G.mix


def scales(base, seed=1):
    """Кожа каппы: крапинки и тёмные чешуйки."""
    def paint(fc, d, r):
        rnd = random.Random(seed * 7 + hash(fc) % 53)
        k = {'up': 1.12, 'down': 0.72, 'north': 1.0, 'south': 0.9, 'east': 0.86, 'west': 0.86}[fc]
        for x in range(r[0], r[2]):
            for y in range(r[1], r[3]):
                c = base
                if rnd.random() < 0.12: c = mul(base, 0.78)
                if rnd.random() < 0.05: c = mul(base, 1.18)
                d.point((x, y), mul(c, k))
    return paint


def shell(base, rim):
    """Панцирь: шестиугольные пластины с тёмными швами и светлой каймой."""
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        flat(base, noise=0.06)(fc, d, r)
        if fc in ('south', 'up', 'north'):
            for i in range(1, 4): d.line([(x0, y0 + H * i / 4), (x1, y0 + H * i / 4)], fill=mul(base, 0.55))
            for j in range(1, 3):
                for i in range(4):
                    xx = x0 + W * (j / 3 + (0.08 if i % 2 else -0.08))
                    d.line([(xx, y0 + H * i / 4), (xx, y0 + H * (i + 1) / 4)], fill=mul(base, 0.55))
        d.rectangle([x0, y0, x1 - 1, y0 + max(1, H // 12)], fill=rim)
    return paint


def kappa_face(skin, beak, eye):
    sp = scales(skin, 3)
    def paint(fc, d, r):
        sp(fc, d, r)
        if fc != 'north': return
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        for ex in (0.27, 0.73):
            cx, cy = x0 + W * ex, y0 + H * 0.42
            d.ellipse([cx - W * 0.11, cy - H * 0.11, cx + W * 0.11, cy + H * 0.11], fill=(250, 248, 230, 255))
            d.ellipse([cx - W * 0.055, cy - H * 0.07, cx + W * 0.055, cy + H * 0.07], fill=eye)
            d.point((int(cx - W * 0.02), int(cy - H * 0.04)), (255, 255, 255, 255))
    return paint


SEASONS = {
    'spring': dict(skin='6aae4a', accent='ff8ac0', shell='5a6a2a', eye='3a1a2a'),
    'summer': dict(skin='4a9a3a', accent='f4f0e0', shell='4a5a24', eye='1a1a10'),
    'autumn': dict(skin='7a9a3a', accent='e06a20', shell='6a4a24', eye='2a1a0a'),
    'winter': dict(skin='5a8a86', accent='a8e4ff', shell='3a4a5a', eye='0a1a2a'),
}


def kappa(season):
    P = SEASONS[season]
    skin, acc, sh, eye = hexc(P['skin']), hexc(P['accent']), hexc(P['shell']), hexc(P['eye'])
    beak = hexc('d8c048'); hair = mul(skin, 0.55); water = (90, 170, 255, 255)
    big = season == 'summer'
    m = G.Model('kappa_' + season, 1024)
    sk = scales(skin, 1)
    m.bone('root', None, [0, 0, 0])
    for side, nm in ((-1, 'right_leg'), (1, 'left_leg')):
        x = 2.6 * side * (1.2 if big else 1)
        m.bone(nm, 'root', [x, 6, 0], [([x - 1.7, 0, -1.7], [3.4, 6, 3.4], sk, 6),
                                       ([x - 2.1, 0, -3.2], [4.2, 1.2, 4.6], flat(mul(skin, 0.8)), 6)])   # лапы-ласты
    bw = 11 if big else 9
    body = [([-bw / 2, 6, -3.5], [bw, 10, 7], sk, 5),
            ([-bw / 2 + 1, 7, -3.9], [bw - 2, 8, 0.6], flat(hexc('e8d890')), 5),        # светлый живот-пластрон
            ([-bw / 2 - 0.2, 8.5, -3.8], [bw + 0.4, 1.6, 7.6], flat(acc), 6)]            # пояс / мавахи
    if big: body.append(([-1.5, 5.4, -4.0], [3, 3.2, 0.8], flat(acc), 6))
    m.bone('body', 'root', [0, 6, 0], body)
    m.bone('shell', 'body', [0, 11, 3.5], [([-bw / 2 - 1, 5.5, 3.2], [bw + 2, 11.5, 3.6], shell(sh, mul(sh, 1.6)), 4)], rotation=[-6, 0, 0])
    head = [([-4, 16, -4], [8, 7, 8], kappa_face(skin, beak, eye), 8),
            ([-2.2, 16.6, -6.6], [4.4, 2.4, 2.8], flat(beak), 8),                      # клюв
            ([-4.4, 22.4, -4.4], [8.8, 1.6, 8.8], flat(hair), 6),                      # бахрома волос вокруг блюдца
            ([-3, 23.4, -3], [6, 0.8, 6], flat(hexc('e8e8d8')), 8)]                    # блюдце
    m.bone('head', 'body', [0, 16, 0], head)
    m.bone('water', 'head', [0, 24, 0], [([-2.4, 23.9, -2.4], [4.8, 0.5, 4.8], flat(water, noise=0.12), 8)])
    for side, nm in ((-1, 'right_arm'), (1, 'left_arm')):
        x = (bw / 2 + 1.4) * side
        m.bone(nm, 'body', [x, 15, 0], [([x - 1.4, 8, -1.4], [2.8, 7.5, 2.8], sk, 6),
                                        ([x - 1.7, 7, -1.7], [3.4, 1.6, 3.4], flat(mul(skin, 0.85)), 6)])
    hx = -(bw / 2 + 1.4)
    wood, steel = hexc('6a4a2a'), hexc('d8dce4')
    if season == 'spring':      # парные кама
        for side, arm in ((-1, 'right_arm'), (1, 'left_arm')):
            ax = hx * -side if side == 1 else hx
            m.bone('weapon' if side == -1 else 'weapon2', arm, [ax, 8, 0], [([ax - 0.4, 3, -0.4], [0.8, 6, 0.8], flat(wood), 8),
                                                                           ([ax - 0.3, 8.4, -3.6], [0.6, 1.2, 3.4], flat(steel), 8),
                                                                           ([ax - 0.35, 7.6, -4.2], [0.7, 1.0, 1.0], flat(acc), 8)])
    elif season == 'autumn':    # нагината
        m.bone('weapon', 'right_arm', [hx, 8, 0], [([hx - 0.4, -6, -0.4], [0.8, 26, 0.8], flat(wood), 8),
                                                   ([hx - 0.5, 20, -0.9], [1.0, 6.5, 1.8], flat(steel), 8),
                                                   ([hx - 0.7, 19, -0.7], [1.4, 1.0, 1.4], flat(acc), 8)])
    elif season == 'winter':    # посох со льдом
        m.bone('weapon', 'right_arm', [hx, 8, 0], [([hx - 0.45, -8, -0.45], [0.9, 28, 0.9], flat(hexc('4a3a2a')), 8),
                                                   ([hx - 1.4, 19.5, -1.4], [2.8, 3.2, 2.8], flat(acc, noise=0.15), 8),
                                                   ([hx - 0.8, 22.7, -0.8], [1.6, 1.8, 1.6], flat(hexc('eaffff')), 8)])
    m.save('kappa_' + season)


def kappa_anims():
    S = lambda t: math.sin(2 * math.pi * t)
    k = B.keys
    once = lambda ln, bones: {'animation_length': ln, 'bones': bones}
    A = {'idle': {'loop': True, 'animation_length': 2.0, 'bones': {
            'body': {'rotation': k(lambda t: [0, 0, 3 * S(t)], 2.0)}, 'head': {'rotation': k(lambda t: [4 * S(t), 10 * S(t / 2), 0], 2.0)},
            'right_arm': {'rotation': k(lambda t: [-10 + 6 * S(t), 0, 8], 2.0)}, 'left_arm': {'rotation': k(lambda t: [-10 - 6 * S(t), 0, -8], 2.0)},
            'water': {'position': k(lambda t: [0, 0.1 * S(t * 2), 0], 2.0)}}},
         'walk': {'loop': True, 'animation_length': 0.6, 'bones': {
            'right_leg': {'rotation': k(lambda t: [35 * S(t), 0, 0], 0.6)}, 'left_leg': {'rotation': k(lambda t: [-35 * S(t), 0, 0], 0.6)},
            'right_arm': {'rotation': k(lambda t: [-30 * S(t), 0, 10], 0.6)}, 'left_arm': {'rotation': k(lambda t: [30 * S(t), 0, -10], 0.6)},
            'body': {'rotation': k(lambda t: [6, 0, 6 * S(t)], 0.6)}}},
         'attack': once(0.5, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.15': [-130, 0, 20], '0.3': [-20, 0, -10], '0.5': [0, 0, 0]}},
                              'body': {'rotation': {'0': [0, 0, 0], '0.3': [12, -20, 0], '0.5': [0, 0, 0]}}}),
         'dash': once(0.8, {'body': {'rotation': {'0': [0, 0, 0], '0.2': [40, 0, 0], '0.6': [40, 0, 0], '0.8': [0, 0, 0]}},
                            'right_arm': {'rotation': {'0': [0, 0, 0], '0.2': [-90, 30, 0], '0.6': [-90, 30, 0], '0.8': [0, 0, 0]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.2': [-90, -30, 0], '0.6': [-90, -30, 0], '0.8': [0, 0, 0]}}}),
         'slam': once(1.2, {'root': {'position': {'0': [0, 0, 0], '0.5': [0, 14, 0], '0.75': [0, 0, 0], '1.2': [0, 0, 0]}},
                            'right_arm': {'rotation': {'0': [0, 0, 0], '0.5': [-170, 0, 0], '0.75': [-40, 0, 0], '1.2': [0, 0, 0]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.5': [-170, 0, 0], '0.75': [-40, 0, 0], '1.2': [0, 0, 0]}}}),
         'sweep': once(0.9, {'root': {'rotation': {'0': [0, 0, 0], '0.6': [0, 360, 0], '0.9': [0, 360, 0]}},
                             'right_arm': {'rotation': {'0': [0, 0, 0], '0.2': [-90, 0, 60], '0.6': [-90, 0, 60], '0.9': [0, 0, 0]}}}),
         'cast': once(1.2, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-160, 0, 10], '1.2': [-160, 0, 10]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-160, 0, -10], '1.2': [-160, 0, -10]}},
                            'head': {'rotation': {'0': [0, 0, 0], '0.4': [-25, 0, 0], '1.2': [-25, 0, 0]}}}),
         'bow': once(1.0, {'body': {'rotation': {'0': [0, 0, 0], '0.3': [55, 0, 0], '0.7': [55, 0, 0], '1.0': [0, 0, 0]}},
                           'water': {'scale': {'0': [1, 1, 1], '0.35': [1, 1, 1], '0.5': [0, 0, 0], '1.0': [0, 0, 0]}}})}
    json.dump({'format_version': '1.8.0', 'animations': A}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'kappa.animation.json')), 'w'))


def fox():
    m = G.Model('fox_spirit', 256)
    orange, white, dark = hexc('e0782a'), hexc('f4ece0'), hexc('3a2014')
    F2.quadruped(m, F2.fur(orange, 21, mul(orange, 0.8)), flat(white), F2.eyes_side(orange, (255, 200, 40, 255), 22),
                 F2.snout_face(white, None, dark, 23, teeth=False), flat(dark), F2.fur(orange, 24), F2.fur(mul(orange, 0.85), 25, dark), 1.05)
    s = 1.05
    m.bone('brush', 'tail2', [0, 5.5 * s, 13 * s], [([-1.4 * s, 4.4 * s, 12 * s], [2.8 * s, 2.8 * s, 4.5 * s], F2.fur(orange, 26), 6),
                                                    ([-1.1 * s, 4.7 * s, 16.4 * s], [2.2 * s, 2.2 * s, 1.6 * s], flat(white), 6)])
    m.save('fox_spirit')


def kumiho():
    m = G.Model('kumiho', 1024)
    skin = hexc('f6e2d4'); hair = hexc('f0f0f6')
    red, white, gold, fox = hexc('c8203a'), hexc('f8f4f0'), hexc('e8c45a'), hexc('f0a040')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('ffb030'), sex='f', expr='smug' if False else 'stern', bangs='hime',
                             brow=hexc('c8c0c8'), lash=hexc('8a2030'), blush_c=(255, 120, 150, 90)), long_hair=True)

    def extra(m):
        hp = B.strands(hair, mix(hair, hexc('ffb0d0'), 0.5), seed=61)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.3, -4.9], [10.2, 2.9, 10.2], hp, 4),
                                             ([-5.3, 14.5, 2.6], [10.6, 16, 2.4], hp, 4),
                                             ([-5.6, 21.5, -3.0], [1.3, 9, 6.2], hp, 4), ([4.3, 21.5, -3.0], [1.3, 9, 6.2], hp, 4)])
        # лисьи уши
        for side in (-1, 1):
            x = 3.2 * side
            m.bone('ear_' + ('l' if side > 0 else 'r'), 'head', [x, 31, 0], [([x - 1.3, 31, -1.2], [2.6, 2.8, 1.4], flat(fox), 8), ([x - 0.8, 33.8, -1.2], [1.6, 1.2, 1.4], flat(hexc('3a2014')), 8),
                                                                              ([x - 0.7, 31.6, -1.35], [1.4, 2.4, 0.2], flat(hexc('ff9ab8')), 8)],
                   rotation=[0, 0, -12 * side])
        # кимоно: длинная юбка-хакама и пояс-оби
        m.bone('skirt', 'body', [0, 13, 0], [([-4.6, 0.2, -3.2], [9.2, 13, 6.4], B.gown(red, mul(red, 0.6), gold, seed=7), 3)])
        m.bone('obi', 'body', [0, 15, 0], [([-3.4, 14.4, -2.3], [6.8, 2.4, 4.6], flat(gold, noise=0.06), 4),
                                           ([-1.6, 13.4, 2.2], [3.2, 3.6, 1.2], flat(gold, noise=0.06), 4)])
        # девять хвостов веером
        for i in range(9):
            a = -60 + i * 15
            tail = [([-1.3, 0, 0], [2.6, 2.6, 13], F2.fur(fox, 70 + i, mul(fox, 0.8)), 4),
                    ([-1.0, 0.3, 13], [2.0, 2.0, 3], flat(white), 4)]
            m.bone('tail%d' % i, 'body', [0, 13.5, 2.5], [([c[0][0], c[0][1] + 12.2, c[0][2] + 2.5], c[1], c[2], c[3]) for c in tail],
                   rotation=[38 + abs(a) * 0.12, a, 0])

    G.humanoid(m, skin, face, B.gown(white, mul(white, 0.85), red, seed=3), flat(white), flat(white), flat(hexc('3a2a2a')), fem=True, extra=extra)
    m.save('kumiho')


def kumiho_anims():
    S = lambda t: math.sin(2 * math.pi * t)
    k = B.keys
    src = json.load(open(os.path.join(G.ROOT, 'animations', 'civ_npc.animation.json')))['animations']
    once = lambda ln, bones: {'animation_length': ln, 'bones': bones}
    tails = lambda amp, ln: {'tail%d' % i: {'rotation': k(lambda t, i=i: [amp * S(t + i / 9), amp * 0.6 * S(t * 0.5 + i / 9), 0], ln)} for i in range(9)}
    A = {'idle': {'loop': True, 'animation_length': 3.0, 'bones': {**src['idle']['bones'], **tails(6, 3.0)}},
         'walk': {'loop': True, 'animation_length': 0.8, 'bones': {**src['walk']['bones'], **tails(10, 0.8)}},
         'attack': once(0.5, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.15': [-120, 0, 30], '0.3': [-30, 0, -10], '0.5': [0, 0, 0]}}}),
         'cast': once(1.2, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-150, 0, 20], '1.2': [-150, 0, 20]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-150, 0, -20], '1.2': [-150, 0, -20]}}, **tails(18, 1.2)}),
         'blink': once(0.4, {'root': {'scale': {'0': [1, 1, 1], '0.2': [0.2, 1.4, 0.2], '0.4': [1, 1, 1]}}}),
         'spin': once(0.9, {'root': {'rotation': {'0': [0, 0, 0], '0.6': [0, 360, 0], '0.9': [0, 360, 0]}}, **tails(25, 0.9)})}
    json.dump({'format_version': '1.8.0', 'animations': A}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'kumiho.animation.json')), 'w'))


if __name__ == '__main__':
    kumiho(); kumiho_anims()
    for s in SEASONS: kappa(s)
    kappa_anims(); fox()
    print('этаж 3: модели готовы')
