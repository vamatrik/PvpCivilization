#!/usr/bin/env python3
"""Финальный босс этажа 4 (0.11): доктор Ноэль (учёный с плазменной пушкой) и Лиана (хищное растение-питомец)."""
import json, math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G
import bosses as B
G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, strands, stripes = G.hexc, G.flat, G.mul, G.strands, G.stripes
S = lambda t: math.sin(2 * math.pi * t)
k = B.keys
once = lambda ln, bones: {'animation_length': ln, 'bones': bones}


def noel():
    m = G.Model('noel', 1024)
    skin = hexc('e8d0c0'); hair = hexc('3a2a4a')
    coat, shirt, dark, violet, cyan = hexc('f4f4f8'), hexc('3a4a6a'), hexc('1e1e26'), hexc('a040ff'), hexc('40d0ff')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('a040ff'), sex='m', expr='grim', bangs='messy', glasses=True,
                             brow=hexc('2a1e30'), lash=hexc('1a1420'), blush_c=(200, 120, 120, 30)), long_hair=False)

    def extra(m):
        hp = strands(mul(hair, 1.4), mul(hair, 0.8), seed=19)
        m.bone('hair', 'head', [0, 31, 4], [([-5.2, 29.2, -5.0], [10.4, 3.2, 10.4], hp, 4), ([-5.3, 22.5, 2.4], [10.6, 8, 2.8], hp, 4),
                                             ([-5.5, 23.0, -3.6], [1.3, 7, 7.6], hp, 4), ([4.2, 23.0, -3.6], [1.3, 7, 7.6], hp, 4),
                                             ([-2.0, 31.5, -1.0], [4.0, 1.6, 3.0], hp, 4)])
        # длинный лабораторный халат с пятнами зелёного сока
        m.bone('coat', 'body', [0, 21, 0], [([-4.5, 2.0, -2.6], [9.0, 19.0, 5.2], stripes(coat, mul(coat, 0.9), 7, seed=8), 3)])
        m.bone('stain', 'coat', [0, 10, -2.6], [([-3.0, 6.0, -2.75], [2.0, 2.0, 0.2], flat(hexc('5ab830')), 8), ([1.0, 3.0, -2.75], [1.6, 3.0, 0.2], flat(hexc('3a8a2a')), 8)])
        m.bone('sleeve_l', 'left_arm', [4, 20.5, 0], [([2.3, 11.0, -1.8], [3.6, 10, 3.6], flat(coat, noise=0.03), 3)])
        # плазменная пушка на правой руке
        m.bone('cannon', 'right_arm', [-4, 12, 0], [([-6.6, 4.0, -3.4], [5.2, 9.0, 5.2], flat(dark, noise=0.05), 4),
                                                   ([-6.0, 1.5, -2.8], [4.0, 2.6, 4.0], flat(mul(dark, 1.4)), 4),
                                                   ([-5.4, 0.8, -2.2], [2.8, 0.8, 2.8], flat(violet), 8),
                                                   ([-6.8, 7.0, -3.6], [5.6, 0.8, 5.6], flat(cyan), 8),
                                                   ([-3.2, 6.0, -1.0], [1.4, 4.0, 2.0], flat(violet), 8)])
        m.bone('tank', 'body', [0, 16, 2.6], [([-2.0, 12.0, 2.4], [4.0, 8.0, 2.8], flat(hexc('5a5e66')), 4), ([-1.0, 13.0, 5.2], [2.0, 6.0, 0.4], flat(violet), 8)])

    G.humanoid(m, skin, face, flat(shirt, noise=0.03), flat(coat), flat(dark), flat(hexc('2a2a30')), fem=False, extra=extra)
    m.save('noel')


def noel_anims():
    src = json.load(open(os.path.join(G.ROOT, 'animations', 'civ_npc.animation.json')))['animations']
    A = {'idle': src['idle'], 'walk': src['walk'],
         'attack': once(0.5, {'left_arm': {'rotation': {'0': [0, 0, 0], '0.15': [-110, 0, -20], '0.3': [-20, 0, 10], '0.5': [0, 0, 0]}}}),
         'aim': once(1.2, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.3': [-90, 0, 0], '1.2': [-90, 0, 0]}}}),
         'shoot': once(0.6, {'right_arm': {'rotation': {'0': [-90, 0, 0], '0.08': [-110, 0, 0], '0.6': [-90, 0, 0]}},
                             'body': {'rotation': {'0': [0, 0, 0], '0.08': [-6, 0, 0], '0.6': [0, 0, 0]}}}),
         'cast': once(1.4, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-160, 0, 15], '1.4': [-160, 0, 15]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-160, 0, -15], '1.4': [-160, 0, -15]}}}),
         'heal': once(1.0, {'left_arm': {'rotation': {'0': [0, 0, 0], '0.3': [-80, -30, 0], '1.0': [-80, -30, 0]}}})}
    json.dump({'format_version': '1.8.0', 'animations': A}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'noel.animation.json')), 'w'))


def liana():
    m = G.Model('liana', 1024)
    green, dgreen, lgreen, red, pink, cream, pot = hexc('3a8a2a'), hexc('1e4a16'), hexc('8ad050'), hexc('b01830'), hexc('ff7aa0'), hexc('f0e8c8'), hexc('5a5e66')
    m.bone('root', None, [0, 0, 0])
    # контейнер-кадка NaZa и корни
    m.bone('pot', 'root', [0, 0, 0], [([-7, 0, -7], [14, 5, 14], stripes(pot, mul(pot, 0.75), 3, seed=2), 2),
                                      ([-7.5, 5, -7.5], [15, 1.2, 15], flat(hexc('e8c020')), 2),
                                      ([-6, 5, -6], [12, 1, 12], flat(hexc('3a2a1a')), 2)])
    for i, a in enumerate((20, 110, 200, 290)):
        r = math.radians(a)
        m.bone('rootv%d' % i, 'pot', [0, 5.5, 0], [([math.cos(r) * 6 - 1, 3.5, math.sin(r) * 6 - 1], [2, 3, 2], flat(dgreen), 4)])
    # стебель из двух сегментов
    m.bone('stem', 'root', [0, 6, 0], [([-2.2, 6, -2.2], [4.4, 14, 4.4], stripes(green, dgreen, 3, seed=4), 3),
                                       ([-3.2, 12, -3.2], [6.4, 1.2, 6.4], flat(lgreen), 4)])
    m.bone('stem2', 'stem', [0, 20, 0], [([-1.9, 20, -1.9], [3.8, 12, 3.8], stripes(green, dgreen, 3, seed=6), 3),
                                         ([-4.5, 25, -1], [3, 1, 6], flat(lgreen), 4), ([1.5, 28, -1], [3, 1, 6], flat(lgreen), 4)])
    # голова-ловушка: верхняя и нижняя челюсть с зубами, внутри — розовая пасть
    m.bone('head', 'stem2', [0, 32, 0], [([-6, 31, -4], [12, 4, 10], flat(green, noise=0.06), 3)])
    m.bone('jaw_top', 'head', [0, 34, 4], [([-6.5, 34, -8], [13, 5, 13], stripes(red, mul(red, 0.7), 4, seed=9), 3),
                                           ([-6.0, 33.4, -8.4], [12, 0.8, 12], flat(pink), 4)] +
           [([-6 + i * 2.2, 32.4, -8.6], [0.8, 1.6, 0.8], flat(cream), 8) for i in range(6)] +
           [([5.7, 39, -6 + i * 3], [1.2, 1.2, 1.2], flat(lgreen), 8) for i in range(4)])
    m.bone('jaw_bot', 'head', [0, 32, 4], [([-6.5, 28, -8], [13, 4, 13], stripes(red, mul(red, 0.7), 4, seed=10), 3),
                                           ([-6.0, 31.6, -8.4], [12, 0.8, 12], flat(pink), 4)] +
           [([-5 + i * 2.2, 32.0, -8.6], [0.8, 1.6, 0.8], flat(cream), 8) for i in range(5)])
    # четыре лианы-щупальца по два сегмента
    for i, (x, z, ry) in enumerate(((-4, -2, 40), (4, -2, -40), (-4, 3, 140), (4, 3, -140))):
        m.bone('vine%d' % i, 'stem', [x * 0.5, 14, z * 0.5], [([x * 0.5 - 1, 14, z * 0.5 - 1], [2, 2, 14], stripes(dgreen, green, 3, seed=20 + i), 4,
                                                              ([x * 0.5, 15, z * 0.5], [-20, ry, 0]))])
        m.bone('vine%d_tip' % i, 'vine%d' % i, [x * 0.5, 15, z * 0.5], [([x * 0.5 - 0.7, 14.3, z * 0.5 + 12], [1.4, 1.4, 10], flat(lgreen), 4,
                                                                        ([x * 0.5, 15, z * 0.5], [-35, ry, 0])),
                                                                       ([x * 0.5 - 1.5, 13.5, z * 0.5 + 21], [3, 3, 3], flat(red), 6, ([x * 0.5, 15, z * 0.5], [-35, ry, 0]))])
    m.save('liana')


def liana_anims():
    vines = lambda amp, ln, sp=1: {'vine%d' % i: {'rotation': k(lambda t, i=i: [amp * S(t * sp + i / 4), amp * 0.5 * S(t * sp + i / 3), 0], ln)} for i in range(4)}
    sway = lambda amp, ln: {'stem': {'rotation': k(lambda t: [amp * S(t), 0, amp * 0.6 * S(t + 0.25)], ln)},
                            'stem2': {'rotation': k(lambda t: [amp * 0.8 * S(t + 0.1), 0, amp * 0.5 * S(t + 0.35)], ln)},
                            'jaw_top': {'rotation': k(lambda t: [-28 - 6 * S(t * 2), 0, 0], ln)},
                            'jaw_bot': {'rotation': k(lambda t: [12 + 3 * S(t * 2), 0, 0], ln)}}
    A = {'idle': {'loop': True, 'animation_length': 3.0, 'bones': {**sway(4, 3.0), **vines(10, 3.0)}},
         'walk': {'loop': True, 'animation_length': 3.0, 'bones': {**sway(4, 3.0), **vines(10, 3.0)}},
         'attack': once(0.7, {'stem2': {'rotation': {'0': [0, 0, 0], '0.25': [-35, 0, 0], '0.45': [30, 0, 0], '0.7': [0, 0, 0]}},
                              'jaw_top': {'rotation': {'0': [0, 0, 0], '0.25': [-45, 0, 0], '0.45': [0, 0, 0]}},
                              'jaw_bot': {'rotation': {'0': [0, 0, 0], '0.25': [25, 0, 0], '0.45': [0, 0, 0]}}}),
         'lash': once(1.0, {**{'vine%d' % i: {'rotation': {'0': [0, 0, 0], '0.4': [-60, 60 if i % 2 else -60, 0], '0.6': [20, -80 if i % 2 else 80, 0], '1.0': [0, 0, 0]}} for i in range(4)},
                            'stem': {'rotation': {'0': [0, 0, 0], '0.4': [0, -25, 0], '0.6': [0, 30, 0], '1.0': [0, 0, 0]}}}),
         'slam': once(1.1, {'stem': {'rotation': {'0': [0, 0, 0], '0.5': [-20, 0, 0], '0.7': [45, 0, 0], '1.1': [0, 0, 0]}},
                            'stem2': {'rotation': {'0': [0, 0, 0], '0.5': [-20, 0, 0], '0.7': [40, 0, 0], '1.1': [0, 0, 0]}}}),
         'swallow': once(1.2, {'jaw_top': {'rotation': {'0': [0, 0, 0], '0.4': [-70, 0, 0], '0.6': [0, 0, 0], '1.2': [0, 0, 0]}},
                               'jaw_bot': {'rotation': {'0': [0, 0, 0], '0.4': [35, 0, 0], '0.6': [0, 0, 0]}},
                               'stem2': {'rotation': {'0': [0, 0, 0], '0.4': [-15, 0, 0], '0.6': [35, 0, 0], '1.2': [0, 0, 0]}}}),
         'chew': {'loop': True, 'animation_length': 0.6, 'bones': {'jaw_top': {'rotation': k(lambda t: [-12 * abs(S(t)), 0, 0], 0.6)},
                                                                   'head': {'scale': k(lambda t: [1 + 0.06 * S(t), 1 - 0.04 * S(t), 1 + 0.06 * S(t)], 0.6)}}},
         'roar': once(1.4, {'jaw_top': {'rotation': {'0': [0, 0, 0], '0.3': [-60, 0, 0], '1.1': [-60, 0, 0], '1.4': [0, 0, 0]}},
                            'jaw_bot': {'rotation': {'0': [0, 0, 0], '0.3': [30, 0, 0], '1.1': [30, 0, 0], '1.4': [0, 0, 0]}}, **vines(30, 1.4, 3)})}
    json.dump({'format_version': '1.8.0', 'animations': A}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'liana.animation.json')), 'w'))


if __name__ == '__main__':
    noel(); noel_anims(); liana(); liana_anims()
    print('босс этажа 4: модели готовы')
