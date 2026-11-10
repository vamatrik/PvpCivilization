#!/usr/bin/env python3
"""Генератор объёмных моделей оружия «PvP Цивилизации».
Запуск: python3 tools/weapon_models.py   (из корня проекта)

Оружие описывается в WEAPONS одной строкой через шаблоны sword / saber / dagger / cleaver / axe / hammer.
Для каждого создаются: models/item/<id>.json, шесть вариантов свечения <id>_g1.._g6 и анимированная
текстура-палитра textures/item/w3d/<id>.png. Вариант выбирается предикатом pvpciv:stars (заточка/16).

Система координат: 16 единиц = 1 блок, оружие стоит вертикально, рукоять внизу по центру (x=8, z=8),
середина хвата на высоте ~3. Плоскость клинка — XY, толщина — по Z."""
import json, os, math
from PIL import Image
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/pvpciv')
FRAMES = 8
# стадии свечения: (мин. заточка, цвет A, цвет B, альфа, раздувание оболочки)
GLOW = [(1, '#7FD4FF', '#D0F4FF', 40, 0.25), (4, '#3D7BFF', '#8FC0FF', 70, 0.3), (6, '#C060FF', '#7A30E0', 95, 0.35),
        (9, '#FFD030', '#FFF6B0', 115, 0.4), (10, '#FF3030', '#FF9A30', 140, 0.45), (11, '#06040C', '#9040F0', 215, 0.5)]

def rgb(h): h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))
def mul(c, k): return tuple(max(0, min(255, int(v*k))) for v in c)

class Model:
    def __init__(s, name): s.name = name; s.boxes = []
    def box(s, x0, y0, x1, y1, depth, color, blade=False, z=8.0):
        s.boxes.append((min(x0, x1), y0, z-depth/2, max(x0, x1), y1, z+depth/2, rgb(color), blade)); return s
    def c(s, cx, y0, y1, w, depth, color, blade=False):  # коробка по центру cx
        return s.box(cx-w/2, y0, cx+w/2, y1, depth, color, blade)

# ---------------------------------------------------------------- детали
def grip(m, wood='#5A3A22', wrap='#3A2416', pommel='#C9A24A', length=5.0):
    m.c(8, 0.0, 0.9, 1.7, 1.7, pommel)
    m.c(8, 0.9, length, 1.1, 1.1, wood)
    for y in (1.5, 2.7, 3.9):
        if y+0.5 < length: m.c(8, y, y+0.5, 1.3, 1.3, wrap)
    return length

def guard(m, y, w=5.0, color='#8A8F9B', tips='#C9A24A'):
    m.c(8, y, y+0.9, w, 1.6, color)
    m.c(8-w/2+0.4, y-0.3, y+1.2, 0.9, 1.8, tips); m.c(8+w/2-0.4, y-0.3, y+1.2, 0.9, 1.8, tips)
    return y+0.9

def blade(m, y0, y1, w, core, edge, cx=8.0, tip=3.0):
    body = y1-tip
    m.c(cx, y0, body, w-1.0, 1.0, core, True)                       # дол
    m.box(cx-w/2, y0, cx-w/2+0.6, body, 0.5, edge, True); m.box(cx+w/2-0.6, y0, cx+w/2, body, 0.5, edge, True)
    n = 3
    for i in range(n):                                              # остриё
        ww = w*(n-i)/(n+1); m.c(cx, body+tip*i/n, body+tip*(i+1)/n, ww, 0.8 if i < 2 else 0.5, edge if i else core, True)

# ---------------------------------------------------------------- шаблоны
def sword(name, length=15, width=2.6, core='#9AA2AD', edge='#E6ECF2', guard_w=5.0, guard_c='#8A8F9B', wood='#5A3A22', pommel='#C9A24A'):
    m = Model(name); y = grip(m, wood=wood, pommel=pommel); y = guard(m, y, guard_w, guard_c, pommel)
    blade(m, y, y+length, width, core, edge); return m

def dagger(name, length=7.5, width=2.0, core='#D8D2BC', edge='#FFFFFF', guard_c='#8A7F66', wood='#4A3A2A'):
    m = Model(name); y = grip(m, wood=wood, pommel=guard_c, length=4.2); y = guard(m, y, 3.2, guard_c, guard_c)
    blade(m, y, y+length, width, core, edge, tip=3.5); return m

def saber(name, length=17, width=1.8, curve=3.2, core='#8E96A2', edge='#F0F4F8', guard_c='#3B3F48', wood='#2A2A35', pommel='#C9A24A'):
    m = Model(name); y = grip(m, wood=wood, pommel=pommel)
    m.c(8, y, y+0.8, 3.6, 1.6, guard_c); m.box(9.3, y-2.6, 10.1, y+0.4, 0.9, guard_c)   # гарда с дужкой
    y += 0.8; n = 9
    for i in range(n):
        t0, t1 = i/n, (i+1)/n; off = curve*(t0**2.2); w = width*(1-0.55*t0**2)
        m.box(8-width/2+off, y+length*t0, 8-width/2+off+w*0.55, y+length*t1, 0.9, core, True)       # обух
        m.box(8-width/2+off+w*0.55, y+length*t0, 8-width/2+off+w, y+length*t1, 0.45, edge, True)    # лезвие
    return m

def cleaver(name, length=11, width=4.6, core='#8A6B55', edge='#C08A5A', rust='#6A4030', wood='#3A2A1E', guard_c='#4A3A30'):
    m = Model(name); y = grip(m, wood=wood, pommel=guard_c); m.c(8, y, y+0.8, 2.6, 1.6, guard_c); y += 0.8
    x0 = 7.0                                                         # обух слева, лезвие справа
    m.box(x0, y, x0+1.0, y+length, 1.0, rust, True)
    m.box(x0+1.0, y+0.8, x0+width-0.7, y+length, 0.8, core, True)
    m.box(x0+width-0.7, y+1.6, x0+width, y+length-0.8, 0.4, edge, True)
    m.box(x0+1.6, y+length-2.6, x0+2.6, y+length-1.6, 1.0, '#1A1410')  # отверстие
    m.box(x0+1.0, y+length, x0+width-1.6, y+length+0.7, 0.7, core, True)
    for i, (dx, dy) in enumerate(((1.6, 2.2), (2.8, 5.2), (1.9, 7.4))): m.box(x0+dx, y+dy, x0+dx+0.9, y+dy+0.7, 0.9, rust)
    return m

def axe(name, shaft=19, head='#3A3A3E', edge='#FF7A2E', wood='#4A2E1E', band='#2A2A2E', double=False):
    m = Model(name); m.c(8, 0, 0.8, 1.6, 1.6, band); m.c(8, 0.8, shaft, 1.1, 1.1, wood)
    for y in (2.0, 3.6): m.c(8, y, y+0.5, 1.3, 1.3, band)
    top = shaft-1.0; m.c(8, top-4.2, top+0.6, 1.7, 1.7, band)
    def half(sgn):
        x = 8+sgn*0.8
        m.box(x, top-3.4, x+sgn*1.6, top-0.2, 1.2, head, True)
        m.box(x+sgn*1.6, top-4.4, x+sgn*3.2, top+0.8, 0.9, head, True)
        m.box(x+sgn*3.2, top-5.2, x+sgn*4.0, top+1.6, 0.6, head, True)
        m.box(x+sgn*4.0, top-5.6, x+sgn*4.6, top+2.0, 0.35, edge, True)
    half(1)
    if double: half(-1)
    else: m.box(6.2, top-2.6, 7.2, top-1.0, 1.0, head)
    return m

def hammer(name, shaft=18, head='#5E6B52', face='#9FB08A', dark='#2F3A28', wood='#5A3A22', band='#C9A24A', accent='#8B2020', head_w=9.0, head_h=5.0):
    m = Model(name); m.c(8, 0, 0.8, 1.7, 1.7, band); m.c(8, 0.8, shaft, 1.2, 1.2, wood)
    for y in (2.0, 3.6, 5.2): m.c(8, y, y+0.5, 1.4, 1.4, dark)
    y0 = shaft-1.5
    m.c(8, y0, y0+head_h, head_w-2.0, 3.6, head, True)
    m.c(8-head_w/2+0.5, y0-0.4, y0+head_h+0.4, 1.0, 4.2, face, True); m.c(8+head_w/2-0.5, y0-0.4, y0+head_h+0.4, 1.0, 4.2, face, True)
    m.c(8, y0+head_h/2-0.4, y0+head_h/2+0.4, head_w-2.0, 3.9, dark)
    m.c(8, y0+head_h, y0+head_h+1.2, 1.6, 1.6, band)
    m.c(8+head_w/2-0.5, y0+0.6, y0+1.6, 1.05, 4.3, accent)
    return m

# ---------------------------------------------------------------- список оружия (добавляй сюда)
WEAPONS = [
    sword('novice_sword', length=12, width=2.4, core='#A87C48', edge='#D9AE73', guard_w=4.2, guard_c='#6B4A2A', wood='#5A3A22', pommel='#8A6538'),
    cleaver('rusty_cleaver'),
    saber('ash_saber'),
    dagger('bone_dagger'),
    axe('charred_axe'),
    hammer('brute_cleaver'),
    # тир 1+ — Инеевый Кряж
    sword('rime_blade', length=15, width=2.8, core='#7FC8EC', edge='#E8FAFF', guard_w=5.6, guard_c='#C8D6E0', wood='#2A3A4A', pommel='#9ADFFF'),
    saber('frost_rapier', length=18, width=1.4, curve=1.2, core='#9ADFFF', edge='#F4FDFF', guard_c='#3A5A7A', wood='#22303E', pommel='#E8FAFF'),
    dagger('icicle_dagger', length=8.5, width=1.8, core='#B8ECFF', edge='#FFFFFF', guard_c='#5A7A9A', wood='#2A3A4A'),
    axe('glacier_axe', head='#4A6A8A', edge='#9ADFFF', wood='#3A2E2A', band='#C8D6E0', double=True),
]

# ---------------------------------------------------------------- вывод
def build(m):
    pal = {}                       # ключ -> номер ячейки
    def cell(key):
        if key not in pal: pal[key] = len(pal)
        return pal[key]
    def uv(i): x, y = i % 16, i // 16; return [x+0.25, y+0.25, x+0.75, y+0.75]
    SH = {'north': 1.0, 'south': 1.0, 'east': 0.8, 'west': 0.8, 'up': 1.18, 'down': 0.62}
    def elem(b, glow=None, inflate=0, phase=0):
        x0, y0, z0, x1, y1, z1, col, _ = b
        e = {"from": [round(x0-inflate, 3), round(y0-inflate*0.6, 3), round(z0-inflate, 3)], "to": [round(x1+inflate, 3), round(y1+inflate*0.6, 3), round(z1+inflate, 3)],
             "rotation": {"origin": [8, 8, 8], "axis": "z", "angle": -45}, "faces": {}}  # вся модель лежит по диагонали, как ванильный меч
        for f, k in SH.items():
            idx = cell(('g', glow, phase)) if glow is not None else cell(('c', mul(col, k)))
            e["faces"][f] = {"uv": uv(idx), "texture": "#0"}
        if glow is not None:
            e["shade"] = False; e["neoforge_data"] = {"block_light": 15, "sky_light": 15}
        return e
    base = [elem(b) for b in m.boxes]
    ys = [b[1] for b in m.boxes] + [b[4] for b in m.boxes]; xs = [b[0] for b in m.boxes] + [b[3] for b in m.boxes]
    h = max(ys)-min(ys); cy = (max(ys)+min(ys))/2-8; cx = (max(xs)+min(xs))/2-8
    s = min(1.0, 21.5/max(h, 1)); k = 0.7071*s
    disp = {  # стандартные «handheld»-значения: модель уже повёрнута на 45°
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85]*3},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85]*3},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68]*3},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68]*3},
        "gui": {"translation": [round(-k*(cy+cx), 2), round(-k*(cy-cx), 2), 0], "scale": [round(s, 3)]*3},
        "ground": {"translation": [0, 2, 0], "scale": [0.5]*3},
        "fixed": {"rotation": [0, 180, 0], "translation": [round(k*(cy+cx), 2), round(-k*(cy-cx), 2), 0], "scale": [round(s, 3)]*3},
    }
    tex = {"0": f"pvpciv:item/w3d/{m.name}", "particle": f"pvpciv:item/w3d/{m.name}"}
    md = os.path.join(ROOT, 'models/item')
    variants = []
    for gi, (stars, a, b_, alpha, infl) in enumerate(GLOW):
        els = list(base)
        bl = [b for b in m.boxes if b[7]]
        for i, b in enumerate(bl): els.append(elem(b, glow=gi, inflate=infl, phase=i % 3))
        json.dump({"textures": tex, "gui_light": "front", "elements": els, "display": disp}, open(os.path.join(md, f"{m.name}_g{gi+1}.json"), 'w'), separators=(',', ':'))
        variants.append({"predicate": {"pvpciv:stars": stars/16}, "model": f"pvpciv:item/{m.name}_g{gi+1}"})
    json.dump({"textures": tex, "gui_light": "front", "elements": base, "display": disp, "overrides": variants}, open(os.path.join(md, f"{m.name}.json"), 'w'), separators=(',', ':'))
    # анимированная палитра
    td = os.path.join(ROOT, 'textures/item/w3d'); os.makedirs(td, exist_ok=True)
    im = Image.new('RGBA', (16, 16*FRAMES), (0, 0, 0, 0)); px = im.load()
    for key, i in pal.items():
        x, y = i % 16, i // 16
        for f in range(FRAMES):
            if key[0] == 'c': col = key[1] + (255,)
            else:
                _, a, b_, alpha, _ = GLOW[key[1]]; ca, cb = rgb(a), rgb(b_)
                t = 0.5+0.5*math.sin(2*math.pi*(f/FRAMES + key[2]/3))
                col = tuple(int(ca[j]+(cb[j]-ca[j])*t) for j in range(3)) + (int(alpha*(0.7+0.3*t)),)
            px[x, y+16*f] = col
    im.save(os.path.join(td, m.name + '.png'))
    json.dump({"animation": {"frametime": 3, "interpolate": True}}, open(os.path.join(td, m.name + '.png.mcmeta'), 'w'))
    return len(base), len(pal)

if __name__ == '__main__':
    for w in WEAPONS:
        n, p = build(w); print(f"{w.name}: {n} элементов, {p} цветов")
