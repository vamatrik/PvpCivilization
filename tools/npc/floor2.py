#!/usr/bin/env python3
"""Модели этажа 2 (GeckoLib): крыса, дворняга (общие анимации beast), Дон Витторио (босс улицы), Крысиный король (босс канализации).
Запуск: python3 tools/npc/floor2.py  (--anim — только анимации)"""
import json, math, os, random, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G
import bosses as B

G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, mix = G.hexc, G.flat, G.mul, G.mix


def fur(base, seed=1, dark=None):
    dark = dark or mul(base, 0.7)
    def paint(fc, d, r):
        x0, y0, x1, y1 = r
        rnd = random.Random(seed * 13 + hash(fc) % 71)
        k = {'up': 1.12, 'down': 0.72, 'north': 1.0, 'south': 0.9, 'east': 0.86, 'west': 0.86}[fc]
        for x in range(x0, x1):
            for y in range(y0, y1):
                c = base if rnd.random() > 0.25 else dark
                if (x + y * 3) % 5 == 0: c = mul(c, 1.12)
                d.point((x, y), mul(c, k * (1 + rnd.uniform(-0.05, 0.05))))
    return paint


def snout_face(base, eye, nose, seed=2, teeth=True):
    fp = fur(base, seed)
    def paint(fc, d, r):
        fp(fc, d, r)
        if fc != 'north': return
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        d.rectangle([x0 + W * 0.35, y0, x1 - W * 0.35 - 1, y0 + H * 0.3], fill=nose)
        if teeth: d.rectangle([x0 + W * 0.38, y1 - H * 0.3, x1 - W * 0.38 - 1, y1 - 1], fill=(240, 230, 200, 255))
    return paint


def eyes_side(base, eye, seed=3):
    fp = fur(base, seed)
    def paint(fc, d, r):
        fp(fc, d, r)
        if fc == 'north':
            x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
            for ex in (0.22, 0.78):
                cx, cy = x0 + W * ex, y0 + H * 0.4
                d.ellipse([cx - W * 0.09, cy - H * 0.1, cx + W * 0.09, cy + H * 0.1], fill=eye)
                d.point((int(cx), int(cy - H * 0.04)), (255, 255, 255, 255))
    return paint


def quadruped(m, body, belly, head, snout, ear, tail, leg, size):
    """Четвероногий: root → body → head (+snout, ears), 4 лапы, хвост из двух звеньев. size — масштаб."""
    s = size
    m.bone('root', None, [0, 0, 0])
    m.bone('body', 'root', [0, 4 * s, 0], [([-2.5 * s, 3 * s, -4 * s], [5 * s, 4 * s, 9 * s], body, 3),
                                           ([-2.3 * s, 2.8 * s, -3.5 * s], [4.6 * s, 0.6 * s, 8 * s], belly, 3)])
    m.bone('head', 'body', [0, 6 * s, -4 * s], [([-2 * s, 4.5 * s, -7.5 * s], [4 * s, 3.5 * s, 4 * s], head, 6),
                                               ([-1.2 * s, 4.6 * s, -9.5 * s], [2.4 * s, 2 * s, 2.2 * s], snout, 8),
                                               ([-2.4 * s, 7.6 * s, -5.2 * s], [1.6 * s, 1.6 * s, 0.6 * s], ear, 8),
                                               ([0.8 * s, 7.6 * s, -5.2 * s], [1.6 * s, 1.6 * s, 0.6 * s], ear, 8)])
    for nm, x, z in (('leg_fl', 1.4, -2.8), ('leg_fr', -1.4, -2.8), ('leg_bl', 1.4, 3.6), ('leg_br', -1.4, 3.6)):
        m.bone(nm, 'body', [x * s, 3.2 * s, z * s], [([(x - 0.7) * s, 0, (z - 0.7) * s], [1.4 * s, 3.2 * s, 1.4 * s], leg, 6)])
    m.bone('tail', 'body', [0, 5.5 * s, 5 * s], [([-0.4 * s, 5.1 * s, 5 * s], [0.8 * s, 0.8 * s, 4 * s], tail, 8)], rotation=[-15, 0, 0])
    m.bone('tail2', 'tail', [0, 5.5 * s, 9 * s], [([-0.3 * s, 5.2 * s, 9 * s], [0.6 * s, 0.6 * s, 4 * s], tail, 8)], rotation=[10, 0, 0])


def rat():
    m = G.Model('rat', 256)
    grey, pink = hexc('6a6058'), hexc('d89090')
    quadruped(m, fur(grey, 1), flat(hexc('9a9088')), eyes_side(grey, (200, 30, 30, 255), 2), snout_face(mul(grey, 1.1), None, pink, 3),
              flat(pink), flat(pink, noise=0.02), flat(pink), 0.75)
    m.save('rat')


def dog():
    m = G.Model('street_dog', 256)
    tan, dark = hexc('8a6a4a'), hexc('3a2a1a')
    quadruped(m, fur(tan, 4, dark), flat(hexc('b09070')), eyes_side(tan, (230, 200, 40, 255), 5), snout_face(dark, None, (20, 20, 20, 255), 6),
              flat(dark), fur(tan, 7, dark), fur(mul(tan, 0.9), 8, dark), 1.15)
    m.save('street_dog')


def beast_anims():
    S = lambda t: math.sin(2 * math.pi * t)
    k = B.keys
    A = {'idle': {'loop': True, 'animation_length': 2.0, 'bones': {
            'body': {'scale': k(lambda t: [1, 1 + 0.02 * S(t), 1], 2.0)},
            'head': {'rotation': k(lambda t: [4 * S(t), 8 * S(t / 2), 0], 2.0)},
            'tail': {'rotation': k(lambda t: [0, 18 * S(t), 0], 2.0)}, 'tail2': {'rotation': k(lambda t: [0, 25 * S(t - 0.15), 0], 2.0)}}},
         'walk': {'loop': True, 'animation_length': 0.5, 'bones': {
            'leg_fl': {'rotation': k(lambda t: [35 * S(t), 0, 0], 0.5)}, 'leg_br': {'rotation': k(lambda t: [35 * S(t), 0, 0], 0.5)},
            'leg_fr': {'rotation': k(lambda t: [-35 * S(t), 0, 0], 0.5)}, 'leg_bl': {'rotation': k(lambda t: [-35 * S(t), 0, 0], 0.5)},
            'body': {'position': k(lambda t: [0, abs(S(t)) * 0.6, 0], 0.5)},
            'tail': {'rotation': k(lambda t: [0, 25 * S(t), 0], 0.5)}, 'tail2': {'rotation': k(lambda t: [0, 30 * S(t - 0.2), 0], 0.5)}}},
         'attack': {'animation_length': 0.4, 'bones': {
            'head': {'rotation': {'0': [0, 0, 0], '0.15': [-25, 0, 0], '0.25': [20, 0, 0], '0.4': [0, 0, 0]}},
            'body': {'position': {'0': [0, 0, 0], '0.2': [0, 0.5, -1.5], '0.4': [0, 0, 0]}}}}}
    json.dump({'format_version': '1.8.0', 'animations': A}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'beast.animation.json')), 'w'))


# ------------------------------------------------------------------ Дон Витторио
def don():
    m = G.Model('don', 1024)
    skin = hexc('e8c4a4'); hair = hexc('2a2220')
    suit, shirt, tie, hat = hexc('1c1c22'), hexc('eeeeee'), hexc('b01a2a'), hexc('2a2a2e')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('5a3a20'), sex='m', expr='stern', bangs='side',
                             brow=hexc('201810'), lash=hexc('1a1010'), blush_c=(200, 120, 100, 60)), long_hair=False)

    def jacket(fc, d, r):
        flat(suit, noise=0.05)(fc, d, r)
        if fc == 'north':
            x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
            d.polygon([(x0 + W * 0.3, y0), (x1 - W * 0.3, y0), (x0 + W * 0.5, y0 + H * 0.55)], fill=shirt)
            d.rectangle([x0 + W * 0.46, y0 + H * 0.05, x1 - W * 0.46, y0 + H * 0.5], fill=tie)
            for yy in (0.62, 0.78): d.ellipse([x0 + W * 0.48, y0 + H * yy, x0 + W * 0.53, y0 + H * yy + 2], fill=(200, 170, 80, 255))
            d.rectangle([x0 + W * 0.68, y0 + H * 0.2, x0 + W * 0.8, y0 + H * 0.25], fill=(220, 40, 60, 255))   # платочек

    def extra(m):
        m.bone('hat', 'head', [0, 31, 0], [([-5.8, 30.8, -5.8], [11.6, 0.8, 11.6], flat(hat), 6),
                                           ([-4.6, 31.6, -4.6], [9.2, 3.4, 9.2], flat(hat), 6),
                                           ([-4.7, 31.8, -4.7], [9.4, 0.9, 9.4], flat(tie), 6)])
        m.bone('cigar', 'head', [0, 24, -4.5], [([1.2, 24.2, -6.2], [0.6, 0.6, 2.2], flat(hexc('6a4020')), 10),
                                                ([1.2, 24.2, -6.5], [0.6, 0.6, 0.3], flat(hexc('ff6020'), noise=0.2), 10)])
        m.bone('coat', 'body', [0, 21, 2], [([-3.8, 6, 1.5], [7.6, 15, 0.8], flat(suit), 3)], rotation=[4, 0, 0])
        # автомат-«томми» в правой руке
        hx_ = -4.2
        gun = [([hx_ - 0.5, 11.5, -3], [1.0, 1.2, 9], flat(hexc('2a2a2a')), 8),
               ([hx_ - 0.6, 10.5, -5.5], [1.2, 1.0, 3], flat(hexc('5a3a1a')), 8),
               ([hx_ - 1.1, 9.6, -2.6], [2.2, 2.2, 2.2], flat(hexc('3a3a3a')), 8),     # диск
               ([hx_ - 0.4, 12.6, -9], [0.8, 0.8, 6], flat(hexc('1a1a1a')), 8),       # ствол
               ([hx_ - 0.5, 9.5, 4.5], [1.0, 2.2, 3.5], flat(hexc('5a3a1a')), 8)]     # приклад
        m.bone('gun', 'right_arm', [hx_, 12.5, 0], gun)

    G.humanoid(m, skin, face, jacket, flat(suit), flat(hexc('18181c')), flat(hexc('0a0a0a')), fem=False, extra=extra)
    m.save('don')


# ------------------------------------------------------------------ Крысиный король
def six_pack(base):
    """Рельефный пресс: 3 ряда кубиков, тени между ними, блики сверху (для смеха)."""
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        k = {'up': 1.1, 'down': 0.7, 'north': 1.0, 'south': 0.9, 'east': 0.85, 'west': 0.85}[fc]
        for x in range(x0, x1):
            for y in range(y0, y1): d.point((x, y), mul(base, k))
        if fc != 'north': return
        for x in range(x0, x1):
            d.point((x, y0), mul(base, 1.35)); d.point((x, y1 - 1), mul(base, 0.6))
        for y in range(y0, y1):
            d.point((x0, y), mul(base, 1.15)); d.point((x1 - 1, y), mul(base, 0.7))
    return paint


def rat_king():
    m = G.Model('rat_king', 1024)
    grey, pink, purple, gold = hexc('5a5048'), hexc('d08888'), hexc('4a2060'), hexc('e0b840')
    f = fur(grey, 11)
    m.bone('root', None, [0, 0, 0])
    for side, nm in ((-1, 'right_leg'), (1, 'left_leg')):
        x = 3.2 * side
        m.bone(nm, 'root', [x, 13, 0], [([x - 2, 0, -2], [4, 13, 4], f, 3), ([x - 2.4, 0, -4.4], [4.8, 1.6, 4.4], flat(pink), 4)])
    m.bone('body', 'root', [0, 13, 0], [([-5.5, 12, -3.8], [11, 13, 8], f, 3),
                                        ([-4.6, 13, -4.2], [9.2, 10, 0.6], flat(hexc('9a8a7a')), 4)]
           + [([-3.4 + col * 3.6, 14 + row * 2.6, -4.7], [3.0, 2.2, 0.8], six_pack(hexc('c4b4a2')), 10) for row in range(3) for col in range(2)]
           + [([-4.2 + col * 4.4, 21.8, -4.6], [3.8, 1.8, 0.7], flat(hexc('b8a898')), 8) for col in range(2)])
    m.bone('cloak', 'body', [0, 25, 3], [([-6.5, 4, 3.8], [13, 21, 1], B.gown(purple, mul(purple, 0.55), gold, seed=21), 3)], rotation=[8, 0, 0])
    m.bone('collar', 'body', [0, 25, 0], [([-6.5, 23.5, -4.5], [13, 2.5, 9.5], fur(hexc('e8e0d0'), 12, hexc('a8a090')), 4)])
    m.bone('head', 'body', [0, 25, -1], [([-4, 25, -6], [8, 7, 8], eyes_side(grey, (255, 60, 60, 255), 13), 8),
                                         ([-2.5, 25, -10.5], [5, 4, 4.5], snout_face(mul(grey, 1.1), None, pink, 14), 8),
                                         ([-5.2, 31, -3], [3, 3.5, 1], flat(pink), 8), ([2.2, 31, -3], [3, 3.5, 1], flat(pink), 8)])
    crown = [([-3.5, 32, -5], [7, 1.6, 6], flat(gold, noise=0.1), 8)]
    for i, x in enumerate((-3.2, -1.2, 0.8, 2.6)):
        crown.append(([x, 33.6, -5], [0.8, 1.8 + (i % 2), 0.8], flat(gold, noise=0.1), 10))
    crown.append(([-0.5, 33.2, -5.3], [1, 1, 0.4], flat(hexc('e02040')), 10))
    m.bone('crown', 'head', [0, 32, -2], crown, rotation=[0, 0, -8])
    for side, nm in ((-1, 'right_arm'), (1, 'left_arm')):
        x = 7 * side
        m.bone(nm, 'body', [x, 24, 0], [([x - 1.6, 13, -1.6], [3.2, 11, 3.2], f, 3), ([x - 1.8, 11.5, -2.2], [3.6, 2, 3.6], flat(pink), 4)])
    # посох-дудка в правой руке
    m.bone('staff', 'right_arm', [-7, 13, 0], [([-7.5, -2, -0.5], [1, 30, 1], flat(hexc('6a4a2a')), 6),
                                               ([-8.2, 27, -1.2], [2.4, 2.4, 2.4], flat(gold, noise=0.1), 8),
                                               ([-7.3, 29.4, -0.3], [0.6, 3, 0.6], flat(hexc('3a2a1a')), 8)])
    m.bone('tail', 'body', [0, 13, 4], [([-0.8, 12.2, 4], [1.6, 1.6, 9], flat(pink), 4)], rotation=[25, 0, 0])
    m.bone('tail2', 'tail', [0, 12.6, 13], [([-0.6, 12.2, 13], [1.2, 1.2, 9], flat(pink), 4)], rotation=[-20, 0, 0])
    m.save('rat_king')


def boss_anims():
    S = lambda t: math.sin(2 * math.pi * t)
    k = B.keys
    src = json.load(open(os.path.join(G.ROOT, 'animations', 'civ_npc.animation.json')))['animations']
    once = lambda ln, bones: {'animation_length': ln, 'bones': bones}
    # --- Дон (гуманоид — берём idle/walk из общих)
    gun_hold = {'right_arm': {'rotation': {'0': [-55, 10, 0]}}, 'left_arm': {'rotation': {'0': [-50, -35, 0]}}}
    D = {'idle': {'loop': True, 'animation_length': 3.0, 'bones': {**src['idle']['bones'], 'right_arm': {'rotation': {'0': [-20, 10, 0]}}}},
         'walk': {'loop': True, 'animation_length': 0.8, 'bones': {**src['walk']['bones'], 'right_arm': {'rotation': {'0': [-25, 10, 0]}}}},
         'attack': once(0.6, {'right_arm': {'rotation': {'0': [-20, 10, 0], '0.2': [-140, 20, 0], '0.35': [-40, -10, 0], '0.6': [-20, 10, 0]}},
                              'body': {'rotation': {'0': [0, 0, 0], '0.35': [10, -15, 0], '0.6': [0, 0, 0]}}}),
         'burst': once(1.4, {**gun_hold, 'body': {'rotation': k(lambda t: [0, 0, 1.5 * S(t * 6)], 1.4)},
                             'gun': {'position': k(lambda t: [0, 0, 0.6 * abs(S(t * 6))], 1.4)}}),
         'spray': once(2.4, {**gun_hold, 'body': {'rotation': k(lambda t: [0, 360 * t, 0], 2.4, 16)}}),
         'grenade': once(1.1, {'left_arm': {'rotation': {'0': [0, 0, 0], '0.5': [-160, 0, -20], '0.7': [-40, 0, 0], '1.1': [0, 0, 0]}},
                               'body': {'rotation': {'0': [0, 0, 0], '0.5': [-10, 20, 0], '0.7': [15, -10, 0], '1.1': [0, 0, 0]}}}),
         'call': once(1.6, {'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-150, 0, 0], '1.2': [-150, 0, 0], '1.6': [0, 0, 0]}},
                            'head': {'rotation': {'0': [0, 0, 0], '0.4': [-15, 0, 0], '1.6': [0, 0, 0]}}})}
    json.dump({'format_version': '1.8.0', 'animations': D}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'don.animation.json')), 'w'))
    # --- Крысиный король
    K = {'idle': {'loop': True, 'animation_length': 2.4, 'bones': {
            'body': {'rotation': k(lambda t: [6 + 2 * S(t), 0, 0], 2.4)}, 'head': {'rotation': k(lambda t: [3 * S(t), 10 * S(t / 2), 0], 2.4)},
            'tail': {'rotation': k(lambda t: [25, 20 * S(t), 0], 2.4)}, 'tail2': {'rotation': k(lambda t: [-20, 25 * S(t - 0.2), 0], 2.4)},
            'cloak': {'rotation': k(lambda t: [8 + 3 * S(t), 0, 0], 2.4)}}},
         'walk': {'loop': True, 'animation_length': 0.9, 'bones': {
            'right_leg': {'rotation': k(lambda t: [30 * S(t), 0, 0], 0.9)}, 'left_leg': {'rotation': k(lambda t: [-30 * S(t), 0, 0], 0.9)},
            'left_arm': {'rotation': k(lambda t: [20 * S(t), 0, 0], 0.9)}, 'body': {'rotation': k(lambda t: [10, 0, 4 * S(t)], 0.9)},
            'tail': {'rotation': k(lambda t: [25, 30 * S(t), 0], 0.9)}, 'cloak': {'rotation': k(lambda t: [18 + 5 * S(t * 2), 0, 0], 0.9)}}},
         'attack': once(0.6, {'left_arm': {'rotation': {'0': [0, 0, 0], '0.2': [-120, 0, -30], '0.35': [-30, 0, 10], '0.6': [0, 0, 0]}}}),
         'summon': once(1.6, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.5': [-150, 0, 10], '1.6': [-150, 0, 10]}},
                              'left_arm': {'rotation': {'0': [0, 0, 0], '0.5': [-150, 0, -10], '1.6': [-150, 0, -10]}},
                              'head': {'rotation': {'0': [0, 0, 0], '0.5': [-35, 0, 0], '1.6': [-35, 0, 0]}}}),
         'burrow': once(1.0, {'root': {'position': {'0': [0, 0, 0], '1.0': [0, -36, 0]}}, 'body': {'rotation': {'0': [0, 0, 0], '0.4': [40, 0, 0]}}}),
         'emerge': once(1.0, {'root': {'position': {'0': [0, -36, 0], '0.6': [0, 4, 0], '1.0': [0, 0, 0]}}}),
         'sweep': once(1.0, {'root': {'rotation': {'0': [0, 0, 0], '0.7': [0, 360, 0], '1.0': [0, 360, 0]}},
                             'tail': {'rotation': {'0': [25, 0, 0], '0.35': [-10, 0, 0], '0.7': [25, 0, 0]}}})}
    json.dump({'format_version': '1.8.0', 'animations': K}, open(G.ensure(os.path.join(G.ROOT, 'animations', 'rat_king.animation.json')), 'w'))


if __name__ == '__main__':
    if '--anim' not in sys.argv:
        rat(); dog(); don(); rat_king()
    beast_anims(); boss_anims()
    print('этаж 2: модели готовы')
