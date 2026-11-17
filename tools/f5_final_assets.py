#!/usr/bin/env python3
"""Ассеты 1.0 (финал этажа 5): тёмная материя, тёмная звезда, свиток забвения, адский след, контракт, трофеи."""
import os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.argv = [sys.argv[0]]
import importlib.util
spec = importlib.util.spec_from_file_location('f5', os.path.join(HERE, 'f5_assets.py'))
src = open(os.path.join(HERE, 'f5_assets.py')).read().split("art('cmp_lazuratron'")[0]
ns = {'__file__': os.path.join(HERE, 'f5_assets.py')}
exec(src, ns)
art, recolor, hx = ns['art'], ns['recolor'], ns['hx']

art('dark_matter', [
    "................",
    "......pppp......",
    "....ppkkkkpp....",
    "...pkkvvvvkkp...",
    "..pkkvwvvvvkkp..",
    "..pkvvvkkkvvkp..",
    ".pkkvvkkkkkvkkp.",
    ".pkvvkkmmkkvvkp.",
    ".pkvvkkmmkkvvkp.",
    ".pkkvkkkkkvvkkp.",
    "..pkvvkkkvvvkp..",
    "..pkkvvvvvvkkp..",
    "...pkkvvvvkkp...",
    "....ppkkkkpp....",
    "......pppp......",
    "................"], {'p': hx('6a2aa0'), 'k': hx('0a0614'), 'v': hx('3a1660'), 'w': hx('e0c0ff'), 'm': hx('c040ff')})
art('dark_star', [
    "................",
    ".......p........",
    ".......kp.......",
    "......pkkp......",
    "......kvvk......",
    ".ppkkkvwvvkkkpp.",
    "..pkkvvvvvvkkp..",
    "...pkvvmmvvkp...",
    "....pkvmmvkp....",
    "....kvvvvvvk....",
    "...pkvkppkvkp...",
    "...kvkp..pkvk...",
    "..pkkp....pkkp..",
    "..kp........pk..",
    "................",
    "................"], {'p': hx('9a6aff'), 'k': hx('140a24'), 'v': hx('3a2a5a'), 'w': hx('ffffff'), 'm': hx('c060ff')})
art('forget_scroll', [
    "................",
    "...oooooooooo...",
    "..oyyyyyyyyyyo..",
    "..oyoooooooyyo..",
    "...ollllllllo...",
    "...olbblbbllo...",
    "...ollllllllo...",
    "...olbbblbblo...",
    "...ollllllllo...",
    "...olblbbbllo...",
    "...ollllmmllo...",
    "...olllmmmmlo...",
    "..oyoooooooyyo..",
    "..oyyyyyyyyyyo..",
    "...oooooooooo...",
    "................"], {'o': hx('3a2a5a'), 'y': hx('c9b6ee'), 'l': hx('f1eef6'), 'b': hx('8a76b0'), 'm': hx('e8c45a')})
art('hell_trail', [
    "................",
    "........r.......",
    ".......rO.......",
    "......rOyr......",
    ".....rOyyOr.....",
    "....rOyywyOr....",
    "....rOywwyOr....",
    "...rOyywwyyOr...",
    "...rOyywwyyOr...",
    "...rrOyyyyOrr...",
    "....rrOOOOrr....",
    "...kkrrrrrrkk...",
    "..kkkkkkkkkkkk..",
    "...kkkkkkkkkk...",
    "................",
    "................"], {'r': hx('8a1010'), 'O': hx('ff5a10'), 'y': hx('ffb030'), 'w': hx('fff0b0'), 'k': hx('2a1414')})
art('contract', [
    "................",
    "...ooooooooo....",
    "...owwwwwwwoo...",
    "...owbbbbwwwoo..",
    "...owwwwwwwwwo..",
    "...owbbbbbbbwo..",
    "...owwwwwwwwwo..",
    "...owbbbbbbwwo..",
    "...owwwwwwwwwo..",
    "...owbbbbbbbwo..",
    "...owwwwwwwwwo..",
    "...owwwwwrrrwo..",
    "...owwwwrwrwwo..",
    "...owwwwwwwwwo..",
    "...ooooooooooo..",
    "................"], {'o': hx('5a5040'), 'w': hx('f4f0e0'), 'b': hx('8a8070'), 'r': hx('c8242c')})
recolor('naza_trophy', 'diplomat_trophy', 0.55, 0.7, 1.0)
recolor('naza_trophy', 'worm_trophy', 0.83, 0.8, 1.0)
print('1.0: иконки готовы')
