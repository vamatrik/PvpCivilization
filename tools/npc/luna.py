#!/usr/bin/env python3
"""
3D-модель Луны (GeckoLib) в том же стиле, что Кармилла и Элеонора (генератор gen_assets.py из хоррор-мода).
Лавандовые волосы с двумя хвостами, фиолетовые глаза, тёмно-фиолетовое худи-платье с матросским воротником,
золотой полумесяц, полосатые гольфы. Запуск: python3 tools/npc/luna.py  (пишет в assets/pvpciv).
Также собирает общий файл анимаций civ_npc.animation.json = анимации хоррор-мода + свои (руки скрещены, тащит, кусает).
"""
import json, math, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G

G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, strands, lace_trim, stripes = G.hexc, G.flat, G.mul, G.strands, G.lace_trim, G.stripes


def luna():
    m = G.Model('luna', 1024)
    skin = hexc('f7e6dc'); hair = hexc('c9b6ee')
    purple, deep, white, gold = hexc('3a2a5a'), hexc('241a38'), hexc('f1eef6'), hexc('e8c45a')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('9a5ae0'), sex='f', expr='stern', bangs='hime',
                             brow=hexc('8a76b0'), lash=hexc('2a1a3a'), blush_c=(245, 120, 160, 130)), long_hair=True)

    def extra(m):
        hp = strands(mul(hair, 1.18), mul(hair, 0.72), seed=41)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.3, -4.9], [10.2, 2.9, 10.2], hp, 4),
                                             ([-5.3, 21.5, 2.6], [10.6, 9, 2.4], hp, 4),
                                             ([-5.6, 22.5, -3.0], [1.3, 8, 6.2], hp, 4), ([4.3, 22.5, -3.0], [1.3, 8, 6.2], hp, 4)])
        m.bone('hair_mid', 'hair', [0, 22, 4.2], [([-5.2, 15.5, 2.8], [10.4, 6.5, 2.4], hp, 4)])
        tips = []
        for k, (x, ln, ang) in enumerate(((-4.8, 4.5, 6), (-2.2, 5.5, 2), (0.4, 5.5, -2), (2.8, 4.5, -6))):
            tips.append(([x, 15.6 - ln, 2.9], [2.4, ln, 2.0], hp, 4, ([x + 1.2, 15.6, 3.9], [0, 0, ang])))
        m.bone('hair_tip', 'hair_mid', [0, 15.6, 3.9], tips)
        # два хвоста по бокам, с бантиками-резинками
        for side, nm in ((-1, 'r'), (1, 'l')):
            x = 5.6 * side
            m.bone('twin_' + nm, 'head', [x, 29.5, 1.5], [([x - 1.6, 21, 0.0], [3.2, 9, 3.2], hp, 4, ([x, 29.5, 1.6], [0, 0, -8 * side])),
                                                        ([x - 0.9, 29, 0.7], [1.8, 1.4, 1.8], flat(purple, noise=0.05), 6)])
            m.bone('twin_tip_' + nm, 'twin_' + nm, [x + 0.3 * side, 21, 1.6], [([x - 1.2 + 0.6 * side, 13.5, 0.4], [2.4, 7.5, 2.4], hp, 4, ([x + 0.6 * side, 21, 1.6], [0, 0, -6 * side]))])
        m.bone('lock_r', 'head', [-4.6, 27, -3.9], [([-5.7, 20.5, -4.8], [1.6, 7.5, 2.0], hp, 4)])
        m.bone('lock_l', 'head', [4.6, 27, -3.9], [([4.1, 20.5, -4.8], [1.6, 7.5, 2.0], hp, 4)])
        m.bone('tuft', 'hair', [0, 31.5, 0], [([-0.5, 32.0, -1.0], [1.0, 3.0, 1.6], hp, 6)], rotation=[-25, 0, 20])
        # заколка-полумесяц над левым виском
        m.bone('moon', 'head', [4.4, 30, -2.5], [([4.6, 29.2, -3.6], [0.6, 2.6, 0.8], flat(gold, noise=0.08), 8),
                                                ([4.6, 31.6, -3.0], [0.6, 0.7, 1.2], flat(gold, noise=0.08), 8),
                                                ([4.6, 28.6, -3.0], [0.6, 0.7, 1.2], flat(gold, noise=0.08), 8)])
        # матросский воротник с белым кантом и золотой полумесяц-брошь
        m.bone('collar', 'body', [0, 21, 0], [([-3.4, 19.6, -0.2], [6.8, 1.6, 2.2], lace_trim(white, purple, 'bottom', 0.3), 4),
                                              ([-0.6, 18.0, -1.95], [1.2, 1.2, 0.3], flat(gold, noise=0.1), 8)])
        # юбка в складку
        m.bone('skirt', 'body', [0, 13, 0], [([-4.0, 8.5, -2.6], [8.0, 4.5, 5.2], stripes(deep, mul(deep, 1.35), 3, seed=7), 3)])
        # худи-капюшон за спиной
        m.bone('hood', 'body', [0, 21, 2], [([-3.4, 18.4, 1.6], [6.8, 3.2, 1.6], flat(purple), 3)])
        # рукава-фонарики (манжеты)
        m.bone('cuff_r', 'right_arm', [-4, 20.5, 0], [([-5.25, 12.2, -1.25], [2.5, 1.2, 2.5], flat(white), 4)])
        m.bone('cuff_l', 'left_arm', [4, 20.5, 0], [([2.75, 12.2, -1.25], [2.5, 1.2, 2.5], flat(white), 4)])
        # кеды
        m.bone('shoe_r', 'right_leg', [-1.5, 12, 0], [([-3.0, 0, -2.0], [3.0, 1.4, 3.6], flat(hexc('5a3a8a')), 4)])
        m.bone('shoe_l', 'left_leg', [1.5, 12, 0], [([0.0, 0, -2.0], [3.0, 1.4, 3.6], flat(hexc('5a3a8a')), 4)])

    sk = flat(skin, noise=0.01)
    socks = G.split_v(sk, stripes(hexc('1c1626'), white, 2, seed=3), 0.38, trim=purple)   # полосатые гольфы выше колена
    G.humanoid(m, skin, face, flat(purple), G.split_v(flat(purple), sk, 0.82), socks, flat(hexc('5a3a8a')), fem=True,
               top=G.neckline(flat(purple), sk, white, 'v', front=0.6, scallop=False),
               bust=flat(purple), extra=extra)
    m.save('luna_hd')


def animations():
    """Анимации хоррор-мода + свои."""
    src = os.path.join(HERE, 'actor.animation.json')
    data = json.load(open(src))
    A = data['animations']
    sway = lambda a, per, ph=0: {str(round(per * k / 8, 3)): [0, 0, round(a * math.sin(2 * math.pi * k / 8 + ph), 2)] for k in range(9)}
    # руки скрещены на груди, голова чуть отвёрнута — «хмф!»
    A['cross'] = {'loop': True, 'animation_length': 3.0, 'bones': {
        'right_arm': {'rotation': {'0': [-60, 35, 0]}}, 'left_arm': {'rotation': {'0': [-62, -35, 0]}},
        'head': {'rotation': {'0': [-4, 22, 0], '1.5': [-2, 26, 3], '3': [-4, 22, 0]}},
        'twin_r': {'rotation': sway(3, 3.0)}, 'twin_l': {'rotation': sway(-3, 3.0, 0.6)},
        'body': {'scale': {'0': [1, 1, 1], '1.5': [1, 1.012, 1], '3': [1, 1, 1]}}}}
    # тащит за руку: правая рука вытянута назад-вниз, шаг
    A['drag'] = {'loop': True, 'animation_length': 0.8, 'bones': {**A['walk']['bones'],
        'right_arm': {'rotation': {'0': [30, 0, 25]}},
        'body': {'rotation': {'0': [10, -12, 0]}},
        'head': {'rotation': {'0': [0, -30, 0]}}}}
    # укус: наклон к жертве, руки на плечах
    A['bite'] = {'loop': True, 'animation_length': 1.6, 'bones': {
        'body': {'rotation': {'0': [20, 0, 0], '0.8': [24, 0, 0], '1.6': [20, 0, 0]}},
        'head': {'rotation': {'0': [25, 0, -15], '0.8': [32, 0, -18], '1.6': [25, 0, -15]}},
        'right_arm': {'rotation': {'0': [-70, -20, 0]}}, 'left_arm': {'rotation': {'0': [-70, 20, 0]}},
        'wing_r': {'rotation': {'0': [0, 20, 30], '0.8': [0, 26, 40], '1.6': [0, 20, 30]}},
        'wing_l': {'rotation': {'0': [0, -20, -30], '0.8': [0, -26, -40], '1.6': [0, -20, -30]}}}}
    # сидит на троне / кровати
    A['sit'] = {'loop': True, 'animation_length': 3.0, 'bones': {
        'root': {'position': {'0': [0, -6, 0]}},
        'right_leg': {'rotation': {'0': [-88, 8, 0]}}, 'left_leg': {'rotation': {'0': [-88, -8, 0]}},
        'right_arm': {'rotation': {'0': [-25, 0, 8]}}, 'left_arm': {'rotation': {'0': [-25, 0, -8]}},
        'head': {'rotation': {'0': [0, 0, 0], '1.5': [3, 6, 0], '3': [0, 0, 0]}}}}
    # крылья напоказ: раскрыть и медленно взмахивать (Кармилла делает это время от времени и в разговоре)
    def wing_keys(sign, amp, base_z, base_y, per, ph=0.0):
        return {str(round(per * k / 12, 3)): [round(-6 + 4 * math.sin(2 * math.pi * k / 12 + ph), 2), round(sign * base_y, 2),
                                               round(sign * (base_z + amp * math.sin(2 * math.pi * k / 12 + ph)), 2)] for k in range(13)}
    spread_wings = {'wing_r': {'rotation': wing_keys(1, 8, -28, -18, 2.4)}, 'wing_l': {'rotation': wing_keys(-1, 8, -28, -18, 2.4)},
                    'wing_r_tip': {'rotation': wing_keys(1, 10, -14, -6, 2.4, -0.9)}, 'wing_l_tip': {'rotation': wing_keys(-1, 10, -14, -6, 2.4, -0.9)}}
    A['spread'] = {'loop': True, 'animation_length': 2.4, 'bones': {**A['idle']['bones'], **spread_wings,
        'right_arm': {'rotation': {'0': [-15, 0, 25]}}, 'left_arm': {'rotation': {'0': [-15, 0, -25]}},
        'head': {'rotation': {'0': [-6, 0, 0], '1.2': [-3, 4, 0], '2.4': [-6, 0, 0]}}}}
    A['sit_spread'] = {'loop': True, 'animation_length': 2.4, 'bones': {**A['sit']['bones'], **spread_wings}}
    out = os.path.join(G.ROOT, 'animations', 'civ_npc.animation.json')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    json.dump(data, open(out, 'w'), ensure_ascii=False)


if __name__ == '__main__':
    if '--anim' not in sys.argv: luna()
    animations()
    print('Луна готова')
