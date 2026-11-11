#!/usr/bin/env python3
"""Иконки 16×16 зелий Элеоноры (у каждого своя форма бутылки и цвет).
Запуск: python3 tools/potion_icons.py  → textures/item/potion_*.png"""
import os
from PIL import Image

TEX = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'pvpciv', 'textures', 'item')

def hx(s, a=255):
    s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)

# o — контур, g — стекло, w — блик, c/C — пробка, 1/2/3 — жидкость (светлая/средняя/тёмная), e — эмблема
SHAPES = {
    # круглая колба с крестом
    'potion_heal': [
        "................",
        "......cCCc......",
        "......cCCc......",
        ".......oo.......",
        "......ogwo......",
        "......ogwo......",
        ".....oggggo.....",
        "...oogg11ggoo...",
        "..og1111111wgo..",
        "..o11112111w1o..",
        ".o11111e11111wo.",
        ".o2111eee11112o.",
        ".o22211e122222o.",
        "..o22223322223o.",
        "...oo3333333oo..",
        ".....ooooooo....",
    ],
    # бутылка-сердце
    'potion_regen': [
        "................",
        "......cCCc......",
        ".......oo.......",
        "......ogwo......",
        "..ooo.ogwo.ooo..",
        ".ogw1oo11oo11go.",
        "o1w1111111111w1o",
        "o11111e11e11111o",
        "o2111eeeeee1112o",
        ".o2111eeee1112o.",
        "..o2211ee1122o..",
        "...o22211222o...",
        "....o223322o....",
        ".....o2332o.....",
        "......o33o......",
        ".......oo.......",
    ],
    # высокая тонкая пробирка с молнией
    'potion_swift': [
        "......cCCc......",
        "......cCCc......",
        "......oggo......",
        "......owgo......",
        ".....ow11o......",
        ".....ow11o......",
        ".....o1e1o......",
        ".....o1ee1o.....",
        ".....o11ee1o....",
        ".....o2ee11o....",
        ".....o2e112o....",
        ".....o22e12o....",
        ".....o2222o.....",
        ".....o3322o.....",
        "......o33o......",
        ".......oo.......",
    ],
    # квадратная бутыль с кулаком-ромбом
    'potion_might': [
        "................",
        "......cCCc......",
        "......cCCc......",
        "......oggo......",
        "....oooggooo....",
        "...ogwggggggo...",
        "...ow1111111o...",
        "...ow111e111o...",
        "...o111eee11o...",
        "...o11eeeee1o...",
        "...o211eee12o...",
        "...o2211e122o...",
        "...o2222222o....",
        "...o3322233o....",
        "....ooooooo.....",
        "................",
    ],
    # широкая банка с «каменными» крапинками
    'potion_stone': [
        "................",
        "................",
        "....cCCCCCCc....",
        "....cCCCCCCc....",
        "....oggggggo....",
        "...oggwgggggo...",
        "..ow11111111go..",
        "..ow1e111e111o..",
        "..o111e11111eo..",
        "..o1111e1e111o..",
        "..o2e1111111e2o.",
        "..o22e122e1222o.",
        "..o2222e22222o..",
        "..o33322233e3o..",
        "...oooooooooo...",
        "................",
    ],
}

PAL = {
    'potion_heal':  ('e83a3a', 'b81e28', '7a0e18', 'ffffff'),
    'potion_regen': ('ff8ab8', 'e0508a', 'a02860', 'fff0f6'),
    'potion_swift': ('7fe8ff', '30b8e0', '1a7aa8', 'fff6a0'),
    'potion_might': ('ffa040', 'e0601a', '9a3010', '5a1a08'),
    'potion_stone': ('a8b4c8', '7a8aa0', '4a5670', '3a404c'),
}

BASE = {'o': hx('1a1420'), 'g': hx('c8e0f0', 150), 'w': hx('ffffff', 220), 'c': hx('8a5a30'), 'C': hx('b07a44')}

def build(name, rows):
    l1, l2, l3, e = PAL[name]
    pal = dict(BASE); pal.update({'1': hx(l1), '2': hx(l2), '3': hx(l3), 'e': hx(e)})
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row[:16]):
            if ch in pal: im.putpixel((x, y), pal[ch])
    im.save(os.path.join(TEX, name + '.png'))

if __name__ == '__main__':
    for n, r in SHAPES.items(): build(n, r)
    print('ok', len(SHAPES))
