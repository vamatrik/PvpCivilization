#!/usr/bin/env python3
"""Генератор HD-спрайтов GUI «PvP Цивилизации».
Запуск: python3 tools/gui_sprites.py   (из корня проекта)
Все размеры — в GUI-пикселях; текстура пишется с плотностью x2, рисуется с суперсэмплингом x8."""
from PIL import Image, ImageDraw
import json, os, math
OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/pvpciv/textures/gui/sprites')
os.makedirs(OUT, exist_ok=True)
K = 8
def C(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2],16), int(h[2:4],16), int(h[4:6],16), a)
INK=C('#07060B'); BG=C('#15121F'); BG2=C('#0C0A13')
GOLD=C('#D9A441'); GOLD_L=C('#FFE08A'); GOLD_D=C('#7A5420'); GOLD_DD=C('#4A3416')
EMBER=C('#FF8C42')

class Cv:
    def __init__(s, w, h):
        s.w, s.h = w, h
        s.im = Image.new('RGBA', (int(w*K), int(h*K)), (0,0,0,0)); s.d = ImageDraw.Draw(s.im)
    def rect(s, x0, y0, x1, y1, fill): s.d.rectangle([x0*K, y0*K, x1*K-1, y1*K-1], fill=fill)
    def rrect(s, x0, y0, x1, y1, r, fill): s.d.rounded_rectangle([x0*K, y0*K, x1*K-1, y1*K-1], radius=r*K, fill=fill)
    def poly(s, pts, fill): s.d.polygon([(x*K, y*K) for x, y in pts], fill=fill)
    def ell(s, cx, cy, rx, ry, fill): s.d.ellipse([(cx-rx)*K, (cy-ry)*K, (cx+rx)*K, (cy+ry)*K], fill=fill)
    def ring(s, cx, cy, r, col, w): s.d.ellipse([(cx-r)*K, (cy-r)*K, (cx+r)*K, (cy+r)*K], outline=col, width=max(1,int(w*K)))
    def arc(s, cx, cy, r, a0, a1, col, w): s.d.arc([(cx-r)*K, (cy-r)*K, (cx+r)*K, (cy+r)*K], a0, a1, fill=col, width=max(1,int(w*K)))
    def line(s, pts, col, w): s.d.line([(x*K, y*K) for x, y in pts], fill=col, width=max(1,int(w*K)))
    def grad(s, x0, y0, x1, y1, c0, c1, mask=None):
        g = Image.new('RGBA', s.im.size, (0,0,0,0)); gd = ImageDraw.Draw(g)
        Y0, Y1 = int(y0*K), int(y1*K)
        for y in range(Y0, Y1):
            t = (y-Y0)/max(1, Y1-Y0-1)
            gd.line([(x0*K, y), (x1*K-1, y)], fill=tuple(int(c0[i]+(c1[i]-c0[i])*t) for i in range(4)))
        if mask is not None:
            a = Image.composite(g, Image.new('RGBA', s.im.size, (0,0,0,0)), mask.im.split()[3])
            s.im.alpha_composite(a)
        else: s.im.alpha_composite(g)
        s.d = ImageDraw.Draw(s.im)
    def over(s, o): s.im.alpha_composite(o.im); s.d = ImageDraw.Draw(s.im)
    def cut(s, mask):  # вырезать прозрачную дыру по маске
        r, g, b, a = s.im.split(); ma = mask.im.split()[3]
        from PIL import ImageChops
        s.im = Image.merge('RGBA', (r, g, b, ImageChops.subtract(a, ma))); s.d = ImageDraw.Draw(s.im)
    def save(s, name, border=None):
        im = s.im.resize((int(s.w*2), int(s.h*2)), Image.LANCZOS)
        im.save(os.path.join(OUT, name + '.png'))
        mp = os.path.join(OUT, name + '.png.mcmeta')
        if border:
            l, t, r, b = [v*2 for v in border]
            json.dump({"gui": {"scaling": {"type": "nine_slice", "width": int(s.w*2), "height": int(s.h*2),
                       "border": {"left": l, "top": t, "right": r, "bottom": b}}}}, open(mp, 'w'))
        elif os.path.exists(mp): os.remove(mp)

def diamond(c, cx, cy, r, fill): c.poly([(cx, cy-r), (cx+r, cy), (cx, cy+r), (cx-r, cy)], fill)
def gem_at(c, cx, cy, r, base):
    lt = tuple(min(255, int(v*1.5+40)) for v in base[:3]) + (255,)
    dk = tuple(int(v*0.55) for v in base[:3]) + (255,)
    dd = tuple(int(v*0.3) for v in base[:3]) + (255,)
    diamond(c, cx, cy, r+1.2, INK); diamond(c, cx, cy, r+0.6, GOLD_D); diamond(c, cx, cy, r, base)
    c.poly([(cx, cy-r), (cx, cy), (cx-r, cy)], lt); c.poly([(cx, cy), (cx+r, cy), (cx, cy+r)], dd)
    c.poly([(cx-r, cy), (cx, cy), (cx, cy+r)], dk)
    c.ell(cx-r*0.3, cy-r*0.35, r*0.16, r*0.16, (255,255,255,230))

# ---------------------------------------------------------------- панели
def lattice(c, w, h, col, dot):
    for n in range(-10, 20):
        c.line([(n*10, 0), (n*10+h, h)], col, 0.4); c.line([(n*10, 0), (n*10-h, h)], col, 0.4)
    for i in range(0, w+1, 10):
        for j in range(0, h+1, 10):
            c.ell(i, j, 0.7, 0.7, dot)
            c.ell(i+5, j+5, 0.45, 0.45, dot)

def frame_lines(c, w, h):
    c.rect(0, 0, w, h, INK)
    c.rect(1, 1, w-1, h-1, GOLD_D); c.rect(1, 1, w-1.4, h-1.4, GOLD); c.rect(1, 1, w-1, 1.45, GOLD_L); c.rect(1, 1, 1.45, h-1, GOLD_L)
    c.rect(2, 2, w-2, h-2, INK)

def panel():
    w = 48; c = Cv(w, w)
    frame_lines(c, w, w)
    c.rect(2.6, 2.6, w-2.6, w-2.6, BG)
    lat = Cv(w, w); lattice(lat, w, w, C('#211C31'), C('#2A2440'))
    m = Cv(w, w); m.rect(2.6, 2.6, w-2.6, w-2.6, (255,255,255,255))
    lat.im.putalpha(Image.composite(lat.im.split()[3], Image.new('L', lat.im.size, 0), m.im.split()[3])); c.over(lat)
    # тонкая внутренняя линия и кружево
    for a, b in ((4.5, w-4.5),):
        c.line([(a, a), (b, a), (b, b), (a, b), (a, a)], C('#6B5226'), 0.5)
    for k in range(4):
        x = 16.5 + 5*k
        c.arc(x, 4.6, 2.5, 0, 180, C('#8A6A2E'), 0.55); c.arc(x, w-4.6, 2.5, 180, 360, C('#6B5226'), 0.55)
        c.arc(4.6, x, 2.5, 270, 90, C('#8A6A2E'), 0.55); c.arc(w-4.6, x, 2.5, 90, 270, C('#6B5226'), 0.55)
    for k in range(5):
        x = 14 + 5*k
        for px, py in ((x, 5.0), (x, w-5.0), (5.0, x), (w-5.0, x)): c.ell(px, py, 0.75, 0.75, GOLD)
    # уголки: завитки и камень
    for sx in (0, 1):
        for sy in (0, 1):
            fx = (lambda v: v) if sx == 0 else (lambda v: w - v)
            fy = (lambda v: v) if sy == 0 else (lambda v: w - v)
            c.line([(fx(4.5), fy(13)), (fx(4.5), fy(4.5)), (fx(13), fy(4.5))], GOLD_D, 0.9)
            cx, cy = fx(7.5), fy(7.5)
            c.ring(fx(11.5), fy(6.2), 1.7, GOLD, 0.6); c.ring(fx(6.2), fy(11.5), 1.7, GOLD, 0.6)
            gem_at(c, cx, cy, 2.6, EMBER)
    c.save('panel', (14, 14, 14, 14))

def plate():
    w = 24; c = Cv(w, w)
    c.rrect(0, 0, w, w, 2.5, C('#07060B', 235))
    c.rrect(0.8, 0.8, w-0.8, w-0.8, 2, GOLD_D); c.rrect(0.8, 0.8, w-1.1, w-1.1, 2, GOLD)
    c.rrect(1.6, 1.6, w-1.6, w-1.6, 1.5, C('#100D18', 232))
    c.line([(3.2, 3.2), (w-3.2, 3.2), (w-3.2, w-3.2), (3.2, w-3.2), (3.2, 3.2)], C('#4A3A1C', 200), 0.4)
    for x, y in ((3.2, 3.2), (w-3.2, 3.2), (3.2, w-3.2), (w-3.2, w-3.2)): diamond(c, x, y, 1.3, GOLD_L)
    c.save('plate', (7, 7, 7, 7))

def inset():
    w = 16; c = Cv(w, w)
    c.rrect(0, 0, w, w, 2, C('#000000', 150)); c.rrect(0, 0.7, w, w, 2, C('#3A3350', 120))
    c.rrect(0.7, 0.7, w-0.7, w-0.7, 1.6, C('#0A0810', 215))
    c.save('inset', (4, 4, 4, 4))

def header():
    w, h = 48, 18; c = Cv(w, h)
    shape = lambda o: [(o*1.2, h/2), (7+o*0.4, 1+o), (w-7-o*0.4, 1+o), (w-o*1.2, h/2), (w-7-o*0.4, h-1-o), (7+o*0.4, h-1-o)]
    c.poly(shape(0), INK); c.poly(shape(0.9), GOLD_D)
    t = Cv(w, h); t.poly(shape(0.9), (255,)*4); c.grad(0, 0, w, h/2, GOLD_L, GOLD, t)
    c.poly(shape(1.9), INK)
    m = Cv(w, h); m.poly(shape(2.4), (255,)*4); c.grad(0, 2, w, h-2, C('#3B2E52'), C('#1C162B'), m)
    c.line([(10, 4.6), (w-10, 4.6)], C('#FFFFFF', 38), 0.5); c.line([(10, h-4.6), (w-10, h-4.6)], C('#000000', 90), 0.5)
    gem_at(c, 7.5, h/2, 1.9, EMBER); gem_at(c, w-7.5, h/2, 1.9, EMBER)
    c.save('header', (14, 0, 14, 0))

def button(name, top, bot, edge, edge_l, gloss=60):
    w, h = 32, 16; c = Cv(w, h)
    c.rrect(0, 0, w, h, 3.2, INK); c.rrect(0.8, 0.8, w-0.8, h-0.8, 2.6, edge)
    m = Cv(w, h); m.rrect(0.8, 0.8, w-0.8, h/2, 2.6, (255,)*4); c.grad(0, 0, w, h/2, edge_l, edge, m)
    m = Cv(w, h); m.rrect(1.8, 1.8, w-1.8, h-1.8, 1.8, (255,)*4); c.grad(0, 1.8, w, h-1.8, top, bot, m)
    g = Cv(w, h); g.rrect(2.6, 2.4, w-2.6, 6.5, 1.4, (255, 255, 255, gloss)); c.over(g)
    c.save(name, (6, 0, 6, 0))

def tab(name, active):
    w, h = 32, 14; c = Cv(w, h)
    edge = GOLD if active else C('#4A4063')
    c.rrect(0, 0, w, h+4, 3, INK); c.rrect(0.8, 0.8, w-0.8, h+4, 2.4, edge)
    m = Cv(w, h); m.rrect(1.7, 1.7, w-1.7, h+4, 1.8, (255,)*4)
    if active: c.grad(0, 1.7, w, h, C('#4A3A66'), C('#211A33'), m)
    else: c.grad(0, 1.7, w, h, C('#1D1830'), C('#0F0C18'), m)
    if active:
        c.rect(1.7, 1.7, w-1.7, 2.4, C('#FFE08A', 110)); c.rect(2, h-1, w-2, h, GOLD)
    else: c.rect(0, h-1, w, h, INK)
    c.save(name, (6, 0, 6, 0))

def slot():
    w = 18; c = Cv(w, w)
    c.rrect(0, 0, w, w, 2, C('#5A4E78')); c.rrect(0, 0, w-0.9, w-0.9, 2, C('#231D35'))
    c.rrect(0.9, 0.9, w-0.9, w-0.9, 1.5, C('#0B0911'))
    m = Cv(w, w); m.rrect(0.9, 0.9, w-0.9, w-0.9, 1.5, (255,)*4); c.grad(0, 0.9, w, 6, C('#000000', 150), C('#000000', 0), m)
    c.save('slot')
    l = Cv(w, w); l.rrect(0, 0, w, w, 2, C('#2A2438')); l.rrect(0.9, 0.9, w-0.9, w-0.9, 1.5, C('#13101C'))
    for i in range(-18, 18, 4): l.line([(i, 18), (i+18, 0)], C('#191525'), 0.8)
    l.rrect(0, 0, 0.9, w, 0, C('#2A2438')); l.rect(0, 0, w, 0.9, C('#2A2438')); l.rect(w-0.9, 0, w, w, C('#2A2438')); l.rect(0, w-0.9, w, w, C('#2A2438'))
    lock(l, 9, 9.5, 0.95, C('#6B5A3A'), C('#3D3322'))
    l.save('slot_locked')
    s = Cv(24, 24)
    for i, a in enumerate((40, 80, 140)): s.rrect(i, i, 24-i, 24-i, 4-i*0.5, C('#FFD070', a))
    s.rrect(2.2, 2.2, 21.8, 21.8, 2.6, GOLD_L); s.rrect(3.1, 3.1, 20.9, 20.9, 2, GOLD)
    h = Cv(24, 24); h.rrect(3.8, 3.8, 20.2, 20.2, 1.6, (255,)*4); s.cut(h)
    s.save('slot_selected')

def lock(c, cx, cy, k, body, dark):
    c.arc(cx, cy-1.6*k, 2.6*k, 180, 360, body, 1.1*k)
    c.rect(cx-2.6*k, cy-1.7*k, cx-1.5*k, cy-0.4*k, body); c.rect(cx+1.5*k, cy-1.7*k, cx+2.6*k, cy-0.4*k, body)
    c.rrect(cx-3.6*k, cy-0.6*k, cx+3.6*k, cy+4.4*k, 1*k, body)
    c.ell(cx, cy+1.5*k, 0.9*k, 0.9*k, dark); c.rect(cx-0.4*k, cy+1.5*k, cx+0.4*k, cy+3.2*k, dark)

def bar_frame():
    w, h = 32, 12; c = Cv(w, h)
    sh = lambda o: [(o*1.25, h/2), (5+o*0.45, 0.6+o), (w-5-o*0.45, 0.6+o), (w-o*1.25, h/2), (w-5-o*0.45, h-0.6-o), (5+o*0.45, h-0.6-o)]
    c.poly(sh(0), INK); c.poly(sh(0.8), GOLD_D)
    t = Cv(w, h); t.poly(sh(0.8), (255,)*4); c.grad(0, 0, w, h/2, GOLD_L, GOLD, t)
    c.poly(sh(1.7), INK)
    hole = Cv(w, h); hole.poly(sh(2.5), (255,)*4); c.cut(hole)
    c.save('bar_frame', (8, 0, 8, 0))
    t = Cv(8, 12)  # насечки
    t.rect(3.75, 2.5, 4.25, 4.2, C('#000000', 120)); t.rect(3.75, 7.8, 4.25, 9.5, C('#000000', 120))
    t.save('bar_ticks')

def gems():
    for n, col in (('red', '#E0283C'), ('amber', '#F0A020'), ('blue', '#3FA8E8'), ('violet', '#A050F0'), ('green', '#40C870')):
        c = Cv(10, 10); gem_at(c, 5, 5, 3.4, C(col)); c.save('gem_' + n)

def icons():
    c = Cv(10, 10)  # искра
    pts = [(5, 0.2), (6.3, 3.7), (9.8, 5), (6.3, 6.3), (5, 9.8), (3.7, 6.3), (0.2, 5), (3.7, 3.7)]
    c.poly([(5+(x-5)*1.12, 5+(y-5)*1.12) for x, y in pts], C('#3A2606')); c.poly(pts, GOLD)
    c.poly([(5, 0.2), (6.3, 3.7), (5, 5), (3.7, 3.7)], GOLD_L); c.poly([(0.2, 5), (3.7, 3.7), (5, 5), (3.7, 6.3)], C('#F4C860'))
    c.poly([(5, 9.8), (3.7, 6.3), (5, 5), (6.3, 6.3)], GOLD_D); c.ell(5, 5, 1, 1, C('#FFF6D0')); c.save('icon_spark')
    c = Cv(10, 10)  # сердце
    def heart(o, col):
        c.ell(3.1, 3.4, 2.5-o, 2.5-o, col); c.ell(6.9, 3.4, 2.5-o, 2.5-o, col); c.poly([(0.8+o*1.2, 4.4), (9.2-o*1.2, 4.4), (5, 9.4-o*1.4)], col)
    heart(0, C('#3A0810')); heart(0.7, C('#E8384C')); c.ell(3, 2.9, 0.9, 0.7, C('#FFB0B8')); c.save('icon_heart')
    c = Cv(10, 10)  # еда (окорок)
    c.line([(3.6, 6.4), (1.6, 8.4)], C('#3A2606'), 2.2); c.line([(3.6, 6.4), (1.6, 8.4)], C('#F0E8D8'), 1.1); c.ell(1.5, 8.5, 1.1, 1.1, C('#F0E8D8'))
    c.ell(5.9, 4.1, 3.9, 3.5, C('#3A1A06')); c.ell(5.9, 4.1, 3.2, 2.8, C('#D8842A')); c.ell(6.6, 3.3, 1.6, 1.2, C('#F0B050')); c.save('icon_food')
    c = Cv(10, 10)  # щит
    sh = lambda o: [(1+o, 1+o), (9-o, 1+o), (9-o, 5), (5, 9.6-o*1.3), (1+o, 5)]
    c.poly(sh(0), C('#10182A')); c.poly(sh(0.8), C('#8FB0E0')); c.poly([(5, 1.8), (8.2, 1.8), (8.2, 5), (5, 8.5)], C('#5878A8')); c.save('icon_shield')
    c = Cv(12, 12)  # череп
    c.ell(6, 5.2, 5, 4.8, INK); c.rrect(3.2, 7, 8.8, 11.4, 1, INK)
    c.ell(6, 5.2, 4.2, 4, C('#E8E2D0')); c.rrect(3.9, 7, 8.1, 10.7, 0.8, C('#E8E2D0'))
    c.ell(4.2, 5.4, 1.3, 1.5, C('#1A0A0A')); c.ell(7.8, 5.4, 1.3, 1.5, C('#1A0A0A')); c.poly([(6, 6.6), (6.7, 8), (5.3, 8)], C('#1A0A0A'))
    for x in (5, 6, 7): c.rect(x-0.15, 9, x+0.15, 10.7, C('#6A6458'))
    c.ell(4.2, 5.5, 0.5, 0.5, C('#FF5030')); c.ell(7.8, 5.5, 0.5, 0.5, C('#FF5030')); c.save('icon_skull')
    c = Cv(10, 10); lock(c, 5, 4.6, 1.0, GOLD, GOLD_DD); c.save('icon_lock')
    c = Cv(10, 10); c.poly([(1, 3.6), (5, 3.6), (5, 1), (9.4, 5), (5, 9), (5, 6.4), (1, 6.4)], C('#3A2606'))
    c.poly([(1.8, 4.3), (5.8, 4.3), (5.8, 2.6), (8.3, 5), (5.8, 7.4), (5.8, 5.7), (1.8, 5.7)], GOLD); c.save('icon_arrow')
    c = Cv(10, 10)  # часы завоза
    c.ell(5, 5, 4.6, 4.6, C('#3A2606')); c.ell(5, 5, 3.8, 3.8, GOLD); c.ell(5, 5, 2.9, 2.9, C('#1A1528')); c.line([(5, 5), (5, 2.9)], GOLD_L, 0.7); c.line([(5, 5), (6.8, 5.6)], GOLD_L, 0.7); c.save('icon_clock')
    c = Cv(40, 7)  # разделитель
    c.line([(0, 3.5), (15, 3.5)], GOLD_D, 0.6); c.line([(25, 3.5), (40, 3.5)], GOLD_D, 0.6)
    diamond(c, 20, 3.5, 3, GOLD_D); diamond(c, 20, 3.5, 2.2, GOLD); diamond(c, 20, 3.5, 1, GOLD_L)
    diamond(c, 15.5, 3.5, 1.1, GOLD); diamond(c, 24.5, 3.5, 1.1, GOLD); c.save('divider')

def hints():
    col = C('#3D3556')
    def mk(name, fn):
        c = Cv(16, 16); fn(c); c.save('hint_' + name)
    mk('weapon', lambda c: (c.line([(4.5, 11.5), (13, 3)], col, 1.8), c.line([(3.2, 8.2), (7.8, 12.8)], col, 1.5), c.line([(2.5, 13.5), (4.6, 11.4)], col, 1.6)))
    def star(c):
        pts = []
        for i in range(10):
            r = 6.5 if i % 2 == 0 else 2.8; a = -math.pi/2 + i*math.pi/5
            pts.append((8+r*math.cos(a), 8.4+r*math.sin(a)))
        c.poly(pts, col)
    mk('star', star)
    mk('helmet', lambda c: (c.d.pieslice([2.5*K, 2.5*K, 13.5*K, 14.5*K], 180, 360, fill=col), c.rect(2.5, 8.2, 13.5, 10.5, col), c.rect(2.5, 10.5, 5.5, 13, col), c.rect(10.5, 10.5, 13.5, 13, col)))
    mk('chest', lambda c: (c.poly([(2, 3), (5.5, 2), (8, 4), (10.5, 2), (14, 3), (14, 7), (12, 7.5), (12, 14), (4, 14), (4, 7.5), (2, 7)], col),))
    mk('legs', lambda c: (c.poly([(3.5, 2), (12.5, 2), (13, 14), (9, 14), (8, 6.5), (7, 14), (3, 14)], col),))
    mk('boots', lambda c: (c.poly([(2, 5), (6, 5), (6, 10), (7.3, 11.5), (7.3, 13), (2, 13)], col), c.poly([(9, 5), (13, 5), (13, 10), (14.3, 11.5), (14.3, 13), (9, 13)], col)))
    mk('shield', lambda c: (c.poly([(3, 2.5), (13, 2.5), (13, 8), (8, 14), (3, 8)], col),))
    def ring(c): c.ring(8, 9.3, 4.6, col, 1.7); diamond(c, 8, 3.8, 2.4, col)
    mk('artifact', ring)

def anvil():
    c = Cv(56, 34)
    def body(o, col):
        c.poly([(1+o, 9.5), (13, 7+o), (53-o, 7+o), (53-o, 14-o), (40, 15.2-o*0.6), (37, 20), (37.5, 24), (46-o, 26+o*0.4), (47-o, 32-o), (9+o, 32-o), (10+o, 26+o*0.4), (18.5, 24), (19, 20), (16, 15.5-o*0.6), (8, 13.6-o*0.4)], col)
    body(0, INK); body(1, C('#3C4254'))
    m = Cv(56, 34); m.poly([(2, 9.6), (13, 8), (52, 8), (52, 11), (2.2, 11)], (255,)*4); c.grad(0, 8, 56, 11, C('#B8C4DC'), C('#6C7890'), m)
    m = Cv(56, 34); m.poly([(8, 11), (52, 11), (52, 13), (40, 14.2), (16, 14.4)], (255,)*4); c.grad(0, 11, 56, 14.4, C('#4C5468'), C('#2A2E3C'), m)
    c.poly([(19.5, 20), (36.5, 20), (36.8, 23.4), (19.2, 23.4)], C('#2C3040'))
    m = Cv(56, 34); m.poly([(11, 26.5), (45, 26.5), (46, 31), (10, 31)], (255,)*4); c.grad(0, 26.5, 56, 31, C('#565E74'), C('#2E3242'), m)
    c.line([(13.5, 8.4), (51.5, 8.4)], C('#FFFFFF', 150), 0.5); c.line([(11.5, 27), (44.5, 27)], C('#FFFFFF', 60), 0.5)
    c.save('anvil')

def glow():
    n = 64; im = Image.new('RGBA', (n, n)); px = im.load()
    for x in range(n):
        for y in range(n):
            d = math.hypot(x-31.5, y-31.5)/32; a = max(0, 1-d); px[x, y] = (255, 255, 255, int(255*a*a))
    im.save(os.path.join(OUT, 'glow.png'))
    n = 64; im = Image.new('RGBA', (n, 16)); px = im.load()  # овальная тень/подиум
    for x in range(n):
        for y in range(16):
            d = math.hypot((x-31.5)/32, (y-7.5)/8); a = max(0, 1-d); px[x, y] = (255, 255, 255, int(255*a))
    im.save(os.path.join(OUT, 'podium.png'))

def crack():
    c = Cv(40, 12)
    pts = [(0, 6), (5, 4), (9, 7.5), (14, 3.5), (19, 8), (24, 4.5), (29, 8), (34, 5), (40, 6.5)]
    c.line(pts, C('#000000', 200), 1.1); c.line(pts, C('#FFD0A0', 160), 0.35)
    c.line([(9, 7.5), (10.5, 11)], C('#000000', 180), 0.7); c.line([(24, 4.5), (25.5, 0.8)], C('#000000', 180), 0.7); c.line([(14, 3.5), (12.8, 0.5)], C('#000000', 160), 0.6)
    c.save('crack')

panel(); plate(); inset(); header()
button('button', C('#3E3350'), C('#231C33'), GOLD_D, GOLD)
button('button_hover', C('#57477A'), C('#30264A'), GOLD, GOLD_L, 90)
button('button_ember', C('#C4601E'), C('#74300E'), GOLD_D, GOLD, 80)
button('button_ember_hover', C('#E87A2A'), C('#963E12'), GOLD, GOLD_L, 110)
button('button_off', C('#24212C'), C('#18161E'), C('#3A3644'), C('#4A4658'), 14)
tab('tab', False); tab('tab_active', True)
slot(); bar_frame(); gems(); icons(); hints(); anvil(); glow(); crack()
print('sprites:', len([f for f in os.listdir(OUT) if f.endswith('.png')]))
