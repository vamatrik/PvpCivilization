#!/usr/bin/env python3
"""Проверка: помещается ли текст в форму интерфейса. Ширины считаются по настоящему шрифту мода
(assets/pvpciv/font/ui.ttf, size 11 — как в assets/minecraft/font/default.json), переносы — как в игре (по словам).

  python3 tools/ui_fit.py "Текст"                 ширина в GUI-пикселях
  python3 tools/ui_fit.py "Текст" 120 2           как перенесётся в 120 px и влезет ли в 2 строки
  python3 tools/ui_fit.py --scan                  пройтись по исходникам: Skin.fit/para с текстом-литералом,
                                                  реплики диалогов, названия предметов в лавке, меню еды, точки такси
  python3 tools/ui_fit.py --log путь/latest.log   что игра пометила «[UI-FIT]» (обрезанный текст) во время теста

Код выхода --scan: 1, если что-то не влезло (можно вешать на сборку)."""
import json, os, re, sys, math
from fontTools.ttLib import TTFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
RES = os.path.join(ROOT, 'src', 'main', 'resources', 'assets')
SRC = os.path.join(ROOT, 'src', 'main', 'java')

_font = TTFont(os.path.join(RES, 'pvpciv', 'font', 'ui.ttf'))
_cmap = _font.getBestCmap()
_hmtx = _font['hmtx']
_upem = _font['head'].unitsPerEm
SIZE, OVERSAMPLE = 11.0, 4.0
SKIP = set('★☆✦❤☠')        # эти значки берутся из ванильного шрифта
BITMAP = {'★': 8, '☆': 8, '✦': 8, '❤': 8, '☠': 8}

def char_w(ch):
    if ch == ' ': return 4.0                       # minecraft:include/space
    if ch in BITMAP: return BITMAP[ch]
    g = _cmap.get(ord(ch))
    if g is None or ch in SKIP: return 6.0         # ванильный запасной шрифт (≈)
    adv = _hmtx[g][0]
    # FreeType рендерит в size×oversample пикселях и округляет шаг до пикселя, игра делит обратно
    return round(adv * SIZE * OVERSAMPLE / _upem) / OVERSAMPLE

def width(s):
    s = re.sub(r'§.', '', s)
    return math.ceil(sum(char_w(c) for c in s))

def wrap(s, w):
    """Как StringSplitter.splitLines: по словам, слишком длинное слово режется."""
    out = []
    for para in s.split('\n'):
        line = ''
        for word in para.split(' '):
            cand = word if not line else line + ' ' + word
            if width(cand) <= w: line = cand; continue
            if line: out.append(line)
            line = word
            while width(line) > w:                  # слово длиннее строки
                k = len(line)
                while k > 1 and width(line[:k]) > w: k -= 1
                out.append(line[:k]); line = line[k:]
        out.append(line)
    return out

def report(text, w=None, lines=None):
    tw = width(text)
    if w is None:
        print(f'{tw} px  «{text}»'); return True
    ls = wrap(text, w)
    ok = lines is None or len(ls) <= lines
    print(f'{"OK " if ok else "НЕ ВЛЕЗАЕТ"}  {tw} px в {w} px → {len(ls)} стр.' + (f' (можно {lines})' if lines else ''))
    for l in ls: print(f'   {width(l):>4} | {l}')
    return ok

# ------------------------------------------------------------------ сканер исходников
LIT = r'"((?:[^"\\]|\\.)*)"'
WORST_DIALOG_W = 480 - 48 - 36       # GUI-шкала 4 на 1920×1080: окно 480 px, рамки диалога
DIALOG_LINES = 7

def scan():
    bad = 0
    lang = json.load(open(os.path.join(RES, 'pvpciv', 'lang', 'ru_ru.json'), encoding='utf-8'))
    for dp, _, fs in os.walk(SRC):
        for fn in fs:
            if not fn.endswith('.java'): continue
            path = os.path.join(dp, fn); rel = os.path.relpath(path, ROOT)
            src = open(path, encoding='utf-8').read()
            for m in re.finditer(r'Skin\.fit\(\s*\w+\s*,\s*' + LIT + r'\s*,\s*(\d+)\s*\)', src):
                t, w = m.group(1), int(m.group(2))
                if width(t) > w: bad += 1; print(f'[fit]   {rel}: «{t}» {width(t)} > {w} px')
            for m in re.finditer(r'Skin\.para(?:Centered)?\(\s*\w+\s*,\s*\w+\s*,\s*' + LIT + r'\s*,[^,]+,[^,]+,\s*(\d+)\s*,\s*\d+\s*,\s*(\d+)', src):
                t, w, n = m.group(1), int(m.group(2)), int(m.group(3))
                if len(wrap(t, w)) > n: bad += 1; print(f'[para]  {rel}: «{t[:50]}…» {len(wrap(t, w))} стр. > {n} при {w} px')
            # реплики: .node("id", "Кто", COLOR, "текст" + "текст"...)
            for m in re.finditer(r'\.node\(\s*"[^"]*"\s*,\s*[^,]+,\s*[^,]+,\s*((?:' + LIT + r'\s*\+?\s*)+)', src):
                t = ''.join(re.findall(LIT, m.group(1)))
                n = len(wrap(t.replace('\\n', '\n').replace('{name}', 'Игрок_Длинный'), WORST_DIALOG_W))
                if n > DIALOG_LINES: bad += 1; print(f'[диалог] {rel}: {n} стр. > {DIALOG_LINES} при {WORST_DIALOG_W} px: «{t[:60]}…»')
            # кнопки ответов: opt("текст", ...) — кнопка растёт, но не шире половины окна
            for m in re.finditer(r'\bopt\(\s*' + LIT, src):
                t = m.group(1)
                if width(t) + 36 > WORST_DIALOG_W * 0.75: bad += 1; print(f'[ответ] {rel}: «{t}» {width(t)} px — слишком длинная кнопка')
    # названия предметов в лавке (132 px; яйца призыва и трофеи не продаются)
    for k, v in lang.items():
        if k.startswith('item.pvpciv.') and not k.endswith('_spawn_egg') and 'trophy' not in k and width(v) > 132: print(f'[лавка]  {k}: «{v}» {width(v)} > 132 px (будет «…»)')
    # меню «Шпиль.Еды» (80 px) и точки такси (84 px × 2 строки)
    food = open(os.path.join(SRC, 'com', 'pvpciv', 'game', 'FoodApp.java'), encoding='utf-8').read()
    for m in re.finditer(r'new Dish\("\w+",\s*' + LIT, food):
        if width(m.group(1)) > 80: bad += 1; print(f'[еда]    «{m.group(1)}» {width(m.group(1))} > 80 px')
    taxi = open(os.path.join(SRC, 'com', 'pvpciv', 'game', 'Taxi.java'), encoding='utf-8').read()
    for m in re.finditer(r'(?:f1|pol|new Pt)\(\s*' + LIT, taxi):
        if len(wrap(m.group(1), 84)) > 2: bad += 1; print(f'[такси]  «{m.group(1)}» не влезает в 2 строки по 84 px')
    print('Всё влезает.' if bad == 0 else f'Не влезает: {bad}')
    return bad

def log(path):
    n = 0
    for line in open(path, encoding='utf-8', errors='replace'):
        if '[UI-FIT]' in line and 'калибровка' not in line: n += 1; print(line.split('[UI-FIT]', 1)[1].strip())
    print('Переполнений нет.' if n == 0 else f'Всего: {n}')

if __name__ == '__main__':
    a = sys.argv[1:]
    if not a: print(__doc__); sys.exit(0)
    if a[0] == '--scan': sys.exit(1 if scan() else 0)
    if a[0] == '--log': log(a[1]); sys.exit(0)
    ok = report(a[0], int(a[1]) if len(a) > 1 else None, int(a[2]) if len(a) > 2 else None)
    sys.exit(0 if ok else 1)
