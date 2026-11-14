#!/usr/bin/env python3
"""HD-модели NPC этажа 4 (0.11): Чарователь Аурелий и Стелла (космическая эльфийка). Анимации — общие civ_npc."""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ.setdefault('FACE_STYLE', 'ref')
import gen_assets as G
G.ROOT = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
hexc, flat, mul, strands, stripes = G.hexc, G.flat, G.mul, G.strands, G.stripes


def aurelius():
    m = G.Model('aurelius', 1024)
    skin = hexc('ecd0b8'); hair = hexc('e8e4f0')
    coat, violet, cyan, dark = hexc('2a1e4a'), hexc('6a3ac0'), hexc('40d0ff'), hexc('16121e')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('40c0f0'), sex='m', expr='calm', bangs='swept', glasses=False,
                             brow=hexc('c8c8d8'), lash=hexc('3a3a50'), blush_c=(220, 140, 120, 40)), long_hair=False)

    def extra(m):
        hp = strands(mul(hair, 1.05), mul(hair, 0.75), seed=41)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.4, -4.9], [10.2, 2.2, 10.2], hp, 4), ([-5.2, 21.0, 2.4], [10.4, 9, 2.6], hp, 4),
                                             ([-5.4, 23.5, -3.6], [1.2, 6.5, 7.6], hp, 4), ([4.2, 23.5, -3.6], [1.2, 6.5, 7.6], hp, 4)])
        # очки-гогглы на лбу
        m.bone('goggles', 'head', [0, 29, 0], [([-5.2, 28.0, -5.2], [10.4, 1.2, 10.4], flat(dark), 6),
                                               ([-3.8, 28.2, -5.8], [3.0, 2.0, 1.0], flat(cyan), 8), ([0.8, 28.2, -5.8], [3.0, 2.0, 1.0], flat(cyan), 8)])
        # длинный фиолетовый халат-мантия со светящимися швами
        m.bone('coat', 'body', [0, 21, 0], [([-4.4, 4.5, -2.5], [8.8, 16.6, 5.0], stripes(coat, mul(coat, 1.25), 6, seed=4), 3),
                                            ([-0.3, 4.5, -2.65], [0.6, 16.6, 0.2], flat(cyan), 8)])
        m.bone('collar', 'body', [0, 21, 0], [([-4.0, 20.0, -2.8], [8.0, 2.6, 5.6], flat(violet), 5)])
        m.bone('sleeve_r', 'right_arm', [-4, 20.5, 0], [([-5.8, 11.5, -1.7], [3.4, 9.5, 3.4], flat(coat, noise=0.04), 3), ([-5.9, 11.0, -1.8], [3.6, 1.0, 3.6], flat(cyan), 8)])
        m.bone('sleeve_l', 'left_arm', [4, 20.5, 0], [([2.4, 11.5, -1.7], [3.4, 9.5, 3.4], flat(coat, noise=0.04), 3), ([2.3, 11.0, -1.8], [3.6, 1.0, 3.6], flat(cyan), 8)])
        # кристалл лазуратрона на поясе
        m.bone('crystal', 'body', [2, 12, -2.6], [([1.6, 10.5, -3.2], [1.4, 2.6, 1.0], flat(hexc('2a6aff')), 8)])

    G.humanoid(m, skin, face, flat(coat, noise=0.03), flat(coat), flat(dark), flat(hexc('2a2a34')), fem=False, extra=extra)
    m.save('aurelius')


def stella():
    m = G.Model('stella', 1024)
    skin = hexc('f2e6f4'); hair = hexc('d8e8ff')
    suit, trim, cyan, violet, dark = hexc('f0f2f8'), hexc('a8b0c8'), hexc('40d0ff'), hexc('c060ff'), hexc('2a2a3a')
    face = G.anime_face(dict(skin=skin, hair=hair, eye=hexc('9a50ff'), sex='f', expr='sly', bangs='swept',
                             brow=hexc('b8c8e8'), lash=hexc('3a3060'), blush_c=(240, 140, 180, 90)), long_hair=True)

    def extra(m):
        hp = strands(mul(hair, 1.08), mul(hair, 0.8), seed=88)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.3, -4.9], [10.2, 2.9, 10.2], hp, 4), ([-5.3, 14.5, 2.6], [10.6, 16, 2.4], hp, 4),
                                             ([-5.6, 21.5, -3.0], [1.3, 9, 6.2], hp, 4), ([4.3, 21.5, -3.0], [1.3, 9, 6.2], hp, 4)])
        # длинные эльфийские уши
        m.bone('ear_r', 'head', [-4.5, 26, -0.5], [([-8.6, 25.6, -1.0], [4.2, 1.6, 1.0], flat(skin, noise=0.01), 4, ([-4.5, 26, -0.5], [0, 0, -18]))])
        m.bone('ear_l', 'head', [4.5, 26, -0.5], [([4.4, 25.6, -1.0], [4.2, 1.6, 1.0], flat(skin, noise=0.01), 4, ([4.5, 26, -0.5], [0, 0, 18]))])
        # ободок-гарнитура с антенной
        m.bone('headset', 'head', [0, 30, 0], [([-5.3, 29.6, -1.0], [10.6, 0.8, 2.0], flat(trim), 6), ([-5.6, 25.0, -1.4], [0.8, 2.4, 2.4], flat(cyan), 8),
                                                ([4.4, 29.5, -0.3], [0.5, 4.0, 0.5], flat(trim), 8), ([4.25, 33.4, -0.45], [0.8, 0.8, 0.8], flat(violet), 8)])
        # скафандр: воротник-кольцо, нагрудная панель, ранец
        m.bone('ring', 'body', [0, 21, 0], [([-3.8, 19.6, -2.6], [7.6, 1.6, 5.2], flat(trim), 5)])
        m.bone('panel', 'body', [0, 16, -2.3], [([-1.6, 14.5, -2.6], [3.2, 3.0, 0.4], flat(dark), 7), ([-1.0, 15.2, -2.75], [0.8, 0.8, 0.2], flat(cyan), 8),
                                                ([0.3, 15.2, -2.75], [0.8, 0.8, 0.2], flat(violet), 8)])
        m.bone('pack', 'body', [0, 16, 2.2], [([-3.0, 11.5, 2.0], [6.0, 8.0, 2.4], flat(trim, noise=0.03), 4), ([-2.4, 12.0, 4.4], [1.2, 6.5, 0.4], flat(cyan), 8),
                                              ([1.2, 12.0, 4.4], [1.2, 6.5, 0.4], flat(cyan), 8)])
        m.bone('belt', 'body', [0, 13, 0], [([-3.3, 12.6, -2.2], [6.6, 1.2, 4.4], flat(dark), 5)])
        m.bone('cuff_r', 'right_arm', [-4, 20.5, 0], [([-5.2, 12.0, -1.3], [2.6, 2.4, 2.6], flat(trim), 4), ([-5.3, 12.8, -1.4], [2.8, 0.5, 2.8], flat(violet), 8)])
        m.bone('cuff_l', 'left_arm', [4, 20.5, 0], [([2.6, 12.0, -1.3], [2.6, 2.4, 2.6], flat(trim), 4), ([2.5, 12.8, -1.4], [2.8, 0.5, 2.8], flat(cyan), 8)])
        m.bone('boot_r', 'right_leg', [-1.5, 12, 0], [([-3.0, 0, -1.8], [3.0, 5.0, 3.4], flat(trim), 4)])
        m.bone('boot_l', 'left_leg', [1.5, 12, 0], [([0.0, 0, -1.8], [3.0, 5.0, 3.4], flat(trim), 4)])

    G.humanoid(m, skin, face, flat(suit, noise=0.03), flat(suit), flat(suit), flat(trim), fem=True,
               top=G.neckline(flat(suit), flat(skin), cyan, 'v', front=0.35, scallop=False), bust=flat(suit), extra=extra)
    m.save('stella')


if __name__ == '__main__':
    aurelius(); stella()
    print('NPC этажа 4: модели готовы')
