#!/usr/bin/env python3
"""Боссы этажа 5 (1.0): Инопланетный дипломат, Галактический червь (голова + сегмент), демоница Пустоты; анимации боя Луны."""
import json, math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G
import bosses as B
G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, strands, stripes, lace_trim = G.hexc, G.flat, G.mul, G.strands, G.stripes, G.lace_trim
S = lambda t: math.sin(2 * math.pi * t)
k = B.keys
once = lambda ln, bones: {'animation_length': ln, 'bones': bones}


def npc_base():
    return json.load(open(os.path.join(G.ROOT, 'animations', 'civ_npc.animation.json')))['animations']


def save_anims(name, A):
    json.dump({'format_version': '1.8.0', 'animations': A}, open(G.ensure(os.path.join(G.ROOT, 'animations', name + '.animation.json')), 'w'))


# ------------------------------------------------------------------ дипломат
def diplomat():
    m = G.Model('diplomat', 1024)
    skin, suit, shirt, tie, gold, eye = hexc('8fb4c8'), hexc('22263a'), hexc('e8ecf4'), hexc('c8242c'), hexc('e8c45a'), hexc('101018')

    def face(fc, d, r):
        x0, y0, x1, y1 = r
        G.flat(skin, noise=0.05)(fc, d, r)
        if fc != 'north': return
        w, h = x1 - x0, y1 - y0
        # огромные чёрные миндалевидные глаза
        d.ellipse([x0 + w * 0.12, y0 + h * 0.38, x0 + w * 0.44, y0 + h * 0.62], fill=eye)
        d.ellipse([x0 + w * 0.56, y0 + h * 0.38, x0 + w * 0.88, y0 + h * 0.62], fill=eye)
        d.ellipse([x0 + w * 0.2, y0 + h * 0.42, x0 + w * 0.27, y0 + h * 0.48], fill=(220, 240, 255, 255))
        d.ellipse([x0 + w * 0.64, y0 + h * 0.42, x0 + w * 0.71, y0 + h * 0.48], fill=(220, 240, 255, 255))
        d.line([x0 + w * 0.4, y0 + h * 0.82, x0 + w * 0.6, y0 + h * 0.82], fill=mul(skin, 0.55), width=3)

    def extra(m):
        # вытянутый купол черепа
        m.bone('dome', 'head', [0, 31, 0], [([-5.5, 30.5, -4.5], [11, 4, 9], flat(skin, noise=0.05), 4), ([-4.5, 34.5, -3.5], [9, 2, 7], flat(mul(skin, 1.05)), 4)])
        m.bone('antenna', 'dome', [0, 36.5, 0], [([-0.3, 36.5, -0.3], [0.6, 3, 0.6], flat(mul(skin, 0.8)), 8), ([-0.7, 39.5, -0.7], [1.4, 1.4, 1.4], flat(hexc('60f0ff')), 8)])
        m.bone('jacket', 'body', [0, 21, 0], [([-3.9, 9.5, -2.1], [7.8, 12, 4.2], flat(suit, noise=0.03), 3)])
        m.bone('lapels', 'jacket', [0, 21, -2.1], [([-1.6, 15.0, -2.25], [3.2, 6.4, 0.2], flat(shirt), 6), ([-0.45, 14.5, -2.4], [0.9, 6.0, 0.2], flat(tie), 8),
                                                  ([-3.4, 18.0, -2.3], [1.0, 1.0, 0.2], flat(gold), 8)])
        m.bone('tablet', 'left_arm', [4.5, 13, 0], [([4.2, 9.5, -4.5], [1.0, 6.5, 5.0], flat(hexc('1a1a22')), 6), ([5.15, 10.0, -4.0], [0.1, 5.5, 4.0], flat(hexc('40d0ff')), 8)])
        m.bone('papers', 'right_arm', [-4.5, 13, 0], [([-6.6, 10.5, -3.0], [2.4, 0.6, 4.0], flat(hexc('f4f0e0')), 8), ([-6.5, 11.1, -2.8], [2.2, 0.4, 3.6], flat(hexc('e8e4d4')), 8)])
        m.bone('badge', 'jacket', [2, 18, -2.2], [([1.3, 17.0, -2.35], [1.6, 1.6, 0.2], flat(hexc('60f0ff')), 8)])

    G.humanoid(m, skin, face, flat(suit, noise=0.03), flat(suit), flat(suit), flat(hexc('101014')), head_size=(10, 9, 8), fem=False, extra=extra)
    m.save('diplomat')
    src = npc_base()
    A = {'idle': src['idle'], 'walk': src['walk'],
         'attack': once(0.55, {'left_arm': {'rotation': {'0': [0, 0, 0], '0.18': [-130, 0, -10], '0.32': [-30, 0, 10], '0.55': [0, 0, 0]}}}),
         'throw': once(0.6, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.2': [-150, 0, 0], '0.35': [-60, 0, 0], '0.6': [0, 0, 0]}}}),
         'sign': once(1.2, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.3': [-90, 20, 0], '1.2': [-90, 20, 0]}}, 'left_arm': {'rotation': {'0': [0, 0, 0], '0.3': [-80, -20, 0], '1.2': [-80, -20, 0]}}}),
         'audit': once(1.4, {'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-170, 0, -10], '1.4': [-170, 0, -10]}}, 'body': {'rotation': {'0': [0, 0, 0], '0.4': [-8, 0, 0], '1.4': [0, 0, 0]}}}),
         'summon': once(1.4, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-160, 0, 20], '1.4': [-160, 0, 20]}}, 'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-160, 0, -20], '1.4': [-160, 0, -20]}}})}
    save_anims('diplomat', A)


# ------------------------------------------------------------------ червь
def worm():
    hide, belly, glow, tooth, dark = hexc('3a2a5a'), hexc('7a5aa0'), hexc('ff60e0'), hexc('f0e8ff'), hexc('140c22')
    m = G.Model('worm_head', 512)
    m.bone('root', None, [0, 0, 0])
    m.bone('skull', 'root', [0, 8, 0], [([-9, 0, -8], [18, 16, 18], stripes(hide, mul(hide, 0.75), 4, seed=3), 2),
                                        ([-7, 15.5, -6], [14, 2, 14], flat(mul(hide, 1.2)), 2),
                                        ([-10, 4, -6], [1, 8, 8], flat(glow), 4), ([9, 4, -6], [1, 8, 8], flat(glow), 4)])
    m.bone('jaw_top', 'skull', [0, 9, -8], [([-8, 9, -20], [16, 6, 13], stripes(hide, belly, 5, seed=5), 2)] +
           [([-7 + i * 2.6, 7.5, -20 + (i % 2)], [1.2, 2, 1.2], flat(tooth), 8) for i in range(6)])
    m.bone('jaw_bot', 'skull', [0, 7, -8], [([-8, 2, -20], [16, 5, 13], stripes(belly, hide, 5, seed=6), 2)] +
           [([-7 + i * 2.6, 6.5, -19.5], [1.2, 2, 1.2], flat(tooth), 8) for i in range(6)])
    m.bone('maw', 'skull', [0, 7, -8], [([-7, 4, -8.2], [14, 7, 0.5], flat(dark), 4), ([-2, 6, -8.4], [4, 3, 0.4], flat(glow), 6)])
    for i, (x, y) in enumerate(((-7, 14), (7, 14), (-9, 10), (9, 10))):
        m.bone('horn%d' % i, 'skull', [x, y, 2], [([x - 1, y, 0], [2, 6, 2], flat(tooth), 6, ([x, y, 2], [30, 0, -20 if x < 0 else 20]))])
    m.save('worm_head')

    s = G.Model('worm_seg', 512)
    s.bone('root', None, [0, 0, 0])
    s.bone('ring', 'root', [0, 8, 0], [([-8, 0, -7], [16, 15, 14], stripes(hide, mul(hide, 0.7), 3, seed=8), 2),
                                       ([-7, 0.5, -7.5], [14, 3, 15], flat(belly), 2),
                                       ([-1.5, 15, -5], [3, 4, 3], flat(glow), 6), ([-1.5, 15, 2], [3, 3, 3], flat(glow), 6),
                                       ([-8.5, 6, -2], [0.5, 4, 4], flat(glow), 6), ([8, 6, -2], [0.5, 4, 4], flat(glow), 6)])
    s.save('worm_seg')
    A = {'idle': {'loop': True, 'animation_length': 2.0, 'bones': {'jaw_top': {'rotation': k(lambda t: [-6 - 6 * S(t), 0, 0], 2.0)},
                                                                   'jaw_bot': {'rotation': k(lambda t: [4 + 4 * S(t), 0, 0], 2.0)}}},
         'walk': {'loop': True, 'animation_length': 1.0, 'bones': {'jaw_top': {'rotation': k(lambda t: [-10 - 8 * S(t), 0, 0], 1.0)},
                                                                   'jaw_bot': {'rotation': k(lambda t: [6 + 5 * S(t), 0, 0], 1.0)}}},
         'attack': once(0.6, {'jaw_top': {'rotation': {'0': [0, 0, 0], '0.25': [-50, 0, 0], '0.4': [0, 0, 0], '0.6': [0, 0, 0]}},
                              'jaw_bot': {'rotation': {'0': [0, 0, 0], '0.25': [30, 0, 0], '0.4': [0, 0, 0]}}}),
         'roar': once(1.6, {'jaw_top': {'rotation': {'0': [0, 0, 0], '0.3': [-60, 0, 0], '1.3': [-60, 0, 0], '1.6': [0, 0, 0]}},
                            'jaw_bot': {'rotation': {'0': [0, 0, 0], '0.3': [35, 0, 0], '1.3': [35, 0, 0], '1.6': [0, 0, 0]}},
                            'skull': {'rotation': {'0': [0, 0, 0], '0.3': [-15, 0, 0], '1.3': [-15, 0, 0], '1.6': [0, 0, 0]}}}),
         'stunned': {'loop': True, 'animation_length': 1.5, 'bones': {'skull': {'rotation': k(lambda t: [8, 0, 10 * S(t)], 1.5)},
                                                                      'jaw_bot': {'rotation': k(lambda t: [25, 0, 0], 1.5)}}}}
    save_anims('worm', A)


# ------------------------------------------------------------------ демоница Пустоты
def void_demon():
    m = G.Model('void_demon', 1024)
    skin, hair = hexc('d8c8f0'), hexc('2a1440')
    dress, deep, glow, horn = hexc('1a0c2a'), hexc('0c0614'), hexc('d040ff'), hexc('2a2236')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('ff40c0'), sex='f', expr='sly', fangs=True, bangs='hime',
                             brow=hexc('3a1a50'), lash=hexc('1a0a24'), blush_c=(220, 100, 180, 120)), long_hair=True)

    def extra(m):
        hp = strands(mul(hair, 1.5), mul(glow, 0.6), seed=77)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.3, -4.9], [10.2, 2.9, 10.2], hp, 4), ([-5.3, 18.5, 2.6], [10.6, 12, 2.4], hp, 4),
                                             ([-5.6, 21.5, -3.0], [1.3, 9, 6.2], hp, 4), ([4.3, 21.5, -3.0], [1.3, 9, 6.2], hp, 4)])
        m.bone('hair_long', 'hair', [0, 18.5, 4], [([-5.0, 8.0, 2.8], [10.0, 10.5, 2.2], hp, 4)])
        for side, nm in ((-1, 'r'), (1, 'l')):
            x = 3.8 * side
            m.bone('horn_' + nm, 'head', [x, 31, 0], [([x - 0.9, 31, -1.0], [1.8, 3.2, 1.8], flat(horn), 8, ([x, 31, 0], [-15, 0, 18 * side])),
                                                    ([x - 0.6 + 1.6 * side, 33.6, -0.7], [1.2, 2.6, 1.2], flat(mul(horn, 1.6)), 8, ([x, 31, 0], [-30, 0, 38 * side]))])
        m.bone('halo', 'head', [0, 35, 3], [([-4, 35.5, 2.5], [8, 0.6, 0.6], flat(glow), 10), ([-4, 35.5, 2.5], [0.6, 0.6, 0.6], flat(glow), 10)], rotation=[0, 0, 0])
        m.bone('dress', 'body', [0, 13, 0], [([-4.6, 4.0, -3.0], [9.2, 9.0, 6.0], stripes(dress, deep, 3, seed=9), 3), ([-4.8, 3.6, -3.2], [9.6, 0.8, 6.4], flat(glow), 6)])
        m.bone('collar', 'body', [0, 21, 0], [([-3.2, 20.0, -1.8], [6.4, 1.2, 3.6], flat(glow), 6)])
        m.bone('gem', 'body', [0, 18, -1.6], [([-0.7, 17.4, -1.95], [1.4, 1.4, 0.4], flat(hexc('ff60ff')), 10)])
        # крылья-мембраны Пустоты
        for side, nm in ((-1, 'r'), (1, 'l')):
            m.bone('wing_' + nm, 'body', [1.5 * side, 19, 1.6], [([1.5 * side - (12 if side < 0 else 0), 11, 1.6], [12, 12, 0.5], G.membrane(hexc('2a1040'), glow, seed=5 + side), 3,
                                                                  ([1.5 * side, 19, 1.6], [0, 25 * side, 0]))])
        m.bone('tail', 'body', [0, 12, 1.5], [([-0.6, 4, 1.5], [1.2, 8, 1.2], flat(horn), 6, ([0, 12, 1.5], [35, 0, 0])), ([-1.2, 1.0, 6.0], [2.4, 2.4, 1.0], flat(glow), 8, ([0, 12, 1.5], [35, 0, 0]))])

    G.humanoid(m, skin, face, flat(dress, noise=0.03), flat(skin), flat(hexc('140a20')), flat(hexc('0a0610')), fem=True, extra=extra,
               bust=flat(dress), top=flat(dress))
    m.save('void_demon')
    src = npc_base()
    wings = lambda amp, ln: {'wing_r': {'rotation': k(lambda t: [0, -amp * S(t), 0], ln)}, 'wing_l': {'rotation': k(lambda t: [0, amp * S(t), 0], ln)}}
    A = {'idle': {'loop': True, 'animation_length': 2.0, 'bones': {**src['idle'].get('bones', {}), **wings(18, 2.0),
                                                                   'root': {'position': k(lambda t: [0, 1.2 * S(t), 0], 2.0)}}},
         'walk': {'loop': True, 'animation_length': 1.0, 'bones': {**wings(25, 1.0), 'root': {'position': k(lambda t: [0, 1.0 * S(t), 0], 1.0)}}},
         'attack': once(0.6, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.2': [-140, 0, 0], '0.35': [-40, 0, 0], '0.6': [0, 0, 0]}}}),
         'cast': once(1.4, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-170, 0, 20], '1.4': [-170, 0, 20]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.4': [-170, 0, -20], '1.4': [-170, 0, -20]}}}),
         'point': once(1.0, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.25': [-90, 0, 0], '1.0': [-90, 0, 0]}}}),
         'laugh': once(1.5, {'body': {'rotation': k(lambda t: [-6 * abs(S(t * 3)), 0, 0], 1.5)}, 'head': {'rotation': k(lambda t: [-12 * abs(S(t * 3)), 0, 0], 1.5)}})}
    save_anims('void_demon', A)


# ------------------------------------------------------------------ Луна в бою (модель — та же, что у NPC)
def luna_boss_anims():
    src = npc_base()
    A = {'idle': src['idle'], 'walk': src['walk'],
         'attack': once(0.45, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.15': [-120, 0, -30], '0.3': [-20, 0, 20], '0.45': [0, 0, 0]}}}),
         'slash': once(0.9, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.35': [-150, -30, 0], '0.55': [-30, 40, 0], '0.9': [0, 0, 0]}},
                             'body': {'rotation': {'0': [0, 0, 0], '0.35': [0, -20, 0], '0.55': [0, 25, 0], '0.9': [0, 0, 0]}}}),
         'cast': once(1.3, {'right_arm': {'rotation': {'0': [0, 0, 0], '0.35': [-170, 0, 15], '1.3': [-170, 0, 15]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.35': [-170, 0, -15], '1.3': [-170, 0, -15]}}}),
         'dash': once(0.6, {'body': {'rotation': {'0': [0, 0, 0], '0.15': [25, 0, 0], '0.6': [0, 0, 0]}},
                            'right_arm': {'rotation': {'0': [0, 0, 0], '0.15': [40, 0, 20], '0.6': [0, 0, 0]}},
                            'left_arm': {'rotation': {'0': [0, 0, 0], '0.15': [40, 0, -20], '0.6': [0, 0, 0]}}})}
    save_anims('luna_boss', A)


if __name__ == '__main__':
    diplomat(); worm(); void_demon(); luna_boss_anims()
    print('боссы этажа 5: модели готовы')
