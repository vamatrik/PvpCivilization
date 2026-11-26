"""1.3: HD-спрайты древа классов (суперсэмплинг ×4 → сглаживание)."""
import math
from PIL import Image, ImageDraw, ImageFilter
OUT = '/home/claude/civ/src/main/resources/assets/pvpciv/textures/gui/sprites/tree/'
SS = 4

def canvas(n): return Image.new('RGBA', (n * SS, n * SS), (0, 0, 0, 0))
def save(im, n, name): im.resize((n, n), Image.LANCZOS).save(OUT + name + '.png')

def radial(n, inner, outer, r0, r1):
    """Диск: радиальный градиент inner→outer."""
    im = canvas(n); px = im.load(); c = n * SS / 2
    for y in range(n * SS):
        for x in range(n * SS):
            d = math.hypot(x - c + .5, y - c + .5) / SS
            if d <= r1:
                k = max(0, min(1, (d - r0) / max(1e-6, r1 - r0)))
                px[x, y] = tuple(int(inner[i] + (outer[i] - inner[i]) * k) for i in range(4))
    return im

def ring(n, r0, r1, light=True):
    """Металлическое кольцо (белое — тонируется): свет сверху-слева, тень снизу-справа."""
    im = canvas(n); px = im.load(); c = n * SS / 2
    for y in range(n * SS):
        for x in range(n * SS):
            dx, dy = x - c + .5, y - c + .5
            d = math.hypot(dx, dy) / SS
            if r0 <= d <= r1:
                a = math.atan2(dy, dx)
                l = 0.62 + 0.38 * math.cos(a + math.pi * 0.75)        # свет сверху-слева
                bev = 1 - abs((d - r0) / (r1 - r0) - 0.5) * 0.9           # округлый профиль
                v = int(255 * max(0.25, min(1, l * bev + 0.15)))
                edge = min(d - r0, r1 - d) * SS
                al = int(255 * min(1, edge / 1.5))
                px[x, y] = (v, v, v, al)
    return im

def poly(n, pts, fill, width=0, outline=None):
    im = canvas(n); d = ImageDraw.Draw(im)
    P = [(x * SS, y * SS) for x, y in pts]
    d.polygon(P, fill=fill, outline=outline, width=width * SS if width else 0)
    return im

def regular(cx, cy, r, k, rot=0):
    return [(cx + r * math.cos(rot + 2 * math.pi * i / k), cy + r * math.sin(rot + 2 * math.pi * i / k)) for i in range(k)]

# ---------------- узлы
# стат: круглый медальон
base = radial(64, (30, 24, 42, 255), (12, 9, 18, 255), 0, 25)
save(base, 64, 'node_base')
save(ring(64, 24, 30), 64, 'node_ring')
# класс: щит-герб
def shield(n, s):
    c = n / 2
    return [(c, c - s), (c + s * .86, c - s * .62), (c + s * .8, c + s * .2), (c, c + s), (c - s * .8, c + s * .2), (c - s * .86, c - s * .62)]
im = poly(80, shield(80, 34), (255, 255, 255, 255)); im2 = poly(80, shield(80, 27), (0, 0, 0, 255))
rim = Image.composite(Image.new('RGBA', im.size, (0, 0, 0, 0)), im, im2.split()[3])
# металл: вертикальный градиент
px = rim.load()
for y in range(rim.size[1]):
    for x in range(rim.size[0]):
        if px[x, y][3]:
            v = int(255 * (1 - 0.45 * y / rim.size[1]))
            px[x, y] = (v, v, v, px[x, y][3])
save(rim, 80, 'class_ring')
inner = poly(80, shield(80, 28), (26, 20, 38, 255))
save(inner, 80, 'class_base')
# навык: восьмиугольник-камень
oct_o = regular(40, 40, 36, 8, math.pi / 8); oct_i = regular(40, 40, 29, 8, math.pi / 8)
o = poly(80, oct_o, (255, 255, 255, 255)); i_ = poly(80, oct_i, (0, 0, 0, 255))
r = Image.composite(Image.new('RGBA', o.size, (0, 0, 0, 0)), o, i_.split()[3]); px = r.load()
for y in range(r.size[1]):
    for x in range(r.size[0]):
        if px[x, y][3]:
            v = int(255 * (0.55 + 0.45 * (1 - (x + y) / (2 * r.size[0]))))
            px[x, y] = (v, v, v, px[x, y][3])
save(r, 80, 'skill_ring')
save(poly(80, regular(40, 40, 30, 8, math.pi / 8), (22, 16, 34, 255)), 80, 'skill_base')
# центр: восьмилучевая звезда-роза
star = []
for k in range(16):
    rr = 46 if k % 2 == 0 else 30
    a = -math.pi / 2 + k * math.pi / 8
    star.append((48 + rr * math.cos(a), 48 + rr * math.sin(a)))
s_ = poly(96, star, (255, 255, 255, 255)); px = s_.load()
for y in range(s_.size[1]):
    for x in range(s_.size[0]):
        if px[x, y][3]:
            d = math.hypot(x - s_.size[0] / 2, y - s_.size[1] / 2) / s_.size[0]
            v = int(255 * (0.55 + 0.9 * d))
            px[x, y] = (min(255, v), min(255, v), min(255, v), px[x, y][3])
save(s_, 96, 'center_star')
save(radial(96, (60, 30, 110, 255), (16, 10, 28, 255), 0, 25), 96, 'center_base')
save(ring(96, 24, 30), 96, 'center_ring')
# мягкое свечение
g = canvas(64); px = g.load(); c = 32 * SS
for y in range(64 * SS):
    for x in range(64 * SS):
        d = math.hypot(x - c, y - c) / (32 * SS)
        if d < 1: px[x, y] = (255, 255, 255, int(255 * (1 - d) ** 2.2))
save(g, 64, 'halo')

# ---------------- иконки (белые, тонируются), 48×48
def icon(name, draw_fn):
    im = canvas(48); d = ImageDraw.Draw(im)
    draw_fn(d, SS)
    sh = im.filter(ImageFilter.GaussianBlur(2 * SS)); sp = sh.load()
    out = Image.new('RGBA', im.size, (0, 0, 0, 0)); op = out.load(); ip = im.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            a = ip[x, y][3]
            s = sp[x, y][3]
            if a: op[x, y] = (255, 255, 255, a)
            elif s: op[x, y] = (0, 0, 0, int(s * 0.55))
    save(out, 48, 'i_' + name)

def L(d, s, pts, w): d.line([(x * s, y * s) for x, y in pts], fill=(255, 255, 255, 255), width=int(w * s), joint='curve')
def P(d, s, pts): d.polygon([(x * s, y * s) for x, y in pts], fill=(255, 255, 255, 255))
def C(d, s, cx, cy, r, w=None):
    if w: d.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], outline=(255, 255, 255, 255), width=int(w * s))
    else: d.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], fill=(255, 255, 255, 255))

def sword(d, s, x0=24, rot=0):
    P(d, s, [(22, 6), (26, 6), (26, 30), (24, 34), (22, 30)]); P(d, s, [(14, 30), (34, 30), (34, 33), (14, 33)]); P(d, s, [(22.5, 33), (25.5, 33), (25.5, 41), (22.5, 41)]); C(d, s, 24, 43, 2.5)
icon('sword', sword)
def portal(d, s):
    for k in range(3):
        r = 17 - k * 5
        d.arc([(24 - r) * s, (24 - r) * s, (24 + r) * s, (24 + r) * s], start=200 + k * 40, end=200 + k * 40 + 250, fill=(255, 255, 255, 255), width=int(3 * s))
    C(d, s, 24, 24, 3)
icon('portal', portal)
def dagger2(d, s):
    for sx in (-1, 1):
        cx = 24 + sx * 7
        P(d, s, [(cx - 2, 9), (cx + 2, 9), (cx + 2, 28), (cx, 32), (cx - 2, 28)][::1]) if False else P(d, s, [(cx, 7), (cx + 3, 12), (cx + 2.5, 28), (cx - 2.5, 28), (cx - 3, 12)])
        P(d, s, [(cx - 6, 28), (cx + 6, 28), (cx + 6, 31), (cx - 6, 31)]); P(d, s, [(cx - 1.5, 31), (cx + 1.5, 31), (cx + 1.5, 39), (cx - 1.5, 39)])
icon('daggers', dagger2)
def hammer(d, s):
    P(d, s, [(10, 9), (38, 9), (38, 21), (10, 21)]); P(d, s, [(22, 21), (26, 21), (26, 42), (22, 42)]); P(d, s, [(14, 11), (34, 11), (34, 12.5), (14, 12.5)])
icon('hammer', hammer)
def heart(d, s):
    C(d, s, 17, 18, 8); C(d, s, 31, 18, 8); P(d, s, [(9.5, 21), (38.5, 21), (24, 38)])
icon('hp', heart)
def shield_i(d, s): P(d, s, [(24, 7), (38, 12), (37, 26), (24, 41), (11, 26), (10, 12)])
icon('def', shield_i)
def regen(d, s): P(d, s, [(20, 9), (28, 9), (28, 20), (39, 20), (39, 28), (28, 28), (28, 39), (20, 39), (20, 28), (9, 28), (9, 20), (20, 20)])
icon('regen', regen)
def dmg(d, s):
    P(d, s, [(41, 7), (39, 14), (19, 34), (14, 29), (34, 9)])          # клинок
    P(d, s, [(9, 28), (12, 25), (23, 36), (20, 39)])                  # гарда
    P(d, s, [(13, 33), (15, 35), (9, 41), (7, 39)]); C(d, s, 7, 41, 2.5)
icon('dmg', dmg)
def drop(d, s): P(d, s, [(24, 7), (34, 24), (14, 24)]); C(d, s, 24, 29, 10)
icon('lifesteal', drop)
def aspd(d, s):
    for ox in (0, 12): P(d, s, [(8 + ox, 10), (22 + ox, 24), (8 + ox, 38), (13 + ox, 38), (27 + ox, 24), (13 + ox, 10)])
icon('aspd', aspd)
def mspd(d, s):
    for y, w in ((14, 22), (24, 30), (34, 18)): L(d, s, [(40 - w, y), (40, y)], 3.5)
    C(d, s, 10, 24, 3)
icon('mspd', mspd)
def crit(d, s):
    C(d, s, 24, 24, 14, 3); C(d, s, 24, 24, 4)
    for a, b in (((24, 4), (24, 12)), ((24, 36), (24, 44)), ((4, 24), (12, 24)), ((36, 24), (44, 24))): L(d, s, [a, b], 3)
icon('crit', crit)
def burst(d, s):
    pts = []
    for k in range(16):
        r = 20 if k % 2 == 0 else 9
        a = -math.pi / 2 + k * math.pi / 8
        pts.append((24 + r * math.cos(a), 24 + r * math.sin(a)))
    P(d, s, pts)
icon('critdmg', burst)
def dodge(d, s):
    d.arc([8 * s, 8 * s, 40 * s, 40 * s], start=150, end=390, fill=(255, 255, 255, 255), width=int(4 * s))
    P(d, s, [(36, 30), (44, 22), (34, 20)])
icon('dodge', dodge)
def flame(d, s): P(d, s, [(24, 5), (31, 16), (36, 26), (34, 36), (27, 42), (21, 42), (14, 36), (12, 26), (17, 19), (20, 25), (22, 15)])
icon('rage', flame)
def moon(d, s):
    C(d, s, 24, 24, 16)
    d.ellipse([(24 - 9) * s, (24 - 20) * s, (24 + 21) * s, (24 + 10) * s], fill=(0, 0, 0, 0))
icon('retreat', moon)
def tower(d, s):
    P(d, s, [(12, 16), (36, 16), (34, 42), (14, 42)])
    for x in (12, 20.5, 29): P(d, s, [(x, 8), (x + 7, 8), (x + 7, 16), (x, 16)])
icon('bastion', tower)
def lock(d, s):
    d.rounded_rectangle([12 * s, 21 * s, 36 * s, 41 * s], radius=3 * s, fill=(255, 255, 255, 255))
    d.arc([16 * s, 8 * s, 32 * s, 28 * s], start=180, end=360, fill=(255, 255, 255, 255), width=int(4 * s))
icon('lock', lock)
def q(d, s):
    d.arc([14 * s, 7 * s, 34 * s, 27 * s], start=200, end=420, fill=(255, 255, 255, 255), width=int(4.5 * s))
    L(d, s, [(26, 26), (24, 30), (24, 33)], 4.5); C(d, s, 24, 40, 3)
icon('q', q)
def mage(d, s):
    P(d, s, [(24, 4), (27, 21), (44, 24), (27, 27), (24, 44), (21, 27), (4, 24), (21, 21)])
icon('mage', mage)
def gun(d, s):
    P(d, s, [(6, 16), (40, 16), (40, 23), (20, 23), (17, 38), (9, 38), (12, 23), (6, 23)]); P(d, s, [(40, 17.5), (44, 17.5), (44, 21.5), (40, 21.5)])
icon('archer', gun)
def spellblade(d, s):
    sword(d, s)
    for k in range(5):
        a = k * 2 * math.pi / 5
        C(d, s, 24 + 13 * math.cos(a), 20 + 13 * math.sin(a), 1.6)
icon('spellblade', spellblade)
print('ok')
