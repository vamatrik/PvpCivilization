"""1.3: золотые чеканные рамки узлов древа (рельеф через карту высот → освещение), 128 px."""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
OUT = '/home/claude/civ/src/main/resources/assets/pvpciv/textures/gui/sprites/tree/'
N, SS = 128, 4
S = N * SS

def mask():
    im = Image.new('L', (S, S), 0); return im, ImageDraw.Draw(im)

def P(pts): return [(x * SS * N / 128, y * SS * N / 128) for x, y in pts]
def ell(d, cx, cy, r, fill): d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], fill=fill)
def regular(cx, cy, r, k, rot=0): return [(cx + r * math.cos(rot + 2 * math.pi * i / k), cy + r * math.sin(rot + 2 * math.pi * i / k)) for i in range(k)]

GOLD = np.array([[52, 30, 12], [120, 74, 26], [196, 142, 52], [236, 192, 96], [255, 240, 186]], float)
def palette(t):
    t = np.clip(t, 0, 1) * (len(GOLD) - 1)
    i = np.minimum(np.floor(t).astype(int), len(GOLD) - 2)
    f = (t - i)[..., None]
    return GOLD[i] * (1 - f) + GOLD[i + 1] * f

def emboss(m, name, blur=3.0, engrave=None):
    """m — маска (L, S×S). Рельеф: размытая маска = высота, нормаль → свет сверху-слева. engrave — маска гравировки (вдавлено)."""
    a = np.asarray(m, float) / 255
    h = np.asarray(m.filter(ImageFilter.GaussianBlur(blur * SS)), float) / 255
    if engrave is not None:
        e = np.asarray(engrave.filter(ImageFilter.GaussianBlur(0.8 * SS)), float) / 255
        h = h - 0.55 * e
    gy, gx = np.gradient(h)
    k = 9.0 * SS
    light = 0.58 + (-gx * 0.7 - gy * 0.7) * k
    # общий вертикальный градиент металла
    yy = np.linspace(0, 1, S)[:, None]
    light = light + 0.12 * (0.5 - yy)
    rgb = palette(light)
    if engrave is not None:
        e = np.asarray(engrave, float)[..., None] / 255
        rgb = rgb * (1 - 0.35 * e)
    img = np.dstack([rgb, a * 255]).astype(np.uint8)
    out = Image.fromarray(img, 'RGBA')
    # тень вокруг (мягкая, для отрыва от фона)
    sh = m.filter(ImageFilter.GaussianBlur(3 * SS))
    shadow = Image.new('RGBA', (S, S), (0, 0, 0, 0)); shadow.putalpha(sh.point(lambda v: int(v * 0.55)))
    res = Image.alpha_composite(shadow, out)
    res.resize((N, N), Image.LANCZOS).save(OUT + name + '.png')

def accent(draw_fn, name, width):
    """Тонкое белое кольцо внутри рамки — тонируется цветом класса/состояния."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    draw_fn(d, width)
    glow = im.filter(ImageFilter.GaussianBlur(2 * SS))
    res = Image.alpha_composite(glow, im)
    res.resize((N, N), Image.LANCZOS).save(OUT + name + '.png')

C = 64
# ---------------- стат: кольцо с четырьмя ромбами-шипами и гравированной канавкой
m, d = mask()
ell(d, C, C, 58, 255); ell(d, C, C, 46, 0)
for k in range(4):
    a = k * math.pi / 2 + math.pi / 4
    x, y = C + 58 * math.cos(a), C + 58 * math.sin(a)
    d.polygon(P([(x + 6 * math.cos(a), y + 6 * math.sin(a)), (x + 4 * math.cos(a + math.pi / 2), y + 4 * math.sin(a + math.pi / 2)),
                 (x - 3 * math.cos(a), y - 3 * math.sin(a)), (x + 4 * math.cos(a - math.pi / 2), y + 4 * math.sin(a - math.pi / 2))]), fill=255)
e, de = mask(); de.ellipse([(C - 52.5) * SS, (C - 52.5) * SS, (C + 52.5) * SS, (C + 52.5) * SS], outline=255, width=int(1.2 * SS))
emboss(m, 'node_frame', 2.4, e)
accent(lambda d, w: d.ellipse([(C - 45) * SS, (C - 45) * SS, (C + 45) * SS, (C + 45) * SS], outline=(255, 255, 255, 255), width=int(w * SS)), 'node_accent', 2.2)

# ---------------- класс: щит с короной и камнем
def shield(s, cx=C, cy=C + 4):
    return [(cx, cy - s), (cx + s * .52, cy - s * .84), (cx + s * .9, cy - s * .66), (cx + s * .84, cy + s * .14), (cx + s * .5, cy + s * .66),
            (cx, cy + s), (cx - s * .5, cy + s * .66), (cx - s * .84, cy + s * .14), (cx - s * .9, cy - s * .66), (cx - s * .52, cy - s * .84)]
m, d = mask()
d.polygon(P(shield(56)), fill=255); d.polygon(P(shield(44)), fill=0)
for k, (x, h) in enumerate(((C - 18, 12), (C, 18), (C + 18, 12))):   # корона
    d.polygon(P([(x - 6, C - 44), (x, C - 44 - h), (x + 6, C - 44)]), fill=255)
ell(d, C, C + 58, 7, 255)                                              # камень внизу
e, de = mask(); de.line(P(shield(50) + [shield(50)[0]]), fill=255, width=int(1.2 * SS))
emboss(m, 'class_frame', 2.6, e)
accent(lambda d, w: d.line(P(shield(42) + [shield(42)[0]]), fill=(255, 255, 255, 255), width=int(w * SS), joint='curve'), 'class_accent', 2.4)
inner = Image.new('RGBA', (S, S), (0, 0, 0, 0)); di = ImageDraw.Draw(inner); di.polygon(P(shield(45)), fill=(24, 18, 36, 255))
inner.resize((N, N), Image.LANCZOS).save(OUT + 'class_base.png')

# ---------------- навык: восьмиугольник с заклёпками и двойной кромкой
m, d = mask()
d.polygon(P(regular(C, C, 60, 8, math.pi / 8)), fill=255); d.polygon(P(regular(C, C, 47, 8, math.pi / 8)), fill=0)
for q in regular(C, C, 53.5, 8, math.pi / 8): ell(d, q[0], q[1], 0, 255)
e, de = mask()
for q in regular(C, C, 53.5, 8, math.pi / 8): de.ellipse([(q[0] - 2.6) * SS, (q[1] - 2.6) * SS, (q[0] + 2.6) * SS, (q[1] + 2.6) * SS], fill=255)
emboss(m, 'skill_frame', 2.4, e)
accent(lambda d, w: d.line(P(regular(C, C, 45, 8, math.pi / 8) + [regular(C, C, 45, 8, math.pi / 8)[0]]), fill=(255, 255, 255, 255), width=int(w * SS), joint='curve'), 'skill_accent', 2.4)
inner = Image.new('RGBA', (S, S), (0, 0, 0, 0)); di = ImageDraw.Draw(inner); di.polygon(P(regular(C, C, 48, 8, math.pi / 8)), fill=(22, 16, 34, 255))
inner.resize((N, N), Image.LANCZOS).save(OUT + 'skill_base.png')

# ---------------- центр: солнечное кольцо — 16 лучей и 8 заклёпок
m, d = mask()
pts = []
for k in range(32):
    r = 63 if k % 4 == 0 else 58 if k % 2 == 0 else 54
    a = -math.pi / 2 + k * math.pi / 16
    pts.append((C + r * math.cos(a), C + r * math.sin(a)))
d.polygon(P(pts), fill=255); ell(d, C, C, 42, 0)
e, de = mask()
de.ellipse([(C - 48) * SS, (C - 48) * SS, (C + 48) * SS, (C + 48) * SS], outline=255, width=int(1.3 * SS))
for q in regular(C, C, 51, 8, -math.pi / 2): de.ellipse([(q[0] - 2.4) * SS, (q[1] - 2.4) * SS, (q[0] + 2.4) * SS, (q[1] + 2.4) * SS], fill=255)
emboss(m, 'center_frame', 2.6, e)
accent(lambda d, w: d.ellipse([(C - 41) * SS, (C - 41) * SS, (C + 41) * SS, (C + 41) * SS], outline=(255, 255, 255, 255), width=int(w * SS)), 'center_accent', 2.4)

# ---------------- основания (диски) под рамки
def disc(name, r, inner, outer):
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0)); px = np.zeros((S, S, 4))
    yy, xx = np.mgrid[0:S, 0:S]; dd = np.hypot(xx - S / 2, yy - S / 2) / SS
    k = np.clip(dd / r, 0, 1)[..., None]
    col = np.array(inner, float) * (1 - k) + np.array(outer, float) * k
    a = np.clip((r - dd) * SS / 2, 0, 1) * 255
    img = np.dstack([col, a]).astype(np.uint8)
    Image.fromarray(img, 'RGBA').resize((N, N), Image.LANCZOS).save(OUT + name + '.png')
disc('node_base', 47, (34, 26, 50), (12, 9, 18))
disc('center_base', 43, (78, 40, 140), (18, 10, 32))
# ---------------- плашка-рамка подсказки и полотна не нужна: используем panel/plate мода
# предпросмотр
prev = Image.new('RGBA', (4 * 140, 140), (22, 16, 34, 255))
for i, n in enumerate(['node', 'class', 'skill', 'center']):
    b = Image.open(OUT + (n + '_base.png' if n in ('class', 'skill') else 'node_base.png')).resize((128, 128)) if n != 'center' else Image.open(OUT + 'center_base.png').resize((128, 128))
    f = Image.open(OUT + n + '_frame.png'); a = Image.open(OUT + n + '_accent.png')
    a = Image.fromarray((np.asarray(a).astype(float) * np.array([0.42, 0.78, 1.0, 1.0])).astype(np.uint8))
    tile = Image.new('RGBA', (128, 128), (0, 0, 0, 0)); tile.alpha_composite(b); tile.alpha_composite(a); tile.alpha_composite(f)
    prev.alpha_composite(tile, (i * 140 + 6, 6))
prev.resize((prev.width * 2, prev.height * 2), Image.LANCZOS).save('/tmp/frames.png')
print('ok')
