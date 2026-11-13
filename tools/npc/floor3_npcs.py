#!/usr/bin/env python3
"""HD-модели NPC этажа 3 (0.9.7, бета: «сделай Юки красивую аниме-модель как у Луны»): Юки, Хранитель Сёдо, повар Харуки.
Анимации — общие civ_npc (idle/walk/slash/…)."""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G
G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, strands, stripes = G.hexc, G.flat, G.mul, G.strands, G.stripes


def yuki():
    m = G.Model('yuki', 1024)
    skin = hexc('f6e4dc'); hair = hexc('1c1a26')
    navy, deep, red, gold, steel = hexc('22203a'), hexc('15131f'), hexc('c8203a'), hexc('d8b048'), hexc('d8dce4')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('e0304a'), sex='f', expr='stern', bangs='hime',
                             brow=hexc('2a2030'), lash=hexc('120e18'), blush_c=(240, 120, 140, 90)), long_hair=True)

    def extra(m):
        hp = strands(mul(hair, 1.6), mul(hair, 0.8), seed=77)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.3, -4.9], [10.2, 2.9, 10.2], hp, 4),
                                             ([-5.3, 22.5, 2.6], [10.6, 8, 2.4], hp, 4),
                                             ([-5.6, 23.5, -3.0], [1.3, 7, 6.2], hp, 4), ([4.3, 23.5, -3.0], [1.3, 7, 6.2], hp, 4)])
        # высокий хвост с алой лентой
        m.bone('pony', 'head', [0, 31, 4.5], [([-1.5, 30.5, 4.0], [3.0, 2.0, 2.0], flat(red), 6),
                                              ([-1.8, 18.0, 4.6], [3.6, 13, 3.0], hp, 4, ([0, 31, 5.5], [18, 0, 0]))])
        m.bone('pony_tip', 'pony', [0, 18, 5.5], [([-1.4, 10.5, 5.0], [2.8, 7.5, 2.4], hp, 4, ([0, 18, 6], [10, 0, 0]))])
        m.bone('lock_r', 'head', [-4.6, 27, -3.9], [([-5.7, 20.5, -4.8], [1.6, 7.5, 2.0], hp, 4)])
        m.bone('lock_l', 'head', [4.6, 27, -3.9], [([4.1, 20.5, -4.8], [1.6, 7.5, 2.0], hp, 4)])
        # алый шарф: воротник и два хвоста
        m.bone('scarf', 'body', [0, 21, 0], [([-3.6, 19.4, -2.4], [7.2, 2.2, 4.8], flat(red, noise=0.06), 4)])
        m.bone('scarf_tail', 'scarf', [1.5, 20, 2.4], [([0.6, 13.0, 2.2], [1.8, 7, 0.6], flat(red, noise=0.06), 4, ([1.5, 20, 2.4], [14, 0, -8])),
                                                       ([2.6, 14.0, 2.3], [1.6, 6, 0.6], flat(mul(red, 0.85), noise=0.06), 4, ([3.4, 20, 2.4], [18, 0, -14]))])
        # катана за спиной по диагонали
        m.bone('katana', 'body', [0, 16, 2.6], [([-0.45, 9.5, 2.5], [0.9, 13, 0.8], flat(hexc('101014')), 8, ([0, 16, 2.9], [0, 0, 40])),
                                               ([-1.3, 22.4, 2.3], [2.6, 0.5, 1.2], flat(gold), 8, ([0, 16, 2.9], [0, 0, 40])),
                                               ([-0.4, 22.9, 2.55], [0.8, 3.6, 0.7], stripes(red, deep, 2, seed=5), 8, ([0, 16, 2.9], [0, 0, 40]))])
        # пояс-оби с узлом и короткая хакама-юбка
        m.bone('obi', 'body', [0, 14, 0], [([-3.4, 13.4, -2.2], [6.8, 1.8, 4.4], flat(red, noise=0.05), 5),
                                           ([1.2, 12.6, -2.6], [1.6, 1.6, 0.6], flat(red, noise=0.05), 6)])
        m.bone('skirt', 'body', [0, 13, 0], [([-4.0, 8.4, -2.6], [8.0, 4.8, 5.2], stripes(deep, mul(deep, 1.4), 3, seed=9), 3)])
        # наручи и таби
        m.bone('cuff_r', 'right_arm', [-4, 20.5, 0], [([-5.3, 12.4, -1.3], [2.6, 3.2, 2.6], flat(deep), 4)])
        m.bone('cuff_l', 'left_arm', [4, 20.5, 0], [([2.7, 12.4, -1.3], [2.6, 3.2, 2.6], flat(deep), 4)])
        m.bone('shoe_r', 'right_leg', [-1.5, 12, 0], [([-3.0, 0, -2.0], [3.0, 1.2, 3.6], flat(hexc('2a2a34')), 4)])
        m.bone('shoe_l', 'left_leg', [1.5, 12, 0], [([0.0, 0, -2.0], [3.0, 1.2, 3.6], flat(hexc('2a2a34')), 4)])

    sk = flat(skin, noise=0.01)
    legs = G.split_v(sk, flat(deep), 0.45, trim=red)   # высокие гетры
    G.humanoid(m, skin, face, flat(navy, noise=0.04), G.split_v(flat(navy), sk, 0.85), legs, flat(hexc('2a2a34')), fem=True,
               top=G.neckline(flat(navy), sk, red, 'v', front=0.55, scallop=False), bust=flat(navy), extra=extra)
    m.save('yuki')


def shodo():
    m = G.Model('shodo', 1024)
    skin = hexc('e8cdb4'); hair = hexc('c8c8d0')
    haori, under, gold = hexc('1e2a4a'), hexc('e8e4d8'), hexc('c8a86a')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('5a4a30'), sex='m', expr='calm', bangs='short', glasses=True,
                             brow=hexc('d8d8e0'), lash=hexc('3a3a44'), blush_c=(200, 120, 100, 40)), long_hair=False)

    def extra(m):
        hp = strands(mul(hair, 1.1), mul(hair, 0.8), seed=31)
        m.bone('hair', 'head', [0, 31, 4], [([-5.0, 29.6, -4.8], [10.0, 1.6, 10.0], hp, 4), ([-5.1, 23.0, 2.4], [10.2, 7.5, 2.6], hp, 4), ([-5.3, 24.0, -3.6], [1.2, 6, 7.6], hp, 4), ([4.1, 24.0, -3.6], [1.2, 6, 7.6], hp, 4)])
        m.bone('beard', 'head', [0, 24, -4.2], [([-2.4, 20.6, -4.6], [4.8, 3.6, 1.2], hp, 6), ([-1.2, 18.4, -4.5], [2.4, 2.4, 1.0], hp, 6)])
        m.bone('eboshi', 'head', [0, 31, 0], [([-3.6, 31.0, -3.2], [7.2, 3.6, 6.4], flat(hexc('15151a')), 6),
                                              ([-2.6, 34.4, -1.6], [5.2, 2.6, 4.4], flat(hexc('15151a')), 6),
                                              ([-3.7, 31.0, -3.3], [7.4, 0.5, 6.6], flat(under), 6)])
        # хаори с гербом-кругом на спине, длинные рукава
        m.bone('haori', 'body', [0, 21, 0], [([-4.4, 7.5, -2.5], [8.8, 13.6, 5.0], flat(haori, noise=0.04), 3)])
        m.bone('crest', 'haori', [0, 18, 2.5], [([-1.2, 16.0, 2.45], [2.4, 2.4, 0.2], flat(under), 8)])
        m.bone('sleeve_r', 'right_arm', [-4, 20.5, 0], [([-6.0, 12.5, -1.8], [3.6, 7.5, 3.6], flat(haori, noise=0.04), 3)])
        m.bone('sleeve_l', 'left_arm', [4, 20.5, 0], [([2.4, 12.5, -1.8], [3.6, 7.5, 3.6], flat(haori, noise=0.04), 3)])
        m.bone('scroll', 'body', [0, 16, 2.6], [([-4.0, 15.0, 2.8], [8.0, 1.6, 1.6], flat(hexc('efe6d0')), 8, ([0, 16, 3.6], [0, 0, -20])),
                                               ([-4.4, 14.8, 2.6], [0.6, 2.0, 2.0], flat(hexc('6a2a20')), 8, ([0, 16, 3.6], [0, 0, -20]))])
        m.bone('sash', 'body', [0, 14, 0], [([-3.4, 13.0, -2.2], [6.8, 1.4, 4.4], flat(gold, noise=0.06), 5)])

    G.humanoid(m, skin, face, flat(under, noise=0.03), flat(under), flat(hexc('3a3a48')), flat(hexc('d8d0c0')), fem=False, extra=extra)
    m.save('shodo')


def haruki():
    m = G.Model('haruki', 1024)
    skin = hexc('ecc8a8'); hair = hexc('3a2418')
    white, blue, red = hexc('f4f2ee'), hexc('2a3a6a'), hexc('d02a30')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('6a4020'), sex='m', expr='grin', bangs='messy',
                             brow=hexc('2a1a10'), lash=hexc('1a1008'), blush_c=(230, 130, 110, 70)), long_hair=False)

    def extra(m):
        hp = strands(mul(hair, 1.3), mul(hair, 0.8), seed=13)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.4, -4.9], [10.2, 2.4, 10.2], hp, 4), ([-5.2, 23.0, 2.4], [10.4, 7, 2.6], hp, 4), ([-5.4, 24.5, -3.6], [1.2, 5.5, 7.6], hp, 4), ([4.2, 24.5, -3.6], [1.2, 5.5, 7.6], hp, 4)])
        m.bone('hachimaki', 'head', [0, 29, 0], [([-5.2, 28.0, -5.2], [10.4, 1.4, 10.4], flat(white), 6),
                                                 ([-0.8, 28.2, -5.5], [1.6, 1.0, 0.4], flat(red), 8),
                                                 ([1.5, 26.0, 5.0], [1.0, 2.6, 0.4], flat(white), 6), ([2.6, 25.6, 5.0], [1.0, 2.8, 0.4], flat(white), 6)])
        m.bone('apron', 'body', [0, 15, 0], [([-3.6, 4.0, -2.4], [7.2, 11.0, 0.5], flat(blue, noise=0.04), 4)])
        m.bone('towel', 'body', [0, 21, 0], [([-3.8, 19.0, -2.4], [1.2, 1.2, 4.8], flat(white), 6)])
        m.bone('ladle', 'right_arm', [-4, 12, 0], [([-4.4, 6.0, -0.4], [0.8, 7.0, 0.8], flat(hexc('8a6a3a')), 8),
                                                   ([-5.2, 5.0, -2.2], [2.4, 1.4, 2.4], flat(hexc('c8ccd4')), 8)])

    G.humanoid(m, skin, face, flat(white, noise=0.02), flat(white), flat(hexc('2a2a34')), flat(hexc('3a2a20')), fem=False, extra=extra)
    m.save('haruki')


if __name__ == '__main__':
    yuki(); shodo(); haruki()
    print('NPC этажа 3: модели готовы')
